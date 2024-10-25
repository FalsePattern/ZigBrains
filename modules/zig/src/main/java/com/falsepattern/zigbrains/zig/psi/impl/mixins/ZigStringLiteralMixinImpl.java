package com.falsepattern.zigbrains.zig.psi.impl.mixins;

import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral;
import com.falsepattern.zigbrains.zig.util.PsiTextUtil;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.tree.LeafElement;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.experimental.UtilityClass;
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
    public @NotNull List<Pair<TextRange, String>> getDecodeReplacements(@NotNull CharSequence input) {
        if (isMultiLine())
            return List.of();

        val result = new ArrayList<Pair<TextRange, String>>();
        for (int i = 0; i + 1 < input.length(); i++) {
            if (input.charAt(i) == '\\') {
                val length = Escaper.findEscapementLength(input, i);
                val charCode = Escaper.toUnicodeChar(input, i, length);
                val range = TextRange.create(i, Math.min(i + length + 1, input.length()));
                result.add(Pair.create(range, Character.toString(charCode)));
                i += range.getLength() - 1;
            }
        }
        return result;
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

    private static @NotNull String processReplacements(@NotNull CharSequence input,
                                                       @NotNull List<? extends Pair<TextRange, String>> replacements) throws IndexOutOfBoundsException {
        StringBuilder result = new StringBuilder();
        int currentOffset = 0;
        for (val replacement: replacements) {
            result.append(input.subSequence(currentOffset, replacement.getFirst().getStartOffset()));
            result.append(replacement.getSecond());
            currentOffset = replacement.getFirst().getEndOffset();
        }
        result.append(input.subSequence(currentOffset, input.length()));
        return result.toString();
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
                    outChars.append(isMultiline ? substring : processReplacements(substring, myHost.getDecodeReplacements(substring)));
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

                    final List<Pair<TextRange, String>> replacementsForThisLine = myHost.getDecodeReplacements(curString);
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
                return !myHost.isMultiLine();
            }
        };
    }

    @UtilityClass
    private static class Escaper {
        private static final Int2IntMap ESC_TO_CODE = new Int2IntOpenHashMap();
        static {
            ESC_TO_CODE.put('n', '\n');
            ESC_TO_CODE.put('r', '\r');
            ESC_TO_CODE.put('t', '\t');
            ESC_TO_CODE.put('\\', '\\');
            ESC_TO_CODE.put('"', '"');
            ESC_TO_CODE.put('\'', '\'');
        }
        static int findEscapementLength(@NotNull CharSequence text, int pos) {
            if (pos + 1 < text.length() && text.charAt(pos) == '\\') {
                char c = text.charAt(pos + 1);
                return switch (c) {
                    case 'x' -> 3;
                    case 'u' -> {
                        if (pos + 3 >= text.length() || text.charAt(pos + 2) != '{') {
                            throw new IllegalArgumentException("Invalid unicode escape sequence");
                        }
                        int digits = 0;
                        while (pos + 3 + digits < text.length() && text.charAt(pos + 3 + digits) != '}') {
                            digits++;
                        }
                        yield 3 + digits;
                    }
                    default -> 1;
                };
            } else {
                throw new IllegalArgumentException("This is not an escapement start");
            }
        }

        static int toUnicodeChar(@NotNull CharSequence text, int pos, int length) {
            if (length > 1) {
                val s = switch (text.charAt(pos + 1)) {
                    case 'x' -> text.subSequence(pos + 2, Math.min(text.length(), pos + length + 1));
                    case 'u' -> text.subSequence(pos + 3, Math.min(text.length(), pos + length));
                    default -> throw new AssertionError();
                };
                try {
                    return Integer.parseInt(s.toString(), 16);
                } catch (NumberFormatException e) {
                    return 63;
                }
            } else {
                val c = text.charAt(pos + 1);
                return ESC_TO_CODE.getOrDefault(c, c);
            }
        }
    }
}
