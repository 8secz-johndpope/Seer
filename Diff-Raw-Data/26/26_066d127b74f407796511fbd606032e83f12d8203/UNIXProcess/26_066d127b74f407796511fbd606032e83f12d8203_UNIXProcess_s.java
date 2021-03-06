 /*
  * Java core library component.
  *
  * Copyright (c) 1997, 1998
  *      Transvirtual Technologies, Inc.  All rights reserved.
  *
  * See the file "license.terms" for information on usage and redistribution
  * of this file.
  */
 
 package kaffe.lang;
 
 import java.io.FileDescriptor;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
import java.io.PipedOutputStream;
 
 public class UNIXProcess
   extends Process
   implements Runnable
 {
 	boolean isalive;
 	int exit_code;
 	FileDescriptor stdin_fd;
 	FileDescriptor stdout_fd;
 	FileDescriptor stderr_fd;
 	FileDescriptor sync_fd;
 	int pid;
 	OutputStream stdin_stream;
 	InputStream raw_stdout;
 	InputStream raw_stderr;
	ProcessInputStream piped_stdout_in;
	ProcessInputStream piped_stderr_in;
	PipedOutputStream piped_stdout_out;
	PipedOutputStream piped_stderr_out;
	int numReaders;
 	private static Thread tidy;
 
 static {
 	tidy = new Thread(new UNIXProcess());
 	tidy.setDaemon(true);
 	tidy.start();
 }
 
 public UNIXProcess() {
 }
 
 public UNIXProcess(String argv[], String arge[]) {
 
 	stdin_fd = new FileDescriptor();
 	stdout_fd = new FileDescriptor();
 	stderr_fd = new FileDescriptor();
 	sync_fd = new FileDescriptor();
 	pid = forkAndExec(argv, arge);
 	isalive = true;
 
 	FileOutputStream sync = new FileOutputStream(sync_fd);
 	byte[] sbuf = new byte[1];
 	try {
 		sync.write(sbuf);
 	}
 	catch (IOException _) {
 	}
 }
 
public void destory() {
}

 native public void destroy();
 
 public int exitValue() {
 	return exit_code;
 }
 
 native public int forkAndExec(Object cmd[], Object env[]);
 
 public InputStream getErrorStream() {
 	return raw_stderr;
 }
 
 public InputStream getInputStream() {
 	return raw_stdout;
 }
 
 public OutputStream getOutputStream() {
 	return stdin_stream;
 }
 
native public void notifyReaders();

 native public void run();
 
 public int waitFor() throws InterruptedException {
 	synchronized(this) {
 		while (isalive == true) {
 			wait();
 		}
 	}
 	return (exit_code);
 }
 }
