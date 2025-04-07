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

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.local.LocalZigToolchain
import com.falsepattern.zigbrains.shared.coroutine.asContextElement
import com.falsepattern.zigbrains.shared.coroutine.runInterruptibleEDT
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.util.whenFocusGained
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.asSafely
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.awt.Component
import java.nio.file.Path
import java.util.*
import javax.swing.DefaultComboBoxModel
import javax.swing.JList
import javax.swing.event.DocumentEvent

//TODO lang
object Downloader {
    suspend fun downloadToolchain(component: Component): ZigToolchain? {
        val info = withModalProgress(
            ModalTaskOwner.component(component),
            "Fetching zig version information",
            TaskCancellation.cancellable()
        ) {
            ZigVersionInfo.downloadVersionList()
        }
        val (downloadPath, version) = runInterruptibleEDT(component.asContextElement()) {
            selectToolchain(info)
        } ?: return null
        withModalProgress(
            ModalTaskOwner.component(component),
            "Installing Zig ${version.version}",
            TaskCancellation.cancellable()
        ) {
            version.downloadAndUnpack(downloadPath)
        }
        return LocalZigToolchain.tryFromPath(downloadPath)
    }

    @RequiresEdt
    private fun selectToolchain(info: List<ZigVersionInfo>): Pair<Path, ZigVersionInfo>? {
        val dialog = DialogBuilder()
        val theList = ComboBox(DefaultComboBoxModel(info.toTypedArray()))
        theList.renderer = object: ColoredListCellRenderer<ZigVersionInfo>() {
            override fun customizeCellRenderer(
                list: JList<out ZigVersionInfo>,
                value: ZigVersionInfo?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                value?.let { append(it.version.rawVersion) }
            }
        }
        val outputPath = textFieldWithBrowseButton(
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle(ZigBrainsBundle.message("dialog.title.zig-toolchain"))
        )
        Disposer.register(dialog, outputPath)
        outputPath.textField.columns = 50

        lateinit var errorMessageBox: JBLabel
        fun onChanged() {
            val path = outputPath.text.ifBlank { null }?.toNioPathOrNull()
            val state = DirectoryState.determine(path)
            if (state.isValid()) {
                errorMessageBox.icon = AllIcons.General.Information
                dialog.setOkActionEnabled(true)
            } else {
                errorMessageBox.icon = AllIcons.General.Error
                dialog.setOkActionEnabled(false)
            }
            errorMessageBox.text = when(state) {
                DirectoryState.Invalid -> "Invalid path"
                DirectoryState.NotAbsolute -> "Must be an absolute path"
                DirectoryState.NotDirectory -> "Path is not a directory"
                DirectoryState.NotEmpty -> "Directory is not empty"
                DirectoryState.CreateNew -> "Directory will be created"
                DirectoryState.Ok -> "Directory OK"
            }
            dialog.window.repaint()
        }
        outputPath.whenFocusGained {
            onChanged()
        }
        outputPath.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                onChanged()
            }
        })
        var archiveSizeCell: Cell<*>? = null
        fun detect(item: ZigVersionInfo) {
            outputPath.text = System.getProperty("user.home") + "/.zig/" + item.version
            val size = item.dist.size
            val sizeMb = size / (1024f * 1024f)
            archiveSizeCell?.comment?.text = "Archive size: %.2fMB".format(sizeMb)
        }
        theList.addItemListener {
            detect(it.item as ZigVersionInfo)
        }
        val center = panel {
            row("Version:") {
                cell(theList).resizableColumn().align(AlignX.FILL)
            }
            row("Location:") {
                cell(outputPath).resizableColumn().align(AlignX.FILL).apply { archiveSizeCell = comment("") }
            }
            row {
                errorMessageBox = JBLabel()
                cell(errorMessageBox)
            }
        }
        detect(info[0])
        dialog.centerPanel(center)
        dialog.setTitle("Zig Downloader")
        dialog.addCancelAction()
        dialog.addOkAction().also { it.setText("Download") }
        if (!dialog.showAndGet()) {
            return null
        }
        val path = outputPath.text.ifBlank { null }?.toNioPathOrNull()
                   ?: return null
        if (!DirectoryState.determine(path).isValid()) {
            return null
        }
        val version = theList.selectedItem?.asSafely<ZigVersionInfo>()
                      ?: return null

        return path to version
    }
}