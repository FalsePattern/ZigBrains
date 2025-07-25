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

package com.falsepattern.zigbrains.zon.psi.impl.mixins

import com.falsepattern.zigbrains.shared.psi.StringLiteralImpl
import com.falsepattern.zigbrains.zon.psi.ZonStringLiteral
import com.falsepattern.zigbrains.zon.psi.ZonTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class ZonStringLiteralMixinImpl(node: ASTNode): ASTWrapperPsiElement(node), ZonStringLiteral, StringLiteralImpl {
    override val stringLiteralSingleType = ZonTypes.STRING_LITERAL_SINGLE!!
    override val stringLiteralMultiType = ZonTypes.STRING_LITERAL_MULTI!!
    override val zbStringLiteralSingle = stringLiteralSingle
}