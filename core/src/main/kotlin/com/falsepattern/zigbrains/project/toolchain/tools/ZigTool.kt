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

package com.falsepattern.zigbrains.project.toolchain.tools

import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.util.io.awaitExit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.file.Path

abstract class ZigTool(val toolchain: AbstractZigToolchain) {
    abstract val toolName: String

    suspend fun callWithArgs(workingDirectory: Path?, vararg parameters: String, timeoutMillis: Long = Long.MAX_VALUE): ProcessOutput {
        val cli = createBaseCommandLine(workingDirectory, *parameters)

        val (process, exitCode) = withContext(Dispatchers.IO) {
            val process = cli.createProcess()
            val exit = withTimeoutOrNull(timeoutMillis) {
                process.awaitExit()
            }
            process to exit
        }
        return runInterruptible {
            ProcessOutput(
                process.inputStream.bufferedReader().use { it.readText() },
                process.errorStream.bufferedReader().use { it.readText() },
                exitCode ?: -1,
                exitCode == null,
                false
            )
        }
    }

    private suspend fun createBaseCommandLine(
        workingDirectory: Path?,
        vararg parameters: String
    ): GeneralCommandLine {
        val cli = GeneralCommandLine()
            .withExePath(toolchain.pathToExecutable(toolName).toString())
            .withWorkingDirectory(workingDirectory)
            .withParameters(*parameters)
            .withCharset(Charsets.UTF_8)
        return toolchain.patchCommandLine(cli)
    }
}