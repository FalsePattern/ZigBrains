package com.falsepattern.zigbrains.zon

import com.intellij.lang.Language

object ZonLanguage: Language("Zon") {
    private fun readResolve(): Any = ZonLanguage
}