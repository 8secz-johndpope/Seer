 /*
  * The source code contained in this file is in in the public domain.
  * It can be used in any project or product without prior permission,
  * license or royalty payments. There is  NO WARRANTY OF ANY KIND,
  * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, WITHOUT LIMITATION,
  * THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
  * AND DATA ACCURACY.  We do not warrant or make any representations
  * regarding the use of the software or the  results thereof, including
  * but not limited to the correctness, accuracy, reliability or
  * usefulness of the software.
  */
 package org.mobicents.media.server.impl.conference;
 
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Properties;
 import org.mobicents.media.format.UnsupportedFormatException;
 import org.mobicents.media.protocol.PushBufferStream;
 import org.apache.log4j.Logger;
 import org.mobicents.media.server.impl.BaseConnection;
 import org.mobicents.media.server.impl.BaseResource;
 import org.mobicents.media.server.impl.common.MediaResourceState;
 import org.mobicents.media.server.impl.common.MediaResourceType;
 import org.mobicents.media.server.impl.jmf.splitter.MediaSplitter;
 import org.mobicents.media.server.spi.Connection;
 import org.mobicents.media.server.spi.Endpoint;
 import org.mobicents.media.server.spi.MediaSink;
 import org.mobicents.media.server.spi.NotificationListener;
 
 /**
  *
  * @author Oleg Kulikov
  */
 public class LocalSplitter extends BaseResource implements MediaSink {
 
     private String id;
     private Map streams = Collections.synchronizedMap(new HashMap());
     private MediaSplitter splitter = new MediaSplitter();
     private ConfEndpointImpl endpoint;
     private BaseConnection connection;
     private Logger logger = Logger.getLogger(LocalSplitter.class);
 
     public LocalSplitter(Endpoint endpoint, Connection connection) {
         this.endpoint = (ConfEndpointImpl) endpoint;
         this.connection = (BaseConnection) connection;
         this.id = connection.getId();
         this.addStateListener(this.endpoint.splitterStateListener);
     }
 
     public LocalSplitter(String id) {
         this.id = id;
     }
 
     public void setInputStream(PushBufferStream pushStream) {
         splitter.setInputStream(pushStream);
     }
 
     public PushBufferStream newBranch(String id) {
         PushBufferStream branch = splitter.newBranch();
         streams.put(id, branch);
         if (logger.isDebugEnabled()) {
             logger.debug("id=" + this.id + ", created new branch for connection id = " +
                     id + ", branches=" + splitter.getSize());
         }
         return branch;
     }
 
    public void remove(String id) {
         PushBufferStream pushStream = (PushBufferStream) streams.remove(id);
         if (pushStream != null) {
             splitter.closeBranch(pushStream);
             if (logger.isDebugEnabled()) {
                 logger.debug("id=" + this.id + ", removed branch for connection id = " +
                         id + ", branches=" + splitter.getSize());
             }
         }
        //return pushStream;
     }
 
     public void close() {
         splitter.close();
         if (logger.isDebugEnabled()) {
             logger.debug("id=" + this.id + " close splitter");
         }
     }
 
     @Override
     public String toString() {
         return "LocalSplitter[" + id + "]";
     }
 
     public void configure(Properties config) {
         setState(MediaResourceState.CONFIGURED);
     }
 
     public void prepare(Endpoint epn, PushBufferStream mediaStream) throws UnsupportedFormatException {
         splitter.setInputStream(mediaStream);
         Collection<BaseConnection> connections = endpoint.getConnections();
         for (BaseConnection conn : connections) {
             if (!conn.getId().equals(this.id)) {
                 LocalMixer mixer = (LocalMixer) endpoint.getResource(MediaResourceType.AUDIO_SOURCE, conn.getId());
                 if (mixer!= null && mixer.getState()!=MediaResourceState.NULL) {
                     try {
                         mixer.add(id, this.newBranch(conn.getId()));
                     } catch (UnsupportedFormatException e) {
                         logger.error("Unexpected error", e);
                     }
                 }
             }
         }
         setState(MediaResourceState.PREPARED);
     }
 
     public void addListener(NotificationListener listener) {
         throw new UnsupportedOperationException("Not supported yet.");
     }
 
     public void removeListener(NotificationListener listener) {
         throw new UnsupportedOperationException("Not supported yet.");
     }
 
     public void start() {
         setState(MediaResourceState.STARTED);
     }
 
     public void stop() {
         if (getState() == MediaResourceState.STARTED) {
             setState(MediaResourceState.PREPARED);
         }
     }
 
     public void release() {
         setState(MediaResourceState.NULL);
         Collection<BaseConnection> connections = endpoint.getConnections();
         for (BaseConnection conn : connections) {
             if (!conn.getId().equals(this.id)) {
                 LocalMixer mixer = (LocalMixer) endpoint.getResource(
                 		MediaResourceType.AUDIO_SOURCE,
                         conn.getId());
                 if (mixer != null) mixer.remove(id);
             }
         }
     }

 }
