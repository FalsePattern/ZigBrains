/*
 * Copyright 2023-2024 FalsePattern
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.falsepattern.zigbrains.zig.util;

import com.falsepattern.zigbrains.zig.ide.SemaEdit;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.redhat.devtools.lsp4ij.LSPFileSupport;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.LanguageServerItem;
import com.redhat.devtools.lsp4ij.LanguageServerWrapper;
import com.redhat.devtools.lsp4ij.LanguageServersRegistry;
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.lsp4ij.internal.CompletableFutures;
import lombok.SneakyThrows;
import lombok.val;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensDelta;
import org.eclipse.lsp4j.SemanticTokensDeltaParams;
import org.eclipse.lsp4j.SemanticTokensEdit;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class HighlightingUtil {
    private static final Key<Integer> HL_HASH = Key.create("HIGHLIGHTING_HASH");

    public static void refreshHighlighting(Editor editor) {
        var app = ApplicationManager.getApplication();
        app.executeOnPooledThread(() -> {
            if (editor.isDisposed())
                return;

            val project = editor.getProject();
            if (project == null)
                return;

            val highlightRanges = semanticHighlighting(editor);

            var newHash = highlightRanges.hashCode();
            if (editor.isDisposed()) {
                return;
            }
            var markup = editor.getMarkupModel();
            var hash = markup.getUserData(HL_HASH);
            if (hash != null && hash == newHash) {
                return;
            }
            markup.putUserData(HL_HASH, newHash);
            var highlightersSorted = new ArrayList<>(Stream.of(WriteAction.computeAndWait(markup::getAllHighlighters))
                                                           .map(hl -> Map.entry(hl, hl.getStartOffset()))
                                                           .sorted(Comparator.comparingInt(Map.Entry::getValue))
                                                           .toList());
            val writes = new ArrayList<Runnable>();
            app.runReadAction(() -> {
                if (editor.isDisposed()) {
                    return;
                }
                if (highlightRanges.size() == 1 &&
                    highlightRanges.get(0).start() == 0 &&
                    highlightRanges.get(0).remove() == -1) {
                    writes.add(markup::removeAllHighlighters);
                }
                var documentLength = editor.getDocument().getTextLength();
                for (var range : highlightRanges) {
                    var start = range.start();
                    var toRemove = range.remove();
                    if (toRemove > 0) {
                        for (int i = 0; i < highlightersSorted.size(); i++) {
                            var hl = highlightersSorted.get(i);
                            if (hl.getValue() >= start) {
                                for (int j = 0; j < toRemove; j++) {
                                    highlightersSorted.remove(i);
                                    val key = hl.getKey();
                                    writes.add(() -> markup.removeHighlighter(key));
                                }
                            }
                        }
                    }
                    for (var edit : range.add()) {
                        var editStart = edit.start();
                        var end = edit.end();
                        if (end > documentLength || editStart > documentLength) {
                            continue;
                        }
                        val color = edit.color();
                        writes.add(() -> markup.addRangeHighlighter(color, editStart, end, HighlighterLayer.ADDITIONAL_SYNTAX, HighlighterTargetArea.EXACT_RANGE));
                    }
                }
                app.invokeLater(() -> {
                    for (val write: writes)
                        write.run();
                });
            });
        });
    }

    private static String previousResultID = null;

    private static Optional<Pair<LanguageServerItem, SemanticTokensLegend>> sematicLegend(Collection<LanguageServerItem> servers) {
        for (val server: servers) {
            val caps = server.getServerCapabilities();
            if (caps == null)
                continue;
            val provider = caps.getSemanticTokensProvider();
            if (provider == null)
                continue;
            val legend = provider.getLegend();
            if (legend == null)
                continue;
            return Optional.of(new Pair<>(server, legend));
        }
        return Optional.empty();
    }
    @SneakyThrows
    private static List<SemaEdit> semanticHighlighting(Editor editor) {
        var result = new ArrayList<SemaEdit>();
        val virtualFile = editor.getVirtualFile();
        if (virtualFile == null)
            return result;
        val project = editor.getProject();
        if (project == null)
            return result;

        val definition = LanguageServersRegistry.getInstance().getServerDefinition("ZigBrains");
        val servers = LanguageServiceAccessor.getInstance(project).getLanguageServers(virtualFile, (ignored) -> true, definition).get();
        val legendOptional = sematicLegend(servers);
        if (legendOptional.isEmpty()) {
            return result;
        }
        val legend = legendOptional.get().second;
        val server = legendOptional.get().first.getServer();
        CompletableFuture<Either<SemanticTokens, SemanticTokensDelta>> request = null;
        val service = server.getTextDocumentService();
        if (previousResultID == null) {
            var param = new SemanticTokensParams(LSPIJUtils.toTextDocumentIdentifier(editor.getVirtualFile()));
            request = service.semanticTokensFull(param)
                                    .thenApply(tokens -> tokens != null ? Either.forLeft(tokens) : null);
        } else {
            var param = new SemanticTokensDeltaParams(LSPIJUtils.toTextDocumentIdentifier(editor.getVirtualFile()), previousResultID);
            request = service.semanticTokensFullDelta(param);
        }

        try {
            CompletableFutures.waitUntilDone(request);
            if (!CompletableFutures.isDoneNormally(request))
                return result;
            var res = request.getNow(null);
            if (res == null) {
                return result;
            }
            if (res.isLeft()) {
                var response = res.getLeft();
                previousResultID = response.getResultId();
                var responseData = response.getData();
                result.add(new SemaEdit(0, -1, TokenDecoder.decodePayload(0, editor, legend, responseData)));
            } else {
                var response = res.getRight();
                previousResultID = response.getResultId();
                var edits = response.getEdits();
                for (SemanticTokensEdit edit : edits) {
                    var add = TokenDecoder.decodePayload(0, editor, legend, edit.getData());
                    result.add(new SemaEdit(edit.getStart(), edit.getDeleteCount(), add));
                }
            }
        } catch (CancellationException e) {
            return null;
        } catch (ExecutionException e) {
            System.err.println("Error while consuming LSP 'textDocument/semanticTokens' request");
            e.printStackTrace();
        }
        return result;
    }

//    public static void refreshHighlighting(EditorEventManager eem) {
//        var app = ApplicationManager.getApplication();
//        app.executeOnPooledThread(() -> {
//            if (!(eem instanceof ZLSEditorEventManager manager)) {
//                return;
//            }
//            var editor = manager.editor;
//            if (editor == null) {
//                return;
//            }
//            if (editor.isDisposed()) {
//                return;
//            }
//            var highlightRanges = manager.semanticHighlighting();
//            var newHash = highlightRanges.hashCode();
//            if (editor.isDisposed()) {
//                return;
//            }
//            var markup = editor.getMarkupModel();
//            var hash = markup.getUserData(HL_HASH);
//            if (hash != null && hash == newHash) {
//                return;
//            }
//            markup.putUserData(HL_HASH, newHash);
//            var highlightersSorted = new ArrayList<>(Stream.of(WriteAction.computeAndWait(markup::getAllHighlighters))
//                                                           .map(hl -> Map.entry(hl, hl.getStartOffset()))
//                                                           .sorted(Comparator.comparingInt(Map.Entry::getValue))
//                                                           .toList());
//            val writes = new ArrayList<Runnable>();
//            app.runReadAction(() -> {
//                if (editor.isDisposed()) {
//                    return;
//                }
//                if (highlightRanges.size() == 1 &&
//                    highlightRanges.get(0).start() == 0 &&
//                    highlightRanges.get(0).remove() == -1) {
//                    writes.add(markup::removeAllHighlighters);
//                }
//                var documentLength = editor.getDocument().getTextLength();
//                for (var range : highlightRanges) {
//                    var start = range.start();
//                    var toRemove = range.remove();
//                    if (toRemove > 0) {
//                        for (int i = 0; i < highlightersSorted.size(); i++) {
//                            var hl = highlightersSorted.get(i);
//                            if (hl.getValue() >= start) {
//                                for (int j = 0; j < toRemove; j++) {
//                                    highlightersSorted.remove(i);
//                                    val key = hl.getKey();
//                                    writes.add(() -> markup.removeHighlighter(key));
//                                }
//                            }
//                        }
//                    }
//                    for (var edit : range.add()) {
//                        var editStart = edit.start();
//                        var end = edit.end();
//                        if (end > documentLength || editStart > documentLength) {
//                            continue;
//                        }
//                        val color = edit.color();
//                        writes.add(() -> markup.addRangeHighlighter(color, editStart, end, HighlighterLayer.ADDITIONAL_SYNTAX, HighlighterTargetArea.EXACT_RANGE));
//                    }
//                }
//                app.invokeLater(() -> {
//                    for (val write: writes)
//                        write.run();
//                });
//            });
//        });
//    }
}
