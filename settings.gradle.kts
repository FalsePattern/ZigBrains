plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "ZigBrains"

for (module in arrayOf("zig", "zon", "lsp")) {
    include(module)
    project(":$module").projectDir = file("modules/$module")
}
