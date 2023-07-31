<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# ZigBrains

## [Unreleased]

### Added

#### LSP
- Temporary "increase timeout" toggle (currently, it bumps all timeouts to 15 seconds)

#### Highlighting
- Support for Semantic Token Deltas (more compact way for the LSP server to send back data when typing fast)

### Fixed

#### Highlighting
- Last token in file not getting highlighted

#### Folding
- Occasional NPE in LSP4IntellIJ

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