package com.falsepattern.zigbrains.zig.util;

import com.intellij.openapi.options.Configurable;
import lombok.val;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

//JetBrains Internal API, but we need to access it, so access it reflectively (hopefully safe enough to pass verifier)
public class JBInternalPluginManagerConfigurable {
    private static final Constructor<?> constructor;
    private static final Method openMarketplaceTab;
    public static final boolean successful;

    public final Configurable instance;

    static {
        boolean success = false;
        Constructor<?> constructor1 = null;
        Method openMarketplaceTab1 = null;
        try {
            val theClass = Class.forName("com_intellij_ide_plugins_PluginManagerConfigurable".replace('_', '.'));
            constructor1 = theClass.getDeclaredConstructor();
            constructor1.setAccessible(true);
            openMarketplaceTab1 = theClass.getDeclaredMethod("openMarketplaceTab", String.class);
            openMarketplaceTab1.setAccessible(true);
            success = true;
        } catch (Throwable ignored) {
        }
        successful = success;
        constructor = constructor1;
        openMarketplaceTab = openMarketplaceTab1;
    }

    public JBInternalPluginManagerConfigurable() {
        try {
            instance = (Configurable) constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public void openMarketplaceTab(String option) {
        try {
            openMarketplaceTab.invoke(instance, option);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
