 /**
  * Copyright 2005-2010 Noelios Technologies.
  * 
  * The contents of this file are subject to the terms of one of the following
  * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
  * "Licenses"). You can select the license that you prefer but you may not use
  * this file except in compliance with one of these Licenses.
  * 
  * You can obtain a copy of the LGPL 3.0 license at
  * http://www.opensource.org/licenses/lgpl-3.0.html
  * 
  * You can obtain a copy of the LGPL 2.1 license at
  * http://www.opensource.org/licenses/lgpl-2.1.php
  * 
  * You can obtain a copy of the CDDL 1.0 license at
  * http://www.opensource.org/licenses/cddl1.php
  * 
  * You can obtain a copy of the EPL 1.0 license at
  * http://www.opensource.org/licenses/eclipse-1.0.php
  * 
  * See the Licenses for the specific language governing permissions and
  * limitations under the Licenses.
  * 
  * Alternatively, you can obtain a royalty free commercial license with less
  * limitations, transferable or non-transferable, directly at
  * http://www.noelios.com/products/restlet-engine
  * 
  * Restlet is a registered trademark of Noelios Technologies.
  */
 
 package org.restlet.engine.http.connector;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.InetSocketAddress;
 import java.net.Socket;
 import java.net.UnknownHostException;
 import java.nio.channels.SocketChannel;
 import java.security.GeneralSecurityException;
 import java.security.KeyStore;
 import java.security.SecureRandom;
 import java.util.concurrent.CountDownLatch;
 import java.util.logging.Level;
 
 import javax.net.SocketFactory;
 import javax.net.ssl.KeyManager;
 import javax.net.ssl.KeyManagerFactory;
 import javax.net.ssl.SSLContext;
 import javax.net.ssl.TrustManager;
 import javax.net.ssl.TrustManagerFactory;
 
 import org.restlet.Client;
 import org.restlet.Request;
 import org.restlet.Response;
 import org.restlet.data.Reference;
 import org.restlet.data.Status;
 
 /**
  * Base client helper based on NIO blocking sockets. Here is the list of
  * parameters that are supported. They should be set in the Client's context
  * before it is started:
  * <table>
  * <tr>
  * <th>Parameter name</th>
  * <th>Value type</th>
  * <th>Default value</th>
  * <th>Description</th>
  * </tr>
  * <tr>
  * <td>tcpNoDelay</td>
  * <td>boolean</td>
  * <td>false</td>
  * <td>Indicate if Nagle's TCP_NODELAY algorithm should be used.</td>
  * </tr>
  * <td>keystorePath</td>
  * <td>String</td>
  * <td>${user.home}/.keystore</td>
  * <td>SSL keystore path.</td>
  * </tr>
  * <tr>
  * <td>keystorePassword</td>
  * <td>String</td>
  * <td>System property "javax.net.ssl.keyStorePassword"</td>
  * <td>SSL keystore password.</td>
  * </tr>
  * <tr>
  * <td>keystoreType</td>
  * <td>String</td>
  * <td>JKS</td>
  * <td>SSL keystore type</td>
  * </tr>
  * <tr>
  * <td>keyPassword</td>
  * <td>String</td>
  * <td>System property "javax.net.ssl.keyStorePassword"</td>
  * <td>SSL key password.</td>
  * </tr>
  * <tr>
  * <td>certAlgorithm</td>
  * <td>String</td>
  * <td>SunX509</td>
  * <td>SSL certificate algorithm.</td>
  * </tr>
  * <tr>
  * <td>secureRandomAlgorithm</td>
  * <td>String</td>
  * <td>null (see java.security.SecureRandom)</td>
  * <td>Name of the RNG algorithm. (see java.security.SecureRandom class).</td>
  * </tr>
  * <tr>
  * <td>securityProvider</td>
  * <td>String</td>
  * <td>null (see javax.net.ssl.SSLContext)</td>
  * <td>Java security provider name (see java.security.Provider class).</td>
  * </tr>
  * <tr>
  * <td>sslProtocol</td>
  * <td>String</td>
  * <td>TLS</td>
  * <td>SSL protocol.</td>
  * </tr>
  * <tr>
  * <td>truststoreType</td>
  * <td>String</td>
  * <td>System property "javax.net.ssl.trustStoreType"</td>
  * <td>Trust store type</td>
  * </tr>
  * <tr>
  * <td>truststorePath</td>
  * <td>String</td>
  * <td>null</td>
  * <td>Path to trust store</td>
  * </tr>
  * <tr>
  * <td>truststorePassword</td>
  * <td>String</td>
  * <td>System property "javax.net.ssl.trustStorePassword"</td>
  * <td>Trust store password</td>
  * </tr>
  * </table>
  * 
  * @author Jerome Louvel
  */
 public class BaseClientHelper extends BaseHelper<Client> {
 
     /** The regular socket factory. */
     private volatile SocketFactory regularSocketFactory;
 
     /** The secure socket factory. */
     private volatile SocketFactory secureSocketFactory;
 
     /**
      * Constructor.
      * 
      * @param connector
      *            The helped client connector.
      */
     public BaseClientHelper(Client connector) {
         super(connector, true);
         this.regularSocketFactory = null;
         this.secureSocketFactory = null;
     }
 
     @Override
     protected Connection<Client> createConnection(BaseHelper<Client> helper,
             Socket socket, SocketChannel socketChannel) throws IOException {
         return new ClientConnection(helper, socket, socketChannel);
     }
 
     /**
      * Creates a properly configured secure socket factory.
      * 
      * @return Properly configured secure socket factory.
      * @throws IOException
      * @throws GeneralSecurityException
      */
     protected SocketFactory createSecureSocketFactory() throws IOException,
             GeneralSecurityException {
         // Retrieve the configuration variables
         String certAlgorithm = getCertAlgorithm();
         String keystorePath = getKeystorePath();
         String keystorePassword = getKeystorePassword();
         String keyPassword = getKeyPassword();
         String truststoreType = getTruststoreType();
         String truststorePath = getTruststorePath();
         String truststorePassword = getTruststorePassword();
         String secureRandomAlgorithm = getSecureRandomAlgorithm();
         String securityProvider = getSecurityProvider();
 
         // Initialize a key store
         InputStream keystoreInputStream = null;
         if ((keystorePath != null) && (new File(keystorePath).exists())) {
             keystoreInputStream = new FileInputStream(keystorePath);
         }
 
         KeyStore keystore = null;
         if (keystoreInputStream != null) {
             try {
                 keystore = KeyStore.getInstance(getKeystoreType());
                 keystore.load(keystoreInputStream,
                         keystorePassword == null ? null : keystorePassword
                                 .toCharArray());
             } catch (IOException ioe) {
                 getLogger().log(Level.WARNING, "Unable to load the key store",
                         ioe);
                 keystore = null;
             }
         }
 
         KeyManager[] keyManagers = null;
         if ((keystore != null) && (keyPassword != null)) {
             // Initialize a key manager
             KeyManagerFactory keyManagerFactory = KeyManagerFactory
                     .getInstance(certAlgorithm);
             keyManagerFactory.init(keystore, keyPassword.toCharArray());
             keyManagers = keyManagerFactory.getKeyManagers();
         }
 
         // Initialize the trust store
         InputStream truststoreInputStream = null;
         if ((truststorePath != null) && (new File(truststorePath).exists())) {
             truststoreInputStream = new FileInputStream(truststorePath);
         }
 
         KeyStore truststore = null;
         if ((truststoreType != null) && (truststoreInputStream != null)) {
             try {
                 truststore = KeyStore.getInstance(truststoreType);
                 truststore.load(truststoreInputStream,
                         truststorePassword == null ? null : truststorePassword
                                 .toCharArray());
             } catch (IOException ioe) {
                 getLogger().log(Level.WARNING,
                         "Unable to load the trust store", ioe);
                 truststore = null;
             }
         }
 
         TrustManager[] trustManagers = null;
         if (truststore != null) {
             // Initialize the trust manager
             TrustManagerFactory trustManagerFactory = TrustManagerFactory
                     .getInstance(certAlgorithm);
             trustManagerFactory.init(truststore);
             trustManagers = trustManagerFactory.getTrustManagers();
         }
 
         // Initialize the SSL context
         SecureRandom secureRandom = secureRandomAlgorithm == null ? null
                 : SecureRandom.getInstance(secureRandomAlgorithm);
 
         SSLContext context = securityProvider == null ? SSLContext
                 .getInstance(getSslProtocol()) : SSLContext.getInstance(
                 getSslProtocol(), securityProvider);
         context.init(keyManagers, trustManagers, secureRandom);
 
         // Return the SSL socket factory
         return context.getSocketFactory();
     }
 
     /**
      * Creates the socket that will be used to send the request and get the
      * response.
      * 
      * @param hostDomain
      *            The target host domain name.
      * @param hostPort
      *            The target host port.
      * @return The created socket.
      * @throws UnknownHostException
      * @throws IOException
      */
     public Socket createSocket(boolean secure, String hostDomain, int hostPort)
             throws UnknownHostException, IOException {
         Socket result = null;
         SocketFactory factory = getSocketFactory(secure);
 
         if (factory != null) {
             result = factory.createSocket();
             InetSocketAddress address = new InetSocketAddress(hostDomain,
                     hostPort);
             result.connect(address, getConnectTimeout());
             result.setTcpNoDelay(getTcpNoDelay());
         }
 
         return result;
     }
 
     /**
      * Creates a normal or secure socket factory.
      * 
      * @param secure
      *            Indicates if the sockets should be secured.
      * @return A normal or secure socket factory.
      */
     protected SocketFactory createSocketFactory(boolean secure) {
         SocketFactory result = null;
 
         if (secure) {
             try {
                 return createSecureSocketFactory();
             } catch (IOException ex) {
                 getLogger().log(
                         Level.SEVERE,
                         "Could not create secure socket factory: "
                                 + ex.getMessage(), ex);
             } catch (GeneralSecurityException ex) {
                 getLogger().log(
                         Level.SEVERE,
                         "Could not create secure socket factory: "
                                 + ex.getMessage(), ex);
             }
         } else {
             result = SocketFactory.getDefault();
         }
 
         return result;
     }
 
     /**
      * Returns the SSL certificate algorithm.
      * 
      * @return The SSL certificate algorithm.
      */
     public String getCertAlgorithm() {
         return getHelpedParameters().getFirstValue("certAlgorithm", "SunX509");
     }
 
     /**
      * Returns the connection timeout.
      * 
      * @return The connection timeout.
      */
     public int getConnectTimeout() {
         return getHelped().getConnectTimeout();
     }
 
     /**
      * Returns the SSL key password.
      * 
      * @return The SSL key password.
      */
     public String getKeyPassword() {
         return getHelpedParameters().getFirstValue("keyPassword",
                 System.getProperty("javax.net.ssl.keyStorePassword"));
     }
 
     /**
      * Returns the SSL keystore password.
      * 
      * @return The SSL keystore password.
      */
     public String getKeystorePassword() {
         return getHelpedParameters().getFirstValue("keystorePassword",
                 System.getProperty("javax.net.ssl.keyStorePassword"));
     }
 
     /**
      * Returns the SSL keystore path.
      * 
      * @return The SSL keystore path.
      */
     public String getKeystorePath() {
         return getHelpedParameters().getFirstValue("keystorePath",
                 System.getProperty("user.home") + File.separator + ".keystore");
     }
 
     /**
      * Returns the SSL keystore type.
      * 
      * @return The SSL keystore type.
      */
     public String getKeystoreType() {
         return getHelpedParameters().getFirstValue("keystoreType", "JKS");
     }
 
     /**
      * Returns the regular socket factory.
      * 
      * @return The regular socket factory.
      */
     public SocketFactory getRegularSocketFactory() {
         return regularSocketFactory;
     }
 
     /**
      * Returns the name of the RNG algorithm.
      * 
      * @return The name of the RNG algorithm.
      */
     public String getSecureRandomAlgorithm() {
         return getHelpedParameters().getFirstValue("secureRandomAlgorithm",
                 null);
     }
 
     /**
      * Returns the secure socket factory.
      * 
      * @return The secure socket factory.
      */
     public SocketFactory getSecureSocketFactory() {
         return secureSocketFactory;
     }
 
     /**
      * Returns the Java security provider name.
      * 
      * @return The Java security provider name.
      */
     public String getSecurityProvider() {
         return getHelpedParameters().getFirstValue("securityProvider", null);
     }
 
     /**
      * Returns the socket factory.
      * 
      * @param secure
      *            Indicates if a factory for secure sockets is expected.
      * @return The socket factory.
      */
     public SocketFactory getSocketFactory(boolean secure) {
         return secure ? getSecureSocketFactory() : getRegularSocketFactory();
     }
 
     /**
      * Returns the SSL keystore type.
      * 
      * @return The SSL keystore type.
      */
     public String getSslProtocol() {
         return getHelpedParameters().getFirstValue("sslProtocol", "TLS");
     }
 
     /**
      * Indicates if the protocol will use Nagle's algorithm
      * 
      * @return True to enable TCP_NODELAY, false to disable.
      * @see java.net.Socket#setTcpNoDelay(boolean)
      */
     public boolean getTcpNoDelay() {
         return Boolean.parseBoolean(getHelpedParameters().getFirstValue(
                 "tcpNoDelay", "false"));
     }
 
     /**
      * Returns the SSL trust store password.
      * 
      * @return The SSL trust store password.
      */
     public String getTruststorePassword() {
         return getHelpedParameters().getFirstValue("truststorePassword",
                 System.getProperty("javax.net.ssl.keyStorePassword"));
     }
 
     /**
      * Returns the SSL trust store path.
      * 
      * @return The SSL trust store path.
      */
     public String getTruststorePath() {
         return getHelpedParameters().getFirstValue("truststorePath", null);
     }
 
     /**
      * Returns the SSL trust store type.
      * 
      * @return The SSL trust store type.
      */
     public String getTruststoreType() {
         return getHelpedParameters().getFirstValue("truststoreType",
                 System.getProperty("javax.net.ssl.trustStoreType"));
     }
 
     @Override
     public void handle(Request request, Response response) {
         try {
             if (request.getOnResponse() == null) {
                 // Synchronous mode
                 CountDownLatch latch = new CountDownLatch(1);
                 request.getAttributes().put(
                         "org.restlet.engine.http.connector.latch", latch);
 
                 // Add the message to the outbound queue for processing
                 getOutboundMessages().add(response);
 
                 // Await on the latch
                 latch.await();
             } else {
                 // Add the message to the outbound queue for processing
                 getOutboundMessages().add(response);
             }
         } catch (Exception e) {
             getLogger().log(
                     Level.INFO,
                     "Error while handling a " + request.getProtocol().getName()
                             + " client request", e);
             response.setStatus(Status.CONNECTOR_ERROR_INTERNAL, e);
         }
     }
 
     @Override
     public void handleInbound(Response response) {
         if (response != null) {
             if (response.getRequest().getOnResponse() != null) {
                 response.getRequest().getOnResponse().handle(
                         response.getRequest(), response);
             }
 
             if (!response.getStatus().isInformational()) {
                 CountDownLatch latch = (CountDownLatch) response.getRequest()
                         .getAttributes().get(
                                 "org.restlet.engine.http.connector.latch");
                 if (latch != null) {
                     latch.countDown();
                 }
             }
         }
     }
 
     @Override
     public void handleOutbound(Response response) {
         if ((response != null) && (response.getRequest() != null)) {
             Request request = response.getRequest();
 
             // Resolve relative references
             Reference resourceRef = request.getResourceRef().isRelative() ? request
                     .getResourceRef().getTargetRef()
                     : request.getResourceRef();
 
             // Extract the host info
             String hostDomain = resourceRef.getHostDomain();
             int hostPort = resourceRef.getHostPort();
 
             if (hostPort == -1) {
                 if (resourceRef.getSchemeProtocol() != null) {
                     hostPort = resourceRef.getSchemeProtocol().getDefaultPort();
                 } else {
                     hostPort = getProtocols().get(0).getDefaultPort();
                 }
             }
 
             try {
                 // Try to reuse an existing connection for the same host and
                 // port
                 InetSocketAddress socketAddress = new InetSocketAddress(
                         hostDomain, hostPort);
                if (socketAddress.getAddress() == null) {
                    throw new UnknownHostException(hostDomain);
                }
                
                 int hostConnectionCount = 0;
                 int bestCount = 0;
                 Connection<Client> bestConn = null;
                 boolean foundConn = false;
 
                 for (Connection<Client> currConn : getConnections()) {
                     if (socketAddress.getAddress().equals(
                             currConn.getSocket().getInetAddress())
                             && socketAddress.getPort() == currConn.getSocket()
                                     .getPort()) {
                         hostConnectionCount++;
 
                         if (currConn.getState().equals(ConnectionState.OPEN)
                                 && !currConn.isOutboundBusy()) {
                             bestConn = currConn;
                             foundConn = true;
                             continue;
                         }
 
                         int currCount = currConn.getOutboundMessages().size();
 
                         if (bestCount > currCount) {
                             bestCount = currCount;
                             bestConn = currConn;
                         }
                     }
                 }
 
                 if (!foundConn
                         && ((getMaxTotalConnections() == -1) || (getConnections()
                                 .size() < getMaxTotalConnections()))
                         && ((getMaxConnectionsPerHost() == -1) || (hostConnectionCount < getMaxConnectionsPerHost()))) {
                     // Create a new connection
                     bestConn = createConnection(this, createSocket(request
                             .isConfidential(), hostDomain, hostPort), null);
                     bestConn.open();
                     bestCount = 0;
                 }
 
                 if (bestConn != null) {
                     bestConn.getOutboundMessages().add(response);
                     getConnections().add(bestConn);
 
                     if (!request.isExpectingResponse()) {
                         // Attempt to directly write the response, preventing a
                         // thread context switch
                         bestConn.writeMessages();
                         // Unblock the possibly waiting thread.
                         // NB : the request may not be written at this time.
                         CountDownLatch latch = (CountDownLatch) response
                                 .getRequest().getAttributes()
                                 .get("org.restlet.engine.http.connector.latch");
                         if (latch != null) {
                             latch.countDown();
                         }
                     }
                 } else {
                     getLogger().warning(
                             "Unable to find a connection to send the request");
                 }
             } catch (IOException ioe) {
                 getLogger()
                         .log(
                                 Level.FINE,
                                 "An error occured during the communication with the remote server.",
                                 ioe);
                 response.setStatus(Status.CONNECTOR_ERROR_COMMUNICATION, ioe);
                 // Unblock the possibly waiting thread.
                 // NB : the request may not be written at this time.
                 CountDownLatch latch = (CountDownLatch) response.getRequest()
                         .getAttributes().get(
                                 "org.restlet.engine.http.connector.latch");
                 if (latch != null) {
                     latch.countDown();
                 }
             }
         }
     }
 
     /**
      * Sets the regular socket factory.
      * 
      * @param regularSocketFactory
      *            The regular socket factory.
      */
     public void setRegularSocketFactory(SocketFactory regularSocketFactory) {
         this.regularSocketFactory = regularSocketFactory;
     }
 
     /**
      * Sets the secure socket factory.
      * 
      * @param secureSocketFactory
      *            The secure socket factory.
      */
     public void setSecureSocketFactory(SocketFactory secureSocketFactory) {
         this.secureSocketFactory = secureSocketFactory;
     }
 
     @Override
     public synchronized void start() throws Exception {
         setRegularSocketFactory(createSocketFactory(false));
         setSecureSocketFactory(createSocketFactory(true));
         super.start();
     }
 
     @Override
     public synchronized void stop() throws Exception {
         setRegularSocketFactory(null);
         setSecureSocketFactory(null);
         super.stop();
     }
 
 }
