 /*
  * CreateConnectionSbbonnectionSbb.java
  * Mobicents Media Gateway
  *
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
 
 package org.mobicents.media.server.control.mgcp;
 
 import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpProvider;
 import jain.protocol.ip.mgcp.message.CreateConnection;
 import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
 import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
 import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
 import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
 import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
 import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
 import jain.protocol.ip.mgcp.message.parms.ReturnCode;
 
 import javax.naming.Context;
 import javax.naming.InitialContext;
 import javax.naming.NamingException;
 import javax.slee.ActivityContextInterface;
 import javax.slee.CreateException;
 import javax.slee.RolledBackContext;
 import javax.slee.Sbb;
 import javax.slee.SbbContext;
 import javax.slee.TransactionRequiredLocalException;
 import javax.slee.facilities.ActivityContextNamingFacility;
 import javax.slee.facilities.FacilityException;
 import javax.slee.facilities.NameAlreadyBoundException;
 
 import net.java.slee.resource.mgcp.MgcpActivityContextInterfaceFactory;
 
 import org.apache.log4j.Logger;
 import org.mobicents.media.msc.common.events.MsConnectionEventCause;
 import org.mobicents.mscontrol.MsConnection;
 import org.mobicents.mscontrol.MsConnectionEvent;
 import org.mobicents.mscontrol.MsProvider;
 import org.mobicents.mscontrol.MsSession;
 import org.mobicents.slee.resource.media.ratype.MediaRaActivityContextInterfaceFactory;
 
 /**
  * @author amit bhayani
  * @author Oleg Kulikov
  */
 public abstract class CreateConnectionSbb implements Sbb {
 
 	private SbbContext sbbContext;
 	private Logger logger = Logger.getLogger(CreateConnectionSbb.class);
 
 	private JainMgcpProvider mgcpProvider;
 	private MgcpActivityContextInterfaceFactory mgcpAcif;
 
 	private MsProvider msProvider;
 	private MediaRaActivityContextInterfaceFactory msActivityFactory;
 
 	private ActivityContextNamingFacility activityContextNamingfacility;
 
 	/**
 	 * Creates a new instance of CreateConnectionSbb
 	 * 
 	 */
 	public CreateConnectionSbb() {
 	}
 
 	public void setSbbContext(SbbContext sbbContext) {
 		this.sbbContext = sbbContext;
 		try {
 			Context ctx = (Context) new InitialContext()
 					.lookup("java:comp/env");
 
 			// initialize media api
 			msProvider = (MsProvider) ctx
 					.lookup("slee/resources/media/1.0/provider");
 			msActivityFactory = (MediaRaActivityContextInterfaceFactory) ctx
 					.lookup("slee/resources/media/1.0/acifactory");
 
 			mgcpProvider = (JainMgcpProvider) ctx
 					.lookup("slee/resources/jainmgcp/2.0/provider");
 			mgcpAcif = (MgcpActivityContextInterfaceFactory) ctx
 					.lookup("slee/resources/jainmgcp/2.0/acifactory");
 
 			activityContextNamingfacility = (ActivityContextNamingFacility) ctx
 					.lookup("slee/facilities/activitycontextnaming");
 
 		} catch (NamingException ne) {
 			logger.warn("Could not set SBB context:" + ne.getMessage());
 		}
 	}
 
 	public void onCreateConnection(CreateConnection event,
 			ActivityContextInterface aci) {
 
 		CreateConnectionResponse response = null;
 
 		String remoteSDP = null;
 
 		CallIdentifier callIdentifier = event.getCallIdentifier();
 
 		int txID = event.getTransactionHandle();
 
 		this.setTxId(txID);
 
 		EndpointIdentifier endpointID = event.getEndpointIdentifier();
 
 		String endPointName = endpointID.getLocalEndpointName();
 
 		if (endPointName.endsWith("/$")) {
 			this.setUseSpecificEndPointId(true);
 		}
 
 		MsSession session = msProvider.createSession();
 		MsConnection msConnection = session
 				.createNetworkConnection(endPointName);
 
 		logger.info("--> CRCX TX ID = " + txID + " Endpoint = " + endpointID);
 
 		ActivityContextInterface msAci = null;
 		try {
 			msAci = msActivityFactory.getActivityContextInterface(msConnection);
 			msAci.attach(sbbContext.getSbbLocalObject());
 		} catch (Exception ex) {
 			logger.error("Internal server error", ex);
 			sendResponse(txID, ReturnCode.Internal_Hardware_Failure);// TODO
 			// is
 			// the
 			// ReturnCode
 			// correct?
 			return;
 		}
 
 		// Check if RemoteConnectionDescriptor is available apply it to
 		// Connection
 
 		ConnectionDescriptor remoteConnectionDescriptor = event
 				.getRemoteConnectionDescriptor();
 
 		if (remoteConnectionDescriptor != null) {
 			remoteSDP = remoteConnectionDescriptor.toString();
 		}
 
 		msConnection.modify(endPointName, remoteSDP);
 
 		// TODO : Check for SecondEndpointIdentifier - Local Connection
 
 		// TODO : Check for NotifiedEntity to send Response back to Call Agent
 
 		// TODO : Check for LocalConnectionOptions (Codec, Packetization,
 		// Bandwidth, Type of Service, Usage of echo cancellation, Silence
 		// Suppression, Gain Control, RTP Security, Network Type, Resource
 		// Reservation.)
 
 		// TODO : Check for ConnectionMode (SEND, SEND_RECV, RECV, conf,
 		// inactive, loopback, continuity test, network loop back, network
 		// continuity test)
 
 		// TODO : Check for BearerInformation ( encoding method to be used )
 
 		// TODO : Check for NotificationRequestParms - RequestedEvents A Call
 		// Agent may ask to be notified about certain events occuring in an
 		// endpoint by including the name of the event in a RequestedEvents
 		// param
 
 		// TODO : How to handle the Signals?
 
 	}
 
 	public void onConnectionCreated(MsConnectionEvent evt,
 			ActivityContextInterface aci) {
 
 		MsConnection msConnection = evt.getConnection();
 
 		ConnectionIdentifier connectionIdentifier = new ConnectionIdentifier(
				msConnection.getId());
 		CreateConnectionResponse response = new CreateConnectionResponse(this,
 				ReturnCode.Transaction_Executed_Normally, connectionIdentifier);
 		String sdpLocalDescriptor = msConnection.getLocalDescriptor();
 
 		ConnectionDescriptor localConnectionDescriptor = new ConnectionDescriptor(
 				sdpLocalDescriptor);
 
 		response.setLocalConnectionDescriptor(localConnectionDescriptor);
 
 		response.setTransactionHandle(this.getTxId());
 
 		if (this.getUseSpecificEndPointId()) {
 
 			String localEndpointName = msConnection.getEndpoint();
 			String domainName = "localhost"; // TODO : Get JBoss bind address
 			// here?
 
 			EndpointIdentifier specificEndpointIdentifier = new EndpointIdentifier(
 					localEndpointName, domainName);
 
 			response.setSpecificEndpointIdentifier(specificEndpointIdentifier);
 		}
 
 		mgcpProvider.sendMgcpEvents(new JainMgcpEvent[] { response });
 
 		try {
 			// bind the MsConnection aci to ActivityContextNamingFacility for
 			// processing DLCX
 			activityContextNamingfacility.bind(aci, connectionIdentifier
 					.toString());
 		} catch (TransactionRequiredLocalException e) {
 			logger
 					.warn(
 							"Binding of MsConnection ACI to ActivityContextNamingfacility failed. DLCX for this ConnectionIdentifier may fail",
 							e);
 		} catch (FacilityException e) {
 			logger
 					.warn(
 							"Binding of MsConnection ACI to ActivityContextNamingfacility failed. DLCX for this ConnectionIdentifier may fail",
 							e);
 		} catch (NullPointerException e) {
 			logger
 					.warn(
 							"Binding of MsConnection ACI to ActivityContextNamingfacility failed. DLCX for this ConnectionIdentifier may fail",
 							e);
 		} catch (IllegalArgumentException e) {
 			logger
 					.warn(
 							"Binding of MsConnection ACI to ActivityContextNamingfacility failed. DLCX for this ConnectionIdentifier may fail",
 							e);
 		} catch (NameAlreadyBoundException e) {
 			logger
 					.warn(
 							"Binding of MsConnection ACI to ActivityContextNamingfacility failed. DLCX for this ConnectionIdentifier may fail",
 							e);
 		}
 
 	}
 
 	public void onConnectionTransactionFailed(MsConnectionEvent evt,
 			ActivityContextInterface aci) {
 		logger.warn("ConnectionTransactionFailed");
 
 		MsConnectionEventCause msConnectionEventCause = evt.getCause();
 
 		// TODO is the ReturnCode correct
 		switch (msConnectionEventCause) {
 		case FACILITY_FAILURE:
 			sendResponse(this.getTxId(), ReturnCode.Endpoint_Unknown);
 			break;
 
 		default:
 			sendResponse(this.getTxId(), ReturnCode.Internal_Hardware_Failure);
 			break;
 		}
 
 	}
 
 	private void sendResponse(int txID, ReturnCode reason) {
 		CreateConnectionResponse response = new CreateConnectionResponse(this,
 				reason, new ConnectionIdentifier("0"));
 		response.setTransactionHandle(txID);
 		logger.info("<-- TX ID = " + txID + ": " + response.getReturnCode());
 		mgcpProvider.sendMgcpEvents(new JainMgcpEvent[] { response });
 	}
 
 	/**
 	 * Converts MGCP specific mode value into local value.
 	 * 
 	 * @param mode
 	 *            the MGCP specific mode.
 	 * @return the local mode.
 	 */
 	private int getMode(ConnectionMode mode) {
 		/*
 		 * switch (mode.getConnectionModeValue()) { case ConnectionMode.RECVONLY :
 		 * return BaseConnection.MODE_RECV_ONLY; case ConnectionMode.SENDONLY :
 		 * return BaseConnection.MODE_SEND_ONLY; case ConnectionMode.SENDRECV :
 		 * return BaseConnection.MODE_SEND_RECV; default : return
 		 * BaseConnection.MODE_SEND_RECV; }
 		 */
 		return 0;
 	}
 
 	public void unsetSbbContext() {
 	}
 
 	public void sbbCreate() throws CreateException {
 	}
 
 	public void sbbPostCreate() throws CreateException {
 	}
 
 	public void sbbActivate() {
 	}
 
 	public void sbbPassivate() {
 	}
 
 	public void sbbLoad() {
 	}
 
 	public void sbbStore() {
 	}
 
 	public void sbbRemove() {
 	}
 
 	public void sbbExceptionThrown(Exception exception, Object object,
 			ActivityContextInterface activityContextInterface) {
 	}
 
 	public void sbbRolledBack(RolledBackContext rolledBackContext) {
 	}
 
 	public abstract int getTxId();
 
 	public abstract void setTxId(int txId);
 
 	public abstract boolean getUseSpecificEndPointId();
 
 	public abstract void setUseSpecificEndPointId(boolean useSpecificEndPointId);
 
 }
