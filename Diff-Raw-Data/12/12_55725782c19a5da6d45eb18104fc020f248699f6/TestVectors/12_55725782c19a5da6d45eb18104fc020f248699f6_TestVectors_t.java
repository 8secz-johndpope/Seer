 package mikera.vectorz;
 
 import static org.junit.Assert.*;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 import mikera.arrayz.INDArray;
 import mikera.arrayz.TestArrays;
 import mikera.indexz.Index;
 import mikera.matrixx.AMatrix;
 import mikera.matrixx.Matrixx;
 import mikera.vectorz.impl.ArraySubVector;
 import mikera.vectorz.impl.AxisVector;
 import mikera.vectorz.impl.DoubleScalar;
 import mikera.vectorz.impl.RepeatedElementVector;
 import mikera.vectorz.impl.IndexedArrayVector;
 import mikera.vectorz.impl.IndexedSubVector;
 import mikera.vectorz.impl.JoinedArrayVector;
 import mikera.vectorz.impl.SingleElementVector;
 import mikera.vectorz.impl.SparseIndexedVector;
import mikera.vectorz.impl.StridedArrayVector;
 import mikera.vectorz.impl.Vector0;
 import mikera.vectorz.impl.WrappedSubVector;
 import mikera.vectorz.ops.Constant;
 
 import org.junit.Test;
 
 
 public class TestVectors {
 	@Test public void testDistances() {
 		Vector3 v=Vector3.of(1,2,3);
 		Vector3 v2=Vector3.of(2,4,6);
 		assertEquals(v.magnitude(),v.distance(v2),0.000001);
 		assertEquals(6,v.distanceL1(v2),0.000001);
 		assertEquals(3,v.distanceLinf(v2),0.000001);
 	}
 	
 	public void testDistances(AVector v) {
 		AVector zero=Vectorz.newVector(v.length());
 		assertEquals(v.maxAbsElement(),v.distanceLinf(zero),0.0);
 		
 		if (!v.isFullyMutable()) return;
 		AVector z=v.exactClone();
 		z.fill(0.0);
 		assertEquals(v.maxAbsElement(),v.distanceLinf(z),0.0);
 	}
 	
 	public void testMagnitudes(AVector v) {
 		double d=v.magnitude();
 		double d2=v.magnitudeSquared();
 		assertEquals(d*d,d2,0.00001);
 		
 		assertTrue(d<=(v.maxAbsElement()*v.length()));
 	}
 	
 	public void testSquare(AVector v) {
 		v=v.exactClone();
 		AVector vc=v.clone();
 		v.square();
 		vc.square();
 		assertEquals(vc,v);
 	}
 	
 	@Test public void testCross() {
 		Vector3 v=Vector3.of(1,2,3);
 		v.crossProduct(Vector.of(1,1,1));
 		assertEquals(Vector3.of(-1,2,-1),v);
 	}
 	
 	@Test public void testClamping() {
 		Vector3 v=Vector3.of(1,2,3);
 		v.clamp(1.5, 2.5);
 		assertEquals(Vector3.of(1.5,2,2.5),v);
 	}
 	
 	@Test public void testClampMin() {
 		Vector3 v=Vector3.of(1,2,3);
 		v.clampMin(1.5);
 		assertEquals(Vector3.of(1.5,2,3),v);
 	}
 	
 	@Test public void testClampMax() {
 		Vector3 v=Vector3.of(1,2,3);
 		v.clampMax(2.5);
 		assertEquals(Vector3.of(1,2,2.5),v);
 	}
 	
 	@Test public void testElementSum() {
 		Vector3 v=Vector3.of(1,2,3);
 		assertEquals(6.0,v.elementSum(),0.0);
 	}
 	
 	
 	@Test public void testCreateFromIterable() {
 		ArrayList<Object> al=new ArrayList<Object>();
 		al.add(1);
 		al.add(2L);
 		al.add(3.0);
 		AVector v=Vectorz.create((Iterable<Object>)al);
 		assertEquals(Vector.of(1,2,3),v);
 	}
 	
 	@Test public void testSubVectors() {
 		double[] data=new double[100];
 		for (int i=0; i<100; i++) data[i]=i;
 		
 		ArraySubVector v=new ArraySubVector(data);
 		assertEquals(10,v.get(10),0.0);
 		assertTrue(v.isView());
 		
 		ArraySubVector v2=v.subVector(5, 90);
 		assertEquals(90,v2.length());
 		assertEquals(15,v2.get(10),0.0);
 		assertTrue(v2.isView());
 		
 		ArraySubVector v3=v2.subVector(5,80);
 		assertEquals(20,v3.get(10),0.0);
 		assertTrue(v3.isView());
 		
 		v3.set(10, -99);
 		assertEquals(-99,v.get(20),0.0);
 	}
 	
 	@Test public void testWrap() {
 		double[] data=new double[]{1,2,3};
 		
 		Vector v=Vector.wrap(data);
 		data[0]=13;
 		assertEquals(13,v.get(0),0.0);
 	}
 	
 	@Test public void testSubVectorCopy() {
 		double[] data=new double[100];
 		for (int i=0; i<100; i++) data[i]=i;
 		ArraySubVector v=new ArraySubVector(data);	
 		
 		assertEquals(Arrays.hashCode(data),v.hashCode());
 		
 		ArraySubVector v2=new ArraySubVector(v);
 		assertEquals(v,v2);
 		assertTrue(v2.isView());
 		
 		v.set(10,Math.PI);
 		assertTrue(!v.equals(v2));
 	}
 	
 	@Test public void testString() {
 		assertEquals("[0.0,0.0]",new Vector2().toString());
 		assertEquals("[1.0,2.0]",Vectorz.create(1,2).toString());
 	}
 	
 	private void testParse(AVector v) {
 		String str = v.toString();
 		AVector v2=Vectorz.parse(str);
 		if (v.length()>0) {
 			assertEquals(v.get(0),v2.get(0),0.0);			
 		}
 		assertEquals(v,v2);
 	}
 	
 	private void testHashCode(AVector v) {
 		assertEquals(v.hashCode(),v.toList().hashCode());
 	}
 	
 	private void testAddToArray(AVector v) {
 		int len=v.length();
 		double[] ds=new double[len+10];
 		if (len>=3) {
 			v.addToArray(2, ds, 5, len-2);
 			assertEquals(v.get(2),ds[5],0.000);
 			assertEquals(0.0,ds[5+len],0.0);
 		}
 		
 		v.addToArray(0, ds, 0, len);
 		if (len>0) {
 			assertEquals(ds[0],v.get(0),0.0);
 		}
 	}
 	
 	private void testAddProduct(AVector v) {
 		int len=v.length();
 		if (!v.isFullyMutable()) return;
 		
 		v=v.exactClone();
 		AVector v2=v.exactClone();
 		AVector vc=new Vector(v);
 		
 		AVector p1=Vectorz.createUniformRandomVector(len);
 		AVector p2=Vectorz.createUniformRandomVector(len);
 		
 		v.addProduct(p1, p2,3.0);
 		vc.addProduct(p1, p2, 3.0);
 		for (int i=0; i<3; i++) v2.addProduct(p1, p2);
 		
 		assertTrue(v.epsilonEquals(vc));
 		assertTrue(v.epsilonEquals(v2));
 	}
 	
 	private void testInnerProducts(AVector v) {
 		int len=v.length();
 		AVector c=Vectorz.createUniformRandomVector(v.length());
 		assertEquals(v.dotProduct(c),v.innerProduct((INDArray)c).get(),0.00001);
 		
 		if (len>20) return;
 		
 		AMatrix m=Matrixx.createRandomMatrix(len, len);
 		assertEquals(v.innerProduct(m),m.getTranspose().transform(v));
 	}
 	
 	private void testAddMultipleToArray(AVector v) {
 		int len=v.length();
 		double[] ds=new double[len+10];
 		if (len>=3) {
 			v.addMultipleToArray(3.0,2, ds, 5, len-2);
 			assertEquals(v.get(2)*3.0,ds[5],0.000);
 			assertEquals(0.0,ds[5+len],0.0);
 		}
 		
 		v.addMultipleToArray(2.0,0, ds, 0, len);
 		if (len>0) {
 			assertEquals(v.get(0)*2.0,ds[0],0.0);
 		}
 	}
 	
 	private void testAddMultiple(AVector v) {
 		int len=v.length();
 		Vector tv=Vector.createLength(len+10);
 		tv.addMultiple(5,v,10.0);
 		assertEquals(0.0,tv.get(4),0.0);
 		assertEquals(10.0*v.get(0),tv.get(5),0.0);
 		assertEquals(10.0*v.get(len-1),tv.get(5+len-1),0.0);
 		assertEquals(0.0,tv.get(5+len),0.0);
 	}
 	
 
 	private void testSubvectors(AVector v) {
 		int len=v.length();
 		assertEquals(v,v.subVector(0, len));
 		assertEquals(0,v.subVector(0, 0).length());
 		assertEquals(0,v.subVector(len, 0).length());
 		int m=len/2;
 		
 		AVector v2=v.subVector(0, m).join(v.subVector(m, len-m));
 		assertEquals(v,v2);
 	}
 	
 	private void testClone(AVector v) {
 		AVector cv=v.clone();
 		int len=cv.length();
 		assertEquals(v.length(), len);
 		assertFalse(cv.isView());
 		assertTrue((cv.length()==0)||cv.isMutable());		
 		assertEquals(v,cv);
 		
 		for (int i=0; i<len; i++) {
 			double x=cv.get(i);
 			// clone should have equal values
 			assertEquals(v.get(i),x,0.0000001);
 			cv.set(i, x+1);
 			
 			// changing clone should not change original
 			assertNotSame(v.get(i),cv.get(i));
 		}
 	}
 	
 	private void testExactClone(AVector v) {
 		AVector cv=v.exactClone();
 		AVector cv2=v.clone();
 		assertEquals(v,cv);
 		assertEquals(v.getClass(),cv.getClass());
 		
 		// test that trashing the exact clone doesn't affect the original vector
 		if (cv.isFullyMutable()) {
 			cv.fill(Double.NaN);
 			assertEquals(cv2,v);
 		}
 	}
 	
 	private void testSet(AVector v) {
 		v= (v.isFullyMutable()) ? v.exactClone() : v.clone();
 		
 		int len=v.length();
 		
 		Vectorz.fillRandom(v);
 		AVector v2=Vector.createLength(len);
 		v2.set(v);
 		assertEquals(v,v2);
 		
 		Vectorz.fillRandom(v);
 		v2.set(v,0);
 		assertEquals(v,v2);
 
 		Vectorz.fillRandom(v);
 		double[] data=v.toArray();
 		v2.set(data);
 		assertEquals(v,v2);
 	}
 	
 	private void testAdd(AVector v) {
 		v=v.exactClone();
 		int len=v.length();
 		int split=len/2;
 		int slen=len-split;
 		
 		AVector t=Vectorz.newVector(slen);
 		t.add(v,split);
 		assertEquals(t,v.subVector(split, slen));
 		
 		t.multiply(0.5);
 		t.set(v,split);
 		assertEquals(t,v.subVector(split, slen));
 		t.addProduct(t, Vectorz.createZeroVector(slen));
 		assertEquals(t,v.subVector(split, slen));
 	}
 	
 	
 	private void testAddEquivalents(AVector v) {
 		v=v.exactClone();
 		int len=v.length();
 		
 		AVector c=Vectorz.newVector(len);
 		c.add(v);
 		assertEquals(v,c);
 		
 		c.fill(0.0);
 		c.add(0,v);
 		assertEquals(v,c);
 		
 		c.fill(0.0);
 		c.add(v,0);
 		assertEquals(v,c);
 		
 		c.fill(0.0);
 		c.addMultiple(0,v,1.0);
 		assertEquals(v,c);
 		
 		c.fill(0.0);
 		c.addMultiple(v,1.0);
 		assertEquals(v,c);
 				
 		if (!v.isFullyMutable()) return;
 	}
 
 	
 	private void testAddFromPosition(AVector v) {
 		if (!v.isFullyMutable()) return;
 		AVector tv=v.exactClone();
 		int len=tv.length();
 		
 		AVector sv=Vectorz.createRange(len+10);
 
 		tv.add(sv,5);
 		assertEquals(v.get(0)+5.0,tv.get(0),0.0001);
 		assertEquals(v.get(len-1)+5.0+len-1,tv.get(len-1),0.0001);
 	}
 	
 	private void testAddToPosition(AVector v) {
 		int len=v.length();
 		
 		AVector tv=Vectorz.createRange(len+10);
 
 		tv.add(5,v,0,len);
 		
 		assertEquals(5.0+v.get(0),tv.get(5),0.0001);
 		assertEquals(5.0+len,tv.get(5+len),0.0001);
 	}
 	
 	private void testAddAt(AVector v) {
 		if (!v.isFullyMutable()) return;
 		v=v.exactClone();
 		int len=v.length();
 		AVector c=v.clone();
 		
 		for (int i=0; i<len; i++) {
 			v.addAt(i, c.get(i));
 		}
 		c.scale(2.0);
 		
 		assertEquals(c,v);
 	}
 	
 	public void testOutOfBounds(AVector v) {
 		if (!v.isFullyMutable()) return;
 		try {
 			v.set(-1, 0.0);
 			fail("Should be out of bounds!");
 		} catch (Throwable x) {
 			// OK!
 		}
 		
 		try {
 			if (v instanceof GrowableVector) return;
 			v.set(v.length(), 0.0);
 			fail("Should be out of bounds!");
 		} catch (Throwable x) {
 			// OK!
 		}
 	}
 	
 	public void testSubVectorMutability(AVector v) {
 		// defensive copy
 		v=v.clone();
 		assertTrue(!v.isView());
 		
 		int vlen=v.length();
 		
 		int start=Math.min(vlen/2,vlen-1);
 		
 		AVector s1 = v.subVector(start, vlen-start);
 		AVector s2 = v.subVector(start, vlen-start);
 		
 		assertTrue(s1.isView());
 		assertNotSame(s1,s2);
 		
 		int len=s1.length();
 		for (int i=0; i<len; i++) {
 			double x=s2.get(i);
 			// clone should have equal values
 			assertEquals(s1.get(i),x,0.0000001);
 			s1.set(i, x+1);
 			
 			// change should be reflected in both subvectors
 			assertEquals(s1.get(i),s2.get(i),0.0000001);
 		}
 	}
 	
 	public void testVectorMutability(AVector v) {
 		if (v.isFullyMutable()) {
 			assertTrue(v.isMutable());
 			for (int i=0; i<v.length(); i++) {
 				v.set(i,i);
 				assertEquals(i,v.get(i),0.0);
 			}
 		}
 		
 		if (v.isMutable()) {
 			// v.set(0,0.01); not always, may not be fully mutable
 		}
 	}
 	
 	
 	private void testZero(AVector v) {
 		v=v.clone();
 		v.multiply(0.0);
 		assertTrue(v.isZeroVector());
 	}
 	
 
 	private void testNormalise(AVector v) {
 		v=v.clone();
 		
 		v.set(0,v.get(0)+Math.random());
  
 		double d=v.magnitude();
 		double nresult=v.normalise();
 		
 		assertEquals(d,nresult,0.0000001);
 		
 		assertTrue(v.isUnitLengthVector());
 	}
 	
 	private void testFilling(AVector v) {
 		v=v.clone();
 		v.fill(1.23);
 		assertEquals(1.23,Vectorz.minValue(v),0.0);
 		assertEquals(1.23,Vectorz.maxValue(v),0.0);
 		
 		v.fillRange(0, v.length(), 1.24);
 		assertEquals(1.24,Vectorz.minValue(v),0.0);
 		assertEquals(1.24,Vectorz.maxValue(v),0.0);
 		assertEquals(1.24,Vectorz.averageValue(v),0.0001);
 	}
 	
 	private void testCopyTo(AVector v) {
 		int len=v.length();
 		Vector tv=Vector.createLength(len+2);
 		tv.fill(Double.NaN);
 		v.copyTo(tv, 1);
 		assertTrue(Double.isNaN(tv.get(0)));
 		assertTrue(Double.isNaN(tv.get(len+1)));
 		assertFalse(Double.isNaN(tv.get(1)));
 		assertFalse(Double.isNaN(tv.get(len)));
 	}
 
 
 	
 	private void testAsList(AVector v) {
 		List<Double> al=v.asList();
 		List<Double> tl=v.toList();
 		assertEquals(al,tl);
 		
 		int len=v.length();
 		assertEquals(len,al.size());
 		assertEquals(len,tl.size());
 	}
 	
 	private void testMultiply(AVector v) {
 		int len=v.length();
 		v=v.clone();
 		
 		AVector m=Vectorz.newVector(len);
 		m.fill(2);
 		AVector v2=v.clone();
 		v2.multiply(m);
 		v.multiply(2);
 		assertTrue(v.epsilonEquals(v2,0.00001));
 	}
 	
 
 	
 	private void testDivide(AVector v) {
 		v=v.clone();
 		
 		Vectorz.fillGaussian(v);
 		
 		AVector m=v.clone();
 		m.multiply(v);
 		m.divide(v);
 		assertTrue(v.epsilonEquals(m,0.00001));
 	}
 	
 	private void testIterator(AVector v) {
 		int count=0;
 		double total=0.0;
 		
 		for (double d: v) {
 			count++;
 			total+=d;
 		}
 		
 		assertEquals(v.length(),count);
 		assertEquals(Vectorz.totalValue(v),total,0.00000001);
 	}
 	
 	private void testApplyOp(AVector v) {
 		if (!v.isFullyMutable()) return;
 		AVector c=v.exactClone();
 		AVector d=v.exactClone();
 		
 		c.fill(5.0);
 		d.applyOp(Constant.create(5.0));
 		assertTrue(c.equals(d));
 	}
 	
 	private void doNonDegenerateTests(AVector v) {
 		if (v.length()==0) return;
 		testSubVectorMutability(v);
 		testAddMultiple(v);
 		testAddFromPosition(v);
 		testAddToPosition(v);
 		testVectorMutability(v);
 		testCopyTo(v);
 		testNormalise(v);
 		testFilling(v);
 	}
 	
 	private void doGenericTests(AVector v) {
 		testClone(v);
 		testExactClone(v);
 		testAdd(v);
 		testAddEquivalents(v);
 		testAddToArray(v);
 		testAddAt(v);
 		testAddProduct(v);
 		testAddMultipleToArray(v);
 		testApplyOp(v);
 		testInnerProducts(v);
 		testMultiply(v);
 		testDivide(v);
 		testSet(v);
 		testSquare(v);
 		testSubvectors(v);
 		testParse(v);
 		testDistances(v);
 		testMagnitudes(v);
 		testZero(v);
 		testHashCode(v);
 		testAsList(v);
 		testIterator(v);
 		testOutOfBounds(v);
 		
 		doNonDegenerateTests(v);
 		
 		new TestArrays().testArray(v);
 	}
 
 	@Test public void genericTests() {
 		doGenericTests(Vector0.of());
 		
 		doGenericTests(new Vector1(1.0));
 		doGenericTests(new Vector2(1.0,2.0));
 		doGenericTests(new Vector3(1.0,2.0,3.0));
 		doGenericTests(new Vector4(1.0,2.0,3.0,4.0));
 		
 		// bit vectors
 		doGenericTests(BitVector.of());
 		doGenericTests(BitVector.of(0));
 		doGenericTests(BitVector.of(0,1,0));
 		
 		// zero-length Vectors
 		doGenericTests(Vector.of());
 		doGenericTests(new GrowableVector(Vector.of()));
 		doGenericTests(Vector.wrap(new double[0]));
 		doGenericTests(new Vector3(1.0,2.0,3.0).subVector(2, 0));
 		
 		for (int j=0; j<10; j++) {
 			double[] data=new double[j];
 			for (int i=0; i<j; i++) data[i]=i;
 			doGenericTests(Vectorz.create(data));
 			doGenericTests(new Vector(data));
 		}
 		
 		double[] data=new double[100];
 		int[] indexes=new int[100];
 		for (int i=0; i<100; i++) {
 			data[i]=i;
 			indexes[i]=i;
 		}
 
 		doGenericTests(new ArraySubVector(data));
 		doGenericTests(IndexedArrayVector.wrap(data,indexes));
 		doGenericTests(IndexedSubVector.wrap(Vector.of(data),indexes));
 		
 		doGenericTests(new Vector(data).subVector(25, 50));
 		doGenericTests(new ArraySubVector(data).subVector(25, 50));
 		
 		AVector v3 = new Vector3(1.0,2.0,3.0);
 		doGenericTests(v3.subVector(1, 2));
 		doGenericTests(new WrappedSubVector(v3,1,2));
 
 		AVector joined = Vectorz.join(v3, Vectorz.create(data));
 		doGenericTests(joined);
 		
 		AVector v4 = Vectorz.create(1.0,2.0,3.0,4.0);
 		doGenericTests(v4);
 		
 		AVector g0=new GrowableVector();
 		doGenericTests(g0);
 		
 		AVector g4=new GrowableVector(v4);
 		doGenericTests(g4);
 		
 		AVector j5=Vectorz.join(g4,joined,v3,v4,g0,g0,joined);
 		doGenericTests(j5);
 		
 		AMatrix m1=Matrixx.createRandomSquareMatrix(5);
 		doGenericTests(m1.asVector());
 		doGenericTests(m1.getRow(4));
 		doGenericTests(m1.getColumn(1));
 		
 		AMatrix m2=Matrixx.createRandomSquareMatrix(3);
 		doGenericTests(m2.asVector());
 		doGenericTests(m2.getRow(1));
 		doGenericTests(m2.getColumn(1));
 
 		AMatrix m3=Matrixx.createRandomMatrix(4,5);
 		doGenericTests(m3.asVector());
 		doGenericTests(m3.getRow(2));
 		doGenericTests(m3.getColumn(2));
 		
 		doGenericTests(new AxisVector(1,3));
 		doGenericTests(new AxisVector(0,1));
 		doGenericTests(new AxisVector(5,10));
 		
 		doGenericTests(new SingleElementVector(1,3));
 		doGenericTests(new SingleElementVector(0,1));
 
 		doGenericTests(new RepeatedElementVector(1,1.0));
 		doGenericTests(new RepeatedElementVector(10,1.0));
 		
 		doGenericTests(SparseIndexedVector.create(10,Index.of(1,3,6),Vector.of(1.0,2.0,3.0)));
 		doGenericTests(SparseIndexedVector.create(10,Index.of(),Vector.of()));
 		doGenericTests(Vector3.of(1,2,3).join(SparseIndexedVector.create(5,Index.of(1,3),Vector.of(1.0,2.0))));
 		
 		doGenericTests(new DoubleScalar(1.0).asVector());
 		
 		doGenericTests(JoinedArrayVector.create(v4));
 		doGenericTests(JoinedArrayVector.create(j5));
 		doGenericTests(Vector3.of(1,2,3).join(JoinedArrayVector.create(g4)));
		
		doGenericTests(StridedArrayVector.wrap(new double[]{}, 0, 0, 100));
		doGenericTests(StridedArrayVector.wrap(new double[]{1,2}, 1, 1, 100));
 	}
 }
