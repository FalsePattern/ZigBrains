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

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JPanel;

public class ZLSSettingsComponent {
    private final JPanel myMainPanel;
    private final TextFieldWithBrowseButton zlsPathText = new TextFieldWithBrowseButton();
    private final TextFieldWithBrowseButton zlsConfigPathText = new TextFieldWithBrowseButton();
    private final JBCheckBox asyncFoldingCheckBox = new JBCheckBox();
    private final JBCheckBox increaseTimeouts = new JBCheckBox();

    private final JBCheckBox buildOnSave = new JBCheckBox();
    private final JBTextField buildOnSaveStep = new JBTextField();
    private final JBCheckBox highlightGlobalVarDeclarations = new JBCheckBox();
    private final JBCheckBox dangerousComptimeExperimentsDoNotEnable = new JBCheckBox();

    private final JBCheckBox messageTraceCheckBox = new JBCheckBox();
    private final JBCheckBox debugCheckBox = new JBCheckBox();

    private final JButton autodetectZls = new JButton("Autodetect");

    {
        buildOnSave.setToolTipText("Whether to enable build-on-save diagnostics");
        buildOnSaveStep.setToolTipText("Select which step should be executed on build-on-save");
        highlightGlobalVarDeclarations.setToolTipText("Whether to highlight global var declarations");
        dangerousComptimeExperimentsDoNotEnable.setToolTipText("Whether to use the comptime interpreter");
    }
    public ZLSSettingsComponent() {
        zlsPathText.addBrowseFolderListener(
                new TextBrowseFolderListener(new FileChooserDescriptor(true, false, false, false, false, false)));
        myMainPanel = FormBuilder.createFormBuilder()
                                 .addComponent(new JBLabel("ZLS launch settings"))
                                 .addVerticalGap(10)
                                 .addLabeledComponent(new JBLabel("ZLS path: "), zlsPathText, 1, false)
                                 .addComponent(autodetectZls)
                                 .addLabeledComponent(new JBLabel("ZLS config path (leave empty to use built-in config): "),
                                                      zlsConfigPathText, 1, false)
                                 .addLabeledComponent(new JBLabel("Increase timeouts"), increaseTimeouts, 1, false)
                                 .addLabeledComponent(new JBLabel("Asynchronous code folding ranges: "),
                                                      asyncFoldingCheckBox, 1, false)
                                 .addSeparator()
                                 .addComponent(new JBLabel("ZLS configuration"))
                                 .addVerticalGap(10)
                                 .addLabeledComponent("Build on save", buildOnSave)
                                 .addLabeledComponent("Build on save step", buildOnSaveStep)
                                 .addLabeledComponent("Highlight global variable declarations", highlightGlobalVarDeclarations)
                                 .addLabeledComponent("Dangerous comptime experiments (do not enable)", dangerousComptimeExperimentsDoNotEnable)
                                 .addSeparator()
                                 .addComponent(new JBLabel(
                                         "Developer settings (only usable when the IDE was launched with " +
                                         "the runIDE gradle task in ZigBrains!)"))
                                 .addVerticalGap(10)
                                 .addLabeledComponent(new JBLabel("ZLS debug log: "), debugCheckBox, 1, false)
                                 .addLabeledComponent(new JBLabel("ZLS message trace: "), messageTraceCheckBox, 1,
                                                      false)
                                 .addComponentFillVertically(new JPanel(), 0)
                                 .getPanel();
        autodetectZls.addActionListener(e -> {
            ZLSSettingsState.executablePathFinder("zls").ifPresent(this::setZLSPath);
        });
    }

    public JPanel getPanel() {
        return myMainPanel;
    }

    @NotNull
    public String getZLSPath() {
        return zlsPathText.getText();
    }

    public void setZLSPath(@NotNull String newText) {
        zlsPathText.setText(newText);
    }

    @NotNull
    public String getZLSConfigPath() {
        return zlsConfigPathText.getText();
    }

    public void setZLSConfigPath(@NotNull String newText) {
        zlsConfigPathText.setText(newText);
    }

    public boolean getIncreaseTimeouts() {
        return increaseTimeouts.isSelected();
    }

    public void setIncreaseTimeouts(boolean state) {
        increaseTimeouts.setSelected(state);
    }

    public boolean getAsyncFolding() {
        return asyncFoldingCheckBox.isSelected();
    }

    public void setAsyncFolding(boolean state) {
        asyncFoldingCheckBox.setSelected(state);
    }

    public boolean getDebug() {
        return debugCheckBox.isSelected();
    }

    public void setDebug(boolean state) {
        debugCheckBox.setSelected(state);
    }

    public boolean getMessageTrace() {
        return messageTraceCheckBox.isSelected();
    }

    public void setMessageTrace(boolean state) {
        messageTraceCheckBox.setSelected(state);
    }

    public boolean getBuildOnSave() {
        return buildOnSave.isSelected();
    }

    public void setBuildOnSave(boolean state) {
        buildOnSave.setSelected(state);
    }

    public String getBuildOnSaveStep() {
        return buildOnSaveStep.getText();
    }

    public void setBuildOnSaveStep(String value) {
        buildOnSaveStep.setText(value);
    }

    public boolean getHighlightGlobalVarDeclarations() {
        return highlightGlobalVarDeclarations.isSelected();
    }

    public void setHighlightGlobalVarDeclarations(boolean state) {
        highlightGlobalVarDeclarations.setSelected(state);
    }

    public boolean getDangerousComptimeExperimentsDoNotEnable() {
        return dangerousComptimeExperimentsDoNotEnable.isSelected();
    }

    public void setDangerousComptimeExperimentsDoNotEnable(boolean state) {
        dangerousComptimeExperimentsDoNotEnable.setSelected(state);
    }
}
