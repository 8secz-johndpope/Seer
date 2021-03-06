 /*
  * This software is licensed under the terms of the GNU GENERAL PUBLIC LICENSE
  * Version 2, which can be found at http://www.gnu.org/copyleft/gpl.html
  */
 package org.cubictest.recorder.selenium;
 
 import java.io.IOException;
 import java.lang.reflect.InvocationTargetException;
 import java.net.ServerSocket;
 
 import org.cubictest.common.utils.ErrorHandler;
 import org.cubictest.common.utils.Logger;
 import org.cubictest.recorder.IRecorder;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.jface.operation.IRunnableWithProgress;
 import org.mortbay.http.HttpContext;
 import org.mortbay.jetty.Server;
 import org.mortbay.jetty.servlet.ServletHandler;
 import org.openqa.selenium.server.SeleniumServer;
 
 import com.metaparadigm.jsonrpc.JSONRPCServlet;
 import com.thoughtworks.selenium.DefaultSelenium;
 import com.thoughtworks.selenium.Selenium;
 import com.thoughtworks.selenium.SeleniumException;
 
 public class SeleniumRecorder implements IRunnableWithProgress {
 	private boolean seleniumStarted;
 	private Selenium selenium;
 	private SeleniumServer seleniumProxy;
 	private int port = 4444;
 	private final String url;
 	private Thread serverThread;
 
	public SeleniumRecorder(IRecorder recorder, String url) {
 		this.url = url;
 		
 		// start server
 		
 		try {
 			port = findAvailablePort();
 			System.out.println("Port: " + port);
 			seleniumProxy = new SeleniumServer(port);
 			
 			Server server = seleniumProxy.getServer();
 			HttpContext cubicRecorder = server.getContext("/selenium-server/cubic-recorder/");
 			ServletHandler servletHandler = new ServletHandler();
 			servletHandler.addServlet("JSON-RPC", "/JSON-RPC", JSONRPCServlet.class.getName());
 			cubicRecorder.addHandler(servletHandler);
 			servletHandler.getSessionManager().addEventListener(new SeleniumRecorderSessionListener(recorder, url));
 		} catch (Exception e) {
 			ErrorHandler.logAndShowErrorDialogAndRethrow(e);
 		}
 	}
 	
 	public void stop() {
 		try {
 			selenium.stop();
 			seleniumProxy.stop();
 		} catch(SeleniumException e) {
 			Logger.error(e, e.toString());
 		}
 	}
 
 	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
 		serverThread = new Thread() {
 
 			@Override
 			public void run() {
 		        try {
 					seleniumProxy.start();
 					selenium = new DefaultSelenium("localhost", seleniumProxy.getPort(), "*firefox", url);
 					selenium.start();
 					selenium.open(url);
 					try {
 						Thread.sleep(1000);
 					} catch (InterruptedException e) {
 						ErrorHandler.logAndShowErrorDialogAndRethrow(e);
 					}
 					seleniumStarted = true;
		        } catch (Exception e) {
					ErrorHandler.logAndShowErrorDialogAndRethrow(e);
 				}
 			}
 			
 			
 		};
 		
 		serverThread.start();
 	}
 	
 	private int findAvailablePort() throws IOException {
 		ServerSocket s = new ServerSocket();
 		s.bind(null);
 		int port = s.getLocalPort();
 		s.close();
 		return port;
 	}
 
 	public Selenium getSelenium() {
 		return selenium;
 	}
 
 	public boolean isSeleniumStarted() {
 		return seleniumStarted;
 	}
 
 }
