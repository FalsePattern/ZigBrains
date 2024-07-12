plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}
rootProject.name = "ZigBrains"

File(rootDir, "modules").eachDir { dir ->
    if (dir.resolve("src").exists()) {
        include(dir.name)
        project(":${dir.name}").projectDir = dir
    }
}

fun File.eachDir(block: (File) -> Unit) {
    listFiles()?.filter { it.isDirectory }?.forEach { block(it) }
}