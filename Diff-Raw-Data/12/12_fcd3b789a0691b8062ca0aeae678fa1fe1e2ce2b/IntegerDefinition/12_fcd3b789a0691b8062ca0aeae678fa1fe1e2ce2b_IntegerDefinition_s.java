 /*******************************************************************************
  * Copyright (c) 2011-2012 Ericsson, Ecole Polytechnique de Montreal and others
  *
  * All rights reserved. This program and the accompanying materials are made
  * available under the terms of the Eclipse Public License v1.0 which
  * accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors: Matthew Khouzam - Initial API and implementation
  * Contributors: Simon Marchi - Initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.linuxtools.ctf.core.event.types;
 
 import org.eclipse.linuxtools.internal.ctf.core.event.io.BitBuffer;
 
 /**
  * A CTF integer definition.
 * 
  * The definition of a integer basic data type. It will take the data
  * from a trace and store it (and make it fit) as a long.
  *
  * TODO: Reading integers with an endianness different from the trace endianness
  * is not supported
  *
  * @version 1.0
  * @author Matthew Khouzam
  * @author Simon Marchi
  */
 public class IntegerDefinition extends Definition {
 
     // ------------------------------------------------------------------------
     // Attributes
     // ------------------------------------------------------------------------
 
     private final IntegerDeclaration declaration;
     private long value;
 
     // ------------------------------------------------------------------------
     // Contructors
     // ------------------------------------------------------------------------
 
     /**
      * Constructor
      * @param declaration the parent declaration
      * @param definitionScope the parent scope
      * @param fieldName the field name
      */
     public IntegerDefinition(IntegerDeclaration declaration,
             IDefinitionScope definitionScope, String fieldName) {
         super(definitionScope, fieldName);
         this.declaration = declaration;
     }
 
     // ------------------------------------------------------------------------
     // Gettters/Setters/Predicates
     // ------------------------------------------------------------------------
 
     /**
      * Gets the value of the integer
      * @return the value of the integer (in long)
      */
     public long getValue() {
         return value;
     }
 
     /**
      * Sets the value of an integer
      * @param val the value
      */
     public void setValue(long val) {
         value = val;
     }
 
     @Override
     public IntegerDeclaration getDeclaration() {
         return declaration;
     }
 
     // ------------------------------------------------------------------------
     // Operations
     // ------------------------------------------------------------------------
 
     @Override
     public void read(BitBuffer input) {
         int align = (int) declaration.getAlignment();
         int pos = input.position() + ((align-(input.position() % align))%align);
         input.position(pos);
         boolean signed = declaration.isSigned();
         int length = declaration.getLength();
         long bits = 0;
 
         // TODO: use the eventual getLong from BitBuffer
 
         if (length == 64) {
             long low = input.getInt(32, false);
             low = low & 0x00000000FFFFFFFFL;
             long high = input.getInt(32, false);
             high = high & 0x00000000FFFFFFFFL;
 
             bits = (high << 32) | low;
         } else {
             bits = input.getInt(length, signed);
             bits = bits & 0x00000000FFFFFFFFL;
         }
 
         value = bits;
     }
 
     @Override
     public String toString() {
         if (declaration.isCharacter()) {
             char c = (char) value;
             return Character.toString(c);
         }
         return String.valueOf(value);
     }
 }
