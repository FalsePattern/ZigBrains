package com.falsepattern.zigbrains.lsp;

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
