/*
 * ZigBrains
 * Copyright (C) 2023-2025 FalsePattern
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
package com.falsepattern.zigbrains.project.buildscan

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class ZigScanBuildActivity : ProjectActivity {
	override suspend fun execute(project: Project) {
		// initial loads may use the cached state, as long `build.zig` and `build.zig.zon` didn't change
		project.zigBuildScan.triggerReload(true)
	}
}
