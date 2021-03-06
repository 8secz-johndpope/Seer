 /*
  * OpenFaces - JSF Component Library 2.0
  * Copyright (C) 2007-2009, TeamDev Ltd.
  * licensing@openfaces.org
  * Unless agreed in writing the contents of this file are subject to
  * the GNU Lesser General Public License Version 2.1 (the "LGPL" License).
  * This library is distributed in the hope that it will be useful, but
  * WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * Please visit http://openfaces.org/licensing/ for more details.
  */
 package org.openfaces.renderkit.action;
 
 import org.openfaces.component.action.MenuItem;
 import org.openfaces.component.action.PopupMenu;
 import org.openfaces.renderkit.RendererBase;
 import org.openfaces.util.ResourceUtil;
 import org.openfaces.util.StyleGroup;
 import org.openfaces.util.StyleUtil;
 
 import javax.faces.component.UIComponent;
 import javax.faces.context.FacesContext;
 import javax.faces.context.ResponseWriter;
 import javax.faces.event.ActionEvent;
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 /**
  * @author Vladimir Kurganov
  */
 public class MenuItemRenderer extends RendererBase {
 
     private static final String DEFAULT_LIST_ITEM_CLASS = "o_menu_list_item";
 
     private static final String DEFAULT_IMG_CLASS = "o_menu_list_item_img";
     private static final String DEFAULT_ARROW_SPAN_CLASS = "o_menu_list_item_arrow_span";
     private static final String DEFAULT_INDENT_CLASS = "o_menu_list_item_image_span";
     private static final String DEFAULT_CONTENT_CLASS = "o_menu_list_item_content";
 
     protected static final String MENU_ITEMS_PARAMETERS_KEY = "menuItemsParametersKey";
 
     private static final String A_SUFIX = "::commandLink";
     private static final String SPAN_SUFIX = "::caption";
     private static final String ARROW_SPAN_SUFIX = "::arrowspan";
     private static final String ARROW_SUFIX = "::arrow";
     private static final String IMG_SUFIX = "::image";
     private static final String IMG_SPAN_SUFIX = "::imagespan";
     private static final String IMG_FAKE_SPAN_SUFIX = "::imagefakespan";
     private static final String ARROW_FAKE_SPAN_SUFIX = "::arrowfakespan";
 
     @Override
     public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
 
         MenuItem menuItem = (MenuItem) component;
         ResponseWriter writer = context.getResponseWriter();
         PopupMenu popupMenu = (PopupMenu) menuItem.getParent();
 
         writer.startElement("li", menuItem);
         writeAttribute(writer, "class", DEFAULT_LIST_ITEM_CLASS/* + " " + DefaultStyles.getPopupMenuBackgroundColorClass()*/);
         writeAttribute(writer, "id", menuItem.getClientId(context));
 
         renderStartMenuItemSpan(context, menuItem, writer);
 
         renderStyleClass(context, menuItem, writer);
 
         addSelectedStyleClass(context, menuItem);
 
         renderMenuItemImage(context, menuItem, writer, popupMenu);
         //content span
         writer.startElement("span", menuItem);
         writeAttribute(writer, "id", menuItem.getClientId(context) + SPAN_SUFIX);
 
         String contentClass = StyleUtil.getCSSClass(context, menuItem, menuItem.getContentAreaStyle(), StyleGroup.regularStyleGroup(),
                 menuItem.getContentAreaClass(), StyleUtil.getCSSClass(context, popupMenu, popupMenu.getItemContentStyle(), StyleGroup.regularStyleGroup(),
                         popupMenu.getItemContentClass(), DEFAULT_CONTENT_CLASS));
 
         writeAttribute(writer, "class", contentClass);
 
         renderContentStyleClass(context, menuItem, writer);
 
 
         renderMenuItemChildren(context, menuItem, writer);
 
         writer.endElement("span");
 
         UIComponent popupMenuChild = getChildPopupMenu(menuItem);
 
         StyleUtil.renderStyleClasses(context, menuItem);
 
 
         addSubmenuImage(context, menuItem, writer, popupMenu, popupMenuChild);
         if (popupMenuChild != null) {
             popupMenuChild.encodeAll(context);
         }
 
         StyleUtil.renderStyleClasses(context, menuItem);
 
         writer.endElement("span");
 
         if (menuItem.isDisabled()) {
             addMenuItemParameter(menuItem, "disabled", "true");
         }
     }
 
     private UIComponent getChildPopupMenu(MenuItem menuItem) {
         if (menuItem.getChildCount() > 0) {
             List<UIComponent> children = menuItem.getChildren();
             for (UIComponent child : children) {
                 if (child instanceof PopupMenu) {
                     return child;
                 }
             }
         }
         return null;
     }
 
     private void renderMenuItemChildren(FacesContext context, MenuItem menuItem, ResponseWriter writer) throws IOException {
         boolean isNeededValue = true;
         if (menuItem.getChildCount() > 0) {
             List<UIComponent> children = menuItem.getChildren();
             for (UIComponent child : children) {
                 if (child instanceof PopupMenu) {
                     addMenuItemParameter(menuItem, "menuId", child.getClientId(context));
                 } else {
                     child.encodeAll(context);
                     isNeededValue = false;
                 }
             }
         }
         if (isNeededValue) {
             String value = (String) menuItem.getValue();
             if (value != null)
                 writer.writeText(value, null);
         }
     }
 
     private void renderMenuItemImage(FacesContext context, MenuItem menuItem, ResponseWriter writer, PopupMenu popupMenu) throws IOException {
         if (popupMenu.isIndentVisible()) {
             writer.startElement("span", menuItem);
             writeAttribute(writer, "id", menuItem.getClientId(context) + IMG_SPAN_SUFIX);
 
             String indentAreaClass = StyleUtil.getCSSClass(context, menuItem, menuItem.getIndentAreaStyle(), StyleGroup.regularStyleGroup(),
                     menuItem.getIndentAreaClass(), StyleUtil.getCSSClass(context, popupMenu, popupMenu.getItemIndentStyle(), StyleGroup.regularStyleGroup(),
                             popupMenu.getItemIndentClass(), DEFAULT_INDENT_CLASS));
             writeAttribute(writer, "class", indentAreaClass);
 
             writer.startElement("span", menuItem);
             writeAttribute(writer, "id", menuItem.getClientId(context) + IMG_FAKE_SPAN_SUFIX);
             writeAttribute(writer, "class", "o_menu_list_item_img_fakespan");
             writer.endElement("span");
 
             String imgSrc = menuItem.getIconUrl();
             String imgSelectedSrc = menuItem.getSelectedIconUrl();
             String disabledIconUrl = menuItem.getDisabledIconUrl();
             String disabledSelectedIconUrl = menuItem.getSelectedDisabledIconUrl();
 
             writer.startElement("img", menuItem);
             writeAttribute(writer, "id", menuItem.getClientId(context) + IMG_SUFIX);
             if (imgSrc != null)
                 writeAttribute(writer, "src", ResourceUtil.getApplicationResourceURL(context, imgSrc));
             writeAttribute(writer, "class", DEFAULT_IMG_CLASS);
             writer.endElement("img");
 
             addMenuItemParameter(menuItem, "imgSelectedSrc", ResourceUtil.getApplicationResourceURL(context, imgSelectedSrc));
             addMenuItemParameter(menuItem, "imgSrc", ResourceUtil.getApplicationResourceURL(context, imgSrc));
             addMenuItemParameter(menuItem, "disabledImgSelectedSrc", ResourceUtil.getApplicationResourceURL(context, disabledSelectedIconUrl));
             addMenuItemParameter(menuItem, "disabledImgSrc", ResourceUtil.getApplicationResourceURL(context, disabledIconUrl));
             writer.endElement("span");
         }
     }
 
     private void addSubmenuImage(FacesContext context, MenuItem menuItem, ResponseWriter writer, PopupMenu popupMenu, UIComponent popupMenuChild) throws IOException {
         writer.startElement("span", menuItem);
         writeAttribute(writer, "id", menuItem.getClientId(context) + ARROW_SPAN_SUFIX);
         //todo
         String submenuIconAreaClass = StyleUtil.getCSSClass(context, menuItem, menuItem.getSubmenuIconAreaStyle(), StyleGroup.regularStyleGroup(),
                 menuItem.getSubmenuIconAreaClass(), StyleUtil.getCSSClass(context, popupMenu, popupMenu.getItemSubmenuIconStyle(), StyleGroup.regularStyleGroup(),
                         popupMenu.getItemSubmenuIconClass(), DEFAULT_ARROW_SPAN_CLASS));
         writeAttribute(writer, "class", submenuIconAreaClass);
         if (popupMenuChild != null) {
             String submenuImageUrl = menuItem.getSubmenuImageUrl();
             writer.startElement("span", menuItem);
             writeAttribute(writer, "id", menuItem.getClientId(context) + ARROW_FAKE_SPAN_SUFIX);
             writeAttribute(writer, "class", "o_menu_list_item_img_fakespan");
             writer.endElement("span");
 
             writer.startElement("img", menuItem);
             writeAttribute(writer, "id", menuItem.getClientId(context) + ARROW_SUFIX);
             if (submenuImageUrl != null)
                 writeAttribute(writer, "src", ResourceUtil.getApplicationResourceURL(context, submenuImageUrl));
             writeAttribute(writer, "class", DEFAULT_IMG_CLASS);
             writer.endElement("img");
 
             String disabledSubmenuImageUrl = menuItem.getDisabledSubmenuImageUrl();
             if (disabledSubmenuImageUrl != null) {
                 addMenuItemParameter(menuItem, "disabledSubmenuImageUrl",
                         ResourceUtil.getResourceURL(context, disabledSubmenuImageUrl,
                                 MenuItemRenderer.class, ""));
             }
 
             String selectedDisabledSubmenuImageUrl = menuItem.getSelectedDisabledSubmenuImageUrl();
             if (selectedDisabledSubmenuImageUrl != null) {
                 addMenuItemParameter(menuItem, "selectedDisabledSubmenuImageUrl",
                         ResourceUtil.getResourceURL(context, selectedDisabledSubmenuImageUrl,
                                 MenuItemRenderer.class, ""));
             }
 
 
             if (submenuImageUrl != null) {
                 addMenuItemParameter(menuItem, "submenuImageUrl",
                         ResourceUtil.getResourceURL(context, submenuImageUrl,
                                 MenuItemRenderer.class, ""));
             }
 
 
             String selectedSubmenuImageUrl = menuItem.getSelectedSubmenuImageUrl();
             if (selectedSubmenuImageUrl != null) {
                 addMenuItemParameter(menuItem, "selectedSubmenuImageUrl",
                         ResourceUtil.getResourceURL(context, selectedSubmenuImageUrl,
                                 MenuItemRenderer.class, ""));
             }
         }
         writer.endElement("span");
     }
 
     private void addMenuItemParameter(MenuItem popupMenuItem, String paramName, String paramValue) {
         if (paramValue == null || paramValue.equals("")) return;
         Map<String, String> parameters = (Map<String, String>) popupMenuItem.getAttributes().get(MENU_ITEMS_PARAMETERS_KEY);
         if (parameters == null) {
             parameters = new HashMap<String, String>();
             popupMenuItem.getAttributes().put(MENU_ITEMS_PARAMETERS_KEY, parameters);
         }
         parameters.put(paramName, paramValue);
     }
 
     private void renderStartMenuItemSpan(FacesContext context, MenuItem menuItem, ResponseWriter writer) throws IOException {
         writer.startElement("span", menuItem);
         writeAttribute(writer, "id", menuItem.getClientId(context) + A_SUFIX);
         if (!menuItem.isDisabled() && (menuItem.getActionExpression() != null || menuItem.getActionListeners().length > 0)) // todo: should adding actionListener turn on form submission indeed?
             addMenuItemParameter(menuItem, "action", "O$.submitFormWithAdditionalParam(this, '" + menuItem.getClientId(context) + "::clicked', 'true');");
 

        if (!menuItem.isDisabled()) {
            writeAttribute(writer, "onclick", menuItem.getOnclick());
            writeAttribute(writer, "ondblclick", menuItem.getOndblclick());
            writeAttribute(writer, "onmousedown", menuItem.getOnmousedown());
            writeAttribute(writer, "onmouseup", menuItem.getOnmouseup());
            writeAttribute(writer, "onmouseover", menuItem.getOnmouseover());
            writeAttribute(writer, "onmousemove", menuItem.getOnmousemove());
            writeAttribute(writer, "onmouseout", menuItem.getOnmouseout());
            writeAttribute(writer, "onkeypress", menuItem.getOnkeypress());
            writeAttribute(writer, "onkeydown", menuItem.getOnkeydown());
            writeAttribute(writer, "onkeyup", menuItem.getOnkeyup());
            writeAttribute(writer, "onblur", menuItem.getOnblur());
            writeAttribute(writer, "onfocus", menuItem.getOnfocus());
        }
     }
 
     private void addSelectedStyleClass(FacesContext context, MenuItem menuItem) {
         String styleClass = StyleUtil.getCSSClass(context, menuItem, menuItem.getSelectedStyle(), StyleGroup.selectedStyleGroup(2),
                 menuItem.getSelectedClass(), null);
         addMenuItemParameter(menuItem, "selectedClass", styleClass);
     }
 
 
     private void renderContentStyleClass(FacesContext context, MenuItem menuItem, ResponseWriter writer) throws IOException {
         String styleClass = StyleUtil.getCSSClass(context, menuItem, menuItem.getContentAreaStyle(), StyleGroup.regularStyleGroup(2),
                 menuItem.getContentAreaClass(), null);
         writeAttribute(writer, "class", styleClass);
     }
 
     private void renderStyleClass(FacesContext context, MenuItem menuItem, ResponseWriter writer) throws IOException {
         String styleClass = StyleUtil.getCSSClass(context, menuItem, menuItem.getStyle(), StyleGroup.regularStyleGroup(2),
                 menuItem.getStyleClass(), null);
         String disabledStyleClass = StyleUtil.getCSSClass(context, menuItem, menuItem.getDisabledStyle(), StyleGroup.disabledStyleGroup(2), menuItem.getDisabledClass(), null);
         addMenuItemParameter(menuItem, "disabledClass", disabledStyleClass);
 
         writeAttribute(writer, "class", styleClass);
     }
 
     @Override
     public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
         ResponseWriter writer = context.getResponseWriter();
         writer.endElement("li");
     }
 
     @Override
     public boolean getRendersChildren() {
         return true;
     }
 
     @Override
     public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
 
     }
 
     @Override
     public void decode(FacesContext context, UIComponent component) {
         Map<String, String> requestParameters = context.getExternalContext().getRequestParameterMap();
         String key = component.getClientId(context) + "::clicked";
         if (requestParameters.containsKey(key)) {
             component.queueEvent(new ActionEvent(component));
         }
     }
 }
