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
package com.falsepattern.zigbrains.lsp.contributors.annotator;

import com.falsepattern.zigbrains.lsp.IntellijLanguageClient;
import com.falsepattern.zigbrains.lsp.client.languageserver.ServerStatus;
import com.falsepattern.zigbrains.lsp.client.languageserver.wrapper.LanguageServerWrapper;
import com.falsepattern.zigbrains.lsp.editor.EditorEventManager;
import com.falsepattern.zigbrains.lsp.editor.EditorEventManagerBase;
import com.falsepattern.zigbrains.lsp.utils.DocumentUtils;
import com.falsepattern.zigbrains.lsp.utils.FileUtils;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.SmartList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DiagnosticTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;

public class LSPAnnotator extends ExternalAnnotator<Object, Object> {

    private static final Logger LOG = Logger.getInstance(LSPAnnotator.class);
    private static final Object RESULT = new Object();
    private static final HashMap<DiagnosticSeverity, HighlightSeverity> lspToIntellijAnnotationsMap = new HashMap<>();

    static {
        lspToIntellijAnnotationsMap.put(DiagnosticSeverity.Error, HighlightSeverity.ERROR);
        lspToIntellijAnnotationsMap.put(DiagnosticSeverity.Warning, HighlightSeverity.WARNING);

        // seem flipped, but just different semantics lsp<->intellij. Hint is rendered without any squiggle
        lspToIntellijAnnotationsMap.put(DiagnosticSeverity.Information, HighlightSeverity.WEAK_WARNING);
        lspToIntellijAnnotationsMap.put(DiagnosticSeverity.Hint, HighlightSeverity.INFORMATION);
    }

    @Nullable
    @Override
    public Object collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {

        try {
            VirtualFile virtualFile = file.getVirtualFile();

            // If the file is not supported, we skips the annotation by returning null.
            if (!FileUtils.isFileSupported(virtualFile) || !IntellijLanguageClient.isExtensionSupported(virtualFile)) {
                return null;
            }
            EditorEventManager eventManager = EditorEventManagerBase.forEditor(editor);

            if (eventManager == null) {
                return null;
            }

            // If the diagnostics list is locked, we need to skip annotating the file.
            if (!(eventManager.isDiagnosticSyncRequired() || eventManager.isCodeActionSyncRequired())) {
                return null;
            }
            return RESULT;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    @Override
    public Object doAnnotate(Object collectedInfo) {
        return RESULT;
    }

    @Override
    public void apply(@NotNull PsiFile file, Object annotationResult, @NotNull AnnotationHolder holder) {

        LanguageServerWrapper languageServerWrapper = LanguageServerWrapper.forVirtualFile(file.getVirtualFile(), file.getProject());
        if (languageServerWrapper == null || languageServerWrapper.getStatus() != ServerStatus.INITIALIZED) {
            return;
        }

        VirtualFile virtualFile = file.getVirtualFile();
        if (FileUtils.isFileSupported(virtualFile) && IntellijLanguageClient.isExtensionSupported(virtualFile)) {
            String uri = FileUtils.VFSToURI(virtualFile);
            // TODO annotations are applied to a file / document not to an editor. so store them by file and not by editor..
            EditorEventManager eventManager = EditorEventManagerBase.forUri(uri);
            if (eventManager == null) {
                return;
            }
            if (eventManager.isDiagnosticSyncRequired()) {
                try {
                    createAnnotations(holder, eventManager);
                } catch (ConcurrentModificationException e) {
                    // Todo - Add proper fix to handle concurrent modifications gracefully.
                    LOG.warn("Error occurred when updating LSP code actions due to concurrent modifications.", e);
                } catch (Throwable t) {
                    LOG.warn("Error occurred when updating LSP code actions.", t);
                }
            } else if (eventManager.isCodeActionSyncRequired()) {
                try {
                    updateAnnotations(holder, eventManager);
                } catch (ConcurrentModificationException e) {
                    // Todo - Add proper fix to handle concurrent modifications gracefully.
                    LOG.warn("Error occurred when updating LSP diagnostics due to concurrent modifications.", e);
                } catch (Throwable t) {
                    LOG.warn("Error occurred when updating LSP diagnostics.", t);
                }
            }
        }
    }

    private void updateAnnotations(AnnotationHolder holder, EditorEventManager eventManager) {
        final List<Annotation> annotations = eventManager.getAnnotations();
        if (annotations == null) {
            return;
        }
        var requests = eventManager.fetchQuickFixes();
        annotations.forEach(annotation -> {
            if  (annotation.getQuickFixes() != null && !annotation.getQuickFixes().isEmpty()) {
                AnnotationBuilder builder = holder.newAnnotation(annotation.getSeverity(), annotation.getMessage());
                for (Annotation.QuickFixInfo quickFixInfo : annotation.getQuickFixes()) {
                    builder = builder.withFix(quickFixInfo.quickFix);
                }
                builder.create();
            } else if (requests.containsKey(annotation)) {
                AnnotationBuilder builder = holder.newAnnotation(annotation.getSeverity(), annotation.getMessage());
                var request = requests.remove(annotation);
                for (var quickFixInfo: request) {
                    builder = builder.withFix(quickFixInfo.action());
                }
                builder.create();
            }
        });
    }

    @Nullable
    protected AnnotationBuilder createAnnotation(Editor editor, AnnotationHolder holder, Diagnostic diagnostic) {
        final int start = DocumentUtils.LSPPosToOffset(editor, diagnostic.getRange().getStart());
        final int end = DocumentUtils.LSPPosToOffset(editor, diagnostic.getRange().getEnd());
        if (start >= end) {
            return null;
        }
        final TextRange range = new TextRange(start, end);

        return holder.newAnnotation(lspToIntellijAnnotationsMap.get(diagnostic.getSeverity()), diagnostic.getMessage())
                     .range(range);
    }

    private void createAnnotations(AnnotationHolder holder, EditorEventManager eventManager) {
        final List<Diagnostic> diagnostics = eventManager.getDiagnostics();
        final Editor editor = eventManager.editor;

        List<Annotation> annotations = new ArrayList<>();
        diagnostics.forEach(d -> {
            var annotation = createAnnotation(editor, holder, d);
            if (annotation != null) {
                if (d.getTags() != null && d.getTags().contains(DiagnosticTag.Deprecated)) {
                    annotation = annotation.highlightType(ProblemHighlightType.LIKE_DEPRECATED);
                }
                annotation.create();
                var theList = (SmartList<Annotation>) holder;
                annotations.add(theList.get(theList.size() - 1));
            }
        });

        eventManager.setAnnotations(annotations);
        eventManager.setAnonHolder(holder);
    }
}
