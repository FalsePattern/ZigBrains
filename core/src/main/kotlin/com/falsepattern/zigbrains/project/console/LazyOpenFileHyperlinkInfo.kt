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

package com.falsepattern.zigbrains.project.console

import com.intellij.execution.filters.FileHyperlinkInfo
import com.intellij.execution.filters.FileHyperlinkInfoBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.application
import java.nio.file.Path

class LazyOpenFileHyperlinkInfo(myProject: Project, val path: Path, myDocumentLine: Int, myDocumentColumn: Int, myUseBrowser: Boolean = true) : FileHyperlinkInfoBase(
    myProject,
    myDocumentLine,
    myDocumentColumn,
    myUseBrowser
), FileHyperlinkInfo {
    override val virtualFile: VirtualFile?
        get() {
            val manager = VirtualFileManager.getInstance()
            return if (application.isReadAccessAllowed && !application.isDispatchThread) {
                manager.findFileByNioPath(path) ?: return null
            } else {
                manager.findFileByNioPath(path) ?: manager.refreshAndFindFileByNioPath(path) ?: return null
            }
        }
}