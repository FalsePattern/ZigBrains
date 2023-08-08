package com.falsepattern.zigbrains.zon;

import com.intellij.lang.Language;

public class ZonLanguage extends Language {
    public static final ZonLanguage INSTANCE = new ZonLanguage();
    private ZonLanguage() {
        super("Zon");
    }
}
