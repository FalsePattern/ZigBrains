plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "ZigBrains"

include("zig")
project(":zig").projectDir = file("modules/zig")
include("zon")
project(":zon").projectDir = file("modules/zon")