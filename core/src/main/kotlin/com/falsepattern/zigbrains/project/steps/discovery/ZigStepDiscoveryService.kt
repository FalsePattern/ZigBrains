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

package com.falsepattern.zigbrains.project.steps.discovery

import com.falsepattern.zigbrains.project.settings.zigProjectSettings
import com.falsepattern.zigbrains.project.steps.discovery.ZigStepDiscoveryListener.ErrorType
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.toNioPathOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class ZigStepDiscoveryService(private val project: Project) {
    private val reloading = AtomicBoolean(false)
    private val reloadScheduled = AtomicBoolean(false)
    private val reloadMutex = Mutex()
    private var currentTimeoutSec = DEFAULT_TIMEOUT_SEC
    private val listeners = ArrayList<ZigStepDiscoveryListener>()
    private val listenerMutex = Mutex()

    suspend fun register(listener: ZigStepDiscoveryListener): Disposable {
        return listenerMutex.withLock {
            listeners.add(listener)
            Disposable {
                zigCoroutineScope.launch {
                    listenerMutex.withLock {
                        listeners.remove(listener)
                    }
                }
            }
        }
    }

    fun triggerReload() {
        project.zigCoroutineScope.launch {
            reloadMutex.withLock {
                if (reloading.getAndSet(true)) {
                    reloadScheduled.set(true)
                    return@launch
                }
            }
            dispatchReload()
        }
    }

    private tailrec suspend fun doReload() {
        preReload()
        val toolchain = project.zigProjectSettings.state.toolchain ?: run {
            errorReload(ErrorType.MissingToolchain)
            return
        }
        val zig = toolchain.zig
        val result = zig.callWithArgs(
            project.guessProjectDir()?.toNioPathOrNull(),
            "build", "-l",
            timeoutMillis = currentTimeoutSec * 1000L
        )
        if (result.checkSuccess(LOG)) {
            currentTimeoutSec = DEFAULT_TIMEOUT_SEC
            val lines = result.stdoutLines
            val steps = ArrayList<Pair<String, String?>>()
            for (line in lines) {
                val parts = line.trim().split(SPACES, 2)
                if (parts.size == 2) {
                    steps.add(Pair(parts[0], parts[1]))
                } else {
                    steps.add(Pair(parts[0], null))
                }
            }
            postReload(steps)
        } else if (result.isTimeout) {
            timeoutReload(currentTimeoutSec)
            currentTimeoutSec *= 2
        } else if (result.stderrLines.any { it.contains("error: no build.zig file found, in the current directory or any parent directories") }) {
            errorReload(ErrorType.MissingBuildZig, result.stderr)
        } else {
            errorReload(ErrorType.GeneralError, result.stderr)
        }
        if (reloadMutex.withLock {
            if (reloadScheduled.getAndSet(false)) {
                return@withLock true
            }
            reloading.set(false)
            return@withLock false
        }) {
            doReload()
        }
    }

    private suspend fun dispatchReload() {
        withEDTContext {
            FileDocumentManager.getInstance().saveAllDocuments()
        }
        doReload()
    }

    private suspend fun preReload() {
        listenerMutex.withLock {
            listeners.forEach { it.preReload() }
        }
    }

    private suspend fun postReload(steps: List<Pair<String, String?>>) {
        listenerMutex.withLock {
            listeners.forEach { it.postReload(steps) }
        }
    }

    private suspend fun errorReload(type: ErrorType, details: String? = null) {
        listenerMutex.withLock {
            listeners.forEach { it.errorReload(type, details) }
        }
    }

    private suspend fun timeoutReload(seconds: Int) {
        listenerMutex.withLock {
            listeners.forEach { it.timeoutReload(seconds) }
        }
    }
}

val Project.zigStepDiscovery get() = service<ZigStepDiscoveryService>()

private val SPACES = Regex("\\s+")

private const val DEFAULT_TIMEOUT_SEC = 10

private val LOG = Logger.getInstance(ZigStepDiscoveryService::class.java)