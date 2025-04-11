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

package com.falsepattern.zigbrains.shared

import com.intellij.openapi.components.SerializablePersistentStateComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.UUID
import kotlin.collections.any

typealias UUIDStorage<T> = Map<String, T>

abstract class UUIDMapSerializable<T, S: Any>(init: S): SerializablePersistentStateComponent<S>(init), ChangeTrackingStorage {
    private val changeListeners = ArrayList<WeakReference<StorageChangeListener>>()

    protected abstract fun getStorage(state: S): UUIDStorage<T>

    protected abstract fun updateStorage(state: S, storage: UUIDStorage<T>): S

    override fun addChangeListener(listener: StorageChangeListener) {
        synchronized(changeListeners) {
            changeListeners.add(WeakReference(listener))
        }
    }

    override fun removeChangeListener(listener: StorageChangeListener) {
        synchronized(changeListeners) {
            changeListeners.removeIf {
                val v = it.get()
                v == null || v === listener
            }
        }
    }

    protected fun registerNewUUID(value: T): UUID {
        var uuid = UUID.randomUUID()
        updateState {
            val newMap = HashMap<String, T>()
            newMap.putAll(getStorage(it))
            var uuidStr = uuid.asString()
            while (newMap.containsKey(uuidStr)) {
                uuid = UUID.randomUUID()
                uuidStr = uuid.asString()
            }
            newMap[uuidStr] = value
            updateStorage(it, newMap)
        }
        notifyChanged()
        return uuid
    }
    
    protected fun setStateUUID(uuid: UUID, value: T) {
        val str = uuid.asString()
        updateState {
            val newMap = HashMap<String, T>()
            newMap.putAll(getStorage(it))
            newMap[str] = value
            updateStorage(it, newMap)
        }
        notifyChanged()
    }
    
    protected fun getStateUUID(uuid: UUID): T? {
        return getStorage(state)[uuid.asString()]
    }
    
    protected fun hasStateUUID(uuid: UUID): Boolean {
        return getStorage(state).containsKey(uuid.asString())
    }
    
    protected fun removeStateUUID(uuid: UUID) {
        val str = uuid.asString()
        updateState { 
            updateStorage(state, getStorage(state).filter { it.key != str })
        }
        notifyChanged()
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
                    v()
                }
                i++
            }
        }
    }

    abstract class Converting<R, T, S: Any>(init: S):
        UUIDMapSerializable<T, S>(init),
        AccessibleStorage<R>,
        IterableStorage<R>
    {
        protected abstract fun serialize(value: R): T
        protected abstract fun deserialize(value: T): R?
        override fun registerNew(value: R): UUID {
            val ser = serialize(value)
            return registerNewUUID(ser)
        }
        override operator fun set(uuid: UUID, value: R) {
            val ser = serialize(value)
            setStateUUID(uuid, ser)
        }
        override operator fun get(uuid: UUID): R? {
            return getStateUUID(uuid)?.let { deserialize(it) }
        }
        override operator fun contains(uuid: UUID): Boolean {
            return hasStateUUID(uuid)
        }
        override fun remove(uuid: UUID) {
            removeStateUUID(uuid)
        }

        override fun iterator(): Iterator<Pair<UUID, R>> {
            return getStorage(state)
                .asSequence()
                .mapNotNull {
                    val uuid = it.key.asUUID() ?: return@mapNotNull null
                    val tc = deserialize(it.value) ?: return@mapNotNull null
                    uuid to tc
                }.iterator()
        }
    }

    abstract class Direct<T, S: Any>(init: S): Converting<T, T, S>(init) {
        override fun serialize(value: T): T {
            return value
        }

        override fun deserialize(value: T): T? {
            return value
        }
    }
}

typealias StorageChangeListener = suspend CoroutineScope.() -> Unit

interface ChangeTrackingStorage {
    fun addChangeListener(listener: StorageChangeListener)
    fun removeChangeListener(listener: StorageChangeListener)
}

interface AccessibleStorage<R> {
    fun registerNew(value: R): UUID
    operator fun set(uuid: UUID, value: R)
    operator fun get(uuid: UUID): R?
    operator fun contains(uuid: UUID): Boolean
    fun remove(uuid: UUID)
}

interface IterableStorage<R>: Iterable<Pair<UUID, R>>

fun <R: NamedObject<R>, T: R> IterableStorage<R>.withUniqueName(value: T): T {
    val baseName = value.name ?: ""
    var index = 0
    var currentName = baseName
    val names = this.map { (_, existing) -> existing.name }
    while (names.any { it == currentName }) {
        index++
        currentName = "$baseName ($index)"
    }
    @Suppress("UNCHECKED_CAST")
    return value.withName(currentName) as T
}