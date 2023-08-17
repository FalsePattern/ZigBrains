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