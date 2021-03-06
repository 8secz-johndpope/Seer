 /*******************************************************************************
  * Copyright (c) 2008, 2009 Obeo.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     Obeo - initial API and implementation
  *******************************************************************************/
 package org.eclipse.acceleo.ide.ui.popupMenus;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.List;
 
 import org.eclipse.acceleo.common.AcceleoCommonPlugin;
 import org.eclipse.acceleo.internal.ide.ui.wizards.newfile.CreateTemplateData;
 import org.eclipse.acceleo.internal.ide.ui.wizards.newproject.AcceleoNewProjectWizard;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.jface.action.IAction;
 import org.eclipse.jface.viewers.ISelection;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.jface.wizard.WizardDialog;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.ui.IWorkbenchWindow;
 import org.eclipse.ui.IWorkbenchWindowActionDelegate;
 import org.eclipse.ui.PlatformUI;
 
 /**
  * An abstract action to create automatically a new Acceleo module from an old code generation module (MT,
  * Xpand).
  * 
  * @author <a href="mailto:yvan.lussaud@obeo.fr">Yvan Lussaud</a>
  */
 public abstract class AbstractMigrateProjectWizardAction implements IWorkbenchWindowActionDelegate {
 
 	/**
 	 * The wizard dialog width.
 	 */
 	private static final int SIZING_WIZARD_WIDTH = 500;
 
 	/**
 	 * The wizard dialog height.
 	 */
 	private static final int SIZING_WIZARD_HEIGHT = 500;
 
 	/**
 	 * The current selection.
 	 */
 	private ISelection currentSelection;
 
 	/**
 	 * {@inheritDoc}
 	 * 
 	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
 	 */
 	public void run(IAction action) {
 		if (currentSelection instanceof IStructuredSelection
 				&& PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
 			AcceleoNewProjectWizard wizard = new AcceleoNewProjectWizard() {
 				@Override
 				protected boolean multipleTemplates() {
 					return false;
 				}
 			};
 			wizard.init(PlatformUI.getWorkbench(), (IStructuredSelection)currentSelection);
 			Shell parent = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
 			WizardDialog dialog = new WizardDialog(parent, wizard);
 			dialog.create();
 			Point defaultSize = dialog.getShell().getSize();
 			dialog.getShell().setSize(Math.max(SIZING_WIZARD_WIDTH, defaultSize.x),
 					Math.max(SIZING_WIZARD_HEIGHT, defaultSize.y));
 			List<IProject> projects = new ArrayList<IProject>();
 			Object[] objects = ((IStructuredSelection)currentSelection).toArray();
 			for (int i = 0; i < objects.length; i++) {
 				if (objects[i] instanceof IProject) {
 					projects.add((IProject)objects[i]);
 				}
 			}
 			try {
 				if (wizard.getTemplatePage().getControllers().size() > 0) {
 					CreateTemplateData data = wizard.getTemplatePage().getControllers().get(0).getModel();
					data.setTemplateShortName("chain");
 					data.setTemplateHasFileBlock(false);
 					data.setTemplateIsInitialized(false);
 					browseTemplates(projects.toArray(new IProject[projects.size()]));
 					String metamodelURIs = computeMetamodelURIs();
 					data.setTemplateMetamodel(metamodelURIs);
 					wizard.getTemplatePage().getControllers().get(0).initView();
					if (dialog.open() == WizardDialog.OK
							&& wizard.getTemplatePage().getControllers().size() > 0) {
 						data = wizard.getTemplatePage().getControllers().get(0).getModel();
 						IPath baseFolder = new Path(data.getTemplateContainer());
 						generateMTL(baseFolder);
 					}
 				}
 			} catch (CoreException e) {
 				AcceleoCommonPlugin.log(e.getStatus());
 			} catch (IOException e) {
 				Status status = new Status(IStatus.ERROR, AcceleoCommonPlugin.PLUGIN_ID, e.getMessage()
						.toString());
 				AcceleoCommonPlugin.log(status);
 			}
 		}
 	}
 
 	/**
 	 * {@inheritDoc}
 	 * 
 	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
 	 *      org.eclipse.jface.viewers.ISelection)
 	 */
 	public void selectionChanged(IAction action, ISelection selection) {
 		currentSelection = selection;
 	}
 
 	/**
 	 * {@inheritDoc}
 	 * 
 	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
 	 */
 	public void dispose() {
 	}
 
 	/**
 	 * {@inheritDoc}
 	 * 
 	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
 	 */
 	public void init(IWorkbenchWindow window) {
 	}
 
 	/**
 	 * Browse the old template files of the selected projects.
 	 * 
 	 * @param projects
 	 *            are the projects that contain the template to migrate
 	 * @throws CoreException
 	 *             when a workspace issue occurs
 	 */
 	protected abstract void browseTemplates(IProject[] projects) throws CoreException;
 
 	/**
 	 * Generate the output MTL files.
 	 * 
 	 * @param baseFolder
 	 *            is the target folder
 	 * @throws IOException
 	 *             when the model cannot be saved
 	 * @throws CoreException
 	 *             when a workspace issue occurs
 	 */
 	protected abstract void generateMTL(IPath baseFolder) throws IOException, CoreException;
 
 	/**
 	 * Computes the metamodel URIs.
 	 * 
 	 * @return the URIs separated by the comma character
 	 */
 	protected abstract String computeMetamodelURIs();
 
 }
