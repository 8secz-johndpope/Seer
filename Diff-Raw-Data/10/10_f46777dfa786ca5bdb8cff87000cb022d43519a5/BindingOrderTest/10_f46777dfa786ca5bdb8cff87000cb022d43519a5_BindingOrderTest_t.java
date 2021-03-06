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
 
 package com.google.inject;
 
 import junit.framework.TestCase;
 
 /**
  * @author jessewilson@google.com (Jesse Wilson)
  */
 public class BindingOrderTest extends TestCase {
 
   public void testBindingOutOfOrder() {
     Guice.createInjector(new AbstractModule() {
       protected void configure() {
         bind(BoundFirst.class);
         bind(BoundSecond.class).to(BoundSecondImpl.class);
       }
     });
   }
 
   public static class BoundFirst {
     @Inject public BoundFirst(BoundSecond boundSecond) { }
   }
 
   interface BoundSecond { }
   static class BoundSecondImpl implements BoundSecond { }
 
   public void testBindingOrderAndScopes() {
     Injector injector = Guice.createInjector(new AbstractModule() {
       protected void configure() {
        bind(A.class);
         bind(B.class).asEagerSingleton();
       }
     });
 
     // For untargetted bindings with scopes, sometimes we lose the scope at
     // injector time. This is because we use the injector's just-in-time
     // bindings to build these, rather than the bind command. This is a known
     // bug.
    assertSame(injector.getInstance(A.class).b, injector.getInstance(A.class).b);
   }
 
   static class A {
    @Inject B b;
   }
 
   static class B { }
 }
