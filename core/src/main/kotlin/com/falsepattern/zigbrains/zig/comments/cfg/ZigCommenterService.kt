/*
 * ZigBrains
 * Copyright (C) 2023-2026 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.falsepattern.zigbrains.zig.comments.cfg

import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute

@Service(Service.Level.PROJECT)
@State(
    name = "Commenter",
    storages = [Storage("zigbrains.xml")]
)
class ZigCommenterService(val project: Project): SerializablePersistentStateComponent<ZigCommenterService.State>(State()) {
    var commenterState: ZigCommenterState
        get() = state.state
        set(value) {
            updateState {
                it.copy(state = value)
            }
        }

    data class State(
        @JvmField
        @Attribute
        var state: ZigCommenterState = ZigCommenterState.Standard
    )

    companion object {
        fun getInstance(project: Project): ZigCommenterService = project.service<ZigCommenterService>()
    }
}