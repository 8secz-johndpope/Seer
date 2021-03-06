 package net.sf.eclipsefp.haskell.util;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.Reader;
 import java.io.StringWriter;
 import java.io.Writer;
 
 public class ProcessRunner implements IProcessRunner {
 
   private final IProcessFactory fProcessFactory;
   
   private final static String STDOUT_REDIRECT = "output_redirect";
   private final static String STDERR_REDIRECT = "error_redirect";
 
   public ProcessRunner() {
     this( new ProcessFactory() );
   }
 
   public ProcessRunner( final IProcessFactory factory ) {
     fProcessFactory = factory;
   }
 
   public int executeBlocking( final File workingDir, final Writer out,
       final Writer err, final String ... args ) throws IOException {
 
     Process proc = doExecute( workingDir, args );
 
     Thread outRedirect = redirect( new InputStreamReader( proc.getInputStream() ), out, STDOUT_REDIRECT );
     Thread errRedirect = null;
     if (err!=null){
     	errRedirect = redirect( new InputStreamReader( proc.getErrorStream() ), err, STDERR_REDIRECT );
     }
     int code=-1;
     try {
       code=proc.waitFor(); // wait for process to finish
       outRedirect.join(); // wait until out stream content is redirected
       if (errRedirect!=null){
     	  errRedirect.join(); // wait until err stream content is redirected
       }
     } catch (InterruptedException ex) {
       // ignore
     }
     return code;
   }
 
   public Process executeNonblocking( final File workingDir, final Writer out,
       Writer err, final String ... args ) throws IOException {
     Process proc = doExecute( workingDir, args );
     redirect( new InputStreamReader( proc.getInputStream() ), out, STDOUT_REDIRECT );
     if (err==null){
     	err=new StringWriter();
     }
     redirect( new InputStreamReader( proc.getErrorStream() ), err, STDERR_REDIRECT );
     
     return proc;
   }
 
   private Process doExecute( final File workingDir, final String ... args )
       throws IOException {
     Process proc = fProcessFactory.startProcess( workingDir, args );
     return proc;
   }
 
   private static Thread redirect( final Reader in, final Writer out, String name ) {
     Thread outRedirect = new StreamRedirect( name, in, out );
     outRedirect.start();
     return outRedirect;
   }
   
   public static Thread[] consume(Process proc){
 	  Thread t1=redirect( new InputStreamReader( proc.getInputStream() ), new StringWriter(), STDOUT_REDIRECT );
 	  Thread t2=redirect( new InputStreamReader( proc.getErrorStream() ), new StringWriter(), STDERR_REDIRECT );
 	  return new Thread[]{t1,t2};
   }
 
   public static String getExecutableVersion(String path) throws IOException{
 	  File f=new File(path);
 	  if (f.exists()){
 		  StringWriter sw=new StringWriter();
 		  Process p=new ProcessRunner().executeNonblocking(f.getParentFile(), sw, null, f.getAbsolutePath(),"--version");
 		  for (int a=0;a<10;a++){
 			  try {
 				  p.exitValue();
 				  break;
 			  } catch (IllegalThreadStateException ise){
 				  // still running
 			  }
 			  try {
 				  Thread.sleep(100);
 			  } catch (InterruptedException ie){
 				  //
 			  }
 		  }
 		  try {
 			  String line=sw.toString().trim();
 			  int ix=line.lastIndexOf(' ');
			  if (ix>-1 && ix<line.length()){
				  line=line.substring(ix+1);
				  if (Character.isDigit(line.charAt(0))){
					  return line;
				  }
 			  }
 		  } finally {
 			  p.destroy();
 		  }
 	  }
 	  return null;
   }
 }
