 /*******************************************************************************
  * Copyright (c) 2000, 2004 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.jdt.internal.launching;
 
 import java.text.MessageFormat;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.debug.core.ILaunchConfiguration;
 import org.eclipse.jdt.core.ClasspathContainerInitializer;
 import org.eclipse.jdt.core.IClasspathContainer;
 import org.eclipse.jdt.core.IClasspathEntry;
 import org.eclipse.jdt.core.IJavaProject;
 import org.eclipse.jdt.core.JavaCore;
 import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
 import org.eclipse.jdt.launching.IRuntimeContainerComparator;
 import org.eclipse.jdt.launching.JavaRuntime;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 
 /**
  * Default user classpath entries for a Java project
  */
 public class DefaultProjectClasspathEntry extends AbstractRuntimeClasspathEntry {
 	
 	public static final String TYPE_ID = "org.eclipse.jdt.launching.classpathentry.defaultClasspath"; //$NON-NLS-1$
 	
 	/**
 	 * Default constructor need to instantiate extensions
 	 */
 	public DefaultProjectClasspathEntry() {
 	}
 	
 	/**
 	 * Constructs a new classpath entry for the given project.
 	 * 
 	 * @param project Java project
 	 */
 	public DefaultProjectClasspathEntry(IJavaProject project) {
 		setJavaProject(project);
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.jdt.internal.launching.AbstractRuntimeClasspathEntry#buildMemento(org.w3c.dom.Document, org.w3c.dom.Element)
 	 */
 	protected void buildMemento(Document document, Element memento) throws CoreException {
 		memento.setAttribute("project", getJavaProject().getElementName()); //$NON-NLS-1$
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.jdt.launching.IRuntimeClasspathEntry2#initializeFrom(org.w3c.dom.Element)
 	 */
 	public void initializeFrom(Element memento) throws CoreException {
 		String name = memento.getAttribute("project"); //$NON-NLS-1$
 		if (name == null) {
 			abort(LaunchingMessages.getString("DefaultProjectClasspathEntry.3"), null); //$NON-NLS-1$
 		}		
 		IJavaProject project = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(name));
 		setJavaProject(project);
 	}
 	/* (non-Javadoc)
 	 * @see org.eclipse.jdt.launching.IRuntimeClasspathEntry2#getTypeId()
 	 */
 	public String getTypeId() {
 		return TYPE_ID;
 	}
 	/* (non-Javadoc)
 	 * @see org.eclipse.jdt.launching.IRuntimeClasspathEntry#getType()
 	 */
 	public int getType() {
 		return OTHER;
 	}
 	
 	protected IProject getProject() {
 		return getJavaProject().getProject();
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.jdt.launching.IRuntimeClasspathEntry#getLocation()
 	 */
 	public String getLocation() {
 		return getProject().getLocation().toOSString();
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.jdt.launching.IRuntimeClasspathEntry#getPath()
 	 */
 	public IPath getPath() {
 		return getProject().getFullPath();
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.jdt.launching.IRuntimeClasspathEntry#getResource()
 	 */
 	public IResource getResource() {
 		return getProject();
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.jdt.launching.IRuntimeClasspathEntry2#getRuntimeClasspathEntries(org.eclipse.debug.core.ILaunchConfiguration)
 	 */
 	public IRuntimeClasspathEntry[] getRuntimeClasspathEntries(ILaunchConfiguration configuration) throws CoreException {
 		IClasspathEntry entry = JavaCore.newProjectEntry(getJavaProject().getProject().getFullPath());
 		List classpathEntries = new ArrayList(5);
 		List expanding = new ArrayList(5);
 		expandProject(entry, classpathEntries, expanding);
 		IRuntimeClasspathEntry[] runtimeEntries = new IRuntimeClasspathEntry[classpathEntries == null ? 0 : classpathEntries.size()];
 		for (int i = 0; i < runtimeEntries.length; i++) {
 			Object e = classpathEntries.get(i);
 			if (e instanceof IClasspathEntry) {
 				IClasspathEntry cpe = (IClasspathEntry)e;
 				runtimeEntries[i] = new RuntimeClasspathEntry(cpe);
 			} else {
 				runtimeEntries[i] = (IRuntimeClasspathEntry)e;				
 			}
 		}
 		// remove bootpath entries - this is a default user classpath
 		List ordered = new ArrayList(runtimeEntries.length);
 		for (int i = 0; i < runtimeEntries.length; i++) {
 			if (runtimeEntries[i].getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES) {
 				ordered.add(runtimeEntries[i]);
 			} 
 		}
 		return (IRuntimeClasspathEntry[]) ordered.toArray(new IRuntimeClasspathEntry[ordered.size()]);		
 	}
 	
 	/**
 	 * Returns the transitive closure of classpath entries for the
 	 * given project entry.
 	 * 
 	 * @param projectEntry project classpath entry
 	 * @param expandedPath a list of entries already expanded, should be empty
 	 * to begin, and contains the result
 	 * @param expanding a list of projects that have been or are currently being
 	 * expanded (to detect cycles)
 	 * @exception CoreException if unable to expand the classpath
 	 */
 	private static void expandProject(IClasspathEntry projectEntry, List expandedPath, List expanding) throws CoreException {
 		expanding.add(projectEntry);
 		// 1. Get the raw classpath
 		// 2. Replace source folder entries with a project entry
 		IPath projectPath = projectEntry.getPath();
 		IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(projectPath.lastSegment());
 		if (res == null) {
 			// add project entry and return
 			expandedPath.add(projectEntry);
 			return;
 		}
 		IJavaProject project = (IJavaProject)JavaCore.create(res);
 		if (project == null || !project.getProject().isOpen() || !project.exists()) {
 			// add project entry and return
 			expandedPath.add(projectEntry);
 			return;
 		}
 		
 		IClasspathEntry[] buildPath = project.getRawClasspath();
 		List unexpandedPath = new ArrayList(buildPath.length);
 		boolean projectAdded = false;
 		for (int i = 0; i < buildPath.length; i++) {
 			if (buildPath[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
 				if (!projectAdded) {
 					projectAdded = true;
 					unexpandedPath.add(projectEntry);
 				}
 			} else {
 				unexpandedPath.add(buildPath[i]);
 			}
 		}
 		// 3. expand each project entry (except for the root project)
 		// 4. replace each container entry with a runtime entry associated with the project
 		Iterator iter = unexpandedPath.iterator();
 		while (iter.hasNext()) {
 			IClasspathEntry entry = (IClasspathEntry)iter.next();
 			if (entry == projectEntry) {
 				expandedPath.add(entry);
 			} else {
 				switch (entry.getEntryKind()) {
 					case IClasspathEntry.CPE_PROJECT:
 						if (!expanding.contains(entry)) {
 							expandProject(entry, expandedPath, expanding);
 						}
 						break;
 					case IClasspathEntry.CPE_CONTAINER:
 						IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), project);
 						int property = -1;
 						if (container != null) {
 							switch (container.getKind()) {
 								case IClasspathContainer.K_APPLICATION:
 									property = IRuntimeClasspathEntry.USER_CLASSES;
 									break;
 								case IClasspathContainer.K_DEFAULT_SYSTEM:
 									property = IRuntimeClasspathEntry.STANDARD_CLASSES;
 									break;	
 								case IClasspathContainer.K_SYSTEM:
 									property = IRuntimeClasspathEntry.BOOTSTRAP_CLASSES;
 									break;
 							}
 							IRuntimeClasspathEntry r = JavaRuntime.newRuntimeContainerClasspathEntry(entry.getPath(), property, project);
 							// check for duplicate/redundant entries 
 							boolean duplicate = false;
 							ClasspathContainerInitializer initializer = JavaCore.getClasspathContainerInitializer(r.getPath().segment(0));
 							for (int i = 0; i < expandedPath.size(); i++) {
 								Object o = expandedPath.get(i);
 								if (o instanceof IRuntimeClasspathEntry) {
 									IRuntimeClasspathEntry re = (IRuntimeClasspathEntry)o;
 									if (re.getType() == IRuntimeClasspathEntry.CONTAINER) {
 										if (container instanceof IRuntimeContainerComparator) {
 											duplicate = ((IRuntimeContainerComparator)container).isDuplicate(re.getPath());
 										} else {
 											ClasspathContainerInitializer initializer2 = JavaCore.getClasspathContainerInitializer(re.getPath().segment(0));
											Object id1 = initializer.getComparisonID(r.getPath(), project);
 											Object id2 = initializer2.getComparisonID(re.getPath(), project);
 											if (id1 == null) {
 												duplicate = id2 == null;
 											} else {
 												duplicate = id1.equals(id2);
 											}
 										}
 										if (duplicate) {
 											break;
 										}
 									}
 								}
 							}
 							if (!duplicate) {
 								expandedPath.add(r);
 							}	
 						}
 						break;
 					case IClasspathEntry.CPE_VARIABLE:
 						if (entry.getPath().segment(0).equals(JavaRuntime.JRELIB_VARIABLE)) {
 							IRuntimeClasspathEntry r = JavaRuntime.newVariableRuntimeClasspathEntry(entry.getPath());
 							r.setSourceAttachmentPath(entry.getSourceAttachmentPath());
 							r.setSourceAttachmentRootPath(entry.getSourceAttachmentRootPath());
 							r.setClasspathProperty(IRuntimeClasspathEntry.STANDARD_CLASSES);
 							if (!expandedPath.contains(r)) {
 								expandedPath.add(r);
 							}
 							break;
 						}
 						// fall through if not the special JRELIB variable
 					default:
 						if (!expandedPath.contains(entry)) {
 							expandedPath.add(entry);
 						}
 						break;
 				}
 			}
 		}
 		return;
 	}	
 	/* (non-Javadoc)
 	 * @see org.eclipse.jdt.launching.IRuntimeClasspathEntry2#isComposite()
 	 */
 	public boolean isComposite() {
 		return true;
 	}
 	/* (non-Javadoc)
 	 * @see org.eclipse.jdt.launching.IRuntimeClasspathEntry2#getName()
 	 */
 	public String getName() {
 		return MessageFormat.format(LaunchingMessages.getString("DefaultProjectClasspathEntry.4"), new String[] {getJavaProject().getElementName()}); //$NON-NLS-1$
 	}
 	/* (non-Javadoc)
 	 * @see java.lang.Object#equals(java.lang.Object)
 	 */
 	public boolean equals(Object obj) {
 		return obj instanceof DefaultProjectClasspathEntry &&
 		 ((DefaultProjectClasspathEntry)obj).getJavaProject().equals(getJavaProject());
 	}
 	/* (non-Javadoc)
 	 * @see java.lang.Object#hashCode()
 	 */
 	public int hashCode() {
 		return getJavaProject().hashCode();
 	}
 }
