package com.falsepattern.zigbrains.zig

import com.falsepattern.zigbrains.Icons
import com.intellij.openapi.fileTypes.LanguageFileType

object ZigFileType : LanguageFileType(ZigLanguage) {
    override fun getName() = "Zig File"

    override fun getDescription() = "ZigLang file"

    override fun getDefaultExtension() = "zig"

    override fun getIcon() = Icons.ZIG
}