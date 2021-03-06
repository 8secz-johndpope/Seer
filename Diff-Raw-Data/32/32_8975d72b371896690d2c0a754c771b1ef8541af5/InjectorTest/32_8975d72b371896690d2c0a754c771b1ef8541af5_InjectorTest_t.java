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
 
 import java.lang.annotation.Retention;
 import static java.lang.annotation.RetentionPolicy.RUNTIME;
 import junit.framework.TestCase;
 
 /**
  * @author crazybob@google.com (Bob Lee)
  */
 public class InjectorTest extends TestCase {
 
   @Retention(RUNTIME)
   @BindingAnnotation @interface Other {}
 
   @Retention(RUNTIME)
   @BindingAnnotation @interface S {}
 
   @Retention(RUNTIME)
   @BindingAnnotation @interface I {}
 
  public void testToStringDoesNotInfinitelyRecurse() {
    Guice.createInjector().toString();
  }

   public void testProviderMethods() throws CreationException {
     SampleSingleton singleton = new SampleSingleton();
     SampleSingleton other = new SampleSingleton();
 
     BinderImpl builder = new BinderImpl();
     builder.bind(SampleSingleton.class).toInstance(singleton);
     builder.bind(SampleSingleton.class)
         .annotatedWith(Other.class)
         .toInstance(other);
     Injector injector = builder.createInjector();
 
     assertSame(singleton,
         injector.getInstance(Key.get(SampleSingleton.class)));
     assertSame(singleton, injector.getInstance(SampleSingleton.class));
 
     assertSame(other,
         injector.getInstance(Key.get(SampleSingleton.class, Other.class)));
   }
 
   static class SampleSingleton {}
 
   public void testInjection() throws CreationException {
     Injector injector = createFooInjector();
     Foo foo = injector.getInstance(Foo.class);
 
     assertEquals("test", foo.s);
     assertEquals("test", foo.bar.getTee().getS());
     assertSame(foo.bar, foo.copy);
     assertEquals(5, foo.i);
     assertEquals(5, foo.bar.getI());
 
     // Test circular dependency.
     assertSame(foo.bar, foo.bar.getTee().getBar());
   }
 
   private Injector createFooInjector() throws CreationException {
     return Guice.createInjector(new AbstractModule() {
       protected void configure() {
         bind(Bar.class).to(BarImpl.class);
         bind(Tee.class).to(TeeImpl.class);
         bindConstant().annotatedWith(S.class).to("test");
         bindConstant().annotatedWith(I.class).to(5);
       }
     });
   }
 
   public void testGetInstance() throws CreationException {
     Injector injector = createFooInjector();
 
     Bar bar = injector.getInstance(Key.get(Bar.class));
     assertEquals("test", bar.getTee().getS());
     assertEquals(5, bar.getI());
   }
 
   public void testIntAndIntegerAreInterchangeable()
       throws CreationException {
     BinderImpl builder = new BinderImpl();
     builder.bindConstant().annotatedWith(I.class).to(5);
     Injector injector = builder.createInjector();
     IntegerWrapper iw = injector.getInstance(IntegerWrapper.class);
     assertEquals(5, (int) iw.i);
   }
 
   static class IntegerWrapper {
     @Inject @I Integer i;
   }
 
   static class Foo {
 
     @Inject Bar bar;
     @Inject Bar copy;
 
     @Inject @S String s;
 
     int i;
 
     @Inject
     void setI(@I int i) {
       this.i = i;
     }
   }
 
   interface Bar {
 
     Tee getTee();
     int getI();
   }
 
   @Singleton
   static class BarImpl implements Bar {
 
     @Inject @I int i;
 
     Tee tee;
 
     @Inject
     void initialize(Tee tee) {
       this.tee = tee;
     }
 
     public Tee getTee() {
       return tee;
     }
 
     public int getI() {
       return i;
     }
   }
 
   interface Tee {
 
     String getS();
     Bar getBar();
   }
 
   static class TeeImpl implements Tee {
 
     final String s;
     @Inject Bar bar;
 
     @Inject
     TeeImpl(@S String s) {
       this.s = s;
     }
 
     public String getS() {
       return s;
     }
 
     public Bar getBar() {
       return bar;
     }
   }
 
   public void testInjectStatics() throws CreationException {
     BinderImpl builder = new BinderImpl();
     builder.bindConstant().annotatedWith(S.class).to("test");
     builder.bindConstant().annotatedWith(I.class).to(5);
     builder.requestStaticInjection(Static.class);
     builder.createInjector();
 
     assertEquals("test", Static.s);
     assertEquals(5, Static.i);
   }
 
   static class Static {
 
     @Inject @I static int i;
 
     static String s;
 
     @Inject static void setS(@S String s) {
       Static.s = s;
     }
   }
 
   public void testPrivateInjection() throws CreationException {
     Injector injector = Guice.createInjector(new AbstractModule() {
       protected void configure() {
         bind(String.class).toInstance("foo");
         bind(int.class).toInstance(5);
       }
     });
 
     Private p = injector.getInstance(Private.class);
     assertEquals("foo", p.fromConstructor);
     assertEquals(5, p.fromMethod);
   }
 
   static class Private {
     String fromConstructor;
     int fromMethod;
 
     @Inject
     private Private(String fromConstructor) {
       this.fromConstructor = fromConstructor;
     }
 
     @Inject
     private void setInt(int i) {
       this.fromMethod = i;
     }
   }
 
   public void testProtectedInjection() throws CreationException {
     Injector injector = Guice.createInjector(new AbstractModule() {
       protected void configure() {
         bind(String.class).toInstance("foo");
         bind(int.class).toInstance(5);
       }
     });
 
     Protected p = injector.getInstance(Protected.class);
     assertEquals("foo", p.fromConstructor);
     assertEquals(5, p.fromMethod);
   }
 
   static class Protected {
     String fromConstructor;
     int fromMethod;
 
     @Inject
     protected Protected(String fromConstructor) {
       this.fromConstructor = fromConstructor;
     }
 
     @Inject
     protected void setInt(int i) {
       this.fromMethod = i;
     }
   }
 
   public void testInstanceInjectionHappensAfterFactoriesAreSetUp() {
     Guice.createInjector(new AbstractModule() {
       protected void configure() {
         bind(Object.class).toInstance(new Object() {
           @Inject Runnable r;
         });
 
         bind(Runnable.class).to(MyRunnable.class);
       }
     });
   }
 
   static class MyRunnable implements Runnable {
    public void run() {}
   }  
 }
