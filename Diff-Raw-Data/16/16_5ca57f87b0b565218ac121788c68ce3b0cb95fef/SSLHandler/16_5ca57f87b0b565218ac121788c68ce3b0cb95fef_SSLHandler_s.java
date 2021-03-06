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
 package org.apache.mina.filter.ssl;
 
 import java.net.InetSocketAddress;
 import java.nio.ByteBuffer;
 import java.util.LinkedList;
 import java.util.Queue;
 import java.util.concurrent.ConcurrentLinkedQueue;
 
 import javax.net.ssl.SSLContext;
 import javax.net.ssl.SSLEngine;
 import javax.net.ssl.SSLEngineResult;
 import javax.net.ssl.SSLException;
 import javax.net.ssl.SSLHandshakeException;
 import javax.net.ssl.SSLSession;
 
 import org.apache.mina.common.DefaultWriteRequest;
 import org.apache.mina.common.IoEventType;
 import org.apache.mina.common.IoFilterEvent;
 import org.apache.mina.common.IoSession;
 import org.apache.mina.common.WriteFuture;
 import org.apache.mina.common.WriteRequest;
 import org.apache.mina.common.IoFilter.NextFilter;
 import org.apache.mina.common.support.DefaultWriteFuture;
 import org.apache.mina.util.SessionLog;
 
 /**
  * A helper class using the SSLEngine API to decrypt/encrypt data.
  * <p>
  * Each connection has a SSLEngine that is used through the lifetime of the connection.
  * We allocate byte buffers for use as the outbound and inbound network buffers.
  * These buffers handle all of the intermediary data for the SSL connection. To make things easy,
  * we'll require outNetBuffer be completely flushed before trying to wrap any more data.
  *
  * @author The Apache Directory Project (mina-dev@directory.apache.org)
  * @version $Rev$, $Date$
  */
 class SSLHandler {
     private final SSLFilter parent;
 
     private final SSLContext ctx;
 
     private final IoSession session;
 
     private final Queue<IoFilterEvent> preHandshakeEventQueue = new LinkedList<IoFilterEvent>();
 
     private final Queue<IoFilterEvent> filterWriteEventQueue = new ConcurrentLinkedQueue<IoFilterEvent>();
     
     private final Queue<IoFilterEvent> messageReceivedEventQueue = new ConcurrentLinkedQueue<IoFilterEvent>();
 
     private SSLEngine sslEngine;
 
     /**
      * Encrypted data from the net
      */
     private ByteBuffer inNetBuffer;
 
     /**
      * Encrypted data to be written to the net
      */
     private ByteBuffer outNetBuffer;
 
     /**
      * Applicaton cleartext data to be read by application
      */
     private ByteBuffer appBuffer;
 
     /**
      * Empty buffer used during initial handshake and close operations
      */
     private final ByteBuffer hsBB = ByteBuffer.allocate(0);
 
     /**
      * Handshake status
      */
     private SSLEngineResult.HandshakeStatus handshakeStatus;
 
     /**
     * Initial handshake complete?
      */
     private boolean handshakeComplete;
 
     private boolean writingEncryptedData;
 
     /**
      * Constuctor.
      *
      * @param sslc
      * @throws SSLException
      */
     public SSLHandler(SSLFilter parent, SSLContext sslc, IoSession session)
             throws SSLException {
         this.parent = parent;
         this.session = session;
         this.ctx = sslc;
         init();
     }
 
     public void init() throws SSLException {
         if (sslEngine != null) {
             return;
         }
 
         InetSocketAddress peer = (InetSocketAddress) session
                 .getAttribute(SSLFilter.PEER_ADDRESS);
         if (peer == null) {
             sslEngine = ctx.createSSLEngine();
         } else {
             sslEngine = ctx.createSSLEngine(peer.getHostName(), peer.getPort());
         }
         sslEngine.setUseClientMode(parent.isUseClientMode());
 
         if (parent.isWantClientAuth()) {
             sslEngine.setWantClientAuth(true);
         }
 
         if (parent.isNeedClientAuth()) {
             sslEngine.setNeedClientAuth(true);
         }
 
         if (parent.getEnabledCipherSuites() != null) {
             sslEngine.setEnabledCipherSuites(parent.getEnabledCipherSuites());
         }
 
         if (parent.getEnabledProtocols() != null) {
             sslEngine.setEnabledProtocols(parent.getEnabledProtocols());
         }
 
         sslEngine.beginHandshake();
         handshakeStatus = sslEngine.getHandshakeStatus();//SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
         handshakeComplete = false;
 
         SSLByteBufferUtil.initiate(sslEngine);
 
         appBuffer = SSLByteBufferUtil.getApplicationBuffer();
 
         inNetBuffer = SSLByteBufferUtil.getPacketBuffer();
         outNetBuffer = SSLByteBufferUtil.getPacketBuffer();
         outNetBuffer.position(0);
         outNetBuffer.limit(0);
 
         writingEncryptedData = false;
     }
 
     /**
      * Release allocated ByteBuffers.
      */
     public void destroy() {
         if (sslEngine == null) {
             return;
         }
 
         // Close inbound and flush all remaining data if available.
         try {
             sslEngine.closeInbound();
         } catch (SSLException e) {
             SessionLog.debug(session,
                     "Unexpected exception from SSLEngine.closeInbound().", e);
         }
 
         try {
             do {
                 outNetBuffer.clear();
             } while (sslEngine.wrap(hsBB, outNetBuffer).bytesProduced() > 0);
         } catch (SSLException e) {
             SessionLog.debug(session,
                     "Unexpected exception from SSLEngine.wrap().", e);
         }
         sslEngine.closeOutbound();
         sslEngine = null;
 
         preHandshakeEventQueue.clear();
        //postHandshakeEventQueue.clear();
     }
 
     public SSLFilter getParent() {
         return parent;
     }
 
     public IoSession getSession() {
         return session;
     }
 
     /**
      * Check we are writing encrypted data.
      */
     public boolean isWritingEncryptedData() {
         return writingEncryptedData;
     }
 
     /**
      * Check if handshake is completed.
      */
     public boolean isHandshakeComplete() {
         return handshakeComplete;
     }
 
     public boolean isInboundDone() {
         return sslEngine == null || sslEngine.isInboundDone();
     }
 
     public boolean isOutboundDone() {
         return sslEngine == null || sslEngine.isOutboundDone();
     }
 
     /**
      * Check if there is any need to complete handshake.
      */
     public boolean needToCompleteHandshake() {
         return (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP && !isInboundDone());
     }
 
     public void schedulePreHandshakeWriteRequest(NextFilter nextFilter,
             WriteRequest writeRequest) {
         preHandshakeEventQueue.offer(new IoFilterEvent(nextFilter,
                 IoEventType.WRITE, session, writeRequest));
     }
 
     public void flushPreHandshakeEvents() throws SSLException {
         IoFilterEvent scheduledWrite;
 
         while ((scheduledWrite = preHandshakeEventQueue.poll()) != null) {
             if (SessionLog.isDebugEnabled(session)) {
                 SessionLog.debug(session, " Flushing buffered write request: "
                         + scheduledWrite.getParameter());
             }
             parent.filterWrite(scheduledWrite.getNextFilter(), session,
                     (WriteRequest) scheduledWrite.getParameter());
         }
     }
 
     public void scheduleFilterWrite(NextFilter nextFilter,
             WriteRequest writeRequest) {
         filterWriteEventQueue.offer(new IoFilterEvent(nextFilter,
                 IoEventType.WRITE, session, writeRequest));
     }
 
     public void scheduleMessageReceived(NextFilter nextFilter,
             Object message) {
         messageReceivedEventQueue.offer(new IoFilterEvent(nextFilter,
                 IoEventType.MESSAGE_RECEIVED, session, message));
     }
 
     public void flushScheduledEvents() {
         // Fire events only when no lock is hold for this handler.
         if (Thread.holdsLock(this)) {
             return;
         }
 
         IoFilterEvent e;
         
         // We need synchronization here inevitably because filterWrite can be
         // called simultaneously and cause 'bad record MAC' integrity error.
         synchronized (this) {
             while ((e = filterWriteEventQueue.poll()) != null) {
                 e.getNextFilter().filterWrite(session, (WriteRequest) e.getParameter());
             }
         }
 
         while ((e = messageReceivedEventQueue.poll()) != null) {
             e.getNextFilter().messageReceived(session, e.getParameter());
         }
     }
 
     /**
      * Call when data read from net. Will perform inial hanshake or decrypt provided
      * Buffer.
      * Decrytpted data reurned by getAppBuffer(), if any.
      *
      * @param buf buffer to decrypt
      * @throws SSLException on errors
      */
     public void messageReceived(NextFilter nextFilter, ByteBuffer buf)
             throws SSLException {
         if (buf.limit() > inNetBuffer.remaining()) {
             // We have to expand inNetBuffer
             inNetBuffer = SSLByteBufferUtil.expandBuffer(inNetBuffer,
                     inNetBuffer.capacity() + (buf.limit() * 2));
             // We also expand app. buffer (twice the size of in net. buffer)
             appBuffer = SSLByteBufferUtil.expandBuffer(appBuffer, inNetBuffer
                     .capacity() * 2);
             if (SessionLog.isDebugEnabled(session)) {
                 SessionLog.debug(session, " expanded inNetBuffer:"
                         + inNetBuffer);
                 SessionLog.debug(session, " expanded appBuffer:" + appBuffer);
             }
         }
 
         // append buf to inNetBuffer
         inNetBuffer.put(buf);
         if (!handshakeComplete) {
             handshake(nextFilter);
         } else {
             decrypt(nextFilter);
         }
 
         if (isInboundDone()) {
             // Rewind the MINA buffer if not all data is processed and inbound is finished.
             buf.position(buf.position() - inNetBuffer.position());
             inNetBuffer.clear();
         }
     }
 
     /**
      * Get decrypted application data.
      *
      * @return buffer with data
      */
     public ByteBuffer getAppBuffer() {
         return appBuffer;
     }
 
     /**
      * Get encrypted data to be sent.
      *
      * @return buffer with data
      */
     public ByteBuffer getOutNetBuffer() {
         return outNetBuffer;
     }
 
     /**
      * Encrypt provided buffer. Encytpted data reurned by getOutNetBuffer().
      *
      * @param src data to encrypt
      * @throws SSLException on errors
      */
     public void encrypt(ByteBuffer src) throws SSLException {
         if (!handshakeComplete) {
             throw new IllegalStateException();
         }
 
         // The data buffer is (must be) empty, we can reuse the entire
         // buffer.
         outNetBuffer.clear();
 
         // Loop until there is no more data in src
         while (src.hasRemaining()) {
 
             if (src.remaining() > ((outNetBuffer.capacity() - outNetBuffer
                     .position()) / 2)) {
                 // We have to expand outNetBuffer
                 // Note: there is no way to know the exact size required, but enrypted data
                 // shouln't need to be larger than twice the source data size?
                 outNetBuffer = SSLByteBufferUtil.expandBuffer(outNetBuffer, src
                         .capacity() * 2);
                 if (SessionLog.isDebugEnabled(session)) {
                     SessionLog.debug(session, " expanded outNetBuffer:"
                             + outNetBuffer);
                 }
             }
 
             SSLEngineResult result = sslEngine.wrap(src, outNetBuffer);
             if (SessionLog.isDebugEnabled(session)) {
                 SessionLog.debug(session, " Wrap res:" + result);
             }
 
             if (result.getStatus() == SSLEngineResult.Status.OK) {
                 if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                     doTasks();
                 }
             } else {
                 throw new SSLException("SSLEngine error during encrypt: "
                         + result.getStatus() + " src: " + src
                         + "outNetBuffer: " + outNetBuffer);
             }
         }
 
         outNetBuffer.flip();
     }
 
     /**
      * Start SSL shutdown process.
      *
      * @return <tt>true</tt> if shutdown process is started.
      *         <tt>false</tt> if shutdown process is already finished.
      *
      * @throws SSLException on errors
      */
     public boolean closeOutbound() throws SSLException {
         if (sslEngine == null || sslEngine.isOutboundDone()) {
             return false;
         }
 
         sslEngine.closeOutbound();
 
         // By RFC 2616, we can "fire and forget" our close_notify
         // message, so that's what we'll do here.
         outNetBuffer.clear();
         SSLEngineResult result = sslEngine.wrap(hsBB, outNetBuffer);
         if (result.getStatus() != SSLEngineResult.Status.CLOSED) {
             throw new SSLException("Improper close state: " + result);
         }
         outNetBuffer.flip();
         return true;
     }
 
     /**
      * Decrypt in net buffer. Result is stored in app buffer.
      *
      * @throws SSLException
      */
     private void decrypt(NextFilter nextFilter) throws SSLException {
 
         if (!handshakeComplete) {
             throw new IllegalStateException();
         }
 
         unwrap(nextFilter);
     }
 
     /**
      * @param status
      * @throws SSLException
      */
     private void checkStatus(SSLEngineResult res)
             throws SSLException {
         
         SSLEngineResult.Status status = res.getStatus();
         
         /*
          * The status may be:
          * OK - Normal operation
          * OVERFLOW - Should never happen since the application buffer is
          *      sized to hold the maximum packet size.
          * UNDERFLOW - Need to read more data from the socket. It's normal.
          * CLOSED - The other peer closed the socket. Also normal.
          */
         if (status != SSLEngineResult.Status.OK
                 && status != SSLEngineResult.Status.CLOSED
                 && status != SSLEngineResult.Status.BUFFER_UNDERFLOW) {
             throw new SSLException("SSLEngine error during decrypt: " + status
                     + " inNetBuffer: " + inNetBuffer + "appBuffer: "
                     + appBuffer);
         }
     }
 
     /**
      * Perform any handshaking processing.
      */
     public void handshake(NextFilter nextFilter) throws SSLException {
         if (SessionLog.isDebugEnabled(session)) {
             SessionLog.debug(session, " doHandshake()");
         }
 
         for (;;) {
             if (handshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED) {
                 session.setAttribute(SSLFilter.SSL_SESSION, sslEngine
                         .getSession());
                 if (SessionLog.isDebugEnabled(session)) {
                     SSLSession sslSession = sslEngine.getSession();
                     SessionLog.debug(session,
                             "  handshakeStatus=FINISHED");
                     SessionLog.debug(session, "  sslSession CipherSuite used "
                             + sslSession.getCipherSuite());
                 }
                 handshakeComplete = true;
                if (session.containsAttribute(SSLFilter.USE_NOTIFICATION)) {
                     scheduleMessageReceived(nextFilter,
                             SSLFilter.SESSION_SECURED);
                 }
                 break;
             } else if (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                 if (SessionLog.isDebugEnabled(session)) {
                     SessionLog.debug(session,
                             "  handshakeStatus=NEED_TASK");
                 }
                 handshakeStatus = doTasks();
             } else if (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                 // we need more data read
                 if (SessionLog.isDebugEnabled(session)) {
                     SessionLog.debug(session,
                             "  handshakeStatus=NEED_UNWRAP");
                 }
                 SSLEngineResult.Status status = unwrapHandshake(nextFilter);
                 if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW
                         || isInboundDone()) {
                     // We need more data or the session is closed
                     break;
                 }
             } else if (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                 if (SessionLog.isDebugEnabled(session)) {
                     SessionLog.debug(session,
                             "  handshakeStatus=NEED_WRAP");
                 }
                 // First make sure that the out buffer is completely empty. Since we
                 // cannot call wrap with data left on the buffer
                 if (outNetBuffer.hasRemaining()) {
                     if (SessionLog.isDebugEnabled(session)) {
                         SessionLog
                                 .debug(session, "  Still data in out buffer!");
                     }
                     break;
                 }
                 outNetBuffer.clear();
                 SSLEngineResult result = sslEngine.wrap(hsBB, outNetBuffer);
                 if (SessionLog.isDebugEnabled(session)) {
                     SessionLog.debug(session, " Wrap res:" + result);
                 }
 
                 outNetBuffer.flip();
                 handshakeStatus = result.getHandshakeStatus();
                 writeNetBuffer(nextFilter);
             } else {
                 throw new IllegalStateException("Invalid Handshaking State"
                         + handshakeStatus);
             }
         }
     }
 
     public WriteFuture writeNetBuffer(NextFilter nextFilter)
             throws SSLException {
         // Check if any net data needed to be writen
         if (!getOutNetBuffer().hasRemaining()) {
             // no; bail out
             return DefaultWriteFuture.newNotWrittenFuture(session);
         }
 
         // set flag that we are writing encrypted data
         // (used in SSLFilter.filterWrite())
         writingEncryptedData = true;
 
         // write net data
         WriteFuture writeFuture = null;
 
         try {
             if (SessionLog.isDebugEnabled(session)) {
                 SessionLog.debug(session, " write outNetBuffer: "
                         + getOutNetBuffer());
             }
             org.apache.mina.common.ByteBuffer writeBuffer = copy(getOutNetBuffer());
             if (SessionLog.isDebugEnabled(session)) {
                 SessionLog.debug(session, " session write: " + writeBuffer);
             }
             //debug("outNetBuffer (after copy): {0}", sslHandler.getOutNetBuffer());
 
             writeFuture = new DefaultWriteFuture(session);
             parent.filterWrite(nextFilter, session, new DefaultWriteRequest(
                     writeBuffer, writeFuture));
 
             // loop while more writes required to complete handshake
             while (needToCompleteHandshake()) {
                 try {
                     handshake(nextFilter);
                 } catch (SSLException ssle) {
                     SSLException newSSLE = new SSLHandshakeException(
                             "SSL handshake failed.");
                     newSSLE.initCause(ssle);
                     throw newSSLE;
                 }
                 if (getOutNetBuffer().hasRemaining()) {
                     if (SessionLog.isDebugEnabled(session)) {
                         SessionLog.debug(session, " write outNetBuffer2: "
                                 + getOutNetBuffer());
                     }
                     org.apache.mina.common.ByteBuffer writeBuffer2 = copy(getOutNetBuffer());
                     writeFuture = new DefaultWriteFuture(session);
                     parent.filterWrite(nextFilter, session,
                             new DefaultWriteRequest(writeBuffer2, writeFuture));
                 }
             }
         } finally {
             writingEncryptedData = false;
         }
 
         if (writeFuture != null) {
             return writeFuture;
         } else {
             return DefaultWriteFuture.newNotWrittenFuture(session);
         }
     }
 
     private void unwrap(NextFilter nextFilter) throws SSLException {
         if (SessionLog.isDebugEnabled(session)) {
             SessionLog.debug(session, " unwrap()");
         }
 
         // Prepare the net data for reading.
         inNetBuffer.flip();
 
         SSLEngineResult res = unwrap0();
 
         // prepare to be written again
         inNetBuffer.compact();
         
         checkStatus(res);
         
         renegotiateIfNeeded(nextFilter, res);
     }
 
     private SSLEngineResult.Status unwrapHandshake(NextFilter nextFilter) throws SSLException {
         if (SessionLog.isDebugEnabled(session)) {
             SessionLog.debug(session, " unwrapHandshake()");
         }
 
         // Prepare the net data for reading.
         inNetBuffer.flip();
 
         SSLEngineResult res = unwrap0();
         handshakeStatus = res.getHandshakeStatus();
 
         checkStatus(res);
 
         // If handshake finished, no data was produced, and the status is still ok,
         // try to unwrap more
         if (handshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED
                 && res.getStatus() == SSLEngineResult.Status.OK
                 && inNetBuffer.hasRemaining()) {
             res = unwrap0();
             
             // prepare to be written again
             inNetBuffer.compact();
 
             renegotiateIfNeeded(nextFilter, res);
         } else {
             // prepare to be written again
             inNetBuffer.compact();
         }
 
         return res.getStatus();
     }
 
     private void renegotiateIfNeeded(NextFilter nextFilter, SSLEngineResult res)
             throws SSLException {
         if (res.getStatus() != SSLEngineResult.Status.CLOSED
                 && res.getStatus() != SSLEngineResult.Status.BUFFER_UNDERFLOW
                 && res.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
             // Renegotiation required.
             SessionLog.debug(session, " Renegotiating...");
             handshakeComplete = false;
             handshakeStatus = res.getHandshakeStatus();
             handshake(nextFilter);
         }
     }
 
     private SSLEngineResult unwrap0() throws SSLException {
         SSLEngineResult res;
         do {
             if (SessionLog.isDebugEnabled(session)) {
                 SessionLog.debug(session, "   inNetBuffer: " + inNetBuffer);
                 SessionLog.debug(session, "   appBuffer: " + appBuffer);
             }
             res = sslEngine.unwrap(inNetBuffer, appBuffer);
             if (SessionLog.isDebugEnabled(session)) {
                 SessionLog.debug(session, " Unwrap res:" + res);
             }
         } while (res.getStatus() == SSLEngineResult.Status.OK
                 && (handshakeComplete && res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
                         || res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP));
         
         return res;
     }
 
     /**
      * Do all the outstanding handshake tasks in the current Thread.
      */
     private SSLEngineResult.HandshakeStatus doTasks() {
         if (SessionLog.isDebugEnabled(session)) {
             SessionLog.debug(session, "   doTasks()");
         }
 
         /*
          * We could run this in a separate thread, but I don't see the need
          * for this when used from SSLFilter. Use thread filters in MINA instead?
          */
         Runnable runnable;
         while ((runnable = sslEngine.getDelegatedTask()) != null) {
             if (SessionLog.isDebugEnabled(session)) {
                 SessionLog.debug(session, "    doTask: " + runnable);
             }
             runnable.run();
         }
         if (SessionLog.isDebugEnabled(session)) {
             SessionLog.debug(session, "   doTasks(): "
                     + sslEngine.getHandshakeStatus());
         }
         return sslEngine.getHandshakeStatus();
     }
 
     /**
      * Creates a new Mina byte buffer that is a deep copy of the remaining bytes
      * in the given buffer (between index buf.position() and buf.limit())
      *
      * @param src the buffer to copy
      * @return the new buffer, ready to read from
      */
     public static org.apache.mina.common.ByteBuffer copy(java.nio.ByteBuffer src) {
         org.apache.mina.common.ByteBuffer copy = org.apache.mina.common.ByteBuffer
                 .allocate(src.remaining());
         copy.put(src);
         copy.flip();
         return copy;
     }
 }
