package com.falsepattern.zigbrains.zig.psi;

import com.falsepattern.zigbrains.zig.ZigFileType;
import com.falsepattern.zigbrains.zig.util.PsiTextUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.IncorrectOperationException;
import lombok.SneakyThrows;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ZigStringElementManipulator extends AbstractElementManipulator<ZigStringLiteral> {
    @Override
    public @Nullable ZigStringLiteral handleContentChange(@NotNull ZigStringLiteral element, @NotNull TextRange range, String newContent)
            throws IncorrectOperationException {
        assert (new TextRange(0, element.getTextLength())).contains(range);
        val originalContext = element.getText();
        val isMulti = element.isMultiLine();
        val elementRange = getRangeInElement(element);
        var replacement = originalContext.substring(elementRange.getStartOffset(),
                                                    range.getStartOffset()) +
                          (isMulti ? newContent : escape(newContent)) +
                          originalContext.substring(range.getEndOffset(),
                                                    elementRange.getEndOffset());
        val psiFileFactory = PsiFileFactory.getInstance(element.getProject());
        if (isMulti) {
            val column = StringUtil.offsetToLineColumn(element.getContainingFile().getText(), element.getTextOffset()).column;
            val pfx = " ".repeat(Math.max(0, column)) + "\\\\";
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
        return PsiTextUtil.getTextRangeBounds(element.getContentRanges());
    }

    @SneakyThrows
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
}
