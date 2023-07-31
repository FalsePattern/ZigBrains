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