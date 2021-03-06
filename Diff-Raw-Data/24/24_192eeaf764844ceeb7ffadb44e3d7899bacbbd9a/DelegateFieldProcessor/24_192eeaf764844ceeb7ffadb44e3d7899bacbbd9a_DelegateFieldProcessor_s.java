 package de.plushnikov.intellij.plugin.processor.field;
 
 import com.intellij.openapi.project.Project;
 import com.intellij.openapi.util.Pair;
 import com.intellij.psi.PsiAnnotation;
 import com.intellij.psi.PsiClass;
 import com.intellij.psi.PsiClassType;
 import com.intellij.psi.PsiElement;
 import com.intellij.psi.PsiField;
 import com.intellij.psi.PsiManager;
 import com.intellij.psi.PsiMethod;
 import com.intellij.psi.PsiModifier;
 import com.intellij.psi.PsiParameter;
 import com.intellij.psi.PsiParameterList;
 import com.intellij.psi.PsiReferenceList;
 import com.intellij.psi.PsiSubstitutor;
 import com.intellij.psi.PsiType;
 import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.light.LightTypeParameter;
 import com.intellij.psi.search.GlobalSearchScope;
 import com.intellij.psi.util.PsiUtil;
 import com.intellij.psi.util.TypeConversionUtil;
 import de.plushnikov.intellij.plugin.extension.UserMapKeys;
 import de.plushnikov.intellij.plugin.problem.ProblemBuilder;
 import de.plushnikov.intellij.plugin.psi.LombokLightMethodBuilder;
 import de.plushnikov.intellij.plugin.util.PsiAnnotationUtil;
 import de.plushnikov.intellij.plugin.util.PsiElementUtil;
 import de.plushnikov.intellij.plugin.util.PsiMethodUtil;
 import lombok.Delegate;
 import org.apache.commons.lang.StringUtils;
 import org.jetbrains.annotations.NotNull;
 import org.jetbrains.annotations.Nullable;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 
 /**
  * Inspect and validate @Delegate lombok annotation on a field
  * Creates delegation methods for this field
  *
  * @author Plushnikov Michail
  */
 public class DelegateFieldProcessor extends AbstractFieldProcessor {
 
   public DelegateFieldProcessor() {
     super(Delegate.class, PsiMethod.class);
   }
 
   @Override
   protected boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiField psiField, @NotNull ProblemBuilder builder) {
     boolean result = true;
 
     final PsiClass psiClass = psiField.getContainingClass();
     if (null == psiClass) {
       result = false;
     }
 
     final Collection<PsiType> types = collectDelegateTypes(psiAnnotation, psiField);
     result &= validateTypes(types, builder);
 
     final Collection<PsiType> excludes = collectExcludeTypes(psiAnnotation);
     result &= validateTypes(excludes, builder);
 
     //TODO Error: delegation of methods that doesn't exists on type
     return result;
   }
 
   private boolean validateTypes(Collection<PsiType> psiTypes, ProblemBuilder builder) {
     boolean result = true;
     for (PsiType type : psiTypes) {
       if (!checkConcreteClass(type)) {
         builder.addError("'@Delegate' can only use concrete class types, not wildcards, arrays, type variables, or primitives. '%s' is wrong class type",
             type.getCanonicalText());
         result = false;
       }
     }
     return result;
   }
 
   private boolean checkConcreteClass(@NotNull PsiType psiType) {
     if (psiType instanceof PsiClassType) {
       PsiClass psiClass = ((PsiClassType) psiType).resolve();
       return !(psiClass instanceof PsiTypeParameter);
     }
     return false;
   }
 
   protected void generatePsiElements(@NotNull PsiField psiField, @NotNull PsiAnnotation psiAnnotation, @NotNull List<? super PsiElement> target) {
     final Project project = psiField.getProject();
     final PsiManager manager = psiField.getContainingFile().getManager();
 
     final Collection<Pair<PsiMethod, PsiSubstitutor>> includesMethods = new HashSet<Pair<PsiMethod, PsiSubstitutor>>();
 
     final Collection<PsiType> types = collectDelegateTypes(psiAnnotation, psiField);
     addMethodsOfTypes(types, includesMethods);
 
     final Collection<Pair<PsiMethod, PsiSubstitutor>> excludeMethods = new HashSet<Pair<PsiMethod, PsiSubstitutor>>();
     PsiClassType javaLangObjectType = PsiType.getJavaLangObject(manager, GlobalSearchScope.allScope(project));
     addMethodsOfType(javaLangObjectType, excludeMethods);
 
     final Collection<PsiType> excludes = collectExcludeTypes(psiAnnotation);
     addMethodsOfTypes(excludes, excludeMethods);
 
     final Collection<Pair<PsiMethod, PsiSubstitutor>> methodsToDelegate = findMethodsToDelegate(includesMethods, excludeMethods);
     if (!methodsToDelegate.isEmpty()) {
       final PsiClass psiClass = psiField.getContainingClass();
       for (Pair<PsiMethod, PsiSubstitutor> pair : methodsToDelegate) {
         target.add(generateDelegateMethod(psiClass, psiField, psiAnnotation, pair.getFirst(), pair.getSecond()));
       }
       UserMapKeys.addGeneralUsageFor(psiField);
       UserMapKeys.addReadUsageFor(psiField);
     }
   }
 
   private Collection<PsiType> collectDelegateTypes(PsiAnnotation psiAnnotation, PsiField psiField) {
     Collection<PsiType> types = PsiAnnotationUtil.getAnnotationValues(psiAnnotation, "types", PsiType.class);
     if (types.isEmpty()) {
       final PsiType psiType = psiField.getType();
       types = Collections.singletonList(psiType);
     }
     return types;
   }
 
   private Collection<PsiType> collectExcludeTypes(PsiAnnotation psiAnnotation) {
     return PsiAnnotationUtil.getAnnotationValues(psiAnnotation, "excludes", PsiType.class);
   }
 
   private void addMethodsOfTypes(Collection<PsiType> types, Collection<Pair<PsiMethod, PsiSubstitutor>> includesMethods) {
     for (PsiType type : types) {
       addMethodsOfType(type, includesMethods);
     }
   }
 
   private PsiSubstitutor getSubstitutor(PsiType type) {
     final PsiClassType.ClassResolveResult resolveResult = PsiUtil.resolveGenericsClassInType(type);
     PsiSubstitutor psiSubstitutor = resolveResult.getSubstitutor();
     if (type instanceof PsiClassType) {
       final PsiClass psiClass = resolveResult.getElement();
       if (psiClass instanceof PsiTypeParameter) {
         for (PsiClass aClass : psiClass.getSupers()) {
           psiSubstitutor = psiSubstitutor.putAll(TypeConversionUtil.getSuperClassSubstitutor(aClass, (PsiClassType) type));
         }
       }
     }
     return psiSubstitutor;
   }
 
   private void addMethodsOfType(PsiType psiType, Collection<Pair<PsiMethod, PsiSubstitutor>> allMethods) {
     final PsiSubstitutor classSubstitutor = getSubstitutor(psiType);
 
     PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
     if (null != psiClass) {
       List<Pair<PsiMethod, PsiSubstitutor>> acceptedMethods = psiClass.getAllMethodsAndTheirSubstitutors();
       for (Pair<PsiMethod, PsiSubstitutor> pair : acceptedMethods) {
         PsiMethod psiMethod = pair.getFirst();
         if (!psiMethod.isConstructor() && psiMethod.hasModifierProperty(PsiModifier.PUBLIC) && !psiMethod.hasModifierProperty(PsiModifier.STATIC)) {
           PsiSubstitutor combinedSubstitutor = pair.getSecond().putAll(classSubstitutor);
           allMethods.add(new Pair<PsiMethod, PsiSubstitutor>(psiMethod, combinedSubstitutor));
         }
       }
     }
   }
 
   private void removeDuplicateMethods(Collection<Pair<PsiMethod, PsiSubstitutor>> allMethods) {
     Collection<Pair<PsiMethod, PsiSubstitutor>> processedMethods = new ArrayList<Pair<PsiMethod, PsiSubstitutor>>();
     Iterator<Pair<PsiMethod, PsiSubstitutor>> iterator = allMethods.iterator();
     while (iterator.hasNext()) {
       Pair<PsiMethod, PsiSubstitutor> pair = iterator.next();
       boolean acceptMethod = true;
       for (Pair<PsiMethod, PsiSubstitutor> uniquePair : processedMethods) {
         if (PsiElementUtil.methodMatches(pair, uniquePair)) {
           acceptMethod = false;
           break;
         }
       }
       if (acceptMethod) {
         processedMethods.add(pair);
       } else {
         iterator.remove();
       }
     }
   }
 
   private Collection<Pair<PsiMethod, PsiSubstitutor>> findMethodsToDelegate(Collection<Pair<PsiMethod, PsiSubstitutor>> includesMethods, Collection<Pair<PsiMethod, PsiSubstitutor>> excludeMethods) {
     removeDuplicateMethods(includesMethods);
     removeDuplicateMethods(excludeMethods);
 
     if (excludeMethods.isEmpty()) {
       return includesMethods;
     }
 
     Collection<Pair<PsiMethod, PsiSubstitutor>> result = new ArrayList<Pair<PsiMethod, PsiSubstitutor>>();
     for (Pair<PsiMethod, PsiSubstitutor> includesMethodPair : includesMethods) {
       boolean acceptMethod = true;
       for (Pair<PsiMethod, PsiSubstitutor> excludeMethodPair : excludeMethods) {
         if (PsiElementUtil.methodMatches(includesMethodPair, excludeMethodPair)) {
           acceptMethod = false;
           break;
         }
       }
       if (acceptMethod) {
         result.add(includesMethodPair);
       }
     }
     return result;
   }
 
   @NotNull
   private PsiMethod generateDelegateMethod(@NotNull PsiClass psiClass, @NotNull PsiField psiField, @NotNull PsiAnnotation psiAnnotation, @NotNull PsiMethod psiMethod, @Nullable PsiSubstitutor psiSubstitutor) {
     final PsiType returnType = null == psiSubstitutor ? psiMethod.getReturnType() : psiSubstitutor.substitute(psiMethod.getReturnType());
 
     LombokLightMethodBuilder methodBuilder = new LombokLightMethodBuilder(psiClass.getManager(), psiMethod.getName())
         .withModifier(PsiModifier.PUBLIC)
         .withMethodReturnType(returnType)
         .withContainingClass(psiClass)
         .withNavigationElement(psiAnnotation);
 
     for (PsiTypeParameter typeParameter : psiMethod.getTypeParameters()) {
      methodBuilder.withTypeParameter(new LightTypeParameter(typeParameter));
     }
 
     final PsiReferenceList throwsList = psiMethod.getThrowsList();
     for (PsiClassType psiClassType : throwsList.getReferencedTypes()) {
       methodBuilder.withException(psiClassType);
     }
 
     final PsiParameterList parameterList = psiMethod.getParameterList();
 
     final StringBuilder paramString = new StringBuilder();
     int parameterIndex = 0;
     for (PsiParameter psiParameter : parameterList.getParameters()) {
       final PsiType psiParameterType = null == psiSubstitutor ? psiParameter.getType() : psiSubstitutor.substitute(psiParameter.getType());
       String psiParameterName = psiParameter.getName();
       String generatedParameterName = StringUtils.defaultIfEmpty(psiParameterName, "p" + parameterIndex);
       methodBuilder.withParameter(generatedParameterName, psiParameterType);
       parameterIndex++;
 
       paramString.append(generatedParameterName).append(',');
     }
 
     final boolean isStatic = psiField.hasModifierProperty(PsiModifier.STATIC);
     if (paramString.length() > 0) {
       paramString.deleteCharAt(paramString.length() - 1);
     }
     methodBuilder.withBody(PsiMethodUtil.createCodeBlockFromText(
         String.format("%s%s.%s.%s(%s);",
             PsiType.VOID.equals(returnType) ? "" : "return ",
             isStatic ? psiClass.getName() : "this",
             psiField.getName(),
             psiMethod.getName(),
             paramString.toString()),
         psiClass));
 
     return methodBuilder;
   }
 }
