 /*
  *  Licensed to the Apache Software Foundation (ASF) under one
  *  or more contributor license agreements.  See the NOTICE file
  *  distributed with this work for additional information
  *  regarding copyright ownership.  The ASF licenses this file
  *  to you under the Apache License, Version 2.0 (the
  *  "License"); you may not use this file except in compliance
  *  with the License.  You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing,
  *  software distributed under the License is distributed on an
  *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  *  KIND, either express or implied.  See the License for the
  *  specific language governing permissions and limitations
  *  under the License.
  */
 package org.apache.synapse.transport.nhttp;
 
 import org.apache.axiom.om.OMOutputFormat;
 import org.apache.axis2.AxisFault;
 import org.apache.axis2.Constants;
 import org.apache.axis2.addressing.AddressingConstants;
 import org.apache.axis2.addressing.AddressingHelper;
 import org.apache.axis2.addressing.EndpointReference;
 import org.apache.axis2.context.ConfigurationContext;
 import org.apache.axis2.context.MessageContext;
 import org.apache.axis2.description.TransportOutDescription;
 import org.apache.axis2.description.Parameter;
 import org.apache.axis2.handlers.AbstractHandler;
 import org.apache.axis2.transport.MessageFormatter;
 import org.apache.axis2.transport.OutTransportInfo;
 import org.apache.axis2.transport.TransportSender;
 import org.apache.axis2.transport.base.BaseConstants;
 import org.apache.axis2.transport.base.ManagementSupport;
 import org.apache.axis2.transport.base.MetricsCollector;
 import org.apache.axis2.transport.base.TransportMBeanSupport;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.http.*;
 import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
 import org.apache.http.impl.nio.reactor.SSLIOSessionHandler;
 import org.apache.http.nio.NHttpClientConnection;
 import org.apache.http.nio.NHttpClientHandler;
 import org.apache.http.nio.reactor.IOEventDispatch;
 import org.apache.http.nio.reactor.IOReactorExceptionHandler;
 import org.apache.http.nio.reactor.SessionRequest;
 import org.apache.http.nio.reactor.SessionRequestCallback;
 import org.apache.http.params.BasicHttpParams;
 import org.apache.http.params.HttpConnectionParams;
 import org.apache.http.params.HttpParams;
 import org.apache.http.params.HttpProtocolParams;
 import org.apache.http.protocol.HTTP;
 import org.apache.axis2.transport.base.threads.NativeThreadFactory;
 import org.apache.synapse.transport.nhttp.util.MessageFormatterDecoratorFactory;
 import org.apache.synapse.transport.nhttp.util.NhttpUtil;
 
 import javax.net.ssl.SSLContext;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InterruptedIOException;
 import java.io.OutputStream;
 import java.net.InetSocketAddress;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.*;
 
 /**
  * NIO transport sender for Axis2 based on HttpCore and NIO extensions
  */
 public class HttpCoreNIOSender extends AbstractHandler implements TransportSender, ManagementSupport {
 
     private static final Log log = LogFactory.getLog(HttpCoreNIOSender.class);
 
     /** The Axis2 configuration context */
     private ConfigurationContext cfgCtx;
     /** The IOReactor */
     private DefaultConnectingIOReactor ioReactor = null;
     /** The client handler */
     private ClientHandler handler = null;
     /** The session request callback that calls back to the message receiver with errors */
     private final SessionRequestCallback sessionRequestCallback = getSessionRequestCallback();
     /** The SSL Context to be used */
     private SSLContext sslContext = null;
     /** The SSL session handler that manages hostname verification etc */
     private SSLIOSessionHandler sslIOSessionHandler = null;
     /** JMX support */
     private TransportMBeanSupport mbeanSupport;
     /** Metrics collector for the sender */
     private MetricsCollector metrics = new MetricsCollector();
     /** state of the listener */
     private int state = BaseConstants.STOPPED;
     /** The proxy host */
     private String proxyHost = null;
     /** The proxy port */
     private int proxyPort = 80;
     /** The list of hosts for which the proxy should be bypassed */
     private String[] proxyBypassList = new String[0];
     /** The list of known hosts to bypass proxy */
     private List<String> knownDirectHosts = new ArrayList<String>();
     /** The list of known hosts to go via proxy */
     private List<String> knownProxyHosts = new ArrayList<String>();
 
     /**
      * Initialize the transport sender, and execute reactor in new seperate thread
      * @param cfgCtx the Axis2 configuration context
      * @param transportOut the description of the http/s transport from Axis2 configuration
      * @throws AxisFault thrown on an error
      */
     public void init(ConfigurationContext cfgCtx, TransportOutDescription transportOut) throws AxisFault {
         this.cfgCtx = cfgCtx;
 
         // is this an SSL Sender?
         sslContext = getSSLContext(transportOut);
         sslIOSessionHandler = getSSLIOSessionHandler(transportOut);
 
         // configure proxy settings - only supports HTTP right now (See SYNAPSE-418)
         if (sslContext == null) {
             Parameter proxyHostParam = transportOut.getParameter("http.proxyHost");
             if (proxyHostParam != null || System.getProperty("http.proxyHost") != null) {
                 if (proxyHostParam != null) {
                     proxyHost = (String) proxyHostParam.getValue();
                 } else {
                     proxyHost = System.getProperty("http.proxyHost");
                 }
 
                 Parameter proxyPortParam = transportOut.getParameter("http.proxyPort");
                 if (proxyPortParam != null) {
                     proxyPort = Integer.parseInt((String) proxyPortParam.getValue());
                 } else if (System.getProperty("http.proxyPort") != null) {
                     proxyPort = Integer.parseInt(System.getProperty("http.proxyPort"));
                 }
 
                 Parameter bypassList = transportOut.getParameter("http.nonProxyHosts");
                 if (bypassList != null) {
                     proxyBypassList = ((String) bypassList.getValue()).split("\\|");
                 } else if (System.getProperty("http.nonProxyHosts") != null) {
                     proxyBypassList = (System.getProperty("http.nonProxyHosts")).split("\\|");
                 }
 
                 log.info("HTTP Sender using Proxy : "
                     + proxyHost + ":" + proxyPort + " bypassing : " + Arrays.toString(proxyBypassList));
             }
         }
         
         Parameter param = transportOut.getParameter("warnOnHTTP500");
         if (param != null) {
             String[] warnOnHttp500 = ((String) param.getValue()).split("\\|");
             cfgCtx.setNonReplicableProperty("warnOnHTTP500", warnOnHttp500);
         }
 
         HttpParams params = getClientParameters();
         try {
             String prefix = (sslContext == null ? "http" : "https") + "-Sender I/O dispatcher";
             ioReactor = new DefaultConnectingIOReactor(
                 NHttpConfiguration.getInstance().getClientIOWorkers(),
                 new NativeThreadFactory(new ThreadGroup(prefix + " thread group"), prefix), params);
             ioReactor.setExceptionHandler(new IOReactorExceptionHandler() {
                 public boolean handle(IOException ioException) {
                     log.warn("System may be unstable: IOReactor encountered a checked exception : " +
                         ioException.getMessage(), ioException);
                     return true;
                 }
 
                 public boolean handle(RuntimeException runtimeException) {
                     log.warn("System may be unstable: IOReactor encountered a runtime exception : " +
                         runtimeException.getMessage(), runtimeException);
                     return true;
                 }
             });
         } catch (IOException e) {
             log.error("Error starting the IOReactor", e);
         }
 
         handler = new ClientHandler(cfgCtx, params, metrics);
         final IOEventDispatch ioEventDispatch = getEventDispatch(
             handler, sslContext, sslIOSessionHandler, params);
 
         // start the Sender in a new seperate thread
         Thread t = new Thread(new Runnable() {
             public void run() {
                 try {
                     ioReactor.execute(ioEventDispatch);
                 } catch (InterruptedIOException ex) {
                     log.fatal("Reactor Interrupted");
                 } catch (IOException e) {
                     log.fatal("Encountered an I/O error: " + e.getMessage(), e);
                 }
                 log.info((sslContext == null ? "HTTP" : "HTTPS") + " Sender Shutdown");
             }
         }, "HttpCoreNIOSender");
         t.start();
         log.info((sslContext == null ? "HTTP" : "HTTPS") + " Sender starting");
 
         // register with JMX
         mbeanSupport
             = new TransportMBeanSupport(this, "nio-http" + (sslContext == null ? "" : "s"));
         mbeanSupport.register();
         
         state = BaseConstants.STARTED;
     }
 
     /**
      * Return the IOEventDispatch implementation to be used. This is overridden by the
      * SSL sender
      * @param handler
      * @param sslContext
      * @param params
      * @return
      */
     protected IOEventDispatch getEventDispatch(
         NHttpClientHandler handler, SSLContext sslContext,
         SSLIOSessionHandler sslIOSessionHandler, HttpParams params) {
         return new PlainClientIOEventDispatch(handler, params);
     }
 
     /**
      * Always return null, as this implementation does not support outgoing SSL
      * @param transportOut
      * @return null
      * @throws AxisFault
      */
     protected SSLContext getSSLContext(TransportOutDescription transportOut) throws AxisFault {
         return null;
     }
 
     /**
      * Create the SSL IO Session handler to be used by this listener
      * @param transportOut
      * @return always null
      */
     protected SSLIOSessionHandler getSSLIOSessionHandler(TransportOutDescription transportOut)
         throws AxisFault {
         return null;
     }
 
     /**
      * get HTTP protocol parameters to which the sender must adhere to
      * @return the applicable HTTP protocol parameters
      */
     private HttpParams getClientParameters() {
         NHttpConfiguration cfg = NHttpConfiguration.getInstance();
         HttpParams params = new BasicHttpParams();
         params
             .setIntParameter(HttpConnectionParams.SO_TIMEOUT,
                 cfg.getProperty(HttpConnectionParams.SO_TIMEOUT, 60000))
             .setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT,
                 cfg.getProperty(HttpConnectionParams.CONNECTION_TIMEOUT, 10000))
             .setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE,
                 cfg.getProperty(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024))
             .setBooleanParameter(HttpConnectionParams.STALE_CONNECTION_CHECK,
                 cfg.getProperty(HttpConnectionParams.STALE_CONNECTION_CHECK, 0) == 1)
             .setBooleanParameter(HttpConnectionParams.TCP_NODELAY,
                 cfg.getProperty(HttpConnectionParams.TCP_NODELAY, 1) == 1)
             .setParameter(HttpProtocolParams.USER_AGENT, "Synapse-HttpComponents-NIO");
         return params;
     }
 
     /**
      * transport sender invocation from Axis2 core
      * @param msgContext message to be sent
      * @return the invocation response (always InvocationResponse.CONTINUE)
      * @throws AxisFault on error
      */
     public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {
 
         // remove unwanted HTTP headers (if any from the current message)
         removeUnwantedHeaders(msgContext);
 
         if (AddressingHelper.isReplyRedirected(msgContext)
                 && !msgContext.getReplyTo().hasNoneAddress()) {
 
             msgContext.setProperty(NhttpConstants.IGNORE_SC_ACCEPTED, Constants.VALUE_TRUE);
         }
 
         EndpointReference epr = NhttpUtil.getDestinationEPR(msgContext);
         if (epr != null) {
             if (!epr.hasNoneAddress()) {
                 sendAsyncRequest(epr, msgContext);
             } else {
                 handleException("Cannot send message to " + AddressingConstants.Final.WSA_NONE_URI);
             }
         } else {
 
             if (msgContext.getProperty(Constants.OUT_TRANSPORT_INFO) != null) {
                 if (msgContext.getProperty(Constants.OUT_TRANSPORT_INFO) instanceof ServerWorker) {
                     sendAsyncResponse(msgContext);
                 } else {
                     sendUsingOutputStream(msgContext);
                 }
             } else {
                 handleException("No valid destination EPR or OutputStream to send message");
             }
         }
 
         if (msgContext.getOperationContext() != null) {
             msgContext.getOperationContext().setProperty(
                 Constants.RESPONSE_WRITTEN, Constants.VALUE_TRUE);
         }
 
         return InvocationResponse.CONTINUE;
     }
 
     /**
      * Remove unwanted headers from the http response of outgoing request. These are headers which
      * should be dictated by the transport and not the user. We remove these as these may get
      * copied from the request messages
      * @param msgContext the Axis2 Message context from which these headers should be removed
      */
     private void removeUnwantedHeaders(MessageContext msgContext) {
         Map headers = (Map) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);
 
         if (headers == null || headers.isEmpty()) {
             return;
         }
 
         Iterator iter = headers.keySet().iterator();
         while (iter.hasNext()) {
             String headerName = (String) iter.next();
             if (HTTP.CONN_DIRECTIVE.equalsIgnoreCase(headerName) ||
                 HTTP.TRANSFER_ENCODING.equalsIgnoreCase(headerName) ||
                 HTTP.DATE_HEADER.equalsIgnoreCase(headerName) ||
                 HTTP.SERVER_HEADER.equalsIgnoreCase(headerName) ||
                 HTTP.CONTENT_TYPE.equalsIgnoreCase(headerName) ||
                 HTTP.CONTENT_LEN.equalsIgnoreCase(headerName) ||
                 HTTP.USER_AGENT.equalsIgnoreCase(headerName)) {
                 iter.remove();
             }
         }
     }
 
     /**
      * Send the request message asynchronously to the given EPR
      * @param epr the destination EPR for the message
      * @param msgContext the message being sent
      * @throws AxisFault on error
      */
     private void sendAsyncRequest(EndpointReference epr, MessageContext msgContext) throws AxisFault {
         try {
             URL url = new URL(epr.getAddress());
             String host = url.getHost();
             int port = url.getPort();
             if (port == -1) {
                 // use default
                 if ("http".equals(url.getProtocol())) {
                     port = 80;
                 } else if ("https".equals(url.getProtocol())) {
                     port = 443;
                 }
             }
             HttpHost httpHost = new HttpHost(host, port, url.getProtocol());
 
             Axis2HttpRequest axis2Req = new Axis2HttpRequest(epr, httpHost, msgContext);
             Object timeout = msgContext.getProperty(NhttpConstants.SEND_TIMEOUT);
             if (timeout != null && timeout instanceof Long) {
                 axis2Req.setTimeout( (int) ((Long) timeout).longValue());
             }
 
             // do we have a proxy configured?
             if (proxyHost != null) {
                 // but are we supposed to bypass for this host?
                 if (knownProxyHosts.contains(host)) {
                     // this has already been found to be a proxy host
                     host = proxyHost;
                     port = proxyPort;
                 } else if (knownDirectHosts.contains(host)) {
                     // do nothing, let this request go directly bypassing proxy
                 } else {
                     // we are encountering this host:port pair for the first time
                     if (!isBypass(host)) {
                         host = proxyHost;
                         port = proxyPort;
                     }
                 }
             }
             
             NHttpClientConnection conn = ConnectionPool.getConnection(host, port);
 
             if (conn == null) {
                 ioReactor.connect(new InetSocketAddress(host, port),
                     null, axis2Req, sessionRequestCallback);
                 if (log.isDebugEnabled()) {
                     log.debug("A new connection established to : " + host + ":" + port);
                 }
             } else {
                 try {
                     handler.submitRequest(conn, axis2Req);
                     if (log.isDebugEnabled()) {
                         log.debug("An existing connection reused to : " + host + ":" + port);
                     }                    
                 } catch (ConnectionClosedException e) {
                     ioReactor.connect(new InetSocketAddress(host, port),
                         null, axis2Req, sessionRequestCallback);
                     if (log.isDebugEnabled()) {
                         log.debug("A new connection established to : " + host + ":" + port);
                     }
                 }
             }
 
             try {
                 axis2Req.streamMessageContents();
                 if (metrics != null) {
                     if (metrics.getLevel() == MetricsCollector.LEVEL_FULL) {
                         metrics.incrementMessagesSent(msgContext);
                     } else {
                         metrics.incrementMessagesSent();
                     }
                 }
 
             } catch (AxisFault af) {
                 throw af;
             }
 
         } catch (MalformedURLException e) {
             handleException("Malformed destination EPR : " + epr.getAddress(), e);
         }
     }
 
     /**
      * Send the passed in response message, asynchronously
      * @param msgContext the message context to be sent
      * @throws AxisFault on error
      */
     private void sendAsyncResponse(MessageContext msgContext) throws AxisFault {
 
         // remove unwanted HTTP headers (if any from the current message)
         removeUnwantedHeaders(msgContext);
         
         ServerWorker worker = (ServerWorker) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);
         HttpResponse response = worker.getResponse();
 
         OMOutputFormat format = NhttpUtil.getOMOutputFormat(msgContext);
         MessageFormatter messageFormatter =
                 MessageFormatterDecoratorFactory.createMessageFormatterDecorator(msgContext);
         Boolean noEntityBody = (Boolean) msgContext.getProperty(NhttpConstants.NO_ENTITY_BODY);
         if (noEntityBody == null || Boolean.FALSE == noEntityBody) {
             response.setHeader(
                 HTTP.CONTENT_TYPE,
                 messageFormatter.getContentType(msgContext, format, msgContext.getSoapAction()));
         }
         response.setStatusCode(determineHttpStatusCode(msgContext, response));
 
         // set any transport headers
         Map transportHeaders = (Map) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);
         if (transportHeaders != null && !transportHeaders.values().isEmpty()) {
             Iterator iter = transportHeaders.keySet().iterator();
             while (iter.hasNext()) {
                 Object header = iter.next();
                 Object value = transportHeaders.get(header);
                 if (value != null && header instanceof String && value instanceof String) {
                     response.setHeader((String) header, (String) value);
                 }
             }
         }
 
         MetricsCollector lstMetrics = worker.getServiceHandler().getMetrics();
         try {
             worker.getServiceHandler().commitResponse(worker.getConn(), response);
             lstMetrics.reportResponseCode(response.getStatusLine().getStatusCode());
             OutputStream out = worker.getOutputStream();
 
             /*
              * if this is a dummy message to handle http 202 case with non-blocking IO
              * write an empty byte array as body
              */
             if (msgContext.isPropertyTrue(NhttpConstants.SC_ACCEPTED)
                 || Boolean.TRUE == noEntityBody) {
                 out.write(new byte[0]);
             } else {
                 messageFormatter.writeTo(msgContext, format, out, false);
             }
             out.close();
             if (lstMetrics != null) {
                 lstMetrics.incrementMessagesSent();
             }
 
         } catch (HttpException e) {
             if (lstMetrics != null) {
                 lstMetrics.incrementFaultsSending();
             }
             handleException("Unexpected HTTP protocol error sending response to : " +
                 worker.getRemoteAddress(), e);
         } catch (ConnectionClosedException e) {
             if (lstMetrics != null) {
                 lstMetrics.incrementFaultsSending();
             }
             log.warn("Connection closed by client : " + worker.getRemoteAddress());
         } catch (IllegalStateException e) {
             if (lstMetrics != null) {
                 lstMetrics.incrementFaultsSending();
             }
             log.warn("Connection closed by client : " + worker.getRemoteAddress());
         } catch (IOException e) {
             if (lstMetrics != null) {
                 lstMetrics.incrementFaultsSending();
             }
             handleException("IO Error sending response message to : " +
                 worker.getRemoteAddress(), e);
         } catch (Exception e) {
             if (lstMetrics != null) {
                 lstMetrics.incrementFaultsSending();
             }
             handleException("General Error sending response message to : " +
                 worker.getRemoteAddress(), e);
         }
 
         InputStream is = worker.getIs();
         if (is != null) {
             try {
                 is.close();
             } catch (IOException ignore) {}
         }
     }
 
     /**
      * Determine the HttpStatusCodedepending on the message type processed <br>
      * (normal response versus fault response) as well as Axis2 message context properties set
      * via Synapse configuration or MessageBuilders.
      * 
      * @see NhttpConstants.FAULTS_AS_HTTP_200
      * @see NhttpConstants.HTTP_SC
      * 
      * @param msgContext the Axis2 message context 
      * @param response the HTTP response object
      * 
      * @return the HTTP status code to set in the HTTP response object
      */
     private int determineHttpStatusCode(MessageContext msgContext, HttpResponse response) {
         
         int httpStatus = HttpStatus.SC_OK;
         
         // retrieve original status code (if present)
         if (response.getStatusLine() != null) {
             httpStatus = response.getStatusLine().getStatusCode();
         }
         
         // if this is a dummy message to handle http 202 case with non-blocking IO
         // set the status code to 202
         if (msgContext.isPropertyTrue(NhttpConstants.SC_ACCEPTED)) {
             httpStatus = HttpStatus.SC_ACCEPTED;
         } else {            
             
             // is this a fault message
             boolean handleFault = 
                 msgContext.getEnvelope().getBody().hasFault() || msgContext.isProcessingFault();
             
             // shall faults be transmitted with HTTP 200
             boolean faultsAsHttp200 =
                 NhttpConstants.TRUE.equals(
                     msgContext.getProperty(NhttpConstants.FAULTS_AS_HTTP_200));
             
             // Set HTTP status code to 500 if this is a fault case and we shall not use HTTP 200
             if (handleFault && !faultsAsHttp200) {
                 httpStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
             }
             
             /* 
              * Any status code previously set shall be overwritten with the value of the following
              * message context property if it is set. 
              */
             Object statusCode = msgContext.getProperty(NhttpConstants.HTTP_SC);
             if (statusCode != null) {
                 try {
                     httpStatus = Integer.parseInt(
                             msgContext.getProperty(NhttpConstants.HTTP_SC).toString());
                 } catch (NumberFormatException e) {
                     log.warn("Unable to set the HTTP status code from the property " 
                             + NhttpConstants.HTTP_SC + " with value: " + statusCode);
                 }
             }
         }
         
         return httpStatus;
     }
 
     private void sendUsingOutputStream(MessageContext msgContext) throws AxisFault {
 
         OMOutputFormat format = NhttpUtil.getOMOutputFormat(msgContext);
         MessageFormatter messageFormatter =
                 MessageFormatterDecoratorFactory.createMessageFormatterDecorator(msgContext);
         OutputStream out = (OutputStream) msgContext.getProperty(MessageContext.TRANSPORT_OUT);
 
         if (msgContext.isServerSide()) {
             OutTransportInfo transportInfo =
                 (OutTransportInfo) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);
 
             if (transportInfo != null) {
                 transportInfo.setContentType(
                 messageFormatter.getContentType(msgContext, format, msgContext.getSoapAction()));
             } else {
                 throw new AxisFault(Constants.OUT_TRANSPORT_INFO + " has not been set");
             }
         }
 
         try {
             messageFormatter.writeTo(msgContext, format, out, false);
             out.close();
         } catch (IOException e) {
             handleException("IO Error sending response message", e);
         }
     }
 
 
     public void cleanup(MessageContext msgContext) throws AxisFault {
         // do nothing
     }
 
     public void stop() {
         if (state == BaseConstants.STOPPED) return;
         try {
             ioReactor.shutdown();
             handler.stop();
             state = BaseConstants.STOPPED;
         } catch (IOException e) {
             log.warn("Error shutting down IOReactor", e);
         }
         mbeanSupport.unregister();
     }
 
     /**
      * Return a SessionRequestCallback which gets notified of a connection failure
      * or an error during a send operation. This method finds the corresponding
      * Axis2 message context for the outgoing request, and find the message receiver
      * and sends a fault message back to the message receiver that is marked as
      * related to the outgoing request
      * @return a Session request callback
      */
     private SessionRequestCallback getSessionRequestCallback() {
         return new SessionRequestCallback() {
             public void completed(SessionRequest request) {
             }
 
             public void failed(SessionRequest request) {
                handleError(request, NhttpConstants.CONNECTION_FAILED, 
                    "Connection refused or failed for : " + request.getRemoteAddress() + ", " +
                    "IO Exception occured : " + request.getException().getMessage());
             }
 
             public void timeout(SessionRequest request) {
                 handleError(request, NhttpConstants.CONNECT_TIMEOUT,
                     "Timeout connecting to : " + request.getRemoteAddress());
                 request.cancel();
             }
 
             public void cancelled(SessionRequest request) {
                 handleError(request, NhttpConstants.CONNECT_CANCEL,
                     "Connection cancelled for : " + request.getRemoteAddress());
             }
 
             private void handleError(SessionRequest request, int errorCode, String errorMessage) {
                 if (request.getAttachment() != null &&
                     request.getAttachment() instanceof Axis2HttpRequest) {
 
                     Axis2HttpRequest axis2Request = (Axis2HttpRequest) request.getAttachment();
                     if (!axis2Request.isCompleted()) {
                         handler.markRequestCompletedWithError(
                             axis2Request, errorCode,  errorMessage,  null);
                     }
                 }
             }
         };
     }
 
     // -------------- utility methods -------------
     private boolean isBypass(String hostName) {
         for (String entry : proxyBypassList) {
             if (hostName.matches(entry)) {
                 knownDirectHosts.add(hostName);
                 return true;
             }
         }
         knownProxyHosts.add(hostName);
         return false;
     }
 
     private void handleException(String msg, Exception e) throws AxisFault {
         log.error(msg, e);
         throw new AxisFault(msg, e);
     }
 
     private void handleException(String msg) throws AxisFault {
         log.error(msg);
         throw new AxisFault(msg);
     }
 
     public void pause() throws AxisFault {
         if (state != BaseConstants.STARTED) return;
         state = BaseConstants.PAUSED;
         log.info((sslContext == null ? "HTTP" : "HTTPS") + "Sender Paused");
     }
 
     public void resume() throws AxisFault {
         if (state != BaseConstants.PAUSED) return;
         state = BaseConstants.STARTED;
         log.info((sslContext == null ? "HTTP" : "HTTPS") + "Sender Resumed");
     }
 
     public void maintenenceShutdown(long millis) throws AxisFault {
         if (state != BaseConstants.STARTED) return;
         try {
             long start = System.currentTimeMillis();
             ioReactor.shutdown(millis);
             state = BaseConstants.STOPPED;
             log.info("Sender shutdown in : " + (System.currentTimeMillis() - start) / 1000 + "s");
         } catch (IOException e) {
             handleException("Error shutting down the IOReactor for maintenence", e);
         }
     }
 
     /**
      * Returns the number of active threads processing messages
      * @return number of active threads processing messages
      */    
     public int getActiveThreadCount() {
         return handler.getActiveCount();
     }
 
     /**
      * Returns the number of requestes queued in the thread pool
      * @return queue size
      */
     public int getQueueSize() {
         return handler.getQueueSize();
     }
 
     // -- jmx/management methods--
     public long getMessagesReceived() {
         if (metrics != null) {
             return metrics.getMessagesReceived();
         }
         return -1;
     }
 
     public long getFaultsReceiving() {
         if (metrics != null) {
             return metrics.getFaultsReceiving();
         }
         return -1;
     }
 
     public long getBytesReceived() {
         if (metrics != null) {
             return metrics.getBytesReceived();
         }
         return -1;
     }
 
     public long getMessagesSent() {
         if (metrics != null) {
             return metrics.getMessagesSent();
         }
         return -1;
     }
 
     public long getFaultsSending() {
         if (metrics != null) {
             return metrics.getFaultsSending();
         }
         return -1;
     }
 
     public long getBytesSent() {
         if (metrics != null) {
             return metrics.getBytesSent();
         }
         return -1;
     }
 
     public long getTimeoutsReceiving() {
         if (metrics != null) {
             return metrics.getTimeoutsReceiving();
         }
         return -1;
     }
 
     public long getTimeoutsSending() {
         if (metrics != null) {
             return metrics.getTimeoutsSending();
         }
         return -1;
     }
 
     public long getMinSizeReceived() {
         if (metrics != null) {
             return metrics.getMinSizeReceived();
         }
         return -1;
     }
 
     public long getMaxSizeReceived() {
         if (metrics != null) {
             return metrics.getMaxSizeReceived();
         }
         return -1;
     }
 
     public double getAvgSizeReceived() {
         if (metrics != null) {
             return metrics.getAvgSizeReceived();
         }
         return -1;
     }
 
     public long getMinSizeSent() {
         if (metrics != null) {
             return metrics.getMinSizeSent();
         }
         return -1;
     }
 
     public long getMaxSizeSent() {
         if (metrics != null) {
             return metrics.getMaxSizeSent();
         }
         return -1;
     }
 
     public double getAvgSizeSent() {
         if (metrics != null) {
             return metrics.getAvgSizeSent();
         }
         return -1;
     }
 
     public Map getResponseCodeTable() {
         if (metrics != null) {
             return metrics.getResponseCodeTable();
         }
         return null;
     }
 
     public void resetStatistics() {
         if (metrics != null) {
             metrics.reset();
         }
     }
 
     public long getLastResetTime() {
         if (metrics != null) {
             return metrics.getLastResetTime();
         }
         return -1;
     }
 
     public long getMetricsWindow() {
         if (metrics != null) {
             return System.currentTimeMillis() - metrics.getLastResetTime();
         }
         return -1;
     }
 }
