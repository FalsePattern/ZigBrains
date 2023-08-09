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

package com.falsepattern.zigbrains.zig.ide;

import com.falsepattern.zigbrains.zig.settings.AppSettingsState;
import org.eclipse.lsp4j.FoldingRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.contributors.LSPFoldingRangeProvider;

public class ZigFoldingRangeProvider extends LSPFoldingRangeProvider {
    @Override
    protected @Nullable String getCollapsedText(@NotNull FoldingRange foldingRange) {
        var text = super.getCollapsedText(foldingRange);
        if (text != null) {
            return text;
        }
        var kind = foldingRange.getKind();
        if (kind == null) {
            return "...";
        }
        return switch (kind) {
            case "comment" -> "///..."; //These are only done for doc comments. TODO figure out how to invoke the intellij doc renderer
            default -> "...";
        };
    }

    @Override
    protected boolean async() {
        return AppSettingsState.getInstance().asyncFolding;
    }
}
