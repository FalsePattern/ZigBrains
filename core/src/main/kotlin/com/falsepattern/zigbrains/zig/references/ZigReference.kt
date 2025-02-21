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

package com.falsepattern.zigbrains.zig.references

import com.falsepattern.zigbrains.zig.psi.ZigBlock
import com.falsepattern.zigbrains.zig.psi.ZigContainerDeclaration
import com.falsepattern.zigbrains.zig.psi.ZigContainerMembers
import com.falsepattern.zigbrains.zig.psi.ZigDecl
import com.falsepattern.zigbrains.zig.psi.ZigFnDeclProto
import com.falsepattern.zigbrains.zig.psi.ZigParamDecl
import com.falsepattern.zigbrains.zig.psi.ZigReferenceElement
import com.falsepattern.zigbrains.zig.psi.ZigStatement
import com.falsepattern.zigbrains.zig.psi.ZigVarDeclProto
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.childrenOfType

class ZigReference<T: ZigReferenceElement>(element: T, textRange: TextRange): PsiPolyVariantReferenceBase<T>(element, textRange) {
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        element.putUserData(NAME_RANGE_KEY, rangeInElement)
        return resolveOrEmpty(element)
    }

    companion object {
        private val NAME_RANGE_KEY = Key.create<TextRange>("ZIG_REF_NAME_RANGE")
        private fun resolveOrEmpty(element: PsiElement): Array<out ResolveResult> {
            return CachedValuesManager.getCachedValue(element) {
                val range = element.getUserData(NAME_RANGE_KEY)
                if (range == null)
                    return@getCachedValue null
                val name = element.text.substring(range.startOffset, range.endOffset)
                resolveCached(element, name)
            } ?: emptyArray()
        }
        private fun resolveCached(element: PsiElement, name: String): CachedValueProvider.Result<Array<PsiElementResolveResult>>? {
            val results = ArrayList<PsiNamedElement>()
            val dependencies = ArrayList<Any>()
            var prevContainer: PsiElement = element
            var container: PsiElement? = prevContainer.parent
            while (container != null) {
                when (container) {
                    is ZigBlock -> {
                        var stmt: PsiElement? = prevContainer
                        while (stmt != null) {
                            (stmt as? ZigStatement)?.varDeclExprStatement?.varDeclProtoList?.forEach { varDeclProto ->
                                if (validate(varDeclProto, name)) {
                                    results.add(varDeclProto)
                                    dependencies.add(varDeclProto)
                                }
                            }
                            stmt = stmt.prevSibling
                        }
                    }

                    is ZigDecl -> {
                        container.fnDeclProto?.paramDeclList?.paramDeclList?.forEach { paramDecl ->
                            if (validate(paramDecl, name)) {
                                results.add(paramDecl)
                                dependencies.add(paramDecl)
                                return@forEach
                            }
                        }
                    }

                    is ZigContainerMembers -> {
                        container.childrenOfType<ZigContainerDeclaration>().forEach {
                            val decl = it.decl ?: return@forEach
                            decl.fnDeclProto?.let { fnProto ->
                                if (validate(fnProto, name)) {
                                    results.add(fnProto)
                                    dependencies.add(fnProto)
                                    return@forEach
                                }
                            }
                            decl.globalVarDecl?.varDeclProto?.let { varDeclProto ->
                                if (validate(varDeclProto, name)) {
                                    results.add(varDeclProto)
                                    dependencies.add(varDeclProto)
                                    return@forEach
                                }
                            }
                        }
                    }
                }
                prevContainer = container
                container = container.parent
            }
            if (results.isEmpty())
                return null
            val res = results.map { PsiElementResolveResult(it) }.toTypedArray()
            return CachedValueProvider.Result(res, *dependencies.toArray())
        }
        private fun validate(paramDecl: ZigParamDecl, name: String) = paramDecl.identifier?.text?.equals(name) == true
        private fun validate(fnProto: ZigFnDeclProto, name: String) = fnProto.identifier?.text?.equals(name) == true
        private fun validate(varDeclProto: ZigVarDeclProto, name: String) = varDeclProto.identifier?.text?.equals(name) == true
    }


    override fun handleElementRename(newElementName: String): PsiElement? {
        return element.rename(newElementName)
    }
}