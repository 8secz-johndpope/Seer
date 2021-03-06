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
 
 package org.eclipse.mylar.ide.tests;
 
 import java.util.List;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.mylar.core.IMylarElement;
 import org.eclipse.mylar.core.MylarPlugin;
 import org.eclipse.mylar.ide.MylarChangeSetManager;
 import org.eclipse.mylar.ide.MylarContextChangeSet;
 import org.eclipse.mylar.ide.MylarIdePlugin;
 import org.eclipse.mylar.tasklist.MylarTasklistPlugin;
 import org.eclipse.mylar.tasklist.Task;
 
 /**
  * @author Mik Kersten
  */
 public class ChangeSetManagerTest extends AbstractResourceContextTest {
 
 	private MylarChangeSetManager changeSetManager;
 		
     @Override
     protected void setUp() throws Exception {
     	super.setUp();
     	assertNotNull(MylarIdePlugin.getDefault());
     	changeSetManager = MylarIdePlugin.getDefault().getChangeSetManager();
     	assertNotNull(changeSetManager);
     }
     
     @Override
     protected void tearDown() throws Exception {
     	super.tearDown();
     }
     
     public void testSingleContextActivation() {
     	manager.contextDeactivated(taskId, taskId);
     	changeSetManager.clearActiveChangeSets();
     	assertEquals(0, changeSetManager.getActiveChangeSets().size());
     	Task task1 = new Task("task1", "label", true);
     	MylarTasklistPlugin.getTaskListManager().activateTask(task1);
     	assertEquals(1, changeSetManager.getActiveChangeSets().size()); 
     	MylarTasklistPlugin.getTaskListManager().deactivateTask(task1); 
    	assertEquals(1, changeSetManager.getActiveChangeSets().size());
     	MylarTasklistPlugin.getTaskListManager().deactivateTask(task1);
     }
     
     public void testContentsAfterDecay() throws CoreException {
 		IFile file = project.getProject().getFile(new Path("foo.txt"));
 		file.create(null, true, null);
 		  
 		Task task1 = new Task("task1", "label", true);
     	MylarTasklistPlugin.getTaskListManager().activateTask(task1);
 		
 		monitor.selectionChanged(navigator, new StructuredSelection(file));
 		IMylarElement fileElement = MylarPlugin.getContextManager().getElement(structureBridge.getHandleIdentifier(file));
 		assertTrue(fileElement.getInterest().isInteresting());
 		
 		List<MylarContextChangeSet> changeSets = changeSetManager.getActiveChangeSets();
 		assertEquals(1, changeSets.size());
 		MylarContextChangeSet set = changeSets.get(0); 
 		IResource[] resources = set.getResources();
 		assertEquals(1, resources.length);
 		
         for (int i = 0; i < 1/(scaling.getDecay().getValue())*3; i++) {
             MylarPlugin.getContextManager().handleInteractionEvent(mockSelection());            
         }
         assertTrue(fileElement.getInterest().getValue() < 0);
         assertEquals(1, resources.length);
         
         MylarTasklistPlugin.getTaskListManager().deactivateTask(task1);
     }
 }
