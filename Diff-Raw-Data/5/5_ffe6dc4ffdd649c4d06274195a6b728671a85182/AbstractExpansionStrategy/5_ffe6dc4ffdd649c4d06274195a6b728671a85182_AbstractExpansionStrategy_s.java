 // Copyright 2008 by Oxford University; see license.txt for details
 package org.semanticweb.HermiT.existentials;
 
 import java.io.Serializable;
 import java.util.ArrayList;
 import java.util.List;
 
 import org.semanticweb.HermiT.blocking.BlockingStrategy;
 import org.semanticweb.HermiT.model.AtLeastConcept;
 import org.semanticweb.HermiT.model.AtomicRole;
 import org.semanticweb.HermiT.model.Concept;
 import org.semanticweb.HermiT.model.DLClause;
 import org.semanticweb.HermiT.model.ExistentialConcept;
 import org.semanticweb.HermiT.model.ExistsDescriptionGraph;
 import org.semanticweb.HermiT.model.Inequality;
 import org.semanticweb.HermiT.model.InverseRole;
 import org.semanticweb.HermiT.model.LiteralConcept;
 import org.semanticweb.HermiT.model.Role;
 import org.semanticweb.HermiT.model.Variable;
 import org.semanticweb.HermiT.monitor.TableauMonitor;
 import org.semanticweb.HermiT.tableau.DLClauseEvaluator;
 import org.semanticweb.HermiT.tableau.DescriptionGraphManager;
 import org.semanticweb.HermiT.tableau.ExistentialExpansionManager;
 import org.semanticweb.HermiT.tableau.ExtensionManager;
 import org.semanticweb.HermiT.tableau.ExtensionTable;
 import org.semanticweb.HermiT.tableau.InterruptFlag;
 import org.semanticweb.HermiT.tableau.Node;
 import org.semanticweb.HermiT.tableau.Tableau;
 
 /**
  * Implements the common bits of an ExistentialsExpansionStrategy, leaving only actual processing of existentials in need of expansion to subclasses.
  */
 public abstract class AbstractExpansionStrategy implements Serializable,ExistentialExpansionStrategy {
     private static final long serialVersionUID=2831957929321676444L;
     protected static enum SatType { NOT_SATISFIED,PERMANENTLY_SATISFIED,CURRENTLY_SATISFIED };
 
     protected final BlockingStrategy m_blockingStrategy;
     protected final boolean m_expandNodeAtATime;
     protected final List<ExistentialConcept> m_processedExistentials;
     protected final List<Node> m_auxiliaryNodes1;
     protected final List<Node> m_auxiliaryNodes2;
     protected Tableau m_tableau;
     protected InterruptFlag m_interruptFlag;
     protected ExtensionManager m_extensionManager;
     protected ExtensionTable.Retrieval m_ternaryExtensionTableSearch01Bound;
     protected ExtensionTable.Retrieval m_ternaryExtensionTableSearch02Bound;
     protected ExistentialExpansionManager m_existentialExpansionManager;
     protected DescriptionGraphManager m_descriptionGraphManager;
 
     public AbstractExpansionStrategy(BlockingStrategy blockingStrategy,boolean expandNodeAtATime) {
         m_blockingStrategy=blockingStrategy;
         m_expandNodeAtATime=expandNodeAtATime;
         m_processedExistentials=new ArrayList<ExistentialConcept>();
         m_auxiliaryNodes1=new ArrayList<Node>();
         m_auxiliaryNodes2=new ArrayList<Node>();
     }
     public void initialize(Tableau tableau) {
         m_tableau=tableau;
         m_interruptFlag=m_tableau.getInterruptFlag();
         m_extensionManager=m_tableau.getExtensionManager();
         m_ternaryExtensionTableSearch01Bound=m_extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[] { true,true,false },ExtensionTable.View.TOTAL);
         m_ternaryExtensionTableSearch02Bound=m_extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[] { true,false,true },ExtensionTable.View.TOTAL);
         m_existentialExpansionManager=m_tableau.getExistentialExpansionManager();
         m_descriptionGraphManager=m_tableau.getDescriptionGraphManager();
         m_blockingStrategy.initialize(m_tableau);
     }
     public void clear() {
         m_blockingStrategy.clear();
         m_processedExistentials.clear();
     }
     public boolean expandExistentials(boolean finalChance) {
         TableauMonitor monitor=m_tableau.getTableauMonitor();
         m_blockingStrategy.computeBlocking(finalChance);
         boolean extensionsChanged=false;
         Node node=m_tableau.getFirstTableauNode();
         while (node!=null && (!extensionsChanged || !m_expandNodeAtATime)) {
             if (node.isActive() && !node.isBlocked() && node.hasUnprocessedExistentials()) {
                 // The node's set of unprocessed existentials may be changed during operation, so make a local copy to loop over.
                 m_processedExistentials.clear();
                 m_processedExistentials.addAll(node.getUnprocessedExistentials());
                 for (int index=0;index<m_processedExistentials.size();index++) {
                     ExistentialConcept existentialConcept=m_processedExistentials.get(index);
                     if (existentialConcept instanceof AtLeastConcept) {
                         AtLeastConcept atLeastConcept=(AtLeastConcept)existentialConcept;
                         switch (isSatisfied(atLeastConcept,node)) {
                         case NOT_SATISFIED:
                             expandExistential(atLeastConcept,node);
                             extensionsChanged=true;
                             break;
                         case PERMANENTLY_SATISFIED: // not satisfied by a nominal so that the NN/NI rule can break the existential 
                             m_existentialExpansionManager.markExistentialProcessed(existentialConcept,node);
                             if (monitor!=null)
                                 monitor.existentialSatisfied(atLeastConcept,node);
                             break;
                         case CURRENTLY_SATISFIED: // satisfied until the NN/NI rule is applied and after which the existential might no longer be satisfied
                             // do nothing
                             if (monitor!=null)
                                 monitor.existentialSatisfied(atLeastConcept,node);
                             break;
                         }
                     }
                     else if (existentialConcept instanceof ExistsDescriptionGraph) {
                         ExistsDescriptionGraph existsDescriptionGraph=(ExistsDescriptionGraph)existentialConcept;
                         if (!m_descriptionGraphManager.isSatisfied(existsDescriptionGraph,node)) {
                             m_descriptionGraphManager.expand(existsDescriptionGraph,node);
                             extensionsChanged=true;
                         }
                         else {
                             if (monitor!=null)
                                 monitor.existentialSatisfied(existsDescriptionGraph,node);
                         }
                         m_existentialExpansionManager.markExistentialProcessed(existentialConcept,node);
                     }
                     else
                         throw new IllegalStateException("Unsupported type of existential.");
                     m_interruptFlag.checkInterrupt();
                 }
             }
             node=node.getNextTableauNode();
             m_interruptFlag.checkInterrupt();
         }
         return extensionsChanged;
     }
     public void assertionAdded(Concept concept,Node node,boolean isCore) {
         m_blockingStrategy.assertionAdded(concept,node,isCore);
     }
     public void assertionCoreSet(Concept concept,Node node) {
         m_blockingStrategy.assertionCoreSet(concept,node);
     }
     public void assertionRemoved(Concept concept,Node node,boolean isCore) {
         m_blockingStrategy.assertionRemoved(concept,node,isCore);
     }
     public void assertionAdded(AtomicRole atomicRole,Node nodeFrom,Node nodeTo,boolean isCore) {
         m_blockingStrategy.assertionAdded(atomicRole,nodeFrom,nodeTo,isCore);
     }
     public void assertionCoreSet(AtomicRole atomicRole,Node nodeFrom,Node nodeTo) {
         m_blockingStrategy.assertionCoreSet(atomicRole,nodeFrom,nodeTo);
     }
     public void assertionRemoved(AtomicRole atomicRole,Node nodeFrom,Node nodeTo,boolean isCore) {
         m_blockingStrategy.assertionRemoved(atomicRole,nodeFrom,nodeTo,isCore);
     }
     public void nodeStatusChanged(Node node) {
         m_blockingStrategy.nodeStatusChanged(node);
     }
     public void nodeInitialized(Node node) {
         m_blockingStrategy.nodeInitialized(node);
     }
     public void nodeDestroyed(Node node) {
         m_blockingStrategy.nodeDestroyed(node);
     }
     public void branchingPointPushed() {
     }
     public void backtrack() {
     }
     public void modelFound() {
         m_blockingStrategy.modelFound();
     }
     public boolean isExact() {
         return m_blockingStrategy.isExact();
     }
     public void dlClauseBodyCompiled(List<DLClauseEvaluator.Worker> workers,DLClause dlClause,List<Variable> variables,Object[] valuesBuffer,boolean[] coreVariables) {
         m_blockingStrategy.dlClauseBodyCompiled(workers,dlClause,variables,valuesBuffer,coreVariables);
     }
     protected SatType isSatisfied(AtLeastConcept atLeastConcept,Node forNode) {
         int cardinality=atLeastConcept.getNumber();
         if (cardinality<=0)
             return SatType.PERMANENTLY_SATISFIED;
         Role onRole=atLeastConcept.getOnRole();
         LiteralConcept toConcept=atLeastConcept.getToConcept();
         ExtensionTable.Retrieval retrieval;
         int toNodeIndex;
         if (onRole instanceof AtomicRole) {
             retrieval=m_ternaryExtensionTableSearch01Bound;
             retrieval.getBindingsBuffer()[0]=onRole;
             retrieval.getBindingsBuffer()[1]=forNode;
             toNodeIndex=2;
         }
         else {
             retrieval=m_ternaryExtensionTableSearch02Bound;
             retrieval.getBindingsBuffer()[0]=((InverseRole)onRole).getInverseOf();
             retrieval.getBindingsBuffer()[2]=forNode;
             toNodeIndex=1;
         }
         if (cardinality==1) {
             retrieval.open();
             Object[] tupleBuffer=retrieval.getTupleBuffer();
             while (!retrieval.afterLast()) {
                 Node toNode=(Node)tupleBuffer[toNodeIndex];
                if (!toNode.isIndirectlyBlocked() && m_extensionManager.containsConceptAssertion(toConcept,toNode)) {
                     if (isPermanentSatisfier(forNode,toNode) && m_blockingStrategy.isPermanentAssertion(toConcept,toNode))
                         return SatType.PERMANENTLY_SATISFIED;
                     else
                         return SatType.CURRENTLY_SATISFIED;
                 }
                 retrieval.next();
             }
             return SatType.NOT_SATISFIED;
         }
         else {
             m_auxiliaryNodes1.clear();
             retrieval.open();
             Object[] tupleBuffer=retrieval.getTupleBuffer();
             boolean allSatisfiersArePermanent=true;
             while (!retrieval.afterLast()) {
                 Node toNode=(Node)tupleBuffer[toNodeIndex];
                if (!toNode.isIndirectlyBlocked() && m_extensionManager.containsConceptAssertion(toConcept,toNode)) {
                     if (!isPermanentSatisfier(forNode,toNode) || !m_blockingStrategy.isPermanentAssertion(toConcept,toNode))
                         allSatisfiersArePermanent=false;
                     m_auxiliaryNodes1.add(toNode);
                 }
                 retrieval.next();
             }
             if (m_auxiliaryNodes1.size()>=cardinality) {
                 m_auxiliaryNodes2.clear();
                 if (containsSubsetOfNUnequalNodes(forNode,m_auxiliaryNodes1,0,m_auxiliaryNodes2,cardinality))
                     return allSatisfiersArePermanent ? SatType.PERMANENTLY_SATISFIED : SatType.CURRENTLY_SATISFIED;
             }
             return SatType.NOT_SATISFIED;
         }
     }
     protected boolean isPermanentSatisfier(Node forNode,Node toNode) {
         return forNode==toNode || forNode.getParent()==toNode || toNode.getParent()==forNode || toNode.isRootNode();
     }
     protected boolean containsSubsetOfNUnequalNodes(Node forNode,List<Node> nodes,int startAt,List<Node> selectedNodes,int cardinality) {
         if (selectedNodes.size()==cardinality)
             return true;
         else {
             outer: for (int index=startAt;index<nodes.size();index++) {
                 Node node=nodes.get(index);
                 for (int selectedNodeIndex=0;selectedNodeIndex<selectedNodes.size();selectedNodeIndex++) {
                     Node selectedNode=selectedNodes.get(selectedNodeIndex);
                     if (!m_extensionManager.containsAssertion(Inequality.INSTANCE,node,selectedNode) && !m_extensionManager.containsAssertion(Inequality.INSTANCE,selectedNode,node))
                         continue outer;
                 }
                 selectedNodes.add(node);
                 if (containsSubsetOfNUnequalNodes(forNode,nodes,index+1,selectedNodes,cardinality))
                     return true;
                 selectedNodes.remove(selectedNodes.size()-1);
             }
             return false;
         }
     }
     /**
      * This method performs the actual expansion.
      */
     protected abstract void expandExistential(AtLeastConcept atLeastConcept,Node forNode);
 }
