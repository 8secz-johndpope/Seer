 /* ***** BEGIN LICENSE BLOCK *****
  * Version: MPL 1.1/GPL 2.0/LGPL 2.1
  *
  * The contents of this file are subject to the Mozilla Public License Version
  * 1.1 (the "License"); you may not use this file except in compliance with
  * the License. You may obtain a copy of the License at
  * http://www.mozilla.org/MPL/
  *
  * Software distributed under the License is distributed on an "AS IS" basis,
  * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
  * for the specific language governing rights and limitations under the
  * License.
  *
  * The Original Code is HTML Parser C++ Translator code.
  *
  * The Initial Developer of the Original Code is
  * Mozilla Foundation.
  * Portions created by the Initial Developer are Copyright (C) 2008-2009
  * the Initial Developer. All Rights Reserved.
  *
  * Contributor(s):
  *   Henri Sivonen <hsivonen@iki.fi>
  *
  * Alternatively, the contents of this file may be used under the terms of
  * either the GNU General Public License Version 2 or later (the "GPL"), or
  * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
  * in which case the provisions of the GPL or the LGPL are applicable instead
  * of those above. If you wish to allow use of your version of this file only
  * under the terms of either the GPL or the LGPL, and not to allow others to
  * use your version of this file under the terms of the MPL, indicate your
  * decision by deleting the provisions above and replace them with the notice
  * and other provisions required by the GPL or the LGPL. If you do not delete
  * the provisions above, a recipient may use your version of this file under
  * the terms of any one of the MPL, the GPL or the LGPL.
  *
  * ***** END LICENSE BLOCK ***** */
 
 package nu.validator.htmlparser.cpptranslate;
 
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.OutputStreamWriter;
 import java.io.Writer;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
 
 public class CppTypes {
 
     /**
      * The license for the atom list written by this program.
      */
     private static final String ATOM_LICENSE = "/*\n"
             + " * Copyright (c) 2008-2010 Mozilla Foundation\n"
             + " *\n"
             + " * Permission is hereby granted, free of charge, to any person obtaining a \n"
             + " * copy of this software and associated documentation files (the \"Software\"), \n"
             + " * to deal in the Software without restriction, including without limitation \n"
             + " * the rights to use, copy, modify, merge, publish, distribute, sublicense, \n"
             + " * and/or sell copies of the Software, and to permit persons to whom the \n"
             + " * Software is furnished to do so, subject to the following conditions:\n"
             + " *\n"
             + " * The above copyright notice and this permission notice shall be included in \n"
             + " * all copies or substantial portions of the Software.\n"
             + " *\n"
             + " * THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR \n"
             + " * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, \n"
             + " * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL \n"
             + " * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER \n"
             + " * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING \n"
             + " * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER \n"
             + " * DEALINGS IN THE SOFTWARE.\n" + " */\n\n";
     
     private static Set<String> reservedWords = new HashSet<String>();
 
     static {
         reservedWords.add("small");
         reservedWords.add("for");
         reservedWords.add("false");
         reservedWords.add("true");
         reservedWords.add("default");
         reservedWords.add("class");
         reservedWords.add("switch");
         reservedWords.add("union");
         reservedWords.add("template");
         reservedWords.add("int");
         reservedWords.add("char");
         reservedWords.add("operator");
         reservedWords.add("or");
         reservedWords.add("and");
         reservedWords.add("not");
         reservedWords.add("xor");
         reservedWords.add("unicode");
     }
 
     private static final String[] TREE_BUILDER_INCLUDES = { "prtypes",
             "nsIAtom", "nsHtml5AtomTable", "nsITimer", "nsString",
             "nsINameSpaceManager", "nsIContent", "nsIDocument",
             "nsTraceRefcnt", "jArray", "nsHtml5DocumentMode",
             "nsHtml5ArrayCopy", "nsHtml5Parser", "nsHtml5Atoms",
             "nsHtml5ByteReadable", "nsHtml5TreeOperation",
             "nsHtml5PendingNotification", "nsHtml5StateSnapshot",
             "nsHtml5StackNode", "nsHtml5TreeOpExecutor", "nsHtml5StreamParser",
             "nsAHtml5TreeBuilderState" };
 
     private static final String[] INCLUDES = { "prtypes", "nsIAtom",
             "nsHtml5AtomTable", "nsString", "nsINameSpaceManager",
             "nsIContent", "nsIDocument", "nsTraceRefcnt", "jArray",
             "nsHtml5DocumentMode", "nsHtml5ArrayCopy",
             "nsHtml5NamedCharacters", "nsHtml5NamedCharactersAccel",
             "nsHtml5Atoms", "nsHtml5ByteReadable", "nsIUnicodeDecoder",
             "nsAHtml5TreeBuilderState", "nsHtml5Macros" };
 
     private static final String[] OTHER_DECLATIONS = {};
 
     private static final String[] TREE_BUILDER_OTHER_DECLATIONS = {};
 
     private static final String[] NAMED_CHARACTERS_INCLUDES = { "prtypes",
             "jArray", "nscore", "nsDebug", "prlog", "nsMemory" };
 
     private static final String[] FORWARD_DECLARATIONS = { "nsHtml5StreamParser", };
 
     private static final String[] CLASSES_THAT_NEED_SUPPLEMENT = {
             "MetaScanner", "TreeBuilder", "UTF16Buffer", };
 
     private final Map<String, String> atomMap = new HashMap<String, String>();
 
     private final Writer atomWriter;
 
     public CppTypes(File atomList) {
         if (atomList == null) {
             atomWriter = null;
         } else {
             try {
                 atomWriter = new OutputStreamWriter(new FileOutputStream(
                         atomList), "utf-8");
                 atomWriter.write(ATOM_LICENSE);
             } catch (IOException e) {
                 throw new RuntimeException(e);
             }
         }
     }
 
     public void finished() {
         try {
             if (atomWriter != null) {
                 atomWriter.flush();
                 atomWriter.close();
             }
         } catch (IOException e) {
             throw new RuntimeException(e);
         }
     }
 
     public String classPrefix() {
         return "nsHtml5";
     }
 
     public String booleanType() {
        return "PRBool";
     }
 
     public String byteType() {
         return "PRInt8";
     }
 
     public String charType() {
         return "PRUnichar";
     }
 
     /**
      * Only used for named characters.
      * @return
      */
     public String unsignedShortType() {
         return "PRUint16";
     }
 
     public String intType() {
         return "PRInt32";
     }
 
     public String stringType() {
         return "nsString*";
     }
 
     public String localType() {
         return "nsIAtom*";
     }
 
     public String prefixType() {
         return "nsIAtom*";
     }
 
     public String nsUriType() {
         return "PRInt32";
     }
 
     public String falseLiteral() {
        return "PR_FALSE";
     }
 
     public String trueLiteral() {
        return "PR_TRUE";
     }
 
     public String nullLiteral() {
         return "nsnull";
     }
 
     public String encodingDeclarationHandlerType() {
         return "nsHtml5StreamParser*";
     }
 
     public String nodeType() {
         return "nsIContent**";
     }
 
     public String xhtmlNamespaceLiteral() {
         return "kNameSpaceID_XHTML";
     }
 
     public String svgNamespaceLiteral() {
         return "kNameSpaceID_SVG";
     }
 
     public String xmlnsNamespaceLiteral() {
         return "kNameSpaceID_XMLNS";
     }
 
     public String xmlNamespaceLiteral() {
         return "kNameSpaceID_XML";
     }
 
     public String noNamespaceLiteral() {
         return "kNameSpaceID_None";
     }
 
     public String xlinkNamespaceLiteral() {
         return "kNameSpaceID_XLink";
     }
 
     public String mathmlNamespaceLiteral() {
         return "kNameSpaceID_MathML";
     }
 
     public String arrayTemplate() {
         return "jArray";
     }
 
     public String autoArrayTemplate() {
         return "autoJArray";
     }
 
     public String localForLiteral(String literal) {
         String atom = atomMap.get(literal);
         if (atom == null) {
             atom = createAtomName(literal);
             atomMap.put(literal, atom);
             if (atomWriter != null) {
                 try {
                     atomWriter.write("HTML5_ATOM(" + atom + ", \"" + literal
                             + "\")\n");
                 } catch (IOException e) {
                     throw new RuntimeException(e);
                 }
             }
         }
         return "nsHtml5Atoms::" + atom;
     }
 
     private String createAtomName(String literal) {
         String candidate = literal.replaceAll("[^a-zA-Z0-9_]", "_");
         if ("".equals(candidate)) {
             candidate = "emptystring";
         }
         while (atomMap.values().contains(candidate)
                 || reservedWords.contains(candidate)) {
             candidate = candidate + '_';
         }
         return candidate;
     }
 
     public String stringForLiteral(String literal) {
         return '"' + literal + '"';
     }
 
     public String staticArrayTemplate() {
         return "staticJArray";
     }
     
     public String newArrayCreator() {
         return "newJArray";
     }
 
     public String[] boilerplateIncludes(String javaClass) {
         if ("TreeBuilder".equals(javaClass)) {
             return TREE_BUILDER_INCLUDES;
         } else {
             return INCLUDES;
         }
     }
 
     public String[] boilerplateDeclarations(String javaClass) {
         if ("TreeBuilder".equals(javaClass)) {
             return TREE_BUILDER_OTHER_DECLATIONS;
         } else {
             return OTHER_DECLATIONS;
         }
     }
 
     public String[] namedCharactersIncludes() {
         return NAMED_CHARACTERS_INCLUDES;
     }
 
     public String[] boilerplateForwardDeclarations() {
         return FORWARD_DECLARATIONS;
     }
 
     public String documentModeHandlerType() {
         return "nsHtml5TreeBuilder*";
     }
 
     public String documentModeType() {
         return "nsHtml5DocumentMode";
     }
 
     public String arrayCopy() {
         return "nsHtml5ArrayCopy::arraycopy";
     }
 
     public String maxInteger() {
         return "PR_INT32_MAX";
     }
 
     public String constructorBoilerplate(String className) {
         return "MOZ_COUNT_CTOR(" + className + ");";
     }
 
     public String destructorBoilderplate(String className) {
         return "MOZ_COUNT_DTOR(" + className + ");";
     }
 
     public String literalType() {
         return "const char*";
     }
 
     public boolean hasSupplement(String javaClass) {
         return Arrays.binarySearch(CLASSES_THAT_NEED_SUPPLEMENT, javaClass) > -1;
     }
 
     public String internerType() {
         return "nsHtml5AtomTable*";
     }
 
     public String treeBuilderStateInterface() {
         return "nsAHtml5TreeBuilderState";
     }
 
     public String treeBuilderStateType() {
         return "nsAHtml5TreeBuilderState*";
     }
 
     public String arrayLengthMacro() {
         return "NS_ARRAY_LENGTH";
     }
 
     public String staticAssert() {
         return "PR_STATIC_ASSERT";
     }
 
     public String abortIfFalse() {
         return "NS_ABORT_IF_FALSE";
     }
 
     public String continueMacro() {
         return "NS_HTML5_CONTINUE";
     }
 
     public String breakMacro() {
         return "NS_HTML5_BREAK";
     }
 
     public String characterNameType() {
         return "nsHtml5CharacterName&";
     }
 
     public String characterNameTypeDeclaration() {
         return "nsHtml5CharacterName";
     }
 }
