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

package com.falsepattern.zigbrains.shared.ipc

import com.falsepattern.zigbrains.project.steps.ui.BaseNodeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.util.asSafely
import java.io.DataInput

data class Payload(val completed: UInt, val estimatedTotal: UInt, val name: String, var children: ArrayList<Payload> = ArrayList()) {
    companion object {
        fun DataInput.readPayload(): Payload {
            val completed = readInt().toUInt()
            val estimatedTotal = readInt().toUInt()
            val name = ByteArray(40) {
                readByte()
            }
            val length = name.indexOf(0).let { if (it == -1) 40 else it }
            val nameText = String(name, 0, length)
            return Payload(completed, estimatedTotal, nameText)
        }
    }

    fun addWithChildren(project: Project, parent: ZigIPCService.IPCTreeNode, index: Int) {
        val text = StringBuilder()
        if (estimatedTotal != 0u) {
            text.append('[').append(completed).append('/').append(estimatedTotal).append("] ")
        } else if (completed != 0u) {
            text.append('[').append(completed).append("] ")
        }
        text.append(name)
        val descriptor = BaseNodeDescriptor<Any>(project, text.toString())
        val self = if (index >= parent.childCount) {
            ZigIPCService.IPCTreeNode(descriptor).also { parent.add(it) }
        } else {
            parent.getChildAt(index).asSafely<ZigIPCService.IPCTreeNode>()?.also {
                (it.userObject as BaseNodeDescriptor<*>).applyFrom(descriptor)
            } ?: ZigIPCService.IPCTreeNode(descriptor).also { parent.add(it) }
        }
        for ((i, child) in children.withIndex()) {
            child.addWithChildren(project, self, i)
        }
        while (self.childCount > children.size) {
            self.remove(self.childCount - 1)
        }
    }
}