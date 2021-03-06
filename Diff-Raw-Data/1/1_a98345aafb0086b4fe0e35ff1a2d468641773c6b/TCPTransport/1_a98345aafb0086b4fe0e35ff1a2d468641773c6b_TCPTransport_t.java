 package org.msgpack.rpc.client;
 
 import java.util.ArrayList;
 import java.util.List;
 
 public class TCPTransport {
 	protected Session session;
 	protected EventLoop loop;
 	protected Boolean isConnecting;
 	protected Boolean isConnected;
 	protected TCPSocket socket;
 	protected List<Object> pendingMessages;
 
 	public TCPTransport(Session session, EventLoop loop) {
 		this.session = session;
 		this.loop = loop;
 		this.isConnecting = false;
 		this.isConnected = false;
 		this.socket = new TCPSocket(session.getAddress(), loop, this);
 		this.pendingMessages = new ArrayList<Object>();
 	}
 
 	// hide the connect(2) latency from the Transport user
 	public synchronized void sendMessage(Object msg) throws Exception {
 		if (isConnected) {
 			socket.trySend(msg);
 		} else {
 			if (!isConnecting) {
 				socket.tryConnect();
 				isConnecting = true;
 			}
 			pendingMessages.add(msg);
 		}
 	}
 
 	protected synchronized void trySendPending() throws Exception {
 		for (Object msg : pendingMessages)
 			socket.trySend(msg);
		pendingMessages.clear();
 	}
 	
 	public synchronized void tryClose() {
 		if (socket != null)
 			socket.tryClose();
 		isConnecting = false;
 		isConnected = false;
 		socket = null;
 		pendingMessages.clear();
 	}
 
 	// callback
 	public synchronized void onConnected() throws Exception {
 		isConnecting = false;
 		isConnected = true;
 		trySendPending();
 	}
 
 	// callback
 	public void onConnectFailed() {
 		tryClose();
 		session.onConnectFailed();
 	}
 	
 	// callback
 	public void onMessageReceived(Object replyObject) throws Exception {
 		session.onMessageReceived(replyObject);
 	}
 
 	// callback
 	public void onClosed() {
 		tryClose();
 		session.onClosed();
 	}
 	
 	// callback
 	public void onFailed(Exception e) {
 		tryClose();
 		session.onFailed(e);
 	}
 }
