/*
 * Copyright 2023 FalsePattern
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
package com.falsepattern.zigbrains.lsp.contributors;

import com.falsepattern.zigbrains.lsp.client.languageserver.wrapper.LanguageServerWrapper;
import com.falsepattern.zigbrains.lsp.requests.Timeout;
import com.falsepattern.zigbrains.lsp.requests.Timeouts;
import com.falsepattern.zigbrains.lsp.utils.DocumentUtils;
import com.falsepattern.zigbrains.lsp.utils.FileUtils;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.CustomFoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LSPFoldingRangeProvider extends CustomFoldingBuilder {
    private static final Key<Boolean> ASYNC_FOLDING_KEY = new Key<>("ASYNC_FOLDING");

    protected Logger LOG = Logger.getInstance(LSPFoldingRangeProvider.class);

    private interface FoldingRangeAcceptor {
        void accept(int start, int end, @Nullable String replacement);
    }

    private static class AFoldingRange {
        public final int start;
        public final int end;
        public final String collapsedText;

        private AFoldingRange(int start, int end, String collapsedText) {
            this.start = start;
            this.end = end;
            this.collapsedText = collapsedText;
        }
    }

    @Override
    protected void buildLanguageFoldRegions(@NotNull List<FoldingDescriptor> descriptors, @NotNull PsiElement root, @NotNull Document document, boolean quick) {
        // if the quick flag is set, we do nothing here
        if (quick) {
            return;
        }

        var async = async(root.getProject());
        if (!async) {
            doBuildLanguageFoldRegions((start, end, collapsedText) -> {
                if (collapsedText != null) {
                    descriptors.add(new FoldingDescriptor(root.getNode(), new TextRange(start, end), null, collapsedText));
                } else {
                    descriptors.add(new FoldingDescriptor(root.getNode(), new TextRange(start, end)));
                }
            }, root, document, false);
            return;
        }
        var app = ApplicationManager.getApplication();
        app.executeOnPooledThread(() -> {
            var ranges = new ArrayList<AFoldingRange>();
            doBuildLanguageFoldRegions((start, end, collapsedText) -> ranges.add(
                                               new AFoldingRange(start, end, collapsedText == null ? "..." : collapsedText)),
                                       root, document, true);
            var editor = FileUtils.editorFromPsiFile(root.getContainingFile());
            if (editor == null) {
                return;
            }
            app.invokeLater(() -> {
                if (editor.isDisposed()) {
                    return;
                }
                var foldingModel = editor.getFoldingModel();
                var oldRegions = Arrays.stream(foldingModel.getAllFoldRegions()).filter(region -> {
                    var data = region.getUserData(ASYNC_FOLDING_KEY);
                    return data != null && data;
                }).toList();
                foldingModel.runBatchFoldingOperation(() -> {
                    for (var oldRegion: oldRegions) {
                        foldingModel.removeFoldRegion(oldRegion);
                    }
                    for (var range: ranges) {
                        var region = foldingModel.addFoldRegion(range.start, range.end, range.collapsedText);
                        if (region != null) {
                            region.putUserData(ASYNC_FOLDING_KEY, true);
                        }
                    }
                });
            });
        });
    }

    private void doBuildLanguageFoldRegions(@NotNull FoldingRangeAcceptor acceptor, @NotNull PsiElement root, @NotNull Document document, boolean async) {
        PsiFile psiFile = root.getContainingFile();
        var editor = FileUtils.editorFromPsiFile(psiFile);
        var wrapper = LanguageServerWrapper.forVirtualFile(psiFile.getVirtualFile(), root.getProject());
        if (editor == null || wrapper == null || !editor.getDocument().equals(document)) {
            return;
        }

        var manager = wrapper.getRequestManager();
        if (manager == null) {
            //IDE startup race condition
            if (!async)
                return;

            //We can block the async thread for a moment; wait 2 more seconds
            for (int i = 0; i < 20 && manager == null; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    //We got interrupted, bail
                    return;
                }
                manager = wrapper.getRequestManager();
            }
            if (manager == null)
                return; //LSP did not connect in time, bail
        }

        TextDocumentIdentifier textDocumentIdentifier = FileUtils.editorToLSPIdentifier(editor);
        FoldingRangeRequestParams params = new FoldingRangeRequestParams(textDocumentIdentifier);
        CompletableFuture<List<FoldingRange>> future = manager.foldingRange(params);

        if (future == null) {
            return;
        }
        try {
            List<FoldingRange> foldingRanges = future.get(Timeout.getTimeout(Timeouts.FOLDING), TimeUnit.MILLISECONDS);
            wrapper.notifySuccess(Timeouts.FOLDING);
            if (foldingRanges == null) {
                return;
            }
            for (FoldingRange foldingRange : foldingRanges) {
                int start = getStartOffset(editor, foldingRange, document);
                int end = getEndOffset(editor, foldingRange, document);
                int length = end - start;
                if (length <= 0) {
                    continue;
                }

                if (end > root.getTextLength()) {
                    continue;
                }

                var collapsedText = getCollapsedText(foldingRange);
                acceptor.accept(start, end, collapsedText);
            }
        } catch (TimeoutException | InterruptedException e) {
            LOG.warn(e);
            wrapper.notifyFailure(Timeouts.FOLDING);
        } catch (JsonRpcException | ExecutionException e) {
            LOG.warn(e);
            wrapper.crashed(e);
        }
    }

    protected boolean async(Project project) {
        return true;
    }

    protected @Nullable String getCollapsedText(@NotNull FoldingRange foldingRange) {
        return foldingRange.getCollapsedText();
    }

    private int getEndOffset(Editor editor, @NotNull FoldingRange foldingRange, @NotNull Document document) {
        // EndCharacter is optional. When missing, it should be set to the length of the end line.
        if (foldingRange.getEndCharacter() == null) {
            return document.getLineEndOffset(foldingRange.getEndLine());
        }

        return DocumentUtils.LSPPosToOffset(editor, new Position(foldingRange.getEndLine(), foldingRange.getEndCharacter()));
    }

    private int getStartOffset(Editor editor, @NotNull FoldingRange foldingRange, @NotNull Document document) {
        // StartCharacter is optional. When missing, it should be set to the length of the start line.
        if (foldingRange.getStartCharacter() == null) {
            return document.getLineEndOffset(foldingRange.getStartLine());
        } else {
            return DocumentUtils.LSPPosToOffset(editor, new Position(foldingRange.getStartLine(), foldingRange.getStartCharacter()));
        }
    }

    @Override
    protected String getLanguagePlaceholderText(@NotNull ASTNode node, @NotNull TextRange range) {
        return null;
    }

    @Override
    protected boolean isRegionCollapsedByDefault(@NotNull ASTNode node) {
        return false;
    }
}
