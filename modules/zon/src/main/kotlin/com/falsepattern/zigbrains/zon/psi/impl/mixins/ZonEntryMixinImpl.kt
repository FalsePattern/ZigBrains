package com.falsepattern.zigbrains.zon.psi.impl.mixins

import com.falsepattern.zigbrains.zon.psi.ZonEntry
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class ZonEntryMixinImpl(node: ASTNode): ASTWrapperPsiElement(node), ZonEntry {
    override val keys: Set<String> get() {
        val struct = this.struct ?: return emptySet()
        return struct.propertyList.map { it.identifier.value }.toSet()
    }
}