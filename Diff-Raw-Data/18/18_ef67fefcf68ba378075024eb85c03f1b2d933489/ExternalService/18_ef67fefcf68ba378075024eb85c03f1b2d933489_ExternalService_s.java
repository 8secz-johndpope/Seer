 /*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 
 package org.apache.ode.axis2;
 
 import org.apache.axiom.soap.SOAPEnvelope;
 import org.apache.axis2.AxisFault;
 import org.apache.axis2.addressing.EndpointReference;
 import org.apache.axis2.client.OperationClient;
 import org.apache.axis2.client.Options;
 import org.apache.axis2.client.ServiceClient;
 import org.apache.axis2.context.ConfigurationContext;
 import org.apache.axis2.context.MessageContext;
 import org.apache.axis2.engine.AxisConfiguration;
 import org.apache.axis2.wsdl.WSDLConstants;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.apache.ode.axis2.util.SoapMessageConverter;
 import org.apache.ode.bpel.epr.EndpointFactory;
 import org.apache.ode.bpel.epr.MutableEndpoint;
 import org.apache.ode.bpel.epr.WSAEndpoint;
 import org.apache.ode.bpel.iapi.BpelServer;
 import org.apache.ode.bpel.iapi.Message;
 import org.apache.ode.bpel.iapi.MessageExchange;
 import org.apache.ode.bpel.iapi.MessageExchange.FailureType;
 import org.apache.ode.bpel.iapi.PartnerRoleChannel;
 import org.apache.ode.bpel.iapi.PartnerRoleMessageExchange;
 import org.apache.ode.bpel.iapi.Scheduler;
 import org.apache.ode.utils.DOMUtils;
 import org.w3c.dom.Document;
 import org.w3c.dom.Element;
 
 import javax.wsdl.Definition;
 import javax.wsdl.Operation;
 import javax.xml.namespace.QName;
 import java.util.concurrent.Callable;
 import java.util.concurrent.ExecutorService;
 
 /**
  * Acts as a service not provided by ODE. Used mainly for invocation as a way to maintain the WSDL decription of used
  * services.
  *
  * @author Matthieu Riou <mriou at apache dot org>
  */
 public class ExternalService implements PartnerRoleChannel {
 
     private static final Log __log = LogFactory.getLog(ExternalService.class);
 
     private ExecutorService _executorService;
     private Definition _definition;
     private QName _serviceName;
     private String _portName;
     private AxisConfiguration _axisConfig;
     private boolean _isReplicateEmptyNS = false;
     private SoapMessageConverter _converter;
     private Scheduler _sched;
     private BpelServer _server;
 
     public ExternalService(Definition definition, QName serviceName, String portName, ExecutorService executorService,
             AxisConfiguration axisConfig, Scheduler sched, BpelServer server) throws AxisFault {
         _definition = definition;
         _serviceName = serviceName;
         _portName = portName;
         _executorService = executorService;
         _axisConfig = axisConfig;
         _sched = sched;
         _converter = new SoapMessageConverter(definition, serviceName, portName, _isReplicateEmptyNS);
         _server = server;
     }
 
     public void invoke(final PartnerRoleMessageExchange odeMex) {
         boolean isTwoWay = odeMex.getMessageExchangePattern() == org.apache.ode.bpel.iapi.MessageExchange.MessageExchangePattern.REQUEST_RESPONSE;
         try {
             // Override options are passed to the axis MessageContext so we can
             // retrieve them in our session out handler.
             MessageContext mctx = new MessageContext();
             writeHeader(mctx.getOptions(), odeMex);
 
             _converter.createSoapRequest(mctx, odeMex.getRequest().getMessage(), odeMex.getOperation());
 
             SOAPEnvelope soapEnv = mctx.getEnvelope();
             EndpointReference axisEPR = new EndpointReference(((MutableEndpoint) odeMex.getEndpointReference())
                     .getUrl());
             if (__log.isDebugEnabled()) {
                 __log.debug("Axis2 sending message to " + axisEPR.getAddress() + " using MEX " + odeMex);
                 __log.debug("Message: " + soapEnv);
             }
 
             Options options = new Options();
             options.setTo(axisEPR);
             String soapAction = _converter.getSoapAction(odeMex.getOperationName());
             options.setAction(soapAction);
             options.setTimeOutInMilliSeconds(60000);
 
             ConfigurationContext ctx = new ConfigurationContext(_axisConfig);
             ServiceClient sclient = new ServiceClient(ctx, null);
             final OperationClient operationClient = sclient.createClient(isTwoWay ? ServiceClient.ANON_OUT_IN_OP
                     : ServiceClient.ANON_OUT_ONLY_OP);
             operationClient.setOptions(options);
 
             operationClient.addMessageContext(mctx);
 
             if (isTwoWay) {
                 final String mexId = odeMex.getMessageExchangeId();
                 final Operation operation = odeMex.getOperation();
 
                 // Defer the invoke until the transaction commits.
                 _sched.registerSynchronizer(new Scheduler.Synchronizer() {
 
                     public void afterCompletion(boolean success) {
                         // If the TX is rolled back, then we don't send the request.
                         if (!success)
                             return;
 
                         // The invocation must happen in a separate thread, holding on the afterCompletion
                         // blocks other operations that could have been listed there as well.
                         _executorService.submit(new Callable<Object>() {
                             public Object call() throws Exception {
                                 try {
                                     operationClient.execute(true);
                                     MessageContext response = operationClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
                                     MessageContext flt = operationClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_FAULT_VALUE);
                                     if (flt != null) {
                                         reply(mexId, operation, flt, true);
                                     } else {
                                         reply(mexId, operation, response, false);
                                     }
                                 } catch (Throwable t) {
                                    String errmsg = "Error sending message to Axis2 for ODE mex " + odeMex;
                                     __log.error(errmsg, t);
                                     replyWithFailure(mexId, MessageExchange.FailureType.COMMUNICATION_ERROR, errmsg, null);
                                 }
                                 return null;
                             }
                         });
                     }
 
                     public void beforeCompletion() {
                     }
 
                 });
                 odeMex.replyAsync();
 
             } else /** one-way case * */
             {
                 _executorService.submit(new Callable<Object>() {
                     public Object call() throws Exception {
                         operationClient.execute(false);
                         return null;
                     }
                 });
                 odeMex.replyOneWayOk();
             }
         } catch (AxisFault axisFault) {
             String errmsg = "Error sending message to Axis2 for ODE mex " + odeMex;
             __log.error(errmsg, axisFault);
             odeMex.replyWithFailure(MessageExchange.FailureType.COMMUNICATION_ERROR, errmsg, null);
         }
 
     }
 
     /**
      * Extracts endpoint information from ODE message exchange to stuff them into Axis MessageContext.
      */
     private void writeHeader(Options options, PartnerRoleMessageExchange odeMex) {
         WSAEndpoint targetEPR = EndpointFactory.convertToWSA((MutableEndpoint) odeMex.getEndpointReference());
         WSAEndpoint myRoleEPR = EndpointFactory.convertToWSA((MutableEndpoint) odeMex.getMyRoleEndpointReference());
 
         String partnerSessionId = odeMex.getProperty(MessageExchange.PROPERTY_SEP_PARTNERROLE_SESSIONID);
         String myRoleSessionId = odeMex.getProperty(MessageExchange.PROPERTY_SEP_MYROLE_SESSIONID);
 
         if (partnerSessionId != null) {
             if (__log.isDebugEnabled()) {
                 __log.debug("Partner session identifier found for WSA endpoint: " + partnerSessionId);
             }
             targetEPR.setSessionId(partnerSessionId);
         }
         options.setProperty("targetSessionEndpoint", targetEPR);
         String soapAction = _converter.getSoapAction(odeMex.getOperationName());
         options.setProperty("soapAction", soapAction);
 
         if (myRoleEPR != null) {
             if (myRoleSessionId != null) {
                 if (__log.isDebugEnabled()) {
                     __log.debug("MyRole session identifier found for myrole (callback) WSA endpoint: "
                             + myRoleSessionId);
                 }
                 myRoleEPR.setSessionId(myRoleSessionId);
             }
 
             options.setProperty("callbackSessionEndpoint", odeMex.getMyRoleEndpointReference());
         } else {
             __log.debug("My-Role EPR not specified, SEP will not be used.");
         }
     }
 
     public org.apache.ode.bpel.iapi.EndpointReference getInitialEndpointReference() {
         Element eprElmt = ODEService.genEPRfromWSDL(_definition, _serviceName, _portName);
         if (eprElmt == null)
             throw new IllegalArgumentException("Service " + _serviceName + " and port " + _portName
                     + "couldn't be found in provided WSDL document!");
         return EndpointFactory.convertToWSA(ODEService.createServiceRef(eprElmt));
     }
 
     public void close() {
         // nothing
     }
 
     public void setReplicateEmptyNS(boolean isReplicateEmptyNS) {
         _isReplicateEmptyNS = isReplicateEmptyNS;
         try {
             _converter = new SoapMessageConverter(_definition, _serviceName, _portName, _isReplicateEmptyNS);
         } catch (Exception ex) {
             throw new RuntimeException(ex);
         }
     }
 
     public String getPortName() {
         return _portName;
     }
 
     public QName getServiceName() {
         return _serviceName;
     }
 
     private void replyWithFailure(final String odeMexId, final FailureType error, final String errmsg,
             final Element details) {
         // ODE MEX needs to be invoked in a TX.
        final PartnerRoleMessageExchange odeMex =
                (PartnerRoleMessageExchange)  _server.getEngine().getMessageExchange(odeMexId);
         try {
             _sched.execIsolatedTransaction(new Callable<Void>() {
                 public Void call() throws Exception {
                     odeMex.replyWithFailure(error, errmsg, details);
                     return null;
                 }
             });
 
         } catch (Exception e) {
             String emsg = "Error executing replyWithFailure transaction; reply will be lost.";
             __log.error(emsg, e);
 
         }
 
     }
 
     private void reply(final String odeMexId, final Operation operation, final MessageContext reply, final boolean fault) {
         final Document odeMsg = DOMUtils.newDocument();
         final Element odeMsgEl = odeMsg.createElementNS(null, "message");
         odeMsg.appendChild(odeMsgEl);
 
         final QName faultType;
         try {
             if (fault) {
                 faultType = _converter.parseSoapFault(odeMsgEl, reply.getEnvelope(), operation);
             } else {
                 faultType = null;
                 _converter.parseSoapResponse(odeMsgEl, reply.getEnvelope(), operation);
             }
         } catch (AxisFault af) {
             replyWithFailure(odeMexId, FailureType.FORMAT_ERROR, af.getMessage(), null);
             return;
         }
 
         // ODE MEX needs to be invoked in a TX.
         try {
             _sched.execIsolatedTransaction(new Callable<Void>() {
                 public Void call() throws Exception {
                     PartnerRoleMessageExchange odeMex = (PartnerRoleMessageExchange)  _server.getEngine().getMessageExchange(odeMexId);
                     Message response = fault ? odeMex.createMessage(odeMex.getOperation().getFault(
                             faultType.getLocalPart()).getMessage().getQName()) : odeMex.createMessage(odeMex
                             .getOperation().getOutput().getMessage().getQName());
                     try {
                         if (__log.isDebugEnabled()) {
                             __log.debug("Received response for MEX " + odeMex);
                         }
                         response.setMessage(odeMsgEl);
                         if (fault) {
                             if (faultType != null) {
                                 if (__log.isDebugEnabled()) {
                                     __log.debug("FAULT RESPONSE(" + faultType + "): " + DOMUtils.domToString(odeMsgEl));
                                 }
                                 odeMex.replyWithFault(faultType, response);
                             } else {
                                 if (__log.isDebugEnabled()) {
                                    __log
                                            .debug("FAULT RESPONSE(unknown fault type): "
                                                    + DOMUtils.domToString(odeMsgEl));
                                 }
                                odeMex.replyWithFailure(FailureType.FORMAT_ERROR, reply.getEnvelope().getBody()
                                         .getFault().getText(), null);
                             }
                         } else {
                             if (__log.isDebugEnabled()) {
                                 __log.debug("RESPONSE (NORMAL): " + DOMUtils.domToString(odeMsgEl));
                             }
                             odeMex.reply(response);
 
                         }
                     } catch (Exception ex) {
                         String errmsg = "Unable to process response: " + ex.getMessage();
                         __log.error(errmsg, ex);
                        odeMex.replyWithFailure(FailureType.FORMAT_ERROR, errmsg, null);
                     }
                     return null;
                 }
             });
 
         } catch (Exception e) {
             String errmsg = "Error executing reply transaction; reply will be lost.";
             __log.error(errmsg, e);
         }
     }
 
 }
