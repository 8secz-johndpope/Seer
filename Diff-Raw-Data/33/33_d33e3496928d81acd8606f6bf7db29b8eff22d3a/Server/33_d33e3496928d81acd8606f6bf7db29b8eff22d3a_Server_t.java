 /**
  * Copyright 2005 The Apache Software Foundation
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.apache.hadoop.ipc;
 
 import java.io.IOException;
 import java.io.EOFException;
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
 import java.io.BufferedOutputStream;
 import java.io.StringWriter;
 import java.io.PrintWriter;
 import java.io.ByteArrayInputStream;
 
 import java.nio.ByteBuffer;
 import java.nio.channels.SelectionKey;
 import java.nio.channels.Selector;
 import java.nio.channels.ServerSocketChannel;
 import java.nio.channels.SocketChannel;
 import java.nio.BufferUnderflowException;
 
 import java.net.InetSocketAddress;
 import java.net.Socket;
 
 import java.util.Collections;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Iterator;
 import java.util.Random;
 
 import org.apache.commons.logging.*;
 
 import org.apache.hadoop.conf.Configurable;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.io.Writable;
 import org.apache.hadoop.io.WritableUtils;
 
 import org.mortbay.http.nio.SocketChannelOutputStream;
 
 /** An abstract IPC service.  IPC calls take a single {@link Writable} as a
  * parameter, and return a {@link Writable} as their value.  A service runs on
  * a port and is defined by a parameter class and a value class.
  * 
  * @author Doug Cutting
  * @see Client
  */
 public abstract class Server {
   public static final Log LOG =
     LogFactory.getLog("org.apache.hadoop.ipc.Server");
 
   private static final ThreadLocal SERVER = new ThreadLocal();
 
   /** Returns the server instance called under or null.  May be called under
    * {@link #call(Writable)} implementations, and under {@link Writable}
    * methods of paramters and return values.  Permits applications to access
    * the server context.*/
   public static Server get() {
     return (Server)SERVER.get();
   }
 
   private int port;                               // port we listen on
   private int handlerCount;                       // number of handler threads
   private int maxQueuedCalls;                     // max number of queued calls
   private Class paramClass;                       // class of call parameters
   private int maxIdleTime;                        // the maximum idle time after 
                                                   // which a client may be disconnected
   private int thresholdIdleConnections;           // the number of idle connections
                                                   // after which we will start
                                                   // cleaning up idle 
                                                   // connections
   int maxConnectionsToNuke;                       // the max number of 
                                                   // connections to nuke
                                                   //during a cleanup
   
   private Configuration conf;
 
   private int timeout;
 
   private boolean running = true;                 // true while server runs
   private LinkedList callQueue = new LinkedList(); // queued calls
   private Object callDequeued = new Object();     // used by wait/notify
 
   private List connectionList = 
        Collections.synchronizedList(new LinkedList()); //maintain a list
                                                        //of client connectionss
   private Listener listener;
   private int numConnections = 0;
   
   /** A call queued for handling. */
   private static class Call {
     private int id;                               // the client's call id
     private Writable param;                       // the parameter passed
     private Connection connection;                // connection to client
 
     public Call(int id, Writable param, Connection connection) {
       this.id = id;
       this.param = param;
       this.connection = connection;
     }
   }
 
   /** Listens on the socket. Creates jobs for the handler threads*/
   private class Listener extends Thread {
     
     private ServerSocketChannel acceptChannel = null; //the accept channel
     private Selector selector = null; //the selector that we use for the server
     private InetSocketAddress address; //the address we bind at
     private Random rand = new Random();
     private long lastCleanupRunTime = 0; //the last time when a cleanup connec-
                                          //-tion (for idle connections) ran
     private long cleanupInterval = 10000; //the minimum interval between 
                                           //two cleanup runs
     
     public Listener() throws IOException {
       address = new InetSocketAddress(port);
       // Create a new server socket and set to non blocking mode
       acceptChannel = ServerSocketChannel.open();
       acceptChannel.configureBlocking(false);
 
       // Bind the server socket to the local host and port
       acceptChannel.socket().bind(address);
       // create a selector;
       selector= Selector.open();
 
       // Register accepts on the server socket with the selector.
       acceptChannel.register(selector, SelectionKey.OP_ACCEPT);
       this.setName("Server listener on port " + port);
       this.setDaemon(true);
     }
     /** cleanup connections from connectionList. Choose a random range
      * to scan and also have a limit on the number of the connections
      * that will be cleanedup per run. The criteria for cleanup is the time
      * for which the connection was idle. If 'force' is true then all 
      * connections will be looked at for the cleanup.
      */
     private void cleanupConnections(boolean force) {
       if (force || numConnections > thresholdIdleConnections) {
         long currentTime = System.currentTimeMillis();
         if (!force && (currentTime - lastCleanupRunTime) < cleanupInterval) {
           return;
         }
         int start = 0;
         int end = numConnections - 1;
         if (!force) {
           start = rand.nextInt() % numConnections;
           end = rand.nextInt() % numConnections;
           int temp;
           if (end < start) {
             temp = start;
             start = end;
             end = temp;
           }
         }
         int i = start;
         int numNuked = 0;
         while (i <= end) {
           Connection c;
           synchronized (connectionList) {
             try {
               c = (Connection)connectionList.get(i);
             } catch (Exception e) {return;}
           }
           if (c.timedOut(currentTime)) {
             synchronized (connectionList) {
               if (connectionList.remove(c))
                 numConnections--;
             }
             try {
               LOG.info(getName() + ": disconnecting client " + c.getHostAddress());
               c.close();
             } catch (Exception e) {}
             numNuked++;
             end--;
             c = null;
             if (!force && numNuked == maxConnectionsToNuke) break;
           }
           else i++;
         }
         lastCleanupRunTime = System.currentTimeMillis();
       }
     }
 
     public void run() {
       LOG.info(getName() + ": starting");
       SERVER.set(Server.this);
       while (running) {
         SelectionKey key = null;
         try {
           selector.select();
           Iterator iter = selector.selectedKeys().iterator();
           
           while (iter.hasNext()) {
             key = (SelectionKey)iter.next();
             iter.remove();
            try {
              if (key.isValid()) {
                if (key.isAcceptable())
                  doAccept(key);
                else if (key.isReadable())
                  doRead(key);
              }
            } catch (IOException e) {
              key.cancel();
            }
             key = null;
           }
         } catch (OutOfMemoryError e) {
           // we can run out of memory if we have too many threads
           // log the event and sleep for a minute and give 
           // some thread(s) a chance to finish
          LOG.warn("Out of Memory in server select", e);
           closeCurrentConnection(key, e);
           cleanupConnections(true);
           try { Thread.sleep(60000); } catch (Exception ie) {}
         } catch (Exception e) {
           closeCurrentConnection(key, e);
         }
         cleanupConnections(false);
       }
       LOG.info("Stopping " + this.getName());
 
       try {
         if (acceptChannel != null)
           acceptChannel.close();
         if (selector != null)
           selector.close();
       } catch (IOException e) { }
 
       selector= null;
       acceptChannel= null;
       connectionList = null;
     }
 
     private void closeCurrentConnection(SelectionKey key, Throwable e) {
       if (key != null) {
         Connection c = (Connection)key.attachment();
         if (c != null) {
           synchronized (connectionList) {
             if (connectionList.remove(c))
               numConnections--;
           }
           try {
             LOG.info(getName() + ": disconnecting client " + c.getHostAddress());
             c.close();
           } catch (Exception ex) {}
           c = null;
         }
       }
     }
 
     void doAccept(SelectionKey key) throws IOException,  OutOfMemoryError {
       Connection c = null;
       ServerSocketChannel server = (ServerSocketChannel) key.channel();
       SocketChannel channel = server.accept();
       channel.configureBlocking(false);
       SelectionKey readKey = channel.register(selector, SelectionKey.OP_READ);
       c = new Connection(readKey, channel, System.currentTimeMillis());
       readKey.attach(c);
       synchronized (connectionList) {
         connectionList.add(numConnections, c);
         numConnections++;
       }
       LOG.info("Server connection on port " + port + " from " + 
                 c.getHostAddress() +
                 ": starting. Number of active connections: " + numConnections);
     }
 
     void doRead(SelectionKey key) {
       int count = 0;
       Connection c = (Connection)key.attachment();
       if (c == null) {
         return;  
       }
       c.setLastContact(System.currentTimeMillis());
       
       try {
         count = c.readAndProcess();
       } catch (Exception e) {
        key.cancel();
        LOG.debug(getName() + ": readAndProcess threw exception " + e + ". Count of bytes read: " + count, e);
         count = -1; //so that the (count < 0) block is executed
       }
       if (count < 0) {
         synchronized (connectionList) {
           if (connectionList.remove(c))
             numConnections--;
         }
         try {
           LOG.info(getName() + ": disconnecting client " + 
                   c.getHostAddress() + ". Number of active connections: "+
                   numConnections);
           c.close();
         } catch (Exception e) {}
         c = null;
       }
       else {
         c.setLastContact(System.currentTimeMillis());
       }
     }   
 
     void doStop()
     {
         selector.wakeup();
         Thread.yield();
     }
   }
 
   /** Reads calls from a connection and queues them for handling. */
   private class Connection {
     private SocketChannel channel;
     private SelectionKey key;
     private ByteBuffer data;
     private ByteBuffer dataLengthBuffer;
     private DataOutputStream out;
     private SocketChannelOutputStream channelOut;
     private long lastContact;
     private int dataLength;
     private Socket socket;
 
     public Connection(SelectionKey key, SocketChannel channel, 
     long lastContact) {
       this.key = key;
       this.channel = channel;
       this.lastContact = lastContact;
       this.data = null;
       this.dataLengthBuffer = null;
       this.socket = channel.socket();
       this.out = new DataOutputStream
         (new BufferedOutputStream(
          this.channelOut = new SocketChannelOutputStream(channel, 4096)));
     }   
 
     public String getHostAddress() {
       return socket.getInetAddress().getHostAddress();
     }
 
     public void setLastContact(long lastContact) {
       this.lastContact = lastContact;
     }
 
     public long getLastContact() {
       return lastContact;
     }
 
     private boolean timedOut() {
       if(System.currentTimeMillis() -  lastContact > maxIdleTime)
         return true;
       return false;
     }
 
     private boolean timedOut(long currentTime) {
         if(currentTime -  lastContact > maxIdleTime)
           return true;
         return false;
     }
 
     public int readAndProcess() throws IOException, InterruptedException {
       int count = -1;
       if (dataLengthBuffer == null)
         dataLengthBuffer = ByteBuffer.allocateDirect(4);
       if (dataLengthBuffer.remaining() > 0) {
         count = channel.read(dataLengthBuffer);
         if (count < 0) return count;
         if (dataLengthBuffer.remaining() == 0) {
           dataLengthBuffer.flip(); 
           dataLength = dataLengthBuffer.getInt();
           data = ByteBuffer.allocateDirect(dataLength);
         }
         //return count;
       }
       count = channel.read(data);
       if (data.remaining() == 0) {
         data.flip();
         processData();
         data = dataLengthBuffer = null; 
       }
       return count;
     }
 
     private void processData() throws  IOException, InterruptedException {
       byte[] bytes = new byte[dataLength];
       data.get(bytes);
       DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
       int id = dis.readInt();                    // try to read an id
         
       if (LOG.isDebugEnabled())
         LOG.debug(" got #" + id);
             
       Writable param = makeParam();           // read param
       param.readFields(dis);        
         
       Call call = new Call(id, param, this);
       synchronized (callQueue) {
         callQueue.addLast(call);              // queue the call
         callQueue.notify();                   // wake up a waiting handler
       }
         
       while (running && callQueue.size() >= maxQueuedCalls) {
         synchronized (callDequeued) {         // queue is full
           callDequeued.wait(timeout);         // wait for a dequeue
         }
       }
     }
 
     private void close() throws IOException {
       data = null;
       dataLengthBuffer = null;
       if (!channel.isOpen())
         return;
       try {socket.shutdownOutput();} catch(Exception e) {}
       try {out.close();} catch(Exception e) {}
       try {channelOut.destroy();} catch(Exception e) {}
       if (channel.isOpen()) {
         try {channel.close();} catch(Exception e) {}
       }
       try {socket.close();} catch(Exception e) {}
       try {key.cancel();} catch(Exception e) {}
       key = null;
     }
   }
 
   /** Handles queued calls . */
   private class Handler extends Thread {
     public Handler(int instanceNumber) {
       this.setDaemon(true);
       this.setName("Server handler "+ instanceNumber + " on " + port);
     }
 
     public void run() {
       LOG.info(getName() + ": starting");
       SERVER.set(Server.this);
       while (running) {
         try {
           Call call;
           synchronized (callQueue) {
             while (running && callQueue.size()==0) { // wait for a call
               callQueue.wait(timeout);
             }
             if (!running) break;
             call = (Call)callQueue.removeFirst(); // pop the queue
           }
 
           synchronized (callDequeued) {           // tell others we've dequeued
             callDequeued.notify();
           }
 
           if (LOG.isDebugEnabled())
             LOG.debug(getName() + ": has #" + call.id + " from " +
                      call.connection.socket.getInetAddress().getHostAddress());
           
           String errorClass = null;
           String error = null;
           Writable value = null;
           try {
             value = call(call.param);             // make the call
           } catch (Throwable e) {
             LOG.info(getName() + " call error: " + e, e);
             errorClass = e.getClass().getName();
             error = getStackTrace(e);
           }
             
           DataOutputStream out = call.connection.out;
           synchronized (out) {
             try {
               out.writeInt(call.id);                // write call id
               out.writeBoolean(error!=null);        // write error flag
               if (error == null) {
                 value.write(out);
               } else {
                 WritableUtils.writeString(out, errorClass);
                 WritableUtils.writeString(out, error);
               }
               out.flush();
             } catch (Exception e) {
              LOG.warn("handler output error", e);
               synchronized (connectionList) {
                 if (connectionList.remove(call.connection))
                   numConnections--;
               }
               call.connection.close();
             }
           }
 
         } catch (Exception e) {
           LOG.info(getName() + " caught: " + e, e);
         }
       }
       LOG.info(getName() + ": exiting");
     }
 
     private String getStackTrace(Throwable throwable) {
       StringWriter stringWriter = new StringWriter();
       PrintWriter printWriter = new PrintWriter(stringWriter);
       throwable.printStackTrace(printWriter);
       printWriter.flush();
       return stringWriter.toString();
     }
 
   }
   
   /** Constructs a server listening on the named port.  Parameters passed must
    * be of the named class.  The <code>handlerCount</handlerCount> determines
    * the number of handler threads that will be used to process calls.
    */
   protected Server(int port, Class paramClass, int handlerCount, Configuration conf) {
     this.conf = conf;
     this.port = port;
     this.paramClass = paramClass;
     this.handlerCount = handlerCount;
     this.maxQueuedCalls = handlerCount;
     this.timeout = conf.getInt("ipc.client.timeout",10000);
     this.maxIdleTime = conf.getInt("ipc.client.maxidletime", 120000);
     this.maxConnectionsToNuke = conf.getInt("ipc.client.kill.max", 10);
     this.thresholdIdleConnections = conf.getInt("ipc.client.idlethreshold", 4000);
   }
 
   /** Sets the timeout used for network i/o. */
   public void setTimeout(int timeout) { this.timeout = timeout; }
 
   /** Starts the service.  Must be called before any calls will be handled. */
   public synchronized void start() throws IOException {
     listener = new Listener();
     listener.start();
     
     for (int i = 0; i < handlerCount; i++) {
       Handler handler = new Handler(i);
       handler.start();
     }
   }
 
   /** Stops the service.  No new calls will be handled after this is called.  All
    * subthreads will likely be finished after this returns.
    */
   public synchronized void stop() {
     LOG.info("Stopping server on " + port);
     running = false;
     listener.doStop();
     try {
       Thread.sleep(timeout);     //  inexactly wait for pending requests to finish
     } catch (InterruptedException e) {}
     notifyAll();
   }
 
   /** Wait for the server to be stopped.
    * Does not wait for all subthreads to finish.
    *  See {@link #stop()}.
    */
   public synchronized void join() throws InterruptedException {
     while (running) {
       wait();
     }
   }
 
   /** Called for each call. */
   public abstract Writable call(Writable param) throws IOException;
 
   
   private Writable makeParam() {
     Writable param;                               // construct param
     try {
       param = (Writable)paramClass.newInstance();
       if (param instanceof Configurable) {
         ((Configurable)param).setConf(conf);
       }
     } catch (InstantiationException e) {
       throw new RuntimeException(e.toString());
     } catch (IllegalAccessException e) {
       throw new RuntimeException(e.toString());
     }
     return param;
   }
 
 }
