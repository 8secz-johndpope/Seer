 /** (C) Copyright 2011 Hal Hildebrand, All Rights Reserved
  * 
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or (at your option) any later version.
  * 
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  * 
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  */
 package com.hellblazer.jackal.gossip.udp;
 
 import static com.hellblazer.jackal.gossip.GossipMessages.CONNECT_TO;
 import static com.hellblazer.jackal.gossip.GossipMessages.DIGEST_BYTE_SIZE;
 import static com.hellblazer.jackal.gossip.GossipMessages.GOSSIP;
 import static com.hellblazer.jackal.gossip.GossipMessages.REPLY;
 import static com.hellblazer.jackal.gossip.GossipMessages.UPDATE;
 import static java.lang.Math.min;
 import static java.lang.String.format;
 import static java.util.Arrays.asList;
 
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.PrintStream;
 import java.net.DatagramPacket;
 import java.net.DatagramSocket;
 import java.net.InetSocketAddress;
 import java.net.SocketAddress;
 import java.net.SocketException;
 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Date;
 import java.util.List;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.atomic.AtomicBoolean;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.smartfrog.services.anubis.partition.util.Identity;
 
 import com.hellblazer.jackal.gossip.Digest;
 import com.hellblazer.jackal.gossip.Endpoint;
 import com.hellblazer.jackal.gossip.Gossip;
 import com.hellblazer.jackal.gossip.GossipCommunications;
 import com.hellblazer.jackal.gossip.GossipMessages;
 import com.hellblazer.jackal.gossip.HeartbeatState;
 import com.hellblazer.jackal.util.ByteBufferPool;
 import com.hellblazer.jackal.util.HexDump;
 
 /**
  * A UDP message protocol implementation of the gossip communications
  * 
  * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
  * 
  */
 public class UdpCommunications implements GossipCommunications {
     private class GossipHandler implements GossipMessages {
         private final InetSocketAddress target;
 
         GossipHandler(InetSocketAddress target) {
             assert target.getPort() != 0 : "Invalid port";
             this.target = target;
         }
 
         @Override
         public void close() {
             // no op
         }
 
         @Override
         public void gossip(List<Digest> digests) {
             sendDigests(digests, GOSSIP);
         }
 
         @Override
         public void reply(List<Digest> digests, List<HeartbeatState> states) {
             sendDigests(digests, REPLY);
             update(states);
         }
 
         @Override
         public void requestConnection(Identity node) {
             ByteBuffer buffer = bufferPool.allocate(MAX_SEG_SIZE);
             buffer.order(ByteOrder.BIG_ENDIAN);
             buffer.position(4);
             buffer.put(CONNECT_TO);
             node.writeTo(buffer);
             send(buffer, target);
             bufferPool.free(buffer);
         }
 
         @Override
         public void update(List<HeartbeatState> deltaState) {
             ByteBuffer buffer = bufferPool.allocate(MAX_SEG_SIZE);
             buffer.order(ByteOrder.BIG_ENDIAN);
             for (HeartbeatState state : deltaState) {
                 buffer.position(4);
                 buffer.put(UPDATE);
                 state.writeTo(buffer);
                 send(buffer, target);
                 buffer.clear();
             }
             bufferPool.free(buffer);
         }
 
         private void sendDigests(List<Digest> digests, byte messageType) {
             ByteBuffer buffer = bufferPool.allocate(MAX_SEG_SIZE);
             buffer.order(ByteOrder.BIG_ENDIAN);
             for (int i = 0; i < digests.size();) {
                 int count = min(MAX_DIGESTS, digests.size() - i);
                 buffer.position(4);
                 buffer.put(messageType);
                 buffer.putInt(count);
                 for (int j = i; j < count; j++) {
                     digests.get(j).writeTo(buffer);
                 }
                 send(buffer, target);
                 i += count;
                 buffer.clear();
             }
             bufferPool.free(buffer);
         }
 
     }
 
     private static final int                  DEFAULT_RECEIVE_BUFFER_MULTIPLIER = 4;
     private static final int                  DEFAULT_SEND_BUFFER_MULTIPLIER    = 4;
     @SuppressWarnings("unchecked")
     private static final List<HeartbeatState> EMPTY_HEATBEAT_LIST               = Collections.EMPTY_LIST;
     private static final Logger               log                               = LoggerFactory.getLogger(UdpCommunications.class);
     private static final int                  MAGIC_NUMBER                      = 24051967;
     private static final int                  MAX_DIGESTS;
     /**
      * MAX_SEG_SIZE is a default maximum packet size. This may be small, but any
      * network will be capable of handling this size so the packet transfer
      * semantics are atomic (no fragmentation in the network).
      */
     private static final int                  MAX_SEG_SIZE                      = 1500;
    private static final int                  RECEIVE_TIME_OUT                  = 2000;
 
     static {
         MAX_DIGESTS = (MAX_SEG_SIZE - 4 - 4) / DIGEST_BYTE_SIZE;
     }
 
     private static String toHex(byte[] data, int offset, int length) {
         ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
         PrintStream stream = new PrintStream(baos);
         HexDump.hexdump(stream, data, offset, length);
         stream.close();
         return baos.toString();
     }
 
     private final ExecutorService dispatcher;
     private Gossip                gossip;
     private final AtomicBoolean   running    = new AtomicBoolean();
     private final DatagramSocket  socket;
     private final ByteBufferPool  bufferPool = new ByteBufferPool("UDP Comms",
                                                                   100);
 
     public UdpCommunications(InetSocketAddress endpoint,
                              ExecutorService executor) {
         this(endpoint, executor, DEFAULT_RECEIVE_BUFFER_MULTIPLIER,
              DEFAULT_SEND_BUFFER_MULTIPLIER);
     }
 
     public UdpCommunications(InetSocketAddress endpoint,
                              ExecutorService executor,
                              int receiveBufferMultiplier,
                              int sendBufferMultiplier) {
         dispatcher = executor;
         try {
             socket = new DatagramSocket(endpoint.getPort(),
                                         endpoint.getAddress());
         } catch (SocketException e) {
             log.error(format("Unable to bind to: %s", endpoint));
             throw new IllegalStateException(format("Unable to bind to: %s",
                                                    endpoint), e);
         }
         try {
             socket.setReceiveBufferSize(MAX_SEG_SIZE * receiveBufferMultiplier);
            socket.setReuseAddress(true);
             socket.setSendBufferSize(MAX_SEG_SIZE * sendBufferMultiplier);
            socket.setSendBufferSize(RECEIVE_TIME_OUT);
         } catch (SocketException e) {
             log.error(format("Unable to configure endpoint: %s", socket));
             throw new IllegalStateException(
                                             format("Unable to configure endpoint: %s",
                                                    socket), e);
         }
     }
 
     @Override
     public void connect(InetSocketAddress address, Endpoint endpoint,
                         Runnable connectAction) throws IOException {
         endpoint.setCommunications(new GossipHandler(address));
         connectAction.run();
     }
 
     @Override
     public InetSocketAddress getLocalAddress() {
         return new InetSocketAddress(socket.getLocalAddress(),
                                      socket.getLocalPort());
     }
 
     @Override
     public void send(HeartbeatState state, InetSocketAddress left) {
         if (!gossip.isIgnoring(left)) {
             ByteBuffer buffer = bufferPool.allocate(MAX_SEG_SIZE);
             buffer.order(ByteOrder.BIG_ENDIAN);
             buffer.position(4);
             buffer.put(UPDATE);
             state.writeTo(buffer);
             buffer.flip();
             send(buffer, left);
             bufferPool.free(buffer);
         }
     }
 
     @Override
     public void setGossip(Gossip gossip) {
         this.gossip = gossip;
     }
 
     /**
      * Start the service
      */
     @Override
     public void start() {
         if (running.compareAndSet(false, true)) {
             dispatcher.execute(serviceTask());
         }
     }
 
     /**
      * Stop the service
      */
     @Override
     public void terminate() {
         if (running.compareAndSet(true, false)) {
             if (log.isInfoEnabled()) {
                 log.info(String.format("Terminating UDP Communications on %s",
                                        socket.getLocalSocketAddress()));
             }
             socket.close();
             log.info(bufferPool.toString());
         }
     }
 
     private void handleConnectTo(ByteBuffer buffer) {
         Identity peer;
         try {
             peer = new Identity(buffer);
         } catch (Throwable e) {
             if (log.isWarnEnabled()) {
                 log.warn("Cannot deserialize identity. Ignoring the connection request.",
                          e);
             }
             return;
         }
         gossip.connectTo(peer);
     }
 
     private void handleGossip(final InetSocketAddress target, ByteBuffer msg) {
         int count = msg.getInt();
         if (log.isTraceEnabled()) {
             log.trace("Handling gossip, digest count: " + count);
         }
         final List<Digest> digests = new ArrayList<Digest>(count);
         for (int i = 0; i < count; i++) {
             Digest digest;
             try {
                 digest = new Digest(msg);
             } catch (Throwable e) {
                 if (log.isWarnEnabled()) {
                     log.warn("Cannot deserialize digest. Ignoring the digest.",
                              e);
                 }
                 continue;
             }
             digests.add(digest);
         }
         if (log.isTraceEnabled()) {
             log.trace(format("Gossip digests from %s are : %s", this, digests));
         }
         gossip.gossip(digests, new GossipHandler(target));
     }
 
     private void handleReply(final InetSocketAddress target, ByteBuffer msg) {
         int digestCount = msg.getInt();
         if (log.isTraceEnabled()) {
             log.trace("Handling reply, digest count: " + digestCount);
         }
         final List<Digest> digests = new ArrayList<Digest>(digestCount);
         for (int i = 0; i < digestCount; i++) {
             Digest digest;
             try {
                 digest = new Digest(msg);
             } catch (Throwable e) {
                 if (log.isWarnEnabled()) {
                     log.warn("Cannot deserialize digest. Ignoring the digest.",
                              e);
                 }
                 continue;
             }
             digests.add(digest);
         }
         gossip.reply(digests, EMPTY_HEATBEAT_LIST, new GossipHandler(target));
     }
 
     private void handleUpdate(ByteBuffer msg) {
         final HeartbeatState state;
         try {
             state = new HeartbeatState(msg);
         } catch (Throwable e) {
             if (log.isWarnEnabled()) {
                 log.warn("Cannot deserialize heartbeat state. Ignoring the state.",
                          e);
             }
             return;
         }
         if (log.isTraceEnabled()) {
             log.trace(format("Heartbeat state from %s is : %s", this, state));
         }
         gossip.update(asList(state));
     }
 
     private static String prettyPrint(SocketAddress sender,
                                       SocketAddress target, byte[] bytes) {
         final StringBuilder sb = new StringBuilder(bytes.length * 2);
         sb.append('\n');
         sb.append(new SimpleDateFormat().format(new Date()));
         sb.append(" sender: ");
         sb.append(sender);
         sb.append(" target: ");
         sb.append(target);
         sb.append('\n');
         sb.append(toHex(bytes, 0, bytes.length));
         return sb.toString();
     }
 
     /**
      * Process the inbound message
      * 
      * @param buffer
      *            - the message bytes
      */
     private void processInbound(InetSocketAddress sender, ByteBuffer buffer) {
         if (gossip.isIgnoring(sender)) {
             if (log.isTraceEnabled()) {
                 log.trace(String.format("Ignoring inbound msg from: %s", sender));
             }
             return;
         }
         byte msgType = buffer.get();
         switch (msgType) {
             case GOSSIP: {
                 handleGossip(sender, buffer);
                 break;
             }
             case REPLY: {
                 handleReply(sender, buffer);
                 break;
             }
             case UPDATE: {
                 handleUpdate(buffer);
                 break;
             }
             case CONNECT_TO: {
                 handleConnectTo(buffer);
                 break;
             }
             default: {
                 if (log.isInfoEnabled()) {
                     log.info(format("invalid message type: %s from: %s",
                                     msgType, this));
                 }
             }
         }
     }
 
     /**
      * Send the datagram across the net
      * 
      * @param buffer
      * @param target
      * @throws IOException
      */
     private void send(ByteBuffer buffer, SocketAddress target) {
         buffer.putInt(0, MAGIC_NUMBER);
         try {
             byte[] bytes = buffer.array();
             DatagramPacket packet = new DatagramPacket(bytes, bytes.length,
                                                        target);
             if (log.isTraceEnabled()) {
                 log.trace(String.format("sending packet %s",
                                         prettyPrint(getLocalAddress(), target,
                                                     buffer.array())));
             }
             socket.send(packet);
         } catch (SocketException e) {
             if (!"Socket is closed".equals(e.getMessage())
                 && !"Bad file descriptor".equals(e.getMessage())) {
                 if (log.isWarnEnabled()) {
                     log.warn("Error sending packet", e);
                 }
             }
         } catch (IOException e) {
             if (log.isWarnEnabled()) {
                 log.warn("Error sending packet", e);
             }
         }
     }
 
     /**
      * Service the next inbound datagram
      * 
      * @param buffer
      *            - the buffer to use to receive the datagram
      * @throws IOException
      */
     private void service() throws IOException {
         final ByteBuffer buffer = bufferPool.allocate(MAX_SEG_SIZE);
         buffer.order(ByteOrder.BIG_ENDIAN);
         final DatagramPacket packet = new DatagramPacket(buffer.array(),
                                                          buffer.array().length);
         socket.receive(packet);
         // buffer.limit(packet.getLength());
         dispatcher.execute(new Runnable() {
             @Override
             public void run() {
                 if (log.isTraceEnabled()) {
                     log.trace(String.format("Received packet %s",
                                             prettyPrint(packet.getSocketAddress(),
                                                         getLocalAddress(),
                                                         buffer.array())));
                 } else if (log.isTraceEnabled()) {
                     log.trace("Received packet from: "
                               + packet.getSocketAddress());
                 }
                 int magic = buffer.getInt();
                 if (MAGIC_NUMBER == magic) {
                     try {
                         processInbound((InetSocketAddress) packet.getSocketAddress(),
                                        buffer);
                     } catch (Throwable e) {
                         if (log.isWarnEnabled()) {
                             log.warn(format("Invalid message: %s",
                                             prettyPrint(packet.getSocketAddress(),
                                                         getLocalAddress(),
                                                         buffer.array())), e);
                         }
                     }
                 } else {
                     if (log.isWarnEnabled()) {
                         log.warn(format("Msg with invalid MAGIC header [%s] discarded %s",
                                         magic,
                                         prettyPrint(packet.getSocketAddress(),
                                                     getLocalAddress(),
                                                     buffer.array())));
                     }
                 }
                 bufferPool.free(buffer);
             }
         });
     }
 
     /**
      * The service loop.
      * 
      * @return the Runnable action implementing the service loop.
      */
     private Runnable serviceTask() {
         return new Runnable() {
             @Override
             public void run() {
                 if (log.isInfoEnabled()) {
                     log.info(String.format("UDP Gossip communications started on %s",
                                            socket.getLocalSocketAddress()));
                 }
                 while (running.get()) {
                     try {
                         service();
                     } catch (SocketException e) {
                         if ("Socket closed".equals(e.getMessage())) {
                             if (log.isTraceEnabled()) {
                                 log.trace("Socket closed, shutting down");
                                 terminate();
                                 return;
                             }
                         }
                     } catch (Throwable e) {
                         if (log.isWarnEnabled()) {
                             log.warn("Exception processing inbound message", e);
                         }
                     }
                 }
             }
         };
     }
 }
