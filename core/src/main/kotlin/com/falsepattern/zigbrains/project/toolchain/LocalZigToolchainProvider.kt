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
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.SdkPopupBuilder
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.util.EnvironmentUtil
import com.intellij.util.system.OS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.*
import java.io.File
import java.util.UUID
import kotlin.io.path.pathString

class LocalZigToolchainProvider: ZigToolchainProvider {
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

    override fun deserialize(data: Map<String, String>): AbstractZigToolchain? {
        val location = data["location"]?.toNioPathOrNull() ?: return null
        val std = data["std"]?.toNioPathOrNull()
        val name = data["name"]
        return LocalZigToolchain(location, std, name)
    }

    override fun isCompatible(toolchain: AbstractZigToolchain): Boolean {
        return toolchain is LocalZigToolchain
    }

    override fun serialize(toolchain: AbstractZigToolchain): Map<String, String> {
        toolchain as LocalZigToolchain
        val map = HashMap<String, String>()
        toolchain.location.pathString.let { map["location"] = it }
        toolchain.std?.pathString?.let { map["std"] = it }
        toolchain.name?.let { map["name"] = it }
        return map
    }

    override fun matchesSuggestion(
        toolchain: AbstractZigToolchain,
        suggestion: AbstractZigToolchain
    ): Boolean {
        toolchain as LocalZigToolchain
        suggestion as LocalZigToolchain
        return toolchain.location == suggestion.location
    }

    override fun createConfigurable(
        uuid: UUID,
        toolchain: AbstractZigToolchain,
        project: Project
    ): NamedConfigurable<UUID> {
        toolchain as LocalZigToolchain
        return LocalZigToolchainConfigurable(uuid, toolchain, project)
    }

    override fun suggestToolchains(): List<AbstractZigToolchain> {
        val res = HashSet<String>()
        EnvironmentUtil.getValue("PATH")?.split(File.pathSeparatorChar)?.let { res.addAll(it.toList()) }
        return res.mapNotNull { LocalZigToolchain.tryFromPathString(it) }
    }
}