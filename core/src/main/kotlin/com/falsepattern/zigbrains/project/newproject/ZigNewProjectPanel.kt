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

package com.falsepattern.zigbrains.project.newproject

import com.falsepattern.zigbrains.lsp.settings.ZLSSettingsPanel
import com.falsepattern.zigbrains.project.settings.ZigProjectSettingsPanel
import com.falsepattern.zigbrains.project.template.ZigExecutableTemplate
import com.falsepattern.zigbrains.project.template.ZigInitTemplate
import com.falsepattern.zigbrains.project.template.ZigLibraryTemplate
import com.falsepattern.zigbrains.project.template.ZigProjectTemplate
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.Panel
import com.intellij.util.ui.JBUI
import javax.swing.JList
import javax.swing.ListSelectionModel

class ZigNewProjectPanel(private var handleGit: Boolean): Disposable {
    private val git = JBCheckBox()
    private val projConf = ZigProjectSettingsPanel(null).also { Disposer.register(this, it) }
    private val zlsConf = ZLSSettingsPanel(null).also { Disposer.register(this, it) }
    private val templateList = JBList(JBList.createDefaultListModel(defaultTemplates)).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        selectedIndex = 0
        cellRenderer = object : ColoredListCellRenderer<ZigProjectTemplate>() {
            override fun customizeCellRenderer(list: JList<out ZigProjectTemplate>, value: ZigProjectTemplate?, index: Int, selected: Boolean, hasFocus: Boolean) {
                value?.let {
                    icon = it.icon
                    append(it.name)
                }
            }
        }
    }

    private val templateToolbar get() = ToolbarDecorator.createDecorator(templateList)
        .setToolbarPosition(ActionToolbarPosition.BOTTOM)
        .setPreferredSize(JBUI.size(0, 125))
        .disableUpDownActions()
        .disableAddAction()
        .disableRemoveAction()

    fun getData(): ZigProjectConfigurationData {
        val selectedTemplate = templateList.selectedValue
        return ZigProjectConfigurationData(handleGit && git.isSelected, projConf.data, zlsConf.data, selectedTemplate)
    }

    fun attach(p: Panel): Unit = with(p) {
        if (handleGit) {
            row("Create Git repository") {
                cell(git)
            }
        }
        group("Zig Project Template") {
            row {
                resizableRow()
                cell(templateToolbar.createPanel())
                    .align(AlignX.FILL)
                    .align(AlignY.FILL)
            }
        }
        projConf.attach(p)
        zlsConf.attach(p)
    }

    suspend fun autodetect() {
        projConf.autodetect()
        zlsConf.autodetect()
    }

    override fun dispose() {
    }
}


private val defaultTemplates get() = listOf(
    ZigExecutableTemplate(),
    ZigLibraryTemplate(),
    ZigInitTemplate()
)