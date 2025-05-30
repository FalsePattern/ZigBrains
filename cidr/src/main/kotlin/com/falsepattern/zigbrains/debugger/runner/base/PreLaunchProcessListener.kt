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

package com.falsepattern.zigbrains.debugger.runner.base

import com.falsepattern.zigbrains.project.run.ZigProcessHandler
import com.falsepattern.zigbrains.shared.cli.startIPCAwareProcess
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.withProgressText
import com.intellij.util.io.awaitExit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

class PreLaunchProcessListener(val console: ConsoleView) : ProcessListener {
    var isBuildFailed: Boolean = false
        private set
    var processHandler: ZigProcessHandler.IPCAware? = null
        private set

    @Throws(ExecutionException::class)
    suspend fun executeCommandLineWithHook(project: Project, commandLine: GeneralCommandLine): Boolean {
        return withProgressText(commandLine.commandLineString) {
            val processHandler = commandLine.startIPCAwareProcess(project)
            this@PreLaunchProcessListener.processHandler = processHandler
            hook(processHandler)
            processHandler.startNotify()
            withContext(Dispatchers.IO) {
                processHandler.process.awaitExit()
            }
            runInterruptible {
                processHandler.waitFor()
            }
            return@withProgressText isBuildFailed
        }
    }

    fun hook(handler: ProcessHandler) {
        console.attachToProcess(handler)
        handler.addProcessListener(this)
    }

    override fun processTerminated(event: ProcessEvent) {
        if (event.exitCode != 0) {
            isBuildFailed = true
        } else {
            isBuildFailed = false
            console.print("Build Successful. Starting debug session. \n", ConsoleViewContentType.NORMAL_OUTPUT)
        }
    }
}
