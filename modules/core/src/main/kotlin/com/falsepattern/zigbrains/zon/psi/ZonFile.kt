package com.falsepattern.zigbrains.zon.psi

import com.falsepattern.zigbrains.zon.ZonFileType
import com.falsepattern.zigbrains.zon.ZonLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class ZonFile(viewProvider: FileViewProvider): PsiFileBase(viewProvider, ZonLanguage) {
    override fun getFileType() = ZonFileType

    override fun toString() = "Zon File"
}