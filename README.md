# ZigBrains

### [Website](https://falsepattern.com/zigbrains)

### [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/22456-zigbrains)

## Developer guide

### All platforms

After importing the gradle project, you need to run the `build setup -> generateSources` tasks.

### NixOS

In addition to the generated sources, you also need to run the `build setup -> nixos_jbr` task, otherwise java will
complain about missing files

## Special Thanks

- The [ZigTools](https://github.com/zigtools/) team for developing the Zig Language Server.
- [HTGAzureX1212](https://github.com/HTGAzureX1212) for developing [intellij-zig](https://github.com/intellij-zig/intellij-zig),
which served as a fantastic reference for deep IDE integration features

- The members of the `Zig Programming Language` discord server's `#tooling-dev` channel for providing encouragement,
feedback, and lots of bug reports. 

- The Ballerina Platform developers for `lsp4intellij`, the language server connector between the IntelliJ platform
and the Eclipse LSP4J project

- All the people who have generously funded the project
  - gree7
  - xceno
  - AnErrupTion

- Every contributor who helped with bugfixes and extra features
  - [gatesn](https://github.com/gatesn)
  - [MarioAriasC](https://github.com/MarioAriasC)
  - [JensvandeWiel](https://github.com/JensvandeWiel)

- And everyone who actively reported issues and helped ironing out all the remaining problems

## Versioning scheme
To reduce confusion and to better utilize semver, the plugin uses the following versioning scheme:

X - Major version, incremented any time a relatively large features is added or removed
Y - Minor version, incremented for smaller features or large refactors that don't change user-perceived behaviour
Z - Patch version, incremented only when a fix is purely an internal change and doesn't exceed an arbitrary threshold
of complexity (determined at the discretion of FalsePattern)

Note: before version 11, the version scheme used was 0.X.Y, without separate patch versions.
As this plugin will constantly be evolving together with the zig language, it makes not sense to keep the 0 prefix,
and might as well utilize the full semver string for extra information.

# Description

<!-- Plugin description -->
Adds support for the Zig Language, utilizing the ZLS language server for advanced coding assistance.

## Quick setup guide for Zig and ZLS

1. Download the latest version of Zig from https://ziglang.org/download
2. Download and compile the ZLS language server, available at https://github.com/zigtools/zls
3. Go to `Settings` -> `Languages & Frameworks` -> `Zig`, and point the `Toolchain Location` and `ZLS path` to the correct places
4. Open a .zig file, and wait for the circle in the bottom status bar to turn Green (empty).
   See below (`LSP status icon explanation`) for an explanation on what the circle means.

### LSP status icon explanation
Red (X symbol):
LSP server is stopped. You either don't have a proper ZLS path set, or you don't have a .zig file open.

Yellow ("refresh arrow" symbol):
LSP server is starting, please be patient.

Green (empty):
LSP server is running.

## Debugging

### Note
Debugging on Linux/MacOS/Unix is only available in CLion, as ZigBrains depends on the C++ toolchains system.

On Windows, debugging is also available with the help of the
[Native Debugging Support](https://plugins.jetbrains.com/plugin/12775-native-debugging-support), which is unfortunately
only compatible with paid IDEs.

### Windows

Due to technical limitations, the C++ toolchains cannot be used for debugging zig code on windows.

Go to `Settings | Build, Execution, Deployment | Debugger | Zig (Windows)` and follow the steps shown there to set up a
zig-compatible debugger.

### Linux / MacOS / Unix

ZigBrains uses the CLion C++ toolchains `Settings | Build, Execution, Deployment | Toolchains` for debugging purposes,
and it is fully compatible with both GDB and LLDB debuggers.

Additionally, ZigBrains will prioritize a toolchain if it is called `Zig`, otherwise it will use the default toolchain.

If no toolchain is available, ZigBrains will attempt to use the bundled LLDB debugger, and if that is not available either,
an error popup will be shown when you try to run with debugging.

Note: There is a small issue with the LLDB debugger which does not happen with GDB: The debugger will pause on the first
instruction (usually, deep inside the zig standard library's startup code). Unfortunately, we have not found a fix for
this yet, but fortunately it doesn't break anything, just a bit of inconvenience.

<!-- Plugin description end -->
