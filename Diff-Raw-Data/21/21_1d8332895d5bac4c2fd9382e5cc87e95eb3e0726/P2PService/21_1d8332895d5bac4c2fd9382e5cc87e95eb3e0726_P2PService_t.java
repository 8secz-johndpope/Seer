 /*
  * ################################################################
  *
  * ProActive: The Java(TM) library for Parallel, Distributed,
  *            Concurrent computing with Security and Mobility
  *
  * Copyright (C) 1997-2002 INRIA/University of Nice-Sophia Antipolis
  * Contact: proactive-support@inria.fr
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
  * USA
  *
  *  Initial developer(s):               The ProActive Team
  *                        http://www.inria.fr/oasis/ProActive/contacts.html
  *  Contributor(s):
  *
  * ################################################################
  */
 package org.objectweb.proactive.p2p.service;
 
 import org.apache.log4j.Logger;
 
 import org.objectweb.proactive.ActiveObjectCreationException;
 import org.objectweb.proactive.Body;
 import org.objectweb.proactive.InitActive;
 import org.objectweb.proactive.ProActive;
 import org.objectweb.proactive.core.ProActiveException;
 import org.objectweb.proactive.core.group.ExceptionList;
 import org.objectweb.proactive.core.node.Node;
 import org.objectweb.proactive.core.node.NodeException;
 import org.objectweb.proactive.core.node.NodeFactory;
 import org.objectweb.proactive.core.util.log.Loggers;
 import org.objectweb.proactive.core.util.log.ProActiveLogger;
 import org.objectweb.proactive.p2p.service.exception.P2POldMessageException;
 import org.objectweb.proactive.p2p.service.node.P2PNode;
 import org.objectweb.proactive.p2p.service.node.P2PNodeLookup;
 import org.objectweb.proactive.p2p.service.node.P2PNodeManager;
 import org.objectweb.proactive.p2p.service.util.P2PConstants;
 import org.objectweb.proactive.p2p.service.util.UniversalUniqueID;
 
 import java.io.IOException;
 import java.io.Serializable;
 
 import java.util.Random;
 import java.util.Vector;
 
 
 /**
  * <p>ProActive Peer-to-Peer Service.</p>
  * <p>This class is made to be actived.</p>
  *
  * @author Alexandre di Costanzo
  *
  */
 public class P2PService implements InitActive, P2PConstants, Serializable {
 
     /** Logger. */
     private static final Logger logger = ProActiveLogger.getLogger(Loggers.P2P_SERVICE);
 
     /** ProActive Group of acquaintances. **/
     private P2PService acquaintances;
 
     /**
      * ProActive Group representing <code>acquaintances</code>.
      */
     private P2PAcquaintanceManager acquaintanceManager;
 
     /**
      * Reference to the current Node.
      */
     private Node p2pServiceNode = null;
     private static final int MSG_MEMORY = Integer.parseInt(System.getProperty(
                 P2PConstants.PROPERTY_MSG_MEMORY));
     private static final int NOA = Integer.parseInt(System.getProperty(
                 P2PConstants.PROPERTY_NOA));
     private static final int EXPL_MSG = Integer.parseInt(System.getProperty(
                 P2PConstants.PROPERTY_EXPLORING_MSG)) - 1;
 
     /**
      * Randomizer uses in <code>shouldBeAcquaintance</code> method.
      */
     private static final Random randomizer = new Random();
 
     /**
      * Sequence number list of received messages.
      */
     private Vector oldMessageList = new Vector(MSG_MEMORY);
     private P2PNodeManager nodeManager = null;
 
     /**
      * A collection of not full <code>P2PNodeLookup</code>.
      */
     private Vector waitingNodesLookup = new Vector();
 
     //--------------------------------------------------------------------------
     // Class Constructors
     //--------------------------------------------------------------------------
 
     /**
      * The empty constructor.
      *
      * @see org.objectweb.proactive.ProActive
      */
     public P2PService() {
         // empty
     }
 
     //--------------------------------------------------------------------------
     // Public Class methods
     // -------------------------------------------------------------------------
 
     /**
      * Contact all specified peers to enter in the existing P2P network.
      * @param peers a list of peers URL.
      */
     public void firstContact(Vector peers) {
         // Creating an active P2PFirstContact
         Object[] params = new Object[3];
         params[0] = peers;
         params[1] = this.acquaintanceManager;
         params[2] = ProActive.getStubOnThis();
         try {
             ProActive.newActive(P2PFirstContact.class.getName(), params,
                 this.p2pServiceNode);
         } catch (ActiveObjectCreationException e) {
             logger.warn("Couldn't active P2PFirstContact", e);
         } catch (NodeException e) {
             logger.warn("Couldn't active P2PFirstContact", e);
         }
     }
 
     /**
      * Add the remote P2P service in the local acquaintances group if NOA is
      * not yet reached.
      * @param service the remote P2P service.
      */
     public void register(P2PService service) {
         // if (this.acquaintanceManager.size() < NOA) {
         this.acquaintanceManager.add(service);
         if (logger.isDebugEnabled()) {
             logger.debug("Remote peer localy registered: " +
                 ProActive.getActiveObjectNodeUrl(service));
         }
 
         //}
         // Wake up all node accessor, because new peers are know
         this.wakeUpEveryBody();
     }
 
     /**
      * Just to test if the peer is alive.
      */
     public void heartBeat() {
         logger.debug("Heart-beat message received");
     }
 
     /**
      * <b>Method automaticly forwarded by run activity if needed.</b>
      * <p>Using a random fonction to choose if this peer should be know by the
      * remote peer or not.</p>
      * @param ttl Time to live of the message, in number of hops.
      * @param uuid UUID of the message.
      * @param remoteService The original sender.
      */
     public void exploring(int ttl, UniversalUniqueID uuid,
         P2PService remoteService) {
         if (uuid != null) {
             logger.debug("Exploring message received with #" + uuid);
             ttl--;
         }
 
         boolean broadcast;
         try {
             broadcast = broadcaster(ttl, uuid, remoteService);
         } catch (P2POldMessageException e) {
             return;
         }
         if (broadcast) {
             // Forwarding the message
             if (uuid == null) {
                 logger.debug("Generating uuid for exploring message");
                 uuid = generateUuid();
             }
             try {
                 this.acquaintances.exploring(ttl, uuid, remoteService);
                 logger.debug("Broadcast exploring message with #" + uuid);
             } catch (ExceptionList e) {
                 // nothing to do
             }
         }
 
         // Method code ---------------------------------------------------------
         // This should be register
         if (this.shouldBeAcquaintance(remoteService)) {
             this.register(remoteService);
             remoteService.register((P2PService) ProActive.getStubOnThis());
         }
     }
 
     /**
      * <b>Method automaticly forwarded by run activity if needed.</b>
      * <p>Booking a free node.</p>
      * @param ttl Time to live of the message, in number of hops.
      * @param uuid UUID of the message.
      * @param remoteService The original sender.
      * @param lookup The P2P nodes lookup.
      * @param vnName Virtual node name.
     * @param jobId.
      */
     public void askingNode(int ttl, UniversalUniqueID uuid,
         P2PService remoteService, P2PNodeLookup lookup, String vnName,
         String jobId) {
         boolean broadcast;
         if (uuid != null) {
             logger.debug("AskingNode message received with #" + uuid);
             ttl--;
             try {
                 broadcast = broadcaster(ttl, uuid, remoteService);
             } catch (P2POldMessageException e) {
                 return;
             }
         } else {
             broadcast = true;
         }
 
         // Asking a node to the node manager
         P2PNode askedNode = this.nodeManager.askingNode();
 
         // During asking node, forwards this message
         if (broadcast) {
             // Forwarding the message
             if (uuid == null) {
                 logger.debug("Generating uuid for askingNode message");
                 uuid = generateUuid();
             }
             this.acquaintances.askingNode(ttl, uuid, remoteService, lookup,
                 vnName, jobId);
             logger.debug("Broadcast askingNode message with #" + uuid);
         }
 
         // Asking node available?
         Node nodeAvailable = askedNode.getNode();
         if (nodeAvailable != null) {
             // Setting vnInformation and JobId
             if (vnName != null) {
                 nodeAvailable.setVnName(vnName);
                 try {
                     nodeAvailable.getProActiveRuntime().registerVirtualNode(vnName,
                         true);
                 } catch (ProActiveException e) {
                     logger.warn("Couldn't register " + vnName + " in the PAR", e);
                 }
             }
             if (jobId != null) {
                 nodeAvailable.getNodeInformation().setJobID(jobId);
             }
             logger.info("Giving 1 node to vn: " + vnName);
             lookup.giveNode(nodeAvailable, askedNode.getNodeManager());
         }
     }
 
     /** Put in a <code>P2PNodeLookup</code>, the number of asked nodes.
      * @param numberOfNodes the number of asked nodes.
      * @param vnName Virtual node name.
     * @param jobId of the vn.
      * @return the number of asked nodes.
      */
     public P2PNodeLookup getNodes(int numberOfNodes, String vnName, String jobId) {
         Object[] params = new Object[5];
         params[0] = new Integer(numberOfNodes);
         params[1] = ProActive.getStubOnThis();
         params[2] = this.acquaintanceManager;
         params[3] = vnName;
         params[4] = jobId;
 
         P2PNodeLookup lookup = null;
         try {
             lookup = (P2PNodeLookup) ProActive.newActive(P2PNodeLookup.class.getName(),
                     params, this.p2pServiceNode);
             ProActive.enableAC(lookup);
             this.waitingNodesLookup.add(lookup);
         } catch (ActiveObjectCreationException e) {
             logger.fatal("Couldn't create an active lookup", e);
             return null;
         } catch (NodeException e) {
             logger.fatal("Couldn't connect node to creat", e);
             return null;
         } catch (IOException e) {
             if (logger.isDebugEnabled()) {
                 logger.debug("Couldn't enable AC for a nodes lookup", e);
             }
         }
 
         if (logger.isInfoEnabled()) {
             if (numberOfNodes != MAX_NODE) {
                 logger.info("Asking for" + numberOfNodes + " nodes");
             } else {
                 logger.info("Asking for maxinum nodes");
             }
         }
         return lookup;
     }
 
     /**
      * Put in a <code>P2PNodeLookup</code> all available nodes during all the
      * time where it is actived.
      * @param vnName Virtual node name.
     * @param jobId.
      * @return an active object where nodes are received.
      */
     public P2PNodeLookup getMaximunNodes(String vnName, String jobId) {
         return this.getNodes(P2PConstants.MAX_NODE, vnName, jobId);
     }
 
     /**
        /**
      * Remove a no more waiting nodes accessor.
      * @param accessorToRemove the accessor to remove.
      */
     public void removeWaitingAccessor(P2PNodeLookup accessorToRemove) {
         this.waitingNodesLookup.remove(accessorToRemove);
         logger.debug("Accessor succefuly removed");
     }
 
     /**
      * @return a reference to the local acquaintance manager.
      */
     public P2PAcquaintanceManager getP2PAcquaintanceManager() {
         return this.acquaintanceManager;
     }
 
     // -------------------------------------------------------------------------
     // Private class method
     // -------------------------------------------------------------------------
 
     /**
      * <b>* ONLY FOR INTERNAL USE *</b>
      * @return a random UUID for sending message.
      */
     private UniversalUniqueID generateUuid() {
         UniversalUniqueID uuid = UniversalUniqueID.randomUUID();
         oldMessageList.add(uuid);
         logger.debug(" UUID generated with #" + uuid);
         return uuid;
     }
 
     /**
      * If not an old message and ttl > 1 return true else false.
      * @param ttl TTL of the message.
      * @param uuid UUID of the message.
      * @param remoteService P2PService of the first service.
      * @return tur if you should broadcats, false else.
      */
     private boolean broadcaster(int ttl, UniversalUniqueID uuid,
         P2PService remoteService) throws P2POldMessageException {
         String remoteNodeUrl = ProActive.getActiveObjectNodeUrl(remoteService);
 
         String thisNodeUrl = this.p2pServiceNode.getNodeInformation().getURL();
 
         // is it an old message?
         boolean isAnOldMessage = this.isAnOldMessage(uuid);
         if (!isAnOldMessage && !remoteNodeUrl.equals(thisNodeUrl)) {
             if (ttl > 0) {
                 logger.debug("Forwarding message request");
                 return true;
             }
             return false;
         } else {
             // it is an old message: nothing to do
             // NO REMOVE the isDebugEnabled message
             if (logger.isDebugEnabled()) {
                 if (isAnOldMessage) {
                     logger.debug("Old message request with #" + uuid);
                 } else {
                     logger.debug("The peer is me: " + remoteNodeUrl);
                 }
             }
 
             throw new P2POldMessageException();
         }
     }
 
     /**
      * If number of acquaintances is less than NOA return <code>true</code>, else
      * use random factor.
      * @param remoteService the remote service which is asking acquaintance.
      * @return <code>true</code> if this peer should be an acquaintance, else
      * <code>false</code>.
      */
     private boolean shouldBeAcquaintance(P2PService remoteService) {
         if (this.acquaintanceManager.contains(remoteService)) {
             logger.debug("The remote peer is already known");
             return false;
         }
         if (this.acquaintanceManager.size() < NOA) {
             logger.debug("NOA not reached: I should be an acquaintance");
             return true;
         } else {
             int random = randomizer.nextInt(100);
             if (random < EXPL_MSG) {
                 logger.debug("Random said: I should be an acquaintance");
                 return true;
             } else {
                 logger.debug("Random said: I should not be an acquaintance");
                 return false;
             }
         }
     }
 
     /**
      * If ti's not an old message add teh sequence number in the list.
      * @param uuid the uuid of the message.
      * @return <code>true</code> if it was an old message, <code>false</code> else.
      */
     private boolean isAnOldMessage(UniversalUniqueID uuid) {
         if (uuid == null) {
             return false;
         }
         if (oldMessageList.contains(uuid)) {
             return true;
         } else {
             if (oldMessageList.size() == MSG_MEMORY) {
                 oldMessageList.remove(0);
             }
             oldMessageList.add(uuid);
             return false;
         }
     }
 
     /**
      * Wake up all node lookups.
      */
     private void wakeUpEveryBody() {
         for (int i = 0; i < this.waitingNodesLookup.size(); i++) {
             ((P2PNodeLookup) this.waitingNodesLookup.get(i)).wakeUp();
         }
     }
 
     //--------------------------------------------------------------------------
     // Active Object methods
     //--------------------------------------------------------------------------
 
     /**
      * @see org.objectweb.proactive.InitActive#initActivity(org.objectweb.proactive.Body)
      */
     public void initActivity(Body body) {
         logger.debug("Entering initActivity");
 
         try {
             // Reference to my current p2pServiceNode
             this.p2pServiceNode = NodeFactory.getNode(body.getNodeURL());
         } catch (NodeException e) {
             logger.fatal("Couldn't get reference to the local p2pServiceNode", e);
         }
 
         logger.debug("P2P Service running in p2pServiceNode: " +
             this.p2pServiceNode.getNodeInformation().getURL());
 
         Object[] params = new Object[1];
         params[0] = ProActive.getStubOnThis();
         try {
             // Active acquaintances
             this.acquaintanceManager = (P2PAcquaintanceManager) ProActive.newActive(P2PAcquaintanceManager.class.getName(),
                     params, this.p2pServiceNode);
             logger.debug("P2P acquaintance manager activated");
 
             // Get active group
             this.acquaintances = this.acquaintanceManager.getActiveGroup();
             logger.debug("Got active group reference");
 
             // Active Node Manager
             this.nodeManager = (P2PNodeManager) ProActive.newActive(P2PNodeManager.class.getName(),
                     params, this.p2pServiceNode);
             logger.debug("P2P node manager activated");
         } catch (ActiveObjectCreationException e) {
             logger.fatal("Couldn't create one of managers", e);
         } catch (NodeException e) {
             logger.fatal("Couldn't create one the managers", e);
         }
 
         logger.debug("Exiting initActivity");
     }
 }
