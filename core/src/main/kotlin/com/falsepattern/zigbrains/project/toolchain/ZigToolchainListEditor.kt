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

import com.falsepattern.zigbrains.Icons
import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.project.toolchain.base.createNamedConfigurable
import com.falsepattern.zigbrains.project.toolchain.base.render
import com.falsepattern.zigbrains.project.toolchain.base.suggestZigToolchains
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.MasterDetailsComponent
import com.intellij.openapi.util.Disposer
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.ui.*
import com.intellij.ui.components.panels.OpaquePanel
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.popup.list.ComboBoxPopup
import com.intellij.util.Consumer
import com.intellij.util.IconUtil
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.async
import java.awt.BorderLayout
import java.awt.Component
import java.util.*
import javax.accessibility.AccessibleContext
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.ListCellRenderer
import javax.swing.border.Border
import javax.swing.tree.DefaultTreeModel

class ZigToolchainListEditor() : MasterDetailsComponent() {
    private var isTreeInitialized = false
    private var myComponent: JComponent? = null

    override fun createComponent(): JComponent {
        if (!isTreeInitialized) {
            initTree()
            isTreeInitialized = true
        }
        val comp = super.createComponent()
        myComponent = comp
        return comp
    }

    override fun createActions(fromPopup: Boolean): List<AnAction> {
        val add = object : DumbAwareAction({ "lmaoo" }, Presentation.NULL_STRING, IconUtil.addIcon) {
            override fun actionPerformed(e: AnActionEvent) {
                val toolchains = suggestZigToolchains(zigToolchainList.toolchains.map { it.second }.toList())
                val final = ArrayList<TCListElemIn>()
                final.add(TCListElem.Download)
                final.add(TCListElem.FromDisk)
                final.add(Separator("Detected toolchains", true))
                final.addAll(toolchains.map { TCListElem.Toolchain(it) })
                val model = TCModel(final)
                val context = TCContext(null, model)
                val popup = TCPopup(context, null, ::onItemSelected)
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

    private fun onItemSelected(elem: TCListElem) {
        when (elem) {
            is TCListElem.Toolchain -> {
                val uuid = UUID.randomUUID()
                zigToolchainList.setToolchain(uuid, elem.toolchain)
                addToolchain(uuid, elem.toolchain)
                (myTree.model as DefaultTreeModel).reload()
            }

            is TCListElem.Download -> {
                zigCoroutineScope.async {
                    withEDTContext(ModalityState.stateForComponent(myComponent!!)) {
                        val info = withModalProgress(myComponent?.let { ModalTaskOwner.component(it) } ?: ModalTaskOwner.guess(), "Fetching zig version information", TaskCancellation.cancellable()) {
                            ZigVersionInfo.download()
                        }
                        val dialog = DialogBuilder()
                        val theList = ComboBox<String>(DefaultComboBoxModel(info.map { it.first }.toTypedArray()))
                        val outputPath = textFieldWithBrowseButton(
                            null,
                            FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle(ZigBrainsBundle.message("dialog.title.zig-toolchain"))
                        ).also {
                            Disposer.register(dialog, it)
                        }
                        var archiveSizeCell: Cell<*>? = null
                        fun detect(item: String) {
                            outputPath.text = System.getProperty("user.home") + "/.zig/" + item
                            val data = info.firstOrNull { it.first == item } ?: return
                            val size = data.second.dist.size
                            val sizeMb = size / (1024f * 1024f)
                            archiveSizeCell?.comment?.text = "Archive size: %.2fMB".format(sizeMb)
                        }
                        theList.addItemListener {
                            detect(it.item as String)
                        }
                        val center = panel {
                            row("Version:") {
                                cell(theList).resizableColumn().align(AlignX.FILL)
                            }
                            row("Location:") {
                                cell(outputPath).resizableColumn().align(AlignX.FILL).apply { archiveSizeCell = comment("") }
                            }
                        }
                        detect(info[0].first)
                        dialog.centerPanel(center)
                        dialog.setTitle("Version Selector")
                        dialog.addCancelAction()
                        dialog.showAndGet()
                    }
                }
            }

            is TCListElem.FromDisk -> {}
        }
    }

    override fun reset() {
        reloadTree()
        super.reset()
    }

    override fun getEmptySelectionString() = ZigBrainsBundle.message("settings.toolchains.empty")

    override fun getDisplayName() = ZigBrainsBundle.message("settings.toolchains.title")

    private fun addToolchain(uuid: UUID, toolchain: ZigToolchain) {
        val node = MyNode(toolchain.createNamedConfigurable(uuid, ProjectManager.getInstance().defaultProject))
        addNode(node, myRoot)
    }

    private fun reloadTree() {
        myRoot.removeAllChildren()
        zigToolchainList.toolchains.forEach { (uuid, toolchain) ->
            addToolchain(uuid, toolchain)
        }
        (myTree.model as DefaultTreeModel).reload()
    }

    override fun disposeUIResources() {
        super.disposeUIResources()
        myComponent = null
    }
}

private sealed interface TCListElemIn

private sealed interface TCListElem : TCListElemIn {
    @JvmRecord
    data class Toolchain(val toolchain: ZigToolchain) : TCListElem
    object Download : TCListElem
    object FromDisk : TCListElem
}

@JvmRecord
private data class Separator(val text: String, val separatorBar: Boolean) : TCListElemIn

private class TCPopup(
    context: TCContext,
    selected: TCListElem?,
    onItemSelected: Consumer<TCListElem>,
) : ComboBoxPopup<TCListElem>(context, selected, onItemSelected)

private class TCModel private constructor(elements: List<TCListElem>, private val separators: Map<TCListElem, Separator>) : CollectionListModel<TCListElem>(elements) {
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

private class TCContext(private val project: Project?, private val model: TCModel) : ComboBoxPopup.Context<TCListElem> {
    override fun getProject(): Project? {
        return project
    }

    override fun getModel(): TCModel {
        return model
    }

    override fun getRenderer(): ListCellRenderer<in TCListElem> {
        return TCCellRenderer(::getModel)
    }
}

private class TCCellRenderer(val getModel: () -> TCModel) : ColoredListCellRenderer<TCListElem>() {

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

        val separator = value?.let { model.separatorAbove(it) }

        if (separator != null) {
            val separatorText = separator.text
            val vGap = if (UIUtil.isUnderNativeMacLookAndFeel()) 1 else 3
            val separatorComponent = GroupHeaderSeparator(JBUI.insets(vGap, 10, vGap, 0))
            separatorComponent.isHideLine = !separator.separatorBar
            if (separatorText.isNotBlank()) {
                separatorComponent.caption = separatorText
            }

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
                icon = Icons.Zig
                val toolchain = value.toolchain
                toolchain.render(this)
            }

            is TCListElem.Download -> {
                icon = AllIcons.Actions.Download
                append("Download Zig\u2026")
            }

            is TCListElem.FromDisk -> {
                icon = AllIcons.General.OpenDisk
                append("Add Zig from disk\u2026")
            }

            null -> {}
        }
    }
}

private val EMPTY_ICON = EmptyIcon.create(1, 16)