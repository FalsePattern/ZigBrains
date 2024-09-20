package com.falsepattern.zigbrains.cidr.workspace;

import com.falsepattern.zigbrains.project.util.ProjectUtil;
import com.intellij.openapi.project.Project;
import com.jetbrains.cidr.project.workspace.CidrWorkspace;
import com.jetbrains.cidr.project.workspace.CidrWorkspaceManager;
import com.jetbrains.cidr.project.workspace.CidrWorkspaceProvider;
import lombok.Cleanup;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;

public class ZigWorkspaceProvider implements CidrWorkspaceProvider {
    private @Nullable ZigExternalWorkspace getExistingWorkspace(@NotNull Project project) {
        val workspaces = CidrWorkspaceManager.getInstance(project).getWorkspaces().keySet();
        for (val ws: workspaces) {
            if (ws instanceof ZigExternalWorkspace zew)
                return zew;
        }
        return null;
    }
    @Override
    public @Nullable CidrWorkspace getWorkspace(@NotNull Project project) {
        val existingWorkspace = getExistingWorkspace(project);
        if (existingWorkspace != null) {
            return existingWorkspace;
        }
        val projectDir = ProjectUtil.guessProjectDir(project);
        try {
            @Cleanup val files = Files.walk(projectDir);
            if (files.anyMatch(file -> file.getFileName().toString().endsWith(".zig") || file.getFileName().toString().endsWith(".zig.zon"))) {
                return new ZigExternalWorkspace(project);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void loadWorkspace(@NotNull Project project) {
        if (getExistingWorkspace(project) != null) {
            return;
        }
        val workspace = getWorkspace(project);
        if (workspace != null) {
            val manager = CidrWorkspaceManager.getInstance(project);
            manager.markInitializing(workspace);
            manager.markInitialized(workspace);
            manager.markLoading(workspace);
            manager.markLoaded(workspace);
        }
    }
}
