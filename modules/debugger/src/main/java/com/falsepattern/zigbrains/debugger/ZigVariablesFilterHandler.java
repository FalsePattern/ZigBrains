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

package com.falsepattern.zigbrains.debugger;

import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XSourcePosition;
import com.jetbrains.cidr.execution.debugger.backend.LLValue;
import com.jetbrains.cidr.execution.debugger.evaluation.LocalVariablesFilterHandler;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ZigVariablesFilterHandler implements LocalVariablesFilterHandler {
    @NotNull
    @Override
    public CompletableFuture<List<LLValue>> filterVars(@NotNull Project project, @NotNull XSourcePosition xSourcePosition, @NotNull List<? extends LLValue> list) {
        return CompletableFuture.supplyAsync(() -> {
            val vf = xSourcePosition.getFile();
            if ("zig".equals(vf.getExtension())) {
                return new ArrayList<>(list);
            }
            return List.of();
        });
    }

    @Override
    public boolean canFilterAtPos(@NotNull Project proj, @NotNull XSourcePosition pos) {
        return "zig".equals(pos.getFile().getExtension());
    }
}
