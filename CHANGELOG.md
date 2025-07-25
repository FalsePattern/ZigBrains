# Changelog

## [Unreleased]

## [26.3.1] - 2025-07-23

### Fixed

- Zig
  - IDE freeze when changing toolchains

## [26.3.0] - 2025-07-23

### Added

- Zig
  - Support for ZON-returning `zig env` on zig 0.15+

## [26.2.1] - 2025-07-06

### Fixed

- Zig
  - Color settings code preview had a broken color tag (by ENDERZOMBI102)
  - Quoted strings were being detected as multiline

## [26.2.0] - 2025-06-25

### Added

- Zig
  - Top level documentation comments can now be viewed as documentation popup/panel

## [26.1.0] - 2025-06-21

### Added

- Project
  - Ability to directly open a project when opening a `build.zig` or `build.zig.zon` file via the `File > Open` menu (by ENDERZOMBI102)
  - Hide tool window if no zig toolchains are configured for the current workspace, as it most probably means that it isn't a Zig project (by ENDERZOMBI102)

### Fixed

- Project
  - Exception from zig terminal filter when running under read action

## [26.0.2] - 2025-05-26

### Fixed

- LSP
  - Loosened version matching (ZLS 0.14.0 with Zig 0.14.1, etc.)

## [26.0.1] - 2025-05-21

### Fixed

- LSP
  - Language server not starting on Windows when there's a space in the binary path

## [26.0.0] - 2025-05-19

### Added

- IDEA 2025.2 support

## [25.5.1] - 2025-05-18

### Added

- Toolchain, LSP
  - User input sanitization for path configs

## [25.5.0] - 2025-05-15

### Added

- LSP
  - Custom selection ranges toggle in the ZLS settings, turned off by default.

## [25.4.2] - 2025-05-11

### Fixed

- Zig, Zon
  - Fixed escape sequence parsing in strings/character literals

## [25.4.1] - 2025-05-06

### Fixed

- Zig
  - Multiline strings with comments inside of them now get processed correctly

## [25.4.0] - 2025-05-06

### Added

- Toolchain
  - Toolchains installed in the gradle cache by [Gradle ZigBuild](https://plugins.gradle.org/plugin/com.falsepattern.zigbuild) are now detected and shown in the toolchain suggestions.

### Changed

- Debugger
  - Not specifying a build step for debugging zig build tasks is now an error

## [25.3.0] - 2025-05-01

### Added

- Debugger
  - Hint message for setting up zig build test debugging

### Removed

- Debugger
  - Output executable autodetection. It was too unreliable and people kept making bug reports.

### Fixed

- Debugger
  - Internal error when compilation fails

- Zig, Zon
  - Escaped backslashes in strings broke the parser

## [25.2.0] - 2025-04-20

### Changed

- Project
  - Line marker task suggestions for main/test now defer to Zig Build if build.zig file is detected.

### Added

- Debugger
  - Notify the user if zig run / zig test debugging starts, but a build.zig is present

### Fixed

- Debugger
  - Compilation failures did not open the terminal properly and suppressed the error message

## [25.1.0] - 2025-04-17

### Added

- IDEA 2025.1 support

- LSP
  - Configurable inlay hints file size limit to reduce IDE lag

## [25.0.2] - 2025-04-15

### Fixed

- Project
  - ZLS settings not scrollable in the language server list

## [25.0.1] - 2025-04-11

### Changed

- Project
  - BREAKING MAJOR UPDATE: Fully reworked toolchain and language server management
    The configuration menu is now very similar to the intellij java toolchain management,
    with proper toolchain selection, detection, downloading, etc. This change will require
    you to re-configure your toolchains!
  - Zig external library root is now no longer shown if zig is not configured

### Fixed

- Project
  - Zig.iml file created in every project

## [24.0.1] - 2025-03-27

### Added

- Project, Debugging
  - TTY support for zig processes

### Removed

- Project
  - "Emulate terminal" and "colored output" config options have been removed from zig run/test/build tasks, as they are no longer required for ZigBrains to work.

### Fixed

- Debugger
  - Build errors didn't get shown in the console

- Project
  - File path browse buttons in zig run configurations didn't work
  - Occasional GUI deadlocks

- Zig
  - IPC wrapper wasn't passing exit code

## [23.1.2] - 2025-03-27

### Fixed

- LSP
  - IDE warning when renaming symbols

## [23.1.1] - 2025-03-26

### Fixed

- Project
  - New project creation creates a blank ZLS config

## [23.1.0] - 2025-03-26

### Changed

- Direnv
  - Centralized all direnv toggling into a single project-level option

### Added

- Project
    - Support running file main/tests with hotkey (default: ctrl+shift+f10)

## [23.0.2] - 2025-03-23

### Fixed

- Zig
  - Documentation comment after regular comment was being highlighted as regular comment

## [23.0.1] - 2025-03-19

### Fixed

- Project
  - mkfifo/bash for zig progress visualization is now detected more reliably (fixes error on macOS)
  - Deadlock when launching zig build tasks

## [23.0.0] - 2025-03-15

### Added

- Project
  - Zig std.Progress visualization in the zig tool window (Linux/macOS only)

### Removed

- Project
  - Executable / Library new project templates temporarily removed until zig stabilizes

## [22.0.1] - 2025-03-13

### Fixed

- LSP
  - Changing ZLS configs would not restart ZLS

- Project
  - Occasional "AWT events are not allowed inside write action" error coming from LSP
  - IllegalStateException coming from the standard library handler

## [22.0.0] - 2025-03-13

### Changed

- Project
  - !!BREAKING CHANGE!! Changed file format of zig tasks to store command line arguments as strings instead of string lists.
    This (and newer) versions of the plugin will automatically upgrade tasks from 21.1.0 and before.

### Added

- LSP
  - Error/Warning banner at the top of the editor when ZLS is misconfigured/not running
  - ZLS version indicator in the zig settings

- Toolchain
  - More descriptive error messages when toolchain detection fails

### Fixed

- Debugging
  - Breakpoints could not be placed inside zig code in Android Studio

- Project
  - Zig run/debug configuration command line arguments would lose quotes around arguments

## [21.1.0] - 2025-03-11

### Changed

- Zon
  - Fully refactored the parser for parity with the zig parser

### Added

- Zon
  - ZLS integration

## [21.0.0] - 2025-03-11

### Changed

- Project
  - New project panel is now much more compact

### Added

- Zig
  - Changing the zig standard library path in the project settings now properly updates the dependency
- ZLS
  - All of the config options are now exposed in the GUI

### Fixed

- Zig
  - `zig env` failure causes an IDE error
  - A local toolchain disappearing (std directory or zig exe deleted) is now handled properly

## [20.3.0] - 2025-02-06

- Zig
  - Improved default colors

## [20.2.2] - 2025-01-30

### Fixed

- Debugging
  - `zig build run` would run the process twice, one without, one with debugging

## [20.2.1] - 2025-01-22

### Fixed

- Zig
  - Lexer error when a zig file has a comment or multiline string at the end of file without trailing newline

## [20.2.0] - 2025-01-21

### Added

- Zig
  - Live template support

## [20.1.3] - 2025-01-15

### Added

- Project
  - `.zig-cache` directory added to autogenerated gitignore in the project generator

### Fixed

- Project
  - Zig Build tool window crashes when opening remote projects

## [20.1.2] - 2025-01-12

### Fixed

- Zig
  - Source file path highlighter made the terminal lag with some files
  - Non-terminating rule in lexer could make the editor hang

## [20.1.1] - 2024-12-24

### Fixed

- Zig
  - Unterminated string at the end of the file soft-locks the editor
  - Trailing commas in for loop parameters don't get parsed correctly

## [20.1.0] - 2024-12-22

### Added

- Zig
  - String, character literal, and `@"identifier"` quote matching

### Fixed

- Zon
  - Broken string quote handling

## [20.0.4] - 2024-12-11

### Fixed

- Renamed Zig new file task to "Zig File" and moved to the file creation group

## [20.0.3] - 2024-11-28

### Fixed

- Project
  - "Save all documents" hanging when trying to run a zig file

## [20.0.2] - 2024-11-11

### Changed

- Project
  - Direnv now only runs automatically in trusted projects
  - Toolchain autodetection is now done in the background on project load

### Added

- Zig
  - Escape sequence highlighting in char literals

### Fixed

- Zig
  - Unicode characters in char literals triggered an error

## [20.0.1] - 2024-11-09

### Fixed

- Project
  - IDE freezes when opening a zig project / doing zig init
  - Test tasks don't work and try to run the file as an executable

- Zig
  - Struct fields being styled as static fields instead of instance fields

## [20.0.0] - 2024-11-07

### Changed

- The entire plugin has been re-implemented in Kotlin

### Added

- Debugging
  - Progress indicator while zig is compiling the debuggable exe

### Fixed

- Most of the internals have been rewritten to be fully asynchronous, so freezes should happen way less

## [19.3.0] - 2024-10-31

### Added

- Toolchains, Run Configurations
  - [Direnv](https://github.com/direnv/direnv) support

### Fixed

- Zig
  - Missing description for string conversion intentions

## [19.2.0] - 2024-10-26

### Added

- Zig
  - Enter key handling in strings and multi line strings
  - Intentions for converting between multi line and quoted strings

### Fixed

- Zig
  - Multiline string language injections broke when editing the injected text

## [19.1.0] - 2024-10-25

### Changed

- Runner
  - The process execution pipeline is now fully asynchronous
  - Error output is no longer redirected to standard output

### Added

- Zig
  - Language injections in strings
  - Syntax highlighting for escape sequences in strings

- LSP
  - Option to toggle inlay hints on/off
  - Compacted error{...} blocks in inlay hints

### Fixed

- Debugger
  - Zig compilation will no longer cause IDE freezes
  - Debugging with GDB no longer causes internal IDE warnings
  - Debugging `zig run` configurations is now possible
- LSP
  - Rare error when checking LSP presence
  - No more error spam when zig or zls binary is missing

## [18.0.0] - 2024-10-17

### Changed

- LSP
  - Updated to LSP4IJ 0.7.0

### Added

- Zig
  - Labeled switch statements

## [17.3.0] - 2024-10-03

### Changed

- LSP
  - Updated to LSP4IJ 0.6.0

### Added

- Zig
  - Structure view

### Fixed

- Project
  - CLion will no longer prompt you to import zig projects as CMake

## [17.2.0] - 2024-09-20

### Added

- IDEA 2024.3 support

### Fixed

- Project
  - Safer standard library path resolution

## [17.1.0] - 2024-08-21

### Removed

- ZLS
  - Obsolete config options which are no longer used since migrating to LSP4IJ

### Fixed

- Project
  - Relative paths in zig toolchain configuration would break the entire IDE

## [17.0.0] - 2024-08-06

### Changed

- Project
  - Increased internal zig tool timeout to 10 seconds. Note that tasks don't have timeout, this is only used for
    ZigBrains getting metadata about the compiler and the buildscript.

### Added

- Project
  - Zig Build integrated into an IDE tool window. Currently only supports running single steps, for more complex steps,
create a custom build configuration as before.

### Fixed

- Project
  - Toolchain working directory was not set when requesting compiler metadata

## [16.1.3] - 2024-07-31

### Changed

- Debugger (Windows)
  - MSVC debugger metadata download now requires consent from the user
  - Metadata download is now cached after the first fetch
  - Metadata download timeout has been set to 3 seconds, after which it reverts to the fallback file

## [16.1.2] - 2024-07-26

### Fixed

- Zig
  - Comptime struct fields not being parsed properly

## [16.1.1] - 2024-07-26

### Fixed

- Zig
  - Standard library override always auto-enabling
  - Better toolchain autodetect

- ZLS
  - Better language server autodetect

## [16.1.0] - 2024-07-25

### Added

- Zon
  - Support for .lazy dependency property
  - Comment/uncomment hotkey support

### Fixed

- Zon
  - More reliable autocomplete

## [16.0.0] - 2024-07-20

### Changed

- LSP
  - Migrated to Red Hat's LSP4IJ LSP adapter.

### Fixed

- Debugger
  - Added fallback metadata for windows debugger downloading
  - Automatic exe path discovery for zig build run debugging on windows

- Zig
  - Color settings has more accurate color preview text.
  - Better builtin indentation

## [15.2.0] - 2024-06-13

### Added

- Project
  - Modifying the standard library path now also applies to ZLS

## [15.1.1] - 2024-06-06

### Fixed

- Project
  - PTY emulation is now opt-in in run configurations

## [15.1.0] - 2024-06-03

### Added

- Project
  - PTY emulation for non-debug runs. Fixes colored output in Ziglings.

## [15.0.3] - 2024-06-03

### Fixed

- Zig
  - More autocomplete fixes

## [15.0.2] - 2024-06-02

### Fixed

- Zig
  - Autocomplete not working when the caret is placed right after a "("

## [15.0.1] - 2024-06-01

### Fixed

- Zig
  - Trailing commas in struct initializers showed an error

## [15.0.0] - 2024-05-31

### Changed

- Debugging
  - Major update, debugging on linux now works outside CLion (confirmed working in RustRover, IDEA Ultimate)
  - Windows debugging has been made much more streamlined, user doesn't need to download random files manually anymore
(except the visual studio debugging sdk of course)
  - debugging support on macOS with LLDB
- Project
  - Updated new project templates to the latest Zig 0.13.0 init files

### Removed

- LSP
  - Notification spam about ZLS missing in non-zig projects

### Fixed

- Zig
  - Fixed inconsistent caret indenting for switches and function parameters
  - More robust highlighting when auto-formatting
  - Fixed multiple grammar errors

## [14.4.0] - 2024-05-28

### Fixed

- Zig
  - Fixed indentation to be more consistent with zig fmt
  - Code completion now works correctly on the first line in a file too

## [14.3.0] - 2024-05-15

### Added

- Project
  - Extra compiler arguments field for zig test/run tasks

### Fixed

- Debugging
  - The debugger no longer freezes the IDE while zig is compiling

- Project
  - Exe args for zig run not visible in the GUI

## [14.2.0] - 2024-05-12

### Added

- Zig
  - External Libraries support for zig stdlib

### Fixed

- Debugging (Windows)
  - Variables sometimes don't show up in the variable inspector when in breakpoint state

## [14.1.0] - 2024-05-11

### Fixed

- Debugging
    - Huge rework for starting the various debugging runs, and more robust compilation error visualization instead of a tiny
      popup

- LSP
  - No more notification popup about zig env not being detected when not in a zig projects.

- Project
  - ZLS should now be detected more reliably when creating new projects

## [14.0.1] - 2024-04-27

### Fixed

- Zig
  - If statements without a block always showed an error

## [14.0.0] - 2024-04-19

### Changed

- LSP
  - The injection of the various language actions (Go to declaration/implementation, reformat, etc.) has been
    reimplemented from the ground up to be much more reliable and compatible in the presence of other languages and plugins.

- Zig, ZLS
  - The configurations have been unified into a single cohesive interface
  - Improved auto-detection for both Zig and ZLS

### Added

- LSP
  - The status widget now auto-hides itself when the selected editor is not a zig file in the current window

- Project
  - Completely overhauled the configuration system and the new project creation window. All the configs have been unified
  into a single screen, and project creation has been fully integrated as a mainline feature, instead of just a "nice to have".

### Fixed

- LSP
  - Putting the caret on a diagnostics error now no longer highlights the whole file

- Project
  - Fixed invalid --colored command line argument for zig tasks

- Zig
  - More robust indentation logic, also works with semi-invalid syntax now

## [13.2.0] - 2024-04-05

### Changed

- Project
  - `zig build` steps are now specified separately from miscellaneous command line arguments.
    This is needed for the debugger to work properly.
  - The zig build debug executable target configs are now hidden from Zig build tasks in IDEs without native debugging support.
  - Native Application (Zig) is now hidden in IDEs without native debugging support.

### Added

- Debugging
  - For Zig build tasks, the target executable is now auto-detected in `zig-out/bin` if not specified.
  Autodetect fails if multiple executables are present for consistency's sake.
  - You can specify custom command line arguments for the debugged executable.

- Project
  - The line marker generated `zig build` now defaults to the `run` step.

### Fixed

- Debugging
  - Debugger locks up when trying to debug `zig build run` tasks.

## [13.1.1] - 2024-03-23

### Fixed

- Project
  - Creating new project throws a write access error when git is enabled

- Zig
  - Accidental regression while renaming the action IDs that broke "find usages"

## [13.1.0] - 2024-03-15

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

## [13.0.1] - 2024-03-12

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

### Removed

- Project
  - !!!BREAKING CHANGE!!! There is now no arbitrary "zig execution" task, all zig tasks have been categorized into Zig run/build/test tasks respectively.

### Fixed

- Project
  - Fixed build.zig and build.zig.zon getting placed in src/ during project creation

- Plugin
  - Removed a bunch of write action locking, the editor should feel more responsive now

- Zig
  - Error highlighting was breaking all the time

## [12.0.0] - 2024-02-29

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

## [11.1.0] - 2024-02-21

### Changed

- Zig
  - Updated to latest language grammar (destructuring syntax)

## [11.0.0] - 2024-02-01

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

## [0.10.0] - 2024-01-25

### Added

- Zig
  - Code formatter and code style settings

### Fixed

- Project generation
  - Now actually populates the project directory with example files instead of just creating an empty directory

## [0.9.0] - 2023-12-02

### Changed

- Maximum compatible IDE version 232.* -> 233.*

### Added

- Zig
  - Commenter

- Zon
  - Can now parse the .paths attribute properly

### Fixed

- Dev env
  - nix jbr points to the correct path

- LSP
  - Crash in huge projects


## [0.8.1] - 2023-10-04

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

## [0.8.0] - 2023-08-21

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

## [0.7.0] - 2023-08-19

### Changed

- Accessibility
  - The LSP status icon now has symbols in it instead of just colors:
    - Stopped(Red): X
    - Starting(Yellow): Refresh arrow
    - Started(Green): Empty

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

### Fixed

- LSP
  - NullPointerException in folding range provider when closing editors quickly
- Config
  - Changes to the ZLS configuration no longer require an IDE restart

## [0.6.0] - 2023-08-17

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

## [0.5.2] - 2023-08-14

### Fixed
- NullPointerException when clicking the red circle when the LSP is not connected

## [0.5.1] - 2023-08-12

### Added
- Proper documentation view (CTRL+Q) instead of the janky hover thing

### Removed
- IDEA 2022 support (Necessary change for the new documentation backend in lsp4intellij)

### Fixed
- Error highlighting now works on IDEA 2023

## [0.5.0] - 2023-08-10

### Changed
- Updated the LSP backend, it should be more resilient now

### Added

#### .zon files
- Basic parser and PSI tree
- Basic syntax highlighting
- Color settings page
- Brace and quote matching
- Code completion
- Code Folding
- Indentation

## [0.4.0] - 2023-08-02

### Added

#### Error diagnostics (NEW)
- Basic diagnostics info from LSP (mostly just trivial syntax errors)

#### Code Folding
- Asynchronous folding (Enable it in the settings!)

### Fixed

#### Syntax Highlighting
- Made the logic even more asynchronous, should lead to much less UI stuttering

## [0.3.1] - 2023-08-01

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

## [0.3.0] - 2023-07-31

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

## [0.2.0] - 2023-07-29

### Added
- Code completion
- Code folding
- More ZLS config options

## [0.1.0] - 2023-07-29

### Added
- Initial prototype. Lots of important stuff not yet implemented, but basic syntax highlighting and go to definition works.