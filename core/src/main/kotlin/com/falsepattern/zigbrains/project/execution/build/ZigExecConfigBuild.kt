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

package com.falsepattern.zigbrains.project.execution.build

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.buildscan.Serialization
import com.falsepattern.zigbrains.project.buildscan.zigBuildScan
import com.falsepattern.zigbrains.project.execution.base.*
import com.falsepattern.zigbrains.shared.ZBFeatures
import com.falsepattern.zigbrains.shared.cli.translateCommandline
import com.falsepattern.zigbrains.shared.element.readString
import com.falsepattern.zigbrains.shared.element.readStrings
import com.falsepattern.zigbrains.shared.element.writeString
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.TextFieldWithAutoCompletionListProvider
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import org.jdom.Element
import org.jetbrains.annotations.Nls
import javax.swing.Icon

class ZigExecConfigBuild(project: Project, factory: ConfigurationFactory): ZigExecConfig<ZigExecConfigBuild>(project, factory, ZigBrainsBundle.message("exec.type.build.label")) {
    var buildSteps = StepsConfigurable(project, "buildSteps", ZigBrainsBundle.message("exec.option.label.build.steps"))
        private set
    var extraArgs = ArgsConfigurable("compilerArgs", ZigBrainsBundle.message("exec.option.label.build.args"), true)
        private set
    var debugBuildSteps = StepsConfigurable(project, "debugBuildSteps", ZigBrainsBundle.message("exec.option.label.build.steps-debug"))
        private set
    var debugExtraArgs = ArgsConfigurable("debugCompilerArgs", ZigBrainsBundle.message("exec.option.label.build.args-debug"), true)
        private set
    var exePath = FilePathConfigurable("exePath", ZigBrainsBundle.message("exec.option.label.build.exe-path-debug"))
        private set
    var exeArgs = ArgsConfigurable("exeArgs", ZigBrainsBundle.message("exec.option.label.build.exe-args-debug"), true)
        private set

    @Throws(ExecutionException::class)
    override suspend fun buildCommandLineArgs(debug: Boolean): List<String> {
        val result = ArrayList<String>()
        result.add("build")
        val steps = if (debug) debugBuildSteps.argsSplit() else buildSteps.argsSplit()
        result.addAll(steps)
        result.addAll(if (debug) debugExtraArgs.argsSplit() else extraArgs.argsSplit())
        return result
    }

    override val suggestedName: String
        get() = ZigBrainsBundle.message("configuration.build.suggested-name")

    override fun clone(): ZigExecConfigBuild {
        val clone = super.clone()
        clone.buildSteps = buildSteps.clone()
        clone.exeArgs = exeArgs.clone()
        clone.exePath = exePath.clone()
        clone.exeArgs = exeArgs.clone()
        return clone
    }

    override fun getConfigurables(): List<ZigConfigurable<*>> {
        val baseCfg = super.getConfigurables() + listOf(buildSteps, extraArgs)
        return if (ZBFeatures.debug()) {
            baseCfg + listOf(debugBuildSteps, debugExtraArgs, exePath, exeArgs)
        } else {
            baseCfg
        }
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): ZigProfileState<ZigExecConfigBuild> {
        return ZigProfileStateBuild(environment, this)
    }
}


class StepsConfigurable(
	@Transient private val project: Project,
	@Transient private val serializedName: String,
	@Transient @Nls private val guiName: String
) : ZigConfigurable<StepsConfigurable>, Cloneable {
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

	override fun createEditor(): ZigConfigurable.ZigConfigModule<StepsConfigurable> {
		return StepsConfigModule(project, serializedName, guiName)
	}

	override fun clone(): StepsConfigurable {
		return super<Cloneable>.clone() as StepsConfigurable
	}

	class StepsConfigModule(
		project: Project,
		private val serializedName: String,
		@Nls private val guiName: String
	) : ZigConfigurable.ZigConfigModule<StepsConfigurable> {
		private val argsField = TextFieldWithAutoCompletion(project, StepsTextFieldCompletionProvider(project), true, "Steps...")

		override fun tryMatch(cfg: ZigConfigurable<*>): StepsConfigurable? {
			return if (cfg is StepsConfigurable && cfg.serializedName == serializedName) cfg else null
		}

		override fun apply(configurable: StepsConfigurable): Boolean {
			configurable.args = argsField.text
			return true
		}

		override fun reset(configurable: StepsConfigurable) {
			argsField.text = configurable.args
		}

		override fun construct(p: Panel): Unit = with(p) {
			row(guiName) {
				cell(argsField).align(AlignX.FILL)
			}
		}

		override fun dispose() {

		}

		class StepsTextFieldCompletionProvider(project: Project) : TextFieldWithAutoCompletionListProvider<Serialization.Step>(project.zigBuildScan.rootProject?.steps) {
			override fun getLookupString(item: Serialization.Step): String =
				item.name

			override fun getIcon(item: Serialization.Step): Icon? {
				return when(item.kind) {
					"install" -> AllIcons.Actions.Install
					"uninstall" -> AllIcons.Actions.Uninstall
					else -> AllIcons.RunConfigurations.TestState.Run
				}
			}

			override fun getTypeText(item: Serialization.Step): String? =
				item.kind

			override fun getTailText(item: Serialization.Step): String? =
				"   ${item.description}"
		}
	}
}
