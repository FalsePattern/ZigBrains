import groovy.xml.XmlParser
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.RunPluginVerifierTask

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java") // Java support
    id("java-library")
    id("org.jetbrains.intellij") version("1.17.0")
    id("org.jetbrains.changelog") version("2.2.0")
    id("org.jetbrains.grammarkit") version("2022.3.2.1")
}

val grammarKitGenDir = "build/generated/sources/grammarkit/java"
val rootPackage = "com.falsepattern.zigbrains"

val rootPackagePath = rootPackage.replace('.', '/')

// Keep these in sync with whatever the oldest IDE version we're targeting in gradle.properties needs
val javaLangVersion: JavaLanguageVersion? = JavaLanguageVersion.of(17)
val javaVersion = JavaVersion.VERSION_17

val baseIDE: String = properties("baseIDE").get()
val ideaVersion: String = properties("ideaVersion").get()
val clionVersion: String = properties("clionVersion").get()
val baseVersion = when(baseIDE) {
    "idea" -> ideaVersion
    "clion" -> clionVersion
    else -> error("Unexpected IDE name: `$baseIDE")
}

val clionPlugins = listOf("com.intellij.clion", "com.intellij.cidr.lang", "com.intellij.cidr.base", "com.intellij.nativeDebug")

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }
}

allprojects {
    apply {
        plugin("org.jetbrains.grammarkit")
        plugin("org.jetbrains.intellij")
    }
    repositories {
        mavenCentral()
        maven("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    }
    dependencies {
        compileOnly("org.jetbrains:annotations:24.1.0")

        compileOnly("org.projectlombok:lombok:1.18.30")
        annotationProcessor("org.projectlombok:lombok:1.18.30")
    }
    intellij {
        version = baseVersion
        updateSinceUntilBuild = true
        instrumentCode = false
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

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(javaLangVersion)
        }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    group = properties("pluginGroup").get()
    version = properties("pluginVersion").get()

    tasks {
        runIde { enabled = false }
        prepareSandbox { enabled = false }
        buildSearchableOptions { enabled = false }

        withType<PatchPluginXmlTask> {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }

        withType<org.jetbrains.intellij.tasks.RunIdeBase> {
            rootProject.file("jbr/lib/openjdk/bin/java")
                .takeIf { it.exists() }
                ?.let { projectExecutable.set(it.toString()) }
        }

        withType<org.jetbrains.intellij.tasks.RunPluginVerifierTask> {
            rootProject.file("jbr/lib/openjdk")
                .takeIf { it.exists() }
                ?.let { runtimeDir.set(it.toString()) }
        }

        generateLexer {
            purgeOldFiles = true
        }
        generateParser {
            targetRoot = "${grammarKitGenDir}/parser"
            purgeOldFiles = true
        }

        register<DefaultTask>("generateGrammars") {
            description = "Generate source code from parser/lexer definitions"
            group = "build setup"
            dependsOn("generateLexer")
            dependsOn("generateParser")
        }

        verifyPlugin {
            enabled = false
        }
    }
}

project(":") {
    apply {
        plugin("org.jetbrains.changelog")
    }
    task<Exec>("nixos_jbr") {
        description = "Create a symlink to package jetbrains.jdk"
        group = "build setup"
        commandLine("nix-build", "<nixpkgs>", "-A", "jetbrains.jdk", "-o", "jbr")
    }

    tasks {
        buildPlugin {
            enabled = false
        }
    }

    changelog {
        groups.empty()
        repositoryUrl = properties("pluginRepositoryUrl")
    }
}

project(":debugger") {
    dependencies {
        implementation(project(":zig"))
        implementation(project(":project"))
    }
    intellij {
        version = clionVersion
        plugins = clionPlugins
    }
}

project(":lsp") {
    apply {
        plugin("java-library")
    }
    dependencies {
        api("org.eclipse.lsp4j:org.eclipse.lsp4j:0.21.2")
        implementation("com.vladsch.flexmark:flexmark:0.64.8")
        api("org.apache.commons:commons-lang3:3.14.0")
    }
}

project(":zig") {
    dependencies {
        implementation(project(":lsp"))
        implementation(project(":common"))
    }
    tasks {
        generateLexer {
            enabled = true
            sourceFile = file("src/main/grammar/Zig.flex")
            targetDir = "${grammarKitGenDir}/lexer/${rootPackagePath}/zig/lexer"
            targetClass = "ZigFlexLexer"
        }

        generateParser {
            enabled = true
            sourceFile = file("src/main/grammar/Zig.bnf")
            pathToParser = "${rootPackagePath}/zig/psi/ZigParser.java"
            pathToPsiRoot = "${rootPackagePath}/zig/psi"
        }

        compileJava {
            dependsOn("generateGrammars")
        }

    }
}

project(":project") {
    dependencies {
        implementation(project(":common"))
        implementation(project(":zig"))
    }
}

project(":zon") {
    dependencies {
        implementation(project(":common"))
    }
    tasks {
        generateLexer {
            enabled = true
            sourceFile = file("src/main/grammar/Zon.flex")
            targetDir = "${grammarKitGenDir}/lexer/${rootPackagePath}/zon/lexer"
            targetClass = "ZonFlexLexer"
        }

        generateParser {
            enabled = true
            sourceFile = file("src/main/grammar/Zon.bnf")
            pathToParser = "${rootPackagePath}/zon/psi/ZonParser.java"
            pathToPsiRoot = "${rootPackagePath}/zon/psi"
        }

        compileJava {
            dependsOn("generateGrammars")
        }
    }
}

project(":plugin") {
    apply {
        plugin("org.jetbrains.changelog")
    }

    dependencies {
        implementation(project(":zig"))
        implementation(project(":project"))
        implementation(project(":zon"))
        implementation(project(":debugger"))
        implementation(project(":"))
    }

    intellij {
        pluginName = properties("pluginName")
    }

// Include the generated files in the source set

    // Collects all jars produced by compilation of project modules and merges them into singe one.
    // We need to put all plugin manifest files into single jar to make new plugin model work
    val mergePluginJarTask = task<Jar>("mergePluginJars") {
        duplicatesStrategy = DuplicatesStrategy.FAIL
        archiveBaseName.set("ZigBrains")

        exclude("META-INF/MANIFEST.MF")
        exclude("**/classpath.index")

        val pluginLibDir by lazy {
            val sandboxTask = tasks.prepareSandbox.get()
            sandboxTask.destinationDir.resolve("${sandboxTask.pluginName.get()}/lib")
        }

        val pluginJars by lazy {
            pluginLibDir.listFiles().orEmpty().filter { it.isPluginJar() }
        }

        destinationDirectory.set(project.layout.dir(provider { pluginLibDir }))

        doFirst {
            for (file in pluginJars) {
                from(zipTree(file))
            }
        }

        doLast {
            delete(pluginJars)
        }
    }

    tasks {

        buildPlugin {
            archiveBaseName.set("ZigBrains")
        }

        runIde {
            dependsOn(mergePluginJarTask)
            enabled = true
        }

        prepareSandbox {
            finalizedBy(mergePluginJarTask)
            enabled = true
        }

        buildSearchableOptions {
            dependsOn(mergePluginJarTask)
        }

        patchPluginXml {
            version = properties("pluginVersion")

            // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
            pluginDescription = providers.fileContents(rootProject.layout.projectDirectory.file("README.md")).asText.map {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                with (it.lines()) {
                    if (!containsAll(listOf(start, end))) {
                        throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                    }
                    subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
                }
            }

            val changelog = rootProject.changelog // local variable for configuration cache compatibility
            // Get the latest available change notes from the changelog file
            changeNotes = properties("pluginVersion").map { pluginVersion ->
                with(changelog) {
                    renderItem(
                        (getOrNull(pluginVersion) ?: getUnreleased())
                            .withHeader(false)
                            .withEmptySections(false),
                        Changelog.OutputType.HTML,
                    )
                }
            }
        }

        signPlugin {
            certificateChainFile = rootProject.file("secrets/chain.crt")
            privateKeyFile = rootProject.file("secrets/private.pem")
            password = environment("PRIVATE_KEY_PASSWORD")
        }

        verifyPluginSignature {
            certificateChainFile = rootProject.file("secrets/chain.crt")
        }

        verifyPlugin {
            dependsOn(mergePluginJarTask)
            enabled = true
        }

        listProductsReleases {
            types = listOf("IU", "IC", "CL")
        }

//    publishPlugin {
//        dependsOn("patchChangelog")
//        token = environment("PUBLISH_TOKEN")
//        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
//        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
//        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
//        channels = properties("pluginVersion").map { listOf(it.split('-').getOrElse(1) { "default" }.split('.').first()) }
//    }
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