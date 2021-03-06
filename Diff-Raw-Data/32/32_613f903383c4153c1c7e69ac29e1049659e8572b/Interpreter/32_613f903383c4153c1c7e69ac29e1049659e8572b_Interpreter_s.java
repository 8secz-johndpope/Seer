 /* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
  *
  * The contents of this file are subject to the Netscape Public
  * License Version 1.1 (the "License"); you may not use this file
  * except in compliance with the License. You may obtain a copy of
  * the License at http://www.mozilla.org/NPL/
  *
  * Software distributed under the License is distributed on an "AS
  * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
  * implied. See the License for the specific language governing
  * rights and limitations under the License.
  *
  * The Original Code is Rhino code, released
  * May 6, 1999.
  *
  * The Initial Developer of the Original Code is Netscape
  * Communications Corporation.  Portions created by Netscape are
  * Copyright (C) 1997-2000 Netscape Communications Corporation. All
  * Rights Reserved.
  *
  * Contributor(s):
  * Patrick Beard
  * Norris Boyd
  * Igor Bukanov
  * Roger Lawrence
  *
  * Alternatively, the contents of this file may be used under the
  * terms of the GNU Public License (the "GPL"), in which case the
  * provisions of the GPL are applicable instead of those above.
  * If you wish to allow use of your version of this file only
  * under the terms of the GPL and not to allow others to use your
  * version of this file under the NPL, indicate your decision by
  * deleting the provisions above and replace them with the notice
  * and other provisions required by the GPL.  If you do not delete
  * the provisions above, a recipient may use your version of this
  * file under either the NPL or the GPL.
  */
 
 package org.mozilla.javascript;
 
 import java.io.*;
 import java.util.Vector;
 import java.util.Enumeration;
 
 import org.mozilla.javascript.debug.*;
 
 public class Interpreter extends LabelTable {
 
 // Additional interpreter-specific codes
     static final int
     // To indicating a line number change in icodes.
         LINE_ICODE                      = TokenStream.LAST_TOKEN + 1,
         SOURCEFILE_ICODE                = TokenStream.LAST_TOKEN + 2,
 
     // To place breakpoints
         BREAKPOINT_ICODE                = TokenStream.LAST_TOKEN + 3;
 
     private static final int
     // To store shorts and ints inline
         SHORTNUMBER_ICODE               = TokenStream.LAST_TOKEN + 4,
         INTNUMBER_ICODE                 = TokenStream.LAST_TOKEN + 5,
 
     // To return undefined value
         RETURN_UNDEF_ICODE              = TokenStream.LAST_TOKEN + 6,
     // Last icode
         END_ICODE                       = TokenStream.LAST_TOKEN + 7;
 
     public IRFactory createIRFactory(TokenStream ts,
                                      ClassNameHelper nameHelper, Scriptable scope)
     {
         return new IRFactory(ts, scope);
     }
 
     public Node transform(Node tree, TokenStream ts, Scriptable scope) {
         return (new NodeTransformer()).transform(tree, null, ts, scope);
     }
 
     public Object compile(Context cx, Scriptable scope, Node tree,
                           Object securityDomain,
                           SecuritySupport securitySupport,
                           ClassNameHelper nameHelper)
         throws IOException
     {
         version = cx.getLanguageVersion();
         itsData = new InterpreterData(securityDomain,
                     cx.hasCompileFunctionsWithDynamicScope(), false);
         if (tree instanceof FunctionNode) {
             itsData.isFunction = true;
             FunctionNode f = (FunctionNode) tree;
             InterpretedFunction result =
                 generateFunctionICode(cx, scope, f, securityDomain);
             result.itsData.itsFunctionType = f.getFunctionType();
             createFunctionObject(result, scope);
             return result;
         }
         return generateScriptICode(cx, scope, tree, securityDomain);
     }
 
     private void generateICodeFromTree(Node tree,
                                        VariableTable varTable,
                                        boolean needsActivation,
                                        Object securityDomain)
     {
         int theICodeTop = 0;
         itsVariableTable = varTable;
         itsData.itsNeedsActivation = needsActivation;
         theICodeTop = generateICode(tree, theICodeTop);
         for (int i = 0; i < itsLabelTableTop; i++) {
             itsLabelTable[i].fixGotos(itsData.itsICode);
         }
         if (!itsData.isFunction) {
             // function should always have return statement
             theICodeTop = addByte(END_ICODE, theICodeTop);
         }
         itsData.itsICodeTop = theICodeTop;
         if (itsData.itsICode.length != theICodeTop) {
             // Make itsData.itsICode length exactly theICodeTop to save memory
             // and catch bugs with jumps beyound icode as early as possible
             byte[] tmp = new byte[theICodeTop];
             System.arraycopy(itsData.itsICode, 0, tmp, 0, theICodeTop);
             itsData.itsICode = tmp;
         }
     }
 
     private Object[] generateRegExpLiterals(Context cx,
                                             Scriptable scope,
                                             Vector regexps)
     {
         Object[] result = new Object[regexps.size()];
         RegExpProxy rep = cx.getRegExpProxy();
         for (int i = 0; i < regexps.size(); i++) {
             Node regexp = (Node) regexps.elementAt(i);
             Node left = regexp.getFirstChild();
             Node right = regexp.getLastChild();
             result[i] = rep.newRegExp(cx, scope, left.getString(),
                                 (left != right) ? right.getString() : null, false);
             regexp.putIntProp(Node.REGEXP_PROP, i);
         }
         return result;
     }
 
     private InterpretedScript generateScriptICode(Context cx,
                                                   Scriptable scope,
                                                   Node tree,
                                                   Object securityDomain)
     {
         itsSourceFile = (String) tree.getProp(Node.SOURCENAME_PROP);
         itsData.itsSourceFile = itsSourceFile;
         itsFunctionList = (Vector) tree.getProp(Node.FUNCTION_PROP);
         debugSource = (StringBuffer) tree.getProp(Node.DEBUGSOURCE_PROP);
         if (itsFunctionList != null)
             generateNestedFunctions(scope, cx, securityDomain);
         Object[] regExpLiterals = null;
         Vector regexps = (Vector)tree.getProp(Node.REGEXP_PROP);
         if (regexps != null)
             regExpLiterals = generateRegExpLiterals(cx, scope, regexps);
 
         VariableTable varTable = (VariableTable)tree.getProp(Node.VARS_PROP);
         // The default is not to generate debug information
         boolean activationNeeded = cx.isGeneratingDebugChanged() &&
                                    cx.isGeneratingDebug();
         generateICodeFromTree(tree, varTable, activationNeeded, securityDomain);
         itsData.itsNestedFunctions = itsNestedFunctions;
         itsData.itsRegExpLiterals = regExpLiterals;
         if (Context.printICode) dumpICode(itsData);
 
         String[] argNames = itsVariableTable.getAllNames();
         short argCount = (short)itsVariableTable.getParameterCount();
         InterpretedScript
             result = new InterpretedScript(cx, itsData, argNames, argCount);
         if (cx.debugger != null) {
             cx.debugger.handleCompilationDone(cx, result, debugSource);
         }
         return result;
     }
 
     private void generateNestedFunctions(Scriptable scope,
                                          Context cx,
                                          Object securityDomain)
     {
         itsNestedFunctions = new InterpretedFunction[itsFunctionList.size()];
         for (short i = 0; i < itsFunctionList.size(); i++) {
             FunctionNode def = (FunctionNode)itsFunctionList.elementAt(i);
             Interpreter jsi = new Interpreter();
             jsi.itsSourceFile = itsSourceFile;
             jsi.itsData = new InterpreterData(securityDomain,
                             cx.hasCompileFunctionsWithDynamicScope(),
                             def.getCheckThis());
             jsi.itsData.isFunction = true;
             jsi.itsData.itsFunctionType = def.getFunctionType();
             jsi.itsInFunctionFlag = true;
             jsi.debugSource = debugSource;
             itsNestedFunctions[i] = jsi.generateFunctionICode(cx, scope, def,
                                                               securityDomain);
             def.putIntProp(Node.FUNCTION_PROP, i);
         }
     }
 
     private InterpretedFunction
     generateFunctionICode(Context cx, Scriptable scope,
                           FunctionNode theFunction, Object securityDomain)
     {
         itsFunctionList = (Vector) theFunction.getProp(Node.FUNCTION_PROP);
         if (itsFunctionList != null)
             generateNestedFunctions(scope, cx, securityDomain);
         Object[] regExpLiterals = null;
         Vector regexps = (Vector)theFunction.getProp(Node.REGEXP_PROP);
         if (regexps != null)
             regExpLiterals = generateRegExpLiterals(cx, scope, regexps);
 
         VariableTable varTable = theFunction.getVariableTable();
         boolean needsActivation = theFunction.requiresActivation() ||
                                   (cx.isGeneratingDebugChanged() &&
                                    cx.isGeneratingDebug());
         generateICodeFromTree(theFunction.getLastChild(),
                               varTable, needsActivation,
                               securityDomain);
 
         itsData.itsName = theFunction.getFunctionName();
         itsData.itsSourceFile = (String) theFunction.getProp(
                                     Node.SOURCENAME_PROP);
         itsData.itsSource = (String)theFunction.getProp(Node.SOURCE_PROP);
         itsData.itsNestedFunctions = itsNestedFunctions;
         itsData.itsRegExpLiterals = regExpLiterals;
         if (Context.printICode) dumpICode(itsData);
 
         String[] argNames = itsVariableTable.getAllNames();
         short argCount = (short)itsVariableTable.getParameterCount();
         InterpretedFunction
             result = new InterpretedFunction(cx, itsData, argNames, argCount);
         if (cx.debugger != null) {
             cx.debugger.handleCompilationDone(cx, result, debugSource);
         }
         return result;
     }
 
     boolean itsInFunctionFlag;
     Vector itsFunctionList;
 
     InterpreterData itsData;
     VariableTable itsVariableTable;
     int itsTryDepth = 0;
     int itsStackDepth = 0;
     String itsSourceFile;
     int itsLineNumber = 0;
     InterpretedFunction[] itsNestedFunctions = null;
 
     private int updateLineNumber(Node node, int iCodeTop)
     {
         Object datum = node.getDatum();
         if (datum == null || !(datum instanceof Number))
             return iCodeTop;
         short lineNumber = ((Number) datum).shortValue();
         if (lineNumber != itsLineNumber) {
             itsLineNumber = lineNumber;
             if (itsData.itsLineNumberTable == null &&
                 Context.getCurrentContext().isGeneratingDebug())
             {
                 itsData.itsLineNumberTable = new UintMap();
             }
             if (lineNumber > 0 && itsData.itsLineNumberTable != null) {
                 itsData.itsLineNumberTable.put(lineNumber, iCodeTop);
             }
             iCodeTop = addByte(LINE_ICODE, iCodeTop);
             iCodeTop = addShort(lineNumber, iCodeTop);
         }
 
         return iCodeTop;
     }
 
     private void badTree(Node node)
     {
         try {
             out = new PrintWriter(new FileOutputStream("icode.txt", true));
             out.println("Un-handled node : " + node.toString());
             out.close();
         }
         catch (IOException x) {}
         throw new RuntimeException("Un-handled node : "
                                         + node.toString());
     }
 
     private int generateICode(Node node, int iCodeTop) {
         int type = node.getType();
         Node child = node.getFirstChild();
         Node firstChild = child;
         switch (type) {
 
             case TokenStream.FUNCTION : {
                     iCodeTop = addByte(TokenStream.CLOSURE, iCodeTop);
                     Node fn = (Node) node.getProp(Node.FUNCTION_PROP);
                     int index = fn.getExistingIntProp(Node.FUNCTION_PROP);
                     iCodeTop = addByte(index >> 8, iCodeTop);
                     iCodeTop = addByte(index & 0xff, iCodeTop);
                     itsStackDepth++;
                     if (itsStackDepth > itsData.itsMaxStack)
                         itsData.itsMaxStack = itsStackDepth;
                 }
                 break;
 
             case TokenStream.SCRIPT :
                 iCodeTop = updateLineNumber(node, iCodeTop);
                 while (child != null) {
                     if (child.getType() != TokenStream.FUNCTION)
                         iCodeTop = generateICode(child, iCodeTop);
                     child = child.getNextSibling();
                 }
                 break;
 
             case TokenStream.CASE :
                 iCodeTop = updateLineNumber(node, iCodeTop);
                 child = child.getNextSibling();
                 while (child != null) {
                     iCodeTop = generateICode(child, iCodeTop);
                     child = child.getNextSibling();
                 }
                 break;
 
             case TokenStream.LABEL :
             case TokenStream.WITH :
             case TokenStream.LOOP :
             case TokenStream.DEFAULT :
             case TokenStream.BLOCK :
             case TokenStream.VOID :
             case TokenStream.NOP :
                 iCodeTop = updateLineNumber(node, iCodeTop);
                 while (child != null) {
                     iCodeTop = generateICode(child, iCodeTop);
                     child = child.getNextSibling();
                 }
                 break;
 
             case TokenStream.COMMA :
                 iCodeTop = generateICode(child, iCodeTop);
                 iCodeTop = addByte(TokenStream.POP, iCodeTop);
                 itsStackDepth--;
                 child = child.getNextSibling();
                 iCodeTop = generateICode(child, iCodeTop);
                 break;
 
             case TokenStream.SWITCH : {
                     iCodeTop = updateLineNumber(node, iCodeTop);
                     iCodeTop = generateICode(child, iCodeTop);
                     int theLocalSlot = itsData.itsMaxLocals++;
                     iCodeTop = addByte(TokenStream.NEWTEMP, iCodeTop);
                     iCodeTop = addByte(theLocalSlot, iCodeTop);
                     iCodeTop = addByte(TokenStream.POP, iCodeTop);
                     itsStackDepth--;
          /*
             reminder - below we construct new GOTO nodes that aren't
             linked into the tree just for the purpose of having a node
             to pass to the addGoto routine. (Parallels codegen here).
             Seems unnecessary.
          */
                     Vector cases = (Vector) node.getProp(Node.CASES_PROP);
                     for (int i = 0; i < cases.size(); i++) {
                         Node thisCase = (Node)cases.elementAt(i);
                         Node first = thisCase.getFirstChild();
                         // the case expression is the firstmost child
                         // the rest will be generated when the case
                         // statements are encountered as siblings of
                         // the switch statement.
                         iCodeTop = generateICode(first, iCodeTop);
                         iCodeTop = addByte(TokenStream.USETEMP, iCodeTop);
                         itsStackDepth++;
                         if (itsStackDepth > itsData.itsMaxStack)
                             itsData.itsMaxStack = itsStackDepth;
                         iCodeTop = addByte(theLocalSlot, iCodeTop);
                         iCodeTop = addByte(TokenStream.SHEQ, iCodeTop);
                         Node target = new Node(TokenStream.TARGET);
                         thisCase.addChildAfter(target, first);
                         Node branch = new Node(TokenStream.IFEQ);
                         branch.putProp(Node.TARGET_PROP, target);
                         iCodeTop = addGoto(branch, TokenStream.IFEQ,
                                            iCodeTop);
                         itsStackDepth--;
                     }
 
                     Node defaultNode = (Node) node.getProp(Node.DEFAULT_PROP);
                     if (defaultNode != null) {
                         Node defaultTarget = new Node(TokenStream.TARGET);
                         defaultNode.getFirstChild().addChildToFront(defaultTarget);
                         Node branch = new Node(TokenStream.GOTO);
                         branch.putProp(Node.TARGET_PROP, defaultTarget);
                         iCodeTop = addGoto(branch, TokenStream.GOTO,
                                                             iCodeTop);
                     }
 
                     Node breakTarget = (Node) node.getProp(Node.BREAK_PROP);
                     Node branch = new Node(TokenStream.GOTO);
                     branch.putProp(Node.TARGET_PROP, breakTarget);
                     iCodeTop = addGoto(branch, TokenStream.GOTO,
                                        iCodeTop);
                 }
                 break;
 
             case TokenStream.TARGET : {
                     int label = node.getIntProp(Node.LABEL_PROP, -1);
                     if (label == -1) {
                         label = acquireLabel();
                         node.putIntProp(Node.LABEL_PROP, label);
                     }
                     markLabel(label, iCodeTop);
                     // if this target has a FINALLY_PROP, it is a JSR target
                     // and so has a PC value on the top of the stack
                     if (node.getProp(Node.FINALLY_PROP) != null) {
                         itsStackDepth = 1;
                         if (itsStackDepth > itsData.itsMaxStack)
                             itsData.itsMaxStack = itsStackDepth;
                     }
                 }
                 break;
 
             case TokenStream.EQOP :
             case TokenStream.RELOP : {
                     iCodeTop = generateICode(child, iCodeTop);
                     child = child.getNextSibling();
                     iCodeTop = generateICode(child, iCodeTop);
                     int op = node.getInt();
                     if (version == Context.VERSION_1_2) {
                         if (op == TokenStream.EQ)
                             op = TokenStream.SHEQ;
                         else if (op == TokenStream.NE)
                             op = TokenStream.SHNE;
                     }
                     iCodeTop = addByte(op, iCodeTop);
                     itsStackDepth--;
                 }
                 break;
 
             case TokenStream.NEW :
             case TokenStream.CALL : {
                     if (itsSourceFile != null && (itsData.itsSourceFile == null || ! itsSourceFile.equals(itsData.itsSourceFile)))
                         itsData.itsSourceFile = itsSourceFile;
                     iCodeTop = addByte(SOURCEFILE_ICODE, iCodeTop);
 
                     int childCount = 0;
                     short nameIndex = -1;
                     while (child != null) {
                         iCodeTop = generateICode(child, iCodeTop);
                         if (nameIndex == -1) {
                             if (child.getType() == TokenStream.NAME)
                                 nameIndex = (short)(itsData.itsStringTableIndex - 1);
                             else if (child.getType() == TokenStream.GETPROP)
                                 nameIndex = (short)(itsData.itsStringTableIndex - 1);
                         }
                         child = child.getNextSibling();
                         childCount++;
                     }
                     if (node.getProp(Node.SPECIALCALL_PROP) != null) {
                         // embed line number and source filename
                         iCodeTop = addByte(TokenStream.CALLSPECIAL, iCodeTop);
                         iCodeTop = addShort(itsLineNumber, iCodeTop);
                         iCodeTop = addString(itsSourceFile, iCodeTop);
                     } else {
                         iCodeTop = addByte(type, iCodeTop);
                         iCodeTop = addShort(nameIndex, iCodeTop);
                     }
 
                     itsStackDepth -= (childCount - 1);  // always a result value
                     // subtract from child count to account for [thisObj &] fun
                     if (type == TokenStream.NEW)
                         childCount -= 1;
                     else
                         childCount -= 2;
                     iCodeTop = addShort(childCount, iCodeTop);
                     if (childCount > itsData.itsMaxArgs)
                         itsData.itsMaxArgs = childCount;
 
                     iCodeTop = addByte(SOURCEFILE_ICODE, iCodeTop);
                 }
                 break;
 
             case TokenStream.NEWLOCAL :
             case TokenStream.NEWTEMP : {
                     iCodeTop = generateICode(child, iCodeTop);
                     iCodeTop = addByte(TokenStream.NEWTEMP, iCodeTop);
                     iCodeTop = addLocalRef(node, iCodeTop);
                 }
                 break;
 
             case TokenStream.USELOCAL : {
                     if (node.getProp(Node.TARGET_PROP) != null)
                         iCodeTop = addByte(TokenStream.RETSUB, iCodeTop);
                     else {
                         iCodeTop = addByte(TokenStream.USETEMP, iCodeTop);
                         itsStackDepth++;
                         if (itsStackDepth > itsData.itsMaxStack)
                             itsData.itsMaxStack = itsStackDepth;
                     }
                     Node temp = (Node) node.getProp(Node.LOCAL_PROP);
                     iCodeTop = addLocalRef(temp, iCodeTop);
                 }
                 break;
 
             case TokenStream.USETEMP : {
                     iCodeTop = addByte(TokenStream.USETEMP, iCodeTop);
                     Node temp = (Node) node.getProp(Node.TEMP_PROP);
                     iCodeTop = addLocalRef(temp, iCodeTop);
                     itsStackDepth++;
                     if (itsStackDepth > itsData.itsMaxStack)
                         itsData.itsMaxStack = itsStackDepth;
                 }
                 break;
 
             case TokenStream.IFEQ :
             case TokenStream.IFNE :
                 iCodeTop = generateICode(child, iCodeTop);
                 itsStackDepth--;    // after the conditional GOTO, really
                     // fall thru...
             case TokenStream.GOTO :
                 iCodeTop = addGoto(node, (byte) type, iCodeTop);
                 break;
 
             case TokenStream.JSR : {
                 /*
                     mark the target with a FINALLY_PROP to indicate
                     that it will have an incoming PC value on the top
                     of the stack.
                     !!!
                     This only works if the target follows the JSR
                     in the tree.
                     !!!
                 */
                     Node target = (Node)(node.getProp(Node.TARGET_PROP));
                     target.putProp(Node.FINALLY_PROP, node);
                     // Bug 115717 is due to adding a GOSUB here before
                     // we insert an ENDTRY. I'm not sure of the best way
                     // to fix this; perhaps we need to maintain a stack
                     // of pending trys and have some knowledge of how
                     // many trys we need to close when we perform a
                     // GOTO or GOSUB.
                     iCodeTop = addGoto(node, TokenStream.GOSUB, iCodeTop);
                 }
                 break;
 
             case TokenStream.AND : {
                     iCodeTop = generateICode(child, iCodeTop);
                     iCodeTop = addByte(TokenStream.DUP, iCodeTop);
                     itsStackDepth++;
                     if (itsStackDepth > itsData.itsMaxStack)
                         itsData.itsMaxStack = itsStackDepth;
                     int falseTarget = acquireLabel();
                     iCodeTop = addGoto(falseTarget, TokenStream.IFNE,
                                                     iCodeTop);
                     iCodeTop = addByte(TokenStream.POP, iCodeTop);
                     itsStackDepth--;
                     child = child.getNextSibling();
                     iCodeTop = generateICode(child, iCodeTop);
                     markLabel(falseTarget, iCodeTop);
                 }
                 break;
 
             case TokenStream.OR : {
                     iCodeTop = generateICode(child, iCodeTop);
                     iCodeTop = addByte(TokenStream.DUP, iCodeTop);
                     itsStackDepth++;
                     if (itsStackDepth > itsData.itsMaxStack)
                         itsData.itsMaxStack = itsStackDepth;
                     int trueTarget = acquireLabel();
                     iCodeTop = addGoto(trueTarget, TokenStream.IFEQ,
                                        iCodeTop);
                     iCodeTop = addByte(TokenStream.POP, iCodeTop);
                     itsStackDepth--;
                     child = child.getNextSibling();
                     iCodeTop = generateICode(child, iCodeTop);
                     markLabel(trueTarget, iCodeTop);
                 }
                 break;
 
             case TokenStream.GETPROP : {
                     iCodeTop = generateICode(child, iCodeTop);
                     String s = (String) node.getProp(Node.SPECIAL_PROP_PROP);
                     if (s != null) {
                         if (s.equals("__proto__"))
                             iCodeTop = addByte(TokenStream.GETPROTO, iCodeTop);
                         else
                             if (s.equals("__parent__"))
                                 iCodeTop = addByte(TokenStream.GETSCOPEPARENT, iCodeTop);
                             else
                                 badTree(node);
                     }
                     else {
                         child = child.getNextSibling();
                         iCodeTop = generateICode(child, iCodeTop);
                         iCodeTop = addByte(TokenStream.GETPROP, iCodeTop);
                         itsStackDepth--;
                     }
                 }
                 break;
 
             case TokenStream.DELPROP :
             case TokenStream.BITAND :
             case TokenStream.BITOR :
             case TokenStream.BITXOR :
             case TokenStream.LSH :
             case TokenStream.RSH :
             case TokenStream.URSH :
             case TokenStream.ADD :
             case TokenStream.SUB :
             case TokenStream.MOD :
             case TokenStream.DIV :
             case TokenStream.MUL :
             case TokenStream.GETELEM :
                 iCodeTop = generateICode(child, iCodeTop);
                 child = child.getNextSibling();
                 iCodeTop = generateICode(child, iCodeTop);
                 iCodeTop = addByte(type, iCodeTop);
                 itsStackDepth--;
                 break;
 
             case TokenStream.CONVERT : {
                     iCodeTop = generateICode(child, iCodeTop);
                     Object toType = node.getProp(Node.TYPE_PROP);
                     if (toType == ScriptRuntime.NumberClass)
                         iCodeTop = addByte(TokenStream.POS, iCodeTop);
                     else
                         badTree(node);
                 }
                 break;
 
             case TokenStream.UNARYOP :
                 iCodeTop = generateICode(child, iCodeTop);
                 switch (node.getInt()) {
                     case TokenStream.VOID :
                         iCodeTop = addByte(TokenStream.POP, iCodeTop);
                         iCodeTop = addByte(TokenStream.UNDEFINED, iCodeTop);
                         break;
                     case TokenStream.NOT : {
                             int trueTarget = acquireLabel();
                             int beyond = acquireLabel();
                             iCodeTop = addGoto(trueTarget, TokenStream.IFEQ,
                                                         iCodeTop);
                             iCodeTop = addByte(TokenStream.TRUE, iCodeTop);
                             iCodeTop = addGoto(beyond, TokenStream.GOTO,
                                                         iCodeTop);
                             markLabel(trueTarget, iCodeTop);
                             iCodeTop = addByte(TokenStream.FALSE, iCodeTop);
                             markLabel(beyond, iCodeTop);
                         }
                         break;
                     case TokenStream.BITNOT :
                         iCodeTop = addByte(TokenStream.BITNOT, iCodeTop);
                         break;
                     case TokenStream.TYPEOF :
                         iCodeTop = addByte(TokenStream.TYPEOF, iCodeTop);
                         break;
                     case TokenStream.SUB :
                         iCodeTop = addByte(TokenStream.NEG, iCodeTop);
                         break;
                     case TokenStream.ADD :
                         iCodeTop = addByte(TokenStream.POS, iCodeTop);
                         break;
                     default:
                         badTree(node);
                         break;
                 }
                 break;
 
             case TokenStream.SETPROP : {
                     iCodeTop = generateICode(child, iCodeTop);
                     child = child.getNextSibling();
                     iCodeTop = generateICode(child, iCodeTop);
                     String s = (String) node.getProp(Node.SPECIAL_PROP_PROP);
                     if (s != null) {
                         if (s.equals("__proto__"))
                             iCodeTop = addByte(TokenStream.SETPROTO, iCodeTop);
                         else
                             if (s.equals("__parent__"))
                                 iCodeTop = addByte(TokenStream.SETPARENT, iCodeTop);
                             else
                                 badTree(node);
                     }
                     else {
                         child = child.getNextSibling();
                         iCodeTop = generateICode(child, iCodeTop);
                         iCodeTop = addByte(TokenStream.SETPROP, iCodeTop);
                         itsStackDepth -= 2;
                     }
                 }
                 break;
 
             case TokenStream.SETELEM :
                 iCodeTop = generateICode(child, iCodeTop);
                 child = child.getNextSibling();
                 iCodeTop = generateICode(child, iCodeTop);
                 child = child.getNextSibling();
                 iCodeTop = generateICode(child, iCodeTop);
                 iCodeTop = addByte(type, iCodeTop);
                 itsStackDepth -= 2;
                 break;
 
             case TokenStream.SETNAME :
                 iCodeTop = generateICode(child, iCodeTop);
                 child = child.getNextSibling();
                 iCodeTop = generateICode(child, iCodeTop);
                 iCodeTop = addByte(TokenStream.SETNAME, iCodeTop);
                 iCodeTop = addString(firstChild.getString(), iCodeTop);
                 itsStackDepth--;
                 break;
 
             case TokenStream.TYPEOF : {
                     String name = node.getString();
                     int index = -1;
                     // use typeofname if an activation frame exists
                     // since the vars all exist there instead of in jregs
                     if (itsInFunctionFlag && !itsData.itsNeedsActivation)
                         index = itsVariableTable.getOrdinal(name);
                     if (index == -1) {
                         iCodeTop = addByte(TokenStream.TYPEOFNAME, iCodeTop);
                         iCodeTop = addString(name, iCodeTop);
                     }
                     else {
                         iCodeTop = addByte(TokenStream.GETVAR, iCodeTop);
                         iCodeTop = addByte(index, iCodeTop);
                         iCodeTop = addByte(TokenStream.TYPEOF, iCodeTop);
                     }
                     itsStackDepth++;
                     if (itsStackDepth > itsData.itsMaxStack)
                         itsData.itsMaxStack = itsStackDepth;
                 }
                 break;
 
             case TokenStream.PARENT :
                 iCodeTop = generateICode(child, iCodeTop);
                 iCodeTop = addByte(TokenStream.GETPARENT, iCodeTop);
                 break;
 
             case TokenStream.GETBASE :
             case TokenStream.BINDNAME :
             case TokenStream.NAME :
             case TokenStream.STRING :
                 iCodeTop = addByte(type, iCodeTop);
                 iCodeTop = addString(node.getString(), iCodeTop);
                 itsStackDepth++;
                 if (itsStackDepth > itsData.itsMaxStack)
                     itsData.itsMaxStack = itsStackDepth;
                 break;
 
             case TokenStream.INC :
             case TokenStream.DEC : {
                     int childType = child.getType();
                     switch (childType) {
                         case TokenStream.GETVAR : {
                                 String name = child.getString();
                                 if (itsData.itsNeedsActivation) {
                                     iCodeTop = addByte(TokenStream.SCOPE, iCodeTop);
                                     iCodeTop = addByte(TokenStream.STRING, iCodeTop);
                                     iCodeTop = addString(name, iCodeTop);
                                     itsStackDepth += 2;
                                     if (itsStackDepth > itsData.itsMaxStack)
                                         itsData.itsMaxStack = itsStackDepth;
                                     iCodeTop = addByte(type == TokenStream.INC
                                                        ? TokenStream.PROPINC
                                                        : TokenStream.PROPDEC,
                                                        iCodeTop);
                                     itsStackDepth--;
                                 }
                                 else {
                                     iCodeTop = addByte(type == TokenStream.INC
                                                        ? TokenStream.VARINC
                                                        : TokenStream.VARDEC,
                                                        iCodeTop);
                                     int i = itsVariableTable.getOrdinal(name);
                                     iCodeTop = addByte(i, iCodeTop);
                                     itsStackDepth++;
                                     if (itsStackDepth > itsData.itsMaxStack)
                                         itsData.itsMaxStack = itsStackDepth;
                                 }
                             }
                             break;
                         case TokenStream.GETPROP :
                         case TokenStream.GETELEM : {
                                 Node getPropChild = child.getFirstChild();
                                 iCodeTop = generateICode(getPropChild,
                                                               iCodeTop);
                                 getPropChild = getPropChild.getNextSibling();
                                 iCodeTop = generateICode(getPropChild,
                                                               iCodeTop);
                                 if (childType == TokenStream.GETPROP)
                                     iCodeTop = addByte(type == TokenStream.INC
                                                        ? TokenStream.PROPINC
                                                        : TokenStream.PROPDEC,
                                                        iCodeTop);
                                 else
                                     iCodeTop = addByte(type == TokenStream.INC
                                                        ? TokenStream.ELEMINC
                                                        : TokenStream.ELEMDEC,
                                                        iCodeTop);
                                 itsStackDepth--;
                             }
                             break;
                         default : {
                                 iCodeTop = addByte(type == TokenStream.INC
                                                    ? TokenStream.NAMEINC
                                                    : TokenStream.NAMEDEC,
                                                    iCodeTop);
                                 iCodeTop = addString(child.getString(),
                                                             iCodeTop);
                                 itsStackDepth++;
                                 if (itsStackDepth > itsData.itsMaxStack)
                                     itsData.itsMaxStack = itsStackDepth;
                             }
                             break;
                     }
                 }
                 break;
 
             case TokenStream.NUMBER : {
                 double num = node.getDouble();
                 int inum = (int)num;
                 if (inum == num) {
                     if (inum == 0) {
                         iCodeTop = addByte(TokenStream.ZERO, iCodeTop);
                     }
                     else if (inum == 1) {
                         iCodeTop = addByte(TokenStream.ONE, iCodeTop);
                     }
                     else if ((short)inum == inum) {
                         iCodeTop = addByte(SHORTNUMBER_ICODE, iCodeTop);
                         iCodeTop = addShort(inum, iCodeTop);
                     }
                     else {
                         iCodeTop = addByte(INTNUMBER_ICODE, iCodeTop);
                         iCodeTop = addInt(inum, iCodeTop);
                     }
                 }
                 else {
                     iCodeTop = addByte(TokenStream.NUMBER, iCodeTop);
                     iCodeTop = addDouble(num, iCodeTop);
                 }
                 itsStackDepth++;
                 if (itsStackDepth > itsData.itsMaxStack)
                     itsData.itsMaxStack = itsStackDepth;
                 break;
             }
 
             case TokenStream.POP :
             case TokenStream.POPV :
                 iCodeTop = updateLineNumber(node, iCodeTop);
             case TokenStream.ENTERWITH :
                 iCodeTop = generateICode(child, iCodeTop);
                 iCodeTop = addByte(type, iCodeTop);
                 itsStackDepth--;
                 break;
 
             case TokenStream.GETTHIS :
                 iCodeTop = generateICode(child, iCodeTop);
                 iCodeTop = addByte(type, iCodeTop);
                 break;
 
             case TokenStream.NEWSCOPE :
                 iCodeTop = addByte(type, iCodeTop);
                 itsStackDepth++;
                 if (itsStackDepth > itsData.itsMaxStack)
                     itsData.itsMaxStack = itsStackDepth;
                 break;
 
             case TokenStream.LEAVEWITH :
                 iCodeTop = addByte(type, iCodeTop);
                 break;
 
             case TokenStream.TRY : {
                     itsTryDepth++;
                     if (itsTryDepth > itsData.itsMaxTryDepth)
                         itsData.itsMaxTryDepth = itsTryDepth;
                     Node catchTarget = (Node)node.getProp(Node.TARGET_PROP);
                     Node finallyTarget = (Node)node.getProp(Node.FINALLY_PROP);
                     if (catchTarget == null) {
                         iCodeTop = addByte(TokenStream.TRY, iCodeTop);
                         iCodeTop = addShort(0, iCodeTop);
                     }
                     else
                         iCodeTop =
                             addGoto(node, TokenStream.TRY, iCodeTop);
                     int finallyHandler = 0;
                     if (finallyTarget != null) {
                         finallyHandler = acquireLabel();
                         int theLabel = finallyHandler & 0x7FFFFFFF;
                         itsLabelTable[theLabel].addFixup(iCodeTop);
                     }
                     iCodeTop = addShort(0, iCodeTop);
 
                     Node lastChild = null;
                     /*
                         when we encounter the child of the catchTarget, we
                         set the stackDepth to 1 to account for the incoming
                         exception object.
                     */
                     boolean insertedEndTry = false;
                     while (child != null) {
                         if (catchTarget != null && lastChild == catchTarget) {
                             itsStackDepth = 1;
                             if (itsStackDepth > itsData.itsMaxStack)
                                 itsData.itsMaxStack = itsStackDepth;
                         }
                         /*
                             When the following child is the catchTarget
                             (or the finallyTarget if there are no catches),
                             the current child is the goto at the end of
                             the try statemets, we need to emit the endtry
                             before that goto.
                         */
                         Node nextSibling = child.getNextSibling();
                         if (!insertedEndTry && nextSibling != null &&
                             (nextSibling == catchTarget ||
                              nextSibling == finallyTarget))
                         {
                             iCodeTop = addByte(TokenStream.ENDTRY,
                                                iCodeTop);
                             insertedEndTry = true;
                         }
                         iCodeTop = generateICode(child, iCodeTop);
                         lastChild = child;
                         child = child.getNextSibling();
                     }
                     itsStackDepth = 0;
                     if (finallyTarget != null) {
                         // normal flow goes around the finally handler stublet
                         int skippy = acquireLabel();
                         iCodeTop =
                             addGoto(skippy, TokenStream.GOTO, iCodeTop);
                         // on entry the stack will have the exception object
                         markLabel(finallyHandler, iCodeTop);
                         itsStackDepth = 1;
                         if (itsStackDepth > itsData.itsMaxStack)
                             itsData.itsMaxStack = itsStackDepth;
                         int theLocalSlot = itsData.itsMaxLocals++;
                         iCodeTop = addByte(TokenStream.NEWTEMP, iCodeTop);
                         iCodeTop = addByte(theLocalSlot, iCodeTop);
                         iCodeTop = addByte(TokenStream.POP, iCodeTop);
                         int finallyLabel
                            = finallyTarget.getExistingIntProp(Node.LABEL_PROP);
                         iCodeTop = addGoto(finallyLabel,
                                          TokenStream.GOSUB, iCodeTop);
                         iCodeTop = addByte(TokenStream.USETEMP, iCodeTop);
                         iCodeTop = addByte(theLocalSlot, iCodeTop);
                         iCodeTop = addByte(TokenStream.JTHROW, iCodeTop);
                         itsStackDepth = 0;
                         markLabel(skippy, iCodeTop);
                     }
                     itsTryDepth--;
                 }
                 break;
 
             case TokenStream.THROW :
                 iCodeTop = updateLineNumber(node, iCodeTop);
                 iCodeTop = generateICode(child, iCodeTop);
                 iCodeTop = addByte(TokenStream.THROW, iCodeTop);
                 itsStackDepth--;
                 break;
 
             case TokenStream.RETURN :
                 iCodeTop = updateLineNumber(node, iCodeTop);
                 if (child != null) {
                     iCodeTop = generateICode(child, iCodeTop);
                     iCodeTop = addByte(TokenStream.RETURN, iCodeTop);
                     itsStackDepth--;
                 }
                 else {
                     iCodeTop = addByte(RETURN_UNDEF_ICODE, iCodeTop);
                 }
                 break;
 
             case TokenStream.GETVAR : {
                     String name = node.getString();
                     if (itsData.itsNeedsActivation) {
                         // SETVAR handled this by turning into a SETPROP, but
                         // we can't do that to a GETVAR without manufacturing
                         // bogus children. Instead we use a special op to
                         // push the current scope.
                         iCodeTop = addByte(TokenStream.SCOPE, iCodeTop);
                         iCodeTop = addByte(TokenStream.STRING, iCodeTop);
                         iCodeTop = addString(name, iCodeTop);
                         itsStackDepth += 2;
                         if (itsStackDepth > itsData.itsMaxStack)
                             itsData.itsMaxStack = itsStackDepth;
                         iCodeTop = addByte(TokenStream.GETPROP, iCodeTop);
                         itsStackDepth--;
                     }
                     else {
                         int index = itsVariableTable.getOrdinal(name);
                         iCodeTop = addByte(TokenStream.GETVAR, iCodeTop);
                         iCodeTop = addByte(index, iCodeTop);
                         itsStackDepth++;
                         if (itsStackDepth > itsData.itsMaxStack)
                             itsData.itsMaxStack = itsStackDepth;
                     }
                 }
                 break;
 
             case TokenStream.SETVAR : {
                     if (itsData.itsNeedsActivation) {
                         child.setType(TokenStream.BINDNAME);
                         node.setType(TokenStream.SETNAME);
                         iCodeTop = generateICode(node, iCodeTop);
                     }
                     else {
                         String name = child.getString();
                         child = child.getNextSibling();
                         iCodeTop = generateICode(child, iCodeTop);
                         int index = itsVariableTable.getOrdinal(name);
                         iCodeTop = addByte(TokenStream.SETVAR, iCodeTop);
                         iCodeTop = addByte(index, iCodeTop);
                     }
                 }
                 break;
 
             case TokenStream.PRIMARY:
                 iCodeTop = addByte(node.getInt(), iCodeTop);
                 itsStackDepth++;
                 if (itsStackDepth > itsData.itsMaxStack)
                     itsData.itsMaxStack = itsStackDepth;
                 break;
 
             case TokenStream.ENUMINIT :
                 iCodeTop = generateICode(child, iCodeTop);
                 iCodeTop = addByte(TokenStream.ENUMINIT, iCodeTop);
                 iCodeTop = addLocalRef(node, iCodeTop);
                 itsStackDepth--;
                 break;
 
             case TokenStream.ENUMNEXT : {
                     iCodeTop = addByte(TokenStream.ENUMNEXT, iCodeTop);
                     Node init = (Node)node.getProp(Node.ENUM_PROP);
                     iCodeTop = addLocalRef(init, iCodeTop);
                     itsStackDepth++;
                     if (itsStackDepth > itsData.itsMaxStack)
                         itsData.itsMaxStack = itsStackDepth;
                 }
                 break;
 
             case TokenStream.ENUMDONE :
                 // could release the local here??
                 break;
 
             case TokenStream.OBJECT : {
                     Node regexp = (Node) node.getProp(Node.REGEXP_PROP);
                     int index = regexp.getExistingIntProp(Node.REGEXP_PROP);
                     iCodeTop = addByte(TokenStream.OBJECT, iCodeTop);
                     iCodeTop = addShort(index, iCodeTop);
                     itsStackDepth++;
                     if (itsStackDepth > itsData.itsMaxStack)
                         itsData.itsMaxStack = itsStackDepth;
                 }
                 break;
 
             default :
                 badTree(node);
                 break;
         }
         return iCodeTop;
     }
 
     private int addLocalRef(Node node, int iCodeTop)
     {
         int theLocalSlot = node.getIntProp(Node.LOCAL_PROP, -1);
         if (theLocalSlot == -1) {
             theLocalSlot = itsData.itsMaxLocals++;
             node.putIntProp(Node.LOCAL_PROP, theLocalSlot);
         }
         iCodeTop = addByte(theLocalSlot, iCodeTop);
         if (theLocalSlot >= itsData.itsMaxLocals)
             itsData.itsMaxLocals = theLocalSlot + 1;
         return iCodeTop;
     }
 
     private int addGoto(Node node, int gotoOp, int iCodeTop)
     {
         Node target = (Node)(node.getProp(Node.TARGET_PROP));
         int targetLabel = target.getIntProp(Node.LABEL_PROP, -1);
         if (targetLabel == -1) {
             targetLabel = acquireLabel();
             target.putIntProp(Node.LABEL_PROP, targetLabel);
         }
         iCodeTop = addGoto(targetLabel, (byte) gotoOp, iCodeTop);
         return iCodeTop;
     }
 
     private int addGoto(int targetLabel, int gotoOp, int iCodeTop)
     {
         int gotoPC = iCodeTop;
         iCodeTop = addByte(gotoOp, iCodeTop);
         int theLabel = targetLabel & 0x7FFFFFFF;
         int targetPC = itsLabelTable[theLabel].getPC();
         if (targetPC != -1) {
             int offset = targetPC - gotoPC;
             iCodeTop = addShort(offset, iCodeTop);
         }
         else {
             itsLabelTable[theLabel].addFixup(gotoPC + 1);
             iCodeTop = addShort(0, iCodeTop);
         }
         return iCodeTop;
     }
 
     private int addByte(int b, int iCodeTop) {
         byte[] array = itsData.itsICode;
         if (array.length == iCodeTop) {
             byte[] ba = new byte[iCodeTop * 2];
             System.arraycopy(array, 0, ba, 0, iCodeTop);
             itsData.itsICode = array = ba;
         }
         array[iCodeTop++] = (byte)b;
         return iCodeTop;
     }
 
     private int addShort(int s, int iCodeTop) {
         byte[] array = itsData.itsICode;
         if (iCodeTop + 2 > array.length) {
             byte[] ba = new byte[(iCodeTop + 2) * 2];
             System.arraycopy(array, 0, ba, 0, iCodeTop);
             itsData.itsICode = array = ba;
         }
         array[iCodeTop] = (byte)(s >>> 8);
         array[iCodeTop + 1] = (byte)s;
         return iCodeTop + 2;
     }
 
     private int addInt(int i, int iCodeTop) {
         byte[] array = itsData.itsICode;
         if (iCodeTop + 4 > array.length) {
             byte[] ba = new byte[(iCodeTop + 4) * 2];
             System.arraycopy(array, 0, ba, 0, iCodeTop);
             itsData.itsICode = array = ba;
         }
         array[iCodeTop] = (byte)(i >>> 24);
         array[iCodeTop + 1] = (byte)(i >>> 16);
         array[iCodeTop + 2] = (byte)(i >>> 8);
         array[iCodeTop + 3] = (byte)i;
         return iCodeTop + 4;
     }
 
     private int addDouble(double num, int iCodeTop) {
         int index = itsData.itsDoubleTableIndex;
         if (index == 0) {
             itsData.itsDoubleTable = new double[64];
         }
         else if (itsData.itsDoubleTable.length == index) {
             double[] na = new double[index * 2];
             System.arraycopy(itsData.itsDoubleTable, 0, na, 0, index);
             itsData.itsDoubleTable = na;
         }
         itsData.itsDoubleTable[index] = num;
         itsData.itsDoubleTableIndex = index + 1;
 
         iCodeTop = addShort(index, iCodeTop);
         return iCodeTop;
     }
 
     private int addString(String str, int iCodeTop) {
         int index = itsData.itsStringTableIndex;
         if (itsData.itsStringTable.length == index) {
             String[] sa = new String[index * 2];
             System.arraycopy(itsData.itsStringTable, 0, sa, 0, index);
             itsData.itsStringTable = sa;
         }
         itsData.itsStringTable[index] = str;
         itsData.itsStringTableIndex = index + 1;
 
         iCodeTop = addShort(index, iCodeTop);
         return iCodeTop;
     }
 
     private static int getShort(byte[] iCode, int pc) {
         return (iCode[pc] << 8) | (iCode[pc + 1] & 0xFF);
     }
 
     private static int getInt(byte[] iCode, int pc) {
         return (iCode[pc] << 24) | ((iCode[pc + 1] & 0xFF) << 16)
                | ((iCode[pc + 2] & 0xFF) << 8) | (iCode[pc + 3] & 0xFF);
     }
 
     private static int getTarget(byte[] iCode, int pc) {
         int displacement = getShort(iCode, pc);
         return pc - 1 + displacement;
     }
 
     static PrintWriter out;
     static {
         if (Context.printICode) {
             try {
                 out = new PrintWriter(new FileOutputStream("icode.txt"));
                 out.close();
             }
             catch (IOException x) {
             }
         }
     }
 
     private static String icodeToName(int icode) {
         if (Context.printICode) {
             if (icode <= TokenStream.LAST_TOKEN) {
                 return TokenStream.tokenToName(icode);
             }else {
                 switch (icode) {
                     case LINE_ICODE:         return "line";
                     case SOURCEFILE_ICODE:   return "sourcefile";
                     case BREAKPOINT_ICODE:   return "breakpoint";
                     case SHORTNUMBER_ICODE:  return "shortnumber";
                     case INTNUMBER_ICODE:    return "intnumber";
                     case RETURN_UNDEF_ICODE: return "return_undef";
                     case END_ICODE:          return "end";
                 }
             }
             return "<UNKNOWN ICODE: "+icode+">";
         }
         return "";
     }
 
     private static void dumpICode(InterpreterData theData) {
         if (Context.printICode) {
             try {
                 int iCodeLength = theData.itsICodeTop;
                 byte iCode[] = theData.itsICode;
                 String[] strings = theData.itsStringTable;
 
                 out = new PrintWriter(new FileOutputStream("icode.txt", true));
                 out.println("ICode dump, for " + theData.itsName
                             + ", length = " + iCodeLength);
                 out.println("MaxStack = " + theData.itsMaxStack);
 
                 for (int pc = 0; pc < iCodeLength; ) {
                     out.print("[" + pc + "] ");
                     int token = iCode[pc] & 0xff;
                     String tname = icodeToName(token);
                     ++pc;
                     switch (token) {
                         case TokenStream.SCOPE :
                         case TokenStream.GETPROTO :
                         case TokenStream.GETPARENT :
                         case TokenStream.GETSCOPEPARENT :
                         case TokenStream.SETPROTO :
                         case TokenStream.SETPARENT :
                         case TokenStream.DELPROP :
                         case TokenStream.TYPEOF :
                         case TokenStream.NEWSCOPE :
                         case TokenStream.ENTERWITH :
                         case TokenStream.LEAVEWITH :
                         case TokenStream.RETURN :
                         case TokenStream.ENDTRY :
                         case TokenStream.THROW :
                         case TokenStream.JTHROW :
                         case TokenStream.GETTHIS :
                         case TokenStream.SETELEM :
                         case TokenStream.GETELEM :
                         case TokenStream.SETPROP :
                         case TokenStream.GETPROP :
                         case TokenStream.PROPINC :
                         case TokenStream.PROPDEC :
                         case TokenStream.ELEMINC :
                         case TokenStream.ELEMDEC :
                         case TokenStream.BITNOT :
                         case TokenStream.BITAND :
                         case TokenStream.BITOR :
                         case TokenStream.BITXOR :
                         case TokenStream.LSH :
                         case TokenStream.RSH :
                         case TokenStream.URSH :
                         case TokenStream.NEG :
                         case TokenStream.POS :
                         case TokenStream.SUB :
                         case TokenStream.MUL :
                         case TokenStream.DIV :
                         case TokenStream.MOD :
                         case TokenStream.ADD :
                         case TokenStream.POPV :
                         case TokenStream.POP :
                         case TokenStream.DUP :
                         case TokenStream.LT :
                         case TokenStream.GT :
                         case TokenStream.LE :
                         case TokenStream.GE :
                         case TokenStream.IN :
                         case TokenStream.INSTANCEOF :
                         case TokenStream.EQ :
                         case TokenStream.NE :
                         case TokenStream.SHEQ :
                         case TokenStream.SHNE :
                         case TokenStream.ZERO :
                         case TokenStream.ONE :
                         case TokenStream.NULL :
                         case TokenStream.THIS :
                         case TokenStream.THISFN :
                         case TokenStream.FALSE :
                         case TokenStream.TRUE :
                         case TokenStream.UNDEFINED :
                         case SOURCEFILE_ICODE :
                         case RETURN_UNDEF_ICODE:
                         case END_ICODE:
                             out.println(tname);
                             break;
                         case TokenStream.GOSUB :
                         case TokenStream.GOTO :
                         case TokenStream.IFEQ :
                         case TokenStream.IFNE : {
                                 int newPC = getTarget(iCode, pc);
                                 out.println(tname + " " + newPC);
                                 pc += 2;
                             }
                             break;
                         case TokenStream.TRY : {
                                 int newPC1 = getTarget(iCode, pc);
                                 int newPC2 = getTarget(iCode, pc + 2);
                                 out.println(tname + " " + newPC1
                                             + " " + newPC2);
                                 pc += 4;
                             }
                             break;
                         case TokenStream.RETSUB :
                         case TokenStream.ENUMINIT :
                         case TokenStream.ENUMNEXT :
                         case TokenStream.VARINC :
                         case TokenStream.VARDEC :
                         case TokenStream.GETVAR :
                         case TokenStream.SETVAR :
                         case TokenStream.NEWTEMP :
                         case TokenStream.USETEMP : {
                                 int slot = (iCode[pc] & 0xFF);
                                 out.println(tname + " " + slot);
                                 pc++;
                             }
                             break;
                         case TokenStream.CALLSPECIAL : {
                                 int line = getShort(iCode, pc);
                                 String name = strings[getShort(iCode, pc + 2)];
                                 int count = getShort(iCode, pc + 4);
                                 out.println(tname + " " + count
                                             + " " + line + " " + name);
                                 pc += 6;
                             }
                             break;
                         case TokenStream.OBJECT :
                         case TokenStream.CLOSURE :
                         case TokenStream.NEW :
                         case TokenStream.CALL : {
                                 int count = getShort(iCode, pc + 2);
                                 String name = strings[getShort(iCode, pc)];
                                 out.println(tname + " " + count + " \""
                                             + name + "\"");
                                 pc += 4;
                             }
                             break;
                         case SHORTNUMBER_ICODE : {
                                 int value = getShort(iCode, pc);
                                 out.println(tname + " " + value);
                                 pc += 2;
                             }
                             break;
                         case INTNUMBER_ICODE : {
                                 int value = getInt(iCode, pc);
                                 out.println(tname + " " + value);
                                 pc += 4;
                             }
                             break;
                         case TokenStream.NUMBER : {
                                 int index = getShort(iCode, pc);
                                 double value = theData.itsDoubleTable[index];
                                 out.println(tname + " " + value);
                                 pc += 2;
                             }
                             break;
                         case TokenStream.TYPEOFNAME :
                         case TokenStream.GETBASE :
                         case TokenStream.BINDNAME :
                         case TokenStream.SETNAME :
                         case TokenStream.NAME :
                         case TokenStream.NAMEINC :
                         case TokenStream.NAMEDEC :
                         case TokenStream.STRING :
                             out.println(tname + " \""
                                         + strings[getShort(iCode, pc)] + "\"");
                             pc += 2;
                             break;
                         case LINE_ICODE : {
                                 int line = getShort(iCode, pc);
                                 out.println(tname + " : " + line);
                                 pc += 2;
                             }
                             break;
                         default :
                             out.close();
                             throw new RuntimeException("Unknown icode : "
                                     + token  + " @ pc : " + (pc - 1));
                     }
                 }
                 out.close();
             }
             catch (IOException x) {}
         }
     }
 
     private static void createFunctionObject(InterpretedFunction fn,
                                              Scriptable scope)
     {
         fn.setPrototype(ScriptableObject.getClassPrototype(scope, "Function"));
         fn.setParentScope(scope);
         InterpreterData id = fn.itsData;
         if (id.itsName.length() == 0)
             return;
         if ((id.itsFunctionType == FunctionNode.FUNCTION_STATEMENT &&
             fn.itsClosure == null) ||
            (id.itsFunctionType == FunctionNode.FUNCTION_EXPRESSION_STATEMENT &&
             fn.itsClosure != null))
         {
             ScriptRuntime.setProp(scope, fn.itsData.itsName, fn, scope);
         }
     }
 
     public static Object interpret(Context cx, Scriptable scope,
                                    Scriptable thisObj, Object[] args,
                                    NativeFunction fnOrScript,
                                    InterpreterData theData)
         throws JavaScriptException
     {
         if (cx.interpreterSecurityDomain != theData.securityDomain) {
            // If securityDomain is different, update domain in Cotext
            // and call self under new domain
             Object savedDomain = cx.interpreterSecurityDomain;
             cx.interpreterSecurityDomain = theData.securityDomain;
             try {
                 return interpret(cx, scope, thisObj, args, fnOrScript, theData);
             } finally {
                 cx.interpreterSecurityDomain = savedDomain;
             }
         }
 
         final Object DBL_MRK = Interpreter.DBL_MRK;
         final Scriptable undefined = Undefined.instance;
 
         final int VAR_SHFT = theData.itsMaxStack;
         final int maxVars = (fnOrScript.argNames == null)
                             ? 0 : fnOrScript.argNames.length;
         final int LOCAL_SHFT = VAR_SHFT + maxVars;
         final int TRY_SCOPE_SHFT = LOCAL_SHFT + theData.itsMaxLocals;
 
 // stack[0 <= i < VAR_SHFT]: stack data
 // stack[VAR_SHFT <= i < LOCAL_SHFT]: variables
 // stack[LOCAL_SHFT <= i < TRY_SCOPE_SHFT]: used for newtemp/usetemp
 // stack[TRY_SCOPE_SHFT <= i]: try scopes
 // when 0 <= i < LOCAL_SHFT and stack[x] == DBL_MRK,
 // sDbl[i]  gives the number value
 
         Object[] stack = new Object[TRY_SCOPE_SHFT + theData.itsMaxTryDepth];
         double[] sDbl = new double[TRY_SCOPE_SHFT];
         int stackTop = -1;
 
 // tryStack[i]: starting pc of try block that itself points via its pc+1 and
 // pc+3 to catch and finaly blocks
         int[] tryStack = null;
         int tryStackTop = 0;
 
         if (maxVars != 0) {
             int definedArgs = fnOrScript.argCount;
             if (definedArgs != 0) {
                 if (definedArgs > args.length) { definedArgs = args.length; }
                 for (int i = 0; i != definedArgs; ++i) {
                     stack[VAR_SHFT + i] = args[i];
                 }
             }
             for (int i = definedArgs; i != maxVars; ++i) {
                 stack[VAR_SHFT + i] = undefined;
             }
         }
 
         if (theData.isFunction) {
             if (fnOrScript.itsClosure != null) {
                 scope = fnOrScript.itsClosure;
             }else if (!theData.itsUseDynamicScope) {
                 scope = fnOrScript.getParentScope();
             }
         }else {
             scope = ScriptRuntime.initScript(cx, scope, fnOrScript, thisObj,
                                              theData.itsFromEvalCode);
         }
 
         if (theData.itsCheckThis) {
             thisObj = ScriptRuntime.getThis(thisObj);
         }
 
         if (theData.itsNeedsActivation) {
             scope = ScriptRuntime.initVarObj(cx, scope, fnOrScript,
                                              thisObj, args);
         }
 
         InterpreterFrame frame = null;
         if (cx.debugger != null) {
             frame = new InterpreterFrame(scope, theData, fnOrScript);
             cx.pushFrame(frame);
         }
 
         if (theData.itsNestedFunctions != null) {
             for (int i = 0; i < theData.itsNestedFunctions.length; i++)
                 createFunctionObject(theData.itsNestedFunctions[i], scope);
         }
 
         Object result = undefined;
 
         byte[] iCode = theData.itsICode;
         String[] strings = theData.itsStringTable;
         int pc = 0;
 
         int pcPrevBranch = pc;
         final int instructionThreshold = cx.instructionThreshold;
         // During function call this will be set to -1 so catch can properly
         // adjust it
         int instructionCount = cx.instructionCount;
         // arbitrary number to add to instructionCount when calling
         // other functions
         final int INVOCATION_COST = 100;
 
         Loop: while (true) {
             try {
                 switch (iCode[pc] & 0xff) {
                 case TokenStream.ENDTRY :
                     tryStackTop--;
                     break;
                 case TokenStream.TRY : {
                     if (tryStackTop == 0) {
                         tryStack = new int[theData.itsMaxTryDepth];
                     }
                     tryStack[tryStackTop] = pc;
                     stack[TRY_SCOPE_SHFT + tryStackTop] = scope;
                     ++tryStackTop;
                     pc += 4;
                     break;
                 }
                 case TokenStream.GE : {
                     --stackTop;
                     Object rhs = stack[stackTop + 1];
                     Object lhs = stack[stackTop];
                     boolean valBln;
                     if (rhs == DBL_MRK || lhs == DBL_MRK) {
                         double rDbl = stack_double(stack, sDbl, stackTop + 1);
                         double lDbl = stack_double(stack, sDbl, stackTop);
                         valBln = (rDbl == rDbl && lDbl == lDbl
                                   && rDbl <= lDbl);
                     }
                     else {
                         valBln = (1 == ScriptRuntime.cmp_LE(rhs, lhs));
                     }
                     stack[stackTop] = valBln ? Boolean.TRUE : Boolean.FALSE;
                     break;
                 }
                 case TokenStream.LE : {
                     --stackTop;
                     Object rhs = stack[stackTop + 1];
                     Object lhs = stack[stackTop];
                     boolean valBln;
                     if (rhs == DBL_MRK || lhs == DBL_MRK) {
                         double rDbl = stack_double(stack, sDbl, stackTop + 1);
                         double lDbl = stack_double(stack, sDbl, stackTop);
                         valBln = (rDbl == rDbl && lDbl == lDbl
                                   && lDbl <= rDbl);
                     }
                     else {
                         valBln = (1 == ScriptRuntime.cmp_LE(lhs, rhs));
                     }
                     stack[stackTop] = valBln ? Boolean.TRUE : Boolean.FALSE;
                     break;
                 }
                 case TokenStream.GT : {
                     --stackTop;
                     Object rhs = stack[stackTop + 1];
                     Object lhs = stack[stackTop];
                     boolean valBln;
                     if (rhs == DBL_MRK || lhs == DBL_MRK) {
                         double rDbl = stack_double(stack, sDbl, stackTop + 1);
                         double lDbl = stack_double(stack, sDbl, stackTop);
                         valBln = (rDbl == rDbl && lDbl == lDbl
                                   && rDbl < lDbl);
                     }
                     else {
                         valBln = (1 == ScriptRuntime.cmp_LT(rhs, lhs));
                     }
                     stack[stackTop] = valBln ? Boolean.TRUE : Boolean.FALSE;
                     break;
                 }
                 case TokenStream.LT : {
                     --stackTop;
                     Object rhs = stack[stackTop + 1];
                     Object lhs = stack[stackTop];
                     boolean valBln;
                     if (rhs == DBL_MRK || lhs == DBL_MRK) {
                         double rDbl = stack_double(stack, sDbl, stackTop + 1);
                         double lDbl = stack_double(stack, sDbl, stackTop);
                         valBln = (rDbl == rDbl && lDbl == lDbl
                                   && lDbl < rDbl);
                     }
                     else {
                         valBln = (1 == ScriptRuntime.cmp_LT(lhs, rhs));
                     }
                     stack[stackTop] = valBln ? Boolean.TRUE : Boolean.FALSE;
                     break;
                 }
                 case TokenStream.IN : {
                     Object rhs = stack[stackTop];
                     if (rhs == DBL_MRK) rhs = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     boolean valBln = ScriptRuntime.in(lhs, rhs, scope);
                     stack[stackTop] = valBln ? Boolean.TRUE : Boolean.FALSE;
                     break;
                 }
                 case TokenStream.INSTANCEOF : {
                     Object rhs = stack[stackTop];
                     if (rhs == DBL_MRK) rhs = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     boolean valBln = ScriptRuntime.instanceOf(scope, lhs, rhs);
                     stack[stackTop] = valBln ? Boolean.TRUE : Boolean.FALSE;
                     break;
                 }
                 case TokenStream.EQ : {
                     --stackTop;
                     boolean valBln = do_eq(stack, sDbl, stackTop);
                     stack[stackTop] = valBln ? Boolean.TRUE : Boolean.FALSE;
                     break;
                 }
                 case TokenStream.NE : {
                     --stackTop;
                     boolean valBln = !do_eq(stack, sDbl, stackTop);
                     stack[stackTop] = valBln ? Boolean.TRUE : Boolean.FALSE;
                     break;
                 }
                 case TokenStream.SHEQ : {
                     --stackTop;
                     boolean valBln = do_sheq(stack, sDbl, stackTop);
                     stack[stackTop] = valBln ? Boolean.TRUE : Boolean.FALSE;
                     break;
                 }
                 case TokenStream.SHNE : {
                     --stackTop;
                     boolean valBln = !do_sheq(stack, sDbl, stackTop);
                     stack[stackTop] = valBln ? Boolean.TRUE : Boolean.FALSE;
                     break;
                 }
                 case TokenStream.IFNE : {
                     Object val = stack[stackTop];
                     boolean valBln;
                     if (val != DBL_MRK) {
                         valBln = !ScriptRuntime.toBoolean(val);
                     }
                     else {
                         double valDbl = sDbl[stackTop];
                         valBln = !(valDbl == valDbl && valDbl != 0.0);
                     }
                     --stackTop;
                     if (valBln) {
                         if (instructionThreshold != 0) {
                             instructionCount += pc + 3 - pcPrevBranch;
                             if (instructionCount > instructionThreshold) {
                                 cx.observeInstructionCount
                                     (instructionCount);
                                 instructionCount = 0;
                             }
                         }
                         pcPrevBranch = pc = getTarget(iCode, pc + 1);
                         continue;
                     }
                     pc += 2;
                     break;
                 }
                 case TokenStream.IFEQ : {
                     boolean valBln;
                     Object val = stack[stackTop];
                     if (val != DBL_MRK) {
                         valBln = ScriptRuntime.toBoolean(val);
                     }
                     else {
                         double valDbl = sDbl[stackTop];
                         valBln = (valDbl == valDbl && valDbl != 0.0);
                     }
                     --stackTop;
                     if (valBln) {
                         if (instructionThreshold != 0) {
                             instructionCount += pc + 3 - pcPrevBranch;
                             if (instructionCount > instructionThreshold) {
                                 cx.observeInstructionCount
                                     (instructionCount);
                                 instructionCount = 0;
                             }
                         }
                         pcPrevBranch = pc = getTarget(iCode, pc + 1);
                         continue;
                     }
                     pc += 2;
                     break;
                 }
                 case TokenStream.GOTO :
                     if (instructionThreshold != 0) {
                         instructionCount += pc + 3 - pcPrevBranch;
                         if (instructionCount > instructionThreshold) {
                             cx.observeInstructionCount(instructionCount);
                             instructionCount = 0;
                         }
                     }
                     pcPrevBranch = pc = getTarget(iCode, pc + 1);
                     continue;
                 case TokenStream.GOSUB :
                     sDbl[++stackTop] = pc + 3;
                     if (instructionThreshold != 0) {
                         instructionCount += pc + 3 - pcPrevBranch;
                         if (instructionCount > instructionThreshold) {
                             cx.observeInstructionCount(instructionCount);
                             instructionCount = 0;
                         }
                     }
                     pcPrevBranch = pc = getTarget(iCode, pc + 1);                                    continue;
                 case TokenStream.RETSUB : {
                     int slot = (iCode[pc + 1] & 0xFF);
                     if (instructionThreshold != 0) {
                         instructionCount += pc + 2 - pcPrevBranch;
                         if (instructionCount > instructionThreshold) {
                             cx.observeInstructionCount(instructionCount);
                             instructionCount = 0;
                         }
                     }
                     pcPrevBranch = pc = (int)sDbl[LOCAL_SHFT + slot];
                     continue;
                 }
                 case TokenStream.POP :
                     stackTop--;
                     break;
                 case TokenStream.DUP :
                     stack[stackTop + 1] = stack[stackTop];
                     sDbl[stackTop + 1] = sDbl[stackTop];
                     stackTop++;
                     break;
                 case TokenStream.POPV :
                     result = stack[stackTop];
                     if (result == DBL_MRK)
                         result = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     break;
                 case TokenStream.RETURN :
                     result = stack[stackTop];
                     if (result == DBL_MRK)
                         result = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     break Loop;
                 case RETURN_UNDEF_ICODE :
                     result = undefined;
                     break Loop;
                 case END_ICODE:
                     break Loop;
                 case TokenStream.BITNOT : {
                     int rIntValue = stack_int32(stack, sDbl, stackTop);
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = ~rIntValue;
                     break;
                 }
                 case TokenStream.BITAND : {
                     int rIntValue = stack_int32(stack, sDbl, stackTop);
                     --stackTop;
                     int lIntValue = stack_int32(stack, sDbl, stackTop);
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = lIntValue & rIntValue;
                     break;
                 }
                 case TokenStream.BITOR : {
                     int rIntValue = stack_int32(stack, sDbl, stackTop);
                     --stackTop;
                     int lIntValue = stack_int32(stack, sDbl, stackTop);
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = lIntValue | rIntValue;
                     break;
                 }
                 case TokenStream.BITXOR : {
                     int rIntValue = stack_int32(stack, sDbl, stackTop);
                     --stackTop;
                     int lIntValue = stack_int32(stack, sDbl, stackTop);
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = lIntValue ^ rIntValue;
                     break;
                 }
                 case TokenStream.LSH : {
                     int rIntValue = stack_int32(stack, sDbl, stackTop);
                     --stackTop;
                     int lIntValue = stack_int32(stack, sDbl, stackTop);
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = lIntValue << rIntValue;
                     break;
                 }
                 case TokenStream.RSH : {
                     int rIntValue = stack_int32(stack, sDbl, stackTop);
                     --stackTop;
                     int lIntValue = stack_int32(stack, sDbl, stackTop);
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = lIntValue >> rIntValue;
                     break;
                 }
                 case TokenStream.URSH : {
                     int rIntValue = stack_int32(stack, sDbl, stackTop) & 0x1F;
                     --stackTop;
                     double lDbl = stack_double(stack, sDbl, stackTop);
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = ScriptRuntime.toUint32(lDbl)
                                         >>> rIntValue;
                     break;
                 }
                 case TokenStream.ADD :
                     --stackTop;
                     do_add(stack, sDbl, stackTop);
                     break;
                 case TokenStream.SUB : {
                     double rDbl = stack_double(stack, sDbl, stackTop);
                     --stackTop;
                     double lDbl = stack_double(stack, sDbl, stackTop);
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = lDbl - rDbl;
                     break;
                 }
                 case TokenStream.NEG : {
                     double rDbl = stack_double(stack, sDbl, stackTop);
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = -rDbl;
                     break;
                 }
                 case TokenStream.POS : {
                     double rDbl = stack_double(stack, sDbl, stackTop);
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = rDbl;
                     break;
                 }
                 case TokenStream.MUL : {
                     double rDbl = stack_double(stack, sDbl, stackTop);
                     --stackTop;
                     double lDbl = stack_double(stack, sDbl, stackTop);
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = lDbl * rDbl;
                     break;
                 }
                 case TokenStream.DIV : {
                     double rDbl = stack_double(stack, sDbl, stackTop);
                     --stackTop;
                     double lDbl = stack_double(stack, sDbl, stackTop);
                     stack[stackTop] = DBL_MRK;
                     // Detect the divide by zero or let Java do it ?
                     sDbl[stackTop] = lDbl / rDbl;
                     break;
                 }
                 case TokenStream.MOD : {
                     double rDbl = stack_double(stack, sDbl, stackTop);
                     --stackTop;
                     double lDbl = stack_double(stack, sDbl, stackTop);
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = lDbl % rDbl;
                     break;
                 }
                 case TokenStream.BINDNAME : {
                     String name = strings[getShort(iCode, pc + 1)];
                     stack[++stackTop] = ScriptRuntime.bind(scope, name);
                     pc += 2;
                     break;
                 }
                 case TokenStream.GETBASE : {
                     String name = strings[getShort(iCode, pc + 1)];
                     stack[++stackTop] = ScriptRuntime.getBase(scope, name);
                     pc += 2;
                     break;
                 }
                 case TokenStream.SETNAME : {
                     Object rhs = stack[stackTop];
                     if (rhs == DBL_MRK) rhs = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     Object lhs = stack[stackTop];
                     // what about class cast exception here for lhs?
                     stack[stackTop] = ScriptRuntime.setName
                         ((Scriptable)lhs, rhs, scope,
                          strings[getShort(iCode, pc + 1)]);
                     pc += 2;
                     break;
                 }
                 case TokenStream.DELPROP : {
                     Object rhs = stack[stackTop];
                     if (rhs == DBL_MRK) rhs = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     stack[stackTop] = ScriptRuntime.delete(lhs, rhs);
                     break;
                 }
                 case TokenStream.GETPROP : {
                     String name = (String)stack[stackTop];
                     --stackTop;
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     stack[stackTop]
                             = ScriptRuntime.getProp(lhs, name, scope);
                     break;
                 }
                 case TokenStream.SETPROP : {
                     Object rhs = stack[stackTop];
                     if (rhs == DBL_MRK) rhs = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     String name = (String)stack[stackTop];
                     --stackTop;
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     stack[stackTop]
                             = ScriptRuntime.setProp(lhs, name, rhs, scope);
                     break;
                 }
                 case TokenStream.GETELEM :
                     do_getElem(cx, stack, sDbl, stackTop, scope);
                     --stackTop;
                     break;
                 case TokenStream.SETELEM :
                     do_setElem(cx, stack, sDbl, stackTop, scope);
                     stackTop -= 2;
                     break;
                 case TokenStream.PROPINC : {
                     String name = (String)stack[stackTop];
                     --stackTop;
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     stack[stackTop]
                             = ScriptRuntime.postIncrement(lhs, name, scope);
                     break;
                 }
                 case TokenStream.PROPDEC : {
                     String name = (String)stack[stackTop];
                     --stackTop;
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     stack[stackTop]
                             = ScriptRuntime.postDecrement(lhs, name, scope);
                     break;
                 }
                 case TokenStream.ELEMINC : {
                     Object rhs = stack[stackTop];
                     if (rhs == DBL_MRK) rhs = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     stack[stackTop]
                        = ScriptRuntime.postIncrementElem(lhs, rhs, scope);
                     break;
                 }
                 case TokenStream.ELEMDEC : {
                     Object rhs = stack[stackTop];
                     if (rhs == DBL_MRK) rhs = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     stack[stackTop]
                        = ScriptRuntime.postDecrementElem(lhs, rhs, scope);
                     break;
                 }
                 case TokenStream.GETTHIS : {
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     stack[stackTop]
                         = ScriptRuntime.getThis((Scriptable)lhs);
                     break;
                 }
                 case TokenStream.NEWTEMP : {
                     int slot = (iCode[++pc] & 0xFF);
                     stack[LOCAL_SHFT + slot] = stack[stackTop];
                     sDbl[LOCAL_SHFT + slot] = sDbl[stackTop];
                     break;
                 }
                 case TokenStream.USETEMP : {
                     int slot = (iCode[++pc] & 0xFF);
                     ++stackTop;
                     stack[stackTop] = stack[LOCAL_SHFT + slot];
                     sDbl[stackTop] = sDbl[LOCAL_SHFT + slot];
                     break;
                 }
                 case TokenStream.CALLSPECIAL : {
                     if (instructionThreshold != 0) {
                         instructionCount += INVOCATION_COST;
                         cx.instructionCount = instructionCount;
                         instructionCount = -1;
                     }
                     int lineNum = getShort(iCode, pc + 1);
                     String name = strings[getShort(iCode, pc + 3)];
                     int count = getShort(iCode, pc + 5);
                     Object[] outArgs = getArgsArray(stack, sDbl, stackTop,
                                                     count);
                     stackTop -= count;
                     Object rhs = stack[stackTop];
                     if (rhs == DBL_MRK) rhs = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     stack[stackTop] = ScriptRuntime.callSpecial(
                                         cx, lhs, rhs, outArgs,
                                         thisObj, scope, name, lineNum);
                     pc += 6;
                     instructionCount = cx.instructionCount;
                     break;
                 }
                 case TokenStream.CALL : {
                     if (instructionThreshold != 0) {
                         instructionCount += INVOCATION_COST;
                         cx.instructionCount = instructionCount;
                         instructionCount = -1;
                     }
                     cx.instructionCount = instructionCount;
                     int count = getShort(iCode, pc + 3);
                     Object[] outArgs = getArgsArray(stack, sDbl, stackTop,
                                                     count);
                     stackTop -= count;
                     Object rhs = stack[stackTop];
                     if (rhs == DBL_MRK) rhs = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     if (lhs == undefined) {
                         int i = getShort(iCode, pc + 1);
                         if (i != -1)
                             lhs = strings[i];
                     }
                     Scriptable calleeScope = scope;
                     if (theData.itsNeedsActivation) {
                         calleeScope = ScriptableObject.
                             getTopLevelScope(scope);
                     }
                     stack[stackTop] = ScriptRuntime.call(cx, lhs, rhs,
                                                          outArgs,
                                                          calleeScope);
                     pc += 4;
                     instructionCount = cx.instructionCount;
                     break;
                 }
                 case TokenStream.NEW : {
                     if (instructionThreshold != 0) {
                         instructionCount += INVOCATION_COST;
                         cx.instructionCount = instructionCount;
                         instructionCount = -1;
                     }
                     int count = getShort(iCode, pc + 3);
                     Object[] outArgs = getArgsArray(stack, sDbl, stackTop,
                                                     count);
                     stackTop -= count;
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     if (lhs == undefined && getShort(iCode, pc + 1) != -1)
                     {
                         // special code for better error message for call
                         //  to undefined
                         lhs = strings[getShort(iCode, pc + 1)];
                     }
                     stack[stackTop] = ScriptRuntime.newObject(cx, lhs,
                                                               outArgs,
                                                               scope);
                     pc += 4;                                                                         instructionCount = cx.instructionCount;
                     break;
                 }
                 case TokenStream.TYPEOF : {
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     stack[stackTop] = ScriptRuntime.typeof(lhs);
                     break;
                 }
                 case TokenStream.TYPEOFNAME : {
                     String name = strings[getShort(iCode, pc + 1)];
                     stack[++stackTop]
                                 = ScriptRuntime.typeofName(scope, name);
                     pc += 2;
                     break;
                 }
                 case TokenStream.STRING :
                     stack[++stackTop] = strings[getShort(iCode, pc + 1)];
                     pc += 2;
                     break;
                 case SHORTNUMBER_ICODE :
                     ++stackTop;
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = getShort(iCode, pc + 1);
                     pc += 2;
                     break;
                 case INTNUMBER_ICODE :
                     ++stackTop;
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = getInt(iCode, pc + 1);
                     pc += 4;
                     break;
                 case TokenStream.NUMBER :
                     ++stackTop;
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = theData.
                                 itsDoubleTable[getShort(iCode, pc + 1)];
                     pc += 2;
                     break;
                 case TokenStream.NAME :
                     stack[++stackTop] = ScriptRuntime.name
                                 (scope, strings[getShort(iCode, pc + 1)]);
                     pc += 2;
                     break;
                 case TokenStream.NAMEINC :
                     stack[++stackTop] = ScriptRuntime.postIncrement
                                 (scope, strings[getShort(iCode, pc + 1)]);
                     pc += 2;
                     break;
                 case TokenStream.NAMEDEC :
                     stack[++stackTop] = ScriptRuntime.postDecrement
                                 (scope, strings[getShort(iCode, pc + 1)]);
                     pc += 2;
                     break;
                 case TokenStream.SETVAR : {
                     int slot = (iCode[++pc] & 0xFF);
                     stack[VAR_SHFT + slot] = stack[stackTop];
                     sDbl[VAR_SHFT + slot] = sDbl[stackTop];
                     break;
                 }
                 case TokenStream.GETVAR : {
                     int slot = (iCode[++pc] & 0xFF);
                     ++stackTop;
                     stack[stackTop] = stack[VAR_SHFT + slot];
                     sDbl[stackTop] = sDbl[VAR_SHFT + slot];
                     break;
                 }
                 case TokenStream.VARINC : {
                     int slot = (iCode[++pc] & 0xFF);
                     ++stackTop;
                     stack[stackTop] = stack[VAR_SHFT + slot];
                     sDbl[stackTop] = sDbl[VAR_SHFT + slot];
                     stack[VAR_SHFT + slot] = DBL_MRK;
                     sDbl[VAR_SHFT + slot]
                         = stack_double(stack, sDbl, stackTop) + 1.0;
                     break;
                 }
                 case TokenStream.VARDEC : {
                     int slot = (iCode[++pc] & 0xFF);
                     ++stackTop;
                     stack[stackTop] = stack[VAR_SHFT + slot];
                     sDbl[stackTop] = sDbl[VAR_SHFT + slot];
                     stack[VAR_SHFT + slot] = DBL_MRK;
                     sDbl[VAR_SHFT + slot]
                         = stack_double(stack, sDbl, stackTop) - 1.0;
                     break;
                 }
                 case TokenStream.ZERO :
                     ++stackTop;
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = 0;
                     break;
                 case TokenStream.ONE :
                     ++stackTop;
                     stack[stackTop] = DBL_MRK;
                     sDbl[stackTop] = 1;
                     break;
                 case TokenStream.NULL :
                     stack[++stackTop] = null;
                     break;
                 case TokenStream.THIS :
                     stack[++stackTop] = thisObj;
                     break;
                 case TokenStream.THISFN :
                     stack[++stackTop] = fnOrScript;
                     break;
                 case TokenStream.FALSE :
                     stack[++stackTop] = Boolean.FALSE;
                     break;
                 case TokenStream.TRUE :
                     stack[++stackTop] = Boolean.TRUE;
                     break;
                 case TokenStream.UNDEFINED :
                     stack[++stackTop] = Undefined.instance;
                     break;
                 case TokenStream.THROW :
                     result = stack[stackTop];
                     if (result == DBL_MRK)
                         result = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     throw new JavaScriptException(result);
                 case TokenStream.JTHROW :
                     result = stack[stackTop];
                     // No need to check for DBL_MRK: result is Exception
                     --stackTop;
                     if (result instanceof JavaScriptException)
                         throw (JavaScriptException)result;
                     else
                         throw (RuntimeException)result;
                 case TokenStream.ENTERWITH : {
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     scope = ScriptRuntime.enterWith(lhs, scope);
                     break;
                 }
                 case TokenStream.LEAVEWITH :
                     scope = ScriptRuntime.leaveWith(scope);
                     break;
                 case TokenStream.NEWSCOPE :
                     stack[++stackTop] = ScriptRuntime.newScope();
                     break;
                 case TokenStream.ENUMINIT : {
                     int slot = (iCode[++pc] & 0xFF);
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     stack[LOCAL_SHFT + slot]
                         = ScriptRuntime.initEnum(lhs, scope);
                     break;
                 }
                 case TokenStream.ENUMNEXT : {
                     int slot = (iCode[++pc] & 0xFF);
                     Object val = stack[LOCAL_SHFT + slot];
                     ++stackTop;
                     stack[stackTop] = ScriptRuntime.
                         nextEnum((Enumeration)val);
                     break;
                 }
                 case TokenStream.GETPROTO : {
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     stack[stackTop] = ScriptRuntime.getProto(lhs, scope);
                     break;
                 }
                 case TokenStream.GETPARENT : {
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     stack[stackTop] = ScriptRuntime.getParent(lhs);
                     break;
                 }
                 case TokenStream.GETSCOPEPARENT : {
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     stack[stackTop] = ScriptRuntime.getParent(lhs, scope);
                     break;
                 }
                 case TokenStream.SETPROTO : {
                     Object rhs = stack[stackTop];
                     if (rhs == DBL_MRK) rhs = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     stack[stackTop]
                             = ScriptRuntime.setProto(lhs, rhs, scope);
                     break;
                 }
                 case TokenStream.SETPARENT : {
                     Object rhs = stack[stackTop];
                     if (rhs == DBL_MRK) rhs = doubleWrap(sDbl[stackTop]);
                     --stackTop;
                     Object lhs = stack[stackTop];
                     if (lhs == DBL_MRK) lhs = doubleWrap(sDbl[stackTop]);
                     stack[stackTop]
                             = ScriptRuntime.setParent(lhs, rhs, scope);
                     break;
                 }
                 case TokenStream.SCOPE :
                     stack[++stackTop] = scope;
                     break;
                 case TokenStream.CLOSURE : {
                     int i = getShort(iCode, pc + 1);
                     stack[++stackTop]
                         = new InterpretedFunction(
                                 theData.itsNestedFunctions[i],
                                 scope, cx);
                     createFunctionObject(
                           (InterpretedFunction)stack[stackTop], scope);
                     pc += 2;
                     break;
                 }
                 case TokenStream.OBJECT : {
                     int i = getShort(iCode, pc + 1);
                     stack[++stackTop] = theData.itsRegExpLiterals[i];
                     pc += 2;
                     break;
                 }
                 case SOURCEFILE_ICODE :
                     cx.interpreterSourceFile = theData.itsSourceFile;
                     break;
                 case LINE_ICODE :
                 case BREAKPOINT_ICODE : {
                     int i = getShort(iCode, pc + 1);
                     cx.interpreterLine = i;
                     if (frame != null)
                         frame.setLineNumber(i);
                     if ((iCode[pc] & 0xff) == BREAKPOINT_ICODE ||
                         cx.inLineStepMode)
                     {
                         cx.getDebuggableEngine().
                             getDebugger().handleBreakpointHit(cx);
                     }
                     pc += 2;
                     break;
                 }
                 default :
                     dumpICode(theData);
                     throw new RuntimeException("Unknown icode : "
                                  + (iCode[pc] & 0xff) + " @ pc : " + pc);
                 }
                 pc++;
             }
             catch (Throwable ex) {
                 if (instructionThreshold != 0) {
                     if (instructionCount < 0) {
                         // throw during function call
                         instructionCount = cx.instructionCount;
                     }
                     else {
                         // throw during any other operation
                         instructionCount += pc - pcPrevBranch;
                         cx.instructionCount = instructionCount;
                     }
                 }
 
                 final int SCRIPT_THROW = 0, ECMA = 1, RUNTIME = 2, OTHER = 3;
                 int exType;
                 Object errObj; // Object seen by catch
 
                 for (;;) {
                     if (ex instanceof JavaScriptException) {
                         errObj = ScriptRuntime.
                             unwrapJavaScriptException((JavaScriptException)ex);
                         exType = SCRIPT_THROW;
                     }
                     else if (ex instanceof EcmaError) {
                         // an offical ECMA error object,
                         errObj = ((EcmaError)ex).getErrorObject();
                         exType = ECMA;
                     }
                     else if (ex instanceof WrappedException) {
                         Object w = ((WrappedException) ex).unwrap();
                         if (w instanceof Throwable) {
                             ex = (Throwable) w;
                             continue;
                         }
                         errObj = ex;
                         exType = RUNTIME;
                     }
                     else if (ex instanceof RuntimeException) {
                         errObj = ex;
                         exType = RUNTIME;
                     }
                     else {
                         errObj = ex; // Error instance
                         exType = OTHER;
                     }
                     break;
                 }
 
                 if (exType != OTHER && cx.debugger != null) {
                     cx.debugger.handleExceptionThrown(cx, errObj);
                 }
 
                 boolean rethrow = true;
                 if (exType != OTHER && tryStackTop > 0) {
                     // Do not allow for JS to interfere with Error instances
                     // (exType == OTHER), as they can be used to terminate
                     // long running script
                     --tryStackTop;
                     int try_pc = tryStack[tryStackTop];
                     if (exType == SCRIPT_THROW || exType == ECMA) {
                         // Allow JS to catch only JavaScriptException and
                         // EcmaError
                         int catch_pc = getTarget(iCode, try_pc + 1);
                         if (catch_pc == try_pc) { catch_pc = 0; }
                         if (catch_pc != 0) {
                             // Has catch block
                             rethrow = false;
                             pc = catch_pc;
                         }
                     }
                     if (rethrow) {
                         int finally_pc = getTarget(iCode, try_pc + 3);
                         if (finally_pc == try_pc + 2) { finally_pc = 0; }
                         if (finally_pc != 0) {
                             // has finally block
                             rethrow = false;
                             errObj = ex;
                             pc = finally_pc;
                         }
                     }
                 }
 
                 if (rethrow) {
                     if (frame != null) {
                         cx.popFrame();
                     }
                     if (theData.itsNeedsActivation) {
                         ScriptRuntime.popActivation(cx);
                     }
 
                     if (exType == SCRIPT_THROW)
                         throw (JavaScriptException)ex;
                     if (exType == ECMA || exType == RUNTIME)
                         throw (RuntimeException)ex;
                     throw (Error)ex;
                 }
 
                 // We caught an exception,
 
                 // Notify instruction observer if necessary
                 // and point pcPrevBranch to start of catch/finally block
                 if (instructionThreshold != 0) {
                     if (instructionCount > instructionThreshold) {
                         // Note: this can throw Error
                         cx.observeInstructionCount(instructionCount);
                         instructionCount = 0;
                     }
                 }
                 pcPrevBranch = pc;
 
                 // prepare stack and restore this function's security domain.
                 scope = (Scriptable)stack[TRY_SCOPE_SHFT + tryStackTop];
                 stackTop = 0;
                 stack[0] = errObj;
             }
         }
         if (frame != null) {
             cx.popFrame();
         }
         if (theData.itsNeedsActivation) {
             ScriptRuntime.popActivation(cx);
         }
 
         if (instructionThreshold != 0) {
             if (instructionCount > instructionThreshold) {
                 cx.observeInstructionCount(instructionCount);
                 instructionCount = 0;
             }
             cx.instructionCount = instructionCount;
         }
 
         return result;
     }
 
     private static Object doubleWrap(double x) {
         return new Double(x);
     }
 
     private static int stack_int32(Object[] stack, double[] stackDbl, int i) {
         Object x = stack[i];
         return (x != DBL_MRK)
             ? ScriptRuntime.toInt32(x)
             : ScriptRuntime.toInt32(stackDbl[i]);
     }
 
     private static double stack_double(Object[] stack, double[] stackDbl,
                                        int i)
     {
         Object x = stack[i];
         return (x != DBL_MRK) ? ScriptRuntime.toNumber(x) : stackDbl[i];
     }
 
     private static void do_add(Object[] stack, double[] stackDbl, int stackTop)
     {
         Object rhs = stack[stackTop + 1];
         Object lhs = stack[stackTop];
         if (rhs == DBL_MRK) {
             double rDbl = stackDbl[stackTop + 1];
             if (lhs == DBL_MRK) {
                 stackDbl[stackTop] += rDbl;
             }
             else {
                 do_add(lhs, rDbl, stack, stackDbl, stackTop, true);
             }
         }
         else if (lhs == DBL_MRK) {
             do_add(rhs, stackDbl[stackTop], stack, stackDbl, stackTop, false);
         }
         else {
             if (lhs instanceof Scriptable)
                 lhs = ((Scriptable) lhs).getDefaultValue(null);
             if (rhs instanceof Scriptable)
                 rhs = ((Scriptable) rhs).getDefaultValue(null);
             if (lhs instanceof String || rhs instanceof String) {
                 stack[stackTop] = ScriptRuntime.toString(lhs)
                                    + ScriptRuntime.toString(rhs);
             }
             else {
                 double lDbl = (lhs instanceof Number)
                     ? ((Number)lhs).doubleValue() : ScriptRuntime.toNumber(lhs);
                 double rDbl = (rhs instanceof Number)
                     ? ((Number)rhs).doubleValue() : ScriptRuntime.toNumber(rhs);
                 stack[stackTop] = DBL_MRK;
                 stackDbl[stackTop] = lDbl + rDbl;
             }
         }
     }
 
     // x + y when x is Number, see
     private static void do_add
         (Object lhs, double rDbl,
          Object[] stack, double[] stackDbl, int stackTop,
          boolean left_right_order)
     {
         if (lhs instanceof Scriptable) {
             if (lhs == Undefined.instance) {
                 lhs = ScriptRuntime.NaNobj;
             } else {
                 lhs = ((Scriptable)lhs).getDefaultValue(null);
             }
         }
         if (lhs instanceof String) {
             if (left_right_order) {
                 stack[stackTop] = (String)lhs + ScriptRuntime.toString(rDbl);
             }
             else {
                 stack[stackTop] = ScriptRuntime.toString(rDbl) + (String)lhs;
             }
         }
         else {
             double lDbl = (lhs instanceof Number)
                 ? ((Number)lhs).doubleValue() : ScriptRuntime.toNumber(lhs);
             stack[stackTop] = DBL_MRK;
             stackDbl[stackTop] = lDbl + rDbl;
         }
     }
 
     private static boolean do_eq(Object[] stack, double[] stackDbl,
                                  int stackTop)
     {
         boolean result;
         Object rhs = stack[stackTop + 1];
         Object lhs = stack[stackTop];
         if (rhs == DBL_MRK) {
             if (lhs == DBL_MRK) {
                 result = (stackDbl[stackTop] == stackDbl[stackTop + 1]);
             }
             else {
                 result = do_eq(stackDbl[stackTop + 1], lhs);
             }
         }
         else {
             if (lhs == DBL_MRK) {
                 result = do_eq(stackDbl[stackTop], rhs);
             }
             else {
                 result = ScriptRuntime.eq(lhs, rhs);
             }
         }
         return result;
     }
 
 // Optimized version of ScriptRuntime.eq if x is a Number
     private static boolean do_eq(double x, Object y) {
         for (;;) {
             if (y instanceof Number) {
                 return x == ((Number) y).doubleValue();
             }
             if (y instanceof String) {
                 return x == ScriptRuntime.toNumber((String)y);
             }
             if (y instanceof Boolean) {
                 return x == (((Boolean)y).booleanValue() ? 1 : 0);
             }
             if (y instanceof Scriptable) {
                 if (y == Undefined.instance) { return false; }
                 y = ScriptRuntime.toPrimitive(y);
                 continue;
             }
             return false;
         }
     }
 
     private static boolean do_sheq(Object[] stack, double[] stackDbl,
                                    int stackTop)
     {
         boolean result;
         Object rhs = stack[stackTop + 1];
         Object lhs = stack[stackTop];
         if (rhs == DBL_MRK) {
             double rDbl = stackDbl[stackTop + 1];
             if (lhs == DBL_MRK) {
                 result = (stackDbl[stackTop] == rDbl);
             }
             else {
                 result = (lhs instanceof Number);
                 if (result) {
                     result = (((Number)lhs).doubleValue() == rDbl);
                 }
             }
         }
         else if (rhs instanceof Number) {
             double rDbl = ((Number)rhs).doubleValue();
             if (lhs == DBL_MRK) {
                 result = (stackDbl[stackTop] == rDbl);
             }
             else {
                 result = (lhs instanceof Number);
                 if (result) {
                     result = (((Number)lhs).doubleValue() == rDbl);
                 }
             }
         }
         else {
             result = ScriptRuntime.shallowEq(lhs, rhs);
         }
         return result;
     }
 
     private static void do_getElem(Context cx,
                                    Object[] stack, double[] stackDbl,
                                    int stackTop, Scriptable scope)
     {
         Object lhs = stack[stackTop - 1];
         if (lhs == DBL_MRK) lhs = doubleWrap(stackDbl[stackTop - 1]);
 
         Object result;
         Object id = stack[stackTop];
         if (id != DBL_MRK) {
             result = ScriptRuntime.getElem(lhs, id, scope);
         }
         else {
             Scriptable obj = (lhs instanceof Scriptable)
                              ? (Scriptable)lhs
                              : ScriptRuntime.toObject(cx, scope, lhs);
             double val = stackDbl[stackTop];
             int index = (int)val;
             if (index == val) {
                 result = ScriptRuntime.getElem(obj, index);
             }
             else {
                 String s = ScriptRuntime.toString(val);
                 result = ScriptRuntime.getStrIdElem(obj, s);
             }
         }
         stack[stackTop - 1] = result;
     }
 
     private static void do_setElem(Context cx,
                                    Object[] stack, double[] stackDbl,
                                    int stackTop, Scriptable scope)
     {
         Object rhs = stack[stackTop];
         if (rhs == DBL_MRK) rhs = doubleWrap(stackDbl[stackTop]);
         Object lhs = stack[stackTop - 2];
         if (lhs == DBL_MRK) lhs = doubleWrap(stackDbl[stackTop - 2]);
 
         Object result;
         Object id = stack[stackTop - 1];
         if (id != DBL_MRK) {
             result = ScriptRuntime.setElem(lhs, id, rhs, scope);
         }
         else {
             Scriptable obj = (lhs instanceof Scriptable)
                              ? (Scriptable)lhs
                              : ScriptRuntime.toObject(cx, scope, lhs);
             double val = stackDbl[stackTop - 1];
             int index = (int)val;
             if (index == val) {
                 result = ScriptRuntime.setElem(obj, index, rhs);
             }
             else {
                 String s = ScriptRuntime.toString(val);
                 result = ScriptRuntime.setStrIdElem(obj, s, rhs, scope);
             }
         }
         stack[stackTop - 2] = result;
     }
 
     private static Object[] getArgsArray(Object[] stack, double[] sDbl,
                                          int stackTop, int count)
     {
         if (count == 0) {
             return ScriptRuntime.emptyArgs;
         }
         Object[] args = new Object[count];
         do {
             Object val = stack[stackTop];
             if (val == DBL_MRK)
                 val = doubleWrap(sDbl[stackTop]);
             args[--count] = val;
             --stackTop;
         } while (count != 0);
         return args;
     }
 
     private int version;
     private boolean inLineStepMode;
     private StringBuffer debugSource;
 
     private static final Object DBL_MRK = new Object();
 }
