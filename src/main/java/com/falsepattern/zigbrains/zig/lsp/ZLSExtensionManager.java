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

package com.falsepattern.zigbrains.zig.lsp;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentListener;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.wso2.lsp4intellij.client.ClientContext;
import org.wso2.lsp4intellij.client.languageserver.ServerOptions;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.DefaultRequestManager;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.extensions.LSPExtensionManager;
import org.wso2.lsp4intellij.listeners.EditorMouseListenerImpl;
import org.wso2.lsp4intellij.listeners.EditorMouseMotionListenerImpl;
import org.wso2.lsp4intellij.listeners.LSPCaretListenerImpl;

// There's a couple unchecked casts here, because LSPExtensionManager has generics where it shouldn't,
// but we have to live with it for now, I guess...
@SuppressWarnings("unchecked")
public class ZLSExtensionManager implements LSPExtensionManager {
    @Override
    public <T extends DefaultRequestManager> T getExtendedRequestManagerFor(LanguageServerWrapper wrapper, LanguageServer server, LanguageClient client, ServerCapabilities serverCapabilities) {
        return (T) new ZLSRequestManager(wrapper, server, client, serverCapabilities);
    }

    @Override
    public <T extends EditorEventManager> T getExtendedEditorEventManagerFor(Editor editor, DocumentListener documentListener, EditorMouseListenerImpl mouseListener, EditorMouseMotionListenerImpl mouseMotionListener, LSPCaretListenerImpl caretListener, RequestManager requestManager, ServerOptions serverOptions, LanguageServerWrapper wrapper) {
        return (T) new ZLSEditorEventManager(editor, documentListener, mouseListener, mouseMotionListener,
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