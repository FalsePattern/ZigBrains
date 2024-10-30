package com.falsepattern.zigbrains.zon

import com.falsepattern.zigbrains.Icons
import com.intellij.openapi.fileTypes.LanguageFileType

object ZonFileType: LanguageFileType(ZonLanguage) {
    override fun getName() = "Zon File"

    override fun getDescription() = "Zig object notation file"

    override fun getDefaultExtension() = "zon"

    override fun getIcon() = Icons.ZON
}