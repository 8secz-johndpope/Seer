 /* 
 @ITMillApache2LicenseForJavaFiles@
  */
 
 package com.itmill.toolkit.terminal.gwt.client.ui;
 
 import java.util.HashSet;
 import java.util.Iterator;
 
 import com.google.gwt.user.client.DOM;
 import com.google.gwt.user.client.Element;
 import com.google.gwt.user.client.Event;
 import com.google.gwt.user.client.Timer;
 import com.google.gwt.user.client.Window;
 import com.google.gwt.user.client.WindowResizeListener;
 import com.google.gwt.user.client.ui.RootPanel;
 import com.google.gwt.user.client.ui.SimplePanel;
 import com.google.gwt.user.client.ui.Widget;
 import com.itmill.toolkit.terminal.gwt.client.ApplicationConnection;
 import com.itmill.toolkit.terminal.gwt.client.BrowserInfo;
 import com.itmill.toolkit.terminal.gwt.client.Paintable;
 import com.itmill.toolkit.terminal.gwt.client.UIDL;
 import com.itmill.toolkit.terminal.gwt.client.Util;
 
 /**
  * 
  */
 public class IView extends SimplePanel implements Paintable,
         WindowResizeListener {
 
     private static final String CLASSNAME = "i-view";
 
     private String theme;
 
     private Paintable layout;
 
     private final HashSet subWindows = new HashSet();
 
     private String id;
 
     private ShortcutActionHandler actionHandler;
 
     /** stored width for IE resize optiomization */
     private int width;
 
     /** stored height for IE resize optiomization */
     private int height;
 
     /**
      * We are postponing resize process with IE. IE bugs with scrollbars in some
      * situations, that causes false onWindowResized calls. With Timer we will
      * give IE some time to decide if it really wants to keep current size
      * (scrollbars).
      */
     private Timer resizeTimer;
 
     public IView(String elementId) {
         super();
         setStyleName(CLASSNAME);
 
         DOM.sinkEvents(getElement(), Event.ONKEYDOWN);
 
         DOM.setElementProperty(getElement(), "tabIndex", "0");
 
         RootPanel.get(elementId).add(this);
 
         Window.addWindowResizeListener(this);
 
         // set focus to iview element by default to listen possible keyboard
         // shortcuts
        if (BrowserInfo.get().isOpera() || BrowserInfo.get().isSafari()
                 && BrowserInfo.get().getWebkitVersion() < 526) {
             // old webkits don't support focusing div elements
             Element fElem = DOM.createInputCheck();
             DOM.setStyleAttribute(fElem, "margin", "0");
             DOM.setStyleAttribute(fElem, "padding", "0");
             DOM.setStyleAttribute(fElem, "border", "0");
             DOM.setStyleAttribute(fElem, "outline", "0");
             DOM.setStyleAttribute(fElem, "width", "1px");
             DOM.setStyleAttribute(fElem, "height", "1px");
             DOM.setStyleAttribute(fElem, "position", "absolute");
             DOM.setStyleAttribute(fElem, "opacity", "0.1");
             DOM.appendChild(getElement(), fElem);
             focus(fElem);
         } else {
             focus(getElement());
         }
 
     }
 
     private static native void focus(Element el)
     /*-{
         try {
             el.focus();
         } catch (e) {
         
         }
     }-*/;
 
     public String getTheme() {
         return theme;
     }
 
     public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
 
         id = uidl.getId();
 
         // Some attributes to note
         theme = uidl.getStringAttribute("theme");
         if (uidl.hasAttribute("style")) {
             addStyleName(uidl.getStringAttribute("style"));
         }
 
         com.google.gwt.user.client.Window.setTitle(uidl
                 .getStringAttribute("caption"));
 
         // Process children
         int childIndex = 0;
 
         // Open URL:s
         while (childIndex < uidl.getChildCount()
                 && "open".equals(uidl.getChildUIDL(childIndex).getTag())) {
             final UIDL open = uidl.getChildUIDL(childIndex);
             final String url = open.getStringAttribute("src");
             final String target = open.getStringAttribute("name");
             if (target == null) {
                 goTo(url);
             } else {
                 // TODO width & height
                 Window.open(url, target != null ? target : null, "");
             }
             childIndex++;
         }
 
         // Draw this application level window
         UIDL childUidl = uidl.getChildUIDL(childIndex);
         final Paintable lo = client.getPaintable(childUidl);
 
         if (layout != null) {
             if (layout != lo) {
                 // remove old
                 client.unregisterPaintable(layout);
                 // add new
                 setWidget((Widget) lo);
                 layout = lo;
             }
         } else {
             setWidget((Widget) lo);
             layout = lo;
         }
         layout.updateFromUIDL(childUidl, client);
 
         // Update subwindows
         final HashSet removedSubWindows = new HashSet(subWindows);
 
         // Open new windows
         while ((childUidl = uidl.getChildUIDL(childIndex++)) != null) {
             if ("window".equals(childUidl.getTag())) {
                 final Paintable w = client.getPaintable(childUidl);
                 if (subWindows.contains(w)) {
                     removedSubWindows.remove(w);
                 } else {
                     subWindows.add(w);
                 }
                 w.updateFromUIDL(childUidl, client);
             } else if ("actions".equals(childUidl.getTag())) {
                 if (actionHandler == null) {
                     actionHandler = new ShortcutActionHandler(id, client);
                 }
                 actionHandler.updateActionMap(childUidl);
             } else if (childUidl.getTag().equals("notifications")) {
                 for (final Iterator it = childUidl.getChildIterator(); it
                         .hasNext();) {
                     final UIDL notification = (UIDL) it.next();
                     String html = "";
                     if (notification.hasAttribute("icon")) {
                         final String parsedUri = client
                                 .translateToolkitUri(notification
                                         .getStringAttribute("icon"));
                         html += "<IMG src=\"" + parsedUri + "\" />";
                     }
                     if (notification.hasAttribute("caption")) {
                         html += "<H1>"
                                 + notification.getStringAttribute("caption")
                                 + "</H1>";
                     }
                     if (notification.hasAttribute("message")) {
                         html += "<p>"
                                 + notification.getStringAttribute("message")
                                 + "</p>";
                     }
 
                     final String style = notification.hasAttribute("style") ? notification
                             .getStringAttribute("style")
                             : null;
                     final int position = notification
                             .getIntAttribute("position");
                     final int delay = notification.getIntAttribute("delay");
                     new Notification(delay).show(html, position, style);
                 }
             }
         }
 
         // Close old windows
         for (final Iterator rem = removedSubWindows.iterator(); rem.hasNext();) {
             final IWindow w = (IWindow) rem.next();
             client.unregisterPaintable(w);
             subWindows.remove(w);
             w.hide();
         }
 
         onWindowResized(Window.getClientWidth(), Window.getClientHeight());
         // IE somehow fails some layout on first run, force layout
         // functions
         // Util.runDescendentsLayout(this);
 
     }
 
     public void onBrowserEvent(Event event) {
         super.onBrowserEvent(event);
         if (DOM.eventGetType(event) == Event.ONKEYDOWN && actionHandler != null) {
             actionHandler.handleKeyboardEvent(event);
             return;
         }
     }
 
     public void onWindowResized(int width, int height) {
         if (Util.isIE()) {
             /*
              * IE will give us some false resized events due bugs with
              * scrollbars. Postponing layout phase to see if size was really
              * changed.
              */
             if (resizeTimer == null) {
                 resizeTimer = new Timer() {
                     public void run() {
                         boolean changed = false;
                         if (IView.this.width != getOffsetWidth()) {
                             IView.this.width = getOffsetWidth();
                             changed = true;
                             ApplicationConnection.getConsole().log(
                                     "window w" + IView.this.width);
                         }
                         if (IView.this.height != getOffsetHeight()) {
                             IView.this.height = getOffsetHeight();
                             changed = true;
                             ApplicationConnection.getConsole().log(
                                     "window h" + IView.this.height);
                         }
                         if (changed) {
                             ApplicationConnection
                                     .getConsole()
                                     .log(
                                             "Running layout functions due window resize");
                             Util.runDescendentsLayout(IView.this);
                         }
                     }
                 };
             } else {
                 resizeTimer.cancel();
             }
             resizeTimer.schedule(200);
         } else {
             // temporary set overflow hidden, not to let scrollbars disturb
             // layout functions
             final String overflow = DOM.getStyleAttribute(getElement(),
                     "overflow");
             DOM.setStyleAttribute(getElement(), "overflow", "hidden");
             ApplicationConnection.getConsole().log(
                     "Running layout functions due window resize");
             Util.runDescendentsLayout(this);
             DOM.setStyleAttribute(getElement(), "overflow", overflow);
         }
     }
 
     public native static void goTo(String url)
     /*-{
        $wnd.location = url;
      }-*/;
 
 }
