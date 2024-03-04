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
import lombok.val;
import org.apache.groovy.util.Arrays;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ZigExecConfigBuild extends ZigExecConfigBase<ZigExecConfigBuild> {
    public String extraArguments = "";
    public ZigExecConfigBuild(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, "Zig Build");
    }

    @Override
    public String[] buildCommandLineArgs() {
        val base = new String[]{"build"};
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
    public @NotNull Editor getConfigurationEditor() {
        return new Editor();
    }

    @Override
    public @Nullable ProfileStateBuild getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new ProfileStateBuild(environment, this);
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);

        val extraArguments = ElementUtil.readString(element, "extraArguments");
        if (extraArguments != null) {
            this.extraArguments = extraArguments;
        }
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);

        ElementUtil.writeString(element, "extraArguments", extraArguments);
    }

    public static class Editor extends ZigConfigEditor<ZigExecConfigBuild> {
        private final JBTextField extraArgs = new JBTextField();

        @Override
        protected void applyEditorTo(@NotNull ZigExecConfigBuild s) throws ConfigurationException {
            super.applyEditorTo(s);
            s.extraArguments = extraArgs.getText();
        }

        @Override
        protected void resetEditorFrom(@NotNull ZigExecConfigBuild s) {
            super.resetEditorFrom(s);
            extraArgs.setText(Objects.requireNonNullElse(s.extraArguments, ""));
        }

        @Override
        protected void constructPanel(Panel p) {
            super.constructPanel(p);
            p.row("Extra arguments", (r) -> {
                r.cell(extraArgs).resizableColumn().align(AlignX.FILL).align(AlignY.FILL);
                return null;
            });
        }
    }
}
