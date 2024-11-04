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

package com.falsepattern.zigbrains.direnv

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.util.EnvironmentUtil
import org.jetbrains.annotations.NonNls
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

data class Env(val env: Map<String, String>) {
    private val path get() = getVariable("PATH")?.split(File.pathSeparatorChar)

    private fun getVariable(name: @NonNls String) =
        env.getOrElse(name) { EnvironmentUtil.getValue(name) }

    fun findExecutableOnPATH(exe: @NonNls String): Path? {
        val exeName = if (SystemInfo.isWindows) "$exe.exe" else exe
        val paths = path ?: return null
        for (dir in paths) {
            val path = dir.toNioPathOrNull()?.absolute() ?: continue
            if (path.notExists() || !path.isDirectory())
                continue
            val exePath = path.resolve(exeName).absolute()
            if (!exePath.isRegularFile() || !exePath.isExecutable())
                continue
            return exePath
        }
        return null
    }
}

val emptyEnv = Env(emptyMap())