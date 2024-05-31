package com.falsepattern.zigbrains.debugger.settings;

import com.falsepattern.zigbrains.ZigBundle;
import com.falsepattern.zigbrains.debugger.toolchain.DebuggerKind;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SimpleConfigurable;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.xdebugger.settings.DebuggerSettingsCategory;
import com.intellij.xdebugger.settings.XDebuggerSettings;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ZigDebuggerSettings extends XDebuggerSettings<ZigDebuggerSettings> {
    public static final String GENERAL_SETTINGS_ID = "Debugger.Zig.General";

    public DebuggerKind debuggerKind = DebuggerKind.defaultKind();

    public boolean downloadAutomatically = false;
    public boolean useClion = true;

    protected ZigDebuggerSettings() {
        super("Zig");
    }

    @Override
    public @Nullable ZigDebuggerSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ZigDebuggerSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Override
    public @NotNull Collection<? extends Configurable> createConfigurables(@NotNull DebuggerSettingsCategory category) {
        val configurable = switch (category) {
            case GENERAL -> createGeneralSettingsConfigurable();
            default -> null;
        };
        return configurable == null ? Collections.emptyList() : List.of(configurable);
    }

    private Configurable createGeneralSettingsConfigurable() {
        return SimpleConfigurable.create(
                GENERAL_SETTINGS_ID,
                ZigBundle.message("settings.debugger.title"),
                ZigDebuggerGeneralSettingsConfigurableUi.class,
                ZigDebuggerSettings::getInstance
                                        );
    }

    public static ZigDebuggerSettings getInstance() {
        return XDebuggerSettings.getInstance(ZigDebuggerSettings.class);
    }
}
