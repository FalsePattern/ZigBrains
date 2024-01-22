/*
 * Copyright 2023 FalsePattern
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.falsepattern.zigbrains.project.platform;

import com.falsepattern.zigbrains.project.ide.newproject.ZigProjectConfigurationData;
import com.falsepattern.zigbrains.project.ide.util.projectwizard.ZigProjectSettingsStep;
import com.falsepattern.zigbrains.project.openapi.components.ZigProjectSettingsService;
import com.falsepattern.zigbrains.zig.Icons;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.platform.ProjectGeneratorPeer;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.io.IOException;

public class ZigDirectoryProjectGenerator implements DirectoryProjectGenerator<ZigProjectConfigurationData>,
        CustomStepProjectGenerator<ZigProjectConfigurationData> {

    @Override
    public @NotNull @NlsContexts.Label String getName() {
        return "Zig";
    }

    @Override
    public @Nullable Icon getLogo() {
        return Icons.ZIG;
    }

    @Override
    public @NotNull ProjectGeneratorPeer<ZigProjectConfigurationData> createPeer() {
        return new ZigProjectGeneratorPeer();
    }

    @Override
    public @NotNull ValidationResult validate(@NotNull String baseDirPath) {
        return ValidationResult.OK;
    }

    @Override
    public void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir, @NotNull ZigProjectConfigurationData data, @NotNull Module module) {
        try {
            val settings = data.settings();

            var svc = ZigProjectSettingsService.getInstance(project);
            svc.getState().setToolchain(settings.toolchain());

            val template = data.selectedTemplate();

            // Create /src directory
            VirtualFile srcDir = baseDir.createChildDirectory(this, "src");

            if (template.name == "Executable (application)") {
                // Create main.zig inside /src directory
                VirtualFile mainFile = srcDir.createChildData(this, "main.zig");
                VfsUtil.saveText(mainFile, "const std = @import(\"std\");\n" +
                                           "\n" +
                                           "pub fn main() !void {\n" +
                                           "    // Prints to stderr (it's a shortcut based on `std.io.getStdErr()`)\n" +
                                           "    std.debug.print(\"All your {s} are belong to us.\\n\", .{\"codebase\"});\n" +
                                           "\n" +
                                           "    // stdout is for the actual output of your application, for example if you\n" +
                                           "    // are implementing gzip, then only the compressed bytes should be sent to\n" +
                                           "    // stdout, not any debugging messages.\n" +
                                           "    const stdout_file = std.io.getStdOut().writer();\n" +
                                           "    var bw = std.io.bufferedWriter(stdout_file);\n" +
                                           "    const stdout = bw.writer();\n" +
                                           "\n" +
                                           "    try stdout.print(\"Run `zig build test` to run the tests.\\n\", .{});\n" +
                                           "\n" +
                                           "    try bw.flush(); // don't forget to flush!\n" +
                                           "}\n" +
                                           "\n" +
                                           "test \"simple test\" {\n" +
                                           "    var list = std.ArrayList(i32).init(std.testing.allocator);\n" +
                                           "    defer list.deinit(); // try commenting this out and see if zig detects the memory leak!\n" +
                                           "    try list.append(42);\n" +
                                           "    try std.testing.expectEqual(@as(i32, 42), list.pop());\n" +
                                           "}");

                // Create build.zig in the project root directory
                VirtualFile buildFile = baseDir.createChildData(this, "build.zig");
                // Add your build script content here
                VfsUtil.saveText(buildFile, "const std = @import(\"std\");\n" +
                                                                    "\n" +
                                                                    "// Although this function looks imperative, note that its job is to\n" +
                                                                    "// declaratively construct a build graph that will be executed by an external\n" +
                                                                    "// runner.\n" +
                                                                    "pub fn build(b: *std.Build) void {\n" +
                                                                    "    // Standard target options allows the person running `zig build` to choose\n" +
                                                                    "    // what target to build for. Here we do not override the defaults, which\n" +
                                                                    "    // means any target is allowed, and the default is native. Other options\n" +
                                                                    "    // for restricting supported target set are available.\n" +
                                                                    "    const target = b.standardTargetOptions(.{});\n" +
                                                                    "\n" +
                                                                    "    // Standard optimization options allow the person running `zig build` to select\n" +
                                                                    "    // between Debug, ReleaseSafe, ReleaseFast, and ReleaseSmall. Here we do not\n" +
                                                                    "    // set a preferred release mode, allowing the user to decide how to optimize.\n" +
                                                                    "    const optimize = b.standardOptimizeOption(.{});\n" +
                                                                    "\n" +
                                                                    "    const exe = b.addExecutable(.{\n" +
                                                                    "        .name = \"untitled\",\n" +
                                                                    "        .root_source_file = .{ .path = \"src/main.zig\" },\n" +
                                                                    "        .target = target,\n" +
                                                                    "        .optimize = optimize,\n" +
                                                                    "    });\n" +
                                                                    "\n" +
                                                                    "    // This declares intent for the executable to be installed into the\n" +
                                                                    "    // standard location when the user invokes the \"install\" step (the default\n" +
                                                                    "    // step when running `zig build`).\n" +
                                                                    "    b.installArtifact(exe);\n" +
                                                                    "\n" +
                                                                    "    // This *creates* a Run step in the build graph, to be executed when another\n" +
                                                                    "    // step is evaluated that depends on it. The next line below will establish\n" +
                                                                    "    // such a dependency.\n" +
                                                                    "    const run_cmd = b.addRunArtifact(exe);\n" +
                                                                    "\n" +
                                                                    "    // By making the run step depend on the install step, it will be run from the\n" +
                                                                    "    // installation directory rather than directly from within the cache directory.\n" +
                                                                    "    // This is not necessary, however, if the application depends on other installed\n" +
                                                                    "    // files, this ensures they will be present and in the expected location.\n" +
                                                                    "    run_cmd.step.dependOn(b.getInstallStep());\n" +
                                                                    "\n" +
                                                                    "    // This allows the user to pass arguments to the application in the build\n" +
                                                                    "    // command itself, like this: `zig build run -- arg1 arg2 etc`\n" +
                                                                    "    if (b.args) |args| {\n" +
                                                                    "        run_cmd.addArgs(args);\n" +
                                                                    "    }\n" +
                                                                    "\n" +
                                                                    "    // This creates a build step. It will be visible in the `zig build --help` menu,\n" +
                                                                    "    // and can be selected like this: `zig build run`\n" +
                                                                    "    // This will evaluate the `run` step rather than the default, which is \"install\".\n" +
                                                                    "    const run_step = b.step(\"run\", \"Run the app\");\n" +
                                                                    "    run_step.dependOn(&run_cmd.step);\n" +
                                                                    "\n" +
                                                                    "    const exe_unit_tests = b.addTest(.{\n" +
                                                                    "        .root_source_file = .{ .path = \"src/main.zig\" },\n" +
                                                                    "        .target = target,\n" +
                                                                    "        .optimize = optimize,\n" +
                                                                    "    });\n" +
                                                                    "\n" +
                                                                    "    const run_exe_unit_tests = b.addRunArtifact(exe_unit_tests);\n" +
                                                                    "\n" +
                                                                    "    // Similar to creating the run step earlier, this exposes a `test` step to\n" +
                                                                    "    // the `zig build --help` menu, providing a way for the user to request\n" +
                                                                    "    // running the unit tests.\n" +
                                                                    "    const test_step = b.step(\"test\", \"Run unit tests\");\n" +
                                                                    "    test_step.dependOn(&run_exe_unit_tests.step);\n" +
                                                                    "}"
);

                // Create build.zig in the project root directory
                VirtualFile zigZonFile = baseDir.createChildData(this, "build.zig.zon");
                // Add your build script content here
                VfsUtil.saveText(zigZonFile, ".{\n" +
                                             "    .name = \"untitled\",\n" +
                                             "    // This is a [Semantic Version](https://semver.org/).\n" +
                                             "    // In a future version of Zig it will be used for package deduplication.\n" +
                                             "    .version = \"0.0.0\",\n" +
                                             "\n" +
                                             "    // This field is optional.\n" +
                                             "    // This is currently advisory only; Zig does not yet do anything\n" +
                                             "    // with this value.\n" +
                                             "    //.minimum_zig_version = \"0.11.0\",\n" +
                                             "\n" +
                                             "    // This field is optional.\n" +
                                             "    // Each dependency must either provide a `url` and `hash`, or a `path`.\n" +
                                             "    // `zig build --fetch` can be used to fetch all dependencies of a package, recursively.\n" +
                                             "    // Once all dependencies are fetched, `zig build` no longer requires\n" +
                                             "    // internet connectivity.\n" +
                                             "    .dependencies = .{\n" +
                                             "        // See `zig fetch --save <url>` for a command-line interface for adding dependencies.\n" +
                                             "        //.example = .{\n" +
                                             "        //    // When updating this field to a new URL, be sure to delete the corresponding\n" +
                                             "        //    // `hash`, otherwise you are communicating that you expect to find the old hash at\n" +
                                             "        //    // the new URL.\n" +
                                             "        //    .url = \"https://example.com/foo.tar.gz\",\n" +
                                             "        //\n" +
                                             "        //    // This is computed from the file contents of the directory of files that is\n" +
                                             "        //    // obtained after fetching `url` and applying the inclusion rules given by\n" +
                                             "        //    // `paths`.\n" +
                                             "        //    //\n" +
                                             "        //    // This field is the source of truth; packages do not come from a `url`; they\n" +
                                             "        //    // come from a `hash`. `url` is just one of many possible mirrors for how to\n" +
                                             "        //    // obtain a package matching this `hash`.\n" +
                                             "        //    //\n" +
                                             "        //    // Uses the [multihash](https://multiformats.io/multihash/) format.\n" +
                                             "        //    .hash = \"...\",\n" +
                                             "        //\n" +
                                             "        //    // When this is provided, the package is found in a directory relative to the\n" +
                                             "        //    // build root. In this case the package's hash is irrelevant and therefore not\n" +
                                             "        //    // computed. This field and `url` are mutually exclusive.\n" +
                                             "        //    .path = \"foo\",\n" +
                                             "        //},\n" +
                                             "    },\n" +
                                             "\n" +
                                             "    // Specifies the set of files and directories that are included in this package.\n" +
                                             "    // Only files and directories listed here are included in the `hash` that\n" +
                                             "    // is computed for this package.\n" +
                                             "    // Paths are relative to the build root. Use the empty string (``) to refer to\n" +
                                             "    // the build root itself.\n" +
                                             "    // A directory listed here means that all files within, recursively, are included.\n" +
                                             "    .paths = .{\n" +
                                             "        // This makes *all* files, recursively, included in this package. It is generally\n" +
                                             "        // better to explicitly list the files and directories instead, to insure that\n" +
                                             "        // fetching from tarballs, file system paths, and version control all result\n" +
                                             "        // in the same contents hash.\n" +
                                             "        \"\",\n" +
                                             "        // For example...\n" +
                                             "        //\"build.zig\",\n" +
                                             "        //\"build.zig.zon\",\n" +
                                             "        //\"src\",\n" +
                                             "        //\"LICENSE\",\n" +
                                             "        //\"README.md\",\n" +
                                             "    },\n" +
                                             "}");
            } else if (template.name == "Library (static)") {
                // Create main.zig inside /src directory
                VirtualFile mainFile = srcDir.createChildData(this, "root.zig");
                VfsUtil.saveText(mainFile, "const std = @import(\"std\");\n" +
                                           "const testing = std.testing;\n" +
                                           "\n" +
                                           "export fn add(a: i32, b: i32) i32 {\n" +
                                           "    return a + b;\n" +
                                           "}\n" +
                                           "\n" +
                                           "test \"basic add functionality\" {\n" +
                                           "    try testing.expect(add(3, 7) == 10);\n" +
                                           "}");

                // Create build.zig in the project root directory
                VirtualFile buildFile = baseDir.createChildData(this, "build.zig");
                // Add your build script content here
                VfsUtil.saveText(buildFile, "const std = @import(\"std\");\n" +
                                            "\n" +
                                            "// Although this function looks imperative, note that its job is to\n" +
                                            "// declaratively construct a build graph that will be executed by an external\n" +
                                            "// runner.\n" +
                                            "pub fn build(b: *std.Build) void {\n" +
                                            "    // Standard target options allow the person running `zig build` to choose\n" +
                                            "    // what target to build for. Here we do not override the defaults, which\n" +
                                            "    // means any target is allowed, and the default is native. Other options\n" +
                                            "    // for restricting supported target set are available.\n" +
                                            "    const target = b.standardTargetOptions(.{});\n" +
                                            "\n" +
                                            "    // Standard optimization options allow the person running `zig build` to select\n" +
                                            "    // between Debug, ReleaseSafe, ReleaseFast, and ReleaseSmall. Here we do not\n" +
                                            "    // set a preferred release mode, allowing the user to decide how to optimize.\n" +
                                            "    const optimize = b.standardOptimizeOption(.{});\n" +
                                            "\n" +
                                            "    const lib = b.addStaticLibrary(.{\n" +
                                            "        .name = \"untitled\",\n" +
                                            "        // In this case, the main source file is merely a path, however, in more\n" +
                                            "        // complicated build scripts, this could be a generated file.\n" +
                                            "        .root_source_file = .{ .path = \"src/root.zig\" },\n" +
                                            "        .target = target,\n" +
                                            "        .optimize = optimize,\n" +
                                            "    });\n" +
                                            "\n" +
                                            "    // This declares intent for the library to be installed into the standard\n" +
                                            "    // location when the user invokes the \"install\" step (the default step when\n" +
                                            "    // running `zig build`).\n" +
                                            "    b.installArtifact(lib);\n" +
                                            "\n" +
                                            "    // Creates a step for unit testing. This only builds the test executable\n" +
                                            "    // but does not run it.\n" +
                                            "    const lib_unit_tests = b.addTest(.{\n" +
                                            "        .root_source_file = .{ .path = \"src/root.zig\" },\n" +
                                            "        .target = target,\n" +
                                            "        .optimize = optimize,\n" +
                                            "    });\n" +
                                            "\n" +
                                            "    const run_lib_unit_tests = b.addRunArtifact(lib_unit_tests);\n" +
                                            "\n" +
                                            "    // Similar to creating the run step earlier, this exposes a `test` step to\n" +
                                            "    // the `zig build --help` menu, providing a way for the user to request\n" +
                                            "    // running the unit tests.\n" +
                                            "    const test_step = b.step(\"test\", \"Run unit tests\");\n" +
                                            "    test_step.dependOn(&run_lib_unit_tests.step);\n" +
                                            "}\n");

                // Create build.zig in the project root directory
                VirtualFile zigZonFile = baseDir.createChildData(this, "zig.zon");
                // Add your build script content here
                VfsUtil.saveText(zigZonFile, ".{\n" +
                                             "    .name = \"untitled\",\n" +
                                             "    // This is a [Semantic Version](https://semver.org/).\n" +
                                             "    // In a future version of Zig it will be used for package deduplication.\n" +
                                             "    .version = \"0.0.0\",\n" +
                                             "\n" +
                                             "    // This field is optional.\n" +
                                             "    // This is currently advisory only; Zig does not yet do anything\n" +
                                             "    // with this value.\n" +
                                             "    //.minimum_zig_version = \"0.11.0\",\n" +
                                             "\n" +
                                             "    // This field is optional.\n" +
                                             "    // Each dependency must either provide a `url` and `hash`, or a `path`.\n" +
                                             "    // `zig build --fetch` can be used to fetch all dependencies of a package, recursively.\n" +
                                             "    // Once all dependencies are fetched, `zig build` no longer requires\n" +
                                             "    // internet connectivity.\n" +
                                             "    .dependencies = .{\n" +
                                             "        // See `zig fetch --save <url>` for a command-line interface for adding dependencies.\n" +
                                             "        //.example = .{\n" +
                                             "        //    // When updating this field to a new URL, be sure to delete the corresponding\n" +
                                             "        //    // `hash`, otherwise you are communicating that you expect to find the old hash at\n" +
                                             "        //    // the new URL.\n" +
                                             "        //    .url = \"https://example.com/foo.tar.gz\",\n" +
                                             "        //\n" +
                                             "        //    // This is computed from the file contents of the directory of files that is\n" +
                                             "        //    // obtained after fetching `url` and applying the inclusion rules given by\n" +
                                             "        //    // `paths`.\n" +
                                             "        //    //\n" +
                                             "        //    // This field is the source of truth; packages do not come from a `url`; they\n" +
                                             "        //    // come from a `hash`. `url` is just one of many possible mirrors for how to\n" +
                                             "        //    // obtain a package matching this `hash`.\n" +
                                             "        //    //\n" +
                                             "        //    // Uses the [multihash](https://multiformats.io/multihash/) format.\n" +
                                             "        //    .hash = \"...\",\n" +
                                             "        //\n" +
                                             "        //    // When this is provided, the package is found in a directory relative to the\n" +
                                             "        //    // build root. In this case the package's hash is irrelevant and therefore not\n" +
                                             "        //    // computed. This field and `url` are mutually exclusive.\n" +
                                             "        //    .path = \"foo\",\n" +
                                             "        //},\n" +
                                             "    },\n" +
                                             "\n" +
                                             "    // Specifies the set of files and directories that are included in this package.\n" +
                                             "    // Only files and directories listed here are included in the `hash` that\n" +
                                             "    // is computed for this package.\n" +
                                             "    // Paths are relative to the build root. Use the empty string (``) to refer to\n" +
                                             "    // the build root itself.\n" +
                                             "    // A directory listed here means that all files within, recursively, are included.\n" +
                                             "    .paths = .{\n" +
                                             "        // This makes *all* files, recursively, included in this package. It is generally\n" +
                                             "        // better to explicitly list the files and directories instead, to insure that\n" +
                                             "        // fetching from tarballs, file system paths, and version control all result\n" +
                                             "        // in the same contents hash.\n" +
                                             "        \"\",\n" +
                                             "        // For example...\n" +
                                             "        //\"build.zig\",\n" +
                                             "        //\"build.zig.zon\",\n" +
                                             "        //\"src\",\n" +
                                             "        //\"LICENSE\",\n" +
                                             "        //\"README.md\",\n" +
                                             "    },\n" +
                                             "}");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AbstractActionWithPanel createStep(DirectoryProjectGenerator<ZigProjectConfigurationData> projectGenerator, AbstractNewProjectStep.AbstractCallback<ZigProjectConfigurationData> callback) {
        return new ZigProjectSettingsStep(projectGenerator);
    }
}
