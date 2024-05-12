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
-->

# ZigBrains

## [Unreleased]

## [14.2.0]

### Added

- Zig
  - External Libraries support for zig stdlib

### Fixed

- Debugging (Windows)
  - Variables sometimes don't show up in the variable inspector when in breakpoint state

## [14.1.0]

### Fixed

- Debugging
    - Huge rework for starting the various debugging runs, and more robust compilation error visualization instead of a tiny
      popup

- LSP
  - No more notification popup about zig env not being detected when not in a zig projects.

- Project
  - ZLS should now be detected more reliably when creating new projects

## [14.0.1]

### Fixed

- Zig
  - If statements without a block always showed an error

## [14.0.0]

### Added

- LSP
  - The status widget now auto-hides itself when the selected editor is not a zig file in the current window

- Project
  - Completely overhauled the configuration system and the new project creation window. All the configs have been unified
  into a single screen, and project creation has been fully integrated as a mainline feature, instead of just a "nice to have".

### Changed

- LSP
  - The injection of the various language actions (Go to declaration/implementation, reformat, etc.) has been
  reimplemented from the ground up to be much more reliable and compatible in the presence of other languages and plugins.

- Zig, ZLS
  - The configurations have been unified into a single cohesive interface
  - Improved auto-detection for both Zig and ZLS

### Fixed

- LSP
  - Putting the caret on a diagnostics error now no longer highlights the whole file

- Project
  - Fixed invalid --colored command line argument for zig tasks

- Zig
  - More robust indentation logic, also works with semi-invalid syntax now

## [13.2.0]

### Added

- Debugging
  - For Zig build tasks, the target executable is now auto-detected in `zig-out/bin` if not specified.
  Autodetect fails if multiple executables are present for consistency's sake.
  - You can specify custom command line arguments for the debugged executable.

- Project
  - The line marker generated `zig build` now defaults to the `run` step.

### Changed

- Project
  - `zig build` steps are now specified separately from miscellaneous command line arguments.
  This is needed for the debugger to work properly.
  - The zig build debug executable target configs are now hidden from Zig build tasks in IDEs without native debugging support.
  - Native Application (Zig) is now hidden in IDEs without native debugging support.

### Fixed

- Debugging
  - Debugger locks up when trying to debug `zig build run` tasks.

## [13.1.1]

### Fixed

- Project
  - Creating new project throws a write access error when git is enabled

- Zig
  - Accidental regression while renaming the action IDs that broke "find usages"

## [13.1.0]

### Added

- Zig
  - Parameter info (CTRL + P) is now properly integrated
  - Parser error recovery (completion will still work even with missing semicolons in a statement)

### Fixed

- LSP
  - The registry IDs of some of the LSP handlers were colliding with the Rust intellij plugin
  - Autocompletion insertion is now fully handled by intellij, this should fix some of the weirdness

- Zig
  - Indent support for function parameters and struct initializers
  - Updated to latest grammar spec (https://github.com/ziglang/zig-spec/commit/78c2e2e5cfa7090965deaf631cb8ca6f405b7c42)

## [13.0.1]

### HOTFIX CHANGES
- Fixed multiple critical null safety problems that caused plugin crashes on some systems
- Splitting the editor now no longer breaks semantic highlighting

The rest of the 13.0.0 changes are available below:

### Added

- Debugging
  - Debugging support for tests when launched using the ZigTest task type (and with the gutter icons in the editor)
  - Debugging support on Windows systems

- Project
  - Added `zig init` as a new project creation option
  - New projects will now have the project name in the build files instead of "untitled"

- Zig
  - Updated semantic highlighting to latest ZLS protocol

- ZLS
  - ZLS configuration is now partially editable through the GUI

### Fixed

- Project
  - Fixed build.zig and build.zig.zon getting placed in src/ during project creation

- Plugin
  - Removed a bunch of write action locking, the editor should feel more responsive now

- Zig
  - Error highlighting was breaking all the time

### Removed

- Project
  - !!!BREAKING CHANGE!!! There is now no arbitrary "zig execution" task, all zig tasks have been categorized into Zig run/build/test tasks respectively.

## [12.0.0]

### Added

- Debugger
  - Now uses the toolchains you set in Settings | Build, Execution, Deployment | Toolchains
  - Standard library stack frames are now automatically filtered from the debug stack trace

- Zig
  - Go to Declaration/Usages now functions as expected, taking you to the declaration of a symbol instead of its resolved
    implementation.
  - For the time being, the "Quick Definition" (CTRL+Shift+I) action has been repurposed as Go To Definition. This will be
    replaced with a properly integrated solution once a way to couple the PSI symbol system and the LSP has been found.

### Fixed

- LSP
  - Diagnostics race condition
  - Code action annotations no longer lose range

- Zig
  - Syntax highlighting no longer breaks after refactoring or reformatting
  - Go to Usages no longer freezes the IDE

## [11.1.0]

### Changed

- Zig
  - Updated to latest language grammar (destructuring syntax)

## [11.0.0]

### Changed

- Zon
  - Updated autocompletion to latest as per the zig spec

- The versioning scheme used for ZigBrains has been revamped. See the plugin's GitHub repo for more information.

### Fixed

- Zig
  - Autocomplete now uses the LSP
  - Auto-indentation is now more accurate when creating new {...} blocks

- Zon
  - Fixed auto-indent for strings and comments

### Removed

- Zig
  - Code style settings. The official zig formatter is not configurable either, and ZigBrains aims to minimize
    divergence from any official or ZLS-supplied features where possible.

## [0.10.0]

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