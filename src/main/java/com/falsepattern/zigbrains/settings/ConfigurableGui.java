/*
 * Copyright 2023 FalsePattern
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

package com.falsepattern.zigbrains.settings;

import com.falsepattern.zigbrains.settings.annotations.Category;
import com.falsepattern.zigbrains.settings.annotations.Config;
import com.falsepattern.zigbrains.settings.annotations.FilePath;
import com.falsepattern.zigbrains.settings.annotations.Label;
import com.falsepattern.zigbrains.settings.annotations.Separator;
import com.falsepattern.zigbrains.settings.annotations.VerticalGap;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.TextAccessor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConfigurableGui<T> {
    private final JPanel thePanel;

    private final Map<String, ConfigurableAccessor<?>> guiProps;
    private final Map<String, VarHandle> configProps;
    private final Set<String> props;
    private ConfigurableGui(JPanel thePanel,
                            Map<String, ConfigurableAccessor<?>> guiProps,
                            Map<String, VarHandle> configProps,
                            Set<String> props) {
        this.thePanel = thePanel;
        this.guiProps = guiProps;
        this.configProps = configProps;
        this.props = props;
    }

    public JPanel getPanel() {
        return thePanel;
    }

    public boolean modified(T holder) {
        return modified(holder, false, null);
    }

    public boolean modified(T holder, boolean exclude, Set<String> mask) {
        for (var prop: props) {
            if (mask != null && mask.contains(prop) == exclude) {
                continue;
            }
            if (!Objects.equals(guiProps.get(prop).get(), configProps.get(prop).get(holder))) {
                return true;
            }
        }
        return false;
    }

    public void guiToConfig(T holder) {
        for (var prop: props) {
            configProps.get(prop).set(holder, guiProps.get(prop).get());
        }
    }

    public void configToGui(T holder) {
        for (var prop: props) {
            guiProps.get(prop).set(configProps.get(prop).get(holder));
        }
    }

    public static <T> Supplier<ConfigurableGui<T>> create(MethodHandles.Lookup lookup, Class<T> dataContainer)
            throws IllegalAccessException {
        var fields = Arrays.stream(dataContainer.getFields())
                .filter(field -> field.isAnnotationPresent(Config.class))
                .filter(field -> field.isAnnotationPresent(Label.class))
                .sorted(Comparator.comparingInt(field -> field.getAnnotation(Config.class).value()))
                .toList();
        var configProps = new HashMap<String, VarHandle>();
        var props = new HashSet<String>();
        var steps = new ArrayList<BiConsumer<HashMap<String, ConfigurableAccessor<?>>, FormBuilder>>();
        for (var field: fields) {
            var propName = field.getName();
            var fieldType = field.getType();
            var label = field.getAnnotation(Label.class).value();
            if (field.isAnnotationPresent(VerticalGap.class)) {
                var gap = field.getAnnotation(VerticalGap.class).value();
                steps.add((gp, fb) -> fb.addVerticalGap(gap));
            }
            if (field.isAnnotationPresent(Separator.class)) {
                steps.add((gp, fb) -> fb.addSeparator());
            }
            if (field.isAnnotationPresent(Category.class)) {
                var cat = field.getAnnotation(Category.class).value();
                steps.add((gp, fb) -> fb.addSeparator()
                                  .addComponent(new JBLabel(cat))
                                  .addVerticalGap(10));
            }
            Function<HashMap<String, ConfigurableAccessor<?>>, JComponent> component;
            if (fieldType.equals(String.class)) {
                var isFilePath = field.isAnnotationPresent(FilePath.class);
                if (isFilePath) {
                    var filePathSpec = field.getAnnotation(FilePath.class);
                    var descriptor = new FileChooserDescriptor(filePathSpec.files(),
                                                               filePathSpec.folders(),
                                                               filePathSpec.jars(),
                                                               filePathSpec.jarsAsFiles(),
                                                               filePathSpec.jarContents(),
                                                               filePathSpec.multiple());
                    component = (guiProps) -> {
                        var theComponent = new TextFieldWithBrowseButton();
                        theComponent.addBrowseFolderListener(new TextBrowseFolderListener(descriptor));
                        guiProps.put(propName, new ConfigurableAccessor.TextFieldAccessor(theComponent));
                        return theComponent;
                    };
                } else {
                    component = (guiProps) -> {
                        var theComponent = new JBTextField();
                        guiProps.put(propName, new ConfigurableAccessor.TextFieldAccessor(theComponent));
                        return theComponent;
                    };
                }
            } else if (fieldType.equals(boolean.class)) {
                component = (guiProps) -> {
                    var theComponent = new JBCheckBox();
                    guiProps.put(propName, new ConfigurableAccessor.CheckBoxAccessor(theComponent));
                    return theComponent;
                };
            } else {
                component = null;
            }
            if (component != null) {
                props.add(propName);
                configProps.put(propName, lookup.unreflectVarHandle(field));
            }
            steps.add((gp, fb) -> {
                var comp = component == null ? null : component.apply(gp);
                if (comp != null) {
                    fb.addLabeledComponent(new JBLabel(label), comp, 1, false);
                } else {
                    fb.addComponent(new JBLabel(label));
                }
            });
        }
        steps.add((gp, fb) -> fb.addComponentFillVertically(new JPanel(), 0));

        return () -> {
            var gp = new HashMap<String, ConfigurableAccessor<?>>();
            var fb = FormBuilder.createFormBuilder();
            for (var step: steps) {
                step.accept(gp, fb);
            }
            for (var prop: props) {

            }
            return new ConfigurableGui<>(fb.getPanel(), gp, configProps, props);
        };
    }
}
