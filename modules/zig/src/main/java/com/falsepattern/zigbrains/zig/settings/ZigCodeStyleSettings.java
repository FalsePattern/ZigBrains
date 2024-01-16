package com.falsepattern.zigbrains.zig.settings;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

public class ZigCodeStyleSettings extends CustomCodeStyleSettings {
    public ZigCodeStyleSettings(CodeStyleSettings settings) {
        super("ZigCodeStyleSettings", settings);
    }
}
