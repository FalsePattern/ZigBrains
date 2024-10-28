package com.falsepattern.zigbrains.zig.psi

import com.falsepattern.zigbrains.zig.ZigFileType
import com.falsepattern.zigbrains.zig.ZigLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class ZigFile(viewProvider: FileViewProvider): PsiFileBase(viewProvider, ZigLanguage) {
    override fun getFileType() = ZigFileType

    override fun toString() = ZigFileType.name
}