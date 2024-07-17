package com.falsepattern.zigbrains.zig.formatter;

import com.intellij.formatting.service.FormattingService;
import com.redhat.devtools.lsp4ij.LanguageServerItem;
import com.redhat.devtools.lsp4ij.features.formatting.AbstractLSPFormattingService;
import com.redhat.devtools.lsp4ij.features.formatting.LSPFormattingAndRangeBothService;
import com.redhat.devtools.lsp4ij.features.formatting.LSPFormattingOnlyService;
import org.eclipse.lsp4j.ServerCapabilities;
import org.jetbrains.annotations.Nullable;

public class ZigLSPFormattingOnlyServiceProxy extends ZigAbstractLSPFormattingServiceProxy {
    @Override
    protected AbstractLSPFormattingService getProxiedService() {
        return FormattingService.EP_NAME.findExtension(LSPFormattingOnlyService.class);
    }

    @Override
    protected boolean canSupportFormatting(@Nullable ServerCapabilities serverCapabilities) {
        return LanguageServerItem.isDocumentFormattingSupported(serverCapabilities) && !LanguageServerItem.isDocumentRangeFormattingSupported(serverCapabilities);
    }
}
