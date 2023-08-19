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

package com.falsepattern.zigbrains.project.toolchain;

import com.google.gson.annotations.SerializedName;

public record ZigToolchainEnvironmentSerializable(
        @SerializedName("zig_exe")
        String zigExecutable,
        @SerializedName("lib_dir")
        String libDirectory,
        @SerializedName("std_dir")
        String stdDirectory,
        @SerializedName("global_cache_dir")
        String globalCacheDirectory,
        @SerializedName("version")
        String version,
        @SerializedName("target")
        String target
) {}
