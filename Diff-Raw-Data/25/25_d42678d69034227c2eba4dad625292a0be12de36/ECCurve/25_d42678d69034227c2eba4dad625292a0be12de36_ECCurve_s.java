 package org.bouncycastle.math.ec;
 
 import java.math.BigInteger;
 
 /**
  * base class for an elliptic curve
  */
 public abstract class ECCurve
 {
     ECFieldElement a, b;
 
     public abstract ECFieldElement fromBigInteger(BigInteger x);
 
     public abstract ECPoint decodePoint(byte[] encoded);
 
     public ECFieldElement getA()
     {
         return a;
     }
 
     public ECFieldElement getB()
     {
         return b;
     }
 
     /**
      * Elliptic curve over Fp
      */
     public static class Fp extends ECCurve
     {
         BigInteger q;
         
         public Fp(BigInteger q, BigInteger a, BigInteger b)
         {
             this.q = q;
             this.a = fromBigInteger(a);
             this.b = fromBigInteger(b);
         }
 
         public BigInteger getQ()
         {
             return q;
         }
 
         public ECFieldElement fromBigInteger(BigInteger x)
         {
             return new ECFieldElement.Fp(this.q, x);
         }
 
         /**
         * decode a point on this curve which has been encoded using
         * point compression (X9.62 s 4.2.1 pg 17) returning the point.
          */
         public ECPoint decodePoint(byte[] encoded)
         {
             ECPoint p = null;
 
             switch (encoded[0])
             {
                 // compressed
             case 0x02:
             case 0x03:
                 int ytilde = encoded[0] & 1;
                 byte[]  i = new byte[encoded.length - 1];
 
                 System.arraycopy(encoded, 1, i, 0, i.length);
 
                 ECFieldElement x = new ECFieldElement.Fp(this.q, new BigInteger(1, i));
                 ECFieldElement alpha = x.multiply(x.square()).add(x.multiply(a).add(b));
                 ECFieldElement beta = alpha.sqrt();
 
                 //
                 // if we can't find a sqrt we haven't got a point on the
                 // curve - run!
                 //
                 if (beta == null)
                 {
                     throw new RuntimeException("Invalid point compression");
                 }
 
                 int bit0 = (beta.toBigInteger().testBit(0) ? 1 : 0);
 
                 if (bit0 == ytilde)
                 {
                     p = new ECPoint.Fp(this, x, beta);
                 }
                 else
                 {
                     p = new ECPoint.Fp(this, x,
                         new ECFieldElement.Fp(this.q, q.subtract(beta.toBigInteger())));
                 }
                 break;
             case 0x04:
                 byte[]  xEnc = new byte[(encoded.length - 1) / 2];
                 byte[]  yEnc = new byte[(encoded.length - 1) / 2];
 
                 System.arraycopy(encoded, 1, xEnc, 0, xEnc.length);
                 System.arraycopy(encoded, xEnc.length + 1, yEnc, 0, yEnc.length);
 
                 p = new ECPoint.Fp(this,
                         new ECFieldElement.Fp(this.q, new BigInteger(1, xEnc)),
                         new ECFieldElement.Fp(this.q, new BigInteger(1, yEnc)));
                 break;
             default:
                 throw new RuntimeException("Invalid point encoding 0x" + Integer.toString(encoded[0], 16));
             }
 
             return p;
         }
     }
 
     /**
      * Elliptic curves over F2m. The Weierstrass equation is given by
      * <code>y<sup>2</sup> + xy = x<sup>3</sup> + ax<sup>2</sup> + b</code>.
      */
     public static class F2m extends ECCurve
     {
         /**
          * The exponent <code>m</code> of <code>F<sub>2<sup>m</sup></sub></code>.
          */
         private final int m;
 
         /**
          * TPB: The integer <code>k</code> where <code>x<sup>m</sup> +
          * x<sup>k</sup> + 1</code> represents the reduction polynomial
          * <code>f(z)</code>.<br>
          * PPB: The integer <code>k1</code> where <code>x<sup>m</sup> +
          * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
          * represents the reduction polynomial <code>f(z)</code>.<br>
          */
         private final int k1;
 
         /**
          * TPB: Always set to <code>0</code><br>
          * PPB: The integer <code>k2</code> where <code>x<sup>m</sup> +
          * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
          * represents the reduction polynomial <code>f(z)</code>.<br>
          */
         private final int k2;
 
         /**
          * TPB: Always set to <code>0</code><br>
          * PPB: The integer <code>k3</code> where <code>x<sup>m</sup> +
          * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
          * represents the reduction polynomial <code>f(z)</code>.<br>
          */
         private final int k3;
 
         /**
          * Constructor for Trinomial Polynomial Basis (TPB).
          * @param m  The exponent <code>m</code> of
          * <code>F<sub>2<sup>m</sup></sub></code>.
          * @param k The integer <code>k</code> where <code>x<sup>m</sup> +
          * x<sup>k</sup> + 1</code> represents the reduction
          * polynomial <code>f(z)</code>.
          * @param a The coefficient <code>a</code> in the Weierstrass equation
          * for non-supersingular elliptic curves over
          * <code>F<sub>2<sup>m</sup></sub></code>.
          * @param b The coefficient <code>b</code> in the Weierstrass equation
          * for non-supersingular elliptic curves over
          * <code>F<sub>2<sup>m</sup></sub></code>.
          */
         public F2m(
             int m, 
             int k, 
             BigInteger a, 
             BigInteger b)
         {
             this(m, k, 0, 0, a, b);
         }
 
         /**
          * Constructor for Pentanomial Polynomial Basis (PPB).
          * @param m  The exponent <code>m</code> of
          * <code>F<sub>2<sup>m</sup></sub></code>.
          * @param k1 The integer <code>k1</code> where <code>x<sup>m</sup> +
          * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
          * represents the reduction polynomial <code>f(z)</code>.
          * @param k2 The integer <code>k2</code> where <code>x<sup>m</sup> +
          * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
          * represents the reduction polynomial <code>f(z)</code>.
          * @param k3 The integer <code>k3</code> where <code>x<sup>m</sup> +
          * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
          * represents the reduction polynomial <code>f(z)</code>.
          * @param a The coefficient <code>a</code> in the Weierstrass equation
          * for non-supersingular elliptic curves over
          * <code>F<sub>2<sup>m</sup></sub></code>.
          * @param b The coefficient <code>b</code> in the Weierstrass equation
          * for non-supersingular elliptic curves over
          * <code>F<sub>2<sup>m</sup></sub></code>.
          */
         public F2m(
             int m, 
             int k1, 
             int k2, 
             int k3,
             BigInteger a, 
             BigInteger b)
         {
             this.m = m;
             this.k1 = k1;
             this.k2 = k2;
             this.k3 = k3;
             
             if (k1 == 0)
             {
                 throw new IllegalArgumentException("k1 must be > 0");
             }
             
            if (k2 <= k1)
             {
                throw new IllegalArgumentException("k2 must be > k1");
            }
            
            if (k3 <= k2)
            {
                throw new IllegalArgumentException("k3 must be > k2");
             }
             
             this.a = fromBigInteger(a);
             this.b = fromBigInteger(b);
         }
         
         public ECFieldElement fromBigInteger(BigInteger x)
         {
             return new ECFieldElement.F2m(this.m, this.k1, this.k2, this.k3, x);
         }
         
         /* (non-Javadoc)
          * @see org.bouncycastle.math.ec.ECCurve#decodePoint(byte[])
          */
         public ECPoint decodePoint(byte[] encoded)
         {
             ECPoint p = null;
 
             switch (encoded[0])
             {
                 // compressed
             case 0x02:
             case 0x03:
                 throw new RuntimeException("Point compression for F2m not " +
                         "supported");
             case 0x04:
                 byte[] xEnc = new byte[(encoded.length - 1) / 2];
                 byte[] yEnc = new byte[(encoded.length - 1) / 2];
 
                 System.arraycopy(encoded, 1, xEnc, 0, xEnc.length);
                 System.arraycopy(encoded, xEnc.length + 1, yEnc, 0, yEnc.length);
 
                 p = new ECPoint.F2m(this,
                     new ECFieldElement.F2m(this.m, this.k1, this.k2, this.k3,
                         new BigInteger(1, xEnc)),
                     new ECFieldElement.F2m(this.m, this.k1, this.k2, this.k3,
                         new BigInteger(1, yEnc)));
                 break;
 
             default:
                 throw new RuntimeException("Invalid point encoding 0x" + Integer.toString(encoded[0], 16));
             }
 
             return p;
         }
 
         public boolean equals(Object anObject)
         {
             if (anObject == this) 
             {
                 return true;
             }
 
             if (!(anObject instanceof ECCurve.F2m)) 
             {
                 return false;
             }
 
             ECCurve.F2m other = (ECCurve.F2m)anObject;
             
             return ((this.m == other.m) && (this.k1 == other.k1)
                 && (this.k2 == other.k2) && (this.k3 == other.k3)
                 && (a.equals(other.a)) && (b.equals(other.b)));
         }
 
         public int hashCode()
         {
             return a.hashCode() ^ b.hashCode() ^ m ^ k1 ^ k2 ^ k3;
         }
 
         public int getM()
         {
             return m;
         }
 
         /**
          * Return true if curve uses a Trinomial basis.
          * 
          * @return true if curve Trinomial, false otherwise.
          */
         public boolean isTrinomial()
         {
             return k2 == 0 && k3 == 0;
         }
         
         public int getK1()
         {
             return k1;
         }
 
         public int getK2()
         {
             return k2;
         }
 
         public int getK3()
         {
             return k3;
         }    
     }
 }
