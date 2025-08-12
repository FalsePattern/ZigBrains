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
import com.falsepattern.zigbrains.lsp.zls.zls
import com.falsepattern.zigbrains.lsp.zlsRunning
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.falsepattern.zigbrains.zig.ZigFileType
import com.falsepattern.zigbrains.zon.ZonFileType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
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
        val task = project.zigCoroutineScope.async {
            if (project.zlsRunning()) {
                return@async null
            } else {
                return@async project.zls?.isValid() == true
            }
        }
        return Function { editor ->
            val status: EditorNotificationPanel.Status
            val text: String
            var actionLabel: String? = null
            var actionId: String? = null
            val result = runBlocking { task.await() }
            if (result == null)
                return@Function null

            if (!result) {
                status = EditorNotificationPanel.Status.Error
                text = ZLSBundle.message("notification.banner.zls-bad-config")
                actionLabel = ZLSBundle.message("notification.banner.zls-bad-config.action")
                actionId = "zigbrains.open.configurable.toolchain"
            } else {
                status = EditorNotificationPanel.Status.Warning
                text = ZLSBundle.message("notification.banner.zls-not-running")
				// TODO: Does this need the same "open-action" treatment?
            }
            val panel = EditorNotificationPanel(editor, status)
			panel.text = text
			if (actionId != null && actionLabel != null) {
				panel.createActionLabel(actionLabel, actionId)
			}

			return@Function panel
        }
    }
}