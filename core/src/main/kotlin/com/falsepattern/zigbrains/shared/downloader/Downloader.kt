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

package com.falsepattern.zigbrains.shared.downloader

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.shared.coroutine.asContextElement
import com.falsepattern.zigbrains.shared.coroutine.runInterruptibleEDT
import com.intellij.icons.AllIcons
import com.intellij.openapi.observable.util.whenFocusGained
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
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
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.awt.Component
import java.nio.file.Path
import java.util.*
import javax.swing.DefaultComboBoxModel
import javax.swing.JList
import javax.swing.event.DocumentEvent
import kotlin.io.path.pathString

abstract class Downloader<T, V: VersionInfo>(val component: Component) {
    suspend fun download(): T? {
        val info = withModalProgress(
            ModalTaskOwner.component(component),
            versionInfoFetchTitle,
            TaskCancellation.cancellable()
        ) {
            downloadVersionList()
        }
        val selector = localSelector()
        val (downloadPath, version) = runInterruptibleEDT(component.asContextElement()) {
            selectVersion(info, selector)
        } ?: return null
        withModalProgress(
            ModalTaskOwner.component(component),
            downloadProgressTitle(version),
            TaskCancellation.cancellable()
        ) {
            version.downloadAndUnpack(downloadPath)
        }
        return selector.browse(downloadPath)
    }

    protected abstract val windowTitle: String
    protected abstract val versionInfoFetchTitle: @NlsContexts.ProgressTitle String
    protected abstract fun downloadProgressTitle(version: V): @NlsContexts.ProgressTitle String
    protected abstract fun localSelector(): LocalSelector<T>
    protected abstract suspend fun downloadVersionList(): List<V>
    protected abstract fun getSuggestedPath(): Path?

    @RequiresEdt
    private fun selectVersion(info: List<V>, selector: LocalSelector<T>): Pair<Path, V>? {
        val dialog = DialogBuilder()
        val theList = ComboBox(DefaultComboBoxModel(Vector(info)))
        theList.renderer = object: ColoredListCellRenderer<V>() {
            override fun customizeCellRenderer(
                list: JList<out V>,
                value: V?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                value?.let { append(it.version.rawVersion) }
            }
        }
        val outputPath = textFieldWithBrowseButton(null, selector.descriptor)
        Disposer.register(dialog, outputPath)
        outputPath.textField.columns = 50

        lateinit var errorMessageBox: JBLabel
        fun onChanged() {
            val path = outputPath.text.ifBlank { null }?.toNioPathOrNull()
            val state = DirectoryState.Companion.determine(path)
            if (state.isValid()) {
                errorMessageBox.icon = AllIcons.General.Information
                dialog.setOkActionEnabled(true)
            } else {
                errorMessageBox.icon = AllIcons.General.Error
                dialog.setOkActionEnabled(false)
            }
            errorMessageBox.text = ZigBrainsBundle.message(when(state) {
                DirectoryState.Invalid -> "settings.shared.downloader.state.invalid"
                DirectoryState.NotAbsolute -> "settings.shared.downloader.state.not-absolute"
                DirectoryState.NotDirectory -> "settings.shared.downloader.state.not-directory"
                DirectoryState.NotEmpty -> "settings.shared.downloader.state.not-empty"
                DirectoryState.CreateNew -> "settings.shared.downloader.state.create-new"
                DirectoryState.Ok -> "settings.shared.downloader.state.ok"
            })
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
        fun detect(item: V) {
            outputPath.text = getSuggestedPath()?.resolve(item.version.rawVersion)?.pathString ?: ""
            val size = item.dist.size
            val sizeMb = size / (1024f * 1024f)
            archiveSizeCell?.comment?.text = ZigBrainsBundle.message("settings.shared.downloader.archive-size.text", "%.2fMB".format(sizeMb))
        }
        theList.addItemListener {
            @Suppress("UNCHECKED_CAST")
            detect(it.item as V)
        }
        val center = panel {
            row(ZigBrainsBundle.message("settings.shared.downloader.version.label")) {
                cell(theList).resizableColumn().align(AlignX.FILL)
            }
            row(ZigBrainsBundle.message("settings.shared.downloader.location.label")) {
                cell(outputPath).resizableColumn().align(AlignX.FILL).apply { archiveSizeCell = comment("") }
            }
            row {
                errorMessageBox = JBLabel()
                cell(errorMessageBox)
            }
        }
        detect(info[0])
        dialog.centerPanel(center)
        dialog.setTitle(windowTitle)
        dialog.addCancelAction()
        dialog.addOkAction().also { it.setText(ZigBrainsBundle.message("settings.shared.downloader.ok-action")) }
        if (!dialog.showAndGet()) {
            return null
        }
        val path = outputPath.text.ifBlank { null }?.toNioPathOrNull()
                   ?: return null
        if (!DirectoryState.Companion.determine(path).isValid()) {
            return null
        }
        val version = theList.item ?: return null

        return path to version
    }
}