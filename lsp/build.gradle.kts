import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

val lsp4ijVersion: String by project
val lsp4jVersion: String by project
val ideaCommunityVersion: String by project
val useInstaller = property("useInstaller").toString().toBoolean()

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, ideaCommunityVersion, useInstaller = useInstaller)
    }
    compileOnly("com.redhat.devtools.intellij:lsp4ij:$lsp4ijVersion")
    compileOnly("org.eclipse.lsp4j:org.eclipse.lsp4j:$lsp4jVersion")
    implementation(project(":core"))
}