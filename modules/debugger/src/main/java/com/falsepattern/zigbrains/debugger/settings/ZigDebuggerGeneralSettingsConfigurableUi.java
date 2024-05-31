package com.falsepattern.zigbrains.debugger.settings;

import com.falsepattern.zigbrains.common.util.dsl.JavaPanel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurableUi;
import com.intellij.openapi.options.ConfigurationException;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;

public class ZigDebuggerGeneralSettingsConfigurableUi implements ConfigurableUi<ZigDebuggerSettings>, Disposable {
    private final List<ZigDebuggerUiComponent> components;

    public ZigDebuggerGeneralSettingsConfigurableUi() {
        components = new ArrayList<>();
        components.add(new ZigDebuggerToolchainConfigurableUi());
    }

    @Override
    public boolean isModified(@NotNull ZigDebuggerSettings settings) {
        for (val it: components) {
            if (it.isModified(settings)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void reset(@NotNull ZigDebuggerSettings settings) {
        for (val it: components) {
            it.reset(settings);
        }
    }

    @Override
    public void apply(@NotNull ZigDebuggerSettings settings) throws ConfigurationException {
        for (val it: components) {
            it.apply(settings);
        }
    }

    @Override
    public @NotNull JComponent getComponent() {
        return JavaPanel.newPanel(p -> {
            for (val it: components) {
                it.buildUi(p);
            }
        });
    }

    @Override
    public void dispose() {
        components.forEach(Disposable::dispose);
    }
}
