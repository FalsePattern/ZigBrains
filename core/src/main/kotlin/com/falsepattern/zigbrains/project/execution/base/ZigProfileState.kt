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

package com.falsepattern.zigbrains.project.execution.base

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.execution.ZigConsoleBuilder
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainService
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.shared.cli.startIPCAwareProcess
import com.falsepattern.zigbrains.shared.coroutine.runModalOrBlocking
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.util.progress.reportProgress
import com.intellij.platform.util.progress.reportRawProgress
import kotlin.io.path.pathString

abstract class ZigProfileState<T: ZigExecConfig<T>> (
    environment: ExecutionEnvironment,
    val configuration: T
): CommandLineState(environment) {

    init {
        consoleBuilder = ZigConsoleBuilder(environment.project, true)
    }

    @Throws(ExecutionException::class)
    override fun startProcess(): ProcessHandler {
        return runModalOrBlocking({ModalTaskOwner.project(environment.project)}, {"ZigProfileState.startProcess"}) {
            startProcessSuspend()
        }
    }

    @Throws(ExecutionException::class)
    suspend fun startProcessSuspend(): ProcessHandler {
        val toolchain = ZigToolchainService.getInstance(environment.project).toolchain ?: throw ExecutionException(ZigBrainsBundle.message("exception.zig-profile-state.start-process.no-toolchain"))
        return getCommandLine(toolchain, false).startIPCAwareProcess(environment.project, emulateTerminal = true)
    }

    @Throws(ExecutionException::class)
    open suspend fun getCommandLine(toolchain: ZigToolchain, debug: Boolean): GeneralCommandLine {
        val workingDir = configuration.workingDirectory
        val zigExePath = toolchain.zig.path()

        val cli = PtyCommandLine().withConsoleMode(false)
        cli.withExePath(zigExePath.pathString)
        workingDir.path?.let { cli.withWorkingDirectory(it) }
        cli.withCharset(Charsets.UTF_8)
        cli.addParameters(configuration.buildCommandLineArgs(debug))
        return configuration.patchCommandLine(cli)
    }
}