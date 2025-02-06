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

package com.falsepattern.zigbrains.project.execution.build

import com.falsepattern.zigbrains.project.execution.base.ZigTopLevelLineMarker
import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.icons.AllIcons.RunConfigurations.TestState
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import javax.swing.Icon

class ZigLineMarkerBuild: ZigTopLevelLineMarker() {
    override fun getDeclaration(element: PsiElement): PsiElement? {
        if (element.elementType != ZigTypes.IDENTIFIER)
            return null

        if (!element.textMatches("build"))
            return null

        val parent = element.parent ?: return null
        if (parent.elementType != ZigTypes.FN_PROTO)
            return null

        val file = element.containingFile ?: return null

        val fileName = file.virtualFile.name
        if (fileName != "build.zig")
            return null

        return parent.parent
    }

    override fun getIcon(element: PsiElement): Icon {
        return TestState.Run
    }
}
