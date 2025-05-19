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

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.util.progress.withProgressText
import com.intellij.util.io.awaitExit
import com.intellij.util.xmlb.annotations.Attribute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.isRegularFile

@Service(Service.Level.PROJECT)
@State(
    name = "Direnv",
    storages = [Storage("zigbrains.xml")]
)
class DirenvService(val project: Project): SerializablePersistentStateComponent<DirenvService.State>(State()), IDirenvService {
    private val mutex = Mutex()

    override val isInstalled: Boolean by lazy {
        // Using the builtin stuff here instead of Env because it should only scan for direnv on the process path
        PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS("direnv") != null
    }

    var isEnabledRaw: DirenvState
        get() = state.enabled
        set(value) {
            updateState {
                it.copy(enabled = value)
            }
        }

    override val isEnabled: DirenvState
        get() = isEnabledRaw

    override suspend fun import(): Env {
        if (!isInstalled || !TrustedProjects.isProjectTrusted(project) || project.isDefault)
            return Env.empty
        val workDir = project.guessProjectDir()?.toNioPath() ?: return Env.empty

        val runOutput = run(workDir, "export", "json")
        if (runOutput.error) {
            if (runOutput.output.contains("is blocked")) {
                Notifications.Bus.notify(Notification(
                    GROUP_DISPLAY_ID,
                    ZigBrainsBundle.message("notification.title.direnv-blocked"),
                    ZigBrainsBundle.message("notification.content.direnv-blocked"),
                    NotificationType.ERROR
                ))
                return Env.empty
            } else {
                Notifications.Bus.notify(Notification(
                    GROUP_DISPLAY_ID,
                    ZigBrainsBundle.message("notification.title.direnv-error"),
                    ZigBrainsBundle.message("notification.content.direnv-error", runOutput.output),
                    NotificationType.ERROR
                ))
                return Env.empty
            }
        }
        return if (runOutput.output.isBlank()) {
            Env.empty
        } else {
            Env(Json.decodeFromString<Map<String, String>>(runOutput.output))
        }
    }

    private suspend fun run(workDir: Path, vararg args: String): DirenvOutput {
        val cli = GeneralCommandLine("direnv", *args).withWorkingDirectory(workDir)

        val (process, exitCode) = withProgressText("Running ${cli.commandLineString}") {
            withContext(Dispatchers.IO) {
                mutex.withLock {
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

    fun hasDotEnv(): Boolean {
        if (!isInstalled)
            return false
        val projectDir = project.guessProjectDir()?.toNioPathOrNull() ?: return false
        return envFiles.any { projectDir.resolve(it).isRegularFile() }
    }

    data class State(
        @JvmField
        @Attribute
        var enabled: DirenvState = DirenvState.Auto
    )

    companion object {
        private const val GROUP_DISPLAY_ID = "zigbrains-direnv"
        fun getInstance(project: Project): IDirenvService = project.service<DirenvService>()

        private val STATE_KEY = Key.create<DirenvState>("DIRENV_STATE")

        fun getStateFor(data: UserDataHolder?, project: Project?): DirenvState {
            return data?.getUserData(STATE_KEY) ?: project?.let { getInstance(project).isEnabled } ?: DirenvState.Disabled
        }

        fun setStateFor(data: UserDataHolder, state: DirenvState) {
            data.putUserData(STATE_KEY, state)
        }
    }
}

sealed interface IDirenvService {
    val isInstalled: Boolean
    val isEnabled: DirenvState
    suspend fun import(): Env
}

private val envFiles = listOf(".envrc", ".env")