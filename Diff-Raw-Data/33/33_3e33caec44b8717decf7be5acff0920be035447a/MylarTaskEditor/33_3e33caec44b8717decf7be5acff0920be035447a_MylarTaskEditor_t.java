 /*******************************************************************************
  * Copyright (c) 2004 - 2005 University Of British Columbia and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     University Of British Columbia - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.mylar.internal.tasklist.ui;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.jface.viewers.ISelection;
 import org.eclipse.jface.viewers.ISelectionProvider;
 import org.eclipse.jface.viewers.SelectionChangedEvent;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.jface.viewers.Viewer;
 import org.eclipse.mylar.core.MylarPlugin;
 import org.eclipse.mylar.core.util.MylarStatusHandler;
 import org.eclipse.mylar.internal.tasklist.MylarTaskListPlugin;
 import org.eclipse.mylar.internal.tasklist.MylarTaskListPrefConstants;
 import org.eclipse.mylar.internal.tasklist.ui.views.TaskListView;
 import org.eclipse.mylar.tasklist.IQueryHit;
 import org.eclipse.mylar.tasklist.ITask;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.SWTError;
 import org.eclipse.swt.browser.Browser;
 import org.eclipse.ui.IEditorInput;
 import org.eclipse.ui.IEditorPart;
 import org.eclipse.ui.IEditorSite;
 import org.eclipse.ui.IPartListener;
 import org.eclipse.ui.IWorkbench;
 import org.eclipse.ui.IWorkbenchPage;
 import org.eclipse.ui.IWorkbenchPart;
 import org.eclipse.ui.IWorkbenchWindow;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.part.MultiPageEditorPart;
 import org.eclipse.ui.part.MultiPageSelectionProvider;
 
 /**
  * @author Mik Kersten
  * @author Eric Booth (initial prototype)
  */
 public class MylarTaskEditor extends MultiPageEditorPart {
 
 	private static final String TASK_INFO_PAGE_LABEL = "Task Info";
 
 	private static final String ISSUE_WEB_PAGE_LABEL = "Browser";
 
 	protected ITask task;
 
 	private TaskInfoEditor taskInfoEditor;
 
 	private Browser webBrowser;
 
 	private TaskEditorInput taskEditorInput;
 	
 	private TaskEditorListener partListener;
 
 	private List<IEditorPart> editorsToNotifyOnChange = new ArrayList<IEditorPart>();
 
 	private static class TaskEditorSelectionProvider extends MultiPageSelectionProvider {
 		private ISelection globalSelection;
 
 		public TaskEditorSelectionProvider(MylarTaskEditor taskEditor) {
 			super(taskEditor);
 		}
 
 		public ISelection getSelection() {
 			IEditorPart activeEditor = ((MylarTaskEditor) getMultiPageEditor()).getActiveEditor();
 			if (activeEditor != null && activeEditor.getSite() != null) {
 				ISelectionProvider selectionProvider = activeEditor.getSite().getSelectionProvider();
 				if (selectionProvider != null)
 					return selectionProvider.getSelection();
 			}
 			return globalSelection;
 		}
 
 		public void setSelection(ISelection selection) {
 			IEditorPart activeEditor = ((MylarTaskEditor) getMultiPageEditor()).getActiveEditor();
 			if (activeEditor != null && activeEditor.getSite() != null) {
 				ISelectionProvider selectionProvider = activeEditor.getSite().getSelectionProvider();
 				if (selectionProvider != null)
 					selectionProvider.setSelection(selection);
 			} else {
 				this.globalSelection = selection;
 				fireSelectionChanged(new SelectionChangedEvent(this, globalSelection));
 			}
 		}
 	}
 
 	public MylarTaskEditor() {
 		super();
 		IWorkbench workbench = MylarTaskListPlugin.getDefault().getWorkbench();
 		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
 		IWorkbenchPage activePage = window.getActivePage();
 		partListener = new TaskEditorListener();
 		activePage.addPartListener(partListener);
 		taskInfoEditor = new TaskInfoEditor();
 		
 	}
 
 	@Override
 	protected void createPages() {
 		try {
 			int index = createTaskSummaryPage();
 			if (task.getUrl().length() > 9) {
 				createTaskIssueWebPage();
 			}
 			for (IContextEditorFactory factory : MylarTaskListPlugin.getDefault().getContextEditors()) {
 				taskInfoEditor.setParentEditor(this);
 				IEditorPart editor = factory.createEditor();
 				editorsToNotifyOnChange.add(editor);
 				index = addPage(editor, factory.createEditorInput(MylarPlugin.getContextManager().getActiveContext()));
 				setPageText(index++, factory.getTitle());
 			}
 		} catch (PartInitException e) {
 			MylarStatusHandler.fail(e, "failed to create task editor pages", false);
 		}
 	}
 
 	public IEditorPart getActiveEditor() {
 		return super.getActiveEditor();
 	}
 
 	private int createTaskSummaryPage() throws PartInitException {
 		try {
 			taskInfoEditor.createPartControl(getContainer());
 			taskInfoEditor.setParentEditor(this);
 			int index = addPage(taskInfoEditor.getControl());
 			setPageText(index, TASK_INFO_PAGE_LABEL);
 			return index;
 		} catch (RuntimeException e) {
 			MylarStatusHandler.fail(e, "could not add task editor", false);
 		}
 		return 0;
 	}
 
 	/**
 	 * Creates page 2 of the multi-page editor, which displays the task issue
 	 * web page
 	 */
 	private void createTaskIssueWebPage() {
 		try {
 			webBrowser = new Browser(getContainer(), SWT.NONE);
 			int index = addPage(webBrowser);
 			setPageText(index, ISSUE_WEB_PAGE_LABEL);
 			webBrowser.setUrl(task.getUrl());
 
 			boolean openWithBrowser = MylarTaskListPlugin.getPrefs().getBoolean(MylarTaskListPrefConstants.REPORT_OPEN_INTERNAL);
 			if (task.isLocal() || openWithBrowser)
 				setActivePage(index);
 		} catch (SWTError e) {
 			MylarStatusHandler.fail(e, "Could not create Browser page: " + e.getMessage(), true);
 		} catch (RuntimeException e) {
 			MylarStatusHandler.fail(e, "could not create issue report page", false);
 		}
 	}
 
 	@Override
 	public void doSave(IProgressMonitor monitor) {
 		if (!taskInfoEditor.getControl().isDisposed()) {
 			taskInfoEditor.doSave(monitor);
 		} else {
 			MylarStatusHandler.log("attempted to save disposed editor: " + taskInfoEditor, this);
 		}
 		if (webBrowser != null) {
 			webBrowser.setUrl(task.getUrl());
 		} else if (task.getUrl().length() > 9) {
 			createTaskIssueWebPage();
 		}
 	}
 
 	/**
 	 * Saves the multi-page editor's document as another file. Also updates the
 	 * text for page 0's tab, and updates this multi-page editor's input to
 	 * correspond to the nested editor's.
 	 * 
 	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
 	 */
 	@SuppressWarnings("deprecation")
 	@Override
 	public void doSaveAs() {
 		IEditorPart editor = getEditor(0);
 		editor.doSaveAs();
 		setPageText(0, editor.getTitle());
 		setInput(editor.getEditorInput());
 	}
 
 	@Override
 	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
 		taskEditorInput = (TaskEditorInput) input;
 		super.init(site, input);
 
 		setSite(site);
 		site.setSelectionProvider(new TaskEditorSelectionProvider(this));
 
 		/*
 		 * The task data is saved only once, at the initialization of the
 		 * editor. This is then passed to each of the child editors. This way,
 		 * only one instance of the task data is stored for each editor opened.
 		 */
 		task = taskEditorInput.getTask();
 		try {
 			taskInfoEditor.init(this.getEditorSite(), this.getEditorInput());			
 			taskInfoEditor.setTask(task);
 			// Set the title on the editor's tab
 			this.setPartName(taskEditorInput.getLabel());
 		} catch (Exception e) {
 			throw new PartInitException(e.getMessage());
 		}
 	}
 
 	@Override
 	public boolean isSaveAsAllowed() {
 		return false;
 	}
 
 	@Override
 	public boolean isDirty() {
 		return taskInfoEditor.isDirty();
 	}
 
 	private class TaskEditorListener implements IPartListener {
 
 		/**
 		 * @see org.eclipse.ui.IPartListener#partActivated(org.eclipse.ui.IWorkbenchPart)
 		 */
 		public void partActivated(IWorkbenchPart part) {
 			if (part.equals(MylarTaskEditor.this)) {				
 				ITask task = taskEditorInput.getTask();
 				IQueryHit hit = null;
 				MylarTaskListPlugin.getTaskListManager().getQueryHitForHandle(task.getHandleIdentifier());
 				
				if (TaskListView.getDefault() != null) {
					Viewer viewer = TaskListView.getDefault().getViewer();				
					viewer.setSelection(new StructuredSelection(task));
					// if no task exists, select the query hit if exists
					if (viewer.getSelection().isEmpty()
							&& (hit = MylarTaskListPlugin.getTaskListManager().getQueryHitForHandle(
									task.getHandleIdentifier())) != null) {
						viewer.setSelection(new StructuredSelection(hit));
					} 
					viewer.refresh();
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
 		super.pageChange(newPageIndex);
 		for (IContextEditorFactory factory : MylarTaskListPlugin.getDefault().getContextEditors()) {
 			for (IEditorPart editor : editorsToNotifyOnChange) {
 				factory.notifyEditorActivationChange(editor);
 			}
 		}
 	}
 	
 	public void dispose() {
 		for (IEditorPart part : editorsToNotifyOnChange) {
 			part.dispose();
 		}
 		if (taskInfoEditor != null)
 			taskInfoEditor.dispose();
 		if (webBrowser != null)
 			webBrowser.dispose();
 
 		IWorkbench workbench = MylarTaskListPlugin.getDefault().getWorkbench();
 		IWorkbenchWindow window = null;
 		IWorkbenchPage activePage = null;
 		if (workbench != null) {
 			window = workbench.getActiveWorkbenchWindow();
 		}
 		if (window != null) {
 			activePage = window.getActivePage();
 		}
 		if (activePage != null) {
 			activePage.removePartListener(partListener);
 		}
 
 	}
 	
 }
