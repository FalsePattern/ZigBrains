# ZigBrains

<!-- Plugin description -->
Yet another attempt at bringing zig to the IntelliJ world.

## QUICKSTART
Go to `Settings` -> `Languages & Frameworks` -> `Zig` -> `ZLS path` -> select your `zls` executable

## Feature tracker:
- Working:
  - Code completion
  - Code folding
  - Syntax highlighting
  - Basic error diagnostics
  - Go to definition
  - Rename symbol
  - Hover documentation
- TODO:
  - Go to implementations / find usages
  - Workspace Symbols

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
<!-- Plugin description end -->

## Notes to self

LSP4IntelliJ uses a somewhat strange popup for hover hints that cannot be clicked, need to investigate

## Licenses
```
All code in this project, unless specified differently, is licensed under the Apache 2.0 license.
```
```
The art assets inside src/art/zig, and all copies of them, are derived from the official Zig Programming Language logo,
which are property of the Zig Software Foundation. (https://github.com/ziglang/logo).
These art assets are licensed under `Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0).`
```
