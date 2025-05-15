import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    kotlin("plugin.serialization")
}

val lsp4ijVersion: String by project
val lsp4jVersion: String by project
val lsp4ijNightly = property("lsp4ijNightly").toString().toBoolean()
val ideaCommunityVersion: String by project
val useInstaller = property("useInstaller").toString().toBoolean()
val serializationVersion: String by project
val lsp4ijPluginString = "com.redhat.devtools.lsp4ij:$lsp4ijVersion${if (lsp4ijNightly) "@nightly" else ""}"

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, ideaCommunityVersion, useInstaller = useInstaller)
        if (lsp4ijNightly) {
            plugin(lsp4ijPluginString)
        }
    }
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:$serializationVersion") {
        isTransitive = false
    }
    if (!lsp4ijNightly) {
        compileOnly("com.redhat.devtools.intellij:lsp4ij:$lsp4ijVersion")
    }
    compileOnly("org.eclipse.lsp4j:org.eclipse.lsp4j:$lsp4jVersion")
    implementation(project(":core")) {
        isTransitive = false
    }
}