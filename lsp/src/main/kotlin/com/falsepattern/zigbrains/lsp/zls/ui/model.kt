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

package com.falsepattern.zigbrains.lsp.zls.ui

import com.falsepattern.zigbrains.lsp.LSPIcons
import com.falsepattern.zigbrains.lsp.ZLSBundle
import com.falsepattern.zigbrains.lsp.zls.ZLSVersion
import com.falsepattern.zigbrains.shared.sanitizedPathString
import com.falsepattern.zigbrains.shared.ui.ListElem
import com.falsepattern.zigbrains.shared.ui.ZBCellRenderer
import com.falsepattern.zigbrains.shared.ui.ZBComboBox
import com.falsepattern.zigbrains.shared.ui.ZBContext
import com.falsepattern.zigbrains.shared.ui.ZBModel
import com.falsepattern.zigbrains.shared.ui.renderPathNameComponent
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.icons.EMPTY_ICON
import javax.swing.JList


class ZLSComboBox(model: ZBModel<ZLSVersion>): ZBComboBox<ZLSVersion>(model, ::ZLSCellRenderer)

class ZLSContext(project: Project?, model: ZBModel<ZLSVersion>): ZBContext<ZLSVersion>(project, model, ::ZLSCellRenderer)

class ZLSCellRenderer(getModel: () -> ZBModel<ZLSVersion>): ZBCellRenderer<ZLSVersion>(getModel) {
    override fun customizeCellRenderer(
        list: JList<out ListElem<ZLSVersion>?>,
        value: ListElem<ZLSVersion>?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        icon = EMPTY_ICON
        when (value) {
            is ListElem.One -> {
                val (icon, isSuggestion) = when(value) {
                    is ListElem.One.Suggested -> AllIcons.General.Information to true
                    is ListElem.One.Actual -> LSPIcons.ZLS to false
                }
                this.icon = icon
                val item = value.instance
                val name = item.name
                val path = item.path.sanitizedPathString ?: "unknown path"
                renderPathNameComponent(path, name, "ZLS", this, isSuggestion, index == -1)
            }

            is ListElem.Download -> {
                icon = AllIcons.Actions.Download
                append(ZLSBundle.message("settings.model.download.text"))
            }

            is ListElem.FromDisk -> {
                icon = AllIcons.General.OpenDisk
                append(ZLSBundle.message("settings.model.from-disk.text"))
            }
            is ListElem.Pending -> {
                icon = AllIcons.Empty
                append(ZLSBundle.message("settings.model.loading.text"), SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
            is ListElem.None, null -> {
                icon = AllIcons.General.BalloonError
                append(ZLSBundle.message("settings.model.none.text"), SimpleTextAttributes.ERROR_ATTRIBUTES)
            }
        }
    }

}