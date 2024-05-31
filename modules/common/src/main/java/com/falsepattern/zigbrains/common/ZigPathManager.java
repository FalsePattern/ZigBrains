package com.falsepattern.zigbrains.common;

import com.intellij.openapi.application.PathManager;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ZigPathManager {
    public static Path pluginDirInSystem() {
        return PathManager.getSystemDir().resolve("zigbrains");
    }

    public static Path tempPluginDirInSystem() {
        return Paths.get(PathManager.getTempPath()).resolve("zigbrains");
    }
}
