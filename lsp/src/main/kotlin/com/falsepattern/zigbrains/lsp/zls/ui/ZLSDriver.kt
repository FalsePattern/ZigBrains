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

import com.falsepattern.zigbrains.lsp.zls.ZLSConfigurable
import com.falsepattern.zigbrains.lsp.zls.ZLSVersion
import com.falsepattern.zigbrains.lsp.zls.downloader.ZLSDownloader
import com.falsepattern.zigbrains.lsp.zls.downloader.ZLSLocalSelector
import com.falsepattern.zigbrains.lsp.zls.zlsInstallations
import com.falsepattern.zigbrains.project.settings.ZigProjectConfigurationProvider
import com.falsepattern.zigbrains.project.toolchain.base.ZigToolchainConfigurable
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
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.util.text.SemVer
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.awt.Component
import java.util.UUID

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
            is ListElem.FromDisk<*> -> ZLSLocalSelector(context).browse()
            else -> null
        }?.let { zlsInstallations.registerNew(it) }
    }

    object ForList: ZLSDriver {
        override fun constructModelList(): List<ListElemIn<ZLSVersion>> {
            return listOf(ListElem.None(), Separator("", true), ListElem.FromDisk())
        }
    }

    @JvmRecord
    data class ForSelector(private val data: ZigProjectConfigurationProvider.IUserDataBridge?): ZLSDriver {
        override fun constructModelList(): List<ListElemIn<ZLSVersion>> {
            val res = ArrayList<ListElemIn<ZLSVersion>>()
            res.add(ListElem.None())
            res.add(compatibleInstallations().asPending())
            res.add(Separator("", true))
            res.addAll(ListElem.fetchGroup())
            return res
        }

        override suspend fun resolvePseudo(
            context: Component,
            elem: ListElem.Pseudo<ZLSVersion>
        ): UUID? {
            return when(elem) {
                is ListElem.FromDisk<*> -> ZLSLocalSelector(context).browse()
                is ListElem.Download<*> -> ZLSDownloader(context, data).download()
                else -> null
            }?.let { zlsInstallations.registerNew(it) }
        }
        private fun compatibleInstallations(): Flow<Actual<ZLSVersion>> = flow {
            val project = data?.getUserData(ZigProjectConfigurationProvider.PROJECT_KEY)
            val toolchainVersion = data?.getUserData(ZigToolchainConfigurable.TOOLCHAIN_KEY)
                ?.get()
                ?.zig
                ?.getEnv(project)
                ?.getOrNull()
                ?.version
                ?.let { SemVer.parseFromText(it) }
                ?: return@flow
            zlsInstallations.forEach { (uuid, version) ->
                val zlsVersion = version.version() ?: return@forEach
                if (numericVersionEquals(toolchainVersion, zlsVersion)) {
                    emit(Actual(uuid, version))
                }
            }
        }
    }
}

private fun numericVersionEquals(a: SemVer, b: SemVer): Boolean {
    return a.major == b.major && a.minor == b.minor && a.patch == b.patch
}