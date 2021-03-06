 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 
 package org.motechproject.mobile.omp.manager;
 
 import org.motechproject.mobile.core.manager.CoreManager;
 import org.motechproject.mobile.core.model.GatewayRequest;
 import org.motechproject.mobile.core.model.GatewayResponse;
 import org.motechproject.mobile.core.model.MStatus;
 import org.motechproject.mobile.core.service.MotechContext;
 import java.util.Date;
 import java.util.Set;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Map.Entry;
 
 /**
  *
  * @author Kofi A. Asamoah (yoofi@dreamoval.com)
  * Date Created: Jul 31, 2009
  */
 public class DummyGatewayMessageHandlerImpl implements GatewayMessageHandler {
     CoreManager coreManager;
     private Map<MStatus, String> codeStatusMap;
     private Map<MStatus, String> codeResponseMap;
 
     public GatewayRequest prepareMessage(String message) {
         return coreManager.createGatewayRequest(coreManager.createMotechContext());
     }
 
     public Set<GatewayResponse> parseMessageResponse(GatewayRequest message, String gatewayResponse, MotechContext context) {
         if(message == null)
             return null;
         
         if(gatewayResponse.isEmpty())
             return null;
 
         String[] respParts = gatewayResponse.trim().split(" ");
 
         Set<GatewayResponse> responseList = new HashSet<GatewayResponse>();
         GatewayResponse response = coreManager.createGatewayResponse(context);
         response.setGatewayRequest(message);
         response.setMessageStatus(MStatus.DELIVERED);
         response.setRecipientNumber(message.getRecipientsNumber());
        response.setGatewayMessageId(respParts[1]);
         response.setRequestId(message.getRequestId());
         response.setResponseText(gatewayResponse);
         response.setDateCreated(new Date());
 
         responseList.add(response);
         return responseList;
     }
 
     public MStatus parseMessageStatus(String gatewayResponse) {
         String status;
 
         String[] responseParts = gatewayResponse.split(" ");
 
         if(responseParts.length == 4){
             status = responseParts[3];
         }
         else{
             status = "";
         }
 
         return lookupStatus(status);
     }
 
     public MStatus lookupStatus(String code) {
         if(code.isEmpty()){
             return MStatus.PENDING;
         }
 
         for(Entry<MStatus, String> entry: codeStatusMap.entrySet()){
             if(entry.getValue().contains(code)){
                 return entry.getKey();
             }
         }
         return MStatus.PENDING;
     }
 
     public MStatus lookupResponse(String code) {
         if(code.isEmpty()){
             return MStatus.SCHEDULED;
         }
 
         for(Entry<MStatus, String> entry: codeResponseMap.entrySet()){
             if(entry.getValue().contains(code)){
                 return entry.getKey();
             }
         }
         return MStatus.SCHEDULED;
     }
 
     public CoreManager getCoreManager() {
         return this.coreManager;
     }
 
     public void setCoreManager(CoreManager coreManager) {
         this.coreManager = coreManager;
     }
 
     /**
      * @param codeStatusMap the codeStatusMap to set
      */
     public void setCodeStatusMap(Map<MStatus, String> codeStatusMap) {
         this.codeStatusMap = codeStatusMap;
     }
 
     /**
      * @param codeResponseMap the codeResponseMap to set
      */
     public void setCodeResponseMap(Map<MStatus, String> codeResponseMap) {
         this.codeResponseMap = codeResponseMap;
     }
 
 }
