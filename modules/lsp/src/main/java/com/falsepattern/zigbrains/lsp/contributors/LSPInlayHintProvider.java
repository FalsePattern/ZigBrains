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

import com.falsepattern.zigbrains.lsp.editor.EditorEventManagerBase;
import com.falsepattern.zigbrains.lsp.utils.DocumentUtils;
import com.falsepattern.zigbrains.lsp.utils.FileUtils;
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector;
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.codeInsight.hints.declarative.InlayTreeSink;
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition;
import com.intellij.codeInsight.hints.declarative.OwnBypassCollector;
import com.intellij.codeInsight.hints.declarative.impl.DeclarativeInlayHintsPassFactory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class LSPInlayHintProvider implements InlayHintsProvider {
    protected static Logger LOG = Logger.getInstance(LSPInlayHintProvider.class);
    private static final LSPInlayHintsCollector DEFAULT_COLLECTOR = new LSPInlayHintsCollector();
    @Nullable
    @Override
    public InlayHintsCollector createCollector(@NotNull PsiFile psiFile, @NotNull Editor editor) {
        if (FileUtils.isFileSupported(psiFile.getVirtualFile())) {
            return getCollector();
        }
        return null;
    }

    public LSPInlayHintsCollector getCollector() {
        return DEFAULT_COLLECTOR;
    }

    public static class LSPInlayHintsCollector implements OwnBypassCollector {
        @Override
        public void collectHintsForFile(@NotNull PsiFile psiFile, @NotNull InlayTreeSink inlayTreeSink) {
            var editor = FileUtils.editorFromPsiFile(psiFile);
            if (editor == null) {
                return;
            }
            var manager = EditorEventManagerBase.forEditor(editor);
            if (manager == null || manager.editor != editor) {
                EditorEventManagerBase.runWhenManagerGetsRegistered(editor,
                                                                    () -> {
                    if (editor.isDisposed()) {
                        return;
                    }
                    var project = editor.getProject();
                    if (project == null) {
                        return;
                    }
                    DeclarativeInlayHintsPassFactory.Companion.scheduleRecompute(editor, project);
                });
                return;
            }
            var res = manager.inlayHint();
            if (res == null) {
                return;
            }
            for (var hint: res) {
                var pos = DocumentUtils.LSPPosToOffset(editor, hint.getPosition());
                var inlayPos = new InlineInlayPosition(pos, false, 0);
                var tt = hint.getTooltip();
                if (tt == null) {
                    continue;
                }
                String tooltipText;
                if (tt.isLeft()) {
                    tooltipText = tt.getLeft();
                } else {
                    var markup = tt.getRight();
                    tooltipText = switch (markup.getKind()) {
                        case "markdown" -> {
                            var markedContent = markup.getValue();
                            if (markedContent.isEmpty()) {
                                yield "";
                            }
                            Parser parser = Parser.builder().build();
                            HtmlRenderer renderer = HtmlRenderer.builder().build();
                            yield "<html>" + renderer.render(parser.parse(markedContent)) + "</html>";
                        }
                        default -> markup.getValue();
                    };
                }
                inlayTreeSink.addPresentation(inlayPos, Collections.emptyList(), tooltipText, true, (builder) -> {
                    var label = hint.getLabel();
                    StringBuilder text = new StringBuilder();
                    if (label.isLeft()) {
                        text.append(label.getLeft());
                    } else if (label.isRight()) {
                        var parts = label.getRight();
                        for (var part: parts) {
                            text.append(part.getValue());
                        }
                    }
                    if (text.length() == 0) {
                        text.append(" ");
                    }
                    builder.text(text.toString(), null);
                    return null;
                });
            }
        }
    }
}
