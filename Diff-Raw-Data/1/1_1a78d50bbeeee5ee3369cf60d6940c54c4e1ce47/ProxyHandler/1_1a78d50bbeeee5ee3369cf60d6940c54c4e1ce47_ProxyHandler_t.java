 // ========================================================================
 // $Id: ProxyHandler.java,v 1.34 2005/10/05 13:32:59 gregwilkins Exp $
 // Copyright 1991-2005 Mort Bay Consulting Pty. Ltd.
 // Portions of this file Copyright 2006 ThoughtWorks, Inc.
 // ------------------------------------------------------------------------
 // Licensed under the Apache License, Version 2.0 (the "License");
 // you may not use this file except in compliance with the License.
 // You may obtain a copy of the License at
 // http://www.apache.org/licenses/LICENSE-2.0
 // Unless required by applicable law or agreed to in writing, software
 // distributed under the License is distributed on an "AS IS" BASIS,
 // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 // See the License for the specific language governing permissions and
 // limitations under the License.
 // ========================================================================
 
 package org.openqa.selenium.server;
 
 import org.apache.commons.logging.Log;
 import org.mortbay.http.*;
 import org.mortbay.http.handler.AbstractHttpHandler;
 import org.mortbay.log.LogFactory;
 import org.mortbay.util.*;
 import org.mortbay.util.URI;
 
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.*;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Set;
 
 /* ------------------------------------------------------------ */
 
 /**
  * Proxy request handler. A HTTP/1.1 Proxy. This implementation uses the JVMs URL implementation to
  * make proxy requests.
  * <p/>
  * The HttpTunnel mechanism is also used to implement the CONNECT method.
  *
  * @author Greg Wilkins (gregw)
  * @author giacof@tiscali.it (chained proxy)
  * @version $Id: ProxyHandler.java,v 1.34 2005/10/05 13:32:59 gregwilkins Exp $
  */
 public class ProxyHandler extends AbstractHttpHandler {
     private static Log log = LogFactory.getLog(ProxyHandler.class);
 
     protected Set<String> _proxyHostsWhiteList;
     protected Set<String> _proxyHostsBlackList;
     protected int _tunnelTimeoutMs = 250;
     private boolean _anonymous = false;
     private transient boolean _chained = false;
     private HashMap<InetAddrPort,SslRelay> _sslMap = new HashMap<InetAddrPort, SslRelay>();
 
     /* ------------------------------------------------------------ */
     /**
      * Map of leg by leg headers (not end to end). Should be a set, but more efficient string map is
      * used instead.
      */
     protected StringMap _DontProxyHeaders = new StringMap();
 
     {
         Object o = new Object();
         _DontProxyHeaders.setIgnoreCase(true);
         _DontProxyHeaders.put(HttpFields.__ProxyConnection, o);
         _DontProxyHeaders.put(HttpFields.__Connection, o);
         _DontProxyHeaders.put(HttpFields.__KeepAlive, o);
         _DontProxyHeaders.put(HttpFields.__TransferEncoding, o);
         _DontProxyHeaders.put(HttpFields.__TE, o);
         _DontProxyHeaders.put(HttpFields.__Trailer, o);
         _DontProxyHeaders.put(HttpFields.__Upgrade, o);
     }
 
     /* ------------------------------------------------------------ */
     /**
      * Map of leg by leg headers (not end to end). Should be a set, but more efficient string map is
      * used instead.
      */
     protected StringMap _ProxyAuthHeaders = new StringMap();
 
     {
         Object o = new Object();
         _ProxyAuthHeaders.put(HttpFields.__ProxyAuthorization, o);
         _ProxyAuthHeaders.put(HttpFields.__ProxyAuthenticate, o);
     }
 
     /* ------------------------------------------------------------ */
     /**
      * Map of allows schemes to proxy Should be a set, but more efficient string map is used
      * instead.
      */
     protected StringMap _ProxySchemes = new StringMap();
 
     {
         Object o = new Object();
         _ProxySchemes.setIgnoreCase(true);
         _ProxySchemes.put(HttpMessage.__SCHEME, o);
         _ProxySchemes.put(HttpMessage.__SSL_SCHEME, o);
         _ProxySchemes.put("ftp", o);
     }
 
     /* ------------------------------------------------------------ */
     /**
      * Set of allowed CONNECT ports.
      */
     protected HashSet<Integer> _allowedConnectPorts = new HashSet<Integer>();
 
     {
         _allowedConnectPorts.add(80);
         _allowedConnectPorts.add(SeleniumServer.DEFAULT_PORT);
         _allowedConnectPorts.add(8000);
         _allowedConnectPorts.add(8080);
         _allowedConnectPorts.add(8888);
         _allowedConnectPorts.add(443);
         _allowedConnectPorts.add(8443);
     }
 
 
     /* ------------------------------------------------------------ */
     /*
      */
     public void start() throws Exception {
         _chained = System.getProperty("http.proxyHost") != null;
         super.start();
     }
 
     /* ------------------------------------------------------------ */
 
     /**
      * Get proxy host white list.
      *
      * @return Array of hostnames and IPs that are proxied, or an empty array if all hosts are
      *         proxied.
      */
     public String[] getProxyHostsWhiteList() {
         if (_proxyHostsWhiteList == null || _proxyHostsWhiteList.size() == 0)
             return new String[0];
 
         String[] hosts = new String[_proxyHostsWhiteList.size()];
         hosts = _proxyHostsWhiteList.toArray(hosts);
         return hosts;
     }
 
     /* ------------------------------------------------------------ */
 
     /**
      * Set proxy host white list.
      *
      * @param hosts Array of hostnames and IPs that are proxied, or null if all hosts are proxied.
      */
     public void setProxyHostsWhiteList(String[] hosts) {
         if (hosts == null || hosts.length == 0)
             _proxyHostsWhiteList = null;
         else {
             _proxyHostsWhiteList = new HashSet<String>();
             for (int i = 0; i < hosts.length; i++) {
                 String host = hosts[i];
                 if (host != null && host.trim().length() > 0)
                     _proxyHostsWhiteList.add(host);
             }
         }
     }
 
     /* ------------------------------------------------------------ */
 
     /**
      * Get proxy host black list.
      *
      * @return Array of hostnames and IPs that are NOT proxied.
      */
     public String[] getProxyHostsBlackList() {
         if (_proxyHostsBlackList == null || _proxyHostsBlackList.size() == 0)
             return new String[0];
 
         String[] hosts = new String[_proxyHostsBlackList.size()];
         hosts = _proxyHostsBlackList.toArray(hosts);
         return hosts;
     }
 
     /* ------------------------------------------------------------ */
 
     /**
      * Set proxy host black list.
      *
      * @param hosts Array of hostnames and IPs that are NOT proxied.
      */
     public void setProxyHostsBlackList(String[] hosts) {
         if (hosts == null || hosts.length == 0)
             _proxyHostsBlackList = null;
         else {
             _proxyHostsBlackList = new HashSet<String>();
             for (int i = 0; i < hosts.length; i++) {
                 String host = hosts[i];
                 if (host != null && host.trim().length() > 0)
                     _proxyHostsBlackList.add(host);
             }
         }
     }
 
     /* ------------------------------------------------------------ */
     public int getTunnelTimeoutMs() {
         return _tunnelTimeoutMs;
     }
 
     /* ------------------------------------------------------------ */
 
     /**
      * Tunnel timeout. IE on win2000 has connections issues with normal timeout handling. This
      * timeout should be set to a low value that will expire to allow IE to see the end of the
      * tunnel connection.
      */
     public void setTunnelTimeoutMs(int ms) {
         _tunnelTimeoutMs = ms;
     }
 
     /* ------------------------------------------------------------ */
     public void handle(String pathInContext, String pathParams, HttpRequest request, HttpResponse response) throws HttpException, IOException {
         URI uri = request.getURI();
 
         // Is this a CONNECT request?
         if (HttpRequest.__CONNECT.equalsIgnoreCase(request.getMethod())) {
             response.setField(HttpFields.__Connection, "close"); // TODO Needed for IE????
             handleConnect(pathInContext, pathParams, request, response);
             return;
         }
 
         try {
             // Do we proxy this?
             URL url = isProxied(uri);
             if (url == null) {
                 if (isForbidden(uri))
                     sendForbid(request, response, uri);
                 return;
             }
 
             // is this URL a /selenium URL?
             if (url.toString().indexOf("/selenium") != -1) { 
                 request.setHandled(false);
                 return;
             }
 
             if (log.isDebugEnabled())
                 log.debug("PROXY URL=" + url);
 
             URLConnection connection = url.openConnection();
             connection.setAllowUserInteraction(false);
             
             if (SeleniumServer.isProxyInjectionMode())
                 adjustRequestForProxyInjection(request, connection);
           
             // Set method
             HttpURLConnection http = null;
             if (connection instanceof HttpURLConnection) {
                 http = (HttpURLConnection) connection;
                 http.setRequestMethod(request.getMethod());
                 http.setInstanceFollowRedirects(false);
             }
 
             // check connection header
             String connectionHdr = request.getField(HttpFields.__Connection);
             if (connectionHdr != null && (connectionHdr.equalsIgnoreCase(HttpFields.__KeepAlive) || connectionHdr.equalsIgnoreCase(HttpFields.__Close)))
                 connectionHdr = null;
 
             // copy headers
             boolean xForwardedFor = false;
             boolean hasContent = false;
             Enumeration enm = request.getFieldNames();
             while (enm.hasMoreElements()) {
                 // TODO could be better than this!
                 String hdr = (String) enm.nextElement();
 
                 if (_DontProxyHeaders.containsKey(hdr) || !_chained && _ProxyAuthHeaders.containsKey(hdr))
                     continue;
                 if (connectionHdr != null && connectionHdr.indexOf(hdr) >= 0)
                     continue;
 
                 if (HttpFields.__ContentType.equals(hdr))
                     hasContent = true;
 
                 Enumeration vals = request.getFieldValues(hdr);
                 while (vals.hasMoreElements()) {
                     String val = (String) vals.nextElement();
                     if (val != null) {
                         // don't proxy Referer headers if the referer is Selenium!
                         if ("Referer".equals(hdr) && (-1 != val.indexOf("/selenium-server/"))) {
                             continue;
                         }
 
                         connection.addRequestProperty(hdr, val);
                         xForwardedFor |= HttpFields.__XForwardedFor.equalsIgnoreCase(hdr);
                     }
                 }
             }
  
             // Proxy headers
             if (!_anonymous)
                 connection.setRequestProperty("Via", "1.1 (jetty)");
             if (!xForwardedFor)
                 connection.addRequestProperty(HttpFields.__XForwardedFor, request.getRemoteAddr());
 
             // a little bit of cache control
             String cache_control = request.getField(HttpFields.__CacheControl);
             if (cache_control != null && (cache_control.indexOf("no-cache") >= 0 || cache_control.indexOf("no-store") >= 0))
                 connection.setUseCaches(false);
 
             // customize Connection
             customizeConnection(pathInContext, pathParams, request, connection);
 
             try {
                 connection.setDoInput(true);
 
                 // do input thang!
                 InputStream in = request.getInputStream();
                 if (hasContent) {
                     connection.setDoOutput(true);
                     IO.copy(in, connection.getOutputStream());
                 }
 
                 // Connect
                 connection.connect();
             }
             catch (Exception e) {
                 LogSupport.ignore(log, e);
             }
 
             InputStream proxy_in = null;
 
             // handler status codes etc.
             int code;
             if (http != null) {
                 proxy_in = http.getErrorStream();
 
                 code = http.getResponseCode();
                 response.setStatus(code);
                 response.setReason(http.getResponseMessage());
                 
                 String contentType = http.getContentType();
                 if (SeleniumServer.isDebugMode()) {
                     SeleniumServer.log("Content-Type is: " + contentType);
                 }
             }
 
             if (proxy_in == null) {
                 try {
                     proxy_in = connection.getInputStream();
                 }
                 catch (Exception e) {
                     LogSupport.ignore(log, e);
                     proxy_in = http.getErrorStream();
                 }
             }
 
             // clear response defaults.
             response.removeField(HttpFields.__Date);
             response.removeField(HttpFields.__Server);
             
             // set response headers
             int h = 0;
             String hdr = connection.getHeaderFieldKey(h);
             String val = connection.getHeaderField(h);
             while (hdr != null || val != null) {
                 if (hdr != null && val != null && !_DontProxyHeaders.containsKey(hdr) && (_chained || !_ProxyAuthHeaders.containsKey(hdr)))
                     response.addField(hdr, val);
                 h++;
                 hdr = connection.getHeaderFieldKey(h);
                 val = connection.getHeaderField(h);
             }
             if (!_anonymous)
                 response.setField("Via", "1.1 (jetty)");
 
             response.removeField(HttpFields.__ETag); // possible cksum?  Stop caching...
             response.removeField(HttpFields.__LastModified); // Stop caching...
 
             // Handled
             request.setHandled(true);
             if (proxy_in != null) {
             	if (SeleniumServer.isProxyInjectionMode()
                         && http.getResponseCode()==HttpURLConnection.HTTP_OK) {
 
                     // check if we should proxy this path based on the dontProxyRegex that can be user-specified
                     if (SeleniumServer.shouldInject(request.getPath())) {
                         InjectionHelper.injectJavaScript(request, response, proxy_in, response.getOutputStream());
                     } else {
                         IO.copy(proxy_in, response.getOutputStream());
                     }
                 }
             	else {
             		IO.copy(proxy_in, response.getOutputStream());
             	}
             }
         }
         catch (Exception e) {
             log.warn(e.toString());
             LogSupport.ignore(log, e);
             if (!response.isCommitted())
                 response.sendError(HttpResponse.__400_Bad_Request);
         }
     }
 
 	private void adjustRequestForProxyInjection(HttpRequest request, URLConnection connection) {
 		request.setState(HttpMessage.__MSG_EDITABLE);
 		if (request.containsField("If-Modified-Since")) {
 			// TODO: still need to disable caching?  I want to prevent 304s during this development phase where 
 			// I'm often changing the injection, and so need HTML caching to be absolutely defeated 
 			request.removeField("If-Modified-Since");
 			request.removeField("If-None-Match");            	
 			connection.setUseCaches(false);  // maybe I don't need the stuff above?
 		}
 		request.removeField("Accept-Encoding");	// js injection is hard w/ gzip'd data, so try to prevent it ahead of time
 		request.setState(HttpMessage.__MSG_RECEIVED);
 	}
 
     
 	/* ------------------------------------------------------------ */
     public void handleConnect(String pathInContext, String pathParams, HttpRequest request, HttpResponse response) throws HttpException, IOException {
         URI uri = request.getURI();
 
         try {
             if (log.isDebugEnabled())
                 log.debug("CONNECT: " + uri);
             InetAddrPort addrPort = new InetAddrPort(uri.toString());
 
             if (isForbidden(HttpMessage.__SSL_SCHEME, addrPort.getHost(), addrPort.getPort(), false)) {
                 sendForbid(request, response, uri);
             } else {
                 HttpConnection http_connection = request.getHttpConnection();
                 http_connection.forceClose();
 
                 HttpServer server = http_connection.getHttpServer();
 
                 SslRelay listener = _sslMap.get(addrPort);
                 if (listener==null)
                 {
                     listener = new SslRelay(addrPort);
 
                     // grab a keystore that has been signed by a CA cert that has already been imported in to the browser
                     // note: this logic assumes the tester is using *custom and has imported the CA cert in to IE/Firefox/etc
                     // the CA cert can be found at http://dangerous-certificate-authority.openqa.org
                     File keystore = File.createTempFile("selenium-rc-" + addrPort.getHost() + "-" + addrPort.getPort(), "keystore");
                     URL url = new URL("http://dangerous-certificate-authority.openqa.org/genkey.jsp?domain=" + addrPort.getHost());
                     URLConnection conn = url.openConnection();
                     conn.connect();
                     InputStream is = conn.getInputStream();
                     byte[] buffer = new byte[1024];
                     int length;
                     FileOutputStream fos = new FileOutputStream(keystore);
                     while ((length = is.read(buffer)) != -1) {
                         fos.write(buffer, 0, length);
                     }
                     fos.close();
                     is.close();
 
                     listener.setKeystore(keystore.getAbsolutePath());
                     listener.setPassword("password");
                     listener.setKeyPassword("password");
                     server.addListener(listener);
                     try
                     {
                         listener.start();
                     }
                     catch(Exception e)
                     {
                         e.printStackTrace();
                         throw e;
                     }
                     _sslMap.put(addrPort,listener);
                 }
 
                 int port = listener.getPort();
 
                 // Get the timeout
                 int timeoutMs = 30000;
                 Object maybesocket = http_connection.getConnection();
                 if (maybesocket instanceof Socket) {
                     Socket s = (Socket) maybesocket;
                     timeoutMs = s.getSoTimeout();
                 }
 
                 // Create the tunnel
                 HttpTunnel tunnel = newHttpTunnel(request, response, InetAddress.getLocalHost(), port, timeoutMs);
 
                 if (tunnel != null) {
                     // TODO - need to setup semi-busy loop for IE.
                     if (_tunnelTimeoutMs > 0) {
                         tunnel.getSocket().setSoTimeout(_tunnelTimeoutMs);
                         if (maybesocket instanceof Socket) {
                             Socket s = (Socket) maybesocket;
                             s.setSoTimeout(_tunnelTimeoutMs);
                         }
                     }
                     tunnel.setTimeoutMs(timeoutMs);
 
                     customizeConnection(pathInContext, pathParams, request, tunnel.getSocket());
                     request.getHttpConnection().setHttpTunnel(tunnel);
                     response.setStatus(HttpResponse.__200_OK);
                     response.setContentLength(0);
                 }
                 request.setHandled(true);
             }
         }
         catch (Exception e) {
            System.err.println("handleConnect: ProxyHandler.java: " + e);
             LogSupport.ignore(log, e);
             response.sendError(HttpResponse.__500_Internal_Server_Error);
         }
     }
 
     /* ------------------------------------------------------------ */
     protected HttpTunnel newHttpTunnel(HttpRequest request, HttpResponse response, InetAddress iaddr, int port, int timeoutMS) throws IOException {
         try {
             Socket socket = null;
             InputStream in = null;
 
             String chained_proxy_host = System.getProperty("http.proxyHost");
             if (chained_proxy_host == null) {
                 socket = new Socket(iaddr, port);
                 socket.setSoTimeout(timeoutMS);
                 socket.setTcpNoDelay(true);
             } else {
                 int chained_proxy_port = Integer.getInteger("http.proxyPort", 8888).intValue();
 
                 Socket chain_socket = new Socket(chained_proxy_host, chained_proxy_port);
                 chain_socket.setSoTimeout(timeoutMS);
                 chain_socket.setTcpNoDelay(true);
                 if (log.isDebugEnabled()) log.debug("chain proxy socket=" + chain_socket);
 
                 LineInput line_in = new LineInput(chain_socket.getInputStream());
                 byte[] connect = request.toString().getBytes(org.mortbay.util.StringUtil.__ISO_8859_1);
                 chain_socket.getOutputStream().write(connect);
 
                 String chain_response_line = line_in.readLine();
                 HttpFields chain_response = new HttpFields();
                 chain_response.read(line_in);
 
                 // decode response
                 int space0 = chain_response_line.indexOf(' ');
                 if (space0 > 0 && space0 + 1 < chain_response_line.length()) {
                     int space1 = chain_response_line.indexOf(' ', space0 + 1);
 
                     if (space1 > space0) {
                         int code = Integer.parseInt(chain_response_line.substring(space0 + 1, space1));
 
                         if (code >= 200 && code < 300) {
                             socket = chain_socket;
                             in = line_in;
                         } else {
                             Enumeration iter = chain_response.getFieldNames();
                             while (iter.hasMoreElements()) {
                                 String name = (String) iter.nextElement();
                                 if (!_DontProxyHeaders.containsKey(name)) {
                                     Enumeration values = chain_response.getValues(name);
                                     while (values.hasMoreElements()) {
                                         String value = (String) values.nextElement();
                                         response.setField(name, value);
                                     }
                                 }
                             }
                             response.sendError(code);
                             if (!chain_socket.isClosed())
                                 chain_socket.close();
                         }
                     }
                 }
             }
 
             if (socket == null)
                 return null;
             return new HttpTunnel(socket, in, null);
         }
         catch (IOException e) {
             log.debug(e);
             response.sendError(HttpResponse.__400_Bad_Request);
             return null;
         }
     }
 
     /* ------------------------------------------------------------ */
 
     /**
      * Customize proxy Socket connection for CONNECT. Method to allow derived handlers to customize
      * the tunnel sockets.
      */
     protected void customizeConnection(String pathInContext, String pathParams, HttpRequest request, Socket socket) {
     }
 
     /* ------------------------------------------------------------ */
 
     /**
      * Customize proxy URL connection. Method to allow derived handlers to customize the connection.
      */
     protected void customizeConnection(String pathInContext, String pathParams, HttpRequest request, URLConnection connection) {
     }
 
     /* ------------------------------------------------------------ */
 
     /**
      * Is URL Proxied. Method to allow derived handlers to select which URIs are proxied and to
      * where.
      *
      * @param uri The requested URI, which should include a scheme, host and port.
      * @return The URL to proxy to, or null if the passed URI should not be proxied. The default
      *         implementation returns the passed uri if isForbidden() returns true.
      */
     protected URL isProxied(URI uri) throws MalformedURLException {
         // Is this a proxy request?
         if (isForbidden(uri))
             return null;
 
         // OK return URI as untransformed URL.
         return new URL(uri.toString());
     }
 
     /* ------------------------------------------------------------ */
 
     /**
      * Is URL Forbidden.
      *
      * @return True if the URL is not forbidden. Calls isForbidden(scheme,host,port,true);
      */
     protected boolean isForbidden(URI uri) {
         String scheme = uri.getScheme();
         String host = uri.getHost();
         int port = uri.getPort();
         return isForbidden(scheme, host, port, true);
     }
 
     /* ------------------------------------------------------------ */
 
     /**
      * Is scheme,host & port Forbidden.
      *
      * @param scheme           A scheme that mast be in the proxySchemes StringMap.
      * @param host             A host that must pass the white and black lists
      * @param port             A port that must in the allowedConnectPorts Set
      * @param openNonPrivPorts If true ports greater than 1024 are allowed.
      * @return True if the request to the scheme,host and port is not forbidden.
      */
     protected boolean isForbidden(String scheme, String host, int port, boolean openNonPrivPorts) {
         // Check port
         if (port > 0 && !_allowedConnectPorts.contains(new Integer(port))) {
             if (!openNonPrivPorts || port <= 1024)
                 return true;
         }
 
         // Must be a scheme that can be proxied.
         if (scheme == null || !_ProxySchemes.containsKey(scheme))
             return true;
 
         // Must be in any defined white list
         if (_proxyHostsWhiteList != null && !_proxyHostsWhiteList.contains(host))
             return true;
 
         // Must not be in any defined black list
         return _proxyHostsBlackList != null && _proxyHostsBlackList.contains(host);
 
     }
 
     /* ------------------------------------------------------------ */
 
     /**
      * Send Forbidden. Method called to send forbidden response. Default implementation calls
      * sendError(403)
      */
     protected void sendForbid(HttpRequest request, HttpResponse response, URI uri) throws IOException {
         response.sendError(HttpResponse.__403_Forbidden, "Forbidden for Proxy");
     }
 
     /* ------------------------------------------------------------ */
 
     /**
      * @return Returns the anonymous.
      */
     public boolean isAnonymous() {
         return _anonymous;
     }
 
     /* ------------------------------------------------------------ */
 
     /**
      * @param anonymous The anonymous to set.
      */
     public void setAnonymous(boolean anonymous) {
         _anonymous = anonymous;
     }
 
     private static class SslRelay extends SslListener
     {
         InetAddrPort _addr;
 
         SslRelay(InetAddrPort addr)
         {
             _addr=addr;
         }
 
         protected void customizeRequest(Socket socket, HttpRequest request)
         {
             super.customizeRequest(socket,request);
             URI uri=request.getURI();
 
             // Convert the URI to a proxy URL
             uri.setScheme("https");
             uri.setHost(_addr.getHost());
             uri.setPort(_addr.getPort());
         }
     }
 }
