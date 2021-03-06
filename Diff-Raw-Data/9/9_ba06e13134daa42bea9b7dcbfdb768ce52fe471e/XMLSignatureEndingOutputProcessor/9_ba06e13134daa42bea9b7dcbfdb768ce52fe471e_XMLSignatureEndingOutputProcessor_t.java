 /**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements. See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership. The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License. You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied. See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 package org.apache.xml.security.stax.impl.processor.output;
 
 import java.security.cert.X509Certificate;
 import java.util.ArrayList;
 import java.util.Deque;
 import java.util.List;
 
 import javax.xml.stream.XMLStreamException;
 
 import org.apache.xml.security.exceptions.XMLSecurityException;
 import org.apache.xml.security.stax.ext.OutputProcessorChain;
 import org.apache.xml.security.stax.ext.XMLSecurityConstants;
 import org.apache.xml.security.stax.ext.XMLSecurityUtils;
 import org.apache.xml.security.stax.ext.stax.XMLSecAttribute;
 import org.apache.xml.security.stax.ext.stax.XMLSecEvent;
 import org.apache.xml.security.stax.ext.stax.XMLSecStartElement;
 import org.apache.xml.security.stax.impl.SignaturePartDef;
 import org.apache.xml.security.stax.impl.algorithms.SignatureAlgorithm;
 import org.apache.xml.security.stax.impl.securityToken.OutboundSecurityToken;
 import org.apache.xml.security.stax.securityEvent.SignatureValueSecurityEvent;
 
 /**
  * An EndingOutputProcessor for XML Signature.
  */
 public class XMLSignatureEndingOutputProcessor extends AbstractSignatureEndingOutputProcessor {
 
     private SignedInfoProcessor signedInfoProcessor = null;
 
     public XMLSignatureEndingOutputProcessor(XMLSignatureOutputProcessor signatureOutputProcessor) throws XMLSecurityException {
         super(signatureOutputProcessor);
         this.addAfterProcessor(XMLSignatureOutputProcessor.class.getName());
     }
 
     @Override
     protected SignedInfoProcessor newSignedInfoProcessor(
             SignatureAlgorithm signatureAlgorithm, XMLSecStartElement xmlSecStartElement,
             OutputProcessorChain outputProcessorChain) throws XMLSecurityException {
 
         this.signedInfoProcessor = new SignedInfoProcessor(signatureAlgorithm, xmlSecStartElement);
         this.signedInfoProcessor.setXMLSecurityProperties(getSecurityProperties());
         this.signedInfoProcessor.setAction(getAction());
         this.signedInfoProcessor.addAfterProcessor(XMLSignatureEndingOutputProcessor.class.getName());
         this.signedInfoProcessor.init(outputProcessorChain);
         return this.signedInfoProcessor;
     }
     
     @Override
     public void processHeaderEvent(OutputProcessorChain outputProcessorChain) throws XMLStreamException, XMLSecurityException {
         super.processHeaderEvent(outputProcessorChain);
         SignatureValueSecurityEvent signatureValueSecurityEvent = new SignatureValueSecurityEvent();
         signatureValueSecurityEvent.setSignatureValue(this.signedInfoProcessor.getSignatureValue());
         outputProcessorChain.getSecurityContext().registerSecurityEvent(signatureValueSecurityEvent);
     }
 
     @Override
     protected void flushBufferAndCallbackAfterTokenID(
             OutputProcessorChain outputProcessorChain, Deque<XMLSecEvent> xmlSecEventDeque)
             throws XMLStreamException, XMLSecurityException {
 
         //@see SANTUARIO-324
         //output root element...
        XMLSecEvent xmlSecEvent = xmlSecEventDeque.pop();
        while (!xmlSecEvent.isStartElement()) {
            outputProcessorChain.reset();
            outputProcessorChain.processEvent(xmlSecEvent);
            xmlSecEvent = xmlSecEventDeque.pop();
        }
         outputProcessorChain.reset();
        outputProcessorChain.processEvent(xmlSecEvent);
         //...then call super to append the signature and flush the rest
         super.flushBufferAndCallbackAfterTokenID(outputProcessorChain, xmlSecEventDeque);
     }
 
     @Override
     protected void createKeyInfoStructureForSignature(
             OutputProcessorChain outputProcessorChain,
             OutboundSecurityToken securityToken,
             boolean useSingleCertificate)
             throws XMLStreamException, XMLSecurityException {
         XMLSecurityConstants.KeyIdentifierType keyIdentifierType = getSecurityProperties().getSignatureKeyIdentifierType();
 
         X509Certificate[] x509Certificates = securityToken.getX509Certificates();
         if (x509Certificates == null) {
             return;
         }
         
         if (keyIdentifierType == XMLSecurityConstants.XMLKeyIdentifierType.KEY_VALUE) {
             XMLSecurityUtils.createKeyValueTokenStructure(this, outputProcessorChain, x509Certificates);
         } else if (keyIdentifierType == null 
                 || keyIdentifierType == XMLSecurityConstants.XMLKeyIdentifierType.X509_ISSUER_SERIAL) {
             createX509IssuerSerialStructure(outputProcessorChain, x509Certificates);
         } else if (keyIdentifierType == XMLSecurityConstants.XMLKeyIdentifierType.X509_SKI) {
             XMLSecurityUtils.createX509SubjectKeyIdentifierStructure(this, outputProcessorChain, x509Certificates);
         } else if (keyIdentifierType == XMLSecurityConstants.XMLKeyIdentifierType.X509_CERTIFICATE) {
             XMLSecurityUtils.createX509CertificateStructure(this, outputProcessorChain, x509Certificates);
         } else if (keyIdentifierType == XMLSecurityConstants.XMLKeyIdentifierType.X509_SUBJECT_NAME) {
             XMLSecurityUtils.createX509SubjectNameStructure(this, outputProcessorChain, x509Certificates);
         } else {
             throw new XMLSecurityException("stax.unsupportedToken", keyIdentifierType);
         }
     }
 
     @Override
     protected void createTransformsStructureForSignature(OutputProcessorChain subOutputProcessorChain, SignaturePartDef signaturePartDef) throws XMLStreamException, XMLSecurityException {
         if (signaturePartDef.getTransforms() != null) {
             createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_Transforms, false, null);
 
             String[] transforms = signaturePartDef.getTransforms();
             for (int i = 0; i < transforms.length; i++) {
                 String transform = transforms[i];
 
                 List<XMLSecAttribute> attributes = new ArrayList<XMLSecAttribute>(1);
                 attributes.add(createAttribute(XMLSecurityConstants.ATT_NULL_Algorithm, transform));
                 createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_Transform, false, attributes);
 
                 if (getSecurityProperties().isAddExcC14NInclusivePrefixes()) {
                     attributes = new ArrayList<XMLSecAttribute>(1);
                     attributes.add(createAttribute(XMLSecurityConstants.ATT_NULL_PrefixList, signaturePartDef.getInclusiveNamespacesPrefixes()));
                     createStartElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_c14nExcl_InclusiveNamespaces, true, attributes);
                     createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_c14nExcl_InclusiveNamespaces);
                 }
 
                 createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_Transform);
             }
             createEndElementAndOutputAsEvent(subOutputProcessorChain, XMLSecurityConstants.TAG_dsig_Transforms);
         }
     }
 }
