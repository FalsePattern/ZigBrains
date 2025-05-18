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

import com.falsepattern.zigbrains.lsp.settings.ZLSSettings
import com.falsepattern.zigbrains.shared.NamedObject
import com.falsepattern.zigbrains.shared.cli.call
import com.falsepattern.zigbrains.shared.cli.createCommandLineSafe
import com.falsepattern.zigbrains.shared.sanitizedPathString
import com.falsepattern.zigbrains.shared.sanitizedToNioPath
import com.intellij.util.text.SemVer
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import java.nio.file.Path
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile

data class ZLSVersion(val path: Path, override val name: String? = null, val settings: ZLSSettings = ZLSSettings()): NamedObject<ZLSVersion> {
    override fun withName(newName: String?): ZLSVersion {
        return copy(name = newName)
    }

    fun toRef(): Ref {
        return Ref(path.sanitizedPathString, name, settings)
    }

    fun isValid(): Boolean {
        if (!path.toFile().exists())
            return false
        if (!path.isRegularFile() || !path.isExecutable())
            return false
        return true
    }

    suspend fun version(): SemVer? {
        if (!isValid())
            return null
        val cli = createCommandLineSafe(null, path, "--version").getOrElse { return null }
        val info = cli.call(5000).getOrElse { return null }
        return SemVer.parseFromText(info.stdout.trim())
    }

    companion object {
        suspend fun tryFromPath(path: Path): ZLSVersion? {
            var zls = ZLSVersion(path)
            if (!zls.isValid())
                return null
            val version = zls.version()?.rawVersion
            if (version != null) {
                zls = zls.copy(name = "ZLS $version")
            }
            return zls
        }
    }

    data class Ref(
        @JvmField
        @Attribute
        val path: String? = "",
        @JvmField
        @Attribute
        val name: String? = "",
        @JvmField
        @Tag
        val settings: ZLSSettings = ZLSSettings()
    ) {
        fun resolve(): ZLSVersion? {
            return path?.sanitizedToNioPath()?.let { ZLSVersion(it, name, settings) }
        }
    }
}