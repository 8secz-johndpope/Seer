 /*
 * $Id: HtmlUtils.java,v 1.10 2006/08/03 21:09:37 youngm Exp $
  */
 /*
 * $Id: HtmlUtils.java,v 1.10 2006/08/03 21:09:37 youngm Exp $
  */
 
 /*
  * The contents of this file are subject to the terms
  * of the Common Development and Distribution License
  * (the License). You may not use this file except in
  * compliance with the License.
  * 
  * You can obtain a copy of the License at
  * https://javaserverfaces.dev.java.net/CDDL.html or
  * legal/CDDLv1.0.txt. 
  * See the License for the specific language governing
  * permission and limitations under the License.
  * 
  * When distributing Covered Code, include this CDDL
  * Header Notice in each file and include the License file
  * at legal/CDDLv1.0.txt.    
  * If applicable, add the following below the CDDL Header,
  * with the fields enclosed by brackets [] replaced by
  * your own identifying information:
  * "Portions Copyrighted [year] [name of copyright owner]"
  * 
  * [Name of File] [ver.__] [Date]
  * 
  * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
  */
 
 package com.sun.faces.util;
 
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.OutputStreamWriter;
 import java.io.StringWriter;
 import java.io.UnsupportedEncodingException;
 import java.io.Writer;
 import java.util.BitSet;
 
 import com.sun.faces.RIConstants;
 
 /**
  * Utility class for HTML.
  * Kudos to Adam Winer (Oracle) for much of this code.
  */
 public class HtmlUtils {
 
     //-------------------------------------------------
     // The following methods include the handling of
     // escape characters....
     //-------------------------------------------------
 
     static public void writeText(Writer out,
                                  char[] buffer,
                                  char[] text) throws IOException {
         writeText(out, buffer, text, 0, text.length);
     }
 
 
     /**
      * Write char array text.  Note that this code is duplicated below
      * for Strings - change both places if you make any changes!!!
      */
     static public void writeText(Writer out,
                                  char[] buff,
                                  char[] text,
                                  int start,
                                  int length) throws IOException {
         int buffLength = buff.length;
         int buffIndex = 0;
 
         int end = start + length;
         for (int i = start; i < end; i++) {
             char ch = text[i];
             
             // Tilde or less...
             if (ch < 0xA0) {
                 // If "?" or over, no escaping is needed (this covers
                 // most of the Latin alphabet)
                 if (ch >= 0x3f) {
                     buffIndex = addToBuffer(out, buff, buffIndex,
                                             buffLength, ch);
                 } else if (ch >= 0x27) { // If above "'"...
                     // If between "'" and ";", no escaping is needed
                     if (ch < 0x3c) {
                         buffIndex = addToBuffer(out, buff, buffIndex,
                                                 buffLength, ch);
                     } else if (ch == '<') {
                         buffIndex = flushBuffer(out, buff, buffIndex);
 
                         out.write("&lt;");
                     } else if (ch == '>') {
                         buffIndex = flushBuffer(out, buff, buffIndex);
 
                         out.write("&gt;");
                     } else {
                         buffIndex = addToBuffer(out, buff, buffIndex,
                                                 buffLength, ch);
                     }
                 } else {
                     if (ch == '&') {
                         buffIndex = flushBuffer(out, buff, buffIndex);
 
                         out.write("&amp;");
                     } else {
                         buffIndex = addToBuffer(out, buff, buffIndex,
                                                 buffLength, ch);
                     }
                 }
             } else if (ch <= 0xff) {
                 // ISO-8859-1 entities: encode as needed
                 buffIndex = flushBuffer(out, buff, buffIndex);
 
                 out.write('&');
                 out.write(sISO8859_1_Entities[ch - 0xA0]);
                 out.write(';');
             } else {
                 // Double-byte characters to encode.
                 // PENDING: when outputting to an encoding that
                 // supports double-byte characters (UTF-8, for example),
                 // we should not be encoding
                 buffIndex = flushBuffer(out, buff, buffIndex);
                 _writeDecRef(out, ch);
             }
         }
 
         flushBuffer(out, buff, buffIndex);
     }
 
 
     /**
      * Write String text.  Note that this code is duplicated above for
      * character arrays - change both places if you make any changes!!!
      */
     static public void writeText(Writer out,
                                  char[] buff,
                                  String text) throws IOException {
         int buffLength = buff.length;
         int buffIndex = 0;
 
         int length = text.length();
 
         for (int i = 0; i < length; i++) {
             char ch = text.charAt(i);
             
             // Tilde or less...
             if (ch < 0xA0) {
                 // If "?" or over, no escaping is needed (this covers
                 // most of the Latin alphabet)
                 if (ch >= 0x3f) {
                     buffIndex = addToBuffer(out, buff, buffIndex,
                                             buffLength, ch);
                 } else if (ch >= 0x27) {  // If above "'"...
                     // If between "'" and ";", no escaping is needed
                     if (ch < 0x3c) {
                         buffIndex = addToBuffer(out, buff, buffIndex,
                                                 buffLength, ch);
                     } else if (ch == '<') {
                         buffIndex = flushBuffer(out, buff, buffIndex);
                         out.write("&lt;");
                     } else if (ch == '>') {
                         buffIndex = flushBuffer(out, buff, buffIndex);
                         out.write("&gt;");
                     } else {
                         buffIndex = addToBuffer(out, buff, buffIndex,
                                                 buffLength, ch);
                     }
                 } else {
                     if (ch == '&') {
                         buffIndex = flushBuffer(out, buff, buffIndex);
 
                         out.write("&amp;");
                     } else {
                         buffIndex = addToBuffer(out, buff, buffIndex,
                                                 buffLength, ch);
                     }
                 }
             } else if (ch <= 0xff) {
                 // ISO-8859-1 entities: encode as needed 
                 buffIndex = flushBuffer(out, buff, buffIndex);
 
                 out.write('&');
                 out.write(sISO8859_1_Entities[ch - 0xA0]);
                 out.write(';');
             } else {
                 // Double-byte characters to encode.
                 // PENDING: when outputting to an encoding that
                 // supports double-byte characters (UTF-8, for example),
                 // we should not be encoding
                 buffIndex = flushBuffer(out, buff, buffIndex);
                 _writeDecRef(out, ch);
             }
         }
 
         flushBuffer(out, buff, buffIndex);
     }
 
 
     /**
      * Write a string attribute.  Note that this code
      * is duplicated below for character arrays - change both
      * places if you make any changes!!!
      */
     static public void writeAttribute(Writer out,
                                       char[] buff,
                                       String text) throws IOException {
         int buffLength = buff.length;
         int buffIndex = 0;
 
         int length = text.length();
         for (int i = 0; i < length; i++) {
             char ch = text.charAt(i);
             
             // Tilde or less...
             if (ch < 0xA0) {
                 // If "?" or over, no escaping is needed (this covers
                 // most of the Latin alphabet)
                 if (ch >= 0x3f) {
                     buffIndex = addToBuffer(out, buff, buffIndex,
                                             buffLength, ch);
                 } else if (ch >= 0x27) { // If above "'"...
                     // If between "'" and ";", no escaping is needed
                     if (ch < 0x3c) {
                         buffIndex = addToBuffer(out, buff, buffIndex,
                                                 buffLength, ch);
                         // Note - "<" isn't escaped in attributes, as per
                         // HTML spec
                     } else if (ch == '>') {
                         buffIndex = flushBuffer(out, buff, buffIndex);
 
                         out.write("&gt;");
                     } else {
                         buffIndex = addToBuffer(out, buff, buffIndex,
                                                 buffLength, ch);
                     }
                 } else {
                     if (ch == '&') {
                         buffIndex = flushBuffer(out, buff, buffIndex);
                         
                         // HTML 4.0, section B.7.1: ampersands followed by
                         // an open brace don't get escaped
                         if ((i + 1 < length) && (text.charAt(i + 1) == '{'))
                             out.write(ch);
                         else
                             out.write("&amp;");
                     } else if (ch == '"') {
                         buffIndex = flushBuffer(out, buff, buffIndex);
 
                         out.write("&quot;");
                     } else {
                         buffIndex = addToBuffer(out, buff, buffIndex,
                                                 buffLength, ch);
                     }
                 }
             } else if (ch <= 0xff) {
                 // ISO-8859-1 entities: encode as needed
                 buffIndex = flushBuffer(out, buff, buffIndex);
 
                 out.write('&');
                 out.write(sISO8859_1_Entities[ch - 0xA0]);
                 out.write(';');
             } else {
                 buffIndex = flushBuffer(out, buff, buffIndex);
                 
                 // Double-byte characters to encode.
                 // PENDING: when outputting to an encoding that
                 // supports double-byte characters (UTF-8, for example),
                 // we should not be encoding
                 _writeDecRef(out, ch);
             }
         }
 
         flushBuffer(out, buff, buffIndex);
     }
 
 
     static public void writeAttribute(Writer out,
                                       char[] buffer,
                                       char[] text) throws IOException {
         writeAttribute(out, buffer, text, 0, text.length);
     }
 
 
     /**
      * Write a character array attribute.  Note that this code
      * is duplicated above for string - change both places if you make
      * any changes!!!
      */
     static public void writeAttribute(Writer out,
                                       char[] buff,
                                       char[] text,
                                       int start,
                                       int length) throws IOException {
         int buffLength = buff.length;
         int buffIndex = 0;
 
         int end = start + length;
         for (int i = start; i < end; i++) {
             char ch = text[i];
 
             // Tilde or less...
             if (ch < 0xA0) {
                 // If "?" or over, no escaping is needed (this covers
                 // most of the Latin alphabet)
                 if (ch >= 0x3f) {
                     buffIndex = addToBuffer(out, buff, buffIndex,
                                             buffLength, ch);
                 } else if (ch >= 0x27) { // If above "'"...
                     if (ch < 0x3c) {
                         // If between "'" and ";", no escaping is needed
                         buffIndex = addToBuffer(out, buff, buffIndex,
                                                 buffLength, ch);
                         // Note - "<" isn't escaped in attributes, as per HTML spec
                     } else if (ch == '>') {
                         buffIndex = flushBuffer(out, buff, buffIndex);
 
                         out.write("&gt;");
                     } else {
                         buffIndex = addToBuffer(out, buff, buffIndex,
                                                 buffLength, ch);
                     }
                 } else {
                     if (ch == '&') {
                         buffIndex = flushBuffer(out, buff, buffIndex);
 
                         // HTML 4.0, section B.7.1: ampersands followed by
                         // an open brace don't get escaped
                         if ((i + 1 < end) && (text[i + 1] == '{'))
                             out.write(ch);
                         else
                             out.write("&amp;");
                     } else if (ch == '"') {
                         buffIndex = flushBuffer(out, buff, buffIndex);
 
                         out.write("&quot;");
                     } else {
                         buffIndex = addToBuffer(out, buff, buffIndex,
                                                 buffLength, ch);
                     }
                 }
             } else if (ch <= 0xff) {
                 // ISO-8859-1 entities: encode as needed
                 buffIndex = flushBuffer(out, buff, buffIndex);
 
                 out.write('&');
                 out.write(sISO8859_1_Entities[ch - 0xA0]);
                 out.write(';');
             } else {
                 buffIndex = flushBuffer(out, buff, buffIndex);
                 
                 // Double-byte characters to encode.
                 // PENDING: when outputting to an encoding that
                 // supports double-byte characters (UTF-8, for example),
                 // we should not be encoding
                 _writeDecRef(out, ch);
             }
         }
 
         flushBuffer(out, buff, buffIndex);
     }
 
 
     /**
      * Writes a character as a decimal escape.  Hex escapes are smaller than
      * the decimal version, but Netscape didn't support hex escapes until
      * 4.7.4.
      */
     static private void _writeDecRef(Writer out, char ch) throws IOException {
         if (ch == '\u20ac') {
             out.write("&euro;");
             return;
         }
         out.write("&#");
         // Formerly used String.valueOf().  This version tests out
         // about 40% faster in a microbenchmark (and on systems where GC is
         // going gonzo, it should be even better)
         int i = (int) ch;
         if (i > 10000) {
             out.write('0' + (i / 10000));
             i = i % 10000;
             out.write('0' + (i / 1000));
             i = i % 1000;
             out.write('0' + (i / 100));
             i = i % 100;
             out.write('0' + (i / 10));
             i = i % 10;
             out.write('0' + i);
         } else if (i > 1000) {
             out.write('0' + (i / 1000));
             i = i % 1000;
             out.write('0' + (i / 100));
             i = i % 100;
             out.write('0' + (i / 10));
             i = i % 10;
             out.write('0' + i);
         } else {
             out.write('0' + (i / 100));
             i = i % 100;
             out.write('0' + (i / 10));
             i = i % 10;
             out.write('0' + i);
         }
 
         out.write(';');
     }
 
     // 
     // Buffering scheme: we use a tremendously simple buffering
     // scheme that greatly reduces the number of calls into the
     // Writer/PrintWriter.  In practice this has produced significant
     // measured performance gains (at least in JDK 1.3.1).  We only
     // support adding single characters to the buffer, so anytime
     // multiple characters need to be written out, the entire buffer
     // gets flushed.  In practice, this is good enough, and keeps
     // the core simple.
     //
 
     /**
      * Add a character to the buffer, flushing the buffer if the buffer is
      * full, and returning the new buffer index
      */
     private static int addToBuffer(Writer out,
                                    char[] buffer,
                                    int bufferIndex,
                                    int bufferLength,
                                    char ch) throws IOException {
         if (bufferIndex >= bufferLength) {
             out.write(buffer, 0, bufferIndex);
             bufferIndex = 0;
         }
 
         buffer[bufferIndex] = ch;
 
         return bufferIndex + 1;
     }
 
 
     /**
      * Flush the contents of the buffer to the output stream
      * and return the reset buffer index
      */
     private static int flushBuffer(Writer out,
                                    char[] buffer,
                                    int bufferIndex) throws IOException {
         if (bufferIndex > 0)
             out.write(buffer, 0, bufferIndex);
 
         return 0;
     }
 
 
     private HtmlUtils() {
     }
 
 
     /**
      * Writes a string into URL-encoded format out to a Writer.
      * <p/>
      * All characters before the start of the query string will be encoded
      * using ISO-8859-1.
      * PENDING: Ideally, we'd encode characters before the query string
      * using UTF-8, which is what the HTML spec recommends.  Unfortunately,
      * the Apache server doesn't support this until 2.0.
      * <p/>
      * Characters after the start of the query string will be encoded
      * using a client-defined encoding.  You'll need to use the encoding
      * that the server will expect.  (HTML forms will generate query
      * strings using the character encoding that the HTML itself was
      * generated in.)
      * <p/>
      * All characters will be encoded as needed for URLs, with the exception
      * of the percent symbol ("%").  Because this is the character
      * itself used for escaping, attempting to escape this character
      * would cause this code to double-escape some strings.  It also may
      * be necessary to pre-escape some characters.  In particular, a
      * question mark ("?") is considered the start of the query string.
      * <p/>
      *
      * @param out           a Writer for the output
      * @param text          the unencoded (or partially encoded) String
      * @param queryEncoding the character set encoding for after the first
      *                      question mark
      */
     static public void writeURL(Writer out,
    							char[] buff,
                                String text,
                                 String queryEncoding,
                                 String contentType)
         throws IOException, UnsupportedEncodingException {
     	int length = text.length();
 
         for (int i = 0; i < length; i++) {
             char ch = text.charAt(i);
 
             if ((ch < 33) || (ch > 126)) {
                 if (ch == ' ') {
                     out.write('+');
                 } else {
                     // ISO-8859-1.  Blindly assume the character will be < 255.
                     // Not much we can do if it isn't.
                     writeURIDoubleHex(out, ch);
 
                 }
             }
             // DO NOT encode '%'.  If you do, then for starters,
             // we'll double-encode anything that's pre-encoded.
             // And, what's worse, there becomes no way to use
             // characters that must be encoded if you
             // don't want them to be interpreted, like '?' or '&'.
             // else if('%' == ch)
             // {
             //   writeURIDoubleHex(out, ch);
             // } 
             else if (ch == '"') {
                 out.write("%22");
             }
             // Everything in the query parameters will be decoded
             // as if it were in the request's character set.  So use
             // the real encoding for those!
             else if (ch == '?') {
             	out.write('?');
             	//If the content type is an XML file use standard attribute rules.
             	if (RIConstants.XHTML_CONTENT_TYPE.equals(contentType) ||
             			RIConstants.APPLICATION_XML_CONTENT_TYPE.equals(contentType) ||
             			RIConstants.TEXT_XML_CONTENT_TYPE.equals(contentType)) {
             		String encodedURL = encodeURIString(text, queryEncoding, i + 1);
             		//Convert all & to &amp; and all &amp%3B to &amp;
             		char[] encodedURLArray = encodedURL.toCharArray();
             		for(int loop = 0;loop < encodedURLArray.length;loop++) {
             			if(encodedURLArray[loop] != '&') {
             				out.write(encodedURLArray[loop]);
             			} else if(loop+7 <= encodedURLArray.length && encodedURL.substring(loop, loop+7).equals("&amp%3B")) {
             				out.write("&amp;");
            					loop += 6;
             			} else {
             				out.write("&amp;");
             			}
             		}
             	} else {
             		out.write(encodeURIString(text, queryEncoding, i + 1));
             	}
                 return;
             } else {
                 out.write(ch);
             }
         }
     }
 
 
     // Encode a String into URI-encoded form.  This code will
     // appear rather (ahem) similar to java.net.URLEncoder
     static private String encodeURIString(String text,
                                         String encoding,
                                         int start)
         throws IOException, UnsupportedEncodingException {
     	Writer out = new StringWriter(text.length());
         ByteArrayOutputStream buf = null;
         OutputStreamWriter writer = null;
         char[] charArray = null;
 
         int length = text.length();
         for (int i = start; i < length; i++) {
             char ch = text.charAt(i);
             if (DONT_ENCODE_SET.get(ch)) {
                 out.write(ch);
             } else {
                 if (buf == null) {
                     buf = new ByteArrayOutputStream(MAX_BYTES_PER_CHAR);
                     if (encoding != null) {
                         writer = new OutputStreamWriter(buf, encoding);
                     } else {
                         writer = new OutputStreamWriter(buf);
                     }
                     charArray = new char[1];
                 }
 
                 // convert to external encoding before hex conversion
                 try {
                     // An inspection of OutputStreamWriter reveals
                     // that write(char) always allocates a one element
                     // character array.  We can reuse our own.
                     charArray[0] = ch;
                     writer.write(charArray, 0, 1);
                     writer.flush();
                 } catch (IOException e) {
                     buf.reset();
                     continue;
                 }
 
                 byte[] ba = buf.toByteArray();
                 for (int j = 0; j < ba.length; j++) {
                     writeURIDoubleHex(out, ba[j] + 256);
                 }
 
                 buf.reset();
             }
         }
         return out.toString();
     }
 
 
     static private void writeURIDoubleHex(Writer out,
                                           int i) throws IOException {
         out.write('%');
         out.write(intToHex((i >> 4) % 0x10));
         out.write(intToHex(i % 0x10));
     }
 
 
     static private char intToHex(int i) {
         if (i < 10)
             return ((char) ('0' + i));
         else
             return ((char) ('A' + (i - 10)));
     }
 
 
     static private final int MAX_BYTES_PER_CHAR = 10;
     static private final BitSet DONT_ENCODE_SET = new BitSet(256);
 
 
     // See: http://www.ietf.org/rfc/rfc2396.txt
     // We're not fully along for that ride either, but we do encode
     // ' ' as '%20', and don't bother encoding '~' or '/'
     static {
         for (int i = 'a'; i <= 'z'; i++) {
             DONT_ENCODE_SET.set(i);
         }
 
         for (int i = 'A'; i <= 'Z'; i++) {
             DONT_ENCODE_SET.set(i);
         }
 
         for (int i = '0'; i <= '9'; i++) {
             DONT_ENCODE_SET.set(i);
         }
         
         // Don't encode '%' - we don't want to double encode anything.
         DONT_ENCODE_SET.set('%');
         // Ditto for '+', which is an encoded space
         DONT_ENCODE_SET.set('+');
 
         DONT_ENCODE_SET.set('#');
         DONT_ENCODE_SET.set('&');
         DONT_ENCODE_SET.set('=');
         DONT_ENCODE_SET.set('-');
         DONT_ENCODE_SET.set('_');
         DONT_ENCODE_SET.set('.');
         DONT_ENCODE_SET.set('*');
         DONT_ENCODE_SET.set('~');
         DONT_ENCODE_SET.set('/');
         DONT_ENCODE_SET.set('\'');
         DONT_ENCODE_SET.set('!');
         DONT_ENCODE_SET.set('(');
         DONT_ENCODE_SET.set(')');
     }
 
 
     //
     // Entities from HTML 4.0, section 24.2.1; character codes 0xA0 to 0xFF
     //
     static private String[] sISO8859_1_Entities = new String[]{
         "nbsp",
         "iexcl",
         "cent",
         "pound",
         "curren",
         "yen",
         "brvbar",
         "sect",
         "uml",
         "copy",
         "ordf",
         "laquo",
         "not",
         "shy",
         "reg",
         "macr",
         "deg",
         "plusmn",
         "sup2",
         "sup3",
         "acute",
         "micro",
         "para",
         "middot",
         "cedil",
         "sup1",
         "ordm",
         "raquo",
         "frac14",
         "frac12",
         "frac34",
         "iquest",
         "Agrave",
         "Aacute",
         "Acirc",
         "Atilde",
         "Auml",
         "Aring",
         "AElig",
         "Ccedil",
         "Egrave",
         "Eacute",
         "Ecirc",
         "Euml",
         "Igrave",
         "Iacute",
         "Icirc",
         "Iuml",
         "ETH",
         "Ntilde",
         "Ograve",
         "Oacute",
         "Ocirc",
         "Otilde",
         "Ouml",
         "times",
         "Oslash",
         "Ugrave",
         "Uacute",
         "Ucirc",
         "Uuml",
         "Yacute",
         "THORN",
         "szlig",
         "agrave",
         "aacute",
         "acirc",
         "atilde",
         "auml",
         "aring",
         "aelig",
         "ccedil",
         "egrave",
         "eacute",
         "ecirc",
         "euml",
         "igrave",
         "iacute",
         "icirc",
         "iuml",
         "eth",
         "ntilde",
         "ograve",
         "oacute",
         "ocirc",
         "otilde",
         "ouml",
         "divide",
         "oslash",
         "ugrave",
         "uacute",
         "ucirc",
         "uuml",
         "yacute",
         "thorn",
         "yuml"
     };
 
 
     //----------------------------------------------------------
     // The following is used to verify encodings
     //----------------------------------------------------------
     //
     static public void validateEncoding(String encoding)
         throws UnsupportedEncodingException {
         if (encoding != null) {
             // Try creating a string off of the default encoding
             new String(encodingTestBytes, encoding);
         }
     }
 
 
     // Private array used simply to verify character encodings
     static private final byte[] encodingTestBytes = new byte[]{(byte) 65};
 
     //----------------------------------------------------------
     // The following is used to verify "empty" Html elements.
     // "Empty" Html elements are those that do not require an
     // ending tag.  For example, <br>  or <hr>...
     //----------------------------------------------------------
 
     static public boolean isEmptyElement(String name) {
         char firstChar = name.charAt(0);
         if (firstChar > _LAST_EMPTY_ELEMENT_START)
             return false;
 
         // Can we improve performance here?  It's certainly slower to use
         // a HashMap, at least if we can't assume the input name is lowercased.
         String[] array = emptyElementArr[name.charAt(0)];
         if (array != null) {
             for (int i = array.length - 1; i >= 0; i--) {
                 if (name.equalsIgnoreCase(array[i]))
                     return true;
             }
         }
         return false;
     }
 
 
     static private char _LAST_EMPTY_ELEMENT_START = 'p';
     static private String[][] emptyElementArr =
         new String[((int) _LAST_EMPTY_ELEMENT_START) + 1][];
 
     static private String[] aNames = new String[]{
         "area",
     };
 
     static private String[] bNames = new String[]{
         "br",
         "base",
         "basefont",
     };
 
     static private String[] cNames = new String[]{
         "col",
     };
 
     static private String[] fNames = new String[]{
         "frame",
     };
 
     static private String[] hNames = new String[]{
         "hr",
     };
 
     static private String[] iNames = new String[]{
         "img",
         "input",
         "isindex",
     };
 
     static private String[] lNames = new String[]{
         "link",
     };
 
     static private String[] mNames = new String[]{
         "meta",
     };
 
     static private String[] pNames = new String[]{
         "param",
     };
 
 
     static {
         emptyElementArr['a'] = aNames;
         emptyElementArr['A'] = aNames;
         emptyElementArr['b'] = bNames;
         emptyElementArr['B'] = bNames;
         emptyElementArr['c'] = cNames;
         emptyElementArr['C'] = cNames;
         emptyElementArr['f'] = fNames;
         emptyElementArr['F'] = fNames;
         emptyElementArr['h'] = hNames;
         emptyElementArr['H'] = hNames;
         emptyElementArr['i'] = iNames;
         emptyElementArr['I'] = iNames;
         emptyElementArr['l'] = lNames;
         emptyElementArr['L'] = lNames;
         emptyElementArr['m'] = mNames;
         emptyElementArr['M'] = mNames;
         emptyElementArr['p'] = pNames;
         emptyElementArr['P'] = pNames;
     }
 }
