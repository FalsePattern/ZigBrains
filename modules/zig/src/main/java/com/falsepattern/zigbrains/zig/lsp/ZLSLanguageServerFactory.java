package com.falsepattern.zigbrains.zig.lsp;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LanguageServerEnablementSupport;
import com.redhat.devtools.lsp4ij.LanguageServerFactory;
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl;
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures;
import com.redhat.devtools.lsp4ij.client.features.LSPFormattingFeature;
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider;
import org.jetbrains.annotations.NotNull;

public class ZLSLanguageServerFactory implements LanguageServerFactory, LanguageServerEnablementSupport {
    private boolean enabled = true;
    @Override
    public @NotNull StreamConnectionProvider createConnectionProvider(@NotNull Project project) {
        return new ZLSStreamConnectionProvider(project);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public @NotNull LSPClientFeatures createClientFeatures() {
        return new LSPClientFeatures()
                .setFormattingFeature(new LSPFormattingFeature() {
                    @Override
                    protected boolean isExistingFormatterOverrideable(@NotNull PsiFile file) {
                        return true;
                    }
                });
    }

    @Override
    public @NotNull LanguageClientImpl createLanguageClient(@NotNull Project project) {
        return new ZLSLanguageClient(project);
    }

    @Override
    public @NotNull Class<ZLSLanguageServer> getServerInterface() {
        return ZLSLanguageServer.class;
    }

    @Override
    public boolean isEnabled(@NotNull Project project) {
        return enabled && ZLSStreamConnectionProvider.doGetCommand(project, false) != null;
    }

    @Override
    public void setEnabled(boolean enabled, @NotNull Project project) {
        this.enabled = enabled;
    }
}
