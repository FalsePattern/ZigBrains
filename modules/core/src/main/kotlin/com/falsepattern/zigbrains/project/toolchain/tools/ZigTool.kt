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
import com.intellij.util.io.readLineAsync
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout
import java.nio.file.Path
import java.time.Duration

abstract class ZigTool(val toolchain: AbstractZigToolchain) {
    abstract val toolName: String

    suspend fun callWithArgs(workingDirectory: Path?, vararg parameters: String): ProcessOutput {
        val process = createBaseCommandLine(workingDirectory, *parameters).createProcess()
        val exitCode = process.awaitExit()
        return runInterruptible {
            ProcessOutput(
                process.inputStream.bufferedReader().use { it.readText() },
                process.errorStream.bufferedReader().use { it.readText() },
                exitCode,
                false,
                false
            )
        }
    }

    protected suspend fun createBaseCommandLine(workingDirectory: Path?,
                                                vararg parameters: String): GeneralCommandLine {
        return GeneralCommandLine()
            .withExePath(toolchain.pathToExecutable(toolName).toString())
            .withWorkDirectory(workingDirectory?.toString())
            .withParameters(*parameters)
            .withCharset(Charsets.UTF_8)
    }
}