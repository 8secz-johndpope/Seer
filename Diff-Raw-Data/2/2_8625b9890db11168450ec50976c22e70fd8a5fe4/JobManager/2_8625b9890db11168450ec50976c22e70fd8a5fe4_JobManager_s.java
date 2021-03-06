 /*******************************************************************************
  * Copyright (c) 2005 The Regents of the University of California. 
  * This material was produced under U.S. Government contract W-7405-ENG-36 
  * for Los Alamos National Laboratory, which is operated by the University 
  * of California for the U.S. Department of Energy. The U.S. Government has 
  * rights to use, reproduce, and distribute this software. NEITHER THE 
  * GOVERNMENT NOR THE UNIVERSITY MAKES ANY WARRANTY, EXPRESS OR IMPLIED, OR 
  * ASSUMES ANY LIABILITY FOR THE USE OF THIS SOFTWARE. If software is modified 
  * to produce derivative works, such modified software should be clearly marked, 
  * so as not to confuse it with the version available from LANL.
  * 
  * Additionally, this program and the accompanying materials 
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  * 
  * LA-CC 04-115
  *******************************************************************************/
 package org.eclipse.ptp.ui.managers;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.debug.core.DebugPlugin;
 import org.eclipse.debug.core.ILaunch;
 import org.eclipse.debug.core.ILaunchManager;
 import org.eclipse.ptp.core.elements.IPElement;
 import org.eclipse.ptp.core.elements.IPJob;
 import org.eclipse.ptp.core.elements.IPProcess;
 import org.eclipse.ptp.core.elements.IPQueue;
 import org.eclipse.ptp.core.elements.IPUniverse;
 import org.eclipse.ptp.core.elements.IResourceManager;
 import org.eclipse.ptp.core.elements.attributes.ElementAttributes;
 import org.eclipse.ptp.core.elements.attributes.JobAttributes;
 import org.eclipse.ptp.internal.ui.ParallelImages;
 import org.eclipse.ptp.ui.IJobManager;
 import org.eclipse.ptp.ui.IRuntimeModelPresentation;
 import org.eclipse.ptp.ui.PTPUIPlugin;
 import org.eclipse.ptp.ui.listeners.IJobChangedListener;
 import org.eclipse.ptp.ui.messages.Messages;
 import org.eclipse.ptp.ui.model.Element;
 import org.eclipse.ptp.ui.model.ElementHandler;
 import org.eclipse.ptp.ui.model.IElement;
 import org.eclipse.ptp.ui.model.IElementHandler;
 import org.eclipse.ptp.ui.model.IElementSet;
 import org.eclipse.swt.graphics.Image;
 
 /**
  * @author Clement chu
  * 
  */
 
 public class JobManager extends AbstractElementManager implements IJobManager {
 	protected Map<String, IPJob> jobList = new HashMap<String, IPJob>();
 	protected IPJob cur_job = null;
 	protected IPQueue cur_queue = null;
 	protected final String DEFAULT_TITLE = Messages.JobManager_0;
 
 	/** 
 	 * Add a new job to jobList. Check for any new processes and add these to
 	 * the element handler for the job.
 	 * 
 	 * @param job
 	 */
 	public void addJob(IPJob job) {
 		if (job != null) {
 			IElementSet set = createElementHandler(job).getSetRoot();
 			List<IElement> elements = new ArrayList<IElement>();
 			for (IPProcess proc : job.getProcesses()) {
 				if (proc == null || set.contains(proc.getID()))
 					continue;
 				elements.add(createProcessElement(set, proc.getID(), proc.getProcessIndex(), proc));
 			}
 			set.addElements(elements.toArray(new IElement[0]));
 		}
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IJobManager#addProcess(org.eclipse.ptp.core.elements.IPProcess)
 	 */
 	public void addProcess(IPProcess proc) {
 		IPJob job = proc.getJob();
 		IElementSet set = createElementHandler(job).getSetRoot();
 		if (!set.contains(proc.getID())) {
 			set.addElements(new IElement[] { createProcessElement(set, proc.getID(), proc.getProcessIndex(), proc) });
 		}
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IElementManager#clear()
 	 */
 	@Override
 	public void clear() {
 		jobList.clear();
 		super.clear();
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IJobManager#createElementHandler(org.eclipse.ptp.core.elements.IPJob)
 	 */
 	public IElementHandler createElementHandler(IPJob job) {
 		IElementHandler handler = getElementHandler(job.getID());
 		if (handler == null) {
 			handler = new ElementHandler();
 			jobList.put(job.getID(), job);
 			setElementHandler(job.getID(), handler);
 		}
 		return handler;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IJobManager#findJobById(java.lang.String)
 	 */
 	public IPJob findJobById(String job_id) {
 		return jobList.get(job_id);
 	}
 	
 	/** 
 	 * Find process
 	 * @param job job
 	 * @param id process ID
 	 * @return
 	 */
 	public IPProcess findProcess(IPJob job, String task_id) {
 		if (job == null)
 			return null;
 		return job.getProcessByIndex(task_id);
 		//return job.findProcess(id);
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IJobManager#findProcess(java.lang.String)
 	 */
 	public IPProcess findProcess(String proc_id) {
 		IPJob job = getJob();
 		if (job != null) {
 			return job.getProcessById(proc_id);
 		}
 		return null;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IElementManager#getFullyQualifiedName(java.lang.String)
 	 */
 	public String getFullyQualifiedName(String id) {
 		if (id.equals(EMPTY_ID)) {
 			return DEFAULT_TITLE;
 		}
 		IPJob job = getJob();
 		if (job != null) {
 			IPQueue queue = job.getQueue();
 			if (queue != null) {
 				IResourceManager rm = queue.getResourceManager();
 				if (rm != null) {
 					return rm.getName() + ": " + queue.getName() + ":" + job.getName(); //$NON-NLS-1$ //$NON-NLS-2$
 				}
 			}
 		}
 		return ""; //$NON-NLS-1$
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IJobManager#getJob()
 	 */
 	public IPJob getJob() {
 		return cur_job;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IJobManager#getJobs()
 	 */
 	public IPJob[] getJobs() {
 		return jobList.values().toArray(new IPJob[0]);
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IElementManager#getName(java.lang.String)
 	 */
 	public String getName(String id) {
 		IPJob job = findJobById(id);
 		if (job == null)
 			return ""; //$NON-NLS-1$
 		return job.getName();
 	}
 	
 	/** 
 	 * Get process status text
 	 * @param proc process
 	 * @return status
 	 */
 	public String getProcessStatusText(IPProcess proc) {
 		if (proc != null) {
 			return proc.getState().toString();
 		}
 		return Messages.JobManager_1;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IJobManager#getQueue()
 	 */
 	public IPQueue getQueue() {
 		return cur_queue;
 	}
 	
 	/**
 	 * @return
 	 */
 	public IPQueue[] getQueues() {
 		if (cur_queue != null) {
 			return cur_queue.getResourceManager().getQueues();
 		}
 		return new IPQueue[] {};
 	}
 	
 	/** 
 	 * Return set id
 	 * @param jid
 	 * @return
 	 */
 	public String[] getSets(String jid) {
 		IElementHandler eHandler = getElementHandler(jid);
 		if (eHandler == null)
 			return new String[0];
 		
 		IElement[] elements = eHandler.getElements();
 		String[] sets = new String[elements.length];
 		for (int i=1; i<sets.length+1; i++) {
 			String tmp = elements[i-1].getID();
 			if (tmp.equals(IElementHandler.SET_ROOT_ID)) {
 				continue;
 			}
 			if (i == sets.length) {
 				sets[i-1] = tmp;				
 			}
 			else {
 				sets[i] = tmp;				
 			}
 		}
 		if (sets.length > 0) {
 			sets[0] = IElementHandler.SET_ROOT_ID;
 		}
 		return sets;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IElementManager#getImage(org.eclipse.ptp.ui.model.IElement)
 	 */
 	public Image getImage(IElement element) {
 		IPJob job = getJob();
 		if (job != null) {
 			IResourceManager rm = job.getQueue().getResourceManager();
 			final IRuntimeModelPresentation presentation = PTPUIPlugin.getDefault().getRuntimeModelPresentation(rm.getResourceManagerId());
 			if (presentation != null) {
 				final Image image = presentation.getImage(element);
 				if (image != null) {
 					return image;
 				}
 			}
 			IPProcess proc = job.getProcessById(element.getID());
 			if (proc != null) {
				return ParallelImages.procImages[0][element.isSelected() ? 1 : 0];
 			}
 		}
 		return null;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IJobManager#hasStoppedJob()
 	 */
 	public boolean hasStoppedJob() {
 		for (IPJob job : getJobs()) {
 			if (job.getState() == JobAttributes.State.COMPLETED) {
 				return true;
 			}
 		}
 		return false;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IElementManager#initial()
 	 */
 	public IPElement initial(IPUniverse universe) {
 		for (IResourceManager rm : universe.getResourceManagers()) {
 			for (IPQueue queue : rm.getQueues()) {
 				for (IPJob job : queue.getJobs()) {
 					addJob(job);
 				}
 			}
 		}
 
 		setCurrentSetId(IElementHandler.SET_ROOT_ID);
 		return null;
 	}
 	
 	/** 
 	 * Is current set contain process
 	 * @param jid job ID
 	 * @param processID process ID
 	 * @return true if job contains process
 	 */
 	public boolean isCurrentSetContainProcess(String jid, String processID) {
 		IPJob job = getJob();
 		if (job != null) {
 			if (!job.getID().equals(jid))
 				return false;
 			IElementHandler elementHandler = getElementHandler(jid);
 			if (elementHandler == null)
 				return false;
 			IElementSet set = (IElementSet)elementHandler.getElementByID(getCurrentSetId());
 			if (set == null)
 				return false;
 			return set.contains(processID);
 		}
 		return false;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IElementManager#isJobStop(java.lang.String)
 	 */
 	public boolean isJobStop(String job_id) {
 		if (isNoJob(job_id))
 			return true;
 		IPJob job = findJobById(job_id);
 		return (job == null || job.getState() == JobAttributes.State.COMPLETED);
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IElementManager#isNoJob(java.lang.String)
 	 */
 	public boolean isNoJob(String jid) {
 		return (jid == null || jid.length() == 0);
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IJobManager#removeAllStoppedJobs()
 	 */
 	public void removeAllStoppedJobs() {
 		Map<String, IPQueue> queues = new HashMap<String, IPQueue>();
 		for (IPJob job : getJobs()) {
 			IPQueue queue = job.getQueue();
 			if (!queues.containsKey(queue.getID())) {
 				queue.getResourceManager().removeTerminatedJobs(queue);
 				queues.put(queue.getID(), queue);
 			}
 		}
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IJobManager#removeJob(org.eclipse.ptp.core.elements.IPJob)
 	 */
 	public void removeJob(IPJob job) {
 		//remove launch from debug view
 		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
 		String jid = job.getID();
 		for (ILaunch launch : launchManager.getLaunches()) {
 			String launchedJobID = launch.getAttribute(ElementAttributes.getIdAttributeDefinition().getId());
 			if (launchedJobID != null && launchedJobID.equals(jid)) {
 				launchManager.removeLaunch(launch);
 			}
 		}
 		jobList.remove(jid);
 		removeElementHandler(job);
 		fireJobChangedEvent(IJobChangedListener.REMOVED, null, jid);
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IJobManager#removeProcess(org.eclipse.ptp.core.elements.IPProcess)
 	 */
 	public void removeProcess(IPProcess proc) {
 		IElementHandler elementHandler = getElementHandler(proc.getJob().getID());
 		if (elementHandler != null) {
 			IElementSet set = elementHandler.getSetRoot();
 			IElement element = set.getElementByID(proc.getID());
 			if (element != null) {
 				set.removeElement(proc.getID());
 			}
 		}
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IElementManager#setCurrentSetId(java.lang.String)
 	 */
 	public void setCurrentSetId(String set_id) {
 		cur_set_id = set_id;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IJobManager#setJob(org.eclipse.ptp.core.elements.IPJob)
 	 */
 	public void setJob(IPJob job) {
 		String old_id = (cur_job==null)?EMPTY_ID:cur_job.getID();
 		String new_id = (job==null)?EMPTY_ID:job.getID();
 		if (job != null) {
 			addJob(job);
 		}
 		cur_job = job;
 		fireJobChangedEvent(IJobChangedListener.CHANGED, new_id, old_id);
 	}
 	
 	/**
 	 * @param queue
 	 */
 	public void setQueue(IPQueue queue) {
 		if (queue != cur_queue) {
 			cur_queue = queue;
 			setJob(null);
 		}
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IElementManager#shutdown()
 	 */
 	public void shutdown() {
 		clear();
 		modelPresentation = null;
 		super.shutdown();
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IElementManager#size()
 	 */
 	public int size() {
 		return jobList.size();
 	}
 	
 	/*******************************************************************************************************************************************************************************************************************************************************************************************************
 	 * terminate action
 	 ******************************************************************************************************************************************************************************************************************************************************************************************************/
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.ui.IJobManager#terminateJob()
 	 */
 	public void terminateJob() throws CoreException {
 		IPJob job = getJob();
 		if (job != null) {
 			job.getQueue().getResourceManager().terminateJob(job);
 		}
 	}
 	
 	/**
 	 * @param job
 	 */
 	private void removeElementHandler(IPJob job) {
 		IElementHandler elementHandler = getElementHandler(job.getID());
 		if (elementHandler != null) {
 			elementHandler.removeAllRegistered();
 			elementHandler.clean();
 		}
 		removeElementHandler(job.getID());		
 	}
 	
 	/**
 	 * @param set
 	 * @param key
 	 * @param taskID
 	 * @return
 	 */
 	protected IElement createProcessElement(IElementSet set, String key, String taskID, IPProcess process) {
 		return new Element(set, key, taskID, process) {
 			public int compareTo(IElement e) {
 				return new Integer(getName()).compareTo(new Integer(e.getName()));
 			}
 		};
 	}
 }
