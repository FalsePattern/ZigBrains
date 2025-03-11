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

package com.falsepattern.zigbrains.project.toolchain.stdlib

import com.falsepattern.zigbrains.Icons
import com.falsepattern.zigbrains.project.settings.ZigProjectSettings
import com.falsepattern.zigbrains.project.settings.zigProjectSettings
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.jps.entities.*
import com.intellij.workspaceModel.ide.impl.LegacyBridgeJpsEntitySourceFactory
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.swing.Icon

class ZigSyntheticLibrary(val project: Project) : SyntheticLibrary(), ItemPresentation {
    private var state: ZigProjectSettings = project.zigProjectSettings.state.copy()
    private val roots by lazy {
        runBlocking {getRoot(state, project)}?.let { setOf(it) } ?: emptySet()
    }

    private val name by lazy {
        getName(state, project)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ZigSyntheticLibrary)
            return false

        return state == other.state
    }

    override fun hashCode(): Int {
        return Objects.hash(roots)
    }

    override fun getPresentableText(): String {
        return name
    }

    override fun getIcon(unused: Boolean): Icon {
        return Icons.Zig
    }

    override fun getSourceRoots(): Collection<VirtualFile> {
        return roots
    }

    companion object {
        private const val ZIG_LIBRARY_ID = "Zig SDK"
        private const val ZIG_MODULE_ID = "Zig"
        suspend fun reload(project: Project, state: ZigProjectSettings) {
            val moduleId = ModuleId(ZIG_MODULE_ID)
            val workspaceModel = WorkspaceModel.getInstance(project)
            if (moduleId !in workspaceModel.currentSnapshot) {
                val baseModuleDir = project.guessProjectDir()?.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager()) ?: return

                val moduleEntitySource = LegacyBridgeJpsEntitySourceFactory
                    .createEntitySourceForModule(project, baseModuleDir, null)

                val moduleEntity = ModuleEntity(ZIG_MODULE_ID, emptyList(), moduleEntitySource)
                workspaceModel.update("Add new module") {builder ->
                    builder.addEntity(moduleEntity)
                }
            }
            val moduleEntity = workspaceModel.currentSnapshot.resolve(moduleId) ?: return
            val root = getRoot(state, project) ?: return
            val libRoot = LibraryRoot(root.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager()), LibraryRootTypeId.SOURCES)
            val libraryTableId = LibraryTableId.ProjectLibraryTableId
            val libraryId = LibraryId(ZIG_LIBRARY_ID, libraryTableId)
            if (libraryId in workspaceModel.currentSnapshot) {
                val library = workspaceModel.currentSnapshot.resolve(libraryId) ?: return
                workspaceModel.update("Update library") { builder ->
                    builder.modifyLibraryEntity(library) {
                        roots.clear()
                        roots.add(libRoot)
                    }
                }
            } else {
                val libraryEntitySource = LegacyBridgeJpsEntitySourceFactory
                    .createEntitySourceForProjectLibrary(project, null)
                val libraryEntity = LibraryEntity(
                    ZIG_LIBRARY_ID,
                    libraryTableId, emptyList(),
                    libraryEntitySource
                ) {
                    roots.add(libRoot)
                }
                workspaceModel.update("Add new library") { builder ->
                    builder.addEntity(libraryEntity)
                }
            }
            workspaceModel.update("Link dep") { builder ->
                builder.modifyModuleEntity(moduleEntity) {
                    dependencies.add(LibraryDependency(libraryId, false, DependencyScope.COMPILE))
                }
            }
        }
    }
}

private fun getName(
    state: ZigProjectSettings,
    project: Project
): String {
    val tc = state.toolchain ?: return "Zig"
    val version = runBlocking { tc.zig.getEnv(project) }.mapCatching { it.version }.getOrElse { return "Zig" }
    return "Zig $version"
}

suspend fun getRoot(
    state: ZigProjectSettings,
    project: Project
): VirtualFile? {
    val toolchain = state.toolchain
    if (state.overrideStdPath) run {
        val ePathStr = state.explicitPathToStd ?: return@run
        val ePath = ePathStr.toNioPathOrNull() ?: return@run
        if (ePath.isAbsolute) {
            val roots = ePath.refreshAndFindVirtualDirectory() ?: return@run
            return roots
        } else if (toolchain != null) {
            val stdPath = toolchain.location.resolve(ePath)
            if (stdPath.isAbsolute) {
                val roots = stdPath.refreshAndFindVirtualDirectory() ?: return@run
                return roots
            }
        }
    }
    if (toolchain != null) {
        val stdPath = toolchain.zig.getEnv(project).mapCatching { it.stdPath(toolchain, project) }.getOrNull() ?: return null
        val roots = stdPath.refreshAndFindVirtualDirectory() ?: return null
        return roots
    }
    return null
}