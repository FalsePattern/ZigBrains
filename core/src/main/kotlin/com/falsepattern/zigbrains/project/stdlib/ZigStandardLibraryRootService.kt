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

package com.falsepattern.zigbrains.project.stdlib

import com.falsepattern.zigbrains.project.toolchain.ZigToolchainService
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.local.LocalZigToolchain
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class ZigStandardLibraryRootService(val project: Project) {
    var root: VirtualFile? = null
        private set
    var name: String = "Zig"
        private set

    init {
        zigCoroutineScope.launch(Dispatchers.EDT) {
            reset(ZigToolchainService.getInstance(project).toolchain)
        }
    }

    suspend fun reset(toolchain: ZigToolchain?) {
        root = getRoot(toolchain)
        name = getName(toolchain)
    }

    private suspend fun getName(
        toolchain: ZigToolchain?
    ): String {
        val tc = toolchain ?: return "Zig"
        toolchain.name?.let { return it }
        tc.zig.getEnv(project)
            .mapCatching { it.version }
            .getOrNull()
            ?.let { return "Zig $it" }
        return "Zig"
    }

    private suspend fun getRoot(
        toolchain: ZigToolchain?
    ): VirtualFile? {
        //TODO universal
        if (toolchain !is LocalZigToolchain) {
            return null
        }
        if (toolchain.std != null) run {
            val ePath = toolchain.std
            if (ePath.isAbsolute) {
                val roots = ePath.refreshAndFindVirtualDirectory() ?: return@run
                return roots
            }
            val stdPath = toolchain.location.resolve(ePath)
            if (stdPath.isAbsolute) {
                val roots = stdPath.refreshAndFindVirtualDirectory() ?: return@run
                return roots
            }
        }
        val stdPath = toolchain.zig.getEnv(project).mapCatching { it.stdPath(toolchain, project) }.getOrNull() ?: return null
        val roots = stdPath.refreshAndFindVirtualDirectory() ?: return null
        return roots
    }
}