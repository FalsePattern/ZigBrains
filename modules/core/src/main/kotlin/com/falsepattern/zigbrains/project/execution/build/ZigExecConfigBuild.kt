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

package com.falsepattern.zigbrains.project.execution.build

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.execution.base.*
import com.falsepattern.zigbrains.shared.ZBFeatures
import com.falsepattern.zigbrains.shared.cli.coloredCliFlags
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project

class ZigExecConfigBuild(project: Project, factory: ConfigurationFactory): ZigExecConfig<ZigExecConfigBuild>(project, factory, "Zig Run") {
    var buildSteps = ArgsConfigurable("buildSteps", ZigBrainsBundle.message("exec.option.label.build.steps"))
        private set
    var extraArgs = ArgsConfigurable("compilerArgs", ZigBrainsBundle.message("exec.option.label.build.args"))
        private set
    var colored = ColoredConfigurable("colored")
        private set
    var exePath = FilePathConfigurable("exePath", ZigBrainsBundle.message("exec.option.label.build.exe-path-debug"))
        private set
    var exeArgs = ArgsConfigurable("exeArgs", ZigBrainsBundle.message("exec.option.label.build.exe-args-debug"))
        private set

    override suspend fun buildCommandLineArgs(debug: Boolean): List<String> {
        val result = ArrayList<String>()
        result.add("build")
        var steps = buildSteps.args
        if (debug) {
            val truncatedSteps = ArrayList<String>()
            for (step in steps) {
                if (step == "run")
                    continue

                if (step == "test")
                    throw ExecutionException(ZigBrainsBundle.message("exception.zig-build.debug.test-not-supported"))

                truncatedSteps.add(step)
            }
        }
        result.addAll(steps)
        result.addAll(coloredCliFlags(colored.value, debug))
        result.addAll(extraArgs.args)
        return result
    }

    override val suggestedName: String
        get() = ZigBrainsBundle.message("configuration.build.suggested-name")

    override fun clone(): ZigExecConfigBuild {
        val clone = super.clone()
        clone.buildSteps = buildSteps.clone()
        clone.exeArgs = exeArgs.clone()
        clone.colored = colored.clone()
        clone.exePath = exePath.clone()
        clone.exeArgs = exeArgs.clone()
        return clone
    }

    override fun getConfigurables(): List<ZigConfigurable<*>> {
        val baseCfg = super.getConfigurables() + listOf(buildSteps, extraArgs, colored)
        if (ZBFeatures.debug()) {
            return baseCfg + listOf(exePath, exeArgs)
        } else {
            return baseCfg
        }
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): ZigProfileState<ZigExecConfigBuild> {
        return ZigProfileStateBuild(environment, this)
    }
}