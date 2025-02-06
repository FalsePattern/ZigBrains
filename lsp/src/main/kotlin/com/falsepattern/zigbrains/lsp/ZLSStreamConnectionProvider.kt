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

import com.falsepattern.zigbrains.direnv.emptyEnv
import com.falsepattern.zigbrains.direnv.getDirenv
import com.falsepattern.zigbrains.lsp.config.ZLSConfigProviderBase
import com.falsepattern.zigbrains.lsp.settings.zlsSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.eclipse.lsp4j.InlayHint
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage
import org.eclipse.lsp4j.services.LanguageServer
import java.nio.file.Path
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString

class ZLSStreamConnectionProvider private constructor(private val project: Project, commandLine: GeneralCommandLine?) : OSProcessStreamConnectionProvider(commandLine) {
    companion object {
        suspend fun create(project: Project): ZLSStreamConnectionProvider {
            val projectDir = project.guessProjectDir()?.toNioPathOrNull()
            val commandLine = getCommand(project)?.let { GeneralCommandLine(it) }?.withWorkingDirectory(projectDir)
            return ZLSStreamConnectionProvider(project, commandLine)
        }

        @OptIn(ExperimentalSerializationApi::class)
        suspend fun getCommand(project: Project): List<String>? {
            val svc = project.zlsSettings
            val state = svc.state
            val zlsPath: Path = state.zlsPath.let { zlsPath ->
                if (zlsPath.isEmpty()) {
                    val env = if (state.direnv) project.getDirenv() else emptyEnv
                    env.findExecutableOnPATH("zls") ?: run {
                        Notification(
                            "zigbrains-lsp",
                            ZLSBundle.message("notification.message.could-not-detect.content"),
                            NotificationType.ERROR
                        ).notify(project)
                        return null
                    }
                } else {
                    zlsPath.toNioPathOrNull() ?: run {
                        Notification(
                            "zigbrains-lsp",
                            ZLSBundle.message("notification.message.zls-exe-path-invalid.content", zlsPath),
                            NotificationType.ERROR
                        ).notify(project)
                        return null
                    }
                }
            }
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
            val configPath: Path? = state.zlsConfigPath.let { configPath ->
                if (configPath.isNotBlank()) {
                    configPath.toNioPathOrNull()?.let { nioPath ->
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
            cmd.add(zlsPath.pathString)
            if (configPath != null) {
                cmd.add("--config-path")
                cmd.add(configPath.pathString)
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


