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

package com.falsepattern.zigbrains.debugger.runner.base;

import com.falsepattern.zigbrains.project.execution.base.ProfileStateBase;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.intellij.util.system.CpuArch;
import com.jetbrains.cidr.ArchitectureType;
import com.jetbrains.cidr.execution.RunParameters;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@RequiredArgsConstructor
public abstract class ZigDebugParametersBase<ProfileState extends ProfileStateBase<?>> extends RunParameters {
    private final DebuggerDriverConfiguration driverConfiguration;
    protected final AbstractZigToolchain toolchain;
    protected final ProfileState profileState;

    @Override
    public @NotNull DebuggerDriverConfiguration getDebuggerDriverConfiguration() {
        return driverConfiguration;
    }

    @Override
    public @Nullable String getArchitectureId() {
        return ArchitectureType.forVmCpuArch(CpuArch.CURRENT).getId();
    }
}
