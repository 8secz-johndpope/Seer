 /**
  *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
  *  All rights reserved.
  *  Redistribution and use in source and binary forms, with or without
  *  modification, are permitted provided that the following conditions are met:
  *
  *      * Redistributions of source code must retain the above copyright
  *        notice, this list of conditions and the following disclaimer.
  *      * Redistributions in binary form must reproduce the above copyright
  *        notice, this list of conditions and the following disclaimer in the
  *        documentation and/or other materials provided with the distribution.
  *      * Neither the name of the Ecole des Mines de Nantes nor the
  *        names of its contributors may be used to endorse or promote products
  *        derived from this software without specific prior written permission.
  *
  *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
  *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
  *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package solver.propagation;
 
 
 import solver.ICause;
 import solver.Solver;
 import solver.constraints.Constraint;
 import solver.constraints.propagators.Propagator;
 import solver.exception.ContradictionException;
 import solver.propagation.generator.PropagationStrategy;
 import solver.propagation.generator.Sort;
 import solver.propagation.wm.IWaterMarking;
 import solver.propagation.wm.WaterMarkers;
 import solver.variables.EventType;
 import solver.variables.Variable;
 
 /**
  * An abstract class of IPropagatioEngine.
  * It allows scheduling and propagation of ISchedulable object, like IEventRecorder or Group.
  * <br/>
  *
  * @author Charles Prud'homme
  * @since 05/12/11
  */
 public class PropagationEngine implements IPropagationEngine {
 
     protected final ContradictionException exception;
 
     protected PropagationStrategy propagationStrategy;
 
     protected IWaterMarking watermarks; // marks every pair of V-P, breaking multiple apperance of V in P
 
     protected int pivot;
 
     protected boolean initialized = false;
 
     public PropagationEngine() {
         this.exception = new ContradictionException();
     }
 
     @Override
     public boolean initialized() {
         return initialized;
     }
 
     @Override
     public boolean hasStrategy() {
         return propagationStrategy != null;
     }
 
     @Override
     public void set(PropagationStrategy propagationStrategy) {
         this.propagationStrategy = propagationStrategy;
     }
 
     public void init(Solver solver) {
         if (!initialized) {
             pivot = solver.getNbIdElt();
             Constraint[] constraints = solver.getCstrs();
             // 1. water mark every couple variable-propagator of the solver
             waterMark(constraints);
             // 2. add default strategy, default group => arc and coarse in a queue
             propagationStrategy = Sort.build(propagationStrategy, buildDefault(solver));
             // 3. build groups based on the strategy defined
             propagationStrategy.populate(this, solver);
             if (!watermarks.isEmpty()) {
                 throw new RuntimeException("default strategy has encountered a problem :: " + watermarks);
             }
             // 4. remove default if empty
             ///cpru a faire
             //Then, schedule constraints for initial propagation
             for (int c = 0; c < constraints.length; c++) {
                 Propagator[] propagators = constraints[c].propagators;
                 for (int p = 0; p < propagators.length; p++) {
                     propagators[p].forcePropagate(EventType.FULL_PROPAGATION);
                 }
             }
 
         }
         initialized = true;
     }
 
     private void waterMark(Constraint[] constraints) {
         watermarks = WaterMarkers.make(pivot);
         for (int c = 0; c < constraints.length; c++) {
             Propagator[] propagators = constraints[c].propagators;
             for (int p = 0; p < propagators.length; p++) {
                 Propagator propagator = propagators[p];
                 int idP = propagator.getId();
                 watermarks.putMark(idP);
                 int nbV = propagator.getNbVars();
                 for (int v = 0; v < nbV; v++) {
                     Variable variable = propagator.getVar(v);
                     int idV = variable.getId();
                     watermarks.putMark(idV, idP, v);
                 }
             }
         }
     }
 
     public void clearWatermark(int id1, int id2, int id3) {
         if (id1 == 0) {// coarse case
             watermarks.clearMark(id2);
         } else if (id2 == 0) {// coarse case
             watermarks.clearMark(id1);
         } else {
             watermarks.clearMark(id1, id2, id3);
         }
     }
 
     public boolean isMarked(int id1, int id2, int id3) {
         if (id1 == 0) {// coarse case
             return watermarks.isMarked(id2);
         } else if (id2 == 0) {// coarse case
             return watermarks.isMarked(id1);
         } else {
             return watermarks.isMarked(id1, id2, id3);
     }
     }
 
     protected PropagationStrategy buildDefault(Solver solver) {
         return PropagationStrategies.ONE_QUEUE_WITH_ARCS.make(solver);
     }
 
 
     @Override
     public void propagate() throws ContradictionException {
         propagationStrategy.execute();
         assert propagationStrategy.isEmpty();
     }
 
     @Override
     public void flush() {
         propagationStrategy.flush();
     }
 
     @Override
     public void fails(ICause cause, Variable variable, String message) throws ContradictionException {
         throw exception.set(cause, variable, message);
     }
 
     @Override
     public ContradictionException getContradictionException() {
         return exception;
     }
 
     @Override
     public void clear() {
         propagationStrategy = null;
         initialized = false;
     }
 
     //    /**
 //     * Initialize this <code>IPropagationEngine</code> object with the array of <code>Constraint</code> and <code>Variable</code> objects.
 //     * It automatically pushes an event (call to <code>propagate</code>) for each constraints, the initial awake.
 //     */
 //    public void init() {
 //        if (engine != null) {
 //            throw new SolverException("PropagationEngine.init() has already been called once");
 //        }
 //        final IRequest[] tmp = records;
 //        records = new IRequest[size];
 //        System.arraycopy(tmp, 0, records, 0, size);
 //
 //        // FIRST: sort records, give them a unique group
 //        // build a default group
 //        addGroup(Group.buildQueue(Predicates.all(), Policy.FIXPOINT));
 //
 ////        eval();
 //        extract();
 //
 //
 //        switch (deal) {
 //            case SEQUENCE:
 //                engine = new WhileEngine();
 //                break;
 //            case QUEUE:
 //                engine = new OldestEngine();
 //                break;
 //        }
 //        engine.setGroups(Arrays.copyOfRange(groups, 0, nbGroup));
 //
 //        // FINALLY, post initial propagation event for every heavy records
 //        for (int i = offset; i < size; i++) {
 //            records[i].update(EventType.FULL_PROPAGATION); // post initial propagation
 //        }
 //
 //    }
 //
 //    private void eval() {
 //        int i, j;
 //        for (i = 0; i < size; i++) {
 //            lastPoppedRequest = records[i];
 //            j = 0;
 //            // look for the first right group
 //            while (!groups[j].getPredicate().eval(lastPoppedRequest)) {
 //                j++;
 //            }
 //            groups[j].addRecorder(lastPoppedRequest);
 //        }
 //        for (j = 0; j < nbGroup; j++) {
 //            if (groups[j].isEmpty()) {
 //                groups[j] = groups[nbGroup - 1];
 //                groups[j].setIndex(j);
 //                groups[nbGroup - 1] = null;
 //                nbGroup--;
 //                j--;
 //            } else {
 //                groups[j].make();
 //            }
 //        }
 //    }
 //
 //
 //    private void extract() {
 //        int i, j;
 //        for (i = 0; i < size; i++) {
 //            records[i].setIndex(IRequest.IN_GROUP,i);
 //        }
 //        for (j = 0; j < nbGroup; j++) {
 //            int[] indices = groups[j].getPredicate().extract(records);
 //            Arrays.sort(indices);
 //            for (i = 0; i < indices.length; i++) {
 //                if (records[indices[i]].getIndex(IRequest.GROUP_ID) < 0) {
 //                    groups[j].addRecorder(records[indices[i]]);
 //                }
 //            }
 //            if (groups[j].isEmpty()) {
 //                groups[j] = groups[nbGroup - 1];
 //                groups[j].setIndex(j);
 //                groups[nbGroup - 1] = null;
 //                nbGroup--;
 //                j--;
 //            } else {
 //                groups[j].make();
 //            }
 //        }
 //    }
 }
