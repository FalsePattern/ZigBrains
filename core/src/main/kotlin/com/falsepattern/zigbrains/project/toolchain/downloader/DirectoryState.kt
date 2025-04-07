package com.falsepattern.zigbrains.project.toolchain.downloader

import java.nio.file.Files
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

enum class DirectoryState {
    Invalid,
    NotAbsolute,
    NotDirectory,
    NotEmpty,
    CreateNew,
    Ok;

    fun isValid(): Boolean {
        return when(this) {
            Invalid, NotAbsolute, NotDirectory, NotEmpty -> false
            CreateNew, Ok -> true
        }
    }

    companion object {
        @JvmStatic
        fun determine(path: Path?): DirectoryState {
            if (path == null) {
                return Invalid
            }
            if (!path.isAbsolute) {
                return NotAbsolute
            }
            if (!path.exists()) {
                var parent: Path? = path.parent
                while(parent != null) {
                    if (!parent.exists()) {
                        parent = parent.parent
                        continue
                    }
                    if (!parent.isDirectory()) {
                        return NotDirectory
                    }
                    return CreateNew
                }
                return Invalid
            }
            if (!path.isDirectory()) {
                return NotDirectory
            }
            val isEmpty = Files.newDirectoryStream(path).use { !it.iterator().hasNext() }
            if (!isEmpty) {
                return NotEmpty
            }
            return Ok
        }
    }
}