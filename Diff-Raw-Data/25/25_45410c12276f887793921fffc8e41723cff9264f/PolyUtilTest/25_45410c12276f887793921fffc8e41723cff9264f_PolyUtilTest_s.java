 /*
  * $Id$
  */
 
 package edu.jas.poly;
 
 import junit.framework.Test;
 import junit.framework.TestCase;
 import junit.framework.TestSuite;
 
 import edu.jas.arith.BigInteger;
 import edu.jas.arith.ModInteger;
 import edu.jas.arith.BigRational;
 import edu.jas.arith.BigComplex;
 
 import edu.jas.poly.TermOrder;
 import edu.jas.poly.GenPolynomial;
 import edu.jas.poly.GenPolynomialRing;
 
 
 /**
  * PolyUtil Test using JUnit.
  * @author Heinz Kredel.
  */
 
 public class PolyUtilTest extends TestCase {
 
 /**
  * main.
  */
    public static void main (String[] args) {
           junit.textui.TestRunner.run( suite() );
    }
 
 /**
  * Constructs a <CODE>PolyUtilTest</CODE> object.
  * @param name String.
  */
    public PolyUtilTest(String name) {
           super(name);
    }
 
 /**
  */ 
  public static Test suite() {
      TestSuite suite= new TestSuite(PolyUtilTest.class);
      return suite;
    }
 
    //private final static int bitlen = 100;
 
    TermOrder to = new TermOrder( TermOrder.INVLEX );
 
    GenPolynomialRing<BigInteger> dfac;
    GenPolynomialRing<BigInteger> cfac;
    GenPolynomialRing<GenPolynomial<BigInteger>> rfac;
 
    BigInteger ai;
    BigInteger bi;
    BigInteger ci;
    BigInteger di;
    BigInteger ei;
 
    GenPolynomial<BigInteger> a;
    GenPolynomial<BigInteger> b;
    GenPolynomial<BigInteger> c;
    GenPolynomial<BigInteger> d;
    GenPolynomial<BigInteger> e;
 
    GenPolynomial<GenPolynomial<BigInteger>> ar;
    GenPolynomial<GenPolynomial<BigInteger>> br;
    GenPolynomial<GenPolynomial<BigInteger>> cr;
    GenPolynomial<GenPolynomial<BigInteger>> dr;
    GenPolynomial<GenPolynomial<BigInteger>> er;
 
    int rl = 5; 
    int kl = 5;
    int ll = 5;
    int el = 3;
    float q = 0.3f;
 
    protected void setUp() {
        a = b = c = d = e = null;
        ai = bi = ci = di = ei = null;
        ar = br = cr = dr = er = null;
        dfac = new GenPolynomialRing<BigInteger>(new BigInteger(1),rl,to);
        cfac = new GenPolynomialRing<BigInteger>(new BigInteger(1),rl-1,to);
        rfac = new GenPolynomialRing<GenPolynomial<BigInteger>>(cfac,1,to);
    }
 
    protected void tearDown() {
        a = b = c = d = e = null;
        ai = bi = ci = di = ei = null;
        ar = br = cr = dr = er = null;
        dfac = null;
        cfac = null;
        rfac = null;
    }
 
 
    protected static java.math.BigInteger getPrime1() {
        long prime = 2; //2^60-93; // 2^30-35; //19; knuth (2,390)
        for ( int i = 1; i < 60; i++ ) {
            prime *= 2;
        }
        prime -= 93;
        //prime = 37;
        //System.out.println("p1 = " + prime);
        return new java.math.BigInteger(""+prime);
    }
 
    protected static java.math.BigInteger getPrime2() {
        long prime = 2; //2^60-93; // 2^30-35; //19; knuth (2,390)
        for ( int i = 1; i < 30; i++ ) {
            prime *= 2;
        }
        prime -= 35;
        //prime = 19;
        //System.out.println("p1 = " + prime);
        return new java.math.BigInteger(""+prime);
    }
 
 
 /**
  * Test recursive <--> distributive conversion.
  * 
  */
  public void testConversion() {
      c = dfac.getONE();
      assertTrue("length( c ) = 1", c.length() == 1);
      assertTrue("isZERO( c )", !c.isZERO() );
      assertTrue("isONE( c )", c.isONE() );
 
      cr = PolyUtil.recursive(rfac,c);
      a = PolyUtil.distribute(dfac,cr);
      assertEquals("c == dist(rec(c))", c, a );
 
      d = dfac.getZERO();
      assertTrue("length( d ) = 0", d.length() == 0);
      assertTrue("isZERO( d )", d.isZERO() );
      assertTrue("isONE( d )", !d.isONE() );
 
      dr = PolyUtil.recursive(rfac,d);
      b = PolyUtil.distribute(dfac,dr);
      assertEquals("d == dist(rec(d))", d, b );
  }
 
 
 /**
  * Test random recursive <--> distributive conversion.
  * 
  */
  public void testRandomConversion() {
      for (int i = 0; i < 7; i++) {
          c = dfac.random(kl*(i+2),ll+2*i,el+i,q);
 
          assertTrue("length( c"+i+" ) <> 0", c.length() >= 0);
          assertTrue(" not isZERO( c"+i+" )", !c.isZERO() );
          assertTrue(" not isONE( c"+i+" )", !c.isONE() );
 
          cr = PolyUtil.recursive(rfac,c);
          a = PolyUtil.distribute(dfac,cr);
          //System.out.println("c   = " + c);
          //System.out.println("cr  = " + cr);
          //System.out.println("crd = " + a);
 
          assertEquals("c == dist(rec(c))", c, a );
      }
  }
 
 
 /**
  * Test random rational <--> integer conversion.
  * 
  */
  public void testRationalConversion() {
      GenPolynomialRing<BigRational> rfac
          = new GenPolynomialRing<BigRational>(new BigRational(1),rl,to);
 
      GenPolynomial<BigRational> ar;
      GenPolynomial<BigRational> br;
 
      for (int i = 0; i < 3; i++) {
          c = dfac.random(kl*(i+9),ll*(i+3),el+i,q).abs();
          //c = c.multiply( new BigInteger(99) ); // fails, since not primitive
          //c = GreatestCommonDivisor.primitivePart(c);
 
          assertTrue("length( c"+i+" ) <> 0", c.length() >= 0);
          assertTrue(" not isZERO( c"+i+" )", !c.isZERO() );
          assertTrue(" not isONE( c"+i+" )", !c.isONE() );
 
          ar = PolyUtil.<BigRational>fromIntegerCoefficients(rfac,c);
          br = ar.monic();
          a = PolyUtil.integerFromRationalCoefficients(dfac,br);
          //System.out.println("c   = " + c);
          //System.out.println("ar  = " + ar);
          //System.out.println("br  = " + br);
          //System.out.println("crd = " + a);
 
          assertEquals("c == integer(rational(c))", c, a );
      }
  }
 
 
 /**
  * Test random modular <--> integer conversion.
  * 
  */
  public void testModularConversion() {
      ModInteger pm = new ModInteger(getPrime1(),1);
      GenPolynomialRing<ModInteger> mfac
          = new GenPolynomialRing<ModInteger>(pm,rl,to);
 
      GenPolynomial<ModInteger> ar;
      GenPolynomial<ModInteger> br;
 
      for (int i = 0; i < 3; i++) {
          c = dfac.random(kl*(i+2),ll*(i+1),el+i,q).abs();
          //c = c.multiply( new BigInteger(99) ); // fails, since not primitive
          //c = GreatestCommonDivisor.primitivePart(c);
 
          assertTrue("length( c"+i+" ) <> 0", c.length() >= 0);
          assertTrue(" not isZERO( c"+i+" )", !c.isZERO() );
          assertTrue(" not isONE( c"+i+" )", !c.isONE() );
 
          ar = PolyUtil.<ModInteger>fromIntegerCoefficients(mfac,c);
          a = PolyUtil.integerFromModularCoefficients(dfac,ar);
          //System.out.println("c   = " + c);
          //System.out.println("ar  = " + ar);
          //System.out.println("crd = " + a);
 
          assertEquals("c == integer(modular(c))", c, a );
      }
  }
 
 
 /**
  * Test chinese remainder.
  * 
  */
  public void testChineseRemainder() {
      java.math.BigInteger p1 = getPrime1();
      java.math.BigInteger p2 = getPrime2();
      java.math.BigInteger p12 = p1.multiply(p2);
 
      ModInteger pm1 = new ModInteger(p1,1);
      GenPolynomialRing<ModInteger> mfac1
          = new GenPolynomialRing<ModInteger>(pm1,rl,to);
 
      ModInteger pm2 = new ModInteger(p2,1);
      GenPolynomialRing<ModInteger> mfac2
          = new GenPolynomialRing<ModInteger>(pm2,rl,to);
 
      ModInteger pm12 = new ModInteger(p12,1);
      GenPolynomialRing<ModInteger> mfac
          = new GenPolynomialRing<ModInteger>(pm12,rl,to);
 
      ModInteger di = new ModInteger(p2,p1);
      di = di.inverse();
      //System.out.println("di = " + di);
 
      GenPolynomial<ModInteger> am;
      GenPolynomial<ModInteger> bm;
      GenPolynomial<ModInteger> cm;
 
      ExpVector degv, qdegv;
 
      for (int i = 0; i < 3; i++) {
          c = dfac.random( (59+29)/2, ll*(i+1), el+i, q);
          //c = c.multiply( new BigInteger(99) ); // fails, since not primitive
          //c = GreatestCommonDivisor.primitivePart(c);
          degv = c.degreeVector();
          //System.out.println("degv  = " + degv);
 
          assertTrue("length( c"+i+" ) <> 0", c.length() >= 0);
          assertTrue(" not isZERO( c"+i+" )", !c.isZERO() );
          assertTrue(" not isONE( c"+i+" )", !c.isONE() );
 
          am = PolyUtil.<ModInteger>fromIntegerCoefficients(mfac1,c);
          qdegv = am.degreeVector();
          //System.out.println("qdegv  = " + qdegv);
          if ( !degv.equals( qdegv) ) {
             continue;
          }
          bm = PolyUtil.<ModInteger>fromIntegerCoefficients(mfac2,c);
          qdegv = bm.degreeVector();
          //System.out.println("qdegv  = " + qdegv);
          if ( !degv.equals( qdegv) ) {
             continue;
          }
 
          cm = PolyUtil.chineseRemainder(mfac,am,di,bm);
          a = PolyUtil.integerFromModularCoefficients(dfac,cm);
 
          //System.out.println("c  = " + c);
          //System.out.println("am = " + am);
          //System.out.println("bm = " + bm);
          //System.out.println("cm = " + cm);
          //System.out.println("a  = " + a);
 
          assertEquals("cra(c mod p1,c mod p2) = c",c,a);
      }
  }
 
 
 /**
  * Test complex conversion.
  * 
  */
  public void testComplexConversion() {
      BigRational rf = new BigRational(1);
      GenPolynomialRing<BigRational> rfac
          = new GenPolynomialRing<BigRational>(rf,rl,to);
 
      BigComplex cf = new BigComplex(1);
      GenPolynomialRing<BigComplex> cfac
          = new GenPolynomialRing<BigComplex>(cf,rl,to);
 
      BigComplex imag = BigComplex.I;
 
      GenPolynomial<BigRational> rp;
      GenPolynomial<BigRational> ip;
      GenPolynomial<BigComplex> crp;
      GenPolynomial<BigComplex> cip;
      GenPolynomial<BigComplex> cp;
      GenPolynomial<BigComplex> ap;
 
      for (int i = 0; i < 3; i++) {
          cp = cfac.random( kl+2*i, ll*(i+1), el+i, q);
  
          assertTrue("length( c"+i+" ) <> 0", cp.length() >= 0);
          assertTrue(" not isZERO( c"+i+" )", !cp.isZERO() );
          assertTrue(" not isONE( c"+i+" )", !cp.isONE() );
 
          rp = PolyUtil.realPart(rfac,cp);
          ip = PolyUtil.imaginaryPart(rfac,cp);
 
          crp = PolyUtil.complexFromRational(cfac,rp);
          cip = PolyUtil.complexFromRational(cfac,ip);
 
          ap = crp.sum( cip.multiply( imag ) );
 
          //System.out.println("cp = " + cp);
          //System.out.println("rp = " + rp);
          //System.out.println("ip = " + ip);
          //System.out.println("crp = " + crp);
          //System.out.println("cip = " + cip);
          //System.out.println("ap  = " + ap);
 
          assertEquals("re(c)+i*im(c) = c",cp,ap);
      }
  }
 
 
 /**
  * Test evaluate main recursive.
  * 
  */
  public void testEvalMainRecursive() {
      ai  = (new BigInteger()).random(kl);
      //System.out.println("ai  = " + ai);
 
      ar = rfac.getZERO();
      //System.out.println("ar  = " + ar);
 
      a = PolyUtil.<BigInteger>evaluateMain(cfac,ar,ai);
      //System.out.println("a   = " + a);
 
      assertTrue("isZERO( a )", a.isZERO() );
 
      ar = rfac.getONE();
      //System.out.println("ar  = " + ar);
 
      a = PolyUtil.<BigInteger>evaluateMain(cfac,ar,ai);
      //System.out.println("a   = " + a);
 
      assertTrue("isONE( a )", a.isONE() );
 
 
      //ar = rfac.getONE();
      ar = rfac.random(kl,ll,el,q);
      //System.out.println("ar  = " + ar);
      //br = rfac.getONE();
      br = rfac.random(kl,ll,el,q);
      //System.out.println("br  = " + br);
 
      cr = br.sum(ar);
      //System.out.println("cr  = " + cr);
 
      a = PolyUtil.<BigInteger>evaluateMain(cfac,ar,ai);
      b = PolyUtil.<BigInteger>evaluateMain(cfac,br,ai);
      c = PolyUtil.<BigInteger>evaluateMain(cfac,cr,ai);
      //System.out.println("a   = " + a);
      //System.out.println("b   = " + b);
      //System.out.println("c   = " + c);
 
      d = a.sum( b );
      //System.out.println("d   = " + d);
 
      assertEquals("eval(a+b) == eval(a) + eval(b)", c, d );
 
 
      cr = br.multiply(ar);
      //System.out.println("cr  = " + cr);
 
      a = PolyUtil.<BigInteger>evaluateMain(cfac,ar,ai);
      b = PolyUtil.<BigInteger>evaluateMain(cfac,br,ai);
      c = PolyUtil.<BigInteger>evaluateMain(cfac,cr,ai);
      //System.out.println("a   = " + a);
      //System.out.println("b   = " + b);
      //System.out.println("c   = " + c);
 
      d = a.multiply( b );
      //System.out.println("d   = " + d);
 
      assertEquals("eval(a*b) == eval(a) * eval(b)", c, d );
  }
 
 
 /**
  * Test evaluate main.
  * 
  */
  public void testEvalMain() {
      ei  = (new BigInteger()).random(kl);
      //System.out.println("ei  = " + ei);
 
      cfac = new GenPolynomialRing<BigInteger>(new BigInteger(1),1,to);
      //System.out.println("cfac  = " + cfac);
 
      a = cfac.getZERO();
      //System.out.println("a  = " + a);
 
      ai = PolyUtil.<BigInteger>evaluateMain(ei,a,ei);
      //System.out.println("ai   = " + ai);
 
      assertTrue("isZERO( ai )", ai.isZERO() );
 
      a = cfac.getONE();
      //System.out.println("a  = " + a);
 
      ai = PolyUtil.<BigInteger>evaluateMain(ei,a,ei);
      //System.out.println("ai   = " + ai);
 
      assertTrue("isONE( ai )", ai.isONE() );
 
      //a = cfac.getONE();
      a = cfac.random(kl,ll,el,q);
      //System.out.println("a  = " + a);
      //b = cfac.getONE();
      b = cfac.random(kl,ll,el,q);
      //System.out.println("b  = " + b);
 
      c = b.sum(a);
      //System.out.println("c  = " + c);
 
      ai = PolyUtil.<BigInteger>evaluateMain(ei,a,ei);
      bi = PolyUtil.<BigInteger>evaluateMain(ei,b,ei);
      ci = PolyUtil.<BigInteger>evaluateMain(ei,c,ei);
      //System.out.println("ai   = " + ai);
      //System.out.println("bi   = " + bi);
      //System.out.println("ci   = " + ci);
 
      di = bi.sum( ai );
      //System.out.println("di   = " + di);
 
      assertEquals("eval(a+b) == eval(a) + eval(b)", ci, di );
 
 
      c = b.multiply(a);
      //System.out.println("c  = " + c);
 
      ai = PolyUtil.<BigInteger>evaluateMain(ei,a,ei);
      bi = PolyUtil.<BigInteger>evaluateMain(ei,b,ei);
      ci = PolyUtil.<BigInteger>evaluateMain(ei,c,ei);
      //System.out.println("ai   = " + ai);
      //System.out.println("bi   = " + bi);
      //System.out.println("ci   = " + ci);
 
      di = bi.multiply( ai );
      //System.out.println("di   = " + di);
 
      assertEquals("eval(a*b) == eval(a) * eval(b)", ci, di );
  }
 
 
 /**
  * Test evaluate first.
  * 
  */
  public void testEvalFirst() {
      ei  = (new BigInteger()).random(kl);
      //System.out.println("ei  = " + ei);
 
      GenPolynomial<BigInteger> ae, be, ce, de, ee;
 
      GenPolynomialRing<BigInteger> fac;
      fac = new GenPolynomialRing<BigInteger>(new BigInteger(1),rl,to);
      //System.out.println("fac  = " + fac);
 
      cfac = new GenPolynomialRing<BigInteger>(new BigInteger(1),1,to);
      //System.out.println("cfac  = " + cfac);
 
      dfac = new GenPolynomialRing<BigInteger>(new BigInteger(1),rl-1,to);
      //System.out.println("dfac  = " + dfac);
 
      a = fac.getZERO();
      //System.out.println("a  = " + a);
 
      ae = PolyUtil.<BigInteger>evaluateFirst(cfac,dfac,a,ei);
      //System.out.println("ae   = " + ae);
 
      assertTrue("isZERO( ae )", ae.isZERO() );
 
      a = fac.getONE();
      //System.out.println("a  = " + a);
 
      ae = PolyUtil.<BigInteger>evaluateFirst(cfac,dfac,a,ei);
      //System.out.println("ae   = " + ae);
 
      assertTrue("isONE( ae )", ae.isONE() );
 
      //a = fac.getONE();
      a = fac.random(kl,ll,el,q);
      //System.out.println("a  = " + a);
      //b = fac.getONE();
      b = fac.random(kl,ll,el,q);
      //System.out.println("b  = " + b);
 
      c = b.sum(a);
      //System.out.println("c  = " + c);
 
      ae = PolyUtil.<BigInteger>evaluateFirst(cfac,dfac,a,ei);
      be = PolyUtil.<BigInteger>evaluateFirst(cfac,dfac,b,ei);
      ce = PolyUtil.<BigInteger>evaluateFirst(cfac,dfac,c,ei);
      //System.out.println("ae   = " + ae);
      //System.out.println("be   = " + be);
      //System.out.println("ce   = " + ce);
 
      de = be.sum( ae );
      //System.out.println("de   = " + de);
 
      assertEquals("eval(a+b) == eval(a) + eval(b)", ce, de );
 
 
      c = b.multiply(a);
      //System.out.println("c  = " + c);
 
      ae = PolyUtil.<BigInteger>evaluateFirst(cfac,dfac,a,ei);
      be = PolyUtil.<BigInteger>evaluateFirst(cfac,dfac,b,ei);
      ce = PolyUtil.<BigInteger>evaluateFirst(cfac,dfac,c,ei);
      //System.out.println("ae   = " + ae);
      //System.out.println("be   = " + be);
      //System.out.println("ce   = " + ce);
 
      de = be.multiply( ae );
      //System.out.println("de   = " + de);
 
      assertEquals("eval(a*b) == eval(a) * eval(b)", ce, de );
  }
 
 
 /**
  * Test interpolate univariate 1 polynomial.
  * 
  */
  public void testInterpolateUnivariateOne() {
      ModInteger ai, bi, ci, di, ei, fi, gi, hi;
      GenPolynomial<ModInteger> a, b, c, d, e;
      GenPolynomialRing<ModInteger> cfac;
      ModInteger fac;
      GenPolynomial<ModInteger> r;
      GenPolynomial<ModInteger> Q;
      GenPolynomial<ModInteger> Qp;
 
      fac = new ModInteger(19,1);
      //System.out.println("fac.modul  = " + fac.getModul());
      cfac = new GenPolynomialRing<ModInteger>(fac,1,to);
      //System.out.println("cfac  = " + cfac);
 
 
      a = cfac.getONE();
      //System.out.println("a  = " + a);
 
 
      ei  = fac.fromInteger(11);
      //System.out.println("ei  = " + ei);
      // a(ei)
      ai = PolyUtil.<ModInteger>evaluateMain(fac,a,ei);
      //System.out.println("ai   = " + ai);
      assertTrue("isONE( ai )", ai.isONE() );
 
      di  = fac.fromInteger(13);
      //System.out.println("di  = " + di);
      // a(di)
      bi = PolyUtil.<ModInteger>evaluateMain(fac,a,di);
      //System.out.println("bi   = " + bi);
      assertTrue("isONE( bi )", bi.isONE() );
 
 
      // interpolation result
      r = cfac.getZERO();
      //System.out.println("r   = " + r);
 
      // interpolation polynomials product
      Q = cfac.getONE();
      //System.out.println("Q   = " + Q);
 
 
      ci = PolyUtil.<ModInteger>evaluateMain(fac,Q,ei);
      //System.out.println("ci   = " + ci);
      // Q(ei)^-1
      fi = ci.inverse();
      //System.out.println("fi   = " + fi);
      r = PolyUtil.<ModInteger>interpolate(cfac,r,Q,fi,ai,ei);
      //System.out.println("r   = " + r);
 
 
      // next evaluation polynomial
      Qp = cfac.univariate(0);
      Qp = Qp.subtract( cfac.getONE().multiply(ei) );
      //System.out.println("Qp   = " + Qp);
      Q = Q.multiply( Qp );
      //System.out.println("Q   = " + Q);
 
      ci = PolyUtil.<ModInteger>evaluateMain(fac,Q,di);
      //System.out.println("ci   = " + ci);
      // Q(di)^-1
      fi = ci.inverse();
      //System.out.println("fi   = " + fi);
      r = PolyUtil.<ModInteger>interpolate(cfac,r,Q,fi,bi,di);
      //System.out.println("r   = " + r);
 
      // check evaluation
      gi = PolyUtil.<ModInteger>evaluateMain(fac,r,ei);
      //System.out.println("gi   = " + gi);
      hi = PolyUtil.<ModInteger>evaluateMain(fac,r,di);
      //System.out.println("hi   = " + hi);
 
      //            interpolate( a(ei), a(di) )            = a (mod 19)
      assertEquals("interpolate(a mod (x-ei),a mod (x-di)) = a (mod 19)",a,r);
  }
 
 
 /**
  * Test interpolate univariate deg > 0 polynomial.
  * 
  */
  public void testInterpolateUnivariate() {
      ModInteger ai, bi, ci, di, ei, fi, gi, hi;
      GenPolynomial<ModInteger> a, b, c, d, e;
      GenPolynomialRing<ModInteger> cfac;
      ModInteger fac;
      GenPolynomial<ModInteger> r;
      GenPolynomial<ModInteger> Q;
      GenPolynomial<ModInteger> Qp;
 
      //long prime = 19;
      long prime = getPrime1().longValue();
      fac = new ModInteger(prime,1);
      //System.out.println("fac.modul  = " + fac.getModul());
      cfac = new GenPolynomialRing<ModInteger>(fac,1,to);
      //System.out.println("cfac  = " + cfac);
      int maxdeg = 19;
 
      // polynomial to interpolate
      long deg = 0;
      do {
         a = cfac.random(kl,ll,maxdeg,q);
         if ( !a.isZERO() ) {
            deg = a.degree(0);
         }
      } while ( deg <= 0  );
      //System.out.println("a  = " + a);
      //System.out.println("deg  = " + deg);
 
      // interpolation result
      r = cfac.getZERO();
      //System.out.println("r   = " + r);
 
      // interpolation polynomials product
      Q = cfac.getONE();
      //System.out.println("Q   = " + Q);
 
      long i = -1;
      long qdeg;
      do {
          i++;
          if ( i >= prime ) {
             assertTrue("elements of Z_prime exhausted", i < prime);
          }
          qdeg = Q.degree(0);
          ei  = fac.fromInteger(i);
          //System.out.println("ei  = " + ei);
          // a(ei)
          ai = PolyUtil.<ModInteger>evaluateMain(fac,a,ei);
          //System.out.println("ai   = " + ai);
 
          ci = PolyUtil.<ModInteger>evaluateMain(fac,Q,ei);
          //System.out.println("ci   = " + ci);
          // Q(ei)^-1
          fi = ci.inverse();
          //System.out.println("fi   = " + fi);
          r = PolyUtil.<ModInteger>interpolate(cfac,r,Q,fi,ai,ei);
          //System.out.println("r   = " + r);
 
          // next evaluation polynomial
          Qp = cfac.univariate(0);
          Qp = Qp.subtract( cfac.getONE().multiply(ei) );
          //System.out.println("Qp   = " + Qp);
          Q = Q.multiply( Qp );
          //System.out.println("Q   = " + Q);
      } while ( qdeg < deg );
 
      //System.out.println("a   = " + a);
      //System.out.println("r   = " + r);
 
      //            interpolate( a(e1), ..., a(ei) )           = a (mod 19)
      assertEquals("interpolate(a mod (x-e1),...,a mod (x-ei)) = a (mod 19)",a,r);
  }
 
 
 /**
  * Test interpolate multivariate deg > 0 polynomial.
  * 
  */
  public void testInterpolateMultivariate() {
      ModInteger ai, bi, ci, di, ei, fi, gi, hi;
      GenPolynomial<ModInteger> ap, bp, cp, dp, ep, fp, gp, hp;
      GenPolynomial<GenPolynomial<ModInteger>> a, b, c, d, e;
      GenPolynomialRing<GenPolynomial<ModInteger>> cfac;
      GenPolynomialRing<ModInteger> ufac;
      GenPolynomialRing<ModInteger> dfac;
      ModInteger fac;
      GenPolynomial<GenPolynomial<ModInteger>> r;
      GenPolynomial<ModInteger> Q;
      GenPolynomial<ModInteger> Qp;
 
      //long prime = 19;
      long prime = getPrime1().longValue();
      fac = new ModInteger(prime,1);
      //System.out.println("fac.modul  = " + fac.getModul());
      ufac = new GenPolynomialRing<ModInteger>(fac,1,to);
      //System.out.println("ufac  = " + ufac);
      cfac = new GenPolynomialRing<GenPolynomial<ModInteger>>(ufac,rl,to);
      //System.out.println("cfac  = " + cfac);
      dfac = new GenPolynomialRing<ModInteger>(fac,rl,to);
      //System.out.println("dfac  = " + dfac);
      int maxdeg = 19;
 
      // polynomial to interpolate
      long deg = 0;
      do {
         a = cfac.random(kl,ll+9,maxdeg,q);
         if ( !a.isZERO() ) {
            deg = PolyUtil.<ModInteger>coeffMaxDegree( a );
         }
      } while ( deg <= 0  );
      //System.out.println("a  = " + a);
      //System.out.println("deg  = " + deg);
      ExpVector degv = a.degreeVector();
      //System.out.println("degv  = " + degv);
 
      // interpolation result
      r = cfac.getZERO();
      //System.out.println("r   = " + r);
 
      // interpolation polynomials product
      Q = ufac.getONE();
      //System.out.println("Q   = " + Q);
 
      long i = -1;
      long qdeg;
      ExpVector qdegv;
      do {
          i++;
          if ( i >= prime ) {
             assertTrue("elements of Z_prime exhausted", i < prime);
          }
          qdeg = Q.degree(0);
          ei  = fac.fromInteger(i);
          //System.out.println("ei  = " + ei);
          // a(ei)
          ap = PolyUtil.<ModInteger>evaluateFirstRec(ufac,dfac,a,ei);
          //System.out.println("ap   = " + ap);
          qdegv = ap.degreeVector();
          //System.out.println("qdegv = " + qdegv);
          if ( !degv.equals( qdegv) ) {
             continue;
          }
          ci = PolyUtil.<ModInteger>evaluateMain(fac,Q,ei);
          //System.out.println("ci   = " + ci);
          // Q(ei)^-1
          fi = ci.inverse();
          //System.out.println("fi   = " + fi);
          r = PolyUtil.<ModInteger>interpolate(cfac,r,Q,fi,ap,ei);
          //System.out.println("r   = " + r);
 
          // check
          bp = PolyUtil.<ModInteger>evaluateFirstRec(ufac,dfac,r,ei);
          //System.out.println("bp   = " + bp);
          assertEquals("interpolate(a)(ei) == a ",bp,ap);
 
 
          // next evaluation polynomial
          Qp = ufac.univariate(0);
          Qp = Qp.subtract( ufac.getONE().multiply(ei) );
          //System.out.println("Qp   = " + Qp);
          Q = Q.multiply( Qp );
          //System.out.println("Q   = " + Q);
      } while ( qdeg <= deg );
 
      //System.out.println("a   = " + a);
      //System.out.println("r   = " + r);
 
      //            interpolate( a(e1), ..., a(ei) )           = a (mod 19)
      assertEquals("interpolate(a mod (x-e1),...,a mod (x-ei)) = a (mod 19)",a,r);
  }
 
 
 /**
  * Test interpolate rational multivariate deg > 0 polynomial.
  * 
  */
  public void testInterpolateRationalMultivariate() {
      BigRational ai, bi, ci, di, ei, fi, gi, hi;
      GenPolynomial<BigRational> ap, bp, cp, dp, ep, fp, gp, hp;
      GenPolynomial<GenPolynomial<BigRational>> a, b, c, d, e;
      GenPolynomialRing<GenPolynomial<BigRational>> cfac;
      GenPolynomialRing<BigRational> ufac;
      GenPolynomialRing<BigRational> dfac;
      BigRational fac;
      GenPolynomial<GenPolynomial<BigRational>> r;
      GenPolynomial<BigRational> Q;
      GenPolynomial<BigRational> Qp;
 
      fac = new BigRational();
      //System.out.println("fac.modul  = " + fac.getModul());
      ufac = new GenPolynomialRing<BigRational>(fac,1,to);
      //System.out.println("ufac  = " + ufac);
      cfac = new GenPolynomialRing<GenPolynomial<BigRational>>(ufac,rl,to);
      //System.out.println("cfac  = " + cfac);
      dfac = new GenPolynomialRing<BigRational>(fac,rl,to);
      //System.out.println("dfac  = " + dfac);
      int maxdeg = 19;
 
      // polynomial to interpolate
      long deg = 0;
      do {
         a = cfac.random(kl,ll+9,maxdeg,q);
         if ( !a.isZERO() ) {
            deg = PolyUtil.<BigRational>coeffMaxDegree( a );
         }
      } while ( deg <= 0  );
      //System.out.println("a  = " + a);
      //System.out.println("deg  = " + deg);
      ExpVector degv = a.degreeVector();
      //System.out.println("degv  = " + degv);
 
      // interpolation result
      r = cfac.getZERO();
      //System.out.println("r   = " + r);
 
      // interpolation polynomials product
      Q = ufac.getONE();
      //System.out.println("Q   = " + Q);
 
      long i = -1;
      long qdeg;
      ExpVector qdegv;
      do {
          i++;
          qdeg = Q.degree(0);
          ei  = fac.fromInteger(i);
          //System.out.println("ei  = " + ei);
          // a(ei)
          ap = PolyUtil.<BigRational>evaluateFirstRec(ufac,dfac,a,ei);
          //System.out.println("ap   = " + ap);
          qdegv = ap.degreeVector();
          //System.out.println("qdegv = " + qdegv);
          if ( !degv.equals( qdegv) ) {
             continue;
          }
          ci = PolyUtil.<BigRational>evaluateMain(fac,Q,ei);
          //System.out.println("ci   = " + ci);
          // Q(ei)^-1
          fi = ci.inverse();
          //System.out.println("fi   = " + fi);
          r = PolyUtil.<BigRational>interpolate(cfac,r,Q,fi,ap,ei);
          //System.out.println("r   = " + r);
 
          // check
          bp = PolyUtil.<BigRational>evaluateFirstRec(ufac,dfac,r,ei);
          //System.out.println("bp   = " + bp);
          assertEquals("interpolate(a)(ei) == a ",bp,ap);
 
 
          // next evaluation polynomial
          Qp = ufac.univariate(0);
          Qp = Qp.subtract( ufac.getONE().multiply(ei) );
          //System.out.println("Qp   = " + Qp);
          Q = Q.multiply( Qp );
          //System.out.println("Q   = " + Q);
      } while ( qdeg <= deg );
 
      //System.out.println("a   = " + a);
      //System.out.println("r   = " + r);
 
      //            interpolate( a(e1), ..., a(ei) )           = a (mod 19)
      assertEquals("interpolate(a mod (x-e1),...,a mod (x-ei)) = a (mod 19)",a,r);
  }
 
 
 /**
  * Test Hensel listing.
  * 
  */
  public void testHenselLifting() {
     //java.math.BigInteger p = getPrime1();
     java.math.BigInteger p = new java.math.BigInteger("19");
     //java.math.BigInteger p = new java.math.BigInteger("5");
     java.math.BigInteger m = p.multiply(p).multiply(p); 
      //.multiply(p).multiply(p).multiply(p);
 
     BigInteger mi = new BigInteger(m);
 
      ModInteger pm = new ModInteger(p,1);
      GenPolynomialRing<ModInteger> pfac
          = new GenPolynomialRing<ModInteger>(pm,1,to);
 
     ModInteger mm = new ModInteger(m,1);
     GenPolynomialRing<ModInteger> mfac
         = new GenPolynomialRing<ModInteger>(mm,1,to);

      dfac = new GenPolynomialRing<BigInteger>(mi,1,to);
 
      GenPolynomial<ModInteger> am;
      GenPolynomial<ModInteger> bm;
      GenPolynomial<ModInteger> cm;
      GenPolynomial<ModInteger> ap;
      GenPolynomial<ModInteger> bp;
      GenPolynomial<ModInteger> cp;
      GenPolynomial<ModInteger> sp;
      GenPolynomial<ModInteger> tp;
      GenPolynomial<ModInteger>[] egcd;
      GenPolynomial<ModInteger> ap1;
      GenPolynomial<ModInteger> bp1;
 
      GenPolynomial<BigInteger>[] lift;
      GenPolynomial<BigInteger> a1;
      GenPolynomial<BigInteger> b1;
      GenPolynomial<BigInteger> c1;
 
      ExpVector degv, qdegv;
 
      for (int i = 0; i < 5; i++) {
          a = dfac.random( kl, ll, el+5, q).abs();
          b = dfac.random( kl, ll, el+5, q).abs();
          //a = dfac.univariate(0).sum( dfac.fromInteger(30) );
          //b = dfac.univariate(0).subtract( dfac.fromInteger(20) );
          //b = b.multiply( dfac.univariate(0) ).sum( dfac.fromInteger(168));
          c = a.multiply(b);
          if ( a.degree(0) < 1 || b.degree(0) < 2 ) {
             continue;
          }
 
          ap = PolyUtil.fromIntegerCoefficients(pfac,a);
          if ( !a.degreeVector().equals( ap.degreeVector() ) ) {
             continue;
          }
          bp = PolyUtil.fromIntegerCoefficients(pfac,b);
          if ( !b.degreeVector().equals( bp.degreeVector() ) ) {
             continue;
          }
          cp = PolyUtil.fromIntegerCoefficients(pfac,c);
          if ( !c.degreeVector().equals( cp.degreeVector() ) ) {
             continue;
          }
 
          ap1 = ap; //.monic();
          bp1 = bp; //.monic();
          egcd = ap1.egcd(bp1);
          if ( !egcd[0].isONE() ) {
             continue;
          }
          sp = egcd[1];
          tp = egcd[2];
 
          BigInteger an = a.maxNorm();
          BigInteger bn = b.maxNorm();
          if ( an.compareTo(bn) > 0 ) {
              mi = an;
          } else {
              mi = bn;
          }
 
          System.out.println("a     = " + a);
          System.out.println("b     = " + b);
          System.out.println("c     = " + c);
         System.out.println("mi    = " + mi);
          System.out.println("ap    = " + ap);
          System.out.println("bp    = " + bp);
          System.out.println("cp    = " + cp);
          // System.out.println("ap*bp = " + ap.multiply(bp));
          //System.out.println("gcd   = " + egcd[0]);
          //System.out.println("gcd   = " + ap1.multiply(sp).sum(bp1.multiply(tp)));
          System.out.println("sp    = " + sp);
          System.out.println("tp    = " + tp);
 
          lift = PolyUtil.liftHensel(c,mi,ap,bp,sp,tp);
          a1 = lift[0];
          b1 = lift[1];
          c1 = a1.multiply(b1);
 
          System.out.println("\na     = " + a);
          System.out.println("b     = " + b);
          System.out.println("c     = " + c);
          System.out.println("a1    = " + a1);
          System.out.println("b1    = " + b1);
          System.out.println("a1*b1 = " + c1);
 
          //assertEquals("lift(a mod p) = a",a,a1);
          //assertEquals("lift(b mod p) = b",b,b1);
          assertEquals("lift(a,b) = ab",c,c1);
          break;
      }
  }
 
 }
