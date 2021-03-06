 /**
  * Copyright 2012 Ralph Schaer <ralphschaer@gmail.com>
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package ch.rasc.embeddedtc;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.net.ServerSocket;
 import java.net.Socket;
 import java.net.URL;
 import java.net.UnknownHostException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.TimeUnit;
 
 import javax.servlet.ServletException;
 import javax.xml.parsers.DocumentBuilder;
 import javax.xml.parsers.DocumentBuilderFactory;
 import javax.xml.parsers.ParserConfigurationException;
 
 import org.apache.catalina.Context;
 import org.apache.catalina.Lifecycle;
 import org.apache.catalina.LifecycleEvent;
 import org.apache.catalina.LifecycleException;
 import org.apache.catalina.LifecycleListener;
 import org.apache.catalina.Server;
 import org.apache.catalina.connector.Connector;
 import org.apache.catalina.core.AprLifecycleListener;
 import org.apache.catalina.core.JasperListener;
 import org.apache.catalina.core.JreMemoryLeakPreventionListener;
 import org.apache.catalina.core.ThreadLocalLeakPreventionListener;
 import org.apache.catalina.deploy.ContextEnvironment;
 import org.apache.catalina.deploy.ContextResource;
 import org.apache.catalina.deploy.NamingResources;
 import org.apache.catalina.mbeans.GlobalResourcesLifecycleListener;
 import org.apache.catalina.session.StandardManager;
 import org.apache.catalina.startup.Tomcat;
 import org.apache.juli.logging.Log;
 import org.apache.juli.logging.LogFactory;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 import org.xml.sax.SAXException;
 
 /**
  * Helper class to simplify setting up a Embedded Tomcat in a IDE and with a
  * Maven web project.
  * 
  * @author Ralph Schaer
  */
 public class EmbeddedTomcat {
 
 	private static final Log log = LogFactory.getLog(EmbeddedTomcat.class);
 
 	private static final String SHUTDOWN_COMMAND = "EMBEDDED_TC_SHUTDOWN";
 
 	private String contextPath;
 
 	private Integer port;
 
 	private Integer shutdownPort;
 
 	private int secondsToWaitBeforePortBecomesAvailable;
 
 	private String tempDirectory;
 
 	private String contextDirectory;
 
 	private boolean removeDefaultServlet;
 
 	private boolean privileged;
 
 	private boolean silent;
 
 	private boolean addDefaultListeners = false;
 
 	private boolean useNio = false;
 
 	private boolean enableNaming = false;
 
 	private final List<Artifact> resourceArtifacts;
 
 	private final List<ContextEnvironment> contextEnvironments;
 
 	private final List<ContextResource> contextResources;
 
 	private File pomFile;
 
 	private File m2Directory;
 
 	private Tomcat tomcat;
 
 	/**
 	 * Starts a embedded Tomcat on port 8080 with context path "/" and context
 	 * directory current directory + /src/main/webapp
 	 * 
 	 * @param args program arguments
 	 */
 	public static void main(String[] args) {
 		new EmbeddedTomcat().startAndWait();
 	}
 
 	/**
 	 * Convenient method to create a embedded Tomcat that listens on port 8080
 	 * and with a context path of "/"
 	 * 
 	 * @return EmbeddedTomcat the embedded tomcat
 	 */
 	public static EmbeddedTomcat create() {
 		return new EmbeddedTomcat();
 	}
 
 	/**
 	 * Creates an embedded Tomcat with context path "/" and port 8080. Context
 	 * directory points to current directory + /src/main/webapp Change context
 	 * directory with the method <code>setContextDirectory(String)</code>
 	 * 
 	 * @see EmbeddedTomcat#setContextDirectory(String)
 	 */
 	public EmbeddedTomcat() {
 		this("/", 8080);
 	}
 
 	/**
 	 * Creates an embedded Tomcat with context path "/" and specified port.
 	 * Context directory points to current directory + /src/main/webapp Change
 	 * context directory with the method
 	 * <code>setContextDirectory(String)</code>
 	 * 
 	 * @param port ip port the server is listening
 	 * 
 	 * @see #setContextDirectory(String)
 	 */
 	public EmbeddedTomcat(int port) {
 		this("/", port);
 	}
 
 	/**
 	 * Creates an embedded Tomcat with the specified context path and port 8080
 	 * Context directory points to current directory + /src/main/webapp Change
 	 * context directory with the method
 	 * <code>setContextDirectory(String)</code>
 	 * 
 	 * @param contextPath The context path has to start with /
 	 * 
 	 * @see #setContextDirectory(String)
 	 */
 	public EmbeddedTomcat(String contextPath) {
 		this(contextPath, 8080);
 	}
 
 	/**
 	 * Creates an embedded Tomcat with specified context path and specified
 	 * port. Context directory points to current directory + /src/main/webapp
 	 * Change context directory with the method
 	 * <code>setContextDirectory(String)</code>
 	 * 
 	 * @param contextPath has to start with /
 	 * @param port ip port the server is listening
 	 * 
 	 * @see EmbeddedTomcat#setContextDirectory(String)
 	 */
 	public EmbeddedTomcat(String contextPath, int port) {
 		tomcat = null;
 
 		setContextPath(contextPath);
 		setPort(port);
 		setPomFile(null);
 		setM2Directory(null);
 		setShutdownPort(port + 1000);
 		setSecondsToWaitBeforePortBecomesAvailable(10);
 		setPrivileged(false);
 		setSilent(false);
 		setContextDirectory(null);
 
 		this.tempDirectory = null;
 		this.contextEnvironments = new ArrayList<ContextEnvironment>();
 		this.contextResources = new ArrayList<ContextResource>();
 		this.resourceArtifacts = new ArrayList<Artifact>();
 		this.removeDefaultServlet = false;
 	}
 
 	/**
 	 * Sets the port the server is listening for http requests
 	 * 
 	 * @param port The new port
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat setPort(int port) {
 		this.port = port;
 		return this;
 	}
 
 	/**
 	 * Sets the contextPath for the webapplication
 	 * 
 	 * @param contextPath The new contextPath. Has to start with /
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat setContextPath(String contextPath) {
 
 		if (contextPath != null && !contextPath.startsWith("/")) {
 			throw new IllegalArgumentException("contextPath does not start with /");
 		}
 
 		this.contextPath = contextPath;
 		return this;
 	}
 
 	/**
 	 * Sets the context directory. Normally this point to the directory that
 	 * hosts the WEB-INF directory. Default value is: current directory +
 	 * /src/main/webapp This is the normal location of the webapplication
 	 * directory in a Maven web app project.
 	 * 
 	 * @param contextDirectory Path name to the directory that contains the web
 	 *        application.
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat setContextDirectory(String contextDirectory) {
 		this.contextDirectory = contextDirectory;
 		return this;
 	}
 
 	/**
 	 * Sets the location of the temporary directory. Tomcat needs this for
 	 * storing temporary files like compiled jsp files. Default value is
 	 * <p>
 	 * <code>
 	 * target/tomcat. + port 
 	 * </code>
 	 * 
 	 * @param tempDirectory File object that represents the location of the temp
 	 *        directory
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat setTempDirectory(File tempDirectory) {
 		this.tempDirectory = tempDirectory.getAbsolutePath();
 		return this;
 	}
 
 	/**
 	 * Sets the temporary directory to a directory beneath the target directory <br>
 	 * The directory does not have to exists, Tomcat will create it
 	 * automatically if necessary.
 	 * 
 	 * @param name directory name
 	 * @return The embedded Tomcat
 	 * 
 	 * @see EmbeddedTomcat#setTempDirectory(File)
 	 */
 	public EmbeddedTomcat setTempDirectoryName(String name) {
 		tempDirectory = new File(".", "target/" + name).getAbsolutePath();
 		return this;
 	}
 
 	/**
 	 * Sets the location of the pom file. <br>
 	 * Default value is current directory + /pom.xml
 	 * 
 	 * @param pomFile File object that points to the maven pom file (pom.xml)
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat setPomFile(File pomFile) {
 		this.pomFile = pomFile;
 		return this;
 	}
 
 	/**
 	 * Sets the path to the Maven local repository. Default value is <code>
 	 * System.getProperty("user.home") + /.m2/repository/
 	 * </code>
 	 * 
 	 * @param m2Directory File object that points to the Maven local repository
 	 *        directory
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat setM2Directory(File m2Directory) {
 		this.m2Directory = m2Directory;
 		return this;
 	}
 
 	/**
 	 * Calling this method prevents adding the DefaultServlet to the context.
 	 * This is needed if the programm uses an initializer that adds a servlet
 	 * with a mapping of "/"
 	 * 
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat removeDefaultServlet() {
 		removeDefaultServlet = true;
 		return this;
 	}
 
 	/**
 	 * The EmbeddedTomcat opens as default a shutdown port on port + 1000 with
 	 * the shutdown command <code>EMBEDDED_TC_SHUTDOWN</code> Calling this
 	 * method disables adding the shutdown hook.
 	 * 
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat dontAddShutdownHook() {
 		shutdownPort = null;
 		return this;
 	}
 
 	/**
 	 * Before starting the embedded Tomcat the programm tries to stop a previous
 	 * process by sendig the shutdown command to the shutdown port. It then
 	 * waits for the port to become available. It checks this every second for
 	 * the specified number of seconds
 	 * 
 	 * @param seconds number of seconds
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat setSecondsToWaitBeforePortBecomesAvailable(int seconds) {
 		secondsToWaitBeforePortBecomesAvailable = seconds;
 		return this;
 	}
 
 	/**
 	 * Specifies the port the server is listen for the shutdown command. Default
 	 * is port + 1000
 	 * 
 	 * @param shutdownPort the shutdown port
 	 * @return The embedded Tomcat *
 	 * 
 	 * @see EmbeddedTomcat#dontAddShutdownHook()
 	 */
 	public EmbeddedTomcat setShutdownPort(int shutdownPort) {
 		this.shutdownPort = shutdownPort;
 		return this;
 	}
 
 	/**
 	 * Set the privileged flag for this web application.
 	 * 
 	 * @param privileged The new privileged flag
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat setPrivileged(boolean privileged) {
 		this.privileged = privileged;
 		return this;
 	}
 
 	/**
 	 * Instructs the embedded tomcat to use the Non Blocking Connector
 	 * (org.apache.coyote.http11.Http11NioProtocol) instead of the Blocking
 	 * Connector (org.apache.coyote.http11.Http11Protocol)
 	 * 
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat useNio() {
 		this.useNio = true;
 		return this;
 	}
 
 	/**
 	 * Enables JNDI naming which is disabled by default.
 	 * 
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat enableNaming() {
 		this.enableNaming = true;
 		return this;
 	}
 
 	/**
 	 * Installs the default listeners AprLifecycleListener, JasperListener,
 	 * JreMemoryLeakPreventionListener, GlobalResourcesLifecycleListener and
 	 * ThreadLocalLeakPreventionListener during startup.
 	 * 
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat addDefaultListeners() {
 		this.addDefaultListeners = true;
 		return this;
 	}
 
 	/**
 	 * Set the silent flag of Tomcat. If true Tomcat no longer prints any log
 	 * messages
 	 * 
 	 * @param silent The new silent flag
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat setSilent(boolean silent) {
 		this.silent = silent;
 		return this;
 	}
 
 	/**
 	 * Adds all the dependencies specified in the pom.xml (except scope
 	 * provided) to the context as a resource jar. A resource jar contains
 	 * static resources in the directory META-INF/resources
 	 * 
 	 * @return The embedded Tomcat
 	 * 
 	 * @see Context#addResourceJarUrl(URL)
 	 */
 	public EmbeddedTomcat addAllDependenciesAsResourceJar() {
 		resourceArtifacts.add(new AllArtifact());
 		return this;
 	}
 
 	/**
 	 * Adds the specified jar to the context as a resource jar. The helper class
 	 * automatically locates the version of the specified artifact in the
 	 * pom.xml and locates the jar file in the local maven repository.
 	 * 
 	 * @param groupId the maven groupId name
 	 * @param artifact the maven artifact name
 	 * @return The embedded Tomcat
 	 * 
 	 * @see Context#addResourceJarUrl(URL)
 	 * @see EmbeddedTomcat#addAllDependenciesAsResourceJar()
 	 */
 	public EmbeddedTomcat addDependencyAsResourceJar(String groupId, String artifact) {
 		resourceArtifacts.add(new Artifact(groupId, artifact));
 		return this;
 	}
 
 	/**
 	 * Adds a {@link ContextEnvironment} object to embedded Tomcat.
 	 * <p>
 	 * Example:<br>
 	 * Tomcat context xml file
 	 * 
 	 * <pre>
 	 *    &lt;Environment name="aparam" 
 	 *                 value="test" 
 	 *                 type="java.lang.String" 
 	 *                 override="false"/&gt;
 	 * </pre>
 	 * 
 	 * A programmatic way to add this environment to the embedded Tomcat is by
 	 * calling this method
 	 * 
 	 * <pre>
 	 * ContextEnvironment env = new ContextEnvironment();
 	 * env.setType(&quot;java.lang.String&quot;);
 	 * env.setName(&quot;aparam&quot;);
 	 * env.setValue(&quot;test&quot;);
 	 * embeddedTomcat.addContextEnvironment(env);
 	 * </pre>
 	 * 
 	 * 
 	 * @param env context environment variable
 	 * @return The embedded Tomcat
 	 * 
 	 * @see ContextEnvironment
 	 * @see NamingResources#addEnvironment(ContextEnvironment)
 	 * @see EmbeddedTomcat#addContextEnvironment(String, String, String)
 	 * @see EmbeddedTomcat#addContextEnvironmentString(String, String)
 	 */
 	public EmbeddedTomcat addContextEnvironment(ContextEnvironment env) {
 		contextEnvironments.add(env);
 		return this;
 	}
 
 	/**
 	 * Adds a {@link ContextResource} object to the list of resources in the
 	 * embedded Tomcat.
 	 * 
 	 * <p>
 	 * Example:<br>
 	 * Tomcat context xml file
 	 * 
 	 * <pre>
 	 *  &ltResource name="jdbc/ds" auth="Container"
 	 *     type="javax.sql.DataSource" username="sa" password=""
 	 *     driverClassName="org.h2.Driver"
 	 *     url="jdbc:h2:~/mydb"
 	 *     maxActive="20" maxIdle="4" maxWait="10000"
 	 *     defaultAutoCommit="false"/&gt;
 	 * </pre>
 	 * 
 	 * Programmatic way:
 	 * 
 	 * <pre>
 	 * ContextResource res = new ContextResource();
 	 * res.setName(&quot;jdbc/ds&quot;);
 	 * res.setType(&quot;javax.sql.DataSource&quot;);
 	 * res.setAuth(&quot;Container&quot;);
 	 * res.setProperty(&quot;username&quot;, &quot;sa&quot;);
 	 * res.setProperty(&quot;password&quot;, &quot;&quot;);
 	 * res.setProperty(&quot;driverClassName&quot;, &quot;org.h2.Driver&quot;);
 	 * res.setProperty(&quot;url&quot;, &quot;jdbc:h2:&tilde;/mydb&quot;);
 	 * res.setProperty(&quot;maxActive&quot;, &quot;20&quot;);
 	 * res.setProperty(&quot;maxIdle&quot;, &quot;4&quot;);
 	 * res.setProperty(&quot;maxWait&quot;, &quot;10000&quot;);
 	 * res.setProperty(&quot;defaultAutoCommit&quot;, &quot;false&quot;);
 	 * 
 	 * embeddedTomcat.addContextResource(res);
 	 * </pre>
 	 * 
 	 * @param res resource object
 	 * @return The embedded Tomcat
 	 * 
 	 * @see ContextResource
 	 * @see NamingResources#addResource(ContextResource)
 	 */
 	public EmbeddedTomcat addContextResource(ContextResource res) {
 		contextResources.add(res);
 		return this;
 	}
 
 	/**
 	 * Convenient method for adding a context environment to the embedded
 	 * Tomcat. Creates a <code>ContextEnvironment</code> object and adds it to
 	 * the list of the context environments.
 	 * 
 	 * <p>
 	 * Example:<br>
 	 * Tomcat context xml file
 	 * 
 	 * <pre>
 	 *    &lt;Environment name="aparam" 
 	 *                 value="test" 
 	 *                 type="java.lang.String" 
 	 *                 override="false"/&gt;
 	 * </pre>
 	 * 
 	 * Programmatic way:
 	 * 
 	 * <pre>
 	 * embeddedTomcat.addContextEnvironment(&quot;aparam&quot;, &quot;test&quot;, &quot;java.lang.String&quot;);
 	 * </pre>
 	 * 
 	 * @param name name of the context environment
 	 * @param value value of the context environment
 	 * @param type type of the context environment
 	 * @return The embedded Tomcat
 	 * 
 	 * @see ContextEnvironment
 	 * @see NamingResources#addEnvironment(ContextEnvironment)
 	 * @see EmbeddedTomcat#addContextEnvironment(ContextEnvironment)
 	 * @see EmbeddedTomcat#addContextEnvironmentString(String, String)
 	 */
 	public EmbeddedTomcat addContextEnvironment(String name, String value, String type) {
 		final ContextEnvironment env = new ContextEnvironment();
 		env.setType(type);
 		env.setName(name);
 		env.setValue(value);
 		addContextEnvironment(env);
 		return this;
 	}
 
 	/**
 	 * Convenient method for adding a context environment with type
 	 * java.lang.String to the embedded Tomcat.
 	 * 
 	 * <pre>
 	 * embeddedTomcat.addContextEnvironment(&quot;aparam&quot;, &quot;test&quot;);
 	 * </pre>
 	 * 
 	 * @param name name of the context environment
 	 * @param value value of the context environment
 	 * @return The embedded Tomcat
 	 * 
 	 * @see ContextEnvironment
 	 * @see NamingResources#addEnvironment(ContextEnvironment)
 	 * @see EmbeddedTomcat#addContextEnvironment(ContextEnvironment)
 	 * @see EmbeddedTomcat#addContextEnvironment(String, String, String)
 	 */
 	public EmbeddedTomcat addContextEnvironmentString(String name, String value) {
 		addContextEnvironment(name, value, "java.lang.String");
 		return this;
 	}
 
 	/**
 	 * Read ContextEnvironment and ContextResource definition from a text file.
 	 * 
 	 * @param contextFile Location to a context file
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat addContextEnvironmentAndResourceFromFile(File contextFile) {
 		try {
 			final ContextConfig cc = (ContextConfig) ContextConfig.createDigester().parse(contextFile);
 			if (cc != null) {
 				contextEnvironments.addAll(cc.getEnvironments());
 				contextResources.addAll(cc.getResources());
 			}
 		} catch (IOException e) {
 			throw new RuntimeException(e);
 		} catch (SAXException e) {
 			throw new RuntimeException(e);
 		}
 		return this;
 	}
 
 	/**
 	 * Read ContextEnvironment and ContextResource definition from a text file.
 	 * 
 	 * @param contextFile Location to a context file
 	 * @return The embedded Tomcat
 	 */
 	public EmbeddedTomcat addContextEnvironmentAndResourceFromFile(String contextFile) {
 		addContextEnvironmentAndResourceFromFile(new File(contextFile));
 		return this;
 	}
 
 	/**
 	 * Starts the embedded Tomcat and do not wait for incoming requests. Returns
 	 * immediately if the configured port is in use.
 	 * 
 	 * @see EmbeddedTomcat#startAndWait()
 	 */
 	public void start() {
 		start(false);
 	}
 
 	/**
 	 * Starts the embedded Tomcat and waits for incoming requests. Returns
 	 * immediately if the configured port is in use.
 	 * 
 	 * @see EmbeddedTomcat#start()
 	 */
 	public void startAndWait() {
 		start(true);
 	}
 
 	private void start(boolean await) {
 
 		// try to shutdown a previous Tomcat
 		sendShutdownCommand();
 
 		try {
 			final ServerSocket srv = new ServerSocket(port);
 			srv.close();
 		} catch (IOException e) {
 			log.error("PORT " + port + " ALREADY IN USE");
 			return;
 		}
 
 		tomcat = new Tomcat();
 
 		if (addDefaultListeners) {
 			tomcat.getServer().addLifecycleListener(new AprLifecycleListener());
 		}
 
 		if (useNio) {
 			Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
 			connector.setPort(port);
 			tomcat.setConnector(connector);
 			tomcat.getService().addConnector(connector);
 		} else {
 			tomcat.setPort(port);
		}

		if (tempDirectory == null) {
			tempDirectory = new File(".", "/target/tomcat." + port).getAbsolutePath();
		}

		tomcat.setBaseDir(tempDirectory);

		if (silent) {
			tomcat.setSilent(true);
 		}
 
 		if (shutdownPort != null) {
 			tomcat.getServer().setPort(shutdownPort);
 			tomcat.getServer().setShutdown(SHUTDOWN_COMMAND);
 		}
 
		tomcat.getConnector().setURIEncoding("UTF-8");

 		String contextDir = contextDirectory;
 		if (contextDir == null) {
 			contextDir = new File(".").getAbsolutePath() + "/src/main/webapp";
 		}
 
 		final Context ctx;
 		try {
 			ctx = tomcat.addWebapp(contextPath, contextDir);
 			ctx.setResources(new TargetClassesContext());
 		} catch (ServletException e) {
 			throw new RuntimeException(e);
 		}
 
 		if (!resourceArtifacts.isEmpty()) {
 
 			List<File> jarFiles;
 			try {
 				jarFiles = findJarFiles();
 
 				for (File jarFile : jarFiles) {
 					ctx.addResourceJarUrl(new URL("jar:" + jarFile.toURI() + "!/"));
 				}
 
 			} catch (ParserConfigurationException e) {
 				throw new RuntimeException(e);
 			} catch (SAXException e) {
 				throw new RuntimeException(e);
 			} catch (IOException e) {
 				throw new RuntimeException(e);
 			}
 
 		}
 
 		if (privileged) {
 			ctx.setPrivileged(true);
 		}
 
 		if (enableNaming || !contextEnvironments.isEmpty() || !contextResources.isEmpty()) {
 			tomcat.enableNaming();
 
 			if (addDefaultListeners) {
 				tomcat.getServer().addLifecycleListener(new GlobalResourcesLifecycleListener());
 			}
 		}
 
 		if (addDefaultListeners) {
 			Server server = tomcat.getServer();
 			server.addLifecycleListener(new JasperListener());
 			server.addLifecycleListener(new JreMemoryLeakPreventionListener());
 			server.addLifecycleListener(new ThreadLocalLeakPreventionListener());
 		}
 
 		for (ContextEnvironment env : contextEnvironments) {
 			ctx.getNamingResources().addEnvironment(env);
 		}
 
 		for (ContextResource res : contextResources) {
 			ctx.getNamingResources().addResource(res);
 		}
 
 		if (removeDefaultServlet) {
 			ctx.addLifecycleListener(new LifecycleListener() {
 				@Override
 				public void lifecycleEvent(LifecycleEvent event) {
 					if (Lifecycle.BEFORE_START_EVENT.equals(event.getType())) {
 						ctx.removeServletMapping("/");
 					}
 				}
 			});
 		}
 
 		try {
 			tomcat.start();
 		} catch (LifecycleException e) {
 			throw new RuntimeException(e);
 		}
 
 		((StandardManager) ctx.getManager()).setPathname("");
 
 		installSlf4jBridge();
 
 		if (await) {
 			tomcat.getServer().await();
 		}
 	}
 
 	/**
 	 * Stops the embedded tomcat. Does nothing if it's not started
 	 */
 	public void stop() {
 		if (tomcat != null) {
 			try {
 				tomcat.stop();
 			} catch (LifecycleException e) {
 				throw new RuntimeException(e);
 			}
 		}
 	}
 
 	private void sendShutdownCommand() {
 		if (shutdownPort != null) {
 			try {
 				final Socket socket = new Socket("localhost", shutdownPort);
 				final OutputStream stream = socket.getOutputStream();
 
 				for (int i = 0; i < SHUTDOWN_COMMAND.length(); i++) {
 					stream.write(SHUTDOWN_COMMAND.charAt(i));
 				}
 
 				stream.flush();
 				stream.close();
 				socket.close();
 			} catch (UnknownHostException e) {
 				if (!silent) {
 					log.debug(e);
 				}
 				return;
 			} catch (IOException e) {
 				if (!silent) {
 					log.debug(e);
 				}
 				return;
 			}
 
 			// try to wait specified seconds until port becomes available
 			int count = 0;
 			while (count < secondsToWaitBeforePortBecomesAvailable * 2) {
 				try {
 					final ServerSocket srv = new ServerSocket(port);
 					srv.close();
 					return;
 				} catch (IOException e) {
 					count++;
 				}
 				try {
 					TimeUnit.MILLISECONDS.sleep(500);
 				} catch (InterruptedException e) {
 					return;
 				}
 			}
 		}
 	}
 
 	private static void installSlf4jBridge() {
 		try {
 			// Check class is available
 			final Class<?> clazz = Class.forName("org.slf4j.bridge.SLF4JBridgeHandler");
 
 			// Remove all JUL handlers
 			java.util.logging.LogManager.getLogManager().reset();
 
 			// Install slf4j bridge handler
 			final Method method = clazz.getMethod("install", new Class<?>[0]);
 			method.invoke(null);
 		} catch (ClassNotFoundException e) {
 			// do nothing
 		} catch (IllegalArgumentException e) {
 			throw new RuntimeException(e);
 		} catch (IllegalAccessException e) {
 			throw new RuntimeException(e);
 		} catch (InvocationTargetException e) {
 			throw new RuntimeException(e);
 		} catch (SecurityException e) {
 			throw new RuntimeException(e);
 		} catch (NoSuchMethodException e) {
 			throw new RuntimeException(e);
 		}
 	}
 
 	private List<File> findJarFiles() throws ParserConfigurationException, SAXException, IOException {
 
 		File m2Dir = m2Directory;
 		if (m2Dir == null) {
 			final File homeDir = new File(System.getProperty("user.home"));
 			m2Dir = new File(homeDir, ".m2/repository/");
 		}
 
 		final List<File> jars = new ArrayList<File>();
 
 		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
 		final DocumentBuilder db = dbf.newDocumentBuilder();
 
 		File pom = pomFile;
 		if (pom == null) {
 			pom = new File("./pom.xml");
 		}
 
 		final Document doc = db.parse(pom);
 
 		final Map<String, String> properties = new HashMap<String, String>();
 		final NodeList propertiesNodeList = doc.getElementsByTagName("properties");
 		if (propertiesNodeList != null && propertiesNodeList.item(0) != null) {
 			final NodeList propertiesChildren = propertiesNodeList.item(0).getChildNodes();
 			for (int i = 0; i < propertiesChildren.getLength(); i++) {
 				final Node node = propertiesChildren.item(i);
 				if (node instanceof Element) {
 					properties.put("${" + node.getNodeName() + "}", stripWhitespace(node.getTextContent()));
 				}
 			}
 		}
 
 		final NodeList nodeList = doc.getElementsByTagName("dependency");
 		for (int i = 0; i < nodeList.getLength(); i++) {
 			final Element node = (Element) nodeList.item(i);
 			String groupId = node.getElementsByTagName("groupId").item(0).getTextContent();
 			String artifact = node.getElementsByTagName("artifactId").item(0).getTextContent();
 			String version = node.getElementsByTagName("version").item(0).getTextContent();
 
 			groupId = stripWhitespace(groupId);
 			artifact = stripWhitespace(artifact);
 			version = stripWhitespace(version);
 
 			groupId = resolveProperty(groupId, properties);
 			artifact = resolveProperty(artifact, properties);
 			version = resolveProperty(version, properties);
 
 			if (isIncluded(groupId, artifact)) {
 
 				String scope = null;
 				final NodeList scopeNode = node.getElementsByTagName("scope");
 				if (scopeNode != null && scopeNode.item(0) != null) {
 					scope = stripWhitespace(scopeNode.item(0).getTextContent());
 				}
 
 				if (scope == null || !scope.equals("provided")) {
 					groupId = groupId.replace(".", "/");
 					final String artifactFileName = groupId + "/" + artifact + "/" + version + "/" + artifact + "-"
 							+ version + ".jar";
 
 					jars.add(new File(m2Dir, artifactFileName));
 				}
 
 			}
 		}
 
 		return jars;
 	}
 
 	private boolean isIncluded(String groupId, String artifactId) {
 
 		for (Artifact artifact : resourceArtifacts) {
 			if (artifact.is(groupId, artifactId)) {
 				return true;
 			}
 		}
 		return false;
 
 	}
 
 	private static String stripWhitespace(String orig) {
 		if (orig != null) {
 			return orig.replace("\r", "").replace("\n", "").replace("\t", "").trim();
 		}
 		return orig;
 	}
 
 	private static String resolveProperty(String orig, Map<String, String> properties) {
 		final String property = properties.get(orig);
 		if (property != null) {
 			return property;
 		}
 		return orig;
 	}
 
 }
