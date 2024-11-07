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

package com.falsepattern.zigbrains.lsp

import com.falsepattern.zigbrains.lsp.settings.zlsSettings
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
                return features.project.zlsSettings.state.inlayHints
            }
        }
        return features
    }

    override fun isEnabled(project: Project): Boolean {
        return (project.getUserData(ENABLED_KEY) ?: true) && project.zlsSettings.validate()
    }

    override fun setEnabled(enabled: Boolean, project: Project) {
        project.putUserData(ENABLED_KEY, enabled)
    }
}

class ZLSStarter: LanguageServerStarter {
    override fun startLSP(project: Project, restart: Boolean) {
        project.zigCoroutineScope.launch {
            val manager = project.service<LanguageServerManager>()
            val status = manager.getServerStatus("ZigBrains")
            if ((status == ServerStatus.started || status == ServerStatus.starting) && !restart)
                return@launch
            manager.start("ZigBrains")
        }
    }

}

private val ENABLED_KEY = Key.create<Boolean>("ZLS_ENABLED")