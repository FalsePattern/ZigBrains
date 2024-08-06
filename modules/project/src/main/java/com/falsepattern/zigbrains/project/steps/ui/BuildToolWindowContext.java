package com.falsepattern.zigbrains.project.steps.ui;

import com.falsepattern.zigbrains.ZigBundle;
import com.falsepattern.zigbrains.project.execution.build.ConfigTypeBuild;
import com.falsepattern.zigbrains.project.execution.build.ZigExecConfigBuild;
import com.falsepattern.zigbrains.project.steps.discovery.ZigStepDiscoveryListener;
import com.falsepattern.zigbrains.zig.Icons;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.Tree;
import kotlin.Pair;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class BuildToolWindowContext implements Disposable {
    private static final Key<JBScrollPane> VIEWPORT = Key.create("MODEL");

    public final DefaultMutableTreeNode rootNode;
    private final DefaultMutableTreeNode buildZig;

    private final Project project;

    public static void create(Project project, ToolWindow window) {
        val context = new BuildToolWindowContext(project);
        project.getMessageBus().connect(context).subscribe(ZigStepDiscoveryListener.TOPIC, context.new BuildReloadListener());
        Disposer.register(window.getDisposable(), context);
        window.getContentManager().addContent(context.createContentPanel());
    }

    private class BuildReloadListener implements ZigStepDiscoveryListener {
        @Override
        public void preReload() {
            val viewport = getViewport(project);
            if (viewport == null)
                return;
            setViewportLoading(viewport);
        }

        @Override
        public void postReload(List<Pair<String, String>> steps) {
            buildZig.removeAllChildren();
            for (val step : steps) {
                val icon = switch (step.component1()) {
                    case "install" -> AllIcons.Actions.Install;
                    case "uninstall" -> AllIcons.Actions.Uninstall;
                    default -> AllIcons.RunConfigurations.TestState.Run;
                };
                buildZig.add(new DefaultMutableTreeNode(new StepNodeDescriptor(project, step.component1(), step.component2(), icon)));
            }
            invokeLaterWithViewport(BuildToolWindowContext.this::setViewportTree);
        }

        @Override
        public void errorReload(ErrorType errorType) {
            invokeLaterWithViewport(viewport -> {
                setViewportError(viewport, ZigBundle.message(switch (errorType) {
                    case MissingToolchain -> "build.tool.window.status.error.missing-toolchain";
                    case MissingBuildZig -> "build.tool.window.status.error.missing-build-zig";
                    case GeneralError -> "build.tool.window.status.error.general";
                }));
            });
        }

        @Override
        public void timeoutReload(int seconds) {
            invokeLaterWithViewport(viewport -> {
                setViewportError(viewport, ZigBundle.message("build.tool.window.status.timeout", seconds));
            });
        }
    }

    private void invokeLaterWithViewport(Consumer<JBScrollPane> callback) {
        ToolWindowManager.getInstance(project).invokeLater(() -> {
            val viewport = getViewport(project);
            if (viewport == null)
                return;
            callback.accept(viewport);
        });
    }

    public BuildToolWindowContext(Project project) {
        this.project = project;
        rootNode = new DefaultMutableTreeNode(new BaseNodeDescriptor<>(project, project.getName(), AllIcons.Actions.ProjectDirectory));
        buildZig = new DefaultMutableTreeNode(new BaseNodeDescriptor<>(project, ZigBundle.message("build.tool.window.tree.steps.label"), Icons.ZIG));
        rootNode.add(buildZig);
    }

    private static @Nullable JBScrollPane getViewport(Project project) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("zigbrains.build");
        if (toolWindow == null)
            return null;
        val cm = toolWindow.getContentManager();
        val content = cm.getContent(0);
        if (content == null)
            return null;
        return content.getUserData(VIEWPORT);
    }

    private static @Nullable RunnerAndConfigurationSettings getExistingRunConfig(RunManager manager, String stepName) {
        for (val config : manager.getConfigurationSettingsList(ConfigTypeBuild.class)) {
            if (!(config.getConfiguration() instanceof ZigExecConfigBuild build))
                continue;
            val steps = build.getBuildSteps().args;
            if (steps == null || steps.length != 1)
                continue;
            if (!(Objects.equals(steps[0], stepName)))
                continue;
            return config;
        }
        return null;
    }

    private void setViewportTree(JBScrollPane viewport) {
        val model = new DefaultTreeModel(rootNode);
        val tree = new Tree(model);
        tree.expandPath(new TreePath(model.getPathToRoot(buildZig)));
        viewport.setViewportView(tree);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    val node = tree.getLastSelectedPathComponent();
                    if (node == null)
                        return;
                    if (!(node instanceof DefaultMutableTreeNode mut))
                        return;
                    if (!(mut.getUserObject() instanceof StepNodeDescriptor step))
                        return;

                    val stepName = step.getElement().name();
                    val manager = RunManager.getInstance(project);
                    val config = Objects.requireNonNullElseGet(getExistingRunConfig(manager, stepName), () -> {
                        val factory = ConfigTypeBuild.getInstance().getConfigurationFactories()[0];
                        val newConfig = manager.createConfiguration("zig build " + stepName, factory);
                        ((ZigExecConfigBuild)newConfig.getConfiguration()).getBuildSteps().args = new String[]{stepName};
                        manager.addConfiguration(newConfig);
                        return newConfig;
                    });

                    manager.setSelectedConfiguration(config);
                    ProgramRunnerUtil.executeConfiguration(config, DefaultRunExecutor.getRunExecutorInstance());
                }
            }
        });
    }

    private void setViewportLoading(JBScrollPane viewport) {
        viewport.setViewportView(new JBLabel(ZigBundle.message("build.tool.window.status.loading"), new AnimatedIcon.Default(), SwingConstants.CENTER));
    }

    private void setViewportNoContent(JBScrollPane viewport) {
        viewport.setViewportView(new JBLabel(ZigBundle.message("build.tool.window.status.not-scanned"), AllIcons.General.Information, SwingConstants.CENTER));
    }

    private void setViewportError(JBScrollPane viewport, String msg) {
        viewport.setViewportView(new JBLabel(msg, AllIcons.General.Error, SwingConstants.CENTER));
    }

    private Content createContentPanel() {
        val contentPanel = new SimpleToolWindowPanel(false);
        val body = new JPanel(new GridBagLayout());
        contentPanel.setLayout(new GridBagLayout());
        val c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        contentPanel.add(body, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        c.weighty = 0;
        val toolbar = ActionManager.getInstance().createActionToolbar("ZigToolbar", new DefaultActionGroup(ActionManager.getInstance().getAction("ZigBrains.Reload")), true);
        toolbar.setTargetComponent(null);
        body.add(toolbar.getComponent(), c);
        c.gridwidth = 1;
        c.gridy = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        val viewport = new JBScrollPane();
        setViewportNoContent(viewport);
        body.add(viewport, c);
        val content = ContentFactory.getInstance().createContent(contentPanel, "", false);
        content.putUserData(VIEWPORT, viewport);
        return content;
    }

    @Override
    public void dispose() {

    }
}
