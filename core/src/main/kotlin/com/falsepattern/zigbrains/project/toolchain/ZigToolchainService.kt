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

import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute
import java.util.UUID

@Service(Service.Level.PROJECT)
@State(
    name = "ZigToolchain",
    storages = [Storage("zigbrains.xml")]
)
class ZigToolchainService: SerializablePersistentStateComponent<ZigToolchainService.State>(State()) {
    var toolchainUUID: UUID?
        get() = state.toolchain.ifBlank { null }?.let { UUID.fromString(it) }?.takeIf {
            if (ZigToolchainListService.getInstance().hasToolchain(it)) {
                true
            } else {
                updateState {
                    it.copy(toolchain = "")
                }
                false
            }
        }
        set(value) {
            updateState {
                it.copy(toolchain = value?.toString() ?: "")
            }
        }

    val toolchain: ZigToolchain?
        get() = toolchainUUID?.let { ZigToolchainListService.getInstance().getToolchain(it) }

    data class State(
        @JvmField
        @Attribute
        var toolchain: String = ""
    )

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ZigToolchainService = project.service()
    }
}