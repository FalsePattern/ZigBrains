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

package com.falsepattern.zigbrains.project.toolchain.ui

import com.falsepattern.zigbrains.project.toolchain.ZigToolchainListService
import com.falsepattern.zigbrains.project.toolchain.downloader.Downloader
import com.falsepattern.zigbrains.project.toolchain.downloader.LocalSelector
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import java.awt.Component
import java.util.UUID

internal object ZigToolchainComboBoxHandler {
    @RequiresBackgroundThread
    suspend fun onItemSelected(context: Component, elem: TCListElem.Pseudo): UUID? = when(elem) {
        is TCListElem.Toolchain.Suggested -> elem.toolchain
        is TCListElem.Download -> Downloader.downloadToolchain(context)
        is TCListElem.FromDisk -> LocalSelector.browseFromDisk(context)
    }?.let { ZigToolchainListService.getInstance().registerNewToolchain(it) }
}