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

package com.falsepattern.zigbrains.lsp.zls

import com.falsepattern.zigbrains.project.toolchain.zigToolchainList
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute
import java.util.UUID

@Service(Service.Level.PROJECT)
@State(
    name = "ZLS",
    storages = [Storage("zigbrains.xml")]
)
class ZLSService: SerializablePersistentStateComponent<ZLSService.MyState>(MyState()) {
    var zlsUUID: UUID?
        get() = state.zls.ifBlank { null }?.let { UUID.fromString(it) }?.takeIf {
            if (it in zigToolchainList) {
                true
            } else {
                updateState {
                    it.copy(zls = "")
                }
                false
            }
        }
        set(value) {
            updateState {
                it.copy(zls = value?.toString() ?: "")
            }
        }

    val zls: ZLSVersion?
        get() = zlsUUID?.let { zlsInstallations[it] }

    data class MyState(@JvmField @Attribute var zls: String = "")

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ZLSService = project.service<ZLSService>()
    }
}