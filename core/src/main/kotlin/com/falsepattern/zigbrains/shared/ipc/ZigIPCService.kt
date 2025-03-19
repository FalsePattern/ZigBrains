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
import com.falsepattern.zigbrains.shared.ipc.Payload.Companion.readPayload
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.google.common.io.LittleEndianDataInputStream
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.DataInput
import java.io.EOFException
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream

@Service(Service.Level.PROJECT)
class ZigIPCService(val project: Project) {
    class IPCTreeNode(userObject: Any?): DefaultMutableTreeNode(userObject) {
        var changed: Boolean = true

        override fun add(newChild: MutableTreeNode?) {
            super.add(newChild)
            changed = true
        }

        override fun remove(aChild: MutableTreeNode?) {
            super.remove(aChild)
            changed = true
        }

        override fun remove(childIndex: Int) {
            super.remove(childIndex)
            changed = true
        }
    }
    val nodes = ArrayList<IPCTreeNode>()
    val changed = Channel<Unit>(1)
    val mutex = Mutex()

    private fun DataInput.readRoots(): List<Payload> {
        val len = readByte().toUByte().toInt()
        val payloads = Array<Payload>(len) {
            readPayload()
        }
        val parents = ByteArray(len) {
            readByte()
        }

        val roots = ArrayList<Payload>()
        for (i in 0..len - 1) {
            val parent = parents[i].toUByte()
            val payload = payloads[i]
            if (parent.toUInt() == 255u) {
                roots.add(payloads[i])
            } else {
                payloads[parent.toInt()].children.add(payload)
            }
        }
        return roots
    }

    private suspend fun watch(ipc: IPC, process: Process) {
        val currentNode = IPCTreeNode(BaseNodeDescriptor<Any>(project, "pid: ${process.pid()}", AllIcons.Actions.InlayGear))
        mutex.withLock {
            nodes.add(currentNode)
        }
        withContext(Dispatchers.IO) {
            try {
                LittleEndianDataInputStream(BufferedInputStream(ipc.fifoPath.inputStream())).use { fifo ->
                    while (!project.isDisposed && process.isAlive) {
                        val roots = fifo.readRoots()
                        mutex.withLock {
                            for ((id, root) in roots.withIndex()) {
                                root.addWithChildren(project, currentNode, id)
                            }
                            while (currentNode.childCount > roots.size) {
                                currentNode.remove(currentNode.childCount - 1)
                            }
                        }
                        changed.trySend(Unit)
                    }
                }
            } catch (_: EOFException) {
            } finally {
                mutex.withLock {
                    nodes.remove(currentNode)
                }
                changed.trySend(Unit)
                ipc.fifoPath.deleteIfExists()
            }
        }
    }

    fun launchWatcher(ipc: IPC, process: Process) {
        project.zigCoroutineScope.launch {
            watch(ipc, process)
        }
    }
}

val Project.ipc get() = if (IPCUtil.haveIPC) service<ZigIPCService>() else null