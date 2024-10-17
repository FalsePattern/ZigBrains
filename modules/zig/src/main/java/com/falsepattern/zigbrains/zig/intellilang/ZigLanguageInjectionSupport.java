package com.falsepattern.zigbrains.zig.intellilang;

import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral;
import com.intellij.psi.PsiLanguageInjectionHost;
import org.intellij.plugins.intelliLang.inject.AbstractLanguageInjectionSupport;
import org.jetbrains.annotations.NotNull;

public class ZigLanguageInjectionSupport extends AbstractLanguageInjectionSupport {
    @Override
    public @NotNull String getId() {
        return "zig";
    }

    @Override
    public Class<?> @NotNull [] getPatternClasses() {
        return new Class[0];
    }

    @Override
    public boolean isApplicableTo(PsiLanguageInjectionHost host) {
        return host instanceof ZigStringLiteral;
    }
}
