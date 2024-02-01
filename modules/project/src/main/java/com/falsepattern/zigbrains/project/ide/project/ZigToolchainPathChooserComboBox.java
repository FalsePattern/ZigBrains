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

package com.falsepattern.zigbrains.project.ide.project;

import com.falsepattern.zigbrains.common.util.TextFieldUtil;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.ComboBoxWithWidePopup;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import lombok.val;

import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.nio.file.Path;

public class ZigToolchainPathChooserComboBox extends ComponentWithBrowseButton<ComboBoxWithWidePopup<Path>> {
    public Runnable onTextChanged;

    private final BasicComboBoxEditor comboBoxEditor = new BasicComboBoxEditor() {
        @Override
        protected JTextField createEditorComponent() {
            return new ExtendableTextField();
        }
    };

    private ExtendableTextField getPathTextField() {
        return (ExtendableTextField) getChildComponent().getEditor().getEditorComponent();
    }

    private final ExtendableTextComponent.Extension busyIconExtension = hovered -> AnimatedIcon.Default.INSTANCE;

    public Path getSelectedPath() {
        return Path.of(getPathTextField().getText());
    }

    public void setSelectedPath(Path path) {
        if (path == null) {
            getPathTextField().setText("");
            return;
        }
        getPathTextField().setText(path.toString());
    }

    public ZigToolchainPathChooserComboBox(Runnable onTextChanged) {
        super(new ComboBoxWithWidePopup<>(), null);
        this.onTextChanged = onTextChanged;

        ComboboxSpeedSearch.installOn(getChildComponent());
        getChildComponent().setEditor(comboBoxEditor);
        getChildComponent().setEditable(true);

        addActionListener(e -> {
            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            //noinspection UsagesOfObsoleteApi
            FileChooser.chooseFile(descriptor, null, null, (file) -> getChildComponent().setSelectedItem(file.toNioPath()));
        });

        TextFieldUtil.addTextChangeListener(getPathTextField(), ignored -> onTextChanged.run());
    }
}
