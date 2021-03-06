 /*
  * Copyright (c) 2002-2008 Gargoyle Software Inc. All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *
  * 1. Redistributions of source code must retain the above copyright notice,
  *    this list of conditions and the following disclaimer.
  * 2. Redistributions in binary form must reproduce the above copyright notice,
  *    this list of conditions and the following disclaimer in the documentation
  *    and/or other materials provided with the distribution.
  * 3. The end-user documentation included with the redistribution, if any, must
  *    include the following acknowledgment:
  *
  *       "This product includes software developed by Gargoyle Software Inc.
  *        (http://www.GargoyleSoftware.com/)."
  *
  *    Alternately, this acknowledgment may appear in the software itself, if
  *    and wherever such third-party acknowledgments normally appear.
  * 4. The name "Gargoyle Software" must not be used to endorse or promote
  *    products derived from this software without prior written permission.
  *    For written permission, please contact info@GargoyleSoftware.com.
  * 5. Products derived from this software may not be called "HtmlUnit", nor may
  *    "HtmlUnit" appear in their name, without prior written permission of
  *    Gargoyle Software Inc.
  *
  * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
  * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
  * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GARGOYLE
  * SOFTWARE INC. OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
  * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
  * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package com.gargoylesoftware.htmlunit;
 
 import static org.junit.Assert.assertTrue;
 
 import java.lang.annotation.ElementType;
 import java.lang.annotation.Retention;
 import java.lang.annotation.RetentionPolicy;
 import java.lang.annotation.Target;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 
 import org.junit.internal.runners.CompositeRunner;
 import org.junit.internal.runners.InitializationError;
 import org.junit.internal.runners.JUnit4ClassRunner;
 import org.junit.internal.runners.TestMethod;
 import org.junit.runner.Description;
 import org.junit.runner.notification.RunNotifier;
 
 /**
  * The custom runner <code>BrowserRunner</code> implements browser parameterized
  * tests. When running a test class, instances are created for the
  * cross-product of the test methods and {@link BrowserVersion}s.
  *
  * For example, write:
  * <pre>
  * &#064;RunWith(BrowserRunner.class)
  * public class SomeTest extends WebTestCase {
  *
  *    &#064;Test
  *    &#064;Browsers({Browser.FIREFOX_2})
  *    public void test() {
  *       //your test case that succeeds with only Firefox 2
  *    }
  * }
  * </pre>
  *
  * @version $Revision$
  * @author Ahmed Ashour
  */
 public class BrowserRunner extends CompositeRunner {
 
     /**
      * Browser.
      */
     public enum Browser {
         /** Internet Explorer 6.*/
         INTERNET_EXPLORER_6,
         
         /** Internet Explorer 7.*/
         INTERNET_EXPLORER_7,
 
         /** Firefox 7.*/
         FIREFOX_2;
     }
     
     
     /**
      * Browsers to test with, default value is all.
      */
     @Retention(RetentionPolicy.RUNTIME)
     @Target(ElementType.METHOD)
     public static @interface Browsers {
 
         /**
          * The browsers which the case succeeds (but fails with remaining ones).
          */
         Browser[] value() default {
             Browser.INTERNET_EXPLORER_6, Browser.INTERNET_EXPLORER_7, Browser.FIREFOX_2
         };
     }
     
     /**
      * Browsers with which the case is not yet implemented, default value is all.
      */
     @Retention(RetentionPolicy.RUNTIME)
     @Target(ElementType.METHOD)
     public static @interface NotYetImplemented {
 
         /**
          * The browsers with which the case is not yet implemented.
          */
         Browser[] value() default {
             Browser.INTERNET_EXPLORER_6, Browser.INTERNET_EXPLORER_7, Browser.FIREFOX_2
         };
     }
 
     static class TestClassRunnerForBrowserVersion extends JUnit4ClassRunner {
 
         private final BrowserVersion browserVersion_;
         public TestClassRunnerForBrowserVersion(final Class< ? extends WebTestCase> klass,
             final BrowserVersion browserVersion) throws InitializationError {
             super(klass);
             this.browserVersion_ = browserVersion;
         }
 
         @Override
         protected void invokeTestMethod(final Method method, final RunNotifier notifier) {
             final Description description = methodDescription(method);
             Object test;
             try {
                 test = createTest();
             }
             catch (final InvocationTargetException e) {
                 notifier.testAborted(description, e.getCause());
                 return;
             }
             catch (final Exception e) {
                 notifier.testAborted(description, e);
                 return;
             }
             final Browsers browsers = method.getAnnotation(Browsers.class);
             final boolean shouldFail = browsers != null && !isDefinedIn(browsers.value());
             
             final NotYetImplemented notYetImplementedBrowsers = method.getAnnotation(NotYetImplemented.class);
             final boolean notYetImplemented = notYetImplementedBrowsers != null
                 && isDefinedIn(notYetImplementedBrowsers.value());
             final TestMethod testMethod = wrapMethod(method);
             new BrowserRoadie(test, testMethod, notifier, description, method, shouldFail, notYetImplemented,
                 getShortname(browserVersion_)).run();
         }
 
         @Override
         protected Object createTest() throws Exception {
             final WebTestCase object = (WebTestCase) super.createTest();
             object.setBrowserVersion(browserVersion_);
             return object;
         }
 
         @Override
         protected String getName() {
             return String.format("[%s]", getShortname(browserVersion_));
         }
 
         @Override
         protected String testName(final Method method) {
             return String.format("%s[%s]", method.getName(), getShortname(browserVersion_));
         }
 
         /**
          * Returns true if current {@link #browserVersion_} is contained in the specifidc <tt>browsers</tt>.
          */
         private boolean isDefinedIn(final Browser[] browsers) {
             for (final Browser browser : browsers) {
                 switch(browser) {
                     case INTERNET_EXPLORER_6:
                         if (browserVersion_ == BrowserVersion.INTERNET_EXPLORER_6_0) {
                             return true;
                         }
                         break;
 
                     case INTERNET_EXPLORER_7:
                         if (browserVersion_ == BrowserVersion.INTERNET_EXPLORER_7_0) {
                             return true;
                         }
                         break;
 
                     case FIREFOX_2:
                         if (browserVersion_ == BrowserVersion.FIREFOX_2) {
                             return true;
                         }
                         break;
 
                     default:
                 }
             }
             return false;
         }
    
     }
 
     private static String getShortname(final BrowserVersion browserVersion) {
         if (browserVersion == BrowserVersion.INTERNET_EXPLORER_6_0) {
             return "Internet Explorer 6";
         }
         else if (browserVersion == BrowserVersion.INTERNET_EXPLORER_7_0) {
             return "Internet Explorer 7";
         }
         else if (browserVersion == BrowserVersion.FIREFOX_2) {
             return "Firefox 2";
         }
         else {
             return browserVersion.getApplicationName() + browserVersion.getApplicationVersion();
         }
     }
 
     /**
      * Constructs a new instance.
      *
      * @param klass test case.
      * @throws Exception If an error occurs.
      */
     public BrowserRunner(final Class< ? extends WebTestCase> klass) throws Exception {
         super(klass.getName());
         assertTrue("Test case must extend WebTestCase", WebTestCase.class.isAssignableFrom(klass));
         add(new TestClassRunnerForBrowserVersion(klass, BrowserVersion.INTERNET_EXPLORER_6_0));
         add(new TestClassRunnerForBrowserVersion(klass, BrowserVersion.INTERNET_EXPLORER_7_0));
         add(new TestClassRunnerForBrowserVersion(klass, BrowserVersion.FIREFOX_2));
     }
 
 }
