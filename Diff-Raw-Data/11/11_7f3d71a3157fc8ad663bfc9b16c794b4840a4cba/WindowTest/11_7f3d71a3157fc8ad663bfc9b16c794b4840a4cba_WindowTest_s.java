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
 package com.gargoylesoftware.htmlunit.javascript.host;
 
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.List;
 import java.util.Map;
 
 import com.gargoylesoftware.base.testing.EventCatcher;
 import com.gargoylesoftware.htmlunit.BrowserVersion;
 import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
 import com.gargoylesoftware.htmlunit.ConfirmHandler;
 import com.gargoylesoftware.htmlunit.KeyValuePair;
 import com.gargoylesoftware.htmlunit.MockWebConnection;
 import com.gargoylesoftware.htmlunit.Page;
 import com.gargoylesoftware.htmlunit.PromptHandler;
 import com.gargoylesoftware.htmlunit.StatusHandler;
 import com.gargoylesoftware.htmlunit.ThreadManager;
 import com.gargoylesoftware.htmlunit.WebClient;
 import com.gargoylesoftware.htmlunit.WebTestCase;
 import com.gargoylesoftware.htmlunit.WebWindow;
 import com.gargoylesoftware.htmlunit.WebWindowEvent;
 import com.gargoylesoftware.htmlunit.WebWindowNotFoundException;
 import com.gargoylesoftware.htmlunit.html.ClickableElement;
 import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
 import com.gargoylesoftware.htmlunit.html.HtmlButton;
 import com.gargoylesoftware.htmlunit.html.HtmlDivision;
 import com.gargoylesoftware.htmlunit.html.HtmlInlineFrame;
 import com.gargoylesoftware.htmlunit.html.HtmlPage;
 import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
 import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
 
 /**
  * Tests for {@link Window}.
  *
  * @version $Revision$
  * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
  * @author <a href="mailto:chen_jun@users.sourceforge.net">Chen Jun</a>
  * @author David K. Taylor
  * @author Darrell DeBoer
  * @author Marc Guillemot
  * @author Dierk Koenig
  * @author Chris Erskine
  * @author David D. Kilzer
  * @author Ahmed Ashour
  * @author Daniel Gredler
  */
 public class WindowTest extends WebTestCase {
 
     /**
      * Creates an instance.
      * @param name The name of the test.
      */
     public WindowTest(final String name) {
         super(name);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testSetLocation() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
 
         final String firstContent
             = "<html><head><title>First</title></head><body>\n"
             + "<form name='form1'>\n"
             + "    <a id='link' onClick='location=\"" + URL_SECOND + "\"; return false;'>Click me</a>\n"
             + "</form>\n"
             + "</body></html>";
         final String secondContent
             = "<html><head><title>Second</title></head><body></body></html>";
 
         webConnection.setResponse(URL_FIRST, firstContent);
         webConnection.setResponse(URL_SECOND, secondContent);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         final HtmlAnchor anchor = (HtmlAnchor) firstPage.getHtmlElementById("link");
         final HtmlPage secondPage = (HtmlPage) anchor.click();
         assertNotNull("secondPage", secondPage);
         assertEquals("Second", secondPage.getTitleText());
         assertSame(webClient.getCurrentWindow(), secondPage.getEnclosingWindow());
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testOpenWindow() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
 
         final List<String> collectedAlerts = new ArrayList<String>();
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
 
         final String firstContent
             = "<html><head><title>First</title></head><body>\n"
             + "<form name='form1'>\n"
             + "    <a id='link' onClick='open(\"" + URL_SECOND + "\", \"MyNewWindow\").focus(); "
             + "return false;'>Click me</a>\n"
             + "</form>\n"
             + "</body></html>";
         final String secondContent
             = "<html><head><title>Second</title></head><body>\n"
             + "<script>alert(self.name)</script>\n"
             + "</body></html>";
 
         final EventCatcher eventCatcher = new EventCatcher();
         eventCatcher.listenTo(webClient);
 
         webConnection.setResponse(URL_FIRST, firstContent);
         webConnection.setResponse(URL_SECOND, secondContent);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         final HtmlAnchor anchor = (HtmlAnchor) firstPage.getHtmlElementById("link");
         final HtmlPage secondPage = (HtmlPage) anchor.click();
         assertNotSame(firstPage, secondPage);
 
         // Expecting contentChanged, opened, contentChanged
         assertEquals(3, eventCatcher.getEventCount());
 
         final WebWindow firstWebWindow = (WebWindow) ((WebWindowEvent) eventCatcher.getEventAt(0)).getSource();
         final WebWindow secondWebWindow = (WebWindow) ((WebWindowEvent) eventCatcher.getEventAt(2)).getSource();
         assertSame(webClient.getCurrentWindow(), secondWebWindow);
         assertEquals("MyNewWindow", secondWebWindow.getName());
 
         assertEquals("First", ((HtmlPage) firstWebWindow.getEnclosedPage()).getTitleText());
         assertEquals("Second", ((HtmlPage) secondWebWindow.getEnclosedPage()).getTitleText());
 
         final WebWindowEvent changedEvent = (WebWindowEvent) eventCatcher.getEventAt(2);
         assertNull(changedEvent.getOldPage());
         assertEquals("Second", ((HtmlPage) changedEvent.getNewPage()).getTitleText());
 
         assertEquals(new String[] {"MyNewWindow"}, collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testOpenWindow_base() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
 
         final List<String> collectedAlerts = new ArrayList<String>();
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
 
         final String firstContent
             = "<html><head><title>First</title><base target='MyNewWindow'></head><body>\n"
             + "<form name='form1'>\n"
             + "    <a id='link' href='" + URL_SECOND + "'>Click me</a>\n"
             + "</form>\n"
             + "</body></html>";
         final String secondContent
             = "<html><head><title>Second</title></head><body>\n"
             + "<script>alert(self.name)</script>\n"
             + "</body></html>";
 
         webConnection.setResponse(URL_FIRST, firstContent);
         webConnection.setResponse(URL_SECOND, secondContent);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
         final WebWindow firstWebWindow = firstPage.getEnclosingWindow();
         assertEquals(firstWebWindow, firstWebWindow.getTopWindow());
 
         final HtmlAnchor anchor = (HtmlAnchor) firstPage.getHtmlElementById("link");
         final HtmlPage secondPage = (HtmlPage) anchor.click();
         assertEquals("Second", secondPage.getTitleText());
         assertNotSame(firstPage, secondPage);
 
         final WebWindow secondWebWindow = secondPage.getEnclosingWindow();
         assertNotSame(firstWebWindow, secondWebWindow);
         assertEquals("MyNewWindow", secondWebWindow.getName());
         assertEquals(secondWebWindow, secondWebWindow.getTopWindow());
 
         assertEquals(new String[] {"MyNewWindow"}, collectedAlerts);
     }
 
     /**
      * _blank is a magic name.  If we call open(url, '_blank') then a new
      * window must be loaded.
      * @throws Exception If the test fails
      */
     public void testOpenWindow_blank() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
 
         final List<String> collectedAlerts = new ArrayList<String>();
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
 
         final String firstContent
             = "<html><head><title>First</title></head><body>\n"
             + "  <iframe name='secondFrame' id='secondFrame' src='" + URL_SECOND + "' />\n"
             + "</body></html>";
         final String secondContent
             = "<html><head><title>Second</title></head><body>\n"
             + "  <a id='link' "
             + "onClick='open(\"" + URL_THIRD + "\", \"_blank\").focus(); '>\n"
             + "Click me</a>\n"
             + "</body></html>";
         final String thirdContent
             = "<html><head><title>Third</title></head><body>\n"
             + "</body></html>";
 
         webConnection.setResponse(URL_FIRST, firstContent);
         webConnection.setResponse(URL_SECOND, secondContent);
         webConnection.setResponse(URL_THIRD, thirdContent);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
         final WebWindow firstWindow = firstPage.getEnclosingWindow();
 
         final HtmlInlineFrame secondFrame = (HtmlInlineFrame) firstPage.getHtmlElementById("secondFrame");
         final HtmlPage secondPage = (HtmlPage) secondFrame.getEnclosedPage();
         assertEquals("Second", secondPage.getTitleText());
         try {
             assertEquals(secondFrame.getEnclosedWindow(),
                 webClient.getWebWindowByName("secondFrame"));
             // Expected path
         }
         catch (final WebWindowNotFoundException exception) {
             fail("Expected secondFrame would be found before click.");
         }
         final HtmlAnchor anchor = (HtmlAnchor) secondPage.getHtmlElementById("link");
         final HtmlPage thirdPage = (HtmlPage) anchor.click();
         assertEquals("Third", thirdPage.getTitleText());
         final WebWindow thirdWindow = thirdPage.getEnclosingWindow();
         assertNotSame(firstWindow, thirdWindow);
 
         assertEquals("", thirdWindow.getName());
 
         assertEquals(thirdWindow, thirdWindow.getTopWindow());
         try {
             assertEquals(secondFrame.getEnclosedWindow(),
                 webClient.getWebWindowByName("secondFrame"));
             // Expected path
         }
         catch (final WebWindowNotFoundException exception) {
             fail("Expected secondFrame would be found after click.");
         }
 
         assertEquals(
             Collections.EMPTY_LIST,
             collectedAlerts);
     }
 
     /**
      * _self is a magic name.  If we call open(url, '_self') then the current window must be
      * reloaded.
      * @throws Exception If the test fails.
      */
     public void testOpenWindow_self() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
 
         final String firstContent
             = "<html><head><title>First</title></head><body>\n"
             + "<form name='form1'>\n"
             + "    <a id='link' onClick='open(\"" + URL_SECOND + "\", \"_self\"); "
             + "return false;'>Click me</a>\n"
             + "</form>\n"
             + "</body></html>";
         final String secondContent
             = "<html><head><title>Second</title></head><body></body></html>";
 
         final EventCatcher eventCatcher = new EventCatcher();
 
         webConnection.setResponse(URL_FIRST, firstContent);
         webConnection.setResponse(URL_SECOND, secondContent);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         eventCatcher.listenTo(webClient);
 
         final WebWindow firstWebWindow = firstPage.getEnclosingWindow();
 
         final HtmlAnchor anchor = (HtmlAnchor) firstPage.getHtmlElementById("link");
         final HtmlPage secondPage = (HtmlPage) anchor.click();
         assertEquals("First", firstPage.getTitleText());
         assertEquals("Second", secondPage.getTitleText());
 
         assertEquals(1, eventCatcher.getEventCount());
 
         final WebWindow secondWebWindow = (WebWindow) ((WebWindowEvent) eventCatcher.getEventAt(0)).getSource();
         assertSame(webClient.getCurrentWindow(), firstWebWindow);
         assertSame(firstWebWindow, secondWebWindow);
     }
 
     /**
      * _top is a magic name.  If we call open(url, '_top') then the top level
      * window must be reloaded.
      * @throws Exception If the test fails.
      */
     public void testOpenWindow_top() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
 
         final String firstContent
             = "<html><head><title>First</title></head><body>\n"
             + "  <iframe name='secondFrame' id='secondFrame' src='" + URL_SECOND + "' />\n"
             + "</body></html>";
         final String secondContent
             = "<html><head><title>Second</title></head><body>\n"
             + "  <iframe name='thirdFrame' id='thirdFrame' src='" + URL_THIRD + "' />\n"
             + "</body></html>";
         final String thirdContent
             = "<html><head><title>Third</title></head><body>\n"
             + "    <a id='link' onClick='open(\"http://fourth\", \"_top\"); "
             + "return false;'>Click me</a>\n"
             + "</body></html>";
         final String fourthContent
             = "<html><head><title>Fourth</title></head><body></body></html>";
 
         webConnection.setResponse(URL_FIRST, firstContent);
         webConnection.setResponse(URL_SECOND, secondContent);
         webConnection.setResponse(URL_THIRD, thirdContent);
         webConnection.setResponse(new URL("http://fourth"), fourthContent);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         final WebWindow firstWebWindow = firstPage.getEnclosingWindow();
         assertEquals("First", firstPage.getTitleText());
         final HtmlInlineFrame secondFrame = (HtmlInlineFrame) firstPage.getHtmlElementById("secondFrame");
         final HtmlPage secondPage = (HtmlPage) secondFrame.getEnclosedPage();
         assertEquals("Second", secondPage.getTitleText());
         final HtmlInlineFrame thirdFrame = (HtmlInlineFrame) secondPage.getHtmlElementById("thirdFrame");
         final HtmlPage thirdPage = (HtmlPage) thirdFrame.getEnclosedPage();
         assertEquals("Third", thirdPage.getTitleText());
 
         assertSame(webClient.getCurrentWindow(), firstWebWindow);
         assertNotSame(firstWebWindow, secondPage);
 
         final HtmlAnchor anchor = (HtmlAnchor) thirdPage.getHtmlElementById("link");
         final HtmlPage fourthPage = (HtmlPage) anchor.click();
         final WebWindow fourthWebWindow = fourthPage.getEnclosingWindow();
         assertSame(firstWebWindow, fourthWebWindow);
         assertSame(fourthWebWindow, fourthWebWindow.getTopWindow());
         try {
             webClient.getWebWindowByName("secondFrame");
             fail("Did not expect secondFrame to still exist after click.");
         }
         catch (final WebWindowNotFoundException exception) {
             // Expected path
         }
         try {
             webClient.getWebWindowByName("thirdFrame");
             fail("Did not expect thirdFrame to still exist after click.");
         }
         catch (final WebWindowNotFoundException exception) {
             // Expected path
         }
     }
 
     /**
      * _parent is a magic name.  If we call open(url, '_parent') then the
      * parent window must be reloaded.
      * @throws Exception If the test fails.
      */
     public void testOpenWindow_parent() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
 
         final String firstContent
             = "<html><head><title>First</title></head><body>\n"
             + "  <iframe name='secondFrame' id='secondFrame' src='" + URL_SECOND + "' />\n"
             + "</body></html>";
         final String secondContent
             = "<html><head><title>Second</title></head><body>\n"
             + "  <iframe name='thirdFrame' id='thirdFrame' src='" + URL_THIRD + "' />\n"
             + "</body></html>";
         final String thirdContent
             = "<html><head><title>Third</title></head><body>\n"
             + "    <a id='link' onClick='open(\"http://fourth\", \"_parent\"); "
             + "return false;'>Click me</a>\n"
             + "</body></html>";
         final String fourthContent
             = "<html><head><title>Fourth</title></head><body></body></html>";
 
         webConnection.setResponse(URL_FIRST, firstContent);
         webConnection.setResponse(URL_SECOND, secondContent);
         webConnection.setResponse(URL_THIRD, thirdContent);
         webConnection.setResponse(new URL("http://fourth"), fourthContent);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         final WebWindow firstWebWindow = firstPage.getEnclosingWindow();
         assertEquals("First", firstPage.getTitleText());
         final HtmlInlineFrame secondFrame = (HtmlInlineFrame) firstPage.getHtmlElementById("secondFrame");
         final HtmlPage secondPage = (HtmlPage) secondFrame.getEnclosedPage();
         assertEquals("Second", secondPage.getTitleText());
         final HtmlInlineFrame thirdFrame = (HtmlInlineFrame) secondPage.getHtmlElementById("thirdFrame");
         final HtmlPage thirdPage = (HtmlPage) thirdFrame.getEnclosedPage();
         assertEquals("Third", thirdPage.getTitleText());
 
         assertSame(webClient.getCurrentWindow(), firstWebWindow);
         assertNotSame(firstWebWindow, secondFrame);
 
         final HtmlAnchor anchor = (HtmlAnchor) thirdPage.getHtmlElementById("link");
         final HtmlPage fourthPage = (HtmlPage) anchor.click();
         final WebWindow fourthWebWindow = fourthPage.getEnclosingWindow();
         assertSame(secondFrame.getEnclosedWindow(), fourthWebWindow);
         try {
             final WebWindow namedWindow = webClient.getWebWindowByName("secondFrame");
             assertSame(namedWindow.getEnclosedPage(), fourthPage);
             // Expected path
         }
         catch (final WebWindowNotFoundException exception) {
             fail("Expected secondFrame would be found after click.");
         }
         try {
             webClient.getWebWindowByName("thirdFrame");
             fail("Did not expect thirdFrame to still exist after click.");
         }
         catch (final WebWindowNotFoundException exception) {
             // Expected path
         }
     }
 
     /**
      * Regression test for 1592723: window.open('', 'someName') should
      * retrieve existing window named 'someName' rather than opening a new window
      * if such a window exists.
      * @throws Exception if the test fails
      */
     public void testOpenWindow_existingWindow() throws Exception {
         final String content
             = "<html><head><script>\n"
             + "function test()\n"
             + "{\n"
             + "  var w1 = window.open('about:blank', 'foo');\n"
             + "  alert(w1 != null);\n"
             + "  var w2 = window.open('', 'foo');\n"
             + "  alert(w1 == w2);\n"
             + "  var w3 = window.open('', 'myFrame');\n"
             + "  alert(w3 == window.frames.myFrame);\n"
             + "}\n"
             + "</script></head><body onload='test()'>\n"
             + "<iframe name='myFrame' id='myFrame'></iframe>\n"
             + "</body></html>";
 
         final List<String> collectedAlerts = new ArrayList<String>();
         final String[] expectedAlerts = {"true", "true", "true"};
         loadPage(content, collectedAlerts);
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Regression test to reproduce a known bug
      * @throws Exception if the test fails
      */
     public void testOpenWindow_emptyUrl() throws Exception {
         final String content
             = "<html><head><script>\n"
             + "var w = window.open('');\n"
             + "alert(w ? w.document.location : w);\n"
             + "</script></head>\n"
             + "<body></body></html>";
 
         final List<String> collectedAlerts = new ArrayList<String>();
         final String[] expectedAlertsMoz = {"null"};
         loadPage(BrowserVersion.FIREFOX_2, content, collectedAlerts);
         createTestPageForRealBrowserIfNeeded(content, expectedAlertsMoz);
         assertEquals(expectedAlertsMoz, collectedAlerts);
 
         collectedAlerts.clear();
         final String[] expectedAlertsIE = {"about:blank"};
         loadPage(BrowserVersion.INTERNET_EXPLORER_6_0, content, collectedAlerts);
         createTestPageForRealBrowserIfNeeded(content, expectedAlertsIE);
         assertEquals(expectedAlertsIE, collectedAlerts);
     }
 
     /**
      * Verifies that <tt>window.open</tt> behaves correctly when popups are blocked.
      * @throws Exception if an error occurs
      */
     public void testOpenWindow_blocked() throws Exception {
 
         final String html =
             "<html>\n"
             + "<head>\n"
             + "<script>\n"
             + "  var w;\n"
             + "  function test() {\n"
             + "    w = window.open('', 'foo');\n"
             + "  }\n"
             + "</script>\n"
             + "</head>\n"
             + "<body onload='test()'>\n"
             + "<div id='d' onclick='alert(w)'>test</div>\n"
             + "</body></html>";
 
         final List<String> actual = new ArrayList<String>();
         final WebClient client = new WebClient();
         client.setPopupBlockerEnabled(true);
         client.setAlertHandler(new CollectingAlertHandler(actual));
 
         final MockWebConnection webConnection = new MockWebConnection(client);
         webConnection.setDefaultResponse(html);
         client.setWebConnection(webConnection);
 
         final HtmlPage page = (HtmlPage) client.getPage("http://foo");
         ((HtmlDivision) page.getHtmlElementById("d")).click();
         final String[] expected = {"null"};
         assertEquals(expected, actual);
     }
 
     /**
      * Regression test to reproduce a known bug
      * @throws Exception if the test fails
      */
     public void testAlert_NoAlertHandler() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>function doTest(){alert('foo')}</script></head>\n"
             + "<body onload='doTest()'></body></html>";
 
         getLog().warn("Warning for no alert handler expected next");
         final HtmlPage firstPage = loadPage(firstContent);
         assertEquals("First", firstPage.getTitleText());
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testParentAndTop() throws Exception {
         final String firstContent
             = "<html><head><title>First</title></head><body>\n"
             + "  <iframe name='left' src='" + URL_SECOND + "' />\n"
             + "</body></html>";
         final String secondContent
             = "<html><head><title>Second</title></head><body>\n"
             + "  <iframe name='innermost' src='" + URL_THIRD + "' />\n"
             + "</body></html>";
         final String thirdContent
             = "<html><head><title>Third</title><script>\n"
             + "function doAlert() {\n"
             + "    alert(parent != this);\n"
             + "    alert(top != this);\n"
             + "    alert(parent != top);\n"
             + "    alert(parent.parent == top);\n"
             + "    alert(parent.frames[0] == this);\n"
             + "    alert(top.frames[0] == parent);\n"
             + "}\n"
             + "</script></head>\n"
             + "<body><a id='clickme' onClick='doAlert()'>foo</a></body></html>";
 
         final WebClient webClient = new WebClient();
         final List<String> collectedAlerts = new ArrayList<String>();
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
 
         final MockWebConnection webConnection = new MockWebConnection(webClient);
         webConnection.setResponse(URL_FIRST, firstContent);
         webConnection.setResponse(URL_SECOND, secondContent);
         webConnection.setResponse(URL_THIRD, thirdContent);
 
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         final WebWindow innermostWebWindow = webClient.getWebWindowByName("innermost");
         final HtmlPage innermostPage = (HtmlPage) innermostWebWindow.getEnclosedPage();
         ((HtmlAnchor) innermostPage.getHtmlElementById("clickme")).click();
 
         assertNotSame(innermostWebWindow.getParentWindow(), innermostWebWindow);
         assertNotSame(innermostWebWindow.getTopWindow(), innermostWebWindow);
         assertNotSame(innermostWebWindow.getParentWindow(),
             innermostWebWindow.getTopWindow());
         assertSame(innermostWebWindow.getParentWindow().getParentWindow(),
             innermostWebWindow.getTopWindow());
 
         assertEquals(
             new String[] {"true", "true", "true", "true", "true", "true"},
             collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testConfirm() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
         final List<String> collectedAlerts = new ArrayList<String>();
         final List<String> collectedConfirms = new ArrayList<String>();
 
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
         webClient.setConfirmHandler(new ConfirmHandler() {
             public boolean handleConfirm(final Page page, final String message) {
                 collectedConfirms.add(message);
                 return true;
             }
         });
 
         final String firstContent
             = "<html><head><title>First</title><script>function doTest(){alert(confirm('foo'))}</script>\n"
             + "</head><body onload='doTest()'></body></html>";
 
         webConnection.setResponse(URL_FIRST, firstContent);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         assertEquals(new String[] {"foo"}, collectedConfirms);
         assertEquals(new String[] {"true"}, collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testConfirm_noConfirmHandler() throws Exception {
         final String html
             = "<html><head><title>First</title><script>function doTest(){alert(confirm('foo'))}</script>\n"
             + "</head><body onload='doTest()'></body></html>";
 
         getLog().warn("Warning for no confirm handler expected next");
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(html, collectedAlerts);
 
         assertEquals(new String[] {"true"}, collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testPrompt() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
         final List<String> collectedAlerts = new ArrayList<String>();
         final List<String> collectedPrompts = new ArrayList<String>();
 
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
         webClient.setPromptHandler(new PromptHandler() {
             public String handlePrompt(final Page page, final String message) {
                 collectedPrompts.add(message);
                 return "Flintstone";
             }
         });
 
         final String firstContent
             = "<html><head><title>First</title><script>function doTest(){alert(prompt('foo'))}</script>\n"
             + "</head><body onload='doTest()'></body></html>";
 
         webConnection.setResponse(URL_FIRST, firstContent);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         assertEquals(new String[] {"foo"}, collectedPrompts);
         assertEquals(new String[] {"Flintstone"}, collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testPrompt_noPromptHandler() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
         final List<String> collectedAlerts = new ArrayList<String>();
         final List<String> collectedPrompts = new ArrayList<String>();
 
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
 
         final String firstContent
             = "<html><head><title>First</title><script>function doTest(){alert(prompt('foo'))}</script>\n"
             + "</head><body onload='doTest()'></body></html>";
 
         webConnection.setResponse(URL_FIRST, firstContent);
         webClient.setWebConnection(webConnection);
         getLog().warn("Warning for no prompt handler expected next");
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         assertEquals(Collections.EMPTY_LIST, collectedPrompts);
         assertEquals(new String[] {"null"}, collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testOpener() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
         final List<String> collectedAlerts = new ArrayList<String>();
 
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
 
         final String firstContent
             = "<html><head><title>First</title><script>\n"
             + "function runtest() {\n"
             + "    alert(window.opener)\n"
             + "    alert('one')\n"
             + "    open('" + URL_SECOND + "', 'foo')\n"
             + "}\n"
             + "function callAlert(text) {\n"
             + "    alert(text)\n"
             + "}\n"
             + "</script></head><body onload='runtest()'>\n"
             + "</body></html>";
         final String secondContent
             = "<html><head><title>Second</title><script>\n"
             + "function runtest() {\n"
             + "    opener.callAlert('two')\n"
             + "    document.form1.submit()\n"
             + "}\n"
             + "</script></head><body onload='runtest()'>\n"
             + "<form name='form1' action='" + URL_THIRD + "' method='post'><input type='submit'></form>\n"
             + "</body></html>";
         final String thirdContent
             = "<html><head><title>Third</title><script>\n"
             + "function runtest() {\n"
             + "    opener.callAlert('three')\n"
             + "}\n"
             + "</script></head><body onload='runtest()'>\n"
             + "</body></html>";
 
         webConnection.setResponse(URL_FIRST, firstContent);
         webConnection.setResponse(URL_SECOND, secondContent);
         webConnection.setResponse(URL_THIRD, thirdContent);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         final String[] expectedAlerts = {"null", "one", "two", "three"};
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testSetTimeout() throws Exception {
         final String content
             = "<html><body><script language='JavaScript'>window.setTimeout('alert(\"Yo!\")',1);\n"
             + "</script></body></html>";
 
         final List<String> collectedAlerts = Collections.synchronizedList(new ArrayList<String>());
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertTrue("thread failed to stop in 1 second", page.getEnclosingWindow().getThreadManager().joinAll(1000));
         assertEquals(new String[] {"Yo!"}, collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testSetTimeoutByReference() throws Exception {
         final String content = "<html><body><script language='JavaScript'>\n"
             + "function doTimeout() {alert('Yo!');}\n"
             + "window.setTimeout(doTimeout,1);\n"
             + "</script></body></html>";
 
         final List<String> collectedAlerts = Collections.synchronizedList(new ArrayList<String>());
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertTrue("thread failed to stop in 1 second", page.getEnclosingWindow().getThreadManager().joinAll(1000));
         assertEquals(new String[] {"Yo!"}, collectedAlerts);
     }
 
     /**
      * Just tests that setting and clearing an interval doesn't throw
      * @throws Exception If the test fails
      */
     public void testSetAndClearInterval() throws Exception {
         final String content
             = "<html><body>\n"
             + "<script>\n"
             + "window.setInterval('alert(\"Yo!\")', 500);\n"
             + "function foo() { alert('Yo2'); }\n"
             + "var i = window.setInterval(foo, 500);\n"
             + "window.clearInterval(i);\n"
             + "</script></body></html>";
 
         final List<String> collectedAlerts = Collections.synchronizedList(new ArrayList<String>());
         loadPage(content, collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testSetIntervalFunctionReference() throws Exception {
         final String content = "<html>\n"
             + "<head>\n"
             + "  <title>test</title>\n"
             + "  <script>\n"
             + "    var threadID;\n"
             + "    function test() {\n"
             + "      threadID = setInterval(doAlert, 100);\n"
             + "    }\n"
             + "    var iterationNumber=0;\n"
             + "    function doAlert() {\n"
             + "      alert('blah');\n"
             + "      if (++iterationNumber >= 3) {\n"
             + "        clearInterval(threadID);\n"
             + "      }\n"
             + "    }\n"
             + "  </script>\n"
             + "</head>\n"
             + "<body onload='test()'>\n"
             + "</body>\n"
             + "</html>";
 
         final List<String> collectedAlerts = Collections.synchronizedList(new ArrayList<String>());
         final HtmlPage page = loadPage(content, collectedAlerts);
         final ThreadManager threadManager = page.getEnclosingWindow().getThreadManager();
         threadManager.joinAll(1000);
         assertEquals(0, threadManager.activeCount());
         assertEquals(Collections.nCopies(3, "blah"), collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testClearInterval() throws Exception {
         final String html = "<html><body onload='test()'><script>\n"
             + "  var count;\n"
             + "  var id;\n"
             + "  function test() {\n"
             + "    count = 0;\n"
             + "    id = setInterval(callback, 100);\n"
             + "  };\n"
             + "  function callback() {\n"
             + "    count++;\n"
             + "    clearInterval(id);\n"
             + "    // Give the callback time to show its ugly face.\n"
             + "    // If it fires between now and then, we'll know.\n"
             + "    setTimeout('alert(count)', 500);\n"
             + "  }\n"
             + "</script></body></html>";
         final String[] expected = {"1"};
         final List<String> actual = Collections.synchronizedList(new ArrayList<String>());
         final HtmlPage page = loadPage(html, actual);
         final ThreadManager threadManager = page.getEnclosingWindow().getThreadManager();
         threadManager.joinAll(1000);
         assertEquals(0, threadManager.activeCount());
         assertEquals(expected, actual);
     }
 
     /**
      * Test that a script started by a timer is stopped if the page that started it
      * is not loaded anymore.
      * @throws Exception If the test fails
      */
     public void testSetTimeoutStopped() throws Exception {
         //TODO: This test fails with eclipse (sometimes), but never with ant!!!!
         final String firstContent
             = "<html><head>\n"
             + "<script language='JavaScript'>window.setTimeout('alert(\"Yo!\")', 1000);</script>\n"
             + "</head><body onload='document.location.replace(\"" + URL_SECOND + "\")'></body></html>";
         final String secondContent = "<html><head><title>Second</title></head><body></body></html>";
 
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
         final List<String> collectedAlerts = Collections.synchronizedList(new ArrayList<String>());
 
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
 
         webConnection.setResponse(URL_FIRST, firstContent);
         webConnection.setResponse(URL_SECOND, secondContent);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage page = (HtmlPage) webClient.getPage(URL_FIRST);
         page.getEnclosingWindow().getThreadManager().joinAll(2000);
         assertEquals("Second", page.getTitleText());
         assertEquals("no thread should be running", 0, page.getEnclosingWindow().getThreadManager().activeCount());
         assertEquals(Collections.EMPTY_LIST, collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testClearTimeout() throws Exception {
         final String content =
               "<html>\n"
             + "<head>\n"
             + "  <title>test</title>\n"
             + "  <script>\n"
             + "    function test() {\n"
             + "      var id = setTimeout('doAlert()', 2000);\n"
             + "      clearTimeout(id);\n"
             + "    }\n"
             + "    function doAlert() {\n"
             + "      alert('blah');\n"
             + "    }\n"
             + "  </script>\n"
             + "</head>\n"
             + "<body onload='test()'>\n"
             + "</body>\n"
             + "</html>";
         final List<String> collectedAlerts = Collections.synchronizedList(new ArrayList<String>());
         final HtmlPage page = loadPage(content, collectedAlerts);
         page.getEnclosingWindow().getThreadManager().joinAll(2000);
         assertEquals(Collections.EMPTY_LIST, collectedAlerts);
     }
 
     /**
      * Verifies that calling clearTimeout() on a callback which has already fired
      * does not affect said callback.
      * @throws Exception If the test fails
      */
     public void testClearTimeout_DoesNotStopExecutingCallback() throws Exception {
         final String html = "<html><body onload='test()'><script>\n"
             + "  var id;\n"
             + "  function test() {\n"
             + "    id = setTimeout(callback, 1);\n"
             + "  };\n"
             + "  function callback() {\n"
             + "    alert(id != 0);\n"
             + "    clearTimeout(id);\n"
             + "    // Make sure we weren't stopped.\n"
             + "    alert('completed');\n"
             + "  }\n"
             + "</script><div id='a'></div></body></html>";
         final String[] expected = {"true", "completed"};
         final List<String> actual = Collections.synchronizedList(new ArrayList<String>());
         final HtmlPage page = loadPage(html, actual);
         page.getEnclosingWindow().getThreadManager().joinAll(5000);
         assertEquals(expected, actual);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testAboutURL() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection =
             new MockWebConnection(webClient);
         final String firstContent =
             "<html><body><script language='JavaScript'>\n"
             + "w2 = window.open('about:blank', 'AboutBlank');\n"
             + "w2.document.open();\n"
             + "w2.document.write('<html><head><title>hello</title></head><body></body></html>');\n"
             + "w2.document.close();\n"
             + "</script></body></html>";
         webConnection.setResponse(URL_FIRST, firstContent);
         webClient.setWebConnection(webConnection);
 
         webClient.getPage(URL_FIRST);
         final WebWindow webWindow = webClient.getWebWindowByName("AboutBlank");
         assertNotNull(webWindow);
 
         //  final HtmlPage page = (HtmlPage) webWindow.getEnclosedPage();
         //  assertEquals("<html><head><title>hello</title></head><body></body></html>",page.getDocument().toString());
     }
 
     /**
      *
      * @throws Exception If the test fails
      */
     public void testWindowFrames() throws Exception {
         final String firstContent =
             "<html><body><script language='JavaScript'>\n"
             + "if (typeof top.frames['anyXXXname'] == 'undefined') {\n"
             + "alert('one')};\n"
             + "</script></body></html>";
 
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(firstContent, collectedAlerts);
         assertEquals(1, collectedAlerts.size());
     }
 
     /**
      *
      * @throws Exception If the test fails
      */
     public void testWindowFramesLive() throws Exception {
         final String content =
             "<html>\n"
             + "<script>\n"
             + "var oFrames = window.frames;\n"
             + "alert(oFrames.length);\n"
             + "function test()\n"
             + "{\n"
             + "    alert(oFrames.length);\n"
             + "    alert(window.frames.length);\n"
             + "    alert(oFrames == window.frames);\n"
             + "}\n"
             + "</script>\n"
             + "<frameset rows='50,*' onload='test()'>\n"
             + "<frame src='about:blank'/>\n"
             + "<frame src='about:blank'/>\n"
             + "</frameset>\n"
             + "</html>";
 
         final String[] expectedAlerts = {"0", "2", "2", "true"};
 
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(content, collectedAlerts);
 
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Variables that are defined inside javascript should be accessible through the
      * window object (ie window.myVariable).  Test that this works.
      * @throws Exception If the test fails.
      */
     public void testJavascriptVariableFromWindow() throws Exception {
         final String firstContent =
             "<html><head><title>first</title></head><body><script>\n"
             + "myVariable = 'foo';\n"
             + "alert(window.myVariable);\n"
             + "</script></body></head>";
 
         final List<String> collectedAlerts = new ArrayList<String>();
         final HtmlPage page = loadPage(firstContent, collectedAlerts);
         assertEquals(new String[] {"foo"}, collectedAlerts);
         assertEquals("first", page.getTitleText());
     }
 
     /**
      * Variables that are defined inside javascript should be accessible through the
      * window object (ie window.myVariable).  Test that this works.
      * @throws Exception If the test fails.
      */
     public void testJavascriptVariableFromTopAndParentFrame() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
         final List<String> collectedAlerts = new ArrayList<String>();
 
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
 
         final String firstContent
             = "<html><head><title>First</title></head><body><script>\n"
             + "myVariable = 'first'"
             + "  </script><iframe name='left' src='" + URL_SECOND + "' />\n"
             + "</body></html>";
         webConnection.setResponse(URL_FIRST, firstContent);
 
         final String secondContent
             = "<html><head><title>Second</title></head><body><script>\n"
             + "myVariable = 'second'"
             + "  </script><iframe name='innermost' src='" + URL_THIRD + "' />\n"
             + "</body></html>";
         webConnection.setResponse(URL_SECOND, secondContent);
 
         final String thirdContent
             = "<html><head><title>Third</title><script>\n"
             + "myVariable = 'third';\n"
             + "function doTest() {\n"
             + "alert('parent.myVariable = ' + parent.myVariable);\n"
             + "alert('top.myVariable = ' + top.myVariable);\n"
             + "}\n"
             + "</script></head>\n"
             + "<body onload='doTest()'></body></html>";
 
         webConnection.setResponse(URL_THIRD, thirdContent);
 
         webClient.setWebConnection(webConnection);
 
         final HtmlPage page = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", page.getTitleText());
 
         final String[] expectedAlerts = {
             "parent.myVariable = second",
             "top.myVariable = first",
         };
         assertEquals(expectedAlerts, collectedAlerts);
     }
     
     /**
      * Variables that are defined inside javascript should be accessible through the
      * window object (ie window.myVariable).  Test that this works.
      * @throws Exception If the test fails.
      */
     public void testJavascriptVariableFromNamedFrame() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
         final List<String> collectedAlerts = new ArrayList<String>();
 
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
 
         final String firstContent
             = "<html><head><title>first</title></head>\n"
             + "<frameset cols='20%,80%'>\n"
             + "    <frameset rows='30%,70%'>\n"
             + "        <frame src='" + URL_SECOND + "' name='second'>\n"
             + "        <frame src='" + URL_THIRD + "' name='third'>\n"
             + "    </frameset>\n"
             + "    <frame src='http://fourth' name='fourth'>\n"
             + "</frameset></html>";
         webConnection.setResponse(URL_FIRST, firstContent);
 
         final String secondContent
             = "<html><head><title>second</title></head><body><script>\n"
             + "myVariable = 'second';\n"
             + "</script><p>second</p></body></html>";
         webConnection.setResponse(URL_SECOND, secondContent);
 
         final String thirdContent
             = "<html><head><title>third</title></head><body><script>\n"
             + "myVariable = 'third';\n"
             + "</script><p>third</p></body></html>";
         webConnection.setResponse(URL_THIRD, thirdContent);
 
         final String fourthContent
             = "<html><head><title>fourth</title></head><body onload='doTest()'><script>\n"
             + "myVariable = 'fourth';\n"
             + "function doTest() {\n"
             + "alert('parent.second.myVariable = ' + parent.second.myVariable);\n"
             + "alert('parent.third.myVariable = ' + parent.third.myVariable);\n"
             + "}\n"
             + "</script></body></html>";
         webConnection.setResponse(new URL("http://fourth"), fourthContent);
 
         webClient.setWebConnection(webConnection);
 
         final HtmlPage page = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("first", page.getTitleText());
 
         final String[] expectedAlerts = {
             "parent.second.myVariable = second",
             "parent.third.myVariable = third",
         };
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Variables that have not been defined should return null when accessed.
      * @throws Exception If the test fails.
      */
     public void testJavascriptVariableFromWindow_NotFound() throws Exception {
         final String firstContent =
             "<html><head><title>first</title></head><body><script>\n"
             + "myVariable = 'foo';\n"
             + "alert(window.myOtherVariable == null);\n"
             + "</script></body></head>";
 
         final List<String> collectedAlerts = new ArrayList<String>();
         final HtmlPage page = loadPage(firstContent, collectedAlerts);
         assertEquals(new String[] {"true"}, collectedAlerts);
         assertEquals("first", page.getTitleText());
     }
 
     /**
      *
      * @throws Exception If the test fails.
      */
     public void testGetFrameByName() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection =
             new MockWebConnection(webClient);
         final List<String> collectedAlerts = new ArrayList<String>();
 
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
 
         final String firstContent
             = "<html><head><title>first</title></head>\n"
             + "<frameset cols='20%,80%'>\n"
             + "    <frameset rows='30%,70%'>\n"
             + "        <frame src='" + URL_SECOND + "' name='second'>\n"
             + "        <frame src='" + URL_THIRD + "' name='third'>\n"
             + "    </frameset>\n"
             + "    <frame src='http://fourth' name='fourth'>\n"
             + "</frameset></html>";
         webConnection.setResponse(URL_FIRST, firstContent);
 
         final String secondContent
             = "<html><head><title>second</title></head><body><p>second</p></body></html>";
         webConnection.setResponse(URL_SECOND, secondContent);
 
         final String thirdContent
             = "<html><head><title>third</title></head><body><p>third</p></body></html>";
         webConnection.setResponse(URL_THIRD, thirdContent);
 
         final String fourthContent
             = "<html><head><title>fourth</title></head><body onload='doTest()'><script>\n"
             + "function doTest() {\n"
             + "alert('fourth-second='+parent.second.document.location);\n"
             + "alert('fourth-third='+parent.third.document.location);\n"
             + "}\n"
             + "</script></body></html>";
         webConnection.setResponse(new URL("http://fourth"), fourthContent);
 
         webClient.setWebConnection(webConnection);
 
         final HtmlPage page = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("first", page.getTitleText());
 
         final String[] expectedAlerts = {"fourth-second=" + URL_SECOND, "fourth-third=" + URL_THIRD};
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testSetOpenerLocationHrefRelative() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
 
         final String aContent
             = "<html><head><title>A</title></head><body>\n"
             + "<button id='clickme' onClick='window.open(\"b/b.html\");'>Click me</a>\n"
             + "</body></html>";
         final String bContent
             = "<html><head><title>B</title></head><body>\n"
             + "<button id='clickme' onClick='opener.location.href=\"../c.html\";'>Click me</a>\n"
             + "</body></html>";
         final String cContent
             = "<html><head><title>C</title></head><body></body></html>";
         final String failContent
             = "<html><head><title>FAILURE!!!</title></head><body></body></html>";
 
         webConnection.setResponse(new URL("http://opener/test/a.html"), aContent);
         webConnection.setResponse(new URL("http://opener/test/b/b.html"), bContent);
         webConnection.setResponse(new URL("http://opener/test/c.html"), cContent);
         webConnection.setResponse(new URL("http://opener/c.html"), failContent);
 
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage("http://opener/test/a.html");
         assertEquals("A", firstPage.getTitleText());
 
         final HtmlButton buttonA = (HtmlButton) firstPage.getHtmlElementById("clickme");
         final HtmlPage secondPage = (HtmlPage) buttonA.click();
         assertNotNull("B", secondPage);
         assertEquals("B", secondPage.getTitleText());
 
         final HtmlButton buttonB = (HtmlButton) secondPage.getHtmlElementById("clickme");
         final HtmlPage thirdPage = (HtmlPage) buttonB.click();
         assertNotNull("C", thirdPage);
         assertEquals("C", thirdPage.getTitleText());
     }
 
     /**
      * Test the window.closed property
      * @throws Exception if the test fails.
      */
     public void testClosed() throws Exception {
         final String content = "<html><head>\n"
             + "<script>\n"
             + "function test()\n"
             + "{\n"
             + "  alert(window.closed);\n"
             + "  var newWindow = window.open('about:blank', 'foo');\n"
             + "  alert(newWindow.closed);\n"
             + "  newWindow.close();\n"
             + "  alert(newWindow.closed);\n"
             + "}\n"
             + "</script>\n"
             + "</head>\n"
             + "<body onload='test()'>\n"
             + "</body></html>";
 
         final String[] expectedAlerts = {"false", "false", "true"};
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
 
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(content, collectedAlerts);
 
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Test closing using javascript
      * @throws Exception if the test fails.
      */
     public void testClose() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
 
         final List<String> collectedAlerts = new ArrayList<String>();
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
 
         final String firstContent
             = "<html><head><title>First</title></head><body>\n"
              + "<a href='" + URL_SECOND + "' id='link' target='_blank'>Link</a>\n"
              + "</body></html>";
         final String secondContent
             = "<html><head><title>Second</title></head><body>\n"
              + "<h1>Second</h1><form>\n"
              + "<input type='submit' name='action' value='Close' id='button' "
              + "onclick='window.close(); return false;'>\n"
              + "</form></body></html>";
 
         webConnection.setResponse(URL_FIRST, firstContent);
         webConnection.setResponse(URL_SECOND, secondContent);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
         assertEquals(1, webClient.getWebWindows().size());
         final WebWindow firstWindow = firstPage.getEnclosingWindow();
 
         final HtmlPage secondPage = (HtmlPage) ((HtmlAnchor) firstPage.getHtmlElementById("link")).click();
         assertEquals("Second", secondPage.getTitleText());
         assertEquals(2, webClient.getWebWindows().size());
         final WebWindow secondWindow = secondPage.getEnclosingWindow();
 
         assertNotSame(firstWindow, secondWindow);
 
         final EventCatcher eventCatcher = new EventCatcher();
         eventCatcher.listenTo(webClient);
         ((HtmlSubmitInput) secondPage.getHtmlElementById("button")).click();
 
         final List<WebWindowEvent> expectedEvents = Arrays.asList(new WebWindowEvent[]{
             new WebWindowEvent(secondWindow, WebWindowEvent.CLOSE, secondPage, null)
         });
         assertEquals(expectedEvents, eventCatcher.getEvents());
 
         assertEquals(1, webClient.getWebWindows().size());
         assertEquals(firstWindow, webClient.getCurrentWindow());
 
         assertEquals(Collections.EMPTY_LIST, collectedAlerts);
     }
 
     /**
      * Test that length of frames collection is retrieved
      * @throws Exception if the test fails
      */
     public void testFramesLengthZero() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "alert(window.frames.length)\n"
             + "</script></head><body>\n"
             + "</body></html>";
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(content, collectedAlerts);
 
         final String[] expectedAlerts = {"0"};
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Test that length of frames collection is retrieved when there
      * are frames.
      * @throws Exception If the test fails
      */
     public void testFramesLengthAndFrameAccess() throws Exception {
         final String content =
             "<html>\n"
             + "<script>\n"
             + "function test()\n"
             + "{\n"
             + "    alert(window.frames.length);\n"
             + "    alert(window.frames[0].name);\n"
             + "    alert(window.frames.frame2.name);\n"
             + "}\n"
             + "</script>\n"
             + "<frameset rows='50,*' onload='test()'>\n"
             + "<frame name='frame1' src='about:blank'/>\n"
             + "<frame name='frame2' src='about:blank'/>\n"
             + "</frameset>\n"
             + "</html>";
 
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(content, collectedAlerts);
 
         final String[] expectedAlerts = {"2", "frame1", "frame2"};
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Test that Window.moveTo method gets correctly called and handled
      * by the scripting engine.
      * @throws Exception if the test fails
      */
     public void testMoveTo() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "window.moveTo(10, 20)\n"
             + "</script></head><body>\n"
             + "</body></html>";
         loadPage(content);
     }
 
     /**
      * Test that Window.moveBy method gets correctly called and handled
      * by the scripting engine.
      * @throws Exception if the test fails
      */
     public void testMoveBy() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "window.moveBy(10, 20)\n"
             + "</script></head><body>\n"
             + "</body></html>";
         loadPage(content);
     }
 
     /**
      * Tests that the Window.resizeTo method gets correctly called and
      * handled by the scripting engine.
      * @throws Exception if the test fails
      */
     public void testResizeTo() throws Exception {
         final String content = "<html><head><title>foo</title><script>\n"
             + "window.resizeTo(10, 20);\n"
             + "window.resizeTo(-10, 20);\n"
             + "</script></head><body></body></html>";
         loadPage(content);
     }
 
     /**
      * Tests that the Window.resizeBy method gets correctly called and
      * handled by the scripting engine.
      * @throws Exception if the test fails
      */
     public void testResizeBy() throws Exception {
         final String content = "<html><head><title>foo</title><script>\n"
             + "window.resizeBy(10, 20);\n"
             + "window.resizeBy(-10, 20);\n"
             + "</script></head><body></body></html>";
         loadPage(content);
     }
 
     /**
      * Test that Window.scroll method gets correctly called and handled
      * by the scripting engine.
      * @throws Exception if the test fails
      */
     public void testScroll() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "window.scroll(10, 20);\n"
             + "</script></head><body>\n"
             + "</body></html>";
         loadPage(content);
     }
 
     /**
      * Test that Window.scrollBy method gets correctly called and handled
      * by the scripting engine.
      * @throws Exception if the test fails
      */
     public void testScrollBy() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "window.scrollBy(10, 20);\n"
             + "</script></head><body>\n"
             + "</body></html>";
         loadPage(content);
     }
 
     /**
      * Test that Window.scrollByLines method gets correctly called and handled
      * by the scripting engine.
      * @throws Exception if the test fails
      */
     public void testScrollByLines() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "window.scrollByLines(2);\n"
             + "</script></head><body>\n"
             + "</body></html>";
         loadPage(content);
     }
 
     /**
      * Test that Window.scrollByPages method gets correctly called and handled
      * by the scripting engine.
      * @throws Exception if the test fails
      */
     public void testScrollByPages() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "window.scrollByPages(2);\n"
             + "</script></head><body>\n"
             + "</body></html>";
         loadPage(content);
     }
 
     /**
      * Test that Window.scrollTo method gets correctly called and handled
      * by the scripting engine.
      * @throws Exception if the test fails
      */
     public void testScrollTo() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "window.scrollTo(10, 20);\n"
             + "</script></head><body>\n"
             + "</body></html>";
         loadPage(content);
     }
 
      /**
      * All elements should be accessible via the window object by their name, both in qualified
      * format (<tt>window.elementName</tt>) and unqualified format (<tt>elementName</tt>), if we
      * are emulating Microsoft Internet Explorer. Both of these expressions are therefore equivalent
      * to <tt>document.getElementsByName(elementName)</tt> in IE, and should return a collection of
      * elements if there is more than one with the specified name.
      * @throws Exception If the test fails.
      */
     public void testElementByNameFromWindow() throws Exception {
         final String content = "<html>\n"
             + "<head><title>test</title>\n"
             + "<script>\n"
             + "  function test() {\n"
             + "    alert(window.form1.name);\n"
             + "    alert(form2.name);\n"
             + "    alert(window.input1.length);\n"
             + "    alert(input2[1].value);\n"
             + "  }\n"
             + "</script>\n"
             + "</head>\n"
             + "<body onload='test()'>\n"
             + "<form name='form1'></form>\n"
             + "<form name='form2'></form>\n"
             + "<input type='text' name='input1' value='1'/>\n"
             + "<input type='text' name='input1' value='2'/>\n"
             + "<input type='text' name='input2' value='3'/>\n"
             + "<input type='text' name='input2' value='4'/>\n"
             + "</body>\n"
             + "</html>";
         final List<String> collectedAlerts = new ArrayList<String>();
         final String[] expectedAlerts = {"form1", "form2", "2", "4"};
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         loadPage(content, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * All elements should be accessible via the window object by their id, if we
      * are emulating Microsoft Internet Explorer.
      * @throws Exception If the test fails.
      */
     public void testElementByIdFromWindow() throws Exception {
         final String content = "<html>\n"
             + "<head><title>test</title>\n"
             + "<script>\n"
             + "  function test() {\n"
             + "    alert(window.form1Id.name);\n"
             + "    alert(form2Id.name);\n"
             + "    alert(window.input1Id.value);\n"
             + "    alert(myDiv.tagName);\n"
             + "  }\n"
             + "</script>\n"
             + "</head>\n"
             + "<body onload='test()'>\n"
             + "<div id='myDiv'>\n"
             + "<form name='form1' id='form1Id'></form>\n"
             + "</div>\n"
             + "<form name='form2' id='form2Id'></form>\n"
             + "<input type='text' name='input1' id='input1Id' value='1'/>\n"
             + "</form>\n"
             + "</body>\n"
             + "</html>";
         final List<String> collectedAlerts = new ArrayList<String>();
         final String[] expectedAlerts = {"form1", "form2", "1", "DIV"};
         loadPage(content, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Test that Window.execScript method gets called correctly.
      * @throws Exception if the test fails
      */
     public void testExecScript() throws Exception {
         final String content = "<html>\n"
             + "<head><title>test</title>\n"
             + "<script>\n"
             + "  function test()\n"
             + "  {\n"
             + "    window.execScript('alert(\"JavaScript\")', 'JavaScript');\n"
             + "    window.execScript('alert(\"JScript\")',    'JScript');\n"
             + "    window.execScript('alert(\"VBScript\")',   'VBScript');\n"
             + "    try {\n"
             + "      window.execScript('alert(\"BadLanguage\")', 'BadLanguage');\n"
             + "    }\n"
             + "    catch (e) {\n"
             + "      alert(e.message.substr(0, 20)); // msg now contains info on error location\n"
             + "    }\n"
             + "  }\n"
             + "</script>\n"
             + "</head>\n"
             + "<body onload='test()'>\n"
             + "  <div id='div1'>blah</div>\n"
             + "</body>\n"
             + "</html>";
         final String[] expectedAlerts = {"JavaScript", "JScript", "Invalid class string"};
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(content, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
    /**
      * @throws Exception If the test fails.
      */
     public void testOnLoadFunction() throws Exception {
         final String content = "<html>\n"
             + "<head><title>test</title>\n"
             + "<script>\n"
             + "  function test()\n"
             + "  {\n"
             + "    alert('test');\n"
             + "  }\n"
             + "</script>\n"
             + "</head>\n"
             + "<body onload='test()'>\n"
             + "<script>\n"
             + "  var oldOnLoad = window.onload;\n"
             + "  window.onload = test2;\n"
             + "  function test2()\n"
             + "  {\n"
             + "    alert('test2');\n"
             + "    oldOnLoad();\n"
             + "  }\n"
             + "</script>\n"
             + "</body>\n"
             + "</html>";
         final String[] expectedAlerts = {"test2", "test"};
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(content, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Verifies that you can set window.onload to something else than a function.
      * See bug 1708532 & 1201561.
      * @throws Exception If an error occurs.
      */
     public void testOnloadNotAFunction() throws Exception {
         final String html = "<html><body><script>\n"
             + "window.onload = new function() {alert('a')};\n"
             + "window.onload = undefined;\n"
             + "alert(window.onload);\n"
             + "</script></body></html>";
 
         final String[] expectedAlerts = {"a", "undefined"};
         createTestPageForRealBrowserIfNeeded(html, expectedAlerts);
 
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(html, collectedAlerts);
 
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails.
      */
     public void testAddOnLoadEventListener() throws Exception {
         final String content = "<html>\n"
             + "<head><title>test</title>\n"
             + "<script>\n"
             + "  function test1() { alert('test1'); }\n"
             + "  function test2() { alert('test2'); }\n"
             + "  function test3() { alert('test3'); }\n"
             + "alert(window.addEventListener == null);\n"
             + "alert(window.attachEvent == null);\n"
             + "alert(window.removeEventListener == null);\n"
             + "alert(window.detachEvent == null);\n"
             + "window.addEventListener('load', test1, true);\n"
             + "window.addEventListener('load', test1, true);\n"
             + "window.addEventListener('load', test2, true);\n"
             + "window.addEventListener('load', test3, true);\n"
             + "window.removeEventListener('load', test3, true);\n"
             + "</script></head>\n"
             + "<body onload='alert(\"onload\")'></body></html>";
         final String[] expectedAlerts = {"false", "true", "false", "true", "test1", "test2", "onload"};
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(BrowserVersion.FIREFOX_2, content, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails.
      */
     public void testAttachOnLoadEvent() throws Exception {
         final String content = "<html>\n"
             + "<head><title>test</title>\n"
             + "<script>\n"
             + "  function test1(_e) { alert('test1, param null: ' + (_e == null)); }\n"
             + "  function test2() { alert('test2'); }\n"
             + "  function test3() { alert('test3'); }\n"
             + "alert(window.addEventListener == null);\n"
             + "alert(window.attachEvent == null);\n"
             + "alert(window.removeEventListener == null);\n"
             + "alert(window.detachEvent == null);\n"
             + "window.attachEvent('onload', test1);\n"
             + "window.attachEvent('onload', test1);\n"
             + "window.attachEvent('onload', test2);\n"
             + "window.attachEvent('onload', test3);\n"
             + "window.detachEvent('onload', test3);\n"
             + "</script></head>\n"
             + "<body onload='alert(\"onload\")'></body></html>";
         final String[] expectedAlerts = {"true", "false", "true", "false",
             "onload", "test1, param null: false", "test2"};
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(BrowserVersion.INTERNET_EXPLORER_6_0, content, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Regression test for 1596926
      * @throws Exception If the test fails.
      */
     public void testDetachEventInAttachEvent() throws Exception {
         final String content = "<html>\n"
             + "<head><title>test</title>\n"
             + "<script>\n"
             + "function test()\n"
             + "{\n"
             + "  window.detachEvent('onload', test);\n"
             + "  alert('detached');\n"
             + "}\n"
             + "window.attachEvent('onload', test);\n"
             + "</script></head>\n"
             + "<body></body></html>";
         final String[] expectedAlerts = {"detached"};
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(BrowserVersion.INTERNET_EXPLORER_6_0, content, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testStatus() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
 
         final String firstContent
             = "<html><head><title>First</title><script>\n"
             + "function doTest() {\n"
             + "    alert(window.status);\n"
             + "    window.status = 'newStatus';\n"
             + "    alert(window.status);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>\n"
             + "</body></html>";
 
         final URL url = URL_FIRST;
         webConnection.setResponse(url, firstContent);
         webClient.setWebConnection(webConnection);
 
         final List<String> collectedAlerts = new ArrayList<String>();
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
 
         final List<String> collectedStatus = new ArrayList<String>();
         webClient.setStatusHandler(new StatusHandler() {
             public void statusMessageChanged(final Page page, final String message) {
                 collectedStatus.add(message);
             }
         });
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         final String[] expectedAlerts = {"", "newStatus"};
         assertEquals("alerts", expectedAlerts, collectedAlerts);
 
         final String[] expectedStatus = {"newStatus"};
         assertEquals("status", expectedStatus, collectedStatus);
     }
 
     /**
      * Test <code>window.name</code>.
      *
      * @throws Exception If the test fails.
      */
     public void testWindowName() throws Exception {
         final String windowName = "main";
         final String content = "<html>\n"
             + "<head><title>window.name test</title></head>\n"
             + "<body>\n"
             + "<script>\n"
             + "alert('window.name before: ' + window.name);\n"
             + "window.name = '" + windowName + "';\n"
             + "alert('window.name after: ' + window.name);\n"
             + "</script>\n"
             + "</body>\n"
             + "</html>";
         final String[] expectedAlerts = {"window.name before: ", "window.name after: " + windowName};
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(content, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Test Mozilla viewport properties
      *
      * @throws Exception If the test fails.
      */
     public void testMozillaViewport() throws Exception {
         final String content = "<html>\n"
             + "<head></head>\n"
             + "<body>\n"
             + "<script>\n"
             + "alert(typeof window.innerWidth);\n"
             + "alert(typeof window.innerHeight);\n"
             + "alert(typeof window.outerWidth);\n"
             + "alert(typeof window.outerHeight);\n"
             + "</script>\n"
             + "</body>\n"
             + "</html>";
         final List<String> expectedAlerts = Collections.nCopies(4, "number");
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(BrowserVersion.FIREFOX_2, content, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
     
     /**
      * @throws Exception If the test fails.
      */
     public void testPrint() throws Exception {
         final String content = "<html>\n"
             + "<head></head>\n"
             + "<body>\n"
             + "<script>\n"
             + "window.print();\n"
             + "</script>\n"
             + "</body>\n"
             + "</html>";
 
         loadPage(content);
     }
     
     /**
      * Open a window with only an image for content, then try to set focus to it.
      *
      * @throws Exception If the test fails.
      */
     public void testOpenWindow_image() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
 
         final String firstContent = "<html><head><title>First</title></head><body>\n"
             + "<form name='form1'>\n"
             + "    <a id='link' onClick='open(\"" + URL_SECOND + "\", \"_blank\").focus(); return false;'"
             + "return false;'>Click me</a>\n"
             + "</form>\n"
             + "</body></html>";
         final String secondContent = new String(new char[]{
             'G', 'I', 'F', '8', '9', 'a', 0x01, 0x00,
             0x01, 0x00, 0x80, 0x00, 0x00, 0xfe, 0xd4, 0xaf,
             0x00, 0x00, 0x00, 0x21, 0xf9, 0x04, 0x00, 0x00,
             0x00, 0x00, 0x00, 0x2c, 0x00, 0x00, 0x00, 0x00,
             0x01, 0x00, 0x01, 0x00, 0x00, 0x02, 0x02, 0x44,
             0x01, 0x00, 0x3b});
 
         final EventCatcher eventCatcher = new EventCatcher();
 
         final List< ? extends KeyValuePair> emptyList = Collections.emptyList();
         webConnection.setResponse(URL_FIRST, firstContent, 200, "OK", "text/html", emptyList);
         webConnection.setResponse(URL_SECOND, secondContent, 200, "OK", "image/gif", emptyList);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         eventCatcher.listenTo(webClient);
 
         final WebWindow firstWebWindow = firstPage.getEnclosingWindow();
 
         final HtmlAnchor anchor = (HtmlAnchor) firstPage.getHtmlElementById("link");
         final Page secondPage = anchor.click();
         assertEquals("First", firstPage.getTitleText());
         assertEquals("image/gif", secondPage.getWebResponse().getContentType());
 
         assertEquals(2, eventCatcher.getEventCount());
 
         final WebWindow secondWebWindow = (WebWindow) eventCatcher.getEventAt(0).getSource();
 
         assertSame(webClient.getCurrentWindow(), secondWebWindow);
         assertNotSame(firstWebWindow, secondWebWindow);
     }
 
     /**
      * Open a window with only text for content, then try to set focus to it.
      *
      * @throws Exception If the test fails.
      */
     public void testOpenWindow_text() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
 
         final String firstContent = "<html><head><title>First</title></head><body>\n"
             + "<form name='form1'>\n"
             + "    <a id='link' onClick='open(\"" + URL_SECOND + "\", \"_blank\").focus(); return false;'"
             + "return false;'>Click me</a>\n"
             + "</form>\n"
             + "</body></html>";
         final String secondContent = "Hello World";
 
         final EventCatcher eventCatcher = new EventCatcher();
 
         final List< ? extends KeyValuePair> emptyList = Collections.emptyList();
         webConnection.setResponse(URL_FIRST, firstContent, 200, "OK", "text/html", emptyList);
         webConnection.setResponse(URL_SECOND, secondContent, 200, "OK", "text/plain", emptyList);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         eventCatcher.listenTo(webClient);
 
         final WebWindow firstWebWindow = firstPage.getEnclosingWindow();
 
         final HtmlAnchor anchor = (HtmlAnchor) firstPage.getHtmlElementById("link");
         final Page secondPage = anchor.click();
         assertEquals("First", firstPage.getTitleText());
         assertEquals("text/plain", secondPage.getWebResponse().getContentType());
 
         assertEquals(2, eventCatcher.getEventCount());
 
         final WebWindow secondWebWindow = (WebWindow) eventCatcher.getEventAt(0).getSource();
 
         assertSame(webClient.getCurrentWindow(), secondWebWindow);
         assertNotSame(firstWebWindow, secondWebWindow);
     }
 
     /**
      * Open a window with only XML for content, then try to set focus to it.
      *
      * @throws Exception If the test fails.
      */
     public void testOpenWindow_xml() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
 
         final String firstContent = "<html><head><title>First</title></head><body>\n"
             + "<form name='form1'>\n"
             + "    <a id='link' onClick='open(\"" + URL_SECOND + "\", \"_blank\").focus(); return false;'"
             + "return false;'>Click me</a>\n"
             + "</form>\n"
             + "</body></html>";
         final String secondContent = "<junk></junk>\n";
 
         final EventCatcher eventCatcher = new EventCatcher();
 
         final List< ? extends KeyValuePair> emptyList = Collections.emptyList();
         webConnection.setResponse(URL_FIRST, firstContent, 200, "OK", "text/html", emptyList);
         webConnection.setResponse(URL_SECOND, secondContent, 200, "OK", "text/xml", emptyList);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         eventCatcher.listenTo(webClient);
 
         final WebWindow firstWebWindow = firstPage.getEnclosingWindow();
 
         final HtmlAnchor anchor = (HtmlAnchor) firstPage.getHtmlElementById("link");
         final Page secondPage = anchor.click();
         assertEquals("First", firstPage.getTitleText());
         assertEquals("text/xml", secondPage.getWebResponse().getContentType());
 
         assertEquals(2, eventCatcher.getEventCount());
 
         final WebWindow secondWebWindow = (WebWindow) eventCatcher.getEventAt(0).getSource();
 
         assertSame(webClient.getCurrentWindow(), secondWebWindow);
         assertNotSame(firstWebWindow, secondWebWindow);
     }
 
     /**
      * Open a window with only javascript for content, then try to set focus to it.
      *
      * @throws Exception If the test fails.
      */
     public void testOpenWindow_javascript() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
 
         final String firstContent = "<html><head><title>First</title></head><body>\n"
             + "<form name='form1'>\n"
             + "    <a id='link' onClick='open(\"" + URL_SECOND + "\", \"_blank\").focus(); return false;'"
             + "return false;'>Click me</a>\n"
             + "</form>\n"
             + "</body></html>";
         final String secondContent = "var x=1;\n";
 
         final EventCatcher eventCatcher = new EventCatcher();
 
         final List< ? extends KeyValuePair> emptyList = Collections.emptyList();
         webConnection.setResponse(URL_FIRST, firstContent, 200, "OK", "text/html", emptyList);
         webConnection.setResponse(URL_SECOND, secondContent, 200, "OK", "text/javascript", emptyList);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         eventCatcher.listenTo(webClient);
 
         final WebWindow firstWebWindow = firstPage.getEnclosingWindow();
 
         final HtmlAnchor anchor = (HtmlAnchor) firstPage.getHtmlElementById("link");
         final Page secondPage = anchor.click();
         assertEquals("First", firstPage.getTitleText());
         assertEquals("text/javascript", secondPage.getWebResponse().getContentType());
 
         assertEquals(2, eventCatcher.getEventCount());
 
         final WebWindow secondWebWindow = (WebWindow) eventCatcher.getEventAt(0).getSource();
 
         assertSame(webClient.getCurrentWindow(), secondWebWindow);
         assertNotSame(firstWebWindow, secondWebWindow);
     }
 
     /**
      * Open a window with only text for content, then try to set focus to it.
      *
      * @throws Exception If the test fails.
      */
     public void testOpenWindow_html() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(webClient);
 
         final String firstContent = "<html><head><title>First</title></head><body>\n"
             + "<form name='form1'>\n"
             + "    <a id='link' onClick='open(\"" + URL_SECOND + "\", \"_blank\").focus(); return false;'"
             + "return false;'>Click me</a>\n"
             + "</form>\n"
             + "</body></html>";
         final String secondContent = new String("<html><head><title>Second</title></head><body>\n"
             + "<p>Hello World</p>\n"
             + "</body></html>");
 
         final EventCatcher eventCatcher = new EventCatcher();
 
         webConnection.setResponse(URL_FIRST, firstContent);
         webConnection.setResponse(URL_SECOND, secondContent);
         webClient.setWebConnection(webConnection);
 
         final HtmlPage firstPage = (HtmlPage) webClient.getPage(URL_FIRST);
         assertEquals("First", firstPage.getTitleText());
 
         eventCatcher.listenTo(webClient);
 
         final WebWindow firstWebWindow = firstPage.getEnclosingWindow();
 
         final HtmlAnchor anchor = (HtmlAnchor) firstPage.getHtmlElementById("link");
         final Page secondPage = anchor.click();
         assertEquals("First", firstPage.getTitleText());
         assertEquals("text/html", secondPage.getWebResponse().getContentType());
 
         assertEquals(2, eventCatcher.getEventCount());
 
         final WebWindow secondWebWindow = (WebWindow) eventCatcher.getEventAt(0).getSource();
 
         assertSame(webClient.getCurrentWindow(), secondWebWindow);
         assertNotSame(firstWebWindow, secondWebWindow);
     }
 
     /**
      * Test the 'Referer' HTTP header by window.open
      * @throws Exception if the test fails.
      */
     public void testOpenWindow_refererHeader() throws Exception {
         final String headerIE = null;
         testOpenWindow_refererHeader(BrowserVersion.INTERNET_EXPLORER_6_0, headerIE);
         testOpenWindow_refererHeader(BrowserVersion.INTERNET_EXPLORER_7_0, headerIE);
         final String headerFF = URL_FIRST.toString();
         testOpenWindow_refererHeader(BrowserVersion.FIREFOX_2, headerFF);
     }
 
     /**
      * @throws Exception If the test fails
      */
     private void testOpenWindow_refererHeader(final BrowserVersion browser,
             final String expectedRefererHeader) throws Exception {
         final String firstContent = "<html><head></head>\n"
             + "<body>\n"
             + "<button id='clickme' onClick='window.open(\"" + URL_SECOND + "\");'>Click me</a>\n"
             + "</body></html>";
 
         final String secondContent
             = "<html><head><title>Second</title></head><body></body></html>";
 
         final WebClient client = new WebClient(browser);
         final MockWebConnection conn = new MockWebConnection(client);
         client.setWebConnection(conn);
 
         conn.setResponse(URL_FIRST, firstContent);
         conn.setResponse(URL_SECOND, secondContent);
         
         final HtmlPage firstPage = (HtmlPage) client.getPage(URL_FIRST);
         final HtmlButton buttonA = (HtmlButton) firstPage.getHtmlElementById("clickme");
         
         buttonA.click();
         final Map<String, String> lastAdditionalHeaders = conn.getLastAdditionalHeaders();
         assertEquals(expectedRefererHeader, lastAdditionalHeaders.get("Referer"));
     }
 
     /**
      * Tests that nested setTimeouts that are deeper than Thread.MAX_PRIORITY
      * do not cause an exception.
      * @throws Exception If the test fails
      */
     public void testNestedSetTimeoutAboveMaxPriority() throws Exception {
         final int max = Thread.MAX_PRIORITY + 1;
         final String content = "<html><body><script language='JavaScript'>\n"
             + "var depth = 0;\n"
             + "var maxdepth = " + max + ";\n"
             + "function addAnother() {\n"
             + "  if (depth < maxdepth) {\n"
             + "    window.alert('ping');\n"
             + "    depth++;\n"
             + "    window.setTimeout('addAnother();', 1);\n"
             + "  }\n"
             + "}\n"
             + "addAnother();\n"
             + "</script></body></html>";
 
         final List<String> collectedAlerts = Collections.synchronizedList(new ArrayList<String>());
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertTrue("threads did not stop in time", page.getEnclosingWindow()
                 .getThreadManager().joinAll((max + 1) * 1000));
         assertEquals(Collections.nCopies(max, "ping"), collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testEvalScopeOtherWindow() throws Exception {
         final String content = "<html><body>\n"
             + "<iframe src='iframe.html'></iframe>\n"
             + "</body></html>";
         final String iframe = "<html><body>\n"
             + "<script>\n"
             + "window.parent.eval('var foo = 1');\n"
             + "alert(window.parent.foo)\n"
             + "</script>\n"
             + "</body></html>";
 
         final WebClient webClient = new WebClient();
         final List<String> collectedAlerts = new ArrayList<String>();
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
 
         final MockWebConnection webConnection = new MockWebConnection(webClient);
         webConnection.setResponse(URL_FIRST, content);
         webConnection.setDefaultResponse(iframe);
         webClient.setWebConnection(webConnection);
 
         webClient.getPage(URL_FIRST);
         final String[] expectedAlerts = {"1"};
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Regression test for [ 1608555 ] JavaScript: window.eval does evaluate local scope
      * https://sourceforge.net/tracker/index.php?func=detail&aid=1608555&group_id=47038&atid=448266
      * @throws Exception If the test fails
      */
     public void testEvalScopeLocal() throws Exception {
         final String content = "<html><body><form id='formtest'><input id='element' value='elementValue'/></form>\n"
             + "<script> \n"
             + "var docPatate = 'patate';\n"
             + "function test() {\n"
             + " var f = document.forms['formtest']; \n"
             + " alert(eval(\"document.forms['formtest'].element.value\")); \n"
             + " alert(f.element.value); \n"
             + " alert(eval('f.element.value')); \n"
             + "}\n"
             + "test(); \n"
             + "</script>\n"
             + "</body></html>";
 
         final String[] expectedAlerts = {"elementValue", "elementValue", "elementValue"};
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(content, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Verifies that eval() works correctly when triggered from an event handler. Event handlers are
      * executed in a child scope of the global window scope, so variables set from inside eval()
      * should go to this child scope, and not to the window scope.
      * @throws Exception if the test fails
      */
     public void testEvalScopeEvent() throws Exception {
         final String html = "<html><body onload='test()'><script>\n"
             + "   function test() {\n"
             + "      var s = 'string';\n"
             + "      var f = 'initial';\n"
             + "      eval('f = function(){alert(s);}');\n"
             + "      invoke(f);\n"
             + "   };\n"
             + "   function invoke(fn) {\n"
             + "      fn();\n"
             + "   }\n"
             + "</script></body></html>";
         final String[] expected = {"string"};
         final List<String> actual = new ArrayList<String>();
         loadPage(html, actual);
         assertEquals(expected, actual);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testFunctionEquality() throws Exception {
         final String content = "<html><body>\n"
             + "<script>\n"
             + "alert(window.focus == window.focus)\n"
             + "</script>\n"
             + "</body></html>";
 
         final List<String> collectedAlerts = new ArrayList<String>();
         final String[] expectedAlerts = {"true"};
         loadPage(content, collectedAlerts);
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
     
     /**
      * Test for 1225021.
      * @throws Exception If the test fails
      */
     public void testCaptureEvents() throws Exception {
         final String content = "<html><head><title>foo</title>\n"
             + "<script>\n"
             + "function t()\n"
             + "{\n"
             + "    alert('captured');\n"
             + "}\n"
             + "window.captureEvents(Event.CLICK);\n"
             + "window.onclick = t;\n"
             + "</script></head><body>\n"
             + "<div id='theDiv' onclick='alert(123)'>foo</div>\n"
             + "</body></html>";
         
         final List<String> collectedAlerts = new ArrayList<String>();
         final HtmlPage page = loadPage(BrowserVersion.FIREFOX_2, content, collectedAlerts);
         ((ClickableElement) page.getHtmlElementById("theDiv")).click();
 
         final String[] expectedAlerts = {"123", "captured"};
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testGetComputedStyle() throws Exception {
         testGetComputedStyle(BrowserVersion.FIREFOX_2);
         try {
             testGetComputedStyle(BrowserVersion.INTERNET_EXPLORER_6_0);
             fail("'getComputedStyle' is not defined for IE");
         }
         catch (final Exception e) {
             //expected
         }
     }
 
     private void testGetComputedStyle(final BrowserVersion browserVersion) throws Exception {
         final String content = "<html>\n"
             + "<head>\n"
             + "<script>\n"
             + "  function test() {\n"
             + "    var e = document.getElementById('myDiv');\n"
             + "    alert(window.getComputedStyle(e,null).color);\n"
             + "  }\n"
             + "</script>\n"
             + "</head>\n"
             + "<body onload='test()'>\n"
             + "<div id='myDiv'></div>\n"
             + "</body></html>";
         
         final String[] expectedAlerts = {""};
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(browserVersion, content, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * @throws Exception if an error occurs
      */
     public void testGetComputedStyle_WithComputedColor() throws Exception {
         final String html =
               "<html>\n"
             + "  <head>\n"
             + "    <style>div.x { color: red; }</style>\n"
             + "  </head>\n"
             + "  <body onload='alert(window.getComputedStyle(document.getElementById(\"d\"), \"\").color)'>\n"
             + "    <div id='d' class='x'>foo bar</div>\n"
             + "  </body>\n"
             + "</html>";
         final String[] expectedAlerts = {"red"};
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(BrowserVersion.FIREFOX_2, html, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Verifies that the onload handler is executed with "this" referring to the window.
      *
      * @throws Exception If an error occurs.
      */
     public void testOnLoadContext() throws Exception {
         final String html = "<html><body><script>\n"
             + "var x = function() { alert(this==window) };\n"
             + "window.onload = x;\n"
             + "</script></body></html>";
 
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(html, collectedAlerts);
 
         final String[] expectedAlerts = {"true"};
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Added test for [ 1727599 ] Bad context in evaluation of the javascript.
      * @throws Exception If the test fails
      */
     public void testEval() throws Exception {
         final String content = "<html><body>\n"
             + "<input type='button' id='myButton' value='Click Me' onclick='test(this)'>\n"
             + "<script>\n"
             + "function test(f) {\n"
             + "   alert(eval('f.tagName'));\n"
             + "}\n"
             + "</script>\n"
             + "</body></html>";
 
         final String[] expectedAlerts = {"INPUT"};
         
         final List<String> collectedAlerts = new ArrayList<String>();
         final HtmlPage page = loadPage(content, collectedAlerts);
         ((ClickableElement) page.getHtmlElementById("myButton")).click();
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testUndefinedProperty() throws Exception {
         final String content = "<html><head><title>foo</title><script>\n"
             + "  function test() {\n"
             + "    alert(window['something']);\n"
             + "    alert(typeof window['something']);\n"
             + "    alert(typeof window['something']=='undefined');\n"
             + "  }\n"
             + "</script></head><body onload='test()'>\n"
             + "</body></html>";
         final String[] expectedAlerts = {"undefined", "undefined", "true"};
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(content, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Regression test for
      * https://sourceforge.net/tracker/index.php?func=detail&aid=1153708&group_id=47038&atid=448266
      * @throws Exception If the test fails
      */
     public void testOverwriteFunctions() throws Exception {
         if (notYetImplemented()) {
             return;
         }
 
         final String content = "<html><head><script language='JavaScript'>\n"
             + "function alert()\n"
             + "{\n"
             + "  scroll = 'xxx';\n"
             + "  document.write(scroll);\n"
             + "}\n"
             + "function navigator()\n"
             + "{\n"
             + "  alert('xxx');\n"
             + "}\n"
             + "navigator();\n"
             + "</script></head><body></body></html>";
 
         loadPage(content);
     }
 
     /**
      * @throws Exception If an error occurs.
      */
     public void testFrames() throws Exception {
         final String framesetContent =
             "<html><head><title>First</title></head>\n"
             + "<frameset id='fs' rows='20%,*'>\n"
             + "    <frame name='top' src='" + URL_SECOND + "' />\n"
             + "    <frame name='bottom' src='about:blank' />\n"
             + "</frameset>\n"
             + "</html>";
 
         final String frameContent =
             "<html><head><title>TopFrame</title>\n"
             + "<script>\n"
             + "function doTest() {\n"
             + "    var bottomFrame = window.top.frames['bottom'];\n"
             + "    bottomFrame.location = 'about:blank';\n"
             + "}</script>\n"
             + "</head>\n"
             + "<body onload='doTest()'></body></html>";
 
         final List<String> collectedAlerts = new ArrayList<String>();
         final WebClient webClient = new WebClient();
         webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
 
         final MockWebConnection webConnection = new MockWebConnection(webClient);
         webConnection.setResponse(URL_FIRST, framesetContent);
         webConnection.setResponse(URL_SECOND, frameContent);
         webClient.setWebConnection(webConnection);
 
         webClient.getPage(URL_FIRST);
     }
 
     /**
      * Verifies that <tt>this.arguments</tt> works from within a method invocation, in a
      * function defined on the Function prototype object. This usage is required by the
      * Ajax.NET Professional JavaScript library.
      *
      * @throws Exception if an error occurs
      */
     public void testFunctionPrototypeArguments() throws Exception {
         if (notYetImplemented()) {
             return;
         }
         final String html =
               "<html>\n"
             + "<body onload='test()'>\n"
             + "<script>\n"
             + "  function test() {\n"
             + "    \n"
             + "    Function.prototype.doAlerts = function() {\n"
             + "      alert(this==o.f);\n"
             + "      alert(this.arguments ? this.arguments.length : 'null');\n"
             + "    }\n"
             + "    \n"
             + "    var o = function() {};\n"
             + "    o.f = function(x, y, z) { this.f.doAlerts(); }\n"
             + "    o.f('a', 'b');\n"
             + "  }\n"
             + "</script>\n"
             + "</body>\n"
             + "</html>";
         final List<String> actual = new ArrayList<String>();
         loadPage(html, actual);
         final String[] expected = {"true", "2"};
         assertEquals(expected, actual);
     }
 
     /**
      * Test window properties that match Prototypes.
      *
      * @throws Exception if the test fails
      */
     public void testWindowProperties() throws Exception {
         testWindowProperties(BrowserVersion.INTERNET_EXPLORER_7_0, new String[] {"undefined", "undefined"});
         testWindowProperties(BrowserVersion.FIREFOX_2, new String[] {"[Node]", "[Element]"});
     }
 
     private void testWindowProperties(final BrowserVersion browserVersion, final String[] expectedAlerts)
         throws Exception {
         final String html = "<html><head><title>foo</title><script>\n"
             + "  function test() {\n"
             + "    alert(window.Node);\n"
             + "    alert(window.Element);\n"
             + "  }\n"
             + "</script></head><body onload='test()'>\n"
             + "<form name='myForm'></form>\n"
             + "</body></html>";
 
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(browserVersion, html, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testEval_localVariable() throws Exception {
         final String html = "<html><head><title>foo</title><script>\n"
             + "  function test() {\n"
             + "    var f = document.getElementById('testForm1');\n"
             + "    alert(f.text1.value);\n"
             + "    eval('f.text_' + 1).value = 'changed';\n"
             + "    alert(f.text1.value);\n"
             + "  }\n"
             + "</script></head><body onload='test()'>\n"
             + "  <form id='testForm1'>\n"
             + "    <input id='text1' type='text' name='text_1' value='original'>\n"
             + "  </form>\n"
             + "</body></html>";
 
         final String[] expectedAlerts = {"original", "changed"};
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(html, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * @throws Exception If the test fails
      */
     public void testGetComputedStyle2() throws Exception {
         if (notYetImplemented()) {
             return;
         }
         final String content = "<html>\n"
             + "<head>\n"
             + "<script>\n"
             + "  function test() {\n"
             + "    var e = document.getElementById('myDiv');\n"
             + "    alert(window.getComputedStyle(e,null).cssFloat);\n"
             + "  }\n"
             + "</script>\n"
             + "</head>\n"
             + "<body onload='test()'>\n"
             + "  <div id='myDiv'></div>\n"
             + "</body></html>";
         
         final String[] expectedAlerts = {"none"};
         final List<String> collectedAlerts = new ArrayList<String>();
         loadPage(BrowserVersion.FIREFOX_2, content, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Compares all style and getComputedStle.
      * @throws Exception If the test fails
      */
     public void testGetComputedStyle3() throws Exception {
         if (notYetImplemented()) {
             return;
         }
         final String html
             = "<html><head><title>First</title><script>\n"
             + "function test() {\n"
             + "  var e = document.getElementById('myDiv');\n"
             + "  var str = '';\n"
             + "  for (var i in e.style) {\n"
             + "    var s1 = eval('e.style.' + i);\n"
             + "    var s2 = eval('window.getComputedStyle(e,null).' + i);\n"
            + "    if(typeof s1 != 'function')\n"
             + "      str += i + '=' + s1 + ':' + s2 + ',';\n"
             + "  }\n"
             + "  document.getElementById('myTextarea').value = str;\n"
             + "}\n"
             + "</script></head>\n"
             + "<body onload='test()'>\n"
             + "  <div id='myDiv'><br>\n"
             + "  <textarea id='myTextarea' cols='120' rows='20'></textarea>\n"
             + "</body></html>";
 
        final String expectedText = "length=0:112,cssText=:,parentRule=null:null,azimuth=:,background=:,"
             + "backgroundAttachment=:scroll,backgroundColor=:transparent,backgroundImage=:none,"
             + "backgroundPosition=:,backgroundRepeat=:repeat,border=:,borderCollapse=:separate,borderColor=:,"
             + "borderSpacing=:0px 0px,borderStyle=:,borderTop=:,borderRight=:,borderBottom=:,borderLeft=:,"
             + "borderTopColor=:rgb(0, 0, 0),borderRightColor=:rgb(0, 0, 0),borderBottomColor=:rgb(0, 0, 0),"
             + "borderLeftColor=:rgb(0, 0, 0),borderTopStyle=:none,borderRightStyle=:none,borderBottomStyle=:none,"
             + "borderLeftStyle=:none,borderTopWidth=:0px,borderRightWidth=:0px,borderBottomWidth=:0px,"
             + "borderLeftWidth=:0px,borderWidth=:,bottom=:auto,captionSide=:top,clear=:none,clip=:auto,"
             + "color=:rgb(0, 0, 0),content=:,counterIncrement=:none,counterReset=:none,cue=:,cueAfter=:,cueBefore=:,"
             + "cursor=:auto,direction=:ltr,display=:block,elevation=:,emptyCells=:-moz-show-background,cssFloat=:none,"
             + "font=:,fontFamily=:serif,fontSize=:16px,fontSizeAdjust=:none,fontStretch=:,fontStyle=:normal,"
             + "fontVariant=:normal,fontWeight=:400,height=:363px,left=:auto,letterSpacing=:normal,lineHeight=:normal,"
             + "listStyle=:,listStyleImage=:none,listStylePosition=:outside,listStyleType=:disc,margin=:,"
             + "marginTop=:0px,marginRight=:0px,marginBottom=:0px,marginLeft=:0px,markerOffset=:none,marks=:,"
             + "maxHeight=:none,maxWidth=:none,minHeight=:0px,minWidth=:0px,orphans=:,outline=:,"
             + "outlineColor=:rgb(0, 0, 0),outlineStyle=:none,outlineWidth=:0px,overflow=:visible,padding=:,"
             + "paddingTop=:0px,paddingRight=:0px,paddingBottom=:0px,paddingLeft=:0px,page=:,pageBreakAfter=:,"
             + "pageBreakBefore=:,pageBreakInside=:,pause=:,pauseAfter=:,pauseBefore=:,pitch=:,pitchRange=:,"
             + "position=:static,quotes=:,richness=:,right=:auto,size=:,speak=:,speakHeader=:,speakNumeral=:,"
             + "speakPunctuation=:,speechRate=:,stress=:,tableLayout=:auto,textAlign=:start,textDecoration=:none,"
             + "textIndent=:0px,textShadow=:,textTransform=:none,top=:auto,unicodeBidi=:normal,verticalAlign=:baseline,"
             + "visibility=:visible,voiceFamily=:,volume=:,whiteSpace=:normal,widows=:,width=:1256px,"
             + "wordSpacing=:normal,zIndex=:auto,MozAppearance=:none,MozBackgroundClip=:border,"
             + "MozBackgroundInlinePolicy=:continuous,MozBackgroundOrigin=:padding,MozBinding=:none,"
             + "MozBorderBottomColors=:none,MozBorderLeftColors=:none,MozBorderRightColors=:none,"
             + "MozBorderTopColors=:none,MozBorderRadius=:,MozBorderRadiusTopleft=:0px,MozBorderRadiusTopright=:0px,"
             + "MozBorderRadiusBottomleft=:0px,MozBorderRadiusBottomright=:0px,MozBoxAlign=:stretch,"
             + "MozBoxDirection=:normal,MozBoxFlex=:0,MozBoxOrient=:horizontal,MozBoxOrdinalGroup=:1,MozBoxPack=:start,"
             + "MozBoxSizing=:content-box,MozColumnCount=:auto,MozColumnWidth=:auto,MozColumnGap=:0px,"
             + "MozFloatEdge=:content-box,MozForceBrokenImageIcon=:,MozImageRegion=:auto,MozMarginEnd=:,"
             + "MozMarginStart=:,MozOpacity=:1,MozOutline=:,MozOutlineColor=:rgb(0, 0, 0),MozOutlineRadius=:,"
             + "MozOutlineRadiusTopleft=:0px,MozOutlineRadiusTopright=:0px,MozOutlineRadiusBottomleft=:0px,"
             + "MozOutlineRadiusBottomright=:0px,MozOutlineStyle=:none,MozOutlineWidth=:0px,MozOutlineOffset=:0px,"
             + "MozPaddingEnd=:,MozPaddingStart=:,MozUserFocus=:none,MozUserInput=:auto,MozUserModify=:read-only,"
             + "MozUserSelect=:auto,opacity=:1,outlineOffset=:0px,overflowX=:visible,overflowY=:visible,";
         final HtmlPage page = loadPage(BrowserVersion.FIREFOX_2, html, null);
         final List<String> expectedValues = testGetComputedStyle3(expectedText);
         final List<String> collectedValues =
             testGetComputedStyle3(((HtmlTextArea) page.getHtmlElementById("myTextarea")).getText());
         assertEquals(expectedValues, collectedValues);
     }
 
     private List<String> testGetComputedStyle3(final String string) throws Exception {
         final List<String> values = new ArrayList<String>();
 
         //string.split(",") will not work because we have values of 'rgb(0, 0, 0)'
         int i = string.indexOf('=');
         i = string.indexOf('=', i + 1);
        for (int p0 = 0; i != -1;) {
             final int p1 = string.lastIndexOf(',', i);
             values.add(string.substring(p0, p1));
             i = string.indexOf('=', i + 1);
             p0 = p1 + 1;
         }
         
         Collections.sort(values, new Comparator<String>() {
         
             public int compare(String o1, String o2) {
                 o1 = o1.substring(0, o1.indexOf('='));
                 o2 = o2.substring(0, o2.indexOf('='));
                 return o1.compareToIgnoreCase(o2);
             }
         
         });
         return values;
     }
 }
