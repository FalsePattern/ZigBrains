package com.falsepattern.zigbrains;


import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.PropertyKey;

public class ZigBundle extends DynamicBundle {
    public static final String BUNDLE = "zigbrains.Bundle";
    public static final ZigBundle INSTANCE = new ZigBundle();
    private ZigBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
