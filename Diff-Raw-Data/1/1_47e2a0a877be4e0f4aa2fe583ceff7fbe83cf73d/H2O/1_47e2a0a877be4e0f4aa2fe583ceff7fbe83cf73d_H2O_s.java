 package water;
 import java.io.*;
 import java.net.*;
 import java.nio.ByteBuffer;
 import java.nio.channels.DatagramChannel;
 import java.util.*;
 import java.util.concurrent.atomic.AtomicReference;
 
 import jsr166y.ForkJoinPool;
 import jsr166y.ForkJoinWorkerThread;
 import water.exec.Function;
 import water.hdfs.HdfsLoader;
 import water.nbhm.NonBlockingHashMap;
 import water.store.s3.PersistS3;
 import water.util.Utils;
 
 import com.google.common.base.Strings;
 import com.google.common.collect.Lists;
 import com.google.common.collect.Sets;
 import com.google.common.io.Closeables;
 
 /**
 * Start point for creating or joining an <code>H2O</code> Cloud.
 *
 * @author <a href="mailto:cliffc@0xdata.com"></a>
 * @version 1.0
 */
 public final class H2O {
 
   static boolean _hdfsActive = false;
 
   static final String VERSION = "v0.3";
 
   // User name for this Cloud
   public static String NAME;
 
   // The default port for finding a Cloud
   static final int DEFAULT_PORT = 54321;
   public static int UDP_PORT; // Fast/small UDP transfers
   public static int API_PORT; // RequestServer and the new API HTTP port
 
   // The multicast discovery port
   static MulticastSocket  CLOUD_MULTICAST_SOCKET;
   static NetworkInterface CLOUD_MULTICAST_IF;
   static InetAddress      CLOUD_MULTICAST_GROUP;
   static int              CLOUD_MULTICAST_PORT ;
   // Default NIO Datagram channel
   static DatagramChannel  CLOUD_DGRAM;
 
   // Myself, as a Node in the Cloud
   public static H2ONode SELF = null;
 
   // Initial arguments
   public static String[] ARGS;
 
   public static final PrintStream OUT = System.out;
   public static final PrintStream ERR = System.err;
 
   // Convenience error
   public static final RuntimeException unimpl() { return new RuntimeException("unimplemented"); }
 
   // --------------------------------------------------------------------------
   // The Current Cloud. A list of all the Nodes in the Cloud. Changes if we
   // decide to change Clouds via atomic Cloud update.
   static public volatile H2O CLOUD;
 
   // ---
   // A Set version of the members.
   public final HashSet<H2ONode> _memset;
   // A dense array indexing all Cloud members. Fast reversal from "member#" to
   // Node. No holes. Cloud size is _members.length.
   public final H2ONode[] _memary;
   // UUID to uniquely identify this cloud during paxos voting
   public final UUID _id;
   // A dense integer identifier that rolls over rarely. Rollover limits the
   // number of simultaneous nested Clouds we are operating on in-parallel.
   // Really capped to 1 byte, under the assumption we won't have 256 nested
   // Clouds. Capped at 1 byte so it can be part of an atomically-assigned
   // 'long' holding info specific to this Cloud.
   public final char _idx; // no unsigned byte, so unsigned char instead
 
   // Is nnn larger than old (counting for wrap around)? Gets confused if we
   // start seeing a mix of more than 128 unique clouds at the same time. Used
   // to tell the order of Clouds appearing.
   static public boolean larger( int nnn, int old ) {
     assert (0 <= nnn && nnn <= 255);
     assert (0 <= old && old <= 255);
     return ((nnn-old)&0xFF) < 64;
   }
 
   // Static list of acceptable Cloud members
   static HashSet<H2ONode> STATIC_H2OS = null;
 
   // Reverse cloud index to a cloud; limit of 256 old clouds.
   static private final H2O[] CLOUDS = new H2O[256];
 
   // Construct a new H2O Cloud from the member list
   public H2O( UUID cloud_id, HashSet<H2ONode> memset, int idx ) {
     _id = cloud_id; // Set the Cloud identity
     _memset = memset; // Record membership list
     _memary = memset.toArray(new H2ONode[memset.size()]); // As an array
     Arrays.sort(_memary); // ... sorted!
     _idx = (char)(idx&0x0ff); // Roll-over at 256
   }
 
   // One-shot atomic setting of the next Cloud, with an empty K/V store.
   // Called single-threaded from Paxos. Constructs the new H2O Cloud from a
   // member list.
   void set_next_Cloud( UUID id, HashSet<H2ONode> members) {
     synchronized(this) {
       int idx = _idx+1; // Unique 1-byte Cloud index
       if( idx == 256 ) idx=1; // wrap, avoiding zero
       H2O cloud = CLOUD = new H2O(id,members,idx);
       CLOUDS[idx] = cloud; // Also remember here
     }
     Paxos.print("Announcing new Cloud Membership: ",members,"");
   }
 
   // Check if the cloud id matches with one of the old clouds
   static boolean isIDFromPrevCloud(H2ONode h2o) {
     if ( h2o == null ) return false;
     HeartBeat hb = h2o._heartbeat;
     long lo = hb._cloud_id_lo;
     long hi = hb._cloud_id_hi;
     for( int i=0; i < 256; i++ )
       if( (CLOUDS[i] != null) &&
           (lo == CLOUDS[i]._id.getLeastSignificantBits() &&
            hi == CLOUDS[i]._id.getMostSignificantBits()))
         return true;
     return false;
   }
 
   public final int size() { return _memary.length; }
 
   // *Desired* distribution function on keys & replication factor. Replica #0
   // is the master, replica #1, 2, 3, etc represent additional desired
   // replication nodes. Note that this function is just the distribution
   // function - it does not DO any replication, nor does it dictate any policy
   // on how fast replication occurs. Returns -1 if the desired replica
   // is nonsense, e.g. asking for replica #3 in a 2-Node system.
   public int D( Key key, int repl ) {
     if( repl >= size() ) return -1;
 
     // See if this is a specifically homed Key
     byte[] kb = key._kb;
     if( !key.user_allowed() && repl < kb[1] ) { // Asking for a replica# from the homed list?
       H2ONode h2o=null, h2otmp = new H2ONode(); // Fill in the fields from the Key
       AutoBuffer ab = new AutoBuffer(kb,2);
       for( int i=0; i<=repl; i++ )
         h2o = h2otmp.read(ab);  // Read util we get the specified H2O
       // Reverse the home to the index
       int idx = nidx(h2o);
       if( idx != -1 ) return idx;
       // Else homed to a node which is no longer in the cloud!
       // Fall back to the normal home mode
     }
 
     // Easy Cheesy Stupid:
     return ((key._hash+repl)&0x7FFFFFFF) % size();
   }
 
   // Find the node index for this H2ONode. Not so cheap.
   public int nidx( H2ONode h2o ) {
     for( int i=0; i<_memary.length; i++ )
       if( _memary[i]==h2o )
         return i;
     return -1;
   }
 
   static InetAddress findInetAddressForSelf() throws Error {
     // Get a list of all valid IPs on this machine.  Typically 1 on Mac or
     // Windows, but could be many on Linux or if a hypervisor is present.
     ArrayList<InetAddress> ips = new ArrayList<InetAddress>();
     try {
       Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
       while( nis.hasMoreElements() ) {
         NetworkInterface ni = nis.nextElement();
         Enumeration<InetAddress> ias = ni.getInetAddresses();
         while( ias.hasMoreElements() ) {
           ips.add(ias.nextElement());
         }
       }
     } catch( SocketException e ) { }
 
     InetAddress local = null;   // My final choice
 
     // Check for an "-ip xxxx" option and accept a valid user choice; required
     // if there are multiple valid IP addresses.
     InetAddress arg = null;
     if (OPT_ARGS.ip != null) {
       try{
         arg = InetAddress.getByName(OPT_ARGS.ip);
       } catch( UnknownHostException e ) {
         System.err.println(e.toString());
         System.exit(-1);
       }
       if( !(arg instanceof Inet4Address) ) {
         System.err.println("Only IP4 addresses allowed.");
         System.exit(-1);
       }
       if( !ips.contains(arg) ) {
         System.err.println("IP address not found on this machine");
         System.exit(-1);
       }
       local = arg;
     } else {
       // No user-specified IP address.  Attempt auto-discovery.  Roll through
       // all the network choices on looking for a single Inet4.
       List<InetAddress> validIps = Lists.newArrayList();
       for( InetAddress ip : ips ) {
         // make sure the given IP address can be found here
         if( ip instanceof Inet4Address &&
             !ip.isLoopbackAddress() &&
             !ip.isLinkLocalAddress() ) {
           validIps.add(ip);
         }
       }
       if( validIps.size() == 1 ) {
         local = validIps.get(0);
       } else {
         local = guessInetAddress(validIps);
       }
     }
 
     // The above fails with no network connection, in that case go for a truly
     // local host.
     if( local == null ) {
       try {
         System.err.println("Failed to determine IP, falling back to localhost.");
         // set default ip address to be 127.0.0.1 /localhost
         local = InetAddress.getByName("127.0.0.1");
       } catch( UnknownHostException e ) {
         throw new Error(e);
       }
     }
     return local;
   }
 
   private static InetAddress guessInetAddress(List<InetAddress> ips) {
     System.err.println("Multiple local IPs detected:");
     for(InetAddress ip : ips) System.err.println("  " + ip);
     System.err.println("Attempting to determine correct address...");
     Socket s = null;
     try {
       // using google's DNS server as an external IP to find
       s = new Socket("8.8.8.8", 53);
       System.err.println("Using " + s.getLocalAddress());
       return s.getLocalAddress();
     } catch( Throwable t ) {
       return null;
     } finally {
       Utils.close(s);
     }
   }
 
   // --------------------------------------------------------------------------
   // The (local) set of Key/Value mappings.
   static final NonBlockingHashMap<Key,Value> STORE = new NonBlockingHashMap<Key, Value>();
 
   // Dummy shared volatile for ordering games
   static public volatile int VOLATILE;
 
   // PutIfMatch
   // - Atomically update the STORE, returning the old Value on success
   // - Kick the persistence engine as needed
   // - Return existing Value on fail, no change.
   //
   // Keys are interned here: I always keep the existing Key, if any. The
   // existing Key is blind jammed into the Value prior to atomically inserting
   // it into the STORE and interning.
   //
   // Because of the blind jam, there is a narrow unusual race where the Key
   // might exist but be stale (deleted, mapped to a TOMBSTONE), a fresh put()
   // can find it and jam it into the Value, then the Key can be deleted
   // completely (e.g. via an invalidate), the table can resize flushing the
   // stale Key, an unrelated weak-put can re-insert a matching Key (but as a
   // new Java object), and delete it, and then the original thread can do a
   // successful put_if_later over the missing Key and blow the invariant that a
   // stored Value always points to the physically equal Key that maps to it
   // from the STORE. If this happens, some of replication management bits in
   // the Key will be set in the wrong Key copy... leading to extra rounds of
   // replication.
 
   public static final Value putIfMatch( Key key, Value val, Value old ) {
     assert val==null || val._key == key; // Keys matched
     if( old != null && val != null ) // Have an old value?
       key = val._key = old._key; // Use prior key in val
 
     // Insert into the K/V store
     Value res = STORE.putIfMatchUnlocked(key,val,old);
     assert chk_equals_key(res, old);
     if( res != old ) return res; // Return the failure cause
     assert chk_equals_key(res, old, val);
     // Persistence-tickle.
     // If the K/V mapping is going away, remove the old guy.
     // If the K/V mapping is changing, let the store cleaner just overwrite.
     // If the K/V mapping is new, let the store cleaner just create
     if( old != null && val == null ) old.removeIce(); // Remove the old guy
     if( val != null ) dirty_store(); // Start storing the new guy
     return old; // Return success
   }
 
   // assert that all of val, old & res that are not-null all agree on key.
   private static final boolean chk_equals_key( Value... vs ) {
     Key k = null;
     for( Value v : vs ) {
       if( v != null ) {
         assert k == null || k == v._key;
         k = v._key;
       }
     }
     return true;
   }
 
   // Raw put; no marking the memory as out-of-sync with disk. Used to import
   // initial keys from local storage, or to intern keys.
   public static final Value putIfAbsent_raw( Key key, Value val ) {
     assert val.isSameKey(key);
     Value res = STORE.putIfMatchUnlocked(key,val,null);
     assert res == null;
     return res;
   }
 
   // Get the value from the store
   public static Value get( Key key ) {
     Value v = STORE.get(key);
     // Lazily manifest array chunks, if the backing file exists.
     if( v == null ) {
       v = Value.lazyArrayChunk(key);
       if( v == null ) return null;
       // Insert the manifested value, as-if it existed all along
       Value res = putIfMatch(key,v,null);
       if( res != null ) v = res; // This happens racily, so take any prior result
     }
     if( v != null ) v.touch();
     return v;
   }
 
   public static Value raw_get( Key key ) { return STORE.get(key); }
   public static Key getk( Key key ) { return STORE.getk(key); }
   public static Set<Key> keySet( ) { return STORE.keySet(); }
   public static Collection<Value> values( ) { return STORE.values(); }
   public static int store_size() { return STORE.size(); }
 
 
   // --------------------------------------------------------------------------
   // The main Fork/Join worker pool(s).
   static class FJWThr extends ForkJoinWorkerThread {
     FJWThr(ForkJoinPool pool, int priority) {
       super(pool);
       setPriority(priority);
     }
   }
   static class FJWThrFact implements ForkJoinPool.ForkJoinWorkerThreadFactory {
     final int _priority;
     FJWThrFact( int priority ) { _priority = priority; }
     public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
       // The "Normal" or "Low" priority work queues get capped at 99 threads
       // blocked on I/O. I/O work *should* be done by HI priority threads on
       // other nodes - so hopefully this will not lead to deadlock. Capping
       // all thread pools definitely does lead to deadlock.
       return (_priority > Thread.MIN_PRIORITY || pool.getPoolSize() < 100)
         ? new FJWThr(pool,_priority) : null;
     }
   }
   // Hi-priority work is things that block other things, eg. TaskGetKey, and
   // typically does I/O.
   public static final ForkJoinPool FJP_HI =
     new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                      new FJWThrFact(Thread.MAX_PRIORITY-2), null, false);
 
   // Normal-priority work is generally directly-requested user ops.
   public static final ForkJoinPool FJP_NORM =
     new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                      new FJWThrFact(Thread.MIN_PRIORITY), null, false);
 
   // --------------------------------------------------------------------------
   public static OptArgs OPT_ARGS = new OptArgs();
   public static class OptArgs extends Arguments.Opt {
     public String name; // set_cloud_name_and_mcast()
     public String flatfile; // set_cloud_name_and_mcast()
     public int port; // set_cloud_name_and_mcast()
     public String ip; // Named IP4/IP6 address instead of the default
     public String ice_root; // ice root directory
     public String hdfs; // HDFS backend
     public String hdfs_version; // version of the filesystem
     public String hdfs_config; // configuration file of the HDFS
     public String aws_credentials; // properties file for aws credentials
     public String keepice; // Do not delete ice on startup
     public String soft = null; // soft launch for demos
     public String random_udp_drop = null; // test only, randomly drop udp incoming
     public String log_headers = null; // add machine name, PID and time to logs
   }
   public static boolean IS_SYSTEM_RUNNING = false;
 
   // Start up an H2O Node and join any local Cloud
   public static void main( String[] args ) {
     // To support launching from JUnit, JUnit expects to call main() repeatedly.
     // We need exactly 1 call to main to startup all the local services.
     if (IS_SYSTEM_RUNNING) return;
     IS_SYSTEM_RUNNING = true;
 
     // Parse args
     Arguments arguments = new Arguments(args);
     arguments.extract(OPT_ARGS);
     ARGS = arguments.toStringArray();
 
     if(OPT_ARGS.log_headers != null)
       Log.initHeaders();
 
     startLocalNode(); // start the local node
     // Load up from disk and initialize the persistence layer
     initializePersistence();
     // Start network services, including heartbeats & Paxos
     startNetworkServices(); // start server services
 
     initializeExpressionEvaluation(); // starts the expression evaluation system
 
     startupFinalize(); // finalizes the startup & tests (if any)
     // Hang out here until the End of Time
   }
 
   private static void initializeExpressionEvaluation() {
     Function.initializeCommonFunctions();
   }
 
   /** Starts the local k-v store.
 * Initializes the local k-v store, local node and the local cloud with itself
 * as the only member.
 *
 * @param args Command line arguments
 * @return Unprocessed command line arguments for further processing.
 */
   private static void startLocalNode() {
     // Figure self out; this is surprisingly hard
     initializeNetworkSockets();
     // Do not forget to put SELF into the static configuration (to simulate
     // proper multicast behavior)
     if( STATIC_H2OS != null && !STATIC_H2OS.contains(SELF)) {
       System.err.println("[h2o] *WARNING* flatfile configuration does not include self: " + SELF);
       System.err.println("[h2o] *WARNING* flatfile contains: " + STATIC_H2OS);
       STATIC_H2OS.add(SELF);
     }
 
     System.out.println("[h2o] ("+VERSION+") '"+NAME+"' on " + SELF+
                        (OPT_ARGS.flatfile==null
                         ? (", discovery address "+CLOUD_MULTICAST_GROUP+":"+CLOUD_MULTICAST_PORT)
                         : ", static configuration based on -flatfile "+OPT_ARGS.flatfile));
 
     // Create the starter Cloud with 1 member
     CLOUD = new H2O(UUID.randomUUID(), Sets.newHashSet(SELF), 1);
   }
 
   /** Initializes the network services of the local node.
 *
 * Starts the worker threads, receiver threads, heartbeats and all other
 * network related services.
 */
   private static void startNetworkServices() {
     // We've rebooted the JVM recently. Tell other Nodes they can ignore task
     // prior tasks by us. Do this before we receive any packets
     UDPRebooted.T.reboot.broadcast();
 
     // Start the UDPReceiverThread, to listen for requests from other Cloud
     // Nodes. There should be only 1 of these, and it never shuts down.
     // Started first, so we can start parsing UDP packets
     new UDPReceiverThread().start();
 
     // Start the MultiReceiverThread, to listen for multi-cast requests from
     // other Cloud Nodes. There should be only 1 of these, and it never shuts
     // down. Started soon, so we can start parsing multicast UDP packets
     new MultiReceiverThread().start();
 
     // Start the Persistent meta-data cleaner thread, which updates the K/V
     // mappings periodically to disk. There should be only 1 of these, and it
     // never shuts down.  Needs to start BEFORE the HeartBeatThread to build
     // an initial histogram state.
     new Cleaner().start();
 
     // Start the heartbeat thread, to publish the Clouds' existence to other
     // Clouds. This will typically trigger a round of Paxos voting so we can
     // join an existing Cloud.
     new HeartBeatThread().start();
 
     // Start a UDP timeout worker thread. This guy only handles requests for
     // which we have not recieved a timely response and probably need to
     // arrange for a re-send to cover a dropped UDP packet.
     new UDPTimeOutThread().start();
 
     // Start the TCPReceiverThread, to listen for TCP requests from other Cloud
     // Nodes. There should be only 1 of these, and it never shuts down.
     new TCPReceiverThread().start();
     water.api.RequestServer.start();
   }
 
   /** Finalizes the node startup.
    *
    * Displays the startup message and runs the tests (if applicable).
    */
   private static void startupFinalize() {
     // Sleep a bit so all my other threads can 'catch up'
     try { Thread.sleep(1000); } catch( InterruptedException e ) { }
   }
 
   public static DatagramChannel _udpSocket;
   public static ServerSocket _apiSocket;
 
 
   // Parse arguments and set cloud name in any case. Strip out "-name NAME"
   // and "-flatfile <filename>". Ignore the rest. Set multi-cast port as a hash
   // function of the name. Parse node ip addresses from the filename.
   static void initializeNetworkSockets( ) {
     // Assign initial ports
     InetAddress inet = findInetAddressForSelf();
     API_PORT = OPT_ARGS.port != 0 ? OPT_ARGS.port : DEFAULT_PORT;
 
     while (true) {
       UDP_PORT = API_PORT+1;
       try {
         // kbn. seems like we need to set SO_REUSEADDR before binding?
         // http://www.javadocexamples.com/java/net/java.net.ServerSocket.html#setReuseAddress:boolean
         // When a TCP connection is closed the connection may remain in a timeout state
         // for a period of time after the connection is closed (typically known as the
         // TIME_WAIT state or 2MSL wait state). For applications using a well known socket address
         // or port it may not be possible to bind a socket to the required SocketAddress
         // if there is a connection in the timeout state involving the socket address or port.
         // Enabling SO_REUSEADDR prior to binding the socket using bind(SocketAddress)
         // allows the socket to be bound even though a previous connection is in a timeout state.
         // cnc: this is busted on windows.  Back to the old code.
         _apiSocket = new ServerSocket(API_PORT);
         _udpSocket = DatagramChannel.open();
         _udpSocket.socket().setReuseAddress(true);
         _udpSocket.socket().bind(new InetSocketAddress(inet, UDP_PORT));
         break;
       } catch (IOException e) {
         try { if( _apiSocket != null ) _apiSocket.close(); } catch( IOException ohwell ) { }
         Closeables.closeQuietly(_udpSocket);
         _apiSocket = null;
         _udpSocket = null;
         if( OPT_ARGS.port != 0 )
           Log.die("On " + H2O.findInetAddressForSelf() +
               " some of the required ports " + (OPT_ARGS.port+0) +
               ", " + (OPT_ARGS.port+1) +
              ", " + (OPT_ARGS.port+2) +
               " are not available, change -port PORT and try again.");
       }
       API_PORT += 2;
     }
     SELF = H2ONode.self(inet);
     System.out.println("[h2o] Internal communication uses port: "+UDP_PORT);
     System.out.println("[h2o] Listening for HTTP and REST traffic on");
     System.out.println("[h2o]\t\thttp:/"+inet+":"+_apiSocket.getLocalPort()+"/");
 
     NAME = OPT_ARGS.name==null? System.getProperty("user.name") : OPT_ARGS.name;
     // Read a flatfile of allowed nodes
     STATIC_H2OS = parseFlatFile(OPT_ARGS.flatfile);
 
     // Multi-cast ports are in the range E1.00.00.00 to EF.FF.FF.FF
     int hash = NAME.hashCode()&0x7fffffff;
     int port = (hash % (0xF0000000-0xE1000000))+0xE1000000;
     byte[] ip = new byte[4];
     for( int i=0; i<4; i++ )
       ip[i] = (byte)(port>>>((3-i)<<3));
     try {
       CLOUD_MULTICAST_GROUP = InetAddress.getByAddress(ip);
     } catch( UnknownHostException e ) { throw new Error(e); }
     CLOUD_MULTICAST_PORT = (port>>>16);
   }
 
   // Multicast send-and-close.  Very similar to udp_send, except to the
   // multicast port (or all the individuals we can find, if multicast is
   // disabled).
   static void multicast( ByteBuffer bb ) {
     if( H2O.STATIC_H2OS == null ) {
       byte[] buf = new byte[bb.remaining()];
       bb.get(buf);
 
       synchronized( H2O.class ) { // Sync'd so single-thread socket create/destroy
         assert H2O.CLOUD_MULTICAST_IF != null;
         try {
           if( CLOUD_MULTICAST_SOCKET == null ) {
             CLOUD_MULTICAST_SOCKET = new MulticastSocket();
             // Allow multicast traffic to go across subnets
             CLOUD_MULTICAST_SOCKET.setTimeToLive(2);
             CLOUD_MULTICAST_SOCKET.setNetworkInterface(H2O.CLOUD_MULTICAST_IF);
           }
           // Make and send a packet from the buffer
           CLOUD_MULTICAST_SOCKET.send(new DatagramPacket(buf, buf.length, CLOUD_MULTICAST_GROUP,CLOUD_MULTICAST_PORT));
         } catch( Exception e ) {
           // On any error from anybody, close all sockets & re-open
 		  // and if not a soft launch (hibernate mode)
 		  if(H2O.OPT_ARGS.soft == null)
            System.err.println("Multicast Error "+e);
           if( CLOUD_MULTICAST_SOCKET != null )
             try { CLOUD_MULTICAST_SOCKET.close(); }
             catch( Exception e2 ) { }
             finally { CLOUD_MULTICAST_SOCKET = null; }
         }
       }
 
     } else {                    // Multicast Simulation
       // The multicast simulation is little bit tricky. To achieve union of all
       // specified nodes' flatfiles (via option -flatfile), the simulated
       // multicast has to send packets not only to nodes listed in the node's
       // flatfile (H2O.STATIC_H2OS), but also to all cloud members (they do not
       // need to be specified in THIS node's flatfile but can be part of cloud
       // due to another node's flatfile).
       //
       // Furthermore, the packet have to be send also to Paxos proposed members
       // to achieve correct functionality of Paxos.  Typical situation is when
       // this node receives a Paxos heartbeat packet from a node which is not
       // listed in the node's flatfile -- it means that this node is listed in
       // another node's flatfile (and wants to create a cloud).  Hence, to
       // allow cloud creation, this node has to reply.
       //
       // Typical example is:
       //    node A: flatfile (B)
       //    node B: flatfile (C), i.e., A -> (B), B-> (C), C -> (A)
       //    node C: flatfile (A)
       //    Cloud configuration: (A, B, C)
       //
 
       // Hideous O(n) algorithm for broadcast - avoid the memory allocation in
       // this method (since it is heavily used)
       HashSet<H2ONode> nodes = (HashSet<H2ONode>)H2O.STATIC_H2OS.clone();
       nodes.addAll(H2O.CLOUD._memset);
       nodes.addAll(Paxos.PROPOSED_MEMBERS);
       bb.mark();
       for( H2ONode h2o : nodes ) {
         bb.reset();
         try {
           H2O.CLOUD_DGRAM.send(bb, h2o._key);
         } catch( IOException e ) {
           System.err.println("Multicast Error to "+h2o);
           e.printStackTrace(System.err);
         }
       }
     }
   }
 
 
   /**
    * Read a set of Nodes from a file. Format is:
    *
    * name/ip_address:port
    * - name is unused and optional
    * - port is optional
    * - leading '#' indicates a comment
    *
    * For example:
    *
    * 10.10.65.105:54322
    * # disabled for testing
    * # 10.10.65.106
    * /10.10.65.107
    * # run two nodes on 108
    * 10.10.65.108:54322
    * 10.10.65.108:54325
    */
   private static HashSet<H2ONode> parseFlatFile( String fname ) {
     if( fname == null ) return null;
     File f = new File(fname);
     if( !f.exists() ) return null; // No flat file
     HashSet<H2ONode> h2os = new HashSet<H2ONode>();
     BufferedReader br = null;
     int port = DEFAULT_PORT;
     try {
       br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
       String strLine = null;
       while( (strLine = br.readLine()) != null) {
         strLine = strLine.trim();
         // be user friendly and skip comments and empty lines
         if (strLine.startsWith("#") || strLine.isEmpty()) continue;
 
         String ip = null, portStr = null;
         int slashIdx = strLine.indexOf('/');
         int colonIdx = strLine.indexOf(':');
         if( slashIdx == -1 && colonIdx == -1 ) {
           ip = strLine;
         } else if( slashIdx == -1 ) {
           ip = strLine.substring(0, colonIdx);
           portStr = strLine.substring(colonIdx+1);
         } else if( colonIdx == -1 ) {
           ip = strLine.substring(slashIdx+1);
         } else if( slashIdx > colonIdx ) {
           Log.die("Invalid format, must be name/ip[:port], not '"+strLine+"'");
         } else {
           ip = strLine.substring(slashIdx+1, colonIdx);
           portStr = strLine.substring(colonIdx+1);
         }
 
         InetAddress inet = InetAddress.getByName(ip);
         if( !(inet instanceof Inet4Address) )
           Log.die("Only IP4 addresses allowed: given " + ip);
         if( !Strings.isNullOrEmpty(portStr) ) {
           try {
             port = Integer.decode(portStr);
           } catch( NumberFormatException nfe ) {
             Log.die("Invalid port #: "+portStr);
           }
         }
         h2os.add(H2ONode.intern(inet, port+1));// use the UDP port here
       }
     } catch( Exception e ) { Log.die(e.toString()); }
     finally { Closeables.closeQuietly(br); }
     return h2os;
   }
 
   static void initializePersistence() {
     PersistIce.initialize();
     PersistNFS.initialize();
     HdfsLoader.initialize();
     if( OPT_ARGS.aws_credentials != null ) {
       try {
         PersistS3.getClient();
       } catch( IllegalArgumentException iae ) { }
     }
   }
 
 
   // Cleaner ---------------------------------------------------------------
 
   // msec time at which the STORE was dirtied.
   // Long.MAX_VALUE if clean.
   static private volatile long _dirty; // When was store dirtied
 
   static void dirty_store() { dirty_store(System.currentTimeMillis()); }
   static void dirty_store( long x ) {
     // Keep earliest dirty time seen
     if( x < _dirty ) _dirty = x;
   }
   static void kick_store_cleaner() {
     synchronized(STORE) { STORE.notifyAll(); }
   }
 
   // Periodically write user keys to disk
   public static class Cleaner extends Thread {
     // Desired cache level. Set by the MemoryManager asynchronously.
     static public volatile long DESIRED;
     // Histogram used by the Cleaner
     private final Histo _myHisto;
     // Turn on to see copious cache-cleaning stats
     static public final boolean VERBOSE = Boolean.getBoolean("h2o.cleaner.verbose");
 
     public Cleaner() {
       super("Memory Cleaner");
       setDaemon(true);
       _dirty = Long.MAX_VALUE; // Set to clean-store
       _myHisto = new Histo(); // Build/allocate a first histogram
       _myHisto.compute(0); // Compute lousy histogram; find eldest
       Histo.H.set(_myHisto); // Force to be the most recent
       _myHisto.histo(true); // Force a recompute with a good eldest
       MemoryManager.set_goals("init",false);
     }
 
     public void run() {
       while (true) {
         // Sweep the K/V store, writing out Values (cleaning) and free'ing
         // - Clean all "old" values (lazily, optimistically)
         // - Clean and free old values if above the desired cache level
         // Do not let optimistic cleaning get in the way of emergency cleaning.
 
         // Get a recent histogram, computing one as needed
         Histo h = _myHisto.histo(false);
         long now = System.currentTimeMillis();
         long dirty = _dirty; // When things first got dirtied
 
         // Start cleaning if: "dirty" was set a "long" time ago, or we beyond
         // the desired cache levels. Inverse: go back to sleep if the cache
         // is below desired levels & nothing has been dirty awhile.
         if( h._cached < DESIRED && // Cache is low and
             (now-dirty < 5000) ) { // not dirty a long time
           // Block asleep, waking every 5 secs to check for stuff, or when poked
           synchronized( STORE ) {
             try { STORE.wait(5000); } catch (InterruptedException ie) { }
           }
           continue; // Awoke; loop back and re-check histogram.
         }
 
         now = System.currentTimeMillis();
         _dirty = Long.MAX_VALUE; // Reset, since we are going write stuff out
 
         // The age beyond which we need to toss out things to hit the desired
         // caching levels. If forced, be exact (toss out the minimal amount).
         // If lazy, store-to-disk things down to 1/2 the desired cache level
         // and anything older than 5 secs.
         boolean force = (h._cached >= DESIRED); // Forced to clean
         long clean_to_age = h.clean_to(force ? DESIRED : (DESIRED>>1));
         // If not forced cleaning, expand the cleaning age to allows Values
         // more than 5sec old
         if( !force ) clean_to_age = Math.max(clean_to_age,now-5000);
 
         if( VERBOSE )
           System.out.println("[clean >>>] "+h+" DESIRED="+(DESIRED>>20)+"M dirtysince="+(now-dirty)+" force="+force+" clean2age="+(now-clean_to_age));
         long cleaned = 0;
         long freed = 0;
 
         // For faster K/V store walking get the NBHM raw backing array,
         // and walk it directly.
         Object[] kvs = STORE.raw_array();
         // Start the walk at slot 2, because slots 0,1 hold meta-data
         for( int i=2; i<kvs.length; i += 2 ) {
           // In the raw backing array, Keys and Values alternate in slots
           Object ok = kvs[i+0], ov = kvs[i+1];
           if( !(ok instanceof Key ) ) continue; // Ignore tombstones and Primes and null's
           Key key = (Key )ok;
           if( !(ov instanceof Value) ) continue; // Ignore tombstones and Primes and null's
           Value val = (Value)ov;
           byte[] m = val.mem();
           if( m == null ) continue; // Nothing to throw out
 
           // ValueArrays covering large files in global filesystems such as NFS
           // or HDFS are only made on import (right now), and not reconstructed
           // by inspection of the Key or filesystem.... so we cannot toss them
           // out because they will not be reconstructed merely by loading the Value.
           if( val._isArray != 0 &&
               (val._persist & Value.BACKEND_MASK)!=Value.ICE )
             continue; // Cannot throw out
 
           // Ignore things younger than the required age. In particular, do
           // not spill-to-disk all dirty things we find.
           long touched = val._lastAccessedTime;
           if( touched > clean_to_age ) {
             dirty_store(touched); // But may write it out later
             continue;
           }
 
           // Should I write this value out to disk?
           // Should I further force it from memory?
           if( force || lazy_clean(key) ) {
             if( VERBOSE && !val.isPersisted() ) { System.out.print('.'); cleaned += m.length; }
             val.storePersist(); // Write to disk
             if( force ) val.freeMem(); // And, under pressure, free mem
             if( VERBOSE ) freed += m.length;
           }
         }
 
         h = _myHisto.histo(true); // Force a new histogram
         MemoryManager.set_goals("postclean",false);
         if( VERBOSE )
           System.out.println("[clean <<<] "+h+" cleaned="+(cleaned>>20)+"M, freed="+(freed>>20)+"M, DESIRED="+(DESIRED>>20)+"M");
       }
     }
 
     // Rules on when to write & free a Key, when not under memory pressure.
     boolean lazy_clean( Key key ) {
       // Only arraylets are worth tossing out even lazily.
       if( key._kb[0] != Key.ARRAYLET_CHUNK ) // Not arraylet?
         return false; // Not enough savings to write it with mem-pressure to force us
       // If this is a chunk of a system-defined array, then assume it has
       // short lifetime, and we do not want to spin the disk writing it
       // unless we're under memory pressure.
       Key arykey = ValueArray.getArrayKey(key);
       return arykey.user_allowed(); // Write user keys but not system keys
     }
 
 
     // Histogram class
     public static class Histo {
       // Current best histogram
       static public final AtomicReference<Histo> H = new AtomicReference(null);
 
       final long[] _hs = new long[128];
       long _oldest; // Time of the oldest K/V discovered this pass
       long _eldest; // Time of the eldest K/V found in some prior pass
       long _hStep; // Histogram step: (now-eldest)/histogram.length
       long _cached; // Total alive data in the histogram
       long _when; // When was this histogram computed
       Value _vold; // For assertions: record the oldest Value
       boolean _clean; // Was "clean" K/V when built?
 
       // Return the current best histogram
       static Histo best_histo() { return H.get(); }
 
       // Return the current best histogram, recomputing in-place if it is
       // getting stale. Synchronized so the same histogram can be called into
       // here and will be only computed into one-at-a-time.
       synchronized Histo histo( boolean force ) {
         Histo h = H.get(); // Grab current best histogram
         if( !force && System.currentTimeMillis() < h._when+100 )
           return h; // It is recent; use it
         if( h._clean && _dirty==Long.MAX_VALUE )
           return h; // No change to the K/V store, so no point
         compute(h._oldest); // Use last oldest value for computing the next histogram in-place
         // Atomically set a more recent histogram, racing against other threads
         // setting a newer histogram. Probably just the Cleaner thread racing
         // against F/J workers calling into the MemoryManager.
         while( h._when <= _when && !H.compareAndSet(h,this) )
           h = H.get();
         return H.get();
       }
 
       // Compute a histogram
       public void compute( long eldest ) {
         Arrays.fill(_hs, 0);
         _when = System.currentTimeMillis();
         _eldest = eldest; // Eldest seen in some prior pass
         _hStep = Math.max(1,(_when-eldest)/_hs.length);
         boolean clean = _dirty==Long.MAX_VALUE;
         // Compute the hard way
         Object[] kvs = STORE.raw_array();
         long cached = 0; // Total K/V cached in ram
         long oldest = Long.MAX_VALUE; // K/V with the longest time since being touched
         Value vold = null;
         // Start the walk at slot 2, because slots 0,1 hold meta-data
         for( int i=2; i<kvs.length; i += 2 ) {
           // In the raw backing array, Keys and Values alternate in slots
           Object ok = kvs[i+0], ov = kvs[i+1];
           if( !(ok instanceof Key ) ) continue; // Ignore tombstones and Primes and null's
           if( !(ov instanceof Value) ) continue; // Ignore tombstones and Primes and null's
           Value val = (Value)ov;
           byte[] m = val.mem();
           if( m == null ) continue;
           if( val._isArray != 0 &&
               (val._persist & Value.BACKEND_MASK)!=Value.ICE )
             continue; // Cannot throw out
 
           cached += m.length; // Accumulate total amount of cached keys
           if( val._lastAccessedTime < oldest ) { // Found an older Value?
             vold = val; // Record oldest Value seen
             oldest = val._lastAccessedTime;
           }
           // Compute histogram bucket
           int idx = (int)((val._lastAccessedTime - eldest)/_hStep);
           if( idx < 0 ) idx = 0;
           else if( idx >= _hs.length ) idx = _hs.length-1;
           _hs[idx] += m.length; // Bump histogram bucket
         }
         _cached = cached; // Total cached
         _oldest = oldest; // Oldest seen in this pass
         _vold = vold;
         _clean = clean && _dirty==Long.MAX_VALUE; // Looks like a clean K/V the whole time?
         if( VERBOSE ) System.out.println("[compute histo "+(cached>>20)+"M]");
       }
 
       // Compute the time (in msec) for which we need to throw out things
       // to throw out enough things to hit the desired cached memory level.
       long clean_to( long desired ) {
         if( _cached < desired ) return Long.MAX_VALUE; // Already there; nothing to remove
         long age = _eldest; // Age of bucket zero
         long s = 0; // Total amount toss out
         for( long t : _hs ) { // For all buckets...
           s += t; // Raise amount tossed out
           age += _hStep; // Raise age beyond which you need to go
           if( _cached - s < desired ) break;
         }
         return age;
       }
 
       // Pretty print
       public String toString() {
         long x = _eldest;
         long now = System.currentTimeMillis();
         return "H("+(_cached>>20)+"M, "+x+" < +"+(_oldest-x)+" <...{"+_hStep+"}...< +"+(_hStep*128)+" < +"+(now-x)+")";
       }
     }
   }
 }
