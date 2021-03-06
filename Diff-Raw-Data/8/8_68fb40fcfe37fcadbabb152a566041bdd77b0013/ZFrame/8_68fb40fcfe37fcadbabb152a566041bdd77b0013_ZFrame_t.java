 package org.zeromq;
 
 
 import org.zeromq.ZMQ.Socket;
 
 /**
  * ZFrame
  * 
  * @author rsmith (at) rsbatechnology (dot) co (dot) uk
  * 
  * The ZFrame class provides methods to send and receive single message
  * frames across 0MQ sockets. A 'frame' corresponds to one underlying zmq_msg_t in the libzmq code.
  * When you read a frame from a socket, the more() method indicates if the frame is part of an 
  * unfinished multipart message.  The send() method normally destroys the frame, but with the ZFRAME_REUSE flag, you can send
  * the same frame many times. Frames are binary, and this class has no special support for text data.
  * 
  * Based on <a href="http://github.com/zeromq/czmq/blob/master/src/zframe.c">zframe.c</a> in czmq
  * 
  */
 public class ZFrame {
 
 	private boolean more;
 	
 	private byte[] data;
 	
 
 	/**
 	 * Class Constructor
 	 * Creates an empty frame.
 	 * (Useful when reading frames from a 0MQ Socket)
 	 */
 	protected ZFrame() {
 		// Empty constructor
 	}
 
 	/**
 	 * Class Constructor
 	 * Copies message data into ZFrame object
 	 * @param data
 	 * 			Data to copy into ZFrame object
 	 */
 	protected ZFrame(byte[] data)
 	{
 		if (data != null) {
 			this.data = (byte[]) data.clone();
 		}
 	}
 	
 	/**
 	 * Class Constructor
 	 * Copies String into frame data
 	 * @param data
 	 */
 	protected ZFrame(String data) {
 		if (data != null) {
 			this.data = data.getBytes();
 		}
 	}
 	
 	/**
 	 * Destructor.
 	 */
 	public void destroy() {
 		if (hasData())
 			data = null;
 	}
 	
 	/**
 	 * @return the data
 	 */
 	public byte[] getData() {
 		return data;
 	}
 
 	/**
 	 * @return More flag, true if last read had MORE message parts to come
 	 */
 	public boolean hasMore() {
 		return more;
 	}
 
 	/**
 	 * Returns byte size of frame, if set, else 0
 	 * @return
 	 * 			Number of bytes in frame data, else 0
 	 */
 	public int size() {
 		if (hasData())
 			return data.length;
 		else
 			return 0;
 	}
 	
 	/**
 	 * Convenience method to ascertain if this frame contains some message data
 	 * @return
 	 * 			True if frame contains data
 	 */
 	public boolean hasData() {
 		return data != null;
 	}
 	
 	/**
 	 * Internal method to call org.zeromq.Socket send() method.
 	 * @param socket
 	 * 			0MQ socket to send on
 	 * @param flags
 	 * 			Valid send() method flags, defined in org.zeromq.ZMQ class
 	 */
 	private void send(Socket socket, int flags) {
 		if (socket == null) {
 			throw new IllegalArgumentException("socket parameter must be set");
 		}
 		if (!hasData()) {
 			throw new IllegalAccessError("Cannot send frame without data");
 		}
 		
 		// Note the jzmq Socket.cpp JNI class does a memcpy of the byte data before calling
 		// the 0MQ send function, so don't have to clone the message data again here.
 		socket.send(data, flags);
 	}
 	
 	/**
 	 * Sends frame to socket if it contains any data.
 	 * Frame contents are kept after the send.
 	 * @param socket	
 	 * 			0MQ socket to send frame
 	 * @param flags
 	 * 			Valid send() method flags, defined in org.zeromq.ZMQ class	
 	 */
 	public void sendAndKeep(Socket socket, int flags) {
 		send(socket, flags);
 	}
 	
 	/**
 	 * Sends frame to socket if it contains any data.
 	 * Frame contents are kept after the send.
 	 * Uses default behaviour of Socket.send() method, with no flags set
 	 * @param socket	
 	 * 			0MQ socket to send frame
 	 */
 	public void sendAndKeep(Socket socket) {
 		sendAndKeep(socket, 0);		
 	}
 
 	/**
 	 * Sends frame to socket if it contains data.
 	 * Use this method to send a frame and destroy the data after.
 	 * @param socket
 	 * 			0MQ socket to send frame
 	 * @param flags
 	 * 			Valid send() method flags, defined in org.zeromq.ZMQ class	
 	 */
 	public void sendAndDestroy(Socket socket, int flags) {
 		send(socket, flags);
 		destroy();
 	}
 
 	/**
 	 * Sends frame to socket if it contains data.
 	 * Use this method to send an isolated frame and destroy the data after.
 	 * Uses default behaviour of Socket.send() method, with no flags set
 	 * @param socket
 	 * 			0MQ socket to send frame
 	 */
 	public void sendAndDestroy(Socket socket) {
 		sendAndDestroy(socket, 0);
 	}
 	
 	/**
 	 * Creates a new frame that duplicates an existing frame
 	 * @return
 	 *			Duplicate of frame; message contents copied into new byte array
 	 */
 	public ZFrame duplicate() {
 		return new ZFrame(this.data.clone());
 	}
 	
 	/**
 	 * Returns true if both frames have byte - for byte identical data
 	 * @param other
 	 * 			The other ZFrame to compare
 	 * @return
 	 * 			True if both ZFrames have same byte-identical data, else false
 	 */
 	public boolean hasSameData(ZFrame other) {
 		if (other == null) return false;
 		
 		if (size() == other.size()) {
 			if (hasData() && other.hasData()) {
 				for (int i = 0;i<size();i++) {
 					if (this.data[i] != other.data[i])
 						return false;
 				}
 				return true;
 			}
 		}
 		return false;
 	}
 	
 	/**
 	 * Sets new contents for frame
 	 * @param data
 	 * 			New byte array contents for frame
 	 */
 	public void reset(byte[] data) {
 		this.data = data;
 	}
 	
 	/**
 	 * Returns frame data as a printable hex string
 	 * @return
 	 */
 	public String strhex() {		
 		String hexChar = "0123456789ABCDEF";
 		
 		StringBuffer b = new StringBuffer();
 		for (int nbr = 0;nbr<data.length;nbr++) {
			int b1 = data[nbr] >>> 4 & 0xf;
			int b2 = data[nbr] & 0xf;
			b.append(hexChar.charAt(b1));
			b.append(hexChar.charAt(b2));
 		}
 		return b.toString();
 	}
 	
 	/**
 	 * String equals.
 	 * Uses String compareTo for the comparison (lexigraphical)
 	 * @param str
 	 * 			String to compare with frame data
 	 * @return
 	 * 			True if frame body data matches given string
 	 */
 	public boolean streq(String str) {
 		if (!hasData()) return false;
 		return new String(this.data).compareTo(str) == 0;
 	}
 	
 	/**
 	 * Returns a human - readable representation of frame's data
 	 * @return
 	 * 			A text string or hex-encoded string if data contains any non-printable ASCII characters
 	 */
 	public String toString() {
 		if (!hasData()) return null;
 		// Dump message as text or hex-encoded string
 		boolean isText = true;
 		for (int i = 0;i<data.length;i++) {
 			if (data[i] < 32 || data[i] > 127)
 				isText = false;
 		}
 		if (isText) 
 			return new String(data);
 		else
 			return strhex();
 	}
 	
 	/**
 	 * Internal method to call recv on the socket.
 	 * Does not trap any ZMQExceptions but expects caling routine to handle them.
 	 * @param socket
 	 * 			0MQ socket to read from
 	 * @return
 	 * 			Byte array
 	 */
 	private byte[] recv(Socket socket, int flags) {
 		if (socket == null)
 			throw new IllegalArgumentException("socket parameter must not be null");
 		
 		data = socket.recv(flags);
 		more = socket.hasReceiveMore();
 		return data;
 	}
 	
     /**
      * Receives single frame from socket, returns the received frame object, or null if the recv
      * was interrupted. Does a blocking recv, if you want to not block then use
      * recvFrame(socket, ZMQ.DONTWAIT);
      * 
      * @param	socket
      * 				Socket to read from
      * @return  
      * 				received frame, else null
      */
 	public static ZFrame recvFrame(Socket socket) {
 		ZFrame f = new ZFrame();
 		f.recv(socket, 0);
 		return f;
 	}
 	
     /**
      * Receive a new frame off the socket, Returns newly-allocated frame, or
      * null if there was no input waiting, or if the read was interrupted.
      * @param	socket
      * 				Socket to read from
      * @param	flags
      * 				Pass flags to 0MQ socket.recv call
      * @return  
      * 				received frame, else null
      */	
 	public static ZFrame recvFrame(Socket socket, int flags) {
 		ZFrame f = new ZFrame();
 		f.recv(socket, flags);
 		return f;
 	}
 	
 }
