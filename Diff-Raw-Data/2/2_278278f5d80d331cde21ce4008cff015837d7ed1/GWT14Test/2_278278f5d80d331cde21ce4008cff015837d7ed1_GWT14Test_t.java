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
 package com.gargoylesoftware.htmlunit.libraries;
 
 import static org.junit.Assert.assertNotNull;
 import static org.junit.Assert.assertSame;
 import static org.junit.Assert.fail;
 
 import java.net.URL;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Set;
 
 import org.junit.After;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.mortbay.jetty.Server;
 
 import com.gargoylesoftware.htmlunit.BrowserRunner;
 import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
 import com.gargoylesoftware.htmlunit.HttpWebConnectionTest;
 import com.gargoylesoftware.htmlunit.Page;
 import com.gargoylesoftware.htmlunit.WebClient;
 import com.gargoylesoftware.htmlunit.WebTestCase;
 import com.gargoylesoftware.htmlunit.BrowserRunner.Browser;
 import com.gargoylesoftware.htmlunit.BrowserRunner.NotYetImplemented;
 import com.gargoylesoftware.htmlunit.html.DomNode;
 import com.gargoylesoftware.htmlunit.html.DomText;
 import com.gargoylesoftware.htmlunit.html.HtmlButton;
 import com.gargoylesoftware.htmlunit.html.HtmlDivision;
 import com.gargoylesoftware.htmlunit.html.HtmlInput;
 import com.gargoylesoftware.htmlunit.html.HtmlPage;
 import com.gargoylesoftware.htmlunit.html.HtmlSelect;
 import com.gargoylesoftware.htmlunit.html.HtmlSpan;
 import com.gargoylesoftware.htmlunit.html.HtmlTableDataCell;
 import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
 import com.gargoylesoftware.htmlunit.html.HtmlUnknownElement;
 
 /**
  * Tests for 1.4 version of <a href="http://code.google.com/webtoolkit">Google Web Toolkit</a>.
  *
  * @version $Revision$
  * @author Ahmed Ashour
  */
 @RunWith(BrowserRunner.class)
 public class GWT14Test extends WebTestCase {
 
     private Server server_;
 
     /**
      * @throws Exception If an error occurs.
      */
     @Test
     public void hello() throws Exception {
         final List<String> collectedAlerts = new ArrayList<String>();
         final HtmlPage page = loadGWTPage("Hello", collectedAlerts);
         final HtmlButton button = (HtmlButton) page.getFirstByXPath("//button");
         final DomText buttonLabel = (DomText) button.getChildren().iterator().next();
         assertEquals("Click me", buttonLabel.getData());
         button.click();
         final String[] expectedAlerts = {"Hello, AJAX"};
         assertEquals(expectedAlerts, collectedAlerts);
     }
 
     /**
      * Tests I18N default language.
      * @throws Exception If an error occurs.
      */
     @Test
     @NotYetImplemented(Browser.FIREFOX_2)
     public void i18n() throws Exception {
         final Locale locale = Locale.getDefault();
         Locale.setDefault(Locale.US);
 
         final HtmlPage page = loadGWTPage("I18N", null);
         i18n(page, "numberFormatOutputText", "31,415,926,535.898");
 
         String timeZone = new SimpleDateFormat("Z").format(
                 new SimpleDateFormat("d MMMMMMMM yyyy").parse("13 September 1999"));
         timeZone = timeZone.substring(0, 3) + ':' + timeZone.substring(3);
 
         i18n(page, "dateTimeFormatOutputText", "Monday, September 13, 1999 12:00:00 AM GMT" + timeZone);
         i18n(page, "messagesFormattedOutputText",
             "User 'amelie' has security clearance 'guest' and cannot access '/secure/blueprints.xml'");
         i18n(page, "constantsFirstNameText", "Amelie");
         i18n(page, "constantsLastNameText", "Crutcher");
         i18n(page, "constantsFavoriteColorList",
                 new String[] {"Red", "White", "Yellow", "Black", "Blue", "Green", "Grey", "Light Grey"});
         i18n(page, "constantsWithLookupResultsText", "Red");
         final Map<String, String> map = new HashMap<String, String>();
         map.put("name", "Amelie Crutcher");
         map.put("timeZone", "EST");
         map.put("userID", "123");
         map.put("lastLogOn", "2/2/2006");
         i18nDictionary(page, map);
         
         Locale.setDefault(locale);
     }
 
     /**
      * Test I18N French language.
      * @throws Exception If an error occurs.
      */
     @Test
     @NotYetImplemented(Browser.FIREFOX_2)
     public void i18n_fr() throws Exception {
         final Locale locale = Locale.getDefault();
         Locale.setDefault(Locale.US);
         
         server_ = HttpWebConnectionTest.startWebServer("src/test/resources/gwt/" + getDirectory() + "/I18N");
         final WebClient client = getWebClient();
 
         final String url = "http://localhost:" + HttpWebConnectionTest.PORT + "/I18N.html?locale=fr";
         final HtmlPage page = (HtmlPage) client.getPage(url);
         page.getEnclosingWindow().getThreadManager().joinAll(10000);
 
         //visible space in browser is not normal space but '\u00A0' instead, as noted by the following test in browser:
         /*
             var tr = document.getElementById('numberFormatOutputText');
             var value = tr.childNodes[0].childNodes[0].nodeValue;
             var output = '';
             for( var i=0; i < value.length; i++ ) {
               output += value.charCodeAt(i) + ' ';
             }
             alert(output);
          */
         i18n(page, "numberFormatOutputText", "31\u00A0415\u00A0926\u00A0535,898");
 
         String timeZone = new SimpleDateFormat("Z").format(
                 new SimpleDateFormat("d MMMMMMMM yyyy").parse("13 September 1999"));
         timeZone = timeZone.substring(0, 3) + ':' + timeZone.substring(3);
 
         i18n(page, "dateTimeFormatOutputText", "lundi 13 septembre 1999 00 h 00 GMT" + timeZone);
         i18n(page, "messagesFormattedOutputText",
             "L'utilisateur 'amelie' a un niveau de securit\u00E9 'guest', "
             + "et ne peut acc\u00E9der \u00E0 '/secure/blueprints.xml'");
         i18n(page, "constantsFirstNameText", "Amelie");
         i18n(page, "constantsLastNameText", "Crutcher");
         i18n(page, "constantsFavoriteColorList",
                 new String[] {"Rouge", "Blanc", "Jaune", "Noir", "Bleu", "Vert", "Gris", "Gris clair"});
         i18n(page, "constantsWithLookupResultsText", "Rouge");
         final Map<String, String> map = new HashMap<String, String>();
         map.put("name", "Amelie Crutcher");
         map.put("timeZone", "EST");
         map.put("userID", "123");
         map.put("lastLogOn", "2/2/2006");
         i18nDictionary(page, map);
         
         Locale.setDefault(locale);
     }
 
     /**
      * Test value inside {@link HtmlDivision} or {@link HtmlInput}
      * @param page The page to load.
      * @param id id of the element to search for.
      * @param expectedValue Expected value of the value inside the element
      * @throws Exception If the test fails.
      */
     private void i18n(final HtmlPage page, final String id, final String expectedValue) {
         final HtmlTableDataCell cell = (HtmlTableDataCell) page.getHtmlElementById(id);
         tableDataCell(cell, expectedValue);
     }
 
     /**
      * Test value inside {@link HtmlDivision}, {@link HtmlInput} or {@link DomText}
      *
      * @param cell the cells to search in.
      * @param expectedValue Expected value of the value inside the cell
      * @throws Exception If the test fails.
      */
     private void tableDataCell(final HtmlTableDataCell cell, final String expectedValue) {
         final Object child = cell.getFirstDomChild();
         if (child instanceof HtmlDivision) {
             final HtmlDivision div = (HtmlDivision) child;
             DomNode firstChild = div.getFirstDomChild();
             if (firstChild instanceof HtmlUnknownElement
                     && (firstChild.getNodeName().equals("b") || firstChild.getNodeName().equals("i"))) {
                 firstChild = firstChild.getFirstDomChild();
             }
             if (firstChild instanceof DomText) {
                 final DomText text = (DomText) firstChild;
                 assertEquals(expectedValue, text.getData());
             }
             else {
                 fail("Could not find '" + expectedValue + "'");
             }
         }
         else if (child instanceof HtmlInput) {
             final HtmlInput input = (HtmlInput) child;
             assertEquals(expectedValue, input.getValueAttribute());
         }
         else if (child instanceof DomText) {
             final DomText text = (DomText) child;
             assertEquals(expectedValue, text.getData());
         }
         else {
             fail("Could not find '" + expectedValue + "'");
         }
     }
 
     /**
      * Test value of {@link HtmlSelect}
      *
      * @param page The page to load.
      * @param id id of the element to search for.
      * @param expectedValues Expected value of the value inside the select.
      * @throws Exception If the test fails.
      */
     private void i18n(final HtmlPage page, final String id, final String[] expectedValues) {
         final HtmlTableDataCell cell = (HtmlTableDataCell) page.getHtmlElementById(id);
         final Object child = cell.getFirstDomChild();
         if (child instanceof HtmlSelect) {
             final HtmlSelect select = (HtmlSelect) child;
             assertEquals(expectedValues.length, select.getOptionSize());
             for (int i = 0; i < expectedValues.length; i++) {
                 assertEquals(expectedValues[i], select.getOption(i).getFirstDomChild().getNodeValue());
             }
         }
         else {
             fail("Could not find '" + expectedValues + "'");
         }
     }
 
     private void i18nDictionary(final HtmlPage page, final Map<String, String> expectedMap) throws Exception {
         final HtmlTableRow headerRow =
             (HtmlTableRow) page.getFirstByXPath("//*[@class='i18n-dictionary-header-row']");
         final HtmlTableRow valueRow = (HtmlTableRow) headerRow.getNextDomSibling();
         DomNode headerNode = headerRow.getFirstDomChild();
         DomNode valueNode = valueRow.getFirstDomChild();
         final Set<String> foundHeaders = new HashSet<String>();
         for (int i = 0; i < expectedMap.size(); i++) {
             final String header = headerNode.getFirstDomChild().getNodeValue();
             final String value = valueNode.getFirstDomChild().getNodeValue();
 
             assertNotNull(expectedMap.get(header));
             assertEquals(expectedMap.get(header), value);
             foundHeaders.add(header);
 
             valueNode = valueNode.getNextDomSibling();
             headerNode = headerNode.getNextDomSibling();
         }
         assertEquals(expectedMap.size(), foundHeaders.size());
     }
 
     /**
      * @throws Exception If an error occurs.
      */
     @Test
     @NotYetImplemented(Browser.FIREFOX_2)
     public void simpleXML() throws Exception {
         final HtmlPage page = loadGWTPage("SimpleXML", null);
 
         final String[] pendingOrders =
         {"123-2", "3 45122 34566", "2/2/2004", "43 Butcher lane", "Atlanta", "Georgia", "30366"};
 
 
         //try 20 times to wait .5 second each for filling the page.
         for (int i = 0; i < 20; i++) {
             if (page.getByXPath("//table[@class='userTable'][1]//tr[2]/td").size() == pendingOrders.length) {
                 break;
             }
             synchronized (page) {
                 page.wait(500);
             }
         }
 
         final List< ? > cells = page.getByXPath("//table[@class='userTable'][1]//tr[2]/td");
         assertEquals(pendingOrders.length, cells.size());
         for (int i = 0; i < pendingOrders.length; i++) {
             final HtmlTableDataCell cell = (HtmlTableDataCell) cells.get(i);
             tableDataCell(cell, pendingOrders[i]);
         }
     }
 
     /**
      * @throws Exception If an error occurs.
      */
     @Test
     public void mail() throws Exception {
         final HtmlPage page = loadGWTPage("Mail", null);
         final HtmlTableDataCell cell = (HtmlTableDataCell)
             page.getFirstByXPath("//table[@class='mail-TopPanel']//div[@class='gwt-HTML']//..");
         tableDataCell(cell, "Welcome back, foo@example.com");
 
         final String[] selectedRow = {"markboland05", "mark@example.com", "URGENT -[Mon, 24 Apr 2006 02:17:27 +0000]"};
 
         final List< ? > selectedRowCells = page.getByXPath("//tr[@class='mail-SelectedRow']/td");
         assertEquals(selectedRow.length, selectedRowCells.size());
         for (int i = 0; i < selectedRow.length; i++) {
             final HtmlTableDataCell selectedRowCell = (HtmlTableDataCell) selectedRowCells.get(i);
             tableDataCell(selectedRowCell, selectedRow[i]);
         }
 
         final List< ? > detailsCells = page.getByXPath("//div[@class='mail-DetailBody']/text()");
         final String[] details = {"Dear Friend,",
             "I am Mr. Mark Boland the Bank Manager of ABN AMRO BANK 101 Moorgate, London, EC2M 6SB."};
         for (int i = 0; i < details.length; i++) {
             final DomText text = (DomText) detailsCells.get(i);
             assertEquals(details[i], text.getData());
         }
     }
 
     /**
      * @throws Exception If an error occurs.
      */
     @Test
     public void json() throws Exception {
         final HtmlPage page = loadGWTPage("JSON", null);
         final HtmlButton button = (HtmlButton) page.getFirstByXPath("//button");
         button.click();
 
         //try 20 times to wait .5 second each for filling the page.
         for (int i = 0; i < 20; i++) {
             if (page.getFirstByXPath("//div[@class='JSON-JSONResponseObject']") != null) {
                 break;
             }
             synchronized (page) {
                 page.wait(500);
             }
         }
 
         final HtmlSpan span = (HtmlSpan)
             page.getFirstByXPath("//div[@class='JSON-JSONResponseObject']/span/div/table//td[2]/span/span");
         assertEquals("ResultSet", span.getFirstDomChild().getNodeValue());
     }
 
     /**
      * @throws Exception If an error occurs.
      */
     @Test
     public void dynaTable() throws Exception {
         server_ = HttpWebConnectionTest.startWebServer("src/test/resources/gwt/" + getDirectory() + "/DynaTable",
                 new String[] {"src/test/resources/gwt/" + getDirectory() + "/gwt-servlet.jar"});
         
        final WebClient client = getWebClient();
 
         final String url = "http://localhost:" + HttpWebConnectionTest.PORT + "/DynaTable.html";
         final HtmlPage page = (HtmlPage) client.getPage(url);
 
         final String[] firstRow = {"Inman Mendez",
             "Majoring in Phrenology", "Mon 9:45-10:35, Tues 2:15-3:05, Fri 8:45-9:35, Fri 9:45-10:35"};
 
         //try 40 times to wait .5 second each for filling the page.
         for (int i = 0; i < 40; i++) {
             final List< ? > detailsCells = page.getByXPath("//table[@class='table']//tr[2]/td");
             if (detailsCells.size() == firstRow.length) {
                 final HtmlTableDataCell firstCell = (HtmlTableDataCell) detailsCells.get(0);
                 if (firstCell.getFirstDomChild().getNodeValue().equals(firstRow[0])) {
                     break;
                 }
             }
             synchronized (page) {
                 page.wait(500);
             }
         }
 
         final List< ? > detailsCells = page.getByXPath("//table[@class='table']//tr[2]/td");
         assertEquals(firstRow.length, detailsCells.size());
         for (int i = 0; i < firstRow.length; i++) {
             final HtmlTableDataCell cell = (HtmlTableDataCell) detailsCells.get(i);
             tableDataCell(cell, firstRow[i]);
         }
     }
 
     /**
      * @throws Exception If an error occurs.
      */
     @Test
     @NotYetImplemented(Browser.FIREFOX_2)
     public void kitchenSink() throws Exception {
         server_ = HttpWebConnectionTest.startWebServer("src/test/resources/gwt/" + getDirectory() + "/KitchenSink");
         final WebClient client = getWebClient();
 
         final String url = "http://localhost:" + HttpWebConnectionTest.PORT + "/KitchenSink.html";
         final HtmlPage page = (HtmlPage) client.getPage(url);
         page.getEnclosingWindow().getThreadManager().joinAll(3000);
 
         final HtmlDivision infoDiv = (HtmlDivision) page.getFirstByXPath("//div[@class='ks-Info']");
         assertEquals("Introduction to the Kitchen Sink", infoDiv.getFirstDomChild().getFirstDomChild().getNodeValue());
 
         final Page page2 = page.getAnchorByHref("#Widgets").click();
         assertSame(page, page2);
         assertEquals("Basic Widgets", infoDiv.getFirstDomChild().getFirstDomChild().getNodeValue());
 
         final Page page3 = page.getAnchorByHref("#Panels").click();
         assertSame(page, page3);
         assertEquals("Panels", infoDiv.getFirstDomChild().getFirstDomChild().getNodeValue());
     }
 
     /**
      * Returns the GWT directory being tested.
      * @return the GWT directory being tested.
      */
     protected String getDirectory() {
         return "1.4.60";
     }
 
     /**
      * Loads the GWT unit test index page using the specified browser version, and test name.
      *
      * @param testName The test name.
      * @param collectedAlerts The List to collect alerts into.
      * @throws Exception if an error occurs.
      * @return The loaded page.
      */
     protected HtmlPage loadGWTPage(final String testName, final List<String> collectedAlerts) throws Exception {
         final String resource = "gwt/" + getDirectory() + "/" + testName + "/" + testName + ".html";
         final URL url = getClass().getClassLoader().getResource(resource);
         assertNotNull(url);
 
         final WebClient client = getWebClient();
         if (collectedAlerts != null) {
             client.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
         }
 
         final HtmlPage page = (HtmlPage) client.getPage(url);
         page.getEnclosingWindow().getThreadManager().joinAll(10000);
         return page;
     }
 
     /**
      * Performs post-test deconstruction.
      * @throws Exception if an error occurs
      */
     @After
     public void after() throws Exception {
         HttpWebConnectionTest.stopWebServer(server_);
         server_ = null;
     }
 
     /**
      * Test JavaScript: 'new Date().getTimezoneOffset()' compared to java.text.SimpleDateFormat.format().
      *
      * @throws Exception if the test fails
      */
     @Test
     public void dateGetTimezoneOffset() throws Exception {
         final String content = "<html><head><title>foo</title><script>\n"
             + "  function test() {\n"
             + "    var offset = Math.abs(new Date().getTimezoneOffset());\n"
             + "    var timezone = '' + (offset/60);\n"
             + "    if (timezone.length == 1)\n"
             + "      timezone = '0' + timezone;\n"
             + "    alert(timezone);\n"
             + "  }\n"
             + "</script></head><body onload='test()'>\n"
             + "</body></html>";
         String timeZone = new SimpleDateFormat("Z").format(Calendar.getInstance().getTime());
         timeZone = timeZone.substring(1, 3);
         final String[] expectedAlerts = {timeZone};
         final List<String> collectedAlerts = new ArrayList<String>();
         createTestPageForRealBrowserIfNeeded(content, expectedAlerts);
         loadPage(content, collectedAlerts);
         assertEquals(expectedAlerts, collectedAlerts);
     }
 }
