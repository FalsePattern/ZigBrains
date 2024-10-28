package com.falsepattern.zigbrains.zig

import com.intellij.lang.Language

object ZigLanguage: Language("Zig") {
    private fun readResolve(): Any = ZigLanguage
}