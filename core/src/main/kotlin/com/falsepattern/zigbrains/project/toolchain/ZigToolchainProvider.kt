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

import com.falsepattern.zigbrains.project.toolchain.ZigToolchainProvider.Companion.EXTENSION_POINT_NAME
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.util.asSafely
import com.intellij.util.xmlb.Converter
import kotlinx.serialization.json.*

sealed interface ZigToolchainProvider<in T: AbstractZigToolchain> {
    suspend fun suggestToolchain(project: Project?, extraData: UserDataHolder): AbstractZigToolchain?

    val serialMarker: String
    fun deserialize(data: JsonElement): AbstractZigToolchain?
    fun canSerialize(toolchain: AbstractZigToolchain): Boolean
    fun serialize(toolchain: T): JsonElement

    companion object {
        val EXTENSION_POINT_NAME = ExtensionPointName.create<ZigToolchainProvider<*>>("com.falsepattern.zigbrains.toolchainProvider")

        suspend fun suggestToolchain(project: Project?, extraData: UserDataHolder): AbstractZigToolchain? {
            return EXTENSION_POINT_NAME.extensionList.firstNotNullOfOrNull { it.suggestToolchain(project, extraData) }
        }

        fun fromJson(json: JsonObject): AbstractZigToolchain? {
            val marker = (json["marker"] as? JsonPrimitive)?.contentOrNull ?: return null
            val data = json["data"] ?: return null
            val provider = EXTENSION_POINT_NAME.extensionList.find { it.serialMarker == marker } ?: return null
            return provider.deserialize(data)
        }

        fun toJson(tc: AbstractZigToolchain): JsonObject? {
            val provider = EXTENSION_POINT_NAME.extensionList.find { it.canSerialize(tc) } ?: return null
            return buildJsonObject {
                put("marker", provider.serialMarker)
                put("data", provider.serialize(tc))
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T: AbstractZigToolchain> ZigToolchainProvider<T>.serialize(toolchain: AbstractZigToolchain) = serialize(toolchain as T)

class ZigToolchainConverter: Converter<AbstractZigToolchain>() {
    override fun fromString(value: String): AbstractZigToolchain? {
        val json = Json.parseToJsonElement(value) as? JsonObject ?: return null
        return ZigToolchainProvider.fromJson(json)
    }

    override fun toString(value: AbstractZigToolchain): String? {
        return ZigToolchainProvider.toJson(value)?.toString()
    }
}

class ZigToolchainListConverter: Converter<List<AbstractZigToolchain>>() {
    override fun fromString(value: String): List<AbstractZigToolchain> {
        val json = Json.parseToJsonElement(value) as? JsonArray ?: return emptyList()
        return json.mapNotNull { it.asSafely<JsonObject>()?.let { ZigToolchainProvider.fromJson(it) } }
    }

    override fun toString(value: List<AbstractZigToolchain>): String {
        return buildJsonArray {
            value.mapNotNull { ZigToolchainProvider.toJson(it) }.forEach {
                add(it)
            }
        }.toString()
    }
}