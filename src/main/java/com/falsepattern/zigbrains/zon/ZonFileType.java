package com.falsepattern.zigbrains.zon;

import com.falsepattern.zigbrains.common.Icons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class ZonFileType extends LanguageFileType {
    @SuppressWarnings("unused") //Used by plugin.xml
    public static final ZonFileType INSTANCE = new ZonFileType();

    private ZonFileType() {
        super(ZonLanguage.INSTANCE);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "ZON File";
    }

    @Override
    public @NotNull String getDescription() {
        return "Zig object notation file";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "zon";
    }

    @Override
    public Icon getIcon() {
        return Icons.ZON;
    }
}
