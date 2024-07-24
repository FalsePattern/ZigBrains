import de.undercouch.gradle.tasks.download.Download
import groovy.xml.XmlParser
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.platform.gradle.tasks.PublishPluginTask
import org.jetbrains.intellij.platform.gradle.utils.extensionProvider

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    java
    `maven-publish`
    `java-library`
    id("org.jetbrains.intellij.platform") version("2.0.0-rc1")
    id("org.jetbrains.changelog") version("2.2.1")
    id("org.jetbrains.grammarkit") version("2022.3.2.2")
    id("de.undercouch.download") version("5.6.0")
}

val publishVersions = listOf("232", "233", "241", "242")

val gitVersion: groovy.lang.Closure<String> by extra

val grammarKitGenDir = "build/generated/sources/grammarkit/java"
val rootPackage = "com.falsepattern.zigbrains"

val rootPackagePath = rootPackage.replace('.', '/')

// Keep these in sync with whatever the oldest IDE version we're targeting in gradle.properties needs
val javaLangVersion: JavaLanguageVersion = JavaLanguageVersion.of(21)
val javaVersion = JavaVersion.VERSION_21

val baseIDE = properties("baseIDE").get()
val ideaVersion = properties("ideaVersion").get()
val clionVersion = properties("clionVersion").get()

val clionPlugins = listOf("com.intellij.clion", "com.intellij.cidr.lang", "com.intellij.cidr.base", "com.intellij.nativeDebug")

val lsp4jVersion = "0.21.1"
val lsp4ijVersion = "0.3.0"

val lsp4ijNightly = lsp4ijVersion.contains("-")
val lsp4ijDepString = "${if (lsp4ijNightly) "nightly." else ""}com.jetbrains.plugins:com.redhat.devtools.lsp4ij:$lsp4ijVersion"
val lsp4ijPluginString = "com.redhat.devtools.lsp4ij:$lsp4ijVersion${if (lsp4ijNightly) "@nightly" else ""}"

val lsp4ijDep: DependencyHandler.() -> Unit = {
    intellijPlatformPluginDependency(lsp4ijDepString)
    compileOnlyApi(lsp4ijDepString)
    compileOnlyApi("org.eclipse.lsp4j:org.eclipse.lsp4j:$lsp4jVersion")
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }
}

fun pluginVersion(): Provider<String> {
    return provider {
        System.getenv("RELEASE_VERSION")
    }.orElse(properties("pluginVersion"))
}

fun pluginVersionFull(): Provider<String> {
    return pluginVersion().map { it + "-" + properties("pluginSinceBuild").get() }
}

allprojects {
    apply {
        plugin("org.jetbrains.intellij.platform")
    }
    repositories {
        mavenCentral()
        intellijPlatform {
            localPlatformArtifacts {
                content {
                    includeGroup("bundledPlugin")
                }
            }
            marketplace {
                content {
                    includeGroup("com.jetbrains.plugins")
                    includeGroup("nightly.com.jetbrains.plugins")
                }
            }
            snapshots {
                content {
                    includeModule("com.jetbrains.intellij.clion", "clion")
                    includeModule("com.jetbrains.intellij.idea", "ideaIC")
                    includeModule("com.jetbrains.intellij.idea", "ideaIU")
                }
            }
        }
    }
    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.32")
        annotationProcessor("org.projectlombok:lombok:1.18.32")
        if (path !in listOf(":", ":plugin", ":debugger")) {
            intellijPlatform {
                intellijIdeaCommunity(ideaVersion, useInstaller = false)
            }
        }
    }

    if (path in listOf(":zig", ":zon")) {
        apply {
            plugin("org.jetbrains.grammarkit")
        }
        sourceSets {
            main {
                java {
                    srcDirs(
                        "${grammarKitGenDir}/lexer",
                        "${grammarKitGenDir}/parser"
                    )
                }
            }
        }
        tasks {

            generateLexer {
                enabled = true
                purgeOldFiles = true
            }

            generateParser {
                enabled = true
                targetRootOutputDir = file("${grammarKitGenDir}/parser")
            }


            register<DefaultTask>("generateGrammars") {
                description = "Generate source code from parser/lexer definitions"
                group = "build setup"
                dependsOn("generateLexer")
                dependsOn("generateParser")
            }

            compileJava {
                dependsOn("generateGrammars")
            }
        }
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(javaLangVersion)
            vendor = JvmVendorSpec.JETBRAINS
        }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    tasks.withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }


    group = properties("pluginGroup").get()
    version = pluginVersionFull().get()

    tasks {
        runIde { enabled = false }
        prepareSandbox { enabled = false }
        buildSearchableOptions { enabled = false }
        verifyPlugin { enabled = false }
        buildPlugin { enabled = false }
        signPlugin { enabled = false }
        verifyPluginProjectConfiguration { enabled = false }

        withType<PatchPluginXmlTask> {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }
    }
    intellijPlatform {
        instrumentCode = false
    }
}

project(":common") {

}

project(":zig") {
    apply {
        plugin("java-library")
    }
    dependencies {
        implementation(project(":common"))
        lsp4ijDep()
        intellijPlatform {
            plugin(lsp4ijPluginString)
        }
    }
    tasks {
        generateLexer {
            sourceFile = file("src/main/grammar/Zig.flex")
            targetOutputDir = file("${grammarKitGenDir}/lexer/${rootPackagePath}/zig/lexer")
        }

        generateParser {
            sourceFile = file("src/main/grammar/Zig.bnf")
            pathToParser = "${rootPackagePath}/zig/psi/ZigParser.java"
            pathToPsiRoot = "${rootPackagePath}/zig/psi"
        }
    }
}

project(":project") {
    dependencies {
        implementation(project(":common"))
        implementation(project(":zig"))
    }
}

project(":debugger") {
    dependencies {
        implementation(project(":zig"))
        implementation(project(":project"))
        implementation(project(":common"))
        implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.debug:$lsp4jVersion") {
            exclude("org.eclipse.lsp4j", "org.eclipse.lsp4j")
            exclude("org.eclipse.lsp4j", "org.eclipse.lsp4j.jsonrpc")
            exclude("com.google.code.gson", "gson")
        }
        intellijPlatform {
            clion(clionVersion, useInstaller = false)
            for (p in clionPlugins) {
                bundledPlugin(p)
            }
        }
    }

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
}

project(":zon") {
    dependencies {
        implementation(project(":common"))
    }
    tasks {
        generateLexer {
            sourceFile = file("src/main/grammar/Zon.flex")
            targetOutputDir = file("${grammarKitGenDir}/lexer/${rootPackagePath}/zon/lexer")
        }

        generateParser {
            sourceFile = file("src/main/grammar/Zon.bnf")
            pathToParser = "${rootPackagePath}/zon/psi/ZonParser.java"
            pathToPsiRoot = "${rootPackagePath}/zon/psi"
        }
    }
}

project(":plugin") {
    dependencies {
        implementation(project(":common"))
        implementation(project(":zig"))
        implementation(project(":project"))
        implementation(project(":zon"))
        implementation(project(":debugger"))
        intellijPlatform {
            zipSigner()
            pluginVerifier()
            when (baseIDE) {
                "idea" -> intellijIdeaCommunity(ideaVersion, useInstaller = false)
                "clion" -> clion(clionVersion, useInstaller = false)
            }
            plugin(lsp4ijPluginString)
        }
    }

    intellijPlatform {
        projectName = "ZigBrains"
        pluginConfiguration {
            name = properties("pluginName")
            description = providers.fileContents(rootProject.layout.projectDirectory.file("README.md")).asText.map {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                with(it.lines()) {
                    if (!containsAll(listOf(start, end))) {
                        throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                    }
                    subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
                }
            }
            changeNotes = pluginVersion().map { pluginVersion ->
                with(rootProject.changelog) {
                    renderItem(
                        (getOrNull(pluginVersion) ?: getUnreleased())
                            .withHeader(false)
                            .withEmptySections(false),
                        Changelog.OutputType.HTML,
                    )
                }
            }
            version = pluginVersionFull()
        }
        signing {
            certificateChainFile = rootProject.file("secrets/chain.crt")
            privateKeyFile = rootProject.file("secrets/private.pem")
            password = environment("PRIVATE_KEY_PASSWORD")
        }
        verifyPlugin {
            ides {
                ide(IntelliJPlatformType.IntellijIdeaCommunity, ideaVersion)
                ide(IntelliJPlatformType.IntellijIdeaUltimate, ideaVersion)
                ide(IntelliJPlatformType.CLion, clionVersion)
            }
        }
    }

    tasks {
        runIde {
            enabled = true
        }

        prepareSandbox {
            enabled = true
        }

        verifyPlugin {
            enabled = true
        }

        verifyPluginProjectConfiguration {
            enabled = true
        }

        signPlugin {
            enabled = true
        }

        verifyPluginSignature {
            dependsOn(signPlugin)
        }

        buildPlugin {
            enabled = true
        }
    }
}

dependencies {
    intellijPlatform {
        when (baseIDE) {
            "idea" -> intellijIdeaCommunity(ideaVersion, useInstaller = false)
            "clion" -> clion(clionVersion, useInstaller = false)
        }
    }
}

tasks {
    generateLexer {
        enabled = false
    }
    generateParser {
        enabled = false
    }
}

fun distFile(it: String) = layout.buildDirectory.file("dist/ZigBrains-${pluginVersion().get()}-$it-signed.zip")

publishVersions.forEach {
    tasks.register<PublishPluginTask>("jbpublish-$it").configure {
        archiveFile = distFile(it)
        token = environment("IJ_PUBLISH_TOKEN")
        channels = if (pluginVersion().get().contains("-")) listOf("nightly") else listOf("default")
    }
    tasks.named("publish").configure {
        dependsOn("jbpublish-$it")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.falsepattern"
            artifactId = "zigbrains"
            version = pluginVersion().get()

            publishVersions.forEach {
                artifact(distFile(it)) {
                    classifier = "$it-signed"
                    extension = "zip"
                }
            }
        }
    }
    repositories {
        maven {
            name = "mavenpattern"
            url = uri("https://mvn.falsepattern.com/releases/");
            credentials {
                username = System.getenv("MAVEN_DEPLOY_USER")
                password = System.getenv("MAVEN_DEPLOY_PASSWORD")
            }
        }
    }
}

fun File.isPluginJar(): Boolean {
    if (!isFile) return false
    if (extension != "jar") return false
    return zipTree(this).files.any { it.isManifestFile() }
}

fun File.isManifestFile(): Boolean {
    if (extension != "xml") return false
    val rootNode = try {
        val parser = XmlParser()
        parser.parse(this)
    } catch (e: Exception) {
        logger.error("Failed to parse $path", e)
        return false
    }
    return rootNode.name() == "idea-plugin"
}
