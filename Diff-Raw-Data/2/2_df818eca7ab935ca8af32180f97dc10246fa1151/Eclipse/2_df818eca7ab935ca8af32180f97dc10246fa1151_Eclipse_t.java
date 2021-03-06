 package org.eclipse.help.servlet;
 /*
  * (c) Copyright IBM Corp. 2000, 2001.
  * All Rights Reserved.
  */
 import java.io.File;
 import java.lang.reflect.Method;
 import java.net.*;
 import java.util.ResourceBundle;
 
 import javax.servlet.*;
 
 /**
  * Eclipse launcher
  */
 public class Eclipse {
 	private static final String RESOURCE_BUNDLE = InitServlet.class.getName();
	private static final String SERVLET_FACADE = "org.eclipse.help.servletFacade";
 	private Class bootLoader;
 	private Object platformRunnable;
 	private Method runMethod;
 	private ResourceBundle resBundle;
 	private ServletContext context;
 
 	/**
 	 * Constructor
 	 */
 	public Eclipse(ServletContext context) throws ServletException {
 		this.context = context;
 		// initializes string resources
 		resBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE);
 
 		init();
 	}
 
 	/**
 	 * Servlet destroy method shuts down Eclipse.
 	 */
 	public void shutdown() {
 		try {
 			Class bootLoader = getBootLoader();
 			bootLoader.getMethod("shutdown", new Class[] {
 			}).invoke(bootLoader, new Object[] {
 			});
 		} catch (Exception e) {
 			context.log(resBundle.getString("problemShutdown"));
 		}
 
 	}
 
 	/**
 	 * Gets content from the named url (this could be and eclipse defined url)
 	 */
 	public URLConnection openConnection(String url) throws Exception {
 		
 		//System.out.println("opening connection");
 		Object[] params = new Object[1];
 		params[0] = new Object[] { "openConnection", url };
 		Object retObj = runMethod.invoke(platformRunnable, params);
 		if (retObj != null && retObj instanceof URLConnection)
 			return (URLConnection) retObj;
 		else
 			return null;
 
 	}
 
 	/**
 	 * returns a platform <code>BootLoader</code> which can be used to start
 	 * up and run the platform.
 	 */
 	private Class getBootLoader() throws Exception {
 		if (bootLoader == null) {
 			URL baseURL =
 				new URL("file", null, context.getRealPath("/").replace('\\', '/'));
 			
 			// At some point, the webapp was the actual eclipse directory, so the line below worked,
 			// but now we just install eclipse elsewhere, and point the webapp to the actual help webapp
 			//String path = baseURL.getFile() + "/plugins/org.eclipse.core.boot/boot.jar";
 			File f = new File(baseURL.getFile());
 			f = f.getParentFile();
 			String path = (f.toString() + "/org.eclipse.core.boot/boot.jar").replace('\\', '/');
 			
 			URL bootUrl =
 				new URL(baseURL.getProtocol(), baseURL.getHost(), baseURL.getPort(), path);
 				//System.out.println("URL for bootloader:"+bootUrl);
 			bootLoader =
 				new URLClassLoader(new URL[] { bootUrl }, null).loadClass(
 					"org.eclipse.core.boot.BootLoader");
 		}
 		return bootLoader;
 	}
 
 	private synchronized void init() throws ServletException {
 		System.out.println("init eclipse");
 		try {
 			if (context.getAttribute("platformRunnable") == null) {
 				//System.out.println("getting boot loader");
 				bootLoader = getBootLoader();
 				//System.out.println("got bootloader: " + bootLoader);
 				Method mStartup =
 					bootLoader.getMethod(
 						"startup",
 						new Class[] { URL.class, String.class, String[].class });
 
 				String work =
 					((File) context.getAttribute("javax.servlet.context.tempdir"))
 						.getAbsolutePath();
 
 				//System.out.println("starting eclipse");
 				mStartup.invoke(bootLoader, new Object[] { null, work, new String[] {
 					}
 				});
 
 				Method mGetRunnable =
 					bootLoader.getMethod("getRunnable", new Class[] { String.class });
 
 				//System.out.println("get platform runnable");
 				platformRunnable =
 					mGetRunnable.invoke(bootLoader, new Object[] { SERVLET_FACADE });
 
 				runMethod =
 					platformRunnable.getClass().getMethod("run", new Class[] { Object.class });
 			}
 
 		} catch (Throwable e) {
 			context.log(resBundle.getString("problemInit"), e);
 			throw new ServletException(e);
 		}
 	}
 
 }
