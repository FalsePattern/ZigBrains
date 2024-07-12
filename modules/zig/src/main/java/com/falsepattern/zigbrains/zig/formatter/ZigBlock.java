/*
 * Copyright 2023-2024 FalsePattern
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.falsepattern.zigbrains.zig.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.falsepattern.zigbrains.zig.psi.ZigTypes.ADDITION_EXPR;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.ASM_EXPR;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.ASM_INPUT_LIST;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.ASM_OUTPUT_LIST;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.BITWISE_EXPR;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.BIT_SHIFT_EXPR;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.BLOCK;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.BOOL_AND_EXPR;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.BOOL_OR_EXPR;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.COMPARE_EXPR;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.CONTAINER_DECL_AUTO;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.CONTAINER_DECL_TYPE;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.CONTAINER_DOC_COMMENT;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.EXPR_LIST;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.FN_CALL_ARGUMENTS;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.FN_PROTO;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.IF_EXPR;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.IF_PREFIX;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.IF_STATEMENT;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.INIT_LIST;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.KEYWORD_ASM;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.KEYWORD_ELSE;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.KEYWORD_VOLATILE;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.LBRACE;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.LPAREN;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.MULTIPLY_EXPR;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.PARAM_DECL_LIST;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.RBRACE;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.RPAREN;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.SWITCH_EXPR;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.SWITCH_PRONG_LIST;

public class ZigBlock extends AbstractBlock {

    private final SpacingBuilder spacingBuilder;

    protected ZigBlock(@NotNull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment, SpacingBuilder spacingBuilder) {
        super(node, wrap, alignment);
        this.spacingBuilder = spacingBuilder;
    }


    @Override
    protected List<Block> buildChildren() {
        var blocks = new ArrayList<Block>();
        var child = myNode.getFirstChildNode();
        while (child != null) {
            if (child.getElementType() != TokenType.WHITE_SPACE) {
                var block = new ZigBlock(child, null, null, spacingBuilder);
                blocks.add(block);
            }
            child = child.getTreeNext();
        }

        return blocks;
    }

    @Override
    public @Nullable Spacing getSpacing(@Nullable Block block, @NotNull Block block1) {
        return null;
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }

    private static final IElementType PLACEHOLDER = new IElementType("placeholder", null);

    @Override
    protected @Nullable Indent getChildIndent() {
        val node = getNode();
        return getIndentBasedOnParentType(node, null, node.getElementType(), PLACEHOLDER);
    }

    @Override
    public Indent getIndent() {
        val node = getNode();
        val parent = node.getTreeParent();
        if (parent != null) {
            return getIndentBasedOnParentType(parent, node, parent.getElementType(), node.getElementType());
        }
        return Indent.getNoneIndent();
    }

    private static boolean isBrace(IElementType element) {
        return element == LBRACE || element == RBRACE;
    }

    private static boolean isParen(IElementType element) {
        return element == LPAREN || element == RPAREN;
    }

    private static boolean isEmpty(ASTNode child) {
        return child.getFirstChildNode() == null;
    }


    private static @NotNull Indent getIndentBasedOnParentType(@NotNull ASTNode parent, @Nullable ASTNode child, @NotNull IElementType parentElementType, @NotNull IElementType childElementType) {
        //Statement blocks
        if (parentElementType == BLOCK && !isBrace(childElementType))
            return Indent.getNormalIndent();

        //Struct/tuple initializers
        if (parentElementType == INIT_LIST && !isBrace(childElementType))
            return Indent.getNormalIndent();

        //Function call args
        if (parentElementType == EXPR_LIST ||
            parentElementType == FN_CALL_ARGUMENTS && childElementType == PLACEHOLDER)
            return Indent.getNormalIndent();

        //Function declaration parameters
        if (parentElementType == PARAM_DECL_LIST ||
            parentElementType == FN_PROTO && childElementType == PLACEHOLDER)
            return Indent.getNormalIndent();

        //Chained operations on newlines
        if ((parentElementType == BOOL_OR_EXPR ||
            parentElementType == BOOL_AND_EXPR ||
            parentElementType == COMPARE_EXPR ||
            parentElementType == BITWISE_EXPR ||
            parentElementType == BIT_SHIFT_EXPR ||
            parentElementType == ADDITION_EXPR ||
            parentElementType == MULTIPLY_EXPR) &&
            parent.getFirstChildNode() != child)
            return Indent.getNormalIndent();

        //Switch prongs
        if (parentElementType == SWITCH_PRONG_LIST ||
            parentElementType == SWITCH_EXPR && childElementType == PLACEHOLDER)
            return Indent.getNormalIndent();

        //If expressions/statements
        if ((parentElementType == IF_EXPR || parentElementType == IF_STATEMENT) && childElementType != KEYWORD_ELSE && childElementType != IF_PREFIX)
            return Indent.getNormalIndent();

        //Struct members
        if (parentElementType == CONTAINER_DECL_AUTO && childElementType != CONTAINER_DECL_TYPE && childElementType != CONTAINER_DOC_COMMENT && !isBrace(childElementType))
            return Indent.getNormalIndent();

        //Inline assembly body
        if (parentElementType == ASM_EXPR && childElementType != KEYWORD_ASM && childElementType != KEYWORD_VOLATILE && !isParen(childElementType))
            return Indent.getNormalIndent();

        //Assembly params
        if (parentElementType == ASM_INPUT_LIST || parentElementType == ASM_OUTPUT_LIST)
            return Indent.getSpaceIndent(2);

        return Indent.getNoneIndent();
    }
}
