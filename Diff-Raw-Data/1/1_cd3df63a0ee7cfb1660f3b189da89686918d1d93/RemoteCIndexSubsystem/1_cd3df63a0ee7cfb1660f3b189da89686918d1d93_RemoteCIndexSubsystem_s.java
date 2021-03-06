 /*******************************************************************************
  * Copyright (c) 2008 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  * IBM - Initial API and implementation
  *******************************************************************************/
 package org.eclipse.ptp.rdt.ui.subsystems;
 
 import java.io.IOException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Set;
 
 import org.eclipse.cdt.core.CCProjectNature;
 import org.eclipse.cdt.core.CProjectNature;
 import org.eclipse.cdt.core.model.CModelException;
 import org.eclipse.cdt.core.model.CoreModelUtil;
 import org.eclipse.cdt.core.model.ICContainer;
 import org.eclipse.cdt.core.model.ICElement;
 import org.eclipse.cdt.core.model.ICProject;
 import org.eclipse.cdt.core.model.IParent;
 import org.eclipse.cdt.core.model.ITranslationUnit;
 import org.eclipse.cdt.internal.core.indexer.IStandaloneScannerInfoProvider;
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.IResourceVisitor;
 import org.eclipse.core.resources.IWorkspace;
 import org.eclipse.core.resources.IWorkspaceRoot;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.dstore.core.model.DataElement;
 import org.eclipse.dstore.core.model.DataStore;
 import org.eclipse.dstore.core.model.DataStoreSchema;
 import org.eclipse.ptp.internal.rdt.core.Serializer;
 import org.eclipse.ptp.internal.rdt.core.callhierarchy.CalledByResult;
 import org.eclipse.ptp.internal.rdt.core.callhierarchy.CallsToResult;
 import org.eclipse.ptp.internal.rdt.core.contentassist.Proposal;
 import org.eclipse.ptp.internal.rdt.core.contentassist.RemoteContentAssistInvocationContext;
 import org.eclipse.ptp.internal.rdt.core.miners.CDTMiner;
 import org.eclipse.ptp.internal.rdt.core.model.Scope;
 import org.eclipse.ptp.internal.rdt.core.search.RemoteSearchMatch;
 import org.eclipse.ptp.internal.rdt.core.search.RemoteSearchQuery;
 import org.eclipse.ptp.internal.rdt.core.subsystems.ICIndexSubsystem;
 import org.eclipse.ptp.internal.rdt.core.typehierarchy.THGraph;
 import org.eclipse.ptp.rdt.core.RDTLog;
 import org.eclipse.ptp.rdt.core.resources.RemoteNature;
 import org.eclipse.ptp.rdt.services.core.IService;
 import org.eclipse.ptp.rdt.services.core.IServiceConfiguration;
 import org.eclipse.ptp.rdt.services.core.IServiceProvider;
 import org.eclipse.ptp.rdt.services.core.ServiceModelManager;
 import org.eclipse.ptp.rdt.ui.messages.Messages;
 import org.eclipse.ptp.rdt.ui.serviceproviders.RemoteCIndexServiceProvider;
 import org.eclipse.rse.connectorservice.dstore.DStoreConnectorService;
 import org.eclipse.rse.connectorservice.dstore.util.StatusMonitor;
 import org.eclipse.rse.connectorservice.dstore.util.StatusMonitorFactory;
 import org.eclipse.rse.core.model.IHost;
 import org.eclipse.rse.core.subsystems.IConnectorService;
 import org.eclipse.rse.core.subsystems.SubSystem;
 import org.eclipse.swt.widgets.Display;
 
 /**
  * An RSE subsystem which is used to provide C/C++ indexing services from a Miner
  * running on a remote host.
  * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
  * part of a work in progress. There is no guarantee that this API will work or
  * that it will remain the same. Please do not use this API without consulting
  * with the RDT team.
  * 
  * @author crecoskie
  *
  */
 public class RemoteCIndexSubsystem extends SubSystem implements ICIndexSubsystem {
 
 	private Set<IProject> fInitializedProjects;
 	private ProjectChangeListener fProjectOpenListener;
 
 	protected RemoteCIndexSubsystem(IHost host,
 			IConnectorService connectorService) {
 		super(host, connectorService);
 		// TODO Auto-generated constructor stub
 	}
 
 	
 	
 	// index management
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.rse.core.subsystems.SubSystem#initializeSubSystem(org.eclipse.core.runtime.IProgressMonitor)
 	 */
 	@Override
 	public void initializeSubSystem(IProgressMonitor monitor) {
 		super.initializeSubSystem(monitor);
 		
 		fInitializedProjects = new HashSet<IProject>();
 		fProjectOpenListener = new ProjectChangeListener(this);
 		ResourcesPlugin.getWorkspace().addResourceChangeListener(fProjectOpenListener);
 		
 		getDataStore().activateMiner("org.eclipse.ptp.internal.rdt.core.miners.CDTMiner"); //$NON-NLS-1$
 	}
 
 	@Override
 	public void uninitializeSubSystem(IProgressMonitor monitor) {
 		super.uninitializeSubSystem(monitor);
 		
 		ResourcesPlugin.getWorkspace().removeResourceChangeListener(fProjectOpenListener);
 		fInitializedProjects = null;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.internal.rdt.core.subsystems.ICIndexSubsystem#startIndexOfScope(org.eclipse.ptp.internal.rdt.core.model.Scope, org.eclipse.core.runtime.IProgressMonitor)
 	 */
 	public IStatus startIndexOfScope(Scope scope, IStandaloneScannerInfoProvider provider, IProgressMonitor monitor)
 	{
 		DataStore dataStore = getDataStore();
 		   
 	    if (dataStore != null)
 	    {
 	    	
 	     	StatusMonitor smonitor = StatusMonitorFactory.getInstance().getStatusMonitorFor(getConnectorService(), dataStore);
 	     	
 	    	
 	    	monitor.beginTask(Messages.getString("RemoteCIndexSubsystem.0"), 100); //$NON-NLS-1$
 	   
 	        DataElement queryCmd = dataStore.localDescriptorQuery(dataStore.getDescriptorRoot(), CDTMiner.C_INDEX_START);
             if (queryCmd != null)
             {
                       	
             	ArrayList<Object> args = new ArrayList<Object>();
             	            	
             	// need to know the scope
             	DataElement scopeElement = dataStore.createObject(null, CDTMiner.T_SCOPE_SCOPENAME_DESCRIPTOR, scope.getName());
             	args.add(scopeElement);
             	
             	String serializedProvider = null;
             	try {
 					serializedProvider = Serializer.serialize(provider);
 				} catch (IOException e) {
 					RDTLog.logError(e);
 				}
 				
 				DataElement providerElement = dataStore.createObject(null, CDTMiner.T_INDEX_SCANNER_INFO_PROVIDER, serializedProvider);
 				args.add(providerElement);
             
            	
             	// execute the command
             	//DataElement status = dataStore.command(queryCmd, dataStore.getDescriptorRoot(), true); 
             	DataElement status = dataStore.command(queryCmd, args, dataStore.getDescriptorRoot());
             	
             	try
                 {
                 	smonitor.waitForUpdate(status, monitor);
                 	if (monitor.isCanceled())
                 	{
                 		cancelOperation(monitor, status.getParent());
                 	}
                 }
                 catch (Exception e)
                 {                	
                 }
             	
             }	
                     
 	    }
 	    
 	    return Status.OK_STATUS;
 
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.internal.rdt.core.subsystems.ICIndexSubsystem#reindexScope(org.eclipse.ptp.internal.rdt.core.model.Scope, org.eclipse.core.runtime.IProgressMonitor)
 	 */
 	public IStatus reindexScope(Scope scope, IStandaloneScannerInfoProvider provider, IProgressMonitor monitor)
 	{
 		DataStore dataStore = getDataStore();
 		   
 	    if (dataStore != null)
 	    {
 	     	
 	    	DataElement result = getDataStore().createObject(null, CDTMiner.T_INDEX_STATUS_DESCRIPTOR, null);
 	     	StatusMonitor smonitor = StatusMonitorFactory.getInstance().getStatusMonitorFor(getConnectorService(), dataStore);
 	     	
 //	     	int count = 0;
 //	    	DataElement countCmd = dataStore.localDescriptorQuery(datastore.getDescriptorRoot(), CDTMiner.C_SCOPE_COUNT_ELEMENTS);
 //	    	if (countCmd != null)
 //	    	{
 //	    		DataElement countStatus  = dataStore.command(countCmd, result, true);
 //	    		try
 //                {
 //                	smonitor.waitForUpdate(countStatus, monitor, 5000);
 //                	if (monitor.isCanceled())
 //                	{
 //                		cancelOperation(monitor, countStatus.getParent());
 //                	}
 //                }
 //                catch (Exception e)
 //                {                	
 //                } 
 //                count = Integer.parseInt(countStatus.getSource());
 //	    	}
 //	    	
 //	    	monitor.beginTask(Messages.getString("RemoteCIndexSubsystem.1"), count); //$NON-NLS-1$
 	   
 	        DataElement queryCmd = dataStore.localDescriptorQuery(result.getDescriptor(), CDTMiner.C_INDEX_REINDEX);
             if (queryCmd != null)
             {
                       	
             	ArrayList<Object> args = new ArrayList<Object>();
             	            	
             	// need to know the scope
             	args.add(scope.getName());
             	
             	String serializedProvider = null;
             	try {
 					serializedProvider = Serializer.serialize(provider);
 					System.out.println("length " + serializedProvider.length());//$NON-NLS-1$
 				} catch (IOException e) {
 					RDTLog.logError(e);
 				}
 				
 				DataElement providerElement = dataStore.createObject(null, CDTMiner.T_INDEX_SCANNER_INFO_PROVIDER, serializedProvider);
 				args.add(providerElement);
             	
                 DataElement status = dataStore.command(queryCmd, args, result);   
                 int num = 0;
                 try
                 {
                 	Display display = Display.getCurrent();
 
 //                	if (count > 0)
 //                	{
              		String statValue = status.getName();
 //                		int totalWorked = 0;
                			while (!statValue.equals("done") && !monitor.isCanceled()) //$NON-NLS-1$
                			{
 //                			  
 //                			while (display != null && display.readAndDispatch());
 //                			             			
 //      
 //                			String numStr = status.getValue();
 //                			if (!numStr.equals("start") && !numStr.equals("working")) //$NON-NLS-1$ //$NON-NLS-2$
 //                			{
 //                				try
 //                				{
 //                					int newnum = Integer.parseInt(numStr);
 //                					if (newnum > num)
 //                					{
 //                						int delta = newnum - num; 
 //                		
 //                						num = newnum;
 //                				   
 //                						
 //                			       		monitor.subTask(Messages.getString("RemoteCIndexSubsystem.2") + status.getAttribute(DE.A_SOURCE));   //$NON-NLS-1$
 //                						monitor.worked(delta);
 //                						totalWorked+=delta;
 //                						while (display != null && display.readAndDispatch());
 //                					}    
 //                					else
 //                					{
                 						Thread.sleep(100); 
 //                					}
 //                				}
 //                				catch (Exception e)
 //                				{
 //                					e.printStackTrace();
 //                				}
 //               			}
 //             
 //                			
                 			statValue = status.getName();
                 		}
                 		monitor.done();
 //                	}
 //                	else
 //                	{               
 //                		smonitor.waitForUpdate(status, monitor, 5000);
 //                		if (monitor.isCanceled())
 //                		{
 //                			cancelOperation(monitor, status.getParent());
 //                		}
 //                	}
                 }
                 catch (Exception e)
                 {       
                 	e.printStackTrace();
                 }
             }
 	    }
 	    
 	    return Status.OK_STATUS;
 
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.internal.rdt.core.subsystems.ICIndexSubsystem#indexDelta(org.eclipse.ptp.internal.rdt.core.model.Scope, java.util.List, java.util.List, java.util.List, org.eclipse.core.runtime.IProgressMonitor)
 	 */
 	public IStatus indexDelta(Scope scope, IStandaloneScannerInfoProvider provider, List<ICElement> newElements, List<ICElement> changedElements, List<ICElement> deletedElements, IProgressMonitor monitor)
 	{
 		DataStore dataStore = getDataStore();
 		   
 	    if (dataStore != null)
 	    {
 	     	
 	    	DataElement result = getDataStore().createObject(null, CDTMiner.T_INDEX_STATUS_DESCRIPTOR, "index"); //$NON-NLS-1$
 	     	StatusMonitor smonitor = StatusMonitorFactory.getInstance().getStatusMonitorFor(_connectorService, dataStore);
 	     	
 	     	int workCount = newElements.size() + changedElements.size();
 	    	
 	    	monitor.beginTask("Incrementally Indexing...", workCount); //$NON-NLS-1$
 	   
 	        DataElement queryCmd = dataStore.localDescriptorQuery(dataStore.getDescriptorRoot(), CDTMiner.C_INDEX_DELTA);
             if (queryCmd != null)
             {
                       	
             	ArrayList<Object> args = new ArrayList<Object>();
             	            	
             	// need to know the scope
                	DataElement scopeElement = dataStore.createObject(null, CDTMiner.T_SCOPE_SCOPENAME_DESCRIPTOR, scope.getName());
                	args.add(scopeElement);
                	
                	
                	String serializedProvider = null;
             	try {
 					serializedProvider = Serializer.serialize(provider);
 				} catch (IOException e) {
 					RDTLog.logError(e);
 				}
 				
 				DataElement providerElement = dataStore.createObject(null, CDTMiner.T_INDEX_SCANNER_INFO_PROVIDER, serializedProvider);
 				args.add(providerElement);
 				
 				
                	// iterate through the additions and create an object for each addition
                	Iterator<ICElement> iterator = newElements.iterator();
                	
                	while(iterator.hasNext()) {
                		ICElement element = iterator.next();
                		
                		// figure out the path to the element on the remote machine
                		String remotePath = convertURIToRemotePath(element.getLocationURI());
                		
                    	DataElement addedElement = dataStore.createObject(null, CDTMiner.T_INDEX_DELTA_ADDED, remotePath);
                    	args.add(addedElement);
                	}
                	
                	// iterate through the changed elements and create an object for each change
                	iterator = changedElements.iterator();
                	
                	while(iterator.hasNext()) {
                		ICElement element = iterator.next();
                		
                		// figure out the path to the element on the remote machine
                		String remotePath = convertURIToRemotePath(element.getLocationURI());
                		
                    	DataElement changedElement = dataStore.createObject(null, CDTMiner.T_INDEX_DELTA_CHANGED, remotePath);
                    	args.add(changedElement);
                	}
                	
                	// iterate through the deleted elements and create an object for each change
                	iterator = deletedElements.iterator();
                	
                	while(iterator.hasNext()) {
                		ICElement element = iterator.next();
                		
                		// figure out the path to the element on the remote machine
                		String remotePath = convertURIToRemotePath(element.getLocationURI());
                		
                    	DataElement deletedElement = dataStore.createObject(null, CDTMiner.T_INDEX_DELTA_REMOVED, remotePath);
                    	args.add(deletedElement);
                	}
                	
         
             	
                 DataElement status = dataStore.command(queryCmd, args, result);   
 //                int num = 0;
 //                try
 //                {
 //                	Display display = Display.getCurrent();
 //
 //                	if (workCount > 0)
 //                	{
 //                		String statValue = status.getName();
 //                		int totalWorked = 0;
 //                		while (!statValue.equals("done") && !monitor.isCanceled()) //$NON-NLS-1$
 //                		{
 //                			  
 //                			while (display != null && display.readAndDispatch());
 //                			             			
 //      
 //                			String numStr = status.getValue();
 //                			if (!numStr.equals("start") && !numStr.equals("working")) //$NON-NLS-1$ //$NON-NLS-2$
 //                			{
 //                				try
 //                				{
 //                					int newnum = Integer.parseInt(numStr);
 //                					if (newnum > num)
 //                					{
 //                						int delta = newnum - num; 
 //                		
 //                						num = newnum;
 //	
 //                			       		monitor.subTask("Indexing " + status.getAttribute(DE.A_SOURCE_LOCATION));   //$NON-NLS-1$
 //                						monitor.worked(delta);
 //                						totalWorked+=delta;
 //                						while (display != null && display.readAndDispatch());
 //                					}    
 //                					else
 //                					{
 //                						Thread.sleep(100); 
 //                					}
 //                				}
 //                				catch (Exception e)
 //                				{
 //                					e.printStackTrace();
 //                				}
 //                			}
 //             
 //                			
 //                			statValue = status.getName();
 //                		}
                 try {
 					smonitor.waitForUpdate(status, monitor);
 					if (monitor.isCanceled()) {
 						cancelOperation(monitor, status.getParent());
 					}
 				} catch (Exception e) {
 				}
 
 				monitor.done();
 			}
 
 		}
 	    
     return Status.OK_STATUS;
 
 	}
 	
 	// scope management
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.internal.rdt.core.subsystems.ICIndexSubsystem#registerScope(org.eclipse.ptp.internal.rdt.core.model.Scope, java.lang.String[], org.eclipse.core.runtime.IProgressMonitor)
 	 */
 	public IStatus registerScope(Scope scope, List<ICElement> elements, IProgressMonitor monitor)
 	{
 		DataStore dataStore = getDataStore();
 		   
 	    if (dataStore != null)
 	    {
 	    	
 	     	StatusMonitor smonitor = StatusMonitorFactory.getInstance().getStatusMonitorFor(getConnectorService(), dataStore);
 	     	
 	    	
 	    	monitor.beginTask(Messages.getString("RemoteCIndexSubsystem.3"), 100); //$NON-NLS-1$
 	   
 	        DataElement queryCmd = dataStore.localDescriptorQuery(dataStore.getDescriptorRoot(), CDTMiner.C_SCOPE_REGISTER);
             if (queryCmd != null)
             {
                       	
             	ArrayList<Object> args = new ArrayList<Object>();
             	            	
             	// need to know the scope
             	DataElement scopeElement = dataStore.createObject(null, CDTMiner.T_SCOPE_SCOPENAME_DESCRIPTOR, scope.getName());
             	args.add(scopeElement);
             
             	// add in the filenames
             	Iterator<ICElement> iterator = elements.iterator();
             	while(iterator.hasNext())
             	{
             		addElement(dataStore, args, iterator.next());
             	}
             	
             	// execute the command
             	DataElement status = dataStore.command(queryCmd, args, dataStore.getDescriptorRoot());
             	
             	try
                 {
                 	smonitor.waitForUpdate(status, monitor);
                 	if (monitor.isCanceled())
                 	{
                 		cancelOperation(monitor, status.getParent());
                 	}
                 }
                 catch (Exception e)
                 {                	
                 }
             	
             	int i=0;
             	i++;
             }	
             
             
 	    }
 	    
 	    return Status.OK_STATUS;
 
 	}
 
 
 	/**
 	 * @param dataStore
 	 * @param args
 	 * @param filePath
 	 * @param element
 	 */
 	protected void addElement(DataStore dataStore, ArrayList<Object> args, ICElement element) {
 		
    		String filePath = null;
 		
 		// if it's a translation unit, we can just add it
 		if(element instanceof ITranslationUnit) {
 			filePath = convertURIToRemotePath(element.getLocationURI());
			return;
 		}
 		
 		else {
 			// if it's a container or a project, add the child elements
 			if(element instanceof ICContainer || element instanceof ICProject) {
 				try {
 					ICElement[] children = ((IParent) element).getChildren();
 					
 					for(int k = 0; k < children.length; k++)
 						addElement(dataStore, args, children[k]);
 					
 				} catch (CModelException e) {
 					// TODO Auto-generated catch block
 					e.printStackTrace();
 				}
 			}
 			
 		}
 		
 		if (filePath != null) {
 			DataElement fileElement = dataStore.createObject(null,
 					CDTMiner.T_INDEX_FILENAME_DESCRIPTOR, filePath);
 			args.add(fileElement);
 		}
 	}
 	
 	protected URI convertRemotePathToURI(String path) throws URISyntaxException {
 		return new URI("rse", _host.getHostName(), path, null); //$NON-NLS-1$
 	}
 	
 	protected String convertURIToRemotePath(URI locationURI) {
 		// RSE URIs are of the form rse://host/path
 		
 		// it had better be an RSE URI
 		assert(locationURI.getScheme().equals("rse")); //$NON-NLS-1$
 		
 		// the URI had better correspond to a location on the host that this subsystem is connected to
 		assert(_host.getHostName().equals(locationURI.getHost()));
 		
 		return locationURI.getPath();
 	}
 
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.internal.rdt.core.subsystems.ICIndexSubsystem#unregisterScope(org.eclipse.ptp.internal.rdt.core.model.Scope, org.eclipse.core.runtime.IProgressMonitor)
 	 */
 	public IStatus unregisterScope(Scope scope, IProgressMonitor monitor) {
 	    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(scope.getName());
 		fInitializedProjects.remove(project);
 		
 		DataStore dataStore = getDataStore();
 		   
 	    if (dataStore != null)
 	    {
 	     	
 //	    	DataElement result = dataStore.createObject(null, CDTMiner.T_INDEX_STATUS_DESCRIPTOR, name);
 //	    	DataElement statusDescriptor = dataStore.createObjectDescriptor(null, CDTMiner.T_INDEX_STATUS_DESCRIPTOR);
 	    	
 //	    	result.setDescriptor(statusDescriptor);
 	    	
 	     	StatusMonitor smonitor = StatusMonitorFactory.getInstance().getStatusMonitorFor(getConnectorService(), dataStore);
 	     	
 	    	
 	    	monitor.beginTask(Messages.getString("RemoteCIndexSubsystem.4"), 100); //$NON-NLS-1$
 	   
 	        DataElement queryCmd = dataStore.localDescriptorQuery(dataStore.getDescriptorRoot(), CDTMiner.C_SCOPE_UNREGISTER);
             if (queryCmd != null)
             {
                       	
             	ArrayList<Object> args = new ArrayList<Object>();
             	            	
             	// need to know the scope
             	DataElement scopeElement = dataStore.createObject(null, CDTMiner.T_SCOPE_SCOPENAME_DESCRIPTOR, scope.getName());
             	args.add(scopeElement);
             
             	
             	// execute the command
             	//DataElement status = dataStore.command(queryCmd, dataStore.getDescriptorRoot(), true); 
             	DataElement status = dataStore.command(queryCmd, args, dataStore.getDescriptorRoot());
             	
             	try
                 {
                 	smonitor.waitForUpdate(status, monitor);
                 	if (monitor.isCanceled())
                 	{
                 		cancelOperation(monitor, status.getParent());
                 	}
                 }
                 catch (Exception e)
                 {                	
                 }
             	
 
             }	
             
             
 	    }
 		
 		return Status.OK_STATUS;
 	}
 	
 	// call hierarchy
 	
 
 	public CalledByResult getCallers(Scope scope, ICElement subject, IProgressMonitor monitor) {
     	monitor.beginTask(Messages.getString("RemoteCIndexSubsystem.5") + subject, 100); //$NON-NLS-1$
 		Object result = sendRequest(CDTMiner.C_CALL_HIERARCHY_GET_CALLERS, new Object[] { scope, getHostName(), subject }, null);
 		if (result == null) {
 			return new CalledByResult();
 		}
 		return (CalledByResult) result;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.internal.rdt.core.subsystems.ICIndexSubsystem#getCallees(org.eclipse.ptp.internal.rdt.core.model.Scope, java.lang.String, java.lang.String, int, int, org.eclipse.core.runtime.IProgressMonitor)
 	 */
 	public CallsToResult getCallees(Scope scope, ICElement subject, IProgressMonitor monitor) {
     	monitor.beginTask(Messages.getString("RemoteCIndexSubsystem.6") + subject, 100); //$NON-NLS-1$
 		Object result = sendRequest(CDTMiner.C_CALL_HIERARCHY_GET_CALLS, new Object[] { scope, getHostName(), subject }, null);
 		if (result == null) {
 			return new CallsToResult();
 		}
 		return (CallsToResult) result;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.internal.rdt.core.subsystems.ICIndexSubsystem#getCHDefinitions(org.eclipse.ptp.internal.rdt.core.model.Scope, java.lang.String, java.lang.String, int, int, org.eclipse.core.runtime.IProgressMonitor)
 	 */
 	public ICElement[] getCHDefinitions(Scope scope, ICElement subject, IProgressMonitor monitor) {
     	monitor.beginTask(Messages.getString("RemoteCIndexSubsystem.7") + subject, 100); //$NON-NLS-1$
 		Object result = sendRequest(CDTMiner.C_CALL_HIERARCHY_GET_DEFINITIONS_FROM_ELEMENT, new Object[] { scope, getHostName(), subject }, null);
 		if (result == null) {
 			return new ICElement[0];
 		}
 		return (ICElement[]) result;
 	}
 	
 	public ICElement[] getCHDefinitions(Scope scope, ITranslationUnit unit, int selectionStart, int selectionLength, IProgressMonitor monitor) {
     	monitor.beginTask(Messages.getString("RemoteCIndexSubsystem.7") + unit, 100); //$NON-NLS-1$
 		Object result = sendRequest(CDTMiner.C_CALL_HIERARCHY_GET_DEFINITIONS_FROM_WORKING_COPY, new Object[] { scope, getHostName(), unit, selectionStart, selectionLength }, null);
 		if (result == null) {
 			return new ICElement[0];
 		}
 		return (ICElement[]) result;
 	}
 	
 	@SuppressWarnings("unchecked")
 	public List<RemoteSearchMatch> runQuery(Scope scope, RemoteSearchQuery query, IProgressMonitor monitor) {
     	monitor.beginTask(Messages.getString("RemoteCIndexSubsystem.8") + query.getScopeDescription(), 100); //$NON-NLS-1$
 		Object result = sendRequest(CDTMiner.C_SEARCH_RUN_QUERY, new Object[] { scope, getHostName(), query  }, null);
 		if (result == null) {
 			return Collections.emptyList();
 		}
 		return (List<RemoteSearchMatch>) result;
 	}
 	
 	public List<Proposal> computeCompletionProposals(Scope scope, RemoteContentAssistInvocationContext context, ITranslationUnit unit) {
 		DataStore dataStore = getDataStore();
 	    if (dataStore == null)
 	    {
 	    	return Collections.emptyList();
 	    }
 	    
         DataElement queryCmd = dataStore.localDescriptorQuery(dataStore.getDescriptorRoot(), CDTMiner.C_CONTENT_ASSIST_COMPUTE_PROPOSALS);
         
         if (queryCmd == null)
         {
 	    	return Collections.emptyList();
         }
 
      	NullProgressMonitor monitor = new NullProgressMonitor();
      	StatusMonitor smonitor = StatusMonitorFactory.getInstance().getStatusMonitorFor(getConnectorService(), dataStore);
     	ArrayList<Object> args = new ArrayList<Object>();
 
     	// need to know the scope
     	DataElement dataElement = dataStore.createObject(null, CDTMiner.T_SCOPE_SCOPENAME_DESCRIPTOR, scope.getName());
     	args.add(dataElement);
 
     	// invocation context
     	args.add(createSerializableElement(dataStore, context));
     	
     	// translation unit
     	args.add(createSerializableElement(dataStore, unit));
     	
     	// execute the command
     	DataElement status = dataStore.command(queryCmd, args, dataStore.getDescriptorRoot());
     	
     	try
         {
         	smonitor.waitForUpdate(status, monitor);
         	if (monitor.isCanceled())
         	{
         		cancelOperation(monitor, status.getParent());
         	}
         }
         catch (Exception e)
         {
         	e.printStackTrace();
         }
     	
     	DataElement element = status.get(0);
     	String data = element.getName();
 		try
 		{
 			Object result = Serializer.deserialize(data);
 			if (result == null || !(result instanceof List))
 			{
 				return Collections.emptyList();
 			}
 			return (List<Proposal>) result;
 		} catch (IOException e) {
 			e.printStackTrace();
 		} catch (ClassNotFoundException e) {
 			e.printStackTrace();
 		}
     	return Collections.emptyList();
 	}
 	
 	public THGraph computeTypeGraph(Scope scope, ICElement input, IProgressMonitor monitor) {
 		Object result = sendRequest(CDTMiner.C_TYPE_HIERARCHY_COMPUTE_TYPE_GRAPH, new Object[] { scope, getHostName(), input }, monitor);
 		if (result == null) {
 			return new THGraph();
 		}
 		return (THGraph) result;
 	}
 	
 	public ICElement[] findTypeHierarchyInput(Scope scope, ICElement memberInput) {
 		Object result = sendRequest(CDTMiner.C_TYPE_HIERARCHY_FIND_INPUT1, new Object[] { scope, getHostName(), memberInput }, null);
 		if (result == null) {
 			return new ICElement[] { null, null };
 		}
 		return (ICElement[]) result;
 	}
 	
 	public ICElement[] findTypeHierarchyInput(Scope scope, ITranslationUnit unit, int selectionStart, int selectionLength) {
 		Object result = sendRequest(CDTMiner.C_TYPE_HIERARCHY_FIND_INPUT2, new Object[] { scope, getHostName(), unit, new Integer(selectionStart), new Integer(selectionLength)}, null);
 		if (result == null) {
 			return new ICElement[] { null, null };
 		}
 		return (ICElement[]) result;
 	}
 	
 	public Object sendRequest(String requestType, Object[] arguments, IProgressMonitor monitor) {
 		DataStore dataStore = getDataStore();
 	    if (dataStore == null)
 	    {
 	    	return null;
 	    }
 	    
         DataElement queryCmd = dataStore.localDescriptorQuery(dataStore.getDescriptorRoot(), requestType);
         
         if (queryCmd == null)
         {
 	    	return null;
         }
 
      	StatusMonitor smonitor = StatusMonitorFactory.getInstance().getStatusMonitorFor(getConnectorService(), dataStore);
     	ArrayList<Object> args = new ArrayList<Object>();
 
     	for (Object argument : arguments) {
     		if (argument instanceof Scope) {
     	    	DataElement dataElement = dataStore.createObject(null, CDTMiner.T_SCOPE_SCOPENAME_DESCRIPTOR, ((Scope) argument).getName());
     	    	args.add(dataElement);
     		} else if (argument instanceof String) {
             	DataElement dataElement = dataStore.createObject(null, CDTMiner.T_INDEX_STRING_DESCRIPTOR, (String) argument);
             	args.add(dataElement);
     		} else if (argument instanceof Integer
     				|| argument instanceof Boolean
     				|| argument instanceof Character
     				|| argument instanceof Double
     				|| argument instanceof Float) {
             	DataElement dataElement = dataStore.createObject(null, CDTMiner.T_INDEX_STRING_DESCRIPTOR, argument.toString());
             	args.add(dataElement);
     		} else {
     	    	args.add(createSerializableElement(dataStore, argument));
     		}
     	}
     	
     	// execute the command
     	DataElement status = dataStore.command(queryCmd, args, dataStore.getDescriptorRoot());
     	
     	try
         {
     		IProgressMonitor progressMonitor;
     		if (monitor == null) {
     			progressMonitor = new NullProgressMonitor();
     		} else {
     			progressMonitor = monitor;
     		}
         	smonitor.waitForUpdate(status, progressMonitor);
         	if (progressMonitor.isCanceled())
         	{
         		cancelOperation(progressMonitor, status.getParent());
         	}
         }
         catch (Exception e)
         {
         	e.printStackTrace();
         }
     	
     	DataElement element = status.get(0);
     	if (element == null) {
     		return null;
     	}
     	String data = element.getName();
 		try
 		{
 			Object result = Serializer.deserialize(data);
 			if (result == null)
 			{
 				return null;
 			}
 			return result;
 		} catch (IOException e) {
 			e.printStackTrace();
 		} catch (ClassNotFoundException e) {
 			e.printStackTrace();
 		}
     	return null;
 	}
 	
 	private DataElement createSerializableElement(DataStore dataStore, Object object) {
     	try {
         	String serialized = Serializer.serialize(object);
         	return dataStore.createObject(null, CDTMiner.T_INDEX_STRING_DESCRIPTOR, serialized);
     	} catch (IOException e) {
     		e.printStackTrace();
     		return null;
     	}
 	}
 
 	protected DataStore getDataStore()
 	{
 		IConnectorService connectorService = getConnectorService();
 		
 		if(connectorService instanceof DStoreConnectorService) {
 			return ((DStoreConnectorService) connectorService).getDataStore();
 
 		}
 		return null;
 	}
 	
 	protected void cancelOperation(IProgressMonitor monitor, DataElement cmd) throws java.lang.reflect.InvocationTargetException, java.lang.InterruptedException
 	{
 		DataStore dataStore = cmd.getDataStore();
 		DataElement commandDescriptor = dataStore.findCommandDescriptor(DataStoreSchema.C_CANCEL);
 		if (commandDescriptor != null)
 		{
 			dataStore.command(commandDescriptor, cmd, false, true);
 		}	
 	}
 	
 	public void checkAllProjects(IProgressMonitor monitor) {
 		IWorkspace workspace = ResourcesPlugin.getWorkspace();
 		IWorkspaceRoot workspaceRoot = workspace.getRoot();
 
 		for (IProject project : workspaceRoot.getProjects()) {
 			// is the project open? if not, there's not much we can do
 			if (!project.isOpen())
 				continue;
 
 			// is this an RDT C/C++ project?
 			// check the project natures... we care about the project if it has
 			// both the remote nature and
 			// at least one of the CDT natures
 			try {
 				if (!project.hasNature(RemoteNature.REMOTE_NATURE_ID)
 						|| !(project.hasNature(CProjectNature.C_NATURE_ID)
 						|| project.hasNature(CCProjectNature.CC_NATURE_ID)))
 					continue;
 				
 				checkProject(project, monitor);
 			} catch (Throwable e) {
 				e.printStackTrace();
 			}
 		}
 	}
 		
 	public void checkProject(IProject project, IProgressMonitor monitor) {
 		if (project == null || fInitializedProjects.contains(project)) {
 			return;
 		}
 		try {
 			initializeScope(project, monitor);
 		} catch (CoreException e) {
 			RDTLog.logError(e);
 		}
 	}
 
 	private void initializeScope(IProject project, IProgressMonitor monitor) throws CoreException {
 		// get the service model configuration for this project
 		final ServiceModelManager serviceModelManager = ServiceModelManager.getInstance();
 		IServiceConfiguration config = serviceModelManager.getActiveConfiguration(project);
 
 		// is the indexing service associated with our service provider?
 		IService service = serviceModelManager.getService(RemoteCIndexServiceProvider.SERVICE_ID);
 		IServiceProvider provider = config.getServiceProvider(service);
 		if (provider.getId().equals(RemoteCIndexServiceProvider.ID)) {
 
 			// if so, initialize a scope for the project consisting of all
 			// its translation units
 			final List<ICElement> cElements = new LinkedList<ICElement>();
 
 			IResourceVisitor fileCollector = new IResourceVisitor() {
 
 				public boolean visit(IResource resource) throws CoreException {
 					if (resource instanceof IFile) {
 						// add the path
 						ITranslationUnit tu = CoreModelUtil.findTranslationUnit((IFile) resource);
 						if (tu != null) {
 							cElements.add(tu);
 							return false;
 						}
 					}
 					return true;
 				}
 			};
 
 			// collect the translation units
 			project.accept(fileCollector);
 
 			Scope scope = new Scope(project.getName());
 
 			// unregister the scope if there already is one
 			unregisterScope(scope, monitor);
 
 			// register the new scope
 			registerScope(scope, cElements, monitor);
 			
 			fInitializedProjects.add(project);
 		}
 	}
 	
 }
