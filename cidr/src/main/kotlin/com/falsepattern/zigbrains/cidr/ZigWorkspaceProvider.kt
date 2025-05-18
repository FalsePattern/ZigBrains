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

package com.falsepattern.zigbrains.cidr

import com.falsepattern.zigbrains.shared.sanitizedPathString
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.toNioPathOrNull
import com.jetbrains.cidr.project.workspace.CidrWorkspace
import com.jetbrains.cidr.project.workspace.CidrWorkspaceManager
import com.jetbrains.cidr.project.workspace.CidrWorkspaceProvider
import java.io.IOException
import java.nio.file.Files

class ZigWorkspaceProvider: CidrWorkspaceProvider {
    override fun getWorkspace(project: Project): CidrWorkspace? {
        getExistingWorkspace(project)?.let { return it }

        val projectDir = project.guessProjectDir()?.toNioPathOrNull() ?: return null
        try {
            Files.walk(projectDir).use { files ->
                if (files.anyMatch { it.fileName.sanitizedPathString?.let { it2 -> it2.endsWith(".zig") || it2.endsWith(".zig.zon") } ?: false }) {
                    return ZigWorkspace(project)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun loadWorkspace(project: Project) {
        if (getExistingWorkspace(project) != null)
            return
        val workspace = getWorkspace(project)
        if (workspace != null) {
            val manager = CidrWorkspaceManager.getInstance(project)
            manager.markInitializing(workspace)
            manager.markInitialized(workspace)
            manager.markLoading(workspace)
            manager.markLoaded(workspace)
        }
    }
}

private fun getExistingWorkspace(project: Project): ZigWorkspace? {
    val workspaces = CidrWorkspaceManager.getInstance(project).workspaces.keys
    for (ws in workspaces)
        if (ws is ZigWorkspace)
            return ws

    return null
}