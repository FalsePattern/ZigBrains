package com.falsepattern.zigbrains.zon.psi.mixins

import com.intellij.psi.PsiElement

interface ZonEntryMixin: PsiElement {
    val keys: Set<String>
}