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

package com.falsepattern.zigbrains.project.toolchain.local

import com.falsepattern.zigbrains.direnv.DirenvCmd
import com.falsepattern.zigbrains.direnv.emptyEnv
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchainConfigurable
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchainProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.EnvironmentUtil
import com.intellij.util.system.OS
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString

class LocalZigToolchainProvider: ZigToolchainProvider {
    override suspend fun suggestToolchain(project: Project?, extraData: UserDataHolder): LocalZigToolchain? {
        //TODO direnv
//        val env = if (project != null && (extraData.getUserData(LocalZigToolchain.DIRENV_KEY) ?: project.zigProjectSettings.state.direnv)) {
//            DirenvCmd.importDirenv(project)
//        } else {
//            emptyEnv
//        }
        val env = emptyEnv
        val zigExePath = env.findExecutableOnPATH("zig") ?: return null
        return LocalZigToolchain(zigExePath.parent)
    }

    override val serialMarker: String
        get() = "local"

    override fun deserialize(data: Map<String, String>): ZigToolchain? {
        val location = data["location"]?.toNioPathOrNull() ?: return null
        val std = data["std"]?.toNioPathOrNull()
        val name = data["name"]
        return LocalZigToolchain(location, std, name)
    }

    override fun isCompatible(toolchain: ZigToolchain): Boolean {
        return toolchain is LocalZigToolchain
    }

    override fun serialize(toolchain: ZigToolchain): Map<String, String> {
        toolchain as LocalZigToolchain
        val map = HashMap<String, String>()
        toolchain.location.pathString.let { map["location"] = it }
        toolchain.std?.pathString?.let { map["std"] = it }
        toolchain.name?.let { map["name"] = it }
        return map
    }

    override fun matchesSuggestion(
        toolchain: ZigToolchain,
        suggestion: ZigToolchain
    ): Boolean {
        toolchain as LocalZigToolchain
        suggestion as LocalZigToolchain
        return toolchain.location == suggestion.location
    }

    override fun createConfigurable(
        uuid: UUID,
        toolchain: ZigToolchain
    ): ZigToolchainConfigurable<*> {
        toolchain as LocalZigToolchain
        return LocalZigToolchainConfigurable(uuid, toolchain)
    }

    override fun suggestToolchains(): List<ZigToolchain> {
        val res = HashSet<String>()
        EnvironmentUtil.getValue("PATH")?.split(File.pathSeparatorChar)?.let { res.addAll(it.toList()) }
        val wellKnown = getWellKnown()
        wellKnown.forEach { dir ->
            if (!dir.isDirectory())
                return@forEach
            runCatching {
                Files.newDirectoryStream(dir).use { stream ->
                    stream.forEach { subDir ->
                        res.add(subDir.pathString)
                    }
                }
            }
        }
        return res.mapNotNull { LocalZigToolchain.tryFromPathString(it) }
    }

    override fun render(toolchain: ZigToolchain, component: SimpleColoredComponent, isSuggestion: Boolean) {
        toolchain as LocalZigToolchain
        val path = presentDetectedPath(toolchain.location.pathString)
        val name = toolchain.name
        val primary: String
        val secondary: String?
        if (isSuggestion) {
            primary = path
            secondary = name
        } else {
            primary = name ?: "Zig"
            secondary = path
        }
        component.append(primary)
        if (secondary != null) {
            component.append(" ")
            component.append(secondary, SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
    }
}

fun getSuggestedLocalToolchainPath(): Path? {
    return getWellKnown().getOrNull(0)
}

private fun getWellKnown(): List<Path> {
    val home = System.getProperty("user.home")?.toNioPathOrNull() ?: return emptyList()
    val xdgDataHome = when(OS.CURRENT) {
        OS.macOS -> home.resolve("Library")
        OS.Windows -> System.getenv("LOCALAPPDATA")?.toNioPathOrNull()
        else -> System.getenv("XDG_DATA_HOME")?.toNioPathOrNull() ?: home.resolve(Path.of(".local", "share"))
    }
    val res = ArrayList<Path>()
    if (xdgDataHome != null && xdgDataHome.isDirectory()) {
        res.add(xdgDataHome.resolve("zig"))
        res.add(xdgDataHome.resolve("zigup"))
    }
    res.add(home.resolve(".zig"))
    return res
}

private fun presentDetectedPath(home: String, maxLength: Int = 50, suffixLength: Int = 30): String {
    //for macOS, let's try removing Bundle internals
    var home = home
    home = StringUtil.trimEnd(home, "/Contents/Home") //NON-NLS
    home = StringUtil.trimEnd(home, "/Contents/MacOS") //NON-NLS
    home = FileUtil.getLocationRelativeToUserHome(home, false)
    home = StringUtil.shortenTextWithEllipsis(home, maxLength, suffixLength)
    return home
}