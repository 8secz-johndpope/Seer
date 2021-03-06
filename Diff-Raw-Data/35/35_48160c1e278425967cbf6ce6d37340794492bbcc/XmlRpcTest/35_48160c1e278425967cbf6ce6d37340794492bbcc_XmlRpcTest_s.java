 package org.python.pydev.debug.codecoverage;
 
 import java.io.File;
 import java.io.IOException;
 import java.net.MalformedURLException;
 
 import junit.framework.TestCase;
 
 import org.apache.xmlrpc.XmlRpcException;
 import org.apache.xmlrpc.XmlRpcHandler;
 import org.apache.xmlrpc.XmlRpcRequest;
 import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
 import org.apache.xmlrpc.server.XmlRpcNoSuchHandlerException;
 import org.apache.xmlrpc.server.XmlRpcServer;
 import org.apache.xmlrpc.webserver.WebServer;
 import org.python.pydev.core.REF;
 import org.python.pydev.core.TestDependent;
 import org.python.pydev.core.docutils.StringUtils;
 import org.python.pydev.debug.newconsole.IPydevXmlRpcClient;
 import org.python.pydev.debug.newconsole.PydevXmlRpcClient;
 import org.python.pydev.plugin.SocketUtil;
 import org.python.pydev.runners.ThreadStreamReader;
 
 public class XmlRpcTest extends TestCase{
 
     String[] EXPECTED = new String[]{
             "false",
             "false",
             "10",
             "false",
             "false",
             "false",
             "false",
             "true",
             "false",
             "true",
             "false",
             "true",
             "false",
             "20",
             "30",
             "false",
             "false",
             "false",
             "false",
             
             "false",
             "false",
             "start get completions",
             "Foo",
             "1",
             "foo",
             "3|4",
             "end get completions",
             "start raw_input",
             "'input_request'",
             "false",
             "false",
             "finish raw_input",
             "'foo'",
             "false",
             "false",
             "Console already exited with value: 0 while waiting for an answer.|exceptions.SystemExit:0",
     };
     
     private int next = -1;
 
     private ThreadStreamReader err;
 
     private ThreadStreamReader out;
 
     private WebServer webServer;
     
     public static void main(String[] args) throws MalformedURLException, XmlRpcException {
        junit.textui.TestRunner.run(XmlRpcTest.class);
     }
     
     @Override
     protected void tearDown() throws Exception {
         if(this.webServer != null){
             this.webServer.shutdown();
         }
     }
     
     private Process startServer(int client_port, int port, boolean python) throws IOException {
         File f = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"pysrc/pydevconsole.py");
         
         String[] cmdLine;
         if(python){
             cmdLine = new String[]{TestDependent.PYTHON_EXE, "-u", REF.getFileAbsolutePath(f), ""+port, ""+client_port};
         }else{
             cmdLine = new String[]{TestDependent.JAVA_LOCATION, "-classpath",
                             TestDependent.JYTHON_JAR_LOCATION, "org.python.util.jython",
                             REF.getFileAbsolutePath(f), ""+port, ""+client_port};
         }
         
         Process process = Runtime.getRuntime().exec(cmdLine);
         err = new ThreadStreamReader(process.getErrorStream());
         out = new ThreadStreamReader(process.getInputStream());
         err.start();
         out.start();
         
         
         this.webServer = new WebServer(client_port);
         XmlRpcServer serverToHandleRawInput = this.webServer.getXmlRpcServer();
         serverToHandleRawInput.setHandlerMapping(new XmlRpcHandlerMapping(){
 
             public XmlRpcHandler getHandler(String handlerName) throws XmlRpcNoSuchHandlerException, XmlRpcException {
                 return new XmlRpcHandler(){
 
                     public Object execute(XmlRpcRequest request) throws XmlRpcException {
                         return "input_request";
                     }};
             }});
         
         this.webServer.start();
         return process;
     }
     
     public void testXmlRpcServerPython() throws XmlRpcException, IOException, InterruptedException {
         checkServer(true);
     }
     
     public void testXmlRpcServerJython() throws XmlRpcException, IOException, InterruptedException {
         checkServer(false);
     }
     
     public void checkServer(boolean python) throws XmlRpcException, IOException, InterruptedException {
         int port = SocketUtil.findUnusedLocalPort();
         int client_port = SocketUtil.findUnusedLocalPort();
         Process process = startServer(client_port, port, python);
         
         
 //        int port = 8000;
 //        Process process = null;
         
         //give some time for the process to start
         if(!python){
             synchronized (this) {
                 this.wait(1500);
             }
         }else{
             synchronized (this) {
                 this.wait(500);
             }
         }
         
         
         try {
             ThreadStreamReader stdErr = new ThreadStreamReader(process.getErrorStream());
             stdErr.start();
             IPydevXmlRpcClient client = new PydevXmlRpcClient(process, stdErr);
             client.setPort(port);
             
             printArr(client.execute("addExec", new Object[]{"abc = 10"}));
             printArr(client.execute("addExec", new Object[]{"abc"}));
             printArr(client.execute("addExec", new Object[]{"import sys"}));
             printArr(client.execute("addExec", new Object[]{"class Foo:"}));
             printArr(client.execute("addExec", new Object[]{"    print 20"}));
             printArr(client.execute("addExec", new Object[]{"    print >> sys.stderr, 30"}));
             printArr(client.execute("addExec", new Object[]{""}));
             printArr(client.execute("addExec", new Object[]{"foo=Foo()"}));
             printArr(client.execute("addExec", new Object[]{"foo.__doc__=None"}));
             printArr("start get completions");
             Object[] completions = (Object[]) client.execute("getCompletions", new Object[]{"fo"});
             //the completions may come in any order
             if(!((Object[])completions[0])[0].toString().equals("Foo")){
                 Object object = completions[1];
                 completions[1] = completions[0];
                 completions[0] = object;
             }
             printArr(completions);
             printArr("end get completions");
             
             printArr("start raw_input");
             printArr(client.execute("addExec", new Object[]{"raw_input()"}));
             printArr("finish raw_input");
             printArr(client.execute("addExec", new Object[]{"'foo'"}));
 //            System.out.println("Ask exit");
             printArr(client.execute("addExec", new Object[]{"sys.exit(0)"}));
 //            System.out.println("End Ask exit");
         } finally {
             if(process != null){
                 process.destroy();
             }
         }
         assertEquals(next, EXPECTED.length-1);
     }
 
     private void printArr(Object ... execute) {
         if(this.out != null){
             print(this.out.getAndClearContents());
             print(this.err.getAndClearContents());
         }
         
         for(Object o:execute){
             print(o);
         }
     }
     
     private void print(Object execute) {
         if(execute instanceof Object[]){
             Object[] objects = (Object[]) execute;
             for(Object o:objects){
                 print(o);
             }
         }else{
 //            System.out.println(execute);
             String s = ""+execute;
             if(s.length() > 0){
                 String expected = EXPECTED[nextExpected()].trim();
                 String found = s.trim();
                 if(!expected.equals(found)){
                     if(expected.equals("false")){
                         expected = "0";
                     }
                     if(expected.equals("true")){
                         expected = "1";
                     }
                     if(expected.equals("3|4")){
                         if(found.equals("3") || found.equals("4")){
                             return;
                         }
                     }
                     if(expected.equals("Console already exited with value: 0 while waiting for an answer.|exceptions.SystemExit:0")){
                         if(found.equals("Console already exited with value: 0 while waiting for an answer.") || 
                                found.equals("exceptions.SystemExit:0")){
                             return;
                         }
                     }
                     String errorMessage = StringUtils.format("Expected: >>%s<< and not: >>%s<< (position:%s)", 
                             expected, found, next);
                     assertEquals(errorMessage, expected, found);
                 }
             }
         }
     }
 
     private int nextExpected() {
         next += 1;
         return next;
     }
     
 }
