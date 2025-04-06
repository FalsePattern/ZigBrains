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

package com.falsepattern.zigbrains.project.settings

import com.falsepattern.zigbrains.project.toolchain.local.LocalZigToolchain
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.util.xmlb.annotations.Transient
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString

data class ZigProjectSettings(
    var direnv: Boolean = false,
    var overrideStdPath: Boolean = false,
    var explicitPathToStd: String? = null,
    var toolchainPath: String? = null
): ZigProjectConfigurationProvider.Settings, ZigProjectConfigurationProvider.ToolchainProvider {
    override fun apply(project: Project) {
        project.zigProjectSettings.loadState(this)
    }

    @get:Transient
    @set:Transient
    override var toolchain: LocalZigToolchain?
        get() {
            val nioPath = toolchainPath?.toNioPathOrNull() ?: return null
            if (!nioPath.isDirectory()) {
                return null
            }
            return LocalZigToolchain(nioPath)
        }
        set(value) {
            toolchainPath = value?.location?.pathString
        }
}
