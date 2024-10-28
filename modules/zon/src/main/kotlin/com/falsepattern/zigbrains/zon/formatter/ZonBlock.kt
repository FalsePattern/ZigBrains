package com.falsepattern.zigbrains.zon.formatter

import com.falsepattern.zigbrains.zon.psi.ZonTypes
import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock

class ZonBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    private val spacingBuilder: SpacingBuilder
) : AbstractBlock(node, wrap, alignment) {

    override fun buildChildren(): MutableList<Block> {
        val blocks = ArrayList<Block>()
        var child = myNode.firstChildNode
        while (child != null) {
            if (child.elementType != TokenType.WHITE_SPACE) {
                blocks.add(ZonBlock(child, null, null, spacingBuilder))
            }
            child = child.treeNext
        }
        return blocks
    }

    override fun getIndent(): Indent {
        val parent = myNode.treeParent ?: return Indent.getNoneIndent()
        val elementType = myNode.elementType
        return if (parent.elementType == ZonTypes.ENTRY &&
            elementType != ZonTypes.DOT &&
            elementType != ZonTypes.LBRACE &&
            elementType != ZonTypes.RBRACE
        ) {
            Indent.getNormalIndent()
        } else {
            Indent.getNoneIndent()
        }
    }

    override fun getSpacing(child1: Block?, child2: Block) = null

    override fun isLeaf(): Boolean = myNode.firstChildNode == null

    override fun getChildIndent(): Indent {
        return if (myNode.elementType == ZonTypes.ENTRY) {
            Indent.getNormalIndent()
        } else {
            Indent.getNoneIndent()
        }
    }

}