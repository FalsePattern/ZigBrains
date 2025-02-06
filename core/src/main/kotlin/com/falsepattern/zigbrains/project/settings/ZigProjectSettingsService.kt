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

package com.falsepattern.zigbrains.project.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "ZigProjectSettings",
    storages = [Storage("zigbrains.xml")]
)
class ZigProjectSettingsService: PersistentStateComponent<ZigProjectSettings> {
    @Volatile
    private var state = ZigProjectSettings()

    override fun getState(): ZigProjectSettings {
        return state.copy()
    }

    fun setState(value: ZigProjectSettings) {
        this.state = value
    }

    override fun loadState(state: ZigProjectSettings) {
        this.state = state
    }

    fun isModified(otherData: ZigProjectSettings): Boolean {
        return state != otherData
    }
}

val Project.zigProjectSettings get() = service<ZigProjectSettingsService>()