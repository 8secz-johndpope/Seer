 package de.plushnikov.intellij.lombok.processor.clazz.log;
 
 import com.intellij.openapi.project.Project;
 import com.intellij.psi.JavaPsiFacade;
 import com.intellij.psi.PsiAnnotation;
 import com.intellij.psi.PsiClass;
 import com.intellij.psi.PsiElement;
 import com.intellij.psi.PsiField;
 import com.intellij.psi.PsiManager;
 import com.intellij.psi.PsiMethod;
 import com.intellij.psi.PsiModifier;
 import com.intellij.psi.PsiType;
 import com.intellij.psi.impl.light.LightElement;
 import de.plushnikov.intellij.lombok.processor.clazz.AbstractLombokClassProcessor;
 import de.plushnikov.intellij.lombok.psi.MyLightFieldBuilder;
 import org.jetbrains.annotations.NotNull;
 
 import java.util.List;
 
 /**
  * Base lombok processor class for logger processing
  *
  * @author Plushnikov Michail
  */
 public abstract class AbstractLogProcessor extends AbstractLombokClassProcessor {
 
   private final static String loggerName = "log";
   private final String loggerType;
   private final String loggerInitializer;//TODO add Initializer support
 
   protected AbstractLogProcessor(@NotNull String supportedAnnotation, @NotNull String loggerType, @NotNull String loggerInitializer) {
     super(supportedAnnotation, PsiField.class);
     this.loggerType = loggerType;
     this.loggerInitializer = loggerInitializer;
   }
 
   public <Psi extends PsiElement> void process(@NotNull PsiClass psiClass, @NotNull PsiMethod[] classMethods, @NotNull PsiAnnotation psiAnnotation, @NotNull List<Psi> target) {
     if (!hasFieldByName(psiClass, loggerName)) {
       Project project = psiClass.getProject();
       PsiManager manager = psiClass.getContainingFile().getManager();
 
       PsiType psiLoggerType = JavaPsiFacade.getElementFactory(project).createTypeFromText(loggerType, psiClass);
       LightElement loggerField = new MyLightFieldBuilder(manager, loggerName, psiLoggerType)
           .setHasInitializer(true)
           .setContainingClass(psiClass)
          .setModifiers(PsiModifier.FINAL, PsiModifier.STATIC, PsiModifier.PUBLIC)
           .setNavigationElement(psiAnnotation);
 
       target.add((Psi) loggerField);
     } else {
       //TODO create warning in code
       //Not generating fieldName(): A field with that name already exists
     }
   }
 
   protected boolean hasFieldByName(@NotNull PsiClass psiClass, String... fieldNames) {
     final PsiField[] psiFields = collectClassFieldsIntern(psiClass);
     for (PsiField psiField : psiFields) {
       for (String fieldName : fieldNames) {
         if (psiField.getName().equals(fieldName)) {
           return true;
         }
       }
     }
     return false;
   }
 }
