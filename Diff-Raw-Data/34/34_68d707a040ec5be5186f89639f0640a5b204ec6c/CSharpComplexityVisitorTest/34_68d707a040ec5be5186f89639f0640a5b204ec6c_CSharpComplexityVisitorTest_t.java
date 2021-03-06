 /*
  * Copyright (C) 2010 SonarSource SA
  * All rights reserved
  * mailto:contact AT sonarsource DOT com
  */
 package com.sonar.csharp.squid.metric;
 
 import static org.hamcrest.Matchers.is;
 import static org.junit.Assert.assertThat;
 
 import java.io.File;
 import java.nio.charset.Charset;
 
 import org.apache.commons.io.FileUtils;
 import org.junit.Test;
 import org.sonar.plugins.csharp.api.CSharpMetric;
 import org.sonar.squid.Squid;
 import org.sonar.squid.api.SourceProject;
 
 import com.sonar.csharp.squid.CSharpConfiguration;
 import com.sonar.csharp.squid.scanner.CSharpAstScanner;
 
 public class CSharpComplexityVisitorTest {
 
   @Test
   public void testScanFile() {
     Squid squid = new Squid(new CSharpConfiguration(Charset.forName("UTF-8")));
     squid.register(CSharpAstScanner.class).scanFile(readFile("/metric/Money.cs"));
     SourceProject project = squid.decorateSourceCodeTreeWith(CSharpMetric.COMPLEXITY);
 
     assertThat(project.getInt(CSharpMetric.COMPLEXITY), is(76));
   }
 
   @Test
   public void testScanSimpleFile() {
     Squid squid = new Squid(new CSharpConfiguration(Charset.forName("UTF-8")));
     squid.register(CSharpAstScanner.class).scanFile(readFile("/metric/simpleFile.cs"));
     SourceProject project = squid.decorateSourceCodeTreeWith(CSharpMetric.COMPLEXITY);
 
     assertThat(project.getInt(CSharpMetric.COMPLEXITY), is(15));
   }
 
  @Test
  public void testRealLifeFile() {
    Squid squid = new Squid(new CSharpConfiguration(Charset.forName("UTF-8")));
    squid.register(CSharpAstScanner.class).scanFile(readFile("/metric/BasicConfigurator.cs"));
    SourceProject project = squid.decorateSourceCodeTreeWith(CSharpMetric.values());

    assertThat(project.getInt(CSharpMetric.COMPLEXITY), is(6));
  }

   protected File readFile(String path) {
     return FileUtils.toFile(getClass().getResource(path));
   }
 
 }
