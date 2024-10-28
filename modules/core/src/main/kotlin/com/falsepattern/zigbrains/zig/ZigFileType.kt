package com.falsepattern.zigbrains.zig

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object ZigFileType : LanguageFileType(ZigLanguage) {
    override fun getName() = "Zig File"

    override fun getDescription() = "ZigLang file"

    override fun getDefaultExtension() = "zig"

    override fun getIcon() = Icons.ZIG
}