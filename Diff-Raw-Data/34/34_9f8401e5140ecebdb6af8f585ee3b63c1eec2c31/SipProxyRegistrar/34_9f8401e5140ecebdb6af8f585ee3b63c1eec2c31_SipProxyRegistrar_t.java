 /*
 p2pproxy Copyright (C) 2007  Jehan Monnier ()
 
 SipListener.java - sip proxy.
 
 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
 
 package org.linphone.p2pproxy.core.sipproxy;
 
 
 import java.io.File;
 import java.io.IOException;
 import java.net.InetSocketAddress;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Timer;
 import java.util.TimerTask;
 import java.util.concurrent.Callable;
 import java.util.concurrent.Executors;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Future;
 import net.jxta.document.AdvertisementFactory;
 import net.jxta.id.IDFactory;
 import org.apache.log4j.Logger;
 import org.apache.log4j.NDC;
 import org.linphone.p2pproxy.api.P2pProxyException;
 import org.linphone.p2pproxy.api.P2pProxyUserNotFoundException;
 
 import org.linphone.p2pproxy.core.Configurator;
 import org.linphone.p2pproxy.core.JxtaNetworkManager;
 import org.linphone.p2pproxy.core.P2pProxyAccountManagementMBean;
 import org.linphone.p2pproxy.core.media.rtprelay.MediaType;
 import org.linphone.p2pproxy.core.sipproxy.superpeers.SuperPeerProxy;
 import org.zoolu.sip.header.ExpiresHeader;
 import org.zoolu.sip.header.MultipleHeader;
 import org.zoolu.sip.message.Message;
 import org.zoolu.sip.message.MessageFactory;
 import org.zoolu.sip.provider.SipProvider;
 import org.zoolu.sip.provider.SipProviderListener;
 import org.zoolu.sip.provider.SipStack;
 import org.zoolu.sip.transaction.TransactionServer;
 import java.util.Collections;
 
 public class SipProxyRegistrar implements SipProviderListener,SipProxyRegistrarMBean {
    private final static Logger mLog = Logger.getLogger(SipProxyRegistrar.class);   
    public final static String REGISTRAR_PORT="org.linphone.p2pproxy.SipListener.registrar.port";
    public final static String REGISTRAR_PUBLIC_ADDRESS="org.linphone.p2pproxy.SipListener.registrar.public.address";
    public final static String ADV_NAME = "p2p-proxy-proxyregistrar";
    private final int ADV_LIFE_TIME=6000000;
    //
    private final SipProvider mProvider;
    private final JxtaNetworkManager mJxtaNetworkManager;
    private final ExecutorService mPool;
 
    private final Map<String,Registration> mRegistrationTab = Collections.synchronizedMap(new HashMap<String,Registration>()); 
    private final Map<String,SipMessageTask> mCancalableTaskTab = Collections.synchronizedMap(new HashMap<String,SipMessageTask>());
 
    private final P2pProxyAccountManagementMBean mP2pProxyAccountManagement;
    private final Configurator mProperties;
    private final SuperPeerProxy mSuperPeerProxy;
    private  NetworkResourceAdvertisement mProxyRegistrationAdvertisement;
    private final Timer mTimer = new Timer ("Proxy registrar adv publisher");
   
    //private long mNumberOfEstablishedCall;
    private long mNumberOfRefusedRegistration;
    private long mNumberOfSuccessfullRegistration;
    private long mNumberOfUSerNotFound;
    private long mNumberOfUnknownUSers;
    private long mNumberOfUnknownUsersForRegistration;
    private long mNumberOfUnRegistration;
    
    
    public static class Registration {
       long RegistrationDate;
       public long Expiration;
       //implementation specific context
       public Object NetResources;
       public  Map<MediaType,InetSocketAddress> RtpRelays = new HashMap<MediaType,InetSocketAddress>() ;
       public String Contact;
       public final String From;
       public Registration(String aFrom) {From = aFrom;}
      public String toString() {
    	  return "reg date ["+RegistrationDate+"] exp ["+Expiration+"] contact ["+Contact+"] from ["+From+"]";
      }
    }
    
    class SipMessageTask implements Callable<Boolean> {
       private final SipProvider mProvider;
       private final Message mMessage;
       private Future<?> mFuture;
       
       /**
        * @return Returns the mMessage.
        */
       public Message getMessage() {
          return mMessage;
       }
       SipMessageTask(SipProvider aProvider, Message aMessage) {
          mProvider = aProvider;
          mMessage = aMessage;
       }
       public Boolean call() throws Exception {
     	  NDC.push(mMessage.getFirstLine() + mMessage.getCallIdHeader().getCallId() +":");
          try {
             if (mMessage.isRequest()) {
                if (mMessage.isRegister()) {
                   processRegister(mProvider, mMessage);
                } else {
                   proxyRequest(mProvider, mMessage);
                }
             } else {
                //1 remove via header   
                SipUtils.removeVia(mProvider,mMessage);
                //2 process response
                proxyResponse(mProvider, mMessage);
             }
             if (mMessage.isInvite() && mCancalableTaskTab.containsKey(mMessage.getCallIdHeader().getCallId()) )  {
                mCancalableTaskTab.remove(mMessage.getCallIdHeader().getCallId());
             }
          } catch (InterruptedException eInter) {
             mLog.info("request interrupted",eInter);
             //nop
          }
          catch (Exception e) {
             mLog.error("unexpected behavior",e);
             if (mMessage.isRequest()) {
                Message lResp= null;
                lResp = MessageFactory.createResponse(mMessage,500,e.getMessage(),null);
                TransactionServer lTransactionServer = new TransactionServer(mProvider,mMessage,null);
                lTransactionServer.respondWith(lResp);
                synchronized (SipProxyRegistrar.this) {
                   if (mMessage.isInvite() && mCancalableTaskTab.containsKey(mMessage.getCallIdHeader().getCallId()) )  {
                      mCancalableTaskTab.remove(mMessage.getCallIdHeader().getCallId());
                   }
                }
             }
          } finally {
         	 NDC.pop();
          }
          return true;
       }
       /**
        * @return Returns the mFuture.
        */
       public Future<?> getFuture() {
          return mFuture;
       }
       /**
        * @param future The mFuture to set.
        */
       public void setFuture(Future<?> future) {
          mFuture = future;
       }
       
    }
    
    public SipProxyRegistrar(Configurator lProperties,JxtaNetworkManager aJxtaNetworkManager,P2pProxyAccountManagementMBean aP2pProxyAccountManagement) throws IOException {
       mJxtaNetworkManager =  aJxtaNetworkManager;
       mP2pProxyAccountManagement = aP2pProxyAccountManagement;
       mProperties = lProperties;
       File lFile = new File(SipStack.log_path);
       if (lFile.exists() == false) lFile.mkdir();
       String lViaAddress = lProperties.getProperty(REGISTRAR_PUBLIC_ADDRESS);
       int lPort = Integer.parseInt(lProperties.getProperty(REGISTRAR_PORT, "5060"));
       String[] lProto = {SipProvider.PROTO_UDP};
       mProvider=new SipProvider(lViaAddress,lPort,lProto,SipProvider.ALL_INTERFACES);
       mProvider.addSipProviderListener(SipProvider.PROMISQUE,this);
       mPool = Executors.newCachedThreadPool();
       mSuperPeerProxy = new SuperPeerProxy(aJxtaNetworkManager, "sip:"+mProvider.getViaAddress()+":"+mProvider.getPort(),mRegistrationTab);
       TimerTask lPublisherTask = new TimerTask() {
 
 		@Override
 		public void run() {
 			try {
 				SipProxyRegistrar.this.publishAdvertisement();
 			} catch (IOException e) {
 				mLog.error("cannot publish proxy registar adv", e);
 			}
 			
 		}
     	  
       };
       mTimer.scheduleAtFixedRate(lPublisherTask, 0, ADV_LIFE_TIME-ADV_LIFE_TIME/10);
       TimerTask lRegistrationTableGC = new TimerTask() {
 
 		@Override
 		public void run() {
 			// copy list
 			Collection<Registration> lCurrentRegistrations = mRegistrationTab.values();
 			long lCurrentDate = System.currentTimeMillis();
 			for (Registration lRegistration : lCurrentRegistrations) {
 				if ((lCurrentDate - lRegistration.RegistrationDate - lRegistration.Expiration) > 0) {
 					if (mLog.isInfoEnabled()) mLog.info("registration entry ["+lRegistration+"] has expired");
 					mRegistrationTab.remove(lRegistration.From);
 				}
 			}
 		}
     	  
       };
       mTimer.scheduleAtFixedRate(lRegistrationTableGC, 0, 60000);
       
    }
    public  void onReceivedMessage(SipProvider aProvider, Message aMessage) {
       if (aProvider.getListeners().containsKey(aMessage.getTransactionId())) {
          if (mLog.isDebugEnabled()) mLog.debug ("nothing to do, transaction already handled for ["+aMessage+"]");
          return;
       }
       if (mLog.isInfoEnabled()) mLog.info("receiving message ["+aMessage+"]");
       String lCallId = aMessage.getCallIdHeader().getCallId();
       SipMessageTask lPendingSipMessageTask = mCancalableTaskTab.get(lCallId);
       
       if (aMessage.isCancel() && lPendingSipMessageTask != null ) {
          // search for pending transaction
          
          lPendingSipMessageTask.getFuture().cancel(true);
          mCancalableTaskTab.remove(lCallId);
 
          SipUtils.removeVia(mProvider,lPendingSipMessageTask.getMessage());
          // accept cancel
          Message lCancelResp = MessageFactory.createResponse(aMessage,200,"ok",null);
          TransactionServer lCancelTransactionServer = new TransactionServer(mProvider,aMessage,null);
          lCancelTransactionServer.respondWith(lCancelResp);
          
          // cancel invite
          Message lInviteResp = MessageFactory.createResponse(lPendingSipMessageTask.getMessage(),487,"Request Terminated",null);
          TransactionServer lInviteTransactionServer = new TransactionServer(mProvider,lPendingSipMessageTask.getMessage(),null);
          lInviteTransactionServer.respondWith(lInviteResp);          
       } else {
          // normal behavior
          SipMessageTask lSipMessageTask = new SipMessageTask(aProvider,aMessage);
          lSipMessageTask.setFuture(mPool.submit(lSipMessageTask));
          if (aMessage.isInvite()) {
             mCancalableTaskTab.put(aMessage.getCallIdHeader().getCallId(),lSipMessageTask);                
          }
          
       }
       
    }
 //////////////////////////////////////////////////////////////////////
 ////Proxy methods
 /////////////////////////////////////////////////////////////////////	
    private void proxyResponse(SipProvider aProvider, Message aMessage) throws NumberFormatException, InterruptedException, P2pProxyException, IOException {
       mSuperPeerProxy.proxyResponse(aProvider, aMessage);
    }
    private void proxyRequest(SipProvider aProvider, Message aMessage) throws Exception {
       if (aMessage.isAck() && aMessage.getToHeader().getTag() == null) {
          // just terminate the Invite transaction
          return;
       }
       
       if (aMessage.isInvite() == true) {
          // 100 trying
          TransactionServer lTransactionServer = new TransactionServer(aProvider,aMessage,null);
          Message l100Trying = MessageFactory.createResponse(aMessage,100,"trying",null);
          lTransactionServer.respondWith(l100Trying);
          lTransactionServer.terminate();
       }
       //remove route
       MultipleHeader lMultipleRoute = aMessage.getRoutes();
       if (lMultipleRoute != null) {
          lMultipleRoute.removeTop();
          aMessage.setRoutes(lMultipleRoute);
       }
       // add Via only udp
       SipUtils.addVia(aProvider,aMessage);
       try {
          mSuperPeerProxy.proxyRequest(aProvider, aMessage);
       }catch (P2pProxyUserNotFoundException e) {
          //remove via 
          SipUtils.removeVia(aProvider, aMessage);
          if (aMessage.isInvite()) {
             Message lresp = MessageFactory.createResponse(aMessage,404,e.getMessage(),null);
             TransactionServer lTransactionServer = new TransactionServer(aProvider,aMessage,null);
             lTransactionServer.respondWith(lresp);
             lTransactionServer.terminate();
          } else {
             throw e;
          }
       }
    }
    
    
 //////////////////////////////////////////////////////////////////////
 ////Registrar methods
 /////////////////////////////////////////////////////////////////////	
    
    private  void processRegister(SipProvider aProvider, Message aMessage) throws IOException, P2pProxyException {
       TransactionServer lTransactionServer = new TransactionServer(aProvider,aMessage,null);
       Message l100Trying = MessageFactory.createResponse(aMessage,100,"trying",null);
       lTransactionServer.respondWith(l100Trying);
       Registration lRegistration=null;
       
       //check if already registered
       
       String lFromName = aMessage.getFromHeader().getNameAddress().getAddress().toString();
       if ((lRegistration = mRegistrationTab.get(lFromName)) != null) {
          
          updateRegistration(lRegistration,aMessage);
          
          if (aMessage.getExpiresHeader().getDeltaSeconds() == 0) {
             mRegistrationTab.remove(lFromName);
          } 
          
       } else {
          // new registration
          // test if account already created
          
          if (mP2pProxyAccountManagement.isValidAccount(lFromName)) {
          lRegistration = new Registration(lFromName);
          // forgot contact, use rport lRegistration.Contact = aMessage.getContactHeader().getNameAddress().getAddress().toString();;
          
          updateRegistration(lRegistration,aMessage);
          mRegistrationTab.put(lFromName, lRegistration);
          } else {
             // create negative answers
             mLog.info("account for user ["+lFromName+"] not created yet");
             Message lresp = MessageFactory.createResponse(aMessage,404,"Not found",null);
             lTransactionServer.respondWith(lresp);
             return;
          }
       }
       // ok, create answers
       Message lresp = MessageFactory.createResponse(aMessage,200,"Ok",null);
       lresp.addContactHeader(aMessage.getContactHeader(), false);
       ExpiresHeader lExpireHeader = new  ExpiresHeader((int) (lRegistration.Expiration/1000));
       lresp.addHeader(lExpireHeader, false);
       lTransactionServer.respondWith(lresp);
       
    }
    private void updateRegistration(Registration aRegistration, Message aRegistrationMessage) throws P2pProxyException {
       aRegistration.RegistrationDate = System.currentTimeMillis();
       // default registration period
       aRegistration.Expiration = 3600000;
       if (aRegistrationMessage.getExpiresHeader() != null ) {
          aRegistration.Expiration =  aRegistrationMessage.getExpiresHeader().getDeltaSeconds()*1000; 
       }
       aRegistration.Contact = "sip:"+aRegistrationMessage.getRemoteAddress()+":"+aRegistrationMessage.getRemotePort();
       mSuperPeerProxy.updateRegistration(aRegistration, aRegistrationMessage); 
    }
    
    
 public void stop() {
    try {
       mJxtaNetworkManager.getPeerGroup().getDiscoveryService().flushAdvertisement(mProxyRegistrationAdvertisement);
    } catch (IOException e) {
       mLog.warn("cannot flush registrar adv",e );
    }
    mProvider.halt();
 }
  
  //   public long getNumberOfEstablishedCall() {
 //      return mNumberOfEstablishedCall;
 //   }
    public long getNumberOfRefusedRegistration() {
       return mNumberOfRefusedRegistration;
    }
    public long getNumberOfSuccessfullRegistration() {
       return mNumberOfSuccessfullRegistration;
    }
    public long getNumberOfUSerNotFound() {
       return mNumberOfUSerNotFound;
    }
    public long getNumberOfUnknownUSers() {
       return mNumberOfUnknownUSers;
    }
    public long getNumberOfUnknownUsersForRegistration() {
        return mNumberOfUnknownUsersForRegistration;
    }
    public String[] getRegisteredList() {
       String[] lRegisteredList = new String[mRegistrationTab.size()] ;
       int i=0;
       for (String lRegistrationKey : mRegistrationTab.keySet()) {
          lRegisteredList[i++] = lRegistrationKey;
       }
       return   lRegisteredList;
    }
    public long getNumberOfUnRegistration() {
       return mNumberOfUnRegistration;
    }
    private void publishAdvertisement() throws IOException {
 	   if (mProxyRegistrationAdvertisement == null) {   
 	   mProxyRegistrationAdvertisement = (NetworkResourceAdvertisement) AdvertisementFactory.newAdvertisement(NetworkResourceAdvertisement.getAdvertisementType());
 	      mProxyRegistrationAdvertisement.setID(IDFactory.newCodatID(mJxtaNetworkManager.getPeerGroup().getPeerGroupID(), mSuperPeerProxy.getSipProxyRegistrarAddress().toString().getBytes()));
 	      mProxyRegistrationAdvertisement.setAddress(mSuperPeerProxy.getSipProxyRegistrarAddress());
 	      mProxyRegistrationAdvertisement.setName(ADV_NAME);
 	   }
 	   mJxtaNetworkManager.getPeerGroup().getDiscoveryService().publish(mProxyRegistrationAdvertisement,ADV_LIFE_TIME,ADV_LIFE_TIME/2);	   
 	   mLog.info(mProxyRegistrationAdvertisement + "published");
    }
 }
