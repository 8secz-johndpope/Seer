 /*
  * JBoss, Home of Professional Open Source
  * Copyright 2008, Red Hat Middleware LLC, and individual contributors
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 package org.mobicents.ha.javax.sip;
 
 import gov.nist.core.StackLogger;
 
 import java.net.InetAddress;
 import java.net.UnknownHostException;
 import java.rmi.registry.LocateRegistry;
 import java.rmi.registry.Registry;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.Properties;
 import java.util.Set;
 import java.util.Timer;
 import java.util.TimerTask;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.CopyOnWriteArraySet;
 
 import javax.sip.ListeningPoint;
 
 import org.mobicents.ha.javax.sip.util.Inet6Util;
 import org.mobicents.tools.sip.balancer.NodeRegisterRMIStub;
 import org.mobicents.tools.sip.balancer.SIPNode;
 
 /**
  *  <p>implementation of the <code>LoadBalancerHeartBeatingService</code> interface.</p>
  *     
  *  <p>
  *  It sends heartbeats and health information to the sip balancers configured through the stack property org.mobicents.ha.javax.sip.BALANCERS 
  *  </p>
  * 
  * @author <A HREF="mailto:jean.deruelle@gmail.com">Jean Deruelle</A> 
  *
  */
 public class LoadBalancerHeartBeatingServiceImpl implements LoadBalancerHeartBeatingService {
 	private static final int DEFAULT_RMI_PORT = 2000;
 	private static final String BALANCER_SIP_PORT_CHAR_SEPARATOR = ":";
 	private static final String BALANCERS_CHAR_SEPARATOR = ";";
 	private static final int DEFAULT_LB_SIP_PORT = 5065;		
 	
 	ClusteredSipStack sipStack = null;
 	//the logger
     StackLogger logger = null;
     //the balancers to send heartbeat to and our health info
 	private String balancers;
 	//the jvmRoute for this node
 	private String jvmRoute;
     //the balancers names to send heartbeat to and our health info
 	private Map<String, SipLoadBalancer> register = new ConcurrentHashMap<String, SipLoadBalancer>();
 	//heartbeat interval, can be modified through JMX
 	private long heartBeatInterval = 5000;
 	private Timer heartBeatTimer = new Timer();
 	private TimerTask hearBeatTaskToRun = null;
 
     private boolean started = false;
     
     private boolean displayBalancerWarning = true;
     private boolean displayBalancerFound = true;
     private Set<LoadBalancerHeartBeatingListener> loadBalancerHeartBeatingListeners;
     
     public LoadBalancerHeartBeatingServiceImpl() {
 		loadBalancerHeartBeatingListeners = new CopyOnWriteArraySet<LoadBalancerHeartBeatingListener>();
 	}
     
 	public void init(ClusteredSipStack clusteredSipStack,
 			Properties stackProperties) {
 		sipStack = clusteredSipStack;
 		logger = clusteredSipStack.getStackLogger();
 		balancers = stackProperties.getProperty(BALANCERS);
 	}
     
     public void start() {
     	if (!started) {
 			if (balancers != null && balancers.length() > 0) {
 				String[] balancerDescriptions = balancers.split(BALANCERS_CHAR_SEPARATOR);
 				for (String balancerDescription : balancerDescriptions) {
 					String balancerAddress = balancerDescription;
 					int sipPort = DEFAULT_LB_SIP_PORT;
 					int rmiPort = DEFAULT_RMI_PORT;
 					if(balancerDescription.indexOf(BALANCER_SIP_PORT_CHAR_SEPARATOR) != -1) {
 						String[] balancerDescriptionSplitted = balancerDescription.split(BALANCER_SIP_PORT_CHAR_SEPARATOR);
 						balancerAddress = balancerDescriptionSplitted[0];
 						try {
 							sipPort = Integer.parseInt(balancerDescriptionSplitted[1]);
 							if(balancerDescriptionSplitted.length>2) {
 								rmiPort = Integer.parseInt(balancerDescriptionSplitted[2]);
 							}
 						} catch (NumberFormatException e) {
 							logger.logError("Impossible to parse the following sip balancer port " + balancerDescriptionSplitted[1], e);
 						}
 					} 
 					if(Inet6Util.isValidIP6Address(balancerAddress) || Inet6Util.isValidIPV4Address(balancerAddress)) {
 						try {
 							this.addBalancer(InetAddress.getByName(balancerAddress).getHostAddress(), sipPort, rmiPort);
 						} catch (UnknownHostException e) {
 							logger.logError("Impossible to parse the following sip balancer address " + balancerAddress, e);
 						}
 					} else {
 						this.addBalancer(balancerAddress, sipPort, 0, rmiPort);
 					}
 				}
 			}		
 			started = true;
 		}
 		this.hearBeatTaskToRun = new BalancerPingTimerTask();
 		this.heartBeatTimer.scheduleAtFixedRate(this.hearBeatTaskToRun, 0,
 				this.heartBeatInterval);
 		if(logger.isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
 			logger.logDebug("Created and scheduled tasks for sending heartbeats to the sip balancer.");
 		}
     }
     
     public void stop() {
     	// Force removal from load balancer upon shutdown 
     	// added for Issue 308 (http://code.google.com/p/mobicents/issues/detail?id=308)
     	ArrayList<SIPNode> info = getConnectorsAsSIPNode();
     	removeNodesFromBalancers(info);
     	//cleaning 
 //    	balancerNames.clear();
     	register.clear();
     	if(hearBeatTaskToRun != null) {
     		this.hearBeatTaskToRun.cancel();
     	}
 		this.hearBeatTaskToRun = null;
 		started = false;
     }
 	
     /**
      * {@inheritDoc}
      */
 	public long getHeartBeatInterval() {
 		return heartBeatInterval;
 	}
 	/**
      * {@inheritDoc}
      */
 	public void setHeartBeatInterval(long heartBeatInterval) {
 		if (heartBeatInterval < 100)
 			return;
 		this.heartBeatInterval = heartBeatInterval;
 		this.hearBeatTaskToRun.cancel();
 		this.hearBeatTaskToRun = new BalancerPingTimerTask();
 		this.heartBeatTimer.scheduleAtFixedRate(this.hearBeatTaskToRun, 0,
 				this.heartBeatInterval);
 
 	}
 
 	/**
 	 * 
 	 * @param hostName
 	 * @param index
 	 * @return
 	 */
 	private InetAddress fetchHostAddress(String hostName, int index) {
 		if (hostName == null)
 			throw new NullPointerException("Host name cant be null!!!");
 
 		InetAddress[] hostAddr = null;
 		try {
 			hostAddr = InetAddress.getAllByName(hostName);
 		} catch (UnknownHostException uhe) {
 			throw new IllegalArgumentException(
 					"HostName is not a valid host name or it doesnt exists in DNS",
 					uhe);
 		}
 
 		if (index < 0 || index >= hostAddr.length) {
 			throw new IllegalArgumentException(
 					"Index in host address array is wrong, it should be [0]<x<["
 							+ hostAddr.length + "] and it is [" + index + "]");
 		}
 
 		InetAddress address = hostAddr[index];
 		return address;
 	}
 
 	/**
      * {@inheritDoc}
      */
 	public String[] getBalancers() {
 		return this.register.keySet().toArray(new String[register.keySet().size()]);
 	}
 
 	/**
      * {@inheritDoc}
      */
 	public boolean addBalancer(String addr, int sipPort, int rmiPort) {
 		if (addr == null)
 			throw new NullPointerException("addr cant be null!!!");
 
 		InetAddress address = null;
 		try {
 			address = InetAddress.getByName(addr);
 		} catch (UnknownHostException e) {
 			throw new IllegalArgumentException(
 					"Somethign wrong with host creation.", e);
 		}		
 		String balancerName = address.getCanonicalHostName() + ":" + rmiPort;
 
 		if (register.get(balancerName) != null) {
 			logger.logInfo("Sip balancer " + balancerName + " already present, not added");
 			return false;
 		}
 
 		if(logger.isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
 			logger.logDebug("Adding following balancer name : " + balancerName +"/address:"+ addr);
 		}
 
 		SipLoadBalancer sipLoadBalancer = new SipLoadBalancer(this, address, sipPort, rmiPort);
 		register.put(balancerName, sipLoadBalancer);
 
 		// notify the listeners
 		for (LoadBalancerHeartBeatingListener loadBalancerHeartBeatingListener : loadBalancerHeartBeatingListeners) {
 			loadBalancerHeartBeatingListener.loadBalancerAdded(sipLoadBalancer);
 		}
 		
 		return true;
 	}
 
 	/**
      * {@inheritDoc}
      */
 	public boolean addBalancer(String hostName, int sipPort, int index, int rmiPort) {
 		return this.addBalancer(fetchHostAddress(hostName, index)
 				.getHostAddress(), sipPort, rmiPort);
 	}
 
 	/**
      * {@inheritDoc}
      */
 	public boolean removeBalancer(String addr, int sipPort, int rmiPort) {
 		if (addr == null)
 			throw new NullPointerException("addr cant be null!!!");
 
 		InetAddress address = null;
 		try {
 			address = InetAddress.getByName(addr);
 		} catch (UnknownHostException e) {
 			throw new IllegalArgumentException(
 					"Something wrong with host creation.", e);
 		}
 
 		SipLoadBalancer sipLoadBalancer = new SipLoadBalancer(this, address, sipPort, rmiPort);
 
 		String keyToRemove = null;
 		Iterator<String> keyIterator = register.keySet().iterator();
 		while (keyIterator.hasNext() && keyToRemove ==null) {
 			String key = keyIterator.next();
 			if(register.get(key).equals(sipLoadBalancer)) {
 				keyToRemove = key;
 			}
 		}
 		
 		if(keyToRemove !=null ) {
 			if(logger.isLoggingEnabled(StackLogger.TRACE_DEBUG)) {
 				logger.logDebug("Removing following balancer name : " + keyToRemove +"/address:"+ addr);
 			}
 			register.remove(keyToRemove);
 			
 			// notify the listeners
 			for (LoadBalancerHeartBeatingListener loadBalancerHeartBeatingListener : loadBalancerHeartBeatingListeners) {
 				loadBalancerHeartBeatingListener.loadBalancerRemoved(sipLoadBalancer);
 			}
 			return true;
 		}
 
 		return false;
 	}
 
 	/**
      * {@inheritDoc}
      */
 	public boolean removeBalancer(String hostName, int sipPort, int index, int rmiPort) {
 		InetAddress[] hostAddr = null;
 		try {
 			hostAddr = InetAddress.getAllByName(hostName);
 		} catch (UnknownHostException uhe) {
 			throw new IllegalArgumentException(
 					"HostName is not a valid host name or it doesnt exists in DNS",
 					uhe);
 		}
 
 		if (index < 0 || index >= hostAddr.length) {
 			throw new IllegalArgumentException(
 					"Index in host address array is wrong, it should be [0]<x<["
 							+ hostAddr.length + "] and it is [" + index + "]");
 		}
 
 		InetAddress address = hostAddr[index];
 
 		return this.removeBalancer(address.getHostAddress(), sipPort, rmiPort);
 	}
 
 	private ArrayList<SIPNode> getConnectorsAsSIPNode() {
 		ArrayList<SIPNode> info = new ArrayList<SIPNode>();
 		// Gathering info about server' sip listening points
 		Iterator<ListeningPoint> listeningPointIterator = sipStack.getListeningPoints();
 		while (listeningPointIterator.hasNext()) {
 			ListeningPoint listeningPoint = listeningPointIterator.next();
 			String address = listeningPoint.getIPAddress();
 			// From Vladimir: for some reason I get "localhost" here instead of IP and this confiuses the LB
 			if(address.equals("localhost")) address = "127.0.0.1";
 			
 			int port = listeningPoint.getPort();
 			String transport = listeningPoint.getTransport();
 			String[] transports = new String[] {transport};
 			
 			String hostName = null;
 			try {
 				InetAddress[] aArray = InetAddress
 						.getAllByName(address);
 				if (aArray != null && aArray.length > 0) {
 					// Damn it, which one we should pick?
 					hostName = aArray[0].getCanonicalHostName();
 				}
 			} catch (UnknownHostException e) {
 				logger.logError("An exception occurred while trying to retrieve the hostname of a sip connector", e);
 			}
 			
 			String httpPortString = System.getProperty("org.mobicents.properties.httpPort");
 			String sslPortString = System.getProperty("org.mobicents.properties.sslPort");
 			
 			int httpPort = 0;
 			int sslPort = 0;
 			
 			if(httpPortString != null) {
 				httpPort = Integer.parseInt(httpPortString);
 			}
 			if(sslPortString != null) {
 				sslPort = Integer.parseInt(sslPortString);
 			}
 			
 			SIPNode node = new SIPNode(hostName, address, port,
 					transports, jvmRoute, httpPort, sslPort, null);
 
 			info.add(node);
 		}
 		return info;
 	}
 	
 	/**
 	 * @param info
 	 */
 	private void sendKeepAliveToBalancers(ArrayList<SIPNode> info) {
 		for(SipLoadBalancer  balancerDescription:new HashSet<SipLoadBalancer>(register.values())) {
 			try {
 				Registry registry = LocateRegistry.getRegistry(balancerDescription.getAddress().getHostAddress(), balancerDescription.getRmiPort());
 				NodeRegisterRMIStub reg=(NodeRegisterRMIStub) registry.lookup("SIPBalancer");
 				reg.handlePing(info);
 				displayBalancerWarning = true;
 				if(displayBalancerFound) {
 					logger.logInfo("SIP Load Balancer Found!");
 					displayBalancerFound = false;
 				}
 			} catch (Exception e) {
 				if(displayBalancerWarning) {
 					logger.logWarning("Cannot access the SIP load balancer RMI registry: " + e.getMessage() +
 							"\nIf you need a cluster configuration make sure the SIP load balancer is running. Host " + balancerDescription.toString());
 //					logger.error("Cannot access the SIP load balancer RMI registry: " , e);
 					displayBalancerWarning = false;
 				}
 				displayBalancerFound = true;
 			}
 		}
 		if(logger.isLoggingEnabled(StackLogger.TRACE_TRACE)) {
 			logger.logTrace("Finished gathering, Gathered info[" + info + "]");
 		}
 	}		
 	
 	/**
 	 * @param info
 	 */
 	private void removeNodesFromBalancers(ArrayList<SIPNode> info) {
 		for(SipLoadBalancer balancerDescription:new HashSet<SipLoadBalancer>(register.values())) {
 			try {
 				Registry registry = LocateRegistry.getRegistry(balancerDescription.getAddress().getHostAddress(),balancerDescription.getRmiPort());
 				NodeRegisterRMIStub reg=(NodeRegisterRMIStub) registry.lookup("SIPBalancer");
 				reg.forceRemoval(info);
 				displayBalancerWarning = true;
 				if(displayBalancerFound) {
 					logger.logInfo("SIP Load Balancer Found!");
 					displayBalancerFound = false;
 				}
 			} catch (Exception e) {
 				if(displayBalancerWarning) {
 					logger.logWarning("Cannot access the SIP load balancer RMI registry: " + e.getMessage() +
 							"\nIf you need a cluster configuration make sure the SIP load balancer is running.");
 //					logger.error("Cannot access the SIP load balancer RMI registry: " , e);
 					displayBalancerWarning = false;
 				}
 				displayBalancerFound = true;
 			}
 		}
 		if(logger.isLoggingEnabled(StackLogger.TRACE_TRACE)) {
 			logger.logTrace("Finished gathering, Gathered info[" + info + "]");
 		}
 	}
 	
 	/**
 	 * 
 	 * @author <A HREF="mailto:jean.deruelle@gmail.com">Jean Deruelle</A> 
 	 *
 	 */
 	class BalancerPingTimerTask extends TimerTask {
 
 		@SuppressWarnings("unchecked")
 		@Override
 		public void run() {			
 			ArrayList<SIPNode> info = getConnectorsAsSIPNode();						
 			sendKeepAliveToBalancers(info);
 		}
 	}
 
 	/**
 	 * @param balancers the balancers to set
 	 */
 	public void setBalancers(String balancers) {
 		this.balancers = balancers;
 	}
 
 	/**
 	 * @return the jvmRoute
 	 */
 	public String getJvmRoute() {
 		return jvmRoute;
 	}
 
 	/**
 	 * @param jvmRoute the jvmRoute to set
 	 */
 	public void setJvmRoute(String jvmRoute) {
 		this.jvmRoute = jvmRoute;
 	}
 
 	public void addLoadBalancerHeartBeatingListener(
 			LoadBalancerHeartBeatingListener loadBalancerHeartBeatingListener) {
 		loadBalancerHeartBeatingListeners.add(loadBalancerHeartBeatingListener);
 	}
 
 	public void removeLoadBalancerHeartBeatingListener(
 			LoadBalancerHeartBeatingListener loadBalancerHeartBeatingListener) {
 		loadBalancerHeartBeatingListeners.remove(loadBalancerHeartBeatingListener);
 	}
 	
 	/**
 	 * @param info
 	 */
 	public void sendSwitchoverInstruction(SipLoadBalancer sipLoadBalancer, String fromJvmRoute, String toJvmRoute) {
 		logger.logInfo("switching over from " + fromJvmRoute + " to " + toJvmRoute);
 		if(fromJvmRoute == null || toJvmRoute == null) {
 			return;
 		}		
 		try {
 			Registry registry = LocateRegistry.getRegistry(sipLoadBalancer.getAddress().getHostAddress(),sipLoadBalancer.getRmiPort());
 			NodeRegisterRMIStub reg=(NodeRegisterRMIStub) registry.lookup("SIPBalancer");
 			reg.switchover(fromJvmRoute, toJvmRoute);
 			displayBalancerWarning = true;
 			if(displayBalancerFound) {
 				logger.logInfo("SIP Load Balancer Found!");
 				displayBalancerFound = false;
 			}
 		} catch (Exception e) {
 			if(displayBalancerWarning) {
 				logger.logWarning("Cannot access the SIP load balancer RMI registry: " + e.getMessage() +
 						"\nIf you need a cluster configuration make sure the SIP load balancer is running.");
 //					logger.error("Cannot access the SIP load balancer RMI registry: " , e);
 				displayBalancerWarning = false;
 			}
 			displayBalancerFound = true;
 		}				
 	}
 }
