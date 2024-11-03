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

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.platform.util.progress.withProgressText
import com.intellij.util.io.awaitExit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.NonNls
import java.nio.file.Path

object DirenvCmd {
    suspend fun importDirenv(project: Project): Env {
        if (!direnvInstalled())
            return emptyEnv
        val workDir = project.guessProjectDir()?.toNioPath() ?: return emptyEnv

        val runOutput = run(project, workDir, "export", "json")
        if (runOutput.error) {
            if (runOutput.output.contains("is blocked")) {
                Notifications.Bus.notify(Notification(
                    GROUP_DISPLAY_ID,
                    ZigBrainsBundle.message("notification.title.direnv-blocked"),
                    ZigBrainsBundle.message("notification.content.direnv-blocked"),
                    NotificationType.ERROR
                ))
                return emptyEnv
            } else {
                Notifications.Bus.notify(Notification(
                    GROUP_DISPLAY_ID,
                    ZigBrainsBundle.message("notification.title.direnv-error"),
                    ZigBrainsBundle.message("notification.content.direnv-error", runOutput.output),
                    NotificationType.ERROR
                ))
                return emptyEnv
            }
        }
        return Env(Json.decodeFromString<Map<String, String>>(runOutput.output))
    }

    private suspend fun run(project: Project, workDir: Path, vararg args: String): DirenvOutput {
        val cli = GeneralCommandLine("direnv", *args).withWorkingDirectory(workDir)

        val (process, exitCode) = withProgressText("Running ${cli.commandLineString}") {
            withContext(Dispatchers.IO) {
                project.direnvService.mutex.withLock {
                    val process = cli.createProcess()
                    val exitCode = process.awaitExit()
                    process to exitCode
                }
            }
        }

        if (exitCode != 0) {
            val stdErr = process.errorStream.bufferedReader().use { it.readText() }
            return DirenvOutput(stdErr, true)
        }

        val stdOut = process.inputStream.bufferedReader().use { it.readText() }
        return DirenvOutput(stdOut, false)
    }

    private const val GROUP_DISPLAY_ID = "zigbrains-direnv"
    fun direnvInstalled() =
        // Using the builtin stuff here instead of Env because it should only scan for direnv on the process path
        PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS("direnv") != null
}

suspend fun Project?.getDirenv(): Env {
    if (this == null)
        return emptyEnv
    return DirenvCmd.importDirenv(this)
}