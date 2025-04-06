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

package com.falsepattern.zigbrains.project.toolchain.ui.popup

import com.falsepattern.zigbrains.Icons
import com.falsepattern.zigbrains.project.toolchain.base.render
import com.falsepattern.zigbrains.project.toolchain.ui.Separator
import com.falsepattern.zigbrains.project.toolchain.ui.TCListElem
import com.falsepattern.zigbrains.project.toolchain.ui.TCListElemIn
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.CellRendererPanel
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.GroupHeaderSeparator
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.components.panels.OpaquePanel
import com.intellij.ui.popup.list.ComboBoxPopup
import com.intellij.util.Consumer
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.util.IdentityHashMap
import javax.accessibility.AccessibleContext
import javax.swing.JList
import javax.swing.border.Border

