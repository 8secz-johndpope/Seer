 /*
  * Copyright (c) 2005-2010 Grameen Foundation USA
  * All rights reserved.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
  * implied. See the License for the specific language governing
  * permissions and limitations under the License.
  *
  * See also http://www.apache.org/licenses/LICENSE-2.0.html for an
  * explanation of the license and how it is applied.
  */
 
 package org.mifos.customers.client.struts.action;
 
 import org.apache.struts.action.ActionErrors;
 import org.apache.struts.action.ActionForm;
 import org.apache.struts.action.ActionForward;
 import org.apache.struts.action.ActionMapping;
 import org.joda.time.DateTime;
 import org.mifos.application.meeting.business.MeetingBO;
 import org.mifos.application.questionnaire.struts.DefaultQuestionnaireServiceFacadeLocator;
 import org.mifos.application.questionnaire.struts.QuestionnaireAction;
 import org.mifos.application.questionnaire.struts.QuestionnaireFlowAdapter;
 import org.mifos.application.questionnaire.struts.QuestionnaireServiceFacadeLocator;
 import org.mifos.application.servicefacade.ClientDetailDto;
 import org.mifos.application.servicefacade.ClientFamilyDetailsDto;
 import org.mifos.application.servicefacade.ClientFamilyInfoDto;
 import org.mifos.application.servicefacade.ClientFormCreationDto;
 import org.mifos.application.servicefacade.ClientMfiInfoDto;
 import org.mifos.application.servicefacade.ClientPersonalInfoDto;
 import org.mifos.application.servicefacade.ClientRulesDto;
 import org.mifos.application.servicefacade.CustomerDetailsDto;
 import org.mifos.application.servicefacade.ProcessRulesDto;
 import org.mifos.application.util.helpers.ActionForwards;
 import org.mifos.config.ClientRules;
 import org.mifos.config.util.helpers.HiddenMandatoryFieldNamesConstants;
 import org.mifos.customers.business.CustomerBO;
 import org.mifos.customers.business.CustomerCustomFieldEntity;
 import org.mifos.customers.center.util.helpers.CenterConstants;
 import org.mifos.customers.client.business.ClientBO;
 import org.mifos.customers.client.business.ClientFamilyDetailDto;
 import org.mifos.customers.client.business.ClientNameDetailDto;
 import org.mifos.customers.client.business.service.ClientInformationDto;
 import org.mifos.customers.client.struts.actionforms.ClientCustActionForm;
 import org.mifos.customers.client.util.helpers.ClientConstants;
 import org.mifos.customers.group.util.helpers.GroupConstants;
 import org.mifos.customers.struts.action.CustAction;
 import org.mifos.customers.util.helpers.CustomerConstants;
 import org.mifos.customers.util.helpers.SavingsDetailDto;
import org.mifos.customers.util.helpers.CustomerStatus;
 import org.mifos.dto.screen.OnlyBranchOfficeHierarchyDto;
 import org.mifos.framework.components.fieldConfiguration.util.helpers.FieldConfig;
 import org.mifos.framework.exceptions.ApplicationException;
 import org.mifos.framework.exceptions.PageExpiredException;
 import org.mifos.framework.util.helpers.CloseSession;
 import org.mifos.framework.util.helpers.Constants;
 import org.mifos.framework.util.helpers.DateUtils;
 import org.mifos.framework.util.helpers.SessionUtils;
 import org.mifos.framework.util.helpers.TransactionDemarcate;
 import org.mifos.platform.questionnaire.service.QuestionGroupInstanceDetail;
 import org.mifos.platform.questionnaire.service.QuestionnaireServiceFacade;
 import org.mifos.security.util.ActionSecurity;
 import org.mifos.security.util.SecurityConstants;
 import org.mifos.security.util.UserContext;
 import org.mifos.service.MifosServiceFactory;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import java.io.BufferedOutputStream;
 import java.io.InputStream;
 import java.io.UnsupportedEncodingException;
 import java.net.URLEncoder;
 import java.sql.Date;
 import java.util.Calendar;
 import java.util.List;
 import java.util.Map;
 
 import static org.mifos.accounts.loan.util.helpers.LoanConstants.METHODCALLED;
 
 public class ClientCustAction extends CustAction implements QuestionnaireAction {
 
     private QuestionnaireServiceFacadeLocator questionnaireServiceFacadeLocator = new DefaultQuestionnaireServiceFacadeLocator();
 
     private final QuestionnaireFlowAdapter createClientQuestionnaire = new QuestionnaireFlowAdapter("Create", "Client",
             ActionForwards.next_success, "clientCustAction.do?method=cancel", questionnaireServiceFacadeLocator);
 
     public static ActionSecurity getSecurity() {
         ActionSecurity security = new ActionSecurity("clientCustAction");
         security.allow("load", SecurityConstants.VIEW);
         security.allow("chooseOffice", SecurityConstants.VIEW);
         security.allow("next", SecurityConstants.VIEW);
         security.allow("preview", SecurityConstants.VIEW);
         security.allow("previewPersonalInfo", SecurityConstants.VIEW);
         security.allow("retrievePictureOnPreview", SecurityConstants.VIEW);
         security.allow("prevPersonalInfo", SecurityConstants.VIEW);
         security.allow("prevMFIInfo", SecurityConstants.VIEW);
         security.allow("prevMeeting", SecurityConstants.VIEW);
         security.allow("create", SecurityConstants.VIEW);
         security.allow("loadMeeting", SecurityConstants.MEETING_CREATE_CLIENT_MEETING);
         security.allow("get", SecurityConstants.VIEW);
         security.allow("editPersonalInfo", SecurityConstants.CLIENT_UPDATE_PERSONNEL_INFO);
         security.allow("editFamilyInfo", SecurityConstants.CLIENT_UPDATE_PERSONNEL_INFO);
         security.allow("editAddFamilyRow", SecurityConstants.CLIENT_UPDATE_PERSONNEL_INFO);
         security.allow("editDeleteFamilyRow", SecurityConstants.CLIENT_UPDATE_PERSONNEL_INFO);
         security.allow("previewEditFamilyInfo", SecurityConstants.CLIENT_UPDATE_PERSONNEL_INFO);
         security.allow("previewEditPersonalInfo", SecurityConstants.VIEW);
         security.allow("prevEditPersonalInfo", SecurityConstants.VIEW);
         security.allow("updatePersonalInfo", SecurityConstants.CLIENT_UPDATE_PERSONNEL_INFO);
         security.allow("editMfiInfo", SecurityConstants.CIENT_EDIT_MFI_INFORMATION);
         security.allow("previewEditMfiInfo", SecurityConstants.VIEW);
         security.allow("prevEditMfiInfo", SecurityConstants.VIEW);
         security.allow("updateMfiInfo", SecurityConstants.CIENT_EDIT_MFI_INFORMATION);
         security.allow("retrievePicture", SecurityConstants.VIEW);
         security.allow("showPicture", SecurityConstants.VIEW);
         security.allow("loadChangeLog", SecurityConstants.VIEW);
         security.allow("cancelChangeLog", SecurityConstants.VIEW);
         security.allow("familyInfoNext", SecurityConstants.VIEW);
         security.allow("prevFamilyInfo", SecurityConstants.VIEW);
         security.allow("prevFamilyInfoNext", SecurityConstants.VIEW);
         security.allow("addFamilyRow", SecurityConstants.VIEW);
         security.allow("deleteFamilyRow", SecurityConstants.VIEW);
         security.allow("updateFamilyInfo", SecurityConstants.CLIENT_UPDATE_PERSONNEL_INFO);
         security.allow("editPreviewEditFamilyInfo", SecurityConstants.CLIENT_UPDATE_PERSONNEL_INFO);
         security.allow("captureQuestionResponses", SecurityConstants.VIEW);
         security.allow("editQuestionResponses", SecurityConstants.VIEW);
         return security;
     }
 
     @TransactionDemarcate(saveToken = true)
     public ActionForward chooseOffice(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                       @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         actionForm.setGroupFlag(ClientConstants.NO);
 
         UserContext userContext = getUserContext(request);
 
         OnlyBranchOfficeHierarchyDto officeHierarchy = customerServiceFacade
                 .retrieveBranchOnlyOfficeHierarchy(userContext);
 
         SessionUtils.setAttribute(OnlyBranchOfficeHierarchyDto.IDENTIFIER, officeHierarchy, request);
 
         return mapping.findForward(ActionForwards.chooseOffice_success.toString());
     }
 
     @TransactionDemarcate(saveToken = true)
     public ActionForward load(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                               @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
 
         actionForm.clearMostButNotAllFieldsOnActionForm();
         SessionUtils.removeAttribute(CustomerConstants.CUSTOMER_MEETING, request);
 
         Short officeId = actionForm.getOfficeIdValue();
         Short groupFlag = actionForm.getGroupFlagValue();
         String parentGroupId = actionForm.getParentGroupId();
 
         UserContext userContext = getUserContext(request);
         ClientFormCreationDto clientFormCreationDto = this.customerServiceFacade.retrieveClientFormCreationData(userContext, groupFlag, officeId, parentGroupId);
 
         if (clientFormCreationDto.getFormedByPersonnelId() != null) {
             actionForm.setFormedByPersonnel(clientFormCreationDto.getFormedByPersonnelId().toString());
         }
         actionForm.setCenterDisplayName(clientFormCreationDto.getCenterDisplayName());
         actionForm.setGroupDisplayName(clientFormCreationDto.getGroupDisplayName());
         actionForm.setOfficeId(clientFormCreationDto.getOfficeId().toString());
         actionForm.setOfficeName(clientFormCreationDto.getOfficeName());
 
         if (clientFormCreationDto.getFormedByPersonnelId() != null) {
             actionForm.setLoanOfficerId(clientFormCreationDto.getFormedByPersonnelId().toString());
         }
         actionForm.setLoanOfficerName(clientFormCreationDto.getFormedByPersonnelName());
         actionForm.setCustomFields(clientFormCreationDto.getCustomFieldViews());
         actionForm.setDefaultFees(clientFormCreationDto.getApplicableFees().getDefaultFees());
 
         SessionUtils.setCollectionAttribute(ClientConstants.SALUTATION_ENTITY, clientFormCreationDto.getClientDropdowns().getSalutations(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.GENDER_ENTITY, clientFormCreationDto.getClientDropdowns().getGenders(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.MARITAL_STATUS_ENTITY, clientFormCreationDto.getClientDropdowns().getMaritalStatuses(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.CITIZENSHIP_ENTITY, clientFormCreationDto.getClientDropdowns().getCitizenship(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.ETHINICITY_ENTITY, clientFormCreationDto.getClientDropdowns().getEthinicity(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.EDUCATION_LEVEL_ENTITY, clientFormCreationDto.getClientDropdowns().getEducationLevels(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.BUSINESS_ACTIVITIES_ENTITY, clientFormCreationDto.getClientDropdowns().getBusinessActivity(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.POVERTY_STATUS, clientFormCreationDto.getClientDropdowns().getPoverty(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.HANDICAPPED_ENTITY, clientFormCreationDto.getClientDropdowns().getHandicapped(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.SPOUSE_FATHER_ENTITY, clientFormCreationDto.getClientDropdowns().getSpouseFather(), request);
         SessionUtils.setCollectionAttribute(CustomerConstants.CUSTOM_FIELDS_LIST, clientFormCreationDto.getCustomFieldViews(), request);
         SessionUtils.setCollectionAttribute(CustomerConstants.LOAN_OFFICER_LIST, clientFormCreationDto.getPersonnelList(), request);
         SessionUtils.setCollectionAttribute(CustomerConstants.ADDITIONAL_FEES_LIST, clientFormCreationDto.getApplicableFees().getAdditionalFees(), request);
         SessionUtils.setCollectionAttribute(CustomerConstants.FORMEDBY_LOAN_OFFICER_LIST, clientFormCreationDto.getFormedByPersonnelList(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.SAVINGS_OFFERING_LIST, clientFormCreationDto.getSavingsOfferings(), request);
         SessionUtils.setAttribute(GroupConstants.CENTER_HIERARCHY_EXIST, ClientRules.getCenterHierarchyExists(), request);
         SessionUtils.setAttribute(ClientConstants.MAXIMUM_NUMBER_OF_FAMILY_MEMBERS, ClientRules.getMaximumNumberOfFamilyMembers(), request);
         boolean isFamilyDetailsRequired = ClientRules.isFamilyDetailsRequired();
         SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_REQUIRED, isFamilyDetailsRequired, request);
         if (isFamilyDetailsRequired) {
             boolean isFamilyDetailsMandatory = isFamilyDetailsMandatory();
             if (!isFamilyDetailsMandatory) {
                 actionForm.removeFamilyMember(0);
             }
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_MANDATORY, isFamilyDetailsMandatory, request);
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_HIDDEN, false, request);
         } else {
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_MANDATORY, isSpouseFatherInformationMandatory(), request);
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_HIDDEN, isSpouseFatherInformationHidden(), request);
         }
         return mapping.findForward(ActionForwards.load_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward next(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                               @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
 
         ClientFamilyDetailsDto clientFamilyDetails = this.customerServiceFacade.retrieveClientFamilyDetails();
 
         if (clientFamilyDetails.isFamilyDetailsRequired()) {
 
             SessionUtils.setCollectionAttribute(ClientConstants.LIVING_STATUS_ENTITY, clientFamilyDetails.getLivingStatus(), request);
             SessionUtils.setCollectionAttribute(ClientConstants.GENDER_ENTITY, clientFamilyDetails.getGenders(), request);
 
             actionForm.setFamilyDetailBean(clientFamilyDetails.getFamilyDetails());
 
             return mapping.findForward(ActionForwards.next_success_family.toString());
         }
 
         return createClientQuestionnaire.fetchAppliedQuestions(mapping, actionForm, request, ActionForwards.next_success);
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward familyInfoNext(ActionMapping mapping, ActionForm form,
                                         @SuppressWarnings("unused") HttpServletRequest request,
                                         @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         actionForm.setFamilyDateOfBirth();
         actionForm.constructFamilyDetails();
         return createClientQuestionnaire.fetchAppliedQuestions(mapping, actionForm, request, ActionForwards.next_success);
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward addFamilyRow(ActionMapping mapping, ActionForm form,
                                       @SuppressWarnings("unused") HttpServletRequest request,
                                       @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         if (actionForm.getFamilyNames().size() < ClientRules.getMaximumNumberOfFamilyMembers()) {
             actionForm.addFamilyMember();
         }
         return mapping.findForward(ActionForwards.addFamilyRow_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward deleteFamilyRow(ActionMapping mapping, ActionForm form,
                                          @SuppressWarnings("unused") HttpServletRequest request,
                                          @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         if (Integer.parseInt(actionForm.getDeleteThisRow()) < actionForm.getFamilyFirstName().size()) {
             actionForm.removeFamilyMember(Integer.parseInt(actionForm.getDeleteThisRow()));
         }
         return mapping.findForward(ActionForwards.addFamilyRow_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward editAddFamilyRow(ActionMapping mapping, ActionForm form,
                                           @SuppressWarnings("unused") HttpServletRequest request,
                                           @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         if (actionForm.getFamilyNames().size() < ClientRules.getMaximumNumberOfFamilyMembers()) {
             actionForm.addFamilyMember();
         }
         return mapping.findForward(ActionForwards.editAddFamilyRow_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward editDeleteFamilyRow(ActionMapping mapping, ActionForm form,
                                              @SuppressWarnings("unused") HttpServletRequest request,
                                              @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         if (Integer.parseInt(actionForm.getDeleteThisRow()) < actionForm.getFamilyFirstName().size()) {
             actionForm.removeFamilyMember(Integer.parseInt(actionForm.getDeleteThisRow()));
         }
         return mapping.findForward(ActionForwards.editAddFamilyRow_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward preview(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                  @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         String governmentId = actionForm.getGovernmentId();
         String clientName = actionForm.getClientName().getDisplayName();
         String givenDateOfBirth = actionForm.getDateOfBirth();
         if (actionForm.isDefaultFeeRemoved()) {
             customerDao.checkPermissionForDefaultFeeRemoval(getUserContext(request), actionForm.getOfficeIdValue(), actionForm.getLoanOfficerIdValue());
         }
         DateTime dateOfBirth = new DateTime(DateUtils.getDateAsSentFromBrowser(givenDateOfBirth));
         ProcessRulesDto processRules = this.customerServiceFacade.previewClient(governmentId, dateOfBirth, clientName);
         String pendingApprovalState = processRules.isClientPendingApprovalStateEnabled() ? CustomerConstants.YES : CustomerConstants.NO;
         SessionUtils.setAttribute(CustomerConstants.PENDING_APPROVAL_DEFINED, pendingApprovalState, request);
         addWarningMessages(request, processRules);
         actionForm.setEditFamily("edit");
         actionForm.setAge(calculateAge(DateUtils.getDateAsSentFromBrowser(givenDateOfBirth)));
         return mapping.findForward(ActionForwards.preview_success.toString());
     }
 
     private void addWarningMessages(HttpServletRequest request, ProcessRulesDto processRules)
             throws PageExpiredException {
         if (processRules.isGovernmentIdValidationFailing()) {
             SessionUtils.addWarningMessage(request, CustomerConstants.CLIENT_WITH_SAME_GOVT_ID_EXIST_IN_CLOSED);
         }
 
         if (processRules.isDuplicateNameOnBlackListedClient()) {
             SessionUtils.addWarningMessage(request, CustomerConstants.CLIENT_WITH_SAME_NAME_DOB_EXIST_IN_BLACKLISTED);
         }
 
         if (processRules.isDuplicateNameOnClosedClient()) {
             SessionUtils.addWarningMessage(request, CustomerConstants.CLIENT_WITH_SAME_NAME_DOB_EXIST_IN_CLOSED);
         }
     }
 
     @TransactionDemarcate(validateAndResetToken = true)
     public ActionForward cancel(ActionMapping mapping, ActionForm form,
                                 @SuppressWarnings("unused") HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         String forward = null;
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         String fromPage = actionForm.getInput();
         if (ClientConstants.INPUT_PERSONAL_INFO.equals(fromPage) || ClientConstants.INPUT_MFI_INFO.equals(fromPage)
                 || CenterConstants.INPUT_CREATE.equals(fromPage)) {
             actionForm.setEditFamily("notEdit");
             forward = ActionForwards.cancelCreate_success.toString();
         } else if (ClientConstants.INPUT_EDIT_PERSONAL_INFO.equals(fromPage)
                 || ClientConstants.INPUT_EDIT_MFI_INFO.equals(fromPage)
                 || ClientConstants.INPUT_EDIT_FAMILY_INFO.equals(fromPage)) {
             forward = ActionForwards.cancelEdit_success.toString();
         }
         return mapping.findForward(forward);
     }
 
     public ActionForward editPreviewEditFamilyInfo(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
                                                    @SuppressWarnings("unused") HttpServletRequest request,
                                                    @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         return mapping.findForward(ActionForwards.editPreviewEditFamilyInfo_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward retrievePictureOnPreview(ActionMapping mapping, ActionForm form,
                                                   @SuppressWarnings("unused") HttpServletRequest request, HttpServletResponse response) throws Exception {
 
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         InputStream in = actionForm.getPicture().getInputStream();
         in.mark(0);
         response.setContentType("image/jpeg");
         BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream());
         byte[] by = new byte[1024 * 4]; // 4K buffer buf, 0, buf.length
         int index = in.read(by, 0, 1024 * 4);
         while (index != -1) {
             out.write(by, 0, index);
             index = in.read(by, 0, 1024 * 4);
         }
         out.flush();
         out.close();
         in.reset();
         String forward = ClientConstants.CUSTOMER_PICTURE_PAGE;
         return mapping.findForward(forward);
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward retrievePicture(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
                                          HttpServletRequest request, HttpServletResponse response) throws Exception {
 
         ClientBO clientBO = getClientFromSession(request);
         InputStream in = clientBO.getCustomerPicture().getPicture().getBinaryStream();
 
         in.mark(0);
         response.setContentType("image/jpeg");
         BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream());
         byte[] by = new byte[1024 * 4]; // 4K buffer buf, 0, buf.length
         int index = in.read(by, 0, 1024 * 4);
         while (index != -1) {
             out.write(by, 0, index);
             index = in.read(by, 0, 1024 * 4);
         }
         out.flush();
         out.close();
         in.reset();
         String forward = ClientConstants.CUSTOMER_PICTURE_PAGE;
         return mapping.findForward(forward);
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward previewPersonalInfo(ActionMapping mapping, ActionForm form,
                                              @SuppressWarnings("unused") HttpServletRequest request,
                                              @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         actionForm.setAge(calculateAge(DateUtils.getDateAsSentFromBrowser(actionForm.getDateOfBirth())));
         return mapping.findForward(ActionForwards.previewPersonalInfo_success.toString());
 
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward prevFamilyInfo(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
                                         @SuppressWarnings("unused") HttpServletRequest request,
                                         @SuppressWarnings("unused") HttpServletResponse httpservletresponse) throws Exception {
         return mapping.findForward(ActionForwards.prevFamilyInfo_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward prevFamilyInfoNext(ActionMapping mapping, ActionForm form,
                                             @SuppressWarnings("unused") HttpServletRequest request,
                                             @SuppressWarnings("unused") HttpServletResponse httpservletresponse) throws Exception {
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         actionForm.setFamilyDateOfBirth();
         actionForm.constructFamilyDetails();
         return mapping.findForward(ActionForwards.prevFamilyInfoNext_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward prevPersonalInfo(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
                                           @SuppressWarnings("unused") HttpServletRequest request,
                                           @SuppressWarnings("unused") HttpServletResponse httpservletresponse) throws Exception {
         return mapping.findForward(ActionForwards.prevPersonalInfo_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward prevMFIInfo(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
                                      @SuppressWarnings("unused") HttpServletRequest request,
                                      @SuppressWarnings("unused") HttpServletResponse httpservletresponse) throws Exception {
         return mapping.findForward(ActionForwards.prevMFIInfo_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward prevMeeting(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
                                      @SuppressWarnings("unused") HttpServletRequest request,
                                      @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         return mapping.findForward(ActionForwards.next_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward loadMeeting(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
                                      @SuppressWarnings("unused") HttpServletRequest request,
                                      @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         return mapping.findForward(ActionForwards.loadMeeting_success.toString());
     }
 
     @TransactionDemarcate(validateAndResetToken = true)
     public ActionForward create(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         UserContext userContext = getUserContext(request);
 
         MeetingBO meeting = (MeetingBO) SessionUtils.getAttribute(CustomerConstants.CUSTOMER_MEETING, request);
         List<SavingsDetailDto> allowedSavingProducts = getSavingsOfferingsFromSession(request);
 
         List<CustomerCustomFieldEntity> customerCustomFields = CustomerCustomFieldEntity.fromDto(actionForm.getCustomFields(), null);
         CustomerDetailsDto clientDetails = this.customerServiceFacade.createNewClient(actionForm, meeting, userContext, allowedSavingProducts, customerCustomFields);
 
         actionForm.setCustomerId(clientDetails.getId().toString());
         actionForm.setGlobalCustNum(clientDetails.getGlobalCustNum());
         actionForm.setEditFamily("notEdit");
         createClientQuestionnaire.saveResponses(request, actionForm, clientDetails.getId());
         return mapping.findForward(ActionForwards.create_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward validate(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
                                   HttpServletRequest request, @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         String method = (String) request.getAttribute("methodCalled");
         return mapping.findForward(method + "_failure");
     }
 
     @TransactionDemarcate(saveToken = true)
     public ActionForward get(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                              @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         // John W - UserContext object passed because some status' need to be looked up for internationalisation based
         // on UserContext info
         UserContext userContext = getUserContext(request);
         ClientInformationDto clientInformationDto = clientDetailsServiceFacade.getClientInformationDto(
                 ((ClientCustActionForm) form).getGlobalCustNum(), userContext);
         SessionUtils.removeThenSetAttribute("clientInformationDto", clientInformationDto, request);
 
         // John W - for breadcrumb or another other action downstream that exists business_key set (until refactored)
         ClientBO clientBO = (ClientBO) this.customerDao.findCustomerById(clientInformationDto.getClientDisplay().getCustomerId());
         SessionUtils.removeThenSetAttribute(Constants.BUSINESS_KEY, clientBO, request);
         setCurrentPageUrl(request, clientBO);
         setQuestionGroupInstances(request, clientBO);
 
         return mapping.findForward(ActionForwards.get_success.toString());
     }
 
     private void setQuestionGroupInstances(HttpServletRequest request, ClientBO clientBO) throws PageExpiredException {
         QuestionnaireServiceFacade questionnaireServiceFacade = questionnaireServiceFacadeLocator.getService(request);
        boolean containsQGForCloseClient = false;
         if (questionnaireServiceFacade != null) {
             setQuestionGroupInstances(questionnaireServiceFacade, request, clientBO.getCustomerId());
            if (clientBO.getCustomerStatus().getId().equals(CustomerStatus.CLIENT_CLOSED.getValue())) {
                containsQGForCloseClient = questionnaireServiceFacade.getQuestionGroupInstances(clientBO.getCustomerId(), "Close", "Client").size() > 0;
            }
         }
        SessionUtils.removeThenSetAttribute("containsQGForCloseClient", containsQGForCloseClient, request);
     }
 
     // Intentionally made public to aid testing !
     public void setQuestionGroupInstances(QuestionnaireServiceFacade questionnaireServiceFacade, HttpServletRequest request, Integer customerId) throws PageExpiredException {
         List<QuestionGroupInstanceDetail> instanceDetails = questionnaireServiceFacade.getQuestionGroupInstances(customerId, "View", "Client");
         SessionUtils.setCollectionAttribute("questionGroupInstances", instanceDetails, request);
     }
 
     private void setCurrentPageUrl(HttpServletRequest request, ClientBO clientBO) throws PageExpiredException, UnsupportedEncodingException {
         SessionUtils.removeThenSetAttribute("currentPageUrl", constructCurrentPageUrl(request, clientBO), request);
     }
 
     private String constructCurrentPageUrl(HttpServletRequest request, CustomerBO clientBO) throws UnsupportedEncodingException {
         String officerId = request.getParameter("recordOfficeId");
         String loanOfficerId = request.getParameter("recordLoanOfficerId");
         String url = String.format("clientCustAction.do?globalCustNum=%s&recordOfficeId=%s&recordLoanOfficerId=%s",
                 clientBO.getGlobalCustNum(), officerId, loanOfficerId);
         return URLEncoder.encode(url, "UTF-8");
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward showPicture(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
                                      @SuppressWarnings("unused") HttpServletRequest request,
                                      @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         String forward = ClientConstants.CUSTOMER_PICTURE_PAGE;
         return mapping.findForward(forward);
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward editPersonalInfo(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                           @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         actionForm.clearMostButNotAllFieldsOnActionForm();
         UserContext userContext = getUserContext(request);
         ClientBO clientFromSession = getClientFromSession(request);
         final String clientSystemId = clientFromSession.getGlobalCustNum();
 
         ClientPersonalInfoDto personalInfo = this.customerServiceFacade.retrieveClientPersonalInfoForUpdate(clientSystemId, userContext);
 
         SessionUtils.setCollectionAttribute(ClientConstants.SALUTATION_ENTITY, personalInfo.getClientDropdowns().getSalutations(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.GENDER_ENTITY, personalInfo.getClientDropdowns().getGenders(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.MARITAL_STATUS_ENTITY, personalInfo.getClientDropdowns().getMaritalStatuses(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.CITIZENSHIP_ENTITY, personalInfo.getClientDropdowns().getCitizenship(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.ETHINICITY_ENTITY, personalInfo.getClientDropdowns().getEthinicity(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.EDUCATION_LEVEL_ENTITY, personalInfo.getClientDropdowns().getEducationLevels(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.BUSINESS_ACTIVITIES_ENTITY, personalInfo.getClientDropdowns().getBusinessActivity(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.POVERTY_STATUS, personalInfo.getClientDropdowns().getPoverty(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.HANDICAPPED_ENTITY, personalInfo.getClientDropdowns().getHandicapped(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.SPOUSE_FATHER_ENTITY, personalInfo.getClientDropdowns().getSpouseFather(), request);
 
         boolean isFamilyDetailsRequired = personalInfo.getClientRules().isFamilyDetailsRequired();
         SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_REQUIRED, isFamilyDetailsRequired, request);
         if (isFamilyDetailsRequired) {
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_MANDATORY, isFamilyDetailsMandatory(), request);
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_HIDDEN, false, request);
         } else {
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_MANDATORY, isSpouseFatherInformationMandatory(), request);
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_HIDDEN, isSpouseFatherInformationHidden(), request);
         }
         SessionUtils.setCollectionAttribute(CustomerConstants.CUSTOM_FIELDS_LIST, personalInfo.getCustomFieldViews(), request);
 
         // customer specific
         actionForm.setCustomerId(personalInfo.getCustomerDetail().getCustomerId().toString());
         actionForm.setLoanOfficerId(personalInfo.getCustomerDetail().getLoanOfficerIdAsString());
         actionForm.setGlobalCustNum(personalInfo.getCustomerDetail().getGlobalCustNum());
         actionForm.setExternalId(personalInfo.getCustomerDetail().getExternalId());
         actionForm.setAddress(personalInfo.getCustomerDetail().getAddress());
 
         // client specific
         actionForm.setGovernmentId(personalInfo.getClientDetail().getGovernmentId());
         actionForm.setDateOfBirth(personalInfo.getClientDetail().getDateOfBirth());
         actionForm.setClientDetailView(personalInfo.getClientDetail().getCustomerDetail());
         actionForm.setClientName(personalInfo.getClientDetail().getClientName());
         actionForm.setSpouseName(personalInfo.getClientDetail().getSpouseName());
         actionForm.setCustomFields(personalInfo.getCustomFieldViews());
 
         ClientBO client = this.customerDao.findClientBySystemId(clientSystemId);
         SessionUtils.removeThenSetAttribute(Constants.BUSINESS_KEY, client, request);
 
         return mapping.findForward(ActionForwards.editPersonalInfo_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward previewEditPersonalInfo(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                                  @SuppressWarnings("unused") HttpServletResponse httpservletresponse) throws Exception {
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
 
         String governmentId = actionForm.getGovernmentId();
         String dateOfBirth = actionForm.getDateOfBirth();
         String clientName = actionForm.getClientName().getDisplayName();
         actionForm.setAge(calculateAge(DateUtils.getDateAsSentFromBrowser(dateOfBirth)));
 
         ClientDetailDto clientDetailDto = new ClientDetailDto(governmentId, dateOfBirth, clientName);
         ClientRulesDto clientRules = this.customerServiceFacade.retrieveClientDetailsForPreviewingEditOfPersonalInfo(clientDetailDto);
 
         boolean isFamilyDetailsRequired = clientRules.isFamilyDetailsRequired();
         SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_REQUIRED, isFamilyDetailsRequired, request);
         if (isFamilyDetailsRequired) {
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_MANDATORY, isFamilyDetailsMandatory(), request);
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_HIDDEN, false, request);
         } else {
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_MANDATORY, isSpouseFatherInformationMandatory(), request);
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_HIDDEN, isSpouseFatherInformationHidden(), request);
         }
         return mapping.findForward(ActionForwards.previewEditPersonalInfo_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward prevEditPersonalInfo(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                               @SuppressWarnings("unused") HttpServletResponse httpservletresponse) throws Exception {
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         boolean isFamilyDetailsRequired = ClientRules.isFamilyDetailsRequired();
         SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_REQUIRED, isFamilyDetailsRequired, request);
         if (isFamilyDetailsRequired) {
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_MANDATORY, isFamilyDetailsMandatory(), request);
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_HIDDEN, false, request);
         } else {
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_MANDATORY, isSpouseFatherInformationMandatory(), request);
             SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_HIDDEN, isSpouseFatherInformationHidden(), request);
         }
         actionForm.setAge(calculateAge(DateUtils.getDateAsSentFromBrowser(actionForm.getDateOfBirth())));
         return mapping.findForward(ActionForwards.prevEditPersonalInfo_success.toString());
     }
 
     @CloseSession
     @TransactionDemarcate(validateAndResetToken = true)
     public ActionForward updatePersonalInfo(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                             @SuppressWarnings("unused") HttpServletResponse response) throws ApplicationException {
 
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         ClientBO clientInSession = getClientFromSession(request);
         Integer oldClientVersionNumber = clientInSession.getVersionNo();
         Integer customerId = clientInSession.getCustomerId();
         UserContext userContext = getUserContext(request);
 
         this.customerServiceFacade.updateClientPersonalInfo(userContext, oldClientVersionNumber, customerId, actionForm);
 
         return mapping.findForward(ActionForwards.updatePersonalInfo_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward editFamilyInfo(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                         @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         actionForm.clearMostButNotAllFieldsOnActionForm();
         ClientBO clientFromSession = getClientFromSession(request);
         UserContext userContext = getUserContext(request);
 
         ClientFamilyInfoDto clientFamilyInfo = this.customerServiceFacade.retrieveFamilyInfoForEdit(clientFromSession.getGlobalCustNum(), userContext);
 
         SessionUtils.setCollectionAttribute(ClientConstants.LIVING_STATUS_ENTITY, clientFamilyInfo.getClientDropdowns().getLivingStatus(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.GENDER_ENTITY, clientFamilyInfo.getClientDropdowns().getGenders(), request);
         SessionUtils.setCollectionAttribute(ClientConstants.SPOUSE_FATHER_ENTITY, clientFamilyInfo.getClientDropdowns().getSpouseFather(), request);
         SessionUtils.setCollectionAttribute(CustomerConstants.CUSTOM_FIELDS_LIST, clientFamilyInfo.getCustomFieldViews(), request);
 
         SessionUtils.setAttribute(ClientConstants.ARE_FAMILY_DETAILS_MANDATORY, isFamilyDetailsMandatory(), request);
 
         // customer specific
         actionForm.setCustomerId(clientFamilyInfo.getCustomerDetail().getCustomerId().toString());
         actionForm.setLoanOfficerId(clientFamilyInfo.getCustomerDetail().getLoanOfficerIdAsString());
         actionForm.setGlobalCustNum(clientFamilyInfo.getCustomerDetail().getGlobalCustNum());
         actionForm.setExternalId(clientFamilyInfo.getCustomerDetail().getExternalId());
         actionForm.setAddress(clientFamilyInfo.getCustomerDetail().getAddress());
 
         // client specific
         actionForm.setGovernmentId(clientFamilyInfo.getClientDetail().getGovernmentId());
         actionForm.setDateOfBirth(clientFamilyInfo.getClientDetail().getDateOfBirth());
         actionForm.initializeFamilyMember();
 
         actionForm.setClientDetailView(clientFamilyInfo.getClientDetail().getCustomerDetail());
         actionForm.setClientName(clientFamilyInfo.getClientDetail().getClientName());
         actionForm.setSpouseName(clientFamilyInfo.getClientDetail().getSpouseName());
         actionForm.setCustomFields(clientFamilyInfo.getCustomFieldViews());
 
         // client family specific
         int familyMemberCount = 0;
         for (ClientNameDetailDto familyMember : clientFamilyInfo.getFamilyMembers()) {
 
             actionForm.addFamilyMember();
             actionForm.setFamilyPrimaryKey(familyMemberCount, familyMember.getCustomerNameId());
             actionForm.setFamilyFirstName(familyMemberCount, familyMember.getFirstName());
             actionForm.setFamilyMiddleName(familyMemberCount, familyMember.getMiddleName());
             actionForm.setFamilyLastName(familyMemberCount, familyMember.getLastName());
             actionForm.setFamilyRelationship(familyMemberCount, familyMember.getNameType());
 
             Map<Integer, List<ClientFamilyDetailDto>> clientFamilyDetailsMap = clientFamilyInfo.getClientFamilyDetails();
 
             Integer key = Integer.valueOf(familyMemberCount);
             List<ClientFamilyDetailDto> clientFamilyDetails = clientFamilyDetailsMap.get(key);
 
             if (clientFamilyDetails != null) {
                 for (ClientFamilyDetailDto clientFamilyDetailDto : clientFamilyDetails) {
                     Calendar cal = Calendar.getInstance();
 
                     if (clientFamilyDetailDto.getDateOfBirth() != null) {
                         String date1 = DateUtils.makeDateAsSentFromBrowser(clientFamilyDetailDto.getDateOfBirth());
                         java.util.Date date = DateUtils.getDate(date1);
                         cal.setTime(date);
                         actionForm.setFamilyDateOfBirthDD(familyMemberCount, String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
                         actionForm.setFamilyDateOfBirthMM(familyMemberCount, String.valueOf(cal.get(Calendar.MONTH) + 1));
                         actionForm.setFamilyDateOfBirthYY(familyMemberCount, String.valueOf(cal.get(Calendar.YEAR)));
                     }
 
                     actionForm.setFamilyGender(familyMemberCount, clientFamilyDetailDto.getGender());
                     actionForm.setFamilyLivingStatus(familyMemberCount, clientFamilyDetailDto.getLivingStatus());
                 }
 
             }
 
             familyMemberCount++;
         }
 
         ClientBO client = this.customerDao.findClientBySystemId(clientFromSession.getGlobalCustNum());
         SessionUtils.removeThenSetAttribute(Constants.BUSINESS_KEY, client, request);
 
         return mapping.findForward(ActionForwards.editFamilyInfo_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward previewEditFamilyInfo(ActionMapping mapping, ActionForm form,
                                                @SuppressWarnings("unused") HttpServletRequest request,
                                                @SuppressWarnings("unused") HttpServletResponse httpservletresponse) throws Exception {
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         actionForm.setFamilyDateOfBirth();
         actionForm.constructFamilyDetails();
         return mapping.findForward(ActionForwards.previewEditFamilyInfo_success.toString());
     }
 
     @CloseSession
     @TransactionDemarcate(validateAndResetToken = true)
     public ActionForward updateFamilyInfo(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                           @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         UserContext userContext = getUserContext(request);
         ClientBO clientInSession = getClientFromSession(request);
 
         Integer customerId = clientInSession.getCustomerId();
         this.customerServiceFacade.updateFamilyInfo(customerId, userContext, clientInSession.getVersionNo(), actionForm);
 
         actionForm.setFamilyDateOfBirth();
         actionForm.constructFamilyDetails();
 
         SessionUtils.removeAttribute(Constants.BUSINESS_KEY, request);
         return mapping.findForward(ActionForwards.updateFamilyInfo_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward editMfiInfo(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                      @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         actionForm.clearMostButNotAllFieldsOnActionForm();
         UserContext userContext = getUserContext(request);
         ClientBO clientFromSession = getClientFromSession(request);
 
         String clientSystemId = clientFromSession.getGlobalCustNum();
 
         ClientMfiInfoDto mfiInfoDto = this.customerServiceFacade.retrieveMfiInfoForEdit(clientSystemId, userContext);
 
         SessionUtils.setAttribute(GroupConstants.CENTER_HIERARCHY_EXIST, ClientRules.getCenterHierarchyExists(), request);
         SessionUtils.setCollectionAttribute(CustomerConstants.LOAN_OFFICER_LIST, mfiInfoDto.getLoanOfficersList(), request);
 
         actionForm.setGroupDisplayName(mfiInfoDto.getGroupDisplayName());
         actionForm.setCenterDisplayName(mfiInfoDto.getCenterDisplayName());
 
         actionForm.setLoanOfficerId(mfiInfoDto.getCustomerDetail().getLoanOfficerIdAsString());
         actionForm.setCustomerId(mfiInfoDto.getCustomerDetail().getCustomerId().toString());
         actionForm.setGlobalCustNum(mfiInfoDto.getCustomerDetail().getGlobalCustNum());
         actionForm.setExternalId(mfiInfoDto.getCustomerDetail().getExternalId());
 
         actionForm.setGroupFlag(mfiInfoDto.getClientDetail().getGroupFlagAsString());
         actionForm.setParentGroupId(mfiInfoDto.getClientDetail().getParentGroupId().toString());
         actionForm.setTrained(mfiInfoDto.getClientDetail().getTrainedAsString());
         actionForm.setTrainedDate(mfiInfoDto.getClientDetail().getTrainedDate());
 
         ClientBO client = this.customerDao.findClientBySystemId(clientSystemId);
         SessionUtils.removeThenSetAttribute(Constants.BUSINESS_KEY, client, request);
 
         return mapping.findForward(ActionForwards.editMfiInfo_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward previewEditMfiInfo(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
                                             @SuppressWarnings("unused") HttpServletRequest request,
                                             @SuppressWarnings("unused") HttpServletResponse httpservletresponse) throws Exception {
         return mapping.findForward(ActionForwards.previewEditMfiInfo_success.toString());
     }
 
     @TransactionDemarcate(joinToken = true)
     public ActionForward prevEditMfiInfo(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
                                          @SuppressWarnings("unused") HttpServletRequest request,
                                          @SuppressWarnings("unused") HttpServletResponse httpservletresponse) throws Exception {
         return mapping.findForward(ActionForwards.prevEditMfiInfo_success.toString());
     }
 
     @CloseSession
     @TransactionDemarcate(validateAndResetToken = true)
     public ActionForward updateMfiInfo(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                        @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
 
         UserContext userContext = getUserContext(request);
         ClientCustActionForm actionForm = (ClientCustActionForm) form;
         ClientBO clientInSession = getClientFromSession(request);
 
         Integer clientId = clientInSession.getCustomerId();
         Integer oldVersionNumber = clientInSession.getVersionNo();
 
         this.customerServiceFacade.updateClientMfiInfo(clientId, oldVersionNumber, userContext, actionForm);
 
         return mapping.findForward(ActionForwards.updateMfiInfo_success.toString());
     }
 
     private int calculateAge(Date date) {
         return DateUtils.DateDiffInYears(date);
     }
 
     private ClientBO getClientFromSession(HttpServletRequest request) throws PageExpiredException {
         return (ClientBO) SessionUtils.getAttribute(Constants.BUSINESS_KEY, request);
     }
 
     @SuppressWarnings("unchecked")
     private List<SavingsDetailDto> getSavingsOfferingsFromSession(HttpServletRequest request)
             throws PageExpiredException {
         return (List<SavingsDetailDto>) SessionUtils.getAttribute(ClientConstants.SAVINGS_OFFERING_LIST, request);
     }
 
     @SuppressWarnings({"unchecked"})
     private boolean isSpouseFatherInformationMandatory() {
         return FieldConfig.getInstance().isFieldManadatory("Client." + HiddenMandatoryFieldNamesConstants.SPOUSE_FATHER_INFORMATION);
     }
 
     @SuppressWarnings({"unchecked"})
     private boolean isSpouseFatherInformationHidden() {
         return FieldConfig.getInstance().isFieldHidden("Client." + HiddenMandatoryFieldNamesConstants.SPOUSE_FATHER_INFORMATION);
     }
 
     @SuppressWarnings({"unchecked"})
     private boolean isFamilyDetailsMandatory() {
         return FieldConfig.getInstance().isFieldManadatory("Client." + HiddenMandatoryFieldNamesConstants.FAMILY_DETAILS);
     }
 
     @Override
     public ActionForward captureQuestionResponses(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
         request.setAttribute(METHODCALLED, "captureQuestionResponses");
         ActionErrors errors = createClientQuestionnaire.validateResponses(request, (ClientCustActionForm) form);
         if (errors != null && !errors.isEmpty()) {
             addErrors(request, errors);
             return mapping.findForward(ActionForwards.captureQuestionResponses.toString());
         }
         return createClientQuestionnaire.rejoinFlow(mapping);
     }
 
     @Override
     public ActionForward editQuestionResponses(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
         request.setAttribute(METHODCALLED, "editQuestionResponses");
         return createClientQuestionnaire.editResponses(mapping, request, (ClientCustActionForm) form);
     }
 }
