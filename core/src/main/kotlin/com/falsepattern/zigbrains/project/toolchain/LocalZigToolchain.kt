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

package com.falsepattern.zigbrains.project.toolchain

import com.falsepattern.zigbrains.direnv.DirenvCmd
import com.falsepattern.zigbrains.project.settings.zigProjectSettings
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.toNioPathOrNull
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.pathString

data class LocalZigToolchain(val location: Path, val std: Path? = null, val name: String? = null): AbstractZigToolchain() {
    override fun workingDirectory(project: Project?): Path? {
        return project?.guessProjectDir()?.toNioPathOrNull()
    }

    override suspend fun patchCommandLine(commandLine: GeneralCommandLine, project: Project?): GeneralCommandLine {
        if (project != null && (commandLine.getUserData(DIRENV_KEY) ?: project.zigProjectSettings.state.direnv)) {
            commandLine.withEnvironment(DirenvCmd.importDirenv(project).env)
        }
        return commandLine
    }

    override fun pathToExecutable(toolName: String, project: Project?): Path {
        val exeName = if (SystemInfo.isWindows) "$toolName.exe" else toolName
        return location.resolve(exeName)
    }

    companion object {
        val DIRENV_KEY = KeyWithDefaultValue.create<Boolean>("ZIG_LOCAL_DIRENV")

        @Throws(ExecutionException::class)
        fun ensureLocal(toolchain: AbstractZigToolchain): LocalZigToolchain {
            if (toolchain is LocalZigToolchain) {
                return toolchain
            } else {
                // TODO
                throw ExecutionException("The debugger only supports local zig toolchain")
            }
        }

        fun tryFromPathString(pathStr: String): LocalZigToolchain? {
            return pathStr.toNioPathOrNull()?.let(::tryFromPath)
        }

        fun tryFromPath(path: Path): LocalZigToolchain? {
            val tc = LocalZigToolchain(path)
            if (!tc.zig.fileValid()) {
                return null
            }
            return tc
        }
    }
}