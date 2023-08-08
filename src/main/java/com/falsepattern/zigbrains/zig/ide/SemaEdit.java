package com.falsepattern.zigbrains.zig.ide;

import java.util.List;

public record SemaEdit(int start, int remove, List<SemaRange> add) {
}
