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

package com.falsepattern.zigbrains.lsp.contributors;

import com.falsepattern.zigbrains.common.util.ApplicationUtil;
import com.falsepattern.zigbrains.lsp.editor.EditorEventManagerBase;
import com.falsepattern.zigbrains.lsp.requests.HoverHandler;
import com.falsepattern.zigbrains.lsp.requests.Timeout;
import com.falsepattern.zigbrains.lsp.requests.Timeouts;
import com.falsepattern.zigbrains.lsp.utils.DocumentUtils;
import com.falsepattern.zigbrains.lsp.utils.FileUtils;
import com.falsepattern.zigbrains.backports.com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter;
import com.intellij.model.Pointer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.platform.backend.documentation.DocumentationResult;
import com.intellij.platform.backend.documentation.DocumentationTarget;
import com.intellij.platform.backend.documentation.DocumentationTargetProvider;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiFileRange;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.intellij.codeInsight.documentation.DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL;

public class LSPDocumentationTargetProvider implements DocumentationTargetProvider {
    @Override
    public @NotNull List<? extends @NotNull DocumentationTarget> documentationTargets(@NotNull PsiFile file, int offset) {
        if (!FileUtils.isFileSupported(file.getVirtualFile())) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new LSPDocumentationTarget(file, offset));
    }

    public static class LSPDocumentationTarget implements DocumentationTarget {
        private final Pointer<LSPDocumentationTarget> pointer;
        public final PsiFile file;
        private final int offset;
        public LSPDocumentationTarget(PsiFile file, int offset) {
            this.file = file;
            this.offset = offset;

            var range = TextRange.from(offset, 0);
            SmartPsiFileRange base = SmartPointerManager.getInstance(file.getProject()).createSmartPsiFileRangePointer(file, range);
            pointer = new FileRangePointer(base);
        }

        protected Logger LOG = Logger.getInstance(LSPDocumentationTargetProvider.class);

        @Nullable
        @Override
        public DocumentationResult computeDocumentation() {
            var editor = FileUtils.editorFromPsiFile(file);
            if (editor == null) {
                return null;
            }
            var manager = EditorEventManagerBase.forEditor(editor);
            if (manager == null) {
                return null;
            }
            var wrapper = manager.wrapper;
            if (wrapper == null) {
                return null;
            }
            var caretPos = editor.offsetToLogicalPosition(offset);
            var serverPos = ApplicationUtil.computableReadAction(() -> DocumentUtils.logicalToLSPPos(caretPos, editor));
            return DocumentationResult.asyncDocumentation(() -> {
                var identifier = manager.getIdentifier();
                var request = wrapper.getRequestManager().hover(new HoverParams(identifier, serverPos));
                if (request == null) {
                    return null;
                }
                try {
                    var hover = request.get(Timeout.getTimeout(Timeouts.HOVER), TimeUnit.MILLISECONDS);
                    wrapper.notifySuccess(Timeouts.HOVER);
                    if (hover == null) {
                        LOG.debug(String.format("Hover is null for file %s and pos (%d;%d)", identifier.getUri(),
                                                serverPos.getLine(), serverPos.getCharacter()));
                        return null;
                    }

                    val markdown = HoverHandler.getHoverString(hover).replaceAll("file://", PSI_ELEMENT_PROTOCOL + "zigbrains://");
                    val string = ApplicationUtil.computableReadAction(() -> DocMarkdownToHtmlConverter
                            .convert(manager.getProject(), markdown));
                    if (StringUtils.isEmpty(string)) {
                        LOG.warn(String.format("Hover string returned is empty for file %s and pos (%d;%d)",
                                               identifier.getUri(), serverPos.getLine(), serverPos.getCharacter()));
                        return null;
                    }
                    return DocumentationResult.documentation(string);
                } catch (TimeoutException e) {
                    LOG.warn(e);
                    wrapper.notifyFailure(Timeouts.HOVER);
                } catch (InterruptedException | JsonRpcException | ExecutionException e) {
                    LOG.warn(e);
                    wrapper.crashed(e);
                }
                return null;
            });
        }

        @NotNull
        @Override
        public TargetPresentation computePresentation() {
            return TargetPresentation.builder("Doc from language server").presentation();
        }

        @NotNull
        @Override
        public Pointer<? extends DocumentationTarget> createPointer() {
            return pointer;
        }

        private static class FileRangePointer implements Pointer<LSPDocumentationTarget> {
            private final SmartPsiFileRange base;
            public FileRangePointer(SmartPsiFileRange base) {
                this.base = base;
            }
            @Override
            public @Nullable LSPDocumentationTarget dereference() {
                if (base.getElement() == null) {
                    return null;
                }
                if (base.getRange() == null) {
                    return null;
                }
                return new LSPDocumentationTarget(base.getElement(), TextRange.create(base.getRange()).getStartOffset());
            }
        }
    }
}
