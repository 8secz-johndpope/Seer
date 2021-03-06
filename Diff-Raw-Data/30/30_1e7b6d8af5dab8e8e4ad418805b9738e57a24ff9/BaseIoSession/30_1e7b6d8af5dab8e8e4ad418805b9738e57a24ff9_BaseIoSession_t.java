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
 package org.apache.mina.common.support;
 
 import java.net.SocketAddress;
 import java.util.HashSet;
 import java.util.Set;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
 
import org.apache.mina.common.ByteBuffer;
 import org.apache.mina.common.CloseFuture;
 import org.apache.mina.common.DefaultWriteRequest;
 import org.apache.mina.common.IdleStatus;
 import org.apache.mina.common.IoAcceptor;
 import org.apache.mina.common.IoService;
 import org.apache.mina.common.IoSession;
 import org.apache.mina.common.TrafficMask;
 import org.apache.mina.common.WriteFuture;
 import org.apache.mina.common.WriteRequest;
 
 /**
  * Base implementation of {@link IoSession}.
  * 
  * @author The Apache MINA Project (dev@mina.apache.org)
  * @version $Rev$, $Date$
  */
 public abstract class BaseIoSession implements IoSession
 {
     private final Object lock = new Object();
     private final ConcurrentMap<String,Object> attributes = new ConcurrentHashMap<String, Object>( );
     private final long creationTime;
 
     /** 
      * A future that will be set 'closed' when the connection is closed.
      */
     private final CloseFuture closeFuture = new DefaultCloseFuture( this );
     private boolean closing;
 
     // Configuration variables
     private int idleTimeForRead;
     private int idleTimeForWrite;
     private int idleTimeForBoth;
     private int writeTimeout;
     private TrafficMask trafficMask = TrafficMask.ALL; 
     
     // Status variables
     private long readBytes;
     private long writtenBytes;
     
     private long readMessages;
     private long writtenMessages;
     
     private long lastReadTime;
     private long lastWriteTime;
 
     private int idleCountForBoth;
     private int idleCountForRead;
     private int idleCountForWrite;
     
     private long lastIdleTimeForBoth;
     private long lastIdleTimeForRead;
     private long lastIdleTimeForWrite;
 
 
     protected BaseIoSession()
     {
         creationTime = lastReadTime = lastWriteTime =
             lastIdleTimeForBoth = lastIdleTimeForRead = lastIdleTimeForWrite =
                 System.currentTimeMillis();
     }
     
     public boolean isConnected()
     {
         return !closeFuture.isClosed();
     }
     
     public boolean isClosing()
     {
         synchronized( lock )
         {
             return closing || closeFuture.isClosed();
         }
     }
     
     public CloseFuture getCloseFuture()
     {
         return closeFuture;
     }
     
     public CloseFuture close()
     {
         synchronized( lock )
         {
             if( isClosing() )
             {
                 return closeFuture;
             }
             else
             {
                 closing = true;
             }
         }
 
         close0();
         return closeFuture;
     }
 
     /**
      * Implement this method to perform real close operation.
      * By default, this method is implemented to set the future to
      * 'closed' immediately.
      */
     protected void close0()
     {
         closeFuture.setClosed();
     }
     
     public WriteFuture write( Object message )
     {
         return write( message, null );
     }
     
     public WriteFuture write( Object message, SocketAddress remoteAddress )
     {
        if (message == null) {
            throw new NullPointerException("message");
        }
        
        if (message instanceof ByteBuffer && !((ByteBuffer) message).hasRemaining()) {
            throw new IllegalArgumentException("message is empty. Forgot to call flip()?");
        }

         synchronized( lock )
         {
             if( isClosing() || !isConnected() )
             {
                 return DefaultWriteFuture.newNotWrittenFuture( this );
             }
         }
 
         WriteFuture future = new DefaultWriteFuture( this );
         write0( new DefaultWriteRequest( message, future, remoteAddress ) );
         
         return future;
     }
     
     /**
      * Implement this method to perform real write operation with
      * the specified <code>writeRequest</code>.
      * 
      * By default, this method is implemented to set the future to
      * 'not written' immediately.
      */
     protected void write0( WriteRequest writeRequest )
     {
         writeRequest.getFuture().setWritten( false );
     }
 
     public Object getAttachment()
     {
         return getAttribute( "" );
     }
 
     public Object setAttachment( Object attachment )
     {
         return setAttribute( "", attachment );
     }
 
     public Object getAttribute( String key )
     {
         if (key == null) {
             throw new NullPointerException("key");
         }
         
         return attributes.get( key );
     }
 
     public Object setAttribute( String key, Object value )
     {
         if (key == null) {
             throw new NullPointerException("key");
         }
         
         if( value == null )
         {
             return attributes.remove( key );
         }
         else
         {
             return attributes.put( key, value );
         }
     }
     
     public Object setAttribute( String key )
     {
         return setAttribute( key, Boolean.TRUE );
     }
     
     public Object setAttributeIfAbsent( String key, Object value )
     {
         if (key == null) {
             throw new NullPointerException("key");
         }
         
         if (value == null) {
             return null;
         }
 
         return attributes.putIfAbsent( key, value );
     }
     
     public Object removeAttribute( String key )
     {
         if (key == null) {
             throw new NullPointerException("key");
         }
         
         return attributes.remove( key );
     }
     
     public boolean removeAttribute(String key, Object value) {
         if (key == null) {
             throw new NullPointerException("key");
         }
         
         if (value == null) {
             return false;
         }
 
         return attributes.remove(key, value);
     }
     
     public boolean replaceAttribute(String key, Object oldValue, Object newValue) {
         return attributes.replace(key, oldValue, newValue);
     }
     
     public boolean containsAttribute( String key )
     {
         return getAttribute( key ) != null;
     }
 
     public Set<String> getAttributeKeys()
     {
         return new HashSet<String>( attributes.keySet() );
     }
     
     public int getIdleTime( IdleStatus status )
     {
         if( status == IdleStatus.BOTH_IDLE ) {
             return idleTimeForBoth;
         }
 
         if( status == IdleStatus.READER_IDLE ) {
             return idleTimeForRead;
         }
 
         if( status == IdleStatus.WRITER_IDLE ) {
             return idleTimeForWrite;
         }
 
         throw new IllegalArgumentException( "Unknown idle status: " + status );
     }
 
     public long getIdleTimeInMillis( IdleStatus status )
     {
         return getIdleTime( status ) * 1000L;
     }
 
     public void setIdleTime( IdleStatus status, int idleTime )
     {
         if( idleTime < 0 ) {
             throw new IllegalArgumentException( "Illegal idle time: "
                                                 + idleTime );
         }
 
         if( status == IdleStatus.BOTH_IDLE ) {
             idleTimeForBoth = idleTime;
         } else if( status == IdleStatus.READER_IDLE ) {
             idleTimeForRead = idleTime;
         } else if( status == IdleStatus.WRITER_IDLE ) {
             idleTimeForWrite = idleTime;
         } else {
             throw new IllegalArgumentException( "Unknown idle status: "
                                                 + status );
         }
     }
 
     public int getWriteTimeout()
     {
         return writeTimeout;
     }
 
     public long getWriteTimeoutInMillis()
     {
         return writeTimeout * 1000L;
     }
 
     public void setWriteTimeout( int writeTimeout )
     {
         if( writeTimeout < 0 ) {
             throw new IllegalArgumentException( "Illegal write timeout: "
                                                 + writeTimeout );
         }
         this.writeTimeout = writeTimeout;
     }
 
     public TrafficMask getTrafficMask()
     {
         return trafficMask;
     }
     
     public void setTrafficMask( TrafficMask trafficMask )
     {
         if( trafficMask == null )
         {
             throw new NullPointerException( "trafficMask" );
         }
         
         if( this.trafficMask == trafficMask )
         {
             return;
         }
         
         this.trafficMask = trafficMask;
         updateTrafficMask();
     }
     
     public void suspendRead()
     {
         setTrafficMask( getTrafficMask().and( TrafficMask.READ.not() ) );
     }
 
     public void suspendWrite()
     {
         setTrafficMask( getTrafficMask().and( TrafficMask.WRITE.not() ) );
     }
 
     public void resumeRead()
     {
         setTrafficMask( getTrafficMask().or( TrafficMask.READ ) );
     }
 
     public void resumeWrite()
     {
         setTrafficMask( getTrafficMask().or( TrafficMask.WRITE ) );
     }
 
     /**
      * Signals the {@link IoService} that the {@link TrafficMask} of this
      * session has been changed.
      */
     protected abstract void updateTrafficMask();
     
     public long getReadBytes()
     {
         return readBytes;
     }
 
     public long getWrittenBytes()
     {
         return writtenBytes;
     }
     
     public long getReadMessages()
     {
         return readMessages;
     }
         
     public long getWrittenMessages()
     {
         return writtenMessages;        
     }
     
     public void increaseReadBytes( int increment )
     {
         readBytes += increment;
         lastReadTime = System.currentTimeMillis();
         idleCountForBoth = 0;
         idleCountForRead = 0;
     }
 
     public void increaseWrittenBytes( int increment )
     {
         writtenBytes += increment;
         lastWriteTime = System.currentTimeMillis();
         idleCountForBoth = 0;
         idleCountForWrite = 0;
     }
     
     public void increaseReadMessages()
     {
         readMessages++;
     }
     
     public void increaseWrittenMessages()
     {
         writtenMessages++;
     }
     
     public long getCreationTime()
     {
         return creationTime;
     }
 
     public long getLastIoTime()
     {
         return Math.max( lastReadTime, lastWriteTime );
     }
 
     public long getLastReadTime()
     {
         return lastReadTime;
     }
 
     public long getLastWriteTime()
     {
         return lastWriteTime;
     }
 
     public boolean isIdle( IdleStatus status )
     {
         if( status == IdleStatus.BOTH_IDLE ) {
             return idleCountForBoth > 0;
         }
 
         if( status == IdleStatus.READER_IDLE ) {
             return idleCountForRead > 0;
         }
 
         if( status == IdleStatus.WRITER_IDLE ) {
             return idleCountForWrite > 0;
         }
 
         throw new IllegalArgumentException( "Unknown idle status: " + status );
     }
 
     public int getIdleCount( IdleStatus status )
     {
         if( status == IdleStatus.BOTH_IDLE ) {
             return idleCountForBoth;
         }
 
         if( status == IdleStatus.READER_IDLE ) {
             return idleCountForRead;
         }
 
         if( status == IdleStatus.WRITER_IDLE ) {
             return idleCountForWrite;
         }
 
         throw new IllegalArgumentException( "Unknown idle status: " + status );
     }
     
     public long getLastIdleTime( IdleStatus status )
     {
         if( status == IdleStatus.BOTH_IDLE ) {
             return lastIdleTimeForBoth;
         }
 
         if( status == IdleStatus.READER_IDLE ) {
             return lastIdleTimeForRead;
         }
 
         if( status == IdleStatus.WRITER_IDLE ) {
             return lastIdleTimeForWrite;
         }
 
         throw new IllegalArgumentException( "Unknown idle status: " + status );
     }
 
     public void increaseIdleCount( IdleStatus status )
     {
         if( status == IdleStatus.BOTH_IDLE )
         {
             idleCountForBoth ++;
             lastIdleTimeForBoth = System.currentTimeMillis();
         }
         else if( status == IdleStatus.READER_IDLE )
         {
             idleCountForRead ++;
             lastIdleTimeForRead = System.currentTimeMillis();
         }
         else if( status == IdleStatus.WRITER_IDLE )
         {
             idleCountForWrite ++;
             lastIdleTimeForWrite = System.currentTimeMillis();
         } else {
             throw new IllegalArgumentException( "Unknown idle status: "
                                                 + status );
         }
     }
     
     public SocketAddress getServiceAddress()
     {
         IoService service = getService();
         if( service instanceof IoAcceptor )
         {
             return ( ( IoAcceptor ) service ).getLocalAddress();
         }
         else
         {
             return getRemoteAddress();
         }
     }
     
     @Override
     public String toString()
     {
         return "(" + getTransportType() +
                ", R: " + getRemoteAddress() +
                ", L: " + getLocalAddress() +
                ", S: " + getServiceAddress() +
                ')';
     }
 }
