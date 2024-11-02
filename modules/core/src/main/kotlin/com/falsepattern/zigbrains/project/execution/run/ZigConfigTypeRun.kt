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

package com.falsepattern.zigbrains.project.execution.run

import com.falsepattern.zigbrains.Icons
import com.falsepattern.zigbrains.ZigBrainsBundle
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class ZigConfigTypeRun : ConfigurationTypeBase(
    IDENTIFIER,
    ZigBrainsBundle.message("configuration.run.name"),
    ZigBrainsBundle.message("configuration.run.description"),
    Icons.ZIG
) {
    init {
        addFactory(ConfigFactoryRun())
    }

    inner class ConfigFactoryRun: ConfigurationFactory(this@ZigConfigTypeRun) {
        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            return ZigExecConfigRun(project, this)
        }

        override fun getId(): String {
            return IDENTIFIER
        }
    }
}

private const val IDENTIFIER = "ZIGBRAINS_RUN"