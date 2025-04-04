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
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.base.render
import com.falsepattern.zigbrains.shared.ui.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.icons.EMPTY_ICON
import com.intellij.util.ui.EmptyIcon
import javax.swing.JList

class TCComboBox(model: ZBModel<ZigToolchain>): ZBComboBox<ZigToolchain>(model, ::TCCellRenderer)

class TCContext(project: Project?, model: ZBModel<ZigToolchain>): ZBContext<ZigToolchain>(project, model, ::TCCellRenderer)

class TCCellRenderer(getModel: () -> ZBModel<ZigToolchain>): ZBCellRenderer<ZigToolchain>(getModel) {
    override fun customizeCellRenderer(
        list: JList<out ListElem<ZigToolchain>?>,
        value: ListElem<ZigToolchain>?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        icon = EMPTY_ICON
        when (value) {
            is ListElem.One -> {
                val (icon, isSuggestion) = when(value) {
                    is ListElem.One.Suggested -> AllIcons.General.Information to true
                    is ListElem.One.Actual -> Icons.Zig to false
                }
                this.icon = icon
                val item = value.instance
                item.render(this, isSuggestion, index == -1)
            }

            is ListElem.Download -> {
                icon = AllIcons.Actions.Download
                append(ZigBrainsBundle.message("settings.toolchain.model.download.text"))
            }

            is ListElem.FromDisk -> {
                icon = AllIcons.General.OpenDisk
                append(ZigBrainsBundle.message("settings.toolchain.model.from-disk.text"))
            }
            is ListElem.Pending -> {
                icon = EMPTY_ICON
                append(ZigBrainsBundle.message("settings.toolchain.model.loading.text"), SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
            is ListElem.None, null -> {
                icon = AllIcons.General.BalloonError
                append(ZigBrainsBundle.message("settings.toolchain.model.none.text"), SimpleTextAttributes.ERROR_ATTRIBUTES)
            }
        }
    }

}
private val EMPTY_ICON = EmptyIcon.create(16, 16)