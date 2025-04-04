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
import com.falsepattern.zigbrains.shared.ui.renderPathNameComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.ui.SimpleColoredComponent
import com.intellij.util.system.OS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString

class LocalZigToolchainProvider: ZigToolchainProvider {
    override val serialMarker: String
        get() = "local"

    override fun deserialize(data: Map<String, String>): ZigToolchain? {
        val location = data["location"]?.toNioPathOrNull() ?: return null
        val std = data["std"]?.toNioPathOrNull()
        val name = data["name"]
        return LocalZigToolchain(location, std, name)
    }

    override fun isCompatible(toolchain: ZigToolchain): Boolean {
        return toolchain is LocalZigToolchain
    }

    override fun serialize(toolchain: ZigToolchain): Map<String, String> {
        toolchain as LocalZigToolchain
        val map = HashMap<String, String>()
        toolchain.location.pathString.let { map["location"] = it }
        toolchain.std?.pathString?.let { map["std"] = it }
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
        val wellKnown = getWellKnown().asFlow().flatMapConcat { dir ->
            runCatching {
                Files.newDirectoryStream(dir).use { stream ->
                    stream.toList().filterNotNull().asFlow()
                }
            }.getOrElse { emptyFlow() }
        }
        val joined = flowOf(pathToolchains, wellKnown).flattenConcat()
        return joined.mapNotNull { LocalZigToolchain.tryFromPath(it) }
    }

    override fun render(toolchain: ZigToolchain, component: SimpleColoredComponent, isSuggestion: Boolean, isSelected: Boolean) {
        toolchain as LocalZigToolchain
        val name = toolchain.name
        val path = toolchain.location.pathString
        renderPathNameComponent(path, name, "Zig", component, isSuggestion, isSelected)
    }
}

fun getSuggestedLocalToolchainPath(): Path? {
    return getWellKnown().getOrNull(0)
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
private fun getWellKnown(): List<Path> {
    val home = System.getProperty("user.home")?.toNioPathOrNull() ?: return emptyList()
    val xdgDataHome = when(OS.CURRENT) {
        OS.macOS -> home.resolve("Library")
        OS.Windows -> System.getenv("LOCALAPPDATA")?.toNioPathOrNull()
        else -> System.getenv("XDG_DATA_HOME")?.toNioPathOrNull() ?: home.resolve(Path.of(".local", "share"))
    }
    val res = ArrayList<Path>()
    if (xdgDataHome != null && xdgDataHome.isDirectory()) {
        res.add(xdgDataHome.resolve("zig"))
        res.add(xdgDataHome.resolve("zigup"))
    }
    res.add(home.resolve(".zig"))
    return res
}