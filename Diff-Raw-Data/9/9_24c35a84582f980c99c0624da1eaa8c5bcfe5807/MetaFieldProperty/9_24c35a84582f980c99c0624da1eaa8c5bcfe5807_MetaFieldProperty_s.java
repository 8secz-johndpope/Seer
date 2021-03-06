 /*
  * $Id$
  *
  * Copyright 2003 (C) James Strachan and Bob Mcwhirter. All Rights Reserved.
  *
  * Redistribution and use of this software and associated documentation
  * ("Software"), with or without modification, are permitted provided that the
  * following conditions are met:
  *  1. Redistributions of source code must retain copyright statements and
  * notices. Redistributions must also contain a copy of this document.
  *  2. Redistributions in binary form must reproduce the above copyright
  * notice, this list of conditions and the following disclaimer in the
  * documentation and/or other materials provided with the distribution.
  *  3. The name "groovy" must not be used to endorse or promote products
  * derived from this Software without prior written permission of The Codehaus.
  * For written permission, please contact info@codehaus.org.
  *  4. Products derived from this Software may not be called "groovy" nor may
  * "groovy" appear in their names without prior written permission of The
  * Codehaus. "groovy" is a registered trademark of The Codehaus.
  *  5. Due credit should be given to The Codehaus - http://groovy.codehaus.org/
  *
  * THIS SOFTWARE IS PROVIDED BY THE CODEHAUS AND CONTRIBUTORS ``AS IS'' AND ANY
  * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED. IN NO EVENT SHALL THE CODEHAUS OR ITS CONTRIBUTORS BE LIABLE FOR
  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
  * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
  * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  * DAMAGE.
  *
  */
  
 package groovy.lang;
 
 
 import org.codehaus.groovy.runtime.InvokerHelper;
 import java.lang.reflect.Field;
 
 /**
  * Represents a property on a bean which may have a getter and/or a setter
  * 
  * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
  * @version $Revision$
  */
 public class MetaFieldProperty extends MetaProperty {
 
     private Field field;
 
     public MetaFieldProperty(Field field) {
 		super(field.getName(), field.getType());
 		
         this.field = field;
     }
 
     /**
      * @return the property of the given object
      * @throws Exception if the property could not be evaluated
      */
     public Object getProperty(Object object) throws Exception {
         return field.get(object);
     }
 
     /**
      * Sets the property on the given object to the new value
      * 
      * @param object on which to set the property
      * @param newValue the new value of the property
      * @throws Exception if the property could not be set
      */
     public void setProperty(Object object, Object newValue) {
         try {
             field.set(object, newValue);
         }
         catch (IllegalArgumentException e) {
             try {
                 field.set(object, InvokerHelper.asType(newValue, field.getType()));
             }
             catch (Exception ex) {
                throw new TypeMissMatchException( "'" + toName(object.getClass()) + "." + field.getName()
                                                   + "' can not refer to the value '"
                                                   + newValue + "' (type " + toName(newValue.getClass())
                                                   + "), because it is of the type " + toName(field.getType()) );
             }
         }
         catch (Exception e) {
             throw new GroovyRuntimeException("Cannot set the property '" + name + "'.", e);
         }
     }
 
     private String toName(Class c) {
         String s = c.toString();
         if (s.startsWith("class ") && s.length() > 6)
             return s.substring(6);
         else
             return s;
     }
 }
