 /**
  * Part of the CCNx Java Library.
  *
  * Copyright (C) 2008, 2009 Palo Alto Research Center, Inc.
  *
  * This library is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License version 2.1
  * as published by the Free Software Foundation. 
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details. You should have received
  * a copy of the GNU Lesser General Public License along with this library;
  * if not, write to the Free Software Foundation, Inc., 51 Franklin Street,
  * Fifth Floor, Boston, MA 02110-1301 USA.
  */
 
 package org.ccnx.ccn.profiles.security.access.group;
 
 
 import java.util.logging.Level;
 
 import org.bouncycastle.util.Arrays;
 import org.ccnx.ccn.impl.security.crypto.CCNDigestHelper;
 import org.ccnx.ccn.impl.support.DataUtils;
 import org.ccnx.ccn.impl.support.Log;
 import org.ccnx.ccn.impl.support.Tuple;
 import org.ccnx.ccn.io.content.ContentEncodingException;
 import org.ccnx.ccn.profiles.CCNProfile;
 import org.ccnx.ccn.profiles.VersionMissingException;
 import org.ccnx.ccn.profiles.VersioningProfile;
 import org.ccnx.ccn.profiles.security.access.AccessControlProfile;
 import org.ccnx.ccn.protocol.CCNTime;
 import org.ccnx.ccn.protocol.ContentName;
 
 /**
  * This class specifies how a number of access control elements are named:
  * - users, and their keys
  * - groups, and their keys
  * - access control lists (ACLs)
  * - node keys, and their encryption under ACL member keys
  * - if used, markers indicating where to find ACLs/node keys
  */
 public class GroupAccessControlProfile extends AccessControlProfile implements CCNProfile {
 	
 	// These may eventually want to move somewhere more general
 	public static final String GROUP_PREFIX = "Groups";
 	public static final byte [] GROUP_PREFIX_BYTES = ContentName.componentParseNative(GROUP_PREFIX);
 	public static final String USER_PREFIX = "Users";
 	public static final byte [] USER_PREFIX_BYTES = ContentName.componentParseNative(USER_PREFIX);
 	
 	public static final String GROUP_MEMBERSHIP_LIST_NAME = "MembershipList";
 	public static final String GROUP_POINTER_TO_PARENT_GROUP_NAME = "PointerToParentGroup";
 	public static final String ACL_NAME = "ACL";
 	public static final byte [] ACL_NAME_BYTES = ContentName.componentParseNative(ACL_NAME);
 	public static final String NODE_KEY_NAME = "NK";
 	public static final byte [] NODE_KEY_NAME_BYTES = ContentName.componentParseNative(NODE_KEY_NAME);	
 	// These two must be the same length
 	public static final byte [] USER_PRINCIPAL_PREFIX = ContentName.componentParseNative("p");
 	public static final byte [] GROUP_PRINCIPAL_PREFIX = ContentName.componentParseNative("g");
 	public static final ContentName ACL_POSTFIX = new ContentName(new byte[][]{ACCESS_CONTROL_MARKER_BYTES, ACL_NAME_BYTES});
 
 	/**
 	 * This class records information about a CCN principal.
 	 * This information includes: 
 	 * - principal type (group, etc),
 	 * - friendly name (the name the principal is known by)
 	 * - version
 	 * 
 	 * We define a mapping between name components and principals:
 	 * <TYPE_PREFIX>:<NAMESPACE_HASH>:<FRIENDLY_NAME>:<VERSION>
 	 *
 	 */
 	public static class PrincipalInfo {
 		
 		// Number of parts expected in a PI component representation
 		private static final int PI_COMPONENT_COUNT = 4;
 		
 		// However long our distinguishing hashes should be
 		public static final int DISTINGUISHING_HASH_LENGTH = 8;
 	
 		private byte [] _typeMarker;
 		private byte[] _distinguishingHash;
 		private String _friendlyName;
 		private CCNTime _versionTimestamp;
 		
 		public PrincipalInfo(byte [] type, byte[] distinguishingHash, String friendlyName, CCNTime versionTimestamp) {
 			_typeMarker = type;
 			_distinguishingHash = distinguishingHash;
 			_friendlyName = friendlyName;
 			_versionTimestamp = versionTimestamp;
 		}
 		
 		/**
 		 * Parse the principal info for a specified public key name
 		 * @param isGroup whether the principal is a group
 		 * @param publicKeyName the public key name
 		 * @return the corresponding principal info
 		 * @throws VersionMissingException
 		 * @throws ContentEncodingException 
 		 */
 		public PrincipalInfo(GroupAccessControlManager accessControlManager, ContentName publicKeyName) throws VersionMissingException, ContentEncodingException {
 			boolean isGroup = accessControlManager.isGroupName(publicKeyName);
 			_typeMarker = (isGroup ? GROUP_PRINCIPAL_PREFIX : USER_PRINCIPAL_PREFIX);
 			_versionTimestamp = VersioningProfile.getLastVersionAsTimestamp(publicKeyName);
 			
 			// Now need to parse the principal name into a distinguishing hash and friendly name
 			// How to do this depends on which group it is; there might be a postfix for the
 			// name to deal with
 			Tuple<ContentName, String> distinguishingPrefixAndFriendlyName = accessControlManager.parsePrefixAndFriendlyNameFromPublicKeyName(publicKeyName);
 			_distinguishingHash = contentPrefixToDistinguishingHash(distinguishingPrefixAndFriendlyName.first());
 			_friendlyName = distinguishingPrefixAndFriendlyName.second();
 		}
 		
 		public PrincipalInfo(byte [] principalInfoNameComponent) {
 			
 			if (!PrincipalInfo.isPrincipalNameComponent(principalInfoNameComponent) || (principalInfoNameComponent.length <= USER_PRINCIPAL_PREFIX.length))
 				throw new IllegalArgumentException("Not a valid principal name component!");
 			
 			// First time we see COMPONENT_SEPARATOR is the separation point.
 			// Could jump back based on fixed width of timestamp.
 			byte [][] pieces = 
 				DataUtils.binarySplit(principalInfoNameComponent, CCNProfile.COMPONENT_SEPARATOR[0]);
 			if (pieces.length < PI_COMPONENT_COUNT) {
 				if (Log.isLoggable(Log.FAC_ACCESSCONTROL, Level.WARNING)) {
 					Log.warning(Log.FAC_ACCESSCONTROL, "Unexpected principal name format - insufficient number of components: " + 
 							ContentName.componentPrintURI(principalInfoNameComponent, USER_PRINCIPAL_PREFIX.length, principalInfoNameComponent.length-USER_PRINCIPAL_PREFIX.length));
 				}
 				throw new IllegalArgumentException("Not a valid principal name component -- insufficient number of components!");				
 			}
 			
 			_typeMarker = pieces[0];
 			_distinguishingHash = pieces[1];
 			_friendlyName = ContentName.componentPrintNative(pieces[2]);
			try {
				_versionTimestamp = new CCNTime(pieces[3]);
			} catch (IllegalArgumentException e) {
				// we've been having some trouble here...
				Log.severe(Log.FAC_ACCESSCONTROL, "PrincipalInfo: error in parsing component {0}", 
						ContentName.componentPrintURI(principalInfoNameComponent));
				Log.severe(Log.FAC_ACCESSCONTROL, "PrincpialInfo: typeMarker {0}, distinguishing hash {1}, friendly name {2}, part to parse into timestamp {3}",
						ContentName.componentPrintURI(_typeMarker), ContentName.componentPrintURI(_distinguishingHash),
						_friendlyName, ContentName.componentPrintURI(pieces[3]));
				throw e;
			}
 		}
 		
 		/**
 		 * Principal names for links to wrapped key blocks take the form:
 		 * {GROUP_PRINCIPAL_PREFIX | PRINCIPAL_PREFIX} COMPONENT_SEPARATOR distinguisingHash COMPONENT_SEPARATOR friendlName COMPONENT_SEPARATOR timestamp as 12-bit binary
 		 * This allows a single enumeration of a wrapped key directory to determine
 		 * not only which principals the keys are wrapped for, but also what versions of their
 		 * private keys the keys are wrapped under (also determinable from the contents of the
 		 * wrapped key blocks, but to do that you have to pull the wrapped key block).
 		 * These serve as the name of a link to the actual wrapped key block.
 		 */
 		public byte[] toNameComponent() {
 			byte [] prefix = (isGroup() ? GROUP_PRINCIPAL_PREFIX : USER_PRINCIPAL_PREFIX);
 			byte [] bytePrincipal = ContentName.componentParseNative(friendlyName());
 			byte [] byteTime = versionTimestamp().toBinaryTime();
 			byte [] component = new byte[prefix.length + distinguishingHash().length + bytePrincipal.length + 
 			                             byteTime.length + 3*COMPONENT_SEPARATOR.length];
 			// java 1.6 has much better functions for array copying
 			int offset = 0;
 			System.arraycopy(prefix, 0, component, offset, prefix.length);
 			offset += prefix.length;
 			System.arraycopy(COMPONENT_SEPARATOR, 0, component, offset, COMPONENT_SEPARATOR.length);
 			offset += COMPONENT_SEPARATOR.length;
 
 			System.arraycopy(distinguishingHash(), 0, component, offset, distinguishingHash().length);
 			offset += distinguishingHash().length;
 			System.arraycopy(COMPONENT_SEPARATOR, 0, component, offset, COMPONENT_SEPARATOR.length);
 			offset += COMPONENT_SEPARATOR.length;
 
 			System.arraycopy(bytePrincipal, 0, component, offset, bytePrincipal.length);
 			offset += bytePrincipal.length;
 			System.arraycopy(COMPONENT_SEPARATOR, 0, component, offset, COMPONENT_SEPARATOR.length);
 			offset += COMPONENT_SEPARATOR.length;
 			
 			System.arraycopy(byteTime, 0, component, offset, byteTime.length);
 			
 			return component;
 		}
 		
 		/**
 		 * Parses the principal name from a group public key.
 		 * For groups, the last component of the public key is GROUP_PUBLIC_KEY_NAME = "Key".
 		 * The principal name is the one-before-last component.
 		 * @param publicKeyName the name of the group public key
 		 * @return the corresponding principal name
 		 */
 		public static String parsePrincipalNameFromGroupPublicKeyName(ContentName publicKeyName) {
 			ContentName cn = VersioningProfile.cutTerminalVersion(publicKeyName).first();
 			return ContentName.componentPrintNative(cn.component(cn.count() - 2));
 		}
 		
 		/**
 		 * Parses the principal name from a public key name.
 		 * Do not use this method for group public keys.
 		 * For groups, use instead parsePrincipalNameFromGroupPublicKeyName
 		 * @param publicKeyName the public key name
 		 * @return the corresponding principal name
 		 */
 		public static String parsePrincipalNameFromPublicKeyName(ContentName publicKeyName) {
 			return ContentName.componentPrintNative(VersioningProfile.cutTerminalVersion(publicKeyName).first().lastComponent());
 		}
 
 		public boolean isGroup() { return Arrays.areEqual(GROUP_PRINCIPAL_PREFIX, _typeMarker); }
 		public String friendlyName() { return _friendlyName; }
 		public byte[] distinguishingHash() { return _distinguishingHash; }
 		public CCNTime versionTimestamp() { return _versionTimestamp; }
 
 		/**
 		 * Returns whether a specified name component is the name of a principal
 		 * @param nameComponent the name component
 		 * @return
 		 */
 		public static boolean isPrincipalNameComponent(byte [] nameComponent) {
 			return (DataUtils.isBinaryPrefix(GroupAccessControlProfile.USER_PRINCIPAL_PREFIX, nameComponent) ||
 					DataUtils.isBinaryPrefix(GroupAccessControlProfile.GROUP_PRINCIPAL_PREFIX, nameComponent));
 		}
 		
 		/**
 		 * A first stab
 		 * @throws ContentEncodingException 
 		 */
 		public static byte [] contentPrefixToDistinguishingHash(ContentName name) throws ContentEncodingException {
 			byte [] fullDigest = CCNDigestHelper.digest(name.encode());
 			if (fullDigest.length > DISTINGUISHING_HASH_LENGTH) {
 				byte [] returnedDigest = new byte[DISTINGUISHING_HASH_LENGTH];
 				System.arraycopy(fullDigest, 0, returnedDigest, 0, DISTINGUISHING_HASH_LENGTH);
 				return returnedDigest;
 			}
 			return fullDigest;
 		}
 	}
 	
 	/**
 	 * Returns whether the specified name is the name of a node key
 	 * @param name the name
 	 * @return
 	 */
 	public static boolean isNodeKeyName(ContentName name) {
 		if (!isAccessName(name) || !VersioningProfile.hasTerminalVersion(name)) {
 			return false;
 		}
 		int versionComponent = VersioningProfile.findLastVersionComponent(name);
 		if (name.stringComponent(versionComponent - 1).equals(NODE_KEY_NAME)) {
 			return true;
 		}
 		return false;
 	}
 
 	/**
 	 * Get the name of the node key for a given content node, if there is one.
 	 * This is nodeName/<access marker>/NK, with a version then added for a specific node key.
 	 * @param nodeName the name of the content node
 	 * @return the name of the corresponding node key
 	 */
 	public static ContentName nodeKeyName(ContentName nodeName) {
 		ContentName rootName = accessRoot(nodeName);
 		ContentName nodeKeyName = new ContentName(rootName, ACCESS_CONTROL_MARKER_BYTES, NODE_KEY_NAME_BYTES);
 		return nodeKeyName;
 	}
 	
 	/**
 	 * Get the name of the access control list (ACL) for a given content node.
 	 * This is nodeName/<access marker>/ACL.
 	 * @param nodeName the name of the content node
 	 * @return the name of the corresponding ACL
 	 */
 	public static ContentName aclName(ContentName nodeName) {
 		ContentName baseName = accessRoot(nodeName);
 		return baseName.append(aclPostfix());
 	}
 	
 	public static ContentName aclPostfix() {
 		return ACL_POSTFIX;
 	}
 	
 	/**
 	 * Get the name of the user namespace.
 	 * This assumes a top-level namespace, where the group information is stored in 
 	 * namespace/Groups and namespace/Users..
 	 * @param namespace the top-level name space
 	 * @return the name of the user namespace
 	 */
 	public static ContentName userNamespaceName(ContentName namespace) {
 		return new ContentName(accessRoot(namespace), USER_PREFIX_BYTES);
 	}
 
 	/**
 	 * Get the name of the namespace for a specified user.
 	 * @param userNamespace the name of the user namespace
 	 * @param userName the user name
 	 * @return the name of the namespace for the user
 	 */
 	public static ContentName userNamespaceName(ContentName userNamespace,
 			String userName) {
 		return ContentName.fromNative(userNamespace, userName);
 	}
 	
 	/**
 	 * Get the name of the group namespace.
 	 * This assumes a top-level namespace, where the group information is stored in 
 	 * namespace/Groups and namespace/Users..
 	 * @param namespace the top-level name space
 	 * @return the name of the group namespace
 	 */
 	public static ContentName groupNamespaceName(ContentName namespace) {
 		return new ContentName(accessRoot(namespace), GROUP_PREFIX_BYTES);
 	}
 	
 	/**
 	 * Get the name of the namespace for a specified group.
 	 * @param namespace the top-level namespace
 	 * @param groupFriendlyName the name of the group
 	 * @return the name of the namespace for the group
 	 */
 	public static ContentName groupName(ContentName namespace, String groupFriendlyName) {
 		return ContentName.fromNative(groupNamespaceName(namespace), groupFriendlyName);
 	}
 	
 	/**
 	 * Get the name of a group public key.
 	 * This is the unversioned root. The actual public key is stored at the latest version of
 	 * this name. The private key and decoding blocks are stored under that version, with
 	 * the segments of the group public key.
 	 * @param groupNamespaceName the namespace of the group
 	 * @param groupFriendlyName the name of the group
 	 * @return the name of the group public key
 	 */
 	public static ContentName groupPublicKeyName(ContentName groupNamespaceName, String groupFriendlyName) {
 		return ContentName.fromNative(ContentName.fromNative(groupNamespaceName, groupFriendlyName),  AccessControlProfile.GROUP_PUBLIC_KEY_NAME);
 	}
 	
 	/**
 	 * Get the name of the public key of a group specified by its full name
 	 * @param groupFullName the full name of the group
 	 * @return the name of the group public key
 	 */
 	public static ContentName groupPublicKeyName(ContentName groupFullName) {
 		return ContentName.fromNative(groupFullName,  AccessControlProfile.GROUP_PUBLIC_KEY_NAME);
 	}
 	
 	/**
 	 * Get the name of a group membership list for a specified group
 	 * @param groupNamespaceName the namespace of the group
 	 * @param groupFriendlyName the name of the group
 	 * @return the name of the group membership list
 	 */
 	public static ContentName groupMembershipListName(ContentName groupNamespaceName, String groupFriendlyName) {
 		return ContentName.fromNative(ContentName.fromNative(groupNamespaceName, groupFriendlyName),  GROUP_MEMBERSHIP_LIST_NAME);
 	}
 
 	/**
 	 * Get the friendly name of a specified group
 	 * @param groupName the full name of the group
 	 * @return the friendly name of the group
 	 */
 	public static String groupNameToFriendlyName(ContentName groupName) {
 		return ContentName.componentPrintNative(groupName.lastComponent());
 	}
 
 	/**
 	 * Get the name of a group private key.
 	 * We hang the wrapped private key directly off the public key version.
 	 * @param groupPublicKeyNameAndVersion the versioned name of the group public key
 	 * @return the versioned name of the group private key
 	 */
 	public static ContentName groupPrivateKeyDirectory(ContentName groupPublicKeyNameAndVersion) {
 		return groupPublicKeyNameAndVersion;
 	}
 	
 	public static ContentName groupPointerToParentGroupName(ContentName groupFullName) {
 		return ContentName.fromNative(groupFullName, GROUP_POINTER_TO_PARENT_GROUP_NAME);
 	}
 
 }
