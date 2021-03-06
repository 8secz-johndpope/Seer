 package de.gebit.integrity.tests.fixtures.basic;
 
 import java.math.BigDecimal;
 import java.math.BigInteger;
 
 import de.gebit.integrity.fixtures.FixtureMethod;
 import de.gebit.integrity.fixtures.FixtureParameter;
 
 /**
  * A simple test fixture which does nothing (except echoing some input).
  * 
  * 
  * @author Rene Schneider
  * 
  */
 // SUPPRESS CHECKSTYLE LONG Javadoc
 public class NoOpFixture {
 
 	@FixtureMethod(description = "Do absolutely nothing.")
 	public void noOp() {
 		// does nothing!
 	}
 
 	@FixtureMethod(description = "Always return true.")
 	public boolean returnTrue() {
 		return true;
 	}
 
 	@FixtureMethod(description = "Echo the string '$string$'")
 	public String echoString(@FixtureParameter(name = "string") String aStringToEcho) {
 		return aStringToEcho;
 	}
 
	@FixtureMethod(description = "Echo the string array '$strings$'")
	public String[] echoStringArray(@FixtureParameter(name = "strings") String[] someStringsToEcho) {
		return someStringsToEcho;
	}

 	@FixtureMethod(description = "Echo the integer '$integer$'")
 	public Integer echoInteger(@FixtureParameter(name = "integer") Integer anIntToEcho) {
 		return anIntToEcho;
 	}
 
 	@FixtureMethod(description = "Echo the short '$short$'")
 	public Short echoShort(@FixtureParameter(name = "short") Short aShortToEcho) {
 		return aShortToEcho;
 	}
 
 	@FixtureMethod(description = "Echo the byte '$byte$'")
 	public Byte echoByte(@FixtureParameter(name = "byte") Byte aByteToEcho) {
 		return aByteToEcho;
 	}
 
 	@FixtureMethod(description = "Echo the long '$long$'")
 	public Long echoLong(@FixtureParameter(name = "long") Long aLongToEcho) {
 		return aLongToEcho;
 	}
 
 	@FixtureMethod(description = "Echo the BigDecimal '$bigdecimal$'")
 	public BigDecimal echoBigDecimal(@FixtureParameter(name = "bigdecimal") BigDecimal aBigDecimalToEcho) {
 		return aBigDecimalToEcho;
 	}
 
 	@FixtureMethod(description = "Echo the BigInteger '$biginteger$'")
 	public BigInteger echoBigInteger(@FixtureParameter(name = "biginteger") BigInteger aBigIntegerToEcho) {
 		return aBigIntegerToEcho;
 	}
 
 }
