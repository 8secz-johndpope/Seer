 /*  Copyright (c) 2006-2007, Vladimir Nikic
     All rights reserved.
 	
     Redistribution and use of this software in source and binary forms, 
     with or without modification, are permitted provided that the following 
     conditions are met:
 	
     * Redistributions of source code must retain the above
       copyright notice, this list of conditions and the
       following disclaimer.
 	
     * Redistributions in binary form must reproduce the above
       copyright notice, this list of conditions and the
       following disclaimer in the documentation and/or other
       materials provided with the distribution.
 	
     * The name of HtmlCleaner may not be used to endorse or promote 
       products derived from this software without specific prior
       written permission.
 
     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
     AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
     IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
     ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
     LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
     CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
     SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
     INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
     CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
     ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
     POSSIBILITY OF SUCH DAMAGE.
 	
     You can contact Vladimir Nikic by sending e-mail to
     nikic_vladimir@yahoo.com. Please include the word "HtmlCleaner" in the
     subject line.
 */
 
 package org.htmlcleaner;
 
 import java.io.*;
 import java.util.*;
 
 /**
  * <p>Abstract HTML serializer - contains common logic for descendants.</p>
  */
 public abstract class HtmlSerializer extends Serializer {
 
 	protected HtmlSerializer(CleanerProperties props) {
 		super(props);
     }
 
 
     protected boolean isMinimizedTagSyntax(TagNode tagNode) {
         final TagInfo tagInfo = props.getTagInfoProvider().getTagInfo(tagNode.getName());
         return tagInfo != null && !tagNode.hasChildren() && tagInfo.isEmptyTag();
     }
 
     protected boolean dontEscape(TagNode tagNode) {
         return isScriptOrStyle(tagNode);
     }
 
     protected String escapeText(String s) {
         boolean recognizeUnicodeChars = props.isRecognizeUnicodeChars();
         boolean translateSpecialEntities = props.isTranslateSpecialEntities();
 
         if (s != null) {
     		int len = s.length();
     		StringBuilder result = new StringBuilder(len);
 
     		for (int i = 0; i < len; i++) {
     			char ch = s.charAt(i);
 
     			if (ch == '&') {
     				if (i < len-2 && s.charAt(i+1) == '#') {
                         boolean isHex = Character.toLowerCase(s.charAt(i+2)) == 'x';
                         int charIndex = i + (isHex ? 3 : 2);
                         int radix = isHex ? 16 : 10;
                         String unicode = "";
                         while (charIndex < len) {
                             char currCh = s.charAt(charIndex);
                             if (currCh == ';') {
                                 break;
                             } else if (Utils.isValidInt(unicode + currCh, radix)) {
                                 unicode += currCh;
                                 charIndex++;
                             } else {
                                 charIndex--;
                                 break;
                             }
                         }
 
     					if (Utils.isValidInt(unicode, radix)) {
                             char unicodeChar = (char)Integer.parseInt(unicode, radix);
                             if ( !Utils.isValidXmlChar(unicodeChar) ) {
                                 i = charIndex;
                             } else if ( !Utils.isReservedXmlChar(unicodeChar) ) {
                                 result.append( recognizeUnicodeChars ? String.valueOf(unicodeChar) : "&#" + unicode + ";" );
                                 i = charIndex;
                             } else {
                                 i = charIndex;
                                 result.append("&#" + unicode + ";");
                             }
     					} else {
     						result.append(props.transResCharsToNCR ? "&#" + (int)'&' + ";" : "&");
     					}
     				} else {
                        // get following sequence of most 10 characters
                        String seq = s.substring(i, i+Math.min(10, len-i));
                         int semiIndex = seq.indexOf(';');
                         if (semiIndex > 0) {
                             String entityKey = seq.substring(1, semiIndex);
                             SpecialEntity entity = SpecialEntity.getEntity(entityKey);
                             if (entity != null) {
                                 result.append( translateSpecialEntities ? entity.getCharacter() : entity.getEscapedValue() );
                                 i += entityKey.length() + 1;
                                 continue;
                             }
                         }
 
                         String sub = s.substring(i);
                         boolean isReservedSeq = false;
                         for (Map.Entry<Character, String> entry: Utils.RESERVED_XML_CHARS.entrySet()) {
                             seq = entry.getValue();
                             if ( sub.startsWith(seq) ) {
                                 result.append( props.transResCharsToNCR ? "&#" + (int)entry.getKey() + ";" : seq );
                                 i += seq.length() - 1;
                                 isReservedSeq = true;
                                 break;
                             }
                         }
                         if (!isReservedSeq) {
                             result.append( props.transResCharsToNCR ? "&#" + (int)'&' + ";" : "&" );
                         }
//    					result.append("&amp;");
     				}
     			} else if (Utils.isReservedXmlChar(ch)) {
     				result.append( props.transResCharsToNCR ? "&#" + (int)ch + ";" : ch );
     			} else {
     				result.append(ch);
     			}
     		}
 
     		return result.toString();
     	}
 
     	return null;
     }
 
     protected void serializeOpenTag(TagNode tagNode, Writer writer, boolean newLine) throws IOException {
         String tagName = tagNode.getName();
 
         writer.write("<" + tagName);
         for (Map.Entry<String, String> entry: tagNode.getAttributes().entrySet()) {
             String attName = entry.getKey();
             if ( !props.isNamespacesAware() && ("xmlns".equals(attName) || attName.startsWith("xmlns:")) ) {
                 continue;
             }
             writer.write(" " + attName + "=\"" + escapeText(entry.getValue()) + "\"");
         }
 
         if ( isMinimizedTagSyntax(tagNode) ) {
             writer.write(" />");
             if (newLine) {
                 writer.write("\n");
             }
         } else {
             writer.write(">");
         }
     }
 
     protected void serializeEndTag(TagNode tagNode, Writer writer, boolean newLine) throws IOException {
         String tagName = tagNode.getName();
         writer.write( "</" + tagName + ">" );
         if (newLine) {
             writer.write("\n");
         }
     }
 
 }
