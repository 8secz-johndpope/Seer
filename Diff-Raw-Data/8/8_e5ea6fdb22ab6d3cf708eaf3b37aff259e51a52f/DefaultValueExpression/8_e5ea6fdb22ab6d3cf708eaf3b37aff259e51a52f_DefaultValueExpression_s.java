 /*
  * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
  *
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the GNU Lesser General Public License
  * (LGPL) version 2.1 which accompanies this distribution, and is available at
  * http://www.gnu.org/licenses/lgpl-2.1.html
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * Contributors:
  *     Anahide Tchertchian
  */
 package org.nuxeo.ecm.platform.ui.web.binding;
 
 import java.util.Collection;
 
 import javax.el.ELContext;
 import javax.el.PropertyNotFoundException;
 import javax.el.ValueExpression;
 
 /**
  * Value expression holding a default value expression for resolve on get, when
  * original value expression defaults to null.
  * <p>
  * Accepts a null original value expression in case default value should be
  * resolved even if no mapping should be done.
  *
  * @since 5.7.3
  */
 public class DefaultValueExpression extends ValueExpression {
 
     private static final long serialVersionUID = 1L;
 
     protected final ValueExpression originalExpression;
 
     protected final ValueExpression defaultExpression;
 
     public DefaultValueExpression(ValueExpression originalExpression,
             ValueExpression defaultExpression) {
         super();
         this.originalExpression = originalExpression;
         this.defaultExpression = defaultExpression;
     }
 
     @Override
     public Class<?> getExpectedType() {
         if (originalExpression != null) {
             return originalExpression.getExpectedType();
         } else {
             return defaultExpression.getExpectedType();
         }
     }
 
     @Override
     public Class<?> getType(ELContext arg0) throws PropertyNotFoundException {
         if (originalExpression != null) {
             return originalExpression.getType(arg0);
         } else {
             return defaultExpression.getType(arg0);
         }
     }
 
     @Override
     @SuppressWarnings("rawtypes")
     public Object getValue(ELContext arg0) throws PropertyNotFoundException {
         Object value = null;
         if (originalExpression != null) {
             value = originalExpression.getValue(arg0);
         }
         if (value == null
                 || ((value instanceof Object[]) && ((Object[]) value).length == 0)
                 || ((value instanceof Collection) && ((Collection) value).size() == 0)) {
             value = defaultExpression.getValue(arg0);
         }
         return value;
     }
 
     @Override
     public boolean isReadOnly(ELContext arg0) throws PropertyNotFoundException {
         if (originalExpression != null) {
             return originalExpression.isReadOnly(arg0);
         } else {
             return true;
         }
     }
 
     @Override
     public void setValue(ELContext arg0, Object arg1)
             throws PropertyNotFoundException {
         if (originalExpression != null) {
             originalExpression.setValue(arg0, arg1);
         }
     }
 
     @Override
     public boolean equals(Object obj) {
         if (this == obj) {
             return true;
         }
         if (!(obj instanceof DefaultValueExpression)) {
             return false;
         }
         DefaultValueExpression other = (DefaultValueExpression) obj;
         if (originalExpression != null) {
             return originalExpression.equals(other.originalExpression)
                     && defaultExpression.equals(other.defaultExpression);
         } else {
             return defaultExpression.equals(other.defaultExpression);
         }
     }
 
     @Override
     public String getExpressionString() {
         if (originalExpression != null) {
             return originalExpression.getExpressionString();
         }
         return null;
     }
 
     @Override
     public int hashCode() {
         if (originalExpression != null) {
             return originalExpression.hashCode() + defaultExpression.hashCode();
         } else {
             return defaultExpression.hashCode();
         }
     }
 
     @Override
     public boolean isLiteralText() {
        if (originalExpression != null) {
            return originalExpression.isLiteralText();
        }
        return true;
     }

 }
