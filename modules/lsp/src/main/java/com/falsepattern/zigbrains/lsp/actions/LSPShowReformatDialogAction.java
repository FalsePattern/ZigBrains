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
import com.intellij.codeInsight.actions.LayoutCodeDialog;
import com.intellij.codeInsight.actions.LayoutCodeOptions;
import com.intellij.codeInsight.actions.ShowReformatFileDialog;
import com.intellij.codeInsight.actions.TextRangeType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import lombok.val;

/**
 * Class overriding the default action handling the Reformat dialog event (CTRL+ALT+SHIFT+L by default)
 * Fallback to the default action if the language is already supported or not supported by any language server
 */
public class LSPShowReformatDialogAction extends WrappedAction<ShowReformatFileDialog> implements DumbAware {

    private String HELP_ID = "editing.codeReformatting";
    private Logger LOG = Logger.getInstance(LSPShowReformatDialogAction.class);

    public LSPShowReformatDialogAction(ShowReformatFileDialog wrapped) {
        super(wrapped);
    }

    @Override
    public void actionPerformedLSP(AnActionEvent e, EditorEventManager manager, PsiFile psiFile) {
        val editor = manager.editor;
        val project = manager.getProject();
        VirtualFile virFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (!IntellijLanguageClient.isExtensionSupported(virFile)) {
            wrapped.actionPerformed(e);
            return;
        }
        boolean hasSelection = editor.getSelectionModel().hasSelection();
        LayoutCodeDialog dialog = new LayoutCodeDialog(project, psiFile, hasSelection, HELP_ID);
        dialog.show();
        if (!dialog.isOK()) {
            // if user chose cancel , the dialog in super.actionPerformed(e) will show again
            // super.actionPerformed(e);
            return;
        }

        LayoutCodeOptions options = dialog.getRunOptions();
        EditorEventManager eventManager = EditorEventManagerBase.forEditor(editor);
        if (eventManager != null) {
            if (options.getTextRangeType() == TextRangeType.SELECTED_TEXT) {
                eventManager.reformatSelection();
            } else {
                eventManager.reformat();
            }
        }
    }
}

