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

package com.falsepattern.zigbrains.debugger.runner.test;

import com.falsepattern.zigbrains.debugger.runner.base.ZigDebugParametersEmitBinaryBase;
import com.falsepattern.zigbrains.project.execution.test.ProfileStateTest;
import com.falsepattern.zigbrains.project.toolchain.AbstractZigToolchain;
import com.falsepattern.zigbrains.debugger.runner.base.ZigDebugEmitBinaryInstaller;
import com.jetbrains.cidr.execution.Installer;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import org.jetbrains.annotations.NotNull;

public class ZigDebugParametersTest extends ZigDebugParametersEmitBinaryBase<ProfileStateTest> {
    public ZigDebugParametersTest(DebuggerDriverConfiguration driverConfiguration, AbstractZigToolchain toolchain, ProfileStateTest profileState) {
        super(driverConfiguration, toolchain, profileState, "test");
    }

    @Override
    public @NotNull Installer getInstaller() {
        assert executableFile != null;
        return new ZigDebugEmitBinaryInstaller<>(profileState, toolchain, executableFile, new String[0]);
    }
}
