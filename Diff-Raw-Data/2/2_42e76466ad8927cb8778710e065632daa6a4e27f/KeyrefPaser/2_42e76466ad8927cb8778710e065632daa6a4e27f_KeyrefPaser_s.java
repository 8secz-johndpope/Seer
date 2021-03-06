 /*
  * This file is part of the DITA Open Toolkit project hosted on
  * Sourceforge.net. See the accompanying license.txt file for
  * applicable licenses.
  */
 
 /*
  * (c) Copyright IBM Corp. 2010 All Rights Reserved.
  */
 package org.dita.dost.writer;
 
 import static org.dita.dost.util.Constants.*;
 import static javax.xml.XMLConstants.*;
 
 import java.io.BufferedOutputStream;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.OutputStream;
 import java.io.StringReader;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Hashtable;
 import java.util.List;
 import java.util.Map;
 import java.util.Properties;
 import java.util.Set;
 import java.util.Stack;
 
 import javax.xml.parsers.DocumentBuilder;
 import javax.xml.parsers.DocumentBuilderFactory;
 import javax.xml.transform.Result;
 import javax.xml.transform.Source;
 import javax.xml.transform.Transformer;
 import javax.xml.transform.TransformerFactory;
 import javax.xml.transform.sax.SAXSource;
 import javax.xml.transform.stream.StreamResult;
 
 import org.dita.dost.exception.DITAOTException;
 import org.dita.dost.log.DITAOTLogger;
 import org.dita.dost.log.MessageUtils;
 import org.dita.dost.module.Content;
 import org.dita.dost.util.DitaClass;
 import org.dita.dost.util.FileUtils;
 import org.dita.dost.util.MergeUtils;
 import org.dita.dost.util.StringUtils;
 import org.dita.dost.util.XMLUtils;
 
 import org.w3c.dom.Attr;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.NamedNodeMap;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 import org.xml.sax.Attributes;
 import org.xml.sax.InputSource;
 import org.xml.sax.SAXException;
 import org.xml.sax.XMLReader;
 import org.xml.sax.helpers.XMLFilterImpl;
 import org.xml.sax.helpers.AttributesImpl;
 
 
 /**
  * Filter for processing key reference elements in DITA files.
  * Instances are reusable but not thread-safe.
  */
 public final class KeyrefPaser extends XMLFilterImpl {
 
     /**
      * Set of attributes which should not be copied from
      * key definition to key reference which is {@code <topicref>}.
      */
     private static final Set<String> no_copy;
     static {
         final Set<String> nc = new HashSet<String>();
         nc.add(ATTRIBUTE_NAME_ID);
         nc.add(ATTRIBUTE_NAME_CLASS);
         nc.add(ATTRIBUTE_NAME_XTRC);
         nc.add(ATTRIBUTE_NAME_XTRF);
         nc.add(ATTRIBUTE_NAME_HREF);
         nc.add(ATTRIBUTE_NAME_KEYS);
         nc.add(ATTRIBUTE_NAME_TOC);
         nc.add(ATTRIBUTE_NAME_PROCESSING_ROLE);
         no_copy = Collections.unmodifiableSet(nc);
     }
 
     /**
      * Set of attributes which should not be copied from
      * key definition to key reference which is not {@code <topicref>}.
      */
     private static final Set<String> no_copy_topic;
     static {
         final Set<String> nct = new HashSet<String>();
         nct.addAll(no_copy);
         nct.add("query");
         nct.add("search");
         nct.add(ATTRIBUTE_NAME_TOC);
         nct.add(ATTRIBUTE_NAME_PRINT);
         nct.add(ATTRIBUTE_NAME_COPY_TO);
         nct.add(ATTRIBUTE_NAME_CHUNK);
         nct.add(ATTRIBUTE_NAME_NAVTITLE);
         no_copy_topic = Collections.unmodifiableSet(nct);
     }
     
     /** List of key reference element definitions. */
     private final static List<KeyrefInfo> keyrefInfos;
     static {
         final List<KeyrefInfo> ki = new ArrayList<KeyrefInfo>();
         ki.add(new KeyrefInfo(TOPIC_AUTHOR, ATTRIBUTE_NAME_HREF, false));
         ki.add(new KeyrefInfo(TOPIC_DATA, ATTRIBUTE_NAME_HREF, false));
         ki.add(new KeyrefInfo(TOPIC_DATA_ABOUT, ATTRIBUTE_NAME_HREF, false));
         ki.add(new KeyrefInfo(TOPIC_IMAGE, ATTRIBUTE_NAME_HREF, true));
         ki.add(new KeyrefInfo(TOPIC_LINK, ATTRIBUTE_NAME_HREF, false));
         ki.add(new KeyrefInfo(TOPIC_LQ, ATTRIBUTE_NAME_HREF, false));
         ki.add(new KeyrefInfo(MAP_NAVREF, "mapref", true));
         ki.add(new KeyrefInfo(TOPIC_PUBLISHER, ATTRIBUTE_NAME_HREF, false));
         ki.add(new KeyrefInfo(TOPIC_SOURCE, ATTRIBUTE_NAME_HREF, false));
         ki.add(new KeyrefInfo(MAP_TOPICREF, ATTRIBUTE_NAME_HREF, false));
         ki.add(new KeyrefInfo(TOPIC_XREF, ATTRIBUTE_NAME_HREF, false));
         ki.add(new KeyrefInfo(TOPIC_CITE, null, false));
         ki.add(new KeyrefInfo(TOPIC_DT, null, false));
         ki.add(new KeyrefInfo(TOPIC_KEYWORD, null, false));
         ki.add(new KeyrefInfo(TOPIC_TERM, null, false));
         ki.add(new KeyrefInfo(TOPIC_PH, null, false));
         ki.add(new KeyrefInfo(TOPIC_INDEXTERM, null, false));
         ki.add(new KeyrefInfo(TOPIC_INDEX_BASE, null, false));
         ki.add(new KeyrefInfo(TOPIC_INDEXTERMREF, null, false));
         keyrefInfos = Collections.unmodifiableList(ki);
     }
     
     private final XMLReader parser;
     private final TransformerFactory tf;
     
     private DITAOTLogger logger;
     private Hashtable<String, String> definitionMap;
     private String tempDir;
 
     /**
      * It is stack used to store the place of current element
      * relative to the key reference element. Because keyref can be nested.
      */
     private final Stack<Integer> keyrefLevalStack;
 
     /**
      * It is used to store the place of current element
      * relative to the key reference element. If it is out of range of key
      * reference element it is zero, otherwise it is positive number.
      * It is also used to indicate whether current element is descendant of the
      * key reference element.
      */
     private int keyrefLeval;
 
     /** Relative path of the filename to the temporary directory. */
     private String filepath;
     private String extName;
 
     /**
      * It is used to store the target of the keys
      * In the from the map <keys, target>.
      */
     private Map<String, String> keyMap;
 
     /**
      * It is used to indicate whether the keyref is valid.
      * The descendant element should know whether keyref is valid because keyrefs can be nested.
      */
     private final Stack<Boolean> validKeyref;
 
     /**
      * Flag indicating whether the key reference element is empty,
      * if it is empty, it should pull matching content from the key definition.
      */
     private boolean empty;
 
     /** Stack of element names of the element containing keyref attribute. */
     private final Stack<String> elemName;
 
     /** Current element keyref info, {@code null} if not keyref type element. */
     private KeyrefInfo currentElement;
 
     private boolean hasChecked;
 
     /** Flag stack to indicate whether key reference element has sub-elements. */
     private final Stack<Boolean> hasSubElem;
 
     /** Current key definition. */
     private Document doc;
 
     /** File name with relative path to the temporary directory of input file. */
     private String fileName;
 
     /**
      * Constructor.
      */
     public KeyrefPaser() {
         keyrefLeval = 0;
         keyrefLevalStack = new Stack<Integer>();
         validKeyref = new Stack<Boolean>();
         empty = true;
         keyMap = new HashMap<String, String>();
         elemName = new Stack<String>();
         hasSubElem = new Stack<Boolean>();
         try {
             parser = StringUtils.getXMLReader();
             parser.setFeature(FEATURE_NAMESPACE_PREFIX, true);
             parser.setFeature(FEATURE_NAMESPACE, true);
             setParent(parser);
             tf = TransformerFactory.newInstance();
         } catch (final Exception e) {
             throw new RuntimeException("Failed to initialize XML parser: " + e.getMessage(), e);
         }
     }
     
     /**
      * Set logger
      * 
      * @param logger output logger
      */
     public void setLogger(final DITAOTLogger logger) {
         this.logger = logger;
     }
     
     /**
      * Get extension name.
      * @return extension name
      */
     public String getExtName() {
         return extName;
     }
     
     /**
      * Set extension name.
      * @param extName extension name
      */
     public void setExtName(final String extName) {
         this.extName = extName;
     }
     
     /**
      * Set key definition map.
      * 
      * @param content value {@code Hashtable<String, String>}
      */
     @SuppressWarnings("unchecked")
     public void setContent(final Content content) {
         definitionMap = (Hashtable<String, String>) content.getValue();
         if (definitionMap == null) {
             throw new IllegalArgumentException("Content value must be non-null Hashtable<String, String>");
         }
     }
     
     /**
      * Set temp dir.
      * @param tempDir temp dir
      */
     public void setTempDir(final String tempDir) {
         this.tempDir = tempDir;
     }
     
     /**
      * Set key map.
      * @param map key map
      */
     public void setKeyMap(final Map<String, String> map) {
         this.keyMap = map;
     }
     
     /**
      * Process key references.
      * 
      * @param filename file to process
      * @throws DITAOTException if key reference resolution failed
      */
     public void write(final String filename) throws DITAOTException {
         final File inputFile = new File(tempDir, filename);
         filepath = inputFile.getAbsolutePath();
         final File outputFile = new File(tempDir, filename + ATTRIBUTE_NAME_KEYREF);
         this.fileName = filename;
         OutputStream output = null;
         try {
             output = new BufferedOutputStream(new FileOutputStream(outputFile));
             final Transformer t = tf.newTransformer();
             final Source src = new SAXSource(this, new InputSource(inputFile.toURI().toString()));
             final Result dst = new StreamResult(output); 
             t.transform(src, dst);
         } catch (final Exception e) {
             throw new DITAOTException("Failed to process key references: " + e.getMessage(), e);
         } finally {
             if (output != null) {
                 try {
                     output.close();
                 } catch (final Exception ex) {
                     logger.logError("Failed to close output stream: " + ex.getMessage(), ex);
                 }
             }
         }
         if (!inputFile.delete()) {
             final Properties prop = new Properties();
             prop.put("%1", inputFile.getPath());
             prop.put("%2", outputFile.getPath());
             logger.logError(MessageUtils.getMessage("DOTJ009E", prop).toString());
         }
         if (!outputFile.renameTo(inputFile)) {
             final Properties prop = new Properties();
             prop.put("%1", inputFile.getPath());
             prop.put("%2", outputFile.getPath());
             logger.logError(MessageUtils.getMessage("DOTJ009E", prop).toString());
         }
     }
     
     // XML filter methods ------------------------------------------------------
 
     @Override
     public void characters(final char[] ch, final int start, final int length) throws SAXException {
         if (keyrefLeval != 0 && new String(ch,start,length).trim().length() == 0) {
             if (!hasChecked) {
                 empty = true;
             }
         } else {
             hasChecked = true;
             empty = false;
         }
         getContentHandler().characters(ch, start, length);
     }
 
     @Override
     public void endElement(final String uri, final String localName, final String name) throws SAXException {
         if (keyrefLeval != 0 && empty && !elemName.peek().equals(MAP_TOPICREF.localName)) {
             // If current element is in the scope of key reference element
             // and the element is empty
             if (!validKeyref.isEmpty() && validKeyref.peek()) {
                 // Key reference is valid,
                 // need to pull matching content from the key definition
                 final Element  elem = doc.getDocumentElement();
                 NodeList nodeList = null;
                 // If current element name doesn't equal the key reference element
                 // just grab the content from the matching element of key definition
                 if(!name.equals(elemName.peek())){
                     nodeList = elem.getElementsByTagName(name);
                     if(nodeList.getLength() > 0){
                         final Node node = nodeList.item(0);
                         final NodeList nList = node.getChildNodes();
                         int index = 0;
                         while(index < nList.getLength()){
                             final Node n = nList.item(index++);
                             if(n.getNodeType() == Node.TEXT_NODE){
                                 final char[] ch = n.getNodeValue().toCharArray();
                                 getContentHandler().characters(ch, 0, ch.length);
                                 break;
                             }
                         }
                     }
                 }else{
                     // Current element name equals the key reference element
                     // grab keyword or term from key definition
                     nodeList = elem.getElementsByTagName(TOPIC_KEYWORD.localName);
                     if(nodeList.getLength() == 0 ){
                         nodeList = elem.getElementsByTagName(TOPIC_TERM.localName);
                     }
                     if(!hasSubElem.peek()){
                         if(nodeList.getLength() > 0){
                             if(currentElement != null && !currentElement.isRefType){
                                 // only one keyword or term is used.
                                 nodeToString((Element)nodeList.item(0), false);
                             } else if(currentElement != null){
                                 // If the key reference element carries href attribute
                                 // all keyword or term are used.
                                 if(TOPIC_LINK.matches(currentElement.type)){
                                     final AttributesImpl atts = new AttributesImpl();
                                     XMLUtils.addOrSetAttribute(atts, ATTRIBUTE_NAME_CLASS, TOPIC_LINKTEXT.toString());
                                     getContentHandler().startElement(NULL_NS_URI, TOPIC_LINKTEXT.localName, TOPIC_LINKTEXT.localName, atts);
                                 }
                                 if (!currentElement.isEmpty) {
                                     for(int index =0; index<nodeList.getLength(); index++){
                                         final Node node = nodeList.item(index);
                                         if(node.getNodeType() == Node.ELEMENT_NODE){
                                             nodeToString((Element)node, true);
                                         }
                                     }
                                 }
                                 if(TOPIC_LINK.matches(currentElement.type)){
                                     getContentHandler().endElement(NULL_NS_URI, TOPIC_LINKTEXT.localName, TOPIC_LINKTEXT.localName);
                                 }
                             }
                         }else{
                             if(currentElement != null && TOPIC_LINK.matches(currentElement.type)){
                                 // If the key reference element is link or its specification,
                                 // should pull in the linktext
                                 final NodeList linktext = elem.getElementsByTagName(TOPIC_LINKTEXT.localName);
                                 if(linktext.getLength()>0){
                                     nodeToString((Element)linktext.item(0), true);
                                 }else if (!StringUtils.isEmptyString(elem.getAttribute(ATTRIBUTE_NAME_NAVTITLE))){
                                     final AttributesImpl atts = new AttributesImpl();
                                     XMLUtils.addOrSetAttribute(atts, ATTRIBUTE_NAME_CLASS, TOPIC_LINKTEXT.toString());
                                     getContentHandler().startElement(NULL_NS_URI, TOPIC_LINKTEXT.localName, TOPIC_LINKTEXT.localName, atts);
                                     if (elem.getAttribute(ATTRIBUTE_NAME_NAVTITLE) != null) {
                                         final char[] ch = elem.getAttribute(ATTRIBUTE_NAME_NAVTITLE).toCharArray();
                                         getContentHandler().characters(ch, 0, ch.length);
                                     }
                                     getContentHandler().endElement(NULL_NS_URI, TOPIC_LINKTEXT.localName, TOPIC_LINKTEXT.localName);
                                 }
                             }else if(currentElement != null && currentElement.isRefType){
                                 final NodeList linktext = elem.getElementsByTagName(TOPIC_LINKTEXT.localName);
                                 if(linktext.getLength()>0){
                                     nodeToString((Element)linktext.item(0), false);
                                 }else{
                                     if (elem.getAttribute(ATTRIBUTE_NAME_NAVTITLE) != null) {
                                         final char[] ch = elem.getAttribute(ATTRIBUTE_NAME_NAVTITLE).toCharArray();
                                         getContentHandler().characters(ch, 0, ch.length);
                                     }
                                 }
                             }
                         }
 
                     }
                 }
             }
         }
         if (keyrefLeval != 0){
             keyrefLeval--;
             empty = false;
         }
 
         if (keyrefLeval == 0 && !keyrefLevalStack.empty()) {
             // To the end of key reference, pop the stacks.
             keyrefLeval = keyrefLevalStack.pop();
             validKeyref.pop();
             elemName.pop();
             hasSubElem.pop();
         }
         getContentHandler().endElement(uri, localName, name);
     }
     
     @Override
     public void startElement(final String uri, final String localName, final String name,
             final Attributes atts) throws SAXException {
         currentElement = null;
         final String cls = atts.getValue(ATTRIBUTE_NAME_CLASS);
         for (final KeyrefInfo k: keyrefInfos) {
             if (k.type.matches(cls)) {
                 currentElement = k;
             }
         }
         final AttributesImpl resAtts = new AttributesImpl(atts);
         hasChecked = false;
         empty = true;
         boolean valid = false;
         if (atts.getIndex(ATTRIBUTE_NAME_KEYREF) == -1) {
             // If the keyrefLeval doesn't equal 0, it means that current element is under the key reference element
             if(keyrefLeval != 0){
                 keyrefLeval ++;
                 hasSubElem.pop();
                 hasSubElem.push(true);
             }
         } else {
             // If there is @keyref, use the key definition to do
             // combination.
             // HashSet to store the attributes copied from key
             // definition to key reference.
 
             elemName.push(name);
             //hasKeyref = true;
             if (keyrefLeval != 0) {
                 keyrefLevalStack.push(keyrefLeval);
                 hasSubElem.pop();
                 hasSubElem.push(true);
             }
             hasSubElem.push(false);
             keyrefLeval = 0;
             keyrefLeval++;
             //keyref.push(atts.getValue(ATTRIBUTE_NAME_KEYREF));
             //the @keyref could be in the following forms:
             // 1.keyName 2.keyName/elementId
             /*String definition = ((Hashtable<String, String>) content
 					.getValue()).get(atts
 					.getValue(ATTRIBUTE_NAME_KEYREF));*/
             final String keyrefValue=atts.getValue(ATTRIBUTE_NAME_KEYREF);
             final int slashIndex=keyrefValue.indexOf(SLASH);
             String keyName= keyrefValue;
             String tail= "";
             if (slashIndex != -1) {
                 keyName = keyrefValue.substring(0, slashIndex);
                 tail = keyrefValue.substring(slashIndex);
             }
             final String definition = definitionMap.get(keyName);
 
             // If definition is not null
             if(definition!=null){
                 doc = keyDefToDoc(definition);
                 final Element elem = doc.getDocumentElement();
                 final NamedNodeMap namedNodeMap = elem.getAttributes();
                 // first resolve the keyref attribute
                 if (currentElement != null && currentElement.isRefType) {
                     String target = keyMap.get(keyName);
                     if (target != null && target.length() != 0) {
                         String target_output = target;
                         // if the scope equals local, the target should be verified that
                         // it exists.
                         final String scopeValue=elem.getAttribute(ATTRIBUTE_NAME_SCOPE);
                         if (TOPIC_IMAGE.matches(currentElement.type)) {
                             valid = true;
                             XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_SCOPE);
                             XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_HREF);
                             XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_TYPE);
                             XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_FORMAT);
                             target_output = FileUtils.getRelativePathFromMap(fileName, target_output);
                             target_output = normalizeHrefValue(target_output, tail);
                             XMLUtils.addOrSetAttribute(resAtts, currentElement.refAttr, target_output);
                         } else if ("".equals(scopeValue) || ATTR_SCOPE_VALUE_LOCAL.equals(scopeValue)){
                         	if (!(MAPGROUP_D_MAPREF.matches(cls)
 									&& FileUtils.isDITAMapFile(target.toLowerCase()))){
                         		target = FileUtils.replaceExtName(target,extName);
 							}
                         	
                             final File topicFile = new File(FileUtils.resolveFile(tempDir, target));
                             if (topicFile.exists()) {  
                                 final String topicId = this.getFirstTopicId(topicFile);
                                 target_output = FileUtils.getRelativePathFromMap(filepath, new File(tempDir, target).getAbsolutePath());
                                 valid = true;
                                 XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_HREF);
                                 XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_SCOPE);
                                 XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_TYPE);
                                 XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_FORMAT);
                                 target_output = normalizeHrefValue(target_output, tail, topicId);
                                 XMLUtils.addOrSetAttribute(resAtts, currentElement.refAttr, target_output);
                             } else {
                                 // referenced file does not exist, emits a message.
                                 // Should only emit this if in a debug mode; comment out for now
                                 /*Properties prop = new Properties();
 								prop.put("%1", atts.getValue(ATTRIBUTE_NAME_KEYREF));
 								javaLogger
 										.logInfo(MessageUtils.getMessage("DOTJ047I", prop)
 												.toString());*/
                             }
                         }
                         // scope equals peer or external
                         else {
                             valid = true;
                             XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_SCOPE);
                             XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_HREF);
                             XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_TYPE);
                             XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_FORMAT);
                             target_output = normalizeHrefValue(target_output, tail);
                             XMLUtils.addOrSetAttribute(resAtts, ATTRIBUTE_NAME_HREF, target_output);
                         }
 
                    } else if(target.length() == 0){
                         // Key definition does not carry an href or href equals "".
                         valid = true;
                         XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_SCOPE);
                         XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_HREF);
                         XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_TYPE);
                         XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_FORMAT);
                     }else{
                         // key does not exist.
                         final Properties prop = new Properties();
                         prop.put("%1", atts.getValue(ATTRIBUTE_NAME_KEYREF));
                         logger.logInfo(MessageUtils.getMessage("DOTJ047I", prop).toString());
                     }
 
                 } else if (currentElement != null && !currentElement.isRefType) {
                     final String target = keyMap.get(keyName);
 
                     if (target != null) {
                         valid = true;
                         XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_SCOPE);
                         XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_HREF);
                         XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_TYPE);
                         XMLUtils.removeAttribute(resAtts, ATTRIBUTE_NAME_FORMAT);
                     } else {
                         // key does not exist
                         // Do not log error, key should not need a link
                         //final Properties prop = new Properties();
                         //prop.put("%1", atts.getValue(ATTRIBUTE_NAME_KEYREF));
                         //logger.logInfo(MessageUtils.getMessage("DOTJ047I", prop).toString());
                     }
 
                 }
 
 
                 // copy attributes in key definition to key reference
                 // Set no_copy and no_copy_topic define some attributes should not be copied.
                 if (valid) {
                     if (currentElement != null && MAP_TOPICREF.matches(currentElement.type)) {
                         // @keyref in topicref
                         for (int index = 0; index < namedNodeMap.getLength(); index++) {
                             final Node node = namedNodeMap.item(index);
                             if (node.getNodeType() == Node.ATTRIBUTE_NODE
                                     && !no_copy.contains(node.getNodeName())) {
                                 XMLUtils.removeAttribute(resAtts, node.getNodeName());
                                 XMLUtils.addOrSetAttribute(resAtts, node);
                             }
                         }
                     } else {
                         // @keyref not in topicref
                         // different elements have different attributes
                         if (currentElement != null && currentElement.isRefType) {
                             // current element with href attribute
                             for (int index = 0; index < namedNodeMap.getLength(); index++) {
                                 final Node node = namedNodeMap.item(index);
                                 if (node.getNodeType() == Node.ATTRIBUTE_NODE
                                         && !no_copy_topic.contains(node.getNodeName())) {
                                     XMLUtils.removeAttribute(resAtts, node.getNodeName());
                                     XMLUtils.addOrSetAttribute(resAtts, node);
                                 }
                             }
                         } else if (currentElement != null && !currentElement.isRefType) {
                             // current element without href attribute
                             // so attributes about href should not be copied.
                             for (int index = 0; index < namedNodeMap.getLength(); index++) {
                                 final Node node = namedNodeMap.item(index);
                                 if (node.getNodeType() == Node.ATTRIBUTE_NODE
                                         && !no_copy_topic.contains(node.getNodeName())
                                         && !(node.getNodeName().equals(ATTRIBUTE_NAME_SCOPE)
                                                 || node.getNodeName().equals(ATTRIBUTE_NAME_FORMAT)
                                                 || node.getNodeName().equals(ATTRIBUTE_NAME_TYPE))) {
                                     XMLUtils.removeAttribute(resAtts, node.getNodeName());
                                     XMLUtils.addOrSetAttribute(resAtts, node);
                                 }
                             }
                         }
 
                     }
                 } else {
                     // keyref is not valid, don't copy any attribute.
                 }
             }else{
                 // key does not exist
                 final Properties prop = new Properties();
                 prop.put("%1", atts.getValue(ATTRIBUTE_NAME_KEYREF));
                 logger.logInfo(MessageUtils.getMessage("DOTJ047I", prop).toString());;
             }
 
             validKeyref.push(valid);
 
 
         }
 
         getContentHandler().startElement(uri, localName, name, resAtts);
     }
 
     // Private methods ---------------------------------------------------------
     
     /**
      * Read key definition
      * 
      * @param key key definition XML string
      * @return parsed key definition document
      */
     private Document keyDefToDoc(final String key) {
         final InputSource inputSource = new InputSource(new StringReader(key));
         final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         Document document = null;
         try {
             final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
             document = documentBuilder.parse(inputSource);
         } catch (final Exception e) {
             logger.logError("Failed to parse key definition: " + e.getMessage(), e);
         }
         return document;
     }
 
     /**
      * Serialize DOM node into a string.
      * 
      * @param elem element to serialize
      * @param flag {@code true} to serialize elements, {@code false} to only serialize text nodes.
      * @return
      */
     private void nodeToString(final Element elem, final boolean flag) throws SAXException{
         // use flag to indicate that whether there is need to copy the element name
         if(flag){
             final AttributesImpl atts = new AttributesImpl();
             final NamedNodeMap namedNodeMap = elem.getAttributes();
             for(int i=0; i<namedNodeMap.getLength(); i++){
                 final Attr a = (Attr) namedNodeMap.item(i);
                 if(a.getNodeName().equals(ATTRIBUTE_NAME_CLASS)) {
                     XMLUtils.addOrSetAttribute(atts, ATTRIBUTE_NAME_CLASS, changeclassValue(a.getNodeValue()));
                 } else {
                     XMLUtils.addOrSetAttribute(atts, a);
                 }
             }
             getContentHandler().startElement(NULL_NS_URI, elem.getNodeName(), elem.getNodeName(), atts);
         }
         final NodeList nodeList = elem.getChildNodes();
         for(int i=0; i<nodeList.getLength(); i++){
             final Node node = nodeList.item(i);
             if(node.getNodeType() == Node.ELEMENT_NODE){
                 final Element e = (Element) node;
                 //special process for tm tag.
                 if(TOPIC_TM.matches(e)){
                     nodeToString(e, true);
                 }else{
                     // If the type of current node is ELEMENT_NODE, process current node.
                     nodeToString(e, flag);
                 }
                 // If the type of current node is ELEMENT_NODE, process current node.
                 //stringBuffer.append(nodeToString((Element)node, flag));
             } else if(node.getNodeType() == Node.TEXT_NODE){
                 final char[] ch = node.getNodeValue().toCharArray();
                 getContentHandler().characters(ch, 0, ch.length);
             }
         }
         if(flag) {
             getContentHandler().endElement(NULL_NS_URI, elem.getNodeName(), elem.getNodeName());
         }
     }
 
     /**
      * Change map type to topic type. 
      */
     private String changeclassValue(final String classValue){
         return classValue.replaceAll("map/", "topic/");
     }
     
     /**
      * change elementId into topicId if there is no topicId in key definition.
      */
     private static String normalizeHrefValue(final String keyName, final String tail) {
         final int sharpIndex=keyName.indexOf(SHARP);
         if(sharpIndex == -1){
             return keyName + tail.replaceAll(SLASH, SHARP);
         }
         return keyName + tail;
     }
 
     /**
      * Get first topic id
      */
     private String getFirstTopicId(final File topicFile) {
         final String path = topicFile.getParent();
         final String name = topicFile.getName();
         final String topicId = MergeUtils.getFirstTopicId(name, path, false);
         return topicId;
     }
     
     /**
      * Insert topic id into href
      */
     private static String normalizeHrefValue(final String fileName, final String tail, final String topicId) {
         final int sharpIndex=fileName.indexOf(SHARP);
         //Insert first topic id only when topicid is not set in keydef
         //and keyref has elementid
         if(sharpIndex == -1 && !"".equals(tail)){
             return fileName + SHARP + topicId + tail;
         }
         return fileName + tail;
     }
 
     // Inner classes -----------------------------------------------------------
     
     private static final class KeyrefInfo {
         /** DITA class. */
         final DitaClass type;
         /** Reference attribute name. */
         final String refAttr;
         /** Element is reference type. */
         final boolean isRefType;
         /** Element is empty. */
         final boolean isEmpty;
         /**
          * Construct a new key reference info object.
          * 
          * @param type element type
          * @param refAttr hyperlink attribute name
          * @param isEmpty flag if element is empty
          */
         KeyrefInfo(final DitaClass type, final String refAttr, final boolean isEmpty) {
             this.type = type;
             this.refAttr = refAttr;
             this.isEmpty = isEmpty;
             this.isRefType = refAttr != null;
         }
     }
     
 }
