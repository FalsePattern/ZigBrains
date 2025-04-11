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
import com.falsepattern.zigbrains.project.toolchain.zigToolchainList
import com.falsepattern.zigbrains.shared.downloader.LocalSelector
import com.falsepattern.zigbrains.shared.withUniqueName
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withModalProgress
import java.awt.Component
import java.nio.file.Path

class LocalToolchainSelector(component: Component): LocalSelector<LocalZigToolchain>(component) {
    override val windowTitle: String
        get() = ZigBrainsBundle.message("settings.toolchain.local-selector.title")
    override val descriptor: FileChooserDescriptor
        get() = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle(ZigBrainsBundle.message("settings.toolchain.local-selector.chooser.title"))

    override suspend fun verify(path: Path): VerifyResult {
        var tc = resolve(path, null)
        var result: VerifyResult
        if (tc == null) {
            result = VerifyResult(
                null,
                false,
                AllIcons.General.Error,
                ZigBrainsBundle.message("settings.toolchain.local-selector.state.invalid"),
            )
        } else {
            val existingToolchain = zigToolchainList
                .mapNotNull { it.second as? LocalZigToolchain }
                .firstOrNull { it.location == tc.location }
            if (existingToolchain != null) {
                result = VerifyResult(
                    null,
                    true,
                    AllIcons.General.Warning,
                    existingToolchain.name?.let { ZigBrainsBundle.message("settings.toolchain.local-selector.state.already-exists-named", it) }
                        ?: ZigBrainsBundle.message("settings.toolchain.local-selector.state.already-exists-unnamed")
                )
            } else {
                result = VerifyResult(
                    null,
                    true,
                    AllIcons.General.Information,
                    ZigBrainsBundle.message("settings.toolchain.local-selector.state.ok")
                )
            }
        }
        if (tc != null) {
            tc = zigToolchainList.withUniqueName(tc)
        }
        return result.copy(name = tc?.name)
    }

    override suspend fun resolve(path: Path, name: String?): LocalZigToolchain? {
        return runCatching { withModalProgress(ModalTaskOwner.component(component), "Resolving toolchain", TaskCancellation.cancellable()) {
            LocalZigToolchain.tryFromPath(path)?.let { it.withName(name ?: it.name) }
        } }.getOrNull()
    }
}