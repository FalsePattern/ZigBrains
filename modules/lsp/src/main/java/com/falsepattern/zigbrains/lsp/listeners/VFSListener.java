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
package com.falsepattern.zigbrains.lsp.listeners;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileCopyEvent;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VFSListener implements BulkFileListener {

    @Override
    public void before(@NotNull List<? extends @NotNull VFileEvent> events) {
    }

    @Override
    public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        for (val event: events) {
            if (event instanceof VFilePropertyChangeEvent propEvent)
                propertyChanged(propEvent);
            else if (event instanceof VFileContentChangeEvent changeEvent)
                contentsChanged(changeEvent);
            else if (event instanceof VFileDeleteEvent deleteEvent)
                fileDeleted(deleteEvent);
            else if (event instanceof VFileMoveEvent moveEvent)
                fileMoved(moveEvent);
            else if (event instanceof VFileCopyEvent copyEvent)
                fileCopied(copyEvent);
            else if (event instanceof VFileCreateEvent createEvent)
                fileCreated(createEvent);
        }
    }

    /**
     * Fired when a virtual file is renamed from within IDEA, or its writable status is changed.
     * For files renamed externally, {@link #fileCreated} and {@link #fileDeleted} events will be fired.
     *
     * @param event the event object containing information about the change.
     */
    public void propertyChanged(@NotNull VFilePropertyChangeEvent event) {
        if (event.getPropertyName().equals(VirtualFile.PROP_NAME)) {
            LSPFileEventManager.fileRenamed((String) event.getOldValue(), (String) event.getNewValue());
        }
    }

    /**
     * Fired when the contents of a virtual file is changed.
     *
     * @param event the event object containing information about the change.
     */
    public void contentsChanged(@NotNull VFileContentChangeEvent event) {
        LSPFileEventManager.fileChanged(event.getFile());
    }

    /**
     * Fired when a virtual file is deleted.
     *
     * @param event the event object containing information about the change.
     */
    public void fileDeleted(@NotNull VFileDeleteEvent event) {
        LSPFileEventManager.fileDeleted(event.getFile());
    }

    /**
     * Fired when a virtual file is moved from within IDEA.
     *
     * @param event the event object containing information about the change.
     */
    public void fileMoved(@NotNull VFileMoveEvent event) {
        LSPFileEventManager.fileMoved(event);
    }

    /**
     * Fired when a virtual file is copied from within IDEA.
     *
     * @param event the event object containing information about the change.
     */
    public void fileCopied(@NotNull VFileCopyEvent event) {
        LSPFileEventManager.fileCreated(event.findCreatedFile());
    }

    /**
     * Fired when a virtual file is created. This event is not fired for files discovered during initial VFS initialization.
     *
     * @param event the event object containing information about the change.
     */
    public void fileCreated(@NotNull VFileCreateEvent event) {
        LSPFileEventManager.fileCreated(event.getFile());
    }
}
