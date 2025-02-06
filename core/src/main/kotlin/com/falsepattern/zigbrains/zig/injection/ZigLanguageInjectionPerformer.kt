/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2025 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * ZigBrains is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * ZigBrains is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ZigBrains. If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.zigbrains.zig.injection

import com.falsepattern.zigbrains.zig.psi.ZigStringLiteral
import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.falsepattern.zigbrains.zig.psi.getMultilineContent
import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.lang.injection.general.Injection
import com.intellij.lang.injection.general.LanguageInjectionPerformer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost

class ZigLanguageInjectionPerformer : LanguageInjectionPerformer {
    override fun isPrimary() = false

    override fun performInjection(registrar: MultiHostRegistrar, injection: Injection, context: PsiElement): Boolean {
        if (context !is PsiLanguageInjectionHost)
            return false

        val language = injection.injectedLanguage ?: return false

        val ranges: List<TextRange> = if (context is ZigStringLiteral) {
            context.contentRanges
        } else if (context is PsiComment) {
            val comment = context as PsiComment
            when (comment.tokenType) {
                ZigTypes.LINE_COMMENT -> comment.text.getMultilineContent("//")
                ZigTypes.DOC_COMMENT -> comment.text.getMultilineContent("///")
                ZigTypes.CONTAINER_DOC_COMMENT -> comment.text.getMultilineContent("//!")
                else -> return false
            }
        } else {
            return false
        }
        injectIntoStringMultiRanges(
            registrar,
            context,
            ranges,
            language,
            injection.prefix,
            injection.suffix
        )
        return true
    }
}

private fun injectIntoStringMultiRanges(
    registrar: MultiHostRegistrar,
    context: PsiLanguageInjectionHost,
    ranges: List<TextRange>,
    language: Language,
    prefix: String,
    suffix: String
) {
    if (ranges.isEmpty())
        return

    registrar.startInjecting(language)

    if (ranges.size == 1) {
        registrar.addPlace(prefix, suffix, context, ranges.first())
    } else {
        registrar.addPlace(prefix, null, context, ranges.first())
        for (range in ranges.subList(1, ranges.size - 1)) {
            registrar.addPlace(null, null, context, range)
        }
        registrar.addPlace(null, suffix, context, ranges.last())
    }
    registrar.doneInjecting()
}