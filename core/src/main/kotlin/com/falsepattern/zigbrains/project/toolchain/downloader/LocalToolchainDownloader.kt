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
import com.falsepattern.zigbrains.project.toolchain.local.LocalZigToolchain
import com.falsepattern.zigbrains.project.toolchain.local.suggestedLocalToolchainPath
import com.falsepattern.zigbrains.shared.downloader.Downloader
import java.awt.Component

class LocalToolchainDownloader(component: Component) : Downloader<LocalZigToolchain, ZigVersionInfo>(component) {
    override val windowTitle get() = ZigBrainsBundle.message("settings.toolchain.downloader.title")
    override val versionInfoFetchTitle get() = ZigBrainsBundle.message("settings.toolchain.downloader.progress.fetch")
    override val suggestedPath get() = suggestedLocalToolchainPath
    override fun downloadProgressTitle(version: ZigVersionInfo) = ZigBrainsBundle.message("settings.toolchain.downloader.progress.install", version.version.rawVersion)
    override fun localSelector() = LocalToolchainSelector(component)
    override suspend fun downloadVersionList() = ZigVersionInfo.downloadVersionList()
}