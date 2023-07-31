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
  - Code folding
  - Syntax highlighting
  - Go to definition
  - Rename symbol
  - Hover documentation
- TODO:
  - Go to implementations / find usages
  - Workspace Symbols

### Why LSP-only?

ZLS is completely universal and compatible with any IDE that can communicate with language servers.

This also means that ZLS has a much more active development/maintenance team than a single intellij plugin.
Any sort of parsing issues that might arise from language changes get fixed much faster in ZLS.

By *completely* relying on ZLS, hopefully this plugin will "just work" even when the language itself changes,
as long as the ZLS binary is kept up to date.
<!-- Plugin description end -->

## Extended TODOs and notes for self (and potential contributors)

### Better hints

LSP4IntellIJ uses a somewhat strange popup for hover hints that cannot be clicked, need to investigate

### Non-file syntax highlighting

Probably the most difficult one, as we don't have a PSI tree, so there's a pretty high change that we'll need to
reach deep into intellij internals for this one.
This applies to things like the Find Usages, mentioned in the TODOs.

An important part is that we need to avoid falling back onto a PSI tree, as that would go against one of the primary
goals of this project, which is completely relying on ZLS to do the heavy lifting.

Possible solutions:
- Take the "file-less" editor, read the text into a temporary file, and then feed it to ZLS for syntax highlighting.
- Study more of the LSP protocol and see if there's a builtin way to feed "virtual files" to the language server
- Make the preview text a "sub-view" of an actual editor in intellij. This might be quite difficult if intellij has no
builtin support for this kind of thing.

## Licenses
```
All code in this project, unless specified differently, is licensed under the Apache 2.0 license.
```
```
The `zig.svg` and `zigbrains.svg` files are derived from the official Zig Programming Language logo,
which are property of the Zig Software Foundation. (https://github.com/ziglang/logo).
These art assets are licensed under `Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0).`
```
