 /*
  * Copyright 2005 John G. Wilson
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
  *
  */
 
 package org.codehaus.groovy.sandbox.util.slurpersupport;
 
 import groovy.lang.Closure;
 import groovy.lang.GroovyObject;
 import groovy.lang.GroovyRuntimeException;
 
 import java.io.IOException;
 import java.io.Writer;
 import java.util.Iterator;
 
 /**
  * @author John Wilson
  *
  */
 
 class NodeChildren extends GPathResult {
   private int size = -1;
   
   /**
    * @param parent
    * @param name
    * @param namespacePrefix
    */
   public NodeChildren(final GPathResult parent, final String name, final String namespacePrefix) {
     super(parent, name, namespacePrefix);
   }
   
   /**
    * @param parent
    * @param name
    */
   public NodeChildren(final GPathResult parent, final String name) {
     this(parent, name, "*");
   }
   
   /**
    * @param parent
    */
   public NodeChildren(final GPathResult parent) {
     this(parent, "*");
   }
 
   /* (non-Javadoc)
    * @see org.codehaus.groovy.sandbox.util.GPathResult#childNodes()
    */
   public Iterator childNodes() {
     return new Iterator() {
                 private final Iterator iter = NodeChildren.this.parent.childNodes();
                 private Iterator childIter = nextChildIter();
                 
                   /* (non-Javadoc)
                    * @see java.util.Iterator#hasNext()
                    */
                   public boolean hasNext() {
                     return this.childIter != null;
                   }
               
                   /* (non-Javadoc)
                    * @see java.util.Iterator#next()
                    */
                   public Object next() {
                     while (this.childIter != null) {
                       try {
                         if (this.childIter.hasNext()) {
                           return this.childIter.next();
                         }
                       } finally {
                         if (!this.childIter.hasNext()) {
                           this.childIter = nextChildIter();
                         }
                       }
                     }
                     
                     return null;
                   }
                   
                   /* (non-Javadoc)
                    * @see java.util.Iterator#remove()
                    */
                   public void remove() {
                     throw new UnsupportedOperationException();
                   }
                   
                   /**
                    * @return
                    */
                   private Iterator nextChildIter() {
                     while (this.iter.hasNext()) {
                     final Node node = (Node)this.iter.next();
                     
                       if (NodeChildren.this.name.equals(node.name())) {
                       final Iterator result = node.childNodes();
                       
                         if (result.hasNext()) {
                           if ("*".equals(NodeChildren.this.namespacePrefix) ||
                              ("".equals(NodeChildren.this.namespacePrefix) && "".equals(node.namespaceURI())) ||
                              node.namespaceURI().equals(NodeChildren.this.namespaceMap.get(NodeChildren.this.namespacePrefix)))
                           {
                             return result;
                           }
                         }
                       }
                     }
                     
                     return null;
                   }
     };
   }
 
   /* (non-Javadoc)
    * @see org.codehaus.groovy.sandbox.util.slurpersupport.GPathResult#iterator()
    */
   public Iterator iterator() {
     return new Iterator() {
       final Iterator iter = nodeIterator();
       
       public boolean hasNext() {
         return this.iter.hasNext();
       }
       
       public Object next() {
        return new NodeChild((Node)this.iter.next(), NodeChildren.this);
       }
       
       public void remove() {
         throw new UnsupportedOperationException();
       }
     };
   }
 
   /* (non-Javadoc)
    * @see org.codehaus.groovy.sandbox.util.GPathResult#iterator()
    */
   public Iterator nodeIterator() {
     if ("*".equals(this.name)) {
       return this.parent.childNodes();
     } else {
       return new NodeIterator(this.parent.childNodes()) {
                     /* (non-Javadoc)
                      * @see org.codehaus.groovy.sandbox.util.slurpersupport.NodeIterator#getNextNode(java.util.Iterator)
                      */
                     protected Object getNextNode(Iterator iter) {
                       while (iter.hasNext()) {
                         final Node node = (Node)iter.next();
                         
                           if (NodeChildren.this.name.equals(node.name())) {
                             if ("*".equals(NodeChildren.this.namespacePrefix) ||
                                 ("".equals(NodeChildren.this.namespacePrefix) && "".equals(node.namespaceURI())) ||
                                 node.namespaceURI().equals(NodeChildren.this.namespaceMap.get(NodeChildren.this.namespacePrefix)))
                              {
                                return node;
                              }
                           }
                         }
                         
                         return null;
                    }   
                   };
     }
   }
 
   /* (non-Javadoc)
    * @see org.codehaus.groovy.sandbox.util.GPathResult#parents()
    */
   public GPathResult parents() {
     // TODO Auto-generated method stub
     throw new GroovyRuntimeException("parents() not implemented yet");
   }
 
   /* (non-Javadoc)
    * @see org.codehaus.groovy.sandbox.util.GPathResult#size()
    */
   public synchronized int size() {
     if (this.size == -1) {
     final Iterator iter = nodeIterator();
     
       this.size = 0;
       while (iter.hasNext()) {
         iter.next();
         this.size++;
       }
     }
     
     return this.size;
   }
 
   /* (non-Javadoc)
    * @see org.codehaus.groovy.sandbox.util.GPathResult#text()
    */
   public String text() {
   final StringBuffer buf = new StringBuffer();
   final Iterator iter = nodeIterator();
   
     while (iter.hasNext()) {
       buf.append(((Node)iter.next()).text());
     }
     
     return buf.toString();
   }
 
   /* (non-Javadoc)
    * @see org.codehaus.groovy.sandbox.util.slurpersupport.GPathResult#find(groovy.lang.Closure)
    */
   public GPathResult find(final Closure closure) {
   final Iterator iter = iterator();
   
     while (iter.hasNext()) {
     final Object node = iter.next();
     final Boolean result = (Boolean)closure.call(new Object[]{node});
     
       if (result != null && result.booleanValue()) {
         return (GPathResult)node;
       }
     }
     
     return new NoChildren(this, this.name);
   }
 
   /* (non-Javadoc)
    * @see org.codehaus.groovy.sandbox.util.slurpersupport.GPathResult#findAll(groovy.lang.Closure)
    */
   public GPathResult findAll(final Closure closure) {
     return new FilteredNodeChildren(this, closure);
   }
 
   /* (non-Javadoc)
    * @see org.codehaus.groovy.sandbox.markup.Buildable#build(groovy.lang.GroovyObject)
    */
   public void build(final GroovyObject builder) {
   final Iterator iter = nodeIterator();
   
     while (iter.hasNext()) {
       ((Node)iter.next()).build(builder, this.namespaceMap);
     }
   }
 
   /* (non-Javadoc)
    * @see groovy.lang.Writable#writeTo(java.io.Writer)
    */
   public Writer writeTo(final Writer out) throws IOException {
   final Iterator iter = nodeIterator();
   
     while (iter.hasNext()) {
       ((Node)iter.next()).writeTo(out);
     }
     
     return out;
   }
 }
