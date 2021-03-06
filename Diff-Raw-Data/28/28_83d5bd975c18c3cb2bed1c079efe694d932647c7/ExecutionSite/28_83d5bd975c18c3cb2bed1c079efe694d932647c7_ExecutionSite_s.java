 /* This file is part of VoltDB.
  * Copyright (C) 2008-2010 VoltDB L.L.C.
  *
  * VoltDB is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * VoltDB is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package org.voltdb;
 
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.util.Deque;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.concurrent.atomic.AtomicInteger;
 
 import org.apache.log4j.Level;
 import org.apache.log4j.Logger;
 import org.apache.log4j.NDC;
 import org.voltdb.SnapshotSiteProcessor.SnapshotTableTask;
 import org.voltdb.catalog.Catalog;
 import org.voltdb.catalog.CatalogMap;
 import org.voltdb.catalog.Cluster;
 import org.voltdb.catalog.Database;
 import org.voltdb.catalog.Host;
 import org.voltdb.catalog.Procedure;
 import org.voltdb.catalog.Site;
 import org.voltdb.client.ClientResponse;
 import org.voltdb.debugstate.ExecutorContext;
 import org.voltdb.dtxn.SimpleDtxnConnection;
 import org.voltdb.dtxn.SiteConnection;
 import org.voltdb.exceptions.EEException;
 import org.voltdb.exceptions.SQLException;
 import org.voltdb.exceptions.SerializableException;
 import org.voltdb.jni.ExecutionEngine;
 import org.voltdb.jni.ExecutionEngineIPC;
 import org.voltdb.jni.ExecutionEngineJNI;
 import org.voltdb.jni.MockExecutionEngine;
 import org.voltdb.messages.FragmentResponse;
 import org.voltdb.messages.FragmentTask;
 import org.voltdb.messages.InitiateResponse;
 import org.voltdb.messages.InitiateTask;
 import org.voltdb.messaging.FastDeserializer;
 import org.voltdb.messaging.Mailbox;
 import org.voltdb.messaging.Messenger;
 import org.voltdb.messaging.VoltMessage;
 import org.voltdb.utils.DumpManager;
 import org.voltdb.utils.Encoder;
 import org.voltdb.utils.EstTime;
 import org.voltdb.utils.LogKeys;
 import org.voltdb.utils.VoltLoggerFactory;
 
 /**
  * The main executor of transactional work in the system. Controls running
  * stored procedures and manages the execution engine's running of plan
  * fragments. Interacts with the DTXN system to get work to do. The thread might
  * do other things, but this is where the good stuff happens.
  */
 public class ExecutionSite implements Runnable, DumpManager.Dumpable {
     private static final Logger log = Logger.getLogger(ExecutionSite.class.getName(), VoltLoggerFactory.instance());
     private static final Logger hostLog = Logger.getLogger("HOST", VoltLoggerFactory.instance());
     private static AtomicInteger siteIndexCounter = new AtomicInteger(0);
 
     private final int siteIndex = siteIndexCounter.getAndIncrement();
     public int siteId;
 
     SiteConnection dtxnConn;
     final ExecutionEngine ee;
     final HsqlBackend hsql;
 
     // Catalog objects
     Catalog catalog;
     public CatalogContext m_context;
     Host host;
     Site site;
 
     final HashMap<String, VoltProcedure> procs = new HashMap<String, VoltProcedure>(16, (float) .1);
     VoltProcedure currentProc = null;
     public volatile boolean m_shouldContinue = true;
     private InitiateTask m_currentSPTask = null;
 
     // store the id used by the DumpManager to identify this execution site
     public final String m_dumpId;
     public long m_currentDumpTimestamp = 0;
 
     /**
      * When the system procedure @UpdateLogging runs and updates the log levels for loggers it will
      * call updateBackendLogLevels to indicate that the log levels have changed.
      */
     private volatile boolean m_haveToUpdateLogLevels = false;
 
     // The time in ms since epoch of the last call to ExecutionEngine.tick(...)
     long lastTickTime = 0;
     long lastCommittedTxnId = 0;
     private long m_currentTxnId = 0;
     int m_currentInitiatorSiteId;
 
     // Each execution site manages snapshot using a SnapshotSiteProcessor
     private final SnapshotSiteProcessor m_snapshotter;
 
     private final Watchdog m_watchdog;
     private class Watchdog extends Thread {
         private volatile boolean m_shouldContinue = true;
         private volatile boolean m_petted = false;
         private final int m_siteIndex;
         private final int m_siteId;
         private Thread m_watchThread = null;
         public Watchdog(final int siteIndex, final int siteId) {
             super(null, null, "ExecutionSite " + siteIndex + " siteId: " + siteId + " watchdog ", 262144);
             m_siteIndex = siteIndex;
             m_siteId = siteId;
         }
 
         public void pet() {
             m_petted = true;
         }
 
         @Override
         public void run() {
             if (m_watchThread == null) {
                 throw new RuntimeException("Use start(Thread watchThread) not Thread.start()");
             }
             try {
                 Thread.sleep(30000);
             } catch (final InterruptedException e) {
                 return;
             }
             while (m_shouldContinue) {
                 try {
                     Thread.sleep(5000);
                 } catch (final InterruptedException e) {
                     return;
                 }
                 if (!m_petted) {
                     final StackTraceElement trace[] = m_watchThread.getStackTrace();
                     final Throwable throwable = new Throwable();
                     throwable.setStackTrace(trace);
                     log.l7dlog( Level.WARN, LogKeys.org_voltdb_ExecutionSite_Watchdog_possibleHang.name(), new Object[]{ m_siteIndex, m_siteId}, throwable);
                 }
                 m_petted = false;
             }
         }
 
         @Override
         public void start() {
             throw new UnsupportedOperationException("Use start(Thread watchThread)");
         }
 
         public void start(final Thread thread) {
             m_watchThread = thread;
             super.start();
         }
     }
 
     // Associate the system procedure planfragment ids to wrappers.
     // Planfragments are registered when the procedure wrapper is init()'d.
     private final HashMap<Long, VoltSystemProcedure> m_registeredSysProcPlanFragments =
         new HashMap<Long, VoltSystemProcedure>();
 
     public void registerPlanFragment(final long pfId, final VoltSystemProcedure proc) {
         synchronized (m_registeredSysProcPlanFragments) {
             assert(m_registeredSysProcPlanFragments.containsKey(pfId) == false);
             m_registeredSysProcPlanFragments.put(pfId, proc);
         }
     }
 
     /*
      * undoWindowBegin is the first token of a stored procedure invocation.
      * Undoing it will always undo all the work done in the transaction.
      * undoWindowEnd is the counter for the next undo token to be generated.
      * It might be incremented in the ExecutionSite or in the VoltProcedure
      * every time a batch is executed. To release all the undo data associated
      * with the last transaction undoWindowEnd needs to be used instead of
      * undoWindowBegin (which is the first undo token of the transaction).
      */
     private final static long kInvalidUndoToken = -1L;
     //public boolean undoWindowActive = false;
     private long txnBeginUndoToken = kInvalidUndoToken;
     private long batchBeginUndoToken = kInvalidUndoToken;
     private long latestUndoToken = 0L;
 
 
     public void tick() {
         // invoke native ee tick if at least one second has passed
         final long time = EstTime.currentTimeMillis();
         if ((time - lastTickTime) >= 1000) {
             if ((lastTickTime != 0) && (ee != null)) {
                 ee.tick(time, lastCommittedTxnId);
             }
             lastTickTime = time;
         }
 
         m_snapshotter.doSnapshotWork(ee);
 
         // sing it: stay'n alive..
         m_watchdog.pet();
     }
 
     /**
      * Set the txn id from the WorkUnit and set/release undo tokens as
      * necessary. The DTXN currently has no notion of maintaining undo
      * tokens beyond the life of a transaction so it is up to the execution
      * site to release the undo data in the EE up until the current point
      * when the transaction ID changes.
      */
     public final void beginNewTxn(long txnId, boolean readOnly) {
         // push log level changes to the ee.
         if (ee != null && m_haveToUpdateLogLevels) {
             m_haveToUpdateLogLevels = false;
             ee.setLogLevels(org.voltdb.jni.EELoggers.getLogLevels());
         }
 
         m_currentTxnId = txnId;
 
         if (!readOnly) {
             assert(txnBeginUndoToken == kInvalidUndoToken);
             assert(batchBeginUndoToken == kInvalidUndoToken);
 
             txnBeginUndoToken = latestUndoToken;
         }
     }
 
     public final void beginNewBatch(boolean readOnly) {
         if (!readOnly) {
             assert(latestUndoToken != kInvalidUndoToken);
             assert(txnBeginUndoToken != kInvalidUndoToken);
             assert(latestUndoToken >= txnBeginUndoToken);
 
             batchBeginUndoToken = latestUndoToken;
         }
     }
 
     public final void rollbackTransaction(boolean readOnly) {
         if (!readOnly) {
             assert(latestUndoToken != kInvalidUndoToken);
             assert(txnBeginUndoToken != kInvalidUndoToken);
             assert(latestUndoToken >= txnBeginUndoToken);
 
             // return if no work was done
             if (latestUndoToken == txnBeginUndoToken) {
                 return;
             }
 
             // make sure this is sensical
             assert(latestUndoToken > txnBeginUndoToken);
 
             // actually do the undo work
             ee.undoUndoToken(txnBeginUndoToken);
         }
     }
 
     public final void rollbackBatch(boolean readOnly) {
         assert(false);
     }
 
     public final void completeTransaction(boolean readOnly) {
         if (!readOnly) {
             assert(latestUndoToken != kInvalidUndoToken);
             assert(txnBeginUndoToken != kInvalidUndoToken);
             assert(latestUndoToken >= txnBeginUndoToken);
             //assert((batchBeginUndoToken != kInvalidUndoToken) || (latestUndoToken == txnBeginUndoToken));
             //assert((latestUndoToken == txnBeginUndoToken) || (batchBeginUndoToken != kInvalidUndoToken));
 
             // release everything through the end of the current window.
             if (latestUndoToken > txnBeginUndoToken)
                 ee.releaseUndoToken(latestUndoToken);
 
             // these only really need to be reset for error checking purposes
             txnBeginUndoToken = kInvalidUndoToken;
             batchBeginUndoToken = kInvalidUndoToken;
         }
     }
 
     public long getNextUndoToken() {
         return ++latestUndoToken;
     }
 
     /**
      * SystemProcedures are "friends" with ExecutionSites and granted
      * access to internal state via m_systemProcedureContext.
      */
     public interface SystemProcedureExecutionContext {
         public Catalog getCatalog();
         public Database getDatabase();
         public Cluster getCluster();
         public Site getSite();
         public ExecutionEngine getExecutionEngine();
         public long getLastCommittedTxnId();
         public long getNextUndo();
         public long getTxnId();
         public Object getOperStatus();
         public ExecutionSite getExecutionSite();
     }
 
     protected class SystemProcedureContext implements SystemProcedureExecutionContext {
         public Catalog getCatalog()                 { return catalog; }
         public Database getDatabase()               { return m_context.database; }
         public Cluster getCluster()                 { return m_context.cluster; }
         public Site getSite()                       { return site; }
         public ExecutionEngine getExecutionEngine() { return ee; }
         public long getLastCommittedTxnId()         { return lastCommittedTxnId; }
         public long getNextUndo()                   { return getNextUndoToken(); }
         public long getTxnId()                      { return getCurrentTxnId(); }
         public String getOperStatus()               { return VoltDB.getOperStatus(); }
         public ExecutionSite getExecutionSite()     { return ExecutionSite.this; }
     }
 
     SystemProcedureContext m_systemProcedureContext;
 
     /** For test MockExecutionSite */
     ExecutionSite() {
         m_systemProcedureContext = new SystemProcedureContext();
         m_watchdog = null;
         ee = null;
         hsql = null;
         m_dumpId = "MockExecSite";
         m_snapshotter = null;
     }
 
     /**
      * Initialize the StoredProcedure runner and EE for this Site.
      * @param siteManager
      * @param siteId
      * @param context A reference to the current catalog context
      * newlines that, when executed, reconstruct the complete m_catalog.
      */
     ExecutionSite(final int siteId, final CatalogContext context, String serializedCatalog) {
         hostLog.l7dlog( Level.TRACE, LogKeys.host_ExecutionSite_Initializing.name(), new Object[] { String.valueOf(siteId) }, null);
 
         if (serializedCatalog == null)
             serializedCatalog = context.catalog.serialize();
 
         m_watchdog = new Watchdog(siteId, siteIndex);
         this.siteId = siteId;
         m_context = context;
         catalog = context.catalog;
         site = m_context.cluster.getSites().get(Integer.toString(siteId));
         m_systemProcedureContext = new SystemProcedureContext();
 
         m_dumpId = "ExecutionSite." + String.valueOf(siteId);
         DumpManager.register(m_dumpId, this);
 
         // get an array of all the initiators
         int initiatorCount = 0;
         for (final Site s : m_context.cluster.getSites())
             if (s.getIsexec() == false)
                 initiatorCount++;
         final int[] initiatorIds = new int[initiatorCount];
         int index = 0;
         for (final Site s : m_context.cluster.getSites())
             if (s.getIsexec() == false)
                 initiatorIds[index++] = Integer.parseInt(s.getTypeName());
 
         // set up the connection to the dtxn
         final Messenger messenger = VoltDB.instance().getMessenger();
         final Mailbox mqueue = messenger.createMailbox(siteId, VoltDB.DTXN_MAILBOX_ID, null);
         dtxnConn = new SimpleDtxnConnection(this, mqueue, initiatorIds);
 
         // An execution site can be backed by HSQLDB, by volt's EE accessed
         // via JNI or by volt's EE accessed via IPC.  When backed by HSQLDB,
         // the VoltProcedure interface invokes HSQLDB directly through its
         // hsql Backend member variable.  The real volt backend is encapsulated
         // by the ExecutionEngine class. This class has implementations for both
         // JNI and IPC - and selects the desired implementation based on the
         // value of this.eeBackend.
         HsqlBackend hsqlTemp = null;
         ExecutionEngine eeTemp = null;
         try {
             final BackendTarget target = VoltDB.getEEBackendType();
             if (target == BackendTarget.HSQLDB_BACKEND) {
                 hsqlTemp = new HsqlBackend(siteId);
                 final String hexDDL = m_context.database.getSchema();
                 final String ddl = Encoder.hexDecodeToString(hexDDL);
                 final String[] commands = ddl.split(";");
                 for (String command : commands) {
                     command = command.trim();
                     if (command.length() == 0) {
                         continue;
                     }
                     hsqlTemp.runDDL(command);
                 }
                 eeTemp = new MockExecutionEngine();
             }
             else if (target == BackendTarget.NATIVE_EE_JNI) {
                 // set up the EE
                 eeTemp = new ExecutionEngineJNI(this, m_context.cluster.getRelativeIndex(), siteId);
                 eeTemp.loadCatalog(serializedCatalog);
                 lastTickTime = EstTime.currentTimeMillis();
                 eeTemp.tick( lastTickTime, 0);
             }
             else {
                 // set up the EE over IPC
                 eeTemp = new ExecutionEngineIPC(this, m_context.cluster.getRelativeIndex(), siteId, target);
                 eeTemp.loadCatalog(serializedCatalog);
                 lastTickTime = EstTime.currentTimeMillis();
                 eeTemp.tick( lastTickTime, 0);
             }
         }
         // just print error info an bail if we run into an error here
         catch (final Exception ex) {
             hostLog.l7dlog( Level.FATAL, LogKeys.host_ExecutionSite_FailedConstruction.name(), new Object[] { siteId, siteIndex }, ex);
             VoltDB.crashVoltDB();
         }
         ee = eeTemp;
         hsql = hsqlTemp;
 
         // load up all the stored procedures
         loadProceduresFromCatalog();
         m_snapshotter = new SnapshotSiteProcessor();
     }
 
    public boolean updateCatalog() {
         m_context = VoltDB.instance().getCatalogContext();
         loadProceduresFromCatalog();
         return true;
     }
 
     void loadProceduresFromCatalog() {
         m_registeredSysProcPlanFragments.clear();
         procs.clear();
         // load up all the stored procedures
         final CatalogMap<Procedure> catalogProcedures = m_context.database.getProcedures();
         for (final Procedure proc : catalogProcedures) {
             VoltProcedure wrapper = null;
 
             if (proc.getHasjava()) {
                 final String className = proc.getClassname();
                 Class<?> procClass = null;
                 try {
                     procClass = m_context.classForProcedure(className);
                 }
                 catch (final ClassNotFoundException e) {
                     hostLog.l7dlog(
                             Level.WARN,
                             LogKeys.host_ExecutionSite_GenericException.name(),
                             new Object[] { siteId, siteIndex },
                             e);
                     VoltDB.crashVoltDB();
                 }
                 try {
                     wrapper = (VoltProcedure) procClass.newInstance();
                 }
                 catch (final InstantiationException e) {
                     hostLog.l7dlog( Level.WARN, LogKeys.host_ExecutionSite_GenericException.name(), new Object[] { siteId, siteIndex }, e);
                 }
                 catch (final IllegalAccessException e) {
                     hostLog.l7dlog( Level.WARN, LogKeys.host_ExecutionSite_GenericException.name(), new Object[] { siteId, siteIndex }, e);
                 }
             }
             else {
                 wrapper = new VoltProcedure.StmtProcedure();
             }
 
             wrapper.init(this, proc, VoltDB.getEEBackendType(), hsql, m_context.cluster);
             procs.put(proc.getTypeName(), wrapper);
         }
     }
 
     public long getCurrentTxnId() {
         return m_currentTxnId;
     }
 
     public InitiateTask getCurrentSPTask()
     {
         return m_currentSPTask;
     }
 
     public int getCurrentInitiatorSiteId()
     {
         return m_currentInitiatorSiteId;
     }
 
     public void setConnectionForTest(final SiteConnection connection) {
         dtxnConn = connection;
     }
 
     /**
      * Primary run method that is invoked a single time when the thread is started.
      * Has the opportunity to do startup config.
      */
     @Override
     public void run() {
         // pick a name with four places for siteid so it can be sorted later
         // bit hackish, sorry
         String name = "ExecutionSite:";
         if (siteId < 10) name += "0";
         if (siteId < 100) name += "0";
         if (siteId < 1000) name += "0";
         name += String.valueOf(siteId);
         Thread.currentThread().setName(name);
 
         NDC.push("ExecutionSite - " + siteId + " index " + siteIndex);
         if (VoltDB.getUseWatchdogs()) {
             m_watchdog.start(Thread.currentThread());
         }
         if (VoltDB.getUseThreadAffinity()) {
             final boolean startingAffinity[] = org.voltdb.utils.ThreadUtils.getThreadAffinity();
             for (int ii = 0; ii < startingAffinity.length; ii++) {
                 log.l7dlog( Level.INFO, LogKeys.org_voltdb_ExecutionSite_StartingThreadAffinity.name(), new Object[] { startingAffinity[ii] }, null);
                 startingAffinity[ii] = false;
             }
             startingAffinity[ siteIndex % startingAffinity.length] = true;
             org.voltdb.utils.ThreadUtils.setThreadAffinity(startingAffinity);
             final boolean endingAffinity[] = org.voltdb.utils.ThreadUtils.getThreadAffinity();
             for (int ii = 0; ii < endingAffinity.length; ii++) {
                 log.l7dlog( Level.INFO, LogKeys.org_voltdb_ExecutionSite_EndingThreadAffinity.name(), new Object[] { endingAffinity[ii] }, null);
                 startingAffinity[ii] = false;
             }
         }
         try {
             // call recurable run and let it know this is the base call,
             // meaning it's allowed/expected to return null
             Object test = dtxnConn.recursableRun(true);
             assert(test == null);
         }
         catch (final RuntimeException e) {
             hostLog.l7dlog( Level.ERROR, LogKeys.host_ExecutionSite_RuntimeException.name(), e);
             throw e;
         }
         shutdown();
     }
 
     public FragmentResponse processFragmentTask(
             final HashMap<Integer,List<VoltTable>> dependencies, final VoltMessage task) {
         // change to true of release dependencies is required.
         {
             final FragmentTask ftask = (FragmentTask) task;
             m_currentInitiatorSiteId = ftask.getInitiatorSiteId();
 
             assert(ftask.getFragmentCount() == 1);
             final long fragmentId = ftask.getFragmentId(0);
             final int outputDepId = ftask.getOutputDepId(0);
 
             final FragmentResponse currentFragResponse = new FragmentResponse(ftask, siteId);
 
             // this is a horrible performance hack, and can be removed with small changes
             // to the ee interface layer.. (rtb: not sure what 'this' encompasses...)
             final ByteBuffer paramData = ftask.getParameterDataForFragment(0);
             ParameterSet params = null;
 
             if (paramData != null) {
                 final FastDeserializer fds = new FastDeserializer(paramData);
                 try {
                     params = fds.readObject(ParameterSet.class);
                 }
                 catch (final IOException e) {
                     hostLog.l7dlog( Level.FATAL,
                             LogKeys.host_ExecutionSite_FailedDeserializingParamsForFragmentTask.name(), e);
                     VoltDB.crashVoltDB();
                 }
             }
             else {
                 params = new ParameterSet();
             }
 
             if (ftask.isSysProcTask()) {
                 return processSysprocFragmentTask(dependencies, fragmentId,
                                            currentFragResponse, params);
             }
             else {
 
                 // pass attached dependencies to the EE (for non-sysproc work).
                 if (dependencies != null)
                     ee.stashWorkUnitDependencies(dependencies);
 
                 // start the clock on this statement
                 ProcedureProfiler.startStatementCounter(fragmentId);
 
                 final int inputDepId = ftask.getOnlyInputDepId(0);
 
                 /*
                  * Currently the error path when executing plan fragments
                  * does not adequately distinguish between fatal errors and
                  * abort type errors that should result in a roll back.
                  * Assume that it is ninja: succeeds or doesn't return.
                  * No roll back support.
                  */
                 currentFragResponse.setStatus(FragmentResponse.SUCCESS, null);
                 try {
                     final DependencyPair dep = ee.executePlanFragment(fragmentId,
                                                                       outputDepId,
                                                                       inputDepId,
                                                                       params,
                                                                       m_currentTxnId,
                                                                       lastCommittedTxnId,
                                                                       getNextUndoToken());
 
                     sendDependency(currentFragResponse, dep.depId, dep.dependency);
 
                 } catch (final EEException e) {
                     hostLog.l7dlog( Level.TRACE, LogKeys.host_ExecutionSite_ExceptionExecutingPF.name(), new Object[] { fragmentId }, e);
                     currentFragResponse.setStatus(FragmentResponse.UNEXPECTED_ERROR, e);
                 } catch (final SQLException e) {
                     hostLog.l7dlog( Level.TRACE, LogKeys.host_ExecutionSite_ExceptionExecutingPF.name(), new Object[] { fragmentId }, e);
                     currentFragResponse.setStatus(FragmentResponse.UNEXPECTED_ERROR, e);
                 }
 
                 ProcedureProfiler.stopStatementCounter();
                 return currentFragResponse;
             }
         }
     }
 
 
     public FragmentResponse processSysprocFragmentTask(final HashMap<Integer,List<VoltTable>> dependencies,
             final long fragmentId, final FragmentResponse currentFragResponse,
             final ParameterSet params) {
         // assume success. errors correct this assumption as they occurr
         currentFragResponse.setStatus(FragmentResponse.SUCCESS, null);
 
         VoltSystemProcedure proc = null;
         synchronized (m_registeredSysProcPlanFragments) {
             proc = m_registeredSysProcPlanFragments.get(fragmentId);
         }
 
         try {
             final DependencyPair dep
                 = proc.executePlanFragment(dependencies,
                                            fragmentId,
                                            params,
                                            m_systemProcedureContext);
             if (dep == null) {
                 log.l7dlog(Level.TRACE,
                            LogKeys.org_voltdb_ExecutionSite_SysprocReturnedNoDependencies.name(),
                            null);
             }
 
             sendDependency(currentFragResponse, dep.depId, dep.dependency);
 
         }
         catch (final EEException e)
         {
             hostLog.l7dlog( Level.TRACE, LogKeys.host_ExecutionSite_ExceptionExecutingPF.name(), new Object[] { fragmentId }, e);
             currentFragResponse.setStatus(FragmentResponse.UNEXPECTED_ERROR, e);
         }
         catch (final SQLException e)
         {
             hostLog.l7dlog( Level.TRACE, LogKeys.host_ExecutionSite_ExceptionExecutingPF.name(), new Object[] { fragmentId }, e);
             currentFragResponse.setStatus(FragmentResponse.UNEXPECTED_ERROR, e);
         }
         catch (final Exception e)
         {
             // Just indicate that we failed completely
             currentFragResponse.setStatus(FragmentResponse.UNEXPECTED_ERROR, new SerializableException(e));
         }
 
         return currentFragResponse;
     }
 
 
     /*
      * Do snapshot work exclusively until there is no more. Also blocks
      * until the syncing and closing of snapshot data targets has completed.
      */
     public HashSet<Exception> completeSnapshotWork() throws InterruptedException {
         return m_snapshotter.completeSnapshotWork(ee);
     }
 
 
     public InitiateResponse processInitiateTask(final VoltMessage task) {
         final InitiateTask itask = (InitiateTask)task;
 
         // keep track of the current procedure
         m_currentSPTask = itask;
         m_currentInitiatorSiteId = itask.getInitiatorSiteId();
 
         //ClientResponse response = null;
         final VoltProcedure wrapper = procs.get(itask.getStoredProcedureName());
         assert(wrapper != null); // existed in ClientInterface's catalog.
 
         final InitiateResponse response = new InitiateResponse(itask);
 
         try {
             final ClientResponseImpl cr = wrapper.call(itask.getParameters());
             response.setResults( cr, itask);
             //response.setClientHandle(task.getClientHandle());
         }
         catch (final ExpectedProcedureException e) {
             log.l7dlog( Level.TRACE, LogKeys.org_voltdb_ExecutionSite_ExpectedProcedureException.name(), e);
             response.setResults(
                     new ClientResponseImpl(
                             ClientResponse.GRACEFUL_FAILURE,
                             new VoltTable[]{},
                             e.toString()));
         }
         catch (final Exception e) {
             // Show the WHOLE exception in the log
             hostLog.l7dlog( Level.ERROR, LogKeys.host_ExecutionSite_UnexpectedProcedureException.name(), e);
             VoltDB.crashVoltDB();
         }
 
         log.l7dlog( Level.TRACE, LogKeys.org_voltdb_ExecutionSite_SendingCompletedWUToDtxn.name(), null);
         if (m_currentTxnId > lastCommittedTxnId) {
             lastCommittedTxnId = m_currentTxnId;
         }
 
         // note whether this plan fragment was successful
         m_currentSPTask = null;
 
         return response;
     }
 
     public void sendDependency(final FragmentResponse currentFragResponse,
                                final int dependencyId, final VoltTable dependency) {
         if (log.isTraceEnabled()) {
             log.l7dlog( Level.TRACE, LogKeys.org_voltdb_ExecutionSite_SendingDependency.name(), new Object[] { dependencyId }, null);
         }
 
         currentFragResponse.addDependency(dependencyId, dependency);
     }
 
     void startShutdown() {
         m_shouldContinue = false;
     }
 
     /**
      * Shutdown all resources that need to be shutdown for this <code>ExecutionSite</code>.
      * May be called twice if recurssing via recursableRun(). Protected against that..
      */
     private boolean haveShutdownAlready;
     void shutdown() {
         if (haveShutdownAlready) {
             return;
         }
         haveShutdownAlready = true;
         m_shouldContinue = false;
 
         boolean finished = false;
         while (!finished) {
             try {
                 if (m_watchdog.isAlive()) {
                     m_watchdog.m_shouldContinue = false;
                     m_watchdog.interrupt();
                     m_watchdog.join();
                 }
 
                 dtxnConn.shutdown();
 
                 ProcedureProfiler.flushProfile();
                 if (hsql != null) {
                     hsql.shutdown();
                 }
                 if (ee != null) {
                     ee.release();
                 }
                 finished = true;
             } catch (final InterruptedException e) {
                 //Ignore interruptions and finish shutting down.
             }
         }
 
         m_snapshotter.shutdown();
     }
 
     /**
      * Signal that the log settings have changed and that the EE should update
      * it's backend's log levels.
      */
     public void updateBackendLogLevels() {
         m_haveToUpdateLogLevels = true;
     }
 
     public void initiateSnapshots(Deque<SnapshotTableTask> tasks) {
         m_snapshotter.initiateSnapshots(ee, tasks);
     }
 
     @Override
     public void goDumpYourself(final long timestamp) {
         // queue a threadsafe dump in the most horrible way
         // the dtxn conn will create a message for its message queue,
         // which will cause it to ask this object to dump, using a special workunit
         m_currentDumpTimestamp = timestamp;
         if (dtxnConn instanceof SimpleDtxnConnection)
             ((SimpleDtxnConnection) dtxnConn).requestDebugMessage(true);
 
         // do an unsafe dump in case the execution site is hung
         // unsafe dumps are actually really hard to do
         // anything with an iterator chokes pretty quick under load
         //StringBuilder sb = new StringBuilder();
         //getDumpContents(sb);
         //DumpManager.putDump(m_dumpId, timestamp, false, sb.toString());
     }
 
     /**
      * Get the actual file contents for a dump of state reachable by
      * this thread. Can be called unsafely or safely.
      */
     public ExecutorContext getDumpContents() {
         final ExecutorContext context = new ExecutorContext();
         context.siteId = siteId;
 
         if (dtxnConn instanceof SimpleDtxnConnection)
             ((SimpleDtxnConnection) dtxnConn).getDumpContents(context);
 
         return context;
     }
 }
