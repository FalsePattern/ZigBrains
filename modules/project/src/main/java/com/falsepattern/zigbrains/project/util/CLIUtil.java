/*
 * Copyright 2023-2024 FalsePattern
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

package com.falsepattern.zigbrains.project.util;

import com.falsepattern.zigbrains.project.execution.ZigCapturingProcessHandler;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CLIUtil {
    public static Optional<ProcessOutput> execute(GeneralCommandLine cli, int timeoutMillis) {
        val handler = ZigCapturingProcessHandler.startProcess(cli);
        return handler.map(h -> runProcessWithGlobalProgress(h, timeoutMillis));
    }

    public static ProcessOutput runProcessWithGlobalProgress(CapturingProcessHandler handler, @Nullable Integer timeoutMillis) {
        return runProcess(handler, ProgressManager.getGlobalProgressIndicator(), timeoutMillis);
    }

    public static ProcessOutput runProcess(CapturingProcessHandler handler, @Nullable ProgressIndicator indicator, @Nullable Integer timeoutMillis) {
        if (indicator != null && timeoutMillis != null) {
            return handler.runProcessWithProgressIndicator(indicator, timeoutMillis);
        } else if (indicator != null) {
            return handler.runProcessWithProgressIndicator(indicator);
        } else if (timeoutMillis != null) {
            return handler.runProcess(timeoutMillis);
        } else {
            return handler.runProcess();
        }
    }
}
