<!--
Changelog structure reference:
<<
## [Version]

### Type

#### Category
>>
"Type" is one of [Added, Changed, Deprecated, Removed, Fixed, Security]
"Category" should be something that can be quickly recognized by readers ("Highlighting", "Code Completion", "Folding", etc.)

"Type" ALWAYS follows the order in the list above
"Category" ALWAYS alphabetically sorted
-->

# ZigBrains

## [Unreleased]

### Added

- Zig
  - Code formatter and code style settings

### Fixed

- Project generation
  - Now actually populates the project directory with example files instead of just creating an empty directory

## [0.9.0]

### Added

- Zig
  - Commenter

- Zon
  - Can now parse the .paths attribute properly

### Changed

- Maximum compatible IDE version 232.* -> 233.*

### Fixed

- Dev env
  - nix jbr points to the correct path

- LSP
  - Crash in huge projects


## [0.8.1]

### Changed

- LSP
  - Dulled the colors of the status indicator, and added a gray "idle" color when ZLS is disconnected but not due to an error.

### Fixed

- Actions
  - Blocking other languages' run tasks
  - Files not autosaving before actions run

- Documentation
  - No longer breaks the documentation of other languages

- Editor
  - Race condition causing IllegalArgumentException

- LSP
  - Occasional NullPointerException when LSP returns blank data for inlay hints

## [0.8.0]

### Added

- Editor
  - Compatibility with 0.11 for loop ranges
  - Gutter icons for:
    - Launching a file with a `main` top level function
    - Launching a file with tests in it
    - Running `zig build` from a build.zig file

- Toolchain
  - Debugging Support


### Fixed

- Toolchain
  - Zig run configurations now save properly

## [0.7.0]

### Added

- Toolchain
  - Zig compiler toolchain integration and run actions (no debugging support yet, see the readme)
- Zig
  - Inlay hints
  - Breakpoints (CLion/IDEA Ultimate)
  - File creation prompt
- LSP
  - ZLS is now auto-detected on project startup from PATH
    - (You can also manually auto-detect it in the config menu)

### Changed

- Accessibility
  - The LSP status icon now has symbols in it instead of just colors:
    - Stopped(Red): X
    - Starting(Yellow): Refresh arrow
    - Started(Green): Empty

### Fixed

- LSP
  - NullPointerException in folding range provider when closing editors quickly
- Config
  - Changes to the ZLS configuration no longer require an IDE restart

## [0.6.0]

### Added

#### LSP
- Separate timeout category for syntax highlighting

#### Zig
- Basic "dumb" syntax highlighting when LSP is not connected
- Go to usages now works properly
- Color scheme preview now works properly
- Better "smart" syntax highlighting when LSP is connected
- Brace/Parenthesis/Bracket matching

### Fixed

#### Code Actions
- IDE no longer freezes when ZLS responds slowly


### Security

- Updated dependencies
- Integrated LSP4IntelliJ directly into ZigBrains

## [0.5.2]

### Fixed
- NullPointerException when clicking the red circle when the LSP is not connected

## [0.5.1] "Modernization"

### Added
- Proper documentation view (CTRL+Q) instead of the janky hover thing

### Removed
- IDEA 2022 support (Necessary change for the new documentation backend in lsp4intellij)

### Fixed
- Error highlighting now works on IDEA 2023

## [0.5.0] "The *ZON*iverse"

### Added

#### .zon files
- Basic parser and PSI tree
- Basic syntax highlighting
- Color settings page
- Brace and quote matching
- Code completion
- Code Folding
- Indentation

### Changed
- Updated the LSP backend, it should be more resilient now

## [0.4.0]

### Added

#### Error diagnostics (NEW)
- Basic diagnostics info from LSP (mostly just trivial syntax errors)

#### Code Folding
- Asynchronous folding (Enable it in the settings!)

### Fixed

#### Syntax Highlighting
- Made the logic even more asynchronous, should lead to much less UI stuttering

## [0.3.1]

### Added

#### Folding
- Better folding regions instead of just `{...}`
  - `...` for the general case
  - `///...` for doc comments

### Fixed

#### Folding
- Race condition on IDE startup throwing exceptions
- Folding ranges not appearing on Windows
- Typo in the bounds checking code

## [0.3.0]

### Added

#### Highlighting
- Support for Semantic Token Deltas (more compact way for the LSP server to send back data when typing fast)

#### LSP
- Temporary "increase timeout" toggle (currently, it bumps all timeouts to 15 seconds)

### Fixed

#### Folding
- Occasional NPE in LSP4IntellIJ

#### Highlighting
- Last token in file not getting highlighted

#### LSP
- (Windows) ZLS binary not executing if the file path has weird characters

## [0.2.0]

### Added
- Code completion
- Code folding
- More ZLS config options

## [0.1.0]

### Added
- Initial prototype. Lots of important stuff not yet implemented, but basic syntax highlighting and go to definition works.