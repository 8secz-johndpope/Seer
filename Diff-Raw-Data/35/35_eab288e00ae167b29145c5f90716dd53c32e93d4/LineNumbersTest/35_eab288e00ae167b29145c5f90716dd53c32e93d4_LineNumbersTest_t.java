 /**
  * Copyright (C) 2008 Google Inc.
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
 
 
 package com.google.inject.internal;
 
 import com.google.inject.AbstractModule;
 import com.google.inject.CreationException;
 import com.google.inject.Guice;
 import com.google.inject.Inject;
 import com.google.inject.matcher.Matchers;
 import junit.framework.TestCase;
 import org.aopalliance.intercept.MethodInterceptor;
 import org.aopalliance.intercept.MethodInvocation;
 
 /**
  * @author jessewilson@google.com (Jesse Wilson)
  */
 public class LineNumbersTest extends TestCase {
 
   public void testCanHandleLineNumbersForGuiceGeneratedClasses() {
     try {
       Guice.createInjector(new AbstractModule() {
         protected void configure() {
          bindInterceptor(Matchers.only(A.class), Matchers.any(),
               new MethodInterceptor() {
                 public Object invoke(MethodInvocation methodInvocation) {
                   return null;
                 }
               });
 
           bind(A.class);
         }
       });
       fail();
     } catch (CreationException expected) {
       assertTrue(expected.getMessage().contains("LineNumbersTest$B"));
      assertTrue(expected.getMessage().contains("No bindings to that type were found."));
     }
   }
 
   static class A {
     @Inject A(B b) {}
   }
   interface B {}
 
 }
