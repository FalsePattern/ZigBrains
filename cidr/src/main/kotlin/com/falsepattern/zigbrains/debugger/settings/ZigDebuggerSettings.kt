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

package com.falsepattern.zigbrains.debugger.settings

import com.falsepattern.zigbrains.debugger.ZigDebugBundle
import com.falsepattern.zigbrains.debugger.toolchain.DebuggerKind
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SimpleConfigurable
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.xdebugger.settings.DebuggerSettingsCategory
import com.intellij.xdebugger.settings.XDebuggerSettings

class ZigDebuggerSettings: XDebuggerSettings<ZigDebuggerSettings>("Zig") {
    var debuggerKind = DebuggerKind.default

    var downloadAutomatically = false
    var useClion = true
    var msvcConsent = MSVCDownloadPermission.AskMe

    override fun getState(): ZigDebuggerSettings {
        return this
    }

    override fun loadState(p0: ZigDebuggerSettings) {
        XmlSerializerUtil.copyBean(p0, this)
    }

    override fun createConfigurables(category: DebuggerSettingsCategory): Collection<Configurable> {
        val configurable = when(category) {
            DebuggerSettingsCategory.GENERAL -> createGeneralSettingsConfigurable()
            else -> null
        }
        return configurable?.let { listOf(configurable) } ?: emptyList()
    }

    private fun createGeneralSettingsConfigurable(): Configurable {
        return SimpleConfigurable.create(
            GENERAL_SETTINGS_ID,
            ZigDebugBundle.message("settings.debugger.title"),
            ZigDebuggerGeneralSettingsConfigurableUi::class.java,
        ) {
            instance
        }
    }

    companion object {
        val instance: ZigDebuggerSettings get() = getInstance(ZigDebuggerSettings::class.java)
    }
}

private const val GENERAL_SETTINGS_ID = "Debugger.Zig.General"