 /* Copyright (c) 2008, Avian Contributors
 
    Permission to use, copy, modify, and/or distribute this software
    for any purpose with or without fee is hereby granted, provided
    that the above copyright notice and this permission notice appear
    in all copies.
 
    There is NO WARRANTY for this software.  See license.txt for
    details. */
 
 package java.nio.channels;
 
 import java.io.IOException;
 
 import java.net.InetSocketAddress;
 import java.net.SocketAddress;
 import java.net.ServerSocket;
 
 public class ServerSocketChannel extends SelectableChannel {
   private final SocketChannel channel = new SocketChannel();
 
   public static ServerSocketChannel open() {
     return new ServerSocketChannel();
   }
 
  public int socketFD() {
    return channel.socketFD();
  }

   public SelectableChannel configureBlocking(boolean v) throws IOException {
     return channel.configureBlocking(v);
   }
 
   public void close() throws IOException {
     channel.close();
   }
 
   public SocketChannel accept() throws Exception {
     SocketChannel c = new SocketChannel();
     c.socket = doAccept();
     c.connected = true;
     return c;
   }
 
   public ServerSocket socket() {
     return new Handle();
   }
 
   private int doAccept() throws IOException {
     return natDoAccept(channel.socket);
   }
 
   private int doListen(String host, int port) throws IOException {
     return natDoListen(host, port);
   }
 
   public class Handle extends ServerSocket {
     public void bind(SocketAddress address)
       throws IOException
     {
       InetSocketAddress a;
       try {
         a = (InetSocketAddress) address;
       } catch (ClassCastException e) {
         throw new IllegalArgumentException();
       }
       channel.socket = doListen(a.getHostName(), a.getPort());
       channel.configureBlocking(channel.isBlocking());
     }
   }
 
   private static native int natDoAccept(int socket) throws IOException;
   private static native int natDoListen(String host, int port) throws IOException;
 }
