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

WHITE_SPACE=\s+

// visual studio parity
LF=\r\n?|[\n\u0085\u2028\u2029]

bin=[01]
bin_="_"? {bin}
oct=[0-7]
oct_="_"? {oct}
hex=[0-9a-fA-F]
hex_="_"? {hex}
dec=[0-9]
dec_="_"? {dec}

bin_int={bin} {bin_}*
oct_int={oct} {oct_}*
dec_int={dec} {dec_}*
hex_int={hex} {hex_}*

char_char= \\ .
         | [^\'\r\n\u0085\u2028\u2029]

string_char= \\ .
           | [^\"\r\n\u0085\u2028\u2029]

nl_wrap={LF} (\s|{LF})*
all_no_nl=[^\r\n\u0085\u2028\u2029]+


FLOAT= "0x" {hex_int} "." {hex_int} ([pP] [-+]? {dec_int})?
     |      {dec_int} "." {dec_int} ([eE] [-+]? {dec_int})?
     | "0x" {hex_int} [pP] [-+]? {dec_int}
     |      {dec_int} [eE] [-+]? {dec_int}

INTEGER= "0b" {bin_int}
       | "0o" {oct_int}
       | "0x" {hex_int}
       |      {dec_int}

IDENTIFIER_PLAIN=[A-Za-z_][A-Za-z0-9_]*

%state STR_LIT
%state STR_MULT_LINE
%state CHAR_LIT

%state ID_QUOT
%state UNT_SQUOT
%state UNT_DQUOT

%state CMT_LINE
%%

//Comments

<YYINITIAL>      "//"                     { yybegin(CMT_LINE); }
<CMT_LINE>       {all_no_nl}              { }
<CMT_LINE>       {nl_wrap} "//"           { }
<CMT_LINE>       \R                       { yybegin(YYINITIAL); return LINE_COMMENT; }
<CMT_LINE>       <<EOF>>                  { yybegin(YYINITIAL); return LINE_COMMENT; }

//Symbols

<YYINITIAL>      "."                      { return DOT; }
<YYINITIAL>      "="                      { return EQUAL; }
<YYINITIAL>      "{"                      { return LBRACE; }
<YYINITIAL>      "}"                      { return RBRACE; }
<YYINITIAL>      ","                      { return COMMA; }

//Keywords

<YYINITIAL>      "false"                  { return KEYWORD_FALSE; }
<YYINITIAL>      "true"                   { return KEYWORD_TRUE; }
<YYINITIAL>      "null"                   { return KEYWORD_NULL; }
<YYINITIAL>      "nan"                    { return NUM_NAN; }
<YYINITIAL>      "inf"                    { return NUM_INF; }

//Strings

<YYINITIAL>      "'"                      { yybegin(CHAR_LIT); }
<CHAR_LIT>       {char_char}*"'"          { yybegin(YYINITIAL); return CHAR_LITERAL; }
<CHAR_LIT>       <<EOF>>                  { yybegin(YYINITIAL); return BAD_SQUOT; }
<CHAR_LIT>       [^]                      { yypushback(1); yybegin(UNT_SQUOT); }

<YYINITIAL>      "\""                     { yybegin(STR_LIT); }
<STR_LIT>        {string_char}*"\""       { yybegin(YYINITIAL); return STRING_LITERAL_SINGLE; }
<STR_LIT>        <<EOF>>                  { yybegin(YYINITIAL); return BAD_DQUOT; }
<STR_LIT>        [^]                      { yypushback(1); yybegin(UNT_DQUOT); }

<YYINITIAL>      "\\\\"                   { yybegin(STR_MULT_LINE); }
<STR_MULT_LINE>  {all_no_nl}              { }
<STR_MULT_LINE>  {nl_wrap} "\\\\"         { }
<STR_MULT_LINE>  {LF}                     { yybegin(YYINITIAL); return STRING_LITERAL_MULTI; }
<STR_MULT_LINE>  <<EOF>>                  { yybegin(YYINITIAL); return STRING_LITERAL_MULTI; }

//Numbers

<YYINITIAL>      {FLOAT}                  { return FLOAT; }
<YYINITIAL>      {INTEGER}                { return INTEGER; }

//Identifiers

<YYINITIAL>      {IDENTIFIER_PLAIN}       { return IDENTIFIER; }
<YYINITIAL>      "@\""                    { yybegin(ID_QUOT); }
<ID_QUOT>        {string_char}*"\""       { yybegin(YYINITIAL); return IDENTIFIER; }
<ID_QUOT>        <<EOF>>                  { yybegin(YYINITIAL); return BAD_DQUOT; }
<ID_QUOT>        [^]                      { yypushback(1); yybegin(UNT_DQUOT); }

//Error handling

<UNT_SQUOT>       <<EOF>>                 { yybegin(YYINITIAL); return BAD_SQUOT; }
<UNT_SQUOT>       {LF}                    { yybegin(YYINITIAL); return BAD_SQUOT; }
<UNT_SQUOT>       {all_no_nl}             { }
<UNT_DQUOT>       <<EOF>>                 { yybegin(YYINITIAL); return BAD_DQUOT; }
<UNT_DQUOT>       {LF}                    { yybegin(YYINITIAL); return BAD_DQUOT; }
<UNT_DQUOT>       {all_no_nl}             { }

//Misc

<YYINITIAL>      {WHITE_SPACE}            { return WHITE_SPACE; }

[^] { return BAD_CHARACTER; }
