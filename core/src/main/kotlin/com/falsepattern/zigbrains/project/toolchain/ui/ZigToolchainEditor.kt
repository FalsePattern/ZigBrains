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

import com.falsepattern.zigbrains.project.toolchain.ZigToolchainListService
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainService
import com.falsepattern.zigbrains.project.toolchain.base.suggestZigToolchains
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import java.awt.event.ItemEvent
import javax.swing.JComponent
import kotlin.collections.addAll

class ZigToolchainEditor(private val project: Project): Configurable {
    private var myUi: UI? = null
    override fun getDisplayName(): @NlsContexts.ConfigurableName String? {
        return "Zig"
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
        private var oldSelectionIndex: Int = 0
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
            if (item !is TCListElem) {
                toolchainBox.selectedIndex = oldSelectionIndex
                return
            }
            when(item) {
                is TCListElem.None, is TCListElem.Toolchain.Actual -> {
                    oldSelectionIndex = toolchainBox.selectedIndex
                }
                else -> {
                    toolchainBox.selectedIndex = oldSelectionIndex
                }
            }
        }

        override fun toolchainListChanged() {
            val selected = model.selected
            val list = getModelList()
            model.updateContents(list)
            if (selected != null && list.contains(selected)) {
                model.selectedItem = selected
            } else {
                model.selectedItem = TCListElem.None
            }
        }

        fun attach(p: Panel): Unit = with(p) {
            row("Toolchain") {
                cell(toolchainBox).resizableColumn().align(AlignX.FILL)
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
            oldSelectionIndex = toolchainBox.selectedIndex
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
    modelList.add(Separator("Detected toolchains", true))
    modelList.addAll(suggestZigToolchains().map { it.asSuggested() })
    return modelList
}