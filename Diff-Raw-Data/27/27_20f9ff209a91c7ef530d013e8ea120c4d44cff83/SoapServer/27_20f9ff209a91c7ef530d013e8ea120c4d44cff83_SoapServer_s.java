 package com.centeractive.ws.server.core;
 
 import com.centeractive.ws.server.ServiceRegistrationException;
 import com.centeractive.ws.server.endpoint.GenericContextDomEndpoint;
 import com.centeractive.ws.server.responder.RequestResponder;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.mortbay.jetty.AbstractConnector;
 import org.mortbay.jetty.Handler;
 import org.mortbay.jetty.Server;
 import org.mortbay.jetty.nio.SelectChannelConnector;
 import org.mortbay.jetty.security.SslSelectChannelConnector;
 import org.mortbay.jetty.servlet.Context;
 import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
 import org.springframework.context.support.ClassPathXmlApplicationContext;
 import org.springframework.web.context.WebApplicationContext;
 import org.springframework.web.context.support.GenericWebApplicationContext;
 
 import javax.servlet.ServletContext;
 import java.net.URL;
 import java.util.Collections;
 import java.util.List;
 import java.util.Properties;
 
 import static com.google.common.base.Preconditions.checkNotNull;
 
 /**
  * Copyright (c) centeractive ag, Inc. All Rights Reserved.
  *
  * User: Tom Bujok (tomasz.bujok@centeractive.com)
  * Date: 19/11/11
  * Time: 4:41 PM
  */
 public final class SoapServer {
 
     private final static Log log = LogFactory.getLog(SoapServer.class);
 
     public static final int HTTP_PORT = 8080;
     public static final int HTTPS_PORT = 8443;
     public static final int CONNECTION_MAX_IDLE_TIME_IN_SECONDS = 60;
     public static final int ACCEPTOR_THREADS_COUNT = 4;
     public static final int CORE_THREADS_COUNT = 8;
     public static final int MAX_THREADS_COUNT = 16;
     public static final int THREAD_KEEP_ALIVE_TIME_IN_SECONDS = 60;
     public static final String KEYSTORE_TYPE = "JKS";
     public static final boolean REUSE_ADDRESS = true;
 
     private Integer httpPort = HTTP_PORT;
     private Integer httpsPort = HTTPS_PORT;
     private boolean reuseAddress = REUSE_ADDRESS;
     private Integer connectionMaxIdleTimeInSeconds = CONNECTION_MAX_IDLE_TIME_IN_SECONDS;
     private Integer acceptorThreads = ACCEPTOR_THREADS_COUNT;
     private Integer coreThreads = CORE_THREADS_COUNT;
     private Integer maxThreads = MAX_THREADS_COUNT;
     private Integer threadKeepAliveTimeInSeconds = THREAD_KEEP_ALIVE_TIME_IN_SECONDS;
 
     private boolean http;
     private boolean https;
     private String keyStorePath;
     private String keyStoreType = KEYSTORE_TYPE;
     private String keyStorePassword;
 
     private ClassPathXmlApplicationContext context;
     private Server server;
     private GenericContextDomEndpoint endpoint;
 
     // ----------------------------------------------------------------
     // PUBLIC API
     // ----------------------------------------------------------------
     public void start() {
         try {
             server.start();
         } catch (Exception ex) {
             throw new RuntimeException(ex);
         }
     }
 
     public void stop() {
         try {
             server.stop();
         } catch (Exception ex) {
             throw new RuntimeException(ex);
         }
     }
 
     public void destroy() {
         stop();
         context.close();
     }
 
     public void registerRequestResponder(String contextPath, RequestResponder responder) throws ServiceRegistrationException {
         checkNotNull(contextPath, "contextPath cannot be null");
         checkNotNull(responder, "responder cannot be null");
         endpoint.registerRequestResponder(contextPath, responder);
     }
 
     public void unregisterRequestResponder(String contextPath) throws ServiceRegistrationException {
         checkNotNull(contextPath, "contextPath cannot be null");
         endpoint.unregisterRequestResponder(contextPath);
     }
 
     public List<String> getRegisteredContextPaths() {
         return Collections.list(endpoint.getRegisteredContextPaths());
     }
 
 
     // ----------------------------------------------------------------
     // INTERNAL API
     // ----------------------------------------------------------------
     GenericContextDomEndpoint getEndpoint() {
         return this.endpoint;
     }
 
     private void configure() {
         configureParentContext();
         configureConnectors();
         configureWebContext();
     }
 
     private void configureParentContext() {
         PropertyPlaceholderConfigurer config = new PropertyPlaceholderConfigurer();
         config.setProperties(buildProperties());
         context = new ClassPathXmlApplicationContext();
         context.addBeanFactoryPostProcessor(config);
         context.setConfigLocation("classpath:soap-server.xml");
         context.refresh();
         context.registerShutdownHook();
         server = context.getBean("jettyServer", Server.class);
     }
 
     private void configureConnectors() {
         if (http) {
             SelectChannelConnector httpConnector = context.getBean("connector", SelectChannelConnector.class);
             configureHttpConnector(httpConnector);
             server.addConnector(httpConnector);
         }
         if (https) {
             checkNotNull(keyStorePath, "keyStore has to be set in https mode");
             checkNotNull(keyStorePassword, "keyStorePassword has to be set in https mode");
             SslSelectChannelConnector httpsConnector = context.getBean("sslConnector", SslSelectChannelConnector.class);
             configureHttpsConnector(httpsConnector);
             server.addConnector(httpsConnector);
         }
     }
 
     private SelectChannelConnector configureHttpConnector(SelectChannelConnector connector) {
         configureGenericConnector(connector);
         connector.setReuseAddress(reuseAddress);
         connector.setPort(httpPort);
         return connector;
     }
 
     private SslSelectChannelConnector configureHttpsConnector(SslSelectChannelConnector connector) {
         configureGenericConnector(connector);
         connector.setReuseAddress(reuseAddress);
         connector.setPort(httpsPort);
         connector.setKeystore(keyStorePath);
         connector.setKeystoreType(keyStoreType);
         connector.setKeyPassword(keyStorePassword);
         return connector;
     }
 
     private AbstractConnector configureGenericConnector(AbstractConnector connector) {
         connector.setAcceptors(acceptorThreads);
         connector.setMaxIdleTime(connectionMaxIdleTimeInSeconds * 1000);
         return connector;
     }
 
     private void configureWebContext() {
         ServletContext servletContext = getServletContext();
         GenericWebApplicationContext webContext = new GenericWebApplicationContext();
         webContext.setServletContext(servletContext);
         webContext.setParent(context);
         webContext.refresh();
         servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, webContext);
         if (webContext != null) {
             endpoint = webContext.getBean("endpoint", GenericContextDomEndpoint.class);
         }
     }
 
     private ServletContext getServletContext() {
         for (Handler handler : server.getHandlers()) {
             if (handler instanceof Context) {
                 return ((Context) handler).getServletContext();
             }
         }
         return null;
     }
 
     private Properties buildProperties() {
         Properties properties = new Properties();
         properties.setProperty("core.pool.size", coreThreads.toString());
         properties.setProperty("max.pool.size", maxThreads.toString());
         properties.setProperty("keep.alive.time", threadKeepAliveTimeInSeconds.toString());
         return properties;
     }
 
     // ----------------------------------------------------------------
     // BUILDER API
     // ----------------------------------------------------------------
     private SoapServer() {
     }
 
     public static SoapServerBuilder builder() {
         return new SoapServerBuilder();
     }
 
     public static class SoapServerBuilder {
         private final SoapServer server;
 
         public SoapServerBuilder() {
             server = new SoapServer();
         }
 
         public SoapServerBuilder reuseAddress(boolean value) {
             server.reuseAddress = value;
             return this;
         }
 
         public SoapServerBuilder httpPort(int value) {
             server.http = true;
             server.httpPort = value;
             return this;
         }
 
         public SoapServerBuilder httpsPort(int value) {
             server.https = true;
             server.httpsPort = value;
             return this;
         }
 
         public SoapServerBuilder connectionMaxIdleTimeInSeconds(int value) {
             server.connectionMaxIdleTimeInSeconds = value;
             return this;
         }
 
         public SoapServerBuilder acceptorThreads(int value) {
             server.acceptorThreads = value;
             return this;
         }
 
         public SoapServerBuilder coreThreads(int value) {
             server.coreThreads = value;
             return this;
         }
 
         public SoapServerBuilder maxThreads(int value) {
             server.maxThreads = value;
             return this;
         }
 
         public SoapServerBuilder threadKeepAliveTimeInSeconds(int value) {
             server.threadKeepAliveTimeInSeconds = value;
             return this;
         }
 
         public SoapServer create() {
             server.configure();
             return server;
         }
 
        @Deprecated
         public SoapServerBuilder keyStoreUrl(URL value) {
            server.keyStorePath = value.toString();
            return this;
         }
 
         public SoapServerBuilder keyStorePath(String path) {
             server.keyStorePath = path;
             return this;
         }
 
         public SoapServerBuilder keyStoreType(String value) {
             server.keyStoreType = value;
             return this;
         }
 
         public SoapServerBuilder keyStorePassword(String value) {
             server.keyStorePassword = value;
             return this;
         }
     }
 }
