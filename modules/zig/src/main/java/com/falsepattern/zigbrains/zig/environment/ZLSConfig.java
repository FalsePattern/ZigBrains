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

package com.falsepattern.zigbrains.zig.environment;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record ZLSConfig(@NotNull Optional<String> zigExePath,
                        @NotNull Optional<String> zigLibPath) {
    public ZLSConfig(String zigExePath, String zigLibPath) {
        this(Optional.ofNullable(zigExePath), Optional.ofNullable(zigLibPath));
    }

    public ZLSConfig overrideWith(ZLSConfig other) {
        return new ZLSConfig(other.zigExePath.or(() -> zigExePath),
                             other.zigLibPath.or(() -> zigLibPath));
    }

    public static final ZLSConfig EMPTY = new ZLSConfig(Optional.empty(), Optional.empty());

    public JsonObject toJson() {
        val result = new JsonObject();
        zigExePath.ifPresent(s -> result.addProperty("zig_exe_path", s));
        zigLibPath.ifPresent(s -> result.addProperty("zig_lib_path", s));
        return result;
    }
}
