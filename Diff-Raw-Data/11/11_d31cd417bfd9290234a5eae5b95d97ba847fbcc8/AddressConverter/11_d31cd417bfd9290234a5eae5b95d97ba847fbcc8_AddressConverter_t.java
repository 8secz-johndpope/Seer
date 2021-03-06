 /*
  * Copyright 2012 Tamas Blummer tamas@bitsofproof.com
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package com.bitsofproof.supernode.core;
 
 import java.math.BigInteger;
 
 public class AddressConverter
 {
 	private final boolean production;
 
 	private static final char[] b58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray ();
 	private static final int[] r58 = new int[256];
	static
 	{
 		for ( int i = 0; i < 256; ++i )
 		{
 			r58[i] = -1;
 		}
 		for ( int i = 0; i < b58.length; ++i )
 		{
 			r58[b58[i]] = i;
 		}
 	}
 
	public AddressConverter (Chain chain)
	{
		production = chain.getGenesis ().getHash ().equals ("000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f");
	}

 	public static String toBase58 (byte[] b)
 	{
 		int lz = 0;
 		while ( b[lz] == 0 && lz < b.length )
 		{
 			++lz;
 		}
 
 		StringBuffer s = new StringBuffer ();
 		BigInteger n = new BigInteger (b);
 		while ( n.compareTo (BigInteger.ZERO) > 0 )
 		{
 			BigInteger[] r = n.divideAndRemainder (BigInteger.valueOf (58));
 			n = r[0];
 			char digit = b58[r[1].intValue ()];
 			s.append (digit);
 		}
 		while ( lz > 0 )
 		{
 			--lz;
 			s.append ("1");
 		}
 		return s.reverse ().toString ();
 	}
 
 	public static byte[] fromBase58 (String s) throws ValidationException
 	{
 		try
 		{
 			boolean leading = true;
 			int lz = 0;
 			BigInteger b = BigInteger.ZERO;
 			for ( char c : s.toCharArray () )
 			{
 				if ( leading && c == '1' )
 				{
 					++lz;
 				}
 				else
 				{
 					leading = false;
 					b = b.multiply (BigInteger.valueOf (58));
 					b = b.add (BigInteger.valueOf (r58[c]));
 				}
 			}
 			byte[] encoded = b.toByteArray ();
 			if ( encoded[0] == 0 )
 			{
 				if ( lz > 0 )
 				{
 					--lz;
 				}
 				else
 				{
 					byte[] e = new byte[encoded.length - 1];
 					System.arraycopy (encoded, 1, e, 0, e.length);
 					encoded = e;
 				}
 			}
 			byte[] result = new byte[encoded.length + lz];
 			System.arraycopy (encoded, 0, result, lz, encoded.length);
 
 			return result;
 		}
 		catch ( ArrayIndexOutOfBoundsException e )
 		{
 			throw new ValidationException ("Invalid character in address");
 		}
 		catch ( Exception e )
 		{
 			throw new ValidationException (e);
 		}
 	}
 
 	public byte[] fromSatoshiStyle (String s) throws ValidationException
 	{
 		try
 		{
 			byte[] raw = fromBase58 (s);
 			if ( production )
 			{
 				if ( raw[0] != 0 && raw[0] != 5 )
 				{ // 5 is multisig
 					throw new ValidationException ("invalid address for this chain");
 				}
 			}
 			byte[] check = Hash.hash (raw, 0, raw.length - 4);
 			for ( int i = 0; i < 4; ++i )
 			{
 				if ( check[i] != raw[raw.length - 4 + i] )
 				{
 					throw new ValidationException ("Address checksum mismatch");
 				}
 			}
 			byte[] keyDigest = new byte[raw.length - 5];
 			System.arraycopy (raw, 1, keyDigest, 0, raw.length - 5);
 			return keyDigest;
 		}
 		catch ( Exception e )
 		{
 			throw new ValidationException (e);
 		}
 	}
 
 	public String toSatoshiStyle (byte[] keyDigest, boolean multisig)
 	{
 		byte[] addressBytes = new byte[1 + keyDigest.length + 4];
 		addressBytes[0] = (byte) (production ? (multisig ? 5 : 0) : (multisig ? 1 : 0xc4));
 		System.arraycopy (keyDigest, 0, addressBytes, 1, keyDigest.length);
 		byte[] check = Hash.hash (addressBytes, 0, keyDigest.length + 1);
 		System.arraycopy (check, 0, addressBytes, keyDigest.length + 1, 4);
 		return toBase58 (addressBytes);
 	}
 }
