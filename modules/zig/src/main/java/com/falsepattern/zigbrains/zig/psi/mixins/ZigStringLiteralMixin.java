package com.falsepattern.zigbrains.zig.psi.mixins;

import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.tree.LeafElement;
import org.jetbrains.annotations.NotNull;

public interface ZigStringLiteralMixin extends PsiLanguageInjectionHost {
}