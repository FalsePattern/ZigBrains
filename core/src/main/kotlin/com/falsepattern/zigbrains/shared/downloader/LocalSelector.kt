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
import com.falsepattern.zigbrains.shared.coroutine.launchWithEDT
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.sanitizedPathString
import com.falsepattern.zigbrains.shared.sanitizedToNioPath
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.system.OS
import java.awt.Component
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.Icon
import javax.swing.event.DocumentEvent
import kotlin.io.path.isDirectory

abstract class LocalSelector<T>(val component: Component) {
    suspend open fun browse(preSelected: Path? = null): T? {
        return withEDTContext(component.asContextElement()) {
            doBrowseFromDisk(preSelected)
        }
    }

    abstract val windowTitle: String
    abstract val descriptor: FileChooserDescriptor
    protected abstract suspend fun verify(path: Path): VerifyResult
    protected abstract suspend fun resolve(path: Path, name: String?): T?

    @RequiresEdt
    private suspend fun doBrowseFromDisk(preSelected: Path?): T? {
        val dialog = DialogBuilder()
        val name = JBTextField().also { it.columns = 25 }
        val path = textFieldWithBrowseButton(null, descriptor)
        Disposer.register(dialog, path)
        lateinit var errorMessageBox: JBLabel
        suspend fun verifyAndUpdate(path: Path?) {
            val result = path?.let { verify(it) } ?: VerifyResult(
                "",
                false,
                AllIcons.General.Error,
                ZigBrainsBundle.message("settings.shared.local-selector.state.invalid")
            )
            val prevNameDefault = name.emptyText.text.trim() == name.text.trim() || name.text.isBlank()
            name.emptyText.text = result.name ?: ""
            if (prevNameDefault) {
                name.text = name.emptyText.text
            }
            errorMessageBox.icon = result.errorIcon
            errorMessageBox.text = result.errorText
            dialog.setOkActionEnabled(result.allowed)
        }
        val active = AtomicBoolean(false)
        path.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                if (!active.get())
                    return
                zigCoroutineScope.launchWithEDT(ModalityState.current()) {
                    verifyAndUpdate(path.text.sanitizedToNioPath())
                }
            }
        })
        val center = panel {
            row(ZigBrainsBundle.message("settings.shared.local-selector.name.label")) {
                cell(name).resizableColumn().align(AlignX.FILL)
            }
            row(ZigBrainsBundle.message("settings.shared.local-selector.path.label")) {
                cell(path).resizableColumn().align(AlignX.FILL)
            }
            row {
                errorMessageBox = JBLabel()
                cell(errorMessageBox)
            }
        }
        dialog.centerPanel(center)
        dialog.setTitle(windowTitle)
        dialog.addCancelAction()
        dialog.addOkAction().also { it.setText(ZigBrainsBundle.message("settings.shared.local-selector.ok-action")) }
        if (preSelected == null) {
            val chosenFile = FileChooser.chooseFile(descriptor, null, null)
            if (chosenFile != null) {
                verifyAndUpdate(chosenFile.toNioPath())
                path.text = chosenFile.path
            }
        } else {
            verifyAndUpdate(preSelected)
            path.text = preSelected.sanitizedPathString ?: ""
        }
        active.set(true)
        if (!dialog.showAndGet()) {
            active.set(false)
            return null
        }
        active.set(false)
        return path.text.sanitizedToNioPath()?.let { resolve(it, name.text.ifBlank { null }) }
    }

    @JvmRecord
    data class VerifyResult(
        val name: String?,
        val allowed: Boolean,
        val errorIcon: Icon,
        val errorText: String,
    )
}

val homePath: Path? by lazy {
    System.getProperty("user.home")?.sanitizedToNioPath()?.takeIf { it.isDirectory() }
}

val xdgDataHome: Path? by lazy {
    System.getenv("XDG_DATA_HOME")?.sanitizedToNioPath()?.takeIf { it.isDirectory() } ?:
    when(OS.CURRENT) {
        OS.macOS -> homePath?.resolve("Library")
        OS.Windows -> System.getenv("LOCALAPPDATA")?.sanitizedToNioPath()
        else -> homePath?.resolve(Path.of(".local", "share"))
    }?.takeIf { it.isDirectory() }
}