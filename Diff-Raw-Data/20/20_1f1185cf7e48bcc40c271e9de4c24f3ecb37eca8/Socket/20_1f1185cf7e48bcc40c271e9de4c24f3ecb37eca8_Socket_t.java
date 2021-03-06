 /*
  * Socket.java
  *
  * Created on 4 Февраль 2009 г., 15:11
  *
  * To change this template, choose Tools | Template Manager
  * and open the template in the editor.
  */
 
 package protocol.xmpp;
 
 import com.jcraft.jzlib.*;
 import jimm.JimmException;
 import jimm.modules.*;
 import protocol.net.TcpSocket;
 
 import java.util.Vector;
 
 /**
  *
  * @author Vladimir Krukov
  */
 final class Socket implements Runnable {
     private TcpSocket socket = new TcpSocket();
    private volatile boolean connected;
     private byte[] inputBuffer = new byte[1024];
     private int inputBufferLength = 0;
     public int inputBufferIndex = 0;
     // #sijapp cond.if modules_ZLIB is "true" #
     private ZInputStream zIn;
     private ZOutputStream zOut;
     private boolean compressed;
     private boolean secured;
     // #sijapp cond.end #
     private final Vector<Object> read = new Vector<Object>();
    private final Object readLock = new Object();

     /**
      * Creates a new instance of Socket
      */
     public Socket() {
     }
     // #sijapp cond.if modules_ZLIB is "true" #
     public void startCompression() {
         zIn = new ZInputStream(socket);
         zOut = new ZOutputStream(socket, JZlib.Z_DEFAULT_COMPRESSION);
         zOut.setFlushMode(JZlib.Z_SYNC_FLUSH);
         compressed = true;
         // #sijapp cond.if modules_DEBUGLOG is "true" #
         DebugLog.println("zlib is working");
         // #sijapp cond.end #
     }
 
     public void startTls(String host) {
         socket.startTls(host);
         secured = true;
     }
     // #sijapp cond.end #
 
     public boolean isConnected() {
         return connected;
     }
     
     public void connectTo(String url) throws JimmException {
         System.out.println("url: " + url);
         socket.connectTo(url);
         connected = true;
     }
 
     private int read(byte[] data) throws JimmException {
         // #sijapp cond.if modules_ZLIB is "true" #
         if (compressed) {
             int bRead = zIn.read(data);
             if (-1 == bRead) {
                 throw new JimmException(120, 13);
             }
             return bRead;
         }
         // #sijapp cond.end #
         int bRead = socket.read(data, 0, data.length);
         if (-1 == bRead) {
             throw new JimmException(120, 12);
         }
         return bRead;
     }
 
     public void write(byte[] data) throws JimmException {
         // #sijapp cond.if modules_ZLIB is "true" #
         if (compressed) {
             zOut.write(data);
             zOut.flush();
             return;
         }
         // #sijapp cond.end #
         socket.write(data, 0, data.length);
         socket.flush();
     }
     public void close() {
         connected = false;
         // #sijapp cond.if modules_ZLIB is "true" #
         try {
             zIn.close();
             zOut.close();
         } catch (Exception ignored) {
         }
         // #sijapp cond.end #
         socket.close();
         inputBufferLength = 0;
         inputBufferIndex = 0;
     }
 
     public void sleep(long ms) {
         try {
             Thread.sleep(ms);
         } catch (Exception ignored) {
         }
     }
     private void fillBuffer() throws JimmException {
         inputBufferIndex = 0;
         inputBufferLength = read(inputBuffer);
         while (0 == inputBufferLength) {
             sleep(100);
             inputBufferLength = read(inputBuffer);
         }
     }
 
     private byte readByte() throws JimmException {
         if (inputBufferIndex >= inputBufferLength) {
             fillBuffer();
         }
         return inputBuffer[inputBufferIndex++];
     }
 
     char readChar() throws JimmException {
         try {
             byte bt = readByte();
             if (0 <= bt) {
                 return (char)bt;
             }
             if ((bt & 0xE0) == 0xC0) {
                 byte bt2 = readByte();
                 return (char)(((bt & 0x3F) << 6) | (bt2 & 0x3F));
 
             } else if ((bt & 0xF0) == 0xE0) {
                 byte bt2 = readByte();
                 byte bt3 = readByte();
                 return (char)(((bt & 0x1F) << 12) | ((bt2 & 0x3F) << 6) | (bt3 & 0x3F));
 
             } else {
                 int seqLen = 0;
                 if ((bt & 0xF8) == 0xF0) seqLen = 3;
                 else if ((bt & 0xFC) == 0xF8) seqLen = 4;
                 else if ((bt & 0xFE) == 0xFC) seqLen = 5;
                 for (; 0 < seqLen; --seqLen) {
                     readByte();
                 }
                 return '?';
             }
         } catch (JimmException e) {
             // #sijapp cond.if modules_DEBUGLOG is "true" #
             DebugLog.panic("readChar je ", e);
             // #sijapp cond.end #
             throw e;
         } catch (Exception e) {
             // #sijapp cond.if modules_DEBUGLOG is "true" #
             DebugLog.panic("readChar e ", e);
             // #sijapp cond.end #
             throw new JimmException(120, 7);
         }
     }
 
     boolean isSecured() {
         return secured;
     }
 
     @Override
     public void run() {
         while (connected) {
            synchronized (readLock) {
                try {
                    readLock.wait();
                } catch (InterruptedException ignored) {
                }
            }
             Object readObject;
             try {
                 readObject = XmlNode.parse(this);
                 if (null == readObject) continue;
             } catch (JimmException e) {
                 readObject = e;
             }
             synchronized (read) {
                 read.addElement(readObject);
                read.notifyAll();
             }
         }
         synchronized (read) {
             read.notifyAll();
         }
     }
     public XmlNode readNode(boolean wait) throws JimmException {
         Object readObject = null;
        synchronized (readLock) {
            readLock.notify();
        }
         synchronized (read) {
             do {
                 if (0 < read.size()) {
                     readObject = read.elementAt(0);
                     read.removeElementAt(0);
                     wait = false;
                 } else if (wait) {
                     try {
                         read.wait();
                     } catch (InterruptedException ignored) {
                     }
                 }
             } while (wait);
         }
         if (readObject instanceof JimmException) throw (JimmException) readObject;
         return (XmlNode) readObject;
     }
 
     public void start() {
         new Thread(this).start();
     }
 }
