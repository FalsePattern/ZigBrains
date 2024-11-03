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

package com.falsepattern.zigbrains.debugger.toolchain

import com.falsepattern.zigbrains.debugger.ZigDebugBundle
import com.falsepattern.zigbrains.shared.coroutine.runInterruptibleEDT
import com.falsepattern.zigbrains.shared.coroutine.runModalOrBlocking
import com.intellij.execution.ExecutionModes.ModalProgressMode
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.application
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.Decompressor
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import com.jetbrains.cidr.execution.debugger.CidrDebuggerPathManager
import com.jetbrains.cidr.execution.debugger.backend.bin.UrlProvider
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.name
import kotlin.io.path.notExists

@Service
class ZigDebuggerToolchainService {
    suspend fun debuggerAvailability(kind: DebuggerKind): DebuggerAvailability<*> {
        return when(kind) {
            DebuggerKind.LLDB -> lldbAvailability()
            DebuggerKind.GDB -> gdbAvailability()
            DebuggerKind.MSVC -> msvcAvailability()
        }
    }

    fun lldbAvailability(): DebuggerAvailability<LLDBBinaries> {
//        if (LLDBDriverConfiguration.hasBundledLLDB()) return DebuggerAvailability.Bundled

        val (frameworkPath, frontendPath) = when {
            SystemInfo.isMac -> "LLDB.framework" to "LLDBFrontend"
            SystemInfo.isUnix -> "lib/liblldb.so" to "bin/LLDBFrontend"
            SystemInfo.isWindows -> "bin/liblldb.dll" to "bin/LLDBFrontend.exe"
            else -> return DebuggerAvailability.Unavailable
        }

        val lldbPath = lldbPath()
        val frameworkFile = lldbPath.resolve(frameworkPath)
        val frontendFile = lldbPath.resolve(frontendPath)
        if (frameworkFile.notExists() || frontendFile.notExists()) return DebuggerAvailability.NeedToDownload

        val versions = loadDebuggerVersions(DebuggerKind.LLDB)
        val (lldbFrameworkUrl, lldbFrontendUrl) = lldbUrls() ?: return DebuggerAvailability.Unavailable

        val lldbFrameworkVersion = fileNameWithoutExtension(lldbFrameworkUrl.toString())
        val lldbFrontendVersion = fileNameWithoutExtension(lldbFrontendUrl.toString())

        if (versions[LLDB_FRAMEWORK_PROPERTY_NAME] != lldbFrameworkVersion ||
            versions[LLDB_FRONTEND_PROPERTY_NAME] != lldbFrontendVersion) return DebuggerAvailability.NeedToUpdate

        return DebuggerAvailability.Binaries(LLDBBinaries(frameworkFile, frontendFile))
    }

    fun gdbAvailability(): DebuggerAvailability<GDBBinaries> {
        if (SystemInfo.isMac) return DebuggerAvailability.Unavailable
//        if (CidrDebuggerPathManager.getBundledGDBBinary().exists()) return DebuggerAvailability.Bundled

        val gdbBinaryPath = when {
            SystemInfo.isUnix -> "bin/gdb"
            SystemInfo.isWindows -> "bin/gdb.exe"
            else -> return DebuggerAvailability.Unavailable
        }

        val gdbFile = gdbPath().resolve(gdbBinaryPath)
        if (gdbFile.notExists()) return DebuggerAvailability.NeedToDownload

        val versions = loadDebuggerVersions(DebuggerKind.GDB)
        val gdbUrl = gdbUrl() ?: return DebuggerAvailability.Unavailable

        val gdbVersion = fileNameWithoutExtension(gdbUrl.toString())

        if (versions[GDB_PROPERTY_NAME] != gdbVersion) return DebuggerAvailability.NeedToUpdate

        return DebuggerAvailability.Binaries(GDBBinaries(gdbFile))
    }

    suspend fun msvcAvailability(): DebuggerAvailability<MSVCBinaries> {
//        if (!SystemInfo.isWindows) return DebuggerAvailability.Unavailable

        val msvcBinaryPath = "vsdbg.exe"

        val msvcFile = msvcPath().resolve(msvcBinaryPath)
        if (msvcFile.notExists()) return DebuggerAvailability.NeedToDownload

        val msvcUrl = msvcUrl() ?: return DebuggerAvailability.Binaries(MSVCBinaries(msvcFile))

        val versions = loadDebuggerVersions(DebuggerKind.MSVC)

        if (versions[MSVC_PROPERTY_NAME] != msvcUrl.version) return DebuggerAvailability.NeedToUpdate

        return DebuggerAvailability.Binaries(MSVCBinaries(msvcFile))
    }

    @RequiresEdt
    private suspend fun doDownloadDebugger(project: Project? = null, debuggerKind: DebuggerKind): DownloadResult {
        val baseDir = debuggerKind.basePath()
        val downloadableBinaries = when(debuggerKind) {
            DebuggerKind.LLDB -> {
                val (lldbFrameworkUrl, lldbFrontendUrl) = lldbUrls()?.run { first.toString() to second.toString() } ?: return DownloadResult.NoUrls
                listOf(
                    DownloadableDebuggerBinary(lldbFrameworkUrl, LLDB_FRAMEWORK_PROPERTY_NAME, fileNameWithoutExtension(lldbFrameworkUrl)),
                    DownloadableDebuggerBinary(lldbFrontendUrl, LLDB_FRONTEND_PROPERTY_NAME, fileNameWithoutExtension(lldbFrontendUrl))
                )
            }
            DebuggerKind.GDB -> {
                val gdbUrl = gdbUrl()?.run { toString() } ?: return DownloadResult.NoUrls
                listOf(DownloadableDebuggerBinary(gdbUrl, GDB_PROPERTY_NAME, fileNameWithoutExtension(gdbUrl)))
            }
            DebuggerKind.MSVC -> {
                val msvcUrl = msvcUrl() ?: return DownloadResult.NoUrls

                val dialog = DialogBuilder()
                dialog.setTitle(msvcUrl.dialogTitle)
                dialog.addCancelAction().setText("Reject")
                dialog.addOkAction().setText("Accept")
                val centerPanel = JBPanel<JBPanel<*>>()
                val hyperlink = HyperlinkLabel()
                hyperlink.setTextWithHyperlink(msvcUrl.dialogBody)
                hyperlink.setHyperlinkText(msvcUrl.dialogLink)
                hyperlink.addHyperlinkListener(BrowserHyperlinkListener())
                centerPanel.add(hyperlink)
                dialog.centerPanel(centerPanel)
                if (!dialog.showAndGet()) return DownloadResult.NoUrls

                listOf(DownloadableDebuggerBinary(msvcUrl.url, MSVC_PROPERTY_NAME, msvcUrl.version, "extension/debugAdapters/vsdbg/bin"))
            }
        }

        try {
            downloadAndUnArchive(baseDir, downloadableBinaries)
            return DownloadResult.Ok(baseDir)
        } catch (e: IOException) {
            //TODO logging
            e.printStackTrace()
            return DownloadResult.Failed(e.message)
        }
    }

    @RequiresEdt
    suspend fun downloadDebugger(project: Project? = null, debuggerKind: DebuggerKind): DownloadResult {
        val result = doDownloadDebugger(project, debuggerKind)

        when(result) {
            is DownloadResult.Ok -> {
                Notification(
                    "zigbrains",
                    ZigDebugBundle.message("notification.title.debugger"),
                    ZigDebugBundle.message("notification.content.debugger.successfully.downloaded"),
                    NotificationType.INFORMATION
                ).notify(project)
            }
            is DownloadResult.Failed -> {
                Notification(
                    "zigbrains",
                    ZigDebugBundle.message("notification.title.debugger"),
                    ZigDebugBundle.message("notification.content.debugger.downloading.failed"),
                    NotificationType.ERROR
                ).notify(project)
            }
            else -> Unit
        }

        return result
    }

    @Throws(IOException::class)
    @RequiresEdt
    private suspend fun downloadAndUnArchive(baseDir: Path, binariesToDownload: List<DownloadableDebuggerBinary>) {
        val service = DownloadableFileService.getInstance()

        val downloadDir = baseDir.toFile()
        downloadDir.deleteRecursively()

        val descriptions = binariesToDownload.map {
            service.createFileDescription(it.url, fileName(it.url))
        }

        val downloader = service.createDownloader(descriptions, "Debugger downloading")
        val downloadDirectory = downloadPath().toFile()
        val downloadResults = withContext(Dispatchers.IO) {
            coroutineToIndicator {
                downloader.download(downloadDirectory)
            }
        }
        val versions = Properties()
        for (result in downloadResults) {
            val downloadUrl = result.second.downloadUrl
            val binaryToDownload = binariesToDownload.first { it.url == downloadUrl }
            val propertyName = binaryToDownload.propertyName
            val archiveFile = result.first
            Unarchiver.unarchive(archiveFile, downloadDir)
            archiveFile.delete()
            versions[propertyName] = binaryToDownload.version
        }

        saveVersionsFile(baseDir, versions)
    }

    private fun lldbUrls(): Pair<URL, URL>? {
        val lldb = UrlProvider.lldb(OS.CURRENT, CpuArch.CURRENT) ?: return null
        val lldbFrontend = UrlProvider.lldbFrontend(OS.CURRENT, CpuArch.CURRENT) ?: return null
        return lldb to lldbFrontend
    }

    private fun gdbUrl(): URL? = UrlProvider.gdb(OS.CURRENT, CpuArch.CURRENT)

    private suspend fun msvcUrl(): MSVCUrl? {
        val dlKey = when(CpuArch.CURRENT) {
            CpuArch.X86 -> "downloadX86"
            CpuArch.X86_64 -> "downloadX86_64"
            CpuArch.ARM64 -> "downloadARM64"
            else -> return null
        }

        val props = msvcMetadata()
        val version = props.getProperty("version") ?: return null
        val url = props.getProperty(dlKey) ?: return null
        return MSVCUrl(url, version, props.getProperty("dialogTitle")!!, props.getProperty("dialogBody")!!, props.getProperty("dialogLink")!!)
    }

    private data class MSVCUrl(
        val url: String,
        val version: String,
        val dialogTitle: String,
        val dialogBody: String,
        val dialogLink: String
    )

    private fun loadDebuggerVersions(kind: DebuggerKind): Properties = loadVersions(kind.basePath())

    private fun saveVersionsFile(basePath: Path, versions: Properties) {
        val file = basePath.resolve(DEBUGGER_VERSIONS).toFile()
        try {
            versions.store(file.bufferedWriter(), "")
        } catch (e: IOException) {
            LOG.warn("Failed to save `${basePath.name}/${file.name}`", e)
        }
    }

    private fun loadVersions(basePath: Path): Properties {
        val versions = Properties()
        val versionsFile = basePath.resolve(DEBUGGER_VERSIONS).toFile()

        if (versionsFile.exists()) {
            try {
                versionsFile.bufferedReader().use { versions.load(it) }
            } catch (e: IOException) {
                LOG.warn("Failed to load `${basePath.name}/${versionsFile.name}`", e)
            }
        }

        return versions
    }

    private fun DebuggerKind.basePath(): Path {
        return when(this) {
            DebuggerKind.LLDB -> lldbPath()
            DebuggerKind.GDB -> gdbPath()
            DebuggerKind.MSVC -> msvcPath()
        }
    }

    companion object {
        private val LOG = logger<ZigDebuggerToolchainService>()

        private const val DEBUGGER_VERSIONS: String = "versions.properties"

        private const val LLDB_FRONTEND_PROPERTY_NAME = "lldbFrontend"
        private const val LLDB_FRAMEWORK_PROPERTY_NAME = "lldbFramework"
        private const val GDB_PROPERTY_NAME = "gdb"
        private const val MSVC_PROPERTY_NAME = "msvc"

        fun downloadPath() = tempPluginDir
        private fun lldbPath() = pluginDir.resolve("lldb")
        private fun gdbPath() = pluginDir.resolve("gdb")
        private fun msvcPath() = pluginDir.resolve("msvc")

        private val pluginDir get() = PathManager.getSystemDir().resolve("zigbrains")

        private val tempPluginDir get(): Path = PathManager.getTempPath().toNioPathOrNull()!!.resolve("zigbrains")

        private fun fileNameWithoutExtension(url: String): String {
            return url.substringAfterLast("/").removeSuffix(".zip").removeSuffix(".tar.gz")
        }

        private fun fileName(url: String): String {
            return url.substringAfterLast("/")
        }
    }

    private enum class Unarchiver {
        ZIP {
            override val extension = "zip"
            override fun createDecompressor(file: File) = Decompressor.Zip(file)
        },
        TAR {
            override val extension = "tar.gz"
            override fun createDecompressor(file: File) = Decompressor.Tar(file)
        },
        VSIX {
            override val extension = "vsix"
            override fun createDecompressor(file: File) = Decompressor.Zip(file)
        };

        protected abstract val extension: String
        protected abstract fun createDecompressor(file: File): Decompressor

        companion object {
            @Throws(IOException::class)
            suspend fun unarchive(archivePath: File, dst: File, prefix: String? = null) {
                runInterruptible {
                    val unarchiver = entries.find { archivePath.name.endsWith(it.extension) } ?: error("Unexpected archive type: $archivePath")
                    val dec = unarchiver.createDecompressor(archivePath)
                    if (prefix != null) {
                        dec.removePrefixPath(prefix)
                    }
                    dec.extract(dst)
                }
            }
        }
    }

    sealed class DownloadResult {
        class Ok(val baseDir: Path): DownloadResult()
        data object NoUrls: DownloadResult()
        class Failed(val message: String?): DownloadResult()
    }

    @JvmRecord
    private data class DownloadableDebuggerBinary(val url: String, val propertyName: String, val version: String, val prefix: String? = null)
}

val zigDebuggerToolchainService get() = application.service<ZigDebuggerToolchainService>()
