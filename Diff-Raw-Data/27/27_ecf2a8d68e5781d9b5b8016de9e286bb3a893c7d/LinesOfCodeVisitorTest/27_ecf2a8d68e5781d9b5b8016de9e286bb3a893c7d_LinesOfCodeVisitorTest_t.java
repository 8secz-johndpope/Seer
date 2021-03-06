 /*
  * Copyright (C) 2010 SonarSource SA
  * All rights reserved
  * mailto:contact AT sonarsource DOT com
  */
 package com.sonar.sslr.squid;
 
 import static org.hamcrest.Matchers.is;
 import static org.junit.Assert.assertThat;
 
 import org.junit.Test;
 import org.sonar.squid.api.SourceProject;
 
 import com.sonar.sslr.api.GenericTokenType;
 import com.sonar.sslr.api.Grammar;
 import com.sonar.sslr.api.Token;
 
 public class LinesOfCodeVisitorTest {
 
   private SourceProject project = new SourceProject("myProject");
   private SquidAstVisitorContextImpl<Grammar> context = new SquidAstVisitorContextImpl<Grammar>(project);
 
   @Test
   public void shouldIncrementTheLinesOfCodeMeasure() {
     assertThat(project.getInt(MyMetrics.LINES_OF_CODE), is(0));
 
     LinesOfCodeVisitor visitor = new LinesOfCodeVisitor(context, MyMetrics.LINES_OF_CODE);
 
     visitor.visitToken(new Token(GenericTokenType.EOF, "", 1, 0));
     assertThat(project.getInt(MyMetrics.LINES_OF_CODE), is(0));
 
     visitor.visitToken(new Token(GenericTokenType.IDENTIFIER, "value", 2, 0));
     assertThat(project.getInt(MyMetrics.LINES_OF_CODE), is(1));
 
     visitor.visitToken(new Token(GenericTokenType.IDENTIFIER, "value", 2, 0));
     assertThat(project.getInt(MyMetrics.LINES_OF_CODE), is(1));
 
     visitor.visitToken(new Token(GenericTokenType.IDENTIFIER, "value", 3, 0));
     assertThat(project.getInt(MyMetrics.LINES_OF_CODE), is(2));
    
    visitor.visitToken(new Token(GenericTokenType.IDENTIFIER, "multi-line-value\nline2 hehe\nline 3 hehe", 4, 0));
    assertThat(project.getInt(MyMetrics.LINES_OF_CODE), is(5));
    
    visitor.visitToken(new Token(GenericTokenType.IDENTIFIER, "this one is already counted!\nbut not this one!", 6, 10));
    assertThat(project.getInt(MyMetrics.LINES_OF_CODE), is(6));
    
    visitor.visitToken(new Token(GenericTokenType.IDENTIFIER, "this one is already counted too!", 7, 0));
    assertThat(project.getInt(MyMetrics.LINES_OF_CODE), is(6));
   }
  
 }
