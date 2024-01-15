package com.falsepattern.zigbrains.zig.settings;

import com.falsepattern.zigbrains.zig.ZigLanguage;
import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleConfigurable;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZigCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
    @Override
    public ZigCodeStyleSettings createCustomSettings(@NotNull CodeStyleSettings settings) {
        return new ZigCodeStyleSettings(settings);
    }

    @Nullable
    @Override
    public String getConfigurableDisplayName() {
        return "Zig";
    }

    @NotNull
    public CodeStyleConfigurable createConfigurable(@NotNull CodeStyleSettings settings, @NotNull CodeStyleSettings modelSettings) {
        return new CodeStyleAbstractConfigurable(settings, modelSettings, this.getConfigurableDisplayName()) {
            @Override
            protected @NotNull CodeStyleAbstractPanel createPanel(@NotNull CodeStyleSettings codeStyleSettings) {
                return new ZigCodeStyleMainPanel(getCurrentSettings(), codeStyleSettings);
            }
        };
    }

    @Override
    public @Nullable Language getLanguage() {
        return ZigLanguage.INSTANCE;
    }
}
