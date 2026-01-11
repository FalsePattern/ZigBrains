/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2026 FalsePattern
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

package com.falsepattern.zigbrains.project

import com.falsepattern.zigbrains.Icons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import javax.swing.Icon

class ZigProjectOpenProcessor: ProjectOpenProcessor() {
    override val icon: Icon get() = Icons.Zig
    override val name: String get() = "Zig"

    // check if we can open a file/folder as a Zig project
    override fun canOpenProject(file: VirtualFile): Boolean =
        FileUtil.namesEqual(file.name, "build.zig") ||
        FileUtil.namesEqual(file.name, "build.zig.zon") ||
        file.isDirectory && (file.findChild("build.zig") != null || file.findChild("build.zig.zon") != null)

    override fun doOpenProject(
        virtualFile: VirtualFile,
        projectToClose: Project?,
        forceOpenInNewFrame: Boolean
    ): Project? {
        val basedir = if (virtualFile.isDirectory) virtualFile else virtualFile.parent

        return PlatformProjectOpenProcessor.getInstance().doOpenProject(basedir, projectToClose, forceOpenInNewFrame)
    }

    override suspend fun openProjectAsync(
        virtualFile: VirtualFile,
        projectToClose: Project?,
        forceOpenInNewFrame: Boolean
    ): Project? {
        val basedir = if (virtualFile.isDirectory) virtualFile else virtualFile.parent
        return PlatformProjectOpenProcessor.getInstance().openProjectAsync(basedir, projectToClose, forceOpenInNewFrame)
    }
}