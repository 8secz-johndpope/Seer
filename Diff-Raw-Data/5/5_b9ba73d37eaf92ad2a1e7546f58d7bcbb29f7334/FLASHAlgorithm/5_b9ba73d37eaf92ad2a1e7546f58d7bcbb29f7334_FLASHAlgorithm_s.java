 /*
  * ARX: Efficient, Stable and Optimal Data Anonymization
  * Copyright (C) 2012 - 2013 Florian Kohlmayer, Fabian Prasser
  * 
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  * 
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  * 
  * You should have received a copy of the GNU General Public License
  * along with this program. If not, see <http://www.gnu.org/licenses/>.
  */
 
 package org.deidentifier.arx.algorithm;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 import java.util.PriorityQueue;
 import java.util.Stack;
 
 import org.deidentifier.arx.criteria.KAnonymity;
 import org.deidentifier.arx.framework.check.INodeChecker;
 import org.deidentifier.arx.framework.check.history.History;
 import org.deidentifier.arx.framework.check.history.History.PruningStrategy;
 import org.deidentifier.arx.framework.lattice.Lattice;
 import org.deidentifier.arx.framework.lattice.Node;
 
 /**
  * This class provides a reference implementation of the ARX algorithm.
  * 
  * @author Prasser, Kohlmayer
  */
 public class FLASHAlgorithm extends AbstractAlgorithm {
 
     /** Potential traverse types */
     private static enum TraverseType {
         FIRST_PHASE_ONLY,
         FIRST_AND_SECOND_PHASE,
         SECOND_PHASE_ONLY
     }
 
     /** The stack. */
     private final Stack<Node>         stack;
 
     /** The heap. */
     private final PriorityQueue<Node> pqueue;
 
     /** The current path. */
     private final ArrayList<Node>     path;
 
     /** Are the pointers for a node with id 'index' already sorted?. */
     private final boolean[]           sorted;
 
     /** The strategy. */
     private final FLASHStrategy       strategy;
 
     /** Traverse type of 2PF */
     private TraverseType              traverseType;
     
     /** The history*/
     private History                   history;
 
     /**
      * Creates a new instance of the ARX algorithm.
      * 
      * @param lattice
      *            The lattice
      * @param history
      *            The history
      * @param checker
      *            The checker
      * @param metric
      *            The metric
      */
     public FLASHAlgorithm(final Lattice lattice,
                           final INodeChecker checker,
                           final FLASHStrategy metric) {
         super(lattice, checker);
         strategy = metric;
         pqueue = new PriorityQueue<Node>(11, strategy);
         sorted = new boolean[lattice.getSize()];
         path = new ArrayList<Node>();
         stack = new Stack<Node>();
         this.history = checker.getHistory();
 
         // NOTE: If we assume practical monotonicity then we assume
         // monotonicity for both criterion AND metric!
         // NOTE: We assume monotonicity for criterion with 0% suppression
         if ((checker.getConfiguration().getAbsoluteMaxOutliers() == 0) ||
             (checker.getConfiguration().isCriterionMonotonic() && checker.getMetric()
                                                                          .isMonotonic()) ||
             (checker.getConfiguration().isPracticalMonotonicity())) {
             traverseType = TraverseType.FIRST_PHASE_ONLY;
             history.setPruningStrategy(PruningStrategy.ANONYMOUS);
         } else {
             if (checker.getConfiguration().getMinimalGroupSize() != Integer.MAX_VALUE) {
                 traverseType = TraverseType.FIRST_AND_SECOND_PHASE;
                 history.setPruningStrategy(PruningStrategy.K_ANONYMOUS);
             } else {
                 traverseType = TraverseType.SECOND_PHASE_ONLY;
                 history.setPruningStrategy(PruningStrategy.CHECKED);
             }
         }
     }
 
     /**
      * Check a node during the first phase
      * 
      * @param node
      */
     protected void checkNode1(final Node node) {
         
         checker.check(node);
 
         // NOTE: SECOND_PHASE_ONLY not needed, as in this case
         // checkNode1 would never been called
         switch (traverseType) { 
             case FIRST_PHASE_ONLY:
                 lattice.tagAnonymous(node, node.isAnonymous());
                 break;
             case FIRST_AND_SECOND_PHASE:
                 lattice.tagKAnonymous(node, node.isKAnonymous());
                 break;
             default:
                 throw new RuntimeException("Not implemented!");
         }
     }
 
     /**
      * Check a node during the second phase
      * 
      * @param node
      */
     protected void checkNode2(final Node node) {
         if (!node.isChecked()) {
 
             // TODO: Rethink var1 & var2
             final boolean var1 = !checker.getMetric().isMonotonic() &&
                                  checker.getConfiguration()
                                         .isCriterionMonotonic();
 
             final boolean var2 = !checker.getMetric().isMonotonic() &&
                                  !checker.getConfiguration()
                                          .isCriterionMonotonic() &&
                                  checker.getConfiguration()
                                         .isPracticalMonotonicity();
 
             // NOTE: Might return non-anonymous result as optimum, when
             // 1. the criterion is not monotonic, and
             // 2. practical monotonicity is assumed, and
             // 3. the metric is non-monotonic BUT independent.
             // -> Such a metric does currently not exist
             if (checker.getMetric().isIndependent() && (var1 || var2)) {
                 checker.getMetric().evaluate(node, null);
             } else {
                 checker.check(node);
             }
 
         }
 
         // In case metric is monotone it can be tagged if the node is anonymous
         if (checker.getMetric().isMonotonic() && node.isAnonymous()) { 
             lattice.tagAnonymous(node, node.isAnonymous());
         } else {
             node.setTagged();
             lattice.untaggedCount[node.getLevel()]--;
         }
     }
 
     /**
      * Checks a path binary.
      * 
      * @param path
      *            The path
      */
     private final Node checkPathBinary(final List<Node> path) {
         int low = 0;
         int high = path.size() - 1;
         Node lastAnonymousNode = null;
 
         while (low <= high) {
 
             final int mid = (low + high) >>> 1;
             final Node node = path.get(mid);
 
             if (!node.isTagged()) { 
                 checkNode1(node);
                 if (!isNodeAnonymous(node)) { // put only non-anonymous nodes in
                                               // pqueue, as potetnially a
                                               // snaphsot is avaliable
                     for (final Node up : node.getSuccessors()) {
                         if (!up.isTagged()) { // only unknown nodes are
                                               // nesessary
                             pqueue.add(up);
                         }
                     }
                 }
             }
 
             if (isNodeAnonymous(node)) {
                 lastAnonymousNode = node;
                 high = mid - 1;
             } else {
                 low = mid + 1;
             }
         }
         return lastAnonymousNode;
     }
 
     /**
      * Checks a path sequentially.
      * 
      * @param path
      *            The path
      */
     private final void checkPathExhaustive(final List<Node> path) {
 
         for (final Node node : path) {
             if (!node.isTagged()) { 
                 checkNode2(node);
                 // Put all untagged nodes on the stack
                 for (final Node up : node.getSuccessors()) {
                     if (!up.isTagged()) {
                         stack.push(up);
                     }
                 }
             }
         }
     }
 
     /**
      * Greedy find path.
      * 
      * @param current
      *            The current
      * @return the list
      */
     private final List<Node> findPath(Node current) {
         path.clear();
         path.add(current);
         boolean found = true;
         while (found) {
             found = false;
             this.sort(current);
             for (final Node candidate : current.getSuccessors()) {
                 if (!candidate.isTagged()) {
                     current = candidate;
                     path.add(candidate);
                     found = true;
                     break;
                 }
             }
         }
         return path;
     }
 
     /**
      * Is the node anonymous according to the first run of the algorithm
      * 
      * @param node
      * @param traverseType
      * @return
      */
     private boolean isNodeAnonymous(final Node node) {
 
     	// SECOND_PHASE_ONLY not needed, as isNodeAnonymous is only used during the first phase
         switch (traverseType) { 
         case FIRST_PHASE_ONLY:
             return node.isAnonymous();
         case FIRST_AND_SECOND_PHASE:
             return node.isKAnonymous();
         default:
             throw new RuntimeException("Not implemented!");
         }
     }
 
     /**
      * Sorts a level.
      * 
      * @param level
      *            The level
      * @return the node[]
      */
 
     private final Node[] sort(final int level) {
         final Node[] result = new Node[lattice.getUntaggedCount(level)];
         if (result.length == 0) { return result; }
         int index = 0;
         final Node[] nlevel = lattice.getLevels()[level];
         for (final Node n : nlevel) {
             if (!n.isTagged()) {
                 result[index++] = n;
             }
         }
         this.sort(result);
         return result;
     }
 
     /**
      * Sorts upwards pointers of a node.
      * 
      * @param current
      *            The current
      */
     private final void sort(final Node current) {
         if (!sorted[current.id]) {
             this.sort(current.getSuccessors());
             sorted[current.id] = true;
         }
     }
 
     /**
      * Sorts a node array.
      * 
      * @param array
      *            The array
      */
     protected final void sort(final Node[] array) {
         Arrays.sort(array, strategy);
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see org.deidentifier.ARX.algorithm.AbstractAlgorithm#traverse()
      */
     @Override
     public void traverse() {
         pqueue.clear();
         stack.clear();
 
         // check first node
         for (final Node[] level : lattice.getLevels()) {
             if (level.length != 0) {
                 if (level.length == 1) {
                     checker.check(level[0]);
                     break;
                 } else {
                     throw new RuntimeException("Multiple bottom nodes!");
                 }
             }
         }
 
         // For each node
         final int length = lattice.getLevels().length;
         for (int i = 0; i < length; i++) {
             Node[] level;
             level = this.sort(i);
             for (final Node node : level) {
                 if (!node.isTagged()) { 
                     pqueue.add(node);
                     while (!pqueue.isEmpty()) {
                         Node head = pqueue.poll();
                         // if anonymity is unknown
                         if (!head.isTagged()) {
                         	// If first phase is needed
                             if (traverseType == TraverseType.FIRST_PHASE_ONLY ||
                                 traverseType == TraverseType.FIRST_AND_SECOND_PHASE) { 
                                 findPath(head);
                                 head = checkPathBinary(path);
                             }
 
                             // if second phase needed, process path
                             if (head != null &&
                                 (traverseType == TraverseType.FIRST_AND_SECOND_PHASE || traverseType == TraverseType.SECOND_PHASE_ONLY)) {
                                 
                                 // Change strategy
                                 final PruningStrategy pruning = history.getPruningStrategy();
                                 history.setPruningStrategy(PruningStrategy.CHECKED);
                                 
                                // Untag all nodes above first anonymous node if they have already been tagged by first phase;
                                // They will all be tagged again by StackFlash
                                 if (traverseType == TraverseType.FIRST_AND_SECOND_PHASE) { 
                                     lattice.doUnTagUpwards(head);
                                 }
 
                                 stack.push(head);
                                 while (!stack.isEmpty()) {
                                     final Node start = stack.pop();
                                     if (!start.isTagged()) {
                                         findPath(start);
                                         checkPathExhaustive(path);
                                     }
                                 }
 
                                 // Switch back to previous strategy
                                 history.setPruningStrategy(pruning);
                             }
                         }
                     }
                 }
             }
         }
 
     }
 }
