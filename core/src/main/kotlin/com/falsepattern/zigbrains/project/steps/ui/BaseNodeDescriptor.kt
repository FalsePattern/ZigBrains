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

package com.falsepattern.zigbrains.project.steps.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.PresentableNodeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import javax.swing.Icon

open class BaseNodeDescriptor<T>(project: Project?, displayName: String, displayIcon: Icon? = null, private var description: String? = null): PresentableNodeDescriptor<T>(project, null) {
    init {
        icon = displayIcon
        myName = displayName
        update()
    }

    override fun update(presentation: PresentationData) {
        presentation.setIcon(icon)
        presentation.addText(myName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        presentation.tooltip = description
    }

    override fun getElement(): T? {
        return null
    }
}