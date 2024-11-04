import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("org.jetbrains.grammarkit")
    kotlin("plugin.serialization")
}

val lsp4ijVersion: String by project
val lsp4jVersion: String by project

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, providers.gradleProperty("ideaCommunityVersion"))
    }
    compileOnly("com.redhat.devtools.intellij:lsp4ij:$lsp4ijVersion")
    compileOnly("org.eclipse.lsp4j:org.eclipse.lsp4j:$lsp4jVersion")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
}

//region grammars
run {
    val grammarGenRoot = layout.buildDirectory.dir("generated/sources/grammarkit")
    val rootPackagePath = "com/falsepattern/zigbrains"
    val grammarSources = layout.projectDirectory.dir("src/main/grammar")

    val parserDir = grammarGenRoot.map {it.dir("zig/parser")}
    val lexerDir = grammarGenRoot.map {it.dir("zig/lexer")}
    val lexerStringDir = grammarGenRoot.map {it.dir("zig/lexerstring")}
    val zonParserDir = grammarGenRoot.map {it.dir("zon/parser")}
    val zonLexerDir = grammarGenRoot.map {it.dir("zon/lexer")}

    val grammarGenDirs = listOf(parserDir, lexerDir, lexerStringDir, zonParserDir, zonLexerDir)

    sourceSets {
        main {
            java {
                grammarGenDirs.forEach { srcDir(it) }
            }
        }
    }

    idea {
        module {
            grammarGenDirs.forEach {
                val file = it.get().asFile
                sourceDirs.add(file)
                generatedSourceDirs.add(file)
            }
            sourceDirs.add(grammarSources.asFile)
        }
    }

    tasks {
        // region grammarkit
        generateLexer {
            purgeOldFiles = true
            sourceFile = grammarSources.file("Zig.flex")
            targetOutputDir = lexerDir.map { it.dir("$rootPackagePath/zig/lexer") }
        }

        register<GenerateLexerTask>("generateLexerString") {
            purgeOldFiles = true
            sourceFile = grammarSources.file("ZigString.flex")
            targetOutputDir = lexerStringDir.map { it.dir("$rootPackagePath/zig/lexerstring") }

        }

        generateParser {
            purgeOldFiles = true
            sourceFile = grammarSources.file("Zig.bnf")
            targetRootOutputDir = parserDir
            pathToParser = "$rootPackagePath/zig/parser/ZigParser.java"
            pathToPsiRoot = "$rootPackagePath/zig/psi"
        }

        register<GenerateLexerTask>("generateZonLexer") {
            purgeOldFiles = true
            sourceFile = grammarSources.file("Zon.flex")
            targetOutputDir = zonLexerDir.map { it.dir("$rootPackagePath/zon/lexer") }
        }

        register<GenerateParserTask>("generateZonParser") {
            purgeOldFiles = true
            sourceFile = grammarSources.file("Zon.bnf")
            targetRootOutputDir = zonParserDir
            pathToParser = "$rootPackagePath/zon/parser/ZonParser.java"
            pathToPsiRoot = "$rootPackagePath/zon/psi"
        }

        register<DefaultTask>("generateGrammars") {
            group = "grammarkit"
            dependsOn("generateLexer", "generateLexerString", "generateParser")
            dependsOn("generateZonLexer", "generateZonParser")
        }

        compileJava {
            dependsOn("generateGrammars")
        }

        compileKotlin {
            dependsOn("generateGrammars")
        }
    }
}
//endregion grammars
