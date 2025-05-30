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

package com.falsepattern.zigbrains.lsp.settings

import com.falsepattern.zigbrains.lsp.config.SemanticTokens
import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.annotations.NonNls

@Suppress("PropertyName")
data class ZLSSettings(
    @JvmField @Attribute val zlsConfigPath: @NonNls String = "",
    @JvmField @Attribute val inlayHints: Boolean = true,
    @JvmField @Attribute val inlayHintsMaxFileSizeKb: Int = 128,
    @JvmField @Attribute val selectionRanges: Boolean = false,
    @JvmField @Attribute val enable_snippets: Boolean = true,
    @JvmField @Attribute val enable_argument_placeholders: Boolean = true,
    @JvmField @Attribute val completion_label_details: Boolean = true,
    @JvmField @Attribute val enable_build_on_save: Boolean = false,
    @JvmField @Attribute val build_on_save_args: String = "",
    @JvmField @Attribute val semantic_tokens: SemanticTokens = SemanticTokens.full,
    @JvmField @Attribute val inlay_hints_show_variable_type_hints: Boolean = true,
    @JvmField @Attribute val inlay_hints_show_struct_literal_field_type: Boolean = true,
    @JvmField @Attribute val inlay_hints_show_parameter_name: Boolean = true,
    @JvmField @Attribute val inlay_hints_show_builtin: Boolean = true,
    @JvmField @Attribute val inlay_hints_exclude_single_argument: Boolean = true,
    @JvmField @Attribute val inlay_hints_hide_redundant_param_names: Boolean = false,
    @JvmField @Attribute val inlay_hints_hide_redundant_param_names_last_token: Boolean = false,
    @JvmField @Attribute val warn_style: Boolean = false,
    @JvmField @Attribute val highlight_global_var_declarations: Boolean = false,
    @JvmField @Attribute val skip_std_references: Boolean = false,
    @JvmField @Attribute val prefer_ast_check_as_child_process: Boolean = true,
    @JvmField @Attribute val builtin_path: String? = null,
    @JvmField @Attribute val build_runner_path: @NonNls String? = null,
    @JvmField @Attribute val global_cache_path: @NonNls String? = null,
)