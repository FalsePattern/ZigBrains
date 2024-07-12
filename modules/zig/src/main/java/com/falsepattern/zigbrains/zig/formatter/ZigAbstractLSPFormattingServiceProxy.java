package com.falsepattern.zigbrains.zig.formatter;

import com.falsepattern.zigbrains.zig.ZigLanguage;
import com.intellij.formatting.FormattingRangesInfo;
import com.intellij.formatting.service.FormattingService;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LanguageServersRegistry;
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.lsp4ij.features.formatting.AbstractLSPFormattingService;
import lombok.val;
import org.eclipse.lsp4j.ServerCapabilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/*
TODO remove once https://github.com/redhat-developer/lsp4ij/issues/424 is resolved
 */
public abstract class ZigAbstractLSPFormattingServiceProxy implements FormattingService {
    protected abstract AbstractLSPFormattingService getProxiedService();
    protected abstract boolean canSupportFormatting(@Nullable ServerCapabilities var1);

    @Override
    public @NotNull Set<Feature> getFeatures() {
        return getProxiedService().getFeatures();
    }

    @Override
    public boolean canFormat(@NotNull PsiFile file) {
        val language = file.getLanguage();
        if (language != ZigLanguage.INSTANCE)
            return false;

        if (!LanguageServersRegistry.getInstance().isFileSupported(file)) {
            return false;
        } else {
            Project project = file.getProject();
            return LanguageServiceAccessor.getInstance(project).hasAny(file.getVirtualFile(), (ls) -> this.canSupportFormatting(ls.getServerCapabilitiesSync()));
        }
    }

    @Override
    public @NotNull PsiElement formatElement(@NotNull PsiElement psiElement, boolean b) {
        return getProxiedService().formatElement(psiElement, b);
    }

    @Override
    public @NotNull PsiElement formatElement(@NotNull PsiElement psiElement, @NotNull TextRange textRange, boolean b) {
        return getProxiedService().formatElement(psiElement, textRange, b);
    }

    @Override
    public void formatRanges(@NotNull PsiFile psiFile, FormattingRangesInfo formattingRangesInfo, boolean b, boolean b1) {
        getProxiedService().formatRanges(psiFile, formattingRangesInfo, b, b1);
    }

    @Override
    public @NotNull Set<ImportOptimizer> getImportOptimizers(@NotNull PsiFile psiFile) {
        return getProxiedService().getImportOptimizers(psiFile);
    }
}
