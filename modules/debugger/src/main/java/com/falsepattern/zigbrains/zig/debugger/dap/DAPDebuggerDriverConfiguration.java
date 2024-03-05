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

package com.falsepattern.zigbrains.zig.debugger.dap;

import com.falsepattern.zigbrains.zig.ZigLanguage;
import com.intellij.execution.ExecutionException;
import com.intellij.lang.Language;
import com.intellij.openapi.util.Expirable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.UserDataHolderEx;
import com.jetbrains.cidr.ArchitectureType;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerCommandException;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import com.jetbrains.cidr.execution.debugger.backend.EvaluationContext;
import com.jetbrains.cidr.execution.debugger.backend.LLFrame;
import com.jetbrains.cidr.execution.debugger.backend.LLThread;
import com.jetbrains.cidr.execution.debugger.backend.LLValue;
import com.jetbrains.cidr.execution.debugger.backend.LLValueData;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DAPDebuggerDriverConfiguration extends DebuggerDriverConfiguration {
    @Override
    public abstract @NotNull String getDriverName();

    @Override
    public abstract @NotNull DebuggerDriver createDriver(@NotNull DebuggerDriver.Handler handler,
                                                @NotNull ArchitectureType architectureType) throws ExecutionException;

    public abstract void customizeInitializeArguments(InitializeRequestArguments initArgs);

    @Override
    public @NotNull Language getConsoleLanguage() {
        return ZigLanguage.INSTANCE;
    }

    @Override
    public EvaluationContext createEvaluationContext(@NotNull DebuggerDriver debuggerDriver, @Nullable Expirable expirable, @NotNull LLThread llThread, @NotNull LLFrame llFrame, @NotNull UserDataHolderEx userDataHolderEx) {
        return new EvaluationContext(debuggerDriver,expirable,llThread,llFrame,userDataHolderEx) {
            @Override
            public @NotNull String convertToRValue(@NotNull LLValueData llValueData, @NotNull Pair<LLValue, String> pair) throws DebuggerCommandException, ExecutionException {
                return cast(pair.getSecond(), pair.getFirst().getType());
            }
        } ;
    }
}
