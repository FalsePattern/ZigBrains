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

import com.falsepattern.zigbrains.project.execution.base.ZigProfileState
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain
import com.intellij.util.system.CpuArch
import com.jetbrains.cidr.ArchitectureType
import com.jetbrains.cidr.execution.RunParameters
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration

abstract class ZigDebugParametersBase<ProfileState: ZigProfileState<*>>(
    private val driverConfiguration: DebuggerDriverConfiguration,
    protected val toolchain: AbstractZigToolchain,
    protected val profileState: ProfileState
): RunParameters() {
    override fun getDebuggerDriverConfiguration(): DebuggerDriverConfiguration {
        return driverConfiguration
    }

    override fun getArchitectureId(): String {
        return ArchitectureType.forVmCpuArch(CpuArch.CURRENT).id
    }
}