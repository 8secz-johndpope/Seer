 package endrov.util;
 
 import java.io.Serializable;
 import java.math.BigDecimal;
 import java.math.BigInteger;
 
 /**
  * Arbitrary precision decimal.
  * Internally a wrapper around BigDecimal, the original turned out to be inconvenient to work with.
  * Disadvantage: requires more memory at the moment, will not benefit from new bigdecimal syntax
  * @author Johan Henriksson
  */
 public class EvDecimal extends Number implements Comparable<EvDecimal>, Serializable
 	{
 	private static final long serialVersionUID=0;
 	private final BigDecimal dec;
 	
 	public EvDecimal(BigInteger val)
 		{
 		dec=new BigDecimal(val);
 		}
 	public EvDecimal(BigDecimal val)
 		{
 		dec=val;
 		}
 	public EvDecimal(double val)
 		{
 		dec=new BigDecimal(val);
 		}
 	public EvDecimal(int val)
 		{
 		dec=new BigDecimal(val);
 		}
 	public EvDecimal(long val)
 		{
 		dec=new BigDecimal(val);
 		}
 	public EvDecimal(String val)
 		{
 		dec=new BigDecimal(val);
 		}
 	
 	
 	public static final EvDecimal ZERO=new EvDecimal(0);
 	public static final EvDecimal ONE=new EvDecimal(1);
 	
 	
 	public EvDecimal abs()
 		{
 		return new EvDecimal(dec.abs());
 		}
 	public EvDecimal add(EvDecimal val)
 		{
 		return new EvDecimal(dec.add(val.dec));
 		}
 	public EvDecimal add(int val)
 		{
 		return new EvDecimal(dec.add(new BigDecimal(val)));
 		}
 	
 	
 	
 	
 	public boolean less(EvDecimal b)
 		{
 		return compareTo(b)==-1;
 		}
 	public boolean lessEqual(EvDecimal b)
 		{
 		return compareTo(b)<=0;
 		}
 	public boolean greater(EvDecimal b)
 		{
 		return compareTo(b)==1;
 		}	
 	public boolean greaterEqual(EvDecimal b)
 		{
 		return compareTo(b)>=0;
 		}	
 	public int compareTo(EvDecimal val)
 		{
 		return dec.compareTo(val.dec);
 		}
 
 	
 	public EvDecimal divide(EvDecimal val)
 		{
		return new EvDecimal(dec.divide(val.dec));
 		}
 	public EvDecimal divide(double val)
 		{
		return new EvDecimal(dec.divide(new BigDecimal(val)));
 		}
 	public double doubleValue()
 		{
 		return dec.doubleValue();
 		}
 	public boolean equals(Object x)
 		{
 		if(x instanceof EvDecimal)
 			return dec.equals(((EvDecimal)x).dec);
 		else
 			return false;
 		}
 	public float floatValue()
 		{
 		return dec.floatValue();
 		}
 	public int hashCode()
 		{
 		return dec.hashCode();
 		}
 	public int intValue()
 		{
 		return dec.intValue();
 		}
 	public long longValue()
 		{
 		return dec.longValue();
 		}
 
 	public EvDecimal max(EvDecimal val)
 		{
 		return new EvDecimal(dec.max(val.dec));
 		}
 	public EvDecimal min(EvDecimal val)
 		{
 		return new EvDecimal(dec.max(val.dec));
 		}
 	
 
 	public EvDecimal multiply(EvDecimal val)
 		{
 		return new EvDecimal(dec.multiply(val.dec));
 		}
 	public EvDecimal multiply(double val)
 		{
 		return new EvDecimal(dec.multiply(new BigDecimal(val)));
 		}
 	public EvDecimal negate()
 		{
 		return new EvDecimal(dec.negate());
 		}
 	public EvDecimal pow(int n)
 		{
 		return new EvDecimal(dec.pow(n));
 		}
 
 	public EvDecimal remainder(EvDecimal divisor)
 		{
 		return new EvDecimal(dec.remainder(divisor.dec));
 		}
 
 	public int signum()
 		{
 		return dec.signum();
 		}
 
 	
 	public EvDecimal subtract(EvDecimal val)
 		{
 		return new EvDecimal(dec.subtract(val.dec));
 		}
 	public EvDecimal subtract(int val)
 		{
 		return new EvDecimal(dec.subtract(new BigDecimal(val)));
 		}
 
 	public BigInteger toBigInteger()
 		{
 		return dec.toBigInteger();
 		}
 	public BigDecimal toBigDecimal()
 		{
 		return dec;
 		}
 
 	
 	
 	
 	
 	public String toString()
 		{
 		return dec.stripTrailingZeros().toPlainString();
 		}
 	
 	
 	public static void main(String[] arg)
 		{
 		EvDecimal a=new EvDecimal("0002234234324324234657657656765.123000");
 		System.out.println(a);
 		}
 	
 	}
