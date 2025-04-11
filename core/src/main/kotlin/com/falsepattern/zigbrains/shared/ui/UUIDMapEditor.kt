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

package com.falsepattern.zigbrains.shared.ui

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.shared.StorageChangeListener
import com.falsepattern.zigbrains.shared.coroutine.asContextElement
import com.falsepattern.zigbrains.shared.coroutine.launchWithEDT
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.observable.util.whenListChanged
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.MasterDetailsComponent
import com.intellij.util.IconUtil
import com.intellij.util.asSafely
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.launch
import java.util.*
import javax.swing.JComponent
import javax.swing.tree.DefaultTreeModel

abstract class UUIDMapEditor<T>(val driver: UUIDComboBoxDriver<T>): MasterDetailsComponent() {
    private var isTreeInitialized = false
    private var registered: Boolean = false
    private var selectOnNextReload: UUID? = null
    private var disposed: Boolean = false
    private val changeListener: StorageChangeListener = { this@UUIDMapEditor.listChanged() }

    override fun createComponent(): JComponent {
        if (!isTreeInitialized) {
            initTree()
            isTreeInitialized = true
        }
        if (!registered) {
            driver.theMap.addChangeListener(changeListener)
            registered = true
        }
        return super.createComponent()
    }

    override fun createActions(fromPopup: Boolean): List<AnAction> {
        val add = object : DumbAwareAction({ ZigBrainsBundle.message("settings.shared.list.add-action.name") }, Presentation.NULL_STRING, IconUtil.addIcon) {
            override fun actionPerformed(e: AnActionEvent) {
                zigCoroutineScope.launchWithEDT(ModalityState.current()) {
                    if (disposed)
                        return@launchWithEDT
                    val modelList = driver.constructModelList()
                    val model = ZBModel(modelList)
                    val context = driver.createContext(model)
                    val popup = ZBComboBoxPopup(context, null, ::onItemSelected)
                    model.whenListChanged {
                        popup.syncWithModelChange()
                    }
                    popup.showInBestPositionFor(e.dataContext)
                }
            }
        }
        return listOf(add, MyDeleteAction())
    }

    override fun onItemDeleted(item: Any?) {
        if (item is UUID) {
            driver.theMap.remove(item)
        }
        super.onItemDeleted(item)
    }

    private fun onItemSelected(elem: ListElem<T>) {
        if (elem !is ListElem.Pseudo)
            return
        zigCoroutineScope.launch(myWholePanel.asContextElement()) {
            if (disposed)
                return@launch
            val uuid = driver.resolvePseudo(myWholePanel, elem)
            if (uuid != null) {
                withEDTContext(myWholePanel.asContextElement()) {
                    applyUUIDNowOrOnReload(uuid)
                }
            }
        }
    }

    override fun reset() {
        reloadTree()
        super.reset()
    }

    override fun getEmptySelectionString() = ZigBrainsBundle.message("settings.shared.list.empty")

    override fun disposeUIResources() {
        disposed = true
        super.disposeUIResources()
        if (registered) {
            driver.theMap.removeChangeListener(changeListener)
        }
    }

    private fun addElem(uuid: UUID, elem: T) {
        val node = MyNode(driver.createNamedConfigurable(uuid, elem))
        addNode(node, myRoot)
    }

    private fun reloadTree() {
        if (disposed)
            return
        val currentSelection = selectedObject?.asSafely<UUID>()
        selectedNode = null
        myRoot.removeAllChildren()
        (myTree.model as DefaultTreeModel).reload()
        val onReload = selectOnNextReload
        selectOnNextReload = null
        var hasOnReload = false
        driver.theMap.forEach { (uuid, elem) ->
            addElem(uuid, elem)
            if (uuid == onReload) {
                hasOnReload = true
            }
        }
        (myTree.model as DefaultTreeModel).reload()
        if (hasOnReload) {
            selectedNode = findNodeByObject(myRoot, onReload)
            return
        }
        selectedNode = currentSelection?.let { findNodeByObject(myRoot, it) }
    }

    @RequiresEdt
    private fun applyUUIDNowOrOnReload(uuid: UUID?) {
        selectNodeInTree(uuid)
        val currentSelection = selectedObject?.asSafely<UUID>()
        if (uuid != null && uuid != currentSelection) {
            selectOnNextReload = uuid
        } else {
            selectOnNextReload = null
        }
    }

    private suspend fun listChanged() {
        if (disposed)
            return
        withEDTContext(myWholePanel.asContextElement()) {
            reloadTree()
        }
    }
}