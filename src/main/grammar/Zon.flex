/*
 * Copyright 2023 FalsePattern
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
