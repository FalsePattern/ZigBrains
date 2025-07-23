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
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.project.isDirectoryBased
import com.intellij.project.stateStore
import com.intellij.workspaceModel.ide.impl.LegacyBridgeJpsEntitySourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Objects
import javax.swing.Icon

class ZigSyntheticLibrary(val project: Project) : SyntheticLibrary(), ItemPresentation {
    private var toolchain: ZigToolchain? = ZigToolchainService.getInstance(project).toolchain
    private val roots get() = project.service<ZigLibraryRootService>().root?.let { setOf(it) } ?: emptySet()
    private val name get() = project.service<ZigLibraryRootService>().name

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

    override fun isShowInExternalLibrariesNode(): Boolean {
        return !roots.isEmpty()
    }

    companion object {
        private const val ZIG_LIBRARY_ID = "Zig SDK"
        private const val ZIG_MODULE_ID = "ZigBrains"
        private val libraryTableId = LibraryTableId.ProjectLibraryTableId
        private val libraryId = LibraryId(ZIG_LIBRARY_ID, libraryTableId)
        private val moduleId = ModuleId(ZIG_MODULE_ID)
        suspend fun reload(project: Project, toolchain: ZigToolchain?) {
            val svc = project.service<ZigLibraryRootService>()
            svc.reset(toolchain)
            val root = svc.root
            if (root != null) {
                add(project, root)
            } else {
                remove(project)
            }
        }

        private suspend fun remove(project: Project) {
            val workspaceModel = WorkspaceModel.getInstance(project)
            workspaceModel.update("Update Zig std") { builder ->
                builder.resolve(moduleId)?.let { moduleEntity ->
                    builder.removeEntity(moduleEntity)
                }
                builder.resolve(libraryId)?.let { libraryEntity ->
                    builder.removeEntity(libraryEntity)
                }
            }
        }

        private suspend fun add(project: Project, root: VirtualFile) {
            val workspaceModel = WorkspaceModel.getInstance(project)
            val libRoot = LibraryRoot(root.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager()), LibraryRootTypeId.SOURCES)

            var baseModuleDirFile: VirtualFile? = null
            if (project.isDirectoryBased) {
                baseModuleDirFile = project.stateStore.directoryStorePath?.refreshAndFindVirtualDirectory()
            }
            if (baseModuleDirFile == null) {
                baseModuleDirFile = project.guessProjectDir()
            }
            val baseModuleDir = baseModuleDirFile?.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager()) ?: return
            workspaceModel.update("Update Zig std") { builder ->
                builder.resolve(moduleId)?.let { moduleEntity ->
                    builder.removeEntity(moduleEntity)
                }
                val moduleEntitySource = LegacyBridgeJpsEntitySourceFactory
                    .createEntitySourceForModule(project, baseModuleDir, null)

                val moduleEntity = builder.addEntity(ModuleEntity(ZIG_MODULE_ID, emptyList(), moduleEntitySource))

                builder.resolve(libraryId)?.let { libraryEntity ->
                    builder.removeEntity(libraryEntity)
                }
                val libraryEntitySource = LegacyBridgeJpsEntitySourceFactory
                    .createEntitySourceForProjectLibrary(project, null)
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

@Service(Service.Level.PROJECT)
class ZigLibraryRootService(val project: Project) {
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