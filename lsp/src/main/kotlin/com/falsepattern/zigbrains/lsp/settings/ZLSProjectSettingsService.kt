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

package com.falsepattern.zigbrains.lsp.settings

import com.falsepattern.zigbrains.direnv.emptyEnv
import com.falsepattern.zigbrains.direnv.getDirenv
import com.falsepattern.zigbrains.lsp.ZLSBundle
import com.falsepattern.zigbrains.lsp.startLSP
import com.falsepattern.zigbrains.project.settings.zigProjectSettings
import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.util.application
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Path
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

    private val mutex = Mutex()
    override fun getState(): ZLSSettings {
        return state.copy()
    }

    fun setState(value: ZLSSettings) {
        runBlocking {
            mutex.withLock {
                this@ZLSProjectSettingsService.state = value
                dirty = true
            }
        }
        startLSP(project, true)
    }

    override fun loadState(state: ZLSSettings) {
        setState(state)
    }

    suspend fun validateAsync(): Boolean {
        mutex.withLock {
            if (dirty) {
                val state = this.state
                valid = doValidate(project, state)
                dirty = false
            }
            return valid
        }
    }

    fun validateSync(): Boolean {
        val isValid: Boolean? = runBlocking {
            mutex.withLock {
                if (dirty)
                    null
                else
                    valid
            }
        }
        if (isValid != null) {
            return isValid
        }
        return if (useModalProgress()) {
            runWithModalProgressBlocking(ModalTaskOwner.project(project), ZLSBundle.message("progress.title.validate")) {
                validateAsync()
            }
        } else {
            runBlocking {
                validateAsync()
            }
        }
    }
}

private val prohibitClass: Class<*>? = runCatching {
    Class.forName("com_intellij_ide_ProhibitAWTEvents".replace('_', '.'))
}.getOrNull()

private val postProcessors: List<*>? = runCatching {
    if (prohibitClass == null)
        return@runCatching null
    val postProcessorsField = IdeEventQueue::class.java.getDeclaredField("postProcessors")
    postProcessorsField.isAccessible = true
    postProcessorsField.get(IdeEventQueue.getInstance()) as? List<*>
}.getOrNull()

private fun useModalProgress(): Boolean {
    if (!application.isDispatchThread)
        return false

    if (application.isWriteAccessAllowed)
        return false

    if (postProcessors == null)
        return true

    return postProcessors.none { prohibitClass!!.isInstance(it) }
}

private suspend fun doValidate(project: Project, state: ZLSSettings): Boolean {
    val zlsPath: Path = state.zlsPath.let { zlsPath ->
        if (zlsPath.isEmpty()) {
            val env = if (project.zigProjectSettings.state.direnv) project.getDirenv() else emptyEnv
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