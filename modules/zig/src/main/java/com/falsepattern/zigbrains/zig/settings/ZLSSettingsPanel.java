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

package com.falsepattern.zigbrains.zig.settings;

import com.falsepattern.zigbrains.common.direnv.DirenvCmd;
import com.falsepattern.zigbrains.common.util.FileUtil;
import com.falsepattern.zigbrains.common.util.TextFieldUtil;
import com.falsepattern.zigbrains.common.util.dsl.JavaPanel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.falsepattern.zigbrains.common.util.KtUtil.$f;

public class ZLSSettingsPanel implements Disposable {
    private final @Nullable Project project;

    private final TextFieldWithBrowseButton zlsPath = TextFieldUtil.pathToFileTextField(this,
                                                                                        "Path to the ZLS Binary",
                                                                                        () -> {});
    private final TextFieldWithBrowseButton zlsConfigPath = TextFieldUtil.pathToFileTextField(this,
                                                                                              "Path to the Custom ZLS Config File (Optional)",
                                                                                              () -> {});

    private final JBCheckBox buildOnSave = new JBCheckBox();
    private final JBTextField buildOnSaveStep = new ExtendableTextField();
    private final JBCheckBox highlightGlobalVarDeclarations = new JBCheckBox();
    private final JBCheckBox dangerousComptimeExperimentsDoNotEnable = new JBCheckBox();
    private final JBCheckBox inlayHints = new JBCheckBox();
    private final JBCheckBox inlayHintsCompact = new JBCheckBox();

    private final JBCheckBox messageTrace = new JBCheckBox();
    private final JBCheckBox debug = new JBCheckBox();
    private final JBCheckBox direnv = new JBCheckBox("Use direnv");

    {
        buildOnSave.setToolTipText("Whether to enable build-on-save diagnostics");
        buildOnSaveStep.setToolTipText("Select which step should be executed on build-on-save");
        highlightGlobalVarDeclarations.setToolTipText("Whether to highlight global var declarations");
        dangerousComptimeExperimentsDoNotEnable.setToolTipText("Whether to use the comptime interpreter");
        inlayHints.setToolTipText("Enable inlay hints");
        inlayHintsCompact.setToolTipText("Replace extremely long error{...} snippets in inlay hints with a placeholder");
    }

    private void autodetect(ActionEvent e) {
        autodetect();
    }

    private @Nullable Path tryWorkDirFromProject() {
        if (project == null)
            return null;
        val projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir == null) {
            return null;
        }
        return projectDir.toNioPath();
    }

    private @NotNull CompletableFuture<Map<String, String>> tryGetDirenv() {
        if (!DirenvCmd.direnvInstalled() || !direnv.isSelected()) {
            return CompletableFuture.completedFuture(Map.of());
        }
        val workDir = tryWorkDirFromProject();
        if (workDir == null)
            return CompletableFuture.completedFuture(Map.of());
        val direnvCmd = new DirenvCmd(workDir);
        return direnvCmd.importDirenvAsync();
    }

    public void autodetect() {
        tryGetDirenv().thenAcceptAsync((env) -> {
            FileUtil.findExecutableOnPATH(env, "zls").map(Path::toString).ifPresent(zlsPath::setText);
        }, AppExecutorUtil.getAppExecutorService());
    }

    public ZLSSettingsPanel(@Nullable Project project) {
        zlsPath.addBrowseFolderListener(new TextBrowseFolderListener(new FileChooserDescriptor(true, false, false, false, false, false)));
        this.project = project;
    }

    public void attachPanelTo(JavaPanel panel) {
        Optional.ofNullable(ZLSProjectSettingsService.getInstance(ProjectManager.getInstance().getDefaultProject()))
                .map(ZLSProjectSettingsService::getState)
                .ifPresent(this::setData);
        panel.group("ZLS Settings", true, p -> {
            p.row("Executable path", r -> {
                r.cell(zlsPath).resizableColumn().align(AlignX.FILL);
                if (DirenvCmd.direnvInstalled() && project != null) {
                    r.cell(direnv);
                }
                r.button("Autodetect", $f(this::autodetect));
            });
            p.cell("Config path (leave empty to use built-in config)", zlsConfigPath, AlignX.FILL);
            p.cell("Inlay hints", inlayHints);
            p.cell("Compact errors in inlay hint", inlayHintsCompact);
            p.cell("Build on save", buildOnSave);
            p.row("Build on save step", r -> {
                r.cell(buildOnSaveStep).resizableColumn().align(AlignX.FILL);
            });
            p.cell("Highlight global variable declarations", highlightGlobalVarDeclarations);
            p.cell("Dangerous comptime experiments (do not enable)", dangerousComptimeExperimentsDoNotEnable);
        });
        panel.group("ZLS Developer settings", false, p -> {
            p.cell("Debug log", debug);
            p.cell("Message trace", messageTrace);
        });
    }

    public ZLSSettings getData() {
        return new ZLSSettings(direnv.isSelected(),
                               zlsPath.getText(),
                               zlsConfigPath.getText(),
                               debug.isSelected(),
                               messageTrace.isSelected(),
                               buildOnSave.isSelected(),
                               buildOnSaveStep.getText(),
                               highlightGlobalVarDeclarations.isSelected(),
                               dangerousComptimeExperimentsDoNotEnable.isSelected(),
                               inlayHints.isSelected(),
                               inlayHintsCompact.isSelected());
    }

    public void setData(ZLSSettings value) {
        direnv.setSelected(value.direnv);
        zlsPath.setText(value.zlsPath == null ? "" : value.zlsPath);
        zlsConfigPath.setText(value.zlsConfigPath);
        debug.setSelected(value.debug);
        messageTrace.setSelected(value.messageTrace);
        buildOnSave.setSelected(value.buildOnSave);
        buildOnSaveStep.setText(value.buildOnSaveStep);
        highlightGlobalVarDeclarations.setSelected(value.highlightGlobalVarDeclarations);
        dangerousComptimeExperimentsDoNotEnable.setSelected(value.dangerousComptimeExperimentsDoNotEnable);
        inlayHints.setSelected(value.inlayHints);
        inlayHintsCompact.setSelected(value.inlayHintsCompact);
    }

    @Override
    public void dispose() {
        zlsPath.dispose();
        zlsConfigPath.dispose();
    }
}
