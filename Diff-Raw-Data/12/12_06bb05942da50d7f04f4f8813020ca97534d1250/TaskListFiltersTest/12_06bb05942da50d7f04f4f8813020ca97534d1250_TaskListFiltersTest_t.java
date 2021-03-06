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
 
 package org.eclipse.mylar.tests.integration;
 
 import java.util.Calendar;
 import java.util.List;
import java.util.Set;
 
 import junit.framework.TestCase;
 
 import org.eclipse.mylar.core.tests.UiTestUtil;
 import org.eclipse.mylar.internal.tasklist.ui.AbstractTaskListFilter;
 import org.eclipse.mylar.internal.tasklist.ui.views.TaskListView;
 import org.eclipse.mylar.internal.ui.TaskListInterestFilter;
 import org.eclipse.mylar.provisional.tasklist.ITask;
 import org.eclipse.mylar.provisional.tasklist.MylarTaskListPlugin;
 import org.eclipse.mylar.provisional.tasklist.Task;
 import org.eclipse.mylar.provisional.tasklist.TaskListManager;
 
 /**
  * @author Mik Kersten
  */
 public class TaskListFiltersTest extends TestCase {
 
 	private TaskListView view = TaskListView.getDefault();
 	
 	private TaskListManager manager = MylarTaskListPlugin.getTaskListManager();
 	
	private Set<AbstractTaskListFilter> previousFilters;
 	
 	private ITask taskCompleted;
 	
 	private ITask taskIncomplete;
 	
 	private ITask taskOverdue;
 	
 	private ITask taskDueToday;
 	
 	private ITask taskCompletedToday;
 	  
 	@Override
 	protected void setUp() throws Exception {
 		super.setUp();
 		assertNotNull(view);
 		previousFilters = view.getFilters();
		view.clearFilters(false);
 		
 		manager.getTaskList().reset();
 		assertEquals(0, manager.getTaskList().getAllTasks().size());
 		
 		taskCompleted = new Task("t-completed", "t-completed", true);
 		taskCompleted.setCompleted(true);
 		taskCompleted.setCompletionDate(manager.setSecheduledIn(Calendar.getInstance(), -1).getTime());
 		manager.getTaskList().addTask(taskCompleted, manager.getTaskList().getRootCategory());
 		
 		taskIncomplete = new Task("t-incomplete", "t-incomplete", true);
 		manager.getTaskList().addTask(taskIncomplete, manager.getTaskList().getRootCategory());
 		
 		taskOverdue = new Task("t-overdue", "t-overdue", true);
 		taskOverdue.setReminderDate(manager.setSecheduledIn(Calendar.getInstance(), -1).getTime());
 		manager.getTaskList().addTask(taskOverdue, manager.getTaskList().getRootCategory());
 		
 		taskDueToday = new Task("t-today", "t-today", true);
 		taskDueToday.setReminderDate(manager.setScheduledToday(Calendar.getInstance()).getTime());
 		manager.getTaskList().addTask(taskDueToday, manager.getTaskList().getRootCategory());
 		
 		taskCompletedToday = new Task("t-donetoday", "t-donetoday", true);
 		taskCompletedToday.setReminderDate(manager.setScheduledToday(Calendar.getInstance()).getTime());
 		taskCompletedToday.setCompleted(true);
 		manager.getTaskList().addTask(taskCompletedToday, manager.getTaskList().getRootCategory());
 	}
  
 	@Override
 	protected void tearDown() throws Exception {
 		super.tearDown();
		view.clearFilters(false);
 		for (AbstractTaskListFilter filter : previousFilters) {
 			view.addFilter(filter);
 		}
 	}
 
 	public void testInterestFilter() {
 		view.addFilter(new TaskListInterestFilter());
 		view.getViewer().refresh();
 		List<Object> items = UiTestUtil.getAllData(view.getViewer().getTree());
 		assertFalse(items.contains(taskCompleted));
 		assertFalse(items.contains(taskIncomplete));
 		assertTrue(items.contains(taskOverdue));
 		assertTrue(items.contains(taskDueToday));
 		assertTrue(items.contains(taskCompletedToday));
 		
 	}
 	
 	public void testNoFilters() {
 		assertEquals(0, view.getFilters().size());
 		view.getViewer().refresh();
 		assertEquals(6, view.getViewer().getTree().getItemCount());
 	}
 }
