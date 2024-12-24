/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2024 FalsePattern
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

package com.falsepattern.zigbrains.project.execution.base

import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import javax.swing.Icon

abstract class ZigTopLevelLineMarker: RunLineMarkerContributor() {
    private fun getParentIfTopLevel(element: PsiElement): PsiElement? {
        var parent = getDeclaration(element)

        var nestingLevel = 0;
        while (parent != null && parent !is PsiFile) {
            if (parent.elementType == ZigTypes.CONTAINER_DECLARATION) {
                if (nestingLevel != 0)
                    return null
                nestingLevel++
            }
            parent = parent.parent
        }
        if (nestingLevel != 1)
            return null
        return parent
    }

    fun elementMatches(element: PsiElement): Boolean {
        return getParentIfTopLevel(element) != null
    }

    override fun getInfo(element: PsiElement): Info? {
        if (!elementMatches(element))
            return null;
        val actions = ExecutorAction.getActions(0)
        return Info(getIcon(element), actions, null)
    }

    abstract fun getDeclaration(element: PsiElement): PsiElement?

    abstract fun getIcon(element: PsiElement): Icon
}