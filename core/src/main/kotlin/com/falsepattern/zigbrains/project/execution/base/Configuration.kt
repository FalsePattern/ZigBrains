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
import com.falsepattern.zigbrains.project.execution.base.ZigConfigurable.ZigConfigModule
import com.falsepattern.zigbrains.shared.cli.translateCommandline
import com.falsepattern.zigbrains.shared.element.readBoolean
import com.falsepattern.zigbrains.shared.element.readChild
import com.falsepattern.zigbrains.shared.element.readEnum
import com.falsepattern.zigbrains.shared.element.readString
import com.falsepattern.zigbrains.shared.element.readStrings
import com.falsepattern.zigbrains.shared.element.writeBoolean
import com.falsepattern.zigbrains.shared.element.writeChild
import com.falsepattern.zigbrains.shared.element.writeEnum
import com.falsepattern.zigbrains.shared.element.writeString
import com.falsepattern.zigbrains.shared.sanitizedPathString
import com.falsepattern.zigbrains.shared.sanitizedToNioPath
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rd.framework.base.deepClonePolymorphic
import org.jdom.Element
import org.jetbrains.annotations.Nls
import java.io.Serializable
import java.nio.file.Path
import javax.swing.JComponent

class ZigConfigEditor<T : ZigExecConfig<T>>(private val state: ZigExecConfig<T>) : SettingsEditor<T>() {
    private val configModules = ArrayList<ZigConfigModule<*>>()

    override fun resetEditorFrom(s: T) {
        outer@ for (cfg in s.getConfigurables()) {
            for (module in configModules) {
                if (module.tryReset(cfg))
                    continue@outer
            }
        }
    }

    override fun applyEditorTo(s: T) {
        outer@ for (cfg in s.getConfigurables()) {
            for (module in configModules) {
                if (module.tryApply(cfg)) {
                    continue@outer
                }
            }
        }
    }

    override fun createEditor(): JComponent {
        configModules.clear()
        configModules.addAll(state.getConfigurables().map { it.createEditor() })
        return panel {
            for (module in configModules) {
                module.construct(this)
            }
        }
    }

    override fun disposeEditor() {
        for (module in configModules) {
            module.dispose()
        }
        configModules.clear()
    }
}

interface ZigConfigurable<T : ZigConfigurable<T>> : Serializable, Cloneable {
    fun readExternal(element: Element)
    fun writeExternal(element: Element)
    fun createEditor(): ZigConfigModule<T>
    public override fun clone(): T

    interface ZigConfigModule<T : ZigConfigurable<T>> : Disposable {
        fun tryMatch(cfg: ZigConfigurable<*>): T?
        fun apply(configurable: T): Boolean
        fun reset(configurable: T)
        fun tryApply(cfg: ZigConfigurable<*>): Boolean {
            val x = tryMatch(cfg)
            if (x != null) {
                return apply(x)
            }
            return false
        }

        fun tryReset(cfg: ZigConfigurable<*>): Boolean {
            val x = tryMatch(cfg)
            if (x != null) {
                reset(x)
                return true
            }
            return false
        }

        fun construct(p: Panel)
    }
}

abstract class PathConfigurable<T : PathConfigurable<T>> : ZigConfigurable<T> {
    var path: Path? = null

    override fun readExternal(element: Element) {
        path = element.readString(serializedName)?.sanitizedToNioPath() ?: return
    }

    override fun writeExternal(element: Element) {
        element.writeString(serializedName, path?.sanitizedPathString ?: "")
    }

    abstract val serializedName: String

    abstract class PathConfigModule<T : PathConfigurable<T>> : ZigConfigModule<T> {
        override fun apply(configurable: T): Boolean {
            val str = stringValue
            if (str.isBlank()) {
                configurable.path = null
            } else {
                configurable.path = str.sanitizedToNioPath() ?: return false
            }
            return true
        }

        override fun reset(configurable: T) {
            stringValue = configurable.path?.sanitizedPathString ?: ""
        }

        protected abstract var stringValue: String
    }
}

class WorkDirectoryConfigurable(@Transient override val serializedName: String) : PathConfigurable<WorkDirectoryConfigurable>(), Cloneable {
    override fun createEditor(): ZigConfigModule<WorkDirectoryConfigurable> {
        return WorkDirectoryConfigModule(serializedName)
    }

    override fun clone(): WorkDirectoryConfigurable {
        return super<Cloneable>.clone() as WorkDirectoryConfigurable
    }

    class WorkDirectoryConfigModule(private val serializedName: String) : PathConfigModule<WorkDirectoryConfigurable>() {
        private val field = textFieldWithBrowseButton(
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle(ZigBrainsBundle.message("dialog.title.working-directory"))
        ).also { Disposer.register(this, it) }

        override var stringValue by field::text

        override fun tryMatch(cfg: ZigConfigurable<*>): WorkDirectoryConfigurable? {
            return if (cfg is WorkDirectoryConfigurable && cfg.serializedName == serializedName) cfg else null
        }

        override fun construct(p: Panel): Unit = with(p) {
            row(ZigBrainsBundle.message("exec.option.label.working-directory")) {
                cell(field).resizableColumn().align(AlignX.FILL)
            }
        }

        override fun dispose() {
            field.dispose()
        }

    }
}

class FilePathConfigurable(
    @Transient override val serializedName: String,
    @Transient @Nls private val guiLabel: String
) : PathConfigurable<FilePathConfigurable>(), Cloneable {

    override fun createEditor(): ZigConfigModule<FilePathConfigurable> {
        return FilePathConfigModule(serializedName, guiLabel)
    }

    override fun clone(): FilePathConfigurable {
        return super<Cloneable>.clone() as FilePathConfigurable
    }

    class FilePathConfigModule(private val serializedName: String, @Nls private val label: String) : PathConfigModule<FilePathConfigurable>() {
        private val field = textFieldWithBrowseButton(
            null,
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(),
        )

        override var stringValue by field::text

        override fun tryMatch(cfg: ZigConfigurable<*>): FilePathConfigurable? {
            return if (cfg is FilePathConfigurable && cfg.serializedName == serializedName) cfg else null
        }

        override fun construct(p: Panel): Unit = with(p) {
            row(label) {
                cell(field).resizableColumn().align(AlignX.FILL)
            }
        }

        override fun dispose() {
            Disposer.dispose(field)
        }
    }
}

open class CheckboxConfigurable(
    @Transient private val serializedName: String,
    @Transient @Nls private val label: String,
    var value: Boolean
) : ZigConfigurable<CheckboxConfigurable>, Cloneable {
    override fun readExternal(element: Element) {
        value = element.readBoolean(serializedName) ?: return
    }

    override fun writeExternal(element: Element) {
        element.writeBoolean(serializedName, value)
    }

    override fun createEditor(): ZigConfigModule<CheckboxConfigurable> {
        return CheckboxConfigModule(serializedName, label)
    }

    override fun clone(): CheckboxConfigurable {
        return super<Cloneable>.clone() as CheckboxConfigurable
    }

    class CheckboxConfigModule(
        private val serializedName: String,
        @Nls private val label: String
    ) : ZigConfigModule<CheckboxConfigurable> {
        private val checkBox = JBCheckBox()

        override fun tryMatch(cfg: ZigConfigurable<*>): CheckboxConfigurable? {
            return if (cfg is CheckboxConfigurable && cfg.serializedName == serializedName) cfg else null
        }

        override fun apply(configurable: CheckboxConfigurable): Boolean {
            configurable.value = checkBox.isSelected
            return true
        }

        override fun reset(configurable: CheckboxConfigurable) {
            checkBox.isSelected = configurable.value
        }

        override fun construct(p: Panel): Unit = with(p) {
            row(label) {
                cell(checkBox)
            }
        }

        override fun dispose() {}
    }
}

class OptimizationConfigurable(
    @Transient private val serializedName: String,
    var level: OptimizationLevel = OptimizationLevel.Debug,
    var forced: Boolean = false
) : ZigConfigurable<OptimizationConfigurable>, Cloneable {
    override fun readExternal(element: Element) {
        element.readChild(serializedName)?.apply {
            readEnum<OptimizationLevel>("level")?.let { level = it }
            readBoolean("forced")?.let { forced = it }
        }
    }

    override fun writeExternal(element: Element) {
        element.writeChild(serializedName).apply {
            writeEnum("level", level)
            writeBoolean("forced", forced)
        }
    }

    override fun createEditor(): ZigConfigModule<OptimizationConfigurable> {
        return OptimizationConfigModule(serializedName)
    }

    override fun clone(): OptimizationConfigurable {
        return super<Cloneable>.clone() as OptimizationConfigurable
    }

    class OptimizationConfigModule(private val serializedName: String) : ZigConfigModule<OptimizationConfigurable> {
        private val levels = ComboBox(OptimizationLevel.entries.toTypedArray())
        private val forced = JBCheckBox(ZigBrainsBundle.message("exec.option.label.optimization.force"))
        override fun tryMatch(cfg: ZigConfigurable<*>): OptimizationConfigurable? {
            return if (cfg is OptimizationConfigurable && cfg.serializedName == serializedName) cfg else null
        }

        override fun apply(configurable: OptimizationConfigurable): Boolean {
            configurable.level = levels.item
            configurable.forced = forced.isSelected
            return true
        }

        override fun reset(configurable: OptimizationConfigurable) {
            levels.item = configurable.level
            forced.isSelected = configurable.forced
        }

        override fun construct(p: Panel): Unit = with(p) {
            row(ZigBrainsBundle.message("exec.option.label.optimization")) {
                cell(levels)
                cell(forced)
            }
        }

        override fun dispose() {
        }
    }
}

class ArgsConfigurable(
    @Transient private val serializedName: String,
    @Transient @Nls private val guiName: String,
	@Transient private val expandable: Boolean
) : ZigConfigurable<ArgsConfigurable>, Cloneable {
    var args: String = ""

    override fun readExternal(element: Element) {
        args = element.readString(serializedName) ?: element.readStrings(serializedName)?.joinToString(separator = " ") { if (it.contains(' ')) "\"$it\"" else it } ?: ""
    }

    fun argsSplit(): List<String> {
        return translateCommandline(args)
    }

    override fun writeExternal(element: Element) {
        element.writeString(serializedName, args)
    }

    override fun createEditor(): ZigConfigModule<ArgsConfigurable> {
        return ArgsConfigModule(serializedName, guiName, expandable)
    }

    override fun clone(): ArgsConfigurable {
        return super<Cloneable>.clone() as ArgsConfigurable
    }

    class ArgsConfigModule(
        private val serializedName: String,
        @Nls private val guiName: String,
        expandable: Boolean
    ) : ZigConfigModule<ArgsConfigurable> {
        private val argsField = if (expandable) ExpandableTextField() else JBTextField()

        override fun tryMatch(cfg: ZigConfigurable<*>): ArgsConfigurable? {
            return if (cfg is ArgsConfigurable && cfg.serializedName == serializedName) cfg else null
        }

        override fun apply(configurable: ArgsConfigurable): Boolean {
            configurable.args = argsField.text ?: ""
            return true
        }

        override fun reset(configurable: ArgsConfigurable) {
            argsField.text = configurable.args
        }

        override fun construct(p: Panel): Unit = with(p) {
            row(guiName) {
                cell(argsField).align(AlignX.FILL)
            }
        }

        override fun dispose() {

        }
    }
}

class EnvConfigurable : ZigConfigurable<EnvConfigurable>, Cloneable {
    var variables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT

    override fun readExternal(element: Element) {
        variables = EnvironmentVariablesData.readExternal(element)
    }

    override fun writeExternal(element: Element) {
		variables.writeExternal(element)
    }

    override fun createEditor(): ZigConfigModule<EnvConfigurable> {
        return EnvConfigModule()
    }

    override fun clone(): EnvConfigurable {
		val clone = super<Cloneable>.clone() as EnvConfigurable
		clone.variables = clone.variables.deepClonePolymorphic()
        return clone
    }

    class EnvConfigModule : ZigConfigModule<EnvConfigurable> {
        private val envComponent = EnvironmentVariablesTextFieldWithBrowseButton()

        override fun tryMatch(cfg: ZigConfigurable<*>): EnvConfigurable? {
            return cfg as? EnvConfigurable
        }

        override fun apply(configurable: EnvConfigurable): Boolean {
            configurable.variables = envComponent.data
            return true
        }

        override fun reset(configurable: EnvConfigurable) {
            envComponent.data = configurable.variables
        }

        override fun construct(p: Panel): Unit = with(p) {
            row(ExecutionBundle.message("environment.variables.component.title")) {
                cell(envComponent).align(AlignX.FILL)
            }
        }

        override fun dispose() {

        }
    }
}