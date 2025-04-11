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

package com.falsepattern.zigbrains.project.run

import com.falsepattern.zigbrains.project.execution.base.ZigExecConfig
import com.falsepattern.zigbrains.project.execution.base.ZigProfileState
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.RunContentBuilder
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.blockingContext

class ZigRegularRunner: ZigProgramRunner<ZigProfileState<*>>(DefaultRunExecutor.EXECUTOR_ID) {
    override suspend fun execute(state: ZigProfileState<*>, toolchain: ZigToolchain, environment: ExecutionEnvironment): RunContentDescriptor? {
        val exec = blockingContext {
            state.execute(environment.executor, this)
        }
        return withEDTContext(ModalityState.any()) {
            val runContentBuilder = RunContentBuilder(exec, environment)
            runContentBuilder.showRunContent(null)
        }
    }

    override fun castProfileState(state: ZigProfileState<*>): ZigProfileState<*>? {
        return state
    }

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return this.executorId == executorId && profile is ZigExecConfig<*>
    }

    override fun getRunnerId(): String {
        return "ZigRegularRunner"
    }
}