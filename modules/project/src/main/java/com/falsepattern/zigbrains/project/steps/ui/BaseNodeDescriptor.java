package com.falsepattern.zigbrains.project.steps.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class BaseNodeDescriptor<T> extends PresentableNodeDescriptor<T> {
    private String description;
    public BaseNodeDescriptor(@Nullable Project project, String displayName, Icon displayIcon) {
        this(project, displayName, null, displayIcon);
    }
    public BaseNodeDescriptor(@Nullable Project project, String displayName, String description, Icon displayIcon) {
        super(project, null);
        setIcon(displayIcon);
        myName = displayName;
        this.description = description;
        update();
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setIcon(getIcon());
        presentation.addText(myName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        presentation.setTooltip(description);
    }

    @Override
    public T getElement() {
        return null;
    }
}
