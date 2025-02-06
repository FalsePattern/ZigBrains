/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2025 FalsePattern
 * All Rights Reserved
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * ZigBrains is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 * ZigBrains is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ZigBrains. If not, see <https://www.gnu.org/licenses/>.
 */
package com.falsepattern.zigbrains.zig.lexerstring;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.*;
import static com.intellij.psi.StringEscapesTokenTypes.*;

%%

%public
%class ZigLexerString
%implements FlexLexer
%function advance
%type IElementType

hex=[0-9a-fA-F]

char_escape_unicode= "\\x" {hex} {hex} | "\\u{" {hex}+ "}"
char_escape_unicode_invalid= "\\x" .? .? | "\\u" ("{" [^}]* "}"?)?

char_escape_single_valid= "\\" [nr\\t'\"]
char_escape_single_invalid= "\\" [^nr\\t'\"]

%state STR
%state CHAR
%state CHAR_END
%state CHAR_FINISH
%%


<YYINITIAL> {
      "\"" { yybegin(STR); return STRING_LITERAL_SINGLE; }
      "'"  { yybegin(CHAR); return CHAR_LITERAL; }
      [^]  { return STRING_LITERAL_SINGLE; }
}

<STR> {
      {char_escape_unicode} { return VALID_STRING_ESCAPE_TOKEN; }
      {char_escape_unicode_invalid} { return INVALID_UNICODE_ESCAPE_TOKEN; }
      {char_escape_single_valid} { return VALID_STRING_ESCAPE_TOKEN; }
      {char_escape_single_invalid} { return INVALID_CHARACTER_ESCAPE_TOKEN; }
      [^] { return STRING_LITERAL_SINGLE; }
}

<CHAR> {
      {char_escape_unicode} { yybegin(CHAR_END); return VALID_STRING_ESCAPE_TOKEN; }
      {char_escape_unicode_invalid} { yybegin(CHAR_END); return INVALID_UNICODE_ESCAPE_TOKEN; }
      {char_escape_single_valid} { yybegin(CHAR_END); return VALID_STRING_ESCAPE_TOKEN; }
      {char_escape_single_invalid} { yybegin(CHAR_END); return INVALID_CHARACTER_ESCAPE_TOKEN; }
      [^] { yybegin(CHAR_END); return CHAR_LITERAL; }
}

<CHAR_END> {
      "'" { yybegin(CHAR_FINISH); return CHAR_LITERAL; }
      [^] { return BAD_CHARACTER; }
}

<CHAR_FINISH> {
      [^] { return BAD_CHARACTER; }
}