 package mikera.vectorz.impl;
 
 import mikera.vectorz.AVector;
 
 public class UnmodifiableVector extends ADerivedVector {
 	private static final long serialVersionUID = 2709404707262677811L;
 
 	public UnmodifiableVector(AVector source) {
 		super(source);
 	}
 
 	@Override
 	public void set(int i, double value) {
 		throw new UnsupportedOperationException("UnmodifiableVector is immutable!");
 	}
 	
 	@Override
 	public boolean isMutable() {
 		return false;
 	}
 	
 	@Override
 	public UnmodifiableVector exactClone() {
 		return new UnmodifiableVector(source);
 	}
 }
