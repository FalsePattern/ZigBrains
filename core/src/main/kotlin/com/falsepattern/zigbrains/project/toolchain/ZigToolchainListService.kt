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

package com.falsepattern.zigbrains.project.toolchain

import com.falsepattern.zigbrains.project.settings.ZigProjectSettings
import com.falsepattern.zigbrains.project.toolchain.stdlib.ZigSyntheticLibrary
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch

@Service(Service.Level.APP)
@State(
    name = "ZigProjectSettings",
    storages = [Storage("zigbrains.xml")]
)
class ZigToolchainListService(): PersistentStateComponent<ZigToolchainList> {
    @Volatile
    private var state = ZigToolchainList()

    override fun getState(): ZigToolchainList {
        return state.copy()
    }

    fun setState(value: ZigToolchainList) {
        this.state = value
    }

    override fun loadState(state: ZigToolchainList) {
        setState(state)
    }

    fun isModified(otherData: ZigToolchainList): Boolean {
        return state != otherData
    }
}

val zigToolchainList get() = service<ZigToolchainListService>()