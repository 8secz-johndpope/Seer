 package com.parc.ccn.network;
 
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.net.InetSocketAddress;
 import java.nio.ByteBuffer;
 import java.nio.channels.DatagramChannel;
 import java.nio.channels.SelectionKey;
 import java.nio.channels.Selector;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.Timer;
 import java.util.TimerTask;
 import java.util.concurrent.Executors;
 import java.util.concurrent.Semaphore;
 import java.util.concurrent.ThreadPoolExecutor;
 import java.util.concurrent.TimeUnit;
 
 import javax.xml.stream.XMLStreamException;
 
 import com.parc.ccn.CCNBase;
 import com.parc.ccn.Library;
 import com.parc.ccn.data.ContentName;
 import com.parc.ccn.data.ContentObject;
 import com.parc.ccn.data.WirePacket;
 import com.parc.ccn.data.query.CCNFilterListener;
 import com.parc.ccn.data.query.CCNInterestListener;
 import com.parc.ccn.data.query.Interest;
 import com.parc.ccn.data.util.InterestTable;
 import com.parc.ccn.data.util.InterestTable.Entry;
 import com.parc.ccn.security.keys.KeyManager;
 
 /**
  * Interface to the ccnd.  Most clients,
  * and the CCN library, will use this as the "CCN".
  * 
  * @author smetters, rasmusse
  *
  */
 public class CCNNetworkManager implements Runnable {
 	
 	public static final int DEFAULT_AGENT_PORT = 4485;
 	public static final String DEFAULT_AGENT_HOST = "localhost";
 	public static final String PROP_AGENT_PORT = "ccn.agent.port";
 	public static final String PROP_AGENT_HOST = "ccn.agent.host";
 	public static final String PROP_TAP = "ccn.tap";
 	public static final String ENV_TAP = "CCN_TAP"; // match C library
 	public static final int MAX_PAYLOAD = 8800; // number of bytes in UDP payload
 	public static final int SOCKET_TIMEOUT = 1000; // period to wait in ms.
 	public static final int PERIOD = 2000; // period for occasional ops in ms.
 	public static final int HEARTBEAT_PERIOD = 3500;
 	public static final int MAX_PERIOD = PERIOD * 8;
 	public static final String KEEPALIVE_NAME = "/HereIAm";
 	public static final int THREAD_LIFE = 8;	// in seconds
 	
 	/**
 	 * Static singleton.
 	 */
 	
 	protected Thread _thread = null; // the main processing thread
 	protected ThreadPoolExecutor _threadpool = null; // pool service for callback threads
 	protected DatagramChannel _channel = null; // for use by run thread only!
 	protected Selector _selector = null;
 	protected Throwable _error = null; // Marks error state of socket
 	protected boolean _run = true;
 	protected KeyManager _keyManager;
 	protected ContentObject _keepalive; 
 	protected FileOutputStream _tapStreamOut = null;
 	protected FileOutputStream _tapStreamIn = null;
 	protected long _lastHeartbeat = 0;
 
 	// Tables of interests/filters: users must synchronize on collection
 	protected InterestTable<InterestRegistration> _myInterests = new InterestTable<InterestRegistration>();
 	protected InterestTable<Filter> _myFilters = new InterestTable<Filter>();
 	
 	private Timer _periodicTimer = null;
 	private boolean timersSetup = false;
 	
 	/**
 	 * @author rasmusse
 	 * Do scheduled writes.
 	 */
 	private class PeriodicWriter extends TimerTask {
 		public void run() {
 			
 			long ourTime = new Date().getTime();
			if ((ourTime - _lastHeartbeat) < HEARTBEAT_PERIOD) {
 				_lastHeartbeat = ourTime;
 				heartbeat();
 			}
 
 			// Library.logger().finest("Refreshing interests (size " + _myInterests.size() + ")");
 
 			// Re-express interests that need to be re-expressed
 			try {
 				synchronized (_myInterests) {
 					for (Entry<InterestRegistration> entry : _myInterests.values()) {
 						InterestRegistration reg = entry.value();
 						if (ourTime > reg.nextRefresh) {
 							Library.logger().finer("Refresh interest: " + reg.interest.name());
 							// Temporarily back out refresh period decay to allow the repository
 							// to work with an "eavesdropping interest"
 							//reg.nextRefreshPeriod = (reg.nextRefreshPeriod * 2) > MAX_PERIOD ? MAX_PERIOD
 									//: reg.nextRefreshPeriod * 2;
 							reg.nextRefresh += reg.nextRefreshPeriod;
 							write(reg.interest);
 						}
 					}
 				}
 			} catch (XMLStreamException xmlex) {
 				Library.logger().severe("Processing thread failure (Malformed datagram): " + xmlex.getMessage()); 
 				Library.warningStackTrace(xmlex);
 			}
 			
 			_periodicTimer.schedule(new PeriodicWriter(), PERIOD);
 		}
 	}
 	
 	// Send heartbeat
 	private void heartbeat() {
		if (!timersSetup)
			return;
 		try {
 			ByteBuffer heartbeat = ByteBuffer.allocate(1);
 			_channel.write(heartbeat);
 		} catch (IOException io) {
 			// We do not see errors on send typically even if 
 			// agent is gone, so log each but do not track
 			Library.logger().warning("Error sending heartbeat packet: " + io.getMessage());
 		}
 	}
 	
 	/**
 	 * First time startup of timing stuff after first registration
 	 * We don't bother to "unstartup" if everything is deregistered
 	 */
 	private void setupTimers() {
 		if (!timersSetup) {
 			timersSetup = true;
 			heartbeat();
 			
 			// Create timer for heartbeats and other periodic behavior
 			_periodicTimer = new Timer(true);
 			_periodicTimer.schedule(new PeriodicWriter(), PERIOD);
 		}
 	}
 			
 	// Generic superclass for registration objects that may have a listener
 	// Handles invalidation and pending delivery consistently to enable 
 	// subclass to call listener callback without holding any library locks,
 	// yet avoid delivery to a cancelled listener.
 	protected abstract class ListenerRegistration implements Runnable {
 		protected Object listener;
 		protected CCNNetworkManager manager;
 		public Semaphore sema = null;
 		public Object owner = null;
 		protected boolean deliveryPending = false;
 		protected long id;
 		
 		public abstract void deliver();
 		
 		public void invalidate() {
 			// There may be a pending delivery in progress, and it doesn't 
 			// happen while holding this lock because that would give the 
 			// application callback code power to block library processing.
 			// Instead, we use a flag that is checked and set under this lock
 			// to be sure that on exit from invalidate() there will be 
 			// no future deliveries based on the now-invalid registration.
 			while (true) {
 				synchronized (this) {
 					// Make invalid, this will prevent any new delivery that comes
 					// along from doing anything.
 					this.listener = null;
 					this.sema = null;
 					// Return only if no delivery is in progress now (or if we are
 					// called out of our own handler)
 					if (!deliveryPending || (Thread.currentThread().getId() == id)) {
 						return;
 					}
 				}
 				Thread.yield();
 			}
 		}
 		
 		public void run() {
 			id = Thread.currentThread().getId();
 			synchronized (this) {
 				// Mark us pending delivery, so that any invalidate() that comes 
 				// along will not return until delivery has finished
 				this.deliveryPending = true;
 			}
 			try {
 				// Delivery may synchronize on this object to access data structures
 				// but should hold no locks when calling the listener
 				deliver();
 			} catch (Exception ex) {
 				Library.logger().warning("failed delivery: " + ex.toString());
 			} finally {
 				synchronized(this) {
 					this.deliveryPending = false;
 				}
 			}
 		}
 		// Equality based on listener if present, so multiple objects can 
 		// have the same interest registered without colliding
 		public boolean equals(Object obj) {
 			if (obj instanceof ListenerRegistration) {
 				ListenerRegistration other = (ListenerRegistration)obj;
 				if (this.owner == other.owner) {
 					if (null == this.listener && null == other.listener){
 						return super.equals(obj);
 					} else if (null != this.listener && null != other.listener) {
 						return this.listener.equals(other.listener);
 					}
 				}
 			}
 			return false;
 		}
 		public int hashCode() {
 			if (null != this.listener) {
 				if (null != owner) {
 					return owner.hashCode() + this.listener.hashCode();
 				} else {
 					return this.listener.hashCode();
 				}
 			} else {
 				return super.hashCode();
 			}
 		}
 	}
 	
 	/**
 	 * Record of Interest
 	 * @field interest Interest itself
 	 * @field listener Listener to be notified of data matching the Interest.  The
 	 * listener must be set (non-null) for cases of standing Interest that holds 
 	 * until canceled by the application.  The listener should be null when a 
 	 * thread is blocked waiting for data, in which case the thread will be 
 	 * blocked on semaphore.
 	 * @field semaphore used to block thread waiting for data or null if none
 	 * @field data data for waiting thread
 	 * @field lastRefresh last time this interest was refreshed
 	 * @field data Holds data responsive to the interest for a waiting thread
 	 * @author jthornto
 	 *
 	 */
 	protected class InterestRegistration extends ListenerRegistration {
 		public final Interest interest;
 		protected ArrayList<ContentObject> data = new ArrayList<ContentObject>(1);
 		protected long nextRefresh;
 		protected long nextRefreshPeriod = PERIOD;
 		
 		// All internal client interests must have an owner
 		public InterestRegistration(CCNNetworkManager mgr, Interest i, CCNInterestListener l, Object owner) {
 			manager = mgr;
 			interest = i; 
 			listener = l;
 			this.owner = owner;
 			if (null == listener) {
 				sema = new Semaphore(0);
 			}
 			nextRefresh = new Date().getTime() + nextRefreshPeriod;
 		}
 		// Add a copy of data, not the original data object, so that 
 		// the recipient cannot disturb the buffers of the sender
 		// We need this even when data comes from network, since receive
 		// buffer will be reused while recipient thread may proceed to read
 		// from buffer it is handed
 		/**
 		 * Return true if data was added
 		 */
 		public synchronized boolean add(ContentObject obj) {
 			boolean hasData = (null == data);
 			if (!hasData)
 				this.data.add(obj.clone());
 			return !hasData;
 		}
 		
 		/**
 		 * This used to be called just data, but its similarity
 		 * to a simple accessor made the fact that it cleared the data
 		 * really confusing and error-prone...
 		 * Pull the available data out for processing.
 		 * @return
 		 */
 		public synchronized ArrayList<ContentObject> popData() {
 			ArrayList<ContentObject> result = this.data;
 			this.data = new ArrayList<ContentObject>(1);
 			return result;
 		}
 		
 		public void deliver() {
 			try {
 				if (null != this.listener) {
 					// Standing interest: call listener callback
 					ArrayList<ContentObject> results = null;
 					CCNInterestListener listener = null;
 					synchronized (this) {
 						if (this.data.size() > 0) {
 							results = this.data;
 							this.data = new ArrayList<ContentObject>(1);
 							listener = (CCNInterestListener)this.listener;
 						}
 					}
 					// Call into client code without holding any library locks
 					if (null != results) {
 						Library.logger().finer("Interest callback (" + results.size() + " data) for: " + this.interest.name());
 						
 						synchronized (this) {
 							// DKS -- dynamic interests, unregister the interest here and express new one if we have one
 							// previous interest is final, can't update it
 							this.deliveryPending = false;
 						}
 						manager.unregisterInterest(this);
 						
 						// paul r. note - contract says interest will be gone after the call into user's code.
 						// Eventually this may be modified for "pipelining".
 						
 						// DKS TODO tension here -- what object does client use to cancel?
 						// Original implementation had expressInterest return a descriptor
 						// used to cancel it, perhaps we should go back to that. Otherwise
 						// we may need to remember at least the original interest for cancellation,
 						// or a fingerprint representation that doesn't include the exclude filter.
 						// DKS even more interesting -- how do we update our interest? Do we?
 						// it's final now to avoid contention, but need to change it or change
 						// the registration.
 						Interest updatedInterest = listener.handleContent(results, interest);
 						
 						// Possibly we should optimize here for the case where the same interest is returned back
 						// (now we would unregister it, then reregister it) but need to be careful that the timing
 						// behavior is right if we do that
 						if (null != updatedInterest) {
 							Library.logger().finer("Interest callback: updated interest to express: " + updatedInterest.name());
 							// luckily we saved the listener
 							// if we want to cancel this one before we get any data, we need to remember the
 							// updated interest in the listener
 							manager.expressInterest(this.owner, updatedInterest, listener);
 						}
 					
 					} else {
 						Library.logger().finer("Interest callback skipped (no data) for: " + this.interest.name());
 					}
 				} else {
 					synchronized (this) {
 						if (null != this.sema) {
 							Library.logger().finer("Data consumes pending get: " + this.interest.name());
 							// Waiting thread will pickup data -- wake it up
 							// If this interest came from net or waiting thread timed out,
 							// then no thread will be waiting but no harm is done
 							Library.logger().finest("releasing " + this.sema);
 							this.sema.release();
 						} 
 					}
 					if (null == this.sema) {
 						// this is no longer valid registration
 						Library.logger().finer("Interest callback skipped (not valid) for: " + this.interest.name());
 					}
 				}
 			} catch (Exception ex) {
 				Library.logger().warning("failed to deliver data: " + ex.toString());
 				Library.warningStackTrace(ex);
 			}
 		}
 		
 		public void run() {
 			synchronized (this) {
 				// For now only one piece of data may be delivered per InterestRegistration
 				// This might change when "pipelining" is implemented
 				if (deliveryPending)
 					return;
 			}
 			super.run();
 		}
 	}
 	
 	/**
 	 * Record of a filter describing portion of namespace for which this 
 	 * application can respond to interests.
 	 * @author jthornto
 	 *
 	 */
 	protected class Filter extends ListenerRegistration {
 		public ContentName name;
 		protected boolean deliveryPending = false;
 		protected ArrayList<Interest> interests= new ArrayList<Interest>(1);
 		public Filter(CCNNetworkManager mgr, ContentName n, CCNFilterListener l, Object o) {
 			name = n; listener = l; owner = o;
 			manager = mgr;
 		}
 		public synchronized void add(Interest i) {
 			interests.add(i);
 		}
 		public void deliver() {
 			try {
 				ArrayList<Interest> results = null;
 				CCNFilterListener listener = null;
 				synchronized (this) {
 					if (this.interests.size() > 0) { 
 						results = interests;
 						interests = new ArrayList<Interest>(1);
 						listener = (CCNFilterListener)this.listener;
 					}
 				}
 				
 				if (null != results) {								
 					// Call into client code without holding any library locks
 					Library.logger().finer("Filter callback (" + results.size() + " interests) for: " + name);
 					listener.handleInterests(results);
 				} else {
 					Library.logger().finer("Filter callback skipped (no interests) for: " + name);
 				}
 			} catch (RuntimeException ex) {
 				Library.logger().warning("failed to deliver interest: " + ex.toString());
 			}
 		}
 	}
 	
 	
 	/***************************************************************
 	 * NOTE: former statics getNetworkManager(), createNetworkManager()
 	 * are no longer appropriate given that there are multiple instances
 	 * of a network manager (one per library instance)
 	 ***************************************************************/
 	
 	public CCNNetworkManager() throws IOException {
 		// Determine port at which to contact agent
 		int port = DEFAULT_AGENT_PORT;
 		String host = DEFAULT_AGENT_HOST;
 		String portval = System.getProperty(PROP_AGENT_PORT);
 		if (null != portval) {
 			try {
 				port = new Integer(portval);
 			} catch (Exception ex) {
 				throw new IOException("Invalid port '" + portval + "' specified in " + PROP_AGENT_PORT);
 			}
 			Library.logger().warning("Non-standard CCN agent port " + port + " per property " + PROP_AGENT_PORT);
 		}
 		String hostval = System.getProperty(PROP_AGENT_HOST);
 		if (null != hostval && hostval.length() > 0) {
 			host = hostval;
 			Library.logger().warning("Non-standard CCN agent host " + host + " per property " + PROP_AGENT_HOST);
 		}
 		Library.logger().info("Contacting CCN agent at " + host + ":" + port);
 		
 		String tapname = System.getProperty(PROP_TAP);
 		if (null == tapname) {
 			tapname = System.getenv(ENV_TAP);
 		}
 		if (null != tapname) {
 			long msecs = new Date().getTime();
 			long secs = msecs/1000;
 			msecs = msecs % 1000;
 			String unique_tapname = tapname + "-T" + Thread.currentThread().getId() +
 								    "-" + secs + "-" + msecs;
 			setTap(unique_tapname);
 		}
 		
 		// Socket is to belong exclusively to run thread started here
 		_channel = DatagramChannel.open();
 		_channel.connect(new InetSocketAddress(host, port));
 		_channel.configureBlocking(false);
 		_selector = Selector.open();
 		_channel.register(_selector, SelectionKey.OP_READ);
 		
 		// Create callback threadpool and main processing thread
 		_threadpool = (ThreadPoolExecutor)Executors.newCachedThreadPool();
 		_threadpool.setKeepAliveTime(THREAD_LIFE, TimeUnit.SECONDS);
 		_thread = new Thread(this);
 		_thread.start();
 	}
 	
 	/**
 	 * 
 	 * @param putWait - wait for all puts to be output before
 	 * 					shutting down the server.
 	 */
 	public void shutdown() {
 		Library.logger().info("Shutdown requested");
 		_run = false;
 		if (_periodicTimer != null)
 			_periodicTimer.cancel();
 		_selector.wakeup();
 		try {
 			setTap(null);
 		} catch (IOException io) {
 			// Ignore since we're shutting down
 		}
 	}
 	
 	/**
 	 * Turn on writing of all packets to a file for test/debug
 	 * Overrides any previous setTap or environment/property setting.
 	 * Pass null to turn off tap.
 	 * @param filename
 	 */
 	public void setTap(String pathname) throws IOException {
 		// Turn off any active tap
 		if (null != _tapStreamOut) {
 			FileOutputStream closingStream = _tapStreamOut;
 			_tapStreamOut = null;
 			closingStream.close();
 		}
 		if (null != _tapStreamIn) {
 			FileOutputStream closingStream = _tapStreamIn;
 			_tapStreamIn = null;
 			closingStream.close();
 		}
 
 		if (pathname != null && pathname.length() > 0) {
 			_tapStreamOut = new FileOutputStream(new File(pathname + "_out"));
 			_tapStreamIn = new FileOutputStream(new File(pathname + "_in"));
 			Library.logger().info("Tap writing to " + pathname);
 		}
 	}
 	
 	public ContentObject put(ContentObject co) throws IOException, InterruptedException {	
 		try {
 			write(co);
 		} catch (XMLStreamException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
 		return co;
 		//return CCNRepositoryManager.getRepositoryManager().put(name, signedInfo, signature, content);
 	}
 	
 	public ContentObject get(Interest interest, long timeout) throws IOException, InterruptedException {
 		Library.logger().fine("get: " + interest.name());
 		InterestRegistration reg = new InterestRegistration(this, interest, null, null);
 		expressInterest(reg);
 		Library.logger().finest("blocking for " + interest.name() + " on " + reg.sema);
 		// Await data to consume the interest
 		if (timeout == CCNBase.NO_TIMEOUT)
 			reg.sema.acquire(); // currently no timeouts
 		else
 			reg.sema.tryAcquire(timeout, TimeUnit.MILLISECONDS);
 		Library.logger().finest("unblocked for " + interest.name() + " on " + reg.sema);
 		// Typically the main processing thread will have registered the interest
 		// which must be undone here, but no harm if never registered
 		unregisterInterest(reg);
 		ArrayList<ContentObject> result = reg.popData();
 		return result.size() > 0 ? result.get(0) : null;
 	}
 
 
 	/**
 	 * We express interests to the ccnd and register them within the network manager
 	 */
 	public void expressInterest(
 			Object caller,
 			Interest interest,
 			CCNInterestListener callbackListener) throws IOException {
 		if (null == callbackListener) {
 			throw new NullPointerException("expressInterest: callbackListener cannot be null");
 		}		
 	
 		Library.logger().fine("expressInterest: " + interest.name());
 		InterestRegistration reg = new InterestRegistration(this, interest, callbackListener, caller);
 		expressInterest(reg);
 	}
 	
 	private void expressInterest(InterestRegistration reg) throws IOException {
 		try {
 			registerInterest(reg);
 			write(reg.interest);
 		} catch (XMLStreamException e) {
 			unregisterInterest(reg);
 			throw new IOException(e.getMessage());
 		}
 	}
 	
 	/**
 	 * Cancel this query with all the repositories we sent
 	 * it to.
 	 */
 	public void cancelInterest(Object caller, Interest interest, CCNInterestListener callbackListener) {
 		if (null == callbackListener) {
 			throw new NullPointerException("cancelInterest: callbackListener cannot be null");
 		}
 	
 		Library.logger().fine("cancelInterest: " + interest.name());
 		// Remove interest from repeated presentation to the network.
 		unregisterInterest(caller, interest, callbackListener);
 	}
 
 	
 	/**
 	 * Register a standing interest filter with callback to receive any 
 	 * matching interests seen
 	 */
 	public void setInterestFilter(Object caller, ContentName filter, CCNFilterListener callbackListener) {
 		//Library.logger().fine("setInterestFilter: " + filter);
 		setupTimers();
 		synchronized (_myFilters) {
 			_myFilters.add(filter, new Filter(this, filter, callbackListener, caller));
 		}
 	}
 	
 	/**
 	 * Unregister a standing interest filter
 	 */
 	public void cancelInterestFilter(Object caller, ContentName filter, CCNFilterListener callbackListener) {
 		Library.logger().fine("cancelInterestFilter: " + filter);
 		synchronized (_myFilters) {
 			Entry<Filter> found = _myFilters.remove(filter, new Filter(this, filter, callbackListener, caller));
 			if (null != found) {
 				found.value().invalidate();
 			}
 		}
 	}
 	
 	protected void write(ContentObject data) throws XMLStreamException {
 		WirePacket packet = new WirePacket(data);
 		writeInner(packet);
 		Library.logger().finest("Wrote content object: " + data.name());
 	}
 
 	protected void write(Interest interest) throws XMLStreamException {
 		WirePacket packet = new WirePacket(interest);
 		writeInner(packet);
 	}
 
 	private void writeInner(WirePacket packet) throws XMLStreamException {
 		try {
 			byte[] bytes = packet.encode();
 			ByteBuffer datagram = ByteBuffer.wrap(bytes);
 			synchronized (_channel) {
 				int result = _channel.write(datagram);
 				Library.logger().finest("Wrote datagram (" + datagram.position() + " bytes, result " + result + ")");
 				if (null != _tapStreamOut) {
 					try {
 						_tapStreamOut.write(bytes);
 					} catch (IOException io) {
 						Library.logger().warning("Unable to write packet to tap stream for debugging");
 					}
 				}
 			}
 		} catch (IOException io) {
 			// We do not see errors on send typically even if 
 			// agent is gone, so log each but do not track
 			Library.logger().warning("Error sending packet: " + io.toString());
 		}
 	}
 
 
 	/**
 	 * Pass things on to the network stack.
 	 */
 	InterestRegistration registerInterest(InterestRegistration reg) {
 		// Add to standing interests table
 		setupTimers();
 		Library.logger().finest("registerInterest for " + reg.interest.name() + " and obj is " + _myInterests.hashCode());
 		synchronized (_myInterests) {
 			_myInterests.add(reg.interest, reg);
 		}
 		return reg;
 	}
 	
 	// external version: for use when we only have interest from client.  For all internal
 	// purposes we should unregister the InterestRegistration we already have
 	void unregisterInterest(Object caller, Interest interest, CCNInterestListener callbackListener) {
 		InterestRegistration reg = new InterestRegistration(this, interest, callbackListener, caller);
 		unregisterInterest(reg);
 	}
 	
 	/**
 	 * @param reg - registration to unregister
 	 * 
 	 * Important Note: This can indirectly need to obtain the lock for "reg" with the lock on
 	 * "myInterests" held.  Therefore it can't be called when holding the lock for "reg".
 	 */
 	void unregisterInterest(InterestRegistration reg) {
 		synchronized (_myInterests) {
 			Entry<InterestRegistration> found = _myInterests.remove(reg.interest, reg);
 			if (null != found) {
 				found.value().invalidate();
 			}
 		}		
 	}
 
 	// Thread method: this thread will handle reading datagrams and 
 	// the periodic re-expressing of standing interests
 	public void run() {
 		if (! _run) {
 			Library.logger().warning("CCNSimpleNetworkManager run() called after shutdown");
 			return;
 		}
 		// Allocate datagram buffer: want to wrap array to ensure backed by
 		// array to permit decoding
 		byte[] buffer = new byte[MAX_PAYLOAD];
 		ByteBuffer datagram = ByteBuffer.wrap(buffer);
 		WirePacket packet = new WirePacket();
 		Library.logger().info("CCNSimpleNetworkManager processing thread started");
 		while (_run) {
 			try {
 				
 				//--------------------------------- Read and decode
 				try {
 					if (_selector.select(SOCKET_TIMEOUT) != 0) {
 						// Note: we're selecting on only one channel to get
 						// the ability to use wakeup, so there is no need to 
 						// inspect the selected-key set
 						datagram.clear(); // make ready for new read
 						synchronized (_channel) {
 							_channel.read(datagram); // queue readers and writers
 						}
 						Library.logger().finest("Read datagram (" + datagram.position() + " bytes)");
 						_selector.selectedKeys().clear();
 						if (null != _error) {
 							Library.logger().info("Receive error cleared");
 							_error = null;
 						}
 						datagram.flip(); // make ready to decode
 						if (null != _tapStreamIn) {
 							byte [] b = new byte[datagram.limit()];
 							datagram.get(b);
 							_tapStreamIn.write(b);
 							datagram.rewind();
 						}
 						packet.clear();
 						packet.decode(datagram);
 					} else {
 						// This was a timeout or wakeup, no data
 						packet.clear();
 						if (!_run) {
 							// exit immediately if wakeup for shutdown
 							break;
 						}
 					}
 				} catch (IOException io) {
 					// We see IOException on receive every time if agent is gone
 					// so track it to log only start and end of outages
 					if (null == _error) {
 						Library.logger().info("Unable to receive from agent: is it still running?");
 					}
 					_error = io;
 					packet.clear();
 				}
                 if (!_run) {
                     // exit immediately if wakeup for shutdown
                     break;
                 }
                 
                 // If we got a data packet, hand it back to all the interested
 				// parties (registered interests and getters).
 				//--------------------------------- Process data from net (if any) 
 				for (ContentObject co : packet.data()) {
 					Library.logger().fine("Data from net: " + co.name());
 					//	SystemConfiguration.logObject("Data from net:", co);
 					
 					deliverData(co);
 					// External data never goes back to network, never held onto here
 					// External data never has a thread waiting, so no need to release sema
 				}
 
 				//--------------------------------- Process interests from net (if any)
 				for (Interest interest : packet.interests()) {
 					Library.logger().fine("Interest from net: " + interest.print());
 					InterestRegistration oInterest = new InterestRegistration(this, interest, null, null);
 					deliverInterest(oInterest);
 					// External interests never go back to network
 				} // for interests
 				
 			} catch (Exception ex) {
 				Library.logger().severe("Processing thread failure (UNKNOWN): " + ex.getMessage());
                                 Library.warningStackTrace(ex);
 			}
 		}
 		
 		_threadpool.shutdown();
 		Library.logger().info("Shutdown complete");
 	}
 
 	// Internal delivery of interests to pending filter listeners
 	protected void deliverInterest(InterestRegistration ireg) throws XMLStreamException {
 		// Call any listeners with matching filters
 		synchronized (_myFilters) {
 			for (Filter filter : _myFilters.getValues(ireg.interest.name())) {
 				if (filter.owner != ireg.owner) {
 					Library.logger().finer("Schedule delivery for interest: " + ireg.interest.print());
 					filter.add(ireg.interest);
 					_threadpool.execute(filter);
 				}
 			}
 		}
 	}
 
 	// Deliver data to blocked getters and registered interests
 	protected void deliverData(ContentObject co) throws XMLStreamException {
 		synchronized (_myInterests) {
 			for (InterestRegistration ireg : _myInterests.getValues(co)) {
 				if (ireg.add(co)) { // this is a copy of the data
 					_threadpool.execute(ireg);
 				}
 			}
 		}
 	}
 }
