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

import com.falsepattern.zigbrains.project.toolchain.local.LocalZigToolchain
import com.falsepattern.zigbrains.shared.sanitizedToNioPath
import com.intellij.openapi.project.Project
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path

@JvmRecord
@Serializable
data class ZigToolchainEnvironmentSerializable(
    @SerialName("zig_exe") val zigExecutable: String,
    @SerialName("std_dir") val stdDirectory: String,
    @SerialName("global_cache_dir") val globalCacheDirectory: String,
    @SerialName("lib_dir") val libDirectory: String,
    @SerialName("version") val version: String,
    @SerialName("target") val target: String
) {
    fun stdPath(toolchain: LocalZigToolchain, project: Project?): Path? {
        val path = stdDirectory.sanitizedToNioPath() ?: return null
        if (path.isAbsolute)
            return path

        val resolvedPath = toolchain.workingDirectory(project)?.resolve(path) ?: return null
        if (resolvedPath.isAbsolute)
            return resolvedPath

        return null
    }
}