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

package com.falsepattern.zigbrains.shared

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.application
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
class ZigProjectService(val cs: CoroutineScope)

@Service
class ZigAppService(val cs: CoroutineScope)

val Project?.zigCoroutineScope get() =
    if (this == null || this.isDefault)
        application.service<ZigAppService>().cs
    else
        this.service<ZigProjectService>().cs

val zigCoroutineScope get() = application.service<ZigAppService>().cs