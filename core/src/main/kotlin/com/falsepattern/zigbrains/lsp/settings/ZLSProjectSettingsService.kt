/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2024 FalsePattern
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

package com.falsepattern.zigbrains.lsp.settings

import com.falsepattern.zigbrains.direnv.emptyEnv
import com.falsepattern.zigbrains.direnv.getDirenv
import com.falsepattern.zigbrains.lsp.ZLSBundle
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.util.application
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile

@Service(Service.Level.PROJECT)
@State(
    name = "ZLSSettings",
    storages = [Storage(value = "zigbrains.xml")]
)
class ZLSProjectSettingsService(val project: Project): PersistentStateComponent<ZLSSettings> {
    @Volatile
    private var state = ZLSSettings()
    @Volatile
    private var dirty = true
    @Volatile
    private var valid = false

    private val mutex = ReentrantLock()
    override fun getState(): ZLSSettings {
        return state.copy()
    }

    fun setState(value: ZLSSettings) {
        mutex.withLock {
            this.state = value
            dirty = true
        }
    }

    override fun loadState(state: ZLSSettings) {
        mutex.withLock {
            this.state = state
            dirty = true
        }
    }

    fun isModified(otherData: ZLSSettings): Boolean {
        return state != otherData
    }

    fun validate(): Boolean {
        mutex.withLock {
            if (dirty) {
                val state = this.state
                valid = if (application.isDispatchThread) {
                    runWithModalProgressBlocking(ModalTaskOwner.project(project), ZLSBundle.message("progress.title.validate")) {
                        doValidate(project, state)
                    }
                } else {
                    runBlocking {
                        doValidate(project, state)
                    }
                }
                dirty = false
            }
            return valid
        }
    }
}

private suspend fun doValidate(project: Project, state: ZLSSettings): Boolean {
    val zlsPath: Path = state.zlsPath.let { zlsPath ->
        if (zlsPath.isEmpty()) {
            val env = if (state.direnv) project.getDirenv() else emptyEnv
            env.findExecutableOnPATH("zls") ?: run {
                return false
            }
        } else {
            zlsPath.toNioPathOrNull() ?: run {
                return false
            }
        }
    }
    if (!zlsPath.toFile().exists()) {
        return false
    }
    if (!zlsPath.isRegularFile() || !zlsPath.isExecutable()) {
        return false
    }
    return true
}

val Project.zlsSettings get() = service<ZLSProjectSettingsService>()