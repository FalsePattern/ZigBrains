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

package com.falsepattern.zigbrains

import com.falsepattern.zigbrains.direnv.DirenvCmd
import com.falsepattern.zigbrains.direnv.emptyEnv
import com.falsepattern.zigbrains.direnv.getDirenv
import com.falsepattern.zigbrains.lsp.settings.zlsSettings
import com.falsepattern.zigbrains.project.settings.zigProjectSettings
import com.falsepattern.zigbrains.project.toolchain.LocalZigToolchain
import com.falsepattern.zigbrains.project.toolchain.ZigToolchainProvider
import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.PluginManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.UserDataHolderBase
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.io.path.pathString

class ZBStartup: ProjectActivity {
    var firstInit = true
    override suspend fun execute(project: Project) {
        if (firstInit) {
            firstInit = false
            if (!PluginManager.isPluginInstalled(PluginId.getId("com.intellij.modules.cidr.debugger")) &&
                PluginManager.isPluginInstalled(PluginId.getId("com.intellij.modules.nativeDebug-plugin-capable"))) {
                val notif = Notification(
                    "zigbrains",
                    ZigBrainsBundle.message("notification.title.native-debug"),
                    ZigBrainsBundle.message("notification.content.native-debug"),
                    NotificationType.INFORMATION
                )
                if (JBInternalPluginManagerConfigurable.successful) {
                    notif.addAction(object: NotificationAction(ZigBrainsBundle.message("notification.content.native-debug.market")) {
                        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                            val configurable = JBInternalPluginManagerConfigurable()
                            ShowSettingsUtil.getInstance().editConfigurable(null as Project?, configurable.instance) {
                                configurable.openMarketplaceTab("/vendor:\"JetBrains s.r.o.\" /tag:Debugging \"Native Debugging Support\"")
                            }
                        }
                    })
                }
                notif.addAction(object: NotificationAction(ZigBrainsBundle.message("notification.content.native-debug.browser")) {
                    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                        BrowserUtil.browse("https://plugins.jetbrains.com/plugin/12775-native-debugging-support")
                    }
                })
                notif.notify(null)
            }
        }
        //Autodetection
        val zigProjectState = project.zigProjectSettings.state
        if (zigProjectState.toolchainPath.isNullOrBlank()) {
            val data = UserDataHolderBase()
            data.putUserData(LocalZigToolchain.DIRENV_KEY,
                DirenvCmd.direnvInstalled() && !project.isDefault && zigProjectState.direnv
            )
            val tc = ZigToolchainProvider.suggestToolchain(project, data) ?: return
            if (tc is LocalZigToolchain) {
                zigProjectState.toolchainPath = tc.location.pathString
                project.zigProjectSettings.state = zigProjectState
            }
        }
        val zlsState = project.zlsSettings.state
        if (zlsState.zlsPath.isBlank()) {
            val env = if (DirenvCmd.direnvInstalled() && !project.isDefault && zlsState.direnv)
                project.getDirenv()
            else
                emptyEnv
            env.findExecutableOnPATH("zls")?.let {
                zlsState.zlsPath = it.pathString
                project.zlsSettings.state = zlsState
            }
        }
    }
}

//JetBrains Internal API, but we need to access it, so access it reflectively (hopefully safe enough to pass verifier)
private class JBInternalPluginManagerConfigurable {
    init {
        if (!successful) {
            throw IllegalStateException()
        }
    }
    val instance = constructor.newInstance() as Configurable

    fun openMarketplaceTab(option: String) {
        openMarketplaceTab.invoke(instance, option)
    }

    companion object {
        private lateinit var constructor: Constructor<*>
        private lateinit var openMarketplaceTab: Method
        val successful: Boolean

        init {
            lateinit var constructor: Constructor<*>
            lateinit var openMarketplaceTab: Method
            val successful = try {
                val theClass = Class.forName("com_intellij_ide_plugins_PluginManagerConfigurable".replace('_', '.'))
                constructor = theClass.getDeclaredConstructor().apply { isAccessible = true }
                openMarketplaceTab = theClass.getDeclaredMethod("openMarketplaceTab", String::class.java).apply { isAccessible = true }
                true
            } catch (_: Throwable) { false }
            if (successful) {
                this.constructor = constructor
                this.openMarketplaceTab = openMarketplaceTab
            }
            this.successful = successful
        }
    }
}