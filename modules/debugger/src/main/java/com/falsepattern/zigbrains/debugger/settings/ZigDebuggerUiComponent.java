package com.falsepattern.zigbrains.debugger.settings;

import com.falsepattern.zigbrains.common.util.dsl.JavaPanel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurableUi;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public abstract class ZigDebuggerUiComponent implements ConfigurableUi<ZigDebuggerSettings>, Disposable {
    public abstract void buildUi(JavaPanel panel);

    @Override
    public @NotNull JComponent getComponent() {
        return JavaPanel.newPanel(this::buildUi);
    }

    @Override
    public void dispose() {

    }
}
