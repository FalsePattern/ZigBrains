package com.falsepattern.zigbrains.zig.lsp;

import com.falsepattern.zigbrains.zig.settings.ZLSProjectSettingsService;
import com.falsepattern.zigbrains.zig.settings.ZLSSettings;
import com.falsepattern.zigbrains.zig.settings.ZLSSettingsConfigProvider;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LanguageServerEnablementSupport;
import com.redhat.devtools.lsp4ij.LanguageServerFactory;
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl;
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures;
import com.redhat.devtools.lsp4ij.client.features.LSPFormattingFeature;
import com.redhat.devtools.lsp4ij.client.features.LSPInlayHintFeature;
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider;
import com.redhat.devtools.lsp4ij.server.capabilities.InlayHintCapabilityRegistry;
import lombok.val;
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
        val features = new LSPClientFeatures();
        features.setFormattingFeature(new LSPFormattingFeature() {
            @Override
            protected boolean isExistingFormatterOverrideable(@NotNull PsiFile file) {
                return true;
            }
        });
        features.setInlayHintFeature(new LSPInlayHintFeature() {
            @Override
            public boolean isEnabled(@NotNull PsiFile file) {
                return ZLSProjectSettingsService.getInstance(features.getProject()).getState().inlayHints;
            }
        });
        return features;
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
        return enabled && ZLSStreamConnectionProvider.getCommandSync(project, false) != null;
    }

    @Override
    public void setEnabled(boolean enabled, @NotNull Project project) {
        this.enabled = enabled;
    }
}
