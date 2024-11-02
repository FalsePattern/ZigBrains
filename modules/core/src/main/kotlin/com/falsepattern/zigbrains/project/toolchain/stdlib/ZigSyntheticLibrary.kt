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

package com.falsepattern.zigbrains.project.toolchain.stdlib

import com.falsepattern.zigbrains.Icons
import com.falsepattern.zigbrains.project.settings.ZigProjectSettings
import com.falsepattern.zigbrains.project.settings.zigProjectSettings
import com.falsepattern.zigbrains.shared.coroutine.getOrAwaitModalOrBlocking
import com.falsepattern.zigbrains.shared.coroutine.runModalOrBlocking
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.util.suspendingLazy
import kotlinx.coroutines.async
import java.util.*
import javax.swing.Icon

class ZigSyntheticLibrary(val project: Project) : SyntheticLibrary(), ItemPresentation {
    private val roots = project.zigCoroutineScope.suspendingLazy {
        getRoots(project.zigProjectSettings.state, project)
    }

    private val name = project.zigCoroutineScope.suspendingLazy {
        getName(project.zigProjectSettings.state, project)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ZigSyntheticLibrary)
            return false

        return roots == other.roots
    }

    override fun hashCode(): Int {
        return Objects.hash(roots)
    }

    override fun getPresentableText(): String {
        return name.getOrAwaitModalOrBlocking({ModalTaskOwner.project(project)}, {"ZigSyntheticLibrary.getPresentableText"})
    }

    override fun getIcon(unused: Boolean): Icon {
        return Icons.ZIG
    }

    override fun getSourceRoots(): Collection<VirtualFile> {
        return roots.getOrAwaitModalOrBlocking({ ModalTaskOwner.project(project)}, {"ZigSyntheticLibrary.getSourceRoots"})
    }

}



private suspend fun getName(
    state: ZigProjectSettings,
    project: Project
): String {
    val tc = state.toolchain ?: return "Zig"
    val version = tc.zig.getEnv(project).version
    return "Zig $version"
}

private suspend fun getRoots(
    state: ZigProjectSettings,
    project: Project
): Set<VirtualFile> {
    val toolchain = state.toolchain
    run {
        val ePathStr = state.explicitPathToStd ?: return@run
        val ePath = ePathStr.toNioPathOrNull() ?: return@run
        if (ePath.isAbsolute) {
            val roots = ePath.refreshAndFindVirtualDirectory() ?: return@run
            return setOf(roots)
        } else if (toolchain != null) {
            val stdPath = toolchain.location.resolve(ePath)
            if (stdPath.isAbsolute) {
                val roots = stdPath.refreshAndFindVirtualDirectory() ?: return@run
                return setOf(roots)
            }
        }
    }
    if (toolchain != null) {
        val stdPath = toolchain.zig.getEnv(project).stdPath(toolchain) ?: return emptySet()
        val roots = stdPath.refreshAndFindVirtualDirectory() ?: return emptySet()
        return setOf(roots)
    }
    return emptySet()
}