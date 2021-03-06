 // ========================================================================
 // Copyright (c) 2009 Intalio, Inc.
 // ------------------------------------------------------------------------
 // All rights reserved. This program and the accompanying materials
 // are made available under the terms of the Eclipse Public License v1.0
 // and Apache License v2.0 which accompanies this distribution.
 // The Eclipse Public License is available at 
 // http://www.eclipse.org/legal/epl-v10.html
 // The Apache License v2.0 is available at
 // http://www.opensource.org/licenses/apache2.0.php
 // You may elect to redistribute this code under either of these licenses. 
 // Contributors:
 //    Hugues Malphettes - initial API and implementation
 // ========================================================================
 package org.eclipse.jetty.osgi.boot.internal.webapp;
 
 import java.io.BufferedInputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLClassLoader;
 import java.util.ArrayList;
 import java.util.Collection;
import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.StringTokenizer;
 import java.util.jar.JarFile;
 import java.util.zip.ZipEntry;
 
 import org.eclipse.jetty.deploy.AppProvider;
 import org.eclipse.jetty.deploy.ContextDeployer;
 import org.eclipse.jetty.deploy.DeploymentManager;
 import org.eclipse.jetty.deploy.WebAppDeployer;
 import org.eclipse.jetty.osgi.boot.JettyBootstrapActivator;
 import org.eclipse.jetty.osgi.boot.OSGiAppProvider;
 import org.eclipse.jetty.osgi.boot.OSGiWebappConstants;
 import org.eclipse.jetty.osgi.boot.internal.jsp.TldLocatableURLClassloader;
 import org.eclipse.jetty.osgi.boot.utils.BundleClassLoaderHelper;
 import org.eclipse.jetty.osgi.boot.utils.BundleFileLocatorHelper;
 import org.eclipse.jetty.osgi.boot.utils.WebappRegistrationCustomizer;
 import org.eclipse.jetty.osgi.boot.utils.internal.DefaultBundleClassLoaderHelper;
 import org.eclipse.jetty.osgi.boot.utils.internal.DefaultFileLocatorHelper;
 import org.eclipse.jetty.server.Handler;
 import org.eclipse.jetty.server.Server;
 import org.eclipse.jetty.server.handler.ContextHandler;
 import org.eclipse.jetty.server.handler.ContextHandlerCollection;
 import org.eclipse.jetty.server.handler.DefaultHandler;
 import org.eclipse.jetty.server.handler.HandlerCollection;
 import org.eclipse.jetty.server.handler.RequestLogHandler;
 import org.eclipse.jetty.server.nio.SelectChannelConnector;
 import org.eclipse.jetty.util.log.Log;
 import org.eclipse.jetty.util.log.Logger;
 import org.eclipse.jetty.util.resource.Resource;
 import org.eclipse.jetty.webapp.WebAppContext;
 import org.eclipse.jetty.xml.XmlConfiguration;
 import org.osgi.framework.Bundle;
 import org.osgi.framework.BundleContext;
 import org.xml.sax.SAXException;
 import org.xml.sax.SAXParseException;
 
 /**
  * Bridges the jetty deployers with the OSGi lifecycle where applications are
  * managed inside OSGi-bundles.
  * <p>
  * This class should be called as a consequence of the activation of a new
  * service that is a ContextHandler.<br/>
  * This way the new webapps are exposed as OSGi services.
  * </p>
  * <p>
  * Helper methods to register a bundle that is a web-application or a context.
  * </p>
  * Limitations:
  * <ul>
  * <li>support for jarred webapps is somewhat limited.</li>
  * </ul>
  */
 public class WebappRegistrationHelper
 {
 
     private static Logger __logger = Log.getLogger(WebappRegistrationHelper.class.getName());
 
     private static boolean INITIALIZED = false;
 
     /**
      * By default set to: {@link DefaultBundleClassLoaderHelper}. It supports
      * equinox and apache-felix fragment bundles that are specific to an OSGi
      * implementation should set a different implementation.
      */
     public static BundleClassLoaderHelper BUNDLE_CLASS_LOADER_HELPER = null;
     /**
      * By default set to: {@link DefaultBundleClassLoaderHelper}. It supports
      * equinox and apache-felix fragment bundles that are specific to an OSGi
      * implementation should set a different implementation.
      */
     public static BundleFileLocatorHelper BUNDLE_FILE_LOCATOR_HELPER = null;
 
     /**
      * By default set to: {@link DefaultBundleClassLoaderHelper}. It supports
      * equinox and apache-felix fragment bundles that are specific to an OSGi
      * implementation should set a different implementation.
      * <p>
      * Several of those objects can be added here: For example we could have an optional fragment that setups
      * a specific implementation of JSF for the whole of jetty-osgi.
      * </p>
      */
     public static Collection<WebappRegistrationCustomizer> JSP_REGISTRATION_HELPERS = new ArrayList<WebappRegistrationCustomizer>();
 
     private Server _server;
     private ContextHandlerCollection _ctxtHandler;
 
     /**
      * this class loader loads the jars inside {$jetty.home}/lib/ext it is meant
      * as a migration path and for jars that are not OSGi ready. also gives
      * access to the jsp jars.
      */
     // private URLClassLoader _libExtClassLoader;
 
     /**
      * This is the class loader that should be the parent classloader of any
      * webapp classloader. It is in fact the _libExtClassLoader with a trick to
      * let the TldScanner find the jars where the tld files are.
      */
     private URLClassLoader _commonParentClassLoaderForWebapps;
 
     private DeploymentManager _deploymentManager;
 
     private OSGiAppProvider _provider;
 
     public WebappRegistrationHelper(Server server)
     {
         _server = server;
         staticInit();
     }
 
     // Inject the customizing classes that might be defined in fragment bundles.
     private static synchronized void staticInit()
     {
         if (!INITIALIZED)
         {
             INITIALIZED = true;
             // setup the custom BundleClassLoaderHelper
             try
             {
                 BUNDLE_CLASS_LOADER_HELPER = (BundleClassLoaderHelper)Class.forName(BundleClassLoaderHelper.CLASS_NAME).newInstance();
             }
             catch (Throwable t)
             {
                 // System.err.println("support for equinox and felix");
                 BUNDLE_CLASS_LOADER_HELPER = new DefaultBundleClassLoaderHelper();
             }
             // setup the custom FileLocatorHelper
             try
             {
                 BUNDLE_FILE_LOCATOR_HELPER = (BundleFileLocatorHelper)Class.forName(BundleFileLocatorHelper.CLASS_NAME).newInstance();
             }
             catch (Throwable t)
             {
                 // System.err.println("no jsp/jasper support");
                 BUNDLE_FILE_LOCATOR_HELPER = new DefaultFileLocatorHelper();
             }
         }
     }
 
     /**
      * Removes quotes around system property values before we try to make them
      * into file pathes.
      */
     public static String stripQuotesIfPresent(String filePath)
     {
         if (filePath == null)
             return null;
 
         if ((filePath.startsWith("\"") || filePath.startsWith("'")) && (filePath.endsWith("\"") || filePath.endsWith("'")))
             return filePath.substring(1,filePath.length() - 1);
         return filePath;
     }
     
     /**
      * Look for the home directory of jetty as defined by the system property
      * 'jetty.home'. If undefined, look at the current bundle and uses its own
      * jettyhome folder for this feature.
      * <p>
      * Special case: inside eclipse-SDK:<br/>
      * If the bundle is jarred, see if we are inside eclipse-PDE itself. In that
      * case, look for the installation directory of eclipse-PDE, try to create a
      * jettyhome folder there and install the sample jettyhome folder at that
      * location. This makes the installation in eclipse-SDK easier. <br/>
      * This is a bit redundant with the work done by the jetty configuration
      * launcher.
      * </p>
      * 
      * @param context
      * @throws Exception
      */
     public void setup(BundleContext context, Map<String, String> configProperties) throws Exception
     {
    	Enumeration<?> enUrls = context.getBundle().findEntries("/etc", "jetty.xml", false);System.err.println();
    	if (enUrls.hasMoreElements())
    	{
	    	URL url = (URL) enUrls.nextElement();
	    	if (url != null)
	    	{
	    		//bug 317231: there is a fragment that defines the jetty configuration file.
	    		//let's use that as the jetty home.
	    		url = DefaultFileLocatorHelper.getLocalURL(url);
	    		if (url.getProtocol().equals("file"))
	    		{
	    			//ok good.
	    			File jettyxml = new File(url.toURI());
	    			File jettyhome = jettyxml.getParentFile().getParentFile();
	    			System.setProperty("jetty.home", jettyhome.getAbsolutePath());
	    		}
	    	}
    	}
    	File _installLocation = BUNDLE_FILE_LOCATOR_HELPER.getBundleInstallLocation(context.getBundle());
         // debug:
         // new File("~/proj/eclipse-install/eclipse-3.5.1-SDK-jetty7/" +
         // "dropins/jetty7/plugins/org.eclipse.jetty.osgi.boot_0.0.1.001-SNAPSHOT.jar");
         boolean bootBundleCanBeJarred = true;
         String jettyHome = stripQuotesIfPresent(System.getProperty("jetty.home"));
 
         if (jettyHome == null || jettyHome.length() == 0)
         {
             if (_installLocation.getName().endsWith(".jar"))
             {
                 jettyHome = JettyHomeHelper.setupJettyHomeInEclipsePDE(_installLocation);
             }
             if (jettyHome == null)
             {
                 jettyHome = _installLocation.getAbsolutePath() + "/jettyhome";
                 bootBundleCanBeJarred = false;
             }
         }
         // in case we stripped the quotes.
         System.setProperty("jetty.home",jettyHome);
 
         String jettyLogs = stripQuotesIfPresent(System.getProperty("jetty.logs"));
         if (jettyLogs == null || jettyLogs.length() == 0)
         {
             System.setProperty("jetty.logs",jettyHome + "/logs");
         }
 
         if (!bootBundleCanBeJarred && !_installLocation.isDirectory())
         {
             String install = _installLocation != null?_installLocation.getCanonicalPath():" unresolved_install_location";
             throw new IllegalArgumentException("The system property -Djetty.home" + " must be set to a directory or the bundle "
                     + context.getBundle().getSymbolicName() + " installed here " + install + " must be unjarred.");
         }
         try
         {
             System.err.println("JETTY_HOME set to " + new File(jettyHome).getCanonicalPath());
         }
         catch (Throwable t)
         {
             System.err.println("JETTY_HOME _set to " + new File(jettyHome).getAbsolutePath());
         }
 
         ClassLoader contextCl = Thread.currentThread().getContextClassLoader();
         try
         {
 
             // passing this bundle's classloader as the context classlaoder
             // makes sure there is access to all the jetty's bundles
 
             File jettyHomeF = new File(jettyHome);
             URLClassLoader libExtClassLoader = null;
             try
             {
             	libExtClassLoader = LibExtClassLoaderHelper.createLibEtcClassLoaderHelper(jettyHomeF,_server,
             			JettyBootstrapActivator.class.getClassLoader());
             }
             catch (MalformedURLException e)
             {
                 e.printStackTrace();
             }
 
             Thread.currentThread().setContextClassLoader(libExtClassLoader);
 
             String jettyetc = System.getProperty(OSGiWebappConstants.SYS_PROP_JETTY_ETC_FILES,"etc/jetty.xml");
             StringTokenizer tokenizer = new StringTokenizer(jettyetc,";,");
             
             Map<Object,Object> id_map = new HashMap<Object,Object>();
             id_map.put("Server",_server);
             Map<Object,Object> properties = new HashMap<Object,Object>();
             properties.put("jetty.home",jettyHome);
             properties.put("jetty.host",System.getProperty("jetty.host",""));
             properties.put("jetty.port",System.getProperty("jetty.port","8080"));
             properties.put("jetty.port.ssl",System.getProperty("jetty.port.ssl","8443"));
 
             while (tokenizer.hasMoreTokens())
             {
                 String etcFile = tokenizer.nextToken().trim();
                 File conffile = etcFile.startsWith("/")?new File(etcFile):new File(jettyHomeF,etcFile);
                 if (!conffile.exists())
                 {
                     __logger.warn("Unable to resolve the jetty/etc file " + etcFile);
 
                     if ("etc/jetty.xml".equals(etcFile))
                     {
                         // Missing jetty.xml file, so create a minimal Jetty configuration
                         __logger.info("Configuring default server on 8080");
                         SelectChannelConnector connector = new SelectChannelConnector();
                         connector.setPort(8080);
                         _server.addConnector(connector);
 
                         HandlerCollection handlers = new HandlerCollection();
                         ContextHandlerCollection contexts = new ContextHandlerCollection();
                         RequestLogHandler requestLogHandler = new RequestLogHandler();
                         handlers.setHandlers(new Handler[] { contexts, new DefaultHandler(), requestLogHandler });
                         _server.setHandler(handlers);
                     }
                 }
                 else
                 {
                     try
                     {
                         // Execute a Jetty configuration file
                         XmlConfiguration config = new XmlConfiguration(new FileInputStream(conffile));
                         config.setIdMap(id_map);
                         config.setProperties(properties);
                         config.configure();
                         id_map=config.getIdMap();
                     }
                     catch (SAXParseException saxparse)
                     {
                         Log.getLogger(WebappRegistrationHelper.class.getName()).warn("Unable to configure the jetty/etc file " + etcFile,saxparse);
                         throw saxparse;
                     }
                 }
             }
 
             init();
 
             //now that we have an app provider we can call the registration customizer.
             try
             {
                 URL[] jarsWithTlds = getJarsWithTlds();
                 _commonParentClassLoaderForWebapps = jarsWithTlds == null?libExtClassLoader:new TldLocatableURLClassloader(libExtClassLoader,getJarsWithTlds());
             }
             catch (MalformedURLException e)
             {
                 e.printStackTrace();
             }
 
             
             _server.start();
         }
         catch (Throwable t)
         {
             t.printStackTrace();
         }
         finally
         {
             Thread.currentThread().setContextClassLoader(contextCl);
         }
 
     }
 
     /**
      * Must be called after the server is configured.
      * 
      * Locate the actual instance of the ContextDeployer and WebAppDeployer that
      * was created when configuring the server through jetty.xml. If there is no
      * such thing it won't be possible to deploy webapps from a context and we
      * throw IllegalStateExceptions.
      */
     private void init()
     {
         // Get the context handler
         _ctxtHandler = (ContextHandlerCollection)_server.getChildHandlerByClass(ContextHandlerCollection.class);
         
         // get a deployerManager
         List<DeploymentManager> deployers = _server.getBeans(DeploymentManager.class);
         if (deployers != null && !deployers.isEmpty())
         {
             _deploymentManager = deployers.get(0);
             
             for (AppProvider provider : _deploymentManager.getAppProviders())
             {
                 if (provider instanceof OSGiAppProvider)
                 {
                     _provider=(OSGiAppProvider)provider;
                     break;
                 }
             }
             if (_provider == null)
             {
             	//create it on the fly with reasonable default values.
             	try
             	{
 					_provider = new OSGiAppProvider();
 					_provider.setMonitoredDir(
 							Resource.newResource(getDefaultOSGiContextsHome(
 									new File(System.getProperty("jetty.home"))).toURI()));
 				} catch (IOException e) {
 					e.printStackTrace();
 				}
             	_deploymentManager.addAppProvider(_provider);
             }
         }
 
         if (_ctxtHandler == null || _provider==null)
             throw new IllegalStateException("ERROR: No ContextHandlerCollection or OSGiAppProvider configured");
         
 
     }
 
     /**
      * Deploy a new web application on the jetty server.
      * 
      * @param bundle
      *            The bundle
      * @param webappFolderPath
      *            The path to the root of the webapp. Must be a path relative to
      *            bundle; either an absolute path.
      * @param contextPath
      *            The context path. Must start with "/"
      * @param extraClasspath
      * @param overrideBundleInstallLocation
      * @param webXmlPath
      * @param defaultWebXmlPath
      *            TODO: parameter description
      * @return The contexthandler created and started
      * @throws Exception
      */
     public ContextHandler registerWebapplication(Bundle bundle, String webappFolderPath, String contextPath, String extraClasspath,
             String overrideBundleInstallLocation, String webXmlPath, String defaultWebXmlPath) throws Exception
     {
         File bundleInstall = overrideBundleInstallLocation == null?BUNDLE_FILE_LOCATOR_HELPER.getBundleInstallLocation(bundle):new File(
                 overrideBundleInstallLocation);
         File webapp = null;
         if (webappFolderPath != null && webappFolderPath.length() != 0 && !webappFolderPath.equals("."))
         {
             if (webappFolderPath.startsWith("/") || webappFolderPath.startsWith("file:/"))
             {
                 webapp = new File(webappFolderPath);
             }
             else
             {
                 webapp = new File(bundleInstall,webappFolderPath);
             }
         }
         else
         {
             webapp = bundleInstall;
         }
         if (!webapp.exists())
         {
             throw new IllegalArgumentException("Unable to locate " + webappFolderPath + " inside "
                     + (bundleInstall != null?bundleInstall.getAbsolutePath():"unlocated bundle '" + bundle.getSymbolicName() + "'"));
         }
         return registerWebapplication(bundle,webapp,contextPath,extraClasspath,bundleInstall,webXmlPath,defaultWebXmlPath);
     }
 
     /**
      * TODO: refactor this into the createContext method of OSGiAppProvider.
      * @see WebAppDeployer#scan()
 
      * @param contributor
      * @param webapp
      * @param contextPath
      * @param extraClasspath
      * @param bundleInstall
      * @param webXmlPath
      * @param defaultWebXmlPath
      * @return The contexthandler created and started
      * @throws Exception
      */
     public ContextHandler registerWebapplication(Bundle contributor, File webapp, String contextPath, String extraClasspath, File bundleInstall,
             String webXmlPath, String defaultWebXmlPath) throws Exception
     {
 
         ClassLoader contextCl = Thread.currentThread().getContextClassLoader();
         String[] oldServerClasses = null;
         WebAppContext context = null;
         try
         {
             // make sure we provide access to all the jetty bundles by going
             // through this bundle.
             OSGiWebappClassLoader composite = createWebappClassLoader(contributor);
             // configure with access to all jetty classes and also all the classes
             // that the contributor gives access to.
             Thread.currentThread().setContextClassLoader(composite);
 
             context = new WebAppContext(webapp.getAbsolutePath(),contextPath);
             context.setExtraClasspath(extraClasspath);
 
             if (webXmlPath != null && webXmlPath.length() != 0)
             {
                 File webXml = null;
                 if (webXmlPath.startsWith("/") || webXmlPath.startsWith("file:/"))
                 {
                     webXml = new File(webXmlPath);
                 }
                 else
                 {
                     webXml = new File(bundleInstall,webXmlPath);
                 }
                 if (webXml.exists())
                 {
                     context.setDescriptor(webXml.getAbsolutePath());
                 }
             }
 
             if (defaultWebXmlPath == null || defaultWebXmlPath.length() == 0)
             {
             	//use the one defined by the OSGiAppProvider.
             	defaultWebXmlPath = _provider.getDefaultsDescriptor();
             }
             if (defaultWebXmlPath != null && defaultWebXmlPath.length() != 0)
             {
                 File defaultWebXml = null;
                 if (defaultWebXmlPath.startsWith("/") || defaultWebXmlPath.startsWith("file:/"))
                 {
                     defaultWebXml = new File(webXmlPath);
                 }
                 else
                 {
                     defaultWebXml = new File(bundleInstall,defaultWebXmlPath);
                 }
                 if (defaultWebXml.exists())
                 {
                     context.setDefaultsDescriptor(defaultWebXml.getAbsolutePath());
                 }
             }
             
             //other parameters that might be defines on the OSGiAppProvider:
             context.setParentLoaderPriority(_provider.isParentLoaderPriority());
 
             configureWebAppContext(context,contributor);
             configureWebappClassLoader(contributor,context,composite);
 
             // @see
             // org.eclipse.jetty.webapp.JettyWebXmlConfiguration#configure(WebAppContext)
             // during initialization of the webapp all the jetty packages are
             // visible
             // through the webapp classloader.
             oldServerClasses = context.getServerClasses();
             context.setServerClasses(null);
             _provider.addContext(context);
 
             return context;
         }
         finally
         {
             if (context != null && oldServerClasses != null)
             {
                 context.setServerClasses(oldServerClasses);
             }
             Thread.currentThread().setContextClassLoader(contextCl);
         }
 
     }
 
     /**
      * Stop a ContextHandler and remove it from the collection.
      * 
      * @see ContextDeployer#undeploy
      * @param contextHandler
      * @throws Exception
      */
     public void unregister(ContextHandler contextHandler) throws Exception
     {
         contextHandler.stop();
         _ctxtHandler.removeHandler(contextHandler);
     }
 
     /**
      * @return The default folder in which the context files of the osgi bundles
      *         are located and watched. Or null when the system property
      *         "jetty.osgi.contexts.home" is not defined.
      *         If the configuration file defines the OSGiAppProvider's context.
      *         This will not be taken into account.
      */
     File getDefaultOSGiContextsHome(File jettyHome)
     {
         String jettyContextsHome = System.getProperty("jetty.osgi.contexts.home");
         if (jettyContextsHome != null)
         {
             File contextsHome = new File(jettyContextsHome);
             if (!contextsHome.exists() || !contextsHome.isDirectory())
             {
                 throw new IllegalArgumentException("the ${jetty.osgi.contexts.home} '" + jettyContextsHome + " must exist and be a folder");
             }
             return contextsHome;
         }
         return new File(jettyHome, "/contexts");
     }
     
     File getOSGiContextsHome()
     {
     	return _provider.getContextXmlDirAsFile();
     }
 
     /**
      * This type of registration relies on jetty's complete context xml file.
      * Context encompasses jndi and all other things. This makes the definition
      * of the webapp a lot more self-contained.
      * 
      * @param contributor
      * @param contextFileRelativePath
      * @param extraClasspath
      * @param overrideBundleInstallLocation
      * @return The contexthandler created and started
      * @throws Exception
      */
     public ContextHandler registerContext(Bundle contributor, String contextFileRelativePath, String extraClasspath, String overrideBundleInstallLocation)
             throws Exception
     {
         File contextsHome = _provider.getContextXmlDirAsFile();
         if (contextsHome != null)
         {
             File prodContextFile = new File(contextsHome,contributor.getSymbolicName() + "/" + contextFileRelativePath);
             if (prodContextFile.exists())
             {
                 return registerContext(contributor,prodContextFile,extraClasspath,overrideBundleInstallLocation);
             }
         }
         File contextFile = overrideBundleInstallLocation != null?new File(overrideBundleInstallLocation,contextFileRelativePath):new File(
                 BUNDLE_FILE_LOCATOR_HELPER.getBundleInstallLocation(contributor),contextFileRelativePath);
         if (contextFile.exists())
         {
             return registerContext(contributor,contextFile,extraClasspath,overrideBundleInstallLocation);
         }
         else
         {
             if (contextFileRelativePath.startsWith("./"))
             {
                 contextFileRelativePath = contextFileRelativePath.substring(1);
             }
             if (!contextFileRelativePath.startsWith("/"))
             {
                 contextFileRelativePath = "/" + contextFileRelativePath;
             }
             if (overrideBundleInstallLocation == null)
             {
                 URL contextURL = contributor.getEntry(contextFileRelativePath);
                 if (contextURL != null)
                 {
                     return registerContext(contributor,contextURL.openStream(),extraClasspath,overrideBundleInstallLocation);
                 }
             }
             else
             {
                 JarFile zipFile = null;
                 try
                 {
                     zipFile = new JarFile(overrideBundleInstallLocation);
                     ZipEntry entry = zipFile.getEntry(contextFileRelativePath.substring(1));
                     return registerContext(contributor,zipFile.getInputStream(entry),extraClasspath,overrideBundleInstallLocation);
                 }
                 catch (Throwable t)
                 {
 
                 }
                 finally
                 {
                     if (zipFile != null)
                         try
                         {
                             zipFile.close();
                         }
                         catch (IOException ioe)
                         {
                         }
                 }
             }
             throw new IllegalArgumentException("Could not find the context " + "file " + contextFileRelativePath + " for the bundle "
                     + contributor.getSymbolicName() + (overrideBundleInstallLocation != null?" using the install location " + overrideBundleInstallLocation:""));
         }
     }
 
     /**
      * This type of registration relies on jetty's complete context xml file.
      * Context encompasses jndi and all other things. This makes the definition
      * of the webapp a lot more self-contained.
      * 
      * @param webapp
      * @param contextPath
      * @param classInBundle
      * @throws Exception
      */
     private ContextHandler registerContext(Bundle contributor, File contextFile, String extraClasspath, String overrideBundleInstallLocation) throws Exception
     {
         InputStream contextFileInputStream = null;
         try
         {
             contextFileInputStream = new BufferedInputStream(new FileInputStream(contextFile));
             return registerContext(contributor,contextFileInputStream,extraClasspath,overrideBundleInstallLocation);
         }
         finally
         {
             if (contextFileInputStream != null)
                 try
                 {
                     contextFileInputStream.close();
                 }
                 catch (IOException ioe)
                 {
                 }
         }
     }
 
     /**
      * @param contributor
      * @param contextFileInputStream
      * @return The ContextHandler created and registered or null if it did not
      *         happen.
      * @throws Exception
      */
     private ContextHandler registerContext(Bundle contributor, InputStream contextFileInputStream, String extraClasspath, String overrideBundleInstallLocation)
             throws Exception
     {
         ClassLoader contextCl = Thread.currentThread().getContextClassLoader();
         String[] oldServerClasses = null;
         WebAppContext webAppContext = null;
         try
         {
             // make sure we provide access to all the jetty bundles by going
             // through this bundle.
             OSGiWebappClassLoader composite = createWebappClassLoader(contributor);
             // configure with access to all jetty classes and also all the
             // classes
             // that the contributor gives access to.
             Thread.currentThread().setContextClassLoader(composite);
             ContextHandler context = createContextHandler(contributor,contextFileInputStream,extraClasspath,overrideBundleInstallLocation);
             if (context == null)
             {
                 return null;// did not happen
             }
 
             // ok now register this webapp. we checked when we started jetty
             // that there
             // was at least one such handler for webapps.
             //the actual registration must happen via the new Deployment API.
 //            _ctxtHandler.addHandler(context);
 
             configureWebappClassLoader(contributor,context,composite);
             if (context instanceof WebAppContext)
             {
                 webAppContext = (WebAppContext)context;
                 // @see
                 // org.eclipse.jetty.webapp.JettyWebXmlConfiguration#configure(WebAppContext)
                 oldServerClasses = webAppContext.getServerClasses();
                 webAppContext.setServerClasses(null);
             }
 
              _provider.addContext(context);
             return context;
         }
         finally
         {
             if (webAppContext != null)
             {
                 webAppContext.setServerClasses(oldServerClasses);
             }
             Thread.currentThread().setContextClassLoader(contextCl);
         }
 
     }
 
     /**
      * TODO: right now only the jetty-jsp bundle is scanned for common taglibs.
      * Should support a way to plug more bundles that contain taglibs.
      * 
      * The jasper TldScanner expects a URLClassloader to parse a jar for the
      * /META-INF/*.tld it may contain. We place the bundles that we know contain
      * such tag-libraries. Please note that it will work if and only if the
      * bundle is a jar (!) Currently we just hardcode the bundle that contains
      * the jstl implemenation.
      * 
      * A workaround when the tld cannot be parsed with this method is to copy
      * and paste it inside the WEB-INF of the webapplication where it is used.
      * 
      * Support only 2 types of packaging for the bundle: - the bundle is a jar
      * (recommended for runtime.) - the bundle is a folder and contain jars in
      * the root and/or in the lib folder (nice for PDE developement situations)
      * Unsupported: the bundle is a jar that embeds more jars.
      * 
      * @return
      * @throws Exception
      */
     private URL[] getJarsWithTlds() throws Exception
     {
         ArrayList<URL> res = new ArrayList<URL>();
         for (WebappRegistrationCustomizer regCustomizer : JSP_REGISTRATION_HELPERS)
         {
             URL[] urls = regCustomizer.getJarsWithTlds(_provider, BUNDLE_FILE_LOCATOR_HELPER);
             for (URL url : urls)
             {
                 if (!res.contains(url))
                     res.add(url);
             }
         }
         if (!res.isEmpty())
             return res.toArray(new URL[res.size()]);
         else
             return null;
     }
 
     /**
      * Applies the properties of WebAppDeployer as defined in jetty.xml.
      * 
      * @see {WebAppDeployer#scan} around the comment
      *      <code>// configure it</code>
      */
     protected void configureWebAppContext(WebAppContext wah, Bundle contributor)
     {
         // rfc66
         wah.setAttribute(OSGiWebappConstants.RFC66_OSGI_BUNDLE_CONTEXT,contributor.getBundleContext());
 
         //spring-dm-1.2.1 looks for the BundleContext as a different attribute.
         //not a spec... but if we want to support 
         //org.springframework.osgi.web.context.support.OsgiBundleXmlWebApplicationContext
         //then we need to do this to:
         wah.setAttribute("org.springframework.osgi.web." + BundleContext.class.getName(),
                         contributor.getBundleContext());
         
     }
 
     /**
      * @See {@link ContextDeployer#scan}
      * @param contextFile
      * @return
      */
     protected ContextHandler createContextHandler(Bundle bundle, File contextFile, String extraClasspath, String overrideBundleInstallLocation)
     {
         try
         {
             return createContextHandler(bundle,new BufferedInputStream(new FileInputStream(contextFile)),extraClasspath,overrideBundleInstallLocation);
         }
         catch (FileNotFoundException e)
         {
             e.printStackTrace();
         }
         return null;
     }
 
     /**
      * @See {@link ContextDeployer#scan}
      * @param contextFile
      * @return
      */
     @SuppressWarnings("unchecked")
     protected ContextHandler createContextHandler(Bundle bundle, InputStream contextInputStream, String extraClasspath, String overrideBundleInstallLocation)
     {
         /*
          * Do something identical to what the ContextProvider would have done:
          * XmlConfiguration xmlConfiguration=new
          * XmlConfiguration(resource.getURL()); HashMap properties = new
          * HashMap(); properties.put("Server", _contexts.getServer()); if
          * (_configMgr!=null) properties.putAll(_configMgr.getProperties());
          * 
          * xmlConfiguration.setProperties(properties); ContextHandler
          * context=(ContextHandler)xmlConfiguration.configure();
          * context.setAttributes(new AttributesMap(_contextAttributes));
          */
         try
         {
             XmlConfiguration xmlConfiguration = new XmlConfiguration(contextInputStream);
             HashMap properties = new HashMap();
             properties.put("Server",_server);
             
             // insert the bundle's location as a property.
             setThisBundleHomeProperty(bundle,properties,overrideBundleInstallLocation);
             xmlConfiguration.setProperties(properties);
 
             ContextHandler context = (ContextHandler)xmlConfiguration.configure();
             if (context instanceof WebAppContext)
             {
                 ((WebAppContext)context).setExtraClasspath(extraClasspath);
                 ((WebAppContext)context).setParentLoaderPriority(_provider.isParentLoaderPriority());
                 if (_provider.getDefaultsDescriptor() != null && _provider.getDefaultsDescriptor().length() != 0)
                 {
                 	((WebAppContext)context).setDefaultsDescriptor(_provider.getDefaultsDescriptor());
                 }
             }
 
             // rfc-66:
             context.setAttribute(OSGiWebappConstants.RFC66_OSGI_BUNDLE_CONTEXT,bundle.getBundleContext());
 
             //spring-dm-1.2.1 looks for the BundleContext as a different attribute.
             //not a spec... but if we want to support 
             //org.springframework.osgi.web.context.support.OsgiBundleXmlWebApplicationContext
             //then we need to do this to:
             context.setAttribute("org.springframework.osgi.web." + BundleContext.class.getName(),
                             bundle.getBundleContext());
             return context;
         }
         catch (FileNotFoundException e)
         {
             return null;
         }
         catch (SAXException e)
         {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
         catch (IOException e)
         {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
         catch (Throwable e)
         {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
         finally
         {
             if (contextInputStream != null)
                 try
                 {
                     contextInputStream.close();
                 }
                 catch (IOException ioe)
                 {
                 }
         }
         return null;
     }
 
     /**
      * Configure a classloader onto the context. If the context is a
      * WebAppContext, build a WebAppClassLoader that has access to all the jetty
      * classes thanks to the classloader of the JettyBootStrapper bundle and
      * also has access to the classloader of the bundle that defines this
      * context.
      * <p>
      * If the context is not a WebAppContext, same but with a simpler
      * URLClassLoader. Note that the URLClassLoader is pretty much fake: it
      * delegate all actual classloading to the parent classloaders.
      * </p>
      * <p>
      * The URL[] returned by the URLClassLoader create contained specifically
      * the jars that some j2ee tools expect and look into. For example the jars
      * that contain tld files for jasper's jstl support.
      * </p>
      * <p>
      * Also as the jars in the lib folder and the classes in the classes folder
      * might already be in the OSGi classloader we filter them out of the
      * WebAppClassLoader
      * </p>
      * 
      * @param context
      * @param contributor
      * @param webapp
      * @param contextPath
      * @param classInBundle
      * @throws Exception
      */
     protected void configureWebappClassLoader(Bundle contributor, ContextHandler context, OSGiWebappClassLoader webappClassLoader) throws Exception
     {
         if (context instanceof WebAppContext)
         {
             WebAppContext webappCtxt = (WebAppContext)context;
             context.setClassLoader(webappClassLoader);
             webappClassLoader.setWebappContext(webappCtxt);
         }
         else
         {
             context.setClassLoader(webappClassLoader);
         }
     }
 
     /**
      * No matter what the type of webapp, we create a WebappClassLoader.
      */
     protected OSGiWebappClassLoader createWebappClassLoader(Bundle contributor) throws Exception
     {
         // we use a temporary WebAppContext object.
         // if this is a real webapp we will set it on it a bit later: once we
         // know.
         OSGiWebappClassLoader webappClassLoader = new OSGiWebappClassLoader(_commonParentClassLoaderForWebapps,new WebAppContext(),contributor);
         return webappClassLoader;
     }
 
     /**
      * Set the property &quot;this.bundle.install&quot; to point to the location
      * of the bundle. Useful when <SystemProperty name="this.bundle.home"/> is
      * used.
      */
     private void setThisBundleHomeProperty(Bundle bundle, HashMap<String, Object> properties, String overrideBundleInstallLocation)
     {
         try
         {
             File location = overrideBundleInstallLocation != null?new File(overrideBundleInstallLocation):BUNDLE_FILE_LOCATOR_HELPER
                     .getBundleInstallLocation(bundle);
             properties.put("this.bundle.install",location.getCanonicalPath());
         }
         catch (Throwable t)
         {
             System.err.println("Unable to set 'this.bundle.install' " + " for the bundle " + bundle.getSymbolicName());
             t.printStackTrace();
         }
     }
 
 
 }
