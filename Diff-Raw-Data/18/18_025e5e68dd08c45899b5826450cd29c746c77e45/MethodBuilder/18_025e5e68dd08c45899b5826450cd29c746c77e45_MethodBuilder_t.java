 /*
   This file is part of JOP, the Java Optimized Processor
     see <http://www.jopdesign.com/>
 
   Copyright (C) 2008, Benedikt Huber (benedikt.huber@gmail.com)
 
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.
 
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
 
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
 package com.jopdesign.wcet.uppaal.translator;
 
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Set;
 
 import com.jopdesign.build.MethodInfo;
 import com.jopdesign.wcet.ProcessorModel;
 import com.jopdesign.wcet.Project;
 import com.jopdesign.wcet.analysis.RecursiveAnalysis;
 import com.jopdesign.wcet.analysis.WcetCost;
 import com.jopdesign.wcet.config.Config;
 import com.jopdesign.wcet.frontend.ControlFlowGraph;
 import com.jopdesign.wcet.frontend.WcetAppInfo;
 import com.jopdesign.wcet.frontend.ControlFlowGraph.BasicBlockNode;
 import com.jopdesign.wcet.frontend.ControlFlowGraph.CFGEdge;
 import com.jopdesign.wcet.frontend.ControlFlowGraph.CFGNode;
 import com.jopdesign.wcet.frontend.ControlFlowGraph.CfgVisitor;
 import com.jopdesign.wcet.frontend.ControlFlowGraph.DedicatedNode;
 import com.jopdesign.wcet.frontend.ControlFlowGraph.InvokeNode;
 import com.jopdesign.wcet.frontend.ControlFlowGraph.SummaryNode;
 import com.jopdesign.wcet.graphutils.FlowGraph;
 import com.jopdesign.wcet.graphutils.Pair;
 import com.jopdesign.wcet.graphutils.LoopColoring.IterationBranchLabel;
 import com.jopdesign.wcet.graphutils.TopOrder.BadGraphException;
 import com.jopdesign.wcet.jop.MethodCache;
 import com.jopdesign.wcet.jop.CacheConfig.StaticCacheApproximation;
 import com.jopdesign.wcet.uppaal.UppAalConfig;
 import com.jopdesign.wcet.uppaal.model.Location;
 import com.jopdesign.wcet.uppaal.model.Template;
 import com.jopdesign.wcet.uppaal.model.Transition;
 import com.jopdesign.wcet.uppaal.model.TransitionAttributes;
 /**
  * Build UppAal templates for a Java method.
  * We map the CFG's nodes and edges to 
  * {@link Location}s and {@link Transition}s.
  * Nodes are mapped to subgraphs (with unique start and end node),
  * i.e. instances of <code>FlowGraph<Location,Transition></code>
  * 
  * @author Benedikt Huber <benedikt.huber@gmail.com>
  *
  */
 public class MethodBuilder implements CfgVisitor {
 	private abstract class SyncBuilder {
 		public abstract void methodEntry(Location entry);
 		public abstract void methodExit(Location exit, Transition entryToExit);
 		protected abstract Location createMethodInvocation(InvokeNode n, Location basicBlockExit);
 		public Location invokeMethod(InvokeNode n, Location basicBlockNode) {
 			if(project.getCallGraph().isLeafNode(n.getImplementedMethod()) &&
 			   Config.instance().getOption(UppAalConfig.UPPAAL_COLLAPSE_LEAVES)) {
 				RecursiveAnalysis<StaticCacheApproximation> ilpAn = 
 					new RecursiveAnalysis<StaticCacheApproximation>(project,new RecursiveAnalysis.LocalIPETStrategy());
 				StaticCacheApproximation cacheApprox;
 				if(cacheSim.isAlwaysMiss()) {
 					cacheApprox = StaticCacheApproximation.ALWAYS_MISS;
 				} else {
 					cacheApprox = StaticCacheApproximation.ALWAYS_HIT;
 				}
 				WcetCost wcet = ilpAn.computeWCET(n.getImplementedMethod(), cacheApprox);
 				Location inv = tBuilder.createLocation("IN_"+n.getId());
 				tBuilder.waitAtLocation(inv, wcet.getCost());
 				return inv;
 			} else {
 				return createMethodInvocation(n,basicBlockNode);
 			}
 		}
 	}
 	private class SyncViaVariables extends SyncBuilder {
 		public void methodEntry(Location entry) {
 			tBuilder.getOutgoingAttrs(entry)
 			.appendGuard(getId() + " == " + SystemBuilder.ACTIVE_METHOD)
 			.appendUpdate(String.format("%s := %s", 
 							TemplateBuilder.LOCAL_CALL_STACK_DEPTH, 
 							SystemBuilder.CURRENT_CALL_STACK_DEPTH))
 			.setSync(SystemBuilder.INVOKE_CHAN+"?");			
 		}
 		public void methodExit(Location exit, Transition exitToEntry) {
 			exitToEntry.getAttrs().setSync(SystemBuilder.RETURN_CHAN+"!");
 			tBuilder.getIncomingAttrs(exit).
 				appendUpdate(String.format("%1$s := %1$s - 1", 
 	        				 SystemBuilder.CURRENT_CALL_STACK_DEPTH)); 
 			
 		}
 		public Location createMethodInvocation(InvokeNode n, Location basicBlockExit) {
 			Location inNode = tBuilder.createLocation("IN_"+n.getId());
 			tBuilder.getIncomingAttrs(inNode)
 				.appendUpdate(String.format("%1$s := %1$s + 1",
 												  SystemBuilder.CURRENT_CALL_STACK_DEPTH))
 				.setSync(SystemBuilder.INVOKE_CHAN+"!");
 			tBuilder.getIncomingAttrs(basicBlockExit)
 				.appendUpdate(String.format("%s := %d", 
 							  					   SystemBuilder.ACTIVE_METHOD, 
 							  					   n.receiverFlowGraph().getId()));
 			tBuilder.getOutgoingAttrs(inNode)
 				.appendGuard(String.format("%s == %s", 
 												  SystemBuilder.CURRENT_CALL_STACK_DEPTH, 
 												  TemplateBuilder.LOCAL_CALL_STACK_DEPTH))
 				.setSync(SystemBuilder.RETURN_CHAN+"?");
 			return inNode;
 		}
 	}
 	private class SyncViaChannels extends SyncBuilder {
 		public void methodEntry(Location entry) {
 			tBuilder.getOutgoingAttrs(entry)
 			 .setSync(SystemBuilder.methodChannel(cfg.getId())+"?");
 		}
 		public void methodExit(Location exit, Transition exitToEntry) {
 			exitToEntry.getAttrs()
 				.setSync(SystemBuilder.methodChannel(cfg.getId())+"!");			
 		}
 		public Location createMethodInvocation(InvokeNode n, Location _) {
 			Location inNode = tBuilder.createLocation("IN_"+n.getId());
 			tBuilder.getIncomingAttrs(inNode)
 				.setSync(SystemBuilder.methodChannel(n.receiverFlowGraph().getId())+"!");
 			tBuilder.getOutgoingAttrs(inNode)
 				.setSync(SystemBuilder.methodChannel(n.receiverFlowGraph().getId())+"?");
 			return inNode;
 		}
 	}
 
 	private static class NodeAutomaton extends Pair<Location,Location>{
 		private static final long serialVersionUID = 1L;
 		public NodeAutomaton(Location entry, Location exit) {
 			super(entry, exit);
 		}
 		public Location getEntry() { return fst(); }
 		public Location getExit()  { return snd(); }
 		public static NodeAutomaton singleton(Location exit) {
 			return new NodeAutomaton(exit,exit);
 		}
 	}
 	private Project project;
 	private WcetAppInfo wAppInfo;
 	private ControlFlowGraph cfg;
 	private TemplateBuilder tBuilder;
 	private Map<CFGNode,NodeAutomaton> nodeTemplates;
 	private boolean isRoot;
 	private SyncBuilder syncBuilder;
 	private int mId;
 	private CacheSimBuilder cacheSim;
 	private boolean oneChanPerMethod;
 	private SystemBuilder sys;
 	private ProcessorModel processor;
 	private MethodCache cacheImpl;
 	public MethodBuilder(SystemBuilder sys, int mId, MethodInfo mi) {
 		this.sys = sys;
 		this.project = sys.getProject();
 		this.wAppInfo = sys.getProject().getWcetAppInfo();
 		this.processor = project.getProcessorModel();
 		this.cacheImpl = processor.getMethodCache();
 		this.mId = mId;
 		this.cfg = wAppInfo.getFlowGraph(mi);
 		this.cacheSim = sys.getCacheSim();
 		this.oneChanPerMethod = Config.instance().getOption(UppAalConfig.UPPAAL_ONE_CHANNEL_PER_METHOD);
 		if(Config.instance().getOption(UppAalConfig.UPPAAL_COLLAPSE_LEAVES)) {
 			try {
 				cfg.insertSummaryNodes();
 			} catch (BadGraphException e) {
 				throw new AssertionError("Faild to insert summary nodes: "+e);
 			}			
 		}
 	}
 	/**
 	 * To translate the root method
 	 * @param fg
 	 * @return
 	 */
 	public Template buildRootMethod() {
 		return buildMethod(true);
 	}
 
 	public Template buildMethod() {
 		return buildMethod(false);
 	}
 	private Template buildMethod(boolean isRoot) {
 		if(oneChanPerMethod) {
 			syncBuilder = new SyncViaChannels();
 		} else {
 			syncBuilder = new SyncViaVariables();
 		}
 		this.nodeTemplates = new HashMap<CFGNode, NodeAutomaton>();
 		this.isRoot = isRoot;
 		this.tBuilder = new TemplateBuilder("Method"+mId,mId,
 										    cfg.getLoopBounds());
 		this.tBuilder.addDescription("Template for method "+cfg.getMethodInfo());
 		FlowGraph<CFGNode, CFGEdge> graph = cfg.getGraph();
 		/* Translate the CFGs nodes */
 		for(CFGNode node : graph.vertexSet()) {
 			node.accept(this);
 		}
 		/* Translate the CFGs edges */
 		for(CFGEdge edge : graph.edgeSet()) {
 			buildEdge(edge);
 		}
 		new LayoutCFG(100,120).layoutCfgModel(tBuilder.getFinalTemplate());
 		return tBuilder.getFinalTemplate();
 	}
 	
 	public void visitSpecialNode(DedicatedNode n) {
 		NodeAutomaton localTranslation = null;
 		switch(n.getKind()) {
 		case ENTRY:
 			if(isRoot)  localTranslation = createRootEntry(n); 
 			else		localTranslation = createEntry(n);
 			break;
 		case EXIT:
 			if(isRoot) localTranslation = createRootExit(n);
 			else 	   localTranslation = createExit(n);
 			break;
 		case SPLIT:
 			localTranslation = createSplit(n);break;
 		case JOIN:
 			localTranslation = createJoin(n);break;
 		}
 		this.nodeTemplates.put(n,localTranslation);
 	}	
 	
 	public void visitBasicBlockNode(BasicBlockNode n) {
 		NodeAutomaton bbLoc = 
 			createBasicBlock(n.getId(),project.getProcessorModel().basicBlockWCET(n.getBasicBlock()));
 		this.nodeTemplates.put(n,bbLoc);
 	}
 	
 	public void visitInvokeNode(InvokeNode n) {
 		this.nodeTemplates.put(n,createInvoke(n));		
 	}
 	
 	public void visitSummaryNode(SummaryNode n) {
 		RecursiveAnalysis<StaticCacheApproximation> an = 
 			new RecursiveAnalysis<StaticCacheApproximation>(project,new RecursiveAnalysis.LocalIPETStrategy());
 		WcetCost cost = an.runWCETComputation("SUBGRAPH"+n.getId(), n.getSubGraph(), StaticCacheApproximation.ALWAYS_MISS)
 				          .getTotalCost();
 		NodeAutomaton sumLoc = createBasicBlock(n.getId(),cost.getCost());
 		this.nodeTemplates.put(n,sumLoc);
 	}
 	private void buildEdge(CFGEdge edge) {
 		FlowGraph<CFGNode, CFGEdge> graph = cfg.getGraph();
 		Set<CFGNode> hols = cfg.getLoopColoring().getHeadOfLoops();
 		Set<CFGEdge> backEdges = cfg.getLoopColoring().getBackEdges();
 		Map<CFGEdge, IterationBranchLabel<CFGNode>> edgeColoring = 
 			cfg.getLoopColoring().getIterationBranchEdges();
 		CFGNode src = graph.getEdgeSource(edge);
 		CFGNode target = graph.getEdgeTarget(edge);
		if(src == cfg.getEntry() && target == cfg.getExit()) return;
 		Transition transition = tBuilder.createTransition(
 				nodeTemplates.get(src).snd(),
 				nodeTemplates.get(target).fst());
 		TransitionAttributes attrs = transition.getAttrs();
 		IterationBranchLabel<CFGNode> edgeColor = edgeColoring.get(edge);
 		if(edgeColor != null) {
 			for(CFGNode loop : edgeColor.getContinues()) {
 				attrs.appendGuard(tBuilder.contLoopGuard(loop));
 				attrs.appendUpdate(tBuilder.incrLoopCounter(loop));
 			}
 			for(CFGNode loop : edgeColor.getExits()) {
 				attrs.appendGuard(tBuilder.exitLoopGuard(loop));
 				attrs.appendUpdate(tBuilder.resetLoopCounter(loop));
 			}
 		}
 		if(hols.contains(target) && ! backEdges.contains(edge)) {
 			attrs.appendUpdate(tBuilder.resetLoopCounter(target));
 		}
 	}
 	
 	private NodeAutomaton createRootEntry(DedicatedNode n) {
 		Location initLoc = tBuilder.getInitial();
 		initLoc.setCommited();
 		if(! oneChanPerMethod) {
 			tBuilder.getOutgoingAttrs(initLoc)
 			.appendUpdate(String.format("%s := %s", 
 					TemplateBuilder.LOCAL_CALL_STACK_DEPTH, 
 					SystemBuilder.CURRENT_CALL_STACK_DEPTH));
 		}
 		return NodeAutomaton.singleton(initLoc);
 	}
 	private NodeAutomaton createRootExit(DedicatedNode n) {
 		Location progExit = tBuilder.createLocation("E");
 		progExit.setCommited();
 		Location pastExit = tBuilder.createLocation("EE");
 		tBuilder.createTransition(progExit, pastExit);
 		return new NodeAutomaton(progExit,pastExit);
 	}
 	private NodeAutomaton createEntry(DedicatedNode n) {
 		Location init = tBuilder.getInitial();
 		syncBuilder.methodEntry(init);
 		return NodeAutomaton.singleton(init);
 	}
 	private NodeAutomaton createExit(DedicatedNode n) {
 		Location exit = tBuilder.createLocation("E");
 		exit.setCommited();
 		Transition t = tBuilder.createTransition(exit,tBuilder.getInitial());
 		syncBuilder.methodExit(exit,t);
 		return NodeAutomaton.singleton(exit);
 	}
 	private NodeAutomaton createSplit(DedicatedNode n) {
 		Location split = tBuilder.createLocation("SPLIT_"+n.getId());
 		split.setCommited();
 		return NodeAutomaton.singleton(split);
 	}
 	private NodeAutomaton createJoin(DedicatedNode n) {
 		Location join = tBuilder.createLocation("JOIN"+n.getId());
 		join.setCommited();
 		return NodeAutomaton.singleton(join);
 	}
 	private NodeAutomaton createBasicBlock(int nID, long blockWCET) {
 		Location bbNode = tBuilder.createLocation("N"+nID);
 		tBuilder.waitAtLocation(bbNode,blockWCET);
 		return NodeAutomaton.singleton(bbNode);		
 	}
 	private NodeAutomaton createInvoke(InvokeNode n) {
 		long blockWCET = processor.basicBlockWCET(n.getBasicBlock());
 		if(cacheSim.isAlwaysMiss()) {
 			blockWCET+= cacheImpl.getMissOnInvokeCost(processor,n.receiverFlowGraph());
 			blockWCET+= cacheImpl.getMissOnReturnCost(processor,cfg);
 		}		
 		Location basicBlockNode = createBasicBlock(n.getId(),blockWCET).getExit();
 		Location invokeNode = syncBuilder.invokeMethod(n,basicBlockNode);
 		Transition bbInvTrans = tBuilder.createTransition(basicBlockNode,invokeNode);			
 		if(UppAalConfig.isDynamicCacheSim(project.getConfig())) {
 			Location invokeMissNode = tBuilder.createLocation("CACHEI_"+n.getId());
 			Transition bbMissTrans = tBuilder.createTransition(basicBlockNode, invokeMissNode);
 			tBuilder.createTransition(invokeMissNode, invokeNode);
 			tBuilder.getIncomingAttrs(basicBlockNode).appendUpdate(
 					sys.accessCache(n.getImplementedMethod()));
 			cacheSim.onHit(bbInvTrans);
 			cacheSim.onMiss(bbMissTrans);
 			tBuilder.waitAtLocation(invokeMissNode, cacheImpl.getMissOnInvokeCost(processor,n.receiverFlowGraph()));
 		}
 		Location invokeExitNode;
 		if(UppAalConfig.isDynamicCacheSim(project.getConfig())) {
 			Location cacheAccess = tBuilder.createLocation("CACHER_"+n.getId());
 			cacheAccess.setCommited();
 			Transition invAcc = tBuilder.createTransition(invokeNode, cacheAccess);
 			invAcc.getAttrs().appendUpdate(sys.accessCache(cfg.getMethodInfo()));
 			invokeExitNode = tBuilder.createLocation("INVEXIT_"+n.getId());
 			invokeExitNode.setCommited();
 			Location returnMissNode = tBuilder.createLocation("CACHERMISS_"+n.getId());
 			Transition accExit = tBuilder.createTransition(cacheAccess, invokeExitNode);
 			Transition accMiss = tBuilder.createTransition(cacheAccess, returnMissNode);
 			/* missExit */ tBuilder.createTransition(returnMissNode, invokeExitNode);
 			cacheSim.onHit(accExit);
 			cacheSim.onMiss(accMiss);
 			tBuilder.waitAtLocation(returnMissNode, cacheImpl.getMissOnReturnCost(processor,cfg));
 		} else {
 			 invokeExitNode = invokeNode;			
 		}
 		return new NodeAutomaton(basicBlockNode,invokeExitNode);
 	}
 
 	public int getId() {
 		return mId;
 	}
 }
