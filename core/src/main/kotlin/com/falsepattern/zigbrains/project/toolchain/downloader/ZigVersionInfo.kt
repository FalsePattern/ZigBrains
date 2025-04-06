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

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.platform.util.progress.withProgressText
import com.intellij.util.asSafely
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
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
import java.nio.file.Path

@JvmRecord
data class ZigVersionInfo(val date: String, val docs: String, val notes: String, val src: Tarball?, val dist: Tarball) {
    companion object {
        suspend fun download(): List<Pair<String, ZigVersionInfo>> {
            return withProgressText("Fetching zig version information") {
                withContext(Dispatchers.IO) {
                    doDownload()
                }
            }
        }

        @OptIn(ExperimentalSerializationApi::class)
        private suspend fun doDownload(): List<Pair<String, ZigVersionInfo>> {
            val service = DownloadableFileService.getInstance()
            val desc = service.createFileDescription("https://ziglang.org/download/index.json", "index.json")
            val downloader = service.createDownloader(listOf(desc), "Zig version information downloading")
            val downloadDirectory = tempPluginDir.toFile()
            val downloadResults = coroutineToIndicator {
                downloader.download(downloadDirectory)
            }
            var info: JsonObject? = null
            for (result in downloadResults) {
                if (result.second.defaultFileName == "index.json") {
                    info = result.first.inputStream().use { Json.decodeFromStream<JsonObject>(it) }
                }
            }
            return info?.mapNotNull { (version, data) -> parseVersion(data)?.let { Pair(version, it) } }?.toList() ?: emptyList()
        }

        private fun parseVersion(data: JsonElement): ZigVersionInfo? {
            data as? JsonObject ?: return null
            val date = data["date"]?.asSafely<JsonPrimitive>()?.content ?: ""
            val docs = data["docs"]?.asSafely<JsonPrimitive>()?.content ?: ""
            val notes = data["notes"]?.asSafely<JsonPrimitive>()?.content ?: ""
            val src = data["src"]?.asSafely<JsonObject>()?.let { Json.decodeFromJsonElement<Tarball>(it) }
            val dist = data.firstNotNullOfOrNull { (dist, tb) -> getTarballIfCompatible(dist, tb) } ?: return null

            return ZigVersionInfo(date, docs, notes, src, dist)
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

private val tempPluginDir get(): Path = PathManager.getTempPath().toNioPathOrNull()!!.resolve("zigbrains")