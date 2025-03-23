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

package com.falsepattern.zigbrains.project.execution.base

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.direnv.DirenvCmd
import com.falsepattern.zigbrains.project.settings.zigProjectSettings
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jdom.Element
import org.jetbrains.annotations.Nls

abstract class ZigExecConfig<T: ZigExecConfig<T>>(project: Project, factory: ConfigurationFactory, @Nls name: String): LocatableConfigurationBase<ZigProfileState<T>>(project, factory, name) {
    var workingDirectory = WorkDirectoryConfigurable("workingDirectory").apply { path = project.guessProjectDir()?.toNioPathOrNull() }
        private set
    var pty = CheckboxConfigurable("pty", ZigBrainsBundle.message("exec.option.label.emulate-terminal"), false)
        private set

    abstract val suggestedName: @ActionText String
    @Throws(ExecutionException::class)
    abstract suspend fun buildCommandLineArgs(debug: Boolean): List<String>
    abstract override fun getState(executor: Executor, environment: ExecutionEnvironment): ZigProfileState<T>

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return ZigConfigEditor(this)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        getConfigurables().forEach { it.readExternal(element) }
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        getConfigurables().forEach { it.writeExternal(element) }
    }


    suspend fun patchCommandLine(commandLine: GeneralCommandLine): GeneralCommandLine {
        if (project.zigProjectSettings.state.direnv) {
            commandLine.withEnvironment(DirenvCmd.importDirenv(project).env)
        }
        return commandLine
    }

    fun emulateTerminal(): Boolean {
        return pty.value
    }

    override fun clone(): T {
        val myClone = super.clone() as ZigExecConfig<*>
        myClone.workingDirectory = workingDirectory.clone()
        myClone.pty = pty.clone()
        @Suppress("UNCHECKED_CAST")
        return myClone as T
    }

    open fun getConfigurables(): List<ZigConfigurable<*>> = listOf(workingDirectory, pty)
}