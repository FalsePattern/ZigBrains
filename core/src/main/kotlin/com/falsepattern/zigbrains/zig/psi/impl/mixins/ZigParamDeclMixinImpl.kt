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

package com.falsepattern.zigbrains.zig.psi.impl.mixins

import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter
import com.falsepattern.zigbrains.zig.psi.ZigParamDecl
import com.falsepattern.zigbrains.zig.util.ZigElementFactory
import com.falsepattern.zigbrains.zig.psi.ZigVarDeclProto
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.util.startOffset

abstract class ZigParamDeclMixinImpl(node: ASTNode): ASTWrapperPsiElement(node), ZigParamDecl {
    override fun getName(): String? {
        return identifier?.text
    }

    override fun setName(name: String): PsiElement? {
        identifier?.replace(ZigElementFactory.createZigIdentifier(project, name) ?: return null) ?: return null
        return this
    }

    override fun getNameIdentifier(): PsiElement? {
        return identifier
    }

    override fun getTextOffset(): Int {
        return identifier?.startOffset ?: 0
    }

    override val declarationAttribute get() = ZigSyntaxHighlighter.PARAMETER

    override val referenceAttribute get() = ZigSyntaxHighlighter.VARIABLE_REF
}