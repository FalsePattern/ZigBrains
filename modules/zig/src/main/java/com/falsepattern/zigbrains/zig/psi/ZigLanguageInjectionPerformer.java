package com.falsepattern.zigbrains.zig.psi;

import com.falsepattern.zigbrains.zig.util.PsiTextUtil;
import com.intellij.lang.Language;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.lang.injection.general.Injection;
import com.intellij.lang.injection.general.LanguageInjectionPerformer;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ZigLanguageInjectionPerformer implements LanguageInjectionPerformer {
    @Override
    public boolean isPrimary() {
        return false;
    }

    @Override
    public boolean performInjection(@NotNull MultiHostRegistrar registrar, @NotNull Injection injection, @NotNull PsiElement context) {
        if (!(context instanceof PsiLanguageInjectionHost host))
            return false;
        val language = injection.getInjectedLanguage();
        if (language == null)
            return false;
        List<TextRange> ranges;
        if (host instanceof ZigStringLiteral str) {
            ranges = str.getContentRanges();
        } else if (host instanceof PsiComment comment) {
            val tt = comment.getTokenType();
            if (tt == ZigTypes.LINE_COMMENT) {
                ranges = PsiTextUtil.getMultiLineContent(comment.getText(), "//");
            } else if (tt == ZigTypes.DOC_COMMENT) {
                ranges = PsiTextUtil.getMultiLineContent(comment.getText(), "///");
            } else if (tt == ZigTypes.CONTAINER_DOC_COMMENT) {
                ranges = PsiTextUtil.getMultiLineContent(comment.getText(), "//!");
            } else {
                return false;
            }
        } else {
            return false;
        }
        injectIntoStringMultiRanges(registrar, host, ranges, language, injection.getPrefix(), injection.getSuffix());
        return true;
    }

    private static void injectIntoStringMultiRanges(MultiHostRegistrar registrar,
                                                    PsiLanguageInjectionHost context,
                                                    List<TextRange> ranges,
                                                    Language language,
                                                    String prefix,
                                                    String suffix) {
        if (ranges.isEmpty())
            return;

        registrar.startInjecting(language);

        if (ranges.size() == 1) {
            registrar.addPlace(prefix, suffix, context, ranges.getFirst());
        } else {
            registrar.addPlace(prefix, null, context, ranges.getFirst());
            for (val range : ranges.subList(1, ranges.size() - 1)) {
                registrar.addPlace(null, null, context, range);
            }
            registrar.addPlace(null, suffix, context, ranges.getLast());
        }
        registrar.doneInjecting();
    }
}
