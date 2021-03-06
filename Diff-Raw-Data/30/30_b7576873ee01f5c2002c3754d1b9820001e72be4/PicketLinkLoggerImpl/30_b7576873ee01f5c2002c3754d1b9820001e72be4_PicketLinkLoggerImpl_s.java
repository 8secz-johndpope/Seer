 /*
  * JBoss, Home of Professional Open Source.
  * Copyright 2012, Red Hat, Inc., and individual contributors
  * as indicated by the @author tags. See the copyright.txt file in the
  * distribution for a full listing of individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 
 package org.picketlink.identity.federation;
 
 import java.io.IOException;
 import java.security.Principal;
 import java.util.Date;
 
 import javax.security.auth.login.LoginException;
 import javax.xml.crypto.dsig.XMLSignatureException;
 import javax.xml.datatype.XMLGregorianCalendar;
 import javax.xml.namespace.QName;
 import javax.xml.stream.Location;
 import javax.xml.ws.WebServiceException;
 
 import org.picketlink.identity.federation.core.ErrorCodes;
 import org.picketlink.identity.federation.core.exceptions.ConfigurationException;
 import org.picketlink.identity.federation.core.exceptions.ParsingException;
 import org.picketlink.identity.federation.core.exceptions.ProcessingException;
 import org.picketlink.identity.federation.core.interfaces.TrustKeyConfigurationException;
 import org.picketlink.identity.federation.core.interfaces.TrustKeyProcessingException;
 import org.picketlink.identity.federation.core.saml.v2.exceptions.AssertionExpiredException;
 import org.picketlink.identity.federation.core.saml.v2.exceptions.IssuerNotTrustedException;
 import org.picketlink.identity.federation.core.saml.v2.exceptions.SignatureValidationException;
 import org.picketlink.identity.federation.core.wstrust.SamlCredential;
 import org.picketlink.identity.federation.core.wstrust.WSTrustException;
 import org.w3c.dom.Element;
 
 
 /**
  * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
  *
  */
 /**
  * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
  *
  */
 /**
  * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
  *
  */
 public final class PicketLinkLoggerImpl implements PicketLinkLogger {
 
     PicketLinkLoggerImpl() {
         
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#nullArgument(java.lang.String)
      */
     public IllegalArgumentException nullArgumentError(String argument) {
         return PicketLinkMessages.MESSAGES.nullArgument(argument);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#shouldNotBeTheSame(java.lang.String)
      */
     public IllegalArgumentException shouldNotBeTheSameError(String message) {
         return PicketLinkMessages.MESSAGES.shouldNotBeTheSameError(message);
     }
     
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#resourceNotFound(java.lang.String)
      */
     public ProcessingException resourceNotFound(String resource) {
         return PicketLinkMessages.MESSAGES.resourceNotFoundError(resource);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#processingError(java.lang.Throwable)
      */
     public ProcessingException processingError(Throwable t) {
         return PicketLinkMessages.MESSAGES.processingError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#unsupportedType(java.lang.String)
      */
     public RuntimeException unsupportedType(String name) {
         return PicketLinkMessages.MESSAGES.unsupportedType(name);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlResponseDocument(java.lang.String)
      */
     public void samlResponseDocument(String samlResponseDocumentAsString) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlResponseDocument(samlResponseDocumentAsString);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#signatureError(java.lang.Throwable)
      */
     public XMLSignatureException signatureError(Throwable e) {
         return PicketLinkMessages.MESSAGES.signatureError(e);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#sendingXACMLDecisionQuery(java.lang.String)
      */
     public void xacmlSendingDecisionQuery(String xacmlDecisionQueryDocument) {
         PicketLinkLoggerMessages.ROOT_LOGGER.xacmlSendingDecisionQuery(xacmlDecisionQueryDocument);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#nullValue(java.lang.String)
      */
     public RuntimeException nullValueError(String nullValue) {
         return PicketLinkMessages.MESSAGES.nullValue(nullValue);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#notImplementedYet()
      */
     public RuntimeException notImplementedYet(String feature) {
         return PicketLinkMessages.MESSAGES.notImplementedYet(feature);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#auditConfigurationError(javax.naming.NamingException)
      */
     public ConfigurationException auditConfigurationError(Throwable t) {
         return PicketLinkMessages.MESSAGES.auditConfigurationError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#auditNullAuditManager()
      */
     public IllegalStateException auditNullAuditManager() {
         return PicketLinkMessages.MESSAGES.auditNullAuditManagerError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#isInfoEnabled()
      */
     public boolean isInfoEnabled() {
         return PicketLinkLoggerMessages.ROOT_LOGGER.isInfoEnabled();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#auditEvent(java.lang.String)
      */
     public void auditEvent(String auditEvent) {
         PicketLinkLoggerMessages.ROOT_LOGGER.auditEvent(auditEvent);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#injectedValueMissing(java.lang.String)
      */
     public RuntimeException injectedValueMissing(String value) {
         return PicketLinkMessages.MESSAGES.injectedValueMissing(value);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#keystoreSetup()
      */
     public void keyStoreSetup() {
         PicketLinkLoggerMessages.ROOT_LOGGER.keyStoreSetup();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#keyStoreNullStore()
      */
     public IllegalStateException keyStoreNullStore() {
         return PicketLinkMessages.MESSAGES.keyStoreNullStore();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#keyStoreNullPublicKeyForAlias(java.lang.String)
      */
     public void keyStoreNullPublicKeyForAlias(String alias) {
         PicketLinkLoggerMessages.ROOT_LOGGER.keyStoreNullPublicKeyForAlias(alias);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#keyStoreConfigurationError(java.lang.Throwable)
      */
     public TrustKeyConfigurationException keyStoreConfigurationError(Throwable t) {
         return PicketLinkMessages.MESSAGES.keyStoreConfigurationError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#keyStoreProcessingError(java.lang.Throwable)
      */
     public TrustKeyProcessingException keyStoreProcessingError(Throwable t) {
         return PicketLinkMessages.MESSAGES.keyStoreProcessingError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#keyStoreMissingDomainAlias(java.lang.String)
      */
     public IllegalStateException keyStoreMissingDomainAlias(String domain) {
         return PicketLinkMessages.MESSAGES.keyStoreMissingDomainAlias(domain);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#keyStoreNullSigningKeyPass()
      */
     public RuntimeException keyStoreNullSigningKeyPass() {
         return PicketLinkMessages.MESSAGES.keyStoreNullSigningKeyPass();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#keyStoreNotLocated(java.lang.String)
      */
     public RuntimeException keyStoreNotLocated(String keyStore) {
         return PicketLinkMessages.MESSAGES.keyStoreNotLocated(keyStore);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#keyStoreNullAlias()
      */
     public IllegalStateException keyStoreNullAlias() {
         return PicketLinkMessages.MESSAGES.keyStoreNullAlias();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#parserUnknownEndElement(java.lang.String)
      */
     public RuntimeException parserUnknownEndElement(String endElementName) {
         return PicketLinkMessages.MESSAGES.parserUnknownEndElement(endElementName);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#parseUnknownTag(java.lang.String, javax.xml.stream.Location)
      */
     public RuntimeException parserUnknownTag(String tag, Location location) {
         return PicketLinkMessages.MESSAGES.parseUnknownTag(tag, location);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#parseRequiredAttribute(java.lang.String)
      */
     public ParsingException parserRequiredAttribute(String attribute) {
         return PicketLinkMessages.MESSAGES.parseRequiredAttribute(attribute);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#parserUnknownStartElement(java.lang.String, javax.xml.stream.Location)
      */
     public RuntimeException parserUnknownStartElement(String elementName, Location location) {
         return PicketLinkMessages.MESSAGES.parserUnknownStartElement(elementName, location);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#parserNullStartElement()
      */
     public IllegalStateException parserNullStartElement() {
         return PicketLinkMessages.MESSAGES.parserNullStartElement();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#parserUnknownXSI(java.lang.String)
      */
     public ParsingException parserUnknownXSI(String xsiTypeValue) {
         return PicketLinkMessages.MESSAGES.parserUnknownXSI(xsiTypeValue);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#parserExpectedEndTag(java.lang.String)
      */
     public ParsingException parserExpectedEndTag(String tagName) {
         return PicketLinkMessages.MESSAGES.parserExpectedEndTag(tagName);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#parserException(java.lang.Throwable)
      */
     public ParsingException parserException(Throwable t) {
         return PicketLinkMessages.MESSAGES.parserException(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#parserExpectedTextValue(java.lang.String)
      */
     public ParsingException parserExpectedTextValue(String string) {
         return PicketLinkMessages.MESSAGES.parserExpectedTextValue(string);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#parserExpectedXSI(java.lang.String)
      */
     public RuntimeException parserExpectedXSI(String expectedXsi) {
         return PicketLinkMessages.MESSAGES.parserExpectedXSI(expectedXsi);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#parserExpectedTag(java.lang.String, java.lang.String)
      */
     public RuntimeException parserExpectedTag(String tag, String foundElementTag) {
         return PicketLinkMessages.MESSAGES.parserExpectedTag(tag, foundElementTag);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#parserFailed()
      */
     public RuntimeException parserFailed(String elementName) {
         return PicketLinkMessages.MESSAGES.parserFailed(elementName);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#parserUnableParsingNullToken()
      */
     public ParsingException parserUnableParsingNullToken() {
         return PicketLinkMessages.MESSAGES.parserUnableParsingNullToken();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#parserError(java.lang.Throwable)
      */
     public ParsingException parserError(Throwable t) {
         return PicketLinkMessages.MESSAGES.parserError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#lookingParserForElement(javax.xml.namespace.QName)
      */
     public void xmlLookingParserForElement(QName qname) {
         PicketLinkLoggerMessages.ROOT_LOGGER.xmllookingParserForElement(qname);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#receivedXACMLMessage(java.lang.String)
      */
     public void xacmlReceivedMessage(String asString) {
         PicketLinkLoggerMessages.ROOT_LOGGER.xacmlReceivedMessage(asString);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#pdpMessageProcessingError(java.lang.Throwable)
      */
     public RuntimeException xacmlPDPMessageProcessingError(Throwable t) {
         return PicketLinkMessages.MESSAGES.xacmlPDPMessageProcessingError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#fileNotLocated(java.lang.String)
      */
     public IllegalStateException fileNotLocated(String policyConfigFileName) {
         return PicketLinkMessages.MESSAGES.fileNotLocated(policyConfigFileName);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#optionNotSet(java.lang.String)
      */
     public IllegalStateException optionNotSet(String option) {
         return PicketLinkMessages.MESSAGES.optionNotSet(option);
     }
 
     public void stsTokenRegistryNotSpecified() {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsTokenRegistryNotSpecified();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#securityTokenRegistryInvalidType(java.lang.String)
      */
     public void stsTokenRegistryInvalidType(String tokenRegistryOption) {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsTokenRegistryInvalidType(tokenRegistryOption);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#securityTokenRegistryInstantiationError()
      */
     public void stsTokenRegistryInstantiationError() {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsTokenRegistryInstantiationError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#revocationRegistryNotSpecified()
      */
     public void stsRevocationRegistryNotSpecified() {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsRevocationRegistryNotSpecified();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#revocationRegistryInvalidType(java.lang.String)
      */
     public void stsRevocationRegistryInvalidType(String registryOption) {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsRevocationRegistryInvalidType(registryOption);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#revocationRegistryInstantiationError()
      */
     public void stsRevocationRegistryInstantiationError() {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsRevocationRegistryInstantiationError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#assertionExpiredError()
      */
     public ProcessingException samlAssertionExpiredError() {
         return PicketLinkMessages.MESSAGES.assertionExpiredError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#assertionInvalidError()
      */
     public ProcessingException assertionInvalidError() {
         return PicketLinkMessages.MESSAGES.assertionInvalidError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#writerUnknownTypeError(java.lang.String)
      */
     public RuntimeException writerUnknownTypeError(String name) {
         return PicketLinkMessages.MESSAGES.writerUnknownTypeError(name);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#writerNullValueError(java.lang.String)
      */
     public ProcessingException writerNullValueError(String value) {
         return PicketLinkMessages.MESSAGES.writerNullValueError(value);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#writerUnsupportedAttributeValueError(java.lang.String)
      */
     public RuntimeException writerUnsupportedAttributeValueError(String value) {
         return PicketLinkMessages.MESSAGES.writerUnsupportedAttributeValueError(value);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#issuerInfoMissingStatusCodeError()
      */
     public IllegalArgumentException issuerInfoMissingStatusCodeError() {
         return PicketLinkMessages.MESSAGES.issuerInfoMissingStatusCodeError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#classNotLoadedError(java.lang.String)
      */
     public ProcessingException classNotLoadedError(String fqn) {
         return PicketLinkMessages.MESSAGES.classNotLoadedError(fqn);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#couldNotCreateInstance(java.lang.String, java.lang.Throwable)
      */
     public ProcessingException couldNotCreateInstance(String fqn, Throwable t) {
         return PicketLinkMessages.MESSAGES.couldNotCreateInstance(fqn, t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#systemPropertyMissingError(java.lang.String)
      */
     public RuntimeException systemPropertyMissingError(String property) {
         return PicketLinkMessages.MESSAGES.systemPropertyMissingError(property);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#metadataStoreDirectoryCreation(java.lang.String)
      */
     public void samlMetaDataStoreDirectoryCreation(String directory) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlMetaDataDirectoryCreation(directory);
     }
 
     public void samlMetaDataIdentityProviderLoadingError(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlMetaDataIdentityProviderLoadingError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#metaDataServiceProviderLoadingError(java.lang.Throwable)
      */
     public void samlMetaDataServiceProviderLoadingError(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlMetaDataServiceProviderLoadingError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#metaDataPersistEntityDescriptor(java.lang.String)
      */
     public void samlMetaDataPersistEntityDescriptor(String path) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlMetaDataPersistEntityDescriptor(path);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#metaDataPersistTrustedMap(java.lang.String)
      */
     public void samlMetaDataPersistTrustedMap(String path) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlMetaDataPersistTrustedMap(path);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#signatureAssertionValidationError(java.lang.Throwable)
      */
     public void signatureAssertionValidationError(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.signatureAssertionValidationError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#assertionConditions(java.lang.String, java.lang.String, javax.xml.datatype.XMLGregorianCalendar)
      */
     public void samlAssertionConditions(String now, String notBefore, XMLGregorianCalendar notOnOrAfter) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlAssertionConditions(now, notBefore, notOnOrAfter);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#assertionExpired(java.lang.String)
      */
     public void samlAssertionExpired(String id) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlAssertionExpired(id);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#unknownObjectType(java.lang.Object)
      */
     public RuntimeException unknownObjectType(Object attrValue) {
         return PicketLinkMessages.MESSAGES.unknownObjectType(attrValue);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#configurationError(java.lang.Throwable)
      */
     public ConfigurationException configurationError(Throwable t) {
         return PicketLinkMessages.MESSAGES.configurationError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#trace(java.lang.String)
      */
     public void trace(String message) {
         PicketLinkLoggerMessages.ROOT_LOGGER.trace(message);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#signatureUnknownAlgo(java.lang.String)
      */
     public RuntimeException signatureUnknownAlgo(String algo) {
         return PicketLinkMessages.MESSAGES.signatureUnknownAlgo(algo);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#invalidArgumentError(java.lang.String)
      */
     public IllegalArgumentException invalidArgumentError(String message) {
         return PicketLinkMessages.MESSAGES.invalidArgumentError(message);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#creatingDefaultSTSConfig()
      */
     public void stsCreatingDefaultSTSConfig() {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsCreatingDefaultSTSConfig();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsLoadingConfiguration(java.lang.String)
      */
     public void stsLoadingConfiguration(String fileName) {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsLoadingConfiguration(fileName);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsNoTokenProviderError(java.lang.String, java.lang.String)
      */
     public ProcessingException stsNoTokenProviderError(String configuration, String protocolContext) {
         return PicketLinkMessages.MESSAGES.stsNoTokenProviderError(configuration, protocolContext);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#debug(java.lang.String)
      */
     public void debug(String message) {
         PicketLinkLoggerMessages.ROOT_LOGGER.debug(message);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsConfigurationFileNotFoundTCL(java.lang.String)
      */
     public void stsConfigurationFileNotFoundTCL(String fileName) {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsConfigurationFileNotFoundTCL(fileName);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsConfigurationFileNotFoundClassLoader(java.lang.String)
      */
     public void stsConfigurationFileNotFoundClassLoader(String fileName) {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsConfigurationFileNotFoundClassLoader(fileName);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsUsingDefaultConfiguration(java.lang.String)
      */
     public void stsUsingDefaultConfiguration(String fileName) {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsUsingDefaultConfiguration(fileName);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsConfigurationFileLoaded(java.lang.String)
      */
     public void stsConfigurationFileLoaded(String fileName) {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsConfigurationFileLoaded(fileName);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsConfigurationFileParsingError(java.lang.Throwable)
      */
     public ConfigurationException stsConfigurationFileParsingError(Throwable t) {
         return PicketLinkMessages.MESSAGES.stsConfigurationFileParsingError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#notSerializableError(java.lang.String)
      */
     public IOException notSerializableError(String message) {
         return PicketLinkMessages.MESSAGES.notSerializableError(message);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#trustKeyCreationError(java.lang.Throwable)
      */
     public void trustKeyManagerCreationError(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.trustKeyManagerCreationError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#info(java.lang.String)
      */
     public void info(String message) {
         PicketLinkLoggerMessages.ROOT_LOGGER.info(message);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#error(java.lang.String)
      */
     public void error(String message) {
         PicketLinkLoggerMessages.ROOT_LOGGER.error(message);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#couldNotGetXMLSchema(java.lang.Throwable)
      */
     public void xmlCouldNotGetSchema(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.xmlCouldNotGetSchema(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#isTraceEnabled()
      */
     public boolean isTraceEnabled() {
         return PicketLinkLoggerMessages.ROOT_LOGGER.isTraceEnabled();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#isDebugEnabled()
      */
     public boolean isDebugEnabled() {
         return PicketLinkLoggerMessages.ROOT_LOGGER.isDebugEnabled();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#jceProviderCouldNotBeLoaded(java.lang.String, java.lang.Throwable)
      */
     public void jceProviderCouldNotBeLoaded(String name, Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.jceProviderCouldNotBeLoaded(name, t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#writerInvalidKeyInfoNullContent()
      */
     public ProcessingException writerInvalidKeyInfoNullContentError() {
         return PicketLinkMessages.MESSAGES.writerInvalidKeyInfoNullContentError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#notEqualError(java.lang.String, java.lang.String)
      */
     public RuntimeException notEqualError(String first, String second) {
         return PicketLinkMessages.MESSAGES.notEqualError(first, second);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#wrongTypeError(java.lang.String)
      */
     public IllegalArgumentException wrongTypeError(String message) {
         return PicketLinkMessages.MESSAGES.wrongTypeError(message);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#encryptUnknownAlgoError(java.lang.String)
      */
     public RuntimeException encryptUnknownAlgoError(String certAlgo) {
         return PicketLinkMessages.MESSAGES.encryptUnknownAlgoError(certAlgo);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#domMissingDocElementError(java.lang.String)
      */
     public IllegalStateException domMissingDocElementError(String element) {
         return PicketLinkMessages.MESSAGES.domMissingDocElementError(element);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#domMissingElementError(java.lang.String)
      */
     public IllegalStateException domMissingElementError(String element) {
         return PicketLinkMessages.MESSAGES.domMissingElementError(element);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsWSInvalidTokenRequestError()
      */
     public WebServiceException stsWSInvalidTokenRequestError() {
         return PicketLinkMessages.MESSAGES.wsTrustInvalidTokenRequestError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsWSError(java.lang.Throwable)
      */
     public WebServiceException stsWSError(Throwable t) {
         return PicketLinkMessages.MESSAGES.stsWSError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsWSConfigurationError(java.lang.Throwable)
      */
     public WebServiceException stsWSConfigurationError(Throwable t) {
         return PicketLinkMessages.MESSAGES.wsTrustConfigurationError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsWSInvalidRequestTypeError(java.lang.String)
      */
     public WSTrustException stsWSInvalidRequestTypeError(String requestType) {
         return PicketLinkMessages.MESSAGES.stsWSInvalidRequestTypeError(requestType);
     }
 
     public WebServiceException stsWSHandlingTokenRequestError(Throwable t) {
         return PicketLinkMessages.MESSAGES.wsTrustHandlingTokenRequestError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsWSResponseWritingError(java.lang.Throwable)
      */
     public WebServiceException stsWSResponseWritingError(Throwable t) {
         return PicketLinkMessages.MESSAGES.wsTrustResponseWritingError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsUnableToConstructKeyManagerError(java.lang.Throwable)
      */
     public RuntimeException stsUnableToConstructKeyManagerError(Throwable t) {
         return PicketLinkMessages.MESSAGES.stsUnableToConstructKeyManagerError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsPublicKeyError(java.lang.String, java.lang.Throwable)
      */
     public RuntimeException stsPublicKeyError(String serviceName, Throwable t) {
         return PicketLinkMessages.MESSAGES.stsPublicKeyError(serviceName, t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsSigningKeyPairError(java.lang.Throwable)
      */
     public RuntimeException stsSigningKeyPairError(Throwable t) {
         return PicketLinkMessages.MESSAGES.stsSigningKeyPairError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsPublicKeyCertError(java.lang.Throwable)
      */
     public RuntimeException stsPublicKeyCertError(Throwable t) {
         return PicketLinkMessages.MESSAGES.stsPublicKeyCertError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#issuingTokenForPrincipal(java.security.Principal)
      */
     public void samlIssuingTokenForPrincipal(Principal callerPrincipal) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlIssuingTokenForPrincipal(callerPrincipal);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#tokenTimeoutNotSpecified()
      */
     public void stsTokenTimeoutNotSpecified() {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsTokenTimeoutNotSpecified();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#claimsDialectProcessorNotFound(java.lang.String)
      */
     public void wsTrustClaimsDialectProcessorNotFound(String dialect) {
         PicketLinkLoggerMessages.ROOT_LOGGER.wsTrustClaimsDialectProcessorNotFound(dialect);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsCombinedSecretKeyError(java.lang.Throwable)
      */
     public WSTrustException wsTrustCombinedSecretKeyError(Throwable t) {
         return PicketLinkMessages.MESSAGES.wsTrustCombinedSecretKeyError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsClientPublicKeyError()
      */
     public WSTrustException wsTrustClientPublicKeyError() {
         return PicketLinkMessages.MESSAGES.wsTrustClientPublicKeyError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsError(java.lang.Throwable)
      */
     public WSTrustException stsError(Throwable t) {
         return PicketLinkMessages.MESSAGES.stsError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsValidatingTokenForRenewal(java.lang.String)
      */
     public void stsValidatingTokenForRenewal(String details) {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsValidatingTokenForRenewal(details);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#signatureInvalidError(java.lang.String, java.lang.Throwable)
      */
     public XMLSignatureException signatureInvalidError(String message, Throwable t) {
         return PicketLinkMessages.MESSAGES.signatureInvalidError(message, t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsSecurityTokenSignatureNotVerified()
      */
     public void stsSecurityTokenSignatureNotVerified() {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsSecurityTokenSignatureNotVerified();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsStartedValidationForRequest(java.lang.String)
      */
     public void stsStartedValidationForRequest(String details) {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsStartedValidationForRequest(details);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#signatureValidatingDocument(java.lang.String)
      */
     public void signatureValidatingDocument(String nodeAsString) {
         PicketLinkLoggerMessages.ROOT_LOGGER.signatureValidatingDocument(nodeAsString);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsDelegatingValidationToTokenProvider()
      */
     public void stsDelegatingValidationToTokenProvider() {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsDelegatingValidationToTokenProvider();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#signatureElementToBeSigned(java.lang.String)
      */
     public void signatureElementToBeSigned(String namespaceURI) {
         PicketLinkLoggerMessages.ROOT_LOGGER.signatureElementToBeSigned(namespaceURI);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#signatureSignedElement(java.lang.String)
      */
     public void signatureSignedElement(String nodeAsString) {
         PicketLinkLoggerMessages.ROOT_LOGGER.signatureSignedElement(nodeAsString);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#encryptProcessError(java.lang.Throwable)
      */
     public RuntimeException encryptProcessError(Throwable t) {
         return PicketLinkMessages.MESSAGES.encryptProcessError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#pkiLocatingPublic(java.lang.String)
      */
     public void pkiLocatingPublic(String alias) {
         PicketLinkLoggerMessages.ROOT_LOGGER.pkiLocatingPublicKey(alias);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsSecurityTokenShouldBeEncrypted()
      */
     public void stsSecurityTokenShouldBeEncrypted() {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsSecurityTokenShouldBeEncrypted();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsReceivedRequestType(java.lang.String)
      */
     public void stsReceivedRequestType(String requestType) {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsReceivedRequestType(requestType);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsKeyTypeNotFoundUsingDefaultBearer()
      */
     public void stsKeyTypeNotFoundUsingDefaultBearer() {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsKeyTypeNotFoundUsingDefaultBearer();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsKeySizeNotFoundUsingDefault(long)
      */
     public void stsKeySizeNotFoundUsingDefault(long keySize) {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsKeySizeNotFoundUsingDefault(keySize);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsUnableToDecodePasswordError(java.lang.String)
      */
     public RuntimeException unableToDecodePasswordError(String password) {
         return PicketLinkMessages.MESSAGES.stsUnableToDecodePasswordError(password);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#couldNotLoadProperties(java.lang.String)
      */
     public IllegalStateException couldNotLoadProperties(String configFile) {
         return PicketLinkMessages.MESSAGES.couldNotLoadProperties(configFile);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsUnableToParseOnBehalfType(java.lang.Object)
      */
     public void stsUnableToParseOnBehalfType(Object type) {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsUnableToParseOnBehalfType(type);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsKeyInfoTypeCreationError(java.lang.Throwable)
      */
     public WSTrustException stsKeyInfoTypeCreationError(Throwable t) {
         return PicketLinkMessages.MESSAGES.stsKeyInfoTypeCreationError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsSecretKeyNotEncrypted()
      */
     public void stsSecretKeyNotEncrypted() {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsSecretKeyNotEncrypted();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authCouldNotIssueSAMLToken()
      */
     public LoginException authCouldNotIssueSAMLToken() {
         return PicketLinkMessages.MESSAGES.authCouldNotIssueSAMLToken();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authLoginError(java.lang.Throwable)
      */
     public LoginException authLoginError(Throwable t) {
         return PicketLinkMessages.MESSAGES.authLoginError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authAddedSAMLCredential(org.picketlink.identity.federation.core.wstrust.SamlCredential)
      */
     public void authAddedSAMLCredential(SamlCredential samlCredential) {
         PicketLinkLoggerMessages.ROOT_LOGGER.authAddedSAMLCredential(samlCredential);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authUserNameFromCallbackIsNull()
      */
     public void authUserNameFromCallbackIsNull() {
         PicketLinkLoggerMessages.ROOT_LOGGER.authUserNameFromCallbackisNull();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authPasswordFromCallbackIsNull()
      */
     public void authPasswordFromCallbackIsNull() {
         PicketLinkLoggerMessages.ROOT_LOGGER.authPasswordFromCallbackIsNull();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authCouldNotCreateWSTrustClient(java.lang.Throwable)
      */
     public IllegalStateException authCouldNotCreateWSTrustClient(Throwable t) {
         return PicketLinkMessages.MESSAGES.authCouldNotCreateWSTrustClient(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authSAMLAssertionWithoutExpiration(java.lang.String)
      */
     public void samlAssertionWithoutExpiration(String id) {
         PicketLinkLoggerMessages.ROOT_LOGGER.authSAMLAssertionWithoutExpiration(id);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authCouldNotValidateSAMLToken(org.w3c.dom.Element)
      */
     public LoginException authCouldNotValidateSAMLToken(Element token) {
         return PicketLinkMessages.MESSAGES.authCouldNotValidateSAMLToken(token);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authSAMLValidationResult(boolean)
      */
     public void authSAMLValidationResult(boolean result) {
         PicketLinkLoggerMessages.ROOT_LOGGER.authSAMLValidationResult(result);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authCouldNotLocateSecurityToken()
      */
     public LoginException authCouldNotLocateSecurityToken() {
         return PicketLinkMessages.MESSAGES.authCouldNotLocateSecurityTokenError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#wsTrustNullCancelTargetError()
      */
     public ProcessingException wsTrustNullCancelTargetError() {
         return PicketLinkMessages.MESSAGES.wsTrustNullCancelTargetError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#saml11MarshallError(java.lang.Throwable)
      */
     public ProcessingException samlAssertionMarshallError(Throwable t) {
         return PicketLinkMessages.MESSAGES.saml11MarshallError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#wsTrustNullRenewTargetError()
      */
     public ProcessingException wsTrustNullRenewTargetError() {
         return PicketLinkMessages.MESSAGES.wsTrustNullRenewTargetError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#saml11UnmarshallError(java.lang.Throwable)
      */
     public ProcessingException samlAssertionUnmarshallError(Throwable t) {
         return PicketLinkMessages.MESSAGES.saml11UnmarshallError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlAssertionRevokedCouldNotRenew(java.lang.String)
      */
     public ProcessingException samlAssertionRevokedCouldNotRenew(String id) {
         return PicketLinkMessages.MESSAGES.samlAssertionRevokedCouldNotRenew(id);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlStartingValidation()
      */
     public void samlAssertionStartingValidation() {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlStartingValidation();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#wsTrustNullValidationTargetError()
      */
     public ProcessingException wsTrustNullValidationTargetError() {
         return PicketLinkMessages.MESSAGES.wsTrustNullValidationTargetError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsNoAttributeProviderSet()
      */
     public void stsNoAttributeProviderSet() {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsNoAttributeProviderSet();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsWrongAttributeProviderTypeNotInstalled(java.lang.String)
      */
     public void stsWrongAttributeProviderTypeNotInstalled(String attributeProviderClassName) {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsWrongAttributeProviderTypeNotInstalled(attributeProviderClassName);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#stsAttributeProviderInstationError(java.lang.Exception)
      */
     public void attributeProviderInstationError(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.stsAttributeProviderInstantiationError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlAssertion(java.lang.String)
      */
     public void samlAssertion(String nodeAsString) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlAssertion(nodeAsString);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#wsTrustUnableToGetDataTypeFactory(java.lang.Throwable)
      */
     public RuntimeException wsTrustUnableToGetDataTypeFactory(Throwable t) {
         return PicketLinkMessages.MESSAGES.wsTrustUnableToGetDataTypeFactoryError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#wsTrustValidationStatusCodeMissing()
      */
     public ProcessingException wsTrustValidationStatusCodeMissing() {
         return PicketLinkMessages.MESSAGES.wsTrustValidationStatusCodeMissing();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#identityServerActiveSessionCount(int)
      */
     public void samlIdentityServerActiveSessionCount(int activeSessionCount) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlIdentityServerActiveSessionCount(activeSessionCount);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#identityServerSessionCreated(java.lang.String, int)
      */
     public void samlIdentityServerSessionCreated(String id, int activeSessionCount) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlIdentityServerSessionCreated(id, activeSessionCount);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#identityServerSessionDestroyed(java.lang.String, int)
      */
     public void samlIdentityServerSessionDestroyed(String id, int activeSessionCount) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlIdentityServerSessionDestroyed(id, activeSessionCount);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#unknowCredentialType(java.lang.String)
      */
     public RuntimeException unknowCredentialType(String name) {
         return PicketLinkMessages.MESSAGES.unknownCredentialTypeError(name);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerRoleGeneratorSetup(java.lang.String)
      */
     public void samlHandlerRoleGeneratorSetup(String name) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerRoleGeneratorSetup(name);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerRoleGeneratorSetupError(java.lang.Throwable)
      */
     public void samlHandlerRoleGeneratorSetupError(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerRoleGeneratorSetupError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerAttributeSetup(java.lang.String)
      */
     public void samlHandlerAttributeSetup(String name) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerAttributeSetup(name);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerAssertionNotFound()
      */
     public RuntimeException samlHandlerAssertionNotFound() {
         return PicketLinkMessages.MESSAGES.samlHandlerAssertionNotFound();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerAuthnRequestIsNull()
      */
     public ProcessingException samlHandlerAuthnRequestIsNull() {
         return PicketLinkMessages.MESSAGES.samlHandlerAuthnRequestIsNullError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#destination(java.lang.String)
      */
     public void destination(String destination) {
         PicketLinkLoggerMessages.ROOT_LOGGER.destination(destination);
     }
 
     public void samlHandlerAuthenticationError(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerAuthenticationError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerNoAssertionFromIDP()
      */
     public IllegalArgumentException samlHandlerNoAssertionFromIDP() {
         return PicketLinkMessages.MESSAGES.samlHandlerNoAssertionFromIDPError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerNullEncryptedAssertion()
      */
     public ProcessingException samlHandlerNullEncryptedAssertion() {
         return PicketLinkMessages.MESSAGES.samlHandlerNullEncryptedAssertion();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerIDPAuthenticationFailedError()
      */
     public SecurityException samlHandlerIDPAuthenticationFailedError() {
         return PicketLinkMessages.MESSAGES.samlHandlerIDPAuthenticationFailedError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#assertionExpiredError(org.picketlink.identity.federation.core.saml.v2.exceptions.AssertionExpiredException)
      */
     public ProcessingException assertionExpiredError(AssertionExpiredException aee) {
         return PicketLinkMessages.MESSAGES.assertionExpiredErrorWithException(aee);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#invalidRole(java.lang.String)
      */
     public void invalidRole(String roles) {
         PicketLinkLoggerMessages.ROOT_LOGGER.invalidRole(roles);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#unsupportedRoleType(java.lang.Object)
      */
     public RuntimeException unsupportedRoleType(Object attrValue) {
         return PicketLinkMessages.MESSAGES.unsupportedRoleType(attrValue);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerSavedAuthnRequestIdIntoSession(java.lang.String)
      */
     public void samlHandlerSavedAuthnRequestIdIntoSession(String authnRequestId) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerSavedAuthnRequestIdIntoSession(authnRequestId);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerSuccessfulInResponseToValidation(java.lang.String)
      */
     public void samlHandlerSuccessfulInResponseToValidation(String inResponseTo) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerSuccessfulInResponseToValidation(inResponseTo);
     }
     
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerFailedInResponseToVerification(java.lang.String, java.lang.String)
      */
     public void samlHandlerFailedInResponseToVerification(String inResponseTo, String authnRequestId) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerFailedInResponseToVerification(inResponseTo, authnRequestId);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerFailedInResponseToVerificarionError()
      */
     public ProcessingException samlHandlerFailedInResponseToVerificarionError() {
         return PicketLinkMessages.MESSAGES.samlHandlerFailedInResponseToVerificarionError();
     }
     
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerDomainsTrustedByIDP(java.lang.String, java.lang.String)
      */
     public void samlTrustedDomains(String domainsTrusted, String issuerDomain) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerDomainsTrustedByIDP(domainsTrusted, issuerDomain);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerTrustDomainCheck(java.lang.String)
      */
     public void samlTrustedDomainCheck(String uriBit) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerTrustDomainCheck(uriBit);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerTrustedDomainMatched(java.lang.String, java.lang.String)
      */
     public void samlHandlerTrustedDomainMatched(String uriBit, String issuerDomain) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerTrustedDomainMatched(uriBit, issuerDomain);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerIssuerNotTrustedError(java.lang.String)
      */
     public IssuerNotTrustedException samlIssuerNotTrustedError(String issuer) {
         return PicketLinkMessages.MESSAGES.samlHandlerIssuerNotTrustedError(issuer);
     }
 
     public IssuerNotTrustedException samlIssuerNotTrustedException(Throwable t) {
         return PicketLinkMessages.MESSAGES.samlHandlerIssuerNotTrustedError(t);
     }
 
     public void samlHandlerDomainsTrustedBySP(String domainsTrusted, String issuerDomain) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerDomainsTrustedBySP(domainsTrusted, issuerDomain);       
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerTrustElementMissingError()
      */
     public ConfigurationException samlHandlerTrustElementMissingError() {
         return PicketLinkMessages.MESSAGES.samlHandlerTrustElementMissingError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerIdentityServerNotFound()
      */
     public ProcessingException samlHandlerIdentityServerNotFoundError() {
         return PicketLinkMessages.MESSAGES.samlHandlerIdentityServerNotFoundError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerPrincipalNotFoundError()
      */
     public ProcessingException samlHandlerPrincipalNotFoundError() {
         return PicketLinkMessages.MESSAGES.samlHandlerPrincipalNotFoundError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerGeneratingSuccessStatusResponse(java.lang.String)
      */ 
     public void samlHandlerGeneratingSuccessStatusResponse(String originalIssuer) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerGeneratingSuccessStatusResponse(originalIssuer);
     }
     
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerNoDocumentToSign()
      */
     public void samlHandlerNoDocumentToSign() {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerNoDocumentToSign();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerNoResponseDocumentFound()
      */
     public void samlHandlerNoResponseDocumentFound() {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerNoResponseDocumentFound();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerSigningDocumentForPOSTBinding()
      */
     public void samlHandlerSigningDocumentForPOSTBinding() {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerSigningDocumentForPOSTBinding();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerSigningDocumentForRedirectBinding()
      */
     public void samlHandlerSigningDocumentForRedirectBinding() {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerSigningDocumentForRedirectBinding();
     }
     
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerKeyPairNotFound()
      */
     public void samlHandlerKeyPairNotFound() {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerKeyPairNotFound();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerKeyPairNotFoundError()
      */
     public ProcessingException samlHandlerKeyPairNotFoundError() {
         return PicketLinkMessages.MESSAGES.samlHandlerKeyPairNotFoundError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerErrorSigningRedirectBindingMessage(java.lang.Throwable)
      */
     public void samlHandlerErrorSigningRedirectBindingMessage(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerErrorSigningRedirectBindingMessage(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerSigningRedirectBindingMessageError(java.lang.Throwable)
      */
     public RuntimeException samlHandlerSigningRedirectBindingMessageError(Throwable t) {
         return PicketLinkMessages.MESSAGES.samlHandlerSigningRedirectBindingMessageError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerValidatingResponseForHTTPMethod(java.lang.String)
      */
     public void samlHandlerValidatingResponseForHTTPMethod(String method) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerValidatingResponseForHTTPMethod(method);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#signatureValidationError()
      */
     public SignatureValidationException samlHandlerSignatureValidationFailed() {
         return PicketLinkMessages.MESSAGES.signatureValidationFailed();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerErrorValidatingSignature(java.lang.Throwable)
      */
     public void samlHandlerErrorValidatingSignature(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerErrorValidatingSignature(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerInvalidSignatureError()
      */
     public ProcessingException samlHandlerInvalidSignatureError() {
         return PicketLinkMessages.MESSAGES.samlHandlerInvalidSignatureError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerSignatureNorPresentError()
      */
     public ProcessingException samlHandlerSignatureNotPresentError() {
         return PicketLinkMessages.MESSAGES.samlHandlerSignatureNorPresentError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerSignatureValidationError(java.lang.Throwable)
      */
     public ProcessingException samlHandlerSignatureValidationError(Throwable t) {
         return PicketLinkMessages.MESSAGES.samlHandlerSignatureValidationError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlIDPUserClosedBrowserCancelingToken()
      */
     public void samlIDPUserClosedBrowserCancelingToken() {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlIDPUserClosedBrowserCancelingToken();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#error(java.lang.Throwable)
      */
     public void error(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.error(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerList(java.lang.String)
      */
     public void samlHandlerList(String handlers) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerList(handlers);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerFinishedProcessing(java.lang.String)
      */
     public void samlHandlerFinishedProcessing(String handlerClassName) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlHandlerFinishedProcessing(handlerClassName);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerChainProcessingError(java.lang.Throwable)
      */
     public RuntimeException samlHandlerChainProcessingError(Throwable t) {
         return PicketLinkMessages.MESSAGES.samlHandlerChainProcessingError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#trustKeyManagerMissing()
      */
     public TrustKeyConfigurationException trustKeyManagerMissing() {
         return PicketLinkMessages.MESSAGES.trustKeyManagerMissing();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlRequestDocument(java.lang.String)
      */
     public void samlRequestDocument(String samlRequestDocument) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlRequestDocument(samlRequestDocument);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlBase64DecodingError(java.lang.Throwable)
      */
     public void samlBase64DecodingError(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlBase64DecodingError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#samlParsingError(java.lang.Throwable)
      */
     public void samlParsingError(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.samlParsingError(t);
     }
 
     public void trace(Throwable t) {
         if (isTraceEnabled()) {
             PicketLinkLoggerMessages.ROOT_LOGGER.trace(t);
         }
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#attributeManagerMappingContextNull()
      */
     public void mappingContextNull() {
         PicketLinkLoggerMessages.ROOT_LOGGER.attributeManagerMappingContextNull();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#attributeManagerError(java.lang.Throwable)
      */
     public void attributeManagerError(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.attributeManagerError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#couldNotObtainSecurityContext()
      */
     public void couldNotObtainSecurityContext() {
         PicketLinkLoggerMessages.ROOT_LOGGER.couldNotObtainSecurityContext();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#attributeManagerMapSize(int)
      */
     public void attributeManagerMapSize(int size) {
         PicketLinkLoggerMessages.ROOT_LOGGER.attributeManagerMapSize(size);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authenticationSubjectNotFound()
      */
     public void authenticationSubjectNotFound() {
         PicketLinkLoggerMessages.ROOT_LOGGER.authenticationSubjectNotFound();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#returningAttributeStatement(java.lang.String, java.lang.String)
      */
     public void returningAttributeStatement(String tokenRoleAttributeName, String attributes) {
         PicketLinkLoggerMessages.ROOT_LOGGER.returningAttributeStatement(tokenRoleAttributeName, attributes);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authFailedToCreatePrincipal(java.lang.Throwable)
      */
     public LoginException authFailedToCreatePrincipal(Throwable t) {
         return PicketLinkMessages.MESSAGES.authFailedToCreatePrincipal(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authSharedCredentialIsNotSAMLCredential()
      */
     public LoginException authSharedCredentialIsNotSAMLCredential(String className) {
         return PicketLinkMessages.MESSAGES.authSharedCredentialIsNotSAMLCredential(className);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authSTSConfigFileNotFound()
      */
     public LoginException authSTSConfigFileNotFound() {
         return PicketLinkMessages.MESSAGES.authSTSConfigFileNotFound();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authErrorHandlingCallback(java.lang.Throwable)
      */
     public LoginException authErrorHandlingCallback(Throwable t) {
         return PicketLinkMessages.MESSAGES.authErrorHandlingCallbackError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authPerformingLocalValidation()
      */
     public void authPerformingLocalValidation() {
         PicketLinkLoggerMessages.ROOT_LOGGER.authPerformingLocalValidation();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authSuccessfulLocalValidation()
      */
     public void authSuccessfulLocalValidation() {
         PicketLinkLoggerMessages.ROOT_LOGGER.authSuccessfulLocalValidation();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authLocalValidationDisabledCheckSTS()
      */
     public void authLocalValidationDisabledCheckSTS() {
         PicketLinkLoggerMessages.ROOT_LOGGER.authLocalValidationDisabledCheckSTS();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authInvalidSAMLAssertionBySTS()
      */
     public LoginException authInvalidSAMLAssertionBySTS() {
         return PicketLinkMessages.MESSAGES.authInvalidSAMLAssertionBySTSError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authAssertionValidationValies(java.lang.Throwable)
      */
     public LoginException authAssertionValidationError(Throwable t) {
         return PicketLinkMessages.MESSAGES.authAssertionValidationValidationError(t);
     }
     
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authCreatingCacheEntry(java.util.Date, java.util.Date)
      */
     public void authCreatingCacheEntry(Date date, Date expiryDate) {
         PicketLinkLoggerMessages.ROOT_LOGGER.authCreatingCacheEntry(date, expiryDate);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authFailedToParseSAMLAssertion(java.lang.Throwable)
      */
     public LoginException authFailedToParseSAMLAssertion(Throwable t) {
         return PicketLinkMessages.MESSAGES.authFailedToParseSAMLAssertionError(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authSAMLAssertionToGetRolesFrom(java.lang.String)
      */ 
     public void authSAMLAssertionToGetRolesFrom(String samlAssertion) {
         PicketLinkLoggerMessages.ROOT_LOGGER.authSAMLAssertionToGetRolesFrom(samlAssertion);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#initializedWith(java.lang.String)
      */
     public void initializedWith(String string) {
         PicketLinkLoggerMessages.ROOT_LOGGER.initializedWith(string);
     }
 
     public void authSharedTokenNotFound(String name, String sharedToken) {
         PicketLinkLoggerMessages.ROOT_LOGGER.authSharedTokenNotFound(name, sharedToken);
     }
 
     public void authMappedRoles(String roles) {
         PicketLinkLoggerMessages.ROOT_LOGGER.authMappedRoles(roles);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authMappedPrincipal(java.lang.String)
      */
     public void authMappedPrincipal(String principal) {
         PicketLinkLoggerMessages.ROOT_LOGGER.authMappedPrincipal(principal);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authSAMLAssertionPasingFailed(java.lang.Throwable)
      */
     public void authSAMLAssertionPasingFailed(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.authSAMLAssertionParsingFailed(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#determinedSecurityDomain(java.lang.String)
      */
     public void determinedSecurityDomain(String securityDomain) {
         PicketLinkLoggerMessages.ROOT_LOGGER.determinedSecurityDomain(securityDomain);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#cacheWillExpireForPrincipal(int, java.lang.String)
      */
     public void cacheWillExpireForPrincipal(int seconds, String principal) {
         PicketLinkLoggerMessages.ROOT_LOGGER.cacheWillExpireForPrincipal(seconds, principal);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authNullKeyStoreFromSecurityDomainError(java.lang.String)
      */
     public LoginException authNullKeyStoreFromSecurityDomainError(String name) {
         return PicketLinkMessages.MESSAGES.authNullKeyStoreFromSecurityDomainError(name);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authNullKeyStoreAliasFromSecurityDomainError(java.lang.String)
      */
     public LoginException authNullKeyStoreAliasFromSecurityDomainError(String name) {
         return PicketLinkMessages.MESSAGES.authNullKeyStoreAliasFromSecurityDomainError(name);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authNoCertificateFoundForAlias(java.lang.String, java.lang.String)
      */
     public LoginException authNoCertificateFoundForAliasError(String alias, String name) {
         return PicketLinkMessages.MESSAGES.authNoCertificateFoundForAliasError(alias, name);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authSAMLInvalidSignature()
      */
     public LoginException authSAMLInvalidSignatureError() {
         return PicketLinkMessages.MESSAGES.authSAMLInvalidSignatureError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authSAMLAssertionExpiredError()
      */
     public LoginException authSAMLAssertionExpiredError() {
         return PicketLinkMessages.MESSAGES.authSAMLAssertionExpiredError();
     }
     
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authConstructingSTSClientInterceptor(java.lang.String)
      */
     public void authConstructingSTSClientInterceptor(String propertiesFile) {
         PicketLinkLoggerMessages.ROOT_LOGGER.authConstructingSTSClientInterceptor(propertiesFile);
     }
     
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authRetrievedSecurityContextFromInvocation(java.lang.String)
      */
     public void authRetrievedSecurityContextFromInvocation(String string) {
         PicketLinkLoggerMessages.ROOT_LOGGER.authRetrievedSecurityContextFromInvocation(string);
     }
     
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authInvokingSTSForSAMLAssertion(java.lang.String)
      */
     public void authInvokingSTSForSAMLAssertion(String principalName) {
         PicketLinkLoggerMessages.ROOT_LOGGER.authInvokingSTSForSAMLAssertion(principalName);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authSAMLAssertionObtainedForPrincipal(java.lang.String)
      */
     public void authSAMLAssertionObtainedForPrincipal(String principalName) {
         PicketLinkLoggerMessages.ROOT_LOGGER.authSAMLAssertionObtainedForPrincipal(principalName);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#authSAMLAssertionIssuingFailed(java.lang.Throwable)
      */
     public void authSAMLAssertionIssuingFailed(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.authSAMLAssertionIssuingFailed(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#jbossWSHandlingOutboundMessage()
      */
     public void jbossWSHandlingOutboundMessage() {
         PicketLinkLoggerMessages.ROOT_LOGGER.jbossWSHandlingOutboundMessage();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#jbossWSUnableToCreateBinaryToken(java.lang.Throwable)
      */
     public void jbossWSUnableToCreateBinaryToken(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.jbossWSUnableToCreateBinaryToken(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#jbossWSUnableToCreateSecurityToken()
      */
     public void jbossWSUnableToCreateSecurityToken() {
         PicketLinkLoggerMessages.ROOT_LOGGER.jbossWSUnableToCreateSecurityToken();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#jbossWSUnableToWriteSOAPMessage(java.lang.Throwable)
      */
     public void jbossWSUnableToWriteSOAPMessage(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.jbossWSUnableToWriteSOAPMessage(t);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#jbossWSHeaderValueIdentified(java.lang.String)
      */
     public void jbossWSHeaderValueIdentified(String headerValue) {
         PicketLinkLoggerMessages.ROOT_LOGGER.jbossWSHeaderValueIdentified(headerValue);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#jbossWSCookieValueIdentified(java.lang.String)
      */
     public void jbossWSCookieValueIdentified(String cookie) {
         PicketLinkLoggerMessages.ROOT_LOGGER.jbossWSCookieValueIdentified(cookie);
     }
 
     public void jbossWSHandlingInboundMessage() {
         PicketLinkLoggerMessages.ROOT_LOGGER.jbossWSHandlingInboundMessage();       
     }
 
     public void jbossWSSAMLAssertionFoundInPayload(String assertionAsString) {
         PicketLinkLoggerMessages.ROOT_LOGGER.jbossWSSAMLAssertionFoundInPayload(assertionAsString);
     }
     
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#jbossWSRoleKeysExtractRolesFromAssertion(java.lang.String)
      */
     public void jbossWSRoleKeysExtractRolesFromAssertion(String string) {
         PicketLinkLoggerMessages.ROOT_LOGGER.jbossWSRoleKeysExtractRolesFromAssertion(string);
     }
 
     public void jbossWSRolesInAssertion(String roles) {
         PicketLinkLoggerMessages.ROOT_LOGGER.jbossWSRolesInAssertion(roles);
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#jbossWSNoRolesFoundInAssertion()
      */
     public void jbossWSNoRolesFoundInAssertion() {
         PicketLinkLoggerMessages.ROOT_LOGGER.jbossWSNoRolesFoundInAssertion();
     }
 
     public void jbossWSNoAssertionsFound() {
         PicketLinkLoggerMessages.ROOT_LOGGER.jbossWSNoAssertionsFound();
     }
 
    public void jbosswsSuccessfullyAuthenticatedPrincipal(String principal, String subject) {
         PicketLinkLoggerMessages.ROOT_LOGGER.jbossWSSuccessfullyAuthenticatedPrincipal(principal, subject);
     }
     
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#jbossWSUnableToLoadJBossWSSEConfigError()
      */
     public RuntimeException jbossWSUnableToLoadJBossWSSEConfigError() {
         return PicketLinkMessages.MESSAGES.jbossWSUnableToLoadJBossWSSEConfigError();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#jbossWSAuthorizationFailed()
      */
     public RuntimeException jbossWSAuthorizationFailed() {
         return PicketLinkMessages.MESSAGES.jbossWSAuthorizationFailed();
     }
 
     /* (non-Javadoc)
      * @see org.picketlink.identity.federation.PicketLinkLogger#jbossWSErrorGettingOperationName(java.lang.Throwable)
      */
     public void jbossWSErrorGettingOperationName(Throwable t) {
         PicketLinkLoggerMessages.ROOT_LOGGER.jbossWSErrorGettingOperationname(t);
     }
 
 }
