import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    kotlin("plugin.serialization")
}

val lsp4ijVersion: String by project
val lsp4jVersion: String by project
val ideaCommunityVersion: String by project
val useInstaller = property("useInstaller").toString().toBoolean()

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, ideaCommunityVersion, useInstaller = useInstaller)
    }
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
    compileOnly("com.redhat.devtools.intellij:lsp4ij:$lsp4ijVersion")
    compileOnly("org.eclipse.lsp4j:org.eclipse.lsp4j:$lsp4jVersion")
    implementation(project(":core")) {
        isTransitive = false
    }
}