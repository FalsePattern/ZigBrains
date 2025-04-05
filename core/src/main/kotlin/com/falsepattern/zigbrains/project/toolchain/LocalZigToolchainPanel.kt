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

package com.falsepattern.zigbrains.project.toolchain

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.swing.event.DocumentEvent
import kotlin.io.path.pathString

class LocalZigToolchainPanel() : Disposable {
    private val nameField = JBTextField()
    private val pathToToolchain = textFieldWithBrowseButton(
        null,
        FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle(ZigBrainsBundle.message("dialog.title.zig-toolchain"))
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
        null,
        FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle(ZigBrainsBundle.message("dialog.title.zig-std"))
    ).also { Disposer.register(this, it) }
    private var debounce: Job? = null

    fun attach(p: Panel): Unit = with(p) {
        row("Name") {
            cell(nameField).resizableColumn().align(AlignX.FILL)
        }
        separator()
        row(ZigBrainsBundle.message("settings.project.label.toolchain")) {
            cell(pathToToolchain).resizableColumn().align(AlignX.FILL)
        }
        row(ZigBrainsBundle.message("settings.project.label.toolchain-version")) {
            cell(toolchainVersion)
        }
        row(ZigBrainsBundle.message("settings.project.label.std-location")) {
            cell(pathToStd).resizableColumn().align(AlignX.FILL)
            cell(stdFieldOverride)
        }
    }

    fun isModified(cfg: LocalZigToolchainConfigurable): Boolean {
        val name = nameField.text.ifBlank { null } ?: return false
        val tc = cfg.toolchain
        val location = this.pathToToolchain.text.ifBlank { null }?.toNioPathOrNull() ?: return false
        val std = if (stdFieldOverride.isSelected) pathToStd.text.ifBlank { null }?.toNioPathOrNull() else null
        return name != cfg.displayName || tc.location != location || tc.std != std
    }

    fun apply(cfg: LocalZigToolchainConfigurable): Boolean {
        cfg.displayName = nameField.text
        val tc = cfg.toolchain
        val location = this.pathToToolchain.text.ifBlank { null }?.toNioPathOrNull() ?: return false
        val std = if (stdFieldOverride.isSelected) pathToStd.text.ifBlank { null }?.toNioPathOrNull() else null
        tc.location = location
        tc.std = std
        return true
    }

    fun reset(cfg: LocalZigToolchainConfigurable) {
        nameField.text = cfg.displayName ?: ""
        val tc = cfg.toolchain
        this.pathToToolchain.text = tc.location.pathString
        val std = tc.std
        if (std != null) {
            stdFieldOverride.isSelected = true
            pathToStd.text = std.pathString
            pathToStd.isEnabled = true
        } else {
            stdFieldOverride.isSelected = false
            pathToStd.text = ""
            pathToStd.isEnabled = false
            dispatchUpdateUI()
        }
    }

    private fun dispatchUpdateUI() {
        debounce?.cancel("New debounce")
        debounce = zigCoroutineScope.launch {
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
        val env = zig.getEnv(null).getOrElse { throwable ->
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
        val stdPath = env.stdPath(toolchain, null)

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