package com.falsepattern.zigbrains.zig.formatter;

import com.falsepattern.zigbrains.zig.psi.ZigTypes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.WrapType;
import com.intellij.psi.TokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.psi.formatter.common.AbstractBlock;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected @Nullable Indent getChildIndent() {
        if (myNode.getElementType() == ZigTypes.BLOCK) {
            return Indent.getNormalIndent();
        }

        return Indent.getNoneIndent();
    }

    @Override
    public Indent getIndent() {
        return Indent.getNoneIndent();
    }
}
