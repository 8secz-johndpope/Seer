 /**
  * Copyright (c) The Dojo Foundation 2011. All Rights Reserved.
  * Copyright (c) IBM Corporation 2008, 2011. All Rights Reserved.
  */
 package org.coweb;
 
import java.util.concurrent.atomic.AtomicInteger;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.Map;
 
 import java.util.logging.Logger;
 
 import org.cometd.bayeux.server.BayeuxServer;
 import org.cometd.bayeux.Message;
 import org.cometd.bayeux.server.ServerSession;
 import org.cometd.bayeux.server.LocalSession;
 import org.cometd.bayeux.server.ServerChannel;
 import org.cometd.bayeux.server.ServerMessage;
 import org.cometd.bayeux.server.ConfigurableServerChannel;
 
 import org.coweb.oe.OperationEngineException;
 
 import java.io.IOException;
 import java.security.MessageDigest;
 
 /**
  * SessionHandler receives and handles all coweb protocol messages belonging to
  * this session. Maintains a ServiceHandler for all incoming bot messages.
  * Maintains a Moderator.
  */
 public class SessionHandler implements ServerChannel.MessageListener {
 
 	private static final Logger log = Logger.getLogger(SessionHandler.class
 			.getName());
 
 	private String confKey = null;
 	private boolean cacheState = false;
 	private String sessionId = null;
 	private ServiceHandler serviceHandler = null;
 	private BayeuxServer server = null;
 
 	private LateJoinHandler lateJoinHandler = null;
 	private SessionModerator sessionModerator = null;
 	private OperationEngineHandler operationEngine = null;
 
	private AtomicInteger order = new AtomicInteger(0);
 
 	private String syncAppChannel = null;
 	private String syncEngineChannel = null;
 	private String rosterAvailableChannel = null;
 	private String rosterUnavailableChannel = null;
 
 	private String requestUrl = null;
 	private String sessionName = null;
 
 	private ArrayList<ServerSession> attendees = new ArrayList<ServerSession>();
 
 	public SessionHandler(String confkey, boolean cacheState, Map<String, Object> config) {
 
 		this.confKey = confkey;
 		this.cacheState = cacheState;
 		this.sessionId = hashURI(confkey);
 		this.serviceHandler = new ServiceHandler(this.sessionId, config);
 
 		this.server = SessionManager.getInstance().getBayeux();
 
 		this.syncAppChannel = "/session/" + this.sessionId + "/sync/app";
 		this.syncEngineChannel = "/session/" + this.sessionId + "/sync/engine";
 		this.rosterAvailableChannel = "/session/" + this.sessionId
 				+ "/roster/available";
 		this.rosterUnavailableChannel = "/session/" + this.sessionId
 				+ "/roster/unavailable";
 
 		ServerChannel.Initializer initializer = new ServerChannel.Initializer() {
 			@Override
 			public void configureChannel(ConfigurableServerChannel channel) {
 				channel.setPersistent(true);
 				channel.setLazy(false);
 			}
 		};
 		this.server.createIfAbsent(this.syncAppChannel, initializer);
 		this.server.createIfAbsent(this.syncEngineChannel, initializer);
 
 		ServerChannel sync = server.getChannel(this.syncAppChannel);
 		sync.addListener(this);
 		sync = server.getChannel(this.syncEngineChannel);
 		sync.addListener(this);
 
 		this.sessionModerator = SessionModerator.getInstance(this, (String)config.get("sessionModerator"), confKey);
 		if (null == this.sessionModerator) {
 			config.put("moderatorIsUpdater", false);
 			/* Perhaps config.get("sessionModerator") had an exception or didn't exist,
 			   so either we try to create default implementation of moderator, or throw an exception. */
 			log.severe("SessionModerator.getInstance(" + config.get("sessionModerator") +
 					") failed, reverting to trying to create default implementation.");
 			this.sessionModerator = SessionModerator.getInstance(this, null, confKey);
 			if (null == this.sessionModerator) {
 				throw new CowebException("Create SessionModerator", "");
 			}
 			log.severe("SessionModerator created default implementation, but moderator can no longer be updater.");
 		}
 
 		// create the late join handler. clients will be updaters by default.
 		boolean mustUseEngine = false;
 		LateJoinHandler lh = null;
 		if (config.containsKey("moderatorIsUpdater") &&
 				((Boolean) config.get("moderatorIsUpdater")).booleanValue()) {
 			// If moderator is updater, the OperationEngine *must* be used!
 			mustUseEngine = true;
 			lh = new ModeratorLateJoinHandler(this, config);
 		} else {
 			log.info("creating LateJoinHandler");
 			lh = new LateJoinHandler(this, config);
 		}
 
 		this.lateJoinHandler = lh;
 
 		// create the OT engine only if turned on in the config, or moderatorIsUpdater.
 		boolean useEngine = config.containsKey("operationEngine") && 
 			((Boolean) config.get("operationEngine")).booleanValue();
 		if (!useEngine && mustUseEngine) {
 			log.warning("Must use OperationEngine because moderatorIsUpdater==true, even though operationEngine is not set.");
 			useEngine = true;
 		}
 		if (useEngine) {
 			LocalSession modSession = this.sessionModerator.getLocalSession();
 			Integer siteId = (Integer) modSession.getAttribute("siteid");
 
 			try {
 				log.info("creating operation engine with siteId = " + siteId);
 				this.operationEngine = new OperationEngineHandler(this, siteId.intValue());
 			} catch (OperationEngineException e) {
 				e.printStackTrace();
 			}
 		}
 		else {
 			log.info("No op engine for this session");
 		}
 	}
 
 	public SessionModerator getSessionModerator() {
 		return this.sessionModerator;
 	}
 
 	public String getRequestUrl() {
 		return this.requestUrl;
 	}
 
 	public void setRequestUrl(String url) {
 		this.requestUrl = url;
 	}
 
 	public String getSessionName() {
 		return this.sessionName;
 	}
 
 	public void setSessionName(String name) {
 		this.sessionName = name;
 	}
 
 	public ServiceHandler getServiceHandler() {
 		return this.serviceHandler;
 	}
 
 	public ArrayList<ServerSession> getAttendees() {
 		return this.attendees;
 	}
 
 	public String getRosterAvailableChannel() {
 		return this.rosterAvailableChannel;
 	}
 
 	public String getRosterUnavailableChannel() {
 		return this.rosterUnavailableChannel;
 	}
 
 	public String getConfKey() {
 		return this.confKey;
 	}
 
 	public String getSessionId() {
 		return this.sessionId;
 	}
 
 	public boolean isCachingState() {
 		return this.cacheState;
 	}
 
 	public String toString() {
 		StringBuffer sb = new StringBuffer();
 		sb.append("{\"confkey\":");
 		sb.append(this.confKey);
 		sb.append(",\"sessionid\":");
 		sb.append(this.sessionId);
 		sb.append("}");
 
 		return sb.toString();
 	}
 
 	public ServerSession getServerSessionFromSiteid(String siteStr) {
 		return this.lateJoinHandler.getServerSessionFromSiteid(siteStr);
 	}
 
 	/**
 	  * Called whenever a client sends a message to the cometd server.
 	  *
 	  * The cometd implementation is free to have multiple threads invoke onMessage
 	  * for different incoming messages, so be aware of mutual exclusion issues.
 	  */
 	public boolean onMessage(ServerSession from, ServerChannel channel,
 			ServerMessage.Mutable message) {
 
 		Integer siteId = (Integer) from.getAttribute("siteid");
 
 		String msgSessionId = (String) from.getAttribute("sessionid");
 		if (!msgSessionId.equals(this.sessionId)) {
 
 			log.severe("Received message not belonging to this session "
 					+ msgSessionId);
 
 			return true;
 		}
 		
 		this.lateJoinHandler.clearCacheState();
 
 		Map<String, Object> data = message.getDataAsMap();
 		data.put("siteId", siteId);
 
 		/* Some of the following code must acquire this.operationEngine's lock.
 		   OperationEngine must only be accessed by one client at a time.
 
 		   Note that the moderator's onSync method is already declared with execute
 		   with mutual exclusion.
 		 */
 		String channelName = message.getChannel();
 		if (channelName.equals(this.syncAppChannel)) {
 			// put total order on message
			data.put("order", this.order.getAndIncrement());
 
 			if (this.operationEngine != null) {
 				
 				synchronized (this.operationEngine) {
 					log.info("data before operation engine");
 					log.info(data.toString());
 					Map<String, Object> syncEvent = this.operationEngine.syncInbound(data);
 					if (syncEvent != null) {
 						this.sessionModerator.onSync(syncEvent);
 
 						log.info("data after operation engine");
 						log.info(syncEvent.toString());
 					}
 				}
 
 			}
 
 			try {
 				String topic = (String)data.get("topic");
 				if(!topic.startsWith("coweb.engine.sync"))
 					this.serviceHandler.forwardSyncEvent(from, message);
 			} catch (Exception e) {
 				e.printStackTrace();
 			}
 		} else if (channelName.equals(this.syncEngineChannel)) {
 			if(operationEngine != null) {
 				synchronized (this.operationEngine) {
 					log.info("sending engine sync to operation engine");
 					log.info(data.toString());
 					this.operationEngine.engineSyncInbound(data);
 				}
 			}
 		}
 
 		return true;
 	}
 
 	public void onPublish(ServerSession remote, Message message) {
 		String channel = message.getChannel();
 		try {
 			if (channel.startsWith("/service/bot")) {
 				Map<String, Object> data = message.getDataAsMap();
 				String topic = (String) data.get("topic");
 				if (!topic.startsWith("coweb.engine.sync"))
 					if (this.sessionModerator.canClientMakeServiceRequest(
 							remote, message))
 						this.serviceHandler.forwardUserRequest(remote, message);
 					else {
 						// TODO
 						// send error message.
 					}
 			} else if (channel.equals("/service/session/updater")) {
 				this.lateJoinHandler.onUpdaterSendState(remote, message);
 			}
 		} catch (Exception e) {
 			log.severe("error receiving publish message");
 			log.severe(message.getJSON());
 			e.printStackTrace();
 		}
 	}
 
 	public void onSubscribe(ServerSession serverSession, Message message)
 			throws IOException {
 		String channel = (String) message.get(Message.SUBSCRIPTION_FIELD);
 		if (channel.equals("/service/session/join/*")) {
 			if (this.sessionModerator.canClientJoinSession(serverSession))
 				this.lateJoinHandler.onClientJoin(serverSession, message);
 			else {
 				// TODO
 				// need to send error message.
 			}
 		} else if (channel.startsWith("/service/bot")
 				|| channel.startsWith("/bot")) {
 			if (this.sessionModerator.canClientSubscribeService(serverSession))
 				this.serviceHandler.subscribeUser(serverSession, message);
 			else {
 				// TODO
 				// need to send error message.
 			}
 		} else if (channel.endsWith("/session/updater")) {
 			log.info("client subscribes to /session/updater");
 			this.attendees.add(serverSession);
 			this.lateJoinHandler.onUpdaterSubscribe(serverSession, message);
 			this.sessionModerator.onClientJoinSession(serverSession);
 		}
 	}
 
 	public void onUnsubscribe(ServerSession serverSession, Message message)
 			throws IOException {
 
 		String channel = (String) message.get(Message.SUBSCRIPTION_FIELD);
 		if (channel.startsWith("/service/bot") || channel.startsWith("/bot")) {
 			this.serviceHandler.unSubscribeUser(serverSession, message);
 		}
 
 		return;
 	}
 
 	public void onPurgingClient(ServerSession client) {
 		this.attendees.remove(client);
 
 		this.sessionModerator.onClientLeaveSession(client);
 		boolean last = this.lateJoinHandler.onClientRemove(client);
 
 		System.out.println("Puring client last="+last);
 		if (last) {
 			this.endSession();
 		}
 	}
 
 	public static String hashURI(String url) {
 
 		String hash = null;
 
 		try {
 			String t = Long.toString(System.currentTimeMillis());
 			url = url + t;
 			byte[] bytes = url.getBytes("UTF-8");
 			MessageDigest md = MessageDigest.getInstance("MD5");
 			byte[] digest = md.digest(bytes);
 
 			StringBuffer sb = new StringBuffer();
 			for (int i = 0; i < digest.length; i++) {
 				sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16)
 						.substring(1));
 			}
 
 			hash = sb.toString();
 
 		} catch (Exception e) {
 			hash = url;
 		}
 
 		return hash;
 	}
 
 	public void removeBadClient(ServerSession client) {
 		return;
 	}
 
 	public void endSession() {
 		log.fine("SessionHandler::endSession ***********");
 		log.info("end session");
 
 		ServerChannel sync = this.server.getChannel(this.syncAppChannel);
 		sync.removeListener(this);
 
 		sync = server.getChannel(this.syncEngineChannel);
 		sync.removeListener(this);
 
 		// The following ordering must be observed!
 		this.sessionModerator.onSessionEnd();
 		if (null != this.operationEngine)
 			this.operationEngine.shutdown();
 		this.lateJoinHandler.onEndSession();
 		this.serviceHandler.shutdown();
 
 		// just to make sure we are not pinning anything down.
 		this.sessionModerator = null;
 		this.operationEngine = null;
 		this.lateJoinHandler = null;
 		this.serviceHandler = null;
 
 		SessionManager manager = SessionManager.getInstance();
 		manager.removeSessionHandler(this);
 	}
 	
 	/**
      * Publishes a local op engine sync event to the /session/sync Bayeux 
      * channel.
      *
      * @param int[] context int array context vector for this site
      */
 	public void postEngineSync(Integer[] sites) {
 		log.info("sites = " + Arrays.toString(sites));
 		ServerChannel sync = this.server.getChannel(this.syncEngineChannel);
 		
 		HashMap<String, Object> data = new HashMap<String, Object>();
 		data.put("context", sites);
 		
 		// We publish *from* the LocalSession.
 		sync.publish(this.sessionModerator.getLocalSession(), data, null);
 	}
 
 	/**
 	  *
 	  * Retreives the four element Object engine state array and returns it.
 	  *
 	  * return engine state array
 	  */
 	public Object[] getEngineState() {
 		Object[] ret;
 		synchronized (this.operationEngine) {
 			ret = this.operationEngine.getEngineState();
 		}
 		return ret;
 	}
 
 }
