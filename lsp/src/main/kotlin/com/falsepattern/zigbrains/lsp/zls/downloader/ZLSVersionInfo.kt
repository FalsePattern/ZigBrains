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

package com.falsepattern.zigbrains.lsp.zls.downloader

import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.downloader.ZigVersionInfo
import com.falsepattern.zigbrains.shared.downloader.VersionInfo
import com.falsepattern.zigbrains.shared.downloader.VersionInfo.Tarball
import com.falsepattern.zigbrains.shared.downloader.getTarballIfCompatible
import com.falsepattern.zigbrains.shared.downloader.tempPluginDir
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.asSafely
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.text.SemVer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import java.net.URLEncoder

@JvmRecord
data class ZLSVersionInfo(
    override val version: SemVer,
    override val date: String,
    override val dist: Tarball
): VersionInfo {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        suspend fun downloadVersionInfoFor(toolchain: ZigToolchain, project: Project?): List<ZLSVersionInfo> {
            return withContext(Dispatchers.IO) {
                val zigVersion = toolchain.zig.getEnv(project).getOrNull()?.version ?: return@withContext emptyList()
                val service = DownloadableFileService.getInstance()
                val tempFile = FileUtil.createTempFile(tempPluginDir, "zls_version_info", ".json", false, false)
                val desc = service.createFileDescription("https://releases.zigtools.org/v1/zls/select-version?zig_version=${URLEncoder.encode(zigVersion, Charsets.UTF_8)}&compatibility=only-runtime", tempFile.name)
                val downloader = service.createDownloader(listOf(desc), "ZLS version information")
                val downloadResults = coroutineToIndicator {
                    downloader.download(tempPluginDir)
                }
                if (downloadResults.isEmpty())
                    return@withContext emptyList()
                val index = downloadResults[0].first
                val info = index.inputStream().use { Json.decodeFromStream<JsonObject>(it) }
                index.delete()
                return@withContext listOfNotNull(parseVersion(info))
            }
        }
    }
}
private fun parseVersion(data: JsonObject): ZLSVersionInfo? {
    val versionTag = data["version"]?.asSafely<JsonPrimitive>()?.content

    val version = SemVer.parseFromText(versionTag) ?: return null
    val date = data["date"]?.asSafely<JsonPrimitive>()?.content ?: ""
    val dist = data.firstNotNullOfOrNull { (dist, tb) -> getTarballIfCompatible(dist, tb) }
        ?: return null

    return ZLSVersionInfo(version, date, dist)
}
