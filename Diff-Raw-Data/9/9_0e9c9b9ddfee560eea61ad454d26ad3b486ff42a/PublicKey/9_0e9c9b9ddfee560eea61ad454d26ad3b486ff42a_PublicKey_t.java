 package org.twuni.common.crypto.rsa;
 
 import java.math.BigInteger;
 
 import org.twuni.common.crypto.Base64;
 import org.twuni.common.crypto.Transformer;
 
 public class PublicKey implements Transformer<BigInteger, BigInteger> {
 
 	private final BigInteger modulus;
 	private final BigInteger exponent;
 
 	public PublicKey( BigInteger modulus, BigInteger exponent ) {
 		this.modulus = modulus;
 		this.exponent = exponent;
 	}
 
 	public BigInteger getModulus() {
 		return modulus;
 	}
 
 	public BigInteger getExponent() {
 		return exponent;
 	}
 
 	/**
 	 * Transforms the given input by calling
 	 * <code>input.modPow( {@link #getExponent()}, {@link #getModulus()} )</code>.
 	 * 
 	 * @param input
 	 *            A number less than {@link #getModulus()} that represents either a block of
 	 *            plaintext or a block that has been encrypted with the this key's corresponding
 	 *            private key.
 	 */
 	@Override
 	public BigInteger transform( BigInteger input ) {
 		return input.modPow( exponent, modulus );
 	}
 
 	/**
 	 * Serializes the essential fields of this key as a newline-delimited, base64-encoded string.
 	 */
 	public String serialize() {
 
 		StringBuilder string = new StringBuilder();
 
 		string.append( Base64.encode( modulus.toByteArray() ) ).append( "|" );
 		string.append( Base64.encode( exponent.toByteArray() ) );
 
 		return string.toString();
 
 	}
 
 	/**
 	 * Constructs a new RSA public key using the given serial, generated by calling
 	 * {@link #serialize()}.
 	 * 
 	 * @param serial
 	 *            The base64-encoded serialization of the public key, obtained by calling
 	 *            {@link #serialize()}.
 	 */
 	public static PublicKey deserialize( String serial ) {
 
		String [] args = serial.split( "\\|" );
 
 		BigInteger modulus = new BigInteger( Base64.decode( args[0] ) );
 		BigInteger exponent = new BigInteger( Base64.decode( args[1] ) );
 
 		return new PublicKey( modulus, exponent );
 
 	}
 
 	@Override
 	public boolean equals( Object object ) {
 		if( object instanceof PublicKey ) {
 			PublicKey key = (PublicKey) object;
 			return modulus.equals( key.modulus ) && exponent.equals( key.exponent );
 		}
 		return false;
 	}
 
 	@Override
 	public String toString() {
 		return serialize();
 	}
 
 }
