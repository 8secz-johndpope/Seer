 package org.intellij.erlang.psi.impl;
 
 import com.intellij.codeInsight.lookup.LookupElement;
 import com.intellij.codeInsight.lookup.LookupElementBuilder;
 import com.intellij.openapi.util.TextRange;
 import com.intellij.openapi.util.io.FileUtil;
 import com.intellij.openapi.util.text.StringUtil;
 import com.intellij.openapi.vfs.VirtualFile;
 import com.intellij.psi.PsiElement;
 import com.intellij.psi.PsiFile;
 import com.intellij.psi.PsiReference;
 import com.intellij.psi.ResolveState;
 import com.intellij.psi.scope.PsiScopeProcessor;
 import com.intellij.psi.util.PsiTreeUtil;
 import com.intellij.util.Function;
 import com.intellij.util.containers.ContainerUtil;
 import org.intellij.erlang.ErlangIcons;
 import org.intellij.erlang.psi.*;
 import org.jetbrains.annotations.NotNull;
 import org.jetbrains.annotations.Nullable;
 
 import java.io.IOException;
 import java.util.Collections;
 import java.util.List;
 
 public class ErlangPsiImplUtil {
   private ErlangPsiImplUtil() {
   }
 
   @SuppressWarnings("UnusedParameters")
   public static boolean processDeclarations(@NotNull ErlangQVar o, @NotNull PsiScopeProcessor processor, @NotNull ResolveState state, PsiElement lastParent, @NotNull PsiElement place) {
     return processor.execute(o, state);
   }
 
   @Nullable
   public static PsiReference getReference(@NotNull ErlangQVar o) {
     return new ErlangVariableReferenceImpl(o, TextRange.from(0, o.getTextLength()));
   }
 
   @Nullable
   public static PsiReference getReference(@NotNull ErlangFunctionCallExpression o) {
     PsiElement parent = o.getParent();
     ErlangModuleRef moduleReference = null;
     if (parent instanceof ErlangGlobalFunctionCallExpression) {
       moduleReference = ((ErlangGlobalFunctionCallExpression) parent).getModuleRef();
     }
     ErlangQAtom moduleAtom = moduleReference == null ? null : moduleReference.getQAtom();
     ErlangQAtom nameAtom = o.getQAtom();
 
     return new ErlangFunctionReferenceImpl<ErlangQAtom>(
       nameAtom, moduleAtom, TextRange.from(0, nameAtom.getTextLength()),
       nameAtom.getText(), o.getArgumentList().getExpressionList().size());
   }
 
   @NotNull
   public static PsiReference getReference(@NotNull ErlangExportFunction o) {
     PsiElement arity = o.getInteger();
     return new ErlangFunctionReferenceImpl<ErlangQAtom>(o.getQAtom(), null, TextRange.from(0, o.getQAtom().getTextLength()),
       o.getQAtom().getText(), StringUtil.parseInt(arity == null ? "" : arity.getText(), -1));
   }
 
   public static boolean inDefinition(PsiElement psiElement) {
     return PsiTreeUtil.getParentOfType(psiElement, ErlangArgumentDefinition.class) != null;
   }
 
   public static boolean inAtomAttribute(PsiElement psiElement) {
     return PsiTreeUtil.getParentOfType(psiElement, ErlangAtomAttribute.class) != null;
   }
 
   public static boolean isLeftPartOfAssignment(@NotNull PsiElement psiElement) {
     ErlangAssignmentExpression assignmentExpression = PsiTreeUtil.getParentOfType(psiElement, ErlangAssignmentExpression.class);
     if (assignmentExpression == null) return false;
     return PsiTreeUtil.isAncestor(assignmentExpression.getLeft(), psiElement, false);
   }
 
   @NotNull
   static List<LookupElement> getFunctionLookupElements(@NotNull PsiFile containingFile) {
     if (containingFile instanceof ErlangFile) {
       return ContainerUtil.map(((ErlangFile) containingFile).getFunctions(), new Function<ErlangFunction, LookupElement>() {
         @Override
         public LookupElement fun(@NotNull ErlangFunction function) {
           return LookupElementBuilder.create(function)
             .setIcon(ErlangIcons.FUNCTION).setTailText("/" + function.getArity());
         }
       });
     }
     return Collections.emptyList();
   }
 
   @NotNull
   public static List<LookupElement> getRecordLookupElements(@NotNull PsiFile containingFile) {
     if (containingFile instanceof ErlangFile) {
       return ContainerUtil.map(((ErlangFile) containingFile).getRecords(), new Function<ErlangRecordDefinition, LookupElement>() {
         @Override
         public LookupElement fun(@NotNull ErlangRecordDefinition rd) {
           return LookupElementBuilder.create(rd).setIcon(ErlangIcons.RECORD);
         }
       });
     }
     return Collections.emptyList();
   }
 
   @NotNull
   public static String getName(@NotNull ErlangFunction o) {
     return o.getAtomName().getAtom().getText();
   }
 
   @NotNull
   public static String getName(@NotNull ErlangQVar o) {
     return o.getText();
   }
 
   public static int getArity(@NotNull ErlangFunction o) {
     return o.getFunctionClauseList().get(0).getArgumentDefinitionList().size();
   }
 
   @NotNull
   public static String getName(@NotNull ErlangRecordDefinition o) {
     ErlangQAtom atom = o.getQAtom();
     if (atom == null) return "";
     return atom.getText();
   }
 
   @NotNull
   public static PsiElement getNameIdentifier(@NotNull ErlangRecordDefinition o) {
     ErlangQAtom atom = o.getQAtom();
     return atom != null ? atom : o;
   }
 
   public static int getTextOffset(@NotNull ErlangRecordDefinition o) {
     return o.getNameIdentifier().getTextOffset();
   }
 
   @NotNull
   public static PsiElement getNameIdentifier(@NotNull ErlangQVar o) {
     return o;
   }
 
   @NotNull
   public static PsiElement getNameIdentifier(@NotNull ErlangFunction o) {
     return o.getAtomName();
   }
 
   @Nullable
   public static PsiReference getReference(@NotNull ErlangRecordExpression o) {
     ErlangQAtom atom = o.getAtomName();
     if (atom == null) return null;
    return new ErlangRecordReferenceImpl<ErlangQAtom>(atom,
       TextRange.from(0, atom.getTextLength()), atom.getText());
   }
 
   @Nullable
   public static PsiReference getReference(@NotNull ErlangModuleRef o) {
     ErlangQAtom atom = o.getQAtom();
     return new ErlangModuleReferenceImpl<ErlangQAtom>(atom,
       TextRange.from(0, atom.getTextLength()), atom.getText() + ".erl");
   }
 
   @NotNull
   public static PsiElement setName(@NotNull ErlangFunction o, @NotNull String newName) {
     for (ErlangFunctionClause clause : o.getFunctionClauseList()) {
       clause.getQAtom().getAtom().replace(ErlangElementFactory.createQAtomFromText(o.getProject(), newName));
     }
     return o;
   }
 
   @NotNull
   public static PsiElement setName(@NotNull ErlangQVar o, @NotNull String newName) {
     o.replace(ErlangElementFactory.createQVarFromText(o.getProject(), newName));
     return o;
   }
 
   @NotNull
   public static PsiElement setName(@NotNull ErlangRecordDefinition o, @NotNull String newName) {
     ErlangQAtom atom = o.getQAtom();
     if (atom != null) {
       atom.getAtom().replace(ErlangElementFactory.createQAtomFromText(o.getProject(), newName));
     }
     return o;
   }
 
   @NotNull
   public static String getName(@NotNull ErlangModule o) {
     ErlangQAtom atom = o.getQAtom();
     return atom == null ? "" : atom.getText();
   }
 
   @NotNull
   public static PsiElement setName(@NotNull ErlangModule o, String newName) {
     VirtualFile virtualFile = o.getContainingFile().getVirtualFile();
     if (virtualFile != null) {
       try {
         String ext = FileUtil.getExtension(virtualFile.getName());
         virtualFile.rename(o, newName + "." + ext);
 
         ErlangQAtom atom = o.getQAtom();
         if (atom != null) {
           atom.getAtom().replace(ErlangElementFactory.createQAtomFromText(o.getProject(), newName));
         }
       } catch (IOException ignored) {
       }
     }
     return o;
   }
 
   @NotNull
   public static PsiElement getNameIdentifier(@NotNull ErlangModule o) {
     ErlangQAtom atom = o.getQAtom();
     return atom == null ? o : atom;
   }
 
   public static int getTextOffset(@NotNull ErlangModule o) {
     return o.getNameIdentifier().getTextOffset();
   }
 
   @NotNull
   public static PsiElement getNameIdentifier(@NotNull ErlangFunctionCallExpression o) {
     return o.getQAtom();
   }
 
   public static int getTextOffset(@NotNull ErlangFunctionCallExpression o) {
     return o.getQAtom().getTextOffset();
   }
 }
