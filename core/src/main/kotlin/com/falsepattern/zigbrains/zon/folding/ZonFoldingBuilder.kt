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

package com.falsepattern.zigbrains.zon.folding

import com.falsepattern.zigbrains.zon.psi.ZonStruct
import com.falsepattern.zigbrains.zon.psi.ZonVisitor
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class ZonFoldingBuilder: CustomFoldingBuilder(), DumbAware {
    override fun buildLanguageFoldRegions(
        descriptors: MutableList<FoldingDescriptor>,
        root: PsiElement,
        document: Document,
        quick: Boolean
    ) {
        root.accept(object: ZonVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                element.acceptChildren(this)
            }

            override fun visitStruct(o: ZonStruct) {
                super.visitStruct(o)
                descriptors.add(FoldingDescriptor(o, o.textRange))
            }
        })
    }

    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange) = ".{...}"

    override fun isRegionCollapsedByDefault(node: ASTNode) = false
}