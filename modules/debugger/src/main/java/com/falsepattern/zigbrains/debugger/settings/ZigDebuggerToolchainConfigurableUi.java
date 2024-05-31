package com.falsepattern.zigbrains.debugger.settings;

import com.falsepattern.zigbrains.ZigBundle;
import com.falsepattern.zigbrains.common.util.dsl.JavaPanel;
import com.falsepattern.zigbrains.debugger.toolchain.DebuggerAvailability;
import com.falsepattern.zigbrains.debugger.toolchain.DebuggerKind;
import com.falsepattern.zigbrains.debugger.toolchain.ZigDebuggerToolchainService;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.observable.util.ListenerUiUtil;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JEditorPane;
import java.util.Arrays;
import java.util.Vector;

import static com.intellij.ui.dsl.builder.UtilsKt.DEFAULT_COMMENT_WIDTH;

public class ZigDebuggerToolchainConfigurableUi extends ZigDebuggerUiComponent {
    private final ComboBox<DebuggerKind> debuggerKindCombobox = new ComboBox<>(createDebuggerKindComboBoxModel());

    private final JBCheckBox downloadAutomaticallyCheckBox = new JBCheckBox(
            ZigBundle.message("settings.debugger.toolchain.download.debugger.automatically.checkbox"),
            ZigDebuggerSettings.getInstance().downloadAutomatically
    );

    private final JBCheckBox useClion = new JBCheckBox(
            ZigBundle.message("settings.debugger.toolchain.use.clion.toolchains"),
            ZigDebuggerSettings.getInstance().useClion
    );

    private JEditorPane comment = null;

    private DebuggerKind currentDebuggerKind() {
        return debuggerKindCombobox.getItem();
    }

    @Override
    public boolean isModified(@NotNull ZigDebuggerSettings settings) {
        return settings.debuggerKind != debuggerKindCombobox.getItem() ||
               settings.downloadAutomatically != downloadAutomaticallyCheckBox.isSelected() ||
               settings.useClion != useClion.isSelected();
    }

    @Override
    public void reset(@NotNull ZigDebuggerSettings settings) {
        debuggerKindCombobox.setItem(settings.debuggerKind);
        downloadAutomaticallyCheckBox.setSelected(settings.downloadAutomatically);
        useClion.setSelected(settings.useClion);
    }

    @Override
    public void apply(@NotNull ZigDebuggerSettings settings) throws ConfigurationException {
        settings.debuggerKind = debuggerKindCombobox.getItem();
        settings.downloadAutomatically = downloadAutomaticallyCheckBox.isSelected();
        settings.useClion = useClion.isSelected();
    }

    @Override
    public void buildUi(JavaPanel panel) {
        panel.row(ZigBundle.message("settings.debugger.toolchain.debugger.label"), r -> {
            comment = r.cell(debuggerKindCombobox)
                    .comment("", DEFAULT_COMMENT_WIDTH, (e) -> downloadDebugger())
                    .applyToComponent(c -> {
                        ListenerUiUtil.whenItemSelected(c, null, (x) -> {
                            update();
                            return null;
                        });
                        return null;
                    })
                    .getComment();
        });
        panel.row(r -> {
            r.cell(downloadAutomaticallyCheckBox);
        });
        if (PluginManager.isPluginInstalled(PluginId.getId("com.intellij.modules.clion"))) {
            panel.row(r -> {
                r.cell(useClion);
            });
        }
        update();
    }

    private ComboBoxModel<DebuggerKind> createDebuggerKindComboBoxModel() {
        val toolchainService = ZigDebuggerToolchainService.getInstance();
        val availableKinds = Arrays.stream(DebuggerKind.values())
                                   .filter(kind -> toolchainService.debuggerAvailability(kind) != DebuggerAvailability.Unavailable)
                                   .toList();

        val model = new DefaultComboBoxModel<>(new Vector<>(availableKinds));
        model.setSelectedItem(ZigDebuggerSettings.getInstance().debuggerKind);
        return model;
    }

    private void downloadDebugger() {
        val result = ZigDebuggerToolchainService.getInstance().downloadDebugger(null, currentDebuggerKind());
        if (result instanceof ZigDebuggerToolchainService.DownloadResult.Ok) {
            update();
        }
    }

    private void update() {
        val availability = ZigDebuggerToolchainService.getInstance().debuggerAvailability(currentDebuggerKind());
        final String text;
        if (availability == DebuggerAvailability.NeedToDownload) {
            text = ZigBundle.message("settings.debugger.toolchain.download.comment");
        } else if (availability == DebuggerAvailability.NeedToUpdate) {
            text = ZigBundle.message("settings.debugger.toolchain.update.comment");
        } else {
            text = null;
        }

        if (comment != null) {
            comment.setText(text);
            comment.setVisible(text != null);
        }
    }
}
