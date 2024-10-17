package com.falsepattern.zigbrains.zig.psi;

import com.falsepattern.zigbrains.zig.ZigFileType;
import com.falsepattern.zigbrains.zig.util.PsiUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.IncorrectOperationException;
import lombok.SneakyThrows;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ZigStringElementManipulator extends AbstractElementManipulator<ZigStringLiteral> {


    @Override
    public @Nullable ZigStringLiteral handleContentChange(@NotNull ZigStringLiteral element, @NotNull TextRange range, String newContent)
            throws IncorrectOperationException {
        assert (new TextRange(0, element.getTextLength())).contains(range);
        val originalContext = element.getText();
        val isMulti = element.getStringLiteralMulti() != null;
        val elementRange = getRangeInElement(element);
        var replacement = originalContext.substring(elementRange.getStartOffset(),
                                                    range.getStartOffset()) +
                          (isMulti ? newContent : escape(newContent)) +
                          originalContext.substring(range.getEndOffset(),
                                                    elementRange.getEndOffset());
        val psiFileFactory = PsiFileFactory.getInstance(element.getProject());
        if (isMulti) {
            val column = StringUtil.offsetToLineColumn(element.getContainingFile().getText(), element.getTextOffset()).column;
            val pfxB = new StringBuilder(column + 2);
            for (int i = 0; i < column; i++) {
                pfxB.append(' ');
            }
            pfxB.append("\\\\");
            val pfx = pfxB.toString();
            replacement = Arrays.stream(replacement.split("(\\r\\n|\\r|\\n)")).map(line -> pfx + line).collect(
                    Collectors.joining("\n"));
        } else {
            replacement = "\"" + replacement + "\"";
        }
        val dummy = psiFileFactory.createFileFromText("dummy." + ZigFileType.INSTANCE.getDefaultExtension(),
                                                      ZigFileType.INSTANCE, "const x = \n" + replacement + "\n;");
        val stringLiteral = ((ZigPrimaryTypeExpr)((ZigContainerMembers) dummy.getFirstChild()).getContainerDeclarationsList().get(0).getDeclList().get(0).getGlobalVarDecl().getExpr()).getStringLiteral();
        return (ZigStringLiteral) element.replace(stringLiteral);
    }

    @Override
    public @NotNull TextRange getRangeInElement(@NotNull ZigStringLiteral element) {
        if (element.getStringLiteralSingle() != null) {
            return new TextRange(1, element.getTextLength() - 1);
        }
        return super.getRangeInElement(element);
    }

    @SneakyThrows
    public static String escape(String input) {
        val bytes = input.getBytes(StandardCharsets.UTF_8);
        val result = new ByteArrayOutputStream();
        for (int i = 0; i < bytes.length; i++) {
            byte c = bytes[i];
            switch (c) {
                case '\n' -> result.write("\\n".getBytes(StandardCharsets.UTF_8));
                case '\r' -> result.write("\\r".getBytes(StandardCharsets.UTF_8));
                case '\t' -> result.write("\\t".getBytes(StandardCharsets.UTF_8));
                case '\\' -> result.write("\\\\".getBytes(StandardCharsets.UTF_8));
                case '"' -> result.write("\\\"".getBytes(StandardCharsets.UTF_8));
                case '\'', ' ', '!' -> result.write(c);
                default -> {
                    if (c >= '#' && c <= '&' ||
                        c >= '(' && c <= '[' ||
                        c >= ']' && c <= '~') {
                        result.write(c);
                    } else {
                        result.write("\\x".getBytes(StandardCharsets.UTF_8));
                        result.write(String.format("%02x", c).getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
        }
        return result.toString(StandardCharsets.UTF_8);
    }

    @SneakyThrows
    public static String unescape(String input, boolean[] noErrors) {
        noErrors[0] = true;
        val result = new ByteArrayOutputStream();
        val bytes = input.getBytes(StandardCharsets.UTF_8);
        val len = bytes.length;
        loop:
        for (int i = 0; i < len; i++) {
            byte c = bytes[i];
            switch (c) {
                case '\\' -> {
                    i++;
                    if (i < len) {
                        switch (input.charAt(i)) {
                            case 'n' -> result.write('\n');
                            case 'r' -> result.write('\r');
                            case 't' -> result.write('\t');
                            case '\\' -> result.write('\\');
                            case '"' -> result.write('"');
                            case 'x' -> {
                                if (i + 2 < len) {
                                    try {
                                        int b1 = decodeHex(bytes[i + 1]);
                                        int b2 = decodeHex(bytes[i + 2]);
                                        result.write((b1 << 4) | b2);
                                    } catch (NumberFormatException ignored) {
                                        noErrors[0] = false;
                                        break loop;
                                    }
                                    i += 2;
                                }
                            }
                            case 'u' -> {
                                i++;
                                if (i >= len || bytes[i] != '{') {
                                    noErrors[0] = false;
                                    break loop;
                                }
                                int codePoint = 0;
                                try {
                                    while (i < len && bytes[i] != '}') {
                                        codePoint <<= 4;
                                        codePoint |= decodeHex(bytes[i + 1]);
                                        i++;
                                    }
                                } catch (NumberFormatException ignored) {
                                    noErrors[0] = false;
                                    break loop;
                                }
                                if (i >= len) {
                                    noErrors[0] = false;
                                    break loop;
                                }
                                result.write(Character.toString(codePoint).getBytes(StandardCharsets.UTF_8));
                            }
                            default -> {
                                noErrors[0] = false;
                                break loop;
                            }
                        }
                    } else {
                        noErrors[0] = false;
                        break loop;
                    }
                }
                default -> result.write(c);
            }
        }
        return result.toString(StandardCharsets.UTF_8);
    }

    public static String unescapeWithLengthMappings(String input, List<Integer> inputOffsets, boolean[] noErrors) {
        String output = "";
        int lastOutputLength = 0;
        int inputOffset = 0;
        for (int i = 0; i < input.length(); i++) {
            output = unescape(input.substring(0, i + 1), noErrors);
            val outputLength = output.length();
            if (noErrors[0]) {
                inputOffset = i;
            }
            while (lastOutputLength < outputLength) {
                inputOffsets.add(inputOffset);
                lastOutputLength++;
                inputOffset = i + 1;
            }
        }
        return output;
    }

    private static int decodeHex(int b) {
        if (b >= '0' && b <= '9') {
            return b - '0';
        }
        if (b >= 'A' && b <= 'F') {
            return b - 'A' + 10;
        }
        if (b >= 'a' && b <= 'f') {
            return b - 'a' + 10;
        }
        throw new NumberFormatException();
    }
}
