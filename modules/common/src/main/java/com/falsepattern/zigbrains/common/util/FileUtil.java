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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class FileUtil {
    private static final Logger LOG = Logger.getInstance(FileUtil.class);

    public final static String SPACE_ENCODED = "%20";
    private final static OS os = (System.getProperty("os.name").toLowerCase().contains("win")) ? OS.WINDOWS : OS.UNIX;
    private final static String COLON_ENCODED = "%3A";
    private final static String URI_FILE_BEGIN = "file:";
    private final static String URI_VALID_FILE_BEGIN = "file:///";
    private final static char URI_PATH_SEP = '/';

    /**
     * Fixes common problems in uri, mainly related to Windows
     *
     * @param uri The uri to sanitize
     * @return The sanitized uri
     */
    public static String sanitizeURI(String uri) {
        if (uri != null) {
            StringBuilder reconstructed = new StringBuilder();
            String uriCp = uri.replaceAll(" ", SPACE_ENCODED); //Don't trust servers
            if (!uri.startsWith(URI_FILE_BEGIN)) {
                LOG.warn("Malformed uri : " + uri);
                return uri; //Probably not an uri
            } else {
                uriCp = uriCp.substring(URI_FILE_BEGIN.length());
                while (uriCp.startsWith(Character.toString(URI_PATH_SEP))) {
                    uriCp = uriCp.substring(1);
                }
                reconstructed.append(URI_VALID_FILE_BEGIN);
                if (os == OS.UNIX) {
                    return reconstructed.append(uriCp).toString();
                } else {
                    reconstructed.append(uriCp.substring(0, uriCp.indexOf(URI_PATH_SEP)));
                    char driveLetter = reconstructed.charAt(URI_VALID_FILE_BEGIN.length());
                    if (Character.isLowerCase(driveLetter)) {
                        reconstructed.setCharAt(URI_VALID_FILE_BEGIN.length(), Character.toUpperCase(driveLetter));
                    }
                    if (reconstructed.toString().endsWith(COLON_ENCODED)) {
                        reconstructed.delete(reconstructed.length() - 3, reconstructed.length());
                    }
                    if (!reconstructed.toString().endsWith(":")) {
                        reconstructed.append(":");
                    }
                    return reconstructed.append(uriCp.substring(uriCp.indexOf(URI_PATH_SEP))).toString();
                }
            }
        } else {
            return null;
        }
    }

    /**
     * Returns the URI string corresponding to a VirtualFileSystem file
     *
     * @param file The file
     * @return the URI
     */
    public static String URIFromVirtualFile(VirtualFile file) {
        return file == null? null : pathToUri(file.getPath());
    }
    /**
     * Transforms an URI string into a VFS file
     *
     * @param uri The uri
     * @return The virtual file
     */
    public static VirtualFile virtualFileFromURI(URI uri) {
        return LocalFileSystem.getInstance().findFileByIoFile(new File(uri));
    }

    public static VirtualFile virtualFileFromPath(Path path) {
        return LocalFileSystem.getInstance().findFileByNioFile(path);
    }

    /**
     * Transforms an URI string into a VFS file
     *
     * @param uri The uri
     * @return The virtual file
     */
    public static VirtualFile virtualFileFromURI(String uri) {
        try {
            return virtualFileFromURI(new URI(sanitizeURI(uri)));
        } catch (URISyntaxException e) {
            LOG.warn(e);
            return null;
        }
    }

    /**
     * Transforms a path into an URI string
     *
     * @param path The path
     * @return The uri
     */
    public static String pathToUri(@Nullable String path) {
        return path != null ? sanitizeURI(new File(path).toURI().toString()) : null;
    }

    public static String pathToUri(@Nullable Path path) {
        return path != null ? sanitizeURI(path.toUri().toString()) : null;
    }

    public static Optional<Path> findExecutableOnPATH(String exe) {
        var exeName = SystemInfo.isWindows ? exe + ".exe" : exe;
        var PATH = System.getenv("PATH").split(File.pathSeparator);
        for (var dir: PATH) {
            var path = Path.of(dir);
            try {
                path = path.toAbsolutePath();
            } catch (Exception ignored) {
                continue;
            }
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                continue;
            }
            var exePath = path.resolve(exeName).toAbsolutePath();
            if (!Files.isRegularFile(exePath) || !Files.isExecutable(exePath)) {
                continue;
            }
            return Optional.of(exePath);
        }
        return Optional.empty();
    }

    /**
     * Object representing the OS type (Windows or Unix)
     */
    public enum OS {
        WINDOWS, UNIX
    }
}
