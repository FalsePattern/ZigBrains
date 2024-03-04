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

import lombok.val;

import java.util.Arrays;

public class StringUtil {
    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public static String orEmpty(String value) {
        return value == null ? "" : value;
    }


    private static final char[] VT100_CHARS = new char[256];

    static {
        Arrays.fill(VT100_CHARS, ' ');
        VT100_CHARS[0x6A] = '┘';
        VT100_CHARS[0x6B] = '┐';
        VT100_CHARS[0x6C] = '┌';
        VT100_CHARS[0x6D] = '└';
        VT100_CHARS[0x6E] = '┼';
        VT100_CHARS[0x71] = '─';
        VT100_CHARS[0x74] = '├';
        VT100_CHARS[0x75] = '┤';
        VT100_CHARS[0x76] = '┴';
        VT100_CHARS[0x77] = '┬';
        VT100_CHARS[0x78] = '│';
    }

    private static final String VT100_BEGIN_SEQ = "\u001B(0";
    private static final String VT100_END_SEQ = "\u001B(B";
    private static final int VT100_BEGIN_SEQ_LENGTH = VT100_BEGIN_SEQ.length();
    private static final int VT100_END_SEQ_LENGTH = VT100_END_SEQ.length();

    public static String translateVT100Escapes(String text) {
        int offset = 0;
        val result = new StringBuilder();
        val textLength = text.length();
        while (offset < textLength) {
            val startIndex = text.indexOf(VT100_BEGIN_SEQ, offset);
            if (startIndex < 0) {
                result.append(text.substring(offset, textLength).replace(VT100_END_SEQ, ""));
                break;
            }
            result.append(text, offset, startIndex);
            val blockOffset = startIndex + VT100_BEGIN_SEQ_LENGTH;
            var endIndex = text.indexOf(VT100_END_SEQ, blockOffset);
            if (endIndex < 0) {
                endIndex = textLength;
            }
            for (int i = blockOffset; i < endIndex; i++) {
                val c = text.charAt(i);
                if (c >= 256) {
                    result.append(c);
                } else {
                    result.append(VT100_CHARS[c]);
                }
            }
            offset = endIndex + VT100_END_SEQ_LENGTH;
        }
        return result.toString();
    }
}
