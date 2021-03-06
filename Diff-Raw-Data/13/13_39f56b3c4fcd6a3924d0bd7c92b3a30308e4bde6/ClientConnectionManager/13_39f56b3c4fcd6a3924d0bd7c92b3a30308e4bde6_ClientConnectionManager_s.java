 /*
  *  Licensed to the Apache Software Foundation (ASF) under one
  *  or more contributor license agreements.  See the NOTICE file
  *  distributed with this work for additional information
  *  regarding copyright ownership.  The ASF licenses this file
  *  to you under the Apache License, Version 2.0 (the
  *  "License"); you may not use this file except in compliance
  *  with the License.  You may obtain a copy of the License at
  *  
  *    http://www.apache.org/licenses/LICENSE-2.0
  *  
  *  Unless required by applicable law or agreed to in writing,
  *  software distributed under the License is distributed on an
  *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  *  KIND, either express or implied.  See the License for the
  *  specific language governing permissions and limitations
  *  under the License. 
  *  
  */
 package org.apache.directory.mitosis.service;
 
 
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.LinkedBlockingQueue;
 import java.util.concurrent.ThreadPoolExecutor;
 import java.util.concurrent.TimeUnit;
 
 import org.apache.directory.mitosis.common.Replica;
 import org.apache.directory.mitosis.common.ReplicaId;
 import org.apache.directory.mitosis.configuration.ReplicationConfiguration;
 import org.apache.directory.mitosis.service.protocol.codec.ReplicationClientProtocolCodecFactory;
 import org.apache.directory.mitosis.service.protocol.handler.ReplicationClientContextHandler;
 import org.apache.directory.mitosis.service.protocol.handler.ReplicationClientProtocolHandler;
 import org.apache.directory.mitosis.service.protocol.handler.ReplicationProtocolHandler;
 import org.apache.mina.common.ConnectFuture;
 import org.apache.mina.common.ExecutorThreadModel;
 import org.apache.mina.common.IoConnector;
 import org.apache.mina.common.IoConnectorConfig;
 import org.apache.mina.common.IoSession;
 import org.apache.mina.common.RuntimeIOException;
 import org.apache.mina.filter.LoggingFilter;
 import org.apache.mina.filter.codec.ProtocolCodecFilter;
 import org.apache.mina.transport.socket.nio.SocketConnector;
 import org.apache.mina.transport.socket.nio.SocketConnectorConfig;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * Manages all outgoing connections to remote replicas.
  * It gets the list of the peer {@link Replica}s from
  * {@link ReplicationInterceptor} and keeps trying to connect to them.
  * <p>
  * When the connection attempt fails, the interval between each connection
  * attempt doubles up (0, 2, 4, 8, 16, ...) to 60 seconds at maximum.
  * <p>
  * Once the connection attempt succeeds, the interval value is reset to
  * its initial value (0 second) and the established connection is handled
  * by {@link ReplicationClientProtocolHandler}.
  * The {@link ReplicationClientProtocolHandler} actually wraps
  * a {@link ReplicationClientContextHandler} that drives the actual
  * replication process.
  *
  * @author The Apache Directory Project (dev@directory.apache.org)
  * @version $Rev: 116 $, $Date: 2006-09-18 13:47:53Z $
  */
 class ClientConnectionManager
 {
     private static final Logger LOG = LoggerFactory.getLogger( ClientConnectionManager.class );
 
     private final ReplicationInterceptor interceptor;
     private final IoConnector connector = new SocketConnector();
     private final IoConnectorConfig connectorConfig = new SocketConnectorConfig();
     private final Map<ReplicaId,Connection> sessions = new HashMap<ReplicaId,Connection>();
     private ReplicationConfiguration configuration;
     private ConnectionMonitor monitor;
 
 
     ClientConnectionManager( ReplicationInterceptor interceptor )
     {
         this.interceptor = interceptor;
 
         ExecutorThreadModel threadModel = ExecutorThreadModel.getInstance( "mitosis" );
         threadModel.setExecutor( new ThreadPoolExecutor( 16, 16, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>() ) );
         connectorConfig.setThreadModel( threadModel );
 
         //// add codec
         connectorConfig.getFilterChain().addLast( "protocol",
             new ProtocolCodecFilter( new ReplicationClientProtocolCodecFactory() ) );
 
         //// add logger
         connectorConfig.getFilterChain().addLast( "logger", new LoggingFilter() );
     }
 
 
     public void start( ReplicationConfiguration cfg ) throws Exception
     {
         this.configuration = cfg;
         monitor = new ConnectionMonitor();
         monitor.start();
     }
 
 
     public void stop() throws Exception
     {
         // close all connections
         monitor.shutdown();
 
         // remove all filters
         connector.getFilterChain().clear();
 
         ( ( ExecutorService ) ( ( ExecutorThreadModel ) connectorConfig.getThreadModel() ).getExecutor() ).shutdown();
         
         // Remove all status values.
         sessions.clear();
     }
 
 
     public void replicate()
     {
         // FIXME Can get ConcurrentModificationException.
         for ( Connection connection : sessions.values() )
         {
             synchronized ( connection )
             {
                 // Begin replication for the connected replicas.
                 if ( connection.session != null )
                 {
                     ( ( ReplicationProtocolHandler ) connection.session.getHandler() )
                             .getContext( connection.session ).replicate();
                 }
             }
         }
     }
 
 
     /**
      * Interrupt the unconnected connections to make them attempt to connect immediately.
      *
      */
     public void interruptConnectors()
     {
         for ( Connection connection : sessions.values() )
         {
             synchronized ( connection )
             {
                 // Wake up the replicas that are sleeping.
                 if ( connection.inProgress && connection.connector != null )
                 {
                     connection.connector.interrupt();
                 }
             }
         }
         
     }
 
 
     private class ConnectionMonitor extends Thread
     {
         private boolean timeToShutdown;
 
 
         public ConnectionMonitor()
         {
             super( "ClientConnectionManager" );
             
             // Initialize the status map.
             for ( Replica replica : configuration.getPeerReplicas() )
             {
                 Connection con = sessions.get( replica.getId() );
                 if ( con == null )
                 {
                     con = new Connection();
                     con.replicaId = replica.getId();
                     sessions.put( replica.getId(), con );
                 }
             }
         }
 
 
         public void shutdown()
         {
             timeToShutdown = true;
             while ( isAlive() )
             {
                 try
                 {
                     join();
                 }
                 catch ( InterruptedException e )
                 {
                     LOG.warn( "[Replica-{}] Unexpected exception.", configuration.getReplicaId(), e );
                 }
             }
         }
 
 
         public void run()
         {
             while ( !timeToShutdown )
             {
                 connectUnconnected();
                 try
                 {
                     Thread.sleep( 1000 );
                 }
                 catch ( InterruptedException e )
                 {
                     LOG.warn( "[Replica-{}] Unexpected exception.", configuration.getReplicaId(), e );
                 }
             }
 
             disconnectConnected();
         }
 
 
         private void connectUnconnected()
         {
             for ( Replica replica : configuration.getPeerReplicas() )
             {
                 // Someone might have modified the configuration,
                 // and therefore we try to detect newly added replicas.
                 Connection con = sessions.get( replica.getId() );
                 if ( con == null )
                 {
                     con = new Connection();
                     con.replicaId = replica.getId();
                     sessions.put( replica.getId(), con );
                 }
 
                 synchronized ( con )
                 {
                     if ( con.inProgress )
                     {
                         // connection is in progress
                         continue;
                     }
 
                     if ( con.session != null )
                     {
                         if ( con.session.isConnected() )
                         {
                             continue;
                         }
                         con.session = null;
                     }
 
                     // put to connectingSession with dummy value to mark
                     // that connection is in progress
                     con.inProgress = true;
 
                     if ( con.delay < 0 )
                     {
                         con.delay = 0;
                     }
                     else if ( con.delay == 0 )
                     {
                         con.delay = 2;
                     }
                     else
                     {
                         con.delay *= 2;
                         if ( con.delay > 60 )
                         {
                             con.delay = 60;
                         }
                     }
                 }
 
                 Connector connector = new Connector( replica, con );
                 synchronized ( con )
                 {
                     con.connector = connector;
                 }
                 connector.start();
             }
         }
 
 
         private void disconnectConnected()
         {
             LOG.info( "[Replica-{}] Closing all connections...", configuration.getReplicaId() );
             for ( ;; )
             {
                 Iterator i = sessions.values().iterator();
                 while ( i.hasNext() )
                 {
                     Connection con = ( Connection ) i.next();
                     synchronized ( con )
                     {
                         if ( con.inProgress )
                         {
                             if ( con.connector != null )
                             {
                                 con.connector.interrupt();
                             }
                             continue;
                         }
 
                         i.remove();
 
                         if ( con.session != null )
                         {
                             LOG.info( "[Replica-{}] Closed connection to Replica-{}", configuration.getReplicaId(),
                                     con.replicaId );
                             con.session.close();
                         }
                     }
                 }
 
                 if ( sessions.isEmpty() )
                 {
                     break;
                 }
 
                 // Sleep 1 second and try again waiting for Connector threads.
                 try
                 {
                     Thread.sleep( 1000 );
                 }
                 catch ( InterruptedException e )
                 {
                     LOG.warn( "[Replica-{}] Unexpected exception.", configuration.getReplicaId(), e );
                 }
             }
         }
     }
 
 
     private class Connector extends Thread
     {
         private final Replica replica;
         private final Connection con;
 
 
         public Connector( Replica replica, Connection con )
         {
             super( "ClientConnectionManager-" + replica );
             this.replica = replica;
             this.con = con;
         }
 
 
         public void run()
         {
             if ( con.delay > 0 )
             {
                 if ( LOG.isInfoEnabled() )
                 {
                     LOG.info( "[Replica-{}] Waiting for {} seconds to reconnect to replica-" + con.replicaId,
                             ClientConnectionManager.this.configuration.getReplicaId(), con.delay );
                 }
                 
                 try
                 {
                     Thread.sleep( con.delay * 1000L );
                 }
                 catch ( InterruptedException e )
                 {
                     e.printStackTrace();
                 }
             }
 
             LOG.info( "[Replica-{}] Connecting to replica-{}",
                     ClientConnectionManager.this.configuration.getReplicaId(), replica.getId() );
 
             IoSession session;
             try
             {
                 connectorConfig.setConnectTimeout( configuration.getResponseTimeout() );
                 ConnectFuture future = connector.connect( replica.getAddress(), new ReplicationClientProtocolHandler(
                         interceptor ), connectorConfig );
 
                 future.join();
                 session = future.getSession();
 
                 synchronized ( con )
                 {
                     con.session = session;
                     con.delay = -1; // reset delay
                     con.inProgress = false;
                     con.replicaId = replica.getId();
                 }
             }
             catch ( RuntimeIOException e )
             {
                LOG.error( "[Replica-{}] Failed to connect to replica-" + replica.getId(),
                        ClientConnectionManager.this.configuration.getReplicaId(), e );
             }
             finally
             {
                 synchronized ( con )
                 {
                     con.inProgress = false;
                     con.connector = null;
                 }
             }
         }
     }
 
     private static class Connection
     {
         private IoSession session;
         private int delay = -1;
         private boolean inProgress;
         private Connector connector;
         private ReplicaId replicaId;
 
 
         public Connection()
         {
         }
     }
 }
