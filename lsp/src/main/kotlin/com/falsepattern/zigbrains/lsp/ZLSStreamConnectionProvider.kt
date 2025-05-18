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

package com.falsepattern.zigbrains.lsp

import com.falsepattern.zigbrains.lsp.config.ZLSConfigProviderBase
import com.falsepattern.zigbrains.lsp.zls.zls
import com.falsepattern.zigbrains.shared.sanitizedPathString
import com.falsepattern.zigbrains.shared.sanitizedToNioPath
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.toNioPathOrNull
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.nio.file.Path
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile

class ZLSStreamConnectionProvider private constructor(private val project: Project, commandLine: GeneralCommandLine?) : OSProcessStreamConnectionProvider(commandLine) {
    companion object {
        suspend fun create(project: Project): ZLSStreamConnectionProvider {
            val projectDir = project.guessProjectDir()?.toNioPathOrNull()
            val commandLine = getCommand(project)?.let { GeneralCommandLine(it) }?.withWorkDirectory(projectDir?.toFile())
            return ZLSStreamConnectionProvider(project, commandLine)
        }

        @OptIn(ExperimentalSerializationApi::class)
        suspend fun getCommand(project: Project): List<String>? {
            val zls = project.zls ?: return null
            val zlsPath: Path = zls.path
            if (!zlsPath.toFile().exists()) {
                Notification(
                    "zigbrains-lsp",
                    ZLSBundle.message("notification.message.zls-exe-not-exists.content", zlsPath),
                    NotificationType.ERROR
                ).notify(project)
                return null
            }
            if (!zlsPath.isRegularFile() || !zlsPath.isExecutable()) {
                Notification(
                    "zigbrains-lsp",
                    ZLSBundle.message("notification.message.zls-exe-not-executable.content", zlsPath),
                    NotificationType.ERROR
                ).notify(project)
                return null
            }
            val configPath: Path? = "".let { configPath ->
                if (configPath.isNotBlank()) {
                    configPath.sanitizedToNioPath()?.let { nioPath ->
                        if (!nioPath.toFile().exists()) {
                            Notification(
                                "zigbrains-lsp",
                                ZLSBundle.message("notification.message.zls-config-not-exists.content", nioPath),
                                NotificationType.ERROR
                            ).notify(project)
                            null
                        } else if (!nioPath.isRegularFile()) {
                            Notification(
                                "zigbrains-lsp",
                                ZLSBundle.message("notification.message.zls-config-not-file.content", nioPath),
                                NotificationType.ERROR
                            ).notify(project)
                            null
                        } else {
                            nioPath
                        }
                    } ?: run {
                        Notification(
                            "zigbrains-lsp",
                            ZLSBundle.message("notification.message.zls-config-path-invalid.content", configPath),
                            NotificationType.ERROR
                        ).notify(project)
                        null
                    }
                } else {
                    null
                }
            } ?: run {
                val config = ZLSConfigProviderBase.findEnvironment(project)
                if (config.zig_exe_path.isNullOrEmpty() || config.zig_lib_path.isNullOrEmpty()) {
                    Notification(
                        "zigbrains-lsp",
                        ZLSBundle.message("notification.message.zls-config-autogen-failed.content"),
                        NotificationType.ERROR
                    ).notify(project)
                    null
                } else {
                    val tmpFile = FileUtil.createTempFile("zigbrains-zls-autoconf", ".json", true)
                    tmpFile.outputStream().buffered().use { writer -> Json.encodeToStream(config, writer) }
                    tmpFile.toPath()
                }
            }
            val cmd = ArrayList<String>()
            cmd.add(zlsPath.sanitizedPathString!!)
            configPath?.sanitizedPathString?.let { cfgPath ->
                cmd.add("--config-path")
                cmd.add(cfgPath)
            }

            if (SystemInfo.isWindows) {
                val sb: StringBuilder by lazy { StringBuilder() }
                for (i in 0..<cmd.size) {
                    val s = cmd[i]
                    if (s.contains(' ')) {
                        sb.setLength(0)
                        cmd[i] = sb.append('"').append(s).append('"').toString()
                    }
                }
            }
            return cmd
        }
    }
}


