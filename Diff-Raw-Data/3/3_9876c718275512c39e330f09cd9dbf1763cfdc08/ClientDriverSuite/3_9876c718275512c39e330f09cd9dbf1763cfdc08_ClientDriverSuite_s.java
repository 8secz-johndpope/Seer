 /*
  * Created on Feb 25, 2006
  *
  */
 package com.thoughtworks.selenium;
 
 import junit.extensions.*;
 import junit.framework.*;
 
 import org.openqa.selenium.server.*;
 
 import com.thoughtworks.selenium.corebased.*;
 
 /** The wrapper test suite for these tests, which spawns an in-process Selenium Server
  * for simple integration testing.
  *
  * <p>Normally, users should start the Selenium Server
  * out-of-process, and just leave it up and running, available for the tests to use.
  * But, if you like, you can do what we do here and start a Selenium Server before
  * launching the tests.</p>
  *
  * <p>Note that we don't recommend starting and stopping the
  * entire server during each test's setUp and tearDown for these Integration tests;
  * it shouldn't be necessary, and doing so may conceal bugs in the server.</p>
  *
  *
  *  @author Dan Fabulich
  *
  */
 public class ClientDriverSuite extends TestCase {
 
     /** Construct a test suite containing the other integration tests,
      * wrapping them up in a TestSetup object that will launch the Selenium
      * Server in-proc.
      * @return a test suite containing tests to run
      */
 	public static Test suite() {
         boolean isProxyInjectionMode = System.getProperty("selenium.proxyInjectionMode")!=null
         && System.getProperty("selenium.proxyInjectionMode").equals("true");
     
         String forcedBrowserMode = System.getProperty("selenium.forcedBrowserMode");
         if (forcedBrowserMode==null) {
             forcedBrowserMode = "none";
         }
     
         try {
 // TODO This class extends TestCase to workaround MSUREFIRE-113
             // http://jira.codehaus.org/browse/MSUREFIRE-113
 // Once that bug is fixed, this class should be a TestSuite, not a TestCase
             TestSuite supersuite = new TestSuite(ClientDriverSuite.class.getName());
             TestSuite suite = new TestSuite(ClientDriverSuite.class.getName());
             suite.addTestSuite(ApacheMyFacesSuggestTest.class);
             if (isProxyInjectionMode) {
                if (forcedBrowserMode.equals("*piiexplore")) { // BUG in PI mode multiple window handling
 //                  once frames support is added to the main trunk, we will be able to run the following in non-proxy injection mode:
                     suite.addTestSuite(TestFramesClick.class);
                     suite.addTestSuite(TestFramesOpen.class);
                     suite.addTestSuite(TestFramesNested.class);
                }
             }
             suite.addTest(I18nTest.suite());
             suite.addTestSuite(RealDealIntegrationTest.class);
             suite.addTestSuite(TestErrorChecking.class);
             suite.addTestSuite(TestJavascriptParameters.class);
             suite.addTestSuite(TestClick.class);
             suite.addTestSuite(GoogleTestSearch.class);
             suite.addTestSuite(GoogleTest.class);
             suite.addTestSuite(TestCheckUncheck.class);
             suite.addTestSuite(TestClick.class);
             suite.addTestSuite(TestXPathLocators.class);
             suite.addTestSuite(TestClickJavascriptHref.class);
             suite.addTestSuite(TestCommandError.class);
             suite.addTestSuite(TestComments.class);
             suite.addTestSuite(TestFailingAssert.class);
             suite.addTestSuite(TestFailingVerifications.class);
             suite.addTestSuite(TestFocusOnBlur.class);
             suite.addTestSuite(TestGoBack.class);
             suite.addTestSuite(TestImplicitLocators.class);
             suite.addTestSuite(TestLocators.class);
             suite.addTestSuite(TestOpen.class);
             suite.addTestSuite(TestPatternMatching.class);
             suite.addTestSuite(TestPause.class);
             if (!isProxyInjectionMode) {
                 suite.addTestSuite(TestSelectWindow.class);
             }
             suite.addTestSuite(TestStore.class);
             suite.addTestSuite(TestSubmit.class);
             suite.addTestSuite(TestType.class);
             suite.addTestSuite(TestVerifications.class);
             suite.addTestSuite(TestWait.class);
             suite.addTestSuite(TestSelect.class);
             suite.addTestSuite(TestEditable.class);
             if (!isProxyInjectionMode || forcedBrowserMode.equals("*piiexplore")) { // BUG in PI mode propogation of state
                 suite.addTestSuite(TestPrompt.class);
                 suite.addTestSuite(TestConfirmations.class);
                 suite.addTestSuite(TestAlerts.class);
                 suite.addTestSuite(TestRefresh.class);
             }
             suite.addTestSuite(TestVisibility.class);
             suite.addTestSuite(TestMultiSelect.class);
             if (!isProxyInjectionMode) {
                 suite.addTestSuite(TestWaitInPopupWindow.class);
             }
             suite.addTestSuite(TestWaitFor.class);
             suite.addTestSuite(TestWaitForNot.class);
             ClientDriverTestSetup setup = new ClientDriverTestSetup(suite);
             supersuite.addTest(setup);
             return supersuite;
         } catch (RuntimeException e) {
             e.printStackTrace();
             throw e;
         }
     }
 
     /** A TestSetup decorator that runs a super setUp and tearDown at the
      * beginning and end of the entire run: in this case, we use it to
      * startup and shutdown the in-process Selenium Server.
      *
      *
      *  @author danielf
      *
      */
 static class ClientDriverTestSetup extends TestSetup {
     SeleniumServer server;
     
 	public ClientDriverTestSetup(Test test) {
         super(test);
     }
     
 	public void setUp() throws Exception {
         try {
             server = new SeleniumServer(SeleniumServer.DEFAULT_PORT);
             System.out.println("Starting the Selenium Server as part of global setup...");
             server.start();
         } catch (Exception e) {
             e.printStackTrace();
             throw e;
         }
     }
     
         public void tearDown() throws Exception {
             try {
                 server.stop();
             } catch (Exception e) {
                 e.printStackTrace();
                 throw e;
             }
         }
         
     }
 }
