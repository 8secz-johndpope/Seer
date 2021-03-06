 /**
  * Part of the CCNx Java Library.
  *
  * Copyright (C) 2009 Palo Alto Research Center, Inc.
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
 
 package org.ccnx.ccn.profiles.ccnd;
 
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 
 import org.ccnx.ccn.CCNHandle;
 import org.ccnx.ccn.impl.encoding.BinaryXMLCodec;
 import org.ccnx.ccn.impl.encoding.GenericXMLEncodable;
 import org.ccnx.ccn.impl.encoding.XMLCodecFactory;
 import org.ccnx.ccn.impl.encoding.XMLDecoder;
 import org.ccnx.ccn.impl.encoding.XMLEncodable;
 import org.ccnx.ccn.impl.encoding.XMLEncoder;
 import org.ccnx.ccn.impl.support.Log;
 import org.ccnx.ccn.io.content.ContentDecodingException;
 import org.ccnx.ccn.io.content.ContentEncodingException;
 import org.ccnx.ccn.protocol.ContentName;
 import org.ccnx.ccn.protocol.MalformedContentNameStringException;
 import org.ccnx.ccn.protocol.PublisherPublicKeyDigest;
 
 /**
  *
  */
 public class FaceManager extends CCNDaemonHandle /* extends GenericXMLEncodable implements XMLEncodable */ {
 
 	
 	public enum ActionType {
 		NewFace ("newface"), DestroyFace ("destroyface"), QueryFace ("queryface");
 
 		ActionType(String st) { this.st = st; }
 		private final String st;
 		public String value() { return st; }
 	}
 	
 	public enum NetworkProtocol {
 		UDP (17), TCP (6);
 		NetworkProtocol(Integer i) { this.i = i; }
 		private final Integer i;
 		public Integer value() { return i; }
 	}
 		
 
 public class FaceInstance extends GenericXMLEncodable implements XMLEncodable {
 	/* extends CCNEncodableObject<PolicyXML>  */
 	
 	/**
 	 * From the XML definitions:
 	 * <xs:element name="Action" type="xs:string" minOccurs="0" maxOccurs="1"/>
 	 * <xs:element name="PublisherPublicKeyDigest" type="DigestType" minOccurs="0" maxOccurs="1"/>
 	 * <xs:element name="FaceID" type="xs:nonNegativeInteger" minOccurs="0" maxOccurs="1"/>
 	 * <xs:element name="IPProto" type="xs:nonNegativeInteger" minOccurs="0" maxOccurs="1"/>
 	 * <xs:element name="Host" type="xs:string" minOccurs="0" maxOccurs="1"/>
 	 * <xs:element name="Port" type="xs:nonNegativeInteger" minOccurs="0" maxOccurs="1"/>
 	 * <xs:element name="MulticastInterface" type="xs:string" minOccurs="0" maxOccurs="1"/>
 	 * <xs:element name="MulticastTTL" type="xs:nonNegativeInteger" minOccurs="0" maxOccurs="1"/>
 	 * <xs:element name="FreshnessSeconds" type="xs:nonNegativeInteger" minOccurs="0" maxOccurs="1"/>
 	 */
 
 	protected static final String 	FACE_INSTANCE_OBJECT_ELEMENT = "FaceInstance";
 	protected static final String		ACTION_ELEMENT = "Action";
 	protected static final String		FACE_ID_ELEMENT = "FaceID";
 	protected static final String		IP_PROTO_ELEMENT = "IPProto";
 	protected static final String		HOST_ELEMENT = "Host";
 	protected static final String		PORT_ELEMENT = "Port";
 	protected static final String		MC_INTER_ELEMENT = "MulticastInterface";
 	protected static final String		MC_TTL_ELEMENT = "MulticastTTL";
 	protected static final String		FRESHNESS_ELEMENT = "FreshnessSeconds";
 
 	protected String		_action;
 	protected PublisherPublicKeyDigest _ccndID;
 	protected Integer		_faceID;
 	protected NetworkProtocol		_ipProto;
 	protected String		_host;
 	protected Integer		_port;
 	protected String		_multicastInterface;
 	protected Integer		_multicastTTL;
 	protected Integer 		_lifetime;
 
 
 	public FaceInstance(ActionType action, PublisherPublicKeyDigest ccndID, NetworkProtocol ipProto, String host, Integer port) {
 		_action = action.value();
 		_ccndID = ccndID;
 		_ipProto = ipProto;
 		_host = host;
 		_port = port;
 	}
 
 	public FaceInstance(ActionType action, PublisherPublicKeyDigest ccndID, Integer faceID) {
 		_action = action.value();
 		_ccndID = ccndID;
 		_faceID = faceID;
 	}
 
 	public FaceInstance(ActionType action, PublisherPublicKeyDigest ccndID, NetworkProtocol ipProto, String host, Integer port,
 			String multicastInterface, Integer multicastTTL, Integer lifetime) {
 		_action = action.value();
 		_ccndID = ccndID;
 		_ipProto = ipProto;
 		_host = host;
 		_port = port;
 		_multicastInterface = multicastInterface;
 		_multicastTTL = multicastTTL;
 		_lifetime = lifetime;
 	}
 
 	public FaceInstance(byte[] raw) {
 		ByteArrayInputStream bais = new ByteArrayInputStream(raw);
 		XMLDecoder decoder = XMLCodecFactory.getDecoder(BinaryXMLCodec.CODEC_NAME);
 		try {
 			decoder.beginDecoding(bais);
 			decode(decoder);
 			decoder.endDecoding();	
 		} catch (ContentDecodingException e) {
 			String reason = e.getMessage();
 			Log.fine("Unexpected error decoding FaceInstance from bytes.  reason: " + reason + "\n");
 			Log.warningStackTrace(e);
 			throw new IllegalArgumentException("Unexpected error decoding FaceInstance from bytes.  reason: " + reason);
 		}
 	}
 
 	public Integer faceID() { return _faceID; }
 	public void setFaceID(Integer faceID) { _faceID = faceID; }
 
 	public String action() { return _action; }
 
 	
 	public PublisherPublicKeyDigest ccndId() { return _ccndId; }
 	public void setccndId(PublisherPublicKeyDigest id) { _ccndId = id; }
 
 
 	public String toFormattedString() {
 		String out = "";
 		if (null != _action) {
 			out.concat("Action: "+ _action + "\n");
 		} else {
 			out.concat("Action: not present\n");
 		}
 		if (null != _faceID) {
 			out.concat("FaceID: "+ _faceID.toString() + "\n");
 		} else {
 			out.concat("FaceID: not present\n");
 		}
 		if (null != _host) {
 			out.concat("Host: "+ _host + "\n");
 		} else {
 			out.concat("Host: not present\n");
 		}
 		if (null != _port) {
 			out.concat("Port: "+ _port.toString() + "\n");
 		} else {
 			out.concat("Port: not present\n");
 		}
 		return out;
 	}	
 
 	public byte[] getBinaryEncoding() {
 		// Do setup. Binary codec doesn't write a preamble or anything.
 		// If allow to pick, text encoder would sometimes write random stuff...
 		ByteArrayOutputStream baos = new ByteArrayOutputStream();
 		XMLEncoder encoder = XMLCodecFactory.getEncoder(BinaryXMLCodec.CODEC_NAME);
 		try {
 			encoder.beginEncoding(baos);
 			encode(encoder);
 			encoder.endEncoding();	
 		} catch (ContentEncodingException e) {
 			String reason = e.getMessage();
 			Log.fine("Unexpected error encoding allocated FaceInstance.  reason: " + reason + "\n");
 			Log.warningStackTrace(e);
 			throw new IllegalArgumentException("Unexpected error encoding allocated FaceInstance.  reason: " + reason);
 		}
 		return baos.toByteArray();
 	}
 
 	public boolean validateAction(String action) {
 		if (action != null){
 			if (action.equals(ActionType.NewFace.value()) || 
 					action.equals(ActionType.DestroyFace.value()) || 
 					action.equals(ActionType.QueryFace.value())) {
 				return true;
 			}
 		}
 		return false;
 	}
 
 	/**
 	 * Used by NetworkObject to decode the object from a network stream.
 	 * @see org.ccnx.ccn.impl.encoding.XMLEncodable
 	 */
 	public void decode(XMLDecoder decoder) throws ContentDecodingException {
 		decoder.readStartElement(getElementLabel());
 		if (decoder.peekStartElement(ACTION_ELEMENT)) {
 			_action = decoder.readUTF8Element(ACTION_ELEMENT); 
 		}
 		if (decoder.peekStartElement(PublisherPublicKeyDigest.PUBLISHER_PUBLIC_KEY_DIGEST_ELEMENT)) {
 			_ccndID = new PublisherPublicKeyDigest();
 			_ccndID.decode(decoder);
 		}
 		if (decoder.peekStartElement(FACE_ID_ELEMENT)) {
 			_faceID = decoder.readIntegerElement(FACE_ID_ELEMENT); 
 		}
 		if (decoder.peekStartElement(IP_PROTO_ELEMENT)) {
 			Integer proto = decoder.readIntegerElement(IP_PROTO_ELEMENT);
 			_ipProto = null;
			if (NetworkProtocol.TCP.value() == proto) {
 				_ipProto = NetworkProtocol.TCP;
			} else if (NetworkProtocol.UDP.value() == proto) {
 				_ipProto = NetworkProtocol.UDP;
 			} else {
 				throw new ContentDecodingException("FaceInstance.decoder.  Invalid " + IP_PROTO_ELEMENT + " field: " + proto.toString());
 			}
 		}
 		if (decoder.peekStartElement(HOST_ELEMENT)) {
 			_host = decoder.readUTF8Element(HOST_ELEMENT); 
 		}
 		if (decoder.peekStartElement(PORT_ELEMENT)) {
 			 _port = decoder.readIntegerElement(PORT_ELEMENT); 
 		}
 		if (decoder.peekStartElement(MC_INTER_ELEMENT)) {
 			_multicastInterface = decoder.readUTF8Element(MC_INTER_ELEMENT); 
 		}
 		if (decoder.peekStartElement(MC_TTL_ELEMENT)) {
 			_multicastTTL = decoder.readIntegerElement(MC_TTL_ELEMENT); 
 		}
 		if (decoder.peekStartElement(FRESHNESS_ELEMENT)) {
 			_lifetime = decoder.readIntegerElement(FRESHNESS_ELEMENT); 
 		}
 		decoder.readEndElement();
 	}
 
 	/**
 	 * Used by NetworkObject to encode the object to a network stream.
 	 * @see org.ccnx.ccn.impl.encoding.XMLEncodable
 	 */
 	public void encode(XMLEncoder encoder) throws ContentEncodingException {
 		if (!validate()) {
 			throw new ContentEncodingException("Cannot encode " + this.getClass().getName() + ": field values missing.");
 		}
 		encoder.writeStartElement(getElementLabel());
 		if (null != _action && _action.length() != 0)
 			encoder.writeElement(ACTION_ELEMENT, _action);	
 		if (null != _ccndID) {
 			_ccndID.encode(encoder);
 		}
 		if (null != _faceID) {
 			encoder.writeIntegerElement(FACE_ID_ELEMENT, _faceID);
 		}
 		if (null != _ipProto) {
 			encoder.writeIntegerElement(IP_PROTO_ELEMENT, _ipProto.value());
 		}
 		if (null != _host && _host.length() != 0) {
 			encoder.writeElement(HOST_ELEMENT, _host);	
 		}
 		if (null != _port) {
 			encoder.writeIntegerElement(PORT_ELEMENT, _port);
 		}
 		if (null != _multicastInterface && _multicastInterface.length() != 0) {
 			encoder.writeElement(MC_INTER_ELEMENT, _multicastInterface);
 		}
 		if (null != _multicastTTL) {
 			encoder.writeIntegerElement(MC_TTL_ELEMENT, _multicastTTL);
 		}
 		if (null != _lifetime) {
 			encoder.writeIntegerElement(FRESHNESS_ELEMENT, _lifetime);
 		}
 		encoder.writeEndElement();   			
 	}
 
 	@Override
 	public String getElementLabel() { return FACE_INSTANCE_OBJECT_ELEMENT; }
 
 	@Override
 	public boolean validate() {
 		if (validateAction(_action)){
 			return true;
 		}
 		return false;
 	}
 
 	@Override
 	public int hashCode() {
 		final int prime = 31;
 		int result = 1;
 		result = prime * result + ((_action == null) ? 0 : _action.hashCode());
 		result = prime * result + ((_ccndID == null) ? 0 : _ccndID.hashCode());
 		result = prime * result + ((_faceID == null) ? 0 : _faceID.hashCode());
 		result = prime * result + ((_ipProto == null) ? 0 : _ipProto.hashCode());
 		result = prime * result + ((_host == null) ? 0 : _host.hashCode());
 		result = prime * result + ((_port == null) ? 0 : _port.hashCode());
 		result = prime * result + ((_multicastInterface == null) ? 0 : _multicastInterface.hashCode());
 		result = prime * result + ((_multicastTTL == null) ? 0 : _multicastTTL.hashCode());
 		result = prime * result + ((_lifetime == null) ? 0 : _lifetime.hashCode());
 		return result;
 	}
 
 	@Override
 	public boolean equals(Object obj) {
 		if (this == obj) return true;
 		if (obj == null) return false;
 		if (getClass() != obj.getClass()) return false;
 		FaceInstance other = (FaceInstance) obj;
 		if (_action == null) {
 			if (other._action != null) return false;
 		} else if (!_action.equals(other._action)) return false;
 		if (_ccndID == null) {
 			if (other._ccndID != null) return false;
 		} else if (!_ccndID.equals(other._ccndID)) return false;
 		if (_faceID == null) {
 			if (other._faceID != null) return false;
 		} else if (!_faceID.equals(other._faceID)) return false;
 		if (_ipProto == null) {
 			if (other._ipProto != null) return false;
 		} else if (!_ipProto.equals(other._ipProto)) return false;
 		if (_host == null) {
 			if (other._host != null) return false;
 		} else if (!_host.equals(other._host)) return false;
 		if (_port == null) {
 			if (other._port != null) return false;
 		} else if (!_port.equals(other._port)) return false;
 		if (_multicastInterface == null) {
 			if (other._multicastInterface != null) return false;
 		} else if (!_multicastInterface.equals(other._multicastInterface)) return false;
 		if (_multicastTTL == null) {
 			if (other._multicastTTL != null) return false;
 		} else if (!_multicastTTL.equals(other._multicastTTL)) return false;
 		if (_lifetime == null) {
 			if (other._lifetime != null) return false;
 		} else if (!_lifetime.equals(other._lifetime)) return false;
 		return true;
 	}
 
 } /* FaceInstance */
 
 /*************************************************************************************/
 /*************************************************************************************/
 
 	public FaceManager(CCNHandle handle) throws CCNDaemonException {
 		super(handle, null);
 	}
 
 	public FaceManager(CCNHandle handle, PublisherPublicKeyDigest ccndID) throws CCNDaemonException {
 		super(handle, ccndID);
 		
 	}
 	public Integer createFace(NetworkProtocol ipProto, String host, Integer port) 
 							throws CCNDaemonException {
 		return this.createFace(ipProto, host, port, null, null, null);
 	}
 	
 	public Integer createFace(NetworkProtocol ipProto, String host, Integer port, Integer freshnessSeconds) 
 							throws CCNDaemonException {
 		return this.createFace(ipProto, host, port, null, null, freshnessSeconds);
 	}
 
 	public Integer createFace(NetworkProtocol ipProto, String host, Integer port,
 			String multicastInterface, Integer multicastTTL, Integer freshnessSeconds) 
 							throws CCNDaemonException {
 		FaceInstance face = new FaceInstance(ActionType.NewFace, _ccndId, ipProto, host, port, 
 											multicastInterface, multicastTTL, freshnessSeconds);
 		FaceInstance returned = this.sendIt(face);
 		return returned.faceID();
 	}
 		
 	public void deleteFace(Integer faceID) throws CCNDaemonException {
 		FaceInstance face = new FaceInstance(ActionType.DestroyFace, _ccndId, faceID);
 		this.sendIt(face);
 	}
 	
 	public FaceInstance queryFace(Integer faceID) throws CCNDaemonException {
 		FaceInstance face = new FaceInstance(ActionType.QueryFace, _ccndId, faceID);
 		FaceInstance returned = this.sendIt(face);
 		return returned;
 	}
 	
 	private FaceInstance sendIt(FaceInstance face) throws CCNDaemonException {
 
 		byte[] faceBits = face.getBinaryEncoding();
 
 		/*
 		 * First create a name that looks like 'ccnx:/ccnx/CCNDId/action/ContentObjectWithFaceInIt'
 		 */
 		final String startURI = "ccnx:/ccnx/";
 		ContentName interestName = null;
 		try {
 			interestName = ContentName.fromURI(startURI);
 			interestName = ContentName.fromNative(interestName, _ccndId.digest());
 			interestName = ContentName.fromNative(interestName, face.action());
 		} catch (MalformedContentNameStringException e) {
 			String reason = e.getMessage();
 			Log.fine("Call to create ContentName failed: " + reason + "\n");
 			Log.warningStackTrace(e);
 			String msg = ("Unexpected MalformedContentNameStringException in call creating ContentName, reason: " + reason);
 			throw new CCNDaemonException(msg);
 		}
 
 		byte[] payloadBack = super.sendIt(interestName, faceBits);
 		FaceInstance faceBack = new FaceInstance(payloadBack);
 
 		String formattedFace = faceBack.toFormattedString();
 		Log.fine(formattedFace);
 		return faceBack; 
 	} /* private FaceInstance sendIt(FaceInstance face) throws CCNDaemonException */
 	
 
 }
