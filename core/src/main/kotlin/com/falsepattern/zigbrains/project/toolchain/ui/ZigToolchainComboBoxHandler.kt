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

import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.downloader.LocalToolchainDownloader
import com.falsepattern.zigbrains.project.toolchain.downloader.LocalToolchainSelector
import com.falsepattern.zigbrains.project.toolchain.zigToolchainList
import com.falsepattern.zigbrains.shared.ui.ListElem
import com.falsepattern.zigbrains.shared.withUniqueName
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import java.awt.Component
import java.util.*

internal object ZigToolchainComboBoxHandler {
    @RequiresBackgroundThread
    suspend fun onItemSelected(context: Component, elem: ListElem.Pseudo<ZigToolchain>): UUID? = when(elem) {
        is ListElem.One.Suggested -> zigToolchainList.withUniqueName(elem.instance)
        is ListElem.Download -> LocalToolchainDownloader(context).download()
        is ListElem.FromDisk -> LocalToolchainSelector(context).browse()
    }?.let { zigToolchainList.registerNew(it) }
}