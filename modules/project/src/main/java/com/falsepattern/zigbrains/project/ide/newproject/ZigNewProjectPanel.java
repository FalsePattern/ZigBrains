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

package com.falsepattern.zigbrains.project.ide.newproject;

import com.falsepattern.zigbrains.common.util.dsl.JavaPanel;
import com.falsepattern.zigbrains.project.ide.project.ZigDefaultTemplate;
import com.falsepattern.zigbrains.project.ide.project.ZigProjectSettingsPanel;
import com.falsepattern.zigbrains.project.ide.project.ZigProjectTemplate;
import com.falsepattern.zigbrains.zig.settings.ZLSSettingsPanel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBList;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.ui.dsl.builder.AlignY;
import com.intellij.ui.dsl.builder.Panel;
import com.intellij.util.ui.JBUI;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ZigNewProjectPanel implements Disposable {
    private boolean handleGit;
    private JBCheckBox git = new JBCheckBox();
    private ZigProjectSettingsPanel projConf;
    private ZLSSettingsPanel zlsConf;

    public ZigNewProjectPanel(boolean handleGit) {
        this.handleGit = handleGit;
        projConf = new ZigProjectSettingsPanel();
        zlsConf = new ZLSSettingsPanel();
    }

    public ZigProjectConfigurationData getData() {
        ZigProjectTemplate selectedTemplate = templateList.getSelectedValue();
        return new ZigProjectConfigurationData(handleGit && git.isSelected(), projConf.getData(), zlsConf.getData(), selectedTemplate);
    }

    private final List<ZigProjectTemplate> defaultTemplates = Arrays.asList(
            ZigDefaultTemplate.ZigExecutableTemplate.INSTANCE,
            ZigDefaultTemplate.ZigLibraryTemplate.INSTANCE,
            ZigDefaultTemplate.ZigInitTemplate.INSTANCE
                                                                           );
    private final DefaultListModel<ZigProjectTemplate> templateListModel = JBList.createDefaultListModel(defaultTemplates);

    private final JBList<ZigProjectTemplate> templateList = Optional.of(new JBList<>(templateListModel))
            .map((it) -> {
                it.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                it.setSelectedIndex(0);
                it.setCellRenderer(new ColoredListCellRenderer<ZigProjectTemplate>() {
                    @Override
                    protected void customizeCellRenderer(@NotNull JList<? extends ZigProjectTemplate> list, ZigProjectTemplate value, int index, boolean selected, boolean hasFocus) {
                        setIcon(value.icon);
                        append(value.name);
                    }
                });
                return it;
            }).get();

    private final ToolbarDecorator templateToolbar = ToolbarDecorator.createDecorator(templateList)
            .setToolbarPosition(ActionToolbarPosition.BOTTOM)
            .setPreferredSize(JBUI.size(0, 125))
            .disableUpDownActions()
            .disableAddAction()
            .disableRemoveAction();

    public void attachPanelTo(JavaPanel p) {
        if (handleGit) {
            p.row("Create Git repository", r -> r.cell(git));
        }
        p.group("Zig Project Template", (p2) -> {
            p2.row((r) -> {
                r.resizableRow();
                r.cell(templateToolbar.createPanel())
                 .align(AlignX.FILL)
                 .align(AlignY.FILL);
            });
        });
        projConf.attachPanelTo(p);
        zlsConf.attachPanelTo(p);
        projConf.autodetect();
        zlsConf.autodetect();
    }

    @Override
    public void dispose() {
        projConf = null;
        zlsConf = null;
    }
}
