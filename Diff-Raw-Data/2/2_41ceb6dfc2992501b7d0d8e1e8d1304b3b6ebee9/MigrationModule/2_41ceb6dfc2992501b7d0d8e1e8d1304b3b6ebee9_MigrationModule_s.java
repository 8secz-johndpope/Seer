 package org.dcache.pool.migration;
 
 import java.util.Map;
 import java.util.HashMap;
 import java.util.List;
 import java.util.ArrayList;
 import java.util.NoSuchElementException;
 import java.util.Iterator;
 import java.util.Collection;
 import java.util.Arrays;
 import java.util.HashSet;
 import java.util.concurrent.ScheduledExecutorService;
 import java.util.regex.Pattern;
 import java.util.regex.Matcher;
 import java.io.StringWriter;
 import java.io.PrintWriter;
 
 import diskCacheV111.util.PnfsId;
 import diskCacheV111.util.CacheFileAvailable;
 import diskCacheV111.vehicles.Message;
 import diskCacheV111.vehicles.StorageInfo;
 
 import org.dcache.cells.AbstractCellComponent;
 import org.dcache.cells.CellCommandListener;
 import org.dcache.cells.CellMessageReceiver;
 import org.dcache.cells.CellStub;
 import org.dcache.pool.repository.Repository;
 import org.dcache.pool.repository.StickyRecord;
 import org.dcache.pool.repository.EntryState;
 
 import dmg.util.Args;
 import dmg.cells.nucleus.CellEndpoint;
 
 import org.apache.log4j.Logger;
 
 /**
  * Module for migrating files between pools.
  *
  * This module provides services for copying replicas from a source
  * pool to a set of target pools.  The repository state and sticky
  * list of both the source replica and the target replica can be
  * defined, supporting several use cases, including migration,
  * replication and caching.
  *
  * The module consists of two components: The MigrationModule class
  * provides the user interface and must run on the source pool. The
  * MigrationModuleServer must run on any pool that is to be used as a
  * transfer destination.
  *
  * Most of the functionality is implemented on the source pool. The
  * user executes commands to define jobs. A job consists of rules for
  * selecting replicas on the source pool, for selecting target pools,
  * defines the state of the target replica, and how the state of the
  * source replica must be updated.
  *
  * A job is idempotent, that is, it can be repeated without ill
  * effect. This is achieved by querying the set of target pools for
  * existing copies of the replica. If found, the transfer may be
  * skipped. Care is taken to check the state of the replica on the
  * target pool - and updating it if necessary.
  *
  * Jobs monitor the local repository for changes. If a replica changes
  * state before it is transfered, and the replica no longer passes the
  * selection criteria of the job, then it will not be transferred. If
  * it is in the process of being transferred, then the transfer is
  * cancelled. If the transfer has already completed, then nothing
  * happens.
  *
  * Jobs can be defined as permanent. A permanent job will monitor the
  * repository for state changes. Should a replica be added or change
  * state in such a way that is passes the selection criteria of the
  * job, then it is added to the transfer queue of the job. A permanent
  * job does not terminate, even if its transfer queue becomes
  * empty. Permanent jobs are saved to the pool setup file and restored
  * on pool start. (PERMANENT JOBS HAVE NOT BEEN IMPLEMENTD YET!)
  *
  * Each job schedules transfer tasks. Whereas a job defines a bulk
  * operation, a task encapsulates a transfer of a single replica.
  *
  * Most classes in this package are thread safe. Non of the classes
  * create threads themselves. Instead they rely on an injected
  * ScheduledExecutorService. Most cell communication is implemented
  * asynchronously.
  */
 public class MigrationModule
     extends AbstractCellComponent
     implements CellCommandListener,
                CellMessageReceiver
 {
     private final static Logger _log =
         Logger.getLogger(MigrationModule.class);
 
     private final List<Job> _alive = new ArrayList();
     private final Map<Integer,Job> _jobs = new HashMap();
     private final Map<Job,String> _commands = new HashMap();
     private final ModuleConfiguration _configuration =
         new ModuleConfiguration();
 
     private int _counter = 1;
 
     public void setCellEndpoint(CellEndpoint endpoint)
     {
         super.setCellEndpoint(endpoint);
         _configuration.setPoolName(getCellName());
     }
 
     public void setRepository(Repository repository)
     {
         _configuration.setRepository(repository);
     }
 
     public void setExecutor(ScheduledExecutorService executor)
     {
         _configuration.setExecutor(executor);
     }
 
     public void setPnfsStub(CellStub stub)
     {
         _configuration.setPnfsStub(stub);
     }
 
     public void setPoolManagerStub(CellStub stub)
     {
         _configuration.setPoolManagerStub(stub);
     }
 
     public void setPoolStub(CellStub stub)
     {
         _configuration.setPoolStub(stub);
     }
 
     /** Returns the job with the given id. */
     private synchronized Job getJob(int id)
         throws NoSuchElementException
     {
         Job job = _jobs.get(id);
         if (job == null) {
             throw new NoSuchElementException("Job not found");
         }
         return job;
     }
 
     private List<CacheEntryFilter> createFilters(Args args)
     {
         String state = args.getOpt("state");
         String sticky = args.getOpt("sticky");
         String sc = args.getOpt("storage");
         String pnfsid = args.getOpt("pnfsid");
 
         List<CacheEntryFilter> filters = new ArrayList();
 
         filters.add(new NotFilter(new StateFilter(EntryState.BROKEN)));
 
         if (sc != null) {
             filters.add(new StorageClassFilter(sc));
         }
 
         if (pnfsid != null) {
             filters.add(new PnfsIdFilter(new PnfsId(pnfsid)));
         }
 
         if (state != null) {
             if (state.equals("cached")) {
                 filters.add(new StateFilter(EntryState.CACHED));
             } else if (state.equals("precious")) {
                 filters.add(new StateFilter(EntryState.PRECIOUS));
             } else {
                 throw new IllegalArgumentException(state + ": Invalid state");
             }
         }
 
         if (sticky != null) {
             if (sticky.equals("")) {
                 filters.add(new StickyFilter());
             } else {
                 for (String owner: sticky.split(",")) {
                     filters.add(new StickyOwnerFilter(owner));
                 }
             }
         }
 
         return filters;
     }
 
     private RefreshablePoolList
         createPoolList(double spaceCost,
                        double cpuCost,
                        String type,
                        List<String> targets,
                        Collection<String> exclude)
     {
         CellStub poolManager = _configuration.getPoolManagerStub();
 
         if (type.equals("pool")) {
             return new FixedPoolList(targets);
         } else if (type.equals("pgroup")) {
             if (targets.size() != 1) {
                 throw new IllegalArgumentException(targets.toString() +
                                                    ": Only one target supported for -type=pgroup");
             }
             return new PoolListByPoolGroup(poolManager,
                                            exclude,
                                            spaceCost,
                                            cpuCost,
                                            targets.get(0));
         } else if (type.equals("link")) {
             if (targets.size() != 1) {
                 throw new IllegalArgumentException(targets.toString() +
                                                    ": Only one target supported for -type=link");
             }
             return new PoolListByLink(poolManager,
                                       exclude,
                                       spaceCost,
                                       cpuCost,
                                       targets.get(0));
         } else {
             throw new IllegalArgumentException(type + ": Invalid value");
         }
     }
 
     private PoolSelectionStrategy
         createPoolSelectionStrategy(String type)
     {
         if (type.equals("proportional")) {
             return new ProportionalPoolSelectionStrategy();
         } else if (type.equals("best")) {
             return new BestPoolSelectionStrategy();
         } else if (type.equals("random")) {
             return new RandomPoolSelectionStrategy();
         } else {
             throw new IllegalArgumentException(type + ": Invalid value");
         }
     }
 
     private final static Pattern STICKY_PATTERN =
         Pattern.compile("(\\w+)(\\((-?\\d+)\\))?");
 
     private StickyRecord parseStickyRecord(String s)
         throws IllegalArgumentException
     {
         Matcher matcher = STICKY_PATTERN.matcher(s);
         if (!matcher.matches()) {
             throw new IllegalArgumentException(s + ": Syntax error");
         }
         String owner = matcher.group(1);
         String lifetime = matcher.group(3);
         try {
             long expire = (lifetime == null) ? -1 : Integer.valueOf(lifetime);
             if (expire < -1) {
                 throw new IllegalArgumentException(lifetime + ": Invalid lifetime");
             } else if (expire > 0) {
                 expire = System.currentTimeMillis() + expire * 1000;
             }
             return new StickyRecord(owner, expire);
         } catch (NumberFormatException e) {
             throw new IllegalArgumentException(lifetime + ": Invalid lifetime");
         }
     }
 
     private CacheEntryMode
         createCacheEntryMode(String type)
     {
         String[] s = type.split("\\+");
         List<StickyRecord> records = new ArrayList();
 
         for (int i = 1; i < s.length; i++) {
             records.add(parseStickyRecord(s[i]));
         }
 
         if (s[0].equals("same")) {
             return new CacheEntryMode(CacheEntryMode.State.SAME, records);
         } else if (s[0].equals("cached")) {
             return new CacheEntryMode(CacheEntryMode.State.CACHED, records);
         } else if (s[0].equals("delete")) {
             return new CacheEntryMode(CacheEntryMode.State.DELETE, records);
         } else if (s[0].equals("removable")) {
             return new CacheEntryMode(CacheEntryMode.State.REMOVABLE, records);
         } else if (s[0].equals("precious")) {
             return new CacheEntryMode(CacheEntryMode.State.PRECIOUS, records);
         } else {
             throw new IllegalArgumentException(type + ": Invalid value");
         }
     }
 
     private synchronized int copy(Args args,
                                   String defaultSelect,
                                   String defaultTarget,
                                   String defaultSourceMode,
                                   String defaultTargetMode,
                                   String defaultRefresh)
     {
         String exclude = args.getOpt("exclude");
         boolean permanent = (args.getOpt("permanent") != null);
         String sourceMode = args.getOpt("smode");
         String targetMode = args.getOpt("tmode");
         String select = args.getOpt("select");
         String target = args.getOpt("target");
         String refresh = args.getOpt("refresh");
         String concurrency = args.getOpt("concurrency");
 
         if (select == null) {
             select = defaultSelect;
         }
         if (target == null) {
             target = defaultTarget;
         }
         if (sourceMode == null) {
             sourceMode = defaultSourceMode;
         }
         if (targetMode == null) {
             targetMode = defaultTargetMode;
         }
         if (refresh == null) {
             refresh = defaultRefresh;
         }
         if (concurrency == null) {
             concurrency = "1";
         }
 
         List<String> targets = new ArrayList();
         for (int i = 0; i < args.argc(); i++) {
             targets.add(args.argv(i));
         }
 
         Collection<String> excluded = new HashSet();
         excluded.add(_configuration.getPoolName());
         if (exclude != null) {
             excluded.addAll(Arrays.asList(exclude.split(",")));
         }
 
         JobDefinition definition =
             new JobDefinition(createFilters(args),
                               createCacheEntryMode(sourceMode),
                               createCacheEntryMode(targetMode),
                               createPoolSelectionStrategy(select),
                               createPoolList(1.0, 0.0, target, targets,
                                              excluded),
                               Integer.valueOf(refresh) * 1000);
 
         if (definition.targetMode.state == CacheEntryMode.State.DELETE
             || definition.targetMode.state == CacheEntryMode.State.REMOVABLE) {
             throw new IllegalArgumentException(targetMode + ": Invalid value");
         }
 
         int n = Integer.valueOf(concurrency);
         int id = _counter++;
         Job job = new Job(_configuration, definition);
         job.setConcurrency(n);
         _jobs.put(id, job);
         _alive.add(job);
         return id;
     }
 
 
     public final static String hh_migration_copy = "[options] <target> ...";
     public final static String fh_migration_copy =
         "Copies files to other pools. Unless filter options are specified,\n" +
         "all files on the source pool are copied.\n\n" +
 
         "The operation is idempotent, that is, it can safely be repeated\n" +
         "without creating extra copies of the files. If the replica exists\n" +
         "on any of the target pools, then it is not copied again.\n\n" +
 
         "Both the state of the local replica and that of the target replica\n" +
         "can be specified. If the target replica already exists, the state\n" +
         "is updated to be at least as strong as the specified target state,\n" +
         "that is, the lifetime of sticky bits is extended, but never reduced,\n" +
         "and cached can be changed to precious, but never the opposite.\n\n" +
 
 //         "Jobs can be marked permanent. Permanent jobs never terminate and\n" +
 //         "are stored in the pool setup file with the 'save' command. Permanent\n" +
 //         "jobs watch the repository and copy any new replicas that match the\n" +
 //         "selection criteria.\n\n" +
 
         "Syntax:\n" +
         "  copy [options] <target> ...\n\n" +
         "Options:\n" +
         "  -state=cached|precious\n" +
         "          Only copy replicas in the given state.\n"+
         "  -sticky[=<owner>[,<owner> ...]]\n" +
         "          Only copy sticky replicas. Can optionally be limited to\n" +
         "          the list of owners. A sticky flag for each owner must be\n" +
         "          present for the replica to be selected.\n" +
         "  -storage=<class>\n" +
         "          Only copy replicas with the given storage class.\n" +
         "  -pnfsid=<pnfsid>\n" +
         "          Only copy the replica with the given PNFS ID.\n" +
         "  -smode=same|cached|precious|removable|delete[+<owner>[(<lifetime>)] ...]\n" +
         "          Update the local replica to the given mode after transfer.\n" +
         "          'same' does not change the local state (this is the\n" +
         "          default), 'cached' marks it cached, 'precious' marks it\n" +
         "          precious, 'removable' marks it cached and strips all\n" +
         "          existing sticky flags, and 'delete' deletes the replica.\n" +
         "          An optional list of sticky flags can be specified. The\n" +
         "          lifetime is in seconds. A lifetime of 0 causes the flag\n" +
         "          to immediate expire. Notice that existing sticky flags\n" +
         "          of the same owner are overwritten.\n" +
         "  -tmode=same|cached|precious[+<owner>[(<lifetime>)] ...]\n" +
         "          Set the mode of the target replica. 'same' applies the\n" +
         "          state and sticky bits of the local replica (this is the\n" +
         "          default), 'cached' marks it cached, 'precious' marks it\n" +
         "          precious. An optional list of sticky flags can be\n" +
         "          specified. The lifetime is in seconds.\n" +
         "  -select=proportional|best|random\n" +
         "          Determines how a pool is selected from the set of target\n" +
         "          pools. 'proportional' selects a pool with a probability\n" +
         "          inversely proportional to the cost of the pool. 'best'\n" +
         "          selects the pool with the lowest cost. 'random' selects\n" +
         "          a pool randomly. The default is 'proportional'.\n" +
         "  -target=pool|pgroup|link\n" +
         "          Determines the interpretation of the target names. 'pool'\n" +
         "          is the default.\n" +
         "  -refresh=<time>\n" +
         "          Specifies the period in seconds of when target pool\n" +
         "          information is queried from the pool manager. The\n" +
         "          default is 300 seconds.\n" +
         "  -exclude=<pool>[,<pool> ...]\n" +
         "          Exclude target pools.\n" +
         "  -concurrency=<concurrency>\n" +
         "          Specifies how many concurrent transfers to perform.\n" +
         "          Defaults to 1.\n";
 //         "  -permanent\n" +
 //         "          Mark job as permanent.\n" +
 //         "  -dry-run\n" +
 //         "          Perform all the steps without actually copying anything\n" +
 //         "          or updating the state.";
     public synchronized String ac_migration_copy_$_1_99(Args args)
     {
         int id = copy(args, "proportional", "pool", "same", "same", "300");
         String command = "migration copy " + args.toString();
         _commands.put(_jobs.get(id), command);
         return "[" + id + "] " + command;
     }
 
     public final static String hh_migration_migrate = "[options] <target> ...";
     public final static String fh_migration_migrate =
         "Migrates replicas to other pools. The source replica is deleted.\n" +
         "Accepts the same options as 'migration copy'. Corresponds to\n\n" +
         "     migration copy -smode=delete -tmode=same\n";
     public String ac_migration_migrate_$_1_99(Args args)
     {
         int id = copy(args, "proportional", "pool", "delete", "same", "300");
         String command = "migration migrate " + args.toString();
         _commands.put(_jobs.get(id), command);
         return "[" + id + "] " + command;
     }
 
     public final static String hh_migration_replicate =
         "[options] <target> ...";
     public final static String fh_migration_replicate =
         "Caches replicas on other pools. Accepts the same options as\n" +
         "'migration copy'. Corresponds to\n\n" +
         "     migration copy -smode=same -tmode=cached\n";
     public String ac_migration_replicate_$_1_99(Args args)
     {
        int id = copy(args, "proportional", "pool", "delete", "same", "300");
         String command = "migration replicate " + args.toString();
         _commands.put(_jobs.get(id), command);
         return "[" + id + "] " + command;
     }
 
     public final static String hh_migration_suspend =
         "<job>";
     public final static String fh_migration_suspend =
         "Suspends a migration job. A suspended job finishes ongoing\n" +
         "transfers, but is does not start any new transfer.";
     public String ac_migration_suspend_$_1(Args args)
     {
         int id = Integer.valueOf(args.argv(0));
         Job job = getJob(id);
         job.suspend();
         return String.format("[%d] Suspended     %s", id, _commands.get(job));
     }
 
     public final static String hh_migration_resume =
         "<job>";
     public final static String fh_migration_resume =
         "Resumes a suspended migration job.";
     public String ac_migration_resume_$_1(Args args)
     {
         int id = Integer.valueOf(args.argv(0));
         Job job = getJob(id);
         job.resume();
         return String.format("[%d] Resumes       %s", id, _commands.get(job));
     }
 
     public final static String hh_migration_cancel =
         "<job>";
     public final static String fh_migration_cancel =
         "Cancels a migration job. Ongoing transfers are killed.";
     public String ac_migration_cancel_$_1(Args args)
     {
         int id = Integer.valueOf(args.argv(0));
         Job job = getJob(id);
         job.cancel();
         return String.format("[%d] Cancelling    %s", id, _commands.get(job));
     }
 
     public final static String hh_migration_clear =
         "<job>";
     public final static String fh_migration_clear =
         "Removes completed migration jobs. For reference, information about\n" +
         "migration jobs are kept until explicitly cleared.\n";
     public synchronized String ac_migration_clear(Args args)
     {
         Iterator<Job> i = _jobs.values().iterator();
         while (i.hasNext()) {
             Job job = i.next();
             switch (job.getState()) {
             case CANCELLED:
             case FINISHED:
                 i.remove();
                 _commands.remove(job);
                 break;
             default:
                 break;
             }
         }
         return "";
     }
 
     public final static String fh_migration_ls =
         "Lists all migration jobs.";
     public synchronized String ac_migration_ls(Args args)
     {
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         for (Map.Entry<Integer,Job> entry: _jobs.entrySet()) {
             pw.println(String.format("[%d] %s", entry.getKey(), ""));
         }
         return sw.toString();
     }
 
     public final static String hh_migration_info =
         "<job>";
     public final static String fh_migration_info =
         "Shows detailed information about a migration job.";
     public String ac_migration_info_$_1(Args args)
         throws NoSuchElementException
     {
         int id = Integer.valueOf(args.argv(0));
         StringWriter sw = new StringWriter();
         getJob(id).getInfo(new PrintWriter(sw));
         return sw.toString();
     }
 
     public synchronized
         void messageArrived(PoolMigrationCopyFinishedMessage message)
     {
         /* To avoid callbacks or an extra update thread, we lazily
          * move jobs out of _alive.
          */
         Iterator<Job> i = _alive.iterator();
         while (i.hasNext()) {
             Job job = i.next();
             switch (job.getState()) {
             case CANCELLED:
             case FINISHED:
                 i.remove();
                 break;
             default:
                 job.messageArrived(message);
             }
         }
     }
 }
