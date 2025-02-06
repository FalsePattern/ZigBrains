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

package com.falsepattern.zigbrains.shared

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

abstract class MultiConfigurable(private val configurables: List<SubConfigurable>): Configurable {
    override fun createComponent(): JComponent? {
        return panel {
            for (configurable in configurables) {
                configurable.createComponent(this)
            }
        }
    }

    override fun isModified(): Boolean {
        return configurables.any { it.isModified() }
    }

    override fun apply() {
        configurables.forEach { it.apply() }
    }

    override fun reset() {
        configurables.forEach { it.reset() }
    }

    override fun disposeUIResources() {
        configurables.forEach { Disposer.dispose(it) }
    }
}
