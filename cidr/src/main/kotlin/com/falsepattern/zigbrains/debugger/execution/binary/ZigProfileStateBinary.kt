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
import com.falsepattern.zigbrains.project.execution.base.ZigProfileState
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.shared.sanitizedPathString
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.runners.ExecutionEnvironment

class ZigProfileStateBinary(environment: ExecutionEnvironment, configuration: ZigExecConfigBinary) : ZigProfileState<ZigExecConfigBinary>(environment, configuration) {
    override suspend fun getCommandLine(toolchain: ZigToolchain, debug: Boolean): GeneralCommandLine {
        val cli = GeneralCommandLine()
        val cfg = configuration
        cfg.workingDirectory.path?.let { cli.withWorkDirectory(it.toFile()) }
        cli.withExePath(cfg.exePath.path?.sanitizedPathString ?: throw ExecutionException(ZigDebugBundle.message("exception.missing-exe-path")))
        cli.withCharset(Charsets.UTF_8)
        cli.addParameters(cfg.args.args)
        return cli
    }
}