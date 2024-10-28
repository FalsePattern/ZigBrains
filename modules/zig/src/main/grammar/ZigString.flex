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
package com.falsepattern.zigbrains.zig.stringlexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.*;
import static com.intellij.psi.StringEscapesTokenTypes.*;

%%

%public
%class ZigStringLexer
%implements FlexLexer
%function advance
%type IElementType
%{
    public ZigStringLexer() {

    }
%}

hex=[0-9a-fA-F]

char_escape_unicode= "\\x" {hex} {hex} | "\\u{" {hex}+ "}"
char_escape_unicode_invalid= "\\x" | "\\u"

char_escape_single_valid= "\\" [nr\\t'\"]
char_escape_single_invalid= "\\" [^nr\\t'\"]

%state STR
%%


<YYINITIAL> {
      "\"" { yybegin(STR); return STRING_LITERAL_SINGLE; }
      [^]  { return STRING_LITERAL_SINGLE; }
}

<STR> {
      {char_escape_unicode} { return VALID_STRING_ESCAPE_TOKEN; }
      {char_escape_unicode_invalid} { return INVALID_UNICODE_ESCAPE_TOKEN; }
      {char_escape_single_valid} { return VALID_STRING_ESCAPE_TOKEN; }
      {char_escape_single_invalid} { return INVALID_CHARACTER_ESCAPE_TOKEN; }
      [^] { return STRING_LITERAL_SINGLE; }
}
