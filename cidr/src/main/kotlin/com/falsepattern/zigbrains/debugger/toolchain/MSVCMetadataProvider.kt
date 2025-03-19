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

package com.falsepattern.zigbrains.debugger.toolchain

import com.falsepattern.zigbrains.debugger.ZigDebugBundle
import com.falsepattern.zigbrains.debugger.settings.MSVCDownloadPermission
import com.falsepattern.zigbrains.debugger.settings.ZigDebuggerSettings
import com.falsepattern.zigbrains.debugger.toolchain.ZigDebuggerToolchainService.Companion.downloadPath
import com.falsepattern.zigbrains.shared.coroutine.withCurrentEDTModalityContext
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.platform.util.progress.withProgressText
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.download.DownloadableFileService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.*
import javax.swing.BoxLayout

private val mutex = Mutex()
private var cache: Properties? = null

suspend fun msvcMetadata(): Properties {
    cache?.let { return it }
    mutex.withLock {
        cache?.let { return it }
        val settings = ZigDebuggerSettings.instance
        var permission = settings.msvcConsent
        if (permission == MSVCDownloadPermission.AskMe) {
            val allowDownload = withCurrentEDTModalityContext {
                val dialog = DialogBuilder()
                dialog.setTitle(ZigDebugBundle.message("msvc.consent.title"))
                dialog.addCancelAction().setText(ZigDebugBundle.message("msvc.consent.deny"))
                dialog.addOkAction().setText(ZigDebugBundle.message("msvc.consent.allow"))
                val centerPanel = JBPanel<JBPanel<*>>()
                centerPanel.setLayout(BoxLayout(centerPanel, BoxLayout.Y_AXIS))
                val lines = ZigDebugBundle.message("msvc.consent.body").split('\n')
                for (line in lines) {
                    centerPanel.add(JBLabel(line))
                }
                dialog.centerPanel(centerPanel)
                dialog.showAndGet()
            }
            permission = if (allowDownload) MSVCDownloadPermission.Allow else MSVCDownloadPermission.Deny
            settings.msvcConsent = permission
        }
        val data = if (permission == MSVCDownloadPermission.Allow) {
            withTimeoutOrNull(3000L) {
                downloadMSVCProps()
            } ?: run {
                Notification(
                    "zigbrains",
                    ZigDebugBundle.message("notification.title.debugger"),
                    ZigDebugBundle.message("notification.content.debugger.metadata.downloading.failed"),
                    NotificationType.ERROR
                ).notify(null)
                fetchBuiltinMSVCProps()
            }
        } else {
            fetchBuiltinMSVCProps()
        }
        cache = data
        return data
    }
}

private suspend fun downloadMSVCProps(): Properties {
    return withProgressText("Downloading debugger metadata") {
        val service = DownloadableFileService.getInstance()
        val desc = service.createFileDescription("https://falsepattern.com/zigbrains/msvc.properties", "msvc.properties")
        val downloader = service.createDownloader(listOf(desc), "Debugger metadata downloading")
        val downloadDirectory = downloadPath().toFile()
        val prop = Properties()
        val downloadResults = runBlocking {
            downloader.download(downloadDirectory)
        }
        for (result in downloadResults) {
            if (result.second.defaultFileName == "msvc.properties") {
                result.first.reader().use { prop.load(it) }
            }
        }
        return@withProgressText prop
    }
}

private fun fetchBuiltinMSVCProps(): Properties {
    val prop = Properties()
    try {
        val resource = ZigDebuggerToolchainService::class.java.getResourceAsStream("/msvc.properties") ?: throw IOException("null")
        resource.reader().use { prop.load(it) }
    } catch (ex: IOException) {
        ex.printStackTrace()
        Notification(
            "zigbrains",
            ZigDebugBundle.message("notification.title.debugger"),
            ZigDebugBundle.message("notification.content.debugger.metadata.fallback.parse.failed"),
            NotificationType.ERROR
        ).notify(null)
    }
    return prop
}