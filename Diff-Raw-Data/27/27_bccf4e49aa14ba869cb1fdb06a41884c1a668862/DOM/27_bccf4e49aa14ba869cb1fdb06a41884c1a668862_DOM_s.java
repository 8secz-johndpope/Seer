 /*
  * Copyright 2007 Google Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 package com.google.gwt.user.client;
 
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
 import com.google.gwt.user.client.impl.DOMImpl;
 
 import java.util.ArrayList;
 
 /**
  * This class provides a set of static methods that allow you to manipulate the
  * browser's Document Object Model (DOM). It contains methods for manipulating
  * both {@link com.google.gwt.user.client.Element elements} and
  * {@link com.google.gwt.user.client.Event events}.
  */
 public class DOM {
 
   private static DOMImpl impl;
   private static Element sCaptureElem;
 
   //<BrowserEventPreview>
   private static ArrayList sEventPreviewStack = new ArrayList();
 
   static {
     impl = (DOMImpl) GWT.create(DOMImpl.class);
     impl.init();
   }
 
   /**
    * Adds an event preview to the preview stack. As long as this preview remains
    * on the top of the stack, it will receive all events before they are fired
    * to their listeners. Note that the event preview will receive <u>all </u>
    * events, including those received due to bubbling, whereas normal event
    * handlers only receive explicitly sunk events.
    *
    * @param preview the event preview to be added to the stack.
    */
   public static void addEventPreview(EventPreview preview) {
     // Add the event preview to the stack. It will automatically
     // begin receiving events.
     sEventPreviewStack.add(preview);
   }
 
   /**
    * Appends one element to another's list of children.
    *
    * @param parent the parent element
    * @param child its new child
    */
   public static void appendChild(Element parent, Element child) {
     impl.appendChild(parent, child);
   }
 
   /**
    * Compares two elements for equality (note that reference equality is not
    * sufficient to determine equality among elements on most browsers).
    *
    * @param elem1 the first element to be compared
    * @param elem2 the second element to be compared
    * @return <code>true</code> if they are in fact the same element
    * @see #isOrHasChild(Element, Element)
    */
   public static boolean compare(Element elem1, Element elem2) {
     return impl.compare(elem1, elem2);
   }
 
   /**
    * Creates an HTML A element.
    *
    * @return the newly-created element
    */
   public static Element createAnchor() {
     return impl.createElement("A");
   }
 
   /**
    * Creates an HTML BUTTON element.
    *
    * @return the newly-created element
    */
   public static Element createButton() {
     return impl.createElement("button");
   }
 
   /**
    * Creates an HTML CAPTION element.
    *
    * @return the newly-created element
    */
   public static Element createCaption() {
     return impl.createElement("caption");
   }
 
   /**
    * Creates an HTML COL element.
    *
    * @return the newly-created element
    */
   public static Element createCol() {
     return impl.createElement("col");
   }
 
   /**
    * Creates an HTML COLGROUP element.
    *
    * @return the newly-created element
    */
   public static Element createColGroup() {
     return impl.createElement("colgroup");
   }
 
   /**
    * Creates an HTML DIV element.
    *
    * @return the newly-created element
    */
   public static Element createDiv() {
     return impl.createElement("div");
   }
 
   /**
    * Creates an HTML element.
    *
    * @param tagName the HTML tag of the element to be created
    * @return the newly-created element
    */
   public static Element createElement(String tagName) {
     return impl.createElement(tagName);
   }
 
   /**
    * Creates an HTML FIELDSET element.
    *
    * @return the newly-created element
    */
   public static Element createFieldSet() {
     return impl.createElement("fieldset");
   }
 
   /**
    * Creates an HTML FORM element.
    *
    * @return the newly-created element
    */
   public static Element createForm() {
     return impl.createElement("form");
   }
 
   /**
    * Creates an HTML IFRAME element.
    *
    * @return the newly-created element
    */
   public static Element createIFrame() {
     return impl.createElement("iframe");
   }
 
   /**
    * Creates an HTML IMG element.
    *
    * @return the newly-created element
    */
   public static Element createImg() {
     return impl.createElement("img");
   }
 
   /**
    * Creates an HTML INPUT type='CHECK' element.
    *
    * @return the newly-created element
    */
   public static Element createInputCheck() {
     return impl.createInputElement("checkbox");
   }
 
   /**
    * Creates an HTML INPUT type='PASSWORD' element.
    *
    * @return the newly-created element
    */
   public static Element createInputPassword() {
     return impl.createInputElement("password");
   }
 
   /**
    * Creates an HTML INPUT type='RADIO' element.
    *
    * @param group the name of the group with which this radio button will be
    *          associated
    * @return the newly-created element
    */
   public static Element createInputRadio(String group) {
     return impl.createInputRadioElement(group);
   }
 
   /**
    * Creates an HTML INPUT type='TEXT' element.
    *
    * @return the newly-created element
    */
   public static Element createInputText() {
     return impl.createInputElement("text");
   }
 
   /**
    * Creates an HTML LABEL element.
    *
    * @return the newly-created element
    */
   public static Element createLabel() {
     return impl.createElement("label");
   }
 
   /**
    * Creates an HTML LEGEND element.
    *
    * @return the newly-created element
    */
   public static Element createLegend() {
     return impl.createElement("legend");
   }
 
   /**
    * Creates an HTML OPTIONS element.
    *
    * @return the newly-created element
    */
   public static Element createOptions() {
     return impl.createElement("options");
   }
 
   /**
    * Creates a single-selection HTML SELECT element. Equivalent to 
    * <pre>
    * createSelect(false)
    * </pre>
    *
    * @return the newly-created element
    */
   public static Element createSelect() {
     return DOM.createSelect(false);
   }
 
   /**
    * Creates an HTML SELECT element.
    *
    * @return the newly-created element
    */
   public static Element createSelect(boolean multiple) {
     return impl.createSelectElement(multiple);
   }
 
   /**
    * Creates an HTML SPAN element.
    *
    * @return the newly-created element
    */
   public static Element createSpan() {
     return impl.createElement("span");
   }
 
   /**
    * Creates an HTML TABLE element.
    *
    * @return the newly-created element
    */
   public static Element createTable() {
     return impl.createElement("table");
   }
 
   /**
    * Creates an HTML TBODY element.
    *
    * @return the newly-created element
    */
   public static Element createTBody() {
     return impl.createElement("tbody");
   }
 
   /**
    * Creates an HTML TD element.
    *
    * @return the newly-created element
    */
   public static Element createTD() {
     return impl.createElement("td");
   }
 
   /**
    * Creates an HTML TEXTAREA element.
    *
    * @return the newly-created element
    */
   public static Element createTextArea() {
     return impl.createElement("textarea");
   }
 
   /**
    * Creates an HTML TFOOT element.
    *
    * @return the newly-created element
    */
   public static Element createTFoot() {
     return impl.createElement("tfoot");
   }
 
   /**
    * Creates an HTML TH element.
    *
    * @return the newly-created element
    */
   public static Element createTH() {
     return impl.createElement("th");
   }
 
   /**
    * Creates an HTML THEAD element.
    *
    * @return the newly-created element
    */
   public static Element createTHead() {
     return impl.createElement("thead");
   }
 
   /**
    * Creates an HTML TR element.
    *
    * @return the newly-created element
    */
   public static Element createTR() {
     return impl.createElement("tr");
   }
 
   /**
    * Cancels bubbling for the given event. This will stop the event from being
    * propagated to parent elements.
    *
    * @param evt the event on which to cancel bubbling
    * @param cancel <code>true</code> to cancel bubbling
    */
   public static void eventCancelBubble(Event evt, boolean cancel) {
     impl.eventCancelBubble(evt, cancel);
   }
 
   /**
    * Gets whether the ALT key was depressed when the given event occurred.
    *
    * @param evt the event to be tested
    * @return <code>true</code> if ALT was depressed when the event occurred
    */
   public static boolean eventGetAltKey(Event evt) {
     return impl.eventGetAltKey(evt);
   }
 
   /**
    * Gets the mouse buttons that were depressed when the given event occurred.
    *
    * @param evt the event to be tested
    * @return a bit-field, defined by {@link Event#BUTTON_LEFT},
    *         {@link Event#BUTTON_MIDDLE}, and {@link Event#BUTTON_RIGHT}
    */
   public static int eventGetButton(Event evt) {
     return impl.eventGetButton(evt);
   }
 
   /**
    * Gets the mouse x-position within the browser window's client area.
    *
    * @param evt the event to be tested
    * @return the mouse x-position
    */
   public static int eventGetClientX(Event evt) {
     return impl.eventGetClientX(evt);
   }
 
   /**
    * Gets the mouse y-position within the browser window's client area.
    *
    * @param evt the event to be tested
    * @return the mouse y-position
    */
   public static int eventGetClientY(Event evt) {
     return impl.eventGetClientY(evt);
   }
 
   /**
    * Gets whether the CTRL key was depressed when the given event occurred.
    *
    * @param evt the event to be tested
    * @return <code>true</code> if CTRL was depressed when the event occurred
    */
   public static boolean eventGetCtrlKey(Event evt) {
     return impl.eventGetCtrlKey(evt);
   }
 
   /**
    * Gets the current target element of the given event. This is the element
    * whose listener fired last, not the element which fired the event
    * initially.
    *
    * @param evt the event
    * @return the event's current target element
    * @see DOM#eventGetTarget(Event)
    */
   public static Element eventGetCurrentTarget(Event evt) {
     return impl.eventGetCurrentTarget(evt);
   }
 
   /**
    * Gets the element from which the mouse pointer was moved (only valid for
    * {@link Event#ONMOUSEOVER}).
    *
    * @param evt the event to be tested
    * @return the element from which the mouse pointer was moved
    */
   public static Element eventGetFromElement(Event evt) {
     return impl.eventGetFromElement(evt);
   }
 
   /**
    * Gets the key code associated with this event.
    *
    * <p>
    * For {@link Event#ONKEYPRESS}, this method returns the Unicode value of the
    * character generated. For {@link Event#ONKEYDOWN} and {@link Event#ONKEYUP},
    * it returns the code associated with the physical key.
    * </p>
    *
    * @param evt the event to be tested
    * @return the Unicode character or key code.
    * @see com.google.gwt.user.client.ui.KeyboardListener
    */
   public static int eventGetKeyCode(Event evt) {
     return impl.eventGetKeyCode(evt);
   }
 
   /**
    * Gets whether the META key was depressed when the given event occurred.
    *
    * @param evt the event to be tested
    * @return <code>true</code> if META was depressed when the event occurred
    */
   public static boolean eventGetMetaKey(Event evt) {
     return impl.eventGetMetaKey(evt);
   }
 
   /**
    * Gets the velocity of the mouse wheel associated with the event along the
    * Y axis.
    * <p>
    * The velocity of the event is an artifical measurement for relative
    * comparisons of wheel activity.  It is affected by some non-browser
    * factors, including choice of input hardware and mouse acceleration
    * settings.  The sign of the velocity measurement agrees with the screen
    * coordinate system; negative values are towards the origin and positive
    * values are away from the origin.  Standard scrolling speed is approximately
    * ten units per event.
    * </p>
    *
    * @param evt the event to be examined.
    * @return The velocity of the mouse wheel.
    */
   public static int eventGetMouseWheelVelocityY(Event evt) {
     return impl.eventGetMouseWheelVelocityY(evt);
   }
 
   /**
    * Gets the key-repeat state of this event.
    *
    * @param evt the event to be tested
    * @return <code>true</code> if this key event was an auto-repeat
    */
   public static boolean eventGetRepeat(Event evt) {
     return impl.eventGetRepeat(evt);
   }
 
   /**
    * Gets the mouse x-position on the user's display.
    *
    * @param evt the event to be tested
    * @return the mouse x-position
    */
   public static int eventGetScreenX(Event evt) {
     return impl.eventGetScreenX(evt);
   }
 
   /**
    * Gets the mouse y-position on the user's display.
    *
    * @param evt the event to be tested
    * @return the mouse y-position
    */
   public static int eventGetScreenY(Event evt) {
     return impl.eventGetScreenY(evt);
   }
 
   /**
    * Gets whether the shift key was depressed when the given event occurred.
    *
    * @param evt the event to be tested
    * @return <code>true</code> if shift was depressed when the event occurred
    */
   public static boolean eventGetShiftKey(Event evt) {
     return impl.eventGetShiftKey(evt);
   }
 
   /**
    * Returns the element that was the actual target of the given event.
    *
    * @param evt the event to be tested
    * @return the target element
    */
   public static Element eventGetTarget(Event evt) {
     return impl.eventGetTarget(evt);
   }
 
   /**
    * Gets the element to which the mouse pointer was moved (only valid for
    * {@link Event#ONMOUSEOUT}).
    *
    * @param evt the event to be tested
    * @return the element to which the mouse pointer was moved
    */
   public static Element eventGetToElement(Event evt) {
     return impl.eventGetToElement(evt);
   }
 
   /**
    * Gets the enumerated type of this event (as defined in {@link Event}).
    *
    * @param evt the event to be tested
    * @return the event's enumerated type
    */
   public static int eventGetType(Event evt) {
     return impl.eventGetTypeInt(evt);
   }
 
   /**
    * Gets the type of the given event as a string.
    *
    * @param evt the event to be tested
    * @return the event's type name
    */
   public static String eventGetTypeString(Event evt) {
     return impl.eventGetType(evt);
   }
 
   /**
    * Prevents the browser from taking its default action for the given event.
    *
    * @param evt the event whose default action is to be prevented
    */
   public static void eventPreventDefault(Event evt) {
     impl.eventPreventDefault(evt);
   }
 
   /**
    * Sets the key code associated with the given keyboard event.
    *
    * @param evt the event whose key code is to be set
    * @param key the new key code
    */
   public static void eventSetKeyCode(Event evt, char key) {
     impl.eventSetKeyCode(evt, key);
   }
 
   /**
    * Returns a stringized version of the event. This string is for debugging
    * purposes and will NOT be consistent on different browsers.
    *
    * @param evt the event to stringize
    * @return a string form of the event
    */
   public static String eventToString(Event evt) {
     return impl.eventToString(evt);
   }
 
   /**
    * Gets an element's absolute left coordinate in the document's coordinate
    * system.
    *
    * @param elem the element to be measured
    * @return the element's absolute left coordinate
    */
   public static int getAbsoluteLeft(Element elem) {
     return impl.getAbsoluteLeft(elem);
   }
 
   /**
    * Gets an element's absolute top coordinate in the document's coordinate
    * system.
    *
    * @param elem the element to be measured
    * @return the element's absolute top coordinate
    */
   public static int getAbsoluteTop(Element elem) {
     return impl.getAbsoluteTop(elem);
   }
 
   /**
    * Gets any named attribute from an element, as a string.
    *
    * @param elem the element whose attribute is to be retrieved
    * @param attr the name of the attribute
    * @return the attribute's value
    * @deprecated Use the more appropriately named
    * {@link #getElementProperty(Element, String)} instead.
    */
   public static String getAttribute(Element elem, String attr) {
     return getElementProperty(elem, attr);
   }
 
   /**
    * Gets a boolean attribute on the given element.
    *
    * @param elem the element whose attribute is to be set
    * @param attr the name of the attribute to be set
    * @return the attribute's value as a boolean
    * @deprecated Use the more appropriately named
    * {@link #getElementPropertyBoolean(Element, String)} instead.
    */
   public static boolean getBooleanAttribute(Element elem, String attr) {
     return getElementPropertyBoolean(elem, attr);
   }
 
   /**
    * Gets the element that currently has mouse capture.
    *
    * @return a handle to the capture element, or <code>null</code> if none
    *         exists
    */
   public static Element getCaptureElement() {
     return sCaptureElem;
   }
 
   /**
    * Gets an element's n-th child element.
    *
    * @param parent the element whose child is to be retrieved
    * @param index the index of the child element
    * @return the n-th child element
    */
   public static Element getChild(Element parent, int index) {
     return impl.getChild(parent, index);
   }
 
   /**
    * Gets the number of child elements present in a given parent element.
    *
    * @param parent the element whose children are to be counted
    * @return the number of children
    */
   public static int getChildCount(Element parent) {
     return impl.getChildCount(parent);
   }
 
   /**
    * Gets the index of a given child element within its parent.
    *
    * @param parent the parent element
    * @param child the child element
    * @return the child's index within its parent, or <code>-1</code> if it is
    *         not a child of the given parent
    */
   public static int getChildIndex(Element parent, Element child) {
     return impl.getChildIndex(parent, child);
   }
 
   /**
    * Gets the named attribute from the element.
    *
    * @param elem the element whose property is to be retrieved
    * @param attr the name of the attribute
    * @return the value of the attribute
    */
   public static String getElementAttribute(Element elem, String attr) {
      return impl.getElementAttribute(elem, attr);
   }
 
   /**
    * Gets the element associated with the given unique id within the entire
    * document.
    *
    * @param id the id whose associated element is to be retrieved
    * @return the associated element, or <code>null</code> if none is found
    */
   public static Element getElementById(String id) {
     return impl.getElementById(id);
   }
   /**
    * Gets any named property from an element, as a string.
    *
    * @param elem the element whose property is to be retrieved
    * @param prop the name of the property
    * @return the property's value
    */
   public static String getElementProperty(Element elem, String prop) {
      return impl.getElementProperty(elem, prop);
   }
 
   /**
    * Gets any named property from an element, as a boolean.
    *
    * @param elem the element whose property is to be retrieved
    * @param prop the name of the property
    * @return the property's value as a boolean
    */
   public static boolean getElementPropertyBoolean(Element elem, String prop) {
      return impl.getElementPropertyBoolean(elem, prop);
   }
 
   /**
    * Gets any named property from an element, as an int.
    *
    * @param elem the element whose property is to be retrieved
    * @param prop the name of the property
    * @return the property's value as an int
    */
   public static int getElementPropertyInt(Element elem, String prop) {
      return impl.getElementPropertyInt(elem, prop);
   }
 
   /**
    * Gets the current set of events sunk by a given element.
    *
    * @param elem the element whose events are to be retrieved
    * @return a bitfield describing the events sunk on this element (its possible
    *         values are described in {@link Event})
    */
   public static int getEventsSunk(Element elem) {
     return impl.getEventsSunk(elem);
   }
 
   /**
    * Gets the first child element of the given element.
    *
    * @param elem the element whose child is to be retrieved
    * @return the child element
    */
   public static Element getFirstChild(Element elem) {
     return impl.getFirstChild(elem);
   }
 
   /**
    * Gets the src attribute of an img element. This method is paired with
    * {@link #setImgSrc(Element, String)} so that it always returns the correct
    * url.
    *
    * @param img a non-null img whose src attribute is to be read.
    * @return the src url of the img
    */
   public static String getImgSrc(Element img) {
     return impl.getImgSrc(img);
   }
 
   /**
    * Gets an HTML representation of an element's children.
    *
    * @param elem the element whose HTML is to be retrieved
    * @return the HTML representation of the element's children
    */
   public static String getInnerHTML(Element elem) {
     return impl.getInnerHTML(elem);
   }
 
   /**
    * Gets the text contained within an element. If the element has child
    * elements, only the text between them will be retrieved.
    *
    * @param elem the element whose inner text is to be retrieved
    * @return the text inside this element
    */
   public static String getInnerText(Element elem) {
     return impl.getInnerText(elem);
   }
 
   /**
    * Gets an integer attribute on a given element.
    *
    * @param elem the element whose attribute is to be retrieved
    * @param attr the name of the attribute to be retrieved
    * @return the attribute's value as an integer
    * @deprecated Use the more appropriately named
    * {@link #getElementPropertyInt(Element, String)} instead.
    */
   public static int getIntAttribute(Element elem, String attr) {
     return getElementPropertyInt(elem, attr);
   }
 
   /**
    * Gets an integer attribute on a given element's style.
    *
    * @param elem the element whose style attribute is to be retrieved
    * @param attr the name of the attribute to be retrieved
    * @return the style attribute's value as an integer
    */
   public static int getIntStyleAttribute(Element elem, String attr) {
     return impl.getIntStyleAttribute(elem, attr);
   }
 
   /**
    * Gets an element's next sibling element.
    *
    * @param elem the element whose sibling is to be retrieved
    * @return the sibling element
    */
   public static Element getNextSibling(Element elem) {
     return impl.getNextSibling(elem);
   }
 
   /**
    * Gets an element's parent element.
    *
    * @param elem the element whose parent is to be retrieved
    * @return the parent element
    */
   public static Element getParent(Element elem) {
     return impl.getParent(elem);
   }
 
   /**
    * Gets an attribute of the given element's style.
    *
    * @param elem the element whose style attribute is to be retrieved
    * @param attr the name of the style attribute to be retrieved
    * @return the style attribute's value
    */
   public static String getStyleAttribute(Element elem, String attr) {
     return impl.getStyleAttribute(elem, attr);
   }
 
   /**
    * Inserts an element as a child of the given parent element, before another
    * child of that parent.
    * 
    * @param parent the parent element
    * @param child the child element to add to <code>parent</code>
    * @param before an existing child element of <code>parent</code> before
    *          which <code>child</code> will be inserted
    */
   public static void insertBefore(Element parent, Element child, Element before) {
     impl.insertBefore(parent, child, before);
   }
 
   /**
    * Inserts an element as a child of the given parent element.
    *
    * @param parent the parent element
    * @param child the child element to add to <code>parent</code>
    * @param index the index before which the child will be inserted (any value
    *          greater than the number of existing children will cause the child
    *          to be appended)
    */
   public static void insertChild(Element parent, Element child, int index) {
     impl.insertChild(parent, child, index);
   }
 
   /**
    * Creates an <code>&lt;option&gt;</code> element and inserts it as a child
    * of the specified <code>&lt;select&gt;</code> element.
    * 
    * @param select the <code>&lt;select&gt;</code> element
    * @param item the text of the new item; cannot be <code>null</code>
    * @param value the <code>value</code> attribute for the new
    *          <code>&lt;option&gt;</code>; cannot be <code>null</code>
    * @param index the index at which to insert the child
    */
   public static void insertListItem(Element select, String item, String value,
       int index) {
     impl.insertListItem(select, item, value, index);
   }
 
   /**
    * Determine whether one element is equal to, or the child of, another.
    *
    * @param parent the potential parent element
    * @param child the potential child element
    * @return <code>true</code> if the relationship holds
    * @see #compare(Element, Element)
    */
   public static boolean isOrHasChild(Element parent, Element child) {
     return impl.isOrHasChild(parent, child);
   }
 
   /**
    * Releases mouse capture on the given element. Calling this method has no
    * effect if the element does not currently have mouse capture.
    *
    * @param elem the element to release capture
    * @see #setCapture(Element)
    */
   public static void releaseCapture(Element elem) {
     if ((sCaptureElem != null) && compare(elem, sCaptureElem)) {
       sCaptureElem = null;
     }
     impl.releaseCapture(elem);
   }
 
   /**
    * Removes a child element from the given parent element.
    *
    * @param parent the parent element
    * @param child the child element to be removed
    */
   public static void removeChild(Element parent, Element child) {
     impl.removeChild(parent, child);
   }
 
   /**
    * Removes the named attribute from the given element.
    *
    * @param elem the element whose attribute is to be removed
    * @param attr the name of the element to remove
    */
   public static void removeElementAttribute(Element elem, String attr) {
     impl.removeElementAttribute(elem, attr);
   }
 
   /**
    * Removes an element from the preview stack. This element will no longer
    * capture events, though any preview underneath it will begin to do so.
    *
    * @param preview the event preview to be removed from the stack
    */
   public static void removeEventPreview(EventPreview preview) {
     // Remove the event preview from the stack. If it was on top,
     // any preview underneath it will automatically begin to
     // receive events.
     sEventPreviewStack.remove(preview);
   }
 
   /**
    * Scrolls the given element into view.
    *
    * <p>
    * This method crawls up the DOM hierarchy, adjusting the scrollLeft and
    * scrollTop properties of each scrollable element to ensure that the
    * specified element is completely in view. It adjusts each scroll position by
    * the minimum amount necessary.
    * </p>
    *
    * @param elem the element to be made visible
    */
   public static void scrollIntoView(Element elem) {
     impl.scrollIntoView(elem);
   }
 
   /**
    * Sets an attribute on the given element.
    *
    * @param elem the element whose attribute is to be set
    * @param attr the name of the attribute to be set
    * @param value the new attribute value
    * @deprecated Use the more appropriately named
    * {@link #setElementProperty(Element, String, String)} instead.
    */
   public static void setAttribute(Element elem, String attr, String value) {
     setElementProperty(elem, attr, value);
   }
 
   /**
    * Sets a boolean attribute on the given element.
    *
    * @param elem the element whose attribute is to be set
    * @param attr the name of the attribute to be set
    * @param value the attribute's new boolean value
    * @deprecated Use the more appropriately named
    * {@link #setElementPropertyBoolean(Element, String, boolean)} instead.
    */
   public static void setBooleanAttribute(Element elem, String attr,
       boolean value) {
     setElementPropertyBoolean(elem, attr, value);
   }
 
   /**
    * Sets mouse-capture on the given element. This element will directly receive
    * all mouse events until {@link #releaseCapture(Element)} is called on it.
    *
    * @param elem the element on which to set mouse capture
    */
   public static void setCapture(Element elem) {
     sCaptureElem = elem;
     impl.setCapture(elem);
   }
 
   /**
    * Sets an attribute on a given element.
    *
    * @param elem element whose attribute is to be set
    * @param attr the name of the attribute
    * @param value the value to which the attribute should be set
    */
   public static void setElementAttribute(Element elem, String attr,
       String value) {
     impl.setElementAttribute(elem, attr, value);
   }
 
   /**
    * Sets a property on the given element.
    *
    * @param elem the element whose property is to be set
    * @param prop the name of the property to be set
    * @param value the new property value
    */
   public static void setElementProperty(Element elem, String prop,
       String value) {
     impl.setElementProperty(elem, prop, value);
   }
 
   /**
    * Sets a boolean property on the given element.
    *
    * @param elem the element whose property is to be set
    * @param prop the name of the property to be set
    * @param value the new property value as a boolean
    */
   public static void setElementPropertyBoolean(Element elem, String prop,
       boolean value) {
     impl.setElementPropertyBoolean(elem, prop, value);
   }
 
   /**
    * Sets an int property on the given element.
    *
    * @param elem the element whose property is to be set
    * @param prop the name of the property to be set
    * @param value the new property value as an int
    */
   public static void setElementPropertyInt(Element elem, String prop,
       int value) {
     impl.setElementPropertyInt(elem, prop, value);
   }
 
   /**
    * Sets the {@link EventListener} to receive events for the given element.
    * Only one such listener may exist for a single element.
    *
    * @param elem the element whose listener is to be set
    * @param listener the listener to receive {@link Event events}
    */
   public static void setEventListener(Element elem, EventListener listener) {
     impl.setEventListener(elem, listener);
   }
 
   /**
    * Sets the src attribute of an img element. This method ensures that imgs
    * only ever have their contents requested one single time from the server.
    *
    * @param img a non-null img whose src attribute will be set.
    * @param src a non-null url for the img
    */
   public static void setImgSrc(Element img, String src) {
     impl.setImgSrc(img, src);
   }
 
   /**
    * Sets the HTML contained within an element.
    *
    * @param elem the element whose inner HTML is to be set
    * @param html the new html
    */
   public static void setInnerHTML(Element elem, String html) {
     impl.setInnerHTML(elem, html);
   }
 
   /**
    * Sets the text contained within an element. If the element already has
    * children, they will be destroyed.
    *
    * @param elem the element whose inner text is to be set
    * @param text the new text
    */
   public static void setInnerText(Element elem, String text) {
     impl.setInnerText(elem, text);
   }
 
   /**
    * Sets an integer attribute on the given element.
    *
    * @param elem the element whose attribute is to be set
    * @param attr the name of the attribute to be set
    * @param value the attribute's new integer value
    * @deprecated Use the more appropriately named
    * {@link #setElementPropertyInt(Element, String, int)} instead.
    */
   public static void setIntAttribute(Element elem, String attr, int value) {
     setElementPropertyInt(elem, attr, value);
   }
 
   /**
    * Sets an integer attribute on the given element's style.
    *
    * @param elem the element whose style attribute is to be set
    * @param attr the name of the style attribute to be set
    * @param value the style attribute's new integer value
    */
   public static void setIntStyleAttribute(Element elem, String attr,
       int value) {
     impl.setIntStyleAttribute(elem, attr, value);
   }
 
   /**
    * Sets the option text of the given select object.
    *
    * @param select the select object whose option text is being set
    * @param text the text to set
    * @param index the index of the option whose text should be set
    */
   public static void setOptionText(Element select, String text, int index) {
     impl.setOptionText(select, text, index);
   }
 
   /**
    * Sets an attribute on the given element's style.
    *
    * @param elem the element whose style attribute is to be set
    * @param attr the name of the style attribute to be set
    * @param value the style attribute's new value
    */
   public static void setStyleAttribute(Element elem, String attr,
       String value) {
     impl.setStyleAttribute(elem, attr, value);
   }
 
   /**
    * Sets the current set of events sunk by a given element. These events will
    * be fired to the nearest {@link EventListener} specified on any of the
    * element's parents.
    *
    * @param elem the element whose events are to be retrieved
    * @param eventBits a bitfield describing the events sunk on this element (its
    *          possible values are described in {@link Event})
    */
   public static void sinkEvents(Element elem, int eventBits) {
     impl.sinkEvents(elem, eventBits);
   }
 
   /**
    * Returns a stringized version of the element. This string is for debugging
    * purposes and will NOT be consistent on different browsers.
    *
    * @param elem the element to stringize
    * @return a string form of the element
    */
   public static String toString(Element elem) {
     return impl.toString(elem);
   }
   
   /**
    * Gets the height of the browser window's client area excluding the
    * scroll bar.
    * 
    * @return the window's client height
    */
   public static int windowGetClientHeight() {
     return impl.windowGetClientHeight();
   };
 
   /**
    * Gets the width of the browser window's client area excluding the
    * vertical scroll bar.
    * 
    * @return the window's client width
    */
   public static int windowGetClientWidth() {
     return impl.windowGetClientWidth();
   }
   
   /**
    * This method is called directly by native code when any event is fired.
    *
    * @param evt the handle to the event being fired.
    * @param elem the handle to the element that received the event.
    * @param listener the listener associated with the element that received the
    *          event.
    */
   static void dispatchEvent(Event evt, Element elem, EventListener listener) {
     UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
     if (handler != null) {
       dispatchEventAndCatch(evt, elem, listener, handler);
     } else {
       dispatchEventImpl(evt, elem, listener);
     }
   }
 
   /**
    * This method is called directly by native code when event preview is being
    * used.
    *
    * @param evt a handle to the event being previewed
    * @return <code>false</code> to cancel the event
    */
   static boolean previewEvent(Event evt) {
     // If event previews are present, redirect events to the topmost of them.
     boolean ret = true;
     if (sEventPreviewStack.size() > 0) {
       EventPreview preview =
         (EventPreview) sEventPreviewStack.get(sEventPreviewStack.size() - 1);
       if (!(ret = preview.onEventPreview(evt))) {
         // If the preview cancels the event, stop it from bubbling and
         // performing its default action.
         eventCancelBubble(evt, true);
         eventPreventDefault(evt);
       }
     }
 
     return ret;
   }
 
   private static void dispatchEventAndCatch(Event evt, Element elem,
       EventListener listener, UncaughtExceptionHandler handler) {
     try {
       dispatchEventImpl(evt, elem, listener);
     } catch (Throwable e) {
       handler.onUncaughtException(e);
     }
   }
 
   private static void dispatchEventImpl(Event evt, Element elem,
       EventListener listener) {
     // If this element has capture...
     if (elem == sCaptureElem) {
       // ... and it's losing capture, clear sCaptureElem.
       if (eventGetType(evt) == Event.ONLOSECAPTURE) {
         sCaptureElem = null;
       }
     }
 
    // Pass the event to the listener.
    listener.onBrowserEvent(evt);
   }
 }
