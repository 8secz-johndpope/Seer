 /* 
 @ITMillApache2LicenseForJavaFiles@
  */
 
 package com.vaadin.terminal.gwt.client.ui;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Set;
 
 import com.google.gwt.dom.client.Style;
 import com.google.gwt.user.client.DOM;
 import com.google.gwt.user.client.Element;
 import com.google.gwt.user.client.ui.FlowPanel;
 import com.google.gwt.user.client.ui.SimplePanel;
 import com.google.gwt.user.client.ui.Widget;
 import com.vaadin.terminal.gwt.client.ApplicationConnection;
 import com.vaadin.terminal.gwt.client.BrowserInfo;
 import com.vaadin.terminal.gwt.client.Container;
 import com.vaadin.terminal.gwt.client.Paintable;
 import com.vaadin.terminal.gwt.client.RenderSpace;
 import com.vaadin.terminal.gwt.client.StyleConstants;
 import com.vaadin.terminal.gwt.client.UIDL;
 import com.vaadin.terminal.gwt.client.VCaption;
 import com.vaadin.terminal.gwt.client.ValueMap;
 
 public class VCssLayout extends SimplePanel implements Paintable, Container {
     public static final String TAGNAME = "csslayout";
     public static final String CLASSNAME = "v-" + TAGNAME;
 
     private FlowPane panel = new FlowPane();
 
     private Element margin = DOM.createDiv();
 
     private boolean hasHeight;
     private boolean hasWidth;
 
     public VCssLayout() {
         super();
         getElement().appendChild(margin);
         setStyleName(CLASSNAME);
         margin.setClassName(CLASSNAME + "-margin");
         setWidget(panel);
     }
 
     @Override
     protected Element getContainerElement() {
         return margin;
     }
 
     @Override
     public void setWidth(String width) {
         super.setWidth(width);
         // panel.setWidth(width);
         hasWidth = width != null && !width.equals("");
     }
 
     @Override
     public void setHeight(String height) {
         super.setHeight(height);
         // panel.setHeight(height);
         hasHeight = height != null && !height.equals("");
     }
 
     public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
 
         if (client.updateComponent(this, uidl, true)) {
             return;
         }
 
         final VMarginInfo margins = new VMarginInfo(uidl
                 .getIntAttribute("margins"));
         setStyleName(margin, CLASSNAME + "-" + StyleConstants.MARGIN_TOP,
                 margins.hasTop());
         setStyleName(margin, CLASSNAME + "-" + StyleConstants.MARGIN_RIGHT,
                 margins.hasRight());
         setStyleName(margin, CLASSNAME + "-" + StyleConstants.MARGIN_BOTTOM,
                 margins.hasBottom());
         setStyleName(margin, CLASSNAME + "-" + StyleConstants.MARGIN_LEFT,
                 margins.hasLeft());
 
         setStyleName(margin, CLASSNAME + "-" + "spacing", uidl
                 .hasAttribute("spacing"));
         panel.updateFromUIDL(uidl, client);
     }
 
     public boolean hasChildComponent(Widget component) {
         return panel.hasChildComponent(component);
     }
 
     public void replaceChildComponent(Widget oldComponent, Widget newComponent) {
         panel.replaceChildComponent(oldComponent, newComponent);
     }
 
     public void updateCaption(Paintable component, UIDL uidl) {
         panel.updateCaption(component, uidl);
     }
 
     public class FlowPane extends FlowPanel {
 
         private final HashMap<Widget, VCaption> widgetToCaption = new HashMap<Widget, VCaption>();
         private ApplicationConnection client;
 
         public FlowPane() {
             super();
             setStyleName(CLASSNAME + "-container");
         }
 
         public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
 
             // for later requests
             this.client = client;
 
             final ArrayList<Widget> oldWidgets = new ArrayList<Widget>();
             for (final Iterator<Widget> iterator = iterator(); iterator
                     .hasNext();) {
                 oldWidgets.add(iterator.next());
             }
             clear();
 
             ValueMap mapAttribute = null;
             if (uidl.hasAttribute("css")) {
                 mapAttribute = uidl.getMapAttribute("css");
             }
 
             for (final Iterator<Object> i = uidl.getChildIterator(); i
                     .hasNext();) {
                 final UIDL r = (UIDL) i.next();
                 final Paintable child = client.getPaintable(r);
                 if (oldWidgets.contains(child)) {
                     oldWidgets.remove(child);
                 }
 
                 add((Widget) child);
                 if (mapAttribute != null && mapAttribute.containsKey(r.getId())) {
                     String css = null;
                     try {
                         Style style = ((Widget) child).getElement().getStyle();
                         css = mapAttribute.getString(r.getId());
                         String[] cssRules = css.split(";");
                         for (int j = 0; j < cssRules.length; j++) {
                             String[] rule = cssRules[j].split(":");
                             if (rule.length == 0) {
                                 continue;
                             } else {
                                 style.setProperty(
                                         makeCamelCase(rule[0].trim()), rule[1]
                                                 .trim());
                             }
                         }
                     } catch (Exception e) {
                         ApplicationConnection.getConsole().log(
                                 "CssLayout encounterd invalid css string: "
                                         + css);
                     }
                 }
 
                 if (!r.getBooleanAttribute("cached")) {
                     child.updateFromUIDL(r, client);
                 }
             }
 
             // loop oldWidgetWrappers that where not re-attached and unregister
             // them
             for (final Iterator<Widget> it = oldWidgets.iterator(); it
                     .hasNext();) {
                 final Paintable w = (Paintable) it.next();
                 client.unregisterPaintable(w);
                 widgetToCaption.remove(w);
             }
         }
 
         public boolean hasChildComponent(Widget component) {
             return component.getParent() == this;
         }
 
         public void replaceChildComponent(Widget oldComponent,
                 Widget newComponent) {
             VCaption caption = widgetToCaption.get(oldComponent);
             if (caption != null) {
                 remove(caption);
                 widgetToCaption.remove(oldComponent);
             }
             int index = getWidgetIndex(oldComponent);
             if (index >= 0) {
                 remove(oldComponent);
                 insert(newComponent, index);
             }
         }
 
         public void updateCaption(Paintable component, UIDL uidl) {
             VCaption caption = widgetToCaption.get(component);
             if (VCaption.isNeeded(uidl)) {
                 Widget widget = (Widget) component;
                 if (caption == null) {
                     caption = new VCaption(component, client);
                     widgetToCaption.put(widget, caption);
                     insert(caption, getWidgetIndex(widget));
                 } else if (!caption.isAttached()) {
                     insert(caption, getWidgetIndex(widget));
                 }
                 caption.updateCaption(uidl);
             } else if (caption != null) {
                 remove(caption);
                 widgetToCaption.remove(component);
             }
         }
     }
 
     private RenderSpace space;
 
     public RenderSpace getAllocatedSpace(Widget child) {
         if (space == null) {
             space = new RenderSpace(-1, -1) {
                 @Override
                 public int getWidth() {
                     if (BrowserInfo.get().isIE()) {
                         int width = getOffsetWidth();
                         int margins = margin.getOffsetWidth()
                                 - panel.getOffsetWidth();
                         return width - margins;
                     } else {
                         return panel.getOffsetWidth();
                     }
                 }
 
                 @Override
                 public int getHeight() {
                     int height = getOffsetHeight();
                     int margins = margin.getOffsetHeight()
                             - panel.getOffsetHeight();
                     return height - margins;
                 }
             };
         }
         return space;
     }
 
     public boolean requestLayout(Set<Paintable> children) {
         if (hasSize()) {
             return true;
        } else {
            // Size may have changed
            // TODO optimize this: cache size if not fixed, handle both width
            // and height separately
            return false;
         }
     }
 
     private boolean hasSize() {
        return hasWidth && hasHeight;
     }
 
     private static final String makeCamelCase(String cssProperty) {
         // TODO this might be cleaner to implement with regexp
         while (cssProperty.contains("-")) {
             int indexOf = cssProperty.indexOf("-");
             cssProperty = cssProperty.substring(0, indexOf)
                     + String.valueOf(cssProperty.charAt(indexOf + 1))
                             .toUpperCase() + cssProperty.substring(indexOf + 2);
         }
         if ("float".equals(cssProperty)) {
             if (BrowserInfo.get().isIE()) {
                 return "styleFloat";
             } else {
                 return "cssFloat";
             }
         }
         return cssProperty;
     }
 }
