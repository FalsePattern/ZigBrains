package com.falsepattern.zigbrains.zig.psi.impl.mixins;

import com.falsepattern.zigbrains.zig.psi.ZigStringElementManipulator;
import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
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
        if (this.getStringLiteralSingle() != null) {
            return new LiteralTextEscaper<>(this) {
                private final List<Integer> inputOffsets = new ArrayList<>();
                @Override
                public boolean decode(@NotNull TextRange rangeInsideHost, @NotNull StringBuilder outChars) {
                    boolean[] noErrors = new boolean[] {true};
                    outChars.append(ZigStringElementManipulator.unescapeWithLengthMappings(rangeInsideHost.substring(myHost.getText()), inputOffsets, noErrors));
                    return noErrors[0];
                }

                @Override
                public int getOffsetInHost(int offsetInDecoded, @NotNull TextRange rangeInsideHost) {
                    int size = inputOffsets.size();
                    int realOffset = 0;
                    if (size == 0) {
                        realOffset = rangeInsideHost.getStartOffset() + offsetInDecoded;
                    } else if (offsetInDecoded >= size) {
                        realOffset = rangeInsideHost.getStartOffset() + inputOffsets.get(size - 1) +
                                     (offsetInDecoded - (size - 1));
                    } else {
                        realOffset = rangeInsideHost.getStartOffset() + inputOffsets.get(offsetInDecoded);
                    }
                    return realOffset;
                }

                @Override
                public @NotNull TextRange getRelevantTextRange() {
                    return new TextRange(1, myHost.getTextLength() - 1);
                }

                @Override
                public boolean isOneLine() {
                    return true;
                }
            };
        } else if (this.getStringLiteralMulti() != null) {
            return new LiteralTextEscaper<>(this) {
                @Override
                public boolean decode(@NotNull TextRange rangeInsideHost, @NotNull StringBuilder outChars) {
                    val str = myHost.getText();
                    boolean inMultiLineString = false;
                    for (int i = 0; i < str.length(); i++) {
                        val cI = str.charAt(i);
                        if (!inMultiLineString) {
                            if (cI == '\\' &&
                                i + 1 < str.length() &&
                                str.charAt(i + 1) == '\\') {
                                i++;
                                inMultiLineString = true;
                            }
                            continue;
                        }
                        if (cI == '\r') {
                            outChars.append('\n');
                            if (i + 1 < str.length() && str.charAt(i + 1) == '\n') {
                                i++;
                            }
                            inMultiLineString = false;
                            continue;
                        }
                        if (cI == '\n') {
                            outChars.append('\n');
                            inMultiLineString = false;
                            continue;
                        }
                        outChars.append(cI);
                    }
                    return true;
                }

                @Override
                public int getOffsetInHost(int offsetInDecoded, @NotNull TextRange rangeInsideHost) {
                    val str = myHost.getText();
                    boolean inMultiLineString = false;
                    int i = rangeInsideHost.getStartOffset();
                    for (; i < rangeInsideHost.getEndOffset() && offsetInDecoded > 0; i++) {
                        val cI = str.charAt(i);
                        if (!inMultiLineString) {
                            if (cI == '\\' &&
                                i + 1 < str.length() &&
                                str.charAt(i + 1) == '\\') {
                                i++;
                                inMultiLineString = true;
                            }
                            continue;
                        }
                        if (cI == '\r') {
                            offsetInDecoded--;
                            if (i + 1 < str.length() && str.charAt(i + 1) == '\n') {
                                i++;
                            }
                            inMultiLineString = false;
                            continue;
                        }
                        if (cI == '\n') {
                            offsetInDecoded--;
                            inMultiLineString = false;
                            continue;
                        }
                        offsetInDecoded--;
                    }
                    if (offsetInDecoded != 0)
                        return -1;
                    return i;
                }

                @Override
                public boolean isOneLine() {
                    return false;
                }
            };
        } else {
            throw new AssertionError();
        }
    }
}
