 /*
  * Copyright (c) 2002-2006 Gargoyle Software Inc. All rights reserved.
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
 import java.util.List;
 import java.util.Date;
 
 import org.apache.commons.httpclient.Cookie;
 import org.apache.commons.httpclient.HttpState;
 import org.apache.commons.httpclient.util.DateUtil;
 
 import com.gargoylesoftware.htmlunit.BrowserVersion;
 import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
 import com.gargoylesoftware.htmlunit.ElementNotFoundException;
 import com.gargoylesoftware.htmlunit.KeyValuePair;
 import com.gargoylesoftware.htmlunit.MockWebConnection;
 import com.gargoylesoftware.htmlunit.ScriptException;
 import com.gargoylesoftware.htmlunit.WebClient;
 import com.gargoylesoftware.htmlunit.WebTestCase;
 import com.gargoylesoftware.htmlunit.html.DomNode;
 import com.gargoylesoftware.htmlunit.html.FrameWindow;
 import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
 import com.gargoylesoftware.htmlunit.html.HtmlElement;
 import com.gargoylesoftware.htmlunit.html.HtmlInlineFrame;
 import com.gargoylesoftware.htmlunit.html.HtmlPage;
 
 /**
  * Tests for {@link Document}.
  *
  * @version  $Revision$
  * @author  <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
  * @author  David K. Taylor
  * @author  Barnaby Court
  * @author  Chris Erskine
  * @author  Marc Guillemot
  * @author  Michael Ottati
  * @author <a href="mailto:george@murnock.com">George Murnock</a>
  */
 public class DocumentTest extends WebTestCase {
     /**
      * Create an instance
      * @param name The name of the test
      */
     public DocumentTest( final String name ) {
         super(name);
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testFormsAccessor_TwoForms() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    alert(document.forms.length)\n"
             + "    for( var i=0; i< document.forms.length; i++) {\n"
             + "        alert( document.forms[i].name )\n"
             + "    }\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>\n"
             + "<p>hello world</p>"
             + "<form name='form1'>"
             + "    <input type='text' name='textfield1' value='foo' />"
             + "</form>"
             + "<form name='form2'>"
             + "    <input type='text' name='textfield2' value='foo' />"
             + "</form>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertEquals("foo", page.getTitleText());
 
         final String[] expectedAlerts = { "2", "form1", "form2" };
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Previously, forms with no names were not being returned by document.forms.
      * @throws Exception if the test fails
      */
     public void testFormsAccessor_FormWithNoName() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    alert(document.forms.length)\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>\n"
             + "<p>hello world</p>"
             + "<form>"
             + "    <input type='text' name='textfield1' value='foo' />"
             + "</form>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertEquals("foo", page.getTitleText());
 
         final List expectedAlerts = Collections.singletonList("1");
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testFormsAccessor_NoForms() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    alert(document.forms.length)\n"
             + "    for( var i=0; i< document.forms.length; i++) {\n"
             + "        alert( document.forms[i].name )\n"
             + "    }\n"
             +"}\n"
             + "</script></head><body onload='doTest()'>\n"
             + "<p>hello world</p>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertEquals("foo", page.getTitleText());
 
         final String[] expectedAlerts = { "0" };
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testDocumentWrite_AssignedToVar() throws Exception {
         // IE accept use of detached function 
         final String[] expectedAlertsIE = {};
         testDocumentWrite_AssignedToVar(BrowserVersion.INTERNET_EXPLORER_6_0, expectedAlertsIE);
 
         // but FF doesn't 
         final String[] expectedAlertsFF = {"exception occured"};
         testDocumentWrite_AssignedToVar(BrowserVersion.MOZILLA_1_0, expectedAlertsFF);
     }
 
     /**
      * @throws Exception if the test fails
      */
     private void testDocumentWrite_AssignedToVar(final BrowserVersion browserVersion, final String[] expectedAlerts)
         throws Exception {
 
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "function doTheFoo() {\n"
             + "var d = document.writeln\n"
             + "try {\n"
             + "d('foo')\n"
             + "} catch (e) { alert('exception occured') }\n"
             + "document.writeln('foo')\n"
             + "}"
             + "</script></head><body onload='doTheFoo()'>\n"
             + "<p>hello world</p>"
             + "</body></html>";
 
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(browserVersion, content, collectedAlerts);
         assertEquals("foo", page.getTitleText());
 
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testFormArray() throws Exception {
         final WebClient client = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection( client );
 
         final String firstContent
             = "<html><head><SCRIPT lang='JavaScript'>"
             + "    function doSubmit(formName){"
             + "        var form = document.forms[formName];" // This line used to blow up
             + "        form.submit()"
             + "}"
             + "</SCRIPT></head><body><form name='formName' method='POST' "
             + "action='http://second'>"
             + "<a href='.' id='testJavascript' name='testJavascript' "
             + "onclick=\" doSubmit('formName');return false;\">"
             + "Test Link </a><input type='submit' value='Login' "
             + "name='loginButton'></form>"
             + "</body></html> ";
         final String secondContent
             = "<html><head><title>second</title></head><body>\n"
             + "<p>hello world</p>"
             + "</body></html>";
 
         webConnection.setResponse(URL_FIRST, firstContent);
         webConnection.setResponse(URL_SECOND, secondContent);
         client.setWebConnection( webConnection );
 
         final HtmlPage page = (HtmlPage)client.getPage(URL_FIRST);
         assertEquals( "", page.getTitleText() );
 
         final HtmlAnchor testAnchor = page.getAnchorByName("testJavascript");
         final HtmlPage secondPage = (HtmlPage)testAnchor.click();
         assertEquals( "second", secondPage.getTitleText() );
     }
 
 
     /**
      * Test that forms is a live collection
      * @throws Exception if the test fails
      */
     public void testFormsLive() throws Exception {
         final String content =
             "<html>"
             + "<head>"
             + "<script>"
             + "var oCol = document.forms;"
             + "alert(oCol.length);"
             + "function test()"
             + "{"
             + "    alert(oCol.length);"
             + "    alert(document.forms.length);"
             + "    alert(document.forms == oCol);"
             + "}"
             + "</script>"
             + "</head>"
             + "<body onload='test()'>"
             + "<form id='myForm' action='foo.html'>"
             + "</form>"
             + "</body>"
             + "</html>";
 
 
         final List collectedAlerts = new ArrayList();
         final String[] expectedAlerts = { "0", "1", "1", "true" };
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         loadPage(content, collectedAlerts);
 
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Tests for document.anchors
      * @throws Exception if the test fails
      */
     public void testAnchors() throws Exception {
         testAnchors(BrowserVersion.MOZILLA_1_0,
                 Arrays.asList( new String[]{ "0", "1", "1", "true", "name: end"} ));
         testAnchors(BrowserVersion.INTERNET_EXPLORER_6_0,
             Arrays.asList( new String[]{ "0", "3", "3", "true", "id: firstLink"} ));
     }
 
     /**
      * Tests for document.anchors
      * @throws Exception if the test fails
      */
     void testAnchors(final BrowserVersion browserVersion, final List expectedAlerts) throws Exception {
         final String content =
             "<html>"
             + "<head>"
             + "<script>"
             + "var oCol = document.anchors;"
             + "alert(oCol.length);"
             + "function test()"
             + "{"
             + "    alert(oCol.length);"
             + "    alert(document.anchors.length);"
             + "    alert(document.anchors == oCol);"
             + "    if (document.anchors[0].name)"
             + "     alert('name: ' + document.anchors[0].name);"
             + "    else"
             + "     alert('id: ' + document.anchors[0].id);"
             + "}"
             + "</script>"
             + "</head>"
             + "<body onload='test()'>"
             + "<a href='foo.html' id='firstLink'>foo</a>"
             + "<a href='foo2.html'>foo2</a>"
             + "<a name='end'/>"
             + "<a href=''>null2</a>"
             + "<a id='endId'/>"
             + "</body>"
             + "</html>";
 
         final List collectedAlerts = new ArrayList();
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         loadPage(browserVersion, content, collectedAlerts);
 
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Tests for document.links
      * @throws Exception if the test fails
      */
     public void testLinks() throws Exception {
         final String content =
             "<html>"
             + "<head>"
             + "<script>"
             + "var oCol = document.links;"
             + "alert(oCol.length);"
             + "function test()"
             + "{"
             + "    alert(oCol.length);"
             + "    alert(document.links.length);"
             + "    alert(document.links == oCol);"
             + "    alert(document.links[0].id);"
             + "}"
             + "</script>"
             + "</head>"
             + "<body onload='test()'>"
             + "<a href='foo.html' id='firstLink'>foo</a>"
             + "<a href='foo2.html'>foo2</a>"
             + "<a name='end'/>"
             + "<a href=''>null2</a>"
             + "</body>"
             + "</html>";
 
         final List collectedAlerts = new ArrayList();
         final String[] expectedAlerts = { "0", "3", "3", "true", "firstLink" };
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         loadPage(content, collectedAlerts);
 
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Ensures that <tt>document.createElement()</tt> works correctly.
      * @throws Exception if the test fails.
      */
     public void testDocumentCreateElement() throws Exception {
 
         final String htmlContent
             = "<html>\n"
             + "  <head>\n"
             + "    <title>First</title>\n"
             + "    <script>\n"
             + "      function doTest() {\n"
             + "        // Create a DIV element.\n"
             + "        var div1 = document.createElement('div');\n"
             + "        div1.id = 'div1';\n"
             + "        document.body.appendChild(div1);\n"
             + "        alert(div1.tagName);\n"
             + "        alert(div1.nodeType);\n"
             + "        alert(div1.nodeValue);\n"
             + "        alert(div1.nodeName);\n"
             + "        // Create an INPUT element.\n"
             + "        var input = document.createElement('input');\n"
             + "        input.id = 'text1id';\n"
             + "        input.name = 'text1name';\n"
             + "        input.value = 'text1value';\n"
             + "        var form = document.getElementById('form1');\n"
             + "        form.appendChild(input);\n"
             + "        alert(document.getElementById('button1id').value);\n"
             + "        alert(document.getElementById('text1id').value);\n"
             + "        // The default type of an INPUT element is 'text'.\n"
             + "        alert(document.getElementById('text1id').type);\n"
             + "      }\n"
             + "    </script>\n"
             + "  </head>\n"
             + "  <body onload='doTest()'>\n"
             + "    <form name='form1' id='form1'>\n"
             + "      <input type='button' id='button1id' name='button1name' value='button1value'/>\n"
             + "      This is form1.\n"
             + "    </form>\n"
             + "  </body>\n"
             + "</html>\n";
 
         final String[] expectedAlerts = { "DIV", "1", "null", "DIV", "button1value", "text1value", "text" };
         createTestPageForRealBrowserIfNeeded(htmlContent, expectedAlerts);
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(htmlContent, collectedAlerts);
         assertEquals( "First", page.getTitleText() );
 
         assertEquals( expectedAlerts, collectedAlerts );
 
         final HtmlElement div1 = page.getHtmlElementById("div1");
         assertEquals("div", div1.getTagName());
         assertEquals(1, div1.getNodeType());
         assertEquals(null, div1.getNodeValue());
         assertEquals("div", div1.getNodeName());
 
     }
 
     /**
      * Regression test for createTextNode
      * @throws Exception if the test fails
      */
     public void testDocumentCreateTextNode() throws Exception {
         final String htmlContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var text1=document.createTextNode('Some Text');\n"
             + "    var body1=document.getElementById('body');\n"
             + "    body1.appendChild(text1);\n"
             + "    alert(text1.data);\n"
             + "    alert(text1.length);\n"
             + "    alert(text1.nodeType);\n"
             + "    alert(text1.nodeValue);\n"
             + "    alert(text1.nodeName);\n"
             + "}\n"
             + "</script></head><body onload='doTest()' id='body'>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(htmlContent, collectedAlerts);
         assertEquals( "First", page.getTitleText() );
 
         final DomNode div1 =
             page.getHtmlElementById("body").getLastChild();
         assertEquals(3, div1.getNodeType());
         assertEquals("Some Text", div1.getNodeValue());
         assertEquals("#text", div1.getNodeName());
 
         final String[] expectedAlerts = { "Some Text", "9", "3", "Some Text", "#text" };
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for RFE 741930
      * @throws Exception if the test fails
      */
     public void testAppendChild() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    var form = document.forms['form1'];\n"
             + "    var div = document.createElement( 'DIV' );\n"
             + "    form.appendChild( div );\n"
             + "    var elements = document.getElementsByTagName('DIV');\n"
             + "    alert( elements.length )\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>\n"
             + "<p>hello world</p>"
             + "<form name='form1'>"
             + "</form>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertEquals("foo", page.getTitleText());
 
         final String[] expectedAlerts = { "1" };
 
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for appendChild of a text node
      * @throws Exception if the test fails
      */
     public void testAppendChild_textNode() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    var form = document.forms['form1'];\n"
             + "    var child = document.createTextNode( 'Some Text' );\n"
             + "    form.appendChild( child );\n"
             + "    alert( form.lastChild.data )\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>\n"
             + "<p>hello world</p>"
             + "<form name='form1'>"
             + "</form>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertEquals("foo", page.getTitleText());
 
         final String[] expectedAlerts = { "Some Text" };
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for cloneNode
      * @throws Exception if the test fails
      */
     public void testCloneNode() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    var form = document.forms['form1'];\n"
             + "    var cloneShallow = form.cloneNode(false);\n"
             + "    alert( cloneShallow!=null )\n"
             + "    alert( cloneShallow.firstChild==null )\n"
             + "    var cloneDeep = form.cloneNode(true);\n"
             + "    alert( cloneDeep!=null )\n"
             + "    alert( cloneDeep.firstChild!=null )\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>\n"
             + "<form name='form1'>"
             + "<p>hello world</p>"
             + "</form>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertEquals("foo", page.getTitleText());
 
         final String[] expectedAlerts = { "true", "true", "true", "true" };
 
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for insertBefore
      * @throws Exception if the test fails
      */
     public void testInsertBefore() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    var form = document.forms['form1'];\n"
             + "    var oldChild = document.getElementById('oldChild');\n"
             + "    var div = document.createElement( 'DIV' );\n"
             + "    form.insertBefore( div, oldChild );\n"
             + "    alert( form.firstChild==div )\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>\n"
             + "<form name='form1'><div id='oldChild'/></form>"
             + "</body></html>";
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertEquals("foo", page.getTitleText());
 
         final String[] expectedAlerts = { "true" };
 
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testGetElementById() throws Exception {
         final String content
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    alert(top.document.getElementById('input1').value);\n"
             + "    alert(document.getElementById(''));\n"
             + "    alert(document.getElementById('non existing'));\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<form id='form1'>"
             + "<input id='input1' name='foo' type='text' value='bar' />"
             + "</form>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertEquals( "First", page.getTitleText() );
 
         final String[] expectedAlerts = {"bar", "null", "null"};
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testGetElementById_resetId() throws Exception {
         final String content
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var input1=top.document.getElementById('input1');\n"
             + "    input1.id='newId';\n"
             + "    alert(top.document.getElementById('newId').value);\n"
             + "    alert(top.document.getElementById('input1'));\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<form id='form1'>"
             + "<input id='input1' name='foo' type='text' value='bar' />"
             + "</form>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertEquals( "First", page.getTitleText() );
 
         final String[] expectedAlerts = { "bar", "null" };
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testGetElementById_setNewId() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var div1=top.document.getElementById('div1');\n"
             + "    div1.nextSibling.id='newId';\n"
             + "    alert(top.document.getElementById('newId').value);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<form id='form1'>"
             + "<div id='div1'/><input name='foo' type='text' value='bar' />"
             + "</form>"
             + "</body></html>";
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final List expectedAlerts = Collections.singletonList("bar");
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for bug 740665
      * @throws Exception if the test fails
      */
     public void testGetElementById_divId() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var element = document.getElementById('id1');\n"
             + "    alert(element.id);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<div id='id1'></div></body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final List expectedAlerts = Collections.singletonList("id1");
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for bug 740665
      * @throws Exception if the test fails
      */
     public void testGetElementById_scriptId() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script id='script1'>"
             + "function doTest() {\n"
             + "    alert(top.document.getElementById('script1').id);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final List expectedAlerts = Collections.singletonList("script1");
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for bug 740665
      * @throws Exception if the test fails
      */
     public void testGetElementById_scriptType() throws Exception {
         final String firstContent
             = "<html><head><title>First</title>"
             + "<script id='script1' type='text/javascript'>\n"
             + "doTest=function () {\n"
             + "    alert(top.document.getElementById('script1').type);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final List expectedAlerts = Collections.singletonList("text/javascript");
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for bug 740665
      * @throws Exception if the test fails
      */
     public void testGetElementById_scriptSrc() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection( webClient );
         webClient.setWebConnection( webConnection );
 
         final String firstContent
             = "<html><head><title>First</title>"
             + "<script id='script1' src='http://script'>\n"
             + "</script></head><body onload='doTest()'>"
             + "</body></html>";
         webConnection.setResponse(URL_FIRST, firstContent);
 
         final String scriptContent
             = "doTest=function () {\n"
             + "    alert(top.document.getElementById('script1').src);\n"
             + "}\n";
         webConnection.setResponse(new URL("http://script"), scriptContent, "text/javascript");
 
         final List collectedAlerts = new ArrayList();
         webClient.setAlertHandler( new CollectingAlertHandler(collectedAlerts) );
 
         final HtmlPage firstPage = ( HtmlPage )webClient.getPage( URL_FIRST );
         assertEquals( "First", firstPage.getTitleText() );
 
         final List expectedAlerts = Collections.singletonList("http://script");
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for parentNode with nested elements
      * @throws Exception if the test fails
      */
     public void testParentNode_Nested() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var div1=document.getElementById('childDiv');\n"
             + "    alert(div1.parentNode.id);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<div id='parentDiv'><div id='childDiv'></div></div>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final HtmlElement div1 = firstPage.getHtmlElementById("childDiv");
         assertEquals("parentDiv",
             ((HtmlElement) div1.getParentNode()).getAttributeValue("id"));
 
         final String[] expectedAlerts = {"parentDiv"};
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for parentNode of document
      * @throws Exception if the test fails
      */
     public void testParentNode_Document() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    alert(document.parentNode==null);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final List expectedAlerts = Collections.singletonList("true");
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for parentNode and createElement
      * @throws Exception if the test fails
      */
     public void testParentNode_CreateElement() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var div1=document.createElement('div');\n"
             + "    alert(div1.parentNode==null);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final String[] expectedAlerts = {"true"};
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for parentNode and appendChild
      * @throws Exception if the test fails
      */
     public void testParentNode_AppendChild() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var childDiv=document.getElementById('childDiv');\n"
             + "    var parentDiv=document.getElementById('parentDiv');\n"
             + "    parentDiv.appendChild(childDiv);\n"
             + "    alert(childDiv.parentNode.id);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<div id='parentDiv'></div><div id='childDiv'></div>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final HtmlElement childDiv = firstPage.getHtmlElementById("childDiv");
         assertEquals("parentDiv",
             ((HtmlElement) childDiv.getParentNode()).getAttributeValue("id"));
 
         final String[] expectedAlerts = {"parentDiv"};
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for documentElement of document
      * @throws Exception if the test fails
      */
     public void testDocumentElement() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    alert(document.documentElement!=null);\n"
             + "    alert(document.documentElement.tagName);\n"
             + "    alert(document.documentElement.parentNode==document);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final String[] expectedAlerts = { "true", "HTML", "true"};
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for firstChild with nested elements
      * @throws Exception if the test fails
      */
     public void testFirstChild_Nested() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var div1=document.getElementById('parentDiv');\n"
             + "    alert(div1.firstChild.id);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<div id='parentDiv'><div id='childDiv'/><div id='childDiv2'/></div>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final HtmlElement div1 = firstPage.getHtmlElementById("parentDiv");
         assertEquals("childDiv",
             ((HtmlElement) div1.getFirstChild()).getAttributeValue("id"));
 
         final String[] expectedAlerts = {"childDiv"};
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
 
     /**
      * Regression test for firstChild and appendChild
      * @throws Exception if the test fails
      */
     public void testFirstChild_AppendChild() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var childDiv=document.getElementById('childDiv');\n"
             + "    var parentDiv=document.getElementById('parentDiv');\n"
             + "    parentDiv.appendChild(childDiv);\n"
             + "    var childDiv2=document.getElementById('childDiv2');\n"
             + "    parentDiv.appendChild(childDiv2);\n"
             + "    alert(parentDiv.firstChild.id);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<div id='parentDiv'/><div id='childDiv'/><div id='childDiv2'/>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final HtmlElement parentDiv = firstPage.getHtmlElementById("parentDiv");
         assertEquals("childDiv",
             ((HtmlElement) parentDiv.getFirstChild()).getAttributeValue("id"));
 
         final String[] expectedAlerts = {"childDiv"};
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for lastChild with nested elements
      * @throws Exception if the test fails
      */
     public void testLastChild_Nested() throws Exception {
         final String lastContent
             = "<html><head><title>Last</title><script>"
             + "function doTest() {\n"
             + "    var div1=document.getElementById('parentDiv');\n"
             + "    alert(div1.lastChild.id);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<div id='parentDiv'><div id='childDiv1'/><div id='childDiv'/></div>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage lastPage = loadPage(lastContent, collectedAlerts);
         assertEquals( "Last", lastPage.getTitleText() );
 
         final HtmlElement parentDiv = lastPage.getHtmlElementById("parentDiv");
         assertEquals("childDiv",
             ((HtmlElement) parentDiv.getLastChild()).getAttributeValue("id"));
 
         final String[] expectedAlerts = {"childDiv"};
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for lastChild and appendChild
      * @throws Exception if the test fails
      */
     public void testLastChild_AppendChild() throws Exception {
         final String lastContent
             = "<html><head><title>Last</title><script>"
             + "function doTest() {\n"
             + "    var childDiv1=document.getElementById('childDiv1');\n"
             + "    var parentDiv=document.getElementById('parentDiv');\n"
             + "    parentDiv.appendChild(childDiv1);\n"
             + "    var childDiv=document.getElementById('childDiv');\n"
             + "    parentDiv.appendChild(childDiv);\n"
             + "    alert(parentDiv.lastChild.id);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<div id='parentDiv'/><div id='childDiv1'/><div id='childDiv'/>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage lastPage = loadPage(lastContent, collectedAlerts);
         assertEquals( "Last", lastPage.getTitleText() );
 
         final HtmlElement parentDiv = lastPage.getHtmlElementById("parentDiv");
         assertEquals("childDiv",
             ((HtmlElement) parentDiv.getLastChild()).getAttributeValue("id"));
 
         final List expectedAlerts = Collections.singletonList("childDiv");
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for nextSibling with nested elements
      * @throws Exception if the test fails
      */
     public void testNextSibling_Nested() throws Exception {
         final String lastContent
             = "<html><head><title>Last</title><script>"
             + "function doTest() {\n"
             + "    var div1=document.getElementById('previousDiv');\n"
             + "    alert(div1.nextSibling.id);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<div id='parentDiv'><div id='previousDiv'/><div id='nextDiv'/></div>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage lastPage = loadPage(lastContent, collectedAlerts);
         assertEquals( "Last", lastPage.getTitleText() );
 
         final HtmlElement div1 = lastPage.getHtmlElementById("previousDiv");
         assertEquals("nextDiv",
             ((HtmlElement) div1.getNextSibling()).getAttributeValue("id"));
 
         final List expectedAlerts = Collections.singletonList("nextDiv");
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for nextSibling and appendChild
      * @throws Exception if the test fails
      */
     public void testNextSibling_AppendChild() throws Exception {
         final String lastContent
             = "<html><head><title>Last</title><script>"
             + "function doTest() {\n"
             + "    var previousDiv=document.getElementById('previousDiv');\n"
             + "    var parentDiv=document.getElementById('parentDiv');\n"
             + "    parentDiv.appendChild(previousDiv);\n"
             + "    var nextDiv=document.getElementById('nextDiv');\n"
             + "    parentDiv.appendChild(nextDiv);\n"
             + "    alert(previousDiv.nextSibling.id);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<div id='parentDiv'/><div id='junk1'/><div id='previousDiv'/><div id='junk2'/><div id='nextDiv'/>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage lastPage = loadPage(lastContent, collectedAlerts);
         assertEquals( "Last", lastPage.getTitleText() );
 
         final HtmlElement previousDiv = lastPage.getHtmlElementById("previousDiv");
         assertEquals("nextDiv",
             ((HtmlElement) previousDiv.getNextSibling()).getAttributeValue("id"));
 
         final List expectedAlerts = Collections.singletonList("nextDiv");
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for previousSibling with nested elements
      * @throws Exception if the test fails
      */
     public void testPreviousSibling_Nested() throws Exception {
         final String lastContent
             = "<html><head><title>Last</title><script>"
             + "function doTest() {\n"
             + "    var div1=document.getElementById('nextDiv');\n"
             + "    alert(div1.previousSibling.id);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<div id='parentDiv'><div id='previousDiv'/><div id='nextDiv'/></div>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage lastPage = loadPage(lastContent, collectedAlerts);
         assertEquals( "Last", lastPage.getTitleText() );
 
         final HtmlElement div1 = lastPage.getHtmlElementById("nextDiv");
         assertEquals("previousDiv",
             ((HtmlElement) div1.getPreviousSibling()).getAttributeValue("id"));
 
         final List expectedAlerts = Collections.singletonList("previousDiv");
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for previousSibling and appendChild
      * @throws Exception if the test fails
      */
     public void testPreviousSibling_AppendChild() throws Exception {
         final String lastContent
             = "<html><head><title>Last</title><script>"
             + "function doTest() {\n"
             + "    var previousDiv=document.getElementById('previousDiv');\n"
             + "    var parentDiv=document.getElementById('parentDiv');\n"
             + "    parentDiv.appendChild(previousDiv);\n"
             + "    var nextDiv=document.getElementById('nextDiv');\n"
             + "    parentDiv.appendChild(nextDiv);\n"
             + "    alert(nextDiv.previousSibling.id);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<div id='parentDiv'/><div id='junk1'/><div id='previousDiv'/><div id='junk2'/><div id='nextDiv'/>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage lastPage = loadPage(lastContent, collectedAlerts);
         assertEquals( "Last", lastPage.getTitleText() );
 
         final HtmlElement nextDiv = lastPage.getHtmlElementById("nextDiv");
         assertEquals("previousDiv",
             ((HtmlElement) nextDiv.getPreviousSibling()).getAttributeValue("id"));
 
         final List expectedAlerts = Collections.singletonList("previousDiv");
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testAllProperty_KeyByName() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    alert(document.all['input1'].value);\n"
             + "    alert(document.all['foo2'].value);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'><form id='form1'>"
             + "    <input id='input1' name='foo1' type='text' value='tangerine' />"
             + "    <input id='input2' name='foo2' type='text' value='ginger' />"
             + "</form>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final List expectedAlerts = Arrays.asList( new String[]{
             "tangerine", "ginger"} );
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Regression test for bug 707750
      * @throws Exception if the test fails
      */
     public void testAllProperty_CalledDuringPageLoad() throws Exception {
         final String firstContent
             = "<html><body>"
             + "<div id='ARSMenuDiv1' style='VISIBILITY: hidden; POSITION: absolute; z-index: 1000000'></div>"
             + "<script language='Javascript'>"
             + "    var divObj = document.all['ARSMenuDiv1'];"
             + "    alert(divObj.tagName);"
             + "</script></body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals("", firstPage.getTitleText());
 
         final List expectedAlerts = Collections.singletonList("DIV");
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testDocumentWrite() throws Exception {
         final String firstContent
             = "<html><head><title>First</title></head><body>"
             + "<script>\n"
             + "document.write(\"<div id='div1'></div>\");\n"
             + "document.write('<div', \" id='div2'>\", '</div>');\n"
             + "document.writeln('<div', \" id='div3'>\", '</div>');\n"
             + "</script>"
             + "</form></body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         try {
             firstPage.getHtmlElementById("div1");
             firstPage.getHtmlElementById("div2");
             firstPage.getHtmlElementById("div3");
         }
         catch (final ElementNotFoundException e) {
             fail("Element not written to page as expected: " + e.getMessage());
         }
     }
 
     /**
      * Regression test for bug 743241
      * @throws Exception if the test fails
      */
     public void testDocumentWrite_LoadScript() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection( webClient );
         webClient.setWebConnection( webConnection );
 
         final String firstContent
             = "<html><head><title>First</title></head><body>"
             + "<script src='http://script'></script>"
             + "</form></body></html>";
         webConnection.setResponse(URL_FIRST, firstContent);
 
         final String scriptContent
             = "document.write(\"<div id='div1'></div>\");";
         webConnection.setResponse(new URL("http://script"), scriptContent, "text/javascript");
 
         final List collectedAlerts = new ArrayList();
         webClient.setAlertHandler( new CollectingAlertHandler(collectedAlerts) );
 
         final HtmlPage firstPage = ( HtmlPage )webClient.getPage( URL_FIRST );
         assertEquals( "First", firstPage.getTitleText() );
 
         try {
             firstPage.getHtmlElementById("div1");
         }
         catch (final ElementNotFoundException e) {
             fail("Element not written to page as expected");
         }
     }
 
     /**
      * Regression test for bug 715379
      * @throws Exception if the test fails
      */
     public void testDocumentWrite_script() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection( webClient );
         webClient.setWebConnection( webConnection );
 
         final String mainContent
             = "<html><head><title>Main</title></head><body>"
             + "<iframe name='iframe' id='iframe' src='http://first'></iframe>"
             + "<script type='text/javascript'>"
             + "document.write('<script type=\"text/javascript\" src=\"http://script\"></' + 'script>');"
             + "</script></body></html> ";
         webConnection.setResponse(new URL("http://main"), mainContent);
 
         final String firstContent
             = "<html><body><h1 id='first'>First</h1></body></html>";
         webConnection.setResponse(URL_FIRST, firstContent);
 
         final String secondContent
             = "<html><body><h1 id='second'>Second</h1></body></html>";
         webConnection.setResponse(URL_SECOND, secondContent);
 
         final String scriptContent
             = "document.getElementById('iframe').src = 'http://second';";
         webConnection.setResponse(new URL("http://script"), scriptContent, "text/javascript");
 
         final List collectedAlerts = new ArrayList();
         webClient.setAlertHandler( new CollectingAlertHandler(collectedAlerts) );
 
         final HtmlPage mainPage = ( HtmlPage )webClient.getPage( new URL( "http://main" ) );
         assertEquals( "Main", mainPage.getTitleText() );
 
         final HtmlInlineFrame iFrame =
             (HtmlInlineFrame) mainPage.getHtmlElementById( "iframe" );
 
         assertEquals( "http://second", iFrame.getSrcAttribute() );
 
         final HtmlPage enclosedPage = (HtmlPage) iFrame.getEnclosedPage();
         // This will blow up if the script hasn't been written to the document
         // and executed so the second page has been loaded.
         enclosedPage.getHtmlElementById( "second" );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testDocumentWrite_InDOM() throws Exception {
         final String mainContent
             = "<html><head><title>First</title></head><body>"
             + "<script type='text/javascript'>"
             + "document.write('<a id=\"blah\">Hello World</a>');"
             + "alert(document.getElementById('blah').tagName);"
             + "</script>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(mainContent, collectedAlerts);
         assertEquals("First", firstPage.getTitleText());
 
         assertEquals(collectedAlerts, Collections.singletonList("A"));
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testGetReferrer() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection( webClient );
 
         final String firstContent
             = "<html><head><title>First</title></head><body onload='alert(document.referrer);'>"
             + "</form></body></html>";
 
         final List responseHeaders = Collections.singletonList( new KeyValuePair("referrer", "http://ref") );
         webConnection.setResponse(
             URL_FIRST, firstContent, 200, "OK", "text/html", responseHeaders );
         webClient.setWebConnection( webConnection );
 
         final List collectedAlerts = new ArrayList();
         webClient.setAlertHandler( new CollectingAlertHandler(collectedAlerts) );
 
         final HtmlPage firstPage = ( HtmlPage )webClient.getPage( URL_FIRST );
         assertEquals( "First", firstPage.getTitleText() );
 
         assertEquals( Collections.singletonList("http://ref"), collectedAlerts );
     }
 
 
     /**
      * @throws Exception if the test fails
      */
     public void testGetReferrer_NoneSpecified() throws Exception {
         final String firstContent
             = "<html><head><title>First</title></head><body onload='alert(document.referrer);'>"
             + "</form></body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         assertEquals( Collections.singletonList(""), collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testGetURL() throws Exception {
         final String firstContent
             = "<html><head><title>First</title></head><body onload='alert(document.URL);'>"
             + "</form></body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         assertEquals(Collections.singletonList(URL_GARGOYLE.toExternalForm()), collectedAlerts);
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testGetElementsByTagName() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var elements = document.getElementsByTagName('input');\n"
             + "    for (var i=0; i<elements.length; i++) {\n"
             + "        alert(elements[i].type);\n"
             + "        alert(elements.item(i).type);\n"
             + "    }\n"
             + "    alert(elements == document.getElementsByTagName('input'));\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<form><input type='button' name='button1' value='pushme'></form>"
             + "</body></html>";
 
         final String[] expectedAlerts = {"button", "button", "true"};
         createTestPageForRealBrowserIfNeeded(firstContent, expectedAlerts);
 
         final List collectedAlerts = new ArrayList();
         loadPage(firstContent, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Regression test for bug 740636
      * @throws Exception if the test fails
      */
     public void testGetElementsByTagName_CaseInsensitive() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var elements = document.getElementsByTagName('InPuT');\n"
             + "    for( i=0; i<elements.length; i++ ) {\n"
             + "        alert(elements[i].type);\n"
             + "    }\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<form><input type='button' name='button1' value='pushme'></form>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final List expectedAlerts = Collections.singletonList("button");
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Regression test for bug 740605
      * @throws Exception if the test fails
      */
     public void testGetElementsByTagName_Inline() throws Exception {
         final String firstContent
             = "<html><body><script type=\"text/javascript\">"
             + "alert(document.getElementsByTagName('script').length);"
             + "</script></body></html>";
 
         final List collectedAlerts = new ArrayList();
         loadPage(firstContent, collectedAlerts);
         final List expectedAlerts = Collections.singletonList("1");
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Regression test for bug 740605
      * @throws Exception if the test fails
      */
     public void testGetElementsByTagName_LoadScript() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection( webClient );
         webClient.setWebConnection( webConnection );
 
         final String firstContent
             = "<html><body><script src=\"http://script\"></script></body></html>";
         webConnection.setResponse(URL_FIRST, firstContent);
 
         final String scriptContent
             = "alert(document.getElementsByTagName('script').length);";
         webConnection.setResponse(new URL("http://script"), scriptContent, "text/javascript");
 
         final List collectedAlerts = new ArrayList();
         webClient.setAlertHandler( new CollectingAlertHandler(collectedAlerts) );
 
         webClient.getPage( URL_FIRST );
 
         final List expectedAlerts = Collections.singletonList("1");
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testDocumentAll_WithParentheses() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var length = document.all.length;\n"
             + "    for( i=0; i< length; i++ ) {\n"
             + "        alert(document.all(i).tagName);\n"
             + "    }\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final String[] expectedAlerts = { "HTML", "HEAD", "TITLE", "SCRIPT", "BODY"};
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testDocumentAll_IndexByInt() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var length = document.all.length;\n"
             + "    for( i=0; i< length; i++ ) {\n"
             + "        alert(document.all[i].tagName);\n"
             + "    }\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final String[] expectedAlerts = { "HTML", "HEAD", "TITLE", "SCRIPT", "BODY"};
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testDocumentAll_Item() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    alert(document.all.item(0).tagName);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final List expectedAlerts = Arrays.asList( new String[] {"HTML"} );
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testDocumentAll_NamedItem() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>\n"
             + "function doTest() {\n"
             + "    alert(document.all.namedItem('form1').name);\n"
             + "    alert(document.all.namedItem('form2').id);\n"
             + "    alert(document.all.namedItem('form3').length);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>\n"
             + "<form name='form1'></form>\n"
             + "<form id='form2'></form>\n"
             + "<form name='form3'></form>\n"
             + "<form name='form3'></form>\n"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final String[] expectedAlerts = {"form1", "form2", "2"};
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testDocumentAll_tags() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var inputs = document.all.tags('input');\n"
             + "    var inputCount = inputs.length;\n"
             + "    for( i=0; i< inputCount; i++ ) {\n"
             + "        alert(inputs[i].name);\n"
             + "    }\n"
             + "    // Make sure tags() returns an element array that you can call item() on.\n"
             + "    alert(document.all.tags('input').item(0).name);\n"
             + "    alert(document.all.tags('input').item(1).name);\n"
             + "    // Make sure tags() returns an empty element array if there are no matches.\n"
             + "    alert(document.all.tags('xxx').length);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<input type='text' name='a' value='1'>"
             + "<input type='text' name='b' value='1'>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final String[] expectedAlerts = { "a", "b", "a", "b", "0"};
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Makes sure that the document.all collection contents are not cached if the
      * collection is accessed before the page has finished loading.
      * @throws Exception If the test fails.
      */
     public void testDocumentAll_Caching() throws Exception {
         final String firstContent
             = "<html><head><title>Test</title></head>"
             + "<body onload='alert(document.all.b.value)'>"
             + "<input type='text' name='a' value='1'>"
             + "<script>alert(document.all.a.value)</script>"
             + "<input type='text' name='b' value='2'>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "Test", firstPage.getTitleText() );
 
         final List expectedAlerts = Arrays.asList( new String[]{"1", "2"} );
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testCookie_read() throws Exception {
         final WebClient webClient = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection( webClient );
 
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var cookieSet = document.cookie.split(';');\n"
             + "    var setSize = cookieSet.length;\n"
             + "    var crumbs;\n"
             + "    var x=0;\n"
             + "    for (x=0;((x<setSize)); x++) {\n"
             + "        crumbs = cookieSet[x].split('=');\n"
             + "        alert ( crumbs[0] );\n"
             + "        alert ( crumbs[1] );\n"
             + "    } \n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "</body></html>";
 
         final URL url = URL_FIRST;
         webConnection.setResponse(url, firstContent);
         webClient.setWebConnection( webConnection );
 
         final HttpState state = webConnection.getState();
         state.addCookie( new Cookie("first", "one", "two", "/", -1, false) );
         state.addCookie( new Cookie("first", "three", "four", "/", -1, false) );
 
         final List collectedAlerts = new ArrayList();
         webClient.setAlertHandler( new CollectingAlertHandler(collectedAlerts) );
 
         final HtmlPage firstPage = ( HtmlPage )webClient.getPage( URL_FIRST );
         assertEquals( "First", firstPage.getTitleText() );
 
         final String[] expectedAlerts = { "one", "two", "three", "four" };
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testGetElementsByName() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    var elements = document.getElementsByName('name1');\n"
             + "    for (var i=0; i<elements.length; i++) {\n"
             + "        alert(elements[i].value);\n"
             + "        alert(elements.item(i).value);\n"
             + "    }\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<form>"
             + "<input type='radio' name='name1' value='value1'>"
             + "<input type='radio' name='name1' value='value2'>"
             + "<input type='button' name='name2' value='value3'>"
             + "</form>"
             + "</body></html>";
 
         final String[] expectedAlerts = { "value1", "value1", "value2", "value2" };
         createTestPageForRealBrowserIfNeeded(firstContent, expectedAlerts);
 
         final List collectedAlerts = new ArrayList();
         loadPage(firstContent, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testDocumentBody_read() throws Exception {
 
         final String html = "<html><head><title>First</title></head>"
             + "<body id='IAmTheBody' onload='alert(document.body.id)'>"
             + "</body></html>";
 
         final List expectedAlerts  = new ArrayList();
         expectedAlerts.add("IAmTheBody");
         createTestPageForRealBrowserIfNeeded(html, expectedAlerts);
 
         final List collectedAlerts = new ArrayList();
         loadPage(html, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
 
         final String html2 = "<html>"
             + "<frameset onload='alert(document.body.tagName)'>"
             + "<frame src='about:blank' name='foo'>"
             + "</frameset></html>";
 
         expectedAlerts.clear();
         collectedAlerts.clear();
 
         expectedAlerts.add("FRAMESET");
 
         loadPage(html2, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Test the access to the images value.  This should return the 2 images in the document
      * @throws Exception if the test fails
      */
     public void testGetImages() throws Exception {
         final String firstContent
             = "<html><head><title>First</title><script>"
             + "function doTest() {\n"
             + "    alert(document.images.length);\n"
             + "    alert(allImages.length);\n"
             + "    alert(document.images == allImages);\n"
             + "}\n"
             + "</script></head><body onload='doTest()'>"
             + "<img src='firstImage'>"
             + "<script>"
             + "var allImages = document.images;\n"
             + "alert(allImages.length);\n"
             + "</script>"
             + "<form>"
             + "<img src='2ndImage'>"
             + "</form>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage firstPage = loadPage(firstContent, collectedAlerts);
         assertEquals( "First", firstPage.getTitleText() );
 
         final String[] expectedAlerts = { "1", "2", "2", "true" };
         createTestPageForRealBrowserIfNeeded(firstContent, expectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Test setting and reading the title for an existing title
      *
      * @throws Exception if the test fails
      */
     public void testSettingTitle() throws Exception {
         final String content
             = "<html><head><title>Bad Title</title></head>\n"
             + "<body>\n"
             + "<script>\n"
             + "    document.title = 'correct title';\n"
             + "    alert(document.title)\n"
             + "</script>\n"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertEquals("correct title", page.getTitleText());
 
         final List expectedAlerts = Arrays.asList( new String[]{
             "correct title"
         } );
 
         assertEquals("Test the alert", expectedAlerts, collectedAlerts );
     }
 
     /**
      * Test setting and reading the title for when the is not in
      * the page to begin
      *
      * @throws Exception if the test fails
      */
     public void testSettingMissingTitle() throws Exception {
         final String content = "<html><head></head>\n"
             + "<body>\n"
             + "<script>\n"
             + "    document.title = 'correct title';\n"
             + "    alert(document.title)\n"
             + "</script>\n"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         loadPage(content, collectedAlerts);
         final List expectedAlerts = Arrays.asList( new String[]{ "correct title" } );
 
         assertEquals("Test the alert", expectedAlerts, collectedAlerts );
     }
 
     /**
      * Test setting and reading the title for when the is not in
      * the page to begin
      *
      * @throws Exception if the test fails
      */
     public void testSettingBlankTitle() throws Exception {
         final String content = "<html><head><title></title></head>\n"
             + "<body>\n"
             + "<script>\n"
             + "    document.title = 'correct title';\n"
             + "    alert(document.title)\n"
             + "</script>\n"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertEquals("correct title", page.getTitleText());
 
         final List expectedAlerts = Arrays.asList( new String[]{
             "correct title"
         } );
 
         assertEquals("Test the alert", expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testTitle() throws Exception {
         final String content = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    alert(document.title)\n"
             + "}\n"
             + "</script></head>\n"
             + "<body onload='doTest()'>\n"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertEquals("foo", page.getTitleText());
 
         final List expectedAlerts = Arrays.asList( new String[]{
             "foo"
         } );
 
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Test the ReadyState.  This should only work in IE.
      * Currently locked out since the browser type of code is not working.
      *
      * @throws Exception if the test fails
      */
     public void testReadyStateNonIE() throws Exception {
         final WebClient client = new WebClient(BrowserVersion.NETSCAPE_6_2_3);
         final MockWebConnection webConnection = new MockWebConnection( client );
         final String content = "<html><head>\n"
             + "<script>\n"
             + "function testIt() {\n"
             + "  alert(document.readyState);\n"
             + "}\n"
             + "alert(document.readyState);\n"
             + "</script>\n"
             + "</head>\n"
             + "<body onLoad='testIt()'></body></html>\n";
 
         webConnection.setResponse(URL_FIRST, content);
         client.setWebConnection( webConnection );
 
         final List collectedAlerts = new ArrayList();
         client.setAlertHandler( new CollectingAlertHandler(collectedAlerts) );
         client.getPage(URL_FIRST);
 
         final List expectedAlerts = Arrays.asList( new String[]{
             "undefined", "undefined"
         } );
 
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Test the ReadyState.  This should only work in IE
      *
      * @throws Exception if the test fails
      */
     public void testReadyStateIE() throws Exception {
         final WebClient client = new WebClient(BrowserVersion.INTERNET_EXPLORER_6_0);
         final MockWebConnection webConnection = new MockWebConnection( client );
         final String content = "<html><head>\n"
             + "<script>\n"
             + "function testIt() {\n"
             + "  alert(document.readyState);\n"
             + "}\n"
             + "alert(document.readyState);\n"
             + "</script>\n"
             + "</head>\n"
             + "<body onLoad='testIt()'></body></html>\n";
         webConnection.setResponse(URL_FIRST, content);
         client.setWebConnection( webConnection );
 
         final List collectedAlerts = new ArrayList();
         client.setAlertHandler( new CollectingAlertHandler(collectedAlerts) );
         client.getPage(URL_FIRST);
 
         final String[] expectedAlerts = { "loading", "complete" };
 
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Calling document.body before the page is fully loaded used to cause an exception.
      * @throws Exception if the test fails
      */
     public void testDocumentWithNoBody() throws Exception {
         final String content
             = "<html><head><title>foo</title><script>\n"
             + "alert(document.body)\n"
             + "</script></head><body></body></html>";
 
         final List collectedAlerts = new ArrayList();
         loadPage(content, collectedAlerts);
 
         final List expectedAlerts = Collections.singletonList("undefined");
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * IE has a bug which returns the element by name if it can not find it by ID.
      * @throws Exception if the test fails
      */
     public void testGetElementByIdForIE() throws Exception {
         final String content
             = "<html><head><title>foo</title></head>"
             + "<body>\n"
             + "<input type='text' name='findMe'>\n"
             + "<input type='text' id='findMe2' name='byId'>\n"
             + "<script>\n"
             + "alert(document.getElementById('findMe').name)\n"
             + "alert(document.getElementById('findMe2').name)\n"
             + "</script></body></html>";
 
         final List collectedAlerts = new ArrayList();
         loadPage(content, collectedAlerts);
 
         final String[] expectedAlerts = { "findMe", "byId" };
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * IE has a bug which returns the element by name if it can not find it by ID.
      * @throws Exception if the test fails
      */
     public void testGetElementByIdForNetscape() throws Exception {
         final WebClient client = new WebClient(BrowserVersion.NETSCAPE_6_2_3);
         final MockWebConnection webConnection = new MockWebConnection( client );
         final String content
             = "<html><head><title>foo</title></head>"
             + "<body>\n"
             + "<input type='text' name='findMe'>\n"
             + "<input type='text' id='findMe2' name='byId'>\n"
             + "<script>\n"
             + "alert(document.getElementById('findMe'))\n"
             + "alert(document.getElementById('findMe2').name)\n"
             + "</script></body></html>";
 
         webConnection.setResponse(URL_FIRST, content);
         client.setWebConnection( webConnection );
         final List collectedAlerts = new ArrayList();
         client.setAlertHandler( new CollectingAlertHandler(collectedAlerts) );
 
         client.getPage(URL_FIRST);
 
         final String[] expectedAlerts = { "null", "byId" };
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testBuildCookie() throws Exception {
         checkCookie(Document.buildCookie("toto=foo", URL_FIRST), "toto", "foo", "", "first", false, null);
         checkCookie(Document.buildCookie("toto=", URL_FIRST), "toto", "", "", "first", false, null);
         checkCookie(Document.buildCookie("toto=foo;secure", URL_FIRST), "toto", "foo", "", "first", true, null);
         checkCookie(Document.buildCookie("toto=foo;path=/myPath;secure", URL_FIRST),
                 "toto", "foo", "/myPath", "first", true, null);
 
         // Check that leading and trailing whitespaces are ignored
         checkCookie(Document.buildCookie("   toto=foo;  path=/myPath  ; secure  ", URL_FIRST),
                 "toto", "foo", "/myPath", "first", true, null);
 
         // Check that we accept reserved attribute names (e.g expires, domain) in any case
         checkCookie(Document.buildCookie("toto=foo; PATH=/myPath; SeCURE", URL_FIRST),
                 "toto", "foo", "/myPath", "first", true, null);
 
         // Check that we are able to parse and set the expiration date correctly
         final String dateString = "Fri, 21 Jul 2006 20:47:11 UTC";
         final Date date = DateUtil.parseDate(dateString);
         checkCookie(Document.buildCookie("toto=foo; expires=" + dateString, URL_FIRST),
                 "toto", "foo", "", "first", false, date);
 
     }
 
     private void checkCookie(final Cookie cookie, final String name, final String value,
             final String path, final String domain, final boolean secure, final Date date) {
         assertEquals(name, cookie.getName());
         assertEquals(value, cookie.getValue());
         assertNull(cookie.getComment());
         assertEquals(path, cookie.getPath());
         assertEquals(domain, cookie.getDomain());
         assertEquals(secure, cookie.getSecure());
         assertEquals(date, cookie.getExpiryDate());
     }
 
     /**
      * Test that img and form can be retrieved directly by name but not
      * a, input or button
      * @throws Exception if the test fails
      */
     public void testDirectAccessByName() throws Exception {
         final String content = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    alert(document.myImage.id);\n"
             + "    alert(document.myImage2.length);\n"
             + "    alert(document.myForm.tagName);\n"
             + "    alert(document.myAnchor);\n"
             + "    alert(document.myInput);\n"
             + "    alert(document.myInputImage);\n"
             + "    alert(document.myButton);\n"
             + "}\n"
             + "</script></head>\n"
             + "<body onload='doTest()'>\n"
             + "<img src='foo' name='myImage' id='myImageId'>\n"
             + "<img src='foo' name='myImage2'>\n"
             + "<img src='foo' name='myImage2'>\n"
             + "<a name='myAnchor'/>\n"
             + "<form name='myForm'>\n"
             + "<input name='myInput' type='text'>\n"
             + "<input name='myInputImage' type='image' src='foo'>\n"
             + "<button name='myButton'>foo</button>\n"
             + "</form>\n"
             + "</body></html>";
 
         final String[] expectedAlerts 
             = { "myImageId", "2", "FORM", "undefined", "undefined", "undefined", "undefined" };
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(content, collectedAlerts);
         assertEquals("foo", page.getTitleText());
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testWriteInManyTimes() throws Exception {
         final String content = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    alert(document.getElementById('inner').parentNode.id);\n"
             + "}\n"
             + "</script></head>\n"
             + "<body onload='doTest()'>\n"
             + "<script>\n"
             + "document.write('<div id=\"outer\">');"
             + "document.write('<div id=\"inner\"/>');"
             + "document.write('</div>');"
             + "</script>\n"
             + "</body></html>";
 
         final String[] expectedAlerts = { "outer" };
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
 
         final List collectedAlerts = new ArrayList();
         loadPage(content, collectedAlerts);
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testWriteWithSpace() throws Exception {
         final String content = "<html><body><script>\n"
             + "document.write('Hello ');\n"
             + "document.write('World');\n"
             + "</script>\n"
             + "</body></html>";
 
         final List expectedAlerts = Arrays.asList( new String[]{} );
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
 
         final WebClient client = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(client);
         client.setWebConnection(webConnection);
         webConnection.setResponse(URL_FIRST, content);
         final HtmlPage page = (HtmlPage) client.getPage(URL_FIRST);
         assertTrue(page.asText().indexOf("Hello World")>=0);
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testWriteWithSplitAnchorTag() throws Exception {
         final String content = "<html><body><script>\n"
             + "document.write(\"<a href=\'start.html\");\n"
             + "document.write(\"\'>\");\n"
             + "document.write('click here</a>');\n"
             + "</script>\n"
             + "</body></html>";
 
         final List expectedAlerts = Arrays.asList( new String[]{} );
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
 
         final WebClient client = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(client);
         client.setWebConnection(webConnection);
         webConnection.setResponse(URL_FIRST, content);
         final HtmlPage page = (HtmlPage) client.getPage(URL_FIRST);
         final List anchorList = page.getAnchors();
         assertEquals(1, anchorList.size());
         final HtmlAnchor anchor = (HtmlAnchor) anchorList.get(0);
         assertEquals("start.html", anchor.getHrefAttribute());
         assertEquals("click here", anchor.asText());
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testWriteScriptInManyTimes() throws Exception {
         final String content = "<html><head><title>foo</title>\n"
             + "<script>\n"
             + "document.write('<script src=\"script.js\">');"
             + "document.write('<' + '/script>');"
             + "document.write('<script>alert(\"foo2\");</' + 'script>');"
             + "</script>\n"
             + "</head>\n"
             + "<body>\n"
             + "</body></html>";
 
         final String[] expectedAlerts = { "foo", "foo2" };
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
 
         final URL scriptUrl = new URL(URL_FIRST.toExternalForm() + "/script.js");
         final WebClient client = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection(client);
         client.setWebConnection(webConnection);
         webConnection.setDefaultResponse(content);
         webConnection.setResponse(scriptUrl, "alert('foo');", "text/javascript");
 
         final List collectedAlerts = new ArrayList();
         client.setAlertHandler( new CollectingAlertHandler(collectedAlerts) );
         client.getPage(URL_FIRST);
 
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * Test for 1613119
      * @throws Exception if the test fails
      */
     public void testWriteAddNodesInCorrectPositions() throws Exception {
         final String content = "<html><head><title>foo</title></head>\n"
             + "<body id=\"theBody\">\n"
             + "<div id='target1'></div>"
             + "<script>\n"
             + "document.write(\""
             + "<div>"
             + "  <sc\"+\"ript id='scr1'>document.write('<div id=\\\"div1\\\" />');</s\"+\"cript>"
             + "  <sc\"+\"ript id='scr2'>document.write('<div id=\\\"div2\\\" />');</s\"+\"cript>"
             + "</div>"
             + "\");"
  /*           + "document.getElementById('target1').innerHTML = \""
             + "<div>"
             + "  <sc\"+\"ript id='scr3'>document.write('<div id=\\\"div3\\\" />');</s\"+\"cript>"
             + "  <sc\"+\"ript id='scr4'>document.write('<div id=\\\"div4\\\" />');</s\"+\"cript>"
             + "</div>"
             + "\";"
   */
             + "</script>\n"
             + "<script>\n"
             + "function alertId(obj) { alert(obj != null ? obj.id : 'null'); }\n"
             + "alertId(document.getElementById('div1').previousSibling);\n"
             + "alertId(document.getElementById('div2').previousSibling);\n"
  /*           + "alertId(document.getElementById('div3').previousSibling);\n"
             + "alertId(document.getElementById('div4').previousSibling);\n"
   */
             + "</script>\n"
             + "</body></html>";
 
         final String[] expectedAlerts = {"scr1", "scr2"/*, "scr3", "scr4"*/};
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
 
         final List collectedAlerts = new ArrayList();
         loadPage(content, collectedAlerts);
         
         assertEquals( expectedAlerts, collectedAlerts );
         
     }    
   
     /**
      * @throws Exception if the test fails
      */
     public void testDomain() throws Exception {
         final String content = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    alert(document.domain);\n"
             + "    document.domain = 'gargoylesoftware.com';\n"
             + "    alert(document.domain);\n"
             + "}\n"
             + "</script></head>\n"
             + "<body onload='doTest()'>\n"
             + "</body></html>";
 
         final List expectedAlerts = Arrays.asList( new String[]{
             "www.gargoylesoftware.com", "gargoylesoftware.com"
         } );
 
         final List collectedAlerts = new ArrayList();
         loadPage(content, collectedAlerts);
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
   /**
     * @throws Exception if the test fails
     */
     public void testDomainMixedCaseNetscape() throws Exception {
 
         final URL urlGargoyleUpperCase = new URL("http://WWW.GARGOYLESOFTWARE.COM");
 
         final WebClient client = new WebClient(BrowserVersion.NETSCAPE_6_2_3);
         final MockWebConnection webConnection = new MockWebConnection( client );
 
         final String content = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    alert(document.domain);\n"
             + "    document.domain = 'GaRgOyLeSoFtWaRe.CoM';\n"
             + "    alert(document.domain);\n"
             + "}\n"
             + "</script></head>\n"
             + "<body onload='doTest()'>\n"
             + "</body></html>";
 
 
         webConnection.setResponse(urlGargoyleUpperCase, content);
         client.setWebConnection( webConnection );
         final List collectedAlerts = new ArrayList();
         client.setAlertHandler( new CollectingAlertHandler(collectedAlerts) );
 
         client.getPage(urlGargoyleUpperCase);
 
         final String[] expectedAlerts = { "www.gargoylesoftware.com", "gargoylesoftware.com" };
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
   /**
     * @throws Exception if the test fails
     */
     public void testDomainMixedCase() throws Exception {
         final String content = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    alert(document.domain);\n"
             + "    document.domain = 'GaRgOyLeSoFtWaRe.CoM';\n"
             + "    alert(document.domain);\n"
             + "}\n"
             + "</script></head>\n"
             + "<body onload='doTest()'>\n"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         loadPage(content, collectedAlerts);
         final String[] expectedAlerts = { "www.gargoylesoftware.com", "GaRgOyLeSoFtWaRe.CoM" };
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
 
       /**
      * @throws Exception if the test fails
      */
     public void testDomainLong() throws Exception {
         final String content = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    alert(document.domain);\n"
             + "    document.domain = 'd4.d3.d2.d1.gargoylesoftware.com';\n"
             + "    alert(document.domain);\n"
             + "    document.domain = 'd1.gargoylesoftware.com';\n"
             + "    alert(document.domain);\n"
             + "}\n"
             + "</script></head>\n"
             + "<body onload='doTest()'>\n"
             + "</body></html>";
 
         final String[] expectedAlerts = 
         { "d4.d3.d2.d1.gargoylesoftware.com", "d4.d3.d2.d1.gargoylesoftware.com", "d1.gargoylesoftware.com" };
 
         final List collectedAlerts = new ArrayList();
         loadPage(content, collectedAlerts, new URL("http://d4.d3.d2.d1.gargoylesoftware.com"));
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testDomainSetSelf() throws Exception {
         final String content = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    alert(document.domain);\n"
             + "    document.domain = 'localhost';\n"
             + "    alert(document.domain);\n"
             + "}\n"
             + "</script></head>\n"
             + "<body onload='doTest()'>\n"
             + "</body></html>";
 
         final String[] expectedAlerts = { "localhost", "localhost" };
 
         final List collectedAlerts = new ArrayList();
         loadPage(content, collectedAlerts, new URL("http://localhost"));
         assertEquals( expectedAlerts, collectedAlerts );
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testDomainTooShort() throws Exception {
         final String content = "<html><head><title>foo</title><script>\n"
             + "function doTest(){\n"
             + "    alert(document.domain);\n"
             + "    document.domain = 'com';\n"
             + "    alert(document.domain);\n"
             + "}\n"
             + "</script></head>\n"
             + "<body onload='doTest()'>\n"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         try {
             loadPage(content, collectedAlerts);
         }
         catch (final ScriptException ex) {
             return;
         }
         fail();
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testDocumentWriteFrameRelativeURLMultipleFrameset() throws Exception {
         final String framesetContent = "<html><head><title>frameset</title></head>\n"
             + "<script>\n"
             + "    document.write('<frameset><frame src=\"frame.html\"/></frameset>');\n"
             + "</script>\n"
             + "<frameset><frame src='blank.html'/></frameset>"
             + "</html>";
 
         final URL baseURL = new URL("http://base/subdir/");
         final URL framesetURL = new URL(baseURL.toExternalForm() + "test.html");
         final URL frameURL = new URL(baseURL.toExternalForm() + "frame.html");
         final URL blankURL = new URL(baseURL.toExternalForm() + "blank.html");
 
         final WebClient client = new WebClient();
         final MockWebConnection webConnection = new MockWebConnection( client );
         webConnection.setResponse(framesetURL, framesetContent);
         webConnection.setResponseAsGenericHtml(frameURL, "frame");
         webConnection.setResponseAsGenericHtml(blankURL, "blank");
         client.setWebConnection( webConnection );
 
         final HtmlPage framesetPage = (HtmlPage) client.getPage(framesetURL);
         final FrameWindow frame = (FrameWindow) framesetPage.getFrames().get(0);
 
         assertNotNull(frame);
         assertEquals(frameURL.toExternalForm(), frame.getEnclosedPage().getWebResponse().getUrl().toExternalForm());
         assertEquals("frame", ((HtmlPage)frame.getEnclosedPage()).getTitleText());
     }
 
    /**
     * Test for 1185389
     * @throws Exception if the test fails
     */
     public void testWriteAddNodesToCorrectParent() throws Exception {
         if (notYetImplemented()) {
             return;
         }
 
         final String content = "<html><head><title>foo</title></head>\n"
             + "<body id=\"theBody\">\n"
             + "<script>\n"
             + "document.write('<p id=\"para1\">Paragraph #1</p>');\n"
             + "document.write('<p id=\"para2\">Paragraph #2</p>');\n"
             + "document.write('<p id=\"para3\">Paragraph #3</p>');\n"
             + "alert(document.getElementById('para1').parentNode.id);\n"
             + "alert(document.getElementById('para2').parentNode.id);\n"            
             + "alert(document.getElementById('para3').parentNode.id);\n"            
             + "</script>\n"
             + "</body></html>";
 
         final String[] expectedAlerts = {"theBody", "theBody", "theBody"};
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
 
         final List collectedAlerts = new ArrayList();
         loadPage(content, collectedAlerts);
         assertEquals( expectedAlerts, collectedAlerts );
     }
    
    /**
      * @throws Exception if the test fails
      */
     public void testScriptsArray() throws Exception {
         final String htmlContent = "<html><head><script lang='JavaScript'>"
             + "    function doTest(){"
             + "        alert(document.scripts.length);" // This line used to blow up
             + "}"
             + "</script></head><body onload='doTest();'>"
             + "<script>var scriptTwo = 1;</script>"
             + "</body></html> ";
 
         final List collectedAlerts = new ArrayList();
         loadPage(htmlContent, collectedAlerts);
         assertEquals(Collections.singletonList("2"), collectedAlerts);
     }
 
     /**
      * document.XXX should first look at elements named XXX before using standard functions.
      * @throws Exception if the test fails
      */
     public void testPrecedence() throws Exception {
         final String htmlContent = "<html><head></head>"
             + "<body>"
             + "<form name='writeln'>foo</form>"
             + "<script>alert(document.writeln.tagName);</script>"
             + "</body></html>";
         final List expectedAlerts = Arrays.asList(new String[] {"FORM"});
         createTestPageForRealBrowserIfNeeded(htmlContent, expectedAlerts);
 
         final List collectedAlerts = new ArrayList();
         loadPage(htmlContent, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testFrames() throws Exception {
         final String html = "<html><head><script>"
             + "function test(){"
             + "  if (document.frames)"
             + "  {"
             + "    alert(document.frames == window.frames);"
             + "    alert(document.frames.length);"
             + "    alert(document.frames(0).location);"
             + "    alert(document.frames('foo').location);"
             + "  }"
             + "  else"
             + "    alert('not defined');"
             + "}"
             + "</script></head><body onload='test();'>"
             + "<iframe src='about:blank' name='foo'></iframe>"
             + "</body></html> ";
 
         final List collectedAlerts = new ArrayList();
 
         // test for IE
         final String[] expectedAlerts = { "true", "1", "about:blank", "about:blank"};
         createTestPageForRealBrowserIfNeeded(html, expectedAlerts);
         loadPage(BrowserVersion.INTERNET_EXPLORER_6_0, html, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
 
         // test for Mozilla
         collectedAlerts.clear();
         loadPage(BrowserVersion.MOZILLA_1_0, html, collectedAlerts);
         assertEquals(Collections.singletonList("not defined"), collectedAlerts);
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testDefaultViewAndParentWindow() throws Exception {
         final String html = "<html><head><script>"
             + "function test(){"
             + "    alert(document.defaultView == window);"
             + "    alert(document.parentWindow == window);"
             + "}"
             + "</script></head><body onload='test()'>"
             + "</body></html> ";
 
         final List collectedAlerts = new ArrayList();
 
         // test for Mozilla
         final String[] expectedAlertsMoz = { "true", "false" };
         createTestPageForRealBrowserIfNeeded(html, expectedAlertsMoz);
         loadPage(BrowserVersion.MOZILLA_1_0, html, collectedAlerts);
         assertEquals(expectedAlertsMoz, collectedAlerts);
 
         // test for IE
         final String[] expectedAlertsIE = { "false", "true"};
         collectedAlerts.clear();
         createTestPageForRealBrowserIfNeeded(html, expectedAlertsIE);
         loadPage(BrowserVersion.INTERNET_EXPLORER_6_0, html, collectedAlerts);
         assertEquals(expectedAlertsIE, collectedAlerts);
     }
 
     /**
      * @throws Exception if the test fails
      */
     public void testAddEventListener() throws Exception {
         final String content = "<html><body>"
             + "<script>"
             + "function test()"
             + "{"
             + "  alert('in test');"
             + "}"
             + "document.addEventListener('click', test, false);"
             + "</script>"
             + "</body></html>";
 
         final List collectedAlerts = new ArrayList();
         final HtmlPage page = loadPage(BrowserVersion.MOZILLA_1_0, content, collectedAlerts);
         assertNotNull( page.getEventHandler( "click" ) );
     }
 
 }
