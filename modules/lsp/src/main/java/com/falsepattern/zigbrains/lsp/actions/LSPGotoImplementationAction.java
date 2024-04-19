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

import com.falsepattern.zigbrains.lsp.editor.EditorEventManager;
import com.intellij.codeInsight.navigation.actions.GotoImplementationAction;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PerformWithDocumentsCommitted;
import com.intellij.psi.PsiFile;
import lombok.val;

public class LSPGotoImplementationAction extends WrappedAction<GotoImplementationAction> implements PerformWithDocumentsCommitted {
    public LSPGotoImplementationAction(GotoImplementationAction wrapped) {
        super(wrapped);
    }

    @Override
    protected void actionPerformedLSP(AnActionEvent e, EditorEventManager manager, PsiFile file) {
        val offset = manager.editor.getCaretModel().getOffset();
        val psiElement = file.findElementAt(offset);
        if (psiElement == null) {
            return;
        }
        manager.gotoDefinition(psiElement);
    }

    @Override
    protected void updateLSP(AnActionEvent e, EditorEventManager manager, PsiFile file) {
        if (e.getPresentation().getTextWithMnemonic() == null) {
            e.getPresentation().setText(ActionsBundle.actionText("GotoImplementation"));
            e.getPresentation().setDescription(ActionsBundle.actionDescription("GotoImplementation"));
        }
    }
}
