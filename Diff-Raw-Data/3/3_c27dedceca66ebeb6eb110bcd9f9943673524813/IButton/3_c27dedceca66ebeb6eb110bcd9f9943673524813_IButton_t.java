 /* 
 @ITMillApache2LicenseForJavaFiles@
  */
 
 package com.itmill.toolkit.terminal.gwt.client.ui;
 
 import com.google.gwt.user.client.DOM;
 import com.google.gwt.user.client.Element;
 import com.google.gwt.user.client.Event;
 import com.google.gwt.user.client.ui.Button;
 import com.google.gwt.user.client.ui.ClickListener;
 import com.google.gwt.user.client.ui.Widget;
 import com.itmill.toolkit.terminal.gwt.client.ApplicationConnection;
 import com.itmill.toolkit.terminal.gwt.client.BrowserInfo;
 import com.itmill.toolkit.terminal.gwt.client.ITooltip;
 import com.itmill.toolkit.terminal.gwt.client.Paintable;
 import com.itmill.toolkit.terminal.gwt.client.UIDL;
 import com.itmill.toolkit.terminal.gwt.client.Util;
 
 public class IButton extends Button implements Paintable {
 
     private String width = null;
 
     public static final String CLASSNAME = "i-button";
 
     String id;
 
     ApplicationConnection client;
 
     private Element errorIndicatorElement;
 
     private final Element captionElement = DOM.createSpan();
 
     private Icon icon;
 
     /**
      * Helper flat to handle special-case where the button is moved from under
      * mouse while clicking it. In this case mouse leaves the button without
      * moving.
      */
     private boolean clickPending;
 
     public IButton() {
         setStyleName(CLASSNAME);
 
         DOM.appendChild(getElement(), captionElement);
 
         addClickListener(new ClickListener() {
             public void onClick(Widget sender) {
                 if (id == null || client == null) {
                     return;
                 }
                 /*
                  * TODO isolate workaround. Safari don't always seem to fire
                  * onblur previously focused component before button is clicked.
                  */
                 IButton.this.setFocus(true);
                 client.updateVariable(id, "state", true, true);
                 clickPending = false;
             }
         });
         sinkEvents(ITooltip.TOOLTIP_EVENTS);
         sinkEvents(Event.ONMOUSEDOWN);
         sinkEvents(Event.ONMOUSEUP);
     }
 
     public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
 
         // Ensure correct implementation,
         // but don't let container manage caption etc.
         if (client.updateComponent(this, uidl, false)) {
             return;
         }
 
         // Save details
         this.client = client;
         id = uidl.getId();
 
         // Set text
         setText(uidl.getStringAttribute("caption"));
 
         // handle error
         if (uidl.hasAttribute("error")) {
             if (errorIndicatorElement == null) {
                 errorIndicatorElement = DOM.createDiv();
                 DOM.setElementProperty(errorIndicatorElement, "className",
                         "i-errorindicator");
             }
             DOM.insertChild(getElement(), errorIndicatorElement, 0);
 
             // Fix for IE6, IE7
             if (BrowserInfo.get().isIE()) {
                 DOM.setInnerText(errorIndicatorElement, " ");
             }
 
         } else if (errorIndicatorElement != null) {
             DOM.removeChild(getElement(), errorIndicatorElement);
             errorIndicatorElement = null;
         }
 
         if (uidl.hasAttribute("readonly")) {
             setEnabled(false);
         }
 
         if (uidl.hasAttribute("icon")) {
             if (icon == null) {
                 icon = new Icon(client);
                 DOM.insertChild(getElement(), icon.getElement(), 0);
             }
             icon.setUri(uidl.getStringAttribute("icon"));
         } else {
             if (icon != null) {
                 DOM.removeChild(getElement(), icon.getElement());
                 icon = null;
             }
         }
         if (BrowserInfo.get().isIE7()) {
             if (width.equals("")) {
                 setWidth(getOffsetWidth() + "px");
             }
         }
     }
 
     @Override
     public void setText(String text) {
         DOM.setInnerText(captionElement, text);
     }
 
     @Override
     public void onBrowserEvent(Event event) {
         super.onBrowserEvent(event);
 
         if (DOM.eventGetType(event) == Event.ONLOAD) {
             Util.notifyParentOfSizeChange(this, true);
 
        } else if (DOM.eventGetType(event) == Event.ONMOUSEDOWN
                && event.getButton() == Event.BUTTON_LEFT) {
             clickPending = true;
         } else if (DOM.eventGetType(event) == Event.ONMOUSEMOVE) {
             clickPending = false;
         } else if (DOM.eventGetType(event) == Event.ONMOUSEOUT) {
             if (clickPending) {
                 click();
             }
             clickPending = false;
         }
 
         if (client != null) {
             client.handleTooltipEvent(event, this);
         }
     }
 
     @Override
     public void setWidth(String width) {
         /* Workaround for IE7 button size part 1 (#2014) */
         if (BrowserInfo.get().isIE7() && this.width != null) {
             if (this.width.equals(width)) {
                 return;
             }
 
             if (width == null) {
                 width = "";
             }
         }
 
         this.width = width;
         super.setWidth(width);
 
         /* Workaround for IE7 button size part 2 (#2014) */
         if (BrowserInfo.get().isIE7()) {
             super.setWidth(width);
         }
     }
 
 }
