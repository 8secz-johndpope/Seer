 /* 
 @ITMillApache2LicenseForJavaFiles@
  */
 
 package com.itmill.toolkit.terminal.gwt.client;
 
 import java.util.Iterator;
 
 import com.google.gwt.user.client.Element;
 import com.google.gwt.user.client.ui.HasWidgets;
 import com.google.gwt.user.client.ui.Widget;
 
 public class Util {
 
     /**
      * Helper method for debugging purposes.
      * 
      * Stops execution on firefox browsers on a breakpoint.
      * 
      */
     public static native void browserDebugger()
     /*-{
         if(window.console)
             debugger;
     }-*/;
 
     /**
      * Detects if current browser is IE.
      * 
      * @return true if IE
      */
     public static native boolean isIE()
     /*-{
        var browser=$wnd.navigator.appName;
        if (browser=="Microsoft Internet Explorer") {
            return true;
        }
        return false;
     }-*/;
 
     /**
      * Detects if current browser is IE6.
      * 
      * @return true if IE6
      */
     public static native boolean isIE6()
     /*-{
     var browser=$wnd.navigator.appName;
     if (browser=="Microsoft Internet Explorer") {
     	var ua = navigator.userAgent;
       		var re  = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
       		if (re.exec(ua) != null)
         		rv = parseFloat(RegExp.$1);
         		if(rv == 6) return true;
     }
     return false;
     }-*/;
 
     /**
      * Nulls oncontextmenu function on given element. We need to manually clear
      * context menu events due bad browsers memory leaks, since GWT don't
      * support them.
      * 
      * @param el
      */
     public native static void removeContextMenuEvent(Element el)
     /*-{
       	el.oncontextmenu = null;
     }-*/;
 
     /**
      * Traverses recursively ancestors until ContainerResizedListener child
      * widget is found. They will delegate it futher if needed.
      * 
      * @param container
      */
     public static void runDescendentsLayout(HasWidgets container) {
         final Iterator childWidgets = container.iterator();
         while (childWidgets.hasNext()) {
             final Widget child = (Widget) childWidgets.next();
             if (child instanceof ContainerResizedListener) {
                 ((ContainerResizedListener) child).iLayout();
             } else if (child instanceof HasWidgets) {
                 final HasWidgets childContainer = (HasWidgets) child;
                 runDescendentsLayout(childContainer);
             }
         }
     }
 
     /**
      * Returns closest parent Widget in hierarchy that implements Container
      * interface
      * 
      * @param component
      * @return closest parent Container
      */
     public static Container getParentLayout(Widget component) {
         Widget parent = component.getParent();
         while (parent != null && !(parent instanceof Container)) {
             parent = parent.getParent();
         }
         if (parent != null && ((Container) parent).hasChildComponent(component)) {
             return (Container) parent;
         }
         return null;
     }
 
     public static boolean isIE7() {
         return isIE() && !isIE6();
     }
 }
