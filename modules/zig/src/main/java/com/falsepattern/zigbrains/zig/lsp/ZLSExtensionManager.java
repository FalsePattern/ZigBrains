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

package com.falsepattern.zigbrains.zig.lsp;

import com.falsepattern.zigbrains.lsp.client.ClientContext;
import com.falsepattern.zigbrains.lsp.client.languageserver.ServerOptions;
import com.falsepattern.zigbrains.lsp.client.languageserver.requestmanager.RequestManager;
import com.falsepattern.zigbrains.lsp.client.languageserver.wrapper.LanguageServerWrapper;
import com.falsepattern.zigbrains.lsp.extensions.LSPExtensionManager;
import com.falsepattern.zigbrains.lsp.listeners.EditorMouseListenerImpl;
import com.falsepattern.zigbrains.lsp.listeners.EditorMouseMotionListenerImpl;
import com.falsepattern.zigbrains.lsp.listeners.LSPCaretListenerImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentListener;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

public class ZLSExtensionManager implements LSPExtensionManager {
    @Override
    public ZLSRequestManager getExtendedRequestManagerFor(LanguageServerWrapper wrapper, LanguageServer server, LanguageClient client, ServerCapabilities serverCapabilities) {
        return new ZLSRequestManager(wrapper, server, client, serverCapabilities);
    }

    @Override
    public ZLSEditorEventManager getExtendedEditorEventManagerFor(Editor editor, DocumentListener documentListener, EditorMouseListenerImpl mouseListener, EditorMouseMotionListenerImpl mouseMotionListener, LSPCaretListenerImpl caretListener, RequestManager requestManager, ServerOptions serverOptions, LanguageServerWrapper wrapper) {
        return new ZLSEditorEventManager(editor, documentListener, mouseListener, mouseMotionListener,
                                             caretListener, requestManager, serverOptions, wrapper);
    }

    @Override
    public Class<? extends LanguageServer> getExtendedServerInterface() {
        return null;
    }

    @Override
    public LanguageClient getExtendedClientFor(ClientContext context) {
        return null;
    }
}
