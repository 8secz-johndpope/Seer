 package main.java;
 
 import java.net.MalformedURLException;
 import java.rmi.NotBoundException;
 import java.rmi.RemoteException;
 
 public interface DA_BYZ_RMI extends java.rmi.Remote{
 
 	String receive(Message m) throws RemoteException;
 
	void sendMsg(String lieutenants, Message msg) throws RemoteException;
 
 	void sendAckMsg(Message m) throws MalformedURLException, RemoteException,
 			NotBoundException;
 
	void recvAckMsg(AckMessage m) throws RemoteException;
	void broadcast(OrderValue order) throws RemoteException;
 	
 }
