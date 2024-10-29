package com.falsepattern.zigbrains.zig.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType
import com.falsepattern.zigbrains.zig.psi.ZigTypes.*

class ZigBlock(
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
                blocks.add(ZigBlock(child, null, null, spacingBuilder))
            }
            child = child.treeNext
        }

        return blocks
    }

    override fun getChildIndent(): Indent {
        val node = this.node
        return getIndentBasedOnParentType(node, null, node.elementType, PLACEHOLDER)
    }

    override fun getIndent(): Indent {
        val node = this.node
        val parent = node.treeParent ?: return noneIndent
        return getIndentBasedOnParentType(parent, node, parent.elementType, node.elementType)
    }
}

private fun getIndentBasedOnParentType(
    parent: ASTNode,
    child: ASTNode?,
    parentType: IElementType,
    childType: IElementType
): Indent {
    //Statement blocks
    if (parentType == BLOCK && !childType.isBrace)
        return normalIndent

    //Struct/tuple initializers
    if (parentType == INIT_LIST && !childType.isBrace)
        return normalIndent

    //Function call args
    if (parentType == EXPR_LIST ||
        parentType == FN_CALL_ARGUMENTS && childType === PLACEHOLDER
    )
        return normalIndent

    //Function declaration parameters
    if (parentType == PARAM_DECL_LIST ||
        parentType == FN_PROTO && childType === PLACEHOLDER
    )
        return normalIndent

    //Chained operations on newlines
    if ((parentType == BOOL_OR_EXPR ||
         parentType == BOOL_AND_EXPR ||
         parentType == COMPARE_EXPR ||
         parentType == BITWISE_EXPR ||
         parentType == BIT_SHIFT_EXPR ||
         parentType == ADDITION_EXPR ||
         parentType == MULTIPLY_EXPR) &&
        parent.firstChildNode != child
    )
        return normalIndent

    //Switch prongs
    if (parentType == SWITCH_PRONG_LIST ||
        parentType == SWITCH_EXPR && childType == PLACEHOLDER
    )
        return normalIndent

    //If expressions/statements
    if ((parentType == IF_EXPR ||
         parentType == IF_STATEMENT) &&
        childType != KEYWORD_ELSE &&
        childType != IF_PREFIX
    )
        return normalIndent

    //Struct members
    if (parentType == CONTAINER_DECL_AUTO &&
        childType != CONTAINER_DECL_TYPE &&
        childType != CONTAINER_DOC_COMMENT &&
        !childType.isBrace
    )
        return normalIndent

    //Inline assembly body
    if (parentType == ASM_EXPR &&
        childType != KEYWORD_ASM &&
        childType != KEYWORD_VOLATILE &&
        !childType.isParen
    )
        return normalIndent

    //Assembly params
    if (parentType == ASM_INPUT_LIST || parentType == ASM_OUTPUT_LIST)
        return spaceIndent(2)

    //Variable declarations
    if (parentType == VAR_DECL_EXPR_STATEMENT &&
        (childType == PLACEHOLDER ||
         child?.treePrevNonSpace?.elementType == EQUAL)
    )
        return normalIndent

    return noneIndent
}

private val ASTNode.treePrevNonSpace: ASTNode? get() {
    var it = this.treePrev
    while (it?.elementType == TokenType.WHITE_SPACE) {
        it = it.treePrev
    }
    return it
}

private val normalIndent: Indent get() = Indent.getNormalIndent()

private fun spaceIndent(spaces: Int) = Indent.getSpaceIndent(2)

private val noneIndent: Indent get() = Indent.getNoneIndent()

private val IElementType?.isBrace: Boolean
    get() {
        return this == LBRACE || this == RBRACE
    }

private val IElementType?.isParen: Boolean
    get() {
        return this == LPAREN || this == RPAREN
    }

private val PLACEHOLDER = IElementType("placeholder", null)