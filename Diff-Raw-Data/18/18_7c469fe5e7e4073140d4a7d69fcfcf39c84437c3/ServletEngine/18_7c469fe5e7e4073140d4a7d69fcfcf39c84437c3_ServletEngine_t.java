 /*******************************************************************************
  * Copyright (c) 2011 EclipseSource and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    EclipseSource - initial API and implementation
  ******************************************************************************/
 package org.eclipse.rap.rwt.cluster.testfixture.server;
 
 import java.io.File;
 import java.io.IOException;
 import java.net.HttpURLConnection;
 import java.net.URL;
 import java.util.Collection;
 import java.util.Map;
 
 import javax.servlet.http.HttpSession;
 
 import org.eclipse.jetty.server.*;
 import org.eclipse.jetty.server.handler.ContextHandlerCollection;
 import org.eclipse.jetty.server.session.*;
 import org.eclipse.jetty.servlet.*;
 import org.eclipse.jetty.util.log.Log;
 import org.eclipse.jetty.util.resource.FileResource;
 import org.eclipse.rap.rwt.cluster.testfixture.internal.util.SocketUtil;
 import org.eclipse.rwt.internal.engine.*;
 
 
 @SuppressWarnings("restriction")
 public class ServletEngine implements IServletEngine {
   private static final String SERVLET_NAME = "/rap";
 
   static {
     Log.setLog( new ServletEngineLogger() );
   }
   
   private final Server server;
   private final ContextHandlerCollection contextHandlers;
   private final SessionManager sessionManager;
 
   public ServletEngine() {
     this( new SessionManagerProvider() );
   }
   
   ServletEngine( ISessionManagerProvider sessionManagerProvider ) {
     this.server = new Server( SocketUtil.getFreePort() );
     this.contextHandlers = new ContextHandlerCollection();
     this.server.setHandler( contextHandlers );
     this.sessionManager = createSessionManager( sessionManagerProvider );
   }
 
   public void start( Class entryPointClass ) throws Exception {
     if( entryPointClass != null ) {
       addEntryPoint( entryPointClass );
     }
     server.start();
   }
 
   public void stop() throws Exception {
     server.stop();
     cleanUp();
   }
 
   public int getPort() {
     return server.getConnectors()[ 0 ].getLocalPort();
   }
   
   public HttpURLConnection createConnection( URL url ) throws IOException {
     return ( HttpURLConnection )url.openConnection();
   }
 
   @SuppressWarnings({ "deprecation", "unchecked" })
   public HttpSession[] getSessions() {
     Map sessionMap = ( ( AbstractSessionManager )sessionManager ).getSessionMap();
     Collection<HttpSession> sessions = sessionMap.values();
     return sessions.toArray( new HttpSession[ sessions.size() ] );
   }
 
   private void addEntryPoint( Class entryPointClass ) {
     ServletContextHandler context = createServletContext( "/" );
     context.addServlet( RWTDelegate.class.getName(), SERVLET_NAME );
     context.addFilter( RWTClusterSupport.class.getName(), SERVLET_NAME, FilterMapping.DEFAULT );
     context.addEventListener( new RWTServletContextListener() );
     context.setInitParameter( "org.eclipse.rwt.entryPoints", entryPointClass.getName() );
   }
 
   private SessionManager createSessionManager( ISessionManagerProvider sessionManagerProvider ) {
     SessionManager result = sessionManagerProvider.createSessionManager( server );
     SessionIdManager sessionIdManager = sessionManagerProvider.createSessionIdManager( server );
     result.setMaxInactiveInterval( 60 * 60 );
     result.setIdManager( sessionIdManager );
     server.setSessionIdManager( sessionIdManager );
     return result;
   }
 
   private ServletContextHandler createServletContext( String path ) {
     SessionHandler sessionHandler = new SessionHandler( sessionManager );
     sessionManager.setSessionHandler( sessionHandler );
     ServletContextHandler result = new ServletContextHandler( contextHandlers, path );
     result.setSessionHandler( sessionHandler );
     result.setBaseResource( createServletContextPath() );
     result.addServlet( DefaultServlet.class.getName(), "/" );
     return result;
   }
 
   private FileResource createServletContextPath() {
     String tempDir = System.getProperty( "java.io.tmpdir" );
     File contextRoot = new File( tempDir, this.toString() + "-context-root" );
     try {
       return new FileResource( contextRoot.toURI().toURL() );
     } catch( Exception e ) {
       throw new RuntimeException( e );
     }
   }
 
   private void cleanUp() throws IOException {
     Handler[] handlers = contextHandlers.getHandlers();
     if( handlers != null ) {
       for( int i = 0; i < handlers.length; i++ ) {
         if( handlers[ i ] instanceof ServletContextHandler ) {
           ServletContextHandler contextHandler = ( ServletContextHandler )handlers[ i ];
           deleteDirectory( contextHandler.getBaseResource().getFile() );
         }
       }
     }
   }
 
   private static void deleteDirectory( File directory ) {
     if( directory.isDirectory() ) {
       File[] files = directory.listFiles();
       for( int i = 0; i < files.length; i++ ) {
         deleteDirectory( files[ i ] );
       }
     }
     directory.delete();
   }
 
   private static class SessionManagerProvider implements ISessionManagerProvider {
 
     public SessionManager createSessionManager( Server server ) {
       HashSessionManager result = new HashSessionManager();
       result.setUsingCookies( true );
       return result;
     }
 
     public SessionIdManager createSessionIdManager( Server server ) {
       return new HashSessionIdManager();
     }
   }
 }
