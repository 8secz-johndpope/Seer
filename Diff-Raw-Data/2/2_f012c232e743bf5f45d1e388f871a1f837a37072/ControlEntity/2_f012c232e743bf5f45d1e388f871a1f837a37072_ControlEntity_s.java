 /*******************************************************************************
  * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
  * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
  * 
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html.
  ******************************************************************************/
 package de.tuilmenau.ics.fog.routing.hierarchical.management;
 
 import java.awt.Color;
 import java.util.LinkedList;
 
 import de.tuilmenau.ics.fog.facade.Name;
 import de.tuilmenau.ics.fog.facade.Namespace;
 import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyPriorityUpdate;
 import de.tuilmenau.ics.fog.packets.hierarchical.topology.AnnounceCoordinator;
 import de.tuilmenau.ics.fog.packets.hierarchical.topology.InvalidCoordinator;
 import de.tuilmenau.ics.fog.routing.Route;
 import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
 import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
 import de.tuilmenau.ics.fog.routing.hierarchical.Localization;
 import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
 import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
 import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
 import de.tuilmenau.ics.fog.ui.Decorator;
 import de.tuilmenau.ics.fog.ui.Logging;
 
 /**
  * A control entity can be either a cluster or a coordinator instance.
  * This class is used to concentrate common function of clusters and coordinators
  */
 public abstract class ControlEntity implements AbstractRoutingGraphNode, Localization, Decorator
 {
 	private static final long serialVersionUID = 6770007191316056223L;
 
 	/**
 	 * Stores the hierarchy level of this cluster.
 	 */
 	private HierarchyLevel mHierarchyLevel = null;
 	
 	/**
 	 * Stores the Bully priority of this node for this cluster.
 	 * The value is also used inside the Elector of this cluster.
 	 */
 	protected BullyPriority mBullyPriority = null;
 
 	/**
 	 * The HRM ID of this cluster.
 	 */
 	private HRMID mHRMID = null;
 
 	/**
 	 * Stores a reference to the parent HRMController.
 	 */
 	protected HRMController mHRMController = null;
 
 	/**
 	 * Stores the registered communication channels
 	 */
 	private LinkedList<ComChannel> mComChannels = new LinkedList<ComChannel>();
 
 	/**
 	 * Stores the communication channel to the superior coordinator.
 	 * For a base hierarchy level cluster, this is a level 0 coordinator.
 	 * For a level n coordinator, this is a level n+1 coordinator. 
 	 */
 	private ComChannel mSuperiorCoordinatorComChannel = null;
 
 	/**
 	 * Stores the unique ID of the superior coordinator.
 	 * For a Cluster object, this is the coordinator of the cluster's coordinator.
 	 */
 	private int mSuperiorCoordinatorID = 0;
 	
 	/**
 	 * Stores the name of the node where the superior coordinator is located.
 	 */
 	private Name mSuperiorCoordinatorNodeName = null;
 	
 	/**
 	 * Stores a descriptive string about the superior coordinator
 	 */
 	private String mSuperiorCoordinatorDescription = null;
 
 	/**
 	 * Stores the L2Address of the superior coordinator.
 	 */
 	private L2Address mSuperiorCoordinatorHostL2Address = null;
 	
 	/**
 	 * Counter about how many times a superior coordinator was defined
 	 */
 	private int mSuperiorCoordinatorUpdateCounter = 0;
 
 	/**
 	 * Stores the unique cluster ID
 	 */
 	private Long mClusterID = null;
 
 	/**
 	 * Stores the unique coordinator ID
 	 */
 	private int mCoordinatorID = -1;
 	
 	/**
 	 * Stores the physical simulation machine specific multiplier, which is used to create unique IDs even if multiple physical simulation machines are connected by FoGSiEm instances
 	 * The value "-1" is important for initialization!
 	 */
 	private static int sIDMachineMultiplier = -1;
 
 	/**
 	 * Stores if the role of this entity is still valid
 	 */
 	private boolean mRoleValid = true;
 
 	private static boolean DEBUG_EQUALS = false;
 	/**
 	 * Constructor
 	 */
 	public ControlEntity(HRMController pHRMController, HierarchyLevel pHierarchyLevel)
 	{
 		// initialize the HRMID of the cluster to ".0.0.0"
 		mHRMID = new HRMID(0);
 
 		// the hierarchy level is defined from outside
 		mHierarchyLevel = pHierarchyLevel;
 		
 		// update the reference to the HRMController application for internal use
 		mHRMController = pHRMController;
 
 		// create a new standard Bully priority
 		mBullyPriority = BullyPriority.createForControlEntity(mHRMController, this);
 	}
 
 	/**
 	 * Returns the Bully priority of this node for this cluster
 	 * 
 	 * @return the Bully priority
 	 */
 	public BullyPriority getPriority()
 	{
 		if (mBullyPriority == null){
 			mBullyPriority = BullyPriority.create(this);
 		}
 			
 		return mBullyPriority;
 	}
 
 	/**
 	 * Sets a new Bully priority
 	 * 
 	 * @param pPriority the new Bully priority
 	 */
 	public void setPriority(BullyPriority pPriority)
 	{
 		// store the old one
 		BullyPriority tBullyPriority = mBullyPriority;
 		
 		// update to the new one
 		mBullyPriority = pPriority;
 		
 		Logging.log(this, "ASSIGNED BULLY PRIORITY for cluster " + toString() + " updated from " + (tBullyPriority != null ? tBullyPriority.getValue() : "null") + " to " + (mBullyPriority != null ? mBullyPriority.getValue() : "null"));
 	}
 
 	/**
 	 * Returns the hierarchy level of this cluster
 	 * 
 	 * @return the hierarchy level
 	 */
 	public HierarchyLevel getHierarchyLevel()
 	{
 		return mHierarchyLevel;
 	}
 
 	/**
 	 * Assign new HRMID for being addressable.
 	 *  
 	 * @param pCaller the caller who assigns the new HRMID
 	 * @param pHRMID the new HRMID
 	 */
 	public void setHRMID(Object pCaller, HRMID pHRMID)
 	{
 		HRMID tOldHRMID = getHRMID();
 		
 		if(pHRMID != null){
 			if(!pHRMID.isZero()){
 				Logging.log(this, "ASSINGED HRMID=" + pHRMID + " (old=" + (mHRMID != null ? mHRMID.toString() : "null") + ", assigner=" + pCaller + ")");
 
 				// is this a new HRMID?
 				if(!pHRMID.equals(mHRMID)){
 					// update the HRMID
 					mHRMID = pHRMID.clone();
 					
 					if (this instanceof Cluster){
 						Cluster tCluster = (Cluster)this;
 			
 						// inform HRM controller about the address change
 						mHRMController.updateClusterAddress(tCluster, tOldHRMID);
 			
 						return;
 					}
 					if (this instanceof Coordinator){
 						Coordinator tCoordinator = (Coordinator)this;
 			
 						// inform HRM controller about the address change
 						mHRMController.updateCoordinatorAddress(tCoordinator, tOldHRMID);
 			
 						return;
 					}
 					if (this instanceof CoordinatorAsClusterMember){
 						Coordinator tCoordinator = ((CoordinatorAsClusterMember)this).getCoordinator();
 						
 						// inform HRM controller about the address change
 						mHRMController.updateCoordinatorAddress(tCoordinator, tOldHRMID);
 			
 						return;
 					}
 					if (this instanceof ClusterMember){
 						ClusterMember tClusterMember = (ClusterMember)this;
 			
 						// inform HRM controller about the address change
 						mHRMController.updateClusterMemberAddress(tClusterMember, tOldHRMID);
 						
 						return;
 					}
 				}else{
 					Logging.log(this, "Got the same HRMID assignement again: " + pHRMID);
 				}
 			}else{
 				Logging.log(this, "Got a zero HRMID: " + pHRMID.toString());
 			}
 		}else{
 			mHRMID = null;
 		}
 	}
 
 	/**
 	 * EVENT: role invalid
 	 */
 	protected void eventInvalidation()
 	{
 		Logging.log(this, "================ Invalidation ============== ");
 		mRoleValid = false;
 	}
 	
 	/**
 	 * Returns if the role is still valid
 	 * 
 	 * @return
 	 */
 	protected boolean isThisEntityValid()
 	{
 		return mRoleValid;
 	}
 
 	/**
 	 * Revokes a HRMID for this entity.
 	 *  
 	 * @param pCaller the caller who assigns the new HRMID
 	 * @param pHRMID the revoked HRMID
 	 */
 	public synchronized void eventRevokedHRMID(Object pCaller, HRMID pHRMID)
 	{
 		Logging.log(this, "REVOKING HRMID=" + pHRMID + " (caller=" + pCaller + ")");
 
 		if (pHRMID != null){
 			if(!pHRMID.isZero()){
 				// update the HRMID
				if (mHRMID.equals(pHRMID)){
 					Logging.log(this, "     ..revoking local HRMID: " + mHRMID);
 				
 					if (this instanceof Cluster){
 						Cluster tCluster = (Cluster)this;
 			
 						// inform HRM controller about the address change
 						mHRMController.revokeClusterAddress(tCluster, pHRMID);
 					}else if (this instanceof ClusterMember){
 						ClusterMember tClusterMember = (ClusterMember)this;
 						
 						// inform HRM controller about the address change
 						mHRMController.revokeClusterMemberAddress(tClusterMember, pHRMID);
 					}
 					if (this instanceof Coordinator){
 						Coordinator tCoordinator = (Coordinator)this;
 			
 						// inform HRM controller about the address change
 						mHRMController.revokeCoordinatorAddress(tCoordinator, pHRMID);
 					}
 					if(this instanceof CoordinatorAsClusterMember){
 						Coordinator tCoordinator = ((CoordinatorAsClusterMember)this).getCoordinator();
 			
 						// inform HRM controller about the address change
 						mHRMController.revokeCoordinatorAddress(tCoordinator, pHRMID);
 					}
 					
 					setHRMID(this, null);
 				}else{
 					//throw new RuntimeException(this + "cannot revoke unknown HRMID: " + pHRMID);
 					Logging.err(this, "Cannot revoke unknown HRMID: " + pHRMID);
 				}
 			}else{
 				Logging.log(this, "Got a zero HRMID: " + pHRMID.toString());
 			}
 		}else{
 			Logging.warn(this, "Cannot revoke invalid HRMID");
 		}
 	}
 
 	/**
 	 * Returns the HRMID under which this node is addressable for this cluster
 	 * 
 	 * @return the HRMID
 	 */
 	public HRMID getHRMID() {
 		return mHRMID;
 	}
 	
 	/**
 	 * Returns all register communication channels
 	 * 
 	 * @return the communication channels
 	 */
 	@SuppressWarnings("unchecked")
 	public LinkedList<ComChannel> getComChannels()
 	{
 		LinkedList<ComChannel> tResult = null;
 			
 		synchronized (mComChannels) {
 			tResult = (LinkedList<ComChannel>) mComChannels.clone();
 		}
 		
 		return tResult;
 	}
 
 	/**
 	 * Returns if a comm. channel to the given control entity does exist
 	 * 
 	 * @param pPeer the peer to which a comm. channel is searched
 	 * 
 	 * @return true or false
 	 */
 	protected boolean hasComChannel(ControlEntity pPeer)
 	{
 		boolean tResult = false;
 		
 		for(ComChannel tComChannel : getComChannels()){
 			if(tComChannel.getRemoteClusterName().equals(pPeer)){
 				tResult = true;
 			}
 		}
 		
 		return tResult;
 	}
 	
 	/**
 	 * Registers a communication channel to the internal database
 	 * 
 	 * @param pComChan the communication channel
 	 */
 	public void registerComChannel(ComChannel pComChannel)
 	{
 		synchronized (mComChannels) {
 			if(!mComChannels.contains(pComChannel)) {
 				Logging.log(this, "Registering communication channel " + pComChannel + ", " + mComChannels.size() + " communication channels already registered");
 
 				// add the channel to the database
 				mComChannels.add(pComChannel);
 			}else{
 				Logging.err(this, "Communication channel " + pComChannel + " is alredy known");
 			}			
 		}
 	}
 
 	/**
 	 * Unregisters a communication channel from the internal database
 	 * 
 	 * @param pComChan the communication channel
 	 */
 	public void unregisterComChannel(ComChannel pComChannel)
 	{
 		synchronized (mComChannels) {
 			if(mComChannels.contains(pComChannel)) {
 				Logging.log(this, "Unregistering communication channel " + pComChannel + ", " + mComChannels.size() + " communication channels still registered");
 
 				// add the channel to the database
 				mComChannels.remove(pComChannel);
 			}			
 		}
 
 		// close the communication channel to the peer
 		pComChannel.closeChannel();
 	}
 
 	/**
 	 * Determines all registered neighbors for this control entity, which can be found withing the ARG of the HRMController instance
 	 * 
 	 * @return the found neighbors of the ARG
 	 */
 	public LinkedList<ControlEntity> getNeighborsARG()
 	{
 		LinkedList<ControlEntity> tResult = new LinkedList<ControlEntity>();
 		
 		for(AbstractRoutingGraphNode tNode : mHRMController.getNeighborsARG(this)) {
 			if (tNode instanceof ControlEntity){
 				tResult.add((ControlEntity)tNode);
 			}else{
 				Logging.warn(this, "getNeighborsARG() ignores ARG neighbor: " + tNode);
 			}
 		}
 		
 		return tResult;
 	}
 
 	/**
 	 * EVENT: coordinator announcement
 	 * 
 	 * @param pComChannel the source comm. channel
 	 * @param pAnnounceCoordinator the received announcement
 	 */
 	public synchronized void eventCoordinatorAnnouncement(ComChannel pComChannel, AnnounceCoordinator pAnnounceCoordinator)
 	{
 		Logging.warn(this, "Fired event COORDINATOR_ANNOUNCEMENT: " + pAnnounceCoordinator);
 		Logging.warn(this, "Ignoring COORDINATOR_ANNOUNCEMENT from comm. channel: " + pComChannel);
 	}
 
 	/**
 	 * EVENT: coordinator invalidation
 	 * 
 	 * @param pComChannel the source comm. channel
 	 * @param pInvalidCoordinator the received invalidation
 	 */
 	public synchronized void eventCoordinatorInvalidation(ComChannel pComChannel, InvalidCoordinator pInvalidCoordinator)
 	{
 		Logging.warn(this, "Fired event COORDINATOR_INVALIDATION: " + pInvalidCoordinator);
 		Logging.warn(this, "Ignoring COORDINATOR_INVALIDATION from comm. channel: " + pComChannel);
 	}
 	
 	/**
 	 * EVENT: cluster coordinator is available
 	 * 
 	 * For a coordinator instance, this is its superior coordinator.
 	 * For a cluster instance, this is its local coordinator.
 	 * 
 	 * @param pCoordinatorComChannel the communication channel to the coordinator
 	 * @param pCoordinatorNodeName the name of the node where the coordinator is located
 	 * @param pCoordinatorID the unique ID of the coordinator
 	 * @param pCoordinatorHostL2Address the L2Address of the node where the coordinator is located
 	 * @param pCoordinatorDescription a description of the new coordinator
 	 */
 	public void eventClusterCoordinatorAvailable(ComChannel pCoordinatorComChannel, Name pCoordinatorNodeName, int pCoordinatorID, L2Address pCoordinatorHostL2Address, String pCoordinatorDescription)
 	{
 		Logging.log(this, "EVENT: superior coordinator available (update " + (++mSuperiorCoordinatorUpdateCounter) + ", node=" + pCoordinatorNodeName + ", comm. chann.=" + pCoordinatorComChannel + ", L2Address=" + pCoordinatorHostL2Address + ")");
 
 		// store the communication channel to the superior coordinator
 		setSuperiorCoordinatorComChannel(pCoordinatorComChannel);
 		
 		// store the L2Address of the superior coordinator
 		setSuperiorCoordinatorHostL2Address(pCoordinatorHostL2Address);
 		
 		// store the unique ID of the superior coordinator
 		setSuperiorCoordinatorID(pCoordinatorID);
 		
 		// store the name of the node where the superior coordinator is located
 		setSuperiorCoordinatorNodeName(pCoordinatorNodeName);
 		
 		// store the description about the new superior coordinator
 		setSuperiorCoordinatorDescription(pCoordinatorDescription);
 		
 		if(this instanceof CoordinatorAsClusterMember){
 			Coordinator tCoordinator = ((CoordinatorAsClusterMember)this).getCoordinator();
 			
 			tCoordinator.eventClusterCoordinatorAvailable(pCoordinatorComChannel, pCoordinatorNodeName, pCoordinatorID, pCoordinatorHostL2Address, pCoordinatorDescription);
 		}
 	}
 
 	/**
 	 * Returns the description of the superior coordinator
 	 * 
 	 * @return the description
 	 */
 	public String superiorCoordinatorDescription() {
 		return mSuperiorCoordinatorDescription;
 	}
 	
 	/**
 	 * Sets the new description about the superior coordinator
 	 * 
 	 * @param pDescription the new description
 	 */
 	protected void setSuperiorCoordinatorDescription(String pDescription)
 	{
 		Logging.log(this, "Setting superior coordinator desription: " + pDescription);
 		mSuperiorCoordinatorDescription = pDescription;
 	}
 
 	/**
 	 * Returns the name of the node where the superior coordinator is located
 	 * 
 	 * @return the name of the node
 	 */
 	public Name superiorCoordinatorNodeName() {
 		return mSuperiorCoordinatorNodeName;
 	}
 
 	/**
 	 * Sets the name of the node where the superior coordinator is located.
 	 * 
 	 * @param pNodeName the name of the node
 	 */
 	private void setSuperiorCoordinatorNodeName(Name pNodeName)
 	{
 		//Logging.log(this, "Setting superior coordinator node name: " + pNodeName);
 		mSuperiorCoordinatorNodeName = pNodeName;
 	}
 	
 	/**
 	 * Sets the unique ID of the superior coordinator
 	 * 
 	 * @param pCoordinatorID the unique ID
 	 */
 	protected void setSuperiorCoordinatorID(int pCoordinatorID)
 	{
 		//Logging.log(this, "Setting superior coordinator ID: " + pCoordinatorID);
 		mSuperiorCoordinatorID = pCoordinatorID;
 	}
 
 	/**
 	 * Returns the unique ID of the superior coordinator
 	 * 
 	 * @return the unique ID of the superior coordinator
 	 */
 	public int superiorCoordinatorID()
 	{
 		return mSuperiorCoordinatorID;
 	}
 
 	/**
 	 * Sets the communication channel to the superior coordinator.
 	 * For a base hierarchy level cluster, this is a level 0 coordinator.
 	 * For a level n coordinator, this is a level n+1 coordinator.
 	 *  
 	 * @param pComChannel the new communication channel
 	 */
 	private void setSuperiorCoordinatorComChannel(ComChannel pComChannel)
 	{
 		//Logging.log(this, "Setting superior comm. channel: " + pComChannel);
 		mSuperiorCoordinatorComChannel = pComChannel;
 	}
 	
 	/**
 	 * Returns a reference to the communication channel towards the superior coordinator.
 	 * 
 	 * @return the communication channel
 	 */
 	public ComChannel superiorCoordinatorComChannel()
 	{
 		return mSuperiorCoordinatorComChannel;
 	}
 	
 	/**
 	 * Sets the L2Address of the superior coordinator.
 	 *  
 	 * @param pAddr the new L2Address
 	 */
 	private void setSuperiorCoordinatorHostL2Address(L2Address pAddr)
 	{
 		mSuperiorCoordinatorHostL2Address = pAddr;
 	}
 	
 	/**
 	 * Returns the L2Address of the superior coordinator.
 	 * 
 	 * @return the L2Address
 	 */
 	public L2Address superiorCoordinatorHostL2Address()
 	{
 		return mSuperiorCoordinatorHostL2Address;
 	}
 
 	/**
 	 * Determines the physical simulation machine specific ID multiplier
 	 * 
 	 * @return the generated multiplier
 	 */
 	static protected int idMachineMultiplier()
 	{
 		if (sIDMachineMultiplier < 0){
 			String tHostName = HRMController.getHostName();
 			if (tHostName != null){
 				sIDMachineMultiplier = Math.abs((tHostName.hashCode() % 10000) * 10000);
 			}else{
 				Logging.err(null, "Unable to determine the machine-specific ClusterID multiplier because host name couldn't be indentified");
 			}
 		}
 
 		return sIDMachineMultiplier;
 	}
 
 	/**
 	 * Returns the full ClusterID (including the machine specific multiplier)
 	 * 
 	 *  @return the full ClusterID
 	 */
 	public Long getClusterID()
 	{
 		return mClusterID;
 	}
 	
 	/**
 	 * Sets the cluster ID
 	 * 
 	 * @param pNewClusterID the new cluster ID
 	 */
 	protected void setClusterID(Long pNewClusterID)
 	{
 		mClusterID = pNewClusterID;
 	}
 	
 	/**
 	 * Returns the full CoordinatorID (including the machine specific multiplier)
 	 * 
 	 *  @return the full CoordinatorID
 	 */
 	public int getCoordinatorID()
 	{
 		return mCoordinatorID;
 	}
 	
 	/**
 	 * Sets the cluster ID
 	 * 
 	 * @param pNewCoordinatorID the new cluster ID
 	 */
 	protected void setCoordinatorID(int pNewCoordinatorID)
 	{
 		//Logging.log(this, "Setting coordinator ID: " + pNewCoordinatorID);
 		mCoordinatorID = pNewCoordinatorID;
 	}
 	
 	/**
 	 * Returns the machine-local ClusterID (excluding the machine specific multiplier)
 	 * 
 	 * @return the machine-local ClusterID
 	 */
 	public long getGUIClusterID()
 	{
 		//TODO: bei signalisierten ClusterName-Objekten stimmt hier der Bezug zum richtigen MachineMultiplier nicht
 		if (getClusterID() != null)
 			return getClusterID() / idMachineMultiplier();
 		else
 			return -1;
 	}
 
 	/**
 	 * Returns the machine-local CoordinatorID (excluding the machine specific multiplier)
 	 * 
 	 * @return the machine-local CoordinatorID
 	 */
 	public long getGUICoordinatorID()
 	{
 		//TODO: bei signalisierten ClusterName-Objekten stimmt hier der Bezug zum richtigen MachineMultiplier nicht
 		if (getClusterID() != null)
 			return getCoordinatorID() / idMachineMultiplier();
 		else
 			return -1;
 	}
 
 	/**
 	 * Returns a descriptive string about the cluster
 	 * 
 	 * @return the descriptive string
 	 */
 	public String getClusterDescription()
 	{
 		if (this instanceof Cluster){
 			return toLocation();
 		}
 		if (this instanceof ClusterMember){
 			return toLocation();
 		}
 		if (this instanceof Coordinator){
 			Coordinator tCoordinator = (Coordinator)this;
 			return tCoordinator.getCluster().getClusterDescription();  
 		}
 		return toString();
 	}
 
 	/**
 	 * EVENT: "communication available", triggered by parent comm. session
 	 */
 	public void eventCommunicationAvailable()
 	{
 		Logging.warn(this, "Fired event COMMUNICATION_AVAILABLE");
 		Logging.warn(this, "Ignoring COMMUNICATION_AVAILABLE");
 	}
 
 	/**
 	 * EVENT: new HRMID assigned
      * The function is called when an address update was received.
 	 * 
 	 * @param pHRMID the new HRMID
 	 */
 	public void eventAssignedHRMID(HRMID pHRMID)
 	{
 		if (HRMConfig.DebugOutput.SHOW_DEBUG_ADDRESS_DISTRIBUTION){
 			Logging.log(this, "Handling AssignHRMID with assigned HRMID " + pHRMID.toString());
 		}
 
 		if(pHRMID != null){
 			/**
 			 * Store the new HRMID
 			 */
 			// we process such packets for cluster only on base hierarchy level and on all hierarchy level for coordinators
 			if ((getHierarchyLevel().isBaseLevel()) || (this instanceof Coordinator) || (this instanceof CoordinatorAsClusterMember)){
 				if (HRMConfig.DebugOutput.SHOW_DEBUG_ADDRESS_DISTRIBUTION){
 					Logging.log(this, "     ..setting assigned HRMID " + pHRMID.toString());
 				}
 				
 				// update the local HRMID
 				setHRMID(this, pHRMID);
 			}else{
 				Logging.warn(this, "     ..ignoring assigned HRMID " + pHRMID + " at hierachy level " + getHierarchyLevel().getValue());
 			}
 		}
 	}
 
 	/**
 	 * Store the announced coordinator within the local ARG
 	 * 
 	 * @param pSourceEntity the source of the announcement (corresponds to the next hop towards the announcer)
 	 * @param pAnnounceCoordinator the announcement
 	 */
 	protected void registerAnnouncedCoordinatorARG(ControlEntity pSourceEntity, AnnounceCoordinator pAnnounceCoordinator)
 	{
 		// check if the "remote" coordinator isn't stored at this physical node
 		if(!pAnnounceCoordinator.getSenderClusterCoordinatorNodeL2Address().equals(mHRMController.getNodeL2Address())){
 			ClusterName tRemoteClusterName = pAnnounceCoordinator.getSenderClusterName();
 			if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_ANNOUNCEMENT_PACKETS){
 				Logging.log(this, "Registering ANNOUNCED REMOTE COORDINATOR: " + tRemoteClusterName);
 				if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_ANNOUNCEMENT_PACKETS_ROUTE){
 					Logging.log(this, "     ..announcement took the following route: " + pAnnounceCoordinator.getSourceRoute());
 				}
 			}
 
 			/**
 			 * Storing the ARG node for this announced remote coordinator
 			 */
 			// search for an already existing CoordintorProxy instance
 			boolean tNewCoordinatorProxy = false;
 			CoordinatorProxy tCoordinatorProxy = mHRMController.getCoordinatorProxyByName(tRemoteClusterName);
 			if(tCoordinatorProxy == null){
 				tCoordinatorProxy = CoordinatorProxy.create(mHRMController, tRemoteClusterName, pAnnounceCoordinator.getSenderClusterCoordinatorNodeL2Address(), pAnnounceCoordinator.getRouteHopCount());
 				tNewCoordinatorProxy = true;
 			}else{
 				if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_ANNOUNCEMENT_PACKETS){
 					// did we receive a coordinator announcement from our own coordinator?
 					if(!equals(tRemoteClusterName)){
 						Logging.log(this, "     ..already known remote coordinator: " + tRemoteClusterName);
 					}else{
 						Logging.log(this, "     ..ignoring announcement of own remote coordinator: " + tRemoteClusterName);
 					}
 				}
 			}
 			
 			/**
 			 * Storing the route to the announced remote coordinator
 			 * HINT: we provide a minimum hop count for the routing
 			 */
 			
 			/**
 			 * Update an already existing link
 			 */ 
 			AbstractRoutingGraphLink tLink = mHRMController.getLinkARG(pSourceEntity, tCoordinatorProxy);
 			if(tLink != null){
 				Route tOldLinkRoute = tLink.getRoute();
 				Route tNewLinkRoute = pAnnounceCoordinator.getRoute();
 				
 				// does a route exist for the stored link?
 				if(tOldLinkRoute != null){
 					// is the new route shorter than the old one?
 					if(tNewLinkRoute.isShorter(tOldLinkRoute)){
 						// replace the stored route by the new route which is shorter than the old one
 						//Logging.log(this, "Updating ARG link: " + tLink);
 						//Logging.log(this, "          ..new route: " + tNewLinkRoute);
 						tLink.setRoute(tNewLinkRoute);
 						
 						// update L2 link (update is automatically done in registerLinkL2() )
 						mHRMController.registerLinkL2(tCoordinatorProxy.getCoordinatorNodeL2Address(), tNewLinkRoute);
 
 						tCoordinatorProxy.setDistance(pAnnounceCoordinator.getRouteHopCount());
 					}
 				}
 			}
 			/**
 			 * Do we have a new CoordinatorProxy?
 			 */ 
 			if(tNewCoordinatorProxy){
 				AbstractRoutingGraphLink tNewLink = new AbstractRoutingGraphLink(pAnnounceCoordinator.getRoute());
 				
 				Logging.log(this, "Registering new ARG link: " + tNewLink);
 
 				// register the link to the announced coordinator
 				mHRMController.registerLinkARG(pSourceEntity, tCoordinatorProxy, tNewLink);
 				
 				// register L2 link
 				mHRMController.registerLinkL2(tCoordinatorProxy.getCoordinatorNodeL2Address(), pAnnounceCoordinator.getRoute());
 				
 				// update the hop distance of the route to the coordinator node
 				tCoordinatorProxy.setDistance(pAnnounceCoordinator.getRouteHopCount());
 
 				/**
 				 * Trigger: restart clustering
 				 */
 				if((!tCoordinatorProxy.getHierarchyLevel().isHighest() /* is a higher cluster possible? */) && (mHRMController.getCoordinator(tCoordinatorProxy.getHierarchyLevel()) != null /* does a local coordinator at this hierarchy level exist? */)){
 					HierarchyLevel tSuperiorClusterLevel = new HierarchyLevel(this, tCoordinatorProxy.getHierarchyLevel().getValue() + 1);
 					if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_ANNOUNCEMENT_PACKETS){
 						Logging.log(this, "     ..restarting clustering at hierarchy level: " + tSuperiorClusterLevel.getValue());
 					}
 					mHRMController.cluster(this, tSuperiorClusterLevel);
 				}else{
 					//Logging.warn(this, "Skipped clustering at hierarchy level " + (tCoordinatorProxy.getHierarchyLevel().getValue() + 1) + " for remote coordinator: " + tCoordinatorProxy);
 				}
 			}
 		}else{
 			//Logging.warn(this, "Avoiding redundant registration of locally instantiated coordinator: " + pAnnounceCoordinator);
 		}
 	}
 
 	/**
 	 * Removes an announced coordinator from the local ARG
 	 * 
 	 * @param pSourceEntity the source of the invalidation (corresponds to the next hop towards the announcer)
 	 * @param pInvalidCoordinator the invalidation
 	 */
 	protected void unregisterAnnouncedCoordinatorARG(ControlEntity pSourceEntity, InvalidCoordinator pInvalidCoordinator)
 	{
 		// check if the "remote" coordinator isn't stored at this physical node
 		if(!pInvalidCoordinator.getSenderClusterCoordinatorNodeL2Address().equals(mHRMController.getNodeL2Address())){
 			ClusterName tRemoteClusterName = pInvalidCoordinator.getSenderClusterName();
 			if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_ANNOUNCEMENT_PACKETS){
 				Logging.log(this, "Unregistering ANNOUNCED REMOTE COORDINATOR: " + tRemoteClusterName);
 			}
 
 			/**
 			 * Remove the invalid coordinator as superior one of this node from the HRMController database
 			 */
 			// is the packet still on its way from the top to the bottom AND does it not belong to an L0 coordinator?
 			if((!pInvalidCoordinator.enteredSidewardForwarding()) && (!pInvalidCoordinator.getSenderClusterName().getHierarchyLevel().isBaseLevel())){
 				mHRMController.unregisterSuperiorCoordinator(pInvalidCoordinator.getSenderClusterName());
 			}
 
 			// search for an already existing CoordintorProxy instance
 			CoordinatorProxy tCoordinatorProxy = mHRMController.getCoordinatorProxyByName(tRemoteClusterName);
 			if(tCoordinatorProxy != null){
 				Logging.log(this, "REMOVING PROXY FOR ANNOUNCED REMOTE COORDINATOR: " + tRemoteClusterName);
 			
 				/**
 				 * Trigger: remote coordinator role invalid
 				 */
 				tCoordinatorProxy.eventRemoteCoordinatorRoleInvalid();
 			}
 		}else{
 			//Logging.warn(this, "Avoiding unregistration of locally instantiated coordinator: " + pInvalidCoordinator);
 		}
 	}
 
 	/**
 	 * Returns if both objects address the same cluster/coordinator
 	 * 
 	 * @return true or false
 	 */
 	@Override
 	public boolean equals(Object pObj)
 	{
 		if(DEBUG_EQUALS){
 			Logging.log(this, "EQUALS COMPARING with " + pObj);
 		}
 
 		if (((this instanceof Cluster) && (pObj instanceof Coordinator)) ||
 			((this instanceof Cluster) && (pObj instanceof CoordinatorAsClusterMember)) ||
 			((this instanceof ClusterMember) && (pObj instanceof Coordinator)) ||
 			((this instanceof ClusterMember) && (!(this instanceof CoordinatorProxy)) && (pObj instanceof CoordinatorProxy)) ||
 			((this instanceof CoordinatorProxy) && (pObj instanceof ClusterMember) && (!(pObj instanceof CoordinatorProxy))) ||
 			((this instanceof Coordinator) && (pObj instanceof ClusterMember)) ||
 			((this instanceof CoordinatorAsClusterMember) && (pObj instanceof Cluster)) ||
 			((this instanceof Coordinator) && (pObj instanceof Cluster))){
 			if(DEBUG_EQUALS){
 				Logging.log(this, "  ..false!");
 			}
 			return false;
 		}
 		
 		if (pObj instanceof Long){
 			Long tOtherClusterID = (Long)pObj;
 
 			if (tOtherClusterID.equals(getClusterID())) {
 				if(DEBUG_EQUALS){
 					Logging.log(this, "  ..true!");
 				}
 				return true;
 			}
 		}
 		
 		if(this instanceof CoordinatorAsClusterMember){
 			CoordinatorAsClusterMember tThisCoordinatorAsClusterMember = (CoordinatorAsClusterMember)this;
 			CoordinatorAsClusterMember tComparedCoordinatorAsClusterMember = (CoordinatorAsClusterMember)pObj;
 			if ((tThisCoordinatorAsClusterMember.getCoordinator().equals(tComparedCoordinatorAsClusterMember.getCoordinator())) && (tThisCoordinatorAsClusterMember.getCoordinatorNodeL2Address().equals(tComparedCoordinatorAsClusterMember.getCoordinatorNodeL2Address()))){
 				if(DEBUG_EQUALS){
 					Logging.log(this, "  ..true!");
 				}
 				return true;
 			}else{
 				if(DEBUG_EQUALS){
 					Logging.log(this, "  ..false!");
 				}
 				return false;
 			}
 		}
 
 		if(pObj instanceof ControlEntity){
 			ControlEntity tComparedObj = (ControlEntity) pObj;
 			
 			if (this instanceof Coordinator){
 				Coordinator tThisCoordinator = (Coordinator)this;
 				if(tThisCoordinator.getCoordinatorID() == tComparedObj.getCoordinatorID()){
 					if(DEBUG_EQUALS){
 						Logging.log(this, "  ..true!");
 					}
 					return true;
 				}
 			}
 					
 			//HINT: we ignore the coordinator ID because the clusterID is unique enough for identification
 			if (tComparedObj.getClusterID().equals(getClusterID()) && (tComparedObj.getHierarchyLevel().equals(getHierarchyLevel()))) {
 				if(DEBUG_EQUALS){
 					Logging.log(this, "  ..true!");
 				}
 				return true;
 			}
 		}
 
 		if(DEBUG_EQUALS){
 			Logging.log(this, "  ..false!");
 		}
 		return false;
 	}	
 
 	/**
 	 * Defines the decoration color for the ARG viewer
 	 * 
 	 * @return color for the control entity or null if no specific color is available
 	 */
 	@Override
 	public Color getColor()
 	{
 		Float tSaturation = Float.valueOf(1.0f - 0.5f * (getHierarchyLevel().getValue() + 1)/ HRMConfig.Hierarchy.HEIGHT);
 		
 		if (this instanceof Coordinator){
 			return new Color(tSaturation, (float)0.6, (float)0.8);
 		}
 		if (this instanceof CoordinatorProxy){
 			return new Color(tSaturation, (float)0.6, (float)0.8);
 		}
 		if (this instanceof CoordinatorAsClusterMember){
 			return new Color(tSaturation, (float)0.8, (float)0.6);
 		}
 		if (this instanceof Cluster){
 			return new Color((float)0.7, tSaturation, 0);
 		}
 		if (this instanceof ClusterMember){
 			return new Color((float)0.7, tSaturation, (float)0.3);
 		}
 		
 		return null;
 	}
 	
 	
 	/**
 	 * Defines the decoration text for the ARG viewer
 	 * 
 	 * @return text for the control entity or null if no text is available
 	 */
 	@Override
 	public String getText()
 	{
 		return null;
 	}
 	
 	/**
 	 * Defines the decoration image for the ARG viewer
 	 *  
 	 * @return file name of image for the control entity or null if no specific image is available
 	 */
 	@Override
 	public String getImageName()
 	{
 		return null;
 	}
 
 	/**
 	 * Returns the name space of this "Name".
 	 * This function is introduced by the "Name" class.
 	 * 
 	 * @return the name space
 	 */
 	@Override
 	public Namespace getNamespace() {
 		return new Namespace("HRM entities");
 	}
 		
 	/**
 	 * Returns the size of a serialized representation.
 	 * This function is introduced by the "Name" class.
 	 * 
 	 * @return the size of a serialized representation
 	 */
 	@Override
 	public int getSerialisedSize()
 	{
 		return 0;
 	}
 
 	/**
 	 * Returns a location description about this instance
 	 * 
 	 * @return the location description
 	 */
 	@Override
 	public String toLocation()
 	{
 		String tResult = getClass().getSimpleName() + "@" + mHRMController.getNodeGUIName() + "@" + getHierarchyLevel().getValue();
 		
 		return tResult;
 	}
 }
