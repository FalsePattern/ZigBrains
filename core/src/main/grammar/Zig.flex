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
package com.falsepattern.zigbrains.zig.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.falsepattern.zigbrains.zig.psi.ZigTypes.*;

%%

%class ZigFlexLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

CRLF=\R
WHITE_SPACE=[\s]+

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
         | [^\'\n]

string_char= \\ .
           | [^\"\n]

all_nl_wrap=[^\n]* [ \n]*
all_no_nl=[^\n]+


FLOAT= "0x" {hex_int} "." {hex_int} ([pP] [-+]? {dec_int})?
     |      {dec_int} "." {dec_int} ([eE] [-+]? {dec_int})?
     | "0x" {hex_int} [pP] [-+]? {dec_int}
     |      {dec_int} [eE] [-+]? {dec_int}

INTEGER= "0b" {bin_int}
       | "0o" {oct_int}
       | "0x" {hex_int}
       |      {dec_int}

IDENTIFIER_PLAIN=[A-Za-z_][A-Za-z0-9_]*
BUILTINIDENTIFIER="@"[A-Za-z_][A-Za-z0-9_]*

%state STR_LIT
%state STR_MULT_LINE
%state CHAR_LIT

%state ID_QUOT
%state UNT_SQUOT
%state UNT_DQUOT

%state CDOC_CMT
%state DOC_CMT
%state LINE_CMT
%%

//Comments

<YYINITIAL>      "//!"                    { yybegin(CDOC_CMT); }
<CDOC_CMT>       {all_nl_wrap} "//!"      { }
<CDOC_CMT>       {all_no_nl}          { }
<CDOC_CMT>       \n                       { yybegin(YYINITIAL); return CONTAINER_DOC_COMMENT; }
<CDOC_CMT>       <<EOF>>                  { yybegin(YYINITIAL); return CONTAINER_DOC_COMMENT; }

<YYINITIAL>      "///"                    { yybegin(DOC_CMT); }
<DOC_CMT>        {all_nl_wrap} "///"      { }
<DOC_CMT>        {all_no_nl}          { }
<DOC_CMT>        \n                       { yybegin(YYINITIAL); return DOC_COMMENT; }
<DOC_CMT>        <<EOF>>                  { yybegin(YYINITIAL); return DOC_COMMENT; }

<YYINITIAL>      "//"                     { yybegin(LINE_CMT); }
<LINE_CMT>       {all_nl_wrap} "//"       { }
<LINE_CMT>       {all_no_nl}          { }
<LINE_CMT>       \n                       { yybegin(YYINITIAL); return LINE_COMMENT; }
<LINE_CMT>       <<EOF>>                  { yybegin(YYINITIAL); return LINE_COMMENT; }

//Symbols
<YYINITIAL>      "&"                      { return AMPERSAND; }
<YYINITIAL>      "&="                     { return AMPERSANDEQUAL; }
<YYINITIAL>      "*"                      { return ASTERISK; }
<YYINITIAL>      "**"                     { return ASTERISK2; }
<YYINITIAL>      "*="                     { return ASTERISKEQUAL; }
<YYINITIAL>      "*%"                     { return ASTERISKPERCENT; }
<YYINITIAL>      "*%="                    { return ASTERISKPERCENTEQUAL; }
<YYINITIAL>      "*|"                     { return ASTERISKPIPE; }
<YYINITIAL>      "*|="                    { return ASTERISKPIPEEQUAL; }
<YYINITIAL>      "^"                      { return CARET; }
<YYINITIAL>      "^="                     { return CARETEQUAL; }
<YYINITIAL>      ":"                      { return COLON; }
<YYINITIAL>      ","                      { return COMMA; }
<YYINITIAL>      "."                      { return DOT; }
<YYINITIAL>      ".."                     { return DOT2; }
<YYINITIAL>      "..."                    { return DOT3; }
<YYINITIAL>      ".*"                     { return DOTASTERISK; }
<YYINITIAL>      ".?"                     { return DOTQUESTIONMARK; }
<YYINITIAL>      "="                      { return EQUAL; }
<YYINITIAL>      "=="                     { return EQUALEQUAL; }
<YYINITIAL>      "=>"                     { return EQUALRARROW; }
<YYINITIAL>      "!"                      { return EXCLAMATIONMARK; }
<YYINITIAL>      "!="                     { return EXCLAMATIONMARKEQUAL; }
<YYINITIAL>      "<"                      { return LARROW; }
<YYINITIAL>      "<<"                     { return LARROW2; }
<YYINITIAL>      "<<="                    { return LARROW2EQUAL; }
<YYINITIAL>      "<<|"                    { return LARROW2PIPE; }
<YYINITIAL>      "<<|="                   { return LARROW2PIPEEQUAL; }
<YYINITIAL>      "<="                     { return LARROWEQUAL; }
<YYINITIAL>      "{"                      { return LBRACE; }
<YYINITIAL>      "["                      { return LBRACKET; }
<YYINITIAL>      "("                      { return LPAREN; }
<YYINITIAL>      "-"                      { return MINUS; }
<YYINITIAL>      "-="                     { return MINUSEQUAL; }
<YYINITIAL>      "-%"                     { return MINUSPERCENT; }
<YYINITIAL>      "-%="                    { return MINUSPERCENTEQUAL; }
<YYINITIAL>      "-|"                     { return MINUSPIPE; }
<YYINITIAL>      "-|="                    { return MINUSPIPEEQUAL; }
<YYINITIAL>      "->"                     { return MINUSRARROW; }
<YYINITIAL>      "%"                      { return PERCENT; }
<YYINITIAL>      "%="                     { return PERCENTEQUAL; }
<YYINITIAL>      "|"                      { return PIPE; }
<YYINITIAL>      "||"                     { return PIPE2; }
<YYINITIAL>      "|="                     { return PIPEEQUAL; }
<YYINITIAL>      "+"                      { return PLUS; }
<YYINITIAL>      "++"                     { return PLUS2; }
<YYINITIAL>      "+="                     { return PLUSEQUAL; }
<YYINITIAL>      "+%"                     { return PLUSPERCENT; }
<YYINITIAL>      "+%="                    { return PLUSPERCENTEQUAL; }
<YYINITIAL>      "+|"                     { return PLUSPIPE; }
<YYINITIAL>      "+|="                    { return PLUSPIPEEQUAL; }
//This one is directly in the tokenizer, because it conflicts with identifiers without context
//<YYINITIAL>      "c"                      { return LETTERC; }
<YYINITIAL>      "?"                      { return QUESTIONMARK; }
<YYINITIAL>      ">"                      { return RARROW; }
<YYINITIAL>      ">>"                     { return RARROW2; }
<YYINITIAL>      ">>="                    { return RARROW2EQUAL; }
<YYINITIAL>      ">="                     { return RARROWEQUAL; }
<YYINITIAL>      "}"                      { return RBRACE; }
<YYINITIAL>      "]"                      { return RBRACKET; }
<YYINITIAL>      ")"                      { return RPAREN; }
<YYINITIAL>      ";"                      { return SEMICOLON; }
<YYINITIAL>      "/"                      { return SLASH; }
<YYINITIAL>      "/="                     { return SLASHEQUAL; }
<YYINITIAL>      "~"                      { return TILDE; }

// keywords
<YYINITIAL>      "addrspace"              { return KEYWORD_ADDRSPACE; }
<YYINITIAL>      "align"                  { return KEYWORD_ALIGN; }
<YYINITIAL>      "allowzero"              { return KEYWORD_ALLOWZERO; }
<YYINITIAL>      "and"                    { return KEYWORD_AND; }
<YYINITIAL>      "anyframe"               { return KEYWORD_ANYFRAME; }
<YYINITIAL>      "anytype"                { return KEYWORD_ANYTYPE; }
<YYINITIAL>      "asm"                    { return KEYWORD_ASM; }
<YYINITIAL>      "async"                  { return KEYWORD_ASYNC; }
<YYINITIAL>      "await"                  { return KEYWORD_AWAIT; }
<YYINITIAL>      "break"                  { return KEYWORD_BREAK; }
<YYINITIAL>      "callconv"               { return KEYWORD_CALLCONV; }
<YYINITIAL>      "catch"                  { return KEYWORD_CATCH; }
<YYINITIAL>      "comptime"               { return KEYWORD_COMPTIME; }
<YYINITIAL>      "const"                  { return KEYWORD_CONST; }
<YYINITIAL>      "continue"               { return KEYWORD_CONTINUE; }
<YYINITIAL>      "defer"                  { return KEYWORD_DEFER; }
<YYINITIAL>      "else"                   { return KEYWORD_ELSE; }
<YYINITIAL>      "enum"                   { return KEYWORD_ENUM; }
<YYINITIAL>      "errdefer"               { return KEYWORD_ERRDEFER; }
<YYINITIAL>      "error"                  { return KEYWORD_ERROR; }
<YYINITIAL>      "export"                 { return KEYWORD_EXPORT; }
<YYINITIAL>      "extern"                 { return KEYWORD_EXTERN; }
<YYINITIAL>      "fn"                     { return KEYWORD_FN; }
<YYINITIAL>      "for"                    { return KEYWORD_FOR; }
<YYINITIAL>      "if"                     { return KEYWORD_IF; }
<YYINITIAL>      "inline"                 { return KEYWORD_INLINE; }
<YYINITIAL>      "noalias"                { return KEYWORD_NOALIAS; }
<YYINITIAL>      "nosuspend"              { return KEYWORD_NOSUSPEND; }
<YYINITIAL>      "noinline"               { return KEYWORD_NOINLINE; }
<YYINITIAL>      "opaque"                 { return KEYWORD_OPAQUE; }
<YYINITIAL>      "or"                     { return KEYWORD_OR; }
<YYINITIAL>      "orelse"                 { return KEYWORD_ORELSE; }
<YYINITIAL>      "packed"                 { return KEYWORD_PACKED; }
<YYINITIAL>      "pub"                    { return KEYWORD_PUB; }
<YYINITIAL>      "resume"                 { return KEYWORD_RESUME; }
<YYINITIAL>      "return"                 { return KEYWORD_RETURN; }
<YYINITIAL>      "linksection"            { return KEYWORD_LINKSECTION; }
<YYINITIAL>      "struct"                 { return KEYWORD_STRUCT; }
<YYINITIAL>      "suspend"                { return KEYWORD_SUSPEND; }
<YYINITIAL>      "switch"                 { return KEYWORD_SWITCH; }
<YYINITIAL>      "test"                   { return KEYWORD_TEST; }
<YYINITIAL>      "threadlocal"            { return KEYWORD_THREADLOCAL; }
<YYINITIAL>      "try"                    { return KEYWORD_TRY; }
<YYINITIAL>      "union"                  { return KEYWORD_UNION; }
<YYINITIAL>      "unreachable"            { return KEYWORD_UNREACHABLE; }
<YYINITIAL>      "usingnamespace"         { return KEYWORD_USINGNAMESPACE; }
<YYINITIAL>      "var"                    { return KEYWORD_VAR; }
<YYINITIAL>      "volatile"               { return KEYWORD_VOLATILE; }
<YYINITIAL>      "while"                  { return KEYWORD_WHILE; }

<YYINITIAL>      "'"                      { yybegin(CHAR_LIT); }
<CHAR_LIT>       {char_char}*"'"          { yybegin(YYINITIAL); return CHAR_LITERAL; }
<CHAR_LIT>       <<EOF>>                  { yybegin(YYINITIAL); return BAD_SQUOT; }
<CHAR_LIT>       [^]                      { yypushback(1); yybegin(UNT_SQUOT); }

<YYINITIAL>      {FLOAT}                  { return FLOAT; }
<YYINITIAL>      {INTEGER}                { return INTEGER; }

<YYINITIAL>      "\""                     { yybegin(STR_LIT); }
<STR_LIT>        {string_char}*"\""       { yybegin(YYINITIAL); return STRING_LITERAL_SINGLE; }
<STR_LIT>        <<EOF>>                  { yybegin(YYINITIAL); return BAD_DQUOT; }
<STR_LIT>        [^]                      { yypushback(1); yybegin(UNT_DQUOT); }
<YYINITIAL>      "\\\\"                   { yybegin(STR_MULT_LINE); }
<STR_MULT_LINE>  {all_nl_wrap} "\\\\"     { }
<STR_MULT_LINE>  {all_no_nl}          { }
<STR_MULT_LINE>  \n                       { yybegin(YYINITIAL); return STRING_LITERAL_MULTI; }
<STR_MULT_LINE>  <<EOF>>                  { yybegin(YYINITIAL); return STRING_LITERAL_MULTI; }

<YYINITIAL>      {IDENTIFIER_PLAIN}       { return IDENTIFIER; }
<YYINITIAL>      "@\""                    { yybegin(ID_QUOT); }
<ID_QUOT>        {string_char}*"\""       { yybegin(YYINITIAL); return IDENTIFIER; }
<ID_QUOT>        <<EOF>>                  { yybegin(YYINITIAL); return BAD_DQUOT; }
<ID_QUOT>        [^]                      { yypushback(1); yybegin(UNT_DQUOT); }

<YYINITIAL>      {BUILTINIDENTIFIER}      { return BUILTINIDENTIFIER; }

<UNT_SQUOT>       <<EOF>>                 { yybegin(YYINITIAL); return BAD_SQUOT; }
<UNT_SQUOT>       {CRLF}                  { yybegin(YYINITIAL); return BAD_SQUOT; }
<UNT_SQUOT>       {all_no_nl}             { }
<UNT_DQUOT>       <<EOF>>                 { yybegin(YYINITIAL); return BAD_DQUOT; }
<UNT_DQUOT>       {CRLF}                  { yybegin(YYINITIAL); return BAD_DQUOT; }
<UNT_DQUOT>       {all_no_nl}             { }

<YYINITIAL>      {WHITE_SPACE}            { return WHITE_SPACE; }

[^] { return BAD_CHARACTER; }
