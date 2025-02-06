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
import com.falsepattern.zigbrains.project.run.ZigProcessHandler
import com.falsepattern.zigbrains.project.settings.zigProjectSettings
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain
import com.falsepattern.zigbrains.shared.coroutine.runModalOrBlocking
import com.intellij.build.BuildTextConsoleView
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.platform.ide.progress.ModalTaskOwner
import kotlin.io.path.pathString

abstract class ZigProfileState<T: ZigExecConfig<T>> (
    environment: ExecutionEnvironment,
    val configuration: T
): CommandLineState(environment) {

    @Throws(ExecutionException::class)
    override fun startProcess(): ProcessHandler {
        return runModalOrBlocking({ModalTaskOwner.project(environment.project)}, {"ZigProfileState.startProcess"}) {
            startProcessSuspend()
        }
    }

    @Throws(ExecutionException::class)
    suspend fun startProcessSuspend(): ProcessHandler {
        val toolchain = environment.project.zigProjectSettings.state.toolchain ?: throw ExecutionException(ZigBrainsBundle.message("exception.zig-profile-state.start-process.no-toolchain"))
        return ZigProcessHandler(getCommandLine(toolchain, false))
    }

    @Throws(ExecutionException::class)
    open suspend fun getCommandLine(toolchain: AbstractZigToolchain, debug: Boolean): GeneralCommandLine {
        val workingDir = configuration.workingDirectory
        val zigExePath = toolchain.zig.path()

        // TODO remove this check once JetBrains implements colored terminal in the debugger
        // https://youtrack.jetbrains.com/issue/CPP-11622/ANSI-color-codes-not-honored-in-Debug-Run-Configuration-output-window
        val cli = if (configuration.emulateTerminal() && !debug) PtyCommandLine().withConsoleMode(true).withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE) else GeneralCommandLine()
        cli.withExePath(zigExePath.pathString)
        workingDir.path?.let { cli.withWorkDirectory(it.toFile()) }
        cli.withCharset(Charsets.UTF_8)
        cli.addParameters(configuration.buildCommandLineArgs(debug))
        return configuration.patchCommandLine(cli)
    }
}

@Throws(ExecutionException::class)
fun executeCommandLine(commandLine: GeneralCommandLine, environment: ExecutionEnvironment): DefaultExecutionResult {
    val handler = startProcess(commandLine)
    val console = BuildTextConsoleView(environment.project, false, emptyList())
    console.attachToProcess(handler)
    return DefaultExecutionResult(console, handler)
}

@Throws(ExecutionException::class)
fun startProcess(commandLine: GeneralCommandLine): ProcessHandler {
    val handler = ZigProcessHandler(commandLine)
    ProcessTerminatedListener.attach(handler)
    return handler
}