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

package com.falsepattern.zigbrains.project.toolchain

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.util.treeView.AbstractTreeStructureBase
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ui.SdkAppearanceService
import com.intellij.openapi.roots.ui.configuration.SdkListPresenter
import com.intellij.openapi.roots.ui.configuration.SdkPopupFactory
import com.intellij.openapi.ui.MasterDetailsComponent
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.BaseTreePopupStep
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.ui.popup.list.ComboBoxPopup
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.ui.treeStructure.SimpleTreeStructure
import com.intellij.util.IconUtil
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.UIUtil.FontColor
import java.awt.Component
import java.awt.LayoutManager
import java.util.*
import java.util.function.Consumer
import javax.swing.AbstractListModel
import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.ListModel
import javax.swing.SwingConstants
import javax.swing.tree.DefaultTreeModel
import kotlin.io.path.pathString

class ZigToolchainListEditor(): MasterDetailsComponent() {
    private var isTreeInitialized = false

    override fun createComponent(): JComponent {
        if (!isTreeInitialized) {
            initTree()
            isTreeInitialized = true
        }
        return super.createComponent()
    }

    class ToolchainContext(private val project: Project?, private val model: ListModel<Any>): ComboBoxPopup.Context<Any> {
        override fun getProject(): Project? {
            return project
        }

        override fun getModel(): ListModel<Any> {
            return model
        }

        override fun getRenderer(): ListCellRenderer<in Any> {
            return object: ColoredListCellRenderer<Any>() {
                override fun customizeCellRenderer(
                    list: JList<out Any?>,
                    value: Any?,
                    index: Int,
                    selected: Boolean,
                    hasFocus: Boolean
                ) {
                    icon = EMPTY_ICON
                    if (value is LocalZigToolchain) {
                        icon = IconUtil.addIcon
                        append(SdkListPresenter.presentDetectedSdkPath(value.location.pathString))
                        if (value.name != null) {
                            append(" ")
                            append(value.name, SimpleTextAttributes.GRAYED_ATTRIBUTES)
                        }
                    }
                }
            }
        }

    }

    class ToolchainPopup(context: ToolchainContext,
                         selected: Any?,
                         onItemSelected: Consumer<Any>
    ): ComboBoxPopup<Any>(context, selected, onItemSelected) {

    }

    override fun createActions(fromPopup: Boolean): List<AnAction?>? {
        val add = object : DumbAwareAction({"lmaoo"}, Presentation.NULL_STRING, IconUtil.addIcon) {
            override fun actionPerformed(e: AnActionEvent) {
                val toolchains = suggestZigToolchains(zigToolchainList.toolchains.map { it.second }.toList())
                val final = ArrayList<Any>()
                final.addAll(toolchains)
                val popup = ToolchainPopup(ToolchainContext(null, CollectionListModel(final)), null, {})
                popup.showInBestPositionFor(e.dataContext)
            }
        }
        return listOf(add, MyDeleteAction())
    }

    override fun onItemDeleted(item: Any?) {
        if (item is UUID) {
            zigToolchainList.removeToolchain(item)
        }
        super.onItemDeleted(item)
    }

    override fun reset() {
        reloadTree()
        super.reset()
    }

    override fun getEmptySelectionString() = ZigBrainsBundle.message("settings.toolchains.empty")

    override fun getDisplayName() = ZigBrainsBundle.message("settings.toolchains.title")

    private fun addLocalToolchain(uuid: UUID, toolchain: LocalZigToolchain) {
        val node = MyNode(LocalZigToolchainConfigurable(uuid, toolchain, ProjectManager.getInstance().defaultProject))
        addNode(node, myRoot)
    }

    private fun reloadTree() {
        myRoot.removeAllChildren()
        zigToolchainList.toolchains.forEach { (uuid, toolchain) ->
            if (toolchain is LocalZigToolchain) {
                addLocalToolchain(uuid, toolchain)
            }
        }
        (myTree.model as DefaultTreeModel).reload()
    }
}

private val EMPTY_ICON = EmptyIcon.create(1, 16)