 package mikera.vectorz;
 
 /**
  * Base class for vectors backed by a double[] array.
  * 
  * The double array can be directly accessed for performance purposes
  * 
  * @author Mike
  */
 @SuppressWarnings("serial")
 public abstract class ArrayVector extends AVector {
 
 	public abstract double[] getArray();
 	
 	public abstract int getArrayOffset();
 
 	/**
 	 * Returns a vector referencing a sub-vector of the current vector
 	 * 
 	 * @param offset
 	 * @param length
 	 * @return
 	 */
 	public ArraySubVector subVector(int offset, int length) {
 		int len=this.length();
 		if ((offset + length) > len)
 			throw new IndexOutOfBoundsException("Upper bound " + len
 					+ " breached:" + (offset + length));
 		if (offset < 0)
 			throw new IndexOutOfBoundsException("Lower bound breached:"
 					+ offset);
 		return new ArraySubVector(this, offset, length);
 	}
 	
 	@Override
 	public void copyTo(double[] data, int offset) {
 		System.arraycopy(getArray(), getArrayOffset(), data, offset, length());
 	}
 	
 	public void add(ArrayVector v) {
 		int vlength=v.length();
 		int length=length();
 		if (vlength != length) {
 			throw new Error("Source vector has different size: " + vlength);
 		}
 		double[] vdata=v.getArray();
 		double[] data=getArray();
 		int offset=getArrayOffset();
 		int voffset=v.getArrayOffset();
 		for (int i = 0; i < length; i++) {
 			data[offset+i] += vdata[voffset + i];
 		}
 	}
 	
 	@Override
 	public void copy(int start, int length, AVector dest, int destOffset) {
 		if (dest instanceof ArrayVector) {
 			copy(start,length,(ArrayVector)dest,destOffset);
			return;
 		}
 		double[] src=getArray();
 		int off=getArrayOffset();
 		for (int i = 0; i < length; i++) {
 			dest.set(destOffset+i,src[off+start+i]);
 		}
 	}
 	
 	public void copy(int start, int length, ArrayVector dest, int destOffset) {
 		double[] src=getArray();
 		int off=getArrayOffset();
 		double[] dst=dest.getArray();
 		System.arraycopy(src, off+start, dst, destOffset, length);
 	}
 	
 	public void addMultiple(ArrayVector v, double factor) {
 		int vlength=v.length();
 		int length=length();
 		if (vlength != length) {
 			throw new Error("Source vector has different size: " + vlength);
 		}
 		double[] data=getArray();
 		int offset=getArrayOffset();
 		double[] vdata=v.getArray();
 		int voffset=v.getArrayOffset();
 		for (int i = 0; i < length; i++) {
 			data[offset+i] += vdata[voffset + i]*factor;
 		}
 	}
 }
