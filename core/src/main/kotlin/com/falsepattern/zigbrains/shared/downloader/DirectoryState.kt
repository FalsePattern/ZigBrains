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

package com.falsepattern.zigbrains.shared.downloader

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

enum class DirectoryState {
    Invalid,
    NotAbsolute,
    NotDirectory,
    NotEmpty,
    CreateNew,
    Ok;

    fun isValid(): Boolean {
        return when(this) {
            Invalid, NotAbsolute, NotDirectory, NotEmpty -> false
            CreateNew, Ok -> true
        }
    }

    companion object {
        @JvmStatic
        fun determine(path: Path?): DirectoryState {
            if (path == null) {
                return Invalid
            }
            if (!path.isAbsolute) {
                return NotAbsolute
            }
            if (!path.exists()) {
                var parent: Path? = path.parent
                while(parent != null) {
                    if (!parent.exists()) {
                        parent = parent.parent
                        continue
                    }
                    if (!parent.isDirectory()) {
                        return NotDirectory
                    }
                    return CreateNew
                }
                return Invalid
            }
            if (!path.isDirectory()) {
                return NotDirectory
            }
            val isEmpty = Files.newDirectoryStream(path).use { !it.iterator().hasNext() }
            if (!isEmpty) {
                return NotEmpty
            }
            return Ok
        }
    }
}