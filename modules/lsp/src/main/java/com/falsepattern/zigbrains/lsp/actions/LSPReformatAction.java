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

import com.falsepattern.zigbrains.common.util.ApplicationUtil;
import com.falsepattern.zigbrains.lsp.editor.EditorEventManager;
import com.falsepattern.zigbrains.lsp.requests.ReformatHandler;
import com.intellij.codeInsight.actions.ReformatCodeAction;
import com.intellij.ide.lightEdit.LightEditCompatible;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiFile;
import lombok.val;

/**
 * Action overriding the default reformat action
 * Fallback to the default action if the language is already supported or not supported by any language server
 */
public class LSPReformatAction extends WrappedAction<ReformatCodeAction> implements DumbAware, LightEditCompatible {
    public LSPReformatAction(ReformatCodeAction wrapped) {
        super(wrapped);
    }

    @Override
    public void actionPerformedLSP(AnActionEvent e, EditorEventManager manager, PsiFile file) {
        val editor = manager.editor;
        ApplicationUtil.writeAction(() -> FileDocumentManager.getInstance().saveDocument(editor.getDocument()));
        // if editor hasSelection, only reformat selection, not reformat the whole file
        if (editor.getSelectionModel().hasSelection()) {
            ReformatHandler.reformatSelection(editor);
        } else {
            ReformatHandler.reformatFile(editor);
        }
    }
}
