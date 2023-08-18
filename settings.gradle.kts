rootProject.name = "ZigBrains"

include("plugin")

File(rootDir, "modules").eachDir { dir ->
    include(dir.name)
    project(":${dir.name}").projectDir = dir
}

fun File.eachDir(block: (File) -> Unit) {
    listFiles()?.filter { it.isDirectory }?.forEach { block(it) }
}