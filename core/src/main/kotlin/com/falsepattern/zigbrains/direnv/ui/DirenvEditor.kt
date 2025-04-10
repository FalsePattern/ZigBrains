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

package com.falsepattern.zigbrains.direnv.ui

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.direnv.DirenvService
import com.falsepattern.zigbrains.direnv.DirenvState
import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.shared.SubConfigurable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.Panel
import java.awt.event.ItemEvent

abstract class DirenvEditor<T>(private val sharedState: ZigProjectConfigurationProvider.IUserDataBridge?): SubConfigurable<T> {
    private var cb: ComboBox<DirenvState>? = null
    override fun attach(panel: Panel): Unit = with(panel) {
        row(ZigBrainsBundle.message("settings.direnv.enable.label")) {
            comboBox(DirenvState.entries).component.let {
                cb = it
                it.addItemListener { e ->
                    if (e.stateChange != ItemEvent.SELECTED)
                        return@addItemListener
                    sharedState
                }
            }
        }
    }

    override fun isModified(context: T): Boolean {
        return isEnabled(context) != cb?.selectedItem as DirenvState
    }

    override fun apply(context: T) {
        setEnabled(context, cb?.selectedItem as DirenvState)
    }

    override fun reset(context: T?) {
        if (context == null) {
            cb?.selectedItem = DirenvState.Auto
            return
        }
        cb?.selectedItem = isEnabled(context)
    }

    override fun dispose() {
    }

    abstract fun isEnabled(context: T): DirenvState
    abstract fun setEnabled(context: T, value: DirenvState)

    class ForProject(sharedState: ZigProjectConfigurationProvider.IUserDataBridge) : DirenvEditor<Project>(sharedState) {
        override fun isEnabled(context: Project): DirenvState {
            return context.service<DirenvService>().isEnabledRaw
        }

        override fun setEnabled(context: Project, value: DirenvState) {
            context.service<DirenvService>().isEnabledRaw = value
        }
    }

    class Provider: ZigProjectConfigurationProvider {
        override fun create(project: Project?, sharedState: ZigProjectConfigurationProvider.IUserDataBridge): SubConfigurable<Project>? {
            if (project?.isDefault != false) {
                return null
            }
            sharedState.putUserData(DirenvService.STATE_KEY, DirenvState.Auto)
            return ForProject(sharedState)
        }

        override val index: Int
            get() = 1
    }
}