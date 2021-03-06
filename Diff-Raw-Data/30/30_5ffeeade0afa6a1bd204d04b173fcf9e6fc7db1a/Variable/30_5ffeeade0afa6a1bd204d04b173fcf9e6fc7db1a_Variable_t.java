 /*
  $Id$
 
  Copyright 2003 (C) James Strachan and Bob Mcwhirter. All Rights Reserved.
 
  Redistribution and use of this software and associated documentation
  ("Software"), with or without modification, are permitted provided
  that the following conditions are met:
 
  1. Redistributions of source code must retain copyright
     statements and notices.  Redistributions must also contain a
     copy of this document.
 
  2. Redistributions in binary form must reproduce the
     above copyright notice, this list of conditions and the
     following disclaimer in the documentation and/or other
     materials provided with the distribution.
 
  3. The name "groovy" must not be used to endorse or promote
     products derived from this Software without prior written
     permission of The Codehaus.  For written permission,
     please contact info@codehaus.org.
 
  4. Products derived from this Software may not be called "groovy"
     nor may "groovy" appear in their names without prior written
     permission of The Codehaus. "groovy" is a registered
     trademark of The Codehaus.
 
  5. Due credit should be given to The Codehaus -
     http://groovy.codehaus.org/
 
  THIS SOFTWARE IS PROVIDED BY THE CODEHAUS AND CONTRIBUTORS
  ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
  NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
  THE CODEHAUS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
  STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
  OF THE POSSIBILITY OF SUCH DAMAGE.
 
  */
 package org.codehaus.groovy.classgen;
 
 import org.codehaus.groovy.ast.ClassHelper;
 import org.codehaus.groovy.ast.ClassNode;
 import org.objectweb.asm.Label;
 
 
 /**
  * Represents compile time variable metadata while compiling a method.
  * 
  * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
  * @version $Revision$
  */
 public class Variable {
     
     public static Variable THIS_VARIABLE = new Variable();
     public static Variable SUPER_VARIABLE = new Variable();
 
     private int index;
     private ClassNode type;
     private String name;
     private boolean holder;
     private boolean property;
 
     // br for setting on the LocalVariableTable in the class file
     // these fields should probably go to jvm Operand class
     private Label startLabel = null;
     private Label endLabel = null;
     private boolean dynamicTyped;
 
     private Variable(){
         dynamicTyped = true;
         index=0;
         holder=false;
         property=false;
     }
     
     public Variable(int index, ClassNode type, String name) {
         this.index = index;
         this.type = type;
         this.name = name;
     }
 
     public String getName() {
         return name;
     }
 
     public ClassNode getType() {
         return type;
     }
     
     public String getTypeName() {
         return type.getName();
     }
 
     /**
      * @return the stack index for this variable
      */
     public int getIndex() {
         return index;
     }
 
     /**
      * @return is this local variable shared in other scopes (and so must use a ValueHolder)
      */
     public boolean isHolder() {
         return holder;
     }
 
     public void setHolder(boolean holder) {
         this.holder = holder;
     }
 
     public boolean isProperty() {
         return property;
     }
 
     public void setProperty(boolean property) {
         this.property = property;
     }
     
     public Label getStartLabel() {
         return startLabel;
     }
 
     public void setStartLabel(Label startLabel) {
         this.startLabel = startLabel;
     }
 
     public Label getEndLabel() {
         return endLabel;
     }
 
     public void setEndLabel(Label endLabel) {
         this.endLabel = endLabel;
     }
 
     public String toString() {
         // TODO Auto-generated method stub
         return super.toString() + "[" + type + " " + name + " (" + index + ")";
     }
 
     public void setType(ClassNode type) {
         this.type = type;
         dynamicTyped |= type==ClassHelper.DYNAMIC_TYPE;
     }
 
     public void setDynamicTyped(boolean b) {
         dynamicTyped = b;
     }
     
     public boolean isDynamicTyped() {
         return dynamicTyped;
     }
 }
