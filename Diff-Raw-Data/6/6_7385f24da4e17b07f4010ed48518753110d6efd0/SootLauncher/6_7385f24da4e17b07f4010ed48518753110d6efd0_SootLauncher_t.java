 /* Soot - a J*va Optimization Framework
  * Copyright (C) 2003 Jennifer Lhotak
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the
  * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
  * Boston, MA 02111-1307, USA.
  */
 
 package ca.mcgill.sable.soot.launching;
 
 import org.eclipse.ui.*;
 import org.eclipse.jface.dialogs.*;
 import org.eclipse.jface.operation.IRunnableWithProgress;
 import org.eclipse.jface.operation.ModalContext;
 import org.eclipse.jface.viewers.*;
 import org.eclipse.jface.action.*;
 import org.eclipse.core.runtime.*;
 import java.lang.reflect.InvocationTargetException;
 import ca.mcgill.sable.soot.*;
 import org.eclipse.swt.widgets.*;
 
 
 import java.util.*;
 
 /**
  * Main Soot Launcher. Handles running Soot directly (or as a 
  * process) 
  */
 public class SootLauncher  implements IWorkbenchWindowActionDelegate {
 	
 	private IWorkbenchPart part;
  	protected IWorkbenchWindow window;
 	private ISelection selection;
 	private IStructuredSelection structured;
 	protected String platform_location;
 	protected String external_jars_location;
 	public SootClasspath sootClasspath = new SootClasspath();
 	public SootSelection sootSelection;
 	//private IFolder sootOutputFolder;
 	private SootCommandList sootCommandList;
 	private String outputLocation;
 	private SootDefaultCommands sdc;
 	private SootOutputFilesHandler fileHandler;
 	private DavaHandler davaHandler;
 	
 	public void run(IAction action) {
 		
 		setSootSelection(new SootSelection(structured));		
  		getSootSelection().initialize(); 		
 		setFileHandler(new SootOutputFilesHandler(window));
 		getFileHandler().resetSootOutputFolder(getSootSelection().getProject());		
 		//System.out.println("starting SootLauncher");
 		setDavaHandler(new DavaHandler());
 		getDavaHandler().setSootOutputFolder(getFileHandler().getSootOutputFolder());
 		getDavaHandler().handleBefore();
 		initPaths();
 		initCommandList();
 
 		
 	}
 	
 	
 	private void initCommandList() {
 		setSootCommandList(new SootCommandList());
 	}
 	
 	protected void runSootDirectly(){
 		runSootDirectly("soot.Main");
 	}
 	
 	private void sendSootOutputEvent(String toSend){
 		SootOutputEvent send = new SootOutputEvent(this, ISootOutputEventConstants.SOOT_NEW_TEXT_EVENT);
 		send.setTextToAppend(toSend);
 		final SootOutputEvent sendFinal = send;
 		
 		Display.getCurrent().asyncExec(new Runnable(){
 			public void run() {
 				SootPlugin.getDefault().fireSootOutputEvent(sendFinal);
 			};
 		});
 	}
 	
 	protected void runSootDirectly(String mainClass) {
 		
 		int length = getSootCommandList().getList().size();
 		//Object [] temp = getSootCommandList().getList().toArray();
 		String temp [] = new String [length];
 		
 		getSootCommandList().getList().toArray(temp);
 		
 		sendSootOutputEvent(mainClass);
 		sendSootOutputEvent(" ");
 		
 		final String [] cmdAsArray = temp;
 		
 		for (int i = 0; i < temp.length; i++) {
 			
 			//System.out.println(temp[i]);
 			sendSootOutputEvent(temp[i]);
 			sendSootOutputEvent(" ");
 		}
 		sendSootOutputEvent("\n");
 		
 		
 		//System.out.println("about to make list be array of strings");
 		//final String [] cmdAsArray = (String []) temp;
 		IRunnableWithProgress op; 
 		try {   
         	newProcessStarting();
             op = new SootRunner(temp, Display.getCurrent(), mainClass);
             ModalContext.run(op, true, new NullProgressMonitor(), Display.getCurrent());
             
  		} 
  		catch (InvocationTargetException e1) {
     		// handle exception
     		System.out.println("InvocationTargetException: "+e1.getMessage());
  		} 
  		catch (InterruptedException e2) {
     		// handle cancelation
     		System.out.println("InterruptedException: "+e2.getMessage());
     		//op.getProc().destroy();
  		}
 
 	}
 	
 	protected void runSootAsProcess(String cmd) {
 		
         SootProcessRunner op;    
         try {   
         	newProcessStarting();
             op = new SootProcessRunner(Display.getCurrent(), cmd, sootClasspath);
 
             if (window == null) {
             	window = SootPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
             }
             new  ProgressMonitorDialog(window.getShell()).run(true, true, op);
  			
  			
  		} 
  		catch (InvocationTargetException e1) {
     		// handle exception
  		} 
  		catch (InterruptedException e2) {
     		// handle cancelation
     		System.out.println(e2.getMessage());
     		//op.getProc().destroy();
  		}
  
    
         
 	}
 	private void newProcessStarting() {
 		SootOutputEvent clear_event = new SootOutputEvent(this, ISootOutputEventConstants.SOOT_CLEAR_EVENT);
         SootPlugin.getDefault().fireSootOutputEvent(clear_event);    
         SootOutputEvent se = new SootOutputEvent(this, ISootOutputEventConstants.SOOT_NEW_TEXT_EVENT);
         se.setTextToAppend("Starting ...");
         SootPlugin.getDefault().fireSootOutputEvent(se);
 	}
 			
 	private String[] formCmdLine(String cmd) {
 	 
 	  	
 	   	StringTokenizer st = new StringTokenizer(cmd);
 	  	int count = st.countTokens();
         String[] cmdLine = new String[count];        
         count = 0;
         
         while(st.hasMoreTokens()) {
             cmdLine[count++] = st.nextToken();
             //System.out.println(cmdLine[count-1]); 
         }
         
         return cmdLine; 
 	}
 	
 	private void initPaths() {
 		
 		sootClasspath.initialize();
 		
 		// platform location 
		//platform_location = Platform.getLocation().toOSString();
		platform_location = getSootSelection().getJavaProject().getProject().getLocation().toOSString();
		System.out.println("platform_location: "+platform_location);
		platform_location = platform_location.substring(0, platform_location.lastIndexOf(System.getProperty("file.separator")));
        System.out.println("platform_location: "+platform_location);
 		// external jars location - may need to change don't think I use this anymore
 		external_jars_location = Platform.getLocation().removeLastSegments(2).toOSString();
 		setOutputLocation(platform_location+getFileHandler().getSootOutputFolder().getFullPath().toOSString());
 		
 	}
 	
 	public void runFinish() {
 		getFileHandler().refreshFolder();
 		//for updating markers
 		SootPlugin.getDefault().getManager().updateSootRanFlag();
 		//getDavaHandler().handleAfter();
 		//getFileHandler().handleFilesChanged();
 		
 	}
 	
 
 	public IStructuredSelection getStructured() {
 		return structured;
 	}
 		
 	private void setStructured(IStructuredSelection struct) {
 		structured = struct;
 	}
 	
 	public void init(IWorkbenchWindow window) {
  		this.window = window;
  	}
  	
  	public void dispose() {
  	}
  	
  	public void selectionChanged(IAction action, ISelection selection) {
 		this.selection = selection;	
 		
 		if (selection instanceof IStructuredSelection){
 			setStructured((IStructuredSelection)selection);
 		}
 	}
 	
 	/**
 	 * Returns the sootClasspath.
 	 * @return SootClasspath
 	 */
 	public SootClasspath getSootClasspath() {
 		return sootClasspath;
 	}
 
 	/**
 	 * Sets the sootClasspath.
 	 * @param sootClasspath The sootClasspath to set
 	 */
 	public void setSootClasspath(SootClasspath sootClasspath) {
 		this.sootClasspath = sootClasspath;
 	}
 
 	/**
 	 * Returns the sootSelection.
 	 * @return SootSelection
 	 */
 	public SootSelection getSootSelection() {
 		return sootSelection;
 	}
 
 	/**
 	 * Sets the sootSelection.
 	 * @param sootSelection The sootSelection to set
 	 */
 	public void setSootSelection(SootSelection sootSelection) {
 		this.sootSelection = sootSelection;
 	}
 
 	
 
 	/**
 	 * Returns the window.
 	 * @return IWorkbenchWindow
 	 */
 	public IWorkbenchWindow getWindow() {
 		return window;
 	}
 
 	/**
 	 * Returns the sootCommandList.
 	 * @return SootCommandList
 	 */
 	public SootCommandList getSootCommandList() {
 		return sootCommandList;
 	}
 
 	/**
 	 * Sets the sootCommandList.
 	 * @param sootCommandList The sootCommandList to set
 	 */
 	public void setSootCommandList(SootCommandList sootCommandList) {
 		this.sootCommandList = sootCommandList;
 	}
 
 	/**
 	 * Returns the output_location.
 	 * @return String
 	 */
 	public String getOutputLocation() {
 		return outputLocation;
 	}
 
 	/**
 	 * Sets the output_location.
 	 * @param output_location The output_location to set
 	 */
 	public void setOutputLocation(String outputLocation) {
 		this.outputLocation = outputLocation;
 	}
 
 	/**
 	 * Returns the sdc.
 	 * @return SootDefaultCommands
 	 */
 	public SootDefaultCommands getSdc() {
 		return sdc;
 	}
 
 	/**
 	 * Sets the sdc.
 	 * @param sdc The sdc to set
 	 */
 	public void setSdc(SootDefaultCommands sdc) {
 		this.sdc = sdc;
 	}
 
 	/**
 	 * Returns the fileHandler.
 	 * @return SootOutputFilesHandler
 	 */
 	public SootOutputFilesHandler getFileHandler() {
 		return fileHandler;
 	}
 
 	/**
 	 * Sets the fileHandler.
 	 * @param fileHandler The fileHandler to set
 	 */
 	public void setFileHandler(SootOutputFilesHandler fileHandler) {
 		this.fileHandler = fileHandler;
 	}
 
 	/**
 	 * @return
 	 */
 	public DavaHandler getDavaHandler() {
 		return davaHandler;
 	}
 
 	/**
 	 * @param handler
 	 */
 	public void setDavaHandler(DavaHandler handler) {
 		davaHandler = handler;
 	}
 
 }
