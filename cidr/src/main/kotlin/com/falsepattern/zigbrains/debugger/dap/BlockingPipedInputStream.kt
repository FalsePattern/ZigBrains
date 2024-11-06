/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2024 FalsePattern
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

package com.falsepattern.zigbrains.debugger.dap

import java.io.IOException
import java.io.InterruptedIOException
import java.io.PipedInputStream
import java.io.PipedOutputStream

class BlockingPipedInputStream(src: PipedOutputStream, pipeSize: Int) : PipedInputStream(src, pipeSize) {
    var closed = false

    override fun read(): Int {
        if (closed) {
            throw IOException("stream closed")
        } else {
            while (super.`in` < 0) {
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                this as java.lang.Object

                this.notifyAll()

                try {
                    this.wait(750)
                } catch (e: InterruptedException) {
                    throw InterruptedIOException()
                }
            }

            val ret = buffer[this.out++].toUInt()
            if (this.out >= buffer.size) {
                this.out = 0
            }

            if (this.`in` == this.out) {
                this.`in` = -1
            }

            return ret.toInt()
        }
    }

    override fun close() {
        closed = true
        super.close()
    }
}