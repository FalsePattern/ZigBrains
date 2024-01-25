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

package com.falsepattern.zigbrains.common.util;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.DocumentAdapter;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.util.function.Consumer;

public class TextFieldUtil {
    public static TextFieldWithBrowseButton pathToDirectoryTextField(Disposable disposable,
                                                                     @NlsContexts.DialogTitle String dialogTitle,
                                                                     Runnable onTextChanged) {
        return pathTextField(FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                             disposable,
                             dialogTitle,
                             onTextChanged);
    }

    public static TextFieldWithBrowseButton pathTextField(FileChooserDescriptor fileChooserDescriptor,
                                                          Disposable disposable,
                                                          @NlsContexts.DialogTitle String dialogTitle,
                                                          Runnable onTextChanged) {
        val component = new TextFieldWithBrowseButton(null, disposable);
        component.addBrowseFolderListener(dialogTitle, null, null, fileChooserDescriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        addTextChangeListener(component.getChildComponent(), ignored -> onTextChanged.run());

        return component;
    }

    public static void addTextChangeListener(JTextField textField, Consumer<DocumentEvent> listener) {
        textField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                listener.accept(e);
            }
        });
    }
}
