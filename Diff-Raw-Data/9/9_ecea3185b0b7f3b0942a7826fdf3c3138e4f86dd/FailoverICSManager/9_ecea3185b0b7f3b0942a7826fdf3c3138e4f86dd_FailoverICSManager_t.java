 package com.jakeapp.core.services;
 
 import com.jakeapp.core.domain.Project;
 import com.jakeapp.core.domain.ProtocolType;
 import com.jakeapp.core.domain.Account;
 import com.jakeapp.core.domain.User;
 import com.jakeapp.core.services.exceptions.ProtocolNotSupportedException;
 import com.jakeapp.jake.ics.ICService;
 import com.jakeapp.jake.ics.UserId;
 import com.jakeapp.jake.ics.exceptions.NotLoggedInException;
 import com.jakeapp.jake.ics.filetransfer.FailoverCapableFileTransferService;
 import com.jakeapp.jake.ics.filetransfer.methods.ITransferMethodFactory;
 import com.jakeapp.jake.ics.impl.sockets.filetransfer.SimpleSocketFileTransferFactory;
 import com.jakeapp.jake.ics.impl.xmpp.XmppICService;
 import com.jakeapp.jake.ics.impl.xmpp.XmppUserId;
 import com.jakeapp.jake.ics.msgservice.IMsgService;
 import org.apache.log4j.Logger;
 
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.Map;
 
 
 /**
  * FailoverICSManager tries Socket-Transfers first, then XMPP-Inband-Transfers.
  * 
  * 
  * @see ICSManager
  */
 public class FailoverICSManager implements ICSManager {
 
	private static final boolean SOCKETS_ENABLED = true;
 
 	private static Logger log = Logger.getLogger(FailoverICSManager.class);
 
 	private Map<String, ICService> services = new HashMap<String, ICService>();
 
 	private Map<String, FailoverCapableFileTransferService> transfer = new HashMap<String, FailoverCapableFileTransferService>();
 
 	private Map<String, ICService> activeServices = new HashMap<String, ICService>();
 
 	@Override
 	public boolean hasTransferService(Project p) {
 		return this.transfer.containsKey(p.getProjectId());
 	}
 
 	@Override
 	public ICService getICService(Project p) {
 		ICService ics = null;
 
 		if (this.services.containsKey(p.getProjectId()))
 			ics = this.services.get(p.getProjectId());
 		else {
 			ics = this.createICService(p);
 			this.services.put(p.getProjectId(), ics);
 		}
 
 		return ics;
 	}
 
 	@Override
 	public FailoverCapableFileTransferService getTransferService(Project p)
 			throws NotLoggedInException {
 		ICService ics = getICService(p);
 		IMsgService msg = ics.getMsgService();
 		FailoverCapableFileTransferService fcfts;
 
 		if (!this.transfer.containsKey(p.getProjectId())) {
 			fcfts = createTransferService(this.getBackendUserId(p), ics, msg);
 			this.transfer.put(p.getProjectId(), fcfts);
 		}
 
 		return this.transfer.get(p.getProjectId());
 	}
 
 	private FailoverCapableFileTransferService createTransferService(
 			com.jakeapp.jake.ics.UserId user, ICService ics, IMsgService msg)
 			throws NotLoggedInException {
 		FailoverCapableFileTransferService fcfts;
 		fcfts = new FailoverCapableFileTransferService();
 		if (SOCKETS_ENABLED)
			fcfts.addTransferMethod(new SimpleSocketFileTransferFactory(), msg, user);
 
 		ITransferMethodFactory inbandMethod = ics.getTransferMethodFactory();
 		if (inbandMethod == null) {
 			log.fatal("inband method not provided");
 		} else {
 			fcfts.addTransferMethod(inbandMethod, msg, user);
 		}
 		return fcfts;
 	}
 
 	@Override
 	public com.jakeapp.jake.ics.UserId getBackendUserId(Project p, User u) {
 		if (p.getCredentials().getProtocol().equals(ProtocolType.XMPP)) {
 			return new XmppUserId(u.getUserId() + "/" + p.getProjectId());
 		} else {
 			log.fatal("Currently unsupported protocol given");
 			throw new IllegalArgumentException(new ProtocolNotSupportedException());
 		}
 	}
 
 
 	@Override
 	public UserId getBackendUserId(User u) {
 		return new XmppUserId(u.getUserId() + "/Jake");
 	}
 
 	@Override
 	public User getFrontendUserId(Project p, com.jakeapp.jake.ics.UserId u) {
 		if (p.getMessageService().getProtocolType().equals(ProtocolType.XMPP)) {
 			return new User(ProtocolType.XMPP, u.getUserId());
 		} else {
 			log.fatal("Currently unsupported protocol given");
 			throw new IllegalArgumentException(new ProtocolNotSupportedException());
 		}
 	}
 
 	@Override
 	public com.jakeapp.jake.ics.UserId getBackendUserId(Project p) {
 		return this.getBackendUserId(p, p.getUserId());
 	}
 
 	private ICService createICService(Project p) {
 		log.debug("creating ICS");
 		Account cred = p.getCredentials();
 		ICService ics = null;
 
 		if (p.getCredentials().getProtocol().equals(ProtocolType.XMPP)) {
 			log.debug("Creating new XMPPICService for cred:  " + cred);
 			ics = new XmppICService(XMPPMsgService.namespace, p.getName());
 		} else {
 			log.fatal("Currently unsupported protocol given");
 			throw new IllegalArgumentException(new ProtocolNotSupportedException());
 		}
 		return ics;
 	}
 
 
 	public Collection<ICService> getAll() {
 		return this.services.values();
 	}
 }
