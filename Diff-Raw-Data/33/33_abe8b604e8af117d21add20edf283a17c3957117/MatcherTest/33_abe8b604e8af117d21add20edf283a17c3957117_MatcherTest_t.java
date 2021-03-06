 /**
  * Copyright (C) 2006 Google Inc.
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
 
 package com.google.inject.matcher;
 
 import static com.google.inject.matcher.Matchers.annotatedWith;
 import static com.google.inject.matcher.Matchers.any;
 import static com.google.inject.matcher.Matchers.inPackage;
 import static com.google.inject.matcher.Matchers.not;
 import static com.google.inject.matcher.Matchers.only;
 import static com.google.inject.matcher.Matchers.returns;
 import static com.google.inject.matcher.Matchers.subclassesOf;
 import java.lang.annotation.Retention;
 import java.lang.annotation.RetentionPolicy;
 import java.lang.reflect.Method;
 import junit.framework.TestCase;
 
 /**
  * @author crazybob@google.com (Bob Lee)
  */
 public class MatcherTest extends TestCase {
 
   public void testAny() {
     assertTrue(any().matches(null));
   }
 
   public void testNot() {
     assertFalse(not(any()).matches(null));
   }
 
   public void testAnd() {
     assertTrue(any().and(any()).matches(null));
     assertFalse(any().and(not(any())).matches(null));
   }
 
   public void testAnnotatedWith() {
     assertTrue(annotatedWith(Foo.class).matches(Bar.class));
     assertFalse(annotatedWith(Foo.class).matches(
         MatcherTest.class.getMethods()[0]));

    try {
      annotatedWith(Baz.class).matches(Car.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
   }
 
   public void testSubclassesOf() {
     assertTrue(subclassesOf(Runnable.class).matches(Runnable.class));
     assertTrue(subclassesOf(Runnable.class).matches(MyRunnable.class));
     assertFalse(subclassesOf(Runnable.class).matches(Object.class));
   }
 
   public void testOnly() {
     assertTrue(only(1000).matches(new Integer(1000)));
     assertFalse(only(1).matches(new Integer(1000)));
   }
 
   public void testSameAs() {
     Object o = new Object();
     assertTrue(only(o).matches(o));
     assertFalse(only(o).matches(new Object()));
   }
 
   public void testInPackage() {
     assertTrue(inPackage(Matchers.class.getPackage())
         .matches(MatcherTest.class));
     assertFalse(inPackage(Matchers.class.getPackage())
         .matches(Object.class));
   }
 
   public void testReturns() throws NoSuchMethodException {
     Matcher<Method> predicate = returns(only(String.class));
     assertTrue(predicate.matches(
         Object.class.getMethod("toString")));
     assertFalse(predicate.matches(
         Object.class.getMethod("hashCode")));
   }
   
   static abstract class MyRunnable implements Runnable {}
   
   @Retention(RetentionPolicy.RUNTIME)
   @interface Foo {}
 
   @Foo
   static class Bar {}

  @interface Baz {}

  @Baz
  static class Car {}
 }
