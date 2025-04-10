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
import com.intellij.openapi.util.io.toNioPathOrNull
import java.nio.file.Path
import com.intellij.util.xmlb.annotations.Attribute
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString

data class ZLSVersion(val path: Path, override val name: String?, val settings: ZLSSettings): NamedObject<ZLSVersion> {
    override fun withName(newName: String?): ZLSVersion {
        return copy(name = newName)
    }

    fun toRef(): Ref {
        return Ref(path.pathString, name, settings)
    }

    fun isValid(): Boolean {
        if (!path.toFile().exists())
            return false
        if (!path.isRegularFile() || !path.isExecutable())
            return false
        return true
    }

    data class Ref(
        @JvmField
        @Attribute
        val path: String? = "",
        @JvmField
        @Attribute
        val name: String? = "",
        @JvmField
        val settings: ZLSSettings = ZLSSettings()
    ) {
        fun resolve(): ZLSVersion? {
            return path?.ifBlank { null }?.toNioPathOrNull()?.let { ZLSVersion(it, name, settings) }
        }
    }
}