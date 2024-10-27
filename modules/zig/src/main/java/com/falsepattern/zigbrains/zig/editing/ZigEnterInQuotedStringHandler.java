package com.falsepattern.zigbrains.zig.editing;

import com.falsepattern.zigbrains.zig.parser.ZigFile;
import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral;
import com.falsepattern.zigbrains.zig.psi.ZigTypes;
import com.falsepattern.zigbrains.zig.util.PsiTextUtil;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class ZigEnterInQuotedStringHandler extends EnterHandlerDelegateAdapter {
    @Override
    public Result preprocessEnter(@NotNull PsiFile file,
                                  @NotNull Editor editor,
                                  @NotNull Ref<Integer> caretOffsetRef,
                                  @NotNull Ref<Integer> caretAdvanceRef,
                                  @NotNull DataContext dataContext,
                                  EditorActionHandler originalHandler) {
        if (!(file instanceof ZigFile)) {
            return Result.Continue;
        }

        val caretOffset = (int)caretOffsetRef.get();
        var psiAtOffset = file.findElementAt(caretOffset);
        if (psiAtOffset instanceof LeafPsiElement leaf) {
            if ( leaf.getTokenType() == ZigTypes.STRING_LITERAL_SINGLE) {
                psiAtOffset = leaf.getParent();
            }
        }
        if (psiAtOffset instanceof ZigStringLiteral str &&
            !str.isMultiLine() &&
            str.getTextOffset() < caretOffset) {
            PsiTextUtil.splitString(editor, str, caretOffset, true);
            return Result.Stop;
        }
        return Result.Continue;
    }
}
