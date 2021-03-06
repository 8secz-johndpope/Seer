 /*******************************************************************************
  * Copyright (c) 2004, 2008 Tasktop Technologies and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Tasktop Technologies - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.mylyn.tasks.ui.wizards;
 
 import org.eclipse.core.runtime.Assert;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.jface.wizard.IWizardPage;
 import org.eclipse.jface.wizard.Wizard;
 import org.eclipse.mylyn.commons.core.StatusHandler;
 import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
 import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
 import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
 import org.eclipse.mylyn.internal.tasks.ui.wizards.Messages;
 import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
 import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
 import org.eclipse.mylyn.tasks.core.TaskRepository;
 import org.eclipse.mylyn.tasks.ui.TasksUi;
 import org.eclipse.mylyn.tasks.ui.TasksUiImages;
 
 /**
  * Extend to provide a custom edit query dialog, typically invoked by the user requesting properties on a query node in
  * the Task List.
  * 
  * @author Mik Kersten
  * @author Steffen Pingel
  * @since 3.0
  */
 public class RepositoryQueryWizard extends Wizard {
 
 	private final TaskRepository repository;
 
 	/**
 	 * @since 3.0
 	 */
 	public RepositoryQueryWizard(TaskRepository repository) {
 		Assert.isNotNull(repository);
 		this.repository = repository;
 		setNeedsProgressMonitor(true);
 		setWindowTitle(Messages.RepositoryQueryWizard_Edit_Repository_Query);
 		setDefaultPageImageDescriptor(TasksUiImages.BANNER_REPOSITORY);
 	}
 
 	@Override
 	public boolean canFinish() {
 		IWizardPage currentPage = getContainer().getCurrentPage();
 		if (currentPage instanceof AbstractRepositoryQueryPage) {
 			return currentPage.isPageComplete();
 		}
 		return false;
 	}
 
 	@Override
 	public boolean performFinish() {
 		IWizardPage currentPage = getContainer().getCurrentPage();
 		if (!(currentPage instanceof AbstractRepositoryQueryPage)) {
 			StatusHandler.fail(new Status(IStatus.ERROR, TasksUiPlugin.ID_PLUGIN,
 					"Current wizard page does not extends AbstractRepositoryQueryPage")); //$NON-NLS-1$
 			return false;
 		}
 
 		AbstractRepositoryQueryPage page = (AbstractRepositoryQueryPage) currentPage;
 		IRepositoryQuery query = page.getQuery();
 		if (query != null) {
 			page.applyTo(query);
 			if (query instanceof RepositoryQuery) {
 				TasksUiPlugin.getTaskList().notifyElementChanged((RepositoryQuery) query);
 			}
 		} else {
 			query = page.createQuery();
 			TasksUiInternal.getTaskList().addQuery((RepositoryQuery) query);
 		}
 		AbstractRepositoryConnector connector = TasksUi.getRepositoryManager().getRepositoryConnector(
 				getTaskRepository().getConnectorKind());
 		TasksUiInternal.synchronizeQuery(connector, (RepositoryQuery) query, null, true);
 		return true;
 	}
 
 	public TaskRepository getTaskRepository() {
 		return repository;
 	}
 
 }
