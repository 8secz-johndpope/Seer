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
 
 package org.eclipse.mylar.internal.tasks.ui.editors;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.jface.action.IMenuListener;
 import org.eclipse.jface.action.IMenuManager;
 import org.eclipse.jface.action.MenuManager;
 import org.eclipse.jface.viewers.ISelection;
 import org.eclipse.mylar.context.core.MylarStatusHandler;
 import org.eclipse.mylar.internal.tasks.ui.ITaskEditorFactory;
 import org.eclipse.mylar.internal.tasks.ui.TaskListImages;
 import org.eclipse.mylar.internal.tasks.ui.TaskListPreferenceConstants;
 import org.eclipse.mylar.internal.tasks.ui.views.TaskListView;
 import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
 import org.eclipse.mylar.tasks.core.ITask;
 import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.SWTError;
 import org.eclipse.swt.browser.Browser;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Menu;
 import org.eclipse.ui.IEditorInput;
 import org.eclipse.ui.IEditorPart;
 import org.eclipse.ui.IEditorSite;
 import org.eclipse.ui.IPartListener;
 import org.eclipse.ui.IWorkbench;
 import org.eclipse.ui.IWorkbenchPage;
 import org.eclipse.ui.IWorkbenchPart;
 import org.eclipse.ui.IWorkbenchWindow;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.forms.editor.FormEditor;
 import org.eclipse.ui.forms.editor.IFormPage;
 import org.eclipse.ui.forms.widgets.FormToolkit;
 
 /**
  * @author Mik Kersten
  * @author Eric Booth (initial prototype)
  */
 public class MylarTaskEditor extends FormEditor {
 
 	// private static final String TASK_INFO_PAGE_LABEL = "Planning";
 
 	private static final String ISSUE_WEB_PAGE_LABEL = "Browser";
 
 	protected ITask task;
 
 	private TaskPlanningEditor taskPlanningEditor;
 
 	private Browser webBrowser;
 
 	private TaskEditorInput taskEditorInput;
 
 	private TaskEditorListener partListener;
 
 	private List<IEditorPart> editors = new ArrayList<IEditorPart>();
 
 	private Menu contextMenu;
 
 	private IEditorPart contentOutlineProvider = null;
 
 	// private TaskEditorSelectionProvider selectionProvider;
 	//
 	// private static class TaskEditorSelectionProvider extends
 	// MultiPageSelectionProvider {
 	// private ISelection globalSelection;
 	//
 	// public TaskEditorSelectionProvider(MylarTaskEditor taskEditor) {
 	// super(taskEditor);
 	// }
 	//
 	// public ISelection getSelection() {
 	// IEditorPart activeEditor = ((MylarTaskEditor)
 	// getMultiPageEditor()).getActiveEditor();
 	// if (activeEditor != null && activeEditor.getSite() != null) {
 	// ISelectionProvider selectionProvider =
 	// activeEditor.getSite().getSelectionProvider();
 	// if (selectionProvider != null)
 	// return selectionProvider.getSelection();
 	// }
 	//
 	// return globalSelection;
 	// }
 	//
 	// public void setSelection(ISelection selection) {
 	// IEditorPart activeEditor = ((MylarTaskEditor)
 	// getMultiPageEditor()).getActiveEditor();
 	// if (activeEditor != null && activeEditor.getSite() != null) {
 	// ISelectionProvider selectionProvider =
 	// activeEditor.getSite().getSelectionProvider();
 	// if (selectionProvider != null)
 	// selectionProvider.setSelection(selection);
 	// } else {
 	// this.globalSelection = selection;
 	// fireSelectionChanged(new SelectionChangedEvent(this, globalSelection));
 	// }
 	// }
 	// }
 
 	public MylarTaskEditor() {
 		super();
 		taskPlanningEditor = new TaskPlanningEditor(this);
 		taskPlanningEditor.setParentEditor(this);
 	}
 
 	// @Override
 	// protected void createPages() {
 	// try {
 	// MenuManager manager = new MenuManager();
 	// IMenuListener listener = new IMenuListener() {
 	// public void menuAboutToShow(IMenuManager manager) {
 	// contextMenuAboutToShow(manager);
 	// }
 	// };
 	// manager.setRemoveAllWhenShown(true);
 	// manager.addMenuListener(listener);
 	// contextMenu = manager.createContextMenu(getContainer());
 	// getContainer().setMenu(contextMenu);
 	//
 	// int index = 0;
 	// index = createTaskSummaryPage();
 	// int selectedIndex = index;
 	// for (ITaskEditorFactory factory :
 	// TasksUiPlugin.getDefault().getTaskEditorFactories()) {
 	// if (factory.canCreateEditorFor(task)) {
 	// try {
 	// IEditorPart editor = factory.createEditor(this);
 	// IEditorInput input = factory.createEditorInput(task);
 	// if (editor != null && input != null) {
 	// editors.add(editor);
 	// if (editor instanceof AbstractRepositoryTaskEditor) {
 	// AbstractRepositoryTaskEditor repositoryTaskEditor =
 	// (AbstractRepositoryTaskEditor)editor;
 	// repositoryTaskEditor.setParentEditor(this);
 	// editor.init(getEditorSite(), input);
 	// repositoryTaskEditor.createPartControl(getContainer());
 	// index = addPage(repositoryTaskEditor.getControl());
 	// } else {
 	// index = addPage(editor, input);
 	// }
 	// selectedIndex = index;
 	// setPageText(index++, factory.getTitle());
 	// }
 	// // HACK: overwrites if multiple present
 	// if (factory.providesOutline()) {
 	// contentOutlineProvider = editor;
 	// }
 	// } catch (Exception e) {
 	// MylarStatusHandler.fail(e, "Could not create editor via factory: " +
 	// factory, true);
 	// }
 	// }
 	// }
 	// if (hasValidUrl()) {
 	// int browserIndex = createBrowserPage();
 	// if (selectedIndex == 0 && !taskEditorInput.isNewTask()) {
 	// selectedIndex = browserIndex;
 	// }
 	// }
 	// setActivePage(selectedIndex);
 	//
 	// if (task instanceof AbstractRepositoryTask) {
 	// setTitleImage(TaskListImages.getImage(TaskListImages.TASK_REPOSITORY));
 	// } else if (hasValidUrl()) {
 	// setTitleImage(TaskListImages.getImage(TaskListImages.TASK_WEB));
 	// }
 	// } catch (PartInitException e) {
 	// MylarStatusHandler.fail(e, "failed to create task editor pages", false);
 	// }
 	// }
 
 	protected void contextMenuAboutToShow(IMenuManager manager) {
 		TaskEditorActionContributor contributor = getContributor();
 		// IFormPage page = getActivePageInstance();
 		if (contributor != null)
 			contributor.contextMenuAboutToShow(manager);
 	}
 
 	public TaskEditorActionContributor getContributor() {
 		return (TaskEditorActionContributor) getEditorSite().getActionBarContributor();
 	}
 
 	@Override
 	public Object getAdapter(Class adapter) {
 		// TODO: consider adding: IContentOutlinePage.class.equals(adapter) &&
 		if (contentOutlineProvider != null) {
 			return contentOutlineProvider.getAdapter(adapter);
 		} else {
 			return super.getAdapter(adapter);
 		}
 	}
 
 	public IEditorPart getActiveEditor() {
 		return super.getActiveEditor();
 	}
 
 	private int createBrowserPage() {
 		if (!TasksUiPlugin.getDefault().getPreferenceStore().getBoolean(
 				TaskListPreferenceConstants.REPORT_DISABLE_INTERNAL)) {
 			try {
 				webBrowser = new Browser(getContainer(), SWT.NONE);
 				int index = addPage(webBrowser);
 				setPageText(index, ISSUE_WEB_PAGE_LABEL);
 				webBrowser.setUrl(task.getUrl());
 
 				boolean openWithBrowser = TasksUiPlugin.getDefault().getPreferenceStore().getBoolean(
 						TaskListPreferenceConstants.REPORT_OPEN_INTERNAL);
 				// if (!(task instanceof AbstractRepositoryTask) ||
 				// openWithBrowser) {
 				if (openWithBrowser) {
 					setActivePage(index);
 				}
 				return index;
 			} catch (SWTError e) {
 				MylarStatusHandler.fail(e, "Could not create Browser page: " + e.getMessage(), true);
 			} catch (RuntimeException e) {
 				MylarStatusHandler.fail(e, "could not create issue report page", false);
 			}
 		}
 		return 0;
 	}
 
 	@Override
 	public void doSave(IProgressMonitor monitor) {
 		// commitFormPages(true);
 		// editorDirtyStateChanged();
 
 		for (IFormPage page : getPages()) {
 			if (page.isDirty()) {
 				page.doSave(monitor);
 			}
 		}
 
 		editorDirtyStateChanged();
 
 		// for (IEditorPart editor : editors) {
 		// if (editor.isDirty())
 		// editor.doSave(monitor);
 		// }
 		//
 		// if (webBrowser != null) {
 		// webBrowser.setUrl(task.getUrl());
 		// } else if (hasValidUrl()) {
 		// createBrowserPage();
 		// }
 	}
 
 	// // see PDEFormEditor
 	// private void commitFormPages(boolean onSave) {
 	// IFormPage[] pages = getPages();
 	// for (int i = 0; i < pages.length; i++) {
 	// IFormPage page = pages[i];
 	// IManagedForm mform = page.getManagedForm();
 	// if (mform != null && mform.isDirty()) {
 	// mform.commit(true);
 	// }
 	// }
 	// }
 
 	// see PDEFormEditor
 	/* package */@SuppressWarnings("unchecked")
 	IFormPage[] getPages() {
 		ArrayList formPages = new ArrayList();
 		for (int i = 0; i < pages.size(); i++) {
 			Object page = pages.get(i);
 			if (page instanceof IFormPage)
 				formPages.add(page);
 		}
 		return (IFormPage[]) formPages.toArray(new IFormPage[formPages.size()]);
 	}
 
 	/**
 	 * HACK: perform real check
 	 */
 	private boolean hasValidUrl() {
 		return task != null && task.getUrl().length() > 9;
 	}
 
 	/**
 	 * Saves the multi-page editor's document as another file. Also updates the
 	 * text for page 0's tab, and updates this multi-page editor's input to
 	 * correspond to the nested editor's.
 	 * 
 	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
 	 */
 	@Override
 	public void doSaveAs() {
 		IEditorPart editor = getEditor(0);
 		if (editor != null) {
 			editor.doSaveAs();
 			setPageText(0, editor.getTitle());
 			setInput(editor.getEditorInput());
 		}
 	}
 
 	@Override
 	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
 
 		partListener = new TaskEditorListener();
 		site.getPage().addPartListener(partListener);
 		super.init(site, input);
 
 		// taskEditorInput = (TaskEditorInput) input;
 		setSite(site);
 		// // selectionProvider = new TaskEditorSelectionProvider(this);
 		// // site.setSelectionProvider(selectionProvider);
 		//
 		// /*
 		// * The task data is saved only once, at the initialization of the
 		// * editor. This is then passed to each of the child editors. This way,
 		// * only one instance of the task data is stored for each editor
 		// opened.
 		// */
 		// task = taskEditorInput.getTask();
 		//
 		// try {
 		// // taskPlanningEditor.init(this.getEditorSite(),
 		// // this.getEditorInput());
 		// // taskPlanningEditor.setTask(task);
 		// // Set the title on the editor's tab
 		// this.setPartName(taskEditorInput.getLabel());
 		// } catch (Exception e) {
 		// throw new PartInitException(e.getMessage());
 		// }
 	}
 
 	public void notifyTaskChanged() {
 		TasksUiPlugin.getTaskListManager().getTaskList().notifyLocalInfoChanged(task);
 	}
 
 	@Override
 	public boolean isSaveAsAllowed() {
 		return false;
 	}
 
 	// public boolean isDirty() {
 	// fLastDirtyState = computeDirtyState();
 	// return fLastDirtyState;
 	// }
 
 	// private boolean computeDirtyState() {
 	// IFormPage page = getActivePageInstance();
 	// if (page != null && page.isDirty())
 	// return true;
 	// return super.isDirty();
 	// }
 
 	@Override
 	public boolean isDirty() {
 		for (IFormPage page : getPages()) {
 			if (page.isDirty()) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	private class TaskEditorListener implements IPartListener {
 
 		public void partActivated(IWorkbenchPart part) {
 			if (part.equals(MylarTaskEditor.this)) {
 				if (taskEditorInput != null) {
 					ITask task = taskEditorInput.getTask();
 					if (TaskListView.getFromActivePerspective() != null) {
 						TaskListView.getFromActivePerspective().selectedAndFocusTask(task);
 					}
 				}
 			}
 		}
 
 		/**
 		 * @see org.eclipse.ui.IPartListener#partBroughtToTop(org.eclipse.ui.IWorkbenchPart)
 		 */
 		public void partBroughtToTop(IWorkbenchPart part) {
 			// don't care about this event
 		}
 
 		/**
 		 * @see org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
 		 */
 		public void partClosed(IWorkbenchPart part) {
 			// don't care about this event
 		}
 
 		/**
 		 * @see org.eclipse.ui.IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart)
 		 */
 		public void partDeactivated(IWorkbenchPart part) {
 			// don't care about this event
 		}
 
 		/**
 		 * @see org.eclipse.ui.IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
 		 */
 		public void partOpened(IWorkbenchPart part) {
 			// don't care about this event
 		}
 	}
 
 	/**
 	 * Updates the tab titile
 	 */
 	public void changeTitle() {
 		this.setPartName(taskEditorInput.getLabel());
 	}
 
 	public void markDirty() {
 		firePropertyChange(PROP_DIRTY);
 		return;
 	}
 
 	@Override
 	public void setFocus() {
 		// taskInfoEditor.setFocus();
 	}
 
 	public Browser getWebBrowser() {
 		return webBrowser;
 	}
 
 	@Override
 	protected void pageChange(int newPageIndex) {
 		for (ITaskEditorFactory factory : TasksUiPlugin.getDefault().getTaskEditorFactories()) {
 			for (IEditorPart editor : editors) {
 				factory.notifyEditorActivationChange(editor);
 			}
 		}
 		super.pageChange(newPageIndex);
 	}
 
 	public void dispose() {
 		for (IEditorPart part : editors) {
 			part.dispose();
 		}
 		if (taskPlanningEditor != null)
 			taskPlanningEditor.dispose();
 		if (webBrowser != null) {
 			webBrowser.dispose();
 		}
 
 		IWorkbench workbench = TasksUiPlugin.getDefault().getWorkbench();
 		if (workbench != null && partListener != null) {
 			for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
 				IWorkbenchPage activePage = window.getActivePage();
 				if (activePage != null) {
 					activePage.removePartListener(partListener);
 				}
 			}
 		}
 		super.dispose();
 	}
 
 	public TaskEditorInput getTaskEditorInput() {
 		return taskEditorInput;
 	}
 
 	@Override
 	protected void addPages() {
 		try {
 			MenuManager manager = new MenuManager();
 			IMenuListener listener = new IMenuListener() {
 				public void menuAboutToShow(IMenuManager manager) {
 					contextMenuAboutToShow(manager);
 				}
 			};
 			manager.setRemoveAllWhenShown(true);
 			manager.addMenuListener(listener);
 			contextMenu = manager.createContextMenu(getContainer());
 			getContainer().setMenu(contextMenu);
 			int index = -1;
 			if (getEditorInput() instanceof TaskEditorInput) {
 				addPage(taskPlanningEditor);
 				index++;
 				taskEditorInput = (TaskEditorInput) getEditorInput();
 				task = taskEditorInput.getTask();
 				setPartName(taskEditorInput.getLabel());					
 			}
//			else {
//				this.setTitleImage(TaskListImages.getImage(TaskListImages.O));
//			}
 			
 			int selectedIndex = index;
 			for (ITaskEditorFactory factory : TasksUiPlugin.getDefault().getTaskEditorFactories()) {
 				if ((task != null && factory.canCreateEditorFor(task)) || factory.canCreateEditorFor(getEditorInput())) {
 					try {
 						IEditorPart editor = factory.createEditor(this);
 						IEditorInput input = task != null ? factory.createEditorInput(task) : getEditorInput();
 						if (editor != null && input != null) {
 							if (editor instanceof AbstractRepositoryTaskEditor) {
 								TaskFormPage repositoryTaskEditor = (TaskFormPage) editor;
 								// repositoryTaskEditor.setParentEditor(this);								
 								editor.init(getEditorSite(), input);
 								repositoryTaskEditor.createPartControl(getContainer());
 								index = addPage(repositoryTaskEditor);
 								if(getEditorInput() instanceof ExistingBugEditorInput) {
 									setPartName(((ExistingBugEditorInput)getEditorInput()).getToolTipText());
 								}
 							} else {
 								index = addPage(editor, input);
 							}
 							selectedIndex = index;
 							setPageText(index++, factory.getTitle());
 						}
 
 						// HACK: overwrites if multiple present
 						if (factory.providesOutline()) {
 							contentOutlineProvider = editor;
 						}
 					} catch (Exception e) {
 						MylarStatusHandler.fail(e, "Could not create editor via factory: " + factory, true);
 					}
 				}
 			}
 			if (hasValidUrl()) {
 				int browserIndex = createBrowserPage();
 				if (selectedIndex == 0 && !taskEditorInput.isNewTask()) {
 					selectedIndex = browserIndex;
 				}
 			}
 			setActivePage(selectedIndex);
 
 			if (task instanceof AbstractRepositoryTask) {
 				setTitleImage(TaskListImages.getImage(TaskListImages.TASK_REPOSITORY));
 			} else if (hasValidUrl()) {
 				setTitleImage(TaskListImages.getImage(TaskListImages.TASK_WEB));
 			}
 		} catch (PartInitException e) {
 			MylarStatusHandler.fail(e, "failed to create task editor pages", false);
 		}
 	}
 
 	protected FormToolkit createToolkit(Display display) {
 		// Create a toolkit that shares colors between editors.
 		return new FormToolkit(PlatformUI.getWorkbench().getDisplay());
 	}
 
 	public ISelection getSelection() {
 		return getSite().getSelectionProvider().getSelection();
 	}
 
 }
