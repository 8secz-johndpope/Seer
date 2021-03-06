 /*
  * Fast Infoset ver. 0.1 software ("Software")
  *
  * Copyright, 2004-2005 Sun Microsystems, Inc. All Rights Reserved.
  *
  * Software is licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License. You may
  * obtain a copy of the License at:
  *
  *        http://www.apache.org/licenses/LICENSE-2.0
  *
  *    Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations.
  *
  *    Sun supports and benefits from the global community of open source
  * developers, and thanks the community for its important contributions and
  * open standards-based technology, which Sun has adopted into many of its
  * products.
  *
  *    Please note that portions of Software may be provided with notices and
  * open source licenses from such communities and third parties that govern the
  * use of those portions, and any licenses granted hereunder do not alter any
  * rights and obligations you may have under such open source licenses,
  * however, the disclaimer of warranty and limitation of liability provisions
  * in this License will apply to all Software in this distribution.
  *
  *    You acknowledge that the Software is not designed, licensed or intended
  * for use in the design, construction, operation or maintenance of any nuclear
  * facility.
  *
  * Apache License
  * Version 2.0, January 2004
  * http://www.apache.org/licenses/
  *
  */
 
 
 package com.sun.xml.fastinfoset.dom;
 
 import com.sun.xml.fastinfoset.Decoder;
 import com.sun.xml.fastinfoset.DecoderStateTables;
 import com.sun.xml.fastinfoset.EncodingConstants;
 import com.sun.xml.fastinfoset.QualifiedName;
 import com.sun.xml.fastinfoset.algorithm.BuiltInEncodingAlgorithmFactory;
 import com.sun.xml.fastinfoset.util.CharArray;
 import com.sun.xml.fastinfoset.util.CharArrayString;
 import java.io.IOException;
 import java.io.InputStream;
 import org.jvnet.fastinfoset.EncodingAlgorithm;
 import org.jvnet.fastinfoset.EncodingAlgorithmException;
 import org.jvnet.fastinfoset.EncodingAlgorithmIndexes;
 import org.jvnet.fastinfoset.FastInfosetException;
 import org.w3c.dom.Attr;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.Node;
 import com.sun.xml.fastinfoset.CommonResourceBundle;
 
 
 public class DOMDocumentParser extends Decoder {
     protected Document _document;
     
     protected Node _currentNode;
     
     protected Element _currentElement;
 
     protected Attr[] _namespaceAttributes = new Attr[16];
 
     protected int _namespaceAttributesIndex;
     
     protected int[] _namespacePrefixes = new int[16];
     
     protected int _namespacePrefixesIndex;
 
     public void parse(Document d, InputStream s) throws FastInfosetException, IOException {        
         _currentNode = _document = d;
         _namespaceAttributesIndex = 0;
                     
         parse(s);        
     }
     
     protected final void parse(InputStream s) throws FastInfosetException, IOException {
         setInputStream(s);
         parse();
     }
     
     protected void resetOnError() {
         _namespacePrefixesIndex = 0;
         
         if (_v == null) {
             _prefixTable.clearCompletely();
         }
        _duplicateAttributeVerifier.clear();
     }
     
     protected final void parse() throws FastInfosetException, IOException {
         try {
             reset();
             decodeHeader();                                                                                
             processDII();
         } catch (RuntimeException e) {
             resetOnError();
             // Wrap runtime exception
             throw new FastInfosetException(e);
         } catch (FastInfosetException e) {
             resetOnError();
             throw e;
         } catch (IOException e) {
             resetOnError();
             throw e;
         }
     }
     
     protected final void processDII() throws FastInfosetException, IOException {        
         _b = read();
         if (_b > 0) {
             processDIIOptionalProperties();
         }
         
         // Decode one Document Type II, Comment IIs, PI IIs and one EII
         boolean firstElementHasOccured = false;
         boolean documentTypeDeclarationOccured = false;
         while(!_terminate || !firstElementHasOccured) {
             _b = read();
             switch(DecoderStateTables.DII[_b]) {
                 case DecoderStateTables.EII_NO_AIIS_INDEX_SMALL:
                     processEII(_elementNameTable._array[_b], false);
                     firstElementHasOccured = true;
                     break;
                 case DecoderStateTables.EII_AIIS_INDEX_SMALL:
                     processEII(_elementNameTable._array[_b & EncodingConstants.INTEGER_3RD_BIT_SMALL_MASK], true);
                     firstElementHasOccured = true;
                     break;
                 case DecoderStateTables.EII_INDEX_MEDIUM:
                     processEII(decodeEIIIndexMedium(), (_b & EncodingConstants.ELEMENT_ATTRIBUTE_FLAG) > 0);
                     firstElementHasOccured = true;
                     break;
                 case DecoderStateTables.EII_INDEX_LARGE:
                     processEII(decodeEIIIndexLarge(), (_b & EncodingConstants.ELEMENT_ATTRIBUTE_FLAG) > 0);
                     firstElementHasOccured = true;
                     break;
                 case DecoderStateTables.EII_LITERAL:
                 {
                     final QualifiedName qn = processLiteralQualifiedName(
                                 _b & EncodingConstants.LITERAL_QNAME_PREFIX_NAMESPACE_NAME_MASK);
                     _elementNameTable.add(qn);
                     processEII(qn, (_b & EncodingConstants.ELEMENT_ATTRIBUTE_FLAG) > 0);
                     firstElementHasOccured = true;
                     break;
                 }
                 case DecoderStateTables.EII_NAMESPACES:
                     processEIIWithNamespaces();
                     firstElementHasOccured = true;
                     break;
                 case DecoderStateTables.DOCUMENT_TYPE_DECLARATION_II:
                 {
                     if (documentTypeDeclarationOccured) {
                         throw new FastInfosetException(CommonResourceBundle.getInstance().getString("message.secondOccurenceOfDTDII"));
                     }
                     documentTypeDeclarationOccured = true;
 
                     String system_identifier = ((_b & EncodingConstants.DOCUMENT_TYPE_SYSTEM_IDENTIFIER_FLAG) > 0) 
                         ? decodeIdentifyingNonEmptyStringOnFirstBit(_v.otherURI) : null;
                     String public_identifier = ((_b & EncodingConstants.DOCUMENT_TYPE_PUBLIC_IDENTIFIER_FLAG) > 0) 
                         ? decodeIdentifyingNonEmptyStringOnFirstBit(_v.otherURI) : null;
                     
                     _b = read();
                     while (_b == EncodingConstants.PROCESSING_INSTRUCTION) {
                         switch(decodeNonIdentifyingStringOnFirstBit()) {
                             case NISTRING_STRING:
                                 final String data = new String(_charBuffer, 0, _charBufferLength);
                                 if (_addToTable) {
                                     _v.otherString.add(new CharArray(_charBuffer, 0, _charBufferLength, true));
                                 }
                                 break;
                             case NISTRING_ENCODING_ALGORITHM:
                                 throw new FastInfosetException(CommonResourceBundle.getInstance().getString("message.processingIIWithEncodingAlgorithm"));                        
                             case NISTRING_INDEX:
                                 break;
                             case NISTRING_EMPTY_STRING:
                                 break;
                         }
                         _b = read();
                     }
                     if ((_b & EncodingConstants.TERMINATOR) != EncodingConstants.TERMINATOR) {
                         throw new FastInfosetException(CommonResourceBundle.getInstance().getString("message.processingInstructionIIsNotTerminatedCorrectly"));
                     }
                     if (_b == EncodingConstants.DOUBLE_TERMINATOR) {
                         _terminate = true;
                     }
                     
                     _notations.clear();
                     _unparsedEntities.clear();
                     /*
                      * TODO
                      * Report All events associated with DTD, PIs, notations etc
                      */
                     break;
                 }
                 case DecoderStateTables.COMMENT_II:
                     processCommentII();
                     break;
                 case DecoderStateTables.PROCESSING_INSTRUCTION_II:
                     processProcessingII();
                     break;
                 case DecoderStateTables.TERMINATOR_DOUBLE:                    
                     _doubleTerminate = true; 
                 case DecoderStateTables.TERMINATOR_SINGLE:
                     _terminate = true;
                     break;
                 default:
                     throw new FastInfosetException(CommonResourceBundle.getInstance().getString("message.IllegalStateDecodingDII"));
             }
         }
 
         // Decode any remaining Comment IIs, PI IIs
         while(!_terminate) {
             _b = read();
             switch(DecoderStateTables.DII[_b]) {
                 case DecoderStateTables.COMMENT_II:
                     processCommentII();
                     break;
                 case DecoderStateTables.PROCESSING_INSTRUCTION_II:
                     processProcessingII();
                     break;
                 case DecoderStateTables.TERMINATOR_DOUBLE:                    
                     _doubleTerminate = true; 
                 case DecoderStateTables.TERMINATOR_SINGLE:
                     _terminate = true;
                     break;
                 default:
                     throw new FastInfosetException(CommonResourceBundle.getInstance().getString("message.IllegalStateDecodingDII"));
             }
         }
 
     }
 
     protected final void processDIIOptionalProperties() throws FastInfosetException, IOException {     
         // Optimize for the most common case
         if (_b == EncodingConstants.DOCUMENT_INITIAL_VOCABULARY_FLAG) {
             decodeInitialVocabulary();
             return;
         }
         
         if ((_b & EncodingConstants.DOCUMENT_ADDITIONAL_DATA_FLAG) > 0) {
             decodeAdditionalData();
             /*
              * TODO 
              * how to report the additional data?
              */
         }
         
         if ((_b & EncodingConstants.DOCUMENT_INITIAL_VOCABULARY_FLAG) > 0) {
             decodeInitialVocabulary();
         }
 
         if ((_b & EncodingConstants.DOCUMENT_NOTATIONS_FLAG) > 0) {
             decodeNotations();
             // TODO Report notations
         }
 
         if ((_b & EncodingConstants.DOCUMENT_UNPARSED_ENTITIES_FLAG) > 0) {
             decodeUnparsedEntities();
             // TODO Report unparsed entities
         }
 
         if ((_b & EncodingConstants.DOCUMENT_CHARACTER_ENCODING_SCHEME) > 0) {
             String version = decodeCharacterEncodingScheme();
             /*
              * TODO 
              * how to report the character encoding scheme?
              */
         }
         
         if ((_b & EncodingConstants.DOCUMENT_STANDALONE_FLAG) > 0) {
             boolean standalone = (read() > 0) ? true : false ;
             /*
              * TODO 
              * how to report the standalone flag?
              */
         }
 
         if ((_b & EncodingConstants.DOCUMENT_VERSION_FLAG) > 0) {
             String version = decodeVersion();
             /*
              * TODO 
              * how to report the document version?
              */
         }
     }
 
     protected final void processEII(QualifiedName name, boolean hasAttributes) throws FastInfosetException, IOException {
         if (_prefixTable._currentInScope[name.prefixIndex] != name.namespaceNameIndex) {
             throw new FastInfosetException(CommonResourceBundle.getInstance().getString("message.qnameOfEIINotInScope"));
         }
         
         final Node parentCurrentNode = _currentNode;
         
         _currentNode = _currentElement = createElement(name.namespaceName, name.qName, name.localName);
         
         if (_namespaceAttributesIndex > 0) {
             for (int i = 0; i < _namespaceAttributesIndex; i++) {
                 _currentElement.setAttributeNode(_namespaceAttributes[i]);
                 _namespaceAttributes[i] = null;
             }
             _namespaceAttributesIndex = 0;
         }
         
         if (hasAttributes) {
             processAIIs();
         }
         
         parentCurrentNode.appendChild(_currentElement);
 
         while(!_terminate) {
             _b = read();
             switch(DecoderStateTables.EII[_b]) {
                 case DecoderStateTables.EII_NO_AIIS_INDEX_SMALL:
                     processEII(_elementNameTable._array[_b], false);
                     break;
                 case DecoderStateTables.EII_AIIS_INDEX_SMALL:
                     processEII(_elementNameTable._array[_b & EncodingConstants.INTEGER_3RD_BIT_SMALL_MASK], true);
                     break;
                 case DecoderStateTables.EII_INDEX_MEDIUM:
                     processEII(decodeEIIIndexMedium(), (_b & EncodingConstants.ELEMENT_ATTRIBUTE_FLAG) > 0);
                     break;
                 case DecoderStateTables.EII_INDEX_LARGE:
                     processEII(decodeEIIIndexLarge(), (_b & EncodingConstants.ELEMENT_ATTRIBUTE_FLAG) > 0);
                     break;
                 case DecoderStateTables.EII_LITERAL:
                 {
                     final QualifiedName qn = processLiteralQualifiedName(
                                 _b & EncodingConstants.LITERAL_QNAME_PREFIX_NAMESPACE_NAME_MASK);
                     _elementNameTable.add(qn);
                     processEII(qn, (_b & EncodingConstants.ELEMENT_ATTRIBUTE_FLAG) > 0);
                     break;
                 }
                 case DecoderStateTables.EII_NAMESPACES:
                     processEIIWithNamespaces();
                     break;
                 case DecoderStateTables.CII_UTF8_SMALL_LENGTH:
                 {
                     _octetBufferLength = (_b & EncodingConstants.OCTET_STRING_LENGTH_7TH_BIT_SMALL_MASK) 
                         + 1;
                     String v = decodeUtf8StringAsString();
                     if ((_b & EncodingConstants.CHARACTER_CHUNK_ADD_TO_TABLE_FLAG) > 0) {
                         _characterContentChunkTable.add(_charBuffer, _charBufferLength);
                     }
                     
                     _currentNode.appendChild(_document.createTextNode(v));
                     break;
                 }
                 case DecoderStateTables.CII_UTF8_MEDIUM_LENGTH:
                 {
                     _octetBufferLength = read() + EncodingConstants.OCTET_STRING_LENGTH_7TH_BIT_SMALL_LIMIT;
                     String v = decodeUtf8StringAsString();
                     if ((_b & EncodingConstants.CHARACTER_CHUNK_ADD_TO_TABLE_FLAG) > 0) {
                         _characterContentChunkTable.add(_charBuffer, _charBufferLength);
                     }
                     
                     _currentNode.appendChild(_document.createTextNode(v));
                     break;
                 }
                 case DecoderStateTables.CII_UTF8_LARGE_LENGTH:
                 {
                     _octetBufferLength = (read() << 24) |
                         (read() << 16) |
                         (read() << 8) |
                         read();
                     _octetBufferLength += EncodingConstants.OCTET_STRING_LENGTH_7TH_BIT_MEDIUM_LIMIT;
                     String v = decodeUtf8StringAsString();
                     if ((_b & EncodingConstants.CHARACTER_CHUNK_ADD_TO_TABLE_FLAG) > 0) {
                         _characterContentChunkTable.add(_charBuffer, _charBufferLength);
                     }
                     
                     _currentNode.appendChild(_document.createTextNode(v));
                     break;
                 }
                 case DecoderStateTables.CII_UTF16_SMALL_LENGTH:
                 {
                     _octetBufferLength = (_b & EncodingConstants.OCTET_STRING_LENGTH_7TH_BIT_SMALL_MASK) 
                         + 1;
                     String v = decodeUtf16StringAsString();
                     if ((_b & EncodingConstants.CHARACTER_CHUNK_ADD_TO_TABLE_FLAG) > 0) {
                         _characterContentChunkTable.add(_charBuffer, _charBufferLength);
                     }
                     
                     _currentNode.appendChild(_document.createTextNode(v));
                     break;
                 }
                 case DecoderStateTables.CII_UTF16_MEDIUM_LENGTH:
                 {
                     _octetBufferLength = read() + EncodingConstants.OCTET_STRING_LENGTH_7TH_BIT_SMALL_LIMIT;
                     String v = decodeUtf16StringAsString();
                     if ((_b & EncodingConstants.CHARACTER_CHUNK_ADD_TO_TABLE_FLAG) > 0) {
                         _characterContentChunkTable.add(_charBuffer, _charBufferLength);
                     }
                     
                     _currentNode.appendChild(_document.createTextNode(v));
                     break;
                 }
                 case DecoderStateTables.CII_UTF16_LARGE_LENGTH:
                 {
                     _octetBufferLength = (read() << 24) |
                         (read() << 16) |
                         (read() << 8) |
                         read();
                     _octetBufferLength += EncodingConstants.OCTET_STRING_LENGTH_7TH_BIT_MEDIUM_LIMIT;
                     String v = decodeUtf16StringAsString();
                     if ((_b & EncodingConstants.CHARACTER_CHUNK_ADD_TO_TABLE_FLAG) > 0) {
                         _characterContentChunkTable.add(_charBuffer, _charBufferLength);
                     }
                     
                     _currentNode.appendChild(_document.createTextNode(v));
                     break;
                 }
                 case DecoderStateTables.CII_RA:
                 {
                     final boolean addToTable = (_b & EncodingConstants.CHARACTER_CHUNK_ADD_TO_TABLE_FLAG) > 0;
                     
                     // Decode resitricted alphabet integer
                     _identifier = (_b & 0x02) << 6;
                     _b = read();
                     _identifier |= (_b & 0xFC) >> 2;
 
                     decodeOctetsOnSeventhBitOfNonIdentifyingStringOnThirdBit(_b);
                     
                     String v = decodeRestrictedAlphabetAsString();                    
                     if (addToTable) {
                         _characterContentChunkTable.add(_charBuffer, _charBufferLength);
                     }
                     
                     _currentNode.appendChild(_document.createTextNode(v));
                     break;
                 }
                 case DecoderStateTables.CII_EA:
                 {
                     if ((_b & EncodingConstants.NISTRING_ADD_TO_TABLE_FLAG) > 0) {
                         throw new EncodingAlgorithmException(CommonResourceBundle.getInstance().getString("message.addToTableNotSupported"));
                     }
 
                     // Decode encoding algorithm integer
                     _identifier = (_b & 0x02) << 6;
                     _b = read();
                     _identifier |= (_b & 0xFC) >> 2;
                     
                     decodeOctetsOnSeventhBitOfNonIdentifyingStringOnThirdBit(_b);
                     final String s = convertEncodingAlgorithmDataToCharacters(false);
                     _currentNode.appendChild(_document.createTextNode(s));
                     break;
                 }
                 case DecoderStateTables.CII_INDEX_SMALL:
                 {
                     final CharArray ca = _characterContentChunkTable.get(_b & EncodingConstants.INTEGER_4TH_BIT_SMALL_MASK);
                     
                     _currentNode.appendChild(_document.createTextNode(ca.toString()));
                     break;
                 }
                 case DecoderStateTables.CII_INDEX_MEDIUM:
                 {
                     final int index = (((_b & EncodingConstants.INTEGER_4TH_BIT_MEDIUM_MASK) << 8) | read())
                         + EncodingConstants.INTEGER_4TH_BIT_SMALL_LIMIT;
                     final String s = _characterContentChunkTable.get(index).toString();
                     
                     _currentNode.appendChild(_document.createTextNode(s));
                     break;
                 }
                 case DecoderStateTables.CII_INDEX_LARGE:
                 {
                     int index = ((_b & EncodingConstants.INTEGER_4TH_BIT_LARGE_MASK) << 16) |
                         (read() << 8) |
                         read();
                     index += EncodingConstants.INTEGER_4TH_BIT_MEDIUM_LIMIT;
                     final String s = _characterContentChunkTable.get(index).toString();
                     
                     _currentNode.appendChild(_document.createTextNode(s));
                     break;
                 }
                 case DecoderStateTables.CII_INDEX_LARGE_LARGE:
                 {
                     int index = (read() << 16) | 
                         (read() << 8) |
                         read();
                     index += EncodingConstants.INTEGER_4TH_BIT_LARGE_LIMIT;
                     final String s = _characterContentChunkTable.get(index).toString();
                     
                     _currentNode.appendChild(_document.createTextNode(s));
                     break;
                 }                       
                 case DecoderStateTables.COMMENT_II:
                     processCommentII();
                     break;
                 case DecoderStateTables.PROCESSING_INSTRUCTION_II:
                     processProcessingII();
                     break;
                 case DecoderStateTables.UNEXPANDED_ENTITY_REFERENCE_II:
                 {
                     String entity_reference_name = decodeIdentifyingNonEmptyStringOnFirstBit(_v.otherNCName);
                     
                     String system_identifier = ((_b & EncodingConstants.UNEXPANDED_ENTITY_SYSTEM_IDENTIFIER_FLAG) > 0) 
                         ? decodeIdentifyingNonEmptyStringOnFirstBit(_v.otherURI) : null;
                     String public_identifier = ((_b & EncodingConstants.UNEXPANDED_ENTITY_PUBLIC_IDENTIFIER_FLAG) > 0) 
                         ? decodeIdentifyingNonEmptyStringOnFirstBit(_v.otherURI) : null;
 
                     // TODO create Node
                     break;
                 }
                 case DecoderStateTables.TERMINATOR_DOUBLE:                    
                     _doubleTerminate = true; 
                 case DecoderStateTables.TERMINATOR_SINGLE:
                     _terminate = true;
                     break;
                 default:
                     throw new FastInfosetException(CommonResourceBundle.getInstance().getString("message.IllegalStateDecodingEII"));
             }
         }
 
         _terminate = _doubleTerminate;
         _doubleTerminate = false;
         
         _currentNode = parentCurrentNode;
     }
 
     protected final void processEIIWithNamespaces() throws FastInfosetException, IOException {
         final boolean hasAttributes = (_b & EncodingConstants.ELEMENT_ATTRIBUTE_FLAG) > 0;
 
         if (++_prefixTable._declarationId == Integer.MAX_VALUE) {
             _prefixTable.clearDeclarationIds();
         }
 
         String prefix;
         Attr a = null;
         final int start = _namespacePrefixesIndex;
         int b = read();
         while ((b & EncodingConstants.NAMESPACE_ATTRIBUTE_MASK) == EncodingConstants.NAMESPACE_ATTRIBUTE) {
             if (_namespaceAttributesIndex == _namespaceAttributes.length) {
                 final Attr[] newNamespaceAttributes = new Attr[_namespaceAttributesIndex * 3 / 2 + 1];
                 System.arraycopy(_namespaceAttributes, 0, newNamespaceAttributes, 0, _namespaceAttributesIndex);
                 _namespaceAttributes = newNamespaceAttributes;
             }
 
             if (_namespacePrefixesIndex == _namespacePrefixes.length) {
                 final int[] namespaceAIIs = new int[_namespacePrefixesIndex * 3 / 2 + 1];
                 System.arraycopy(_namespacePrefixes, 0, namespaceAIIs, 0, _namespacePrefixesIndex);
                 _namespacePrefixes = namespaceAIIs;
             }
 
             
             switch (b & EncodingConstants.NAMESPACE_ATTRIBUTE_PREFIX_NAME_MASK) {
                 // no prefix, no namespace
                 // Undeclaration of default namespace
                 case 0:
                     a = createAttribute(
                             EncodingConstants.XMLNS_NAMESPACE_NAME,
                             EncodingConstants.XMLNS_NAMESPACE_PREFIX, 
                             EncodingConstants.XMLNS_NAMESPACE_PREFIX);
                     a.setValue ("");
                     
                     _prefixIndex = _namespaceNameIndex = _namespacePrefixes[_namespacePrefixesIndex++] = -1;
                     break;
                 // no prefix, namespace
                 // Declaration of default namespace
                 case 1:
                     a = createAttribute(
                             EncodingConstants.XMLNS_NAMESPACE_NAME,
                             EncodingConstants.XMLNS_NAMESPACE_PREFIX, 
                             EncodingConstants.XMLNS_NAMESPACE_PREFIX);
                     a.setValue (decodeIdentifyingNonEmptyStringOnFirstBitAsNamespaceName(false));
                            
                     _prefixIndex = _namespacePrefixes[_namespacePrefixesIndex++] = -1;
                     break;
                 // prefix, no namespace
                 // Undeclaration of namespace
                 case 2:
                     prefix = decodeIdentifyingNonEmptyStringOnFirstBitAsPrefix(false);
                     a = createAttribute(
                             EncodingConstants.XMLNS_NAMESPACE_NAME,
                             createQualifiedNameString(EncodingConstants.XMLNS_NAMESPACE_PREFIX_CHARS, prefix),
                             prefix);
                     a.setValue ("");
                     
                     _namespaceNameIndex = -1;
                     _namespacePrefixes[_namespacePrefixesIndex++] = _prefixIndex;
                     break;
                 // prefix, namespace
                 // Declaration of prefixed namespace 
                 case 3:
                     prefix = decodeIdentifyingNonEmptyStringOnFirstBitAsPrefix(true);
                     a = createAttribute(
                             EncodingConstants.XMLNS_NAMESPACE_NAME,
                             createQualifiedNameString(EncodingConstants.XMLNS_NAMESPACE_PREFIX_CHARS, prefix),
                             prefix);
                     a.setValue (decodeIdentifyingNonEmptyStringOnFirstBitAsNamespaceName(true));
                     
                     _namespacePrefixes[_namespacePrefixesIndex++] = _prefixIndex;
                     break;
             }
                         
             _prefixTable.pushScope(_prefixIndex, _namespaceNameIndex);
             
             _namespaceAttributes[_namespaceAttributesIndex++] = a;
             
             b = read();
         }
         if (b != EncodingConstants.TERMINATOR) {
             throw new IOException(CommonResourceBundle.getInstance().getString("message.EIInamespaceNameNotTerminatedCorrectly"));
         }
         final int end = _namespacePrefixesIndex;
 
         _b = read();
         switch(DecoderStateTables.EII[_b]) {
             case DecoderStateTables.EII_NO_AIIS_INDEX_SMALL:
                 processEII(_elementNameTable._array[_b], hasAttributes);
                 break;
             case DecoderStateTables.EII_INDEX_MEDIUM:
                 processEII(decodeEIIIndexMedium(), hasAttributes);
                 break;
             case DecoderStateTables.EII_INDEX_LARGE:
                 processEII(decodeEIIIndexLarge(), hasAttributes);
                 break;
             case DecoderStateTables.EII_LITERAL:
             {
                 final QualifiedName qn = processLiteralQualifiedName(
                             _b & EncodingConstants.LITERAL_QNAME_PREFIX_NAMESPACE_NAME_MASK);
                 _elementNameTable.add(qn);
                 processEII(qn, hasAttributes);
                 break;
             }
             default:
                 throw new IOException(CommonResourceBundle.getInstance().getString("message.IllegalStateDecodingEIIAfterAIIs"));
         }
         
         for (int i = start; i < end; i++) {
             _prefixTable.popScope(_namespacePrefixes[i]);
         }
         _namespacePrefixesIndex = start;
         
     }
 
     protected final QualifiedName processLiteralQualifiedName(int state) throws FastInfosetException, IOException {
         switch (state) {
             // no prefix, no namespace
             case 0:
                 return new QualifiedName(
                         null, 
                         null, 
                         decodeIdentifyingNonEmptyStringOnFirstBit(_v.localName),
                         -1,
                         -1,
                         _identifier,
                         null);
             // no prefix, namespace
             case 1:
                 return new QualifiedName(
                         null,
                         decodeIdentifyingNonEmptyStringIndexOnFirstBitAsNamespaceName(false), 
                         decodeIdentifyingNonEmptyStringOnFirstBit(_v.localName),
                         -1,
                         _namespaceNameIndex,
                         _identifier,
                         null);
             // prefix, no namespace
             case 2:
                 throw new FastInfosetException(CommonResourceBundle.getInstance().getString("message.qNameMissingNamespaceName"));
             // prefix, namespace
             case 3:
                 return new QualifiedName(
                         decodeIdentifyingNonEmptyStringIndexOnFirstBitAsPrefix(true), 
                         decodeIdentifyingNonEmptyStringIndexOnFirstBitAsNamespaceName(true), 
                         decodeIdentifyingNonEmptyStringOnFirstBit(_v.localName),
                         _prefixIndex,
                         _namespaceNameIndex, 
                         _identifier,
                         _charBuffer);
             default:
                 throw new FastInfosetException(CommonResourceBundle.getInstance().getString("message.decodingEII"));                
         }        
     }
 
     protected final void processAIIs() throws FastInfosetException, IOException {
         QualifiedName name;
         int b;
         String value;
 
         if (++_duplicateAttributeVerifier._currentIteration == Integer.MAX_VALUE) {
             _duplicateAttributeVerifier.clear();
         }
         
         do {
             // AII qualified name
             b = read();
             switch (DecoderStateTables.AII[b]) {
                 case DecoderStateTables.AII_INDEX_SMALL:
                     name = _attributeNameTable._array[b];
                     break;
                 case DecoderStateTables.AII_INDEX_MEDIUM:
                 {
                     final int i = (((b & EncodingConstants.INTEGER_2ND_BIT_MEDIUM_MASK) << 8) | read()) 
                         + EncodingConstants.INTEGER_2ND_BIT_SMALL_LIMIT;            
                     name = _attributeNameTable._array[i];
                     break;
                 }
                 case DecoderStateTables.AII_INDEX_LARGE:
                 {
                     final int i = (((b & EncodingConstants.INTEGER_2ND_BIT_LARGE_MASK) << 16) | (read() << 8) | read()) 
                         + EncodingConstants.INTEGER_2ND_BIT_MEDIUM_LIMIT;
                     name = _attributeNameTable._array[i];
                     break;
                 }
                 case DecoderStateTables.AII_LITERAL:
                     name = processLiteralQualifiedName(
                             b & EncodingConstants.LITERAL_QNAME_PREFIX_NAMESPACE_NAME_MASK);
                     name.createAttributeValues(_duplicateAttributeVerifier.MAP_SIZE);
                     _attributeNameTable.add(name);
                     break;
                 case DecoderStateTables.AII_TERMINATOR_DOUBLE:                    
                     _doubleTerminate = true;
                 case DecoderStateTables.AII_TERMINATOR_SINGLE:
                     _terminate = true;
                     // AIIs have finished break out of loop
                     continue;
                 default:
                     throw new IOException(CommonResourceBundle.getInstance().getString("message.decodingAIIs"));
             }
 
             if (name.prefixIndex > 0 && _prefixTable._currentInScope[name.prefixIndex] != name.namespaceNameIndex) {
                 throw new FastInfosetException(CommonResourceBundle.getInstance().getString("message.AIIqNameNotInScope"));
             }
 
             _duplicateAttributeVerifier.checkForDuplicateAttribute(name.attributeHash, name.attributeId);
 
             Attr a = createAttribute(
                         name.namespaceName,
                         name.qName, 
                         name.localName);
                         
             // [normalized value] of AII
             
             b = read();
             switch(DecoderStateTables.NISTRING[b]) {
                 case DecoderStateTables.NISTRING_UTF8_SMALL_LENGTH:
                 {
                     final boolean addToTable = (b & EncodingConstants.NISTRING_ADD_TO_TABLE_FLAG) > 0;
                     _octetBufferLength = (b & EncodingConstants.OCTET_STRING_LENGTH_5TH_BIT_SMALL_MASK) + 1;
                     value = decodeUtf8StringAsString();
                     if (addToTable) {
                         _attributeValueTable.add(value);
                     }
                     
                     a.setValue(value);                
                     _currentElement.setAttributeNode(a);
                     break;
                 }
                 case DecoderStateTables.NISTRING_UTF8_MEDIUM_LENGTH:
                 {
                     final boolean addToTable = (b & EncodingConstants.NISTRING_ADD_TO_TABLE_FLAG) > 0;
                     _octetBufferLength = read() + EncodingConstants.OCTET_STRING_LENGTH_5TH_BIT_SMALL_LIMIT;
                     value = decodeUtf8StringAsString();
                     if (addToTable) {
                         _attributeValueTable.add(value);
                     }
                     
                     a.setValue(value);                
                     _currentElement.setAttributeNode(a);
                     break;
                 }
                 case DecoderStateTables.NISTRING_UTF8_LARGE_LENGTH:
                 {
                     final boolean addToTable = (b & EncodingConstants.NISTRING_ADD_TO_TABLE_FLAG) > 0;
                     final int length = (read() << 24) |
                         (read() << 16) |
                         (read() << 8) |
                         read();
                     _octetBufferLength = length + EncodingConstants.OCTET_STRING_LENGTH_5TH_BIT_MEDIUM_LIMIT;
                     value = decodeUtf8StringAsString();
                     if (addToTable) {
                         _attributeValueTable.add(value);
                     }
 
                     a.setValue(value);                
                     _currentElement.setAttributeNode(a);
                     break;
                 }
                 case DecoderStateTables.NISTRING_UTF16_SMALL_LENGTH:
                 {
                     final boolean addToTable = (b & EncodingConstants.NISTRING_ADD_TO_TABLE_FLAG) > 0;
                     _octetBufferLength = (b & EncodingConstants.OCTET_STRING_LENGTH_5TH_BIT_SMALL_MASK) + 1;
                     value = decodeUtf16StringAsString();
                     if (addToTable) {
                         _attributeValueTable.add(value);
                     }
                     
                     a.setValue(value);                
                     _currentElement.setAttributeNode(a);
                     break;
                 }
                 case DecoderStateTables.NISTRING_UTF16_MEDIUM_LENGTH:
                 {
                     final boolean addToTable = (b & EncodingConstants.NISTRING_ADD_TO_TABLE_FLAG) > 0;
                     _octetBufferLength = read() + EncodingConstants.OCTET_STRING_LENGTH_5TH_BIT_SMALL_LIMIT;
                     value = decodeUtf16StringAsString();
                     if (addToTable) {
                         _attributeValueTable.add(value);
                     }
                     
                     a.setValue(value);                
                     _currentElement.setAttributeNode(a);
                     break;
                 }
                 case DecoderStateTables.NISTRING_UTF16_LARGE_LENGTH:
                 {
                     final boolean addToTable = (b & EncodingConstants.NISTRING_ADD_TO_TABLE_FLAG) > 0;
                     final int length = (read() << 24) |
                         (read() << 16) |
                         (read() << 8) |
                         read();
                     _octetBufferLength = length + EncodingConstants.OCTET_STRING_LENGTH_5TH_BIT_MEDIUM_LIMIT;
                     value = decodeUtf16StringAsString();
                     if (addToTable) {
                         _attributeValueTable.add(value);
                     }
 
                     a.setValue(value);                
                     _currentElement.setAttributeNode(a);
                     break;
                 }
                 case DecoderStateTables.NISTRING_RA:
                 {
                     final boolean addToTable = (b & EncodingConstants.NISTRING_ADD_TO_TABLE_FLAG) > 0;
                     // Decode resitricted alphabet integer
                     _identifier = (b & 0x0F) << 4;
                     b = read();
                     _identifier |= (b & 0xF0) >> 4;
                     
                     decodeOctetsOnFifthBitOfNonIdentifyingStringOnFirstBit(b);
                     
                     value = decodeRestrictedAlphabetAsString();
                     if (addToTable) {
                         _attributeValueTable.add(value);
                     }
 
                     a.setValue(value);                
                     _currentElement.setAttributeNode(a);
                     break;
                 }
                 case DecoderStateTables.NISTRING_EA:
                 {
                     if ((b & EncodingConstants.NISTRING_ADD_TO_TABLE_FLAG) > 0) {
                         throw new EncodingAlgorithmException(CommonResourceBundle.getInstance().getString("message.addToTableNotSupported"));
                     }
 
                     _identifier = (b & 0x0F) << 4;
                     b = read();
                     _identifier |= (b & 0xF0) >> 4;
 
                     decodeOctetsOnFifthBitOfNonIdentifyingStringOnFirstBit(b);
                     value = convertEncodingAlgorithmDataToCharacters(true);
                     a.setValue(value);                
                     _currentElement.setAttributeNode(a);
                     break;
                 }
                 case DecoderStateTables.NISTRING_INDEX_SMALL:
                     value = _attributeValueTable._array[b & EncodingConstants.INTEGER_2ND_BIT_SMALL_MASK];
 
                     a.setValue(value);                
                     _currentElement.setAttributeNode(a);
                     break;
                 case DecoderStateTables.NISTRING_INDEX_MEDIUM:
                 {
                     final int index = (((b & EncodingConstants.INTEGER_2ND_BIT_MEDIUM_MASK) << 8) | read()) 
                         + EncodingConstants.INTEGER_2ND_BIT_SMALL_LIMIT;
                     value = _attributeValueTable._array[index];
 
                     a.setValue(value);                
                     _currentElement.setAttributeNode(a);
                     break;
                 }
                 case DecoderStateTables.NISTRING_INDEX_LARGE:
                 {
                     final int index = (((b & EncodingConstants.INTEGER_2ND_BIT_LARGE_MASK) << 16) | (read() << 8) | read()) 
                         + EncodingConstants.INTEGER_2ND_BIT_MEDIUM_LIMIT;
                     value = _attributeValueTable._array[index];
 
                     a.setValue(value);                
                     _currentElement.setAttributeNode(a);
                     break;
                 }
                 case DecoderStateTables.NISTRING_EMPTY:
                     a.setValue("");                
                     _currentElement.setAttributeNode(a);
                     break;
                 default:
                     throw new IOException(CommonResourceBundle.getInstance().getString("message.decodingAIIValue"));
             }
             
         } while (!_terminate);
  
         // Reset duplication attribute verfifier
         _duplicateAttributeVerifier._poolCurrent = _duplicateAttributeVerifier._poolHead;
         
         _terminate = _doubleTerminate;
         _doubleTerminate = false;
     }
         
     protected final void processCommentII() throws FastInfosetException, IOException {
         switch(decodeNonIdentifyingStringOnFirstBit()) {
             case NISTRING_STRING:
             {
                 final String s = new String(_charBuffer, 0, _charBufferLength);
                 if (_addToTable) {
                     _v.otherString.add(new CharArrayString(s, false));
                 }
                 
                 _currentNode.appendChild(_document.createComment(s));
                 break;
             }
             case NISTRING_ENCODING_ALGORITHM:
                 throw new IOException(CommonResourceBundle.getInstance().getString("message.commentIIAlgorithmNotSupported"));                        
             case NISTRING_INDEX:
             {
                 final String s = _v.otherString.get(_integer).toString();
 
                 _currentNode.appendChild(_document.createComment(s));
                 break;
             }
             case NISTRING_EMPTY_STRING:
                 _currentNode.appendChild(_document.createComment(""));
                 break;
         }        
     }
 
     protected final void processProcessingII() throws FastInfosetException, IOException {
         final String target = decodeIdentifyingNonEmptyStringOnFirstBit(_v.otherNCName);
 
         switch(decodeNonIdentifyingStringOnFirstBit()) {
             case NISTRING_STRING:
             {
                 final String data = new String(_charBuffer, 0, _charBufferLength);
                 if (_addToTable) {
                     _v.otherString.add(new CharArrayString(data, false));
                 }
 
                 _currentNode.appendChild (_document.createProcessingInstruction (target, data));
                 break;
             }
             case NISTRING_ENCODING_ALGORITHM:
                 throw new IOException(CommonResourceBundle.getInstance().getString("message.processingIIWithEncodingAlgorithm"));                        
             case NISTRING_INDEX:
             {
                 final String data = _v.otherString.get(_integer).toString();
                 
                 _currentNode.appendChild (_document.createProcessingInstruction (target, data));
                 break;
             }
             case NISTRING_EMPTY_STRING:
                 _currentNode.appendChild (_document.createProcessingInstruction (target, ""));
                 break;
         }
     }
     
     protected Element createElement(String namespaceName, String qName, String localName) {
         return _document.createElementNS(namespaceName, qName);
     }
     
     protected Attr createAttribute(String namespaceName, String qName, String localName) {
         return _document.createAttributeNS(namespaceName, qName);
     }
 
     protected String convertEncodingAlgorithmDataToCharacters(boolean isAttributeValue) throws FastInfosetException, IOException {
         StringBuffer buffer = new StringBuffer();
         if (_identifier < EncodingConstants.ENCODING_ALGORITHM_BUILTIN_END) {
             Object array = BuiltInEncodingAlgorithmFactory.table[_identifier].
                 decodeFromBytes(_octetBuffer, _octetBufferStart, _octetBufferLength);
             BuiltInEncodingAlgorithmFactory.table[_identifier].convertToCharacters(array,  buffer);
         } else if (_identifier == EncodingAlgorithmIndexes.CDATA) {
             if (!isAttributeValue) {
                 // Set back buffer position to start of encoded string
                 _octetBufferOffset -= _octetBufferLength;
                 return decodeUtf8StringAsString();                
             }
             throw new EncodingAlgorithmException(CommonResourceBundle.getInstance().getString("message.CDATAAlgorithmNotSupported"));            
         } else if (_identifier >= EncodingConstants.ENCODING_ALGORITHM_APPLICATION_START) {
             final String URI = _v.encodingAlgorithm.get(_identifier - EncodingConstants.ENCODING_ALGORITHM_APPLICATION_START);
             final EncodingAlgorithm ea = (EncodingAlgorithm)_registeredEncodingAlgorithms.get(URI);
             if (ea != null) {
                 final Object data = ea.decodeFromBytes(_octetBuffer, _octetBufferStart, _octetBufferLength);
                 ea.convertToCharacters(data, buffer);
             } else {
                 throw new EncodingAlgorithmException(
                         CommonResourceBundle.getInstance().getString("message.algorithmDataCannotBeReported"));
             }
         }
         return buffer.toString();
     }
     
 }
