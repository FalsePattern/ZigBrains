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

import com.falsepattern.zigbrains.debugbridge.ZigDebuggerDriverConfigurationProviderBase
import com.falsepattern.zigbrains.debugger.ZigLocalDebugProcess
import com.falsepattern.zigbrains.debugger.runner.build.ZigDebugRunnerBuild
import com.falsepattern.zigbrains.project.execution.base.ZigProfileState
import com.falsepattern.zigbrains.project.run.ZigProcessHandler
import com.falsepattern.zigbrains.project.run.ZigProgramRunner
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.shared.coroutine.runInterruptibleEDT
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.RunContentBuilder
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.guessProjectDir
import com.intellij.platform.util.progress.reportProgress
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration

abstract class ZigDebugRunnerBase<ProfileState : ZigProfileState<*>> : ZigProgramRunner<ProfileState>(DefaultDebugExecutor.EXECUTOR_ID) {
    @Throws(ExecutionException::class)
    override suspend fun execute(
        state: ProfileState,
        toolchain: ZigToolchain,
        environment: ExecutionEnvironment
    ): RunContentDescriptor? {
        val project = environment.project
        val driverProviders = ZigDebuggerDriverConfigurationProviderBase.EXTENSION_POINT_NAME.extensionList
        for (provider in driverProviders) {
            val driver = provider.getDebuggerConfiguration(project, isElevated = false, emulateTerminal = true, DebuggerDriverConfiguration::class.java) ?: continue
            return executeWithDriver(state, toolchain, environment, driver) ?: continue
        }
        return null
    }

    @Throws(ExecutionException::class)
    private suspend fun executeWithDriver(
        state: ProfileState,
        toolchain: ZigToolchain,
        environment: ExecutionEnvironment,
        debuggerDriver: DebuggerDriverConfiguration
    ): RunContentDescriptor? {
        return reportProgress { reporter ->
            val runParameters = getDebugParameters(state, debuggerDriver, toolchain)
            val console = state.consoleBuilder.console
            if (runParameters is PreLaunchAware) {
                val listener = PreLaunchProcessListener(console)
                var failed: Boolean
                var handler: ZigProcessHandler.IPCAware
                try {
                    reporter.indeterminateStep {
                        runParameters.preLaunch(listener)
                    }
                    handler = listener.processHandler ?: throw ExecutionException("Unknown error while trying to compile executable.")
                    failed = listener.isBuildFailed
                } catch (e: ExecutionException) {
                    handler = listener.processHandler ?: throw e
                    failed = true
                    console.print("\n", ConsoleViewContentType.ERROR_OUTPUT)
                    e.message?.let { listener.console.print(it, ConsoleViewContentType.ERROR_OUTPUT) }
                    if (this !is ZigDebugRunnerBuild && environment.project.guessProjectDir()?.children?.any { it.name == "build.zig" } == true) {
                        console.print("\n  Warning: build.zig file detected in project.\n  Did you want to use a Zig Build task instead?", ConsoleViewContentType.ERROR_OUTPUT)
                    }
                }
                if (failed) {
                    val executionResult = DefaultExecutionResult(console, handler.unwrap())
                    return@reportProgress withEDTContext(ModalityState.any()) {
                        val runContentBuilder = RunContentBuilder(executionResult, environment)
                        runContentBuilder.showRunContent(null)
                    }
                }
            }
            return@reportProgress runInterruptibleEDT(ModalityState.any()) {
                val debuggerManager = XDebuggerManager.getInstance(environment.project)
                debuggerManager.startSession(environment, object: XDebugProcessStarter() {
                    override fun start(session: XDebugSession): XDebugProcess {
                        val project = session.project
                        val textConsoleBuilder = state.consoleBuilder
                        val debugProcess = ZigLocalDebugProcess(runParameters, session, textConsoleBuilder)
                        ProcessTerminatedListener.attach(debugProcess.processHandler, project)
                        debugProcess.start()
                        return debugProcess
                    }
                }).runContentDescriptor
            }
        }
    }

    @Throws(ExecutionException::class)
    protected abstract fun getDebugParameters(
        state: ProfileState,
        debuggerDriver: DebuggerDriverConfiguration,
        toolchain: ZigToolchain
    ): ZigDebugParametersBase<ProfileState>

    private class SharedConsoleBuilder(private val console: ConsoleView) : TextConsoleBuilder() {
        override fun getConsole(): ConsoleView {
            return console
        }

        override fun addFilter(filter: Filter) {
        }

        override fun setViewer(isViewer: Boolean) {
        }

    }
}