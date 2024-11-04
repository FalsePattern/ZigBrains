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

package com.falsepattern.zigbrains.project.settings

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.direnv.DirenvCmd
import com.falsepattern.zigbrains.project.toolchain.LocalZigToolchain
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainProvider
import com.falsepattern.zigbrains.shared.coroutine.launchWithEDT
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.swing.event.DocumentEvent
import kotlin.io.path.pathString

class ZigProjectSettingsPanel(private val project: Project?) : Disposable {
    private val direnv = JBCheckBox(ZigBrainsBundle.message("settings.project.label.direnv"))
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
    private val toolchainVersion = JBLabel()
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

    suspend fun autodetect() {
        val data = UserDataHolderBase()
        data.putUserData(LocalZigToolchain.DIRENV_KEY, direnv.isSelected)
        val tc = ZigToolchainProvider.suggestToolchain(project, data) ?: return
        if (tc !is LocalZigToolchain) {
            TODO("Implement non-local zig toolchain in config")
        }
        pathToToolchain.text = tc.location.pathString
        dispatchUpdateUI()
    }

    var data
        get() = ZigProjectSettings(
            direnv.isSelected,
            stdFieldOverride.isSelected,
            pathToStd.text,
            pathToToolchain.text
        )
        set(value) {
            direnv.isSelected = value.direnv
            pathToToolchain.text = value.toolchainPath ?: ""
            stdFieldOverride.isSelected = value.overrideStdPath
            pathToStd.text = value.explicitPathToStd ?: ""
            pathToStd.isEnabled = value.overrideStdPath
            dispatchUpdateUI()
        }

    fun attach(p: Panel): Unit = with(p) {
        val project = project ?: ProjectManager.getInstance().defaultProject
        data = project.zigProjectSettings.state
        group(ZigBrainsBundle.message("settings.project.group.title")) {
            row(ZigBrainsBundle.message("settings.project.label.toolchain")) {
                cell(pathToToolchain).resizableColumn().align(AlignX.FILL)
                if (DirenvCmd.direnvInstalled() && !project.isDefault) {
                    cell(direnv)
                }
                button(ZigBrainsBundle.message("settings.project.label.toolchain-autodetect")) {
                    project.zigCoroutineScope.launchWithEDT {
                        withModalProgress(ModalTaskOwner.component(pathToToolchain), "Detecting Zig...", TaskCancellation.cancellable()) {
                            autodetect()
                        }
                    }
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

    private fun dispatchUpdateUI() {
        debounce?.cancel("New debounce")
        debounce = project.zigCoroutineScope.launch {
            updateUI()
        }
    }


    private suspend fun updateUI() {
        val pathToToolchain = this.pathToToolchain.text.toNioPathOrNull()
        delay(200)
        val toolchain = pathToToolchain?.let { LocalZigToolchain(it) }
        val zig = toolchain?.zig
        val env = zig?.getEnv(project)
        val version = env?.version
        val stdPath = env?.stdPath(toolchain)
        withEDTContext {
            toolchainVersion.text = version ?: ""
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