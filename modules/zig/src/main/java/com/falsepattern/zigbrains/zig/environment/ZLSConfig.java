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

import lombok.Builder;
import lombok.With;


@With
@Builder
public record ZLSConfig(String zig_exe_path,
                        String zig_lib_path,
                        Boolean enable_build_on_save,
                        String build_on_save_step,
                        Boolean dangerous_comptime_experiments_do_not_enable,
                        Boolean highlight_global_var_declarations
                        ) {
}
