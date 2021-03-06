 /*
  * This library is part of OpenCms -
  * the Open Source Content Management System
  *
  * Copyright (c) Alkacon Software GmbH (http://www.alkacon.com)
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * For further information about Alkacon Software, please see the
  * company website: http://www.alkacon.com
  *
  * For further information about OpenCms, please see the
  * project website: http://www.opencms.org
  * 
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  */
 
 package com.alkacon.forms.client.widgets;
 
 import com.alkacon.vie.shared.I_Entity;
 
 import com.google.gwt.event.dom.client.BlurEvent;
 import com.google.gwt.event.dom.client.BlurHandler;
 import com.google.gwt.event.dom.client.ChangeEvent;
 import com.google.gwt.event.dom.client.ChangeHandler;
 import com.google.gwt.event.dom.client.KeyPressEvent;
 import com.google.gwt.event.dom.client.KeyPressHandler;
 import com.google.gwt.event.logical.shared.ValueChangeEvent;
 import com.google.gwt.event.logical.shared.ValueChangeHandler;
 import com.google.gwt.event.shared.HandlerRegistration;
 import com.google.gwt.user.client.DOM;
 import com.google.gwt.user.client.Element;
 
 /**
  * The string edit widget.<p>
  */
 public class StringWidget extends A_EditWidget {
 
     /** The value changed handler initialized flag. */
     private boolean m_valueChangeHandlerInitialized;
 
     /**
      * @see com.google.gwt.event.logical.shared.HasValueChangeHandlers#addValueChangeHandler(com.google.gwt.event.logical.shared.ValueChangeHandler)
      */
     public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
 
         // Initialization code
         if (!m_valueChangeHandlerInitialized) {
             m_valueChangeHandlerInitialized = true;
             addDomHandler(new KeyPressHandler() {
 
                 public void onKeyPress(KeyPressEvent event) {
 
                     fireValueChange();
 
                 }
             }, KeyPressEvent.getType());
             addDomHandler(new ChangeHandler() {
 
                 public void onChange(ChangeEvent event) {
 
                     fireValueChange();
 
                 }
             }, ChangeEvent.getType());
             addDomHandler(new BlurHandler() {
 
                 public void onBlur(BlurEvent event) {
 
                     fireValueChange();
                 }
             }, BlurEvent.getType());
         }
         return addHandler(handler, ValueChangeEvent.getType());
     }
 
     /**
      * @see com.google.gwt.user.client.ui.HasValue#getValue()
      */
     @Override
     public String getValue() {
 
         return getElement().getInnerText();
     }
 
     /**
      * @see com.alkacon.forms.client.widgets.I_EditWidget#initWidget(com.google.gwt.user.client.Element, com.alkacon.vie.shared.I_Entity, java.lang.String, int)
      */
     public I_EditWidget initWidget(Element element, I_Entity entity, String attributeName, int valueIndex) {
 
         setElement(element);
         DOM.setEventListener(getElement(), this);
         setPreviousValue(getValue());
         getElement().setAttribute("contenteditable", "true");
         getElement().getStyle().setColor("red");
         return this;
     }
 
     /**
      * @see com.alkacon.forms.client.widgets.I_EditWidget#setConfiguration(java.lang.String)
      */
     public void setConfiguration(String confuguration) {
 
         // TODO: Auto-generated method stub
 
     }
 
     /**
      * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object)
      */
     public void setValue(String value) {
 
         setValue(value, true);
     }
 
     /**
      * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object, boolean)
      */
     public void setValue(String value, boolean fireEvents) {
 
         getElement().setInnerText(value);
         if (fireEvents) {
             fireValueChange();
         }
     }
 }
