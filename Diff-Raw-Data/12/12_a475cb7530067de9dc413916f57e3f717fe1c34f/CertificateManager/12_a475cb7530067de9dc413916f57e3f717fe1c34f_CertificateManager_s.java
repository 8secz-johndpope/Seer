 /*
  * Copyright (c) 2006-2009 Chris Smith, Shane Mc Cormack, Gregory Holmes
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
 
 package com.dmdirc;
 
 import com.dmdirc.config.ConfigManager;
 import com.dmdirc.ui.core.dialogs.sslcertificate.CertificateAction;
 import com.dmdirc.ui.core.dialogs.sslcertificate.SSLCertificateDialogModel;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.security.InvalidAlgorithmParameterException;
 import java.security.KeyStore;
 import java.security.KeyStoreException;
 import java.security.NoSuchAlgorithmException;
 import java.security.UnrecoverableKeyException;
 import java.security.cert.CertificateException;
 import java.security.cert.PKIXParameters;
 import java.security.cert.TrustAnchor;
 import java.security.cert.X509Certificate;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.Semaphore;
 
 import javax.naming.InvalidNameException;
 import javax.naming.ldap.LdapName;
 import javax.naming.ldap.Rdn;
 import javax.net.ssl.KeyManager;
 import javax.net.ssl.KeyManagerFactory;
 import javax.net.ssl.X509TrustManager;
 
 /**
  * Manages storage and validation of certificates used when connecting to
  * SSL servers.
  *
  * @since 0.6.3
  * @author chris
  */
 public class CertificateManager implements X509TrustManager {
 
     /** The password for the global java cacert file. */
     private final String cacertpass;
 
     /** The server name the user is trying to connect to. */
     private final String serverName;
 
     /** The configuration manager to use for settings. */
     private final ConfigManager config;
 
     /** The set of CAs from the global cacert file. */
     private Set<X509Certificate> globalTrustedCAs = new HashSet<X509Certificate>();
 
     /** Whether or not to check specified parts of the certificate. */
     private boolean checkDate, checkIssuer, checkHost;
 
     /** Used to synchronise the manager with the certificate dialog. */
     private final Semaphore actionSem = new Semaphore(0);
     
     /** The action to perform. */
     private CertificateAction action;
 
     /**
      * Creates a new certificate manager for a client connecting to the
      * specified server.
      *
      * @param serverName The name the user used to connect to the server
      * @param config The configuration manager to use
      */
     public CertificateManager(final String serverName, final ConfigManager config) {
         this.serverName = serverName;
         this.config = config;
         this.cacertpass = config.getOption("ssl", "cacertpass", "changeit");
         this.checkDate = config.getOptionBool("ssl", "checkdate", true);
         this.checkIssuer = config.getOptionBool("ssl", "checkissuer", true);
         this.checkHost = config.getOptionBool("ssl", "checkhost", true);
 
         loadTrustedCAs();
     }
 
     /**
      * Loads the trusted CA certificates from the Java cacerts store.
      */
     protected void loadTrustedCAs() {
         try {
            String filename = System.getProperty("java.home")
                 + "/lib/security/cacerts".replace('/', File.separatorChar);
            FileInputStream is = new FileInputStream(filename);
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
             keystore.load(is, cacertpass.toCharArray());

             // This class retrieves the most-trusted CAs from the keystore
            PKIXParameters params = new PKIXParameters(keystore);
             for (TrustAnchor anchor : params.getTrustAnchors()) {
                 globalTrustedCAs.add(anchor.getTrustedCert());
             }
         } catch (CertificateException ex) {
             // TODO: Handle these exceptions
         } catch (IOException ex) {
 
         } catch (InvalidAlgorithmParameterException ex) {
 
         } catch (KeyStoreException ex) {
 
         } catch (NoSuchAlgorithmException ex) {
 
         }
     }
 
     /**
      * Retrieves a KeyManager[] for the client certicate specified in the
      * configuration, if there is one.
      *
      * @return A KeyManager to use for the SSL connection
      */
     public KeyManager[] getKeyManager() {
         if (config.hasOption("ssl", "clientcert.file")) {
             FileInputStream fis = null;
             try {
                 final char[] pass;
 
                 if (config.hasOption("ssl", "clientcert.pass")) {
                     pass = config.getOption("ssl", "clientcert.pass").toCharArray();
                 } else {
                     pass = null;
                 }
 
                 fis = new FileInputStream(config.getOption("ssl", "clientcert.file"));
                 final KeyStore ks = KeyStore.getInstance("pkcs12");
                 ks.load(fis, pass);
 
                 final KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                         KeyManagerFactory.getDefaultAlgorithm());
                 kmf.init(ks, pass);
 
                 return kmf.getKeyManagers();
             } catch (KeyStoreException ex) {
                 ex.printStackTrace(); // TODO: Handle these exceptions
             } catch (IOException ex) {
                 ex.printStackTrace();
             } catch (CertificateException ex) {
                 ex.printStackTrace();
             } catch (NoSuchAlgorithmException ex) {
                 ex.printStackTrace();
             } catch (UnrecoverableKeyException ex) {
                 ex.printStackTrace();
             } finally {
                 if (fis != null) {
                     try {
                         fis.close();
                     } catch (IOException ex) {
                         // ...
                     }
                 }
             }
         }
 
         return null;
     }
 
     /** {@inheritDoc} */
     @Override
     public void checkClientTrusted(final X509Certificate[] chain, final String authType)
             throws CertificateException {
         throw new CertificateException("Not supported.");
     }
 
     /** {@inheritDoc} */
     @Override
     public void checkServerTrusted(final X509Certificate[] chain, final String authType)
             throws CertificateException {
         final List<CertificateException> problems = new ArrayList<CertificateException>();
         boolean verified = false;
 
         if (checkHost) {
             // Check that the cert is issued to the correct host
             final Map<String, String> fields = getDNFieldsFromCert(chain[0]);
             if (fields.containsKey("CN") && fields.get("CN").equals(serverName)) {
                 verified = true;
             }
             
             if (chain[0].getSubjectAlternativeNames() != null && !verified) {
                 for (List<?> entry : chain[0].getSubjectAlternativeNames()) {
                     final int type = ((Integer) entry.get(0)).intValue();
 
                     // DNS or IP
                     if ((type == 2 || type == 7) && entry.get(1).equals(serverName)) {
                         verified = true;
                         break;
                     }
                 }
             }
 
             if (!verified) {
                 problems.add(new CertificateDoesntMatchHostException(
                         "Certificate was not issued to " + serverName));
             }
 
             verified = false;
         }
 
         for (X509Certificate cert : chain) {
             if (checkDate) {
                 // Check that the certificate is in-date
                 try {
                     cert.checkValidity();
                 } catch (CertificateException ex) {
                     problems.add(ex);
                 }
             }
 
             if (checkIssuer) {
                 // Check that we trust an issuer
                 try {
                     for (X509Certificate trustedCert : globalTrustedCAs) {
                         if (cert.getSerialNumber().equals(trustedCert.getSerialNumber())
                                 && cert.getIssuerDN().getName()
                                 .equals(trustedCert.getIssuerDN().getName())) {
                             cert.verify(trustedCert.getPublicKey());
                             verified = true;
                             break;
                         }
                     }
                 } catch (Exception ex) {
                    problems.add(new CertificateException("Issuer couldn't be verified", ex));
                 }
             }
         }
 
         if (!verified && checkIssuer) {
             problems.add(new CertificateNotTrustedException("Issuer is not trusted"));
         }
 
         if (!problems.isEmpty()) {
             Main.getUI().showSSLCertificateDialog(
                     new SSLCertificateDialogModel(chain, problems, this));
 
             actionSem.acquireUninterruptibly();
             
             switch (action) {
                 case DISCONNECT:
                     throw new CertificateException("Not trusted");
                 case IGNORE_PERMANENTY:
                     // TODO: implement
                     break;
                 case IGNORE_TEMPORARILY:
                     // Do nothing, continue connecting
                     break;
             }
         }
     }
 
     /**
      * Sets the action to perform for the request that's in progress.
      *
      * @param action The action that's been selected
      */
     public void setAction(final CertificateAction action) {
         this.action = action;
         
         actionSem.release();
     }
 
     /**
      * Reads the fields from the subject's designated name in the specified
      * certificate.
      *
      * @param cert The certificate to read
      * @return A map of the fields in the certificate's subject's designated
      * name
      */
     public static Map<String, String> getDNFieldsFromCert(final X509Certificate cert) {
         final Map<String, String> res = new HashMap<String, String>();
 
         try {
             final LdapName name = new LdapName(cert.getSubjectX500Principal().getName());
             for (Rdn rdn : name.getRdns()) {
                 res.put(rdn.getType(), rdn.getValue().toString());
             }
         } catch (InvalidNameException ex) {
             // Don't care
         }
 
         return res;
     }
 
     /** {@inheritDoc} */
     @Override
     public X509Certificate[] getAcceptedIssuers() {
         return globalTrustedCAs.toArray(new X509Certificate[globalTrustedCAs.size()]);
     }
 
     /**
      * An exception to indicate that the host on a certificate doesn't match
      * the host we're trying to connect to.
      */
     public static class CertificateDoesntMatchHostException extends CertificateException {
 
         /**
          * A version number for this class. It should be changed whenever the
          * class structure is changed (or anything else that would prevent
          * serialized objects being unserialized with the new class).
          */
         private static final long serialVersionUID = 1;
 
         /**
          * Creates a new CertificateDoesntMatchHostException
          *
          * @param msg A description of the problem
          */
         public CertificateDoesntMatchHostException(String msg) {
             super(msg);
         }
 
     }
 
     /**
      * An exception to indicate that we do not trust the issuer of the
      * certificate (or the CA).
      */
     public static class CertificateNotTrustedException extends CertificateException {
 
         /**
          * A version number for this class. It should be changed whenever the
          * class structure is changed (or anything else that would prevent
          * serialized objects being unserialized with the new class).
          */
         private static final long serialVersionUID = 1;
 
         /**
          * Creates a new CertificateNotTrustedException
          *
          * @param msg A description of the problem
          */
         public CertificateNotTrustedException(String msg) {
             super(msg);
         }
 
     }
 
 }
