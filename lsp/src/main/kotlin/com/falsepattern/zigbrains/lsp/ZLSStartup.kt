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

package com.falsepattern.zigbrains.lsp

import com.falsepattern.zigbrains.lsp.zls.zls
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.ui.EditorNotifications
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ZLSStartup: ProjectActivity {
    override suspend fun execute(project: Project) {
        project.zigCoroutineScope.launch {
            var currentState = project.zlsRunning()
            var currentZLS = project.zls
            while (!project.isDisposed) {
                val zls = project.zls
                if (currentZLS != zls) {
                    startLSP(project, true)
                }
                currentZLS = zls
                val running = project.zlsRunning()
                if (currentState != running) {
                    EditorNotifications.getInstance(project).updateAllNotifications()
                }
                currentState = running
                if (handleStartLSP(project)) {
                    EditorNotifications.getInstance(project).updateAllNotifications()
                }
                delay(1000)
            }
        }
    }
}