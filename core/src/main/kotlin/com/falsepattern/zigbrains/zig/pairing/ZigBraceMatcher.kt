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

package com.falsepattern.zigbrains.zig.pairing

import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class ZigBraceMatcher: PairedBraceMatcher {
    override fun getPairs() =
        PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?) =
        true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int) =
        file?.findElementAt(openingBraceOffset)?.parent?.textOffset ?: openingBraceOffset
}

private val BRACE_PAIR = BracePair(ZigTypes.LBRACE, ZigTypes.RBRACE, true)
private val PAREN_PAIR = BracePair(ZigTypes.LPAREN, ZigTypes.RPAREN, false)
private val BRACKET_PAIR = BracePair(ZigTypes.LBRACKET, ZigTypes.RBRACKET, false)
private val PAIRS = arrayOf(BRACE_PAIR, PAREN_PAIR, BRACKET_PAIR)