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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import kotlin.io.path.pathString

/**
 * Zig build progress node IPC glue code
 */
object IPCUtil {

    val haveIPC: Boolean get() = info != null

    @JvmRecord
    data class IPCInfo(val mkfifo: MKFifo, val bash: String)

    private val info: IPCInfo? by lazy { runBlocking {
        createInfo()
    } }

    private suspend fun createInfo(): IPCInfo? {
        if (SystemInfo.isWindows) {
            return null
        }
        val mkfifo = emptyEnv
            .findAllExecutablesOnPATH("mkfifo")
            .map { it.pathString }
            .map(::MKFifo)
            .toList()
            .find { mkfifo ->
                val fifo = mkfifo.createTemp() ?: return@find false
                fifo.second.close()
                true
            } ?: return null

        val selectedBash = emptyEnv
            .findAllExecutablesOnPATH("bash")
            .map { it.pathString }
            .filter {
                val cli = GeneralCommandLine(it)
                val tmpFile = FileUtil.createTempFile("zigbrains-bash-detection", null, true).toPath()
                try {
                    cli.addParameters("-c", "exec {var}>${tmpFile.pathString}; echo foo >&\$var; exec {var}>&-")
                    val process = cli.createProcess()
                    val exitCode = process.awaitExit()
                    if (exitCode != 0) {
                        return@filter false
                    }
                    val text = tmpFile.inputStream().use { it.readAllBytes().toString(Charset.defaultCharset()).trim() }
                    if (text != "foo") {
                        return@filter false
                    }
                    true
                } finally {
                    tmpFile.deleteIfExists()
                }
            }
            .firstOrNull() ?: return null

        return IPCInfo(mkfifo, selectedBash)
    }

    suspend fun wrapWithIPC(cli: GeneralCommandLine): IPC? {
        if (!haveIPC)
            return null
        val (fifoFile, fifo) = info!!.mkfifo.createTemp() ?: return null
        //FIFO created, hack cli
        val exePath = cli.exePath
        val args = "exec {var}>${fifoFile.pathString}; ZIG_PROGRESS=\$var $exePath ${cli.parametersList.parametersString}; exec {var}>&-"
        cli.withExePath(info!!.bash)
        cli.parametersList.clearAll()
        cli.addParameters("-c", args)
        return IPC(cli, fifoFile, fifo)
    }

    @JvmRecord
    data class MKFifo(val exe: String) {
        suspend fun createTemp(): Pair<Path, AutoCloseable>? {
            val fifoFile = FileUtil.createTempFile("zigbrains-ipc-pipe", null, true).toPath()
            fifoFile.deleteIfExists()
            val fifo = create(fifoFile)
            if (fifo == null) {
                fifoFile.deleteIfExists()
                return null
            }
            return Pair(fifoFile, fifo)
        }
        suspend fun create(path: Path): AutoCloseable? {
            val cli = GeneralCommandLine(exe, path.pathString)
            val process = cli.createProcess()
            val exitCode = process.awaitExit()
            return if (exitCode == 0) AutoCloseable {
                path.deleteIfExists()
            } else null
        }
    }
}