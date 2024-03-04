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

package com.falsepattern.zigbrains.project.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.TextAccessor;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public class ZigFilePathPanel extends JPanel implements TextAccessor {
    private final TextFieldWithBrowseButton textField = new TextFieldWithBrowseButton();
    public ZigFilePathPanel() {
        super(new BorderLayout());
        textField.addBrowseFolderListener(
                new TextBrowseFolderListener(new FileChooserDescriptor(true, false, false, false, false, false)));
        add(textField, BorderLayout.CENTER);
    }

    @Override
    public void setText(String text) {
        textField.setText(text);
    }

    @Override
    public String getText() {
        return textField.getText();
    }
}
