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

package com.falsepattern.zigbrains.errorreporter

import com.falsepattern.zigbrains.ZigBrains
import com.falsepattern.zigbrains.ZigBrainsBundle
import com.falsepattern.zigbrains.shared.zigCoroutineScope
import com.intellij.ide.DataManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.idea.IdeaLogger
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.util.NlsActions
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.util.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.awt.Component

class ErrorReporter: ErrorReportSubmitter() {
    override fun getReportActionText(): @NlsActions.ActionText String {
        return ZigBrainsBundle.message("error-reporter.submit.action")
    }

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>
    ): Boolean {
        val dataContext = DataManager.getInstance().getDataContext(parentComponent)
        val project = CommonDataKeys.PROJECT.getData(dataContext)
        val lastAction = IdeaLogger.ourLastActionId

        project.zigCoroutineScope.launch {
            runCatching {
                val owner = project?.let { if (it.isDisposed) null else ModalTaskOwner.project(it) } ?: ModalTaskOwner.component(parentComponent)
                withModalProgress(owner, ZigBrainsBundle.message("error-reporter.submit.in-progress"), TaskCancellation.cancellable()) {
                    val event = events[0]

                    val builder = ErrorDataBuilder(event.throwable, lastAction)

                    builder.description = additionalInfo
                    builder.message = event.message

                    PluginManagerCore.getPlugin(ZigBrains.pluginId)?.let { plugin ->
                        builder.pluginName = plugin.name
                        builder.pluginVersion = plugin.version
                    }

                    builder.throwable = event.throwable
                    builder.attachments = event.attachments

                    val data = builder.build()

                    runInterruptible(Dispatchers.IO) {
                        AnonymousFeedback.sendFeedback(data)
                    }
                }
            }.onSuccess { response ->
                val jbStatus = when(response.status) {
                    ErrorResponse.Status.New -> SubmittedReportInfo.SubmissionStatus.NEW_ISSUE
                    ErrorResponse.Status.Duplicate -> SubmittedReportInfo.SubmissionStatus.DUPLICATE
                }
                consumer.consume(SubmittedReportInfo(response.url, response.linkText, jbStatus))
            }.onFailure {
                it.printStackTrace()
                consumer.consume(SubmittedReportInfo(null, null, SubmittedReportInfo.SubmissionStatus.FAILED))
            }
        }
        return true
    }
}