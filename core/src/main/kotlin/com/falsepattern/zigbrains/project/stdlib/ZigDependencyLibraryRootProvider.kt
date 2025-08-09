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
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import javax.swing.Icon

class ZigDependencyLibraryRootProvider: AdditionalLibraryRootsProvider() {
	override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
		val urlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
		val libraries = ArrayList<SyntheticLibrary>()
		val std = project.service<ZigStandardLibraryRootService>()
		std.root?.let { root ->
			libraries.add(ZigDependencyLibrary(std.name, root))
		}
		if (project.zigBuildScan.enabled) {
			libraries.addAll(
				project.zigBuildScan
					.projects
					.stream()
					.skip(1)  // first is always the root project
					.map { it.path.toNioPathOrNull()!!.toVirtualFileUrl(urlManager).virtualFile!! }
					.map {
						val (name, version, _) = it.name.split("-", limit = 3)
						ZigDependencyLibrary("$name $version", it)
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