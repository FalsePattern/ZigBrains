/*
 * ZigBrains
 * Copyright (C) 2023-2025 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.zigbrains.project.stdlib

import com.falsepattern.zigbrains.Icons
import com.falsepattern.zigbrains.project.buildscan.zigBuildScan
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.pointers.VirtualFilePointer
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import javax.swing.Icon

class ZigDependencyLibraryRootProvider: AdditionalLibraryRootsProvider() {
	override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
		val urlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
		val vfsManager = VirtualFileManager.getInstance()
		val libraries = ArrayList<SyntheticLibrary>()
		val std = project.service<ZigStandardLibraryRootService>()
		std.root?.let { root ->
			libraries.add(ZigDependencyLibrary(std.name, root))
		}
		if (project.zigBuildScan.enabled) {
			libraries.addAll(
				project.zigBuildScan
					.projects
					.asSequence()
					.drop(1) // first is always the root project
					.mapNotNull { pathToVirtualFile(urlManager, vfsManager, it.path) }
					.map {
						if (it.name.startsWith("N-V-__")) {
							ZigDependencyLibrary("[unnamed package]", it)
						} else {
							val (name, version, _) = it.name.split("-", limit = 3)
							ZigDependencyLibrary("$name $version", it)
						}
					}
					.toList()
			)
		}

		return libraries
	}

	class ZigDependencyLibrary(private val name: String, private val root: VirtualFile): SyntheticLibrary(), ItemPresentation {
		override fun equals(other: Any?): Boolean =
			other is ZigDependencyLibrary && other.root == this.root

		override fun hashCode(): Int =
			this.root.hashCode()

		override fun getSourceRoots(): Collection<VirtualFile> =
			listOf(this.root)

		override fun isShowInExternalLibrariesNode(): Boolean {
			return true
		}

		override fun getPresentableText(): String {
			return this.name
		}

		override fun getIcon(unused: Boolean): Icon {
			return Icons.Zig
		}
	}
}

private fun pathToVirtualFile(urlManager: VirtualFileUrlManager, vfsManager: VirtualFileManager, path: String): VirtualFile? {
	val nioPath = path.toNioPathOrNull() ?: run {
		logger.warn("NIO path null for $path")
		return null
	}
	val url = nioPath.toVirtualFileUrl(urlManager)
	(url as? VirtualFilePointer)?.file?.let {
		return it
	}
	//Non-refresh first to reduce overhead a little
	val virtualFile = vfsManager.findFileByUrl(url.url) ?: vfsManager.refreshAndFindFileByUrl(url.url) ?: run {
		logger.warn("VirtualFile null for $path")
		return null
	}
	return virtualFile
}

private val logger = Logger.getInstance(ZigDependencyLibraryRootProvider::class.java)