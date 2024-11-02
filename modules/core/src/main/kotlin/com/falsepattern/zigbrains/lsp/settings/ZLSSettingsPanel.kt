/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2024 FalsePattern
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

import com.falsepattern.zigbrains.direnv.*
import com.falsepattern.zigbrains.lsp.ZLSBundle
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.util.application
import kotlinx.coroutines.launch
import kotlin.io.path.pathString

class ZLSSettingsPanel(private val project: Project?) : Disposable {
    private val zlsPath = textFieldWithBrowseButton(
        project,
        ZLSBundle.message("settings.zls-path.browse.title"),
        FileChooserDescriptorFactory.createSingleFileDescriptor(),
    ).also { Disposer.register(this, it) }
    private val zlsConfigPath = textFieldWithBrowseButton(
        project,
        ZLSBundle.message("settings.zls-config-path.browse.title"),
        FileChooserDescriptorFactory.createSingleFileDescriptor()
    ).also { Disposer.register(this, it) }

    private val buildOnSave = JBCheckBox().apply { toolTipText = ZLSBundle.message("settings.build-on-save.tooltip") }
    private val buildOnSaveStep = ExtendableTextField().apply { toolTipText = ZLSBundle.message("settings.build-on-save-step.tooltip") }
    private val globalVarDeclarations = JBCheckBox()
    private val comptimeInterpreter = JBCheckBox()
    private val inlayHints = JBCheckBox()
    private val inlayHintsCompact = JBCheckBox().apply { toolTipText = ZLSBundle.message("settings.inlay-hints-compact.tooltip") }

    private val messageTrace = JBCheckBox()
    private val debug = JBCheckBox()
    private val direnv = JBCheckBox(ZLSBundle.message("settings.zls-path.use-direnv.label"))

    fun attach(panel: Panel) = with(panel) {
        group(ZLSBundle.message("settings.group.title")) {
            row(ZLSBundle.message("settings.zls-path.label")) {
                cell(zlsPath).resizableColumn().align(AlignX.FILL)
                if (DirenvCmd.direnvInstalled() && project != null) {
                    cell(direnv)
                }
                button(ZLSBundle.message("settings.zls-path.autodetect.label")) {
                    project.zigCoroutineScope.launch {
                        autodetect()
                    }
                }
            }
            row(ZLSBundle.message("settings.zls-config-path.label")) { cell(zlsConfigPath).align(AlignX.FILL) }
            row(ZLSBundle.message("settings.inlay-hints.label")) { cell(inlayHints) }
            row(ZLSBundle.message("settings.inlay-hints-compact.label")) { cell(inlayHintsCompact) }
            row(ZLSBundle.message("settings.build-on-save.label")) { cell(buildOnSave) }
            row(ZLSBundle.message("settings.build-on-save-step.label")) { cell(buildOnSaveStep).resizableColumn().align(AlignX.FILL) }
            row(ZLSBundle.message("settings.global-var-declarations.label")) { cell(globalVarDeclarations) }
            row(ZLSBundle.message("settings.comptime-interpreter.label")) { cell(comptimeInterpreter) }
        }
        group(ZLSBundle.message("dev-settings.group.title")) {
            row(ZLSBundle.message("dev-settings.debug.label")) { cell(debug) }
            row(ZLSBundle.message("dev-settings.message-trace.label")) { cell(messageTrace) }
        }
    }

    var data
        get() = ZLSSettings(
            direnv.isSelected,
            zlsPath.text,
            zlsConfigPath.text,
            debug.isSelected,
            messageTrace.isSelected,
            buildOnSave.isSelected,
            buildOnSaveStep.text,
            globalVarDeclarations.isSelected,
            comptimeInterpreter.isSelected,
            inlayHints.isSelected,
            inlayHintsCompact.isSelected
        )
        set(value) {
            direnv.isSelected = value.direnv
            zlsPath.text = value.zlsPath
            zlsConfigPath.text = value.zlsConfigPath
            debug.isSelected = value.debug
            messageTrace.isSelected = value.messageTrace
            buildOnSave.isSelected = value.buildOnSave
            buildOnSaveStep.text = value.buildOnSaveStep
            globalVarDeclarations.isSelected = value.globalVarDeclarations
            comptimeInterpreter.isSelected = value.comptimeInterpreter
            inlayHints.isSelected = value.inlayHints
            inlayHintsCompact.isSelected = value.inlayHintsCompact
        }

    suspend fun autodetect() {
        getDirenv().findExecutableOnPATH("zls")?.let { zlsPath.text = it.pathString }
    }

    override fun dispose() {
    }

    private suspend fun getDirenv(): Env {
        if (!direnv.isSelected)
            return emptyEnv
        return project.getDirenv()
    }
}