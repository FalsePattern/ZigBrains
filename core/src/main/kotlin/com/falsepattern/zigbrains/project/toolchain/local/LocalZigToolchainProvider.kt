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

package com.falsepattern.zigbrains.project.toolchain.local

import com.falsepattern.zigbrains.direnv.DirenvService
import com.falsepattern.zigbrains.direnv.Env
import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchainConfigurable
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchainProvider
import com.falsepattern.zigbrains.shared.downloader.homePath
import com.falsepattern.zigbrains.shared.downloader.xdgDataHome
import com.falsepattern.zigbrains.shared.sanitizedPathString
import com.falsepattern.zigbrains.shared.sanitizedToNioPath
import com.falsepattern.zigbrains.shared.ui.renderPathNameComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.ui.SimpleColoredComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.isDirectory

class LocalZigToolchainProvider: ZigToolchainProvider {
    override val serialMarker: String
        get() = "local"

    override fun deserialize(data: Map<String, String>): ZigToolchain? {
        val location = data["location"]?.sanitizedToNioPath() ?: return null
        val std = data["std"]?.sanitizedToNioPath()
        val name = data["name"]
        return LocalZigToolchain(location, std, name)
    }

    override fun isCompatible(toolchain: ZigToolchain): Boolean {
        return toolchain is LocalZigToolchain
    }

    override fun serialize(toolchain: ZigToolchain): Map<String, String> {
        toolchain as LocalZigToolchain
        val map = HashMap<String, String>()
        (toolchain.location.sanitizedPathString ?: "").let { map["location"] = it }
        toolchain.std?.sanitizedPathString?.let { map["std"] = it }
        toolchain.name?.let { map["name"] = it }
        return map
    }

    override fun matchesSuggestion(
        toolchain: ZigToolchain,
        suggestion: ZigToolchain
    ): Boolean {
        toolchain as LocalZigToolchain
        suggestion as LocalZigToolchain
        return toolchain.location == suggestion.location
    }

    override fun createConfigurable(
        uuid: UUID,
        toolchain: ZigToolchain,
        data: ZigProjectConfigurationProvider.IUserDataBridge?,
        modal: Boolean
    ): ZigToolchainConfigurable<*> {
        toolchain as LocalZigToolchain
        return LocalZigToolchainConfigurable(uuid, toolchain, data, modal)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun suggestToolchains(project: Project?, data: UserDataHolder): Flow<ZigToolchain> {
        val env = if (project != null && DirenvService.getStateFor(data, project).isEnabled(project)) {
            DirenvService.getInstance(project).import()
        } else {
            Env.empty
        }
        val pathToolchains = env.findAllExecutablesOnPATH("zig").mapNotNull { it.parent }
        val wellKnown = wellKnown.asFlow().flatMapConcat { dir ->
            runCatching {
                Files.newDirectoryStream(dir).use { stream ->
                    stream.asSequence().mapNotNull { path ->
                        if (path == null || !path.isDirectory())
                            return@mapNotNull null
                        return@mapNotNull tryDetermineStructure(path)
                    }.toList().asFlow()
                }
            }.getOrElse { emptyFlow() }
        }
        val joined = flowOf(pathToolchains, wellKnown).flattenConcat()
        return joined.mapNotNull { LocalZigToolchain.tryFromPath(it) }
    }

    override fun render(toolchain: ZigToolchain, component: SimpleColoredComponent, isSuggestion: Boolean, isSelected: Boolean) {
        toolchain as LocalZigToolchain
        val name = toolchain.name
        val path = toolchain.location.sanitizedPathString ?: "unknown path"
        renderPathNameComponent(path, name, "Zig", component, isSuggestion, isSelected)
    }
}

val suggestedLocalToolchainPath: Path? by lazy {
    wellKnown.getOrNull(0)
}

/**
 * Returns the paths to the following list of folders:
 *
 * 1. DATA/zig
 * 2. DATA/zigup
 * 3. HOME/.zig
 *
 * Where DATA is:
 *  - ~/Library on macOS
 *  - %LOCALAPPDATA% on Windows
 *  - $XDG_DATA_HOME (or ~/.local/share if not set) on other OSes
 *
 * and HOME is the user home path
 */
private val wellKnown: List<Path> by lazy {
    val res = ArrayList<Path>()
    xdgDataHome?.let {
        res.add(it.resolve("zig"))
        res.add(it.resolve("zigup"))
    }
    homePath?.let { res.add(it.resolve(".zig")) }
    gradleUserHome?.let { res.add(it.resolve(Path.of("caches", "com.falsepattern.zigbuild", "toolchains-1"))) }
    res
}

private val gradleUserHome: Path? by lazy {
    System.getenv("GRADLE_USER_HOME")?.sanitizedToNioPath()?.takeIf { it.isDirectory() } ?: homePath?.resolve(".gradle")?.takeIf { it.isDirectory() }
}

private fun tryDetermineStructure(outputDir: Path): Path? {
    //More efficient than reading the whole list
    val filesInDirectory = runCatching { Files.newDirectoryStream(outputDir).use { it.take(2) } }.getOrNull()
    if (filesInDirectory == null || filesInDirectory.isEmpty()) {
        return null
    }

    // If there's one file, we go down one level. Otherwise, zig is here
    return if (filesInDirectory.size > 1) {
        outputDir
    } else {
        filesInDirectory[0]
    }
}