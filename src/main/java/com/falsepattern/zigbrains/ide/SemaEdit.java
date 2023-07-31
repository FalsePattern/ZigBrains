package com.falsepattern.zigbrains.ide;

import java.util.List;

public record SemaEdit(int start, int remove, List<SemaRange> add) {
}
