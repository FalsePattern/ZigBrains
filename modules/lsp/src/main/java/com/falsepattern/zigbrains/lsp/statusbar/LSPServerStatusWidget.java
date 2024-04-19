/*
 * Copyright 2023-2024 FalsePattern
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.falsepattern.zigbrains.lsp.statusbar;

import com.falsepattern.zigbrains.lsp.IntellijLanguageClient;
import com.falsepattern.zigbrains.lsp.client.languageserver.ServerStatus;
import com.falsepattern.zigbrains.lsp.client.languageserver.wrapper.LanguageServerWrapper;
import com.falsepattern.zigbrains.lsp.contributors.icon.LSPDefaultIconProvider;
import com.falsepattern.zigbrains.lsp.requests.Timeouts;
import com.falsepattern.zigbrains.lsp.utils.GUIUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import lombok.val;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LSPServerStatusWidget extends EditorBasedStatusBarPopup {

    private final Map<Timeouts, Pair<Integer, Integer>> timeouts = new HashMap<>();
    private final String projectName;
    private ServerStatus status = ServerStatus.NONEXISTENT;

    private static final List<WeakReference<LSPServerStatusWidget>> widgets = Collections.synchronizedList(new ArrayList<>());
    private static final ScheduledExecutorService refresher = Executors.newSingleThreadScheduledExecutor();
    static {
        refresher.scheduleWithFixedDelay(() -> {
            synchronized (widgets) {
                val iter = widgets.iterator();
                while (iter.hasNext()) {
                    val weakWidget = iter.next();
                    val widget = weakWidget.get();
                    if (widget == null) {
                        iter.remove();
                        continue;
                    }
                    widget.update();
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public Project project() {
        return super.getProject();
    }

    LSPServerStatusWidget(Project project) {
        super(project, false);
        this.projectName = project.getName();

        for (Timeouts t : Timeouts.values()) {
            timeouts.put(t, new MutablePair<>(0, 0));
        }
        widgets.add(new WeakReference<>(this));
    }

    public void notifyResult(Timeouts timeout, Boolean success) {
        Pair<Integer, Integer> oldValue = timeouts.get(timeout);
        if (success) {
            timeouts.replace(timeout, new MutablePair<>(oldValue.getKey() + 1, oldValue.getValue()));
        } else {
            timeouts.replace(timeout, new MutablePair<>(oldValue.getKey(), oldValue.getValue() + 1));
        }
    }

    /**
     * Sets the status of the server
     *
     * @param status The new status
     */
    public void setStatus(ServerStatus status) {
        this.status = status;
        update();
    }

    @NotNull
    @Override
    public String ID() {
        return "LSP";
    }

    @NotNull
    @Override
    protected StatusBarWidget createInstance(@NotNull Project project) {
        return new LSPServerStatusWidget(project);
    }

    private LSPServerStatusPanel component;

    @NotNull
    @Override
    protected JPanel createComponent() {
        component = new LSPServerStatusPanel();
        updateComponent();
        return component;
    }

    @Override
    protected void updateComponent(@NotNull EditorBasedStatusBarPopup.WidgetState state) {
        updateComponent();
    }

    private void updateComponent() {
        if (component != null) {
            component.setToolTipText(getTooltipText());
            component.setIcon(getIcon());
        }
    }

    @Nullable
    @Override
    public WidgetPresentation getPresentation() {
        return super.getPresentation();
    }

    @Nullable
    private Icon getIcon() {
        LanguageServerWrapper wrapper = LanguageServerWrapper.forProject(project());
        Map<ServerStatus, Icon> icons = new LSPDefaultIconProvider().getStatusIcons();
        if (wrapper != null) {
            icons = GUIUtils.getIconProviderFor(wrapper.getServerDefinition())
                            .getStatusIcons();
        }
        return icons.get(status);
    }

    private String getTooltipText() {
        LanguageServerWrapper wrapper = LanguageServerWrapper.forProject(project());
        if (wrapper == null) {
            return "Language server, project " + projectName;
        } else {
            return "Language server for extension " + wrapper.getServerDefinition().ext + ", project " + projectName;
        }
    }

    @Override
    protected boolean isEmpty() {
        return false;
    }

    @Nullable
    @Override
    protected ListPopup createPopup(@NotNull DataContext dataContext) {
        var wrapper = LanguageServerWrapper.forProject(project());
        if (wrapper == null) {
            return null;
        }
        List<AnAction> actions = new ArrayList<>();
        if (wrapper.getStatus() == ServerStatus.INITIALIZED) {
            actions.add(new ShowConnectedFiles());
        }
        actions.add(new ShowTimeouts());

        actions.add(new Restart());

        JBPopupFactory.ActionSelectionAid mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS;
        String title = "Server Actions";
        DefaultActionGroup group = new DefaultActionGroup(actions);
        return JBPopupFactory.getInstance()
                             .createActionGroupPopup(title, group, dataContext, mnemonics, true);
    }

    @NotNull
    @Override
    protected WidgetState getWidgetState(@Nullable VirtualFile virtualFile) {
        if (virtualFile == null) {
            return WidgetState.HIDDEN;
        }
        val manager = IntellijLanguageClient.getExtensionManagerFor(virtualFile.getExtension());
        if (manager != null) {
            val ws = new WidgetState(getTooltipText(), null, true);
            ws.setIcon(getIcon());
            return ws;
        } else {
            return WidgetState.HIDDEN;
        }
    }



    class ShowConnectedFiles extends AnAction implements DumbAware {
        ShowConnectedFiles() {
            super("&Show Connected Files", "Show the files connected to the server", null);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            var wrapper = LanguageServerWrapper.forProject(project());
            if (wrapper == null) {
                return;
            }
            StringBuilder connectedFiles = new StringBuilder("Connected files :");
            wrapper.getConnectedFiles().forEach(f -> connectedFiles.append(System.lineSeparator()).append(f));
            Messages.showInfoMessage(connectedFiles.toString(), "Connected Files");
        }
    }

    class ShowTimeouts extends AnAction implements DumbAware {
        ShowTimeouts() {
            super("&Show Timeouts", "Show the timeouts proportions of the server", null);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            StringBuilder message = new StringBuilder();
            message.append("<html>");
            message.append("Timeouts (failed requests) :<br>");
            timeouts.forEach((t, v) -> {
                int timeouts = v.getRight();
                message.append(t.name(), 0, 1).append(t.name().substring(1).toLowerCase()).append(" => ");
                int total = v.getLeft() + timeouts;
                if (total != 0) {
                    if (timeouts > 0) {
                        message.append("<font color=\"red\">");
                    }
                    message.append(timeouts).append("/").append(total).append(" (")
                           .append(100 * (double) timeouts / total).append("%)<br>");
                    if (timeouts > 0) {
                        message.append("</font>");
                    }
                } else {
                    message.append("0/0 (0%)<br>");
                }
            });
            message.append("</html>");
            Messages.showInfoMessage(message.toString(), "Timeouts");
        }
    }

    class Restart extends AnAction implements DumbAware {

        Restart() {
            super("&Restart", "Restarts the language server.", null);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            var wrapper = LanguageServerWrapper.forProject(project());
            if (wrapper != null) {
                wrapper.restart();
            }
        }
    }
    private class LSPServerStatusPanel extends JPanel {
        private final JLabel myIconLabel;

        LSPServerStatusPanel() {
            super();

            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            setAlignmentY(Component.CENTER_ALIGNMENT);
            myIconLabel = new JLabel("");

            add(myIconLabel);
        }

        public void setIcon(@Nullable Icon icon) {
            myIconLabel.setIcon(icon);
            myIconLabel.setVisible(icon != null);
        }
    }
}
