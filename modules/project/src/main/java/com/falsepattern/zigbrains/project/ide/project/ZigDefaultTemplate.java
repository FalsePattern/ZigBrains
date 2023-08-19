/*
 * Copyright 2023 FalsePattern
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

package com.falsepattern.zigbrains.project.ide.project;

import com.falsepattern.zigbrains.zig.Icons;
import com.intellij.openapi.util.NlsContexts;

public sealed class ZigDefaultTemplate extends ZigProjectTemplate {
    public ZigDefaultTemplate(@NlsContexts.ListItem String name, boolean isBinary) {
        super(name, isBinary, Icons.ZIG);
    }

    public static final class ZigExecutableTemplate extends ZigDefaultTemplate {
        public static final ZigExecutableTemplate INSTANCE = new ZigExecutableTemplate();
        private ZigExecutableTemplate() {
            super("Executable (application)", true);
        }
    }

    public static final class ZigLibraryTemplate extends ZigDefaultTemplate {
        public static final ZigLibraryTemplate INSTANCE = new ZigLibraryTemplate();
        private ZigLibraryTemplate() {
            super("Library (static)", true);
        }
    }
}
