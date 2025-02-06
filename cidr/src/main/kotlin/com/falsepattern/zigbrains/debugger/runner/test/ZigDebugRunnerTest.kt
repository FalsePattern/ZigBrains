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

package com.falsepattern.zigbrains.debugger.runner.test

import com.falsepattern.zigbrains.debugger.runner.base.ZigDebugParametersBase
import com.falsepattern.zigbrains.debugger.runner.base.ZigDebugRunnerBase
import com.falsepattern.zigbrains.project.execution.base.ZigProfileState
import com.falsepattern.zigbrains.project.execution.test.ZigExecConfigTest
import com.falsepattern.zigbrains.project.execution.test.ZigProfileStateTest
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain
import com.falsepattern.zigbrains.project.toolchain.LocalZigToolchain
import com.intellij.execution.configurations.RunProfile
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration

class ZigDebugRunnerTest: ZigDebugRunnerBase<ZigProfileStateTest>() {
    override fun getDebugParameters(
        state: ZigProfileStateTest,
        debuggerDriver: DebuggerDriverConfiguration,
        toolchain: AbstractZigToolchain
    ): ZigDebugParametersBase<ZigProfileStateTest> {
        return ZigDebugParametersTest(debuggerDriver, LocalZigToolchain.ensureLocal(toolchain), state)
    }

    override fun castProfileState(state: ZigProfileState<*>): ZigProfileStateTest? {
        return state as? ZigProfileStateTest
    }

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return this.executorId == executorId && profile is ZigExecConfigTest
    }

    override fun getRunnerId(): String {
        return "ZigDebugRunnerTest"
    }
}