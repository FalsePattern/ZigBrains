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

import java.util.ArrayList;
import java.util.List;

public class CollectionUtil {

    @SafeVarargs
    public static <T> List<T> concat(List<T>... lists) {
        val res = new ArrayList<T>();
        for (List<T> list : lists) {
            res.addAll(list);
        }
        return res;
    }

    @SafeVarargs
    public static <T> List<T> concat(T[] a, List<T>... b) {
        val res = new ArrayList<>(List.of(a));
        for (List<T> list : b) {
            res.addAll(list);
        }
        return res;
    }

    @SafeVarargs
    public static <T> List<T> concat(List<T> a, T... b) {
        val res = new ArrayList<>(a);
        res.addAll(List.of(b));
        return res;
    }

    @SafeVarargs
    public static <T> List<T> concat(List<T> a, T[]... b) {
        val res = new ArrayList<>(a);
        for (val arr: b) {
            res.addAll(List.of(arr));
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> concat(T[]... arrays) {
        val res = new ArrayList<T>();
        for (val arr: arrays) {
            res.addAll(List.of(arr));
        }
        return res;
    }
}
