package com.falsepattern.zigbrains.zig.lsp;

import com.falsepattern.zigbrains.zig.util.HighlightingUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider;
import lombok.val;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.messages.NotificationMessage;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.File;
import java.net.URI;

public class ZLSStreamConnectionProvider extends ProcessStreamConnectionProvider {
    private final Project project;
    public ZLSStreamConnectionProvider(Project project) {
        this.project = project;
        super.setCommands(ZLSStartupActivity.getCommand(project));
    }



    @Override
    public void handleMessage(Message message, LanguageServer languageServer, VirtualFile rootUri) {
        if (message instanceof NotificationMessage notif) {
            switch (notif.getMethod()) {
                case "textDocument/didOpen", "textDocument/didChange" -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    val params = notif.getParams();
                    VirtualFile file;
                    if (params instanceof DidOpenTextDocumentParams didOpen) {
                        file = VfsUtil.findFileByIoFile(new File(URI.create(didOpen.getTextDocument().getUri())), true);
                    } else if (params instanceof DidChangeTextDocumentParams didChange) {
                        file = VfsUtil.findFileByIoFile(new File(URI.create(didChange.getTextDocument().getUri())), true);
                    } else {
                        file = null;
                    }
                    if (file == null)
                        return;
                    val editors = LSPIJUtils.editorsForFile(file, project);
                    for (val editor: editors) {
                        HighlightingUtil.refreshHighlighting(editor);
                    }
                });
            }
        }
        super.handleMessage(message, languageServer, rootUri);
    }
}
