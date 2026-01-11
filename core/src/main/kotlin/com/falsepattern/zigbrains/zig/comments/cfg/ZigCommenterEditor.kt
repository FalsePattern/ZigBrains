/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2026 FalsePattern
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

package com.falsepattern.zigbrains.zig.comments.cfg

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.shared.SubConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.Panel

class ZigCommenterEditor: SubConfigurable<Project> {
    private var cb: ComboBox<ZigCommenterState>? = null
    override fun attach(panel: Panel): Unit = with(panel) {
        row(ZigBrainsBundle.message("settings.commenter.label")) {
            comboBox(ZigCommenterState.entries).component.let {
                cb = it
            }
        }
    }

    override fun isModified(context: Project): Boolean {
        return ZigCommenterService.getInstance(context).commenterState != cb?.selectedItem
    }

    override fun apply(context: Project) {
        ZigCommenterService.getInstance(context).commenterState = (cb?.selectedItem as? ZigCommenterState) ?: ZigCommenterState.Standard
    }

    override fun reset(context: Project?) {
        if (context == null) {
            cb?.selectedItem = ZigCommenterState.Standard
            return
        }
        cb?.selectedItem = ZigCommenterService.getInstance(context).commenterState
    }

    override fun dispose() {
    }

    class Provider: ZigProjectConfigurationProvider {
        override fun create(sharedState: ZigProjectConfigurationProvider.IUserDataBridge): SubConfigurable<Project>? {
            return ZigCommenterEditor()
        }

        override val index: Int
            get() = 200

    }
}