 // Copyright (C) 2012 jOVAL.org.  All rights reserved.
 // This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
 
 package jwsmv;
 
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.io.PipedInputStream;
 import java.io.PipedOutputStream;
 import javax.xml.bind.JAXBElement;
 import javax.xml.bind.JAXBException;
 import javax.security.auth.login.FailedLoginException;
 
 import com.microsoft.wsman.fault.WSManFaultType;
 import com.microsoft.wsman.shell.CommandLine;
 import com.microsoft.wsman.shell.CommandResponse;
 import com.microsoft.wsman.shell.CommandStateType;
 import com.microsoft.wsman.shell.DesiredStreamType;
 import com.microsoft.wsman.shell.Receive;
 import com.microsoft.wsman.shell.ReceiveResponse;
 import com.microsoft.wsman.shell.Signal;
 import com.microsoft.wsman.shell.SignalResponse;
 import com.microsoft.wsman.shell.Send;
 import com.microsoft.wsman.shell.SendResponse;
 import com.microsoft.wsman.shell.StreamType;
 import org.dmtf.wsman.AttributableEmpty;
 import org.dmtf.wsman.AttributablePositiveInteger;
 import org.dmtf.wsman.OptionSet;
 import org.dmtf.wsman.OptionType;
 import org.dmtf.wsman.SelectorSetType;
 import org.dmtf.wsman.SelectorType;
 import org.w3c.soap.envelope.Fault;
 
 import jwsmv.wsman.FaultException;
 import jwsmv.wsman.Port;
 import jwsmv.wsman.operation.CommandOperation;
 import jwsmv.wsman.operation.DeleteOperation;
 import jwsmv.wsman.operation.ReceiveOperation;
 import jwsmv.wsman.operation.SendOperation;
 import jwsmv.wsman.operation.SignalOperation;
 
 /**
  * Simple implementation of a WinRM Shell-based Process.
  *
  * @author David A. Solin
  * @version %I% %G%
  */
 public class ShellCommand extends Process implements Constants, Runnable {
     static final long TIMEOUT_CODE = 2150858793L;
 
     /**
      * An enumeration of codes that can be issued to a running process using a signal.
      */
     static enum SignalCode {
 	TERMINATE("http://schemas.microsoft.com/wbem/wsman/1/windows/shell/signal/terminate"),
 	CTL_C("http://schemas.microsoft.com/wbem/wsman/1/windows/shell/signal/ctrl_c"),
 	CTL_BREAK("http://schemas.microsoft.com/wbem/wsman/1/windows/shell/signal/ctrl_break");
 
 	private String value;
 
 	private SignalCode(String value) {
 	    this.value = value;
 	}
 
 	String value() {
 	    return value;
 	}
     }
 
     /**
      * An enumeration of the possible states of a running process, embedding their URI values.
      */
     static enum State {
 	DONE("http://schemas.microsoft.com/wbem/wsman/1/windows/shell/CommandState/Done"),
 	PENDING("http://schemas.microsoft.com/wbem/wsman/1/windows/shell/CommandState/Pending"),
 	ERROR("http://schemas.microsoft.com/wbem/wsman/1/windows/shell/CommandState/Error"),
 	RUNNING("http://schemas.microsoft.com/wbem/wsman/1/windows/shell/CommandState/Running");
 
 	private String value;
 
 	private State(String value) {
 	    this.value = value;
 	}
 
 	String value() {
 	    return value;
 	}
 
 	static State fromValue(String s) throws IllegalArgumentException {
 	    for (State state : values()) {
 		if (state.value().equals(s)) {
 		    return state;
 		}
 	    }
 	    throw new IllegalArgumentException(s);
 	}
     }
 
     /**
      * Get the ID of the command.
      */
     public String getId() {
 	return id;
     }
 
     public String getCommand() {
 	StringBuffer sb = new StringBuffer(cmd);
 	if (args != null) {
 	    for (String arg : args) {
 		sb.append(" ");
 		if (arg.indexOf(" ") == -1) {
 		    sb.append(arg);
 		} else {
 		    sb.append("\"").append(arg.replace("\"","\\\"")).append("\"");
 		}
 	    }
 	}
 	return sb.toString();
     }
 
     /**
      * Wait for at most millis milliseconds for the process to finish executing.
      */
     public void waitFor(long millis) throws InterruptedException {
 	long endTime = System.currentTimeMillis() + millis;
 	while (isRunning() && System.currentTimeMillis() < endTime) {
 	    Thread.sleep(100);
 	}
 	long maxWait = endTime - System.currentTimeMillis();
 	if (maxWait > 0) {
 	    if (thread != null && thread.isAlive()) {
 		thread.join(maxWait);
 	    }
 	}
     }
 
     /**
      * Test to see if the process is running.
      */
     public boolean isRunning() {
 	return state == State.RUNNING;
     }
 
     /**
      * Test to see if there was an error managing the remote process execution.
      */
     public boolean isError() {
 	return state == State.ERROR;
     }
 
     /**
      * Get the error.
      */
     public Exception getError() throws IllegalStateException {
 	if (state == State.ERROR) {
 	    return error;
 	} else {
 	    throw new IllegalStateException(state.toString());
 	}
     }
 
     /**
      * Start the process running.
      */
     public void start() throws JAXBException, IOException, FaultException {
 	CommandLine cl = Factories.SHELL.createCommandLine();
 	cl.setCommand(cmd);
 	if (args != null) {
 	    for (String arg : args) {
 		cl.getArguments().add(arg);
 	    }
 	}
 
 	CommandOperation commandOperation = new CommandOperation(cl);
 	commandOperation.addResourceURI(SHELL_URI);
 	commandOperation.addSelectorSet(selector);
 
 	//
 	// The client-side mode for standard input is console if TRUE and pipe if FALSE. This does not
 	// have an impact on the wire protocol. This option name MUST be used by the client of the Text-based
 	// Command Shell when starting the execution of a command using rsp:Command request to indicate that
 	// the client side of the standard input is console; the default implies pipe.
 	//
 	OptionType winrsStdin = Factories.WSMAN.createOptionType();
 	winrsStdin.setName("WINRS_CONSOLEMODE_STDIN");
 	winrsStdin.setValue("FALSE");
 
 	//
 	// If set to TRUE, this option requests that the server runs the command without using cmd.exe; if
 	// set to FALSE, the server is requested to use cmd.exe. By default the value is FALSE. This does
 	// not have any impact on the wire protocol.
 	//
 	OptionType winrsSkipCmd = Factories.WSMAN.createOptionType();
 	winrsSkipCmd.setName("WINRS_SKIP_CMD_SHELL");
 	winrsSkipCmd.setValue("FALSE");
 
 	OptionSet options = Factories.WSMAN.createOptionSet();
 	options.getOption().add(winrsStdin);
 	options.getOption().add(winrsSkipCmd);
 	commandOperation.addOptionSet(options);
 
 	try {
 	    CommandResponse response = commandOperation.dispatch(port);
 	    state = State.RUNNING;
 	    disposable = true;
 	    id = response.getCommandId();
 	    stdoutPipe = new PipedOutputStream();
 	    stdout = new PipedInputStream(stdoutPipe);
 	    stderrPipe = new PipedOutputStream();
 	    stderr = new PipedInputStream(stderrPipe);
 	    thread = new Thread(group, this, "ShellCommand:" + id);
 	    thread.start();
 	} catch (FailedLoginException e) {
 	    throw new RuntimeException(e);
 	}
     }
 
     // Overrides of Process methods
 
     @Override
     public int waitFor() throws InterruptedException {
 	waitFor(360000L);
 	if (isRunning()) {
 	    destroy();
 	}
 	return exitValue();
     }
 
     @Override
     public void destroy() {
 	finalize();
 	state = State.DONE;
     }
 
     @Override
     public OutputStream getOutputStream() {
 	if (stdin == null) {
 	    stdin = new CommandOutputStream(1024);
 	}
 	return stdin;
     }
 
     @Override
     public InputStream getInputStream() {
 	return stdout;
     }
 
     @Override
     public InputStream getErrorStream() {
 	return stderr;
     }
 
     @Override
     public int exitValue() throws IllegalThreadStateException {
 	switch(state) {
 	  case DONE:
 	    return exitCode;
 	  case ERROR:
 	    IllegalThreadStateException ex = new IllegalThreadStateException(state.toString());
 	    ex.initCause(getError());
 	    throw ex;
 	  default:
 	    throw new IllegalThreadStateException(state.toString());
 	}
     }
 
     // Implement Runnable
 
     /**
      * Read stdout and stderr from the remote process.
      */
     public void run() {
 	while(isRunning()) {
 	    try {
 		DesiredStreamType desired = Factories.SHELL.createDesiredStreamType();
 		desired.setCommandId(id);
 		desired.getValue().add(Shell.STDOUT);
 		desired.getValue().add(Shell.STDERR);
 		Receive receive = Factories.SHELL.createReceive();
 		receive.setDesiredStream(desired);
 		ReceiveOperation receiveOperation = new ReceiveOperation(receive);
 		receiveOperation.addResourceURI(SHELL_URI);
 		receiveOperation.addSelectorSet(selector);
 		OptionType keepAlive = Factories.WSMAN.createOptionType();
 		keepAlive.setName("WSMAN_CMDSHELL_OPTION_KEEPALIVE");
 		keepAlive.setValue("TRUE");
 		OptionSet options = Factories.WSMAN.createOptionSet();
 		options.getOption().add(keepAlive);
 		receiveOperation.addOptionSet(options);
 
 		ReceiveResponse response = receiveOperation.dispatch(port);
 		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
 		for (StreamType stream : response.getStream()) {
 		    if (stream.isSetValue()) {
 			byte[] val = stream.getValue();
 			if (val.length > 0) {
 			    String streamName = stream.getName();
 			    if (Shell.STDOUT.equals(streamName)) {
 				stdoutPipe.write(val);
 				stdoutPipe.flush();
 			    } else if (Shell.STDERR.equals(streamName)) {
 				stderrPipe.write(val);
 				stderrPipe.flush();
 			    }
 			}
 		    }
 		}
 		if (response.isSetCommandState()) {
 		    CommandStateType state = response.getCommandState();
 		    ShellCommand.this.state = State.fromValue(state.getState());
 		    if (state.isSetExitCode()) {
 			exitCode = state.getExitCode().intValue();
 			//
 			// Per section section 3.1.4.14, point 4 of MS-WSMV specification client MUST send
 			// signal message with Terminate code after receiving final response from server.
 			//
 			ShellCommand.this.finalize();
 		    }
 		}
 	    } catch (FaultException e) {
 		boolean retry = false;
 		Fault fault = e.getFault();
 		if (fault.isSetDetail()) {
 		    for (Object obj : fault.getDetail().getAny()) {
 			if (obj instanceof JAXBElement) {
 			    obj = ((JAXBElement)obj).getValue();
 			}
 			if (obj instanceof WSManFaultType) {
 			    WSManFaultType wsFault = (WSManFaultType)obj;
 			    if (wsFault.getCode() == TIMEOUT_CODE) {
 				retry = true;
 			    }
 			}
 		    }
 		}
 		if (!retry) {
 		    error = e;
 		    state = State.ERROR;
 		}
 	    } catch (Exception e) {
 		error = e;
 		state = State.ERROR;
 	    }
 	}
 	try {
 	    stdoutPipe.close();
 	} catch (IOException e) {
 	}
 	try {
 	    stderrPipe.close();
 	} catch (IOException e) {
 	}
     }
 
     // Internal
 
     private Port port;
     private String id;
     private SelectorSetType selector;
     private State state;
     private int exitCode;
     private PipedOutputStream stdoutPipe, stderrPipe;
     private InputStream stdout, stderr;
     private OutputStream stdin;
     private String cmd;
     private String[] args;
     private boolean disposable;
     private Exception error;
     private ThreadGroup group;
     private Thread thread;
 
     /**
      * Create a command for the specified Shell.
      */
     ShellCommand(Shell shell, String cmd, String[] args) {
 	selector = shell.getSelectorSet();
 	group = shell.group;
 	this.port = shell.port;
 	this.cmd = cmd;
 	this.args = args;
 	stdin = null;
 	stderr = null;
 	stdout = null;
 	exitCode = -1;
 	disposable = false;
 	state = State.PENDING;
     }
 
     /**
      * Delete the ShellCommand on the target machine (idempotent).
      */
     @Override
     protected void finalize() {
 	if (disposable) {
 	    try {
 		Signal signal = Factories.SHELL.createSignal();
 		signal.setCommandId(id);
 		signal.setCode(SignalCode.TERMINATE.value());
 		SignalOperation signalOperation = new SignalOperation(signal);
 		signalOperation.addResourceURI(SHELL_URI);
 		signalOperation.addSelectorSet(selector);
 		SignalResponse response = signalOperation.dispatch(port);
		stderrPipe.close();
 	    } catch (Exception e) {
		e.printStackTrace();
 	    }
 	    disposable = false;
 	}
     }
 
     /**
      * An OutputStream implementation that is triggered by flush() to send data upstream to the process.
      */
     class CommandOutputStream extends ByteArrayOutputStream {
 	CommandOutputStream(int size) {
 	    super(size);
 	}
 
 	@Override
 	public synchronized void flush() throws IOException {
 	    try {
 		StreamType stream = Factories.SHELL.createStreamType();
 		stream.setName(Shell.STDIN);
 		stream.setCommandId(id);
 		stream.setValue(toByteArray());
 		reset();
 		Send send = Factories.SHELL.createSend();
 		send.getStream().add(stream);
 		SendOperation sendOperation = new SendOperation(send);
 		sendOperation.addResourceURI(SHELL_URI);
 		sendOperation.addSelectorSet(selector);
 
 		SendResponse response = sendOperation.dispatch(port);
 		if (response.isSetDesiredStream()) {
 		    StreamType rs = response.getDesiredStream();
 		    if (rs.getName().equals(Shell.STDIN) && rs.getEnd()) {
 			close();
 		    }
 		}
 	    } catch (FailedLoginException e) {
 		throw new IOException(e);
 	    } catch (JAXBException e) {
 		throw new IOException(e);
 	    } catch (FaultException e) {
 		throw new IOException(e);
 	    }
 	}
     }
 }
