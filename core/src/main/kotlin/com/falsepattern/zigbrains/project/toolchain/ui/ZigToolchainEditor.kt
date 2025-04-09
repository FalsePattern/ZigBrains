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
import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.project.toolchain.ToolchainListChangeListener
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainListService
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainService
import com.falsepattern.zigbrains.project.toolchain.base.createNamedConfigurable
import com.falsepattern.zigbrains.project.toolchain.base.suggestZigToolchains
import com.falsepattern.zigbrains.shared.SubConfigurable
import com.falsepattern.zigbrains.shared.coroutine.asContextElement
import com.falsepattern.zigbrains.shared.coroutine.launchWithEDT
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.application.EDT
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.event.ItemEvent
import java.util.UUID
import javax.swing.JButton
import kotlin.collections.addAll

class ZigToolchainEditor(private val isForDefaultProject: Boolean = false): SubConfigurable<Project>, ToolchainListChangeListener {
    private val toolchainBox: TCComboBox
    private var selectOnNextReload: UUID? = null
    private val model: TCModel
    private var editButton: JButton? = null
    init {
        model = TCModel(getModelList())
        toolchainBox = TCComboBox(model)
        toolchainBox.addItemListener(::itemStateChanged)
        ZigToolchainListService.getInstance().addChangeListener(this)
    }

    private fun refreshButtonState(item: Any?) {
        editButton?.isEnabled = item is TCListElem.Toolchain.Actual
        editButton?.repaint()
    }

    private fun itemStateChanged(event: ItemEvent) {
        if (event.stateChange != ItemEvent.SELECTED) {
            return
        }
        val item = event.item
        refreshButtonState(item)
        if (item !is TCListElem.Pseudo)
            return
        zigCoroutineScope.launch(toolchainBox.asContextElement()) {
            val uuid = runCatching { ZigToolchainComboBoxHandler.onItemSelected(toolchainBox, item) }.getOrNull()
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

    override fun attach(p: Panel): Unit = with(p) {
        row(ZigBrainsBundle.message(
            if (isForDefaultProject)
                "settings.toolchain.editor.toolchain-default.label"
            else
                "settings.toolchain.editor.toolchain.label")
        ) {
            cell(toolchainBox).resizableColumn().align(AlignX.FILL)
            button(ZigBrainsBundle.message("settings.toolchain.editor.toolchain.edit-button.name")) { e ->
                zigCoroutineScope.launchWithEDT(toolchainBox.asContextElement()) {
                    var selectedUUID = toolchainBox.selectedToolchain ?: return@launchWithEDT
                    val toolchain = ZigToolchainListService.getInstance().getToolchain(selectedUUID) ?: return@launchWithEDT
                    val config = toolchain.createNamedConfigurable(selectedUUID)
                    val apply = ShowSettingsUtil.getInstance().editConfigurable(DialogWrapper.findInstance(toolchainBox)?.contentPane, config)
                    if (apply) {
                        applyUUIDNowOrOnReload(selectedUUID)
                    }
                }
            }.component.let {
                editButton = it
                refreshButtonState(toolchainBox.selectedItem)
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

    override fun isModified(context: Project): Boolean {
        return ZigToolchainService.getInstance(context).toolchainUUID != toolchainBox.selectedToolchain
    }

    override fun apply(context: Project) {
        ZigToolchainService.getInstance(context).toolchainUUID = toolchainBox.selectedToolchain
    }

    override fun reset(context: Project?) {
        val project = context ?: ProjectManager.getInstance().defaultProject
        toolchainBox.selectedToolchain = ZigToolchainService.getInstance(project).toolchainUUID
        refreshButtonState(toolchainBox.selectedItem)
    }

    override fun dispose() {
        ZigToolchainListService.getInstance().removeChangeListener(this)
    }

    override val newProjectBeforeInitSelector get() = true

    class Provider: ZigProjectConfigurationProvider {
        override fun create(project: Project?): SubConfigurable<Project>? {
            return ZigToolchainEditor(project?.isDefault ?: false).also { it.reset(project) }
        }

        override val index: Int get() = 0

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