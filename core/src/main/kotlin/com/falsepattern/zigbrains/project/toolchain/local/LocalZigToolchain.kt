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

package com.falsepattern.zigbrains.project.toolchain.local

import com.falsepattern.zigbrains.direnv.DirenvService
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.toNioPathOrNull
import java.nio.file.Path

@JvmRecord
data class LocalZigToolchain(val location: Path, val std: Path? = null, override val name: String? = null): ZigToolchain {
    override fun workingDirectory(project: Project?): Path? {
        return project?.guessProjectDir()?.toNioPathOrNull()
    }

    override suspend fun patchCommandLine(commandLine: GeneralCommandLine, project: Project?): GeneralCommandLine {
        if (project != null && DirenvService.getStateFor(commandLine, project).isEnabled(project)) {
            commandLine.withEnvironment(DirenvService.getInstance(project).import().env)
        }
        return commandLine
    }

    override fun pathToExecutable(toolName: String, project: Project?): Path {
        val exeName = if (SystemInfo.isWindows) "$toolName.exe" else toolName
        return location.resolve(exeName)
    }

    override fun withName(newName: String?): LocalZigToolchain {
        return this.copy(name = newName)
    }

    companion object {
        @Throws(ExecutionException::class)
        fun ensureLocal(toolchain: ZigToolchain): LocalZigToolchain {
            if (toolchain is LocalZigToolchain) {
                return toolchain
            } else {
                // TODO
                throw ExecutionException("The debugger only supports local zig toolchain")
            }
        }

        suspend fun tryFromPathString(pathStr: String?): LocalZigToolchain? {
            return pathStr?.ifBlank { null }?.toNioPathOrNull()?.let { tryFromPath(it) }
        }

        suspend fun tryFromPath(path: Path): LocalZigToolchain? {
            var tc = LocalZigToolchain(path)
            if (!tc.zig.fileValid()) {
                return null
            }
            val versionStr = tc.zig
                .getEnv(null)
                .getOrNull()
                ?.version
            if (versionStr != null) {
                tc = tc.copy(name = "Zig $versionStr")
            }
            return tc
        }
    }
}