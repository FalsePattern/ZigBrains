package com.falsepattern.zigbrains.zig.psi.mixins;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiLanguageInjectionHost;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ZigStringLiteralMixin extends PsiLanguageInjectionHost {
    boolean isMultiLine();
    @NotNull List<TextRange> getContentRanges();
}
