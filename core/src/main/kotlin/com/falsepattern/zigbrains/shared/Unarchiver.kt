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

package com.falsepattern.zigbrains.shared

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.io.Decompressor
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.name

enum class Unarchiver {
    ZIP {
        override val extension = "zip"
        override fun createDecompressor(file: Path) = Decompressor.Zip(file)
    },
    TAR_GZ {
        override val extension = "tar.gz"
        override fun createDecompressor(file: Path) = Decompressor.Tar(file)
    },
    TAR_XZ {
        override val extension = "tar.xz"
        override fun createDecompressor(file: Path) = Decompressor.Tar(file)
    },
    VSIX {
        override val extension = "vsix"
        override fun createDecompressor(file: Path) = Decompressor.Zip(file)
    };

    protected abstract val extension: String
    protected abstract fun createDecompressor(file: Path): Decompressor

    companion object {
        @Throws(IOException::class)
        fun unarchive(archivePath: Path, dst: Path, prefix: String? = null) {
            val unarchiver = entries.find { archivePath.name.endsWith(it.extension) }
                             ?: error("Unexpected archive type: $archivePath")
            val dec = unarchiver.createDecompressor(archivePath)
            val indicator = ProgressManager.getInstance().progressIndicator ?: EmptyProgressIndicator()
            indicator.isIndeterminate = true
            indicator.text = "Extracting archive"
            dec.filter {
                indicator.text2 = it
                indicator.checkCanceled()
                true
            }
            if (prefix != null) {
                dec.removePrefixPath(prefix)
            }
            dec.extract(dst)
        }
    }
}