/*
 * Copyright 2023 FalsePattern
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

package com.falsepattern.zigbrains.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;

public class AppSettingsComponent {
    private final JPanel myMainPanel;
    private final TextFieldWithBrowseButton zlsPathText = new TextFieldWithBrowseButton();
    private final TextFieldWithBrowseButton zlsConfigPathText = new TextFieldWithBrowseButton();
    private final JBCheckBox debugCheckBox = new JBCheckBox();
    private final JBCheckBox messageTraceCheckBox = new JBCheckBox();

    public AppSettingsComponent() {
        zlsPathText.addBrowseFolderListener(
                new TextBrowseFolderListener(new FileChooserDescriptor(true, false, false, false, false, false)));
        myMainPanel = FormBuilder.createFormBuilder()
                                 .addComponent(new JBLabel("Regular settings"))
                                 .addVerticalGap(10)
                                 .addLabeledComponent(new JBLabel("ZLS path: "), zlsPathText, 1, false)
                                 .addLabeledComponent(new JBLabel("ZLS config path (leave empty to use default): "), zlsConfigPathText, 1, false)
                                 .addSeparator()
                                 .addComponent(new JBLabel("Developer settings"))
                                 .addVerticalGap(10)
                                 .addLabeledComponent(new JBLabel("ZLS debug log: "), debugCheckBox, 1, false)
                                 .addLabeledComponent(new JBLabel("ZLS message trace: "), messageTraceCheckBox, 1, false)
                                 .addComponentFillVertically(new JPanel(), 0)
                                 .getPanel();
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
}
