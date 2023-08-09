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

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.PublishDiagnosticsCapabilities;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.RawCommandServerDefinition;

public class ZLSServerDefinition extends RawCommandServerDefinition {
    public ZLSServerDefinition(String[] command) {
        super("zig", command);
    }

    @Override
    public void customizeInitializeParams(InitializeParams params) {
        var textCaps = params.getCapabilities().getTextDocument();
        if (textCaps.getPublishDiagnostics() == null) {
            textCaps.setPublishDiagnostics(new PublishDiagnosticsCapabilities());
        }
    }
}
