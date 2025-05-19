/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2025 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * ZigBrains is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * ZigBrains is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ZigBrains. If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.zigbrains.zon.formatter

import com.falsepattern.zigbrains.zon.psi.ZonTypes
import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.Indent
import com.intellij.formatting.SpacingBuilder
import com.intellij.formatting.Wrap
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType

class ZonBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    val spacingBuilder: SpacingBuilder
) : AbstractBlock(node, wrap, alignment) {

    override fun getSpacing(child1: Block?, child2: Block) = null

    override fun isLeaf() = myNode.firstChildNode == null

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

    override fun getChildIndent(): Indent {
        val node = this.node
        return getIndentBasedOnParentType(node.elementType, PLACEHOLDER)
    }

    override fun getIndent(): Indent {
        val node = this.node
        val parent = node.treeParent ?: return noneIndent
        return getIndentBasedOnParentType(parent.elementType, node.elementType)
    }
}
private fun getIndentBasedOnParentType(
    parentType: IElementType,
    childType: IElementType
): Indent {
    //Struct/tuple initializers
    if (parentType == ZonTypes.INIT_LIST && !childType.isBrace)
        return normalIndent

    return noneIndent
}

private val normalIndent: Indent get() = Indent.getNormalIndent()

private val noneIndent: Indent get() = Indent.getNoneIndent()

private val IElementType?.isBrace: Boolean
    get() {
        return this == ZonTypes.LBRACE || this == ZonTypes.RBRACE
    }

private val PLACEHOLDER = IElementType("placeholder", null)