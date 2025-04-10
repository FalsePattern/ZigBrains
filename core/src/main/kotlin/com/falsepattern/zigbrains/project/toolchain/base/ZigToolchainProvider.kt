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

package com.falsepattern.zigbrains.project.toolchain.base

import com.falsepattern.zigbrains.direnv.DirenvState
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainListService
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.ui.SimpleColoredComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import java.util.UUID

private val EXTENSION_POINT_NAME = ExtensionPointName.create<ZigToolchainProvider>("com.falsepattern.zigbrains.toolchainProvider")

internal interface ZigToolchainProvider {
    val serialMarker: String
    fun isCompatible(toolchain: ZigToolchain): Boolean
    fun deserialize(data: Map<String, String>): ZigToolchain?
    fun serialize(toolchain: ZigToolchain): Map<String, String>
    fun matchesSuggestion(toolchain: ZigToolchain, suggestion: ZigToolchain): Boolean
    fun createConfigurable(uuid: UUID, toolchain: ZigToolchain): ZigToolchainConfigurable<*>
    suspend fun suggestToolchains(project: Project?, data: UserDataHolder): Flow<ZigToolchain>
    fun render(toolchain: ZigToolchain, component: SimpleColoredComponent, isSuggestion: Boolean, isSelected: Boolean)
}

fun ZigToolchain.Ref.resolve(): ZigToolchain? {
    val marker = this.marker ?: return null
    val data = this.data ?: return null
    val provider = EXTENSION_POINT_NAME.extensionList.find { it.serialMarker == marker } ?: return null
    return provider.deserialize(data)
}

fun ZigToolchain.toRef(): ZigToolchain.Ref {
    val provider = EXTENSION_POINT_NAME.extensionList.find { it.isCompatible(this) } ?: throw IllegalStateException()
    return ZigToolchain.Ref(provider.serialMarker, provider.serialize(this))
}

fun ZigToolchain.createNamedConfigurable(uuid: UUID): ZigToolchainConfigurable<*> {
    val provider = EXTENSION_POINT_NAME.extensionList.find { it.isCompatible(this) } ?: throw IllegalStateException()
    return provider.createConfigurable(uuid, this)
}

@OptIn(ExperimentalCoroutinesApi::class)
fun suggestZigToolchains(project: Project? = null, data: UserDataHolder = emptyData): Flow<ZigToolchain> {
    val existing = ZigToolchainListService.getInstance().toolchains.map { (_, tc) -> tc }.toList()
    return EXTENSION_POINT_NAME.extensionList.asFlow().flatMapConcat { ext ->
        val compatibleExisting = existing.filter { ext.isCompatible(it) }
        val suggestions = ext.suggestToolchains(project, data)
        suggestions.filter { suggestion ->
            compatibleExisting.none { existing -> ext.matchesSuggestion(existing, suggestion) }
        }
    }.flowOn(Dispatchers.IO)
}

fun ZigToolchain.render(component: SimpleColoredComponent, isSuggestion: Boolean, isSelected: Boolean) {
    val provider = EXTENSION_POINT_NAME.extensionList.find { it.isCompatible(this) } ?: throw IllegalStateException()
    return provider.render(this, component, isSuggestion, isSelected)
}

private val emptyData = object: UserDataHolder {
    override fun <T : Any?> getUserData(key: Key<T?>): T? {
        return null
    }

    override fun <T : Any?> putUserData(key: Key<T?>, value: T?) {

    }

}