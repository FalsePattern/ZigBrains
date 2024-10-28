import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, providers.gradleProperty("ideaCommunityVersion"))
    }
}