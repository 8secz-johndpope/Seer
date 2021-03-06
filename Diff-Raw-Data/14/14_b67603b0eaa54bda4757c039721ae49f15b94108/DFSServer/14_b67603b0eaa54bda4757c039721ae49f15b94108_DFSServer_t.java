 import java.io.File;
 import java.lang.reflect.Method;
 import java.util.Iterator;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.NavigableMap;
 import java.util.Set;
 import java.util.TreeMap;
 import edu.washington.cs.cse490h.lib.Callback;
 import edu.washington.cs.cse490h.lib.PersistentStorageReader;
 import edu.washington.cs.cse490h.lib.PersistentStorageWriter;
 
 public class DFSServer extends DFSComponent {
 
   public class Transaction {
     TransactionId id;
     //imy
     Map<DFSFilename,Checkout> checkouts;
     VersionRange committableRange;
     
     public Transaction(TransactionId id) {
       this.id = id;
       this.checkouts = new HashMap<DFSFilename,Checkout>();
      committableRange = new VersionRange();
     }
 
     public VersionRange getValidCommitTimes() {
       return committableRange;
     }
   }
 
   public class Checkout {
     Transaction transaction;
     DFSFilename file;
     FileVersion version;
     boolean isOwner;
     PersistentStorageCache.CacheEntry entry;
 
     public Checkout(Transaction t, DFSFilename file, FileVersion version, boolean isOwner) {
       this(t, file, version, isOwner, null);
     }
 
     public Checkout(Transaction t,
                     DFSFilename file,
                     FileVersion version, 
                     boolean isOwner,
                     PersistentStorageCache.CacheEntry entry) {
       this.transaction = t;
       this.file = file;
       this.version = version;
       this.isOwner = isOwner;
       this.entry = entry;
     }
 
     public DFSFilename getCachePath() {
       if (entry != null)
         return entry.getKey();
       return new DFSFilename(DFSFilename.kPrefixString + 
                              parent.addr +
                              "/tx" + transaction.id.toString() +
                              file.getPath());
     }
 
     public DFSFilename getCheckInPath() {
       DFSFilename key = entry.getKey();
       int pathIndex = key.getPath().indexOf('/', 2);
       String newPath = key.getPath().substring(pathIndex);
       return new DFSFilename(DFSFilename.kPrefixString + 
                              key.getOwningServer() + 
                              newPath);
     }
 
     public Checkout getParent() {
       return files.get(file).checkouts.get(new FileVersion(version.getTransactionId(),
                                                            version.getVersion(),
                                                            0));
     }
 
 
     public String getData() {
       if (entry != null)
         return entry.getData();
 
       if (getParent() == this)
         return exists() ? "" : null;
       return getParent().getData();
     }
 
 
     public boolean exists() {
       if (entry != null)
         return entry.exists();
 
       if (getParent() == this) // Special case for newly-created file!
         return true;
 
       return getParent().exists();
     }
 
 
     public Errors update(FileVersion v, String data, boolean exists) {
       if (!v.getTransactionId().equals(transaction.id))
         return Errors.NoTransaction;
 
       if (v.getVersion() == version.getVersion())
         return Errors.VersionNotFound;
 
       cache.set(getCachePath(), v, data, transaction.id.getClientId(), exists);
       entry = cache.get(getCachePath());
 
       return Errors.Success;
     }
 
     public void commit(int commitTime) {
       cache.rename(getCachePath(), getCheckInPath());
       version.commit(commitTime);
     }
     
     public void delete() {
       if (version.isCommitted())
         throw new IllegalStateException("Cannot delete a committed version");
       
       if (entry == null)
         return;
 
       cache.forget(entry.getKey());
     }
   }
 
   /**
    * Tracker class to track state related to transactions.
    */
   public class FileState { 
     public VersionedString liveVersions;
     public TreeMap<FileVersion,Checkout> checkouts;
     public PersistentStorageCache.CacheEntry entry;
     
     public FileState(DFSFilename file, PersistentStorageCache cache) {
       entry = cache.get(file);
       if (entry == null || entry.isInvalid())
         throw new IllegalArgumentException("No such cache entry!");
 
       liveVersions = new VersionedString(entry.getVersion(), entry.getData());
 
       checkouts = new TreeMap<FileVersion,Checkout>();
       checkouts.put(entry.getVersion(), 
                     new Checkout(null, file, entry.getVersion(), false, entry));
     }
 
     /**
      * Get latest committed version, or null if no version has been 
      * committed.
      *
      * @return latest committed version.
      */
     public FileVersion getLatestCommittedVersion() {
       FileVersion latest = checkouts.lastKey();
       
       while (latest != null) {
         if (latest.isCommitted())
           return latest;
         latest = checkouts.lowerKey(latest);
       }
 
       return null;
     }
 
     public boolean isLatestCommittedVersion(FileVersion ver) {
       FileVersion next = checkouts.higherKey(ver);
       
       while (next != null) {
         if (next.isCommitted())
           return false;
         next = checkouts.higherKey(next);
       }
       
       return ver.isCommitted();
     }  
 
     /**
      * Reduce the given commit window to bound the times when the associated
      * commit may proceed.
      *
      * @param checkoutVersion Checkout to ensure is in the version bound
      * @param range Version time bound.
      */
     public void reduceCommittableVersionRange(FileVersion checkoutVersion,
                                               VersionRange range) {
       VersionRange.Bound upperBound = null;
       VersionRange.Bound lowerBound = null;
       Map.Entry<FileVersion,Checkout> entry = 
         checkouts.floorEntry(checkoutVersion);
       if (entry == null)
         throw new IllegalArgumentException("No such checkout!");
 
       NavigableMap.Entry<FileVersion,Checkout> nextEntry = checkouts.higherEntry(entry.getKey());
 
       while (nextEntry != null) {
         if (nextEntry.getKey().getRevision() == 0) {
           if (entry.getKey().getOwner() != PersistentStorageCache.kNotOwned) {
             // TODO(andrew) this method needs to be examined. range.collapse();
             return;
           }
 
           upperBound = new VersionRange.Bound(nextEntry.getKey().getCommitTime());
           break;
         }
       }
 
       nextEntry = checkouts.lowerEntry(entry.getKey());
       while (nextEntry != null) {
         if (nextEntry.getKey().getRevision() == 0) {
           lowerBound = new VersionRange.Bound(nextEntry.getKey().getCommitTime());
           break;
         }
       }
       
       range.reduce(lowerBound, upperBound);
     }
 
     /**
      * Handles a commit on the file. 
      */
       //go through list of syncdatamessages (from syncDataMessagesByTxID to deduce the state of files.
       //use a hashmap, dude, put(DFSFilename, int version (or FileVersion))
       //this determines where to record a commit relative to other commits
       //ex: for the given transaction, file A is at version 3
       //B is at 1
       //C is at 0
       //but someone else wrote A and committed before this commit. (Check the cache) A is now 4.
       //Commit of B must be placed in between 3, 4, according to Android's drawings
     public Errors commit(FileVersion ver, int commitTime) {
       Checkout commit = checkouts.remove(ver);
       if (commit == null)
         return Errors.VersionNotFound;
 
       if (checkouts.containsKey(commit.version.nextVersion())) {
         checkouts.put(commit.version, commit);
         throw new IllegalStateException("Can't commit; next version already committed!");
       }
 
       ver = ver.nextVersion();
       checkouts.put(ver, commit);
       
       for (FileVersion v : checkouts.keySet()) {
         if (v.equals(ver))
           continue;
         if (!v.isCommitted()) {
           VersionRange conflictingRange = checkouts.get(v).transaction.committableRange;
           if (v.compareTo(ver) < 0 && (commit.isOwner ^ checkouts.get(v).isOwner)) {
             conflictingRange.reduce(null, new VersionRange.Bound(commitTime));
           } else if (v.compareTo(ver) > 0 && 
                      (commit.isOwner ^ checkouts.get(v).isOwner)) {
             conflictingRange.reduce(new VersionRange.Bound(commitTime), null);
           }
 
           if (!conflictingRange.isValid())
             abortTransaction(checkouts.get(v).transaction.id);
         }
       }
 
       return Errors.Success;
     }
 
     public Errors cancel(FileVersion ver) {
       Checkout c = checkouts.remove(ver);
       if (c == null) 
         return Errors.NoTransaction;
       return Errors.Success;
     }
   }
 
 
   // Temp filename used by the server to preserve atomic writes.
   public static final String tempFileName = ".temp";
 
   // Map of all files that have ever had a live transaction since server start.
   private HashMap<DFSFilename, FileState> files;
 
   // Map of all the live transactions.
   private HashMap<TransactionId, Transaction> transactions;
 
   // Server's master cache. The backing store for all data.
   private PersistentStorageCache cache;
 
   // Log of currently-being-committed transaction.
   private TransactionLog log;
 
   // Next free slot in the commit time continuum.
   private int nextValidCommitTime;
 
   private void createFileState(DFSFilename file) {
     if (files.containsKey(file))
       return;
 
     files.put(file, new FileState(file, cache));
   }
 
   /**Note: Given this method signature, it is the user's perogative to give me the correct txId
   *  It should be the txId of the received request.
   */
   private void updateCheckout(DFSFilename file, TransactionId txId, boolean writePermit) {
     PersistentStorageCache.CacheEntry e = cache.get(file);
     FileVersion cachedVersion = e.getVersion();
     
     FileVersion sendVersion = new FileVersion(txId, e.getVersion().getVersion(), 0);
     
     Transaction t = transactions.get(txId);   
     Checkout c = new Checkout(t, file, sendVersion, writePermit); 
     t.checkouts.put(file, c);
     files.get(file).checkouts.put(sendVersion,c);   
   }
 
   /**
    * Starts the DFSServer. This method will trigger a cleanup of the filesystem
    * layout by instantiating a PersistentStorageCache.
    *
    * TODO(andrew): Handle any dirty files.
    */
   public DFSServer(DFSNode parent) {
     super(parent);
 
     System.err.println("Starting the DFSServer...");
     
     files = new HashMap<DFSFilename,FileState>();
     transactions = new HashMap<TransactionId,Transaction>();
 
     cache = new PersistentStorageCache(parent, this, true);
     log = new TransactionLog(parent);
     log.restore();
   }
 
 
   /**
    * General message handler.
    *
    * @param from Sender of the message
    * @param msg The message
    */
   @Override
   public void onReceive(Integer from, DFSMessage msg) {
     System.err.println("Server (addr " + this.addr + ") received: " +
                        msg.toString());
 
     // Demux on the message type and dispatch accordingly.
     switch (msg.getMessageType()) {
     case Transaction:
       handleTransaction(from, (TransactionMessage) msg);
       break;
     case SyncRequest:
       handleSyncRequest(from, (SyncRequestMessage) msg);
       break;
     case SyncData:
       handleSyncData(from, (SyncDataMessage) msg);
       break;
     case AckData:
       handleAckData(from, (AckDataMessage) msg);
       break;
     case Response:
       handleResponse(from, (ResponseMessage) msg);
       break;
     default:
     }
   }
 
   /**
    * Handles Transaction messages. These deal with starting, aborting, and committing
    * transactions.
    *
    *
    * @param from Sender of the message
    * @param msg The message
    */
   private void handleTransaction(int from, TransactionMessage msg) {
     // Clients don't ever confirm anything. We have the authority around here...
     Flags flags = msg.getFlags();
     
     System.err.println("Got txn msg with flags: " + flags.prettyPrint(TransactionFlags.byWireId));
     if (flags.isSet(TransactionFlags.Confirm))
       return;
 
     if (flags.isSet(TransactionFlags.Start)) {
       startTransaction(msg.getTxId());
     } else if (flags.isSet(TransactionFlags.Abort)) {
       abortTransaction(msg.getTxId());
     } else if (flags.isSet(TransactionFlags.Commit)) {
       commitTransaction(msg.getTxId());
     }
 
     // Nothing to do, drop packet on floor.
   }
 
   /**
    * Handles AckData messages. An AckData can trigger the following response:
    *  - An attempt to continue a transfer-of-ownership. The client may have the file
    *    checked out for read, in which case that client is removed from the list of
    *    outstanding clients on the request. The request is completed if no more
    *    clients have yet to respond.
    *
    * @param from Sender of the message
    * @param msg The message
    */
   private void handleAckData(Integer from, AckDataMessage msg) {
     DFSFilename filename;
     try {
       filename = new DFSFilename(msg.getFileName());
     } catch (IllegalArgumentException e) {
       return;
     }
 
     PersistentStorageCache.CacheEntry entry = cache.get(filename);
     if (entry.getState() == PersistentStorageCache.CacheState.INVALID) {
       System.err.println("WARN: Server got AckData for an invalid cache entry...");
       return;
     }
 //    updateCheckout(filename, msg.getTxId(), true);
     // TODO(jimmy) replace. Possibly this method really doesn't have to do much.
 /*    OwnershipRequestWrapper req = transferOwnerRequests.get(filename);
     if (req == null) {
       System.err.println("WARN: Server got AckData for an unknown ownership transfer...");
       return;
     }
 
     if (req != null &&
         req.outstandingReaders != null &&
         req.outstandingReaders.contains(from) &&
         req.desiredOwner != from) {
       req.outstandingReaders.remove(from);
       if (req.outstandingReaders.size() == 0)
         sendOwnershipToClient(req);
     }
 
     if (req.desiredOwner == from) {
       cache.set(filename,
                 entry.getVersion(),
                 entry.getData(),
                 from,
                 entry.exists());
       transferOwnerRequests.remove(filename);
       } */
   }
 
 
   /**
    * Handles SyncData messages. 
    *
    * If the file is owned by someone else, the SyncRequest is effectively
    * forwarded to the owner. Any read occurs at the time the request
    * is processed by the owner. If the request is a transfer-of-ownership
    * request, all clients that have a copy in their cache will be
    * notified before the request is satisfied.
    *
    * @param from Sender of the message
    * @param msg The message
    */
   private void handleSyncData(Integer from, SyncDataMessage msg) {
     DFSFilename filename;
     try {
       filename = new DFSFilename(msg.getFileName());
     } catch (IllegalArgumentException e) {
       return;
     }
 
     TransactionId id = msg.getTxId();
     Transaction t = transactions.get(id);
     if (t == null) {
       RIOSend(from, 
               Protocol.DATA, 
               new ResponseMessage(id, 
                                   msg.getFileName(),
                                   msg.getVersion(),
                                   Errors.NoTransaction.getId()).pack());
       return;
     }
 
     Checkout c = t.checkouts.get(msg.getDFSFileName());
     Errors e = c.update(msg.getVersion(), msg.getData(), msg.exists());
     if (!e.equals(Errors.Success)) {
       RIOSend(from, 
               Protocol.DATA, 
               new ResponseMessage(msg.getTxId(),
                                   msg.getFileName(),
                                   msg.getVersion(),
                                   e.getId()).pack());
       return;
     }
       
 
     if (msg.getFlags().isSet(SyncFlags.TransferOwnership)) {
       // TODO(jimmy): Decide appropriate logic here.
     }
 
     AckDataMessage response = new AckDataMessage(filename.toString(),
                                                  msg.getTxId(),
                                                  c.version);
     System.err.println("Server (addr " + this.addr + ") sending to " +
                        from + ": " + response.toString());
     RIOSend(from, Protocol.DATA, response.pack());
   }
 
 
   /**
    * Handle a ResponseMessage. The server receives this message in the
    * following cases:
    *  - The server sent a SyncData message to inform the client of an
    *    ownership transfer. The client encountered an error while
    *    processing the message.
    *  - Deprecated: The server sent a SyncRequest message to a client to request an
    *    ownership transfer. The client refused the message or encountered
    *    an error.
    *
    * In all cases, the client is considered at fault and loses any data
    * or ongoing request.
    *
    * @param from Sender of the message
    * @param msg The message
    */
   private void handleResponse(Integer from, ResponseMessage msg) {
     // TODO(jimmy): Do we even need this?
     // Heck no, not now
   }
 
   /** Handles a SyncRequestMessage. The following actions can be taken:
    *   - If the message has Create set, responds immediately as appropriate.
    *   - If the message is a transfer-ownership request, responds immediately
    *     if possible, else sends an appropriate transfer-ownership request to
    *     the current owner.
    *   - Else the message is a standard sync request. Adds the sender to the
    *     set of readers on the requested file. Responds with a SyncData message
    *     if the file is not currently owned, else forwards the request to the
    *     current owner to request the latest copy.
    *
    * @param from address of requester
    * @param msg the message itself
    */
   private void handleSyncRequest(Integer from, SyncRequestMessage msg) {
     DFSFilename filename;
     try {
       filename = new DFSFilename(msg.getFileName());
     } catch (IllegalArgumentException e) {
       return;
     }
 
     Flags f = msg.getFlags();
     // Create is a special case; handle separately.
     if (f.isSet(SyncFlags.Create)) {
       Errors result = createFile(msg.getTxId(), filename, msg);
       DFSMessage reply = null;
 
      switch (result) {
       case Success:
       case Delayed:
         break;
       default:
         reply = new ResponseMessage(msg.getTxId(),
                                     filename.toString(),
                                     msg.getVersion(),
                                     result.getId());
         System.err.println("Server (addr " + this.addr + ") sending to " +
                            from + ": " + reply.toString());
         RIOSend(from, Protocol.DATA, reply.pack());
         break;
       }
 
       return;
     }
 
     PersistentStorageCache.CacheEntry entry = cache.get(filename);
     if (entry.getState() == PersistentStorageCache.CacheState.INVALID) {
       RIOSend(from, Protocol.DATA, new ResponseMessage(msg.getTxId(),
                                                        filename.toString(),
                                                        entry.getVersion(),
                                                        Errors.FileDoesNotExist.getId()).pack());
       return;
     }
 
     if (f.isSet(SyncFlags.TransferOwnership)) {
       // Probably nothing here...
     }
 
     SyncDataMessage sdMsg = buildSyncDataMessage(msg.getTxId(), filename);
     copyAppropriateSyncRequestFlags(sdMsg, msg);    
 
     System.err.println("Server (addr " + this.addr + ") sending to " +
                          from + ": " + sdMsg.toString());
     RIOSend(from, Protocol.DATA, sdMsg.pack());
   }
 
 
   /**
    * Build a SyncData message from the current contents in the cache.
    *
    * @param f File to build the SyncDataMessage for.
    */
   private SyncDataMessage buildSyncDataMessage(TransactionId id,
                                                DFSFilename f) {
     Transaction t = transactions.get(id);
     Checkout checkout = t.checkouts.get(f);
     
     return new SyncDataMessage(f.toString(),
                                id,
                                checkout.version,
                                new Flags(),
                                checkout.isOwner ? id.getClientId() : PersistentStorageCache.kNotOwned,
                                checkout.exists(),
                                checkout.getData());
   }
 
 
   /**
    * Set any flags on msg as appropriate for the given request. Does not
    * currently clear any flags.
    *
    * Currenty this really only sets ReadOnly if it is present in req.
    *
    * @param msg Response to set flags on
    * @param req Request.
    */
   private void copyAppropriateSyncRequestFlags(SyncDataMessage msg,
                                                SyncRequestMessage req) {
     Flags f = req.getFlags();
 
     if (f.isSet(SyncFlags.ReadOnly))
       msg.getFlags().set(SyncFlags.ReadOnly);
     if (f.isSet(SyncFlags.TransferOwnership))
       msg.getFlags().set(SyncFlags.TransferOwnership);
   }
 
   /**
    * Starts a transaction with the given transaction ID, or returns
    * an error code.
    */
   private void startTransaction(TransactionId id) {
     if (transactions.containsKey(id)) {
       RIOSend(id.getClientId(), 
               Protocol.DATA, 
               new ResponseMessage(id,
                                   "", 
                                   new FileVersion(id, 0, 0),
                                   Errors.DuplicateTransaction.getId()).pack());
       
       return;
     }
 
     System.err.println("Adding transaction: " + id.toString());
     transactions.put(id, new Transaction(id));
     
     Flags f = new Flags();
     f.set(TransactionFlags.Start);
     f.set(TransactionFlags.Confirm);
 
     RIOSend(id.getClientId(),
             Protocol.DATA,
             new TransactionMessage(id, f, null).pack());
   }
 
   private void abortTransaction(TransactionId id) {
     Errors error = doAbortTransaction(id);
     
     if (error != null && !error.equals(Errors.Success)) {
       RIOSend(id.getClientId(),
               Protocol.DATA,
               new ResponseMessage(id,
                                   "", 
                                   new FileVersion(id, 0, 0),
                                   error.getId()).pack());
     } else {
       Flags f = new Flags();
       f.set(TransactionFlags.Abort);
       f.set(TransactionFlags.Confirm);
       RIOSend(id.getClientId(),
               Protocol.DATA,
               new TransactionMessage(id, f, null).pack());
     }
   }
 
   private Errors doAbortTransaction(TransactionId id) {
     System.err.println("Aborting transaction: " + id);
     if (!transactions.containsKey(id)) {
       return Errors.NoTransaction;
     }
 
     Transaction t = transactions.remove(id);
     if (t == null)
       return Errors.NoTransaction;
     System.err.println("Found txn: " + t);
 
     // Step 1. Delete all checked-out versions in the transaction.
     for (DFSFilename f : t.checkouts.keySet()) {
       System.err.println("Deleting " + f);
       FileState state = files.get(f);
       if (state == null)
         return Errors.Unknown;
 
       state.cancel(t.checkouts.get(f).version);
     }
 
     // Step 2. Adjust any commit windows as needed.
 
     return Errors.Success;
   }
 
   private void commitTransaction(TransactionId id) {
     Errors error = Errors.Unknown;
     
     try {
       System.err.println("Searching for txn with id " + id);
       if (!transactions.containsKey(id)) {
         error = Errors.NoTransaction;
         return;
       }
 
       Transaction t = transactions.get(id);
       System.err.println("Found txn: " + t);
 
       // Step 1. Ensure txn can be committed.
       VersionRange validCommitTimes = t.committableRange;
       if (validCommitTimes == null) {
         System.err.println("No valid commit times!");
         abortTransaction(id);
         error = null;
         return;
       }
 
       // Step 2. Pick commit time
       // TODO(andrew): If we were really being picky, we'd do some type
       // of in-use histogram and pick the commit time least-likely to cause
       // a conflict. For now, we pick the latest finite time and hope for
       // the best!
       int commitTime;
       if (validCommitTimes.getEnd().time == VersionRange.kInfinite) {
         if (validCommitTimes.getStart().time == VersionRange.kInfinite)
           commitTime = nextValidCommitTime;
         else
           commitTime = validCommitTimes.getStart().time;
       } else {
         commitTime = validCommitTimes.getEnd().time - 1;
       }
 
       System.err.println("Committing transaction " + id + " @ time " + commitTime);
 
       // Step 3. Log intent to commit.
       log.startCommit(id, t);
 
       // Step 4. Commit all files, send invalidation requests
       for (DFSFilename f : t.checkouts.keySet()) {
         System.err.println("Committing file " + f);
         Checkout c = t.checkouts.get(f);
         FileState state = files.get(f);
         if (state == null) {
           doAbortTransaction(id);
           return;
         }
 
         state.commit(c.version, commitTime);
         if (!state.isLatestCommittedVersion(c.version))
           // TODO Fix once we refactor invalidateOwnership!
           ;
       }
 
       // Step 5. Log commit completion
       log.finishCommit();
       
       // Step 6. Remove transaction
       transactions.remove(id);
      
      error = null;
      Flags f = new Flags();
      f.set(TransactionFlags.Commit);
      f.set(TransactionFlags.Confirm);
      RIOSend(id.getClientId(),
              Protocol.DATA,
              new TransactionMessage(id, f, null).pack());
     } finally {
       if (error != null)
         RIOSend(id.getClientId(),
                 Protocol.DATA,
                 new ResponseMessage(id,
                                    "/dfs/" + parent.addr + "/",
                                     new FileVersion(id, 0, 0),
                                     error.getId()).pack());
     }
   }
 
 
   /**
    * Creates the given file in the cache, if it is currently unowned and
    * legal to do so. If not, it returns an appropriate error code from the
    * Errors enum, which should be transmitted to the requesting client.
    * If it is currently owned, initiates a transfer-of-ownership to the
    * server.
    */
   private Errors createFile(TransactionId id,
                             DFSFilename file,
                             SyncRequestMessage msg) {
     System.err.println("createFile");
 
     createFileState(file);
     FileState fs = files.get(file);
     FileVersion latestCommit = fs.getLatestCommittedVersion();
     FileVersion newVersion;
     Checkout newCheckout;
     if (latestCommit == null) {
       // file has never existed; create
       newVersion = new FileVersion(id, 0, 0);
     } else {
       if (fs.checkouts.get(latestCommit).entry.exists)
         return Errors.FileAlreadyExists;
 
       newVersion = new FileVersion(id, latestCommit.getVersion(), 0);
     }
     
     int newOwner = PersistentStorageCache.kNotOwned;
     if (msg.getFlags().isSet(SyncFlags.TransferOwnership))
       newOwner = id.getClientId();
 
     newCheckout = new Checkout(null, file, newVersion, newOwner != PersistentStorageCache.kNotOwned);
     fs.checkouts.put(newVersion, newCheckout);
     transactions.get(id).checkouts.put(file, newCheckout);
 
     SyncDataMessage sdMsg = buildSyncDataMessage(id, file);
     copyAppropriateSyncRequestFlags(sdMsg, msg);
 
     if (msg.getFlags().isSet(SyncFlags.TransferOwnership)) {
       sdMsg.getFlags().set(SyncFlags.TransferOwnership);
       
     }
 
     System.err.println("Sendign response; " + sdMsg);
 
     RIOSend(id.getClientId(), Protocol.DATA, sdMsg.pack());
 
     return Errors.Success;
   }
 
   /**
    * Transfer ownership to the server. Does not send any network messages;
    * simply marks the transfer in the cache and returns.
    *
    * @param filename File to transfer.
    */
   private void transferOwnerToServer(DFSFilename filename) {
     PersistentStorageCache.CacheEntry entry = cache.get(filename);
     if (entry.getState().equals(PersistentStorageCache.CacheState.INVALID))
       throw new IllegalStateException("Should not get here; server hasn't heard of " + filename);
   }
 
   /**
    * TODO
    */
   private Callback newDeliveryCBInstance() {
     try {
       String[] args = new String[]{ "java.lang.Integer", "java.lang.Exception" };
       Method m = Callback.getMethod("networkDeliveryCallback", this, args);
       return new Callback(m, this, new Object[]{ null, null });
     } catch (NoSuchMethodException nsme) {
       assert(false) : "Should never get here.";
       nsme.printStackTrace();
       return null;
     } catch (ClassNotFoundException cnfe) {
       assert(false) : "Should never get here.";
       cnfe.printStackTrace();
       return null;
     }
   }
 
 
   /**
    * TODO
    */
   public void networkDeliveryCallback(Integer seqNum, Exception e) {
     // TODO bitches
   }
 
 }
