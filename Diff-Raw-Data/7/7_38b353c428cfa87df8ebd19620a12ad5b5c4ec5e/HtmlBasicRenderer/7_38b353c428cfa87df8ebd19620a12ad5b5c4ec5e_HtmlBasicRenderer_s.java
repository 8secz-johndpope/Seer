 /*
 * $Id: HtmlBasicRenderer.java,v 1.96 2005/07/11 17:43:49 jayashri Exp $
  */
 
 /*
  * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
  * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
  */
 
 // HtmlBasicRenderer.java
 
 package com.sun.faces.renderkit.html_basic;
 
 import com.sun.faces.util.MessageFactory;
 import com.sun.faces.util.Util;
 
 import javax.faces.component.NamingContainer;
 import javax.faces.component.UIComponent;
 import javax.faces.component.UIInput;
 import javax.faces.component.UIForm;
 import javax.faces.component.UIParameter;
 import javax.faces.component.UIViewRoot;
 import javax.faces.component.ValueHolder;
 import javax.faces.context.FacesContext;
 import javax.faces.context.ResponseWriter;
 import javax.faces.convert.Converter;
 import javax.faces.convert.ConverterException;
 import javax.faces.render.Renderer;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import java.util.logging.Logger;
 import java.util.logging.Level;
 
 /**
  * <B>HtmlBasicRenderer</B> is a base class for implementing renderers
  * for HtmlBasicRenderKit.
  */
 
 public abstract class HtmlBasicRenderer extends Renderer {
 
     //
     // Protected Constants
     //
 
     //
     // Class Variables
     //
     // Log instance for this class
     protected static final Logger logger = 
             Util.getLogger(Util.FACES_LOGGER + Util.RENDERKIT_LOGGER);
    
     //
     // Instance Variables
     //
 
     // Attribute Instance Variables
 
     // Relationship Instance Variables
 
     //
     // Constructors and Initializers    
     //
 
     public static final String SCRIPT_ELEMENT = "script";
     public static final String SCRIPT_LANGUAGE = "language";
     public static final String SCRIPT_TYPE = "type";
     public static final String SCRIPT_LANGUAGE_JAVASCRIPT = "JavaScript";
 
     public static final String CLEAR_HIDDEN_FIELD_FN_NAME = 
          "clearFormHiddenParams";
    
     public HtmlBasicRenderer() {
         super();
     }
 
     //
     // Class methods
     //
 
     //
     // Methods From Renderer
 
     public void addGenericErrorMessage(FacesContext facesContext,
                                        UIComponent component,
                                        String messageId, String param) {
         Object[] params = new Object[3];
         params[0] = param;
         facesContext.addMessage(component.getClientId(facesContext),
             MessageFactory.getMessage(facesContext, messageId, params));
     }
 
 
     public void decode(FacesContext context, UIComponent component) {
 
         if (context == null || component == null) {
             throw new NullPointerException(Util.getExceptionMessageString(
                 Util.NULL_PARAMETERS_ERROR_MESSAGE_ID));
         }
 
         if (logger.isLoggable(Level.FINER)) {
             logger.log(Level.FINER, 
                     "Begin decoding component " + component.getId());
         }
 
         if (!(component instanceof UIInput)) {
             // decode needs to be invoked only for components that are
             // instances or subclasses of UIInput.
             if (logger.isLoggable(Level.FINE)) {
                  logger.fine("No decoding necessary since the component "
                           + component.getId() +
                           " is not an instance or a sub class of UIInput");
             }
             return;
         }    
 
         // If the component is disabled, do not change the value of the
         // component, since its state cannot be changed.
         if (Util.componentIsDisabledOnReadonly(component)) {
             if (logger.isLoggable(Level.FINE)) {
                  logger.fine("No decoding necessary since the component " +
                           component.getId() + " is disabled");
             }
             return;
         }
 
         String clientId = component.getClientId(context);
         assert (clientId != null);
         Map requestMap = context.getExternalContext().getRequestParameterMap();
         // Don't overwrite the value unless you have to!
         if (requestMap.containsKey(clientId)) {
             String newValue = (String) requestMap.get(clientId);
             setSubmittedValue(component, newValue);
             if (logger.isLoggable(Level.FINE)) {
                  logger.fine("new value after decoding" + newValue);
             }
         }
         if (logger.isLoggable(Level.FINER)) {
             logger.log(Level.FINER, 
                     "End decoding component " + component.getId());
         }
     }
 
     public boolean getRendersChildren() {
 	return true;
     }
 
     public void encodeEnd(FacesContext context, UIComponent component)
         throws IOException {
 
         String currentValue = null;
         ResponseWriter writer = null;
         String styleClass = null;
 
         if (context == null || component == null) {
             throw new NullPointerException(Util.getExceptionMessageString(
                 Util.NULL_PARAMETERS_ERROR_MESSAGE_ID));
         }
 
         if (logger.isLoggable(Level.FINER)) {
             logger.log(Level.FINER, 
                     "Begin encoding component " + component.getId());
         } 
         
         // suppress rendering if "rendered" property on the component is
         // false.
         if (!component.isRendered()) {
             if (logger.isLoggable(Level.FINE)) {
                  logger.fine("End encoding component " + component.getId() +
                           " since " +
                           "rendered attribute is set to false ");
             }
             return;
         }
 
         writer = context.getResponseWriter();
         assert (writer != null);
 
         currentValue = getCurrentValue(context, component);
         if (logger.isLoggable(Level.FINE)) {
             logger.log(Level.FINE, "Value to be rendered " + currentValue);
         }
         getEndTextToRender(context, component, currentValue);
     }
 
 
     /**
      * <p>Render nested child components by invoking the encode methods
      * on those components, but only when the <code>rendered</code>
      * property is <code>true</code>.</p>
      */
     protected void encodeRecursive(FacesContext context, UIComponent component)
         throws IOException {
 
         // suppress rendering if "rendered" property on the component is
         // false.
         if (!component.isRendered()) {
             return;
         }
 
         // Render this component and its children recursively
         component.encodeBegin(context);
         if (component.getRendersChildren()) {
             component.encodeChildren(context);
         } else {
             Iterator kids = getChildren(component);
             while (kids.hasNext()) {
                 UIComponent kid = (UIComponent) kids.next();
                 encodeRecursive(context, kid);
             }
         }
         component.encodeEnd(context);
     }
 
 
     /**
      * <p>Return an Iterator over the children of the specified
      * component, selecting only those that have a
      * <code>rendered</code> property of <code>true</code>.</p>
      *
      * @param component <code>UIComponent</code> for which to extract children
      */
     protected Iterator getChildren(UIComponent component) {
 
         List results = new ArrayList();
         Iterator kids = component.getChildren().iterator();
         while (kids.hasNext()) {
             UIComponent kid = (UIComponent) kids.next();
             if (kid.isRendered()) {
                 results.add(kid);
             }
         }
         return (results.iterator());
 
     }
 
 
 
     /**
      * Gets value to be rendered and formats it if required. Sets to empty
      * string if value is null.
      */
     protected String getCurrentValue(FacesContext context, UIComponent component) {
 
         if (component instanceof UIInput) {
             Object submittedValue = ((UIInput) component).getSubmittedValue();
             if (submittedValue != null) {
                 return (String) submittedValue;
             }
         }
 
         String currentValue = null;
         Object currentObj = getValue(component);
        currentValue = getFormattedValue(context, component, currentObj);
         return currentValue;
     }
 
 
     protected Object getValue(UIComponent component) {
         // Make sure this method isn't being called except 
         // from subclasses that override getValue()!
         throw new UnsupportedOperationException();
     }
 
 
     /**
      * Renderers override this method to write appropriate HTML content into
      * the buffer.
      */
     protected void getEndTextToRender(FacesContext context, UIComponent component,
                                       String currentValue) throws IOException {
         return;
     }
 
 
     /**
      * Renderers override this method to store the previous value
      * of the associated component.
      */
     protected void setSubmittedValue(UIComponent component, Object value) {
     }
 
 
     /**
      * Renderers override this method in case output value needs to be
      * formatted
      */
     protected String getFormattedValue(FacesContext context, UIComponent component,
                                        Object currentValue)
         throws ConverterException {
 
         String result = null;
         // formatting is supported only for components that support
         // converting value attributes.
         if (!(component instanceof ValueHolder)) {
             if (currentValue != null) {
                 result = currentValue.toString();
             }
             return result;
         }
 
         Converter converter = null;
 
         // If there is a converter attribute, use it to to ask application
         // instance for a converter with this identifer.
         converter = ((ValueHolder) component).getConverter();
 
 
         // if value is null and no converter attribute is specified, then
         // return a zero length String.
         if (converter == null && currentValue == null) {
             return "";
         }
 
         if (converter == null) {
             // Do not look for "by-type" converters for Strings
             if (currentValue instanceof String) {
                 return (String) currentValue;
             }
 
             // if converter attribute set, try to acquire a converter
             // using its class type.
 
             Class converterType = currentValue.getClass();
             converter = Util.getConverterForClass(converterType);
 
             // if there is no default converter available for this identifier,
             // assume the model type to be String.
             if (converter == null) {
                 result = currentValue.toString();
                 return result;
             }
         }
 
         return converter.getAsString(context, component, currentValue);
 
     }
 
 
     public String convertClientId(FacesContext context, String clientId) {
         return clientId;
     }
 
 
     protected Iterator getMessageIter(FacesContext context,
                                       String forComponent,
                                       UIComponent component) {
         Iterator messageIter = null;
         // Attempt to use the "for" attribute to locate 
         // messages.  Three possible scenarios here:
         // 1. valid "for" attribute - messages returned
         //    for valid component identified by "for" expression.
         // 2. zero length "for" expression - global errors
         //    not associated with any component returned
         // 3. no "for" expression - all messages returned.
         if (null != forComponent) {
             if (forComponent.length() == 0) {
                 messageIter = context.getMessages(null);
             } else {
                 UIComponent result = getForComponent(context, forComponent,
                                                      component);
                 if (result == null) {
                     messageIter = Collections.EMPTY_LIST.iterator();
                 } else {
                     messageIter =
                         context.getMessages(result.getClientId(context));
                 }
             }
         } else {
             messageIter = context.getMessages();
         }
         return messageIter;
     }
 
 
     /**
      * Locates the component identified by <code>forComponent</code>
      *
      * @param forComponent - the component to search for
      * @param component    - the starting point in which to begin the search
      * @return the component with the the <code>id</code that matches
      *         <code>forComponent</code> otheriwse null if no match is found.
      */
     protected UIComponent getForComponent(FacesContext context,
                                           String forComponent, UIComponent component) {
         if (null == forComponent || forComponent.length() == 0) {
             return null;
         }
 
         UIComponent result = null;
         UIComponent currentParent = component;
         try {
             // Check the naming container of the current 
             // component for component identified by
             // 'forComponent'
             while (currentParent != null) {
                 // If the current component is a NamingContainer,
                 // see if it contains what we're looking for.
                 result = currentParent.findComponent(forComponent);
                 if (result != null)
                     break;
                 // if not, start checking further up in the view
                 currentParent = currentParent.getParent();
             }                   
 
             // no hit from above, scan for a NamingContainer
             // that contains the component we're looking for from the root.    
             if (result == null) {
                 result =
                     findUIComponentBelow(context.getViewRoot(), forComponent);
             }
         } catch (Throwable t) {
             Object[] params = {forComponent};
             throw new RuntimeException(Util.getExceptionMessageString(
                 Util.COMPONENT_NOT_FOUND_ERROR_MESSAGE_ID, params));
         }
         // log a message if we were unable to find the specified
         // component (probably a misconfigured 'for' attribute
         if (result == null) {
             if (logger.isLoggable(Level.WARNING)) {
                  logger.warning(Util.getExceptionMessageString(
                     Util.COMPONENT_NOT_FOUND_IN_VIEW_WARNING_ID,
                     new Object[]{forComponent}));
             }
         }
         return result;
     }
 
 
     /**
      * <p>Recursively searches for {@link NamingContainer}s from the
      * given start point looking for the component with the <code>id</code>
      * specified by <code>forComponent</code>.
      *
      * @param startPoint   - the starting point in which to begin the search
      * @param forComponent - the component to search for
      * @return the component with the the <code>id</code that matches
      *         <code>forComponent</code> otheriwse null if no match is found.
      */
     private UIComponent findUIComponentBelow(UIComponent startPoint, String forComponent) {
         UIComponent retComp = null;
         List children = startPoint.getChildren();
         for (int i = 0, size = children.size(); i < size; i++) {
             UIComponent comp = (UIComponent) children.get(i);
 
             if (comp instanceof NamingContainer) {
                 retComp = comp.findComponent(forComponent);
             }
 
             if (retComp == null) {
                 if (comp.getChildCount() > 0) {
                     retComp = findUIComponentBelow(comp, forComponent);
                 }
             }
 
             if (retComp != null)
                 break;
         }
         return retComp;
     }
 
     /**
      * <p>Return the specified facet from the specified component, but
      * <strong>only</strong> if its <code>rendered</code> property is
      * set to <code>true</code>.
      *
      * @param component Component from which to return a facet
      * @param name      Name of the desired facet
      */
     protected UIComponent getFacet(UIComponent component, String name) {
 
         UIComponent facet = component.getFacet(name);
         if ((facet != null) && !facet.isRendered()) {
             facet = null;
         }
         return (facet);
 
     }
 
 
     /**
      * @return true if this renderer should render an id attribute.
      */
     protected boolean shouldWriteIdAttribute(UIComponent component) {
         String id;
         return (null != (id = component.getId()) &&
             !id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX));
     }
 
 
     protected String writeIdAttributeIfNecessary(FacesContext context,
                                                ResponseWriter writer,
                                                UIComponent component) {
         String id = null;
         if (shouldWriteIdAttribute(component)) {
             try {
                 writer.writeAttribute("id", id = component.getClientId(context),
                                       "id");
             } catch (IOException e) {
                 if (logger.isLoggable(Level.WARNING)) {
                     // PENDING I18N
                     logger.warning("Can't write ID attribute" + e.getMessage());
                 }
             }
         }
         return id;
     }
 
 
     protected Param[] getParamList(FacesContext context, UIComponent command) {
         ArrayList parameterList = new ArrayList();
 
         Iterator kids = command.getChildren().iterator();
         while (kids.hasNext()) {
             UIComponent kid = (UIComponent) kids.next();
 
             if (kid instanceof UIParameter) {
                 UIParameter uiParam = (UIParameter) kid;
                 Object value = uiParam.getValue();
                 Param param = new Param(uiParam.getName(),
                                         (value == null ? null :
                                          value.toString()));
                 parameterList.add(param);
             }
         }
 
         return (Param[]) parameterList.toArray(new Param[parameterList.size()]);
     }
 
 
     //inner class to store parameter name and value pairs
     protected static class Param {
 
         public Param(String name, String value) {
             set(name, value);
         }
 
 
         private String name;
         private String value;
 
 
         public void set(String name, String value) {
             this.name = name;
             this.value = value;
         }
 
 
         public String getName() {
             return name;
         }
 
 
         public String getValue() {
             return value;
         }
     }
 
 } // end of class HtmlBasicRenderer
