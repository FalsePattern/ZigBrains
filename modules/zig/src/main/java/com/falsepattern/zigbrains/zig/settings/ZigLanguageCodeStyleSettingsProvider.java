package com.falsepattern.zigbrains.zig.settings;

import com.falsepattern.zigbrains.zig.ZigLanguage;
import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final public class ZigLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {

    @NotNull
    @Override
    public Language getLanguage() {
        return ZigLanguage.INSTANCE;
    }

    @Override
    public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
        if (settingsType == SettingsType.INDENT_SETTINGS) {
            consumer.showStandardOptions("INDENT_SIZE",
                                         "CONTINUATION_INDENT_SIZE",
                                         "TAB_SIZE",
                                         "USE_TAB_CHARACTER",
                                         "SMART_TABS",
                                         "LABEL_INDENT_SIZE",
                                         "LABEL_INDENT_ABSOLUTE",
                                         "USE_RELATIVE_INDENTS");
        }
    }

    @Nullable
    @Override
    public IndentOptionsEditor getIndentOptionsEditor() {
        return new IndentOptionsEditor();
    }

    @Override
    public String getCodeSample(@NotNull SettingsType settingsType) {
        return """
                const ray = @cImport({
                    @cInclude("raylib.h");
                });
                                
                pub fn main() void {
                    const screenWidth = 800;
                    const screenHeight = 450;
                                
                    ray.InitWindow(screenWidth, screenHeight, "raylib [core] example - basic window");
                    defer ray.CloseWindow();
                                
                    ray.SetTargetFPS(60);
                                
                    while (!ray.WindowShouldClose()) {
                        ray.BeginDrawing();
                        defer ray.EndDrawing();
                                
                        ray.ClearBackground(ray.RAYWHITE);
                        ray.DrawText("Hello, World!", 190, 200, 20, ray.LIGHTGRAY);
                    }
                }
                """;
    }
}
