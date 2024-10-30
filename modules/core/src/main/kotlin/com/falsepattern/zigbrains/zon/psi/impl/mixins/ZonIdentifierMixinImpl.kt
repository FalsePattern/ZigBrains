package com.falsepattern.zigbrains.zon.psi.impl.mixins

import com.falsepattern.zigbrains.zon.psi.ZonIdentifier
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class ZonIdentifierMixinImpl(node: ASTNode): ASTWrapperPsiElement(node), ZonIdentifier {
    override val value: String get() {
        val text = this.text!!
        return if (text.startsWith('@'))
            text.substring(2, text.length - 1)
        else
            text
    }
}
