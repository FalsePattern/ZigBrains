package com.falsepattern.zigbrains.zig.intentions;

import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral;
import com.falsepattern.zigbrains.zig.util.ZigStringUtil;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import lombok.val;
import org.jetbrains.annotations.NotNull;

public class MakeStringQuoted extends PsiElementBaseIntentionAction implements IntentionAction {
    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        val str = PsiTreeUtil.getParentOfType(element, ZigStringLiteral.class);
        return str != null && str.isMultiLine();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        val document = editor.getDocument();
        val file = element.getContainingFile();
        val str = PsiTreeUtil.getParentOfType(element, ZigStringLiteral.class);
        if (str == null)
            return;
        val escaper = str.createLiteralTextEscaper();
        val contentRange = escaper.getRelevantTextRange();
        val contentStart = contentRange.getStartOffset();
        val contentEnd = contentRange.getEndOffset();
        val fullRange = str.getTextRange();
        var caretOffset = editor.getCaretModel().getOffset();
        val prefix = new TextRange(contentStart, Math.max(contentStart, caretOffset - fullRange.getStartOffset()));
        val suffix = new TextRange(Math.min(contentEnd, caretOffset - fullRange.getStartOffset()), contentEnd);
        val sb = new StringBuilder();
        escaper.decode(prefix, sb);
        val prefixStr = ZigStringUtil.escape(sb.toString());
        sb.setLength(0);
        escaper.decode(suffix, sb);
        val suffixStr = ZigStringUtil.escape(sb.toString());
        val stringRange = document.createRangeMarker(fullRange.getStartOffset(), fullRange.getEndOffset());
        stringRange.setGreedyToRight(true);
        document.deleteString(stringRange.getStartOffset(), stringRange.getEndOffset());
        val documentText = document.getCharsSequence();
        boolean addSpace = true;
        int scanStart = stringRange.getEndOffset();
        int scanEnd = scanStart;
        loop:
        while (scanEnd < documentText.length()) {
            switch (documentText.charAt(scanEnd)) {
                case ' ', '\t', '\r', '\n':
                    break;
                case ',', ';':
                    addSpace = false;
                default:
                    break loop;
            }
            scanEnd++;
        }
        if (scanEnd > scanStart) {
            if (addSpace) {
                document.replaceString(scanStart, scanEnd, " ");
            } else {
                document.deleteString(scanStart, scanEnd);
            }
        }
        document.insertString(stringRange.getEndOffset(), "\"");
        document.insertString(stringRange.getEndOffset(), prefixStr);
        caretOffset = stringRange.getEndOffset();
        document.insertString(stringRange.getEndOffset(), suffixStr);
        document.insertString(stringRange.getEndOffset(), "\"");
        stringRange.dispose();
        editor.getCaretModel().moveToOffset(caretOffset);
    }

    @Override
    public @NotNull @IntentionName String getText() {
        return getFamilyName();
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "Convert to quoted";
    }
}
