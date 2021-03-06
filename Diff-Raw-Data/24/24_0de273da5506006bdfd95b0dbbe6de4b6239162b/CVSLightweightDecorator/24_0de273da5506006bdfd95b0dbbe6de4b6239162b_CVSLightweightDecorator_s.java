 /*******************************************************************************
  * Copyright (c) 2000, 2004 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.team.internal.ccvs.ui;
 
 
 import java.text.SimpleDateFormat;
 import java.util.*;
 
 import org.eclipse.core.resources.*;
 import org.eclipse.core.resources.mapping.ResourceMapping;
 import org.eclipse.core.runtime.*;
 import org.eclipse.jface.preference.IPreferenceStore;
 import org.eclipse.jface.util.IPropertyChangeListener;
 import org.eclipse.jface.util.PropertyChangeEvent;
 import org.eclipse.jface.viewers.*;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.team.core.RepositoryProvider;
 import org.eclipse.team.core.diff.IDiffNode;
 import org.eclipse.team.core.diff.IThreeWayDiff;
 import org.eclipse.team.internal.ccvs.core.*;
 import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
 import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
 import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
 import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
 import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
 import org.eclipse.team.internal.ccvs.core.util.ResourceStateChangeListeners;
 import org.eclipse.team.internal.core.ExceptionCollector;
 import org.eclipse.team.internal.ui.Utils;
 import org.eclipse.team.ui.TeamUI;
 import org.eclipse.team.ui.mapping.SynchronizationStateTester;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.themes.ITheme;
 import org.osgi.framework.Bundle;
 
 public class CVSLightweightDecorator extends LabelProvider implements ILightweightLabelDecorator, IResourceStateChangeListener, IPropertyChangeListener {
 
 	// Decorator id as defined in the decorator extension point
 	public final static String ID = "org.eclipse.team.cvs.ui.decorator"; //$NON-NLS-1$
 
 	private static ExceptionCollector exceptions = new ExceptionCollector(CVSUIMessages.CVSDecorator_exceptionMessage, CVSUIPlugin.ID, IStatus.ERROR, CVSUIPlugin.getPlugin().getLog()); //;
 	
 	private static String DECORATOR_FORMAT = "yyyy/MM/dd HH:mm:ss"; //$NON-NLS-1$
 	private static SimpleDateFormat decorateFormatter = new SimpleDateFormat(DECORATOR_FORMAT, Locale.getDefault());
 	
 	private static String[] fonts = new String[]  {
 			CVSDecoratorConfiguration.IGNORED_FONT,
 			CVSDecoratorConfiguration.OUTGOING_CHANGE_FONT};
 	
 	private static String[] colors = new String[] {
 			 CVSDecoratorConfiguration.OUTGOING_CHANGE_BACKGROUND_COLOR,
 			 CVSDecoratorConfiguration.OUTGOING_CHANGE_FOREGROUND_COLOR,
 			 CVSDecoratorConfiguration.IGNORED_BACKGROUND_COLOR,
 			 CVSDecoratorConfiguration.IGNORED_FOREGROUND_COLOR};
 	
 	private static final SynchronizationStateTester DEFAULT_TESTER = new SynchronizationStateTester();
 
 	public CVSLightweightDecorator() {
 		ResourceStateChangeListeners.getListener().addResourceStateChangeListener(this);
 		TeamUI.addPropertyChangeListener(this);
 		CVSUIPlugin.addPropertyChangeListener(this);
 		
 		// This is an optimization to ensure that while decorating our fonts and colors are
 		// pre-created and decoration can occur without having to syncExec.
 		ensureFontAndColorsCreated(fonts, colors);
 		
 		PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().addPropertyChangeListener(this);
 		CVSProviderPlugin.broadcastDecoratorEnablementChanged(true /* enabled */);
 	}
 	
 	/**
 	 * This method will ensure that the fonts and colors used by the decorator
 	 * are cached in the registries. This avoids having to syncExec when
 	 * decorating since we ensure that the fonts and colors are pre-created.
 	 * 
 	 * @param fonts fonts ids to cache
 	 * @param colors color ids to cache
 	 */
 	private void ensureFontAndColorsCreated(final String[] fonts, final String[] colors) {
 		CVSUIPlugin.getStandardDisplay().syncExec(new Runnable() {
 			public void run() {
 				ITheme theme  = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
 				for (int i = 0; i < colors.length; i++) {
 					theme.getColorRegistry().get(colors[i]);
 					
 				}
 				for (int i = 0; i < fonts.length; i++) {
 					theme.getFontRegistry().get(fonts[i]);
 				}
 			}
 		});
 	}
 
 	public static boolean isDirty(final ICVSResource resource) throws CVSException {
 		return getSubscriber().isDirty(resource, null);
 	}
 
 	public static boolean isDirty(IResource resource) {
 		try {
 			return getSubscriber().isDirty(resource, null);
 		} catch (CVSException e) {
 			handleException(resource, e);
 			return true;
 		}
 	}
 	
 	/*
 	 * Answers null if a provider does not exist or the provider is not a CVS provider. These resources
 	 * will be ignored by the decorator.
 	 */
 	private static CVSTeamProvider getCVSProviderFor(IResource resource) {
 		if (resource == null) return null;
 		RepositoryProvider p =
 			RepositoryProvider.getProvider(
 				resource.getProject(),
 				CVSProviderPlugin.getTypeId());
 		if (p == null) {
 			return null;
 		}
 		return (CVSTeamProvider) p;
 	}
 	
 	/**
 	 * This method should only be called by the decorator thread.
 	 * 
 	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
 	 */
 	public void decorate(Object element, IDecoration decoration) {
 		
 		// Don't decorate the workspace root
 		IResource resource = getResource(element);
 		if (resource != null && resource.getType() == IResource.ROOT)
 			return;
 		
 		// Get the mapping for the object and ensure it overlaps with CVS projects
 		ResourceMapping mapping = Utils.getResourceMapping(element);
 		if (mapping == null)
 			return;
 		if (!isMappedToCVS(mapping))
 			return;	
 		
 		// Get the sync state tester from the context
 		IDecorationContext context = decoration.getDecorationContext();
 		SynchronizationStateTester tester = DEFAULT_TESTER;
 		Object property = context.getProperty(SynchronizationStateTester.PROP_TESTER);
 		if (property instanceof SynchronizationStateTester) {
 			tester = (SynchronizationStateTester) property;
 		}
 		
 		// Calculate and apply the decoration
 		try {
 			CVSDecoration cvsDecoration = decorate(element, tester);
 			cvsDecoration.apply(decoration);
 		} catch(CoreException e) {
			handleException(resource, e);
 		} catch (IllegalStateException e) {
 		    // This is thrown by Core if the workspace is in an illegal state
 		    // If we are not active, ignore it. Otherwise, propogate it.
 		    // (see bug 78303)
 		    if (Platform.getBundle(CVSUIPlugin.ID).getState() == Bundle.ACTIVE) {
 		        throw e;
 		    }
 		}
 	}
 
 	private IResource getResource(Object element) {
 		if (element instanceof ResourceMapping) {
 			element = ((ResourceMapping) element).getModelObject();
 		}
 		return Utils.getResource(element);
 	}
 
 	/*
 	 * Return whether any of the projects of the mapping are mapped to CVS
 	 */
     private boolean isMappedToCVS(ResourceMapping mapping) {
         IProject[] projects = mapping.getProjects();
         for (int i = 0; i < projects.length; i++) {
             IProject project = projects[i];
             if (getCVSProviderFor(project) != null) {
                 return true;
             }
         }
         return false;
     }
     
     private CVSDecoration decorate(Object element, SynchronizationStateTester tester) throws CoreException {
     	IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
         CVSDecoration result = new CVSDecoration();
         
         // First, decorate the synchronization state
         if (tester.isStateDecorationEnabled()) {
 			if (tester.isSupervised(element)) {
 				result.setHasRemote(true);
 				int state = tester.getState(element, 
 						store.getBoolean(ICVSUIConstants.PREF_CALCULATE_DIRTY) 
 							? IDiffNode.ADD | IDiffNode.REMOVE | IDiffNode.CHANGE | IThreeWayDiff.OUTGOING 
 							: 0, 
 						new NullProgressMonitor());
 	        	if ((state & IThreeWayDiff.OUTGOING) != 0) {
 	        		result.setDirty(true);
 	        	}
 	        } else {
 	        	result.setIgnored(true);
 	        }
         }
         
         // If the element adapts to a single resource, add additional decorations
 		IResource resource = getResource(element);
 		if (resource == null) {
 			result.setResourceType(CVSDecoration.MODEL);
 		} else {
 			decorate(resource, result);
 		}
         return result;
     }
     
 	private static void decorate(IResource resource, CVSDecoration cvsDecoration) throws CVSException {
 		IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
 		ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
 		cvsDecoration.setResourceType(resource.getType());
 		
 		if (cvsResource.isIgnored()) {
 			cvsDecoration.setIgnored(true);
 		}
 		if (!cvsDecoration.isIgnored()) {
 			// Dirty: Only decorate dirty state if we're not set to decorate models
 			boolean decorateModel = store.getBoolean(ICVSUIConstants.PREF_CALCULATE_DIRTY);
 			if (!decorateModel) {
 				// Dirty
 				try {
 					IDiffNode node = getSubscriber().getDiff(resource);
 					if (node != null) {
 						if (node instanceof IThreeWayDiff) {
 							IThreeWayDiff twd = (IThreeWayDiff) node;
 							cvsDecoration.setDirty(twd.getDirection() == IThreeWayDiff.OUTGOING 
 								|| twd.getDirection() == IThreeWayDiff.CONFLICTING);
 						}
 					}
 				} catch (CoreException e) {
 					handleException(resource, e);
 				}
 				// Has a remote
 				//cvsDecoration.setHasRemote(CVSWorkspaceRoot.hasRemote(resource));
 			}
 			// Tag
 			CVSTag tag = getTagToShow(resource);
 			if (tag != null) {
 				String name = tag.getName();
 				if (tag.getType() == CVSTag.DATE) {
 					Date date = tag.asDate();
 					if (date != null) {
 						name = decorateFormatter.format(date);
 					}
 				}
 				cvsDecoration.setTag(name);
 			}
 			// Is a new resource
 			if (store.getBoolean(ICVSUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION)) {
 				if (cvsResource.exists()) {
 					if (cvsResource.isFolder()) {
 						if (!((ICVSFolder) cvsResource).isCVSFolder()) {
 							cvsDecoration.setNewResource(true);
 						}
 					} else if (!cvsResource.isManaged()) {
 						cvsDecoration.setNewResource(true);
 					}
 				}
 			}
 			// Extract type specific properties
 			if (resource.getType() == IResource.FILE) {
 				extractFileProperties((IFile) resource, cvsDecoration);
 			} else {
 				extractContainerProperties((IContainer) resource, cvsDecoration);
 			}
 		}
 	}
 
 	public static CVSDecoration decorate(IResource resource, boolean includeDirtyCheck) throws CVSException {
 		IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
 		ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
 		CVSDecoration cvsDecoration = new CVSDecoration();
 		cvsDecoration.setResourceType(resource.getType());
 		
 		if (cvsResource.isIgnored()) {
 			cvsDecoration.setIgnored(true);
 		}
 		if (!cvsDecoration.isIgnored()) {
 			// Dirty
             if (includeDirtyCheck) {
     			boolean computeDeepDirtyCheck = store.getBoolean(ICVSUIConstants.PREF_CALCULATE_DIRTY);
     			int type = resource.getType();
     			if (type == IResource.FILE || computeDeepDirtyCheck) {
     				cvsDecoration.setDirty(CVSLightweightDecorator.isDirty(resource));
     			}
             }
 		}
 		decorate(resource, cvsDecoration);
 		return cvsDecoration;
 	}
 
 	private static void extractContainerProperties(IContainer resource, CVSDecoration cvsDecoration) throws CVSException {
 		ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(resource);
 		FolderSyncInfo folderInfo = folder.getFolderSyncInfo();
 		if (folderInfo != null) {
 			cvsDecoration.setLocation(KnownRepositories.getInstance().getRepository(folderInfo.getRoot()));
 			cvsDecoration.setRepository(folderInfo.getRepository());
 			cvsDecoration.setVirtualFolder(folderInfo.isVirtualDirectory());
 		}
 	}
 
 	private static void extractFileProperties(IFile resource, CVSDecoration cvsDecoration) throws CVSException {
 		ICVSFile file = CVSWorkspaceRoot.getCVSFileFor(resource);
 		ResourceSyncInfo fileInfo = file.getSyncInfo();
 		KSubstOption option = KSubstOption.fromFile(resource);
 		if (fileInfo != null) {
 			cvsDecoration.setAdded(fileInfo.isAdded());
 			cvsDecoration.setRevision(fileInfo.getRevision());
 			cvsDecoration.setReadOnly(file.isReadOnly());
 			cvsDecoration.setNeedsMerge(fileInfo.isNeedsMerge(file.getTimeStamp()));
 			option = fileInfo.getKeywordMode();
 		}
 		cvsDecoration.setKeywordSubstitution(option.getShortDisplayText());
 		cvsDecoration.setWatchEditEnabled(getCVSProviderFor(resource).isWatchEditEnabled());	
 	}
 
 	/**
 	 * Only show the tag if the resources tag is different than the parents. Or else, tag
 	 * names will clutter the text decorations.
 	 */
 	protected static CVSTag getTagToShow(IResource resource) throws CVSException {
 		ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
 		CVSTag tag = null;
 
 		// for unmanaged resources don't show a tag since they will be added in
 		// the context of their parents tag. For managed resources only show tags
 		// if different than parent.
 		boolean managed = false;
 
 		if(cvsResource.isFolder()) {
 			FolderSyncInfo folderInfo = ((ICVSFolder)cvsResource).getFolderSyncInfo();
 			if(folderInfo != null) {
 				tag = folderInfo.getTag();
 				managed = true;
 			}
 		} else {
 			ResourceSyncInfo info = ((ICVSFile)cvsResource).getSyncInfo();
 			if(info != null) {
 				tag = info.getTag();
 				managed = true;
 			}
 		}
 
 		ICVSFolder parent = cvsResource.getParent();
 		if(parent != null && managed) {
 			FolderSyncInfo parentInfo = parent.getFolderSyncInfo();
 			if(parentInfo != null) {
 				CVSTag parentTag = parentInfo.getTag();
 				parentTag = (parentTag == null ? CVSTag.DEFAULT : parentTag);
 				tag = (tag == null ? CVSTag.DEFAULT : tag);
 				// must compare tags by name because CVS doesn't do a good job of
 				// using  T and N prefixes for folders and files.
 				if( parentTag.getName().equals(tag.getName())) {
 					tag = null;
 				}
 			}
 		}
 		return tag;
 	}
 
 	/*
 	 * Add resource and its parents to the List
 	 */
 	 
 	private void addWithParents(IResource resource, Set resources) {
 		IResource current = resource;
 
 		while (current.getType() != IResource.ROOT) {
 			resources.add(current);
 			current = current.getParent();
 		}
 	}
 	
 	/*
 	* Perform a blanket refresh of all CVS decorations
 	*/
 	public static void refresh() {
 		CVSUIPlugin.getPlugin().getWorkbench().getDecoratorManager().update(CVSUIPlugin.DECORATOR_ID);
 	}
 
 	/*
 	 * Update the decorators for every resource in project
 	 */
 	 
 	public void refresh(IProject project) {
 		final List resources = new ArrayList();
 		try {
 			project.accept(new IResourceVisitor() {
 				public boolean visit(IResource resource) {
 					resources.add(resource);
 					return true;
 				}
 			});
 			postLabelEvent(new LabelProviderChangedEvent(this, resources.toArray()));
 		} catch (CoreException e) {
 			handleException(project, e);
 		}
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#resourceSyncInfoChanged(org.eclipse.core.resources.IResource[])
 	 */
 	public void resourceSyncInfoChanged(IResource[] changedResources) {
 		resourceStateChanged(changedResources);
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#externalSyncInfoChange(org.eclipse.core.resources.IResource[])
 	 */
 	public void externalSyncInfoChange(IResource[] changedResources) {
 		resourceStateChanged(changedResources);
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#resourceModificationStateChanged(org.eclipse.core.resources.IResource[])
 	 */
 	public void resourceModified(IResource[] changedResources) {
 		resourceStateChanged(changedResources);
 	}
 
 	/**
 	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#resourceStateChanged(org.eclipse.core.resources.IResource[])
 	 */
 	public void resourceStateChanged(IResource[] changedResources) {
 		// add depth first so that update thread processes parents first.
 		//System.out.println(">> State Change Event");
 		Set resourcesToUpdate = new HashSet();
 
 		IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
 		boolean showingDeepDirtyIndicators = store.getBoolean(ICVSUIConstants.PREF_CALCULATE_DIRTY);
 
 		for (int i = 0; i < changedResources.length; i++) {
 			IResource resource = changedResources[i];
 
 			if(showingDeepDirtyIndicators) {
 				addWithParents(resource, resourcesToUpdate);
 			} else {
 				resourcesToUpdate.add(resource);
 			}
 		}
 
 		postLabelEvent(new LabelProviderChangedEvent(this, resourcesToUpdate.toArray()));
 	}
 	
 	/**
 	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#projectConfigured(org.eclipse.core.resources.IProject)
 	 */
 	public void projectConfigured(IProject project) {
 		refresh(project);
 	}
 	/**
 	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#projectDeconfigured(org.eclipse.core.resources.IProject)
 	 */
 	public void projectDeconfigured(IProject project) {
 		refresh(project);
 	}
 
 	/**
 	 * Post the label event to the UI thread
 	 *
 	 * @param events  the events to post
 	 */
 	private void postLabelEvent(final LabelProviderChangedEvent event) {
 		Display.getDefault().asyncExec(new Runnable() {
 			public void run() {
 				fireLabelProviderChanged(event);
 			}
 		});
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
 	 */
 	public void dispose() {
 		super.dispose();
 		PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().removePropertyChangeListener(this);
 		CVSProviderPlugin.broadcastDecoratorEnablementChanged(false /* disabled */);
 		TeamUI.removePropertyChangeListener(this);
 		CVSUIPlugin.removePropertyChangeListener(this);
 	}
 	
 	/**
 	 * Handle exceptions that occur in the decorator.
 	 * Exceptions are only logged for resources that
 	 * are accessible (i.e. exist in an open project).
 	 */
 	private static void handleException(IResource resource, CoreException e) {
 		if (resource == null || resource.isAccessible())
 			exceptions.handleException(e);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
 	 */
 	public void propertyChange(PropertyChangeEvent event) {
 		if (isEventOfInterest(event)) {
 			ensureFontAndColorsCreated(fonts, colors);
 		    refresh();
 		}	
 	}
 
     private boolean isEventOfInterest(PropertyChangeEvent event) {
         String prop = event.getProperty();
         return prop.equals(TeamUI.GLOBAL_IGNORES_CHANGED) 
         	|| prop.equals(TeamUI.GLOBAL_FILE_TYPES_CHANGED) 
         	|| prop.equals(CVSUIPlugin.P_DECORATORS_CHANGED)
 			|| prop.equals(CVSDecoratorConfiguration.OUTGOING_CHANGE_BACKGROUND_COLOR)
 			|| prop.equals(CVSDecoratorConfiguration.OUTGOING_CHANGE_FOREGROUND_COLOR)
 			|| prop.equals(CVSDecoratorConfiguration.OUTGOING_CHANGE_FONT)
 			|| prop.equals(CVSDecoratorConfiguration.IGNORED_FOREGROUND_COLOR)
 			|| prop.equals(CVSDecoratorConfiguration.IGNORED_BACKGROUND_COLOR)
 			|| prop.equals(CVSDecoratorConfiguration.IGNORED_FONT);
     }
 	
 	private static CVSWorkspaceSubscriber getSubscriber() {
 		return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber();
 	}
 }
