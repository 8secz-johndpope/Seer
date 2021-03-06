 /*******************************************************************************
  * Copyright (c) 2002 - 2006 IBM Corporation.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package com.ibm.wala.ipa.cfg;
 
 import com.ibm.wala.cfg.ControlFlowGraph;
 import com.ibm.wala.ipa.callgraph.CGNode;
 import com.ibm.wala.ipa.callgraph.CallGraph;
 import com.ibm.wala.ssa.IR;
 import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
 import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph.ExplodedBasicBlock;
 import com.ibm.wala.util.collections.Filtersection;
 
 /**
  * 
  * Interprocedural control-flow graph.
  * 
  * TODO: think about a better implementation; perhaps a lazy view of the
  * constituent CFGs Lots of ways this can be optimized?
  * 
  * @author sfink
  * @author Julian Dolby
  */
 public class ExplodedInterproceduralCFG extends AbstractInterproceduralCFG<ExplodedBasicBlock> {
 
 
   public static ExplodedInterproceduralCFG make(CallGraph CG) {
     return new ExplodedInterproceduralCFG(CG);
   }
 
   private ExplodedInterproceduralCFG(CallGraph CG) {
     super(CG);
   }
 
   public ExplodedInterproceduralCFG(CallGraph cg, Filtersection<CGNode> filtersection) {
     super(cg,filtersection);
   }
 
   /**
    * @return the cfg for n, or null if none found
    * @throws IllegalArgumentException
    *             if n == null
    */
   @Override
   public ControlFlowGraph<ExplodedBasicBlock> getCFG(CGNode n) throws IllegalArgumentException {
     if (n == null) {
       throw new IllegalArgumentException("n == null");
     }
     IR ir = n.getIR();
    if (ir == null) {
      return null;
    }
     return ExplodedControlFlowGraph.make(ir);
   }
 
 }
