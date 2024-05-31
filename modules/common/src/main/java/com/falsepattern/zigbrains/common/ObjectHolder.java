package com.falsepattern.zigbrains.common;

import org.jetbrains.annotations.NotNull;

/**
 * Evil hack for bypassing plugin verifier restrictions via generics. Unfortunately this is necessary due to public api limitations.
 */
public record ObjectHolder<T>(@NotNull T value) {
}
