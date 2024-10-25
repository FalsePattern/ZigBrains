package com.falsepattern.zigbrains.zig.util;

import com.intellij.openapi.util.TextRange;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PsiTextUtil {
    public static @NotNull TextRange getTextRangeBounds(@NotNull List<TextRange> contentRanges) {
        if (contentRanges.isEmpty()) {
            return TextRange.EMPTY_RANGE;
        }
        return TextRange.create(contentRanges.get(0).getStartOffset(), contentRanges.get(contentRanges.size() - 1).getEndOffset());
    }
    public static @NotNull List<TextRange> getMultiLineContent(@NotNull String text, @NotNull String startMark) {
        val result = new ArrayList<TextRange>();
        int stringStart = 0;
        boolean inBody = false;
        val textLength = text.length();
        val firstChar = startMark.charAt(0);
        val extraChars = startMark.substring(1);
        for (int i = 0; i < textLength; i++) {
            val cI = text.charAt(i);
            if (!inBody) {
                if (cI == firstChar &&
                    i + extraChars.length() < textLength) {
                    for (int j = 0; j < extraChars.length(); j++) {
                        if (text.charAt(i + j + 1) != startMark.charAt(j)) {
                            continue;
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
                result.add(new TextRange(stringStart, i + 1));
                continue;
            }
            if (cI == '\n') {
                inBody = false;
                result.add(new TextRange(stringStart, i + 1));
            }
        }
        return result;
    }
}
