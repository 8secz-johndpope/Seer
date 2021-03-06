 package org.rice.crosby.historytree;
 
 
 import org.rice.crosby.historytree.generated.Serialization;
 import org.rice.crosby.historytree.generated.Serialization.HistTree;
 
 import com.google.protobuf.InvalidProtocolBufferException;
 
 
 
 public class HistoryTree<A,V> {
 
 
 	/** Make an empty history tree with a given aggobj and datastore.  */
 	public HistoryTree(AggregationInterface<A,V> aggobj,
 	    		   HistoryDataStoreInterface<A,V> datastore) {
 	    this.time = -1;
 		this.root = null;
 		this.aggobj = aggobj;
 		this.datastore = datastore;
 	}
 
 	/** Make an history at a given timestamp (used as a template for building a pruned trees or parsing trees.)
 	 */
 	public HistoryTree<A,V> updateTime(int time) {
 		this.time = time;
 		datastore.updateTime(time);
 		return this;
 	}
 	
 	//
 	// Operations for adding to a log and getting the commitment
 	//
 	
 	
 	/** Add an event to the log */
 	public void append(V val) {
 		NodeCursor<A,V> leaf;
 		if (time < 0) {
 			time = 0;
 			datastore.updateTime(time);
 			root = leaf = datastore.makeRoot(0);
 		} else {
 			time = time+1;
 			datastore.updateTime(time);
 			reparent(time);
 			leaf = forceLeaf(time);
 		}
 		leaf.setVal(val);
 		computefrozenaggs(leaf);
 	}
 		
 	private void reparent(int time) {
 		while (!(time <= (1<<root.layer)-1))
 			this.root = root.reparent();
 	}
 
 	/** Compute any frozen aggregates on a node */
 	private void computeAggOnNode(NodeCursor<A,V> node) {
 		if (node.isLeaf()) {
 			if (node.hasVal() && node.getAgg() == null) 
 				node.setAgg(aggobj.aggVal(node.getVal()));
 		} else {
 			node.setAgg(aggobj.aggChildren(node.left().getAgg(),node.right().getAgg()));
 		}
 	}
 	
 	/** Recurse from the leaf upwards, computing the agg for all frozen nodes */
     private void computefrozenaggs(NodeCursor<A,V> leaf) {
     	// First, set the leaf agg from the stored event (if it exists
     	if (leaf.hasVal() && leaf.getAgg() == null) {
     		leaf.markValid();
     		leaf.setAgg(aggobj.aggVal(leaf.getVal()));
     	}
     	NodeCursor<A,V> node=leaf.getParent(root);
     	while (node != null && node.isFrozen(time) && node.getAgg() == null) {
     		node.setAgg(aggobj.aggChildren(node.left().getAgg(),node.right().getAgg()));
     		node = node.getParent(root);
     	}
     }
     
     public AggregationInterface<A,V> getAggObj() {
     	return aggobj.clone();
     }
     
     public int version() {
     	return time;
     }
     
     public A agg() {
     	return aggV(time);
     }
     public A aggV(int version) {
    	assert (version <= time);
     	NodeCursor<A,V>  child, leaf, node;
 
     	child = leaf = this.leaf(version);
     	node= leaf.getParent(root);
     	A agg = leaf.getAgg();
 
     	while (node!=null && version >= (1<<node.layer-1)) {
     		NodeCursor<A,V>  left = node.left();
     		if (child.equals(left))
     			agg = aggobj.aggChildren(agg,null);
    		else {
    			A leftagg = left.getAgg(); assert leftagg != null;
    			agg = aggobj.aggChildren(leftagg,agg);
    		}
     		child = node;
     		node = node.getParent(root);
     	}
     	return agg;
     }
     
     //
     //  Operations for making pruned trees.
     //
     
 	/** Make a cursor pointing to the given leaf, if possible */
     private NodeCursor<A,V> leaf(int version)  {
     	if (time == 0)
     		return root;
     	NodeCursor<A,V> node=root,child;
     	for (int layer = log2(time) ; layer >= 0 ; layer--) {
     		//System.out.println("leaf"+node);
     		int mask = 1 << (layer-1);
     		if ((mask & version) == mask)
     			child = node.right();
     		else
     			child = node.left();
     		if (layer == 1)
     			return child;
     		node = child;
     	}
     	assert false;
     	return null;
     }
     /** Make a cursor pointing to the given leaf, forcibly creating the path if possible */
     private NodeCursor<A,V> forceLeaf(int version) {
     	if (time == 0)
    		return root.markValid();
     	NodeCursor<A,V> node=root,child;
     	for (int layer = log2(time) ; layer >= 0 ; layer--) {
     		//System.out.println("forceleaf"+node);
     		int mask = 1 << (layer-1);
     		if ((mask & version) == mask)
     			child = node.forceRight();
     		else
     			child = node.forceLeft();
     		if (layer == 1)
     			return child;
     		node = child;
     	}
     	assert false;
     	return null;
     }
 
     public HistoryTree<A,V> makePruned(HistoryDataStoreInterface<A, V> newdatastore) {
     	HistoryTree<A,V> out = new HistoryTree<A,V>(this.aggobj,newdatastore);
     	out.updateTime(this.time);
     	out.copyRoot(this);
     	out._copyAgg(this,this.leaf(time),out.forceLeaf(time),true);
     	return out;
         }
     
     private void copyRoot(HistoryTree<A,V> orig) {
     	assert this.root == null;
     	root = datastore.makeRoot(orig.root.layer);
     	//if (root.isFrozen(time))
     	//	root.copyAgg(orig.root);
     }
 
     /** Make a path to one leaf and copy over its value or agg. 
      * @throws ProofError */
     private NodeCursor<A,V> copyVersionHelper(HistoryTree<A,V> orig, int version, boolean copyValFlag) throws ProofError {
     	NodeCursor<A,V> origleaf, selfleaf;
     	selfleaf = forceLeaf(version);
     	origleaf = orig.leaf(version);
     	
     	if (!origleaf.isAggValid())
     		throw new ProofError("Leaf not in the tree");    	
     	selfleaf.copyAgg(origleaf);
     	// If we want a value, have one to copy from, and don't have one already... Copy it.
     	if (copyValFlag && !selfleaf.hasVal() && origleaf.hasVal())
     		selfleaf.copyVal(origleaf);
     	return selfleaf;
     	}
 
     
     private void _copyAgg(HistoryTree<A,V> orig, NodeCursor<A,V> origleaf,NodeCursor<A,V> leaf, boolean force) {
		assert(orig.time == this.time); // Except for concurrent copies&updates, time shouldn't change.
     	NodeCursor<A,V> node,orignode;
     	orignode = origleaf.getParent(orig.root);
     	node = leaf.getParent(root);
 
     	boolean continuing = true;
     	// Invariant: We have a well-formed tree with all stubs include hashes EXCEPT possibly siblings in the path from the leaf to where it merged into the existing pruned tree.
    	    // Iterate up the tree, copying over sibling agg's for stubs. If we hit a node with two siblings. we're done. Earlier inserts will have already inserted sibling hashes for ancestor nodes.
     	while (continuing && node != null) {
     		//System.out.println("CA: "+orignode+" --> "+node);
     		// FIX: THE INITAL TREE VIOLATES THE INVARIANTS.
     		if (!force && node.left() != null && node.right() != null)
     			continuing = false;
     		NodeCursor<A,V> origleft,origright;
     		//System.out.println("NO BREAK");
     		origleft = orignode.left();
     		//System.out.println("CL: "+origleft+" --> "+node.forceLeft());
     		if (origleft.isFrozen(this.time))
     			node.forceLeft().copyAgg(origleft);
     		
     		// A right node may or may not exist.
     		origright = orignode.right();
     		//System.out.println("RIGHT:"+origright+"  "+time); 
     		if (origright!= null && origright.isFrozen(time))
     				node.forceRight().copyAgg(origright);
 
     		//System.out.println("LOOP");
  		
     		orignode = orignode.getParent(orig.root);
     		node = node.getParent(root);
     	}
     }    
     public void copyV(HistoryTree<A,V> orig, int version, boolean copyValueFlag) throws ProofError {
     	if (root == null) {
     		copyRoot(orig);
     		//copyVersionHelper(orig,this.time,false);
     	}
     		
     	NodeCursor<A,V> origleaf, selfleaf;
     	selfleaf = forceLeaf(version);
     	origleaf = orig.leaf(version);
 
     	selfleaf.equals(root);
     	origleaf.equals(orig);
     	
    		//System.out.println("copyV"+selfleaf+"  "+origleaf);
    	    	
     	assert origleaf.getAgg() != null;
     	if (selfleaf.isAggValid() && selfleaf.getAgg() != null) {
     		// If the leaf is already in the tree...
     		assert selfleaf.getAgg().equals(origleaf.getAgg());
     	} else {
     		copyVersionHelper(orig,version,copyValueFlag);
     		_copyAgg(orig,origleaf,selfleaf,false);
     	}    			
     	if (copyValueFlag) {
     		if (!origleaf.hasVal())
     			throw new ProofError("Missing value in proof");
     	} else {
     		if (!selfleaf.hasVal() && origleaf.hasVal()) 
     			selfleaf.copyVal(origleaf);
     	}
     
     }
     
     
 
     //
     //  TODO: Serialization code
     //
     /*static HistoryTree makeFromConfig(Serialization.Config config) {
     	return null;
     }*/
     
     /** Serialize a pruned tree to a protocol buffer */
     public void serializeTree(Serialization.HistTree.Builder out) {
     	out.setVersion(time);
     	if (root != null) {
     		Serialization.HistNode.Builder builder = Serialization.HistNode.newBuilder();
     		serializeNode(builder,root);
     		out.setRoot(builder.build());
     	}
     }
     
     public byte[] serializeTree() {
     	Serialization.HistTree.Builder builder= Serialization.HistTree.newBuilder();
     	serializeTree(builder);
     	return builder.build().toByteArray();
     }
 
     /** Parse from a protocol buffer. I assume that the history tree has 
      * been configured with the right aggobj and a datastore. */
     public void parseTree(Serialization.HistTree in) {
     	this.time = in.getVersion();
     	if (in.hasRoot()) {
     		root = datastore.makeRoot(log2(in.getVersion()));
     		parseNode(root,in.getRoot());    		
     	}
     }
 
     public void parseTree(byte data[]) throws InvalidProtocolBufferException {
 		parseTree(HistTree.parseFrom(data));
     }
     
     /** Helper function for recursively serializing a history tree */
     private void serializeNode(Serialization.HistNode.Builder out, NodeCursor<A,V> node) {
     	if (node.isLeaf()) {
     		//System.out.println("SN:"+node);
     		if (node.hasVal())
     			out.setVal(aggobj.serializeVal(node.getVal()));
     		else
     			out.setAgg(aggobj.serializeAgg(node.getAgg()));
     		return;
     	}
     	if (node.left() == null && node.right() == null) {
     		// Either a stub or a leaf. 
     		// Gotta include the agg for this node.
     		out.setAgg(aggobj.serializeAgg(node.getAgg()));
     		return;
     	}
     	// Ok, recurse both sides. Don't forget, we need to make a builder.
     	if (node.left() != null) {
     		Serialization.HistNode.Builder b = Serialization.HistNode.newBuilder();
     		serializeNode(b,node.left());
     		out.setLeft(b.build());
     	}
     	if (node.right() != null) {
     		Serialization.HistNode.Builder b = Serialization.HistNode.newBuilder();
     		serializeNode(b,node.right());
     		out.setRight(b.build());
     	}
     }
     
     private void parseNode(NodeCursor<A,V> node, Serialization.HistNode in) {
     	if (in.hasVal()) {
     		V val = aggobj.parseVal(in.getVal());
     		node.setVal(val);
     		node.setAgg(aggobj.aggVal(val));
     		return;
     	}
     	if (in.hasAgg()) {
     		// If it has an agg, it should be a stub or a leaf stub.
     		assert !in.hasLeft();
     		assert !in.hasRight();
     		A agg = aggobj.parseAgg(in.getAgg());
     		node.setAgg(agg);
     		return;
     	}
     	// Must always have a left and right child.
     	assert in.hasLeft();
     	parseNode(node.forceLeft(),in.getLeft());
 
     	if (in.hasRight()) {
     		parseNode(node.forceRight(),in.getRight());
     		if (node.isFrozen(time))
     			node.setAgg(aggobj.aggChildren(node.left().getAgg(),node.right().getAgg()));
     	}
     }
     
     public String toString(String prefix) {
     	StringBuilder b = new StringBuilder();
     	b.append(prefix);
     	b.append("  version = ");
     	b.append(time);
     	b.append("\n");
     	debugString(b,prefix,root);
     	return new String(b);
     }
     public String toString() {
     	return toString("");
     }
 	public void debugString(StringBuilder b, String prefix, NodeCursor<A,V> node) {
 		b.append(prefix);
 		b.append("\t"+toString(node));
 		b.append("\n");
 
 		if (node.isLeaf())
 			return;
 			
 		NodeCursor<A,V> left=node.left(), right=node.right();
 		
 		if (left != null)
 			debugString(b,prefix+"L",left);
 		if (right != null) 
 			debugString(b,prefix+"R",right);
 	}
 
 	/** Return this as a longer tab-delimited string */
 	public String toString(NodeCursor<A,V> node) {
 		StringBuilder b=new StringBuilder();
 		b.append(String.format("<%d,%d>",node.layer,node.index));
 		b.append("\t");
 		// Print out the value (if any)
 		
 		if (node.isLeaf() && node.hasVal())
 			b.append(valToString(node.getVal()));
 			else
 				b.append("<>");
 				
 		b.append("\t");	
 
 		if (node.isAggValid()) {
 			A agg = node.getAgg();
 			if (agg == null)
 				b.append("Null");
 			else
 				b.append(aggToString(agg));
 		} else
 			b.append("<>");
 		//b.append(":");
 		//b.append(datastore.hashCode());
 		return b.toString();
 	}
 	
     protected String aggToString(A a) {
     	return aggobj.serializeAgg(a).toStringUtf8();
     }
     protected String valToString(V v) {
     	return aggobj.serializeVal(v).toStringUtf8();
     }
  
     /* Merge another pruned tree in with this tree.
      * 
      * Assume both merged trees have been confirmed to be consistent.
      * 
      */
     
     public void mergeTree(HistoryTree<A,V> peer) {
     	NodeCursor <A,V> thisroot, peerroot;
 
     	if (peer.version() <0) {
     		// Nothing to merge.
     		return;
     	}
 
     	if (this.version() <0 ) {
     		// Need to make a root node to act as the destination
     		this.root = new NodeCursor<A,V>(this.datastore,log2(peer.version()),0);
     		assert log2(peer.version()) == peer.root.layer;
     	}
     	
     	
     	thisroot = this.root;
     	peerroot = peer.root;
 
     	// Get two new roots on the same layer before merging.
     	if (this.version() < peer.version()) {
     		while (peerroot.layer > this.root.layer)
     			this.root = this.root.reparent();
     		this.updateTime(peer.version());
     		thisroot = this.root;
     	} else {
     		while (thisroot.layer > peerroot.layer)
     			thisroot=thisroot.left();
     	}
 
     	mergeNode(peer,thisroot,peerroot);
      }
 
 
     private void mergeNode(HistoryTree<A,V> peer, NodeCursor<A,V> thisnode, NodeCursor<A,V> peernode) {
     	/*
     	 * 
     	 *  Invariant: The thisnode and peernode are always 'valid'
     	 */
     	assert thisnode.isAggValid();
     	assert peernode.isAggValid();
 
     	if (peernode.isLeaf()) {
     		if (peernode.hasVal()) 
     			thisnode.copyVal(peernode);
     		return;
     	}
     	if (peernode.isStub()) {
     		if (thisnode.getAgg() == null) {
     			peernode.copyAgg(thisnode);
     		}
     		return;
     	}
 
     	// OK, now recurse the left subtrees.
     	mergeNode(peer,thisnode.forceLeft(),peernode.left());
 
     	// Do we recurse the right trees? The peer is not a stub.
     	if (thisnode.right().index <= version() && peernode.right().index <= peer.version()) {
     		// Both have valid right children for this snapshot version.
     		mergeNode(peer,thisnode.forceRight(),peernode.right());
     		if (thisnode.isFrozen(time) && thisnode.getAgg() == null)
     			computeAggOnNode(thisnode);
     	}
     }
 
 
     
     //
     //  Member fields
     //    
     private int time;
     private NodeCursor<A,V> root;
     private HistoryDataStoreInterface<A,V> datastore;
     private AggregationInterface<A,V> aggobj;
 
     // Misc helpers
     public static int log2(int x) {
     	int i = 0, pow = 1;
     	while (pow <= x) {
     		pow = pow*2;
     		i=i+1;
     	}
     	return i;
     }
 }
