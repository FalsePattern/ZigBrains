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

package com.falsepattern.zigbrains

import com.intellij.psi.PsiElement

/**
 * Provides the start offset of the receiver element
 * relative to the start offset of one of its ancestors.
 *
 * Useful, for instance, when a name identifier element is not
 * a direct descendant of an element, yet we need to calculate
 * the relative offset.
 */
fun PsiElement.startOffsetInAncestor(ancestor: PsiElement): Int {
    var result = 0
    var tmp: PsiElement? = this
    while (tmp != ancestor && tmp != null) {
        result += tmp.startOffsetInParent
        tmp = tmp.parent
    }
    return if (tmp != null)
        result
    else -1
}