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

package com.falsepattern.zigbrains.debugger.settings

import com.falsepattern.zigbrains.debugger.ZigDebugBundle
import com.falsepattern.zigbrains.debugger.toolchain.DebuggerAvailability
import com.falsepattern.zigbrains.debugger.toolchain.DebuggerKind
import com.falsepattern.zigbrains.debugger.toolchain.ZigDebuggerToolchainService
import com.falsepattern.zigbrains.debugger.toolchain.zigDebuggerToolchainService
import com.falsepattern.zigbrains.shared.coroutine.launchWithEDT
import com.falsepattern.zigbrains.shared.coroutine.runModalOrBlocking
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.observable.util.whenItemSelected
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.SystemInfo
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.DEFAULT_COMMENT_WIDTH
import com.intellij.ui.dsl.builder.Panel
import com.intellij.util.concurrency.annotations.RequiresEdt
import javax.swing.ComboBoxModel
import javax.swing.DefaultComboBoxModel
import javax.swing.JEditorPane

class ZigDebuggerToolchainConfigurableUi : ZigDebuggerUiComponent {
    private val debuggerKindComboBox = ComboBox(
        runModalOrBlocking({ ModalTaskOwner.guess() }, { "ZigDebuggerToolchainConfigurableUi" }) {
            createDebuggerKindComboBoxModel()
        }
    )
    private val downloadAutomaticallyCheckBox = JBCheckBox(
        ZigDebugBundle.message("settings.debugger.toolchain.download.debugger.automatically.checkbox"),
        ZigDebuggerSettings.instance.downloadAutomatically
    )

    private val useClion = JBCheckBox(
        ZigDebugBundle.message("settings.debugger.toolchain.use.clion.toolchains"),
        ZigDebuggerSettings.instance.useClion
    )

    private var comment: JEditorPane? = null

    private val currentDebuggerKind get() = debuggerKindComboBox.item

    override fun reset(settings: ZigDebuggerSettings) {
        debuggerKindComboBox.item = settings.debuggerKind
        downloadAutomaticallyCheckBox.isSelected = settings.downloadAutomatically
        useClion.isSelected = settings.useClion
    }

    override fun isModified(settings: ZigDebuggerSettings): Boolean {
        return settings.debuggerKind != debuggerKindComboBox.item ||
                settings.downloadAutomatically != downloadAutomaticallyCheckBox.isSelected ||
                settings.useClion != useClion.isSelected
    }

    override fun apply(settings: ZigDebuggerSettings) {
        settings.debuggerKind = debuggerKindComboBox.item
        settings.downloadAutomatically = downloadAutomaticallyCheckBox.isSelected
        settings.useClion = useClion.isSelected
    }

    override fun buildUi(panel: Panel): Unit = with(panel) {
        row(ZigDebugBundle.message("settings.debugger.toolchain.debugger.label")) {
            comment = cell(debuggerKindComboBox)
                .comment("", DEFAULT_COMMENT_WIDTH) {
                    zigCoroutineScope.launchWithEDT {
                        withModalProgress(ModalTaskOwner.component(debuggerKindComboBox), "Downloading debugger", TaskCancellation.cancellable()) {
                            downloadDebugger()
                        }
                    }
                }
                .applyToComponent {
                    whenItemSelected(null) {
                        zigCoroutineScope.launchWithEDT {
                            this@ZigDebuggerToolchainConfigurableUi.update()
                        }
                    }
                }
                .comment
        }
        row {
            cell(downloadAutomaticallyCheckBox)
        }
        if (PluginManager.isPluginInstalled(PluginId.getId("com.intellij.modules.clion")) && !SystemInfo.isWindows) {
            row {
                cell(useClion)
            }
        }
        zigCoroutineScope.launchWithEDT {
            update()
        }
    }

    override fun dispose() {

    }

    @RequiresEdt
    private suspend fun downloadDebugger() {
        val result = zigDebuggerToolchainService.downloadDebugger(null, currentDebuggerKind)
        if (result is ZigDebuggerToolchainService.DownloadResult.Ok) {
            update()
        }
    }

    @RequiresEdt
    private suspend fun update() {
        val availability = zigDebuggerToolchainService.debuggerAvailability(currentDebuggerKind)
        val text = when (availability) {
            is DebuggerAvailability.NeedToDownload -> ZigDebugBundle.message("settings.debugger.toolchain.download.comment")
            is DebuggerAvailability.NeedToUpdate -> ZigDebugBundle.message("settings.debugger.toolchain.update.comment")
            else -> null
        }
        comment?.let {
            it.text = text
            it.isVisible = text != null
        }
    }

    companion object {
        private suspend fun createDebuggerKindComboBoxModel(): ComboBoxModel<DebuggerKind> {
            val toolchainService = zigDebuggerToolchainService
            val availableKinds = DebuggerKind.entries.filter { toolchainService.debuggerAvailability(it) !is DebuggerAvailability.Unavailable }
            return DefaultComboBoxModel(availableKinds.toTypedArray()).also { it.selectedItem = ZigDebuggerSettings.instance.debuggerKind }
        }

    }
}