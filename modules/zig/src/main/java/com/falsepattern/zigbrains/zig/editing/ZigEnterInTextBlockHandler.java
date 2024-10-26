package com.falsepattern.zigbrains.zig.editing;

import com.falsepattern.zigbrains.zig.parser.ZigFile;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@RequiredArgsConstructor
public class ZigEnterInTextBlockHandler extends EnterHandlerDelegateAdapter {
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
        for (val assistant: ZigMultiLineAssistant.Assistants.ASSISTANTS) {
            val result = preprocessEnterWithAssistant(file, editor, assistant);
            if (result != null)
                return result;
        }
        return Result.Continue;
    }

    private static <T extends PsiElement> Result preprocessEnterWithAssistant(@NotNull PsiFile file,
                                                                              @NotNull Editor editor,
                                                                              ZigMultiLineAssistant<T> assistant) {
        val offset = editor.getCaretModel().getOffset();
        val textBlock = getTextBlockAt(file, offset, assistant);
        if (textBlock == null) {
            return null;
        }
        val textBlockOffset = textBlock.getTextOffset();
        val document = editor.getDocument();
        val textBlockLine = document.getLineNumber(textBlockOffset);
        val textBlockLineStart = document.getLineStartOffset(textBlockLine);
        val indentPre = textBlockOffset - textBlockLineStart;
        val project = textBlock.getProject();
        val lineNumber = document.getLineNumber(offset);
        val lineStartOffset = document.getLineStartOffset(lineNumber);
        val text = document.getText(new TextRange(lineStartOffset, offset + 1));
        val parts = new ArrayList<>(StringUtil.split(text, assistant.prefix));
        if (parts.size() <= 1)
            return Result.Continue;
        if (parts.size() > 2) {
            val sb = new StringBuilder();
            sb.append(parts.get(1));
            while (parts.size() > 2) {
                sb.append(assistant.prefix);
                sb.append(parts.remove(2));
            }
            parts.set(1, sb.toString());
        }
        val indentPost = measureSpaces(parts.get(1));
        val newLine = '\n' + StringUtil.repeatSymbol(' ', indentPre) + assistant.prefix + StringUtil.repeatSymbol(' ', indentPost);
        document.insertString(offset, newLine);
        PsiDocumentManager.getInstance(project).commitDocument(document);
        editor.getCaretModel().moveToOffset(offset + newLine.length());
        return Result.Stop;
    }

    private static int measureSpaces(String str) {
        for (int i = 0; i < str.length(); i++) {
            val c = str.charAt(i);
            switch (c) {
                case ' ':
                case '\t':
                    continue;
                default:
                    return i;
            }
        }
        return str.length();
    }

    private static <T extends PsiElement> T getTextBlockAt(PsiFile file, int offset, ZigMultiLineAssistant<T> assistant) {
        val psiAtOffset = file.findElementAt(offset);
        if (psiAtOffset == null)
            return null;
        return assistant.acceptPSI(psiAtOffset);
    }
}
