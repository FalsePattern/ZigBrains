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

import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.CellRendererPanel
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.GroupHeaderSeparator
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.panels.OpaquePanel
import com.intellij.ui.popup.list.ComboBoxPopup
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Component
import java.util.IdentityHashMap
import java.util.UUID
import java.util.function.Consumer
import javax.accessibility.AccessibleContext
import javax.swing.JList
import javax.swing.border.Border
import kotlin.io.path.pathString

class ZBComboBoxPopup<T>(
    context: ZBContext<T>,
    selected: ListElem<T>?,
    onItemSelected: Consumer<ListElem<T>>,
) : ComboBoxPopup<ListElem<T>>(context, selected, onItemSelected)

open class ZBComboBox<T>(model: ZBModel<T>, renderer: (() -> ZBModel<T>)-> ZBCellRenderer<T>): ComboBox<ListElem<T>>(model) {
    init {
        setRenderer(renderer { model })
    }

    var selectedUUID: UUID?
        set(value) {
            if (value == null) {
                selectedItem = ListElem.None<Any>()
                return
            }
            for (i in 0..<model.size) {
                val element = model.getElementAt(i)
                if (element is ListElem.One.Actual) {
                    if (element.uuid == value) {
                        selectedIndex = i
                        return
                    }
                }
            }
            selectedItem = ListElem.None<Any>()
        }
        get() {
            val item = selectedItem
            return when(item) {
                is ListElem.One.Actual<*> -> item.uuid
                else -> null
            }
        }
}

class ZBModel<T> private constructor(elements: List<ListElem<T>>, private var separators: MutableMap<ListElem<T>, Separator<T>>) : CollectionComboBoxModel<ListElem<T>>(elements) {
    private var counter: Int = 0
    companion object {
        operator fun <T> invoke(input: List<ListElemIn<T>>): ZBModel<T> {
            val (elements, separators) = convert(input)
            val model = ZBModel<T>(elements, separators)
            model.launchPendingResolve()
            return model
        }

        private fun <T> convert(input: List<ListElemIn<T>>): Pair<List<ListElem<T>>, MutableMap<ListElem<T>, Separator<T>>> {
            val separators = IdentityHashMap<ListElem<T>, Separator<T>>()
            var lastSeparator: Separator<T>? = null
            val elements = ArrayList<ListElem<T>>()
            input.forEach {
                when (it) {
                    is ListElem -> {
                        if (lastSeparator != null) {
                            separators[it] = lastSeparator
                            lastSeparator = null
                        }
                        elements.add(it)
                    }

                    is Separator -> lastSeparator = it
                }
            }
            return elements to separators
        }
    }

    fun separatorAbove(elem: ListElem<T>) = separators[elem]

    private fun launchPendingResolve() {
        runInEdt(ModalityState.any()) {
            val counter = this.counter
            val size = this.size
            for (i in 0..<size) {
                val elem = getElementAt(i)
                    ?: continue
                if (elem !is ListElem.Pending)
                    continue
                zigCoroutineScope.launch(Dispatchers.EDT + ModalityState.any().asContextElement()) {
                    elem.elems.collect { newElem ->
                        insertBefore(elem, newElem, counter)
                    }
                    remove(elem, counter)
                }
            }
        }
    }

    @RequiresEdt
    private fun remove(old: ListElem<T>, oldCounter: Int) {
        val newCounter = this@ZBModel.counter
        if (oldCounter != newCounter) {
            return
        }
        val index = this@ZBModel.getElementIndex(old)
        this@ZBModel.remove(index)
        val sep = separators.remove(old)
        if (sep != null && this@ZBModel.size > index) {
            this@ZBModel.getElementAt(index)?.let { separators[it] = sep }
        }
    }

    @RequiresEdt
    private fun insertBefore(old: ListElem<T>, new: ListElem<T>?, oldCounter: Int) {
        val newCounter = this@ZBModel.counter
        if (oldCounter != newCounter) {
            return
        }
        if (new == null) {
            return
        }
        val currentIndex = this@ZBModel.getElementIndex(old)
        separators.remove(old)?.let {
            separators.put(new, it)
        }
        this@ZBModel.add(currentIndex, new)
    }

    @RequiresEdt
    fun updateContents(input: List<ListElemIn<T>>) {
        counter++
        val (elements, separators) = convert(input)
        this.separators = separators
        replaceAll(elements)
        launchPendingResolve()
    }
}

open class ZBContext<T>(private val project: Project?, private val model: ZBModel<T>, private val getRenderer: (() -> ZBModel<T>) -> ZBCellRenderer<T>) : ComboBoxPopup.Context<ListElem<T>> {
    override fun getProject(): Project? {
        return project
    }

    override fun getModel(): ZBModel<T> {
        return model
    }

    override fun getRenderer(): ZBCellRenderer<T> {
        return getRenderer(::getModel)
    }
}

abstract class ZBCellRenderer<T>(val getModel: () -> ZBModel<T>) : ColoredListCellRenderer<ListElem<T>>() {
    final override fun getListCellRendererComponent(
        list: JList<out ListElem<T>?>?,
        value: ListElem<T>?,
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

    abstract override fun customizeCellRenderer(
        list: JList<out ListElem<T>?>,
        value: ListElem<T>?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    )
}

fun renderPathNameComponent(path: String, name: String?, nameFallback: String, component: SimpleColoredComponent, isSuggestion: Boolean, isSelected: Boolean) {
    val path = presentDetectedPath(path)
    val primary: String
    var secondary: String?
    val tooltip: String?
    if (isSuggestion) {
        primary = path
        secondary = name
    } else {
        primary = name ?: nameFallback
        secondary = path
    }
    if (isSelected) {
        tooltip = secondary
        secondary = null
    } else {
        tooltip = null
    }
    component.append(primary)
    if (secondary != null) {
        component.append(" ")
        component.append(secondary, SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }
    component.toolTipText = tooltip
}

fun presentDetectedPath(home: String, maxLength: Int = 50, suffixLength: Int = 30): String {
    //for macOS, let's try removing Bundle internals
    var home = home
    home = StringUtil.trimEnd(home, "/Contents/Home") //NON-NLS
    home = StringUtil.trimEnd(home, "/Contents/MacOS") //NON-NLS
    home = FileUtil.getLocationRelativeToUserHome(home, false)
    home = StringUtil.shortenTextWithEllipsis(home, maxLength, suffixLength)
    return home
}

private val EMPTY_ICON = EmptyIcon.create(1, 16)