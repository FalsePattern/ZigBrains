package com.falsepattern.zigbrains.zig.util;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PsiTextUtil {
    public static @NotNull TextRange getTextRangeBounds(@NotNull List<TextRange> contentRanges) {
        if (contentRanges.isEmpty()) {
            return TextRange.EMPTY_RANGE;
        }
        return TextRange.create(contentRanges.getFirst().getStartOffset(), contentRanges.getLast().getEndOffset());
    }
    public static @NotNull List<TextRange> getMultiLineContent(@NotNull String text, @NotNull String startMark) {
        val result = new ArrayList<TextRange>();
        int stringStart = 0;
        boolean inBody = false;
        val textLength = text.length();
        val firstChar = startMark.charAt(0);
        val extraChars = startMark.substring(1);
        loop:
        for (int i = 0; i < textLength; i++) {
            val cI = text.charAt(i);
            if (!inBody) {
                if (cI == firstChar &&
                    i + extraChars.length() < textLength) {
                    for (int j = 0; j < extraChars.length(); j++) {
                        if (text.charAt(i + j + 1) != startMark.charAt(j)) {
                            continue loop;
                        }
                    }
                    i += extraChars.length();
                    inBody = true;
                    stringStart = i + 1;
                }
                continue;
            }
            if (cI == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                inBody = false;
                result.add(new TextRange(stringStart, Math.min(textLength - 1, i + 1)));
                continue;
            }
            if (cI == '\n') {
                inBody = false;
                result.add(new TextRange(stringStart, Math.min(textLength - 1, i + 1)));
            }
        }
        return result;
    }

    public static int getIndentSize(PsiElement element) {
        return StringUtil.offsetToLineColumn(element.getContainingFile().getText(), element.getTextOffset()).column;
    }

    public static String getIndentString(PsiElement element) {
        val indent = getIndentSize(element);
        return " ".repeat(Math.max(0, indent));
    }
}
