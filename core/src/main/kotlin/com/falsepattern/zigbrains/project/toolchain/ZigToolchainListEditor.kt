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

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ui.configuration.SdkPopupFactory
import com.intellij.openapi.ui.MasterDetailsComponent
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.util.IconUtil
import javax.swing.JComponent
import javax.swing.tree.DefaultTreeModel

class ZigToolchainListEditor(): MasterDetailsComponent() {
    private var isTreeInitialized = false

    override fun createComponent(): JComponent {
        if (!isTreeInitialized) {
            initTree()
            isTreeInitialized = true
        }
        return super.createComponent()
    }

    override fun createActions(fromPopup: Boolean): List<AnAction?>? {
        val add = object : DumbAwareAction({"lmaoo"}, Presentation.NULL_STRING, IconUtil.addIcon) {
            override fun actionPerformed(e: AnActionEvent) {
                SdkPopupFactory
                    .newBuilder()
                    .withSdkTypeFilter { it is ZigSDKType }
                    .onSdkSelected {
                        val path = it.homePath?.toNioPathOrNull() ?: return@onSdkSelected
                        val toolchain = LocalZigToolchain(path)
                        zigToolchainList.state = ZigToolchainList(zigToolchainList.state.toolchains + listOf(toolchain))
                        addLocalToolchain(toolchain)
                        (myTree.model as DefaultTreeModel).reload()
                    }
                    .buildPopup()
                    .showPopup(e)
            }
        }
        return listOf(add, MyDeleteAction())
    }

    override fun onItemDeleted(item: Any?) {
        if (item is AbstractZigToolchain) {
            zigToolchainList.state = ZigToolchainList(zigToolchainList.state.toolchains.filter { it != item })
        }
        super.onItemDeleted(item)
    }

    override fun reset() {
        reloadTree()
        super.reset()
    }

    override fun getEmptySelectionString() = ZigBrainsBundle.message("settings.toolchains.empty")

    override fun getDisplayName() = ZigBrainsBundle.message("settings.toolchains.title")

    private fun addLocalToolchain(toolchain: LocalZigToolchain) {
        val node = MyNode(LocalZigToolchainConfigurable(toolchain, ProjectManager.getInstance().defaultProject))
        addNode(node, myRoot)
    }

    private fun reloadTree() {
        myRoot.removeAllChildren()
        zigToolchainList.state.toolchains.forEach { toolchain ->
            if (toolchain is LocalZigToolchain) {
                addLocalToolchain(toolchain)
            }
        }
        (myTree.model as DefaultTreeModel).reload()
    }
}