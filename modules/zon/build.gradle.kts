import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("org.jetbrains.grammarkit")
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, providers.gradleProperty("ideaCommunityVersion"))
    }
}

val grammarGenRoot = "generated/sources/grammarkit/zon"
val rootPackagePath = "com/falsepattern/zigbrains/zon"

val parserDir = layout.buildDirectory.dir("$grammarGenRoot/parser")
val lexerDir = layout.buildDirectory.dir("$grammarGenRoot/lexer")

sourceSets {
    main {
        java {
            srcDir(parserDir)
            srcDir(lexerDir)
        }
    }
}

idea {
    module {
        sourceDirs.addAll(listOf(parserDir.get().asFile, lexerDir.get().asFile))
        generatedSourceDirs.addAll(listOf(parserDir.get().asFile, lexerDir.get().asFile))
    }
}

tasks {
    generateLexer {
        purgeOldFiles = true
        sourceFile = file("src/main/grammar/Zon.flex")
        targetOutputDir = layout.buildDirectory.dir("$grammarGenRoot/lexer/$rootPackagePath/lexer")
    }

    generateParser {
        purgeOldFiles = true
        sourceFile = file("src/main/grammar/Zon.bnf")
        targetRootOutputDir = layout.buildDirectory.dir("$grammarGenRoot/parser")
        pathToParser = "$rootPackagePath/psi/ZonParser.java"
        pathToPsiRoot = "$rootPackagePath/psi"
    }

    register<DefaultTask>("generateGrammars") {
        group = "grammarkit"
        dependsOn("generateLexer")
        dependsOn("generateParser")
    }

    compileJava {
        dependsOn("generateGrammars")
    }

    compileKotlin {
        dependsOn("generateGrammars")
    }
}