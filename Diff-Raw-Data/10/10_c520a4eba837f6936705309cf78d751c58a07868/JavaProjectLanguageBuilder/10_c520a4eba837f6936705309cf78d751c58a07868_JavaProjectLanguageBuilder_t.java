 /*******************************************************************************
  * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *******************************************************************************/
 package org.eclipse.xtext.ui.core.builder.impl;
 
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedHashSet;
 import java.util.Set;
 
 import org.apache.log4j.Logger;
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.IResourceVisitor;
 import org.eclipse.core.resources.IStorage;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.emf.common.util.URI;
 import org.eclipse.emf.common.util.WrappedException;
 import org.eclipse.emf.emfindex.ContainerDescriptor;
 import org.eclipse.emf.emfindex.query.ContainerDescriptorQuery;
 import org.eclipse.emf.emfindex.query.QueryExecutor;
 import org.eclipse.emf.emfindex.query.QueryResult;
 import org.eclipse.emf.emfindex.store.IndexUpdater;
 import org.eclipse.emf.emfindex.store.UpdateCommand;
 import org.eclipse.jdt.core.ElementChangedEvent;
 import org.eclipse.jdt.core.IElementChangedListener;
 import org.eclipse.jdt.core.IJarEntryResource;
 import org.eclipse.jdt.core.IJavaElement;
 import org.eclipse.jdt.core.IJavaElementDelta;
 import org.eclipse.jdt.core.IJavaModel;
 import org.eclipse.jdt.core.IJavaProject;
 import org.eclipse.jdt.core.IPackageFragmentRoot;
 import org.eclipse.jdt.core.JavaCore;
 import org.eclipse.jdt.core.JavaModelException;
 import org.eclipse.xtext.ui.core.util.JdtURIUtil;
 import org.eclipse.xtext.ui.core.util.JdtUtil;
 
 import com.google.common.collect.ImmutableSet;
 import com.google.inject.Inject;
 
 /**
  * @author Sven Efftinge - Initial contribution and API
  */
 public class JavaProjectLanguageBuilder extends DefaultLanguageBuilder implements IElementChangedListener {
 
 	private static Logger log = Logger.getLogger(JavaProjectLanguageBuilder.class);
 
 	@Inject
 	protected JdtURIUtil jdtURIUtil;
 
 	public JavaProjectLanguageBuilder() {
 		JavaCore.addElementChangedListener(this);
 	}
 
 	@Override
 	public void clean(IProgressMonitor monitor) {
 		final String javaProject = getJavaProject().getElementName();
 		index.executeUpdateCommand(new UpdateCommand<Void>() {
 			public Void execute(IndexUpdater indexUpdater, QueryExecutor queryExecutor) {
 				QueryResult<ContainerDescriptor> result = queryExecutor.execute(new ContainerDescriptorQuery());
 				Set<String> toBeDeleted = new HashSet<String>();
 				for (ContainerDescriptor containerDescriptor : result) {
 					if (javaProject.equals(containerDescriptor.getUserData(MANAGED_BY)))
 						toBeDeleted.add(containerDescriptor.getName());
 				}
 				for (String string : toBeDeleted) {
 					indexUpdater.deleteContainer(string);
 				}
 				return null;
 			}
 		});
 	}
 
 	protected void delete(final IPackageFragmentRoot root) {
 		if (isManaged(root)) {
 			index.executeUpdateCommand(new UpdateCommand<Void>() {
 				public Void execute(IndexUpdater indexUpdater, QueryExecutor queryExecutor) {
 					indexUpdater.deleteContainer(getContainerName(root));
 					return null;
 				}
 			});
 		}
 	}
 
 	@Override
 	protected void fullBuild(IProgressMonitor monitor) throws CoreException {
 		super.fullBuild(monitor);
		try {
			IPackageFragmentRoot[] roots = getJavaProject().getPackageFragmentRoots();
			for (IPackageFragmentRoot root : roots) {
				build(root);
			}
		} catch (JavaModelException jme) {
			log.error(jme.getMessage());
 		}
 	}
 
 	/**
 	 * checks whether the package fragment root is managed by this project builder
 	 */
 	protected boolean isManaged(final IPackageFragmentRoot root) {
 		try {
 			if ("org.eclipse.jdt.launching.JRE_CONTAINER".equals(root.getRawClasspathEntry().getPath().toString()))
 				return false;
 			if (root.getKind() == IPackageFragmentRoot.K_SOURCE)
 				return true;
 			if (root.isArchive()) {
 				UpdateCommand<Boolean> updateCommand = new UpdateCommand<Boolean>() {
 
 					public Boolean execute(IndexUpdater indexUpdater, QueryExecutor queryExecutor) {
 						final String name = getContainerName(root);
 						ContainerDescriptorQuery query = new ContainerDescriptorQuery();
 						query.setName(name);
 						QueryResult<ContainerDescriptor> result = queryExecutor.execute(query);
 						Iterator<ContainerDescriptor> iterator = result.iterator();
 						if (iterator.hasNext()) {
 							ContainerDescriptor descriptor = iterator.next();
 							return builder.getProject().getName().equals(descriptor.getUserData(MANAGED_BY));
 						} else {
 							indexUpdater.createContainer(name, getUserDataForContainer(name));
 						}
 						return true;
 					}
 				};
 				return index.executeUpdateCommand(updateCommand);
 			}
 			return false;
 		} catch (JavaModelException e) {
 			log.error("skipping " + root);
 			return false;
 		}
 	}
 
 	@Override
 	protected boolean isLanguageResource(IResource resource) {
 		if (!super.isLanguageResource(resource))
 			return false;
 		if (!isJavaProject())
 			return true;
 		if (JdtUtil.isInSourceFolder(getJavaProject(), resource))
 			return true;
 		return false;
 	}
 
 	private IJavaProject getJavaProject() {
 		IProject project = builder.getProject();
 		IJavaProject jp = JavaCore.create(project);
 		return jp;
 	}
 
 	protected boolean isJavaProject() {
 		return getJavaProject() != null;
 	}
 
 	@Override
 	protected void build(IStorage storage) {
 		if (storage instanceof IFile) {
 			try {
 				IPackageFragmentRoot[] roots = getJavaProject().getPackageFragmentRoots();
 				for (IPackageFragmentRoot root : roots) {
 					if (storage.equals(root.getUnderlyingResource())) {
 						build(root);
 					}
 				}
 			} catch (JavaModelException e) {
 				throw new RuntimeException(e);
 			}
 		}
 		super.build(storage);
 	}
 
 	protected void build(IPackageFragmentRoot root) {
 		if (!isManaged(root))
 			return;
 		if (root.isArchive()) {
 			try {
 				new JarWalker() {
 					@Override
 					protected void handle(IJarEntryResource jarEntry) {
 						if (hasRightFileExtension(jarEntry.getFullPath())) {
 							build(jarEntry);
 						}
 					}
 				}.traverse(root);
 			} catch (JavaModelException e) {
 				throw new WrappedException(e);
 			}
 		} else {
 			try {
 				if (root.getUnderlyingResource() == null)
 					return;
 				IResourceVisitor visitor = new IResourceVisitor() {
 					public boolean visit(IResource resource) throws CoreException {
 						if ((resource instanceof IFile) && isLanguageResource(resource))
 							build((IFile) resource);
 						return true;
 					}
 				};
 				root.getUnderlyingResource().accept(visitor);
 			} catch (JavaModelException e) {
 				throw new WrappedException(e);
 			} catch (CoreException e) {
 				throw new WrappedException(e);
 			}
 		}
 	}
 
 	@Override
 	protected URI getURI(IStorage resource) {
 		if (resource instanceof IJarEntryResource) {
 			return jdtURIUtil.getURI((IJarEntryResource) resource);
 		}
 		return super.getURI(resource);
 	}
 
 	protected URI getURI(IPackageFragmentRoot root) {
 		return jdtURIUtil.getURI(root);
 	}
 
 	@Override
 	protected String getContainerName(IStorage storage) {
 		if (storage instanceof IJarEntryResource) {
 			IJarEntryResource iJarEntryResource = (IJarEntryResource) storage;
 			return getContainerName(iJarEntryResource.getPackageFragmentRoot());
 		} else if (storage instanceof IFile) {
 			try {
 				IPackageFragmentRoot[] roots = getJavaProject().getPackageFragmentRoots();
 				for (IPackageFragmentRoot root : roots) {
 					if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
 						if (root.getUnderlyingResource().contains((IFile) storage)) {
 							return getContainerName(root);
 						}
 					}
 				}
 			} catch (JavaModelException e) {
 				throw new WrappedException(e);
 			}
 
 		}
 		return super.getContainerName(storage);
 	}
 
 	public void elementChanged(ElementChangedEvent event) {
 		try {
 			if (event.getDelta() != null) {
 				Set<IJavaElementDelta> deltas = getPackageFragmentRootDeltas(event.getDelta());
 				for (IJavaElementDelta delta : deltas) {
 					IPackageFragmentRoot element = (IPackageFragmentRoot) delta.getElement();
 					if (delta.getKind() == IJavaElementDelta.REMOVED
 							|| (delta.getFlags() & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) {
 						delete(element);
 					} else if (delta.getKind() == IJavaElementDelta.ADDED
 							|| (delta.getFlags() & IJavaElementDelta.F_ADDED_TO_CLASSPATH) != 0) {
 						build(element);
 					}
 				}
 			}
 		} catch (WrappedException e) {
 			log.error(e.getCause().getMessage(), e.getCause());
 		} catch (RuntimeException e) {
 			log.error(e.getMessage(), e);
 		}
 	}
 
 	protected Set<IJavaElementDelta> getPackageFragmentRootDeltas(IJavaElementDelta delta) {
 		IJavaElement element = delta.getElement();
 		if (element instanceof IPackageFragmentRoot) {
 			return ImmutableSet.of(delta);
 		} else if (element instanceof IJavaModel) {
 			return getPackageFragmentRootDeltas(delta.getAffectedChildren());
 		} else if (element instanceof IJavaProject) {
 			if (getJavaProject().equals(element)) {
 				return getPackageFragmentRootDeltas(delta.getAffectedChildren());
 			}
 		}
 		return Collections.emptySet();
 	}
 
 	private Set<IJavaElementDelta> getPackageFragmentRootDeltas(IJavaElementDelta[] affectedChildren) {
 		LinkedHashSet<IJavaElementDelta> set = new LinkedHashSet<IJavaElementDelta>();
 		for (IJavaElementDelta delta : affectedChildren) {
 			set.addAll(getPackageFragmentRootDeltas(delta));
 		}
 		return set;
 	}
 
 	protected String getContainerName(final IPackageFragmentRoot root) {
 		return jdtURIUtil.getURI(root).toString();
 	}
 }
