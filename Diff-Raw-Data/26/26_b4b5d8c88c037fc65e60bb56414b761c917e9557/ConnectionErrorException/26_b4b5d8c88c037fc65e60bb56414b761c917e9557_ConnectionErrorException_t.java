 package com.nexus.network.exception;
 
 import java.io.IOException;
 
 
 public class ConnectionErrorException extends IOException{
 
 	private static final long serialVersionUID = 1880651125876166847L;

	public ConnectionErrorException(){
		super();
	}
	
	public ConnectionErrorException(String msg){
		super(msg);
	}
 	
	public ConnectionErrorException(Throwable t){
		super(t);
	}
 }
