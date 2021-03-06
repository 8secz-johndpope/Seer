 /*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements. See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership. The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License. You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied. See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 
 package org.apache.axiom.om;
 
 import javax.xml.namespace.QName;
 
 import org.apache.axiom.ext.stax.datahandler.DataHandlerProvider;
 
 /** Class OMFactory */
 public interface OMFactory {
     /**
      * Get the {@link OMMetaFactory} from which this factory was obtained. More precisely, if the
      * {@link OMFactory} instance has been obtained from a {@link OMMetaFactory} using
      * {@link OMMetaFactory#getOMFactory()}, {@link OMMetaFactory#getSOAP11Factory()} or
      * {@link OMMetaFactory#getSOAP12Factory()}, then the return value is the same as the original
      * {@link OMMetaFactory}. Since {@link OMAbstractFactory} creates a single {@link OMMetaFactory}
      * instance per Axiom implementation, this means that this method can be used to check if two
      * {@link OMFactory} instances belong to the same Axiom implementation.
      * 
      * @return the meta factory
      */
     OMMetaFactory getMetaFactory();
 
     /** Creates a new OMDocument. */
     OMDocument createOMDocument();
 
     OMDocument createOMDocument(OMXMLParserWrapper builder);
 
 
     /**
      * Create an element with the given name. If a namespace is given, a namespace declaration will
      * be added automatically to the newly created element.
      * 
      * @param localName
      *            the local part of the name; must not be <code>null</code>
      * @param ns
      *            the namespace, or <code>null</code> if the element has no namespace
      * @return the newly created element
      * @throws IllegalArgumentException
      *             if an attempt is made to create a prefixed element with an empty namespace name
      */
     OMElement createOMElement(String localName, OMNamespace ns);
 
     /**
      * Create an element with the given name and parent. If the specified {@link OMNamespace} has a
      * namespace URI but a <code>null</code> prefix, the method will reuse an existing prefix if a
      * namespace declaration with a matching namespace URI is in scope on the parent or generate a
      * new prefix if no such namespace declaration exists.
      * <p>
      * If a new prefix is generated or if the specified prefix and namespace URI are not bound in
      * the scope of the parent element, the method will add an appropriate namespace declaration to
      * the new element. Note that this may also occur if <code>null</code> is passed as
      * {@link OMNamespace} parameter. In that case, if there is a default namespace declaration with
      * a non empty namespace URI in the scope of the parent element, a namespace declaration needs
      * to be added to the newly created element to override the default namespace.
      * 
      * @param localName
      * @param ns
      * @param parent
      *            the parent to which the newly created element will be added; this may be
      *            <code>null</code>, in which case the behavior of the method is the same as
      *            {@link #createOMElement(String, OMNamespace)}
      * @return the newly created element
      * @throws OMException
      * @throws IllegalArgumentException
      *             if an attempt is made to create a prefixed element with an empty namespace name
      */
     OMElement createOMElement(String localName, OMNamespace ns, OMContainer parent)
             throws OMException;
 
     /**
      * @param localName
      * @param ns        - this can be null
      * @param parent
      * @param builder
      */
     OMElement createOMElement(String localName, OMNamespace ns,
                                      OMContainer parent,
                                      OMXMLParserWrapper builder);
 
     /**
      * Construct element with arbitrary data source. This is an optional operation which may not be
      * supported by all factories.
      *
      * @param source
      * @param localName
      * @param ns
      */
     OMSourcedElement createOMElement(OMDataSource source, String localName,
                                      OMNamespace ns);
 
     /**
      * Construct element with arbitrary data source. This is an optional operation which may not be
      * supported by all factories.
      *
      * @param source the data source
      * @param qname the name of the element produced by the data source
      */
     OMSourcedElement createOMElement(OMDataSource source, QName qname);
 
     /**
      * Create an element with the given name. If a namespace is given, a namespace declaration will
      * be added automatically to the newly created element.
      * 
      * @param localName
      *            the local part of the name; must not be <code>null</code>
      * @param namespaceURI
      *            the namespace URI, or the empty string if the element has no namespace; must not
      *            be <code>null</code>
      * @param prefix
      *            the namespace prefix, or <code>null</code> if a prefix should be generated
      * @return the newly created OMElement.
      * @throws IllegalArgumentException
      *             if <code>namespaceURI</code> is <code>null</code> or if an attempt is made to
      *             create a prefixed element with an empty namespace name
      */
     OMElement createOMElement(String localName,
                                      String namespaceURI,
                                      String prefix);
 
     /**
      * Create an element with the given {@link QName} and parent. If a namespace URI is given but no
      * prefix, the method will use an appropriate prefix if a corresponding namespace declaration is
      * in scope on the parent or generate a new prefix if no corresponding namespace declaration is
      * in scope. If a new prefix is generated or if the specified prefix and namespace URI are not
      * bound in the scope of the parent element, the method will add an appropriate namespace
      * declaration to the new element.
      * 
      * @param qname
      *            the {@link QName} defining the name of the element to be created
      * @param parent
      *            the parent to which the newly created element will be added; this may be
      *            <code>null</code>, in which case the behavior of the method is the same as
      *            {@link #createOMElement(QName)}
      * @return the new element
      * @throws IllegalArgumentException
      *             if an attempt is made to create a prefixed element with an empty namespace name
      */
     OMElement createOMElement(QName qname, OMContainer parent);
 
     /**
      * Create an element with the given {@link QName}. If a namespace URI is given but no prefix,
      * the method will automatically generate a prefix for the element. If a namespace URI is given,
      * the method will also add a namespace declaration to the element, binding the auto-generated
      * prefix or the prefix given in the {@link QName} to the given namespace URI. If neither a
      * namespace URI nor a prefix is given, no namespace declaration will be added.
      * 
      * @param qname
      *            the {@link QName} defining the name of the element to be created
      * @return the new element
      * @throws IllegalArgumentException
      *             if an attempt is made to create a prefixed element with an empty namespace name
      */
     OMElement createOMElement(QName qname);
 
     /**
      * Create an {@link OMNamespace} instance or retrieve an existing one if the factory supports
      * pooling.
      * 
      * @param uri
      *            the namespace URI; must not be <code>null</code>
      * @param prefix
      *            the prefix
      * @return the {@link OMNamespace} instance
      * @throws IllegalArgumentException
      *             if <code>uri</code> is null
      */
     OMNamespace createOMNamespace(String uri, String prefix);
 
     /**
      * Creates a new {@link OMText} node with the given value and appends it to the given parent
      * element.
      * 
      * @param parent
      * @param text
      * @return Returns OMText.
      */
     OMText createOMText(OMContainer parent, String text);
 
     /**
      * Create OMText node that is a copy of the source text node
      * @param parent
      * @param source
      * @return TODO
      */
     public OMText createOMText(OMContainer parent, OMText source);
     
     /**
      * @param parent
      * @param text   - This text itself can contain a namespace inside it.
      */
     OMText createOMText(OMContainer parent, QName text);
 
     /**
      * @param parent
      * @param text
      * @param type   - this should be either of XMLStreamConstants.CHARACTERS,
      *               XMLStreamConstants.CDATA, XMLStreamConstants.SPACE, XMLStreamConstants.ENTITY_REFERENCE
      * @return Returns OMText.
      */
     OMText createOMText(OMContainer parent, String text, int type);
 
     OMText createOMText(OMContainer parent, char[] charArary, int type);
 
     /**
      * @param parent
      * @param text   - This text itself can contain a namespace inside it.
      * @param type
      */
     OMText createOMText(OMContainer parent, QName text, int type);
 
     /**
      * @param s
      * @return Returns OMText.
      */
     OMText createOMText(String s);
 
     /**
      * @param s
      * @param type - OMText node can handle SPACE, CHARACTERS, CDATA and ENTITY REFERENCES. For
      *             Constants, use either XMLStreamConstants or constants found in OMNode.
      * @return Returns OMText.
      */
     OMText createOMText(String s, int type);
 
     OMText createOMText(String s, String mimeType, boolean optimize);
 
     OMText createOMText(Object dataHandler, boolean optimize);
 
     OMText createOMText(OMContainer parent, String s, String mimeType,
                                boolean optimize);
 
     /**
      * Create a binary {@link OMText} node supporting deferred loading of the content.
      * 
      * @param contentID
      *            the content ID identifying the binary content; may be <code>null</code>
      * @param dataHandlerProvider
      *            used to load the {@link javax.activation.DataHandler} when requested from the returned
      *            {@link OMText} node
      * @param optimize
      *            determines whether the binary content should be optimized
      * @return TODO
      */
     OMText createOMText(String contentID, DataHandlerProvider dataHandlerProvider,
             boolean optimize);
 
     OMText createOMText(String contentID, OMContainer parent,
                                OMXMLParserWrapper builder);
 
     /**
      * Create an attribute with the given name and value. If the provided {@link OMNamespace} object
      * has a <code>null</code> prefix, then a prefix will be generated, except if the namespace URI
      * is the empty string, in which case the result is the same as if a <code>null</code>
      * {@link OMNamespace} was given.
      * 
      * @param localName
      * @param ns
      * @param value
     * @return
      * @throws IllegalArgumentException
      *             if an attempt is made to create a prefixed attribute with an empty namespace name
      */
     OMAttribute createOMAttribute(String localName,
                                          OMNamespace ns,
                                          String value);
 
     /**
      * Creates DocType/DTD.
      *
      * @param parent
      * @param content
      * @return Returns doctype.
      */
     OMDocType createOMDocType(OMContainer parent, String content);
 
     /**
      * Creates a PI.
      *
      * @param parent
      * @param piTarget
      * @param piData
      * @return Returns OMProcessingInstruction.
      */
     OMProcessingInstruction createOMProcessingInstruction(OMContainer parent,
                                                                  String piTarget, String piData);
 
     /**
      * Creates a comment.
      *
      * @param parent
      * @param content
      * @return Returns OMComment.
      */
     OMComment createOMComment(OMContainer parent, String content);
 }
