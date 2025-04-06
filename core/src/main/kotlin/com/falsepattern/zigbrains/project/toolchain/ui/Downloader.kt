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

package com.falsepattern.zigbrains.project.toolchain.ui

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.toolchain.downloader.ZigVersionInfo
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Disposer
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import java.awt.Component
import javax.swing.DefaultComboBoxModel

object Downloader {
    suspend fun openDownloadDialog(component: Component) {
        val info = withModalProgress(
            component.let { ModalTaskOwner.component(it) },
            "Fetching zig version information",
            TaskCancellation.Companion.cancellable()) {
            ZigVersionInfo.Companion.download()
        }
        withEDTContext(ModalityState.stateForComponent(component)) {
            val dialog = DialogBuilder()
            val theList = ComboBox<String>(DefaultComboBoxModel(info.map { it.first }.toTypedArray()))
            val outputPath = textFieldWithBrowseButton(
                null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle(ZigBrainsBundle.message("dialog.title.zig-toolchain"))
            ).also {
                Disposer.register(dialog, it)
            }
            var archiveSizeCell: Cell<*>? = null
            fun detect(item: String) {
                outputPath.text = System.getProperty("user.home") + "/.zig/" + item
                val data = info.firstOrNull { it.first == item } ?: return
                val size = data.second.dist.size
                val sizeMb = size / (1024f * 1024f)
                archiveSizeCell?.comment?.text = "Archive size: %.2fMB".format(sizeMb)
            }
            theList.addItemListener {
                detect(it.item as String)
            }
            val center = panel {
                row("Version:") {
                    cell(theList).resizableColumn().align(AlignX.FILL)
                }
                row("Location:") {
                    cell(outputPath).resizableColumn().align(AlignX.FILL).apply { archiveSizeCell = comment("") }
                }
            }
            detect(info[0].first)
            dialog.centerPanel(center)
            dialog.setTitle("Version Selector")
            dialog.addCancelAction()
            dialog.showAndGet()
            Disposer.dispose(dialog)
        }
    }
}