/*
 * ZigBrains
 * Copyright (C) 2023-2025 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.zigbrains.zig.documentation

import com.falsepattern.zigbrains.zig.psi.ZigFile
import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.intellij.markdown.utils.convertMarkdownToHtml
import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType

class ZigTopLevelDocumentationProvider: DocumentationTargetProvider {
    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        if (file !is ZigFile)
            return emptyList()

        val elem = file.findElementAt(offset)?.takeIf { it is PsiComment && it.elementType == ZigTypes.CONTAINER_DOC_COMMENT } ?: return emptyList()
        val rawText = elem.text
        val fileName = file.name
        return listOf(object: DocumentationTarget {
            override fun createPointer(): Pointer<out DocumentationTarget> {
                return Pointer.hardPointer(this)
            }

            override fun computeDocumentation(): DocumentationResult? {
                return DocumentationResult.asyncDocumentation {
                    val text = rawText.lines().joinToString(separator = "\n") { line ->
                        if (line.isEmpty())
                            return@joinToString line
                        val idx = line.indexOf("//!")
                        if (idx < 0 || idx + 3 > line.length)
                            return@joinToString line
                        line.substring( idx + 3)
                    }
                    DocumentationResult.documentation(convertMarkdownToHtml(text))
                }
            }

            override fun computePresentation(): TargetPresentation {
                return TargetPresentation.builder(fileName).presentation()
            }

        })
    }
}