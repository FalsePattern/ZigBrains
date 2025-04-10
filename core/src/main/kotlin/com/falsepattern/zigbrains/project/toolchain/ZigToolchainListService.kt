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

import com.falsepattern.zigbrains.project.toolchain.ZigToolchainListService.MyState
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.base.resolve
import com.falsepattern.zigbrains.project.toolchain.base.toRef
import com.falsepattern.zigbrains.shared.AccessibleStorage
import com.falsepattern.zigbrains.shared.ChangeTrackingStorage
import com.falsepattern.zigbrains.shared.IterableStorage
import com.falsepattern.zigbrains.shared.UUIDMapSerializable
import com.falsepattern.zigbrains.shared.UUIDStorage
import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(
    name = "ZigToolchainList",
    storages = [Storage("zigbrains.xml")]
)
class ZigToolchainListService: UUIDMapSerializable.Converting<ZigToolchain, ZigToolchain.Ref, MyState>(MyState()), IZigToolchainListService {
    override fun serialize(value: ZigToolchain) = value.toRef()
    override fun deserialize(value: ZigToolchain.Ref) = value.resolve()
    override fun getStorage(state: MyState) = state.toolchains
    override fun updateStorage(state: MyState, storage: ToolchainStorage) = state.copy(toolchains = storage)

    data class MyState(
        @JvmField
        val toolchains: ToolchainStorage = emptyMap(),
    )

    companion object {
        @JvmStatic
        fun getInstance(): IZigToolchainListService = service<ZigToolchainListService>()
    }
}

inline val zigToolchainList: IZigToolchainListService get() = ZigToolchainListService.getInstance()

sealed interface IZigToolchainListService: ChangeTrackingStorage, AccessibleStorage<ZigToolchain>, IterableStorage<ZigToolchain>

private typealias ToolchainStorage = UUIDStorage<ZigToolchain.Ref>
