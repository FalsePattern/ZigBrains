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
package com.falsepattern.zigbrains.lsp.client;

import com.falsepattern.zigbrains.lsp.client.languageserver.requestmanager.RequestManager;
import com.falsepattern.zigbrains.lsp.client.languageserver.wrapper.LanguageServerWrapper;
import com.falsepattern.zigbrains.lsp.editor.EditorEventManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerWrapperBaseClientContext implements ClientContext {

    private final LanguageServerWrapper wrapper;

    public ServerWrapperBaseClientContext(@NotNull LanguageServerWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public EditorEventManager getEditorEventManagerFor(@NotNull String documentUri) {
        return wrapper.getEditorManagerFor(documentUri);
    }

    @Nullable
    @Override
    public Project getProject() {
        return wrapper.getProject();
    }

    @Nullable
    @Override
    public RequestManager getRequestManager() {
        return wrapper.getRequestManager();
    }
}
