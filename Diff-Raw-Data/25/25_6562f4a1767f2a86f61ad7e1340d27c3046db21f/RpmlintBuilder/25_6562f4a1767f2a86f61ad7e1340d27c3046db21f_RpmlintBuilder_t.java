 /*******************************************************************************
  * Copyright (c) 2007 Alphonse Van Assche.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Alphonse Van Assche - initial API and implementation
  *******************************************************************************/
 package org.eclipse.linuxtools.rpm.rpmlint.builder;
 
 import java.util.ArrayList;
 import java.util.Map;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResourceDelta;
 import org.eclipse.core.resources.IncrementalProjectBuilder;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.OperationCanceledException;
 import org.eclipse.jface.text.Document;
 import org.eclipse.linuxtools.rpm.rpmlint.Activator;
 import org.eclipse.linuxtools.rpm.rpmlint.parser.RpmlintItem;
 import org.eclipse.linuxtools.rpm.rpmlint.parser.RpmlintParser;
 import org.eclipse.linuxtools.rpm.ui.editor.markers.SpecfileErrorHandler;
import org.eclipse.linuxtools.rpm.ui.editor.markers.SpecfileTaskHandler;
 import org.eclipse.linuxtools.rpm.ui.editor.parser.SpecfileParser;
 
 public class RpmlintBuilder extends IncrementalProjectBuilder {
 
 	public static final int MAX_WORKS = 100;
 
 	public static final String BUILDER_ID = Activator.PLUGIN_ID + ".rpmlintBuilder";
 
 	public static final String MARKER_ID = Activator.PLUGIN_ID +  ".rpmlintProblem";
 
 	private SpecfileParser specfileParser;
 
 	private SpecfileErrorHandler errorHandler;
	private SpecfileTaskHandler taskHandler;
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
 	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
 	 */
 	@Override
 	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
 			throws CoreException {
 		// TODO: handle the monitor in a more clean way.
 		monitor.beginTask("Check rpmlint problems", MAX_WORKS);
 		monitor.worked(20);
 		if (kind == FULL_BUILD) {
 			fullBuild(monitor);
 		} else {
 			IResourceDelta delta = getDelta(getProject());
 			if (delta == null) {
 				fullBuild(monitor);
 			} else {
 				incrementalBuild(delta, monitor);
 			}
 		}			
 		return null;
 	}
 
 	protected void fullBuild(IProgressMonitor monitor) throws CoreException {
 		RpmlintPreVisitor resourceVisitor = new RpmlintPreVisitor();
 		getProject().accept(resourceVisitor);
 		checkCancel(monitor);
 		monitor.worked(50);
 		monitor.setTaskName("Retrive Rpmlint problems...");
 		ArrayList<RpmlintItem> rpmlintItems = RpmlintParser.getInstance().parseVisisted(
 				resourceVisitor.getVisitedPaths());
 		visitAndMarkRpmlintItems(monitor, rpmlintItems);
 	}
 
 	protected void incrementalBuild(IResourceDelta delta,
 			IProgressMonitor monitor) throws CoreException {
 		RpmlintDeltaVisitor deltaVisitor = new RpmlintDeltaVisitor();
 		delta.accept(deltaVisitor);
 		monitor.worked(50);
 		monitor.setTaskName("Retrive Rpmlint problems...");
 		ArrayList<RpmlintItem> rpmlintItems = RpmlintParser.getInstance().parseVisisted(
 				deltaVisitor.getVisitedPaths());
 		visitAndMarkRpmlintItems(monitor, rpmlintItems);
 	}
 
 	private void visitAndMarkRpmlintItems(IProgressMonitor monitor,
 			ArrayList<RpmlintItem> rpmlintItems) throws CoreException {
 		if (rpmlintItems.size() > 0) {
 			checkCancel(monitor);
 			monitor.worked(70);
 			monitor.setTaskName("Add Rpmlint problems...");
 			getProject().accept(new RpmlintMarkerVisitor(this, rpmlintItems));
 			monitor.worked(MAX_WORKS);
 		}
 	}
 
 	protected SpecfileParser getSpecfileParser() {
 		if (specfileParser == null) {
 			specfileParser = new SpecfileParser();
 		}
 		return specfileParser;
 	}
 
 	protected SpecfileErrorHandler getSpecfileErrorHandler(IFile file,
 			String specContent) {
 		if (errorHandler == null) {
 			errorHandler = new SpecfileErrorHandler(file, new Document(
 					specContent));
 		} else {
 			errorHandler.setFile(file);
 			errorHandler.setDocument(new Document(specContent));
 		}
 		return errorHandler;
 	}
 
	public SpecfileTaskHandler getSpecfileTaskHandler(IFile file,
			String specContent) {
		if (taskHandler == null) {
			taskHandler = new SpecfileTaskHandler(file, new Document(
					specContent));
		} else {
			taskHandler.setFile(file);
			taskHandler.setDocument(new Document(specContent));
		}
		return taskHandler;
	}
 
 	protected void checkCancel(IProgressMonitor monitor) {
 		if (monitor.isCanceled()) {
 			throw new OperationCanceledException();
 		}
 	}
 }
