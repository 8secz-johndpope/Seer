 /*******************************************************************************
  * Copyright (c) 2004, 2007 Mylyn project committers and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *******************************************************************************/
 
 package org.eclipse.mylyn.web.core;
 
 import java.io.IOException;
 import java.io.InterruptedIOException;
 import java.net.InetAddress;
 import java.net.InetSocketAddress;
 import java.net.Proxy;
 import java.net.Socket;
 import java.util.concurrent.Callable;
 import java.util.concurrent.ExecutionException;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Future;
 import java.util.concurrent.SynchronousQueue;
 import java.util.concurrent.ThreadPoolExecutor;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.TimeoutException;
 
 import org.apache.commons.httpclient.Credentials;
 import org.apache.commons.httpclient.HostConfiguration;
 import org.apache.commons.httpclient.HttpClient;
 import org.apache.commons.httpclient.HttpMethod;
 import org.apache.commons.httpclient.NTCredentials;
 import org.apache.commons.httpclient.UsernamePasswordCredentials;
 import org.apache.commons.httpclient.auth.AuthScope;
 import org.apache.commons.httpclient.params.DefaultHttpParams;
 import org.apache.commons.httpclient.params.HttpClientParams;
 import org.apache.commons.httpclient.params.HttpMethodParams;
 import org.apache.commons.httpclient.protocol.Protocol;
 import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
 import org.eclipse.core.net.proxy.IProxyData;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.OperationCanceledException;
 import org.eclipse.core.runtime.Plugin;
 import org.eclipse.mylyn.internal.web.core.CloneableHostConfiguration;
 import org.eclipse.mylyn.internal.web.core.PollingProtocolSocketFactory;
 import org.eclipse.mylyn.internal.web.core.PollingSslProtocolSocketFactory;
 
 /**
  * @author Mik Kersten
  * @author Steffen Pingel
  * @author Rob Elves
  * @since 3.0
  */
 public class WebUtil {
 
 	/**
 	 * @since 3.0
 	 */
 	public interface AbortableCallable<T> extends Callable<T> {
 
 		public void abort();
 
 	}
 
 	private static final int MAX_THREADS = 10;
 
 	/**
 	 * like Mylyn/2.1.0 (Rally Connector 1.0) Eclipse/3.3.0 (JBuilder 2007) HttpClient/3.0.1 Java/1.5.0_11 (Sun)
 	 * Linux/2.6.20-16-lowlatency (i386; en)
 	 */
 	private static final String USER_AGENT;
 
 	private static final int CONNNECT_TIMEOUT = 60000;
 
 	private static final int SOCKET_TIMEOUT = 60000;
 
 	private static final int HTTP_PORT = 80;
 
 	private static final int HTTPS_PORT = 443;
 
 	private static final long POLL_INTERVAL = 1000;
 
 	private static final String USER_AGENT_PREFIX;
 
 	private static final String USER_AGENT_POSTFIX;
 
 	private static final ExecutorService service = new ThreadPoolExecutor(1, MAX_THREADS, 60L, TimeUnit.SECONDS,
 			new SynchronousQueue<Runnable>());
 
 	static {
 		initCommonsLoggingSettings();
 
 		StringBuilder sb = new StringBuilder();
 		sb.append("Mylyn");
 		sb.append(getBundleVersion(WebCorePlugin.getDefault()));
 
 		USER_AGENT_PREFIX = sb.toString();
 		sb.setLength(0);
 
 		if (System.getProperty("org.osgi.framework.vendor") != null) {
 			sb.append(" ");
 			sb.append(System.getProperty("org.osgi.framework.vendor"));
 			sb.append(stripQualifier(System.getProperty("osgi.framework.version")));
 
 			if (System.getProperty("eclipse.product") != null) {
 				sb.append(" (");
 				sb.append(System.getProperty("eclipse.product"));
 				sb.append(")");
 			}
 		}
 
 		sb.append(" ");
 		sb.append(DefaultHttpParams.getDefaultParams().getParameter(HttpMethodParams.USER_AGENT).toString().split("-")[1]);
 
 		sb.append(" Java/");
 		sb.append(System.getProperty("java.version"));
 		sb.append(" (");
 		sb.append(System.getProperty("java.vendor").split(" ")[0]);
 		sb.append(") ");
 
 		sb.append(System.getProperty("os.name"));
 		sb.append("/");
 		sb.append(System.getProperty("os.version"));
 		sb.append(" (");
 		sb.append(System.getProperty("os.arch"));
 		if (System.getProperty("osgi.nl") != null) {
 			sb.append("; ");
 			sb.append(System.getProperty("osgi.nl"));
 		}
 		sb.append(")");
 
 		USER_AGENT_POSTFIX = sb.toString();
 
 		USER_AGENT = USER_AGENT_PREFIX + USER_AGENT_POSTFIX;
 	}
 
 	/**
 	 * @since 3.0
 	 */
 	public static void configureHttpClient(HttpClient client, String userAgent) {
 		client.getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
 		client.getParams().setParameter(HttpMethodParams.USER_AGENT, getUserAgent(userAgent));
 		// API REVIEW consider setting this as the default
 		//client.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
 		configureHttpClientConnectionManager(client);
 	}
 
 	private static void configureHttpClientConnectionManager(HttpClient client) {
 		client.getHttpConnectionManager().getParams().setSoTimeout(WebUtil.SOCKET_TIMEOUT);
 		client.getHttpConnectionManager().getParams().setConnectionTimeout(WebUtil.CONNNECT_TIMEOUT);
 	}
 
 	private static void configureHttpClientProxy(HttpClient client, HostConfiguration hostConfiguration,
 			AbstractWebLocation location) {
 		String host = WebUtil.getHost(location.getUrl());
 
 		Proxy proxy;
 		if (WebUtil.isRepositoryHttps(location.getUrl())) {
 			proxy = location.getProxyForHost(host, IProxyData.HTTP_PROXY_TYPE);
 		} else {
 			proxy = location.getProxyForHost(host, IProxyData.HTTPS_PROXY_TYPE);
 		}
 
 		if (proxy != null && !Proxy.NO_PROXY.equals(proxy)) {
 			InetSocketAddress address = (InetSocketAddress) proxy.address();
 			hostConfiguration.setProxy(address.getHostName(), address.getPort());
 			if (proxy instanceof AuthenticatedProxy) {
 				AuthenticatedProxy authProxy = (AuthenticatedProxy) proxy;
 				Credentials credentials = getCredentials(authProxy.getUserName(), authProxy.getPassword(),
 						address.getAddress());
 				AuthScope proxyAuthScope = new AuthScope(address.getHostName(), address.getPort(), AuthScope.ANY_REALM);
 				client.getState().setProxyCredentials(proxyAuthScope, credentials);
 			}
 		} else {
 			hostConfiguration.setProxyHost(null);
 		}
 	}
 
 	/**
 	 * @since 3.0
 	 */
 	public static void connect(final Socket socket, final InetSocketAddress address, final int timeout,
 			IProgressMonitor monitor) throws IOException {
 		if (socket == null) {
 			throw new IllegalArgumentException();
 		}
 
 		AbortableCallable<?> executor = new AbortableCallable<Object>() {
 			public void abort() {
 				try {
 					socket.close();
 				} catch (IOException e) {
 					// ignore
 				}
 			}
 
 			public Object call() throws Exception {
 				socket.connect(address, timeout);
 				return null;
 			}
 
 		};
 
 		pollIo(monitor, executor);
 	}
 
 	/**
 	 * @since 3.0
 	 */
 	public static HostConfiguration createHostConfiguration(HttpClient client, AbstractWebLocation location,
 			IProgressMonitor monitor) {
 		if (client == null || location == null) {
 			throw new IllegalArgumentException();
 		}
 
 		String url = location.getUrl();
 		String host = WebUtil.getHost(url);
 		int port = WebUtil.getPort(url);
 
 		configureHttpClientConnectionManager(client);
 
 		HostConfiguration hostConfiguration = new CloneableHostConfiguration();
 		configureHttpClientProxy(client, hostConfiguration, location);
 
 		AuthenticationCredentials credentials = location.getCredentials(AuthenticationType.HTTP);
 		if (credentials != null) {
 			AuthScope authScope = new AuthScope(host, port, AuthScope.ANY_REALM);
 			client.getState().setCredentials(authScope, getHttpClientCredentials(credentials, host));
 		}
 
 		if (WebUtil.isRepositoryHttps(url)) {
 			ProtocolSocketFactory socketFactory = new PollingSslProtocolSocketFactory(monitor);
 			Protocol protocol = new Protocol("https", socketFactory, port);
 			hostConfiguration.setHost(host, port, protocol);
 		} else {
 			ProtocolSocketFactory socketFactory = new PollingProtocolSocketFactory(monitor);
 			Protocol protocol = new Protocol("http", socketFactory, port);
 			hostConfiguration.setHost(host, port, protocol);
 		}
 
 		return hostConfiguration;
 	}
 
 	/**
 	 * @since 3.0
 	 */
 	public static int execute(final HttpClient client, final HostConfiguration hostConfiguration,
 			final HttpMethod method, IProgressMonitor monitor) throws IOException {
 		if (client == null || method == null) {
 			throw new IllegalArgumentException();
 		}
 
 		monitor = Policy.monitorFor(monitor);
 
 		AbortableCallable<Integer> executor = new AbortableCallable<Integer>() {
 			public void abort() {
 				method.abort();
 			}
 
 			public Integer call() throws Exception {
 				return client.executeMethod(hostConfiguration, method);
 			}
 		};
 
 		return pollIo(monitor, executor);
 	}
 
 	private static String getBundleVersion(Plugin plugin) {
 		if (null == plugin) {
 			return "";
 		}
 		Object bundleVersion = plugin.getBundle().getHeaders().get("Bundle-Version");
 		if (null == bundleVersion) {
 			return "";
 		}
 		return stripQualifier((String) bundleVersion);
 	}
 
 	/**
 	 * @since 3.0
 	 */
 	public static int getConnectionTimeout() {
 		return CONNNECT_TIMEOUT;
 	}
 
 	/**
 	 * @since 2.0
 	 */
 	public static Credentials getCredentials(AuthenticatedProxy authProxy, InetSocketAddress address) {
 		return getCredentials(authProxy.getUserName(), authProxy.getPassword(), address.getAddress());
 	}
 
 	static Credentials getCredentials(final String username, final String password, final InetAddress address) {
 		int i = username.indexOf("\\");
 		if (i > 0 && i < username.length() - 1 && address != null) {
 			return new NTCredentials(username.substring(i + 1), password, address.getHostName(), username.substring(0,
 					i));
 		} else {
 			return new UsernamePasswordCredentials(username, password);
 		}
 	}
 
 	/**
 	 * @since 3.0
 	 */
 	public static String getHost(String repositoryUrl) {
 		String result = repositoryUrl;
 		int colonSlashSlash = repositoryUrl.indexOf("://");
 
 		if (colonSlashSlash >= 0) {
 			result = repositoryUrl.substring(colonSlashSlash + 3);
 		}
 
 		int colonPort = result.indexOf(':');
 		int requestPath = result.indexOf('/');
 
 		int substringEnd;
 
 		// minimum positive, or string length
 		if (colonPort > 0 && requestPath > 0) {
 			substringEnd = Math.min(colonPort, requestPath);
 		} else if (colonPort > 0) {
 			substringEnd = colonPort;
 		} else if (requestPath > 0) {
 			substringEnd = requestPath;
 		} else {
 			substringEnd = result.length();
 		}
 
 		return result.substring(0, substringEnd);
 	}
 
 	/**
 	 * @since 2.2
 	 */
 	public static Credentials getHttpClientCredentials(AuthenticationCredentials credentials, String host) {
 		String username = credentials.getUserName();
 		String password = credentials.getPassword();
 		int i = username.indexOf("\\");
 		if (i > 0 && i < username.length() - 1 && host != null) {
 			return new NTCredentials(username.substring(i + 1), password, host, username.substring(0, i));
 		} else {
 			return new UsernamePasswordCredentials(username, password);
 		}
 	}
 
 	/**
 	 * @since 2.0
 	 */
 	public static int getPort(String repositoryUrl) {
 		int colonSlashSlash = repositoryUrl.indexOf("://");
 		int firstSlash = repositoryUrl.indexOf("/", colonSlashSlash + 3);
 		int colonPort = repositoryUrl.indexOf(':', colonSlashSlash + 1);
 		if (firstSlash == -1) {
 			firstSlash = repositoryUrl.length();
 		}
 		if (colonPort < 0 || colonPort > firstSlash) {
 			return isRepositoryHttps(repositoryUrl) ? HTTPS_PORT : HTTP_PORT;
 		}
 
 		int requestPath = repositoryUrl.indexOf('/', colonPort + 1);
 		int end = requestPath < 0 ? repositoryUrl.length() : requestPath;
 		String port = repositoryUrl.substring(colonPort + 1, end);
 		if (port.length() == 0) {
 			return isRepositoryHttps(repositoryUrl) ? HTTPS_PORT : HTTP_PORT;
 		}
 
 		return Integer.parseInt(port);
 	}
 
 	/**
 	 * @since 2.0
 	 */
 	public static String getRequestPath(String repositoryUrl) {
 		int colonSlashSlash = repositoryUrl.indexOf("://");
 		int requestPath = repositoryUrl.indexOf('/', colonSlashSlash + 3);
 
 		if (requestPath < 0) {
 			return "";
 		} else {
 			return repositoryUrl.substring(requestPath);
 		}
 	}
 
 	/**
 	 * @since 3.0
 	 */
 	public static int getSocketTimeout() {
 		return SOCKET_TIMEOUT;
 	}
 
 	/**
 	 * Returns a user agent string that contains information about the platform and operating system. The
 	 * <code>product</code> parameter allows to additional specify custom text that is inserted into the returned
 	 * string. The exact return value depends on the environment.
 	 * 
 	 * <p>
 	 * Examples:
 	 * <ul>
 	 * <li>Headless: <code>Mylyn MyProduct HttpClient/3.1 Java/1.5.0_13 (Sun) Linux/2.6.22-14-generic (i386)</code>
 	 * <li>Eclipse:
 	 * <code>Mylyn/2.2.0 Eclipse/3.4.0 (org.eclipse.sdk.ide) HttpClient/3.1 Java/1.5.0_13 (Sun) Linux/2.6.22-14-generic (i386; en_CA)</code>
 	 * 
 	 * @param product
 	 *            an identifier that is inserted into the returned user agent string
 	 * @return a user agent string
 	 * @since 2.3
 	 */
 	public static String getUserAgent(String product) {
 		if (product != null && product.length() > 0) {
 			StringBuilder sb = new StringBuilder();
 			sb.append(USER_AGENT_PREFIX);
 			sb.append(" ");
 			sb.append(product);
 			sb.append(USER_AGENT_POSTFIX);
 			return sb.toString();
 		} else {
 			return USER_AGENT;
 		}
 	}
 
 	private static void initCommonsLoggingSettings() {
 		// remove?
 		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
 		System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "off");
 		System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "off");
 		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "off");
 	}
 
 	private static boolean isRepositoryHttps(String repositoryUrl) {
 		return repositoryUrl.matches("https.*");
 	}
 
 	/**
 	 * @since 3.0
 	 */
 	public static <T> T poll(IProgressMonitor monitor, AbortableCallable<T> executor) throws Throwable {
 		monitor = Policy.monitorFor(monitor);
 
 		Future<T> future = service.submit(executor);
 		while (true) {
 			if (monitor.isCanceled()) {
 				if (!future.cancel(false)) {
 					executor.abort();
 				}
 				// wait for executor to finish
 				try {
 					if (!future.isCancelled()) {
 						future.get();
 					}
 				} catch (InterruptedException e) {
 					// ignore
 				} catch (ExecutionException e) {
 					// ignore
 				}
 				throw new OperationCanceledException();
 			}
 
 			try {
 				return future.get(POLL_INTERVAL, TimeUnit.MILLISECONDS);
 			} catch (InterruptedException e) {
 				throw new InterruptedIOException();
 			} catch (ExecutionException e) {
 				throw e.getCause();
 			} catch (TimeoutException ignored) {
 			}
 		}
 	}
 
 	@SuppressWarnings("unchecked")
 	private static <T> T pollIo(IProgressMonitor monitor, AbortableCallable<?> executor) throws IOException {
 		try {
 			return (T) poll(monitor, executor);
 		} catch (IOException e) {
 			throw e;
 		} catch (RuntimeException e) {
 			throw e;
 		} catch (Error e) {
 			throw e;
 		} catch (Throwable e) {
 			throw new RuntimeException(e);
 		}
 	}
 
 	private static String stripQualifier(String longVersion) {
 		if (longVersion == null) {
 			return "";
 		}
 
 		String parts[] = longVersion.split("\\.");
 		StringBuilder version = new StringBuilder();
 		if (parts.length > 0) {
 			version.append("/");
 			version.append(parts[0]);
 			if (parts.length > 1) {
 				version.append(".");
 				version.append(parts[1]);
 				if (parts.length > 2) {
 					version.append(".");
 					version.append(parts[2]);
 				}
 			}
 		}
 		return version.toString();
 
 	}
 
 	public static void init() {
 		// initialization is done in the static initializer		
 	}
 
 }
