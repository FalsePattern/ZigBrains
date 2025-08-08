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

package com.falsepattern.zigbrains.project.buildscan

import com.falsepattern.zigbrains.project.buildscan.ZigBuildScanListener.ErrorType
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainService
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.vfs.toNioPathOrNull
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.net.ServerSocket
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.writeBytes

@Service(Service.Level.PROJECT)
class ZigBuildScannerService(private val project: Project) {
	private val reloading = AtomicBoolean(false)
	private val reloadScheduled = AtomicBoolean(false)
	private val reloadMutex = Mutex()
	private var currentTimeoutSec = DEFAULT_TIMEOUT_SEC
	private val listeners = ArrayList<ZigBuildScanListener>()
	private val listenerMutex = Mutex()

	private val projects: MutableList<Serialization.Project> = ArrayList()

	init {
		this.listeners += object : ZigBuildScanListener {
			override suspend fun errorReload(type: ErrorType, details: String?) {
				Notifications.Bus.notify(Notification(
					GROUP_DISPLAY_ID,
					"Failed to run build scan",
					"Output:\n${details}",
					NotificationType.ERROR
				))
			}

			override suspend fun postReload(projects: List<Serialization.Project>) {
				withEDTContext(ModalityState.defaultModalityState()) {
					MessageDialogBuilder.okCancel( "notice", "Output:\n${projects}" ).ask( project )
				}
			}
		}
	}

	suspend fun register(listener: ZigBuildScanListener): Disposable {
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

	@Suppress("UnstableApiUsage")
	@OptIn(ExperimentalSerializationApi::class, ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
	private tailrec suspend fun doReload() {
		preReload()
		// get the zig toolchain for this project
		val toolchain = ZigToolchainService.getInstance(project).toolchain ?: run {
			errorReload(ErrorType.MissingToolchain)
			return
		}
		val zig = toolchain.zig

		// get project dir
		val workDir = project.guessProjectDir()?.toNioPathOrNull() ?: run {
			errorReload(ErrorType.GeneralError)
			return
		}

		// copy the helper
		writeAction {
			runCatching {
				ZigBuildScannerService::class.java.getResourceAsStream("/fileTemplates/internal/builder.zig")?.use {
					workDir.resolve(HELPER_NAME).writeBytes(it.readAllBytes())
				}
			}
		}.getOrElse { throwable ->
			errorReload(ErrorType.FailedToCopyHelper, throwable.message)
			return
		}

		// start socket server
		val server = withContext(Dispatchers.IO) { ServerSocket(0) }
        val projectsFuture = zigCoroutineScope.async(newSingleThreadContext("ZigBrains network thread")) {
            runInterruptible {
                server.accept().use {
					Json.decodeFromStream<List<Serialization.Project>>( it.getInputStream() )
				}
            }
        }

		val result = zig.callWithArgs(
			workDir,
			"build", "--build-file", HELPER_NAME, "-l",
			timeoutMillis = currentTimeoutSec * 1000L,
			ipcProject = project,
			env = mapOf( "ZIGBRAINS_PORT" to "${server.localPort}" )
		).getOrElse { throwable ->
			errorReload(ErrorType.MissingZigExe, throwable.message)
			null
		}

        withTimeoutOrNull(2000) {
            val newProjects = projectsFuture.await()
            projects.clear()
            projects.addAll(newProjects)
        } ?: run {
            projectsFuture.cancel()
            //TODO handle failed reload
        }

		// delete the helper
		withContext(Dispatchers.IO) {
			Files.deleteIfExists(workDir.resolve(HELPER_NAME))
		}

		when {
			result == null -> {}
			result.checkSuccess(LOG) -> {
				currentTimeoutSec = DEFAULT_TIMEOUT_SEC
				postReload()
			}
			result.isTimeout -> {
				timeoutReload(currentTimeoutSec)
				currentTimeoutSec *= 2
			}
			result.stderrLines.any { it.contains("error: no build.zig file found") } -> {
				errorReload(ErrorType.MissingBuildZig, result.stderr)
			}
			else -> errorReload(ErrorType.GeneralError, result.stderr)
		}

		val needRepeat = reloadMutex.withLock {
			if (reloadScheduled.getAndSet(false)) {
				return@withLock true
			}
			reloading.set(false)
			return@withLock false
		}
		if (needRepeat) {
			doReload()
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

	private suspend fun dispatchReload() {
		withEDTContext(ModalityState.defaultModalityState()) {
			FileDocumentManager.getInstance().saveAllDocuments()
		}
		doReload()
	}

	private suspend fun preReload() {
		listenerMutex.withLock {
			listeners.forEach { it.preReload() }
		}
	}

	private suspend fun postReload() {
		listenerMutex.withLock {
			listeners.forEach { it.postReload(projects) }
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

	companion object {
		private const val GROUP_DISPLAY_ID = "zigbrains-buildscan"
		private const val HELPER_NAME = ".zbscanhelper"
		private const val DEFAULT_TIMEOUT_SEC = 32
		private val LOG = Logger.getInstance(ZigBuildScannerService::class.java)
	}
}

val Project.zigBuildScan get() = service<ZigBuildScannerService>()
