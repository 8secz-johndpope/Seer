 /*
  * Copyright 2007-2009 Sun Microsystems, Inc.
  *
  * This file is part of Project Darkstar Server.
  *
  * Project Darkstar Server is free software: you can redistribute it
  * and/or modify it under the terms of the GNU General Public License
  * version 2 as published by the Free Software Foundation and
  * distributed hereunder to you.
  *
  * Project Darkstar Server is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package com.sun.sgs.impl.service.nodemap.affinity.dlpa;
 
 import com.sun.sgs.auth.Identity;
 import com.sun.sgs.impl.kernel.StandardProperties;
 import com.sun.sgs.impl.service.nodemap.affinity.AbstractLPA;
 import com.sun.sgs.impl.service.nodemap.affinity.AffinityGroup;
 import com.sun.sgs.impl.service.nodemap.affinity.graph.LabelVertex;
 import com.sun.sgs.impl.service.nodemap.affinity.dlpa.graph.DLPAGraphBuilder;
 import com.sun.sgs.impl.sharedutil.PropertiesWrapper;
 import com.sun.sgs.impl.util.Exporter;
 import com.sun.sgs.impl.util.IoRunnable;
 import java.io.IOException;
 import java.net.InetAddress;
 import java.rmi.registry.LocateRegistry;
 import java.rmi.registry.Registry;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Properties;
 import java.util.concurrent.Callable;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.concurrent.atomic.AtomicLong;
 import java.util.logging.Level;
 
 /**
  * A distributed implementation of the algorithm presented in
  * "Near linear time algorithm to detect community structures in large-scale
  * networks" by U.N. Raghavan, R. Albert and S. Kumara 2007.
  * <p>
  * This is the portion of code that is on each application node.
  */
 public class LabelPropagation extends AbstractLPA implements LPAClient {
     /** The property name for the server host. */
     static final String SERVER_HOST_PROPERTY = PROP_NAME + ".server.host";
 
     /** The server : our master. */
     private final LPAServer server;
 
     /** Our graph builder, which provides us with our input. */
     private final DLPAGraphBuilder builder;
 
     /** A map of cached nodeId->LPAClient.  The contents of this map
      * can change.
      */
     private final Map<Long, LPAClient> nodeProxies = 
             new ConcurrentHashMap<Long, LPAClient>();
 
     /** Our exporter. */
     private final Exporter<LPAClient> clientExporter;
 
     /** Lock to ensure we aren't modifying the vertices list at the same
      * time we're processing an asynchronous call from another node.
      */
     private final Object verticesLock = new Object();
 
     /** The map of conflicts in the system, nodeId->objId, count.
      * Updates are multi-threaded.
      */
     private final ConcurrentMap<Long, ConcurrentMap<Object, AtomicLong>>
         nodeConflictMap =
             new ConcurrentHashMap<Long, ConcurrentMap<Object, AtomicLong>>();
 
     /** Map of identity -> label and count
      * This sums all uses of that identity on other nodes. The count takes
      * weights for object uses into account.
      * Updates are currently single threaded, node by node.
      */
     private final ConcurrentMap<Identity, Map<Integer, Long>>
         remoteLabelMap =
             new ConcurrentHashMap<Identity, Map<Integer, Long>>();
 
 
     /** States of this instance, ensuring that calls from the server are
      * idempotent.
      */
     private enum State {
         // Preparing for an algorithm run
         PREPARING,
         // In the midst of an iteration
         IN_ITERATION,
         // Gathering up the final groups
         GATHERING_GROUPS,
         // Completed gathering groups
         GATHERED_GROUPS,
         // Idle (none of the above)
         IDLE
     }
 
     /** The current state of this instance. */
     private State state = State.IDLE;
 
     /** The current algorithm run number, used to ensure we're returning
      * values for the correct algorithm run.
      */
     private volatile long runNumber = -1;
 
     /** The current iteration being run, used to detect multiple calls
      * for an iteration.
      */
     private volatile int iteration = -1;
 
     /** Synchronization for state, runNumber, and iteration. */
     private final Object stateLock = new Object();
 
     /** The groups collected in the last run. */
     private Collection<AffinityGroup> groups;
 
     /** The time (in milliseconds) to wait between retries for IO
      * operations. */
     private final int retryWaitTime;
 
     /** The maximum number of retry attempts for IO operations. */
     private final int maxIoAttempts;
     /**
      *
      * Constructs a new instance of the label propagation algorithm.
      * @param builder the graph producer
      * @param nodeId the local node ID
      * @param properties the properties for configuring this service
      *
      * @throws IllegalArgumentException if {@code numThreads} is
      *       less than {@code 1}
      * @throws Exception if any other error occurs
      */
     public LabelPropagation(DLPAGraphBuilder builder, long nodeId,
                             Properties properties)
         throws Exception
     {
         super(nodeId, properties);
         this.builder = builder;
         PropertiesWrapper wrappedProps = new PropertiesWrapper(properties);
         // Retry behavior
         retryWaitTime = wrappedProps.getIntProperty(
                 LabelPropagationServer.IO_TASK_WAIT_TIME_PROPERTY,
                 LabelPropagationServer.DEFAULT_RETRY_WAIT_TIME, 0,
                 Integer.MAX_VALUE);
         maxIoAttempts = wrappedProps.getIntProperty(
                 LabelPropagationServer.IO_TASK_RETRIES_PROPERTY,
                 LabelPropagationServer.DEFAULT_MAX_IO_ATTEMPTS, 0,
                 Integer.MAX_VALUE);
         
         String host = wrappedProps.getProperty(SERVER_HOST_PROPERTY,
 			wrappedProps.getProperty(
 			    StandardProperties.SERVER_HOST));
         if (host == null) {
             // None specified, use local host
             host = InetAddress.getLocalHost().getHostName();
         }
         int port = wrappedProps.getIntProperty(
                 LabelPropagationServer.SERVER_PORT_PROPERTY,
                 LabelPropagationServer.DEFAULT_SERVER_PORT, 0, 65535);
         // Look up our server
         Registry registry = LocateRegistry.getRegistry(host, port);
         server = (LPAServer) registry.lookup(
                          LabelPropagationServer.SERVER_EXPORT_NAME);
         // Export ourselves using an anonymous port, and register with server
         // Another option is to have the LPAServer collect and exchange
         // all cross node edge info, and the remote labels at the start
         // of each iteration.  That would be helpful, because then the
         // server knows when all preliminary information has been exchanged.
         clientExporter = new Exporter<LPAClient>(LPAClient.class);
         int exportPort = clientExporter.export(this, 0);
         server.register(nodeId, clientExporter.getProxy());
         if (logger.isLoggable(Level.CONFIG)) {
             logger.log(Level.CONFIG, "Created label propagation node on {0} " +
                     " using server on {1}:{2}, and exported self on {3}",
                     localNodeId, host, port, exportPort);
         }
     }
     
     // --- implement LPAClient -- //
     
     /** {@inheritDoc} */
     public Collection<AffinityGroup> getAffinityGroups(long runNumber,
                                                        boolean done)
         throws IOException
     {
         synchronized (stateLock) {
             if (this.runNumber != runNumber) {
                 throw new IllegalArgumentException(
                     "bad run number " + runNumber +
                     ", expected " + this.runNumber);
             }
             if (done) {
                 // If done is true, we will be initializing the graph for
                 // our next iteration as we gather the final group data,
                 // making it impossible for us to gather the data again.
                 while (state == State.GATHERING_GROUPS) {
                     try {
                         stateLock.wait();
                     } catch (InterruptedException e) {
                         // Do nothing - ignore until state changes
                     }
                 }
                 if (state == State.GATHERED_GROUPS) {
                     // We have collected data for this run already, just return
                     // them.
                     logger.log(Level.FINE,
                             "{0}: returning {1} precalculated groups",
                             localNodeId, groups.size());
                     new HashSet<AffinityGroup>(groups);
                 }
                 state = State.GATHERING_GROUPS;
             }
         }
 
         // Log the final graph before calling gatherGroups, which is
         // also responsible for reinitializing the vertices for the next run
         // if done is true
         if (done && logger.isLoggable(Level.FINER)) {
             logger.log(Level.FINER, "{0}: FINAL GRAPH IS {1}",
                                      localNodeId, graph);
         }
         groups = gatherGroups(vertices, done);
 
         if (done) {
             // Clear our maps that are set up as the first step of an
             // algorithm run.  Doing this here ensures we catch protocol
             // errors, where we attempt to run iterations before we've
             // prepared for a run.
             nodeConflictMap.clear();
             vertices = null;
 
             synchronized (stateLock) {
                 state = State.GATHERED_GROUPS;
                 stateLock.notifyAll();
             }
         }
         logger.log(Level.FINEST, "{0}: returning {1} groups",
                    localNodeId, groups.size());
         return new HashSet<AffinityGroup>(groups);
     }
 
     /** 
      * {@inheritDoc}
      * <p>
      * Asynchronously prepare ourselves for a run.
      */
     public void prepareAlgorithm(long runNumber) throws IOException {
         PrepareRun pr = new PrepareRun(runNumber);
         String name = "PrepareAlgorithm-" + runNumber;
         new Thread(pr, name).start();
     }
 
     /**
      * Private class for running asynchronous method.
      */
     private class PrepareRun implements Runnable {
         final long run;
         PrepareRun(long run) {
             this.run = run;
         }
         public void run() {
             prepareAlgorithmInternal(run);
         }
 
     }
 
     /**
      * Prepare for an algorithm run.  If we are unable to contact the server
      * to report we are finished, log the failure.
      * <p>
      * Each pair of nodes needs to exchange conflict information to ensure
      * that both pairs know the complete set for both (e.g. if node 1 has a
      * data conflict on obj1 with node 2, it lets node 2 know.
      * <p>
      * It might be better to just let the server ask each vertex for its
      * information and merge it there.
      *
      * @param runNumber
      */
     private void prepareAlgorithmInternal(long runNumber) {
         synchronized (stateLock) {
             if (runNumber == this.runNumber) {
                 // We assume this happened if the server called us twice
                 // due to IO retries.
                 return;
             }
             while (state == State.PREPARING) {
                 try {
                     stateLock.wait();
                 } catch (InterruptedException e) {
                     // Do nothing - ignore until state changes
                 }
             }
             if (this.runNumber > runNumber) {
                 // Things are confused;  we should have already performed
                 // the run.  Do nothing.
                 logger.log(Level.FINE,
                             "{0}: bad run number {1}, " +
                             " we are on run {2}.  Returning.",
                             localNodeId, runNumber, this.runNumber);
                 return;
             }
             this.runNumber = runNumber;
             iteration = -1;
             state = State.PREPARING;
         }
 
         initializeLPARun(builder);
 
         // If we cannot reach a proxy, we invalidate the run.
         boolean failed = false;
         // Now, go through the new map, and tell each vertex about the
         // edges we might have in common.
         assert (nodeConflictMap != null);
         for (Map.Entry<Long, ConcurrentMap<Object, AtomicLong>> entry :
              nodeConflictMap.entrySet())
         {
             Long nodeId = entry.getKey();
             final LPAClient proxy = getProxy(nodeId);
 
             if (proxy != null) {
                 logger.log(Level.FINEST, "{0}: exchanging edges with {1}",
                            localNodeId, nodeId);
                 final Map<Object, AtomicLong> map = entry.getValue();
                 assert (map != null);
 
                 boolean ok = LabelPropagationServer.runIoTask(new IoRunnable() {
                     public void run() throws IOException {
                         proxy.crossNodeEdges(new HashSet<Object>(map.keySet()),
                                          localNodeId);
                     } }, nodeId, maxIoAttempts, retryWaitTime);
                 if (!ok) {
                     failed = true;
                 }
             } else {
                 logger.log(Level.FINE, "{0}: could not exchange edges with {1}",
                            localNodeId, nodeId);
                 failed = true;
                 break;
             }
         }
 
         // Tell the server we're ready for the iterations to begin
         final boolean runFailed = failed;
         boolean ok = LabelPropagationServer.runIoTask(new IoRunnable() {
             public void run() throws IOException {
                 server.readyToBegin(localNodeId, runFailed);
             } }, -1, maxIoAttempts, retryWaitTime);
         if (!ok) {
             failed = true;
             logger.log(Level.WARNING, "{0}: could not contact server",
                                       localNodeId);
         }
         synchronized (stateLock) {
             state = State.IDLE;
             stateLock.notifyAll();
         }
     }
 
     /** {@inheritDoc} */
     public void crossNodeEdges(Collection<Object> objIds, long nodeId)
         throws IOException
     {
         if (objIds == null) {
             // This is unexpected;  the other node should have returned
             // an empty collection.
             logger.log(Level.FINE, "unexpected null objIds");
             return;
         }
         ConcurrentMap<Object, AtomicLong> conflicts =
                 nodeConflictMap.get(nodeId);
         if (conflicts == null) {
             ConcurrentMap<Object, AtomicLong> newConf =
                     new ConcurrentHashMap<Object, AtomicLong>();
             conflicts = nodeConflictMap.putIfAbsent(nodeId, newConf);
             if (conflicts == null) {
                 conflicts = newConf;
             }
         }
 
         for (Object objId : objIds) {
             // Just the original value or 1
             // If we start using the number of conflicts, this might change.
             conflicts.putIfAbsent(objId, new AtomicLong(1));
         }
     }
 
     /** {@inheritDoc} */
     public void removeNode(long nodeId) throws IOException {
         removeNodeInternal(nodeId);
     }
 
     /**
      * Remove a failed node, telling dependent objects to do the same.
      * This method is not called remotely.
      * @param nodeId the ID of the failed node.
      */
     private void removeNodeInternal(long nodeId) {
         logger.log(Level.FINEST, "{0}: Removing node {1} from LPA",
                    localNodeId, nodeId);
         nodeProxies.remove(nodeId);
         nodeConflictMap.remove(nodeId);
         builder.removeNode(nodeId);
     }
 
     /**
      * {@inheritDoc}
      * <p>
      * This method is run asynchronously.
      */
     public void startIteration(int iteration) throws IOException {
         IterationRun ir = new IterationRun(iteration);
         String name = "StartIteration-" + iteration;
         new Thread(ir, name).start();
     }
 
     /**
      * Private class for running asynchronous method.
      */
     private class IterationRun implements Runnable {
         final int iter;
         IterationRun(int iter) {
             this.iter = iter;
         }
         public void run() {
             startIterationInteral(iter);
         }
     }
 
     /**
      * Run an iteration of the algorithm.
      * <p>
      * If we are unable to contact the server to report we are finished,
      * log the failure.
      * @param iteration the iteration to run
      */
     private void startIterationInteral(final int iteration) {
         // We should have been prepared by now.
         assert (vertices != null);
        
         // Block any additional threads entering this iteration
         synchronized (stateLock) {
             if (this.iteration == iteration) {
                 // We have been called more than once by the server. Assume this
                 // is due to IO retries, so no action is needed.
                 return;
             }
             while (state == State.IN_ITERATION) {
                 try {
                     stateLock.wait();
                 } catch (InterruptedException e) {
                     // Do nothing - ignore until state changes
                 }
             }
             if (this.iteration > iteration) {
                 // Things are confused;  we should have already performed
                 // the iteration.  Do nothing.
                 logger.log(Level.FINE,
                             "{0}: bad iteration number {1}, " +
                             " we are on iteration {2}.  Returning.",
                             localNodeId, iteration, this.iteration);
                 return;
             }
             // Record the current iteration so we can use it for error checks.
             this.iteration = iteration;
             state = State.IN_ITERATION;
         }
 
         if (logger.isLoggable(Level.FINEST)) {
             logger.log(Level.FINEST, "{0}: GRAPH at iteration {1} is {2}",
                                       localNodeId, iteration, graph);
         }
 
         // Gather the remote labels from each node.
         boolean failed = updateRemoteLabels();
 
         // We include the current label when calculating the most frequent
         // label, so no labels changing indicates the algorithm has converged
         // and we can stop.
         boolean changed = false;
 
         if (!failed) {
             // Arrange the vertices in a random order for each iteration.
             // For the first iteration, we just use the iterator ordering.
             if (iteration > 1) {
                 synchronized (verticesLock) {
                     Collections.shuffle(vertices);
                 }
             }
 
             // For each of the vertices, set the label to the label with the
             // highest frequency of its neighbors.
             if (numThreads > 1) {
                 final AtomicBoolean abool = new AtomicBoolean(false);
                 List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
                 for (final LabelVertex vertex : vertices) {
                     tasks.add(new Callable<Void>() {
                         public Void call() {
                             abool.set(setMostFrequentLabel(vertex, true) ||
                                       abool.get());
                             return null;
                         }
                     });
                 }
 
                 // Invoke all the tasks, waiting for them to be done.
                 // We don't look at the returned futures.
                 try {
                     executor.invokeAll(tasks);
                 } catch (InterruptedException ie) {
                     failed = true;
                     logger.logThrow(Level.INFO, ie,
                                     " during iteration " + iteration);
                 }
                 changed = abool.get();
 
             } else {
                 for (LabelVertex vertex : vertices) {
                     changed = setMostFrequentLabel(vertex, true) || changed;
                 }
             }
 
             if (logger.isLoggable(Level.FINEST)) {
                 // Log the affinity groups so far:
                 Collection<AffinityGroup> intermediateGroups =
                         gatherGroups(vertices, false);
                 for (AffinityGroup group : intermediateGroups) {
                     StringBuffer logSB = new StringBuffer();
                     for (Identity id : group.getIdentities()) {
                         logSB.append(id + " ");
                     }
                     logger.log(Level.FINEST,
                                "{0}: Intermediate group {1} , members: {2}",
                                localNodeId, group, logSB.toString());
                 }
             }
         }
         // Tell the server we've finished this iteration
         final boolean converged = !changed;
         final boolean runFailed = failed;
         boolean ok = LabelPropagationServer.runIoTask(new IoRunnable() {
             public void run() throws IOException {
                 server.finishedIteration(localNodeId, converged,
                                          runFailed, iteration);
             } }, -1, maxIoAttempts, retryWaitTime);
         if (!ok) {
             failed = true;
             logger.log(Level.WARNING, "{0}: could not contact server",
                                       localNodeId);
         }
 
         synchronized (stateLock) {
             state = State.IDLE;
             stateLock.notifyAll();
         }
     }
 
     /** {@inheritDoc} */
     public Map<Object, Map<Integer, List<Long>>> getRemoteLabels(
                 Collection<Object> objIds)
             throws IOException
     {
         Map<Object, Map<Integer, List<Long>>> retMap =
                 new HashMap<Object, Map<Integer, List<Long>>>();
         if (objIds == null) {
             // This is unexpected;  the other node should have passed in an
             // empty collection
             logger.log(Level.FINE, "unexpected null objIds");
             return retMap;
         }
         
         ConcurrentMap<Object, ConcurrentMap<Identity, AtomicLong>> objectMap =
                 builder.getObjectUseMap();
         assert (objectMap != null);
 
         synchronized (verticesLock) {
             for (Object obj : objIds) {
                 // look up the set of identities
                 ConcurrentMap<Identity, AtomicLong> idents = objectMap.get(obj);
                 Map<Integer, List<Long>> labelWeightMap =
                         new HashMap<Integer, List<Long>>();
                 // If idents is null, the identity is no longer used, probably
                 // because the graph was pruned (we are using a live object
                 // use map).
                 if (idents != null) {
                     for (Map.Entry<Identity, AtomicLong> entry :
                         idents.entrySet())
                     {
                         // Find the label associated with the identity in
                         // the graph.
                         // We do this by creating vid, a template of the
                         // LabelVertex, and then finding the actual graph
                         // vertex with that identity.  The current label can
                         // be found in the actual graph vertex.
                         LabelVertex vid = new LabelVertex(entry.getKey());
                         int index = vertices.indexOf(vid);
                         if (index != -1) {
                             // If the vid wasn't found in the vertices list,
                             // it is a new identity since the vertices were
                             // captured at the start of this algorithm run,
                             // and we just ignore the label.
                             // Otherwise, add the label to set of labels for
                             // this identity.
                             Integer label = vertices.get(index).getLabel();
 
                             List<Long> weightList = labelWeightMap.get(label);
                             if (weightList == null) {
                                 weightList = new ArrayList<Long>();
                                 labelWeightMap.put(label, weightList);
                             }
                             weightList.add(entry.getValue().get());
                         }
                     }
                 }
                 retMap.put(obj, labelWeightMap);
             }
         }
         return retMap;
     }
     
     /** {@inheritDoc} */
     public void shutdown() {
         clientExporter.unexport();
         if (executor != null) {
             executor.shutdown();
         }
     }
 
     /**
      * {@inheritDoc}
      * <p>
      * Initialize our vertex conflicts.  This needs to happen before
      * we send our vertex conflict information to other nodes in
      * response to an prepareAlgorithm call from the server, and before
      * any crossNodeEdges calls.
      */
     protected void doOtherInitialization() {
         // Get conflict information from the graph builder.
         nodeConflictMap.putAll(builder.getConflictMap());
         logger.log(Level.FINEST,
                 "{0}: initialized node conflict map", localNodeId);
         printNodeConflictMap();
     }
 
     /** {@inheritDoc} */
     protected void doOtherNeighbors(LabelVertex vertex,
                                     Map<Integer, Long> labelMap,
                                     StringBuffer logSB)
     {
         // Account for the remote neighbors:  look up this LabelVertex in
         // the remoteNeighborMap
         Map<Integer, Long> remoteMap =
                 remoteLabelMap.get(vertex.getIdentity());
         if (remoteMap != null) {
             // The check above is just so I can continue to test in single
             // node mode
             for (Map.Entry<Integer, Long> entry : remoteMap.entrySet()) {
                 Integer label = entry.getKey();
                 if (logger.isLoggable(Level.FINEST)) {
                     logSB.append("RLabel:" + label +
                                  "(" + entry.getValue() + ") ");
                 }
                 Long value = labelMap.containsKey(label) ?
                                 labelMap.get(label) : 0;
                 value += entry.getValue();
                 labelMap.put(label, value);
             }
         }
     }
     
     /**
      * Exchanges information with other nodes in the system to fill in the
      * remoteLabelMap.
      * @return {@code true} if a problem occurred
      */
     private boolean updateRemoteLabels() {
         // reinitialize the remote label map
         remoteLabelMap.clear();
         ConcurrentMap<Object, ConcurrentMap<Identity, AtomicLong>> objectMap =
                 builder.getObjectUseMap();
         assert (objectMap != null);
         
         boolean failed = false;
 
         // Now, go through the new map, asking for its labels
         assert (nodeConflictMap != null);
         for (Map.Entry<Long, ConcurrentMap<Object, AtomicLong>> entry :
              nodeConflictMap.entrySet())
         {
             Long nodeId = entry.getKey();
             LPAClient proxy = getProxy(nodeId);
             if (proxy == null) {
                 logger.log(Level.FINE,
                           "{0}: could not exchange edges with {1}",
                           localNodeId, nodeId);
                 failed = true;
                 break;
             }
 
             // Tell the other vertex about the conflicts we know of.
             Map<Object, AtomicLong> map = entry.getValue();
             assert (map != null);
             logger.log(Level.FINEST, "{0}: exchanging labels with {1}",
                        localNodeId, nodeId);
             Map<Object, Map<Integer, List<Long>>> labels = null;
             GetRemoteLabelsRunnable task = 
                 new GetRemoteLabelsRunnable(proxy,
                                             new HashSet<Object>(map.keySet()));
             boolean ok = LabelPropagationServer.runIoTask(task, nodeId,
                                                 maxIoAttempts, retryWaitTime);
             if (ok) {
                 labels = task.getLabels();
             } else {
                 failed = true;
                 logger.log(Level.WARNING,
                         "{0}: could not contact node {1}", localNodeId, nodeId);
             }
 
             if (labels == null) {
                 // This is unexpected; the other node should have returned
                 // an empty collection.  Log it, but act as if it
                 // was an empty collection.
                 logger.log(Level.FINE, "unexpected null labels");
                 continue;
             }
 
             // Process the returned labels
             // For each object returned...
             for (Map.Entry<Object, Map<Integer, List<Long>>> remoteEntry :
                  labels.entrySet())
             {
                 Object remoteObject = remoteEntry.getKey();
                 // ... look up the local node use of the object.
                 ConcurrentMap<Identity, AtomicLong> objUse =
                         objectMap.get(remoteObject);
                 if (objUse == null) {
                     // no local uses of this object
                     continue;
                 }
                 Map<Integer, List<Long>> remoteLabels = remoteEntry.getValue();
                 // Compare each local use's weight with each remote use of
                 // the weight, and fill in our remoteLabelMap.
                 for (Map.Entry<Identity, AtomicLong> objUseId :
                     objUse.entrySet())
                 {
                     Identity ident = objUseId.getKey();
                     long localCount = objUseId.getValue().get();
                     Map<Integer, Long> labelCount = remoteLabelMap.get(ident);
                     if (labelCount == null) {
                         // Effective Java item 69, faster to use get before
                         // putIfAbsent
                         Map<Integer, Long> newMap =
                                 new ConcurrentHashMap<Integer, Long>();
                         labelCount = remoteLabelMap.putIfAbsent(ident, newMap);
                         if (labelCount == null) {
                             labelCount = newMap;
                         }
                     }
                     for (Map.Entry<Integer, List<Long>> rLabelCount :
                         remoteLabels.entrySet())
                     {
                         Integer rlabel = rLabelCount.getKey();
                         List<Long> rcounts = rLabelCount.getValue();
                         Long updateCount = labelCount.get(rlabel);
                         if (updateCount == null) {
                             updateCount = Long.valueOf(0);
                         }
                         for (Long rc : rcounts) {
                             updateCount += Math.min(localCount, rc.longValue());
                         }
                         labelCount.put(rlabel, updateCount);
                         logger.log(Level.FINEST,
                                 "{0}: label {1}, updateCount {2}, " +
                                 "localCount {3}, : ident {4}",
                                 localNodeId, rlabel, updateCount,
                                 localCount, ident);
                     }
                 }
             }
         }
 
         return failed;
     }
 
     /**
      * Runnable which calls another node to get its labels.  This is
      * not an anonymous class because we need to obtain a result.
      */
    private class GetRemoteLabelsRunnable implements IoRunnable {
         private Map<Object, Map<Integer, List<Long>>> labels;
         private final HashSet<Object> objects;
         private final LPAClient proxy;
         GetRemoteLabelsRunnable(LPAClient proxy, HashSet<Object> objects) {
             this.proxy = proxy;
             this.objects = objects;
         }
         public void run() throws IOException {
             labels = proxy.getRemoteLabels(objects);
         }
         public Map<Object, Map<Integer, List<Long>>> getLabels() {
             return labels;
         }
     }
 
     /**
      * Returns the client for the given nodeId, asking the server if necessary.
      * @param nodeId
      * @return
      */
     private LPAClient getProxy(long nodeId) {
         LPAClient proxy = nodeProxies.get(nodeId);
         if (proxy == null) {
             GetProxyRunnable task = new GetProxyRunnable(nodeId);
             boolean ok = LabelPropagationServer.runIoTask(task, -1,
                                             maxIoAttempts, retryWaitTime);
             if (ok) {
                 proxy = task.getProxy();
             } else {
                 logger.log(Level.WARNING, "{0}: could not contact server",
                                           localNodeId);
             }
             if (proxy != null) {
                 nodeProxies.put(nodeId, proxy);
             } else {
                 removeNodeInternal(nodeId);
             }
         }
         return proxy;
     }
 
     /**
      * Runnable which gets a proxy from the server.  This is
      * not an anonymous class because we need to obtain a result.
      */
     private class GetProxyRunnable implements IoRunnable {
         private final long nodeId;
         private LPAClient proxy;
         GetProxyRunnable(long nodeId) {
             this.nodeId = nodeId;
         }
         public void run() throws IOException {
             proxy = server.getLPAClientProxy(nodeId);
         }
         public LPAClient getProxy() {
             return proxy;
         }
     }
 
     // For debugging.
     private void printNodeConflictMap() {
         if (!logger.isLoggable(Level.FINEST)) {
             return;
         }
         for (Map.Entry<Long, ConcurrentMap<Object, AtomicLong>> entry :
              nodeConflictMap.entrySet())
         {
             StringBuilder sb = new StringBuilder();
             sb.append(entry.getKey());
             sb.append(":  ");
             for (Map.Entry<Object, AtomicLong> subEntry :
                  entry.getValue().entrySet())
             {
                 sb.append(subEntry.getKey() + "," + subEntry.getValue() + " ");
             }
             logger.log(Level.FINEST, "{0}: nodeConflictMap: {1}",
                     localNodeId, sb.toString());
         }
     }
 
     // For testing
     /**
      * Returns a copy of the node conflict map.
      * @return a copy of the node conflict map.
      */
     public ConcurrentMap<Long, ConcurrentMap<Object, AtomicLong>>
             getNodeConflictMap()
     {
         ConcurrentMap<Long, ConcurrentMap<Object, AtomicLong>> copy =
             new ConcurrentHashMap<Long, ConcurrentMap<Object, AtomicLong>>(
                                                             nodeConflictMap);
         return copy;
     }
 
     /**
      * Returns a copy of the remote label map.
      * @return a copy of the remote label map
      */
     public ConcurrentMap<Identity, Map<Integer, Long>> getRemoteLabelMap() {
         ConcurrentMap<Identity, Map<Integer, Long>> copy =
             new ConcurrentHashMap<Identity, Map<Integer, Long>>(remoteLabelMap);
         return copy;
     }
 }
