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

package com.falsepattern.zigbrains.project.toolchain.ui

import com.falsepattern.zigbrains.ZigBrainsBundle
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel

abstract class ImmutableNamedElementPanelBase<T>: ImmutableElementPanel<T> {
    private val nameField = JBTextField(25)

    protected var nameFieldValue: String?
        get() = nameField.text.ifBlank { null }
        set(value) {nameField.text = value ?: ""}

    override fun attach(p: Panel): Unit = with(p) {
        row(ZigBrainsBundle.message("settings.toolchain.base.name.label")) {
            cell(nameField).resizableColumn().align(AlignX.FILL)
        }
        separator()
    }
}