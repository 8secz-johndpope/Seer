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
 package org.apache.mina.common;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.net.SocketAddress;
 import java.nio.channels.FileChannel;
 import java.util.Queue;
 import java.util.Set;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.atomic.AtomicLong;
 
 import org.apache.mina.util.CircularQueue;
 import org.apache.mina.util.SynchronizedQueue;
 
 
 /**
  * Base implementation of {@link IoSession}.
  *
  * @author The Apache MINA Project (dev@mina.apache.org)
  * @version $Rev$, $Date$
  */
 public abstract class AbstractIoSession implements IoSession {
 
     private static final IoFutureListener<CloseFuture> SCHEDULED_COUNTER_RESETTER =
         new IoFutureListener<CloseFuture>() {
             public void operationComplete(CloseFuture future) {
                 AbstractIoSession s = (AbstractIoSession) future.getSession();
                 s.scheduledWriteBytes.set(0);
                 s.scheduledWriteMessages.set(0);
             }
     };
 
     /**
      * An internal write request object that triggers session close.
      * @see #writeRequestQueue
      */
     private static final WriteRequest CLOSE_REQUEST =
         new DefaultWriteRequest(new Object());
 
     private final Object lock = new Object();
 
     private IoSessionAttributeMap attributes;
 
     private final Queue<WriteRequest> writeRequestQueue =
         new SynchronizedQueue<WriteRequest>(new CircularQueue<WriteRequest>(512)) {
 
             private static final long serialVersionUID = 6579730560333933524L;
 
             // Discard close request offered by closeOnFlush() silently.
             @Override
             public synchronized WriteRequest peek() {
                 WriteRequest answer = super.peek();
                 if (answer == CLOSE_REQUEST) {
                     AbstractIoSession.this.close();
                     clear();
                     answer = null;
                 }
                 return answer;
             }
 
             @Override
             public synchronized WriteRequest poll() {
                 WriteRequest answer = super.poll();
                 if (answer == CLOSE_REQUEST) {
                     AbstractIoSession.this.close();
                     clear();
                     answer = null;
                 }
                 return answer;
             }
     };
 
     private final long creationTime;
 
     /**
      * A future that will be set 'closed' when the connection is closed.
      */
     private final CloseFuture closeFuture = new DefaultCloseFuture(this);
 
     private volatile boolean closing;
 
     private volatile TrafficMask trafficMask = TrafficMask.ALL;
 
     // Status variables
     private final AtomicBoolean scheduledForFlush = new AtomicBoolean();
 
     private final AtomicLong scheduledWriteBytes = new AtomicLong();
 
     private final AtomicInteger scheduledWriteMessages = new AtomicInteger();
 
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
 
     private boolean deferDecreaseReadBuffer = true;
 
     protected AbstractIoSession() {
         creationTime = lastReadTime = lastWriteTime =
             lastIdleTimeForBoth = lastIdleTimeForRead =
                 lastIdleTimeForWrite = System.currentTimeMillis();
         closeFuture.addListener(SCHEDULED_COUNTER_RESETTER);
     }
 
     protected abstract IoProcessor getProcessor();
 
     public boolean isConnected() {
         return !closeFuture.isClosed();
     }
 
     public boolean isClosing() {
         return closing || closeFuture.isClosed();
     }
 
     public CloseFuture getCloseFuture() {
         return closeFuture;
     }
 
     protected boolean isScheduledForFlush() {
         return scheduledForFlush.get();
     }
 
     protected boolean setScheduledForFlush(boolean flag) {
         if (flag) {
             return scheduledForFlush.compareAndSet(false, true);
         } else {
             scheduledForFlush.set(false);
             return true;
         }
     }
 
     public CloseFuture close(boolean rightNow) {
         if (rightNow) {
             return close();
         } else {
             return closeOnFlush();
         }
     }
 
     public CloseFuture close() {
         synchronized (lock) {
             if (isClosing()) {
                 return closeFuture;
             } else {
                 closing = true;
             }
         }
 
         getFilterChain().fireFilterClose();
         return closeFuture;
     }
 
     public CloseFuture closeOnFlush() {
         getWriteRequestQueue().offer(CLOSE_REQUEST);
         getProcessor().flush(this);
         return closeFuture;
     }
 
     public WriteFuture write(Object message) {
         return write(message, null);
     }
 
     public WriteFuture write(Object message, SocketAddress remoteAddress) {
         if (message == null) {
             throw new NullPointerException("message");
         }
 
         if (!getTransportMetadata().isConnectionless() &&
                 remoteAddress != null) {
             throw new UnsupportedOperationException();
         }
 
         if (isClosing() || !isConnected()) {
             WriteFuture future = new DefaultWriteFuture(this);
             WriteRequest request = new DefaultWriteRequest(message, future, remoteAddress);
             future.setException(new WriteToClosedSessionException(request));
             return future;
         }
 
         FileChannel channel = null;
         if (message instanceof IoBuffer
                 && !((IoBuffer) message).hasRemaining()) {
             throw new IllegalArgumentException(
                     "message is empty. Forgot to call flip()?");
         } else if (message instanceof FileChannel) {
             channel = (FileChannel) message;
             try {
                 message = new DefaultFileRegion(channel, 0, channel.size());
             } catch (IOException e) {
                 ExceptionMonitor.getInstance().exceptionCaught(e);
                 return DefaultWriteFuture.newNotWrittenFuture(this, e);
             }
         } else if (message instanceof File) {
             File file = (File) message;
             try {
                 channel = new FileInputStream(file).getChannel();
             } catch (IOException e) {
                 ExceptionMonitor.getInstance().exceptionCaught(e);
                 return DefaultWriteFuture.newNotWrittenFuture(this, e);
             }
         }
 
         WriteFuture future = new DefaultWriteFuture(this);
         getFilterChain().fireFilterWrite(
                 new DefaultWriteRequest(message, future, remoteAddress));
 
         if (message instanceof File) {
             final FileChannel finalChannel = channel;
             future.addListener(new IoFutureListener<WriteFuture>() {
                 public void operationComplete(WriteFuture future) {
                     try {
                         finalChannel.close();
                     } catch (IOException e) {
                         ExceptionMonitor.getInstance().exceptionCaught(e);
                     }
                 }
             });
         }
 
         return future;
     }
 
     public Object getAttachment() {
         return getAttribute("");
     }
 
     public Object setAttachment(Object attachment) {
         return setAttribute("", attachment);
     }
 
     public Object getAttribute(Object key) {
         return attributes.getAttribute(this, key);
     }
 
     public Object getAttribute(Object key, Object defaultValue) {
         return attributes.getAttribute(this, key, defaultValue);
     }
 
     public Object setAttribute(Object key, Object value) {
         return attributes.setAttribute(this, key, value);
     }
 
     public Object setAttribute(Object key) {
         return attributes.setAttribute(this, key);
     }
 
     public Object setAttributeIfAbsent(Object key, Object value) {
         return attributes.setAttributeIfAbsent(this, key, value);
     }
 
     public Object removeAttribute(Object key) {
         return attributes.removeAttribute(this, key);
     }
 
     public boolean removeAttribute(Object key, Object value) {
         return attributes.removeAttribute(this, key, value);
     }
 
     public boolean replaceAttribute(Object key, Object oldValue, Object newValue) {
         return attributes.replaceAttribute(this, key, oldValue, newValue);
     }
 
     public boolean containsAttribute(Object key) {
         return attributes.containsAttribute(this, key);
     }
 
     public Set<Object> getAttributeKeys() {
         return attributes.getAttributeKeys(this);
     }
     
     protected IoSessionAttributeMap getAttributeMap() {
         return attributes;
     }
 
     protected void setAttributeMap(IoSessionAttributeMap attributes) {
         this.attributes = attributes;
     }
 
     public TrafficMask getTrafficMask() {
         return trafficMask;
     }
 
     public void setTrafficMask(TrafficMask trafficMask) {
         if (trafficMask == null) {
             throw new NullPointerException("trafficMask");
         }
         
         getFilterChain().fireFilterSetTrafficMask(trafficMask);
     }
     
     protected void setTrafficMaskNow(TrafficMask trafficMask) {
         this.trafficMask = trafficMask;
     }
 
     public void suspendRead() {
         setTrafficMask(getTrafficMask().and(TrafficMask.READ.not()));
     }
 
     public void suspendWrite() {
         setTrafficMask(getTrafficMask().and(TrafficMask.WRITE.not()));
     }
 
     public void resumeRead() {
         setTrafficMask(getTrafficMask().or(TrafficMask.READ));
     }
 
     public void resumeWrite() {
         setTrafficMask(getTrafficMask().or(TrafficMask.WRITE));
     }
 
     public long getReadBytes() {
         return readBytes;
     }
 
     public long getWrittenBytes() {
         return writtenBytes;
     }
 
     public long getReadMessages() {
         return readMessages;
     }
 
     public long getWrittenMessages() {
         return writtenMessages;
     }
 
     public long getScheduledWriteBytes() {
         return scheduledWriteBytes.get();
     }
 
     public int getScheduledWriteMessages() {
         return scheduledWriteMessages.get();
     }
 
     protected void increaseReadBytes(long increment) {
         if (increment > 0) {
             readBytes += increment;
             lastReadTime = System.currentTimeMillis();
             idleCountForBoth = 0;
             idleCountForRead = 0;
 
             if (getService() instanceof AbstractIoService) {
                 ((AbstractIoService) getService()).increaseReadBytes(increment);
             }
         }
     }
     
    protected void increaseReadMessages() {
        readMessages++;
        lastWriteTime = System.currentTimeMillis();
        if (getService() instanceof AbstractIoService) {
            ((AbstractIoService) getService()).increaseReadMessages();
        }
    }

     protected void increaseWrittenBytesAndMessages(WriteRequest request) {
         Object message = request.getMessage();
         if (message instanceof IoBuffer) {
             IoBuffer b = (IoBuffer) message;
             if (b.hasRemaining()) {
                 increaseWrittenBytes(((IoBuffer) message).remaining());
             } else {
                 increaseWrittenMessages();
             }
         } else {
             increaseWrittenMessages();
         }
     }
     
     protected void increaseWrittenBytes(long increment) {
         if (increment > 0) {
             writtenBytes += increment;
             lastWriteTime = System.currentTimeMillis();
             idleCountForBoth = 0;
             idleCountForWrite = 0;
 
             if (getService() instanceof AbstractIoService) {
                 ((AbstractIoService) getService()).increaseWrittenBytes(increment);
             }
 
             increaseScheduledWriteBytes(-increment);
         }
     }
 
     protected void increaseWrittenMessages() {
         writtenMessages++;
        lastWriteTime = System.currentTimeMillis();
         if (getService() instanceof AbstractIoService) {
             ((AbstractIoService) getService()).increaseWrittenMessages();
         }
 
         decreaseScheduledWriteMessages();
     }
 
     protected void increaseScheduledWriteBytes(long increment) {
         scheduledWriteBytes.addAndGet(increment);
         if (getService() instanceof AbstractIoService) {
             ((AbstractIoService) getService()).increaseScheduledWriteBytes(increment);
         }
     }
 
     protected void increaseScheduledWriteMessages() {
         scheduledWriteMessages.incrementAndGet();
         if (getService() instanceof AbstractIoService) {
             ((AbstractIoService) getService()).increaseScheduledWriteMessages();
         }
     }
 
     protected void decreaseScheduledWriteMessages() {
         scheduledWriteMessages.decrementAndGet();
         if (getService() instanceof AbstractIoService) {
             ((AbstractIoService) getService()).decreaseScheduledWriteMessages();
         }
     }
 
     protected void decreaseScheduledBytesAndMessages(WriteRequest request) {
         Object message = request.getMessage();
         if (message instanceof IoBuffer) {
             IoBuffer b = (IoBuffer) message;
             if (b.hasRemaining()) {
                 increaseScheduledWriteBytes(-((IoBuffer) message).remaining());
             } else {
                 decreaseScheduledWriteMessages();
             }
         } else {
             decreaseScheduledWriteMessages();
         }
     }
 
     protected Queue<WriteRequest> getWriteRequestQueue() {
         return writeRequestQueue;
     }
 
     protected void increaseReadBufferSize() {
         int newReadBufferSize = getConfig().getReadBufferSize() << 1;
         if (newReadBufferSize <= getConfig().getMaxReadBufferSize()) {
             getConfig().setReadBufferSize(newReadBufferSize);
         } else {
             getConfig().setReadBufferSize(getConfig().getMaxReadBufferSize());
         }
 
         deferDecreaseReadBuffer = true;
     }
 
     protected void decreaseReadBufferSize() {
         if (deferDecreaseReadBuffer) {
             deferDecreaseReadBuffer = false;
             return;
         }
 
         if (getConfig().getReadBufferSize() > getConfig().getMinReadBufferSize()) {
             getConfig().setReadBufferSize(getConfig().getReadBufferSize() >>> 1);
         }
 
         deferDecreaseReadBuffer = true;
     }
 
     public long getCreationTime() {
         return creationTime;
     }
 
     public long getLastIoTime() {
         return Math.max(lastReadTime, lastWriteTime);
     }
 
     public long getLastReadTime() {
         return lastReadTime;
     }
 
     public long getLastWriteTime() {
         return lastWriteTime;
     }
 
     public boolean isIdle(IdleStatus status) {
         if (status == IdleStatus.BOTH_IDLE) {
             return idleCountForBoth > 0;
         }
 
         if (status == IdleStatus.READER_IDLE) {
             return idleCountForRead > 0;
         }
 
         if (status == IdleStatus.WRITER_IDLE) {
             return idleCountForWrite > 0;
         }
 
         throw new IllegalArgumentException("Unknown idle status: " + status);
     }
 
     public int getIdleCount(IdleStatus status) {
         if (status == IdleStatus.BOTH_IDLE) {
             return idleCountForBoth;
         }
 
         if (status == IdleStatus.READER_IDLE) {
             return idleCountForRead;
         }
 
         if (status == IdleStatus.WRITER_IDLE) {
             return idleCountForWrite;
         }
 
         throw new IllegalArgumentException("Unknown idle status: " + status);
     }
 
     public long getLastIdleTime(IdleStatus status) {
         if (status == IdleStatus.BOTH_IDLE) {
             return lastIdleTimeForBoth;
         }
 
         if (status == IdleStatus.READER_IDLE) {
             return lastIdleTimeForRead;
         }
 
         if (status == IdleStatus.WRITER_IDLE) {
             return lastIdleTimeForWrite;
         }
 
         throw new IllegalArgumentException("Unknown idle status: " + status);
     }
 
     protected void increaseIdleCount(IdleStatus status) {
         if (status == IdleStatus.BOTH_IDLE) {
             idleCountForBoth++;
             lastIdleTimeForBoth = System.currentTimeMillis();
         } else if (status == IdleStatus.READER_IDLE) {
             idleCountForRead++;
             lastIdleTimeForRead = System.currentTimeMillis();
         } else if (status == IdleStatus.WRITER_IDLE) {
             idleCountForWrite++;
             lastIdleTimeForWrite = System.currentTimeMillis();
         } else {
             throw new IllegalArgumentException("Unknown idle status: " + status);
         }
     }
 
     public SocketAddress getServiceAddress() {
         IoService service = getService();
         if (service instanceof IoAcceptor) {
             return ((IoAcceptor) service).getLocalAddress();
         } else {
             return getRemoteAddress();
         }
     }
 
     @Override
     public final int hashCode() {
         return System.identityHashCode(this);
     }
 
     @Override
     public final boolean equals(Object o) {
         return this == o;
     }
 
     @Override
     public String toString() {
         if (getService() instanceof IoAcceptor) {
             return "(" + getServiceName() + ", server, " +
                     getRemoteAddress() + " => " +
                     getLocalAddress() + ')';
         } else {
             return "(" + getServiceName() + ", client, " +
                     getLocalAddress() + " => " +
                     getRemoteAddress() + ')';
         }
     }
 
     private String getServiceName() {
         TransportMetadata tm = getTransportMetadata();
         if (tm == null) {
             return "null";
         } else {
             return tm.getName();
         }
     }
 }
