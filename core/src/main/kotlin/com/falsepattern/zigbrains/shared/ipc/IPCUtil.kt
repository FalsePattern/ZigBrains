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

package com.falsepattern.zigbrains.shared.ipc

import com.falsepattern.zigbrains.direnv.emptyEnv
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.awaitExit
import java.io.File
import java.nio.file.Path
import javax.swing.tree.DefaultMutableTreeNode
import kotlin.io.path.deleteIfExists
import kotlin.io.path.pathString

/**
 * Zig build progress node IPC glue code
 */
object IPCUtil {
    val haveIPC = checkHaveIPC()

    private fun checkHaveIPC(): Boolean {
        if (SystemInfo.isWindows) {
            return false;
        }
        val mkfifo = emptyEnv.findExecutableOnPATH("mkfifo")
        val bash = emptyEnv.findExecutableOnPATH("bash")
        return mkfifo != null && bash != null
    }

    private suspend fun mkfifo(path: Path): AutoCloseable? {
        val cli = GeneralCommandLine("mkfifo", path.pathString)
        val process = cli.createProcess()
        val exitCode = process.awaitExit()
        return if (exitCode == 0) AutoCloseable {
            path.deleteIfExists()
        } else null
    }

    data class IPC(val cli: GeneralCommandLine, val fifoPath: Path, val fifoClose: AutoCloseable)

    suspend fun wrapWithIPC(cli: GeneralCommandLine): IPC? {
        if (!haveIPC)
            return null
        val fifoFile = FileUtil.createTempFile("zigbrains-ipc-pipe", null, true).toPath()
        fifoFile.deleteIfExists()
        val fifo = mkfifo(fifoFile)
        if (fifo == null) {
            fifoFile.deleteIfExists()
            return null
        }
        //FIFO created, hack cli
        val exePath = cli.exePath
        val args = "exec {var}>${fifoFile.pathString}; ZIG_PROGRESS=\$var $exePath ${cli.parametersList.parametersString}; exec {var}>&-"
        cli.withExePath("bash")
        cli.parametersList.clearAll()
        cli.addParameters("-c", args)
        return IPC(cli, fifoFile, fifo)
    }

}