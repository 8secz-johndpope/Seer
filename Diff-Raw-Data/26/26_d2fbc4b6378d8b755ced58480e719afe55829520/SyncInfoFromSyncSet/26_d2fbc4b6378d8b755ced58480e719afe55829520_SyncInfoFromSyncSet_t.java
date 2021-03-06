 /*******************************************************************************
  * Copyright (c) 2000, 2003 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.team.tests.ccvs.ui.sync;
 
 import junit.framework.AssertionFailedError;
 
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.jobs.Job;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.team.core.TeamException;
 import org.eclipse.team.core.subscribers.SyncInfo;
 import org.eclipse.team.core.subscribers.TeamSubscriber;
 import org.eclipse.team.internal.ui.sync.sets.SubscriberInput;
 import org.eclipse.team.internal.ui.sync.sets.SyncSet;
 import org.eclipse.team.internal.ui.sync.views.SynchronizeView;
 import org.eclipse.team.tests.ccvs.core.subscriber.SyncInfoSource;
 
 /**
  * SyncInfoSource that obtains SyncInfo from the SynchronizeView's SyncSet.
  */
 public class SyncInfoFromSyncSet extends SyncInfoSource {
 
 	public SyncInfoFromSyncSet() {		
 	}
 	
 	public void waitForEventNotification(SubscriberInput input) {
 		// process UI events first, give the main thread a chance
 		// to handle any syncExecs or asyncExecs posted as a result
 		// of the event processing thread.
 		while (Display.getCurrent().readAndDispatch()) {};
 		
 		// wait for the event handler to process changes.
 		Job job = input.getEventHandler().getEventHandlerJob();
 		while(job.getState() != Job.NONE) {
 			while (Display.getCurrent().readAndDispatch()) {};
 			try {
 				Thread.sleep(50);
 			} catch (InterruptedException e) {
 			}
 		}						
 	}
 	
 	public SyncInfo getSyncInfo(TeamSubscriber subscriber, IResource resource) throws TeamException {
 		SubscriberInput input = getInput(subscriber);
 		SyncSet set = input.getWorkingSetSyncSet();
 		SyncInfo info = set.getSyncInfo(resource);
 		if (info == null) {
 			info = subscriber.getSyncInfo(resource, DEFAULT_MONITOR);
 			if ((info != null && info.getKind() != SyncInfo.IN_SYNC)) {
 				throw new AssertionFailedError();
 			}
 		}
 		return info;
 	}
 	
 	private SubscriberInput getInput(TeamSubscriber subscriber) throws AssertionFailedError {
 		// show the sync view
		SynchronizeView syncView = (SynchronizeView)SynchronizeView.showInActivePage(null, true);
 		SubscriberInput input = syncView.getInput();
 		if (subscriber != input.getSubscriber()) {
 			// ensure that the CVS subscriber is active
 			syncView.activateSubscriber(subscriber);
 			input = syncView.getInput();
 		}
 		if (subscriber != input.getSubscriber()) {
 			throw new AssertionFailedError();
 		}
 		waitForEventNotification(input);
 		return input;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.team.tests.ccvs.core.subscriber.SyncInfoSource#assertProjectRemoved(org.eclipse.team.core.subscribers.TeamSubscriber, org.eclipse.core.resources.IProject)
 	 */
 	protected void assertProjectRemoved(TeamSubscriber subscriber, IProject project) throws TeamException {		
 		super.assertProjectRemoved(subscriber, project);
 		SubscriberInput input = getInput(subscriber);
 		SyncSet set = input.getFilteredSyncSet();
 		if (set.getOutOfSyncDescendants(project).length != 0) {
 			throw new AssertionFailedError("The sync set still contains resources from the deleted project " + project.getName());	
 		}
 	}
 }
