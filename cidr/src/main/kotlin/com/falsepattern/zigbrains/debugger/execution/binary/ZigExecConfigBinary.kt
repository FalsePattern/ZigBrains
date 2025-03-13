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

package com.falsepattern.zigbrains.debugger.execution.binary

import com.falsepattern.zigbrains.debugger.ZigDebugBundle
import com.falsepattern.zigbrains.project.execution.base.*
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project

class ZigExecConfigBinary(project: Project, factory: ConfigurationFactory) : ZigExecConfig<ZigExecConfigBinary>(project, factory, ZigDebugBundle.message("exec.type.binary.label")) {
    var exePath = FilePathConfigurable("exePath", ZigDebugBundle.message("exec.option.label.binary.exe-path"))
        private set
    var args = ArgsConfigurable("args", ZigDebugBundle.message("exec.option.label.binary.args"))
        private set

    override val suggestedName: String
        get() = ZigDebugBundle.message("configuration.binary.suggested-name")

    override suspend fun buildCommandLineArgs(debug: Boolean): List<String> {
        return args.argsSplit()
    }

    override fun getConfigurables(): List<ZigConfigurable<*>> {
        return super.getConfigurables() + listOf(exePath, args)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): ZigProfileState<ZigExecConfigBinary> {
        return ZigProfileStateBinary(environment, this)
    }

    override fun clone(): ZigExecConfigBinary {
        val clone = super.clone()
        clone.exePath = exePath.clone()
        clone.args = args.clone()
        return clone
    }
}