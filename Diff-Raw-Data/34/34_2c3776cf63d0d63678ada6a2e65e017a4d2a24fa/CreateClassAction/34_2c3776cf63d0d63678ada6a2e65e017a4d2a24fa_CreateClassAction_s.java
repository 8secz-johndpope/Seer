 package com.intellij.plugins.haxe.ide.actions;
 
 import com.intellij.ide.IdeBundle;
 import com.intellij.ide.actions.CreateFileFromTemplateDialog;
 import com.intellij.ide.actions.CreateTemplateInPackageAction;
 import com.intellij.ide.fileTemplates.FileTemplate;
 import com.intellij.ide.fileTemplates.FileTemplateManager;
 import com.intellij.ide.fileTemplates.FileTemplateUtil;
 import com.intellij.openapi.project.Project;
 import com.intellij.openapi.roots.impl.DirectoryIndex;
 import com.intellij.plugins.haxe.HaxeBundle;
 import com.intellij.plugins.haxe.HaxeIcons;
 import com.intellij.plugins.haxe.ide.HaxeFileTemplateUtil;
 import com.intellij.psi.PsiDirectory;
 import com.intellij.psi.PsiElement;
 import com.intellij.psi.PsiFile;
 import com.intellij.util.IncorrectOperationException;
 import org.jetbrains.annotations.NotNull;
 
 import javax.swing.*;
 import java.util.Properties;
 
 /**
  * @author: Fedor.Korotkov
  */
 public class CreateClassAction extends CreateTemplateInPackageAction<PsiFile> {
   public CreateClassAction() {
     super(HaxeBundle.message("action.create.new.class"), HaxeBundle.message("action.create.new.class"), HaxeIcons.HAXE_ICON_16x16, true);
   }
 
   @Override
   protected PsiElement getNavigationElement(@NotNull PsiFile createdElement) {
     return createdElement.getNavigationElement();
   }
 
   @Override
   protected boolean checkPackageExists(PsiDirectory directory) {
     return DirectoryIndex.getInstance(directory.getProject()).getPackageName(directory.getVirtualFile()) != null;
   }
 
   @Override
   protected String getActionName(PsiDirectory directory, String newName, String templateName) {
     return HaxeBundle.message("progress.creating.class", newName);
   }
 
   @Override
   protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
     builder.setTitle(IdeBundle.message("action.create.new.class"));
     for (FileTemplate fileTemplate : HaxeFileTemplateUtil.getApplicableTemplates()) {
       final String templateName = fileTemplate.getName();
       final String shortName = HaxeFileTemplateUtil.getTemplateShortName(templateName);
       final Icon icon = HaxeFileTemplateUtil.getTemplateIcon(templateName);
       builder.addKind(shortName, icon, templateName);
     }
   }
 
   @Override
   protected PsiFile doCreate(PsiDirectory dir, String className, String templateName) throws IncorrectOperationException {
     String packageName = DirectoryIndex.getInstance(dir.getProject()).getPackageName(dir.getVirtualFile());
     try {
       return createClass(className, packageName, dir, templateName).getContainingFile();
     }
     catch (Exception e) {
       throw new IncorrectOperationException(e.getMessage(), e);
     }
   }
 
   private static PsiElement createClass(String className, String packageName, PsiDirectory directory, final String templateName)
     throws Exception {
     final Properties props = new Properties(FileTemplateManager.getInstance().getDefaultProperties());
     props.setProperty(FileTemplate.ATTRIBUTE_NAME, className);
     props.setProperty(FileTemplate.ATTRIBUTE_PACKAGE_NAME, packageName);
 
     final FileTemplate template = FileTemplateManager.getInstance().getInternalTemplate(templateName);
 
     return FileTemplateUtil.createFromTemplate(template, className, props, directory, CreateClassAction.class.getClassLoader());
   }
 }
