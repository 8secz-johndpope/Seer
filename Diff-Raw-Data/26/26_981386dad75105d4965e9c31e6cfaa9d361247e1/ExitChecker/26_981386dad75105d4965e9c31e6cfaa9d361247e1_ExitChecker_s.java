 /*
  * This file is part of the Polyglot extensible compiler framework.
  *
  * Copyright (c) 2000-2006 Polyglot project group, Cornell University
  * Copyright (c) 2007 IBM Corporation
  * 
  */
 
 package polyglot.visit;
 
 import java.util.*;
 
 import polyglot.ast.*;
 import polyglot.frontend.Job;
 import polyglot.types.SemanticException;
 import polyglot.types.TypeSystem;
 import polyglot.visit.DataFlow.Item;
 import polyglot.visit.FlowGraph.EdgeKey;
 
 /**
  * Visitor which checks that all (terminating) paths through a 
  * method must return.
  */
 public class ExitChecker extends DataFlow
 {
     protected CodeNode code;
 
     public ExitChecker(Job job, TypeSystem ts, NodeFactory nf) {
 	super(job, ts, nf, false /* backward analysis */);
     }
 
     protected FlowGraph initGraph(CodeNode code, Term root) {
         boolean returnsValue;
 
         this.code = code;
 
         if (code instanceof MethodDecl) {
             MethodDecl d = (MethodDecl) code;
             if (! d.methodDef().returnType().get().isVoid()) {
                 return super.initGraph(code, root);
             }
         }
 
         return null;
     }
 
     public Item createInitialItem(FlowGraph graph, Term node, boolean entry) {
         return DataFlowItem.EXITS;
     }
 
     protected static class DataFlowItem extends Item {
         public final boolean exits; // whether all paths leaving this node lead to an exit 
 
         protected DataFlowItem(boolean exits) {
             this.exits = exits;
         }
         
         public static final DataFlowItem EXITS = new DataFlowItem(true);
         public static final DataFlowItem DOES_NOT_EXIT = new DataFlowItem(false);
 
         public String toString() {
             return "exits=" + exits;
         }
         public boolean equals(Object o) {
             if (o instanceof DataFlowItem) {
                 return this.exits == ((DataFlowItem)o).exits;
             }
             return false;
         }
         public int hashCode() {
             return (exits ? 5235 : 8673);
         }
         
     }
     
     public Map<EdgeKey, Item> flow(Item in, FlowGraph graph, Term n, boolean entry, Set<EdgeKey> succEdgeKeys) {
         // If every path from the exit node to the entry goes through a return,
         // we're okay.  So make the exit bit false at exit and true at every return;
         // the confluence operation is &&. 
         // We deal with exceptions specially, and assume that any exception
         // edge to the exit node is OK.
         if (n instanceof Return) {
             return itemToMap(DataFlowItem.EXITS, succEdgeKeys);
         }
 
         if (n == graph.root() && !entry) {           
             // all exception edges to the exit node are regarded as exiting
             // correctly. Make sure non-exception edges have the
             // exit bit false.
             Map<EdgeKey, Item> m = itemToMap(DataFlowItem.EXITS, succEdgeKeys);
             if (succEdgeKeys.contains(FlowGraph.EDGE_KEY_OTHER)) {
                 m.put(FlowGraph.EDGE_KEY_OTHER, DataFlowItem.DOES_NOT_EXIT);
             }
             if (succEdgeKeys.contains(FlowGraph.EDGE_KEY_TRUE)) {
                 m.put(FlowGraph.EDGE_KEY_TRUE, DataFlowItem.DOES_NOT_EXIT);
             }
             if (succEdgeKeys.contains(FlowGraph.EDGE_KEY_FALSE)) {
                 m.put(FlowGraph.EDGE_KEY_FALSE, DataFlowItem.DOES_NOT_EXIT);
             }
             
             return m;
         }
 
         return itemToMap(in, succEdgeKeys);
     }
 
 
     public Item confluence(List<Item> inItems, Term node, boolean entry, FlowGraph graph) {
         // all paths must have an exit
         for (Item item : inItems) {
             if (!((DataFlowItem)item).exits) {
                 return DataFlowItem.DOES_NOT_EXIT;
             }
         }
         return DataFlowItem.EXITS; 
     }
 
     public void check(FlowGraph graph, Term n, boolean entry, Item inItem, Map<EdgeKey, Item> outItems) {
         // Check for statements not on the path to exit; compound
         // statements are allowed to be off the path.  (e.g., "{ return; }"
         // or "while (true) S").  If a compound statement is truly
         // unreachable, one of its sub-statements will be also and we will
         // report an error there.
         if (n == graph.root() && entry) {
             if (outItems != null && !outItems.isEmpty()) {
                 // due to the flow equations, all DataFlowItems in the outItems map
                 // are the same, so just take the first one.
                DataFlowItem outItem = (DataFlowItem)outItems.values().iterator().next(); 
                if (outItem != null && !outItem.exits) { 
                    reportError("Missing return statement.",
                            code.position());
                 }
             }
         }
     }
 }
