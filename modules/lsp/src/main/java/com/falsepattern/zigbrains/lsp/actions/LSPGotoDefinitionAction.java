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

package com.falsepattern.zigbrains.lsp.actions;

import com.falsepattern.zigbrains.lsp.IntellijLanguageClient;
import com.falsepattern.zigbrains.lsp.editor.EditorEventManager;
import com.falsepattern.zigbrains.lsp.editor.EditorEventManagerBase;
import com.intellij.codeInsight.hint.actions.ShowImplementationsAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import lombok.val;
import org.jetbrains.annotations.NotNull;

public class LSPGotoDefinitionAction extends ShowImplementationsAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(CommonDataKeys.PROJECT);
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null || project == null) {
            super.actionPerformed(e);
            return;
        }
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null || !IntellijLanguageClient.isExtensionSupported(file.getVirtualFile())) {
            super.actionPerformed(e);
            return;
        }
        EditorEventManager manager = EditorEventManagerBase.forEditor(editor);
        if (manager == null) {
            super.actionPerformed(e);
            return;
        }
        val offset = editor.getCaretModel().getOffset();
        val psiElement = file.findElementAt(offset);
        if (psiElement == null) {
            super.actionPerformed(e);
            return;
        }
        if (!manager.gotoDefinition(psiElement)) {
            super.actionPerformed(e);
        }
    }
}
