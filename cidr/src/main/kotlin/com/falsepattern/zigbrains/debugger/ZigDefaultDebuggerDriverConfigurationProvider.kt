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

package com.falsepattern.zigbrains.debugger

import com.falsepattern.zigbrains.debugger.settings.ZigDebuggerSettings
import com.falsepattern.zigbrains.debugger.toolchain.*
import com.falsepattern.zigbrains.debugger.win.MSVCDriverConfiguration
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.sanitizedPathString
import com.falsepattern.zigbrains.zig.ZigLanguage
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DoNotAskOption
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.jetbrains.cidr.ArchitectureType
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import com.jetbrains.cidr.execution.debugger.backend.gdb.GDBDriverConfiguration
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration
import java.io.File

class ZigDefaultDebuggerDriverConfigurationProvider: ZigDebuggerDriverConfigurationProvider() {
    override suspend fun getDebuggerConfiguration(project: Project, isElevated: Boolean, emulateTerminal: Boolean): DebuggerDriverConfiguration? {
        val settings = ZigDebuggerSettings.instance
        val service = zigDebuggerToolchainService
        val kind = settings.debuggerKind
        if (!availabilityCheck(project, kind))
            return null

        return when(val availability = service.debuggerAvailability(kind)) {
            DebuggerAvailability.Bundled -> when(kind) {
                DebuggerKind.LLDB -> ZigLLDBDriverConfiguration(isElevated, emulateTerminal)
                DebuggerKind.GDB -> ZigGDBDriverConfiguration(isElevated, emulateTerminal)
                DebuggerKind.MSVC -> throw AssertionError("MSVC is never bundled")
            }
            is DebuggerAvailability.Binaries -> when(val binary = availability.binaries) {
                is LLDBBinaries -> ZigCustomBinariesLLDBDriverConfiguration(binary, isElevated, emulateTerminal)
                is GDBBinaries -> ZigCustomBinariesGDBDriverConfiguration(binary, isElevated, emulateTerminal)
                is MSVCBinaries -> ZigMSVCDriverConfiguration(binary, isElevated, emulateTerminal)
                else -> throw AssertionError("Unreachable")
            }
            DebuggerAvailability.Unavailable,
            DebuggerAvailability.NeedToDownload,
            DebuggerAvailability.NeedToUpdate -> throw AssertionError("Unreachable")
        }
    }
}

private suspend fun availabilityCheck(project: Project, kind: DebuggerKind): Boolean {
    val service = zigDebuggerToolchainService
    val availability = service.debuggerAvailability(kind)
    val (message, action) = when(availability) {
        DebuggerAvailability.Unavailable -> return false
        DebuggerAvailability.NeedToDownload ->
            ZigDebugBundle.message("debugger.run.unavailable.reason.download") to ZigDebugBundle.message("debugger.run.unavailable.reason.download.button")
        DebuggerAvailability.NeedToUpdate ->
            ZigDebugBundle.message("debugger.run.unavailable.reason.update") to ZigDebugBundle.message("debugger.run.unavailable.reason.update.button")
        DebuggerAvailability.Bundled,
            is DebuggerAvailability.Binaries -> return true
    }

    val downloadDebugger = if (!ZigDebuggerSettings.instance.downloadAutomatically) {
        showDialog(project, message, action)
    } else {
        true
    }

    if (downloadDebugger) {
        val result = withEDTContext(ModalityState.any()) {
            service.downloadDebugger(project, kind)
        }
        if (result is ZigDebuggerToolchainService.DownloadResult.Ok) {
            return true
        }
    }
    return false
}

private suspend fun showDialog(project: Project, message: String, action: String): Boolean {
    val doNotAsk = object: DoNotAskOption.Adapter() {
        override fun rememberChoice(isSelected: Boolean, exitCode: Int) {
            if (exitCode == Messages.OK) {
                ZigDebuggerSettings.instance.downloadAutomatically = isSelected
            }
        }
    }

    return withEDTContext(ModalityState.any()) {
        MessageDialogBuilder
            .okCancel(ZigDebugBundle.message("debugger.run.unavailable"), message)
            .yesText(action)
            .icon(Messages.getErrorIcon())
            .doNotAsk(doNotAsk)
            .ask(project)
    }
}

private open class ZigLLDBDriverConfiguration(private val isElevated: Boolean, private val emulateTerminal: Boolean): LLDBDriverConfiguration() {
    override fun getDriverName() = "Zig LLDB"
    override fun isElevated() = isElevated
    override fun emulateTerminal() = emulateTerminal
}
private class ZigCustomBinariesLLDBDriverConfiguration(
    private val binaries: LLDBBinaries,
    isElevated: Boolean,
    emulateTerminal: Boolean
) : ZigLLDBDriverConfiguration(isElevated, emulateTerminal) {
    override fun useSTLRenderers() = false
    override fun getLLDBFrameworkFile(architectureType: ArchitectureType): File = binaries.frameworkFile.toFile()
    override fun getLLDBFrontendFile(architectureType: ArchitectureType): File = binaries.frontendFile.toFile()
}

private open class ZigGDBDriverConfiguration(private val isElevated: Boolean, private val emulateTerminal: Boolean): GDBDriverConfiguration() {
    override fun getDriverName() = "Zig GDB"
    override fun isAttachSupported() = false
    override fun isElevated() = isElevated
    override fun emulateTerminal() = emulateTerminal
}
private class ZigCustomBinariesGDBDriverConfiguration(
    private val binaries: GDBBinaries,
    isElevated: Boolean,
    emulateTerminal: Boolean
) : ZigGDBDriverConfiguration(isElevated, emulateTerminal) {
    override fun getGDBExecutablePath() = binaries.gdbFile.sanitizedPathString!!
}

private class ZigMSVCDriverConfiguration(
    private val binaries: MSVCBinaries,
    private val isElevated: Boolean,
    private val emulateTerminal: Boolean
): MSVCDriverConfiguration() {
    override val debuggerExecutable get() = binaries.msvcFile
    override fun getDriverName() = "Zig MSVC"
    override fun getConsoleLanguage() = ZigLanguage
    override fun isElevated() = isElevated
    override fun emulateTerminal() = emulateTerminal
}