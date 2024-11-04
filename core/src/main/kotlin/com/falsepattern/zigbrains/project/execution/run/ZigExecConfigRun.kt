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

package com.falsepattern.zigbrains.project.execution.run

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.execution.base.*
import com.falsepattern.zigbrains.shared.cli.coloredCliFlags
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import kotlin.io.path.pathString

class ZigExecConfigRun(project: Project, factory: ConfigurationFactory): ZigExecConfig<ZigExecConfigRun>(project, factory, ZigBrainsBundle.message("exec.type.run.label")) {
    var filePath = FilePathConfigurable("filePath", ZigBrainsBundle.message("exec.option.label.file-path"))
        private set
    var colored = ColoredConfigurable("colored")
        private set
    var optimization = OptimizationConfigurable("optimization")
        private set
    var compilerArgs = ArgsConfigurable("compilerArgs", ZigBrainsBundle.message("exec.option.label.compiler-args"))
        private set
    var exeArgs = ArgsConfigurable("exeArgs", ZigBrainsBundle.message("exec.option.label.exe-args"))
        private set

    override suspend fun buildCommandLineArgs(debug: Boolean): List<String> {
        val result = ArrayList<String>()
        result.add(if (debug) "build-exe" else "run")
        result.addAll(coloredCliFlags(colored.value, debug))
        result.add(filePath.path?.pathString ?: throw ExecutionException(ZigBrainsBundle.message("exception.zig.empty-file-path")))
        if (!debug || optimization.forced) {
            result.addAll(listOf("-O", optimization.level.name))
        }
        result.addAll(compilerArgs.args)
        if (!debug) {
            result.add("--")
            result.addAll(exeArgs.args)
        }
        return result
    }

    override val suggestedName: String
        get() = ZigBrainsBundle.message("configuration.run.suggested-name")

    override fun clone(): ZigExecConfigRun {
        val clone = super.clone()
        clone.filePath = filePath.clone()
        clone.colored = colored.clone()
        clone.compilerArgs = compilerArgs.clone()
        clone.optimization = optimization.clone()
        clone.exeArgs = exeArgs.clone()
        return clone
    }

    override fun getConfigurables(): List<ZigConfigurable<*>> {
        return super.getConfigurables() + listOf(filePath, optimization, colored, compilerArgs, exeArgs)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): ZigProfileState<ZigExecConfigRun> {
        return ZigProfileStateRun(environment, this)
    }
}