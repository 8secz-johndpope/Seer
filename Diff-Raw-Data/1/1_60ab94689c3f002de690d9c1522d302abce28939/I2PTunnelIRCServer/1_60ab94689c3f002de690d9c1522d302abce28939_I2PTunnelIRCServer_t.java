 package net.i2p.i2ptunnel;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.net.InetAddress;
 import java.net.Socket;
 import java.net.SocketException;
 import java.util.Properties;
 
 import net.i2p.I2PAppContext;
 import net.i2p.client.streaming.I2PSocket;
 import net.i2p.crypto.SHA256Generator;
 import net.i2p.data.DataFormatException;
 import net.i2p.data.DataHelper;
 import net.i2p.data.Destination;
 import net.i2p.data.Hash;
 import net.i2p.data.Base32;
 import net.i2p.util.EventDispatcher;
 import net.i2p.util.I2PThread;
 import net.i2p.util.Log;
 
 /**
  * Simple extension to the I2PTunnelServer that filters the registration
  * sequence to pass the destination hash of the client through as the hostname,
  * so an IRC Server may track users across nick changes.
  *
  * Of course, this requires the ircd actually use the hostname sent by
  * the client rather than the IP. It is common for ircds to ignore the
  * hostname in the USER message (unless it's coming from another server)
  * since it is easily spoofed. So you have to fix or, if you are lucky,
  * configure your ircd first. At least in unrealircd and ngircd this is
  * not configurable.
  *
  * There are three options for mangling the desthash. Put the option in the
  * "custom options" section of i2ptunnel.
  *   - ircserver.cloakKey unset:          Cloak with a random value that is persistent for
  *                                        the life of this tunnel. This is the default.
  *   - ircserver.cloakKey=somepassphrase: Cloak with the hash of the passphrase. Use this to
  *                                        have consistent mangling across restarts, or to
  *                                        have multiple IRC servers cloak consistently to
  *                                        be able to track users even when they switch servers.
  *                                        Note: don't quote or put spaces in the passphrase,
  *                                        the i2ptunnel gui can't handle it.
  *   - ircserver.fakeHostname=%f.b32.i2p: Set the fake hostname sent by I2PTunnel,
  *                                        %f is the full B32 destination hash
  *                                        %c is the cloaked hash.
  *
  * There is no outbound filtering.
  *
  * @author zzz
  */
 public class I2PTunnelIRCServer extends I2PTunnelServer implements Runnable {
     public static final String PROP_CLOAK="ircserver.cloakKey";
     public static final String PROP_HOSTNAME="ircserver.fakeHostname";
     public static final String PROP_HOSTNAME_DEFAULT="%f.b32.i2p";
     
     private static final Log _log = new Log(I2PTunnelIRCServer.class);
     
     
     /**
      * @throws IllegalArgumentException if the I2PTunnel does not contain
      *                                  valid config to contact the router
      */
 
     public I2PTunnelIRCServer(InetAddress host, int port, File privkey, String privkeyname, Logging l, EventDispatcher notifyThis, I2PTunnel tunnel) {
         super(host, port, privkey, privkeyname, l, notifyThis, tunnel);
         initCloak(tunnel);
     }
 
     /** generate a random 32 bytes, or the hash of the passphrase */
     private void initCloak(I2PTunnel tunnel) {
         Properties opts = tunnel.getClientOptions();
         String passphrase = opts.getProperty(PROP_CLOAK);
         if (passphrase == null) {
             this.cloakKey = new byte[Hash.HASH_LENGTH];
             tunnel.getContext().random().nextBytes(this.cloakKey);
         } else {
             this.cloakKey = SHA256Generator.getInstance().calculateHash(passphrase.trim().getBytes()).getData();
         }
         
         this.hostname = opts.getProperty(PROP_HOSTNAME, PROP_HOSTNAME_DEFAULT);
     }
     
     protected void blockingHandle(I2PSocket socket) {
         try {
             // give them 15 seconds to send in the request
             socket.setReadTimeout(15*1000);
             InputStream in = socket.getInputStream();
             String modifiedRegistration = filterRegistration(in, cloakDest(socket.getPeerDestination()));
             socket.setReadTimeout(readTimeout);
             Socket s = new Socket(remoteHost, remotePort);
             new I2PTunnelRunner(s, socket, slock, null, modifiedRegistration.getBytes(), null);
         } catch (SocketException ex) {
             try {
                 socket.close();
             } catch (IOException ioe) {
                 if (_log.shouldLog(Log.ERROR))
                     _log.error("Error while closing the received i2p con", ex);
             }
         } catch (IOException ex) {
             try {
                 socket.close();
             } catch (IOException ioe) {}
             if (_log.shouldLog(Log.WARN))
                 _log.warn("Error while receiving the new IRC Connection", ex);
         } catch (OutOfMemoryError oom) {
             try {
                 socket.close();
             } catch (IOException ioe) {}
             if (_log.shouldLog(Log.ERROR))
                 _log.error("OOM in IRC server", oom);
         }
     }
 
     /**
      * (Optionally) append 32 bytes of crap to the destination then return
      * the first few characters of the hash of the whole thing, + ".i2p".
      * Or do we want the full hash if the ircd is going to use this for
      * nickserv auto-login? Or even Base32 if it will be used in a
      * case-insensitive manner?
      *
      */
     String cloakDest(Destination d) {
         String hf;
         String hc;
         
         byte[] b = new byte[d.size() + this.cloakKey.length];
         System.arraycopy(b, 0, d.toByteArray(), 0, d.size());
         System.arraycopy(b, d.size(), this.cloakKey, 0, this.cloakKey.length);
         hc = Base32.encode(SHA256Generator.getInstance().calculateHash(b).getData());
         
         hf = Base32.encode(d.calculateHash().getData());
         
         return this.hostname.replace("%f", hf).replace("%c", hc);
     }
 
     /** keep reading until we see USER or SERVER */
     private String filterRegistration(InputStream in, String newHostname) throws IOException {
         StringBuffer buf = new StringBuffer(128);
         int lineCount = 0;
         
         while (true) {
             String s = DataHelper.readLine(in);
             if (s == null)
                 throw new IOException("EOF reached before the end of the headers [" + buf.toString() + "]");
             if (++lineCount > 10)
                 throw new IOException("Too many lines before USER or SERVER, giving up");
             s = s.trim();
             if (_log.shouldLog(Log.DEBUG))
                 _log.debug("Got line: " + s);
 
             String field[]=s.split(" ",5);
             String command;
             int idx=0;
         
             if(field[0].charAt(0)==':')
                 idx++;
 
             try {
                 command = field[idx++];
             } catch (IndexOutOfBoundsException ioobe) {
                 // wtf, server sent borked command?
                throw new IOException("Dropping defective message: index out of bounds while extracting command.");
             }
 
             if ("USER".equalsIgnoreCase(command)) {
                 if (field.length < idx + 4)
                     throw new IOException("Too few parameters in USER message: " + s);
                 // USER zzz1 hostname localhost :zzz
                 //  =>
                 // USER zzz1 abcd1234.i2p localhost :zzz
                 // this whole class is for these two lines...
                 buf.append("USER ").append(field[idx]).append(' ').append(newHostname);
                buf.append(' ');
                 buf.append(field[idx+2]).append(' ').append(field[idx+3]).append("\r\n");
                 break;
             }
             buf.append(s).append("\r\n");
             if ("SERVER".equalsIgnoreCase(command))
                 break;
         }
         if (_log.shouldLog(Log.DEBUG))
             _log.debug("All done, sending: " + buf.toString());
         return buf.toString();
     }
     
     private byte[] cloakKey; // 32 bytes of stuff to scramble the dest with
     private String hostname;
 }
