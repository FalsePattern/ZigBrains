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

import com.falsepattern.zigbrains.zig.psi.ZigContainerField
import com.falsepattern.zigbrains.zig.psi.ZigContainerMembers
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.util.childrenOfType
import com.intellij.util.resettableLazy

abstract class ZigContainerMembersMixinImpl(node: ASTNode): ASTWrapperPsiElement(node), ZigContainerMembers {
    private val _isNamespace = resettableLazy {
        childrenOfType<ZigContainerField>().isEmpty()
    }
    override val isNamespace by _isNamespace

    override fun subtreeChanged() {
        super.subtreeChanged()
        _isNamespace.reset()
    }
}