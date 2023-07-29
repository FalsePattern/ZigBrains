# ZigBrains

<!-- Plugin description -->
Yet another attempt at bringing zig to the intellij world, but this time without fumbling about with the
IntellIJ parser/tokenizer.

QUICKSTART:
1. Go to `Settings` -> `Languages & Frameworks` -> `Zig` -> `ZLS path` -> select your `zls` executable
2. If you want to change the color scheme, go to `Settings` -> `Editor` -> `Color Scheme` -> `Zig`

That's it. (for now)

Feature tracker:
- Working:
  - Code completion
  - Syntax highlighting
  - Go to definition
  - Rename symbol
  - Hover documentation
- TODO:
  - Autocomplete
    (Worked with an older version of ZLS, probably a protocol change, will investigate)
  - Go to implementations / find usages
    (A bit broken without a PSI tree, will need to poke my nose into LSP4IntellIJ internals for this)
  - Workspace Symbols
    (Will add it later, personally I never used this feature yet)
<!-- Plugin description end -->

## Licenses
```
All code in this project, unless specified differently, is licensed under the Apache 2.0 license.
```
```
The `zig.svg` and `zigbrains.svg` files are derived from the official Zig Programming Language logo,
which are property of the Zig Software Foundation. (https://github.com/ziglang/logo).
These art assets are licensed under `Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0).`
```
