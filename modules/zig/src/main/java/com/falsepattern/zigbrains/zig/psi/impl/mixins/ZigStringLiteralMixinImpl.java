package com.falsepattern.zigbrains.zig.psi.impl.mixins;

import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral;
import com.falsepattern.zigbrains.zig.util.PsiTextUtil;
import com.falsepattern.zigbrains.zig.util.ZigStringUtil;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.tree.LeafElement;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class ZigStringLiteralMixinImpl extends ASTWrapperPsiElement implements ZigStringLiteral {
    public ZigStringLiteralMixinImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public boolean isValidHost() {
        return true;
    }


    @Override
    public boolean isMultiLine() {
        return getStringLiteralMulti() != null;
    }

    @Override
    public @NotNull List<TextRange> getContentRanges() {
        if (!isMultiLine()) {
            return List.of(new TextRange(1, getTextLength() - 1));
        } else {
            return PsiTextUtil.getMultiLineContent(getText(), "\\\\");
        }
    }

    @Override
    public PsiLanguageInjectionHost updateText(@NotNull String text) {
        if (this.getStringLiteralSingle() instanceof LeafElement leaf) {
            leaf.replaceWithText(text);
        } else if (this.getStringLiteralMulti() instanceof LeafElement leaf) {
            leaf.replaceWithText(text);
        }
        return this;
    }

    @Override
    public @NotNull LiteralTextEscaper<ZigStringLiteral> createLiteralTextEscaper() {

        return new LiteralTextEscaper<>(this) {
            private String text;
            private List<TextRange> contentRanges;
            @Override
            public boolean decode(@NotNull TextRange rangeInsideHost, @NotNull StringBuilder outChars) {
                text = myHost.getText();
                val isMultiline = myHost.isMultiLine();
                contentRanges = myHost.getContentRanges();
                boolean decoded = false;
                for (val range: contentRanges) {
                    val intersection = range.intersection(rangeInsideHost);
                    if (intersection == null) continue;
                    decoded = true;
                    val substring = intersection.subSequence(text);
                    outChars.append(ZigStringUtil.unescape(substring, isMultiline));
                }
                return decoded;
            }

            @Override
            public @NotNull TextRange getRelevantTextRange() {
                if (contentRanges == null) {
                    contentRanges = myHost.getContentRanges();
                }
                return PsiTextUtil.getTextRangeBounds(contentRanges);
            }

            @Override
            public int getOffsetInHost(int offsetInDecoded, @NotNull TextRange rangeInsideHost) {
                int currentOffsetInDecoded = 0;

                TextRange last = null;
                for (int i = 0; i < contentRanges.size(); i++) {
                    final TextRange range = rangeInsideHost.intersection(contentRanges.get(i));
                    if (range == null) continue;
                    last = range;

                    String curString = range.subSequence(text).toString();

                    val replacementsForThisLine = ZigStringUtil.getDecodeReplacements(curString, myHost.isMultiLine());
                    int encodedOffsetInCurrentLine = 0;
                    for (Pair<TextRange, String> replacement : replacementsForThisLine) {
                        final int deltaLength = replacement.getFirst().getStartOffset() - encodedOffsetInCurrentLine;
                        int currentOffsetBeforeReplacement = currentOffsetInDecoded + deltaLength;
                        if (currentOffsetBeforeReplacement > offsetInDecoded) {
                            return range.getStartOffset() + encodedOffsetInCurrentLine + (offsetInDecoded - currentOffsetInDecoded);
                        }
                        else if (currentOffsetBeforeReplacement == offsetInDecoded && !replacement.getSecond().isEmpty()) {
                            return range.getStartOffset() + encodedOffsetInCurrentLine + (offsetInDecoded - currentOffsetInDecoded);
                        }
                        currentOffsetInDecoded += deltaLength + replacement.getSecond().length();
                        encodedOffsetInCurrentLine += deltaLength + replacement.getFirst().getLength();
                    }

                    final int deltaLength = curString.length() - encodedOffsetInCurrentLine;
                    if (currentOffsetInDecoded + deltaLength > offsetInDecoded) {
                        return range.getStartOffset() + encodedOffsetInCurrentLine + (offsetInDecoded - currentOffsetInDecoded);
                    }
                    currentOffsetInDecoded += deltaLength;
                }

                return last != null ? last.getEndOffset() : -1;
            }

            @Override
            public boolean isOneLine() {
                return true;
            }
        };
    }
}
