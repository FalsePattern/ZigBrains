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
package com.falsepattern.zigbrains.lsp.utils;

import com.falsepattern.zigbrains.common.util.FileUtil;
import com.falsepattern.zigbrains.lsp.IntellijLanguageClient;
import com.falsepattern.zigbrains.lsp.extensions.LSPExtensionManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightVirtualFileBase;
import lombok.val;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.falsepattern.zigbrains.common.util.ApplicationUtil.computableReadAction;

/**
 * Various file / uri related methods
 */
public class FileUtils {
    private static final Logger LOG = Logger.getInstance(FileUtils.class);

    public static List<Editor> getAllOpenedEditors(Project project) {
        return computableReadAction(() -> {
            List<Editor> editors = new ArrayList<>();
            FileEditor[] allEditors = FileEditorManager.getInstance(project).getAllEditors();
            for (FileEditor fEditor : allEditors) {
                if (fEditor instanceof TextEditor) {
                    Editor editor = ((TextEditor) fEditor).getEditor();
                    if (editor.isDisposed() || !isEditorSupported(editor)) {
                        continue;
                    }
                    editors.add(editor);
                }
            }
            return editors;
        });
    }

    public static List<Editor> getAllOpenedEditorsForUri(@NotNull Project project, String uri) {
        VirtualFile file = FileUtil.virtualFileFromURI(uri);
        if (file == null)
            return Collections.emptyList();
        return getAllOpenedEditorsForVirtualFile(project, file);
    }

    public static List<Editor> getAllOpenedEditorsForVirtualFile(@NotNull Project project, @NotNull VirtualFile file) {
        return computableReadAction(() -> {
            List<Editor> editors = new ArrayList<>();
            FileEditor[] allEditors = FileEditorManager.getInstance(project).getAllEditors(file);
            for (FileEditor fEditor : allEditors) {
                if (fEditor instanceof TextEditor) {
                    Editor editor = ((TextEditor) fEditor).getEditor();
                    if (editor.isDisposed() || !isEditorSupported(editor)) {
                        continue;
                    }
                    editors.add(editor);
                }
            }
            return editors;
        });
    }


    public static Editor editorFromPsiFile(PsiFile psiFile) {
        return editorFromVirtualFile(psiFile.getVirtualFile(), psiFile.getProject());
    }

    public static Editor editorFromUri(String uri, Project project) {
        return editorFromVirtualFile(FileUtil.virtualFileFromURI(uri), project);
    }

    @Nullable
    public static Editor editorFromVirtualFile(VirtualFile file, Project project) {
        FileEditor[] allEditors = FileEditorManager.getInstance(project).getAllEditors(file);
        if (allEditors.length > 0 && allEditors[0] instanceof TextEditor) {
            return ((TextEditor) allEditors[0]).getEditor();
        }
        return null;
    }

    /**
     * Transforms an editor (Document) identifier to an LSP identifier
     *
     * @param editor The editor
     * @return The TextDocumentIdentifier
     */
    public static TextDocumentIdentifier editorToLSPIdentifier(Editor editor) {
        return new TextDocumentIdentifier(editorToURIString(editor));
    }

    /**
     * Returns the URI string corresponding to an Editor (Document)
     *
     * @param editor The Editor
     * @return The URI
     */
    public static String editorToURIString(Editor editor) {
        return FileUtil.sanitizeURI(
                FileUtil.URIFromVirtualFile(FileDocumentManager.getInstance().getFile(editor.getDocument())));
    }

    public static VirtualFile virtualFileFromEditor(Editor editor) {
        return FileDocumentManager.getInstance().getFile(editor.getDocument());
    }

    /**
     * Returns the project base dir uri given an editor
     *
     * @param editor The editor
     * @return The project whose the editor belongs
     */
    public static String editorToProjectFolderUri(Editor editor) {
        return FileUtil.pathToUri(editorToProjectFolderPath(editor));
    }

    public static String editorToProjectFolderPath(Editor editor) {
        if (editor == null)
            return null;

        val project = editor.getProject();
        if (project == null)
            return null;

        val projectDir = ProjectUtil.guessProjectDir(editor.getProject());
        if (projectDir == null)
            return null;

        return projectDir.toNioPath().toAbsolutePath().toString();
    }

    public static String projectToUri(Project project) {
        if (project == null)
            return null;

        val path = ProjectUtil.guessProjectDir(project);
        if (path == null)
            return null;

        return FileUtil.pathToUri(path.toNioPath());
    }

    public static String documentToUri(Document document) {
        return FileUtil.sanitizeURI(FileUtil.URIFromVirtualFile(FileDocumentManager.getInstance().getFile(document)));
    }

    /**
     * Find projects which contains the given file. This search runs among all open projects.
     */
    @NotNull
    public static Set<Project> findProjectsFor(@NotNull VirtualFile file) {
        return Arrays.stream(ProjectManager.getInstance().getOpenProjects())
                     .filter(p -> searchFiles(file.getName(), p).stream().anyMatch(f -> f.getPath().equals(file.getPath())))
                     .collect(Collectors.toSet());
    }

    public static Collection<VirtualFile> searchFiles(String fileName, Project p) {
        try {
            return computableReadAction(() -> FilenameIndex.getVirtualFilesByName(fileName, GlobalSearchScope.projectScope(p)));
        } catch (Throwable t) {
            // Todo - Find a proper way to handle when IDEA file indexing is in-progress.
            return Collections.emptyList();
        }
    }

    /**
     * This can be used to instantly apply a language server definition without restarting the IDE.
     */
    public static void reloadAllEditors() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : openProjects) {
            reloadEditors(project);
        }
    }

    /**
     * This can be used to instantly apply a project-specific language server definition without restarting the
     * project/IDE.
     *
     * @param project The project instance which need to be restarted
     */
    public static void reloadEditors(@NotNull Project project) {
        try {
            List<Editor> allOpenedEditors = FileUtils.getAllOpenedEditors(project);
            allOpenedEditors.forEach(IntellijLanguageClient::editorClosed);
            allOpenedEditors.forEach(IntellijLanguageClient::editorOpened);
        } catch (Exception e) {
            LOG.warn(String.format("Refreshing project: %s is failed due to: ", project.getName()), e);
        }
    }

    /**
     * Checks if the given virtual file instance is supported by this LS client library.
     */
    public static boolean isFileSupported(@Nullable VirtualFile file) {
        if (file == null) {
            return false;
        }

        if (file instanceof LightVirtualFileBase) {
            return false;
        }

        if (file.getUrl().isEmpty() || file.getUrl().startsWith("jar:")) {
            return false;
        }

        return IntellijLanguageClient.isExtensionSupported(file);
    }

    /**
     * Checks if the file in editor is supported by this LS client library.
     */
    public static boolean isEditorSupported(@NotNull Editor editor) {
        return isFileSupported(virtualFileFromEditor(editor)) &&
                isFileContentSupported(editor);
    }

    // Always returns true unless the user has registered filtering to validate file content via LS protocol extension
    // manager implementation.
    private static boolean isFileContentSupported(Editor editor) {
        return computableReadAction(() -> {
            if (editor.getProject() == null) {
                return true;
            }
            PsiFile file = PsiDocumentManager.getInstance(editor.getProject()).getPsiFile(editor.getDocument());
            if (file == null) {
                return true;
            }
            LSPExtensionManager lspExtManager = IntellijLanguageClient.getExtensionManagerFor(FileUtilRt.getExtension(file.getName()));
            if (lspExtManager == null) {
                return true;
            }
            return lspExtManager.isFileContentSupported(file);
        });
    }
}
