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

package com.falsepattern.zigbrains.shared.cli

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.run.ZigProcessHandler
import com.falsepattern.zigbrains.shared.ipc.IPCUtil
import com.falsepattern.zigbrains.shared.ipc.ipc
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.util.io.awaitExit
import com.intellij.util.system.OS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString


//From Apache Ant
/**
 * Crack a command line.
 * @param toProcess the command line to process.
 * @return the command line broken into strings.
 * An empty or null toProcess parameter results in a zero sized array.
 */
@Throws(ConfigurationException::class)
fun translateCommandline(toProcess: String): List<String> {
    if (toProcess.isEmpty()) {
        //no command? no string
        return listOf()
    }

    // parse with a simple finite state machine
    val normal = 0
    val inQuote = 1
    val inDoubleQuote = 2
    var state = normal
    val tok = StringTokenizer(toProcess, "\"' ", true)
    val result = ArrayList<String>()
    val current = StringBuilder()
    var lastTokenHasBeenQuoted = false

    while (tok.hasMoreTokens()) {
        val nextTok: String = tok.nextToken()
        when (state) {
            inQuote -> if ("'" == nextTok) {
                lastTokenHasBeenQuoted = true
                state = normal
            } else {
                current.append(nextTok)
            }

            inDoubleQuote -> if ("\"" == nextTok) {
                lastTokenHasBeenQuoted = true
                state = normal
            } else {
                current.append(nextTok)
            }

            else -> {
                if ("'" == nextTok) {
                    state = inQuote
                } else if ("\"" == nextTok) {
                    state = inDoubleQuote
                } else if (" " == nextTok) {
                    if (lastTokenHasBeenQuoted || current.isNotEmpty()) {
                        result.add(current.toString())
                        current.setLength(0)
                    }
                } else {
                    current.append(nextTok)
                }
                lastTokenHasBeenQuoted = false
            }
        }
    }
    if (lastTokenHasBeenQuoted || current.isNotEmpty()) {
        result.add(current.toString())
    }
    if (state == inQuote || state == inDoubleQuote) {
        throw ConfigurationException(ZigBrainsBundle.message("exception.translate-command-line.unbalanced-quotes", toProcess))
    }
    return result
}

fun createCommandLineSafe(
    workingDirectory: Path?,
    exe: Path,
    vararg parameters: String,
): Result<GeneralCommandLine> {
    if (!exe.exists())
        return Result.failure(IllegalArgumentException("file does not exist: ${exe.pathString}"))
    if (exe.isDirectory())
        return Result.failure(IllegalArgumentException("file is a directory: ${exe.pathString}"))
    val cli = GeneralCommandLine()
        .withExePath(exe.toString())
        .withWorkDirectory(workingDirectory?.toFile())
        .withParameters(*parameters)
        .withCharset(Charsets.UTF_8)
    return Result.success(cli)
}

@Throws(ExecutionException::class)
suspend fun GeneralCommandLine.startIPCAwareProcess(project: Project?, emulateTerminal: Boolean = false): ZigProcessHandler.IPCAware {
    val original = this.commandLineString
    val ipc = if (project != null && !emulateTerminal) IPCUtil.wrapWithIPC(this) else null
    val cli = ipc?.cli ?: this
    if (emulateTerminal && OS.CURRENT != OS.Windows && !cli.environment.contains("TERM")) {
        cli.withEnvironment("TERM", "xterm-256color")
    }
    val handler = ZigProcessHandler.IPCAware(original, cli)
    ProcessTerminatedListener.attach(handler)

    if (ipc != null) {
        project!!.ipc?.launchWatcher(ipc, handler.process)
    }
    return handler
}


suspend fun GeneralCommandLine.call(timeoutMillis: Long = Long.MAX_VALUE, ipcProject: Project? = null): Result<ProcessOutput> {
    val (process, exitCode) = withContext(Dispatchers.IO) {
        val handler = startIPCAwareProcess(ipcProject)
        val process = handler.process
        val exit = withTimeoutOrNull(timeoutMillis) {
            process.awaitExit()
        }
        process to exit
    }
    return runInterruptible {
        Result.success(ProcessOutput(
            process.inputStream.bufferedReader().use { it.readText() },
            process.errorStream.bufferedReader().use { it.readText() },
            exitCode ?: -1,
            exitCode == null,
            false
        ))
    }
}