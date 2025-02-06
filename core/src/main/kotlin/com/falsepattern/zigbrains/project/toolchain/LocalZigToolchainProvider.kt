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

import com.falsepattern.zigbrains.direnv.DirenvCmd
import com.falsepattern.zigbrains.direnv.emptyEnv
import com.falsepattern.zigbrains.project.settings.zigProjectSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.io.toNioPathOrNull
import kotlinx.serialization.json.*
import kotlin.io.path.pathString

class LocalZigToolchainProvider: ZigToolchainProvider<LocalZigToolchain> {
    override suspend fun suggestToolchain(project: Project?, extraData: UserDataHolder): LocalZigToolchain? {
        val env = if (project != null && (extraData.getUserData(LocalZigToolchain.DIRENV_KEY) ?: project.zigProjectSettings.state.direnv)) {
            DirenvCmd.importDirenv(project)
        } else {
            emptyEnv
        }
        val zigExePath = env.findExecutableOnPATH("zig") ?: return null
        return LocalZigToolchain(zigExePath.parent)
    }

    override val serialMarker: String
        get() = "local"

    override fun deserialize(data: JsonElement): LocalZigToolchain? {
        if (data !is JsonObject)
            return null

        val loc = data["location"] as? JsonPrimitive ?: return null
        val path = loc.content.toNioPathOrNull() ?: return null
        return LocalZigToolchain(path)
    }

    override fun canSerialize(toolchain: AbstractZigToolchain): Boolean {
        return toolchain is LocalZigToolchain
    }

    override fun serialize(toolchain: LocalZigToolchain): JsonElement {
        return buildJsonObject {
            put("location", toolchain.location.pathString)
        }
    }
}