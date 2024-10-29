import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

val lsp4ijVersion: String by project
val lsp4jVersion: String by project
val lsp4ijNightly = property("lsp4ijNightly").toString().toBoolean()
val lsp4ijDepString = "${if (lsp4ijNightly) "nightly." else ""}com.jetbrains.plugins:com.redhat.devtools.lsp4ij:$lsp4ijVersion"

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, providers.gradleProperty("ideaCommunityVersion"))
    }
    intellijPlatformPluginDependency(lsp4ijDepString)
    compileOnly("org.eclipse.lsp4j:org.eclipse.lsp4j:$lsp4jVersion")
}
