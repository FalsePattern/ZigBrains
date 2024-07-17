package com.falsepattern.zigbrains.zig.formatter;

import com.intellij.formatting.FormattingRangesInfo;
import com.intellij.formatting.service.FormattingService;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.LanguageServerItem;
import com.redhat.devtools.lsp4ij.features.formatting.AbstractLSPFormattingService;
import com.redhat.devtools.lsp4ij.features.formatting.LSPFormattingAndRangeBothService;
import org.eclipse.lsp4j.ServerCapabilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class ZigLSPFormattingAndRangeBothServiceProxy extends ZigAbstractLSPFormattingServiceProxy {
    @Override
    protected AbstractLSPFormattingService getProxiedService() {
        return FormattingService.EP_NAME.findExtension(LSPFormattingAndRangeBothService.class);
    }

    @Override
    protected boolean canSupportFormatting(@Nullable ServerCapabilities serverCapabilities) {
        return LanguageServerItem.isDocumentRangeFormattingSupported(serverCapabilities);
    }
}
