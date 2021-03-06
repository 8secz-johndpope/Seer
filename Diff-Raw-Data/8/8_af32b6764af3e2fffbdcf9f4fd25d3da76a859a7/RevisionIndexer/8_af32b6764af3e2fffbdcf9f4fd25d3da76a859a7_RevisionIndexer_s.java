 package de.berlios.sventon.index;
 
 import de.berlios.sventon.ctrl.RepositoryConfiguration;
 import de.berlios.sventon.ctrl.RepositoryEntry;
 import de.berlios.sventon.svnsupport.RepositoryFactory;
 import de.berlios.sventon.svnsupport.LogEntryActionType;
 import de.berlios.sventon.util.PathUtil;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.tmatesoft.svn.core.*;
 import org.tmatesoft.svn.core.io.SVNRepository;
 
 import java.io.*;
 import java.util.*;
 
 /**
  * RevisionIndexer.
  *
  * @author jesper@users.berlios.de
  */
 public class RevisionIndexer {
 
   /**
    * The revision index.
    */
   private RevisionIndex index;
 
   /**
    * The repository instance to index.
    */
   private SVNRepository repository;
 
   /**
    * The repository configuration.
    */
   private RepositoryConfiguration configuration;
 
   /**
    * The index url.
    */
   private String indexedUrl;
 
   /**
    * The indexed repository's mount point.
    */
   private String mountPoint = "";
 
   /**
    * The logging instance.
    */
   private final Log logger = LogFactory.getLog(getClass());
 
   /**
    * The index file name, <tt>sventon.idx</tt>.
    */
   public static final String INDEX_FILENAME = "sventon.idx";
 
   /**
    * Constructs the index instance using a given repository.
    * The repository must be correctly initialized and credentials
    * must have been applied (if required by server).
    *
    * @param repository The repository instance
    */
   public RevisionIndexer(final SVNRepository repository) {
     logger.debug("Creating index instance using given repository");
     this.repository = repository;
     indexedUrl = repository.getLocation().toString();
     index = new RevisionIndex(indexedUrl);
   }
 
   /**
    * Constructs the index instance using a given repository configuration.
    *
    * @param configuration The repository configuration
    */
   public RevisionIndexer(final RepositoryConfiguration configuration) throws SVNException {
     logger.debug("Creating index instance using given configuration");
     this.configuration = configuration;
     indexedUrl = configuration.getUrl();
     mountPoint = configuration.getRepositoryMountPoint();
     logger.debug("Creating the repository instance");
     repository = RepositoryFactory.INSTANCE.getRepository(configuration);
     if (repository == null) {
       logger.warn("Repository not configured - unable to create index");
       return;
     }
     initIndex();
   }
 
   /**
    * @return Returns the number of indexed repository items.
    */
   public long getIndexCount() {
     return index.getEntries().size();
   }
 
   /**
    * Gets the interator for index access.
    *
    * @return The iterator.
    */
   public Iterator<RepositoryEntry> getEntriesIterator() {
     return index.getEntries().iterator();
   }
 
   /**
    * Initializes the index.
    * If a serialized index is stored on disk it will be read into memory.
    * Otherwise a complete repository indexing will be executed.
    */
   private void initIndex() {
     logger.debug("Initializing index");
 
     // TODO: Update the index according to what's new.
 
     logger.info("Reading serialized index from disk, "
         + configuration.getSVNConfigurationPath()
         + INDEX_FILENAME);
     ObjectInputStream in = null;
     try {
       in = new ObjectInputStream(new FileInputStream(configuration.getSVNConfigurationPath() + INDEX_FILENAME));
       index = (RevisionIndex) in.readObject();
     } catch (ClassNotFoundException e) {
       logger.warn(e);
     } catch (IOException e) {
       logger.warn(e);
     }
 
     // No serialized index exsisted - initialize an empty one.
     if (index == null) {
       index = new RevisionIndex(configuration.getUrl());
     }
 
   }
 
   /**
    * Indexes the files and directories, starting at the path
    * specified by calling <code>setStartPath()</code>.
    *
    * @throws SVNException if a Subversion error occurs.
    */
   protected synchronized void populateIndex() throws SVNException {
     logger.info("Populating index");
     index = new RevisionIndex(indexedUrl);
     logger.debug("Index url: " + index.getUrl());
     index.setIndexRevision(repository.getLatestRevision());
     logger.debug("Revision: " + index.getIndexRevision());
     populateIndex("".equals(mountPoint) ? "/" : mountPoint);
     logger.info("Number of indexed entries: " + getIndexCount());
   }
 
   /**
    * Updates the index to HEAD revision.
    * A Subversion <i>log</i> command will be performed and
    * the index will be updated accordingly.
    * <table>
    * <tr><th>Type</th><th>Description</th><th>Affects index</th></tr>
    * <tr><td>'A'</td><td>Added</td><td>Entry is added</td></tr>
    * <tr><td>'D'</td><td>Deleted</td><td>Entry is removed</td></tr>
    * <tr><td>'M'</td><td>Modified</td><td>Entry info is updated</td></tr>
    * <tr><td>'R'</td><td>Replaced (what means that the
    * object is first deleted, then another object of the same name is
    * added, all within a single revision)</td><td>Entry info is updated</td></tr>
    * </table>
    *
    * @throws SVNException if Subversion error occurs.
    */
   @SuppressWarnings("unchecked")
   protected synchronized void updateIndex() throws SVNException {
     String[] targetPaths =
         new String[]{"".equals(mountPoint) ? "/" : mountPoint}; // the path to log
     long latestRevision = repository.getLatestRevision();
 
     logger.info("Updating index from revision " + index.getIndexRevision() + " to "
         + latestRevision);
 
     List<SVNLogEntry> logEntries = (List<SVNLogEntry>) repository.log(targetPaths,
         null, latestRevision, index.getIndexRevision() + 1, true, false);
 
     // One logEntry is one commit (or revision)
     for (SVNLogEntry logEntry : logEntries) {
       Map<String, SVNLogEntryPath> map = logEntry.getChangedPaths();
       for (String entryPath : map.keySet()) {
         SVNLogEntryPath logEntryPath = map.get(entryPath);
         switch (LogEntryActionType.valueOf(String.valueOf(logEntryPath.getType()))) {
           case A :
             logger.debug("Adding entry to index: " + logEntryPath.getPath());
             index.add(new RepositoryEntry(
                 repository.info(logEntryPath.getPath(), latestRevision),
                 PathUtil.getPathPart(logEntryPath.getPath()),
                 mountPoint));
             break;
 
           case D :
             logger.debug("Removing entry from index: " + logEntryPath.getPath());
             index.remove(logEntryPath.getPath());
             break;
 
           case R :
             logger.debug("Updating entry in index: " + logEntryPath.getPath());
             index.remove(logEntryPath.getPath());
             index.add(new RepositoryEntry(
                 repository.info(logEntryPath.getPath(), latestRevision),
                 PathUtil.getPathPart(logEntryPath.getPath()),
                 mountPoint));
             break;
 
           case M :
             logger.debug("Updating entry in index: " + logEntryPath.getPath());
             index.remove(logEntryPath.getPath());
             index.add(new RepositoryEntry(
                 repository.info(logEntryPath.getPath(), latestRevision),
                 PathUtil.getPathPart(logEntryPath.getPath()),
                 mountPoint));
             break;
 
           default :
             throw new SVNException("Unknown log entry type: " + logEntryPath.getType() + " in rev " + latestRevision);
         }
       }
     }
     index.setIndexRevision(latestRevision);
   }
 
   /**
    * Updated the index.
    * <p/>
    * If the repository URL has changed in the configuration properties
    * or the index does not contain any entries or if the repository revision
    * is <i>lower</i> than the indexed revision, a complete indexing
    * will be performed.
    * <p/>
    * If the repository revision is greater than the revision of the
    * index, the index will be updated to reflect HEAD revision.
    *
    * @throws SVNException if Subversion error occurs.
    */
   public synchronized void update() throws SVNException {
     if (getIndexCount() == 0 || !index.getUrl().equals(indexedUrl) ||
         index.getIndexRevision() > repository.getLatestRevision()) {
       // index is just created and does not contain any entries
       // or the repository URL has changed in the config properties
       // or the repository revision is LOWER than the index revision
       // do a full repository indexing
       populateIndex();
     } else if (index.getIndexRevision() < repository.getLatestRevision()) {
       // index is out-of-date
       // update it to reflect HEAD revision
       updateIndex();
     }
   }
 
   /**
    * Populates the index by getting all entries in given path
    * and adding them to the index. This method will be recursively
    * called by <code>index()</code>.
    *
    * @param path The path to add to index.
    * @throws SVNException if a Subversion error occurs.
    */
   @SuppressWarnings("unchecked")
   private void populateIndex(final String path) throws SVNException {
     List<SVNDirEntry> entriesList = Collections.checkedList(new ArrayList<SVNDirEntry>(), SVNDirEntry.class);
 
     entriesList.addAll(repository.getDir(path, index.getIndexRevision(), null, (Collection) null));
     for (SVNDirEntry entry : entriesList) {
       if (!index.add(new RepositoryEntry(entry, path, mountPoint))) {
         throw new RuntimeException("Unable to add entry to index: " + path + entry + " (" + mountPoint + ")");
       }
       if (entry.getKind() == SVNNodeKind.DIR) {
         populateIndex(path + entry.getName() + "/");
       }
     }
   }
 
   /**
    * Finds index entries by a search string.
    *
    * @param searchString The string to search for.
    * @return The <code>List</code> of <code>IndexEntry</code> instances found.
    * @throws SVNException if a Subverions error occurs.
    * @see de.berlios.sventon.ctrl.RepositoryEntry
    */
   public List<RepositoryEntry> find(final String searchString) throws SVNException {
     if (searchString == null || searchString.equals("")) {
       throw new IllegalArgumentException("Search string was null or empty");
     }
 
     update();
     List<RepositoryEntry> result = Collections.checkedList(new ArrayList<RepositoryEntry>(), RepositoryEntry.class);
     for (RepositoryEntry entry : index.getEntries()) {
       if (entry.getFullEntryName().toLowerCase().indexOf(searchString.toLowerCase()) > -1) {
         result.add(entry);
       }
     }
     logger.debug("Found " + result.size() + " entries matching search: " + searchString);
     return result;
   }
 
   /**
    * Finds index entries by a search string.
    *
    * @param searchPattern The regex pattern to search for.
    * @return The <code>List</code> of entries found.
    * @throws SVNException if a Subverions error occurs.
    * @see java.util.regex.Pattern
    */
   public List findPattern(final String searchPattern) throws SVNException {
     if (searchPattern == null || searchPattern.equals("")) {
       throw new IllegalArgumentException("Search string was null or empty");
     }
 
     update();
     List<RepositoryEntry> result = Collections.checkedList(new ArrayList<RepositoryEntry>(), RepositoryEntry.class);
     for (RepositoryEntry entry : index.getEntries()) {
       if (entry.getFullEntryName().matches(searchPattern)) {
         result.add(entry);
       }
     }
     logger.debug("Found " + result.size() + " entries matching search: " + searchPattern);
     return result;
   }
 
   /**
    * Gets all subdirectory entries below given <code>fromPath</code>.
    *
    * @param fromPath The base path to start from.
    * @return A list containing all subdirectory entries below <code>fromPath</code>.
    * @throws SVNException if a Subverions error occurs.
    */
   public List<RepositoryEntry> getDirectories(final String fromPath) throws SVNException {
     if (fromPath == null || fromPath.equals("")) {
       throw new IllegalArgumentException("Path was null or empty");
     }
 
     update();
     List<RepositoryEntry> result = Collections.checkedList(new ArrayList<RepositoryEntry>(), RepositoryEntry.class);
     for (RepositoryEntry entry : index.getEntries()) {
       if ("dir".equals(entry.getKind()) && entry.getFullEntryName().startsWith(fromPath)) {
         result.add(entry);
       }
     }
     logger.debug("Found " + result.size() + " directories below: " + fromPath);
     return result;
   }
 
   /**
    * Dumps the entire index to STDOUT.
    */
   public void dumpIndex() {
     logger.info("Dumping index to STDOUT...");
     for (RepositoryEntry repositoryEntry : index.getEntries()) {
       System.out.println(repositoryEntry);
     }
   }
 
   /**
    * This method serializes the index to disk.
    */
   public void destroy() {
     logger.info("Saving index to disk, " + configuration.getSVNConfigurationPath() + INDEX_FILENAME);
     ObjectOutputStream out = null;
     try {
       out = new ObjectOutputStream(new FileOutputStream(configuration.getSVNConfigurationPath() + INDEX_FILENAME));
       out.writeObject(index);
     } catch (IOException e) {
       logger.warn(e);
     }
   }
 
 }
