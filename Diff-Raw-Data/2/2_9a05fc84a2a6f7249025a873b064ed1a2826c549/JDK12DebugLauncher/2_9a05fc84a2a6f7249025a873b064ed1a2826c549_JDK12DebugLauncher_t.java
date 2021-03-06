 package org.eclipse.jdt.internal.debug.ui.launcher;
 
 /*
  * (c) Copyright IBM Corp. 2000, 2001.
  * All Rights Reserved.
  */
 
 import java.io.File;
 import java.io.IOException;
 import java.io.InterruptedIOException;
 import java.util.List;
 import java.util.Map;
 import java.util.Vector;
 
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.debug.core.DebugPlugin;
 import org.eclipse.debug.core.model.IDebugTarget;
 import org.eclipse.debug.core.model.IProcess;
 import org.eclipse.jdi.Bootstrap;
 import org.eclipse.jdt.debug.core.JDIDebugModel;
 import org.eclipse.jdt.launching.IVMInstall;
 import org.eclipse.jdt.launching.JavaRuntime;
 import org.eclipse.jdt.launching.VMRunnerConfiguration;
 import org.eclipse.jdt.launching.VMRunnerResult;
 import org.eclipse.jface.dialogs.ErrorDialog;
 
 import com.sun.jdi.VirtualMachine;
 import com.sun.jdi.connect.Connector;
 import com.sun.jdi.connect.IllegalConnectorArgumentsException;
 import com.sun.jdi.connect.ListeningConnector;
 
 /**
  * A launcher for running Java main classes. Uses JDI to launch a vm in debug 
  * mode.
  */
 public class JDK12DebugLauncher extends JDK12Launcher {
 
 
 	public interface IRetryQuery {
 		/**
 		 * Query the user to retry connecting to the VM.
 		 */
 		boolean queryRetry();
 	}
 
 	private IRetryQuery fRetryQuery;
 
 	/**
 	 * Creates a new launcher
 	 */
 	public JDK12DebugLauncher(IVMInstall vmInstance, IRetryQuery query) {
 		super(vmInstance);
 		setRetryQuery(query);
 	}
 
 	/**
 	 * @see IVMRunner#run(VMRunnerConfiguration)
 	 */
 	public VMRunnerResult run(VMRunnerConfiguration config) throws CoreException {
 		verifyVMInstall();
 		File workingDir = getWorkingDir(config);
 		int port= SocketUtil.findUnusedLocalPort("", 5000, 15000); //$NON-NLS-1$
 		if (port == -1) {
 			throw new CoreException(createStatus(LauncherMessages.getString("jdkLauncher.noPort"), null)); //$NON-NLS-1$
 		}
 		String location= getJDKLocation();
 		String program= location+File.separator+"bin"+File.separator+"java"; //$NON-NLS-2$ //$NON-NLS-1$
 		File javawexe= new File(program+"w.exe"); //$NON-NLS-1$
 		File javaw= new File(program+"w"); //$NON-NLS-1$
 		
 		if (javaw.isFile()) {
 			program= javaw.getAbsolutePath();
 		} else if (javawexe.isFile()) {
 			program= javawexe.getAbsolutePath();
 		}
 
 		Vector arguments= new Vector();
 
 		arguments.addElement(program);
 
 		String[] bootCP= config.getBootClassPath();
 		if (bootCP.length > 0) {
 			arguments.add("-Xbootclasspath:"+convertClassPath(bootCP)); //$NON-NLS-1$
 		} 
 		
 		String[] cp= config.getClassPath();
 		if (cp.length > 0) {
 			arguments.add("-classpath"); //$NON-NLS-1$
 			arguments.add(convertClassPath(cp));
 		}
 		addArguments(config.getVMArguments(), arguments);
 
 		arguments.add("-Xdebug"); //$NON-NLS-1$
 		arguments.add("-Xnoagent"); //$NON-NLS-1$
 		arguments.add("-Djava.compiler=NONE"); //$NON-NLS-1$
		arguments.add("-Xrunjdwp:transport=dt_socket,address=localhost:" + port); //$NON-NLS-1$
 
 		arguments.add(config.getClassToLaunch());
 		addArguments(config.getProgramArguments(), arguments);
 		String[] cmdLine= new String[arguments.size()];
 		arguments.copyInto(cmdLine);
 
 		ListeningConnector connector= getConnector();
 		if (connector == null) {
 			throw new CoreException(createStatus(LauncherMessages.getString("jdkLauncher.error.noConnector"), null)); //$NON-NLS-1$
 		}
 		Map map= connector.defaultArguments();
 		int timeout= fVMInstance.getDebuggerTimeout();
 		
 		specifyArguments(map, port, timeout);
 		Process p= null;
 		try {
 			try {
 				connector.startListening(map);
 
 				try {
 					p= Runtime.getRuntime().exec(cmdLine, null, workingDir);
 				} catch (IOException e) {
 					if (p != null) {
 						p.destroy();
 					}
 					throw new CoreException(createStatus(LauncherMessages.getString("jdkLauncher.error.title"), e)); //$NON-NLS-1$
 				}
 		
 				IProcess process= DebugPlugin.getDefault().newProcess(p, renderProcessLabel(cmdLine));
 				process.setAttribute(JavaRuntime.ATTR_CMDLINE, renderCommandLine(cmdLine));
 				
 				boolean retry= false;
 				do  {
 					try {
 						VirtualMachine vm= connector.accept(map);
 						setTimeout(vm);
 						IDebugTarget debugTarget= JDIDebugModel.newDebugTarget(vm, renderDebugTarget(config.getClassToLaunch(), port), process, true, false);
 						return new VMRunnerResult(debugTarget, new IProcess[] { process });
 					} catch (InterruptedIOException e) {
 						String errorMessage= process.getStreamsProxy().getErrorStreamMonitor().getContents();
 						if (errorMessage.length() == 0) {
 							errorMessage= process.getStreamsProxy().getOutputStreamMonitor().getContents();
 						}
 						if (errorMessage.length() != 0) {
 							reportError(errorMessage);
 						} else {
 							retry= getRetryQuery().queryRetry();
 						}
 					}
 				} while (retry);
 			} finally {
 				connector.stopListening(map);
 			}
 		} catch (IOException e) {
 			throw new CoreException(createStatus(LauncherMessages.getString("jdkLauncher.error.connect"), e)); //$NON-NLS-1$
 		} catch (IllegalConnectorArgumentsException e) {
 			throw new CoreException(createStatus(LauncherMessages.getString("jdkLauncher.error.connect"), e)); //$NON-NLS-1$
 		}
 		if (p != null)
 			p.destroy();
 		return null;
 	}
 	
 	private void reportError(final String errorMessage) {
 		StandardVM.getStandardDisplay().syncExec(new Runnable() {
 			public void run() {
 				IStatus s= new Status(IStatus.ERROR, DebugPlugin.getDefault().getDescriptor().getUniqueIdentifier(), 0, errorMessage, null);
 				ErrorDialog.openError(StandardVM.getStandardDisplay().getActiveShell(),LauncherMessages.getString("JDK12DebugLauncher.Launching_a_Java_VM_1"), LauncherMessages.getString("JDK12DebugLauncher.Problems_encountered_launching_the_Java_VM_in_debug_mode_2"), s); //$NON-NLS-1$ //$NON-NLS-2$
 			}
 			});
 	}
 	
 	private void setTimeout(VirtualMachine vm) {		
 		if (vm instanceof org.eclipse.jdi.VirtualMachine) {
 			int timeout= fVMInstance.getDebuggerTimeout();
 			org.eclipse.jdi.VirtualMachine vm2= (org.eclipse.jdi.VirtualMachine)vm;
 			vm2.setRequestTimeout(timeout);
 		}
 	}
 		
 	protected void specifyArguments(Map map, int portNumber, int timeout) {
 		// XXX: Revisit - allows us to put a quote (") around the classpath
 		Connector.IntegerArgument port= (Connector.IntegerArgument) map.get("port"); //$NON-NLS-1$
 		port.setValue(portNumber);
 		
 		Connector.IntegerArgument timeoutArg= (Connector.IntegerArgument) map.get("timeout"); //$NON-NLS-1$
 		// bug #5163
 		timeoutArg.setValue(20000);
 	}
 
 	protected ListeningConnector getConnector() {
 		List connectors= Bootstrap.virtualMachineManager().listeningConnectors();
 		for (int i= 0; i < connectors.size(); i++) {
 			ListeningConnector c= (ListeningConnector) connectors.get(i);
 			if ("com.sun.jdi.SocketListen".equals(c.name())) //$NON-NLS-1$
 				return c;
 		}
 		return null;
 	}
 
 	protected IRetryQuery getRetryQuery() {
 		return fRetryQuery;
 	}
 
 	protected void setRetryQuery(IRetryQuery retryQuery) {
 		fRetryQuery = retryQuery;
 	}
 }
