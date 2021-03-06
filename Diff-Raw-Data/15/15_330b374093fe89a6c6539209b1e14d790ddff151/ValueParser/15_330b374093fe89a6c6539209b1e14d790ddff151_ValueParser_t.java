 /* 
  * Copyright 2009 Laurent Bovet, Swiss Post IT <lbovet@jminix.org>
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  * 
  */
 
 package org.jminix.console.resource;
 
 import org.apache.commons.lang.StringUtils;
 import org.apache.commons.lang.Validate;
 
 public class ValueParser
 {
 	private static String stringArraySeparator = ";";
 	
 	public static void setStringArraySeparator(String separator) 
 	{
 		Validate.notNull(separator);
 		stringArraySeparator = separator;
 	}
 	
     public Object parse(String value, String type)
     {
         Object result=null;
         
         if( type.equals("java.lang.String") ) {
             return value;
         } else if( type.equals("java.lang.Byte") || type.equals("byte")) {
             result = new Byte(value);
         } else if( type.equals("java.lang.Short") || type.equals("short")) {
             result = new Short(value);
         } else if( type.equals("java.lang.Integer") || type.equals("int")) {
             result = new Integer(value);
         } else if( type.equals("java.lang.Long") || type.equals("long")) {
             result = new Long(value);
        } else if( type.equals("java.lang.Double") || type.equals("double")) {
            result = new Double(value);      
        } else if( type.equals("java.lang.Float") || type.equals("float")) {
            result = new Float(value);               
         } else if( type.equals("java.lang.Boolean") || type.equals("boolean")) {
             result = new Boolean(value);
         } else if( type.equals("[Ljava.lang.String;")) {
         	result = StringUtils.splitPreserveAllTokens(value, stringArraySeparator);
         }
         
         if(result==null) {
             throw new RuntimeException("Type "+type+" is not supported");
         }
         
         return result;
     }
 }
