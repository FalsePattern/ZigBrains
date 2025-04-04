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

import com.falsepattern.zigbrains.lsp.ZLSBundle
import com.falsepattern.zigbrains.lsp.zls.ZLSVersion
import com.falsepattern.zigbrains.lsp.zls.ui.getSuggestedZLSPath
import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider.IUserDataBridge
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchainConfigurable
import com.falsepattern.zigbrains.shared.downloader.Downloader
import java.awt.Component

class ZLSDownloader(component: Component, private val data: IUserDataBridge?) : Downloader<ZLSVersion, ZLSVersionInfo>(component) {
    override val windowTitle get() = ZLSBundle.message("settings.downloader.title")
    override val versionInfoFetchTitle get() = ZLSBundle.message("settings.downloader.progress.fetch")
    override fun downloadProgressTitle(version: ZLSVersionInfo) = ZLSBundle.message("settings.downloader.progress.install", version.version.rawVersion)
    override fun localSelector() = ZLSLocalSelector(component)
    override suspend fun downloadVersionList(): List<ZLSVersionInfo> {
        val toolchain = data?.getUserData(ZigToolchainConfigurable.TOOLCHAIN_KEY)?.get()
        val project = data?.getUserData(ZigProjectConfigurationProvider.PROJECT_KEY)
        return ZLSVersionInfo.downloadVersionInfoFor(toolchain, project)
    }
    override fun getSuggestedPath() = getSuggestedZLSPath()
}