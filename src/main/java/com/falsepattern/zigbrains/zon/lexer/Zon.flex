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
