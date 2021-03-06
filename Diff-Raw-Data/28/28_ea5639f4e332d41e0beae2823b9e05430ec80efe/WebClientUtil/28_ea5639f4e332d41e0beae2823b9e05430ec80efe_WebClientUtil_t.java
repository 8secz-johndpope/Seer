 /*******************************************************************************
  * Copyright (c) 2004 - 2006 University Of British Columbia and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     University Of British Columbia - initial API and implementation
  *******************************************************************************/
 
 package org.eclipse.mylar.core.net;
 
 import java.net.InetSocketAddress;
 import java.net.Proxy;
 import java.net.Proxy.Type;
 
 import org.apache.commons.httpclient.Credentials;
 import org.apache.commons.httpclient.HttpClient;
 import org.apache.commons.httpclient.NTCredentials;
 import org.apache.commons.httpclient.UsernamePasswordCredentials;
 import org.apache.commons.httpclient.auth.AuthScope;
 import org.apache.commons.httpclient.params.HttpClientParams;
 import org.apache.commons.httpclient.protocol.Protocol;
 
 /**
  * @author Mik Kersten
  * @author Steffen Pingel
  */
 public class WebClientUtil {
 
 	public static final int CONNNECT_TIMEOUT = 30000;
 
 	public static final int SOCKET_TIMEOUT = 10000;
 
 	private static final int HTTP_PORT = 80;
 
 	private static final int HTTPS_PORT = 443;
 
 	public static void initCommonsLoggingSettings() {
 		// TODO: move?
 		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
 		System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "off");
 		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "off");
 	}
 
 	/**
 	 * public for testing
 	 */
 	public static boolean repositoryUsesHttps(String repositoryUrl) {
 		return repositoryUrl.matches("https.*");
 	}
 
 	public static int getPort(String repositoryUrl) {
 		int colonSlashSlash = repositoryUrl.indexOf("://");
 		int colonPort = repositoryUrl.indexOf(':', colonSlashSlash + 1);
 		if (colonPort < 0)
 			return repositoryUsesHttps(repositoryUrl) ? HTTPS_PORT : HTTP_PORT;
 
 		int requestPath = repositoryUrl.indexOf('/', colonPort + 1);
 
 		int end;
 		if (requestPath < 0)
 			end = repositoryUrl.length();
 		else
 			end = requestPath;
 
 		return Integer.parseInt(repositoryUrl.substring(colonPort + 1, end));
 	}
 
 	public static String getDomain(String repositoryUrl) {
 		String result = repositoryUrl;
 		int colonSlashSlash = repositoryUrl.indexOf("://");
 
 		if (colonSlashSlash >= 0) {
 			result = repositoryUrl.substring(colonSlashSlash + 3);
 		}
 
 		int colonPort = result.indexOf(':');
 		int requestPath = result.indexOf('/');
 
 		int substringEnd;
 
 		// minimum positive, or string length
 		if (colonPort > 0 && requestPath > 0)
 			substringEnd = Math.min(colonPort, requestPath);
 		else if (colonPort > 0)
 			substringEnd = colonPort;
 		else if (requestPath > 0)
 			substringEnd = requestPath;
 		else
 			substringEnd = result.length();
 
 		return result.substring(0, substringEnd);
 	}
 
 	public static String getRequestPath(String repositoryUrl) {
 		int colonSlashSlash = repositoryUrl.indexOf("://");
 		int requestPath = repositoryUrl.indexOf('/', colonSlashSlash + 3);
 
 		if (requestPath < 0) {
 			return "";
 		} else {
 			return repositoryUrl.substring(requestPath);
 		}
 	}

 	@SuppressWarnings("deprecation")
 	public static void setupHttpClient(HttpClient client, Proxy proxySettings, String repositoryUrl, String user,
 			String password) {
 
 		// Note: The following debug code requires http commons-logging and
 		// commons-logging-api jars
//		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
//		System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
//		System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
//		System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");
//		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");
 
 		client.getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
 		client.getHttpConnectionManager().getParams().setSoTimeout(WebClientUtil.SOCKET_TIMEOUT);
 		client.getHttpConnectionManager().getParams().setConnectionTimeout(WebClientUtil.CONNNECT_TIMEOUT);
 
 		if (proxySettings != null && !Proxy.NO_PROXY.equals(proxySettings)
 				&& !WebClientUtil.repositoryUsesHttps(repositoryUrl)
 				&& proxySettings.address() instanceof InetSocketAddress) {
 			InetSocketAddress address = (InetSocketAddress) proxySettings.address();
 			client.getHostConfiguration().setProxy(WebClientUtil.getDomain(address.getHostName()), address.getPort());
 			if (proxySettings instanceof AuthenticatedProxy) {
 				AuthenticatedProxy authProxy = (AuthenticatedProxy) proxySettings;
 				Credentials credentials = getCredentials(authProxy, address);
 				AuthScope proxyAuthScope = new AuthScope(address.getHostName(), address.getPort(), AuthScope.ANY_REALM);
 				client.getState().setProxyCredentials(proxyAuthScope, credentials);
 			}
 		}
 
 		if (user != null && password != null) {
 			AuthScope authScope = new AuthScope(WebClientUtil.getDomain(repositoryUrl), WebClientUtil
 					.getPort(repositoryUrl), AuthScope.ANY_REALM);
 			client.getState().setCredentials(authScope, new UsernamePasswordCredentials(user, password));
 		}

		if (WebClientUtil.repositoryUsesHttps(repositoryUrl)) {
 			Protocol acceptAllSsl = new Protocol("https", new SslProtocolSocketFactory(proxySettings), WebClientUtil
 					.getPort(repositoryUrl));
 			client.getHostConfiguration().setHost(WebClientUtil.getDomain(repositoryUrl),
 					WebClientUtil.getPort(repositoryUrl), acceptAllSsl);
 			Protocol.registerProtocol("https", acceptAllSsl);
 		} else {
 			client.getHostConfiguration().setHost(WebClientUtil.getDomain(repositoryUrl),
 					WebClientUtil.getPort(repositoryUrl));
 		}
 	}
 
 	public static Credentials getCredentials(AuthenticatedProxy authProxy, InetSocketAddress address) {
 		String username = authProxy.getUserName();
 		int i = username.indexOf("\\");
 		if (i > 0 && i < username.length() - 1) {
			return new NTCredentials(username.substring(i + 1), authProxy.getPassword(), address.getHostName(),
					username.substring(0, i));
 		} else {
 			return new UsernamePasswordCredentials(username, authProxy.getPassword());
 		}
 	}
 
 	/** utility method, should use TaskRepository.getProxy() */
 	public static Proxy getProxy(String proxyHost, String proxyPort, String proxyUsername, String proxyPassword) {
 		boolean authenticated = (proxyUsername != null && proxyPassword != null && proxyUsername.length() > 0 && proxyPassword
 				.length() > 0);
 		if (proxyHost != null && proxyHost.length() > 0 && proxyPort != null && proxyPort.length() > 0) {
 			int proxyPortNum = Integer.parseInt(proxyPort);
 			InetSocketAddress sockAddr = new InetSocketAddress(proxyHost, proxyPortNum);
 			if (authenticated) {
 				return new AuthenticatedProxy(Type.HTTP, sockAddr, proxyUsername, proxyPassword);
 			} else {
 				return new Proxy(Type.HTTP, sockAddr);
 			}
 		}
 		return Proxy.NO_PROXY;
 	}
 
 }
 
 // /**
 // * Returns an opened HttpURLConnection. If the proxy fails a direct
 // * connection is attempted.
 // */
 // public static HttpURLConnection openUrlConnection(URL url, Proxy proxy,
 // boolean useTls, String htAuthUser,
 // String htAuthPass) throws IOException, KeyManagementException,
 // GeneralSecurityException {
 //
 // if (proxy == null) {
 // proxy = Proxy.NO_PROXY;
 // }
 //
 // HttpURLConnection remoteConnection = getUrlConnection(url, proxy, useTls,
 // htAuthUser, htAuthPass);
 // try {
 // remoteConnection = openConnection(url, proxy);
 // } catch (ConnectException e) {
 // remoteConnection = openConnection(url, Proxy.NO_PROXY);
 // }
 //
 // return remoteConnection;
 // }
 
 // /**
 // * Returns connection that has yet to be opened (can still set connection
 // * parameters). Catch ConnectException and retry with Proxy.NO_PROXY if
 // * necessary.
 // */
 // public static HttpURLConnection getUrlConnection(URL url, Proxy proxy,
 // boolean useTls, String htAuthUser,
 // String htAuthPass) throws IOException, KeyManagementException,
 // GeneralSecurityException {
 // SSLContext ctx;
 // if (useTls) {
 // ctx = SSLContext.getInstance("TLS");
 // } else {
 // ctx = SSLContext.getInstance("SSL");
 // }
 //
 // javax.net.ssl.TrustManager[] tm = new javax.net.ssl.TrustManager[] { new
 // TrustAllTrustManager() };
 // ctx.init(null, tm, null);
 // HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
 //
 // if (proxy == null) {
 // proxy = Proxy.NO_PROXY;
 // }
 //
 // URLConnection connection = url.openConnection(proxy);
 //
 // // Add http basic authentication credentials if supplied
 // // Ref: http://www.javaworld.com/javaworld/javatips/jw-javatip47.html
 // if (htAuthUser != null && htAuthPass != null && !htAuthUser.equals("")) {
 // String authenticationString = htAuthUser + ":" + htAuthPass;
 // String encodedAuthenticationString = null;
 // try {
 // sun.misc.BASE64Encoder encoder = (sun.misc.BASE64Encoder)
 // Class.forName("sun.misc.BASE64Encoder")
 // .newInstance();
 // encodedAuthenticationString =
 // encoder.encode(authenticationString.getBytes());
 // connection.setRequestProperty("Authorization", "Basic " +
 // encodedAuthenticationString);
 // } catch (Exception ex) {
 // // ignore, encoder not available
 // }
 // }
 //
 // if (connection == null || !(connection instanceof HttpURLConnection)) {
 // throw new MalformedURLException();
 // }
 // return (HttpURLConnection) connection;
 // }
 
 // private static HttpURLConnection openConnection(URL url, Proxy proxy)
 // throws IOException {
 // URLConnection connection = url.openConnection(proxy);
 // if (connection == null || !(connection instanceof HttpURLConnection)) {
 // throw new MalformedURLException();
 // }
 // HttpURLConnection remoteConnection = (HttpURLConnection) connection;
 // remoteConnection.addRequestProperty("Accept-Encoding", ENCODING_GZIP);
 // remoteConnection.setConnectTimeout(COM_TIME_OUT);
 // remoteConnection.setReadTimeout(COM_TIME_OUT);
 // remoteConnection.connect();
 // return remoteConnection;
 // }
