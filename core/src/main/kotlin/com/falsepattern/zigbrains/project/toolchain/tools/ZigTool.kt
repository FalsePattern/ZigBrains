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

package com.falsepattern.zigbrains.project.toolchain.tools

import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain
import com.falsepattern.zigbrains.shared.cli.call
import com.falsepattern.zigbrains.shared.cli.createCommandLineSafe
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.project.Project
import java.nio.file.Path
import kotlin.io.path.isRegularFile

abstract class ZigTool(val toolchain: AbstractZigToolchain) {
    abstract val toolName: String

    suspend fun callWithArgs(workingDirectory: Path?, vararg parameters: String, timeoutMillis: Long = Long.MAX_VALUE, ipcProject: Project? = null): Result<ProcessOutput> {
        val cli = createBaseCommandLine(workingDirectory, *parameters).let { it.getOrElse { return Result.failure(it) } }
        return cli.call(timeoutMillis, ipcProject = ipcProject)
    }

    fun fileValid(): Boolean {
        val exe = toolchain.pathToExecutable(toolName)
        return exe.isRegularFile()
    }

    private suspend fun createBaseCommandLine(
        workingDirectory: Path?,
        vararg parameters: String
    ): Result<GeneralCommandLine> {
        val exe = toolchain.pathToExecutable(toolName)
        return createCommandLineSafe(workingDirectory, exe, *parameters)
            .mapCatching { toolchain.patchCommandLine(it) }
    }
}