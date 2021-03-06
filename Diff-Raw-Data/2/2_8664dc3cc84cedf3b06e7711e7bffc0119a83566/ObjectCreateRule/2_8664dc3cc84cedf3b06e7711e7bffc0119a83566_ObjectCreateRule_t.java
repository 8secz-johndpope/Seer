 /* $Id$
  *
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
 package org.apache.commons.digester3;
 
 import org.xml.sax.Attributes;
 
 /**
  * Rule implementation that creates a new object and pushes it
  * onto the object stack.  When the element is complete, the
  * object will be popped
  */
 public class ObjectCreateRule extends Rule {
 
     /**
      * The Java class name of the object to be created.
      */
     private final Class<?> type;
 
     /**
      * The attribute containing an override class name if it is present.
      */
     private final String attributeName; // @Nullable
 
     /**
      * 
      *
      * @param className The Java class name of the object to be created
      * @param attributeName The attribute containing an override class name if it is present
      */
     public ObjectCreateRule(Class<?> type, String attributeName) {
         this.type = type;
         this.attributeName = attributeName;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void begin(String namespace, String name, Attributes attributes) throws Exception {
         // Identify the name of the class to instantiate
         Class<?> clazz = this.type;
         if (this.attributeName != null) {
             String value = attributes.getValue(this.attributeName);
             if (value != null) {
                 clazz = this.getDigester().getClassLoader().loadClass(value);
             }
         }
         if (this.getDigester().getLog().isDebugEnabled()) {
             this.getDigester().getLog().debug(String.format("[ObjectCreateRule]{%s} New %s",
                     this.getDigester().getMatch(),
                     clazz.getName()));
         }
 
         if (clazz == null) {
             throw this.getDigester().createSAXException(String.format("[ObjectCreateRule]{%s} No type defined",
                     this.getDigester().getMatch()));
         }
 
         // Instantiate the new object and push it on the context stack
         Object instance = clazz.newInstance();
         this.getDigester().push(instance);
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void end(String namespace, String name) throws Exception {
         Object top = this.getDigester().pop();
 
         if (this.getDigester().getLog().isDebugEnabled()) {
             this.getDigester().getLog().debug(String.format("[ObjectCreateRule]{%s} Pop %s",
                     this.getDigester().getMatch(),
                     top.getClass().getName()));
         }
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public String toString() {
        return String.format("ObjectCreateRule[type=%s, attributeName=%s]", this.type.getName(), this.attributeName);
     }
 
 }
