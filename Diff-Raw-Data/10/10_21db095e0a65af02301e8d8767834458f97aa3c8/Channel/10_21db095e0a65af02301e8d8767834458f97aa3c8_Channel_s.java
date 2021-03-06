 /*
  * Channel.java
  * This file is part of Freemail
  * Copyright (C) 2006,2007,2008 Dave Baker
  * Copyright (C) 2007 Alexander Lehmann
  * Copyright (C) 2009 Martin Nyhus
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License along
  * with this program; if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 
 package freemail.transport;
 
 import java.io.BufferedReader;
 import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.FileReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.io.PrintStream;
 import java.io.PrintWriter;
 import java.io.SequenceInputStream;
 import java.io.UnsupportedEncodingException;
 import java.math.BigInteger;
 import java.security.SecureRandom;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Set;
 import java.util.concurrent.ScheduledExecutorService;
 import java.util.concurrent.TimeUnit;
 
 import org.archive.util.Base32;
 import org.bouncycastle.crypto.AsymmetricBlockCipher;
 import org.bouncycastle.crypto.InvalidCipherTextException;
 import org.bouncycastle.crypto.digests.SHA256Digest;
 import org.bouncycastle.crypto.engines.AESEngine;
 import org.bouncycastle.crypto.engines.RSAEngine;
 import org.bouncycastle.crypto.modes.CBCBlockCipher;
 import org.bouncycastle.crypto.paddings.PKCS7Padding;
 import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
 import org.bouncycastle.crypto.params.KeyParameter;
 import org.bouncycastle.crypto.params.ParametersWithIV;
 import org.bouncycastle.crypto.params.RSAKeyParameters;
 
 import freemail.AccountManager;
 import freemail.AckProcrastinator;
 import freemail.Freemail;
 import freemail.FreemailAccount;
 import freemail.MessageBank;
 import freemail.Postman;
 import freemail.SlotManager;
 import freemail.SlotSaveCallback;
 import freemail.fcp.ConnectionTerminatedException;
 import freemail.fcp.FCPBadFileException;
 import freemail.fcp.FCPFetchException;
 import freemail.fcp.FCPInsertErrorMessage;
 import freemail.fcp.HighLevelFCPClient;
 import freemail.fcp.SSKKeyPair;
 import freemail.utils.DateStringFactory;
 import freemail.utils.Logger;
 import freemail.utils.PropsFile;
 import freemail.wot.Identity;
 import freemail.wot.WoTConnection;
 import freenet.support.SimpleFieldSet;
 
 //FIXME: The message id gives away how many messages has been sent over the channel.
 //       Could it be replaced by a different solution that gives away less information?
 public class Channel extends Postman {
 	private static final String CHANNEL_PROPS_NAME = "props";
 	private static final int POLL_AHEAD = 6;
 	private static final String OUTBOX_DIR_NAME = "outbox";
 
 	//The keys used in the props file
 	private static class PropsKeys {
 		private static final String PRIVATE_KEY = "privateKey";
 		private static final String PUBLIC_KEY = "publicKey";
 		private static final String FETCH_SLOT = "fetchSlot";
 		private static final String SEND_SLOT = "sendSlot";
 		private static final String IS_INITIATOR = "isInitiator";
 		private static final String MESSAGE_ID = "messageId";
 		private static final String SENDER_STATE = "sender-state";
 		private static final String RECIPIENT_STATE = "recipient-state";
 		private static final String RTS_SENT_AT = "rts-sent-at";
 	}
 
 	private final File channelDir;
 	private final PropsFile channelProps;
 	private final Set<Observer> observers = new HashSet<Observer>();
 	private final ScheduledExecutorService executor;
 	private final HighLevelFCPClient fcpClient;
 	private final Freemail freemail;
 	private final FreemailAccount account;
 
 	private Fetcher fetcher;
 	private final Object fetcherLock = new Object();
 
 	private Sender sender;
 	private final Object senderLock = new Object();
 
 	public Channel(File channelDir, ScheduledExecutorService executor, HighLevelFCPClient fcpClient, Freemail freemail, FreemailAccount account) {
 		if(executor == null) throw new NullPointerException();
 		this.executor = executor;
 
 		this.fcpClient = fcpClient;
 		this.account = account;
 
 		if(freemail == null) throw new NullPointerException();
 		this.freemail = freemail;
 
 		assert channelDir.isDirectory();
 		this.channelDir = channelDir;
 
 		File channelPropsFile = new File(channelDir, CHANNEL_PROPS_NAME);
 		if(!channelPropsFile.exists()) {
 			try {
 				if(!channelPropsFile.createNewFile()) {
 					Logger.error(this, "Could not create new props file in " + channelDir);
 				}
 			} catch(IOException e) {
 				Logger.error(this, "Could not create new props file in " + channelDir);
 			}
 		}
 		channelProps = PropsFile.createPropsFile(channelPropsFile);
 
 		//Message id is needed for queuing messages so it must be present
 		if(channelProps.get(PropsKeys.MESSAGE_ID) == null) {
 			channelProps.put(PropsKeys.MESSAGE_ID, "0");
 		}
 	}
 
 	public void processRTS(PropsFile rtsProps) {
 		Logger.debug(this, "Processing RTS");
 
 		//TODO: Set public key using private key
 		//TODO: Handle cases where we get a second RTS
 
 		synchronized(channelProps) {
 			if(channelProps.get(PropsKeys.IS_INITIATOR) == null) {
 				Logger.debug(this, "Setting " + PropsKeys.IS_INITIATOR + " to false");
 				channelProps.put(PropsKeys.IS_INITIATOR, "false");
 			} else {
 				Logger.debug(this, PropsKeys.IS_INITIATOR + " is already set to " + channelProps.get(PropsKeys.IS_INITIATOR));
 			}
 
 			if(channelProps.get(PropsKeys.PRIVATE_KEY) == null) {
 				Logger.debug(this, "Setting " + PropsKeys.PRIVATE_KEY + " to " + rtsProps.get("channel"));
 				channelProps.put(PropsKeys.PRIVATE_KEY, rtsProps.get("channel"));
 			} else {
 				Logger.debug(this, PropsKeys.PRIVATE_KEY + " is already set to " + channelProps.get(PropsKeys.PRIVATE_KEY));
 			}
 
 			if(channelProps.get(PropsKeys.FETCH_SLOT) == null) {
 				Logger.debug(this, "Setting " + PropsKeys.FETCH_SLOT + " to " + rtsProps.get("initiatorSlot"));
 				channelProps.put(PropsKeys.FETCH_SLOT, rtsProps.get("initiatorSlot"));
 			} else {
 				Logger.debug(this, PropsKeys.FETCH_SLOT + " is already set to " + channelProps.get(PropsKeys.FETCH_SLOT));
 			}
 
 			if(channelProps.get(PropsKeys.SEND_SLOT) == null) {
 				Logger.debug(this, "Setting " + PropsKeys.SEND_SLOT + " to " + rtsProps.get("responderSlot"));
 				channelProps.put(PropsKeys.SEND_SLOT, rtsProps.get("responderSlot"));
 			} else {
 				Logger.debug(this, PropsKeys.SEND_SLOT + " is already set to " + channelProps.get(PropsKeys.SEND_SLOT));
 			}
 
 			if(channelProps.get(PropsKeys.MESSAGE_ID) == null) {
 				Logger.debug(this, "Setting " + PropsKeys.MESSAGE_ID + " to 0");
 				channelProps.put(PropsKeys.MESSAGE_ID, "0");
 			} else {
 				Logger.debug(this, PropsKeys.MESSAGE_ID + " is already set to " + channelProps.get(PropsKeys.MESSAGE_ID));
 			}
 
 			if(channelProps.get(PropsKeys.RECIPIENT_STATE) == null) {
 				Logger.debug(this, "Setting " + PropsKeys.RECIPIENT_STATE + " to rts-received");
 				channelProps.put(PropsKeys.RECIPIENT_STATE, "rts-received");
 			} else {
 				Logger.debug(this, PropsKeys.RECIPIENT_STATE + " is already set to " + channelProps.get(PropsKeys.RECIPIENT_STATE));
 			}
 		}
 	}
 
 	public void startTasks() {
 		startFetcher();
 		startSender();
 		startRTSSender();
 	}
 
 	private void startFetcher() {
 		//Start fetcher if possible
 		String fetchSlot;
 		String isInitiator;
 		String publicKey;
 		synchronized(channelProps) {
 			fetchSlot = channelProps.get(PropsKeys.FETCH_SLOT);
 			isInitiator = channelProps.get(PropsKeys.IS_INITIATOR);
 			publicKey = channelProps.get(PropsKeys.PUBLIC_KEY);
 		}
 
 		if((fetchSlot != null) && (isInitiator != null) && (publicKey != null)) {
 			Fetcher f;
 			synchronized(fetcherLock) {
 				fetcher = new Fetcher();
 				f = fetcher;
 			}
 			executor.execute(f);
 		}
 	}
 
 	private void startSender() {
 		//Start sender if possible
 		String sendSlot;
 		String isInitiator;
 		String privateKey;
 		synchronized(channelProps) {
 			sendSlot = channelProps.get(PropsKeys.SEND_SLOT);
 			isInitiator = channelProps.get(PropsKeys.IS_INITIATOR);
 			privateKey = channelProps.get(PropsKeys.PRIVATE_KEY);
 		}
 
 		if((sendSlot != null) && (isInitiator != null) && (privateKey != null)) {
 			Sender s;
 			synchronized(senderLock) {
 				sender = new Sender();
 				s = sender;
 			}
 			executor.execute(s);
 		}
 	}
 
 	private void startRTSSender() {
 		String state;
 		synchronized(channelProps) {
 			state = channelProps.get(PropsKeys.SENDER_STATE);
 		}
 
 		if((state != null) && state.equals("cts-received")) {
 			return;
 		}
 
 		executor.execute(new RTSSender());
 	}
 
 	/**
 	 * Places the data read from {@code message} on the send queue
 	 * @param message the data to be sent
 	 * @return {@code true} if the message was placed on the queue
 	 * @throws NullPointerException if {@code message} is {@code null}
 	 */
 	public boolean sendMessage(InputStream message) {
 		if(message == null) throw new NullPointerException("Parameter message was null");
 
 		long messageId;
 		synchronized(channelProps) {
 			messageId = Long.parseLong(channelProps.get(PropsKeys.MESSAGE_ID));
 			channelProps.put(PropsKeys.MESSAGE_ID, messageId + 1);
 		}
 
 		//Write the message and attributes to the outbox
 		File outbox = new File(channelDir, OUTBOX_DIR_NAME);
 		if(!outbox.exists()) {
 			if(!outbox.mkdir()) {
 				Logger.error(this, "Couldn't create outbox directory: " + outbox);
 				return false;
 			}
 		}
 
 		File messageFile = new File(outbox, "message-" + messageId);
 		if(messageFile.exists()) {
 			//TODO: Pick next message id?
 			Logger.error(this, "Message id already in use");
 			return false;
 		}
 
 		try {
 			if(!messageFile.createNewFile()) {
 				Logger.error(this, "Couldn't create message file: " + messageFile);
 				return false;
 			}
 
 			OutputStream os = new FileOutputStream(messageFile);
 			PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
 
 			//Write the header that will be sent later
 			pw.print("messagetype=message\r\n");
 			pw.print("messageid=" + messageId + "\r\n");
 			pw.print("\r\n");
 			pw.flush();
 
 			//And the message contents
 			byte[] buffer = new byte[1024];
 			while(true) {
 				int count = message.read(buffer, 0, buffer.length);
 				if(count == -1) break;
 
 				os.write(buffer, 0, count);
 			}
 		} catch(IOException e) {
 			if(messageFile.exists()) {
 				if(!messageFile.delete()) {
 					Logger.error(this, "Couldn't delete message file (" + messageFile + ") after IOException");
 				}
 			}
 
 			return false;
 		}
 
 		Sender s;
 		synchronized(senderLock) {
 			s = sender;
 		}
 		if(s != null) {
 			executor.execute(s);
 		}
 
 		return true;
 	}
 
 	/**
 	 * Sends a message to the remote side of the channel containing the given headers and the data
 	 * read from {@code message}. "messagetype" must be set in {@code header}. If message is
 	 * {@code null} no data will be sent after the header.
 	 * @param fcpClient the HighLevelFCPClient used to send the message
 	 * @param header the headers to prepend to the message
 	 * @param message the data to be sent
 	 * @return {@code true} if the message was sent successfully
 	 * @throws ConnectionTerminatedException if the FCP connection is terminated while sending
 	 * @throws IOException if the InputStream throws an IOException
 	 */
 	boolean sendMessage(HighLevelFCPClient fcpClient, SimpleFieldSet header, InputStream message) throws ConnectionTerminatedException, IOException {
 		assert (fcpClient != null);
 		assert (header.get("messagetype") != null);
 
 		String baseKey;
 		synchronized(channelProps) {
 			baseKey = channelProps.get(PropsKeys.PRIVATE_KEY);
 		}
 
 		//SimpleFieldSet seems to only output using \n,
 		//but we need \n\r so we need to do it manually
 		StringBuilder headerString = new StringBuilder();
 		Iterator<String> headerKeyIterator = header.keyIterator();
 		while(headerKeyIterator.hasNext()) {
 			String key = headerKeyIterator.next();
 			String value = header.get(key);
 
 			headerString.append(key + "=" + value + "\r\n");
 		}
 		headerString.append("\r\n");
 
 		ByteArrayInputStream headerBytes = new ByteArrayInputStream(headerString.toString().getBytes("UTF-8"));
 		InputStream data = (message == null) ? headerBytes : new SequenceInputStream(headerBytes, message);
 		while(true) {
 			String slot;
 			synchronized(channelProps) {
 				slot = channelProps.get(PropsKeys.SEND_SLOT);
 			}
 
 			FCPInsertErrorMessage fcpMessage;
 			try {
 				fcpMessage = fcpClient.put(data, baseKey + slot);
 			} catch(FCPBadFileException e) {
 				throw new IOException(e);
 			}
 
 			if(fcpMessage == null) {
 				slot = calculateNextSlot(slot);
 				synchronized(channelProps) {
 					channelProps.put(PropsKeys.SEND_SLOT, slot);
 				}
 				return true;
 			}
 
 			if(fcpMessage.errorcode == FCPInsertErrorMessage.COLLISION) {
 				slot = calculateNextSlot(slot);
 
 				//Write the new slot each round so we won't have
 				//to check all of them again if we fail
 				synchronized(channelProps) {
 					channelProps.put(PropsKeys.SEND_SLOT, slot);
 				}
 
 				Logger.debug(this, "Insert collided, trying slot " + slot);
 				continue;
 			}
 
 			Logger.debug(this, "Insert to slot " + slot + " failed: " + fcpMessage);
 			return false;
 		}
 	}
 
 	private String calculateNextSlot(String slot) {
 		byte[] buf = Base32.decode(slot);
 		SHA256Digest sha256 = new SHA256Digest();
 		sha256.update(buf, 0, buf.length);
 		sha256.doFinal(buf, 0);
 
 		return Base32.encode(buf);
 	}
 
 	private String generateRandomSlot() {
 		SHA256Digest sha256 = new SHA256Digest();
 		byte[] buf = new byte[sha256.getDigestSize()];
 		SecureRandom rnd = new SecureRandom();
 		rnd.nextBytes(buf);
 		return Base32.encode(buf);
 	}
 
 	private class Fetcher implements Runnable {
 		@Override
 		public void run() {
 			Logger.debug(this, "Fetcher running");
 
 			String slots;
 			synchronized(channelProps) {
 				slots = channelProps.get(PropsKeys.FETCH_SLOT);
 			}
 			if(slots == null) {
 				Logger.error(this, "Channel " + channelDir.getName() + " is corrupt - account file has no '" + PropsKeys.FETCH_SLOT + "' entry!");
 				//TODO: Either delete the channel or resend the RTS
 				return;
 			}
 
 			HashSlotManager slotManager = new HashSlotManager(new ChannelSlotSaveImpl(channelProps, PropsKeys.FETCH_SLOT), null, slots);
 			slotManager.setPollAhead(POLL_AHEAD);
 
 			String basekey;
 			synchronized(channelProps) {
 				basekey = channelProps.get(PropsKeys.PRIVATE_KEY);
 			}
 			if(basekey == null) {
 				Logger.error(this, "Contact " + channelDir.getName() + " is corrupt - account file has no '" + PropsKeys.PRIVATE_KEY + "' entry!");
 				//TODO: Either delete the channel or resend the RTS
 				return;
 			}
 
 			String isInitiator;
 			synchronized(channelProps) {
 				isInitiator = channelProps.get(PropsKeys.IS_INITIATOR);
 			}
 
 			if(isInitiator == null) {
 				Logger.error(this, "Contact " + channelDir.getName() + " is corrupt - account file has no '" + PropsKeys.IS_INITIATOR + "' entry!");
 				//TODO: Either delete the channel or resend the RTS
 				return;
 			}
 			if(isInitiator.equalsIgnoreCase("true")) {
 				basekey += "i-";
 			} else if(isInitiator.equalsIgnoreCase("false")) {
 				basekey += "r-";
 			} else {
 				Logger.error(this, "Contact " + channelDir.getName() + " is corrupt - '" + PropsKeys.IS_INITIATOR + "' entry contains an invalid value: " + isInitiator);
 				//TODO: Either delete the channel or resend the RTS
 				return;
 			}
 
 			String slot;
 			while((slot = slotManager.getNextSlot()) != null) {
 				String key = basekey + slot;
 
 				Logger.minor(this, "Attempting to fetch mail on key " + key);
 				File result;
 				try {
 					result = fcpClient.fetch(key);
 				} catch(ConnectionTerminatedException e) {
 					return;
 				} catch(FCPFetchException e) {
 					if(e.isFatal()) {
 						Logger.normal(this, "Fatal fetch failure, marking slot as used");
 						slotManager.slotUsed();
 					}
 
 					Logger.minor(this, "No mail in slot (fetch returned " + e.getMessage() + ")");
 					continue;
 				}
 
 				PropsFile messageProps = PropsFile.createPropsFile(result, true);
 				String messageType = messageProps.get("messagetype");
 
 				if(messageType == null) {
 					Logger.error(this, "Got message without messagetype, discarding");
 					result.delete();
 					continue;
 				}
 
 				if(messageType.equals("message")) {
 					try {
 						if(handleMessage(result, null)) {
 							slotManager.slotUsed();
 						}
 					} catch(ConnectionTerminatedException e) {
 						result.delete();
 						return;
 					}
 				} else if(messageType.equals("cts")) {
 					Logger.minor(this, "Successfully received CTS");
 
 					boolean success;
 					synchronized(channelProps) {
 						success = channelProps.put("status", "cts-received");
 					}
 
 					if(success) {
 						slotManager.slotUsed();
 					}
 				} else if(messageType.equals("ack")) {
 					if(handleAck(result)) {
 						slotManager.slotUsed();
 					}
 				} else {
 					Logger.error(this, "Got message of unknown type: " + messageType);
 					slotManager.slotUsed();
 				}
 
 				if(!result.delete()) {
 					Logger.error(this, "Deletion of " + result + " failed");
 				}
 			}
 
 			//Reschedule
 			executor.schedule(this, 5, TimeUnit.MINUTES);
 		}
 	}
 
 	private class Sender implements Runnable {
 		@Override
 		public void run() {
 			Logger.debug(this, "Sender running");
 
 			//TODO: Try sending messages
 		}
 	}
 
 	private class RTSSender implements Runnable {
 		@Override
 		public void run() {
 			Logger.debug(this, "RTSSender running");
 
 			//Check when the RTS should be sent
 			long sendRtsIn = sendRTSIn();
 			if(sendRtsIn < 0) {
 				return;
 			}
 			if(sendRtsIn > 0) {
 				Logger.debug(this, "Rescheduling RTSSender in " + sendRtsIn + " ms when the RTS is due to be inserted");
 				executor.schedule(this, sendRtsIn, TimeUnit.MILLISECONDS);
 				return;
 			}
 
 			//Get mailsite key from WoT
 			WoTConnection wotConnection = freemail.getWotConnection();
 			if(wotConnection == null) {
 				//WoT isn't loaded, so try again later
 				Logger.debug(this, "WoT not loaded, trying again in 5 minutes");
 				executor.schedule(this, 5, TimeUnit.MINUTES);
 				return;
 			}
 
 			//FIXME: Get the truster id in a better way
 			String senderId = channelDir.getParentFile().getParentFile().getName();
 			Logger.debug(this, "Getting identity from WoT");
 			Identity recipient = wotConnection.getIdentity(channelDir.getName(), senderId);
 			if(recipient == null) {
 				Logger.debug(this, "Didn't get identity from WoT, trying again in 5 minutes");
 				executor.schedule(this, 5, TimeUnit.MINUTES);
 				return;
 			}
 
 			//Strip the WoT part from the key and add the Freemail path
 			String mailsiteKey = recipient.getRequestURI();
 			mailsiteKey = mailsiteKey.substring(0, mailsiteKey.indexOf("/"));
 			mailsiteKey = mailsiteKey + "/mailsite/0/mailpage";
 
 			//Fetch the mailsite
 			File mailsite;
 			try {
 				Logger.debug(this, "Fetching mailsite from " + mailsiteKey);
 				mailsite = fcpClient.fetch(mailsiteKey);
 			} catch(ConnectionTerminatedException e) {
 				Logger.debug(this, "FCP connection has been terminated");
 				return;
 			} catch(FCPFetchException e) {
 				Logger.debug(this, "Mailsite fetch failed (" + e + "), trying again in 5 minutes");
 				executor.schedule(this, 5, TimeUnit.MINUTES);
 				return;
 			}
 
 			//Get RTS KSK
 			PropsFile mailsiteProps = PropsFile.createPropsFile(mailsite, false);
 			String rtsKey = mailsiteProps.get("rtsksk");
 
 			//Get or generate RTS values
 			String privateKey;
 			String publicKey;
 			String initiatorSlot;
 			String responderSlot;
 			synchronized(channelProps) {
 				privateKey = channelProps.get(PropsKeys.PRIVATE_KEY);
 				publicKey = channelProps.get(PropsKeys.PUBLIC_KEY);
 				initiatorSlot = channelProps.get(PropsKeys.SEND_SLOT);
 				responderSlot = channelProps.get(PropsKeys.FETCH_SLOT);
 
 				if((privateKey == null) || (publicKey == null)) {
 					SSKKeyPair keyPair;
 					try {
 						Logger.debug(this, "Making new key pair");
 						keyPair = fcpClient.makeSSK();
 					} catch(ConnectionTerminatedException e) {
 						Logger.debug(this, "FCP connection has been terminated");
 						return;
 					}
 					privateKey = keyPair.privkey;
 					publicKey = keyPair.pubkey;
 				}
 				if(initiatorSlot == null) {
 					initiatorSlot = generateRandomSlot();
 				}
 				if(responderSlot == null) {
 					responderSlot = generateRandomSlot();
 				}
 
 				channelProps.put(PropsKeys.PUBLIC_KEY, publicKey);
 				channelProps.put(PropsKeys.PRIVATE_KEY, privateKey);
 				channelProps.put(PropsKeys.SEND_SLOT, initiatorSlot);
 				channelProps.put(PropsKeys.FETCH_SLOT, responderSlot);
 			}
 
 			//Get the senders mailsite key
 			Logger.debug(this, "Getting sender identity from WoT");
 			Identity senderIdentity = wotConnection.getIdentity(senderId, senderId);
 			if(senderIdentity == null) {
 				Logger.debug(this, "Didn't get identity from WoT, trying again in 5 minutes");
 				executor.schedule(this, 5, TimeUnit.MINUTES);
 				return;
 			}
 			String senderMailsiteKey = recipient.getRequestURI();
 			senderMailsiteKey = senderMailsiteKey.substring(0, senderMailsiteKey.indexOf("/"));
 			senderMailsiteKey = senderMailsiteKey + "/mailsite/0/mailpage";
 
 			//Now build the RTS
 			byte[] rtsMessageBytes = buildRTSMessage(senderMailsiteKey, recipient.getIdentityID(), privateKey, initiatorSlot, responderSlot);
 			if(rtsMessageBytes == null) {
 				return;
 			}
 
 			//Sign the message
 			byte[] signedMessage = signRtsMessage(rtsMessageBytes);
 			if(signedMessage == null) {
 				return;
 			}
 
 			//Encrypt the message using the recipients public key
 			String keyModulus = mailsiteProps.get("asymkey.modulus");
 			if(keyModulus == null) {
 				Logger.error(this, "Mailsite is missing public key modulus");
 				executor.schedule(this, 1, TimeUnit.HOURS);
 				return;
 			}
 
 			String keyExponent = mailsiteProps.get("asymkey.pubexponent");
 			if(keyExponent == null) {
 				Logger.error(this, "Mailsite is missing public key exponent");
 				executor.schedule(this, 1, TimeUnit.HOURS);
 				return;
 			}
 
 			byte[] rtsMessage = encryptMessage(signedMessage, keyModulus, keyExponent);
 
 			//Insert
 			int slot;
 			try {
 				String key = "KSK@" + rtsKey + "-" + DateStringFactory.getKeyString();
 				Logger.debug(this, "Inserting RTS to " + key);
 				slot = fcpClient.slotInsert(rtsMessage, key, 1, "");
 			} catch(ConnectionTerminatedException e) {
 				return;
 			}
 			if(slot < 0) {
 				Logger.debug(this, "Slot insert failed, trying again in 5 minutes");
 				executor.schedule(this, 5, TimeUnit.MINUTES);
 				return;
 			}
 
 			//Update channel props file
 			synchronized(channelProps) {
				//TODO: What if we've gotten the CTS while inserting the RTS?
				channelProps.put(PropsKeys.SENDER_STATE, "rts-sent");
 				channelProps.put(PropsKeys.RTS_SENT_AT, Long.toString(System.currentTimeMillis()));
 			}
 
 			long delay = sendRTSIn();
 			Logger.debug(this, "Rescheduling RTSSender to run in " + delay + " ms when the reinsert is due");
 			executor.schedule(this, delay, TimeUnit.MILLISECONDS);
 		}
 
 		/**
 		 * Returns the number of milliseconds left to when the RTS should be sent, or -1 if it
 		 * should not be sent.
 		 * @return the number of milliseconds left to when the RTS should be sent
 		 */
 		private long sendRTSIn() {
 			//Check if the CTS has been received
 			String channelState;
 			synchronized(channelProps) {
 				channelState = channelProps.get(PropsKeys.SENDER_STATE);
 			}
 			if((channelState != null) && channelState.equals("cts-received")) {
 				Logger.debug(this, "CTS has been received");
 				return -1;
 			}
 
 			//Check when the RTS should be (re)sent
 			String rtsSentAt;
 			synchronized(channelProps) {
 				rtsSentAt = channelProps.get(PropsKeys.RTS_SENT_AT);
 			}
 
 			if(rtsSentAt != null) {
 				long sendTime;
 				try {
 					sendTime = Long.parseLong(rtsSentAt);
 				} catch(NumberFormatException e) {
 					Logger.debug(this, "Illegal value in " + PropsKeys.RTS_SENT_AT + " field, assuming 0");
 					sendTime = 0;
 				}
 
 				long timeToResend = (24 * 60 * 60 * 1000) - (System.currentTimeMillis() - sendTime);
 				if(timeToResend > 0) {
 					return timeToResend;
 				}
 			}
 
 			//Send the RTS immediately
 			return 0;
 		}
 
 		private byte[] buildRTSMessage(String senderMailsiteKey, String recipientIdentityID, String channelPrivateKey, String initiatorSlot, String responderSlot) {
 			StringBuffer rtsMessage = new StringBuffer();
 			rtsMessage.append("mailsite=" + senderMailsiteKey + "\r\n");
 			rtsMessage.append("to=" + recipientIdentityID + "\r\n");
 			rtsMessage.append("channel=" + channelPrivateKey + "\r\n");
 			rtsMessage.append("initiatorSlot=" + initiatorSlot + "\r\n");
 			rtsMessage.append("responderSlot=" + responderSlot + "\r\n");
 			rtsMessage.append("\r\n");
 
 			byte[] rtsMessageBytes;
 			try {
 				rtsMessageBytes = rtsMessage.toString().getBytes("UTF-8");
 			} catch(UnsupportedEncodingException e) {
 				Logger.error(this, "JVM doesn't support UTF-8 charset");
 				e.printStackTrace();
 				return null;
 			}
 
 			return rtsMessageBytes;
 		}
 
 		private byte[] signRtsMessage(byte[] rtsMessageBytes) {
 			SHA256Digest sha256 = new SHA256Digest();
 			sha256.update(rtsMessageBytes, 0, rtsMessageBytes.length);
 			byte[] hash = new byte[sha256.getDigestSize()];
 			sha256.doFinal(hash, 0);
 
 			RSAKeyParameters ourPrivateKey = AccountManager.getPrivateKey(account.getProps());
 
 			AsymmetricBlockCipher signatureCipher = new RSAEngine();
 			signatureCipher.init(true, ourPrivateKey);
 			byte[] signature = null;
 			try {
 				signature = signatureCipher.processBlock(hash, 0, hash.length);
 			} catch(InvalidCipherTextException e) {
 				Logger.error(this, "Failed to RSA encrypt hash: " + e.getMessage());
 				e.printStackTrace();
 				return null;
 			}
 
 			byte[] signedMessage = new byte[rtsMessageBytes.length + signature.length];
 			System.arraycopy(rtsMessageBytes, 0, signedMessage, 0, rtsMessageBytes.length);
 			System.arraycopy(signature, 0, signedMessage, rtsMessageBytes.length, signature.length);
 
 			return signedMessage;
 		}
 
 		private byte[] encryptMessage(byte[] signedMessage, String keyModulus, String keyExponent) {
 			//Make a new symmetric key for the message
 			byte[] aesKeyAndIV = new byte[32 + 16];
 			SecureRandom rnd = new SecureRandom();
 			rnd.nextBytes(aesKeyAndIV);
 
 			//Encrypt the message with the new symmetric key
 			PaddedBufferedBlockCipher aesCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), new PKCS7Padding());
 			KeyParameter aesKeyParameters = new KeyParameter(aesKeyAndIV, 16, 32);
 			ParametersWithIV aesParameters = new ParametersWithIV(aesKeyParameters, aesKeyAndIV, 0, 16);
 			aesCipher.init(true, aesParameters);
 
 			byte[] encryptedMessage = new byte[aesCipher.getOutputSize(signedMessage.length)];
 			int offset = aesCipher.processBytes(signedMessage, 0, signedMessage.length, encryptedMessage, 0);
 
 			try {
 				aesCipher.doFinal(encryptedMessage, offset);
 			} catch(InvalidCipherTextException e) {
 				Logger.error(this, "Failed to perform symmertic encryption on RTS data: " + e.getMessage());
 				e.printStackTrace();
 				return null;
 			}
 
 			RSAKeyParameters recipientPublicKey = new RSAKeyParameters(false, new BigInteger(keyModulus, 32), new BigInteger(keyExponent, 32));
 			AsymmetricBlockCipher keyCipher = new RSAEngine();
 			keyCipher.init(true, recipientPublicKey);
 			byte[] encryptedAesParameters = null;
 			try {
 				encryptedAesParameters = keyCipher.processBlock(aesKeyAndIV, 0, aesKeyAndIV.length);
 			} catch(InvalidCipherTextException e) {
 				Logger.error(this, "Failed to perform asymmertic encryption on RTS symmetric key: " + e.getMessage());
 				e.printStackTrace();
 				return null;
 			}
 
 			//Assemble the final message
 			byte[] rtsMessage = new byte[encryptedAesParameters.length + encryptedMessage.length];
 			System.arraycopy(encryptedAesParameters, 0, rtsMessage, 0, encryptedAesParameters.length);
 			System.arraycopy(encryptedMessage, 0, rtsMessage, encryptedAesParameters.length, encryptedMessage.length);
 
 			return rtsMessage;
 		}
 	}
 
 	/**
 	 * Adds an observer to this Channel.
 	 * @param observer the observer that should be added
 	 * @throws NullPointerException if {@code observer} is {@code null}
 	 */
 	public void addObserver(Observer observer) {
 		if(observer == null) throw new NullPointerException();
 
 		synchronized(observers) {
 			observers.add(observer);
 		}
 	}
 
 	/**
 	 * Removes an observer from this Channel.
 	 * @param observer the observer that should be removed
 	 */
 	public void removeObserver(Observer observer) {
 		//This is a bug in the caller, but leave it as an assert since it won't corrupt any state
 		assert (observer != null);
 
 		synchronized(observers) {
 			observers.remove(observer);
 		}
 	}
 
 	public interface Observer {
 		public void fetched(InputStream data);
 	}
 
 	private boolean handleMessage(File msg, MessageBank mb) throws ConnectionTerminatedException {
 		// parse the Freemail header(s) out.
 		PropsFile msgprops = PropsFile.createPropsFile(msg, true);
 		String s_id = msgprops.get("id");
 		if (s_id == null) {
 			Logger.error(this,"Got a message with an invalid header. Discarding.");
 			msgprops.closeReader();
 			return true;
 		}
 
 		int id;
 		try {
 			id = Integer.parseInt(s_id);
 		} catch (NumberFormatException nfe) {
 			Logger.error(this,"Got a message with an invalid (non-integer) id. Discarding.");
 			msgprops.closeReader();
 			return true;
 		}
 
 		MessageLog msglog = new MessageLog(this.channelDir);
 		boolean isDupe;
 		try {
 			isDupe = msglog.isPresent(id);
 		} catch (IOException ioe) {
 			Logger.error(this,"Couldn't read logfile, so don't know whether received message is a duplicate or not. Leaving in the queue to try later.");
 			msgprops.closeReader();
 			return false;
 		}
 		if (isDupe) {
 			Logger.normal(this,"Got a message, but we've already logged that message ID as received. Discarding.");
 			msgprops.closeReader();
 			return true;
 		}
 
 		BufferedReader br = msgprops.getReader();
 		if (br == null) {
 			Logger.error(this,"Got an invalid message. Discarding.");
 			msgprops.closeReader();
 			return true;
 		}
 
 		try {
 			this.storeMessage(br, mb);
 		} catch (IOException ioe) {
 			return false;
 		}
 		Logger.normal(this,"You've got mail!");
 		try {
 			msglog.add(id);
 		} catch (IOException ioe) {
 			// how should we handle this? Remove the message from the inbox again?
 			Logger.error(this,"warning: failed to write log file!");
 		}
 		String ack_key;
 		synchronized(this.channelProps) {
 			ack_key = this.channelProps.get("ackssk");
 		}
 		if (ack_key == null) {
 			Logger.error(this,"Warning! Can't send message acknowledgement - don't have an 'ackssk' entry! This message will eventually bounce, even though you've received it.");
 			return true;
 		}
 		ack_key += "ack-"+id;
 		AckProcrastinator.put(ack_key);
 
 		return true;
 	}
 
 	private boolean handleAck(File result) {
 		PropsFile ackProps = PropsFile.createPropsFile(result);
 		String id = ackProps.get("id");
 
 		File outbox = new File(channelDir, OUTBOX_DIR_NAME);
 		if(!outbox.exists()) {
 			Logger.minor(this, "Got ack for message " + id + ", but the outbox doesn't exist");
 			return true;
 		}
 
 		File message = new File(outbox, "message-" + id);
 		if(!message.exists()) {
 			Logger.minor(this, "Got ack for message " + id + ", but the message doesn't exist");
 			return true;
 		}
 
 		if(!message.delete()) {
 			Logger.error(this, "Couldn't delete " + message);
 			return false;
 		}
 
 		return true;
 	}
 
 	private class HashSlotManager extends SlotManager {
 		HashSlotManager(SlotSaveCallback cb, Object userdata, String slotlist) {
 			super(cb, userdata, slotlist);
 		}
 
 		@Override
 		protected String incSlot(String slot) {
 			return calculateNextSlot(slot);
 		}
 	}
 
 	private static class MessageLog {
 		private static final String LOGFILE = "log";
 		private final File logfile;
 
 		public MessageLog(File ibctdir) {
 			this.logfile = new File(ibctdir, LOGFILE);
 		}
 
 		public boolean isPresent(int targetid) throws IOException {
 			BufferedReader br;
 			try {
 				br = new BufferedReader(new FileReader(this.logfile));
 			} catch (FileNotFoundException fnfe) {
 				return false;
 			}
 
 			String line;
 			while ( (line = br.readLine()) != null) {
 				int curid = Integer.parseInt(line);
 				if (curid == targetid) {
 					br.close();
 					return true;
 				}
 			}
 
 			br.close();
 			return false;
 		}
 
 		public void add(int id) throws IOException {
 			FileOutputStream fos = new FileOutputStream(this.logfile, true);
 
 			PrintStream ps = new PrintStream(fos);
 			ps.println(id);
 			ps.close();
 		}
 	}
 
 	private static class ChannelSlotSaveImpl implements SlotSaveCallback {
 		private final PropsFile propsFile;
 		private final String keyName;
 
 		private ChannelSlotSaveImpl(PropsFile propsFile, String keyName) {
 			this.propsFile = propsFile;
 			this.keyName = keyName;
 		}
 
 		@Override
 		public void saveSlots(String slots, Object userdata) {
 			synchronized(propsFile) {
 				propsFile.put(keyName, slots);
 			}
 		}
 	}
 }
