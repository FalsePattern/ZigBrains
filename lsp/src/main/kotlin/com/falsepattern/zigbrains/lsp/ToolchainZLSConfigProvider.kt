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

import com.falsepattern.zigbrains.lsp.config.SuspendingZLSConfigProvider
import com.falsepattern.zigbrains.lsp.config.ZLSConfig
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainService
import com.falsepattern.zigbrains.project.toolchain.local.LocalZigToolchain
import com.falsepattern.zigbrains.shared.sanitizedPathString
import com.falsepattern.zigbrains.shared.sanitizedToNioPath
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

class ToolchainZLSConfigProvider: SuspendingZLSConfigProvider {
    override suspend fun getEnvironment(project: Project, previous: ZLSConfig): ZLSConfig {
        val svc = ZigToolchainService.getInstance(project)
        val toolchain = svc.toolchain ?: return previous

        val env = toolchain.zig.getEnv(project).getOrElse { throwable ->
            throwable.printStackTrace()
            Notification(
                "zigbrains-lsp",
                "Failed to evaluate \"zig env\": ${throwable.message}",
                NotificationType.ERROR
            ).notify(project)
            return previous
        }

        val exe = env.zigExecutable.sanitizedToNioPath() ?: run {
            Notification(
                "zigbrains-lsp",
                "Invalid zig executable path: ${env.zigExecutable}",
                NotificationType.ERROR
            ).notify(project)
            return previous
        }
        if (!exe.toFile().exists()) {
            Notification(
                "zigbrains-lsp",
                "Zig executable does not exist: $exe",
                NotificationType.ERROR
            ).notify(project)
            return previous
        }
        var lib = if (toolchain is LocalZigToolchain)
            toolchain.std
        else
            null

        if (lib == null) {
            lib = env.libDirectory.sanitizedToNioPath() ?: run {
                Notification(
                    "zigbrains-lsp",
                    "Invalid zig standard library path: ${env.libDirectory}",
                    NotificationType.ERROR
                ).notify(project)
                null
            }
        }
        if (lib == null)
            return previous

        return previous.copy(zig_exe_path = exe.sanitizedPathString, zig_lib_path = lib.sanitizedPathString)
    }
}