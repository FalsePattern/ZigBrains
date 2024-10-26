package com.falsepattern.zigbrains.zig.editing;

import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral;
import com.falsepattern.zigbrains.zig.psi.ZigTypes;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RequiredArgsConstructor
public abstract class ZigMultiLineAssistant<T extends PsiElement> {
    public static class Assistants {
        private Assistants() {}

        public static final List<ZigMultiLineAssistant<?>> ASSISTANTS = List.of(new StringAssistant(),
                                                                                new CommentAssistant("//", ZigTypes.LINE_COMMENT),
                                                                                new CommentAssistant("///", ZigTypes.DOC_COMMENT),
                                                                                new CommentAssistant("//!", ZigTypes.CONTAINER_DOC_COMMENT));
    }
    public final String prefix;

    public abstract T acceptPSI(@NotNull PsiElement element);

    public static class StringAssistant extends ZigMultiLineAssistant<ZigStringLiteral> {
        public StringAssistant() {
            super("\\\\");
        }

        @Override
        public ZigStringLiteral acceptPSI(final @NotNull PsiElement element) {
            final PsiElement candidate;
            if (element instanceof LeafPsiElement leaf) {
                if (leaf.getElementType() == ZigTypes.STRING_LITERAL_MULTI) {
                    candidate = leaf.getParent();
                } else {
                    return null;
                }
            } else {
                candidate = element;
            }
            return candidate instanceof ZigStringLiteral str && str.isMultiLine() ? str : null;
        }
    }

    public static class CommentAssistant extends ZigMultiLineAssistant<PsiComment> {
        private final IElementType tokenType;
        public CommentAssistant(String prefix, IElementType type) {
            super(prefix);
            tokenType = type;
        }

        @Override
        public PsiComment acceptPSI(@NotNull PsiElement element) {
            return element instanceof PsiComment comment && comment.getTokenType() == tokenType ? comment : null;
        }
    }
}
