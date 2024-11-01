/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2024 FalsePattern
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

import com.falsepattern.zigbrains.project.toolchain.ZigToolchainProvider.Companion.EXTENSION_POINT_NAME
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.Converter
import kotlinx.serialization.json.*

sealed interface ZigToolchainProvider {
    suspend fun getToolchain(project: Project?): ZigToolchain?

    val serialMarker: String
    fun deserialize(data: JsonElement): ZigToolchain?
    fun canSerialize(toolchain: ZigToolchain): Boolean
    fun serialize(toolchain: ZigToolchain): JsonElement

    companion object {
        val EXTENSION_POINT_NAME = ExtensionPointName.create<ZigToolchainProvider>("com.falsepattern.zigbrains.toolchainProvider")

        suspend fun findToolchains(project: Project?): ZigToolchain? {
            return EXTENSION_POINT_NAME.extensionList.firstNotNullOfOrNull { it.getToolchain(project) }
        }
    }
}

class ZigToolchainConverter: Converter<ZigToolchain>() {
    override fun fromString(value: String): ZigToolchain? {
        val json = Json.parseToJsonElement(value) as? JsonObject ?: return null
        val marker = (json["marker"] as? JsonPrimitive)?.contentOrNull ?: return null
        val data = json["data"] ?: return null
        val provider = EXTENSION_POINT_NAME.extensionList.find { it.serialMarker == marker } ?: return null
        return provider.deserialize(data)
    }

    override fun toString(value: ZigToolchain): String? {
        val provider = EXTENSION_POINT_NAME.extensionList.find { it.canSerialize(value) } ?: return null
        return buildJsonObject {
            put("marker", provider.serialMarker)
            put("data", provider.serialize(value))
        }.toString()
    }

}