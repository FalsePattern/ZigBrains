package com.falsepattern.zigbrains.zon.folding

import com.falsepattern.zigbrains.zon.psi.ZonStruct
import com.falsepattern.zigbrains.zon.psi.ZonVisitor
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class ZonFoldingBuilder: CustomFoldingBuilder(), DumbAware {
    override fun buildLanguageFoldRegions(
        descriptors: MutableList<FoldingDescriptor>,
        root: PsiElement,
        document: Document,
        quick: Boolean
    ) {
        root.accept(object: ZonVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                element.acceptChildren(this)
            }

            override fun visitStruct(o: ZonStruct) {
                super.visitStruct(o)
                descriptors.add(FoldingDescriptor(o, o.textRange))
            }
        })
    }

    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange) = ".{...}"

    override fun isRegionCollapsedByDefault(node: ASTNode) = false
}