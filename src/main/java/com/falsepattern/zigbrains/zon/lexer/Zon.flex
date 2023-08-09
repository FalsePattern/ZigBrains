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

WHITE_SPACE=[\s]+
LINE_COMMENT="//" [^\n]* | "////" [^\n]*
COMMENT="///".*

ID=[A-Za-z_][A-Za-z0-9_]* | "@\"" {STRING_CHAR}* \"

STRING_CHAR=( [^\\\"] | \\[^] )
STRING_LITERAL_SINGLE=\"{STRING_CHAR}*\"
LINE_STRING=(\\\\ [^\n]* [ \n]*)+

%%

<YYINITIAL> {
      {WHITE_SPACE}            { return WHITE_SPACE; }
      "."                      { return DOT; }
      "{"                      { return LBRACE; }
      "}"                      { return RBRACE; }
      "="                      { return EQ; }
      ","                      { return COMMA; }
      {COMMENT}                { return COMMENT; }
      {LINE_COMMENT}           { return COMMENT; }

      {ID}                     { return ID; }
      {STRING_LITERAL_SINGLE}  { return STRING_LITERAL_SINGLE; }
      {LINE_STRING}            { return LINE_STRING; }
}

[^] { return BAD_CHARACTER; }
