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
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.psi.PsiFile
import com.intellij.util.application
import com.redhat.devtools.lsp4ij.LanguageServerEnablementSupport
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.LanguageServerManager
import com.redhat.devtools.lsp4ij.ServerStatus
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import com.redhat.devtools.lsp4ij.client.features.LSPFormattingFeature
import com.redhat.devtools.lsp4ij.client.features.LSPInlayHintFeature
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ZigLanguageServerFactory: LanguageServerFactory, LanguageServerEnablementSupport {
    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        return if (application.isDispatchThread) {
            runWithModalProgressBlocking(ModalTaskOwner.project(project), ZLSBundle.message("progress.title.create-connection-provider")) {
                ZLSStreamConnectionProvider.create(project)
            }
        } else {
            runBlocking {
                ZLSStreamConnectionProvider.create(project)
            }
        }
    }

    @Suppress("UnstableApiUsage")
    override fun createClientFeatures(): LSPClientFeatures {
        val features = LSPClientFeatures()
        features.formattingFeature = object: LSPFormattingFeature() {
            override fun isExistingFormatterOverrideable(file: PsiFile): Boolean {
                return true
            }
        }
        features.inlayHintFeature = object: LSPInlayHintFeature() {
            override fun isEnabled(file: PsiFile): Boolean {
                val settings = project.zls?.settings ?: return false
                if (!settings.inlayHints)
                    return false
                val maxFileSizeKb = settings.inlayHintsMaxFileSizeKb
                if (maxFileSizeKb == 0)
                    return true
                val fileSizeKb = file.fileDocument.textLength / 1024
                return fileSizeKb <= maxFileSizeKb
            }
        }
        return features
    }

    override fun isEnabled(project: Project) = project.zlsEnabled()

    override fun setEnabled(enabled: Boolean, project: Project) {
        project.zlsEnabled(enabled)
    }
}

fun Project.zlsEnabled(): Boolean {
    return (getUserData(ENABLED_KEY) != false) && zls?.isValid() == true
}

fun Project.zlsEnabled(value: Boolean) {
    putUserData(ENABLED_KEY, value)
}

fun Project.zlsRunning(): Boolean {
    if (!zlsEnabled())
        return false
    return lsm.isRunning
}

private val Project.lsm get() = service<LanguageServerManager>()

private val LanguageServerManager.isRunning get(): Boolean {
    val status = getServerStatus("ZigBrains")
    return status == ServerStatus.started || status == ServerStatus.starting
}

private val START_MUTEX = Mutex()

class ZLSStarter: LanguageServerStarter {
    override fun startLSP(project: Project, restart: Boolean) {
        project.zigCoroutineScope.launch {
            START_MUTEX.withLock {
                if (restart) {
                    project.putUserData(RESTART_KEY, Unit)
                } else {
                    project.putUserData(START_KEY, Unit)
                }
            }
        }
    }
}

private suspend fun doStart(project: Project, restart: Boolean) {
    if (!restart && project.lsm.isRunning)
        return
    while (!project.isDisposed && project.lsm.isRunning) {
        project.lsm.stop("ZigBrains")
        delay(250)
    }
    if (project.zls?.isValid() == true) {
        delay(250)
        project.lsm.start("ZigBrains")
    }
}

suspend fun handleStartLSP(project: Project) = START_MUTEX.withLock {
    if (project.getUserData(RESTART_KEY) != null) {
        project.putUserData(RESTART_KEY, null)
        project.putUserData(START_KEY, null)
        doStart(project, true)
        true
    } else if (project.getUserData(START_KEY) != null) {
        project.putUserData(START_KEY, null)
        doStart(project, false)
        true
    } else {
        false
    }
}

private val ENABLED_KEY = Key.create<Boolean>("ZLS_ENABLED")

private val RESTART_KEY = Key.create<Unit>("ZLS_RESTART_REQUEST")
private val START_KEY = Key.create<Unit>("ZLS_START_REQUEST")
