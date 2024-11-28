import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.intellij.platform.gradle.Constants
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("de.undercouch.download") version("5.6.0")
}
val lsp4jVersion: String by project
val clionVersion: String by project
val useInstaller = property("useInstaller").toString().toBoolean()

val genOutputDir = layout.buildDirectory.dir("generated-resources")
sourceSets["main"].resources.srcDir(genOutputDir)

tasks {
    register<Download>("downloadProps") {
        src("https://falsepattern.com/zigbrains/msvc.properties")
        dest(genOutputDir.map { it.file("msvc.properties") })
    }
    processResources {
        dependsOn("downloadProps")
    }
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.CLion, clionVersion, useInstaller = useInstaller)
        bundledPlugins("com.intellij.clion", "com.intellij.cidr.base", "com.intellij.nativeDebug")
    }
    implementation(project(":core"))
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.debug:$lsp4jVersion") {
        exclude("org.eclipse.lsp4j", "org.eclipse.lsp4j")
        exclude("org.eclipse.lsp4j", "org.eclipse.lsp4j.jsonrpc")
        exclude("com.google.code.gson", "gson")
    }
    compileOnly("org.eclipse.lsp4j:org.eclipse.lsp4j:$lsp4jVersion")
}
configurations[Constants.Configurations.INTELLIJ_PLATFORM_BUNDLED_PLUGINS].dependencies.configureEach {
    if (this is ExternalModuleDependency) {
        this.isTransitive = false
    }
}