package com.falsepattern.zigbrains.zon.psi.mixins

import com.intellij.psi.PsiElement

interface ZonIdentifierMixin: PsiElement {
    val value: String
}