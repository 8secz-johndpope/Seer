 /*
  * Copyright 1999-2004 The Apache Software Foundation
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.apache.commons.jxpath.ri;
 
 
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 
 import org.apache.commons.jxpath.Pointer;
 import org.apache.commons.jxpath.ri.model.NodeIterator;
 import org.apache.commons.jxpath.ri.model.NodePointer;
 
 /**
  * The reference implementation of JXPathContext.
  *
  * @author Dmitri Plotnikov
 * @version $Revision: 1.2 $ $Date: 2004/06/29 22:57:20 $
  */
 public class NamespaceResolver implements Cloneable {
     
     protected HashMap namespaceMap = new HashMap();
     protected HashMap reverseMap;
     protected NodePointer pointer;
     private boolean sealed;
     
     /**
      * Registers a namespace prefix.
      * 
      * @param prefix A namespace prefix
      * @param namespaceURI A URI for that prefix
      */
     public void registerNamespace(String prefix, String namespaceURI) {
         namespaceMap.put(prefix, namespaceURI);
        reverseMap = null;
     }
     
     /**
      * Register a namespace for the expression context.
      */
     public void setNamespaceContextPointer(NodePointer pointer) {
         this.pointer = pointer;
     }
     
     public Pointer getNamespaceContextPointer() {
         return pointer;
     }
     
     /**
      * Given a prefix, returns a registered namespace URI. If the requested
      * prefix was not defined explicitly using the registerNamespace method,
      * JXPathContext will then check the context node to see if the prefix is
      * defined there. See
      * {@link #setNamespaceContextPointer(Pointer) setNamespaceContextPointer}.
      * 
      * @param prefix The namespace prefix to look up
      * @return namespace URI or null if the prefix is undefined.
      */
     public String getNamespaceURI(String prefix) {
         String uri = (String) namespaceMap.get(prefix);
         if (uri == null && pointer != null) {
             uri = pointer.getNamespaceURI(prefix);
         }
 //        System.err.println("For prefix " + prefix + " URI=" + uri);
         return uri;
     }
     
     public String getPrefix(String namespaceURI) {
         if (reverseMap == null) {
             reverseMap = new HashMap();
             NodeIterator ni = pointer.namespaceIterator();
             if (ni != null) {
                 for (int position = 1; ni.setPosition(position); position++) {
                     NodePointer nsPointer = ni.getNodePointer();
                     QName qname = nsPointer.getName();
                     reverseMap.put(qname.getPrefix(), qname.getName());
                 }
             }
             Iterator it = namespaceMap.entrySet().iterator();
             while (it.hasNext()) {
                 Map.Entry entry = (Map.Entry) it.next();
                 reverseMap.put(entry.getValue(), entry.getKey());
             }
         }
         String prefix = (String) reverseMap.get(namespaceURI);
         return prefix;
     }
     
     public boolean isSealed() {
         return sealed;
     }
     
     public void seal() {
         sealed = true;
     }
     
     public Object clone() {
         try {
             return super.clone();
         }
         catch (CloneNotSupportedException e) {
             // Of course, it's supported.
             e.printStackTrace();
             return null;
         }
     }
 }
