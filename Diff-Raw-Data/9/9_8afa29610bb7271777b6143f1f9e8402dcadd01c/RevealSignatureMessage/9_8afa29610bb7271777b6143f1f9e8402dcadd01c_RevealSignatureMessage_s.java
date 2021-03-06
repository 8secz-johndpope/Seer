 package net.java.otr4j.message.encoded;
 
 import java.math.BigInteger;
 import java.nio.ByteBuffer;
 import java.security.InvalidAlgorithmParameterException;
 import java.security.InvalidKeyException;
 import java.security.NoSuchAlgorithmException;
 import java.security.PrivateKey;
 import java.security.PublicKey;
 import java.security.SignatureException;
 
 import javax.crypto.BadPaddingException;
 import javax.crypto.IllegalBlockSizeException;
 import javax.crypto.NoSuchPaddingException;
 import javax.crypto.interfaces.DHPublicKey;
 
 import net.java.otr4j.message.MessageType;
 import net.java.otr4j.protocol.crypto.CryptoUtils;
 
 public final class RevealSignatureMessage extends SignatureMessageBase {
 
 	public RevealSignatureMessage(int protocolVersion, BigInteger s,
 			DHPublicKey gxKey, DHPublicKey gyKey, int keyidB,
 			PrivateKey privKey, PublicKey pubKey, byte[] r)
 			throws InvalidKeyException, NoSuchAlgorithmException,
 			SignatureException, NoSuchPaddingException,
 			InvalidAlgorithmParameterException, IllegalBlockSizeException,
 			BadPaddingException {
 		this();
 
 		byte[] c = EncodedMessageUtils.getC(s);
 		byte[] m1 = EncodedMessageUtils.getM1(s);
 		byte[] m2 = EncodedMessageUtils.getM2(s);
 
 		byte[] MB = EncodedMessageUtils.computeMB(gxKey, gyKey, keyidB, pubKey,
 				m1);
 		byte[] XB = EncodedMessageUtils.computeXB(privKey, pubKey, keyidB, MB);
 		byte[] XBEncrypted = CryptoUtils.aesEncrypt(c, XB);
 
 		byte[] mac = CryptoUtils.sha256Hmac160(XBEncrypted, m2);
 
		RevealSignatureMessage msg = new RevealSignatureMessage();
		msg.protocolVersion = protocolVersion;
		msg.signatureMac = mac;
		msg.encryptedSignature = XBEncrypted;
		msg.revealedKey = r;
 	}
 	
 	private RevealSignatureMessage() {
 		super(MessageType.REVEALSIG);
 	}
 
 	public byte[] revealedKey;
 
 	public String toString() {
 		int len = 0;
 		// Protocol version (SHORT)
 		byte[] protocolVersion = EncodedMessageUtils
 				.serializeShort(this.protocolVersion);
 		len += protocolVersion.length;
 
 		// Message type (BYTE)
 		byte[] messageType = EncodedMessageUtils.serializeByte(this
 				.getMessageType());
 		len += messageType.length;
 
 		// Revealed key (DATA)
 		byte[] serializedRevealedKey = EncodedMessageUtils
 				.serializeData(this.revealedKey);
 		len += serializedRevealedKey.length;
 
 		// Encrypted Signature (DATA)
 		byte[] serializedSig = EncodedMessageUtils
 				.serializeData(this.encryptedSignature);
 		len += serializedSig.length;
 
 		// MAC'd signature (MAC)
 		byte[] mac = this.signatureMac;
 		len += DataLength.MAC;
 
 		ByteBuffer buff = ByteBuffer.allocate(len);
 		buff.put(protocolVersion);
 		buff.put(messageType);
 		buff.put(serializedRevealedKey);
 		buff.put(serializedSig);
 		buff.put(mac);
 
 		String encodedMessage = EncodedMessageUtils.encodeMessage(buff.array());
 		return encodedMessage;
 	}
 
 	public RevealSignatureMessage(String msgText) {
 		this();
 
 		byte[] decodedMessage = EncodedMessageUtils.decodeMessage(msgText);
 		ByteBuffer buff = ByteBuffer.wrap(decodedMessage);
 
 		// Protocol version (SHORT)
 		int protocolVersion = EncodedMessageUtils.deserializeShort(buff);
 
 		// Message type (BYTE)
 		int msgType = EncodedMessageUtils.deserializeByte(buff);
 		if (msgType != MessageType.REVEALSIG)
 			return;
 
 		// Revealed key (DATA)
 		byte[] revealedKey = EncodedMessageUtils.deserializeData(buff);
 
 		// Encrypted Signature (DATA)
 		byte[] sig = EncodedMessageUtils.deserializeData(buff);
 
 		// MAC'd signature (MAC)
 		byte[] mac = EncodedMessageUtils.deserializeMac(buff);
 
 		this.protocolVersion = protocolVersion;
 		this.revealedKey = revealedKey;
 		this.encryptedSignature = sig;
 		this.signatureMac = mac;
 	}
 }
