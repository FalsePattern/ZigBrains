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

package com.falsepattern.zigbrains.zig.codeInsight

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.zig.psi.ZigFile
import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.psi.util.elementType

class ZigContext: TemplateContextType(ZigBrainsBundle.message("zig")) {
    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        val file = templateActionContext.file
        val offset = templateActionContext.startOffset

        if (file !is ZigFile)
            return false

        val element = file.findElementAt(offset) ?: return true

        return when (element.elementType) {
            ZigTypes.LINE_COMMENT, ZigTypes.CONTAINER_DOC_COMMENT, ZigTypes.DOC_COMMENT -> false
            ZigTypes.STRING_LITERAL_SINGLE, ZigTypes.STRING_LITERAL_MULTI -> false
            else -> true
        }
    }
}