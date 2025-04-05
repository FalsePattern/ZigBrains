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

import com.falsepattern.zigbrains.direnv.emptyEnv
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.AdditionalDataConfigurable
import com.intellij.openapi.projectRoots.SdkAdditionalData
import com.intellij.openapi.projectRoots.SdkModel
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.EnvironmentUtil
import com.intellij.util.asSafely
import com.intellij.util.system.OS
import kotlinx.coroutines.runBlocking
import org.jdom.Element
import org.jetbrains.annotations.Nls
import java.io.File
import kotlin.io.path.pathString

class ZigSDKType: SdkType("Zig") {
    override fun suggestHomePath(): String? {
        return null
    }

    private fun getPathEnv(path: String): ZigToolchainEnvironmentSerializable? {
        return LocalZigToolchain.tryFromPathString(path)?.zig?.let { runBlocking { it.getEnv(null) } }?.getOrNull()
    }

    override fun isValidSdkHome(path: String): Boolean {
        return LocalZigToolchain.tryFromPathString(path) != null
    }

    override fun suggestSdkName(currentSdkName: String?, sdkHome: String): String {
        return getVersionString(sdkHome)?.let { "Zig $it" } ?: currentSdkName ?: "Zig"
    }

    override fun getVersionString(sdkHome: String): String? {
        return getPathEnv(sdkHome)?.version
    }

    override fun suggestHomePaths(): Collection<String?> {
        val res = HashSet<String>()
        EnvironmentUtil.getValue("PATH")?.split(File.pathSeparatorChar)?.let { res.addAll(it.toList()) }
        if (OS.CURRENT != OS.Windows) {
            EnvironmentUtil.getValue("HOME")?.let { res.add("$it/.local/share/zigup") }
            EnvironmentUtil.getValue("XDG_DATA_HOME")?.let { res.add("$it/zigup") }
        }
        return res
    }

    override fun createAdditionalDataConfigurable(
        sdkModel: SdkModel,
        sdkModificator: SdkModificator
    ): AdditionalDataConfigurable? {
        return null
    }

    override fun getPresentableName(): @Nls(capitalization = Nls.Capitalization.Title) String {
        return "Zig"
    }

    override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {

    }

    override fun isRelevantForFile(project: Project, file: VirtualFile): Boolean {
        return file.extension == "zig" || file.extension == "zon"
    }
}