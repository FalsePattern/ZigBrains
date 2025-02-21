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

package com.falsepattern.zigbrains.zig.highlighter

import com.falsepattern.zigbrains.zig.psi.ZigNamedElement
import com.falsepattern.zigbrains.zig.psi.ZigPrimaryTypeExpr
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class ZigSemanticHighlighter: Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when(element) {
            is ZigPrimaryTypeExpr -> {
                val id = element.identifier ?: return
                if (primitiveHighlight(id, holder)) {
                    return
                }
                if (referenceHighlight(element, holder)) {
                    return
                }
            }
            is ZigNamedElement -> {
                val id = element.identifyingElement ?: return
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(id.textRange)
                    .textAttributes(element.declarationAttribute)
                    .create()
            }
        }
    }

    private fun referenceHighlight(element: PsiElement, holder: AnnotationHolder): Boolean {
        val ref = element.reference ?: return false
        val resolved = ref.resolve() ?: return false
        if (resolved is ZigNamedElement) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(ref.absoluteRange)
                .textAttributes(resolved.referenceAttribute)
                .create()
            return true
        }
        return false
    }

    private fun primitiveHighlight(element: PsiElement, holder: AnnotationHolder): Boolean {
        val name = element.text
        if (name in primitives) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.textRange)
                .textAttributes(ZigSyntaxHighlighter.TYPE_REF)
                .create()
            return true
        }
        if (name.startsWith('i') || name.startsWith('u')) {
            val numeric = name.substring(1)
            val num = numeric.toIntOrNull() ?: return false
            if (num < 0 || num > 65535) {
                return false
            }
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.textRange)
                .textAttributes(ZigSyntaxHighlighter.TYPE_REF)
                .create()
            return true
        }
        return false
    }

    private val primitives = setOf(
        "isize",
        "usize",
        "c_char",
        "c_short",
        "c_ushort",
        "c_int",
        "c_uint",
        "c_long",
        "c_ulong",
        "c_longlong",
        "c_ulonglong",
        "c_longdouble",
        "f16",
        "f32",
        "f64",
        "f80",
        "f128",
        "bool",
        "anyopaque",
        "void",
        "noreturn",
        "type",
        "anyerror",
        "comptime_int",
        "comptime_float",
    )

}