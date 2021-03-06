 /*
  * Copyright (c) 2002-2009 Gargoyle Software Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package com.gargoylesoftware.htmlunit.javascript.host;
 
 import java.io.IOException;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.WeakHashMap;
 
 import org.apache.commons.collections.Transformer;
 import org.apache.commons.lang.StringUtils;
 import org.mozilla.javascript.Context;
 import org.mozilla.javascript.Function;
 import org.mozilla.javascript.Scriptable;
 import org.mozilla.javascript.ScriptableObject;
 import org.mozilla.javascript.Undefined;
 
 import com.gargoylesoftware.htmlunit.AlertHandler;
 import com.gargoylesoftware.htmlunit.ConfirmHandler;
 import com.gargoylesoftware.htmlunit.DialogWindow;
 import com.gargoylesoftware.htmlunit.ElementNotFoundException;
 import com.gargoylesoftware.htmlunit.Page;
 import com.gargoylesoftware.htmlunit.PromptHandler;
 import com.gargoylesoftware.htmlunit.StatusHandler;
 import com.gargoylesoftware.htmlunit.TopLevelWindow;
 import com.gargoylesoftware.htmlunit.WebAssert;
 import com.gargoylesoftware.htmlunit.WebClient;
 import com.gargoylesoftware.htmlunit.WebWindow;
 import com.gargoylesoftware.htmlunit.WebWindowNotFoundException;
 import com.gargoylesoftware.htmlunit.html.BaseFrame;
 import com.gargoylesoftware.htmlunit.html.DomChangeEvent;
 import com.gargoylesoftware.htmlunit.html.DomChangeListener;
 import com.gargoylesoftware.htmlunit.html.DomNode;
 import com.gargoylesoftware.htmlunit.html.FrameWindow;
 import com.gargoylesoftware.htmlunit.html.HtmlAttributeChangeEvent;
 import com.gargoylesoftware.htmlunit.html.HtmlAttributeChangeListener;
 import com.gargoylesoftware.htmlunit.html.HtmlElement;
 import com.gargoylesoftware.htmlunit.html.HtmlLink;
 import com.gargoylesoftware.htmlunit.html.HtmlPage;
 import com.gargoylesoftware.htmlunit.html.HtmlStyle;
 import com.gargoylesoftware.htmlunit.html.NonSerializable;
 import com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine;
 import com.gargoylesoftware.htmlunit.javascript.ScriptableWithFallbackGetter;
 import com.gargoylesoftware.htmlunit.javascript.SimpleScriptable;
 
 /**
  * A JavaScript object for a Window.
  *
  * @version $Revision$
  * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
  * @author <a href="mailto:chen_jun@users.sourceforge.net">Chen Jun</a>
  * @author David K. Taylor
  * @author <a href="mailto:cse@dynabean.de">Christian Sell</a>
  * @author Darrell DeBoer
  * @author Marc Guillemot
  * @author Dierk Koenig
  * @author Daniel Gredler
  * @author David D. Kilzer
  * @author Chris Erskine
  * @author Ahmed Ashour
  * @see <a href="http://msdn.microsoft.com/en-us/library/ms535873.aspx">MSDN documentation</a>
  */
 public class Window extends SimpleScriptable implements ScriptableWithFallbackGetter {
 
     private static final long serialVersionUID = -7730298149962810325L;
     private HTMLDocument document_;
     private Navigator navigator_;
     private WebWindow webWindow_;
     private Screen screen_;
     private History history_;
     private Location location_;
     private Object event_;
     private String status_ = "";
     private HTMLCollection frames_; // has to be a member to have equality (==) working
     private Map<Class< ? extends SimpleScriptable>, Scriptable> prototypes_ =
         new HashMap<Class< ? extends SimpleScriptable>, Scriptable>();
     private final JavaScriptEngine scriptEngine_;
     private EventListenersContainer eventListenersContainer_;
     private Object controllers_;
 
     /**
      * Cache computed styles when possible, because their calculation is very expensive, involving lots
      * of CSS parsing and lots of XPath expression evaluation (CSS selectors are translated to XPath and
      * then evaluated). We use a weak hash map because we don't want this cache to be the only reason
      * nodes are kept around in the JVM, if all other references to them are gone.
      */
     private Map<Node, ComputedCSSStyleDeclaration> computedStyles_ =
         new WeakHashMap<Node, ComputedCSSStyleDeclaration>();
 
     /**
      * Creates an instance.
      *
      * @param scriptEngine the JavaScript engine responsible for the new window instance
      */
     public Window(final JavaScriptEngine scriptEngine) {
         scriptEngine_ = scriptEngine;
     }
 
     /**
      * Gets the JavaScript Engine responsible for this object.
      * @return the JavaScript engine
      */
     public JavaScriptEngine getJavaScriptEngine() {
         return scriptEngine_;
     }
 
     /**
      * Returns the prototype object corresponding to the specified HtmlUnit class inside the window scope.
      * @param jsClass the class whose prototype is to be returned
      * @return the prototype object corresponding to the specified class inside the specified scope
      */
     @Override
     public Scriptable getPrototype(final Class< ? extends SimpleScriptable> jsClass) {
         return prototypes_.get(jsClass);
     }
 
     /**
      * Sets the prototypes for HtmlUnit host classes.
      * @param map a Map of ({@link Class}, {@link Scriptable})
      */
     public void setPrototypes(final Map<Class< ? extends SimpleScriptable>, Scriptable> map) {
         prototypes_ = map;
     }
 
     /**
      * The JavaScript function "alert()".
      * @param message the message
      */
     public void jsxFunction_alert(final Object message) {
         // use Object as parameter and perform String conversion by ourself
         // this allows to place breakpoint here and "see" the message object and its properties
         final String stringMessage = Context.toString(message);
         final AlertHandler handler = getWebWindow().getWebClient().getAlertHandler();
         if (handler == null) {
             getLog().warn("window.alert(\"" + stringMessage + "\") no alert handler installed");
         }
         else {
             handler.handleAlert(document_.getHtmlPage(), stringMessage);
         }
     }
 
     /**
      * The JavaScript function "confirm()".
      * @param message the message
      * @return true if ok was pressed, false if cancel was pressed
      */
     public boolean jsxFunction_confirm(final String message) {
         final ConfirmHandler handler = getWebWindow().getWebClient().getConfirmHandler();
         if (handler == null) {
             getLog().warn("window.confirm(\""
                     + message + "\") no confirm handler installed, simulating the OK button");
             return true;
         }
         return handler.handleConfirm(document_.getHtmlPage(), message);
     }
 
     /**
      * The JavaScript function "prompt()".
      * @param message the message
      * @return true if ok was pressed, false if cancel was pressed
      */
     public String jsxFunction_prompt(final String message) {
         final PromptHandler handler = getWebWindow().getWebClient().getPromptHandler();
         if (handler == null) {
             getLog().warn("window.prompt(\"" + message + "\") no prompt handler installed");
             return null;
         }
         return handler.handlePrompt(document_.getHtmlPage(), message);
     }
 
     /**
      * Returns the JavaScript property "document".
      * @return the document
      */
     public HTMLDocument jsxGet_document() {
         return document_;
     }
 
     /**
      * Returns the current event.
      * @return <code>null</code> if no event is currently available
      */
     public Object jsxGet_event() {
         return event_;
     }
 
     /**
      * Sets the current event.
      * @param event the event
      */
     public void setEvent(final Object event) {
         event_ = event;
     }
 
     /**
      * Opens a new window.
      *
      * @param url when a new document is opened, <i>url</i> is a String that specifies a MIME type for the document.
      *        When a new window is opened, <i>url</i> is a String that specifies the URL to render in the new window
      * @param name the name
      * @param features the features
      * @param replace whether to replace in the history list or no
      * @return the newly opened window, or <tt>null</tt> if popup windows have been disabled
      * @see WebClient#isPopupBlockerEnabled()
      * @see <a href="http://msdn.microsoft.com/en-us/library/ms536651.aspx">MSDN documentation</a>
      */
     public Object jsxFunction_open(final Object url, final Object name, final Object features,
             final Object replace) {
         String urlString = null;
         if (url != Undefined.instance) {
             urlString = (String) url;
         }
         String windowName = "";
         if (name != Undefined.instance) {
             windowName = (String) name;
         }
         String featuresString = null;
         if (features != Undefined.instance) {
             featuresString = (String) features;
         }
         boolean replaceCurrentEntryInBrowsingHistory = false;
         if (replace != Undefined.instance) {
             replaceCurrentEntryInBrowsingHistory = (Boolean) replace;
         }
         final WebClient webClient = webWindow_.getWebClient();
 
         if (webClient.isPopupBlockerEnabled()) {
             getLog().debug("Ignoring window.open() invocation because popups are blocked.");
             return null;
         }
 
         if (featuresString != null || replaceCurrentEntryInBrowsingHistory) {
             getLog().debug(
                 "window.open: features and replaceCurrentEntryInBrowsingHistory "
                 + "not implemented: url=[" + urlString
                 + "] windowName=[" + windowName
                 + "] features=[" + featuresString
                 + "] replaceCurrentEntry=[" + replaceCurrentEntryInBrowsingHistory
                 + "]");
         }
 
         // if specified name is the name of an existing window, then hold it
         if (StringUtils.isEmpty(urlString) && !"".equals(windowName)) {
             final WebWindow webWindow;
             try {
                 webWindow = webClient.getWebWindowByName(windowName);
                 return webWindow.getScriptObject();
             }
             catch (final WebWindowNotFoundException e) {
                 // nothing
             }
         }
         final URL newUrl = makeUrlForOpenWindow(urlString);
         final WebWindow newWebWindow = webClient.openWindow(newUrl, windowName, webWindow_);
         return newWebWindow.getScriptObject();
     }
 
     /**
      * Creates a popup window.
      * @see <a href="http://msdn.microsoft.com/en-us/library/ms536392.aspx">MSDN documentation</a>
      * @return the created popup
      */
     public Popup jsxFunction_createPopup() {
         final Popup popup = new Popup();
         popup.setParentScope(this);
         popup.setPrototype(getPrototype(Popup.class));
         popup.init(this);
         return popup;
     }
 
     private URL makeUrlForOpenWindow(final String urlString) {
         if (urlString.length() == 0) {
             return WebClient.URL_ABOUT_BLANK;
         }
 
         try {
             final Page page = webWindow_.getEnclosedPage();
             if (page != null && page instanceof HtmlPage) {
                 return ((HtmlPage) page).getFullyQualifiedUrl(urlString);
             }
             return new URL(urlString);
         }
         catch (final MalformedURLException e) {
             getLog().error("Unable to create URL for openWindow: relativeUrl=[" + urlString + "]", e);
             return null;
         }
     }
 
     /**
      * Sets a chunk of JavaScript to be invoked at some specified time later.
      * The invocation occurs only if the window is opened after the delay
      * and does not contain an other page than the one that originated the setTimeout.
      *
      * @param code specifies the function pointer or string that indicates the code to be executed
      *        when the specified interval has elapsed
      * @param timeout specifies the number of milliseconds
      * @param language specifies language
      * @return the id of the created timer
      */
     public int jsxFunction_setTimeout(final Object code, final int timeout, final Object language) {
         final int id;
         final Page page = (Page) getDomNodeOrNull();
         final String description = "window.setTimeout(" + timeout + ")";
         if (code == null) {
             throw Context.reportRuntimeError("Function not provided.");
         }
         else if (code instanceof String) {
             final String scriptString = (String) code;
             id = getWebWindow().getJobManager().addJob(scriptString, timeout, description, page);
         }
         else if (code instanceof Function) {
             final Function scriptFunction = (Function) code;
             id = getWebWindow().getJobManager().addJob(scriptFunction, timeout, description, page);
         }
         else {
             throw Context.reportRuntimeError("Unknown type for function.");
         }
         return id;
     }
 
     /**
      * Cancels a time-out previously set with the <tt>setTimeout</tt> method.
      *
      * @param timeoutId identifier for the timeout to clear (returned by <tt>setTimeout</tt>)
      */
     public void jsxFunction_clearTimeout(final int timeoutId) {
         getWebWindow().getJobManager().stopJobAsap(timeoutId);
     }
 
     /**
      * Returns the JavaScript property "navigator".
      * @return the navigator
      */
     public Navigator jsxGet_navigator() {
         return navigator_;
     }
 
     /**
      * Returns the JavaScript property "clientInformation".
      * @return the client information
      */
     public Navigator jsxGet_clientInformation() {
         return navigator_;
     }
 
     /**
      * Returns the JavaScript property "clipboardData".
      * @return the ClipboardData
      */
     public ClipboardData jsxGet_clipboardData() {
         final ClipboardData clipboardData = new ClipboardData();
         clipboardData.setParentScope(this);
         clipboardData.setPrototype(getPrototype(clipboardData.getClass()));
         return clipboardData;
     }
 
     /**
      * Returns the window property. This is a synonym for "self".
      * @return the window property (a reference to <tt>this</tt>)
      */
     public Window jsxGet_window() {
         return this;
     }
 
     /**
      * Returns the "self" property.
      * @return this
      */
     public Window jsxGet_self() {
         return this;
     }
 
     /**
      * Returns the location property.
      * @return the location property
      */
     public Location jsxGet_location() {
         return location_;
     }
 
     /**
      * Sets the location property. This will cause a reload of the window.
      * @param newLocation the URL of the new content
      * @throws IOException when location loading fails
      */
     public void jsxSet_location(final String newLocation) throws IOException {
         location_.jsxSet_href(newLocation);
     }
 
     /**
      * Returns the "screen" property.
      * @return the screen property
      */
     public Screen jsxGet_screen() {
         return screen_;
     }
 
     /**
      * Returns the "history" property.
      * @return the "history" property
      */
     public History jsxGet_history() {
         return history_;
     }
 
     /**
      * Initializes this window.
      * @param webWindow the web window corresponding to this window
      */
     public void initialize(final WebWindow webWindow) {
         webWindow_ = webWindow;
         webWindow_.setScriptObject(this);
 
         document_ = new HTMLDocument();
         document_.setParentScope(this);
         document_.setPrototype(getPrototype(HTMLDocument.class));
         document_.setWindow(this);
         if (webWindow.getEnclosedPage() instanceof DomNode) {
             document_.setDomNode((DomNode) webWindow.getEnclosedPage());
         }
 
         final DomHtmlAttributeChangeListenerImpl listener = new DomHtmlAttributeChangeListenerImpl();
         final DomNode docNode = document_.getDomNodeOrNull();
         if (docNode != null) {
             docNode.addDomChangeListener(listener);
             if (docNode instanceof HtmlElement) {
                 ((HtmlElement) docNode).addHtmlAttributeChangeListener(listener);
             }
         }
 
         navigator_ = new Navigator();
         navigator_.setParentScope(this);
         navigator_.setPrototype(getPrototype(Navigator.class));
 
         screen_ = new Screen();
         screen_.setParentScope(this);
         screen_.setPrototype(getPrototype(Screen.class));
 
         history_ = new History();
         history_.setParentScope(this);
         history_.setPrototype(getPrototype(History.class));
 
         location_ = new Location();
         location_.setParentScope(this);
         location_.setPrototype(getPrototype(Location.class));
         location_.initialize(this);
 
         // like a JS new Object()
         final Context ctx = Context.getCurrentContext();
         controllers_ = ctx.newObject(this);
     }
 
     /**
      * Initialize the object.
      * @param enclosedPage the page containing the JavaScript
      */
     public void initialize(final Page enclosedPage) {
         if (enclosedPage instanceof HtmlPage) {
             final HtmlPage htmlPage = (HtmlPage) enclosedPage;
 
             // Windows don't have corresponding DomNodes so set the domNode
             // variable to be the page. If this isn't set then SimpleScriptable.get()
             // won't work properly
             setDomNode(htmlPage);
             eventListenersContainer_ = null;
 
             WebAssert.notNull("document_", document_);
             document_.setDomNode(htmlPage);
         }
     }
 
     /**
      * Initialize the object. Only call for Windows with no contents.
      */
     public void initialize() {
     }
 
     /**
      * Returns the value of the top property.
      * @return the value of "top"
      */
     public SimpleScriptable jsxGet_top() {
         final WebWindow topWebWindow = webWindow_.getTopWindow();
         return (SimpleScriptable) topWebWindow.getScriptObject();
     }
 
     /**
      * Returns the value of the parent property.
      * @return the value of window.parent
      */
     public SimpleScriptable jsxGet_parent() {
         final WebWindow parentWebWindow = webWindow_.getParentWindow();
         return (SimpleScriptable) parentWebWindow.getScriptObject();
     }
 
     /**
      * Returns the value of the opener property.
      * @return the value of window.opener, <code>null</code> for a top level window
      */
     public Object jsxGet_opener() {
         if (webWindow_ instanceof TopLevelWindow) {
             final WebWindow opener = ((TopLevelWindow) webWindow_).getOpener();
             if (opener != null) {
                 return opener.getScriptObject();
             }
         }
 
         return null;
     }
 
     /**
      * Returns the (i)frame in which the window is contained.
      * @return <code>null</code> for a top level window
      */
     public Object jsxGet_frameElement() {
         final WebWindow window = getWebWindow();
         if (window instanceof FrameWindow) {
             return ((FrameWindow) window).getFrameElement().getScriptObject();
         }
         return null;
     }
 
     /**
      * Returns the value of the frames property.
      * @return the live collection of frames
      */
     public HTMLCollection jsxGet_frames() {
         if (frames_ == null) {
             final String xpath = ".//*[(name() = 'frame' or name() = 'iframe')]";
             final HtmlPage page = (HtmlPage) getWebWindow().getEnclosedPage();
             frames_ = new HTMLCollection(this);
             final Transformer toEnclosedWindow = new Transformer() {
                 public Object transform(final Object obj) {
                     if (obj instanceof BaseFrame) {
                         return ((BaseFrame) obj).getEnclosedWindow();
                     }
                     return ((FrameWindow) obj).getFrameElement().getEnclosedWindow();
                 }
             };
             frames_.init(page, xpath, toEnclosedWindow);
         }
 
         return frames_;
     }
 
     /**
      * Returns the WebWindow associated with this Window.
      * @return the WebWindow
      */
     public WebWindow getWebWindow() {
         return webWindow_;
     }
 
     /**
      * Sets the focus to this element.
      */
     public void jsxFunction_focus() {
         webWindow_.getWebClient().setCurrentWindow(webWindow_);
     }
 
     /**
      * Removes focus from this element.
      */
     public void jsxFunction_blur() {
         getLog().debug("window.blur() not implemented");
     }
 
     /**
      * Closes this window.
      */
     public void jsxFunction_close() {
         getWebWindow().getWebClient().deregisterWebWindow(getWebWindow());
     }
 
     /**
      * Indicates if this window is closed.
      * @return <code>true</code> if this window is closed
      */
     public boolean jsxGet_closed() {
         return !getWebWindow().getWebClient().getWebWindows().contains(getWebWindow());
     }
 
     /**
      * Does nothing.
      * @param x the horizontal position
      * @param y the vertical position
      */
     public void jsxFunction_moveTo(final int x, final int y) {
         getLog().debug("window.moveTo() not implemented");
     }
 
     /**
      * Does nothing.
      * @param x the horizontal position
      * @param y the vertical position
      */
     public void jsxFunction_moveBy(final int x, final int y) {
         getLog().debug("window.moveBy() not implemented");
     }
 
     /**
      * Does nothing.
      * @param width the width offset
      * @param height the height offset
      */
     public void jsxFunction_resizeBy(final int width, final int height) {
         getLog().debug("window.resizeBy() not implemented");
     }
 
     /**
      * Does nothing.
      * @param width the width of the Window in pixel after resize
      * @param height the height of the Window in pixel after resize
      */
     public void jsxFunction_resizeTo(final int width, final int height) {
         getLog().debug("window.resizeTo() not implemented");
     }
 
     /**
      * Does nothing.
      * @param x the horizontal position to scroll to
      * @param y the vertical position to scroll to
      */
     public void jsxFunction_scroll(final int x, final int y) {
         getLog().debug("window.scroll() not implemented");
     }
 
     /**
      * Does nothing.
      * @param x the horizontal distance to scroll by
      * @param y the vertical distance to scroll by
      */
     public void jsxFunction_scrollBy(final int x, final int y) {
         getLog().debug("window.scrollBy() not implemented");
     }
 
     /**
      * Does nothing.
      * @param lines the number of lines to scroll down
      */
     public void jsxFunction_scrollByLines(final int lines) {
         getLog().debug("window.scrollByLines() not implemented");
     }
 
     /**
      * Does nothing.
      * @param pages the number of pages to scroll down
      */
     public void jsxFunction_scrollByPages(final int pages) {
         getLog().debug("window.scrollByPages() not implemented");
     }
 
     /**
      * Does nothing.
      * @param x the horizontal position to scroll to
      * @param y the vertical position to scroll to
      */
     public void jsxFunction_scrollTo(final int x, final int y) {
         getLog().debug("window.scrollTo() not implemented");
     }
 
     /**
      * Sets the value of the onload event handler.
      * @param newOnload the new handler
      */
     public void jsxSet_onload(final Object newOnload) {
         getEventListenersContainer().setEventHandlerProp("load", newOnload);
     }
 
     /**
      * Sets the value of the onclick event handler.
      * @param newOnload the new handler
      */
     public void jsxSet_onclick(final Object newOnload) {
         getEventListenersContainer().setEventHandlerProp("click", newOnload);
     }
 
     /**
      * Returns the onclick property (caution this is not necessary a function if something else has
      * been set).
      * @return the onclick property
      */
     public Object jsxGet_onclick() {
         return getEventListenersContainer().getEventHandlerProp("click");
     }
 
     /**
      * Sets the value of the ondblclick event handler.
      * @param newHandler the new handler
      */
     public void jsxSet_ondblclick(final Object newHandler) {
         getEventListenersContainer().setEventHandlerProp("dblclick", newHandler);
     }
 
     /**
      * Returns the ondblclick property (caution this is not necessary a function if something else has
      * been set).
      * @return the ondblclick property
      */
     public Object jsxGet_ondblclick() {
         return getEventListenersContainer().getEventHandlerProp("dblclick");
     }
 
     /**
      * Returns the onload property. Note that this is not necessarily a function if something else has been set.
      * @return the onload property
      */
     public Object jsxGet_onload() {
         final Object onload = getEventListenersContainer().getEventHandlerProp("load");
         if (onload == null) {
             // NB: for IE, the onload of window is the one of the body element but not for Mozilla.
             final HtmlPage page = (HtmlPage) webWindow_.getEnclosedPage();
             final HtmlElement body = page.getBody();
             if (body != null) {
                 return body.getEventHandler("onload");
             }
             return null;
         }
         return onload;
     }
 
     /**
      * Gets the container for event listeners.
      * @return the container (newly created if needed)
      */
     EventListenersContainer getEventListenersContainer() {
         if (eventListenersContainer_ == null) {
             eventListenersContainer_ = new EventListenersContainer(this);
         }
         return eventListenersContainer_;
     }
 
     /**
      * Allows the registration of event listeners on the event target.
      * @param type the event type to listen for (like "load")
      * @param listener the event listener
      * @see <a href="http://msdn.microsoft.com/en-us/library/ms536343.aspx">MSDN documentation</a>
      * @return <code>true</code> if the listener has been added
      */
     public boolean jsxFunction_attachEvent(final String type, final Function listener) {
         return getEventListenersContainer().addEventListener(StringUtils.substring(type, 2), listener, false);
     }
 
     /**
      * Allows the registration of event listeners on the event target.
      * @param type the event type to listen for (like "onload")
      * @param listener the event listener
      * @param useCapture If <code>true</code>, indicates that the user wishes to initiate capture (not yet implemented)
      * @see <a href="http://developer.mozilla.org/en/docs/DOM:element.addEventListener">Mozilla documentation</a>
      */
     public void jsxFunction_addEventListener(final String type, final Function listener, final boolean useCapture) {
         getEventListenersContainer().addEventListener(type, listener, useCapture);
     }
 
     /**
      * Allows the removal of event listeners on the event target.
      * @param type the event type to listen for (like "onload")
      * @param listener the event listener
      * @see <a href="http://msdn.microsoft.com/en-us/library/ms536411.aspx">MSDN documentation</a>
      */
     public void jsxFunction_detachEvent(final String type, final Function listener) {
         getEventListenersContainer().removeEventListener(StringUtils.substring(type, 2), listener, false);
     }
 
     /**
      * Allows the removal of event listeners on the event target.
      * @param type the event type to listen for (like "load")
      * @param listener the event listener
      * @param useCapture If <code>true</code>, indicates that the user wishes to initiate capture (not yet implemented)
      * @see <a href="http://developer.mozilla.org/en/docs/DOM:element.removeEventListener">Mozilla documentation</a>
      */
     public void jsxFunction_removeEventListener(final String type, final Function listener, final boolean useCapture) {
         getEventListenersContainer().removeEventListener(type, listener, useCapture);
     }
 
     /**
      * Returns the value of the name property.
      * @return the window name
      */
     public String jsxGet_name() {
         return webWindow_.getName();
     }
 
      /**
      * Sets the value of the newName property.
      * @param newName the new window name
      */
     public void jsxSet_name(final String newName) {
         webWindow_.setName(newName);
     }
 
     /**
      * Returns the value of the onerror property.
      * @return the value
      */
     public String jsxGet_onerror() {
         getLog().debug("window.onerror not implemented");
         return "";
     }
 
     /**
      * Sets the value of the onerror property.
      * @param newValue the value
      */
     public void jsxSet_onerror(final String newValue) {
         getLog().debug("window.onerror not implemented");
     }
 
     /**
      * Looks at attributes with the specified name.
      * {@inheritDoc}
      */
     public Object getWithFallback(final String name) {
         Object result = NOT_FOUND;
 
         final DomNode domNode = getDomNodeOrNull();
         if (domNode != null) {
 
             // May be attempting to retrieve a frame by name.
             final HtmlPage page = (HtmlPage) domNode.getPage();
             result = getFrameByName(page, name);
 
             if (result == NOT_FOUND) {
                 // May be attempting to retrieve element(s) by name. IMPORTANT: We're using map-backed operations
                 // like getHtmlElementsByName() and getHtmlElementById() as much as possible, so as to avoid XPath
                 // overhead. We only use an XPath-based operation when we have to (where there is more than one
                 // matching element). This optimization appears to improve performance in certain situations by ~15%
                 // vs using XPath-based operations throughout.
                 final List<HtmlElement> elements = page.getElementsByName(name);
                 if (elements.size() == 1) {
                     result = getScriptableFor(elements.get(0));
                 }
                 else if (elements.size() > 1) {
                     result = document_.jsxFunction_getElementsByName(name);
                 }
                 else {
                     // May be attempting to retrieve element by ID (again, try a map-back operation instead of XPath).
                     try {
                         final HtmlElement htmlElement = page.getHtmlElementById(name);
                         result = getScriptableFor(htmlElement);
                     }
                     catch (final ElementNotFoundException e) {
                         result = NOT_FOUND;
                     }
                 }
             }
         }
 
         return result;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public Object get(String name, final Scriptable start) {
         // Hack to make eval work in other window scope when needed.
         // See unit test testEvalScopeOtherWindow().
         // TODO: Find a cleaner way to handle this.
         if ("eval".equals(name)) {
             final Window w = (Window) getTopScope(getStartingScope());
             if (w != this) {
                 return getAssociatedValue("custom_eval");
             }
         }
         else if ("Option".equals(name)) {
             name = "HTMLOptionElement";
         }
         else if ("Image".equals(name)) {
             name = "HTMLImageElement";
         }
         final Object superValue = super.get(name, start);
         if (superValue == NOT_FOUND && getWebWindow() != null && getBrowserVersion().isIE()) {
             final Object element = jsxGet_document().jsxFunction_getElementById(name);
             if (element instanceof HTMLUnknownElement) {
                 final HtmlElement unknownElement = ((HTMLUnknownElement) element).getDomNodeOrDie();
                 if (unknownElement.getNodeName().equals("xml")) {
                     final XMLDocument document = ActiveXObject.buildXMLDocument(getWebWindow());
                     document.setParentScope(this);
                     final Iterator<HtmlElement> children = unknownElement.getAllHtmlChildElements().iterator();
                     if (children.hasNext()) {
                         final HtmlElement root = children.next();
                         document.jsxFunction_loadXML(root.asXml().trim());
                     }
                     return document;
                 }
             }
 
         }
         return superValue;
     }
 
     private static Scriptable getTopScope(final Scriptable s) {
         Scriptable top = s;
         while (top != null && top.getParentScope() != null) {
             top = top.getParentScope();
         }
         return top;
     }
 
     private static Object getFrameByName(final HtmlPage page, final String name) {
         try {
             return page.getFrameByName(name).getScriptObject();
         }
         catch (final ElementNotFoundException e) {
             return NOT_FOUND;
         }
     }
 
     /**
      * Executes the specified script code as long as the language is JavaScript or JScript. Does
      * nothing if the language specified is VBScript.
      * @param script the script code to execute
      * @param language the language of the specified code ("JavaScript", "JScript" or "VBScript")
      * @see <a href="http://msdn.microsoft.com/en-us/library/ms536420.aspx">MSDN documentation</a>
      * @return this method always returns <code>null</code>, like Internet Explorer
      */
     public Object jsxFunction_execScript(final String script, final Object language) {
         final String languageStr = Context.toString(language);
         if (language == Undefined.instance
             || "javascript".equalsIgnoreCase(languageStr) || "jscript".equalsIgnoreCase(languageStr)) {
             custom_eval(script);
             return null;
         }
         else if ("vbscript".equalsIgnoreCase(languageStr)) {
             getLog().warn("VBScript not supported in Window.execScript().");
         }
         else {
             // Unrecognized language: use the IE error message ("Invalid class string").
             throw Context.reportRuntimeError("Invalid class string");
         }
         return null;
     }
 
     /**
      * Executes the specified script code in the scope of this window.
      * This is used only when eval() is called on a Window other than the starting scope
      * @param scriptCode some JavaScript code
      * @return the evaluation result
      */
     public Object custom_eval(final String scriptCode) {
         final Context context = Context.getCurrentContext();
         final org.mozilla.javascript.Script script = context.compileString(scriptCode, "eval body", 0, null);
         return script.exec(context, this);
     }
 
     /**
      * Returns the text from the status line.
      * @return the status line text
      */
     public String jsxGet_status() {
         return status_;
     }
 
     /**
      * Sets the text from the status line.
      * @param message the status line text
      */
     public void jsxSet_status(final String message) {
         status_ = message;
 
         final StatusHandler statusHandler = webWindow_.getWebClient().getStatusHandler();
         if (statusHandler != null) {
             statusHandler.statusMessageChanged(webWindow_.getEnclosedPage(), message);
         }
     }
 
     /**
      * Sets a chunk of JavaScript to be invoked each time a specified number of milliseconds has elapsed
      * Current implementation does nothing.
      *
      * @see <a href="http://msdn.microsoft.com/en-us/library/ms536749.aspx">MSDN documentation</a>
      * @param code specifies the function pointer or string that indicates the code to be executed
      *        when the specified interval has elapsed
      * @param timeout specifies the number of milliseconds
      * @param language specifies language
      * @return the id of the created interval
      */
     public int jsxFunction_setInterval(final Object code, final int timeout, final Object language) {
         final int id;
         final Page page = (Page) getDomNodeOrNull();
         final String description = "window.setInterval(" + timeout + ")";
         if (code == null) {
             throw Context.reportRuntimeError("Function not provided.");
         }
         else if (code instanceof String) {
             final String scriptString = (String) code;
             id = getWebWindow().getJobManager().addRecurringJob(scriptString, timeout, description, page);
         }
         else if (code instanceof Function) {
             final Function scriptFunction = (Function) code;
             id = getWebWindow().getJobManager().addRecurringJob(scriptFunction, timeout, description, page);
         }
         else {
             throw Context.reportRuntimeError("Unknown type for function.");
         }
         return id;
     }
 
     /**
      * Cancels the interval previously started using the setInterval method.
      * Current implementation does nothing.
      * @param intervalID specifies the interval to cancel as returned by the setInterval method
      * @see <a href="http://msdn.microsoft.com/en-us/library/ms536353.aspx">MSDN documentation</a>
      */
     public void jsxFunction_clearInterval(final int intervalID) {
         getWebWindow().getJobManager().stopJobAsap(intervalID);
     }
 
     /**
      * Returns the innerWidth.
      * @return a dummy value
      * @see <a href="http://www.mozilla.org/docs/dom/domref/dom_window_ref28.html">Mozilla doc</a>
      */
     public int jsxGet_innerWidth() {
         return 1276; // why this value? this is the current value of my Mozilla
     }
 
     /**
      * Returns the outerWidth.
      * @return a dummy value
      * @see <a href="http://www.mozilla.org/docs/dom/domref/dom_window_ref79.html">Mozilla doc</a>
      */
     public int jsxGet_outerWidth() {
         return 1276; // why this value? this is the current value of my Mozilla
     }
 
     /**
      * Returns the innerHeight.
      * @return a dummy value
      * @see <a href="http://www.mozilla.org/docs/dom/domref/dom_window_ref27.html">Mozilla doc</a>
      */
     public int jsxGet_innerHeight() {
         return 778; // why this value? this is the current value of my Mozilla
     }
 
     /**
      * Returns the outer height.
      * @return a dummy value
      * @see <a href="http://www.mozilla.org/docs/dom/domref/dom_window_ref78.html">Mozilla doc</a>
      */
     public int jsxGet_outerHeight() {
         return 936; // why this value? this is the current value of my Mozilla
     }
 
     /**
      * Prints the current page. The current implementation does nothing.
      * @see <a href="http://www.mozilla.org/docs/dom/domref/dom_window_ref85.html">
      * Mozilla documentation</a>
      * @see <a href="http://msdn.microsoft.com/en-us/library/ms536672.aspx">MSDN documentation</a>
      */
     public void jsxFunction_print() {
         getLog().debug("window.print() not implemented");
     }
 
     /**
      * Does nothing special anymore... just like FF.
      * @param type the type of events to capture
      * @see Document#jsxFunction_captureEvents(String)
      */
     public void jsxFunction_captureEvents(final String type) {
         // Empty.
     }
 
     /**
      * An undocumented IE function.
      */
     public void jsxFunction_CollectGarbage() {
         // Empty.
     }
 
     /**
      * Returns computed style of the element. Computed style represents the final computed values
      * of all CSS properties for the element. This method's return value is of the same type as
      * that of <tt>element.style</tt>, but the value returned by this method is read-only.
      *
      * @param element the element
      * @param pseudo a string specifying the pseudo-element to match (may be <tt>null</tt>)
      * @return the computed style
      */
     public ComputedCSSStyleDeclaration jsxFunction_getComputedStyle(final HTMLElement element, final String pseudo) {
         ComputedCSSStyleDeclaration style = computedStyles_.get(element);
         if (style != null) {
             return style;
         }
 
         final CSSStyleDeclaration original = element.jsxGet_style();
         style = new ComputedCSSStyleDeclaration(original);
 
         final StyleSheetList sheets = document_.jsxGet_styleSheets();
         for (int i = 0; i < sheets.jsxGet_length(); i++) {
             final Stylesheet sheet = (Stylesheet) sheets.jsxFunction_item(i);
             getLog().debug("modifyIfNecessary: " + sheet + ", " + style + ", " + element);
             sheet.modifyIfNecessary(style, element);
         }
 
         computedStyles_.put(element, style);
 
         return style;
     }
 
     /**
      * Returns the current selection.
      * @return the current selection
      */
     public Selection jsxFunction_getSelection() {
         final Selection selection = new Selection();
         selection.setParentScope(this);
         selection.setPrototype(getPrototype(selection.getClass()));
         return selection;
     }
 
     /**
      * Creates a modal dialog box that displays the specified HTML document.
      * @param url the URL of the document to load and display
      * @param arguments object to be made available via <tt>window.dialogArguments</tt> in the dialog window
      * @param features string that specifies the window ornaments for the dialog window
      * @return the value of the <tt>returnValue</tt> property as set by the modal dialog's window
      * @see <a href="http://msdn.microsoft.com/en-us/library/ms536759.aspx">MSDN Documentation</a>
      * @see <a href="https://developer.mozilla.org/en/DOM/window.showModalDialog">Mozilla Documentation</a>
      */
     public Object jsxFunction_showModalDialog(final String url, final Object arguments, final String features) {
         final WebWindow ww = getWebWindow();
         final WebClient client = ww.getWebClient();
         try {
            final DialogWindow dialog = client.openDialogWindow(new URL(url), ww, arguments);
             // TODO: Theoretically, we shouldn't return until the dialog window has been close()'ed...
             // But we have to return so that the window can be close()'ed...
             // Maybe we can use Rhino's continuation support to save state and restart when
             // the dialog window is close()'ed? Would only work in interpreted mode, though.
             final ScriptableObject jsDialog = (ScriptableObject) dialog.getScriptObject();
             return jsDialog.get("returnValue", jsDialog);
         }
         catch (final IOException e) {
             throw Context.throwAsScriptRuntimeEx(e);
         }
     }
 
     /**
      * Creates a modeless dialog box that displays the specified HTML document.
      * @param url the URL of the document to load and display
      * @param arguments object to be made available via <tt>window.dialogArguments</tt> in the dialog window
      * @param features string that specifies the window ornaments for the dialog window
      * @return a reference to the new window object created for the modeless dialog
      * @see <a href="http://msdn.microsoft.com/en-us/library/ms536761.aspx">MSDN Documentation</a>
      */
     public Object jsxFunction_showModelessDialog(final String url, final Object arguments, final String features) {
         final WebWindow ww = getWebWindow();
         final WebClient client = ww.getWebClient();
         try {
            final DialogWindow dialog = client.openDialogWindow(new URL(url), ww, arguments);
             final Window jsDialog = (Window) dialog.getScriptObject();
             return jsDialog;
         }
         catch (final IOException e) {
             throw Context.throwAsScriptRuntimeEx(e);
         }
     }
 
     /**
      * <p>Listens for changes anywhere in the document and evicts cached computed styles whenever something relevant
      * changes. Note that the very lazy way of doing this (completely clearing the cache every time something happens)
      * results in very meager performance gains. In order to get good (but still correct) performance, we need to be
      * a little smarter.</p>
      *
      * <p>CSS 2.1 has the following <a href="http://www.w3.org/TR/CSS21/selector.html">selector types</a> (where "SN" is
      * shorthand for "the selected node"):</p>
      *
      * <ol>
      *   <li><em>Universal</em> (i.e. "*"): Affected by the removal of SN from the document.</li>
      *   <li><em>Type</em> (i.e. "div"): Affected by the removal of SN from the document.</li>
      *   <li><em>Descendant</em> (i.e. "div span"): Affected by changes to SN or to any of its ancestors.</li>
      *   <li><em>Child</em> (i.e. "div > span"): Affected by changes to SN or to its parent.</li>
      *   <li><em>Adjacent Sibling</em> (i.e. "table + p"): Affected by changes to SN or its previous sibling.</li>
      *   <li><em>Attribute</em> (i.e. "div.up, div[class~=up]"): Affected by changes to an attribute of SN.</li>
      *   <li><em>ID</em> (i.e. "#header): Affected by changes to the <tt>id</tt> attribute of SN.</li>
      *   <li><em>Pseudo-Elements and Pseudo-Classes</em> (i.e. "p:first-child"): Affected by changes to parent.</li>
      * </ol>
      *
      * <p>Together, these rules dictate that the smart (but still lazy) way of removing elements from the computed style
      * cache is as follows -- whenever a node changes in any way, the cache needs to be cleared of styles for nodes
      * which:</p>
      *
      * <ul>
      *   <li>are actually the same node as the node that changed</li>
      *   <li>are siblings of the node that changed</li>
      *   <li>are descendants of the node that changed</li>
      * </ul>
      *
      * <p>Additionally, whenever a <tt>style</tt> node or a <tt>link</tt> node with <tt>rel=stylesheet</tt> is added or
      * removed, all elements should be removed from the computed style cache.</p>
      */
     private class DomHtmlAttributeChangeListenerImpl implements DomChangeListener, HtmlAttributeChangeListener,
         NonSerializable {
 
         /**
          * {@inheritDoc}
          */
         public void nodeAdded(final DomChangeEvent event) {
             nodeChanged(event.getChangedNode());
         }
 
         /**
          * {@inheritDoc}
          */
         public void nodeDeleted(final DomChangeEvent event) {
             nodeChanged(event.getChangedNode());
         }
 
         /**
          * {@inheritDoc}
          */
         public void attributeAdded(final HtmlAttributeChangeEvent event) {
             nodeChanged(event.getHtmlElement());
         }
 
         /**
          * {@inheritDoc}
          */
         public void attributeRemoved(final HtmlAttributeChangeEvent event) {
             nodeChanged(event.getHtmlElement());
         }
 
         /**
          * {@inheritDoc}
          */
         public void attributeReplaced(final HtmlAttributeChangeEvent event) {
             nodeChanged(event.getHtmlElement());
         }
 
         private void nodeChanged(final DomNode changed) {
             // If a stylesheet was changed, all of our calculations could be off; clear the cache.
             if (changed instanceof HtmlStyle) {
                 computedStyles_.clear();
                 return;
             }
             if (changed instanceof HtmlLink) {
                 final String rel = ((HtmlLink) changed).getRelAttribute().toLowerCase();
                 if ("stylesheet".equals(rel)) {
                     computedStyles_.clear();
                     return;
                 }
             }
             // Apparently it wasn't a stylesheet that changed; be semi-smart about what we evict and when.
             final Iterator<Map.Entry<Node, ComputedCSSStyleDeclaration>> i = computedStyles_.entrySet().iterator();
             while (i.hasNext()) {
                 final Map.Entry<Node, ComputedCSSStyleDeclaration> entry = i.next();
                 final DomNode node = entry.getKey().getDomNodeOrDie();
                 if (changed == node
                     || changed.getParentNode() == node.getParentNode()
                     || changed.isAncestorOf(node)) {
                     i.remove();
                 }
             }
         }
     }
 
     /**
      * Gets the controllers. The result doesn't currently matter but it is important to return an
      * object as some JavaScript libraries check it.
      * @see <a href="https://developer.mozilla.org/En/DOM/Window.controllers">Mozilla documentation</a>
      * @return some object
      */
     public Object jsxGet_controllers() {
         return controllers_;
     }
 
     /**
      * Sets the controllers.
      * @param value the new value
      */
     public void jsxSet_controllers(final Object value) {
         controllers_ = value;
     }
 
 }
