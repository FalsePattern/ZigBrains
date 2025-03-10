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
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainEnvironmentSerializable
import com.intellij.openapi.project.Project
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.nio.file.Path

class ZigCompilerTool(toolchain: AbstractZigToolchain) : ZigTool(toolchain) {
    override val toolName: String
        get() = "zig"

    fun path(): Path {
        return toolchain.pathToExecutable(toolName)
    }

    suspend fun getEnv(project: Project?): ZigToolchainEnvironmentSerializable? {
        val stdout = callWithArgs(toolchain.workingDirectory(project), "env").stdout
        return try {
            envJson.decodeFromString<ZigToolchainEnvironmentSerializable>(stdout)
        } catch (e: SerializationException) {
            null
        }
    }
}

private val envJson = Json {
    ignoreUnknownKeys = true
}