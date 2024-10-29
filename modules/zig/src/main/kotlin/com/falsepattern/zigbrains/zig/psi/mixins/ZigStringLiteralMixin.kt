package com.falsepattern.zigbrains.zig.psi.mixins

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiLanguageInjectionHost

interface ZigStringLiteralMixin: PsiLanguageInjectionHost {
    val isMultiline: Boolean
    val contentRanges: List<TextRange>
}