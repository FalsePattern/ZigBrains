package com.falsepattern.zigbrains.zig.settings;

import com.falsepattern.zigbrains.zig.ZigLanguage;
import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.psi.codeStyle.CodeStyleSettings;

public class ZigCodeStyleMainPanel extends TabbedLanguageCodeStylePanel {
    public ZigCodeStyleMainPanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
        super(ZigLanguage.INSTANCE, currentSettings, settings);
    }

}
