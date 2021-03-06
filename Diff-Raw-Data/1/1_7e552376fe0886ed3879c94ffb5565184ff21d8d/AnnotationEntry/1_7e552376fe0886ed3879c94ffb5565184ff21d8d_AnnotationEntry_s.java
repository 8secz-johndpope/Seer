 /*
  * Copyright  2000-2009 The Apache Software Foundation
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  *
  */
 package org.apache.bcel.classfile;
 
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
 import java.io.IOException;
 import java.io.Serializable;
 import java.util.ArrayList;
 import java.util.List;
 
 import org.apache.bcel.Constants;
 
 /**
  * represents one annotation in the annotation table
  * 
  * @version $Id: AnnotationEntry
  * @author  <A HREF="mailto:dbrosius@mebigfatguy.com">D. Brosius</A>
  * @since 5.3
  */
 public class AnnotationEntry implements Node, Constants, Serializable {
 
     private int type_index;
     private int num_element_value_pairs;
     private List element_value_pairs;
     private ConstantPool constant_pool;
     private boolean isRuntimeVisible;
 
 
     /**
      * Construct object from file stream.
      * @param file Input stream
     * @throws IOException
      */
     public AnnotationEntry(int type_index, ConstantPool constant_pool, boolean isRuntimeVisible) {
         this.type_index = type_index;
         
         this.constant_pool = constant_pool;
         this.isRuntimeVisible = isRuntimeVisible;
     }
     
     public static AnnotationEntry read(DataInputStream file, ConstantPool constant_pool, boolean isRuntimeVisible) throws IOException 
     {
     	AnnotationEntry annotationEntry = new AnnotationEntry(file.readUnsignedShort(), constant_pool, isRuntimeVisible);
     	annotationEntry.num_element_value_pairs = (file.readUnsignedShort());
     	annotationEntry.element_value_pairs = new ArrayList();
         for (int i = 0; i < annotationEntry.num_element_value_pairs; i++) {
         	annotationEntry.element_value_pairs.add(new ElementValuePair(file.readUnsignedShort(), ElementValue.readElementValue(file, constant_pool), constant_pool));
         }
         return annotationEntry;
     }
 
 
     /**
      * Called by objects that are traversing the nodes of the tree implicitely
      * defined by the contents of a Java class. I.e., the hierarchy of methods,
      * fields, attributes, etc. spawns a tree of objects.
      *
      * @param v Visitor object
      */
     public void accept( Visitor v ) {
         //	    v.visitAnnotationEntry(this);
     }
 
 
     /**
      * @return the annotation type name
      */
     public String getAnnotationType() {
         ConstantUtf8 c;
         c = (ConstantUtf8) constant_pool.getConstant(type_index, CONSTANT_Utf8);
         return c.getBytes();
     }
     
     /**
      * @return the annotation type index
      */
     public int getAnnotationTypeIndex()
     {
     	return type_index;
     }
 
 
     /**
      * @return the number of element value pairs in this annotation entry
      */
     public final int getNumElementValuePairs() {
         return num_element_value_pairs;
     }
 
 
     /**
      * @return the element value pairs in this annotation entry
      */
     public ElementValuePair[] getElementValuePairs() {
     	// TOFO return List
         return (ElementValuePair[]) element_value_pairs.toArray(new ElementValuePair[element_value_pairs.size()]);
     }
 
 
 	public void dump(DataOutputStream dos) throws IOException
 	{
 		dos.writeShort(type_index);	// u2 index of type name in cpool
 		dos.writeShort(element_value_pairs.size()); // u2 element_value pair count
 		for (int i = 0 ; i<element_value_pairs.size();i++) {
 			ElementValuePair envp = (ElementValuePair) element_value_pairs.get(i);
 			envp.dump(dos);
 		}
 	}
 
 
 	public boolean isRuntimeVisible()
 	{
 		return isRuntimeVisible;
 	}
 
 	public void addElementNameValuePair(ElementValuePair elementNameValuePair)
 	{
 		element_value_pairs.add(elementNameValuePair);
 	}
 
 	public String toShortString()
 	{
 		StringBuffer result = new StringBuffer();
 		result.append("@");
 		result.append(getAnnotationType());
 		if (getElementValuePairs().length > 0)
 		{
 			result.append("(");
 			for (int i = 0; i < getElementValuePairs().length; i++)
 			{
 				ElementValuePair element = getElementValuePairs()[i];
 				result.append(element.toShortString());
 			}
 			result.append(")");
 		}
 		return result.toString();
 	}
 }
