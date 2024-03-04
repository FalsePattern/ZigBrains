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

package com.falsepattern.zigbrains.project.execution.build;

import com.falsepattern.zigbrains.project.execution.base.ZigConfigEditor;
import com.falsepattern.zigbrains.project.execution.base.ZigExecConfigBase;
import com.falsepattern.zigbrains.project.util.ElementUtil;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.ui.dsl.builder.AlignY;
import com.intellij.ui.dsl.builder.Panel;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.apache.groovy.util.Arrays;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class ZigExecConfigBuild extends ZigExecConfigBase<ZigExecConfigBuild> implements
        ZigConfigEditor.ColoredModule.Carrier {
    public String extraArguments = "";
    public boolean colored = true;
    public ZigExecConfigBuild(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, "Zig Build");
    }

    @Override
    public String[] buildCommandLineArgs() {
        val base = new String[]{"build", "--color", colored ? "on" : "off"};
        if (extraArguments.isBlank()) {
            return base;
        } else {
            return Arrays.concat(base, extraArguments.split(" "));
        }
    }

    @Override
    public @Nullable String suggestedName() {
        return "Build";
    }

    @Override
    public @NotNull List<ZigConfigEditor.ZigConfigModule<ZigExecConfigBuild>> getEditorConfigModules() {
        val arr = super.getEditorConfigModules();
        arr.add(new ExtraArgsModule());
        arr.add(new ZigConfigEditor.ColoredModule<>());
        return arr;
    }

    @Override
    public @Nullable ProfileStateBuild getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new ProfileStateBuild(environment, this);
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);

        ElementUtil.readString(element, "extraArguments").ifPresent(x -> extraArguments = x);
        ElementUtil.readBoolean(element, "colored").ifPresent(x -> colored = x);
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);

        ElementUtil.writeString(element, "extraArguments", extraArguments);
        ElementUtil.writeBoolean(element, "colored", colored);
    }

    public static class ExtraArgsModule implements ZigConfigEditor.ZigConfigModule<ZigExecConfigBuild> {
        private final JBTextField extraArgs = new JBTextField();

        @Override
        public void applyTo(@NotNull ZigExecConfigBuild s) throws ConfigurationException {
            s.extraArguments = extraArgs.getText();
        }

        @Override
        public void resetFrom(@NotNull ZigExecConfigBuild s) {
            extraArgs.setText(Objects.requireNonNullElse(s.extraArguments, ""));
        }

        @Override
        public void construct(Panel p) {
            p.row("Extra arguments", (r) -> {
                r.cell(extraArgs).resizableColumn().align(AlignX.FILL).align(AlignY.FILL);
                return null;
            });
        }
    }
}
