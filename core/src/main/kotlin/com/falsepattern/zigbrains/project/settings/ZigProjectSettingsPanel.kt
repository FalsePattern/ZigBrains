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

package com.falsepattern.zigbrains.project.settings

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.direnv.DirenvCmd
import com.falsepattern.zigbrains.project.toolchain.LocalZigToolchain
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainProvider
import com.falsepattern.zigbrains.shared.coroutine.launchWithEDT
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.swing.event.DocumentEvent
import kotlin.io.path.pathString

class ZigProjectSettingsPanel(private val holder: ZigProjectConfigurationProvider.SettingsPanelHolder, private val project: Project) : ZigProjectConfigurationProvider.SettingsPanel {
    private val direnv = JBCheckBox(ZigBrainsBundle.message("settings.project.label.direnv")).apply { addActionListener {
        dispatchDirenvUpdate()
    } }
    private val pathToToolchain = textFieldWithBrowseButton(
        project,
        ZigBrainsBundle.message("dialog.title.zig-toolchain"),
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
    ).also {
        it.textField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                dispatchUpdateUI()
            }
        })
        Disposer.register(this, it)
    }
    private val toolchainVersion = JBTextArea().also { it.isEditable = false }
    private val stdFieldOverride = JBCheckBox(ZigBrainsBundle.message("settings.project.label.override-std")).apply {
        addChangeListener {
            if (isSelected) {
                pathToStd.isEnabled = true
            } else {
                pathToStd.isEnabled = false
                updateUI()
            }
        }
    }
    private val pathToStd = textFieldWithBrowseButton(
        project,
        ZigBrainsBundle.message("dialog.title.zig-std"),
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
    ).also { Disposer.register(this, it) }
    private var debounce: Job? = null

    private fun dispatchDirenvUpdate() {
        holder.panels.forEach {
            it.direnvChanged(direnv.isSelected)
        }
    }

    override fun direnvChanged(state: Boolean) {
        dispatchAutodetect(true)
    }

    private fun dispatchAutodetect(force: Boolean) {
        project.zigCoroutineScope.launchWithEDT {
            withModalProgress(ModalTaskOwner.component(pathToToolchain), "Detecting Zig...", TaskCancellation.cancellable()) {
                autodetect(force)
            }
        }
    }

    suspend fun autodetect(force: Boolean) {
        if (!force && pathToToolchain.text.isNotBlank())
            return
        val data = UserDataHolderBase()
        data.putUserData(LocalZigToolchain.DIRENV_KEY, !project.isDefault && direnv.isSelected && DirenvCmd.direnvInstalled())
        val tc = ZigToolchainProvider.suggestToolchain(project, data) ?: return
        if (tc !is LocalZigToolchain) {
            TODO("Implement non-local zig toolchain in config")
        }
        if (force || pathToToolchain.text.isBlank()) {
            pathToToolchain.text = tc.location.pathString
            dispatchUpdateUI()
        }
    }

    override var data
        get() = ZigProjectSettings(
            direnv.isSelected,
            stdFieldOverride.isSelected,
            pathToStd.text.ifBlank { null },
            pathToToolchain.text.ifBlank { null }
        )
        set(value) {
            direnv.isSelected = value.direnv
            pathToToolchain.text = value.toolchainPath ?: ""
            stdFieldOverride.isSelected = value.overrideStdPath
            pathToStd.text = value.explicitPathToStd ?: ""
            pathToStd.isEnabled = value.overrideStdPath
            dispatchUpdateUI()
        }

    override fun attach(p: Panel): Unit = with(p) {
        data = project.zigProjectSettings.state
        if (project.isDefault) {
            row(ZigBrainsBundle.message("settings.project.label.toolchain")) {
                cell(pathToToolchain).resizableColumn().align(AlignX.FILL)
            }
            row(ZigBrainsBundle.message("settings.project.label.toolchain-version")) {
                cell(toolchainVersion)
            }
        } else {
            group(ZigBrainsBundle.message("settings.project.group.title")) {
                row(ZigBrainsBundle.message("settings.project.label.toolchain")) {
                    cell(pathToToolchain).resizableColumn().align(AlignX.FILL)
                    if (DirenvCmd.direnvInstalled()) {
                        cell(direnv)
                    }
                }
                row(ZigBrainsBundle.message("settings.project.label.toolchain-version")) {
                    cell(toolchainVersion)
                }
                row(ZigBrainsBundle.message("settings.project.label.std-location")) {
                    cell(pathToStd).resizableColumn().align(AlignX.FILL)
                    cell(stdFieldOverride)
                }
            }
        }
        dispatchAutodetect(false)
    }

    private fun dispatchUpdateUI() {
        debounce?.cancel("New debounce")
        debounce = project.zigCoroutineScope.launch {
            updateUI()
        }
    }

    private suspend fun updateUI() {
        delay(200)
        val pathToToolchain = this.pathToToolchain.text.ifBlank { null }?.toNioPathOrNull()
        if (pathToToolchain == null) {
            withEDTContext(ModalityState.any()) {
                toolchainVersion.text = "[toolchain path empty or invalid]"
                if (!stdFieldOverride.isSelected) {
                    pathToStd.text = ""
                }
            }
            return
        }
        val toolchain = LocalZigToolchain(pathToToolchain)
        val zig = toolchain.zig
        val env = zig.getEnv(project).getOrElse { throwable ->
            throwable.printStackTrace()
            withEDTContext(ModalityState.any()) {
                toolchainVersion.text = "[failed to run \"zig env\"]\n${throwable.message}"
                if (!stdFieldOverride.isSelected) {
                    pathToStd.text = ""
                }
            }
            return
        }
        val version = env.version
        val stdPath = env.stdPath(toolchain, project)

        withEDTContext(ModalityState.any()) {
            toolchainVersion.text = version
            toolchainVersion.foreground = JBColor.foreground()
            if (!stdFieldOverride.isSelected) {
                pathToStd.text = stdPath?.pathString ?: ""
            }
        }
    }

    override fun dispose() {
        debounce?.cancel("Disposed")
    }
}