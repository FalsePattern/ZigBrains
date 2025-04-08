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

package com.falsepattern.zigbrains.project.toolchain.downloader

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.shared.Unarchiver
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.platform.util.progress.*
import com.intellij.util.asSafely
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.createDirectories
import com.intellij.util.io.delete
import com.intellij.util.io.move
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import com.intellij.util.text.SemVer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.isDirectory
import kotlin.io.path.name

@JvmRecord
data class ZigVersionInfo(
    val version: SemVer,
    val date: String,
    val docs: String,
    val notes: String,
    val src: Tarball?,
    val dist: Tarball
) {
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

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        suspend fun downloadVersionList(): List<ZigVersionInfo> {
            return withContext(Dispatchers.IO) {
                val service = DownloadableFileService.getInstance()
                val tempFile = FileUtil.createTempFile(tempPluginDir, "index", ".json", false, false)
                val desc = service.createFileDescription("https://ziglang.org/download/index.json", tempFile.name)
                val downloader = service.createDownloader(listOf(desc), ZigBrainsBundle.message("settings.toolchain.downloader.service.index"))
                val downloadResults = coroutineToIndicator {
                    downloader.download(tempPluginDir)
                }
                if (downloadResults.isEmpty())
                    return@withContext emptyList()
                val index = downloadResults[0].first
                val info = index.inputStream().use { Json.decodeFromStream<JsonObject>(it) }
                index.delete()
                return@withContext info.mapNotNull { (version, data) -> parseVersion(version, data) }.toList()
            }
        }
    }

    @JvmRecord
    @Serializable
    data class Tarball(val tarball: String, val shasum: String, val size: Int)
}

private suspend fun downloadTarball(dist: ZigVersionInfo.Tarball, into: Path, reporter: ProgressReporter): Path {
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

private suspend fun flattenDownloadDir(dir: Path, reporter: ProgressReporter) {
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
private suspend fun unpackTarball(tarball: Path, into: Path, reporter: ProgressReporter) {
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

private fun parseVersion(versionKey: String, data: JsonElement): ZigVersionInfo? {
    if (data !is JsonObject)
        return null

    val versionTag = data["version"]?.asSafely<JsonPrimitive>()?.content

    val version = SemVer.parseFromText(versionTag) ?: SemVer.parseFromText(versionKey)
                  ?: return null
    val date = data["date"]?.asSafely<JsonPrimitive>()?.content ?: ""
    val docs = data["docs"]?.asSafely<JsonPrimitive>()?.content ?: ""
    val notes = data["notes"]?.asSafely<JsonPrimitive>()?.content ?: ""
    val src = data["src"]?.asSafely<JsonObject>()?.let { Json.decodeFromJsonElement<ZigVersionInfo.Tarball>(it) }
    val dist = data.firstNotNullOfOrNull { (dist, tb) -> getTarballIfCompatible(dist, tb) }
               ?: return null


    return ZigVersionInfo(version, date, docs, notes, src, dist)
}

private fun getTarballIfCompatible(dist: String, tb: JsonElement): ZigVersionInfo.Tarball? {
    if (!dist.contains('-'))
        return null
    val (arch, os) = dist.split('-', limit = 2)
    val theArch = when (arch) {
        "x86_64" -> CpuArch.X86_64
        "i386" -> CpuArch.X86
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
    return Json.decodeFromJsonElement<ZigVersionInfo.Tarball>(tb)
}

private val tempPluginDir get(): File = PathManager.getTempPath().toNioPathOrNull()!!.resolve("zigbrains").toFile()