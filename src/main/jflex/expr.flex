/* JFlex example: part of Java language lexer specification */
package com.trustwave.dbpjobservice.expr;

import java_cup.runtime.*;

/**
 * This class is a simple example lexer.
 */
%%

%class Lexer
%cupsym Sym
%unicode
%cup
%char

%{
  StringBuffer buf = new StringBuffer();

  private Symbol sym(int type) {
    return new Symbol(type, yychar, yychar+yytext().length());
  }
  private Symbol sym(int type, Object value) {
    return new Symbol(type, yychar, yychar+yytext().length(), value);
  }
%}

LineTerminator   = \r|\n|\r\n
WhiteSpace       = {LineTerminator} | [ \t\f]
Identifier       = [:jletter:] [:jletterdigit:]*
QuotedIdentifier = "[" [:jletterdigit:]+ ([ ][:jletterdigit:]+)* "]"
QuotedInvalid    = "[" [^\]]* "]"
DecInteger       = 0 | [1-9][0-9]*

%state STRING
%state STRING1

%%

/* keywords */
<YYINITIAL> {
  "+"                       { return sym(Sym.PLUS, yytext());    }
  "-"                       { return sym(Sym.MINUS, yytext());   }
  "*"                       { return sym(Sym.TIMES, yytext());   }
  "/"                       { return sym(Sym.DIVIDE, yytext());  }
  "%"|"mod"|"MOD"|"Mod"     { return sym(Sym.MOD, yytext());     }
  "=="|"="|"eq"|"EQ"|"Eq"   { return sym(Sym.EQ, yytext());      }
  "!="|"<>"|"ne"|"NE"|"Ne"  { return sym(Sym.NEQ, yytext());     }
  "like"|"Like"|"LIKE"      { return sym(Sym.LIKE, yytext());   }
  "~"|"matches"|"MATCHES"   { return sym(Sym.MATCH, yytext());   }
  "!~"                      { return sym(Sym.NMATCH, yytext());  }
  ">"|"gt"|"GT"|"Gt"        { return sym(Sym.GT, yytext());      }
  ">="|"ge"|"GE"|"Ge"       { return sym(Sym.GE, yytext());      }
  "<"|"lt"|"LT"|"Lt"        { return sym(Sym.LT, yytext());      }
  "<="|"le"|"LE"|"Le"       { return sym(Sym.LE, yytext());      }
  "and"|"AND"|"And"|"&&"    { return sym(Sym.AND, yytext());     }
  "or"|"OR"|"Or"|"||"       { return sym(Sym.OR, yytext());      }
  "not"|"NOT"|"Not"|"!"     { return sym(Sym.NOT, yytext());     }
  "in"|"IN"|"In"            { return sym(Sym.IN, yytext());      }
  "("                       { return sym(Sym.LPAREN, yytext());  }
  ")"                       { return sym(Sym.RPAREN, yytext());  }
  "?"                       { return sym(Sym.QUESTION, yytext());}
  ":"                       { return sym(Sym.COLON, yytext());   }
  "?:"                      { return sym(Sym.DEFAULT, yytext()); }
  "."                       { return sym(Sym.DOT, yytext());     }
  ","                       { return sym(Sym.COMMA, yytext());   }
  "true"|"TRUE"|"True"      { return sym(Sym.TRUE, yytext());    }
  "false"|"FALSE"|"False"   { return sym(Sym.FALSE, yytext());   }
  "null"|"NULL"|"Null"      { return sym(Sym.NULL, yytext());    }
  "exists"|"EXISTS""Exists" { return sym(Sym.EXISTS, yytext());  }
  "isNum"|"isnum"|"ISNUM"|"IsNum"|"isNumber"|"isnumber"|"ISNUMBER"|"IsNumber" 
                            { return sym(Sym.ISNUMBER, yytext());}
  "empty"|"EMPTY"|"Empty"   { return sym(Sym.EMPTY, yytext());   }
}

<YYINITIAL> {
  {Identifier}        { return sym(Sym.IDENTIFIER, yytext()); }
  {QuotedIdentifier}  { return sym(Sym.IDENTIFIER, yytext().substring(1, yytext().length()-1)); }
  {QuotedInvalid}     { throw new RuntimeException("Invalid identifier: " + yytext()); }
 
  {DecInteger}     { return sym(Sym.NUMBER, new Long(yytext())); }

  \"               { buf.setLength(0); yybegin(STRING); }
  \'               { buf.setLength(0); yybegin(STRING1); }

  {WhiteSpace}     { /* ignore */ }
}

<STRING> {
  \"               { yybegin(YYINITIAL); 
                     return sym( Sym.STRING, buf.toString()); }
  [^\"\\]+         { buf.append( yytext() ); }
  \\t              { buf.append('\t'); }
  \\n              { buf.append('\n'); }
  \\r              { buf.append('\r'); }
  \\\"             { buf.append('\"'); }
  \\\\             { buf.append('\\'); }
}

<STRING1> {
  \'               { yybegin(YYINITIAL); 
                     return sym( Sym.STRING, buf.toString()); }
  [^\'\\]+         { buf.append( yytext() ); }
  \\t              { buf.append('\t'); }
  \\n              { buf.append('\n'); }
  \\r              { buf.append('\r'); }
  \\\'             { buf.append('\''); }
  \\\\             { buf.append('\\'); }
}

/* error fallback */
.|\n               { throw new RuntimeException("Illegal character <"+yytext()+">"); }
