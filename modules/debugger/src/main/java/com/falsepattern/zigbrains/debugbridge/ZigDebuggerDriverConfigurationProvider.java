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

package com.falsepattern.zigbrains.debugbridge;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface ZigDebuggerDriverConfigurationProvider {
    ExtensionPointName<ZigDebuggerDriverConfigurationProvider> EXTENSION_POINT_NAME = ExtensionPointName.create("com.falsepattern.zigbrains.debuggerDriverProvider");

    @SuppressWarnings("unchecked")
    static @NotNull Stream<DebuggerDriverConfiguration> findDebuggerConfigurations(Project project, boolean isElevated, boolean emulateTerminal) {
        return (Stream<DebuggerDriverConfiguration>) EXTENSION_POINT_NAME.getExtensionList()
                                                                        .stream()
                                                                        .map(it -> it.getDebuggerConfiguration(project, isElevated, emulateTerminal))
                                                                        .filter(Objects::nonNull)
                                                                        .map((Function<? super Supplier<DebuggerDriverConfiguration>, ?>) Supplier::get);
    }

    @Nullable Supplier<DebuggerDriverConfiguration> getDebuggerConfiguration(Project project, boolean isElevated, boolean emulateTerminal);
}
