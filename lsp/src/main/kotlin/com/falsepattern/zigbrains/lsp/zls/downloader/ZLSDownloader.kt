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

import com.falsepattern.zigbrains.lsp.zls.ZLSVersion
import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchainConfigurable
import com.falsepattern.zigbrains.shared.downloader.Downloader
import com.falsepattern.zigbrains.shared.downloader.LocalSelector
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.util.system.OS
import java.awt.Component
import java.nio.file.Path
import kotlin.io.path.isDirectory

class ZLSDownloader(component: Component, private val data: ZigProjectConfigurationProvider.IUserDataBridge?) : Downloader<ZLSVersion, ZLSVersionInfo>(component) {
    override val windowTitle: String
        get() = "Install ZLS"
    override val versionInfoFetchTitle: @NlsContexts.ProgressTitle String
        get() = "Fetching zls version information"

    override fun downloadProgressTitle(version: ZLSVersionInfo): @NlsContexts.ProgressTitle String {
        return "Installing ZLS ${version.version.rawVersion}"
    }

    override fun localSelector(): LocalSelector<ZLSVersion> {
        return ZLSLocalSelector(component)
    }

    override suspend fun downloadVersionList(): List<ZLSVersionInfo> {
        val toolchain = data?.getUserData(ZigToolchainConfigurable.TOOLCHAIN_KEY)?.get() ?: return emptyList()
        val project = data.getUserData(ZigProjectConfigurationProvider.PROJECT_KEY)
        return ZLSVersionInfo.downloadVersionInfoFor(toolchain, project)
    }

    override fun getSuggestedPath(): Path? {
        return getSuggestedZLSPath()
    }
}

fun getSuggestedZLSPath(): Path? {
    return getWellKnownZLS().getOrNull(0)
}

/**
 * Returns the paths to the following list of folders:
 *
 * 1. DATA/zls
 * 2. HOME/.zig
 *
 * Where DATA is:
 *  - ~/Library on macOS
 *  - %LOCALAPPDATA% on Windows
 *  - $XDG_DATA_HOME (or ~/.local/share if not set) on other OSes
 *
 * and HOME is the user home path
 */
private fun getWellKnownZLS(): List<Path> {
    val home = System.getProperty("user.home")?.toNioPathOrNull() ?: return emptyList()
    val xdgDataHome = when(OS.CURRENT) {
        OS.macOS -> home.resolve("Library")
        OS.Windows -> System.getenv("LOCALAPPDATA")?.toNioPathOrNull()
        else -> System.getenv("XDG_DATA_HOME")?.toNioPathOrNull() ?: home.resolve(Path.of(".local", "share"))
    }
    val res = ArrayList<Path>()
    if (xdgDataHome != null && xdgDataHome.isDirectory()) {
        res.add(xdgDataHome.resolve("zls"))
    }
    res.add(home.resolve(".zls"))
    return res
}