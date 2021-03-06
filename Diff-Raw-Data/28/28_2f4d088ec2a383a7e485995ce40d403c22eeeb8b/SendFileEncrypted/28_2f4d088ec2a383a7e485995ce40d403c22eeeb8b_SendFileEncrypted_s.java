 /**
  * Neociclo Accord, Open Source B2B Integration Suite
  * Copyright (C) 2005-2010 Neociclo, http://www.neociclo.com
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  *
  * $Id$
  */
 package org.neociclo.odetteftp.examples.client.oftp2;
 
import static org.neociclo.odetteftp.TransferMode.*;
import static org.neociclo.odetteftp.util.OdetteFtpSupport.*;
import static org.neociclo.odetteftp.util.OftpUtil.*;
 
 import java.io.File;
 import java.net.InetSocketAddress;
 import java.security.cert.X509Certificate;
 import java.util.Queue;
 import java.util.concurrent.ConcurrentLinkedQueue;
 
 import javax.net.ssl.SSLEngine;
 
 import org.neociclo.odetteftp.OdetteFtpVersion;
 import org.neociclo.odetteftp.examples.MainSupport;
 import org.neociclo.odetteftp.examples.support.SampleOftpSslContextFactory;
 import org.neociclo.odetteftp.oftplet.OftpletFactory;
 import org.neociclo.odetteftp.protocol.OdetteFtpObject;
 import org.neociclo.odetteftp.protocol.v20.CipherSuite;
 import org.neociclo.odetteftp.protocol.v20.DefaultEnvelopedVirtualFile;
import org.neociclo.odetteftp.protocol.v20.FileCompression;
 import org.neociclo.odetteftp.protocol.v20.FileEnveloping;
 import org.neociclo.odetteftp.protocol.v20.SecurityLevel;
 import org.neociclo.odetteftp.service.TcpClient;
 import org.neociclo.odetteftp.support.InOutSharedQueueOftpletFactory;
 import org.neociclo.odetteftp.support.SessionConfig;
 import org.neociclo.odetteftp.util.SecurityUtil;
 
 /**
  * @author Rafael Marins
  * @version $Rev$ $Date$
  */
 public class SendFileEncrypted {
 
 	private static final String PARTNER_CERTIFICATE_FILE = "src/main/resources/certificates/o0055partnera-public.cer";
 
 	public static void main(String[] args) throws Exception {
 
 		MainSupport ms = new MainSupport(SendFileEncrypted.class, args, "server", "port", "oid", "password",
 				"payload");
 
 		String host = ms.get(0);
 		int port = Integer.parseInt(ms.get(1));
 		String usercode = ms.get(2);
 		String password = ms.get(3);
 		File payloadFile = new File(ms.get(4));
 
 		File encryptedFile = File.createTempFile("encrypted-", "-" + payloadFile.getName(),
 				payloadFile.getParentFile());
 
 		SessionConfig conf = new SessionConfig();
 		conf.setUserCode(usercode);
 		conf.setUserPassword(password);
 
 		conf.setTransferMode(SENDER_ONLY);
 		// require an OFTP2 connection
 		conf.setVersion(OdetteFtpVersion.OFTP_V20);
 
 		Queue<OdetteFtpObject> filesToSend = new ConcurrentLinkedQueue<OdetteFtpObject>();
 
 		// construct enveloped virtual file object
 		DefaultEnvelopedVirtualFile vf = new DefaultEnvelopedVirtualFile();
 		vf.setFile(encryptedFile);
 
 		// encrypting ONLY virtual file options
 		vf.setEnvelopingFormat(FileEnveloping.CMS);
 		vf.setSecurityLevel(SecurityLevel.ENCRYPTED);
 		vf.setCipherSuite(CipherSuite.TRIPLEDES_RSA_SHA1);
 
 		// load the partner's certificate used to encrypt the payload
 		X509Certificate partnerCert = SecurityUtil.openCertificate(new File(PARTNER_CERTIFICATE_FILE));
 
 		// create the compressed file
 		createEnvelopedFile(payloadFile, encryptedFile, vf, null, null, partnerCert);
 
 		// set file size after compression
 		vf.setOriginalFileSize(getFileSize(payloadFile));
 		vf.setSize(getFileSize(encryptedFile));
 
 		filesToSend.offer(vf);
 
 		OftpletFactory factory = new InOutSharedQueueOftpletFactory(conf, filesToSend, null, null);
 
 		// create the client mode SSL engine
 		SSLEngine sslEngine = SampleOftpSslContextFactory.getClientContext().createSSLEngine();
 		sslEngine.setUseClientMode(true);
 		sslEngine.setEnableSessionCreation(true);
 
 		TcpClient oftp = new TcpClient(new InetSocketAddress(host, port), sslEngine, factory);
 
 		oftp.connect(true);
 
 		encryptedFile.delete();
 	}
 
 }
