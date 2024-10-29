package com.falsepattern.zigbrains.lsp

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider

class ZigStreamConnectionProvider(private val project: Project): OSProcessStreamConnectionProvider() {
    init {

    }

    companion object {
        private val LOG = Logger.getInstance(ZigStreamConnectionProvider::class.java)

        suspend fun getCommand(project: Project, full: Boolean) {

        }
    }
}

