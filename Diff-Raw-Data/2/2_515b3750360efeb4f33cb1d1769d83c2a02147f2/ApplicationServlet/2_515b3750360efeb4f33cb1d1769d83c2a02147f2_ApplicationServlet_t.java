 /* *************************************************************************
  
  IT Mill Toolkit 
 
  Development of Browser User Interfaces Made Easy
 
  Copyright (C) 2000-2006 IT Mill Ltd
  
  *************************************************************************
 
  This product is distributed under commercial license that can be found
  from the product package on license.pdf. Use of this product might 
  require purchasing a commercial license from IT Mill Ltd. For guidelines 
  on usage, see licensing-guidelines.html
 
  *************************************************************************
  
  For more information, contact:
  
  IT Mill Ltd                           phone: +358 2 4802 7180
  Ruukinkatu 2-4                        fax:   +358 2 4802 7181
  20540, Turku                          email:  info@itmill.com
  Finland                               company www: www.itmill.com
  
  Primary source for information and releases: www.itmill.com
 
  ********************************************************************** */
 
 package com.itmill.toolkit.terminal.gwt.server;
 
 import java.io.BufferedWriter;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.lang.reflect.Constructor;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.Collection;
 import java.util.Date;
 import java.util.Enumeration;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.Properties;
 import java.util.WeakHashMap;
 
 import javax.servlet.ServletContext;
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import javax.servlet.http.HttpSession;
 
 import org.xml.sax.SAXException;
 
 import com.itmill.toolkit.Application;
 import com.itmill.toolkit.Log;
 import com.itmill.toolkit.external.org.apache.commons.fileupload.servlet.ServletFileUpload;
 import com.itmill.toolkit.service.FileTypeResolver;
 import com.itmill.toolkit.terminal.DownloadStream;
 import com.itmill.toolkit.terminal.ParameterHandler;
 import com.itmill.toolkit.terminal.ThemeResource;
 import com.itmill.toolkit.terminal.URIHandler;
 import com.itmill.toolkit.ui.Window;
 
 /**
  * This servlet connects IT Mill Toolkit Application to Web.
  * 
  * @author IT Mill Ltd.
  * @version
  * @VERSION@
  * @since 5.0
  */
 
 public class ApplicationServlet extends HttpServlet {
 
 	private static final long serialVersionUID = -4937882979845826574L;
 
 	/**
 	 * Version number of this release. For example "4.0.0".
 	 */
 	public static final String VERSION;
 
 	/**
 	 * Major version number. For example 4 in 4.1.0.
 	 */
 	public static final int VERSION_MAJOR;
 
 	/**
 	 * Minor version number. For example 1 in 4.1.0.
 	 */
 	public static final int VERSION_MINOR;
 
 	/**
 	 * Builds number. For example 0-beta1 in 4.0.0-beta1.
 	 */
 	public static final String VERSION_BUILD;
 
 	/* Initialize version numbers from string replaced by build-script. */
 	static {
 		if ("@VERSION@".equals("@" + "VERSION" + "@"))
 			VERSION = "4.9.9-INTERNAL-NONVERSIONED-DEBUG-BUILD";
 		else
 			VERSION = "@VERSION@";
 		String[] digits = VERSION.split("\\.");
 		VERSION_MAJOR = Integer.parseInt(digits[0]);
 		VERSION_MINOR = Integer.parseInt(digits[1]);
 		VERSION_BUILD = digits[2];
 	}
 
 	// Configurable parameter names
 	private static final String PARAMETER_DEBUG = "Debug";
 
 	private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;
 
 	private static final int MAX_BUFFER_SIZE = 64 * 1024;
 
 	private static WeakHashMap applicationToLastRequestDate = new WeakHashMap();
 
 	private static WeakHashMap applicationToAjaxAppMgrMap = new WeakHashMap();
 
 	private static final String RESOURCE_URI = "/RES/";
 
 	private static final String AJAX_UIDL_URI = "/UIDL/";
 
 	static final String THEME_DIRECTORY_PATH = "ITMILL/themes/";
 
 	private static final int DEFAULT_THEME_CACHETIME = 1000 * 60 * 60 * 24;
 
 	static final String WIDGETSET_DIRECTORY_PATH = "ITMILL/widgetsets/";
 
 	// Name of the default widget set, used if not specified in web.xml
 	private static final String DEFAULT_WIDGETSET = "com.itmill.toolkit.terminal.gwt.DefaultWidgetSet";
 
 	// Widget set narameter name
 	private static final String PARAMETER_WIDGETSET = "widgetset";
 
 	// Private fields
 	private Class applicationClass;
 
 	private Properties applicationProperties;
 
 	private String resourcePath = null;
 
 	private String debugMode = "";
 
 	private ClassLoader classLoader;
 
 	/**
 	 * Called by the servlet container to indicate to a servlet that the servlet
 	 * is being placed into service.
 	 * 
 	 * @param servletConfig
 	 *            the object containing the servlet's configuration and
 	 *            initialization parameters
 	 * @throws javax.servlet.ServletException
 	 *             if an exception has occurred that interferes with the
 	 *             servlet's normal operation.
 	 */
 	public void init(javax.servlet.ServletConfig servletConfig)
 			throws javax.servlet.ServletException {
 		super.init(servletConfig);
 
 		// Gets the application class name
 		String applicationClassName = servletConfig
 				.getInitParameter("application");
 		if (applicationClassName == null) {
 			Log.error("Application not specified in servlet parameters");
 		}
 
 		// Stores the application parameters into Properties object
 		this.applicationProperties = new Properties();
 		for (Enumeration e = servletConfig.getInitParameterNames(); e
 				.hasMoreElements();) {
 			String name = (String) e.nextElement();
 			this.applicationProperties.setProperty(name, servletConfig
 					.getInitParameter(name));
 		}
 
 		// Overrides with server.xml parameters
 		ServletContext context = servletConfig.getServletContext();
 		for (Enumeration e = context.getInitParameterNames(); e
 				.hasMoreElements();) {
 			String name = (String) e.nextElement();
 			this.applicationProperties.setProperty(name, context
 					.getInitParameter(name));
 		}
 
 		// Gets the debug window parameter
 		String debug = getApplicationOrSystemProperty(PARAMETER_DEBUG, "")
 				.toLowerCase();
 
 		// Enables application specific debug
 		if (!"".equals(debug) && !"true".equals(debug)
 				&& !"false".equals(debug))
 			throw new ServletException(
 					"If debug parameter is given for an application, it must be 'true' or 'false'");
 		this.debugMode = debug;
 
 		// Gets custom class loader
 		String classLoaderName = getApplicationOrSystemProperty("ClassLoader",
 				null);
 		ClassLoader classLoader;
 		if (classLoaderName == null)
 			classLoader = getClass().getClassLoader();
 		else {
 			try {
 				Class classLoaderClass = getClass().getClassLoader().loadClass(
 						classLoaderName);
 				Constructor c = classLoaderClass
 						.getConstructor(new Class[] { ClassLoader.class });
 				classLoader = (ClassLoader) c
 						.newInstance(new Object[] { getClass().getClassLoader() });
 			} catch (Exception e) {
 				Log.error("Could not find specified class loader: "
 						+ classLoaderName);
 				throw new ServletException(e);
 			}
 		}
 		this.classLoader = classLoader;
 
 		// Loads the application class using the same class loader
 		// as the servlet itself
 		try {
 			this.applicationClass = classLoader.loadClass(applicationClassName);
 		} catch (ClassNotFoundException e) {
 			throw new ServletException("Failed to load application class: "
 					+ applicationClassName);
 		}
 
 	}
 
 	/**
 	 * Gets an application or system property value.
 	 * 
 	 * @param parameterName
 	 *            the Name or the parameter.
 	 * @param defaultValue
 	 *            the Default to be used.
 	 * @return String value or default if not found
 	 */
 	private String getApplicationOrSystemProperty(String parameterName,
 			String defaultValue) {
 
 		// Try application properties
 		String val = this.applicationProperties.getProperty(parameterName);
 		if (val != null) {
 			return val;
 		}
 
 		// Try lowercased application properties for backward compability with
 		// 3.0.2 and earlier
 		val = this.applicationProperties.getProperty(parameterName
 				.toLowerCase());
 		if (val != null) {
 			return val;
 		}
 
 		// Try system properties
 		String pkgName;
 		Package pkg = this.getClass().getPackage();
 		if (pkg != null) {
 			pkgName = pkg.getName();
 		} else {
 			String className = this.getClass().getName();
 			pkgName = new String(className.toCharArray(), 0, className
 					.lastIndexOf('.'));
 		}
 		val = System.getProperty(pkgName + "." + parameterName);
 		if (val != null) {
 			return val;
 		}
 
 		// Try lowercased system properties
 		val = System.getProperty(pkgName + "." + parameterName.toLowerCase());
 		if (val != null) {
 			return val;
 		}
 
 		return defaultValue;
 	}
 
 	/**
 	 * Receives standard HTTP requests from the public service method and
 	 * dispatches them.
 	 * 
 	 * @param request
 	 *            the object that contains the request the client made of the
 	 *            servlet.
 	 * @param response
 	 *            the object that contains the response the servlet returns to
 	 *            the client.
 	 * @throws ServletException
 	 *             if an input or output error occurs while the servlet is
 	 *             handling the TRACE request.
 	 * @throws IOException
 	 *             if the request for the TRACE cannot be handled.
 	 */
 	protected void service(HttpServletRequest request,
 			HttpServletResponse response) throws ServletException, IOException {
 
 		if (request.getPathInfo() != null
 				&& request.getPathInfo().startsWith("/ITMILL/")) {
 			serveStaticResourcesInITMILL(request, response);
 			return;
 		}
 
 		Application application = null;
 		try {
 
 			// handle file upload if multipart request
 			if (ServletFileUpload.isMultipartContent(request)) {
 				application = getApplication(request);
 				getApplicationManager(application).handleFileUpload(request,
 						response);
 				return;
 			}
 
 			// Update browser details
 			WebBrowser browser = WebApplicationContext.getApplicationContext(
 					request.getSession()).getBrowser();
 			browser.updateBrowserProperties(request);
 			// TODO Add screen height and width to the GWT client
 
 			// Gets the application
 			application = getApplication(request);
 
 			// Sets the last application request date
 			synchronized (applicationToLastRequestDate) {
 				applicationToLastRequestDate.put(application, new Date());
 			}
 
 			// Invokes context transaction listeners
 			((WebApplicationContext) application.getContext())
 					.startTransaction(application, request);
 
 			// Is this a download request from application
 			DownloadStream download = null;
 
 			// Handles AJAX UIDL requests
 			String resourceId = request.getPathInfo();
 			if (resourceId != null && resourceId.startsWith(AJAX_UIDL_URI)) {
 				getApplicationManager(application).handleUidlRequest(request,
 						response);
 				return;
 			}
 
 			// Handles the URI if the application is still running
 			if (application.isRunning())
 				download = handleURI(application, request, response);
 
 			// If this is not a download request
 			if (download == null) {
 
 				// TODO Clean this branch
 
 				// Window renders are not cacheable
 				response.setHeader("Cache-Control", "no-cache");
 				response.setHeader("Pragma", "no-cache");
 				response.setDateHeader("Expires", 0);
 
 				// Finds the window within the application
 				Window window = null;
 				if (application.isRunning())
 					window = getApplicationWindow(request, application);
 
 				// Removes application if it has stopped
 				if (!application.isRunning()) {
 					endApplication(request, response, application);
 					return;
 				}
 
 				// Sets terminal type for the window, if not already set
 				if (window.getTerminal() == null) {
 					window.setTerminal(browser);
 				}
 
 				// Finds theme name
 				String themeName = window.getTheme();
 				if (request.getParameter("theme") != null) {
 					themeName = request.getParameter("theme");
 				}
 
 				// Handles resource requests
 				if (handleResourceRequest(request, response, themeName))
 					return;
 
 				writeAjaxPage(request, response, window, themeName);
 			}
 
 			// For normal requests, transform the window
 			if (download != null)
 
 				handleDownload(download, request, response);
 
 		} catch (Throwable e) {
 			// Print stacktrace
 			e.printStackTrace();
 			// Re-throw other exceptions
 			throw new ServletException(e);
 		} finally {
 
 			// Notifies transaction end
 			if (application != null)
 				((WebApplicationContext) application.getContext())
 						.endTransaction(application, request);
 		}
 	}
 
 	/**
 	 * Serve resources in ITMILL directory if requested.
 	 * 
 	 * @param request
 	 * @param response
 	 * @throws IOException
 	 */
 	private void serveStaticResourcesInITMILL(HttpServletRequest request,
 			HttpServletResponse response) throws IOException {
 		String filename = request.getPathInfo();
 		ServletContext sc = getServletContext();
 		InputStream is = sc.getResourceAsStream(filename);
 		if (is == null) {
 			// try if requested file is found from classloader
 			try {
 				// strip leading "/" otherwise stream from JAR wont work
 				filename = filename.substring(1);
 				is = this.classLoader.getResourceAsStream(filename);
 			} catch (Exception e) {
 				e.printStackTrace();
 			}
 			if (is == null) {
 				// cannot serve requested file
 				Log
 						.warn("Requested resource ["
 								+ filename
 								+ "] not found from filesystem or through class loader.");
 				response.setStatus(404);
 				return;
 			}
 		}
 		String mimetype = sc.getMimeType(filename);
 		if (mimetype != null)
 			response.setContentType(mimetype);
 		OutputStream os = response.getOutputStream();
 		byte buffer[] = new byte[20000];
 		int bytes;
 		while ((bytes = is.read(buffer)) >= 0) {
 			os.write(buffer, 0, bytes);
 		}
 	}
 
 	/**
 	 * 
 	 * @param request
 	 *            the HTTP request.
 	 * @param response
 	 *            the HTTP response to write to.
 	 * @param out
 	 * @param unhandledParameters
 	 * @param window
 	 * @param terminalType
 	 * @param theme
 	 * @throws IOException
 	 *             if the writing failed due to input/output error.
 	 * @throws MalformedURLException
 	 *             if the application is denied access the persistent data store
 	 *             represented by the given URL.
 	 */
 	private void writeAjaxPage(HttpServletRequest request,
 			HttpServletResponse response, Window window, String themeName)
 			throws IOException, MalformedURLException {
 		response.setContentType("text/html");
 		BufferedWriter page = new BufferedWriter(new OutputStreamWriter(
 				response.getOutputStream()));
 		String pathInfo = request.getPathInfo() == null ? "/" : request
 				.getPathInfo();
 		page
 				.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" "
 						+ "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
 
 		page
				.write("<html xmlns=\"http://www.w3.org/1999/xhtml\" style=\"width:100%;height:100%;border:0;margin:0;\">\n<head>\n<title>IT Mill Toolkit 5</title>\n"
 						+ "<script type=\"text/javascript\">\n"
 						+ "	var itmill = {\n" + "		appUri:'");
 
 		String[] urlParts = getApplicationUrl(request).toString().split("\\/");
 		String appUrl = "";
 		// don't use server and port in uri. It may cause problems with some
 		// virtual server configurations which lose the server name
 		for (int i = 3; i < urlParts.length; i++)
 			appUrl += "/" + urlParts[i];
 		if (appUrl.endsWith("/")) {
 			appUrl = appUrl.substring(0, appUrl.length() - 1);
 		}
 
 		page.write(appUrl);
 
 		String widgetset = this.applicationProperties
 				.getProperty(PARAMETER_WIDGETSET);
 		if (widgetset == null) {
 			widgetset = DEFAULT_WIDGETSET;
 		}
 
 		if (themeName == null)
 			themeName = "default";
 
 		page.write("', pathInfo: '" + pathInfo + "'\n};\n" + "</script>\n"
 				+ "<script language='javascript' src='" + appUrl + "/"
 				+ WIDGETSET_DIRECTORY_PATH + widgetset + "/" + widgetset
 				+ ".nocache.js'></script>\n");
 		if (!themeName.equals("default"))
 			page.write("<link REL=\"stylesheet\" TYPE=\"text/css\" HREF=\""
 					+ appUrl + "/" // TODO relative url as above?
 					+ THEME_DIRECTORY_PATH + themeName + "/styles.css\">\n");
 		page
 				.write("</head>\n<body style=\"width:100%;height:100%;border:0;margin:0\">\n"
 						+ "	<iframe id=\"__gwt_historyFrame\" style=\"width:0;height:0;border:0;overflow:hidden\"></iframe>\n"
 						+ "	<div id=\"itmill-ajax-window\" style=\"position: absolute;top:0;left:0;width:100%;height:100%;border:0;margin:0\"></div>"
 						+ "	</body>\n" + "</html>\n");
 
 		page.close();
 
 	}
 
 	/**
 	 * Handles the requested URI. An application can add handlers to do special
 	 * processing, when a certain URI is requested. The handlers are invoked
 	 * before any windows URIs are processed and if a DownloadStream is returned
 	 * it is sent to the client.
 	 * 
 	 * @param application
 	 *            the Application owning the URI.
 	 * @param request
 	 *            the HTTP request instance.
 	 * @param response
 	 *            the HTTP response to write to.
 	 * @return boolean <code>true</code> if the request was handled and
 	 *         further processing should be suppressed, <code>false</code>
 	 *         otherwise.
 	 * @see com.itmill.toolkit.terminal.URIHandler
 	 */
 	private DownloadStream handleURI(Application application,
 			HttpServletRequest request, HttpServletResponse response) {
 
 		String uri = request.getPathInfo();
 
 		// If no URI is available
 		if (uri == null || uri.length() == 0 || uri.equals("/"))
 			return null;
 
 		// Removes the leading /
 		while (uri.startsWith("/") && uri.length() > 0)
 			uri = uri.substring(1);
 
 		// Handles the uri
 		DownloadStream stream = null;
 		try {
 			stream = application.handleURI(application.getURL(), uri);
 		} catch (Throwable t) {
 			application.terminalError(new URIHandlerErrorImpl(application, t));
 		}
 
 		return stream;
 	}
 
 	/**
 	 * Handles the requested URI. An application can add handlers to do special
 	 * processing, when a certain URI is requested. The handlers are invoked
 	 * before any windows URIs are processed and if a DownloadStream is returned
 	 * it is sent to the client.
 	 * 
 	 * @param stream
 	 *            the download stream.
 	 * 
 	 * @param request
 	 *            the HTTP request instance.
 	 * @param response
 	 *            the HTTP response to write to.
 	 * 
 	 * @see com.itmill.toolkit.terminal.URIHandler
 	 */
 	private void handleDownload(DownloadStream stream,
 			HttpServletRequest request, HttpServletResponse response) {
 
 		// Download from given stream
 		InputStream data = stream.getStream();
 		if (data != null) {
 
 			// Sets content type
 			response.setContentType(stream.getContentType());
 
 			// Sets cache headers
 			long cacheTime = stream.getCacheTime();
 			if (cacheTime <= 0) {
 				response.setHeader("Cache-Control", "no-cache");
 				response.setHeader("Pragma", "no-cache");
 				response.setDateHeader("Expires", 0);
 			} else {
 				response.setHeader("Cache-Control", "max-age=" + cacheTime
 						/ 1000);
 				response.setDateHeader("Expires", System.currentTimeMillis()
 						+ cacheTime);
 				response.setHeader("Pragma", "cache"); // Required to apply
 				// caching in some
 				// Tomcats
 			}
 
 			// Copy download stream parameters directly
 			// to HTTP headers.
 			Iterator i = stream.getParameterNames();
 			if (i != null) {
 				while (i.hasNext()) {
 					String param = (String) i.next();
 					response.setHeader((String) param, stream
 							.getParameter(param));
 				}
 			}
 
 			int bufferSize = stream.getBufferSize();
 			if (bufferSize <= 0 || bufferSize > MAX_BUFFER_SIZE)
 				bufferSize = DEFAULT_BUFFER_SIZE;
 			byte[] buffer = new byte[bufferSize];
 			int bytesRead = 0;
 
 			try {
 				OutputStream out = response.getOutputStream();
 
 				while ((bytesRead = data.read(buffer)) > 0) {
 					out.write(buffer, 0, bytesRead);
 					out.flush();
 				}
 				out.close();
 			} catch (IOException ignored) {
 			}
 
 		}
 
 	}
 
 	/**
 	 * Handles theme resource file requests. Resources supplied with the themes
 	 * are provided by the WebAdapterServlet.
 	 * 
 	 * @param request
 	 *            the HTTP request.
 	 * @param response
 	 *            the HTTP response.
 	 * @return boolean <code>true</code> if the request was handled and
 	 *         further processing should be suppressed, <code>false</code>
 	 *         otherwise.
 	 * @throws ServletException
 	 *             if an exception has occurred that interferes with the
 	 *             servlet's normal operation.
 	 */
 	private boolean handleResourceRequest(HttpServletRequest request,
 			HttpServletResponse response, String themeName)
 			throws ServletException {
 
 		// If the resource path is unassigned, initialize it
 		if (resourcePath == null) {
 			resourcePath = request.getContextPath() + request.getServletPath()
 					+ RESOURCE_URI;
 			// WebSphere Application Server related fix
 			resourcePath = resourcePath.replaceAll("//", "/");
 		}
 
 		String resourceId = request.getPathInfo();
 
 		// Checks if this really is a resource request
 		if (resourceId == null || !resourceId.startsWith(RESOURCE_URI))
 			return false;
 
 		// Checks the resource type
 		resourceId = resourceId.substring(RESOURCE_URI.length());
 		InputStream data = null;
 
 		// Gets theme resources
 		try {
 			data = getServletContext().getResourceAsStream(
 					THEME_DIRECTORY_PATH + themeName + "/" + resourceId);
 		} catch (Exception e) {
 			Log.info(e.getMessage());
 			data = null;
 		}
 
 		// Writes the response
 		try {
 			if (data != null) {
 				response.setContentType(FileTypeResolver
 						.getMIMEType(resourceId));
 
 				// Use default cache time for theme resources
 				response.setHeader("Cache-Control", "max-age="
 						+ DEFAULT_THEME_CACHETIME / 1000);
 				response.setDateHeader("Expires", System.currentTimeMillis()
 						+ DEFAULT_THEME_CACHETIME);
 				response.setHeader("Pragma", "cache"); // Required to apply
 				// caching in some
 				// Tomcats
 
 				// Writes the data to client
 				byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
 				int bytesRead = 0;
 				OutputStream out = response.getOutputStream();
 				while ((bytesRead = data.read(buffer)) > 0) {
 					out.write(buffer, 0, bytesRead);
 				}
 				out.close();
 				data.close();
 			} else {
 				response.sendError(HttpServletResponse.SC_NOT_FOUND);
 			}
 
 		} catch (java.io.IOException e) {
 			Log.info("Resource transfer failed:  " + request.getRequestURI()
 					+ ". (" + e.getMessage() + ")");
 		}
 
 		return true;
 	}
 
 	/**
 	 * Gets the current application URL from request.
 	 * 
 	 * @param request
 	 *            the HTTP request.
 	 * @throws MalformedURLException
 	 *             if the application is denied access to the persistent data
 	 *             store represented by the given URL.
 	 */
 	private URL getApplicationUrl(HttpServletRequest request)
 			throws MalformedURLException {
 
 		URL applicationUrl;
 		try {
 			URL reqURL = new URL(
 					(request.isSecure() ? "https://" : "http://")
 							+ request.getServerName()
 							+ ((request.isSecure() && request.getServerPort() == 443)
 									|| (!request.isSecure() && request
 											.getServerPort() == 80) ? "" : ":"
 									+ request.getServerPort())
 							+ request.getRequestURI());
 			String servletPath = request.getContextPath()
 					+ request.getServletPath();
 			if (servletPath.length() == 0
 					|| servletPath.charAt(servletPath.length() - 1) != '/')
 				servletPath = servletPath + "/";
 			applicationUrl = new URL(reqURL, servletPath);
 		} catch (MalformedURLException e) {
 			Log.error("Error constructing application url "
 					+ request.getRequestURI() + " (" + e + ")");
 			throw e;
 		}
 
 		return applicationUrl;
 	}
 
 	/**
 	 * Gets the existing application for given request. Looks for application
 	 * instance for given request based on the requested URL.
 	 * 
 	 * @param request
 	 *            the HTTP request.
 	 * @return Application instance, or null if the URL does not map to valid
 	 *         application.
 	 * @throws MalformedURLException
 	 *             if the application is denied access to the persistent data
 	 *             store represented by the given URL.
 	 * @throws SAXException
 	 * @throws LicenseViolation
 	 * @throws InvalidLicenseFile
 	 * @throws LicenseSignatureIsInvalid
 	 * @throws LicenseFileHasNotBeenRead
 	 * @throws IllegalAccessException
 	 * @throws InstantiationException
 	 */
 	private Application getApplication(HttpServletRequest request)
 			throws MalformedURLException, SAXException, IllegalAccessException,
 			InstantiationException {
 
 		// Ensures that the session is still valid
 		HttpSession session = request.getSession(true);
 
 		// Gets application list for the session.
 		Collection applications = WebApplicationContext.getApplicationContext(
 				session).getApplications();
 
 		// Search for the application (using the application URI) from the list
 		for (Iterator i = applications.iterator(); i.hasNext();) {
 			Application a = (Application) i.next();
 			String aPath = a.getURL().getPath();
 			String servletPath = request.getContextPath()
 					+ request.getServletPath();
 			if (servletPath.length() < aPath.length())
 				servletPath += "/";
 			if (servletPath.equals(aPath)) {
 
 				// Found a running application
 				if (a.isRunning())
 					return a;
 
 				// Application has stopped, so remove it before creating a new
 				// application
 				WebApplicationContext.getApplicationContext(session)
 						.removeApplication(a);
 				break;
 			}
 		}
 
 		// Creates application, because a running one was not found
 		WebApplicationContext context = WebApplicationContext
 				.getApplicationContext(request.getSession());
 		URL applicationUrl = getApplicationUrl(request);
 
 		// Creates new application and start it
 		try {
 			Application application = (Application) this.applicationClass
 					.newInstance();
 			context.addApplication(application);
 
 			// Sets initial locale from the request
 			application.setLocale(request.getLocale());
 
 			// Starts application and check license
 			application.start(applicationUrl, this.applicationProperties,
 					context);
 
 			return application;
 
 		} catch (IllegalAccessException e) {
 			Log.error("Illegal access to application class "
 					+ this.applicationClass.getName());
 			throw e;
 		} catch (InstantiationException e) {
 			Log.error("Failed to instantiate application class: "
 					+ this.applicationClass.getName());
 			throw e;
 		}
 	}
 
 	/**
 	 * Ends the application.
 	 * 
 	 * @param request
 	 *            the HTTP request.
 	 * @param response
 	 *            the HTTP response to write to.
 	 * @param application
 	 *            the application to end.
 	 * @throws IOException
 	 *             if the writing failed due to input/output error.
 	 */
 	private void endApplication(HttpServletRequest request,
 			HttpServletResponse response, Application application)
 			throws IOException {
 
 		String logoutUrl = application.getLogoutURL();
 		if (logoutUrl == null)
 			logoutUrl = application.getURL().toString();
 
 		HttpSession session = request.getSession();
 		if (session != null) {
 			WebApplicationContext.getApplicationContext(session)
 					.removeApplication(application);
 		}
 
 		response.sendRedirect(response.encodeRedirectURL(logoutUrl));
 	}
 
 	/**
 	 * Gets the existing application or create a new one. Get a window within an
 	 * application based on the requested URI.
 	 * 
 	 * @param request
 	 *            the HTTP Request.
 	 * @param application
 	 *            the Application to query for window.
 	 * @return Window matching the given URI or null if not found.
 	 * @throws ServletException
 	 *             if an exception has occurred that interferes with the
 	 *             servlet's normal operation.
 	 */
 	private Window getApplicationWindow(HttpServletRequest request,
 			Application application) throws ServletException {
 
 		Window window = null;
 
 		// Finds the window where the request is handled
 		String path = request.getPathInfo();
 
 		// Main window as the URI is empty
 		if (path == null || path.length() == 0 || path.equals("/"))
 			window = application.getMainWindow();
 
 		// Try to search by window name
 		else {
 			String windowName = null;
 			if (path.charAt(0) == '/')
 				path = path.substring(1);
 			int index = path.indexOf('/');
 			if (index < 0) {
 				windowName = path;
 				path = "";
 			} else {
 				windowName = path.substring(0, index);
 				path = path.substring(index + 1);
 			}
 			window = application.getWindow(windowName);
 
 			if (window == null) {
 				// By default, we use main window
 				window = application.getMainWindow();
 			} else if (!window.isVisible()) {
 				// Implicitly painting without actually invoking paint()
 				window.requestRepaintRequests();
 
 				// If the window is invisible send a blank page
 				return null;
 			}
 		}
 
 		return window;
 	}
 
 	/**
 	 * Gets relative location of a theme resource.
 	 * 
 	 * @param theme
 	 *            the Theme name.
 	 * @param resource
 	 *            the Theme resource.
 	 * @return External URI specifying the resource
 	 */
 	public String getResourceLocation(String theme, ThemeResource resource) {
 
 		if (resourcePath == null)
 			return resource.getResourceId();
 		return resourcePath + theme + "/" + resource.getResourceId();
 	}
 
 	/**
 	 * Checks if web adapter is in debug mode. Extra output is generated to log
 	 * when debug mode is enabled.
 	 * 
 	 * @param parameters
 	 * @return <code>true</code> if the web adapter is in debug mode.
 	 *         otherwise <code>false</code>.
 	 */
 	public boolean isDebugMode(Map parameters) {
 		if (parameters != null) {
 			Object[] debug = (Object[]) parameters.get("debug");
 			if (debug != null && !"false".equals(debug[0].toString())
 					&& !"false".equals(debugMode))
 				return true;
 		}
 		return "true".equals(debugMode);
 	}
 
 	/**
 	 * Implementation of ParameterHandler.ErrorEvent interface.
 	 */
 	public class ParameterHandlerErrorImpl implements
 			ParameterHandler.ErrorEvent {
 
 		private ParameterHandler owner;
 
 		private Throwable throwable;
 
 		/**
 		 * Gets the contained throwable.
 		 * 
 		 * @see com.itmill.toolkit.terminal.Terminal.ErrorEvent#getThrowable()
 		 */
 		public Throwable getThrowable() {
 			return this.throwable;
 		}
 
 		/**
 		 * Gets the source ParameterHandler.
 		 * 
 		 * @see com.itmill.toolkit.terminal.ParameterHandler.ErrorEvent#getParameterHandler()
 		 */
 		public ParameterHandler getParameterHandler() {
 			return this.owner;
 		}
 
 	}
 
 	/**
 	 * Implementation of URIHandler.ErrorEvent interface.
 	 */
 	public class URIHandlerErrorImpl implements URIHandler.ErrorEvent {
 
 		private URIHandler owner;
 
 		private Throwable throwable;
 
 		/**
 		 * 
 		 * @param owner
 		 * @param throwable
 		 */
 		private URIHandlerErrorImpl(URIHandler owner, Throwable throwable) {
 			this.owner = owner;
 			this.throwable = throwable;
 		}
 
 		/**
 		 * Gets the contained throwable.
 		 * 
 		 * @see com.itmill.toolkit.terminal.Terminal.ErrorEvent#getThrowable()
 		 */
 		public Throwable getThrowable() {
 			return this.throwable;
 		}
 
 		/**
 		 * Gets the source URIHandler.
 		 * 
 		 * @see com.itmill.toolkit.terminal.URIHandler.ErrorEvent#getURIHandler()
 		 */
 		public URIHandler getURIHandler() {
 			return this.owner;
 		}
 	}
 
 	/**
 	 * Gets AJAX application manager for an application.
 	 * 
 	 * If this application has not been running in ajax mode before, new manager
 	 * is created and web adapter stops listening to changes.
 	 * 
 	 * @param application
 	 * @return AJAX Application Manager
 	 */
 	private CommunicationManager getApplicationManager(Application application) {
 		CommunicationManager mgr = (CommunicationManager) applicationToAjaxAppMgrMap
 				.get(application);
 
 		// This application is going from Web to AJAX mode, create new manager
 		if (mgr == null) {
 			// Creates new manager
 			mgr = new CommunicationManager(application, this);
 			applicationToAjaxAppMgrMap.put(application, mgr);
 
 			// Manager takes control over the application
 			mgr.takeControl();
 		}
 
 		return mgr;
 	}
 
 	/**
 	 * Gets resource path using different implementations. Required fo
 	 * supporting different servlet container implementations (application
 	 * servers).
 	 * 
 	 * @param servletContext
 	 * @param path
 	 *            the resource path.
 	 * @return the resource path.
 	 */
 	protected static String getResourcePath(ServletContext servletContext,
 			String path) {
 		String resultPath = null;
 		resultPath = servletContext.getRealPath(path);
 		if (resultPath != null) {
 			return resultPath;
 		} else {
 			try {
 				URL url = servletContext.getResource(path);
 				resultPath = url.getFile();
 			} catch (Exception e) {
 				// ignored
 			}
 		}
 		return resultPath;
 	}
 
 }
