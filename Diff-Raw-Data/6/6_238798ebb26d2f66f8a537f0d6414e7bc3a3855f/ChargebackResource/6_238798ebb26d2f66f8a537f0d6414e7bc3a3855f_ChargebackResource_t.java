 /*
  * Copyright 2010-2011 Ning, Inc.
  *
  * Ning licenses this file to you under the Apache License, version 2.0
  * (the "License"); you may not use this file except in compliance with the
  * License.  You may obtain a copy of the License at:
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
  * License for the specific language governing permissions and limitations
  * under the License.
  */
 
 package com.ning.billing.jaxrs.resources;
 
 
 import javax.ws.rs.Consumes;
 import javax.ws.rs.GET;
 import javax.ws.rs.HeaderParam;
 import javax.ws.rs.POST;
 import javax.ws.rs.Path;
 import javax.ws.rs.PathParam;
 import javax.ws.rs.Produces;
 import javax.ws.rs.core.Response;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.List;
 import java.util.UUID;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.google.common.base.Predicate;
 import com.google.common.collect.Collections2;
 import com.google.inject.Inject;
 import com.google.inject.Singleton;
 import com.ning.billing.invoice.api.InvoiceApiException;
 import com.ning.billing.invoice.api.InvoicePayment;
 import com.ning.billing.invoice.api.InvoicePaymentApi;
 import com.ning.billing.jaxrs.json.ChargebackCollectionJson;
 import com.ning.billing.jaxrs.json.ChargebackJson;
 import com.ning.billing.jaxrs.util.Context;
 import com.ning.billing.jaxrs.util.JaxrsUriBuilder;
 import com.ning.billing.payment.api.Payment;
 import com.ning.billing.payment.api.Payment.PaymentAttempt;
 import com.ning.billing.payment.api.PaymentApi;
 import com.ning.billing.payment.api.PaymentApiException;
 import com.ning.billing.payment.api.PaymentStatus;
 
 import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
 
 @Singleton
 @Path(JaxrsResource.CHARGEBACKS_PATH)
 public class ChargebackResource implements JaxrsResource {
     private static final Logger log = LoggerFactory.getLogger(ChargebackResource.class);
 
     private final JaxrsUriBuilder uriBuilder;
     private final InvoicePaymentApi invoicePaymentApi;
     private final Context context;
 
     @Inject
     public ChargebackResource(final JaxrsUriBuilder uriBuilder,
                               final InvoicePaymentApi invoicePaymentApi,
                               final PaymentApi paymentApi,
                               final Context context) {
         this.uriBuilder = uriBuilder;
         this.invoicePaymentApi = invoicePaymentApi;
         this.context = context;
     }
 
     @GET
     @Path("/{chargebackId:" + UUID_PATTERN + "}")
     @Produces(APPLICATION_JSON)
     public Response getChargeback(@PathParam("chargebackId") final String chargebackId) {
         try {
             final InvoicePayment chargeback = invoicePaymentApi.getChargebackById(UUID.fromString(chargebackId));
             final ChargebackJson chargebackJson = new ChargebackJson(chargeback);
 
             return Response.status(Response.Status.OK).entity(chargebackJson).build();
         } catch (InvoiceApiException e) {
             final String error = String.format("Failed to locate chargeback for id %s", chargebackId);
             log.info(error, e);
             return Response.status(Response.Status.NO_CONTENT).build();
         }
     }
 
     @GET
     @Path("/accounts/{accountId:" + UUID_PATTERN + "}")
     @Produces(APPLICATION_JSON)
     public Response getForAccount(@PathParam("accountId") final String accountId) {
         final List<InvoicePayment> chargebacks = invoicePaymentApi.getChargebacksByAccountId(UUID.fromString(accountId));
         final List<ChargebackJson> chargebacksJson = convertToJson(chargebacks);
 
         final ChargebackCollectionJson json = new ChargebackCollectionJson(accountId, chargebacksJson);
         return Response.status(Response.Status.OK).entity(json).build();
     }
 
     @GET
     @Path("/payments/{paymentId:" + UUID_PATTERN + "}")
     @Produces(APPLICATION_JSON)
     public Response getForPayment(@PathParam("paymentId") final String paymentId) {
 
         try {
             final List<InvoicePayment> chargebacks = invoicePaymentApi.getChargebacksByPaymentId(UUID.fromString(paymentId));
             if (chargebacks.size() == 0) {
                 return Response.status(Response.Status.NO_CONTENT).build();
             }
 
             final UUID invoicePaymentId = chargebacks.get(0).getId();
             final String accountId = invoicePaymentApi.getAccountIdFromInvoicePaymentId(invoicePaymentId).toString();
             final List<ChargebackJson> chargebacksJson = convertToJson(chargebacks);
             final ChargebackCollectionJson json = new ChargebackCollectionJson(accountId, chargebacksJson);
 
             return Response.status(Response.Status.OK).entity(json).build();
         } catch (InvoiceApiException e) {
             final String error = String.format("Failed to locate account for payment id %s", paymentId);
             return Response.status(Response.Status.NO_CONTENT).entity(error).build();
         }
     }
 
     @POST
     @Consumes(APPLICATION_JSON)
     @Produces(APPLICATION_JSON)
     public Response createChargeback(final ChargebackJson json,
                                      @HeaderParam(HDR_CREATED_BY) final String createdBy,
                                      @HeaderParam(HDR_REASON) final String reason,
                                      @HeaderParam(HDR_COMMENT) final String comment) {
         try {
             final InvoicePayment invoicePayment = invoicePaymentApi.getInvoicePayment(UUID.fromString(json.getPaymentId()));
             if (invoicePayment == null) {
                 final String error = String.format("Failed to locate invoice payment for paymentAttemptId %s", json.getPaymentId());
                return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
             }
 
             final InvoicePayment chargeBack = invoicePaymentApi.createChargeback(invoicePayment.getId(), json.getChargebackAmount(),
                                                                                   context.createContext(createdBy, reason, comment));
             return uriBuilder.buildResponse(ChargebackResource.class, "getChargeback", chargeBack.getId());
         } catch (InvoiceApiException e) {
             final String error = String.format("Failed to create chargeback %s", json);
             log.info(error, e);
             return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
         } catch (IllegalArgumentException e) {
             return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
         }
     }
 
     private List<ChargebackJson> convertToJson(final List<InvoicePayment> chargebacks) {
         final List<ChargebackJson> result = new ArrayList<ChargebackJson>();
         for (final InvoicePayment chargeback : chargebacks) {
             result.add(new ChargebackJson(chargeback));
         }
 
         return result;
     }
 }
