 /*
  * Copyright 2011-2012 SugarCRM Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you
  * may not use this file except in compliance with the License.  You
  * may may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
  * implied.  Please see the License for the specific language
  * governing permissions and limitations under the License.
  */
 
 package org.sugarcrm.voodoodriver;
 
 import java.util.List;
 import org.openqa.selenium.By;
 import org.openqa.selenium.Mouse;
 import org.openqa.selenium.JavascriptExecutor;
 import org.openqa.selenium.NoSuchElementException;
 import org.openqa.selenium.WebDriver;
 import org.openqa.selenium.WebElement;
 import com.gargoylesoftware.htmlunit.ElementNotFoundException;
 
 
 /**
  * Base class for VooDooDriver browser support.
  *
  * @author trampus
  * @author Jon duSaint
  */
 
 public abstract class Browser {
 
    /**
     * {@link WebDriver} backend
     */
 
    private WebDriver Driver = null;
 
    /**
     * Whether the browser window is closed.
     */
 
    private boolean closed = true;
 
    /**
     * The browser profile.
     */
 
    private String profile = null;
 
    /**
     * {@link Reporter} object used for logging.
     */
 
    private Reporter reporter = null;
 
    /**
     * Page assert file.
     */
 
    private String assertPageFile = null;
 
    /**
     * {@link PageAsserter} object.
     */
 
    private PageAsserter asserter = null;
 
 
    /**
     * Set the name of the browser profile.
     *
     * @param profile  browser profile name
     */
 
    public void setProfile(String profile) {
       this.profile = profile;
    }
 
 
    /**
     * Get the name of the current browser profile.
     *
     * @return current browser profile name
     */
    public String getProfile() {
       return this.profile;
    }
 
 
    /**
     * Get the {@link Mouse} object for access to the raw input device.
     *
     * @return the {@link Mouse} device for this machine
     */
 
    public abstract Mouse getMouse();
 
 
    /**
     * Create a new browser window.
     */
 
    public abstract void newBrowser();
 
 
    /**
     * Set this browser's download directory.
     *
     * @param dir  the download directory
     */
 
    public abstract void setDownloadDirectory(String dir);
 
 
    /**
     * Open the specified URL in the browser.
     *
     * @param url  URL to open
     */
 
    public void url(String url) {
       try {
          this.Driver.navigate().to(url);
       } catch (Exception exp) {
          exp.printStackTrace();  // XXX
       }
    }
 
 
    /**
     * Refresh/reload the current browser location.
     */
 
    public void refresh() {
       this.Driver.navigate().refresh();
    }
 
 
    /**
     * Navigate forward one page in the browser.
     */
 
    public void forward() {
       this.Driver.navigate().forward();
    }
 
 
    /**
     * Navigate back one page in the browser.
     */
 
    public void back() {
       this.Driver.navigate().back();
    }
 
 
    /**
     * Close the browser window.
     */
 
    public void close() {
       this.Driver.close();
       this.setBrowserClosed();
    }
 
 
    /**
     * Force the browser window to close via the native operating system.
     */
 
    public abstract void forceClose();
 
 
    /**
     * Return whether the browser window is closed.
     *
     * @return true if the browser window is close, false otherwise
     */
 
    public boolean isClosed() {
       return this.closed;
    }
 
 
    /**
     * Set the browser closed state to true.
     */
 
    public void setBrowserClosed() {
       this.closed = true;
    }
 
    /**
     * Set the browser closed state to false.
     */
 
    public void setBrowserOpened() {
       this.closed = false;
    }
 
 
    /**
     * Fetch the page source for the loaded page.
     *
     * @return HTML source for the current page
     */
 
    public String getPageSource() {
       int retries = 20;
 
       while (retries-- > 0) {
          try {
             return this.Driver.getPageSource();
          } catch (Exception e) {}
 
          try {
             Thread.sleep(1000);
          } catch (java.lang.InterruptedException e) {}
       }
 
       return "";
    }
 
 
    /**
     * Execute javascript in the browser.
     *
     * This method executes a javascript string in the context of the
     * browser.  During execution, a variable, "CONTROL", is created
     * for use by the script.
     *
     * @param script  The javascript to run in the browser.
     * @param element The Element to use on the page as the CONTROL var.
     * @return the {@link Object} returned by the javascript code
     */
 
    public Object executeJS(String script, WebElement element) {
       Object result = null;
       JavascriptExecutor js = (JavascriptExecutor)this.Driver;
 
       if (element != null) {
          result = js.executeScript(script, element);
       } else {
          result = js.executeScript(script);
       }
 
       return result;
    }
 
 
    /**
     * Fire a javascript event in the browser for an HTML element.
     *
     * @param element    the HTML element
     * @param eventType  which javascript event to fire
     * @return the {@link String} value returned by the event
     */
 
    public String fire_event(WebElement element, String eventType) {
       Object result;
       String eventjs_src = "";
       JavascriptEventTypes type = null;
       String tmp_type = eventType.toUpperCase().replaceAll("ON", "");
 
       try {
          UIEvents.valueOf(tmp_type);
          type = JavascriptEventTypes.UIEvent;
       } catch (Exception eo) {
          try {
             HTMLEvents.valueOf(tmp_type);
             type = JavascriptEventTypes.HTMLEvent;
          } catch (Exception ei) {
             return "";
          }
       }
 
       switch (type) {
       case HTMLEvent:
          break;
       case UIEvent:
          eventjs_src = this.generateUIEvent(UIEvents.valueOf(tmp_type));
          break;
       }
 
       result = this.executeJS(eventjs_src, element);
 
       return (result == null) ? "" : result.toString();
    }
 
 
    /**
     * Generate a browser event of the specified type.
     *
     * @param type  the type of browser event
     * @return the resulting browser event code
     */
 
    public String generateUIEvent(UIEvents type) {
       if (type == UIEvents.FOCUS) {
         return ("var ele = arguments[0];\n" +
                 "ele.focus();\nreturn 0;\n");
       }
 
       return ("var ele = arguments[0];\n" +
              "var evObj = document.createEvent('MouseEvents');\n" +
              "evObj.initMouseEvent('" + type.toString().toLowerCase() + "'," +
                                   " true, true, window, 1, 12, 345, 7, 220," +
                                   " false, false, true, false, 0, null);\n" +
              "ele.dispatchEvent(evObj);\n" +
               "return 0;\n");
    }
 
 
    /**
     * Find an element in the browser's DOM.
     *
     * @param by         element selection criteria
     * @param retryTime  time limit (in seconds) for retries
     *
     * @return the {@link WebElement} if found or null
     */
 
    public WebElement findElement(By by, int retryTime) {
       long end = System.currentTimeMillis() + retryTime * 1000;
 
       do {
          try {
             return this.Driver.findElement(by);
          } catch (Exception exp) {}
       } while (System.currentTimeMillis() < end);
 
       return null;
    }
 
 
    /**
     * Find more then one element in the browser's DOM.
     *
     * @param by         element selection criteria
     * @param retryTime  time limit (in seconds) for retries
     * @return a {@link List} of all {@link WebElement}s found
     */
 
    public List<WebElement> findElements(By by, int retryTime)
       throws NoSuchElementException {
       long end = System.currentTimeMillis() + retryTime * 1000;
       
       do {
          try {
             return this.Driver.findElements(by);
          } catch (Exception exp) {}
       } while (System.currentTimeMillis() < end);
 
       throw new NoSuchElementException("Failed to find element by " + by);
    }
 
 
    /**
     * Find an element in the browser's DOM using expanded search criteria.
     *
     * @param by         element selection criteria
     * @param retryTime  time limit (in seconds) for retries
     * @param index      index into the list of elements found
     * @param required   whether this element must be found
     * @param exists     whether this element exists
     * @return the {@link WebElement} found
     */
 
    public WebElement findElements(By by, int retryTime, int index,
                                   boolean required, boolean exists) {
       WebElement result = null;
       List<WebElement> elements = null;
       int len = 0;
       String msg = "";
 
       long end = System.currentTimeMillis() + retryTime * 1000;
 
       while (System.currentTimeMillis() < end) {
          try {
             elements = this.Driver.findElements(by);
             len = elements.size() -1;
             if (len >= index) {
                result = elements.get(index);
             }
          } catch (ElementNotFoundException expnot) {
             if (exists) {
                this.reporter.ReportError("Failed to find element!");
             } else {
                break;
             }
          } catch (Exception exp) {
             result = null;
             this.reporter.ReportException(exp);
          }
 
          if (result != null) {
             break;
          }
       }
 
       if (exists) {
          if (len < index && result == null && required != false) {
             msg = String.format("Failed to find element by index '%d', index is out of bounds!", index);
             this.reporter.ReportError(msg);
             result = null;
          }
       }
 
       return result;
    }
 
 
    /**
     * Set the internal {@link Reporter} object.
     *
     * @param rep  a {@link Reporter} object
     */
 
    public void setReporter(Reporter rep) {
       this.reporter = rep;
    }
 
 
    /**
     * Get the internal {@link Reporter} object.
     *
     * @return the {@link Reporter} object
     */
 
    public Reporter getReporter() {
       return this.reporter;
    }
 
 
    /**
     * Set the {@link WebDriver} for the browser to use.
     *
     * @param driver  the {@link WebDriver}
     */
 
    public void setDriver(WebDriver driver) {
       this.Driver = driver;
    }
 
    /**
     * Get the current {@link WebDriver}.
     *
     * @return {@link WebDriver}
     */
 
    public WebDriver getDriver() {
       return this.Driver;
    }
 
 
    /**
     * When set, the browser bypasses java Alert and Confirm dialogs.
     *
     * @param alert  whether to bypass alerts
     */
 
    public abstract void alertHack(boolean alert);
 
 
    /**
     * Assert the specified text is found in the current page.
     *
     * @param value  the search string
     * @return whether the text was found
     */
 
    public boolean Assert(String value) {
       return this.reporter.Assert(value, this.getPageSource());
    }
 
 
    /**
     * Assert the specified text is found in a {@link WebElement}
     *
     * @param value   the search string
     * @param parent  the element to search
     * @return whether the text was found
     */
 
    public boolean Assert(String value, WebElement parent) {
       return this.reporter.Assert(value, parent.getText());
    }
 
 
    /**
     * Assert the specified text does not exist in the current page.
     *
     * @param value  the search string
     * @return true if the text was not found, false otherwise
     */
 
    public boolean AssertNot(String value) {
       return this.reporter.AssertNot(value, this.getPageSource());
    }
 
 
    /**
     * Assert the specified text does not exist in a {@link WebElement}
     *
     * @param value   the search string
     * @param parent  the element to search
     * @return true if the text was not found, false otherwise
     */
 
    public boolean AssertNot(String value, WebElement parent) {
       return this.reporter.AssertNot(value, parent.getText());
    }
 
 
    /**
     * Check the current page against the page assert list.
     *
     * @param whitelist  {@link VDDHash} with values to ignore
     */
 
    public boolean assertPage(VDDHash whitelist) {
       boolean result = false;
 
       if (this.asserter == null && this.assertPageFile != null) {
          try {
             this.asserter = new PageAsserter(this.assertPageFile,
                                              this.reporter, whitelist);
          } catch (Exception exp) {
             this.reporter.ReportException(exp);
          }
       }
 
       if (this.asserter != null) {
          this.asserter.assertPage(this.getPageSource(), whitelist);
       }
 
       return result;
    }
 
 
    /**
     * Load the page assert file.
     *
     * @param filename  the file of page asserts
     * @param reporter  {@link Reporter} object to use
     */
 
    public void setAssertPageFile(String filename, Reporter reporter) {
       this.assertPageFile = filename;
       this.asserter = new PageAsserter(filename, reporter, null);
    }
 
 
    /**
     * Get page assert file.
     *
     * @return page assert file or null, if no file has been assigned
     */
 
    public String getAssertPageFile() {
       return this.assertPageFile;
    }
 }
