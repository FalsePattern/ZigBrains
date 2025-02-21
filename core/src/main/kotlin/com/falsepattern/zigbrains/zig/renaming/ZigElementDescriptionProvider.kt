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

package com.falsepattern.zigbrains.zig.renaming

import com.falsepattern.zigbrains.zig.psi.ZigFnDeclProto
import com.falsepattern.zigbrains.zig.psi.ZigGlobalVarDecl
import com.falsepattern.zigbrains.zig.psi.ZigVarDeclExprStatement
import com.falsepattern.zigbrains.zig.psi.ZigVarDeclProto
import com.intellij.psi.ElementDescriptionLocation
import com.intellij.psi.ElementDescriptionProvider
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageViewTypeLocation

class ZigElementDescriptionProvider: ElementDescriptionProvider {
    override fun getElementDescription(
        element: PsiElement,
        location: ElementDescriptionLocation
    ): String? {
        if (location != UsageViewTypeLocation.INSTANCE)
            return null
        return when(element) {
            is ZigVarDeclProto -> {
                val type = if (element.keywordConst != null) "constant" else "variable"
                when(element.parent) {
                    is ZigGlobalVarDecl -> "global $type"
                    is ZigVarDeclExprStatement -> "local $type"
                    else -> type
                }
            }
            is ZigFnDeclProto -> "function"
            else -> null
        }
    }
}