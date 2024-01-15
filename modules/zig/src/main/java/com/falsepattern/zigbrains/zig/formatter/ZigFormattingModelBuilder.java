package com.falsepattern.zigbrains.zig.formatter;

import com.falsepattern.zigbrains.zig.ZigLanguage;
import com.intellij.formatting.FormattingContext;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.formatting.FormattingModelProvider;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;

final public class ZigFormattingModelBuilder implements FormattingModelBuilder {
    private static SpacingBuilder createSpacingBuilder(CodeStyleSettings settings) {
        return new SpacingBuilder(settings, ZigLanguage.INSTANCE);
    }

    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        final var codeStyleSettings = formattingContext.getCodeStyleSettings();
        return FormattingModelProvider.createFormattingModelForPsiFile(
                formattingContext.getContainingFile(),
                new ZigBlock(formattingContext.getNode(), null, null, createSpacingBuilder(codeStyleSettings)),
                codeStyleSettings
                                                                      );
    }
}
