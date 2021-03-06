 package com.itmill.toolkit.terminal.gwt.client.ui.layout;
 
 import java.util.HashMap;
 import java.util.Map;
 
 import com.google.gwt.dom.client.DivElement;
 import com.google.gwt.dom.client.Document;
 import com.google.gwt.dom.client.Style;
 import com.google.gwt.user.client.Element;
 import com.google.gwt.user.client.ui.ComplexPanel;
 import com.google.gwt.user.client.ui.Widget;
 import com.itmill.toolkit.terminal.gwt.client.ApplicationConnection;
 import com.itmill.toolkit.terminal.gwt.client.Container;
 import com.itmill.toolkit.terminal.gwt.client.Paintable;
 import com.itmill.toolkit.terminal.gwt.client.UIDL;
 import com.itmill.toolkit.terminal.gwt.client.ui.MarginInfo;
 
 public abstract class CellBasedLayout extends ComplexPanel implements Container {
 
     protected Map<Widget, ChildComponentContainer> widgetToComponentContainer = new HashMap<Widget, ChildComponentContainer>();
 
     protected ApplicationConnection client = null;
 
     private DivElement root;
 
     public static final int ORIENTATION_VERTICAL = 0;
     public static final int ORIENTATION_HORIZONTAL = 1;
 
     protected Margins activeMargins = new Margins(0, 0, 0, 0);
     protected MarginInfo activeMarginsInfo = new MarginInfo(-1);
 
     protected boolean spacingEnabled = false;
     protected final Spacing spacingFromCSS = new Spacing(12, 12);
     protected final Spacing activeSpacing = new Spacing(0, 0);
 
     private boolean dynamicWidth;
 
     private boolean dynamicHeight;
 
     private DivElement clearElement;
 
     private String lastStyleName = "";
 
     private boolean marginsNeedsRecalculation = false;
 
     public static class Spacing {
 
         public int hSpacing = 0;
         public int vSpacing = 0;
 
         public Spacing(int hSpacing, int vSpacing) {
             this.hSpacing = hSpacing;
             this.vSpacing = vSpacing;
         }
 
         @Override
         public String toString() {
             return "Spacing [hSpacing=" + hSpacing + ",vSpacing=" + vSpacing
                     + "]";
         }
 
     }
 
     public CellBasedLayout() {
         super();
 
         setElement(Document.get().createDivElement());
         getElement().getStyle().setProperty("overflow", "hidden");
 
         root = Document.get().createDivElement();
         root.getStyle().setProperty("width", "500%");
         getElement().appendChild(root);
 
         clearElement = Document.get().createDivElement();
         Style style = clearElement.getStyle();
         style.setProperty("width", "0px");
         style.setProperty("height", "0px");
         style.setProperty("clear", "both");
         style.setProperty("overflow", "hidden");
         root.appendChild(clearElement);
 
     }
 
     public boolean hasChildComponent(Widget component) {
         return widgetToComponentContainer.containsKey(component);
     }
 
     public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
         this.client = client;
 
         // Only non-cached UIDL:s can introduce changes
         if (uidl.getBooleanAttribute("cached")) {
             return;
         }
 
         /**
          * Margin and spacind detection depends on classNames and must be set
          * before setting size. Here just update the details from UIDL and from
          * overridden setStyleName run actual margin detections.
          */
         updateMarginAndSpacingInfo(uidl);
 
         /*
          * This call should be made first. Ensure correct implementation, handle
          * size etc.
          */
         if (client.updateComponent(this, uidl, true)) {
             return;
         }
 
         handleDynamicDimensions(uidl);
 
     }
 
     protected static String STYLENAME_SPACING = "";
     protected static String STYLENAME_MARGIN_TOP = "";
     protected static String STYLENAME_MARGIN_RIGHT = "";
     protected static String STYLENAME_MARGIN_BOTTOM = "";
     protected static String STYLENAME_MARGIN_LEFT = "";
 
     @Override
     public void setStyleName(String styleName) {
         super.setStyleName(styleName);
 
         if (isAttached() && marginsNeedsRecalculation
                 || !lastStyleName.equals(styleName)) {
             measureMarginsAndSpacing();
             lastStyleName = styleName;
             marginsNeedsRecalculation = false;
         }
 
     }
 
     private void handleDynamicDimensions(UIDL uidl) {
         String w = uidl.hasAttribute("width") ? uidl
                 .getStringAttribute("width") : "";
 
         String h = uidl.hasAttribute("height") ? uidl
                 .getStringAttribute("height") : "";
 
         if (w.equals("")) {
             dynamicWidth = true;
         } else {
             dynamicWidth = false;
         }
 
         if (h.equals("")) {
             dynamicHeight = true;
         } else {
             dynamicHeight = false;
         }
 
     }
 
     protected void addOrMoveChild(ChildComponentContainer childComponent,
             int position) {
         widgetToComponentContainer.put(childComponent.getWidget(),
                 childComponent);
         super.insert(childComponent, (Element) root.cast(), position, true);
 
     }
 
     protected ChildComponentContainer getComponentContainer(Widget child) {
         return widgetToComponentContainer.get(child);
     }
 
     protected boolean isDynamicWidth() {
         return dynamicWidth;
     }
 
     protected boolean isDynamicHeight() {
         return dynamicHeight;
     }
 
     private void updateMarginAndSpacingInfo(UIDL uidl) {
         int bitMask = uidl.getIntAttribute("margins");
         if (activeMarginsInfo.getBitMask() != bitMask) {
             activeMarginsInfo = new MarginInfo(bitMask);
             marginsNeedsRecalculation = true;
         }
         boolean spacing = uidl.getBooleanAttribute("spacing");
         if (spacing != spacingEnabled) {
             marginsNeedsRecalculation = true;
             spacingEnabled = spacing;
         }
     }
 
     protected boolean measureMarginsAndSpacing() {
         if (!isAttached()) {
             return false;
         }
 
         DivElement measurement = Document.get().createDivElement();
         Style style = measurement.getStyle();
         style.setProperty("position", "absolute");
         style.setProperty("top", "0");
         style.setProperty("left", "0");
         style.setProperty("width", "0");
         style.setProperty("height", "0");
         style.setProperty("visibility", "hidden");
         style.setProperty("overflow", "hidden");
         root.appendChild(measurement);
 
         if (spacingEnabled) {
             // Measure spacing (actually CSS padding)
             measurement.setClassName(STYLENAME_SPACING);
            activeSpacing.vSpacing = measurement.getOffsetHeight() - 1;
            activeSpacing.hSpacing = measurement.getOffsetWidth() - 1;
         } else {
             activeSpacing.hSpacing = 0;
             activeSpacing.vSpacing = 0;
         }
 
         DivElement measurement2 = Document.get().createDivElement();
         style = measurement2.getStyle();
         style.setProperty("width", "0px");
         style.setProperty("height", "0px");
         style.setProperty("visibility", "hidden");
         style.setProperty("overflow", "hidden");
 
         measurement.appendChild(measurement2);
 
         String sn = getStylePrimaryName() + "-margin";
 
         if (activeMarginsInfo.hasTop()) {
             sn += " " + STYLENAME_MARGIN_TOP;
         }
         if (activeMarginsInfo.hasBottom()) {
             sn += " " + STYLENAME_MARGIN_BOTTOM;
         }
         if (activeMarginsInfo.hasLeft()) {
             sn += " " + STYLENAME_MARGIN_LEFT;
         }
         if (activeMarginsInfo.hasRight()) {
             sn += " " + STYLENAME_MARGIN_RIGHT;
         }
 
         // Measure top and left margins (actually CSS padding)
         measurement.setClassName(sn);
 
        activeMargins.setMarginTop(measurement2.getOffsetTop()
                - measurement.getOffsetTop());
        activeMargins.setMarginLeft(measurement2.getOffsetLeft()
                - measurement.getOffsetLeft());
         activeMargins.setMarginRight(measurement.getOffsetWidth()
                 - activeMargins.getMarginLeft());
         activeMargins.setMarginBottom(measurement.getOffsetHeight()
                 - activeMargins.getMarginTop());
 
         root.removeChild(measurement);
 
         // apply margin
         style = root.getStyle();
         style.setPropertyPx("marginLeft", activeMargins.getMarginLeft());
         style.setPropertyPx("marginRight", activeMargins.getMarginRight());
         style.setPropertyPx("marginTop", activeMargins.getMarginTop());
         style.setPropertyPx("marginBottom", activeMargins.getMarginBottom());
 
         return true;
     }
 
     protected ChildComponentContainer getFirstChildComponentContainer() {
         int size = getChildren().size();
         if (size < 2) {
             return null;
         }
 
         return (ChildComponentContainer) getChildren().get(0);
     }
 
     protected void removeChildrenAfter(int pos) {
         // Remove all children after position "pos" but leave the clear element
         // in place
 
         int toRemove = getChildren().size() - pos;
         while (toRemove-- > 0) {
             ChildComponentContainer child = (ChildComponentContainer) getChildren()
                     .get(pos);
             widgetToComponentContainer.remove(child.getWidget());
             remove(child);
         }
 
     }
 
     public void replaceChildComponent(Widget oldComponent, Widget newComponent) {
         ChildComponentContainer componentContainer = widgetToComponentContainer
                 .remove(oldComponent);
         if (componentContainer == null) {
             return;
         }
 
         componentContainer.setWidget(newComponent);
         client.unregisterPaintable((Paintable) oldComponent);
         widgetToComponentContainer.put(newComponent, componentContainer);
     }
 
 }
