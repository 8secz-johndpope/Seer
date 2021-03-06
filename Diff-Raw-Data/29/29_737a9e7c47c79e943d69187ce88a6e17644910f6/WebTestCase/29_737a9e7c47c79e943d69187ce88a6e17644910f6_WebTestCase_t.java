 /*
  * Copyright (c) 2002-2007 Gargoyle Software Inc. All rights reserved.
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
 
 import java.io.BufferedInputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.lang.reflect.Method;
 import java.lang.reflect.Modifier;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.Arrays;
 import java.util.List;
 import java.util.ListIterator;
 
 import junit.framework.AssertionFailedError;
 
 import org.apache.commons.io.FileUtils;
 import org.apache.commons.io.IOUtils;
 import org.apache.commons.lang.StringUtils;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 import com.gargoylesoftware.base.testing.BaseTestCase;
 import com.gargoylesoftware.htmlunit.html.HtmlPage;
 
 /**
  * Common superclass for HtmlUnit tests
  *
  * @version $Revision$
  * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
  * @author David D. Kilzer
  * @author Marc Guillemot
  * @author Chris Erskine
  * @author Michael Ottati
  * @author Daniel Gredler
  * @author Ahmed Ashour
  */
 public abstract class WebTestCase extends BaseTestCase {
     /** Constant for the url http://first which is used in the tests. */
     public static final URL URL_FIRST;
 
     /** Constant for the url http://second which is used in the tests. */
     public static final URL URL_SECOND;
 
     /** Constant for the url http://third which is used in the tests. */
     public static final URL URL_THIRD;
 
     /** Constant for the url http://www.gargoylesoftware.com which is used in the tests. */
     public static final URL URL_GARGOYLE;
 
     /**
      * The name of the system property used to determine if files should be generated
      * or not in {@link #createTestPageForRealBrowserIfNeeded(String,List)}
      */
     public static final String PROPERTY_GENERATE_TESTPAGES
         = "com.gargoylesoftware.htmlunit.WebTestCase.GenerateTestpages";
 
     static {
         try {
             URL_FIRST = new URL("http://first");
             URL_SECOND = new URL("http://second");
             URL_THIRD = new URL("http://third");
             URL_GARGOYLE = new URL("http://www.gargoylesoftware.com/");
         }
         catch (final MalformedURLException e) {
             // This is theoretically impossible.
             throw new IllegalStateException("Unable to create url constants");
         }
     }
 
     /**
      * Create an instance.
      * @param name The name of the test.
      */
     public WebTestCase( final String name ) {
         super( name );
     }
 
     /**
      * Load a page with the specified html using the default browser version.
      * @param html The html to use.
      * @return The new page.
      * @throws Exception if something goes wrong.
      */
     protected static final HtmlPage loadPage( final String html ) throws Exception {
         return loadPage(html, null);
     }
 
    /**
      * Load a page with the specified html and collect alerts into the list.
      * @param browserVersion the browser version to use
      * @param html The HTML to use.
      * @param collectedAlerts The list to hold the alerts.
      * @return The new page.
      * @throws Exception If something goes wrong.
      */
     protected static final HtmlPage loadPage(final BrowserVersion browserVersion,
             final String html, final List collectedAlerts )
         throws Exception {
         return loadPage(browserVersion, html, collectedAlerts, URL_GARGOYLE);
     }
 
    /**
      * User the default browser version to load a page with the specified html
      * and collect alerts into the list.
      * @param html The HTML to use.
      * @param collectedAlerts The list to hold the alerts.
      * @return The new page.
      * @throws Exception If something goes wrong.
      */
     protected static final HtmlPage loadPage( final String html, final List collectedAlerts )
         throws Exception {
         return loadPage(BrowserVersion.getDefault(), html, collectedAlerts, URL_GARGOYLE);
     }
 
     /**
      * Return the log that is being used for all testing objects
      * @return The log.
      */
     protected final Log getLog() {
         return LogFactory.getLog(getClass());
     }
 
     /**
      * Load a page with the specified html and collect alerts into the list.
      * @param html The HTML to use.
      * @param collectedAlerts The list to hold the alerts.
      * @param url The URL that will use as the document host for this page
      * @return The new page.
      * @throws Exception If something goes wrong.
      */
     protected static final HtmlPage loadPage( final String html, final List collectedAlerts,
             final URL url) throws Exception {
 
         return loadPage(BrowserVersion.getDefault(), html, collectedAlerts, url);
     }
 
     /**
      * Load a page with the specified html and collect alerts into the list.
      * @param browserVersion the browser version to use
      * @param html The HTML to use.
      * @param collectedAlerts The list to hold the alerts.
      * @param url The URL that will use as the document host for this page
      * @return The new page.
      * @throws Exception If something goes wrong.
      */
     protected static final HtmlPage loadPage(final BrowserVersion browserVersion,
             final String html, final List collectedAlerts, final URL url)
         throws Exception {
 
         final WebClient client = new WebClient(browserVersion);
         if (collectedAlerts != null) {
             client.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
         }
 
         final MockWebConnection webConnection = new MockWebConnection( client );
         webConnection.setDefaultResponse( html );
         client.setWebConnection( webConnection );
 
         final HtmlPage page = (HtmlPage) client.getPage(url);
         return page;
     }
 
     /**
      * Assert that the specified object is null.
      * @param object The object to check.
      */
     public static void assertNull( final Object object ) {
         if (object != null) {
             throw new AssertionFailedError("Expected null but found [" + object + "]");
         }
     }
 
     /**
      * Facility to test external form of urls. Comparing external form of urls is
      * really faster than URL.equals() as the host doesn't need to be resolved.
      * @param expectedUrl the expected url
      * @param actualUrl the url to test
      */
     protected void assertEquals(final URL expectedUrl, final URL actualUrl) {
         assertEquals(expectedUrl.toExternalForm(), actualUrl.toExternalForm());
     }
 
     /**
      * Facility to test external form of urls. Comparing external form of urls is
      * really faster than URL.equals() as the host doesn't need to be resolved.
      * @param message the message to display if assertion fails
      * @param expectedUrl the string representation of the expected url
      * @param actualUrl the url to test
      */
     protected void assertEquals(final String message, final URL expectedUrl, final URL actualUrl) {
         assertEquals(message, expectedUrl.toExternalForm(), actualUrl.toExternalForm());
     }
 
     /**
      * Facility to test external form of an url.
      * @param expectedUrl the string representation of the expected url
      * @param actualUrl the url to test
      */
     protected void assertEquals(final String expectedUrl, final URL actualUrl) {
         assertEquals(expectedUrl, actualUrl.toExternalForm());
     }
 
     /**
      * Facility method to avoid having to create explicitly a list from
      * a String[] (for example when testing received alerts).
      * Transforms the String[] to a List before calling
      * {@link junit.framework.Assert#assertEquals(java.lang.Object, java.lang.Object)}.
      * @param expected the expected strings
      * @param actual the collection of strings to test
      */
     protected void assertEquals(final String[] expected, final List actual) {
         assertEquals(Arrays.asList(expected), actual);
     }
 
     /**
      * Facility method to avoid having to create explicitly a list from
      * a String[] (for example when testing received alerts).
      * Transforms the String[] to a List before calling
      * {@link junit.framework.Assert#assertEquals(java.lang.String, java.lang.Object, java.lang.Object)}.
      * @param message the message to display if assertion fails
      * @param expected the expected strings
      * @param actual the collection of strings to test
      */
     protected void assertEquals(final String message, final String[] expected, final List actual) {
         assertEquals(message, Arrays.asList(expected), actual);
     }
 
     /**
      * Facility to test external form of an url.
      * @param message the message to display if assertion fails
      * @param expectedUrl the string representation of the expected url
      * @param actualUrl the url to test
      */
     protected void assertEquals(final String message, final String expectedUrl, final URL actualUrl) {
         assertEquals(message, expectedUrl, actualUrl.toExternalForm());
     }
 
     /**
      * Return an input stream for the specified file name.  Refer to {@link #getFileObject(String)}
      * for details on how the file is located.
      * @param fileName The base file name.
      * @return The input stream.
      * @throws FileNotFoundException If the file cannot be found.
      */
     public static InputStream getFileAsStream( final String fileName ) throws FileNotFoundException {
         return new BufferedInputStream(new FileInputStream(getFileObject(fileName)));
     }
 
     /**
      * Return a File object for the specified file name.  This is different from just
      * <code>new File(fileName)</code> because it will adjust the location of the file
      * depending on how the code is being executed.
      *
      * @param fileName The base filename.
      * @return The new File object.
      * @throws FileNotFoundException if !file.exists()
      */
     public static File getFileObject( final String fileName ) throws FileNotFoundException {
         final String localizedName = fileName.replace( '/', File.separatorChar );
 
         File file = new File(localizedName);
         if (!file.exists()) {
             file = new File("../../" + localizedName);
         }
 
         if (!file.exists()) {
             try {
                 System.out.println("currentDir=" + new File(".").getCanonicalPath());
             }
             catch (final IOException e) {
                 e.printStackTrace();
             }
             throw new FileNotFoundException(localizedName);
         }
         return file;
     }
 
     /**
      * Facility method transforming expectedAlerts to a list and calling
      * {@link #createTestPageForRealBrowserIfNeeded(String, List)}
      * @param content the content of the html page
      * @param expectedAlerts the expected alerts
      * @throws IOException if writing file fails
      */
     protected void createTestPageForRealBrowserIfNeeded(final String content, final String[] expectedAlerts)
         throws IOException {
         createTestPageForRealBrowserIfNeeded(content, Arrays.asList(expectedAlerts));
     }
 
     /**
      * Generates an instrumented html file in the temporary dir to easily make a manual test in a real browser.
      * The file is generated only if the system property {@link #PROPERTY_GENERATE_TESTPAGES} is set.
      * @param content the content of the html page
      * @param expectedAlerts the expected alerts
      * @throws IOException if writing file fails
      */
     protected void createTestPageForRealBrowserIfNeeded(final String content, final List expectedAlerts)
         throws IOException {
         final Log log = LogFactory.getLog(WebTestCase.class);
         if (System.getProperty(PROPERTY_GENERATE_TESTPAGES) != null) {
             // should be optimized....
 
             // calls to alert() should be replaced by call to custom function
             String newContent = StringUtils.replace(content,
                    "alert(", "htmlunitReserved_caughtAlert(");
 
             final String instrumentationJS = createInstrumentationScript(expectedAlerts);
 
             // first version, we assume that there is a <head> and a </body> or a </frameset>
             if (newContent.indexOf("<head>") > -1) {
                 newContent = StringUtils.replaceOnce(newContent, "<head>", "<head>" + instrumentationJS);
             }
             else {
                 newContent = StringUtils.replaceOnce(newContent, "<html>",
                         "<html>\n<head>\n" + instrumentationJS + "\n</head>\n");
             }
             final String endScript = "\n<script>htmlunitReserved_addSummaryAfterOnload();</script>\n";
             if (newContent.indexOf("</body>") != -1) {
                 newContent = StringUtils.replaceOnce(newContent, "</body>",  endScript + "</body>");
             }
             else {
                 throw new RuntimeException("Currently only content with a <head> and a </body> is supported");
             }
 
             final File f = File.createTempFile("test", ".html");
             FileUtils.writeStringToFile(f, newContent, "ISO-8859-1");
             log.info("Test file written: " + f.getAbsolutePath());
         }
         else {
             log.debug("System property \"" + PROPERTY_GENERATE_TESTPAGES
                     + "\" not set, don't generate test html page for real browser");
         }
     }
 
     /**
      * @param expectedAlerts the list of the expected alerts
      * @return the script to be included at the beginning of the generated html file
      * @throws IOException in case of problem
      */
     private String createInstrumentationScript(final List expectedAlerts) throws IOException {
         // generate the js code
         final InputStream is = getClass().getClassLoader().getResourceAsStream(
                 "com/gargoylesoftware/htmlunit/alertVerifier.js");
         final String baseJS = IOUtils.toString(is);
         IOUtils.closeQuietly(is);
 
         final StringBuffer sb = new StringBuffer();
         sb.append("\n<script type='text/javascript'>\n");
         sb.append("var htmlunitReserved_tab = [");
         for (final ListIterator iter = expectedAlerts.listIterator(); iter.hasNext();) {
             if (iter.hasPrevious()) {
                 sb.append(", ");
             }
             final String message = (String) iter.next();
             sb.append("{expected: \"").append(message).append("\"}");
         }
         sb.append("];\n\n");
         sb.append(baseJS);
         sb.append("</script>\n");
         return sb.toString();
     }
 
     /**
      * Convenience method to pull the MockWebConnection out of an HtmlPage created with
      * the loadPage method.
      * @param page HtmlPage to get the connection from
      * @return the MockWebConnection that served this page
      */
     protected static final MockWebConnection getMockConnection(final HtmlPage page) {
         return (MockWebConnection) page.getWebClient().getWebConnection();
     }
 
     /**
      * Runs the calling JUnit test again and fails only if it already runs.<br/>
      * This is helpful for tests that don't currently work but should work one day,
      * when the tested functionality has been implemented.<br/>
      * The right way to use it is:
      * <pre>
      * public void testXXX() {
      *   if (notYetImplemented()) {
      *       return;
      *   }
      *
      *   ... the real (now failing) unit test
      * }
      * </pre>
      * @return <false> when not itself already in the call stack
      */
     protected boolean notYetImplemented() {
         if (notYetImplementedFlag.get() != null) {
             return false;
         }
         notYetImplementedFlag.set(Boolean.TRUE);
 
         final Method testMethod = findRunningJUnitTestMethod();
         try {
             getLog().info("Running " + testMethod.getName() + " as not yet implemented");
             testMethod.invoke(this, new Class[] {});
             fail(testMethod.getName() + " is marked as not implemented but already works");
         }
         catch (final Exception e) {
             getLog().info(testMethod.getName() + " fails which is normal as it is not yet implemented");
             // method execution failed, it is really "not yet implemented"
         }
         finally {
             notYetImplementedFlag.set(null);
         }
 
         return true;
     }
 
     /**
      * Finds from the call stack the active running JUnit test case
      * @return the test case method
      * @throws RuntimeException if no method could be found.
      */
     private Method findRunningJUnitTestMethod() {
         final Class cl = getClass();
         final Class[] args = new Class[] {};
 
         // search the initial junit test
         final Throwable t = new Exception();
         for (int i = t.getStackTrace().length - 1; i >= 0; i--) {
             final StackTraceElement element = t.getStackTrace()[i];
             if (element.getClassName().equals(cl.getName())) {
                 try {
                     final Method m = cl.getMethod(element.getMethodName(), args);
                     if (isPublicTestMethod(m)) {
                         return m;
                     }
                 }
                 catch (final Exception e) {
                     // can't acces, ignore it
                 }
             }
         }
 
         throw new RuntimeException("No JUnit test case method found in call stack");
     }
 
     /**
      * From Junit. Test if the method is a junit test.
      * @param method the method
      * @return <code>true</code> if this is a junit test.
      */
     private boolean isPublicTestMethod(final Method method) {
         final String name = method.getName();
         final Class[] parameters = method.getParameterTypes();
         final Class returnType = method.getReturnType();
 
         return parameters.length == 0 && name.startsWith("test")
             && returnType.equals(Void.TYPE)
             && Modifier.isPublic(method.getModifiers());
     }
 
     private static final ThreadLocal notYetImplementedFlag = new ThreadLocal();
 }
