/*
 * Copyright 2023 FalsePattern
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.falsepattern.zigbrains.project.openapi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.util.Alarm;
import lombok.val;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIDebouncer {
    private final MyDisposable parentDisposable;
    private final int delayMs;

    private final Alarm alarm;


    public UIDebouncer(MyDisposable parentDisposable) {
        this(parentDisposable, 200);
    }

    public UIDebouncer(MyDisposable parentDisposable, int delayMs) {
        this.parentDisposable = parentDisposable;
        this.delayMs = delayMs;
        alarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, parentDisposable);
    }

    public <T> void run(Supplier<T> onPooledThread, Consumer<T> onUIThread) {
        if (parentDisposable.isDisposed()) {
            return;
        }

        alarm.cancelAllRequests();
        alarm.addRequest(() -> {
            val r = onPooledThread.get();
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!parentDisposable.isDisposed()) {
                    onUIThread.accept(r);
                }
            }, ModalityState.any());
        }, delayMs);
    }
}
