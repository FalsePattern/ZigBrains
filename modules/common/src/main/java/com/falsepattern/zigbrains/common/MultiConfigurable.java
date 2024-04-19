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

package com.falsepattern.zigbrains.common;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.JBColor;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.Arrays;

import static com.falsepattern.zigbrains.common.util.dsl.JavaPanel.newPanel;

public abstract class MultiConfigurable implements Configurable {
    private final SubConfigurable[] configurables;
    protected MultiConfigurable(SubConfigurable... configurables) {
        this.configurables = Arrays.copyOf(configurables, configurables.length);
    }

    @Override
    public @Nullable JComponent createComponent() {
        return newPanel(p -> {
            for (val configurable: configurables) {
                configurable.createComponent(p);
            }
        });
    }

    @Override
    public boolean isModified() {
        for (val configurable: configurables) {
            if (configurable.isModified())
                return true;
        }
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        for (val config: configurables) {
            config.apply();
        }
    }

    @Override
    public void reset() {
        for (val config: configurables) {
            config.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        for (val config: configurables) {
            config.disposeUIResources();
        }
    }
}
