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

package com.falsepattern.zigbrains.project.actions

import com.falsepattern.zigbrains.project.execution.base.ZigExecConfig
import com.falsepattern.zigbrains.project.execution.run.ZigConfigProducerRun
import com.falsepattern.zigbrains.project.execution.run.ZigExecConfigRun
import com.falsepattern.zigbrains.project.execution.test.ZigConfigProducerTest
import com.falsepattern.zigbrains.project.execution.test.ZigExecConfigTest
import com.intellij.execution.ExecutionManager
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction

class ZigRunFileAction: DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val config = getConfig(e) ?: return
        val project = file.project
        val builder = ExecutionEnvironmentBuilder.createOrNull(DefaultRunExecutor.getRunExecutorInstance(), config) ?: return
        ExecutionManager.getInstance(project).restartRunProfile(builder.build())
    }

    private fun getConfig(e: AnActionEvent): ZigExecConfig<*>? {
        val context = ConfigurationContext.getFromContext(e.dataContext, e.place)
        return getRunConfiguration(context) ?: getTestConfiguration(context)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = getConfig(e) != null
    }

    private fun getRunConfiguration(context: ConfigurationContext): ZigExecConfigRun? {
        try {
            val configProducer = RunConfigurationProducer.getInstance(ZigConfigProducerRun::class.java)
            val settings = configProducer.findExistingConfiguration(context)
            if (settings != null) {
                return settings.configuration as? ZigExecConfigRun
            }
            val fromContext = configProducer.createConfigurationFromContext(context)
            if (fromContext != null) {
                return fromContext.configuration as? ZigExecConfigRun
            }
        } catch (_: NullPointerException) {}
        return null
    }
    private fun getTestConfiguration(context: ConfigurationContext): ZigExecConfigTest? {
        try {
            val configProducer = RunConfigurationProducer.getInstance(ZigConfigProducerTest::class.java)
            val settings = configProducer.findExistingConfiguration(context)
            if (settings != null) {
                return settings.configuration as? ZigExecConfigTest
            }
            val fromContext = configProducer.createConfigurationFromContext(context)
            if (fromContext != null) {
                return fromContext.configuration as? ZigExecConfigTest
            }
        } catch (_: NullPointerException) {}
        return null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}