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
 
 package org.eclipse.mylar.tasks.tests;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Vector;
 
 import junit.framework.TestCase;
 
 import org.eclipse.jface.action.IContributionItem;
 import org.eclipse.jface.action.MenuManager;
 import org.eclipse.jface.viewers.TreeViewer;
 import org.eclipse.mylar.internal.tasks.core.WebTask;
 import org.eclipse.mylar.internal.tasks.ui.MoveToCategoryMenuContributor;
 import org.eclipse.mylar.internal.tasks.ui.TaskPriorityFilter;
 import org.eclipse.mylar.internal.tasks.ui.TaskUiUtil;
 import org.eclipse.mylar.internal.tasks.ui.actions.MarkTaskCompleteAction;
 import org.eclipse.mylar.internal.tasks.ui.actions.NewCategoryAction;
 import org.eclipse.mylar.internal.tasks.ui.views.TaskListView;
 import org.eclipse.mylar.tasks.core.ITask;
 import org.eclipse.mylar.tasks.core.ITaskListChangeListener;
 import org.eclipse.mylar.tasks.core.ITaskListElement;
 import org.eclipse.mylar.tasks.core.Task;
 import org.eclipse.mylar.tasks.core.TaskCategory;
 import org.eclipse.mylar.tasks.tests.connector.MockRepositoryQuery;
 import org.eclipse.mylar.tasks.ui.TaskListManager;
 import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
 import org.eclipse.swt.widgets.TreeItem;
 import org.eclipse.ui.PartInitException;
 
 /**
  * Tests TaskListView's filtering mechanism.
  * 
  * @author Ken Sueda
  * 
  */
 public class TaskListUiTest extends TestCase {
 	
 	private TaskListManager manager = null;
 
 	private TaskCategory cat1 = null;
 
 	private Task cat1task1 = null;
 
 	private Task cat1task2 = null;
 
 	private Task cat1task3 = null;
 
 	private Task cat1task4 = null;
 
 	private Task cat1task5 = null;
 
 	private Task cat1task1sub1 = null;
 
 	private TaskCategory cat2 = null;
 
 	private Task cat2task1 = null;
 
 	private Task cat2task2 = null;
 
 	private Task cat2task3 = null;
 
 	private Task cat2task4 = null;
 
 	private Task cat2task5 = null;
 
 	private Task cat2task1sub1 = null;
 
 	private final static int CHECK_COMPLETE_FILTER = 1;
 
 	private final static int CHECK_INCOMPLETE_FILTER = 2;
 
 	private final static int CHECK_PRIORITY_FILTER = 3;
 
 	public void setUp() throws PartInitException {
 		try {
 			TaskListView.openInActivePerspective();
 			manager = TasksUiPlugin.getTaskListManager();
 			cat1 = new TaskCategory("First Category", manager.getTaskList());
 
 			cat1task1 = new Task(TasksUiPlugin.getTaskListManager().genUniqueTaskHandle(), "task 1", true);
 			cat1task1.setPriority(Task.PriorityLevel.P1.toString());
 			cat1task1.setCompleted(true);
 			cat1task1.setContainer(cat1);
 			manager.getTaskList().moveToContainer(cat1, cat1task1);
 
 			cat1task1sub1 = new Task(TasksUiPlugin.getTaskListManager().genUniqueTaskHandle(), "sub task 1", true);
 			cat1task1sub1.setPriority(Task.PriorityLevel.P1.toString());
 			cat1task1sub1.setCompleted(true);
 			cat1task1sub1.setParent(cat1task1);
 			cat1task1.addSubTask(cat1task1sub1);
 
 			cat1task2 = new Task(TasksUiPlugin.getTaskListManager().genUniqueTaskHandle(), "task 2", true);
 			cat1task2.setPriority(Task.PriorityLevel.P2.toString());
 			cat1task2.setContainer(cat1);
 			manager.getTaskList().moveToContainer(cat1, cat1task2);
 
 			cat1task3 = new Task(TasksUiPlugin.getTaskListManager().genUniqueTaskHandle(), "task 3", true);
 			cat1task3.setPriority(Task.PriorityLevel.P3.toString());
 			cat1task3.setCompleted(true);
 			cat1task3.setContainer(cat1);
 			manager.getTaskList().moveToContainer(cat1, cat1task3);
 
 			cat1task4 = new Task(TasksUiPlugin.getTaskListManager().genUniqueTaskHandle(), "task 4", true);
 			cat1task4.setPriority(Task.PriorityLevel.P4.toString());
 			cat1task4.setContainer(cat1);
 			manager.getTaskList().moveToContainer(cat1, cat1task4);
 
 			cat1task5 = new Task(TasksUiPlugin.getTaskListManager().genUniqueTaskHandle(), "task 5", true);
 			cat1task5.setPriority(Task.PriorityLevel.P5.toString());
 			cat1task5.setCompleted(true);
 			cat1task5.setContainer(cat1);
 			manager.getTaskList().moveToContainer(cat1, cat1task5);
 
 			manager.getTaskList().addCategory(cat1);
 			assertEquals(cat1.getChildren().size(), 5);
 
 			cat2 = new TaskCategory("Second Category", manager.getTaskList());
 
 			cat2task1 = new Task(TasksUiPlugin.getTaskListManager().genUniqueTaskHandle(), "task 1", true);
 			cat2task1.setPriority(Task.PriorityLevel.P1.toString());
 			cat2task1.setContainer(cat2);
 			manager.getTaskList().moveToContainer(cat2, cat2task1);
 
 			cat2task1sub1 = new Task(TasksUiPlugin.getTaskListManager().genUniqueTaskHandle(), "sub task 1", true);
 			cat2task1sub1.setPriority(Task.PriorityLevel.P1.toString());
 			cat2task1sub1.setParent(cat2task1);
 			cat2task1.addSubTask(cat2task1sub1);
 
 			cat2task2 = new Task(TasksUiPlugin.getTaskListManager().genUniqueTaskHandle(), "task 2", true);
 			cat2task2.setPriority(Task.PriorityLevel.P2.toString());
 			cat2task2.setCompleted(true);
 			cat2task2.setContainer(cat2);
 			manager.getTaskList().moveToContainer(cat2, cat2task2);
 
 			cat2task3 = new Task(TasksUiPlugin.getTaskListManager().genUniqueTaskHandle(), "task 3", true);
 			cat2task3.setPriority(Task.PriorityLevel.P3.toString());
 			cat2task3.setContainer(cat2);
 			manager.getTaskList().moveToContainer(cat2, cat2task3);
 
 			cat2task4 = new Task(TasksUiPlugin.getTaskListManager().genUniqueTaskHandle(), "task 4", true);
 			cat2task4.setPriority(Task.PriorityLevel.P4.toString());
 			cat2task4.setCompleted(true);
 			cat2task4.setContainer(cat2);
 			manager.getTaskList().moveToContainer(cat2, cat2task4);
 
 			cat2task5 = new Task(TasksUiPlugin.getTaskListManager().genUniqueTaskHandle(), "task 5", true);
 			cat2task5.setPriority(Task.PriorityLevel.P5.toString());
 			cat2task5.setContainer(cat2);
 			manager.getTaskList().moveToContainer(cat2, cat2task5);
 
 			manager.getTaskList().addCategory(cat2);
 			manager.saveTaskList();
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 	}
 
 	public void tearDown() {
 		// clear everything
 	}
 
 	public void testMarkWebTaskCompleted() {
 		TaskListView view = TaskListView.getFromActivePerspective();
 		assertNotNull(view);
 		WebTask webTask = new WebTask("1", "1", "", "", "web");
 		TasksUiPlugin.getTaskListManager().getTaskList().addTask(webTask,
 				TasksUiPlugin.getTaskListManager().getTaskList().getRootCategory());
 		view.getViewer().refresh();
 		// Arrays.asList(view.getViewer().getVisibleExpandedElements());
 		assertFalse(webTask.isCompleted());
 		ArrayList<ITaskListElement> tasks = new ArrayList<ITaskListElement>();
 		tasks.add(webTask);
 		new MarkTaskCompleteAction(tasks).run();
 		assertTrue(webTask.isCompleted());
 	}
 
 	public void testUiFilter() {
 		try {
 			assertNotNull(TaskListView.getFromActivePerspective());
 			TreeViewer viewer = TaskListView.getFromActivePerspective().getViewer();
 			TaskListView.getFromActivePerspective().addFilter(
 					TaskListView.getFromActivePerspective().getCompleteFilter());
 			viewer.refresh();
 			viewer.expandAll();
 			TreeItem[] items = viewer.getTree().getItems();
 			assertTrue(checkFilter(CHECK_COMPLETE_FILTER, items));
 			TaskListView.getFromActivePerspective().removeFilter(
 					TaskListView.getFromActivePerspective().getCompleteFilter());
 
 			TaskPriorityFilter filter = (TaskPriorityFilter) TaskListView.getFromActivePerspective()
 					.getPriorityFilter();
 			filter.displayPrioritiesAbove("P2");
 			TaskListView.getFromActivePerspective().addFilter(filter);
 			viewer.refresh();
 			viewer.expandAll();
 			items = viewer.getTree().getItems();
 
 			// check priority tasks
 			assertTrue(checkFilter(CHECK_PRIORITY_FILTER, items));
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 	}
 
 	/**
 	 * Tests that TaskEditors remove all listeners when closed
 	 */
 	public void testListenersRemoved() {
 
 		int numListenersBefore = 0;
 		int numListenersDuring = 0;
 		int numListenersAfter = 0;
 
 		TaskListManager manager = TasksUiPlugin.getTaskListManager();
 		List<ITaskListChangeListener> listeners = manager.getTaskList().getChangeListeners();
 		numListenersBefore = listeners.size();
 
 		// open a task in editor
 		// cat1task1.setForceSyncOpen(true);
 		TaskUiUtil.openEditor(cat1task1, false, true);
 		// cat1task1.openTaskInEditor(false);
 		// cat1task2.setForceSyncOpen(true);
 		// cat1task2.openTaskInEditor(false);
 		TaskUiUtil.openEditor(cat1task2, false, true);
 
 		listeners = manager.getTaskList().getChangeListeners();
 		numListenersDuring = listeners.size();
 
 		assertEquals(numListenersDuring, numListenersBefore + 2);
 
 		TasksUiPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
 
 		listeners = manager.getTaskList().getChangeListeners();
 		numListenersAfter = listeners.size();
 		assertEquals(numListenersBefore, numListenersAfter);
 
 	}
 	
	public void testGetSubMenuManagerContainsNewCategoryAction() {
		// setup
		MoveToCategoryMenuContributor moveToMenuContrib = new MoveToCategoryMenuContributor();
		List<ITaskListElement> selectedElements = new Vector<ITaskListElement>();
		selectedElements.add(cat1task1);
		selectedElements.add(cat1task2);
		selectedElements.add(cat2task1);
		
		
		// execute sytem under test
		moveToMenuContrib.getSubMenuManager(selectedElements);
		
		// assert
		
		
		// teardown
	}
 	
 	/**
 	 * Tests whether an additional NewCategory action is added to the category
 	 */
 	public void testGetSubMenuManagerContainsAllCategoriesPlusNewCategory() {
 		// setup
 		MoveToCategoryMenuContributor moveToMenuContrib = new MoveToCategoryMenuContributor();
 		List<ITaskListElement> selectedElements = new Vector<ITaskListElement>();
 		selectedElements.add(cat1task1);
 		int nrOfCategoriesMinusArchiveContainer = manager.getTaskList().getCategories().size() - 1;
		int expectedNrOfSubMenuEntries = nrOfCategoriesMinusArchiveContainer + 1;
 		NewCategoryAction newCatActon = new NewCategoryAction(null);
 
 		// execute sytem under test
 		MenuManager menuManager = moveToMenuContrib.getSubMenuManager(selectedElements);
 		IContributionItem[] items = menuManager.getItems();
 		IContributionItem item = items[menuManager.getItems().length-1];
 
 		// +1 for separator
 		assertEquals(expectedNrOfSubMenuEntries+1, menuManager.getItems().length);
 
 		if (item instanceof NewCategoryAction) {
 			NewCategoryAction action = (NewCategoryAction) item;
 			assertEquals(newCatActon.getText(), action.getText());
 		}
 		
 		// teardown
 	}
 
 	
 	/**
 	 * Tests visibility of SubMenuManager
 	 */
 	public void testVisibilityOfSubMenuManager(){
 		//setup
 		MoveToCategoryMenuContributor moveToMenuContrib = new MoveToCategoryMenuContributor();
 		MenuManager menuManager = null;
 		List<ITaskListElement> selectedElements = new Vector<ITaskListElement>();
 		selectedElements.add(cat1task1);
 		
 		List<ITaskListElement> emptySelection = new Vector<ITaskListElement>();
 
 		List<ITaskListElement> categorySelection = new Vector<ITaskListElement>();
 		categorySelection.add(cat1);
 		
 		List<ITaskListElement> querySelection = new Vector<ITaskListElement>();
 		querySelection.add(new MockRepositoryQuery("query", null));
 		
 		//execute system under test & assert
 		menuManager = moveToMenuContrib.getSubMenuManager(selectedElements);
 		assertTrue(menuManager.isVisible());
 		
 		menuManager = null;
 		menuManager = moveToMenuContrib.getSubMenuManager(emptySelection);
 		assertFalse(menuManager.isVisible());
 		
 		menuManager = null;
 		menuManager = moveToMenuContrib.getSubMenuManager(categorySelection);
 		assertFalse(menuManager.isVisible());
 		
 		menuManager = null;
 		menuManager = moveToMenuContrib.getSubMenuManager(querySelection);
 		assertFalse(menuManager.isVisible());
 		
 		//teardown
 	}
 	
 	
 	/**
 	 * Tests that the category name is shown in the Move To Category submenu, even when they have an @ in their name
 	 */
 	public void testCategoryNameIsShownInMoveToCategoryAction() {
 		String catNameWithAtBefore = "@CatName";
 		String catNameWithAtExpected = "@CatName@";
 		String catNameWithAtActual = "";
 		
 		String catNameNoAtBefore = "CatName";
 		String catNameNoAtExpected = "CatName";
 		String catNameNoAtActual = "";
 		
 		MoveToCategoryMenuContributor menuContrib = new MoveToCategoryMenuContributor();
 		
 		catNameWithAtActual = menuContrib.handleAcceleratorKeys(catNameWithAtBefore);
 		catNameNoAtActual = menuContrib.handleAcceleratorKeys(catNameNoAtBefore);
 		
 		assertEquals(catNameWithAtExpected, catNameWithAtActual);
 		assertEquals(catNameNoAtExpected, catNameNoAtActual);
 	}
 
 	public boolean checkFilter(int type, TreeItem[] items) {
 		switch (type) {
 		case CHECK_COMPLETE_FILTER:
 			return checkCompleteIncompleteFilter(items, false);
 		case CHECK_INCOMPLETE_FILTER:
 			return checkCompleteIncompleteFilter(items, true);
 		case CHECK_PRIORITY_FILTER:
 			return checkPriorityFilter(items);
 		default:
 			return false;
 		}
 	}
 
 	public boolean checkCompleteIncompleteFilter(TreeItem[] items, boolean checkComplete) {
 		assertEquals(2, items.length);
 		int count = 0;
 		for (int i = 0; i < items.length; i++) {
 			assertTrue(items[i].getData() instanceof TaskCategory);
 			TreeItem[] sub = items[i].getItems();
 			for (int j = 0; j < sub.length; j++) {
 				assertTrue(sub[j].getData() instanceof ITask);
 				ITask task = (ITask) sub[j].getData();
 				if (checkComplete) {
 					assertTrue(task.isCompleted());
 				} else {
 					assertFalse(task.isCompleted());
 				}
 				count++;
 			}
 		}
 		assertTrue(count == 5);
 		return true;
 	}
 
 	public boolean checkPriorityFilter(TreeItem[] items) {
 		assertTrue(items.length == 2);
 		int p2Count = 0;
 		int p1Count = 0;
 		for (int i = 0; i < items.length; i++) {
 			assertTrue(items[i].getData() instanceof TaskCategory);
 			TreeItem[] sub = items[i].getItems();
 			for (int j = 0; j < sub.length; j++) {
 				assertTrue(sub[j].getData() instanceof ITask);
 				ITask task = (ITask) sub[j].getData();
 				assertTrue(task.getPriority().equals("P2") || task.getPriority().equals("P1"));
 				if (task.getPriority().equals("P2")) {
 					p2Count++;
 				} else {
 					p1Count++;
 				}
 			}
 		}
 		assertEquals(2, p1Count);
 		assertEquals(2, p2Count);
 		return true;
 	}
 
 }
