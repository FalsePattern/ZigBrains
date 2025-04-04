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

package com.falsepattern.zigbrains.debugger.runner.run

import com.falsepattern.zigbrains.debugger.runner.base.ZigDebugEmitBinaryInstaller
import com.falsepattern.zigbrains.debugger.runner.base.ZigDebugParametersEmitBinaryBase
import com.falsepattern.zigbrains.project.execution.run.ZigProfileStateRun
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.jetbrains.cidr.execution.Installer
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration

class ZigDebugParametersRun(driverConfiguration: DebuggerDriverConfiguration, toolchain: ZigToolchain, profileState: ZigProfileStateRun) :
    ZigDebugParametersEmitBinaryBase<ZigProfileStateRun>(driverConfiguration, toolchain, profileState) {
    override fun getInstaller(): Installer {
        return ZigDebugEmitBinaryInstaller(profileState, toolchain, executableFile, profileState.configuration.exeArgs.argsSplit())
    }
}