 /*
  * Copyright (C) 2010 SonarSource SA
  * All rights reserved
  * mailto:contact AT sonarsource DOT com
  */
 
 package com.sonar.sslr.api;
 
 import java.io.File;
 import java.util.List;
 
 import com.google.common.collect.Lists;
 
 /**
  * Tokens are string of character like an identifier, a literal, an integer, ... which are produced by the lexer to feed the parser.
  * By definition, comments and preprocessing directives should not be seen by the parser that's why such Trivia, when they exist, are
  * attached to the next token.
  */
 public class Token {
 
   private static final int DEFAULT_LINE = 1;
   private static final int DEFAULT_COLUMN = 0;
   private static final File DEFAULT_FILE = null;
   private static final List<Trivia> DEFAULT_EMPTY_TRIVIA = Lists.newArrayList();
 
   private final TokenType type;
   private final String value;
   private final String originalValue;
   private final int line;
   private final int column;
   private final File file;
   private boolean generatedCode = false;
   private List<Trivia> trivia = DEFAULT_EMPTY_TRIVIA;
 
   private boolean copyBook = false;
   private int copyBookOriginalLine = -1;
   private String copyBookOriginalFileName = null;
 
   public Token(TokenType type, String value) {
     this(type, value, DEFAULT_LINE, DEFAULT_COLUMN);
   }
 
   public Token(TokenType type, String value, String originalValue) {
     this(type, value, originalValue, DEFAULT_LINE, DEFAULT_COLUMN);
   }
 
   public Token(TokenType type, String value, int line, int column) {
     this(type, value, line, column, DEFAULT_FILE);
   }
 
   public Token(TokenType type, String value, String originalValue, int line, int column) {
     this(type, value, originalValue, line, column, DEFAULT_FILE);
   }
 
   public Token(TokenType type, String value, int line, int column, File file) {
     this(type, value, value, line, column, file);
   }
 
   public Token(TokenType type, String value, String originalValue, int line, int column, File file) {
     this.type = type;
     this.value = value;
     this.originalValue = originalValue;
     this.line = line;
     this.column = column;
     this.file = file;
   }
 
   public TokenType getType() {
     return type;
   }
 
   public String getValue() {
     return value;
   }
 
   /**
    * @return the original value of the token. This method is useful when a language is case-insensitive as in that case all token values are
    *         capitalized.
    */
   public String getOriginalValue() {
     return originalValue;
   }
 
   /**
    * @return the line of the token in the source code
    */
   public int getLine() {
     return line;
   }
 
   /**
    * @return the column of the token in the source code
    */
   public int getColumn() {
     return column;
   }
 
   /**
    * @return the file this token belongs to
    */
   public File getFile() {
     if (file == null) {
       return new File("Dummy for unit tests");
     }
     return file;
   }
 
   public boolean isCopyBook() {
     return copyBook;
   }
 
   public void setCopyBook(boolean copyBook) {
     this.copyBook = copyBook;
   }
 
   public boolean isGeneratedCode() {
     return generatedCode;
   }
 
   public void setGeneratedCode(boolean generatedCode) {
     this.generatedCode = generatedCode;
   }
 
   /**
    * @return true if there is some trivia like some comments or preprocessing directive between this token and the previous one.
    */
   public boolean hasTrivia() {
     return !trivia.isEmpty();
   }
 
   /**
    * @return the list of trivia located between this token and the previous one
    */
   public List<Trivia> getTrivia() {
     return trivia;
   }
 
  public void addAllTrivia(List<Trivia> trivia) {
     if (this.trivia.isEmpty()) {
       this.trivia = Lists.newArrayList(trivia);
     } else {
       this.trivia.addAll(trivia);
     }
   }
  
  public void addFirstAllTrivia(List<Trivia> trivia) {
    if (this.trivia.isEmpty()) {
      this.trivia = Lists.newArrayList(trivia);
    } else {
      this.trivia.addAll(0, trivia);
    }
  }
 
   @Override
   public int hashCode() {
     final int prime = 31;
     int result = 1;
     result = prime * result + column;
     result = prime * result + line;
     return result;
   }
 
   @Override
   public boolean equals(Object obj) {
     if (this == obj) {
       return true;
     }
     if (obj == null) {
       return false;
     }
     if (getClass() != obj.getClass()) {
       return false;
     }
     Token other = (Token) obj;
     if (column != other.column) {
       return false;
     }
     if (line != other.line) {
       return false;
     }
     return true;
   }
 
   @Override
   public String toString() {
     return getType() + ": " + getValue();
   }
 
   public void setCopyBookOriginalLine(int copyBookOriginalLine) {
     this.copyBookOriginalLine = copyBookOriginalLine;
   }
 
   public int getCopyBookOriginalLine() {
     return copyBookOriginalLine;
   }
 
   public void setCopyBookOriginalFileName(String copyBookOriginalFileName) {
     this.copyBookOriginalFileName = copyBookOriginalFileName;
   }
 
   public String getCopyBookOriginalFileName() {
     return copyBookOriginalFileName;
   }
 }
