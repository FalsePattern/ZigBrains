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

package com.falsepattern.zigbrains.project.toolchain.downloader

import com.falsepattern.zigbrains.Icons
import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainListService
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.local.LocalZigToolchain
import com.falsepattern.zigbrains.shared.coroutine.asContextElement
import com.falsepattern.zigbrains.shared.coroutine.runInterruptibleEDT
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.awt.Component
import javax.swing.event.DocumentEvent

object LocalSelector {
    suspend fun browseFromDisk(component: Component): ZigToolchain? {
        return runInterruptibleEDT(component.asContextElement()) {
            doBrowseFromDisk()
        }
    }

    @RequiresEdt
    private fun doBrowseFromDisk(): ZigToolchain? {
        val dialog = DialogBuilder()
        val name = JBTextField().also { it.columns = 25 }
        val path = textFieldWithBrowseButton(
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle(ZigBrainsBundle.message("settings.toolchain.local-selector.chooser.title"))
        )
        Disposer.register(dialog, path)
        lateinit var errorMessageBox: JBLabel
        fun verify(path: String) {
            val tc = LocalZigToolchain.tryFromPathString(path)?.second
            if (tc == null) {
                errorMessageBox.icon = AllIcons.General.Error
                errorMessageBox.text = ZigBrainsBundle.message("settings.toolchain.local-selector.state.invalid")
                dialog.setOkActionEnabled(false)
            } else if (ZigToolchainListService
                    .getInstance()
                    .toolchains
                    .mapNotNull { it.second as? LocalZigToolchain }
                    .any { it.location == tc.location }
            ) {
                errorMessageBox.icon = AllIcons.General.Warning
                errorMessageBox.text = tc.name?.let { ZigBrainsBundle.message("settings.toolchain.local-selector.state.already-exists-named", it) }
                                       ?: ZigBrainsBundle.message("settings.toolchain.local-selector.state.already-exists-unnamed")
                dialog.setOkActionEnabled(true)
            } else {
                errorMessageBox.icon = AllIcons.General.Information
                errorMessageBox.text = ZigBrainsBundle.message("settings.toolchain.local-selector.state.ok")
                dialog.setOkActionEnabled(true)
            }
            val prevNameDefault = name.emptyText.text.trim() == name.text.trim() || name.text.isBlank()
            name.emptyText.text = tc?.name ?: ""
            if (prevNameDefault) {
                name.text = name.emptyText.text
            }
        }
        path.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                verify(path.text)
            }
        })
        val center = panel {
            row(ZigBrainsBundle.message("settings.toolchain.local-selector.name.label")) {
                cell(name).resizableColumn().align(AlignX.FILL)
            }
            row(ZigBrainsBundle.message("settings.toolchain.local-selector.path.label")) {
                cell(path).resizableColumn().align(AlignX.FILL)
            }
            row {
                errorMessageBox = JBLabel()
                cell(errorMessageBox)
            }
        }
        dialog.centerPanel(center)
        dialog.setTitle(ZigBrainsBundle.message("settings.toolchain.local-selector.title"))
        dialog.addCancelAction()
        dialog.addOkAction().also { it.setText(ZigBrainsBundle.message("settings.toolchain.local-selector.ok-action")) }
        val chosenFile = FileChooser.chooseFile(
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle(ZigBrainsBundle.message("settings.toolchain.local-selector.chooser.title")),
            null,
            null
        )
        if (chosenFile != null) {
            verify(chosenFile.path)
            path.text = chosenFile.path
        }
        if (!dialog.showAndGet()) {
            return null
        }
        return LocalZigToolchain.tryFromPathString(path.text)?.second?.also { it.copy(name = name.text.ifBlank { null } ?: it.name) }
    }
}