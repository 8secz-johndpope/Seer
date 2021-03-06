 package mikera.arrayz;
 
 import java.nio.DoubleBuffer;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 import mikera.matrixx.Matrix;
 import mikera.vectorz.AVector;
 import mikera.vectorz.IOp;
 import mikera.vectorz.Op;
 import mikera.vectorz.Tools;
 import mikera.vectorz.Vector;
 import mikera.vectorz.Vectorz;
 import mikera.vectorz.impl.ArrayIndexScalar;
 import mikera.vectorz.impl.ArraySubVector;
 import mikera.vectorz.impl.StridedArrayVector;
 import mikera.vectorz.impl.Vector0;
 import mikera.vectorz.util.IntArrays;
 import mikera.vectorz.util.VectorzException;
 
 /**
  * General purpose NDArray class
  * 
  * @author Mike
  *
  */
 public final class NDArray extends AbstractArray<INDArray> {
 
 	private final int dimensions;
 	private final int[] shape;
 	private int offset;
 	private final double[] data;
 	private int[] stride;
 	
 	private static final int[] defaultStride(int[] shape) {
 		int dimensions=shape.length;
 		int[] stride=new int[dimensions];
 		int st=1;
 		for (int j=dimensions-1; j>=0; j--) {
 			stride[j]=st;
 			st*=shape[j];
 		}
 		return stride;
 	}
 	
 	private NDArray(int... shape) {
 		this.shape=shape.clone();
 		dimensions=shape.length;
 		data=new double[(int)elementCount()];
 		stride=defaultStride(shape);
 		offset=0;
 	}
 	
 	private NDArray(double[] data, int offset, int[] shape, int[] stride) {
 		this.data=data;
 		this.offset=offset;
 		this.shape=shape;
 		this.stride=stride;
 		this.dimensions=shape.length;
 	}
 	
 	private NDArray(double[] data, int dimensions, int offset, int[] shape, int[] stride) {
 		this.data=data;
 		this.offset=offset;
 		this.shape=shape;
 		this.stride=stride;
 		this.dimensions=dimensions;
 	}
 	
 	public static NDArray wrap(double[] data, int[] shape) {
 		int dims=shape.length;
 		return new NDArray(data,dims,0,shape,defaultStride(shape));
 	}
 	
 	public static NDArray wrap(Vector v) {
 		return wrap(v.data,v.getShape());
 	}
 
 	public static NDArray wrap(Matrix m) {
 		return wrap(m.data,m.getShape());
 	}
 	
 	public static NDArray newArray(int... shape) {
 		return new NDArray(shape);
 	}
 	
 	@Override
 	public int dimensionality() {
 		return dimensions;
 	}
 
 	@Override
 	public int[] getShape() {
 		return shape;
 	}
 	
 	public int getStride(int dim) {
 		return stride[dim];
 	}
 	
 	@Override
 	public int getShape(int dim) {
 		return shape[dim];
 	}
 
 	@Override
 	public long[] getLongShape() {
 		long[] sh=new long[dimensions];
 		Tools.copyIntsToLongs(shape,sh);
 		return sh;
 	}
 
 	@Override
 	public double get() {
 		if (dimensions==0) {
 			return data[offset];
 		} else {
 			throw new UnsupportedOperationException("0-d get not possible on NDArray with dimensionality="+dimensions);
 		}
 	}
 
 	@Override
 	public double get(int x) {
 		if (dimensions==1) {
 			return data[offset+x*getStride(0)];
 		} else {
 			throw new UnsupportedOperationException("1-d get not possible on NDArray with dimensionality="+dimensions);
 		}
 	}
 
 	@Override
 	public double get(int x, int y) {
 		if (dimensions==2) {
 			return data[offset+x*getStride(0)+y*getStride(1)];
 		} else {
 			throw new UnsupportedOperationException("2-d get not possible on NDArray with dimensionality="+dimensions);
 		}
 	}
 
 	@Override
 	public double get(int... indexes) {
 		int ix=offset;
 		for (int i=0; i<dimensions; i++) {
 			ix+=indexes[i]*getStride(i);
 		}
 		return data[ix];
 	}
 
 	@Override
 	public void set(double value) {
 		if (dimensions==0) {
 			data[offset]=value;
 		} else if (dimensions==1) {
 			int n=getShape(0);
 			int st=getStride(0);
 			for (int i=0; i<n; i++) {
 				data[offset+i*st]=value;
 			}
 		} else {
 			for (INDArray s:getSlices()) {
 				s.set(value);
 			}
 		}
 	}
 
 	@Override
 	public void set(int x, double value) {
 		if (dimensions==1) {
 			data[offset+x*getStride(0)]=value;
 		} else {
 			throw new UnsupportedOperationException("1-d set not possible on NDArray with dimensionality="+dimensions);
 		}
 	}
 
 	@Override
 	public void set(int x, int y, double value) {
 		if (dimensions==2) {
 			data[offset+x*getStride(0)+y*getStride(1)]=value;
 		} else {
 			throw new UnsupportedOperationException("2-d set not possible on NDArray with dimensionality="+dimensions);
 		}
 	}
 
 	@Override
 	public void set(int[] indexes, double value) {
 		int ix=offset;
 		for (int i=0; i<dimensions; i++) {
 			ix+=indexes[i]*getStride(i);
 		}
 		data[ix]=value;
 	}
 
 	@Override
 	public void add(double a) {
 		super.add(a);
 	}
 	
 	@Override
 	public void add(INDArray a) {
 		super.add(a);
 	}
 
 	@Override
 	public void sub(INDArray a) {
 		super.sub(a);
 	}
 
 	@Override
 	public INDArray innerProduct(INDArray a) {
 		return super.innerProduct(a);
 	}
 
 	@Override
 	public INDArray outerProduct(INDArray a) {
 		return super.outerProduct(a);
 	}
 	
 	@Override
 	public INDArray getTranspose() {
 		return new NDArray(data,dimensions,offset,IntArrays.reverse(shape),IntArrays.reverse(stride));
 	}
 	
 	@Override
 	public INDArray getTransposeView() {
 		return getTranspose();
 	}
 
 	@Override
 	public AVector asVector() {
 		if (isPackedArray()) {
 			return Vector.wrap(data);
 		} else if (dimensions==0) {
 			return ArraySubVector.wrap(data,offset,1);
 		} else if (dimensions==1) {
 			return StridedArrayVector.wrap(data, offset, getShape(0), getStride(0));
 		} else {
 			AVector v=Vector0.INSTANCE;
 			int n=getShape(0);
 			for (int i=0; i<n; i++) {
 				v=v.join(slice(i).asVector());
 			}
 			return v;
 		}
 	}
 
 	private boolean isPackedArray() {
 		if (offset!=0) return false;
 		
 		int st=1;
 		for (int i=dimensions-1; i>=0; i--) {
 			if (getStride(i)!=st) return false;
 			int d=shape[i];
 			st*=d;
 		}
 			
 		return (st==data.length);
 	}
 
 	@Override
 	public INDArray reshape(int... dimensions) {
 		return super.reshape(dimensions);
 	}
 
 	@Override
 	public INDArray broadcast(int... dimensions) {
 		return super.broadcast(dimensions);
 	}
 
 	@Override
 	public INDArray slice(int majorSlice) {
 		if (dimensions==0) {
 			throw new IllegalArgumentException("Can't slice a 0-d NDArray");
 		} else if (dimensions==1) {
 			return new ArrayIndexScalar(data,offset+majorSlice*getStride(0));
 		} else if (dimensions==2) {
 			if ((majorSlice<0)||(majorSlice>shape[0])) throw new IllegalArgumentException("Slice out of range: "+majorSlice);
 			int st=stride[1];
 			if (st==1) {
 				return Vectorz.wrap(data, offset+majorSlice*getStride(0), getShape(1));
 			} else {
 				return StridedArrayVector.wrapStrided(data, offset+majorSlice*getStride(0), getShape(1), st);
 			}
 		} else {
 			return new NDArray(data,
 					offset+majorSlice*getStride(0),
 					Arrays.copyOfRange(shape, 1,dimensions),
 					Arrays.copyOfRange(stride, 1,dimensions));
 		}
 	}
 	
 	@Override
 	public INDArray slice(int dimension, int index) {
 		if ((dimension<0)||(dimension>=dimensions)) throw new IllegalArgumentException("Dimension out of range!");
 		if (dimension==0) return slice(index);
 		if (dimensions==2) {
 			if (dimension!=1) throw new IllegalArgumentException("Dimension out of range!");
 			return StridedArrayVector.wrap(data, offset+index*getStride(1), getShape(0), getStride(0));
 		}
 		return new NDArray(data,
 				offset,
 				IntArrays.removeIndex(shape,index),
 				IntArrays.removeIndex(stride,index));	
 	}	
 
 	@Override
 	public int sliceCount() {
 		if (dimensions==0) {
 			throw new IllegalArgumentException("Can't count slices o 0-d array");
 		} else {
 			return getShape(0);
 		}
 	}
 
 	@Override
 	public long elementCount() {
 		return Tools.arrayProduct(shape);
 	}
 
 	@Override
 	public boolean isMutable() {
 		return true;
 	}
 
 	@Override
 	public boolean isFullyMutable() {
 		return true;
 	}
 
 	@Override
 	public boolean isElementConstrained() {
 		return false;
 	}
 
 	@Override
 	public boolean isView() {
 		return true;
 	}
 
 	@Override
 	public void applyOp(Op op) {
 		if (dimensions==0) {
 			data[offset]=op.apply(data[offset]);
 		} else if (dimensions==1) {
 			int st=getStride(0);
 			int len=getShape(0);
 			if (st==1) {
 				op.applyTo(data, offset, len);
 			} else {
 				for (int i=0; i<len; i++) {
 					data[offset+i*st]=op.apply(data[offset+i*st]);
 				}
 			}
 		} else {
 			int n=shape[0];
 			for (int i=0; i<n; i++) {
 				slice(i).applyOp(op);
 			}		
 		}
 	}
 
 	@Override
 	public void applyOp(IOp op) {
 		applyOp((Op)op);
 	}
 	
 	public boolean equals(NDArray a) {
 		if (dimensions!=a.dimensions) return false;
 		if (dimensions==0) return get()==a.get();
		for (int i=0; i<dimensions; i++) {
 			if (!(slice(i).equals(a.slice(i)))) return false;
 		}
 		return true;
 	}
 
 	@Override
 	public boolean equals(INDArray a) {
 		if (a instanceof NDArray) {
 			return equals((NDArray)a);
 		}
 		if (dimensions!=a.dimensionality()) return false;
 		if (dimensions==0) return (get()==a.get());
 		
		for (int i=0; i<dimensions; i++) {
 			if (!(slice(i).equals(a.slice(i)))) return false;
 		}
 		return true;
 	}
 
 	@Override
 	public NDArray exactClone() {
 		NDArray c=new NDArray(data.clone(),offset,shape.clone(),stride.clone());
 		return c;
 	}
 	
 	@Override
 	public NDArray clone() {
 		NDArray c=new NDArray(shape.clone());
 		c.set(this);
 		return c;
 	}
 
 	@Override
 	public void multiply(double d) {
 		if (dimensions==0) {
 			data[offset]*=d;
 		} else if (dimensions==1) {
 			int n=getShape(0);
 			for (int i=0; i<n; i++) {
 				data[offset+i*getStride(0)]*=d;
 			}
 		} else {
 			int n=getShape(0);
 			for (int i=0; i<n; i++) {
 				slice(i).scale(d);
 			}
 		}
 	}
 
 	@Override
 	public void setElements(double[] values, int offset, int length) {
 		if (dimensions==0) {
 			data[this.offset]=values[offset];
 		} else if (dimensions==1) {
 			if (length>getShape(0)) throw new IllegalArgumentException("Too many values for NDArray: "+length);
 			int st0=getStride(0);
 			for (int i=0; i<length; i++) {
 				data[this.offset+i*st0]=values[offset+i];
 			}
 		} else {
 			int sc=getShape(0);
 			int ssize=(int) Tools.arrayProduct(shape,1,dimensions);
 			for (int i=0; i<sc; i++) {
 				slice(i).setElements(values,offset+ssize*i,ssize);
 			}
 		}
 	}
 	
 	@Override
 	public void toDoubleBuffer(DoubleBuffer dest) {
 		if (dimensions==0) {
 			dest.put(data[offset]);
 		} else if (isPackedArray()) {
 			dest.put(data,0,data.length);
 		} else {
 			int sc=sliceCount();
 			for (int i=0; i<sc; i++) {
 				INDArray s=slice(i);
 				s.toDoubleBuffer(dest);
 			}
 		}
 	}
 
 
 	@Override
 	public List<INDArray> getSlices() {
 		if (dimensions==0) {
 			throw new IllegalArgumentException("Can't get slices of 0-d NDArray");
 		} else {
 			ArrayList<INDArray> al=new ArrayList<INDArray>();
 			int n=getShape(0);
 			for (int i=0; i<n; i++) {
 				al.add(slice(i));
 			}
 			return al;
 		}
 	}
 	
 	@Override public void validate() {
 		if (dimensions>shape.length) throw new VectorzException("Insufficient shape data");
 		if (dimensions>stride.length) throw new VectorzException("Insufficient stride data");
 		
 		if ((offset<0)||(offset>=data.length)) throw new VectorzException("Offset out of bounds");
 		super.validate();
 	}
 }
