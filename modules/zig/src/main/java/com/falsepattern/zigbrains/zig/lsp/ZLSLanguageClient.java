package com.falsepattern.zigbrains.zig.lsp;

import com.intellij.openapi.project.Project;
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl;

import java.util.concurrent.CompletableFuture;

public class ZLSLanguageClient extends LanguageClientImpl {
    public ZLSLanguageClient(Project project) {
        super(project);
    }
}
