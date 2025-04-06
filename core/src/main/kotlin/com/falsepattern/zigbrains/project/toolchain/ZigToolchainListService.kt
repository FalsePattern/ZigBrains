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

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.MapAnnotation
import java.util.UUID

@Service(Service.Level.APP)
@State(
    name = "ZigToolchainList",
    storages = [Storage("zigbrains.xml")]
)
class ZigToolchainListService: SerializablePersistentStateComponent<ZigToolchainListService.State>(State()) {
    fun setToolchain(uuid: UUID, toolchain: AbstractZigToolchain) {
        updateState {
            val newMap = HashMap<String, AbstractZigToolchain.Ref>()
            newMap.putAll(it.toolchains)
            newMap.put(uuid.toString(), toolchain.toRef())
            it.copy(toolchains = newMap)
        }
    }

    fun getToolchain(uuid: UUID): AbstractZigToolchain? {
        return state.toolchains[uuid.toString()]?.resolve()
    }

    fun removeToolchain(uuid: UUID) {
        val str = uuid.toString()
        updateState {
            it.copy(toolchains = it.toolchains.filter { it.key != str })
        }
    }

    val toolchains: Sequence<Pair<UUID, AbstractZigToolchain>>
        get() = state.toolchains
            .asSequence()
            .mapNotNull {
                val uuid = UUID.fromString(it.key) ?: return@mapNotNull null
                val tc = it.value.resolve() ?: return@mapNotNull null
                uuid to tc
            }

    data class State(
        @JvmField
        val toolchains: Map<String, AbstractZigToolchain.Ref> = emptyMap(),
    )
}

val zigToolchainList get() = service<ZigToolchainListService>()