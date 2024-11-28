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

package com.falsepattern.zigbrains.project.run

import com.falsepattern.zigbrains.project.execution.base.ZigProfileState
import com.falsepattern.zigbrains.project.settings.zigProjectSettings
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.AsyncProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.rd.util.toPromise
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.platform.util.progress.reportProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.jetbrains.concurrency.Promise

abstract class ZigProgramRunner<ProfileState : ZigProfileState<*>>(protected val executorId: String) : AsyncProgramRunner<RunnerSettings>() {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> {
        FileDocumentManager.getInstance().saveAllDocuments()
        return environment.project.zigCoroutineScope.async {
            withModalProgress(ModalTaskOwner.project(environment.project), "Starting zig program...", TaskCancellation.cancellable()) {
                executeAsync(environment, state)
            }
        }.toPromise()
    }

    private suspend inline fun executeAsync(environment: ExecutionEnvironment, baseState: RunProfileState): RunContentDescriptor? {
        if (baseState !is ZigProfileState<*>)
            return null

        val state = castProfileState(baseState) ?: return null

        val toolchain = environment.project.zigProjectSettings.state.toolchain ?: run {
            Notification(
                "zigbrains",
                "Zig project toolchain not set, cannot execute program! Please configure it in [Settings | Languages & Frameworks | Zig]",
                NotificationType.ERROR
            ).notify(environment.project)
            return null
        }

        return reportProgress { reporter ->
            reporter.indeterminateStep {
                execute(state, toolchain, environment)
            }
        }

    }

    protected abstract fun castProfileState(state: ZigProfileState<*>): ProfileState?

    @Throws(ExecutionException::class)
    abstract suspend fun execute(state: ProfileState, toolchain: AbstractZigToolchain, environment: ExecutionEnvironment): RunContentDescriptor?
}