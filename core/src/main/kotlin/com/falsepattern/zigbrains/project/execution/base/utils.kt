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

package com.falsepattern.zigbrains.project.execution.base

import com.falsepattern.zigbrains.zig.psi.ZigContainerMembers
import com.falsepattern.zigbrains.zig.psi.ZigFile
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.psi.util.childrenOfType

fun ZigFile.hasMainFunction(): Boolean {
    val members = childrenOfType<ZigContainerMembers>().firstOrNull() ?: return false
    return members.containerDeclarationList.any { it.decl?.fnProto?.identifier?.textMatches("main") == true }
}

fun ZigFile.hasTests(): Boolean {
    val members = childrenOfType<ZigContainerMembers>().firstOrNull() ?: return false
    return members.containerDeclarationList.any { it.testDecl != null }
}

fun VirtualFile.findBuildZig(): VirtualFile? {
    var parent = this.parent
    while (parent != null) {
        parent.children.forEach {
            if (it.isFile && it.name == "build.zig") {
                return it
            }
        }
        parent = parent.parent
    }
    return null
}

fun VirtualFile.isBuildZig(): Boolean {
    return name == "build.zig"
}