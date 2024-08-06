package com.falsepattern.zigbrains.project.steps.ui;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class StepNodeDescriptor extends BaseNodeDescriptor<StepDetails> {
    private final String stepName;
    public StepNodeDescriptor(@Nullable Project project, String stepName, String description, Icon displayIcon) {
        super(project, stepName, description, displayIcon);
        this.stepName = stepName;
    }

    @Override
    public StepDetails getElement() {
        return new StepDetails(stepName);
    }
}
