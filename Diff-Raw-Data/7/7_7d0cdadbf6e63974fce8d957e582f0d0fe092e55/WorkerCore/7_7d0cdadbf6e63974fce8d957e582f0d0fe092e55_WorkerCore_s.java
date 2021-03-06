 /*
  * Copyright (C) 2010-2011 The University of Manchester
  * 
  * See the file "LICENSE.txt" for license terms.
  */
 package org.taverna.server.localworker.impl;
 
 import static java.io.File.createTempFile;
 import static java.lang.Boolean.parseBoolean;
 import static java.lang.System.out;
 import static org.apache.commons.io.IOUtils.copy;
 import static org.taverna.server.localworker.impl.LocalWorker.PASSWORD_FILE;
 import static org.taverna.server.localworker.impl.LocalWorker.SYSTEM_ENCODING;
 import static org.taverna.server.localworker.remote.RemoteStatus.Finished;
 import static org.taverna.server.localworker.remote.RemoteStatus.Initialized;
 import static org.taverna.server.localworker.remote.RemoteStatus.Operating;
 
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStreamWriter;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 import java.io.Writer;
 import java.rmi.RemoteException;
 import java.rmi.server.UnicastRemoteObject;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.Map;
 
 import javax.xml.bind.JAXBContext;
 import javax.xml.bind.JAXBException;
 import javax.xml.datatype.DatatypeConfigurationException;
 
 import org.ogf.usage.JobUsageRecord;
 import org.taverna.server.localworker.remote.RemoteListener;
 import org.taverna.server.localworker.remote.RemoteStatus;
 
 /**
  * The core class that connects to a Taverna command-line workflow execution
  * engine. This implementation always registers a single listener, &lquo;
  * <tt>io</tt> &rquo;, with two properties representing the stdout and stderr of
  * the run and one representing the exit code. The listener is
  * remote-accessible. It does not support attaching any other listeners.
  * 
  * @author Donal Fellows
  */
 public class WorkerCore extends UnicastRemoteObject implements Worker,
 		RemoteListener {
 	/**
 	 * The name of the standard listener, which is installed by default.
 	 */
 	public static final String DEFAULT_LISTENER_NAME = "io";
 
 	static final Map<String, Property> pmap = new HashMap<String, Property>();
 
 	enum Property {
 		STDOUT("stdout"), STDERR("stderr"), EXIT_CODE("exitcode"), READY_TO_NOTIFY(
 				"readyToNotify"), EMAIL("notificationAddress"), USAGE(
 				"usageRecord");
 
 		private String s;
 
 		private Property(String s) {
 			this.s = s;
 			pmap.put(s, this);
 		}
 
 		@Override
 		public String toString() {
 			return s;
 		}
 
 		public static Property is(String s) {
 			return pmap.get(s);
 		}
 
 		public static String[] names() {
 			return pmap.keySet().toArray(new String[pmap.size()]);
 		}
 	}
 
 	Process subprocess;
 	private boolean finished;
 	StringWriter stdout;
 	StringWriter stderr;
 	Integer exitCode;
 	boolean readyToSendEmail;
 	String emailAddress;
 	JobUsageRecord ur;
 
 	private Date start;
 
 	/**
 	 * @throws RemoteException
 	 */
 	public WorkerCore() throws RemoteException {
 		super();
 		stdout = new StringWriter();
 		stderr = new StringWriter();
 	}
 
 	/**
 	 * An engine for asynchronously copying from an {@link InputStream} to a
 	 * {@link Writer}.
 	 * 
 	 * @author Donal Fellows
 	 */
 	private static class AsyncCopy extends Thread {
 		private InputStream from;
 		private Writer to;
 
 		AsyncCopy(InputStream from, Writer to) {
 			this.from = from;
 			this.to = to;
 			setDaemon(true);
 			start();
 		}
 
 		@Override
 		public void run() {
 			try {
 				copy(from, to);
 			} catch (IOException e) {
 			}
 		}
 	}
 
 	/**
 	 * Fire up the workflow. This causes a transition into the operating state.
 	 * 
 	 * @param executeWorkflowCommand
 	 *            The command to run to execute the workflow.
 	 * @param workflow
 	 *            The workflow document to execute.
 	 * @param workingDir
 	 *            What directory to use as the working directory.
 	 * @param inputBaclava
 	 *            The baclava file to use for inputs, or <tt>null</tt> to use
 	 *            the other <b>input*</b> arguments' values.
 	 * @param inputFiles
 	 *            A mapping of input names to files that supply them. Note that
 	 *            we assume that nothing mapped here will be mapped in
 	 *            <b>inputValues</b>.
 	 * @param inputValues
 	 *            A mapping of input names to values to supply to them. Note
 	 *            that we assume that nothing mapped here will be mapped in
 	 *            <b>inputFiles</b>.
 	 * @param outputBaclava
 	 *            What baclava file to write the output from the workflow into,
 	 *            or <tt>null</tt> to have it written into the <tt>out</tt>
 	 *            subdirectory.
 	 * @throws IOException
 	 *             If any of quite a large number of things goes wrong.
 	 */
 	@Override
 	public void initWorker(String executeWorkflowCommand, String workflow,
 			File workingDir, File inputBaclava, Map<String, File> inputFiles,
 			Map<String, String> inputValues, File outputBaclava,
 			File securityDir, char[] password) throws IOException {
 		// How we execute the workflow in a subprocess
 		ProcessBuilder pb = new ProcessBuilder()
 				.command(executeWorkflowCommand);
 
 		if (securityDir != null) {
 			pb.command().add("-cmdir");
 			pb.command().add(securityDir.getAbsolutePath());
 		}
 		if (password != null) {
 			PrintWriter pw = null;
 			try {
 				pw = new PrintWriter(new OutputStreamWriter(
 						new FileOutputStream(new File(securityDir,
 								PASSWORD_FILE)), SYSTEM_ENCODING));
 				pw.println(password);
 			} finally {
 				if (pw != null)
 					pw.close();
 			}
 		}
 
 		// Add arguments denoting inputs
 		if (inputBaclava != null) {
 			pb.command().add("-inputdoc");
 			pb.command().add(inputBaclava.getAbsolutePath());
 			if (!inputBaclava.exists())
 				throw new IOException("input baclava file doesn't exist");
 		} else {
 			for (String port : inputFiles.keySet()) {
 				File f = inputFiles.get(port);
 				if (f != null) {
 					pb.command().add("-inputfile");
 					pb.command().add(port);
 					pb.command().add(f.getAbsolutePath());
 					if (!f.exists())
 						throw new IOException("input file for port \"" + port
 								+ "\" doesn't exist");
 				}
 			}
 			for (String port : inputValues.keySet()) {
 				String v = inputValues.get(port);
 				if (v != null) {
 					pb.command().add("-inputvalue");
 					pb.command().add(port);
 					pb.command().add(v);
 				}
 			}
 		}
 
 		// Add arguments denoting outputs
 		if (outputBaclava != null) {
 			pb.command().add("-outputdoc");
 			pb.command().add(outputBaclava.getAbsolutePath());
 			if (!outputBaclava.getParentFile().exists())
 				throw new IOException(
 						"parent directory of output baclava file does not exist");
 			if (outputBaclava.exists())
 				throw new IOException("output baclava file exists");
 		} else {
 			File out = new File(workingDir, "out");
 			if (!out.mkdir()) {
 				throw new IOException("failed to make output directory \"out\"");
 			}
 			out.delete(); // Taverna needs the dir to *not* exist now
 			pb.command().add("-outputdir");
 			pb.command().add(out.getAbsolutePath());
 		}
 
 		// Add an argument holding the workflow
 		File tmp = createTempFile("taverna", ".t2flow");
 		FileWriter w = new FileWriter(tmp);
 		w.write(workflow);
 		w.close();
 		tmp.deleteOnExit();
 		pb.command().add(tmp.getAbsolutePath());
 
 		// Indicate what working directory to use
 		pb.directory(workingDir);
 
 		// Start the subprocess
 		out.println("starting " + pb.command() + " in directory " + workingDir);
 		subprocess = pb.start();
 		if (subprocess == null)
 			throw new IOException("unknown failure creating process");
 		start = new Date();
 
 		// Capture its stdout and stderr
 		new AsyncCopy(subprocess.getInputStream(), stdout);
 		new AsyncCopy(subprocess.getErrorStream(), stderr);
 	}
 
 	/**
 	 * Kills off the subprocess if it exists and is alive.
 	 */
 	@Override
 	public void killWorker() {
 		if (!finished && subprocess != null) {
 			int code;
 			try {
 				// Check if the workflow terminated of its own accord
 				code = subprocess.exitValue();
 				buildUR(code == 0 ? "Completed" : "Failed");
 			} catch (IllegalThreadStateException e) {
 				subprocess.destroy();
 				try {
 					code = subprocess.waitFor();
 				} catch (InterruptedException e1) {
 					e1.printStackTrace(out); // not expected
 					return;
 				}
 				buildUR(code == 0 ? "Completed" : "Aborted");
 			}
 			finished = true;
 			exitCode = code;
 			readyToSendEmail = true;
 			if (code > 128) {
 				out.println("workflow aborted, signal=" + (code - 128));
 			} else {
 				out.println("workflow exited, code=" + code);
 			}
 		}
 	}
 
 	private void buildUR(String status) {
 		try {
 			Date now = new Date();
 			ur = new JobUsageRecord();
 			ur.addUser(System.getProperty("user.name"), null);
 			ur.addStartAndEnd(start, now);
 			ur.addWallDuration(now.getTime() - start.getTime());
 			ur.setStatus(status);
 			// TODO: Push back to webapp side
 		} catch (DatatypeConfigurationException e) {
 			e.printStackTrace();
 		}
 	}
 
 	/**
 	 * Move the worker out of the stopped state and back to operating.
 	 * 
 	 * @throws Exception
 	 *             if it fails (which it always does; operation currently
 	 *             unsupported).
 	 */
 	@Override
 	public void startWorker() throws Exception {
 		throw new Exception("starting unsupported");
 	}
 
 	/**
 	 * Move the worker into the stopped state from the operating state.
 	 * 
 	 * @throws Exception
 	 *             if it fails (which it always does; operation currently
 	 *             unsupported).
 	 */
 	@Override
 	public void stopWorker() throws Exception {
 		throw new Exception("stopping unsupported");
 	}
 
 	/**
 	 * @return The status of the workflow run. Note that this can be an
 	 *         expensive operation.
 	 */
 	@Override
 	public RemoteStatus getWorkerStatus() {
 		if (subprocess == null)
 			return Initialized;
 		if (finished)
 			return Finished;
 		try {
 			exitCode = subprocess.exitValue();
 			finished = true;
 			readyToSendEmail = true;
 			buildUR(exitCode.intValue() == 0 ? "Completed" : "Failed");
 			return Finished;
 		} catch (IllegalThreadStateException e) {
 			return Operating;
 		}
 	}
 
 	@Override
 	public String getConfiguration() {
 		return "";
 	}
 
 	@Override
 	public String getName() {
 		return DEFAULT_LISTENER_NAME;
 	}
 
 	@Override
 	public String getProperty(String propName) throws RemoteException {
 		switch (Property.is(propName)) {
 		case STDOUT:
 			return stdout.toString();
 		case STDERR:
 			return stderr.toString();
 		case EXIT_CODE:
 			return (exitCode == null) ? "" : exitCode.toString();
 		case EMAIL:
 			return emailAddress;
 		case READY_TO_NOTIFY:
 			return Boolean.toString(readyToSendEmail);
 		case USAGE:
 			if (ur == null)
 				return "";
 			try {
 				StringWriter writer = new StringWriter();
 				JAXBContext.newInstance(ur.getClass()).createMarshaller()
 						.marshal(ur, writer);
 				return writer.toString();
 			} catch (JAXBException e) {
 				e.printStackTrace();
 				return "";
 			}
 		default:
 			throw new RemoteException("unknown property");
 		}
 	}
 
 	@Override
 	public String getType() {
 		return DEFAULT_LISTENER_NAME;
 	}
 
 	@Override
 	public String[] listProperties() {
 		return Property.names();
 	}
 
 	@Override
 	public void setProperty(String propName, String value)
 			throws RemoteException {
 		switch (Property.is(propName)) {
 		case EMAIL:
 			emailAddress = value;
 			return;
 		case READY_TO_NOTIFY:
 			readyToSendEmail = parseBoolean(value);
 			return;
 		case STDOUT:
 		case STDERR:
 		case EXIT_CODE:
 		case USAGE:
 			throw new RemoteException("property is read only");
 		default:
 			throw new RemoteException("unknown property");
 		}
 	}
 
 	@Override
 	public RemoteListener getDefaultListener() {
 		return this;
 	}
 }
