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

import com.falsepattern.zigbrains.lsp.utils.DocumentUtils;
import com.falsepattern.zigbrains.lsp.utils.FileUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.backend.documentation.DocumentationLinkHandler;
import com.intellij.platform.backend.documentation.DocumentationTarget;
import com.intellij.platform.backend.documentation.LinkResolveResult;
import lombok.val;
import org.eclipse.lsp4j.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

import static com.intellij.codeInsight.documentation.DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL;

public class LSPDocumentationLinkHandler implements DocumentationLinkHandler {
    private static final String prefix = PSI_ELEMENT_PROTOCOL + "zigbrains://";
    private final static Logger LOG = Logger.getInstance(LSPDocumentationLinkHandler.class);
    @Override
    public @Nullable LinkResolveResult resolveLink(@NotNull DocumentationTarget target, @NotNull String url) {
        if (!url.startsWith(PSI_ELEMENT_PROTOCOL) || ! (target instanceof LSPDocumentationTargetProvider.LSPDocumentationTarget tgt)) {
            return null;
        }
        url = url.replace(prefix, "file://");
        val separator = url.indexOf("#L");
        if (separator < 0)
            return null;
        val link = url.substring(0, separator);
        final int line;
        {
            int theLine = 1;
            try {
                theLine = Integer.parseInt(url.substring(separator + 2));
            } catch (NumberFormatException e) {
                LOG.error("Could not parse file line: " + url.substring(separator + 2));
            }
            line = theLine - 1;
        }
        val app = ApplicationManager.getApplication();
        app.executeOnPooledThread(() -> {
            val project = tgt.file.getProject();
            VirtualFile file;
            try {
                file = VfsUtil.findFileByURL(new URL(link));
            } catch (MalformedURLException e1) {
                LOG.warn("Syntax Exception occurred for uri: " + link);
                return;
            }
            if (file == null)
                return;
            val descriptor = new OpenFileDescriptor(project, file);
            app.invokeLater(() -> {
                FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
                val editor = FileUtils.editorFromVirtualFile(file, project);
                if (editor == null) {
                    return;
                }
                val logicalPos = DocumentUtils.getTabsAwarePosition(editor, new Position(line, 0));
                if (logicalPos == null)
                    return;
                editor.getCaretModel().moveToLogicalPosition(logicalPos);
                editor.getScrollingModel().scrollTo(logicalPos, ScrollType.CENTER);
            });
        });
        return null;
    }
}
