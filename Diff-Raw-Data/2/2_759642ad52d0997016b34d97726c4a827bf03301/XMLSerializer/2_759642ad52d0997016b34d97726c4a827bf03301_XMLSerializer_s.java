 /*
  * The contents of this file are subject to the terms
  * of the Common Development and Distribution License
  * (the "License").  You may not use this file except
  * in compliance with the License.
  * 
  * You can obtain a copy of the license at
  * https://jwsdp.dev.java.net/CDDLv1.0.html
  * See the License for the specific language governing
  * permissions and limitations under the License.
  * 
  * When distributing Covered Code, include this CDDL
  * HEADER in each file and include the License file at
  * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
  * add the following below this CDDL HEADER, with the
  * fields enclosed by brackets "[]" replaced with your
  * own identifying information: Portions Copyright [yyyy]
  * [name of copyright owner]
  */
 
 package com.sun.xml.bind.v2.runtime;
 
 import java.io.IOException;
 import java.lang.reflect.Method;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
 
 import javax.activation.MimeType;
 import javax.xml.bind.DatatypeConverter;
 import javax.xml.bind.JAXBException;
 import javax.xml.bind.Marshaller;
 import javax.xml.bind.ValidationEvent;
 import javax.xml.bind.ValidationEventHandler;
 import javax.xml.bind.ValidationEventLocator;
 import javax.xml.bind.annotation.DomHandler;
 import javax.xml.bind.annotation.XmlSchemaType;
 import javax.xml.bind.attachment.AttachmentMarshaller;
 import javax.xml.bind.helpers.NotIdentifiableEventImpl;
 import javax.xml.bind.helpers.ValidationEventImpl;
 import javax.xml.bind.helpers.ValidationEventLocatorImpl;
 import javax.xml.namespace.QName;
 import javax.xml.stream.XMLStreamException;
 import javax.xml.transform.Source;
 import javax.xml.transform.Transformer;
 import javax.xml.transform.TransformerException;
 import javax.xml.transform.sax.SAXResult;
 
 import com.sun.istack.SAXException2;
 import com.sun.xml.bind.api.AccessorException;
 import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
 import com.sun.xml.bind.util.ValidationEventLocatorExImpl;
 import com.sun.xml.bind.v2.WellKnownNamespace;
 import com.sun.xml.bind.v2.model.runtime.RuntimeBuiltinLeafInfo;
 import com.sun.xml.bind.v2.runtime.output.MTOMXmlOutput;
 import com.sun.xml.bind.v2.runtime.output.NamespaceContextImpl;
 import com.sun.xml.bind.v2.runtime.output.Pcdata;
 import com.sun.xml.bind.v2.runtime.output.XmlOutput;
 import com.sun.xml.bind.v2.runtime.unmarshaller.Base64Data;
 import com.sun.xml.bind.v2.runtime.unmarshaller.IntData;
 
 import org.xml.sax.SAXException;
 
 /**
  * Receives XML serialization event and writes to {@link XmlOutput}.
  * 
  * <p>
  * This object coordinates the overall marshalling efforts across different
  * content-tree objects and different target formats.
  * 
  * <p>
  * The following CFG gives the proper sequence of method invocation.
  * 
  * <pre>
  * MARSHALLING  :=  ELEMENT
  * ELEMENT      :=  "startElement" NSDECL* "endNamespaceDecls"
  *                        ATTRIBUTE* "endAttributes" BODY "endElement"
  * 
  * NSDECL       :=  "declareNamespace"
  * 
  * ATTRIBUTE    :=  "attribute"
  * ATTVALUES    :=  "text"*
  * 
  * 
  * BODY         :=  ( "text" | ELEMENT )*
  * </pre>
  * 
  * <p>
  * A marshalling of one element consists of two stages. The first stage is
  * for marshalling attributes and collecting namespace declarations.
  * The second stage is for marshalling characters/child elements of that element.
  * 
  * <p>
  * Observe that multiple invocation of "text" is allowed.
  * 
  * <p>
  * Also observe that the namespace declarations are allowed only between
  * "startElement" and "endAttributes".
  * 
  * <h2>Exceptions in marshaller</h2>
  * <p>
  * {@link IOException}, {@link SAXException}, and {@link XMLStreamException}
  * are thrown from {@link XmlOutput}. They are always considered fatal, and
  * therefore caught only by {@link MarshallerImpl}.
  * <p>
  * {@link AccessorException} can be thrown when an access to a property/field
  * fails, and this is considered as a recoverable error, so it's caught everywhere.
  *
  * @author  Kohsuke Kawaguchi
  */
 public final class XMLSerializer extends Coordinator {
     public final JAXBContextImpl grammar;
 
     /** Object currently marshalling itself. */
     public Object currentTarget;
 
     /** The XML printer. */
     private XmlOutput out;
 
     // TODO: fix the access modifier
     public final NameList nameList;
 
     // TODO: fix the access modifier
     public final int[] knownUri2prefixIndexMap;
 
     private final NamespaceContextImpl nsContext;
 
     private NamespaceContextImpl.Element nse;
 
     /**
      * Set to true if a text is already written,
      * and we need to print ' ' for additional text methods.
      */
     private boolean textHasAlreadyPrinted = false;
 
     /**
      * Set to false once we see the start tag of the root element.
      */
     private boolean seenRoot = false;
 
     /** Marshaller object to which this object belongs. */
     private final MarshallerImpl marshaller;
 
     /** Objects referenced through IDREF. */
     private final Set<Object> idReferencedObjects = new HashSet<Object>();
 
     /** Objects with ID. */
     private final Set<Object> objectsWithId = new HashSet<Object>();
 
     /** Optional attributes to go with root element. */
     private String schemaLocation;
     private String noNsSchemaLocation;
 
     /** Lazily created identitiy transformer. */
     private Transformer identityTransformer;
 
     /** Lazily created. */
     private ContentHandlerAdaptor contentHandlerAdapter;
 
     private boolean fragment;
 
     /**
      * Cached instance of {@link Base64Data}.
      */
     private Base64Data base64Data;
 
     /**
      * Cached instance of {@link IntData}.
      */
     private final IntData intData = new IntData();
 
     public AttachmentMarshaller attachmentMarshaller;
 
 
     /*package*/ XMLSerializer( MarshallerImpl _owner ) {
         this.marshaller = _owner;
         this.grammar = marshaller.context;
         nsContext = new NamespaceContextImpl(this);
         nameList = marshaller.context.nameList;
         knownUri2prefixIndexMap = new int[nameList.namespaceURIs.length];
     }
 
     /**
      * Gets the cached instance of {@link Base64Data}.
      */
     public Base64Data getCachedBase64DataInstance() {
         if(base64Data==null)
             base64Data = new Base64Data();
         return base64Data;
     }
 
     /**
      * Gets the ID value from an identifiable object.
      */
     private String getIdFromObject(Object identifiableObject) throws SAXException, JAXBException {
         return grammar.getBeanInfo(identifiableObject,true).getId(identifiableObject,this);
     }
 
     private void handleMissingObjectError(String fieldName) throws SAXException, IOException, XMLStreamException {
         reportMissingObjectError(fieldName);
         // as a marshaller, we should be robust, so we'll continue to marshal
         // this document by skipping this missing object.
         endNamespaceDecls(null);
         endAttributes();
     }
 
 
     public void reportError( ValidationEvent ve ) throws SAXException {
         ValidationEventHandler handler;
 
         try {
             handler = marshaller.getEventHandler();
         } catch( JAXBException e ) {
             throw new SAXException2(e);
         }
 
         if(!handler.handleEvent(ve)) {
             if(ve.getLinkedException() instanceof Exception)
                 throw new SAXException2((Exception)ve.getLinkedException());
             else
                 throw new SAXException2(ve.getMessage());
         }
     }
 
     /**
      * Report an error found as an exception.
      *
      * @param fieldName
      *      the name of the property being processed when an error is found.
      */
     public final void reportError(String fieldName, Throwable t) throws SAXException {
         ValidationEvent ve = new ValidationEventImpl(ValidationEvent.ERROR,
             t.getMessage(), getCurrentLocation(fieldName), t);
         reportError(ve);
     }
 
     public void startElement(Name tagName, Object outerPeer) {
         startElement();
         nse.setTagName(tagName,outerPeer);
     }
 
     public void startElement(String nsUri, String localName, String preferredPrefix, Object outerPeer) {
         startElement();
         int idx = nsContext.declareNsUri(nsUri, preferredPrefix, false);
         nse.setTagName(idx,localName,outerPeer);
     }
 
     public void endNamespaceDecls(Object innerPeer) throws IOException, XMLStreamException {
         nsContext.collectionMode = false;
         nse.startElement(out,innerPeer);
     }
 
     /**
      * Switches to the "marshal child texts/elements" mode.
      * This method has to be called after the 1st pass is completed.
      */
     public void endAttributes() throws SAXException, IOException, XMLStreamException  {
         if(!seenRoot) {
             seenRoot = true;
             if(schemaLocation!=null || noNsSchemaLocation!=null) {
                 int p = nsContext.getPrefixIndex(WellKnownNamespace.XML_SCHEMA_INSTANCE);
                 if(schemaLocation!=null)
                     out.attribute(p,"schemaLocation",schemaLocation);
                 if(noNsSchemaLocation!=null)
                     out.attribute(p,"noNamespaceSchemaLocation",noNsSchemaLocation);
             }
         }
 
         out.endStartTag();
     }
     
     /**
      * Ends marshalling of an element.
      * Pops the internal stack.
      */
     public void endElement() throws SAXException, IOException, XMLStreamException {
         nse.endElement(out);
         nse = nse.pop();
         textHasAlreadyPrinted = false;
     }
 
     public void leafElement( Name tagName, String data, String fieldName ) throws SAXException, IOException, XMLStreamException {
         if(seenRoot) {
             textHasAlreadyPrinted = false;
             nse = nse.push();
             out.beginStartTag(tagName);
             out.endStartTag();
             out.text(data,false);
             out.endTag(tagName);
             nse = nse.pop();
         } else {
             // root element has additional processing like xsi:schemaLocation,
             // so we need to go the slow way
             startElement(tagName,null);
             endNamespaceDecls(null);
             endAttributes();
             out.text(data,false);
             endElement();
         }
     }
 
     public void leafElement( Name tagName, Pcdata data, String fieldName ) throws SAXException, IOException, XMLStreamException {
         if(seenRoot) {
             textHasAlreadyPrinted = false;
             nse = nse.push();
             out.beginStartTag(tagName);
             out.endStartTag();
             out.text(data,false);
             out.endTag(tagName);
             nse = nse.pop();
         } else {
             // root element has additional processing like xsi:schemaLocation,
             // so we need to go the slow way
             startElement(tagName,null);
             endNamespaceDecls(null);
             endAttributes();
             out.text(data,false);
             endElement();
         }
     }
 
     public void leafElement( Name tagName, int data, String fieldName ) throws SAXException, IOException, XMLStreamException {
         intData.reset(data);
         leafElement(tagName,intData,fieldName);
     }
 
     // TODO: consider some of these in future if we expand the writer to use something other than SAX
 //    void leafElement( QName tagName, byte value, String fieldName ) throws SAXException;
 //    void leafElement( QName tagName, char value, String fieldName ) throws SAXException;
 //    void leafElement( QName tagName, short value, String fieldName ) throws SAXException;
 //    void leafElement( QName tagName, int value, String fieldName ) throws SAXException;
 //    void leafElement( QName tagName, long value, String fieldName ) throws SAXException;
 //    void leafElement( QName tagName, float value, String fieldName ) throws SAXException;
 //    void leafElement( QName tagName, double value, String fieldName ) throws SAXException;
 //    void leafElement( QName tagName, boolean value, String fieldName ) throws SAXException;
 
 
     /**
      * Marshalls text.
      *
      * <p>
      * This method can be called after the {@link #endAttributes()}
      * method to marshal texts inside elements.
      * If the method is called more than once, those texts are considered
      * as separated by whitespaces. For example,
      *
      * <pre>
      * c.startElement("","foo");
      * c.endAttributes();
      * c.text("abc");
      * c.text("def");
      *   c.startElement("","bar");
      *   c.endAttributes();
      *   c.endElement();
      * c.text("ghi");
      * c.endElement();
      * </pre>
      *
      * will generate <code>&lt;foo>abc def&lt;bar/>ghi&lt;/foo></code>.
      */
     public void text( String text, String fieldName ) throws SAXException, IOException, XMLStreamException {
         // If the assertion fails, it must be a bug of xjc.
         // right now, we are not expecting the text method to be called.
         if(text==null) {
             reportMissingObjectError(fieldName);
             return;
         }
 
         out.text(text,textHasAlreadyPrinted);
         textHasAlreadyPrinted = true;
     }
 
     /**
      * The {@link #text(String, String)} method that takes {@link Pcdata}.
      */
     public void text( Pcdata text, String fieldName ) throws SAXException, IOException, XMLStreamException {
         // If the assertion fails, it must be a bug of xjc.
         // right now, we are not expecting the text method to be called.
         if(text==null) {
             reportMissingObjectError(fieldName);
             return;
         }
 
         out.text(text,textHasAlreadyPrinted);
         textHasAlreadyPrinted = true;
     }
 
     public void attribute(String uri, String local, String value) throws SAXException {
         int prefix;
         if(uri.length()==0) {
             // default namespace. don't need prefix
             prefix = -1;
         } else {
             prefix = nsContext.getPrefixIndex(uri);
         }
 
         try {
             out.attribute(prefix,local,value);
         } catch (IOException e) {
             throw new SAXException2(e);
         } catch (XMLStreamException e) {
             throw new SAXException2(e);
         }
     }
 
     public void attribute(Name name, CharSequence value) throws IOException, XMLStreamException {
         // TODO: consider having the version that takes Pcdata.
         // it's common for an element to have int attributes
         out.attribute(name,value.toString());
     }
 
     public NamespaceContext2 getNamespaceContext() {
         return nsContext;
     }
     
     
     public String onID( Object owner, String value ) {
         objectsWithId.add(owner);
         return value;
     }
     
     public String onIDREF( Object obj ) throws SAXException {
         String id;
         try {
             id = getIdFromObject(obj);
         } catch (JAXBException e) {
             reportError(null,e);
             return null; // recover by returning null
         }
         idReferencedObjects.add(obj);
         if(id==null) {
             reportError( new NotIdentifiableEventImpl(
                 ValidationEvent.ERROR,
                 Messages.NOT_IDENTIFIABLE.format(),
                 new ValidationEventLocatorImpl(obj) ) );
         }
         return id;
     }
     
     
     // TODO: think about the exception handling.
     // I suppose we don't want to use SAXException. -kk
 
     public void childAsRoot(Object obj) throws JAXBException, IOException, SAXException, XMLStreamException {
         Object old = currentTarget;
         currentTarget = obj;
 
         final JaxBeanInfo beanInfo = grammar.getBeanInfo(obj, true);
 
         final boolean lookForLifecycleMethods = beanInfo.lookForLifecycleMethods();
         if (lookForLifecycleMethods) {
             fireBeforeMarshalEvents(beanInfo, currentTarget);
         }
 
         beanInfo.serializeRoot(obj,this);
 
         if (lookForLifecycleMethods) {
             fireAfterMarshalEvents(beanInfo, currentTarget);
         }
 
         currentTarget = old;
     }
 
     /**
      * The equivalent of:
      * 
      * <pre>
      * childAsURIs(child, fieldName);
      * endNamespaceDecls();
      * childAsAttributes(child, fieldName);
      * endAttributes();
      * childAsBody(child, fieldName);
      * </pre>
      * 
      * This produces the given child object as the sole content of
      * an element.
      * Used to reduce the code size in the generated marshaller.
      */
     public final void childAsSoleContent( Object child, String fieldName) throws SAXException, IOException, XMLStreamException {
         if(child==null) {
             handleMissingObjectError(fieldName);
         } else {
             JaxBeanInfo beanInfo;
             try {
                 beanInfo = grammar.getBeanInfo(child,true);
             } catch (JAXBException e) {
                 reportError(fieldName,e);
                 // recover by ignore
                 endNamespaceDecls(null);
                 endAttributes();
                 return;
             }
 
             Object oldTarget = currentTarget;
             currentTarget = child;
 
             final boolean lookForLifecycleMethods = beanInfo.lookForLifecycleMethods();
             if (lookForLifecycleMethods) {
                 fireBeforeMarshalEvents(beanInfo, currentTarget);
             }
 
             beanInfo.serializeURIs(child,this);
             endNamespaceDecls(child);
             beanInfo.serializeAttributes(child,this);
             endAttributes();
             beanInfo.serializeBody(child,this);
 
             if (lookForLifecycleMethods) {
                 fireAfterMarshalEvents(beanInfo, currentTarget);
             }
 
             currentTarget = oldTarget;
         }
     }
 
 
     // the version of childAsXXX where it produces @xsi:type if the expected type name
     // and the actual type name differs.
     
     /**
      * This method is called when a type child object is found.
      * 
      * <p>
      * This method produces events of the following form:
      * <pre>
      * NSDECL* "endNamespaceDecls" ATTRIBUTE* "endAttributes" BODY
      * </pre>
      * optionally including @xsi:type if necessary.
      * 
      * @param child
      *      Object to be marshalled. The {@link JaxBeanInfo} for
      *      this object must return a type name.
      * @param expected
      *      Expected type of the object.
      * @param fieldName
      *      property name of the parent objeect from which 'o' comes.
      *      Used as a part of the error message in case anything goes wrong
      *      with 'o'. 
      */
     public final void childAsXsiType( Object child, String fieldName, JaxBeanInfo expected ) throws SAXException, IOException, XMLStreamException {
         if(child==null) {
             handleMissingObjectError(fieldName);
         } else {
             boolean asExpected = child.getClass()==expected.jaxbType;
             JaxBeanInfo actual = expected;
 
             Object oldTarget = currentTarget;
             currentTarget = child;
 
             if((asExpected) && (actual.lookForLifecycleMethods())) {
                 fireBeforeMarshalEvents(actual, currentTarget);
             }
 
             if(!asExpected) {
                 try {
                     actual = grammar.getBeanInfo(child,true);
                     if (actual.lookForLifecycleMethods()) {
                         fireBeforeMarshalEvents(actual, currentTarget);
                     }
                 } catch (JAXBException e) {
                     reportError(fieldName,e);
                     endNamespaceDecls(null);
                     endAttributes();
                     return; // recover by ignore
                 }
                 if(actual==expected)
                     asExpected = true;
                 else {
                     if(actual.typeName==null) {
                         reportError(new ValidationEventImpl(
                                 ValidationEvent.ERROR,
                                 Messages.SUBSTITUTED_BY_ANONYMOUS_TYPE.format(
                                     expected.jaxbType.getName(),
                                     child.getClass().getName(),
                                     actual.jaxbType.getName()),
                                 getCurrentLocation(fieldName)));
                         // recover by not printing @xsi:type
                     } else {
                         getNamespaceContext().declareNamespace(WellKnownNamespace.XML_SCHEMA_INSTANCE,"xsi",true);
                         getNamespaceContext().declareNamespace(actual.typeName.getNamespaceURI(),null,false);
                     }
                 }
             }
             actual.serializeURIs(child,this);
             endNamespaceDecls(child);
             if(!asExpected) {
                 attribute(WellKnownNamespace.XML_SCHEMA_INSTANCE,"type",
                     DatatypeConverter.printQName(actual.typeName,getNamespaceContext()));
             }
             actual.serializeAttributes(child,this);
             endAttributes();
             actual.serializeBody(child,this);
 
             if (actual.lookForLifecycleMethods()) {
                 fireAfterMarshalEvents(actual, currentTarget);
             }
 
             currentTarget = oldTarget;
         }
     }
 
     /**
      * Invoke the afterMarshal api on the external listener (if it exists) and on the bean embedded
      * afterMarshal api(if it exists).
      *
      * This method is called only after the callee has determined that beanInfo.lookForLifecycleMethods == true.
      *
      * @param beanInfo
      * @param currentTarget
      */
     private void fireAfterMarshalEvents(final JaxBeanInfo beanInfo, Object currentTarget) {
         // first invoke bean embedded listener
         if (beanInfo.hasAfterMarshalMethod()) {
             Method m = beanInfo.getLifecycleMethods().getAfterMarshal();
             fireMarshalEvent(currentTarget, m);
         }
 
         // then invoke external listener before bean embedded listener
         Marshaller.Listener externalListener = marshaller.getListener();
         if (externalListener != null) {
             externalListener.afterMarshal(currentTarget);
         }
 
     }
 
     /**
      * Invoke the beforeMarshal api on the external listener (if it exists) and on the bean embedded
      * beforeMarshal api(if it exists).
      *
      * This method is called only after the callee has determined that beanInfo.lookForLifecycleMethods == true.
      *
      * @param beanInfo
      * @param currentTarget
      */
     private void fireBeforeMarshalEvents(final JaxBeanInfo beanInfo, Object currentTarget) {
         // first invoke bean embedded listener
         if (beanInfo.hasBeforeMarshalMethod()) {
             Method m = beanInfo.getLifecycleMethods().getBeforeMarshal();
             fireMarshalEvent(currentTarget, m);
         }
 
         // then invoke external listener
         Marshaller.Listener externalListener = marshaller.getListener();
         if (externalListener != null) {
             externalListener.beforeMarshal(currentTarget);
         }
     }
 
     private void fireMarshalEvent(Object target, Method m) {
         Object[] params = new Object[] { marshaller };
         try {
             m.invoke(target, params);
         } catch (Exception e) {
             // this really only happens if there is a bug in the ri
             throw new IllegalStateException(e);
         }
     }
 
     public void attWildcardAsURIs(Map<QName,String> attributes, String fieldName) {
         if(attributes==null)    return;
         for( Map.Entry<QName,String> e : attributes.entrySet() ) {
             QName n = e.getKey();
             String nsUri = n.getNamespaceURI();
             if(nsUri.length()>0) {
                 String p = n.getPrefix();
                 if(p.length()==0)   p=null;
                 nsContext.declareNsUri(nsUri, p, true);
             }
         }
     }
 
     public void attWildcardAsAttributes(Map<QName,String> attributes, String fieldName) throws SAXException {
         if(attributes==null)    return;
         for( Map.Entry<QName,String> e : attributes.entrySet() ) {
             QName n = e.getKey();
             attribute(n.getNamespaceURI(),n.getLocalPart(),e.getValue());
         }
     }
 
     /**
      * Short for the following call sequence:
      *
      * <pre>
          getNamespaceContext().declareNamespace(WellKnownNamespace.XML_SCHEMA_INSTANCE,"xsi",true);
          endNamespaceDecls();
          attribute(WellKnownNamespace.XML_SCHEMA_INSTANCE,"nil","true");
          endAttributes();
      * </pre>
      */
     public final void writeXsiNilTrue() throws SAXException, IOException, XMLStreamException {
         getNamespaceContext().declareNamespace(WellKnownNamespace.XML_SCHEMA_INSTANCE,"xsi",true);
         endNamespaceDecls(null);
         attribute(WellKnownNamespace.XML_SCHEMA_INSTANCE,"nil","true");
         endAttributes();
     }
 
     public <E> void writeDom(E element, DomHandler<E, ?> domHandler, Object parentBean, String fieldName) throws SAXException {
         Source source = domHandler.marshal(element,this);
         if(contentHandlerAdapter==null)
             contentHandlerAdapter = new ContentHandlerAdaptor(this);
         try {
             getIdentityTransformer().transform(source,new SAXResult(contentHandlerAdapter));
         } catch (TransformerException e) {
             reportError(fieldName,e);
         }
     }
 
     public Transformer getIdentityTransformer() {
         if(identityTransformer==null)
             identityTransformer = JAXBContextImpl.createTransformer();
         return identityTransformer;
     }
 
     public void setPrefixMapper(NamespacePrefixMapper prefixMapper) {
         nsContext.setPrefixMapper(prefixMapper);
     }
 
     /**
      * Reset this object to write to the specified output.
      *
      * @param schemaLocation
      *      if non-null, this value is printed on the root element as xsi:schemaLocation
      * @param noNsSchemaLocation
      *      Similar to 'schemaLocation' but this one works for xsi:noNamespaceSchemaLocation
      */
     public void startDocument(XmlOutput out,boolean fragment,String schemaLocation,String noNsSchemaLocation) throws IOException, SAXException, XMLStreamException {
         setThreadAffinity();
         pushCoordinator();
         nsContext.reset();
         nse = nsContext.getCurrent();
         if(attachmentMarshaller!=null && attachmentMarshaller.isXOPPackage())
             out = new MTOMXmlOutput(out);
         this.out = out;
         objectsWithId.clear();
         idReferencedObjects.clear();
         textHasAlreadyPrinted = false;
         seenRoot = false;
         this.schemaLocation = schemaLocation;
         this.noNsSchemaLocation = noNsSchemaLocation;
         this.fragment = fragment;
         this.inlineBinaryFlag = false;
         this.expectedMimeType = null;
 
         out.startDocument(this,fragment,knownUri2prefixIndexMap,nsContext);
     }
 
     public void endDocument() throws IOException, SAXException, XMLStreamException {
         out.endDocument(fragment);
         currentTarget = null; // avoid memory leak
     }
 
     public void close() {
         popCoordinator();
         resetThreadAffinity();
     }
 
     /**
      * This method can be called after {@link #startDocument} is called
      * but before the marshalling begins, to set the currently in-scope namespace
      * bindings.
      *
      * <p>
      * This method is useful to avoid redundant namespace declarations when
      * the marshalling is producing a sub-document.
      */
     public void addInscopeBinding(String nsUri,String prefix) {
         nsContext.put(nsUri,prefix);
     }
 
     /**
      * Gets the MIME type with which the binary content shall be printed.
      *
      * <p>
      * This method shall be used from those {@link RuntimeBuiltinLeafInfo} that are
      * bound to base64Binary.
      *
      * @see JAXBContextImpl#getXMIMEContentType(Object)
      */
     public String getXMIMEContentType() {
         // xmime:contentType takes precedence
         String v = grammar.getXMIMEContentType(currentTarget);
         if(v!=null)     return v;
 
         // then look for the current in-scope @XmlMimeType
         if(expectedMimeType!=null)
             return expectedMimeType.toString();
 
         return null;
     }
 
     private void startElement() {
         nse = nse.push();
 
         if( !seenRoot ) {
             // seenRoot set to true in endAttributes
             // first declare all known URIs
             String[] knownUris = nameList.namespaceURIs;
             for( int i=0; i<knownUris.length; i++ )
                 knownUri2prefixIndexMap[i] = nsContext.declareNsUri(knownUris[i], null, false);
 
             // then declare user-specified namespace URIs.
             // work defensively. we are calling an user-defined method.
             String[] uris = nsContext.getPrefixMapper().getPreDeclaredNamespaceUris();
             if( uris!=null ) {
                 for (String uri : uris) {
                     if (uri != null)
                         nsContext.declareNsUri(uri, null, false);
                 }
             }
             String[] pairs = nsContext.getPrefixMapper().getPreDeclaredNamespaceUris2();
             if( pairs!=null ) {
                for( int i=0; i<pairs.length; i++ ) {
                     String prefix = pairs[i];
                     String nsUri = pairs[i+1];
                     if(prefix!=null && nsUri!=null)
                         // in this case, we don't want the redundant binding consolidation
                         // to happen (such as declaring the same namespace URI twice with
                         // different prefixes.) Hence we call the put method directly.
                         nsContext.put(nsUri,prefix);
                 }
             }
 
             if(schemaLocation!=null || noNsSchemaLocation!=null) {
                 nsContext.declareNsUri(WellKnownNamespace.XML_SCHEMA_INSTANCE,"xsi",true);
             }
         }
 
         nsContext.collectionMode = true;
         textHasAlreadyPrinted = false;
     }
 
     private MimeType expectedMimeType;
 
     /**
      * This method is used by {@link MimeTypedTransducer} to set the expected MIME type
      * for the encapsulated {@link Transducer}.
      */
     public MimeType setExpectedMimeType(MimeType expectedMimeType) {
         MimeType old = this.expectedMimeType;
         this.expectedMimeType = expectedMimeType;
         return old;
     }
 
     /**
      * True to force inlining.
      */
     private boolean inlineBinaryFlag;
 
     public boolean setInlineBinaryFlag(boolean value) {
         boolean old = inlineBinaryFlag;
         this.inlineBinaryFlag = value;
         return old;
     }
 
     public boolean getInlineBinaryFlag() {
         return inlineBinaryFlag;
     }
 
     /**
      * Field used to support an {@link XmlSchemaType} annotation.
      *
      * <p>
      * When we are marshalling a property with an effective {@link XmlSchemaType},
      * this field is set to hold the QName of that type. The {@link Transducer} that
      * actually converts a Java object into XML can look this property to decide
      * how to marshal the value.
      */
     private QName schemaType;
 
     public QName setSchemaType(QName st) {
         QName old = schemaType;
         schemaType = st;
         return old;
     }
 
     public QName getSchemaType() {
         return schemaType;
     }
 
     void reconcileID() throws SAXException {
         // find objects that were not a part of the object graph
         idReferencedObjects.removeAll(objectsWithId);
 
         for( Object idObj : idReferencedObjects ) {
             try {
                 String id = getIdFromObject(idObj);
                 reportError( new NotIdentifiableEventImpl(
                     ValidationEvent.ERROR,
                     Messages.DANGLING_IDREF.format(id),
                     new ValidationEventLocatorImpl(idObj) ) );
             } catch (JAXBException e) {
                 // this error should have been reported already. just ignore here.
             }
         }
 
         // clear the garbage
         idReferencedObjects.clear();
         objectsWithId.clear();
     }
 
     public boolean handleError(Exception e) {
         return handleError(e,currentTarget,null);
     }
 
     public boolean handleError(Exception e,Object source,String fieldName) {
         return handleEvent(
             new ValidationEventImpl(
                 ValidationEvent.ERROR,
                 e.getMessage(),
                 new ValidationEventLocatorExImpl(source,fieldName),
                     e));
     }
 
     public boolean handleEvent(ValidationEvent event) {
         try {
             return marshaller.getEventHandler().handleEvent(event);
         } catch (JAXBException e) {
             // impossible
             throw new Error(e);
         }
     }
 
     private void reportMissingObjectError(String fieldName) throws SAXException {
         reportError(new ValidationEventImpl(
             ValidationEvent.ERROR,
             Messages.MISSING_OBJECT.format(fieldName),
                 getCurrentLocation(fieldName),
             new NullPointerException() ));
     }
 
     /**
      * Called when a referenced object doesn't have an ID.
      */
     public void errorMissingId(Object obj) throws SAXException {
         reportError( new ValidationEventImpl(
             ValidationEvent.ERROR,
             Messages.MISSING_ID.format(obj),
             new ValidationEventLocatorImpl(obj)) );
     }
 
     public ValidationEventLocator getCurrentLocation(String fieldName) {
         return new ValidationEventLocatorExImpl(currentTarget,fieldName);
     }
 
     protected ValidationEventLocator getLocation() {
         return getCurrentLocation(null);
     }
 
     /**
      * When called from within the realm of the marshaller, this method
      * returns the current {@link XMLSerializer} in charge.
      */
     public static XMLSerializer getInstance() {
         return (XMLSerializer)Coordinator._getInstance();
     }
 }
