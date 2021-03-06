 /*
  * Copyright 2010 Jon S Akhtar (Sylvanaar)
  *
  *   Licensed under the Apache License, Version 2.0 (the "License");
  *   you may not use this file except in compliance with the License.
  *   You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  *   Unless required by applicable law or agreed to in writing, software
  *   distributed under the License is distributed on an "AS IS" BASIS,
  *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *   See the License for the specific language governing permissions and
  *   limitations under the License.
  */
 
 package com.sylvanaar.idea.Lua.lang.psi.impl;
 
 import com.intellij.openapi.fileTypes.FileType;
 import com.intellij.psi.FileViewProvider;
 import com.intellij.psi.PsiElement;
 import com.intellij.psi.ResolveState;
 import com.intellij.psi.impl.PsiFileEx;
 import com.intellij.psi.impl.source.PsiFileWithStubSupport;
 import com.intellij.psi.scope.PsiScopeProcessor;
 import com.intellij.psi.search.GlobalSearchScope;
 import com.intellij.psi.search.ProjectAndLibrariesScope;
 import com.intellij.psi.util.CachedValue;
 import com.intellij.psi.util.CachedValueProvider;
 import com.intellij.psi.util.CachedValuesManager;
 import com.intellij.util.IncorrectOperationException;
 import com.sylvanaar.idea.Lua.LuaFileType;
 import com.sylvanaar.idea.Lua.lang.psi.*;
 import com.sylvanaar.idea.Lua.lang.psi.controlFlow.Instruction;
 import com.sylvanaar.idea.Lua.lang.psi.controlFlow.impl.ControlFlowBuilder;
 import com.sylvanaar.idea.Lua.lang.psi.expressions.LuaAnonymousFunctionExpression;
 import com.sylvanaar.idea.Lua.lang.psi.expressions.LuaDeclarationExpression;
 import com.sylvanaar.idea.Lua.lang.psi.expressions.LuaExpression;
 import com.sylvanaar.idea.Lua.lang.psi.expressions.LuaModuleExpression;
 import com.sylvanaar.idea.Lua.lang.psi.statements.*;
 import com.sylvanaar.idea.Lua.lang.psi.symbols.LuaCompoundIdentifier;
 import com.sylvanaar.idea.Lua.lang.psi.symbols.LuaIdentifier;
 import com.sylvanaar.idea.Lua.lang.psi.symbols.LuaLocalIdentifier;
 import com.sylvanaar.idea.Lua.lang.psi.visitor.LuaElementVisitor;
 import com.sylvanaar.idea.Lua.lang.psi.visitor.LuaRecursiveElementVisitor;
 import org.jetbrains.annotations.NotNull;
 import org.jetbrains.annotations.Nullable;
 
 import java.util.*;
 
 /**
  * Created by IntelliJ IDEA.
  * User: Jon S Akhtar
  * Date: Apr 10, 2010
  * Time: 12:19:03 PM
  */
 public class LuaPsiFileImpl extends LuaPsiFileBaseImpl implements LuaPsiFile, PsiFileWithStubSupport, PsiFileEx, LuaPsiFileBase, LuaExpressionCodeFragment {
     private boolean sdkFile;
 
     public LuaPsiFileImpl(FileViewProvider viewProvider) {
         super(viewProvider, LuaFileType.LUA_LANGUAGE);
     }
 
     @NotNull
     @Override
     public FileType getFileType() {
         return LuaFileType.LUA_FILE_TYPE;
     }
 
 
     @Override
     public String toString() {
         return "Lua script: " + getName();
     }
 
     @Override
     public GlobalSearchScope getFileResolveScope() {
         return new ProjectAndLibrariesScope(getProject());
     }
 
     @Override
     public boolean ignoreReferencedElementAccessibility() {
         return true;
     }
 
 
     @Override
     public LuaStatementElement addStatementBefore(@NotNull LuaStatementElement statement, LuaStatementElement anchor) throws IncorrectOperationException {
         return (LuaStatementElement) addBefore(statement, anchor);
     }
 
     List<LuaModuleExpression> moduleStatements;
 
     @Override
     @Nullable
     public String getModuleNameAtOffset(final int offset) {
         if (moduleStatements == null) {
             moduleStatements = new ArrayList<LuaModuleExpression>();
             acceptChildren(new LuaModuleVisitor(moduleStatements));
         }
 
         if (moduleStatements.size() == 0) return null;
 
         LuaModuleExpression module = null;
         for (LuaModuleExpression m : moduleStatements) {
             if (m.getIncludedTextRange().contains(offset)) module = module == null ? m :
                     m.getIncludedTextRange().getStartOffset() >
                     module.getIncludedTextRange().getStartOffset() ? m : module;
         }
 
        return module == null ? null : module.getName();
     }
 
 
     @Override
     public void clearCaches() {
         super.clearCaches();
         moduleStatements = null;
     }
 
     @Override
     public LuaExpression getReturnedValue() {
         // This only works for the last statement in the file
         LuaStatementElement[] stmts = getStatements();
         if (stmts.length==0) return null;
 
         LuaStatementElement s = stmts[stmts.length-1];
         if (! (s instanceof LuaReturnStatement)) return null;
 
         return ((LuaReturnStatement) s).getReturnValue();
     }
 
 
     @Override
     public boolean isSdkFile() {
        // LuaModuleUtil.checkForSdkFile(this, getProject());
         return sdkFile;
     }
 
     @Override
     public void setSdkFile(boolean b) {
         sdkFile = b;
     }
 
 
     @Override
     public void removeVariable(LuaIdentifier variable) {
 
     }
 
     @Override
     public LuaDeclarationStatement addVariableDeclarationBefore(LuaDeclarationStatement declaration, LuaStatementElement anchor) throws IncorrectOperationException {
         return null;
     }
 
     @Override
     public boolean processDeclarations(PsiScopeProcessor processor,
                                                    ResolveState state, PsiElement lastParent,
                                                    PsiElement place) {
         PsiElement run = lastParent == null ? getLastChild() : lastParent.getPrevSibling();
         if (run != null && run.getParent() != this) run = null;
         while (run != null) {
             if (!run.processDeclarations(processor, state, null, place)) return false;
             run = run.getPrevSibling();
         }
 
         return true;
     }
 
     public void accept(LuaElementVisitor visitor) {
         visitor.visitFile(this);
     }
 
     public void acceptChildren(LuaElementVisitor visitor) {
         PsiElement child = getFirstChild();
         while (child != null) {
             if (child instanceof LuaPsiElement) {
                 ((LuaPsiElement) child).accept(visitor);
             }
 
             child = child.getNextSibling();
         }
     }
 
 
     @Override
     public LuaDeclarationExpression[] getSymbolDefs() {
         final Set<LuaDeclarationExpression> decls =
                 new HashSet<LuaDeclarationExpression>();
 
         LuaElementVisitor v = new LuaRecursiveElementVisitor() {
             public void visitDeclarationExpression(LuaDeclarationExpression e) {
                 super.visitDeclarationExpression(e);
                 if (!(e instanceof LuaLocalIdentifier))
                     decls.add(e);
             }
 
             @Override
             public void visitCompoundIdentifier(LuaCompoundIdentifier e) {
                 super.visitCompoundIdentifier(e);
 
                 if (e.isAssignedTo())
                     decls.add(e);
             }
         };
 
         v.visitElement(this);
 
         return decls.toArray(new LuaDeclarationExpression[decls.size()]);
     }
 
 
     @Override
     public LuaStatementElement[] getAllStatements() {
                 final List<LuaStatementElement> stats =
                 new ArrayList<LuaStatementElement>();
 
         LuaElementVisitor v = new LuaRecursiveElementVisitor() {
             public void visitElement(LuaPsiElement e) {
                 super.visitElement(e);
                 if (e instanceof LuaStatementElement)
                     stats.add((LuaStatementElement) e);
             }
         };
 
         v.visitElement(this);
 
         return stats.toArray(new LuaStatementElement[stats.size()]);
     }
 
     @Override
     public LuaStatementElement[] getStatements() {
          return findChildrenByClass(LuaStatementElement.class);
     }
 
     @Override
     public LuaFunctionDefinition[] getFunctionDefs() {
         final List<LuaFunctionDefinition> funcs =
                 new ArrayList<LuaFunctionDefinition>();
 
         LuaElementVisitor v = new LuaRecursiveElementVisitor() {
             public void visitFunctionDef(LuaFunctionDefinitionStatement e) {
                 super.visitFunctionDef(e);
                 funcs.add(e);
             }
 
             @Override
             public void visitAnonymousFunction(LuaAnonymousFunctionExpression e) {
                 super.visitAnonymousFunction(e);
                 if (e.getName() != null)
                     funcs.add(e);
             }
         };
 
         v.visitElement(this);
 
         return funcs.toArray(new LuaFunctionDefinition[funcs.size()]);
     }
 
     @Override
     public Instruction[] getControlFlow() {
         assert isValid();
         CachedValue<Instruction[]> controlFlow = getUserData(CONTROL_FLOW);
         if (controlFlow == null) {
             controlFlow = CachedValuesManager.getManager(getProject()).createCachedValue(
                     new CachedValueProvider<Instruction[]>() {
                         @Override
                         public Result<Instruction[]> compute() {
                             return Result
                                     .create(new ControlFlowBuilder(getProject()).buildControlFlow(LuaPsiFileImpl.this),
                                             getContainingFile());
                         }
                     }, false);
             putUserData(CONTROL_FLOW, controlFlow);
         }
 
         return controlFlow.getValue();
     }
 
     // Only looks at the current block
     private class LuaModuleVisitor extends LuaElementVisitor {
         private final Collection<LuaModuleExpression> list;
 
         public LuaModuleVisitor(Collection<LuaModuleExpression> list) {this.list = list;}
 
         @Override
         public void visitFunctionCallStatement(LuaFunctionCallStatement e) {
             super.visitFunctionCallStatement(e);
             e.acceptChildren(this);
         }
 
         @Override
         public void visitModuleExpression(LuaModuleExpression e) {
             super.visitModuleExpression(e);
             list.add(e);
         }
     }
 }
