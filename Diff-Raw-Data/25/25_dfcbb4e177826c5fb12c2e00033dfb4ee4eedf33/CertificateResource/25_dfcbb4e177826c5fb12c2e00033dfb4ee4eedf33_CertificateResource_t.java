 /**
  * Copyright (c) 2009 Red Hat, Inc.
  *
  * This software is licensed to you under the GNU General Public License,
  * version 2 (GPLv2). There is NO WARRANTY for this software, express or
  * implied, including the implied warranties of MERCHANTABILITY or FITNESS
  * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
  * along with this software; if not, see
  * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
  *
  * Red Hat trademarks are not licensed under GPLv2. No permission is
  * granted to use or replicate Red Hat trademarks that are incorporated
  * in this software or its documentation.
  */
 package org.fedoraproject.candlepin.resource;
 
 import org.fedoraproject.candlepin.model.CertificateCurator;
 import org.fedoraproject.candlepin.model.Owner;
 import org.fedoraproject.candlepin.model.OwnerCurator;
 import org.fedoraproject.candlepin.model.SpacewalkCertificateCurator;
 
 import com.redhat.rhn.common.cert.Certificate;
 import com.redhat.rhn.common.cert.CertificateFactory;
 
 import com.google.inject.Inject;
 import com.sun.jersey.core.util.Base64;
 
 import org.apache.log4j.Logger;
 import org.jdom.JDOMException;
 
 import java.io.IOException;
 import java.text.ParseException;
 
 import javax.ws.rs.Consumes;
 import javax.ws.rs.GET;
 import javax.ws.rs.POST;
 import javax.ws.rs.Path;
 import javax.ws.rs.Produces;
 import javax.ws.rs.core.MediaType;
 
 
 /**
  * CertificateResource
  */
 @Path("/certificate")
 public class CertificateResource  {
     private static String encodedCert = ""; // bad bad bad
 
     
     private OwnerCurator ownerCurator;
     private CertificateCurator certificateCurator;
     private SpacewalkCertificateCurator spacewalkCertificateCurator;
 
     private static Logger log = Logger.getLogger(CertificateResource.class);
   
    /**
     * default ctor
     * @param ownerCurator interact with the owner.
     * @param spacewalkCertificateCurator interact with spacewalk certificate
     * @param certificateCurator interact with certificates
     */
     @Inject
     public CertificateResource(OwnerCurator ownerCurator,
                                SpacewalkCertificateCurator spacewalkCertificateCurator,
                                CertificateCurator certificateCurator) {
 
         this.ownerCurator = ownerCurator;
         this.spacewalkCertificateCurator = spacewalkCertificateCurator;
         this.certificateCurator = certificateCurator;
     }
     
     /**
      * Uploads the certificate containing list of entitlements.
      * @param base64cert base64 encoded certificate.
      * @return id of certificate.
      */
     @POST
     @Consumes(MediaType.APPLICATION_JSON)
     @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
     public String upload(String base64cert) {
         
         try {
             if (base64cert == null || "".equals(base64cert)) {
                 throw new BadRequestException("Empty certificate is being uploaded.");
             }
             
             encodedCert = base64cert;
             String decoded = Base64.base64Decode(base64cert);
             Certificate cert = CertificateFactory.read(decoded);
             
             Owner owner = addOwner(cert);
             org.fedoraproject.candlepin.model.Certificate certBlob =
                 new org.fedoraproject.candlepin.model.Certificate(decoded, owner);
             certificateCurator.create(certBlob);
            
             spacewalkCertificateCurator.parseCertificate(cert, owner);
         }
         catch (JDOMException e) {
            throw new BadRequestException(
                "Invalid certificate is being uploaded: " + e.getMessage());
         }
         catch (IOException e) {
            throw new BadRequestException(
                "Invalid certificate is being uploaded: " + e.getMessage());
         }
         catch (ParseException e) {
            throw new BadRequestException(
                "Invalid certificate is being uploaded: " + e.getMessage());
         }
         return "uuid";
     }
     
 
    /**
     * Return the encoded certificate.
     * @return the encoded certificate.
     */
     @GET
     @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
     public String get() {
 //        byte[] s = null;
 //        if (cert != null) {
 //            s = Base64.encode(cert.asXmlString());
 //        }
 //        
 //        String str = createString(s);
 //        System.out.println(str);
 //        return str;
         return encodedCert;
     }
     
     private Owner addOwner(Certificate cert) {
         Owner owner = ownerCurator.lookupByName(cert.getOwner());
         if (owner == null) {
             owner = new Owner(cert.getOwner());
             ownerCurator.create(owner);
         }
         return owner;
     }
     
 }
