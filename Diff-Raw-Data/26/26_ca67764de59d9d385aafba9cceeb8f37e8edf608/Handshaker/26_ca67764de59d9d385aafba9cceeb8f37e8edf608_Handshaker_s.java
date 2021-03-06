 /*******************************************************************************
  * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
  * All rights reserved.
  * 
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  * 1. Redistributions of source code must retain the above copyright
  *    notice, this list of conditions and the following disclaimer.
  * 2. Redistributions in binary form must reproduce the above copyright
  *    notice, this list of conditions and the following disclaimer in the
  *    documentation and/or other materials provided with the distribution.
  * 3. Neither the name of the Institute nor the names of its contributors
  *    may be used to endorse or promote products derived from this software
  *    without specific prior written permission.
  * 
  * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
  * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
  * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
  * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
  * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
  * SUCH DAMAGE.
  * 
  * This file is part of the Californium (Cf) CoAP framework.
  ******************************************************************************/
 
 package ch.ethz.inf.vs.californium.dtls;
 
import java.io.File;
 import java.io.InputStream;
import java.io.RandomAccessFile;
 import java.security.KeyFactory;
 import java.security.MessageDigest;
 import java.security.NoSuchAlgorithmException;
 import java.security.PrivateKey;
 import java.security.cert.Certificate;
 import java.security.cert.CertificateFactory;
 import java.security.cert.X509Certificate;
 import java.security.spec.PKCS8EncodedKeySpec;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.logging.Logger;
 
 import javax.crypto.SecretKey;
 import javax.crypto.spec.IvParameterSpec;
 import javax.crypto.spec.SecretKeySpec;
 
 import ch.ethz.inf.vs.californium.coap.EndpointAddress;
 import ch.ethz.inf.vs.californium.coap.Message;
 import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertDescription;
 import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertLevel;
 import ch.ethz.inf.vs.californium.dtls.CipherSuite.KeyExchangeAlgorithm;
 import ch.ethz.inf.vs.californium.util.ByteArrayUtils;
 import ch.ethz.inf.vs.californium.util.Properties;
 
 public abstract class Handshaker {
 
 	// Logging ////////////////////////////////////////////////////////
 
 	protected static final Logger LOG = Logger.getLogger(Handshaker.class.getName());
 
 	// Static members /////////////////////////////////////////////////
 
 	private final static int MASTER_SECRET_LABEL = 1;
 
 	private final static int KEY_EXPANSION_LABEL = 2;
 
 	public final static int CLIENT_FINISHED_LABEL = 3;
 
 	public final static int SERVER_FINISHED_LABEL = 4;
 
 	public final static int TEST_LABEL = 5;
 
 	public final static int TEST_LABEL_2 = 6;
 
 	public final static int TEST_LABEL_3 = 7;
 
 	/**
 	 * A map storing shared keys. The shared key is associated with an PSK
 	 * identity. See <a href="http://tools.ietf.org/html/rfc4279#section-2">RFC
 	 * 4279</a> for details.
 	 */
 	protected static Map<String, byte[]> sharedKeys = new HashMap<String, byte[]>();
 
 	static {
 		sharedKeys.put("TEST", new byte[] { 0x73, 0x65, 0x63, 0x72, 0x65, 0x74, 0x50, 0x53, 0x4b });
 		sharedKeys.put("001", new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 });
 	}
 
 	// Members ////////////////////////////////////////////////////////
 
 	/**
 	 * Indicates, whether the handshake protocol is performed from the client's
 	 * side or the server's.
 	 */
 	protected boolean isClient;
 
 	/**
 	 * Indicates whether only the raw public key is sent or a full X.509
 	 * certificates.
 	 */
 	protected static boolean useRawPublicKey = Properties.std.getBool("USE_RAW_PUBLIC_KEY");
 
 	protected int state = -1;
 
 	protected EndpointAddress endpointAddress;
 
 	protected ProtocolVersion usedProtocol;
 	protected Random clientRandom;
 	protected Random serverRandom;
 	private CipherSuite cipherSuite;
 	private CompressionMethod compressionMethod;
 
 	protected KeyExchangeAlgorithm keyExchange;
 
 	/** The helper class to execute the ECDHE key agreement and key generation. */
 	protected ECDHECryptography ecdhe;
 
 	private byte[] masterSecret;
 
 	private SecretKey clientWriteMACKey;
 	private SecretKey serverWriteMACKey;
 
 	private IvParameterSpec clientWriteIV;
 	private IvParameterSpec serverWriteIV;
 
 	private SecretKey clientWriteKey;
 	private SecretKey serverWriteKey;
 
 	protected DTLSSession session = null;
 
 	/**
 	 * The current sequence number (in the handshake message called message_seq)
 	 * for this handshake.
 	 */
 	private int sequenceNumber = 0;
 
 	/** The next expected handshake message sequence number. */
 	private int nextReceiveSeq = 0;
 
 	/** The CoAP {@link Message} that needs encryption. */
	protected Message message;
 
 	/** Queue for messages, that can not yet be processed. */
 	protected Collection<Record> queuedMessages;
 
 	/**
 	 * The message digest to compute the handshake hashes sent in the
 	 * {@link Finished} messages.
 	 */
 	protected MessageDigest md;
 
 	/** All the handshake messages sent before the CertificateVerify message. */
 	protected byte[] handshakeMessages = new byte[] {};
 
 	/**
 	 * The last flight that is sent during this handshake, will not be
 	 * retransmitted unless the peer retransmits its last flight.
 	 */
 	protected DTLSFlight lastFlight = null;
 
 	/** The handshaker's private key. */
 	protected PrivateKey privateKey;
 
 	/** The handshaker's certificate. */
 	protected X509Certificate[] certificates;
 
 	// Constructor ////////////////////////////////////////////////////
 
 	/**
 	 * 
 	 * @param peerAddress
 	 * @param isClient
 	 * @param session
 	 */
 	public Handshaker(EndpointAddress peerAddress, boolean isClient, DTLSSession session) {
 		this.endpointAddress = peerAddress;
 		this.isClient = isClient;
 		this.session = session;
 		this.queuedMessages = new HashSet<Record>();
 		this.privateKey = loadPrivateKey("ec.pk8");
 		this.certificates = loadCertificate("ec.crt");
 		try {
 			this.md = MessageDigest.getInstance("SHA-256");
 		} catch (NoSuchAlgorithmException e) {
 			LOG.severe("Could not initialize the message digest algorithm.");
 			e.printStackTrace();
 		}
 	}
 
 	// Abstract Methods ///////////////////////////////////////////////
 
 	/**
 	 * Processes the handshake message according to its {@link HandshakeType}
 	 * and reacts according to the protocol specification.
 	 * 
 	 * @param message
 	 *            the received {@link HandshakeMessage}.
 	 * @return the list all handshake messages that need to be sent triggered by
 	 *         this message.
 	 */
 	public abstract DTLSFlight processMessage(Record message);
 
 	/**
 	 * Gets the handshake flight which needs to be sent first to initiate
 	 * handshake. This differs from client side to server side.
 	 * 
 	 * @return the handshake message to start off the handshake protocol.
 	 */
 	public abstract DTLSFlight getStartHandshakeMessage();
 
 	// Methods ////////////////////////////////////////////////////////
 
 	/**
 	 * First, generates the master secret from the given premaster secret and
 	 * then applying the key expansion on the master secret generates a large
 	 * enough key block to generate the write, MAC and IV keys. See <a
 	 * href="http://tools.ietf.org/html/rfc5246#section-6.3">RFC 5246</a> for
 	 * further details about the keys.
 	 * 
 	 * @param premasterSecret
 	 *            the shared premaster secret.
 	 */
 	protected void generateKeys(byte[] premasterSecret) {
 		masterSecret = generateMasterSecret(premasterSecret);
 		System.out.println("Master secret: " + Arrays.toString(masterSecret));
 		session.setMasterSecret(masterSecret);
 		LOG.fine("Generated master secret from premaster secret: " + Arrays.toString(masterSecret));
 
 		calculateKeys(masterSecret);
 	}
 
 	/**
 	 * Calculates the encryption key, MAC key and IV from a given master secret.
 	 * First, applies the key expansion to the master secret.
 	 * 
 	 * @param masterSecret
 	 *            the master secret.
 	 */
 	private void calculateKeys(byte[] masterSecret) {
 		/*
 		 * See http://tools.ietf.org/html/rfc5246#section-6.3: key_block =
 		 * PRF(SecurityParameters.master_secret, "key expansion",
 		 * SecurityParameters.server_random + SecurityParameters.client_random);
 		 */
 
 		byte[] data = doPRF(masterSecret, KEY_EXPANSION_LABEL, ByteArrayUtils.concatenate(serverRandom.getRandomBytes(), clientRandom.getRandomBytes()));
 
 		/*
 		 * Create keys as suggested in
 		 * http://tools.ietf.org/html/rfc5246#section-6.3
 		 * client_write_MAC_key[SecurityParameters.mac_key_length]
 		 * server_write_MAC_key[SecurityParameters.mac_key_length]
 		 * client_write_key[SecurityParameters.enc_key_length]
 		 * server_write_key[SecurityParameters.enc_key_length]
 		 * client_write_IV[SecurityParameters.fixed_iv_length]
 		 * server_write_IV[SecurityParameters.fixed_iv_length]
 		 */
 		if (cipherSuite == null) {
 			cipherSuite = session.getCipherSuite();
 		}
 
 		int macKeyLength = cipherSuite.getBulkCipher().getMacKeyLength();
 		int encKeyLength = cipherSuite.getBulkCipher().getEncKeyLength();
 		int fixedIvLength = cipherSuite.getBulkCipher().getFixedIvLength();
 
 		clientWriteMACKey = new SecretKeySpec(data, 0, macKeyLength, "Mac");
 		serverWriteMACKey = new SecretKeySpec(data, macKeyLength, macKeyLength, "Mac");
 
 		clientWriteKey = new SecretKeySpec(data, 2 * macKeyLength, encKeyLength, "AES");
 		serverWriteKey = new SecretKeySpec(data, (2 * macKeyLength) + encKeyLength, encKeyLength, "AES");
 
 		clientWriteIV = new IvParameterSpec(data, (2 * macKeyLength) + (2 * encKeyLength), fixedIvLength);
 		serverWriteIV = new IvParameterSpec(data, (2 * macKeyLength) + (2 * encKeyLength) + fixedIvLength, fixedIvLength);
 
 		System.out.println("client_MAC_secret: " + Arrays.toString(clientWriteMACKey.getEncoded()));
 		System.out.println("server_MAC_secret: " + Arrays.toString(serverWriteMACKey.getEncoded()));
 		System.out.println("client_write_secret: " + Arrays.toString(clientWriteKey.getEncoded()));
 		System.out.println("server_write_secret: " + Arrays.toString(serverWriteKey.getEncoded()));
 		System.out.println("client_IV: " + Arrays.toString(clientWriteIV.getIV()));
 		System.out.println("server_IV: " + Arrays.toString(serverWriteIV.getIV()));
 	}
 
 	/**
 	 * Generates the master secret from a given shared premaster secret as
 	 * described in <a href="http://tools.ietf.org/html/rfc5246#section-8.1">RFC
 	 * 5246</a>.
 	 * 
 	 * <pre>
 	 * master_secret = PRF(pre_master_secret, "master secret",
 	 * 	ClientHello.random + ServerHello.random) [0..47]
 	 * </pre>
 	 * 
 	 * @param premasterSecret
 	 *            the shared premaster secret.
 	 * @return the master secret.
 	 */
 	private byte[] generateMasterSecret(byte[] premasterSecret) {
 		byte[] randomSeed = ByteArrayUtils.concatenate(clientRandom.getRandomBytes(), serverRandom.getRandomBytes());
 		System.out.println("PRF(" + Arrays.toString(premasterSecret) + ", " + Arrays.toString("master secret".getBytes()) + ", " + Arrays.toString(randomSeed) + ")");
 		return doPRF(premasterSecret, MASTER_SECRET_LABEL, randomSeed);
 	}
 
 	/**
 	 * See <a href="http://tools.ietf.org/html/rfc4279#section-2">RFC 4279</a>:
 	 * The premaster secret is formed as follows: if the PSK is N octets long,
 	 * concatenate a uint16 with the value N, N zero octets, a second uint16
 	 * with the value N, and the PSK itself.
 	 * 
 	 * @param psk
 	 *            the preshared key as byte array.
 	 * @return the premaster secret.
 	 */
 	protected byte[] generatePremasterSecretFromPSK(byte[] psk) {
 		/*
 		 * What we are building is the following with length fields in between:
 		 * struct { opaque other_secret<0..2^16-1>; opaque psk<0..2^16-1>; };
 		 */
 		int length = psk.length;
 
 		byte[] lengthField = new byte[2];
 		lengthField[0] = (byte) (length >> 8);
 		lengthField[1] = (byte) (length);
 
 		byte[] zero = ByteArrayUtils.padArray(new byte[0], (byte) 0x00, length);
 
 		byte[] premasterSecret = ByteArrayUtils.concatenate(lengthField, ByteArrayUtils.concatenate(zero, ByteArrayUtils.concatenate(lengthField, psk)));
 
 		LOG.info("Preshared Key: " + Arrays.toString(psk));
 		LOG.info("Premaster Secret: " + Arrays.toString(premasterSecret));
 
 		return premasterSecret;
 	}
 
 	/**
 	 * Does the Pseudorandom function as defined in <a
 	 * href="http://tools.ietf.org/html/rfc5246#section-5">RFC 5246</a>.
 	 * 
 	 * @param secret
 	 *            the secret
 	 * @param labelId
 	 *            the label
 	 * @param seed
 	 *            the seed
 	 * @return the byte[]
 	 */
 	public static byte[] doPRF(byte[] secret, int labelId, byte[] seed) {
 		try {
 			MessageDigest md = MessageDigest.getInstance("SHA-256");
 
 			String label;
 			switch (labelId) {
 			case MASTER_SECRET_LABEL:
 				// The master secret is always 48 bytes long, see
 				// http://tools.ietf.org/html/rfc5246#section-8.1
 				label = "master secret";
 				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 48);
 
 			case KEY_EXPANSION_LABEL:
 				// The most key material required is 128 bytes, see
 				// http://tools.ietf.org/html/rfc5246#section-6.3
 				label = "key expansion";
 				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 128);
 
 			case CLIENT_FINISHED_LABEL:
 				// The verify data is always 12 bytes long, see
 				// http://tools.ietf.org/html/rfc5246#section-7.4.9
 				label = "client finished";
 				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 12);
 
 			case SERVER_FINISHED_LABEL:
 				// The verify data is always 12 bytes long, see
 				// http://tools.ietf.org/html/rfc5246#section-7.4.9
 				label = "server finished";
 				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 12);
 
 			case TEST_LABEL:
 				// http://www.ietf.org/mail-archive/web/tls/current/msg03416.html
 				label = "test label";
 				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 100);
 
 			case TEST_LABEL_2:
 				// http://www.ietf.org/mail-archive/web/tls/current/msg03416.html
 				label = "test label";
 				md = MessageDigest.getInstance("SHA-512");
 				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 196);
 
 			case TEST_LABEL_3:
 				// http://www.ietf.org/mail-archive/web/tls/current/msg03416.html
 				label = "test label";
 				md = MessageDigest.getInstance("SHA-384");
 				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 148);
 
 			default:
 				LOG.severe("Unknwon label: " + labelId);
 				return null;
 			}
 		} catch (NoSuchAlgorithmException e) {
 			LOG.severe("Message digest algorithm not available.");
 			e.printStackTrace();
 			return null;
 		}
 	}
 
 	/**
 	 * Performs the secret expansion as described in <a
 	 * href="http://tools.ietf.org/html/rfc5246#section-5">RFC 5246</a>.
 	 * 
 	 * @param md
 	 *            the cryptographic hash function.
 	 * @param secret
 	 *            the secret.
 	 * @param data
 	 *            the data.
 	 * @param length
 	 *            the length of the expansion in <tt>bytes</tt>.
 	 * @return the expanded array with given length.
 	 */
 	protected static byte[] doExpansion(MessageDigest md, byte[] secret, byte[] data, int length) {
 		/*
 		 * P_hash(secret, seed) = HMAC_hash(secret, A(1) + seed) +
 		 * HMAC_hash(secret, A(2) + seed) + HMAC_hash(secret, A(3) + seed) + ...
 		 * where + indicates concatenation. A() is defined as: A(0) = seed, A(i)
 		 * = HMAC_hash(secret, A(i-1))
 		 */
 		double hashLength = 32;
 		if (md.getAlgorithm().equals("SHA-1")) {
 			hashLength = 20;
 		} else if (md.getAlgorithm().equals("SHA-384")) {
 			hashLength = 48;
 		}
 
 		int iterations = (int) Math.ceil(length / hashLength);
 		byte[] expansion = new byte[0];
 
 		byte[] A = data;
 		for (int i = 0; i < iterations; i++) {
 			A = doHMAC(md, secret, A);
 			expansion = ByteArrayUtils.concatenate(expansion, doHMAC(md, secret, ByteArrayUtils.concatenate(A, data)));
 		}
 
 		return ByteArrayUtils.truncate(expansion, length);
 	}
 
 	/**
 	 * Performs the HMAC computation as described in <a
 	 * href="http://tools.ietf.org/html/rfc2104#section-2">RFC 2104</a>.
 	 * 
 	 * @param md
 	 *            the cryptographic hash function.
 	 * @param secret
 	 *            the secret key.
 	 * @param data
 	 *            the data.
 	 * @return the hash after HMAC has been applied.
 	 */
 	public static byte[] doHMAC(MessageDigest md, byte[] secret, byte[] data) {
 		// the block size of the hash function, always 64 bytes (for SHA-512 it
 		// would be 128 bytes, but not needed right now, except for test
 		// purpose)
 
 		int B = 64;
 		if (md.getAlgorithm().equals("SHA-512") || md.getAlgorithm().equals("SHA-384")) {
 			B = 128;
 		}
 
 		// See http://tools.ietf.org/html/rfc2104#section-2
 		// ipad = the byte 0x36 repeated B times
 		byte[] ipad = new byte[B];
 		Arrays.fill(ipad, (byte) 0x36);
 
 		// opad = the byte 0x5C repeated B times
 		byte[] opad = new byte[B];
 		Arrays.fill(opad, (byte) 0x5C);
 
 		/*
 		 * (1) append zeros to the end of K to create a B byte string (e.g., if
 		 * K is of length 20 bytes and B=64, then K will be appended with 44
 		 * zero bytes 0x00)
 		 */
 		byte[] step1 = secret;
 		if (secret.length < B) {
 			// append zeros to the end of K to create a B byte string
 			step1 = ByteArrayUtils.padArray(secret, (byte) 0x00, B);
 		} else if (secret.length > B) {
 			// Applications that use keys longer
 			// than B bytes will first hash the key using H and then use the
 			// resultant L byte string as the actual key to HMAC.
 			md.update(secret);
 			step1 = md.digest();
 			md.reset();
 
 			step1 = ByteArrayUtils.padArray(step1, (byte) 0x00, B);
 		}
 
 		/*
 		 * (2) XOR (bitwise exclusive-OR) the B byte string computed in step (1)
 		 * with ipad
 		 */
 		byte[] step2 = ByteArrayUtils.xorArrays(step1, ipad);
 
 		/*
 		 * (3) append the stream of data 'text' to the B byte string resulting
 		 * from step (2)
 		 */
 		byte[] step3 = ByteArrayUtils.concatenate(step2, data);
 
 		/*
 		 * (4) apply H to the stream generated in step (3)
 		 */
 		md.update(step3);
 		byte[] step4 = md.digest();
 		md.reset();
 
 		/*
 		 * (5) XOR (bitwise exclusive-OR) the B byte string computed in step (1)
 		 * with opad
 		 */
 		byte[] step5 = ByteArrayUtils.xorArrays(step1, opad);
 
 		/*
 		 * (6) append the H result from step (4) to the B byte string resulting
 		 * from step (5)
 		 */
 		byte[] step6 = ByteArrayUtils.concatenate(step5, step4);
 
 		/*
 		 * (7) apply H to the stream generated in step (6) and output the result
 		 */
 		md.update(step6);
 		byte[] step7 = md.digest();
 
 		return step7;
 	}
 
 	protected void setCurrentReadState() {
 		DTLSConnectionState connectionState;
 		if (isClient) {
 			connectionState = new DTLSConnectionState(cipherSuite, compressionMethod, serverWriteKey, serverWriteIV, serverWriteMACKey);
 		} else {
 			connectionState = new DTLSConnectionState(cipherSuite, compressionMethod, clientWriteKey, clientWriteIV, clientWriteMACKey);
 		}
 		session.setReadState(connectionState);
 	}
 
 	protected void setCurrentWriteState() {
 		DTLSConnectionState connectionState;
 		if (isClient) {
 			connectionState = new DTLSConnectionState(cipherSuite, compressionMethod, clientWriteKey, clientWriteIV, clientWriteMACKey);
 		} else {
 			connectionState = new DTLSConnectionState(cipherSuite, compressionMethod, serverWriteKey, serverWriteIV, serverWriteMACKey);
 		}
 		session.setWriteState(connectionState);
 	}
 
 	/**
 	 * Wraps the message into a record layer.
 	 * 
 	 * @param fragment
 	 *            the {@link DTLSMessage} fragment.
 	 * @return the fragment wrapped into a record layer.
 	 */
 	protected Record wrapMessage(DTLSMessage fragment) {
 
 		ContentType type = null;
 		if (fragment instanceof ApplicationMessage) {
 			type = ContentType.APPLICATION_DATA;
 		} else if (fragment instanceof AlertMessage) {
 			type = ContentType.ALERT;
 		} else if (fragment instanceof ChangeCipherSpecMessage) {
 			type = ContentType.CHANGE_CIPHER_SPEC;
 		} else if (fragment instanceof HandshakeMessage) {
 			type = ContentType.HANDSHAKE;
 		}
 
 		return new Record(type, session.getWriteEpoch(), session.getSequenceNumber(), fragment, session);
 	}
 
 	/**
 	 * Determines, using the epoch and sequence number, whether this record is
 	 * the next one which needs to be processed by the handshake protocol.
 	 * 
 	 * @param record
 	 *            the current received message.
 	 * @return <tt>true</tt> if the current message is the next to process,
 	 *         <tt>false</tt> otherwise.
 	 */
 	protected boolean processMessageNext(Record record) {
 
 		int epoch = record.getEpoch();
 		if (epoch < session.getReadEpoch()) {
 			// discard old message
 			LOG.info("Discarded message due to older epoch.");
 			return false;
 		} else if (epoch == session.getReadEpoch()) {
 			DTLSMessage fragment = record.getFragment();
 			if (fragment instanceof AlertMessage) {
 				return true; // Alerts must be processed immediately
 			} else if (fragment instanceof ChangeCipherSpecMessage) {
 				return true; // CCS must be processed immediately
 			} else if (fragment instanceof HandshakeMessage) {
 				int messageSeq = ((HandshakeMessage) fragment).getMessageSeq();
 
 				if (messageSeq == nextReceiveSeq) {
 					nextReceiveSeq++;
 					return true;
 				} else {
 					return false;
 				}
 			} else {
 				return false;
 			}
 		} else {
 			// newer epoch, queue message
 			queuedMessages.add(record);
 			return false;
 		}
 	}
 
 	/**
 	 * Closes the current connection and returns the notify_close Alert message
 	 * wrapped in flight.
 	 * 
 	 * @return the close_notify message to indicate closing of the connection.
 	 */
 	protected DTLSFlight closeConnection() {
 		DTLSFlight flight = new DTLSFlight();
 
 		// TODO what to do here?
 		session.setActive(false);
 		DTLSMessage closeNotify = new AlertMessage(AlertLevel.WARNING, AlertDescription.CLOSE_NOTIFY);
 
 		flight.addMessage(wrapMessage(closeNotify));
 		flight.setRetransmissionNeeded(false);
 
 		return flight;
 	}
 
 	/**
 	 * Loads the private key from a file encoded according to the PKCS #8
 	 * standard.
 	 * 
 	 * @param filename
 	 *            the filename where the private key resides.
 	 * @return the private key.
 	 */
 	protected PrivateKey loadPrivateKey(String filename) {
 		PrivateKey privateKey = null;
 		try {
			File file = new File(getClass().getResource("/" + filename).getFile());
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			byte[] encodedKey = new byte[(int) raf.length()];
 
			raf.readFully(encodedKey);
			raf.close();
 
 			PKCS8EncodedKeySpec kspec = new PKCS8EncodedKeySpec(encodedKey);
 			/*
 			 * See
 			 * http://docs.oracle.com/javase/7/docs/technotes/guides/security
 			 * /StandardNames.html#KeyFactory
 			 */
 			KeyFactory keyF = KeyFactory.getInstance("EC");
 			privateKey = keyF.generatePrivate(kspec);
 
 		} catch (Exception e) {
 			LOG.severe("Could not load private key: " + filename);
 			e.printStackTrace();
 		}
 		return privateKey;
 	}
 
 	/**
 	 * 
 	 * @param filename
 	 * @return
 	 */
 	protected X509Certificate[] loadCertificate(String filename) {
 		X509Certificate[] certificates = new X509Certificate[1];
 
 		try {
 			CertificateFactory cf = CertificateFactory.getInstance("X.509");
 
 			InputStream in = getClass().getResourceAsStream("/" + filename);
 			Certificate certificate = cf.generateCertificate(in);
 			in.close();
 
 			certificates[0] = (X509Certificate) certificate;
 		} catch (Exception e) {
 			LOG.severe("Could not create the certificates.");
 			e.printStackTrace();
 			certificates = null;
 		}
 
 		return certificates;
 	}
 
 	// Getters and Setters ////////////////////////////////////////////
 
 	public CipherSuite getCipherSuite() {
 		return cipherSuite;
 	}
 
 	/**
 	 * Sets the negotiated {@link CipherSuite} and the corresponding
 	 * {@link KeyExchangeAlgorithm}.
 	 * 
 	 * @param cipherSuite
 	 *            the cipher suite.
 	 */
 	public void setCipherSuite(CipherSuite cipherSuite) {
 		this.cipherSuite = cipherSuite;
 		this.keyExchange = cipherSuite.getKeyExchange();
 		this.session.setKeyExchange(keyExchange);
 		this.session.setCipherSuite(cipherSuite);
 	}
 
 	public byte[] getMasterSecret() {
 		return masterSecret;
 	}
 
 	public SecretKey getClientWriteMACKey() {
 		return clientWriteMACKey;
 	}
 
 	public SecretKey getServerWriteMACKey() {
 		return serverWriteMACKey;
 	}
 
 	public IvParameterSpec getClientWriteIV() {
 		return clientWriteIV;
 	}
 
 	public IvParameterSpec getServerWriteIV() {
 		return serverWriteIV;
 	}
 
 	public SecretKey getClientWriteKey() {
 		return clientWriteKey;
 	}
 
 	public SecretKey getServerWriteKey() {
 		return serverWriteKey;
 	}
 
 	public DTLSSession getSession() {
 		return session;
 	}
 
 	public void setSession(DTLSSession session) {
 		this.session = session;
 	}
 
 	/**
 	 * Add the smallest available message sequence to the handshake message.
 	 * 
 	 * @param message
 	 *            the {@link HandshakeMessage}.
 	 */
 	public void setSequenceNumber(HandshakeMessage message) {
 		message.setMessageSeq(sequenceNumber);
 		sequenceNumber++;
 	}
 
 	public Message getMessage() {
 		return message;
 	}
 
 	public void setMessage(Message message) {
 		this.message = message;
 	}
 
 	public int getNextReceiveSeq() {
 		return nextReceiveSeq;
 	}
 
 	public void incrementNextReceiveSeq(int nextReceiveSeq) {
 		this.nextReceiveSeq++;
 	}
 
 	public CompressionMethod getCompressionMethod() {
 		return compressionMethod;
 	}
 
 	public void setCompressionMethod(CompressionMethod compressionMethod) {
 		this.compressionMethod = compressionMethod;
 		this.session.setCompressionMethod(compressionMethod);
 	}
 }
