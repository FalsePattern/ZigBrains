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

import ai.grazie.utils.attributes.value
import com.falsepattern.zigbrains.Icons
import com.falsepattern.zigbrains.project.toolchain.base.render
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CellRendererPanel
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.GroupHeaderSeparator
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.panels.OpaquePanel
import com.intellij.ui.popup.list.ComboBoxPopup
import com.intellij.util.Consumer
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.util.IdentityHashMap
import java.util.UUID
import javax.accessibility.AccessibleContext
import javax.swing.JList
import javax.swing.border.Border

internal class TCComboBoxPopup(
    context: TCContext,
    selected: TCListElem?,
    onItemSelected: Consumer<TCListElem>,
) : ComboBoxPopup<TCListElem>(context, selected, onItemSelected)

internal class TCComboBox(model: TCModel): ComboBox<TCListElem>(model) {
    init {
        setRenderer(TCCellRenderer({model}))
    }

    var selectedToolchain: UUID?
        set(value) {
            if (value == null) {
                selectedItem = TCListElem.None
                return
            }
            for (i in 0..<model.size) {
                val element = model.getElementAt(i)
                if (element is TCListElem.Toolchain.Actual) {
                    if (element.uuid == value) {
                        selectedIndex = i
                        return
                    }
                }
            }
            selectedItem = TCListElem.None
        }
        get() {
            val item = selectedItem
            return when(item) {
                is TCListElem.Toolchain.Actual -> item.uuid
                else -> null
            }
        }
}

internal class TCModel private constructor(elements: List<TCListElem>, private val separators: Map<TCListElem, Separator>) : CollectionComboBoxModel<TCListElem>(elements) {
    companion object {
        operator fun invoke(input: List<TCListElemIn>): TCModel {
            val separators = IdentityHashMap<TCListElem, Separator>()
            var lastSeparator: Separator? = null
            val elements = ArrayList<TCListElem>()
            input.forEach {
                when (it) {
                    is TCListElem -> {
                        if (lastSeparator != null) {
                            separators[it] = lastSeparator
                            lastSeparator = null
                        }
                        elements.add(it)
                    }

                    is Separator -> lastSeparator = it
                }
            }
            val model = TCModel(elements, separators)
            return model
        }
    }

    fun separatorAbove(elem: TCListElem) = separators[elem]
}

internal class TCContext(private val project: Project?, private val model: TCModel) : ComboBoxPopup.Context<TCListElem> {
    override fun getProject(): Project? {
        return project
    }

    override fun getModel(): TCModel {
        return model
    }

    override fun getRenderer(): TCCellRenderer {
        return TCCellRenderer(::getModel)
    }
}

internal class TCCellRenderer(val getModel: () -> TCModel) : ColoredListCellRenderer<TCListElem>() {

    override fun getListCellRendererComponent(
        list: JList<out TCListElem?>?,
        value: TCListElem?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ): Component? {
        val component = super.getListCellRendererComponent(list, value, index, selected, hasFocus) as SimpleColoredComponent
        val panel = object : CellRendererPanel(BorderLayout()) {
            val myContext = component.accessibleContext

            override fun getAccessibleContext(): AccessibleContext? {
                return myContext
            }

            override fun setBorder(border: Border?) {
                component.border = border
            }
        }
        panel.add(component, BorderLayout.CENTER)

        component.isOpaque = true
        list?.let { background = if (selected) it.selectionBackground else it.background }

        val model = getModel()

        if (index == -1) {
            component.isOpaque = false
            panel.isOpaque = false
            return panel
        }

        val separator = value?.let { model.separatorAbove(it) }

        if (separator != null) {
            val vGap = if (UIUtil.isUnderNativeMacLookAndFeel()) 1 else 3
            val separatorComponent = GroupHeaderSeparator(JBUI.insets(vGap, 10, vGap, 0))
            separatorComponent.isHideLine = !separator.line
            separatorComponent.caption = separator.text.ifBlank { null }
            val wrapper = OpaquePanel(BorderLayout())
            wrapper.add(separatorComponent, BorderLayout.CENTER)
            list?.let { wrapper.background = it.background }
            panel.add(wrapper, BorderLayout.NORTH)
        }

        return panel
    }

    override fun customizeCellRenderer(
        list: JList<out TCListElem?>,
        value: TCListElem?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        icon = EMPTY_ICON
        when (value) {
            is TCListElem.Toolchain -> {
                val (icon, isSuggestion) = when(value) {
                    is TCListElem.Toolchain.Suggested -> AllIcons.General.Information to true
                    is TCListElem.Toolchain.Actual -> Icons.Zig to false
                }
                this.icon = icon
                val toolchain = value.toolchain
                toolchain.render(this, isSuggestion)
            }

            is TCListElem.Download -> {
                icon = AllIcons.Actions.Download
                append("Download Zig\u2026")
            }

            is TCListElem.FromDisk -> {
                icon = AllIcons.General.OpenDisk
                append("Add Zig from disk\u2026")
            }

            is TCListElem.None, null -> {
                icon = AllIcons.General.BalloonError
                append("<No Toolchain>", SimpleTextAttributes.ERROR_ATTRIBUTES)
            }
        }
    }
}

private val EMPTY_ICON = EmptyIcon.create(1, 16)