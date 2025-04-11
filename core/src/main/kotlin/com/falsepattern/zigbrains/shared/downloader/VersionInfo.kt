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

package com.falsepattern.zigbrains.shared.downloader

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.toolchain.downloader.ZigVersionInfo
import com.falsepattern.zigbrains.shared.Unarchiver
import com.falsepattern.zigbrains.shared.downloader.VersionInfo.Tarball
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.platform.util.progress.ProgressReporter
import com.intellij.platform.util.progress.reportProgress
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.createDirectories
import com.intellij.util.io.delete
import com.intellij.util.io.move
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import com.intellij.util.text.SemVer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.isDirectory
import kotlin.io.path.name

interface VersionInfo {
    val version: SemVer
    val date: String
    val dist: Tarball

    @Throws(Exception::class)
    suspend fun downloadAndUnpack(into: Path) {
        reportProgress { reporter ->
            into.createDirectories()
            val tarball = downloadTarball(dist, into, reporter)
            unpackTarball(tarball, into, reporter)
            tarball.delete()
            flattenDownloadDir(into, reporter)
        }
    }

    @JvmRecord
    @Serializable
    data class Tarball(val tarball: String, val shasum: String, val size: Int)
}

suspend fun downloadTarball(dist: Tarball, into: Path, reporter: ProgressReporter): Path {
    return withContext(Dispatchers.IO) {
        val service = DownloadableFileService.getInstance()
        val fileName = dist.tarball.substringAfterLast('/')
        val tempFile = FileUtil.createTempFile(into.toFile(), "tarball", fileName, false, false)
        val desc = service.createFileDescription(dist.tarball, tempFile.name)
        val downloader = service.createDownloader(listOf(desc), ZigBrainsBundle.message("settings.toolchain.downloader.service.tarball"))
        val downloadResults = reporter.sizedStep(100) {
            coroutineToIndicator {
                downloader.download(into.toFile())
            }
        }
        if (downloadResults.isEmpty())
            throw IllegalStateException("No file downloaded")
        return@withContext downloadResults[0].first.toPath()
    }
}

suspend fun flattenDownloadDir(dir: Path, reporter: ProgressReporter) {
    withContext(Dispatchers.IO) {
        val contents = Files.newDirectoryStream(dir).use { it.toList() }
        if (contents.size == 1 && contents[0].isDirectory()) {
            val src = contents[0]
            reporter.indeterminateStep {
                coroutineToIndicator {
                    val indicator = ProgressManager.getInstance().progressIndicator ?: EmptyProgressIndicator()
                    indicator.isIndeterminate = true
                    indicator.text = ZigBrainsBundle.message("settings.toolchain.downloader.progress.flatten")
                    Files.newDirectoryStream(src).use { stream ->
                        stream.forEach {
                            indicator.text2 = it.name
                            it.move(dir.resolve(src.relativize(it)))
                        }
                    }
                }
            }
            src.delete()
        }
    }
}

@OptIn(ExperimentalPathApi::class)
suspend fun unpackTarball(tarball: Path, into: Path, reporter: ProgressReporter) {
    withContext(Dispatchers.IO) {
        try {
            reporter.indeterminateStep {
                coroutineToIndicator {
                    Unarchiver.unarchive(tarball, into)
                }
            }
        } catch (e: Throwable) {
            tarball.delete()
            val contents = Files.newDirectoryStream(into).use { it.toList() }
            if (contents.size == 1 && contents[0].isDirectory()) {
                contents[0].deleteRecursively()
            }
            throw e
        }
    }
}

fun getTarballIfCompatible(dist: String, tb: JsonElement): Tarball? {
    if (!dist.contains('-'))
        return null
    val (arch, os) = dist.split('-', limit = 2)
    val theArch = when (arch) {
        "x86_64" -> CpuArch.X86_64
        "i386", "x86" -> CpuArch.X86
        "armv7a" -> CpuArch.ARM32
        "aarch64" -> CpuArch.ARM64
        else -> return null
    }
    val theOS = when (os) {
        "linux" -> OS.Linux
        "windows" -> OS.Windows
        "macos" -> OS.macOS
        "freebsd" -> OS.FreeBSD
        else -> return null
    }
    if (theArch != CpuArch.CURRENT || theOS != OS.CURRENT) {
        return null
    }
    return Json.decodeFromJsonElement<Tarball>(tb)
}

val tempPluginDir get(): File = PathManager.getTempPath().toNioPathOrNull()!!.resolve("zigbrains").toFile()
