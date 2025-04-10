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

import com.falsepattern.zigbrains.lsp.zls.ZLSInstallationsService.MyState
import com.falsepattern.zigbrains.shared.UUIDMapSerializable
import com.falsepattern.zigbrains.shared.UUIDStorage
import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(
    name = "ZLSInstallations",
    storages = [Storage("zigbrains.xml")]
)
class ZLSInstallationsService: UUIDMapSerializable.Converting<ZLSVersion, ZLSVersion.Ref, MyState>(MyState()) {
    override fun serialize(value: ZLSVersion) = value.toRef()
    override fun deserialize(value: ZLSVersion.Ref) = value.resolve()
    override fun getStorage(state: MyState) = state.zlsInstallations
    override fun updateStorage(state: MyState, storage: ZLSStorage) = state.copy(zlsInstallations = storage)

    data class MyState(@JvmField val zlsInstallations: ZLSStorage = emptyMap())

    companion object {
        @JvmStatic
        fun getInstance(): ZLSInstallationsService = service<ZLSInstallationsService>()
    }
}

inline val zlsInstallations: ZLSInstallationsService get() = ZLSInstallationsService.getInstance()

private typealias ZLSStorage = UUIDStorage<ZLSVersion.Ref>