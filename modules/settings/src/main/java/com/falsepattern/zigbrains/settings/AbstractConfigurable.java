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

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.function.Supplier;

public abstract class AbstractConfigurable<T> implements Configurable {
    private final String displayName;
    private final Supplier<ConfigurableGui<T>> guiSupplier;
    protected ConfigurableGui<T> configurableGui;

    public AbstractConfigurable(String displayName, Class<T> holderClass)
            throws IllegalAccessException {
        this.displayName = displayName;
        guiSupplier = ConfigurableGui.create(holderClass);
    }

    protected abstract T getHolder();

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return displayName;
    }

    @Override
    public @Nullable JComponent createComponent() {
        configurableGui = guiSupplier.get();
        return configurableGui.getPanel();
    }

    @Override
    public boolean isModified() {
        try {
            return configurableGui.modified(getHolder());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apply() throws ConfigurationException {
        try {
            configurableGui.guiToConfig(getHolder());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reset() {
        try {
            configurableGui.configToGui(getHolder());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void disposeUIResources() {
        configurableGui = null;
    }
}
