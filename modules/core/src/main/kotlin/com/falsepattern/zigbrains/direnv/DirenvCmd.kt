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

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.util.io.awaitExit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class DirenvCmd(val workingDirectory: Path) {
    fun direnvInstalled() =
        PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS("direnv") != null

    private suspend fun run(vararg args: String): DirenvOutput {
        val cli = GeneralCommandLine("direnv", *args)
            .withWorkingDirectory(workingDirectory)

        val process = cli.createProcess()
        if (process.awaitExit() != 0) {
            val stdErr = process.errorStream.bufferedReader().use { it.readText() }
            return DirenvOutput(stdErr, true)
        }

        val stdOut = process.errorStream.bufferedReader().use { it.readText() }
        return DirenvOutput(stdOut, false)
    }
}