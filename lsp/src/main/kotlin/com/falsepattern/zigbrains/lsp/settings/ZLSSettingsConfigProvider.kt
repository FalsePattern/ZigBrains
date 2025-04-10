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

import com.falsepattern.zigbrains.lsp.config.ZLSConfig
import com.falsepattern.zigbrains.lsp.config.ZLSConfigProvider
import com.falsepattern.zigbrains.lsp.zls.ZLSService
import com.falsepattern.zigbrains.shared.cli.translateCommandline
import com.intellij.openapi.project.Project

class ZLSSettingsConfigProvider: ZLSConfigProvider {
    override fun getEnvironment(project: Project, previous: ZLSConfig): ZLSConfig {
        val state = ZLSService.getInstance(project).zls?.settings ?: return previous
        return previous.copy(
            enable_snippets = state.enable_snippets,
            enable_argument_placeholders = state.enable_argument_placeholders,
            completion_label_details = state.completion_label_details,
            enable_build_on_save = state.enable_build_on_save,
            build_on_save_args = run {
                val args = state.build_on_save_args
                return@run if (args.isEmpty()) {
                    emptyList()
                } else {
                    translateCommandline(args).toList()
                }
                                     },
            semantic_tokens = state.semantic_tokens,
            inlay_hints_show_variable_type_hints = state.inlay_hints_show_variable_type_hints,
            inlay_hints_show_struct_literal_field_type = state.inlay_hints_show_struct_literal_field_type,
            inlay_hints_show_parameter_name = state.inlay_hints_show_parameter_name,
            inlay_hints_show_builtin = state.inlay_hints_show_builtin,
            inlay_hints_exclude_single_argument = state.inlay_hints_exclude_single_argument,
            inlay_hints_hide_redundant_param_names = state.inlay_hints_hide_redundant_param_names,
            inlay_hints_hide_redundant_param_names_last_token = state.inlay_hints_hide_redundant_param_names_last_token,
            warn_style = state.warn_style,
            highlight_global_var_declarations = state.highlight_global_var_declarations,
            skip_std_references = state.skip_std_references,
            prefer_ast_check_as_child_process = state.prefer_ast_check_as_child_process,
            builtin_path = state.builtin_path,
            build_runner_path = state.build_runner_path,
            global_cache_path = state.global_cache_path,
        )
    }
}