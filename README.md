# ZigBrains

## Developer guide

### All platforms

After importing the gradle project, you need to run the `build setup -> generateSources` tasks.

### NixOS

In addition to the generated sources, you also need to run the `build setup -> nixos_jbr` task, otherwise java will
complain about missing files

# Description

<!-- Plugin description -->
An LSP-powered language plugin for the Zig Programming Language.


## QUICKSTART
1. Download or compile the ZLS language server, available at https://github.com/zigtools/zls
2. Go to `Settings` -> `Languages & Frameworks` -> `Zig` -> `ZLS path` -> set the path to the `zls` executable you downloaded or compiled
3. Open a .zig file, and wait for the circle in the bottom status bar to turn Green (empty).
See below for an explanation on what the circle means.

## LSP status icon explanation
Red (X symbol):
LSP server is stopped. You either don't have a proper ZLS path set, or you don't have a .zig file open.

Yellow ("refresh arrow" symbol):
LSP server is starting, please be patient.

Green (empty):
LSP server is running.

## Feature tracker:

### .zig files:
- Working:
  - Code completion
  - Code folding
  - Syntax highlighting
  - Inlay hints
  - Basic error diagnostics
  - Go to definition
  - Rename symbol
  - Hover documentation
  - Go to implementations / find usages
  - Brace/Parenthesis/Bracket matching
  - Breakpoints (CLion/IDEA Ultimate)
- TODO:
  - Workspace Symbols

### .zon files:
- Syntax highlighting
- Formatting and indentation
- Code completion
- Brace folding
- Automatic brace and quote pairing

## The motivation
The other existing Zig language plugins for IntelliJ rely a lot on the PSI tree.
This seems correct in theory, until
the sheer power of Zig's comptime is taken into consideration.

The comptime makes any sort of contextual help implemented with the PSI tree a lot more restrictive,
and adding LSP integration at that point is an uphill battle.

## Current state of the project
This project takes the opposite approach: The initial implementation *completely* relies on ZLS, with no lexer or parser
in sight.
Using a language server immediately gives us access to advanced features such as refactoring, go to definition,
semantics-based highlighting, and so on.

However, this also restricts the amount of IDE integration the language plugin can achieve,
and things like live previews, peek definition, go to usage previews, and many other features that deeply integrate with
the PSI system just don't work at all.

## Long-term plans
The first and foremost goal of this project is deeply integrating ZLS into the IDE,
and LSP-provided information *always* takes the first seat.

However, we must also not completely reject the PSI tree,
as it has its own merits when used wisely, such as basic "dumb mode" syntax highlighting,
proper caret placements with go to usages, and so on.

Thus, this project will still use PSI trees and the IntelliJ lexer/parser system, but with heavy moderation, and any
sort of "smart inspection" *shall not* be implemented in the PSI, but instead retrieved from the language server.


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
which are property of the Zig Software Foundation. (https://github.com/ziglang/logo).
These art assets are licensed under `Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0).`

</p>

<!-- Plugin description end -->
