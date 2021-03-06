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
 
 package com.sylvanaar.idea.Lua.editor.completion;
 
 
 import com.intellij.codeInsight.completion.*;
 import com.intellij.openapi.diagnostic.Logger;
 import com.intellij.openapi.util.TextRange;
 import com.intellij.patterns.ElementPattern;
 import com.intellij.patterns.PlatformPatterns;
 import com.intellij.psi.PsiElement;
 import com.intellij.util.ProcessingContext;
 import com.sylvanaar.idea.Lua.lang.psi.LuaPsiFile;
 import com.sylvanaar.idea.Lua.lang.psi.expressions.LuaFieldIdentifier;
 import com.sylvanaar.idea.Lua.lang.psi.impl.statements.LuaFunctionDefinitionStatementImpl;
 import com.sylvanaar.idea.Lua.lang.psi.stubs.index.LuaFieldIndex;
 import com.sylvanaar.idea.Lua.lang.psi.stubs.index.LuaGlobalDeclarationIndex;
 import com.sylvanaar.idea.Lua.lang.psi.symbols.LuaCompoundIdentifier;
 import com.sylvanaar.idea.Lua.lang.psi.symbols.LuaGlobalIdentifier;
 import com.sylvanaar.idea.Lua.lang.psi.symbols.LuaIdentifier;
 import com.sylvanaar.idea.Lua.lang.psi.symbols.LuaSymbol;
 import com.sylvanaar.idea.Lua.lang.psi.visitor.LuaRecursiveElementVisitor;
 import com.sylvanaar.idea.Lua.options.LuaApplicationSettings;
 import org.jetbrains.annotations.NotNull;
 
 import java.util.HashSet;
 import java.util.Set;
 
 import static com.intellij.patterns.PlatformPatterns.psiElement;
 
 public class LuaCompletionContributor extends DefaultCompletionContributor {
     private static final Logger log = Logger.getInstance("#Lua.CompletionContributor");
 
     private static final ElementPattern<PsiElement> AFTER_SELF_DOT = psiElement().withParent(LuaCompoundIdentifier.class).afterSibling(psiElement().withName("self"));
     private static final ElementPattern<PsiElement> AFTER_DOT = psiElement().afterLeaf(".", ":");
 
     private static final ElementPattern<PsiElement> AFTER_FUNCTION = psiElement().afterLeafSkipping(psiElement().whitespace(), PlatformPatterns.string().matches("function"));
     
     private static final ElementPattern<PsiElement> NOT_AFTER_DOT = psiElement().withParent(LuaIdentifier.class).andNot(psiElement().afterLeaf(".", ":"));
 
 
     private static final ElementPattern<PsiElement> AFTER_SELF =
             psiElement().withParent(LuaSymbol.class).afterLeaf(":",".");
 
     public LuaCompletionContributor() {
         extend(CompletionType.BASIC, NOT_AFTER_DOT, new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
                 for (String s : LuaKeywordsManager.getKeywords())
                     result.addElement(new LuaLookupElement(s));
             }
         });
 
 
         extend(CompletionType.BASIC, AFTER_FUNCTION, new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
                 String prefix = result.getPrefixMatcher().getPrefix();
                 int prefixLen = prefix.length();
 
                 for (String key : LuaGlobalDeclarationIndex.getInstance()
                         .getAllKeys(parameters.getOriginalFile().getProject())) {
 
                     if (key.length() > prefixLen && key.startsWith(prefix))
                         result.addElement(new LuaLookupElement(key));
                 }
             }
         });
         extend(CompletionType.BASIC, NOT_AFTER_DOT, new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
                 if (!LuaApplicationSettings.getInstance().INCLUDE_ALL_FIELDS_IN_COMPLETIONS) return;
 
 
                 LuaPsiFile file = (LuaPsiFile) parameters.getOriginalFile();
 
                 globalUsageVisitor.reset();
 
                 file.acceptChildren(globalUsageVisitor);
                 String prefix = result.getPrefixMatcher().getPrefix();
                 int prefixLen = prefix.length();
                 for (String key : globalUsageVisitor.getResult()) {
 
                     if (key.length() > prefixLen && key.startsWith(prefix))
                         result.addElement(new LuaLookupElement(key));
                 }
             }
         });
 
         extend(CompletionType.BASIC, AFTER_SELF, new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context, @NotNull CompletionResultSet result) {
                 PsiElement element = parameters.getPosition();
 
                 try {
                     LuaCompoundIdentifier cid = (LuaCompoundIdentifier) element.getContext().getContext();
 
                     if (!cid.getLeftSymbol().equals("self"))
                         return;
                 } catch (Exception e) {
                     return;
                 }
 
                 while (!(element instanceof LuaFunctionDefinitionStatementImpl) && element != null)
                     element = element.getContext();
 
                 // Must be inside a function
                 if (element == null) return;
 
                 LuaFunctionDefinitionStatementImpl func = (LuaFunctionDefinitionStatementImpl) element;
 
                 LuaSymbol symbol = func.getIdentifier();
 
                 int colonIdx = symbol.getText().lastIndexOf(':');
                 int dotIdx = symbol.getText().lastIndexOf('.');
                 if (colonIdx < 0 && dotIdx < 0) return;
 
                 int idx = Math.max(colonIdx, dotIdx);
 
                 String prefix = symbol.getText().substring(0, idx+1);
 
                 for(String key : LuaGlobalDeclarationIndex.getInstance().getAllKeys(element.getProject())) {
 //                    System.out.println(key);
 
                     if (key.startsWith(prefix)) {
                         result.addElement(new LuaLookupElement("self:"+key.substring(prefix.length())));
                         result.addElement(new LuaLookupElement("self."+key.substring(prefix.length())));
                     }
                 }
 
                 fieldVisitor.reset();
 
                 ((LuaPsiFile)parameters.getOriginalFile()).accept(fieldVisitor);
 
                 for (String s : fieldVisitor.getResult()) {
                     if (s.startsWith(prefix)) {
                         result.addElement(new LuaLookupElement("self:"+s));
                         result.addElement(new LuaLookupElement("self."+s));
                     }
                 }
                 
 
             }
         });
 
 
 
         extend(CompletionType.BASIC, AFTER_DOT, new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
 
                 if (!LuaApplicationSettings.getInstance().INCLUDE_ALL_FIELDS_IN_COMPLETIONS)
                     return;
 
 //                fieldVisitor.reset();
 //
 //                ((LuaPsiFile)parameters.getOriginalFile()).accept(fieldVisitor);
 //
 //                String prefix = null;
 //                try {
 //                    LuaCompoundIdentifier cid = (LuaCompoundIdentifier) parameters.getPosition().getContext().getContext();
 //                    prefix = cid.getLeftSymbol().getText();
 //                } catch (Exception e) {
 //                    return;
 //                }
 
                 String prefix = result.getPrefixMatcher().getPrefix();
                 String matchPrefix = null;
 
                 for(int i=prefix.length()-1; i>=0; i--)
                     if (prefix.charAt(i) == '.' || prefix.charAt(i) == ':') {
                         matchPrefix = prefix.substring(i+1,  prefix.length());
                         prefix = prefix.substring(0, i+1);
                         break;
                     }
 
                 for(String key : LuaFieldIndex.getInstance().getAllKeys(parameters.getOriginalFile().getProject())) {
 //                    System.out.println(key);
 
                    if (matchPrefix != null && key.startsWith(matchPrefix))
                         result.addElement(new LuaLookupElement(prefix + key));
                 }
 
 
 //                for (String s : fieldVisitor.getResult()) {
 //                    assert s.length() > 0;
 //                    result.addElement(new LuaLookupElement(prefix + "." + s));
 //                    result.addElement(new LuaLookupElement(prefix + ":" + s));
 //                }
             }
         });
 
     }
 
     @Override
     public void beforeCompletion(@NotNull CompletionInitializationContext context) {
         int end = context.getIdentifierEndOffset();
         int start = context.getStartOffset();
         String identifierToReplace = context.getEditor().getDocument().getText(new TextRange(start-1, end));
 
         if (identifierToReplace.charAt(0) == '.' || identifierToReplace.charAt(0) == ':')
             context.setReplacementOffset(start);
 
         super.beforeCompletion(context);
     }
 
     @Override
     public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
         super.fillCompletionVariants(parameters, result);    //To change body of overridden methods use File | Settings | File Templates.
     }
 
     LuaFieldElementVisitor fieldVisitor = new LuaFieldElementVisitor();
     LuaGlobalUsageVisitor globalUsageVisitor = new LuaGlobalUsageVisitor();
 
     private static class LuaFieldElementVisitor extends LuaRecursiveElementVisitor {
         Set<String> result = new HashSet<String>();
 
         @Override
         public void visitIdentifier(LuaIdentifier e) {
             super.visitIdentifier(e);
 
             if (e instanceof LuaFieldIdentifier && e.getTextLength() > 0 && e.getText().charAt(0) != '[' && e.getName() != null)
                 result.add(e.getName());
 
         }
 
         public Set<String> getResult() {
             return result;
         }
 
         public void reset() { result.clear(); }
     }
 
 
     private static class LuaGlobalUsageVisitor extends LuaRecursiveElementVisitor {
         Set<String> result = new HashSet<String>();
 
         @Override
         public void visitIdentifier(LuaIdentifier e) {
             super.visitIdentifier(e);
 
             if (e instanceof LuaGlobalIdentifier && e.getTextLength() > 0 && e.getName() != null)
                 result.add(e.getName());
         }
 
         public Set<String> getResult() {
             return result;
         }
 
         public void reset() { result.clear(); }
     }
 }
