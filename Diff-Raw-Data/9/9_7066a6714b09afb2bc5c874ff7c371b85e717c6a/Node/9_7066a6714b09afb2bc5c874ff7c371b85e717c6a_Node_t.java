 package org.eclipse.imp.pdb.facts.impl.hash;
 
 import org.eclipse.imp.pdb.facts.INode;
 import org.eclipse.imp.pdb.facts.ITree;
 import org.eclipse.imp.pdb.facts.IValue;
 import org.eclipse.imp.pdb.facts.type.TreeNodeType;
 import org.eclipse.imp.pdb.facts.type.TupleType;
 import org.eclipse.imp.pdb.facts.type.Type;
 import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
 import org.eclipse.imp.pdb.facts.visitors.VisitorException;
 
 /**
  * Implementation of a typed tree node with access to children via labels
  */
 public class Node extends Tree implements INode {
 	
 	/*package*/ Node(TreeNodeType type, IValue[] children) {
 		super(type.getName(), children);
 		fType = type;
 	}
 	
 	/*package*/ Node(TreeNodeType type) {
 		this(type, new IValue[0]);
 	}
 
 	private Node(Node node, String label, IValue anno) {
 		super(node, label, anno);
 		fType = node.fType;
 	}
 
 	private Node(Node other, int childIndex, IValue newChild) {
 		super(other, childIndex, newChild);
 		fType = other.fType;
 	}
 
 	protected final TreeNodeType fType;
 
 	public IValue get(String label) {
 		return super.get(((TreeNodeType) fType).getChildIndex(label));
 	}
 
 	public TupleType getChildrenTypes() {
 		return ((TreeNodeType) fType).getChildrenTypes();
 	}
 
 	public TreeNodeType getTreeNodeType() {
 		return fType;
 	}
 
 	@Override
 	public Type getType() {
 		return fType.getSuperType();
 	}
 
 	@Override
 	public ITree set(int i, IValue newChild) {
 		return new Node(this, i, newChild);
 	}
 	
	public INode set(String label, IValue newChild) {
 		return new Node(this, ((TreeNodeType) fType).getChildIndex(label), newChild);
 	}
 	
 	@Override
 	public boolean equals(Object obj) {
 		if (obj instanceof Node) {
 		  return fType == ((Node) obj).fType && super.equals(obj);
 		}
 		return false;
 	}
 	
 	@Override
 	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
 		return v.visitNode(this);
 	}
 	
 	@Override
 	protected IValue clone(String label, IValue anno) {
 		return new Node(this, label, anno);
 	}
 }
