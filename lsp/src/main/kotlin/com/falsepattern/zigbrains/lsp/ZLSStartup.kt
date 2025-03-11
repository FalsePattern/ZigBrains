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

import com.falsepattern.zigbrains.direnv.DirenvCmd
import com.falsepattern.zigbrains.direnv.emptyEnv
import com.falsepattern.zigbrains.direnv.getDirenv
import com.falsepattern.zigbrains.lsp.settings.zlsSettings
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.ui.EditorNotifications
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.io.path.pathString

class ZLSStartup: ProjectActivity {
    override suspend fun execute(project: Project) {
        val zlsState = project.zlsSettings.state
        if (zlsState.zlsPath.isBlank()) {
            val env = if (DirenvCmd.direnvInstalled() && !project.isDefault && zlsState.direnv)
                project.getDirenv()
            else
                emptyEnv
            env.findExecutableOnPATH("zls")?.let {
                zlsState.zlsPath = it.pathString
                project.zlsSettings.state = zlsState
            }
        }
        project.zigCoroutineScope.launch {
            var currentState = project.zlsRunningAsync()
            while (!project.isDisposed) {
                val running = project.zlsRunningAsync()
                if (currentState != running) {
                    EditorNotifications.getInstance(project).updateAllNotifications()
                }
                currentState = running
                delay(1000)
            }
        }
    }
}