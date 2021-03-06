 /*
  * MsConnection.java
  *
  * The Simple Media Server Control API
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
 
 package org.mobicents.mscontrol;
 
 import java.io.Serializable;
 
 /**
  * Represents the actual RTP connection. MsConnection is created as shown below
  * <p>
  * <blockquote>
  * 
  * <pre>
  * MsSession msSession;
  * ....
  * MsConnection msConnection = msSession.createNetworkConnection(&quot;media/trunk/Announcement/$&quot;)
  * </pre>
  * 
  * </blockquote>
  * </p>
  * At this stage <code>MsConnection</code> is in <code>IDLE</code> state and
  * <code>CONNECTION_CREATED</code> event is fired.
  * 
  * Through out the lifetime of <code>MsConnection</code>, it maintains the
  * reference to <code>MsSession</code>
  * 
  * @author Oleg Kulikov
  * @author amit.bhayani
  */
 public interface MsConnection extends Serializable {
 
 	/**
 	 * Retrieves the MsConnection ID, basically a UUID
 	 * 
 	 * @return returns the unique identifier of this <code>MsConnection</code>
 	 */
 	public String getId();
 
 	/**
 	 * Retrieves the {@link MsSession} that is associated with this
 	 * <code>MsConnection</code>. This <code>MsSession</code> reference
 	 * remains valid throughout the lifetime of the <code>MsConnection</code>
 	 * object despite the state of the <code>MsConnection</code> object. This
 	 * <code>MsSession</code> reference does not change once the
 	 * <code>MsConnection</code> object has been created.
 	 * 
 	 * @return MsSession object holding this connection.
 	 */
 	public MsSession getSession();
 
 	/**
 	 * Returns the state of <code>MsConnection</code>
 	 * 
 	 * @return {@link MsConnectionState} representing the state of
 	 *         <code>MsConnection</code>
 	 */
 	public MsConnectionState getState();
 
 	/**
 	 * Gets the session descriptor of the local end.
 	 * 
 	 * @return session descriptor as specified by SDP.
 	 */
 	public String getLocalDescriptor();
 
 	/**
 	 * Gets the session descriptor of the remote end.
 	 * 
 	 * @return session descriptor as specified by SDP.
 	 */
 	public String getRemoteDescriptor();
 
 	/**
 	 * Returns the concrete endpoint which executes this connection.
 	 * 
 	 * @return the name of the endpoint on the media server or null if
 	 *         connection is not created on media server yet.
 	 */
 	public MsEndpoint getEndpoint();
 
 	/**
 	 * Adds connection listener.
 	 * 
 	 * @param listener
 	 *            the listener object.
 	 */
 	public void addConnectionListener(MsConnectionListener listener);
 
 	/**
 	 * Removes connection listener.
 	 * 
 	 * @param listener
 	 *            the listener object was added previously.
 	 */
 	public void removeConnectionListener(MsConnectionListener listener);
 
        public void addNotificationListener(MsNotificationListener listener);
        public void removeNotificationListener(MsNotificationListener listener);
        
 	/**
 	 * Creates or modify network connection on the media server side.
 	 * 
 	 * @param remoteDesc
 	 *            the session desriptor of the remote party.
 	 */
 	public void modify(String localDesc, String remoteDesc);
 
 	/**
 	 * Deletes related connection from media server. If the corresponding
 	 * MsSession has only this MsConnection left then the state of
 	 * <code>MsSession</code> becomes <code>INVALID</code>
 	 */
 	public void release();
 
 }
