package com.falsepattern.zigbrains.zig.psi;

import com.falsepattern.zigbrains.zig.ZigFileType;
import com.falsepattern.zigbrains.zig.util.PsiTextUtil;
import com.falsepattern.zigbrains.zig.util.ZigStringUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.IncorrectOperationException;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ZigStringElementManipulator extends AbstractElementManipulator<ZigStringLiteral> {
    private enum InjectTriState {
        NotYet,
        Incomplete,
        Complete
    }
    @Override
    public @Nullable ZigStringLiteral handleContentChange(@NotNull ZigStringLiteral element, @NotNull TextRange range, String newContent)
            throws IncorrectOperationException {
        val originalContext = element.getText();
        val isMulti = element.isMultiLine();
        final CharSequence replacement;
        if (isMulti) {
            val contentRanges = element.getContentRanges();
            val contentBuilder = new StringBuilder();
            var injectState = InjectTriState.NotYet;
            for (val contentRange: contentRanges) {
                val intersection = injectState == InjectTriState.Complete ? null : contentRange.intersection(range);
                if (intersection != null) {
                    if (injectState == InjectTriState.NotYet) {
                        contentBuilder.append(originalContext, contentRange.getStartOffset(), intersection.getStartOffset());
                        contentBuilder.append(newContent);
                        if (intersection.getEndOffset() < contentRange.getEndOffset()) {
                            contentBuilder.append(originalContext, intersection.getEndOffset(), contentRange.getEndOffset());
                            injectState = InjectTriState.Complete;
                        } else {
                            injectState = InjectTriState.Incomplete;
                        }
                    } else if (intersection.getEndOffset() < contentRange.getEndOffset()) {
                        contentBuilder.append(originalContext, intersection.getEndOffset(), contentRange.getEndOffset());
                        injectState = InjectTriState.Complete;
                    }
                } else {
                    contentBuilder.append(originalContext, contentRange.getStartOffset(), contentRange.getEndOffset());
                }
            }
            val content = contentBuilder.toString();
            replacement = ZigStringUtil.prefixWithTextBlockEscape(PsiTextUtil.getIndentSize(element), "\\\\", content, false, true);
        } else {
            val elementRange = getRangeInElement(element);
            replacement = "\"" +
                          originalContext.substring(elementRange.getStartOffset(),
                                                    range.getStartOffset()) +
                          ZigStringUtil.escape(newContent) +
                          originalContext.substring(range.getEndOffset(),
                                                    elementRange.getEndOffset()) +
                          "\"";
        }
        val psiFileFactory = PsiFileFactory.getInstance(element.getProject());
        val dummy = psiFileFactory.createFileFromText("dummy." + ZigFileType.INSTANCE.getDefaultExtension(),
                                                      ZigFileType.INSTANCE, "const x = \n" + replacement + "\n;");
        val stringLiteral = ((ZigPrimaryTypeExpr)((ZigContainerMembers) dummy.getFirstChild()).getContainerDeclarationsList().get(0).getDeclList().get(0).getGlobalVarDecl().getExpr()).getStringLiteral();
        return (ZigStringLiteral) element.replace(stringLiteral);
    }

    @Override
    public @NotNull TextRange getRangeInElement(@NotNull ZigStringLiteral element) {
        return PsiTextUtil.getTextRangeBounds(element.getContentRanges());
    }
}
