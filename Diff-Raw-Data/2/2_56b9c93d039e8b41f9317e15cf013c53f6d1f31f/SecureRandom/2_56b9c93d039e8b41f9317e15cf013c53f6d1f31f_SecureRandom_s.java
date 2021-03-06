 /*
  *  Licensed to the Apache Software Foundation (ASF) under one or more
  *  contributor license agreements.  See the NOTICE file distributed with
  *  this work for additional information regarding copyright ownership.
  *  The ASF licenses this file to You under the Apache License, Version 2.0
  *  (the "License"); you may not use this file except in compliance with
  *  the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */
 
 package java.security;
 
 import java.nio.ByteOrder;
 import java.util.Random;
 import libcore.io.SizeOf;
 import org.apache.harmony.luni.platform.OSMemory;
 import org.apache.harmony.security.fortress.Engine;
 import org.apache.harmony.security.fortress.Services;
 import org.apache.harmony.security.provider.crypto.SHA1PRNG_SecureRandomImpl;
 
 /**
  * This class generates cryptographically secure pseudo-random numbers.
  *
  * <h3>Supported Algorithms</h3>
  * <ul>
  *   <li><strong>SHA1PRNG</strong>: Based on <a
  *     href="http://en.wikipedia.org/wiki/SHA-1">SHA-1</a>. Not guaranteed to be
  *     compatible with the SHA1PRNG algorithm on the reference
  *     implementation.</li>
  * </ul>
  *
  * <p>The default algorithm is defined by the first {@code SecureRandomSpi}
  * provider found in the VM's installed security providers. Use {@link
  * Security} to install custom {@link SecureRandomSpi} providers.
  *
  * <a name="insecure_seed"><h3>Seeding {@code SecureRandom} may be
  * insecure</h3></a>
  * A seed is an array of bytes used to bootstrap random number generation.
  * To produce cryptographically secure random numbers, both the seed and the
  * algorithm must be secure.
  *
  * <p>By default, instances of this class will generate an initial seed using
  * an internal entropy source, such as {@code /dev/urandom}. This seed is
  * unpredictable and appropriate for secure use.
  *
  * <p>You may alternatively specify the initial seed explicitly with the
  * {@link #SecureRandom(byte[]) seeded constructor} or by calling {@link
  * #setSeed} before any random numbers have been generated. Specifying a fixed
  * seed will cause the instance to return a predictable sequence of numbers.
  * This may be useful for testing but it is not appropriate for secure use.
  *
  * <p>It is dangerous to seed {@code SecureRandom} with the current time because
  * that value is more predictable to an attacker than the default seed.
  *
  * <p>Calling {@link #setSeed} on a {@code SecureRandom} <i>after</i> it has
 * been used to generate random numbers (ie. calling {#link nextBytes}) will
  * supplement the existing seed. This does not cause the instance to return a
  * predictable numbers, nor does it harm the security of the numbers generated.
  */
 public class SecureRandom extends Random {
 
     private static final long serialVersionUID = 4940670005562187L;
 
     // The service name.
     private static final String SERVICE = "SecureRandom";
 
     // Used to access common engine functionality
     private static final Engine ENGINE = new Engine(SERVICE);
 
     private final Provider provider;
 
     private final SecureRandomSpi secureRandomSpi;
 
     private final String algorithm;
 
     // Internal SecureRandom used for getSeed(int)
     private static volatile SecureRandom internalSecureRandom;
 
     /**
      * Constructs a new {@code SecureRandom} that uses the default algorithm.
      */
     public SecureRandom() {
         super(0);
         Services.refresh();
         Provider.Service service = Services.getSecureRandomService();
         if (service == null) {
             this.provider = null;
             this.secureRandomSpi = new SHA1PRNG_SecureRandomImpl();
             this.algorithm = "SHA1PRNG";
         } else {
             try {
                 this.provider = service.getProvider();
                 this.secureRandomSpi = (SecureRandomSpi)service.newInstance(null);
                 this.algorithm = service.getAlgorithm();
             } catch (Exception e) {
                 throw new RuntimeException(e);
             }
         }
     }
 
     /**
      * Constructs a new seeded {@code SecureRandom} that uses the default
      * algorithm. <a href="#insecure_seed">Seeding {@code SecureRandom} may be
      * insecure</a>.
      */
     public SecureRandom(byte[] seed) {
         this();
         setSeed(seed);
     }
 
     /**
      * Constructs a new instance of {@code SecureRandom} using the given
      * implementation from the specified provider.
      *
      * @param secureRandomSpi
      *            the implementation.
      * @param provider
      *            the security provider.
      */
     protected SecureRandom(SecureRandomSpi secureRandomSpi,
                            Provider provider) {
         this(secureRandomSpi, provider, "unknown");
     }
 
     // Constructor
     private SecureRandom(SecureRandomSpi secureRandomSpi,
                          Provider provider,
                          String algorithm) {
         super(0);
         this.provider = provider;
         this.algorithm = algorithm;
         this.secureRandomSpi = secureRandomSpi;
     }
 
     /**
      * Returns a new instance of {@code SecureRandom} that utilizes the
      * specified algorithm.
      *
      * @param algorithm
      *            the name of the algorithm to use.
      * @return a new instance of {@code SecureRandom} that utilizes the
      *         specified algorithm.
      * @throws NoSuchAlgorithmException
      *             if the specified algorithm is not available.
      * @throws NullPointerException
      *             if {@code algorithm} is {@code null}.
      */
     public static SecureRandom getInstance(String algorithm)
                                 throws NoSuchAlgorithmException {
         if (algorithm == null) {
             throw new NullPointerException();
         }
         Engine.SpiAndProvider sap = ENGINE.getInstance(algorithm, null);
         return new SecureRandom((SecureRandomSpi) sap.spi, sap.provider,
                                 algorithm);
     }
 
     /**
      * Returns a new instance of {@code SecureRandom} that utilizes the
      * specified algorithm from the specified provider.
      *
      * @param algorithm
      *            the name of the algorithm to use.
      * @param provider
      *            the name of the provider.
      * @return a new instance of {@code SecureRandom} that utilizes the
      *         specified algorithm from the specified provider.
      * @throws NoSuchAlgorithmException
      *             if the specified algorithm is not available.
      * @throws NoSuchProviderException
      *             if the specified provider is not available.
      * @throws NullPointerException
      *             if {@code algorithm} is {@code null}.
      * @throws IllegalArgumentException if {@code provider == null || provider.isEmpty()}
      */
     public static SecureRandom getInstance(String algorithm, String provider)
                                 throws NoSuchAlgorithmException, NoSuchProviderException {
         if (provider == null || provider.isEmpty()) {
             throw new IllegalArgumentException();
         }
         Provider p = Security.getProvider(provider);
         if (p == null) {
             throw new NoSuchProviderException(provider);
         }
         return getInstance(algorithm, p);
     }
 
     /**
      * Returns a new instance of {@code SecureRandom} that utilizes the
      * specified algorithm from the specified provider.
      *
      * @param algorithm
      *            the name of the algorithm to use.
      * @param provider
      *            the security provider.
      * @return a new instance of {@code SecureRandom} that utilizes the
      *         specified algorithm from the specified provider.
      * @throws NoSuchAlgorithmException
      *             if the specified algorithm is not available.
      * @throws NullPointerException
      *             if {@code algorithm} is {@code null}.
      * @throws IllegalArgumentException if {@code provider == null}
      */
     public static SecureRandom getInstance(String algorithm, Provider provider)
                                 throws NoSuchAlgorithmException {
         if (provider == null) {
             throw new IllegalArgumentException();
         }
         if (algorithm == null) {
             throw new NullPointerException();
         }
         Object spi = ENGINE.getInstance(algorithm, provider, null);
         return new SecureRandom((SecureRandomSpi) spi, provider, algorithm);
     }
 
     /**
      * Returns the provider associated with this {@code SecureRandom}.
      *
      * @return the provider associated with this {@code SecureRandom}.
      */
     public final Provider getProvider() {
         return provider;
     }
 
     /**
      * Returns the name of the algorithm of this {@code SecureRandom}.
      *
      * @return the name of the algorithm of this {@code SecureRandom}.
      */
     public String getAlgorithm() {
         return algorithm;
     }
 
     /**
      * Seeds this {@code SecureRandom} instance with the specified {@code
      * seed}. <a href="#insecure_seed">Seeding {@code SecureRandom} may be
      * insecure</a>.
      */
     public synchronized void setSeed(byte[] seed) {
         secureRandomSpi.engineSetSeed(seed);
     }
 
     /**
      * Seeds this {@code SecureRandom} instance with the specified eight-byte
      * {@code seed}. <a href="#insecure_seed">Seeding {@code SecureRandom} may
      * be insecure</a>.
      */
     @Override
     public void setSeed(long seed) {
         if (seed == 0) {    // skip call from Random
             return;
         }
         byte[] byteSeed = new byte[SizeOf.LONG];
         OSMemory.pokeLong(byteSeed, 0, seed, ByteOrder.BIG_ENDIAN);
         setSeed(byteSeed);
     }
 
     /**
      * Generates and stores random bytes in the given {@code byte[]} for each
      * array element.
      *
      * @param bytes
      *            the {@code byte[]} to be filled with random bytes.
      */
     @Override
     public synchronized void nextBytes(byte[] bytes) {
         secureRandomSpi.engineNextBytes(bytes);
     }
 
     /**
      * Generates and returns an {@code int} containing the specified number of
      * random bits (right justified, with leading zeros).
      *
      * @param numBits
      *            number of bits to be generated. An input value should be in
      *            the range [0, 32].
      * @return an {@code int} containing the specified number of random bits.
      */
     @Override
     protected final int next(int numBits) {
         if (numBits < 0) {
             numBits = 0;
         } else {
             if (numBits > 32) {
                 numBits = 32;
             }
         }
         int bytes = (numBits+7)/8;
         byte[] next = new byte[bytes];
         int ret = 0;
 
         nextBytes(next);
         for (int i = 0; i < bytes; i++) {
             ret = (next[i] & 0xFF) | (ret << 8);
         }
         ret = ret >>> (bytes*8 - numBits);
         return ret;
     }
 
     /**
      * Generates and returns the specified number of seed bytes, computed using
      * the seed generation algorithm used by this {@code SecureRandom}.
      *
      * @param numBytes
      *            the number of seed bytes.
      * @return the seed bytes
      */
     public static byte[] getSeed(int numBytes) {
         SecureRandom result = internalSecureRandom;
         if (result == null) {
             // single-check idiom
             internalSecureRandom = result = new SecureRandom();
         }
         return result.generateSeed(numBytes);
     }
 
     /**
      * Generates and returns the specified number of seed bytes, computed using
      * the seed generation algorithm used by this {@code SecureRandom}.
      *
      * @param numBytes
      *            the number of seed bytes.
      * @return the seed bytes.
      */
     public byte[] generateSeed(int numBytes) {
         return secureRandomSpi.engineGenerateSeed(numBytes);
     }
 
 }
