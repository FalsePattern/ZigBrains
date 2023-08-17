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

ox80_oxBF=[\200-\277]
oxF4=\364
ox80_ox8F=[\200-\217]
oxF1_oxF3=[\361-\363]
oxF0=\360
ox90_0xBF=[\220-\277]
oxEE_oxEF=[\356-\357]
oxED=\355
ox80_ox9F=[\200-\237]
oxE1_oxEC=[\341-\354]
oxE0=\340
oxA0_oxBF=[\240-\277]
oxC2_oxDF=[\302-\337]

// From https://lemire.me/blog/2018/05/09/how-quickly-can-you-check-that-a-string-is-valid-unicode-utf-8/
// First Byte      Second Byte     Third Byte      Fourth Byte
// [0x00,0x7F]
// [0xC2,0xDF]     [0x80,0xBF]
//    0xE0         [0xA0,0xBF]     [0x80,0xBF]
// [0xE1,0xEC]     [0x80,0xBF]     [0x80,0xBF]
//    0xED         [0x80,0x9F]     [0x80,0xBF]
// [0xEE,0xEF]     [0x80,0xBF]     [0x80,0xBF]
//    0xF0         [0x90,0xBF]     [0x80,0xBF]     [0x80,0xBF]
// [0xF1,0xF3]     [0x80,0xBF]     [0x80,0xBF]     [0x80,0xBF]
//    0xF4         [0x80,0x8F]     [0x80,0xBF]     [0x80,0xBF]

mb_utf8_literal= {oxF4}      {ox80_ox8F} {ox80_oxBF} {ox80_oxBF}
               | {oxF1_oxF3} {ox80_oxBF} {ox80_oxBF} {ox80_oxBF}
               | {oxF0}      {ox90_0xBF} {ox80_oxBF} {ox80_oxBF}
               | {oxEE_oxEF} {ox80_oxBF} {ox80_oxBF}
               | {oxED}      {ox80_ox9F} {ox80_oxBF}
               | {oxE1_oxEC} {ox80_oxBF} {ox80_oxBF}
               | {oxE0}      {oxA0_oxBF} {ox80_oxBF}
               | {oxC2_oxDF} {ox80_oxBF}

ascii_char_not_nl_slash_squote=[\000-\011\013-\046\050-\133\135-\177]

char_escape= "\\x" {hex} {hex}
           | "\\u{" {hex}+ "}"
           | "\\" [nr\\t'\"]
char_char= {mb_utf8_literal}
         | {char_escape}
         | {ascii_char_not_nl_slash_squote}

string_char= {char_escape}
           | [^\\\"\n]

CONTAINER_DOC_COMMENT=("//!" [^\n]* [ \n]*)+
DOC_COMMENT=("///" [^\n]* [ \n]*)+
LINE_COMMENT="//" [^\n]* | "////" [^\n]*
line_string=("\\\\" [^\n]* [ \n]*)+

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
%state CHAR_LIT

%state ID_QUOT
%state UNT_QUOT

%state CDOC_CMT
%state DOC_CMT
%state LINE_CMT
%%

//Comments

<YYINITIAL>      "//!"                    { yypushback(3); yybegin(CDOC_CMT); }
<CDOC_CMT>       {CONTAINER_DOC_COMMENT}  { yybegin(YYINITIAL); return CONTAINER_DOC_COMMENT; }

<YYINITIAL>      "///"                    { yypushback(3); yybegin(DOC_CMT); }
<DOC_CMT>        {DOC_COMMENT}            { yybegin(YYINITIAL); return DOC_COMMENT; }

<YYINITIAL>      "//"                     { yypushback(2); yybegin(LINE_CMT); }
<LINE_CMT>       {LINE_COMMENT}           { yybegin(YYINITIAL); return LINE_COMMENT; }

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
<CHAR_LIT>       {char_char}"'"           { yybegin(YYINITIAL); return CHAR_LITERAL; }
<CHAR_LIT>       [^]                      { yypushback(1); yybegin(UNT_QUOT); }

<YYINITIAL>      {FLOAT}                  { return FLOAT; }
<YYINITIAL>      {INTEGER}                { return INTEGER; }

<YYINITIAL>      "\""                     { yybegin(STR_LIT); }
<STR_LIT>        {string_char}*"\""       { yybegin(YYINITIAL); return STRING_LITERAL_SINGLE; }
<STR_LIT>        [^]                      { yypushback(1); yybegin(UNT_QUOT); }
<YYINITIAL>      {line_string}+           { return STRING_LITERAL_MULTI; }

<YYINITIAL>      {IDENTIFIER_PLAIN}       { return IDENTIFIER; }
<YYINITIAL>      "@\""                    { yybegin(ID_QUOT); }
<ID_QUOT>        {string_char}*"\""       { yybegin(YYINITIAL); return IDENTIFIER; }
<ID_QUOT>        [^]                      { yypushback(1); yybegin(UNT_QUOT); }

<YYINITIAL>      {BUILTINIDENTIFIER}      { return BUILTINIDENTIFIER; }

<UNT_QUOT>       [^\n]*{CRLF}             { yybegin(YYINITIAL); return BAD_CHARACTER; }

<YYINITIAL>      {WHITE_SPACE}            { return WHITE_SPACE; }

[^] { return BAD_CHARACTER; }
