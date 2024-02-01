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

package com.falsepattern.zigbrains.zon.formatter;

import com.falsepattern.zigbrains.zon.psi.ZonTypes;
import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.formatter.common.AbstractBlock;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ZonBlock extends AbstractBlock {
    private final SpacingBuilder spacingBuilder;

    protected ZonBlock(@NotNull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment, SpacingBuilder spacingBuilder) {
        super(node, wrap, alignment);
        this.spacingBuilder = spacingBuilder;
    }

    @Override
    protected List<Block> buildChildren() {
        var blocks = new ArrayList<Block>();
        var child = myNode.getFirstChildNode();
        while (child != null) {
            if (child.getElementType() != TokenType.WHITE_SPACE) {
                var block = new ZonBlock(child, null, null, spacingBuilder);
                blocks.add(block);
            }
            child = child.getTreeNext();
        }
        return blocks;
    }

    @Override
    public Indent getIndent() {
        val parent = myNode.getTreeParent();
        if (parent == null)
            return Indent.getNoneIndent();

        val myElementType = myNode.getElementType();
        if (parent.getElementType() == ZonTypes.STRUCT &&
            !(myElementType == ZonTypes.DOT ||
              myElementType == ZonTypes.LBRACE ||
              myElementType == ZonTypes.RBRACE)) {
            return Indent.getNormalIndent();
        } else {
            return Indent.getNoneIndent();
        }
    }


    @Override
    public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return null;
    }

    @Override
    public boolean isLeaf() {

        return myNode.getFirstChildNode() == null;
    }

    @Override
    protected @Nullable Indent getChildIndent() {
        if (myNode.getElementType() == ZonTypes.STRUCT) {
            return Indent.getNormalIndent();
        } else {
            return Indent.getNoneIndent();
        }
    }
}
