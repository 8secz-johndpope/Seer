 /*
  * Copyright 2009-2011 Prime Technology.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.primefaces.renderkit;
 
 import java.lang.reflect.Array;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import javax.el.ELException;
 import javax.el.ExpressionFactory;
 import javax.el.ValueExpression;
 import javax.faces.component.UIComponent;
 import javax.faces.component.UIInput;
 import javax.faces.component.UISelectItem;
 import javax.faces.component.UISelectItems;
 import javax.faces.context.FacesContext;
 import javax.faces.convert.Converter;
 import javax.faces.convert.ConverterException;
 import javax.faces.model.SelectItem;
 
 public abstract class InputRenderer extends CoreRenderer {
 
     protected List<SelectItem> getSelectItems(FacesContext context, UIInput component) {
         List<SelectItem> selectItems = new ArrayList<SelectItem>();
 
         for(UIComponent child : component.getChildren()) {
             if(child instanceof UISelectItem) {
                 UISelectItem uiSelectItem = (UISelectItem) child;
 				selectItems.add(new SelectItem(uiSelectItem.getItemValue(), uiSelectItem.getItemLabel(), uiSelectItem.getItemDescription(), uiSelectItem.isItemDisabled(), uiSelectItem.isItemEscaped(), uiSelectItem.isNoSelectionOption()));
 			}
             else if(child instanceof UISelectItems) {
                 UISelectItems uiSelectItems = ((UISelectItems) child);
 				Object value = uiSelectItems.getValue();
 
                 if(value instanceof SelectItem) {
                     selectItems.add((SelectItem) value);
                 }
                 else if(value.getClass().isArray()) {
                     for(int i = 0; i < Array.getLength(value); i++) {
                         Object item = Array.get(value, i);
                         
                         if(item instanceof SelectItem)
                             selectItems.add((SelectItem) item);
                         else
                             selectItems.add(createSelectItem(context, uiSelectItems, item));
                     }
                 }
                 else if(value instanceof Map) {
                     Map map = (Map) value;
 
                     for(Iterator it = map.keySet().iterator(); it.hasNext();) {
                         Object key = it.next();
 
                         selectItems.add(new SelectItem(map.get(key), String.valueOf(key)));
                     }
                 }
                 else if(value instanceof Collection) {
                     Collection collection = (Collection) value;
                     
                     for(Iterator it = collection.iterator(); it.hasNext();) {
                        selectItems.add(createSelectItem(context, uiSelectItems, it.next()));
                     }               
                 }
 			}
         }
 
         return selectItems;
 	}
     
     protected SelectItem createSelectItem(FacesContext context, UISelectItems uiSelectItems, Object object) {
         String var = (String) uiSelectItems.getAttributes().get("var");
         
         if(var != null) {
             context.getExternalContext().getRequestMap().put(var, object);
 
             String itemLabel = (String) uiSelectItems.getAttributes().get("itemLabel");
             Object itemValue = uiSelectItems.getAttributes().get("itemValue");
             String description = (String) uiSelectItems.getAttributes().get("itemDescription");
             Boolean disabled = Boolean.valueOf(((String) uiSelectItems.getAttributes().get("itemDisabled")));
             Boolean escaped = Boolean.valueOf(((String) uiSelectItems.getAttributes().get("itemEscaped")));
             Boolean noSelectionOption = Boolean.valueOf(((String) uiSelectItems.getAttributes().get("noSelectionOption")));
 
             if(itemValue == null)
                 itemValue = object;
             if(itemLabel == null)
                 itemLabel = String.valueOf(object);
 
             return new SelectItem(itemValue, itemLabel, description, disabled, escaped, noSelectionOption);
         }
         else {
             return new SelectItem(object, String.valueOf(object));
         }
     }
 
 	protected String getOptionAsString(FacesContext context, UIInput component, Converter converter, Object value) {
 		if(value == null)
             return "";
         else if(value instanceof String)
             return (String) value;
         else if(converter != null)
 			return converter.getAsString(context, component, value);
         else
             return value.toString();
 	}
 
     protected Converter getConverter(FacesContext context, UIInput component) {
         Converter converter = component.getConverter();
 
         if(converter != null) {
             return converter;
         } else {
             ValueExpression ve = component.getValueExpression("value");
 
             if(ve != null) {
                 Class<?> valueType = ve.getType(context.getELContext());
                 
                 if(valueType != null)
                     return context.getApplication().createConverter(valueType);
             }
         }
 
         return null;
     }
     
     @Override
 	public Object getConvertedValue(FacesContext context, UIComponent component, Object submittedValue) throws ConverterException {
 		UIInput input = (UIInput) component;
 		Converter converter = getConverter(context, input);
 
 		if(converter != null) {
             return converter.getAsObject(context, component, (String) submittedValue);
         } 
         else {
             return submittedValue;
         }
 	}
     
     protected Object coerceToModelType(FacesContext ctx, Object value, Class itemValueType) {
         Object newValue;
         try {
             ExpressionFactory ef = ctx.getApplication().getExpressionFactory();
             newValue = ef.coerceToType(value, itemValueType);
         } 
         catch (ELException ele) {
             newValue = value;
         } 
         catch (IllegalArgumentException iae) {
             newValue = value;
         }
 
         return newValue;
     }
     
     public static boolean shouldDecode(UIComponent component) {
         boolean disabled = Boolean.valueOf(String.valueOf(component.getAttributes().get("disabled")));
         boolean readonly = Boolean.valueOf(String.valueOf(component.getAttributes().get("readonly")));
         
         return !disabled && !readonly;
     }
 }
