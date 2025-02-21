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

package com.falsepattern.zigbrains.zig.util

import com.falsepattern.zigbrains.zig.ZigFileType
import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.source.tree.LeafElement

object ZigElementFactory {
    private val LOG = Logger.getInstance(ZigElementFactory::class.java)
    fun createZigFile(project: Project, text: CharSequence): PsiFile {
        return PsiFileFactory.getInstance(project).createFileFromText("a.zig", ZigFileType, text)
    }

    fun createZigIdentifier(project: Project, name: String): PsiElement? {
        val file = createZigFile(project, "const $name = undefined;")
        val identifier = file.findElementAt("const ".length) ?: return null
        LOG.assertTrue(identifier is LeafElement && identifier.elementType == ZigTypes.IDENTIFIER, name)
        return identifier
    }
}