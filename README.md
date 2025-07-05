# ZigBrains

Zig language support for IntelliJ IDEA, CLion, and other JetBrains IDEs. Now written in Kotlin!

# Installing

You can either install this plugin from the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/22456-zigbrains), or from [FalsePattern's website](https://falsepattern.com/zigbrains).

See [the quick setup guide](#quick-setup-guide-for-zig-and-zls) for how to set up language server integration.

Note: marketplace updates are usually delayed by a few days from the actual release, so if you want to always have the
latest builds of ZigBrains, you can set up your IDE to download signed releases directly from FalsePattern's website
through the built-in plugin browser:

1. Go to `Settings -> Plugins`
2. To the right of the `Installed` button at the top, click on the `...` dropdown menu, then select `Manage Plugin Repositories...`
3. Click the add button, and then enter the ZigBrains updater URL, based on your IDE version:
   - `2025.2.*` or newer: https://falsepattern.com/zigbrains/updatePlugins-252.xml
   - `2025.1.*`: https://falsepattern.com/zigbrains/updatePlugins-251.xml
   - `2024.3.*`: https://falsepattern.com/zigbrains/updatePlugins-243.xml
   - `2024.2.*`: https://falsepattern.com/zigbrains/updatePlugins-242.xml
   - `2024.1.*`: https://falsepattern.com/zigbrains/updatePlugins-241.xml
4. Click `OK`, and your IDE should now automatically detect the latest version
(both in the Installed tab and in the Marketplace tab), even if it's not yet verified on the official JetBrains marketplace yet.

## Versioning scheme
To reduce confusion and to better utilize semver, the plugin uses the following versioning scheme:

X - Major version, incremented any time a relatively large features is added or removed
Y - Minor version, incremented for smaller features or large refactors that don't change user-perceived behaviour
Z - Patch version, incremented only when a fix is purely an internal change and doesn't exceed an arbitrary threshold
of complexity (determined at the discretion of FalsePattern)

Note: before version 11, the version scheme used was 0.X.Y, without separate patch versions.
As this plugin will constantly be evolving together with the zig language, it makes no sense to keep the 0 prefix,
and might as well utilize the full semver string for extra information.

## Changelog
The changelog file follows https://common-changelog.org/

# Credits

## Supporters

- ### [Techatrix](https://github.com/Techatrix)
- ### [nuxusr](https://github.com/nuxusr)
- gree7
- xceno
- AnErrupTion

## Contributors

- [gatesn](https://github.com/gatesn)
- [MarioAriasC](https://github.com/MarioAriasC)
- [JensvandeWiel](https://github.com/JensvandeWiel)

## Additional Thanks

- The [ZigTools](https://github.com/zigtools/) team for developing the Zig Language Server.

- [HTGAzureX1212](https://github.com/HTGAzureX1212) for developing [intellij-zig](https://github.com/intellij-zig/intellij-zig),
which served as a fantastic reference for deep IDE integration features.

- The members of the `Zig Programming Language` discord server's `#tooling-dev` channel for providing encouragement,
feedback, and lots of bug reports. 

- The developers of [LSP4IJ](https://github.com/redhat-developer/lsp4ij) for providing a really good LSP backend

- The developers of the [intellij-rust](https://github.com/intellij-rust/intellij-rust/) plugin for providing an
excellent example on how to write debugger support that doesn't depend on CLion.

- And everyone who actively reported issues and helped ironing out all the remaining problems

# Description

<!-- Plugin description -->
Adds support for the Zig Language, utilizing the ZLS language server for advanced coding assistance.

Before you can properly use the plugin, you need to select or download the Zig toolchain and language server in `Settings` -> `Languages & Frameworks` -> `Zig`.

## Debugging

Debugger settings are available in the `Settings | Build, Execution, Deployment | Debugger` menu, under the `Zig` section. 

### IDE Compatibility
Debugging Zig code is supported in any native debugging capable IDE. The following have been verified to work so far:

- CLion
- IntelliJ IDEA Ultimate
- RustRover (including the non-commercial free version too)
- GoLand
- PyCharm Professional
- Android Studio

Additionally, in CLion, the plugin uses the C++ Toolchains for sourcing the debugger (this can be toggled off in the settings).

The open-source Community edition IDEs don't have the native debugging code as it's a proprietary module, so you cannot
debug zig code with them. You can still use those IDEs to develop code and use everything else the plugin has to offer.

### Windows

Supported debuggers: `MSVC`

Debugging on Windows requires you to set up the Microsoft debugger.

To do this, go to the following URL and install the MSVC compiler toolset according to step 3 in the prerequisites:
https://code.visualstudio.com/docs/cpp/config-msvc

### Linux

Supported debuggers: `LLDB`, `GDB`

### MacOS

Supported debuggers: `LLDB`

<!-- Plugin description end -->
