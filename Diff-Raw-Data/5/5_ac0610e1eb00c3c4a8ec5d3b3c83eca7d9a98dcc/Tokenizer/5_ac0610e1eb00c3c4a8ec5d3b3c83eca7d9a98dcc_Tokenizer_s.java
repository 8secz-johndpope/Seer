 /*
  * Copyright (c) 2005, 2006, 2007 Henri Sivonen
  * Copyright (c) 2007-2008 Mozilla Foundation
  * Portions of comments Copyright 2004-2007 Apple Computer, Inc., Mozilla 
  * Foundation, and Opera Software ASA.
  *
  * Permission is hereby granted, free of charge, to any person obtaining a 
  * copy of this software and associated documentation files (the "Software"), 
  * to deal in the Software without restriction, including without limitation 
  * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
  * and/or sell copies of the Software, and to permit persons to whom the 
  * Software is furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in 
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
  * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
  * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
  * DEALINGS IN THE SOFTWARE.
  */
 
 /*
  * The comments following this one that use the same comment syntax as this 
  * comment are quotes from the WHATWG HTML 5 spec as of 2 June 2007 
  * amended as of June 18 2008.
  * That document came with this statement:
  * "© Copyright 2004-2007 Apple Computer, Inc., Mozilla Foundation, and 
  * Opera Software ASA. You are granted a license to use, reproduce and 
  * create derivative works of this document."
  */
 
 package nu.validator.htmlparser.impl;
 
 import nu.validator.htmlparser.annotation.Local;
 import nu.validator.htmlparser.annotation.NoLength;
 import nu.validator.htmlparser.common.EncodingDeclarationHandler;
 import nu.validator.htmlparser.common.TokenHandler;
 import nu.validator.htmlparser.common.XmlViolationPolicy;
 
 import org.xml.sax.ErrorHandler;
 import org.xml.sax.Locator;
 import org.xml.sax.SAXException;
 import org.xml.sax.SAXParseException;
 
 /**
  * An implementation of
  * http://www.whatwg.org/specs/web-apps/current-work/multipage/tokenization.html
  * 
  * This class implements the <code>Locator</code> interface. This is not an
  * incidental implementation detail: Users of this class are encouraged to make
  * use of the <code>Locator</code> nature.
  * 
  * By default, the tokenizer may report data that XML 1.0 bans. The tokenizer
  * can be configured to treat these conditions as fatal or to coerce the infoset
  * to something that XML 1.0 allows.
  * 
  * @version $Id$
  * @author hsivonen
  */
 public final class Tokenizer implements Locator {
 
     private static final int DATA = 0;
 
     private static final int RCDATA = 1;
 
     private static final int CDATA = 2;
 
     private static final int PLAINTEXT = 3;
 
     private static final int TAG_OPEN = 49;
 
     private static final int CLOSE_TAG_OPEN_PCDATA = 50;
 
     private static final int TAG_NAME = 58;
 
     private static final int BEFORE_ATTRIBUTE_NAME = 4;
 
     private static final int ATTRIBUTE_NAME = 5;
 
     private static final int AFTER_ATTRIBUTE_NAME = 6;
 
     private static final int BEFORE_ATTRIBUTE_VALUE = 7;
 
     private static final int ATTRIBUTE_VALUE_DOUBLE_QUOTED = 8;
 
     private static final int ATTRIBUTE_VALUE_SINGLE_QUOTED = 9;
 
     private static final int ATTRIBUTE_VALUE_UNQUOTED = 10;
 
     private static final int AFTER_ATTRIBUTE_VALUE_QUOTED = 11;
 
     private static final int BOGUS_COMMENT = 12;
 
     private static final int MARKUP_DECLARATION_OPEN = 13;
 
     private static final int DOCTYPE = 14;
 
     private static final int BEFORE_DOCTYPE_NAME = 15;
 
     private static final int DOCTYPE_NAME = 16;
 
     private static final int AFTER_DOCTYPE_NAME = 17;
 
     private static final int BEFORE_DOCTYPE_PUBLIC_IDENTIFIER = 18;
 
     private static final int DOCTYPE_PUBLIC_IDENTIFIER_DOUBLE_QUOTED = 19;
 
     private static final int DOCTYPE_PUBLIC_IDENTIFIER_SINGLE_QUOTED = 20;
 
     private static final int AFTER_DOCTYPE_PUBLIC_IDENTIFIER = 21;
 
     private static final int BEFORE_DOCTYPE_SYSTEM_IDENTIFIER = 22;
 
     private static final int DOCTYPE_SYSTEM_IDENTIFIER_DOUBLE_QUOTED = 23;
 
     private static final int DOCTYPE_SYSTEM_IDENTIFIER_SINGLE_QUOTED = 24;
 
     private static final int AFTER_DOCTYPE_SYSTEM_IDENTIFIER = 25;
 
     private static final int BOGUS_DOCTYPE = 26;
 
     private static final int COMMENT_START = 27;
 
     private static final int COMMENT_START_DASH = 28;
 
     private static final int COMMENT = 29;
 
     private static final int COMMENT_END_DASH = 30;
 
     private static final int COMMENT_END = 31;
 
     private static final int CLOSE_TAG_OPEN_NOT_PCDATA = 32;
 
     private static final int MARKUP_DECLARATION_HYPHEN = 33;
 
     private static final int MARKUP_DECLARATION_OCTYPE = 34;
 
     private static final int DOCTYPE_UBLIC = 35;
 
     private static final int DOCTYPE_YSTEM = 36;
 
     private static final int CONSUME_CHARACTER_REFERENCE = 37;
 
     private static final int CONSUME_NCR = 38;
 
     private static final int CHARACTER_REFERENCE_LOOP = 39;
 
     private static final int HEX_NCR_LOOP = 41;
 
     private static final int DECIMAL_NRC_LOOP = 42;
 
     private static final int HANDLE_NCR_VALUE = 43;
 
     private static final int SELF_CLOSING_START_TAG = 44;
 
     private static final int CDATA_START = 45;
 
     private static final int CDATA_SECTION = 46;
 
     private static final int CDATA_RSQB = 47;
 
     private static final int CDATA_RSQB_RSQB = 48;
 
     private static final int TAG_OPEN_NON_PCDATA = 51;
 
     private static final int ESCAPE_EXCLAMATION = 52;
 
     private static final int ESCAPE_EXCLAMATION_HYPHEN = 53;
 
     private static final int ESCAPE = 54;
 
     private static final int ESCAPE_HYPHEN = 55;
 
     private static final int ESCAPE_HYPHEN_HYPHEN = 56;
 
     private static final int BOGUS_COMMENT_HYPHEN = 57;
 
     /**
      * Magic value for UTF-16 operations.
      */
     private static final int LEAD_OFFSET = 0xD800 - (0x10000 >> 10);
 
     /**
      * Magic value for UTF-16 operations.
      */
     private static final int SURROGATE_OFFSET = 0x10000 - (0xD800 << 10) - 0xDC00;
 
     /**
      * UTF-16 code unit array containing less than and greater than for emitting
      * those characters on certain parse errors.
      */
     private static final @NoLength char[] LT_GT = { '<', '>' };
 
     /**
      * UTF-16 code unit array containing less than and solidus for emitting
      * those characters on certain parse errors.
      */
     private static final @NoLength char[] LT_SOLIDUS = { '<', '/' };
 
     /**
      * UTF-16 code unit array containing ]] for emitting those characters on
      * state transitions.
      */
     private static final @NoLength char[] RSQB_RSQB = { ']', ']' };
 
     /**
      * Array version of U+FFFD.
      */
     private static final char[] REPLACEMENT_CHARACTER = { '\uFFFD' };
 
     /**
      * Array version of space.
      */
     private static final char[] SPACE = { ' ' };
 
     /**
      * Array version of line feed.
      */
     private static final char[] LF = { '\n' };
 
     /**
      * Buffer growth parameter.
      */
     private static final int BUFFER_GROW_BY = 1024;
 
     /**
      * "CDATA[" as <code>char[]</code>
      */
     private static final @NoLength char[] CDATA_LSQB = "CDATA[".toCharArray();
 
     /**
      * "octype" as <code>char[]</code>
      */
     private static final @NoLength char[] OCTYPE = "octype".toCharArray();
 
     /**
      * "ublic" as <code>char[]</code>
      */
     private static final @NoLength char[] UBLIC = "ublic".toCharArray();
 
     /**
      * "ystem" as <code>char[]</code>
      */
     private static final @NoLength char[] YSTEM = "ystem".toCharArray();
 
     private final EncodingDeclarationHandler encodingDeclarationHandler;
 
     /**
      * The token handler.
      */
     private final TokenHandler tokenHandler;
 
     /**
      * The error handler.
      */
     private ErrorHandler errorHandler;
 
     /**
      * The previous <code>char</code> read from the buffer with infoset
      * alteration applied except for CR. Used for CRLF normalization and
      * surrogate pair checking.
      */
     private char prev;
 
     /**
      * The current line number in the current resource being parsed. (First line
      * is 1.) Passed on as locator data.
      */
     private int line;
 
     private int linePrev;
 
     /**
      * The current column number in the current resource being tokenized. (First
      * column is 1, counted by UTF-16 code units.) Passed on as locator data.
      */
     private int col;
 
     private int colPrev;
 
     private boolean nextCharOnNewLine;
 
     private int stateSave;
 
     private int returnStateSave;
 
     private int index;
 
     private boolean forceQuirks;
 
     private char additional;
 
     private int entCol;
 
     private int lo;
 
     private int hi;
 
     private int candidate;
 
     private int strBufMark;
 
     private int prevValue;
 
     private int value;
 
     private boolean seenDigits;
 
     private int pos;
 
     private int end;
 
     private @NoLength char[] buf;
 
     private int cstart;
 
     /**
      * The SAX public id for the resource being tokenized. (Only passed to back
      * as part of locator data.)
      */
     private String publicId;
 
     /**
      * The SAX system id for the resource being tokenized. (Only passed to back
      * as part of locator data.)
      */
     private String systemId;
 
     /**
      * Buffer for short identifiers.
      */
     private char[] strBuf;
 
     /**
      * Number of significant <code>char</code>s in <code>strBuf</code>.
      */
     private int strBufLen = 0;
 
     /**
      * <code>-1</code> to indicate that <code>strBuf</code> is used or
      * otherwise an offset to the main buffer.
      */
     private int strBufOffset = -1;
 
     /**
      * Buffer for long strings.
      */
     private char[] longStrBuf;
 
     /**
      * Number of significant <code>char</code>s in <code>longStrBuf</code>.
      */
     private int longStrBufLen = 0;
 
     /**
      * <code>-1</code> to indicate that <code>longStrBuf</code> is used or
      * otherwise an offset to the main buffer.
      */
     private int longStrBufOffset = -1;
 
     /**
      * The attribute holder.
      */
     private HtmlAttributes attributes;
 
     /**
      * Buffer for expanding NCRs falling into the Basic Multilingual Plane.
      */
     private final char[] bmpChar = new char[1];
 
     /**
      * Buffer for expanding astral NCRs.
      */
     private final char[] astralChar = new char[2];
 
     /**
      * Keeps track of PUA warnings.
      */
     private boolean alreadyWarnedAboutPrivateUseCharacters;
 
     /**
      * http://www.whatwg.org/specs/web-apps/current-work/#content2
      */
     private ContentModelFlag contentModelFlag = ContentModelFlag.PCDATA;
 
     /**
      * The element whose end tag closes the current CDATA or RCDATA element.
      */
     private ElementName contentModelElement = null;
 
     /**
      * <code>true</code> if tokenizing an end tag
      */
     private boolean endTag;
 
     /**
      * The current tag token name.
      */
     private ElementName tagName = null;
 
     /**
      * The current attribute name.
      */
     private AttributeName attributeName = null;
 
     /**
      * Whether comment tokens are emitted.
      */
     private boolean wantsComments = false;
 
     /**
      * If <code>false</code>, <code>addAttribute*()</code> are no-ops.
      */
     private boolean shouldAddAttributes;
 
     /**
      * <code>true</code> when HTML4-specific additional errors are requested.
      */
     private boolean html4;
 
     /**
      * Used together with <code>nonAsciiProhibited</code>.
      */
     private boolean alreadyComplainedAboutNonAscii;
 
     /**
      * Whether the stream is past the first 512 bytes.
      */
     private boolean metaBoundaryPassed;
 
     /**
      * The name of the current doctype token.
      */
     private @Local String doctypeName;
 
     /**
      * The public id of the current doctype token.
      */
     private String publicIdentifier;
 
     /**
      * The system id of the current doctype token.
      */
     private String systemIdentifier;
 
     // [NOCPP[
 
     /**
      * The policy for vertical tab and form feed.
      */
     private XmlViolationPolicy contentSpacePolicy = XmlViolationPolicy.ALTER_INFOSET;
 
     /**
      * The policy for non-space non-XML characters.
      */
     private XmlViolationPolicy contentNonXmlCharPolicy = XmlViolationPolicy.ALTER_INFOSET;
 
     /**
      * The policy for comments.
      */
     private XmlViolationPolicy commentPolicy = XmlViolationPolicy.ALTER_INFOSET;
 
     private XmlViolationPolicy xmlnsPolicy = XmlViolationPolicy.ALTER_INFOSET;
 
     private XmlViolationPolicy namePolicy = XmlViolationPolicy.ALTER_INFOSET;
 
     private boolean html4ModeCompatibleWithXhtml1Schemata;
 
     private final boolean newAttributesEachTime;
 
     // ]NOCPP]
 
     private int mappingLangToXmlLang;
 
     private boolean shouldSuspend;
 
     private boolean confident;
 
     private PushedLocation pushedLocation;
 
     /**
      * The constructor.
      * 
      * @param tokenHandler
      *            the handler for receiving tokens
      */
     public Tokenizer(TokenHandler tokenHandler) {
         this.tokenHandler = tokenHandler;
         this.encodingDeclarationHandler = null;
         this.newAttributesEachTime = false;
     }
 
     public Tokenizer(TokenHandler tokenHandler,
             EncodingDeclarationHandler encodingDeclarationHandler) {
         this.tokenHandler = tokenHandler;
         this.encodingDeclarationHandler = encodingDeclarationHandler;
         this.newAttributesEachTime = false;
     }
 
     public void initLocation(String newPublicId, String newSystemId) {
         this.systemId = newSystemId;
         this.publicId = newPublicId;
 
     }
 
     // [NOCPP[
 
     public Tokenizer(TokenHandler tokenHandler,
             EncodingDeclarationHandler encodingDeclarationHandler,
             boolean newAttributesEachTime) {
         this.tokenHandler = tokenHandler;
         this.encodingDeclarationHandler = encodingDeclarationHandler;
         this.newAttributesEachTime = newAttributesEachTime;
     }
 
     // ]NOCPP]
 
     /**
      * Returns the mappingLangToXmlLang.
      * 
      * @return the mappingLangToXmlLang
      */
     public boolean isMappingLangToXmlLang() {
         return mappingLangToXmlLang == AttributeName.HTML_LANG;
     }
 
     /**
      * Sets the mappingLangToXmlLang.
      * 
      * @param mappingLangToXmlLang
      *            the mappingLangToXmlLang to set
      */
     public void setMappingLangToXmlLang(boolean mappingLangToXmlLang) {
         this.mappingLangToXmlLang = mappingLangToXmlLang ? AttributeName.HTML_LANG
                 : AttributeName.HTML;
     }
 
     /**
      * Sets the error handler.
      * 
      * @see org.xml.sax.XMLReader#setErrorHandler(org.xml.sax.ErrorHandler)
      */
     public void setErrorHandler(ErrorHandler eh) {
         this.errorHandler = eh;
     }
 
     public ErrorHandler getErrorHandler() {
         return this.errorHandler;
     }
 
     // [NOCPP[
 
     /**
      * Sets the commentPolicy.
      * 
      * @param commentPolicy
      *            the commentPolicy to set
      */
     public void setCommentPolicy(XmlViolationPolicy commentPolicy) {
         this.commentPolicy = commentPolicy;
     }
 
     /**
      * Sets the contentNonXmlCharPolicy.
      * 
      * @param contentNonXmlCharPolicy
      *            the contentNonXmlCharPolicy to set
      */
     public void setContentNonXmlCharPolicy(
             XmlViolationPolicy contentNonXmlCharPolicy) {
         this.contentNonXmlCharPolicy = contentNonXmlCharPolicy;
     }
 
     /**
      * Sets the contentSpacePolicy.
      * 
      * @param contentSpacePolicy
      *            the contentSpacePolicy to set
      */
     public void setContentSpacePolicy(XmlViolationPolicy contentSpacePolicy) {
         this.contentSpacePolicy = contentSpacePolicy;
     }
 
     /**
      * Sets the xmlnsPolicy.
      * 
      * @param xmlnsPolicy
      *            the xmlnsPolicy to set
      */
     public void setXmlnsPolicy(XmlViolationPolicy xmlnsPolicy) {
         if (xmlnsPolicy == XmlViolationPolicy.FATAL) {
             throw new IllegalArgumentException("Can't use FATAL here.");
         }
         this.xmlnsPolicy = xmlnsPolicy;
     }
 
     public void setNamePolicy(XmlViolationPolicy namePolicy) {
         this.namePolicy = namePolicy;
     }
 
     /**
      * Sets the html4ModeCompatibleWithXhtml1Schemata.
      * 
      * @param html4ModeCompatibleWithXhtml1Schemata
      *            the html4ModeCompatibleWithXhtml1Schemata to set
      */
     public void setHtml4ModeCompatibleWithXhtml1Schemata(
             boolean html4ModeCompatibleWithXhtml1Schemata) {
         this.html4ModeCompatibleWithXhtml1Schemata = html4ModeCompatibleWithXhtml1Schemata;
     }
 
     // ]NOCPP]
 
     // For the token handler to call
     /**
      * Sets the content model flag and the associated element name.
      * 
      * @param contentModelFlag
      *            the flag
      * @param contentModelElement
      *            the element causing the flag to be set
      */
     public void setContentModelFlag(ContentModelFlag contentModelFlag,
             @Local String contentModelElement) {
         this.contentModelFlag = contentModelFlag;
         char[] asArray = Portability.newCharArrayFromLocal(contentModelElement);
         this.contentModelElement = ElementName.elementNameByBuffer(asArray, 0,
                 asArray.length);
     }
 
     /**
      * Sets the content model flag and the associated element name.
      * 
      * @param contentModelFlag
      *            the flag
      * @param contentModelElement
      *            the element causing the flag to be set
      */
     public void setContentModelFlag(ContentModelFlag contentModelFlag,
             ElementName contentModelElement) {
         this.contentModelFlag = contentModelFlag;
         this.contentModelElement = contentModelElement;
     }
 
     // start Locator impl
 
     /**
      * @see org.xml.sax.Locator#getPublicId()
      */
     public String getPublicId() {
         return publicId;
     }
 
     /**
      * @see org.xml.sax.Locator#getSystemId()
      */
     public String getSystemId() {
         return systemId;
     }
 
     /**
      * @see org.xml.sax.Locator#getLineNumber()
      */
     public int getLineNumber() {
         if (line > 0) {
             return line;
         } else {
             return -1;
         }
     }
 
     /**
      * @see org.xml.sax.Locator#getColumnNumber()
      */
     public int getColumnNumber() {
         if (col > 0) {
             return col;
         } else {
             return -1;
         }
     }
 
     // end Locator impl
 
     // end public API
 
     public void notifyAboutMetaBoundary() {
         metaBoundaryPassed = true;
     }
 
     // [NOCPP[
 
     void turnOnAdditionalHtml4Errors() {
         html4 = true;
     }
 
     // ]NOCPP]
 
     HtmlAttributes emptyAttributes() {
         // [NOCPP[
         if (newAttributesEachTime) {
             return new HtmlAttributes(mappingLangToXmlLang);
         } else {
             // ]NOCPP]
             return HtmlAttributes.EMPTY_ATTRIBUTES;
             // [NOCPP[
         }
         // ]NOCPP]
     }
 
     private void detachStrBuf() {
         // if (strBufOffset == -1) {
         // return;
         // }
         // if (strBufLen > 0) {
         // if (strBuf.length < strBufLen) {
         // Portability.releaseArray(strBuf);
         // strBuf = new char[strBufLen + (strBufLen >> 1)];
         // }
         // System.arraycopy(buf, strBufOffset, strBuf, 0, strBufLen);
         // }
         // strBufOffset = -1;
     }
 
     private void detachLongStrBuf() {
         // if (longStrBufOffset == -1) {
         // return;
         // }
         // if (longStrBufLen > 0) {
         // if (longStrBuf.length < longStrBufLen) {
         // Portability.releaseArray(longStrBuf);
         // longStrBuf = new char[longStrBufLen + (longStrBufLen >> 1)];
         // }
         // System.arraycopy(buf, longStrBufOffset, longStrBuf, 0,
         // longStrBufLen);
         // }
         // longStrBufOffset = -1;
     }
 
     private void clearStrBufAndAppendCurrentC(char c) {
         strBuf[0] = c;
 
         strBufLen = 1;
         // strBufOffset = pos;
     }
 
     private void clearStrBufAndAppendForceWrite(char c) {
         strBuf[0] = c; // test
 
         strBufLen = 1;
         // strBufOffset = pos;
         // buf[pos] = c;
     }
 
     private void clearStrBufForNextState() {
         strBufLen = 0;
         // strBufOffset = pos + 1;
     }
 
     /**
      * Appends to the smaller buffer.
      * 
      * @param c
      *            the UTF-16 code unit to append
      */
     private void appendStrBuf(char c) {
         // if (strBufOffset != -1) {
         // strBufLen++;
         // } else {
         if (strBufLen == strBuf.length) {
             char[] newBuf = new char[strBuf.length + Tokenizer.BUFFER_GROW_BY];
             System.arraycopy(strBuf, 0, newBuf, 0, strBuf.length);
             strBuf = newBuf;
         }
         strBuf[strBufLen++] = c;
         // }
     }
 
     /**
      * Appends to the smaller buffer.
      * 
      * @param c
      *            the UTF-16 code unit to append
      */
     private void appendStrBufForceWrite(char c) {
         // if (strBufOffset != -1) {
         // strBufLen++;
         // buf[pos] = c;
         // } else {
         if (strBufLen == strBuf.length) {
             char[] newBuf = new char[strBuf.length + Tokenizer.BUFFER_GROW_BY];
             System.arraycopy(strBuf, 0, newBuf, 0, strBuf.length);
             strBuf = newBuf;
         }
         strBuf[strBufLen++] = c;
         // }
     }
 
     /**
      * The smaller buffer as a String.
      * 
      * @return the smaller buffer as a string
      */
     private String strBufToString() {
         // if (strBufOffset != -1) {
         // return Portability.newStringFromBuffer(buf, strBufOffset, strBufLen);
         // } else {
         return Portability.newStringFromBuffer(strBuf, 0, strBufLen);
         // }
     }
 
     /**
      * Emits the smaller buffer as character tokens.
      * 
      * @throws SAXException
      *             if the token handler threw
      */
     private void emitStrBuf() throws SAXException {
         if (strBufLen > 0) {
             // if (strBufOffset != -1) {
             // tokenHandler.characters(buf, strBufOffset, strBufLen);
             // } else {
             tokenHandler.characters(strBuf, 0, strBufLen);
             // }
         }
     }
 
     private void clearLongStrBufForNextState() {
         // longStrBufOffset = pos + 1;
         longStrBufLen = 0;
     }
 
     private void clearLongStrBuf() {
         // longStrBufOffset = pos;
         longStrBufLen = 0;
     }
 
     private void clearLongStrBufAndAppendCurrentC() {
         longStrBuf[0] = buf[pos];
         longStrBufLen = 1;
         // longStrBufOffset = pos;
     }
 
     private void clearLongStrBufAndAppendToComment(char c) {
         longStrBuf[0] = c;
         // longStrBufOffset = pos;
         longStrBufLen = 1;
     }
 
     /**
      * Appends to the larger buffer.
      * 
      * @param c
      *            the UTF-16 code unit to append
      */
     private void appendLongStrBuf(char c) {
         // if (longStrBufOffset != -1) {
         // longStrBufLen++;
         // } else {
         if (longStrBufLen == longStrBuf.length) {
             char[] newBuf = new char[longStrBufLen + (longStrBufLen >> 1)];
             System.arraycopy(longStrBuf, 0, newBuf, 0, longStrBuf.length);
             Portability.releaseArray(longStrBuf);
             longStrBuf = newBuf;
         }
         longStrBuf[longStrBufLen++] = c;
         // }
     }
 
     private void appendSecondHyphenToBogusComment() throws SAXException {
         switch (commentPolicy) {
             case ALTER_INFOSET:
                 // detachLongStrBuf();
                 appendLongStrBuf(' ');
                 // FALLTHROUGH
             case ALLOW:
                 warn("The document is not mappable to XML 1.0 due to two consecutive hyphens in a comment.");
                 appendLongStrBuf('-');
                 break;
             case FATAL:
                 fatal("The document is not mappable to XML 1.0 due to two consecutive hyphens in a comment.");
                 break;
         }
     }
 
     private void maybeAppendSpaceToBogusComment() throws SAXException {
         switch (commentPolicy) {
             case ALTER_INFOSET:
                 // detachLongStrBuf();
                 appendLongStrBuf(' ');
                 // FALLTHROUGH
             case ALLOW:
                 warn("The document is not mappable to XML 1.0 due to a trailing hyphen in a comment.");
                 break;
             case FATAL:
                 fatal("The document is not mappable to XML 1.0 due to a trailing hyphen in a comment.");
                 break;
         }
     }
 
     private void adjustDoubleHyphenAndAppendToLongStrBuf(char c)
             throws SAXException {
         switch (commentPolicy) {
             case ALTER_INFOSET:
                 // detachLongStrBuf();
                 longStrBufLen--;
                 appendLongStrBuf(' ');
                 appendLongStrBuf('-');
                 // FALLTHROUGH
             case ALLOW:
                 warn("The document is not mappable to XML 1.0 due to two consecutive hyphens in a comment.");
                 appendLongStrBuf(c);
                 break;
             case FATAL:
                 fatal("The document is not mappable to XML 1.0 due to two consecutive hyphens in a comment.");
                 break;
         }
     }
 
     private void appendLongStrBuf(char[] buffer, int offset, int length) {
         int reqLen = longStrBufLen + length;
         if (longStrBuf.length < reqLen) {
             char[] newBuf = new char[reqLen + (reqLen >> 1)];
             System.arraycopy(longStrBuf, 0, newBuf, 0, longStrBuf.length);
             Portability.releaseArray(longStrBuf);
             longStrBuf = newBuf;
         }
         System.arraycopy(buffer, offset, longStrBuf, longStrBufLen, length);
         longStrBufLen = reqLen;
     }
 
     /**
      * Appends to the larger buffer.
      * 
      * @param arr
      *            the UTF-16 code units to append
      */
     private void appendLongStrBuf(char[] arr) {
         // assert longStrBufOffset == -1;
         appendLongStrBuf(arr, 0, arr.length);
     }
 
     /**
      * Append the contents of the smaller buffer to the larger one.
      */
     private void appendStrBufToLongStrBuf() {
         // assert longStrBufOffset == -1;
         // if (strBufOffset != -1) {
         // appendLongStrBuf(buf, strBufOffset, strBufLen);
         // } else {
         appendLongStrBuf(strBuf, 0, strBufLen);
         // }
     }
 
     /**
      * The larger buffer as a string.
      * 
      * @return the larger buffer as a string
      */
     private String longStrBufToString() {
         // if (longStrBufOffset != -1) {
         // return Portability.newStringFromBuffer(buf, longStrBufOffset,
         // longStrBufLen);
         // } else {
         return Portability.newStringFromBuffer(longStrBuf, 0, longStrBufLen);
         // }
     }
 
     /**
      * Emits the current comment token.
      * 
      * @throws SAXException
      */
     private void emitComment(int provisionalHyphens) throws SAXException {
         if (wantsComments) {
             // if (longStrBufOffset != -1) {
             // tokenHandler.comment(buf, longStrBufOffset, longStrBufLen
             // - provisionalHyphens);
             // } else {
             tokenHandler.comment(longStrBuf, 0, longStrBufLen
                     - provisionalHyphens);
             // }
         }
         cstart = pos + 1;
     }
 
     // [NOCPP[
 
     private String toUPlusString(char c) {
         String hexString = Integer.toHexString(c);
         switch (hexString.length()) {
             case 1:
                 return "U+000" + hexString;
             case 2:
                 return "U+00" + hexString;
             case 3:
                 return "U+0" + hexString;
             case 4:
                 return "U+" + hexString;
             default:
                 throw new RuntimeException("Unreachable.");
         }
     }
 
     // ]NOCPP]
 
     /**
      * Emits a warning about private use characters if the warning has not been
      * emitted yet.
      * 
      * @throws SAXException
      */
     private void warnAboutPrivateUseChar() throws SAXException {
         if (!alreadyWarnedAboutPrivateUseCharacters) {
             warn("Document uses the Unicode Private Use Area(s), which should not be used in publicly exchanged documents. (Charmod C073)");
             alreadyWarnedAboutPrivateUseCharacters = true;
         }
     }
 
     /**
      * Tells if the argument is a BMP PUA character.
      * 
      * @param c
      *            the UTF-16 code unit to check
      * @return <code>true</code> if PUA character
      */
     private boolean isPrivateUse(char c) {
         return c >= '\uE000' && c <= '\uF8FF';
     }
 
     /**
      * Tells if the argument is an astral PUA character.
      * 
      * @param c
      *            the code point to check
      * @return <code>true</code> if astral private use
      */
     private boolean isAstralPrivateUse(int c) {
         return (c >= 0xF0000 && c <= 0xFFFFD)
                 || (c >= 0x100000 && c <= 0x10FFFD);
     }
 
     /**
      * Tells if the argument is a non-character (works for BMP and astral).
      * 
      * @param c
      *            the code point to check
      * @return <code>true</code> if non-character
      */
     private boolean isNonCharacter(int c) {
         return (c & 0xFFFE) == 0xFFFE;
     }
 
     /**
      * Flushes coalesced character tokens.
      * 
      * @throws SAXException
      */
     private void flushChars() throws SAXException {
         if (pos > cstart) {
             int currLine = line;
             int currCol = col;
             line = linePrev;
             col = colPrev;
             tokenHandler.characters(buf, cstart, pos - cstart);
             line = currLine;
             col = currCol;
         }
         cstart = Integer.MAX_VALUE;
     }
 
     /**
      * Reports an condition that would make the infoset incompatible with XML
      * 1.0 as fatal.
      * 
      * @param message
      *            the message
      * @throws SAXException
      * @throws SAXParseException
      */
     public void fatal(String message) throws SAXException {
         SAXParseException spe = new SAXParseException(message, this);
         if (errorHandler != null) {
             errorHandler.fatalError(spe);
         }
         throw spe;
     }
 
     /**
      * Reports a Parse Error.
      * 
      * @param message
      *            the message
      * @throws SAXException
      */
     public void err(String message) throws SAXException {
         if (errorHandler == null) {
             return;
         }
         SAXParseException spe = new SAXParseException(message, this);
         errorHandler.error(spe);
     }
 
     public void errTreeBuilder(String message) throws SAXException {
         ErrorHandler eh = null;
         if (tokenHandler instanceof TreeBuilder<?>) {
             TreeBuilder<?> treeBuilder = (TreeBuilder<?>) tokenHandler;
             eh = treeBuilder.getErrorHandler();
         }
         if (eh == null) {
             eh = errorHandler;
         }
         if (eh == null) {
             return;
         }
         SAXParseException spe = new SAXParseException(message, this);
         eh.error(spe);
     }
 
     /**
      * Reports a warning
      * 
      * @param message
      *            the message
      * @throws SAXException
      */
     public void warn(String message) throws SAXException {
         if (errorHandler == null) {
             return;
         }
         SAXParseException spe = new SAXParseException(message, this);
         errorHandler.warning(spe);
     }
 
     /**
      * 
      */
     private void resetAttributes() {
         // [NOCPP[
         if (newAttributesEachTime) {
             attributes = null;
         } else {
             // ]NOCPP]
             attributes.clear();
             // [NOCPP[
         }
         // ]NOCPP]
     }
 
     private ElementName strBufToElementNameString() {
         // if (strBufOffset != -1) {
         // return ElementName.elementNameByBuffer(buf, strBufOffset, strBufLen);
         // } else {
         return ElementName.elementNameByBuffer(strBuf, 0, strBufLen);
         // }
     }
 
     private int emitCurrentTagToken(boolean selfClosing) throws SAXException {
         cstart = pos + 1;
         if (selfClosing && endTag) {
             err("Stray \u201C/\u201D at the end of an end tag.");
         }
         int rv = Tokenizer.DATA;
         HtmlAttributes attrs = (attributes == null ? HtmlAttributes.EMPTY_ATTRIBUTES
                 : attributes);
         if (endTag) {
             /*
              * When an end tag token is emitted, the content model flag must be
              * switched to the PCDATA state.
              */
             contentModelFlag = ContentModelFlag.PCDATA;
             if (attrs.getLength() != 0) {
                 /*
                  * When an end tag token is emitted with attributes, that is a
                  * parse error.
                  */
                 err("End tag had attributes.");
             }
             tokenHandler.endTag(tagName);
         } else {
             tokenHandler.startTag(tagName, attrs, selfClosing);
             switch (contentModelFlag) {
                 case PCDATA:
                     rv = Tokenizer.DATA;
                     break;
                 case CDATA:
                     rv = Tokenizer.CDATA;
                     break;
                 case RCDATA:
                     rv = Tokenizer.RCDATA;
                     break;
                 case PLAINTEXT:
                     rv = Tokenizer.PLAINTEXT;
             }
         }
         resetAttributes();
         return rv;
     }
 
     private void attributeNameComplete() throws SAXException {
         // if (strBufOffset != -1) {
         // attributeName = AttributeName.nameByBuffer(buf, strBufOffset,
         // strBufLen, namePolicy != XmlViolationPolicy.ALLOW);
         // } else {
         attributeName = AttributeName.nameByBuffer(strBuf, 0, strBufLen,
                 namePolicy != XmlViolationPolicy.ALLOW);
         // }
 
         // [NOCPP[
         if (attributes == null) {
             attributes = new HtmlAttributes(mappingLangToXmlLang);
         }
         // ]NOCPP]
 
         /*
          * When the user agent leaves the attribute name state (and before
          * emitting the tag token, if appropriate), the complete attribute's
          * name must be compared to the other attributes on the same token; if
          * there is already an attribute on the token with the exact same name,
          * then this is a parse error and the new attribute must be dropped,
          * along with the value that gets associated with it (if any).
          */
         if (attributes.contains(attributeName)) {
             shouldAddAttributes = false;
             err("Duplicate attribute \u201C"
                     + attributeName.getLocal(AttributeName.HTML) + "\u201D.");
         } else {
             shouldAddAttributes = true;
             // if (namePolicy == XmlViolationPolicy.ALLOW) {
             // shouldAddAttributes = true;
             // } else {
             // if (NCName.isNCName(attributeName)) {
             // shouldAddAttributes = true;
             // } else {
             // if (namePolicy == XmlViolationPolicy.FATAL) {
             // fatal("Attribute name \u201C" + attributeName
             // + "\u201D is not an NCName.");
             // } else {
             // shouldAddAttributes = false;
             // warn("Attribute name \u201C"
             // + attributeName
             // + "\u201D is not an NCName. Ignoring the attribute.");
             // }
             // }
             // }
         }
     }
 
     private void addAttributeWithoutValue() throws SAXException {
         // [NOCPP[
         if (metaBoundaryPassed && AttributeName.CHARSET == attributeName
                 && ElementName.META == tagName) {
             err("A \u201Ccharset\u201D attribute on a \u201Cmeta\u201D element found after the first 512 bytes.");
         }
         // ]NOCPP]
         if (shouldAddAttributes) {
             // [NOCPP[
             if (html4) {
                 if (attributeName.isBoolean()) {
                     if (html4ModeCompatibleWithXhtml1Schemata) {
                         attributes.addAttribute(attributeName,
                                 attributeName.getLocal(AttributeName.HTML),
                                 xmlnsPolicy);
                     } else {
                         attributes.addAttribute(attributeName, "", xmlnsPolicy);
                     }
                 } else {
                     err("Attribute value omitted for a non-boolean attribute. (HTML4-only error.)");
                     attributes.addAttribute(attributeName, "", xmlnsPolicy);
                 }
             } else {
                 if (AttributeName.SRC == attributeName
                         || AttributeName.HREF == attributeName) {
                     warn("Attribute \u201C"
                             + attributeName.getLocal(AttributeName.HTML)
                             + "\u201D without an explicit value seen. The attribute may be dropped by IE7.");
                 }
                 // ]NOCPP]
                 attributes.addAttribute(attributeName, "", xmlnsPolicy);
                 // [NOCPP[
             }
             // ]NOCPP]
         }
     }
 
     private void addAttributeWithValue() throws SAXException {
         // [NOCPP[
         if (metaBoundaryPassed && ElementName.META == tagName
                 && AttributeName.CHARSET == attributeName) {
             err("A \u201Ccharset\u201D attribute on a \u201Cmeta\u201D element found after the first 512 bytes.");
         }
         // ]NOCPP]
         if (shouldAddAttributes) {
             String value = longStrBufToString();
             // [NOCPP[
             if (!endTag && html4 && html4ModeCompatibleWithXhtml1Schemata
                     && attributeName.isCaseFolded()) {
                 value = newAsciiLowerCaseStringFromString(value);
             }
             // ]NOCPP]
             attributes.addAttribute(attributeName, value, xmlnsPolicy);
         }
     }
 
     // [NOCPP[
 
     private static String newAsciiLowerCaseStringFromString(String str) {
         if (str == null) {
             return null;
         }
         char[] buf = new char[str.length()];
         for (int i = 0; i < str.length(); i++) {
             char c = str.charAt(i);
             if (c >= 'A' && c <= 'Z') {
                 c += 0x20;
             }
             buf[i] = c;
         }
         return new String(buf);
     }
 
     // ]NOCPP]
 
     public void start() throws SAXException {
         confident = false;
         strBuf = new char[64];
         longStrBuf = new char[1024];
         alreadyComplainedAboutNonAscii = false;
         contentModelFlag = ContentModelFlag.PCDATA;
         line = linePrev = 0;
         col = colPrev = 1;
         nextCharOnNewLine = true;
         prev = '\u0000';
         html4 = false;
         alreadyWarnedAboutPrivateUseCharacters = false;
         metaBoundaryPassed = false;
         tokenHandler.startTokenization(this);
         wantsComments = tokenHandler.wantsComments();
         switch (contentModelFlag) {
             case PCDATA:
                 stateSave = Tokenizer.DATA;
                 break;
             case CDATA:
                 stateSave = Tokenizer.CDATA;
                 break;
             case RCDATA:
                 stateSave = Tokenizer.RCDATA;
                 break;
             case PLAINTEXT:
                 stateSave = Tokenizer.PLAINTEXT;
         }
         index = 0;
         forceQuirks = false;
         additional = '\u0000';
         entCol = -1;
         lo = 0;
         hi = (NamedCharacters.NAMES.length - 1);
         candidate = -1;
         strBufMark = 0;
         prevValue = -1;
         value = 0;
         seenDigits = false;
         shouldSuspend = false;
         // [NOCPP[
         if (!newAttributesEachTime) {
             // ]NOCPP]
             attributes = new HtmlAttributes(mappingLangToXmlLang);
             // [NOCPP[
         }
         // ]NOCPP]
     }
 
     public boolean tokenizeBuffer(UTF16Buffer buffer) throws SAXException {
         buf = buffer.getBuffer();
         int state = stateSave;
         int returnState = returnStateSave;
         char c = '\u0000';
         shouldSuspend = false;
 
         int start = buffer.getStart();
         /**
          * The index of the last <code>char</code> read from <code>buf</code>.
          */
         pos = start - 1;
 
         /**
          * The index of the first <code>char</code> in <code>buf</code> that
          * is part of a coalesced run of character tokens or
          * <code>Integer.MAX_VALUE</code> if there is not a current run being
          * coalesced.
          */
         switch (state) {
             case DATA:
             case RCDATA:
             case CDATA:
             case PLAINTEXT:
             case CDATA_SECTION:
             case ESCAPE:
             case ESCAPE_EXCLAMATION:
             case ESCAPE_EXCLAMATION_HYPHEN:
             case ESCAPE_HYPHEN:
             case ESCAPE_HYPHEN_HYPHEN:
                 cstart = start;
                 break;
             default:
                 cstart = Integer.MAX_VALUE;
                 break;
         }
 
         /**
          * The number of <code>char</code>s in <code>buf</code> that have
          * meaning. (The rest of the array is garbage and should not be
          * examined.)
          */
         end = buffer.getEnd();
         boolean reconsume = false;
         stateLoop(state, c, reconsume, returnState);
         detachStrBuf();
         detachLongStrBuf();
         if (pos == end) {
             // exiting due to end of buffer
             buffer.setStart(pos);
         } else {
             buffer.setStart(pos + 1);
         }
         if (prev == '\r') {
             prev = ' ';
             return true;
         } else {
             return false;
         }
     }
 
     // WARNING When editing this, makes sure the bytecode length shown by javap
     // stays under 8000 bytes!
     private void stateLoop(int state, char c, boolean reconsume, int returnState)
             throws SAXException {
         stateloop: for (;;) {
             switch (state) {
                 case DATA:
                     dataloop: for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '&':
                                 /*
                                  * U+0026 AMPERSAND (&) When the content model
                                  * flag is set to one of the PCDATA or RCDATA
                                  * states and the escape flag is false: switch
                                  * to the character reference data state.
                                  * Otherwise: treat it as per the "anything
                                  * else" entry below.
                                  */
                                 flushChars();
                                 clearStrBufAndAppendCurrentC(c);
                                 additional = '\u0000';
                                 returnState = state;
                                 state = Tokenizer.CONSUME_CHARACTER_REFERENCE;
                                 continue stateloop;
                             case '<':
                                 /*
                                  * U+003C LESS-THAN SIGN (<) When the content
                                  * model flag is set to the PCDATA state: switch
                                  * to the tag open state. When the content model
                                  * flag is set to either the RCDATA state or the
                                  * CDATA state and the escape flag is false:
                                  * switch to the tag open state. Otherwise:
                                  * treat it as per the "anything else" entry
                                  * below.
                                  */
                                 flushChars();
 
                                 state = Tokenizer.TAG_OPEN;
                                 break dataloop; // FALL THROUGH continue
                             // stateloop;
                             default:
                                 /*
                                  * Anything else Emit the input character as a
                                  * character token.
                                  */
                                 /*
                                  * Stay in the data state.
                                  */
                                 continue;
                         }
                     }
                     // WARNING FALLTHRU CASE TRANSITION: DON'T REORDER
                 case TAG_OPEN:
                     tagopenloop: for (;;) {
                         /*
                          * The behavior of this state depends on the content
                          * model flag.
                          */
                         /*
                          * If the content model flag is set to the PCDATA state
                          * Consume the next input character:
                          */
                         c = read();
                         if (c >= 'A' && c <= 'Z') {
                             /*
                              * U+0041 LATIN CAPITAL LETTER A through to U+005A
                              * LATIN CAPITAL LETTER Z Create a new start tag
                              * token,
                              */
                             endTag = false;
                             /*
                              * set its tag name to the lowercase version of the
                              * input character (add 0x0020 to the character's
                              * code point),
                              */
                             clearStrBufAndAppendForceWrite((char) (c + 0x20));
                             /* then switch to the tag name state. */
                             state = Tokenizer.TAG_NAME;
                             /*
                              * (Don't emit the token yet; further details will
                              * be filled in before it is emitted.)
                              */
                             break tagopenloop;
                             // continue stateloop;
                         } else if (c >= 'a' && c <= 'z') {
                             /*
                              * U+0061 LATIN SMALL LETTER A through to U+007A
                              * LATIN SMALL LETTER Z Create a new start tag
                              * token,
                              */
                             endTag = false;
                             /*
                              * set its tag name to the input character,
                              */
                             clearStrBufAndAppendCurrentC(c);
                             /* then switch to the tag name state. */
                             state = Tokenizer.TAG_NAME;
                             /*
                              * (Don't emit the token yet; further details will
                              * be filled in before it is emitted.)
                              */
                             break tagopenloop;
                             // continue stateloop;
                         }
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '!':
                                 /*
                                  * U+0021 EXCLAMATION MARK (!) Switch to the
                                  * markup declaration open state.
                                  */
                                 state = Tokenizer.MARKUP_DECLARATION_OPEN;
                                 continue stateloop;
                             case '/':
                                 /*
                                  * U+002F SOLIDUS (/) Switch to the close tag
                                  * open state.
                                  */
                                 state = Tokenizer.CLOSE_TAG_OPEN_PCDATA;
                                 continue stateloop;
                             case '?':
                                 /*
                                  * U+003F QUESTION MARK (?) Parse error.
                                  */
                                 err("Saw \u201C<?\u201D. Probable cause: Attempt to use an XML processing instruction in HTML. (XML processing instructions are not supported in HTML.)");
                                 /*
                                  * Switch to the bogus comment state.
                                  */
                                 clearLongStrBufAndAppendToComment('?');
                                 state = Tokenizer.BOGUS_COMMENT;
                                 continue stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Parse error.
                                  */
                                 err("Saw \u201C<>\u201D. Probable causes: Unescaped \u201C<\u201D (escape as \u201C&lt;\u201D) or mistyped start tag.");
                                 /*
                                  * Emit a U+003C LESS-THAN SIGN character token
                                  * and a U+003E GREATER-THAN SIGN character
                                  * token.
                                  */
                                 tokenHandler.characters(Tokenizer.LT_GT, 0, 2);
                                 /* Switch to the data state. */
                                 cstart = pos + 1;
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             default:
                                 /*
                                  * Anything else Parse error.
                                  */
                                 err("Bad character \u201C"
                                         + c
                                         + "\u201D after \u201C<\u201D. Probable cause: Unescaped \u201C<\u201D. Try escaping it as \u201C&lt;\u201D.");
                                 /*
                                  * Emit a U+003C LESS-THAN SIGN character token
                                  */
                                 tokenHandler.characters(Tokenizer.LT_GT, 0, 1);
                                 /*
                                  * and reconsume the current input character in
                                  * the data state.
                                  */
                                 cstart = pos;
                                 state = Tokenizer.DATA;
                                 reconsume = true;
                                 continue stateloop;
                         }
                     }
                     // FALL THROUGH DON'T REORDER
                 case TAG_NAME:
                     tagnameloop: for (;;) {
                         c = read();
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ' ':
                             case '\t':
                             case '\n':
                             case '\u000C':
                                 /*
                                  * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                  * (LF) U+000C FORM FEED (FF) U+0020 SPACE
                                  * Switch to the before attribute name state.
                                  */
                                 tagName = strBufToElementNameString();
                                 state = Tokenizer.BEFORE_ATTRIBUTE_NAME;
                                 break tagnameloop;
                             // continue stateloop;
                             case '/':
                                 /*
                                  * U+002F SOLIDUS (/) Switch to the self-closing
                                  * start tag state.
                                  */
                                 tagName = strBufToElementNameString();
                                 state = Tokenizer.SELF_CLOSING_START_TAG;
                                 continue stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Emit the current
                                  * tag token.
                                  */
                                 tagName = strBufToElementNameString();
                                 state = emitCurrentTagToken(false);
                                 if (shouldSuspend) {
                                     break stateloop;
                                 }
                                 /*
                                  * Switch to the data state.
                                  */
                                 continue stateloop;
                             default:
                                 if (c >= 'A' && c <= 'Z') {
                                     /*
                                      * U+0041 LATIN CAPITAL LETTER A through to
                                      * U+005A LATIN CAPITAL LETTER Z Append the
                                      * lowercase version of the current input
                                      * character (add 0x0020 to the character's
                                      * code point) to the current tag token's
                                      * tag name.
                                      */
                                     appendStrBufForceWrite((char) (c + 0x20));
                                 } else {
                                     /*
                                      * Anything else Append the current input
                                      * character to the current tag token's tag
                                      * name.
                                      */
                                     appendStrBuf(c);
                                 }
                                 /*
                                  * Stay in the tag name state.
                                  */
                                 continue;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case BEFORE_ATTRIBUTE_NAME:
                     beforeattributenameloop: for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ' ':
                             case '\t':
                             case '\n':
                             case '\u000C':
                                 /*
                                  * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                  * (LF) U+000C FORM FEED (FF) U+0020 SPACE Stay
                                  * in the before attribute name state.
                                  */
                                 continue;
                             case '/':
                                 /*
                                  * U+002F SOLIDUS (/) Switch to the self-closing
                                  * start tag state.
                                  */
                                 state = Tokenizer.SELF_CLOSING_START_TAG;
                                 continue stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Emit the current
                                  * tag token.
                                  */
                                 state = emitCurrentTagToken(false);
                                 if (shouldSuspend) {
                                     break stateloop;
                                 }
                                 /*
                                  * Switch to the data state.
                                  */
                                 continue stateloop;
                             case '\"':
                             case '\'':
                             case '=':
                                 /*
                                  * U+0022 QUOTATION MARK (") U+0027 APOSTROPHE
                                  * (') U+003D EQUALS SIGN (=) Parse error.
                                  */
                                 if (c == '=') {
                                     err("Saw \u201C=\u201D when expecting an attribute name. Probable cause: Attribute name missing.");
                                 } else {
                                     errQuoteBeforeAttributeName(c);
                                 }
                                 /*
                                  * Treat it as per the "anything else" entry
                                  * below.
                                  */
                             default:
                                 /*
                                  * Anything else Start a new attribute in the
                                  * current tag token.
                                  */
                                 if (c >= 'A' && c <= 'Z') {
                                     /*
                                      * U+0041 LATIN CAPITAL LETTER A through to
                                      * U+005A LATIN CAPITAL LETTER Z Set that
                                      * attribute's name to the lowercase version
                                      * of the current input character (add
                                      * 0x0020 to the character's code point)
                                      */
                                     clearStrBufAndAppendForceWrite((char) (c + 0x20));
                                 } else {
                                     /*
                                      * Set that attribute's name to the current
                                      * input character,
                                      */
                                     clearStrBufAndAppendCurrentC(c);
                                 }
                                 /*
                                  * and its value to the empty string.
                                  */
                                 // Will do later.
                                 /*
                                  * Switch to the attribute name state.
                                  */
                                 state = Tokenizer.ATTRIBUTE_NAME;
                                 break beforeattributenameloop;
                             // continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case ATTRIBUTE_NAME:
                     attributenameloop: for (;;) {
                         c = read();
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ' ':
                             case '\t':
                             case '\n':
                             case '\u000C':
                                 /*
                                  * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                  * (LF) U+000C FORM FEED (FF) U+0020 SPACE
                                  * Switch to the after attribute name state.
                                  */
                                 attributeNameComplete();
                                 state = Tokenizer.AFTER_ATTRIBUTE_NAME;
                                 continue stateloop;
                             case '/':
                                 /*
                                  * U+002F SOLIDUS (/) Switch to the self-closing
                                  * start tag state.
                                  */
                                 attributeNameComplete();
                                 addAttributeWithoutValue();
                                 state = Tokenizer.SELF_CLOSING_START_TAG;
                                 continue stateloop;
                             case '=':
                                 /*
                                  * U+003D EQUALS SIGN (=) Switch to the before
                                  * attribute value state.
                                  */
                                 attributeNameComplete();
                                 state = Tokenizer.BEFORE_ATTRIBUTE_VALUE;
                                 break attributenameloop;
                             // continue stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Emit the current
                                  * tag token.
                                  */
                                 attributeNameComplete();
                                 addAttributeWithoutValue();
                                 state = emitCurrentTagToken(false);
                                 if (shouldSuspend) {
                                     break stateloop;
                                 }
                                 /*
                                  * Switch to the data state.
                                  */
                                 continue stateloop;
                             case '\"':
                             case '\'':
                                 /*
                                  * U+0022 QUOTATION MARK (") U+0027 APOSTROPHE
                                  * (') Parse error.
                                  */
                                 errQuoteInAttributeName(c);
                                 /*
                                  * Treat it as per the "anything else" entry
                                  * below.
                                  */
                             default:
                                 if (c >= 'A' && c <= 'Z') {
                                     /*
                                      * U+0041 LATIN CAPITAL LETTER A through to
                                      * U+005A LATIN CAPITAL LETTER Z Append the
                                      * lowercase version of the current input
                                      * character (add 0x0020 to the character's
                                      * code point) to the current attribute's
                                      * name.
                                      */
                                     appendStrBufForceWrite((char) (c + 0x20));
                                 } else {
                                     /*
                                      * Anything else Append the current input
                                      * character to the current attribute's
                                      * name.
                                      */
                                     appendStrBuf(c);
                                 }
                                 /*
                                  * Stay in the attribute name state.
                                  */
                                 continue;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case BEFORE_ATTRIBUTE_VALUE:
                     beforeattributevalueloop: for (;;) {
                         c = read();
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ' ':
                             case '\t':
                             case '\n':
                             case '\u000C':
                                 /*
                                  * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                  * (LF) U+000C FORM FEED (FF) U+0020 SPACE Stay
                                  * in the before attribute value state.
                                  */
                                 continue;
                             case '"':
                                 /*
                                  * U+0022 QUOTATION MARK (") Switch to the
                                  * attribute value (double-quoted) state.
                                  */
                                 clearLongStrBufForNextState();
                                 state = Tokenizer.ATTRIBUTE_VALUE_DOUBLE_QUOTED;
                                 break beforeattributevalueloop;
                             // continue stateloop;
                             case '&':
                                 /*
                                  * U+0026 AMPERSAND (&) Switch to the attribute
                                  * value (unquoted) state and reconsume this
                                  * input character.
                                  */
                                 clearLongStrBuf();
                                 state = Tokenizer.ATTRIBUTE_VALUE_UNQUOTED;
                                 reconsume = true;
                                 continue stateloop;
                             case '\'':
                                 /*
                                  * U+0027 APOSTROPHE (') Switch to the attribute
                                  * value (single-quoted) state.
                                  */
                                 clearLongStrBufForNextState();
                                 state = Tokenizer.ATTRIBUTE_VALUE_SINGLE_QUOTED;
                                 continue stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Parse error.
                                  */
                                 err("Attribute value missing.");
                                 /*
                                  * Emit the current tag token.
                                  */
                                 addAttributeWithoutValue();
                                 state = emitCurrentTagToken(false);
                                 if (shouldSuspend) {
                                     break stateloop;
                                 }
                                 /*
                                  * Switch to the data state.
                                  */
                                 continue stateloop;
                             case '=':
                                 /*
                                  * U+003D EQUALS SIGN (=) Parse error.
                                  */
                                 err("\u201C=\u201D in an unquoted attribute value. Probable cause: Stray duplicate equals sign.");
                                 /*
                                  * Treat it as per the "anything else" entry
                                  * below.
                                  */
                             default:
                                 if (html4
                                         && !((c >= 'a' && c <= 'z')
                                                 || (c >= 'A' && c <= 'Z')
                                                 || (c >= '0' && c <= '9')
                                                 || c == '.' || c == '-'
                                                 || c == '_' || c == ':')) {
                                     err("Non-name character in an unquoted attribute value. (This is an HTML4-only error.)");
                                 }
                                 /*
                                  * Anything else Append the current input
                                  * character to the current attribute's value.
                                  */
                                 clearLongStrBufAndAppendCurrentC();
                                 /*
                                  * Switch to the attribute value (unquoted)
                                  * state.
                                  */
 
                                 state = Tokenizer.ATTRIBUTE_VALUE_UNQUOTED;
                                 continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case ATTRIBUTE_VALUE_DOUBLE_QUOTED:
                     attributevaluedoublequotedloop: for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '"':
                                 /*
                                  * U+0022 QUOTATION MARK (") Switch to the after
                                  * attribute value (quoted) state.
                                  */
                                 addAttributeWithValue();
 
                                 state = Tokenizer.AFTER_ATTRIBUTE_VALUE_QUOTED;
                                 break attributevaluedoublequotedloop;
                             // continue stateloop;
                             case '&':
                                 /*
                                  * U+0026 AMPERSAND (&) Switch to the character
                                  * reference in attribute value state, with the
                                  * additional allowed character being U+0022
                                  * QUOTATION MARK (").
                                  */
                                 detachLongStrBuf();
                                 clearStrBufAndAppendCurrentC(c);
                                 additional = '\"';
                                 returnState = state;
                                 state = Tokenizer.CONSUME_CHARACTER_REFERENCE;
                                 continue stateloop;
                             default:
                                 /*
                                  * Anything else Append the current input
                                  * character to the current attribute's value.
                                  */
                                 appendLongStrBuf(c);
                                 /*
                                  * Stay in the attribute value (double-quoted)
                                  * state.
                                  */
                                 continue;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case AFTER_ATTRIBUTE_VALUE_QUOTED:
                     afterattributevaluequotedloop: for (;;) {
                         c = read();
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ' ':
                             case '\t':
                             case '\n':
                             case '\u000C':
                                 /*
                                  * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                  * (LF) U+000C FORM FEED (FF) U+0020 SPACE
                                  * Switch to the before attribute name state.
                                  */
                                 state = Tokenizer.BEFORE_ATTRIBUTE_NAME;
                                 continue stateloop;
                             case '/':
                                 /*
                                  * U+002F SOLIDUS (/) Switch to the self-closing
                                  * start tag state.
                                  */
                                 state = Tokenizer.SELF_CLOSING_START_TAG;
                                 break afterattributevaluequotedloop;
                             // continue stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Emit the current
                                  * tag token.
                                  */
                                 state = emitCurrentTagToken(false);
                                 if (shouldSuspend) {
                                     break stateloop;
                                 }
                                 /*
                                  * Switch to the data state.
                                  */
                                 continue stateloop;
                             default:
                                 /*
                                  * Anything else Parse error.
                                  */
                                 err("No space between attributes.");
                                 /*
                                  * Reconsume the character in the before
                                  * attribute name state.
                                  */
                                 state = Tokenizer.BEFORE_ATTRIBUTE_NAME;
                                 reconsume = true;
                                 continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case SELF_CLOSING_START_TAG:
                     c = read();
                     /*
                      * Consume the next input character:
                      */
                     switch (c) {
                         case '\u0000':
                             break stateloop;
                         case '>':
                             /*
                              * U+003E GREATER-THAN SIGN (>) Set the self-closing
                              * flag of the current tag token. Emit the current
                              * tag token.
                              */
                             if (html4) {
                                 err("The \u201C/>\u201D syntax on void elements is not allowed.  (This is an HTML4-only error.)");
                             }
                             state = emitCurrentTagToken(true);
                             if (shouldSuspend) {
                                 break stateloop;
                             }
                             /*
                              * Switch to the data state.
                              */
                             continue stateloop;
                         default:
                             /* Anything else Parse error. */
                             err("A slash was not immediate followed by \u201C>\u201D.");
                             /*
                              * Reconsume the character in the before attribute
                              * name state.
                              */
                             state = Tokenizer.BEFORE_ATTRIBUTE_NAME;
                             reconsume = true;
                             continue stateloop;
                     }
                     // XXX reorder point
                 case ATTRIBUTE_VALUE_UNQUOTED:
                     for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ' ':
                             case '\t':
                             case '\n':
 
                             case '\u000C':
                                 /*
                                  * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                  * (LF) U+000C FORM FEED (FF) U+0020 SPACE
                                  * Switch to the before attribute name state.
                                  */
                                 addAttributeWithValue();
 
                                 state = Tokenizer.BEFORE_ATTRIBUTE_NAME;
                                 continue stateloop;
                             case '&':
                                 /*
                                  * U+0026 AMPERSAND (&) Switch to the character
                                  * reference in attribute value state, with no +
                                  * additional allowed character.
                                  */
                                 detachLongStrBuf();
                                 clearStrBufAndAppendCurrentC(c);
                                 additional = '\u0000';
                                 returnState = state;
                                 state = Tokenizer.CONSUME_CHARACTER_REFERENCE;
                                 continue stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Emit the current
                                  * tag token.
                                  */
                                 addAttributeWithValue();
                                 state = emitCurrentTagToken(false);
                                 if (shouldSuspend) {
                                     break stateloop;
                                 }
                                 /*
                                  * Switch to the data state.
                                  */
                                 continue stateloop;
                             case '<':
                             case '\"':
                             case '\'':
                             case '=':
                                 if (c == '<') {
                                     warn("\u201C<\u201D in an unquoted attribute value. This does not end the tag. Probable cause: Missing \u201C>\u201D immediately before.");
                                 } else {
                                     /*
                                      * U+0022 QUOTATION MARK (") U+0027
                                      * APOSTROPHE (') U+003D EQUALS SIGN (=)
                                      * Parse error.
                                      */
                                     err("\u201C"
                                             + c
                                             + "\u201D in an unquoted attribute value. Probable causes: Attributes running together or a URL query string in an unquoted attribute value.");
                                     /*
                                      * Treat it as per the "anything else" entry
                                      * below.
                                      */
                                 }
                                 // fall through
                             default:
                                 if (html4
                                         && !((c >= 'a' && c <= 'z')
                                                 || (c >= 'A' && c <= 'Z')
                                                 || (c >= '0' && c <= '9')
                                                 || c == '.' || c == '-'
                                                 || c == '_' || c == ':')) {
                                     err("Non-name character in an unquoted attribute value. (This is an HTML4-only error.)");
                                 }
                                 /*
                                  * Anything else Append the current input
                                  * character to the current attribute's value.
                                  */
                                 appendLongStrBuf(c);
                                 /*
                                  * Stay in the attribute value (unquoted) state.
                                  */
                                 continue;
                         }
                     }
                     // XXX reorder point
                 case AFTER_ATTRIBUTE_NAME:
                     for (;;) {
                         c = read();
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ' ':
                             case '\t':
                             case '\n':
                             case '\u000C':
                                 /*
                                  * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                  * (LF) U+000C FORM FEED (FF) U+0020 SPACE Stay
                                  * in the after attribute name state.
                                  */
                                 continue;
                             case '/':
                                 /*
                                  * U+002F SOLIDUS (/) Switch to the self-closing
                                  * start tag state.
                                  */
                                 addAttributeWithoutValue();
                                 state = Tokenizer.SELF_CLOSING_START_TAG;
                                 continue stateloop;
                             case '=':
                                 /*
                                  * U+003D EQUALS SIGN (=) Switch to the before
                                  * attribute value state.
                                  */
                                 state = Tokenizer.BEFORE_ATTRIBUTE_VALUE;
                                 continue stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Emit the current
                                  * tag token.
                                  */
                                 addAttributeWithoutValue();
                                 state = emitCurrentTagToken(false);
                                 if (shouldSuspend) {
                                     break stateloop;
                                 }
                                 /*
                                  * Switch to the data state.
                                  */
                                 continue stateloop;
                             case '\"':
                             case '\'':
                                 errQuoteInAttributeName(c);
                                 /*
                                  * Treat it as per the "anything else" entry
                                  * below.
                                  */
                             default:
                                 addAttributeWithoutValue();
                                 /*
                                  * Anything else Start a new attribute in the
                                  * current tag token.
                                  */
                                 if (c >= 'A' && c <= 'Z') {
                                     /*
                                      * U+0041 LATIN CAPITAL LETTER A through to
                                      * U+005A LATIN CAPITAL LETTER Z Set that
                                      * attribute's name to the lowercase version
                                      * of the current input character (add
                                      * 0x0020 to the character's code point)
                                      */
                                     clearStrBufAndAppendForceWrite((char) (c + 0x20));
                                 } else {
                                     /*
                                      * Set that attribute's name to the current
                                      * input character,
                                      */
                                     clearStrBufAndAppendCurrentC(c);
                                 }
                                 /*
                                  * and its value to the empty string.
                                  */
                                 // Will do later.
                                 /*
                                  * Switch to the attribute name state.
                                  */
                                 state = Tokenizer.ATTRIBUTE_NAME;
                                 continue stateloop;
                         }
                     }
                     // XXX reorder point
                 case BOGUS_COMMENT:
                     boguscommentloop: for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         /*
                          * (This can only happen if the content model flag is
                          * set to the PCDATA state.)
                          * 
                          * Consume every character up to and including the first
                          * U+003E GREATER-THAN SIGN character (>) or the end of
                          * the file (EOF), whichever comes first. Emit a comment
                          * token whose data is the concatenation of all the
                          * characters starting from and including the character
                          * that caused the state machine to switch into the
                          * bogus comment state, up to and including the
                          * character immediately before the last consumed
                          * character (i.e. up to the character just before the
                          * U+003E or EOF character). (If the comment was started
                          * by the end of the file (EOF), the token is empty.)
                          * 
                          * Switch to the data state.
                          * 
                          * If the end of the file was reached, reconsume the EOF
                          * character.
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '>':
                                 emitComment(0);
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             case '-':
                                 appendLongStrBuf(c);
                                 state = Tokenizer.BOGUS_COMMENT_HYPHEN;
                                 break boguscommentloop;
                             default:
                                 appendLongStrBuf(c);
                                 continue;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case BOGUS_COMMENT_HYPHEN:
                     boguscommenthyphenloop: for (;;) {
                         c = read();
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '>':
                                 maybeAppendSpaceToBogusComment();
                                 emitComment(0);
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             case '-':
                                 appendSecondHyphenToBogusComment();
                                 continue boguscommenthyphenloop;
                             default:
                                 appendLongStrBuf(c);
                                 continue stateloop;
                         }
                     }
                     // XXX reorder point
                 case MARKUP_DECLARATION_OPEN:
                     markupdeclarationopenloop: for (;;) {
                         c = read();
                         /*
                          * (This can only happen if the content model flag is
                          * set to the PCDATA state.)
                          * 
                          * If the next two characters are both U+002D
                          * HYPHEN-MINUS (-) characters, consume those two
                          * characters, create a comment token whose data is the
                          * empty string, and switch to the comment start state.
                          * 
                          * Otherwise, if the next seven characters are an ASCII
                          * case-insensitive match for the word "DOCTYPE", then
                          * consume those characters and switch to the DOCTYPE
                          * state.
                          * 
                          * Otherwise, if the insertion mode is "in foreign
                          * content" and the current node is not an element in
                          * the HTML namespace and the next seven characters are
                          * an ASCII case-sensitive match for the string
                          * "[CDATA[" (the five uppercase letters "CDATA" with a
                          * U+005B LEFT SQUARE BRACKET character before and
                          * after), then consume those characters and switch to
                          * the CDATA section state (which is unrelated to the
                          * content model flag's CDATA state).
                          * 
                          * Otherwise, is is a parse error. Switch to the bogus
                          * comment state. The next character that is consumed,
                          * if any, is the first character that will be in the
                          * comment.
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '-':
                                 clearLongStrBufAndAppendToComment(c);
                                 state = Tokenizer.MARKUP_DECLARATION_HYPHEN;
                                 break markupdeclarationopenloop;
                             // continue stateloop;
                             case 'd':
                             case 'D':
                                 clearLongStrBufAndAppendToComment(c);
                                 index = 0;
                                 state = Tokenizer.MARKUP_DECLARATION_OCTYPE;
                                 continue stateloop;
                             case '[':
                                 if (tokenHandler.inForeign()) {
                                     clearLongStrBufAndAppendToComment(c);
                                     index = 0;
                                     state = Tokenizer.CDATA_START;
                                     continue stateloop;
                                 } else {
                                     // fall through
                                 }
                             default:
                                 err("Bogus comment.");
                                 clearLongStrBuf();
                                 state = Tokenizer.BOGUS_COMMENT;
                                 reconsume = true;
                                 continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case MARKUP_DECLARATION_HYPHEN:
                     markupdeclarationhyphenloop: for (;;) {
                         c = read();
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '-':
                                 clearLongStrBufForNextState();
                                 state = Tokenizer.COMMENT_START;
                                 break markupdeclarationhyphenloop;
                             // continue stateloop;
                             default:
                                 err("Bogus comment.");
                                 state = Tokenizer.BOGUS_COMMENT;
                                 reconsume = true;
                                 continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case COMMENT_START:
                     commentstartloop: for (;;) {
                         c = read();
                         /*
                          * Comment start state
                          * 
                          * 
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '-':
                                 /*
                                  * U+002D HYPHEN-MINUS (-) Switch to the comment
                                  * start dash state.
                                  */
                                 appendLongStrBuf(c);
                                 state = Tokenizer.COMMENT_START_DASH;
                                 continue stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Parse error.
                                  */
                                 err("Premature end of comment. Use \u201C-->\u201D to end a comment properly.");
                                 /* Emit the comment token. */
                                 emitComment(0);
                                 /*
                                  * Switch to the data state.
                                  */
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             default:
                                 /*
                                  * Anything else Append the input character to
                                  * the comment token's data.
                                  */
                                 appendLongStrBuf(c);
                                 /*
                                  * Switch to the comment state.
                                  */
                                 state = Tokenizer.COMMENT;
                                 break commentstartloop;
                             // continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case COMMENT:
                     commentloop: for (;;) {
                         c = read();
                         /*
                          * Comment state Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '-':
                                 /*
                                  * U+002D HYPHEN-MINUS (-) Switch to the comment
                                  * end dash state
                                  */
                                 appendLongStrBuf(c);
                                 state = Tokenizer.COMMENT_END_DASH;
                                 break commentloop;
                             // continue stateloop;
                             default:
                                 /*
                                  * Anything else Append the input character to
                                  * the comment token's data.
                                  */
                                 appendLongStrBuf(c);
                                 /*
                                  * Stay in the comment state.
                                  */
                                 continue;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case COMMENT_END_DASH:
                     commentenddashloop: for (;;) {
                         c = read();
                         /*
                          * Comment end dash state Consume the next input
                          * character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '-':
                                 /*
                                  * U+002D HYPHEN-MINUS (-) Switch to the comment
                                  * end state
                                  */
                                 appendLongStrBuf(c);
                                 state = Tokenizer.COMMENT_END;
                                 break commentenddashloop;
                             // continue stateloop;
                             default:
                                 /*
                                  * Anything else Append a U+002D HYPHEN-MINUS
                                  * (-) character and the input character to the
                                  * comment token's data.
                                  */
                                 appendLongStrBuf(c);
                                 /*
                                  * Switch to the comment state.
                                  */
                                 state = Tokenizer.COMMENT;
                                 continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case COMMENT_END:
                     for (;;) {
                         c = read();
                         /*
                          * Comment end dash state Consume the next input
                          * character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Emit the comment
                                  * token.
                                  */
                                 emitComment(2);
                                 /*
                                  * Switch to the data state.
                                  */
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             case '-':
                                 /* U+002D HYPHEN-MINUS (-) Parse error. */
                                 err("Consecutive hyphens did not terminate a comment. \u201C--\u201D is not permitted inside a comment, but e.g. \u201C- -\u201D is.");
                                 /*
                                  * Append a U+002D HYPHEN-MINUS (-) character to
                                  * the comment token's data.
                                  */
                                 adjustDoubleHyphenAndAppendToLongStrBuf(c);
                                 /*
                                  * Stay in the comment end state.
                                  */
                                 continue;
                             default:
                                 /*
                                  * Anything else Parse error.
                                  */
                                 err("Consecutive hyphens did not terminate a comment. \u201C--\u201D is not permitted inside a comment, but e.g. \u201C- -\u201D is.");
                                 /*
                                  * Append two U+002D HYPHEN-MINUS (-) characters
                                  * and the input character to the comment
                                  * token's data.
                                  */
                                 adjustDoubleHyphenAndAppendToLongStrBuf(c);
                                 /*
                                  * Switch to the comment state.
                                  */
                                 state = Tokenizer.COMMENT;
                                 continue stateloop;
                         }
                     }
                     // XXX reorder point
                 case COMMENT_START_DASH:
                     c = read();
                     /*
                      * Comment start dash state
                      * 
                      * Consume the next input character:
                      */
                     switch (c) {
                         case '\u0000':
                             break stateloop;
                         case '-':
                             /*
                              * U+002D HYPHEN-MINUS (-) Switch to the comment end
                              * state
                              */
                             appendLongStrBuf(c);
                             state = Tokenizer.COMMENT_END;
                             continue stateloop;
                         case '>':
                             /*
                              * U+003E GREATER-THAN SIGN (>) Parse error.
                              */
                             err("Premature end of comment. Use \u201C-->\u201D to end a comment properly.");
                             /* Emit the comment token. */
                             emitComment(1);
                             /*
                              * Switch to the data state.
                              */
                             state = Tokenizer.DATA;
                             continue stateloop;
                         default:
                             /*
                              * Anything else Append a U+002D HYPHEN-MINUS (-)
                              * character and the input character to the comment
                              * token's data.
                              */
                             appendLongStrBuf(c);
                             /*
                              * Switch to the comment state.
                              */
                             state = Tokenizer.COMMENT;
                             continue stateloop;
                     }
                     // XXX reorder point
                 case MARKUP_DECLARATION_OCTYPE:
                     markupdeclarationdoctypeloop: for (;;) {
                         c = read();
                         if (c == '\u0000') {
                             break stateloop;
                         }
                         if (index < 6) { // OCTYPE.length
                             char folded = c;
                             if (c >= 'A' && c <= 'Z') {
                                 folded += 0x20;
                             }
                             if (folded == Tokenizer.OCTYPE[index]) {
                                 appendLongStrBuf(c);
                             } else {
                                 err("Bogus comment.");
                                 state = Tokenizer.BOGUS_COMMENT;
                                 reconsume = true;
                                 continue stateloop;
                             }
                             index++;
                             continue;
                         } else {
                             state = Tokenizer.DOCTYPE;
                             reconsume = true;
                             break markupdeclarationdoctypeloop;
                             // continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case DOCTYPE:
                     doctypeloop: for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         systemIdentifier = null;
                         publicIdentifier = null;
                         doctypeName = null;
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ' ':
                             case '\t':
                             case '\n':
 
                             case '\u000C':
                                 /*
                                  * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                  * (LF) U+000C FORM FEED (FF) U+0020 SPACE
                                  * Switch to the before DOCTYPE name state.
                                  */
                                 state = Tokenizer.BEFORE_DOCTYPE_NAME;
                                 break doctypeloop;
                             // continue stateloop;
                             default:
                                 /*
                                  * Anything else Parse error.
                                  */
                                 err("Missing space before doctype name.");
                                 /*
                                  * Reconsume the current character in the before
                                  * DOCTYPE name state.
                                  */
                                 state = Tokenizer.BEFORE_DOCTYPE_NAME;
                                 reconsume = true;
                                 break doctypeloop;
                             // continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case BEFORE_DOCTYPE_NAME:
                     beforedoctypenameloop: for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ' ':
                             case '\t':
                             case '\n':
 
                             case '\u000C':
                                 /*
                                  * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                  * (LF) U+000C FORM FEED (FF) U+0020 SPACE Stay
                                  * in the before DOCTYPE name state.
                                  */
                                 continue;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Parse error.
                                  */
                                 err("Nameless doctype.");
                                 /*
                                  * Create a new DOCTYPE token. Set its
                                  * force-quirks flag to on. Emit the token.
                                  */
                                 tokenHandler.doctype("", null, null, true);
                                 /*
                                  * Switch to the data state.
                                  */
                                 cstart = pos + 1;
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             default:
                                 /* Anything else Create a new DOCTYPE token. */
                                 /*
                                  * Set the token's name name to the current
                                  * input character.
                                  */
                                 clearStrBufAndAppendCurrentC(c);
                                 /*
                                  * Switch to the DOCTYPE name state.
                                  */
                                 state = Tokenizer.DOCTYPE_NAME;
                                 break beforedoctypenameloop;
                             // continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case DOCTYPE_NAME:
                     doctypenameloop: for (;;) {
                         c = read();
                         /*
                          * First, consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ' ':
                             case '\t':
                             case '\n':
 
                             case '\u000C':
                                 /*
                                  * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                  * (LF) U+000C FORM FEED (FF) U+0020 SPACE
                                  * Switch to the after DOCTYPE name state.
                                  */
                                 doctypeName = strBufToString();
                                 state = Tokenizer.AFTER_DOCTYPE_NAME;
                                 break doctypenameloop;
                             // continue stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Emit the current
                                  * DOCTYPE token.
                                  */
                                 tokenHandler.doctype(strBufToString(), null,
                                         null, false);
                                 /*
                                  * Switch to the data state.
                                  */
                                 cstart = pos + 1;
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             default:
                                 /*
                                  * Anything else Append the current input
                                  * character to the current DOCTYPE token's
                                  * name.
                                  */
                                 appendStrBuf(c);
                                 /*
                                  * Stay in the DOCTYPE name state.
                                  */
                                 continue;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case AFTER_DOCTYPE_NAME:
                     afterdoctypenameloop: for (;;) {
                         c = read();
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ' ':
                             case '\t':
                             case '\n':
 
                             case '\u000C':
                                 /*
                                  * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                  * (LF) U+000C FORM FEED (FF) U+0020 SPACE Stay
                                  * in the after DOCTYPE name state.
                                  */
                                 continue;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Emit the current
                                  * DOCTYPE token.
                                  */
                                 tokenHandler.doctype(doctypeName, null, null,
                                         false);
                                 /*
                                  * Switch to the data state.
                                  */
                                 cstart = pos + 1;
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             case 'p':
                             case 'P':
                                 index = 0;
                                 state = Tokenizer.DOCTYPE_UBLIC;
                                 break afterdoctypenameloop;
                             // continue stateloop;
                             case 's':
                             case 'S':
                                 index = 0;
                                 state = Tokenizer.DOCTYPE_YSTEM;
                                 continue stateloop;
                             default:
                                 /*
                                  * Otherwise, this is the parse error.
                                  */
                                 bogusDoctype();
 
                                 /*
                                  * Set the DOCTYPE token's force-quirks flag to
                                  * on.
                                  */
                                 // done by bogusDoctype();
                                 /*
                                  * Switch to the bogus DOCTYPE state.
                                  */
                                 state = Tokenizer.BOGUS_DOCTYPE;
                                 continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case DOCTYPE_UBLIC:
                     doctypeublicloop: for (;;) {
                         c = read();
                         if (c == '\u0000') {
                             break stateloop;
                         }
                         /*
                          * If the next six characters are an ASCII
                          * case-insensitive match for the word "PUBLIC", then
                          * consume those characters and switch to the before
                          * DOCTYPE public identifier state.
                          */
                         if (index < 5) { // UBLIC.length
                             char folded = c;
                             if (c >= 'A' && c <= 'Z') {
                                 folded += 0x20;
                             }
                             if (folded != Tokenizer.UBLIC[index]) {
                                 bogusDoctype();
                                 // forceQuirks = true;
                                 state = Tokenizer.BOGUS_DOCTYPE;
                                 reconsume = true;
                                 continue stateloop;
                             }
                             index++;
                             continue;
                         } else {
                             state = Tokenizer.BEFORE_DOCTYPE_PUBLIC_IDENTIFIER;
                             reconsume = true;
                             break doctypeublicloop;
                             // continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case BEFORE_DOCTYPE_PUBLIC_IDENTIFIER:
                     beforedoctypepublicidentifierloop: for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ' ':
                             case '\t':
                             case '\n':
 
                             case '\u000C':
                                 /*
                                  * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                  * (LF) U+000C FORM FEED (FF) U+0020 SPACE Stay
                                  * in the before DOCTYPE public identifier
                                  * state.
                                  */
                                 continue;
                             case '"':
                                 /*
                                  * U+0022 QUOTATION MARK (") Set the DOCTYPE
                                  * token's public identifier to the empty string
                                  * (not missing),
                                  */
                                 clearLongStrBufForNextState();
                                 /*
                                  * then switch to the DOCTYPE public identifier
                                  * (double-quoted) state.
                                  */
                                 state = Tokenizer.DOCTYPE_PUBLIC_IDENTIFIER_DOUBLE_QUOTED;
                                 break beforedoctypepublicidentifierloop;
                             // continue stateloop;
                             case '\'':
                                 /*
                                  * U+0027 APOSTROPHE (') Set the DOCTYPE token's
                                  * public identifier to the empty string (not
                                  * missing),
                                  */
                                 clearLongStrBufForNextState();
                                 /*
                                  * then switch to the DOCTYPE public identifier
                                  * (single-quoted) state.
                                  */
                                 state = Tokenizer.DOCTYPE_PUBLIC_IDENTIFIER_SINGLE_QUOTED;
                                 continue stateloop;
                             case '>':
                                 /* U+003E GREATER-THAN SIGN (>) Parse error. */
                                 err("Expected a public identifier but the doctype ended.");
                                 /*
                                  * Set the DOCTYPE token's force-quirks flag to
                                  * on. Emit that DOCTYPE token.
                                  */
                                 tokenHandler.doctype(doctypeName, null, null,
                                         true);
                                 /*
                                  * Switch to the data state.
                                  */
                                 cstart = pos + 1;
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             default:
                                 bogusDoctype();
                                 /*
                                  * Set the DOCTYPE token's force-quirks flag to
                                  * on.
                                  */
                                 // done by bogusDoctype();
                                 /*
                                  * Switch to the bogus DOCTYPE state.
                                  */
                                 state = Tokenizer.BOGUS_DOCTYPE;
                                 continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case DOCTYPE_PUBLIC_IDENTIFIER_DOUBLE_QUOTED:
                     doctypepublicidentifierdoublequotedloop: for (;;) {
                         c = read();
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '"':
                                 /*
                                  * U+0022 QUOTATION MARK (") Switch to the after
                                  * DOCTYPE public identifier state.
                                  */
                                 publicIdentifier = longStrBufToString();
                                 state = Tokenizer.AFTER_DOCTYPE_PUBLIC_IDENTIFIER;
                                 break doctypepublicidentifierdoublequotedloop;
                             // continue stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Parse error.
                                  */
                                 err("\u201C>\u201D in public identifier.");
                                 /*
                                  * Set the DOCTYPE token's force-quirks flag to
                                  * on. Emit that DOCTYPE token.
                                  */
                                 tokenHandler.doctype(doctypeName,
                                         longStrBufToString(), null, true);
                                 /*
                                  * Switch to the data state.
                                  */
                                 cstart = pos + 1;
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             default:
                                 /*
                                  * Anything else Append the current input
                                  * character to the current DOCTYPE token's
                                  * public identifier.
                                  */
                                 appendLongStrBuf(c);
                                 /*
                                  * Stay in the DOCTYPE public identifier
                                  * (double-quoted) state.
                                  */
                                 continue;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case AFTER_DOCTYPE_PUBLIC_IDENTIFIER:
                     afterdoctypepublicidentifierloop: for (;;) {
                         c = read();
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ' ':
                             case '\t':
                             case '\n':
 
                             case '\u000C':
                                 /*
                                  * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                  * (LF) U+000C FORM FEED (FF) U+0020 SPACE Stay
                                  * in the after DOCTYPE public identifier state.
                                  */
                                 continue;
                             case '"':
                                 /*
                                  * U+0022 QUOTATION MARK (") Set the DOCTYPE
                                  * token's system identifier to the empty string
                                  * (not missing),
                                  */
                                 clearLongStrBufForNextState();
                                 /*
                                  * then switch to the DOCTYPE system identifier
                                  * (double-quoted) state.
                                  */
                                 state = Tokenizer.DOCTYPE_SYSTEM_IDENTIFIER_DOUBLE_QUOTED;
                                 break afterdoctypepublicidentifierloop;
                             // continue stateloop;
                             case '\'':
                                 /*
                                  * U+0027 APOSTROPHE (') Set the DOCTYPE token's
                                  * system identifier to the empty string (not
                                  * missing),
                                  */
                                 clearLongStrBufForNextState();
                                 /*
                                  * then switch to the DOCTYPE system identifier
                                  * (single-quoted) state.
                                  */
                                 state = Tokenizer.DOCTYPE_SYSTEM_IDENTIFIER_SINGLE_QUOTED;
                                 continue stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Emit the current
                                  * DOCTYPE token.
                                  */
                                 tokenHandler.doctype(doctypeName,
                                         publicIdentifier, null, false);
                                 /*
                                  * Switch to the data state.
                                  */
                                 cstart = pos + 1;
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             default:
                                 bogusDoctype();
                                 /*
                                  * Set the DOCTYPE token's force-quirks flag to
                                  * on.
                                  */
                                 // done by bogusDoctype();
                                 /*
                                  * Switch to the bogus DOCTYPE state.
                                  */
                                 state = Tokenizer.BOGUS_DOCTYPE;
                                 continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case DOCTYPE_SYSTEM_IDENTIFIER_DOUBLE_QUOTED:
                     doctypesystemidentifierdoublequotedloop: for (;;) {
                         c = read();
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '"':
                                 /*
                                  * U+0022 QUOTATION MARK (") Switch to the after
                                  * DOCTYPE system identifier state.
                                  */
                                 systemIdentifier = longStrBufToString();
                                 state = Tokenizer.AFTER_DOCTYPE_SYSTEM_IDENTIFIER;
                                 continue stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Parse error.
                                  */
                                 err("\u201C>\u201D in system identifier.");
                                 /*
                                  * Set the DOCTYPE token's force-quirks flag to
                                  * on. Emit that DOCTYPE token.
                                  */
                                 tokenHandler.doctype(doctypeName,
                                         publicIdentifier, longStrBufToString(),
                                         true);
 
                                 /*
                                  * Switch to the data state.
                                  * 
                                  */
                                 cstart = pos + 1;
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             default:
                                 /*
                                  * Anything else Append the current input
                                  * character to the current DOCTYPE token's
                                  * system identifier.
                                  */
                                 appendLongStrBuf(c);
                                 /*
                                  * Stay in the DOCTYPE system identifier
                                  * (double-quoted) state.
                                  */
                                 continue;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case AFTER_DOCTYPE_SYSTEM_IDENTIFIER:
                     afterdoctypesystemidentifierloop: for (;;) {
                         c = read();
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ' ':
                             case '\t':
                             case '\n':
 
                             case '\u000C':
                                 /*
                                  * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                  * (LF) U+000C FORM FEED (FF) U+0020 SPACE Stay
                                  * in the after DOCTYPE system identifier state.
                                  */
                                 continue;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Emit the current
                                  * DOCTYPE token.
                                  */
                                 tokenHandler.doctype(doctypeName,
                                         publicIdentifier, systemIdentifier,
                                         false);
                                 /*
                                  * Switch to the data state.
                                  */
                                 cstart = pos + 1;
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             default:
                                 /*
                                  * Switch to the bogus DOCTYPE state. (This does
                                  * not set the DOCTYPE token's force-quirks flag
                                  * to on.)
                                  */
                                 bogusDoctypeWithoutQuirks();
                                 state = Tokenizer.BOGUS_DOCTYPE;
                                 break afterdoctypesystemidentifierloop;
                             // continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case BOGUS_DOCTYPE:
                     for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Emit that
                                  * DOCTYPE token.
                                  */
                                 tokenHandler.doctype(doctypeName,
                                         publicIdentifier, systemIdentifier,
                                         forceQuirks);
                                 /*
                                  * Switch to the data state.
                                  */
                                 cstart = pos + 1;
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             default:
                                 /*
                                  * Anything else Stay in the bogus DOCTYPE
                                  * state.
                                  */
                                 continue;
                         }
                     }
                     // XXX reorder point
                 case DOCTYPE_YSTEM:
                     doctypeystemloop: for (;;) {
                         c = read();
                         if (c == '\u0000') {
                             break stateloop;
                         }
                         /*
                          * Otherwise, if the next six characters are an ASCII
                          * case-insensitive match for the word "SYSTEM", then
                          * consume those characters and switch to the before
                          * DOCTYPE system identifier state.
                          */
                         if (index < 5) { // YSTEM.length
                             char folded = c;
                             if (c >= 'A' && c <= 'Z') {
                                 folded += 0x20;
                             }
                             if (folded != Tokenizer.YSTEM[index]) {
                                 bogusDoctype();
                                 state = Tokenizer.BOGUS_DOCTYPE;
                                 reconsume = true;
                                 continue stateloop;
                             }
                             index++;
                             continue stateloop;
                         } else {
                             state = Tokenizer.BEFORE_DOCTYPE_SYSTEM_IDENTIFIER;
                             reconsume = true;
                             break doctypeystemloop;
                             // continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case BEFORE_DOCTYPE_SYSTEM_IDENTIFIER:
                     beforedoctypesystemidentifierloop: for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ' ':
                             case '\t':
                             case '\n':
 
                             case '\u000C':
                                 /*
                                  * U+0009 CHARACTER TABULATION U+000A LINE FEED
                                  * (LF) U+000C FORM FEED (FF) U+0020 SPACE Stay
                                  * in the before DOCTYPE system identifier
                                  * state.
                                  */
                                 continue;
                             case '"':
                                 /*
                                  * U+0022 QUOTATION MARK (") Set the DOCTYPE
                                  * token's system identifier to the empty string
                                  * (not missing),
                                  */
                                 clearLongStrBufForNextState();
                                 /*
                                  * then switch to the DOCTYPE system identifier
                                  * (double-quoted) state.
                                  */
                                 state = Tokenizer.DOCTYPE_SYSTEM_IDENTIFIER_DOUBLE_QUOTED;
                                 continue stateloop;
                             case '\'':
                                 /*
                                  * U+0027 APOSTROPHE (') Set the DOCTYPE token's
                                  * system identifier to the empty string (not
                                  * missing),
                                  */
                                 clearLongStrBufForNextState();
                                 /*
                                  * then switch to the DOCTYPE system identifier
                                  * (single-quoted) state.
                                  */
                                 state = Tokenizer.DOCTYPE_SYSTEM_IDENTIFIER_SINGLE_QUOTED;
                                 break beforedoctypesystemidentifierloop;
                             // continue stateloop;
                             case '>':
                                 /* U+003E GREATER-THAN SIGN (>) Parse error. */
                                 err("Expected a system identifier but the doctype ended.");
                                 /*
                                  * Set the DOCTYPE token's force-quirks flag to
                                  * on. Emit that DOCTYPE token.
                                  */
                                 tokenHandler.doctype(doctypeName, null, null,
                                         true);
                                 /*
                                  * Switch to the data state.
                                  */
                                 cstart = pos + 1;
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             default:
                                 bogusDoctype();
                                 /*
                                  * Set the DOCTYPE token's force-quirks flag to
                                  * on.
                                  */
                                 // done by bogusDoctype();
                                 /*
                                  * Switch to the bogus DOCTYPE state.
                                  */
                                 state = Tokenizer.BOGUS_DOCTYPE;
                                 continue stateloop;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case DOCTYPE_SYSTEM_IDENTIFIER_SINGLE_QUOTED:
                     for (;;) {
                         c = read();
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '\'':
                                 /*
                                  * U+0027 APOSTROPHE (') Switch to the after
                                  * DOCTYPE system identifier state.
                                  */
                                 systemIdentifier = longStrBufToString();
                                 state = Tokenizer.AFTER_DOCTYPE_SYSTEM_IDENTIFIER;
                                 continue stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Parse error.
                                  */
                                 err("\u201C>\u201D in system identifier.");
                                 /*
                                  * Set the DOCTYPE token's force-quirks flag to
                                  * on. Emit that DOCTYPE token.
                                  */
                                 tokenHandler.doctype(doctypeName,
                                         publicIdentifier, longStrBufToString(),
                                         true);
                                 /*
                                  * Switch to the data state.
                                  * 
                                  */
                                 cstart = pos + 1;
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             default:
                                 /*
                                  * Anything else Append the current input
                                  * character to the current DOCTYPE token's
                                  * system identifier.
                                  */
                                 appendLongStrBuf(c);
                                 /*
                                  * Stay in the DOCTYPE system identifier
                                  * (double-quoted) state.
                                  */
                                 continue;
                         }
                     }
                     // XXX reorder point
                 case DOCTYPE_PUBLIC_IDENTIFIER_SINGLE_QUOTED:
                     for (;;) {
                         c = read();
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '\'':
                                 /*
                                  * U+0027 APOSTROPHE (') Switch to the after
                                  * DOCTYPE public identifier state.
                                  */
                                 publicIdentifier = longStrBufToString();
                                 state = Tokenizer.AFTER_DOCTYPE_PUBLIC_IDENTIFIER;
                                 continue stateloop;
                             case '>':
                                 /*
                                  * U+003E GREATER-THAN SIGN (>) Parse error.
                                  */
                                 err("\u201C>\u201D in public identifier.");
                                 /*
                                  * Set the DOCTYPE token's force-quirks flag to
                                  * on. Emit that DOCTYPE token.
                                  */
                                 tokenHandler.doctype(doctypeName,
                                         longStrBufToString(), null, true);
                                 /*
                                  * Switch to the data state.
                                  */
                                 cstart = pos + 1;
                                 state = Tokenizer.DATA;
                                 continue stateloop;
                             default:
                                 /*
                                  * Anything else Append the current input
                                  * character to the current DOCTYPE token's
                                  * public identifier.
                                  */
                                 appendLongStrBuf(c);
                                 /*
                                  * Stay in the DOCTYPE public identifier
                                  * (single-quoted) state.
                                  */
                                 continue;
                         }
                     }
                     // XXX reorder point
                 case CDATA_START:
                     for (;;) {
                         c = read();
                         if (c == '\u0000') {
                             break stateloop;
                         }
                         if (index < 6) { // CDATA_LSQB.length
                             if (c == Tokenizer.CDATA_LSQB[index]) {
                                 appendLongStrBuf(c);
                             } else {
                                 err("Bogus comment.");
                                 state = Tokenizer.BOGUS_COMMENT;
                                 reconsume = true;
                                 continue stateloop;
                             }
                             index++;
                             continue;
                         } else {
                             cstart = pos; // start coalescing
                             state = Tokenizer.CDATA_SECTION;
                             reconsume = true;
                             break; // FALL THROUGH continue stateloop;
                         }
                     }
                     // WARNING FALLTHRU CASE TRANSITION: DON'T REORDER
                 case CDATA_SECTION:
                     cdataloop: for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ']':
                                 flushChars();
                                 state = Tokenizer.CDATA_RSQB;
                                 break cdataloop; // FALL THROUGH
                             default:
                                 continue;
                         }
                     }
                     // WARNING FALLTHRU CASE TRANSITION: DON'T REORDER
                 case CDATA_RSQB:
                     cdatarsqb: for (;;) {
                         c = read();
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case ']':
                                 state = Tokenizer.CDATA_RSQB_RSQB;
                                 break cdatarsqb;
                             default:
                                 tokenHandler.characters(Tokenizer.RSQB_RSQB, 0,
                                         1);
                                 cstart = pos;
                                 state = Tokenizer.CDATA_SECTION;
                                 reconsume = true;
                                 continue stateloop;
                         }
                     }
                     // WARNING FALLTHRU CASE TRANSITION: DON'T REORDER
                 case CDATA_RSQB_RSQB:
                     c = read();
                     switch (c) {
                         case '\u0000':
                             break stateloop;
                         case '>':
                             cstart = pos + 1;
                             state = Tokenizer.DATA;
                             continue stateloop;
                         default:
                             tokenHandler.characters(Tokenizer.RSQB_RSQB, 0, 2);
                             cstart = pos;
                             state = Tokenizer.CDATA_SECTION;
                             reconsume = true;
                             continue stateloop;
 
                     }
                     // XXX reorder point
                 case ATTRIBUTE_VALUE_SINGLE_QUOTED:
                     attributevaluesinglequotedloop: for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         /*
                          * Consume the next input character:
                          */
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '\'':
                                 /*
                                  * U+0027 APOSTROPHE (') Switch to the after
                                  * attribute value (quoted) state.
                                  */
                                 addAttributeWithValue();
 
                                 state = Tokenizer.AFTER_ATTRIBUTE_VALUE_QUOTED;
                                 continue stateloop;
                             case '&':
                                 /*
                                  * U+0026 AMPERSAND (&) Switch to the character
                                  * reference in attribute value state, with the +
                                  * additional allowed character being U+0027
                                  * APOSTROPHE (').
                                  */
                                 detachLongStrBuf();
                                 clearStrBufAndAppendCurrentC(c);
                                 additional = '\'';
                                 returnState = state;
                                 state = Tokenizer.CONSUME_CHARACTER_REFERENCE;
                                 break attributevaluesinglequotedloop;
                             // continue stateloop;
                             default:
                                 /*
                                  * Anything else Append the current input
                                  * character to the current attribute's value.
                                  */
                                 appendLongStrBuf(c);
                                 /*
                                  * Stay in the attribute value (double-quoted)
                                  * state.
                                  */
                                 continue;
                         }
                     }
                     // FALLTHRU DON'T REORDER
                 case CONSUME_CHARACTER_REFERENCE:
                     c = read();
                     if (c == '\u0000') {
                         break stateloop;
                     }
                     /*
                      * Unlike the definition is the spec, this state does not
                      * return a value and never requires the caller to
                      * backtrack. This state takes care of emitting characters
                      * or appending to the current attribute value. It also
                      * takes care of that in the case when consuming the
                      * character reference fails.
                      */
                     /*
                      * This section defines how to consume a character
                      * reference. This definition is used when parsing character
                      * references in text and in attributes.
                      * 
                      * The behavior depends on the identity of the next
                      * character (the one immediately after the U+0026 AMPERSAND
                      * character):
                      */
                     switch (c) {
                         case ' ':
                         case '\t':
                         case '\n':
 
                         case '\u000C':
                         case '<':
                         case '&':
                             emitOrAppendStrBuf(returnState);
                             cstart = pos;
                             state = returnState;
                             reconsume = true;
                             continue stateloop;
                         case '#':
                             /*
                              * U+0023 NUMBER SIGN (#) Consume the U+0023 NUMBER
                              * SIGN.
                              */
                             appendStrBuf('#');
                             state = Tokenizer.CONSUME_NCR;
                             continue stateloop;
                         default:
                             if (c == additional) {
                                 emitOrAppendStrBuf(returnState);
                                 state = returnState;
                                 reconsume = true;
                                 continue stateloop;
                             }
                             entCol = -1;
                             lo = 0;
                             hi = (NamedCharacters.NAMES.length - 1);
                             candidate = -1;
                             strBufMark = 0;
                             state = Tokenizer.CHARACTER_REFERENCE_LOOP;
                             reconsume = true;
                             // FALL THROUGH continue stateloop;
                     }
                     // WARNING FALLTHRU CASE TRANSITION: DON'T REORDER
                 case CHARACTER_REFERENCE_LOOP:
                     outer: for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         if (c == '\u0000') {
                             break stateloop;
                         }
                         entCol++;
                         /*
                          * Consume the maximum number of characters possible,
                          * with the consumed characters matching one of the
                          * identifiers in the first column of the named
                          * character references table (in a case-sensitive
                          * manner).
                          */
                         hiloop: for (;;) {
                             if (hi == -1) {
                                 break hiloop;
                             }
                             if (entCol == NamedCharacters.NAMES[hi].length) {
                                 break hiloop;
                             }
                             if (entCol > NamedCharacters.NAMES[hi].length) {
                                 break outer;
                             } else if (c < NamedCharacters.NAMES[hi][entCol]) {
                                 hi--;
                             } else {
                                 break hiloop;
                             }
                         }
 
                         loloop: for (;;) {
                             if (hi < lo) {
                                 break outer;
                             }
                             if (entCol == NamedCharacters.NAMES[lo].length) {
                                 candidate = lo;
                                 strBufMark = strBufLen;
                                 lo++;
                             } else if (entCol > NamedCharacters.NAMES[lo].length) {
                                 break outer;
                             } else if (c > NamedCharacters.NAMES[lo][entCol]) {
                                 lo++;
                             } else {
                                 break loloop;
                             }
                         }
                         if (hi < lo) {
                             break outer;
                         }
                         appendStrBuf(c);
                         continue;
                     }
 
                     // TODO warn about apos (IE) and TRADE (Opera)
                     if (candidate == -1) {
                         /*
                          * If no match can be made, then this is a parse error.
                          */
                         err("Text after \u201C&\u201D did not match an entity name. Probable cause: \u201C&\u201D should have been escaped as \u201C&amp;\u201D.");
                         emitOrAppendStrBuf(returnState);
                         if ((returnState & (~1)) == 0) {
                             cstart = pos;
                         }
                         state = returnState;
                         reconsume = true;
                         continue stateloop;
                     } else {
                         char[] candidateArr = NamedCharacters.NAMES[candidate];
                         if (candidateArr[candidateArr.length - 1] != ';') {
                             /*
                              * If the last character matched is not a U+003B
                              * SEMICOLON (;), there is a parse error.
                              */
                            err("Entity reference was not terminated by a semicolon.");
                             if ((returnState & (~1)) != 0) {
                                 /*
                                  * If the entity is being consumed as part of an
                                  * attribute, and the last character matched is
                                  * not a U+003B SEMICOLON (;),
                                  */
                                 char ch;
                                 if (strBufMark == strBufLen) {
                                     ch = c;
                                 } else {
                                     // if (strBufOffset != -1) {
                                     // ch = buf[strBufOffset + strBufMark];
                                     // } else {
                                     ch = strBuf[strBufMark];
                                     // }
                                 }
                                 if ((ch >= '0' && ch <= '9')
                                         || (ch >= 'A' && ch <= 'Z')
                                         || (ch >= 'a' && ch <= 'z')) {
                                     /*
                                      * and the next character is in the range
                                      * U+0030 DIGIT ZERO to U+0039 DIGIT NINE,
                                      * U+0041 LATIN CAPITAL LETTER A to U+005A
                                      * LATIN CAPITAL LETTER Z, or U+0061 LATIN
                                      * SMALL LETTER A to U+007A LATIN SMALL
                                      * LETTER Z, then, for historical reasons,
                                      * all the characters that were matched
                                      * after the U+0026 AMPERSAND (&) must be
                                      * unconsumed, and nothing is returned.
                                      */
                                     appendStrBufToLongStrBuf();
                                     state = returnState;
                                     reconsume = true;
                                     continue stateloop;
                                 }
                             }
                         }
 
                         /*
                          * Otherwise, return a character token for the character
                          * corresponding to the entity name (as given by the
                          * second column of the named character references
                          * table).
                          */
                         char[] val = NamedCharacters.VALUES[candidate];
                         emitOrAppend(val, returnState);
                         // this is so complicated!
                         if (strBufMark < strBufLen) {
                             if (strBufOffset != -1) {
                                 if ((returnState & (~1)) != 0) {
                                     for (int i = strBufMark; i < strBufLen; i++) {
                                         appendLongStrBuf(buf[strBufOffset + i]);
                                     }
                                 } else {
                                     tokenHandler.characters(buf, strBufOffset
                                             + strBufMark, strBufLen
                                             - strBufMark);
                                 }
                             } else {
                                 if ((returnState & (~1)) != 0) {
                                     for (int i = strBufMark; i < strBufLen; i++) {
                                         appendLongStrBuf(strBuf[i]);
                                     }
                                 } else {
                                     tokenHandler.characters(strBuf, strBufMark,
                                             strBufLen - strBufMark);
                                 }
                             }
                         }
                         if ((returnState & (~1)) == 0) {
                             cstart = pos;
                         }
                         state = returnState;
                         reconsume = true;
                         continue stateloop;
                         /*
                          * If the markup contains I'm &notit; I tell you, the
                          * entity is parsed as "not", as in, I'm ¬it; I tell
                          * you. But if the markup was I'm &notin; I tell you,
                          * the entity would be parsed as "notin;", resulting in
                          * I'm ∉ I tell you.
                          */
                     }
                     // XXX reorder point
                 case CONSUME_NCR:
                     c = read();
                     prevValue = -1;
                     value = 0;
                     seenDigits = false;
                     /*
                      * The behavior further depends on the character after the
                      * U+0023 NUMBER SIGN:
                      */
                     switch (c) {
                         case '\u0000':
                             break stateloop;
                         case 'x':
                         case 'X':
 
                             /*
                              * U+0078 LATIN SMALL LETTER X U+0058 LATIN CAPITAL
                              * LETTER X Consume the X.
                              * 
                              * Follow the steps below, but using the range of
                              * characters U+0030 DIGIT ZERO through to U+0039
                              * DIGIT NINE, U+0061 LATIN SMALL LETTER A through
                              * to U+0066 LATIN SMALL LETTER F, and U+0041 LATIN
                              * CAPITAL LETTER A, through to U+0046 LATIN CAPITAL
                              * LETTER F (in other words, 0-9, A-F, a-f).
                              * 
                              * When it comes to interpreting the number,
                              * interpret it as a hexadecimal number.
                              */
                             appendStrBuf(c);
                             state = Tokenizer.HEX_NCR_LOOP;
                             continue stateloop;
                         default:
                             /*
                              * Anything else Follow the steps below, but using
                              * the range of characters U+0030 DIGIT ZERO through
                              * to U+0039 DIGIT NINE (i.e. just 0-9).
                              * 
                              * When it comes to interpreting the number,
                              * interpret it as a decimal number.
                              */
                             state = Tokenizer.DECIMAL_NRC_LOOP;
                             reconsume = true;
                             // FALL THROUGH continue stateloop;
                     }
                     // WARNING FALLTHRU CASE TRANSITION: DON'T REORDER
                 case DECIMAL_NRC_LOOP:
                     decimalloop: for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         if (c == '\u0000') {
                             break stateloop;
                         }
                         // Deal with overflow gracefully
                         if (value < prevValue) {
                             value = 0x110000; // Value above Unicode range but
                             // within int
                             // range
                         }
                         prevValue = value;
                         /*
                          * Consume as many characters as match the range of
                          * characters given above.
                          */
                         if (c >= '0' && c <= '9') {
                             seenDigits = true;
                             value *= 10;
                             value += c - '0';
                             continue;
                         } else if (c == ';') {
                             if (seenDigits) {
                                 state = Tokenizer.HANDLE_NCR_VALUE;
                                 if ((returnState & (~1)) == 0) {
                                     cstart = pos + 1;
                                 }
                                 // FALL THROUGH continue stateloop;
                                 break decimalloop;
                             } else {
                                 err("No digits after \u201C" + strBufToString()
                                         + "\u201D.");
                                 appendStrBuf(';');
                                 emitOrAppendStrBuf(returnState);
                                 if ((returnState & (~1)) == 0) {
                                     cstart = pos + 1;
                                 }
                                 state = returnState;
                                 continue stateloop;
                             }
                         } else {
                             /*
                              * If no characters match the range, then don't
                              * consume any characters (and unconsume the U+0023
                              * NUMBER SIGN character and, if appropriate, the X
                              * character). This is a parse error; nothing is
                              * returned.
                              * 
                              * Otherwise, if the next character is a U+003B
                              * SEMICOLON, consume that too. If it isn't, there
                              * is a parse error.
                              */
                             if (!seenDigits) {
                                 err("No digits after \u201C" + strBufToString()
                                         + "\u201D.");
                                 emitOrAppendStrBuf(returnState);
                                 if ((returnState & (~1)) == 0) {
                                     cstart = pos;
                                 }
                                 state = returnState;
                                 reconsume = true;
                                 continue stateloop;
                             } else {
                                 err("Character reference was not terminated by a semicolon.");
                                 state = Tokenizer.HANDLE_NCR_VALUE;
                                 reconsume = true;
                                 if ((returnState & (~1)) == 0) {
                                     cstart = pos;
                                 }
                                 // FALL THROUGH continue stateloop;
                                 break decimalloop;
                             }
                         }
                     }
                     // WARNING FALLTHRU CASE TRANSITION: DON'T REORDER
                 case HANDLE_NCR_VALUE:
                     // WARNING previous state sets reconsume
                     // XXX inline this case if the method size can take it
                     handleNcrValue(returnState);
                     state = returnState;
                     continue stateloop;
                     // XXX reorder point
                 case HEX_NCR_LOOP:
                     for (;;) {
                         c = read();
                         if (c == '\u0000') {
                             break stateloop;
                         }
                         // Deal with overflow gracefully
                         if (value < prevValue) {
                             value = 0x110000; // Value above Unicode range but
                             // within int
                             // range
                         }
                         prevValue = value;
                         /*
                          * Consume as many characters as match the range of
                          * characters given above.
                          */
                         if (c >= '0' && c <= '9') {
                             seenDigits = true;
                             value *= 16;
                             value += c - '0';
                             continue;
                         } else if (c >= 'A' && c <= 'F') {
                             seenDigits = true;
                             value *= 16;
                             value += c - 'A' + 10;
                             continue;
                         } else if (c >= 'a' && c <= 'f') {
                             seenDigits = true;
                             value *= 16;
                             value += c - 'a' + 10;
                             continue;
                         } else if (c == ';') {
                             if (seenDigits) {
                                 state = Tokenizer.HANDLE_NCR_VALUE;
                                 cstart = pos + 1;
                                 continue stateloop;
                             } else {
                                 err("No digits after \u201C" + strBufToString()
                                         + "\u201D.");
                                 appendStrBuf(';');
                                 emitOrAppendStrBuf(returnState);
                                 if ((returnState & (~1)) == 0) {
                                     cstart = pos + 1;
                                 }
                                 state = returnState;
                                 continue stateloop;
                             }
                         } else {
                             /*
                              * If no characters match the range, then don't
                              * consume any characters (and unconsume the U+0023
                              * NUMBER SIGN character and, if appropriate, the X
                              * character). This is a parse error; nothing is
                              * returned.
                              * 
                              * Otherwise, if the next character is a U+003B
                              * SEMICOLON, consume that too. If it isn't, there
                              * is a parse error.
                              */
                             if (!seenDigits) {
                                 err("No digits after \u201C" + strBufToString()
                                         + "\u201D.");
                                 emitOrAppendStrBuf(returnState);
                                 if ((returnState & (~1)) == 0) {
                                     cstart = pos;
                                 }
                                 state = returnState;
                                 reconsume = true;
                                 continue stateloop;
                             } else {
                                 err("Character reference was not terminated by a semicolon.");
                                 if ((returnState & (~1)) == 0) {
                                     cstart = pos;
                                 }
                                 state = Tokenizer.HANDLE_NCR_VALUE;
                                 reconsume = true;
                                 continue stateloop;
                             }
                         }
                     }
                     // XXX reorder point
                 case PLAINTEXT:
                     plaintextloop: for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             default:
                                 /*
                                  * Anything else Emit the input character as a
                                  * character token.
                                  */
                                 /*
                                  * Stay in the data state.
                                  */
                                 continue;
                         }
                     }
                     // XXX reorder point
                 case CDATA:
                     cdataloop: for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '<':
                                 /*
                                  * U+003C LESS-THAN SIGN (<) When the content
                                  * model flag is set to the PCDATA state: switch
                                  * to the tag open state. When the content model
                                  * flag is set to either the RCDATA state or the
                                  * CDATA state and the escape flag is false:
                                  * switch to the tag open state. Otherwise:
                                  * treat it as per the "anything else" entry
                                  * below.
                                  */
                                 flushChars();
 
                                 returnState = state;
                                 state = Tokenizer.TAG_OPEN_NON_PCDATA;
                                 break cdataloop; // FALL THRU continue
                             // stateloop;
                             default:
                                 /*
                                  * Anything else Emit the input character as a
                                  * character token.
                                  */
                                 /*
                                  * Stay in the data state.
                                  */
                                 continue;
                         }
                     }
                     // WARNING FALLTHRU CASE TRANSITION: DON'T REORDER
                 case TAG_OPEN_NON_PCDATA:
                     tagopennonpcdataloop: for (;;) {
                         c = read();
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '!':
                                 tokenHandler.characters(Tokenizer.LT_GT, 0, 1);
                                 cstart = pos;
                                 state = Tokenizer.ESCAPE_EXCLAMATION;
                                 break tagopennonpcdataloop; // FALL THRU
                             // continue
                             // stateloop;
                             case '/':
                                 /*
                                  * If the content model flag is set to the
                                  * RCDATA or CDATA states Consume the next input
                                  * character.
                                  */
                                 if (contentModelElement != null) {
                                     /*
                                      * If it is a U+002F SOLIDUS (/) character,
                                      * switch to the close tag open state.
                                      */
                                     index = 0;
                                     clearStrBufForNextState();
                                     state = Tokenizer.CLOSE_TAG_OPEN_NOT_PCDATA;
                                     continue stateloop;
                                 } // else fall through
                             default:
                                 /*
                                  * Otherwise, emit a U+003C LESS-THAN SIGN
                                  * character token
                                  */
                                 tokenHandler.characters(Tokenizer.LT_GT, 0, 1);
                                 /*
                                  * and reconsume the current input character in
                                  * the data state.
                                  */
                                 cstart = pos;
                                 state = returnState;
                                 reconsume = true;
                                 continue stateloop;
                         }
                     }
                     // WARNING FALLTHRU CASE TRANSITION: DON'T REORDER
                 case ESCAPE_EXCLAMATION:
                     escapeexclamationloop: for (;;) {
                         c = read();
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '-':
                                 state = Tokenizer.ESCAPE_EXCLAMATION_HYPHEN;
                                 break escapeexclamationloop; // FALL THRU
                             // continue
                             // stateloop;
                             default:
                                 state = returnState;
                                 reconsume = true;
                                 continue stateloop;
                         }
                     }
                     // WARNING FALLTHRU CASE TRANSITION: DON'T REORDER
                 case ESCAPE_EXCLAMATION_HYPHEN:
                     escapeexclamationhyphenloop: for (;;) {
                         c = read();
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '-':
                                 state = Tokenizer.ESCAPE_HYPHEN_HYPHEN;
                                 break escapeexclamationhyphenloop;
                             // continue stateloop;
                             default:
                                 state = returnState;
                                 reconsume = true;
                                 continue stateloop;
                         }
                     }
                     // WARNING FALLTHRU CASE TRANSITION: DON'T REORDER
                 case ESCAPE_HYPHEN_HYPHEN:
                     escapehyphenhyphenloop: for (;;) {
                         c = read();
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '-':
                                 continue;
                             case '>':
                                 state = returnState;
                                 continue stateloop;
                             default:
                                 state = Tokenizer.ESCAPE;
                                 break escapehyphenhyphenloop;
                             // continue stateloop;
                         }
                     }
                     // WARNING FALLTHRU CASE TRANSITION: DON'T REORDER
                 case ESCAPE:
                     escapeloop: for (;;) {
                         c = read();
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '-':
                                 state = Tokenizer.ESCAPE_HYPHEN;
                                 break escapeloop; // FALL THRU continue
                             // stateloop;
                             default:
                                 continue escapeloop;
                         }
                     }
                     // WARNING FALLTHRU CASE TRANSITION: DON'T REORDER
                 case ESCAPE_HYPHEN:
                     escapehyphenloop: for (;;) {
                         c = read();
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '-':
                                 state = Tokenizer.ESCAPE_HYPHEN_HYPHEN;
                                 continue stateloop;
                             default:
                                 state = Tokenizer.ESCAPE;
                                 continue stateloop;
                         }
                     }
                     // XXX reorder point
                 case CLOSE_TAG_OPEN_NOT_PCDATA:
                     for (;;) {
                         c = read();
                         if (c == '\u0000') {
                             break stateloop;
                         }
                         // ASSERT! when entering this state, set index to 0 and
                         // call clearStrBuf()
                         assert (contentModelElement != null);
                         /*
                          * If the content model flag is set to the RCDATA or
                          * CDATA states but no start tag token has ever been
                          * emitted by this instance of the tokeniser (fragment
                          * case), or, if the content model flag is set to the
                          * RCDATA or CDATA states and the next few characters do
                          * not match the tag name of the last start tag token
                          * emitted (compared in an ASCII case insensitive
                          * manner), or if they do but they are not immediately
                          * followed by one of the following characters: + U+0009
                          * CHARACTER TABULATION + U+000A LINE FEED (LF) + +
                          * U+000C FORM FEED (FF) + U+0020 SPACE + U+003E
                          * GREATER-THAN SIGN (>) + U+002F SOLIDUS (/) + EOF
                          * 
                          * ...then emit a U+003C LESS-THAN SIGN character token,
                          * a U+002F SOLIDUS character token, and switch to the
                          * data state to process the next input character.
                          */
                         // Let's implement the above without lookahead. strBuf
                         // holds
                         // characters that need to be emitted if looking for an
                         // end tag
                         // fails.
                         // Duplicating the relevant part of tag name state here
                         // as well.
                         if (index < contentModelElement.name.length()) {
                             char e = contentModelElement.name.charAt(index);
                             char folded = c;
                             if (c >= 'A' && c <= 'Z') {
                                 folded += 0x20;
                             }
                             if (folded != e) {
                                 if (index > 0
                                         || (folded >= 'a' && folded <= 'z')) {
                                     // [NOCPP[
                                     if (html4) {
                                         if (ElementName.IFRAME != contentModelElement) {
                                             err((contentModelFlag == ContentModelFlag.CDATA ? "CDATA"
                                                     : "RCDATA")
                                                     + " element \u201C"
                                                     + contentModelElement.name
                                                     + "\u201D contained the string \u201C</\u201D, but it was not the start of the end tag. (HTML4-only error)");
                                         }
                                     } else {
                                         // ]NOCPP]
                                         warn((contentModelFlag == ContentModelFlag.CDATA ? "CDATA"
                                                 : "RCDATA")
                                                 + " element \u201C"
                                                 + contentModelElement.name
                                                 + "\u201D contained the string \u201C</\u201D, but this did not close the element.");
                                         // [NOCPP[
                                     }
                                     // ]NOCPP]
                                 }
                                 tokenHandler.characters(Tokenizer.LT_SOLIDUS,
                                         0, 2);
                                 emitStrBuf();
                                 cstart = pos;
                                 state = returnState;
                                 reconsume = true;
                                 continue stateloop;
                             }
                             appendStrBuf(c);
                             index++;
                             continue;
                         } else {
                             endTag = true;
                             // XXX replace contentModelElement with different
                             // type
                             tagName = contentModelElement;
                             switch (c) {
                                 case ' ':
                                 case '\t':
                                 case '\n':
 
                                 case '\u000C':
                                     /*
                                      * U+0009 CHARACTER TABULATION U+000A LINE
                                      * FEED (LF) U+000C FORM FEED (FF) U+0020
                                      * SPACE Switch to the before attribute name
                                      * state.
                                      */
                                     state = Tokenizer.BEFORE_ATTRIBUTE_NAME;
                                     continue stateloop;
                                 case '>':
                                     /*
                                      * U+003E GREATER-THAN SIGN (>) Emit the
                                      * current tag token.
                                      */
                                     state = emitCurrentTagToken(false);
                                     if (shouldSuspend) {
                                         break stateloop;
                                     }
                                     /*
                                      * Switch to the data state.
                                      */
                                     continue stateloop;
                                 case '/':
                                     /*
                                      * U+002F SOLIDUS (/) Parse error unless
                                      * this is a permitted slash.
                                      */
                                     // never permitted here
                                     err("Stray \u201C/\u201D in end tag.");
                                     /*
                                      * Switch to the before attribute name
                                      * state.
                                      */
                                     state = Tokenizer.BEFORE_ATTRIBUTE_NAME;
                                     continue stateloop;
                                 default:
                                     if (html4) {
                                         err((contentModelFlag == ContentModelFlag.CDATA ? "CDATA"
                                                 : "RCDATA")
                                                 + " element \u201C"
                                                 + contentModelElement
                                                 + "\u201D contained the string \u201C</\u201D, but it was not the start of the end tag. (HTML4-only error)");
                                     } else {
                                         warn((contentModelFlag == ContentModelFlag.CDATA ? "CDATA"
                                                 : "RCDATA")
                                                 + " element \u201C"
                                                 + contentModelElement
                                                 + "\u201D contained the string \u201C</\u201D, but this did not close the element.");
                                     }
                                     tokenHandler.characters(
                                             Tokenizer.LT_SOLIDUS, 0, 2);
                                     emitStrBuf();
                                     cstart = pos; // don't drop the character
                                     state = returnState;
                                     continue stateloop;
                             }
                         }
                     }
                     // XXX reorder point
                 case CLOSE_TAG_OPEN_PCDATA:
                     c = read();
                     if (c == '\u0000') {
                         break stateloop;
                     }
                     /*
                      * Otherwise, if the content model flag is set to the PCDATA
                      * state, or if the next few characters do match that tag
                      * name, consume the next input character:
                      */
                     if (c >= 'A' && c <= 'Z') {
                         /*
                          * U+0041 LATIN CAPITAL LETTER A through to U+005A LATIN
                          * CAPITAL LETTER Z Create a new end tag token,
                          */
                         endTag = true;
                         /*
                          * set its tag name to the lowercase version of the
                          * input character (add 0x0020 to the character's code
                          * point),
                          */
                         clearStrBufAndAppendForceWrite((char) (c + 0x20));
                         /*
                          * then switch to the tag name state. (Don't emit the
                          * token yet; further details will be filled in before
                          * it is emitted.)
                          */
                         state = Tokenizer.TAG_NAME;
                         continue stateloop;
                     } else if (c >= 'a' && c <= 'z') {
                         /*
                          * U+0061 LATIN SMALL LETTER A through to U+007A LATIN
                          * SMALL LETTER Z Create a new end tag token,
                          */
                         endTag = true;
                         /*
                          * set its tag name to the input character,
                          */
                         clearStrBufAndAppendCurrentC(c);
                         /*
                          * then switch to the tag name state. (Don't emit the
                          * token yet; further details will be filled in before
                          * it is emitted.)
                          */
                         state = Tokenizer.TAG_NAME;
                         continue stateloop;
                     } else if (c == '>') {
                         /* U+003E GREATER-THAN SIGN (>) Parse error. */
                         err("Saw \u201C</>\u201D. Probable causes: Unescaped \u201C<\u201D (escape as \u201C&lt;\u201D) or mistyped end tag.");
                         /*
                          * Switch to the data state.
                          */
                         cstart = pos + 1;
                         state = Tokenizer.DATA;
                         continue stateloop;
                     } else {
                         /* Anything else Parse error. */
                         err("Garbage after \u201C</\u201D.");
                         /*
                          * Switch to the bogus comment state.
                          */
                         clearLongStrBufAndAppendToComment(c);
                         state = Tokenizer.BOGUS_COMMENT;
                         continue stateloop;
                     }
                     // XXX reorder point
                 case RCDATA:
                     rcdataloop: for (;;) {
                         if (reconsume) {
                             reconsume = false;
                         } else {
                             c = read();
                         }
                         switch (c) {
                             case '\u0000':
                                 break stateloop;
                             case '&':
                                 /*
                                  * U+0026 AMPERSAND (&) When the content model
                                  * flag is set to one of the PCDATA or RCDATA
                                  * states and the escape flag is false: switch
                                  * to the character reference data state.
                                  * Otherwise: treat it as per the "anything
                                  * else" entry below.
                                  */
                                 flushChars();
                                 clearStrBufAndAppendCurrentC(c);
                                 additional = '\u0000';
                                 returnState = state;
                                 state = Tokenizer.CONSUME_CHARACTER_REFERENCE;
                                 continue stateloop;
                             case '<':
                                 /*
                                  * U+003C LESS-THAN SIGN (<) When the content
                                  * model flag is set to the PCDATA state: switch
                                  * to the tag open state. When the content model
                                  * flag is set to either the RCDATA state or the
                                  * CDATA state and the escape flag is false:
                                  * switch to the tag open state. Otherwise:
                                  * treat it as per the "anything else" entry
                                  * below.
                                  */
                                 flushChars();
 
                                 returnState = state;
                                 state = Tokenizer.TAG_OPEN_NON_PCDATA;
                                 continue stateloop;
                             default:
                                 /*
                                  * Anything else Emit the input character as a
                                  * character token.
                                  */
                                 /*
                                  * Stay in the data state.
                                  */
                                 continue;
                         }
                     }
 
             }
         }
         flushChars();
         if (prev == '\r' && pos != end) {
             pos--;
             col--;
         }
         // Save locals
         stateSave = state;
         returnStateSave = returnState;
     }
 
     private void errQuoteBeforeAttributeName(char c) throws SAXException {
         err("Saw \u201C"
                 + c
                 + "\u201D when expecting an attribute name. Probable cause: \u201C=\u201D missing immediately before.");
     }
 
     private void errQuoteInAttributeName(char c) throws SAXException {
         err("Quote \u201C"
                 + c
                 + "\u201D in attribute name. Probable cause: Matching quote missing somewhere earlier.");
     }
 
     private void bogusDoctype() throws SAXException {
         err("Bogus doctype.");
         forceQuirks = true;
     }
 
     private void bogusDoctypeWithoutQuirks() throws SAXException {
         err("Bogus doctype.");
         forceQuirks = false;
     }
 
     private void emitOrAppendStrBuf(int returnState) throws SAXException {
         if ((returnState & (~1)) != 0) {
             appendStrBufToLongStrBuf();
         } else {
             emitStrBuf();
         }
     }
 
     private void handleNcrValue(int returnState) throws SAXException {
         /*
          * If one or more characters match the range, then take them all and
          * interpret the string of characters as a number (either hexadecimal or
          * decimal as appropriate).
          */
         if (value >= 0x80 && value <= 0x9f) {
             /*
              * If that number is one of the numbers in the first column of the
              * following table, then this is a parse error.
              */
             err("A numeric character reference expanded to the C1 controls range.");
             /*
              * Find the row with that number in the first column, and return a
              * character token for the Unicode character given in the second
              * column of that row.
              */
             char[] val = NamedCharacters.WINDOWS_1252[value - 0x80];
             emitOrAppend(val, returnState);
         } else if (value == 0x0D) {
             err("A numeric character reference expanded to carriage return.");
             emitOrAppend(Tokenizer.LF, returnState);
         } else if (value == 0xC
                 && contentSpacePolicy != XmlViolationPolicy.ALLOW) {
             if (contentSpacePolicy == XmlViolationPolicy.ALTER_INFOSET) {
                 emitOrAppend(Tokenizer.SPACE, returnState);
             } else if (contentSpacePolicy == XmlViolationPolicy.FATAL) {
                 fatal("A character reference expanded to a form feed which is not legal XML 1.0 white space.");
             }
         } else if ((value >= 0x0000 && value <= 0x0008) || (value == 0x000B)
                 || (value >= 0x000E && value <= 0x001F) || value == 0x007F) {
             /*
              * OOtherwise, if the number is in the range 0x0000 to 0x0008,
              * U+000B, U+000E to 0x001F, 0x007F to 0x009F, 0xD800 to 0xDFFF ,
              * 0xFDD0 to 0xFDDF, or is one of 0xFFFE, 0xFFFF, 0x1FFFE, 0x1FFFF,
              * 0x2FFFE, 0x2FFFF, 0x3FFFE, 0x3FFFF, 0x4FFFE, 0x4FFFF, 0x5FFFE,
              * 0x5FFFF, 0x6FFFE, 0x6FFFF, 0x7FFFE, 0x7FFFF, 0x8FFFE, 0x8FFFF,
              * 0x9FFFE, 0x9FFFF, 0xAFFFE, 0xAFFFF, 0xBFFFE, 0xBFFFF, 0xCFFFE,
              * 0xCFFFF, 0xDFFFE, 0xDFFFF, 0xEFFFE, 0xEFFFF, 0xFFFFE, 0xFFFFF,
              * 0x10FFFE, or 0x10FFFF, or is higher than 0x10FFFF, then this is a
              * parse error; return a character token for the U+FFFD REPLACEMENT
              * CHARACTER character instead.
              */
             err("Character reference expands to a control character ("
                     + toUPlusString((char) value) + ").");
             emitOrAppend(Tokenizer.REPLACEMENT_CHARACTER, returnState);
         } else if ((value & 0xF800) == 0xD800) {
             err("Character reference expands to a surrogate.");
             emitOrAppend(Tokenizer.REPLACEMENT_CHARACTER, returnState);
         } else if (isNonCharacter(value)) {
             err("Character reference expands to a non-character.");
             emitOrAppend(Tokenizer.REPLACEMENT_CHARACTER, returnState);
         } else if (value <= 0xFFFF) {
             /*
              * Otherwise, return a character token for the Unicode character
              * whose code point is that number.
              */
             char ch = (char) value;
             if (errorHandler != null && isPrivateUse(ch)) {
                 warnAboutPrivateUseChar();
             }
             bmpChar[0] = ch;
             emitOrAppend(bmpChar, returnState);
         } else if (value <= 0x10FFFF) {
             if (errorHandler != null && isAstralPrivateUse(value)) {
                 warnAboutPrivateUseChar();
             }
             astralChar[0] = (char) (Tokenizer.LEAD_OFFSET + (value >> 10));
             astralChar[1] = (char) (0xDC00 + (value & 0x3FF));
             emitOrAppend(astralChar, returnState);
         } else {
             err("Character reference outside the permissible Unicode range.");
             emitOrAppend(Tokenizer.REPLACEMENT_CHARACTER, returnState);
         }
     }
 
     public void eof() throws SAXException {
         int state = stateSave;
         int returnState = returnStateSave;
 
         eofloop: for (;;) {
             switch (state) {
                 case TAG_OPEN_NON_PCDATA:
                     /*
                      * Otherwise, emit a U+003C LESS-THAN SIGN character token
                      */
                     tokenHandler.characters(Tokenizer.LT_GT, 0, 1);
                     /*
                      * and reconsume the current input character in the data
                      * state.
                      */
                     break eofloop;
                 case TAG_OPEN:
                     /*
                      * The behavior of this state depends on the content model
                      * flag.
                      */
                     /*
                      * Anything else Parse error.
                      */
                     err("End of file in the tag open state.");
                     /*
                      * Emit a U+003C LESS-THAN SIGN character token
                      */
                     tokenHandler.characters(Tokenizer.LT_GT, 0, 1);
                     /*
                      * and reconsume the current input character in the data
                      * state.
                      */
                     break eofloop;
                 case CLOSE_TAG_OPEN_NOT_PCDATA:
                     break eofloop;
                 case CLOSE_TAG_OPEN_PCDATA:
                     /* EOF Parse error. */
                     err("Saw \u201C</\u201D immediately before end of file.");
                     /*
                      * Emit a U+003C LESS-THAN SIGN character token and a U+002F
                      * SOLIDUS character token.
                      */
                     tokenHandler.characters(Tokenizer.LT_SOLIDUS, 0, 2);
                     /*
                      * Reconsume the EOF character in the data state.
                      */
                     break eofloop;
                 case TAG_NAME:
                     /*
                      * EOF Parse error.
                      */
                     err("End of file seen when looking for tag name");
                     /*
                      * Emit the current tag token.
                      */
                     tagName = strBufToElementNameString();
                     emitCurrentTagToken(false);
                     /*
                      * Reconsume the EOF character in the data state.
                      */
                     break eofloop;
                 case BEFORE_ATTRIBUTE_NAME:
                 case AFTER_ATTRIBUTE_VALUE_QUOTED:
                 case SELF_CLOSING_START_TAG:
                     /* EOF Parse error. */
                     err("Saw end of file without the previous tag ending with \u201C>\u201D.");
                     /*
                      * Emit the current tag token.
                      */
                     emitCurrentTagToken(false);
                     /*
                      * Reconsume the EOF character in the data state.
                      */
                     break eofloop;
                 case ATTRIBUTE_NAME:
                     /*
                      * EOF Parse error.
                      */
                     err("End of file occurred in an attribute name.");
                     /*
                      * Emit the current tag token.
                      */
                     attributeNameComplete();
                     addAttributeWithoutValue();
                     emitCurrentTagToken(false);
                     /*
                      * Reconsume the EOF character in the data state.
                      */
                     break eofloop;
                 case AFTER_ATTRIBUTE_NAME:
                 case BEFORE_ATTRIBUTE_VALUE:
                     /* EOF Parse error. */
                     err("Saw end of file without the previous tag ending with \u201C>\u201D.");
                     /*
                      * Emit the current tag token.
                      */
                     addAttributeWithoutValue();
                     emitCurrentTagToken(false);
                     /*
                      * Reconsume the character in the data state.
                      */
                     break eofloop;
                 case ATTRIBUTE_VALUE_DOUBLE_QUOTED:
                 case ATTRIBUTE_VALUE_SINGLE_QUOTED:
                 case ATTRIBUTE_VALUE_UNQUOTED:
                     /* EOF Parse error. */
                     err("End of file reached when inside an attribute value.");
                     /* Emit the current tag token. */
                     addAttributeWithValue();
                     emitCurrentTagToken(false);
                     /*
                      * Reconsume the character in the data state.
                      */
                     break eofloop;
                 case BOGUS_COMMENT:
                     emitComment(0);
                     break eofloop;
                 case BOGUS_COMMENT_HYPHEN:
                     maybeAppendSpaceToBogusComment();
                     emitComment(0);
                     break eofloop;
                 case MARKUP_DECLARATION_OPEN:
                     err("Bogus comment.");
                     clearLongStrBuf();
                     emitComment(0);
                     break eofloop;
                 case MARKUP_DECLARATION_HYPHEN:
                     err("Bogus comment.");
                     emitComment(0);
                     break eofloop;
                 case MARKUP_DECLARATION_OCTYPE:
                     err("Bogus comment.");
                     emitComment(0);
                     break eofloop;
                 case COMMENT_START:
                 case COMMENT:
                     /*
                      * EOF Parse error.
                      */
                     err("End of file inside comment.");
                     /* Emit the comment token. */
                     emitComment(0);
                     /*
                      * Reconsume the EOF character in the data state.
                      */
                     break eofloop;
                 case COMMENT_END:
                     /*
                      * EOF Parse error.
                      */
                     err("End of file inside comment.");
                     /* Emit the comment token. */
                     emitComment(2);
                     /*
                      * Reconsume the EOF character in the data state.
                      */
                     break eofloop;
                 case COMMENT_END_DASH:
                 case COMMENT_START_DASH:
                     /*
                      * EOF Parse error.
                      */
                     err("End of file inside comment.");
                     /* Emit the comment token. */
                     emitComment(1);
                     /*
                      * Reconsume the EOF character in the data state.
                      */
                     break eofloop;
                 case DOCTYPE:
                 case BEFORE_DOCTYPE_NAME:
                     /* EOF Parse error. */
                     err("End of file inside doctype.");
                     /*
                      * Create a new DOCTYPE token. Set its force-quirks flag to
                      * on. Emit the token.
                      */
                     tokenHandler.doctype("", null, null, true);
                     /*
                      * Reconsume the EOF character in the data state.
                      */
                     break eofloop;
                 case DOCTYPE_NAME:
                     /* EOF Parse error. */
                     err("End of file inside doctype.");
                     /*
                      * Set the DOCTYPE token's force-quirks flag to on. Emit
                      * that DOCTYPE token.
                      */
                     tokenHandler.doctype(strBufToString(), null, null, true);
                     /*
                      * Reconsume the EOF character in the data state.
                      */
                     break eofloop;
                 case DOCTYPE_UBLIC:
                 case DOCTYPE_YSTEM:
                 case AFTER_DOCTYPE_NAME:
                 case BEFORE_DOCTYPE_PUBLIC_IDENTIFIER:
                     /* EOF Parse error. */
                     err("End of file inside doctype.");
                     /*
                      * Set the DOCTYPE token's force-quirks flag to on. Emit
                      * that DOCTYPE token.
                      */
                     tokenHandler.doctype(doctypeName, null, null, true);
                     /*
                      * Reconsume the EOF character in the data state.
                      */
                     break eofloop;
                 case DOCTYPE_PUBLIC_IDENTIFIER_DOUBLE_QUOTED:
                 case DOCTYPE_PUBLIC_IDENTIFIER_SINGLE_QUOTED:
                     /* EOF Parse error. */
                     err("End of file inside public identifier.");
                     /*
                      * Set the DOCTYPE token's force-quirks flag to on. Emit
                      * that DOCTYPE token.
                      */
                     tokenHandler.doctype(doctypeName, longStrBufToString(),
                             null, true);
                     /*
                      * Reconsume the EOF character in the data state.
                      */
                     break eofloop;
                 case AFTER_DOCTYPE_PUBLIC_IDENTIFIER:
                 case BEFORE_DOCTYPE_SYSTEM_IDENTIFIER:
                     /* EOF Parse error. */
                     err("End of file inside doctype.");
                     /*
                      * Set the DOCTYPE token's force-quirks flag to on. Emit
                      * that DOCTYPE token.
                      */
                     tokenHandler.doctype(doctypeName, publicIdentifier, null,
                             true);
                     /*
                      * Reconsume the EOF character in the data state.
                      */
                     break eofloop;
                 case DOCTYPE_SYSTEM_IDENTIFIER_DOUBLE_QUOTED:
                 case DOCTYPE_SYSTEM_IDENTIFIER_SINGLE_QUOTED:
                     /* EOF Parse error. */
                     err("End of file inside system identifier.");
                     /*
                      * Set the DOCTYPE token's force-quirks flag to on. Emit
                      * that DOCTYPE token.
                      */
                     tokenHandler.doctype(doctypeName, publicIdentifier,
                             longStrBufToString(), true);
                     /*
                      * Reconsume the EOF character in the data state.
                      */
                     break eofloop;
                 case AFTER_DOCTYPE_SYSTEM_IDENTIFIER:
                     /* EOF Parse error. */
                     err("End of file inside doctype.");
                     /*
                      * Set the DOCTYPE token's force-quirks flag to on. Emit
                      * that DOCTYPE token.
                      */
                     tokenHandler.doctype(doctypeName, publicIdentifier,
                             systemIdentifier, true);
                     /*
                      * Reconsume the EOF character in the data state.
                      */
                     break eofloop;
                 case BOGUS_DOCTYPE:
                     /*
                      * Emit that DOCTYPE token.
                      */
                     tokenHandler.doctype(doctypeName, publicIdentifier,
                             systemIdentifier, forceQuirks);
                     /*
                      * Reconsume the EOF character in the data state.
                      */
                     break eofloop;
                 case CONSUME_CHARACTER_REFERENCE:
                     /*
                      * Unlike the definition is the spec, this state does not
                      * return a value and never requires the caller to
                      * backtrack. This state takes care of emitting characters
                      * or appending to the current attribute value. It also
                      * takes care of that in the case when consuming the entity
                      * fails.
                      */
                     /*
                      * This section defines how to consume an entity. This
                      * definition is used when parsing entities in text and in
                      * attributes.
                      * 
                      * The behavior depends on the identity of the next
                      * character (the one immediately after the U+0026 AMPERSAND
                      * character):
                      */
 
                     emitOrAppendStrBuf(returnState);
                     state = returnState;
                     continue;
                 case CHARACTER_REFERENCE_LOOP:
                     outer: for (;;) {
                         char c = '\u0000';
                         entCol++;
                         /*
                          * Consume the maximum number of characters possible,
                          * with the consumed characters matching one of the
                          * identifiers in the first column of the named
                          * character references table (in a case-sensitive
                          * manner).
                          */
                         hiloop: for (;;) {
                             if (hi == -1) {
                                 break hiloop;
                             }
                             if (entCol == NamedCharacters.NAMES[hi].length) {
                                 break hiloop;
                             }
                             if (entCol > NamedCharacters.NAMES[hi].length) {
                                 break outer;
                             } else if (c < NamedCharacters.NAMES[hi][entCol]) {
                                 hi--;
                             } else {
                                 break hiloop;
                             }
                         }
 
                         loloop: for (;;) {
                             if (hi < lo) {
                                 break outer;
                             }
                             if (entCol == NamedCharacters.NAMES[lo].length) {
                                 candidate = lo;
                                 strBufMark = strBufLen;
                                 lo++;
                             } else if (entCol > NamedCharacters.NAMES[lo].length) {
                                 break outer;
                             } else if (c > NamedCharacters.NAMES[lo][entCol]) {
                                 lo++;
                             } else {
                                 break loloop;
                             }
                         }
                         if (hi < lo) {
                             break outer;
                         }
                         continue;
                     }
 
                     // TODO warn about apos (IE) and TRADE (Opera)
                     if (candidate == -1) {
                         /*
                          * If no match can be made, then this is a parse error.
                          */
                         err("Text after \u201C&\u201D did not match an entity name. Probable cause: \u201C&\u201D should have been escaped as \u201C&amp;\u201D.");
                         emitOrAppendStrBuf(returnState);
                         state = returnState;
                         continue eofloop;
                     } else {
                         char[] candidateArr = NamedCharacters.NAMES[candidate];
                         if (candidateArr[candidateArr.length - 1] != ';') {
                             /*
                              * If the last character matched is not a U+003B
                              * SEMICOLON (;), there is a parse error.
                              */
                            err("Entity reference was not terminated by a semicolon.");
                             if ((returnState & (~1)) != 0) {
                                 /*
                                  * If the entity is being consumed as part of an
                                  * attribute, and the last character matched is
                                  * not a U+003B SEMICOLON (;),
                                  */
                                 char ch;
                                 if (strBufMark == strBufLen) {
                                     ch = '\u0000';
                                 } else {
                                     ch = strBuf[strBufMark];
                                 }
                                 if ((ch >= '0' && ch <= '9')
                                         || (ch >= 'A' && ch <= 'Z')
                                         || (ch >= 'a' && ch <= 'z')) {
                                     /*
                                      * and the next character is in the range
                                      * U+0030 DIGIT ZERO to U+0039 DIGIT NINE,
                                      * U+0041 LATIN CAPITAL LETTER A to U+005A
                                      * LATIN CAPITAL LETTER Z, or U+0061 LATIN
                                      * SMALL LETTER A to U+007A LATIN SMALL
                                      * LETTER Z, then, for historical reasons,
                                      * all the characters that were matched
                                      * after the U+0026 AMPERSAND (&) must be
                                      * unconsumed, and nothing is returned.
                                      */
                                     appendStrBufToLongStrBuf();
                                     state = returnState;
                                     continue eofloop;
                                 }
                             }
                         }
 
                         /*
                          * Otherwise, return a character token for the character
                          * corresponding to the entity name (as given by the
                          * second column of the named character references
                          * table).
                          */
                         char[] val = NamedCharacters.VALUES[candidate];
                         emitOrAppend(val, returnState);
                         // this is so complicated!
                         if (strBufMark < strBufLen) {
                             if ((returnState & (~1)) != 0) {
                                 for (int i = strBufMark; i < strBufLen; i++) {
                                     appendLongStrBuf(strBuf[i]);
                                 }
                             } else {
                                 tokenHandler.characters(strBuf, strBufMark,
                                         strBufLen - strBufMark);
                             }
                         }
                         state = returnState;
                         continue eofloop;
                         /*
                          * If the markup contains I'm &notit; I tell you, the
                          * entity is parsed as "not", as in, I'm ¬it; I tell
                          * you. But if the markup was I'm &notin; I tell you,
                          * the entity would be parsed as "notin;", resulting in
                          * I'm ∉ I tell you.
                          */
                     }
                 case CONSUME_NCR:
                 case DECIMAL_NRC_LOOP:
                 case HEX_NCR_LOOP:
                     /*
                      * If no characters match the range, then don't consume any
                      * characters (and unconsume the U+0023 NUMBER SIGN
                      * character and, if appropriate, the X character). This is
                      * a parse error; nothing is returned.
                      * 
                      * Otherwise, if the next character is a U+003B SEMICOLON,
                      * consume that too. If it isn't, there is a parse error.
                      */
                     if (!seenDigits) {
                         err("No digits after \u201C" + strBufToString()
                                 + "\u201D.");
                         emitOrAppendStrBuf(returnState);
                         state = returnState;
                         continue;
                     } else {
                         err("Character reference was not terminated by a semicolon.");
                         // FALL THROUGH continue stateloop;
                     }
                     // WARNING previous state sets reconsume
                     handleNcrValue(returnState);
                     state = returnState;
                     continue;
                 case DATA:
                 default:
                     break eofloop;
             }
         }
         // case DATA:
         /*
          * EOF Emit an end-of-file token.
          */
         tokenHandler.eof();
         return;
     }
 
     private char read() throws SAXException {
         char c;
         pos++;
         if (pos == end) {
             return '\u0000';
         }
 
         linePrev = line;
         colPrev = col;
         if (nextCharOnNewLine) {
             line++;
             col = 1;
             nextCharOnNewLine = false;
         } else {
             col++;
         }
 
         c = buf[pos];
         // [NOCPP[
         if (errorHandler == null
                 && contentNonXmlCharPolicy == XmlViolationPolicy.ALLOW) {
             // ]NOCPP]
             switch (c) {
                 case '\r':
                     nextCharOnNewLine = true;
                     buf[pos] = '\n';
                     prev = '\r';
                     return '\n';
                 case '\n':
                     if (prev == '\r') {
                         return '\u0000';
                     }
                     nextCharOnNewLine = true;
                     break;
                 case '\u0000':
                     /*
                      * All U+0000 NULL characters in the input must be replaced
                      * by U+FFFD REPLACEMENT CHARACTERs. Any occurrences of such
                      * characters is a parse error.
                      */
                     c = buf[pos] = '\uFFFD';
                     break;
             }
             // [NOCPP[
         } else {
             if (!confident
                     && !alreadyComplainedAboutNonAscii && c > '\u007F') {
                 complainAboutNonAscii();
                 alreadyComplainedAboutNonAscii = true;
             }
             switch (c) {
                 case '\r':
                     nextCharOnNewLine = true;
                     buf[pos] = '\n';
                     prev = '\r';
                     return '\n';
                 case '\n':
                     if (prev == '\r') {
                         return '\u0000';
                     }
                     nextCharOnNewLine = true;
                     break;
                 case '\u0000':
                     /*
                      * All U+0000 NULL characters in the input must be replaced
                      * by U+FFFD REPLACEMENT CHARACTERs. Any occurrences of such
                      * characters is a parse error.
                      */
                     err("Found U+0000 in the character stream.");
                     c = buf[pos] = '\uFFFD';
                     break;
 
                 case '\u000C':
                     if (contentNonXmlCharPolicy == XmlViolationPolicy.FATAL) {
                         fatal("This document is not mappable to XML 1.0 without data loss due to "
                                 + toUPlusString(c)
                                 + " which is not a legal XML 1.0 character.");
                     } else {
                         if (contentNonXmlCharPolicy == XmlViolationPolicy.ALTER_INFOSET) {
                             c = buf[pos] = ' ';
                         }
                         warn("This document is not mappable to XML 1.0 without data loss due to "
                                 + toUPlusString(c)
                                 + " which is not a legal XML 1.0 character.");
                     }
                     break;
                 default:
                     if ((c & 0xFC00) == 0xDC00) {
                         // Got a low surrogate. See if prev was high
                         // surrogate
                         if ((prev & 0xFC00) == 0xD800) {
                             int intVal = (prev << 10) + c
                                     + Tokenizer.SURROGATE_OFFSET;
                             if (isNonCharacter(intVal)) {
                                 err("Astral non-character.");
                             }
                             if (isAstralPrivateUse(intVal)) {
                                 warnAboutPrivateUseChar();
                             }
                         } else {
                             // XXX figure out what to do about lone high
                             // surrogates
                             err("Found low surrogate without high surrogate.");
                             // c = buf[pos] = '\uFFFD';
                         }
                     } else if ((c < ' ' || isNonCharacter(c)) && (c != '\t')) {
                         switch (contentNonXmlCharPolicy) {
                             case FATAL:
                                 fatal("Forbidden code point "
                                         + toUPlusString(c) + ".");
                                 break;
                             case ALTER_INFOSET:
                                 c = buf[pos] = '\uFFFD';
                                 // fall through
                             case ALLOW:
                                 err("Forbidden code point " + toUPlusString(c)
                                         + ".");
                         }
                     } else if ((c >= '\u007F') && (c <= '\u009F')
                             || (c >= '\uFDD0') && (c <= '\uFDDF')) {
                         err("Forbidden code point " + toUPlusString(c) + ".");
                     } else if (isPrivateUse(c)) {
                         warnAboutPrivateUseChar();
                     }
             }
         }
         // ]NOCPP]
         prev = c;
         return c;
     }
 
     private void complainAboutNonAscii() throws SAXException {
         String encoding = null;
         if (encodingDeclarationHandler != null) {
             encoding = encodingDeclarationHandler.getCharacterEncoding();
         }
         if (encoding == null) {
             err("The character encoding of the document was not explicit but the document contains non-ASCII.");
         } else {
             err("No explicit character encoding declaration has been seen yet (assumed \u201C"
                     + encoding + "\u201D) but the document contains non-ASCII.");
         }
     }
 
     public void internalEncodingDeclaration(String internalCharset)
             throws SAXException {
         if (encodingDeclarationHandler != null) {
             encodingDeclarationHandler.internalEncodingDeclaration(internalCharset);
         }
     }
 
     /**
      * @param val
      * @throws SAXException
      */
     private void emitOrAppend(char[] val, int returnState) throws SAXException {
         if ((returnState & (~1)) != 0) {
             appendLongStrBuf(val);
         } else {
             tokenHandler.characters(val, 0, val.length);
         }
     }
 
     public void end() throws SAXException {
         strBuf = null;
         longStrBuf = null;
         systemIdentifier = null;
         publicIdentifier = null;
         doctypeName = null;
         tagName = null;
         attributeName = null;
         tokenHandler.endTokenization();
         if (attributes != null) {
             attributes.clear();
             attributes.release();
         }
     }
 
     public void requestSuspension() {
         shouldSuspend = true;
     }
 
     /**
      * Returns the alreadyComplainedAboutNonAscii.
      * 
      * @return the alreadyComplainedAboutNonAscii
      */
     public boolean isAlreadyComplainedAboutNonAscii() {
         return alreadyComplainedAboutNonAscii;
     }
     
     public void becomeConfident() {
         confident = true;
     }
 
     /**
      * Returns the nextCharOnNewLine.
      * 
      * @return the nextCharOnNewLine
      */
     public boolean isNextCharOnNewLine() {
         return nextCharOnNewLine;
     }
 
     public boolean isPrevCR() {
         return prev == '\r';
     }
 
     /**
      * Returns the line.
      * 
      * @return the line
      */
     public int getLine() {
         return line;
     }
 
     /**
      * Returns the col.
      * 
      * @return the col
      */
     public int getCol() {
         return col;
     }
     
     
 }
