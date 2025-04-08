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

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

interface SubConfigurable<T>: Disposable {
    fun attach(panel: Panel)
    fun isModified(context: T): Boolean
    fun apply(context: T)
    fun reset(context: T?)

    val newProjectBeforeInitSelector: Boolean get() = false

    abstract class Adapter<T>: Configurable {
        private var myConfigurable: SubConfigurable<T>? = null

        abstract fun instantiate(): SubConfigurable<T>
        protected abstract val context: T

        override fun createComponent(): JComponent? {
            if (myConfigurable != null) {
                disposeUIResources()
            }
            val configurable = instantiate()
            configurable.reset(context)
            myConfigurable = configurable
            return panel {
                configurable.attach(this)
            }
        }

        override fun isModified(): Boolean {
            return myConfigurable?.isModified(context) == true
        }

        override fun apply() {
            myConfigurable?.apply(context)
        }

        override fun reset() {
            myConfigurable?.reset(context)
        }

        override fun disposeUIResources() {
            val configurable = myConfigurable
            myConfigurable = null
            configurable?.let { Disposer.dispose(it) }
            super.disposeUIResources()
        }
    }
}