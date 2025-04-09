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
import com.falsepattern.zigbrains.project.toolchain.base.resolve
import com.falsepattern.zigbrains.project.toolchain.base.toRef
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.components.*
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.UUID

@Service(Service.Level.APP)
@State(
    name = "ZigToolchainList",
    storages = [Storage("zigbrains.xml")]
)
class ZigToolchainListService: SerializablePersistentStateComponent<ZigToolchainListService.State>(State()), IZigToolchainListService {
    private val changeListeners = ArrayList<WeakReference<ToolchainListChangeListener>>()

    override val toolchains: Sequence<Pair<UUID, ZigToolchain>>
        get() = state.toolchains
            .asSequence()
            .mapNotNull {
                val uuid = UUID.fromString(it.key) ?: return@mapNotNull null
                val tc = it.value.resolve() ?: return@mapNotNull null
                uuid to tc
            }

    override fun setToolchain(uuid: UUID, toolchain: ZigToolchain) {
        val str = uuid.toString()
        val ref = toolchain.toRef()
        updateState {
            val newMap = HashMap<String, ZigToolchain.Ref>()
            newMap.putAll(it.toolchains)
            newMap[str] = ref
            it.copy(toolchains = newMap)
        }
        notifyChanged()
    }

    override fun registerNewToolchain(toolchain: ZigToolchain): UUID {
        val ref = toolchain.toRef()
        var uuid = UUID.randomUUID()
        updateState {
            val newMap = HashMap<String, ZigToolchain.Ref>()
            newMap.putAll(it.toolchains)
            var uuidStr = uuid.toString()
            while (newMap.containsKey(uuidStr)) {
                uuid = UUID.randomUUID()
                uuidStr = uuid.toString()
            }
            newMap[uuidStr] = ref
            it.copy(toolchains = newMap)
        }
        notifyChanged()
        return uuid
    }

    override fun getToolchain(uuid: UUID): ZigToolchain? {
        return state.toolchains[uuid.toString()]?.resolve()
    }

    override fun hasToolchain(uuid: UUID): Boolean {
        return state.toolchains.containsKey(uuid.toString())
    }

    override fun removeToolchain(uuid: UUID) {
        val str = uuid.toString()
        updateState {
            it.copy(toolchains = it.toolchains.filter { it.key != str })
        }
        notifyChanged()
    }

    override fun addChangeListener(listener: ToolchainListChangeListener) {
        synchronized(changeListeners) {
            changeListeners.add(WeakReference(listener))
        }
    }

    override fun removeChangeListener(listener: ToolchainListChangeListener) {
        synchronized(changeListeners) {
            changeListeners.removeIf {
                val v = it.get()
                v == null || v === listener
            }
        }
    }

    override fun <T: ZigToolchain> withUniqueName(toolchain: T): T {
        val baseName = toolchain.name ?: ""
        var index = 0
        var currentName = baseName
        while (toolchains.any { (_, existing) -> existing.name == currentName }) {
            index++
            currentName = "$baseName ($index)"
        }
        @Suppress("UNCHECKED_CAST")
        return toolchain.withName(currentName) as T
    }

    private fun notifyChanged() {
        synchronized(changeListeners) {
            var i = 0
            while (i < changeListeners.size) {
                val v = changeListeners[i].get()
                if (v == null) {
                    changeListeners.removeAt(i)
                    continue
                }
                zigCoroutineScope.launch {
                    v.toolchainListChanged()
                }
                i++
            }
        }
    }

    data class State(
        @JvmField
        val toolchains: Map<String, ZigToolchain.Ref> = emptyMap(),
    )

    companion object {
        @JvmStatic
        fun getInstance(): IZigToolchainListService = service<ZigToolchainListService>()
    }
}

@FunctionalInterface
interface ToolchainListChangeListener {
    suspend fun toolchainListChanged()
}

sealed interface IZigToolchainListService {
    val toolchains: Sequence<Pair<UUID, ZigToolchain>>
    fun setToolchain(uuid: UUID, toolchain: ZigToolchain)
    fun registerNewToolchain(toolchain: ZigToolchain): UUID
    fun getToolchain(uuid: UUID): ZigToolchain?
    fun hasToolchain(uuid: UUID): Boolean
    fun removeToolchain(uuid: UUID)
    fun addChangeListener(listener: ToolchainListChangeListener)
    fun removeChangeListener(listener: ToolchainListChangeListener)
    fun <T: ZigToolchain> withUniqueName(toolchain: T): T
}
