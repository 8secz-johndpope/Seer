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
 
 package org.eclipse.mylar.ide;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.mylar.core.IMylarContext;
 import org.eclipse.mylar.core.IMylarContextListener;
 import org.eclipse.mylar.core.IMylarElement;
 import org.eclipse.mylar.core.IMylarStructureBridge;
 import org.eclipse.mylar.core.MylarPlugin;
 import org.eclipse.mylar.core.internal.MylarContextManager;
 import org.eclipse.mylar.tasklist.ITask;
 import org.eclipse.mylar.tasklist.ITaskActivityListener;
 import org.eclipse.mylar.tasklist.MylarTasklistPlugin;
 import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
 import org.eclipse.team.internal.core.subscribers.ChangeSet;
 import org.eclipse.team.internal.core.subscribers.SubscriberChangeSetCollector;
 
 /**
  * @author Mik Kersten
  */
 public class MylarChangeSetManager implements IMylarContextListener {
 
 	private SubscriberChangeSetCollector collector;
 	
 	private Map<String, MylarContextChangeSet> activeChangeSets = new HashMap<String, MylarContextChangeSet>();
 		
 	private ITaskActivityListener TASK_ACTIVITY_LISTENER = new ITaskActivityListener() {
 
 		public void tasklistRead() {
 			initContextChangeSets();
 		}
 		
 		public void taskActivated(ITask task) {
 			// ignore
 		}
 
 		public void tasksActivated(List<ITask> tasks) {
 			// ignore
 		}
 
 		public void taskDeactivated(ITask task) {
 			// ignore
 		}
 
 		public void taskPropertyChanged(ITask updatedTask, String property) {
 			// ignore
 		}
 	};
 	
 	public MylarChangeSetManager() {
 		collector = CVSUIPlugin.getPlugin().getChangeSetManager();
 		MylarTasklistPlugin.getTaskListManager().addListener(TASK_ACTIVITY_LISTENER); // TODO: remove on stop?
 		if (MylarTasklistPlugin.getTaskListManager().isTaskListRead()) {
 			initContextChangeSets(); // otherwise listener will do it
 		}  
 	}
 
 	private void initContextChangeSets() {
 		ChangeSet[] sets = collector.getSets();
 		for (int i = 0; i < sets.length; i++) {
 			ChangeSet restoredSet = sets[i];
 			if (!(restoredSet instanceof MylarContextChangeSet)) {
 				String encodedTitle = restoredSet.getName();
 				String taskHandle = MylarContextChangeSet.getHandleFromPersistedTitle(encodedTitle);
 				ITask task = MylarTasklistPlugin.getTaskListManager().getTaskForHandle(taskHandle, true);
 				if (task != null) {				
 					try {
 						MylarContextChangeSet contextChangeSet = new MylarContextChangeSet(task, collector);
 						contextChangeSet.restoreResources(restoredSet.getResources());
 						collector.remove(restoredSet);
 						
 //						activeChangeSets.put(task.getHandleIdentifier(), contextChangeSet);
 						collector.add(contextChangeSet);
 					} catch (Exception e) {
 						MylarPlugin.fail(e, "could not restore change set", false);
 					}
 				}
 			}
 		}
 	}
 	
 	/**
 	 * For testing.
 	 */
 	public void clearActiveChangeSets() {
 		activeChangeSets.clear();
 	}
 	
 	public IResource[] getResources(ITask task) {
 		MylarContextChangeSet changeSet = activeChangeSets.get(task);
 		if (changeSet != null) {
 			return changeSet.getResources();
 		} else {
 			return null;
 		}
 	}
 	
 	public void contextActivated(IMylarContext context) {
 		try {
 			ITask task = getTask(context); 
 			if (task == null) {
 				MylarPlugin.log("could not resolve task for context", this);
 			} else if (!activeChangeSets.containsKey(task.getHandleIdentifier())) { 
 				MylarContextChangeSet changeSet = new MylarContextChangeSet(task, collector);
 				changeSet.add(changeSet.getResources());
 				activeChangeSets.put(task.getHandleIdentifier(), changeSet);
 				if (!collector.contains(changeSet)) collector.add(changeSet);
 			}
 		} catch (Exception e) {
 			MylarPlugin.fail(e, "could not update change set", false);
 		}
 	}
 
 	public void contextDeactivated(IMylarContext context) {
		// TODO: support multiple tasks
//		for (String taskHandle : activeChangeSets.keySet()) {
//			collector.remove(activeChangeSets.get(taskHandle));			
//		}
 //		activeChangeSets.clear();
 	}
 
 	public List<MylarContextChangeSet> getActiveChangeSets() {
 		return new ArrayList<MylarContextChangeSet>(activeChangeSets.values());
 	}
 	
 	private ITask getTask(IMylarContext context) {
 		List<ITask> activeTasks = MylarTasklistPlugin.getTaskListManager().getTaskList().getActiveTasks();
 		
 		// TODO: support multiple tasks
 		if (activeTasks.size() > 0) {
 			return activeTasks.get(0);
 		} else {
 			return null;
 		}
 	}
 	
 	public void interestChanged(IMylarElement element) {
 		IMylarStructureBridge bridge = MylarPlugin.getDefault().getStructureBridge(element.getContentType());
 		if (bridge.isDocument(element.getHandleIdentifier())) {
 			IResource resource = MylarIdePlugin.getDefault().getResourceForElement(element);
 			if (resource != null && resource.exists()) {
 				for (MylarContextChangeSet changeSet: getActiveChangeSets()) {
 					try {
 						if (!changeSet.contains(resource)) {
 							if (element.getInterest().isInteresting()) {
 								changeSet.add(new IResource[] { resource });
 							} 
 						} else if (shouldRemove(element)) {
 							changeSet.remove(resource);
 							
 							// HACK: touching ensures file is added outside of set
 							if (resource instanceof IFile) {
 								((IFile)resource).touch(new NullProgressMonitor());
 							}
 							if (!collector.contains(changeSet)) {
 								collector.add(changeSet);
 							}
 						}
 					} catch (Exception e) {
 						MylarPlugin.fail(e, "could not add resource to change set", false);
 					}
 				}
 			}
 		}
 	}
 
 	/**
 	 * Ignores decay.
 	 */
 	private boolean shouldRemove(IMylarElement element) {
 		// TODO: generalize this logic?
 		return (element.getInterest().getValue() + element.getInterest().getDecayValue()) 
 			< MylarContextManager.getScalingFactors().getInteresting();
 	}
 	
 	public void interestChanged(List<IMylarElement> elements) {
 		for (IMylarElement element : elements) {
 			interestChanged(element);
 		}
 	}
 
 	public void nodeDeleted(IMylarElement node) {
 		// TODO: handle?
 	}
 
 	public void landmarkAdded(IMylarElement node) {
 		// ignore
 	}
 
 	public void landmarkRemoved(IMylarElement node) {
 		// ignore
 	}
 
 	public void edgesChanged(IMylarElement node) {
 		// ignore
 	}
 
 	public void presentationSettingsChanging(UpdateKind kind) {
 		// ignore
 	}
 
 	public void presentationSettingsChanged(UpdateKind kind) {
 		// ignore
 	}
 
 }
