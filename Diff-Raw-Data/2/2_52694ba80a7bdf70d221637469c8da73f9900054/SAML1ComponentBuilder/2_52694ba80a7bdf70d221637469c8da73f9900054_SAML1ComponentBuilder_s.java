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
 
 package org.apache.ws.security.saml.ext.builder;
 
 import org.apache.ws.security.WSSecurityException;
 import org.apache.ws.security.saml.ext.OpenSAMLUtil;
 import org.apache.ws.security.saml.ext.bean.ActionBean;
 import org.apache.ws.security.saml.ext.bean.AttributeBean;
 import org.apache.ws.security.saml.ext.bean.AttributeStatementBean;
 import org.apache.ws.security.saml.ext.bean.AuthDecisionStatementBean;
 import org.apache.ws.security.saml.ext.bean.AuthenticationStatementBean;
 import org.apache.ws.security.saml.ext.bean.ConditionsBean;
 import org.apache.ws.security.saml.ext.bean.KeyInfoBean;
 import org.apache.ws.security.saml.ext.bean.SubjectBean;
 import org.apache.ws.security.util.UUIDGenerator;
 
 import org.joda.time.DateTime;
 import org.opensaml.Configuration;
 import org.opensaml.common.SAMLObjectBuilder;
 import org.opensaml.common.SAMLVersion;
 
 import org.opensaml.saml1.core.Action;
 import org.opensaml.saml1.core.Assertion;
 import org.opensaml.saml1.core.Attribute;
 import org.opensaml.saml1.core.AttributeStatement;
 import org.opensaml.saml1.core.AttributeValue;
 import org.opensaml.saml1.core.AuthenticationStatement;
 import org.opensaml.saml1.core.AuthorizationDecisionStatement;
 import org.opensaml.saml1.core.Conditions;
 import org.opensaml.saml1.core.ConfirmationMethod;
 import org.opensaml.saml1.core.DecisionTypeEnumeration;
 import org.opensaml.saml1.core.NameIdentifier;
 import org.opensaml.saml1.core.Subject;
 import org.opensaml.saml1.core.SubjectConfirmation;
 
 import org.opensaml.xml.XMLObjectBuilderFactory;
 import org.opensaml.xml.schema.XSString;
 import org.opensaml.xml.schema.impl.XSStringBuilder;
 import org.opensaml.xml.security.x509.BasicX509Credential;
 import org.opensaml.xml.security.x509.X509KeyInfoGeneratorFactory;
 import org.opensaml.xml.signature.KeyInfo;
 
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * Class SAML1ComponentBuilder provides builder methods that can be used
  * to construct SAML v1.1 statements using the OpenSaml library.
  * <p/>
  * Created on May 18, 2009
  */
 public class SAML1ComponentBuilder {
     
     private static SAMLObjectBuilder<Assertion> assertionV1Builder;
     
     private static SAMLObjectBuilder<Conditions> conditionsV1Builder;
     
     private static SAMLObjectBuilder<AuthenticationStatement> authenticationStatementV1Builder;
     
     private static SAMLObjectBuilder<Subject> subjectV1Builder;
     
     private static SAMLObjectBuilder<NameIdentifier> nameIdentifierV1Builder;
     
     private static SAMLObjectBuilder<SubjectConfirmation> 
         subjectConfirmationV1Builder;
     
     private static SAMLObjectBuilder<ConfirmationMethod> confirmationMethodV1Builder;
     
     private static SAMLObjectBuilder<AttributeStatement> 
         attributeStatementV1Builder;
     
     private static SAMLObjectBuilder<Attribute> attributeV1Builder;
     
     private static XSStringBuilder stringBuilder;
     
     private static SAMLObjectBuilder<AuthorizationDecisionStatement> 
         authorizationDecisionStatementV1Builder;
     
     private static SAMLObjectBuilder<Action> actionElementV1Builder;
     
     private static XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
 
     /**
      * Create a new SAML 1.1 assertion
      *
      * @param issuer of type String
      * @return A SAML 1.1 assertion
      */
     @SuppressWarnings("unchecked")
     public static Assertion createSamlv1Assertion(String issuer) {
         if (assertionV1Builder == null) {
             assertionV1Builder = (SAMLObjectBuilder<Assertion>) 
                 builderFactory.getBuilder(Assertion.DEFAULT_ELEMENT_NAME);
             if (assertionV1Builder == null) {
                 throw new IllegalStateException(
                     "OpenSaml engine not initialized. Please make sure to initialize the OpenSaml "
                     + "engine prior using it"
                 );
             }
         }
         Assertion assertion = 
             assertionV1Builder.buildObject(
                 Assertion.DEFAULT_ELEMENT_NAME, 
                 Assertion.TYPE_NAME
             );
         assertion.setVersion(SAMLVersion.VERSION_11);
         assertion.setIssuer(issuer);
         assertion.setIssueInstant(new DateTime()); // now
         assertion.setID(UUIDGenerator.getUUID());
         return assertion;
     }
 
 
     /**
      * Create a SAML Subject from a SubjectBean instance
      *
      * @param subjectBean A SubjectBean instance
      * @return A Saml 1.1 subject
      */
     @SuppressWarnings("unchecked")
     public static Subject createSaml1v1Subject(SubjectBean subjectBean) 
         throws org.opensaml.xml.security.SecurityException, WSSecurityException {
         if (subjectV1Builder == null) {
             subjectV1Builder = (SAMLObjectBuilder<Subject>) 
                 builderFactory.getBuilder(Subject.DEFAULT_ELEMENT_NAME);
         }
         if (nameIdentifierV1Builder == null) {
             nameIdentifierV1Builder = (SAMLObjectBuilder<NameIdentifier>)
                 builderFactory.getBuilder(NameIdentifier.DEFAULT_ELEMENT_NAME);
         }
         if (subjectConfirmationV1Builder == null) {
             subjectConfirmationV1Builder = (SAMLObjectBuilder<SubjectConfirmation>)
                 builderFactory.getBuilder(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
             
         }
         if (confirmationMethodV1Builder == null) {
             confirmationMethodV1Builder = (SAMLObjectBuilder<ConfirmationMethod>)
                 builderFactory.getBuilder(ConfirmationMethod.DEFAULT_ELEMENT_NAME);
         }
         
         Subject subject = subjectV1Builder.buildObject();
         NameIdentifier nameIdentifier = nameIdentifierV1Builder.buildObject();
         SubjectConfirmation subjectConfirmation = subjectConfirmationV1Builder.buildObject();
         ConfirmationMethod confirmationMethod = confirmationMethodV1Builder.buildObject();
         
         nameIdentifier.setNameQualifier(subjectBean.getSubjectNameQualifier());
         nameIdentifier.setNameIdentifier(subjectBean.getSubjectName());
         nameIdentifier.setFormat(SAML1Constants.NAMEID_FORMAT_UNSPECIFIED);
         String confirmationMethodStr = subjectBean.getSubjectConfirmationMethod();
         
         if (confirmationMethodStr == null) {
             confirmationMethodStr = SAML1Constants.CONF_SENDER_VOUCHES;
         }
         
         confirmationMethod.setConfirmationMethod(confirmationMethodStr);
         subjectConfirmation.getConfirmationMethods().add(confirmationMethod);
         if (subjectBean.getKeyInfo() != null) {
             KeyInfo keyInfo = createKeyInfo(subjectBean.getKeyInfo());
             subjectConfirmation.setKeyInfo(keyInfo);
         }
         subject.setNameIdentifier(nameIdentifier);
         subject.setSubjectConfirmation(subjectConfirmation);
         
         return subject;
     }
     
     /**
      * Create an Opensaml KeyInfo object from the parameters
      * @param keyInfo the KeyInfo bean from which to extract security credentials
      * @return the KeyInfo object
      * @throws org.opensaml.xml.security.SecurityException
      */
     public static KeyInfo createKeyInfo(KeyInfoBean keyInfo) 
         throws org.opensaml.xml.security.SecurityException, WSSecurityException {
         if (keyInfo.getElement() != null) {
             return (KeyInfo)OpenSAMLUtil.fromDom(keyInfo.getElement());
         } else {
             // Set the certificate or public key
             BasicX509Credential keyInfoCredential = new BasicX509Credential();
             if (keyInfo.getCertificate() != null) {
                 keyInfoCredential.setEntityCertificate(keyInfo.getCertificate());
             } else if (keyInfo.getPublicKey() != null) {
                 keyInfoCredential.setPublicKey(keyInfo.getPublicKey());
             }
             
             // Configure how to emit the certificate
             X509KeyInfoGeneratorFactory kiFactory = new X509KeyInfoGeneratorFactory();
             KeyInfoBean.CERT_IDENTIFIER certIdentifier = keyInfo.getCertIdentifer();
             switch (certIdentifier) {
                 case X509_CERT: {
                     kiFactory.setEmitEntityCertificate(true);
                     break;
                 }
                 case KEY_VALUE: {
                     kiFactory.setEmitPublicKeyValue(true);
                     break;
                 }
                 case X509_ISSUER_SERIAL: {
                     kiFactory.setEmitX509IssuerSerial(true);
                 }
             }
             return kiFactory.newInstance().generate(keyInfoCredential);
         }
     }
 
     /**
      * Create a Conditions object
      *
      * @param conditionsBean A ConditionsBean object
      * @return a Conditions object
      */
     @SuppressWarnings("unchecked")
     public static Conditions createSamlv1Conditions(ConditionsBean conditionsBean) {
         if (conditionsV1Builder == null) {
             conditionsV1Builder = (SAMLObjectBuilder<Conditions>) 
                 builderFactory.getBuilder(Conditions.DEFAULT_ELEMENT_NAME);
             
         }
        Conditions conditions = conditionsV1Builder.buildObject(Conditions.TYPE_NAME);
         
         if (conditionsBean == null) {
             DateTime newNotBefore = new DateTime();
             conditions.setNotBefore(newNotBefore);
             conditions.setNotOnOrAfter(newNotBefore.plusMinutes(5));
             return conditions;
         }
         
         int tokenPeriodMinutes = conditionsBean.getTokenPeriodMinutes();
         DateTime notBefore = conditionsBean.getNotBefore();
         DateTime notAfter = conditionsBean.getNotAfter();
         
         if (notBefore != null && notAfter != null) {
             if (notBefore.isAfter(notAfter)) {
                 throw new IllegalStateException(
                     "The value of notBefore may not be after the value of notAfter"
                 );
             }
             conditions.setNotBefore(notBefore);
             conditions.setNotOnOrAfter(notAfter);
         } else {
             DateTime newNotBefore = new DateTime();
             conditions.setNotBefore(newNotBefore);
             conditions.setNotOnOrAfter(newNotBefore.plusMinutes(tokenPeriodMinutes));
         }
         return conditions;
     }
 
     /**
      * Create SAML 1.1 authentication statement(s)
      *
      * @param authBeans A list of AuthenticationStatementBean objects
      * @return a list of SAML 1.1 authentication statement(s)
      */
     @SuppressWarnings("unchecked")
     public static List<AuthenticationStatement> createSamlv1AuthenticationStatement(
         List<AuthenticationStatementBean> authBeans
     ) throws org.opensaml.xml.security.SecurityException, WSSecurityException {
         List<AuthenticationStatement> authenticationStatements = 
             new ArrayList<AuthenticationStatement>();
         
         if (authenticationStatementV1Builder == null) {
             authenticationStatementV1Builder = (SAMLObjectBuilder<AuthenticationStatement>) 
                 builderFactory.getBuilder(AuthenticationStatement.DEFAULT_ELEMENT_NAME);
         }
 
         if (authBeans != null && authBeans.size() > 0) {
             for (AuthenticationStatementBean statementBean : authBeans) {
                 AuthenticationStatement authenticationStatement = 
                     authenticationStatementV1Builder.buildObject(
                         AuthenticationStatement.DEFAULT_ELEMENT_NAME, 
                         AuthenticationStatement.TYPE_NAME
                     );
                 Subject authSubject = 
                     SAML1ComponentBuilder.createSaml1v1Subject(statementBean.getSubject());
                 authenticationStatement.setSubject(authSubject);
 
                 if (statementBean.getAuthenticationInstant() != null) {
                     authenticationStatement.setAuthenticationInstant(
                         statementBean.getAuthenticationInstant()
                     );
                 } else {
                     authenticationStatement.setAuthenticationInstant(new DateTime());
                 }
 
                 authenticationStatement.setAuthenticationMethod(
                     transformAuthenticationMethod(statementBean.getAuthenticationMethod())
                 );
                 authenticationStatements.add(authenticationStatement);
             }
         }
 
         return authenticationStatements;
     }
 
     /**
      * Method transformAuthenticationMethod transforms the user-supplied authentication method 
      * value into one of the supported specification-compliant values.
      * NOTE: Only "Password" is supported at this time.
      *
      * @param sourceMethod of type String
      * @return String
      */
     private static String transformAuthenticationMethod(String sourceMethod) {
         String transformedMethod = "";
 
         if ("Password".equals(sourceMethod)) {
             transformedMethod = SAML1Constants.AUTH_METHOD_PASSWORD;
         }
 
         return transformedMethod;
     }
 
     /**
      * Create SAML 1.1 attribute statement(s)
      *
      * @param attributeData A list of AttributeStatementBean instances
      * @return a list of SAML 1.1 attribute statement(s)
      */
     @SuppressWarnings("unchecked")
     public static List<AttributeStatement> createSamlv1AttributeStatement(
         List<AttributeStatementBean> attributeData
     ) throws org.opensaml.xml.security.SecurityException, WSSecurityException {
         if (attributeStatementV1Builder == null) {
             attributeStatementV1Builder = (SAMLObjectBuilder<AttributeStatement>) 
                 builderFactory.getBuilder(AttributeStatement.DEFAULT_ELEMENT_NAME);
         }
 
         List<AttributeStatement> attributeStatements = new ArrayList<AttributeStatement>();
 
         if (attributeData != null && attributeData.size() > 0) {
             for (AttributeStatementBean statementBean : attributeData) {
                 // Create the attribute statementBean and set the subject
                 AttributeStatement attributeStatement = attributeStatementV1Builder.buildObject();
                 Subject attributeSubject = 
                     SAML1ComponentBuilder.createSaml1v1Subject(statementBean.getSubject());
                 attributeStatement.setSubject(attributeSubject);
                 // Add the individual attributes
                 for (AttributeBean values : statementBean.getSamlAttributes()) {
                     Attribute samlAttribute = 
                         createSamlv1Attribute(
                             values.getSimpleName(),
                             values.getQualifiedName(), 
                             values.getAttributeValues()
                         );
                     attributeStatement.getAttributes().add(samlAttribute);
                 }
                 // Add the completed attribute statementBean to the collection
                 attributeStatements.add(attributeStatement);
             }
         }
 
         return attributeStatements;
     }
 
     /**
      * Create a SAML 1.1 attribute
      *
      * @param attributeName the Attribute Name
      * @param attributeUrn the Attribute Qualified Name
      * @param values the Attribute Values
      * @return a SAML 1.1 attribute
      */
     @SuppressWarnings("unchecked")
     public static Attribute createSamlv1Attribute(
         String attributeName, 
         String attributeUrn,
         List<String> values
     ) {
         if (attributeV1Builder == null) {
             attributeV1Builder = (SAMLObjectBuilder<Attribute>) 
                 builderFactory.getBuilder(Attribute.DEFAULT_ELEMENT_NAME);
         }
         if (stringBuilder == null) {
             stringBuilder = (XSStringBuilder)builderFactory.getBuilder(XSString.TYPE_NAME);
         }
 
         Attribute attribute = attributeV1Builder.buildObject();
         attribute.setAttributeName(attributeName);
         attribute.setAttributeNamespace(attributeUrn);
         
         for (String value : values) {
             XSString attribute1 = 
                 stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
             attribute1.setValue(value);
             attribute.getAttributeValues().add(attribute1);
         }
 
         return attribute;
     }
 
     /**
      * Create SAML 1.1 Authorization Decision Statement(s)
      *
      * @param decisionData        of type List<AuthDecisionStatementBean>
      * @return a list of SAML 1.1 Authorization Decision Statement(s)
      */
     @SuppressWarnings("unchecked")
     public static List<AuthorizationDecisionStatement> createSamlv1AuthorizationDecisionStatement(
             List<AuthDecisionStatementBean> decisionData) 
         throws org.opensaml.xml.security.SecurityException, WSSecurityException {
         List<AuthorizationDecisionStatement> authDecisionStatements = new ArrayList();
         if (authorizationDecisionStatementV1Builder == null) {
             authorizationDecisionStatementV1Builder = 
                 (SAMLObjectBuilder<AuthorizationDecisionStatement>) 
                     builderFactory.getBuilder(AuthorizationDecisionStatement.DEFAULT_ELEMENT_NAME);
             
         }
 
         if (decisionData != null && decisionData.size() > 0) {
             for (AuthDecisionStatementBean decisionStatementBean : decisionData) {
                 AuthorizationDecisionStatement authDecision = 
                     authorizationDecisionStatementV1Builder.buildObject();
                 Subject authDecisionSubject = 
                     SAML1ComponentBuilder.createSaml1v1Subject(decisionStatementBean.getSubject());
                 authDecision.setSubject(authDecisionSubject);
 
                 authDecision.setResource(decisionStatementBean.getResource());
                 authDecision.setDecision(transformDecisionType(decisionStatementBean.getDecision()));
 
                 for (ActionBean actionBean : decisionStatementBean.getActions()) {
                     Action actionElement = createSamlv1Action(actionBean);
                     authDecision.getActions().add(actionElement);
                 }
                 authDecisionStatements.add(authDecision);
             }
         }
 
         return authDecisionStatements;
     }
 
     /**
      * Create an Action object
      *
      * @param actionBean of type SamlAction
      * @return an Action object
      */
     @SuppressWarnings("unchecked")
     public static Action createSamlv1Action(ActionBean actionBean) {
         if (actionElementV1Builder == null) {
             actionElementV1Builder = (SAMLObjectBuilder<Action>)
                 builderFactory.getBuilder(Action.DEFAULT_ELEMENT_NAME);
         }
 
         Action actionElement = actionElementV1Builder.buildObject();
         actionElement.setNamespace(actionBean.getActionNamespace());
         actionElement.setContents(actionBean.getContents());
 
         return actionElement;
     }
 
     /**
      * Transform a DecisionType
      *
      * @param decision of type Decision
      * @return DecisionTypeEnumeration
      */
     private static DecisionTypeEnumeration transformDecisionType(
         AuthDecisionStatementBean.Decision decision
     ) {
         DecisionTypeEnumeration decisionTypeEnum = DecisionTypeEnumeration.DENY;
         if (decision.equals(AuthDecisionStatementBean.Decision.PERMIT)) {
             decisionTypeEnum = DecisionTypeEnumeration.PERMIT;
         } else if (decision.equals(AuthDecisionStatementBean.Decision.INDETERMINATE)) {
             decisionTypeEnum = DecisionTypeEnumeration.INDETERMINATE;
         }
 
         return decisionTypeEnum;
     }
 
 }
