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

package com.falsepattern.zigbrains.lsp.notification

import com.falsepattern.zigbrains.lsp.ZLSBundle
import com.falsepattern.zigbrains.lsp.settings.zlsSettings
import com.falsepattern.zigbrains.lsp.zlsRunningSync
import com.falsepattern.zigbrains.zig.ZigFileType
import com.falsepattern.zigbrains.zon.ZonFileType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import java.util.function.Function
import javax.swing.JComponent

class ZigEditorNotificationProvider: EditorNotificationProvider, DumbAware {
    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?>? {
        when (file.fileType) {
            ZigFileType, ZonFileType -> {}
            else -> return null
        }
        if (project.zlsRunningSync()) {
            return null
        }
        return Function { editor ->
            val status: EditorNotificationPanel.Status
            val message: String
            if (!project.zlsSettings.validateSync()) {
                status = EditorNotificationPanel.Status.Error
                message = ZLSBundle.message("notification.banner.zls-bad-config")
            } else {
                status = EditorNotificationPanel.Status.Warning
                message = ZLSBundle.message("notification.banner.zls-not-running")
            }
            EditorNotificationPanel(editor, status).also { it.text = message }
        }
    }
}