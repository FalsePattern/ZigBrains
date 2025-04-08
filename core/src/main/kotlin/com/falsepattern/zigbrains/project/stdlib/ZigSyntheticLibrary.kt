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

import com.falsepattern.zigbrains.Icons
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainService
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.local.LocalZigToolchain
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.jps.entities.*
import com.intellij.workspaceModel.ide.legacyBridge.LegacyBridgeJpsEntitySourceFactory
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.swing.Icon

class ZigSyntheticLibrary(val project: Project) : SyntheticLibrary(), ItemPresentation {
    private var toolchain: ZigToolchain? = ZigToolchainService.getInstance(project).toolchain
    private val roots by lazy {
        runBlocking {getRoot(toolchain, project)}?.let { setOf(it) } ?: emptySet()
    }

    private val name by lazy {
        getName(toolchain, project)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ZigSyntheticLibrary)
            return false

        return toolchain == other.toolchain
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
        suspend fun reload(project: Project, toolchain: ZigToolchain?) {
            val moduleId = ModuleId(ZIG_MODULE_ID)
            val workspaceModel = WorkspaceModel.getInstance(project)
            val root = getRoot(toolchain, project) ?: return
            val libRoot = LibraryRoot(root.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager()), LibraryRootTypeId.SOURCES)
            val libraryTableId = LibraryTableId.ProjectLibraryTableId
            val libraryId = LibraryId(ZIG_LIBRARY_ID, libraryTableId)
            val baseModuleDir = project.guessProjectDir()?.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager()) ?: return
            workspaceModel.update("Update Zig std") { builder ->
                builder.resolve(moduleId)?.let { moduleEntity ->
                    builder.removeEntity(moduleEntity)
                }
                val moduleEntitySource = LegacyBridgeJpsEntitySourceFactory.getInstance(project)
                    .createEntitySourceForModule(baseModuleDir, null)

                val moduleEntity = builder.addEntity(ModuleEntity(ZIG_MODULE_ID, emptyList(), moduleEntitySource))

                builder.resolve(libraryId)?.let { libraryEntity ->
                    builder.removeEntity(libraryEntity)
                }
                val libraryEntitySource = LegacyBridgeJpsEntitySourceFactory
                    .getInstance(project)
                    .createEntitySourceForProjectLibrary(null)
                val libraryEntity = LibraryEntity(
                    ZIG_LIBRARY_ID,
                    libraryTableId, emptyList(),
                    libraryEntitySource
                ) {
                    roots.add(libRoot)
                }
                builder.addEntity(libraryEntity)
                builder.modifyModuleEntity(moduleEntity) {
                    val dep = LibraryDependency(libraryId, false, DependencyScope.COMPILE)
                    dependencies.clear()
                    dependencies.add(dep)
                }
            }
        }
    }
}

private fun getName(
    toolchain: ZigToolchain?,
    project: Project
): String {
    val tc = toolchain ?: return "Zig"
    toolchain.name?.let { return it }
    runBlocking { tc.zig.getEnv(project) }
        .mapCatching { it.version }
        .getOrNull()
        ?.let { return "Zig $it" }
    return "Zig"
}

suspend fun getRoot(
    toolchain: ZigToolchain?,
    project: Project
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