import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

val lsp4jVersion: String by project

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.CLion, providers.gradleProperty("clionVersion"))
        bundledPlugins("com.intellij.clion", "com.intellij.cidr.lang", "com.intellij.cidr.base", "com.intellij.nativeDebug")
    }
    implementation(project(":core"))
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.debug:$lsp4jVersion") {
        exclude("org.eclipse.lsp4j", "org.eclipse.lsp4j")
        exclude("org.eclipse.lsp4j", "org.eclipse.lsp4j.jsonrpc")
        exclude("com.google.code.gson", "gson")
    }
}