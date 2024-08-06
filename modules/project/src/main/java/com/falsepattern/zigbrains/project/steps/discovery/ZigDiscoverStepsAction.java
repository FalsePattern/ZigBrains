package com.falsepattern.zigbrains.project.steps.discovery;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import lombok.val;
import org.jetbrains.annotations.NotNull;

public class ZigDiscoverStepsAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        val project = e.getProject();
        if (project == null) return;
        ZigStepDiscoveryService.getInstance(project).triggerReload();
    }
}
