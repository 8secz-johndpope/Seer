 package org.mitre.jwt;
 
 import java.io.ByteArrayInputStream;
 import java.io.InputStreamReader;
 import java.util.List;
 
 import org.apache.commons.codec.binary.Base64;
 
 import com.google.common.base.Splitter;
 import com.google.common.base.Strings;
 import com.google.common.collect.Lists;
 import com.google.gson.GsonBuilder;
 import com.google.gson.JsonObject;
 import com.google.gson.JsonParser;
 
 public class Jwt {
 
 	private JwtHeader header;
 	
 	private JwtClaims claims;
 
 	/**
 	 * Base64Url encoded signature string
 	 */
 	private String signature;
 
 
 	
 	
 	public Jwt() {
 	    this.header = new JwtHeader();
 	    this.claims = new JwtClaims();
 	    this.signature = null; // unsigned by default
     }
 
 
 
 	/**
 	 * Create a Jwt from existing components
 	 * @param header
 	 * @param claims
 	 * @param signature
 	 */
 	public Jwt(JwtHeader header, JwtClaims claims, String signature) {
 	    super();
 	    this.header = header;
 	    this.claims = claims;
 	    this.signature = signature;
     }
 
 
 
 	/**
      * @return the header
      */
     public JwtHeader getHeader() {
     	return header;
     }
 
 
 
 	/**
      * @param header the header to set
      */
     public void setHeader(JwtHeader header) {
     	this.header = header;
     }
 
 
 
 	/**
      * @return the claims
      */
     public JwtClaims getClaims() {
     	return claims;
     }
 
 
 
 	/**
      * @param claims the claims to set
      */
     public void setClaims(JwtClaims claims) {
     	this.claims = claims;
     }
 
 
 
 	/**
      * @return the signature
      */
     public String getSignature() {
     	return signature;
     }
 
 
 
 	/**
      * @param signature the signature to set
      */
     public void setSignature(String signature) {
     	this.signature = signature;
     }
 	
 	/**
	 * Return the canonical encoded string of this JWT, the header in Base64, a period ".", the claims in Base64, a period ".", and the signature in Base64.
 	 */
 	public String toString() {
		return getSignatureBase() + "." + Strings.nullToEmpty(this.signature);
 	}
 
	/**
	 * The signature base of a JWT is the header in Base64, a period ".", and the claims in Base64.
	 */
 	public String getSignatureBase() {
 		JsonObject h = header.getAsJsonObject();
 		JsonObject c = claims.getAsJsonObject();
 
 		String h64 = new String(Base64.encodeBase64URLSafe(h.toString().getBytes()));
 		String c64 = new String(Base64.encodeBase64URLSafe(c.toString().getBytes()));
 		
		return h64 + "." + c64;		
 	}
 	
 
 	/**
 	 * Parse a wire-encoded JWT
 	 */
 	public static Jwt parse(String s) {
 		
 		// split on the dots
 		List<String> parts = Lists.newArrayList(Splitter.on(".").split(s));
 		
 		if (parts.size() != 3) {
 			throw new IllegalArgumentException("Invalid JWT format.");
 		}
 		
 		String h64 = parts.get(0);
 		String c64 = parts.get(1);
 		String s64 = parts.get(2);
 		
 		JsonParser parser = new JsonParser();
 		JsonObject hjo = parser.parse(new InputStreamReader(new ByteArrayInputStream(Base64.decodeBase64(h64)))).getAsJsonObject();
 		JsonObject cjo = parser.parse(new InputStreamReader(new ByteArrayInputStream(Base64.decodeBase64(c64)))).getAsJsonObject();		
 		
 		// shuttle for return value
 		Jwt jwt = new Jwt(new JwtHeader(hjo), new JwtClaims(cjo), s64);
 		
		// TODO: save the wire-encoded string in the Jwt object itself?
		
 		return jwt;
 		
 	}
 	
 }
