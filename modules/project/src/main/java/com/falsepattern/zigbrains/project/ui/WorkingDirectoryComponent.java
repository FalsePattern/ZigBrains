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

import com.intellij.execution.ExecutionBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;

public class WorkingDirectoryComponent extends LabeledComponent<TextFieldWithBrowseButton> implements Disposable {
    private final TextFieldWithBrowseButton field;
    public WorkingDirectoryComponent(Disposable parent) {
        field = new TextFieldWithBrowseButton(null, parent);
        var fileChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        fileChooser.setTitle(ExecutionBundle.message("select.working.directory.message"));
        field.addBrowseFolderListener(null, null, null, fileChooser);
        setComponent(field);
        setText(ExecutionBundle.message("run.configuration.working.directory.label"));
    }

    @Override
    public void dispose() {
        field.dispose();
    }
}
