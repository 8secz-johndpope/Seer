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
 package org.eclipse.ptp.launch;
 
 import java.io.FileNotFoundException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.locks.Condition;
 import java.util.concurrent.locks.ReentrantLock;
 
 import org.eclipse.core.filesystem.EFS;
 import org.eclipse.core.filesystem.IFileStore;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IWorkspaceRoot;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.core.runtime.SubMonitor;
 import org.eclipse.core.runtime.jobs.Job;
 import org.eclipse.debug.core.DebugPlugin;
 import org.eclipse.debug.core.ILaunch;
 import org.eclipse.debug.core.ILaunchConfiguration;
 import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
 import org.eclipse.debug.core.ILaunchManager;
 import org.eclipse.debug.core.model.IPersistableSourceLocator;
 import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
 import org.eclipse.ptp.core.IPTPLaunchConfigurationConstants;
 import org.eclipse.ptp.core.PTPCorePlugin;
 import org.eclipse.ptp.core.elements.IPQueue;
 import org.eclipse.ptp.core.elements.IPResourceManager;
 import org.eclipse.ptp.core.events.IJobChangedEvent;
 import org.eclipse.ptp.core.listeners.IJobListener;
 import org.eclipse.ptp.debug.core.IPDebugConfiguration;
 import org.eclipse.ptp.debug.core.IPDebugger;
 import org.eclipse.ptp.debug.core.PTPDebugCorePlugin;
 import org.eclipse.ptp.debug.core.launch.IPLaunch;
 import org.eclipse.ptp.debug.core.launch.PLaunch;
 import org.eclipse.ptp.debug.ui.PTPDebugUIPlugin;
 import org.eclipse.ptp.launch.messages.Messages;
 import org.eclipse.ptp.launch.rulesengine.ILaunchProcessCallback;
 import org.eclipse.ptp.launch.rulesengine.IRuleAction;
 import org.eclipse.ptp.launch.rulesengine.ISynchronizationRule;
 import org.eclipse.ptp.launch.rulesengine.RuleActionFactory;
 import org.eclipse.ptp.launch.rulesengine.RuleFactory;
 import org.eclipse.ptp.remote.core.IRemoteConnection;
 import org.eclipse.ptp.remote.core.IRemoteConnectionManager;
 import org.eclipse.ptp.remote.core.IRemoteFileManager;
 import org.eclipse.ptp.remote.core.IRemoteServices;
 import org.eclipse.ptp.remote.core.PTPRemoteCorePlugin;
 import org.eclipse.ptp.rmsystem.IJobStatus;
 import org.eclipse.ptp.rmsystem.IResourceManager;
 import org.eclipse.ptp.rmsystem.IResourceManagerComponentConfiguration;
 import org.eclipse.ptp.utils.core.ArgumentParser;
 
 /**
  *
  */
 public abstract class AbstractParallelLaunchConfigurationDelegate extends LaunchConfigurationDelegate implements
 		ILaunchProcessCallback {
 
 	private final class JobListener implements IJobListener {
 		/*
 		 * (non-Javadoc)
 		 * 
 		 * @see
 		 * org.eclipse.ptp.core.listeners.IJobListener#handleEvent(org.eclipse
 		 * .ptp.core.events.IJobChangeEvent)
 		 */
 		public void handleEvent(IJobChangedEvent e) {
 			JobSubmission jobSub;
 			synchronized (jobSubmissions) {
 				jobSub = jobSubmissions.get(e.getJobId());
 			}
 			if (jobSub != null) {
 				jobSub.statusChanged();
 			}
 		}
 	}
 
 	/**
 	 * Wait for job to begin running, then perform post launch operations. The
 	 * job is guaranteed not to be in the UNDERTERMINED state.
 	 * 
 	 * <pre>
 	 * Job state transition is:
 	 * 
 	 *  SUBMITTED ----> RUNNING ----> COMPLETED
 	 *             ^              |
 	 *             |- SUSPENDED <-|
 	 * </pre>
 	 * 
 	 * We must call completion method when job state is RUNNING, however it is
 	 * possible that the job may get to COMPLETED or SUSPENDED before we are
 	 * started. If either of these states is reached, assume that RUNNING has
 	 * also been reached.
 	 */
 	private class JobSubmission extends Job {
 		private final IPLaunch fLaunch;
 		private final IPDebugger fDebugger;
 		private final ReentrantLock fSubLock = new ReentrantLock();
 		private final Condition fSubCondition = fSubLock.newCondition();
 
 		public JobSubmission(IPLaunch launch, IPDebugger debugger) {
 			super(launch.getJobId());
 			fLaunch = launch;
 			fDebugger = debugger;
 			setSystem(true);
 		}
 
 		public void statusChanged() {
 			fSubLock.lock();
 			try {
 				fSubCondition.signalAll();
 			} finally {
 				fSubLock.unlock();
 			}
 		}
 
 		/*
 		 * (non-Javadoc)
 		 * 
 		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.
 		 * IProgressMonitor)
 		 */
 		@Override
 		protected IStatus run(IProgressMonitor monitor) {
 			SubMonitor subMon = SubMonitor.convert(monitor, 100);
 			try {
 				IResourceManager rm = fLaunch.getResourceManager();
 				String jobId = fLaunch.getJobId();
 				fSubLock.lock();
 				try {
 					while (rm.getJobStatus(jobId, subMon.newChild(50)).getState().equals(IJobStatus.SUBMITTED)
 							&& !subMon.isCanceled()) {
 						try {
 							fSubCondition.await(100, TimeUnit.MILLISECONDS);
 						} catch (InterruptedException e) {
 							// Expect to be interrupted if monitor is canceled
 						}
 					}
 				} finally {
 					fSubLock.unlock();
 				}
 
 				if (!subMon.isCanceled()) {
 					doCompleteJobLaunch(fLaunch, fDebugger);
 
 					fSubLock.lock();
 					try {
 						while (!rm.getJobStatus(jobId, subMon.newChild(50)).getState().equals(IJobStatus.COMPLETED)
 								&& !subMon.isCanceled()) {
 							try {
 								fSubCondition.await(100, TimeUnit.MILLISECONDS);
 							} catch (InterruptedException e) {
 								// Expect to be interrupted if monitor is
 								// canceled
 							}
 						}
 					} finally {
 						fSubLock.unlock();
 					}
 
 					if (!subMon.isCanceled()) {
 						/*
 						 * When the job terminates, do any post launch data
 						 * synchronization.
 						 */
 						// If needed, copy data back.
 						try {
 							// Get the list of paths to be copied back.
 							doPostLaunchSynchronization(fLaunch.getLaunchConfiguration());
 						} catch (CoreException e) {
 							PTPLaunchPlugin.log(e);
 						}
 
 						/*
 						 * Clean up any launch activities.
 						 */
 						doCleanupLaunch(fLaunch);
 
 						/*
 						 * Remove job submission
 						 */
 						synchronized (jobSubmissions) {
 							jobSubmissions.remove(jobId);
 							if (jobSubmissions.size() == 0) {
 								rm.removeJobListener(fJobListener);
 							}
 						}
 					}
 				}
 				return Status.OK_STATUS;
 			} finally {
 				if (monitor != null) {
 					monitor.done();
 				}
 			}
 		}
 	}
 
 	/**
 	 * Get the program arguments specified in the Arguments tab
 	 * 
 	 * @param configuration
 	 * @return
 	 * @throws CoreException
 	 */
 	protected static String getArguments(ILaunchConfiguration configuration) throws CoreException {
 		return configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_ARGUMENTS, (String) null);
 	}
 
 	/**
 	 * Get the debugger executable path
 	 * 
 	 * @param configuration
 	 * @return
 	 * @throws CoreException
 	 */
 	protected static String getDebuggerExePath(ILaunchConfiguration configuration) throws CoreException {
 		return configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_DEBUGGER_EXECUTABLE_PATH, (String) null);
 	}
 
 	/**
 	 * Get the ID of the debugger for this launch
 	 * 
 	 * @param configuration
 	 * @return
 	 * @throws CoreException
 	 */
 	protected static String getDebuggerID(ILaunchConfiguration configuration) throws CoreException {
 		return configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_DEBUGGER_ID, (String) null);
 	}
 
 	/**
 	 * Get the debugger "stop in main" flag
 	 * 
 	 * @param configuration
 	 * @return "stop in main" flag
 	 * @throws CoreException
 	 */
 	protected static boolean getDebuggerStopInMainFlag(ILaunchConfiguration configuration) throws CoreException {
 		return configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_STOP_IN_MAIN, false);
 	}
 
 	/**
 	 * Get the working directory for this debug session
 	 * 
 	 * @param configuration
 	 * @return
 	 * @throws CoreException
 	 */
 	protected static String getDebuggerWorkDirectory(ILaunchConfiguration configuration) throws CoreException {
 		return configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_DEBUGGER_WORKING_DIR, (String) null);
 	}
 
 	/**
 	 * @param configuration
 	 * @return
 	 * @throws CoreException
 	 */
 	protected static String[] getEnvironmentToAppend(ILaunchConfiguration configuration) throws CoreException {
 		Map<?, ?> defaultEnv = null;
 		Map<?, ?> configEnv = configuration.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, defaultEnv);
 		if (configEnv == null) {
 			return null;
 		}
 		if (!configuration.getAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true)) {
 			throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 					Messages.AbstractParallelLaunchConfigurationDelegate_Parallel_launcher_does_not_support));
 		}
 
 		List<String> strings = new ArrayList<String>(configEnv.size());
 		Iterator<?> iter = configEnv.entrySet().iterator();
 		while (iter.hasNext()) {
 			Entry<?, ?> entry = (Entry<?, ?>) iter.next();
 			String key = (String) entry.getKey();
 			String value = (String) entry.getValue();
 			strings.add(key + "=" + value); //$NON-NLS-1$
 
 		}
 		return strings.toArray(new String[strings.size()]);
 	}
 
 	/**
 	 * Get the name of the project
 	 * 
 	 * @param configuration
 	 * @return
 	 * @throws CoreException
 	 */
 	protected static String getProjectName(ILaunchConfiguration configuration) throws CoreException {
 		return configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
 	}
 
 	/**
 	 * Get the name of the queue for the launch
 	 * 
 	 * @param configuration
 	 * @return
 	 * @throws CoreException
 	 */
 	protected static String getQueueName(ILaunchConfiguration configuration) throws CoreException {
 		return configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_QUEUE_NAME, (String) null);
 	}
 
 	/**
 	 * Get the resource manager to use for the launch
 	 * 
 	 * @param configuration
 	 * @return
 	 * @throws CoreException
 	 */
 	protected static String getResourceManagerUniqueName(ILaunchConfiguration configuration) throws CoreException {
 		return configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_RESOURCE_MANAGER_UNIQUENAME, (String) null);
 	}
 
 	/*
 	 * Model listeners
 	 */
 	private final IJobListener fJobListener = new JobListener();
 
 	/*
 	 * HashMap used to keep track of job submissions
 	 */
 	protected Map<String, JobSubmission> jobSubmissions = Collections.synchronizedMap(new HashMap<String, JobSubmission>());
 
 	/*
 	 * Data synchronization rules
 	 */
 	private List<ISynchronizationRule> extraSynchronizationRules;
 
 	/**
 	 * Constructor
 	 */
 	public AbstractParallelLaunchConfigurationDelegate() {
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.ptp.launch.rulesengine.ILaunchProcessCallback#
 	 * addSynchronizationRule(org.eclipse.ptp.launch.data.ISynchronizationRule)
 	 */
 	/**
 	 * @since 5.0
 	 */
 	public void addSynchronizationRule(ISynchronizationRule rule) {
 		extraSynchronizationRules.add(rule);
 	}
 
 	/**
 	 * Get if the executable shall be copied to remote target before launch.
 	 * 
 	 * @param configuration
 	 * @return
 	 * @throws CoreException
 	 */
 	public boolean getCopyExecutable(ILaunchConfiguration configuration) throws CoreException {
 		return configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_COPY_EXECUTABLE, false);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * org.eclipse.debug.core.model.LaunchConfigurationDelegate#getLaunch(org
 	 * .eclipse.debug.core.ILaunchConfiguration, java.lang.String)
 	 */
 	@Override
 	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
 		return new PLaunch(configuration, mode, null);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * org.eclipse.ptp.launch.rulesengine.ILaunchProcessCallback#getLocalFileManager
 	 * (org.eclipse.debug.core.ILaunchConfiguration)
 	 */
 	public IRemoteFileManager getLocalFileManager(ILaunchConfiguration configuration) throws CoreException {
 		IRemoteServices localServices = PTPRemoteCorePlugin.getDefault().getDefaultServices();
 		assert (localServices != null);
 		IRemoteConnectionManager lconnMgr = localServices.getConnectionManager();
 		assert (lconnMgr != null);
 		IRemoteConnection lconn = lconnMgr.getConnection(IRemoteConnectionManager.DEFAULT_CONNECTION_NAME);
 		assert (lconn != null);
 		IRemoteFileManager localFileManager = localServices.getFileManager(lconn);
 		assert (localFileManager != null);
 		return localFileManager;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.ptp.launch.rulesengine.ILaunchProcessCallback#
 	 * getRemoteFileManager(org.eclipse.debug.core.ILaunchConfiguration,
 	 * org.eclipse.core.runtime.IProgressMonitor)
 	 */
 	/**
 	 * @since 5.0
 	 */
 	public IRemoteFileManager getRemoteFileManager(ILaunchConfiguration configuration, IProgressMonitor monitor)
 			throws CoreException {
 		IResourceManager rm = getResourceManager(configuration);
 		if (rm != null) {
 			IResourceManagerComponentConfiguration conf = rm.getControlConfiguration();
 			IRemoteServices remoteServices = PTPRemoteCorePlugin.getDefault()
 					.getRemoteServices(conf.getRemoteServicesId(), monitor);
 			if (remoteServices != null) {
 				IRemoteConnectionManager rconnMgr = remoteServices.getConnectionManager();
 				if (rconnMgr != null) {
 					IRemoteConnection rconn = rconnMgr.getConnection(conf.getConnectionName());
 					if (rconn != null && rconn.isOpen()) {
 						return remoteServices.getFileManager(rconn);
 					}
 				}
 			}
 		}
 		return null;
 	}
 
 	/**
 	 * Check if the copy local file is enabled. If it is, copy the executable
 	 * file from the local host to the remote host.
 	 * 
 	 * @param configuration
 	 *            launch configuration
 	 * @throws CoreException
 	 *             if the copy fails or is cancelled
 	 */
 	protected void copyExecutable(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
 		boolean copyExecutable = getCopyExecutable(configuration);
 
 		if (copyExecutable) {
 			// Get remote and local paths
 			String remotePath = getExecutablePath(configuration);
 			String localPath = configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_LOCAL_EXECUTABLE_PATH,
 					(String) null);
 
 			// Check if local path is valid
 			if (localPath == null) {
 				throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 						Messages.AbstractParallelLaunchConfigurationDelegate_1));
 			}
 
 			// Copy data
 			copyFileToRemoteHost(localPath, remotePath, configuration, monitor);
 		}
 	}
 
 	/**
 	 * Copy a data from a path (can be a file or directory) from the remote host
 	 * to the local host.
 	 * 
 	 * @param remotePath
 	 * @param localPath
 	 * @param configuration
 	 * @throws CoreException
 	 */
 	protected void copyFileFromRemoteHost(String remotePath, String localPath, ILaunchConfiguration configuration,
 			IProgressMonitor monitor) throws CoreException {
 		SubMonitor progress = SubMonitor.convert(monitor, 15);
 		try {
 			IRemoteFileManager localFileManager = getLocalFileManager(configuration);
 			IRemoteFileManager remoteFileManager = getRemoteFileManager(configuration, progress.newChild(5));
 			if (progress.isCanceled()) {
 				throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 						Messages.AbstractParallelLaunchConfigurationDelegate_Operation_cancelled_by_user, null));
 			}
 			if (remoteFileManager == null) {
 				throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 						Messages.AbstractParallelLaunchConfigurationDelegate_0));
 			}
 
 			IFileStore rres = remoteFileManager.getResource(remotePath);
 			if (!rres.fetchInfo(EFS.NONE, progress.newChild(5)).exists()) {
 				// Local file not found!
 				throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 						Messages.AbstractParallelLaunchConfigurationDelegate_Remote_resource_does_not_exist));
 			}
 			IFileStore lres = localFileManager.getResource(localPath);
 
 			// Copy file
 			rres.copy(lres, EFS.OVERWRITE, progress.newChild(5));
 		} finally {
 			if (monitor != null) {
 				monitor.done();
 			}
 		}
 	}
 
 	/**
 	 * Copy a data from a path (can be a file or directory) from the local host
 	 * to the remote host.
 	 * 
 	 * @param localPath
 	 * @param remotePath
 	 * @param configuration
 	 * @throws CoreException
 	 */
 	protected void copyFileToRemoteHost(String localPath, String remotePath, ILaunchConfiguration configuration,
 			IProgressMonitor monitor) throws CoreException {
 		SubMonitor progress = SubMonitor.convert(monitor, 15);
 		try {
 			IRemoteFileManager localFileManager = getLocalFileManager(configuration);
 			IRemoteFileManager remoteFileManager = getRemoteFileManager(configuration, progress.newChild(5));
 			if (progress.isCanceled()) {
 				throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 						Messages.AbstractParallelLaunchConfigurationDelegate_Operation_cancelled_by_user, null));
 			}
 			if (remoteFileManager == null) {
 				throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 						Messages.AbstractParallelLaunchConfigurationDelegate_0));
 			}
 
 			IFileStore lres = localFileManager.getResource(localPath);
 			if (!lres.fetchInfo(EFS.NONE, progress.newChild(5)).exists()) {
 				// Local file not found!
 				throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 						Messages.AbstractParallelLaunchConfigurationDelegate_Local_resource_does_not_exist));
 			}
 			IFileStore rres = remoteFileManager.getResource(remotePath);
 
 			// Copy file
 			lres.copy(rres, EFS.OVERWRITE, progress.newChild(5));
 		} finally {
 			if (monitor != null) {
 				monitor.done();
 			}
 		}
 	}
 
 	/**
 	 * Called to cleanup once the job terminates
 	 * 
 	 * @param config
 	 * @param mode
 	 * @param launch
 	 * @since 5.0
 	 */
 	protected abstract void doCleanupLaunch(IPLaunch launch);
 
 	/**
 	 * This method is called when the job state changes to RUNNING. This allows
 	 * the launcher to complete the job launch.
 	 * 
 	 * @param launch
 	 * @param debugger
 	 * @since 5.0
 	 */
 	protected abstract void doCompleteJobLaunch(IPLaunch launch, IPDebugger debugger);
 
 	/**
 	 * @param configuration
 	 * @throws CoreException
 	 */
 	protected void doPostLaunchSynchronization(ILaunchConfiguration configuration) throws CoreException {
 		if (configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_SYNC_AFTER, false)) {
 			List<?> rulesList = configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_SYNC_RULES,
 					new ArrayList<String>());
 
 			// This faction generate action objects which execute according to
 			// rules
 			RuleActionFactory ruleActFactory = new RuleActionFactory(configuration, this, new NullProgressMonitor());
 
 			for (Object ruleObj : rulesList) {
 				ISynchronizationRule syncRule = RuleFactory.createRuleFromString((String) ruleObj);
 				if (syncRule.isDownloadRule()) {
 					// Execute the action
 					IRuleAction action = ruleActFactory.getAction(syncRule);
 					action.run();
 				}
 			}
 		}
 	}
 
 	/**
 	 * This method does the synchronization step before the job submission
 	 * 
 	 * @param configuration
 	 * @param monitor
 	 */
 	protected void doPreLaunchSynchronization(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
 		if (configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_SYNC_BEFORE, false)) {
 			// This faction generate action objects which execute according to
 			// rules
 			RuleActionFactory ruleActFactory = new RuleActionFactory(configuration, this, monitor);
 
 			try {
 				List<?> rulesList = configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_SYNC_RULES,
 						new ArrayList<String>());
 
 				// Iterate over rules executing them
 				for (Object ruleObj : rulesList) {
 					ISynchronizationRule syncRule = RuleFactory.createRuleFromString((String) ruleObj);
 					if (syncRule.isUploadRule()) {
 						// Execute the action
 						IRuleAction action = ruleActFactory.getAction(syncRule);
 						action.run();
 					}
 
 				}
 			} finally {
 				if (monitor != null) {
 					monitor.done();
 				}
 			}
 		}
 	}
 
 	/**
 	 * @since 5.0
 	 */
 	protected void verifyLaunchAttributes(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor)
 			throws CoreException {
 		SubMonitor progress = SubMonitor.convert(monitor, 30);
 
 		try {
 			IResourceManager rm = getResourceManager(configuration);
 			if (rm == null) {
 				throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 						Messages.AbstractParallelLaunchConfigurationDelegate_No_ResourceManager));
 			}
 
 			/*
 			 * Verify executable path
 			 */
 			verifyExecutablePath(configuration, progress.newChild(10));
 
 			/*
 			 * Verify working directory. Use the executable path if no working
 			 * directory has been set.
 			 */
 			String workPath = getWorkingDirectory(configuration);
			if (workPath != null) {
 				verifyResource(workPath, configuration, monitor);
 			}
 			if (progress.isCanceled()) {
 				throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 						Messages.AbstractParallelLaunchConfigurationDelegate_Operation_cancelled_by_user, null));
 			}
 
 			if (mode.equals(ILaunchManager.DEBUG_MODE)) {
 				verifyDebuggerPath(configuration, progress.newChild(10));
 			}
 		} finally {
 			if (monitor != null) {
 				monitor.done();
 			}
 		}
 	}
 
 	/**
 	 * Get the debugger configuration
 	 * 
 	 * @param configuration
 	 *            launch configuration
 	 * @return debugger configuration
 	 * @throws CoreException
 	 */
 	protected IPDebugConfiguration getDebugConfig(ILaunchConfiguration config) throws CoreException {
 		return PTPDebugCorePlugin.getDefault().getDebugConfiguration(getDebuggerID(config));
 	}
 
 	/**
 	 * Get the absolute path of the executable to launch. If the executable is
 	 * on a remote machine, this is the path to the executable on that machine.
 	 * 
 	 * @param configuration
 	 * @return
 	 * @throws CoreException
 	 */
 	protected String getExecutablePath(ILaunchConfiguration configuration) throws CoreException {
 		return configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_EXECUTABLE_PATH, (String) null);
 	}
 
 	/**
 	 * Convert application arguments to an array of strings.
 	 * 
 	 * @param configuration
 	 *            launch configuration
 	 * @return array of strings containing the program arguments
 	 * @throws CoreException
 	 */
 	protected String[] getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
 		String temp = getArguments(configuration);
 		if (temp != null && temp.length() > 0) {
 			ArgumentParser ap = new ArgumentParser(temp);
 			List<String> args = ap.getTokenList();
 			if (args != null) {
 				return args.toArray(new String[args.size()]);
 			}
 		}
 		return new String[0];
 	}
 
 	/**
 	 * Get the name of the executable to launch
 	 * 
 	 * @param configuration
 	 * @return
 	 * @throws CoreException
 	 */
 	protected String getProgramName(ILaunchConfiguration configuration) throws CoreException {
 		String exePath = getExecutablePath(configuration);
 		if (exePath != null) {
 			return new Path(exePath).lastSegment();
 		}
 		return null;
 	}
 
 	/**
 	 * Get the path component of the executable to launch.
 	 * 
 	 * @param configuration
 	 * @return
 	 * @throws CoreException
 	 * @since 5.0
 	 */
 	protected String getProgramPath(ILaunchConfiguration configuration) throws CoreException {
 		String exePath = getExecutablePath(configuration);
 		if (exePath != null) {
 			return new Path(exePath).removeLastSegments(1).toString();
 		}
 		return null;
 	}
 
 	/**
 	 * Get the IProject object from the project name.
 	 * 
 	 * @param project
 	 *            name of the project
 	 * @return IProject resource
 	 */
 	protected IProject getProject(String project) {
 		return getWorkspaceRoot().getProject(project);
 	}
 
 	/**
 	 * Get the default queue for the given resource manager
 	 * 
 	 * @param rm
 	 *            resource manager
 	 * @return default queue
 	 * @since 5.0
 	 */
 	protected IPQueue getQueueDefault(IPResourceManager rm) {
 		final IPQueue[] queues = rm.getQueues();
 		if (queues.length == 0) {
 			return null;
 		}
 		return queues[0];
 	}
 
 	/**
 	 * Find the resource manager that corresponds to the unique name specified
 	 * in the configuration
 	 * 
 	 * @param configuration
 	 *            launch configuration
 	 * @return resource manager
 	 * @throws CoreException
 	 * @since 5.0
 	 */
 	protected IResourceManager getResourceManager(ILaunchConfiguration configuration) throws CoreException {
 		String rmUniqueName = getResourceManagerUniqueName(configuration);
 		if (rmUniqueName != null) {
 			return PTPCorePlugin.getDefault().getModelManager().getResourceManagerFromUniqueName(rmUniqueName);
 		}
 		return null;
 	}
 
 	/**
 	 * Returns the (possible empty) list of synchronization rule objects
 	 * according to the rules described in the configuration.
 	 * 
 	 * @since 5.0
 	 */
 	protected ISynchronizationRule[] getSynchronizeRules(ILaunchConfiguration configuration) throws CoreException {
 		List<?> ruleStrings = configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_SYNC_RULES, new ArrayList<String>());
 		List<ISynchronizationRule> result = new ArrayList<ISynchronizationRule>();
 
 		for (Object ruleObj : ruleStrings) {
 			String element = (String) ruleObj;
 			try {
 				ISynchronizationRule rule = RuleFactory.createRuleFromString(element);
 				result.add(rule);
 			} catch (RuntimeException e) {
 				throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 						Messages.AbstractParallelLaunchConfigurationDelegate_Error_converting_rules));
 			}
 		}
 
 		return result.toArray(new ISynchronizationRule[result.size()]);
 	}
 
 	/**
 	 * Get the working directory for the application launch
 	 * 
 	 * @param configuration
 	 * @return
 	 * @throws CoreException
 	 * @since 5.0
 	 */
 	protected String getWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
 		return configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_WORKING_DIR, (String) null);
 	}
 
 	/**
 	 * Set the working directory
 	 * 
 	 * @param configuration
 	 * @param dir
 	 * @throws CoreException
 	 * @since 5.0
 	 */
 	protected void setWorkingDirectory(ILaunchConfigurationWorkingCopy configuration, String dir) throws CoreException {
 		configuration.setAttribute(IPTPLaunchConfigurationConstants.ATTR_WORKING_DIR, dir);
 	}
 
 	/**
 	 * Get the workspace root.
 	 * 
 	 * @return workspace root
 	 */
 	protected IWorkspaceRoot getWorkspaceRoot() {
 		return ResourcesPlugin.getWorkspace().getRoot();
 	}
 
 	/**
 	 * Create a source locator from the ID specified in the configuration, or
 	 * create a default one if it hasn't been specified.
 	 * 
 	 * @param launch
 	 * @param configuration
 	 * @throws CoreException
 	 */
 	protected void setDefaultSourceLocator(ILaunch launch, ILaunchConfiguration configuration) throws CoreException {
 		// set default source locator if none specified
 		if (launch.getSourceLocator() == null) {
 			IPersistableSourceLocator sourceLocator;
 			String id = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, (String) null);
 			if (id == null) {
 				sourceLocator = PTPDebugUIPlugin.createDefaultSourceLocator();
 				sourceLocator.initializeDefaults(configuration);
 			} else {
 				sourceLocator = DebugPlugin.getDefault().getLaunchManager().newSourceLocator(id);
 				String memento = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String) null);
 				if (memento == null) {
 					sourceLocator.initializeDefaults(configuration);
 				} else {
 					sourceLocator.initializeFromMemento(memento);
 				}
 			}
 			launch.setSourceLocator(sourceLocator);
 		}
 	}
 
 	/**
 	 * Set the source locator for this application
 	 * 
 	 * @param launch
 	 * @param config
 	 * @throws CoreException
 	 */
 	protected void setSourceLocator(ILaunch launch, ILaunchConfiguration config) throws CoreException {
 		setDefaultSourceLocator(launch, config);
 	}
 
 	/**
 	 * Submit a job to the resource manager. Keeps track of the submission so we
 	 * know when the job actually starts running. When this happens, the
 	 * abstract method doCompleteJobLaunch() is invoked.
 	 * 
 	 * @param configuration
 	 * @param mode
 	 * @param launch
 	 * @param debugger
 	 * @param monitor
 	 * @throws CoreException
 	 * @since 5.0
 	 */
 	protected void submitJob(ILaunchConfiguration configuration, String mode, IPLaunch launch, IPDebugger debugger,
 			IProgressMonitor monitor) throws CoreException {
 		SubMonitor progress = SubMonitor.convert(monitor, 10);
 		try {
 			final IResourceManager rm = getResourceManager(configuration);
 			if (rm == null) {
 				throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 						Messages.AbstractParallelLaunchConfigurationDelegate_No_ResourceManager));
 			}
 			rm.addJobListener(fJobListener);
 			String jobId = rm.submitJob(configuration, mode, progress.newChild(5));
 			if (rm.getJobStatus(jobId, progress.newChild(50)).equals(IJobStatus.UNDETERMINED)) {
 				throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 						Messages.AbstractParallelLaunchConfigurationDelegate_UnableToDetermineJobStatus));
 			}
 			launch.setJobId(jobId);
 			launch.setResourceManager(rm);
 			JobSubmission jobSub = new JobSubmission(launch, debugger);
 			synchronized (jobSubmissions) {
 				jobSubmissions.put(jobId, jobSub);
 			}
 			jobSub.schedule();
 		} finally {
 			if (monitor != null) {
 				monitor.done();
 			}
 		}
 	}
 
 	/**
 	 * Verify the validity of the debugger path.
 	 * 
 	 * @param configuration
 	 *            launch configuration
 	 * @param monitor
 	 *            progress monitor
 	 * @throws CoreException
 	 *             if the path is invalid or the monitor was canceled.
 	 * @since 5.0
 	 */
 	protected void verifyDebuggerPath(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
 		String dbgPath = getDebuggerExePath(configuration);
 		if (dbgPath == null) {
 			throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 					Messages.AbstractParallelLaunchConfigurationDelegate_debuggerPathNotSpecified));
 		}
 		try {
 			verifyResource(dbgPath, configuration, monitor);
 			if (monitor.isCanceled()) {
 				throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 						Messages.AbstractParallelLaunchConfigurationDelegate_Operation_cancelled_by_user, null));
 			}
 		} catch (CoreException e) {
 			throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 					Messages.AbstractParallelLaunchConfigurationDelegate_Debugger_path_not_found, new FileNotFoundException(
 							e.getLocalizedMessage())));
 		}
 	}
 
 	/**
 	 * Verify the validity of executable path. If the executable is to be
 	 * copied, then no additional verification is required. Otherwise, the path
 	 * must point to an existing file.
 	 * 
 	 * @param configuration
 	 *            launch configuration
 	 * @param monitor
 	 *            progress monitor
 	 * @return IPath representing path to the executable (either local or
 	 *         remote)
 	 * @throws CoreException
 	 *             if the resource can't be found or the monitor was canceled.
 	 * @since 5.0
 	 */
 	protected IPath verifyExecutablePath(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
 		if (getCopyExecutable(configuration)) {
 			return new Path(getExecutablePath(configuration));
 		} else {
 			String exePath = getExecutablePath(configuration);
 			try {
 				IPath path = verifyResource(exePath, configuration, monitor);
 				if (monitor.isCanceled()) {
 					throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 							Messages.AbstractParallelLaunchConfigurationDelegate_Operation_cancelled_by_user, null));
 				}
 				return path;
 			} catch (CoreException e) {
 				throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 						Messages.AbstractParallelLaunchConfigurationDelegate_Application_file_does_not_exist,
 						new FileNotFoundException(e.getLocalizedMessage())));
 			}
 		}
 	}
 
 	/**
 	 * Verify that the project exists prior to the launch.
 	 * 
 	 * @param configuration
 	 * @return
 	 * @throws CoreException
 	 */
 	protected IProject verifyProject(ILaunchConfiguration configuration) throws CoreException {
 		String proName = getProjectName(configuration);
 		if (proName == null) {
 			throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 					Messages.AbstractParallelLaunchConfigurationDelegate_Project_not_specified));
 		}
 
 		IProject project = getProject(proName);
 		if (project == null || !project.exists() || !project.isOpen()) {
 			throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 					Messages.AbstractParallelLaunchConfigurationDelegate_Project_does_not_exist_or_is_not_a_project));
 		}
 
 		return project;
 	}
 
 	/**
 	 * @param path
 	 * @param configuration
 	 * @return
 	 * @throws CoreException
 	 * @since 5.0
 	 */
 	protected IPath verifyResource(String path, ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
 		return PTPLaunchPlugin.getDefault().verifyResource(path, configuration, monitor);
 	}
 
 	/**
 	 * Verify the working directory. If no working directory is specified, the
 	 * default is the location of the executable.
 	 * 
 	 * @param configuration
 	 *            launch configuration
 	 * @param monitor
 	 *            progress monitor
 	 * @return path of working directory
 	 * @throws CoreException
 	 *             if the working directory is invalid or the monitor was
 	 *             canceled.
 	 * @since 5.0
 	 */
 	protected String verifyWorkDirectory(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
 		IPath path;
 		String workPath = getWorkingDirectory(configuration);
 		if (workPath == null) {
 			path = verifyExecutablePath(configuration, monitor).removeLastSegments(1);
 		} else {
 			path = verifyResource(workPath, configuration, monitor);
 		}
 		if (monitor.isCanceled()) {
 			throw new CoreException(new Status(IStatus.ERROR, PTPLaunchPlugin.getUniqueIdentifier(),
 					Messages.AbstractParallelLaunchConfigurationDelegate_Operation_cancelled_by_user, null));
 		}
 		return path.toString();
 	}
 
 }
