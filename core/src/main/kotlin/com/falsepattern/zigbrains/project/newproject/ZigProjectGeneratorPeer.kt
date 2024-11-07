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

package com.falsepattern.zigbrains.project.newproject

import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.platform.ProjectGeneratorPeer
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class ZigProjectGeneratorPeer(var handleGit: Boolean): ProjectGeneratorPeer<ZigProjectConfigurationData>, Disposable {
    private val newProjectPanel: ZigNewProjectPanel = ZigNewProjectPanel(handleGit).also { Disposer.register(this, it) }
    val myComponent: JComponent by lazy {
        panel {
            newProjectPanel.attach(this)
        }
    }

    override fun getComponent(myLocationField: TextFieldWithBrowseButton, checkValid: Runnable): JComponent {
        return myComponent
    }

    override fun buildUI(settingsStep: SettingsStep) {
        myComponent
    }

    override fun getSettings(): ZigProjectConfigurationData {
        return newProjectPanel.getData()
    }

    override fun validate(): ValidationInfo? {
        return null
    }

    override fun isBackgroundJobRunning(): Boolean {
        return false
    }

    override fun dispose() {
    }
}