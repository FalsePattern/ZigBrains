package com.falsepattern.zigbrains.zig.util

import com.intellij.openapi.util.TextRange
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import java.util.regex.Pattern
import java.util.stream.Collectors
import kotlin.math.min

fun CharSequence.escape(): CharSequence {
    val sb = StringBuilder(length)
    this.codePoints().forEachOrdered {
        sb.append(when (it) {
            '\n'.code -> "\\n"
            '\r'.code -> "\\r"
            '\t'.code -> "\\t"
            '\\'.code -> "\\\\"
            '"'.code -> "\\\""
            '\''.code, ' '.code, '!'.code,
            in '#'.code..'&'.code,
            in '('.code..'['.code,
            in ']'.code..'~'.code
                -> Character.toString(it)

            else -> "\\u{" + Integer.toHexString(it) + "}"
        })
    }
    return sb
}

fun CharSequence.decodeReplacements(
    isMultiline: Boolean,
    inputLength: Int = this.length
): List<Pair<TextRange, String>> {
    if (isMultiline)
        return emptyList()

    val result = ArrayList<Pair<TextRange, String>>()
    var index = 0
    while (index + 1 < inputLength) {
        if (this[index] != '\\') {
            index++
            continue
        }

        val length = findEscapementLength(inputLength, index)
        val charCode = toUnicodeChar(index, length)
        val range = TextRange(index, min(index + length + 1, inputLength))
        result.add(Pair(range, Character.toString(charCode)))
        index += range.length
    }
    return result
}

fun CharSequence.unescape(
    isMultiline: Boolean,
    inputLength: Int = if (isMultiline) 0 else this.length
): CharSequence =
    if (isMultiline) {
        // toString done here to decouple input from output in case input is mutable
        this.toString()
    } else {
        // the returned StringBuilder is already decoupled so it's fine
        processReplacements(decodeReplacements(false, inputLength), inputLength)
    }

fun CharSequence.prefixWithTextBlockEscape(
    indent: Int,
    marker: CharSequence,
    indentFirst: Boolean,
    prefixFirst: Boolean,
    newLineAfter: Boolean
): CharSequence {
    val indentStr = if (indent >= 0) {
        if (indent < COMMON_INDENT_COUNT)
            COMMON_INDENTS[indent]
        else
            " ".repeat(indent)
    } else {
        ""
    }
    val parts = NL_MATCHER.split(this, -1)
    val result = StringBuilder(length + (indentStr.length + marker.length) * parts.size + 1)
    if (indentFirst) {
        result.append('\n').append(indentStr)
    }
    if (prefixFirst) {
        result.append(marker)
    }
    result.append(parts.first())
    val partsSize = parts.size
    for (i in 1..<partsSize) {
        result.append('\n').append(indentStr).append(marker).append(parts[i])
    }
    if (newLineAfter)
        result.append('\n').append(indentStr).append(marker)
    return result
}

private const val COMMON_INDENT_COUNT = 32
private val COMMON_INDENTS = Array(COMMON_INDENT_COUNT) { i ->
    " ".repeat(i)
}

private val NL_MATCHER = Pattern.compile("(\\r\\n|\\r|\\n)")

private val ESC_TO_CODE = Int2IntOpenHashMap().apply {
    this['n'.code] = '\n'.code
    this['r'.code] = '\r'.code
    this['t'.code] = '\t'.code
    this['\\'.code] = '\\'.code
    this['"'.code] = '"'.code
    this['\''.code] = '\''.code
}


private fun CharSequence.processReplacements(
    replacements: List<Pair<TextRange, String>>,
    inputLength: Int = this.length
): CharSequence {
    val result = StringBuilder()
    var currentOffset = 0
    for ((range, replacement) in replacements) {
        result.append(this.subSequence(currentOffset, range.startOffset))
        result.append(replacement)
        currentOffset = range.endOffset
    }
    result.append(this.subSequence(currentOffset, inputLength))
    return result
}

private fun CharSequence.findEscapementLength(length: Int, pos: Int): Int {
    if (pos + 1 < length && this[pos] == '\\') {
        val c = this[pos + 1]
        return when (c) {
            'x' -> 3
            'u' -> run {
                if (pos + 2 >= length || this[pos + 2] != '{') {
                    return@run -1
                }
                var digits = 0
                while (pos + 3 + digits < length && this[pos + 3 + digits] != '}') {
                    digits++
                }
                return@run 3 + digits
            }

            else -> 1
        }
    } else {
        return -1
    }
}

private fun CharSequence.toUnicodeChar(pos: Int, length: Int) =
    if (length > 1) {
        val start: Int
        val end: Int
        when (this[pos + 1]) {
            'x' -> {
                start = pos + 2
                end = pos + length + 1
            }

            'u' -> {
                start = pos + 3
                end = pos + length
            }

            else -> throw AssertionError()
        }
        try {
            Integer.parseInt(this, start, end, 16)
        } catch (_: NumberFormatException) {
            '?'.code
        }
    } else {
        val c = this[pos + 1].code
        ESC_TO_CODE.getOrDefault(c, c)
    }
