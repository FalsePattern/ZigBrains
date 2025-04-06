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
import java.util.UUID


internal sealed interface TCListElemIn

internal sealed interface TCListElem : TCListElemIn {
    sealed interface Toolchain : TCListElem {
        val toolchain: ZigToolchain

        @JvmRecord
        data class Suggested(override val toolchain: ZigToolchain): Toolchain

        @JvmRecord
        data class Actual(val uuid: UUID, override val toolchain: ZigToolchain): Toolchain
    }
    object None: TCListElem
    object Download : TCListElem
    object FromDisk : TCListElem

    companion object {
        val fetchGroup get() = listOf(Download, FromDisk)
    }
}

@JvmRecord
internal data class Separator(val text: String, val line: Boolean) : TCListElemIn

internal fun Pair<UUID, ZigToolchain>.asActual() = TCListElem.Toolchain.Actual(first, second)

internal fun ZigToolchain.asSuggested() = TCListElem.Toolchain.Suggested(this)