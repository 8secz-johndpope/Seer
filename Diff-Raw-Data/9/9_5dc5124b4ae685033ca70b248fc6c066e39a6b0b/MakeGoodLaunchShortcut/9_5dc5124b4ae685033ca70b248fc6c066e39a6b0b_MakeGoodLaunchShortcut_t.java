 package com.piece_framework.makegood.ui.launch;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IFolder;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.debug.core.DebugPlugin;
 import org.eclipse.debug.core.ILaunch;
 import org.eclipse.debug.core.ILaunchConfigurationType;
 import org.eclipse.debug.core.ILaunchListener;
 import org.eclipse.debug.core.ILaunchManager;
 import org.eclipse.dltk.core.IMethod;
 import org.eclipse.dltk.core.IModelElement;
 import org.eclipse.dltk.core.IProjectFragment;
 import org.eclipse.dltk.core.IScriptFolder;
 import org.eclipse.dltk.core.ISourceModule;
 import org.eclipse.dltk.core.IType;
 import org.eclipse.dltk.core.ModelException;
 import org.eclipse.dltk.core.ScriptModelUtil;
 import org.eclipse.dltk.internal.ui.editor.EditorUtility;
 import org.eclipse.jface.text.ITextSelection;
 import org.eclipse.jface.viewers.ISelection;
 import org.eclipse.jface.viewers.ISelectionProvider;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.jface.window.Window;
 import org.eclipse.php.internal.debug.ui.launching.PHPExeLaunchShortcut;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.ui.IEditorPart;
 import org.eclipse.ui.internal.dialogs.PropertyDialog;
 import org.eclipse.ui.texteditor.ITextEditor;
 
 import com.piece_framework.makegood.core.MakeGoodProperty;
 import com.piece_framework.makegood.core.PHPResource;
 
 public class MakeGoodLaunchShortcut extends PHPExeLaunchShortcut {
     public static int RUN_TEST_ON_CURSOR = 1;
     public static int RUN_TESTS_ON_CLASS = 2;
     public static int RUN_TESTS_ON_FILE = 3;
 
     private ILaunchListener launchListener;
     private IFolder selectedFolder;
     private IType selectedType;
     private IMethod selectedMethod;
     private int runLevelOnEditor = RUN_TEST_ON_CURSOR;
 
     public void setRunLevelOnEditor(int runLevel) {
         this.runLevelOnEditor = runLevel;
     }
 
     @Override
     public void launch(final ISelection selection, final String mode) {
         final MakeGoodProperty property = new MakeGoodProperty(getResource(selection));
         if (!property.exists()) {
             showPropertyPage(property, selection, mode);
             return;
         }
 
         addLaunchListener();
 
         ISelection element = selection;
         selectedFolder = null;
         selectedType = null;
         selectedMethod = null;
         if (selection instanceof IStructuredSelection) {
             IStructuredSelection structuredSelection = (IStructuredSelection) selection;
             if (structuredSelection.getFirstElement() instanceof IProjectFragment) {
                 IProjectFragment projectFragment = (IProjectFragment) structuredSelection.getFirstElement();
                 selectedFolder = (IFolder) projectFragment.getResource();
             } else if (structuredSelection.getFirstElement() instanceof IScriptFolder) {
                 IScriptFolder scriptFolder = (IScriptFolder) structuredSelection.getFirstElement();
                 selectedFolder = (IFolder) scriptFolder.getResource();
             } else if (structuredSelection.getFirstElement() instanceof IFolder) {
                 selectedFolder = (IFolder) structuredSelection.getFirstElement();
             } else if (structuredSelection.getFirstElement() instanceof IType) {
                 selectedType = (IType) structuredSelection.getFirstElement();
             } else if (structuredSelection.getFirstElement() instanceof IMethod) {
                 selectedMethod = (IMethod) structuredSelection.getFirstElement();
             }
 
             if (selectedFolder != null) {
                 element = new StructuredSelection(findDummyFile(selectedFolder));
             }
         }
         super.launch(element, mode);
     }
 
     @Override
     public void launch(IEditorPart editor, String mode) {
         final MakeGoodProperty property = new MakeGoodProperty(getResource(editor));
         if (!property.exists()) {
             showPropertyPage(property, editor, mode);
             return;
         }
 
         addLaunchListener();
 
         selectedFolder = null;
         selectedType = null;
         selectedMethod = null;
         ISourceModule source = EditorUtility.getEditorInputModelElement(editor, false);
         if (source != null
             && editor instanceof ITextEditor
             ) {
             ITextEditor textEditor = (ITextEditor) editor;
             ISelectionProvider provider = (ISelectionProvider) textEditor.getSelectionProvider();
             ITextSelection selection = (ITextSelection) provider.getSelection();
             int offset = selection.getOffset();
 
             try {
                 ScriptModelUtil.reconcile(source);
                 IModelElement target = source.getElementAt(offset);
                 if (target != null) {
                     if (target.getElementType() == IModelElement.TYPE) {
                         selectedType = (IType) target;
                     }else if (target.getElementType() == IModelElement.METHOD) {
                         selectedMethod = (IMethod) target;
                     }
                 }
             } catch (ModelException e) {
             }
         }
 
         if (runLevelOnEditor == RUN_TESTS_ON_CLASS) {
             if (selectedMethod != null) {
                 if (selectedMethod.getParent() instanceof IType) {
                     selectedType = (IType) selectedMethod.getParent();
                 }
             }
             selectedMethod = null;
         } else if (runLevelOnEditor == RUN_TESTS_ON_FILE) {
             selectedType = null;
             selectedMethod = null;
         }
 
         super.launch(editor, mode);
     }
 
     private void addLaunchListener() {
         if (launchListener != null) {
             return;
         }
 
         launchListener = new ILaunchListener() {
             @Override
             public void launchAdded(ILaunch launch) {
                 if (selectedFolder != null) {
                     launch.setAttribute("TARGET_FOLDER",
                                         selectedFolder.getFullPath().toString()
                                         );
                 } else {
                     launch.setAttribute("TARGET_FOLDER", null);
                 }
 
                 if (selectedType != null) {
                     launch.setAttribute("CLASS",
                                         selectedType.getElementName()
                                         );
                 } else {
                     launch.setAttribute("CLASS", null);
                 }
 
                 if (selectedMethod != null) {
                     launch.setAttribute("METHOD",
                                         selectedMethod.getParent().getElementName() +
                                             "::" +
                                             selectedMethod.getElementName()
                                         );
                 } else {
                     launch.setAttribute("METHOD", null);
                 }
 
                 try {
                     String target = launch.getLaunchConfiguration().getAttribute("ATTR_FILE", "");
                     MakeGoodProperty property = new MakeGoodProperty(target);
                     launch.setAttribute("PRELOAD_SCRIPT", property.getPreloadScript());
                 } catch (CoreException e) {
                     // TODO Auto-generated catch block
                     e.printStackTrace();
                 }
             }
 
             @Override
             public void launchChanged(ILaunch launch) {
             }
 
             @Override
             public void launchRemoved(ILaunch launch) {
             }
         };
         DebugPlugin.getDefault().getLaunchManager().addLaunchListener(launchListener);
     }
 
     private IFile findDummyFile(IFolder folder) {
         try {
             for (IResource resource: folder.members()) {
                 if (PHPResource.isTrue(resource)) {
                     return (IFile) resource;
                 }
             }
             for (IResource resource: folder.members()) {
                 if (resource instanceof IFolder) {
                     IFile file = findDummyFile((IFolder) resource);
                     if (file != null) {
                         return file;
                     }
                 }
             }
         } catch (CoreException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
         return null;
     }
 
     @Override
     protected ILaunchConfigurationType getPHPExeLaunchConfigType() {
         ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
         return manager.getLaunchConfigurationType("com.piece_framework.makegood.launch.launchConfigurationType");
     }
 
     private void showPropertyPage(final MakeGoodProperty property,
                                   final Object target,
                                   final String mode
                                   ) {
         Display.getDefault().asyncExec(new Runnable() {
             @Override
             public void run() {
                 PropertyDialog dialog = PropertyDialog.createDialogOn(null,
                                                                       "com.piece_framework.makegood.ui.MakeGood",
                                                                       property.getProject()
                                                                       );
                 if (dialog.open() == Window.OK) {
                     int runLevelOnEditor = MakeGoodLaunchShortcut.this.runLevelOnEditor;
                     MakeGoodLaunchShortcut shortcut = new MakeGoodLaunchShortcut();
                     shortcut.setRunLevelOnEditor(runLevelOnEditor);
                     if (target instanceof ISelection) {
                         shortcut.launch((ISelection) target, mode);
                     } else if (target instanceof IEditorPart) {
                         shortcut.launch((IEditorPart) target, mode);
                     }
                 }
             }
         });
     }
 
     private IResource getResource(Object target) {
         if (target instanceof IStructuredSelection) {
             IStructuredSelection selection = (IStructuredSelection) target;
             if (selection.getFirstElement() instanceof IModelElement) {
                 return ((IModelElement) selection.getFirstElement()).getResource();
             }
         } else if (target instanceof IResource) {
             return (IResource) target;
         } else if (target instanceof IEditorPart) {
             ISourceModule source = EditorUtility.getEditorInputModelElement((IEditorPart) target, false);
             return source.getResource();
         }
         return null;
     }
 }
