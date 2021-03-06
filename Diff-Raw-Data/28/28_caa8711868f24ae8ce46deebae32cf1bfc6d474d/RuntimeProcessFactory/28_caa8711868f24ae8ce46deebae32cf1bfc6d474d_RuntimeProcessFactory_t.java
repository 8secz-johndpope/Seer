 /*******************************************************************************
  * Copyright (c) 2011 IBM Corporation
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Otavio Busatto Pontes <obusatto@br.ibm.com> - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.linuxtools.tools.launch.core.factory;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.net.URI;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 import java.util.StringTokenizer;
 
 import org.eclipse.core.filesystem.IFileStore;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.linuxtools.profiling.launch.IRemoteCommandLauncher;
 import org.eclipse.linuxtools.profiling.launch.IRemoteFileProxy;
 import org.eclipse.linuxtools.profiling.launch.RemoteProxyManager;
 
 /*
  * Create process using Runtime.getRuntime().exec and prepends the
  * 'Linux tools path' project property to the environment PATH.
  * Use this factory instead of Runtime.getRuntime().exec if the command you
  * are running may be in the linux tools path selected in the project property
  * page.
  */
 public class RuntimeProcessFactory extends LinuxtoolsProcessFactory {
 	private static RuntimeProcessFactory instance = null;
 	private static final String WHICH_CMD = "which"; //$NON-NLS-1$
 
 	private String[] tokenizeCommand(String command) {
 		StringTokenizer tokenizer = new StringTokenizer(command);
 		String[] cmdarray = new String[tokenizer.countTokens()];
 		for (int i = 0; tokenizer.hasMoreElements(); i++)
 			cmdarray[i] = tokenizer.nextToken();
 
 		return cmdarray;
 	}
 
 	private String[] fillPathCommand(String[] cmdarray, IProject project) throws IOException {
 		cmdarray[0] = whichCommand(cmdarray[0], project);
 		return cmdarray;
 	}
 
 	/**
 	 * @deprecated
 	 *
 	 * Use {@link RuntimeProcessFactory#fillPathCommand(String[], IProject)} instead.
 	 */
 	@Deprecated
 	private String[] fillPathCommand(String[] cmdarray, String[] envp) throws IOException {
 		cmdarray[0] = whichCommand(cmdarray[0], envp);
 		return cmdarray;
 	}
 	
 	/**
 	 * @deprecated
 	 *
 	 * Use {@link RuntimeProcessFactory#fillPathSudoCommand(String[], IProject)} instead.
 	 */
 	@Deprecated
 	private String[] fillPathSudoCommand(String[] cmdarray, String[] envp) throws IOException {
 		cmdarray[2] = whichCommand(cmdarray[2], envp);
 		return cmdarray;
 	}
 
 	private String[] fillPathSudoCommand(String[] cmdarray, IProject project) throws IOException {
 		cmdarray[1] = whichCommand(cmdarray[1], project);
 		return cmdarray;
 	}
 
 	/**
 	 * @deprecated
 	 *
 	 * Use {@link RuntimeProcessFactory#whichCommand(String, IProject)} instead.
 	 */
 	@Deprecated
 	public String whichCommand(String command, String[] envp) throws IOException {
 		Process p = Runtime.getRuntime().exec(new String[] {WHICH_CMD, command}, envp);
 		try {
 			if (p.waitFor() == 0) {
 				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
 				command = reader.readLine();
 			} else {
 				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
 				throw new IOException(reader.readLine());
 			}
 		} catch (InterruptedException e) {
 			e.printStackTrace();
 		}
 		return command;
 	}
 
 	/**
 	 * @since 1.1
 	 */
 	public String whichCommand(String command, IProject project) throws IOException {
 		if (project != null) {
 			String[] envp = updateEnvironment(null, project);
 			try {
 				IRemoteFileProxy proxy = RemoteProxyManager.getInstance().getFileProxy(project);
 				URI whichUri = URI.create(WHICH_CMD);
 				IPath whichPath = new Path(proxy.toPath(whichUri));
 				IRemoteCommandLauncher launcher = RemoteProxyManager.getInstance().getLauncher(project);
 				Process pProxy = launcher.execute(whichPath, new String[]{command}, envp, null, new NullProgressMonitor());
 				if (pProxy != null){
 					BufferedReader error = new BufferedReader(new InputStreamReader(pProxy.getErrorStream()));
					BufferedReader reader = new BufferedReader(new InputStreamReader(pProxy.getInputStream()));
 					if(error.readLine() != null){
 						throw new IOException(error.readLine());
 					}
					error.close();
 					String readLine = reader.readLine();
					ArrayList<String> lines = new ArrayList<String>();
					while (readLine != null) {
						lines.add(readLine);
						readLine = reader.readLine();
					}
					reader.close();
					if (project.getLocationURI()!=null) {
						if(project.getLocationURI().toString().startsWith("rse:")) { //$NON-NLS-1$
							// RSE output
							command = lines.get(lines.size()-2);
						} else {
							// Remotetools output
							command = lines.get(0);
						}
					} else {
						// Local output
						command = lines.get(0);
					}
 				}
 			} catch (CoreException e) {
 				e.printStackTrace();
			} catch (IndexOutOfBoundsException e) {
				// Executable cannot be found in system path.
				e.printStackTrace();
 			}
 		}
 		return command;
 	}
 
 	public static RuntimeProcessFactory getFactory() {
 		if (instance == null)
 			instance = new RuntimeProcessFactory();
 		return instance;
 	}
 
 	public Process exec(String cmd, IProject project) throws IOException {
 		return exec(cmd, null, (IFileStore)null, project);
 	}
 
 	public Process exec(String[] cmdarray, IProject project) throws IOException {
 		return exec(cmdarray, null, project);
 	}
 
 	public Process exec(String[] cmdarray, String[] envp, IProject project) throws IOException {
 		return exec(cmdarray, envp, (IFileStore)null, project);
 	}
 
 	public Process exec(String cmd, String[] envp, IProject project) throws IOException {
 		return exec(cmd, envp, (IFileStore)null, project);
 	}
 
 	/**
 	 * @deprecated
 	 *
 	 * Use {@link RuntimeProcessFactory#exec(String, String[], IFileStore, IProject)} instead.
 	 */
 	@Deprecated
 	public Process exec(String cmd, String[] envp, File dir, IProject project)
 		throws IOException {
 		return exec(tokenizeCommand(cmd), envp, dir, project);
 	}
 
 	/**
 	 * @since 1.1
 	 */
 	public Process exec(String cmd, String[] envp, IFileStore dir, IProject project)
 		throws IOException {
 		return exec(tokenizeCommand(cmd), envp, dir, project);
 	}
 
 	/**
 	 * @deprecated
 	 *
 	 * Use {@link RuntimeProcessFactory#exec(String[], String[], IFileStore, IProject)} instead.
 	 */
 	@Deprecated
 	public Process exec(String cmdarray[], String[] envp, File dir, IProject project)
 		throws IOException {
 		envp = updateEnvironment(envp, project);
 		cmdarray = fillPathCommand(cmdarray, envp);
 
 		return Runtime.getRuntime().exec(cmdarray, envp, dir);
 	}
 
 	/**
 	 * @since 1.1
 	 */
 	public Process exec(String cmdarray[], String[] envp, IFileStore dir, IProject project)
 		throws IOException {
 
 		Process p = null;
 		try {
 			cmdarray = fillPathCommand(cmdarray, project);
 
 			String command = cmdarray[0];
 			URI uri = URI.create(command);
 
 			IPath changeToDir = null;
 			IPath path;
 			IRemoteCommandLauncher launcher;
 
 			if (project != null) {
 				IRemoteFileProxy proxy = RemoteProxyManager.getInstance().getFileProxy(project);
 				path = new Path(proxy.toPath(uri));
 				launcher = RemoteProxyManager.getInstance().getLauncher(project);
 				envp = updateEnvironment(envp, project);
 				if (dir != null)
 					changeToDir = new Path(proxy.toPath(dir.toURI()));
 			} else {
 				path = new Path(uri.getPath());
 				launcher = RemoteProxyManager.getInstance().getLauncher(uri);
 				if (dir != null)
 					changeToDir = new Path(dir.toURI().getPath());
 			}
 
 
 			List<String> cmdlist = new ArrayList<String>(Arrays.asList(cmdarray));
 			cmdlist.remove(0);
 			cmdlist.toArray(cmdarray);
 			cmdarray = cmdlist.toArray(new String[0]);
 
 			p = launcher.execute(path, cmdarray, envp, changeToDir , new NullProgressMonitor());
 		} catch (CoreException e) {
 			e.printStackTrace();
 		}
 
 		return p;
 	}
 	
 	public Process sudoExec(String cmd, IProject project) throws IOException {
 		return sudoExec(cmd, null, (IFileStore)null, project);
 	}
 	
 	public Process sudoExec(String cmd, String[] envp, IProject project) throws IOException {
 		return exec(cmd, envp, (IFileStore)null, project);
 	}
 
 	/**
 	 * @deprecated
 	 *
 	 * Use {@link RuntimeProcessFactory#sudoExec(String, String[], IFileStore, IProject)} instead.
 	 */
 	@Deprecated
 	public Process sudoExec(String cmd, String[] envp, File dir, IProject project)
 			throws IOException {
 			return sudoExec(tokenizeCommand(cmd), envp, dir, project);
 	}
 
 	/**
 	 * @since 1.1
 	 */
 	public Process sudoExec(String cmd, String[] envp, IFileStore dir, IProject project)
 			throws IOException {
 			return sudoExec(tokenizeCommand(cmd), envp, dir, project);
 	}
 	
 	public Process sudoExec(String[] cmdarray, IProject project) throws IOException {
 		return sudoExec(cmdarray, null, project);
 	}
 	
 	public Process sudoExec(String[] cmdarray, String[] envp, IProject project) throws IOException {
 		return sudoExec(cmdarray, envp, (IFileStore)null, project);
 	}
 
 	/**
 	 * @deprecated
 	 *
 	 * Use {@link RuntimeProcessFactory#sudoExec(String[], String[], IFileStore, IProject)} instead.
 	 */
 	@Deprecated
 	public Process sudoExec(String[] cmdarray, String[] envp, File dir, IProject project) throws IOException {
 		List<String> cmdList = Arrays.asList(cmdarray);
 		ArrayList<String> cmdArrayList = new ArrayList<String>(cmdList);
 		cmdArrayList.add(0, "sudo"); //$NON-NLS-1$
 		cmdArrayList.add(1, "-n"); //$NON-NLS-1$
 		
 		String[] cmdArraySudo = new String[cmdArrayList.size()];
 		cmdArrayList.toArray(cmdArraySudo);
 		
 		envp = updateEnvironment(envp, project);
 		cmdArraySudo = fillPathSudoCommand(cmdArraySudo, envp);
 
 		return Runtime.getRuntime().exec(cmdArraySudo, envp, dir);
 	}
 
 	/**
 	 * @since 1.1
 	 */
 	public Process sudoExec(String[] cmdarray, String[] envp, IFileStore dir, IProject project) throws IOException {
 		URI uri = URI.create("sudo"); //$NON-NLS-1$
 
 		List<String> cmdList = Arrays.asList(cmdarray);
 		ArrayList<String> cmdArrayList = new ArrayList<String>(cmdList);
 		cmdArrayList.add(0, "-n"); //$NON-NLS-1$
 
 		String[] cmdArraySudo = new String[cmdArrayList.size()];
 		cmdArrayList.toArray(cmdArraySudo);
 
 		Process p = null;
 		try {
 			cmdArraySudo = fillPathSudoCommand(cmdArraySudo, project);
 			IRemoteCommandLauncher launcher;
 			IPath changeToDir = null, path;
 			if (project != null) {
 				IRemoteFileProxy proxy = RemoteProxyManager.getInstance().getFileProxy(project);
 				path = new Path(proxy.toPath(uri));
 				launcher = RemoteProxyManager.getInstance().getLauncher(project);
 				envp = updateEnvironment(envp, project);
 
 				if (dir != null)
 					changeToDir = new Path(proxy.toPath(dir.toURI()));
 			} else {
 				launcher = RemoteProxyManager.getInstance().getLauncher(uri);
 				path = new Path(uri.getPath());
 				if (dir != null)
 					changeToDir = new Path(dir.toURI().getPath());
 			}
 
 			List<String> cmdlist = new ArrayList<String>(Arrays.asList(cmdArraySudo));
 			cmdlist.remove(0);
 			cmdlist.toArray(cmdArraySudo);
 			cmdArraySudo = cmdlist.toArray(new String[0]);
 
 			p = launcher.execute(path, cmdArraySudo, envp, changeToDir , new NullProgressMonitor());
 		} catch (CoreException e) {
 			e.printStackTrace();
 		}
 
 		return p;
 
 	}
 }
