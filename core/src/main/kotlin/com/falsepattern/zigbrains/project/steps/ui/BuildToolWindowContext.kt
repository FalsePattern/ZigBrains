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

package com.falsepattern.zigbrains.project.steps.ui

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.buildscan.Serialization
import com.falsepattern.zigbrains.project.buildscan.ZigBuildScanListener
import com.falsepattern.zigbrains.project.buildscan.zigBuildScan
import com.falsepattern.zigbrains.project.execution.build.ZigConfigTypeBuild
import com.falsepattern.zigbrains.project.execution.build.ZigExecConfigBuild
import com.falsepattern.zigbrains.project.execution.firstConfigFactory
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchain
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.falsepattern.zigbrains.shared.ipc.IPCUtil
import com.falsepattern.zigbrains.shared.ipc.ZigIPCService
import com.falsepattern.zigbrains.shared.ipc.ipc
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreePath


@OptIn(ExperimentalUnsignedTypes::class)
class BuildToolWindowContext(private val project: Project): Disposable {
    inner class TreeBox() {
        val panel = JPanel(VerticalLayout(0))
        val root = DefaultMutableTreeNode(BaseNodeDescriptor<Any>(project, ""))
        val model = DefaultTreeModel(root)
        val tree = Tree(model).also { it.isRootVisible = false }
    }
    private val viewPanel = JPanel(VerticalLayout(0))
    private val stepsBox = TreeBox()
    private val buildBox = if (IPCUtil.haveIPC) TreeBox() else null
    private var live = AtomicBoolean(true)

    init {
        viewPanel.add(JBLabel(ZigBrainsBundle.message("build.tool.window.tree.steps.label")))
        viewPanel.add(stepsBox.panel)
        stepsBox.panel.setRunningBuildScan()

        stepsBox.tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val node = stepsBox.tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
                    val step = node.userObject as? StepNodeDescriptor ?: return

                    val stepName = step.element?.name ?: return
                    val manager = RunManager.getInstance(project)
                    val config = getExistingRunConfig(manager, stepName) ?: run {
                        val factory = firstConfigFactory<ZigConfigTypeBuild>()
                        val newConfig = manager.createConfiguration("zig build $stepName", factory)
                        val config = newConfig.configuration as ZigExecConfigBuild
                        config.buildSteps.args = stepName
                        manager.addConfiguration(newConfig)
                        return@run newConfig
                    }

                    manager.selectedConfiguration = config
                    ProgramRunnerUtil.executeConfiguration(config, DefaultRunExecutor.getRunExecutorInstance())
                }
            }
        })

        if (buildBox != null) {
            viewPanel.add(JBLabel(ZigBrainsBundle.message("build.tool.window.tree.build.label")))
            viewPanel.add(buildBox.panel)
            buildBox.panel.setNoBuilds()

            project.zigCoroutineScope.launch {
                while (!project.isDisposed && live.get()) {
                    val ipc = project.ipc ?: return@launch
                    withTimeoutOrNull(1000) {
                        ipc.changed.receive()
                    } ?: continue
                    ipc.mutex.withLock {
                        withEDTContext(ModalityState.any()) {
                            if (ipc.nodes.isEmpty()) {
                                buildBox.root.removeAllChildren()
                                buildBox.panel.setNoBuilds()
                                return@withEDTContext
                            }
                            val allNodes = ArrayList(ipc.nodes)
                            val existingNodes = ArrayList<ZigIPCService.IPCTreeNode>()
                            val removedNodes = ArrayList<ZigIPCService.IPCTreeNode>()
                            buildBox.root.children().iterator().forEach { child ->
                                if (child !is ZigIPCService.IPCTreeNode) {
                                    return@forEach
                                }
                                if (child !in allNodes) {
                                    removedNodes.add(child)
                                } else {
                                    existingNodes.add(child)
                                }
                            }
                            val newNodes = ArrayList<MutableTreeNode>(allNodes)
                            newNodes.removeAll(existingNodes.toSet())
                            removedNodes.forEach { buildBox.root.remove(it) }
                            newNodes.forEach { buildBox.root.add(it) }
                            if (removedNodes.isNotEmpty() || newNodes.isNotEmpty()) {
                                buildBox.model.reload(buildBox.root)
                            }
                            if (buildBox.root.childCount == 0) {
                                buildBox.panel.setNoBuilds()
                            } else {
                                buildBox.panel.setViewportBody(buildBox.tree)
                            }
                            for (bn in allNodes) {
                                expandRecursively(buildBox, bn)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createContentPanel(): Content {
        val contentPanel = SimpleToolWindowPanel(false)
        val body = JPanel(GridBagLayout())
        contentPanel.setLayout(GridBagLayout())
        val c = GridBagConstraints()
        c.fill = GridBagConstraints.BOTH
        c.weightx = 1.0
        c.weighty = 1.0
        contentPanel.add(body, c)
        c.fill = GridBagConstraints.HORIZONTAL
        c.anchor = GridBagConstraints.NORTHWEST
        c.weightx = 1.0
        c.weighty = 0.0
        val toolbar = ActionManager.getInstance().createActionToolbar("ZigToolbar", DefaultActionGroup(ActionManager.getInstance().getAction("zigbrains.buildscan.sync")), true)
        toolbar.targetComponent = null
        body.add(toolbar.component, c)
        c.gridwidth = 1
        c.gridy = 1
        c.weighty = 1.0
        c.fill = GridBagConstraints.BOTH
        val viewport = JBScrollPane()
        viewport.setViewportView(viewPanel)
        body.add(viewport, c)
        val content = ContentFactory.getInstance().createContent(contentPanel, "", false)
        return content
    }

    override fun dispose() {
        live.set(false)
    }

    companion object {
		private const val BUILD_TOOL_WINDOW_ID = "zigbrains.build"

        suspend fun create(project: Project, window: ToolWindow) {
            withEDTContext(ModalityState.any()) {
                val context = BuildToolWindowContext(project)
                Disposer.register(context, project.zigBuildScan.register(context.BuildReloadListener()))
                Disposer.register(window.disposable, context)
                window.contentManager.addContent(context.createContentPanel())
            }
        }

        private fun expandRecursively(box: TreeBox, node: ZigIPCService.IPCTreeNode) {
            if (node.changed) {
                box.model.reload(node)
                node.changed = false
            }
            box.tree.expandPath(TreePath(box.model.getPathToRoot(node)))
            node.children().asIterator().forEach { child ->
                (child as? ZigIPCService.IPCTreeNode)?.let { expandRecursively(box, it) }
            }
        }

		suspend fun reload(project: Project, toolchain: ZigToolchain?) {
            withEDTContext(ModalityState.any()) {
                ToolWindowManager.getInstance(project)
                    .getToolWindow(BUILD_TOOL_WINDOW_ID)
                    ?.isAvailable = toolchain != null
            }
		}
    }

    inner class BuildReloadListener: ZigBuildScanListener {
        override suspend fun preReload() {
            stepsBox.panel.setRunningBuildScan()
        }

        override suspend fun postReload(projects: List<Serialization.Project>) {
            stepsBox.root.removeAllChildren()
            projects.firstOrNull()?.let { proj ->
                for (step in proj.steps) {
                    val icon = when(step.kind) {
                        "install" -> AllIcons.Actions.Install
                        "uninstall" -> AllIcons.Actions.Uninstall
                        else -> AllIcons.RunConfigurations.TestState.Run
                    }
                    stepsBox.root.add(DefaultMutableTreeNode(StepNodeDescriptor(project, step.name, icon, step.description)))
                }
            }
            withEDTContext(ModalityState.any()) {
                stepsBox.model.reload(stepsBox.root)
                stepsBox.panel.setViewportBody(stepsBox.tree)
            }
        }

        override suspend fun errorReload(type: ZigBuildScanListener.ErrorType, details: String?) {
            withEDTContext(ModalityState.any()) {
                stepsBox.panel.setViewportError(ZigBrainsBundle.message(when(type) {
                    ZigBuildScanListener.ErrorType.MissingToolchain -> "build.tool.window.status.error.missing-toolchain"
                    ZigBuildScanListener.ErrorType.MissingZigExe -> "build.tool.window.status.error.missing-zig-exe"
                    ZigBuildScanListener.ErrorType.MissingBuildZig -> "build.tool.window.status.error.missing-build-zig"
                    ZigBuildScanListener.ErrorType.GeneralError -> "build.tool.window.status.error.general"
					ZigBuildScanListener.ErrorType.FailedToCopyHelper -> "build.tool.window.status.error.failed-copy-helper"
				}), details)
            }
        }

        override suspend fun timeoutReload(seconds: Int) {
            withEDTContext(ModalityState.any()) {
                stepsBox.panel.setViewportError(ZigBrainsBundle.message("build.tool.window.status.timeout", seconds), null)
            }
        }
    }
}

private fun JPanel.setViewportBody(component: Component) {
    removeAll()
    add(component)
    repaint()
}

private fun JPanel.setRunningBuildScan() {
    setViewportBody(JBLabel(ZigBrainsBundle.message("build.tool.window.status.loading"), AnimatedIcon.Default(), SwingConstants.CENTER))
}

private fun JPanel.setNoBuilds() {
    setViewportBody(JBLabel(ZigBrainsBundle.message("build.tool.window.status.no-builds"), AllIcons.General.Information, SwingConstants.CENTER))
}

private fun JPanel.setViewportError(msg: String, details: String?) {
    val result = JPanel()
    result.layout = BoxLayout(result, BoxLayout.Y_AXIS)
    result.add(JBLabel(msg, AllIcons.General.Error, SwingConstants.CENTER))
    if (details != null) {
        val code = JBTextArea()
        code.isEditable = false
        code.text = details
        val scroll = JBScrollPane(code)
        result.add(scroll)
    }
    setViewportBody(result)
}

private fun getExistingRunConfig(manager: RunManager, stepName: String): RunnerAndConfigurationSettings? {
    for (config in manager.getConfigurationSettingsList(ZigConfigTypeBuild::class.java)) {
        val build = config.configuration as? ZigExecConfigBuild ?: continue
        val steps = build.buildSteps.argsSplit()
        if (steps.size != 1)
            continue
        if (steps[0] != stepName)
            continue
        return config
    }
    return null
}
