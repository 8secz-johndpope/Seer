 /*
  * SonarSource Language Recognizer
  * Copyright (C) 2010 SonarSource
  * dev@sonar.codehaus.org
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 3 of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
  */
 package com.sonar.sslr.toolkit.internal;
 
 import org.apache.commons.io.IOUtils;
 
 import java.io.IOException;
 import java.io.InputStream;
 
public final class CssLoader {
 
   private static final String CSS_PATH = "/com/sonar/sslr/toolkit/sourceCodeEditor.css";
 
   private CssLoader() {
   }
 
   public static final String getCss() {
     try {
       InputStream inputStream = CssLoader.class.getResourceAsStream(CSS_PATH);
       return IOUtils.toString(inputStream);
     } catch (IOException e) {
       throw new RuntimeException(e);
     }
   }
 
 }
