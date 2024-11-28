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

package com.falsepattern.zigbrains.project.steps.ui

import com.falsepattern.zigbrains.Icons
import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.execution.build.ZigConfigTypeBuild
import com.falsepattern.zigbrains.project.execution.build.ZigExecConfigBuild
import com.falsepattern.zigbrains.project.execution.firstConfigFactory
import com.falsepattern.zigbrains.project.steps.discovery.ZigStepDiscoveryListener
import com.falsepattern.zigbrains.project.steps.discovery.zigStepDiscovery
import com.falsepattern.zigbrains.shared.coroutine.withEDTContext
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath


class BuildToolWindowContext(private val project: Project): Disposable {
    val rootNode: DefaultMutableTreeNode = DefaultMutableTreeNode(BaseNodeDescriptor<Any>(project, project.name, AllIcons.Actions.ProjectDirectory))
    private val buildZig: DefaultMutableTreeNode = DefaultMutableTreeNode(BaseNodeDescriptor<Any>(project, ZigBrainsBundle.message("build.tool.window.tree.steps.label"), Icons.Zig))
    init {
        rootNode.add(buildZig)
    }

    private fun setViewportTree(viewport: JBScrollPane) {
        val model = DefaultTreeModel(rootNode)
        val tree = Tree(model)
        tree.expandPath(TreePath(model.getPathToRoot(buildZig)))
        viewport.setViewportView(tree)
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
                    val step = node.userObject as? StepNodeDescriptor ?: return

                    val stepName = step.element?.name ?: return
                    val manager = RunManager.getInstance(project)
                    val config = getExistingRunConfig(manager, stepName) ?: run {
                        val factory = firstConfigFactory<ZigConfigTypeBuild>()
                        val newConfig = manager.createConfiguration("zig build $stepName", factory)
                        val config = newConfig.configuration as ZigExecConfigBuild
                        config.buildSteps.args = listOf(stepName)
                        manager.addConfiguration(newConfig)
                        return@run newConfig
                    }

                    manager.selectedConfiguration = config
                    ProgramRunnerUtil.executeConfiguration(config, DefaultRunExecutor.getRunExecutorInstance())
                }
            }
        })
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
        val toolbar = ActionManager.getInstance().createActionToolbar("ZigToolbar", DefaultActionGroup(ActionManager.getInstance().getAction("zigbrains.discover.steps")), true)
        toolbar.targetComponent = null
        body.add(toolbar.component, c)
        c.gridwidth = 1
        c.gridy = 1
        c.weighty = 1.0
        c.fill = GridBagConstraints.BOTH
        val viewport = JBScrollPane()
        viewport.setViewportNoContent()
        body.add(viewport, c)
        val content = ContentFactory.getInstance().createContent(contentPanel, "", false)
        content.putUserData(VIEWPORT, viewport)
        return content
    }

    override fun dispose() {

    }

    companion object {
        suspend fun create(project: Project, window: ToolWindow) {
            withEDTContext {
                val context = BuildToolWindowContext(project)
                Disposer.register(context, project.zigStepDiscovery.register(context.BuildReloadListener()))
                Disposer.register(window.disposable, context)
                window.contentManager.addContent(context.createContentPanel())
            }
        }
    }

    inner class BuildReloadListener: ZigStepDiscoveryListener {
        override suspend fun preReload() {
            getViewport(project)?.setViewportLoading()
        }

        override suspend fun postReload(steps: List<Pair<String, String?>>) {
            buildZig.removeAllChildren()
            for ((task, description) in steps) {
                val icon = when(task) {
                    "install" -> AllIcons.Actions.Install
                    "uninstall" -> AllIcons.Actions.Uninstall
                    else -> AllIcons.RunConfigurations.TestState.Run
                }
                buildZig.add(DefaultMutableTreeNode(StepNodeDescriptor(project, task, icon, description)))
            }
            withEDTContext {
                getViewport(project)?.let { setViewportTree(it) }
            }
        }

        override suspend fun errorReload(type: ZigStepDiscoveryListener.ErrorType, details: String?) {
            withEDTContext {
                getViewport(project)?.setViewportError(ZigBrainsBundle.message(when(type) {
                    ZigStepDiscoveryListener.ErrorType.MissingToolchain -> "build.tool.window.status.error.missing-toolchain"
                    ZigStepDiscoveryListener.ErrorType.MissingBuildZig -> "build.tool.window.status.error.missing-build-zig"
                    ZigStepDiscoveryListener.ErrorType.GeneralError -> "build.tool.window.status.error.general"
                }), details)
            }
        }

        override suspend fun timeoutReload(seconds: Int) {
            withEDTContext {
                getViewport(project)?.setViewportError(ZigBrainsBundle.message("build.tool.window.status.timeout", seconds), null)
            }
        }
    }
}

private fun JBScrollPane.setViewportLoading() {
    setViewportView(JBLabel(ZigBrainsBundle.message("build.tool.window.status.loading"), AnimatedIcon.Default(), SwingConstants.CENTER))
}

private fun JBScrollPane.setViewportNoContent() {
    setViewportView(JBLabel(ZigBrainsBundle.message("build.tool.window.status.not-scanned"), AllIcons.General.Information, SwingConstants.CENTER))
}

private fun JBScrollPane.setViewportError(msg: String, details: String?) {
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
    setViewportView(result)
}

private fun getViewport(project: Project): JBScrollPane? {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("zigbrains.build") ?: return null
    val cm = toolWindow.contentManager
    val content = cm.getContent(0) ?: return null
    return content.getUserData(VIEWPORT)
}

private fun getExistingRunConfig(manager: RunManager, stepName: String): RunnerAndConfigurationSettings? {
    for (config in manager.getConfigurationSettingsList(ZigConfigTypeBuild::class.java)) {
        val build = config.configuration as? ZigExecConfigBuild ?: continue
        val steps = build.buildSteps.args
        if (steps.size != 1)
            continue
        if (steps[0] != stepName)
            continue
        return config
    }
    return null
}

private val VIEWPORT = Key.create<JBScrollPane>("MODEL")
