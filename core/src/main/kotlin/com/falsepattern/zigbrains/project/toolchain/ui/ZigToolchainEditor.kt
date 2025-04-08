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

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainListService
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainService
import com.falsepattern.zigbrains.project.toolchain.base.suggestZigToolchains
import com.falsepattern.zigbrains.shared.coroutine.asContextElement
import com.falsepattern.zigbrains.shared.coroutine.launchWithEDT
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.options.newEditor.SettingsDialog
import com.intellij.openapi.options.newEditor.SettingsTreeView
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.awt.event.ItemEvent
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.UUID
import javax.swing.JComponent
import kotlin.collections.addAll

class ZigToolchainEditor(private val project: Project): Configurable {
    private var myUi: UI? = null
    override fun getDisplayName(): @NlsContexts.ConfigurableName String? {
        return ZigBrainsBundle.message("settings.toolchain.editor.display-name")
    }

    override fun createComponent(): JComponent? {
        if (myUi != null) {
            disposeUIResources()
        }
        val ui = UI()
        myUi = ui
        return panel {
            ui.attach(this)
        }
    }

    override fun isModified(): Boolean {
        return myUi?.isModified() == true
    }

    override fun apply() {
        myUi?.apply()
    }

    override fun reset() {
        myUi?.reset()
    }

    override fun disposeUIResources() {
        myUi?.let { Disposer.dispose(it) }
        myUi = null
        super.disposeUIResources()
    }

    inner class UI(): Disposable, ZigToolchainListService.ToolchainListChangeListener {
        private val toolchainBox: TCComboBox
        private var selectOnNextReload: UUID? = null
        private val model: TCModel
        init {
            model = TCModel(getModelList())
            toolchainBox = TCComboBox(model)
            toolchainBox.addItemListener(::itemStateChanged)
            ZigToolchainListService.getInstance().addChangeListener(this)
            reset()
        }

        private fun itemStateChanged(event: ItemEvent) {
            if (event.stateChange != ItemEvent.SELECTED) {
                return
            }
            val item = event.item
            if (item !is TCListElem.Pseudo)
                return
            zigCoroutineScope.launch(toolchainBox.asContextElement()) {
                val uuid = ZigToolchainComboBoxHandler.onItemSelected(toolchainBox, item)
                withEDTContext(toolchainBox.asContextElement()) {
                    applyUUIDNowOrOnReload(uuid)
                }
            }
        }

        override suspend fun toolchainListChanged() {
            withContext(Dispatchers.EDT + toolchainBox.asContextElement()) {
                val list = getModelList()
                model.updateContents(list)
                val onReload = selectOnNextReload
                selectOnNextReload = null
                if (onReload != null) {
                    val element = list.firstOrNull { when(it) {
                        is TCListElem.Toolchain.Actual -> it.uuid == onReload
                        else -> false
                    } }
                    model.selectedItem = element
                    return@withContext
                }
                val selected = model.selected
                if (selected != null && list.contains(selected)) {
                    model.selectedItem = selected
                    return@withContext
                }
                if (selected is TCListElem.Toolchain.Actual) {
                    val uuid = selected.uuid
                    val element = list.firstOrNull { when(it) {
                        is TCListElem.Toolchain.Actual -> it.uuid == uuid
                        else -> false
                    } }
                    model.selectedItem = element
                    return@withContext
                }
                model.selectedItem = TCListElem.None
            }
        }

        fun attach(p: Panel): Unit = with(p) {
            row(ZigBrainsBundle.message("settings.toolchain.editor.toolchain.label")) {
                cell(toolchainBox).resizableColumn().align(AlignX.FILL)
                button(ZigBrainsBundle.message("settings.toolchain.editor.toolchain.edit-button.name")) { e ->
                    zigCoroutineScope.launchWithEDT(toolchainBox.asContextElement()) {
                        val config = ZigToolchainListEditor()
                        var inited = false
                        var selectedUUID: UUID? = toolchainBox.selectedToolchain
                        config.addItemSelectedListener {
                            if (inited) {
                                selectedUUID = it
                            }
                        }
                        val apply = ShowSettingsUtil.getInstance().editConfigurable(DialogWrapper.findInstance(toolchainBox)?.contentPane, config) {
                            config.selectNodeInTree(selectedUUID)
                            inited = true
                        }
                        if (apply) {
                            applyUUIDNowOrOnReload(selectedUUID)
                        }
                    }
                }
            }
        }

        @RequiresEdt
        private fun applyUUIDNowOrOnReload(uuid: UUID?) {
            toolchainBox.selectedToolchain = uuid
            if (uuid != null && toolchainBox.selectedToolchain == null) {
                selectOnNextReload = uuid
            } else {
                selectOnNextReload = null
            }
        }

        fun isModified(): Boolean {
            return ZigToolchainService.getInstance(project).toolchainUUID != toolchainBox.selectedToolchain
        }

        fun apply() {
            ZigToolchainService.getInstance(project).toolchainUUID = toolchainBox.selectedToolchain
        }

        fun reset() {
            toolchainBox.selectedToolchain = ZigToolchainService.getInstance(project).toolchainUUID
        }



        override fun dispose() {
            ZigToolchainListService.getInstance().removeChangeListener(this)
        }
    }
}


private fun getModelList(): List<TCListElemIn> {
    val modelList = ArrayList<TCListElemIn>()
    modelList.add(TCListElem.None)
    modelList.addAll(ZigToolchainListService.getInstance().toolchains.map { it.asActual() })
    modelList.add(Separator("", true))
    modelList.addAll(TCListElem.fetchGroup)
    modelList.add(Separator(ZigBrainsBundle.message("settings.toolchain.model.detected.separator"), true))
    modelList.addAll(suggestZigToolchains().map { it.asSuggested() })
    return modelList
}