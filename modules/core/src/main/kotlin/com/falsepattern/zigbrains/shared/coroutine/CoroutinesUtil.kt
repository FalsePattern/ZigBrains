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

package com.falsepattern.zigbrains.shared.coroutine

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.util.SuspendingLazy
import com.intellij.util.application
import kotlinx.coroutines.*

inline fun <T> runModalOrBlocking(taskOwnerFactory: () -> ModalTaskOwner, titleFactory: () -> String, cancellationFactory: () -> TaskCancellation = TaskCancellation::cancellable, noinline action: suspend CoroutineScope.() -> T): T {
    return if (application.isDispatchThread) {
        runWithModalProgressBlocking(taskOwnerFactory(), titleFactory(), cancellationFactory(), action)
    } else {
        runBlocking(block = action)
    }
}

inline fun <T> SuspendingLazy<T>.getOrAwaitModalOrBlocking(taskOwnerFactory: () -> ModalTaskOwner, titleFactory: () -> String, cancellationFactory: () -> TaskCancellation = TaskCancellation::cancellable): T {
    if (isInitialized()) {
        return getInitialized()
    } else {
        return runModalOrBlocking(taskOwnerFactory, titleFactory, cancellationFactory) {
            getValue()
        }
    }
}

suspend inline fun <T> withEDTContext(state: ModalityState = ModalityState.defaultModalityState(), noinline block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.EDT + state.asContextElement(), block = block)
}

suspend inline fun <T> runInterruptibleEDT(state: ModalityState = ModalityState.defaultModalityState(), noinline targetAction: () -> T): T {
    return runInterruptible(Dispatchers.EDT + state.asContextElement(), block = targetAction)
}

fun CoroutineScope.launchWithEDT(state: ModalityState = ModalityState.defaultModalityState(), block: suspend CoroutineScope.() -> Unit) {
    launch(Dispatchers.EDT + state.asContextElement(), block = block)
}