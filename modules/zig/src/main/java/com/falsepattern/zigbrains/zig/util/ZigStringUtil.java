package com.falsepattern.zigbrains.zig.util;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ZigStringUtil {
    public static String escape(String input) {
        return input.codePoints().mapToObj(point -> switch (point) {
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            case '\\' -> "\\\\";
            case '"' -> "\\\"";
            case '\'', ' ', '!' -> Character.toString(point);
            default -> {
                if (point >= '#' && point <= '&' ||
                    point >= '(' && point <= '[' ||
                    point >= ']' && point <= '~') {
                    yield Character.toString(point);
                } else {
                    yield "\\u{" + Integer.toHexString(point).toLowerCase() + "}";
                }
            }
        }).collect(Collectors.joining(""));
    }

    public static List<Pair<TextRange, String>> getDecodeReplacements(@NotNull CharSequence input, boolean isMultiline) {
        if (isMultiline) {
            return List.of();
        }

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

    public static String unescape(@NotNull CharSequence input, boolean isMultiline) {
        return isMultiline ? input.toString() : processReplacements(input, getDecodeReplacements(input, false));
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

    private static final Pattern NL_MATCHER = Pattern.compile("(\\r\\n|\\r|\\n)");
    private static final String[] COMMON_INDENTS;
    static {
        val count = 32;
        val sb = new StringBuilder(count);
        COMMON_INDENTS = new String[count];
        for (int i = 0; i < count; i++) {
            COMMON_INDENTS[i] = sb.toString();
            sb.append(" ");
        }
    }

    public static CharSequence prefixWithTextBlockEscape(int indent, CharSequence marker, CharSequence content, boolean indentFirst, boolean prefixFirst) {
        val indentStr = indent >= 0 ? indent < COMMON_INDENTS.length ? COMMON_INDENTS[indent] : " ".repeat(indent) : "";
        val parts = Arrays.asList(NL_MATCHER.split(content, -1));
        val result = new StringBuilder(content.length() + marker.length() * parts.size() + indentStr.length() * parts.size());
        if (indentFirst) {
            result.append(indentStr);
        }
        if (prefixFirst) {
            result.append(marker);
        }
        result.append(parts.getFirst());
        for (val part: parts.subList(1, parts.size())) {
            result.append("\n").append(indentStr).append(marker).append(part);
        }
        return result;
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
