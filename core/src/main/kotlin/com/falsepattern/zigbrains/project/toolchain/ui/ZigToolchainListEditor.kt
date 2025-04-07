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

package com.falsepattern.zigbrains.project.toolchain.ui

import com.falsepattern.zigbrains.Icons
import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainListService
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.base.createNamedConfigurable
import com.falsepattern.zigbrains.project.toolchain.base.suggestZigToolchains
import com.falsepattern.zigbrains.project.toolchain.downloader.Downloader
import com.falsepattern.zigbrains.project.toolchain.local.LocalZigToolchain
import com.falsepattern.zigbrains.shared.coroutine.asContextElement
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.MasterDetailsComponent
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.Consumer
import com.intellij.util.IconUtil
import com.intellij.util.asSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.tree.DefaultTreeModel

class ZigToolchainListEditor : MasterDetailsComponent(), ZigToolchainListService.ToolchainListChangeListener {
    private var isTreeInitialized = false
    private var registered: Boolean = false
    private var itemSelectedListeners = ArrayList<Consumer<UUID?>>()

    fun addItemSelectedListener(c: Consumer<UUID?>) {
        synchronized(itemSelectedListeners) {
            itemSelectedListeners.add(c)
        }
    }

    override fun createComponent(): JComponent {
        if (!isTreeInitialized) {
            initTree()
            isTreeInitialized = true
        }
        if (!registered) {
            ZigToolchainListService.getInstance().addChangeListener(this)
            registered = true
        }
        return super.createComponent()
    }

    override fun createActions(fromPopup: Boolean): List<AnAction> {
        val add = object : DumbAwareAction({ "lmaoo" }, Presentation.NULL_STRING, IconUtil.addIcon) {
            override fun actionPerformed(e: AnActionEvent) {
                val modelList = ArrayList<TCListElemIn>()
                modelList.addAll(TCListElem.fetchGroup)
                modelList.add(Separator("Detected toolchains", true))
                modelList.addAll(suggestZigToolchains().map { it.asSuggested() })
                val model = TCModel.Companion(modelList)
                val context = TCContext(null, model)
                val popup = TCComboBoxPopup(context, null, ::onItemSelected)
                popup.showInBestPositionFor(e.dataContext)
            }
        }
        return listOf(add, MyDeleteAction())
    }

    override fun updateSelection(configurable: NamedConfigurable<*>?) {
        super.updateSelection(configurable)
        val uuid = configurable?.editableObject as? UUID
        synchronized(itemSelectedListeners) {
            itemSelectedListeners.forEach { it.consume(uuid) }
        }
    }

    override fun onItemDeleted(item: Any?) {
        if (item is UUID) {
            ZigToolchainListService.getInstance().removeToolchain(item)
        }
        super.onItemDeleted(item)
    }

    private fun onItemSelected(elem: TCListElem) {
        if (elem !is TCListElem.Pseudo)
            return
        zigCoroutineScope.launch(myWholePanel.asContextElement()) {
            val uuid = ZigToolchainComboBoxHandler.onItemSelected(myWholePanel, elem)
            if (uuid != null) {
                withEDTContext(myWholePanel.asContextElement()) {
                    selectNodeInTree(uuid)
                }
            }
        }
    }

    override fun reset() {
        reloadTree()
        super.reset()
    }

    override fun getEmptySelectionString() = ZigBrainsBundle.message("settings.toolchains.empty")

    override fun getDisplayName() = ZigBrainsBundle.message("settings.toolchains.title")

    private fun addToolchain(uuid: UUID, toolchain: ZigToolchain) {
        val node = MyNode(toolchain.createNamedConfigurable(uuid))
        addNode(node, myRoot)
    }

    private fun reloadTree() {
        val currentSelection = selectedObject?.asSafely<UUID>()
        myRoot.removeAllChildren()
        ZigToolchainListService.getInstance().toolchains.forEach { (uuid, toolchain) ->
            addToolchain(uuid, toolchain)
        }
        (myTree.model as DefaultTreeModel).reload()
        currentSelection?.let {
            selectNodeInTree(it)
        }
    }

    override fun disposeUIResources() {
        super.disposeUIResources()
        if (registered) {
            ZigToolchainListService.getInstance().removeChangeListener(this)
        }
    }

    override suspend fun toolchainListChanged() {
        withEDTContext(myWholePanel.asContextElement()) {
            reloadTree()
        }
    }
}