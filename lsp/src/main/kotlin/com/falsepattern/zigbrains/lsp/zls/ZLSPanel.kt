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

package com.falsepattern.zigbrains.lsp.zls

import com.falsepattern.zigbrains.lsp.ZLSBundle
import com.falsepattern.zigbrains.lsp.settings.ZLSSettingsPanel
import com.falsepattern.zigbrains.project.toolchain.ui.ImmutableNamedElementPanelBase
import com.falsepattern.zigbrains.shared.cli.call
import com.falsepattern.zigbrains.shared.cli.createCommandLineSafe
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.sanitizedPathString
import com.falsepattern.zigbrains.shared.sanitizedToNioPath
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.swing.event.DocumentEvent

class ZLSPanel() : ImmutableNamedElementPanelBase<ZLSVersion>() {
    private val pathToZLS = textFieldWithBrowseButton(
        null,
        FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle(ZLSBundle.message("dialog.title.zls"))
    ).also {
        it.textField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                dispatchUpdateUI()
            }
        })
        Disposer.register(this, it)
    }
    private val zlsVersion = JBTextArea().also { it.isEditable = false }
    private var settingsPanel: ZLSSettingsPanel? = null
    private var debounce: Job? = null

    override fun attach(panel: Panel): Unit = with(panel) {
        super.attach(panel)
        row(ZLSBundle.message("settings.panel.path.label")) {
            cell(pathToZLS).resizableColumn().align(AlignX.FILL)
        }
        row(ZLSBundle.message("settings.panel.version.label")) {
            cell(zlsVersion)
        }
        val sp = ZLSSettingsPanel()
        panel.collapsibleGroup(ZLSBundle.message("settings.panel.settings.group.label"), indent = false) {
            sp.attach(this@collapsibleGroup)
        }
        settingsPanel = sp
    }

    override fun isModified(elem: ZLSVersion): Boolean {
        val name = nameFieldValue ?: return false
        val path = this.pathToZLS.text.sanitizedToNioPath() ?: return false
        return name != elem.name || elem.path != path || settingsPanel?.isModified(elem.settings) == true
    }

    override fun apply(elem: ZLSVersion): ZLSVersion? {
        val path = this.pathToZLS.text.sanitizedToNioPath() ?: return null
        return elem.copy(path = path, name = nameFieldValue ?: "", settings = settingsPanel?.apply(elem.settings) ?: elem.settings)
    }

    override fun reset(elem: ZLSVersion?) {
        nameFieldValue = elem?.name ?: ""
        this.pathToZLS.text = elem?.path?.sanitizedPathString ?: ""
        settingsPanel?.reset(elem?.settings)
        dispatchUpdateUI()
    }

    private fun dispatchUpdateUI() {
        debounce?.cancel("New debounce")
        debounce = zigCoroutineScope.launch {
            updateUI()
        }
    }

    private suspend fun updateUI() {
        delay(200)
        val pathToZLS = this.pathToZLS.text.sanitizedToNioPath()
        if (pathToZLS == null) {
            withEDTContext(ModalityState.any()) {
                zlsVersion.text = "[zls path empty or invalid]"
            }
            return
        }
        val versionCommand = createCommandLineSafe(null, pathToZLS, "--version").getOrElse {
            withEDTContext(ModalityState.any()) {
                zlsVersion.text = "[could not create \"zls --version\" command]\n${it.message}"
            }
            return
        }
        val result = versionCommand.call().getOrElse {
            withEDTContext(ModalityState.any()) {
                zlsVersion.text = "[failed to run \"zls --version\"]\n${it.message}"
            }
            return
        }
        val version = result.stdout.trim()

        withEDTContext(ModalityState.any()) {
            zlsVersion.text = version
            zlsVersion.foreground = JBColor.foreground()
        }
    }

    override fun dispose() {
        debounce?.cancel("Disposed")
        settingsPanel?.dispose()
        settingsPanel = null
    }
}