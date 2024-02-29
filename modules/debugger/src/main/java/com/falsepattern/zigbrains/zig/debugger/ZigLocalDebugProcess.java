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

package com.falsepattern.zigbrains.zig.debugger;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.cidr.execution.RunParameters;
import com.jetbrains.cidr.execution.debugger.CidrLocalDebugProcess;
import org.jetbrains.annotations.NotNull;

public class ZigLocalDebugProcess extends CidrLocalDebugProcess {
    public ZigLocalDebugProcess(@NotNull RunParameters parameters, @NotNull XDebugSession session, @NotNull TextConsoleBuilder consoleBuilder)
            throws ExecutionException {
        super(parameters, session, consoleBuilder, (project) -> Filter.EMPTY_ARRAY, false);
    }
}
