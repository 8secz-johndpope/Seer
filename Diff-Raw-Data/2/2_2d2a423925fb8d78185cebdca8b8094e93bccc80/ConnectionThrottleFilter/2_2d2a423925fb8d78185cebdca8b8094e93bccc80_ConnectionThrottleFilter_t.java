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
 package org.apache.mina.filter;
 
 import java.net.InetSocketAddress;
 import java.net.SocketAddress;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Map;
 
 import org.apache.mina.common.IoFilter;
 import org.apache.mina.common.IoFilterAdapter;
 import org.apache.mina.common.IoSession;
 import org.apache.mina.util.SessionLog;
 
 /**
  * A {@link IoFilter} which blocks connections from connecting
  * at a rate faster than the specified interval.
  * 
  * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
  */
 public class ConnectionThrottleFilter extends IoFilterAdapter {
     private static final long DEFAULT_TIME = 1000;  
     private long allowedInterval;
     private final Map<String,Long> clients;
 
     /**
      * Default constructor.  Sets the wait time to 1 second
      */
     public ConnectionThrottleFilter() {
         this( DEFAULT_TIME );
     }
 
     /**
      * Constructor that takes in a specified wait time.
      * 
      * @param allowedInterval
      * 	The number of milliseconds a client is allowed to wait
      * 	before making another successful connection
      * 
      */
     public ConnectionThrottleFilter( long allowedInterval ){
         this.allowedInterval = allowedInterval;
         clients = Collections.synchronizedMap( new HashMap<String,Long>());
     }
 
     /**
      * Sets the interval between connections from a client. 
      * This value is measured in milliseconds.
      * 
      * @param allowedInterval
      * 	The number of milliseconds a client is allowed to wait
      * 	before making another successful connection
      */
     public void setAllowedInterval(long allowedInterval) {
         this.allowedInterval = allowedInterval;
     }
 
     private synchronized boolean isConnectionOk( IoSession session ){
         SocketAddress remoteAddress = session.getRemoteAddress();
         if( remoteAddress instanceof InetSocketAddress )
         {
             long now = System.currentTimeMillis();
             InetSocketAddress addr = (InetSocketAddress)remoteAddress;
             if( clients.containsKey(addr.getAddress().getHostAddress())){
                 Long time = clients.get(addr.getAddress().getHostAddress());
                 if( (now-time) > allowedInterval ){
                     return false;
                 }
             } else {
                 clients.put( addr.getAddress().getHostAddress(), now );
                 return true;
             }
         }
        
         return false;
     }
    
     @Override
     public void sessionCreated(NextFilter nextFilter, IoSession session) throws Exception {
         if( ! isConnectionOk(session)){
              SessionLog.info( session, "Connections coming in too fast; closing." );
              session.close();
         }
     }
 }
