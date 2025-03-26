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

import com.falsepattern.zigbrains.direnv.DirenvCmd
import com.falsepattern.zigbrains.direnv.Env
import com.falsepattern.zigbrains.direnv.emptyEnv
import com.falsepattern.zigbrains.direnv.getDirenv
import com.falsepattern.zigbrains.lsp.ZLSBundle
import com.falsepattern.zigbrains.lsp.config.SemanticTokens
import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.project.settings.zigProjectSettings
import com.falsepattern.zigbrains.shared.cli.call
import com.falsepattern.zigbrains.shared.cli.createCommandLineSafe
import com.falsepattern.zigbrains.shared.coroutine.launchWithEDT
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.execution.processTools.mapFlat
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.annotations.PropertyKey
import javax.swing.event.DocumentEvent
import kotlin.io.path.pathString

@Suppress("PrivatePropertyName")
class ZLSSettingsPanel(private val project: Project) : ZigProjectConfigurationProvider.SettingsPanel {
    private val zlsPath = textFieldWithBrowseButton(
        project,
        FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
            .withTitle(ZLSBundle.message("settings.zls-path.browse.title")),
    ).also {
        it.textField.document.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(p0: DocumentEvent) {
                dispatchUpdateUI()
            }
        })
        Disposer.register(this, it)
    }
    private val zlsConfigPath = textFieldWithBrowseButton(
        project,
        FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
            .withTitle(ZLSBundle.message("settings.zls-config-path.browse.title"))
    ).also { Disposer.register(this, it) }

    private val zlsVersion = JBTextArea().also { it.isEditable = false }

    private var debounce: Job? = null

    private var direnv: Boolean = project.zigProjectSettings.state.direnv

    private val inlayHints = JBCheckBox()
    private val enable_snippets = JBCheckBox()
    private val enable_argument_placeholders = JBCheckBox()
    private val completion_label_details = JBCheckBox()
    private val enable_build_on_save = JBCheckBox()
    private val build_on_save_args = ExtendableTextField()
    private val semantic_tokens = ComboBox(SemanticTokens.entries.toTypedArray())
    private val inlay_hints_show_variable_type_hints = JBCheckBox()
    private val inlay_hints_show_struct_literal_field_type = JBCheckBox()
    private val inlay_hints_show_parameter_name = JBCheckBox()
    private val inlay_hints_show_builtin = JBCheckBox()
    private val inlay_hints_exclude_single_argument = JBCheckBox()
    private val inlay_hints_hide_redundant_param_names = JBCheckBox()
    private val inlay_hints_hide_redundant_param_names_last_token = JBCheckBox()
    private val warn_style = JBCheckBox()
    private val highlight_global_var_declarations = JBCheckBox()
    private val skip_std_references = JBCheckBox()
    private val prefer_ast_check_as_child_process = JBCheckBox()
    private val builtin_path = ExtendableTextField()
    private val build_runner_path = ExtendableTextField()
    private val global_cache_path = ExtendableTextField()

    override fun attach(p: Panel) = with(p) {
        if (!project.isDefault) {
            group(ZLSBundle.message("settings.group.title")) {
                fancyRow(
                    "settings.zls-path.label",
                    "settings.zls-path.tooltip"
                ) {
                    cell(zlsPath).resizableColumn().align(AlignX.FILL)
                }
                row(ZLSBundle.message("settings.zls-version.label")) {
                    cell(zlsVersion)
                }
                fancyRow(
                    "settings.zls-config-path.label",
                    "settings.zls-config-path.tooltip"
                ) { cell(zlsConfigPath).align(AlignX.FILL) }
                fancyRow(
                    "settings.enable_snippets.label",
                    "settings.enable_snippets.tooltip"
                ) { cell(enable_snippets) }
                fancyRow(
                    "settings.enable_argument_placeholders.label",
                    "settings.enable_argument_placeholders.tooltip"
                ) { cell(enable_argument_placeholders) }
                fancyRow(
                    "settings.completion_label_details.label",
                    "settings.completion_label_details.tooltip"
                ) { cell(completion_label_details) }
                fancyRow(
                    "settings.enable_build_on_save.label",
                    "settings.enable_build_on_save.tooltip"
                ) { cell(enable_build_on_save) }
                fancyRow(
                    "settings.build_on_save_args.label",
                    "settings.build_on_save_args.tooltip"
                ) { cell(build_on_save_args).resizableColumn().align(AlignX.FILL) }
                fancyRow(
                    "settings.semantic_tokens.label",
                    "settings.semantic_tokens.tooltip"
                ) { cell(semantic_tokens) }
                group(ZLSBundle.message("settings.inlay-hints-group.label")) {
                    fancyRow(
                        "settings.inlay-hints-enable.label",
                        "settings.inlay-hints-enable.tooltip"
                    ) { cell(inlayHints) }
                    fancyRow(
                        "settings.inlay_hints_show_variable_type_hints.label",
                        "settings.inlay_hints_show_variable_type_hints.tooltip"
                    ) { cell(inlay_hints_show_variable_type_hints) }
                    fancyRow(
                        "settings.inlay_hints_show_struct_literal_field_type.label",
                        "settings.inlay_hints_show_struct_literal_field_type.tooltip"
                    ) { cell(inlay_hints_show_struct_literal_field_type) }
                    fancyRow(
                        "settings.inlay_hints_show_parameter_name.label",
                        "settings.inlay_hints_show_parameter_name.tooltip"
                    ) { cell(inlay_hints_show_parameter_name) }
                    fancyRow(
                        "settings.inlay_hints_show_builtin.label",
                        "settings.inlay_hints_show_builtin.tooltip"
                    ) { cell(inlay_hints_show_builtin) }
                    fancyRow(
                        "settings.inlay_hints_exclude_single_argument.label",
                        "settings.inlay_hints_exclude_single_argument.tooltip"
                    ) { cell(inlay_hints_exclude_single_argument) }
                    fancyRow(
                        "settings.inlay_hints_hide_redundant_param_names.label",
                        "settings.inlay_hints_hide_redundant_param_names.tooltip"
                    ) { cell(inlay_hints_hide_redundant_param_names) }
                    fancyRow(
                        "settings.inlay_hints_hide_redundant_param_names_last_token.label",
                        "settings.inlay_hints_hide_redundant_param_names_last_token.tooltip"
                    ) { cell(inlay_hints_hide_redundant_param_names_last_token) }
                }
                fancyRow(
                    "settings.warn_style.label",
                    "settings.warn_style.tooltip"
                ) { cell(warn_style) }
                fancyRow(
                    "settings.highlight_global_var_declarations.label",
                    "settings.highlight_global_var_declarations.tooltip"
                ) { cell(highlight_global_var_declarations) }
                fancyRow(
                    "settings.skip_std_references.label",
                    "settings.skip_std_references.tooltip"
                ) { cell(skip_std_references) }
                fancyRow(
                    "settings.prefer_ast_check_as_child_process.label",
                    "settings.prefer_ast_check_as_child_process.tooltip"
                ) { cell(prefer_ast_check_as_child_process) }
                fancyRow(
                    "settings.builtin_path.label",
                    "settings.builtin_path.tooltip"
                ) { cell(builtin_path).resizableColumn().align(AlignX.FILL) }
                fancyRow(
                    "settings.build_runner_path.label",
                    "settings.build_runner_path.tooltip"
                ) { cell(build_runner_path).resizableColumn().align(AlignX.FILL) }
                fancyRow(
                    "settings.global_cache_path.label",
                    "settings.global_cache_path.tooltip"
                ) { cell(global_cache_path).resizableColumn().align(AlignX.FILL) }
            }
        }
        dispatchAutodetect(false)
    }

    override fun direnvChanged(state: Boolean) {
        direnv = state
        dispatchAutodetect(true)
    }

    override var data
        get() = ZLSSettings(
            zlsPath.text,
            zlsConfigPath.text,
            inlayHints.isSelected,
            enable_snippets.isSelected,
            enable_argument_placeholders.isSelected,
            completion_label_details.isSelected,
            enable_build_on_save.isSelected,
            build_on_save_args.text,
            semantic_tokens.item ?: SemanticTokens.full,
            inlay_hints_show_variable_type_hints.isSelected,
            inlay_hints_show_struct_literal_field_type.isSelected,
            inlay_hints_show_parameter_name.isSelected,
            inlay_hints_show_builtin.isSelected,
            inlay_hints_exclude_single_argument.isSelected,
            inlay_hints_hide_redundant_param_names.isSelected,
            inlay_hints_hide_redundant_param_names_last_token.isSelected,
            warn_style.isSelected,
            highlight_global_var_declarations.isSelected,
            skip_std_references.isSelected,
            prefer_ast_check_as_child_process.isSelected,
            builtin_path.text?.ifBlank { null },
            build_runner_path.text?.ifBlank { null },
            global_cache_path.text?.ifBlank { null },
        )
        set(value) {
            zlsPath.text = value.zlsPath
            zlsConfigPath.text = value.zlsConfigPath
            inlayHints.isSelected = value.inlayHints
            enable_snippets.isSelected = value.enable_snippets
            enable_argument_placeholders.isSelected = value.enable_argument_placeholders
            completion_label_details.isSelected = value.completion_label_details
            enable_build_on_save.isSelected = value.enable_build_on_save
            build_on_save_args.text = value.build_on_save_args
            semantic_tokens.item = value.semantic_tokens
            inlay_hints_show_variable_type_hints.isSelected = value.inlay_hints_show_variable_type_hints
            inlay_hints_show_struct_literal_field_type.isSelected = value.inlay_hints_show_struct_literal_field_type
            inlay_hints_show_parameter_name.isSelected = value.inlay_hints_show_parameter_name
            inlay_hints_show_builtin.isSelected = value.inlay_hints_show_builtin
            inlay_hints_exclude_single_argument.isSelected = value.inlay_hints_exclude_single_argument
            inlay_hints_hide_redundant_param_names.isSelected = value.inlay_hints_hide_redundant_param_names
            inlay_hints_hide_redundant_param_names_last_token.isSelected =
                value.inlay_hints_hide_redundant_param_names_last_token
            warn_style.isSelected = value.warn_style
            highlight_global_var_declarations.isSelected = value.highlight_global_var_declarations
            skip_std_references.isSelected = value.skip_std_references
            prefer_ast_check_as_child_process.isSelected = value.prefer_ast_check_as_child_process
            builtin_path.text = value.builtin_path ?: ""
            build_runner_path.text = value.build_runner_path ?: ""
            global_cache_path.text = value.global_cache_path ?: ""
            dispatchUpdateUI()
        }

    private fun dispatchAutodetect(force: Boolean) {
        project.zigCoroutineScope.launchWithEDT {
            withModalProgress(ModalTaskOwner.component(zlsPath), "Detecting ZLS...", TaskCancellation.cancellable()) {
                autodetect(force)
            }
        }
    }

    suspend fun autodetect(force: Boolean) {
        if (force || zlsPath.text.isBlank()) {
            getDirenv().findExecutableOnPATH("zls")?.let {
                if (force || zlsPath.text.isBlank()) {
                    zlsPath.text = it.pathString
                    dispatchUpdateUI()
                }
            }
        }
    }

    override fun dispose() {
        debounce?.cancel("Disposed")
    }

    private suspend fun getDirenv(): Env {
        if (!project.isDefault && DirenvCmd.direnvInstalled() && direnv)
            return project.getDirenv()
        return emptyEnv
    }

    private fun dispatchUpdateUI() {
        debounce?.cancel("New debounce")
        debounce = project.zigCoroutineScope.launch {
            updateUI()
        }
    }

    private suspend fun updateUI() {
        if (project.isDefault)
            return
        delay(200)
        val zlsPath = this.zlsPath.text.ifBlank { null }?.toNioPathOrNull()
        if (zlsPath == null) {
            withEDTContext(ModalityState.any()) {
                zlsVersion.text = "[zls path empty or invalid]"
            }
            return
        }
        val workingDir = project.guessProjectDir()?.toNioPathOrNull()
        val result = createCommandLineSafe(workingDir, zlsPath, "version")
            .map { it.withEnvironment(getDirenv().env) }
            .mapFlat { it.call() }
            .getOrElse { throwable ->
                throwable.printStackTrace()
                withEDTContext(ModalityState.any()) {
                    zlsVersion.text = "[failed to run \"zls version\"]\n${throwable.message}"
                }
                return
            }
        val version = result.stdout.trim()
        withEDTContext(ModalityState.any()) {
            zlsVersion.text = version
            zlsVersion.foreground = JBColor.foreground()
        }
    }
}

private fun Panel.fancyRow(
    label: @PropertyKey(resourceBundle = "zigbrains.lsp.Bundle") String,
    tooltip: @PropertyKey(resourceBundle = "zigbrains.lsp.Bundle") String,
    cb: Row.() -> Unit
) = row(ZLSBundle.message(label)) {
    contextHelp(ZLSBundle.message(tooltip))
    cb()
}