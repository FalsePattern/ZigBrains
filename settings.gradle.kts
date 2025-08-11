plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "ZigBrains"

include("core")
include("lsp")
include("cidr")
