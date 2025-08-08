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

import kotlinx.serialization.Serializable

// just a namespace for stuff
object Serialization {
	@Serializable
	data class Project(
		/**
		 * The root of the project
		 */
		val path: String,
		/**
		 * The (top-level) steps the project declares
		 */
		val steps: List<Step>,
		/**
		 * The modules this project owns, may them be private or public
		 */
		val modules: List<Module>,
		/**
		 * Contains the index of a project in the projects list
		 */
		val dependencies: List<Dependency>,
	)
	@Serializable
	data class Step(
		/**
		 * Self-explanatory
		 */
		val name: String,
		/**
		 * Description given to the step
		 */
		val description: String,
		/**
		 * The kind of step, may be any one declared in `std.Build.Step.Id`
		 */
		val kind: String,
	)
	@Serializable
	data class Module(
		/**
		 * The root `.zig` file
		 */
		val root: String,
		/**
		 * Public modules are dependable on by other projects
		 */
		val public: Boolean,
		/**
		 * Imports of this module
		 */
		val imports: List<Import>,
	)
	@Serializable
	data class Import(
		/**
		 * Name under the import is available in code
		 */
		val name: String,
		/**
		 * The imported module's owner
		 */
		val owner: Int,
		/**
		 * The imported module's idx
		 */
		val module: Int,
	)
	@Serializable
	data class Dependency(
		/**
		 * The index of the project in the projects list
 		 */
		val project: Int,
		/**
		 * Whether the dependency was delcared lazy
 		 */
		val lazy: Boolean,
	)
}
