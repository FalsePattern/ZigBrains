package com.falsepattern.zigbrains.zig.intentions;

import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral;
import com.falsepattern.zigbrains.zig.psi.ZigTypes;
import com.falsepattern.zigbrains.zig.util.PsiTextUtil;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import lombok.val;
import org.jetbrains.annotations.NotNull;

public class MakeStringMultiline extends PsiElementBaseIntentionAction implements IntentionAction {
    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        val str = PsiTreeUtil.getParentOfType(element, ZigStringLiteral.class);
        return str != null && !str.isMultiLine();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {
        val str = PsiTreeUtil.getParentOfType(element, ZigStringLiteral.class);
        if (str == null)
            return;
        PsiTextUtil.splitString(editor, str, editor.getCaretModel().getOffset(), false);
    }

    @Override
    public @NotNull @IntentionName String getText() {
        return getFamilyName();
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "Convert to multiline";
    }
}
