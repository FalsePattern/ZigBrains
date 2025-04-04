plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
rootProject.name = "ZigBrains"

include("core")
include("lsp")
include("cidr")
