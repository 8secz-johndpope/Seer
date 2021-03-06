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
 
 package org.eclipse.mylar.internal.tasks.ui.views;
 
 import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
 import org.eclipse.jface.viewers.Viewer;
 import org.eclipse.jface.viewers.ViewerSorter;
 import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
 import org.eclipse.mylar.tasks.core.AbstractTaskContainer;
 import org.eclipse.mylar.tasks.core.ITask;
 import org.eclipse.mylar.tasks.core.ITaskListElement;
 import org.eclipse.mylar.tasks.core.TaskArchive;
 import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
 import org.eclipse.ui.PlatformUI;
 
 /**
  * @author Mik Kersten
  */
 public class TaskListTableSorter extends ViewerSorter {
 
 	private final TaskListView view;
 
 	private String column;
 
	private ILabelProvider labelProvider;

 	private TaskKeyComparator taskKeyComparator = new TaskKeyComparator();
 
	public TaskListTableSorter(TaskListView view, String column, ILabelProvider labelProvider) {
 		super();
 		this.view = view;
 		this.column = column;
		this.labelProvider = labelProvider;
 	}
 
 	public void setColumn(String column) {
 		this.column = column;
 		if (view.isFocusedMode()) {
 			MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
 					TasksUiPlugin.TITLE_DIALOG, 
 					"Manual sorting is disabled in focused mode, sort order will not take effect until focused mode is disabled.");
 		}
 	}
 
 	/**
 	 * compare - invoked when column is selected calls the actual comparison
 	 * method for particular criteria
 	 */
 	@Override
 	public int compare(Viewer compareViewer, Object o1, Object o2) {
 		if (o1 instanceof AbstractTaskContainer && o2 instanceof TaskArchive) {
 			return -1;
 		} else if (o2 instanceof AbstractTaskContainer && o1 instanceof TaskArchive) {
 			return 1;
 		}
 
 		if (o1 instanceof AbstractTaskContainer && o2 instanceof ITask) {
 			return 1;
 		}
 		if (o1 instanceof AbstractTaskContainer || o1 instanceof AbstractRepositoryQuery) {
 			if (o2 instanceof AbstractTaskContainer || o2 instanceof AbstractRepositoryQuery) {
 				
 				return this.view.sortDirection
 						* ((ITaskListElement) o1).getSummary().compareToIgnoreCase(((ITaskListElement) o2).getSummary());
 			} else {
 				return -1;
 			}
 		} else if (o1 instanceof ITaskListElement) {
 			if (o2 instanceof AbstractTaskContainer || o2 instanceof AbstractRepositoryQuery) {
 				return -1;
 			} else if (o2 instanceof ITaskListElement) {
 				ITaskListElement element1 = (ITaskListElement) o1;
 				ITaskListElement element2 = (ITaskListElement) o2;
 
 				return compareElements(element1, element2);
 			}
 		} else {
 			return 0;
 		}
 		return 0;
 	}
 
 	private int compareElements(ITaskListElement element1, ITaskListElement element2) {
 		if (column != null && column.equals(this.view.columnNames[1])) {
 			return 0;
 		} else if (column == this.view.columnNames[2]) {
 			return this.view.sortDirection * element1.getPriority().compareTo(element2.getPriority());
 		} else if (column == this.view.columnNames[4]) {
			String c1 = labelProvider.getText(element1);
			String c2 = labelProvider.getText(element2);
 			return this.view.sortDirection * taskKeyComparator.compare(c1, c2);
 		} else {
 			return 0;
 		}
 	}
 }
