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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CollectionUtil {
    public static <T> List<T> concat(List<T> a, List<T> b) {
        val res = new ArrayList<>(a);
        res.addAll(b);
        return res;
    }

    public static <T> List<T> concat(T[] a, List<T> b) {
        val res = new ArrayList<>(List.of(a));
        res.addAll(b);
        return res;
    }

    @SafeVarargs
    public static <T> List<T> concat(List<T> a, T... b) {
        val res = new ArrayList<>(a);
        res.addAll(List.of(b));
        return res;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] concat(T[]... arrays) {
        if (null != arrays && 0 != arrays.length) {
            int resultLength = (Integer)java.util.Arrays.stream(arrays).filter(Objects::nonNull).map((e) -> {
                return e.length;
            }).reduce(0, Integer::sum);
            T[] resultArray = (T[]) Array.newInstance(arrays[0].getClass().getComponentType(), resultLength);
            int i = 0;
            int n = arrays.length;

            for(int curr = 0; i < n; ++i) {
                T[] array = arrays[i];
                if (null != array) {
                    int length = array.length;
                    System.arraycopy(array, 0, resultArray, curr, length);
                    curr += length;
                }
            }

            return resultArray;
        } else {
            return null;
        }
    }
}
