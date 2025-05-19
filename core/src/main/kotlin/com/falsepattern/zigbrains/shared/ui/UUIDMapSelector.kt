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
import com.falsepattern.zigbrains.project.toolchain.zigToolchainList
import com.falsepattern.zigbrains.shared.StorageChangeListener
import com.falsepattern.zigbrains.shared.coroutine.asContextElement
import com.falsepattern.zigbrains.shared.coroutine.launchWithEDT
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.observable.util.whenListChanged
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Row
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.event.ItemEvent
import java.util.UUID
import javax.swing.JButton

abstract class UUIDMapSelector<T>(val driver: UUIDComboBoxDriver<T>): Disposable {
    private val comboBox: ZBComboBox<T>
    private var selectOnNextReload: UUID? = null
    private val model: ZBModel<T>
    private var editButton: JButton? = null
    private val changeListener: StorageChangeListener = { this@UUIDMapSelector.listChanged() }
    init {
        model = ZBModel(emptyList())
        comboBox = driver.createComboBox(model)
        comboBox.addItemListener(::itemStateChanged)
        driver.theMap.addChangeListener(changeListener)
        model.whenListChanged {
            zigCoroutineScope.launchWithEDT(comboBox.asContextElement()) {
                tryReloadSelection()
            }
            if (comboBox.isPopupVisible) {
                comboBox.isPopupVisible = false
                comboBox.isPopupVisible = true
            }
        }
        zigCoroutineScope.launchWithEDT(ModalityState.any()) {
            model.updateContents(driver.constructModelList())
        }
    }

    protected var selectedUUID: UUID?
        get() = comboBox.selectedUUID
        set(value) {
            zigCoroutineScope.launchWithEDT(ModalityState.any()) {
                applyUUIDNowOrOnReload(value)
            }
        }

    protected val isEmpty: Boolean get() = model.isEmpty

    protected open fun onSelection(uuid: UUID?) {}

    private fun refreshButtonState(item: ListElem<*>) {
        val actual = item is ListElem.One.Actual<*>
        editButton?.isEnabled = actual
        editButton?.repaint()
        onSelection(if (actual) (item as ListElem.One.Actual<*>).uuid else null)
    }

    private fun itemStateChanged(event: ItemEvent) {
        if (event.stateChange != ItemEvent.SELECTED) {
            return
        }
        val item = event.item
        if (item !is ListElem<*>)
            return
        refreshButtonState(item)
        if (item !is ListElem.Pseudo<*>)
            return
        @Suppress("UNCHECKED_CAST")
        item as ListElem.Pseudo<T>
        zigCoroutineScope.launch(comboBox.asContextElement()) {
            val uuid = runCatching { driver.resolvePseudo(comboBox, item) }.getOrNull()
            delay(100)
            withEDTContext(comboBox.asContextElement()) {
                applyUUIDNowOrOnReload(uuid)
            }
        }
    }

    @RequiresEdt
    private fun tryReloadSelection() {
        val list = model.toList()
        if (list.size == 1) {
            comboBox.selectedItem = list[0]
            comboBox.isEnabled = false
            return
        }
        comboBox.isEnabled = true
        val onReload = selectOnNextReload
        selectOnNextReload = null
        if (onReload != null) {
            val element = list.firstOrNull { when(it) {
                is ListElem.One.Actual<*> -> it.uuid == onReload
                else -> false
            } }
            if (element == null) {
                selectOnNextReload = onReload
            } else {
                comboBox.selectedItem = element
                return
            }
        }
        val selected = model.selected
        if (selected != null && list.contains(selected)) {
            comboBox.selectedItem = selected
            return
        }
        if (selected is ListElem.One.Actual<*>) {
            val uuid = selected.uuid
            val element = list.firstOrNull { when(it) {
                is ListElem.One.Actual -> it.uuid == uuid
                else -> false
            } }
            comboBox.selectedItem = element
            return
        }
        comboBox.selectedItem = ListElem.None<Any>()
    }

    protected suspend fun listChanged() {
        withContext(Dispatchers.EDT + comboBox.asContextElement()) {
            val list = driver.constructModelList()
            model.updateContents(list)
            tryReloadSelection()
        }
    }

    protected fun attachComboBoxRow(row: Row): Unit = with(row) {
        cell(comboBox).resizableColumn().align(AlignX.FILL)
        button(ZigBrainsBundle.message("settings.toolchain.editor.toolchain.edit-button.name")) {
            zigCoroutineScope.launchWithEDT(comboBox.asContextElement()) {
                var selectedUUID = comboBox.selectedUUID ?: return@launchWithEDT
                val elem = driver.theMap[selectedUUID] ?: return@launchWithEDT
                val config = driver.createNamedConfigurable(selectedUUID, elem)
                val apply = ShowSettingsUtil.getInstance().editConfigurable(DialogWrapper.findInstance(comboBox)?.contentPane, config)
                if (apply) {
                    applyUUIDNowOrOnReload(selectedUUID)
                }
            }
        }.component.let {
            editButton = it
        }
    }

    @RequiresEdt
    private fun applyUUIDNowOrOnReload(uuid: UUID?) {
        comboBox.selectedUUID = uuid
        if (uuid != null && comboBox.selectedUUID == null) {
            selectOnNextReload = uuid
        } else {
            selectOnNextReload = null
        }
    }

    override fun dispose() {
        zigToolchainList.removeChangeListener(changeListener)
    }
}