 /***** BEGIN LICENSE BLOCK *****
  * Version: CPL 1.0/GPL 2.0/LGPL 2.1
  *
  * The contents of this file are subject to the Common Public
  * License Version 1.0 (the "License"); you may not use this file
  * except in compliance with the License. You may obtain a copy of
  * the License at http://www.eclipse.org/legal/cpl-v10.html
  *
  * Software distributed under the License is distributed on an "AS
  * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
  * implied. See the License for the specific language governing
  * rights and limitations under the License.
  *
  * Copyright (C) 2006, 2007 Ola Bini <ola@ologix.com>
  * 
  * Alternatively, the contents of this file may be used under the terms of
  * either of the GNU General Public License Version 2 or later (the "GPL"),
  * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
  * in which case the provisions of the GPL or the LGPL are applicable instead
  * of those above. If you wish to allow use of your version of this file only
  * under the terms of either the GPL or the LGPL, and not to allow others to
  * use your version of this file under the terms of the CPL, indicate your
  * decision by deleting the provisions above and replace them with the notice
  * and other provisions required by the GPL or the LGPL. If you do not delete
  * the provisions above, a recipient may use your version of this file under
  * the terms of any one of the CPL, the GPL or the LGPL.
  ***** END LICENSE BLOCK *****/
 package org.jruby.ext.openssl;
 
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.nio.channels.SelectionKey;
 import java.nio.channels.Selector;
 import java.nio.channels.SocketChannel;
 import java.security.KeyManagementException;
 import java.security.NoSuchAlgorithmException;
 import java.security.cert.Certificate;
 import java.security.cert.CertificateEncodingException;
 import java.util.Iterator;
 
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import javax.net.ssl.SSLEngine;
 import javax.net.ssl.SSLEngineResult;
 import javax.net.ssl.SSLException;
 import javax.net.ssl.SSLHandshakeException;
 import javax.net.ssl.SSLPeerUnverifiedException;
 import javax.net.ssl.SSLSession;
 
 import org.jruby.Ruby;
 import org.jruby.RubyArray;
 import org.jruby.RubyClass;
 import org.jruby.RubyIO;
 import org.jruby.RubyModule;
 import org.jruby.RubyNumeric;
 import org.jruby.RubyObject;
 import org.jruby.RubyObjectAdapter;
 import org.jruby.RubyString;
 import org.jruby.anno.JRubyMethod;
 import org.jruby.exceptions.RaiseException;
 import org.jruby.ext.openssl.x509store.X509Utils;
 import org.jruby.javasupport.JavaEmbedUtils;
 import org.jruby.runtime.Arity;
 import org.jruby.runtime.Block;
 import org.jruby.runtime.ObjectAllocator;
 import org.jruby.runtime.ThreadContext;
 import org.jruby.runtime.builtin.IRubyObject;
 import org.jruby.util.ByteList;
 
 /**
  * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
  */
 public class SSLSocket extends RubyObject {
     private static ObjectAllocator SSLSOCKET_ALLOCATOR = new ObjectAllocator() {
         public IRubyObject allocate(Ruby runtime, RubyClass klass) {
             return new SSLSocket(runtime, klass);
         }
     };
 
     private static RubyObjectAdapter api = JavaEmbedUtils.newObjectAdapter();
     
     public static void createSSLSocket(Ruby runtime, RubyModule mSSL) {
         RubyClass cSSLSocket = mSSL.defineClassUnder("SSLSocket",runtime.getObject(),SSLSOCKET_ALLOCATOR);
 
         cSSLSocket.attr_accessor(runtime.getCurrentContext(), new IRubyObject[]{runtime.newSymbol("io")});
         cSSLSocket.attr_accessor(runtime.getCurrentContext(), new IRubyObject[]{runtime.newSymbol("context")});
         cSSLSocket.attr_accessor(runtime.getCurrentContext(), new IRubyObject[]{runtime.newSymbol("sync_close")});
         cSSLSocket.defineAlias("to_io","io");
 
         cSSLSocket.defineAnnotatedMethods(SSLSocket.class);
     }
 
     public SSLSocket(Ruby runtime, RubyClass type) {
         super(runtime,type);
         cSSLError = (RubyClass)((RubyModule)getRuntime().getModule("OpenSSL").getConstant("SSL")).getConstant("SSLError");
         verifyResult = X509Utils.V_OK;
     }
 
     private RubyClass cSSLError;
     private org.jruby.ext.openssl.SSLContext rubyCtx;
     private SSLEngine engine;
     private SocketChannel c = null;
 
     private ByteBuffer peerAppData;
     private ByteBuffer peerNetData;
     private ByteBuffer netData;
     private ByteBuffer dummy;
     
     private boolean initialHandshake = false;
 	
     private SSLEngineResult.HandshakeStatus hsStatus;
     private SSLEngineResult.Status status = null;
 
     private Selector rsel;
     private Selector wsel;
     private Selector asel;
 
     int verifyResult;
     
     @JRubyMethod(name = "initialize", rest = true, frame = true)
     public IRubyObject _initialize(IRubyObject[] args, Block unused) {
         IRubyObject io;
         if (Arity.checkArgumentCount(getRuntime(), args, 1, 2) == 1) {
             RubyClass sslContext = ((RubyModule) (getRuntime().getModule("OpenSSL").getConstant("SSL"))).getClass("SSLContext");
             rubyCtx = (org.jruby.ext.openssl.SSLContext) api.callMethod(sslContext, "new");
         } else {
             rubyCtx = (org.jruby.ext.openssl.SSLContext) args[1];
         }
         io = args[0];
         api.callMethod(this, "io=", io);
         // This is a bit of a hack: SSLSocket should share code with RubyBasicSocket, which always sets sync to true.
         // Instead we set it here for now.
         api.callMethod(io, "sync=", getRuntime().getTrue());
         c = (SocketChannel) (((RubyIO) io).getChannel());
         api.callMethod(this, "context=", rubyCtx);
         api.callMethod(this, "sync_close=", getRuntime().getFalse());
         rubyCtx.setup();
         return api.callSuper(this, args);
     }
 
     private void ossl_ssl_setup() throws NoSuchAlgorithmException, KeyManagementException, IOException {
         if(null == engine) {
             String peerHost = ((SocketChannel)c).socket().getInetAddress().getHostName();
             int peerPort = ((SocketChannel)c).socket().getPort();
             engine = rubyCtx.createSSLEngine(peerHost, peerPort);
             SSLSession session = engine.getSession();
             peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
             peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize());		
             netData = ByteBuffer.allocate(session.getPacketBufferSize());
             peerNetData.limit(0);
             peerAppData.limit(0);
             netData.limit(0);
             dummy = ByteBuffer.allocate(0);
             rsel = Selector.open();
             wsel = Selector.open();
             asel = Selector.open();
             c.register(rsel,SelectionKey.OP_READ);
             c.register(wsel,SelectionKey.OP_WRITE);
             c.register(asel,SelectionKey.OP_READ | SelectionKey.OP_WRITE);
         }
     }
 
     @JRubyMethod
     public IRubyObject connect(ThreadContext context) {
         Ruby runtime = context.getRuntime();
         if (!rubyCtx.isProtocolForClient()) {
             throw new RaiseException(runtime, cSSLError, "called a function you should not call", false);
         }
         try {
             ossl_ssl_setup();
             engine.setUseClientMode(true);
             engine.beginHandshake();
             hsStatus = engine.getHandshakeStatus();
             initialHandshake = true;
             doHandshake();
         } catch(SSLHandshakeException e) {
             // unlike server side, client should close outbound channel even if
             // we have remaining data to be sent.
             forceClose();
             Throwable v = e;
             while(v.getCause() != null && (v instanceof SSLHandshakeException)) {
                 v = v.getCause();
             }
             throw SSL.newSSLError(runtime, v);
         } catch (NoSuchAlgorithmException ex) {
             forceClose();
             throw SSL.newSSLError(runtime, ex);
         } catch (KeyManagementException ex) {
             forceClose();
             throw SSL.newSSLError(runtime, ex);
         } catch (IOException ex) {
             forceClose();
             throw SSL.newSSLError(runtime, ex);
         }
         return this;
     }
 
     @JRubyMethod
     public IRubyObject accept(ThreadContext context) {
         Ruby runtime = context.getRuntime();
         if (!rubyCtx.isProtocolForServer()) {
             throw new RaiseException(runtime, cSSLError, "called a function you should not call", false);
         }
         try {
             int vfy = 0;
             ossl_ssl_setup();
             engine.setUseClientMode(false);
             if(!rubyCtx.isNil() && !rubyCtx.callMethod(context,"verify_mode").isNil()) {
                 vfy = RubyNumeric.fix2int(rubyCtx.callMethod(context,"verify_mode"));
                 if(vfy == 0) { //VERIFY_NONE
                     engine.setNeedClientAuth(false);
                     engine.setWantClientAuth(false);
                 }
                 if((vfy & 1) != 0) { //VERIFY_PEER
                     engine.setWantClientAuth(true);
                 }
                 if((vfy & 2) != 0) { //VERIFY_FAIL_IF_NO_PEER_CERT
                     engine.setNeedClientAuth(true);
                 }
             }
             engine.beginHandshake();
             hsStatus = engine.getHandshakeStatus();
             initialHandshake = true;
             doHandshake();
         } catch(SSLHandshakeException e) {
             throw SSL.newSSLError(runtime, e);
         } catch (NoSuchAlgorithmException ex) {
             throw SSL.newSSLError(runtime, ex);
         } catch (KeyManagementException ex) {
             throw SSL.newSSLError(runtime, ex);
         } catch (IOException ex) {
             throw SSL.newSSLError(runtime, ex);
         }
         return this;
     }
 
     @JRubyMethod
     public IRubyObject verify_result() {
         if (engine == null) {
             getRuntime().getWarnings().warn("SSL session is not started yet.");
             return getRuntime().getNil();
         }
         return getRuntime().newFixnum(verifyResult);
     }
 
     private void waitSelect(Selector sel) {
         try {
             sel.select();
         } catch(Exception e) {
             return;
         }
         Iterator it = sel.selectedKeys().iterator();
         while(it.hasNext()) {
             it.next();
             it.remove();
         }
     }
 
     private void doHandshake() throws IOException {
         while (true) {
             SSLEngineResult res;
             waitSelect(asel);
             if(hsStatus == SSLEngineResult.HandshakeStatus.FINISHED) {
                 if (initialHandshake) {
                     finishInitialHandshake();
                 }
                 return;
             } else if(hsStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                 doTasks();
             } else if(hsStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                 if(readAndUnwrap() == -1 && hsStatus != SSLEngineResult.HandshakeStatus.FINISHED) {
                     throw new SSLHandshakeException("Socket closed");
                 }
                 // during initialHandshake, calling readAndUnwrap that results UNDERFLOW
                 // does not mean writable. we explicitly wait for readable channel to avoid
                 // busy loop.
                 if (initialHandshake && status == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                     waitSelect(rsel);
                 }
             } else if(hsStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                 if (netData.hasRemaining()) {
                     while(flushData()) {}
                 }
                 netData.clear();
                 res = engine.wrap(dummy, netData);
                 hsStatus = res.getHandshakeStatus();
                 netData.flip();
                 flushData();
             } else {
                 assert false : "doHandshake() should never reach the NOT_HANDSHAKING state";
                 return;
             }
         }
     }
 
     private void doTasks() {
         Runnable task;
         while ((task = engine.getDelegatedTask()) != null) {
             task.run();
         }
         hsStatus = engine.getHandshakeStatus();
         verifyResult = rubyCtx.getLastVerifyResult();
     }
 
     private boolean flushData() throws IOException {		
         try {
             writeToChannel(c, netData);
         } catch (IOException ioe) {
             netData.position(netData.limit());
             throw ioe;
         }
         if (netData.hasRemaining()) {
             return false;
         }  else {
             return true;
         }
     }
     
     private int writeToChannel(SocketChannel channel, ByteBuffer buffer) throws IOException {
       int totalWritten = 0;
       while (buffer.hasRemaining()) {
         totalWritten += channel.write(buffer);
       }
       return totalWritten;
     }
 
     private void finishInitialHandshake() {
         initialHandshake = false;
     }
 
     public int write(ByteBuffer src) throws SSLException, IOException {
         if(initialHandshake) {
             return 0;
         }
         if(netData.hasRemaining()) {
             return 0;
         }
         netData.clear();
         SSLEngineResult res = engine.wrap(src, netData);
         netData.flip();
         flushData();
         return res.bytesConsumed();
     }
 
     public int read(ByteBuffer dst) throws IOException {
         if(initialHandshake) {
             return 0;
         }
         if (engine.isInboundDone()) {
             return -1;
         }
         if (!peerAppData.hasRemaining()) {
             int appBytesProduced = readAndUnwrap(); 
             if (appBytesProduced == -1 || appBytesProduced == 0) {
                 return appBytesProduced;
             } 
         }
         int limit = Math.min(peerAppData.remaining(), dst.remaining());
         peerAppData.get(dst.array(), dst.arrayOffset(), limit);
         dst.position(dst.arrayOffset() + limit);
         return limit;
     }
 
     private int readAndUnwrap() throws IOException {
         int bytesRead = c.read(peerNetData);
         if (bytesRead == -1) {
            try {
                engine.closeInbound();
            } catch (SSLException ssle) {
                // ignore any error on close. possibly an error like this;
                // Inbound closed before receiving peer's close_notify: possible truncation attack?
            }
             if ((peerNetData.position() == 0) || (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)) {
                 return -1;
             }
         }
         peerAppData.clear();
         peerNetData.flip();
         SSLEngineResult res;
         do {
             res = engine.unwrap(peerNetData, peerAppData);
         } while (res.getStatus() == SSLEngineResult.Status.OK &&
 				res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP &&
 				res.bytesProduced() == 0);
         if(res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
             finishInitialHandshake();
         }
         if(peerAppData.position() == 0 && 
             res.getStatus() == SSLEngineResult.Status.OK &&
             peerNetData.hasRemaining()) {
             res = engine.unwrap(peerNetData, peerAppData);
         }
         status = res.getStatus();
         hsStatus = res.getHandshakeStatus();
        
         if(status == SSLEngineResult.Status.CLOSED) {
             doShutdown();
             return -1;
         }
         peerNetData.compact();
         peerAppData.flip();
         if(!initialHandshake && (hsStatus == SSLEngineResult.HandshakeStatus.NEED_TASK ||
                                  hsStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP ||
                                  hsStatus == SSLEngineResult.HandshakeStatus.FINISHED)) {
             doHandshake();
         }
         return peerAppData.remaining();
     }
 
     private void doShutdown() throws IOException {
         if (engine.isOutboundDone()) {
             return;
         }
         netData.clear();
         try {
             engine.wrap(dummy, netData);
         } catch(Exception e1) {
             return;
         }
         netData.flip();
         flushData();
 
         rsel.close();
         wsel.close();
         asel.close();
     }
 
     @JRubyMethod(rest = true, required = 1, optional = 1)
     public IRubyObject sysread(ThreadContext context, IRubyObject[] args) {
         Ruby runtime = context.getRuntime();
         int len = RubyNumeric.fix2int(args[0]);
         RubyString str = null;
         
         if (args.length == 2 && !args[1].isNil()) {
             str = args[1].convertToString();
         } else {
             str = getRuntime().newString("");
         }
         if(len == 0) {
             str.clear();
             return str;
         }
         if (len < 0) {
             throw runtime.newArgumentError("negative string size (or size too big)");
         }
 
         // So we need to make sure to only block when there is no data left to process
         if(engine == null || !(peerAppData.hasRemaining() || peerNetData.position() > 0)) {
             waitSelect(rsel);
         }
 
         try {
             ByteBuffer dst = ByteBuffer.allocate(len);
             int rr = -1;
             // ensure >0 bytes read; sysread is blocking read.
             while (rr <= 0) {
                 if (engine == null) {
                     rr = c.read(dst);
                 } else {
                     rr = read(dst);
                 }
                 if (rr == -1) {
                     throw getRuntime().newEOFError();
                 }
             }
             byte[] bss = new byte[rr];
             dst.position(dst.position() - rr);
             dst.get(bss);
             str.setValue(new ByteList(bss));
             return str;
         } catch (IOException ioe) {
             throw getRuntime().newIOError(ioe.getMessage());
         }
     }
 
     @JRubyMethod
     public IRubyObject syswrite(ThreadContext context, IRubyObject arg)  {
         Ruby runtime = context.getRuntime();
         waitSelect(wsel);
         byte[] bls = arg.convertToString().getBytes();
         ByteBuffer b1 = ByteBuffer.wrap(bls);
         try {
             int written;
             if(engine == null) {
                 written = writeToChannel(c, b1);
             } else {
                 written = write(b1);
             }
             ((RubyIO)api.callMethod(this,"io")).flush();
 
             return getRuntime().newFixnum(written);
         } catch (IOException ioe) {
             throw runtime.newIOError(ioe.getMessage());
         }
     }
 
     // do shutdown even if we have remaining data to be sent.
     // call this when you get an exception from client side.
     private void forceClose() {
         close(true);
     }
 
     private void close(boolean force)  {
         if (engine == null) throw getRuntime().newEOFError();
         engine.closeOutbound();
         if (!force && netData.hasRemaining()) {
             return;
         } else {
             try {
                 doShutdown();
             } catch (IOException ex) {
                 // ignore?
             }
         }
     }
 
     @JRubyMethod
     public IRubyObject sysclose()  {
         close(false);
         ThreadContext tc = getRuntime().getCurrentContext();
         if(callMethod(tc,"sync_close").isTrue()) {
             callMethod(tc,"io").callMethod(tc,"close");
         }
         return getRuntime().getNil();
     }
 
     @JRubyMethod
     public IRubyObject cert() {
         try {
             Certificate[] cert = engine.getSession().getLocalCertificates();
             if (cert.length > 0) {
                 return X509Cert.wrap(getRuntime(), cert[0]);
             }
         } catch (CertificateEncodingException ex) {
             throw X509Cert.newCertificateError(getRuntime(), ex);
         }
         return getRuntime().getNil();
     }
 
     @JRubyMethod
     public IRubyObject peer_cert()  {
         try {
             Certificate[] cert = engine.getSession().getPeerCertificates();
             if (cert.length > 0) {
                 return X509Cert.wrap(getRuntime(), cert[0]);
             }
         } catch (CertificateEncodingException ex) {
             throw X509Cert.newCertificateError(getRuntime(), ex);
         } catch (SSLPeerUnverifiedException ex) {
             Logger.getLogger(SSLSocket.class.getName()).log(Level.SEVERE, null, ex);
         }
         return getRuntime().getNil();
     }
 
     @JRubyMethod
     public IRubyObject peer_cert_chain() {
         try {
             javax.security.cert.Certificate[] certs = engine.getSession().getPeerCertificateChain();
 
             RubyArray arr = getRuntime().newArray(certs.length);
             for(int i = 0 ; i < certs.length; i++ ) {
                 arr.add(X509Cert.wrap(getRuntime(), certs[i]));
             }
             return arr;
         } catch (javax.security.cert.CertificateEncodingException e) {
             throw X509Cert.newCertificateError(getRuntime(), e);
         } catch (SSLPeerUnverifiedException ex) {
             Logger.getLogger(SSLSocket.class.getName()).log(Level.SEVERE, null, ex);
         }
         return getRuntime().getNil();
     }
 
     @JRubyMethod
     public IRubyObject cipher() {
         return getRuntime().newString(engine.getSession().getCipherSuite());
     }
 
     @JRubyMethod
     public IRubyObject state() {
         System.err.println("WARNING: unimplemented method called: SSLSocket#state");
         return getRuntime().getNil();
     }
 
     @JRubyMethod
     public IRubyObject pending() {
         System.err.println("WARNING: unimplemented method called: SSLSocket#pending");
         return getRuntime().getNil();
     }
 }// SSLSocket
