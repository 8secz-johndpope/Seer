 /*
  * Copyright (C) 2001 Stefan Matthias Aust <sma@3plus4.de>
  * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
  * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
  * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
  * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
  *
  * JRuby - http://jruby.sourceforge.net
  *
  * This file is part of JRuby
  *
  * JRuby is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License or
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2 of the
  * License, or (at your option) any later version.
  *
  * JRuby is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License and GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public
  * License and GNU Lesser General Public License along with JRuby;
  * if not, write to
  * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  * Boston, MA  02111-1307 USA
  */
 package org.jruby.lexer.yacc;
 
 import java.math.BigInteger;
 
 import org.jruby.ast.BackRefNode;
 import org.jruby.ast.NthRefNode;
 import org.jruby.common.RubyWarnings;
 import org.jruby.parser.ParserSupport;
 import org.jruby.parser.Token;
 import org.jruby.util.IdUtil;
 import org.jruby.util.PrintfFormat;
 
 /** This is a port of the MRI lexer to Java it is compatible to Ruby 1.8.1.
  *
  * @author  jpetersen,enebo
  * @version $Revision$
  */
 public class RubyYaccLexer {
     // Last token read via yylex().
     private int token = 0;
     
     // Value of last token which had a value associated with it.
     Object yaccValue;
 
     // Stream of data that yylex() examines.
     private LexerSource src;
     
     // Used for tiny smidgen of grammar in lexer (see setParserSupport())
     private ParserSupport parserSupport = null;
 
     // The current location of the lexer immediately after a call to yylex()
     private SourcePosition currentPos;
 
     // What handles warnings
     private RubyWarnings warnings;
 
     // Additional context surrounding tokens that both the lexer and
     // grammar use.
     private LexState lex_state;
     
     // Tempory buffer to build up a potential token.
     private StringBuffer tokenBuffer = new StringBuffer(60);
 
     private StackState conditionState = new StackState();
     private StackState cmdArgumentState = new StackState();
     private StrTerm lex_strterm = null;
     private boolean commandStart = true;
 
     // Give a name to a value.  Enebo: This should be used more.
     static final int EOF = 0;
 
     // ruby constants for strings (should this be moved somewhere else?)
     static final int STR_FUNC_ESCAPE=0x01;
     static final int STR_FUNC_EXPAND=0x02;
     static final int STR_FUNC_REGEXP=0x04;
     static final int STR_FUNC_QWORDS=0x08;
     static final int STR_FUNC_SYMBOL=0x10;
     static final int STR_FUNC_INDENT=0x20;
 
     private final int str_squote = 0;
     private final int str_dquote = STR_FUNC_EXPAND;
     private final int str_xquote = STR_FUNC_EXPAND;
     private final int str_regexp = 
         STR_FUNC_REGEXP|STR_FUNC_ESCAPE|STR_FUNC_EXPAND;
     //private final int str_sword  = STR_FUNC_QWORDS;
     //private final int str_dword  = STR_FUNC_QWORDS|STR_FUNC_EXPAND;
     private final int str_ssym   = STR_FUNC_SYMBOL;
     private final int str_dsym   = STR_FUNC_SYMBOL|STR_FUNC_EXPAND;
     
     /**
      * How the parser advances to the next token.
      * 
      * @return true if not at end of file (EOF).
      */
     public boolean advance() {
         return (token = yylex()) != EOF;
     }
     
     /**
      * Last token read from the lexer at the end of a call to yylex()
      * 
      * @return last token read
      */
     public int token() {
         return token;
     }
 
     /**
      * Value of last token (if it is a token which has a value).
      * 
      * @return value of last value-laden token
      */
     public Object value() {
         return yaccValue;
     }
 
     /**
      * Position from where last token came from.
      * 
      * @return the last tokens position from the source
      */
     public SourcePosition getPosition() {
         return currentPos;
     }
 
     /**
      * Parse must pass its support object for some check at bottom of
      * yylex().  Ruby does it this way as well (i.e. a little parsing
      * logic in the lexer).
      * 
      * @param parserSupport
      */
     public void setParserSupport(ParserSupport parserSupport) {
         this.parserSupport = parserSupport;
     }
 
     /**
      * Allow the parser to set the source for its lexer.
      * 
      * @param source where the lexer gets raw data
      */
     public void setSource(LexerSource source) {
         this.src = source;
     }
 
     public StrTerm getStrTerm() {
         return lex_strterm;
     }
     
     public void setStrTerm(StrTerm strterm) {
         this.lex_strterm = strterm;
     }
 
     public void resetStacks() {
         conditionState.reset();
         cmdArgumentState.reset();
     }
     
     public void setWarnings(RubyWarnings warnings) {
         this.warnings = warnings;
     }
 
 
     public void setState(LexState state) {
         this.lex_state = state;
     }
 
     public StackState getCmdArgumentState() {
         return cmdArgumentState;
     }
 
     public StackState getConditionState() {
         return conditionState;
     }
 
     private boolean isNext_identchar() {
         char c = src.read();
         src.unread(c);
 
         return c != EOF && (Character.isLetterOrDigit(c) || c == '-');
     }
     
     private static final Object getInteger(String value, int radix) {
         try {
             return Long.valueOf(value, radix);
         } catch (NumberFormatException e) {
             return new BigInteger(value, radix);
         }
     }
 
     private boolean ISUPPER(char c) {
         return Character.isUpperCase(c);
     }
 
     /**
 	 * Do the next characters from the source match provided String in a case
 	 * insensitive manner.  If so, then consume those characters and return 
 	 * true.  Otherwise, consume none of them.
 	 * 
 	 * @param s to be matched against
      * @return true if string matches
      */ 
     private boolean isNextNoCase(String s) {
     	StringBuffer buf = new StringBuffer();
     	
         for (int i = 0; i < s.length(); i++) {
             char c = s.charAt(i);
             char r = src.read();
             buf.append(r);
             
             if (Character.toLowerCase(c) != r &&
                 Character.toUpperCase(c) != r) {
             	src.unreadMany(buf);
                 return false;
             }
         }
 
         return true;
     }
 
 	/**
 	 * @param c the character to test
 	 * @return true if character is a hex value (0-9a-f)
 	 */
     static final boolean isHexChar(char c) {
         return Character.isDigit(c) || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F');
     }
 
     /**
 	 * @param c the character to test
      * @return true if character is an octal value (0-7)
 	 */
     static final boolean isOctChar(char c) {
         return '0' <= c && c <= '7';
     }
     
     /**
      * @param c is character to be compared
      * @return whether c is an identifier or not
      */
     private static final boolean isIdentifierChar(char c) {
         return Character.isLetterOrDigit(c) || c == '_';
     }
     
     private int parseQuote(char c) {
         char term;
         char paren;
         
         if (!Character.isLetterOrDigit(c)) {
             term = c;
             c = 'Q';
         } else {
             term = src.read();
             if (Character.isLetterOrDigit(term) /* no mb || ismbchar(term)*/) {
                 throw new SyntaxException(src.getPosition(), "unknown type of %string");
             }
         }
         if (c == EOF || term == EOF) {
             throw new SyntaxException(src.getPosition(), "unterminated quoted string meets end of file");
         }
         paren = term;
         if (term == '(') term = ')';
         else if (term == '[') term = ']';
         else if (term == '{') term = '}';
         else if (term == '<') term = '>';
         else paren = '\0';
 
         switch (c) {
         case 'Q':
             lex_strterm = new StringTerm(src.getPosition(), str_dquote, term, paren);
             return Token.tSTRING_BEG;
 
         case 'q':
             lex_strterm = new StringTerm(src.getPosition(), str_squote, term, paren);
             return Token.tSTRING_BEG;
 
         case 'W':
             lex_strterm = new StringTerm(src.getPosition(), str_dquote | STR_FUNC_QWORDS, term, paren);
             do {c = src.read();} while (Character.isWhitespace(c));
             src.unread(c);
             return Token.tWORDS_BEG;
 
         case 'w':
             lex_strterm = new StringTerm(src.getPosition(), str_squote | STR_FUNC_QWORDS, term, paren);
             do {c = src.read();} while (Character.isWhitespace(c));
             src.unread(c);
             return Token.tQWORDS_BEG;
 
         case 'x':
             lex_strterm = new StringTerm(src.getPosition(), str_xquote, term, paren);
             return Token.tXSTRING_BEG;
 
         case 'r':
             lex_strterm = new StringTerm(src.getPosition(), str_regexp, term, paren);
             return Token.tREGEXP_BEG;
 
         case 's':
             lex_strterm = new StringTerm(src.getPosition(), str_ssym, term, paren);
             lex_state = LexState.EXPR_FNAME;
             return Token.tSYMBEG;
 
         default:
             throw new SyntaxException(src.getPosition(), "Unknown type of %string. Expected 'Q', 'q', 'w', 'x', 'r' or any non letter character, but found '" + c + "'.");
         }
     }
     
     private int hereDocumentIdentifier() {
         char c = src.read(); 
         int term;
         int func = 0;
 
         if (c == '-') {
             c = src.read();
             func = STR_FUNC_INDENT;
         }
         
         if (c == '\'' || c == '"' || c == '`') {
             if (c == '\'') {
                 func |= str_squote;
             } else if (c == '"') {
                 func |= str_dquote;
             } else {
                 func |= str_xquote; 
             }
 
             tokenBuffer.setLength(0);
             term = c;
             while ((c = src.read()) != EOF && c != term) {
                 tokenBuffer.append(c);
             }
             if (c == EOF) {
                 throw new SyntaxException(src.getPosition(), "unterminated here document identifier");
             }	
         } else {
             if (!isIdentifierChar(c)) {
                 src.unread(c);
                 if ((func & STR_FUNC_INDENT) != 0) {
                     src.unread(c);
                 }
                 return 0;
             }
             tokenBuffer.setLength(0);
             term = '"';
             func |= str_dquote;
             do {
                 tokenBuffer.append(c);
             } while ((c = src.read()) != EOF && isIdentifierChar(c));
             src.unread(c);
         }
 
         String line = src.readLine() + '\n';
         String tok = tokenBuffer.toString();
         lex_strterm = new HeredocTerm(src.getPosition(), tok, func, line);
 
         return term == '`' ? Token.tXSTRING_BEG : Token.tSTRING_BEG;
     }
     
     private void arg_ambiguous() {
         warnings.warning(src.getPosition(), "Ambiguous first argument; make sure.");
     }
 
 
     /*
      * Not normally used, but is left in here since it can be useful in debugging
      * grammar and lexing problems.
 
     private void printToken(int token) {
         //System.out.print("LOC: " + support.getPosition() + " ~ ");
         
         switch (token) {
         	case Token.yyErrorCode: System.err.print("yyErrorCode,"); break;
         	case Token.kCLASS: System.err.print("kClass,"); break;
         	case Token.kMODULE: System.err.print("kModule,"); break;
         	case Token.kDEF: System.err.print("kDEF,"); break;
         	case Token.kUNDEF: System.err.print("kUNDEF,"); break;
         	case Token.kBEGIN: System.err.print("kBEGIN,"); break;
         	case Token.kRESCUE: System.err.print("kRESCUE,"); break;
         	case Token.kENSURE: System.err.print("kENSURE,"); break;
         	case Token.kEND: System.err.print("kEND,"); break;
         	case Token.kIF: System.err.print("kIF,"); break;
         	case Token.kUNLESS: System.err.print("kUNLESS,"); break;
         	case Token.kTHEN: System.err.print("kTHEN,"); break;
         	case Token.kELSIF: System.err.print("kELSIF,"); break;
         	case Token.kELSE: System.err.print("kELSE,"); break;
         	case Token.kCASE: System.err.print("kCASE,"); break;
         	case Token.kWHEN: System.err.print("kWHEN,"); break;
         	case Token.kWHILE: System.err.print("kWHILE,"); break;
         	case Token.kUNTIL: System.err.print("kUNTIL,"); break;
         	case Token.kFOR: System.err.print("kFOR,"); break;
         	case Token.kBREAK: System.err.print("kBREAK,"); break;
         	case Token.kNEXT: System.err.print("kNEXT,"); break;
         	case Token.kREDO: System.err.print("kREDO,"); break;
         	case Token.kRETRY: System.err.print("kRETRY,"); break;
         	case Token.kIN: System.err.print("kIN,"); break;
         	case Token.kDO: System.err.print("kDO,"); break;
         	case Token.kDO_COND: System.err.print("kDO_COND,"); break;
         	case Token.kDO_BLOCK: System.err.print("kDO_BLOCK,"); break;
         	case Token.kRETURN: System.err.print("kRETURN,"); break;
         	case Token.kYIELD: System.err.print("kYIELD,"); break;
         	case Token.kSUPER: System.err.print("kSUPER,"); break;
         	case Token.kSELF: System.err.print("kSELF,"); break;
         	case Token.kNIL: System.err.print("kNIL,"); break;
         	case Token.kTRUE: System.err.print("kTRUE,"); break;
         	case Token.kFALSE: System.err.print("kFALSE,"); break;
         	case Token.kAND: System.err.print("kAND,"); break;
         	case Token.kOR: System.err.print("kOR,"); break;
         	case Token.kNOT: System.err.print("kNOT,"); break;
         	case Token.kIF_MOD: System.err.print("kIF_MOD,"); break;
         	case Token.kUNLESS_MOD: System.err.print("kUNLESS_MOD,"); break;
         	case Token.kWHILE_MOD: System.err.print("kWHILE_MOD,"); break;
         	case Token.kUNTIL_MOD: System.err.print("kUNTIL_MOD,"); break;
         	case Token.kRESCUE_MOD: System.err.print("kRESCUE_MOD,"); break;
         	case Token.kALIAS: System.err.print("kALIAS,"); break;
         	case Token.kDEFINED: System.err.print("kDEFINED,"); break;
         	case Token.klBEGIN: System.err.print("klBEGIN,"); break;
         	case Token.klEND: System.err.print("klEND,"); break;
         	case Token.k__LINE__: System.err.print("k__LINE__,"); break;
         	case Token.k__FILE__: System.err.print("k__FILE__,"); break;
         	case Token.tIDENTIFIER: System.err.print("tIDENTIFIER["+ value() + "],"); break;
         	case Token.tFID: System.err.print("tFID[" + value() + "],"); break;
         	case Token.tGVAR: System.err.print("tGVAR[" + value() + "],"); break;
         	case Token.tIVAR: System.err.print("tIVAR[" + value() +"],"); break;
         	case Token.tCONSTANT: System.err.print("tCONSTANT["+ value() +"],"); break;
         	case Token.tCVAR: System.err.print("tCVAR,"); break;
         	case Token.tINTEGER: System.err.print("tINTEGER,"); break;
         	case Token.tFLOAT: System.err.print("tFLOAT,"); break;
             case Token.tSTRING_CONTENT: System.err.print("tSTRING_CONTENT[" + yaccValue + "],"); break;
             case Token.tSTRING_BEG: System.err.print("tSTRING_BEG,"); break;
             case Token.tSTRING_END: System.err.print("tSTRING_END,"); break;
             case Token.tSTRING_DBEG: System.err.print("STRING_DBEG,"); break;
             case Token.tSTRING_DVAR: System.err.print("tSTRING_DVAR,"); break;
             case Token.tXSTRING_BEG: System.err.print("tXSTRING_BEG,"); break;
             case Token.tREGEXP_BEG: System.err.print("tREGEXP_BEG,"); break;
             case Token.tREGEXP_END: System.err.print("tREGEXP_END,"); break;
             case Token.tWORDS_BEG: System.err.print("tWORDS_BEG,"); break;
             case Token.tQWORDS_BEG: System.err.print("tQWORDS_BEG,"); break;
         	case Token.tBACK_REF: System.err.print("tBACK_REF,"); break;
         	case Token.tNTH_REF: System.err.print("tNTH_REF,"); break;
         	case Token.tUPLUS: System.err.print("tUPLUS"); break;
         	case Token.tUMINUS: System.err.print("tUMINUS,"); break;
         	case Token.tPOW: System.err.print("tPOW,"); break;
         	case Token.tCMP: System.err.print("tCMP,"); break;
         	case Token.tEQ: System.err.print("tEQ,"); break;
         	case Token.tEQQ: System.err.print("tEQQ,"); break;
         	case Token.tNEQ: System.err.print("tNEQ,"); break;
         	case Token.tGEQ: System.err.print("tGEQ,"); break;
         	case Token.tLEQ: System.err.print("tLEQ,"); break;
         	case Token.tANDOP: System.err.print("tANDOP,"); break;
         	case Token.tOROP: System.err.print("tOROP,"); break;
         	case Token.tMATCH: System.err.print("tMATCH,"); break;
         	case Token.tNMATCH: System.err.print("tNMATCH,"); break;
         	case Token.tDOT2: System.err.print("tDOT2,"); break;
         	case Token.tDOT3: System.err.print("tDOT3,"); break;
         	case Token.tAREF: System.err.print("tAREF,"); break;
         	case Token.tASET: System.err.print("tASET,"); break;
         	case Token.tLSHFT: System.err.print("tLSHFT,"); break;
         	case Token.tRSHFT: System.err.print("tRSHFT,"); break;
         	case Token.tCOLON2: System.err.print("tCOLON2,"); break;
         	case Token.tCOLON3: System.err.print("tCOLON3,"); break;
         	case Token.tOP_ASGN: System.err.print("tOP_ASGN,"); break;
         	case Token.tASSOC: System.err.print("tASSOC,"); break;
         	case Token.tLPAREN: System.err.print("tLPAREN,"); break;
         	case Token.tLPAREN_ARG: System.err.print("tLPAREN_ARG,"); break;
         	case Token.tLBRACK: System.err.print("tLBRACK,"); break;
         	case Token.tLBRACE: System.err.print("tLBRACE,"); break;
         	case Token.tSTAR: System.err.print("tSTAR,"); break;
         	case Token.tAMPER: System.err.print("tAMPER,"); break;
         	case Token.tSYMBEG: System.err.print("tSYMBEG,"); break;
         	case '\n': System.err.println("NL"); break;
         	default: System.err.print("'" + (char)token + "',"); break;
         }
     }
 
     // DEBUGGING HELP 
     private int yylex() {
         int token = yylex2();
         
         printToken(token);
         
         return token;
     }
     */
     
     /**
      *  Returns the next token. Also sets yyVal is needed.
      *
      *@return    Description of the Returned Value
      */
     private int yylex() {
         char c;
         boolean spaceSeen = false;
         boolean commandState;
         
         if (lex_strterm != null) {
 			int tok = lex_strterm.parseString(this, src);
 			if (tok == Token.tSTRING_END || tok == Token.tREGEXP_END) {
 			    lex_strterm = null;
 			    lex_state = LexState.EXPR_END;
 			}
 			return tok;
         }
 
         currentPos = src.getPosition();
         
         commandState = commandStart;
         commandStart = false;
         
         retry: for(;;) {
             c = src.read();
             switch(c) {
             case '\004':		/* ^D */
             case '\032':		/* ^Z */
             case 0:			/* end of script. */
                 return 0;
            
                 /* white spaces */
             case ' ': case '\t': case '\f': case '\r':
             case '\13': /* '\v' */
                 spaceSeen = true;
                 continue retry;
             case '#':		/* it's a comment */
                 while ((c = src.read()) != '\n') {
                     if (c == EOF) {
                         return 0;
                     }
                 }
                 
                 /* fall through */
             case '\n':
                 if (lex_state == LexState.EXPR_BEG ||
                     lex_state == LexState.EXPR_FNAME ||
                     lex_state == LexState.EXPR_DOT ||
                     lex_state == LexState.EXPR_CLASS) {
                     continue retry;
                 } 
 
                 commandStart = true;
                 lex_state = LexState.EXPR_BEG;
                 return '\n';
                 
             case '*':
                 if ((c = src.read()) == '*') {
                     if ((c = src.read()) == '=') {
                         yaccValue = "**";
                         lex_state = LexState.EXPR_BEG;
                         return Token.tOP_ASGN;
                     }
                     src.unread(c);
                     c = Token.tPOW;
                 } else {
                     if (c == '=') {
                         yaccValue = "*";
                         lex_state = LexState.EXPR_BEG;
                         return Token.tOP_ASGN;
                     }
                     src.unread(c);
                     if (lex_state.isArgument() && spaceSeen && !Character.isWhitespace(c)) {
                         warnings.warning(src.getPosition(), "`*' interpreted as argument prefix");
                         c = Token.tSTAR;
                     } else if (lex_state == LexState.EXPR_BEG || 
                             lex_state == LexState.EXPR_MID) {
                         c = Token.tSTAR;
                     } else {
                         c = '*';
                     }
                 }
                 if (lex_state == LexState.EXPR_FNAME ||
                     lex_state == LexState.EXPR_DOT) {
                     lex_state = LexState.EXPR_ARG;
                 } else {
                     lex_state = LexState.EXPR_BEG;
                 }
                 return c;
 
             case '!':
                 lex_state = LexState.EXPR_BEG;
                 if ((c = src.read()) == '=') {
                     return Token.tNEQ;
                 }
                 if (c == '~') {
                     return Token.tNMATCH;
                 }
                 src.unread(c);
                 return '!';
 
             case '=':
                 // Skip documentation nodes
                 if (src.wasBeginOfLine()) {
                     /* skip embedded rd document */
                     if (isNextNoCase("begin")) {
                         c = src.read();
                         
                         if (Character.isWhitespace(c)) {
                             // In case last next was the newline.
                             src.unread(c);
                             for (;;) {
                                 c = src.read();
 
                                 // If a line is followed by a blank line put
                                 // it back.
                                 while (c == '\n') {
                                     c = src.read();
                                 }
                                 if (c == EOF) {
                                     throw new SyntaxException(src.getPosition(), "embedded document meets end of file");
                                 }
                                 if (c != '=') continue;
                                if (src.wasBeginOfLine() && isNextNoCase("end")) {
                                     if (src.peek('\n')) {
                                         break;
                                     } 
                                     
                                     c = src.read();
                                     
                                     if (Character.isWhitespace(c)) {
                                         src.readLine();
                                         break;
                                     }
 									src.unread(c);
                                 }
                             }
                             continue retry;
                         }
 						src.unread(c);
                     }
                 }
 
                 if (lex_state == LexState.EXPR_FNAME || 
                     lex_state == LexState.EXPR_DOT) {
                     lex_state = LexState.EXPR_ARG;
                 } else { 
                     lex_state = LexState.EXPR_BEG;
                 }
 
                 c = src.read();
                 if (c == '=') {
                     c = src.read();
                     if (c == '=') {
                         return Token.tEQQ;
                     }
                     src.unread(c);
                     return Token.tEQ;
                 }
                 if (c == '~') {
                     return Token.tMATCH;
                 } else if (c == '>') {
                     return Token.tASSOC;
                 }
                 src.unread(c);
                 return '=';
                 
             case '<':
                 c = src.read();
                 if (c == '<' &&
                         lex_state != LexState.EXPR_END &&
                         lex_state != LexState.EXPR_DOT &&
                         lex_state != LexState.EXPR_ENDARG && 
                         lex_state != LexState.EXPR_CLASS &&
                         (!lex_state.isArgument() || spaceSeen)) {
                     int tok = hereDocumentIdentifier();
                     if (tok != 0) return tok;
                 }
                 if (lex_state == LexState.EXPR_FNAME ||
                     lex_state == LexState.EXPR_DOT) {
                     lex_state = LexState.EXPR_ARG;
                 } else {
                     lex_state = LexState.EXPR_BEG;
                 }
                 if (c == '=') {
                     if ((c = src.read()) == '>') {
                         return Token.tCMP;
                     }
                     src.unread(c);
                     return Token.tLEQ;
                 }
                 if (c == '<') {
                     if ((c = src.read()) == '=') {
                         yaccValue = "<<";
                         lex_state = LexState.EXPR_BEG;
                         return Token.tOP_ASGN;
                     }
                     src.unread(c);
                     return Token.tLSHFT;
                 }
                 src.unread(c);
                 return '<';
                 
             case '>':
                 if (lex_state == LexState.EXPR_FNAME ||
                     lex_state == LexState.EXPR_DOT) {
                     lex_state = LexState.EXPR_ARG;
                 } else {
                     lex_state = LexState.EXPR_BEG;
                 }
 
                 if ((c = src.read()) == '=') {
                     return Token.tGEQ;
                 }
                 if (c == '>') {
                     if ((c = src.read()) == '=') {
                         yaccValue = ">>";
                         lex_state = LexState.EXPR_BEG;
                         return Token.tOP_ASGN;
                     }
                     src.unread(c);
                     return Token.tRSHFT;
                 }
                 src.unread(c);
                 return '>';
 
             case '"':
                 lex_strterm = new StringTerm(src.getPosition(), str_dquote, '"', '\0');
                 return Token.tSTRING_BEG;
 
             case '`':
                 if (lex_state == LexState.EXPR_FNAME) {
                     lex_state = LexState.EXPR_END;
                     return c;
                 }
                 if (lex_state == LexState.EXPR_DOT) {
                     if (commandState) {
                         lex_state = LexState.EXPR_CMDARG;
                     } else {
                         lex_state = LexState.EXPR_ARG;
                     }
                     return c;
                 }
                 lex_strterm = new StringTerm(src.getPosition(), str_xquote, '`', '\0');
                 return Token.tXSTRING_BEG;
 
             case '\'':
                 lex_strterm = new StringTerm(src.getPosition(), str_squote, '\'', '\0');
                 return Token.tSTRING_BEG;
 
             case '?':
                 if (lex_state == LexState.EXPR_END || 
                     lex_state == LexState.EXPR_ENDARG) {
                     lex_state = LexState.EXPR_BEG;
                     return '?';
                 }
                 c = src.read();
                 if (c == EOF) {
                     throw new SyntaxException(src.getPosition(), "incomplete character syntax");
                 }
                 if (Character.isWhitespace(c)){
                     if (!lex_state.isArgument()){
                         int c2 = 0;
                         switch (c) {
                         case ' ':
                             c2 = 's';
                             break;
                         case '\n':
                             c2 = 'n';
                             break;
                         case '\t':
                             c2 = 't';
                             break;
                             /* What is \v in C?
                         case '\v':
                             c2 = 'v';
                             break;
                             */
                         case '\r':
                             c2 = 'r';
                             break;
                         case '\f':
                             c2 = 'f';
                             break;
                         }
                         if (c2 != 0) {
                             warnings.warn(src.getPosition(), "invalid character syntax; use ?\\" + c2);
                         }
                     }
                     src.unread(c);
                     lex_state = LexState.EXPR_BEG;
                     return '?';
                 /*} else if (ismbchar(c)) { // ruby - we don't support them either?
                     rb_warn("multibyte character literal not supported yet; use ?\\" + c);
                     support.unread(c);
                     lexState = LexState.EXPR_BEG;
                     return '?';*/
                 } else if ((Character.isLetterOrDigit(c) || c == '_') &&
                         !src.peek('\n') && isNext_identchar()) {
                     src.unread(c);
                     lex_state = LexState.EXPR_BEG;
                     return '?';
                 } else if (c == '\\') {
                     c = src.readEscape();
                 }
                 c &= 0xff;
                 lex_state = LexState.EXPR_END;
                 yaccValue = new Long(c);
                 
                 return Token.tINTEGER;
 
             case '&':
                 if ((c = src.read()) == '&') {
                     lex_state = LexState.EXPR_BEG;
                     if ((c = src.read()) == '=') {
                         yaccValue = "&&";
                         lex_state = LexState.EXPR_BEG;
                         return Token.tOP_ASGN;
                     }
                     src.unread(c);
                     return Token.tANDOP;
                 }
                 else if (c == '=') {
                     yaccValue = "&";
                     lex_state = LexState.EXPR_BEG;
                     return Token.tOP_ASGN;
                 }
                 src.unread(c);
                 if (lex_state.isArgument() && spaceSeen && !Character.isWhitespace(c)){
                     warnings.warning(src.getPosition(), "`&' interpreted as argument prefix");
                     c = Token.tAMPER;
                 } else if (lex_state == LexState.EXPR_BEG || 
                         lex_state == LexState.EXPR_MID) {
                     c = Token.tAMPER;
                 } else {
                     c = '&';
                 }
                 
                 if (lex_state == LexState.EXPR_FNAME ||
                     lex_state == LexState.EXPR_DOT) {
                     lex_state = LexState.EXPR_ARG;
                 } else {
                     lex_state = LexState.EXPR_BEG;
                 }
                 return c;
                 
             case '|':
                 if ((c = src.read()) == '|') {
                     lex_state = LexState.EXPR_BEG;
                     if ((c = src.read()) == '=') {
                         yaccValue = "||";
                         lex_state = LexState.EXPR_BEG;
                         return Token.tOP_ASGN;
                     }
                     src.unread(c);
                     return Token.tOROP;
                 }
                 if (c == '=') {
                     yaccValue = "|";
                     lex_state = LexState.EXPR_BEG;
                     return Token.tOP_ASGN;
                 }
                 if (lex_state == LexState.EXPR_FNAME || 
                     lex_state == LexState.EXPR_DOT) {
                     lex_state = LexState.EXPR_ARG;
                 } else {
                     lex_state = LexState.EXPR_BEG;
                 }
                 src.unread(c);
                 return '|';
 
             case '+':
                 c = src.read();
                 if (lex_state == LexState.EXPR_FNAME || 
                     lex_state == LexState.EXPR_DOT) {
                     lex_state = LexState.EXPR_ARG;
                     if (c == '@') {
                         return Token.tUPLUS;
                     }
                     src.unread(c);
                     return '+';
                 }
                 if (c == '=') {
                     yaccValue = "+";
                     lex_state = LexState.EXPR_BEG;
                     return Token.tOP_ASGN;
                 }
                 if (lex_state == LexState.EXPR_BEG ||
                     lex_state == LexState.EXPR_MID ||
                         (lex_state.isArgument() && spaceSeen && !Character.isWhitespace(c))) {
                     if (lex_state.isArgument()) arg_ambiguous();
                     lex_state = LexState.EXPR_BEG;
                     src.unread(c);
                     if (Character.isDigit(c)) {
                         c = '+';
                         return parseNumber(c);
                     }
                     return Token.tUPLUS;
                 }
                 lex_state = LexState.EXPR_BEG;
                 src.unread(c);
                 return '+';
 
             case '-':
                 c = src.read();
                 if (lex_state == LexState.EXPR_FNAME || 
                     lex_state == LexState.EXPR_DOT) {
                     lex_state = LexState.EXPR_ARG;
                     if (c == '@') {
                         return Token.tUMINUS;
                     }
                     src.unread(c);
                     return '-';
                 }
                 if (c == '=') {
                     yaccValue = "-";
                     lex_state = LexState.EXPR_BEG;
                     return Token.tOP_ASGN;
                 }
                 if (lex_state == LexState.EXPR_BEG || 
                     lex_state == LexState.EXPR_MID ||
                         (lex_state.isArgument() && spaceSeen && !Character.isWhitespace(c))) {
                     if (lex_state.isArgument()) arg_ambiguous();
                     lex_state = LexState.EXPR_BEG;
                     src.unread(c);
                     if (Character.isDigit(c)) {
                         return Token.tUMINUS_NUM;
                     }
                     return Token.tUMINUS;
                 }
                 lex_state = LexState.EXPR_BEG;
                 src.unread(c);
                 return '-';
                 
             case '.':
                 lex_state = LexState.EXPR_BEG;
                 if ((c = src.read()) == '.') {
                     if ((c = src.read()) == '.') {
                         return Token.tDOT3;
                     }
                     src.unread(c);
                     return Token.tDOT2;
                 }
                 src.unread(c);
                 if (Character.isDigit(c)) {
                     throw new SyntaxException(src.getPosition(), "no .<digit> floating literal anymore; put 0 before dot"); 
                 }
                 lex_state = LexState.EXPR_DOT;
                 return '.';
             case '0' : case '1' : case '2' : case '3' : case '4' :
             case '5' : case '6' : case '7' : case '8' : case '9' :
                 return parseNumber(c);
                 
             case ']':
             case '}':
             case ')':
                 conditionState.restart();
                 cmdArgumentState.restart();
                 lex_state = LexState.EXPR_END;
                 return c;
 
             case ':':
                 c = src.read();
                 if (c == ':') {
                     if (lex_state == LexState.EXPR_BEG ||
                         lex_state == LexState.EXPR_MID ||
                         lex_state == LexState.EXPR_CLASS || 
                         (lex_state.isArgument() && spaceSeen)) {
                         lex_state = LexState.EXPR_BEG;
                         return Token.tCOLON3;
                     }
                     lex_state = LexState.EXPR_DOT;
                     return Token.tCOLON2;
                 }
                 if (lex_state == LexState.EXPR_END || 
                     lex_state == LexState.EXPR_ENDARG || Character.isWhitespace(c)) {
                     src.unread(c);
                     lex_state = LexState.EXPR_BEG;
                     return ':';
                 }
                 switch (c) {
                 case '\'':
                     lex_strterm = new StringTerm(src.getPosition(), str_ssym, c, '\0');
                     break;
                 case '"':
                     lex_strterm = new StringTerm(src.getPosition(), str_dsym, c, '\0');
                     break;
                 default:
                     src.unread(c);
                     break;
                 }
                 lex_state = LexState.EXPR_FNAME;
                 return Token.tSYMBEG;
 
             case '/':
                 if (lex_state == LexState.EXPR_BEG || 
                     lex_state == LexState.EXPR_MID) {
                     lex_strterm = new StringTerm(src.getPosition(), str_regexp, '/', '\0');
                     return Token.tREGEXP_BEG;
                 }
                 
                 if ((c = src.read()) == '=') {
                     yaccValue = "/";
                     lex_state = LexState.EXPR_BEG;
                     return Token.tOP_ASGN;
                 }
                 src.unread(c);
                 if (lex_state.isArgument() && spaceSeen) {
                     if (!Character.isWhitespace(c)) {
                         arg_ambiguous();
                         lex_strterm = new StringTerm(src.getPosition(), str_regexp, '/', '\0');
                         return Token.tREGEXP_BEG;
                     }
                 }
                 if (lex_state == LexState.EXPR_FNAME || 
                     lex_state == LexState.EXPR_DOT) {
                     lex_state = LexState.EXPR_ARG;
                 } else {
                     lex_state = LexState.EXPR_BEG;
                 }
                 return '/';
 
             case '^':
                 if ((c = src.read()) == '=') {
                     yaccValue = "^";
                     lex_state = LexState.EXPR_BEG;
                     return Token.tOP_ASGN;
                 }
                 if (lex_state == LexState.EXPR_FNAME || 
                     lex_state == LexState.EXPR_DOT) {
                     lex_state = LexState.EXPR_ARG;
                 } else {
                     lex_state = LexState.EXPR_BEG;
                 }
                 src.unread(c);
                 return '^';
 
             case ';':
                 commandStart = true;
             case ',':
                 lex_state = LexState.EXPR_BEG;
                 return c;
 
             case '~':
                 if (lex_state == LexState.EXPR_FNAME || 
                     lex_state == LexState.EXPR_DOT) {
                     if ((c = src.read()) != '@') {
                         src.unread(c);
                     }
                 }
                 if (lex_state == LexState.EXPR_FNAME || 
                         lex_state == LexState.EXPR_DOT) {
                     lex_state = LexState.EXPR_ARG;
                 } else {
                     lex_state = LexState.EXPR_BEG;
                 }
                 return '~';
             case '(':
                 commandStart = true;
                 if (lex_state == LexState.EXPR_BEG || 
                     lex_state == LexState.EXPR_MID) {
                     c = Token.tLPAREN;
                 } else if (spaceSeen) {
                     if (lex_state == LexState.EXPR_CMDARG) {
                         c = Token.tLPAREN_ARG;
                     } else if (lex_state == LexState.EXPR_ARG) {
                         warnings.warn(src.getPosition(), "don't put space before argument parentheses");
                         c = '(';
                     }
                 }
                 conditionState.stop();
                 cmdArgumentState.stop();
                 lex_state = LexState.EXPR_BEG;
                 return c;
 
             case '[':
                 if (lex_state == LexState.EXPR_FNAME || 
                     lex_state == LexState.EXPR_DOT) {
                     lex_state = LexState.EXPR_ARG;
                     if ((c = src.read()) == ']') {
                         if ((c = src.read()) == '=') {
                             return Token.tASET;
                         }
                         src.unread(c);
                         return Token.tAREF;
                     }
                     src.unread(c);
                     return '[';
                 } else if (lex_state == LexState.EXPR_BEG || 
                            lex_state == LexState.EXPR_MID) {
                     c = Token.tLBRACK;
                 } else if (lex_state.isArgument() && spaceSeen) {
                     c = Token.tLBRACK;
                 }
                 lex_state = LexState.EXPR_BEG;
                 conditionState.stop();
                 cmdArgumentState.stop();
                 return c;
                 
             case '{':
                 if (lex_state.isArgument() || lex_state == LexState.EXPR_END) {
                     c = '{';          /* block (primary) */
                 } else if (lex_state == LexState.EXPR_ENDARG) {
                     c = Token.tLBRACE_ARG;  /* block (expr) */
                 } else {
                     c = Token.tLBRACE;      /* hash */
                 }
                 conditionState.stop();
                 cmdArgumentState.stop();
                 lex_state = LexState.EXPR_BEG;
                 return c;
 
             case '\\':
                 c = src.read();
                 if (c == '\n') {
                     spaceSeen = true;
                     continue retry; /* skip \\n */
                 }
                 src.unread(c);
                 return '\\';
 
             case '%':
                 if (lex_state == LexState.EXPR_BEG || 
                     lex_state == LexState.EXPR_MID) {
                     return parseQuote(src.read());
                 }
                 if ((c = src.read()) == '=') {
                     yaccValue = "%";
                     lex_state = LexState.EXPR_BEG;
                     return Token.tOP_ASGN;
                 }
                 if (lex_state.isArgument() && spaceSeen && !Character.isWhitespace(c)) {
                     return parseQuote(c);
                 }
                 if (lex_state == LexState.EXPR_FNAME || 
                     lex_state == LexState.EXPR_DOT) {
                     lex_state = LexState.EXPR_ARG;
                 } else {
                     lex_state = LexState.EXPR_BEG;
                 }
                 src.unread(c);
                 return '%';
 
             case '$':
                 lex_state = LexState.EXPR_END;
                 tokenBuffer.setLength(0);
                 c = src.read();
                 switch (c) {
                 case '_':		/* $_: last read line string */
                     c = src.read();
                     if (isIdentifierChar(c)) {
                         tokenBuffer.append('$');
                         tokenBuffer.append('_');
                         break;
                     }
                     src.unread(c);
                     c = '_';
                     /* fall through */
                 case '~':		/* $~: match-data */
                     // Enebo: We had following line replace line after that,
                     // but it is commented out...
                     //parserSupport.getLocalNames().getLocalIndex(String.valueOf(c));
                     //local_cnt(c);
                     /* fall through */
                 case '*':		/* $*: argv */
                 case '$':		/* $$: pid */
                 case '?':		/* $?: last status */
                 case '!':		/* $!: error string */
                 case '@':		/* $@: error position */
                 case '/':		/* $/: input record separator */
                 case '\\':		/* $\: output record separator */
                 case ';':		/* $;: field separator */
                 case ',':		/* $,: output field separator */
                 case '.':		/* $.: last read line number */
                 case '=':		/* $=: ignorecase */
                 case ':':		/* $:: load path */
                 case '<':		/* $<: reading filename */
                 case '>':		/* $>: default output handle */
                 case '\"':		/* $": already loaded files */
                     tokenBuffer.append('$');
                     tokenBuffer.append(c);
                     yaccValue = tokenBuffer.toString();
                     return Token.tGVAR;
 
                 case '-':
                     tokenBuffer.append('$');
                     tokenBuffer.append(c);
                     c = src.read();
                     tokenBuffer.append(c);
                     yaccValue = tokenBuffer.toString();
                     /* xxx shouldn't check if valid option variable */
                     return Token.tGVAR;
 
                 case '&':		/* $&: last match */
                 case '`':		/* $`: string before last match */
                 case '\'':		/* $': string after last match */
                 case '+':		/* $+: string matches last paren. */
                     yaccValue = new BackRefNode(src.getPosition(), c);
                     return Token.tBACK_REF;
 
                 case '1': case '2': case '3':
                 case '4': case '5': case '6':
                 case '7': case '8': case '9':
                     tokenBuffer.append('$');
                     do {
                         tokenBuffer.append(c);
                         c = src.read();
                     } while (Character.isDigit(c));
                     src.unread(c);
                     yaccValue = new NthRefNode(src.getPosition(), Integer.parseInt(tokenBuffer.substring(1)));
                     return Token.tNTH_REF;
 
                 default:
                     if (!isIdentifierChar(c)) {
                         src.unread(c);
                         return '$';
                     }
                 case '0':
                     tokenBuffer.append('$');
                 }
                 break;
 
             case '@':
                 c = src.read();
                 tokenBuffer.setLength(0);
                 tokenBuffer.append('@');
                 if (c == '@') {
                     tokenBuffer.append('@');
                     c = src.read();
                 }
                 if (Character.isDigit(c)) {
                     if (tokenBuffer.length() == 1) {
                         throw new SyntaxException(src.getPosition(), "`@" + c + "' is not allowed as an instance variable name");
                     }
                     throw new SyntaxException(src.getPosition(), "`@@" + c + "' is not allowed as a class variable name");
                 }
                 if (!isIdentifierChar(c)) {
                     src.unread(c);
                     return '@';
                 }
                 break;
 
             case '_':
                 if (src.wasBeginOfLine() && src.matchString("_END__", false)) {
                     return 0;
                 }
                 tokenBuffer.setLength(0);
                 break;
 
             default:
                 if (!isIdentifierChar(c)) {
                     throw new SyntaxException(src.getPosition(), "Invalid char `\\" + new PrintfFormat("%.3o").sprintf(c) + "' in expression");
                 }
 
                 tokenBuffer.setLength(0);
                 break;
             }
     
             do {
                 tokenBuffer.append(c);
                 /* no special multibyte character handling is needed in Java
                  * if (ismbchar(c)) {
                     int i, len = mbclen(c)-1;
 
                     for (i = 0; i < len; i++) {
                         c = src.read();
                         tokenBuffer.append(c);
                     }
                 }*/
                 c = src.read();
             } while (isIdentifierChar(c));
             
             char peek = src.read();
             if ((c == '!' || c == '?') && 
                 isIdentifierChar(tokenBuffer.charAt(0)) && peek != '=') {
                 src.unread(peek);
                 tokenBuffer.append(c);
             } else {
             	src.unread(peek);
             	src.unread(c);
             }
             
             int result = 0;
 
             switch (tokenBuffer.charAt(0)) {
                 case '$':
                     lex_state = LexState.EXPR_END;
                     result = Token.tGVAR;
                     break;
                 case '@':
                     lex_state = LexState.EXPR_END;
                     if (tokenBuffer.charAt(1) == '@') {
                         result = Token.tCVAR;
                     } else {
                         result = Token.tIVAR;
                     }
                     break;
 
                 default:
                 	char last = tokenBuffer.charAt(tokenBuffer.length() - 1);
                     if (last == '!' || last == '?') {
                         result = Token.tFID;
                     } else {
                         if (lex_state == LexState.EXPR_FNAME) {
                         	/*
                         	// Enebo: This should be equivalent to below without
                         	// so much read/unread action.
                             if ((c = src.read()) == '=') { 
                             	char c2 = src.read();
                         	
                             	if (c2 != '~' && c2 != '>' &&
                             		(c2 != '=' || (c2 == '\n' && src.peek('>')))) {
                             		result = Token.tIDENTIFIER;
                             		tokenBuffer.append(c);
                             	} else { 
                             		src.unread(c2);
                             		src.unread(c);
                             	}
                         	} else {
                             	src.unread(c);
                             }
                             */
                             
                             if ((c = src.read()) == '=' && 
                             	 !src.peek('~') && 
 								 !src.peek('>') &&
                                  (!src.peek('=') || 
                                  		(src.peek('\n') && 
                                  	     src.getCharAt(1) == '>'))) {
                                 result = Token.tIDENTIFIER;
                                 tokenBuffer.append(c);
                             } else {
                                 src.unread(c);
                             }
                         }
                         if (result == 0 && ISUPPER(tokenBuffer.charAt(0))) {
                             result = Token.tCONSTANT;
                         } else {
                             result = Token.tIDENTIFIER;
                         }
                     }
 
                     if (lex_state != LexState.EXPR_DOT) {
                         /* See if it is a reserved word.  */
                         Keyword keyword = Keyword.getKeyword(tokenBuffer.toString(), tokenBuffer.length());
                         if (keyword != null) {
                             // enum lex_state
                             LexState state = lex_state;
 
                             lex_state = keyword.state;
                             if (state.isExprFName()) {
                                 yaccValue = keyword.name;
                             }
                             if (keyword.id0 == Token.kDO) {
                                 if (conditionState.isInState()) {
                                     return Token.kDO_COND;
                                 }
                                 if (cmdArgumentState.isInState() && state != LexState.EXPR_CMDARG) {
                                     return Token.kDO_BLOCK;
                                 }
                                 if (state == LexState.EXPR_ENDARG) {
                                     return Token.kDO_BLOCK;
                                 }
                                 return Token.kDO;
                             }
 
                             if (state == LexState.EXPR_BEG) {
                                 return keyword.id0;
                             }
 							if (keyword.id0 != keyword.id1) {
 								lex_state = LexState.EXPR_BEG;
 							}
 							return keyword.id1;
                         }
                     }
 
                     if (lex_state == LexState.EXPR_BEG ||
                             lex_state == LexState.EXPR_MID ||
                             lex_state == LexState.EXPR_DOT ||
                             lex_state == LexState.EXPR_ARG ||
                             lex_state == LexState.EXPR_CMDARG) {
                         if (commandState) {
                             lex_state = LexState.EXPR_CMDARG;
                         } else {
                             lex_state = LexState.EXPR_ARG;
                         }
                     } else {
                         lex_state = LexState.EXPR_END;
                     }
             }	
             yaccValue = tokenBuffer.toString();
 
             // Lame: parsing logic made it into lexer in ruby...So we
             // are emulating
             if (IdUtil.isLocal((String)yaccValue) &&
                 ((parserSupport.getBlockNames().isInBlock() && 
                   parserSupport.getBlockNames().isDefined((String) yaccValue)) ||
                   parserSupport.getLocalNames().isLocalRegistered((String) yaccValue))) {
                 lex_state = LexState.EXPR_END;
             }
 
             return result;
         }
     }
 
     /**
      *  Parse a number from the input stream.
      *
      *@param c The first character of the number.
      *@return A int constant wich represents a token.
      */
     private int parseNumber(char c) {
         lex_state = LexState.EXPR_END;
 
         tokenBuffer.setLength(0);
 
         if (c == '-') {
         	tokenBuffer.append(c);
             c = src.read();
         } else if (c == '+') {
         	// We don't append '+' since Java number parser gets confused
             c = src.read();
         }
         
         char nondigit = '\0';
 
         if (c == '0') {
             int startLen = tokenBuffer.length();
 
             switch (c = src.read()) {
                 case 'x' :
                 case 'X' : //  hexadecimal
                     c = src.read();
                     if (isHexChar(c)) {
                         for (;; c = src.read()) {
                             if (c == '_') {
                                 if (nondigit != '\0') {
                                     break;
                                 }
 								nondigit = c;
                             } else if (isHexChar(c)) {
                                 nondigit = '\0';
                                 tokenBuffer.append(c);
                             } else {
                                 break;
                             }
                         }
                     }
                     src.unread(c);
 
                     if (tokenBuffer.length() == startLen) {
                         throw new SyntaxException(src.getPosition(), "Hexadecimal number without hex-digits.");
                     } else if (nondigit != '\0') {
                         throw new SyntaxException(src.getPosition(), "Trailing '_' in number.");
                     }
                     yaccValue = getInteger(tokenBuffer.toString(), 16);
                     return Token.tINTEGER;
                 case 'b' :
                 case 'B' : // binary
                     c = src.read();
                     if (c == '0' || c == '1') {
                         for (;; c = src.read()) {
                             if (c == '_') {
                                 if (nondigit != '\0') {
                                     break;
                                 }
 								nondigit = c;
                             } else if (c == '0' || c == '1') {
                                 nondigit = '\0';
                                 tokenBuffer.append(c);
                             } else {
                                 break;
                             }
                         }
                     }
                     src.unread(c);
 
                     if (tokenBuffer.length() == startLen) {
                         throw new SyntaxException(src.getPosition(), "Binary number without digits.");
                     } else if (nondigit != '\0') {
                         throw new SyntaxException(src.getPosition(), "Trailing '_' in number.");
                     }
                     yaccValue = getInteger(tokenBuffer.toString(), 2);
                     return Token.tINTEGER;
                 case 'd' :
                 case 'D' : // decimal
                     c = src.read();
                     if (Character.isDigit(c)) {
                         for (;; c = src.read()) {
                             if (c == '_') {
                                 if (nondigit != '\0') {
                                     break;
                                 }
 								nondigit = c;
                             } else if (Character.isDigit(c)) {
                                 nondigit = '\0';
                                 tokenBuffer.append(c);
                             } else {
                                 break;
                             }
                         }
                     }
                     src.unread(c);
 
                     if (tokenBuffer.length() == startLen) {
                         throw new SyntaxException(src.getPosition(), "Binary number without digits.");
                     } else if (nondigit != '\0') {
                         throw new SyntaxException(src.getPosition(), "Trailing '_' in number.");
                     }
                     yaccValue = getInteger(tokenBuffer.toString(), 2);
                     return Token.tINTEGER;
                 case '0' : case '1' : case '2' : case '3' : case '4' : //Octal
                 case '5' : case '6' : case '7' : case '_' : 
                     for (;; c = src.read()) {
                         if (c == '_') {
                             if (nondigit != '\0') {
                                 break;
                             }
 							nondigit = c;
                         } else if (c >= '0' && c <= '7') {
                             nondigit = '\0';
                             tokenBuffer.append(c);
                         } else {
                             break;
                         }
                     }
                     if (tokenBuffer.length() > startLen) {
                         src.unread(c);
 
                         if (nondigit != '\0') {
                             throw new SyntaxException(src.getPosition(), "Trailing '_' in number.");
                         }
 
                         yaccValue = getInteger(tokenBuffer.toString(), 8);
                         return Token.tINTEGER;
                     }
                 case '8' :
                 case '9' :
                     throw new SyntaxException(src.getPosition(), "Illegal octal digit.");
                 case '.' :
                 case 'e' :
                 case 'E' :
                 	tokenBuffer.append('0');
                     break;
                 default :
                     src.unread(c);
                     yaccValue = new Long(0);
                     return Token.tINTEGER;
             }
         }
 
         boolean seen_point = false;
         boolean seen_e = false;
 
         for (;; c = src.read()) {
             switch (c) {
                 case '0' :
                 case '1' :
                 case '2' :
                 case '3' :
                 case '4' :
                 case '5' :
                 case '6' :
                 case '7' :
                 case '8' :
                 case '9' :
                     nondigit = '\0';
                     tokenBuffer.append(c);
                     break;
                 case '.' :
                     if (nondigit != '\0') {
                         src.unread(c);
                         throw new SyntaxException(src.getPosition(), "Trailing '_' in number.");
                     } else if (seen_point || seen_e) {
                         src.unread(c);
                         return getNumberToken(tokenBuffer.toString(), true, nondigit);
                     } else {
                     	char c2;
                         if (!Character.isDigit(c2 = src.read())) {
                             src.unread(c2);
                         	src.unread('.');
                             if (c == '_') { 
                             		// Enebo:  c can never be antrhign but '.'
                             		// Why did I put this here?
                             } else {
                                 yaccValue = getInteger(tokenBuffer.toString(), 10);
                                 return Token.tINTEGER;
                             }
                         } else {
                             tokenBuffer.append('.');
                             tokenBuffer.append(c2);
                             seen_point = true;
                             nondigit = '\0';
                         }
                     }
                     break;
                 case 'e' :
                 case 'E' :
                     if (nondigit != '\0') {
                         throw new SyntaxException(src.getPosition(), "Trailing '_' in number.");
                     } else if (seen_e) {
                         src.unread(c);
                         return getNumberToken(tokenBuffer.toString(), true, nondigit);
                     } else {
                         tokenBuffer.append(c);
                         seen_e = true;
                         nondigit = c;
                         c = src.read();
                         if (c == '-' || c == '+') {
                             tokenBuffer.append(c);
                             nondigit = c;
                         } else {
                             src.unread(c);
                         }
                     }
                     break;
                 case '_' : //  '_' in number just ignored
                     if (nondigit != '\0') {
                         throw new SyntaxException(src.getPosition(), "Trailing '_' in number.");
                     }
                     nondigit = c;
                     break;
                 default :
                     src.unread(c);
                 return getNumberToken(tokenBuffer.toString(), seen_e || seen_point, nondigit);
             }
         }
     }
 
     private int getNumberToken(String number, boolean isFloat, char nondigit) {
         if (nondigit != '\0') {
             throw new SyntaxException(src.getPosition(), "Trailing '_' in number.");
         }
         if (isFloat) {
             Double d = new Double(0.0);
             try {
                 d = Double.valueOf(number);
             } catch (NumberFormatException e) {
                 warnings.warn(src.getPosition(), "Float " + number + " out of range.");
                 if (number.startsWith("-")) {
                     d = new Double(Double.NEGATIVE_INFINITY);
                 } else {
                     d = new Double(Double.POSITIVE_INFINITY);
                 }
             }
             yaccValue = d;
             return Token.tFLOAT;
         }
 		yaccValue = getInteger(number, 10);
 		return Token.tINTEGER;
     }
 }
