package com.falsepattern.zigbrains.zig.formatter;

import com.intellij.formatting.service.FormattingService;
import com.redhat.devtools.lsp4ij.LanguageServerItem;
import com.redhat.devtools.lsp4ij.features.formatting.AbstractLSPFormattingService;
import com.redhat.devtools.lsp4ij.features.formatting.LSPFormattingAndRangeBothService;
import org.eclipse.lsp4j.ServerCapabilities;
import org.jetbrains.annotations.Nullable;

/*
TODO remove once https://github.com/redhat-developer/lsp4ij/issues/424 is resolved
 */
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
