/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2024 FalsePattern
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

package com.falsepattern.zigbrains.debugger.win

import com.falsepattern.zigbrains.debugger.dap.DAPDebuggerDriverConfiguration
import com.intellij.execution.configurations.GeneralCommandLine
import com.jetbrains.cidr.ArchitectureType
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import org.eclipse.lsp4j.debug.InitializeRequestArguments
import java.nio.file.Path
import kotlin.io.path.pathString

abstract class MSVCDriverConfiguration: DAPDebuggerDriverConfiguration() {
    protected abstract val debuggerExecutable: Path

    override fun createDriver(handler: DebuggerDriver.Handler, arch: ArchitectureType): DebuggerDriver {
        return WinDAPDriver(handler).also { it.initialize(this) }
    }

    override fun createDriverCommandLine(driver: DebuggerDriver, arch: ArchitectureType): GeneralCommandLine {
        val path = debuggerExecutable
        val cli = GeneralCommandLine()
        cli.withExePath(path.pathString)
        cli.addParameters("--interpreter=vscode", "--extconfigdir=%USERPROFILE%\\.cppvsdbg\\extensions")
        cli.withWorkingDirectory(path.parent)
        return cli
    }

    override fun customizeInitializeArguments(initArgs: InitializeRequestArguments) {
        initArgs.pathFormat = "path"
        initArgs.adapterID = "cppvsdbg"
    }
}