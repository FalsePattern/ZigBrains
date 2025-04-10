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
import com.falsepattern.zigbrains.project.toolchain.zigToolchainList
import com.falsepattern.zigbrains.shared.coroutine.asContextElement
import com.falsepattern.zigbrains.shared.coroutine.launchWithEDT
import com.falsepattern.zigbrains.shared.coroutine.runInterruptibleEDT
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.withUniqueName
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Disposer
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.awt.Component
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.event.DocumentEvent
import kotlin.io.path.pathString

object LocalSelector {
    suspend fun browseFromDisk(component: Component, preSelected: LocalZigToolchain? = null): ZigToolchain? {
        return withEDTContext(component.asContextElement()) {
            doBrowseFromDisk(component, preSelected)
        }
    }

    @RequiresEdt
    private suspend fun doBrowseFromDisk(component: Component, preSelected: LocalZigToolchain?): ZigToolchain? {
        val dialog = DialogBuilder()
        val name = JBTextField().also { it.columns = 25 }
        val path = textFieldWithBrowseButton(
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle(ZigBrainsBundle.message("settings.toolchain.local-selector.chooser.title"))
        )
        Disposer.register(dialog, path)
        lateinit var errorMessageBox: JBLabel
        fun verify(tc: LocalZigToolchain?) {
            var tc = tc
            if (tc == null) {
                errorMessageBox.icon = AllIcons.General.Error
                errorMessageBox.text = ZigBrainsBundle.message("settings.toolchain.local-selector.state.invalid")
                dialog.setOkActionEnabled(false)
            } else {
                val existingToolchain = zigToolchainList
                    .mapNotNull { it.second as? LocalZigToolchain }
                    .firstOrNull { it.location == tc.location }
                if (existingToolchain != null) {
                    errorMessageBox.icon = AllIcons.General.Warning
                    errorMessageBox.text = existingToolchain.name?.let { ZigBrainsBundle.message("settings.toolchain.local-selector.state.already-exists-named", it) }
                        ?: ZigBrainsBundle.message("settings.toolchain.local-selector.state.already-exists-unnamed")
                    dialog.setOkActionEnabled(true)
                } else {
                    errorMessageBox.icon = AllIcons.General.Information
                    errorMessageBox.text = ZigBrainsBundle.message("settings.toolchain.local-selector.state.ok")
                    dialog.setOkActionEnabled(true)
                }
            }
            if (tc != null) {
                tc = zigToolchainList.withUniqueName(tc)
            }
            val prevNameDefault = name.emptyText.text.trim() == name.text.trim() || name.text.isBlank()
            name.emptyText.text = tc?.name ?: ""
            if (prevNameDefault) {
                name.text = name.emptyText.text
            }
        }
        suspend fun verify(path: String) {
            val tc = runCatching { withModalProgress(ModalTaskOwner.component(component), "Resolving toolchain", TaskCancellation.cancellable()) {
                LocalZigToolchain.tryFromPathString(path)
            } }.getOrNull()
            verify(tc)
        }
        val active = AtomicBoolean(false)
        path.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                if (!active.get())
                    return
                zigCoroutineScope.launchWithEDT(ModalityState.current()) {
                    verify(path.text)
                }
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
        if (preSelected == null) {
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
        } else {
            verify(preSelected)
            path.text = preSelected.location.pathString
        }
        active.set(true)
        if (!dialog.showAndGet()) {
            active.set(false)
            return null
        }
        active.set(false)
        return runCatching { withModalProgress(ModalTaskOwner.component(component), "Resolving toolchain", TaskCancellation.cancellable()) {
            LocalZigToolchain.tryFromPathString(path.text)?.let { it.withName(name.text.ifBlank { null } ?: it.name) }
        } }.getOrNull()
    }
}