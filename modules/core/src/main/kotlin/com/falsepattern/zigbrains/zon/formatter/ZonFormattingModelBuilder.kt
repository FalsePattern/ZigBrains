package com.falsepattern.zigbrains.zon.formatter

import com.falsepattern.zigbrains.zon.ZonLanguage
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.SpacingBuilder

class ZonFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(context: FormattingContext): FormattingModel {
        val settings = context.codeStyleSettings
        return FormattingModelProvider.createFormattingModelForPsiFile(
            context.containingFile,
            ZonBlock(context.node, null, null, SpacingBuilder(settings, ZonLanguage)),
            settings
        )
    }
}
