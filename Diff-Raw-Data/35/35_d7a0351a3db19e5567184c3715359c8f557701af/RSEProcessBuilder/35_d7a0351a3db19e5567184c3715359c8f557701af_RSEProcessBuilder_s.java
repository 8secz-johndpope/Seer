 /*******************************************************************************
  * Copyright (c) 2007 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  * IBM Corporation - Initial API and implementation
  *******************************************************************************/
 package org.eclipse.ptp.remote.rse;
 
 import java.io.IOException;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.ptp.remote.AbstractRemoteProcessBuilder;
 import org.eclipse.ptp.remote.IRemoteConnection;
 import org.eclipse.ptp.remote.IRemoteProcess;
 
 public class RSEProcessBuilder extends AbstractRemoteProcessBuilder {
 	private final static 	String EXIT_CMD = "exit"; //$NON-NLS-1$
 	private final static 	String CMD_DELIMITER = ";"; //$NON-NLS-1$
 	
 	private RSEConnection connection;
 	private Map<String, String> remoteEnv = new HashMap<String, String>();
 
 	public RSEProcessBuilder(IRemoteConnection conn, List<String> command) {
 		super(conn, command);
		remoteEnv.putAll(System.getenv());
 		this.connection = (RSEConnection)conn;
 	}
 
 	public RSEProcessBuilder(IRemoteConnection conn, String... command) {
 		this(conn, Arrays.asList(command));
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.remote.AbstractRemoteProcessBuilder#environment()
 	 */
 	@Override
 	public Map<String, String> environment() {
 		return remoteEnv;
 	}
 
 	/* (non-Javadoc)
 	 * @see org.eclipse.ptp.remote.IRemoteProcessBuilder#start()
 	 */
 	public IRemoteProcess start() throws IOException {
 		// The exit command is called to force the remote shell to close after our command 
 		// is executed. This is to prevent a running process at the end of the debug session.
 		// See Bug 158786.
 		List<String> cmdArgs = command();
 		if (cmdArgs.size() < 1) {
 			throw new IndexOutOfBoundsException();
 		}
 		
 		String remoteCmd = "";  //$NON-NLS-1$
 		
 		for (int i = 0; i < cmdArgs.size(); i++) {
 			if (i > 0) {
 				remoteCmd += " ";  //$NON-NLS-1$
 			}
 			remoteCmd += spaceEscapify(cmdArgs.get(i));
 		}
 		
 		remoteCmd += CMD_DELIMITER + EXIT_CMD;
 		
 		IShellService shellService = connection.getRemoteShellService();
 		if (shellService == null) {
 			throw new IOException("Remote service not found");
 		}
 		
 		// This is necessary because runCommand does not actually run the command right now.
 		String env[] = new String[0];
 		IHostShell hostShell = null;
 		try {
 			hostShell = shellService.launchShell("", env,new NullProgressMonitor());  //$NON-NLS-1$
 		} catch (SystemMessageException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 			return null;
 		}
 		hostShell.writeToShell(remoteCmd);
 		
 		Process p = new HostShellProcessAdapter(hostShell);
 		return new RSEProcess(p);
 	}
 	
 	private String spaceEscapify(String inputString) {
 		if(inputString == null)
 			return null;
 		return inputString.replaceAll(" ", "\\\\ "); //$NON-NLS-1$ //$NON-NLS-2$
 	}
 
 }
