 /*
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  * 
  *      http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.apache.xerces.impl.xs.util;
 
 import org.apache.xerces.impl.dv.InvalidDatatypeValueException;
 import org.apache.xerces.impl.dv.ValidatedInfo;
 import org.apache.xerces.impl.dv.ValidationContext;
 import org.apache.xerces.impl.dv.XSSimpleType;
 import org.apache.xerces.impl.validation.ValidationState;
 import org.apache.xerces.impl.xs.SchemaSymbols;
 import org.apache.xerces.xs.XSObjectList;
 import org.apache.xerces.xs.XSTypeDefinition;
 
 /**
 * @xerces.internal
 * 
  * Class defining utility/helper methods related to XML schema types.
  * 
  * @author Mukul Gandhi, IBM
  * @version $Id$
  */
 public class XSTypeHelper {
     
     
     /*
      * Checks if the two schema type components are identical.
      */
     public static boolean schemaTypesIdentical(XSTypeDefinition typeDefn1,
                                                XSTypeDefinition typeDefn2) {
         boolean typesIdentical = false;
         
         String type1Name = typeDefn1.getName();
         String type2Name = typeDefn2.getName();
         
         if (("anyType".equals(type1Name) && 
              "anyType".equals(type2Name)) ||
             ("anySimpleType".equals(type1Name) && 
              "anySimpleType".equals(type2Name))) {
                typesIdentical = true;  
         }
         
         if (!typesIdentical) {                        
             if (uriEqual(typeDefn1.getNamespace(), typeDefn2.getNamespace())) {
                 // if targetNamespace of types are same, then check for  equality of type names and of the base type.
                 if ((type1Name == null && type2Name == null) ||
                     (type1Name != null && type1Name.equals(type2Name))
                           && (schemaTypesIdentical(typeDefn1.getBaseType(), typeDefn2.getBaseType()))) {
                      typesIdentical = true;   
                 }
             }
         }
         
         return typesIdentical;
         
     } // schemaTypesIdentical
     
     
     /*
      * Check if two URI values are equal.
      */
     public static boolean uriEqual(String uri1, String uri2) {
         
         boolean uriEqual = false;
         
         if ((uri1 != null && uri2 == null) ||
             (uri1 == null && uri2 != null)) {
            uriEqual = false;
         } else if ((uri1 == null && uri2 == null) ||
                     uri1.equals(uri2)) {
            uriEqual = true;   
         } 
         
         return uriEqual;
         
     } // uriEqual
     
     
     /*
      * Determine if an atomic value is valid with respect to any of the union's member types. 
      * If this method returns 'true', then the value is valid with respect to entire union 
      * schema component. 
      */
     public static boolean isAtomicValueValidForAnUnion(XSObjectList memberTypes,
                                                        String content, ValidatedInfo validatedInfo) {
         
         boolean isValid = false;
         
         // check the union member types in order to check for validity of an 'atomic value'.
         // the validity of 'atomic value' wrt to the 1st available type in this sequence,
         // is sufficient to achieve the objective of this method.
         for (int memTypeIdx = 0; memTypeIdx < memberTypes.getLength(); memTypeIdx++) {
             XSSimpleType simpleTypeDv = (XSSimpleType) memberTypes.item(memTypeIdx);
             if (SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(simpleTypeDv.getNamespace())) {                
                 if (XSTypeHelper.isValueValidForASimpleType(content, simpleTypeDv)) {
                     isValid = true;
                     break;  
                 }
             }
         }
         
         return isValid;
         
     } // isAtomicValueValidForAnUnion
     
     
     /*
      * Determine if a lexical "string value" belongs to the value space (i.e is valid
      * according to the type) of a given schema simpleType definition. Using Xerces API
      * 'XSSimpleType.validate' for this need.
      */
     public static boolean isValueValidForASimpleType(String value, XSSimpleType simplType) {
         
         boolean isValueValid = true;
         
         try {
             // construct necessary context objects
             ValidatedInfo validatedInfo = new ValidatedInfo();
             ValidationContext validationState = new ValidationState();
             
             // attempt to validate the "string value" with a simpleType instance.
             simplType.validate(value, validationState, validatedInfo);
         } 
         catch(InvalidDatatypeValueException ex){
             isValueValid = false;
         }
         
         return isValueValid;
         
     } // isValueValidForASimpleType    
     
 } // class XSTypeHelper
