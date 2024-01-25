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

package com.falsepattern.zigbrains.zig.debugger;

import com.falsepattern.zigbrains.zig.ZigFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.jetbrains.cidr.execution.debugger.breakpoints.CidrLineBreakpointFileTypesProvider;

import java.util.Collections;
import java.util.Set;

public class ZigLineBreakpointFileTypesProvider implements CidrLineBreakpointFileTypesProvider {
    private static final Set<FileType> ZIG_FILE_TYPES = Collections.singleton(ZigFileType.INSTANCE);
    @Override
    public Set<FileType> getFileTypes() {
        return ZIG_FILE_TYPES;
    }
}
