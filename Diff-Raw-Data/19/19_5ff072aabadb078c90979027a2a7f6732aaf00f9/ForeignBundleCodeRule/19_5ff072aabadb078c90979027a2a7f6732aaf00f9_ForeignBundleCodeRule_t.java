 /*
  *  Copyright (c) 2002-2005, the pmd-netbeans team
  *  All rights reserved.
  *
  *  Redistribution and use in source and binary forms, with or without modification,
  *  are permitted provided that the following conditions are met:
  *
  *  - Redistributions of source code must retain the above copyright notice,
  *  this list of conditions and the following disclaimer.
  *
  *  - Redistributions in binary form must reproduce the above copyright
  *  notice, this list of conditions and the following disclaimer in the
  *  documentation and/or other materials provided with the distribution.
  *
  *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  *  ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
  *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
  *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
  *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  *  DAMAGE.
  */
 package org.netbeans.modules.pmd.rules;
 
 import java.text.MessageFormat;
 
 import net.sourceforge.pmd.ast.*;
 import net.sourceforge.pmd.*;
 import net.sourceforge.pmd.symboltable.*;
 
 /** Searches for bundle use that refers to another class
  *
  * Taken from UnusedPrivateVariable
  * @author Radim Kubacki
  */
 public class ForeignBundleCodeRule extends AbstractRule {
 
     // TODO want to ignore interfaces
     
     private boolean isBundleExpression (String value) {
         if (value == null) {
             return false;
         }
 
         if (value.indexOf("NbBundle.getMessage") >= 0   // NOI18N
         ||  value.indexOf("NbBundle.getBundle") >= 0)   // NOI18N
             return true;
         
         return false;
     }
     
     public Object visit(ASTPrimaryExpression node, Object data) {
         RuleContext ctx = (RuleContext)data;
 
 //        for (int i=0;i<node.jjtGetNumChildren(); i++) {
         if (node.jjtGetNumChildren()>=2) {
             SimpleNode child = (SimpleNode)node.jjtGetChild(0);
             if (child instanceof ASTPrimaryPrefix && child.jjtGetNumChildren() > 0 &&  child.jjtGetChild(0) instanceof ASTName) {
                 ASTName prefix = (ASTName)child.jjtGetChild(0);
                 
                 if (isBundleExpression (prefix.getImage())) {
                     SimpleNode suffix = (SimpleNode)node.jjtGetChild(1);
                     if (suffix instanceof ASTPrimarySuffix && suffix.jjtGetNumChildren() > 0 &&  suffix.jjtGetChild(0) instanceof ASTArguments) {
                         ASTArguments args = (ASTArguments)suffix.jjtGetChild(0);
 
                         ASTExpression av = (args.getArgumentCount()>0)? (ASTExpression)args.jjtGetChild(0).jjtGetChild(0):null;
                         ASTPrimaryExpression pExpr = null;
                         if (av != null && av.jjtGetNumChildren() == 1 && av.jjtGetChild(0) instanceof ASTPrimaryExpression) {
                             pExpr = (ASTPrimaryExpression)av.jjtGetChild(0);
                         }
                         ASTPrimaryPrefix pPre = null;
                         if (pExpr != null && pExpr.jjtGetNumChildren() == 1 && pExpr.jjtGetChild(0) instanceof ASTPrimaryPrefix) {
                             pPre = (ASTPrimaryPrefix)pExpr.jjtGetChild(0);
                         }
                         ASTResultType pRT = null;
                         if (pPre != null && pPre.jjtGetNumChildren() == 1 && pPre.jjtGetChild(0) instanceof ASTResultType) {
                             pRT = (ASTResultType)pPre.jjtGetChild(0);
                         }
                         ASTType pT = null;
                         if (pRT != null && pRT.jjtGetNumChildren() == 1 && pRT.jjtGetChild(0) instanceof ASTType) {
                             pT = (ASTType)pRT.jjtGetChild(0);
                         }
                         ASTReferenceType pRefT = null;
                         if (pT != null && pT.jjtGetNumChildren() == 1 && pT.jjtGetChild(0) instanceof ASTReferenceType) {
                             pRefT = (ASTReferenceType)pT.jjtGetChild(0);
                         }
                         ASTClassOrInterfaceType pN = null;
                         if (pRefT != null && pRefT.jjtGetNumChildren() == 1 && pRefT.jjtGetChild(0) instanceof ASTClassOrInterfaceType) {
                             pN = (ASTClassOrInterfaceType)pRefT.jjtGetChild(0);
                         }
                         if (pN != null && !isCorrectClass (node, pN)) {
                            addViolation(
                                     ctx, 
                                     node /*child.getBeginLine()*/, 
                                    MessageFormat.format(getMessage(), new Object[] {pN.getImage()}));
                         }
                     }
                 }
             }
         }
         super.visit(node, data);
         return data;
     }
     
     /** checks if symbol in name matches to class definition encapsulating node
      */
     private boolean isCorrectClass (ASTPrimaryExpression node, ASTClassOrInterfaceType name) {
         if (name == null || name.getImage() == null)
             return false;
         
         ASTClassOrInterfaceDeclaration clz = null;
         for (Node n = node; n != null; n = n.jjtGetParent()) {
             if (n instanceof ASTClassOrInterfaceDeclaration) {
                 clz = (ASTClassOrInterfaceDeclaration)n;
                 if (name.getImage ().indexOf (clz.getImage()) >= 0)
                     return true;
             }
         }
         return false;
     }
 }
