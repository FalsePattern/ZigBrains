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
package com.falsepattern.zigbrains.lsp.contributors.rename;

import com.falsepattern.zigbrains.lsp.contributors.psi.LSPPsiElement;
import com.falsepattern.zigbrains.lsp.editor.EditorEventManager;
import com.falsepattern.zigbrains.lsp.editor.EditorEventManagerBase;
import com.falsepattern.zigbrains.lsp.requests.WorkspaceEditHandler;
import com.falsepattern.zigbrains.lsp.utils.FileUtils;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.rename.RenameDialog;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LSPRenameProcessor implementation.
 */
public class LSPRenameProcessor extends RenamePsiElementProcessor {

    private PsiElement curElem;
    private final Set<PsiElement> elements = new HashSet<>();
    private static final Set<VirtualFile> openedEditors = new HashSet<>();

    @Override
    public boolean canProcessElement(@NotNull PsiElement element) {
        return element instanceof LSPPsiElement;
    }

    @NotNull
    @Override
    public RenameDialog createRenameDialog(@NotNull Project project, @NotNull PsiElement element,
                                           PsiElement nameSuggestionContext, Editor editor) {
        return super.createRenameDialog(project, curElem, nameSuggestionContext, editor);
    }

    @NotNull
    @SuppressWarnings("unused")
    public Collection<PsiReference> findReferences(@NotNull PsiElement element, @NotNull SearchScope searchScope,
                                                   boolean searchInCommentsAndStrings) {
        if (element instanceof LSPPsiElement) {
            if (elements.contains(element)) {
                return elements.stream().map(PsiElement::getReference).filter(Objects::nonNull).collect(Collectors.toList());
            }
            EditorEventManager
                    manager = EditorEventManagerBase.forEditor(FileUtils.editorFromPsiFile(element.getContainingFile()));
            if (manager != null) {
                Pair<List<PsiElement>, List<VirtualFile>> refs = manager.referencesForRename(element, true, false);
                if (refs.getFirst() != null && refs.getSecond() != null) {
                    addEditors(refs.getSecond());
                    return refs.getFirst().stream().map(PsiElement::getReference).filter(Objects::nonNull).collect(Collectors.toList());
                }
            }
        }
        return new ArrayList<>();
    }

    //TODO may rename invalid elements
    @Override
    public void renameElement(@NotNull PsiElement element, @NotNull String newName, @NotNull UsageInfo[] usages,
                              RefactoringElementListener listener) {
        WorkspaceEditHandler.applyEdit(element, newName, usages, listener, new ArrayList<>(openedEditors));
        openedEditors.clear();
        elements.clear();
        curElem = null;
    }

    @Override
    public boolean isInplaceRenameSupported() {
        return true;
    }

    public static void clearEditors() {
        openedEditors.clear();
    }

    public static Set<VirtualFile> getEditors() {
        return openedEditors;
    }

    static void addEditors(List<VirtualFile> toAdd) {
        openedEditors.addAll(toAdd);
    }
}
