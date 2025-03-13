package com.falsepattern.zigbrains.project

import com.falsepattern.zigbrains.Icons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import javax.swing.Icon

class ZigProjectOpenProcessor: ProjectOpenProcessor() {
    override val icon: Icon get() = Icons.Zig
    override val name: String get() = "Zig"

    // check if we can open a file/folder as a Zig project
    override fun canOpenProject(file: VirtualFile): Boolean =
        FileUtil.namesEqual(file.name, "build.zig") ||
        FileUtil.namesEqual(file.name, "build.zig.zon") ||
        file.isDirectory && (file.findChild("build.zig") != null || file.findChild("build.zig.zon") != null)

    override fun doOpenProject(
        virtualFile: VirtualFile,
        projectToClose: Project?,
        forceOpenInNewFrame: Boolean
    ): Project? {
        val basedir = if (virtualFile.isDirectory) virtualFile else virtualFile.parent

        return PlatformProjectOpenProcessor.getInstance().doOpenProject(basedir, projectToClose, forceOpenInNewFrame)
    }
}