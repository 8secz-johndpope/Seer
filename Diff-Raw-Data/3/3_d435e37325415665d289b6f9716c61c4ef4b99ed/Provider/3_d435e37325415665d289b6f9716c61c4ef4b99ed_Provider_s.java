 /*
  * JBoss, Home of Professional Open Source
  * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
  * by the @authors tag. All rights reserved.
  * See the copyright.txt in the distribution for a full listing
  * of individual contributors.
  * This copyrighted material is made available to anyone wishing to use,
  * modify, copy, or redistribute it subject to the terms and conditions
  * of the GNU General Public License, v. 2.0.
  * This program is distributed in the hope that it will be useful, but WITHOUT A
  * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  * PARTICULAR PURPOSE. See the GNU General Public License for more details.
  * You should have received a copy of the GNU General Public License,
  * v. 2.0 along with this distribution; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
  * MA 02110-1301, USA.
  */
 package org.mobicents.protocols.ss7.mtp.provider.m3ua;
 
 import java.io.IOException;
 import java.net.SocketAddress;
 import java.nio.channels.SelectionKey;
 import java.util.Collection;
 import java.util.Properties;
 
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.locks.Condition;
 import java.util.concurrent.locks.ReentrantLock;
 import org.apache.log4j.Logger;
 import org.mobicents.protocols.ConfigurationException;
 import org.mobicents.protocols.StartFailedException;
 import org.mobicents.protocols.ss7.m3ua.M3UAChannel;
 import org.mobicents.protocols.ss7.m3ua.M3UAProvider;
 import org.mobicents.protocols.ss7.m3ua.M3UASelectionKey;
 import org.mobicents.protocols.ss7.m3ua.M3UASelector;
 import org.mobicents.protocols.ss7.m3ua.impl.tcp.TcpProvider;
 import org.mobicents.protocols.ss7.m3ua.message.M3UAMessage;
 import org.mobicents.protocols.ss7.m3ua.message.MessageClass;
 import org.mobicents.protocols.ss7.m3ua.message.MessageType;
 import org.mobicents.protocols.ss7.m3ua.message.TransferMessage;
 import org.mobicents.protocols.ss7.m3ua.message.parm.ProtocolData;
 import org.mobicents.protocols.ss7.mtp.provider.MtpListener;
 import org.mobicents.protocols.ss7.mtp.provider.MtpProvider;
 
 /**
  * base implementation over stream interface
  * 
  * @author baranowb
  * 
  */
 public class Provider implements MtpProvider, Runnable {
     protected SocketAddress localAddress = null;
     protected SocketAddress remoteAddress = null;
     
     private M3UAProvider provider;
     private M3UAChannel channel;
     private M3UASelector selector;
     
     private boolean started = false;
 
     private MtpListener listener;
     private Logger logger = Logger.getLogger(Provider.class);
     
     //outgoing message
     private TransferMessage tm;
     private volatile boolean txFailed = false;
     private volatile boolean sendReady = false;
     
     private ReentrantLock lock = new ReentrantLock();
     private Condition sendCompleted = lock.newCondition();
     
     private int opc;
     private int apc;
     
     /**
      * 
      */
     public Provider() {
         provider = TcpProvider.open();
     }
 
     public void setLocalAddress(SocketAddress localAddress) {
         this.localAddress = localAddress;
     }
 
     public void setRemoteAddress(SocketAddress remoteAddress) {
         this.remoteAddress = remoteAddress;
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see
      * org.mobicents.protocols.ss7.mtp.provider.MtpProvider#configure(java.util
      * .Properties)
      */
     public void configure(Properties p) throws ConfigurationException {
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see org.mobicents.protocols.ss7.mtp.provider.MtpProvider#send(byte[])
      */
     public void send(byte[] msu) throws IOException {
         lock.lock();
         try {
             TransferMessage msg = (TransferMessage) provider.getMessageFactory().createMessage(
                     MessageClass.TRANSFER_MESSAGES, 
                     MessageType.PAYLOAD);
             ProtocolData data = provider.getParameterFactory().createProtocolData(0, msu);
             msg.setData(data);
             try {
                 sendReady = true;
                 txFailed = false;
                 sendCompleted.await(5, TimeUnit.SECONDS);
             } catch (InterruptedException e) {
                 throw new IOException("Interrupted");
             }
             
             //timed out?
             if (sendReady) {                
                 throw new IOException("Wait timeout");
             }
             
             if (this.txFailed) {
                 throw new IOException("Can not send message");
             }
         } finally {
             lock.unlock();
         }
     }
 
     /*
      * (non-Javadoc)
      * 
      * @see org.mobicents.protocols.ss7.mtp.provider.MtpProvider#start()
      */
     public void start() throws StartFailedException {
         logger.info("Starting M3UA provider");
         try {
             logger.info("Starting M3UA connector");
             new Thread(new Connector(this), "M3UAConnector").start();
         } catch (Exception e) {
             throw new StartFailedException(e);
         } 
     }
 
     public void setOriginalPointCode(int opc) {
         this.opc = opc;
     }
 
     public void setAdjacentPointCode(int apc) {
         this.apc = apc;
     }
 
     public int getAdjacentPointCode() {
         return apc;
     }
 
     public int getOriginalPointCode() {
         return opc;
     }
 
     public void setMtpListener(MtpListener listener) {
         this.listener = listener;
     }
 
     public void stop() {
         started = false;
         try {
             channel.close();
         } catch (IOException e) {
         }
     }
 
     private void receive(M3UAChannel channel) {
         //reading message from M3UA channel
         M3UAMessage msg = null;
         try {
             msg = channel.receive();
             logger.info("Receive " + msg);
         } catch (IOException e) {
             logger.error("Unable to read message, caused by ", e);
             //TODO disconnect channel?
             return;
         }
         
         //determine type of the message
         switch (msg.getMessageClass()) {
             case MessageClass.TRANSFER_MESSAGES :
                 //deliver transfer message to the upper layer
                 TransferMessage message = (TransferMessage) msg;
                 if (listener != null) {
                     listener.receive(message.getData().getMsu());
                 }
                 break;
             default :
                 logger.info("Unable to handle message :" + msg);
         }
     }
 
     private void send(M3UAChannel channel) {
         if (!sendReady) {
             return;
         }
         
         try {
             channel.send(tm);
             sendReady = false;
             
             lock.lock();
             try {
                 this.sendCompleted.signal();
             } finally {
                 lock.unlock();
             }
         } catch (IOException e) {
             this.txFailed = true;
         }
     }
 
     public void run() {
         while (started) {
             try {
                 //selecting channels (only one now)
                 Collection<M3UASelectionKey> selection = selector.selectNow();
                 for (M3UASelectionKey key : selection) {
                     //obtain channel ready for IO
                     M3UAChannel chann = (M3UAChannel) key.channel();
                     
                     //do receiving part
                     if (key.isReadable()) {
                         receive(chann);
                     }
                     
                     //do sending part
                     if (key.isWritable()) {
                         send(chann);
                     }
                 }
             } catch (IOException e) {
                 //TODO terminate and reconnect?
             }
         }
     }
 
     public boolean isLinkUp() {
         return true;
     }
     
     /**
      * Starts connection creation in the background.
      */
     private class Connector implements Runnable {
         private Runnable worker;
         
         public Connector(Runnable worker) {
             this.worker = worker;
         }
         
         public void run() {
             boolean connected = false;
             while (!connected) {
                 try {
                     selector = provider.openSelector();            
                     //opening channel, bind it local address and connect 
                     //to the remote address
                     channel = provider.openChannel();
                     channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
             
                     logger.info("Binding M3UA channel to " + localAddress);
                     channel.bind(localAddress);
             
                     logger.info("Connecting M3UA channel to " + remoteAddress);
                     channel.connect(remoteAddress);
             
                     //wait while connection will be established
                     while (!channel.finishConnect()) {
                         synchronized (this) {
                             wait(10);
                         }
                     }
             
                     //run main thread;
                     logger.info("Connected M3UA channel to " + remoteAddress);
                     started = true;
                     new Thread(worker).start();
                     connected = true;
                 } catch (Exception e) {
                     logger.warn("Can not connect to " + remoteAddress + ":" + e.getMessage());
                     try {
                         channel.close();
                     } catch (IOException ie) {
                     }
                     //wait 5 second before reconnect
                     try {
                         synchronized(this) {wait(5000);}
                     } catch (InterruptedException ex) {
                         break;
                     }
                 }
             }
         }
     }
 }
