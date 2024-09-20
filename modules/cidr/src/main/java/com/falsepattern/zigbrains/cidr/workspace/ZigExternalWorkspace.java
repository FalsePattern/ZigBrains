package com.falsepattern.zigbrains.cidr.workspace;

import com.intellij.openapi.project.Project;
import com.jetbrains.cidr.external.system.workspace.ExternalWorkspace;
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment;
import com.jetbrains.cidr.toolchains.EnvironmentProblems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZigExternalWorkspace extends ExternalWorkspace {
    public ZigExternalWorkspace(@NotNull Project project) {
        super(project);
    }

    @Override
    public @Nullable CidrToolEnvironment createEnvironment(@Nullable Project project, @Nullable String s, @NotNull EnvironmentProblems environmentProblems, boolean b, @Nullable Runnable runnable) {
        return null;
    }

    @Override
    public @NotNull String getClientKey() {
        return "ZIG_WORKSPACE";
    }
}
