/*
 * This file is part of ZigBrains.
 *
 * Copyright (C) 2023-2024 FalsePattern
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
package com.falsepattern.zigbrains.zon.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.falsepattern.zigbrains.zon.psi.ZonTypes.*;

%%

%class ZonFlexLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

CRLF=\R
WHITE_SPACE=[\s]+
LINE_COMMENT="//" [^\n]* | "////" [^\n]*
COMMENT="///".*

ID=[A-Za-z_][A-Za-z0-9_]*

hex=[0-9a-fA-F]
char_escape
    = "\\x" {hex} {hex}
    | "\\u{" {hex}+ "}"
    | "\\" [nr\\t'\"]

string_char
    = {char_escape}
    | [^\\\"\n]

LINE_STRING=("\\\\" [^\n]* [ \n]*)+

%state STRING_LITERAL
%state ID_STRING
%state UNCLOSED_STRING
%%


<YYINITIAL>      {WHITE_SPACE}            { return WHITE_SPACE; }
<YYINITIAL>      "."                      { return DOT; }
<YYINITIAL>      "IntellijIdeaRulezzz"    { return INTELLIJ_COMPLETION_DUMMY; }
<YYINITIAL>      "{"                      { return LBRACE; }
<YYINITIAL>      "}"                      { return RBRACE; }
<YYINITIAL>      "="                      { return EQ; }
<YYINITIAL>      ","                      { return COMMA; }
<YYINITIAL>      "true"                   { return BOOL_TRUE; }
<YYINITIAL>      "false"                  { return BOOL_FALSE; }
<YYINITIAL>      {COMMENT}                { return COMMENT; }
<YYINITIAL>      {LINE_COMMENT}           { return COMMENT; }

<YYINITIAL>      {ID}                     { return ID; }
<YYINITIAL>      "@\""                    { yybegin(ID_STRING); }
<ID_STRING>      {string_char}*"\""       { yybegin(YYINITIAL); return ID; }
<ID_STRING>      [^]                      { yypushback(1); yybegin(UNCLOSED_STRING); }

<YYINITIAL>      "\""                     { yybegin(STRING_LITERAL); }
<STRING_LITERAL> {string_char}*"\""       { yybegin(YYINITIAL); return STRING_LITERAL_SINGLE; }
<STRING_LITERAL> [^]                      { yypushback(1); yybegin(UNCLOSED_STRING); }

<UNCLOSED_STRING>[^\n]*{CRLF}             { yybegin(YYINITIAL); return BAD_STRING; }

<YYINITIAL>      {LINE_STRING}            { return LINE_STRING; }

[^] { return BAD_CHARACTER; }
