 /*******************************************************************************
  * Copyright (c) 2004, 2010 Tasktop Technologies and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Tasktop Technologies - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.mylyn.internal.tasks.ui;
 
 import java.util.Set;
 
 import org.eclipse.jface.action.IMenuListener;
 import org.eclipse.jface.action.IMenuManager;
 import org.eclipse.jface.action.MenuManager;
 import org.eclipse.jface.preference.IPreferenceStore;
 import org.eclipse.jface.util.IPropertyChangeListener;
 import org.eclipse.jface.util.PropertyChangeEvent;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.mylyn.commons.ui.SelectionProviderAdapter;
 import org.eclipse.mylyn.internal.tasks.core.ITaskListChangeListener;
 import org.eclipse.mylyn.internal.tasks.core.TaskContainerDelta;
 import org.eclipse.mylyn.internal.tasks.ui.actions.RepositoryElementActionGroup;
 import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
 import org.eclipse.mylyn.internal.tasks.ui.views.TaskListView;
 import org.eclipse.mylyn.tasks.core.ITask;
 import org.eclipse.mylyn.tasks.core.ITaskActivationListener;
 import org.eclipse.mylyn.tasks.core.TaskActivationAdapter;
 import org.eclipse.mylyn.tasks.ui.TasksUi;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.MenuDetectEvent;
 import org.eclipse.swt.events.MenuDetectListener;
 import org.eclipse.swt.events.MouseAdapter;
 import org.eclipse.swt.events.MouseEvent;
 import org.eclipse.swt.graphics.GC;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Menu;
 import org.eclipse.swt.widgets.ToolBar;
 import org.eclipse.ui.IWorkbenchWindow;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.forms.events.HyperlinkAdapter;
 import org.eclipse.ui.forms.events.HyperlinkEvent;
 import org.eclipse.ui.internal.ObjectActionContributorManager;
 import org.eclipse.ui.internal.WorkbenchWindow;
 import org.eclipse.ui.internal.layout.IWindowTrim;
 import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
 
 /**
  * @author Mik Kersten
  * @author Leo Dos Santos
  */
 public class TaskTrimWidget extends WorkbenchWindowControlContribution {
 
 	public static String ID_CONTAINER = "org.eclipse.mylyn.tasks.ui.trim.container"; //$NON-NLS-1$
 
 	public static String ID_CONTROL = "org.eclipse.mylyn.tasks.ui.trim.control"; //$NON-NLS-1$
 
 	private Composite composite;
 
 	private ITask activeTask;
 
 	private MenuManager menuManager;
 
 	private Menu menu;
 
 	private TaskScalingHyperlink activeTaskLabel;
 
 	private final ITaskActivationListener taskActivationListener = new TaskActivationAdapter() {
 
 		@Override
 		public void taskActivated(ITask task) {
 			activeTask = task;
 			indicateActiveTask();
 		}
 
 		@Override
 		public void taskDeactivated(ITask task) {
 			activeTask = null;
 			indicateNoActiveTask();
 		}
 
 	};
 
 	private final ITaskListChangeListener taskListListener = new ITaskListChangeListener() {
 		public void containersChanged(Set<TaskContainerDelta> containers) {
 			// update label in case task changes
 			if (activeTask != null) {
 				for (TaskContainerDelta taskContainerDelta : containers) {
 					if (activeTask.equals(taskContainerDelta.getElement())) {
 						if (taskContainerDelta.getKind().equals(TaskContainerDelta.Kind.CONTENT)) {
 							Display.getDefault().asyncExec(new Runnable() {
 								public void run() {
 									if (activeTask != null && activeTask.isActive()) {
 										indicateActiveTask();
 									}
 								}
 							});
 							return;
 						}
 					}
 				}
 			}
 		}
 	};
 
 	private final IPropertyChangeListener preferencesListener = new IPropertyChangeListener() {
 		public void propertyChange(PropertyChangeEvent event) {
 			String property = event.getProperty();
 			if (property.equals(ITasksUiPreferenceConstants.SHOW_TRIM)) {
 				Object newValue = event.getNewValue();
 				Boolean isVisible = false;
 				if (newValue instanceof Boolean) {
 					isVisible = (Boolean) newValue;
 				} else if (newValue instanceof String) {
 					isVisible = Boolean.parseBoolean((String) newValue);
 				}
 				setTrimVisible(isVisible);
 			}
 		}
 	};
 
 	private SelectionProviderAdapter activeTaskSelectionProvider;
 
 	private RepositoryElementActionGroup actionGroup;
 
 	public TaskTrimWidget() {
 		TasksUi.getTaskActivityManager().addActivationListener(taskActivationListener);
 		TasksUiPlugin.getTaskList().addChangeListener(taskListListener);
 		TasksUiPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(preferencesListener);
 		hookContextMenu();
 	}
 
 	private void setTrimVisible(boolean visible) {
 		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
 		if (window instanceof WorkbenchWindow) {
 			IWindowTrim trim = ((WorkbenchWindow) window).getTrimManager().getTrim(ID_CONTAINER);
 			if (trim != null) {
 				((WorkbenchWindow) window).getTrimManager().setTrimVisible(trim, visible);
 				((WorkbenchWindow) window).getTrimManager().forceLayout();
 			}
 		}
 	}
 
 	@Override
 	public void dispose() {
 		if (composite != null && !composite.isDisposed()) {
 			composite.dispose();
 		}
 		composite = null;
 
 		if (menuManager != null) {
 			menuManager.removeAll();
 			menuManager.dispose();
 		}
 		menuManager = null;
 
 		if (menu != null && !menu.isDisposed()) {
 			menu.dispose();
 		}
 		menu = null;
 
 		actionGroup.setSelectionProvider(null);
 
 		TasksUi.getTaskActivityManager().removeActivationListener(taskActivationListener);
 		TasksUiPlugin.getTaskList().removeChangeListener(taskListListener);
 		TasksUiPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(preferencesListener);
 	}
 
 	@Override
 	protected Control createControl(Composite parent) {
 		composite = new Composite(parent, SWT.NONE);
 
 		GridLayout layout = new GridLayout();
 		layout.numColumns = 1;
 		layout.horizontalSpacing = 0;
 		layout.marginHeight = 0;
 		layout.marginLeft = 0;
 		layout.marginRight = 0;
 		composite.setLayout(layout);
 
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
 
 		createStatusComposite(composite);
 
 		if (!shouldShowTrim()) {
 			if (parent instanceof ToolBar) {
 				// bug 201589: it's not possible to hide the contribution on startup, as a work-around the tool bar is hidden which avoids flickering of the layout
 				parent.setVisible(false);
 			}
 			// needs to be invoked asynchronously since the trim contribution is just getting constructed when createControl() is invoked
 			parent.getDisplay().asyncExec(new Runnable() {
 				public void run() {
 					setTrimVisible(shouldShowTrim());
 				}
 			});
 		}
 
 		return composite;
 	}
 
 	private boolean shouldShowTrim() {
 		IPreferenceStore uiPreferenceStore = TasksUiPlugin.getDefault().getPreferenceStore();
 		return uiPreferenceStore.getBoolean(ITasksUiPreferenceConstants.SHOW_TRIM);
 	}
 
 	private Composite createStatusComposite(final Composite container) {
 		GC gc = new GC(container);
 		Point p = gc.textExtent("WWWWWWWWWWWWWWW"); //$NON-NLS-1$
 		gc.dispose();
 
 		activeTaskLabel = new TaskScalingHyperlink(container, SWT.RIGHT);
 		// activeTaskLabel.setLayoutData(new GridData(p.x, SWT.DEFAULT));
 		GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, true);
 		gridData.widthHint = p.x;
 		gridData.minimumWidth = p.x;
 		gridData.horizontalIndent = 0;
 		activeTaskLabel.setLayoutData(gridData);
 		activeTaskLabel.setText(Messages.TaskTrimWidget__no_task_active_);
 
 		activeTask = TasksUi.getTaskActivityManager().getActiveTask();
 		if (activeTask != null) {
 			indicateActiveTask();
 		}
 
 		activeTaskLabel.addMenuDetectListener(new MenuDetectListener() {
 			public void menuDetected(MenuDetectEvent e) {
 				if (menu != null) {
 					menu.dispose();
 				}
 				menu = menuManager.createContextMenu(container);
 				menu.setVisible(true);
 			}
 		});
 
 		activeTaskLabel.addHyperlinkListener(new HyperlinkAdapter() {
 			@Override
 			public void linkActivated(HyperlinkEvent e) {
 				TaskListView taskListView = TaskListView.getFromActivePerspective();
 				if (taskListView != null && taskListView.getDrilledIntoCategory() != null) {
 					taskListView.goUpToRoot();
 				}
 				TasksUiInternal.refreshAndOpenTaskListElement((TasksUi.getTaskActivityManager().getActiveTask()));
 			}
 		});
 
 		activeTaskLabel.addMouseListener(new MouseAdapter() {
 			@Override
 			public void mouseDown(MouseEvent e) {
 				// only handle left clicks, context menu is handled by platform
 				if (e.button == 1) {
 					if (activeTask == null) {
 						return;
 					}
 
 					TaskListView taskListView = TaskListView.getFromActivePerspective();
 					if (taskListView != null && taskListView.getDrilledIntoCategory() != null) {
 						taskListView.goUpToRoot();
 					}
 
 					TasksUiInternal.refreshAndOpenTaskListElement(activeTask);
 				}
 			}
 		});
 
 		return activeTaskLabel;
 	}
 
 	private void hookContextMenu() {
 		activeTaskSelectionProvider = new SelectionProviderAdapter();
 
 		actionGroup = new RepositoryElementActionGroup();
 		actionGroup.setSelectionProvider(activeTaskSelectionProvider);
 
 		menuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
 		menuManager.setRemoveAllWhenShown(true);
 		menuManager.addMenuListener(new IMenuListener() {
 			public void menuAboutToShow(IMenuManager manager) {
 				actionGroup.fillContextMenu(manager);
 				// trims do not have a workbench part so there is no simple way of registering the 
 				// context menu
 				ObjectActionContributorManager.getManager().contributeObjectActions(null, manager,
 						activeTaskSelectionProvider);
 			}
 		});
 	}
 
 	public void indicateActiveTask() {
 		if (activeTaskLabel != null && activeTaskLabel.isDisposed()) {
 			return;
 		}
 
 		//activeTaskLabel.setText(shortenText(activeTask.getSummary()));
 		activeTaskLabel.setText(activeTask.getSummary());
 		activeTaskLabel.setUnderlined(true);
 		activeTaskLabel.setToolTipText(activeTask.getSummary());
 		activeTaskSelectionProvider.setSelection(new StructuredSelection(activeTask));
 	}
 
 	public void indicateNoActiveTask() {
 		if (activeTaskLabel != null && activeTaskLabel.isDisposed()) {
 			return;
 		}
 
 		activeTaskLabel.setText(Messages.TaskTrimWidget__no_active_task_);
 		activeTaskLabel.setUnderlined(false);
 		activeTaskLabel.setToolTipText(""); //$NON-NLS-1$
 		activeTaskSelectionProvider.setSelection(StructuredSelection.EMPTY);
 	}
 
 //	// From PerspectiveBarContributionItem
 //	private String shortenText(String taskLabel) {
 //		if (taskLabel == null || composite == null || composite.isDisposed()) {
 //			return null;
 //		}
 //
 //		String returnText = taskLabel;
 //		GC gc = new GC(composite);
 //		int maxWidth = p.x;
 //
 //		if (gc.textExtent(taskLabel).x > maxWidth) {
 //			for (int i = taskLabel.length(); i > 0; i--) {
 //				String test = taskLabel.substring(0, i);
 //				test = test + "...";
 //				if (gc.textExtent(test).x < maxWidth) {
 //					returnText = test;
 //					break;
 //				}
 //			}
 //		}
 //
 //		gc.dispose();
 //		return returnText;
 //	}
 }
