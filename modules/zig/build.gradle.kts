import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    kotlin("jvm")
    id("org.jetbrains.grammarkit")
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, providers.gradleProperty("ideaCommunityVersion"))
    }
}

val grammarGenRoot = "generated/sources/grammarkit/zig"
val rootPackagePath = "com/falsepattern/zigbrains/zig"

val parserDir = layout.buildDirectory.dir("$grammarGenRoot/parser")
val lexerDir = layout.buildDirectory.dir("$grammarGenRoot/lexer")
val lexerStringDir = layout.buildDirectory.dir("$grammarGenRoot/lexerstring")

sourceSets {
    main {
        java {
            srcDir(parserDir)
            srcDir(lexerDir)
            srcDir(lexerStringDir)
        }
    }
}

idea {
    module {
        sourceDirs.addAll(listOf(parserDir.get().asFile, lexerDir.get().asFile, lexerStringDir.get().asFile))
        generatedSourceDirs.addAll(listOf(parserDir.get().asFile, lexerDir.get().asFile, lexerStringDir.get().asFile))
    }
}

tasks {
    generateLexer {
        purgeOldFiles = true
        sourceFile = file("src/main/grammar/Zig.flex")
        targetOutputDir = layout.buildDirectory.dir("$grammarGenRoot/lexer/$rootPackagePath/lexer")
    }

    register<GenerateLexerTask>("generateLexerString") {
        purgeOldFiles = true
        sourceFile = file("src/main/grammar/ZigString.flex")
        targetOutputDir = layout.buildDirectory.dir("$grammarGenRoot/lexerstring/$rootPackagePath/lexerstring")

    }

    generateParser {
        purgeOldFiles = true
        sourceFile = file("src/main/grammar/Zig.bnf")
        targetRootOutputDir = layout.buildDirectory.dir("$grammarGenRoot/parser")
        pathToParser = "$rootPackagePath/psi/ZigParser.java"
        pathToPsiRoot = "$rootPackagePath/psi"
    }

    register<DefaultTask>("generateGrammars") {
        group = "grammarkit"
        dependsOn("generateLexer")
        dependsOn("generateLexerString")
        dependsOn("generateParser")
    }

    compileJava {
        dependsOn("generateGrammars")
    }

    compileKotlin {
        dependsOn("generateGrammars")
    }
}