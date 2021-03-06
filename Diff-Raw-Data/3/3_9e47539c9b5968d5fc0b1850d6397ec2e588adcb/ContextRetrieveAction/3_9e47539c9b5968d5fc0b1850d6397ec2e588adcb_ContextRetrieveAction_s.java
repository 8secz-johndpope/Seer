 /*******************************************************************************
  * Copyright (c) 2004 - 2006 University Of British Columbia and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     University Of British Columbia - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.mylyn.internal.context.ui.actions;
 
 import org.eclipse.jface.action.Action;
 import org.eclipse.jface.action.IAction;
 import org.eclipse.jface.dialogs.Dialog;
 import org.eclipse.jface.dialogs.MessageDialog;
 import org.eclipse.jface.viewers.ISelection;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.jface.wizard.WizardDialog;
 import org.eclipse.mylyn.internal.tasks.ui.TasksUiImages;
 import org.eclipse.mylyn.internal.tasks.ui.views.TaskListView;
 import org.eclipse.mylyn.internal.tasks.ui.wizards.ContextRetrieveWizard;
 import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
 import org.eclipse.mylyn.tasks.core.AbstractTask;
 import org.eclipse.mylyn.tasks.core.IAttachmentHandler;
 import org.eclipse.mylyn.tasks.core.RepositoryAttachment;
 import org.eclipse.mylyn.tasks.core.TaskRepository;
 import org.eclipse.mylyn.tasks.ui.ContextUiUtil;
 import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
 import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.ui.IEditorPart;
 import org.eclipse.ui.IViewActionDelegate;
 import org.eclipse.ui.IViewPart;
 import org.eclipse.ui.PlatformUI;
 
 /**
  * @author Mik Kersten
  * @author Rob Elves
  * @author Steffen Pingel
  */
 public class ContextRetrieveAction extends Action implements IViewActionDelegate {
 
 	private AbstractTask task;
 
 	private TaskRepository repository;
 
 	private AbstractRepositoryConnector connector;
 
 	private StructuredSelection selection;
 
 	private static final String ID_ACTION = "org.eclipse.mylyn.context.ui.repository.task.retrieve";
 	
 	public ContextRetrieveAction() {
 		setText("Retrieve...");
 		setToolTipText("Retrieve Task Context");
 		setId(ID_ACTION);
 		setImageDescriptor(TasksUiImages.CONTEXT_RETRIEVE);
 	}
 	
 	public void init(IViewPart view) {
 		// ignore
 	}
 
 	@Override
 	public void run() {
 		run(this);
 	}
 	
 	public void run(IAction action) {
 		if (task != null) {
 			run(task);
 		} else {
 			// TODO: consider refactoring to be based on object contributions
 			if (selection.getFirstElement() instanceof RepositoryAttachment) {
 				RepositoryAttachment attachment = (RepositoryAttachment) selection.getFirstElement();
 
 				// HACK: need better way of getting task
 				IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
 						.getActiveEditor();
 				AbstractTask currentTask = null;
 				if (activeEditor instanceof TaskEditor) {
 					currentTask = ((TaskEditor) activeEditor).getTaskEditorInput().getTask();
 				}
 
 				if (currentTask instanceof AbstractTask) {
 					ContextUiUtil.downloadContext((AbstractTask) currentTask, attachment, PlatformUI
 							.getWorkbench().getProgressService());
 				} else {
 					MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
 							"Retrieve Context", "Can not retrieve contenxt for local tasks.");
 				}
 			}
 		}
 	}
 
 	public void run(AbstractTask task) {
 		ContextRetrieveWizard wizard = new ContextRetrieveWizard(task);
 		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
 		if (wizard != null && shell != null && !shell.isDisposed()) {
 			WizardDialog dialog = new WizardDialog(shell, wizard);
 			dialog.create();
 			dialog.setTitle(ContextRetrieveWizard.WIZARD_TITLE);
 			dialog.setBlockOnOpen(true);
 			if (dialog.open() == Dialog.CANCEL) {
 				dialog.close();
 				return;
 			}
 		}
 	}
 
 	public void selectionChanged(IAction action, ISelection selection) {
 		AbstractTask selectedTask = TaskListView.getSelectedTask(selection);
 
 		if (selectedTask == null) {
 			StructuredSelection structuredSelection = (StructuredSelection) selection;
 			this.selection = structuredSelection;
 			if (structuredSelection.getFirstElement() instanceof RepositoryAttachment) {
 				RepositoryAttachment attachment = (RepositoryAttachment) structuredSelection.getFirstElement();
				if (AbstractRepositoryConnector.MYLAR_CONTEXT_DESCRIPTION.equals(attachment.getDescription())) {
 					action.setEnabled(true);
 				} else {
 					action.setEnabled(false);
 				}
 			}
 		} else if (selectedTask instanceof AbstractTask) {
 			task = (AbstractTask) selectedTask;
 			repository = TasksUiPlugin.getRepositoryManager().getRepository(task.getRepositoryKind(),
 					task.getRepositoryUrl());
 			connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(task.getRepositoryKind());
 			IAttachmentHandler handler = connector.getAttachmentHandler();
 			action.setEnabled(handler != null && handler.canDownloadAttachment(repository, task)
 					&& connector.hasRepositoryContext(repository, task));
 		} else {
 			task = null;
 			action.setEnabled(false);
 		}
 	}
 }
