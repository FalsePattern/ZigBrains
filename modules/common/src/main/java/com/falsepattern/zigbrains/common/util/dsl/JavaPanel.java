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

package com.falsepattern.zigbrains.common.util.dsl;

import com.falsepattern.zigbrains.common.util.KtUtil;
import com.intellij.openapi.ui.DialogPanel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.dsl.builder.Align;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.ui.dsl.builder.BuilderKt;
import com.intellij.ui.dsl.builder.Panel;
import com.intellij.ui.dsl.builder.RightGap;
import com.intellij.ui.dsl.builder.Row;
import com.intellij.ui.dsl.builder.RowsRange;
import lombok.RequiredArgsConstructor;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Color;
import java.util.function.Consumer;

import static com.falsepattern.zigbrains.common.util.KtUtil.$f;

public class JavaPanel {
    private final Panel panel;

    public JavaPanel(Panel p) {
        this.panel = p;
    }

    public void cell(String label, JComponent component, Align align) {
        row(label, row -> row.cell(component).align(align));
    }

    public void cell(String label, JComponent component) {
        row(label, row -> row.cell(component));
    }

    public void cell(JComponent component) {
        cell("", component);
    }

    public void label(String label) {
        panel.row((JLabel) null, $f(r -> r.label(label)));
    }

    public void gap() {
        panel.gap(RightGap.SMALL);
    }

    public void row(Consumer<Row> row) {
        panel.row((JLabel) null, (r) -> {
            row.accept(r);
            return null;
        });
    }

    public void row(String text, Consumer<Row> row) {
        panel.row(text, $f(row));
    }

    public void separator() {
        panel.separator(null);
    }

    public void separator(Color color) {
        panel.separator(color);
    }

    public void panel(Consumer<JavaPanel> c) {
        panel.panel($f(p -> {
            c.accept(new JavaPanel(p));
        }));
    }

    public static DialogPanel newPanel(Consumer<JavaPanel> c) {
        return BuilderKt.panel((p) -> {
            c.accept(new JavaPanel(p));
            return null;
        });
    }

    public void group(String title, Consumer<JavaPanel> c) {
        panel.groupRowsRange(title, false, null, null, (p) -> {
            c.accept(new JavaPanel(p));
            return null;
        });
    }
}
