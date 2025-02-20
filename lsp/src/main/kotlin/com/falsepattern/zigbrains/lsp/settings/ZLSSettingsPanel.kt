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
import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.shared.coroutine.launchWithEDT
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import kotlin.io.path.pathString

class ZLSSettingsPanel(private val project: Project?) : ZigProjectConfigurationProvider.SettingsPanel {
    private val zlsPath = textFieldWithBrowseButton(
        project,
        FileChooserDescriptorFactory.createSingleFileDescriptor().withTitle(ZLSBundle.message("settings.zls-path.browse.title")),
    ).also { Disposer.register(this, it) }
    private val zlsConfigPath = textFieldWithBrowseButton(
        project,
        FileChooserDescriptorFactory.createSingleFileDescriptor().withTitle(ZLSBundle.message("settings.zls-config-path.browse.title"))
    ).also { Disposer.register(this, it) }

    private val buildOnSave = JBCheckBox().apply { toolTipText = ZLSBundle.message("settings.build-on-save.tooltip") }
    private val buildOnSaveStep = ExtendableTextField().apply { toolTipText = ZLSBundle.message("settings.build-on-save-step.tooltip") }
    private val globalVarDeclarations = JBCheckBox()
    private val comptimeInterpreter = JBCheckBox()
    private val inlayHints = JBCheckBox()
    private val inlayHintsCompact = JBCheckBox().apply { toolTipText = ZLSBundle.message("settings.inlay-hints-compact.tooltip") }

    private val messageTrace = JBCheckBox()
    private val debug = JBCheckBox()
    private val direnv = JBCheckBox(ZLSBundle.message("settings.zls-path.use-direnv.label")).apply { addActionListener {
        dispatchAutodetect(true)
    } }

    override fun attach(panel: Panel) = with(panel) {
        group(ZLSBundle.message("settings.group.title")) {
            row(ZLSBundle.message("settings.zls-path.label")) {
                cell(zlsPath).resizableColumn().align(AlignX.FILL)
                if (DirenvCmd.direnvInstalled() && project?.isDefault == false) {
                    cell(direnv)
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
        dispatchAutodetect(false)
    }

    override var data
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
                }
            }
        }
    }

    override fun dispose() {
    }

    private suspend fun getDirenv(): Env {
        if (DirenvCmd.direnvInstalled() && project?.isDefault == false && direnv.isSelected)
            return project.getDirenv()
        return emptyEnv
    }
}