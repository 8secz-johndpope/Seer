 package com.parc.ccn.security.access;
 
 import java.security.InvalidKeyException;
 import java.security.Key;
 import java.sql.Timestamp;
 
 import javax.crypto.spec.SecretKeySpec;
 import javax.xml.stream.XMLStreamException;
 
import com.parc.ccn.Library;
 import com.parc.ccn.data.ContentName;
 import com.parc.ccn.library.profiles.AccessControlProfile;
 import com.parc.ccn.library.profiles.VersionMissingException;
 import com.parc.ccn.library.profiles.VersioningProfile;
 import com.parc.ccn.security.crypto.CCNDigestHelper;
 import com.parc.ccn.security.crypto.KeyDerivationFunction;
 
 public class NodeKey {
 	
 	/**
 	 * Default data key length in bytes. No real reason this can't be bumped up to 32. It
 	 * acts as the seed for a KDF, not an encryption key.
 	 */
 	public static final int DEFAULT_NODE_KEY_LENGTH = 16; 
 	/**
 	 * The keys we're wrapping are really seeds for a KDF, not keys in their own right.
 	 * Eventually we'll use CMAC, so call them AES...
 	 */
 	public static final String DEFAULT_NODE_KEY_ALGORITHM = "AES";
 	
 	/**
 	 * The node this key is associated with, with _access_ information stripped.
 	 */
 	private ContentName _nodeName;
 	/**
 	 * The full name of the stored node key that is either this key itself,
 	 * or the ancestor node key this is derived from, including its version information.
 	 */
 	private ContentName _storedNodeKeyName;
 	private byte [] _storedNodeKeyID;
 	/**
 	 * The unwrapped node key
 	 */
 	private Key _nodeKey;
 	
 	public NodeKey(ContentName nodeKeyName, byte [] unwrappedNodeKey) {
 		this(nodeKeyName, new SecretKeySpec(unwrappedNodeKey, DEFAULT_NODE_KEY_ALGORITHM));
 	}
 	
 	public NodeKey(ContentName nodeKeyName, Key unwrappedNodeKey) {
 		if ((null == nodeKeyName) || (null == unwrappedNodeKey)) {
 			throw new IllegalArgumentException("NodeKey: key name and key cannot be null!");
 		}
		if (!VersioningProfile.isVersioned(nodeKeyName)) {
			throw new IllegalArgumentException("Expect stored node key name to be versioned: " + nodeKeyName);
		}
 		_storedNodeKeyName = nodeKeyName;
 		_storedNodeKeyID = generateKeyID(unwrappedNodeKey.getEncoded());
 		_nodeKey = unwrappedNodeKey;
 		_nodeName = AccessControlProfile.accessRoot(nodeKeyName);
 		if ((null == _nodeName) || (!AccessControlProfile.isNodeKeyName(nodeKeyName))) {
 			throw new IllegalArgumentException("NodeKey: key name " + nodeKeyName + " is not a valid node key name.");			
 		}		
 	}
 	
 	protected NodeKey(ContentName nodeName, byte [] derivedNodeKey, 
 					  ContentName ancestorNodeKeyName, byte [] ancestorNodeKeyID) {
		if (!VersioningProfile.isVersioned(ancestorNodeKeyName)) {
			throw new IllegalArgumentException("Expect stored node key name to be versioned: " + ancestorNodeKeyName);
		}
 		_storedNodeKeyName = ancestorNodeKeyName;
 		_storedNodeKeyID = ancestorNodeKeyID;
 		_nodeName = nodeName;
 		_nodeKey = new SecretKeySpec(derivedNodeKey, DEFAULT_NODE_KEY_ALGORITHM);
 	}
 	
 	public NodeKey computeDescendantNodeKey(ContentName descendantNodeName, String keyLabel) throws InvalidKeyException, XMLStreamException {
 		if (!nodeName().isPrefixOf(descendantNodeName)) {
 			throw new IllegalArgumentException("Node " + descendantNodeName + " is not a child of this node " + nodeName());
 		}
 		byte [] derivedKey = KeyDerivationFunction.DeriveKeyForNode(nodeName(), nodeKey().getEncoded(), keyLabel, descendantNodeName);
 		return new NodeKey(descendantNodeName, derivedKey, storedNodeKeyName(), storedNodeKeyID());
 	}
 	
 	public ContentName nodeName() { return _nodeName; }
 	public ContentName storedNodeKeyName() { return _storedNodeKeyName; }
 	public byte [] storedNodeKeyID() { return _storedNodeKeyID; }
 	public Key nodeKey() { return _nodeKey; }
 	
 	public boolean isDerivedNodeKey() {
 		return (!nodeName().isPrefixOf(storedNodeKeyName()));
 	}
 	
	public Timestamp nodeKeyVersion() { 
		try {
			return VersioningProfile.getVersionAsTimestamp(storedNodeKeyName());
		} catch (VersionMissingException e) {
			Library.logger().warning("Unexpected: name that was confirmed to have a version on construction throws a VersionMissingException: " + storedNodeKeyName());
			throw new IllegalStateException("Unexpected: name that was confirmed to have a version on construction throws a VersionMissingException: " + storedNodeKeyName());
		}
 	}
 	
 	public byte [] generateKeyID() { 
 		return generateKeyID(nodeKey().getEncoded());
 	}
 	
 	public static byte [] generateKeyID(byte [] key) {
 		return CCNDigestHelper.digest(key);
 	}
 }
