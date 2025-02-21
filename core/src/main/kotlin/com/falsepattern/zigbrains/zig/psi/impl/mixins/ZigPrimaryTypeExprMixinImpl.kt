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

import com.falsepattern.zigbrains.project.settings.zigProjectSettings
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainProvider
import com.falsepattern.zigbrains.zig.psi.ZigFnCallArguments
import com.falsepattern.zigbrains.zig.util.ZigElementFactory
import com.falsepattern.zigbrains.zig.psi.ZigPrimaryTypeExpr
import com.falsepattern.zigbrains.zig.references.ZigReference
import com.falsepattern.zigbrains.zig.psi.ZigReferenceElement
import com.falsepattern.zigbrains.zig.psi.ZigTypes
import com.falsepattern.zigbrains.zig.references.ZigType
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.elementType
import com.intellij.util.resettableLazy
import kotlinx.coroutines.runBlocking
import kotlin.io.path.exists

abstract class ZigPrimaryTypeExprMixinImpl(node: ASTNode): ASTWrapperPsiElement(node), ZigPrimaryTypeExpr {
    private val ref = resettableLazy {
        identifier?.let { return@resettableLazy ZigReference(this, it.textRangeInParent) }
        //TODO import resolving
//        val children = children
//        if (children.size == 2) run {
//            val a = children[0]
//            val b = children[1]
//            if (a.elementType != ZigTypes.BUILTINIDENTIFIER || !a.text.equals("@import")) {
//                return@run
//            }
//            if (b !is ZigFnCallArguments) {
//                return@run
//            }
//            val exprList = b.exprList?.exprList ?: return@run
//            if (exprList.size != 1)
//                return@run
//            val expr = exprList[0] as? ZigPrimaryTypeExpr ?: return@run
//            val stringLiteral = expr.stringLiteral ?: return@run
//            val esc = stringLiteral.createLiteralTextEscaper()
//            val sb = StringBuilder()
//            esc.decode(TextRange(0, stringLiteral.textLength), sb)
//            val str = sb.toString()
//            if (str == "std") {
//                val tc = project.zigProjectSettings.state.toolchain ?: return@run
//                val env = runBlocking {
//                    tc.zig.getEnv(project)
//                }
//                val stdPath = env.stdPath(tc, project) ?: return@run
//                val stdFile = stdPath.resolve("std.zig")
//                if (!stdFile.exists()) {
//                    return@run
//                }
//            }
//        }
        return@resettableLazy null
    }
    override fun getReference() = ref.value

    override fun subtreeChanged() {
        super.subtreeChanged()
        ref.reset()
    }

    override fun rename(name: String): ZigReferenceElement? {
        identifier?.replace(ZigElementFactory.createZigIdentifier(project, name) ?: return null) ?: return null
        return this
    }

    override fun getType(): ZigType {
        containerDecl?.containerDeclAuto?.let { containerDecl ->
            val type = containerDecl.containerDeclType
            if (type.keywordStruct != null) {
                if (containerDecl.containerMembers?.isNamespace == true) {
                    return ZigType.Namespace
                }
                return ZigType.Struct
            }
            if (type.keywordEnum != null) {
                return ZigType.EnumType
            }
            if (type.keywordUnion != null) {
                return ZigType.Union
            }
        }
        errorSetDecl?.let {
            return ZigType.ErrorType
        }
        return ZigType.Generic
    }

}