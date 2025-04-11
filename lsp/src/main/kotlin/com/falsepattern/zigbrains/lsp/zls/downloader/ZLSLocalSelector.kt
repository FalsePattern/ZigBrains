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
import com.falsepattern.zigbrains.lsp.zls.zlsInstallations
import com.falsepattern.zigbrains.shared.downloader.LocalSelector
import com.falsepattern.zigbrains.shared.withUniqueName
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.util.SystemInfo
import java.awt.Component
import java.nio.file.Path
import kotlin.io.path.isDirectory

class ZLSLocalSelector(component: Component) : LocalSelector<ZLSVersion>(component) {
    override val windowTitle: String
        get() = ZLSBundle.message("settings.local-selector.title")
    override val descriptor: FileChooserDescriptor
        get() = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle(ZLSBundle.message("settings.local-selector.chooser.title"))

    override suspend fun browse(preSelected: Path?): ZLSVersion? {
        if (preSelected?.isDirectory() == true) {
            return super.browse(preSelected.resolve(if (SystemInfo.isWindows) "zls.exe" else "zls"))
        }
        return super.browse(preSelected)
    }

    override suspend fun verify(path: Path): VerifyResult {
        var zls = resolve(path, null)
        var result: VerifyResult
        result = if (zls == null) VerifyResult(
            null,
            false,
            AllIcons.General.Error,
            ZLSBundle.message("settings.local-selector.state.invalid"),
        ) else VerifyResult(
            null,
            true,
            AllIcons.General.Information,
            ZLSBundle.message("settings.local-selector.state.ok")
        )
        if (zls != null) {
            zls = zlsInstallations.withUniqueName(zls)
        }
        return result.copy(name = zls?.name)
    }

    override suspend fun resolve(path: Path, name: String?): ZLSVersion? {
        return ZLSVersion.tryFromPath(path)?.let { zls -> name?.let { zls.copy(name = it) } ?: zls }
    }
}