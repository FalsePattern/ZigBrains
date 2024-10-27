package com.falsepattern.zigbrains.zig.util;

import com.falsepattern.zigbrains.zig.stringlexer.ZigStringLexer;
import com.intellij.lang.ASTNode;
import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StringEscapesTokenTypes;
import com.intellij.util.MathUtil;
import lombok.SneakyThrows;
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

    public static void splitString(@NotNull Editor editor,
                                   @NotNull PsiElement psiAtOffset,
                                   int caretOffset,
                                   boolean insertNewlineAtCaret) {
        val document = editor.getDocument();
        ASTNode token = psiAtOffset.getNode();
        val text = document.getCharsSequence();

        TextRange range = token.getTextRange();
        val lexer = new FlexAdapter(new ZigStringLexer());
        lexer.start(text, range.getStartOffset(), range.getEndOffset());
        caretOffset = skipStringLiteralEscapes(caretOffset, lexer);
        caretOffset = MathUtil.clamp(caretOffset, range.getStartOffset() + 1, range.getEndOffset() - 1);
        val unescapedPrefix = ZigStringUtil.unescape(text.subSequence(range.getStartOffset() + 1, caretOffset), false);
        val unescapedSuffix = ZigStringUtil.unescape(text.subSequence(caretOffset, range.getEndOffset() - 1), false);
        val stringRange = document.createRangeMarker(range.getStartOffset(), range.getEndOffset());
        stringRange.setGreedyToRight(true);
        val lineNumber = document.getLineNumber(caretOffset);
        val lineOffset = document.getLineStartOffset(lineNumber);
        val indent = stringRange.getStartOffset() - lineOffset;
        val lineIndent = StringUtil.skipWhitespaceForward(document.getText(new TextRange(lineOffset, stringRange.getStartOffset())), 0);
        boolean newLine = indent != lineIndent;
        document.deleteString(stringRange.getStartOffset(), stringRange.getEndOffset());
        document.insertString(stringRange.getStartOffset(),
                              ZigStringUtil.prefixWithTextBlockEscape(newLine ? lineIndent + 4 : lineIndent,
                                                                      "\\\\",
                                                                      insertNewlineAtCaret ? unescapedPrefix + "\n" : unescapedPrefix,
                                                                      newLine,
                                                                      true));
        caretOffset = stringRange.getEndOffset();
        document.insertString(caretOffset,
                              ZigStringUtil.prefixWithTextBlockEscape(newLine ? lineIndent + 4 : lineIndent,
                                                                      "\\\\",
                                                                      unescapedSuffix,
                                                                      false,
                                                                      false));
        int end = stringRange.getEndOffset();
        loop:
        while (end < document.getTextLength()) {
            switch (text.charAt(end)) {
                case ' ', '\t':
                    break;
                default:
                    break loop;
            }
            end++;
        }
        document.replaceString(stringRange.getEndOffset(), end, "\n" + " ".repeat(newLine ? lineIndent : Math.max(lineIndent - 4, 0)));
        stringRange.dispose();
        editor.getCaretModel().moveToOffset(caretOffset);
    }
    @SneakyThrows
    protected static int skipStringLiteralEscapes(int caretOffset, Lexer lexer) {
        while (lexer.getTokenType() != null) {
            if (lexer.getTokenStart() < caretOffset && caretOffset < lexer.getTokenEnd()) {
                if (StringEscapesTokenTypes.STRING_LITERAL_ESCAPES.contains(lexer.getTokenType())) {
                    caretOffset = lexer.getTokenEnd();
                }
                break;
            }
            lexer.advance();
        }
        return caretOffset;
    }
}
