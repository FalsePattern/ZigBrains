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

package com.falsepattern.zigbrains.lsp.zls.ui

import com.falsepattern.zigbrains.direnv.DirenvService
import com.falsepattern.zigbrains.direnv.Env
import com.falsepattern.zigbrains.lsp.ZLSBundle
import com.falsepattern.zigbrains.lsp.zls.ZLSConfigurable
import com.falsepattern.zigbrains.lsp.zls.ZLSVersion
import com.falsepattern.zigbrains.lsp.zls.downloader.ZLSDownloader
import com.falsepattern.zigbrains.lsp.zls.downloader.ZLSLocalSelector
import com.falsepattern.zigbrains.lsp.zls.zlsInstallations
import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchainConfigurable.Companion.TOOLCHAIN_KEY
import com.falsepattern.zigbrains.shared.UUIDMapSerializable
import com.falsepattern.zigbrains.shared.ui.ListElem
import com.falsepattern.zigbrains.shared.ui.ListElem.One.Actual
import com.falsepattern.zigbrains.shared.ui.ListElemIn
import com.falsepattern.zigbrains.shared.ui.Separator
import com.falsepattern.zigbrains.shared.ui.UUIDComboBoxDriver
import com.falsepattern.zigbrains.shared.ui.ZBComboBox
import com.falsepattern.zigbrains.shared.ui.ZBContext
import com.falsepattern.zigbrains.shared.ui.ZBModel
import com.falsepattern.zigbrains.shared.ui.asPending
import com.falsepattern.zigbrains.shared.withUniqueName
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.util.system.OS
import com.intellij.util.text.SemVer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.awt.Component
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.isDirectory
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile

sealed interface ZLSDriver: UUIDComboBoxDriver<ZLSVersion> {
    override val theMap: UUIDMapSerializable.Converting<ZLSVersion, *, *>
        get() = zlsInstallations

    override fun createContext(model: ZBModel<ZLSVersion>): ZBContext<ZLSVersion> {
        return ZLSContext(null, model)
    }

    override fun createComboBox(model: ZBModel<ZLSVersion>): ZBComboBox<ZLSVersion> {
        return ZLSComboBox(model)
    }

    override fun createNamedConfigurable(uuid: UUID, elem: ZLSVersion): NamedConfigurable<UUID> {
        return ZLSConfigurable(uuid, elem)
    }

    override suspend fun resolvePseudo(
        context: Component,
        elem: ListElem.Pseudo<ZLSVersion>
    ): UUID? {
        return when(elem) {
            is ListElem.One.Suggested -> zlsInstallations.withUniqueName(elem.instance)
            is ListElem.FromDisk -> ZLSLocalSelector(context).browse()
            is ListElem.Download -> ZLSDownloader(context, data).download()
        }?.let { zlsInstallations.registerNew(it) }
    }

    val data: ZigProjectConfigurationProvider.IUserDataBridge?

    object ForList: ZLSDriver {
        override suspend fun constructModelList(): List<ListElemIn<ZLSVersion>> {
            val res = ArrayList<ListElemIn<ZLSVersion>>()
            res.addAll(ListElem.fetchGroup())
            res.add(Separator(ZLSBundle.message("settings.model.detected.separator"), true))
            res.add(suggestZLSVersions().asPending())
            return res
        }

        override val data: ZigProjectConfigurationProvider.IUserDataBridge?
            get() = null
    }

    @JvmRecord
    data class ForSelector(override val data: ZigProjectConfigurationProvider.IUserDataBridge?): ZLSDriver {
        override suspend fun constructModelList(): List<ListElemIn<ZLSVersion>> {
            val (project, toolchainVersion) = unpack(data)
            if (toolchainVersion == null) {
                return listOf(ListElem.None())
            }
            val res = ArrayList<ListElemIn<ZLSVersion>>()
            res.add(ListElem.None())
            res.addAll(compatibleInstallations(toolchainVersion))
            res.add(Separator("", true))
            res.addAll(ListElem.fetchGroup())
            res.add(Separator(ZLSBundle.message("settings.model.detected.separator"), true))
            res.add(suggestZLSVersions(project, data, toolchainVersion).asPending())
            return res
        }
    }
}

private suspend fun unpack(data: ZigProjectConfigurationProvider.IUserDataBridge?): Pair<Project?, SemVer?> {
    val toolchain = data?.getUserData(TOOLCHAIN_KEY)?.get()
    val project = data?.getUserData(ZigProjectConfigurationProvider.PROJECT_KEY)
    val toolchainVersion = toolchain
        ?.zig
        ?.getEnv(project)
        ?.getOrNull()
        ?.version
        ?.let { SemVer.parseFromText(it) }
    return project to toolchainVersion
}

private fun suggestZLSVersions(project: Project? = null, data: ZigProjectConfigurationProvider.IUserDataBridge? = null, toolchainVersion: SemVer? = null): Flow<ZLSVersion> = flow {
    val env = if (project != null && DirenvService.getStateFor(data, project).isEnabled(project)) {
        DirenvService.getInstance(project).import()
    } else {
        Env.empty
    }
    val existing = zlsInstallations.map { (_, zls) -> zls }
    env.findAllExecutablesOnPATH("zls").collect { path ->
        if (existing.any { it.path == path }) {
            return@collect
        }
        emitIfCompatible(path, toolchainVersion)
    }
    val exe = if (SystemInfo.isWindows) "zls.exe" else "zls"
    getWellKnownZLS().forEach { wellKnown ->
        runCatching {
            Files.newDirectoryStream(wellKnown).use { stream ->
                stream.asSequence().filterNotNull().forEach { dir ->
                    val path = dir.resolve(exe)
                    if (!path.isRegularFile() || !path.isExecutable()) {
                        return@forEach
                    }
                    if (existing.any { it.path == path }) {
                        return@forEach
                    }
                    emitIfCompatible(path, toolchainVersion)
                }
            }
        }
    }
}.flowOn(Dispatchers.IO)

private suspend fun FlowCollector<ZLSVersion>.emitIfCompatible(path: Path, toolchainVersion: SemVer?) {
    val ver = ZLSVersion.tryFromPath(path) ?: return
    if (isCompatible(ver, toolchainVersion)) {
        emit(ver)
    }
}

private suspend fun compatibleInstallations(toolchainVersion: SemVer): List<Actual<ZLSVersion>> {
    return zlsInstallations.mapNotNull { (uuid, version) ->
        if (!isCompatible(version, toolchainVersion)) {
            return@mapNotNull null
        }
        Actual(uuid, version)
    }
}

private suspend fun isCompatible(version: ZLSVersion, toolchainVersion: SemVer?): Boolean {
    if (toolchainVersion == null)
        return true
    val zlsVersion = version.version() ?: return false
    return numericVersionEquals(zlsVersion, toolchainVersion)
}

private fun numericVersionEquals(a: SemVer, b: SemVer): Boolean {
    return a.major == b.major && a.minor == b.minor && a.patch == b.patch
}


fun getSuggestedZLSPath(): Path? {
    return getWellKnownZLS().getOrNull(0)
}

/**
 * Returns the paths to the following list of folders:
 *
 * 1. DATA/zls
 * 2. HOME/.zig
 *
 * Where DATA is:
 *  - ~/Library on macOS
 *  - %LOCALAPPDATA% on Windows
 *  - $XDG_DATA_HOME (or ~/.local/share if not set) on other OSes
 *
 * and HOME is the user home path
 */
private fun getWellKnownZLS(): List<Path> {
    val home = System.getProperty("user.home")?.toNioPathOrNull() ?: return emptyList()
    val xdgDataHome = when(OS.CURRENT) {
        OS.macOS -> home.resolve("Library")
        OS.Windows -> System.getenv("LOCALAPPDATA")?.toNioPathOrNull()
        else -> System.getenv("XDG_DATA_HOME")?.toNioPathOrNull() ?: home.resolve(Path.of(".local", "share"))
    }
    val res = ArrayList<Path>()
    if (xdgDataHome != null && xdgDataHome.isDirectory()) {
        res.add(xdgDataHome.resolve("zls"))
    }
    res.add(home.resolve(".zls"))
    return res
}