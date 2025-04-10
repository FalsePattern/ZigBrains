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

package com.falsepattern.zigbrains.project.settings

import com.falsepattern.zigbrains.shared.SubConfigurable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase

interface ZigProjectConfigurationProvider {
    fun create(project: Project?, sharedState: IUserDataBridge): SubConfigurable<Project>?
    val index: Int
    companion object {
        private val EXTENSION_POINT_NAME = ExtensionPointName.create<ZigProjectConfigurationProvider>("com.falsepattern.zigbrains.projectConfigProvider")
        fun createPanels(project: Project?): List<SubConfigurable<Project>> {
            val sharedState = UserDataBridge()
            return EXTENSION_POINT_NAME.extensionList.sortedBy { it.index }.mapNotNull { it.create(project, sharedState) }
        }
    }

    interface IUserDataBridge: UserDataHolder {
        fun addUserDataChangeListener(listener: UserDataListener)
        fun removeUserDataChangeListener(listener: UserDataListener)
    }

    interface UserDataListener {
        fun onUserDataChanged(key: Key<*>)
    }

    class UserDataBridge: UserDataHolderBase(), IUserDataBridge {
        private val listeners = ArrayList<UserDataListener>()
        override fun <T : Any?> putUserData(key: Key<T?>, value: T?) {
            super.putUserData(key, value)
            synchronized(listeners) {
                listeners.forEach { listener ->
                    listener.onUserDataChanged(key)
                }
            }
        }

        override fun addUserDataChangeListener(listener: UserDataListener) {
            synchronized(listeners) {
                listeners.add(listener)
            }
        }

        override fun removeUserDataChangeListener(listener: UserDataListener) {
            synchronized(listeners) {
                listeners.remove(listener)
            }
        }
    }
}
