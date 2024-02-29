# ZigBrains

## Developer guide

### All platforms

After importing the gradle project, you need to run the `build setup -> generateSources` tasks.

### NixOS

In addition to the generated sources, you also need to run the `build setup -> nixos_jbr` task, otherwise java will
complain about missing files

## Special Thanks

- [HTGAzureX1212](https://github.com/HTGAzureX1212) for developing [intellij-zig](https://github.com/intellij-zig/intellij-zig),
which served as a fantastic reference for deep IDE integration features

- The members of the `Zig Programming Language` discord server's `#tooling-dev` channel for providing encouragement and
feedback

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
A multifunctional Zig Programming Language plugin for the IDEA platform.

Core features:
- Uses ZLS (Zig Language Server) for code assistance, syntax highlighting, and anything to do with coding assistance
- Supports build.zig.zon files with autocomplete
- Per-project Zig toolchain integration
- Debugging support for CLion (builtin), and IDEA Ultimate [With this plugin](https://plugins.jetbrains.com/plugin/12775-native-debugging-support)
- Gutter icon for running main(), tests, and build


## Setting up the language server

If you have `zls` available on PATH, ZigBrains will automatically discover it. If not, follow this guide:

1. Download or compile the ZLS language server, available at https://github.com/zigtools/zls
2. Go to `Settings` -> `Languages & Frameworks` -> `ZLS` -> `ZLS path` -> set the path to the `zls` executable you downloaded or compiled
3. Open a .zig file, and wait for the circle in the bottom status bar to turn Green (empty).
See below for an explanation on what the circle means.

### LSP status icon explanation
Red (X symbol):
LSP server is stopped. You either don't have a proper ZLS path set, or you don't have a .zig file open.

Yellow ("refresh arrow" symbol):
LSP server is starting, please be patient.

Green (empty):
LSP server is running.

## Debugging

ZigBrains uses the CLion C++ toolchains (Settings | Build, Execution, Deployment | Toolchains) for debugging purposes,
and it is fully compatible with both GDB and LLDB debuggers.

Additionally, ZigBrains will prioritize a toolchain if it is called `Zig`, otherwise it will use the default toolchain.

If no toolchain is available, ZigBrains will attempt to use the bundled LLDB debugger, and if that is not available either,
an error popup will be shown when you try to run with debugging.

Note: There is a small issue with the LLDB debugger which does not happen with GDB: The debugger will pause on the first
instruction (usually, deep inside the zig standard library's startup code). Unfortunately, we have not found a fix for
this yet, but fortunately it doesn't break anything, just a bit of inconvenience.

## Feature tracker:

### .zig files:
- Code completion
- Code folding
- Code formatting
- Syntax highlighting
- Inlay hints
- Basic error diagnostics
- Go to definition
- Rename symbol
- Hover documentation
- Go to implementations / find usages
- Brace/Parenthesis/Bracket matching
- Debugging (CLion/CLion Nova)
- File creation prompt
- Gutter launch buttons
- Commenter (thanks @MarioAriasC !)

- TODO:
  - Workspace Symbols

### .zon files:
- Syntax highlighting
- Formatting and indentation
- Code completion
- Brace folding
- Automatic brace and quote pairing

### Toolchain:
- Basic per-project toolchain management
- Run configurations
- Debugging (CLion/IDEA Ultimate)
- Project generation (thanks @JensvandeWiel !)

## Licenses

<p>

All code in this project, unless specified differently, is licensed under the `Apache 2.0` license.

</p>

<p>

The code inside the `lsp` package is derived from the LSP4IntelliJ project, with various modifications, fixes, and
additions to fix any outstanding issues i was having with the original code. (https://github.com/ballerina-platform/lsp4intellij)

The original code is Copyright WSO2 Inc., licensed under the `Apache 2.0` license.

</p>

<p>

The art assets inside src/art/zig, and all copies of them, are derived from the official Zig Programming Language logo,
which are the property of the Zig Software Foundation.
(https://github.com/ziglang/logo)
These art assets are licensed under `Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0).`

</p>

<p>

Parts of this codebase are based on the `intellij-zig` plugin,
developed by [HTGAzureX1212](https://github.com/HTGAzureX1212), licensed under the `Apache 2.0`.

</p>

<!-- Plugin description end -->
