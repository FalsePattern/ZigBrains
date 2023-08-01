package com.falsepattern.zigbrains.ide;

import org.eclipse.lsp4j.FoldingRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.contributors.LSPFoldingRangeProvider;

public class ZigFoldingRangeProvider extends LSPFoldingRangeProvider {
    @Override
    protected @Nullable String getCollapsedText(@NotNull FoldingRange foldingRange) {
        var text = super.getCollapsedText(foldingRange);
        if (text != null) {
            return text;
        }
        var kind = foldingRange.getKind();
        if (kind == null) {
            return "...";
        }
        return switch (kind) {
            case "comment" -> "///..."; //These are only done for doc comments. TODO figure out how to invoke the intellij doc renderer
            default -> "...";
        };
    }
}
