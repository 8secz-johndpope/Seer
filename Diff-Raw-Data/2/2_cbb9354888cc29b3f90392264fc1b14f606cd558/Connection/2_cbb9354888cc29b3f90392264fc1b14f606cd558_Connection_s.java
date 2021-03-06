 package org.eclipse.team.internal.ccvs.core.connection;
 
 /*
  * (c) Copyright IBM Corp. 2000, 2002.
  * All Rights Reserved.
  */
  
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.team.internal.ccvs.core.CVSException;
 import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
 import org.eclipse.team.internal.ccvs.core.IServerConnection;
 import org.eclipse.team.internal.ccvs.core.Policy;
 
 /**
  * A connection to talk to a cvs server. The life cycle of a connection is
  * as follows:
  * <ul>
  *	<li> constructor: creates a new connection object that wraps the given
  *       repository location and connection method.
  *	<li> open: opens a connection.
  *	<li> send a request: use write* method or use the request stream directly.
  *	     <code>GetRequestStream</code> returns an output stream to directly
  *	     talk to the server.
  *	<li> read responses: use read* methods or use the response stream directly.
  *	     <code>GetResponseStream</code> returns an input stream to directly
  *	     read output from the server.
  *	<li> close: closes the connection. A closed connection can be reopened by
  *	     calling open again.
  * </ul>
  */
 public class Connection {
 	private static final byte NEWLINE= 0xA;
 	
 	private IServerConnection serverConnection;
 	private ICVSRepositoryLocation fCVSRoot;
 	private String fCVSRootDirectory;
 	private boolean fIsEstablished;
 	private InputStream fResponseStream;
 	private byte[] readLineBuffer = new byte[256];
 
 	public Connection(ICVSRepositoryLocation cvsroot, IServerConnection serverConnection) {
 		fCVSRoot = cvsroot;
 		this.serverConnection = serverConnection;
 	}
 	
 	private static byte[] append(byte[] buffer, int index, byte b) {
 		if (index >= buffer.length) {
 			byte[] newBuffer= new byte[index * 2];
 			System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
 			buffer= newBuffer;
 		}
 		buffer[index]= b;
 		return buffer;
 	}
 	/**
 	 * Closes the connection.
 	 */
 	public void close() throws CVSException {
 		if (!isEstablished())
 			return;
 		try {
 			serverConnection.close();
 		} catch (IOException ex) {
 			throw new CVSCommunicationException(Policy.bind("Connection.cannotClose"), ex);//$NON-NLS-1$
 		} finally {
 			fResponseStream = null;
 			fIsEstablished = false;
 		}
 	}
 	/**
 	 * Flushes the request stream.
 	 */
 	public void flush() throws CVSException {
 		if (!isEstablished())
 			return;
 		try {
 			getOutputStream().flush();	
 		} catch(IOException e) {
 			throw new CVSCommunicationException(e);
 		}
 	}
 	
 	/**
 	 * Returns the <code>OutputStream</code> used to send requests
 	 * to the server.
 	 */
 	public OutputStream getOutputStream() throws CVSException {
 		if (!isEstablished())
 			return null;
 		return serverConnection.getOutputStream();
 	}
 	/**
 	 * Returns the <code>InputStream</code> used to read responses from
 	 * the server.
 	 */
 	public InputStream getInputStream() throws CVSException {
 		if (!isEstablished())
 			return null;
 		if (fResponseStream == null)
 			fResponseStream = serverConnection.getInputStream();
 		return fResponseStream;	
 	}
 
 	/**
 	 * Returns <code>true</code> if the connection is established;
 	 * otherwise <code>false</code>.
 	 */
 	public boolean isEstablished() {
 		return fIsEstablished;
 	}
 
 	/**
 	 * Opens the connection.
 	 */	
 	public void open(IProgressMonitor monitor) throws CVSException {
 		if (isEstablished())
 			return;
 		try {
 			serverConnection.open(monitor);
 		} catch (IOException e) {
 			throw new CVSCommunicationException(e);
 		}
 		fIsEstablished= true;
 	}
 	/**
 	 * Reads a line from the response stream.
 	 */
 	public String readLine() throws CVSException {
 		if (!isEstablished())
 			throw new CVSCommunicationException(Policy.bind("Connection.readUnestablishedConnection"));//$NON-NLS-1$
 		try {
 			InputStream in = getInputStream();
 			int index = 0;
 			int r;
 			while ((r = in.read()) != -1) {
 				if (r == NEWLINE) break;
 				readLineBuffer = append(readLineBuffer, index++, (byte) r);
 			}
 			String result = new String(readLineBuffer, 0, index);
 			if (Policy.DEBUG_CVS_PROTOCOL) System.out.println(result);
 			return result;
 		} catch (IOException e) {
 			throw new CVSCommunicationException(e);
 		}
 	}
 	
 	static String readLine(InputStream in) throws IOException {
 		byte[] buffer = new byte[256];
 		int index = 0;
 		int r;
 		while ((r = in.read()) != -1) {
 			if (r == NEWLINE)
 				break;
 			buffer = append(buffer, index++, (byte) r);
 		}
 		String result = new String(buffer, 0, index);
 		if (Policy.DEBUG_CVS_PROTOCOL)
 			System.out.println(result);
 		return result;
 	}
 
 	//---- Helper to send strings to the server ----------------------------
 	
 	/**
 	 * Sends the given string to the server.
 	 */
 	public void write(String s) throws CVSException {
 		write(s.getBytes(), 0, s.length(), false);
 	}
 	/**
 	 * Sends the given bytes to the server.
 	 */
 	public void write(byte[] b, int off, int len) throws CVSException {
 		write(b, off, len, false);
 	}
 	/**
 	 * Sends the given string and a newline to the server. 
 	 */
 	public void writeLine(String s) throws CVSException {
 		write(s.getBytes(), 0, s.length(), true);
 	}
 	/**
 	 * Sends the given bytes and a newline to the server.
 	 */
 	public void writeLine(byte[] b, int off, int len) throws CVSException {
 		write(b, off, len, true);
 	}
 	/**
 	 * Low level method to write a string to the server. All write* methods are
 	 * funneled through this method.
 	 */
 	void write(byte[] b, int off, int len, boolean newline) throws CVSException {
 		if (!isEstablished())
 			throw new CVSCommunicationException(Policy.bind("Connection.writeUnestablishedConnection"));//$NON-NLS-1$
 			
 		if (Policy.DEBUG_CVS_PROTOCOL)
			System.out.print(new String(b) + (newline ? "\n" : ""));//$NON-NLS-1$ //$NON-NLS-2$ 
 	
 		try {
 			OutputStream out= getOutputStream();
 			out.write(b, off, len);
 			if (newline)
 				out.write(NEWLINE);
 			out.flush();
 			
 		} catch (IOException e) {
 			throw new CVSCommunicationException(e);
 		}
 	}
 }
