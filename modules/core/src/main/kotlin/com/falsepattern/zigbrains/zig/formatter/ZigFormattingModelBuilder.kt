package com.falsepattern.zigbrains.zig.formatter

import com.falsepattern.zigbrains.zig.ZigLanguage
import com.intellij.formatting.*

class ZigFormattingModelBuilder: FormattingModelBuilder {
    override fun createModel(context: FormattingContext): FormattingModel {
        val settings = context.codeStyleSettings
        return FormattingModelProvider.createFormattingModelForPsiFile(
            context.containingFile,
            ZigBlock(context.node, null, null, SpacingBuilder(settings, ZigLanguage)),
            settings
        )
    }
}