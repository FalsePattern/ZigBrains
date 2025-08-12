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
import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.RootsChangeRescanningInfo
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.vfs.toNioPathOrNull
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.net.ServerSocket
import java.nio.file.StandardOpenOption
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

@Service(Service.Level.PROJECT)
@State(
	name = "BuildScanner",
	storages = [Storage("zigbrains.xml")]
)
class ZigBuildScannerService(private val project: Project): SerializablePersistentStateComponent<ZigBuildScannerService.State>(State()) {
	private val reloading = AtomicBoolean(false)
	private val reloadScheduled = AtomicBoolean(false)
	private val reloadMutex = Mutex()
	private var currentTimeoutSec = DEFAULT_TIMEOUT_SEC
	private val listeners = ArrayList<ZigBuildScanListener>()
	private val listenerMutex = Mutex()

	var projects: List<Serialization.Project>
		get() = this.state.projects
		private set(value) {
			updateState { it.copy(projects = value) }
		}
	var enabled: Boolean
		get() = this.state.enabled
		set(value) {
			updateState { it.copy(enabled = value) }
		}

	private suspend fun reloadProjectRoots() {
		@Suppress("UnstableApiUsage")
		writeAction {
			ProjectRootManagerEx.getInstanceEx(project)
				.makeRootsChange(EmptyRunnable.getInstance(), RootsChangeRescanningInfo.RESCAN_DEPENDENCIES_IF_NEEDED)
		}
	}

	suspend fun register(listener: ZigBuildScanListener): Disposable {
		return listenerMutex.withLock {
			listeners.add(listener)
			// HACK: if we already scanned, but the listener is registered too late, invoke its postReload
			val projects = this.projects
			if (projects.isNotEmpty()) {
				listener.postReload(projects)
			}
			Disposable {
				zigCoroutineScope.launch {
					listenerMutex.withLock {
						listeners.remove(listener)
					}
				}
			}
		}
	}

	@OptIn(ExperimentalSerializationApi::class, ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
	private suspend fun doReload(allowCache: Boolean): List<Serialization.Project> {
		// be sure we're enabled and running in a trusted environment
		if (!enabled || !TrustedProjects.isProjectTrusted(project) || project.isDefault) {
			return emptyList()
		}

		preReload()
		// get the zig toolchain for this project
		val toolchain = ZigToolchainService.getInstance(project).toolchain ?: run {
			errorReload(ErrorType.MissingToolchain)
			return emptyList()
		}
		val zig = toolchain.zig

		// get project dir
		val workDir = project.guessProjectDir()?.toNioPathOrNull() ?: run {
			errorReload(ErrorType.GeneralError)
			return emptyList()
		}

		val helperPath = workDir.resolve(HELPER_NAME)

		// copy the helper
		runCatching {
			ZigBuildScannerService::class.java.getResourceAsStream("/fileTemplates/internal/builder.zig")?.use {
				it.transferTo(helperPath.outputStream(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))
			}
		}.getOrElse { throwable ->
			errorReload(ErrorType.FailedToCopyHelper, throwable.message)
			return emptyList()
		}

		//Virtual threads make NIO non-blocking
		val (result, projects) = Executors.newVirtualThreadPerTaskExecutor().use { executor ->
			val dispatcher = executor.asCoroutineDispatcher()
			// start socket server
			val server = withContext(dispatcher) { ServerSocket(0) }
			val projectsFuture = project.zigCoroutineScope.async(dispatcher) {
				runCatching {
					runInterruptible {
						server.accept().use {
							Json.decodeFromStream<List<Serialization.Project>>( it.getInputStream() )
						}
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

			val projects = withTimeoutOrNull(2000) {
				projectsFuture.await().getOrElse { throwable ->
					errorReload(ErrorType.GeneralError, throwable.message)
					emptyList()
				}
			} ?: run {
				projectsFuture.cancel()
				errorReload(ErrorType.GeneralError, "Build scan data transfer socket timed out")
				emptyList()
			}
			result to projects
		}

		// delete the helper
		helperPath.deleteIfExists()

		when {
			result == null -> {}
			result.checkSuccess(LOG) -> {
				currentTimeoutSec = DEFAULT_TIMEOUT_SEC
				postReload(projects)
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
		return projects
	}

	fun triggerReload(allowCache: Boolean = false) {
		project.zigCoroutineScope.launch {
			reloadMutex.withLock {
				if (reloading.getAndSet(true)) {
					reloadScheduled.set(true)
					return@launch
				}
			}
			try {
				dispatchReload(allowCache)
			} finally {
				reloadProjectRoots()
			}
		}
	}

	private tailrec suspend fun dispatchReload(allowCache: Boolean) {
		withEDTContext(ModalityState.defaultModalityState()) {
			writeAction {
				FileDocumentManager.getInstance().saveAllDocuments()
			}
		}
		projects = doReload(allowCache)
		val needRepeat = reloadMutex.withLock {
			if (reloadScheduled.getAndSet(false)) {
				return@withLock true
			}
			reloading.set(false)
			return@withLock false
		}
		if (needRepeat) {
			dispatchReload(allowCache)
		}
	}

	private suspend fun preReload() {
		listenerMutex.withLock {
			listeners.forEach { it.preReload() }
		}
	}

	private suspend fun postReload(projects: List<Serialization.Project>) {
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
		private const val HELPER_NAME = ".zbscanhelper.zig"
		private const val DEFAULT_TIMEOUT_SEC = 32
		private val LOG = Logger.getInstance(ZigBuildScannerService::class.java)
	}

	data class State(
		@JvmField val enabled: Boolean = true,
		@JvmField val projects: List<Serialization.Project> = listOf()
	)
}

val Project.zigBuildScan get() = service<ZigBuildScannerService>()