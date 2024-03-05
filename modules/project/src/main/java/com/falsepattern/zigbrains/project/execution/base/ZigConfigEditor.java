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

package com.falsepattern.zigbrains.project.execution.base;

import com.falsepattern.zigbrains.project.ui.WorkingDirectoryComponent;
import com.falsepattern.zigbrains.project.ui.ZigFilePathPanel;
import com.falsepattern.zigbrains.project.util.CLIUtil;
import com.falsepattern.zigbrains.project.util.ElementUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.ui.dsl.builder.AlignY;
import com.intellij.ui.dsl.builder.Panel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.io.Serializable;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.intellij.ui.dsl.builder.BuilderKt.panel;

public class ZigConfigEditor<T extends ZigExecConfigBase<T>> extends SettingsEditor<T> {
    private final ZigExecConfigBase<T> state;
    private final List<ZigConfigurable.ZigConfigModule<?>> configModules = new ArrayList<>();

    public ZigConfigEditor(ZigExecConfigBase<T> state) {
        this.state = state;
    }

    @Override
    protected void applyEditorTo(@NotNull T s) throws ConfigurationException {
        try {
            outer:
            for (val cfg : s.getConfigurables()) {
                for (val module : configModules) {
                    if (module.tryApply(cfg))
                        continue outer;
                }
                System.err.println("EEE");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    protected void resetEditorFrom(@NotNull T s) {
        outer:
        for (val cfg: s.getConfigurables()) {
            for (val module: configModules) {
                if (module.tryReset(cfg))
                    continue outer;
            }
        }
    }

    @Override
    protected final @NotNull JComponent createEditor() {
        configModules.clear();
        configModules.addAll(state.getConfigurables().stream().map(ZigConfigurable::createEditor).toList());
        return panel((p) -> {
            for (val module: configModules) {
                module.construct(p);
            }
            return null;
        });
    }

    @Override
    protected void disposeEditor() {
        for (val module: configModules) {
            module.dispose();
        }
        configModules.clear();
    }

    public interface ZigConfigurable<T extends ZigConfigurable<T>> extends Serializable, Cloneable {
        void readExternal(@NotNull Element element);
        void writeExternal(@NotNull Element element);
        ZigConfigModule<T> createEditor();
        T clone();

        interface ZigConfigModule<T extends ZigConfigurable<T>> extends Disposable {
            @Nullable T tryMatch(ZigConfigurable<?> cfg);
            void apply(T configurable) throws ConfigurationException;
            void reset(T configurable);
            default boolean tryApply(ZigConfigurable<?> cfg) throws ConfigurationException {
                val x = tryMatch(cfg);
                if (x != null) {
                    apply(x);
                    return true;
                }
                return false;
            }

            default boolean tryReset(ZigConfigurable<?> cfg) {
                val x = tryMatch(cfg);
                if (x != null) {
                    reset(x);
                    return true;
                }
                return false;
            }
            void construct(Panel p);
        }
    }

    public static abstract class PathConfigurable<T extends PathConfigurable<T>> implements ZigConfigurable<T> {
        private @Nullable Path path = null;

        public Optional<Path> getPath() {
            return Optional.ofNullable(path);
        }

        public @NotNull Path getPathOrThrow() {
            return getPath().orElseThrow(() -> new IllegalArgumentException("Empty file path!"));
        }

        public void setPath(@Nullable Path path) {
            this.path = path;
        }

        @Override
        public void readExternal(@NotNull Element element) {
            try {
                ElementUtil.readString(element, getSerializedName()).map(Paths::get).ifPresent(x -> path = x);
            } catch (InvalidPathException ignored){}
        }

        @Override
        public void writeExternal(@NotNull Element element) {
            ElementUtil.writeString(element, getSerializedName(), path == null ? null : path.toString());
        }

        @Override
        @SneakyThrows
        public T clone() {
            return (T) super.clone();
        }

        protected abstract String getSerializedName();

        public abstract static class PathConfigModule<T extends PathConfigurable<T>> implements ZigConfigModule<T> {
            @Override
            public void apply(T s) throws ConfigurationException {
                try {
                    s.setPath(Paths.get(getString()));
                } catch (InvalidPathException e) {
                    throw new ConfigurationException(e.getMessage(), e, "Invalid Path");
                }
            }

            @Override
            public void reset(T s) {
                setString(s.getPath().map(Path::toString).orElse(""));
            }

            protected abstract String getString();
            protected abstract void setString(String str);
        }
    }

    @Getter(AccessLevel.PROTECTED)
    @RequiredArgsConstructor
    public static class WorkDirectoryConfigurable extends PathConfigurable<WorkDirectoryConfigurable> {
        private transient final String serializedName;

        @Override
        public WorkDirectoryConfigModule createEditor() {
            return new WorkDirectoryConfigModule(serializedName);
        }

        @RequiredArgsConstructor
        public static class WorkDirectoryConfigModule extends PathConfigModule<WorkDirectoryConfigurable> {
            private final String serializedName;
            @Override
            public @Nullable WorkDirectoryConfigurable tryMatch(ZigConfigurable<?> cfg) {
                return cfg instanceof WorkDirectoryConfigurable cfg$ && cfg$.serializedName.equals(serializedName) ? cfg$ : null;
            }
            @Override
            protected String getString() {
                return workingDirectoryComponent.getComponent().getText();
            }

            @Override
            protected void setString(String str) {
                workingDirectoryComponent.getComponent().setText(str);
            }

            private final WorkingDirectoryComponent workingDirectoryComponent = new WorkingDirectoryComponent(this);

            @Override
            public void construct(Panel p) {
                p.row(workingDirectoryComponent.getLabel(), (r) -> {
                    r.cell(workingDirectoryComponent).resizableColumn().align(AlignX.FILL).align(AlignY.FILL);
                    return null;
                });
            }

            @Override
            public void dispose() {
                workingDirectoryComponent.dispose();
            }
        }
    }

    @RequiredArgsConstructor
    public static class FilePathConfigurable extends PathConfigurable<FilePathConfigurable> {
        @Getter(AccessLevel.PROTECTED)
        private transient final String serializedName;
        private transient final String guiLabel;

        @Override
        public FilePathConfigModule createEditor() {
            return new FilePathConfigModule(serializedName, guiLabel);
        }

        @RequiredArgsConstructor
        public static class FilePathConfigModule extends PathConfigModule<FilePathConfigurable> {
            private final String serializedName;
            private final String label;
            private final ZigFilePathPanel filePathPanel = new ZigFilePathPanel();

            @Override
            public @Nullable FilePathConfigurable tryMatch(ZigConfigurable<?> cfg) {
                return cfg instanceof FilePathConfigurable cfg$ && cfg$.serializedName.equals(serializedName) ? cfg$ : null;
            }

            @Override
            protected String getString() {
                return filePathPanel.getText();
            }

            @Override
            protected void setString(String str) {
                filePathPanel.setText(str);
            }

            @Override
            public void construct(Panel p) {
                p.row(label, (r) -> {
                    r.cell(filePathPanel).resizableColumn().align(AlignX.FILL).align(AlignY.FILL);
                    return null;
                });
            }

            @Override
            public void dispose() {
            }
        }
    }

    @RequiredArgsConstructor
    public static class ColoredConfigurable implements ZigConfigurable<ColoredConfigurable> {
        private transient final String serializedName;
        public boolean colored = true;

        @Override
        public void readExternal(@NotNull Element element) {
            ElementUtil.readBoolean(element, serializedName).ifPresent(x -> colored = x);
        }

        @Override
        public void writeExternal(@NotNull Element element) {
            ElementUtil.writeBoolean(element, serializedName, colored);
        }

        @Override
        public ColoredConfigModule createEditor() {
            return new ColoredConfigModule(serializedName);
        }

        @Override
        @SneakyThrows
        public ColoredConfigurable clone() {
            return (ColoredConfigurable) super.clone();
        }


        @RequiredArgsConstructor
        public static class ColoredConfigModule implements ZigConfigModule<ColoredConfigurable> {
            private final String serializedName;
            private final JBCheckBox checkBox = new JBCheckBox();

            @Override
            public @Nullable ColoredConfigurable tryMatch(ZigConfigurable<?> cfg) {
                return cfg instanceof ColoredConfigurable cfg$ && cfg$.serializedName.equals(serializedName) ? cfg$ : null;
            }

            @Override
            public void apply(ColoredConfigurable s) throws ConfigurationException {
                s.colored = checkBox.isSelected();
            }

            @Override
            public void reset(ColoredConfigurable s) {
                checkBox.setSelected(s.colored);
            }

            @Override
            public void construct(Panel p) {
                p.row("Colored terminal", (r) -> {
                    r.cell(checkBox);
                    return null;
                });
            }

            @Override
            public void dispose() {

            }
        }
    }

    @RequiredArgsConstructor
    public static class OptimizationConfigurable implements ZigConfigurable<OptimizationConfigurable> {
        private transient final String serializedName;
        public OptimizationLevel level = OptimizationLevel.Debug;
        public boolean forced = false;

        @Override
        public void readExternal(@NotNull Element element) {
            ElementUtil.readChild(element, serializedName).ifPresent(child -> {
                ElementUtil.readEnum(child, "level", OptimizationLevel.class).ifPresent(x -> level = x);
                ElementUtil.readBoolean(child,"forced").ifPresent(x -> forced = x);
            });
        }

        @Override
        public void writeExternal(@NotNull Element element) {
            val child = ElementUtil.writeChild(element, serializedName);
            ElementUtil.writeEnum(child, "level", level);
            ElementUtil.writeBoolean(child, "forced", forced);
        }

        @Override
        public OptimizationConfigModule createEditor() {
            return new OptimizationConfigModule(serializedName);
        }

        @Override
        @SneakyThrows
        public OptimizationConfigurable clone() {
            return (OptimizationConfigurable) super.clone();
        }

        @RequiredArgsConstructor
        public static class OptimizationConfigModule implements ZigConfigModule<OptimizationConfigurable> {
            private final String serializedName;
            private final ComboBox<OptimizationLevel> levels = new ComboBox<>(OptimizationLevel.values());
            private final JBCheckBox forced = new JBCheckBox("Force even in debug runs");

            @Override
            public @Nullable OptimizationConfigurable tryMatch(ZigConfigurable<?> cfg) {
                return cfg instanceof OptimizationConfigurable cfg$ && cfg$.serializedName.equals(serializedName) ? cfg$ : null;
            }

            @Override
            public void apply(OptimizationConfigurable s) throws ConfigurationException {
                s.level = levels.getItem();
                s.forced = forced.isSelected();
            }

            @Override
            public void reset(OptimizationConfigurable s) {
                levels.setItem(s.level);
                forced.setSelected(s.forced);
            }

            @Override
            public void construct(Panel p) {
                p.row("Optimization level", (r) -> {
                    r.cell(levels);
                    r.cell(forced);
                    return null;
                });
            }

            @Override
            public void dispose() {

            }
        }
    }

    @RequiredArgsConstructor
    public static class ArgsConfigurable implements ZigConfigurable<ArgsConfigurable> {
        private transient final String serializedName;
        private transient final String guiName;
        public String[] args = new String[0];

        @Override
        public void readExternal(@NotNull Element element) {
            ElementUtil.readStrings(element, serializedName).ifPresent(x -> args = x);
        }

        @Override
        public void writeExternal(@NotNull Element element) {
            ElementUtil.writeStrings(element, serializedName, args);
        }

        @Override
        public ArgsConfigModule createEditor() {
            return new ArgsConfigModule(serializedName, guiName);
        }

        @Override
        @SneakyThrows
        public ArgsConfigurable clone() {
            return (ArgsConfigurable) super.clone();
        }

        @RequiredArgsConstructor
        public static class ArgsConfigModule implements ZigConfigModule<ArgsConfigurable> {
            private final String serializedName;
            private final String guiName;
            private final JBTextField argsField = new JBTextField();

            @Override
            public @Nullable ArgsConfigurable tryMatch(ZigConfigurable<?> cfg) {
                return cfg instanceof ArgsConfigurable cfg$ && cfg$.serializedName.equals(serializedName) ? cfg$ : null;
            }

            @Override
            public void apply(ArgsConfigurable s) throws ConfigurationException {
                s.args = CLIUtil.translateCommandline(argsField.getText());
            }

            @Override
            public void reset(ArgsConfigurable s) {
                argsField.setText(String.join(" ", s.args));
            }

            @Override
            public void construct(Panel p) {
                p.row(guiName, (r) -> {
                    r.cell(argsField).resizableColumn().align(AlignX.FILL).align(AlignY.FILL);
                    return null;
                });
            }

            @Override
            public void dispose() {

            }
        }
    }
}
