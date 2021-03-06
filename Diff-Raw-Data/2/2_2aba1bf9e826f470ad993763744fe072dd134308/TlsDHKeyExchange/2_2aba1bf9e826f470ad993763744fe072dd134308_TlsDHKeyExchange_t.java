 package org.bouncycastle.crypto.tls;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.math.BigInteger;
 
 import org.bouncycastle.asn1.DERBitString;
 import org.bouncycastle.asn1.x509.KeyUsage;
 import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
 import org.bouncycastle.asn1.x509.X509CertificateStructure;
 import org.bouncycastle.asn1.x509.X509Extension;
 import org.bouncycastle.asn1.x509.X509Extensions;
 import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
 import org.bouncycastle.crypto.agreement.DHBasicAgreement;
 import org.bouncycastle.crypto.generators.DHBasicKeyPairGenerator;
 import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
 import org.bouncycastle.crypto.params.DHKeyGenerationParameters;
 import org.bouncycastle.crypto.params.DHParameters;
 import org.bouncycastle.crypto.params.DHPublicKeyParameters;
 import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
 import org.bouncycastle.crypto.params.RSAKeyParameters;
 import org.bouncycastle.crypto.util.PublicKeyFactory;
 import org.bouncycastle.util.BigIntegers;
 
 /**
  * TLS 1.0 DH key exchange.
  */
 class TlsDHKeyExchange implements TlsKeyExchange
 {
     protected static final BigInteger ONE = BigInteger.valueOf(1);
     protected static final BigInteger TWO = BigInteger.valueOf(2);
 
     protected TlsProtocolHandler handler;
     protected CertificateVerifyer verifyer;
     protected short keyExchange;
     protected TlsSigner tlsSigner;
 
     protected AsymmetricKeyParameter serverPublicKey = null;
 
     protected DHPublicKeyParameters dhAgreeServerPublicKey = null;
     protected AsymmetricCipherKeyPair dhAgreeClientKeyPair = null;
 
     TlsDHKeyExchange(TlsProtocolHandler handler, CertificateVerifyer verifyer, short keyExchange)
     {
         switch (keyExchange)
         {
             case TlsKeyExchange.KE_DH_RSA:
             case TlsKeyExchange.KE_DH_DSS:
                 this.tlsSigner = null;
                 break;
             case TlsKeyExchange.KE_DHE_RSA:
                 this.tlsSigner = new TlsRSASigner();
                 break;
             case TlsKeyExchange.KE_DHE_DSS:
                 this.tlsSigner = new TlsDSSSigner();
                 break;
             default:
                 throw new IllegalArgumentException("unsupported key exchange algorithm");
         }
 
         this.handler = handler;
         this.verifyer = verifyer;
         this.keyExchange = keyExchange;
     }
 
     public void skipServerCertificate() throws IOException
     {
         handler.failWithError(AlertLevel.fatal, AlertDescription.unexpected_message);
     }
 
     public void processServerCertificate(Certificate serverCertificate) throws IOException
     {
         X509CertificateStructure x509Cert = serverCertificate.certs[0];
         SubjectPublicKeyInfo keyInfo = x509Cert.getSubjectPublicKeyInfo();
 
         try
         {
             this.serverPublicKey = PublicKeyFactory.createKey(keyInfo);
         }
         catch (RuntimeException e)
         {
             handler.failWithError(AlertLevel.fatal, AlertDescription.unsupported_certificate);
         }
 
         // Sanity check the PublicKeyFactory
         if (this.serverPublicKey.isPrivate())
         {
             handler.failWithError(AlertLevel.fatal, AlertDescription.internal_error);
         }
 
         // TODO 
         /*
          * Perform various checks per RFC2246 7.4.2: "Unless otherwise specified, the
          * signing algorithm for the certificate must be the same as the algorithm for the
          * certificate key."
          */
 
         // TODO Should the 'instanceof' tests be replaces with stricter checks on keyInfo.getAlgorithmId()?
 
         switch (this.keyExchange)
         {
             case TlsKeyExchange.KE_DH_DSS:
                 if (!(this.serverPublicKey instanceof DHPublicKeyParameters))
                 {
                     handler.failWithError(AlertLevel.fatal, AlertDescription.certificate_unknown);
                 }
                 validateKeyUsage(x509Cert, KeyUsage.keyAgreement);
                 // TODO The algorithm used to sign the certificate should be DSS.
 //                x509Cert.getSignatureAlgorithm();
                 this.dhAgreeServerPublicKey = validateDHPublicKey((DHPublicKeyParameters)this.serverPublicKey);
                 break;
             case TlsKeyExchange.KE_DH_RSA:
                 if (!(this.serverPublicKey instanceof DHPublicKeyParameters))
                 {
                     handler.failWithError(AlertLevel.fatal, AlertDescription.certificate_unknown);
                 }
                 validateKeyUsage(x509Cert, KeyUsage.keyAgreement);
                 // TODO The algorithm used to sign the certificate should be RSA.
 //              x509Cert.getSignatureAlgorithm();
                 this.dhAgreeServerPublicKey = validateDHPublicKey((DHPublicKeyParameters)this.serverPublicKey);
                 break;
             case TlsKeyExchange.KE_DHE_RSA:
                 if (!(this.serverPublicKey instanceof RSAKeyParameters))
                 {
                     handler.failWithError(AlertLevel.fatal, AlertDescription.certificate_unknown);
                 }
                 validateKeyUsage(x509Cert, KeyUsage.digitalSignature);
                 break;
             case TlsKeyExchange.KE_DHE_DSS:
                 if (!(this.serverPublicKey instanceof DSAPublicKeyParameters))
                 {
                     handler.failWithError(AlertLevel.fatal, AlertDescription.certificate_unknown);
                 }
                 validateKeyUsage(x509Cert, KeyUsage.digitalSignature);
                 break;
             default:
                 handler.failWithError(AlertLevel.fatal, AlertDescription.unsupported_certificate);
         }
 
         /*
          * Verify them.
          */
         if (!this.verifyer.isValid(serverCertificate.getCerts()))
         {
             handler.failWithError(AlertLevel.fatal, AlertDescription.user_canceled);
         }
     }
 
     public void skipServerKeyExchange() throws IOException
     {
         // OK
     }
 
     public void processServerKeyExchange(InputStream is, SecurityParameters securityParameters)
         throws IOException
     {
         handler.failWithError(AlertLevel.fatal, AlertDescription.unexpected_message);
     }
 
     public void generateClientKeyExchange(OutputStream os) throws IOException
     {
        // TODO RFC 2246 7.4.7.2
         /*
          * If the client certificate already contains a suitable Diffie-Hellman key, then
          * Yc is implicit and does not need to be sent again. In this case, the Client Key
          * Exchange message will be sent, but will be empty.
          */
 //        TlsUtils.writeUint24(0, os);
 
         /*
          * Generate a keypair (using parameters from server key) and send the public value
          * to the server.
          */
         DHBasicKeyPairGenerator dhGen = new DHBasicKeyPairGenerator();
         dhGen.init(new DHKeyGenerationParameters(handler.getRandom(),
             dhAgreeServerPublicKey.getParameters()));
         this.dhAgreeClientKeyPair = dhGen.generateKeyPair();
         BigInteger Yc = ((DHPublicKeyParameters)dhAgreeClientKeyPair.getPublic()).getY();
         byte[] keData = BigIntegers.asUnsignedByteArray(Yc);
         TlsUtils.writeUint24(keData.length + 2, os);
         TlsUtils.writeOpaque16(keData, os);
     }
 
     public byte[] generatePremasterSecret() throws IOException
     {
         /*
          * Diffie-Hellman basic key agreement
          */
         DHBasicAgreement dhAgree = new DHBasicAgreement();
         dhAgree.init(dhAgreeClientKeyPair.getPrivate());
         BigInteger agreement = dhAgree.calculateAgreement(dhAgreeServerPublicKey);
         return BigIntegers.asUnsignedByteArray(agreement);
     }
 
     protected void validateKeyUsage(X509CertificateStructure c, int keyUsageBits) throws IOException
     {
         X509Extensions exts = c.getTBSCertificate().getExtensions();
         if (exts != null)
         {
             X509Extension ext = exts.getExtension(X509Extensions.KeyUsage);
             if (ext != null)
             {
                 DERBitString ku = KeyUsage.getInstance(ext);
                 int bits = ku.getBytes()[0] & 0xff;
                 if ((bits & keyUsageBits) != keyUsageBits)
                 {
                     handler.failWithError(AlertLevel.fatal, AlertDescription.certificate_unknown);
                 }
             }
         }
     }
 
     protected DHPublicKeyParameters validateDHPublicKey(DHPublicKeyParameters key) throws IOException
     {
         BigInteger Y = key.getY();
         DHParameters params = key.getParameters();
         BigInteger p = params.getP();
         BigInteger g = params.getG();
 
         if (!p.isProbablePrime(2))
         {
             handler.failWithError(AlertLevel.fatal, AlertDescription.illegal_parameter);
         }
         if (g.compareTo(TWO) < 0 || g.compareTo(p.subtract(TWO)) > 0)
         {
             handler.failWithError(AlertLevel.fatal, AlertDescription.illegal_parameter);
         }
         if (Y.compareTo(TWO) < 0 || Y.compareTo(p.subtract(ONE)) > 0)
         {
             handler.failWithError(AlertLevel.fatal, AlertDescription.illegal_parameter);
         }
 
         // TODO See RFC 2631 for more discussion of Diffie-Hellman validation
 
         return key;
     }
 }
