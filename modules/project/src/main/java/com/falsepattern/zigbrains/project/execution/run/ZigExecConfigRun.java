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

package com.falsepattern.zigbrains.project.execution.run;

import com.falsepattern.zigbrains.project.execution.base.ZigExecConfigBase;
import com.falsepattern.zigbrains.project.execution.base.ZigConfigEditor;
import com.falsepattern.zigbrains.project.util.ElementUtil;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Setter
@Getter
public class ZigExecConfigRun extends ZigExecConfigBase<ZigExecConfigRun> implements
        ZigConfigEditor.FilePathModule.Carrier, ZigConfigEditor.ColoredModule.Carrier {
    public String filePath = "";
    public boolean colored = true;
    public ZigExecConfigRun(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, "Zig Run");
    }

    @Override
    public String[] buildCommandLineArgs() {
        return new String[]{"run", "--color", colored ? "on" : "off", filePath};
    }

    @Override
    public @Nullable String suggestedName() {
        return "Run";
    }

    @Override
    public @NotNull List<ZigConfigEditor.ZigConfigModule<ZigExecConfigRun>> getEditorConfigModules() {
        val modules = super.getEditorConfigModules();
        modules.add(new ZigConfigEditor.FilePathModule<>());
        modules.add(new ZigConfigEditor.ColoredModule<>());
        return modules;
    }

    @Override
    public @Nullable ProfileStateRun getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new ProfileStateRun(environment, this);
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);

        ElementUtil.readString(element, "filePath").ifPresent(x -> filePath = x);
        ElementUtil.readBoolean(element, "colored").ifPresent(x -> colored = x);
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);

        ElementUtil.writeString(element, "filePath", filePath);
        ElementUtil.writeBoolean(element, "colored", colored);
    }
}