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

package com.falsepattern.zigbrains.zig.annotator.color

import com.falsepattern.zigbrains.zig.highlighter.ZigSyntaxHighlighter
import com.falsepattern.zigbrains.zig.psi.ZigFieldInit
import com.falsepattern.zigbrains.zig.psi.ZigFnProto
import com.falsepattern.zigbrains.zig.psi.ZigParamDecl
import com.falsepattern.zigbrains.zig.psi.ZigPrimaryTypeExpr
import com.falsepattern.zigbrains.zig.psi.ZigVarDeclProto
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

class ZigSimpleColorAnnotator: Annotator, DumbAware {
	override fun annotate( node: PsiElement, holder: AnnotationHolder ) {
		when ( node ) {
			is PsiWhiteSpace -> return
			is ZigVarDeclProto -> {
				holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
					.range( node.identifier )
					.textAttributes(ZigSyntaxHighlighter.VARIABLE_DECL )
					.create()

				node.expr?.let {
					holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
						.range(it)
						.textAttributes(ZigSyntaxHighlighter.TYPE_REF)
						.create()
				}
			}
			is ZigFnProto -> {
				holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
					.range( node.expr )
					.textAttributes(ZigSyntaxHighlighter.TYPE_REF )
					.create()

				node.identifier?.let { name: PsiElement ->
					var attr = ZigSyntaxHighlighter.FUNCTION_DECL

					// we might be a generic type
					val expr = node.expr
					if ( expr is ZigPrimaryTypeExpr ) {
						expr.identifier?.let { ident ->
							if ( ident.text == "type" ) {
								attr = ZigSyntaxHighlighter.TYPE_DECL
							}
						}

					}
					holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
						.range( name )
						.textAttributes( attr )
						.create()
				}
			}
			is ZigFieldInit -> {
				holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
					.range( node.identifier )
					.textAttributes(ZigSyntaxHighlighter.PROPERTY_REF )
					.create()
			}
			is ZigParamDecl -> {
				node.identifier?.let {
					var attr = ZigSyntaxHighlighter.PARAMETER
					if ( node.keywordComptime != null && node.paramType?.text == "type" ) {
						attr = ZigSyntaxHighlighter.TYPE_PARAM_DECL
					}

					holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
						.range(it)
						.textAttributes(attr)
						.create()
				}

				val expr = node.paramType?.expr
				if ( expr is ZigPrimaryTypeExpr ) {
					val ident = expr.identifier
					if ( ident != null ) {
						holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
							.range(ident)
							.textAttributes(ZigSyntaxHighlighter.TYPE_REF)
							.create()
					}
				}
			}
		}
	}
}
