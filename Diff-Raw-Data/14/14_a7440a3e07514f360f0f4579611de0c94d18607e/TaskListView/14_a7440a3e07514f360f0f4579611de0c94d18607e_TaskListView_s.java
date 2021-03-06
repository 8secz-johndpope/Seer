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
 
 package org.eclipse.mylar.tasks.ui.views;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Iterator;
 import java.util.List;
 
 import javax.security.auth.login.LoginException;
 
 import org.eclipse.jface.action.Action;
 import org.eclipse.jface.action.ActionContributionItem;
 import org.eclipse.jface.action.IMenuCreator;
 import org.eclipse.jface.action.IMenuListener;
 import org.eclipse.jface.action.IMenuManager;
 import org.eclipse.jface.action.IToolBarManager;
 import org.eclipse.jface.action.MenuManager;
 import org.eclipse.jface.action.Separator;
 import org.eclipse.jface.dialogs.Dialog;
 import org.eclipse.jface.dialogs.IDialogConstants;
 import org.eclipse.jface.dialogs.InputDialog;
 import org.eclipse.jface.dialogs.MessageDialog;
 import org.eclipse.jface.viewers.CellEditor;
 import org.eclipse.jface.viewers.CheckboxCellEditor;
 import org.eclipse.jface.viewers.ComboBoxCellEditor;
 import org.eclipse.jface.viewers.DoubleClickEvent;
 import org.eclipse.jface.viewers.ICellModifier;
 import org.eclipse.jface.viewers.IDoubleClickListener;
 import org.eclipse.jface.viewers.IStructuredContentProvider;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.jface.viewers.ITreeContentProvider;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.jface.viewers.TextCellEditor;
 import org.eclipse.jface.viewers.TreeViewer;
 import org.eclipse.jface.viewers.Viewer;
 import org.eclipse.jface.viewers.ViewerDropAdapter;
 import org.eclipse.jface.viewers.ViewerFilter;
 import org.eclipse.jface.viewers.ViewerSorter;
 import org.eclipse.jface.window.Window;
 import org.eclipse.mylar.core.ITaskscapeListener;
 import org.eclipse.mylar.core.MylarPlugin;
 import org.eclipse.mylar.dt.MylarWebRef;
 import org.eclipse.mylar.tasks.AbstractCategory;
 import org.eclipse.mylar.tasks.BugzillaHit;
 import org.eclipse.mylar.tasks.BugzillaQueryCategory;
 import org.eclipse.mylar.tasks.BugzillaTask;
 import org.eclipse.mylar.tasks.ITask;
 import org.eclipse.mylar.tasks.ITaskListElement;
 import org.eclipse.mylar.tasks.MylarTasksPlugin;
 import org.eclipse.mylar.tasks.Task;
 import org.eclipse.mylar.tasks.TaskCategory;
 import org.eclipse.mylar.tasks.ui.BugzillaTaskEditorInput;
 import org.eclipse.mylar.tasks.ui.TaskEditorInput;
 import org.eclipse.mylar.tasks.ui.actions.ClearContextAction;
 import org.eclipse.mylar.tasks.ui.actions.CreateBugzillaQueryCategoryAction;
 import org.eclipse.mylar.tasks.ui.actions.CreateBugzillaTaskAction;
 import org.eclipse.mylar.tasks.ui.actions.CreateCategoryAction;
 import org.eclipse.mylar.tasks.ui.actions.CreateTaskAction;
 import org.eclipse.mylar.tasks.ui.actions.DeleteAction;
 import org.eclipse.mylar.tasks.ui.actions.FilterCompletedTasksAction;
 import org.eclipse.mylar.tasks.ui.actions.MarkTaskCompleteAction;
 import org.eclipse.mylar.tasks.ui.actions.MarkTaskIncompleteAction;
 import org.eclipse.mylar.tasks.ui.actions.MoveTaskToRootAction;
 import org.eclipse.mylar.tasks.ui.actions.OpenTaskEditorAction;
 import org.eclipse.mylar.tasks.ui.actions.RefreshBugzillaAction;
 import org.eclipse.mylar.tasks.ui.actions.RefreshBugzillaReportsAction;
 import org.eclipse.mylar.tasks.ui.actions.TaskActivateAction;
 import org.eclipse.mylar.tasks.ui.actions.TaskDeactivateAction;
 import org.eclipse.mylar.ui.MylarImages;
 import org.eclipse.mylar.ui.MylarUiPlugin;
 import org.eclipse.mylar.ui.internal.views.Highlighter;
 import org.eclipse.mylar.ui.internal.views.HighlighterImageDescriptor;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.dnd.DND;
 import org.eclipse.swt.dnd.DragSourceEvent;
 import org.eclipse.swt.dnd.DragSourceListener;
 import org.eclipse.swt.dnd.TextTransfer;
 import org.eclipse.swt.dnd.Transfer;
 import org.eclipse.swt.dnd.TransferData;
 import org.eclipse.swt.events.ControlEvent;
 import org.eclipse.swt.events.ControlListener;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.events.SelectionListener;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Combo;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.swt.widgets.Menu;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.swt.widgets.Text;
 import org.eclipse.swt.widgets.TreeColumn;
 import org.eclipse.swt.widgets.TreeItem;
 import org.eclipse.ui.IActionBars;
 import org.eclipse.ui.IEditorInput;
 import org.eclipse.ui.IEditorPart;
 import org.eclipse.ui.IMemento;
 import org.eclipse.ui.IViewSite;
 import org.eclipse.ui.IWorkbenchActionConstants;
 import org.eclipse.ui.IWorkbenchPage;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.internal.Workbench;
 import org.eclipse.ui.part.DrillDownAdapter;
 import org.eclipse.ui.part.ViewPart;
 
 /**
  * @author Mik Kersten
  */
 public class TaskListView extends ViewPart {
 
 	private static TaskListView INSTANCE;
 	
 	TreeViewer viewer;
     private DrillDownAdapter drillDownAdapter;
     
     private RefreshBugzillaReportsAction refresh;
     private CreateTaskAction createTask;
     private CreateCategoryAction createCategory;
     private CreateBugzillaQueryCategoryAction createBugzillaQueryCategory;
     private CreateBugzillaTaskAction createBugzillaTask; 
 //    private RenameAction rename;
     private DeleteAction delete;
     private OpenTaskEditorAction doubleClickAction;
     private ClearContextAction clearSelectedTaskscapeAction;
 
     //private Action toggleIntersectionModeAction = new ToggleIntersectionModeAction();
 //    private Action toggleFilteringAction = new ToggleGlobalInterestFilteringAction();
 
     private MarkTaskCompleteAction completeTask;
     private MarkTaskIncompleteAction incompleteTask;
     private FilterCompletedTasksAction filterCompleteTask;
 //    private FilterIncompleteTasksAction filterInCompleteTask;
     private PriorityDropDownAction filterOnPriority;
     private Action moveTaskToRoot; 
     private RefreshBugzillaAction refreshQuery;
     private PriorityFilter priorityFilter = new PriorityFilter();
     
     protected String[] columnNames = new String[] { "", ".", "!", "Description" };
     protected int[] columnWidths = new int[] { 70, 20, 20, 120 };
     private TreeColumn[] columns;
     private IMemento taskListMemento;
     public static final String columnWidthIdentifier = "org.eclipse.mylar.tasks.ui.views.tasklist.columnwidth";
     public static final String tableSortIdentifier = "org.eclipse.mylar.tasks.ui.views.tasklist.sortIndex";
     private int sortIndex = 2;
     
     private static String[] PRIORITY_LEVELS = { "P1", "P2", "P3", "P4", "P5" };    
     
     private final class PriorityDropDownAction extends Action implements IMenuCreator {
     	private Menu dropDownMenu = null;
     	
 		public PriorityDropDownAction() {
 			super();
 			setText("Display Priorities");
 			setToolTipText("Show Tasks with Priority Levels");
 			setImageDescriptor(MylarImages.FILTER_PRIORITY);
 			setMenuCreator(this);			
 		}
     	
 		public void dispose() {			
 			if (dropDownMenu != null) {
 				dropDownMenu.dispose();
 				dropDownMenu = null;
 			}
 		}
 
 		public Menu getMenu(Control parent) {			
 			if (dropDownMenu != null) {
 				dropDownMenu.dispose();
 			}
 			dropDownMenu = new Menu(parent);
 			addActionsToMenu();
 			return dropDownMenu;
 		}
 
 		public Menu getMenu(Menu parent) {
 			if (dropDownMenu != null) {
 				dropDownMenu.dispose();
 			}
 			dropDownMenu = new Menu(parent);
 			addActionsToMenu();
 			return dropDownMenu;
 		}     
 		
 		public void addActionsToMenu() {
 			Action P1 = new Action(PRIORITY_LEVELS[0], AS_CHECK_BOX) {	    		
 	    		@Override
 				public void run() {
 	    			MylarTasksPlugin.setPriorityLevel(MylarTasksPlugin.PriorityLevel.P1);
 	    			priorityFilter.displayPrioritiesAbove(PRIORITY_LEVELS[0]);	    			
 	    			viewer.refresh();
 				}
 			};  
 			P1.setEnabled(true);
 			P1.setToolTipText(PRIORITY_LEVELS[0]);
 			ActionContributionItem item= new ActionContributionItem(P1);
 			item.fill(dropDownMenu, -1);
 			
 			Action P2 = new Action(PRIORITY_LEVELS[1], AS_CHECK_BOX) {	    		
 	    		@Override
 				public void run() {
 	    			MylarTasksPlugin.setPriorityLevel(MylarTasksPlugin.PriorityLevel.P2);
 	    			priorityFilter.displayPrioritiesAbove(PRIORITY_LEVELS[1]);	    			
 	    			viewer.refresh();
 				}
 			};  
 			P2.setEnabled(true);			
 			P2.setToolTipText(PRIORITY_LEVELS[1]);
 			item= new ActionContributionItem(P2);
 			item.fill(dropDownMenu, -1);
 			
 			Action P3 = new Action(PRIORITY_LEVELS[2], AS_CHECK_BOX) {	    		
 	    		@Override
 				public void run() { 
 	    			MylarTasksPlugin.setPriorityLevel(MylarTasksPlugin.PriorityLevel.P3);
 	    			priorityFilter.displayPrioritiesAbove(PRIORITY_LEVELS[2]);	    			
 	    			viewer.refresh();
 				}
 			};
 			P3.setEnabled(true);			
 			P3.setToolTipText(PRIORITY_LEVELS[2]);
 			item= new ActionContributionItem(P3);
 			item.fill(dropDownMenu, -1);
 			
 			Action P4 = new Action(PRIORITY_LEVELS[3], AS_CHECK_BOX) {	    		
 	    		@Override
 				public void run() {
 	    			MylarTasksPlugin.setPriorityLevel(MylarTasksPlugin.PriorityLevel.P4);
 	    			priorityFilter.displayPrioritiesAbove(PRIORITY_LEVELS[3]);	    			
 	    			viewer.refresh();
 				}
 			};
 			P4.setEnabled(true);			
 			P4.setToolTipText(PRIORITY_LEVELS[3]);
 			item= new ActionContributionItem(P4);
 			item.fill(dropDownMenu, -1);
 						
 			Action P5 = new Action(PRIORITY_LEVELS[4], AS_CHECK_BOX) {	    		
 	    		@Override
 				public void run() { 
 	    			MylarTasksPlugin.setPriorityLevel(MylarTasksPlugin.PriorityLevel.P5);
 	    			priorityFilter.displayPrioritiesAbove(PRIORITY_LEVELS[4]);	    			
 	    			viewer.refresh();
 	    		}
 			};  
 			P5.setEnabled(true);
 			P5.setToolTipText(PRIORITY_LEVELS[4]);
 			item= new ActionContributionItem(P5);
 			item.fill(dropDownMenu, -1);
 			
 			String priority = MylarTasksPlugin.getPriorityLevel();
 			if (priority.equals(PRIORITY_LEVELS[0])) {
 				P1.setChecked(true);
 			} else if (priority.equals(PRIORITY_LEVELS[1])) {
 				P1.setChecked(true);
 				P2.setChecked(true);
 			} else if (priority.equals(PRIORITY_LEVELS[2])) {
 				P1.setChecked(true);
 				P2.setChecked(true);
 				P3.setChecked(true);
 			} else if (priority.equals(PRIORITY_LEVELS[3])) {
 				P1.setChecked(true);
 				P2.setChecked(true);
 				P3.setChecked(true);
 				P4.setChecked(true);
 			} else if (priority.equals(PRIORITY_LEVELS[4])) {
 				P1.setChecked(true);
 				P2.setChecked(true);
 				P3.setChecked(true);
 				P4.setChecked(true);
 				P5.setChecked(true);
 			}
 		}
 		public void run() {	
 			this.setChecked(isChecked());
 		}
     }
     
     private ViewerFilter completeFilter = new ViewerFilter(){
 		@Override
 		public boolean select(Viewer viewer, Object parentElement, Object element) {
 			if (element instanceof ITask) {
 				return !((ITask)element).isCompleted();
 			} else if (element instanceof BugzillaHit){
 				BugzillaHit hit = (BugzillaHit)element;
 	        	BugzillaTask task = hit.getAssociatedTask();
 	        	if (task != null) {
 	        		return !task.isCompleted();
 	        	}
 				return true;
 			} else {
 				return true;
 			}
 		}    			
     };
     
     private ViewerFilter inCompleteFilter = new ViewerFilter(){
 		@Override
 		public boolean select(Viewer viewer, Object parentElement, Object element) {
 			if (element instanceof ITask) {
 				return ((ITask)element).isCompleted();
 			} else {
 				return true;
 			} 
 		}    			
     };
     
     public class PriorityFilter extends ViewerFilter {
     	private List<String> priorities = new ArrayList<String>();
     	
     	public PriorityFilter() {    		
     		displayPrioritiesAbove(MylarTasksPlugin.getPriorityLevel());
     	}
     	
     	public void displayPrioritiesAbove(String p) {
     		priorities.clear();    		
     		if (p.equals(PRIORITY_LEVELS[0])) {
     			priorities.add(PRIORITY_LEVELS[0]);
     		}
     		if (p.equals(PRIORITY_LEVELS[1])) {
     			priorities.add(PRIORITY_LEVELS[0]);
     			priorities.add(PRIORITY_LEVELS[1]);
     		} else if (p.equals(PRIORITY_LEVELS[2])) {
     			priorities.add(PRIORITY_LEVELS[0]);
     			priorities.add(PRIORITY_LEVELS[1]);
     			priorities.add(PRIORITY_LEVELS[2]);
     		} else if (p.equals(PRIORITY_LEVELS[3])) {
     			priorities.add(PRIORITY_LEVELS[0]);
     			priorities.add(PRIORITY_LEVELS[1]);
     			priorities.add(PRIORITY_LEVELS[2]);
     			priorities.add(PRIORITY_LEVELS[3]);
     		} else if (p.equals(PRIORITY_LEVELS[4])) {
     			priorities.add(PRIORITY_LEVELS[0]);
     			priorities.add(PRIORITY_LEVELS[1]);
     			priorities.add(PRIORITY_LEVELS[2]);
     			priorities.add(PRIORITY_LEVELS[3]);
     			priorities.add(PRIORITY_LEVELS[4]);
     		}
     	}
     	
     	public void hidePriority(String p) {
     		priorities.remove(p);
     	}
 		@Override
 		public boolean select(Viewer viewer, Object parentElement, Object element) {
 			if (element instanceof ITaskListElement) {
 				ITaskListElement task = (ITaskListElement) element;
 				if (priorities.size() == PRIORITY_LEVELS.length) {
 					return true;
 				} else {
 					return checkTask(task);
 				}								
 			} else {
 				return true;
 			}
 		}
 		private boolean checkTask(ITaskListElement task) {
 			for (String filter : priorities) {
 				if (task.getPriority().equals(filter)) {
 					return true;
 				}
 			}
 			return false;
 		}
     };
     
     class TaskListContentProvider implements IStructuredContentProvider, ITreeContentProvider {
         public void inputChanged(Viewer v, Object oldInput, Object newInput) {
         	// don't care if the input changes
         }
         public void dispose() {
         	// don't care if we are disposed
         }
         public Object[] getElements(Object parent) {
             if (parent.equals(getViewSite())) {
             	return MylarTasksPlugin.getTaskListManager().getTaskList().getRoots().toArray();            	          
             }
             return getChildren(parent);
         }
         public Object getParent(Object child) {
             if (child instanceof ITask) {
             	if (((ITask)child).getParent() != null) {
             		return ((ITask)child).getParent();
             	} else {
             		return ((ITask)child).getCategory();
             	}
                 
             }
             return null;
         }
         public Object [] getChildren(Object parent) {
         	if (parent instanceof TaskCategory) {
         		return ((TaskCategory)parent).getChildren().toArray();
         	} else if (parent instanceof Task) {
         		return ((Task)parent).getChildren().toArray();
         	} else if (parent instanceof BugzillaQueryCategory) {
         		return ((BugzillaQueryCategory) parent).getHits().toArray();
         	}
         	return new Object[0];
         }
         public boolean hasChildren(Object parent) {  
             if (parent instanceof TaskCategory) {
             	TaskCategory cat = (TaskCategory)parent;
                 return cat.getChildren() != null && cat.getChildren().size() > 0;
             }  else if (parent instanceof Task) {
             	Task t = (Task) parent;
             	return t.getChildren() != null && t.getChildren().size() > 0;
             } else if (parent instanceof BugzillaQueryCategory) {
             	BugzillaQueryCategory cat = (BugzillaQueryCategory)parent;
                 return cat.getHits() != null && cat.getHits().size() > 0;
             } 
             return false;
         }
     }
 
     public TaskListView() { 
     	INSTANCE = this;
     }
 
     class TaskListCellModifier implements ICellModifier {
 
         public boolean canModify(Object element, String property) {
             int columnIndex = Arrays.asList(columnNames).indexOf(property);
             if (element instanceof ITask) {
             	ITask task = (ITask) element;
                 switch (columnIndex) {
                 case 0: return true;
                 case 1: return false;
                 case 2: return !(task instanceof BugzillaTask);
                 case 3: return !(task instanceof BugzillaTask);
                 }
             } else if (element instanceof AbstractCategory) {
                 switch (columnIndex) {
                 case 0:
                 case 1: 
                 case 2:
                 	return false;
                 case 3: return true;
                 } 
             } else if (element instanceof BugzillaHit){
             	if (columnIndex == 0) {
             		return true;
             	}else {
             		return false;
             	}            	
             }
             return false;
         }
 
         public Object getValue(Object element, String property) {
             int columnIndex = Arrays.asList(columnNames).indexOf(property);
             if (element instanceof ITask) {
 				ITask task = (ITask) element;
 				switch (columnIndex) {
 				case 0:
 					return new Boolean(true); // return tyep doesn't matter
 				case 1:
 					return "";
 				case 2:
 					String priorityString = task.getPriority().substring(1);
 					return new Integer(priorityString);
 				case 3:
 					return task.getLabel();
 				}
 			} else if (element instanceof AbstractCategory) {
 				AbstractCategory cat = (AbstractCategory) element;
 				switch (columnIndex) {
 				case 0:
 					return new Boolean(false);
 				case 1:
 					return "";
 				case 2:
 					return "";
 				case 3:
 					return cat.getDescription(true);
 				}
 			} else if (element instanceof BugzillaHit) {
 				BugzillaHit hit = (BugzillaHit) element;
 				ITask task = hit.getAssociatedTask();
 				switch (columnIndex) {
 				case 0:
 					if(task == null){
 						return new Boolean(true);
 					} else {
 						return new Boolean(task.isCompleted());
 					}
 				case 1:
 					return "";
 				case 2:
 					String priorityString = hit.getPriority().substring(1);
 					return new Integer(priorityString);
 				case 3:
 					return hit.getDescription(true);					
 				}
 			}
             return "";
         }
 
 		public void modify(Object element, String property, Object value) {
 			int columnIndex = -1;
 			try {
 				columnIndex = Arrays.asList(columnNames).indexOf(property);
 				if (((TreeItem) element).getData() instanceof ITask) {
 
 					final ITask task = (ITask) ((TreeItem) element).getData();
 					switch (columnIndex) {
 					case 0:
 						if (task.isActive()) {
 							new TaskDeactivateAction(task, INSTANCE).run();
 						} else {
 							new TaskActivateAction(task).run();
 						}
 						viewer.setSelection(null);
 						break;
 					case 1:
 						break;
 					case 2:
 						Integer intVal = (Integer) value;
 						task.setPriority("P" + (intVal + 1));
 						viewer.setSelection(null);
 						break;
 					case 3:
 						task.setLabel(((String) value).trim());
 						MylarTasksPlugin.getTaskListManager()
 								.taskPropertyChanged(task, columnNames[3]);
 						viewer.setSelection(null);
 						break;
 					}
 				} else if (((TreeItem) element).getData() instanceof AbstractCategory) {
 					AbstractCategory cat = (AbstractCategory)((TreeItem) element).getData();
 					switch (columnIndex) {
 					case 0:						
 						viewer.setSelection(null);
 						break;
 					case 1:
 						break;
 					case 2:
 						break;
 					case 3:
 						cat.setDescription(((String) value).trim());
 						viewer.setSelection(null);
 						break;
 					}
 				} else if (((TreeItem) element).getData() instanceof BugzillaHit) {
 					BugzillaHit hit = (BugzillaHit)((TreeItem) element).getData();
 					switch (columnIndex) {
 					case 0:
 						BugzillaTask task = hit.getAssociatedTask();
 						if(task == null){
 							task = new BugzillaTask(hit);
 							hit.setAssociatedTask(task);
 							MylarTasksPlugin.getTaskListManager().getTaskList().addToBugzillaTaskRegistry(task);
 							// TODO move the task to a special folder
 						} 
 						if (task.isActive()) {
 							MylarTasksPlugin.getTaskListManager()
 									.deactivateTask(task);
 						} else {
 							MylarTasksPlugin.getTaskListManager().activateTask(
 									task);
 						}
 						viewer.setSelection(null);
 						break;
 					case 1:
 						break;
 					case 2:
 						break;
 					case 3:						
 						viewer.setSelection(null);
 						break;
 					}
 				}
 				viewer.refresh();
 			} catch (Exception e) {
 				MylarPlugin.log(e, e.getMessage());
 			}
 		}                
     }
     
     private class TaskListTableSorter extends ViewerSorter {
 
         private String column;
 
         public TaskListTableSorter(String column) {
             super();
             this.column = column;
         }
 
         /**
 		 * compare - invoked when column is selected calls the actual comparison
 		 * method for particular criteria
 		 */
         @Override
         public int compare(Viewer compareViewer, Object o1, Object o2) {
         	if (o1 instanceof AbstractCategory) {
         		if (o2 instanceof AbstractCategory) {
         			return ((AbstractCategory)o1).getDescription(false).compareTo(
         					((AbstractCategory)o2).getDescription(false));
         		} else {
         			return -1;
         		}
         	} else if(o1 instanceof ITask){
         		if (o2 instanceof AbstractCategory) {
         			return -1;
         		} else if(o2 instanceof ITask) {
         			
         			ITask task1 = (ITask) o1;
         			ITask task2 = (ITask) o2;
                     
                     if (task1.isCompleted()) return 1;
                     if (task2.isCompleted()) return -1;
                     if (column == columnNames[1]) {
                         if (task1 instanceof BugzillaTask && !(task2 instanceof BugzillaTask)) {
                             return 1;
                         } else {
                             return -1;
                         }
                     } else if (column == columnNames[2]) {
                         return task1.getPriority().compareTo(task2.getPriority());
                     } else if (column == columnNames[3]) {
                         return task1.getLabel().compareTo(task2.getLabel());
                     } else {
                     	return 0;
                     }
         		}
         	} else if(o1 instanceof BugzillaHit && o2 instanceof BugzillaHit){
         		BugzillaHit task1 = (BugzillaHit) o1;
         		BugzillaHit task2 = (BugzillaHit) o2;
                 
                 if (column == columnNames[1]) {
                     return 0;
                 } else if (column == columnNames[2]) {
                     return task1.getPriority().compareTo(task2.getPriority());
                 } else if (column == columnNames[3]) {
                     return task1.getDescription(false).compareTo(task2.getDescription(false));
                 }  else {
                 	return 0;
                 }
         	} else{
         		return 0;
         	}
         	return 0;
         }
     }
     
     @Override
     public void init(IViewSite site,IMemento memento) throws PartInitException {
     	init(site);
     	this.taskListMemento = memento;
     }
     
     @Override
     public void saveState(IMemento memento) {
 		IMemento colMemento = memento.createChild(columnWidthIdentifier);
 
 		for (int i = 0; i < columnWidths.length; i++) {
 			IMemento m = colMemento.createChild("col"+i);
 			m.putInteger("width", columnWidths[i]);
 		}
 		
 		IMemento sorter = memento.createChild(tableSortIdentifier);
 		IMemento m = sorter.createChild("sorter");
 		m.putInteger("sortIndex", sortIndex);
 	}
     
     private void restoreState() {
 		if (taskListMemento != null) {
 			IMemento taskListWidth = taskListMemento
 					.getChild(columnWidthIdentifier);
 			if (taskListWidth != null) {
 				for (int i = 0; i < columnWidths.length; i++) {
 					IMemento m = taskListWidth.getChild("col" + i);
 					if (m != null) {
 						int width = m.getInteger("width");
 						columnWidths[i] = width;
 						columns[i].setWidth(width);
 					}
 				}
 			}
 			IMemento sorterMemento = taskListMemento
 					.getChild(tableSortIdentifier);
 			if (sorterMemento != null) {
 				IMemento m = sorterMemento.getChild("sorter");
 				if (m != null) {
 					sortIndex = m.getInteger("sortIndex");
 				} else {
 					sortIndex = 2;
 				}
 			} else {
 				sortIndex = 2; // default priority
 			}
 			viewer.setSorter(new TaskListTableSorter(columnNames[sortIndex]));
 		}
         viewer.addFilter(priorityFilter);
         if (MylarTasksPlugin.getDefault().isFilterInCompleteMode()) viewer.addFilter(inCompleteFilter);
         if (MylarTasksPlugin.getDefault().isFilterCompleteMode()) viewer.addFilter(completeFilter);
         if (MylarTasksPlugin.getDefault().refreshOnStartUpEnabled()) {
         	refresh.setShowProgress(false);
         	refresh.run();
         	refresh.setShowProgress(true);
         }
         viewer.refresh();
     }
             
     /** 
      * This is a callback that will allow us
      * to create the viewer and initialize it.
      */
     @Override
     public void createPartControl(Composite parent) {    	    	
         viewer = new TreeViewer(parent, SWT.VERTICAL | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
         viewer.getTree().setHeaderVisible(true);
         viewer.getTree().setLinesVisible(true);
         viewer.setColumnProperties(columnNames);
         viewer.setUseHashlookup(true);    
                 
         columns = new TreeColumn[columnNames.length];
         for (int i = 0; i < columnNames.length; i++) {
             columns[i] = new TreeColumn(viewer.getTree(), 0); // SWT.LEFT
             columns[i].setText(columnNames[i]);
             columns[i].setWidth(columnWidths[i]);
             final int index = i;
             columns[i].addSelectionListener(new SelectionAdapter() {
             	
             	@Override
                 public void widgetSelected(SelectionEvent e) {
             		sortIndex = index;
                     viewer.setSorter(new TaskListTableSorter(columnNames[sortIndex]));
                 }
             });
             columns[i].addControlListener(new ControlListener () {
             	public void controlResized(ControlEvent e) {
             		for (int j = 0; j < columnWidths.length; j++) {
             			if (columns[j].equals(e.getSource())) {
             				columnWidths[j] = columns[j].getWidth();
             			}
             		}
             	}
 				public void controlMoved(ControlEvent e) {	
 					// don't care if the control is moved
 				}
             });
         }
          
         CellEditor[] editors = new CellEditor[columnNames.length];
         TextCellEditor textEditor = new TextCellEditor(viewer.getTree());
         ((Text) textEditor.getControl()).setOrientation(SWT.LEFT_TO_RIGHT);
         editors[0] = new CheckboxCellEditor();
         editors[1] = textEditor;
         editors[2] = new ComboBoxCellEditor(viewer.getTree(), PRIORITY_LEVELS, SWT.READ_ONLY);
         editors[3] = textEditor;
         viewer.setCellEditors(editors);   
         viewer.setCellModifier(new TaskListCellModifier());
         viewer.setSorter(new TaskListTableSorter(columnNames[sortIndex]));
         
         drillDownAdapter = new DrillDownAdapter(viewer);
         viewer.setContentProvider(new TaskListContentProvider());
         TaskListLabelProvider lp = new TaskListLabelProvider();
         lp.setBackgroundColor(parent.getBackground());
         viewer.setLabelProvider(lp);
         viewer.setInput(getViewSite());
         
         makeActions();
         hookContextMenu();
         hookDoubleClickAction();
         contributeToActionBars();       
         ToolTipHandler toolTipHandler = new ToolTipHandler(viewer.getControl().getShell());
         toolTipHandler.activateHoverHelp(viewer.getControl());
         
         initDragAndDrop(parent);
         expandToActiveTasks();
         restoreState();
    }
 
     @MylarWebRef(name="Drag and drop article", url="http://www.eclipse.org/articles/Article-Workbench-DND/drag_drop.html")
     private void initDragAndDrop(Composite parent) {
         Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
 
         viewer.addDragSupport(DND.DROP_MOVE, types, new DragSourceListener() {
 
             public void dragStart(DragSourceEvent event) {
                 if (((StructuredSelection) viewer.getSelection()).isEmpty()) {
                     event.doit = false;
                 }
             }
 
             public void dragSetData(DragSourceEvent event) {
                 StructuredSelection selection = (StructuredSelection) viewer.getSelection();
                 if (selection.getFirstElement() instanceof ITask) {
                 	if (!selection.isEmpty()) {
                         event.data = "" + ((ITask) selection.getFirstElement()).getHandle();
                     } else {
                         event.data = "null";
                     }
                 } else if (selection.getFirstElement() instanceof BugzillaHit) {
                 	if (!selection.isEmpty()) {
                         event.data = "" + ((BugzillaHit) selection.getFirstElement()).getHandle();
                     } else {
                         event.data = "null";
                     }
                 }
             }
 
             public void dragFinished(DragSourceEvent event) {
             	// don't care if the drag is done
             }
         });
 
         viewer.addDropSupport(DND.DROP_MOVE, types, new ViewerDropAdapter(viewer) {
             {
                 setFeedbackEnabled(false);
             }
 
             @Override
             public boolean performDrop(Object data) {
                 Object selectedObject = ((IStructuredSelection) ((TreeViewer) getViewer())
                         .getSelection()).getFirstElement();
                 if (selectedObject instanceof ITask) {
                     ITask source = (ITask) selectedObject;
                     if (source.getCategory() != null) {
                 		source.getCategory().removeTask(source);
                 	} else if (source.getParent() != null) {
                 		source.getParent().removeSubTask(source);
                 	} else {
                 		MylarTasksPlugin.getTaskListManager().getTaskList().getRootTasks().remove(source);
                 	}
                     
                     if (getCurrentTarget() instanceof TaskCategory) {
                     	((TaskCategory) getCurrentTarget()).addTask(source);
                     	source.setCategory((TaskCategory)getCurrentTarget());
                     } else if (getCurrentTarget() instanceof ITask) {
                     	ITask target = (ITask) getCurrentTarget();
                     	source.setCategory(null);
                     	target.addSubTask(source);                    	
                     	source.setParent(target);
                     }           
                     viewer.setSelection(null);
                     viewer.refresh();
                     return true;
                 } else if (selectedObject instanceof BugzillaHit) {
                 	BugzillaHit bh = (BugzillaHit) selectedObject;
                 	if (getCurrentTarget() instanceof TaskCategory) {
                 		TaskCategory cat = (TaskCategory) getCurrentTarget();
                 		if (bh.getAssociatedTask() != null) {
                     		bh.getAssociatedTask().setCategory(cat);
                     		cat.addTask(bh.getAssociatedTask());
                     	} else {
                     		BugzillaTask bt = new BugzillaTask(bh);
                     		bh.setAssociatedTask(bt);
                     		bt.setCategory(cat);
                     		cat.addTask(bt);
                     		MylarTasksPlugin.getTaskListManager().getTaskList().addToBugzillaTaskRegistry(bt);
                     	}
                 		viewer.setSelection(null);
                 		viewer.refresh();
                         return true;
                 	}                	
                 }
                 return false;
             }
 
             @Override
             public boolean validateDrop(Object targetObject, int operation,
                     TransferData transferType) {
                 Object selectedObject = ((IStructuredSelection) ((TreeViewer) getViewer())
                         .getSelection()).getFirstElement();
                 if (selectedObject instanceof ITask) {
                     if (getCurrentTarget() != null &&  getCurrentTarget() instanceof TaskCategory) {
                     	return true;
                     } else {
                     	return false;
                     }
                 } else if (selectedObject instanceof BugzillaHit) {
                 	if (getCurrentTarget() != null &&  getCurrentTarget() instanceof TaskCategory) {
                 		return true;
                 	} else {
                 		return false;
                 	}
                 }
                
                 return TextTransfer.getInstance().isSupportedType(transferType);
             }
 
         });
     }
     
     private void expandToActiveTasks() {
     	List<ITask> activeTasks = MylarTasksPlugin.getTaskListManager().getTaskList().getActiveTasks();
     	for (ITask t : activeTasks) {
     		viewer.expandToLevel(t, 0);
     	}
     }
 
     private void hookContextMenu() {
         MenuManager menuMgr = new MenuManager("#PopupMenu");
         menuMgr.setRemoveAllWhenShown(true);
         menuMgr.addMenuListener(new IMenuListener() {
             public void menuAboutToShow(IMenuManager manager) {
                 TaskListView.this.fillContextMenu(manager);
             }
         });
         Menu menu = menuMgr.createContextMenu(viewer.getControl());
         viewer.getControl().setMenu(menu);
         getSite().registerContextMenu(menuMgr, viewer);
     }
 
     private void contributeToActionBars() {
         IActionBars bars = getViewSite().getActionBars();
         fillLocalPullDown(bars.getMenuManager());
         fillLocalToolBar(bars.getToolBarManager());
     }
 
     private void fillLocalPullDown(IMenuManager manager) {
     	drillDownAdapter.addNavigationActions(manager);
 //        manager.add(createCategory);
 //        manager.add(new Separator());
 //        manager.add(createTask);
     }
 
     void fillContextMenu(IMenuManager manager) {
         manager.add(completeTask);
         manager.add(incompleteTask);
 //        manager.add(new Separator());
         manager.add(createTask);
         manager.add(createBugzillaTask);
 //        manager.add(rename);
         manager.add(delete);
         manager.add(clearSelectedTaskscapeAction);
         manager.add(moveTaskToRoot);
         manager.add(refreshQuery);
         manager.add(new Separator());
         MenuManager subMenuManager = new MenuManager("Choose Highlighter");
         final Object selectedObject = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
         for (Iterator<Highlighter> it = MylarUiPlugin.getDefault().getHighlighters().iterator(); it.hasNext();) {
             final Highlighter highlighter = it.next();
             if (selectedObject instanceof Task){
                 Action action = new Action() {
                 	
                 	@Override
                     public void run() { 
                         Task task = (Task)selectedObject;
                         MylarUiPlugin.getDefault().setHighlighterMapping(task.getHandle(), highlighter.getName());
                         TaskListView.this.viewer.refresh();
                         MylarPlugin.getTaskscapeManager().notifyPostPresentationSettingsChange(ITaskscapeListener.UpdateKind.HIGHLIGHTER);
 //                        taskscapeComponent.getTableViewer().refresh();
                     }
                 };
                 if (highlighter.isGradient()) {
                     action.setImageDescriptor(new HighlighterImageDescriptor(highlighter.getBase(), highlighter.getLandmarkColor()));
                 } else {
                     action.setImageDescriptor(new HighlighterImageDescriptor(highlighter.getLandmarkColor(), highlighter.getLandmarkColor()));
                 }
                 action.setText(highlighter.toString());
                 subMenuManager.add(action);
             } else {
 //                showMessage("Select task before choosing highlighter");
             }
         }
         manager.add(subMenuManager);
         manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
         updateActionEnablement(selectedObject);
     }
     
     private void updateActionEnablement(Object sel){
     	if(sel != null && sel instanceof ITaskListElement){
 	    	if(sel instanceof BugzillaHit){
 				BugzillaTask task = ((BugzillaHit)sel).getAssociatedTask();
 				if(task == null){
 					clearSelectedTaskscapeAction.setEnabled(false);
 				} else {
 					clearSelectedTaskscapeAction.setEnabled(true);
 				}
 				completeTask.setEnabled(false);
 				incompleteTask.setEnabled(false);
 				moveTaskToRoot.setEnabled(false);
 				delete.setEnabled(false);
 				refreshQuery.setEnabled(false);
 			} else if(sel instanceof BugzillaTask){
 				clearSelectedTaskscapeAction.setEnabled(true);
 				completeTask.setEnabled(false);
 				incompleteTask.setEnabled(false);
 				moveTaskToRoot.setEnabled(true);
 				delete.setEnabled(true);
 				refreshQuery.setEnabled(false);
 			} else if(sel instanceof AbstractCategory){
 				clearSelectedTaskscapeAction.setEnabled(false);
 				completeTask.setEnabled(false);
 				incompleteTask.setEnabled(false);
 				moveTaskToRoot.setEnabled(false);
 				delete.setEnabled(true);
 				if (sel instanceof BugzillaQueryCategory) {
 					refreshQuery.setEnabled(true);
 				} else {
 					refreshQuery.setEnabled(false);
 				}
 				//delete.setEnabled(true);
 			} else {
 				clearSelectedTaskscapeAction.setEnabled(true);
 				completeTask.setEnabled(true);
 				incompleteTask.setEnabled(true);
 				moveTaskToRoot.setEnabled(true);
 				delete.setEnabled(true);
 				refreshQuery.setEnabled(false);
 			}			
 		}else {
 			clearSelectedTaskscapeAction.setEnabled(false);
 			completeTask.setEnabled(false);
 			incompleteTask.setEnabled(false);
 			moveTaskToRoot.setEnabled(false);
 			delete.setEnabled(false);
 			refreshQuery.setEnabled(false);
 		}
     }
     
     private void fillLocalToolBar(IToolBarManager manager) {
         manager.add(createTask);
         manager.add(createCategory);
         manager.add(new Separator());
         manager.add(createBugzillaTask);        
     	manager.add(createBugzillaQueryCategory);
     	manager.add(refresh);
         manager.add(new Separator());
         manager.add(filterCompleteTask);
 //        manager.add(filterInCompleteTask);
         manager.add(filterOnPriority);        
     }
 
     /**
      * @see org.eclipse.pde.internal.ui.view.HistoryDropDownAction
      *
      */
     private void makeActions() {
     	refresh = new RefreshBugzillaReportsAction(this);      	               
         createTask = new CreateTaskAction(this);        
         createCategory = new CreateCategoryAction(this);
         createBugzillaQueryCategory = new CreateBugzillaQueryCategoryAction(this);
         createBugzillaTask = new CreateBugzillaTaskAction(this);                
         delete = new DeleteAction(this);
         completeTask = new MarkTaskCompleteAction(this);
         incompleteTask = new MarkTaskIncompleteAction(this);        
 //        rename = new RenameAction();        
         clearSelectedTaskscapeAction = new ClearContextAction(this);
         moveTaskToRoot = new MoveTaskToRootAction(this);
         doubleClickAction = new OpenTaskEditorAction(this);            
         filterCompleteTask = new FilterCompletedTasksAction(this);        
 //        filterInCompleteTask = new FilterIncompleteTasksAction();                        
         filterOnPriority = new PriorityDropDownAction();
         refreshQuery = new RefreshBugzillaAction(this);
     }
 
     /**
 	 * Recursive function that checks for the occurrence of a certain task id.
 	 * All children of the supplied node will be checked.
 	 * 
 	 * @param task
 	 *            The <code>ITask</code> object that is to be searched.
 	 * @param taskId
 	 *            The id that is being searched for.
 	 * @return <code>true</code> if the id was found in the node or any of its
 	 *         children
 	 */
     protected boolean lookForId(String taskId) {
     	for (ITask task : MylarTasksPlugin.getTaskListManager().getTaskList().getRootTasks()) {
     		if (task.getHandle().equals(taskId)) {
     			return true;
     		}
     	}
     	for (TaskCategory cat : MylarTasksPlugin.getTaskListManager().getTaskList().getTaskCategories()) {
     		for (ITask task : cat.getChildren()) {
         		if (task.getHandle().equals(taskId)) {
         			return true;
         		}
         	}
     	}
 		return false;
 	}
 	
 	public void closeTaskEditors(ITask task, IWorkbenchPage page) throws LoginException, IOException{
 		IEditorInput input = null;		
 		if (task instanceof BugzillaTask) {
 			input = new BugzillaTaskEditorInput((BugzillaTask)task);
 		} else if (task instanceof Task) {
 			input = new TaskEditorInput((Task) task);
 		}
 		IEditorPart editor = page.findEditor(input);
 
 		if (editor != null) {
 			page.closeEditor(editor, false);
 		}		
 	}
 	
 	public void refreshChildren(List<ITask> children) {
 		if (children != null) {
             for (ITask child : children) {
 				if (child instanceof BugzillaTask) {
 					((BugzillaTask)child).refresh();
 				}
 			}
 		}
 	}
 
 	private void hookDoubleClickAction() {
         viewer.addDoubleClickListener(new IDoubleClickListener() {
             public void doubleClick(DoubleClickEvent event) {
                 doubleClickAction.run();
             }
         });
     }
     
 	public void showMessage(String message) {
         MessageDialog.openInformation(
             viewer.getControl().getShell(),
             "Tasklist Message",
             message);
     }
 
     /**
      * Passing the focus request to the viewer's control.
      */
     @Override
     public void setFocus() {
         viewer.getControl().setFocus();
         //TODO: foo
     }
 
     public String getBugIdFromUser() {
         InputDialog dialog = new InputDialog(
             Workbench.getInstance().getActiveWorkbenchWindow().getShell(), 
             "Enter Bugzilla ID", 
             "Enter the Bugzilla ID: ", 
             "", 
             null);
         int dialogResult = dialog.open();
         if (dialogResult == Window.OK) { 
             return dialog.getValue();
         } else {
             return null;
         }
     }
     
     public String[] getLabelPriorityFromUser(String kind) {
     	String[] result = new String[2];
     	Dialog dialog = null;
     	boolean isTask = kind.equals("task");
     	if (isTask) {
     		dialog = new TaskInputDialog(
     	            Workbench.getInstance().getActiveWorkbenchWindow().getShell());
     	} else {
     		dialog = new InputDialog(
     				Workbench.getInstance().getActiveWorkbenchWindow().getShell(), 
     	            "Enter name", 
     	            "Enter a name for the " + kind + ": ", 
     	            "", 
     	            null);
     	}
     	
         int dialogResult = dialog.open();
         if (dialogResult == Window.OK) {
         	if (isTask) {
         		result[0] = ((TaskInputDialog)dialog).getTaskname();
         		result[1] = ((TaskInputDialog)dialog).getSelectedPriority();
         	} else {
         		result[0] = ((InputDialog)dialog).getValue();
         	}
         	return result;
         } else {
             return null;
         }
     }
     
     public void notifyTaskDataChanged(ITask task) {
         if (viewer.getTree() != null && !viewer.getTree().isDisposed()) { 
         	viewer.refresh();
         }
     }
     
     public static TaskListView getDefault() {
     	return INSTANCE;
     }
     
     public TreeViewer getViewer() {
     	return viewer;
     }
     
     public ViewerFilter getCompleteFilter() {
     	return completeFilter;
     }
     
     public ViewerFilter getInCompleteFilter() {
     	return inCompleteFilter;
     }
     
     public PriorityFilter getPriorityFilter() {
     	return priorityFilter;
     }
     public class TaskInputDialog extends Dialog {
     	private String taskName = "";
     	private String priority = "P3";
     	private Text text;
 		public TaskInputDialog(Shell parentShell) {
 			super(parentShell);
 		}
 		protected Control createDialogArea(Composite parent) {			
 			Composite composite = (Composite)super.createDialogArea(parent);
 			GridLayout gl = new GridLayout(3, false);
 			composite.setLayout(gl);
 			GridData data = new GridData(GridData.GRAB_HORIZONTAL
                     | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
                     | GridData.VERTICAL_ALIGN_CENTER);
             data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
             composite.setLayoutData(data);
 			
 			
 			Label label = new Label(composite, SWT.WRAP);
             label.setText("Task name:");            
             label.setFont(parent.getFont());
             
             text = new Text(composite, SWT.SINGLE | SWT.BORDER);
             text.setLayoutData(data);
             text.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                 | GridData.HORIZONTAL_ALIGN_FILL));
 
 			
 			final Combo c = new Combo(composite, SWT.NO_BACKGROUND
 						| SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY | SWT.DROP_DOWN);
 			c.setItems(PRIORITY_LEVELS);
 			c.setText(priority);
 			c.addSelectionListener(new SelectionListener() {
 
 				public void widgetSelected(SelectionEvent e) {
 					priority = c.getText();
 				}
 
 				public void widgetDefaultSelected(SelectionEvent e) {	
 					widgetSelected(e);
 				}				
 			});			
 			label = new Label(composite, SWT.NONE);
 			return composite;
 		}
 		public String getSelectedPriority() {
 			return priority;
 		}
 		public String getTaskname() {
 			return taskName;
 		}
 		protected void buttonPressed(int buttonId) {
 	        if (buttonId == IDialogConstants.OK_ID) {
 	        	taskName = text.getText();
 	        } else {
 	        	taskName = null;
 	        }
 	        super.buttonPressed(buttonId);
 	    }
 		protected void configureShell(Shell shell) {
 	        super.configureShell(shell);
 	        shell.setText("Enter Task Name");
 	    }
     };
 }
 
 //TextTransfer textTransfer = TextTransfer.getInstance();
 //DropTarget target = new DropTarget(viewer.getTree(), DND.DROP_MOVE);
 //target.setTransfer(new Transfer[] { textTransfer });
 //target.addDropListener(new TaskListDropTargetListener(parent, null, textTransfer, true));
 //
 //DragSource source = new DragSource(viewer.getTree(), DND.DROP_MOVE);
 //source.setTransfer(types); 
 
 //source.addDragListener(new DragSourceListener() {
 //public void dragStart(DragSourceEvent event) {
 //  if (((StructuredSelection)viewer.getSelection()).isEmpty()) { 
 //      event.doit = false; 
 //  }
 //}
 //public void dragSetData(DragSourceEvent event) {
 //  StructuredSelection selection = (StructuredSelection) viewer.getSelection();
 //  if (!selection.isEmpty()) { 
 //      event.data = "" + ((ITask)selection.getFirstElement()).getId();
 //  } else {
 //      event.data = "null";
 //  }
 //}
 //
 //public void dragFinished(DragSourceEvent event) { }
 //});
 
 
 //	public boolean getServerStatus() {
 //		return serverStatus;
 //	}
 //	
 //	/**
 //	 * Sets whether or not we could connect to the Bugzilla server. If
 //	 * necessary, the corresponding label in the view is updated.
 //	 * 
 //	 * @param canRead
 //	 *            <code>true</code> if the Bugzilla server could be connected
 //	 *            to
 //	 */
 //	public void setServerStatus(boolean canRead) {
 //		if (serverStatus != canRead) {
 //			serverStatus = canRead;
 //			updateServerStatusLabel();
 //		}
 //	}
 //	
 //	private void updateServerStatusLabel() {
 //		if (serverStatusLabel.isDisposed()) {
 //			return;
 //		}
 //		if (serverStatus) {
 //			serverStatusLabel.setText(CAN_READ_LABEL);
 //		}
 //		else {
 //			serverStatusLabel.setText(CANNOT_READ_LABEL);
 //		}
 //	}
 //	
 //	private class ServerPingJob extends Job {
 //		private boolean shouldCheckAgain = true;
 //		private int counter = 0;
 //		
 //		public ServerPingJob(String name) {
 //			super(name);
 //		}
 //		
 //		public void stopPinging() {
 //			shouldCheckAgain = false;
 //		}
 //
 //		protected IStatus run(IProgressMonitor monitor) {
 //			while (shouldCheckAgain) {
 //				try {
 //					final boolean canReadFromServer = TaskListView.checkServer();
 //					Workbench.getInstance().getDisplay().asyncExec(new Runnable() {
 //						public void run() {
 //							setServerStatus(canReadFromServer);
 //						}
 //					});
 //					Thread.sleep(10000/*MylarPreferencePage.getServerPing()*5000*/);
 //				} catch (InterruptedException e) {
 //					break;
 //				}
 //			}
 //			return new Status(IStatus.OK, MylarPlugin.IDENTIFIER, IStatus.OK, "", null);
 //		}
 //	}
 //	
 //	/**
 //	 * @return <code>true</code> if we could connect to the Bugzilla server
 //	 */
 //	public static boolean checkServer() {
 //		boolean canRead = true;
 //		BufferedReader in = null;
 //		
 //		// Call this function to intialize the Bugzilla url that the repository
 //		// is using.
 //		BugzillaRepository.getInstance();
 //
 //		try {
 //			// connect to the bugzilla server
 //			SSLContext ctx = SSLContext.getInstance("TLS");
 //			javax.net.ssl.TrustManager[] tm = new javax.net.ssl.TrustManager[]{new TrustAll()};
 //			ctx.init(null, tm, null);
 //			HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
 //			String urlText = "";
 //			
 //			// use the usename and password to get into bugzilla if we have it
 //			if(BugzillaPreferences.getUserName() != null && !BugzillaPreferences.getUserName().equals("") && BugzillaPreferences.getPassword() != null && !BugzillaPreferences.getPassword().equals(""))
 //			{
 //				/*
 //				 * The UnsupportedEncodingException exception for
 //				 * URLEncoder.encode() should not be thrown, since every
 //				 * implementation of the Java platform is required to support
 //				 * the standard charset "UTF-8"
 //				 */
 //				try {
 //					urlText += "?GoAheadAndLogIn=1&Bugzilla_login=" + URLEncoder.encode(BugzillaPreferences.getUserName(), "UTF-8") + "&Bugzilla_password=" + URLEncoder.encode(BugzillaPreferences.getPassword(), "UTF-8");
 //				} catch (UnsupportedEncodingException e) { }
 //			}
 //			
 //			URL url = new URL(BugzillaRepository.getURL() + "/enter_bug.cgi" + urlText);
 //			
 //			// create a new input stream for getting the bug
 //			in = new BufferedReader(new InputStreamReader(url.openStream()));
 //		}
 //		catch (Exception e) {
 //			// If there was an IOException, then there was a problem connecting.
 //			// If there was some other exception, then it was a problem not
 //			// related to the server.
 //			if (e instanceof IOException) {
 //				canRead = false;
 //			}
 //		}
 //
 //		// Close the BufferedReader if we opened one.
 //		try {
 //			if (in != null)
 //				in.close();
 //		} catch(IOException e) {}
 //		
 //		return canRead;
 //	}
 //
 //	public void dispose() {
 //		if (serverPingJob != null) {
 //			serverPingJob.stopPinging();
 //		}
 //		super.dispose();
 //	}
 
 //      source.addDragListener(new DragSourceListener() {
 //
 //            public void dragStart(DragSourceEvent event) {
 //                if (((StructuredSelection) viewer.getSelection()).getFirstElement() == null) {
 //                    event.doit = false;
 //                }
 //            }
 //
 //            public void dragSetData(DragSourceEvent event) {
 //                StructuredSelection selection = (StructuredSelection)viewer.getSelection();
 //                ITask task = (ITask) selection.getFirstElement();
 //                if (task != null) {
 //                    event.data = "" + task.getId();
 //                } else {
 //                    event.data = " ";
 //                }
 //            }
 //
 //            public void dragFinished(DragSourceEvent event) {
 //                StructuredSelection selection = (StructuredSelection)viewer.getSelection();
 //                if (selection.isEmpty()) {
 //                    return;
 //                } else {
 //                    ITask task = (ITask) selection.getFirstElement();
 //                    
//                    System.err.println(">>> got task: " + task + ">> " + );
 //
 //                }
 //            }
 //
 //        });
