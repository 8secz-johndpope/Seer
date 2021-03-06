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
 
 package org.eclipse.mylar.java.tests;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IFolder;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.jdt.core.ICompilationUnit;
 import org.eclipse.jdt.core.IMethod;
 import org.eclipse.jdt.core.IPackageFragment;
 import org.eclipse.jdt.core.IType;
 import org.eclipse.jdt.core.JavaModelException;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.mylar.core.IMylarElement;
 import org.eclipse.mylar.core.InteractionEvent;
 import org.eclipse.mylar.core.MylarPlugin;
 import org.eclipse.mylar.ide.ResourceSelectionMonitor;
 import org.eclipse.mylar.ide.ResourceStructureBridge;
 import org.eclipse.mylar.ui.actions.AbstractInterestManipulationAction;
 import org.eclipse.ui.IWorkbenchPart;
 import org.eclipse.ui.internal.Workbench;
 
 /**
  * @author Mik Kersten
  */
 public class InterestManipulationTest extends AbstractJavaContextTest {
 
 	private IMylarElement method;
 	private IMylarElement clazz;
 	private IMylarElement cu;
     
 	private IMethod javaMethod;
 	private IType javaType;
 	private ICompilationUnit javaCu;
 	private IPackageFragment javaPackage;
 	
 	private IWorkbenchPart part = Workbench.getInstance().getActiveWorkbenchWindow().getActivePage().getActivePart();
 	
     @Override
 	protected void setUp() throws Exception {
 		super.setUp();
 		javaMethod = type1.createMethod("void testDecrement() { }", null, true, null);  
 		javaType = (IType)javaMethod.getParent();
 		javaCu = (ICompilationUnit) javaType.getParent();
 		javaPackage = (IPackageFragment)javaCu.getParent();
 	}
 
 	@Override
 	protected void tearDown() throws Exception {
 		super.tearDown();
 	}
 
	public void testDecrementAcrossBridges() throws JavaModelException {
		monitor.selectionChanged(part, new StructuredSelection(javaMethod));
        method = MylarPlugin.getContextManager().getElement(javaMethod.getHandleIdentifier());

 		IFile file = (IFile)javaCu.getAdapter(IResource.class);
         ResourceStructureBridge bridge = new ResourceStructureBridge();
 		new ResourceSelectionMonitor().selectionChanged(part, new StructuredSelection(file));
 		
 		IMylarElement fileElement = MylarPlugin.getContextManager().getElement(bridge.getHandleIdentifier(file));
 		IMylarElement projectElement = MylarPlugin.getContextManager().getElement(javaCu.getJavaProject().getHandleIdentifier());       
 		
         assertTrue(fileElement.getInterest().isInteresting());
 		assertTrue(method.getInterest().isInteresting());
         
         MylarPlugin.getContextManager().manipulateInterestForNode(projectElement, false, false, "test");
 
        assertFalse(method.getInterest().isInteresting());
         assertFalse(fileElement.getInterest().isInteresting());
     }
 	
 	/**
 	 * TODO: move to IDE tests?
 	 */
	public void testDecrementOfFile() throws JavaModelException {
 		IFolder folder = (IFolder)javaPackage.getAdapter(IResource.class);
 		IFile file = (IFile)javaCu.getAdapter(IResource.class);
 		ResourceStructureBridge bridge = new ResourceStructureBridge();
 		
 		new ResourceSelectionMonitor().selectionChanged(part, new StructuredSelection(file));
 		
 		IMylarElement folderElement = MylarPlugin.getContextManager().getElement(bridge.getHandleIdentifier(folder));
         IMylarElement fileElement = MylarPlugin.getContextManager().getElement(bridge.getHandleIdentifier(file));
 		        
         assertTrue(fileElement.getInterest().isInteresting());
 		assertTrue(folderElement.getInterest().isInteresting());
         
         MylarPlugin.getContextManager().manipulateInterestForNode(folderElement, false, false, "test");
 
         assertFalse(folderElement.getInterest().isInteresting());
         assertFalse(fileElement.getInterest().isInteresting());
     }
 	
 	public void testDecrementInterestOfCompilationUnit() throws JavaModelException {
         monitor.selectionChanged(part, new StructuredSelection(javaMethod));
         method = MylarPlugin.getContextManager().getElement(javaMethod.getHandleIdentifier());
         clazz = MylarPlugin.getContextManager().getElement(javaType.getHandleIdentifier());
         cu = MylarPlugin.getContextManager().getElement(javaCu.getHandleIdentifier());
 		
 		IMylarElement packageNode = MylarPlugin.getContextManager().getElement(javaPackage.getHandleIdentifier());        
         
         assertTrue(method.getInterest().isInteresting());
         assertTrue(clazz.getInterest().isInteresting());
         assertTrue(cu.getInterest().isInteresting());
         
         MylarPlugin.getContextManager().manipulateInterestForNode(packageNode, false, false, "test");
         assertFalse(packageNode.getInterest().isInteresting());
         assertFalse(cu.getInterest().isInteresting());
         assertFalse(clazz.getInterest().isInteresting());
         assertFalse(method.getInterest().isInteresting());
     }
 	
     public void testManipulation() throws JavaModelException {
     	InterestManipulationAction action = new InterestManipulationAction();
     	
     	IWorkbenchPart part = Workbench.getInstance().getActiveWorkbenchWindow().getActivePage().getActivePart();
         IMethod m1 = type1.createMethod("void m22() { }", null, true, null);     
         StructuredSelection sm1 = new StructuredSelection(m1);
         monitor.selectionChanged(part, sm1);
         IMylarElement node = MylarPlugin.getContextManager().getElement(m1.getHandleIdentifier());
         assertFalse(node.getInterest().isLandmark());
         assertNotNull(MylarPlugin.getContextManager().getActiveElement());
         action.changeInterestForSelected(true);
         assertTrue(node.getInterest().isLandmark());
         action.changeInterestForSelected(true);
         
         assertEquals(node.getInterest().getValue(), scaling.getLandmark() + scaling.get(InteractionEvent.Kind.SELECTION).getValue());
           
         action.changeInterestForSelected(false);
         assertFalse(node.getInterest().isLandmark());
         assertTrue(node.getInterest().isInteresting());
         action.changeInterestForSelected(false);
         assertFalse(node.getInterest().isInteresting());  
         assertEquals(node.getInterest().getValue(), -scaling.get(InteractionEvent.Kind.SELECTION).getValue());
         action.changeInterestForSelected(false);
         assertEquals(node.getInterest().getValue(), -scaling.get(InteractionEvent.Kind.SELECTION).getValue());
     }
 	
 	class InterestManipulationAction extends AbstractInterestManipulationAction {
 		
 		@Override
 		protected boolean isIncrement() {
 			return true;
 		}
 
 		public void changeInterestForSelected(boolean increment) {
 			MylarPlugin.getContextManager().manipulateInterestForNode(MylarPlugin.getContextManager().getActiveElement(), increment, false, "");
 		}
 	}
 }
