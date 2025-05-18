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

package com.falsepattern.zigbrains.direnv

import com.falsepattern.zigbrains.shared.sanitizedToNioPath
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.EnvironmentUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.jetbrains.annotations.NonNls
import java.io.File
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile

@JvmRecord
data class Env(val env: Map<String, String>) {
    private val path get() = getVariable("PATH")?.split(File.pathSeparatorChar)

    private fun getVariable(name: @NonNls String) =
        env.getOrElse(name) { EnvironmentUtil.getValue(name) }

    fun findAllExecutablesOnPATH(exe: @NonNls String) = flow {
        val exeName = if (SystemInfo.isWindows) "$exe.exe" else exe
        val paths = path ?: return@flow
        for (dir in paths) {
            val path = dir.sanitizedToNioPath()?.absolute() ?: continue
            if (!path.toFile().exists() || !path.isDirectory())
                continue
            val exePath = path.resolve(exeName).absolute()
            if (!exePath.isRegularFile() || !exePath.isExecutable())
                continue
            emit(exePath)
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        val empty = Env(emptyMap())
    }
}
