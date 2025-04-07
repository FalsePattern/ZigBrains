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

import com.falsepattern.zigbrains.shared.Unarchiver
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.platform.util.progress.reportProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.util.progress.withProgressText
import com.intellij.util.asSafely
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.createDirectories
import com.intellij.util.io.delete
import com.intellij.util.io.move
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import com.intellij.util.text.SemVer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

@JvmRecord
data class ZigVersionInfo(
    val version: SemVer,
    val date: String,
    val docs: String,
    val notes: String,
    val src: Tarball?,
    val dist: Tarball
) {
    suspend fun downloadAndUnpack(into: Path): Boolean {
        return reportProgress { reporter ->
            try {
                into.createDirectories()
            } catch (e: Exception) {
                return@reportProgress false
            }
            val service = DownloadableFileService.getInstance()
            val fileName = dist.tarball.substringAfterLast('/')
            val tempFile = FileUtil.createTempFile(into.toFile(), "tarball", fileName, false, false)
            val desc = service.createFileDescription(dist.tarball, tempFile.name)
            val downloader = service.createDownloader(listOf(desc), "Zig version information downloading")
            val downloadResults = reporter.sizedStep(100) {
                coroutineToIndicator {
                    downloader.download(into.toFile())
                }
            }
            if (downloadResults.isEmpty())
                return@reportProgress false
            val tarball = downloadResults[0].first
            reporter.indeterminateStep("Extracting tarball") {
                Unarchiver.unarchive(tarball.toPath(), into)
                tarball.delete()
                val contents = Files.newDirectoryStream(into).use { it.toList() }
                if (contents.size == 1 && contents[0].isDirectory()) {
                    val src = contents[0]
                    Files.newDirectoryStream(src).use { stream ->
                        stream.forEach {
                            it.move(into.resolve(src.relativize(it)))
                        }
                    }
                    src.delete()
                }
            }
            return@reportProgress true
        }
    }
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        suspend fun downloadVersionList(): List<ZigVersionInfo> {
            val service = DownloadableFileService.getInstance()
            val tempFile = FileUtil.createTempFile(tempPluginDir, "index", ".json", false, false)
            val desc = service.createFileDescription("https://ziglang.org/download/index.json", tempFile.name)
            val downloader = service.createDownloader(listOf(desc), "Zig version information downloading")
            val downloadResults = coroutineToIndicator {
                downloader.download(tempPluginDir)
            }
            if (downloadResults.isEmpty())
                return emptyList()
            val index = downloadResults[0].first
            val info = index.inputStream().use { Json.decodeFromStream<JsonObject>(it) }
            index.delete()
            return info.mapNotNull { (version, data) -> parseVersion(version, data) }.toList()
        }

        private fun parseVersion(versionKey: String, data: JsonElement): ZigVersionInfo? {
            if (data !is JsonObject)
                return null

            val versionTag = data["version"]?.asSafely<JsonPrimitive>()?.content

            val version = SemVer.parseFromText(versionTag) ?: SemVer.parseFromText(versionKey)
                          ?: return null
            val date = data["date"]?.asSafely<JsonPrimitive>()?.content ?: ""
            val docs = data["docs"]?.asSafely<JsonPrimitive>()?.content ?: ""
            val notes = data["notes"]?.asSafely<JsonPrimitive>()?.content?: ""
            val src = data["src"]?.asSafely<JsonObject>()?.let { Json.decodeFromJsonElement<Tarball>(it) }
            val dist = data.firstNotNullOfOrNull { (dist, tb) -> getTarballIfCompatible(dist, tb) }
                       ?: return null


            return ZigVersionInfo(version, date, docs, notes, src, dist)
        }

        private fun getTarballIfCompatible(dist: String, tb: JsonElement): Tarball? {
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
            return Json.decodeFromJsonElement<Tarball>(tb)
        }
    }
}

@JvmRecord
@Serializable
data class Tarball(val tarball: String, val shasum: String, val size: Int)

private val tempPluginDir get(): File = PathManager.getTempPath().toNioPathOrNull()!!.resolve("zigbrains").toFile()