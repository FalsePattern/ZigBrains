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
import com.falsepattern.zigbrains.project.zigService
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.AsyncProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.rd.util.toPromise
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.jetbrains.concurrency.Promise

abstract class ZigProgramRunner<ProfileState: ZigProfileState<*>>(protected val executorId: String): AsyncProgramRunner<RunnerSettings>() {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> {
        return environment.project.zigService.cs.async {
            executeAsync(environment, state)
        }.toPromise()
    }

    private suspend inline fun executeAsync(environment: ExecutionEnvironment, state: RunProfileState): RunContentDescriptor? {
        if (state !is ZigProfileState<*>)
            return null

        val state = castProfileState(state) ?: return null

        execute(state, null, environment)
    }

    protected abstract fun castProfileState(state: ZigProfileState<*>): ProfileState?

    abstract suspend fun execute(state: ProfileState, toolchain: ZigToolchain, environment: ExecutionEnvironment)
}