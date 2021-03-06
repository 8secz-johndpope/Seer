 /*
  * ################################################################
  *
  * ProActive Parallel Suite(TM): The Java(TM) library for
  *    Parallel, Distributed, Multi-Core Computing for
  *    Enterprise Grids & Clouds
  *
  * Copyright (C) 1997-2010 INRIA/University of 
  * 				Nice-Sophia Antipolis/ActiveEon
  * Contact: proactive@ow2.org or contact@activeeon.com
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; version 3 of
  * the License.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this library; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
  * USA
  *
  * If needed, contact us to obtain a release under GPL Version 2 
  * or a different license than the GPL.
  *
  *  Initial developer(s):               The ActiveEon Team
  *                        http://www.activeeon.com/
  *  Contributor(s):
  *
  * ################################################################
  * $$ACTIVEEON_INITIAL_DEV$$
  */
 package org.objectweb.proactive.extra.messagerouting.client;
 
 import java.io.IOException;
 import java.lang.management.ManagementFactory;
 import java.lang.reflect.Constructor;
 import java.net.InetAddress;
 import java.net.Socket;
 import java.net.URI;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.List;
 
 import java.util.Map;
 import java.util.Timer;
 import java.util.TimerTask;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.TimeoutException;
 import java.util.concurrent.atomic.AtomicLong;
 
 import javax.management.MBeanServer;
 import javax.management.ObjectName;
 
 import org.apache.log4j.Logger;
 import org.objectweb.proactive.core.ProActiveException;
 import org.objectweb.proactive.core.util.Sleeper;
 import org.objectweb.proactive.core.util.SweetCountDownLatch;
 import org.objectweb.proactive.core.util.log.ProActiveLogger;
 import org.objectweb.proactive.extra.messagerouting.PAMRConfig;
 import org.objectweb.proactive.extra.messagerouting.exceptions.MalformedMessageException;
 import org.objectweb.proactive.extra.messagerouting.exceptions.MessageRoutingException;
 import org.objectweb.proactive.extra.messagerouting.protocol.AgentID;
 import org.objectweb.proactive.extra.messagerouting.protocol.MagicCookie;
 import org.objectweb.proactive.extra.messagerouting.protocol.message.DataReplyMessage;
 import org.objectweb.proactive.extra.messagerouting.protocol.message.DataRequestMessage;
 import org.objectweb.proactive.extra.messagerouting.protocol.message.ErrorMessage;
 import org.objectweb.proactive.extra.messagerouting.protocol.message.HeartbeatClientMessage;
 import org.objectweb.proactive.extra.messagerouting.protocol.message.HeartbeatMessage;
 import org.objectweb.proactive.extra.messagerouting.protocol.message.Message;
 import org.objectweb.proactive.extra.messagerouting.protocol.message.RegistrationMessage;
 import org.objectweb.proactive.extra.messagerouting.protocol.message.RegistrationReplyMessage;
 import org.objectweb.proactive.extra.messagerouting.protocol.message.RegistrationRequestMessage;
 import org.objectweb.proactive.extra.messagerouting.protocol.message.ErrorMessage.ErrorType;
 import org.objectweb.proactive.extra.messagerouting.protocol.message.Message.MessageType;
 import org.objectweb.proactive.extra.messagerouting.remoteobject.util.socketfactory.MessageRoutingSocketFactorySPI;
 import org.objectweb.proactive.extra.messagerouting.router.Router;
 import org.objectweb.proactive.extra.messagerouting.router.RouterImpl;
 
 
 /**
  * Implementation of the local message routing client.
  * 
  * 
  * It contacts the router as soon as created and try to maintain the connection
  * open (eg. if the connection is closed then it will be reopened).
  * 
  * @since ProActive 4.1.0
  */
 public class AgentImpl implements Agent, AgentImplMBean {
     public static final Logger logger = ProActiveLogger.getLogger(PAMRConfig.Loggers.FORWARDING_CLIENT);
 
     /** Address of the router */
     final private InetAddress routerAddr;
     /** Port of the router */
     final private int routerPort;
 
     /** Local AgentID, set after initialization. */
     private AgentID agentID = null;
     /** Remote router ID, set after initialization */
     volatile private long routerID;
     /** The magic cookie to use to connect to the router */
     final private MagicCookie magicCookie;
 
     /** Request ID Generator **/
     private final AtomicLong requestIDGenerator;
 
     /** Senders waiting for a response */
     final private WaitingRoom mailboxes;
 
     /** Current tunnel, can be null */
     volatile private Tunnel t = null;
     /** List of tunnel reported as failed */
     final private List<Tunnel> failedTunnels;
 
     /** Every received data message will be handled by this object */
     final private MessageHandler messageHandler;
 
     /** List of Valves that will process each message */
     final private List<Valve> valves;
 
     /** The socket factory to use to create the Tunnel */
     final private MessageRoutingSocketFactorySPI socketFactory;
 
     final private Timer timer;
 
     private HeartbeatTask heartbeatTask;
 
     /**
      * Create a routing agent
      * 
      * The router must be available when the constructor is called.
      * 
      * @param routerAddr
      *            Address of the router
      * @param routerPort
      *            TCP port on which the router listen
      * @param agentId
      *            If the client want to get a reserved agentId number
      * @param magicCookie
      *            The magic cookie to submit to the router
      * @param messageHandlerClass
      *            Class the will handled received message
      * @throws ProActiveException
      *             If the router cannot be contacted.
      */
     public AgentImpl(InetAddress routerAddr, int routerPort, AgentID agentId, MagicCookie magicCookie,
             Class<? extends MessageHandler> messageHandlerClass, MessageRoutingSocketFactorySPI socketFactory)
             throws ProActiveException {
         this(routerAddr, routerPort, agentId, magicCookie, messageHandlerClass, new ArrayList<Valve>(),
                 socketFactory);
     }
 
     /**
      * Create a routing agent
      * 
      * The router must be available when the constructor is called.
      * 
      * @param routerAddr
      *            Address of the router
      * @param routerPort
      *            TCP port on which the router listen
      * @param agentId
      *            If the client want to get a reserved agentId number
      * @param magicCookie
      *            The magic cookie to submit to the router
      * @param messageHandlerClass
      *            Class the will handled received message
      * @param valves
      *            List of {@link Valve} to be applied to all incomming and
      *            outgoing messages.
      * @throws ProActiveException
      *             If the router cannot be contacted.
      */
     public AgentImpl(InetAddress routerAddr, int routerPort, AgentID agentId, MagicCookie magicCookie,
             Class<? extends MessageHandler> messageHandlerClass, List<Valve> valves,
             MessageRoutingSocketFactorySPI socketFactory) throws ProActiveException {
         this.routerAddr = routerAddr;
         this.routerPort = routerPort;
         this.valves = valves;
         this.mailboxes = new WaitingRoom();
         this.requestIDGenerator = new AtomicLong(0);
         this.failedTunnels = new LinkedList<Tunnel>();
         this.socketFactory = socketFactory;
         this.timer = new Timer("PAMR: Heartbeat timer");
         this.agentID = agentId; // Check the agentId number
         this.magicCookie = magicCookie;
         this.routerID = RouterImpl.DEFAULT_ROUTER_ID;
 
         try {
             Constructor<? extends MessageHandler> mhConstructor;
             mhConstructor = messageHandlerClass.getConstructor(Agent.class);
             this.messageHandler = mhConstructor.newInstance(this);
         } catch (Exception e) {
             throw new ProActiveException("Message routing agent failed to create the message handler", e);
         }
 
         // Avoid lazy connection to spot invalid host/port ASAP
 
         if (this.geTunnelOrReconnect(1) == null) {
             logger.info("Failed to create the PAMR tunnel to " + routerAddr + ":" + routerPort +
                 ". PAMR will probably not work");
         }
 
         // Start the message receiver even if connection failed
         // Message reader will try to open the tunnel later
         Thread mrThread = new Thread(new MessageReader(this));
         mrThread.setDaemon(true);
         mrThread.setName("Message routing: message reader for agent " + this.agentID);
         mrThread.start();
 
         MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
         ObjectName name = null;
         try {
             // Uniquely identify the MBeans and register them with the platform MBeanServer 
             name = new ObjectName("org.objectweb.proactive.extra.messagerouting:type=AgentImpl,name=" +
                 this.agentID);
             mbs.registerMBean(this, name);
         } catch (Exception e) {
             logger.warn("Failed to register a JMX MBean for agent " + this.agentID);
         }
     }
 
     /**
      * Get the current tunnel
      * 
      * @return the current tunnel or null is a tunnel cannot be open
      */
     private Tunnel getTunnel() {
         return this.t;
     }
 
     synchronized private Tunnel geTunnelOrReconnect(int nbTry) {
         if (this.t != null)
             return this.t;
 
         int delay = 2000;
         int subtry = 0;
 
         while (this.t == null && nbTry > 0) {
 
             this.t = this.__reconnectToRouter();
             nbTry--;
 
             if (this.t == null) {
                 subtry = ++subtry % 3;
                 if (subtry == 0) {
                     if (delay < 1000 * 60) {
                         delay *= 2;
                     }
                 }
 
                 logger.warn("PAMR Router is unreachable. Will try to estalish a new tunnel in " +
                     (delay / 1000) + " seconds");
                 new Sleeper(delay).sleep();
             }
         }
 
         return this.t;
     }
 
     /**
      * Returns a new tunnel to the router
      * 
      * The tunnel instance field is updated. The agentID instance field is set
      * on the first call. If the agent cannot reconnect to the router, this.t is
      * set to null.
      * 
      * <b>This method must only be called by getTunnel</b>
      */
     private Tunnel __reconnectToRouter() {
         Tunnel t = null;
         try {
             Socket s = socketFactory.createSocket(this.routerAddr.getHostAddress(), this.routerPort);
             Tunnel tunnel = new Tunnel(s);
 
             // start router handshake
             try {
                 routerHandshake(tunnel);
                 t = tunnel;
             } catch (RouterHandshakeException e) {
                 logger.warn(
                         "Failed to reconnect to the router: the router handshake procedure failed. Reason: " +
                             e.getMessage(), e);
                 tunnel.shutdown();
             }
         } catch (IOException exception) {
             logger.debug("Failed to reconnect to the router", exception);
         }
 
         return t;
     }
 
     /**
      * This is the initial handshake process between the {@link Agent} and the {@link Router}
      * <ul>
      * 	<li> The Agent will send a {@link MessageType#REGISTRATION_REQUEST} to the Router
      *  <li> On first connection, the {@link RegistrationMessage.Field#AGENT_ID} is set to -1 and the
      *  	{@link RegistrationMessage.Field#ROUTER_ID} field is set to zero. It is the responsibility of the router to fill them.</li>
      *  <li> The Router will reply with a {@link MessageType#REGISTRATION_REPLY} message</li>
      *  <li> On first connection, the Agent initializes its {@link RegistrationMessage.Field#AGENT_ID} and {@link RegistrationMessage.Field#ROUTER_ID}
      *  	fields according to the Router reply </li>
      *  <li> For subsequent reconnections, the Agent verifies that its {@link RegistrationMessage.Field#AGENT_ID} and {@link RegistrationMessage.Field#ROUTER_ID} fields
      *  	match the ones sent by the Router in the {@link MessageType#REGISTRATION_REPLY} message.</li>
      * </ul>
      * @throws IOException
      */
     private void routerHandshake(Tunnel tunnel) throws RouterHandshakeException, IOException {
         try {
             // if call for the first time then agentID is null
             RegistrationRequestMessage reg = new RegistrationRequestMessage(this.agentID, requestIDGenerator
                     .getAndIncrement(), routerID, this.magicCookie);
             tunnel.write(reg.toByteArray());
 
             // Waiting the router response. The router has 10 seconds to respond
             tunnel.setSoTimeout(10 * 1000);
             byte[] reply = tunnel.readMessage();
             Message replyMsg = Message.constructMessage(reply, 0);
 
             if (!(replyMsg instanceof RegistrationReplyMessage)) {
                 if (replyMsg instanceof ErrorMessage) {
                     ErrorMessage em = (ErrorMessage) replyMsg;
                     switch (em.getErrorType()) {
                         case ERR_INVALID_ROUTER_ID:
                             throw new RouterHandshakeException(
                                 "Router ID does not match. The router has probably been restarted. Disconnecting...");
                         case ERR_MALFORMED_MESSAGE:
                             throw new RouterHandshakeException(
                                 "The router received a corrupted version of the original message.");
                         case ERR_INVALID_AGENT_ID:
                             if (this.agentID.isReserved()) {
                                 throw new RouterHandshakeException(
                                     "Cannot register to the router, invalid agent id: " + this.agentID +
                                         ". This reserved ID has not been configured on the router, check your configuration");
                             } else {
                                 throw new RouterHandshakeException(
                                     "Cannot register to the router, invalid agent id: " + this.agentID);
                             }
                         case ERR_WRONG_MAGIC_COOKIE:
                             throw new RouterHandshakeException(
                                 "Cannot register to the router, invalid magic cookie");
                         default:
                             break;
                     }
                     if (em.getErrorType() == ErrorType.ERR_INVALID_ROUTER_ID) {
                         throw new RouterHandshakeException("The router has been restarted. Disconnecting...");
                     } else if (em.getErrorType() == ErrorType.ERR_MALFORMED_MESSAGE) {
                         throw new RouterHandshakeException(
                             "The router received a corrupted version of the original message.");
                     }
                 } else {
                     throw new RouterHandshakeException("Invalid router response: expected a " +
                         MessageType.REGISTRATION_REPLY.toString() + " message but got " +
                         replyMsg.getType().toString() + " message");
                 }
             }
 
             RegistrationReplyMessage rrm = (RegistrationReplyMessage) replyMsg;
             AgentID replyAgentID = rrm.getAgentID();
             if (this.agentID == null) {
                 this.agentID = replyAgentID;
                 logger.debug("Router assigned agentID=" + this.agentID + " to this client");
             } else {
                 if (!this.agentID.equals(replyAgentID)) {
                     throw new RouterHandshakeException("Invalid router response: Local ID is " +
                         this.agentID + " but server told " + replyAgentID);
                 }
             }
 
             if (this.routerID == Long.MIN_VALUE) {
                 this.routerID = rrm.getRouterID();
             } else if (this.routerID != rrm.getRouterID()) {
                 throw new RouterHandshakeException("Invalid router response: previous router ID  was " +
                     this.agentID + " but server now advertises " + rrm.getRouterID());
             }
 
             int hb = rrm.getHeartbeatPeriod();
             if (hb > 0) {
                 tunnel.setSoTimeout(hb);
 
                 // Cancel the task in case of heartbeat period change
                 if (this.heartbeatTask != null) {
                     this.heartbeatTask.cancel();
                     this.heartbeatTask = null;
                 }
 
                 // Reschedule the task
                 this.heartbeatTask = new HeartbeatTask();
                 this.timer.schedule(this.heartbeatTask, hb / 3, hb / 3);
             }
         } catch (MalformedMessageException e) {
             throw new RouterHandshakeException("Invalid router response: corrupted " +
                 MessageType.REGISTRATION_REPLY.toString() + " message - " + e.getMessage());
         }
     }
 
     private class RouterHandshakeException extends Exception {
 
         public RouterHandshakeException() {
             super();
         }
 
         public RouterHandshakeException(String msg) {
             super(msg);
         }
     }
 
     /**
      * Reports a tunnel failure to the agent
      * 
      * Threads get from the local agent a tunnel to use. If this tunnel fails
      * then they have to notify this failure to the local agent.
      * 
      * Since several threads can encounter and report the same failure this
      * method checks if the error has already been fixed.
      * 
      * <b>This method must only be called by getNewTunnel</b>
      * 
      * @param brokenTunnel
      *            the tunnel that threw an IOException
      */
     synchronized private void reportTunnelFailure(Tunnel brokenTunnel) {
         if (brokenTunnel == null)
             return;
 
         if (!this.failedTunnels.contains(brokenTunnel)) {
             this.failedTunnels.add(brokenTunnel);
 
             this.mailboxes.unlockDueToTunnelFailure();
             this.t.shutdown();
             this.t = null;
         }
 
     }
 
     /**
      * @return The agent Id of this VM or null the agent has never been able to connect to the router
      */
     public AgentID getAgentID() {
         return agentID;
     }
 
     public byte[] sendMsg(URI targetURI, byte[] data, boolean oneWay) throws MessageRoutingException {
         String remoteAgentId = targetURI.getHost();
         AgentID agentID = new AgentID(Long.parseLong(remoteAgentId));
 
         return sendMsg(agentID, data, oneWay);
     }
 
     public byte[] sendMsg(AgentID targetID, byte[] data, boolean oneWay) throws MessageRoutingException {
         if (logger.isDebugEnabled()) {
             logger.debug("Sending a message to agentId=" + targetID);
         }
 
         // Generate a requestID
         Long requestID = requestIDGenerator.getAndIncrement();
         DataRequestMessage msg = new DataRequestMessage(agentID, targetID, requestID, data);
 
         byte[] response = null;
         if (oneWay) { // No response needed, just send it.
             internalSendMsg(msg);
         } else {
 
             Patient mb = mailboxes.enter(targetID, requestID);
             internalSendMsg(msg);
 
             // block until the result arrives
             try {
                 response = mb.waitForResponse(0);
             } catch (TimeoutException e) {
                 throw new MessageRoutingException("Timeout reached ", e);
             }
         }
 
         return response;
     }
 
     public void sendReply(DataRequestMessage request, byte[] data) throws MessageRoutingException {
         DataReplyMessage reply = new DataReplyMessage(this.getAgentID(), request.getSender(), request
                 .getMessageID(), data);
 
         internalSendMsg(reply);
     }
 
     /**
      * Apply each valve to the message and write it into the tunnel
      * 
      * This method throws a {@link MessageRoutingException} if:
      * <ol>
      * <li>The tunnel fails and cannot be recreated</li>
      * <li>The tunnel fails, can be recreated but the second tunnel fails too</li>
      * </ol>
      * 
      * @param msg
      *            The message to be sent
      * @exception MessageRoutingException
      *                if the message cannot be sent
      */
     protected void internalSendMsg(Message msg) throws MessageRoutingException {
         for (Valve valve : this.valves) {
             msg = valve.invokeOutgoing(msg);
             if (logger.isTraceEnabled()) {
                 logger
                         .trace("Applied valve " + valve.getInfo() + ", resulting message is: " +
                             msg.toString());
             }
         }
 
         // Serialize the message
         byte[] msgBuf = msg.toByteArray();
 
         // this.t can change at any time, get a ref on it
         Tunnel tunnel = this.getTunnel();
         if (tunnel != null) {
             try {
                 tunnel.write(msgBuf);
                 if (logger.isTraceEnabled()) {
                     logger.trace("Sent message " + msg);
                 }
             } catch (IOException e) {
                 // Fail fast
                 this.reportTunnelFailure(tunnel);
                 throw new MessageRoutingException("Failed to send a message using the tunnel " + tunnel, e);
 
             }
         } else {
             throw new MessageRoutingException("Agent is not connected to the router");
         }
     }
 
     /** All the clients waiting for a response
      * 
      * Add thread sending a message is a "patient". It will wait in the waiting room
      * until the response is available.
      * 
      * Patient must be created by using
      * {@link WaitingRoom#enter(AgentID, long)} and not by calling its
      * constructor.
      * 
      * It allows to group mailboxes by recipient. When a remote client crash or
      * disconnect, the agent must unblock all the threads waiting for a response
      * from this remote client.
      */
     private class WaitingRoom {
         final private Map<AgentID, Map<Long, Patient>> byRemoteAgent;
 
         /** Must be hold for each addition or removal */
         final private Object lock = new Object();
 
         private WaitingRoom() {
             this.byRemoteAgent = new HashMap<AgentID, Map<Long, Patient>>();
         }
 
         /** Add a new patient into the waiting room
          * 
          * @param remoteAgentId
          *            Message recipient
          * @param messageId
          *            Message ID
          * @return a newly created mailbox
          */
         private Patient enter(AgentID remoteAgentId, long messageId) {
             Patient mb = new Patient(remoteAgentId, messageId);
             synchronized (this.lock) {
                 Map<Long, Patient> byMessageId;
                 byMessageId = this.byRemoteAgent.get(remoteAgentId);
                 if (byMessageId == null) {
                     byMessageId = new HashMap<Long, Patient>();
                     this.byRemoteAgent.put(remoteAgentId, byMessageId);
                 }
                 byMessageId.put(messageId, mb);
             }
 
             return mb;
         }
 
         /**
          * Unblock all the threads waiting for a response from a given remote
          * agent
          * 
          * @param agentID
          *            the remote Agent ID
          */
         private void unlockDueToRemoteAgentDisconnection(AgentID agentID) {
             synchronized (this.lock) {
                 MessageRoutingException e = new MessageRoutingException("Remote agent disconnected");
 
                 Map<Long, Patient> map = this.byRemoteAgent.get(agentID);
                 if (map != null) {
                     for (Patient patient : map.values()) {
                         if (logger.isTraceEnabled()) {
                             logger.trace("Unlocked request " + patient.requestID + " because remote agent" +
                                 patient.recipient + " disconnected");
                         }
                         patient.setAndUnlock(e);
                     }
                 }
             }
         }
 
         /**
          * Unblock all the thread waiting for a response.
          */
         private void unlockDueToTunnelFailure() {
             synchronized (this.lock) {
                 MessageRoutingException e = new MessageRoutingException("Tunnel failure");
 
                 for (Map<Long, Patient> m : this.byRemoteAgent.values()) {
                     for (Patient p : m.values()) {
                         p.setAndUnlock(e);
                     }
                 }
             }
         }
 
         /**
          * Unblock the Patient waiting on a particular messageID
          * @param agentId
          */
         private Patient unlockDueToCorruption(Long messageId) {
             AgentID agent = null;
             for (Map.Entry<AgentID, Map<Long, Patient>> entry : this.byRemoteAgent.entrySet()) {
                 if (entry.getValue().containsKey(messageId)) {
                     agent = entry.getKey();
                     break;
                 }
             }
             if (agent == null)
                 return null;
             return remove(agent, messageId);
         }
 
         /** Remove a patient on response arrival */
         private Patient remove(AgentID agentId, long messageId) {
             Patient patient = null;
             synchronized (this.lock) {
                 Map<Long, Patient> map;
                 map = this.byRemoteAgent.get(agentId);
                 if (map != null) {
                     patient = map.remove(messageId);
                 }
             }
 
             return patient;
         }
 
         private String[] getBlockedCallers() {
             List<String> ret = new LinkedList<String>();
 
             synchronized (this.lock) {
                 for (AgentID recipient : this.byRemoteAgent.keySet()) {
                     Map<Long, Patient> m = this.byRemoteAgent.get(recipient);
                     for (Long messageId : m.keySet()) {
                         ret.add("recipient: " + recipient + " messageId: " + messageId);
                     }
                 }
             }
 
             return ret.toArray(new String[0]);
         }
     }
 
     /** Allows threads to wait for a response */
     private class Patient {
         /** 0 when the response is available or an error has been received */
         final private SweetCountDownLatch latch;
         /** The response */
         volatile private byte[] response = null;
         /** Received exception */
         volatile private MessageRoutingException exception = null;
 
         /** message ID of the request */
         final private long requestID;
         /** Agent ID of recipient of the request */
         final private AgentID recipient;
 
         private Patient(AgentID agentId, long recipient) {
             this.latch = new SweetCountDownLatch(1);
 
             this.requestID = recipient;
             this.recipient = agentId;
         }
 
         /**
          * Wait until the response is available or an error is received
          * 
          * @param timeout
          *            Maximum amount of time to wait before throwing an
          *            exception in milliseconds. 0 means no timeout
          * @return the response
          * @throws MessageRoutingException
          *             If the request failed to be send or if the recipient
          *             disconnected before sending the response.
          * @throws TimeoutException
          *             If the timeout is reached
          */
         private byte[] waitForResponse(long timeout) throws MessageRoutingException, TimeoutException {
 
             if (timeout == 0) {
                 this.latch.await();
             } else {
                 boolean b = this.latch.await(timeout, TimeUnit.MILLISECONDS);
 
                 if (!b) {
                     throw new TimeoutException("Timeout reached");
                 }
             }
 
             if (exception != null) {
                 throw exception;
             }
 
             return response;
         }
 
         /**
          * Set the response and unlock the waiting thread
          * 
          * @param response
          *            the response
          */
         private void setAndUnlock(byte[] response) {
             this.response = response;
             latch.countDown();
         }
 
         /**
          * Set the exception and unlock the waiting thread
          * 
          * @param exception
          *            received error
          */
         public void setAndUnlock(MessageRoutingException exception) {
             this.exception = exception;
             latch.countDown();
         }
     }
 
     class HeartbeatTask extends TimerTask {
         long heartbeatId;
         volatile boolean stop;
 
         public HeartbeatTask() {
             this.stop = false;
             this.heartbeatId = 0;
         }
 
         public void run() {
             try {
                 Tunnel t = getTunnel();
                 if (t != null) {
                     HeartbeatMessage msg = new HeartbeatClientMessage(heartbeatId++, getAgentID());
                     t.write(msg.toByteArray());
                 } else {
                     logger.debug("Agent is not connected, heartbeat not sent");
                 }
             } catch (IOException e) {
                 logger.debug("Failed to send heartbeat to the router", e);
                 reportTunnelFailure(t);
             }
         }
 
     }
 
     /** Read incoming messages from the tunnel */
     class MessageReader implements Runnable {
         /** The local Agent */
         final AgentImpl agent;
 
         public MessageReader(AgentImpl agent) {
             this.agent = agent;
         }
 
         public void run() {
             while (true) {
                 Message msg = readMessage();
 
                 for (Valve valve : valves) {
                     msg = valve.invokeIncoming(msg);
                     if (logger.isTraceEnabled()) {
                         logger.trace("Applied valve " + valve.getInfo() + ", resulting message is: " +
                             msg.toString());
                     }
                 }
 
                 handleMessage(msg);
             }
         }
 
         /**
          * Block until a message is received
          * 
          * Also in charge of handling tunnel failures
          * 
          * @return the received message
          */
         public Message readMessage() {
 
             while (true) {
                 Tunnel tunnel = geTunnelOrReconnect(Integer.MAX_VALUE);
                 try {
                     // Blocking call
                     byte[] msgBuf = tunnel.readMessage();
                     return Message.constructMessage(msgBuf, 0);
                 } catch (MalformedMessageException e) {
                     // TODO : Send an ERR_ ?
                     logger.error("Dropping the message received from the router, reason:" + e.getMessage());
                 } catch (IOException e) {
                     logger
                             .debug(
                                     "PAMR Connection lost (while waiting for a message). A new connection will be established shortly",
                                     e);
                     reportTunnelFailure(tunnel);
                 }
             }
         }
 
         private void handleMessage(Message msg) {
             switch (msg.getType()) {
                 case DATA_REPLY:
                     DataReplyMessage reply = (DataReplyMessage) msg;
                     handleDataReply(reply);
                     break;
                 case DATA_REQUEST:
                     DataRequestMessage request = (DataRequestMessage) msg;
                     handleDataRequest(request);
                     break;
                 case ERR_:
                     ErrorMessage error = (ErrorMessage) msg;
                     handleError(error);
                     break;
                 case HEARTBEAT_ROUTER:
                     // Nothing to do. Heartbeat are only used to be able to set a soTimeout on the tunnel
                     if (logger.isDebugEnabled()) {
                         logger.debug("Heartbeat #" + ((HeartbeatMessage) msg).getHeartbeatId() + " received");
                     }
                     break;
                 default:
                     // Bad message type. Log it.
                     logger.error("Invalid Message received, wrong type: " + msg);
             }
         }
 
         private void handleError(ErrorMessage error) {
             long messageId = error.getMessageID();
             switch (error.getErrorType()) {
                 case ERR_DISCONNECTION_BROADCAST:
                     /*
                      * An agent disconnected. To avoid blocked thread we have to
                      * unlock all thread that are waiting a response from this agent
                      */
                     mailboxes.unlockDueToRemoteAgentDisconnection(error.getSender());
                     break;
                 case ERR_NOT_CONNECTED_RCPT:
                     /*
                      * The recipient of a given message is not connected to the
                      * router Unlock the sender
                      */
                {
                     AgentID sender = error.getSender();
 
                     Patient mbox = mailboxes.remove(sender, messageId);
                     if (mbox == null) {
                         logger.error("Received error for an unknown request: " + error);
                     } else {
                         if (logger.isTraceEnabled()) {
                             logger.trace("Unlocled " + mbox + " because of a non connected recipient");
                         }
 
                         // this is a reply containing data
                         mbox.setAndUnlock(new MessageRoutingException("Recipient not connected " + sender));
                     }
                }
                    break;
                case ERR_UNKNOW_RCPT:
                    /*
                     * The recipient of a given message is unknown of the router
                     * Unlock the sender
                     */
                {
                    AgentID sender = error.getSender();

                    Patient mbox = mailboxes.remove(sender, messageId);
                    if (mbox == null) {
                        logger.error("Received error for an unknown request: " + error);
                    } else {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Unlocled " + mbox + " because of a unknown recipient");
                        }

                        // this is a reply containing data
                        mbox.setAndUnlock(new MessageRoutingException("Recipient unknown & not connected " +
                            sender));
                    }
                }
                     break;
                 case ERR_MALFORMED_MESSAGE:
                     // do we have the faulty AgentID?
                     AgentID faulty = error.getFaulty();
                     Patient patient;
                     if (faulty != null) {
                         patient = mailboxes.remove(faulty, messageId);
                     } else {
                         // harder without the faulty agent id
                         patient = mailboxes.unlockDueToCorruption(messageId);
                     }
                     if (patient == null) {
                         if (logger.isTraceEnabled()) {
                             logger
                                     .trace("The router got a corrupted version of message with ID " +
                                         messageId);
                         }
                     } else {
                         if (logger.isTraceEnabled()) {
                             logger.trace("Unlocked " + patient + " due to corruption of message with ID " +
                                 messageId + " on the router side");
                         }
 
                         patient
                                 .setAndUnlock(new MessageRoutingException("Message corruption on router side"));
                     }
                     break;
                 default:
                     logger.warn("Unexpected error received by agent from the router: " + error);
                     break;
             }
         }
 
         private void handleDataReply(DataReplyMessage reply) {
             Patient mbox = mailboxes.remove(reply.getSender(), reply.getMessageID());
             if (mbox == null) {
                 logger.error("Received reply for an unknown request: " + reply);
             } else {
                 if (logger.isTraceEnabled()) {
                     logger.trace("Received reply: " + reply);
                 }
                 mbox.setAndUnlock(reply.getData());
             }
         }
 
         private void handleDataRequest(DataRequestMessage request) {
             messageHandler.pushMessage(request);
         }
 
     }
 
     /* @@@@@@@@@@@@@@@@@@@@@@@@@@@ MBean @@@@@@@@@@@@@@@@@@@@@@@@@@@@@ */
 
     public String getLocalAddress() {
         String ret = "unknown";
 
         Tunnel t = this.getTunnel();
         if (t != null) {
             ret = t.getLocalAddress();
         }
 
         return ret;
 
     }
 
     public int getLocalPort() {
         int ret = -1;
 
         Tunnel t = this.getTunnel();
         if (t != null) {
             ret = t.getLocalPort();
         }
 
         return ret;
     }
 
     public String getRemoteAddress() {
         String ret = "unknown";
 
         Tunnel t = this.getTunnel();
         if (t != null) {
             ret = t.getRemoteAddress();
         }
 
         return ret;
     }
 
     public int getRemotePort() {
         int ret = -1;
 
         Tunnel t = this.getTunnel();
         if (t != null) {
             ret = t.getRemotePort();
         }
 
         return ret;
     }
 
     public long getLocalAgentID() {
         return this.agentID.getId();
     }
 
     public String[] getMailboxes() {
         return this.mailboxes.getBlockedCallers();
 
     }
 }
