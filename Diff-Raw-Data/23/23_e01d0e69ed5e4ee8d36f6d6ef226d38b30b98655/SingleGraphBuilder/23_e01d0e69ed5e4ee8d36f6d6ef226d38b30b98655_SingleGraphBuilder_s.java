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
 
 package com.sun.sgs.impl.service.nodemap.affinity.single;
 
 import com.sun.sgs.auth.Identity;
 import com.sun.sgs.impl.service.nodemap.affinity.AffinityGroupFinder;
 import
     com.sun.sgs.impl.service.nodemap.affinity.graph.AffinityGraphBuilderStats;
 import com.sun.sgs.impl.service.nodemap.affinity.graph.LabelVertex;
 import com.sun.sgs.impl.service.nodemap.affinity.graph.WeightedEdge;
 import com.sun.sgs.impl.service.nodemap.affinity.graph.AffinityGraphBuilder;
 import com.sun.sgs.impl.sharedutil.LoggerWrapper;
 import com.sun.sgs.impl.sharedutil.PropertiesWrapper;
 import com.sun.sgs.kernel.AccessedObject;
 import com.sun.sgs.management.AffinityGraphBuilderMXBean;
 import com.sun.sgs.profile.AccessedObjectsDetail;
 import com.sun.sgs.profile.ProfileCollector;
 import edu.uci.ics.jung.graph.UndirectedSparseGraph;
 import edu.uci.ics.jung.graph.util.Pair;
 import java.util.ArrayDeque;
 import java.util.Deque;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Properties;
 import java.util.Timer;
 import java.util.TimerTask;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
 import java.util.concurrent.atomic.AtomicLong;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import javax.management.JMException;
 
 /**
  * A minimal graph builder for single node testing.  This is mostly a copy
  * of the WeightedGraphBuilder, with the parts about node conflicts deleted.
  */
 public class SingleGraphBuilder implements AffinityGraphBuilder {
     /** Our property base name. */
     private static final String PROP_NAME =
             "com.sun.sgs.impl.service.nodemap.affinity";
     /** Our logger. */
     protected static final LoggerWrapper logger =
             new LoggerWrapper(Logger.getLogger(PROP_NAME));
 
     /** Map for tracking object-> map of identity-> number accesses
      * (thus we keep track of the number of accesses each identity has made
      * for an object, to aid maintaining weighted edges)
      * Concurrent modifications are protected by locking the affinity graph.
      */
     private final ConcurrentMap<Object, ConcurrentMap<Identity, AtomicLong>>
         objectMap =
            new ConcurrentHashMap<Object, ConcurrentMap<Identity, AtomicLong>>();
 
     /** Our graph of object accesses. */
     private final UndirectedSparseGraph<LabelVertex, WeightedEdge>
         affinityGraph = new UndirectedSparseGraph<LabelVertex, WeightedEdge>();
 
     /** The TimerTask which prunes our data structures over time.  As the data
      * structures above are modified, the pruneTask notes the ways they have
      * changed.  Groups of changes are chunked into periods, each the length
      * of the time snapshot (configured at construction time). We
      * periodically remove the changes made in the earliest snapshot.
      */
     private final PruneTask pruneTask;
 
     /** Our JMX exposed information. */
     private volatile AffinityGraphBuilderStats stats;
 
     /** Our label propagation algorithm. */
     private final SingleLabelPropagation lpa;
     
     /**
      * Creates a weighted graph builder and its JMX MBean.
      * @param col the profile collector
      * @param properties  application properties
      * @param nodeId the local node id
      * @throws Exception if an error occurs
      */
     public SingleGraphBuilder(ProfileCollector col, Properties properties,
                                 long nodeId)
         throws Exception
     {
         this(col, properties, nodeId, true);
     }
 
     /**
      * Creates a weighted graph builder.  The JMX stats object may not be
      * created; this is useful for wrapper objects to break object dependencies.
      * If {@code needStats} is {@code false}, the stats object must be provided
     * with a call to {@code setStats} or a {@code NullPointerException will
      * be thrown on the first call to {@code updateGraph}.
      * 
      * @param col the profile collector
      * @param properties  application properties
      * @param nodeId the local node id
      * @param needStats {@code true} if stats should be constructed
      * @throws Exception if an error occurs
      */
     public SingleGraphBuilder(ProfileCollector col, Properties properties,
                                 long nodeId, boolean needStats)
         throws Exception
     {
         PropertiesWrapper wrappedProps = new PropertiesWrapper(properties);
         long snapshot =
             wrappedProps.getLongProperty(PERIOD_PROPERTY, DEFAULT_PERIOD);
         int periodCount = wrappedProps.getIntProperty(
                 PERIOD_COUNT_PROPERTY, DEFAULT_PERIOD_COUNT,
                 1, Integer.MAX_VALUE);
 
         // Create the LPA algorithm
         lpa = new SingleLabelPropagation(this, col, properties);
 
         if (needStats) {
             // Create our JMX MBean
             stats = new AffinityGraphBuilderStats(col,
                         affinityGraph, periodCount, snapshot);
             try {
                 col.registerMBean(stats,
                                   AffinityGraphBuilderMXBean.MXBEAN_NAME);
             } catch (JMException e) {
                 // Continue on if we couldn't register this bean, although
                 // it's probably a very bad sign
                 logger.logThrow(Level.CONFIG, e, "Could not register MBean");
             }
         }
         
         pruneTask = new PruneTask(periodCount);
         Timer pruneTimer = new Timer("AffinityGraphPruner", true);
         pruneTimer.schedule(pruneTask, snapshot, snapshot);
     }
 
     /**
      * {@inheritDoc}
      * <p>
      * We don't currently use read/write access info.
      */
     public void updateGraph(Identity owner, AccessedObjectsDetail detail) {
         final Object[] ids = new Object[detail.getAccessedObjects().size()];
         int index = 0;
         for (AccessedObject access : detail.getAccessedObjects()) {
             ids[index++] = access.getObjectId();
         }
         updateGraph(owner, ids);
     }
 
     /**
      * Updates the graph with the given identity and object ids.
      * <p>
      * This method is may be called by multiple threads and must protect itself
      * from changes to data structures made by the pruner.
      * @param owner the identity which accessed the objects
      * @param objIds the object ids of objects accessed by the identity
      */
     public void updateGraph(Identity owner, Object[] objIds) {
         long startTime = System.currentTimeMillis();
         stats.updateCountInc();
 
         LabelVertex vowner = new LabelVertex(owner);
 
         // For each object accessed in this task...
         for (Object objId : objIds) {
             // find the identities that have already used this object
             ConcurrentMap<Identity, AtomicLong> idMap = objectMap.get(objId);
             if (idMap == null) {
                 // first time we've seen this object
                 ConcurrentMap<Identity, AtomicLong> newMap =
                         new ConcurrentHashMap<Identity, AtomicLong>();
                 idMap = objectMap.putIfAbsent(objId, newMap);
                 if (idMap == null) {
                     idMap = newMap;
                 }
             }
             AtomicLong value = idMap.get(owner);
             if (value == null) {
                 AtomicLong newVal = new AtomicLong();
                 value = idMap.putIfAbsent(owner, newVal);
                 if (value == null) {
                     value = newVal;
                 }
             }
             long currentVal = value.incrementAndGet();
 
             synchronized (affinityGraph) {
                 affinityGraph.addVertex(vowner);
                 // add or update edges between task owner and identities
                 for (Map.Entry<Identity, AtomicLong> entry : idMap.entrySet()) {
                     Identity ident = entry.getKey();
 
                     // Our folded graph has no self-loops:  only add an
                     // edge if the identity isn't the owner
                     if (!ident.equals(owner)) {
                         LabelVertex vident = new LabelVertex(ident);
                         // Check to see if we already have an edge between
                         // the two vertices.  If so, update its weight.
                         WeightedEdge edge =
                                 affinityGraph.findEdge(vowner, vident);
                         if (edge == null) {
                             WeightedEdge newEdge = new WeightedEdge();
                             affinityGraph.addEdge(newEdge, vowner, vident);
                             // period info
                             pruneTask.incrementEdge(newEdge);
                         } else {
                             AtomicLong otherValue = entry.getValue();
                             if (currentVal <= otherValue.get()) {
                                 edge.incrementWeight();
                                 // period info
                                 pruneTask.incrementEdge(edge);
                             }
                         }
 
                     }
                 }
             }
 
             // period info
             pruneTask.updateObjectAccess(objId, owner);
         }
 
         stats.processingTimeInc(System.currentTimeMillis() - startTime);
     }
     /** {@inheritDoc} */
     public Runnable getPruneTask() {
         return pruneTask;
     }
 
     /** {@inheritDoc} */
     public UndirectedSparseGraph<LabelVertex, WeightedEdge> getAffinityGraph() {
         return affinityGraph;
     }
 
     /** {@inheritDoc} */
     public void shutdown() {
         pruneTask.cancel();
         if (lpa != null) {
             lpa.shutdown();
         }
     }
 
     /** {@inheritDoc} */
     public AffinityGroupFinder getAffinityGroupFinder() {
         return lpa;
     }
 
     /**
      * Sets the JMX MBean for this builder.  This is useful for classes
      * which wrap this object.  Note that stats must be set before the
      * first call to updateGraph, or a {@code NullPointerException} will
      * occur.
      *
      * @param stats our JMX information
      */
     public void setStats(AffinityGraphBuilderStats stats) {
         this.stats = stats;
     }
 
     /**
      * The graph pruner.  It runs periodically, and is the only code
      * that removes edges and vertices from the graph.
      */
     private class PruneTask extends TimerTask {
         // The number of snapshots we retain in our moving window.
         // We fill this window of changes by waiting for count snapshots
         // to occur before we start pruning, ensuring our queues contain
         // count items.  This means we cannot dynamically change the
         // length of the window.
         private final int count;
         // The current snapshot count, used to initially fill up our window.
         private int current = 1;
 
         // The change information we keep for each snapshot.  A new change info
         // object is allocated for each snapshot, and during a snapshot it
         // notes all changes made to this builder's data structures.
         // ObjId -> <Identity -> count times accessed>
         private Map<Object, Map<Identity, Integer>> currentPeriodObject;
         // Edge -> count of times incremented
         private Map<WeightedEdge, Integer> currentPeriodEdgeIncrements;
 
         // A lock to guard all uses of the current period information above.
         // Specifically, we want to ensure that updates to these structures
         // aren't ones currently being pruned.
         private final Object currentPeriodLock = new Object();
 
         // Queues of snapshot information.  As a snapshot time period ends,
         // we add its change info to the back of the appropriate queue.  If
         // we have accumulated enough snapshots in our queues to satisfy our
         // "count" requirement, we also remove the information from the first
         // enqueued info object.
         private final Deque<Map<Object, Map<Identity, Integer>>>
             periodObjectQueue =
                 new ArrayDeque<Map<Object, Map<Identity, Integer>>>();
         private final Deque<Map<WeightedEdge, Integer>>
             periodEdgeIncrementsQueue =
                 new ArrayDeque<Map<WeightedEdge, Integer>>();
 
         /**
          * Creates a PruneTask.
          * @param count the number of full snapshots we wish to
          *              retain as live data
          */
         public PruneTask(int count) {
             this.count = count;
             synchronized (currentPeriodLock) {
                 addPeriodStructures();
             }
         }
 
         /**
          * Performs all processing required when a time period has ended.
          */
         public void run() {
             stats.pruneCountInc();
             // Note: We want to make sure we don't have snapshots that are so
             // short that we cannot do all our pruning within one.
             synchronized (currentPeriodLock) {
                 // Add the data structures for this new period that is just
                 // starting.
                 addPeriodStructures();
                 if (current <= count) {
                     // We're still in our inital time window, and haven't
                     // gathered enough periods yet.
                     current++;
                     return;
                 }
             }
 
             long startTime = System.currentTimeMillis();
 
             // Remove the earliest snasphot.
             Map<Object, Map<Identity, Integer>>
                 periodObject = periodObjectQueue.remove();
             Map<WeightedEdge, Integer>
                 periodEdgeIncrements = periodEdgeIncrementsQueue.remove();
 
             // For each object, remove the added access counts
             for (Map.Entry<Object, Map<Identity, Integer>> entry :
                 periodObject.entrySet())
             {
                 ConcurrentMap<Identity, AtomicLong> idMap =
                         objectMap.get(entry.getKey());
                 for (Map.Entry<Identity, Integer> updateEntry :
                      entry.getValue().entrySet())
                 {
                     Identity updateId = updateEntry.getKey();
                     long updateValue = updateEntry.getValue();
                     AtomicLong val = idMap.get(updateId);
                     // correct? should be using compareAndSet?
                     val.addAndGet(-updateValue);
                     if (val.get() <= 0) {
                         idMap.remove(updateId);
                     }
                 }
                 if (idMap.isEmpty()) {
                     objectMap.remove(entry.getKey());
                 }
             }
 
             synchronized (affinityGraph) {
                 // For each modified edge in the graph, update weights
                 for (Map.Entry<WeightedEdge, Integer> entry :
                      periodEdgeIncrements.entrySet())
                 {
                     WeightedEdge edge = entry.getKey();
                     int weight = entry.getValue();
                     if (edge.getWeight() == weight) {
                         Pair<LabelVertex> endpts =
                                 affinityGraph.getEndpoints(edge);
                         affinityGraph.removeEdge(edge);
                         for (LabelVertex end : endpts) {
                             if (affinityGraph.degree(end) == 0) {
                                 affinityGraph.removeVertex(end);
                             }
                         }
                     } else {
                         edge.addWeight(-weight);
                     }
                 }
             }
 
             stats.processingTimeInc(System.currentTimeMillis() - startTime);
         }
 
         /**
          * Note that an edge's weight has been incremented.
          * @param edge the edge
          */
         void incrementEdge(WeightedEdge edge) {
             synchronized (currentPeriodLock) {
                 int v = currentPeriodEdgeIncrements.containsKey(edge) ?
                          currentPeriodEdgeIncrements.get(edge) : 0;
                 v++;
                 currentPeriodEdgeIncrements.put(edge, v);
             }
         }
 
         /**
          * Note that an object has been accessed.
          * @param objId the object
          * @param owner the accessor
          */
         void updateObjectAccess(Object objId, Identity owner) {
             synchronized (currentPeriodLock) {
                 Map<Identity, Integer> periodIdMap =
                         currentPeriodObject.get(objId);
                 if (periodIdMap == null) {
                     periodIdMap = new HashMap<Identity, Integer>();
                     currentPeriodObject.put(objId, periodIdMap);
                 }
                 int periodValue = periodIdMap.containsKey(owner) ?
                                   periodIdMap.get(owner) : 0;
                 periodValue++;
                 periodIdMap.put(owner, periodValue);
             }
         }
 
         /**
          * Update our queues for this period.
          */
         private void addPeriodStructures() {
             currentPeriodObject =
                     new HashMap<Object, Map<Identity, Integer>>();
             periodObjectQueue.add(currentPeriodObject);
             currentPeriodEdgeIncrements =
                     new HashMap<WeightedEdge, Integer>();
             periodEdgeIncrementsQueue.add(currentPeriodEdgeIncrements);
         }
     }
 }
