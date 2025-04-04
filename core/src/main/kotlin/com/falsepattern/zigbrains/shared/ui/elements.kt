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

package com.falsepattern.zigbrains.shared.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*


sealed interface ListElemIn<T>
@Suppress("UNCHECKED_CAST")
sealed interface ListElem<T> : ListElemIn<T> {
    sealed interface Pseudo<T>: ListElem<T>
    sealed interface One<T> : ListElem<T> {
        val instance: T

        @JvmRecord
        data class Suggested<T>(override val instance: T): One<T>, Pseudo<T>

        @JvmRecord
        data class Actual<T>(val uuid: UUID, override val instance: T): One<T>
    }
    class None<T> private constructor(): ListElem<T> {
        companion object {
            private val INSTANCE = None<Any>()
            operator fun <T> invoke(): None<T> {
                return INSTANCE as None<T>
            }
        }
    }
    class Download<T> private constructor(): ListElem<T>, Pseudo<T> {
        companion object {
            private val INSTANCE = Download<Any>()
            operator fun <T> invoke(): Download<T> {
                return INSTANCE as Download<T>
            }
        }
    }
    class FromDisk<T> private constructor(): ListElem<T>, Pseudo<T> {
        companion object {
            private val INSTANCE = FromDisk<Any>()
            operator fun <T> invoke(): FromDisk<T> {
                return INSTANCE as FromDisk<T>
            }
        }
    }
    data class Pending<T>(val elems: Flow<ListElem<T>>): ListElem<T>

    companion object {
        private val fetchGroup: List<ListElem<Any>> = listOf(Download(), FromDisk())
        fun <T> fetchGroup() = fetchGroup as List<ListElem<T>>
    }
}

@JvmRecord
data class Separator<T>(val text: String, val line: Boolean) : ListElemIn<T>

fun <T> Pair<UUID, T>.asActual() = ListElem.One.Actual(first, second)

fun <T> T.asSuggested() = ListElem.One.Suggested(this)

@JvmName("listElemFlowAsPending")
fun <T> Flow<ListElem<T>>.asPending() = ListElem.Pending(this)

fun <T> Flow<T>.asPending() = map { it.asSuggested() }.asPending()

