 /*******************************************************************************
  * Copyright (c) 2007 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.jdt.debug.tests.refactoring;
 
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.jdt.core.IJavaElement;
 import org.eclipse.jdt.core.IJavaProject;
 import org.eclipse.jdt.core.JavaModelException;
 import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
 import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgDestinationFactory;
 import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
 import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
 import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
 import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
 import org.eclipse.ltk.core.refactoring.Refactoring;
 import org.eclipse.ltk.core.refactoring.RefactoringCore;
 import org.eclipse.ltk.core.refactoring.RefactoringStatus;
 import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
 
 /**
  * Common code for setting up and performing a move refactoring.
  * 
  * @since 3.4
  */
 public class MoveRefactoringTest extends AbstractRefactoringDebugTest {
 
 	/**
 	 * @param name
 	 */
 	public MoveRefactoringTest(String name) {
 		super(name);
 	}
 
 	/** Configures a processor for refactoring
 	 * @param javaProject
 	 * @param type
 	 * @return the configured processor that will be used in refactoring
 	 * @throws JavaModelException
 	 */
 	protected JavaMoveProcessor setupRefactor(IJavaProject javaProject, IJavaElement type) throws JavaModelException {
 		IMovePolicy movePolicy= ReorgPolicyFactory.createMovePolicy(
 				new IResource[0], 
 				new IJavaElement[] {type});
 		JavaMoveProcessor processor= new JavaMoveProcessor(movePolicy);
 		IJavaElement destination= getPackageFragmentRoot(javaProject, "src").getPackageFragment("a.b").getCompilationUnit("MoveeRecipient.java"); 
 		processor.setDestination(ReorgDestinationFactory.createDestination(destination));
 		processor.setReorgQueries(new MockReorgQueries());
		if(processor.canUpdateReferences())
 			processor.setUpdateReferences(true);//assuming is properly set otherwise
 		return processor;
 	}
 	
 	/** Sets up a refactoring and executes it.
 	 * @param javaProject
 	 * @param cunit
 	 * @throws JavaModelException
 	 * @throws Exception
 	 */
 	protected void refactor(IJavaProject javaProject, IJavaElement type) throws JavaModelException, Exception {
 		JavaMoveProcessor processor = setupRefactor(javaProject, type);
 		executeRefactoring(new MoveRefactoring(processor), RefactoringStatus.WARNING);
 	}
 	
 	protected void executeRefactoring(Refactoring refactoring, int maxSeverity) throws Exception {
 		PerformRefactoringOperation operation= new PerformRefactoringOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
 		waitForBuild();
 		// Flush the undo manager to not count any already existing undo objects
 		// into the heap consumption
 		RefactoringCore.getUndoManager().flush();
 
 		ResourcesPlugin.getWorkspace().run(operation, null);
 
 		assertEquals(true, operation.getConditionStatus().getSeverity() <= maxSeverity);
 		assertEquals(true, operation.getValidationStatus().isOK());
 
 		RefactoringCore.getUndoManager().flush();
 	}	
 }
