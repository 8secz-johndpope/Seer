 package mikera.arrayz;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 import mikera.vectorz.AVector;
 import mikera.vectorz.IOp;
 import mikera.vectorz.Op;
 import mikera.vectorz.Tools;
 import mikera.vectorz.Vector;
import mikera.vectorz.impl.ArraySubVector;
 import mikera.vectorz.impl.StridedArrayVector;
 import mikera.vectorz.impl.Vector0;
 
 public class NDArray extends AbstractArray<Double> {
 
 	private final int dimensions;
 	private final int[] shape;
 	private int offset;
 	private final double[] data;
 	private int[] stride;
 	
 	public NDArray(int... shape) {
 		this.shape=shape.clone();
 		dimensions=shape.length;
 		data=new double[(int)elementCount()];
		stride=new int[dimensions];
 		offset=0;
 		
 		int st=1;
 		for (int j=dimensions-1; j>=0; j--) {
 			stride[j]=st;
 			st*=shape[j];
 		}
 	}
 	
 	public NDArray(double[] data, int offset, int[] shape, int[] stride) {
 		this.data=data;
 		this.offset=offset;
 		this.shape=shape;
 		this.stride=stride;
 		this.dimensions=shape.length;
 	}
 	
 	@Override
 	public int dimensionality() {
 		return dimensions;
 	}
 
 	@Override
 	public int[] getShape() {
 		return shape;
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
 			return data[offset+x*stride[0]];
 		} else {
 			throw new UnsupportedOperationException("1-d get not possible on NDArray with dimensionality="+dimensions);
 		}
 	}
 
 	@Override
 	public double get(int x, int y) {
 		if (dimensions==1) {
 			return data[offset+x*stride[0]+y*stride[1]];
 		} else {
 			throw new UnsupportedOperationException("2-d get not possible on NDArray with dimensionality="+dimensions);
 		}
 	}
 
 	@Override
 	public double get(int... indexes) {
 		int ix=offset;
 		for (int i=0; i<dimensions; i++) {
 			ix+=indexes[i]*stride[i];
 		}
 		return data[ix];
 	}
 
 	@Override
 	public void set(double value) {
 		if (dimensions==0) {
 			data[offset]=value;
 		} else {
			for (INDArray s:getSlices()) {
				s.set(value);
			}
 		}
 	}
 
 	@Override
 	public void set(int x, double value) {
 		if (dimensions==1) {
 			data[offset+x*stride[0]]=value;
 		} else {
 			throw new UnsupportedOperationException("1-d set not possible on NDArray with dimensionality="+dimensions);
 		}
 	}
 
 	@Override
 	public void set(int x, int y, double value) {
 		if (dimensions==2) {
 			data[offset+x*stride[0]+y*stride[1]]=value;
 		} else {
 			throw new UnsupportedOperationException("2-d set not possible on NDArray with dimensionality="+dimensions);
 		}
 	}
 
 	@Override
 	public void set(int[] indexes, double value) {
 		int ix=offset;
 		for (int i=0; i<dimensions; i++) {
 			ix+=indexes[i]*stride[i];
 		}
 		data[ix]=value;
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
 	public AVector asVector() {
 		if (fittedDataArray()) {
 			return Vector.wrap(data);
		} else if (dimensions==0) {
			return ArraySubVector.wrap(data,offset,1);
 		} else if (dimensions==1) {
 			return StridedArrayVector.wrap(data, offset, shape[0], stride[0]);
 		} else {
 			AVector v=Vector0.INSTANCE;
 			for (int i=0; i<shape[0]; i++) {
 				v=v.join(slice(i).asVector());
 			}
 			return v;
 		}
 	}
 
 	private boolean fittedDataArray() {
 		if (offset!=0) return false;
 		
 		int st=1;
 		for (int i=dimensions-1; i>=0; i--) {
 			if (stride[i]!=st) return false;
 			int d=shape[i];
 			st*=d;
 		}
 			
		return st==data.length;
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
 		}
 		return new NDArray(data,
 				offset+majorSlice*stride[0],
 				Arrays.copyOfRange(shape, 1,dimensions),
 				Arrays.copyOfRange(stride, 1,dimensions));
 	}
 
 	@Override
 	public int sliceCount() {
 		return shape[0];
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
 			int st=stride[0];
 			int len=shape[0];
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
 	public void scale(double d) {
 		if (dimensions==0) {
 			data[offset]*=d;
 		} else if (dimensions==1) {
 			for (int i=0; i<shape[0]; i++) {
 				data[offset+i*stride[0]]*=d;
 			}
 		} else {
 			for (int i=0; i<shape[0]; i++) {
 				slice(i).scale(d);
 			}
 		}
 	}
 
 	@Override
 	public void setElements(double[] values, int offset, int length) {
 		if (dimensions==0) {
 			data[this.offset]=values[offset];
 		} else if (dimensions==1) {
 			for (int i=0; i<shape[0]; i++) {
				data[this.offset+i*stride[0]]=values[offset+i];
 			}
 		} else {
 			int sc=shape[0];
 			int ssize=(int) Tools.arrayProduct(shape,1,dimensions);
 			for (int i=0; i<sc; i++) {
 				slice(i).setElements(values,offset+ssize*i,ssize);
 			}
 		}
 	}
 
 	@Override
 	public List<INDArray> getSlices() {
 		if (dimensions==0) {
 			throw new IllegalArgumentException("Can't get slices of 0-d NDArray");
 		} else {
 			ArrayList<INDArray> al=new ArrayList<INDArray>();
 			for (int i=0; i<shape[0]; i++) {
 				al.add(slice(i));
 			}
 			return al;
 		}
 	}
 }
